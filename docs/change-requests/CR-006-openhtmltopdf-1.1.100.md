# CR-006: openhtmltopdf 1.1.100: colours with alpha, thead's keepLines, and the move

Status: ACCEPTED 2026-10-24 (Jason: "D5 (b); others as recommended"): all four phases in 17.3.5,
and D1 to D4 and D6 as recommended. Proposed the same day, at Jason's request ("yes" to "draft
CR-006 for openhtmltopdf"), after the 17.3.4 dependency review kept openhtmltopdf at 1.1.31
(Appendix A). Drafted by Claude Opus 5.5 in the docx4j-ImportXHTML session, with each finding
measured first (§1). The doubled `w:br` it found was fixed outside this CR, for 17.3.5 (§1, row 1).
Phases 0 to 3 implemented 2026-10-24 (suite 178/0/0, BundleIT 3/0/0; §5), commit 6da82a2. Phase 1
needs 1.1.100's alpha, so the move came with it. The release is 17.3.5's. Phase 3 amended
2026-10-30 (§5; Jason: "move to 1.1.103"): openhtmltopdf 1.1.103, for 1.1.102's fix, by D6's own
rule, commit 4486504. The review's notes, `docs/dependencies-status.md`, were folded into this CR
on 2026-11-01 (§1, §4, phase 3 and Appendix A), and the file deleted (Jason: "fold the relevant
parts of it into CR-006 and update CR-006 references so that docs/dependencies-status.md can be
deleted").
Scope:
- colour handling: `TableHelper` and `ParagraphBorderHelper` (border colours), and the CSS
  values the importer hands to docx4j's `PropertyFactory` (text colour and shading);
- `page-break-inside` from openhtmltopdf's default stylesheet;
- -core's openhtmltopdf version;
- tests in core-tests, the CHANGELOG, and the dependency review's notes (now Appendix A).

Branch: `VERSION_17_3_5`. Phases (§5): 0. a wider measurement; 1. colours; 2. the default
stylesheet's `page-break-inside`; 3. the move.

## 1. Measured (2026-10-23 and 24)

Each input was converted on 1.1.31 and on 1.1.100, with the same -core, and the main document
and numbering parts compared. The probes are in
`~/claude-scratch-archive/docx4j-ImportXHTML/dependency-bumps-2026-10-23/` (its `README.txt` says
how each was run), and the follow-ups of 24 October in its `followups-2026-10-24/`. The inputs:
- ten documents: two from core-tests (`numbering/*.html`), seven from the samples' `sample-docs`,
  and `css-mix.xhtml`, written for this (margins, borders, units, lists, a table with a thead,
  `pre`). The output of seven of them changes;
- `br.xhtml`, five `<br/>` in paragraphs, a `div` and a table cell;
- `colours.xhtml`, for rows 3 to 7, on 24 October.

In a copy of 24439a2 (`git archive`) with openhtmltopdf at 1.1.100 and mime4j at
0.8.15 together, `mvn clean verify` passed: the enforcer (bytecode 11, dependency convergence),
the suite at 166/0/0, and `BundleIT` at 3/0/0.

| # | input | 1.1.31 | 1.1.100 |
|---|---|---|---|
| 1 | `<br/>` | one `w:br` | two `w:br`; **fixed for 17.3.5** (`LineBreakTest`) |
| 2 | a thead's paragraphs | no `w:keepLines` | `w:keepLines` on each (15 in three inputs) |
| 3 | `color: rgba(200,0,0,0.5)`, `hsl()`, `hsla()` | dropped | applied, alpha ignored |
| 4 | `background-color: rgba(0,0,0,0)`, on a run or a cell | dropped | `w:shd w:fill="000000"`: black |
| 5 | `background-color: rgba(255,255,0,0.4)` | dropped | `w:fill="ffff00"`: full yellow |
| 6 | `border: 1px solid rgba(0,0,255,0.5)` | no border | `w:color="rgba(0, 0, 255, 0.5)"`: schema-invalid |
| 7 | `border: 1px solid transparent` | `w:color="transparent"`: schema-invalid | the same |
| 8 | row heights, cell widths | | ±15 to 75 twips, ±7 twips |

- **Row 1's cause:** openhtmltopdf's default stylesheet has `br:before { content: "\A"; }`. From
  1.1.97, that content is laid out inside the br's own `InlineBox`, so the line break splits the
  box in two. On 1.1.31 each br box starts and ends in place; on 1.1.100 each br gives a pair of
  boxes, one where it starts and one where it ends, and `processInlineBoxContent` gave a `w:br`
  for each. `br.xhtml` had 5 breaks on 1.1.31 and 10 on 1.1.100; the soft return in
  `sample-docxv2.html` was doubled, and `Image-Samples.html` went from 2 to 4. The importer now
  makes the `w:br` only where the box starts, which changes nothing on 1.1.31. `LineBreakTest`
  checks for one `w:br` per `<br/>`: without the fix it fails on 1.1.100, with 10 instead of 5.
- **Row 2's cause:** 1.1.79 adds `page-break-inside: avoid` to `thead` in that stylesheet. docx4j's
  `KeepLines` is that property. `page-break-inside` isn't inherited in CSS, and how the thead's
  value reaches the th paragraphs wasn't traced.
- **Rows 4 and 5:** docx4j's `PropertyFactory` skips only the text `transparent` (lines 655 and
  735). `RShading`, `PShading` and `Shading` take the red, green and blue, and nothing else.
