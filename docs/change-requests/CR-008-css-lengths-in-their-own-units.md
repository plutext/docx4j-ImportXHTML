# CR-008: CSS lengths in their own units

Status: ACCEPTED 2026-10-28 (Jason: "CR-008 D1 to D7: decisions as recommended"): D1 (a) all of
§2.1 and §2.2; D2 (a) padding from the layout; D3 (a) keep `w:tblCellSpacing`'s halving; D4 (a) a
paragraph's double border divided by 3; D5 (a) `padding: 0` writes 0; D6 (b) the layout's width is
a CR of its own, so phase 2 leaves this one; D7 (a) no setting for the old values. Phase 0 done
2026-10-28, by Jason's run in Word 365 (§4). Phase 1 is next, after CR-007's. Proposed 2026-10-28,
from CR-007's D3, which Jason decided the same day: "CR-007 decisions as recommended; the new D3
CR should also be in the 17.3.5 release". So the release is decided: 17.3.5, with CR-006 and
CR-007. Drafted by Claude Opus 5.5 in the docx4j-ImportXHTML session, with each finding measured
first (§2).
Scope:
- the measures ImportXHTML converts from openhtmltopdf's layout itself, in `TableHelper` and
  `ParagraphBorderHelper`: column widths, table indent, cell spacing, cell margins, border `w:sz`
  and `w:space`;
- the width openhtmltopdf lays the document out at (`DocxRenderer`, D6). Not in this CR, by D6
  (b);
- tests in core-tests, and the CHANGELOG.

Not in scope: the ranges these values must stay within (CR-007); the lengths docx4j's CSS
converters compute (paragraph indents and spacing, font size), which are right (§2.3); the pptx
path; the body's default 8px margin, which ImportXHTML writes into paragraph indents (120).

Branch: `VERSION_17_3_5`. Phases (§4): 0. a Word check, with CR-007's; 1. the units; 2. the layout's
width (D6; not in this CR, by D6 (b)); 3. the release.

## 1. Dots

openhtmltopdf measures in dots. `DocxRenderer` sets 20 dots to the CSS pixel and 20 × 4/3 to the
point, so 96 px to the inch. A dot is then 3/4 of a twip.

It wasn't always so. b984054 (2013, "More cell borders support: real units of measures via layout
context") set 20 dots to the point, so a dot was a twip, and the border code it added writes dots
as twips. The same day, eafda52 took the renderer from the xhtmlrenderer jar instead. 5dd3ad6
(2021-01-11, first released in 8.2.0) brought it back into this project with 20 × 4/3. The image
path was corrected after that: 9375246 (2022) changed its `dotsToTwip` from × 4/3 to × 3/4. The
table and border sites (in `TableHelper` since 36f040b, 2014) still write dots as twips.

A second kind of site reads `LengthValue.asFloat()`. That's the number the CSS gives, in whatever
unit, and the code converts it as pixels.

## 2. Measured (2026-10-28)

Each input was converted with `XHTMLImporterImpl.convert(File, null)` at c162792 (the code as
6da82a2), against the docx4j-core 17.3.5-SNAPSHOT installed 2026-10-27, and the main document and
numbering parts were marshalled. "As the CSS says" is the CSS length in the attribute's unit. The
inputs and outputs are in
`~/claude-scratch-archive/docx4j-ImportXHTML/cr008-units-2026-10-28/`.

### 2.1 Dots written as twips: 4/3 too large

| Attribute | CSS | Written | As the CSS says |
|---|---|---|---|
| `w:tcW`, `w:gridCol` | td `width: 1in` | 1920 | 1440 |
| | table `width: 6in`, two columns | 5760 each (8 in) | 4320 each |
| | table `width: 100%`, two columns | 6817 each (9.47 in) | 5113 each (7.1 in, see §2.4) |
| | a cell with a line of text, auto width | 13354, beside a cell of 240 (9.44 in) | 10016 and 180 (7.08 in) |
| `w:tblInd` | table `margin-left: 1in` | 1920 | 1440 |
| | `36pt`, `48px` or `3em` | 960 | 720 |
| `w:tblCellSpacing` (the code halves it) | `border-spacing: 10pt` | 133 | 100 |
| | the default stylesheet's 2px | 20 | 15 |
| border `w:sz`, on table, td and p | `1px` and `thin` | 8 | 6 |
| | `0.5pt` | 5 | 4 |
| | `1pt` | 11 | 8 |
| | `2px` and `medium` | 16 | 12 |
| | `thick` (3px) | 24 | 18 |
| | `4pt` | 43 | 32 |

