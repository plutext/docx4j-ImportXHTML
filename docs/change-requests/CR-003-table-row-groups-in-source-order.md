# CR-003: a table's row groups in source order, as Word imports an altChunk (docx4j CR-053's round 11)

Status: DONE 2026-10-23, released in docx4j-ImportXHTML 17.3.4 (§6). Accepted 2026-10-21 (Jason:
"decisions agreed."), D1 to D4 as recommended. Phase 1 implemented 2026-10-21 (suite 146/0/0; §9),
commit dd7b913, and installed as 17.1.1-SNAPSHOT. §4's Word round (docx4j CR-053's round 12), which
D2 asked for before it ships, confirms source order in all six cases. Proposed 2026-10-21, at
Jason's request ("draft CR-003"), after docx4j-25's report from docx4j CR-053's round 11 Word probe
(r11-8). Drafted by Claude Opus 5.5 in the docx4j-ImportXHTML session, with openhtmltopdf's
reordering measured and the fix prototyped first (§2, §3).
Scope: `XHTMLImporterImpl` (a setter, and the loop over a table's children) and `TableHelper` (one
method). Nothing changes unless the option is set.
Branch: `VERSION_17_3_4`; released with CR-002 as 17.3.4, if decided in time (D4).
Phases (§6): 1. the option; 2. the release.

## 1. Background

docx4j's CR-053 probes how Word 365 imports HTML altChunks, so that docx4j's text model and its
`convertAltChunks` can follow Word. Round 11's r11-8 (Jason's Word 365 save, 21 October 2026) was
this chunk:

```html
<table border='1'><thead><tr><td>R11_8_HEAD</td></tr></thead>
<tfoot style='display:none'><tr><td>R11_8_FOOT</td></tr></tfoot>
<tbody><tr><td>R11_8_BODY</td></tr></tbody></table>
```

- **Word keeps the source order.** Its save (`r11-8-tfoot-word365.docx`, in
  `~/vbShares/Office 2016/altchunk-fun/round11/`) has three `w:tr`s:
  - HEAD, with `w:trPr/w:tblHeader`;
  - FOOT, with `w:trPr/w:hidden` and `w:vanish` on its run and paragraph mark;
  - BODY.
