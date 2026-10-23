# CR-005: hostile input: link schemes, WML built from strings, and tests for what we import

Status: ACCEPTED 2026-10-23 (Jason: "CR-005 decisions: all recommendations accepted"), D1 to D7
as recommended. Proposed the same day, at Jason's request ("please draft a CR-005 for hostile
input"). He had pointed at apache/poi#1455 and asked "should we be testing what we import?".
Drafted by Claude Opus 5.5 in the docx4j-ImportXHTML session. Measured with probes against -core at
d33581f, compiled against the released docx4j 17.3.4 (§1). Phases 1 and 2 implemented 2026-10-23,
with the entry points' exceptions (§5, Jason: "fix please"; suite 162/0/0, BundleIT 3/0/0; §6),
commit ca10f61. Installed as 17.1.1-SNAPSHOT the same day, once docx4j's `HtmlChunkImport` made
the `setLinkSchemesChecked(false)` call (CR-056 phase 3, docx4j 2933e63b7; §8).
Scope: `XHTMLImporterImpl` (`createHyperlink`, and a link check with its setter),
`XHTMLImageHandlerDefault` (alt text), `Docx4jUserAgent` (a failed image fetched once),
`XHTMLtoPPTX` (the same link check), a new class for the check, a new test class in core-tests, the
README and the CHANGELOG.
Branch: `VERSION_17_3_4`. Phase 1 can ship in 17.3.4 with CR-002 to CR-004; phase 2 too if decided
in time (D6).
Depends on: nothing. docx4j's CR-056 (proposed 2026-10-23) fixes the output side and doesn't wait on
this, nor this on it.
Phases (§6): 1. tests, and WML built from objects; 2. the link check; 3. the release.

## 0. Summary

apache/poi#1455 stopped POI's `WordToHtmlConverter` from writing a .doc's `javascript:` link into
HTML. docx4j's CR-056 does the same for docx4j's HTML, PDF and Markdown output. ImportXHTML goes the
other way, HTML into docx, and the probes found three things:

1. **Every link target goes into the document as written.** That includes `javascript:`,
   `ms-msdt:`, and `file:` to another host, in both docx and pptx. No script runs in a docx. But a
   docx made from someone else's HTML is opened in Word, LibreOffice and other converters, which
   CR-056 can't reach.
2. **Two places build WordprocessingML by pasting document text into a string.** They are an
   internal link's `w:anchor` with its link text, and an image's `alt`, which docx4j's
   `createImageInline` pastes into `descr="…"`. A `"` adds attributes to the element. A `&` or `<`
   breaks the XML, and the link or image is lost without an error. The image case affects ordinary
   documents: `<img alt="Profit &amp; Loss">` comes out as "[MISSING IMAGE: …]" text.
3. **The rest holds up.** External entities and DTDs aren't loaded, and entity expansion is
   bounded. `setExternalResourcesAllowed(false)` (CR-002) fetches nothing. Two limits are worth
   knowing:
   - A failed image is fetched four times. On a server that never answers, that is two minutes
     per image.
   - Nesting deeper than about 730 `div`s overflows a 1 MB stack, inside openhtmltopdf.

The fixes:
- Build both elements from JAXB objects (phase 1). Nothing changes for ordinary input.
- Check link schemes against the allowlist POI and CR-056 use (phase 2). A refused link keeps its
  text. There is a setter to turn the check off, and a property to add schemes (D1 to D4).
- Fetch a failed image once.
- Add `HostileInputTest`, one test per row of §1, so that what holds up today keeps holding.

## 1. Measured (2026-10-23)

The probes call `XHTMLImporterImpl.convert(String, baseUrl)` on -core at d33581f, compiled against
the released docx4j 17.3.4. The only network access is a listener on 127.0.0.1. The probes and their
output are in `~/claude-scratch-archive/docx4j-ImportXHTML/hostile-input-probes-2026-10-23`.

