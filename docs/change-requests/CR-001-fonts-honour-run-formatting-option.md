# CR: Fonts should honour the run-level FormattingOption

Status: PROPOSED (2026-09-11) — options surveyed; decisions pending (see end)
Scope: `XHTMLImporterImpl.formatRPr` only — where a text run's `w:rFonts` is
set from CSS `font-family`.  `FontHandler`'s mapping tables are unchanged; the
fonts `ListHelper` puts on list-number symbols are out of scope.
Branch: current line only (`VERSION_17_0_5`).  No backport to 8.3.x
(Jason, 2026-09-11).
Related: docx4j CR-013 (configurable importer formatting options from OpenDoPE
binding), which surfaced this.
Phases: 1. code + property + docs; 2. tests; 3. record the dependency in
docx4j CR-013

## Background

### The documented contract

`FormattingOption` (javadoc, and `ImportXHTMLProperties`):

> CLASS_TO_STYLE_ONLY: a Word style matching a class attribute will be used,
> **and nothing else**
> CLASS_PLUS_OTHER (the default): a Word style matching a class attribute will
> be used; other css will be translated to direct formatting
> IGNORE_CLASS: css will be translated to direct formatting

The importer honours this for every run property except one.  In
`formatRPr(RPr, cssClass, cssMap)` the `runFormatting` switch decides whether
`addRunProperties` (CSS → direct `rPr`) runs at all, but *after* the switch,
unconditionally:

    // Font is handled separately.  TODO: review this
    PropertyValue fontFamily = cssMap.get("font-family");
    FontHandler.setRFont(fontFamily, rPr);

So under `CLASS_TO_STYLE_ONLY` a run still receives direct `w:rFonts`.

### How that came about

- 5b69f1d (2013-07-25, "addFontMapping") added the font line at the end of
  `addRun`, after the then-unconditional `addRunProperties`.  "Handled
  separately" meant only "not inside addRunProperties".
- ee90492 (2013-08-05) introduced `FormattingOption`, moved the run logic into
  `formatRPr` and wrapped `addRunProperties` in the switch — but left the font
  call where it sat, below the switch, and changed the comment to
  "TODO: review this".  The TODO has never been revisited.

