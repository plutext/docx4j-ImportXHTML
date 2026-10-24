# CR-006: openhtmltopdf 1.1.100: colours with alpha, thead's keepLines, and the move

Status: ACCEPTED 2026-10-24 (Jason: "D5 (b); others as recommended"): all four phases in 17.3.5,
and D1 to D4 and D6 as recommended. Proposed the same day, at Jason's request ("yes" to "draft
CR-006 for openhtmltopdf"), after `docs/dependencies-status.md` §3 kept openhtmltopdf at 1.1.31
for 17.3.4. Drafted by Claude Opus 5.5 in the docx4j-ImportXHTML session, with each finding
measured first (§1). The doubled `w:br` it found was fixed outside this CR, for 17.3.5 (§1, row 1).
Phases 0 to 3 implemented 2026-10-24 (suite 178/0/0, BundleIT 3/0/0; §5), commit 6da82a2. Phase 1
needs 1.1.100's alpha, so the move came with it. The release is 17.3.5's.
Scope:
- colour handling: `TableHelper` and `ParagraphBorderHelper` (border colours), and the CSS
  values the importer hands to docx4j's `PropertyFactory` (text colour and shading);
- `page-break-inside` from openhtmltopdf's default stylesheet;
- -core's openhtmltopdf version;
- tests in core-tests, the CHANGELOG, and `docs/dependencies-status.md`.

Branch: `VERSION_17_3_5`. Phases (§5): 0. a wider measurement; 1. colours; 2. the default
stylesheet's `page-break-inside`; 3. the move.

## 1. Measured (2026-10-23 and 24)

Each input was converted on 1.1.31 and on 1.1.100, with the same -core, and the WML compared.
`docs/dependencies-status.md` §2 and §3 have the method. The probes are in
`~/claude-scratch-archive/docx4j-ImportXHTML/dependency-bumps-2026-10-23/`.

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
  box in two. The importer now makes the `w:br` only where the box starts.
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
phase 0 starts now. openhtmltopdf publishes a release for each merge (§7). 1.1.100 declares PDFBox 3.0.8, the
version -core pins, where 1.1.31 declared 3.0.3. The API -core uses has additions only
(`dependencies-status.md` §3). The CHANGELOG names rows 3 and 8, and any of the not-measured
list that phase 0 finds.

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
    `dependencies-status.md` §5.
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

As §4. The CHANGELOG, `docs/dependencies-status.md` §3, and the release (D5).

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
- **The CHANGELOG** (17.3.5) and `docs/dependencies-status.md` §1 and §3 are updated.

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
- D6 (a), the version phase 0 measures, 1.1.100.

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

- `docs/dependencies-status.md` §2 and §3, and the archive's `followups-2026-10-24/README.txt`.
- openhtmltopdf's releases, https://github.com/openhtmltopdf/openhtmltopdf/releases, and its
  `XhtmlNamespaceHandler.css` and `XhtmlCssOnlyNamespaceHandler` at 1.1.31 and 1.1.100.
- docx4j's `model/properties/PropertyFactory.java`, `run/RShading.java`,
  `paragraph/PShading.java` and `table/tc/Shading.java`.
- This repository: `XHTMLImporterImpl.processInlineBoxContent`, `TableHelper`,
  `ParagraphBorderHelper`, `DisplayNoneHelper`, and CR-002 §2 and §7.