| # | Input | Result | |
|---|---|---|---|
| 1 | `<a href="javascript:alert(1)">`; also `" JaVaScRiPt:alert(2)"`, `file://attacker.invalid/share/x`, `ms-msdt:/id%20PCWDiagnostic` | an External hyperlink relationship with that target, exactly as written | passed through |
| 2 | `<a href='#x" w:tooltip="INJECTED'>` | `<w:hyperlink w:tooltip="INJECTED" w:anchor="x">`; with `r:id="rId1"` instead, the link points at relationship rId1 | attribute injection |
| 3 | `<a href="#a&amp;b">LINKTEXT</a>`, or `#a&lt;b` | "before  after": the link and its text are gone. The parse error is logged, and nothing reaches the caller | content lost |
| 4 | link text with a literal `&amp;` and another `&` (source `&amp;amp; then &amp;`) | the link and its text are gone (`R&amp;D` alone is fine) | content lost |
| 5 | `<img alt="Profit &amp; Loss" src="data:…">`, or an `alt` with `&lt;` | `[MISSING IMAGE: Profit & Loss, Profit & Loss ]` instead of the image | content lost |
| 6 | `<img alt='x" title="INJECTED' src="data:…">` | `<wp:docPr id="2" name="" descr="x" title="INJECTED"/>` | attribute injection |
| 7 | `<!ENTITY x SYSTEM 'file:///etc/hostname'>`, used as `&x;` | `[]`: not expanded, with `setExternalResourcesAllowed` either way | safe |
| 8 | `<!DOCTYPE html SYSTEM 'http://127.0.0.1:…/ext.dtd'>`; a parameter entity `%p;` pointing at the listener | not requested; the parameter entity is a parse error | safe |
| 9 | nine levels of ten entity references (10⁹ expansions) | refused by the parser in 0.5 s | safe |
| 10 | `link rel=stylesheet`, `@import`, `img` on the listener | fetched by default, as documented; nothing fetched when `false` (`ExternalResourcesTest`) | as designed |
| 11 | one `img` whose server answers 404 | requested 4 times (an image that loads is requested once) | amplified |
| 12 | one `img` whose server accepts and never answers | the conversion takes 121 s: 4 connections, each waiting 30 s for a read | slow |
| 13 | nested `div`s / nested `span`s, default 1 MB thread stack | up to 730 / about 1,500 convert (two runs gave 730 both times for `div`, and 1,640 and 1,484 for `span`, since JIT frames vary); deeper gives `StackOverflowError` in openhtmltopdf's `BoxBuilder` and `CalculatedStyle.valueByName` | stack |

openhtmltopdf logs "Unable to disable XML External Entities" (SEVERE) on every parse. It can't set
`accessExternalDTD` on the TransformerFactory that docx4j brings (xalan). Rows 7 to 9 show that the
parse is safe all the same. The protection comes from the parser's own settings, which depend on
the parser that is on the classpath. That's why the tests pin it.

From reading the code, not run:
- **The pptx path does the same as row 1.** It also makes every `#fragment` an External
  relationship.
- **Every entry point that parses uses the same parser,** openhtmltopdf's `XMLResource`:
  `convert(String)`, `(InputStream)`, `(Reader)`, `(InputSource)`, `(File)`, `(URL)`, `convertMHT`
  and `XHTMLtoPPTX`. `convert(Node)` takes a DOM that the caller parsed.

## 2. Where the sinks are

- **`XHTMLImporterImpl.createHyperlink`** (line 3090) builds the `w:hyperlink` as a string and
  unmarshals it:
  - For an internal link, `w:anchor="` + the fragment + `"` goes in unescaped (line 3110).
  - The link text is escaped by hand: `&` only when the text has no `&amp;` in it already, and then
    `<` and `>`.
  - For an external link, the target goes into a JAXB `Relationship`, which escapes it correctly.
    Its scheme isn't checked.
- **`XHTMLImageHandlerDefault.addImage`** (lines 218, 220 and 235) passes `img/@alt` to docx4j's
  `BinaryPartAbstractImage.createImageInline`. That method substitutes it into `descr="${altText}"`
  in a template string, and `XmlUtils.unmarshallFromTemplate` doesn't escape (docx4j 17.3.4). The
  code is docx4j's, but the input is ours (§5).
- **`XHTMLtoPPTX.addHyperlinkRelationship`** (line 514) makes every `href` an External relationship.
  It is built with JAXB, so it's well-formed, but the scheme isn't checked.
- **`Docx4jUserAgent.getImageResource`** (line 77) caches an image that loaded, but not one that
  failed. Layout and `addImage` each ask for the image again, so a failed image is fetched four
  times. openhtmltopdf's HTTP stream factory waits 10 s to connect and 30 s for each read.
- **Checked and clean:**
  - The other `unmarshalString` calls take constant strings: `HR_XML`, and pptx's `TXBODY_SHAPE`
    and `createTableCell`.
  - Bookmarks (`BookmarkHelper`) and content control tags (`DivToSdt`) are built as objects.
  - The "[MISSING IMAGE …]" text is a `Text` object.