- **Rows 6 and 7:** `TableHelper` (line 521) and `ParagraphBorderHelper` (line 211) write
  `borderColor.asString()` into `w:color`, dropping only a leading `#`. `ST_HexColor` allows
  `auto` or three hex bytes. What Word does with either value wasn't tested.
- **Row 7 is in the released 17.3.4.** It doesn't need the move.
- **Row 8** fits the release notes' font-metric and `line-height` changes (1.1.58, 1.1.67).
- **Which default stylesheet is read:** openhtmltopdf's own, from its jar (CR-002 §7). -core's
  copy and `xhtmlrenderer.conf` were deleted for 17.3.5. Overriding the path isn't safe, because
  the parsed sheet is cached in a static shared across the JVM. So row 2 can't be fixed in a
  stylesheet.

Not measured; from the release notes, between 1.1.32 and 1.1.100:
- `var()` and `:root` (1.1.75);
- `initial` and `unset` (1.1.89);
- selector fixes for a class prefix and `nth-child` (1.1.39, 1.1.82);
- case-insensitive font-family matching (1.1.54);
- generated content laid out inside its element's box (1.1.97), which is row 1's cause and may
  reach other generated content: `q`'s quotes, list markers.

## 2. Colours

All of them go through one rule, in a new class in -core. It turns a cascaded colour into a hex
colour, or into "none":

- **Alpha 0** (`transparent`, or `rgba(…, 0)`) is none. For text colour and shading, the
  property is left out, as `transparent` is today. For a border, see D3.
- **Alpha 1** is the hex colour, as now.
- **Alpha between** is D2.

It applies in three places:
- **Text colour and shading:** before the cascaded values reach docx4j's `PropertyFactory`, so
  docx4j isn't changed. That covers `color` and `background-color` on runs, paragraphs and cells.
- **`TableHelper`'s and `ParagraphBorderHelper`'s border colours,** which then can't be anything
  but hex or none.
- **The CSS text `transparent`** goes through the same rule, which fixes row 7 on 1.1.31 too.

## 3. thead's keepLines

- **(a)** Accept it. Header rows ask Word to keep each cell's paragraph on one page, which seldom
  matters.
- **(b)** Honour `page-break-inside` only where the document's own CSS sets it: a style attribute,
  a style element or a linked stylesheet. `DisplayNoneHelper` already tells the author's
  `display` from the default stylesheet's (CR-002 §2), and the same walk can answer for
  `page-break-inside`.
- **(c)** Leave `w:keepLines` out of paragraphs inside table cells.

## 4. The move

-core's `openhtmltopdf-core` and `-pdfbox` go to the version phase 0 measures (D6): 1.1.100, if
phase 0 starts now. openhtmltopdf publishes a release for each merge to its `main` (§7), so 1.1.32
to 1.1.99 are all on Central; 1.1.31 is from 8 September 2025, and 1.1.100 from 22 October 2026.
The CHANGELOG names rows 3 and 8, and any of the not-measured list that phase 0 finds.

