# CR-004: docx4j-ImportXHTML-bundle, a fat jar to put beside docx4j-bundle

Status: ACCEPTED 2026-10-23 (Jason: "CR-004 decisions: all recommendations accepted"), D1 to D6
as recommended. Proposed 2026-10-22, at Jason's request relayed by docx4j-25 (its brief of
22 October: "ImportXHTML should offer its own bundle ... Jason decides naming and whether it
ships in your 17.3.4"). Drafted by Claude Opus 5.5 in the docx4j-ImportXHTML session. The module
was prototyped outside the repository and measured against docx4j-bundle and
docx4j-bundle-extras, 17.3.4-SNAPSHOT first and then the released 17.3.4 (§3, §4). Phase 1
implemented 2026-10-23 (suite 146/0/0, BundleIT 3/0/0; §6), uncommitted, and installed as
17.1.1-SNAPSHOT.
Scope: a new module, `docx4j-ImportXHTML-bundle`, with a pom, a marker class, licence files and an
integration test. Also the parent pom's `<modules>` and release notes, the README and the
CHANGELOG. `-core` doesn't change.
Branch: `VERSION_17_3_4`. It ships in 17.3.4 with CR-002 and CR-003 if decided (D6).
Depends on: docx4j 17.3.4 on Central (there since 22 October), since the published pom's one
dependency is its `docx4j-bundle`.
Phases (§6): 1. the module; 2. the release.

## 1. Background

docx4j 17.3.4 (on Central since 22 October, tag `docx4j-17.3.4` at 8f455ebab) publishes two fat
jars. The rationale is in docx4j's `docx4j-bundle/design-rationale.md`, section "2026-10-21":

- **`docx4j-bundle`** holds docx4j-core, the JAXB reference implementation, docx4j-digsig,
  docx4j-markdown and, since 7ee783b72, docx4j-diffx. Its published pom has no dependencies.
- **`docx4j-bundle-extras`** holds PDF output. It goes beside docx4j-bundle, and its published pom
  depends on docx4j-bundle only. Its shade is a positive include list. The check is that no class
  is in both jars.

A docx4j-bundle user who wants XHTML import has no good jar to add:

- **From Maven,** `docx4j-ImportXHTML-core`'s published pom (flattened, `all` mode) declares
  docx4j-core and its whole tree: 33 dependencies at 17.1.1-SNAPSHOT. Every docx4j class then
  comes twice, once from the bundle and once from the separate jars, and which copy loads depends on
  classpath order.
