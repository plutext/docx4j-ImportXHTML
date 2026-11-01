# CR-009: the layout at the docx's text width

Status: ACCEPTED 2026-11-01 (Jason: "CR-009's decisions, D1 to D6 as recommended"): D1 (b) the
text width plus the body's margins; D2 (a) the last section; D3 (a) a setter for another width;
D4 (a) the docx's section over `@page`; D5 (a) the Word check alongside phase 1; D6 (a) 17.3.5.
Phase 0 done 2026-11-01, Word as predicted; phase 1 done 2026-11-01, uncommitted (§4). Proposed
2026-11-01. This is CR-008's D6 (b), "the layout's width is a CR of its own", registered
until then as the deferred xhtml/CR-008.2. Jason, the same day, on the 17.3.5 summary: "let's
investigate the possible problem on wide pages; we could potentially do the layout-width CR before
our 17.3.5 release", then "we should fix the 2 issues you also found (file a github issue first),
then draft CR-009". Those two are #118 and #119, fixed outside this CR (§1). Drafted by Claude
Opus 5.5 in the docx4j-ImportXHTML session, with each finding measured first (§2).
Scope:
- the width `DocxRenderer` lays the document out at, which becomes the target docx's text width;
- a way to give another width, for content bound for somewhere narrower (D3);
- tests in core-tests, and the CHANGELOG.

Not in scope: the pptx path (`XHTMLtoPPTX` makes its own `DocxRenderer`, which is unchanged); row
heights, which since CR-007 come only from a CSS height; the page's height, which only
openhtmltopdf's pagination reads, and nothing written depends on that.

Branch: `VERSION_17_3_5`. Phases (§4): 0. a Word check; 1. the width; 2. the release.

## 1. Background

openhtmltopdf lays the document out before ImportXHTML reads it. `DocxRenderer.getInitialExtents`
takes the width of openhtmltopdf's first page box. With no `@page` in the document, that's A4
less its default stylesheet's 0.5in margins, 7.27in. The body's default 8px margins come off
that, so `width: 100%` is 7.1in (CR-008 §2.4). The target docx's page plays no part.

CR-008 D6 (b) (2026-10-28) left this to a CR of its own, not for 17.3.5. CR-008's phase 0 had
shown that Word fits a table wider than the text to the text (cases 5a, 5b) and keeps a narrower
table at its own width (4b). So what was left after CR-008 was a `%` width about 13% too wide on
an A4 portrait page.

That weighing missed pages wider than 7.1in. 17.3.4 wrote dots as twips (CR-008 §1), so a table
that filled its line came out 9.44in, and Word fitted that to any narrower line. CR-008 makes it
7.08in, and Word keeps it at 7.08in. So on a landscape or A3 page, 17.3.5 as it stands is a
regression from 17.3.4. This came up on 2026-11-01, while drafting the 17.3.5 release summary.

Two more problems turned up while measuring this. Each has a GitHub issue and is fixed outside
this CR. The fixes were uncommitted when this was drafted.
- **#118:** a paragraph's `w:ind` counted the body's margin, so every paragraph got 120 (6pt).
  In a table cell it also counted the cell's padding, which is already its `w:tcMar`, and the
  blocks outside the table, which itself got no `w:tblInd`. Now the sum stops at the body and
  at a table cell, and a table outside a list takes the indent of the blocks around it. Of the
  suite's 381 converts, 165 change, all in `w:ind` (the body's 120 gone).
- **#119:** openhtmltopdf takes a table's horizontal margins off its set width
  (`TableBox.getCSSWidth`, since openhtmltopdf/openhtmltopdf#59, so that a `width: 100%` table
  with a margin fits its page). So `width: 4in; margin-left: 1in` came out 3in wide. The columns
  now get the margins back, as far as the line has room beside them. No convert in the suite
  changes.

#118 treats the body's margin, border and padding as lying outside the docx's text, as the
page's margins do. D1 (b) is the same rule applied to the layout.

## 2. Measured (2026-11-01)

On `VERSION_17_3_5` at a1bb15d (openhtmltopdf 1.1.103, docx4j 17.3.5), before #118 and #119.
The probes are in `~/claude-scratch-archive/docx4j-ImportXHTML/layout-width-2026-11-01/`, with a
README.

### 2.1 The same widths on every page

`LayoutWidthProbe` converts the same cases into packages from `createPackage(size, landscape)`.
It reports the sum of `w:gridCol`, or the image's `wp:extent`, as a share of the last section's
text width. Every page set-up gives the same widths:

| | A4 portrait, 6.27in | Letter portrait, 6.50in | A4 landscape, 9.69in | Letter landscape, 9.00in | A3 portrait, 9.69in | A4, 0.5in margins, 7.27in |
|---|---|---|---|---|---|---|
| table `width: 100%` (7.08in) | 113% | 109% | 73% | 79% | 73% | 97% |
| table, auto width, long text | 113% | 109% | 73% | 79% | 73% | 97% |
| table `width: 50%` (3.53in) | 56% | 54% | 36% | 39% | 36% | 49% |
| div `width: 50%`, table `100%` | 56% | 54% | 36% | 39% | 36% | 49% |
| img `width: 100%`, or `max-width: 100%` (7.10in) | 113% | 109% | 73% | 79% | 73% | 98% |
| img `width: 50%` (3.55in) | 57% | 55% | 37% | 39% | 37% | 49% |
| table `width: 6in` (control) | 96% | 93% | 62% | 67% | 62% | 83% |

What Word shows of these follows from CR-008's phase 0, except where marked:
- **Portrait:** a table that fills its line is fitted to the line, as in 17.3.4. `width: 50%` is
  56% of an A4 line (75% in 17.3.4).
- **Landscape and A3:** a table that fills its line is 73 to 79% of the line. In 17.3.4 it was
  9.44in, which Word fitted to Letter landscape's 9in (100%) and kept on A4 landscape (97%).
- **Images:** not checked in Word until phase 0, which found that Word keeps an image wider than
  the line (2a). An image of 7.10in is 0.83in wider than an A4 portrait line, so it runs into the
  right margin. 17.3.4 gives the same, since its image path has used the right unit since
  9375246.
- **Centred tables** (`margin: 0 auto`) get their `w:tblInd` from where the layout puts them. They
  are centred on 7.1in, so on an A4 portrait page they sit about 0.5in right of centre.
- **A document's own `@page`** sets the width: `@page { size: 11in 8.5in; margin: 1in }` gives a
  `width: 100%` table of 8.81in. That's the only way to change the width today.

### 2.2 The prototype

`DocxRenderer.setLayoutWidth(dots)` is used by `getInitialExtents` in place of the first page
box's width. `XHTMLImporterImpl.layoutAndTraverse` sets it before layout, from the last section's
`PageDimensions.getWritableWidthTwips()`. That's about 20 lines; `src/prototype-b.diff` in the
archive has them. I tried two variants:
- **(a) The text width.** `width: 100%` comes to 97 or 98% of the line, because the body's 8px
  margins come off it.
- **(b) The text width plus the body's horizontal margin, border and padding,** so that the body's
  content box is the text width. `width: 100%` and `width: 50%` come out at 100% and 50% of the
  line on every page set-up above, and images do too. The control, `width: 6in`, doesn't move.

Both pass the suite (196/0/0, BundleIT 3/0/0). Of the suite's 381 converts, 9 change, and only in
table widths. Under (b):
- `w:gridCol` and `w:tcW`: 5203 → 4484 (×5), 10196 → 8997, and a 4-column table, 3560 + 3144 +
  1468 + 1020 → 3130 + 2764 + 1293 + 900;
- `w:tblInd`, for two centred tables (`ContainerParseTest`, `TableAlignTest`): 5188 → 4469 and
  5128 → 4409, centred on the 6.27in line.

The suite converts into `createPackage()`, A4 portrait with 1in margins, a 9026-twip line. These
numbers come from before #118 and #119, and phase 1 measures again on top of them.

### 2.3 What the layout's width reaches

The importer reads widths from the layout in only a few places:
- column positions (`TableProperties.getColumnPos`, for `w:gridCol` and `w:tcW`);
- a table's resolved `margin-left` (`w:tblInd`), which is how centred tables move;
- an image's content box (`addImage`, for `wp:extent`);
- #119's room beside a table's margins.

Paragraph indents and spacing come from CSS values, not the layout. Text is reflowed by Word.

## 3. The change

- **`DocxRenderer`** takes a width for the layout. `getInitialExtents` uses it when it's set, and
  openhtmltopdf's first page box otherwise, as now. The height stays the page box's.
- **`XHTMLImporterImpl`** sets the width before each layout: the last section's writable width
  (D2), unless a width was given (D3), plus the body's horizontal margin, border and padding (D1).
  A package with no `sectPr` has no sections in docx4j's `DocumentModel`, so it keeps today's
  width.
- **A document's `@page`** no longer sets the width (D4).

## 4. Phases

