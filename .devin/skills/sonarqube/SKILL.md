---
name: sonarqube
description: "Converted from Devin playbook: SonarQube"
triggers:
  - user
  - model
---

# SonarQube

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SonarQube (playbook-a9418d8e083a4c158acfd4add7e8d15c), macro `!sonar_qube`

## Procedure

# Playbook: SonarQube Improvement (Signal, Gates, and PR Decoration)

## Overview

This playbook defines a deterministic workflow to raise SonarQube signal and enforce “quality on new code.” It covers scanner setup, multi‑language coverage wiring (Java/Kotlin, JS/TS, Python, Go, .NET), PR decoration, custom Quality Gate & rule profile curation, baseline/new‑code period, backlog triage, high‑ROI fixes, and CI integration (GitHub/GitLab/Bitbucket). The outcome is a PR that introduces correct `sonar-project.properties` (or build plugin config), coverage reports, CI jobs, and documentation—plus an administrator checklist to apply the gate and profiles server‑side.

## 1. Background Data, Documents, and Images

- SonarQube/SonarCloud URL, edition, and version; project key(s)
- Access level (project admin vs. org admin) and a **token** stored as CI secret
- Repo tech stack: languages, build tools (Maven/Gradle, npm/yarn, pip/pytest, go, dotnet)
- Current CI provider (GitHub Actions / GitLab CI / Bitbucket Pipelines)
- Existing test frameworks and coverage tools (JaCoCo/Kover, Istanbul/NYC, coverage.py, Go coverage, Coverlet)
- Branching model and default branch (e.g., `main`), PR naming conventions
- Quality policy (desired thresholds), security/compliance requirements
- Historical Sonar metrics: coverage, duplication, maintainability/security/reliability ratings
- Any previous scanner configs, exclusions, or global ignores

## 2. Detailed Task Description & Rules

**Primary Objective**: Improve the project’s SonarQube setup so that PRs get decorated with actionable findings, the **Quality Gate applies to new code**, coverage reports flow correctly, and noisy/low‑signal rules are tuned, leading to measurable improvements without blocking safe shipping.

**Core Rules**

- **PR‑first**: All changes land via a branch `devin/sonarqube-improvement-<slug>` and a PR.
- **No secrets**: Store tokens in CI secrets (e.g., `SONAR_TOKEN`)—never commit credentials.
- **New Code first**: Gate targets **new code** with explicit *new code definition* (e.g., “since last version” or “30 days”).
- **Evidence**: Include before/after screenshots of dashboards and a link to the quality gate check in the PR.
- **Minimal suppression**: Avoid `// NOSONAR` and rule suppressions; when necessary, justify in code and PR.
- **Reproducible scans**: Scanner configuration lives in repo or build files; CI is deterministic and green when gate passes.
- **Server vs. repo**: Repo contains scanner config and coverage; server (admin) applies Quality Gate and Rule Profile.

**Platform Notes**

- **GitHub**: Sonar posts PR Checks; optional status‑check requirement.
- **GitLab**: MR decoration supported; use `glab` for automation.
- **Bitbucket**: Decoration via Sonar integration and Bitbucket Pipelines.

## 3. Examples

### 3.1 Good PR Title

```
ci(sonar): add multi‑lang coverage and PR decoration; enforce Quality Gate on new code

```

### 3.2 Good Commit Messages

```
build(java): wire JaCoCo aggregate + reportPaths for Sonar
build(js): output lcov.info to coverage/ and map for Sonar
ci(github): add Sonar scan on PR + push; gate is required
docs: add suppression policy and update README badge

```

### 3.3 Target Quality Gate (example)

- New code **coverage ≥ 80%**
- New code **duplicated lines ≤ 3%**
- **Security/Reliability/Maintainability = A**
- **No** new blocker/critical issues

## 4. Reasoning & Decision Checklist (for Devin)

Use this to make decisions without exposing hidden chain‑of‑thought:

