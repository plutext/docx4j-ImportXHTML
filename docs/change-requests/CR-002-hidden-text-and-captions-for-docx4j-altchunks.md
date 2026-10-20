# CR-002: display:none as hidden text, the elements it came from, and the table caption (docx4j CR-053's asks)

Status: ACCEPTED 2026-10-20 (Jason: "CR-002 accept all recommendations. commit then implement."),
D1 to D11 as recommended. Phases 1 to 3 implemented 2026-10-20 (suite 140/0/0; §11), in one
commit (hash in the next); phase 2 installed as docx4j-ImportXHTML-core 17.1.1-SNAPSHOT for docx4j
CR-053 phase 2, and docx4j-25 told. Phase 4 (the release) next. Proposed 2026-10-20, at the
request of the docx4j session (docx4j-25) for docx4j CR-053 §4a (accepted by Jason 2026-10-20,
docx4j c69d4282d), whose phase 2 depends on items 1 and 2. Drafted by Claude Opus 5.5 in the
docx4j-ImportXHTML session, with openhtmltopdf's cascade, the caption defect and the extras
measured first (§2 to §6).
Scope: `XHTMLImporterImpl`, `TableHelper`, and a pre-pass over the DOM before layout; phase 3 also
`ListHelper`, `HeadingHandler`, `DocxRenderer`, a namespace handler of our own, and jsoup as an
optional dependency. Phase 2 changes nothing unless its option is set. Otherwise output changes only
for a table with a `caption`, an `ol type` or `li value`, and an MHT whose HTML fails today.
Branch: `VERSION_17_3_4`; released as 17.3.4, which is due anyway for `setExternalResourcesAllowed`
(c8dc938).
Phases (§8): 1. the caption; 2. `display: none` as hidden text, and the hook; 3. the extras, as D7
to D10 decided; 4. the release.

## 1. Background

