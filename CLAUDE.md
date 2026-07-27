# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

docx4j-ImportXHTML converts XHTML + CSS into OpenXML WordML (docx) using docx4j, with some support for pptx. Licensed LGPL v2.1 (same as its main dependency, openhtmltopdf) — see legals/NOTICE. New source files get the LGPL header used by existing files.

## Copyright headers

The project header is the LGPL v2.1 block beginning `This file is part of the docx4j-ImportXHTML library.`, whose copyright line reads `Copyright 2011-<year>, Plutext Pty Ltd, and contributors.` Copy it verbatim from an existing file (e.g. `ListHelper.java`).

- **When you amend a source file, bring its Plutext copyright line up to the current year** — `Copyright 2011-2013` becomes `Copyright 2011-2026`. Keep the start year as it is; only the end year moves. Do this as part of the same edit, not as a separate pass.
- **Only ever update Plutext's own copyright line.** Some files carry third-party notices inherited from Flying Saucer / openhtmltopdf — `Copyright (c) 2006 Wisconsin Court System`, `Copyright (c) 2004, 2005 Torbjörn Gannholm` — mostly in `org.docx4j.convert.in.xhtml.renderer`. Leave those untouched; extending someone else's years would misattribute their copyright. If such a file has no Plutext line and you make a substantial change, ask before adding one rather than assuming.
- **A number of `.java` files have no header at all.** If you touch one, say so and offer to add the standard header — don't add it silently, and don't go header-hunting across files you weren't asked to change. `module-info.java` doesn't need one.

## Build and test

Multi-module Maven build (Java 11 bytecode is enforced via maven-enforcer; building with a newer JDK is fine since the compiler uses `<release>11</release>`):

```bash
mvn clean install                 # build everything, run tests
mvn install -pl docx4j-ImportXHTML-core            # build just the library
mvn test -pl docx4j-ImportXHTML-core-tests         # run the test suite
mvn test -pl docx4j-ImportXHTML-core-tests -Dtest=TableBordersTest   # single test class
```

Notes:
- Tests are JUnit 4 and live in the separate `docx4j-ImportXHTML-core-tests` module (not in core's own src/test), under `org.docx4j.convert.in.xhtml.tests`.
- The version is set via the `revision` property in the parent pom (flatten-maven-plugin, `ossrh` mode). `version.docx4j` pins the docx4j dependency version; releases of this project track docx4j releases.
- Compiled classes go to `bin/` and `bin-testOutput/` (Eclipse-style, configured in the poms), not `target/classes`.
- The parent pom's `<modules>` list is sometimes edited for deployment (samples/tests commented out); don't "fix" that without checking intent.
- Version branches (`VERSION_x_y_z`) are the working branches; `master` is historical. Check the current branch before assuming where work should land.

## Architecture

The conversion pipeline has two halves:

1. **Layout via openhtmltopdf** (`io.github.openhtmltopdf`): `org.docx4j.convert.in.xhtml.renderer.DocxRenderer` configures an openhtmltopdf `SharedContext` (with `Docx4jUserAgent`, `Docx4jDocxOutputDevice`, `Docx4jTextRenderer`, `Docx4jReplacedElementFactory`) to parse the XHTML+CSS and build openhtmltopdf's styled Box model — it never renders to PDF, though a PDFBox `PDDocument` is needed internally for font resolution. Default element styling comes from `src/main/resources/XhtmlNamespaceHandler.css`; openhtmltopdf config from `xhtmlrenderer.conf`.

2. **Box-tree traversal → WML** in `org.docx4j.convert.in.xhtml`: `XHTMLImporterImpl` (implementing the `XHTMLImporter` interface) walks the `BlockBox`/`InlineBox` tree and emits docx4j WML objects (`P`, `R`, `Tbl`, …). `convert(...)` overloads (File, InputStream, Reader, Node, URL, String) return a `List<Object>` of WML content to add to a `WordprocessingMLPackage`. Delegated concerns:
   - `ListHelper` — ol/ul → Word numbering definitions
   - `TableHelper` — tables, borders, cell merges
   - `FontHandler` — font-family → `w:rFonts` mapping (extensible via `FontHandler.addFontMapping`)
   - `XHTMLImageHandler` / `XHTMLImageHandlerDefault` — images (pluggable)
   - `BookmarkHelper`, `HeadingHandler`, `DivHandler` / `DivToSdt`, `ParagraphBorderHelper`
   - `PPrCleanser` / `RPrCleanser` — strip direct formatting that duplicates style-inherited formatting

   The pptx path (`org.pptx4j.convert.in.xhtml.XHTMLtoPPTX`) is a separate, simpler DOM-based traversal.

Key behavioral knobs:
- `FormattingOption` (set per run/paragraph/table on the importer): `CLASS_TO_STYLE_ONLY` (map @class to an existing Word style only), `CLASS_PLUS_OTHER` (default: style plus direct formatting from CSS), `IGNORE_CLASS` (direct formatting only). There is also a CSS white-list mechanism.
- `ImportXHTMLProperties` reads `docx4j-ImportXHTML.properties` from the classpath (default fonts, bidi heuristic, heading→style mapping, th→tblHeader).

Input must be well-formed XML; MHT input is supported via `MHTFilterInputStream`/`MHTContentHandler` (apache-mime4j).

`docx4j-ImportXHTML-samples` contains runnable usage examples (e.g. `ConvertInXHTMLFragment`, `ConvertInXHTMLDocument`, `XhtmlToDocxAndBack`).

The core module is a JPMS module (`docx4j_ImportXHTML`); when adding dependencies, update `module-info.java` too. `dev_docs/` has working notes on lists, hyperlinks, fonts, and cell borders.