- Which languages/builds are present? (Java/Kotlin, JS/TS, Python, Go, .NET)
- Are coverage reports produced locally for each language and exported to files Sonar expects?
- Is the **new code period** set correctly server‑side?
- Do we need exclusions (generated code, migrations, test data) to reduce false positives?
- Which rules are noisy? Plan to adjust the **Rule Profile** rather than blanket suppress.
- Do we have PR decoration working in a sample PR from the feature branch?
- Are CI secrets configured and referenced via env vars only?

## 5. Output Specification

A successful run yields:

- Branch `devin/sonarqube-improvement-<slug>` with:
    - Scanner config (`sonar-project.properties` or build plugin config)
    - Coverage output scripts and paths (per language)
    - CI workflow(s) invoking Sonar in PRs and on default branch
    - Documentation: README badge, suppression policy, and CONTRIBUTING updates
- A PR with:
    - Before/after metrics evidence and links to Quality Gate check
    - Labels and reviewers (owners of affected modules)
    - Passing Quality Gate on the PR
- Admin checklist completed (Quality Gate and Rule Profile applied to project)

## 6. Procedure

### 6.1 Detect Stack

1. List languages by scanning repo (build files, package manifests).
2. Note coverage tools in use. If missing, add minimal instrumentation.

### 6.2 Create Branch

```bash
git fetch origin --prune
SLUG="baseline-gate"
git checkout -B devin/sonarqube-improvement-${SLUG} origin/main

```

### 6.3 Add/Update Scanner Config

- If using **standalone scanner**, add `sonar-project.properties` at repo root (template A).
- If using **Maven/Gradle**, prefer official plugins (templates D/E). Keep property keys centralized.
- Define project key, name, and source/test paths. Exclude generated code (e.g., `*/generated/**`, `*/build/**`).

### 6.4 Wire Coverage Per Language

- **Java/Kotlin (JaCoCo/Kover)**: produce XML reports and set `sonar.coverage.jacoco.xmlReportPaths`.
- **JS/TS (Jest/NYC)**: create `coverage/lcov.info`; set `sonar.javascript.lcov.reportPaths` and `sonar.typescript.lcov.reportPaths`.
- **Python (pytest + coverage.py)**: produce `coverage.xml`; set `sonar.python.coverage.reportPaths`.
- **Go**: `go test -coverprofile=coverage.out`; set `sonar.go.coverage.reportPaths`.
- **.NET (Coverlet/OpenCover)**: produce `coverage.opencover.xml`; set `sonar.cs.opencover.reportsPaths`.

### 6.5 CI: Run Sonar on PRs and Default Branch

Pick one platform template (B/C) and commit it. Ensure env secrets:

- `SONAR_TOKEN` (project or org level)
- `SONAR_HOST_URL` (e.g., `https://sonar.yourcompany.dev` or `https://sonarcloud.io`)

### 6.6 PR Decoration

- Confirm the scanner receives PR metadata (usually provided automatically by CI variables). If needed, set:
    - `sonar.pullrequest.key` (PR number)
    - `sonar.pullrequest.branch` (feature branch)
    - `sonar.pullrequest.base` (target branch)

### 6.7 Quality Gate (Server‑side)

- Create or select a Quality Gate with thresholds from **3.3**.
- Assign the gate to the project.
- Set *New Code* definition (e.g., **since previous version** or **since 30 days**).
- (Optional) Mark Quality Gate as a required status check in your VCS.

### 6.8 Rule Profile Curation

- Copy the default profile per language and disable low‑signal rules (document changes in PR).
- Enable high‑value rules (security hotspots, injection, resource leaks, null‑safety, dangerous APIs).
- Re‑analyze; validate that signal/noise improves.

### 6.9 Backlog Triage & Baseline

- Do **not** mass‑suppress backlog. Keep focus on **new code**.
- Flag genuine false positives per issue with comments and correct resolution.
- Add targeted exclusions for generated or vendor code only.

### 6.10 High‑ROI Fix Sprint (optional but recommended)