Word 365 reads HTML, XHTML and MHT altChunks as HTML. What `display: none` hides, inline or through a
rule in the chunk's own `style` element, becomes hidden text (`w:vanish`; a class rule becomes a
style with `w:vanish`). Nothing else hides anything: not `visibility`, the `hidden` attribute,
`noscript`, `template` or closed `details` (Jason's probes, 2026-10-20, docx4j CR-053 §2).

docx4j's text model and its inspector will read all three kinds through this importer (CR-053 D3).
docx4j parses each chunk itself (as XML, or with jsoup's WHATWG parser and `W3CDom`). It then calls
`convert(Node)` with `setExternalResourcesAllowed(false)`, so the importer never parses a chunk.
The inspector runs the same import and must report each hidden element by its path in docx4j's tree.

Today the importer drops `display: none` content entirely. openhtmltopdf builds no box for such an
element, and the importer walks boxes, so `hidden: true` in docx4j has nothing to bring back. Nothing
in the importer handles `display: none` today: it reads `display` only to classify table elements.

docx4j's asks (CR-053 §4a), with the extras it asked for but does not need, are:

| # | Ask | Needed by CR-053 | Here |
|---|---|---|---|
| 1 | `display: none` as hidden text, behind an option, off by default | yes (phase 2) | §2, phase 2 |
| 2 | A hook naming the source element of each outermost hidden element's content | yes (phases 2 and 3) | §3, phase 2 |
| 3 | A table with a `caption` comes out as a `w:tbl` directly inside a `w:tbl` | no, but `convertAltChunks` users get it today | §4, phase 1 |
| - | `data:` images admitted with external resources disallowed | no (docx4j guards its own calls) | §5 (D5) |
| - | `ol type`, `li value`; `h1`-`h6` as heading styles; `hr` as a border; `convertMHT` through jsoup | no | §6, phase 3 (D7 to D10) |

docx4j reaches the option and the hook by reflection and falls back where they are absent, so the
two releases need no lockstep.

## 2. `display: none` as hidden text

### 2.1 What openhtmltopdf 1.1.31 offers (measured)

A scratch program run against the importer's own renderer, between `setDocument` and `layout`,
confirmed each step below. Its document had a `p` hidden by a `style` attribute, a `div`, an `li` and
a `tr` hidden by class, a `span` hidden inline, a `!important` rule, and a rule under each of
`@media screen` and `@media print`.

- **Which elements an author hid.**
  - `renderer.getSharedContext().getCss().getCascadedStyle(e, false).propertyByName(CSSName.DISPLAY)`
    returns the winning `display` declaration, or null where no rule sets it.
  - `CascadedStyle.addProperties` keeps only the winner: declarations are sorted by origin and
    importance, then the last one put wins. So the declaration's `getOrigin()` says who won.
  - The origins are `StylesheetInfo.USER_AGENT`, `USER` and `AUTHOR`. A `style` attribute, a
    `<style>` element and a `<link>`ed sheet are all `AUTHOR`. `USER` occurs only on synthetic
    declarations for anonymous boxes.
  - `head`, `script` and a `select`'s `option` come out `none` with origin `USER_AGENT`, and are left
    alone.
- **The element's default `display`.**
  - Build a second `Matcher` over the default stylesheet alone, with no `StylesheetFactory`, so that
    `style` attributes are ignored. Ask it for the element's `display`; where it has none, use the CSS
    initial value, `inline`.
  - Filtering the main cascade by origin cannot work, since only the winner survives.
  - The default stylesheet is openhtmltopdf's own `/resources/css/XhtmlNamespaceHandler.css`, already
    parsed and cached. It gives `div`, `p` and `ul` `block`; `li` `list-item`; `table` `table`; `tr`
    `table-row`; `td` and `th` `table-cell`; `tbody` `table-row-group`; `thead` `table-header-group`;
    `caption` `table-caption`.
- **Re-showing.**
  - Append `display:<default> !important` to the element's `style` attribute, after a `;` if the
    value lacks one.
  - An inline `!important` beats a stylesheet's `!important`: both land in the author-important
    bucket, and the `style` attribute is applied last. This also holds when the attribute itself
    already says `display:none !important`, since the appended declaration comes later.
- **No second `setDocument` is needed.** The `style` attribute is parsed again on every cascade, so
  laying out straight after the edits gave a box for every re-shown element.
  - A second `setDocument` would also work (about 0.6 ms), but it re-reads `<link>`ed sheets through
    the user agent, which keeps no cache.
- **Pitfall: the pre-pass must not call `SharedContext.getStyle(e)`.** Its cache survives
  `setDocument` and is what layout reads. After a `getStyle` pass, re-showing gave no box even with a
  second `setDocument`.
- **`@media`.**
  - `DocxRenderer` sets `setPrint(true)`, so the medium is `print`.
  - A `<style media="screen">` element is dropped whole, and an `@media screen` block is skipped:
    content hidden only for the screen shows today, and stays shown.
  - An `@media print` rule applies, so the pre-pass would make its content hidden text.
  - **Word applies neither** (Jason's Word 365 probe, 2026-10-20, docx4j CR-053 §10.6): a rule
    inside any `@media` block hides nothing. See D3.

**Where Word's selectors differ from openhtmltopdf's** (the same probe, CR-053 §10.7):
- Word hides by an inline style, a class, an `#id`, a descendant selector (`div.box p`), an
  element-qualified class (`p.os`), and `!important` over an inline style. It also honours a
  `<style>` in the `body`, which openhtmltopdf ignores.
- It does not hide by an HTML5 element selector (`article{}`), a child selector (`>`), an attribute
  selector, a compound class (`.a.b`) or `:first-child`. A type selector on an HTML4 element (`p`,
  `span`) is not measured yet, and is suggested for docx4j's next probe.
- Order, specificity and inline-over-rule behave as in a browser.

docx4j will rewrite each chunk's `style` elements before calling the importer: drop the `@media`
blocks and the selectors Word ignores, and move `body` styles to the `head` (CR-053 §10.7, its D4).
So the pre-pass applies openhtmltopdf's cascade as it stands, and Word's selector rules are the
caller's to impose.

### 2.2 The pre-pass

It runs at the top of `layoutAndTraverse` (`XHTMLImporterImpl`:869), on the renderer's thread
(`CSSParser` uses `ThreadCtx`), when the option is on and before `renderer.layout()`. Every `convert`
overload passes through there, `convertMHT` included.
`DocxRenderer` needs a getter for its document (`_doc`), or the pass lives in `DocxRenderer`.

1. Walk the `body`'s descendant elements in document order. For each whose cascaded `display` is
   `none` with origin `AUTHOR`:
   - take its default `display` from the default-stylesheet cascade;
   - if that is `none` too, leave the element alone;
   - otherwise record its `style` attribute (or its absence), append
     `display:<default> !important`, and add the element to an identity set of marked elements.
     Use a set, not an attribute, so that attribute selectors can't be thrown off.

   In the same walk, add to a second set, of revealing elements, each element whose cascaded
   `display` has origin `AUTHOR`, whether set by a rule or a `style` attribute, and is any value but
   `none` or `inherit` (D11, §2.3).
   - `inherit` must be excluded by name. The cascade holds the specified keyword, not the value it
     resolves to (measured: `display:inherit` cascades as `inherit`).
   - openhtmltopdf's parser drops `display:initial`, so it neither hides nor reveals.
2. Lay out and traverse as today (§2.3).
3. In a `finally`, restore every marked element's `style` attribute, or remove it if it was absent.
   The caller's `Document` comes back unchanged. That matters because `convert(Node)` lays out a
   `Document` it is given in place.
   - "Unchanged" means exactly the value found at convert time. docx4j's own pre-pass will already
     have appended `float:none !important` to each element's `style` before calling
     (docx4j CR-053 §10), and that must survive.

`display` is not inherited, so a descendant of a marked element is not itself `display: none` unless
a rule makes it so. It is hidden through its marked ancestor (§2.3), unless it reveals itself. A
descendant that is itself `display: none` is marked too, and reported through the outermost (§3).

Known limits:
- **The default isn't always what the author meant.** With `.blk{display:block}` and
  `span.blk{display:none}`, the span comes back `inline`, the default for a `span`, not `block`. The
  cascade keeps only the winner, so the losing `block` can't be recovered.
- **An unbalanced comment or string** in an existing `style` value could swallow the appended
  declaration. Such a value is broken anyway.
- **Re-showing changes geometry, as Word does** (Jason's probe, CR-053 §10.6):
  - a hidden `td` keeps its cell in its column, with its text hidden. Word keeps `C2_R3_B` in
    column 2, so re-showing gives Word's grid;
  - a hidden `tr` keeps its row: its text is hidden, and the row is marked `w:trPr/w:hidden` (§2.3);
  - a hidden `li` is kept and numbered, so it takes a number, as in Word;
  - a hidden `table`, `thead` or `tbody` keeps every row, with its text hidden and `w:trPr/w:hidden`
    on each row (round 6). In Word's layout, a wholly hidden table's column collapses to almost
    nothing.
- **A `display:inherit` element inside a re-shown one** computes the re-shown display in the
  layout: a `span` inside a re-shown `div` becomes a block. Its text is hidden either way, as Word
  hides it, but it may start a paragraph of its own.

### 2.3 Emitting hidden text

- **Tracking.**
  - The importer already knows each box's element: `BlockBox.getElement()` in `traverseBlockBox`
    (:1098), and an `InlineBox`'s element, which is the nearest enclosing inline element, in
    `processInlineBox` (:2139).
  - The hidden owner of a box is found by a walk up the DOM from its element, cached per element:
    - each marked element passed becomes the owner, so the last one passed before the walk stops
      is the outermost;
    - the walk stops at a revealing element, or at the root.

    If no marked element was passed, the box is not hidden. So, as Word does (D11):
    - in `div(none) > p`, the `p` is hidden, owned by the `div`;
    - in `div(none) > p(display:block)`, the `p` shows;
    - in `div(none) > p(display:block) > span(none)`, the `span` is hidden, owned by itself.
  - While the box being processed has an owner, the traversal records each paragraph it creates
    (`getCurrentParagraph(true)`, and the math `P` at about :1585) and each table, against that
    owner.
  - It also records each run it creates outside a recorded paragraph. The run sites are:
    - `addRun` (:2505);
    - the `br` run (:2388);
    - the hyperlink's run (`createHyperlink`, :2896);
    - the field runs of `prepareCaption` (:1823);
    - the image runs, which `XHTMLImageHandler` adds to the current paragraph. Take the runs added
      across the `addImage` call, so that a custom handler is covered too.
- **Applying.** After the traversal, one pass over what was recorded:
  - a recorded paragraph gets `w:vanish` on its paragraph mark (`pPr/rPr`) and on every run inside
    it, hyperlinks and fields included;
  - a recorded table gets the same on every paragraph in it, and keeps its shape;
  - a row whose `tr` has a hidden owner also gets `w:trPr/w:hidden`, as Word gives it. Its owner is
    itself, or a marked `table`, `thead`, `tbody` or `tr` above it with no revealing element
    between. So the traversal records the `Tr` it creates for such a `tr` (`TableHelper`);
  - a recorded run in a paragraph that isn't recorded gets `w:vanish` on its own `rPr`.

  This gives CR-053 D5's granularity:
  - a hidden `span` makes hidden runs in a visible paragraph;
  - a hidden block makes hidden paragraphs, their marks included;
  - `<p><span style="display:none">X</span></p>` leaves an empty visible paragraph, as Word does;
  - `li/p[1]` shares the `li`'s paragraph (`haveMergedFirstP`). An `li` with a hidden first `p` gets
    hidden runs in a visible, numbered paragraph.
- **Applied after the cleansers.** `RPrCleanser` runs as each run is built, so `w:vanish` added
  afterwards is never stripped as "the same as the style".
- **Under every `FormattingOption`.** `w:vanish` here says the content is hidden, rather than
  translating CSS into formatting, so it is applied even under `CLASS_TO_STYLE_ONLY`. Word puts it
  in a style made from the class; we can't, since the importer doesn't create styles (D4).

## 3. The hook

**Recommended shape** (D2): a getter, filled by the last `convert`:

    /** @since 17.3.4 */
    public Map<Element, List<Object>> getHiddenContent()

- **The key** is each outermost marked element that produced content, in document order (a
  `LinkedHashMap`). An element inside a marked one is reported through the outer one, which is the
  dedupe docx4j needs for REQ-902's one finding per outermost element. A revealing element in
  between ends that: a marked element below it is reported as its own key (§2.3).
- **The value** is what that element produced, in document order:
  - its wholly hidden paragraphs and tables, at the top level (not the paragraphs nested inside a
    reported table);
  - its hidden runs that sit in visible paragraphs.
- **An element that produced nothing is absent.** docx4j decides what "holds text" means.
- **The keys are the caller's elements** when `convert(Node)` is given a `Document`.
  - For any other `Node`, the importer lays out a copy (`importNode`), so the keys are the copy's
    elements. The javadoc says so.
  - For the parsing overloads, the keys are in the importer's own tree.
- The map is empty when the option is off, and is cleared at the start of each `convert`.

A getter suits reflection: docx4j calls one method and reads standard types. A listener interface
would have to be implemented through `java.lang.reflect.Proxy` by a caller that can't compile against
this library.

## 4. The table caption (measured, prototyped)

**Today**, the CR-053 repro (a `caption`, a `thead`, a `rowspan` and a `colspan`) gives:

    w:tbl[ w:tblPr(empty), w:tr[w:tc[w:p "CAPTION" jc=center]], w:tbl[w:tblPr, w:tblGrid(3), w:tr ...] ]

The inner `w:tbl` sits directly in the outer one, which is schema-invalid, and the outer table has no
`w:tblGrid`. The variants:
- **A plain caption,** a table in a `div`, in an `li`, after a `p`, or inside a `td`: the same shape.
- **`caption-side: bottom`:** an empty `w:tc` with no `w:p`, and the caption's `w:p` loose in the
  `w:tbl`.
- **A caption of two `p`s:** the second lands before the outer `w:tblPr`.
- **`<table id="t1">`:** two bookmarks named `t1`.

**Cause.**
- openhtmltopdf (`BoxBuilder.reorderTableContent`) wraps a table that has a caption in an anonymous
  `BlockBox`. That box carries the `table` element and `isFromCaptionedTable()`, and its children are
  the top captions, the `TableBox` and the bottom captions, in order.
- The importer takes the wrapper for a "non-TableBox table" (`XHTMLImporterImpl`:1297) and pushes an
  outer `Tbl`. The caption becomes a `w:p` in it.
- At the `TableBox`, `TableHelper.nestedTableHierarchyFix` (:627) moves that `w:p` into a new
  `w:tr/w:tc`, but never makes the `w:tc` the place the inner table goes.
- Its result has been discarded since 0cd7a41 (2013, "Stack of ContentAccessors"): the method became
  `void`, and the `return contentContext;` was commented out. Before that, the output was a valid 1×1
  table holding the caption and the inner table.

**Fix (prototyped; the suite passes, 105/0/0 at the time):**
- treat the wrapper as transparent in `traverseBlockBox`: emit nothing for it, and no bookmark, since
  the `TableBox` bookmarks the same element;
- in `nestedTableHierarchyFix`, return at once when `parent` is a captioned-table wrapper.

The caption then comes out as its own paragraph before the table, or after it for
`caption-side: bottom`. Its paragraph and run properties are unchanged; only its place moves. With
`class="Caption"` it already gets the `Caption` style through the class mapping. Its `jc=center`
comes from the default stylesheet.

Also:
- the dead caption code in `nestedTableHierarchyFix` goes;
- the stale comment at `XHTMLImporterImpl`:1225-1239 is updated;
- `TableHelper.java` has CRLF line endings, which the edit keeps.

**Not this CR:** `<table><table>…</table></table>` and `<table style="display:block">` reach the
same method and give the same invalid shape. That is the same 2013 regression, but not the caption,
and it is not on docx4j's path. It is recorded here for a later CR.

## 5. `data:` images with external resources disallowed

`setExternalResourcesAllowed(false)` admits `data:` images by design. The c8dc938 javadoc says so:
they are part of the document, not fetched.
- openhtmltopdf's own image loading does honour the deny-all controller: `Docx4jUserAgent.getImageResource`
  returns no image for a `data:` URI, so layout never decodes one.
- The importer's `addImage` (:2044) lets a `data:` `src` through, and `XHTMLImageHandlerDefault`
  decodes it itself.
- Measured: a `data:` PNG still gives an image part and a `w:drawing`.

docx4j's hostile-input review notes that a `data:` image bomb therefore reaches the decoder. docx4j
will drop `img` elements, or set a no-op `XHTMLImageHandler`, for its own calls. See D5.

docx4j's phase 0 (CR-053 §10, against c8dc938) measured how far that goes:
- a 25,000² RGBA PNG bomb is not decoded by the default handler: it reads the header only, about
  100 MB of heap;
- a CSS `data:` URI is refused under deny-all;
- a `data:` EPS fails in `Docx4jUserAgent` ("Unrecognized Image format"), so it never reaches
  docx4j's ImageMagick conversion.

## 6. The extras (asked, not required by CR-053)

Each was measured with scratch probes, and each is a separate decision (D7 to D10). No existing test
touches any of them.

### 6.1 An `ol`'s `type` and an `li`'s `value` (D7)

**Today:** `type='a'` gives `decimal`, and `value='7'` gives no restart.
- No layer reads `type`:
  - openhtmltopdf's default stylesheet has no `ol[type]` rule;
  - `XhtmlNamespaceHandler.getNonCssStyling` has no case for lists;
  - `ListHelper` reads only the computed `list-style-type`.
- `value` is never read: `ListHelper.addNumbering(P, Element e, …)` ignores `e`.
- `start` is honoured only where a level is first created (`createLevel`, as `w:lvl/w:start`).
  The restart path hard-codes 1 (`ndp.restart(…, ilvl, 1)`), so a second nested `<ol start='3'>`
  gets `startOverride 1`.

**Change:**
- **`type`:** a `Docx4jNamespaceHandler` (a new file in the renderer package) that maps an `ol`'s
  `type` to `list-style-type` as a presentational hint. `DocxRenderer.setDocument` would use it in
  place of `XhtmlNamespaceHandler`. A hint enters the cascade at author origin, ahead of the author's
  rules, so it beats the default stylesheet's `decimal` and loses to real CSS, as HTML specifies.
  Doing the same in `ListHelper` would override explicit CSS, since computed values can't tell the
  default stylesheet from the author.
- **`value`:** a new `w:num` on the same abstract numbering with `w:lvlOverride/w:startOverride`,
  through docx4j's `NumberingDefinitionsPart.restart`, which `ListHelper` already uses. The current
  num's other level overrides are copied, and later items follow the new num, so they continue 8,
  9…
- **`start` in the restart path** uses the same helper.

About 70 lines. Output changes only for markup that carries these attributes, so on by default.

### 6.2 `h1`-`h6` as heading styles by default (D8)

**Today:** off by default.
- `HeadingHandler` maps `hN` to the style whose name is "heading N", whatever its id, so German's
  "berschrift1" works too. It is built only when `docx4j-ImportXHTML.Element.Heading.MapToStyle` is
  true, and the code's default is false.
- It was added on 2014-08-02 (c310e87) with the property true in a properties file inside the jar.
  Two days later (b5ecf6d) the file left the jar, and only the samples' copy still sets it.

**Measured:**

| Setting | An `h2` |
|---|---|
| Off (the default) | No `pStyle`: bold, `sz` 33, Times New Roman, spacing 180/180 as direct formatting |
| On, `CLASS_PLUS_OTHER` | `pStyle Heading2`, but the default stylesheet's font, colour and size still written as direct formatting over the style |
| On, `CLASS_TO_STYLE_ONLY` | `pStyle Heading2` only |

**Hazards in switching it on by default:**
- `HeadingHandler` never adds a style. `createPackage()` defines only Heading 1 to 4, so `h5` and
  `h6` stay unmapped. docx4j's known `Heading1`-`9` all carry a `numPr` (`numId 3`), which would
  dangle if activated as they are.
- `HeadingHandler` throws a `NullPointerException` on a style with no `w:name`, which is optional.
- **Every user's output changes:**
  - the table of contents and the navigation pane gain entries;
  - `keepNext` changes pagination;
  - a template whose heading styles are numbered numbers the headings.

**Change:**
- a setter, defaulting to the property;
- the null-name guard;
- where no heading style exists, `w:outlineLvl` N-1 set directly, so that navigation works without
  a visible change.

About 30 to 50 lines.

### 6.3 `hr` (D9)

**Today an `hr` is not an empty paragraph.** `getPforHR()` adds Word's own VML horizontal line
(`v:rect o:hr="t"`), which Word and LibreOffice draw as a grey rule. CR-053 §1a saw an empty
paragraph because docx4j's text model drops `w:pict`. The `hr`'s own CSS is ignored.

**A latent defect:** `getPforHR()` returns one static `P`, built once. The same JAXB object, VML id
included, goes into every document that has an `hr`, on every thread. It should return a fresh copy.

**A border instead:** an empty paragraph with a single `w:bottom` border, taken from the `hr`'s CSS,
with an exact small line height. As the importer stands, an empty block with the `hr`'s CSS gets an
inset box on all four sides, so this needs its own branch (about 30 to 40 lines). Word merges
consecutive paragraphs with identical borders, so `<hr/><hr/>` would draw one line.

### 6.4 `convertMHT` through jsoup (D10)

**Today** the part goes to the XML parser. Word's own MHT, which is HTML rather than XHTML, fails
with an unchecked `XRRuntimeException` ("Open quote is expected for attribute http-equiv"), although
the method declares `Docx4JException`. There is no HTML fallback anywhere in the importer, and jsoup
is not a dependency of core.

**Change:** docx4j's pattern.
- jsoup 1.22.1 as an optional dependency, with `requires static org.jsoup;`. It is Java 8 bytecode
  with a Java 11 module descriptor, so it passes the enforcer.
- A package-private `JsoupHtml`, the only class that names jsoup: `Jsoup.parse` with the MIME
  charset or none, then `W3CDom.fromJsoup`.
- A `Class.forName` check for jsoup.
- In `convertMHT`: the XML parse first; on failure, jsoup where present, otherwise a
  `Docx4JException` naming jsoup.

About 50 lines plus the new file; core-tests gain jsoup and a Word-style MHT test. Verified in
scratch: a Word-style MHT part through jsoup and `convert(Node)` gives its six blocks. Its
`p.MsoNormal` rule is honoured, its unclosed `li`s are numbered, and its `o:p` is harmless.
- **On the module path,** `requires static` does not resolve jsoup by itself, so users there need
  `--add-modules org.jsoup`, as for docx4j.
- **docx4j's own reflective `convertMHT`** (its MHT altChunk conversion) gains this too.
- **`convert(String)` and `convert(InputStream)` keep their well-formed contract;** a caller who
  wants HTML can parse it and call `convert(Node)`.

## 7. Found on the way

- **`convert(File)` and `convert(URL)` threw a `NullPointerException` with external resources
  disallowed,** contrary to c8dc938's javadoc. The deny-all user agent refused the document itself
  (`DocxRenderer.loadDocument`).
  - Fixed in b580262: with the option off, the importer reads the document
    itself, as UTF-8, as the user agent does.
  - The fix comes with a test (`ExternalResourcesTest.fileAndURLReadWhenNotAllowed`); the suite is
    106/0/0.
  - This belongs with c8dc938 rather than with this CR.
- **`DocxRenderer(String extraCSS)` ignores its CSS.** The `StyleReference` that would take it is
  commented out (`DocxRenderer`:158-159), so `stylesToCSS()`, the docx's styles as CSS, never reaches
  the cascade. Not this CR.
- **The default stylesheet in force is openhtmltopdf's,** not this project's
  `src/main/resources/XhtmlNamespaceHandler.css`, whose content differs.
  - openhtmltopdf 1.1.31 reads `/resources/css/XhtmlNamespaceHandler.css` from its own jar, and
    `xhtmlrenderer.conf`'s `xr.css.user-agent-default-css` is no longer used.
  - So a nested `ul` gets a disc, although our copy says `ul ul {circle}`.
  - Overriding `getPathToDefaultStylesheet()` would not be safe: the parsed sheet is cached in a
    private static shared across the JVM.
  - `CLAUDE.md` says our copy is in force. Not this CR.
- **`getPforHR()` shares one `P`** across documents and threads (§6.3).
- **Adjacent tables.** The importer writes no paragraph between two adjacent tables, whether visible
  or hidden. Word writes one, and hides its mark when the tables are hidden (round 6). Not this CR.
- **Hostile input** (docx4j CR-053 §10, measured against c8dc938, for callers who convert untrusted
  XHTML). docx4j guards its own calls; nothing here guards anyone else's.
  - Deep nesting overflows the stack inside openhtmltopdf (`CalculatedStyle.valueByName`,
    `BoxBuilder.createChildren`): 900 nested `div`s on a 1 MB stack, and 2,000 through
    `convert(InputStream)`. jsoup caps the depth at about 512.
  - A `rowspan` or `colspan` of 2^31-1 (a 163-byte document) exhausts a 2 GB heap.
  - Floats are quadratic: 50,000 take 41 s.

  docx4j caps the depth and clamps the spans before calling the importer, and adds
  `float:none !important`. A clamp in the importer itself would be a later CR.
- **The "Unable to disable XML External Entities" SEVERE log** comes from openhtmltopdf's identity
  transform picking docx4j's Xalan, because `DocxRenderer` leaves the preferred transformer factory
  unset (:153, commented out). docx4j now parses chunks itself, so its path no longer reaches it. No
  issue is open on the openhtmltopdf fork; danfickle/openhtmltopdf#154 is the closed precedent.

## 8. Phases

### Phase 1: the caption

§4's fix. Tests (core-tests, a new `TableCaptionTest`): the CR-053 repro; a plain caption; a caption
with `caption-side: bottom`; two paragraphs in a caption; `id` on a captioned table (one bookmark); a
captioned table inside a `td`. Each asserts the shape: the caption's paragraph as a sibling of an
ordinary `w:tbl` with a `w:tblGrid`, and no `w:tbl` directly in a `w:tbl`. The suite otherwise
unchanged. CHANGELOG entry. Independent of phase 2; lands first.

### Phase 2: `display: none` as hidden text, and the hook

§2 and §3: the option (D1), the pre-pass, the recording and the post-pass, `getHiddenContent()`.
Tests (a new `DisplayNoneTest`):
- CR-053 §1's chunk (`INLINE_NONE` on a `p`, `CLASS_NONE` in a `div` by class, `SPAN_NONE` in a
  paragraph): with the option off, today's output exactly. With it on: each as hidden text at D5's
  granularity, and three keys in the hook;
- nested `display: none` (one key);
- `!important` in a rule and in the attribute;
- `@media print` applying and `@media screen` not, as openhtmltopdf has it (D3);
- in a `div(none)`: the div's own text hidden; a `display:inline` `span` and a `display:block` `p`
  showing; a `display:inherit` `span` hidden; a `span(none)` inside the `p` hidden again, and its
  own key (D11, round 6);
- `head` and `script` untouched;
- a hidden `li`, `td` and `tr` (shape kept, text hidden; the row `w:trPr/w:hidden`);
- a hidden `table`, a hidden `thead` over a visible `tbody`, and a second `tbody` hidden: every row
  kept, the hidden ones with `w:trPr/w:hidden`;
- a hidden table, a hidden hyperlink, a hidden image;
- an element with no content (absent from the hook);
- the caller's `Document` unchanged afterwards (`style` attributes restored);
- each `FormattingOption`;
- the whole suite unchanged with the option off.

CHANGELOG entry. Then hand docx4j an installed 17.3.4-SNAPSHOT, which CR-053 phase 2 needs.

### Phase 3: the extras

As D7 to D10 decided: `ol type` and `li value` by default; for headings, a setter, the null-name
guard and `w:outlineLvl`, with the default unchanged; a fresh copy of the `hr` rule; `convertMHT`
through jsoup where present. Each with its tests and CHANGELOG entry.

### Phase 4: the release

The version commit (17.3.4: `revision`, `version.docx4j`, the scm tag, the CHANGELOG heading, and the
"since 17.1.1" notes; prepared, awaiting Jason). Then the release, and the docx4j and portfolio
sessions told.

## 9. Decisions (for Jason; each with a recommendation)

All eleven decided as recommended on 20 October 2026 (Jason: "CR-002 accept all recommendations.
commit then implement.").

- **D1. The option.**
  - (a) A setter on the importer, `setDisplayNoneAsHidden(boolean)`, false by default, with
    `isDisplayNoneAsHidden()`.
  - (b) A key in `docx4j-ImportXHTML.properties`.

  **Recommended: (a).** It is per importer, as `setExternalResourcesAllowed` is, and docx4j reaches
  it by reflection.
- **D2. The hook's shape.**
  - (a) `getHiddenContent()`, as §3.
  - (b) A listener interface set on the importer.

  **Recommended: (a),** for reflection's sake (§3).
- **D3. Which medium and selectors decide `display`.** Measured: Word applies no `@media` rule at
  all, and ignores some selectors (§2.1).
  - (a) openhtmltopdf's cascade as it stands (medium `print`). A caller who wants Word's rules
    rewrites the `style` elements first, as docx4j will (CR-053 §10.7).
  - (b) The pre-pass imitates Word: its own `Matcher` with the `@media` blocks and Word's ignored
    selectors removed, for `display` alone.

  **Recommended: (a).** docx4j imposes Word's rules on its own chunks before calling. (b) would put
  a second, Word-specific cascade inside the importer, for callers who have not asked for one.
- **D4. `w:vanish` under `CLASS_TO_STYLE_ONLY`.**
  - (a) Always, when the option is on.
  - (b) Only where direct formatting is allowed.

  **Recommended: (a).** Hiding is what the content is, not a CSS translation, and docx4j needs it
  whatever the formatting option.
- **D5. `data:` images with external resources disallowed.**
  - (a) As now: admitted. The javadoc names the no-op image handler as the way to drop every image.
  - (b) `setExternalResourcesAllowed(false)` refuses them too.
  - (c) A separate limit, such as a maximum decoded size for a `data:` image.

  **Recommended: (a).** A `data:` image is content, not a fetch, and (b) would change c8dc938's
  documented contract before it ships. docx4j guards its own calls; (c) can follow if anyone asks.
- **D6. The caption's style.**
  - (a) As the fix gives it: a paragraph with the caption's own properties (centred by the default
    stylesheet; the `Caption` style only through `class="Caption"`).
  - (b) The `Caption` style by default, with `keepNext` for a top caption.

  **Recommended: (a) now.** How Word's own HTML import treats `caption` is unmeasured. (b) can
  follow a Word probe.
- **D7. `ol type` and `li value` (§6.1).**
  - (a) In phase 3, on by default.
  - (b) Not in 17.3.4.

  **Recommended: (a).** It is standard HTML, and it changes only markup that carries the
  attributes.
- **D8. Headings (§6.2).**
  - (a) Default unchanged; add a setter, the null-name guard, and `w:outlineLvl` where no heading
    style exists.
  - (b) As (a), with the default flipped to on.
  - (c) Not in 17.3.4.

  **Recommended: (a).** Flipping the default changes every user's headings, their TOC and their
  pagination, which is a change for a major version with a release note, not a point release.
- **D9. `hr` (§6.3).**
  - (a) Keep the VML rule, which Word draws, and return a fresh copy each time.
  - (b) A bottom border instead, by default.
  - (c) A bottom border behind an option.

  **Recommended: (a).** The fresh copy is a fix; the rule is already a rule. docx4j's text model
  can recognise `o:hr` if it wants to show a rule.
- **D10. `convertMHT` through jsoup (§6.4).**
  - (a) In phase 3: the XML parse first, then jsoup where present.
  - (b) Not in 17.3.4.

  **Recommended: (a).** Only input that fails today changes, and a typical MHT then converts.
- **D11. An explicit `display` undoes an ancestor's `display: none` for its content** (§2.3). In
  Word, a `display:block` `p` inside a `display:none` `div` shows, while a plain `p` inside one is
  hidden (Jason's probe, CR-053 §10.6).
  - (a) Follow Word: an element whose `display` comes from an author rule or its `style` attribute,
    with any value but `none` or `inherit`, ends its ancestors' hiding.
  - (b) Hide everything under a marked element.

  **Recommended: (a),** as Word does. Round 6 (CR-053 §10.6) measured it inside a `display:none`
  `div`:
  - a `display:inline` `span` shows;
  - a `display:inherit` `span` stays hidden;
  - a `display:block` `p` shows, and a `display:none` `span` inside it is hidden again.

## 10. Risks

- **Phase 1 changes output** for every captioned table: from an invalid nested table to a paragraph
  and a table. Word most likely repaired the old shape, and no test covered it.
- **Phase 2's re-shown elements are laid out as their default display,** which can differ from what
  the author's losing rule said (§2.2).
- **Lists and tables move** when hidden items are re-shown. The text is right, the numbering and grid
  are Word's to confirm.
- **The pre-pass edits the caller's `Document`** for the length of the convert; it is restored in a
  `finally`, but another thread reading that `Document` meanwhile would see the edits.

## 11. Implementation notes (2026-10-20)

As §8, with these differences and findings:

- **Phase 1.** As prototyped. `nestedTableHierarchyFix` keeps a TODO for `<table><table>`, which
  still gives a `w:tbl` directly inside a `w:tbl` (§4, a later CR).
- **Phase 2: a shared paragraph.** The importer lets a block's first child block use the block's
  paragraph (`attachmentPointP` is only reset when a block ends), so `<body><div none><p>x</p>`
  writes into the body's paragraph. A paragraph therefore belongs to the last block that takes it
  over (`getCurrentParagraph(true)`) before it has a run. An `li`'s merged first `p` doesn't take
  it over, so it keeps the hidden-runs-in-a-numbered-paragraph shape of §2.3.
- **Phase 2: what records what.**
  - A paragraph is recorded against the owner of the enclosing block, not of the box that caused
    it: an `img` or an inline box can create a `td`'s paragraph lazily.
  - The `img` box, which the importer doesn't end a paragraph after, doesn't count as a block.
  - Runs and paragraphs with no hidden owner are recorded too. A run an author shows again
    inside a hidden paragraph stays visible, and that paragraph's mark is hidden without the
    paragraph being reported whole.
  - Anything unrecorded (a cell, a field, a dummy merged cell's paragraph) takes its parent's
    owner.
  - The cell-closing `w:p`, the `hr` paragraph and the math paragraph are recorded as well.
    Equations get `w:rPr/w:vanish` in each `m:r`.
- **D9's fresh `hr` copy came with phase 2,** since a hidden `hr` gets `w:vanish` on the shared
  object otherwise.
- **Phase 3, `Docx4jNamespaceHandler`:** openhtmltopdf reads its default stylesheet with
  `getClass().getResourceAsStream`.
  - For a subclass in another JPMS module (the suite runs on the module path) that finds nothing,
    and the failure is cached in a static for the JVM, so every later convert loses the default
    stylesheet.
  - The subclass hands `getDefaultStylesheet` to a stock `XhtmlNamespaceHandler`.
- **Phase 3, `li value`:** an override can now be a `startOverride` alone. `addNumbering`
  assumed every override had a `w:lvl`, and is guarded.
- **docx4j's numbering cache** (found by docx4j-25 after an `li value`; older than this CR).
  - `NumberingDefinitionsPart` makes each `w:num`'s definition from its abstract's levels as
    they are then, but `ListHelper` adds the levels afterwards.
  - So docx4j's in-memory numbering had no levels for the lists an import added, until something
    rebuilt the maps. `restart()` does that once, through `getEmulator()`, which is why an
    `li value` showed it: the lists after the restart lost their labels.
  - The importer now calls `initialiseMaps()` at the end of any convert that changed the
    numbering (`ListTypeAndValueTest.docx4jsNumberingModelSeesEveryList`).
- **D8:** `w:outlineLvl` applies only with heading mapping on, where no "heading N" style exists.
  Setting it with mapping off would change every user's TOC and navigation pane, which D8 keeps
  off by default.
- **D10:** the availability check is in `XHTMLImporterImpl` (a class name as a string), so
  `JsoupHtml` is loaded only when jsoup is there.
- **The snapshot is 17.1.1-SNAPSHOT,** the pom's `revision`. 17.3.4 is phase 4's version commit.
- **For docx4j (told):** `Jsoup.parse(String)` keeps a leading U+FEFF as text. The `head` then
  closes early, its `style` elements land in the `body`, and openhtmltopdf ignores them there.
  Chunks docx4j writes may now start with a BOM.