- **From drop-in jars** (the route in docx4j's `docs/Docx4j_from_Python.md`), the user collects
  openhtmltopdf, PDFBox, xmpbox, graphics2d and mime4j by hand. They must leave out fontbox and
  pdfbox-io, which the bundle already holds.

docx4j 17.3.4's HTML altChunk reader also needs both ImportXHTML and jsoup on the classpath.
docx4j's `JsoupHtml` looks for `org.jsoup.Jsoup` with `Class.forName`, and docx4j-bundle has
neither.

The ask (Jason, 22 October, via docx4j-25) is a jar in extras' shape that brings only what
docx4j-bundle doesn't have. The proposed name is `docx4j-ImportXHTML-bundle`. When it exists,
docx4j-25 adds it to `Docx4j_from_Python.md` as the third jar.

## 2. What goes in (measured)

The table lists -core's dependency tree, built against docx4j 17.3.4-SNAPSHOT, beside what
docx4j-bundle 17.3.4-SNAPSHOT holds (from the `pom.properties` files in its jar). The released
17.3.4 holds the same artifacts, plus docx4j-diffx, which isn't in -core's tree, so no row changes:

| artifact | licence | in docx4j-bundle | this jar |
|---|---|---|---|
| `org.docx4j:docx4j-ImportXHTML-core` | LGPL 2.1 | no | in |
| `io.github.openhtmltopdf:openhtmltopdf-core` 1.1.31 | LGPL 2.1 or later | no | in |
| `io.github.openhtmltopdf:openhtmltopdf-pdfbox` 1.1.31 | LGPL 2.1 or later | no | in |
| `de.rototor.pdfbox:graphics2d` 3.0.1 | Apache 2.0 | no | in |
| `org.apache.pdfbox:pdfbox` 3.0.8 | Apache 2.0 | no | in |
| `org.apache.pdfbox:xmpbox` 3.0.8 | Apache 2.0 | no | in |
| `org.apache.james:apache-mime4j-core` 0.8.13 | Apache 2.0 | no | in |
| `org.jsoup:jsoup` 1.22.1 (optional in -core) | MIT | no | in (D2) |
| `org.apache.pdfbox:fontbox` 3.0.8 | Apache 2.0 | yes, 3.0.8 | out |
| `org.apache.pdfbox:pdfbox-io` 3.0.8 | Apache 2.0 | yes, 3.0.8 | out |
| docx4j-core's tree, `jakarta.xml.bind-api`, `slf4j-api` | | yes | out |

Three corrections to the brief:

- **xercesImpl and xml-apis aren't needed.** -core's xercesImpl dependency is commented out (a JPMS
  clash with `jdk.xml.dom`), so neither is in its tree.
- **graphics2d goes in.** openhtmltopdf-pdfbox depends on it.
- **pdfbox-io stays out,** like fontbox. docx4j-bundle already has it, at the same version.

## 3. The module (prototyped)

Extras' pom with this project's parent:

- **The shade** is a positive list:

  ```xml
  <include>org.docx4j:docx4j-ImportXHTML-core</include>
  <include>io.github.openhtmltopdf:openhtmltopdf-core</include>
  <include>io.github.openhtmltopdf:openhtmltopdf-pdfbox</include>
  <include>de.rototor.pdfbox:graphics2d</include>
  <include>org.apache.pdfbox:pdfbox</include>
  <include>org.apache.pdfbox:xmpbox</include>
  <include>org.apache.james:apache-mime4j-core</include>
  <include>org.jsoup:jsoup</include>
  ```

  The other settings are as in extras: `shadedArtifactAttached=false` and
  `createDependencyReducedPom=false`. The filters drop `META-INF/*.SF|DSA|RSA`, `module-info.class`
  and `META-INF/versions/*/module-info.class`. Three transformers:
  - **ApacheNotice,** with `addHeader=false` and no `projectName` (D3). With a `projectName`, the
    transformer writes an entry of its own, "Copyright 2006-*year*" followed by its
    `organizationName`, which defaults to The Apache Software Foundation (docx4j-25, measured on
    shade 3.1.0). With neither, a clean build's NOTICE has only the bundled entries and ours from
    `legals/NOTICE`;
  - **Services;**
  - **Manifest,** adding `Multi-Release: true`. jsoup is a multi-release jar, and without the
    attribute its classes under `META-INF/versions/11/` are never loaded.
- **`maven-jar-plugin` with `forceCreation=true`.** With `shadedArtifactAttached=false`, shade
  writes the fat jar over `target/<module>.jar`. Without a `clean`, the jar plugin then finds that
  jar up to date and leaves it, so the next shade starts from the old fat jar (docx4j-25's finding).
  Measured: with jsoup taken out of the includes, a build without `clean` still had its 307
  entries; with `forceCreation`, it had none. So `BundleIT` always tests what the includes say.
- **Dependencies:**
  - `docx4j-bundle` `${version.docx4j}`, compile scope. This is the only dependency the published
    pom carries.
  - docx4j-core and docx4j-JAXB-ReferenceImpl, provided.
  - `docx4j-ImportXHTML-core` `${project.version}` and jsoup, optional.
  - docx4j-bundle-extras and junit, test scope (for the integration test).

  `flatten-maven-plugin` runs with `flattenDependencyMode=direct`. The parent's `all` would hoist
  -core's tree into the published pom.
- **A marker class,** `org.docx4j.convert.in.xhtml.bundle.ImportXHTMLBundle`, with a
  `package-info.java`. Central requires sources and javadoc jars, and this gives them content, as
  extras' `BundleExtras` does.
- **Licence files** (D3):
  - `META-INF/LICENSE` is ours, LGPL 2.1, from `legals/LICENSE`.
  - The shade would drop two files that use that same path, so they're kept under their own names:
    - `META-INF/LICENSE-openhtmltopdf.txt`: openhtmltopdf's 18 lines saying what is LGPL;
    - `META-INF/LICENSE-pdfbox.txt`: Apache 2.0 with PDFBox's appendix of third-party licences. Its
      first 202 lines are the Apache 2.0 text that xmpbox and mime4j ship.
  - openhtmltopdf's `LICENSE-LGPL-2.1.txt`, `LICENSE-LGPL-3.txt` and `LICENSE-W3C-TEST`, and
    jsoup's `META-INF/jsoup/LICENSE`, come through unchanged.
  - `META-INF/NOTICE` merges `legals/NOTICE` with the NOTICE files of PDFBox, xmpbox and mime4j.
- **`BundleIT`** (D4) runs under failsafe in the `integration-test` phase, after the shade, on the
  jar itself. It has three tests:
  1. no file outside `META-INF/` is in both this jar and docx4j-bundle;
  2. the same against docx4j-bundle-extras;
  3. a smoke run in a class loader holding only the test classes, this jar and docx4j-bundle. It
     converts an XHTML string, then converts an HTML altChunk through docx4j's `convertAltChunks`
     (jsoup, then ImportXHTML). It saves and reloads the result, and checks that ImportXHTML and
     jsoup loaded from this jar.

## 4. Measurements on the prototype (22 and 23 October 2026)

The prototype was built against docx4j-bundle 17.3.4-SNAPSHOT (installed 21 October) and
-core 17.1.1-SNAPSHOT (dd7b913):

- **Size:** 4.3 MB, with 2,048 files in 110 packages. PDFBox is most of it (788 classes).
- **Shared with docx4j-bundle:** 0 files outside `META-INF/`. **With extras:** 0. No package is
  split with either jar.
  - The only shared names are `META-INF/LICENSE`, `NOTICE`, `MANIFEST.MF` and, with the base jar,
    `DEPENDENCIES`. No code looks these up by name.
- **The check fails as it should.** With fontbox added to the includes, `BundleIT` lists
  `org/apache/fontbox/...` as being in both jars, and the build fails.
- **Smoke tests from the jars alone** used `java -cp` with no Maven:
  - **base + this jar:** the XHTML string converted, and the altChunk too: the rows came out
    HEAD, FOOT, BODY (CR-003) with the `display:none` tfoot hidden (CR-002), and the document was
    saved and reloaded.
  - **base + extras + this jar:** the same, and `Docx4J.toPDF` wrote its PDF (26,655 bytes).
  - **this jar alone:** `NoClassDefFoundError: org/docx4j/openpackaging/packages/WordprocessingMLPackage`,
    as intended.
- **A Maven consumer** that depends on `docx4j-ImportXHTML-bundle` alone resolves exactly two
  artifacts: this jar and docx4j-bundle 17.3.4-SNAPSHOT. This was checked with
  `dependency:tree` after installing the prototype to a scratch repository; `~/.m2` was untouched.
- **The enforcer** (bytecode 11, dependency convergence) passes.
- **Against docx4j-25's rebuilt jars** (installed 22 October, with §5's fixes), `BundleIT` passes
  again: 3/0/0.
- **Against the released docx4j 17.3.4** (23 October; docx4j-bundle and -extras 17.3.4 as
  installed by the docx4j release, with docx4j-diffx now in the base jar), `BundleIT` passes:
  3/0/0. The jar is unchanged at 4.3 MB.
- **ImportXHTML's suite** runs 146/0/0, compiled against docx4j 17.3.4-SNAPSHOT and again against
  the released 17.3.4 (nothing installed).

## 5. Found on the way

- **In docx4j's jars** (passed to docx4j-25, who confirmed both and fixed them, with the
  `forceCreation` fix, in docx4j f5541a7ea; the released 17.3.4 jars have `Multi-Release: true`
  and "Copyright 2007-2026 Plutext Pty Ltd" as the NOTICE's holder):
  - docx4j-bundle's manifest has no `Multi-Release: true`, so the BouncyCastle classes under
    `META-INF/versions/{11,15,17,25}/` (111 in all, in `jcajce` and `pqc`) are never loaded; the
    base versions are used instead.
  - Both docx4j jars' NOTICE files begin "docx4j-bundle[-extras] Copyright 2006-2026 The Apache
    Software Foundation". That's the transformer's default header, and it names the ASF as the
    copyright holder.
- **-core's published pom** lists docx4j-core's tree as direct dependencies. That's where §1's
  doubling comes from.
- **`legals/NOTICE` is out of date.** It names openhtmltopdf by its old group
  (`com.openhtmltopdf`) and lists commons-logging, which -core excludes. Phase 1 refreshes it (D3).
- **openhtmltopdf logs "Unable to disable XML External Entities" (SEVERE).** It can't set
  `accessExternalDTD` on the TransformerFactory that docx4j brings (xalan-interpretive). It
  appears in the smoke runs and equally on -core's ordinary classpath, so the bundle doesn't cause
  it. It wasn't investigated here and may be worth its own CR.

## 6. Phases

### Phase 1: the module

- `version.docx4j` moves to 17.3.4 (D5). `revision` stays as it is until the release.
- The module, as §3.
- The parent's `<modules>`, and its "TO RELEASE" comment. The deploy becomes
  `-pl docx4j-ImportXHTML-core,docx4j-ImportXHTML-bundle`. The bundle depends on -core, so it's
  last in that reactor, and its staging step uploads both. The "Performing remote staging..." check
  stays.
- A README paragraph for docx4j-bundle users, and a CHANGELOG entry.
- `legals/NOTICE` refreshed (D3).
- Install as 17.1.1-SNAPSHOT, then tell docx4j-25 so `Docx4j_from_Python.md` can name the jar.

**Done 2026-10-23** (uncommitted):
- **Files.**
  - `docx4j-ImportXHTML-bundle/`: the module, as §3.
    - The pom is the prototype's, with a `${revision}` parent.
    - It has `bin`/`bin-testOutput` output directories and a `.gitignore`, as the other modules
      have.
    - The jar plugin is `${version.maven-jar-plugin}` (2.3.1), with `forceCreation`.
    - The pom's `<licenses>` lists LGPL 2.1, Apache 2.0 and MIT (D3).
  - The marker and `package-info.java`, with the LGPL header.
  - `BundleIT` and `Smoke` as prototyped, with the LGPL header.
  - `META-INF/LICENSE` (from `legals/LICENSE`) and `NOTICE` (from `legals/NOTICE`), and the two
    licence files kept under their own names.
- **The parent pom.**
  - `version.docx4j` 17.3.4 (D5).
  - The module goes between -core-tests and -samples in `<modules>`. -samples is then still last
    in a full reactor, so the release comment's account of which project uploads still holds. With
    `-pl docx4j-ImportXHTML-core,docx4j-ImportXHTML-bundle`, the bundle is last, so it uploads
    both.
  - The "TO RELEASE" comment names the new `-pl`, and says the deploy step now runs `BundleIT`.
- **`legals/NOTICE`** (CRLF kept):
  - openhtmltopdf is under `io.github.openhtmltopdf`;
  - commons-logging is gone;
  - pdfbox-io, apache-mime4j-core and jsoup (optional) are added;
  - each entry names its licence.
- **README and CHANGELOG:** a "With docx4j-bundle" section, and an entry under the current
  version.
- **Measured** with `mvn -o clean install -Dgpg.skip`, all five modules green:
  - **Suite:** 146/0/0.
  - **`BundleIT`:** 3/0/0, against the released docx4j-bundle and -extras 17.3.4.
  - **The enforcer** passes.
  - **The jar:** 4,305,539 bytes, with 2,048 files, and classes in 111 packages (the prototype's
    110, plus the marker's).
  - **The manifest** has `Multi-Release: true`.
  - **`META-INF/LICENSE`** is ours (LGPL 2.1).
  - **The NOTICE** has no header of the transformer's own. Its sections are mime4j's, ours,
    PDFBox's and xmpbox's, in that order (the transformer's order).
  - **The flattened pom** has docx4j-bundle 17.3.4 (compile), with -core and jsoup optional and the
    two docx4j modules provided.
  - **A scratch consumer** of `docx4j-ImportXHTML-bundle:17.1.1-SNAPSHOT` alone resolves two
    artifacts: the bundle, and docx4j-bundle 17.3.4.

### Phase 2: the release

This goes with CR-002's phase 4 and CR-003's phase 2 as 17.3.4 (D6). docx4j 17.3.4 is on Central,
so nothing on docx4j's side holds it. `mvn clean install` runs `BundleIT` against the released
docx4j-bundle before the deploy.

## 7. Decisions (for Jason; each with a recommendation)

All six decided as recommended on 23 October 2026 (Jason: "CR-004 decisions: all recommendations
accepted").

- **D1. The name.**
  - (a) `docx4j-ImportXHTML-bundle`, as proposed, with the marker in
    `org.docx4j.convert.in.xhtml.bundle`.
  - (b) `docx4j-bundle-ImportXHTML`, which sorts beside docx4j-bundle and -extras on Central.

  **Recommended: (a).** It's this project's artifact, and this project's artifacts are named
  `docx4j-ImportXHTML-*`.
- **D2. jsoup in the jar.**
  - (a) In.
  - (b) Out; users add jsoup themselves.

  **Recommended: (a).** docx4j's HTML altChunk reader needs it, and so does `convertMHT`'s lenient
  path (CR-002 phase 3). A docx4j-bundle user has no other source of it. If docx4j-bundle later
  carries jsoup, `BundleIT` fails at that version bump, and the include list loses it then.
- **D3. Licence files.**
  - (a) As §3:
    - our LGPL 2.1 `LICENSE`;
    - the two licence files the shade would drop, under their own names;
    - a NOTICE without the transformer's ASF header;
    - the module's pom `<licenses>` listing Apache 2.0 and MIT beside LGPL 2.1, for the bundled
      parts;
    - `legals/NOTICE` brought up to date.
  - (b) As extras: our `LICENSE`, and the merged NOTICE with the default header.

  **Recommended: (a).** The default header would say "docx4j-ImportXHTML-bundle Copyright
  2006-2026 The Apache Software Foundation", which isn't true. The shade would also drop PDFBox's
  licence file, the only place its third-party licences are written, and openhtmltopdf's
  statement of what is LGPL.
- **D4. The check.**
  - (a) `BundleIT`, run by every build after the shade.
  - (b) A command run by hand before a release, as docx4j does.

  **Recommended: (a).** It costs about 2 seconds a build. It also catches a docx4j-bundle that
  changes under a SNAPSHOT, as 17.3.4's did when it gained docx4j-diffx after the first
  measurement. And the release steps in the parent pom are long enough already.
- **D5. `version.docx4j` now.**
  - (a) 17.3.4, the release, in phase 1.
  - (b) Left for CR-002 phase 4's version commit, with the module landing then.

  **Recommended: (a).**
  - The module can only be checked against the docx4j-bundle it pins, and docx4j-bundle
    17.1.1-SNAPSHOT is the old shape.
  - The suite already passes against the released 17.3.4.
  - A release, rather than 17.3.4-SNAPSHOT: docx4j has moved on to 17.3.5, so the snapshot won't
    change again.

  `revision` stays at 17.1.1-SNAPSHOT, so nobody's coordinates change. CR-002 phase 4's prepared
  version commit then has one line less to change.
- **D6. The release.**
  - (a) In 17.3.4, with CR-002 and CR-003.
  - (b) A later release.

  **Recommended: (a).** docx4j 17.3.4 is out, so this adds no wait to ImportXHTML's 17.3.4. The
  three jars then share a version number, and docx4j's documentation can name them together.

## 8. Risks

- **Pairing versions.** The jar is checked against the docx4j-bundle it pins. Paired with another
  docx4j-bundle version, it could duplicate or miss classes; for example, if a later docx4j-bundle
  carries pdfbox or jsoup. A Maven consumer who also declares docx4j-bundle gets the version they
  declare (nearest wins). The README says to use the matching version.
- **LGPL code in a fat jar.** The jar carries ImportXHTML and openhtmltopdf, both LGPL, unmodified,
  with their sources on Central. docx4j kept its base jar free of LGPL code on purpose and left this
  add-on to carry it (design-rationale.md: an add-on "can later carry LGPL code
  (docx4j-ImportXHTML) without touching the base artifact's licence story"). Whether the fat-jar
  form needs more, such as a note on replacing the LGPL parts, is Jason's call.
- **The module path isn't supported.** As with docx4j-bundle, there is no `module-info` (stripped)
  and no `Automatic-Module-Name`. -core stays the artifact for the module path.
