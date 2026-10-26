# CR-007: integer attributes within Word's range

Status: PROPOSED 2026-10-26, at Jason's request ("draft the CR"), after docx4j-25's hand-off of
docx4j CR-066 phase 2 the same day: "your own CR decides whether ImportXHTML should throw, clamp or
leave it". Drafted by Claude Opus 5.5 in the docx4j-ImportXHTML session, with each finding measured
first (§2). Decisions D1 to D5 (§5) await Jason.
Scope:
- the integer attributes ImportXHTML computes itself: `TableHelper`, `ParagraphBorderHelper`,
  `ListHelper.createIndent`, and `XHTMLImporterImpl`'s paragraph indent;
- the ones docx4j's CSS converters compute for it, in `addParagraphProperties` and
  `addRunProperties`;
- tests in core-tests, and the CHANGELOG.

Not in scope: the units ImportXHTML converts with (§2.3, D3); the pptx path; image sizes, which
are EMUs in 64-bit attributes.

Branch: `VERSION_17_3_5`. Phases (§4): 0. a Word check; 1. the guard; 2. the release and the
hand-off.

## 1. Word's storage, and what docx4j now does

docx4j CR-066 measured how Word 365 stores each integer attribute (its §3.1 and §3.4; two rounds,
Jason's runs, 2026-10-26). Word refuses no value and repairs none. It keeps each one in a fixed
width, so a value outside it wraps, with no error. The attributes ImportXHTML writes:

| Attribute | Written by | Word's storage |
|---|---|---|
| `w:ind/@w:left` | `XHTMLImporterImpl`; `ListHelper.createIndent`, in the numbering part | int16 |
| `w:spacing/@w:before`, `@w:after` | docx4j's `SpaceBefore`, `SpaceAfter` | uint16 |
| `w:spacing/@w:line` (auto) | docx4j's `LineSpacing` | int16 |
| `w:rPr/w:spacing/@w:val` | docx4j's run `Spacing` | int16 |
| `w:sz/@w:val` | docx4j's `FontSize` | uint16 |
| `w:tblInd`, `w:tcW`, `w:tblCellSpacing`, `w:tcMar/*` `@w:w` (dxa) | `TableHelper` | int16 |
| `w:gridCol/@w:w` | `TableHelper` | not measured (Word recomputes the grid from the cells) |
| `w:trHeight/@w:val` (atLeast) | `TableHelper`, from the row's laid-out height | int16, the sign is the rule: 32768 to 65535 read as exact |
| a border's `@w:sz` | `TableHelper`, `ParagraphBorderHelper` | 8 bits |
| a border's `@w:space` | `ParagraphBorderHelper` | 5 bits |

What docx4j does from 17.3.5 (docx4j 65ede5ec5 and 109a8d47b):
- **Its CSS converters** WARN when a length converts to a value outside Word's range for its
  attribute, and write it as computed.
- **Every load and save** WARNs for an integer attribute outside its type's range. For
  `ST_TwipsMeasure`, which covers `w:trHeight`, that is only the range certain to change, outside
  -32768..65535. `TblWidth`'s `@w:w` (`ST_DecimalNumber`) isn't reported at all (CR-066 D7).
- **`TblFactory.createTable`** throws for a cell width outside 0..32767. ImportXHTML doesn't call
  it.

Against that snapshot, the suite is 178/0/0 and `BundleIT` 3/0/0. The tests have no SLF4J
provider, so they can't show the WARNs. The probes below ran with one.

## 2. Measured (2026-10-26)

Each input was converted with `XHTMLImporterImpl.convert(File, null)` at 710c2b6, and the main
document and numbering parts were marshalled. "Word reads" is from CR-066's table, not from Word.
The probes are in `~/claude-scratch-archive/docx4j-ImportXHTML/cr066-ranges-2026-10-26/`.

### 2.1 Ordinary documents: tall rows

`TableHelper` writes every row's laid-out height as `w:trHeight`, with `hRule="atLeast"` (a line
of text gives 285). For a tall row, Word reads that height differently:

| Cell content | Written | Word reads | docx4j WARN |
|---|---|---|---|
| a 3000px div (31.3 in) | 45030 | **exact 20506 (14.2 in)** | none |
| 300 paragraphs (110.9 in) | 159690 | atLeast 28618 | at save |

- **A cell whose content is 22.8 to 45.5 inches tall, about two and a half to five pages,** gets
  an exact height of 65536 minus its own. So, if Word does with it what CR-066 measured, Word cuts
  the content off. The same happens in every second 45.5-inch band above that.
- **docx4j says nothing about it:** 45030 is inside `ST_TwipsMeasure`'s certain range.
- **In the other bands,** the row reads as atLeast a smaller height. It still grows to fit its
  content, so nothing shows.