- **Every table is written a third wider than its CSS says,** whatever its width was given in, or
  whether it was given at all. A table that fills its line is written 9.44 inches wide, on a docx
  whose text is 6.27 inches wide (`createPackage`'s A4, 1-inch margins) and whose paper is 8.27.
  Word fits a table wider than the text to the text (phase 0). So the third shows on a table that
  comes out narrower than the text: on that page, one whose CSS width is under 4.7 inches.
- **The commonest border is wrong:** `1px` writes 1pt, not 3/4pt.
- **Right already:** `w:trHeight` (td `height: 1in` and `72pt` give 1440), which converts dots to
  pixels first; and image extents (`width="96"` and `1in` give 914400 EMU), since 9375246.

### 2.2 The unit ignored

| Attribute | CSS | Written | As the CSS says |
|---|---|---|---|
| `w:tcMar` (twips) | td `padding: 10px` | 150 | 150 |
| | `10pt` | 150 | 200 |
| | `0.25in` | 4 | 360 |
| | `1em`; `2em` | 15; 30 | 240; 480 (16px font) |
| | `1em` at `font-size: 20pt` | 15 | 400 |
| | `5%` | 75 | 5% of the table's width |
| | `0` | 15 (the code's "Default") | 0 |
| border `w:space` (points), p with a border | `padding: 24px` | 18 | 18 |
| | `24pt` | 18 | 24 |
| | `4pt` | 3 | 4 |
| | `1em` | 1 | 12 |
| | `0.25in` | none | 18 |

- **Pixels are right:** padding in px comes out as the CSS says.
- **The bordered paragraph's indent is right** where its border space isn't. `padding: 24pt` adds
  480 twips (24pt) to `w:ind/@w:left`, and writes `w:space` 18.

### 2.3 Also measured

- **Double borders differ between tables and paragraphs.** `TableHelper` divides a double
  border's width by 3, since Word's `w:sz` is one line's. `ParagraphBorderHelper` doesn't: td
  `3px double` writes 8, p `3pt double` writes 32. With the units right and the division, the p
  would be 8.
- **Right, from docx4j's converters or `ListHelper`:**
  - p `margin-left: 1in` gives `w:ind/@w:left` 1440;
  - `margin-top: 12pt; margin-bottom: 6pt` give `w:spacing` 240 and 120;
  - ul `padding-left: 1in` gives the numbering level's `w:ind/@w:left` 1800 (1440, and the
    360 hanging);
  - a table in a list item with `margin-left: 1in` gets `w:tblInd` 2400: the list's 960 and the
    1440.

### 2.4 The width openhtmltopdf lays out at

`DocxRenderer.getInitialExtents` takes openhtmltopdf's first page box. With no `@page` in the
document, that's A4 with its default stylesheet's `margin: 0.5in`, so 7.27 inches. The target
docx's own page doesn't come into it. So:

- `width: 100%` means 7.1 inches (7.27, less the body's 8px margins), even with the units right;
  the docx's text is 6.27 inches wide.
- A table that fills its line is as wide.
- Only an image with no size is fitted to the docx's text width (by docx4j, from its last
  section), so one document holds content laid out at both widths.
- Row heights are laid out at that width too. A row whose text wraps less on the wider page gets
  a smaller `w:trHeight`, which Word, as atLeast, grows past.

## 3. The change

- **One conversion for dots** (D1): twips from dots by the renderer's own dots per point, not a
  constant, since `DocxRenderer(float dotsPerPoint, int dotsPerPixel)` is public. At `w:tcW`,
  `w:gridCol`, `w:tblInd`, `w:tblCellSpacing`, and border `w:sz` in both helpers. The image
  path's `dotsToTwip` (72/`UnitsOfMeasurement.DPI`, right at docx4j's default of 96) can use it
  too, with no change to its output.
- **Padding from the layout** (D2): the box's computed padding (`Box.getPadding(CssContext)`, in
  dots), which has resolved em and % as openhtmltopdf lays them out. Then twips for `w:tcMar`,
  points for `w:space`.
