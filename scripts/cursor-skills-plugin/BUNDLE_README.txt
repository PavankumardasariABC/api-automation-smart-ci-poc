Cursor API Automation — shareable Agent Skills bundle
======================================================

What this is
------------
This package installs Cursor Agent Skills into a Java API automation project.
Skills are markdown instructions (plus optional assets) under:

  .cursor/skills/<skill-name>/SKILL.md

Cursor loads them so the AI assistant follows your team’s conventions for:

  - TestNG groups: Smoke, Sanity, Regression
  - Registering tests in testng.xml
  - Gradle: ./gradlew test -Dgroups=...
  - GitHub Actions CI behavior (PR vs push vs manual)

This is not a VS Code Marketplace extension. It is the standard Cursor
project-skills layout, packaged so you can share it like a small “plugin.”


Contents of this bundle
-----------------------
api-automation-smart-ci/
  SKILL.md — Repo layout (Gradle, TestNG, Rest Assured, Allure), how to add
             tests, run commands, CI workflow expectations, key files.

test-buckets-rules/
  SKILL.md — Rules that must match TEST_BUCKETS.md: bucket definitions, CI
             behavior, validation checklist, testng.xml requirements.

TEST_BUCKETS.reference.md (when present)
  Copy of the project’s TEST_BUCKETS.md for alignment. install.sh can place it
  as TEST_BUCKETS.md in the target project if that file does not exist.

install.sh
  Copies both skills into <project>/.cursor/skills/ and optionally adds
  TEST_BUCKETS.md — see “Install (recipient)” below.


Who this is for
---------------
- Recipients: QA / developers using Cursor on another repo who want the same
  API automation and CI conventions.
- Maintainers: Owners of api-automation-smart-ci-poc who run build-bundle.sh
  and distribute the zip or folder.


Prerequisites (recipient)
-------------------------
- Cursor (or a compatible editor that reads .cursor/skills/).
- macOS, Linux, or Git Bash on Windows for install.sh (bash).
- A Java / Gradle / TestNG API test project (conventions match this bundle).


Install (recipient)
-------------------
1. Unzip this bundle anywhere (e.g. Desktop or Downloads).

2. Open a terminal and go inside the unzipped folder (the folder that contains
   install.sh and the two skill directories).

3. Make the installer executable and run it with your project root (the folder
   that should contain .cursor/ after install):

     chmod +x install.sh
     ./install.sh /path/to/your/java-api-automation-project

   To install into the current directory (your project root):

     ./install.sh .

4. Reload Cursor (or restart the app) so it picks up the new skills.

5. Optional: Open .cursor/skills/*/SKILL.md to confirm files are present.


After install — what you should see
-----------------------------------
<your-project>/
  .cursor/
    skills/
      api-automation-smart-ci/SKILL.md
      test-buckets-rules/SKILL.md
  TEST_BUCKETS.md          (optional; added only if missing and reference was bundled)


Notes and limitations
---------------------
- TEST_BUCKETS.md: The test-buckets-rules skill references TEST_BUCKETS.md at the
  repository root (relative path from the skill). If you already have a different
  TEST_BUCKETS.md, install.sh will not overwrite it — compare and merge with
  TEST_BUCKETS.reference.md manually.

- Project-specific paths: Skills describe patterns from api-automation-smart-ci-poc
  (e.g. com.externalAPIs.tests, workflows under .github/workflows). Adapt your
  repo or extend the SKILL.md files locally if your layout differs.

- Secrets / env: Skills do not contain credentials; they describe structure and
  process only.


Create a new bundle (maintainer)
--------------------------------
From a clone of api-automation-smart-ci-poc, at the repository root:

  ./scripts/cursor-skills-plugin/build-bundle.sh

Outputs (under build/, gitignored by the repo):

  build/cursor-api-automation-skills-bundle/     — folder to zip or copy
  build/cursor-api-automation-skills-bundle.zip  — single file to share

Requirements: bash, rsync, zip; source skills must exist at:

  .cursor/skills/api-automation-smart-ci/
  .cursor/skills/test-buckets-rules/


Sharing the bundle
------------------
- Attach cursor-api-automation-skills-bundle.zip to an internal wiki, Slack,
  email, or a GitHub Release.
- Or share the unzipped folder on a shared drive; recipients still run
  install.sh from inside that folder.


Support
-------
Questions about bucket rules or CI: see TEST_BUCKETS.md and .github/workflows
in the main api-automation-smart-ci-poc repository. Update the skills in
.cursor/skills/ there and rebuild the bundle when conventions change.