Phase 0 checks the cut-off in Word.

### 2.2 Large CSS values

| CSS | Written | Word reads | docx4j WARN |
|---|---|---|---|
| p `margin-left: 5000pt` | `w:ind/@w:left` 100120 | -30952 | converter (100000) and save |
| p `margin-left: -5000pt` | -99880 | **31192** | converter and save |
| blockquotes, or divs with `margin-left: 40px`, 60 deep | up to 36120 | up to -29416 | save |
| ul `padding-left: 5000pt` | numbering `w:ind/@w:left` 100360 | -30712 | save |
| p `margin-top`, `margin-bottom: 5000pt` | `w:spacing` 100000 | 34464 | converter and save |
| `letter-spacing: 2000pt` | `w:rPr/w:spacing` 40000 | -25536 | converter and save |
| `font-size: 40000pt` | `w:sz` 80000 | 14464 | converter and save |
| p `border: 1px solid; padding: 60px` | border `w:space` 45 | 13 | save |
| `border: 50px solid`, on p, td or table | border `w:sz` 400 | 144 | save |
| table `margin-left: 5000pt` | `w:tblInd` 133333 | 2261 | **none** |
| `border-spacing: 3000pt` | `w:tblCellSpacing` 40000 | -25536 | **none** |
| td `padding: 3000pt` | `w:tcMar` 45000; `w:tcW` 160200 | -20536; 29128 | **none** |
| td `padding: 3000pt` | `w:trHeight` 120255 | exact 10817 | save |

- **Values that look ordinary can still be out of range.** A bordered box with `padding: 60px` is
  one. A negative indent that wraps comes out positive, so text its author moved off the page lands
  21.7 inches to the right.
- **docx4j's converter WARN doesn't always describe the document.** For p `margin-left`, it reports
  docx4j's 100000, but ImportXHTML then writes its own 100120. For li `margin-left: 5000pt`, it
  fires, but ImportXHTML writes 960 in the numbering part.
- **In range:**
  - `line-height` in points writes no `w:line` (`line-height: 2` gives 480);
  - `td width: 3000pt` is held to the page (13594);
  - a negative table `margin-left` writes no `w:tblInd`;
  - lists nested 50 deep stay in range.

### 2.3 Units (not this CR's)

At ordinary values, three conversions treat openhtmltopdf's dots (1/20 px) as twips (1/20 pt). So
they come out 4/3 too large:

| CSS | Written | As the CSS says |
|---|---|---|
| table `margin-left: 1in` | `w:tblInd` 1920 | 1440 |
| `border-spacing: 10pt` | `w:tblCellSpacing` 133 | 100 (the code halves it) |
| `border: 1pt`; `4pt` | border `w:sz` 11; 43 | 8; 32 |

Cell padding reads the CSS number as px, whatever its unit: `padding: 10pt` gives `w:tcMar` 150,
not 200, and `1em` gives 15. Paragraph `margin-left` is right (1in gives 1440, plus the body's
120), and so is `td height` (1in gives 1440).

Fixing these changes ordinary documents (every table indent, bordered paragraph and padded cell),
so it's D3's. It doesn't change this CR: the guard works on the written attribute, whatever the
conversion before it.

## 3. The guard

- **A small class in -core** (package-private) holds each attribute's range, and one method,
  which:
  - returns the value when it is inside the range;
  - otherwise returns the nearest end, logging a WARN that names the attribute, the value, what
    was written, and the CSS property or element where the writer knows it.
- **The ranges:** Word's storage, as §1, cut to what CSS can mean:

| Attribute | Range |
|---|---|
| `w:ind/@w:left`, `w:tblInd` | -32768..32767 |
| `w:spacing` before and after, `w:sz` | 0..65535 (as docx4j's converter WARN) |
| `w:spacing/@w:line`, `w:rPr/w:spacing` | -32768..32767 (as docx4j's) |
| `w:tcW`, `w:gridCol`, `w:tblCellSpacing`, `w:tcMar` | 0..32767 |
| `w:trHeight` (atLeast) | 0..32767 |
| a border's `@w:sz` | 0..255 |
| a border's `@w:space` | 0..31 |

  A row taller than 32767 twips becomes atLeast 32767, and Word grows it to fit its content. A
  border wider than 255 eighths of a point becomes 255. That's still far beyond Word's UI limit
  of 48, which Word doesn't apply on read.
- **Where (D2):** at each writer of §1's table, and after the converter loops of
  `addParagraphProperties` and `addRunProperties` for the attributes docx4j's converters set.
  There are about 15 sites, each a one-line call.
- **A test that scans everything:** after converting §2's inputs, it checks every integer
  attribute of the main document and numbering parts against the table, so a writer the sites
  missed fails it. The same scan as the probe's.

## 4. Phases

### Phase 0: a Word check

One docx for Jason to open in Word, put wherever he says:
- §2.1's 3000px cell as written today (45030), then clamped (32767);
- a bordered paragraph with `padding: 60px`, today and clamped;
- `border: 50px`, today and clamped.

It records whether Word cuts off the first cell, and what the others look like. The hand-off's
table predicts it but hasn't been seen in Word. The cut-off is the reason D5 (a) ships this in
17.3.5.

### Phase 1: the guard

As §3, with D1, D2 and D4. Tests:
- the scanning test, on §2's inputs;
- one per attribute family, for the clamped value and the WARN:
  - the tall row gives atLeast 32767;
  - `padding: 60px` gives `w:space` 31;
  - `border: 50px` gives `w:sz` 255;
  - the indents give ±32767 and -32768;
  - docx4j's converter outputs are clamped.

Measured: the suite's converts are unchanged, by CR-006 phase 0's method (a temporary dump hook in
`git archive` copies, never committed). The suite has no out-of-range value, so any change there
is a bug.

### Phase 2: the release and the hand-off

The CHANGELOG, the release (D5), and the message to docx4j-25 (D4).

## 5. Decisions (for Jason; each with a recommendation)

- **D1. What ImportXHTML does with an out-of-range value.**
  - (a) Clamp to the nearest end of Word's range, with a WARN.
  - (b) Leave it, as today. docx4j's WARNs report some of them (§2).
  - (c) Throw.

  **Recommended: (a).** A wrapped value is worse than a clamped one: a row cut off, an indent
  turned around, a 60px padding become 13pt. Throwing would fail a whole document over one CSS
  value, and elsewhere the importer drops CSS it can't use. Clamping also covers the four
  `TblWidth` attributes and the tall rows, which docx4j can't report.
- **D2. Where.**
  - (a) At the writers, plus the scanning test (§3).
  - (b) One pass over the content each convert produced, and the numbering levels it made, after
    `layoutAndTraverse`'s traversal, where `DisplayNoneHelper.apply` runs.

  **Recommended: (a).** The WARN can then name the CSS property, as docx4j's does. (b) would be
  one place to maintain, but it would need its own walk through sdts, nested tables and both
  parts, and by then the CSS is gone. The test closes the gap (a) leaves.
- **D3. The units (§2.3).**
  - (a) A CR of its own, with a Word check.
  - (b) A later phase of this CR.
  - (c) Leave them.

  **Recommended: (a).** It changes ordinary documents, where this CR changes only values Word
  would misread. They are better decided and released separately.
- **D4. What docx4j-25 is told,** once D1 is decided.

  **Recommended:** two points:
  - A row height of 32768..65535, which Word reads as exact, is inside `ST_TwipsMeasure`'s
    certain range, so docx4j's report misses it. A report on `CTHeight` would catch it, as
    CR-066 §3.1 suggests for `TblWidth`.
  - Under D1 (a), the converter's "written as computed" may not hold for a caller that guards.
    For `margin-left`, its value isn't the one written even today.
- **D5. The release.**
  - (a) 17.3.5, with CR-006 and docx4j's CR-066.
  - (b) The release after.

  **Recommended: (a),** if phase 0 confirms the cut-off. A tall cell is ordinary content, and the
  guard is small. Otherwise (b) is as good.

## 6. Risks

- **Not seen in Word.** §2's "Word reads" column is CR-066's measurement of the attribute, not of
  these documents. Phase 0 checks the tall row.
- **A clamped value is still wrong**, only less so: an indent of 32767 twips is 22.8 inches. The
  guard keeps Word from reading something else. It doesn't make the layout right.
- **A site missed.** The scanning test covers §2's inputs, not every path. A new writer needs its
  own call, and the test's inputs should grow with it.
- **docx4j's WARNs stay.** Its converter still warns before ImportXHTML clamps, so one value can
  give two lines.

## 7. Sources

- docx4j `docs/developer/change-requests/CR-066-integer-attributes-within-words-range.md` §3.1,
  §3.4 and §9; docx4j 65ede5ec5 and 109a8d47b; `org.docx4j.model.properties.Property`
  `warnIfOutsideWordRange` and its callers; `org.docx4j.wml.LenientInteger`.
- docx4j-25's hand-off message, 2026-10-26.
- This repository: `TableHelper` (`setupTblPr`, `setupTrPr`, `setupTcPr`, `getCellMargin`,
  `copyBorderStyle`), `ParagraphBorderHelper` (`paddingToSpace`, `createBorderStyle`),
  `ListHelper.createIndent`, and `XHTMLImporterImpl` (`addParagraphProperties`,
  `addRunProperties`, `layoutAndTraverse`).
- The probes' README in the archive directory above.