- **`padding: 0` writes 0** (D5).
- **A paragraph's double border divided by 3,** as a table's is (D4).
- **The layout at the docx's text width** (D6): `getInitialExtents` takes the width of the target
  document's last section, `PageDimensions.getWritableWidthTwips()`, in dots. That's the width
  docx4j fits an image with no size to. The document's own `@page` would then no longer set the
  width, which is right for a docx, since Word lays it out on the docx's section. (D6 (b): a CR
  of its own.)

`w:tblCellSpacing` keeps the code's halving (D3), if phase 0 shows Word's gap between cells is
twice the value.

## 4. Phases

### Phase 0: a Word check

With CR-007's phase 0, in the same docx:
`~/claude-scratch-archive/docx4j-ImportXHTML/cr008-units-2026-10-28/word-check/word-check-cr007-cr008.docx`
(built 2026-10-28). Each case is as written today, then with this CR's value set by hand:
- the 6-inch table: 5760 + 5760, then 4320 + 4320;
- the table that fills its line, its cells bordered: 13414 + 220, then 10061 + 165;
- `border-spacing: 10pt`: 133, then 100. Check the gap between cells, and what Table Properties
  shows for spacing between cells;
- p `border: 3pt double`: 32, then 8. Check the lines' widths.

It records whether Word keeps column widths wider than the text (D6 rests on it), what
`w:tblCellSpacing` means (D3), and what `w:sz` means on a double border (D4).

**Result (2026-10-28).** As CR-007's: Jason's run in Word for Microsoft 365, saved as a PDF and
measured at 720 dpi:
- **4a and 4b, the 6-inch table.** Written 8 in, Word draws it 6.27 in, the text's width. Written
  6 in, it draws 6.02 in (6 in and the borders).
- **5a and 5b, the table that fills its line.** Written 9.47 in and 7.10 in, Word draws both
  6.27 in, ending at the right margin, with the second column about 10pt. So Word doesn't keep
  column widths wider than the text: it fits the table to it.
- **6a and 6b, `w:tblCellSpacing`.** Word leaves twice the value between cells, and between the
  table's border and a cell: 133 gives 13.0 to 13.5pt, and 100 gives 9.6 to 10.1pt. CSS's
  `border-spacing` is both gaps, so 100 is right for 10pt.
- **7a and 7b, a double border.** `w:sz` is each line's width, and the gap is the same. 32 gives
  two 3.9pt lines 3.8pt apart (11.6pt); 8 gives two 1.0pt lines 0.9pt apart (2.9pt), the CSS's
  3pt.

The Table Properties and Borders and Shading readings weren't recorded. The drawings answer each
question. The round, round 1, is in
`docx4j-ImportXHTML-core-tests/src/test/resources/word-check/evidence/word-open-results.md`.

### Phase 1: the units

As §3, with D1 to D5, after CR-007's phase 1, which changes the same writers. Tests:
- one per site, at ordinary values, in in, pt, px and em (and % for padding):
  - the 6-inch table gives 4320 + 4320;
  - `1px` gives `w:sz` 6;
  - `padding: 10pt` gives `w:tcMar` 200;
  - p `padding: 24pt` gives `w:space` 24;
- CR-007's scanning test, still passing.

Measured: the suite's converts before and after, by CR-006 phase 0's method (a temporary dump
hook in `git archive` copies, never committed). The attributes §2.1 and §2.2 list are expected to
change, and nothing else.

### Phase 2: the layout's width

Not in this CR (D6 (b), decided 2026-10-28). It becomes a CR of its own, not in 17.3.5. As
proposed:

As §3, with D6. Tests: a `width: 100%` table and one that fills its line come out at the docx's
text width (9027, less the body's margins); wider margins give a narrower table, and a landscape
page a wider one. Measured as phase 1: widths, row heights and image extents change; the rest is
listed.

### Phase 3: the release

The CHANGELOG (each site, with its old and new values, since ordinary documents change), and the
release with CR-007, in 17.3.5.

## 5. Decisions (for Jason; each with a recommendation)

- **D1. Which sites.**
  - (a) All of §2.1 and §2.2.
  - (b) The dots only (§2.1).

  **Recommended: (a).** They're one bug, the CSS length not reaching Word as written, and one
  Word check and one CHANGELOG entry cover them.

  **Decided 2026-10-28 (Jason): (a).**
- **D2. Padding's conversion.**
  - (a) The box's computed padding, from the layout.
  - (b) The CSS number converted by its unit, in ImportXHTML.

  **Recommended: (a).** em and % then mean what they meant in the layout, as for the other lengths
  §2.3 lists. (b) would need its own em and % rules.

  **Decided 2026-10-28 (Jason): (a).**
- **D3. `w:tblCellSpacing`'s halving.**
  - (a) Keep it, if phase 0 shows the gap between cells is twice the value.
  - (b) Drop it.

  **Recommended: (a).** The code's comment ("appears twice thicker, probably taken from both
  sides") says Word applies it at each cell's edge. Phase 0 checks.

  Phase 0 (2026-10-28): the gap is twice the value, so the condition holds.

  **Decided 2026-10-28 (Jason): (a).**
- **D4. A paragraph's double border.**
  - (a) Divide by 3, as for tables.
  - (b) Leave it.

  **Recommended: (a),** if phase 0 shows `w:sz` is one line's width on a double border. A double
  border then has the same width on a table and a paragraph.

  Phase 0 (2026-10-28): it is, and the gap is the same, so the condition holds.

  **Decided 2026-10-28 (Jason): (a).**
- **D5. `padding: 0` on a cell.**
  - (a) Write 0.
  - (b) Keep the 15 twips (a pixel) the code writes.

  **Recommended: (a).** The author asked for none. The default stylesheet's 1px padding still gives
  15.

  **Decided 2026-10-28 (Jason): (a).**
- **D6. The layout's width (§2.4).**
  - (a) Phase 2 of this CR: the docx's text width.
  - (b) A CR of its own.
  - (c) Leave it.

  **Recommended: (a),** if phase 0 shows Word keeps column widths wider than the text. Without it,
  after phase 1 a table that fills its line still runs 0.8 inches into the right margin: phase 0's
  case 5b. Otherwise (b).

  Phase 0 (2026-10-28): Word fits it to the text, so the condition fails. **Recommended now:
  (b).** What's left of §2.4 is a % width about 13% too wide: `width: 50%` gives 3.55 in, where
  50% of the docx's text is 3.13 in. Row heights are laid out at the wider page as well, and
  Word grows them. That's worth a CR, but not 17.3.5. Phase 2 then leaves this CR.

  **Decided 2026-10-28 (Jason): (b).**
- **D7. A setting for the old values.**
  - (a) None. The CHANGELOG gives each site's old and new values.
  - (b) A property in `docx4j-ImportXHTML.properties` to keep the old conversion.

  **Recommended: (a).** The old values aren't what the CSS says. A setting would double what the
  tests must cover, and a document tuned to the old output can be retuned from the CHANGELOG.

  **Decided 2026-10-28 (Jason): (a).**

## 6. Risks

- **Ordinary documents change**, which is why this is a CR of its own (CR-007 D3):
  - every table's column widths, and an indent from its `margin-left`, shrink to 3/4;
  - every border is 3/4 as wide;
  - cell padding in pt, in, em or % changes;
  - with D6, every table that fills its line, and every % width, is narrower (not in this CR,
    by D6 (b)).

  Someone who compensated in their CSS sees a change.
- **Word not checked.** That a docx table wider than its text runs into the margin is from the
  attributes, not seen in Word. Phase 0 checks. (It doesn't: Word fits the table to the text,
  §4. The third shows on narrower tables.)
- **Row heights move** with D6 (not in this CR, by D6 (b)), since the layout wraps text at another
  width. They're atLeast, so a row Word needs taller still grows. (From CR-007 phase 1, a row
  gets a `w:trHeight` only from a CSS height, which the layout's width doesn't change.)
- **CR-007's guard** works on the written value, so it's unaffected. Its inputs stay out of range
  (CR-007 phase 1).

## 7. Sources

- `TableHelper` (`setupTblPr`, `setupTblGrid`, `setupTcPr`, `getCellMargin`, `copyBorderStyle`),
  `ParagraphBorderHelper` (`paddingToSpace`, `createBorderStyle`), `XHTMLImporterImpl`
  (`dotsToTwip`, `TableProperties`), `DocxRenderer` (`DEFAULT_DOTS_PER_POINT`,
  `getInitialExtents`).
- b984054, 5dd3ad6 and 9375246.
- openhtmltopdf 1.1.100's `/resources/css/XhtmlNamespaceHandler.css` (`@page`, `table`).
- CR-007 §2.3, where these were first found.
- The probes' README in the archive directory above.