Nothing in the history records a reason.  The most charitable reading is that
fonts were seen as glyph coverage rather than formatting (the `dev_docs/fonts.txt`
written with 5b69f1d is entirely about CSS3 font matching), but the importer
never checks glyphs ("assumes the first font family for which we have a mapping
will contain a glyph"), so that policy was never carried through.  The evidence
points to an accident of ordering.

### Why it bites harder than a stray property

1. **`font-family` is always present.**  `getCascadedProperties` walks every
   CSS property id and takes `valueByName`, which resolves through the parents,
   so the map holds a *computed* value for every property.  Unstyled XHTML
   gets openhtmltopdf's default, `serif`.
2. **A mapping is nearly always found.**  `FontHandler`'s static init maps
   `serif` / `sans-serif` / `monospace` to configurable defaults (Times New
   Roman / Arial / Courier New) and every Microsoft font name to itself.
3. **The cleanser never sees it.**  `RPrCleanser.removeRedundantProperties`
   (strip direct formatting that merely repeats the style) runs at the tail of
   `addRunProperties`; `setRFont` runs after that, and under
   `CLASS_TO_STYLE_ONLY` `addRunProperties` doesn't run at all.

Net effect today, per option, for a run's font:

| runFormatting | @class → rStyle | other CSS → rPr | `w:rFonts` |
|---|---|---|---|
| CLASS_TO_STYLE_ONLY | yes | no | **yes, always** (Times New Roman for unstyled XHTML) |
| CLASS_PLUS_OTHER | yes | yes | yes |
| IGNORE_CLASS | no | yes | yes |

"Styles only" is therefore unattainable for fonts.  In docx4j's OpenDoPE
binding (CR-013) it is worse still: with `PrioritiseRPr=false` docx4j wraps
the XHTML in `<div style="font-family:'Calibri' ...">` derived from the content
control, so every imported run carries `w:rFonts` for the control's font — and
a docx4j-side guard that drops the wrapper merely swaps that for Times New
Roman on every run.  The fix has to be here.

## Options

### A. Move the font call inside the CSS-translating branches (recommended, with B)

`setRFont` becomes part of "other css → direct formatting": called under
`CLASS_PLUS_OTHER` and `IGNORE_CLASS`, not under `CLASS_TO_STYLE_ONLY`.  A
three-line move; the documented contract is then true for fonts.

Pro: the fix the documentation already promises; no new API.
Con: a behaviour change for existing `CLASS_TO_STYLE_ONLY` users, who have
been getting `w:rFonts` on every run since 2013 (by accident, but some
documents will look different: text now takes the style's font, or the
document default, instead of Times New Roman / the CSS font).

### B. A, plus a legacy opt-out property

    docx4j-ImportXHTML.Fonts.IgnoreRunFormattingOption=false

(name to taste; default false = new behaviour).  `true` restores today's
unconditional `setRFont`.  Read via `ImportXHTMLProperties` like the other
font keys (`docx4j-ImportXHTML.fonts.default.*`).  Cheap insurance against
the con of A; documented as deprecated-from-birth so it can go later.

### C. A fourth knob: `setFontFormatting(FormattingOption)`

Rejected.  `FormattingOption` is about mapping `@class` to a *style*; a font
has no class→style analogue, so only two states make sense (apply CSS font /
don't).  A boolean property (B) says the same thing without inventing API.

### D. Emit `w:rFonts` only when `font-family` was actually specified

i.e. distinguish a *specified* value (author CSS, or an inherited author
value) from the renderer's computed default, so unstyled XHTML would stop
acquiring Times New Roman under *every* option.  Needs the cascaded
(specified) properties rather than `getderivedValuesById` — the commented hint
in `getCascadedProperties` (`getCascadedPropertiesMap(e)`) points the way, and
inheritance has to be re-implemented for `font-family`.

Not this CR: it changes `CLASS_PLUS_OTHER` and `IGNORE_CLASS` output for the
common case, which some users may rely on (HTML's default serif → Times New
Roman is defensible).  Recorded as a possible follow-up; it composes with A/B.

### E. Leave ImportXHTML alone; strip `w:rFonts` in docx4j after import

Would fix the binding case only, not direct callers, and encodes the accident
as an API.  Rejected as a fix; noted only as an interim if release timing ever
forces it.

## Recommendation

B (A + opt-out property).  Ship in 17.0.5.

## Plan

### Phase 1 — code, property, docs

- `XHTMLImporterImpl.formatRPr`: move the `font-family` / `setRFont` call into
  the `IGNORE_CLASS` branch and the `CLASS_PLUS_OTHER` sub-branch; guard with
  the property so `true` keeps today's placement.  Delete the 2013 TODO.
  Bump the file's Plutext copyright end-year (per CLAUDE.md).
- `ImportXHTMLProperties`: add the key to the javadoc list of supported keys.
- `docx4j-ImportXHTML-samples/src/main/resources/docx4j-ImportXHTML.properties`:
  the key, commented, with a one-line explanation.
- `CHANGELOG.md` (17.0.5): "CLASS_TO_STYLE_ONLY now applies to fonts too
  (previously CSS font-family always became w:rFonts); set
  `docx4j-ImportXHTML.Fonts.IgnoreRunFormattingOption=true` for the old
  behaviour."

### Phase 2 — tests (`docx4j-ImportXHTML-core-tests`, `org.docx4j.convert.in.xhtml.tests`)

New `RunFormattingFontTest`, following `NumberingTest`'s pattern (fresh
`XHTMLImporterImpl` per case, `set*Formatting`, `convert(xhtml, "")`):

1. `CLASS_TO_STYLE_ONLY`, `<span class="Strong" style="font-family:Arial">`
   → `w:rStyle=Strong`, **no** `w:rFonts`.
2. `CLASS_TO_STYLE_ONLY`, XHTML with no CSS at all → no `w:rFonts`
   (the Times New Roman default no longer leaks).
3. `CLASS_PLUS_OTHER`, `font-family:sans-serif` → `w:rFonts` Arial (unchanged).
4. `IGNORE_CLASS`, same → `w:rFonts` Arial (unchanged).
5. Property `true` + `CLASS_TO_STYLE_ONLY` → `w:rFonts` present (legacy).
   Set via `ImportXHTMLProperties.setProperty` and reset in `@After`.

Then the full suite: no existing test asserts `w:rFonts` under
`CLASS_TO_STYLE_ONLY` as far as a grep shows (`NumberingTest` uses it for
*paragraph* formatting only), but confirm by running it.

### Phase 3 — docx4j CR-013

Record there that "styles only" for fonts in OpenDoPE binding needs
ImportXHTML ≥ 17.0.5, and that the docx4j-side wrapper guard is then merely
tidy (no `font-family` to leak) rather than necessary.  Done in the same
session this CR was written (CR-013 interaction 1 / decision 2 amended).

## Risks

- **Visible change for `CLASS_TO_STYLE_ONLY` users**: text takes the style's /
  document's font.  That is what the option promises; the opt-out covers
  anyone who disagrees.
- **Glyph coverage**: a document whose styles lack a font for, say, CJK text
  will now show fallback glyphs where it used to get a CSS font.  Under
  "styles only" the template's fonts are the author's responsibility; the same
  is already true of every other run property.
- **List-number symbol fonts** (`ListHelper`, Symbol/Wingdings bullets) are
  untouched — they come from the list style, not from CSS `font-family`.

## Decisions needed

1. Property name / default: `docx4j-ImportXHTML.Fonts.IgnoreRunFormattingOption`
   defaulting to `false`?
2. Is option D (specified-vs-computed `font-family`) wanted as a follow-up
   CR, or is "HTML default serif → Times New Roman" intended behaviour?
