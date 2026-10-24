# CR-006: openhtmltopdf 1.1.100: colours with alpha, thead's keepLines, and the move

Status: PROPOSED 2026-10-24, at Jason's request ("yes" to "draft CR-006 for openhtmltopdf"),
after `docs/dependencies-status.md` §3 kept openhtmltopdf at 1.1.31 for 17.3.4. Drafted by Claude
Opus 5.5 in the docx4j-ImportXHTML session, with each finding measured first (§1). The doubled
`w:br` it found was fixed outside this CR, for 17.3.5 (§1, row 1). Decisions D1 to D6 await
Jason (§6).
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

-core's `openhtmltopdf-core` and `-pdfbox` go to 1.1.100, or to the newest when phase 3 starts.
openhtmltopdf publishes a release for each merge (§7). 1.1.100 declares PDFBox 3.0.8, the
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

### Phase 1: colours

As §2, with D2 and D3, and tests for rows 4 to 7, on 1.1.31 and on the target version. Row 7's
fix doesn't depend on the move, so this phase can land first.

### Phase 2: the default stylesheet's page-break-inside

As §3, with D1.

### Phase 3: the move

As §4. The CHANGELOG, `docs/dependencies-status.md` §3, and the release (D5).

## 6. Decisions (for Jason; each with a recommendation)

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
- **D6. The target version.** (a) 1.1.100; (b) the newest when phase 3 starts.

  **Recommended: (b), at least two weeks old.** 1.1.100 was a day old when measured, and a
  release comes with each merge.

## 7. Risks

- **Phase 0 finds more than §1 lists.** The release notes cover 69 releases, and §1 measured ten
  documents and two probes.
- **Blending (D2 (b)) is a guess at the page.** A document on a dark background gets colours lighter
  than its author saw.
- **The versions move quickly.** One release per merge means the version measured in phase 0 can
  be superseded by phase 3, so phase 3 reruns phase 0's comparison on its own version.
- **Word not checked.** Neither `w:color="transparent"` nor rows 4 to 6 were opened in Word.

## 8. Sources

- `docs/dependencies-status.md` §2 and §3, and the archive's `followups-2026-10-24/README.txt`.
- openhtmltopdf's releases, https://github.com/openhtmltopdf/openhtmltopdf/releases, and its
  `XhtmlNamespaceHandler.css` and `XhtmlCssOnlyNamespaceHandler` at 1.1.31 and 1.1.100.
- docx4j's `model/properties/PropertyFactory.java`, `run/RShading.java`,
  `paragraph/PShading.java` and `table/tc/Shading.java`.
- This repository: `XHTMLImporterImpl.processInlineBoxContent`, `TableHelper`,
  `ParagraphBorderHelper`, `DisplayNoneHelper`, and CR-002 §2 and §7.
