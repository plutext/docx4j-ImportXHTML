# Contributing to docx4j-ImportXHTML

Thank you for your interest in contributing to docx4j-ImportXHTML.
Contributions are accepted under the [GNU Lesser General Public License,
version 2.1](legals/LICENSE), the same license the project is distributed
under. Note this differs from docx4j itself, which is Apache License v2;
this project is LGPL because its main dependency, openhtmltopdf, is.

## Certifying your contribution (DCO)

docx4j-ImportXHTML uses the [Developer Certificate of Origin](https://developercertificate.org/)
(DCO). By adding a `Signed-off-by:` line to each commit, you certify that you
wrote the contribution, or otherwise have the right to submit it under the
LGPL v2.1, as set out in the DCO text.

Add the sign-off with the `-s` flag when committing:

```bash
git commit -s -m "Handle orphan li elements as regular blocks (issue 123)"
```

which appends a trailer of the form:

```
Signed-off-by: Your Name <your.email@example.com>
```

Use your real name and a working email address. The sign-off must be added by
a human: automated tools and AI agents must not add `Signed-off-by:` lines
(see below).

Pull requests are checked for a valid sign-off on every commit. If you forgot
it, you can amend with `git commit --amend -s` or
`git rebase --signoff HEAD~<n>`.

If your employer requires a signed corporate agreement, please say so in the
issue and we will sort it out; the DCO sign-off is what is required for every
pull request.

## AI-assisted contributions

Contributions written with the help of AI tools (Claude, Copilot, and similar)
are welcome, subject to the following conditions:

1. **You are responsible for the contribution.** You must have personally
   reviewed and understood every part of the change, and be able to explain
   and defend it in code review. Bugs, regressions, licensing problems, and
   provenance issues in AI-assisted code remain your responsibility, exactly
   as if you had typed it yourself. Pull requests whose submitter cannot
   answer questions about the code will be closed.

2. **Tool terms must be compatible.** The terms of use of the AI tool must not
   place restrictions on its output that are inconsistent with the LGPL v2.1
   or the Open Source Definition.

3. **No third-party code laundering.** Do not submit AI output that you know
   or suspect reproduces identifiable third-party code. If your tool flags
   output as matching existing licensed code, either comply with that license
   (including any NOTICE/attribution obligations) or do not submit it. This
   matters here in particular: parts of this project are derived from Flying
   Saucer / openhtmltopdf and carry third-party copyright notices — see
   [Copyright headers](#copyright-headers) below.

4. **Disclose AI assistance** with a commit trailer. Preferred form:

   ```
   Assisted-by: Claude Code (claude-fable-5)
   ```

   `Generated-by:` and `Co-Authored-By:` trailers are also accepted. Purely
   mechanical assistance (autocomplete of a line or two) does not need
   disclosure; generation of substantive logic does.

5. **Only humans sign off.** The `Signed-off-by:` line certifies the DCO and
   must be added by you, not by the tool. Do not configure an AI agent to add
   sign-offs on your behalf.

6. **Verify with the real build.** Agents often run tests through a shortcut
   (a plain classpath, a single module) which silently bypasses the module
   path. Before submitting, run `mvn clean install` yourself — see
   [JPMS](#jpms) below for the class of error this catches.

## Practical guidelines

- **Target branch**: each release lives on its own `VERSION_x_y_z` branch;
  `master` is historical. Base your pull request on the current development
  branch (the most recent `VERSION_x_y_z` branch — at the time of writing,
  `VERSION_17_0_1`). If in doubt, ask in the issue first.
- **Discuss first**: for anything beyond a small fix, please open an issue
  describing the problem before investing effort in a pull request.
- **Build**: `mvn clean install` (JDK 11+). Java 11 bytecode is enforced by
  maven-enforcer, but building with a newer JDK is fine, since the compiler
  uses `<release>11</release>`. Releases track docx4j releases: the
  `version.docx4j` property pins the docx4j dependency.
- **Tests**: the suite lives in the separate `docx4j-ImportXHTML-core-tests`
  module (JUnit 4), under `org.docx4j.convert.in.xhtml.tests`. Run it with:

  ```bash
  mvn test -pl docx4j-ImportXHTML-core-tests -am
  ```

  The `-am` matters. Without it the module is built alone, and testCompile
  fails with *"Can't compile test sources when main sources are missing a
  module descriptor"* — the tests are a JPMS module which needs the core
  module built first.

  Name test classes `SomethingTest`, so that surefire's default include
  patterns pick them up. A class named, say, `ImageAddTestGif` will compile
  but never run.

  Please add or extend a test where practical.

- <a name="jpms"></a>**JPMS**: the core module is a JPMS module
  (`docx4j_ImportXHTML`), and so is the test module
  (`docx4j_ImportXHTML_tests`). If you use a new dependency, add a `requires`
  to the relevant `module-info.java` — including the test one. A test which
  compiles on a plain classpath can still fail the real build with
  *"package ... is not visible"*, so always confirm with `mvn clean install`
  rather than an IDE or ad hoc `javac` run.
- **Changelog**: add a line to `CHANGELOG.md` for user-visible changes.
- <a name="copyright-headers"></a>**Copyright headers**: new source files get
  the LGPL header used by existing files (copy it from, say, `ListHelper.java`).
  When you amend a file, bring its Plutext copyright line up to the current
  year — `Copyright 2011-2013` becomes `Copyright 2011-2026`, keeping the
  start year. Files in `org.docx4j.convert.in.xhtml.renderer` also carry
  third-party notices inherited from Flying Saucer / openhtmltopdf (Wisconsin
  Court System, Torbjörn Gannholm); leave those exactly as they are, and put
  your changes under the `Modifications copyright ... Plutext Pty Ltd` line.
- **Style**: match the conventions of the surrounding code. Preserve each
  file's existing line endings — the tree is a mix of LF and CRLF, so a
  whole-file re-ending turns a one-line change into an unreviewable diff.
