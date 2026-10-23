# Dependencies: status

Checked 23 October 2026 for the 17.3.4 release, on `VERSION_17_3_4` at 24439a2, at Jason's
request. He had run Maven's versions plugin, which listed four newer versions, and asked "please
determine whether we should bump to some or all of these updates". Measured by Claude Opus 5.5 in
the docx4j-ImportXHTML session; the probes are archived in
`~/claude-scratch-archive/docx4j-ImportXHTML/dependency-bumps-2026-10-23/` (its `README.txt`
says how each was run). The release notes and advisories were read the same day (sources in §6).

## 1. Summary

None of the four goes into 17.3.4.

| dependency | pinned | newest | 17.3.4 | why |
|---|---|---|---|---|
| `io.github.openhtmltopdf:openhtmltopdf-core`, `-pdfbox` | 1.1.31 | 1.1.100 | stays | one `<br/>` gives two `w:br` (§3) |
| `org.apache.james:apache-mime4j-core` | 0.8.13 | 0.8.15 | stays; next release | an MHT of more than 512 parts fails (§4) |
| `org.jsoup:jsoup` (optional) | 1.22.1 | 1.23.2 | stays | docx4j 17.3.4 pins 1.22.1, and neither advisory reaches either project (§5) |

The versions plugin listed nothing else. The rest of -core's direct dependencies are current:
- pdfbox, xmpbox and fontbox 3.0.8. docx4j 17.3.4 pins fontbox 3.0.8, and docx4j-bundle holds
  fontbox and pdfbox-io at 3.0.8;
- docx4j-core 17.3.4 (docx4j 17.3.5 is a snapshot).

## 2. How it was checked

- **A build with the bumps.** In a copy of HEAD (`git archive`), openhtmltopdf went to 1.1.100
  and mime4j to 0.8.15, together. `mvn clean verify -Dgpg.skip` passed:
  - the enforcer (bytecode 11, dependency convergence);
  - the suite, 166/0/0;
  - `BundleIT`, 3/0/0.

  jsoup 1.23.2 wasn't built or run against the suite.
- **The output compared.** Ten inputs were converted on 1.1.31 and on 1.1.100, and the main
  document and numbering parts diffed:
  - two from core-tests (`numbering/*.html`);
  - seven from the samples' `sample-docs`;
  - `css-mix.xhtml`, written for this: margins, borders, units, lists, a table with a thead,
    `pre`;
  - `br.xhtml`, five `<br/>` in paragraphs, a `div` and a table cell.
- **jsoup:** the parse and `W3CDom` path timed on hostile nesting (§5).
- **mime4j:** `convertMHT` run on MHTs of 10 to 600 parts (§4).

## 3. openhtmltopdf 1.1.31 → 1.1.100: stays

The tests pass, but 7 of the 10 outputs change.

- **One `<br/>` gives two `w:br`.** `br.xhtml` has 5 breaks on 1.1.31 and 10 on 1.1.100. The
  soft return in `sample-docxv2.html` is doubled, and `Image-Samples.html` goes from 2 to 4. Every
  user with a `<br/>` would see it. The cause wasn't traced. The candidate is 1.1.97's box-tree
  change: `::before`/`::after` content is now laid out inside the inline element's `InlineBox`
  (PR 242).
- **thead paragraphs gain `w:keepLines`.** 1.1.79 adds `page-break-inside: avoid` to `thead` in
  openhtmltopdf's `XhtmlNamespaceHandler.css`. docx4j's `KeepLines` is that property, so every
  paragraph in a header row gets it: 15 paragraphs in three of the inputs.
  - So openhtmltopdf's own default stylesheet is in effect. -core's copy in
    `src/main/resources` has no such rule. Whether -core's copy is read at all
    (`xhtmlrenderer.conf` sets `xr.css.user-agent-default-css = /`) wasn't checked.
- **Row heights and widths move slightly.** Some `w:trHeight` change by 15 or 30 twips (0.75 or
  1.5 pt), and two rows by 75. Two columns' `w:tcW` and `w:gridCol` change by 7 twips. This fits the release
  notes' font-metric and `line-height` changes (1.1.58, 1.1.67), but wasn't traced.
- **Colours with alpha.** From the release notes; not measured. `rgba()`, `hsl()` and `hsla()` now
  parse, where 1.1.31 dropped those declarations. `FSRGBColor` carries alpha, but -core's
  `FSColorToHexString` and `TableHelper` ignore it, so a transparent colour such as
  `rgba(0,0,0,0)` may come out black (PRs 151, 189).

In its favour:
- **PDFBox:** it declares 3.0.8, the version -core pins. 1.1.31 declared 3.0.3, which -core
  overrides.
- **Java and modules:** still Java 8 bytecode. There's no `Automatic-Module-Name`, so the module
  names stay `openhtmltopdf.core` and `openhtmltopdf.pdfbox`.
- **API:** a `javap` comparison of the 71 types -core imports found additions only.
  `OutputDevice` gains an abstract `paintBoxShadow`, which `AbstractOutputDevice` implements.
- **Security:** nothing pressing. The only security item in the range is the PDFBox 3.0.8 bump
  for CVE-2026-33929. That CVE is in PDFBox's examples module, not the library, and -core already
  pins 3.0.8.