- **The importer moves the footer to the end.** Through docx4j (jsoup, `W3CDom`, `convert(Node)`
  with CR-002's `setDisplayNoneAsHidden(true)`), at 468a760, the rows are HEAD, BODY, FOOT. FOOT is
  hidden as Word hides it, so only the order differs.

For text extraction and `convertAltChunks`, Word's order is the one that counts. The text extraction
specification now requires it (WD 0.28, `docs/text_extraction_specification_wd.md` in
docx4j-portfolio, f2046e5). Its REQ-513 expands an HTML chunk with "a table's row groups in source
order (a `tfoot` where it stands, not moved last as a browser draws it)", and keeps a hidden `tfoot`
with its text hidden.

docx4j can't restore the order in the rewrite it does before calling. A `display: table-row-group`
on the `tfoot` would stop it moving. But under CR-002's D11, an explicit author `display` other than
`none` re-shows an element inside a hidden one.

The importer follows the browsers here. CSS 2.1 §17.2 has a table-header-group drawn before all
other row groups and a table-footer-group after them. Only the first of each counts; any others are
ordinary row groups. HTML 4.01 required `tfoot` to come before `tbody` in the source, so that a
browser could draw the footer before the body's rows arrived. HTML5 also allows it after.

## 2. What the importer does today (measured)

openhtmltopdf's `BoxBuilder.reorderTableContent` (1.1.31, BoxBuilder.java:569) rebuilds each
`TableBox`'s children as:
1. the first table-header-group, if any;
2. the other row groups, in source order;
3. the first table-footer-group, if any.

The importer's `traverse` visits a table's children in that order. A scratch probe ran each table
below through the importer at 468a760, with `setDisplayNoneAsHidden(true)`. Its last column is the
prototype (§3) with the option on.

| Source order | Today | With the option |
|---|---|---|
| thead, tfoot (`display:none`), tbody (r11-8) | HEAD, BODY, FOOT(hidden) | HEAD, FOOT(hidden), BODY, as Word |
| tfoot, tbody (HTML 4.01's order) | BODY, FOOT | FOOT, BODY |
| tbody, thead | HEAD, BODY | BODY, HEAD |
| tfoot F1, tbody, tfoot F2 | BODY, F2, F1 | F1, BODY, F2 |
| tbody B1, thead H1, thead H2, tbody B2 | H1, B1, H2, B2 | B1, H1, H2, B2 |
| tbody B1, tfoot, tbody B2 | B1, B2, FOOT | B1, FOOT, B2 |
| tfoot, then rows directly in the table | R1, R2, FOOT | FOOT, R1, R2 |
| a row, a tfoot, a row (no tbody) | R1, R2, FOOT | R1, FOOT, R2 |
| tbody `display: table-footer-group`, tbody | BODY, CSSFOOT | CSSFOOT, BODY |
| tfoot with a rowspan, tbody | B1+B2, FA+FB, (merged)+FC | FA+FB, (merged)+FC, B1+B2 |
| captioned: caption, tfoot, tbody | CAP, BODY, FOOT | CAP, FOOT, BODY |
| a table in a td: tfoot, tbody | IBODY, IFOOT | IFOOT, IBODY |

The importer's only use of the table-header-group is in the order. It doesn't set
`w:tblHeader` from a `thead` (§5).

## 3. The change (prototyped)

- **`setTableRowGroupsInSourceOrder(boolean)`**, with `isTableRowGroupsInSourceOrder()`. It is false
  by default and `@since 17.3.4`, beside `setDisplayNoneAsHidden`.
- **In `traverse`'s loop over a block's children:** when the option is set and the box is a
  `TableBox`, the loop takes `TableHelper.rowGroupsInSourceOrder(table)` in place of
  `getChildren()`.
- **`rowGroupsInSourceOrder`** places each row group by the child of the `table` element it comes
  from:
  - a `thead`, `tbody` or `tfoot`: its own element;
  - an anonymous group (rows directly in the table): its first row's element, or that row's first
    cell's.

  It sorts the groups by those positions with a stable sort. If a group can't be placed, it returns
  openhtmltopdf's order unchanged. Nothing else moves: rows within a group, and cells within a row,
  keep their order.

The prototype is 72 lines in the two files.

### What it can't disturb

- **Merged cells:**
  - A `rowspan` can't cross a row group: openhtmltopdf's grid (`TableSectionBox.cellAt`) is per row
    group.
  - `TableHelper.insertDummyVMergedCells` looks only in the cell's own row group.
  - Measured: a `tfoot` with a `rowspan` gives the same `w:vMerge` restart and continue, in either
    order.
- **Cell borders:**
  - The importer writes each cell's own borders and doesn't resolve collapsed borders between
    neighbours.
  - Measured: a 4px red `tfoot` border next to a 1px black `tbody` border gives the same
    `w:tcBorders` in either order.
- **Layout positions:** the box tree's positions aren't used.
- **Hidden row groups:** CR-002's pre-pass lays out a hidden `tfoot` with its default display,
  `table-footer-group`. It moves like a visible one, and the option puts it back.
  `getHiddenContent()` is unaffected: r11-8's key is the `tfoot`, with or without the option.

### Checks on the prototype

- **The suite with the option on by default** (flipped temporarily): 140/0/0. No existing test
  depends on the moved order.
- **r11-8's own `chunk1.htm` the way docx4j reads it:** jsoup, `W3CDom`, then `convert(Node)` with
  `setExternalResourcesAllowed(false)` and `setDisplayNoneAsHidden(true)`.
  - Option off: HEAD, BODY, FOOT(hidden).
  - Option on: HEAD, FOOT(hidden), BODY, as Word's save.

docx4j would reach the setter by reflection next to `setDisplayNoneAsHidden`. On an older importer
it would fall back to today's order.

## 4. Word's behaviour beyond r11-8 (a probe round, before D2)

r11-8 measures one case: a hidden `tfoot` between a `thead` and a `tbody`. Each of these would come
out differently with the option on than today. Word should be checked on them before the option's
scope is settled:

1. a visible `tfoot` before the `tbody` (HTML 4.01's order);
2. a `thead` after the `tbody`;
3. two `tfoot`s, one before the `tbody` and one after;
4. two `thead`s;
5. a `tbody` with `style='display: table-footer-group'`;
6. rows directly in the `table`, around a `tfoot`.

Word's HTML import never draws a table by CSS's table model. If it keeps source order in every
case, as r11-8 suggests, the option is §3 as prototyped. Where Word differs, the option follows
Word. The same files would show where Word puts `w:tblHeader` (§5).

docx4j-25 builds the round, as docx4j CR-053's round 12, in the same way as round 11 (Jason, 21
October 2026: "let docx4j-25 do it").

### Round 12's results (Jason's Word 365 save, 21 October 2026)

`r12-table-row-groups.docx` holds each case as its own chunk, and Jason's save is
`r12-table-row-groups-word365.docx`. Both are in `~/vbShares/Office 2016/altchunk-fun/round12/`.
Word keeps source order in every case, which is what the option gives (§2's last column). The
importer with the option, through docx4j, gives the same order in all six (docx4j-25).

| Case | Word 365's rows (T: `w:tblHeader`) |
|---|---|
| 1. thead, visible tfoot, tbody | HEAD (T), FOOT_VISIBLE, BODY |
| 2. tbody, thead | BODY, HEAD_AFTER_BODY (T) |
| 3. tfoot, tbody, tfoot | FOOT_BEFORE, BODY, FOOT_AFTER |
| 4. tbody, thead, thead, tbody | B1, H1 (T), H2 (T), B2 |
| 5. tbody as `table-footer-group`, tbody | BODY_AS_FOOTER, BODY_PLAIN |
| 6. row, tfoot, row (bare in the table) | R1, FOOT, R2 |

- **Word ignores the CSS footer group** (case 5), as it ignores CSS's table model generally.
- **Word marks every `thead`'s rows `w:tblHeader`, wherever the `thead` stands**: below a `tbody`
  (case 2) and in the middle (case 4), as well as at the top. That is for D3's later CR (§5).
- **No row is `w:hidden`;** nothing in round 12 is hidden.

## 5. Found on the way

- **Word marks a `thead`'s rows `w:tblHeader`** (repeat as a header row at the top of each page),
  as r11-8's save shows.
  - The importer doesn't. Its only `w:tblHeader` rule is the property
    `docx4j-ImportXHTML.Tables.th.to.tblHeader` (default false). That marks any row with a `th`
    after its first cell, whatever its row group.
  - `w:tblHeader` changes the document's look, not its text. Nobody has asked for it (D3).
  - Round 12 (§4) adds that Word marks a `thead`'s rows wherever the `thead` stands, even below a
    `tbody`.
- **CR-002's documentation of hidden rows leaves out `tfoot`.** The `setDisplayNoneAsHidden`
  javadoc and the CHANGELOG say a row is hidden when its "tr, thead, tbody or table" is. The code
  (`DisplayNoneHelper.row`) checks `tfoot` too. A documentation fix, in phase 1.

## 6. Phases

### Phase 1: the option

§3, as D1 and D2 decide. Tests (core-tests, a new `TableRowGroupOrderTest`):
- §2's cases with the option off: today's order exactly;
- the same with it on: source order, or Word's where §4 says otherwise;
- the `rowspan` case: the same `w:vMerge`s in either order;
- r11-8's chunk through jsoup and `convert(Node)` with `setDisplayNoneAsHidden(true)`: HEAD,
  FOOT(hidden), BODY, and one key, the `tfoot`.

Also §5's `tfoot` fix in the javadoc and the CHANGELOG, and a CHANGELOG entry for the option. Then a
reinstalled 17.1.1-SNAPSHOT for docx4j, and docx4j-25 told.

### Phase 2: the release

With CR-002's phase 4 as 17.3.4 (D4), or else in the next release.

**Done 2026-10-23:** released in 17.3.4 with CR-002's phase 4 (D4). CR-002 §8 has the release.

## 7. Decisions (for Jason; each with a recommendation)

All four decided as recommended on 21 October 2026 (Jason: "decisions agreed.").

- **D1. The default.**
  - (a) Off. Only callers who ask for Word's order, such as docx4j for altChunks, get it.
  - (b) On, for every caller.

  **Recommended: (a).** Browsers, and CSS, draw the first `tfoot` last. HTML 4.01 required `tfoot`
  before `tbody`, so a great deal of HTML has its footer first in the source. With (b), each of
  those tables would come out footer first, a visible change for callers who never asked to look
  like Word. `setDisplayNoneAsHidden` set the same pattern: Word's behaviour behind an option that
  docx4j sets. REQ-513 (§1) is met as long as docx4j sets the option for its chunks.
- **D2. Which row groups.**
  - (a) Every row group in source order, as prototyped, except where §4's round shows Word moving
    one.
  - (b) Only `tfoot`, leaving the first `thead` at the top.
  - (c) Decide after §4's round.

  **Recommended: (a), confirmed by §4's round before phase 1 ships.** "Source order" is one rule,
  and r11-8 suggests it is Word's. (b) would be a guess about `thead`, which Word hasn't been asked
  about.
- **D3. `w:tblHeader` for a `thead`'s rows (§5).**
  - (a) Not in this CR; recorded for a later one.
  - (b) In this CR, under the same option.
  - (c) In this CR, under its own option.

  **Recommended: (a).** It changes how the table looks, not what it says. docx4j's text model
  doesn't need it, and nobody has asked for it. A `thead` that ends up below a `tbody` would also
  raise questions of its own (Word repeats header rows only from the top of a table). §4's round
  would show what Word does, ready for that CR.
- **D4. The release.**
  - (a) In 17.3.4 with CR-002, if phase 1 is done before that release; 17.3.4 isn't held for it.
  - (b) The next release after 17.3.4.

  **Recommended: (a).** docx4j's CR-053 targets 17.3.4, and the change is small and off by default.
  If §4's round comes late, 17.3.4 goes without it.

## 8. Risks

- **Word's order is measured for one case** (§4). Until the round, the other cases rest on r11-8
  and on Word's HTML import not drawing tables by CSS's table model.
- **A row group that can't be placed** (a box tree shape not seen here) leaves the whole table in
  openhtmltopdf's order. The table is then laid out exactly as today, not reordered in part.

## 9. Implementation notes (2026-10-21)

As the prototype (§3), with:

- **A debug log** when a row group can't be placed and openhtmltopdf's order is kept.
- **The javadoc** names what counts as a row group: a CSS `table-header-group` or
  `table-footer-group` moves as a `thead` or `tfoot` does, and rows directly in the table form one.
  It says that only the groups' order changes, and that a hidden `tfoot` stays where it is too.
- **§5's `tfoot`** is added to the hidden-row wording in `setDisplayNoneAsHidden`'s javadoc and
  the CHANGELOG.
- **`TableRowGroupOrderTest`** (6 tests):
  - default off;
  - §2's eleven cases, with the option off and on;
  - every row's XML the same either way, in a different order, so merges and borders are unchanged;
  - the `rowspan`'s `w:vMerge`s;
  - a table in a `td`;
  - r11-8 through jsoup and `convert(Node)`, with HEAD, FOOT (`w:hidden`), BODY, and one key, the
    `tfoot`.

§4's Word round (round 12) confirms source order in all six cases, so the tests stand as they are.