Otherwise the move is safe:
- **PDFBox:** 1.1.100 declares 3.0.8, the version -core pins, where 1.1.31 declared 3.0.3, which
  -core overrides.
- **Java and modules:** still Java 8 bytecode. There's no `Automatic-Module-Name`, so the module
  names stay `openhtmltopdf.core` and `openhtmltopdf.pdfbox`.
- **API:** a `javap` comparison of the 71 types -core imports found additions only.
  `OutputDevice` gains an abstract `paintBoxShadow`, which `AbstractOutputDevice` implements.
- **Security:** nothing pressing. The only security item in the range is the PDFBox 3.0.8 bump,
  for CVE-2026-33929, which is in PDFBox's examples module, not the library.

## 5. Phases

### Phase 0: a wider measurement

Rerun the comparison on 1.1.31 and the target version, with more inputs:
- the inputs of every core-tests test;
- the samples;
- documents for each item on §1's not-measured list;
- generated content: `q`, `::before` and `::after`, and list markers;
- a Word "Save as Web Page" document through `convertMHT`.

Record what moves, here.

**Done 2026-10-24** (on e80fdb9; probes in the archive's `cr006-phase0-2026-10-24/`):
- **The suite's converts.** Two `git archive` copies, at 1.1.31 and at 1.1.100, each had a
  temporary hook, never committed, that wrote each convert's WML. The suite ran 169/0/0 on both,
  with 277 converts each. 21 of the 277 move:
  - 21 header-row paragraphs gain `w:keepLines` (row 2; phase 2);
  - 7 `w:trHeight` change (row 8).

  Nothing else moves. `XHTMLtoPPTX` doesn't lay out with openhtmltopdf, so it isn't in these.
- **The extra inputs:**
  - CSS features: `:root` and `var()` with a fallback, `initial`, `unset`, `[class^=…]`, `.a.b`,
    `nth-child`, font-family case, `white-space: pre-line`, `text-transform`, `line-height`;
  - generated content: `q`, `::before`, `::after`, `counter()`, `list-style-type`, `a::after`,
    and a `br` inside an inline element;
  - the samples, as in §1;
  - through `convertMHT`, a Word "Save as Web Page" document and the HTML5 edge cases of
    Appendix A.3's jsoup check.
- **Gains on 1.1.100,** which 1.1.31 dropped, for the CHANGELOG:
  - `var()` and its fallback apply, to colour and margins;
  - `font-weight: initial` and `text-align: initial` reset, as CSS says.
- **Unchanged:**
  - the selectors and font-family case;
  - generated content. Neither version imports `::before` or `::after` content, `q`'s quotes or
    counters, so 1.1.97's change reaches only the `br` (row 1);
  - the Word document through `convertMHT`, apart from its row heights.
- **So D6's version is 1.1.100.** The colours of rows 3 to 7 are phase 1's.

### Phase 1: colours

As §2, with D2 and D3, and tests for rows 4 to 7, on 1.1.31 and on the target version. Row 7's
fix doesn't depend on the move, so this phase can land first.

**Done 2026-10-24** (commit 6da82a2). It differs from the plan in one way:
- **It needs 1.1.100.** `FSRGBColor.getAlpha()` is new in 1.1.100. In 1.1.31,
  `FSRGBColor.TRANSPARENT` is (0, 0, 0), the same as black. So -core's pins moved with this
  phase rather than in phase 3, and the tests run on 1.1.100 only. D5 (b) ships all four phases
  together, so the release isn't affected.

What was done:
- **`CssColorForWord`** (new, public, `@since 17.3.5`, LGPL header):
  - `opaque(FSColor)` returns null at alpha 0, the colour at alpha 1, and partial alpha blended
    over white (D2), rounding half up;
  - `borderColor(FSDerivedValue)` returns hex without the '#', or null where the colour is
    transparent: the identifier `transparent`, or alpha 0.
- **Where it applies:**
  - the colour branch of `getCascadedProperties`, in `XHTMLImporterImpl` and in `XHTMLtoPPTX`. A
    colour that is none is left out of the map, so docx4j's `PropertyFactory` never sees it;
  - `TableHelper`'s cell background;
  - `TableHelper`'s and `ParagraphBorderHelper`'s border colours. A null gives a
    `w:val="nil"` border (D3); the old comments about `transparent` are gone.
- **pptx:** docx4j's `FontColor.set(CTTextCharacterProperties)` is a TODO, so a pptx run gets no
  text colour, before or after this phase.
- **`ColorAlphaTest`** (new, 5 tests): none where transparent, on a run, a paragraph and a cell;
  partial alpha (`e48080`, `ffff99`, `ff8080`); opaque colours unchanged; every `w:color` is
  `ST_HexColor`, with `8080ff`, `ff8080` and `nil`; and pptx converts.
- **Measured on 1.1.100 without this phase:** 3 of `ColorAlphaTest`'s 5 fail. The probe's
  output, `colours.xhtml`:
  - `E48080`, `008000` and `B3B3FF` for the text colours;
  - `FFFF99` for the paragraph, `FF0000` for the hsl cell, and no shading where transparent;
  - `8080FF` and `nil` for the borders.

### Phase 2: the default stylesheet's page-break-inside

As §3, with D1.

**Done 2026-10-24** (commit 6da82a2):
- **`XHTMLImporterImpl.addParagraphProperties`** skips `page-break-inside` unless
  `isAuthorPageBreakInside` says so.
- **How that is decided:** the nearest element, the paragraph's own or an ancestor's, with a
  cascaded `page-break-inside` other than `inherit` decides. That declaration must have
  `StylesheetInfo.AUTHOR` as its origin, the same test `DisplayNoneHelper` makes for `display`.
- **Anonymous inline boxes:** a paragraph made from one, such as a th's text, has no element of
  its own. So the traversal notes the element whose inline content it is processing
  (`inlineOwner`), and the walk starts there.
- **`PageBreakInsideTest`** (new, 4 tests):
  - none from the default stylesheet's thead rule;
  - the author's, from a style element and from a style attribute;
  - the author's, inherited from a div, and from an author thead rule;
  - an author's `auto` on th, which wins over the default stylesheet's thead rule.
- **Measured:** the 21 `w:keepLines` of phase 0 are gone from the suite's converts.

### Phase 3: the move

As §4. The CHANGELOG, the dependency review's notes (folded into this CR since), and the release
(D5).

**Done 2026-10-24** (commit 6da82a2), apart from the release, which is 17.3.5's:
- **The pins:** `openhtmltopdf-core` and `-pdfbox` at 1.1.100 (D6), moved with phase 1.
- **Measured** with `mvn -o clean verify -Dgpg.skip`: all five modules green. The suite is
  178/0/0, with nine new tests in this CR and `LineBreakTest`, and `BundleIT` is 3/0/0. The
  enforcer passes, with bytecode 11 and dependency convergence.
- **Phase 0's comparison, rerun.** The suite's 277 converts, at 1.1.31 against 1.1.100 with
  phases 1 and 2, differ in 7 `w:trHeight` only (row 8). Of the extra inputs:
  - `css-features` gains `var()` and `initial`;
  - `colours` is as phase 1 says;
  - row heights and widths move as in row 8;
  - the rest is the same.

  Every `w:color` in every output is hex.
- **Still true on 1.1.100:** the README's fetch timeouts (10 s and 30 s, the same constants)
  and its nesting note. With `-Xss1m`, 825 nested divs convert on both versions.
- **The CHANGELOG** (17.3.5) and the dependency review's notes (folded into this CR since) are
  updated.

**Amended 2026-10-30: 1.1.103** (Jason: "move to 1.1.103"; commit 4486504). D6 takes a newer release
"only for a reason, such as a fix", with phase 0 rerun on it. With 17.3.5's release near, Jason
asked whether 1.1.101 to 1.1.105 (27 to 29 October) hold a fix we need. One does: 1.1.102's
(openhtmltopdf issue 258). Text with `word-wrap` or `overflow-wrap: break-word` that fits only
when broken between characters made 1.1.100 throw an `AssertionError` from
`LineBreakContext.checkConsistency` when Java assertions are enabled, as Maven's Surefire and
Failsafe enable them. Without assertions the layout is the same. Through ImportXHTML with `-ea`,
the issue's reproducer threw on 1.1.100 and converted on 17.3.4 (1.1.31), so it would have been a
regression in 17.3.5. The other releases don't reach what the importer writes:
- 1.1.101: kerning, off unless a document sets `font-kerning: normal`;
- 1.1.103: a bottom margin added twice for a block moved to the next page; the importer doesn't
  read block positions;
- 1.1.104: trailing spaces hang at a line end, and line alignment and line heights follow CSS;
  the importer writes no laid-out line or row height (CR-007);
- 1.1.105: a block image centred by `margin: auto`; the importer doesn't read its position.

1.1.103 is the smallest step with the fix, so 1.1.104's line-layout changes and 1.1.105 are left
out.
- **The pins:** `openhtmltopdf-core` and `-pdfbox` at 1.1.103. It declares the same dependencies
  as 1.1.100 (PDFBox and xmpbox 3.0.8, graphics2d 3.0.1) and the same licence, LGPL 2.1 or later,
  so `legals/NOTICE` is unchanged.
- **Phase 0's comparison, rerun** on the current branch, 1.1.100 against 1.1.103: the suite's 380
  converts are byte-identical.
- **`BreakWordTest`:** the issue's reproducer, which skips if openhtmltopdf's assertions are off.
  It fails on 1.1.100 and passes on 1.1.103.
- **The CHANGELOG** entry names 1.1.103 and why. Its sentence on row heights moving is gone,
  since CR-007 writes no laid-out row height.
- **Newest on Central** that day: openhtmltopdf 1.1.105; pdfbox 3.0.8, apache-mime4j-core 0.8.15
  and jsoup 1.23.2, as pinned.
- Probes: `~/claude-scratch-archive/docx4j-ImportXHTML/ohtp-1.1.101-105-2026-10-30/`.

## 6. Decisions (for Jason; each with a recommendation)

Decided 2026-10-24 (Jason: "D5 (b); others as recommended"):
- D1 (b), the author's `page-break-inside` only;
- D2 (b), partial alpha blended over white;
- D3 (a), no border for a border whose colour is none;
- D4: docx4j-25 is sent rows 4 and 5. Done the same day. At Jason's word, docx4j's
  `PropertyFactory` now applies the same rule, alpha 0 as none and partial alpha over white, for its
  other callers (docx4j 7f27c9f33, `PropertyFactoryCssAlphaTest`). Against that snapshot this
  suite is still 178/0/0, and ImportXHTML's output is unchanged, since §2 filters first;
- D5 (b), all four phases in 17.3.5;
- D6 (a), the version phase 0 measures, 1.1.100. 1.1.103 from 2026-10-30 (Jason: "move to
  1.1.103"), for 1.1.102's fix, as D6 allows (§5, phase 3).

- **D1. thead's `keepLines` (§3).** (a) accept; (b) only the author's `page-break-inside`; (c) none
  in table cells.

  **Recommended: (b).** It keeps what the author asked for, and nothing the default stylesheet
  adds. It is also how the importer already treats `display: none`.
- **D2. Partial alpha.**
  - (a) Ignore it, as 1.1.100 does.
  - (b) Blend over white, so `rgba(255,255,0,0.4)` gives `ffff99` and `rgba(200,0,0,0.5)` gives
    `e48080`.
  - (c) Treat it as none below 0.5, and as the colour from 0.5.

  **Recommended: (b).** It is what a reader sees on a white page, which most documents have. On a
  coloured background it is wrong either way.
- **D3. A border whose colour is none.**
  - (a) No border (`w:val="nil"`).
  - (b) Keep the border, coloured `auto`.

  **Recommended: (a).** A transparent CSS border draws nothing. In Word, a cell border takes no
  layout space of its own.
- **D4. docx4j's `PropertyFactory`.** Whether docx4j should also treat alpha 0 as transparent is
  docx4j-25's question, for docx4j's other callers. ImportXHTML doesn't wait for it, because §2
  filters before `PropertyFactory`.

  **Recommended:** send docx4j-25 rows 4 and 5 once D2 is decided.
- **D5. The release.**
  - (a) Phase 1 in 17.3.5, and phases 2 and 3 when ready.
  - (b) All in 17.3.5.
  - (c) All in the release after.

  **Recommended: (a).** Row 7 is a schema-invalid document in the released 17.3.4, so its fix
  shouldn't wait for the move.
- **D6. The target version.**
  - (a) The version phase 0 measures: 1.1.100, if phase 0 starts now.
  - (b) The newest when phase 3 starts, at least two weeks old.

  **Recommended: (a).** Phase 3 then ships exactly what was measured, and nothing is compared
  twice.
  - **The releases that bear on us are 1.1.95 to 1.1.97** (9 October), which change line
    breaking and inline layout; 1.1.97 is the one that split the br. By 24 October they were 15
    days old.
  - **The later ones barely touch the importer.** 1.1.98 and 1.1.99 (22 October) are footnotes
    and multi-column pagination, and 1.1.100 is PDFBox 3.0.8, which -core already pins.
  - **No sign of instability.** None of the releases from 1.1.89 to 1.1.100 reverts or hastily
    fixes an earlier one.
  - **A newer release is taken only for a reason**, such as a fix for something phase 0 finds,
    and then phase 0 is rerun on it.

  (Revised 2026-10-24 at Jason's request, "change D6's recommendation to 'ship the version phase 0
  measures', which would be 1.1.100". It had recommended (b), because a release comes with each
  merge and 1.1.100 was a day old when measured. The two-week margin was a rule of thumb, not
  something the release history showed.)

## 7. Risks

- **Phase 0 finds more than §1 lists.** The release notes cover 69 releases, and §1 measured ten
  documents and two probes.
- **Blending (D2 (b)) is a guess at the page.** A document on a dark background gets colours lighter
  than its author saw.
- **The versions move quickly.** With one release per merge, newer versions will be out by
  phase 3. Under D6 (a), phase 3 still ships the version phase 0 measured. If a newer one is taken
  for a reason, phase 0's comparison is rerun on it.
- **Word not checked.** Neither `w:color="transparent"` nor rows 4 to 6 were opened in Word.

## 8. Sources

- The archive's `dependency-bumps-2026-10-23/README.txt` and its `followups-2026-10-24/README.txt`.
- openhtmltopdf:
  - releases: https://github.com/openhtmltopdf/openhtmltopdf/releases
  - the release-per-merge workflow:
    https://github.com/openhtmltopdf/openhtmltopdf/blob/main/.github/workflows/release.yml
  - the PDFBox 3.0.8 bump: https://github.com/openhtmltopdf/openhtmltopdf/pull/256
  - versions and poms: https://repo1.maven.org/maven2/io/github/openhtmltopdf/openhtmltopdf-core/
  - its `XhtmlNamespaceHandler.css` and `XhtmlCssOnlyNamespaceHandler` at 1.1.31 and 1.1.100.
- PDFBox security (CVE-2026-33929, examples module only): https://pdfbox.apache.org/security.html
- docx4j's `model/properties/PropertyFactory.java`, `run/RShading.java`,
  `paragraph/PShading.java` and `table/tc/Shading.java`.
- This repository: `XHTMLImporterImpl.processInlineBoxContent`, `TableHelper`,
  `ParagraphBorderHelper`, `DisplayNoneHelper`, and CR-002 §2 and §7.
- For Appendix A:
  - mime4j's changelog,
    https://github.com/apache/james-mime4j/blob/apache-mime4j-project-0.8.15/CHANGELOG.md; the
    changes,
    https://github.com/apache/james-mime4j/compare/apache-mime4j-project-0.8.13...apache-mime4j-project-0.8.15;
    and `MimeConfig` at 0.8.15,
    https://github.com/apache/james-mime4j/blob/apache-mime4j-project-0.8.15/core/src/main/java/org/apache/james/mime4j/stream/MimeConfig.java
  - jsoup's releases, https://github.com/jhy/jsoup/releases, and its advisories,
    https://github.com/advisories/GHSA-65r4-943x-97jj and
    https://github.com/jhy/jsoup/security/advisories/GHSA-pmhh-3w7g-xqp8
  - this repository's `XHTMLImporterImpl.convertMHT`, `JsoupHtml` and -core's `pom.xml`;
    docx4j's `docx4j-core/src/main/java/org/docx4j/model/text/JsoupHtml.java`, and the jsoup and
    fontbox pins in its parent `pom.xml`.

## Appendix A. The 17.3.4 dependency review

This CR came out of a review of -core's dependencies for the 17.3.4 release, on 23 October 2026
(`VERSION_17_3_4` at 24439a2). Jason had run Maven's versions plugin, which listed four newer
versions, and asked "please determine whether we should bump to some or all of these updates".
Claude Opus 5.5 measured them in the docx4j-ImportXHTML session, and read the release notes and
advisories the same day (sources in §8). The notes were `docs/dependencies-status.md` until
2026-11-01, when they were folded in here: openhtmltopdf's into §1, §4 and phase 3, the rest
below. The probes are in the archive's `dependency-bumps-2026-10-23/`.

### A.1 What was decided

None of the four went into 17.3.4. All of them went into 17.3.5.

| dependency | 17.3.4 | 17.3.5 | why it waited |
|---|---|---|---|
| `io.github.openhtmltopdf:openhtmltopdf-core`, `-pdfbox` | 1.1.31 | 1.1.103 (this CR) | one `<br/>` gave two `w:br` (§1, row 1) |
| `org.apache.james:apache-mime4j-core` | 0.8.13 | 0.8.15 (A.2) | an MHT of more than 512 parts failed |
| `org.jsoup:jsoup` (optional) | 1.22.1 | 1.23.2 (A.3) | docx4j 17.3.4 pinned 1.22.1, and neither advisory reaches either project |

The versions plugin listed nothing else. pdfbox, xmpbox and fontbox were at 3.0.8; docx4j 17.3.4
pins fontbox 3.0.8, and docx4j-bundle holds fontbox and pdfbox-io at 3.0.8.

### A.2 mime4j 0.8.13 → 0.8.15

0.8.14 is from 21 April 2026, and 0.8.15 from 3 September 2026. Neither fixes a CVE: NVD's newest
mime4j entry is CVE-2024-21742, fixed in 0.8.10. 0.8.15 adds hardening against denial of
service, which fits CR-005: limits on the number of parts, on nesting, on the total header count
and on total header size, and MIME parameters accumulated linearly rather than quadratically.
`MimeConfig`'s defaults are 512 parts, nesting 64, 16,384 headers and 1 MiB of headers.

- **Why it waited:** `convertMHT` read the whole MHT, with `new MimeStreamParser()` and no
  `MimeConfig`, though it uses only the first text/html part. With 513 or 600 parts, 0.8.15
  threw `Docx4JException: Maximum part count limit (512) exceeded`, from
  `MaxPartCountLimitException`; 0.8.13 converted them. A Word MHT holds a part for each image
  file, plus a few of its own, so a long illustrated document could pass 512.
- **Otherwise safe:**
  - the suite, with its `convertMHT` tests, passed on 0.8.15 (§1's build);
  - `Field` gained an abstract `getSafeRaw()` in 0.8.14 (MIME4J-333), which breaks only
    implementers, and -core only reads fields. `MimeStreamParser`, `ContentHandler` and
    `BodyDescriptor` are unchanged;
  - still Java 8 bytecode, and the module is still `apache.mime4j.core`;
  - only -core uses it. docx4j-bundle doesn't hold it, and our bundle shades it.
- **Moved for 17.3.5** on 24 October, at Jason's word ("yes"), as a small change rather than a CR
  (da56ff5):
  - `convertMHT` stops parsing once the text/html part is read. Its handler, an anonymous
    subclass of `MHTContentHandler` inside `convertMHT`, throws a private `MimeException`
    subclass, which `convertMHT` catches. Code that uses the public class with its own parser is
    unaffected.
  - mime4j's own `stop()` doesn't do this. `MimeEntity.stopSoft()` drops only what is buffered,
    and the parser refills. On 0.8.15 it stopped after one body on 10 parts, but went on to the
    limit, 512 bodies, on 513 and 5,000. Throwing stopped after one body on 10, 513 and 5,000
    parts, on 0.8.13 and 0.8.15.
  - The limits still hold for whatever comes before the HTML part, and for its headers.
  - `MHTImportTest`: `manyPartsAfterTheHtml`, 1,000 image parts after the HTML, converts, and
    without the fix it fails on 0.8.15; `tooManyPartsBeforeTheHtml`, the same parts before the
    HTML, gives a `Docx4JException`.
  - Suite 169/0/0, `BundleIT` 3/0/0. The bundle holds mime4j 0.8.15.

### A.3 jsoup 1.22.1 → 1.23.2

- **Two advisories affect 1.22.1:**
  - GHSA-65r4-943x-97jj (CVE-2026-75140), High, fixed in 1.23.2. Deeply nested, uniquely
    namespaced XML makes `XmlTreeBuilder` quadratic in time and memory.
  - GHSA-pmhh-3w7g-xqp8 (CVE-2026-71497), Moderate, fixed in 1.23.1. A `Cleaner` whose custom
    `Safelist` permits raw-text elements can be bypassed.
- **Neither reaches ImportXHTML or docx4j.** Both parse with the HTML parser, then convert with
  `W3CDom`:
  - -core's `JsoupHtml`: `Jsoup.parse` then `W3CDom.convert`;
  - docx4j's `model/text/JsoupHtml`: `Parser.htmlParser()` then `W3CDom.fromJsoup`.

  Neither uses the XML parser or `Cleaner`. `W3CDom` had the same quadratic pattern, which 1.23.2
  fixed as a performance change (#2559), with no advisory. On 1.22.1, elements nested 8,000 deep,
  each with its own prefix and namespace, parse in 15 ms and convert in 20 ms (1.23.2: 18 ms and
  36 ms). The HTML parser stops nesting at 512, so the depth never reaches `W3CDom`.
- **Why it waited:** docx4j 17.3.4 pinned 1.22.1. With docx4j-bundle, docx4j's HTML altChunk
  reader runs on the jsoup our bundle ships, because docx4j-bundle has none. jsoup is optional
  in -core and in docx4j-core, so neither passes it on, and Maven doesn't mediate between the
  two. So jsoup moves when docx4j's does, to the same version.
- **Why move at all:** the shaded bundle carries jsoup's `pom.properties`, so a dependency
  scanner reports the released 17.3.4 bundle's 1.22.1 against both advisories, though neither is
  reachable.
- **What 1.22.2 to 1.23.2 change:** `W3CDom`'s namespace and prefix handling is reworked (Word
  MHT's `o:` and `v:` elements go through it); `noscript` content becomes elements; CDATA in HTML
  becomes a bogus comment; RCDATA and RAWTEXT follow the spec; `ins`, `del` and `button` become
  inline.
- **Moved for 17.3.5** on 24 October, with docx4j, on the rule that both move or neither does.
  The portfolio session coordinated, and both sessions said go. Jason: "yes, tell both sessions to
  bump jsoup to 1.23.2 for 17.3.5" (03650a7).
  - **Assessed** on two `git archive` copies of `VERSION_17_3_5` at 19f38d8, one at each jsoup
    (the archive's `jsoup-1.23.2-2026-10-24/`): suite 166/0/0 and `BundleIT` 3/0/0 on both, with
    the same counts in every test class. `BundleIT`'s smoke test runs docx4j's
    `convertAltChunks` on the bundle's jsoup.
  - **The jar:** still Java 8 bytecode. The module descriptor is the same apart from its
    version. `Multi-Release: true`, with a `META-INF/versions/11/` holding `module-info` and the
    HTTP client classes, so the Multi-Release note in the bundle's pom (CR-004 §3) still holds.
  - **`convertMHT`'s output,** with only the jsoup jar swapped, on Word "Save as Web Page" HTML
    (`o:`, `v:`, `w:` and `m:` elements, conditional comments and `mso-list` paragraphs) and on
    HTML5 edge cases (`noscript`, CDATA, `ins`/`del`, `button`, unclosed elements, entities
    without semicolons, 600 nested `div`s). One thing moved: a CDATA section in body text. 1.22.1
    kept `<![CDATA[ <b>raw</b> & ]]>` as the text `<b>raw</b> & `. 1.23.2 reads `<![CDATA[ <b>`
    as a comment, as HTML's parsing rules do, and gives `raw` then ` & ]]>`. The Word output was
    identical.
  - **docx4j's side,** from docx4j-25: its suite, 2,047 tests, the same on both versions; its 93
    tests that run through this importer pass with 1.23.2; no golden file moved.
  - **Done** in -core, -core-tests and the bundle's `build-dependencies` profile: suite 166/0/0,
    `BundleIT` 3/0/0. The bundle's jsoup `pom.properties` says 1.23.2, and the flattened pom
    doesn't name jsoup.