- Address top **Blocker/Critical/Major** issues that are easy and safe: unused code, obvious null checks, resource closing, logging format/taint sinks.
- Add missing tests around changed areas to lift **new code coverage**.

### 6.11 Local Verification

```bash
# Example: Java + JS monorepo
mvn -q -DskipTests clean verify jacoco:report
npm ci && npm test -- --coverage
# Dry‑run sonar (if using CLI scanner)
sonar-scanner -Dsonar.login=$SONAR_TOKEN -Dsonar.host.url=$SONAR_HOST_URL -X

```

Ensure report paths in properties match produced files.

### 6.12 Open PR and Attach Evidence

- Title: `ci(sonar): add coverage + PR decoration; enforce gate on new code`
- Include: screenshots of PR decoration and project overview, link to the gate check, and a summary of rule/profile changes.

### 6.13 Post‑Merge

- Monitor the next default‑branch analysis; ensure badge shows updated status.
- Schedule periodic scans (nightly) and dependency updates.

## 7. Advice, Pitfalls, and Quality Gates

**Advice**

- Start strict on **new code**; relax overall. This avoids legacy churn.
- Keep report paths stable and under version control in CI.
- Prefer rule profile curation over in‑code suppression.

**Common Pitfalls**

- Coverage paths mismatch → “0% coverage” despite tests passing
- Scans run before tests → reports missing
- Token leakage via echoed commands; always mask secrets
- PR metadata not passed → no decoration

**Quality Gates (must pass for PR)**

- New code coverage ≥ configured threshold
- No new **Blocker/Critical** issues
- Security/Rel/Reliab ratings = A on new code
- Duplications within threshold on new code

## 8. Forbidden Actions

- Committing tokens or server URLs with credentials
- Blanket `// NOSONAR` on files or directories
- Disabling entire rule categories to make gate pass
- Modifying server‑side org settings from CI scripts without approval

---

## Templates

### A. `sonar-project.properties` (multi‑language)

```
sonar.projectKey=your-org_your-project
sonar.projectName=Your Project
sonar.sourceEncoding=UTF-8
sonar.host.url=${env.SONAR_HOST_URL}

# Sources & tests (adjust)
sonar.sources=src
sonar.tests=tests
sonar.exclusions=**/build/**,**/dist/**,**/node_modules/**,**/generated/**
sonar.test.exclusions=**/*IT.java

# Java/Kotlin (JaCoCo XML)
sonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml,**/build/reports/jacoco/test/jacocoTestReport.xml

# JavaScript/TypeScript
sonar.javascript.lcov.reportPaths=coverage/lcov.info
sonar.typescript.lcov.reportPaths=coverage/lcov.info

# Python
sonar.python.coverage.reportPaths=coverage.xml

# Go
sonar.go.coverage.reportPaths=coverage.out

# .NET (OpenCover)
sonar.cs.opencover.reportsPaths=**/coverage.opencover.xml

# Pull Request decoration (CI usually sets these)
# sonar.pullrequest.key=
# sonar.pullrequest.branch=
# sonar.pullrequest.base=

```

### B. GitHub Actions: `.github/workflows/sonar.yml`

```yaml
name: SonarQube Scan
on:
  pull_request:
    branches: ["main"]
  push:
    branches: ["main"]

jobs:
  build-and-analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0 # full history for blame

      # Java example
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - name: Build & Test (Java)
        run: |
          mvn -B -DskipITs clean verify jacoco:report

      # Node example
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - name: Test (JS/TS) with coverage
        run: |
          npm ci
          npm test -- --coverage

      - name: Sonar Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        run: |
          bash <(curl -sS https://get.sonarscanner.com/sonar-scanner-cli.zip) # or preinstalled action
          node -v && java -version
          ./gradlew sonarqube || mvn -DskipTests=false sonar:sonar || sonar-scanner

```

### C. GitLab CI: `.gitlab-ci.yml`