### Phase 0: a Word check

One docx, with an A4 portrait section and a Letter landscape one. Each case is converted with
`setTextWidth` giving the width to lay out at: 7.1in, as 17.3.5 laid out before this CR, or the
line. The builder is `LayoutWidthProbeBuilder`, a main class in core-tests, as
`TallRowProbeBuilder` is. Predictions:
- **1a, 1b. A4 portrait, a `width: 100%` table:** at 7.1in and at 6.27in, the two look the same,
  both fitted to the line. This shows that the change doesn't move what portrait documents show.
- **2a, 2b. A4 portrait, a `width: 100%` image:** at 7.10in, it runs 0.83in into the right
  margin; at 6.27in, it fills the line.
- **3a, 3b. A4 portrait, a centred 2in table:** laid out at 7.1in (`w:tblInd` 3652), it sits
  about 0.4in right of centre; at the line (3051), it is centred.
- **4a to 4c. Letter landscape, a `width: 100%` table:** at 7.10in, Word draws it 7.10in, 1.9in
  short of the right margin. At 9.47in, 4a's widths a third larger as 17.3.4 wrote them, Word
  fits it to the 9in line. At 9.00in, as with this CR, it fills the line.

Each section starts with a red-bordered paragraph that spans the text, margin to margin.

**Staged 2026-11-01** on CR-009 phase 1's code (uncommitted, on a1bb15d with #118 and #119):
`~/vbShares/Office 2016/__tmp/xhtml-cr009/round1/cr009-layout-width-r1.docx`, SHA-256
`7e3f784eb5f38e6fef2909255368614d95fdf46bfa68a53f11122ca943b4a087`, with its README. As written:
1a 10229 and 1b 9027 (the columns' sum, in twips); 2a 7.10in and 2b 6.27in wide; 3a `w:tblInd`
3652 and 3b 3051; 4a 10229, 4b 13638 and 4c 12960. A copy is in the archive's `word-check/`.

**Done 2026-11-01.** Jason: "__tmp/xhtml-cr009/round1/ is good i think (3a?)", with Word's PDF
beside the README. Every case is as predicted. Round 3 of core-tests'
`word-check/evidence/word-open-results.md` has the measurements:
- **1a and 1b** are both fitted to the line, 6.27in, margin to margin.
- **2a** is kept at 7.10in and runs 0.83in into the right margin. **2b** fills the line. Word fits
  a table to the text but not an image, so on portrait pages this CR changes what Word shows of a
  `width: 100%` image.
- **3a** sits 0.42in right of centre. **3b** is centred, to within 0.4pt.
- **4a** is kept at 7.10in, 1.88in short of the right margin. **4b**, with 17.3.4's widths, is
  fitted to the 9in line, and **4c** fills it.

### Phase 1: the width

As §3, on top of #118 and #119. Tests (`LayoutWidthTest`):
- on A4 portrait, Letter landscape and A3, a `width: 100%` table's columns add up to the text
  width (to within the border spacing), and `width: 50%` to half of it;
- a `width: 100%` image is the text width;
- a centred table is centred on the line;
- a width given by D3's setter is used;
- the document's `@page` doesn't change the width (D4).

Measured as CR-008's phase 1 was: the suite's converts before and after, listing what changes.

**Done 2026-11-01, uncommitted** (on a1bb15d, with #118 and #119):
- **`DocxRenderer.setTextWidth(int dots)`** (and its getter). When set, `getInitialExtents`
  lays out at that width plus the horizontal margin, border and padding of the html element and
  its body, so the body's content box is that width (D1 (b)). Otherwise it uses openhtmltopdf's
  first page box, as before. The height stays the page box's.
- **`XHTMLImporterImpl.setTextWidth(Integer twips)`** (and its getter) is D3's setter. With null,
  the default, the width is the last section's `getWritableWidthTwips()` (D2 (a)). The importer
  gives it to the renderer before each layout, so the document's `@page` no longer sets the
  width (D4 (a)). A package whose body has no `sectPr` has no sections in docx4j's
  `DocumentModel`, so it keeps openhtmltopdf's page.
- **`LayoutWidthTest`** (6 tests) covers the cases above, and a package with no `sectPr`. Five
  fail on the code without this phase. The sixth, `noSectPr`, holds either way.
- **Measured:** a `git archive` copy with the dump hook, against one with only #118 and #119.
  - 13 converts differ, all in `w:gridCol`, `w:tcW` and `w:tblInd`.
  - Nine are the suite's, the ones §2.2's prototype changed, with the same values: 5203 → 4484
    (×5), 10196 → 8997, and so on; the centred tables' `w:tblInd` 5188 → 4469 and
    5128 → 4409.
  - Four are `TableMarginTest`'s percentage widths, now of the 6.27in line.
  - `LayoutWidthProbe` gives the prototype's widths on every page set-up, except that
    `width: 50%` with `margin-left: 10%` is now 50% of the line (#119).
- **Suite** 210/0/0, BundleIT 3/0/0, and the enforcer rules passed.

The probes are in the archive's `out/` (`*-phase1*`).

### Phase 2: the release

The CHANGELOG, and the release (D6).

## 5. Decisions (for Jason; each with a recommendation)

Decided 2026-11-01 (Jason: "CR-009's decisions, D1 to D6 as recommended").

- **D1. The width.**
  - (a) The docx's text width.
  - (b) The text width plus the body's horizontal margin, border and padding, so that the body's
    content box is the docx's text.

  **Recommended: (b).** `width: 100%` then fills the line, which it doesn't under (a). It is also
  #118's rule: the body's margin, border and padding lie outside the docx's text, as the page's
  margins do.

  **Decided 2026-11-01 (Jason): (b).**
- **D2. Which section.**
  - (a) The last section.
  - (b) The first.

  **Recommended: (a).** That's where `convert`'s content usually goes, at the end of the body. It
  is also the section docx4j fits an image with no size to (`BinaryPartAbstractImage`).

  **Decided 2026-11-01 (Jason): (a).**
- **D3. A width for content bound elsewhere,** such as a table cell, a header, or a section other
  than the last.
  - (a) A setter on `XHTMLImporterImpl`, in twips, with null for the last section's width.
  - (b) None.

  **Recommended: (a).** Without it, content bound for a narrower place is laid out at the
  section's width, as it is laid out at 7.1in today. The setter would go on
  `XHTMLImporterImpl` only. Adding it to the `XHTMLImporter` interface would break other
  implementations.

  **Decided 2026-11-01 (Jason): (a).**
- **D4. The document's `@page`.**
  - (a) The docx's section sets the width, whatever `@page` says.
  - (b) `@page` sets it, where the document has one.

  **Recommended: (a).** Word lays the docx out on its own sections, and a `@page` size describes
  a printed page.

  **Decided 2026-11-01 (Jason): (a).**
- **D5. Phase 0.**
  - (a) Run it alongside phase 1, before the release.
  - (b) Run it after the release.
  - (c) Skip it, and rely on CR-008's phase 0.

  **Recommended: (a).** CR-008's phase 0 covers tables, but nothing yet shows what Word does with
  an image wider than the text, and the CHANGELOG shouldn't claim more than has been seen.

  **Decided 2026-11-01 (Jason): (a).**
- **D6. The release.**
  - (a) 17.3.5.
  - (b) The next release, with a 17.3.5 CHANGELOG note on wide pages, giving `@page` as the way
    round.

  **Recommended: (a).** Without this CR, 17.3.5 ships a regression for landscape and A3 pages.

  **Decided 2026-11-01 (Jason): (a).**

## 6. Risks

- **Ordinary documents change.**
  - On portrait pages, a table that fills its line goes from 7.08in to the line's width. Word
    already drew it that wide. `%` widths narrow: `width: 50%` goes from 3.53in to 3.11in on an
    A4 page.
  - On wider pages, those tables widen.
  - Images with a `%` width or `max-width` change size.
  - Centred tables move to the centre.
- **Content bound for elsewhere** is laid out at the last section's width, which may not be its
  own. D3's setter covers this. Today it gets 7.1in, whatever its place.
- **A document's `@page`** no longer changes the layout (D4 (a)). Anyone who used it to get a
  landscape width now gets that from the docx's section instead.
- **Word not checked** for images, and for the landscape cases, until phase 0. It showed what was
  predicted (§4).

## 7. Sources

- CR-008 §2.4, D6, and phase 0's results (4a to 5b).
- Phase 0's results: round 3 of `docx4j-ImportXHTML-core-tests/src/test/resources/word-check/evidence/word-open-results.md`.
- `DocxRenderer.getInitialExtents`; openhtmltopdf's `Layer.createPageBox`; docx4j's
  `PageDimensions.getWritableWidthTwips` and `DocumentModel`.
- #118, #119; openhtmltopdf/openhtmltopdf#59.
- `~/claude-scratch-archive/docx4j-ImportXHTML/layout-width-2026-11-01/` (README; `src/`, `out/`).
