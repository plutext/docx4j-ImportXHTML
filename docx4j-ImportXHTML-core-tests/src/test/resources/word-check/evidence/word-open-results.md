# Word-open results

Word checks of docx4j-ImportXHTML's output, one dated section per round, newest last. Each round's
predictions are in its CR before the run. Word's own files (its PDF or re-save) stay on the share,
`~/vbShares/Office 2016/__tmp/`, and aren't committed. The conventions are docx4j's (its
`docx4j-core-tests/src/test/resources/text-extraction/phase0/evidence/word-open-results.md`).

## Round 1: CR-007 and CR-008 phase 0, 28 October 2026

- Specimen: `word-check-cr007-cr008.docx`, SHA-256
  `47b92811fdc715a7fa10c1387d03f591a07477b533dc67b8d21f782070231aae`. Built by a scratch program,
  `WordCheck.java`, archived with the docx in
  `~/claude-scratch-archive/docx4j-ImportXHTML/cr008-units-2026-10-28/` (no builder in this tree
  then). Each case was converted at c162792, then the CR's value was set by hand.
- Word: Word for Microsoft 365, as the PDF's producer says. The version and build weren't
  recorded. No prompt or repair was reported.
- Returned: the document saved as PDF, `__tmp/word-check-cr007-cr008.pdf`, SHA-256
  `9799b8e22edd696793d94e5b20891f8d24bcdc3f46c611113ee6e87b44a52053`. It was measured by rendering
  it at 720 dpi and scanning for ink (the archive's `src/runs.py`, about 0.1pt), with pdftotext for
  the lines on each page.

| Case | Looked for | Word 365 |
|---|---|---|
| 1a `w:trHeight` 45600 atLeast, 85 lines | cut off? | read as exact (996.8pt); the row starts a new page, doesn't break, and runs off the paper; lines 1 to 27 show, 28 to 85 on no page |
| 1b 32767 atLeast | every line | all 85, across four pages (24, 24, 24, 13); but the row starts on the page after its heading, and the cell runs on, empty, about 348pt below line 85 to the bottom margin |
| 2a `w:space` 45 | read as 13? | border 18.0pt nearer the text than 2b's, across and down: 13 |
| 2b `w:space` 31 | 31 | 31 |
| 3a border `w:sz` 400 | read as 144 (18pt)? | 18.0pt |
| 3b `w:sz` 255 | 31.9pt | 31.7pt |
| 4a table 6in written 5760 + 5760 | 8 in? | 6.27 in, the text's width |
| 4b 4320 + 4320 | 6 in | 6.02 in (with the borders) |
| 5a a table that fills its line, 13414 + 220 | kept, or fitted to the text? | fitted: 6.27 in, ending at the right margin |
| 5b 10061 + 165 | as 5a | fitted, as 5a |
| 6a `w:tblCellSpacing` 133 | gap twice the value? | 13.0 to 13.5pt between cells, and between the table's border and a cell |
| 6b 100 | 10pt | 9.6 to 10.1pt |
| 7a p double border `w:sz` 32 | each line's width? | two 3.9pt lines, 3.8pt apart (11.6pt) |
| 7b `w:sz` 8 | 3pt in all | two 1.0pt lines, 0.9pt apart (2.9pt) |

Recorded in CR-007 and CR-008, §4, phase 0.

## Round 2: CR-007 D6, 28 October 2026

- Specimen: `__tmp/xhtml-cr007/round2/cr007-tall-row-r2.docx`, with its `README.txt`. SHA-256
  `5a9d84bf3660afc292f5696ce36d8f3ddeef63706994286b5a092066dd2d5542`. Built by
  `TallRowProbeBuilder` (this module's test tree, uncommitted then) on 80e4a12. Case A's row was
  written 45600 and set to 32767; case D's is as written, 21690.
- Predictions: CR-007 §4, round 2.
- Word: Version 2608 (Build 20326.20232). No prompt or repair was reported.
- Returned: Jason's notes, `__tmp/xhtml-cr007/round2/result.txt` (A, B and D "Correct"; C "not
  sure about the 6.9 in, but there is empty space below line 85"), and the document saved as PDF,
  `cr007-tall-row-r2.pdf`, SHA-256
  `ebc4249fc3c075186070067f761551da7caa6e2646f69ec93a96f562e2147bff`. Measured as round 1. "Empty"
  is from the last line's text to the cell's bottom border, less B's 17.1pt, the ordinary gap.

| Case | Looked for | Word 365 |
|---|---|---|
| A 85 lines, 32767 atLeast | as round 1's 1b | as 1b: the heading's page is otherwise empty; 24, 24, 24, 13 lines; the cell runs on to the bottom margin (770.2pt), 330.6pt empty; R2A_AFTER on the next page |
| B 85 lines, no `w:trHeight` | starts under its heading, ends at line 85 | yes: 23 lines under the heading, then 24, 24, 14; the cell ends 17.1pt below line 85, with R2B_AFTER straight after |
| C 85 lines, 10000 atLeast (500pt) | last part at least 500pt (H1), or none (H2)? | H1: lines as B, but the last part is 503.7pt tall (72.0 to 575.7pt), 108.0pt empty below line 85; R2C_AFTER after it on the same page |
| D 40 lines, 21690 atLeast (as written today) | as A | as A: the heading's page is otherwise empty; 24 and 16 lines; the cell runs on to the bottom margin, 246.6pt empty; R2D_AFTER on the next page |

Recorded in CR-007 §4, round 2, and §5, D6.