```yaml
stages: [build, test, sonar]

variables:
  SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
  GIT_DEPTH: "0"

build:
  stage: build
  script:
    - mvn -B -DskipTests clean package

unit_tests:
  stage: test
  script:
    - mvn -B test jacoco:report

sonar:
  stage: sonar
  script:
    - mvn -B sonar:sonar -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_TOKEN
  only:
    - merge_requests
    - main

```

### D. Maven `pom.xml` snippet

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.11</version>
      <executions>
        <execution>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>
        <execution>
          <id>report</id>
          <phase>test</phase>
          <goals>
            <goal>report</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
    <plugin>
      <groupId>org.sonarsource.scanner.maven</groupId>
      <artifactId>sonar-maven-plugin</artifactId>
      <version>3.10.0.2594</version>
    </plugin>
  </plugins>
</build>

```

### E. Gradle (Kotlin DSL) `build.gradle.kts`

```kotlin
plugins {
  id("org.sonarqube") version "5.1.0.4882"
  jacoco
}

tasks.test { useJUnitPlatform() }

sonarqube {
  properties {
    property("sonar.projectKey", "your-org_your-project")
    property("sonar.host.url", System.getenv("SONAR_HOST_URL"))
    property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
  }
}

tasks.register<JacocoReport>("jacocoTestReport") {
  dependsOn(tasks.test)
  reports { xml.required.set(true); html.required.set(true) }
  classDirectories.setFrom(fileTree("build/classes/java/main").exclude("**/generated/**"))
}

```

### F. Jest Coverage (JS/TS) `package.json`

```json
{
  "scripts": {
    "test": "jest --coverage",
    "test:ci": "jest --coverage --runInBand"
  },
  "jest": {
    "collectCoverage": true,
    "coverageReporters": ["lcov", "text-summary"],
    "coverageDirectory": "coverage",
    "coverageThreshold": {"global": {"branches": 80, "functions": 80, "lines": 80, "statements": 80}}
  }
}

```

### G. Python Coverage `.coveragerc`

```
[run]
source = src
branch = True

[report]
omit =
  */tests/*
  */migrations/*

[xml]
output = coverage.xml

```

### H. README Badge (replace with your project key)

```markdown
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=your-org_your-project&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=your-org_your-project)

```

### I. API: Create/Assign Quality Gate (admin)

```bash
# Create gate (once)
curl -u $SONAR_TOKEN: "$SONAR_HOST_URL/api/qualitygates/create?name=NewCodeStrict"
# Add conditions (examples)
curl -u $SONAR_TOKEN: "$SONAR_HOST_URL/api/qualitygates/create_condition?gateName=NewCodeStrict&metric=coverage&op=LT&warning=80&error=80&period=1"
curl -u $SONAR_TOKEN: "$SONAR_HOST_URL/api/qualitygates/create_condition?gateName=NewCodeStrict&metric=new_duplicated_lines_density&op=GT&warning=3&error=3&period=1"
# Assign to project
curl -u $SONAR_TOKEN: "$SONAR_HOST_URL/api/qualitygates/select?projectKey=your-org_your-project&gateName=NewCodeStrict"

```

### J. Suppression Policy (commit as `docs/sonar-suppression-policy.md`)

```markdown
# Sonar Suppression Policy
- Prefer fixing or tuning rule profiles over suppressing.
- If suppressing, document the rationale and scope (link to PR/issue).
- Allowed mechanisms:
  - Rule‑specific annotations (e.g., `@SuppressWarnings("squid:S1319")`) with comment
  - `// NOSONAR` for a single line only, with trailing justification
- Forbidden:
  - File‑wide `NOSONAR` without approval
  - Project‑wide exclusions to hide real issues

```

---

## Output Format

- A PR titled like `ci(sonar): add coverage + PR decoration; enforce gate on new code` including:
    - Scanner config, CI workflow(s), coverage wiring, suppression policy, README badge
    - Links to Sonar project and Quality Gate check
    - Evidence screenshots and a concise changelog entry
- Post‑merge, the project analyzes cleanly and PRs are decorated with the selected gate and curated rules.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