## 3. What the rule should be

**Links: an allowlist of schemes,** the same one as CR-056's D1 and POI's `isSafeHyperlink`:
- no scheme (a relative path, a fragment or a UNC path);
- a Windows drive path (`C:\`);
- `http`, `https`, `ftp`, `mailto`, `file`, `news` and `tel`.

The scheme is read as a browser reads it (WHATWG URL parsing): leading control characters and spaces
are ignored, tabs and line breaks are dropped, and case is ignored. A refused target keeps its text
but gets no link. One WARN per conversion names the first refused target. Fragments aren't affected,
since they have no scheme.

Why check on import when CR-056 fixes the outputs:
- CR-056 protects what docx4j writes.
- ImportXHTML is where a target from HTML that someone else wrote enters an OOXML document. That
  document is then opened by tools neither project controls: Word, LibreOffice, other docx-to-HTML
  converters, and mail and file previewers.

The case against, which is why there's a switch (D2):
- Word's own import of an HTML altChunk keeps such a target (CR-056 §4, from docx4j-25), and CR-002
  and CR-003 move ImportXHTML towards Word's import.
- docx4j's text model and inspector read altChunks through ImportXHTML (`HtmlChunkImport`). The
  inspector's `altchunk.script` finding wants the target as written.

**WordprocessingML is built from objects, never from strings.**
- **`createHyperlink`** makes a `P.Hyperlink` with `setAnchor` or `setId`, and an `R` holding a
  `Text`. An ordinary link's output stays the same.
- **The image's alt text:** `addImage` passes `""` to `createImageInline`, then sets
  `inline.getDocPr().setDescr(alt)`. That is right whether or not docx4j later escapes inside
  `createImageInline`. Escaping before the call would double-escape once docx4j does.

**A failed image is fetched once per conversion.** The user agent remembers the failure until the
next `convert`.

**Not changed:**
- XML parsing: it is safe as measured, and the tests now pin it.
- The default of `setExternalResourcesAllowed`, decided in CR-002. The README says to set it to
  false for HTML you didn't write.
- Deep nesting (D5).

## 4. Proposal

- **Phase 1** (no change for ordinary input):
  - `createHyperlink` and `addImage` as §3.
  - `Docx4jUserAgent` remembers failed URIs, and `convert` clears them.
  - `HostileInputTest` in core-tests, one test per row of §1:
    - rows 2 to 6 assert the injected attribute is absent and the link or image is present;
    - rows 7 to 9 run through each entry point that parses (§1), `convertMHT` included;
    - row 11 asserts a single request.
- **Phase 2** (the link check):
  - `org.docx4j.convert.in.xhtml.LinkSchemes` (D4), a final class with a private constructor:
    `public static boolean isAllowed(String target)`, and the default set.
  - The property `docx4j-ImportXHTML.Hyperlink.Schemes`, a comma-separated list added to the
    default set (D3), read through `ImportXHTMLProperties`.
  - `XHTMLImporterImpl.setLinkSchemesChecked(boolean)` and `isLinkSchemesChecked()`, default true
    (D2). The same pair goes on `XHTMLtoPPTX`, set before `convertSingleSlide`.
  - `createHyperlink` and `XHTMLtoPPTX.createHyperlink` call the check. A refused target gives the
    runs without the hyperlink.
  - Tests:
    - refused: row 1's four targets, `java\tscript:`, `vbscript:` and `data:`;
    - kept: `http`, `https`, `mailto`, a relative path, a fragment, `file:` and `C:\`;
    - the property adds a scheme;
    - the switch keeps `javascript:` as written;
    - pptx: row 1.
- **The README** gets a section, "Converting HTML you didn't write":
  - `setExternalResourcesAllowed(false)`;
  - the link check and the property;
  - the nesting limit and the fetch timeouts.
- **The CHANGELOG** gets an entry for each phase.

## 5. Found on the way

Both were passed to docx4j-25 on 2026-10-23, at Jason's word ("pass docx4j-25 the two docx4j
findings"). Neither is in CR-056, which is about output. Both were measured on docx4j 17.3.4's own
API (`Probe6` in the probe archive). docx4j-25 confirmed both in source and is writing them up as
docx4j CR-057, for the input side:
- **(1)** escaped inside `createImageInline`. Phase 1's `""` plus `setDescr(alt)` stays safe once
  that ships: one is escaping at template substitution, the other a JAXB value, so nothing is
  escaped twice. It shipped as CR-057 phase 1 (docx4j 26ddf11c5, 17.3.5-SNAPSHOT, 2026-10-23).
  Phase 1's code passes `null` and `""`, so it is unchanged against 17.3.5 too. It can stay when
  ImportXHTML moves to 17.3.5; only the comment in `XHTMLImageHandlerDefault`, which says
  docx4j pastes the alt text unescaped, would then describe 17.3.4 alone.
- **(2)** a `Docx4jProperties` switch. "Never fetch" is recommended as the default from docx4j
  17.3.5, with Word's fetching on request; the default is Jason's decision.

- **docx4j's `BinaryPartAbstractImage.createImageInline` doesn't escape** `filenameHint` or
  `altText`. Alt text `Profit & Loss` or `a < b` throws `UnmarshalException`. With
  `x" title="INJECTED`, the `wp:docPr` gets a `title` attribute (rows 5 and 6 are how ImportXHTML
  meets this). Phase 1 avoids it in ImportXHTML, whatever docx4j does.
- **docx4j's `convertAltChunks` fetches what an HTML altChunk refers to, by design.**
  - The path is `HtmlChunkImport.convert` → `imported(…, fetch = true, …)`. Its Javadoc says "The
    importer fetches what the chunk refers to, as Word does on open". Only the text model's `read`
    calls `neverFetch`.
  - Measured on a chunk with a `link`, an `img` on the listener and an `img` of
    `file:///etc/hostname`: one request for the stylesheet and four for the image. The local file
    was opened four times, and refused only because it isn't an image.
  - So a server that runs `convertAltChunks` on a docx someone uploaded fetches the URLs, and reads
    the local image files, that the docx names. Callers have no switch to stop it.

One more, in ImportXHTML, found while writing the tests (`Probe7`). It was outside what was
accepted, so it went to Jason, who said to fix it (2026-10-23: "The InputStream, Reader,
InputSource, File and URL versions throw openhtmltopdf's unchecked XRRuntimeException: fix
please"):

- **The entry points threw different exceptions for input that isn't well-formed.**
  - `convert(String)` wrapped the parse error in a `Docx4JException`.
  - `convert(InputStream)`, `(Reader)`, `(InputSource)`, `(File)` and `(URL)` let openhtmltopdf's
    unchecked `XRRuntimeException` out.
  - `convertMHT` falls back to jsoup.

  A caller that catches `Docx4JException` missed the unchecked ones.
- **Fixed, as part of phase 1:**
  - `convert(String)`'s handling moved into `notWellFormed(XRRuntimeException)`. All six entry
    points now use it; `loadDocument` covers `File` and `URL`, in both of its branches.
  - The message is unchanged: "issues at Line n, Col m", with the `TransformerException` as the
    cause.
  - One difference for `convert(String)`: an `XRRuntimeException` whose cause isn't a
    `TransformerException` used to be rethrown as it was. It is now wrapped too, so no convert that
    parses throws it. The original handler also logged `t.getMessage()` before checking whether
    the cause was null; the helper checks first.
  - Nothing in docx4j catches `XRRuntimeException`.
  - Not changed: `XHTMLtoPPTX`'s constructor, which parses and is declared `throws Exception`.
  - `Probe7` rerun: all six entry points give `Docx4JException`, with fetching allowed and not.
  - `HostileInputTest.notWellFormedGivesDocx4JException` pins that for each of them, both ways.
    `externalDtdAndParameterEntityNotFetched` again catches only `Docx4JException`.

## 6. Phases

### Phase 1: tests, and WML built from objects

As §4. The CHANGELOG entry describes rows 2 to 6 as a fix.

**Done 2026-10-23** (commit ca10f61):
- **`createHyperlink`** builds the `P.Hyperlink`, `R` and `Text` as objects, with their parents
  set as unmarshalling set them. Two things are gone: the hand escaping of the link text, and the
  catch that logged "Dodgy link text" and returned null.
- **`XHTMLImageHandlerDefault.addImage`** passes `""` to `createImageInline` in all three calls,
  then sets `inline.getDocPr().setDescr(alt)`.
- **`Docx4jUserAgent`** keeps a set of URIs that failed, and doesn't ask for those again. This
  differs from §4, which said `convert` would clear the set: nothing needs to. `layoutAndTraverse`
  drops the renderer after each convert, so a user agent, and its set, lasts exactly one convert.
- **`HostileInputTest`** (new, LGPL header) has 9 tests for rows 2 to 9 and 11:
  - Rows 7 and 8 run through seven entry points (`String`, `InputStream`, `Reader`,
    `InputSource`, `File`, `URL`, `convertMHT`), with `setExternalResourcesAllowed` true and false.
  - Run against the old -core (its sources stashed), the six tests for phase 1's fixes fail, and
    the three that pin rows 7 to 9 pass.
- **The probes, rerun:**
  - Rows 2 to 6 are text: `w:anchor="x&quot; w:tooltip=&quot;INJECTED"`, and
    `descr="Profit &amp; Loss"`.
  - Row 11 makes one request.
  - Row 12: an image on a server that never answers now holds a conversion for 31 s, one
    connection and one 30 s read timeout (it was 121 s). The README names the timeouts, and
    `setExternalResourcesAllowed(false)` for HTML you didn't write.

### Phase 2: the link check

As §4, once D1 to D4 are decided. Then send docx4j-25 the commit. They take
`setLinkSchemesChecked(false)` in `HtmlChunkImport`, for the inspector (§8), in CR-056 phase 3
(agreed 2026-10-23).

**Done 2026-10-23** (commit ca10f61):

- **`LinkSchemes`** (new, LGPL header) holds `isAllowed`, `defaults()` and `PROPERTY`. It reads a
  scheme as POI's `isSafeHyperlink` does, written afresh: tabs and line breaks are dropped, and
  leading controls and spaces are ignored. A single-letter scheme is a drive.
- **`XHTMLImporterImpl`:**
  - `setLinkSchemesChecked` and `isLinkSchemesChecked`.
  - `checkedHref` is applied where an `a`'s `href` is read. A refused `href` becomes `""`, so the
    `a` takes the path of an `a` without an `href`. Its content is imported as runs, formatting
    and nested elements included, and an `a/@name` bookmark is kept.
  - One WARN per convert (the flag is reset in `layoutAndTraverse`); DEBUG after that.
- **`XHTMLtoPPTX`:**
  - The same pair of methods.
  - `processHyperlink` gives a refused target no hyperlink.
  - Its Plutext copyright line moves from 2011-2022 to 2011-2026.
- **`HostileInputTest`** has six more tests (15 in all; 16 with §5's):
  - six refused targets: row 1's, plus `java\tscript:`, `vbscript:` and `data:`;
  - ten kept, plus a fragment: `file:` and UNC included, per D3;
  - a refused link keeps its text and its bold;
  - the property adds a scheme;
  - unchecked keeps `javascript:`;
  - pptx, checked and unchecked.
- **The tests' `module-info` comment** names `HostileInputTest` as the second user of
  `jdk.httpserver`.
- **The README** has a section, "Converting HTML you didn't write". **The CHANGELOG** has an
  entry for each phase.
- **Measured:**
  - The suite runs 161/0/0, and 162/0/0 with §5's fix.
  - `mvn -o clean verify -Dgpg.skip` passes for all five modules, with `BundleIT` 3/0/0 (the
    bundle shades the new -core).

### Phase 3: the release

This goes with CR-002's phase 4, CR-003's phase 2 and CR-004's phase 2 as 17.3.4 (D6).

## 7. Decisions (for Jason; each with a recommendation)

All seven decided as recommended on 23 October 2026 (Jason: "CR-005 decisions: all recommendations
accepted").

- **D1. Check link schemes on import?**
  - (a) Yes, as §3.
  - (b) No: the sink is the output, and CR-056 covers docx4j's.

  **Recommended: (a).** The document ImportXHTML writes is read by more than docx4j.
- **D2. The default.**
  - (a) On. `setLinkSchemesChecked(false)` keeps every target as written.
  - (b) Off, opt-in.

  **Recommended: (a).** POI and CR-056 both refuse by default. Most links are `http`, `https` or
  `mailto`, and a refused link keeps its text. docx4j's `HtmlChunkImport` already sets the
  importer's options by reflection, and can turn this one off as well.
- **D3. The set, and adding to it.**
  - (a) CR-056's set (POI's). The property adds to it; nothing removes from it.
  - (b) A narrower set for import. For example, without `file:` and UNC paths.

  **Recommended: (a).** It gives one rule for docx4j and ImportXHTML. §8 has the cost of keeping
  `file:`.
- **D4. Where the check lives.**
  - (a) ImportXHTML's own `LinkSchemes` now, with the same reading as CR-056's `LinkTargets`. It
    calls docx4j's instead once ImportXHTML is built against a docx4j that has it.
  - (b) Wait for docx4j's `LinkTargets` (docx4j 17.3.5), and ship phase 2 in ImportXHTML 17.3.5.

  **Recommended: (a).** It is about 40 lines, the tests pin the same answers, and ImportXHTML's
  next release doesn't wait.
- **D5. Deep nesting.**
  - (a) Documented in the README, with no limit.
  - (b) A depth limit, checked on the DOM before layout, that throws `Docx4JException`.

  **Recommended: (a).**
  - The overflow is in openhtmltopdf, not our code.
  - How deep a document can go depends on the thread's stack size (`-Xss`). A fixed limit would
    refuse documents that a bigger stack converts.
  - `StackOverflowError` ends the conversion, not the JVM. docx4j's `HtmlChunkImport` already runs
    the import as a task with a timeout, and catches `Throwable`.
- **D6. The release.**
  - (a) Phase 1 in 17.3.4, and phase 2 too if D1 to D4 are decided before the release.
  - (b) Both phases in a later release.

  **Recommended: (a).** Phase 1 is a bug fix that ordinary documents hit (row 5).
- **D7. An advisory.**
  - (a) A GitHub security advisory.
  - (b) The CHANGELOG only.

  **Recommended: (b).**
  - The injection reaches attributes on two elements; the probes found no way to add elements
    (§8).
  - No script runs in what ImportXHTML writes.
  - It is Jason's call, as with CR-056's D9.

## 8. Risks

- **Links lost.** With the check on, a link with a scheme outside the set loses its link in
  existing users' output: `notes:`, `onenote:`, `ms-word:`, `sms:` and the like. The text stays,
  the WARN names the first one, and the property brings a scheme back.
- **docx4j's inspector loses what it reports.** When docx4j reads an HTML altChunk through
  ImportXHTML with the check on, a `javascript:` link comes out as text. The inspector's
  `altchunk.script` finding then has nothing to report. `HtmlChunkImport` needs one more
  reflective call, as for `neverFetch`. That is docx4j-25's to make, and phase 2 tells them.
  They made it the same day, in docx4j 2933e63b7 (CR-056 phase 3), and the snapshot was installed
  after it. Their test with the installed bundle jar: a chunk's
  `<a href='javascript:alert(1)'>CLICK</a>` gives the run "CLICK -> javascript:alert(1)", and the
  inspector reports altchunk, altchunk.script and altchunk.external_ref. (This CR first named
  the finding `relationship.external`, from docx4j-25's hand-off; they corrected it after the test.)
- **Unlike Word's altChunk import.** With the check on, `convertAltChunks` drops a link that Word
  keeps. That runs against CR-002 and CR-003's direction, for links only.
- **`file:` and UNC paths stay links.** That is POI's and CR-056's set. Following a link to
  `\\host\share` makes Windows connect to that host. D3's (b) is the alternative.
- **Two copies of the scheme reading,** until D4 moves ImportXHTML to docx4j's.
- **Element injection wasn't found, but isn't ruled out by a proof.** In `createHyperlink`, the
  string after the fragment holds the namespace declarations and an unclosed root. Every injected
  element the probe tried left the XML unbalanced, or declared a prefix twice. Phase 1 removes the
  string, so the question goes away.
- **Coverage.** The sinks are the ones found by reading -core. New code that builds WML from a
  string would need its own test.

## 9. Sources

- apache/poi#1455, "WordToHtmlConverter: don't link HYPERLINK fields with script targets" (merged
  2026-10-22): `WordToHtmlUtils.isSafeHyperlink` and its scheme set.
- docx4j's `docs/developer/change-requests/CR-056-html-output-script-links-and-css-identifiers.md`
  (proposed 2026-10-23): the output side. §3 and D1 to D3 give the scheme rule this CR shares. §4
  puts ImportXHTML out of its scope.
- The WHATWG URL Standard, "URL parsing": how a browser reads a scheme.
- This repository: `XHTMLImporterImpl.java` (`createHyperlink`), `XHTMLImageHandlerDefault.java`,
  `renderer/Docx4jUserAgent.java`, `org/pptx4j/convert/in/xhtml/XHTMLtoPPTX.java`, and the
  `ExternalResourcesTest` in core-tests.
- docx4j 17.3.4: `BinaryPartAbstractImage.createImageInline`, `XmlUtils.unmarshallFromTemplate`, and
  `model/text/HtmlChunkImport.java` (`neverFetch`).
- openhtmltopdf-core 1.1.31: `NaiveUserAgent$DefaultHttpStreamFactory`, 10,000 ms to connect and
  30,000 ms for each read.