The version number: each merge to openhtmltopdf's `main` publishes a release, so 1.1.32 to 1.1.99
are all on Central. 1.1.31 is from 8 September 2025, and 1.1.100 from 22 October 2026, the day
before this check.

**To take it:** a CR. It would:
- fix or work around the doubled `w:br`;
- decide on thead's `keepLines`;
- handle alpha in colours;
- find out which default stylesheet is read;
- add a test of one `w:br` per `<br/>`;
- rerun this comparison.

## 4. mime4j 0.8.13 → 0.8.15: stays; the next release

0.8.14 is from 21 April 2026, and 0.8.15 from 3 September 2026. No CVE: NVD's newest mime4j
entry is CVE-2024-21742, fixed in 0.8.10. 0.8.15 does add hardening against denial of service,
which fits CR-005:
- limits on the number of parts and on nesting;
- a limit on the total header count;
- MIME parameters accumulated linearly rather than quadratically;
- a 1 MiB limit on total header size.

The new `MimeConfig` defaults are 512 parts, nesting 64, 16,384 headers and 1 MiB of headers.

**Measured:** `convertMHT` with 10 and 512 parts works on both versions. With 513 or 600 parts,
0.8.15 throws `Docx4JException: Maximum part count limit (512) exceeded`, caused by
`MaxPartCountLimitException`; 0.8.13 converts them.
- `convertMHT` reads the whole MHT, though it uses only the first text/html part.
- It uses `new MimeStreamParser()`, with no `MimeConfig`. The import of `MimeConfig` in
  `XHTMLImporterImpl` is unused.
- A Word MHT holds a part for each image file, plus a few of its own, so a long illustrated document
  could pass 512.

The rest is safe:
- **Tests:** the suite, including its `convertMHT` tests, passes on 0.8.15 (§2).
- **API:** `Field` gained an abstract `getSafeRaw()` in 0.8.14 (MIME4J-333), which breaks only
  implementers; -core only reads fields. `MimeStreamParser`, `ContentHandler` and `BodyDescriptor`
  are unchanged.
- **Java and modules:** still Java 8 bytecode, and the module is still `apache.mime4j.core`.
- **Who uses it:** only -core. docx4j-bundle doesn't hold it, and our bundle shades it.

**To take it:** a CR-005 follow-up in the next release. It would choose a `MimeConfig` (a higher
part limit, or stopping once the HTML part is read), and add a test with a 513-part MHT.

## 5. jsoup 1.22.1 → 1.23.2: stays

- **docx4j pins 1.22.1.** docx4j 17.3.4 does, and so does `VERSION_17_3_5`. With docx4j-bundle,
  docx4j's HTML altChunk reader runs on the jsoup our bundle ships, because docx4j-bundle has
  none. Moving alone would put docx4j on a jsoup it hasn't tested. So jsoup moves when docx4j's
  does.
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
  fixed as a performance change (#2559), with no advisory.
- **Measured on 1.22.1:** elements nested 8,000 deep, each with its own prefix and namespace, parse
  in 15 ms and convert in 20 ms. The HTML parser stops nesting at 512, so the depth never reaches
  `W3CDom`. 1.23.2 takes 18 ms and 36 ms.
- **What 1.22.2 to 1.23.2 change:**
  - `W3CDom`'s namespace and prefix handling is reworked. Word MHT's `o:` and `v:` elements go
    through it.
  - `noscript` content becomes elements.
  - CDATA in HTML becomes a bogus comment.
  - RCDATA and RAWTEXT follow the spec.
  - `ins`, `del` and `button` become inline.

**To take it:** with docx4j, at the same version on both sides. Before that, run this suite,
docx4j's altChunk tests and a Word MHT through `convertMHT`.

## 6. Sources

- openhtmltopdf:
  - releases: https://github.com/openhtmltopdf/openhtmltopdf/releases
  - the release-per-merge workflow:
    https://github.com/openhtmltopdf/openhtmltopdf/blob/main/.github/workflows/release.yml
  - the PDFBox 3.0.8 bump: https://github.com/openhtmltopdf/openhtmltopdf/pull/256
  - versions and poms: https://repo1.maven.org/maven2/io/github/openhtmltopdf/openhtmltopdf-core/
- PDFBox security (CVE-2026-33929, examples module only): https://pdfbox.apache.org/security.html
- mime4j:
  - changelog:
    https://github.com/apache/james-mime4j/blob/apache-mime4j-project-0.8.15/CHANGELOG.md
  - the changes:
    https://github.com/apache/james-mime4j/compare/apache-mime4j-project-0.8.13...apache-mime4j-project-0.8.15
  - `MimeConfig` at 0.8.15:
    https://github.com/apache/james-mime4j/blob/apache-mime4j-project-0.8.15/core/src/main/java/org/apache/james/mime4j/stream/MimeConfig.java
- jsoup:
  - releases: https://github.com/jhy/jsoup/releases
  - https://github.com/advisories/GHSA-65r4-943x-97jj
  - https://github.com/jhy/jsoup/security/advisories/GHSA-pmhh-3w7g-xqp8
- This repository: `XHTMLImporterImpl.convertMHT`, `JsoupHtml`, -core's `pom.xml`.
- docx4j: `docx4j-core/src/main/java/org/docx4j/model/text/JsoupHtml.java`, and the jsoup and
  fontbox pins in its parent `pom.xml`.
