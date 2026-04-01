Playbook ID: playbook-0044f41728fa484bb368ff3625e07d13
Title: GS SpringBoot Migration
Macro: !gs_springboot_migration
Access: org


# Spring Boot 3.x Migration Playbook (Enterprise Scale)

---

## Overview

This playbook provides a phase-based migration framework for upgrading enterprise Java applications from Spring Boot 2.x to Spring Boot ${TARGET_SPRING_BOOT_VERSION}.x with JDK ${TARGET_JDK_VERSION}. It follows official Spring Boot migration patterns and is designed for single-module and multi-module repositories at enterprise scale.

**Migration Scope:**
- Spring Boot 2.x (2.6.x - 2.7.x) → Spring Boot ${TARGET_SPRING_BOOT_VERSION}.x
- Intermediate upgrade to Spring Boot 2.7.x (required step before 3.x per official guide)
- JDK 8/11/17 → JDK ${TARGET_JDK_VERSION}
- javax.* → jakarta.* namespace migration
- Spring Framework 5.x → 6.x, Spring Security 5.x → 6.x
- Spring Batch 4.x → 5.0 (if applicable)
- Configuration properties migration, dependency upgrades, API adaptation

**Not Covered:** Non-Spring Boot applications, cloud-specific migrations (unless Spring Cloud), database schema migrations, infrastructure changes, GraalVM Native Image.

**Prerequisites:**
- JDK ${TARGET_JDK_VERSION} installed and available
- Maven 3.9.0+ or Gradle 7.5+ (Maven 3.8.1+ minimum for JDK 17+)
- Git repository with clean working directory
- Current Spring Boot version: 2.6.0 or higher
- Recommended: Upgrade to Spring Boot 2.7.x FIRST before migrating to 3.x
- Test suite with reasonable coverage (>60% recommended)

---

## Required from User

Before starting this migration, the user MUST provide:

| # | Parameter | Variable | Example |
|---|-----------|----------|---------|
| 1 | Target Spring Boot version | `${TARGET_SPRING_BOOT_VERSION}` | 3.5.5 |
| 2 | Target JDK version | `${TARGET_JDK_VERSION}` | 21 |
| 3 | Artifact directory path | `${ARTIFACTS_DIR}` | docs/spring-boot-migration/artifacts |
| 4 | Jira ticket number (if required) | (user-specified) | PROJ-1234 |

If any parameter is not provided, **STOP and ASK** before proceeding.

---

## Specifications (Migration Success Criteria)

What MUST be true when the migration is complete:

| # | Specification | Verification |
|---|--------------|--------------|
| S1 | All 10 phases completed with stop gate logs (phase-0 through phase-9) | `ls ${ARTIFACTS_DIR}/../verification-logs/phase-*-stopgate.log \| wc -l` = 10 |
| S2 | All tests passing (100%) with same or more tests than Phase 0 baseline | `mvn test` exit code 0, test count >= baseline |
| S3 | Zero @Disabled/@Ignore annotations added during migration | `grep -r "@Disabled\|@Ignore" src/test/` returns empty |
| S4 | Application starts successfully (if testable without external dependencies) | Console shows "Started Application" |
| S5 | All artifacts created per phase stop gate requirements | Each phase stop gate verification passes |
| S6 | PR created with migration summary | PR exists with MIGRATION_SUMMARY.md |
| S7 | No hardcoded version downgrades | Dependency tree shows no regressions |
| S8 | All deprecated APIs replaced — no "working but deprecated" retained | `grep -r "@Deprecated" src/main/` shows no migration-related deprecations |

---

## Procedure

### Migration Phases Overview

| Phase | Title | Execution Strategy |
|:------|:------|:-------------------|
| **Phase 0** | Pre-Flight Analysis | Sequential |
| **Phase 1** | Test Stability Check | Sequential |
| **Phase 2** | Intermediate 2.7.x Upgrade | Sequential (conditional) |
| **Phase 3** | Core Version Upgrade | Sequential |
| **Phase 4** | Dependency Compatibility | Sequential |
| **Phase 5** | Framework Upgrade | Sequential (sections 5.1–5.3) |
| **Phase 6** | Namespace Migration | Sequential (OpenRewrite) |
| **Phase 7** | Configuration Migration | Sequential (per-config file) |
| **Phase 8** | API Adaptation | Sequential (sections 8.1–8.4) |
| **Phase 9** | Build & Test | Sequential (iterative) |

Execute phases in strict sequential order. See each phase header for organization guidance.

**Reference Appendices:**
- **Appendix A:** Common Migration Issues — referenced by Phase 9 for error pattern identification
- **Appendix B:** Transient Failure Patterns — referenced by Phase 1 for failure categorization
- **Appendix C:** Session Resumption Protocol — use if resuming a migration from a previous session
- **Appendix D:** Rollback Strategy — use if rollback is needed at any phase

### Commit Message Standard

**Required Format:** `[phase-{N}[.{subsection}]] {Description}`

**Examples:**
- `[phase-0] Complete pre-flight analysis`
- `[phase-5] Update Spring Security to 6.x`
- `[phase-7.1] Rename deprecated properties`
- `[phase-8] Skip REQ-P8-7.1 - no Redis in project`

**Single-Commit-Per-Phase Rule:**
Per-requirement commit messages shown throughout the document are REFERENCE LABELS only. Do NOT execute these commits individually. ALL phase work is committed in a SINGLE commit after the stop gate passes. The commit message format for the actual commit is: `[phase-N] Complete Phase N: {Phase Title}`.

### Artifact Naming Convention

**Standard Format:** `phase-{N}-{purpose}.{ext}`

**Examples:**
- `phase-0-build-configuration.md`
- `phase-0-dependency-tree-before.txt`
- `phase-1-test-baseline.md`
- `phase-4-dependency-rationale.md`
- `phase-7-applicability.md`
- `phase-9-final-validation.log`

### Build Tool Command Reference

All phase commands are shown in Maven syntax by default. **For Gradle projects, substitute commands using this table before execution.** Phase 0 detects the build tool — use that result throughout.

| Maven Command | Gradle Equivalent |
|--------------|-------------------|
| `mvn -s <settings-file> clean compile` | `./gradlew clean compileJava` |
| `mvn -s <settings-file> clean test` | `./gradlew clean test` |
| `mvn -s <settings-file> verify` | `./gradlew check` |
| `mvn -s <settings-file> clean install` | `./gradlew clean build` |
| `mvn -s <settings-file> spring-boot:run`| `./gradlew bootRun` |
| `mvn -s <settings-file> dependency:tree` | `./gradlew dependencies` |
| `mvn -s <settings-file> dependency:list` | `./gradlew dependencies --configuration runtimeClasspath` |
| `mvn help:effective-pom` | `./gradlew dependencies --scan` (no direct equivalent) |
| `mvn help:evaluate -Dexpression=X -q -DforceStdout` | `./gradlew properties \| grep X` |
| `mvn -s <settings-file> rewrite:dryRun` | `./gradlew rewriteDryRun` |
| `mvn -s <settings-file> rewrite:run` | `./gradlew rewriteRun` |
| `mvn -s <settings-file> versions:display-dependency-updates` | `./gradlew dependencyUpdates` (requires [ben-manes plugin](https://github.com/ben-manes/gradle-versions-plugin)) |
| `mvn -s <settings-file> dependency:get -Dartifact=G:A:V` | `./gradlew dependencyInsight --dependency G:A` |
| `mvn -s <settings-file> test -Dtest="*Pattern*"` | `./gradlew test --tests "*Pattern*"` |

**Gradle Settings:** Where Maven uses `-s <settings-file>`, Gradle resolves repositories from `build.gradle` `repositories {}` block, `gradle.properties`, or `init.gradle` scripts detected in Phase 0 (REQ-P0-1.2). No per-command flag is needed — ensure repositories are configured in the project before Phase 1.

**Artifact Output:** All `tee` and redirect patterns (`2>&1 | tee file.log`) work identically for both build tools. Only the build command itself changes.

### Configuration Parameters

**Global Variables (Substitute Before Use):**

```
# Version Configuration
TARGET_SPRING_BOOT_VERSION="${TARGET_SPRING_BOOT_VERSION}"  # Set by user (e.g., 3.5.5)
TARGET_JDK_VERSION="21"
INTERMEDIATE_SPRING_BOOT_VERSION="2.7.18"

# Build Configuration
MAVEN_SETTINGS_FILE="maven-settings.xml"  # or path to custom settings
GRADLE_INIT_SCRIPT=""  # optional Gradle init script

# Verification Logging
LOG_DIR="docs/spring-boot-migration/verification-logs"
ARTIFACTS_DIR="docs/spring-boot-migration/artifacts"
```

---

## Forbidden Actions

These rules prevent the most common and costly migration errors. Violating any of these is grounds for stopping and escalating.

1. **Never execute tasks from different phases in parallel.** Complete one phase fully before starting the next. (G1)
2. **All stop gate checks must pass with visible evidence before proceeding.** Do not skip or assume a gate "would have passed." Run it. (G3)
3. **Never downgrade dependency versions** unless explicitly required for compatibility and approved by the user. (G4)
4. **Trust Spring Boot BOM for managed dependencies.** Remove explicit versions that are managed by the BOM. Do not override BOM versions without documented rationale. (G5)
5. **Fix flaky tests before migration.** Migration must not mask pre-existing test issues. Establish a stable test baseline in Phase 1 before proceeding. (G6)
6. **If `maven-settings.xml` or custom Gradle settings exist, use them in ALL build commands.** Never omit the settings flag. (G8)
7. **If blocked >30 minutes or require an architectural decision, STOP and escalate to the user.** Do not make judgment calls on ambiguous situations. (G10)
8. **Use automated tools (OpenRewrite, Properties Migrator) over manual shortcuts.** Verify tool attempts before using alternatives. Even if you expect a tool to fail, run it anyway — the attempt log IS the deliverable. (G11)
9. **No test shortcuts.** FORBIDDEN: commenting out/deleting failing asserts, adding @Disabled/@Ignore, wrapping in try/catch that suppresses failure, asserting trivial/incorrect outcomes, documenting failure as "pre-existing" and proceeding. The ONLY acceptable outcomes: fix the failure, or escalate to the user. (G13)
10. **Test count must not decrease.** Capture baseline in Phase 0, verify in Phases 1 & 9. If test count drops, investigate — tests were lost or disabled during migration. (G18)
11. **No autonomous skipping.** If blocked, STOP and message the user: "I am blocked on [REQ-ID]. I need your approval to [ACTION]." WAIT for response. Never use phrases like "not feasible", "too complex", "constraints prevent", "for efficiency", etc. to justify skipping a requirement. (G20)

**Phase-Specific Prohibited Actions:**

| Phase | Prohibited | Required Instead |
|-------|-----------|------------------|
| Phase 1 | Adding `@Disabled`/`@Ignore`; mocking externals instead of TestContainers; absurd timeouts (>60s) | Fix root cause using proper test patterns |
| Phase 4 | Using "latest"/"RELEASE" qualifiers; blind upgrade without compatibility check; upgrading preemptively without proving incompatibility via failed build/test | Document version choice rationale |
| Phase 6 | Manual find-replace without attempting OpenRewrite | Document OpenRewrite attempt in `openrewrite-unavailable.log` |
| Phase 7 | IDE find-replace instead of Properties Migrator | Add Properties Migrator, run app, use tool output as source of truth |
| Phase 8 | Keeping deprecated APIs because they "still work" | Replace ALL deprecated APIs with new equivalents |
| Phase 9 | Disabling, deleting, or @Ignore-ing tests to achieve pass rate; claiming tests pass without pasting visible evidence | Fix root causes; paste "BUILD SUCCESS" and "Tests run: X, Failures: 0" as proof |

---

## Advice (Correcting Observed Failure Modes)

| ID | Advice |
|----|--------|
| **A1** | Even if you expect a tool (OpenRewrite, Properties Migrator) to fail in an enterprise environment, **run it anyway**. The attempt log IS the deliverable, not the success. |
| **A2** | When an internal GS library has no compatible version, do NOT silently keep the old version and move on. **STOP and escalate.** The user needs to know. |
| **A3** | "Working but deprecated" is never acceptable. If you find yourself thinking "this still works, I'll skip the replacement," that IS the shortcut the guide prohibits. |
| **A4** | Artifact creation is equally important as code changes. A phase with correct code but missing artifacts is an **INCOMPLETE** phase. |
| **A5** | If tests pass on the first iteration, still create the iteration-1 log files. The artifacts prove the process was followed, not just that the outcome was achieved. |
| **A6** | When a requirement says [CONDITIONAL], the detection step is **MANDATORY** even if you are confident the condition is false. The detection output is the proof. |
| **A7** | Stop gates are not optional checkpoints — they are **WALLS**. You cannot proceed by deciding the gate "would have passed." You must actually run it. |
| **A8** | When you encounter a decision point not covered by the guide, the default action is **STOP and ASK**, not make a judgment call and continue. |

---

# Phase 0: Pre-Flight Analysis
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Skipping dependency tree/list capture
- Starting Phase 1 without all 4 baseline artifacts created
- Guessing build tool type instead of running detection commands
**Advice:** This phase is read-only. If any command modifies files, STOP — you are doing it wrong.

**Goal:** Capture current state, detect configuration, verify prerequisites.

### 0.0 TODO List Management

**REQ-P0-0.1: Dynamic Task Tracking**
**Action:** Use the `TodoWrite` tool to track migration progress throughout session. Create one TODO item per REQ-ID. Do NOT consolidate multiple REQ-IDs into summary items.

**Steps:**
1. When starting each phase, scan all `**REQ-PN-*` headings in that phase (including REQ-PN-SG)
2. Create one TodoWrite item per REQ-ID found
3. Mark each item `in_progress` before executing, `completed` after
4. Every phase MUST end with its REQ-PN-SG item marked completed
5. Phase status changes to `in_progress` when first item starts, `completed` when REQ-PN-SG is done

**Expected TODO Structure:**

```markdown
## Phase N: {Phase Title} (Status: not-started | in-progress | completed)
- [ ] REQ-PN-X.Y: {Requirement Name}
- [ ] REQ-PN-SG: Stop Gate Verification
```

**Example (Phase 0):**
```markdown
## Phase 0: Pre-Flight Analysis (Status: in-progress)
- [x] REQ-P0-0.1: Dynamic Task Tracking
- [ ] REQ-P0-1.1: Detect Build Tool Type
- [ ] REQ-P0-1.2: Discover Custom Build Settings
- [ ] REQ-P0-2.1: Capture Pre-Migration Dependency Tree
- [ ] REQ-P0-3.1: Document Current Spring Boot Version
- [ ] REQ-P0-SG: Stop Gate Verification
```

**Verification:**
```
# Verify TodoWrite items created for current phase
# Each REQ-ID in the phase should have a corresponding TODO item
# Verify Phase 0 items are marked in-progress
```

**Artifact:** N/A — TodoWrite task list is the artifact

---

## 0.1 Build Tool Detection

**REQ-P0-1.1: Detect Build Tool Type**
**Action:** Identify Maven or Gradle, locate configuration files, and detect project structure (single vs multi-module).

**Steps:**
1. Search for `pom.xml` in project root
2. Search for `build.gradle`, `build.gradle.kts`, `settings.gradle`
3. Detect wrapper scripts: `mvnw`, `mvnw.cmd`, `gradlew`, `gradlew.bat`
4. Determine build tool using decision logic below
5. Detect multi-module structure:
   - Maven: check for `<modules>` in root `pom.xml`
   - Gradle: check for `include` in `settings.gradle`
6. If multi-module detected:
   - List all modules and parent relationships: `mvn help:evaluate -Dexpression=project.modules -q -DforceStdout` (Maven) or `./gradlew projects` (Gradle)
   - Identify module dependency order (reactor order): `mvn validate -pl` or check `<modules>` declaration order
   - Identify which module owns the parent POM / Spring Boot parent inheritance

**Decision Logic:**
```
IF pom.xml EXISTS AND no Gradle files → MAVEN
IF Gradle files EXIST AND no pom.xml → GRADLE
IF both exist → MAVEN (preferred) + warn about hybrid
IF neither → STOP (unsupported build system)
```

**Artifact:** Document in `## REQ-P0-1.1: Build Tool Configuration` section of `${ARTIFACTS_DIR}/phase-0-preflight-analysis.md`:
1. Build Tool name and version
2. Root configuration file path
3. Multi-module status (yes/no)
4. If multi-module: module list, dependency graph, reactor build order, which module(s) own Spring Boot parent/BOM

**Verification:**
```
# Maven
mvn --version
# Expected: Apache Maven 3.9.0+

# Gradle
./gradlew --version
# Expected: Gradle 7.5+

# Multi-module (if applicable)
# Maven: mvn help:evaluate -Dexpression=project.modules -q -DforceStdout
# Gradle: ./gradlew projects
```

---

**REQ-P0-1.2: Discover Custom Build Settings**
**Action:** Detect custom Maven settings or Gradle init scripts.

**Steps:**
1. Maven: run the detection commands below.
```
# Check project root
find . -maxdepth 1 -name "*settings*.xml"

# Check user .m2 directory
ls ~/.m2/settings.xml

# Check MAVEN_SETTINGS environment variable
echo $MAVEN_SETTINGS
```
2. Gradle: run the detection commands below.
```
# Check project root
find . -maxdepth 1 -name "init.gradle*"

# Check gradle.properties
cat gradle.properties | grep -i "repository\|proxy\|credentials"

# Check Gradle home
ls ~/.gradle/init.d/
```
3. For multi-module: also check parent and child module directories for module-specific settings (`*/pom.xml` profiles, `*/build.gradle` custom repositories)
4. Document findings in `## REQ-P0-1.2: Custom Build Settings` section of `${ARTIFACTS_DIR}/phase-0-preflight-analysis.md`:
   - Settings file location and contents
   - Usage instructions for all build commands

**Artifact:** `${ARTIFACTS_DIR}/phase-0-preflight-analysis.md` (section: `REQ-P0-1.2: Custom Build Settings`)

**Verification:**
```
# If custom settings found, test
mvn -s <file> help:effective-settings
```

---

### 0.2 Dependency Tree Capture

**REQ-P0-2.1: Capture Pre-Migration Dependency Snapshot**
**Action:** Snapshot complete dependency graph before any changes. For multi-module projects, capture per-module dependency information.

**Steps:**
1. Create artifacts directory: `mkdir -p ${ARTIFACTS_DIR}`
2. Generate raw dependency outputs (kept as separate files for Phase 9 diff comparison)
3. If multi-module: run per-module dependency tree using `mvn -pl <module> -am dependency:tree` (or Gradle `:<module>:dependencies`)
4. Create a summary table in the consolidated artifact

**Maven Commands:**
```
# Create artifacts directory first
mkdir -p ${ARTIFACTS_DIR}

# Generate effective POM
mvn -s <settings-file> help:effective-pom \
    -Doutput=${ARTIFACTS_DIR}/phase-0-effective-pom-before.xml

# Generate dependency tree (hierarchical — shows resolution paths)
mvn -s <settings-file> dependency:tree -Dverbose=true \
    -DoutputFile=${ARTIFACTS_DIR}/phase-0-dependency-tree-before.txt

# Generate dependency list (flat — final resolved versions)
mvn -s <settings-file> dependency:list \
    -DoutputFile=${ARTIFACTS_DIR}/phase-0-dependency-list-before.txt

# Multi-module: per-module dependency tree (if applicable)
# mvn -s <settings-file> -pl <module-name> -am dependency:tree
```

**Gradle Commands:**
```
# Create artifacts directory
mkdir -p ${ARTIFACTS_DIR}

# Generate dependency tree
./gradlew dependencies --configuration compileClasspath \
    > ${ARTIFACTS_DIR}/phase-0-dependency-tree-before.txt

# Generate dependency insight
./gradlew dependencyInsight --dependency spring-boot \
    > ${ARTIFACTS_DIR}/phase-0-spring-boot-deps-before.txt

# Multi-module: per-module dependencies (if applicable)
# ./gradlew :<module-name>:dependencies
```

**Artifact:**
- Raw output files (for Phase 9 comparison):
  - `${ARTIFACTS_DIR}/phase-0-effective-pom-before.xml` (Maven only)
  - `${ARTIFACTS_DIR}/phase-0-dependency-tree-before.txt`
  - `${ARTIFACTS_DIR}/phase-0-dependency-list-before.txt`
- Document in `## REQ-P0-2.1: Dependency Snapshot Summary` section of `${ARTIFACTS_DIR}/phase-0-preflight-analysis.md`:
  - Summary table: Module | Key Dependency | Version | Scope | Managed by BOM?
  - Total dependency count
  - References to raw output file locations

**Verification:**
```
# Verify files exist and have content
ls -lh ${ARTIFACTS_DIR}/phase-0-*.txt
# Expected: Files > 0 bytes

# Count dependencies
grep -c ":" ${ARTIFACTS_DIR}/phase-0-dependency-list-before.txt
# Expected: >50 dependencies
```

---

### 0.3 Current Version Baseline

**REQ-P0-3.1: Document Current Spring Boot Version**
**Action:** Extract current Spring Boot version and major components. For multi-module projects, document version per module.

**Steps:**
1. Detect Spring Boot version from root POM/build file
2. If multi-module (detected in REQ-P0-1.1), iterate each module and extract its Spring Boot version
3. Document all findings in the consolidated artifact

**Maven Detection:**
```
# From parent POM
grep -A 2 "spring-boot-starter-parent" pom.xml | grep version

# From dependency management
mvn -s <settings-file> help:evaluate -Dexpression=spring-boot.version -q -DforceStdout

# Multi-module: check each module's effective Spring Boot version
# mvn -s <settings-file> -pl <module-name> help:evaluate -Dexpression=spring-boot.version -q -DforceStdout
```

**Gradle Detection:**
```
# From build.gradle
grep "org.springframework.boot" build.gradle | grep version

# From Gradle properties
./gradlew properties | grep "springBootVersion"
```

**Artifact:** Document in `## REQ-P0-3.1: Version Baseline` section of `${ARTIFACTS_DIR}/phase-0-preflight-analysis.md`:
- Version table (for multi-module, one row per module):
  | Module | Spring Boot Version | Parent POM | Inherits From |
  |--------|-------------------|------------|---------------|
  | root   | 2.7.18            | spring-boot-starter-parent | - |
  | core   | (inherited)       | root       | root |
  | api    | (inherited)       | root       | root |
- Current JDK version and target JDK version
- Major dependency table (name, current version, managed by BOM?)
- Upgrade path assessment (all modules same version? any outliers?)

**Verification:**
```
# Verify current version is 2.6.0+
# If < 2.6.0, requires intermediate upgrade

# Verify consolidated artifact has version baseline section
grep -q "Version Baseline" ${ARTIFACTS_DIR}/phase-0-preflight-analysis.md && \
    echo "PASS: Version baseline documented" || echo "FAIL: Version baseline missing"
```

---

### REQ-P0-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 1 (Test Stability Check) until completed.**

**Action:** Verify all Phase 0 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**
```
# Consolidated artifact check
test -f ${ARTIFACTS_DIR}/phase-0-preflight-analysis.md && echo "PASS: phase-0-preflight-analysis.md" || echo "FAIL: missing phase-0-preflight-analysis.md"

# Raw output file checks
test -f ${ARTIFACTS_DIR}/phase-0-effective-pom-before.xml && echo "PASS: phase-0-effective-pom-before.xml" || echo "FAIL: missing phase-0-effective-pom-before.xml"
test -f ${ARTIFACTS_DIR}/phase-0-dependency-tree-before.txt && echo "PASS: phase-0-dependency-tree-before.txt" || echo "FAIL: missing phase-0-dependency-tree-before.txt"
test -f ${ARTIFACTS_DIR}/phase-0-dependency-list-before.txt && echo "PASS: phase-0-dependency-list-before.txt" || echo "FAIL: missing phase-0-dependency-list-before.txt"

# Content validation - verify consolidated artifact has all required sections
grep -q "Build Tool Configuration" ${ARTIFACTS_DIR}/phase-0-preflight-analysis.md && echo "PASS: Section 'Build Tool Configuration' present" || echo "FAIL: Section 'Build Tool Configuration' missing"
grep -q "Custom Build Settings" ${ARTIFACTS_DIR}/phase-0-preflight-analysis.md && echo "PASS: Section 'Custom Build Settings' present" || echo "FAIL: Section 'Custom Build Settings' missing"
grep -q "Dependency Snapshot Summary" ${ARTIFACTS_DIR}/phase-0-preflight-analysis.md && echo "PASS: Section 'Dependency Snapshot Summary' present" || echo "FAIL: Section 'Dependency Snapshot Summary' missing"
grep -q "Version Baseline" ${ARTIFACTS_DIR}/phase-0-preflight-analysis.md && echo "PASS: Section 'Version Baseline' present" || echo "FAIL: Section 'Version Baseline' missing"

# Clean working directory check
test -z "$(git status --porcelain docs/spring-boot-migration/)" || echo "WARNING: Unstaged changes in migration directory"
```

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 0 at $(date -u)" > ${LOG_DIR}/phase-0-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-0] Complete Phase 0: Pre-Flight Analysis"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-0-preflight-analysis.md` (consolidated: Build Tool Config + Custom Settings + Dependency Summary + Version Baseline)
- `${ARTIFACTS_DIR}/phase-0-effective-pom-before.xml` (raw output)
- `${ARTIFACTS_DIR}/phase-0-dependency-tree-before.txt` (raw output)
- `${ARTIFACTS_DIR}/phase-0-dependency-list-before.txt` (raw output)

**Decision:** Proceed to Phase 1 (Test Stability Check)

# Phase 1: Test Stability Check
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Using @Ignore or @Disabled to hide transient failures
- Claiming "tests pass" without running the full test suite and capturing output
- Modifying application code to fix test failures — only test infrastructure changes allowed
- Mocking external services instead of using TestContainers/embedded infrastructure
- Increasing timeouts to absurd values (>60s)
- Adding `Thread.sleep()` to fix race conditions
- Changing assertions to be less strict
- Removing test verification logic

**Advice:**
- Transient failures MUST be fixed before migration starts. A flaky baseline makes post-migration failure analysis impossible.
- Fix root cause (infrastructure, configuration, test data)
- Use proper test patterns (TestContainers, WireMock, embedded servers)
- Add proper test cleanup (@AfterEach, @DirtiesContext)
- Fix race conditions with proper synchronization (`await().atMost()`)
- Make timeouts environment-aware (CI vs local)

**Goal:** Establish test baseline and fix ALL transient test failures before migration.
---

## 1.0 Prerequisites

**REQ-P1-0.1: Verify Phase 0 Completion**
**Action:** Confirm Phase 0 commit exists before proceeding.

**Steps:**
1. Verify Phase 0 commit: `git log --all --grep="[phase-0] Complete Phase 0" --oneline | head -1`
2. If no result, STOP — Phase 0 has not been completed

**Verification:**
```
git log --all --grep="[phase-0] Complete Phase 0" --oneline | head -1
# Expected: commit hash with phase-0 message
```

**Artifact:** N/A — verification only

---

## 1.1 Test Baseline & Transient Failure Resolution

**REQ-P1-1.1: Test Baseline Validation & Transient Failure Fix**
**Applicability:** MANDATORY
**Detection:** N/A — always executes
**Action:** Run tests, categorize failures, fix all transient issues immediately, document non-transient failures.

**Steps:**

**Step 1: Run Tests (Detect Failures)**
```
# Maven - Run with random order (detect flaky tests)
mvn -s <settings-file> clean test -Dmaven.surefire.runOrder=random

# Run again to verify consistency
mvn -s <settings-file> test -Dmaven.surefire.runOrder=random

# Gradle
./gradlew clean test --rerun-tasks
```

**Step 2: Categorize Each Failure**

Categorize each failure using the categories defined in **Appendix B: Transient Failure Patterns Reference Guide** (TRANSIENT-TIMEOUT, TRANSIENT-INFRASTRUCTURE, TRANSIENT-FLAKY, TRANSIENT-ENVIRONMENT, TRANSIENT-EXTERNAL, TRANSIENT-TEST-LOGIC, or NON-TRANSIENT).

**Step 3: Fix All Transient Failures Immediately**

Fix all transient failures using patterns from **Appendix B: Transient Failure Patterns Reference Guide**. See Phase 1 Forbidden Actions above for prohibited shortcuts.

**Step 4: Document Non-Transient Failures**

If test failure is **APPLICATION CODE BUG**:

1. **DO NOT FIX APPLICATION CODE** in Phase 1
2. Create `${ARTIFACTS_DIR}/phase-1-application-bugs.md` with one entry per bug containing: Test name, Issue description, Root cause, Action required, Status
3. **ESCALATE TO USER** - wait for decision before proceeding

**Step 5: Generate Final Report**

Document findings in `${ARTIFACTS_DIR}/phase-1-test-baseline.md` (see Artifact section below for required contents).

**Verification:**
- All transient failures fixed
- Non-transient failures documented and escalated (if any)
- Final test run passes 100%
```
# Verify no shortcuts taken
git diff HEAD~1 | grep -E "@Disabled|@Ignore|Thread.sleep|timeout.*[0-9]{5,}" && \
    { echo "ERROR: Shortcut detected in test fixes"; exit 1; }
echo "PASS: No shortcuts detected"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-1-test-baseline.md` with required contents:
- Initial test run results table (for multi-module, one row per module):
  | Module | Total | Passed | Failed | Skipped | Success Rate |
  |--------|-------|--------|--------|---------|-------------|
  | root   | 250   | 248    | 2      | 0       | 99.2%       |
  (For single-module repos, use project name as Module)
- Transient failures fixed table (columns: Module, Category, Count, Example Tests) — one row per category
- Final test run results table (same format as initial, one row per module)
- Non-transient failures section (or "None found")
- Test stability score (stability %, flakiness %)
- Migration readiness recommendation

---

### REQ-P1-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 2 (Intermediate Upgrade) until completed.**

**Action:** Verify all Phase 1 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**
```
# Artifact existence checks
test -f ${ARTIFACTS_DIR}/phase-1-test-baseline.md && echo "PASS: phase-1-test-baseline.md" || echo "FAIL: missing phase-1-test-baseline.md"

# Content validation
grep -qE "Tests run:|test|Test" ${ARTIFACTS_DIR}/phase-1-test-baseline.md && echo "PASS: Test baseline has content" || echo "FAIL: Test baseline may be placeholder"
# Clean working directory check
test -z "$(git status --porcelain docs/spring-boot-migration/)" || echo "WARNING: Unstaged changes in migration directory"
```

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 1 at $(date -u)" > ${LOG_DIR}/phase-1-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-1] Complete Phase 1: Test Stability Check"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-1-test-baseline.md`

**Decision:** Proceed to Phase 2 (Intermediate Upgrade)

# Phase 2: Intermediate Spring Boot 2.7.x Upgrade (Conditional)
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Skipping the 2.7.x intermediate step and upgrading directly to 3.x from < 2.7.0
- Proceeding to Phase 3 if the decision logic determined Phase 2 is required but was not executed
**Advice:** Even if Phase 2 is bypassed (already on 2.7.x), the decision artifact (phase-2-decision.txt) MUST still be created.

**CONDITIONAL PHASE NOTE:** Even though Phase 2 is conditional (executes only if version < 2.7.0), you MUST still enter this phase, execute REQ-P2-0.1 (the decision logic), create the `phase-2-decision.txt` artifact, and run the stop gate. "Conditional" means the UPGRADE WORK (REQ-P2-1.1) may be bypassed — NOT that the entire phase is bypassed. The decision artifact and stop gate are ALWAYS required.

**Goal:** Upgrade to Spring Boot 2.7.x IF current version < 2.7.0 to minimize migration risks.
---

## 2.0 Prerequisites

**REQ-P2-0.0: Verify Phase 1 Completion**
**Action:** Confirm Phase 1 commit exists before proceeding.

**Steps:**
1. Verify Phase 1 commit: `git log --all --grep="[phase-1] Complete Phase 1" --oneline | head -1`
2. If no result, STOP — Phase 1 has not been completed

**Verification:**
```
git log --all --grep="[phase-1] Complete Phase 1" --oneline | head -1
# Expected: commit hash with phase-1 message
```

**Artifact:** N/A — verification only

---

## 2.1 Intermediate Upgrade Applicability Check

**REQ-P2-0.1: Determine Need for Spring Boot 2.7.x Intermediate Upgrade**
**Action:** Check current Spring Boot version and determine if an intermediate upgrade to 2.7.x is required before proceeding to 3.x.

**Steps:**
1. Get current Spring Boot version:
   - Maven: `mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout`
   - Gradle: check `plugins { id 'org.springframework.boot' version '...' }` in `build.gradle`
   - For multi-module: check root/parent POM version (child modules typically inherit)
2. Parse major.minor from the version string
3. Decision:
   - If version < 2.7.0 → write `EXECUTE_PHASE_2=true` to `${ARTIFACTS_DIR}/phase-2-decision.txt` with reason (e.g., "Phase 2 REQUIRED: Current version 2.5.4 < 2.7.0. Target: Spring Boot 2.7.18")
   - If version >= 2.7.0 → write `EXECUTE_PHASE_2=false` to `${ARTIFACTS_DIR}/phase-2-decision.txt` with reason (e.g., "Phase 2 SKIPPED: Already on Spring Boot 2.7.x or higher (2.7.18)")

**Artifact:** `${ARTIFACTS_DIR}/phase-2-decision.txt`

**Verification:**
```
# Verify decision artifact exists and has content
test -f ${ARTIFACTS_DIR}/phase-2-decision.txt && echo "PASS: Decision artifact exists" || \
    { echo "FAIL: Decision artifact missing"; exit 1; }

grep -q "EXECUTE_PHASE_2=" ${ARTIFACTS_DIR}/phase-2-decision.txt && \
    echo "PASS: Decision recorded" || echo "FAIL: Decision not recorded"
```

---

## 2.1 Intermediate Version Upgrade

**REQ-P2-1.1: Upgrade to Spring Boot 2.7.18**
**Applicability:** [CONDITIONAL] Only if `phase-2-decision.txt` contains `EXECUTE_PHASE_2=true`
**Action:** Upgrade to Spring Boot 2.7.18 (latest stable 2.x version).

**Decision Table:**

| Current Version | Action | Rationale |
|----------------|--------|-----------|
| >= 2.7.0 | BYPASS Phase 2 (upgrade not needed) | Already on target |
| 2.6.x, 2.5.x, 2.4.x | EXECUTE Phase 2 | Intermediate step reduces risk |
| < 2.4.0 | STOP - Manual upgrade required | Too old for automation |

**Steps:**

**1. Update Spring Boot Version:**
```xml
<!-- Maven: pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version> <!-- Latest stable 2.x -->
</parent>
```

```groovy
// Gradle: build.gradle
plugins {
    id 'org.springframework.boot' version '2.7.18'
}
```

**2. Update Spring Cloud Version (if used):**
```xml
<!-- Maven: pom.xml -->
<properties>
    <spring-cloud.version>2021.0.8</spring-cloud.version> <!-- Compatible with 2.7.x -->
</properties>
```

**3. Build and Run Tests:**
```
# Maven
mvn -s <settings-file> clean install

# Gradle
./gradlew clean build
```

**4. Fix Deprecation Warnings:**

Spring Boot 2.7.x adds warnings for APIs removed in 3.x. Fix NOW:

**Common Deprecations:**

| Deprecated | Replacement | Fix |
|------------|-------------|-----|
| `spring.datasource.initialization-mode` | `spring.sql.init.mode` | Update properties |
| `WebSecurityConfigurerAdapter` | Component-based security | Defer to Phase 5 |
| `@RequestMapping` (no method) | `@GetMapping`, `@PostMapping` | Explicit methods |

**Example Fixes:**

```yaml
# BEFORE
spring:
  datasource:
    initialization-mode: always

# AFTER
spring:
  sql:
    init:
      mode: always
```

```
// BEFORE
@RequestMapping("/users")
public List<User> getUsers() { }

// AFTER
@GetMapping("/users")
public List<User> getUsers() { }
```

**5. Verify Application Startup:**
```
mvn -s <settings-file> spring-boot:run
```

**6. Create Intermediate Upgrade Report:**

Document findings in `${ARTIFACTS_DIR}/phase-2-intermediate-upgrade.md` (see Artifact section below for required contents).

**7. Multi-Module Considerations:**
- Update version in root/parent POM only (child modules inherit)
- Run `mvn -s <settings-file> clean install` from the project root (builds all modules in reactor order)
- For Gradle multi-module: update version in root `build.gradle`; run `./gradlew clean build` from root
- Verify all modules compile and pass tests before proceeding

**Verification:**
```
# Verify upgrade was applied
mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout
# Expected: 2.7.18

# Verify all tests pass after upgrade
mvn -s <settings-file> clean test
# Expected: BUILD SUCCESS

# For multi-module: verify all modules built
mvn -s <settings-file> clean install
# Expected: BUILD SUCCESS for all modules in reactor
```

**Artifact:** `${ARTIFACTS_DIR}/phase-2-intermediate-upgrade.md` with required contents:
- Version change (previous, current, date)
- Upgrade status (COMPLETED / FAILED)
- Build status table (for multi-module, one row per module):
  | Module | Build Result | Test Count | Failures | Startup | Notes |
  |--------|-------------|------------|----------|---------|-------|
  | root   | SUCCESS     | 250        | 0        | OK      |       |
  (For single-module repos, use project name as Module)
- Deprecation warnings fixed (numbered list with file references)
- Outstanding issues
- Recommendation to proceed or not

---

### REQ-P2-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 3 (Core Version Upgrade) until completed.**

**Action:** Verify all Phase 2 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**
```
# Artifact existence checks
test -f ${ARTIFACTS_DIR}/phase-2-decision.txt && echo "PASS: phase-2-decision.txt" || echo "FAIL: missing phase-2-decision.txt"
# Clean working directory check
test -z "$(git status --porcelain docs/spring-boot-migration/)" || echo "WARNING: Unstaged changes in migration directory"
```

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 2 at $(date -u)" > ${LOG_DIR}/phase-2-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-2] Complete Phase 2: Intermediate Upgrade"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-2-decision.txt` (always required)
- `${ARTIFACTS_DIR}/phase-2-intermediate-upgrade.md` (if upgrade was executed)

**Decision:** Proceed to Phase 3 (Core Version Upgrade)

# Phase 3: Core Version Upgrade (Spring Boot & JDK)
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Upgrading to Spring Boot 3.x without being on 2.7.x first
- Keeping explicit version overrides for BOM-managed dependencies after cleanup
- Skipping compilation verification after the version bump

**Advice:**
- Compilation WILL fail after this phase — that is expected. The goal is to upgrade and document the compilation status, not to fix all errors here.
- **Multi-module version upgrade patterns** (if multi-module project per Phase 0 analysis):
  - **Pattern A (Parent inherits from spring-boot-starter-parent):** Update the parent POM only. Child modules inherit the version automatically.
  - **Pattern B (BOM import in dependencyManagement):** Update the BOM version in the parent POM's `<dependencyManagement>` section:
    ```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${TARGET_SPRING_BOOT_VERSION}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    ```
  - **Pattern C (Internal parent POM):** Update the internal parent version AND the Spring Boot BOM version. The internal parent (e.g., `com.gs.up:up`) may manage the Spring Boot version separately.
  - Do NOT update child module POMs individually unless they explicitly override the Spring Boot version.

**Goal:** Upgrade Spring Boot to 3.x and JDK to 21.

---

## 3.0 Prerequisites

**REQ-P3-0.1: Verify Prerequisites**
**Action:** Confirm Phase 2 completion and verify current Spring Boot version is 2.7.x before proceeding.

**Steps:**
1. Verify Phase 2 commit exists:
   - `git log --all --grep="[phase-2] Complete Phase 2" --oneline | head -1`
   - If no result, STOP — Phase 2 has not been completed
2. Verify current Spring Boot version is 2.7.x:
   - Maven: `mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout`
   - Gradle: check `plugins { id 'org.springframework.boot' version '...' }` in `build.gradle`
   - For multi-module: check root/parent POM version (child modules inherit)
3. If version is NOT 2.7.x, STOP — run Phase 2 first

**Verification:**
```
# Verify Phase 2 commit exists
git log --all --grep="[phase-2] Complete Phase 2" --oneline | head -1

# Verify Spring Boot version is 2.7.x
mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout
# Expected: 2.7.x
```

**Artifact:** N/A — verification only

---

## 3.1 Spring Boot Version Upgrade

**REQ-P3-1.1: Upgrade Spring Boot Parent Version**
**Action:** Update Spring Boot parent to ${TARGET_SPRING_BOOT_VERSION}.

**Steps:**
1. Update the Spring Boot version in the root build configuration:
   - For multi-module repos, apply the appropriate pattern (A/B/C) from Phase 3 Advice. Update root/parent POM only — child modules inherit.
2. Maven — update parent version:
```xml
<!-- BEFORE -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<!-- AFTER -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>${TARGET_SPRING_BOOT_VERSION}</version>  <!-- e.g., 3.2.2 -->
</parent>
```
3. Gradle — update plugin version:
```groovy
// BEFORE
plugins {
    id 'org.springframework.boot' version '2.7.18'
}

// AFTER
plugins {
    id 'org.springframework.boot' version '${TARGET_SPRING_BOOT_VERSION}'  // e.g., 3.2.2
}
```

**Verification:**
```
# Maven
mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout
# Expected: ${TARGET_SPRING_BOOT_VERSION}

# Gradle
./gradlew properties | grep springBootVersion
```

**Artifact:** N/A — POM/build file changes (verified by Verification commands above)

---

**REQ-P3-1.2: Update Java Version Requirement**
**Action:** Update Java version to 21.

**Steps:**
1. Update Java version properties in root build configuration:
   - For multi-module: update in root/parent POM; child modules inherit. If any child module overrides `java.version`, update there too.
2. Maven — update Java properties:
```xml
<!-- BEFORE -->
<properties>
    <java.version>11</java.version>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
</properties>

<!-- AFTER -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```
3. Gradle — update sourceCompatibility/targetCompatibility:
```groovy
// BEFORE
java {
    sourceCompatibility = '11'
    targetCompatibility = '11'
}

// AFTER
java {
    sourceCompatibility = '21'
    targetCompatibility = '21'
}
```

**Verification:**
```
# Maven
mvn help:evaluate -Dexpression=java.version -q -DforceStdout
# Expected: 21
```

**Artifact:** N/A — POM/build file changes (verified by Verification commands above)

---

## 3.2 BOM-Managed Version Cleanup

**REQ-P3-2.1: Remove Explicit Versions for BOM-Managed Dependencies**
**Applicability:** [CONDITIONAL] Only if project has explicit versions for BOM-managed dependencies
**Action:** Remove `<version>` tags for dependencies managed by Spring Boot BOM.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P3-2.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-3-applicability.md
   ```
2. Check for BOM conflicts: `mvn dependency:tree | grep "omitted for conflict" > bom-conflicts.txt`
   - For multi-module: run from root (covers all modules), or use `-pl <module>` for module-scoped analysis
3. If `bom-conflicts.txt` has content → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE - BOM-managed version conflicts detected" >> ${ARTIFACTS_DIR}/phase-3-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If `bom-conflicts.txt` is empty → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE (no BOM-managed version conflicts)" >> ${ARTIFACTS_DIR}/phase-3-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Generate effective POM before cleanup:
   - `mvn help:effective-pom > ${ARTIFACTS_DIR}/phase-3-effective-pom-before-cleanup.xml`
2. Identify BOM-managed dependencies by checking the effective POM's `<dependencyManagement>` section
3. For each dependency matching the BOM-managed list, remove the explicit `<version>` tag. Common BOM-managed dependencies:
   - `org.springframework.boot:spring-boot-starter-*`
   - `org.springframework:spring-*`
   - `com.fasterxml.jackson.*:jackson-*`
   - `org.hibernate.*:hibernate-*`
   - `org.junit.jupiter:junit-jupiter*`
   - `org.mockito:mockito-*`
   - `ch.qos.logback:logback-*`
4. For multi-module: BOM cleanup applies primarily to root POM. Child modules that explicitly override versions also need cleanup.
5. Gradle equivalent: remove explicit versions where the Spring Boot plugin or BOM platform manages them

**Before:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>2.7.18</version>  <!-- Remove this -->
</dependency>
```
```groovy
implementation 'org.springframework.boot:spring-boot-starter-web:2.7.18'
```

**After:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Version managed by Spring Boot BOM -->
</dependency>
```
```groovy
implementation 'org.springframework.boot:spring-boot-starter-web'
```

**Verification:**
```
# Generate effective POM AFTER cleanup
mvn help:effective-pom > ${ARTIFACTS_DIR}/phase-3-effective-pom-after-cleanup.xml

# Compare - versions should be SAME (BOM now managing)
diff <(grep "<version>" ${ARTIFACTS_DIR}/phase-3-effective-pom-before-cleanup.xml | sort) \
     <(grep "<version>" ${ARTIFACTS_DIR}/phase-3-effective-pom-after-cleanup.xml | sort)
# If diff shows changes, removed versions were NOT BOM-managed — rollback and verify

echo "PASS: BOM cleanup verified - effective versions unchanged"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-3-effective-pom-before-cleanup.xml` and `${ARTIFACTS_DIR}/phase-3-effective-pom-after-cleanup.xml` (if applicable)

---

## 3.3 JVM Module System Configuration

**REQ-P3-3.1: Configure JVM Module System for Reflection-Heavy Libraries**
**Applicability:** [CONDITIONAL] Only if using reflection-heavy libraries (GS Collections, Eclipse Collections, Reladomo, or similar)
**Action:** Configure `--add-opens` JVM flags to prevent `InaccessibleObjectException` in JDK 17+.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P3-3.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-3-applicability.md
   ```
2. For Maven projects, run: `grep -E "gs-collections|eclipse-collections|reladomo|com\.gs\." pom.xml */pom.xml 2>/dev/null`
3. For Gradle projects, run: `grep -E "gs-collections|eclipse-collections|reladomo|com\.gs\." build.gradle */build.gradle build.gradle.kts */build.gradle.kts 2>/dev/null`
4. If grep found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE - Requires JVM module configuration" >> ${ARTIFACTS_DIR}/phase-3-applicability.md
   ```
   Proceed to the **Steps** section below.
5. If grep found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE (no reflection-heavy libraries)" >> ${ARTIFACTS_DIR}/phase-3-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

JDK 17+ module system blocks reflective access to JDK internals by default, causing `InaccessibleObjectException` at runtime and in tests.

**Steps:**
1. Add `--add-opens` JVM flags to the appropriate build plugins.
   - For multi-module: configure in root POM surefire/failsafe plugins (inherited by all modules). If a child module has its own plugin configuration, update there too.
2. Maven — add to `maven-surefire-plugin` AND `maven-failsafe-plugin` `<configuration>`:
```xml
<argLine>
    --add-opens java.base/java.util=ALL-UNNAMED
    --add-opens java.base/java.lang=ALL-UNNAMED
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED
    --add-opens java.base/java.text=ALL-UNNAMED
    --add-opens java.base/java.util.concurrent=ALL-UNNAMED
</argLine>
```
   Also add to `spring-boot-maven-plugin` `<configuration>` for runtime:
```xml
<jvmArguments>
    --add-opens java.base/java.util=ALL-UNNAMED
    --add-opens java.base/java.lang=ALL-UNNAMED
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED
</jvmArguments>
```
3. Gradle — add to test and bootRun tasks in `build.gradle`:
```groovy
def jvmModuleOpens = [
    '--add-opens', 'java.base/java.util=ALL-UNNAMED',
    '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
    '--add-opens', 'java.base/java.lang.reflect=ALL-UNNAMED',
    '--add-opens', 'java.base/java.text=ALL-UNNAMED',
    '--add-opens', 'java.base/java.util.concurrent=ALL-UNNAMED'
]

tasks.withType(Test) {
    jvmArgs += jvmModuleOpens
}

bootRun {
    jvmArgs += jvmModuleOpens[0..5] // java.util, java.lang, java.lang.reflect
}
```

**Verification:**
1. Run tests: `mvn -s maven-settings.xml clean test 2>&1 | tee ${ARTIFACTS_DIR}/phase-3-jvm-module-test.log`
2. Check for module access errors: `grep -q "InaccessibleObjectException" ${ARTIFACTS_DIR}/phase-3-jvm-module-test.log`
3. If grep found matches → FAIL: `InaccessibleObjectException` still present — review `--add-opens` configuration. STOP and fix before proceeding.
4. If grep found NO matches → PASS: No module access errors detected.

**Artifact:** `${ARTIFACTS_DIR}/phase-3-jvm-module-test.log`

---

## 3.4 Compilation Status Report

**REQ-P3-4.1: Create Compilation Status Report**
**Action:** Run compilation after version upgrade and document the results. Compilation failures are EXPECTED at this stage.

**Steps:**
1. Run compilation and capture output:
   - Maven: `mvn -s <settings-file> clean compile 2>&1 | tee docs/spring-boot-migration/verification-logs/phase-3-compilation.log`
   - Gradle: `./gradlew compileJava 2>&1 | tee docs/spring-boot-migration/verification-logs/phase-3-compilation.log`
   - For multi-module: run from project root to compile all modules in reactor order
2. Create `${ARTIFACTS_DIR}/phase-3-compilation-status.md` with:
   - Command executed
   - Exit code (0 = success, 1 = failure)
   - Compilation status table (for multi-module, one row per module):
     | Module | Status | Error Count | Warning Count | Notes |
     |--------|--------|-------------|---------------|-------|
     | core   | FAIL   | 12          | 3             | javax imports |
     | web    | FAIL   | 8           | 1             | servlet API |
     (For single-module repos, use project name as Module. Compilation failures are EXPECTED after version bump.)
   - Notes on compilation errors observed (these will be resolved in Phases 4-8)

**Verification:**
```
test -f docs/spring-boot-migration/verification-logs/phase-3-compilation.log && echo "PASS: compilation log exists" || echo "FAIL: missing compilation log"
test -f ${ARTIFACTS_DIR}/phase-3-compilation-status.md && echo "PASS: compilation status exists" || echo "FAIL: missing compilation status"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-3-compilation-status.md` and `docs/spring-boot-migration/verification-logs/phase-3-compilation.log`

---

### REQ-P3-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 4 (Dependency Compatibility) until completed.**

**Action:** Verify all Phase 3 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**

1. Check mandatory artifacts:
   ```
   test -f ${ARTIFACTS_DIR}/phase-3-compilation-status.md && echo "PASS: phase-3-compilation-status.md" || echo "FAIL: missing phase-3-compilation-status.md"
   test -f docs/spring-boot-migration/verification-logs/phase-3-compilation.log && echo "PASS: phase-3-compilation.log" || echo "FAIL: missing phase-3-compilation.log"
   ```
2. Check applicability file exists (proves all detection steps were run):
   ```
   test -f ${ARTIFACTS_DIR}/phase-3-applicability.md && echo "PASS: phase-3-applicability.md" || echo "FAIL: missing phase-3-applicability.md"
   ```
3. Check conditional artifacts based on applicability results:
   - Check REQ-P3-2.1: `grep -q "REQ-P3-2.1" ${ARTIFACTS_DIR}/phase-3-applicability.md && echo "PASS: REQ-P3-2.1 detection recorded" || echo "FAIL: REQ-P3-2.1 detection missing from applicability file"`
     - If applicability file contains "REQ-P3-2.1" + "APPLICABLE" (not "NOT APPLICABLE"): verify `test -f ${ARTIFACTS_DIR}/phase-3-effective-pom-before-cleanup.xml && echo "PASS" || echo "FAIL: BOM cleanup was APPLICABLE but effective-pom-before-cleanup.xml missing"`
     - If applicability file contains "REQ-P3-2.1" + "NOT APPLICABLE": effective-pom files not required — OK
   - Check REQ-P3-3.1: `grep -q "REQ-P3-3.1" ${ARTIFACTS_DIR}/phase-3-applicability.md && echo "PASS: REQ-P3-3.1 detection recorded" || echo "FAIL: REQ-P3-3.1 detection missing from applicability file"`
     - If applicability file contains "REQ-P3-3.1" + "APPLICABLE" (not "NOT APPLICABLE"): verify `test -f ${ARTIFACTS_DIR}/phase-3-jvm-module-test.log && echo "PASS" || echo "FAIL: JVM module config was APPLICABLE but jvm-module-test.log missing"`
     - If applicability file contains "REQ-P3-3.1" + "NOT APPLICABLE": jvm-module-test.log not required — OK
4. Clean working directory check: `git status --porcelain docs/spring-boot-migration/`
   - If unexpected files appear, STOP and investigate before staging

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 3 at $(date -u)" > ${LOG_DIR}/phase-3-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-3] Complete Phase 3: Core Version Upgrade"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-3-applicability.md` (ALWAYS — contains detection proof for REQ-P3-2.1 and REQ-P3-3.1)
- `${ARTIFACTS_DIR}/phase-3-compilation-status.md`
- `docs/spring-boot-migration/verification-logs/phase-3-compilation.log`
- `${ARTIFACTS_DIR}/phase-3-jvm-module-test.log` (only if REQ-P3-3.1 APPLICABLE per applicability file)
- `${ARTIFACTS_DIR}/phase-3-effective-pom-before-cleanup.xml` (only if REQ-P3-2.1 APPLICABLE per applicability file)
- `${ARTIFACTS_DIR}/phase-3-effective-pom-after-cleanup.xml` (only if REQ-P3-2.1 APPLICABLE per applicability file)

**Decision:** Proceed to Phase 4 (Dependency Compatibility)

# Phase 4: Dependency Compatibility Management
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Upgrading internal libraries without checking bytecode compatibility first
- Silently keeping old versions when no compatible version exists — escalate instead
- Using "latest" or "RELEASE" version qualifiers
- Blindly upgrading to newest version without compatibility check
**Advice:**
- Internal/proprietary libraries are the #1 blocker in enterprise migrations. When no compatible version exists, STOP and escalate to the user immediately — do not attempt workarounds.
- Simple first: let Spring Boot BOM manage common dependencies. Standard dependencies will show errors naturally during build. Deep analysis is ONLY for internal/proprietary dependencies (no BOM coverage).
- Document version choice rationale for every dependency upgrade.

**Goal:** Upgrade incompatible dependencies for JDK 21 and Spring Boot 3.x compatibility.

---

## 4.0 Prerequisites

**REQ-P4-0.1: Verify Phase 3 Completion**
**Action:** Confirm Phase 3 commit exists before proceeding.

**Steps:**
1. Verify Phase 3 commit: `git log --all --grep="[phase-3] Complete Phase 3" --oneline | head -1`
2. If no result, STOP — Phase 3 has not been completed

**Verification:**
```
git log --all --grep="[phase-3] Complete Phase 3" --oneline | head -1
# Expected: commit hash with phase-3 message
```

**Artifact:** N/A — verification only

---

## 4.1 JDK Incompatible Dependencies

**REQ-P4-1.1: Identify and Upgrade JDK 21 Incompatible Dependencies**
**Action:** Replace dependencies incompatible with JDK 21.

**Detection:**
```
# Multi-module note: run from repository root. For module-scoped: mvn -pl <module> -am dependency:tree
mvn -s <settings-file> dependency:tree | \
    grep -E "log4j:log4j:1|junit:junit:4|powermock|javax.xml.bind" | \
    tee incompatible-deps.txt
```

**Steps:**
1. Review the dependency tree output against this common incompatibilities reference:
   | Old Dependency | Issue | Replacement | Min Version |
   |----------------|-------|-------------|-------------|
   | `javax.*` packages | Removed in JDK 11+ | `jakarta.*` | Varies |
   | `log4j:log4j:1.x` | EOL, security | `org.apache.logging.log4j:log4j-core` | 2.20.0+ |
   | `junit:junit:4.x` | Old API | `org.junit.jupiter:junit-jupiter` | 5.9.0+ |
   | `org.powermock:powermock-*` | JDK 17+ incompatible | Remove (use Mockito inline) | N/A |
   | `javax.xml.bind:jaxb-api` | Removed in JDK 11+ | `jakarta.xml.bind:jakarta.xml.bind-api` | 4.0.0+ |
   | `javax.annotation:javax.annotation-api` | Namespace change | `jakarta.annotation:jakarta.annotation-api` | 2.1.1+ |
   | `org.hibernate:hibernate-core:5.x` | JDK 17+ issues | Managed by Spring Boot 3.x BOM | 6.2.0+ |
   | `com.sun.xml.bind:jaxb-impl` | Removed | `org.glassfish.jaxb:jaxb-runtime` | 4.0.0+ |
2. For each incompatible dependency found, replace with the compatible version
3. For multi-module: check all module POMs, not just the root. Dependencies may be declared in child modules.

**Before (Maven):**
```xml
<!-- log4j 1.x -->
<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.17</version>
</dependency>
<!-- JUnit 4 -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
</dependency>
```

**After (Maven):**
```xml
<!-- log4j 2.x (version managed by Spring Boot BOM) -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId>
</dependency>
<!-- JUnit 5 (version managed by Spring Boot BOM) -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
</dependency>
<!-- Optional: JUnit 4 compatibility (vintage engine) -->
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <scope>test</scope>
</dependency>
```

**Before (Gradle):**
```groovy
implementation 'log4j:log4j:1.2.17'
testImplementation 'junit:junit:4.13.2'
```

**After (Gradle):**
```groovy
implementation 'org.apache.logging.log4j:log4j-core'
implementation 'org.apache.logging.log4j:log4j-slf4j2-impl'
testImplementation 'org.junit.jupiter:junit-jupiter'
testImplementation 'org.junit.vintage:junit-vintage-engine' // Optional: JUnit 4 compat
```

**Verification:**
```
# No incompatible dependencies remain
mvn -s <settings-file> dependency:tree | grep -E "log4j:log4j:1|junit:junit:4|powermock" | wc -l
# Expected: 0
```

**Artifact:** `${ARTIFACTS_DIR}/phase-4-dependency-rationale.md` with one section per upgraded dependency containing:
- Reason for upgrade
- Version chosen (and whether managed by Spring Boot BOM)
- Compatibility verified (JDK ${TARGET_JDK_VERSION} compatible: Yes/No)

---

## 4.2 Internal Dependency Management

**REQ-P4-2.1: Upgrade Internal Spring Boot Libraries**
**Action:** Upgrade all internal Spring Boot libraries (e.g., `com.gs.gs-spring-boot*` or `com.goldmansachs.*-spring-boot*` patterns) to versions aligned with Spring Boot ${TARGET_SPRING_BOOT_VERSION}.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P4-2.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
2. Search for internal Spring Boot libraries:
   ```
   mvn dependency:tree | grep -E "gs-spring-boot|goldmansachs.*spring-boot" > ${ARTIFACTS_DIR}/phase-4-internal-spring-boot-libs.txt
   ```
   - Multi-module: run from repository root. For module-scoped: `mvn -pl <module> -am dependency:tree`
3. If `phase-4-internal-spring-boot-libs.txt` has content → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE - Internal Spring Boot libraries found" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If `phase-4-internal-spring-boot-libs.txt` is empty → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE (no internal Spring Boot libraries)" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. For each internal Spring Boot library found, resolve the compatible version using this strategy (try in order):
   - **Exact Match**: `${TARGET_SPRING_BOOT_VERSION}`
   - **Minor Patch**: `${TARGET_SPRING_BOOT_VERSION}.1`, `.2`, etc.
   - **Next Greater**: next available version > `${TARGET_SPRING_BOOT_VERSION}`
   - **ESCALATE**: if no version found, document for escalation (source migration needed)
2. Search for versions:
   ```
   mvn -s <settings-file> dependency:get -Dartifact=com.gs:gs-spring-boot-starter-security:${TARGET_SPRING_BOOT_VERSION}
   mvn -s <settings-file> versions:display-dependency-updates -Dincludes=com.gs:gs-spring-boot*
   ```
3. Upgrade each library to the resolved version
4. If no compatible version found: keep current version temporarily, document in escalation notes
5. For multi-module: check all module POMs for internal Spring Boot library references, not just root

**Before:**
```xml
<dependency>
    <groupId>com.gs</groupId>
    <artifactId>gs-spring-boot-starter-security</artifactId>
    <version>2.7.15</version>
</dependency>
```

**After:**
```xml
<dependency>
    <groupId>com.gs</groupId>
    <artifactId>gs-spring-boot-starter-security</artifactId>
    <version>${TARGET_SPRING_BOOT_VERSION}.1</version> <!-- Aligned with Spring Boot ${TARGET_SPRING_BOOT_VERSION} -->
</dependency>
```

**Verification:**
```
# Verify internal Spring Boot libraries are upgraded (both com.gs and com.goldmansachs patterns)
grep -rE "gs-spring-boot|goldmansachs.*spring-boot" pom.xml */pom.xml 2>/dev/null | grep -v "${TARGET_SPRING_BOOT_VERSION}" && \
    echo "WARNING: Some internal Spring Boot libs may not be aligned" || \
    echo "PASS: Internal Spring Boot libraries aligned"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-4-internal-spring-boot-libs.txt`

**REQ-P4-2.2: Check Other Internal Dependencies**
**Action:** For other internal dependencies matching `com.gs.*` or `com.goldmansachs.*` patterns (excluding `com.gs.gs-spring-boot*` already handled in P4-2.1), check bytecode compatibility and search for compatible versions only if needed.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P4-2.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
2. Search for other internal dependencies:
   ```
   mvn dependency:tree | grep -E "com\.gs\.|com\.goldmansachs\." | grep -v "gs-spring-boot" > ${ARTIFACTS_DIR}/phase-4-internal-other-deps.txt
   ```
   - Multi-module: run from repository root; check all module POMs for internal deps
3. If `phase-4-internal-other-deps.txt` has content → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE - Other internal dependencies found" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If `phase-4-internal-other-deps.txt` is empty → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE (no other internal dependencies)" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. For each internal dependency found (e.g., `com.gs.common-*`, `com.gs.security-*`, `com.goldmansachs.core-*`, `com.goldmansachs.util-*`):
   a. Download JAR: `mvn -s <settings-file> dependency:get -Dartifact=<groupId>:<artifactId>:<version>`
   b. Find JAR: `find ~/.m2/repository -name "<artifactId>-<version>.jar" | head -1`
   c. Check bytecode: `javap -verbose "$JARPATH" | grep "major version"`
2. Apply decision logic:
   | Bytecode Major Version | Java Version | Action | Rationale |
   |------------------------|--------------|--------|-----------|
   | ≤ 65 | Java 21 or earlier | **RETAIN current version** | Already compatible |
   | > 65 | Java 22+ | **Search for compatible version** | May not work |
   | Check fails | Unknown | **RETAIN current version** | Let build/test reveal issues |
3. If bytecode > 65, search for compatible version:
   ```
   mvn -s <settings-file> versions:display-dependency-updates -Dincludes=<groupId>:<artifactId>
   ```
4. If compatible version found, upgrade. If not, keep current version and document in escalation notes.
5. For multi-module: check internal deps across all module POMs, not just root.

**Before:**
```xml
<dependency>
    <groupId>com.gs.security</groupId>
    <artifactId>gs-security-core</artifactId>
    <version>3.1.0</version>
</dependency>
```

**After:**
```xml
<!-- Only if bytecode > 65 and newer version found -->
<dependency>
    <groupId>com.gs.security</groupId>
    <artifactId>gs-security-core</artifactId>
    <version>4.0.2</version> <!-- JDK ${TARGET_JDK_VERSION} compatible -->
</dependency>
```

**Verification:**
```
# Verify bytecode checks completed
test -f ${ARTIFACTS_DIR}/phase-4-internal-other-deps.txt && \
    echo "PASS: Internal dependency analysis completed" || \
    echo "FAIL: Missing internal dependency analysis"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-4-internal-other-deps.txt`

---

**REQ-P4-2.3: Document Internal Dependency Changes**
**Action:** Create summary of internal dependency changes made in Phase 4.

**Steps:**
1. Compile results from REQ-P4-2.1 (Spring Boot libraries) and REQ-P4-2.2 (other internal deps) into a single analysis document
2. For multi-module: document per-module if different modules use different internal library versions
3. Document in `${ARTIFACTS_DIR}/phase-4-internal-deps-analysis.md` (see Artifact section for required contents)

**Verification:**
```
test -f ${ARTIFACTS_DIR}/phase-4-internal-deps-analysis.md && \
    echo "PASS: Internal dependency analysis documented" || \
    { echo "FAIL: Missing internal dependency analysis"; exit 1; }
```

**Artifact:** `${ARTIFACTS_DIR}/phase-4-internal-deps-analysis.md` with required contents:
- Date, target Spring Boot version, target JDK version
- Table 1: Internal Spring Boot libraries upgraded (for multi-module, one row per module+artifact):
  | Module | ArtifactId | Old Version | New Version | Status |
  |--------|-----------|-------------|-------------|--------|
  | core   | gs-spring-boot-starter-security | 2.7.15 | 3.2.1 | Upgraded |
  (For single-module repos, use project name as Module)
- Table 2: Other internal dependencies checked:
  | Module | ArtifactId | Current Version | Bytecode | Action Taken |
  |--------|-----------|-----------------|----------|--------------|
  | web    | gs-common-utils | 1.5.2 | 52 (Java 8) | Retained |
- Table 3: Escalations — no compatible version found:
  | Module | ArtifactId | Current Version | Issue | Next Steps |
  |--------|-----------|-----------------|-------|------------|
- Summary counts (upgraded, retained, escalations)

---

## 4.3 Library-Specific Jakarta/JDK Updates

**REQ-P4-3.1: Migrate QueryDSL to Jakarta Classifier**
**Applicability:** [CONDITIONAL] Only if QueryDSL used
**Action:** Update QueryDSL to use `jakarta` classifier.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P4-3.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
2. Check for QueryDSL: `grep -rl "querydsl" pom.xml */pom.xml 2>/dev/null`
   - For multi-module: check all module POMs for QueryDSL references
3. If grep found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE - QueryDSL detected" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If grep found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE (no QueryDSL dependency)" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Add `<classifier>jakarta</classifier>` to both `querydsl-jpa` and `querydsl-apt` dependencies
2. For multi-module: update in every module POM that declares QueryDSL dependencies

**Before:**
```xml
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
</dependency>
```

**After:**
```xml
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
    <classifier>jakarta</classifier>
</dependency>
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-apt</artifactId>
    <classifier>jakarta</classifier>
    <scope>provided</scope>
</dependency>
```

**Verification:**
```
# Verify jakarta classifier present
mvn dependency:tree | grep "querydsl.*:jakarta:" || \
    { echo "FAIL: QueryDSL must use :jakarta classifier for JPA"; exit 1; }
```

**Artifact:** N/A — POM changes (verified by Verification commands above)

---

**REQ-P4-3.2: Update MapStruct for Jakarta Namespace**
**Applicability:** [CONDITIONAL] Only if MapStruct used
**Action:** Upgrade MapStruct to 1.5.5+ for Jakarta support.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P4-3.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
2. Check for MapStruct: `grep -rl "mapstruct" pom.xml */pom.xml 2>/dev/null`
   - For multi-module: check all module POMs for MapStruct references
3. If grep found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE - MapStruct detected" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If grep found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE (no MapStruct dependency)" >> ${ARTIFACTS_DIR}/phase-4-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Upgrade `mapstruct` and `mapstruct-processor` to 1.5.5.Final or later
2. For multi-module: update in every module POM that declares MapStruct dependencies

**Before:**
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.4.x</version>
</dependency>
```

**After:**
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

**Verification:**
```
mvn dependency:tree | grep "mapstruct" | grep -v "1\.4\." && \
    echo "PASS: MapStruct upgraded" || echo "CHECK: Verify MapStruct version"
```

**Artifact:** N/A — POM changes (verified by Verification commands above)

---

### REQ-P4-SG:Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 5 (Framework Upgrade) until completed.**

**Action:** Verify all Phase 4 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**

1. Check mandatory artifacts:
   ```
   test -f ${ARTIFACTS_DIR}/phase-4-dependency-rationale.md && echo "PASS: phase-4-dependency-rationale.md" || echo "FAIL: missing phase-4-dependency-rationale.md"
   ```
2. Check applicability file exists (proves all detection steps were run):
   ```
   test -f ${ARTIFACTS_DIR}/phase-4-applicability.md && echo "PASS: phase-4-applicability.md" || echo "FAIL: missing phase-4-applicability.md"
   ```
3. Check conditional artifacts based on applicability results:
   - Check REQ-P4-2.1: `grep -q "REQ-P4-2.1" ${ARTIFACTS_DIR}/phase-4-applicability.md && echo "PASS: REQ-P4-2.1 detection recorded" || echo "FAIL: REQ-P4-2.1 detection missing from applicability file"`
     - If applicability file contains "REQ-P4-2.1" + "APPLICABLE" (not "NOT APPLICABLE"): verify `test -f ${ARTIFACTS_DIR}/phase-4-internal-spring-boot-libs.txt && echo "PASS" || echo "FAIL: Internal Spring Boot libs was APPLICABLE but phase-4-internal-spring-boot-libs.txt missing"`
     - If applicability file contains "REQ-P4-2.1" + "NOT APPLICABLE": phase-4-internal-spring-boot-libs.txt not required — OK
   - Check REQ-P4-2.2: `grep -q "REQ-P4-2.2" ${ARTIFACTS_DIR}/phase-4-applicability.md && echo "PASS: REQ-P4-2.2 detection recorded" || echo "FAIL: REQ-P4-2.2 detection missing from applicability file"`
     - If applicability file contains "REQ-P4-2.2" + "APPLICABLE" (not "NOT APPLICABLE"): verify `test -f ${ARTIFACTS_DIR}/phase-4-internal-other-deps.txt && echo "PASS" || echo "FAIL: Other internal deps was APPLICABLE but phase-4-internal-other-deps.txt missing"`
     - If applicability file contains "REQ-P4-2.2" + "NOT APPLICABLE": phase-4-internal-other-deps.txt not required — OK
   - Check REQ-P4-3.1: `grep -q "REQ-P4-3.1" ${ARTIFACTS_DIR}/phase-4-applicability.md && echo "PASS: REQ-P4-3.1 detection recorded" || echo "FAIL: REQ-P4-3.1 detection missing from applicability file"`
   - Check REQ-P4-3.2: `grep -q "REQ-P4-3.2" ${ARTIFACTS_DIR}/phase-4-applicability.md && echo "PASS: REQ-P4-3.2 detection recorded" || echo "FAIL: REQ-P4-3.2 detection missing from applicability file"`
4. Clean working directory check: `git status --porcelain docs/spring-boot-migration/`
   - If unexpected files appear, STOP and investigate before staging

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 4 at $(date -u)" > ${LOG_DIR}/phase-4-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-4] Complete Phase 4: Dependency Compatibility"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-4-applicability.md` (ALWAYS — contains detection proof for REQ-P4-2.1, P4-2.2, P4-3.1, P4-3.2)
- `${ARTIFACTS_DIR}/phase-4-dependency-rationale.md`
- `${ARTIFACTS_DIR}/phase-4-internal-spring-boot-libs.txt` (only if REQ-P4-2.1 APPLICABLE per applicability file)
- `${ARTIFACTS_DIR}/phase-4-internal-other-deps.txt` (only if REQ-P4-2.2 APPLICABLE per applicability file)
- `${ARTIFACTS_DIR}/phase-4-internal-deps-analysis.md` (if applicable)

**Decision:** Proceed to Phase 5 (Framework Upgrade)

# Phase 5: Framework Upgrade
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Using deprecated WebSecurityConfigurerAdapter pattern in new code
- Skipping applicability detection for CONDITIONAL requirements — the detection output IS the proof
**Advice:** Most framework requirements are CONDITIONAL. Run every detection command even if you are confident the feature is not used. The "NOT APPLICABLE" log entry is the required evidence. This phase upgrades Spring Security, Actuator, Spring Batch, auto-config registration, and observability to Spring Boot 3.x / Spring Framework 6.x standards.

---

## 5.0 Prerequisites

**REQ-P5-0.1: Verify Phase 4 Completion**
**Action:** Confirm Phase 4 commit exists before proceeding.

**Steps:**
1. Verify Phase 4 commit: `git log --all --grep="[phase-4] Complete Phase 4" --oneline | head -1`
2. If no result, STOP — Phase 4 has not been completed

**Verification:**
```
git log --all --grep="[phase-4] Complete Phase 4" --oneline | head -1
# Expected: commit hash with phase-4 message
```

**Artifact:** N/A — verification only

---

## 5.1 Security & Web

---

### 5.1.1 Spring Security 6.x Migration

**REQ-P5-1.1: Update Security Configuration (WebSecurityConfigurerAdapter Removed)**
**Applicability:** [CONDITIONAL] Only applies if your project uses Spring Security.
**Action:** Replace deprecated `WebSecurityConfigurerAdapter` with component-based configuration.
**Context:** `WebSecurityConfigurerAdapter` removed in Spring Security 6.0. Replace with `SecurityFilterChain` bean, `UserDetailsService` bean, and lambda DSL.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P5-1.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
2. Run detection command: `grep -rq "spring-boot-starter-security\|spring-security-\|@EnableWebSecurity\|WebSecurityConfigurerAdapter" pom.xml */pom.xml src/ */src/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Remove `extends WebSecurityConfigurerAdapter` from each security config class
2. Replace `configure(HttpSecurity)` with a `@Bean SecurityFilterChain` method
3. Replace `configure(AuthenticationManagerBuilder)` with a `@Bean UserDetailsService` method
4. Change `.authorizeRequests()` → `.authorizeHttpRequests()`
5. Change `.antMatchers()` → `.requestMatchers()`
6. Use lambda DSL instead of `.and()` chaining
7. For multi-module: check each module’s `src/` for security config classes; apply changes per module

**Before:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .permitAll();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
            .withUser("user").password("{noop}password").roles("USER");
    }
}
```

**After:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authz) -> authz
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
            .username("user")
            .password("password")
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**Verification:**
```
# Verify no WebSecurityConfigurerAdapter remains
grep -r "extends WebSecurityConfigurerAdapter" src/ && \
    { echo "FAIL: WebSecurityConfigurerAdapter still used"; exit 1; }

# Verify no deprecated security DSL
grep -r "\.authorizeRequests()" src/ --include="*.java" && \
    { echo "FAIL: Deprecated authorizeRequests() used - should be authorizeHttpRequests()"; exit 1; }

# Verify SecurityFilterChain beans exist
grep -r "SecurityFilterChain" src/ | wc -l

echo "PASS: Spring Security 6.x migration complete"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

**REQ-P5-1.2: Update Security Method Names and Return Types**
**Action:** Update renamed/changed methods in Spring Security 6.x.
**Context:** Spring Security 6.x renames several matcher and authorization methods:

| Old Method (5.x) | New Method (6.x) | Notes |
|------------------|------------------|-------|
| `.antMatchers()` | `.requestMatchers()` | Renamed for clarity |
| `.mvcMatchers()` | `.requestMatchers()` | Merged into requestMatchers |
| `.regexMatchers()` | `.requestMatchers(RegexRequestMatcher)` | Use explicit matcher |
| `.authorizeRequests()` | `.authorizeHttpRequests()` | More explicit name |
| `.access(String)` | `.access(AuthorizationManager)` | Type-safe authorization |
| `.permitAll()` | `.permitAll()` | No change |
| `.authenticated()` | `.authenticated()` | No change |

**Steps:**
1. Search for deprecated matcher methods across all source files: `grep -rn "\.antMatchers\|\.mvcMatchers\|\.regexMatchers\|\.authorizeRequests" src/ --include="*.java"`
2. Replace each deprecated method with the 6.x equivalent per the table above
3. For multi-module: search each module’s `src/` directory for deprecated methods

**Before:**
```java
http.authorizeRequests()
    .antMatchers("/admin/**").hasRole("ADMIN")
    .mvcMatchers("/api/**").hasAuthority("API_USER")
    .regexMatchers("/api/v[0-9]+/.*").permitAll()
    .anyRequest().authenticated();
```

**After:**
```java
http.authorizeHttpRequests((authz) -> authz
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/**").hasAuthority("API_USER")
    .requestMatchers(RegexRequestMatcher.regexMatcher("/api/v[0-9]+/.*")).permitAll()
    .anyRequest().authenticated()
);
```

**Verification:**
```
# Verify no deprecated matcher methods remain
grep -r "\.antMatchers\|\.mvcMatchers\|\.regexMatchers" src/ --include="*.java" && \
    { echo "FAIL: Deprecated matcher methods still used"; exit 1; }

# Verify no deprecated authorizeRequests remains
grep -r "\.authorizeRequests()" src/ --include="*.java" && \
    { echo "FAIL: Deprecated authorizeRequests() used"; exit 1; }

echo "PASS: Security method names updated"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

### 5.1.2 Spring Boot Actuator Changes

**REQ-P5-2.1: Update Actuator Endpoint Paths**
**Applicability:** [CONDITIONAL] Only applies if your project uses Spring Boot Actuator.
**Action:** Update renamed/removed actuator endpoints for Spring Boot 3.x.
**Context:** Default actuator path `/actuator/*` is unchanged, but several endpoints are renamed/removed:

| Old Endpoint (2.x) | New Endpoint (3.x) | Status |
|--------------------|-------------------|--------|
| `/actuator/httptrace` | `/actuator/httpexchanges` | Renamed |
| `/actuator/jolokia` | N/A | Removed (add dependency manually) |
| `/actuator/info` | `/actuator/info` | No change |
| `/actuator/health` | `/actuator/health` | No change |
| `/actuator/metrics` | `/actuator/metrics` | No change |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P5-2.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
2. Run detection command: `grep -rq "spring-boot-starter-actuator" pom.xml */pom.xml 2>/dev/null || grep -rq "management.endpoints\|@Endpoint\|/actuator/" src/ */src/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Replace `httptrace` with `httpexchanges` in configuration files (application.yml / application.properties)
2. Replace `HttpTraceRepository` with `HttpExchangeRepository` in Java code
3. Add `management.httpexchanges.recording.enabled: true` if httpexchanges is needed
4. For multi-module: check each module’s `src/main/resources/` for actuator configuration

**Before:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,httptrace"
```
```java
@Bean
public HttpTraceRepository httpTraceRepository() {
    return new InMemoryHttpTraceRepository();
}
```

**After:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,httpexchanges"
  httpexchanges:
    recording:
      enabled: true
```
```java
@Bean
public HttpExchangeRepository httpExchangeRepository() {
    return new InMemoryHttpExchangeRepository();
}
```

**Verification:**
```
# Verify no httptrace references remain
grep -r "httptrace\|HttpTraceRepository" src/ --include="*.java" && \
    { echo "FAIL: httptrace references still present"; exit 1; }

# Verify httpexchanges is configured if actuator is used
grep -rq "httpexchanges" src/main/resources/ && echo "PASS: httpexchanges configured" || echo "INFO: No httpexchanges config (may not be needed)"

echo "PASS: Actuator endpoint migration complete"
```

**Artifact:** N/A — code/config changes (verified by Verification commands above)

---

## 5.2 Data & Batch

---

### 5.2.1 Spring Batch 5.0 Migration

**REQ-P5-3.1: Migrate Spring Batch Configuration (If Applicable)**
**Applicability:** [CONDITIONAL] Only applies if your project uses Spring Batch.
**Action:** Update Spring Batch job/step configuration for Spring Batch 5.0 breaking changes.
**Context:** `JobBuilderFactory` and `StepBuilderFactory` are removed in Spring Batch 5.0 (bundled with Spring Boot 3.x). Use `JobBuilder` and `StepBuilder` directly with `JobRepository`. Also, `@EnableBatchProcessing` is now optional (batch infrastructure auto-configured by default).

| Old API (4.x) | New API (5.0) | Notes |
|---------------|---------------|-------|
| `JobBuilderFactory` | `JobBuilder` | Direct instantiation with JobRepository |
| `StepBuilderFactory` | `StepBuilder` | Direct instantiation with JobRepository |
| `chunk(int)` | `chunk(int, PlatformTransactionManager)` | Explicit transaction manager |
| `@EnableBatchProcessing` required | Optional with Boot 3.x auto-config | Auto-config registers beans |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P5-3.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
2. Run detection command: `grep -rq "spring-boot-starter-batch\|JobBuilderFactory\|StepBuilderFactory" pom.xml */pom.xml src/ */src/ 2>/dev/null || grep -rq "import org.springframework.batch" src/ */src/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Remove `@Autowired JobBuilderFactory` → use `new JobBuilder(name, jobRepository)`
2. Remove `@Autowired StepBuilderFactory` → use `new StepBuilder(name, jobRepository)`
3. Change `chunk(int)` → `chunk(int, transactionManager)` (explicit transaction manager required)
4. Remove `@EnableBatchProcessing` UNLESS custom BatchConfigurer, custom transaction manager, or multiple data sources are needed. If kept, document justification in commit message.
5. For multi-module: check each module’s `src/` for batch configuration classes; apply changes per module

**Before:**
```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job importUserJob(Step step1) {
        return jobBuilderFactory.get("importUserJob")
            .incrementer(new RunIdIncrementer())
            .flow(step1)
            .end()
            .build();
    }

    @Bean
    public Step step1(ItemReader<User> reader,
                      ItemProcessor<User, User> processor,
                      ItemWriter<User> writer) {
        return stepBuilderFactory.get("step1")
            .<User, User>chunk(10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
}
```

**After:**
```java
@Configuration
public class BatchConfig {

    @Bean
    public Job importUserJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("importUserJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .flow(step1)
            .end()
            .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      ItemReader<User> reader,
                      ItemProcessor<User, User> processor,
                      ItemWriter<User> writer) {
        return new StepBuilder("step1", jobRepository)
            .<User, User>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
}
```

**Verification:**
1. Verify no deprecated batch factories remain: `grep -r "JobBuilderFactory\|StepBuilderFactory" src/`
   - If grep returns results → FAIL: Deprecated batch factories still used. Fix before proceeding.
   - If grep returns empty → PASS.
2. Check for @EnableBatchProcessing: `grep -r "@EnableBatchProcessing" src/`
   - If found → WARNING: Ensure commit message justifies retention.
   - If not found → PASS (auto-configuration is enabled by default in Spring Boot 3.x).
3. Run batch tests: `mvn -s maven-settings.xml test -Dtest="*Batch*,*Job*,*Step*"`

**Artifact:** N/A — code changes (verified by Verification commands above)

---

## 5.3 Infrastructure

---

### 5.3.1 Auto-configuration Registration Migration

**REQ-P5-4.1: Migrate spring.factories to AutoConfiguration.imports**
**Action:** Update auto-configuration registration from deprecated spring.factories to new AutoConfiguration.imports file.
**Context:** Spring Boot 3.0 deprecates `META-INF/spring.factories` for auto-configuration registration. Library authors must migrate to the new format.
**Applicability:** [CONDITIONAL] Only applies if your project provides custom Spring Boot auto-configurations (starter libraries, internal frameworks).

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P5-4.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
2. Run detection command: `find . -path "*/META-INF/spring.factories" -exec grep -l "EnableAutoConfiguration" {} \; 2>/dev/null | grep -q .`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Locate existing `src/main/resources/META-INF/spring.factories` (for multi-module: check each module’s `src/main/resources/META-INF/`)
2. Find entries under key: `org.springframework.boot.autoconfigure.EnableAutoConfiguration`
3. Create new file: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
4. Copy fully-qualified class names (one per line, no key=value format)
5. Remove auto-configuration entries from spring.factories (keep other entries like ApplicationListener)
6. For multi-module: repeat steps 1–5 for each module that has spring.factories

**Before:**
```properties
# spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.config.MyAutoConfiguration,\
com.example.config.AnotherAutoConfiguration

# Application Listeners (keep these)
org.springframework.context.ApplicationListener=\
com.example.listener.MyApplicationListener
```

**After:**
```
# NEW FILE: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.config.MyAutoConfiguration
com.example.config.AnotherAutoConfiguration
```
```properties
# UPDATED: spring.factories (non-auto-config entries remain)
org.springframework.context.ApplicationListener=\
com.example.listener.MyApplicationListener
```

**Verification:**
```
# Verify new file exists (for multi-module: check each module)
find . -path "*/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports" | grep -q . && \
    echo "PASS: AutoConfiguration.imports file exists" || \
    echo "INFO: No AutoConfiguration.imports (may not be needed)"

# Verify no auto-config entries remain in spring.factories
find . -path "*/META-INF/spring.factories" -exec grep -l "EnableAutoConfiguration" {} \; 2>/dev/null && \
    { echo "FAIL: EnableAutoConfiguration still in spring.factories"; exit 1; } || \
    echo "PASS: No auto-config entries in spring.factories"

# Test auto-configuration still loads
mvn -s maven-settings.xml clean test
```

**Artifact:** N/A — file changes (verified by Verification commands above)

---

### 5.3.2 Micrometer Observation API / Observability

**REQ-P5-5.1: Migrate to Micrometer Observation API**
**Applicability:** [CONDITIONAL] Applies if your project uses custom metrics, distributed tracing, or monitoring.
**Action:** Update observability/metrics configuration for Spring Boot 3.x Micrometer Observation API.
**Context:** Spring Boot 3.x replaces custom metrics instrumentation with the Micrometer Observation API. Spring Cloud Sleuth is deprecated and replaced by Micrometer Tracing.

| Old (Spring Boot 2.x) | New (Spring Boot 3.x) | Notes |
|------------------------|------------------------|-------|
| Spring Cloud Sleuth | Micrometer Tracing | Sleuth deprecated |
| `spring-cloud-starter-sleuth` | `micrometer-tracing-bridge-brave` or `micrometer-tracing-bridge-otel` | Choose Brave or OpenTelemetry |
| `spring-cloud-sleuth-zipkin` | `io.zipkin.reporter2:zipkin-reporter-brave` | Direct Zipkin reporter |
| Custom `MeterBinder` | `ObservationHandler` | New observation API |
| `management.metrics.*` properties | `management.observations.*` properties | Property namespace changes |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P5-5.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
2. Run detection command: `grep -rq "MeterRegistry\|micrometer\|@Timed\|@Counted" src/ */src/ 2>/dev/null || grep -rq "spring-cloud-sleuth\|brave\|zipkin" pom.xml */pom.xml 2>/dev/null || grep -rq "management.metrics\|management.tracing" src/main/resources/ */src/main/resources/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-5-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Replace Sleuth dependencies with Micrometer Tracing bridge (Brave or OpenTelemetry) in pom.xml
2. Update configuration properties: `spring.sleuth.*` → `management.tracing.*`, `spring.zipkin.*` → `management.zipkin.tracing.*`
3. Replace custom `MeterRegistry` / `Timer.Sample` patterns with `ObservationRegistry` / `Observation` API
4. For multi-module: check each module’s pom.xml and `src/main/resources/` for Sleuth/metrics references

**Before:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```
```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0
  zipkin:
    base-url: http://localhost:9411
```
```java
@Service
public class OrderService {
    private final MeterRegistry meterRegistry;

    public Order createOrder(OrderRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Order order = processOrder(request);
            sample.stop(meterRegistry.timer("order.create", "status", "success"));
            return order;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("order.create", "status", "error"));
            throw e;
        }
    }
}
```

**After:**
```xml
<!-- Option A: Brave bridge (if using Brave/Zipkin) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>

<!-- Option B: OpenTelemetry bridge (if using OTel) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```
```java
@Service
public class OrderService {
    private final ObservationRegistry observationRegistry;

    public Order createOrder(OrderRequest request) {
        return Observation.createNotStarted("order.create", observationRegistry)
            .observe(() -> processOrder(request));
    }
}
```

**Verification:**
```
# Verify no Sleuth dependencies remain
grep -r "spring-cloud-starter-sleuth\|spring-cloud-sleuth" pom.xml */pom.xml 2>/dev/null && \
    { echo "FAIL: Sleuth dependencies still present"; exit 1; } || \
    echo "PASS: No Sleuth dependencies"

# Verify Micrometer tracing is configured
mvn -s maven-settings.xml dependency:tree | grep "micrometer-tracing"

# Check actuator metrics endpoint
curl http://localhost:8080/actuator/metrics
```

**Artifact:** N/A — code/config changes (verified by Verification commands above)

---

### REQ-P5-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 6 (Namespace Migration) until completed.**

**Action:** Verify all Phase 5 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**
```
# Artifact existence checks
test -f ${ARTIFACTS_DIR}/phase-5-applicability.md && echo "PASS: phase-5-applicability.md" || echo "FAIL: missing phase-5-applicability.md"
# Clean working directory check
test -z "$(git status --porcelain docs/spring-boot-migration/)" || echo "WARNING: Unstaged changes in migration directory"
```

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 5 at $(date -u)" > ${LOG_DIR}/phase-5-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-5] Complete Phase 5: Framework Upgrade"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-5-applicability.md` (contains detection proof for all CONDITIONAL requirements — APPLICABLE and NOT APPLICABLE entries)

**Decision:** Proceed to Phase 6 (Namespace Migration)

# Phase 6: Namespace Migration
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Skipping OpenRewrite attempt and going straight to manual sed/find-replace
- Proceeding to REQ-P6-1.2 without phase-6-openrewrite-attempt.log existing
**Advice:** OpenRewrite will likely fail in enterprise environments (missing plugin, network restrictions). Run it anyway — the attempt log IS the deliverable, not the success. The failure log proves the tool was tried before falling back to manual. If this project exposes REST APIs, note that namespace changes may affect serialization contracts (e.g., `@JsonTypeInfo(use = Id.CLASS)`, exception class names in responses) — this will be verified in REQ-P9-5.4.

**Multi-Module Note:** For multi-module projects, add the OpenRewrite plugin to the **parent POM** and run from the project root. OpenRewrite will process all modules in the reactor. Do NOT add the plugin to individual child modules.

**Context:** Migrate all `javax.*` imports to `jakarta.*` namespace. Namespace migration tools handle bulk changes efficiently. Phase 6 MUST follow this exact sequence — no step may be skipped:
1. REQ-P6-0.1: Verify Phase 5 complete
2. REQ-P6-1.1: Attempt OpenRewrite (automated). If succeeds → skip to step 4. If fails → proceed to step 3.
3. REQ-P6-1.2: Manual namespace migration (ONLY if REQ-P6-1.1 failed; `phase-6-openrewrite-attempt.log` MUST exist)
4. REQ-P6-2.1/2.2: Configuration file updates (persistence.xml, web.xml)
5. REQ-P6-SG: Stop gate

---

## 6.0 Prerequisites

**REQ-P6-0.1: Verify Phase 5 Completion**
**Action:** Confirm Phase 5 commit exists before proceeding.

**Steps:**
1. Verify Phase 5 commit: `git log --all --grep="[phase-5] Complete Phase 5" --oneline | head -1`
2. If no result, STOP — Phase 5 has not been completed

**Verification:**
```
git log --all --grep="[phase-5] Complete Phase 5" --oneline | head -1
# Expected: commit hash with phase-5 message
```

**Artifact:** N/A — verification only

---

## 6.1 Automated Namespace Migration

**REQ-P6-1.1: Attempt OpenRewrite Namespace Migration**
**Action:** Attempt automated namespace migration using OpenRewrite. The attempt log is the deliverable, not the success. If OpenRewrite fails, capture the failure and proceed to REQ-P6-1.2 (manual migration).

**Detection:**
1. For multi-module: add OpenRewrite plugin to **parent POM** only, run from project root.
2. Run dry run and capture output:
   ```
   mvn -s maven-settings.xml rewrite:dryRun \
       -Drewrite.activeRecipes=org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta \
       2>&1 | tee ${ARTIFACTS_DIR}/phase-6-openrewrite-attempt.log
   ```
3. Check result: `grep -q "BUILD SUCCESS" ${ARTIFACTS_DIR}/phase-6-openrewrite-attempt.log`
4. If grep found "BUILD SUCCESS" → OpenRewrite available. Proceed with automated migration in **Steps** below.
5. If grep did NOT find "BUILD SUCCESS" → OpenRewrite unavailable. Run:
   ```
   echo "OpenRewrite unavailable due to:" > ${ARTIFACTS_DIR}/phase-6-openrewrite-unavailable.log
   grep -i "error\|exception\|fail" ${ARTIFACTS_DIR}/phase-6-openrewrite-attempt.log >> ${ARTIFACTS_DIR}/phase-6-openrewrite-unavailable.log
   ```
   Then proceed to REQ-P6-1.2 (manual migration).

**Steps:**
1. Add OpenRewrite plugin to pom.xml temporarily (for multi-module: add to **parent POM** only — OpenRewrite processes all modules in the reactor)
2. Run dry run: `mvn -s maven-settings.xml rewrite:dryRun -Drewrite.activeRecipes=org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta`
3. If dry run succeeds, apply changes: `mvn -s maven-settings.xml rewrite:run -Drewrite.activeRecipes=org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta`
4. For Gradle: use `org.openrewrite.rewrite` plugin v6.5.0, run `./gradlew rewriteRun`
5. If OpenRewrite fails (network, plugin unavailable), proceed to REQ-P6-1.2

**Verification:**
```
# Verify attempt log was created
test -f ${ARTIFACTS_DIR}/phase-6-openrewrite-attempt.log && echo "PASS: OpenRewrite attempt log exists" || \
    { echo "FAIL: OpenRewrite attempt log missing — run the attempt first"; exit 1; }

# Count remaining javax.* imports (should only be JDK core packages)
grep -r "import javax\." src/ --include="*.java" | grep -v "javax.crypto\|javax.net\|javax.sql\|javax.security.auth" | wc -l
# Should be 0 or very low (only false positives)
```

**Artifact:** `${ARTIFACTS_DIR}/phase-6-openrewrite-attempt.log` (and `phase-6-openrewrite-unavailable.log` if OpenRewrite failed)

---

**REQ-P6-1.2: Manual Namespace Migration (If OpenRewrite Not Available)**
**Applicability:** [CONDITIONAL] Only if REQ-P6-1.1 OpenRewrite failed (`phase-6-openrewrite-unavailable.log` MUST exist).
**Action:** Use find-replace to migrate javax.* imports to jakarta.* namespace.
**Context:** Apply regex-based find-replace for each javax namespace that migrates to jakarta. Do NOT replace JDK core packages (`javax.crypto`, `javax.net`, `javax.sql`, `javax.security.auth`, `javax.security.cert`, `javax.naming`, `javax.management`, `javax.swing`, `javax.imageio`, `javax.xml.parsers`, `javax.xml.transform`).

**Detection:**
```
# Verify OpenRewrite was attempted first
test -f ${ARTIFACTS_DIR}/phase-6-openrewrite-unavailable.log && \
    echo "APPLICABLE: OpenRewrite failed — manual migration required" || \
    { echo "NOT APPLICABLE: OpenRewrite succeeded or not attempted — go back to REQ-P6-1.1"; exit 1; }
```

**Steps:**
1. For each module (or from root for single-module), apply regex replacements per table below
2. Use IDE Find → Replace in Files (Ctrl+Shift+R, Regex mode) or equivalent CLI commands
3. For multi-module: run replacements from project root to cover all modules

| Find (Regex) | Replace | Notes |
|--------------|---------|-------|
| `import javax\.servlet\.` | `import jakarta.servlet.` | Servlets |
| `import javax\.persistence\.` | `import jakarta.persistence.` | JPA |
| `import javax\.validation\.` | `import jakarta.validation.` | Validation |
| `import javax\.transaction\.` | `import jakarta.transaction.` | Transactions |
| `import javax\.annotation\.` | `import jakarta.annotation.` | Annotations |
| `import javax\.ws\.rs\.` | `import jakarta.ws.rs.` | JAX-RS |
| `import javax\.xml\.bind\.` | `import jakarta.xml.bind.` | JAXB |
| `import javax\.inject\.` | `import jakarta.inject.` | CDI |
| `import javax\.jms\.` | `import jakarta.jms.` | JMS |
| `import javax\.mail\.` | `import jakarta.mail.` | Email |
| `import javax\.websocket\.` | `import jakarta.websocket.` | WebSocket |
| `import javax\.el\.` | `import jakarta.el.` | Expression Language |
| `import javax\.faces\.` | `import jakarta.faces.` | JSF |
| `import javax\.json\.` | `import jakarta.json.` | JSON-P / JSON-B |

**Verification:**
```
# Verify replacements (sample spot check)
grep -r "import jakarta\." src/ --include="*.java" | head -20

# Verify no wrong replacements (JDK packages should still be javax)
grep -r "import jakarta\.sql\|import jakarta\.crypto" src/ --include="*.java" && \
    { echo "FAIL: JDK core packages incorrectly replaced"; exit 1; } || \
    echo "PASS: No incorrect replacements"

# Count remaining javax.* that should have been replaced
grep -r "import javax\." src/ --include="*.java" | grep -v "javax.crypto\|javax.net\|javax.sql\|javax.security.auth\|javax.security.cert\|javax.naming\|javax.management\|javax.swing\|javax.imageio\|javax.xml.parsers\|javax.xml.transform" | wc -l
# Expected: 0
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

## 6.2 Configuration File Updates

**REQ-P6-2.1: Update persistence.xml (JPA Configuration)**
**Applicability:** [CONDITIONAL] Only applies if `persistence.xml` exists.
**Action:** Update JPA provider class names and schema versions from javax to jakarta namespace.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P6-2.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-6-applicability.md
   ```
2. Run detection command: `find . -name "persistence.xml" -path "*/META-INF/*" 2>/dev/null | grep -q .`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-6-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-6-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Find all `persistence.xml` files (for multi-module: check each module’s `src/main/resources/META-INF/`)
2. Update XML namespace from `http://xmlns.jcp.org/xml/ns/persistence` to `https://jakarta.ee/xml/ns/persistence`
3. Update schema version from `persistence_2_2.xsd` to `persistence_3_0.xsd` and version from `2.2` to `3.0`

**Before:**
```xml
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
         http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd"
         version="2.2">
```

**After:**
```xml
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
         https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
         version="3.0">
```

**Verification:**
```
# Verify jakarta namespace in all persistence.xml files
find . -name "persistence.xml" -path "*/META-INF/*" -exec grep -l "jakarta.ee" {} \; | grep -q . && \
    echo "PASS: persistence.xml updated to jakarta.ee" || \
    echo "INFO: No persistence.xml found or already updated"
```

**Artifact:** N/A — config file changes (verified by Verification commands above)

---

**REQ-P6-2.2: Update web.xml (Servlet Configuration)**
**Applicability:** [CONDITIONAL] Only applies if `web.xml` exists. Most Spring Boot apps don’t use web.xml (embedded container).
**Action:** Update servlet schema versions from javaee to jakartaee namespace.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P6-2.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-6-applicability.md
   ```
2. Run detection command: `find . -name "web.xml" -path "*/WEB-INF/*" 2>/dev/null | grep -q .`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-6-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-6-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Find all `web.xml` files (for multi-module: check each module’s `src/main/webapp/WEB-INF/`)
2. Update XML namespace from `http://xmlns.jcp.org/xml/ns/javaee` to `https://jakarta.ee/xml/ns/jakartaee`
3. Update schema version from `web-app_4_0.xsd` to `web-app_5_0.xsd` and version from `4.0` to `5.0`

**Before:**
```xml
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
     xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
     http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
     version="4.0">
```

**After:**
```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
     xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
     https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
     version="5.0">
```

**Verification:**
```
# Verify jakarta namespace in all web.xml files
find . -name "web.xml" -path "*/WEB-INF/*" -exec grep -l "jakarta.ee" {} \; | grep -q . && \
    echo "PASS: web.xml updated to jakartaee" || \
    echo "INFO: No web.xml found or already updated"
```

**Artifact:** N/A — config file changes (verified by Verification commands above)

---

**REQ-P6-2.3: Create Namespace Migration Summary**
**Action:** Document the namespace migration method, changes applied, and verification results.

**Steps:**
1. Create `${ARTIFACTS_DIR}/phase-6-namespace-migration.md` with the template below
2. Fill in migration method (OpenRewrite or Manual), file counts, and verification results

**Artifact:** `${ARTIFACTS_DIR}/phase-6-namespace-migration.md` with required contents:
- Migration method used (OpenRewrite / Manual) and reason
- Changes applied table:
  | Package/Import | Before (javax.*) | After (jakarta.*) | Files Changed |
  |---------------|-------------------|-------------------|---------------|
  | servlet | javax.servlet | jakarta.servlet | [count] |
  | persistence | javax.persistence | jakarta.persistence | [count] |
- Verification results: total files modified, remaining javax.* count (expected 0 in src/)

**Verification:**
```
test -f ${ARTIFACTS_DIR}/phase-6-namespace-migration.md && echo "PASS: namespace migration summary exists" || \
    { echo "FAIL: missing phase-6-namespace-migration.md"; exit 1; }
grep -r "javax\." src/ --include="*.java" | grep -v "javax.xml\|javax.crypto\|javax.net\|javax.sql\|javax.security.auth" | wc -l
# Expected: 0
```

---

### REQ-P6-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 7 (Configuration Migration) until completed.**

**Action:** Verify all Phase 6 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**
```
# Artifact existence checks
test -f ${ARTIFACTS_DIR}/phase-6-openrewrite-attempt.log && echo "PASS: phase-6-openrewrite-attempt.log" || echo "FAIL: missing phase-6-openrewrite-attempt.log"
test -f ${ARTIFACTS_DIR}/phase-6-applicability.md && echo "PASS: phase-6-applicability.md" || echo "FAIL: missing phase-6-applicability.md"
test -f ${ARTIFACTS_DIR}/phase-6-namespace-migration.md && echo "PASS: phase-6-namespace-migration.md" || echo "FAIL: missing phase-6-namespace-migration.md"

# Content validation
grep -qE "BUILD SUCCESS|BUILD FAILURE|ERROR|rewrite" ${ARTIFACTS_DIR}/phase-6-openrewrite-attempt.log && echo "PASS: OpenRewrite log has content" || echo "FAIL: OpenRewrite log may be placeholder"
# Clean working directory check
test -z "$(git status --porcelain docs/spring-boot-migration/)" || echo "WARNING: Unstaged changes in migration directory"
```

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 6 at $(date -u)" > ${LOG_DIR}/phase-6-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-6] Complete Phase 6: Namespace Migration"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-6-openrewrite-attempt.log`
- `${ARTIFACTS_DIR}/phase-6-openrewrite-unavailable.log` (if OpenRewrite failed)
- `${ARTIFACTS_DIR}/phase-6-applicability.md`
- `${ARTIFACTS_DIR}/phase-6-namespace-migration.md`

**Decision:** Proceed to Phase 7 (Configuration Migration)

# Phase 7: Configuration Migration
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Manually renaming properties without first running Properties Migrator (REQ-P7-1.1) — the migrator output is the authoritative source
- Applying property changes without first running Properties Migrator (REQ-P7-1.1)
- Using grep-based detection followed by manual migration instead of Properties Migrator output
- Using sed/awk/IDE find-replace for bulk property replacements without migrator validation
**Advice:** The Properties Migrator tool output is the ONLY authoritative source for property renames. Do not rely on documentation tables or prior knowledge. The tool detects project-specific properties that tables miss. For multi-module projects, configuration files may exist in multiple modules — search from the project root with `find . -name "application*.properties" -o -name "application*.yml" -o -name "application*.yaml" | sort` and apply property migrations to ALL discovered configuration files, not just the root module. Common property renames (e.g., `spring.resources.*` → `spring.web.resources.*`, `spring.redis.*` → `spring.data.redis.*`, `server.max-http-header-size` → `server.max-http-request-header-size`) and removals (e.g., `spring.jpa.hibernate.use-new-id-generator-mappings`, `spring.config.use-legacy-processing`) are listed in the Spring Boot 3.x migration guide — but always defer to Properties Migrator output as the authoritative source.

---

## 7.0 Prerequisites

**REQ-P7-0.1: Verify Phase 6 Completion**
**Action:** Confirm Phase 6 commit exists before proceeding.

**Steps:**
1. Verify Phase 6 commit: `git log --all --grep="[phase-6] Complete Phase 6" --oneline | head -1`
2. If no result, STOP — Phase 6 has not been completed

**Verification:**
```
git log --all --grep="[phase-6] Complete Phase 6" --oneline | head -1
# Expected: commit hash with phase-6 message
```

**Artifact:** N/A — verification only

---

## 7.1 Property Renaming (Tool-First Approach)

**REQ-P7-1.1: Add Spring Boot Properties Migrator Tool [REQUIRED]**
**Action:** Add official Spring Boot properties migrator to automatically detect and report ALL deprecated/renamed properties. This is the SOURCE OF TRUTH for property migration and MUST be completed BEFORE any manual property changes.
**Context:** The Properties Migrator is a runtime dependency that logs warnings for deprecated/renamed properties on application startup. WARN messages indicate properties needing migration; INFO messages indicate already-renamed properties; no messages means all properties are up-to-date. Remove this dependency after completing Phase 9.

**Steps:**
1. Add the Properties Migrator dependency to pom.xml (for multi-module: add to the parent POM or each module that has configuration files):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>
```
   For Gradle: `runtimeOnly 'org.springframework.boot:spring-boot-properties-migrator'`
2. Verify the dependency was actually added:
   `mvn -s <settings-file> dependency:tree | grep properties-migrator`
   If the dependency does NOT appear in the output, fix before proceeding — all subsequent steps depend on this dependency being present at runtime.
3. Run the application to collect warnings:
   `mvn -s maven-settings.xml spring-boot:run > ${ARTIFACTS_DIR}/phase-7-startup.log 2>&1 &`
   Wait ~30 seconds, then stop: `pkill -f "spring-boot:run"`
4. Verify the tool actually ran (not just added):
   `grep "PropertyMigration" ${ARTIFACTS_DIR}/phase-7-startup.log`
   If no output, the tool did not run — investigate before proceeding.
5. Extract migration warnings:
   `grep -i "deprecated\|property.*has been renamed" ${ARTIFACTS_DIR}/phase-7-startup.log > ${ARTIFACTS_DIR}/phase-7-migration-warnings.log`
6. Review warnings: `cat ${ARTIFACTS_DIR}/phase-7-migration-warnings.log`
7. For multi-module: repeat steps 3-6 for each module that can be started independently, or run from parent if all modules share a single application entry point

**Verification:**
```
# Verify dependency added
mvn -s maven-settings.xml dependency:tree | grep "spring-boot-properties-migrator"

# Verify tool RAN
grep "PropertyMigration" ${ARTIFACTS_DIR}/phase-7-startup.log || exit 1

# Verify warnings captured
[ -f "${ARTIFACTS_DIR}/phase-7-migration-warnings.log" ] || \
    { echo "FAIL: No migration warnings log created"; exit 1; }
```

**Artifact:** `${ARTIFACTS_DIR}/phase-7-startup.log` and `${ARTIFACTS_DIR}/phase-7-migration-warnings.log`

---

**REQ-P7-1.2: Apply Property Changes Based on Migrator Output [REQUIRED]**
**Action:** Update properties identified by Properties Migrator tool in REQ-P7-1.1. Use migrator warnings as the authoritative source for all changes.
**Context:** Apply changes per configuration file. For multi-module projects, check all modules' configuration files (application.yml / application.properties in each module's `src/main/resources/`).

**Detection:**
```
# Verify REQ-P7-1.1 completed
[ -f "${ARTIFACTS_DIR}/phase-7-migration-warnings.log" ] || \
    { echo "ERROR: Run REQ-P7-1.1 first"; exit 1; }

# Display warnings to address
cat ${ARTIFACTS_DIR}/phase-7-migration-warnings.log
```

**Steps:**
1. Review the migration warnings log from REQ-P7-1.1
2. For each warning found:
   a. Identify the configuration file containing the deprecated property
   b. Update the property to the new name/namespace
   c. Verify syntax (YAML indentation, properties format)
3. For multi-module: apply changes to ALL configuration files discovered by the multi-module config discovery command in phase-level Advice

**Before:**
```yaml
# application.yml (Spring Boot 2.x) - based on migrator warnings
spring:
  mvc:
    throw-exception-if-no-handler-found: true
  resources:
    add-mappings: true
    static-locations: classpath:/static/,classpath:/public/
  redis:
    host: localhost
    port: 6379

server:
  max-http-header-size: 8KB
```

**After:**
```yaml
# application.yml (Spring Boot 3.x) - corrected per migrator output
spring:
  mvc:
    problemdetails:
      enabled: true
  web:
    resources:
      add-mappings: true
      static-locations: classpath:/static/,classpath:/public/
  data:
    redis:
      host: localhost
      port: 6379

server:
  max-http-request-header-size: 8KB
```

**Verification:**
```
# Re-run application to verify warnings are gone
mvn -s maven-settings.xml spring-boot:run > ${ARTIFACTS_DIR}/phase-7-recheck.log 2>&1 &
sleep 30
pkill -f "spring-boot:run"

# Check that specific warnings no longer appear
grep "spring.resources\|spring.redis\|max-http-header-size" ${ARTIFACTS_DIR}/phase-7-recheck.log
# Should be empty for fixed properties
```

**Artifact:** N/A — config file changes (verified by Verification commands above)

---

## 7.2 Logging Configuration

**REQ-P7-2.1: Update Logback Configuration (If Custom)**
**Applicability:** [CONDITIONAL] Only applies if your project has custom Logback configuration. Most apps use Spring Boot's default logging — only update if custom `logback-spring.xml` exists.
**Action:** Update Logback configuration for Spring Boot 3.x defaults (updated log pattern with correlation ID, console appender encoding changes, async appender configuration changes).
**Context:** If using Log4j2 instead of Logback, ensure `log4j-core` version 2.20.0+ (managed by Spring Boot BOM) and update appender configurations if using deprecated APIs.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P7-2.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
2. Run detection command: `find . -name "logback.xml" -o -name "logback-spring.xml" -o -name "logback-*.xml" | grep -q .`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Locate all custom Logback configuration files (for multi-module: check each module's `src/main/resources/`)
2. Review and update log patterns, appender configurations, and encoding settings for Spring Boot 3.x compatibility
3. For Log4j2 users: verify `log4j-core` version is 2.20.0+ via Spring Boot BOM

**Before:**
```xml
<!-- logback-spring.xml (Spring Boot 2.x compatible) -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

**After:**
```xml
<!-- logback-spring.xml (Spring Boot 3.x compatible) -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
    <!-- No changes needed for basic config -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

**Verification:**
```
# Test logging on application startup
mvn -s maven-settings.xml spring-boot:run 2>&1 | head -10
# Check for logging warnings/errors in console
```

**Artifact:** N/A — config file changes (verified by Verification commands above)

---

**REQ-P7-2.2: Handle Log Date Format Change**
**Action:** Verify log date format change from `yyyy-MM-dd HH:mm:ss.SSS` to ISO 8601 format `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`. If log parsing tools/pipelines depend on the old format, configure revert.
**Context:** Spring Boot 3.0 changes the default log timestamp format. Log parsing tools, log aggregation pipelines, and monitoring dashboards that parse timestamps may break. Recommended: update log parsing tools to handle ISO 8601 format instead of reverting.

**Steps:**
1. Start the application and check log output format
2. If log parsing tools/pipelines depend on the old format, add `logging.pattern.dateformat` to revert (see Before/After below)
3. For multi-module: check each module's configuration files if they override the log pattern

**Before:**
```
# Spring Boot 2.x log output
2024-01-15 10:30:45.123  INFO [main] com.example.App : Application started
```

**After:**
```
# Spring Boot 3.x log output (ISO 8601)
2024-01-15T10:30:45.123+00:00  INFO [main] com.example.App : Application started

# To revert to old format (if needed for log pipeline compatibility):
# application.yml: logging.pattern.dateformat: "yyyy-MM-dd HH:mm:ss.SSS"
# application.properties: logging.pattern.dateformat=yyyy-MM-dd HH:mm:ss.SSS
```

**Verification:**
```
# Start application and check log format
mvn -s maven-settings.xml spring-boot:run 2>&1 | head -5
# Confirm ISO 8601 timestamp format (or reverted format if configured)
```

**Artifact:** N/A — awareness/config change (verified by Verification commands above)

---

## 7.3 Data Source Configuration

**REQ-P7-3.1: Update HikariCP Configuration (If Explicit)**
**Applicability:** [CONDITIONAL] Only applies if your project explicitly configures HikariCP properties. Spring Boot 3.x continues using HikariCP as default — most apps don't need changes.
**Action:** Verify HikariCP property prefixes are correct. Standard `spring.datasource.hikari.*` properties are unchanged between 2.x and 3.x.
**Context:** HikariCP property names (`spring.datasource.hikari.*`) remain the same. No migration needed unless using deprecated pool-specific properties. The Properties Migrator (REQ-P7-1.1) will flag any deprecated data source properties.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P7-1.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
2. Run detection command: `grep -rq "spring.datasource.hikari\|HikariConfig\|HikariDataSource" src/ */src/ application*.yml application*.properties */src/main/resources/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Verify HikariCP properties in configuration files are using standard `spring.datasource.hikari.*` prefix
2. Check Properties Migrator output (REQ-P7-1.1) for any deprecated data source property warnings
3. For multi-module: check each module's `src/main/resources/` for data source configuration

**Verification:**
```
# Verify data source connectivity on startup
mvn -s maven-settings.xml spring-boot:run 2>&1 | head -20
# Check logs for successful data source initialization
```

**Artifact:** N/A — config verification (verified by Verification commands above)

---

## 7.4 SQL Script Initialization

**REQ-P7-4.1: Migrate SQL Script Initialization Properties**
**Applicability:** [CONDITIONAL] Only applies if your project uses SQL script initialization (schema.sql, data.sql).
**Action:** Update SQL script initialization configuration for Spring Boot 3.0 property changes.
**Context:** SQL script properties moved from `spring.datasource.*` to `spring.sql.init.*`. Important behavior change: Spring Boot 3.0 runs SQL scripts BEFORE Hibernate schema creation by default. For JPA projects using both schema.sql and Hibernate, set `spring.jpa.defer-datasource-initialization=true` to run scripts AFTER Hibernate.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P7-4.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
2. Run detection command: `find . -name "schema.sql" -o -name "data.sql" -o -name "schema-*.sql" -o -name "data-*.sql" | grep -q . || grep -rq "spring.datasource.schema\|spring.datasource.data\|spring.datasource.initialization-mode" src/ */src/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Rename SQL init properties per the mapping table below
2. If using JPA with schema.sql + Hibernate, add `spring.jpa.defer-datasource-initialization=true`
3. For multi-module: check each module's `src/main/resources/` for SQL init configuration

| Old Property (Spring Boot 2.x) | New Property (Spring Boot 3.x) |
|--------------------------------|--------------------------------|
| `spring.datasource.schema` | `spring.sql.init.schema-locations` |
| `spring.datasource.data` | `spring.sql.init.data-locations` |
| `spring.datasource.initialization-mode` | `spring.sql.init.mode` |
| `spring.datasource.platform` | `spring.sql.init.platform` |
| `spring.datasource.continue-on-error` | `spring.sql.init.continue-on-error` |
| `spring.datasource.separator` | `spring.sql.init.separator` |

**Before:**
```properties
spring.datasource.initialization-mode=always
spring.datasource.schema=classpath:schema.sql
spring.datasource.data=classpath:data.sql
spring.datasource.platform=h2
spring.datasource.continue-on-error=false
```

**After:**
```properties
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql
spring.sql.init.platform=h2
spring.sql.init.continue-on-error=false
# For JPA projects using schema.sql + Hibernate:
# spring.jpa.defer-datasource-initialization=true
```

**Verification:**
```
# Run application and verify SQL scripts execute
mvn -s maven-settings.xml spring-boot:run 2>&1 | grep -i "sql script\|schema.sql\|data.sql"
# Look for: "Executing SQL script from URL [classpath:schema.sql]"
```

**Artifact:** N/A — config file changes (verified by Verification commands above)

---

## 7.5 Config File Processing and Miscellaneous

**REQ-P7-5.1: Remove Legacy Config File Processing**
**Applicability:** [CONDITIONAL] Only applies if project uses `spring.config.use-legacy-processing` property.
**Action:** Remove `spring.config.use-legacy-processing` property if present. Spring Boot 3.0 no longer supports the legacy configuration file processing mode introduced in Spring Boot 2.4.
**Context:** The `spring.config.use-legacy-processing=true` property is removed in Spring Boot 3.0. Applications must use the new `spring.config.import` mechanism. If using Spring Cloud `bootstrap.yml`/`bootstrap.properties`, either add `spring-cloud-starter-bootstrap` dependency or migrate to `spring.config.import` style.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P7-5.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
2. Run detection command: `grep -rq "spring.config.use-legacy-processing" src/ */src/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Remove `spring.config.use-legacy-processing=true` from all config files
2. Ensure `spring.config.import` is used for external configuration sources
3. If using Spring Cloud `bootstrap.yml`/`bootstrap.properties`: add `spring-cloud-starter-bootstrap` dependency OR migrate to `spring.config.import` style
4. For multi-module: check each module's `src/main/resources/` and `src/test/resources/` for the legacy property

**Before:**
```yaml
# bootstrap.yml (legacy)
spring:
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: true
```

**After:**
```yaml
# application.yml (using spring.config.import)
spring:
  config:
    import: optional:configserver:http://config-server:8888
  cloud:
    config:
      fail-fast: true
```

**Verification:**
```
# Ensure no legacy processing property exists
grep -r "use-legacy-processing" src/ */src/ 2>/dev/null
# Should return empty

# Ensure application starts with new config processing
mvn -s maven-settings.xml spring-boot:run 2>&1 | head -10
```

**Artifact:** N/A — config file changes (verified by Verification commands above)

---

**REQ-P7-5.2: Remove Image Banner Configuration**
**Applicability:** [CONDITIONAL] Only applies if project uses image-based banner files.
**Action:** Remove image-based banner configurations. Spring Boot 3.0 no longer supports `banner.gif`, `banner.jpg`, or `banner.png` image banners. Only `banner.txt` text banners are supported.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P7-5.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
2. Run detection command: `find . -name "banner.gif" -o -name "banner.jpg" -o -name "banner.png" | grep -q . || grep -rq "spring.banner.image" src/ */src/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Remove image banner files (`banner.gif`, `banner.jpg`, `banner.png`) from all modules
2. Remove `spring.banner.image.*` properties from configuration files
3. Optionally create/update `banner.txt` as replacement
4. For multi-module: check each module's `src/main/resources/` for banner files

**Verification:**
```
# Ensure no image banner files exist
find . -name "banner.gif" -o -name "banner.jpg" -o -name "banner.png"
# Should return empty

# Ensure no image banner properties
grep -r "spring.banner.image" src/ */src/ 2>/dev/null
# Should return empty
```

**Artifact:** N/A — file removal (verified by Verification commands above)

---

## 7.6 Database Migration Tool Updates

**REQ-P7-6.1: Update Flyway and Liquibase Configuration**
**Applicability:** [CONDITIONAL] Only applies if project uses Flyway or Liquibase database migration tools.
**Action:** Update database migration tool dependencies and configuration for Spring Boot 3.x compatibility.
**Context:** Spring Boot 3.x ships Flyway 9.x+ (was 8.x in 2.7.x) and Liquibase 4.24.x+ (was 4.17.x). Flyway 9.x requires database-specific modules (e.g., `flyway-mysql` for MySQL/MariaDB, `flyway-sqlserver` for SQL Server, `flyway-database-oracle` for Oracle). Also update `javax.persistence` → `jakarta.persistence` in DDL generation properties.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P7-6.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
2. Run detection command: `grep -rq "flyway" pom.xml */pom.xml build.gradle build.gradle.kts 2>/dev/null || grep -rq "liquibase" pom.xml */pom.xml build.gradle build.gradle.kts 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-7-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. If Flyway detected: add database-specific Flyway module dependency (e.g., `flyway-mysql` for MySQL/MariaDB)
2. If Liquibase detected: update changelog schema namespace to `dbchangelog-latest.xsd`
3. Update `javax.persistence` → `jakarta.persistence` in DDL generation properties
4. For multi-module: check each module's pom.xml for Flyway/Liquibase dependencies and each module's `src/main/resources/` for changelog files

**Before:**
```xml
<!-- Flyway (Spring Boot 2.x) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```
```xml
<!-- Liquibase changelog (Spring Boot 2.x) -->
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd">
```
```properties
# Hibernate DDL (Spring Boot 2.x)
spring.jpa.properties.javax.persistence.schema-generation.scripts.action=create
```

**After:**
```xml
<!-- Flyway (Spring Boot 3.x - e.g., MySQL) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!-- Required for MySQL/MariaDB in Flyway 9+ -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```
```xml
<!-- Liquibase changelog (Spring Boot 3.x) -->
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">
```
```properties
# Hibernate DDL (Spring Boot 3.x)
spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create
```

**Verification:**
```
# Verify Flyway migration runs successfully (if Flyway)
mvn -s maven-settings.xml flyway:info

# Verify Liquibase changelog is valid (if Liquibase)
mvn -s maven-settings.xml liquibase:status

# Check no javax.persistence properties remain in config
grep -r "javax.persistence" src/main/resources/ */src/main/resources/ 2>/dev/null
# Should return empty
```

**Artifact:** N/A — dependency/config changes (verified by Verification commands above)

---

**REQ-P7-7.1: Create Final Configuration Check Log**
**Action:** Run compilation to capture post-configuration-migration state and verify all property migrations are complete.

**Steps:**
1. Run compilation: `mvn -s <settings-file> clean compile 2>&1 | tee ${ARTIFACTS_DIR}/phase-7-final-check.log`
2. Append property migration verification: `grep -r "spring\." src/main/resources/ --include="*.properties" --include="*.yml" >> ${ARTIFACTS_DIR}/phase-7-final-check.log 2>&1`
3. For multi-module: the compilation from root will cover all modules in reactor order

**Verification:**
```
test -f ${ARTIFACTS_DIR}/phase-7-final-check.log && echo "PASS: final check log exists" || echo "FAIL: missing final check log"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-7-final-check.log`

---

### REQ-P7-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 8 (API Adaptation) until completed.**

**Action:** Verify all Phase 7 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**
```
# Artifact existence checks
test -f ${ARTIFACTS_DIR}/phase-7-applicability.md && echo "PASS: phase-7-applicability.md" || echo "FAIL: missing phase-7-applicability.md"
test -f ${ARTIFACTS_DIR}/phase-7-startup.log && echo "PASS: phase-7-startup.log" || echo "FAIL: missing phase-7-startup.log"
test -f ${ARTIFACTS_DIR}/phase-7-migration-warnings.log && echo "PASS: phase-7-migration-warnings.log" || echo "FAIL: missing phase-7-migration-warnings.log"
test -f ${ARTIFACTS_DIR}/phase-7-final-check.log && echo "PASS: phase-7-final-check.log" || echo "FAIL: missing phase-7-final-check.log"

# Content validation
grep -qE "Started|Exception|ERROR|spring" ${ARTIFACTS_DIR}/phase-7-startup.log && echo "PASS: Startup log has content" || echo "FAIL: Startup log may be placeholder"
grep -qE "Detection Output|APPLICABLE|NOT APPLICABLE" ${ARTIFACTS_DIR}/phase-7-applicability.md && echo "PASS: Applicability has detection proof" || echo "FAIL: Applicability missing detection proof"
# Clean working directory check
test -z "$(git status --porcelain docs/spring-boot-migration/)" || echo "WARNING: Unstaged changes in migration directory"
```

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 7 at $(date -u)" > ${LOG_DIR}/phase-7-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-7] Complete Phase 7: Configuration Migration"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-7-applicability.md`
- `${ARTIFACTS_DIR}/phase-7-startup.log`
- `${ARTIFACTS_DIR}/phase-7-migration-warnings.log`
- `${ARTIFACTS_DIR}/phase-7-final-check.log`

**Decision:** Proceed to Phase 8 (API Adaptation)

# Phase 8: API Adaptation
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Keeping deprecated APIs that "still work" — all deprecated APIs must be replaced with their modern equivalents
- Skipping detection steps for CONDITIONAL requirements
**Advice:** This phase has the most requirements (18). Track each one individually. Do not batch-complete multiple REQ-IDs without individual verification. Fix code-level API changes and deprecated method calls. Group API changes by technical concern (JPA, Security, Web, Other) across all modules rather than per-module parallelization. For multi-module projects, run all detection commands from the repository root using `src/ */src/` patterns to scan all modules.

---

## 8.0 Prerequisites

```
# Verify Phase 7 completed
git log --all --grep="[phase-7] Complete Phase 7" || \
    { echo "ERROR: Phase 7 not completed"; exit 1; }
```

---

## 8.1 JPA/Hibernate Changes

### 8.1.1 JPA/Hibernate 6.x API Updates

**REQ-P8-1.1: Update Criteria API for Hibernate 6**
**Applicability:** [CONDITIONAL] Only applies if your project uses Hibernate Criteria API.
**Action:** Update deprecated Criteria API methods. Replace `Session.createCriteria()` with JPA Criteria API, `Restrictions.eq()` with `criteriaBuilder.equal()`, etc.
**Context:** Hibernate 6.x removes the legacy Criteria API entirely. All criteria operations must use the JPA Criteria API (`CriteriaBuilder`, `CriteriaQuery`, `Root`).

| Old API (Hibernate 5.x) | New API (Hibernate 6.x) | Notes |
|-------------------------|-------------------------|-------|
| `Session.createCriteria()` | Use JPA Criteria API | Legacy API removed |
| `Criteria.setMaxResults()` | `criteriaQuery.setMaxResults()` | Use JPA API |
| `Restrictions.eq()` | `criteriaBuilder.equal()` | Use JPA API |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-1.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "session.createCriteria\|Restrictions\." src/ */src/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Find all `session.createCriteria()` calls and replace with JPA Criteria API
2. Replace `Restrictions.*` with `CriteriaBuilder` equivalents
3. For multi-module: search all modules' `src/` directories

**Before:**
```java
// Hibernate 5.x legacy API
Criteria criteria = session.createCriteria(User.class);
criteria.add(Restrictions.eq("username", "john"));
criteria.setMaxResults(10);
List<User> users = criteria.list();
```

**After:**
```java
// JPA Criteria API (Hibernate 6.x)
CriteriaBuilder cb = session.getCriteriaBuilder();
CriteriaQuery<User> cq = cb.createQuery(User.class);
Root<User> root = cq.from(User.class);
cq.where(cb.equal(root.get("username"), "john"));

TypedQuery<User> query = session.createQuery(cq);
query.setMaxResults(10);
List<User> users = query.getResultList();
```

**Verification:**
```
# Verify no legacy Criteria API remains
grep -r "session.createCriteria\|Restrictions\." src/ */src/ 2>/dev/null --include="*.java" && \
    { echo "FAIL: Legacy Criteria API still used"; exit 1; }
echo "PASS: Criteria API migration complete"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

**REQ-P8-1.2: Update Entity ID Generation**
**Applicability:** [CONDITIONAL] Only applies if you use JPA entities with auto-generated IDs.
**Action:** Verify ID generation strategies are compatible with Hibernate 6. Ensure all `@GeneratedValue` annotations have an explicit strategy.
**Context:** `@GeneratedValue` without an explicit strategy defaults changed in Hibernate 6. `GenerationType.AUTO` now uses `SequenceStyleGenerator` instead of `TABLE` on databases that support sequences.

| Strategy | Use Case | Database Support |
|----------|----------|------------------|
| `GenerationType.IDENTITY` | Auto-increment columns | MySQL, PostgreSQL, SQL Server |
| `GenerationType.SEQUENCE` | Database sequences | PostgreSQL, Oracle, DB2 |
| `GenerationType.TABLE` | Sequence table (portable) | All databases |
| `GenerationType.UUID` | UUID generation | All databases (UUID column type) |
| `GenerationType.AUTO` | Hibernate chooses | [WARNING] Avoid (implicit behavior) |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-1.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "@GeneratedValue" src/ */src/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Find all `@GeneratedValue` annotations without an explicit `strategy` parameter
2. Add explicit strategy (e.g., `GenerationType.IDENTITY` or `GenerationType.SEQUENCE`) based on database type
3. For multi-module: search all modules' `src/` directories

**Before:**
```java
@Id
@GeneratedValue  // implicit AUTO — behavior changes in Hibernate 6
private Long id;
```

**After:**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY) // explicit strategy
private Long id;
```

**Verification:**
```
# Verify no implicit GenerationType.AUTO usage
grep -rn "@GeneratedValue" src/ */src/ 2>/dev/null --include="*.java" | grep -v "strategy" && \
    echo "WARNING: @GeneratedValue without explicit strategy found — verify Hibernate 6 compatibility" || \
    echo "PASS: All @GeneratedValue have explicit strategy"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

**REQ-P8-1.3: Initialize Entity Collection Fields for Hibernate 6**
**Applicability:** [CONDITIONAL] Only applies if you use JPA entities with collection relationships.
**Action:** Ensure all entity collection fields (`@OneToMany`, `@ManyToMany`, `@ElementCollection`) are initialized to empty collections.
**Context:** Hibernate 6 is stricter about null collections. Uninitialized collection fields can cause `NullPointerException` during lazy loading and cascade operations.

| Pattern | Preference | Rationale |
|---------|-----------|-----------|
| `List<T> = new ArrayList<>()` | **[PREFERRED]** | Standard ordered collection |
| `Set<T> = new HashSet<>()` | **[PREFERRED]** | Standard unique collection |
| `Set<T> = new LinkedHashSet<>()` | **[ACCEPTABLE]** | Unique + insertion order preserved |
| `List<T> = null` | **[DISCOURAGED]** | Causes NPE in Hibernate 6 |
| `Set<T> = null` | **[DISCOURAGED]** | Causes NPE in Hibernate 6 |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-1.3 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "@OneToMany\|@ManyToMany\|@ElementCollection" src/ */src/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Find all `@OneToMany`, `@ManyToMany`, `@ElementCollection` fields
2. Initialize any uninitialized collection fields with `new ArrayList<>()` or `new HashSet<>()`
3. For multi-module: search all modules' `src/` directories

**Before:**
```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;

    @ManyToMany
    private Set<Role> roles;
}
```

**After:**
```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();

    @ManyToMany
    private Set<Role> roles = new HashSet<>();
}
```

**Verification:**
```
# Verify no uninitialized collections remain
grep -B5 -A1 "@OneToMany\|@ManyToMany\|@ElementCollection" src/ */src/ 2>/dev/null --include="*.java" -r | \
    grep "private List<\|private Set<" | grep -v "= new \|= List.of\|= Set.of" || \
    echo "PASS: All collections initialized"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

### 8.1.2 Spring Data Changes

**REQ-P8-2.1: Update Repository Method Return Types**
**Action:** Update deprecated repository methods for Spring Data 3.x. Replace `getOne()` with `getReferenceById()`.
**Context:** Spring Data 3.x deprecates/removes several repository methods.

| Deprecated Method (2.x) | Replacement (3.x) | Status |
|------------------------|-------------------|--------|
| `T getOne(ID id)` | `T getReferenceById(ID id)` | **[DEPRECATED]** - Replace immediately |
| `void delete(T entity)` | `void delete(T entity)` | [YES] No change |
| `void deleteAll(Iterable<? extends T>)` | `void deleteAll(Iterable<? extends T>)` | [YES] No change |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-2.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "\.getOne(" src/ */src/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Find all `.getOne(` calls across all modules
2. Replace with `.getReferenceById(`
3. For multi-module: search all modules' `src/` directories

**Before:**
```java
User user = userRepository.getOne(userId);
```

**After:**
```java
User user = userRepository.getReferenceById(userId);
```

**Verification:**
```
# Verify no deprecated getOne() calls remain
grep -r "\.getOne(" src/ */src/ 2>/dev/null --include="*.java" && \
    { echo "FAIL: Deprecated getOne() still used"; exit 1; }
echo "PASS: Repository method return types updated"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

## 8.2 Spring Security Changes

### 8.2.1 OAuth2 Configuration

**REQ-P8-9.1: Migrate OAuth2 Resource Server Configuration**
**Applicability:** [CONDITIONAL] Only applies if application uses OAuth2/JWT authentication.
**Action:** Update OAuth2 resource server configuration from deprecated patterns to Spring Security 6.x lambda DSL.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-9.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "oauth2ResourceServer\|ResourceServerConfigurerAdapter\|@EnableResourceServer" src/ */src/ 2>/dev/null --include="*.java" || grep -rq "spring.security.oauth2.resourceserver" src/main/resources/ */src/main/resources/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Replace `WebSecurityConfigurerAdapter` with `SecurityFilterChain` bean
2. Replace `authorizeRequests()` with `authorizeHttpRequests()`
3. Replace `antMatchers()` with `requestMatchers()`
4. Convert to lambda DSL for `oauth2ResourceServer()`
5. For multi-module: check each module for security configuration classes

**Before:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt();
    }
}
```

**After:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );
        return http.build();
    }
}
```

**Verification:**
```
# Verify no deprecated OAuth2 patterns
grep -r "@EnableResourceServer\|ResourceServerConfigurerAdapter" src/ */src/ 2>/dev/null --include="*.java"
# Should return empty

# Test JWT authentication
mvn -s maven-settings.xml test -Dtest="*Security*,*Auth*"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

### 8.2.2 CORS Configuration

**REQ-P8-9.2: Migrate CORS Configuration**
**Applicability:** [CONDITIONAL] Only applies if application has custom CORS configuration.
**Action:** Update CORS configuration to use `CorsConfigurationSource` bean pattern (preferred when using Spring Security).
**Context:** The `WebMvcConfigurer.addCorsMappings()` approach still works but `CorsConfigurationSource` is preferred when using Spring Security, as it integrates directly with the security filter chain.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-9.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "cors\|CorsConfiguration\|addCorsMappings\|@CrossOrigin" src/ */src/ 2>/dev/null --include="*.java" || grep -rq "spring.mvc.cors\|spring.webflux.cors" src/main/resources/ */src/main/resources/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Replace `WebMvcConfigurer.addCorsMappings()` with `CorsConfigurationSource` bean if using Spring Security
2. If not using Spring Security, `addCorsMappings()` is acceptable
3. For multi-module: check each module for CORS configuration classes

**Before:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://example.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

**After:**
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://example.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

**Verification:**
```
# Verify CORS configuration exists
grep -r "CorsConfigurationSource\|addCorsMappings\|@CrossOrigin" src/ */src/ 2>/dev/null --include="*.java"

# Test CORS headers
curl -I -X OPTIONS http://localhost:8080/api/ -H "Origin: https://example.com" -H "Access-Control-Request-Method: GET"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

## 8.3 Web/REST Changes

### 8.3.1 Spring MVC Path Matching

**REQ-P8-5.1: Update Trailing Slash Matching Behavior**
**Action:** Handle breaking change in Spring MVC trailing slash matching for PathPatternParser.
**Context:** PathPatternParser (default in Spring Boot 3.x) does NOT match trailing slashes by default. Routes `/users` and `/users/` are now distinct.

**Detection:**
```
# For multi-module: run from repository root
grep -r "@RequestMapping\|@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java"
```

**Steps:**
1. Identify all controller endpoints across all modules
2. Option 1 [PREFERRED]: Add explicit trailing slash handling `@GetMapping({"", "/"})`
3. Option 2 [ACCEPTABLE]: Configure global trailing slash matching via `WebMvcConfigurer`
4. For multi-module: check each module's controller classes

**Before:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("")  // Matched both /api/users and /api/users/
    public List<User> getUsers() {
        return userService.findAll();
    }
}
```

**After:**
```java
// Option 1: Explicit Trailing Slash [PREFERRED]
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping({"", "/"})  // Explicitly handle both
    public List<User> getUsers() {
        return userService.findAll();
    }
}

// Option 2: Configure Global Behavior [ACCEPTABLE]
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}
```

**Verification:**
```
# Test endpoint with and without trailing slash
curl http://localhost:8080/api/users
curl http://localhost:8080/api/users/
# Both should return same response if configured correctly
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

**REQ-P8-5.2: Configure PathPattern Parser Matching**
**Applicability:** [CONDITIONAL] Only applies if project uses AntPathMatcher-specific patterns.
**Action:** Explicitly configure path pattern matching strategy if using AntPathMatcher features.
**Context:** PathPatternParser is now default (replaces AntPathMatcher). Some patterns behave differently.

| Feature | AntPathMatcher | PathPatternParser |
|---------|----------------|-------------------|
| `**` in middle of pattern | Supported | Supported |
| `**` at end of pattern | Supported | Supported |
| Case sensitivity | Configurable | Configurable |
| Trailing slash matching | Auto-matched | Explicit only |
| Performance | Slower | Faster |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-5.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "ant_path_matcher\|AntPathMatcher\|setUseTrailingSlashMatch" src/ */src/ 2>/dev/null --include="*.java" || grep -rq "ant_path_matcher" src/main/resources/ */src/main/resources/ 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Keep PathPatternParser (default) and update `/**` patterns to `/{*path}` capture syntax [PREFERRED]
2. If reverting to AntPathMatcher is necessary, set `spring.mvc.pathmatch.matching-strategy=ant_path_matcher` [DISCOURAGED]
3. For multi-module: check each module's controller classes and config files

**Before:**
```java
// AntPathMatcher (2.x)
@GetMapping("/files/**")  // Matched /files/a/b/c
public Resource getFile(HttpServletRequest request) { ... }
```

**After:**
```java
// PathPatternParser (3.x) [PREFERRED]
@GetMapping("/files/{*path}")  // Capture variable path
public Resource getFile(@PathVariable String path) {
    return fileService.load(path);
}
```

**Verification:**
```
# Check for path matching errors in logs
mvn -s maven-settings.xml spring-boot:run 2>&1 | grep -i "path.*pattern\|mapping"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

## 8.4 Framework-Specific & Other

### 8.4.1 Test Framework Migration

**REQ-P8-4.1: Migrate JUnit 4 to JUnit 5**
**Applicability:** [CONDITIONAL] Only applies if you use JUnit 4.x tests.
**Action:** Migrate test classes from JUnit 4 to JUnit 5 (Jupiter). JUnit 4 packages are removed from Spring Boot 3.x dependencies.
**Context:** Migration table for imports and annotations:

| JUnit 4 | JUnit 5 (Jupiter) |
|---------|-------------------|
| `import org.junit.Test` | `import org.junit.jupiter.api.Test` |
| `import org.junit.Before` | `import org.junit.jupiter.api.BeforeEach` |
| `import org.junit.After` | `import org.junit.jupiter.api.AfterEach` |
| `import org.junit.BeforeClass` | `import org.junit.jupiter.api.BeforeAll` |
| `import org.junit.AfterClass` | `import org.junit.jupiter.api.AfterAll` |
| `import org.junit.Ignore` | `import org.junit.jupiter.api.Disabled` |
| `@RunWith(SpringRunner.class)` | `@ExtendWith(SpringExtension.class)` |
| `@RunWith(MockitoJUnitRunner.class)` | `@ExtendWith(MockitoExtension.class)` |
| `Assert.assertEquals(expected, actual)` | `Assertions.assertEquals(expected, actual)` |

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-4.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "org.junit.Test\|@RunWith\|@Before\|@After" src/test/ */src/test/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Replace JUnit 4 imports with JUnit 5 equivalents (see Context table)
2. Replace `@RunWith` with `@ExtendWith`
3. Replace `Assert.*` with `Assertions.*`
4. For multi-module: search all modules' `src/test/` directories

**Before:**
```java
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class UserServiceTest {
    @Before
    public void setUp() { /* setup code */ }

    @Test
    public void testFindUser() {
        User user = userService.findById(1L);
        assertEquals("John", user.getName());
    }
}
```

**After:**
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
public class UserServiceTest {
    @BeforeEach
    public void setUp() { /* setup code */ }

    @Test
    public void testFindUser() {
        User user = userService.findById(1L);
        assertEquals("John", user.getName());
    }
}
```

**Verification:**
```
# Verify no JUnit 4 imports remain
grep -r "import org.junit.Test\|import org.junit.Before\|import org.junit.After" src/test/ */src/test/ 2>/dev/null --include="*.java"
# Should return empty

# Run tests
mvn -s maven-settings.xml test
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

### 8.4.2 Elasticsearch Integration

**REQ-P8-6.1: Migrate Elasticsearch Client API**
**Applicability:** [CONDITIONAL] Only applies if your project uses Elasticsearch.
**Action:** Migrate from deprecated `RestHighLevelClient` to new `ElasticsearchClient`.
**Context:** Spring Data Elasticsearch 5.x (bundled with Spring Boot 3.x) removes `RestHighLevelClient`.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-6.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "spring-boot-starter-data-elasticsearch\|elasticsearch-rest-high-level-client" pom.xml */pom.xml build.gradle 2>/dev/null || grep -rq "RestHighLevelClient\|ElasticsearchClient" src/ */src/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Replace `RestHighLevelClient` with `ElasticsearchClient`
2. Update import packages from `org.elasticsearch.*` to `co.elastic.clients.elasticsearch.*`
3. Update API call patterns (see Before/After)
4. For multi-module: search all modules for Elasticsearch usage

**Before:**
```java
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;

@Service
public class DocumentService {
    @Autowired
    private RestHighLevelClient client;

    public void indexDocument(String index, String id, Map<String, Object> doc) throws IOException {
        IndexRequest request = new IndexRequest(index).id(id).source(doc);
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
    }
}
```

**After:**
```java
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;

@Service
public class DocumentService {
    @Autowired
    private ElasticsearchClient client;

    public void indexDocument(String index, String id, Map<String, Object> doc) throws IOException {
        IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
            .index(index).id(id).document(doc));
        IndexResponse response = client.index(request);
    }
}
```

**Verification:**
```
# Verify no RestHighLevelClient remains
grep -r "RestHighLevelClient" src/ */src/ 2>/dev/null --include="*.java"
# Should return empty

# Run integration tests
mvn -s maven-settings.xml test -Dtest="*ElasticsearchTest"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---
### 8.4.3 Redis Integration

**REQ-P8-7.1: Update Redis Connection Factory Configuration**
**Applicability:** [CONDITIONAL] Only applies if your project uses Redis.
**Action:** Review Redis connection factory configuration for Spring Boot 3.x. Lettuce is now strongly preferred over Jedis.
**Context:** Spring Boot 3.x deprecates Jedis in favor of Lettuce for reactive and performance benefits. Lettuce is included by default with `spring-boot-starter-data-redis`. Property prefix changed from `spring.redis.*` to `spring.data.redis.*` (handled in Phase 7 via Properties Migrator).

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-7.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "spring-boot-starter-data-redis" pom.xml */pom.xml build.gradle 2>/dev/null || grep -rq "jedis\|lettuce" pom.xml */pom.xml build.gradle 2>/dev/null || grep -rq "RedisConnectionFactory\|JedisConnection\|LettuceConnection" src/ */src/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Remove explicit Jedis dependency if present — Lettuce is included by default
2. Update Redis configuration to use `spring.data.redis.*` prefix (if not already done in Phase 7)
3. For multi-module: check each module's pom.xml for Redis dependencies

**Before:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

**After:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <!-- Lettuce is included by default, no explicit dependency needed -->
</dependency>
```

**Verification:**
```
# Verify no Jedis dependency remains (unless explicitly required)
grep -r "jedis" pom.xml */pom.xml 2>/dev/null
# Should return empty unless Jedis is intentionally kept

# Verify Redis config uses correct prefix
grep -r "spring.redis\." src/main/resources/ */src/main/resources/ 2>/dev/null
# Should return empty (should be spring.data.redis.*)
```

**Artifact:** N/A — dependency/config changes (verified by Verification commands above)

---
### 8.4.4 Spring Framework 6.0 API Changes

**REQ-P8-8.1: Handle HttpMethod Enum-to-Class Change**
**Action:** Update code that depends on `HttpMethod` being an enum. In Spring Framework 6.0, `HttpMethod` is now a class instead of an enum.
**Context:** Code that uses enum-specific operations (`switch` statements, `==` comparison, `EnumSet`) on `HttpMethod` will break. `.valueOf()` still works.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-8.1 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "switch.*HttpMethod\|case.*HttpMethod\|EnumSet.*HttpMethod\|== HttpMethod\." src/main/java/ */src/main/java/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Replace `switch` statements on `HttpMethod` with `if-else` using `.equals()`
2. Replace `==` comparisons with `.equals()`
3. Replace `EnumSet<HttpMethod>` with `Set<HttpMethod>`
4. For multi-module: search all modules' `src/main/java/` directories

**Before:**
```java
// Enum-specific operations (Spring Framework 5.x)
switch (method) {
    case GET: handleGet(); break;
    case POST: handlePost(); break;
}
if (request.getMethod() == HttpMethod.GET) { ... }
```

**After:**
```java
// Class-based API (Spring Framework 6.0)
if (method.equals(HttpMethod.GET)) {
    handleGet();
} else if (method.equals(HttpMethod.POST)) {
    handlePost();
}
if (request.getMethod().equals(HttpMethod.GET)) { ... }
```

**Verification:**
```
# Verify no enum-specific operations on HttpMethod
grep -r "switch.*HttpMethod\|case.*HttpMethod\|EnumSet.*HttpMethod" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java" && \
    { echo "FAIL: HttpMethod enum-specific operations still present"; exit 1; }
echo "PASS: HttpMethod usage updated for class-based API"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

**REQ-P8-8.2: Adopt ProblemDetail for RFC 7807 Error Responses**
**Applicability:** [CONDITIONAL] Only applies if project has REST API endpoints.
**Action:** Configure and adopt `ProblemDetail` (RFC 7807) standardized error response support introduced in Spring Framework 6.0 / Spring Boot 3.x.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-8.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "@RestController\|@RestControllerAdvice" src/ */src/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Enable ProblemDetail in application config: `spring.mvc.problemdetails.enabled=true`
2. Update `@RestControllerAdvice` classes to extend `ResponseEntityExceptionHandler` and return `ProblemDetail`
3. For multi-module: check each module for REST controller advice classes

**Before:**
```java
// Spring Boot 2.x — custom error response
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
```

**After:**
```java
// Spring Boot 3.x — ProblemDetail RFC 7807 [PREFERRED]
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://api.example.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
```

**Verification:**
```
# Test error response format
curl -s http://localhost:8080/api/nonexistent | python -m json.tool
# Should return RFC 7807 format:
# {"type":"...", "title":"...", "status":404, "detail":"...", "instance":"..."}
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

**REQ-P8-8.3: Replace CommonsMultipartResolver with StandardServletMultipartResolver**
**Applicability:** [CONDITIONAL] Only applies if you use CommonsMultipartResolver for file uploads.
**Action:** Remove `CommonsMultipartResolver` usage. Spring Framework 6.0 removed `CommonsMultipartResolver` entirely.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-8.3 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "CommonsMultipartResolver\|commons-fileupload" src/ */src/ pom.xml */pom.xml build.gradle 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Remove `commons-fileupload` dependency from pom.xml/build.gradle
2. Remove any `CommonsMultipartResolver` bean definitions
3. Configure multipart via `spring.servlet.multipart.*` properties
4. For multi-module: check each module's pom.xml and Java source

**Before:**
```xml
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.4</version>
</dependency>
```

**After:**
```yaml
# application.yml - Configure via properties (no explicit bean needed)
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
      enabled: true
```

**Verification:**
```
# Ensure commons-fileupload is removed
grep -r "commons-fileupload" pom.xml */pom.xml build.gradle 2>/dev/null
# Should return empty

# Ensure CommonsMultipartResolver is not referenced
grep -r "CommonsMultipartResolver" src/ */src/ 2>/dev/null
# Should return empty

# Test file upload functionality
mvn -s maven-settings.xml test -Dtest="*Upload*,*Multipart*"
```

**Artifact:** N/A — dependency/config changes (verified by Verification commands above)

---

**REQ-P8-8.4: Update Hibernate 6 Type System Changes**
**Applicability:** [CONDITIONAL] Only applies if you use custom Hibernate types, `@Type` annotations, or `@TypeDef`.
**Action:** Update custom Hibernate type mappings for Hibernate 6.x's new type system.
**Context:** Hibernate 6 replaces `BasicType` with `JdbcType` mappings. `@TypeDef` is removed; use `@JdbcTypeCode` instead.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-8.4 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "@TypeDef\|@Type(\|UserType\|BasicType" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Remove `@TypeDef` annotations
2. Replace `@Type(type = "json")` with `@JdbcTypeCode(SqlTypes.JSON)`
3. Update custom `UserType` implementations to new Hibernate 6 API
4. For multi-module: search all modules' `src/main/java/` directories

**Before:**
```java
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
public class Event {
    @Id
    private Long id;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private Map<String, Object> payload;
}
```

**After:**
```java
@Entity
public class Event {
    @Id
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> payload;
}
```

**Verification:**
```
# Verify no @TypeDef annotations remain
grep -r "@TypeDef" src/main/java/ */src/main/java/ 2>/dev/null
# Should return empty

# Run tests
mvn -s maven-settings.xml test -Dtest="*Entity*,*Repository*"
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

**REQ-P8-8.5: Remove @ConstructorBinding from Configuration Properties Classes**
**Applicability:** [CONDITIONAL] Only applies if you use `@ConfigurationProperties` classes with constructor binding.
**Action:** Remove `@ConstructorBinding` annotation from configuration properties classes.
**Context:** Spring Boot 3.x infers constructor binding automatically when a `@ConfigurationProperties` class has a single parameterized constructor. The annotation is no longer needed and should be removed.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-8.5 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "@ConstructorBinding" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Remove `@ConstructorBinding` annotation from all `@ConfigurationProperties` classes
2. Remove the `import` for `ConstructorBinding`
3. For multi-module: search all modules' `src/main/java/` directories

**Before:**
```java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "app.datasource")
@ConstructorBinding
public class DataSourceProperties {
    private final String url;
    private final String username;
    public DataSourceProperties(String url, String username) {
        this.url = url;
        this.username = username;
    }
}
```

**After:**
```java
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceProperties {
    private final String url;
    private final String username;
    // Constructor binding is inferred automatically
    public DataSourceProperties(String url, String username) {
        this.url = url;
        this.username = username;
    }
}
```

**Verification:**
```
# Verify no @ConstructorBinding remains
grep -r "@ConstructorBinding" src/main/java/ */src/main/java/ 2>/dev/null
# Should return empty

# Verify constructor binding still works
mvn -s maven-settings.xml spring-boot:run 2>&1 | head -20
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

### 8.4.5 Third-Party Library Breaking Changes

**REQ-P8-8.6: Verify Third-Party Library API Compatibility**
**Applicability:** [CONDITIONAL] Only if third-party libraries were upgraded in Phase 4
**Action:** Verify that upgraded third-party libraries have no API breaking changes causing runtime failures.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-8.6 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Check if Phase 4 dependency rationale exists: `test -f ${ARTIFACTS_DIR}/phase-4-dependency-rationale.md`
3. If file exists, check for upgrades: `grep -qE "upgraded|changed|bumped" ${ARTIFACTS_DIR}/phase-4-dependency-rationale.md`
4. If grep found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE - Requires API compatibility verification" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
5. If file does not exist OR grep found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE (no third-party upgrades in Phase 4)" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Review `${ARTIFACTS_DIR}/phase-4-dependency-rationale.md` for upgraded libraries
2. Run full test suite: `mvn -s maven-settings.xml clean test 2>&1 | tee ${ARTIFACTS_DIR}/phase-8-third-party-test.log`
3. Check test output for API-breaking symptoms: `NoSuchMethodError`, `ClassNotFoundException`, `IncompatibleClassChangeError`, `AbstractMethodError`
4. If breaking changes found, update call sites to use the new library API and re-run tests
5. For multi-module: run from repository root; the reactor build will test all modules

**Verification:**
```
# Verify no API breaking change symptoms in test output
grep -qE "NoSuchMethodError|ClassNotFoundException|IncompatibleClassChangeError" \
    ${ARTIFACTS_DIR}/phase-8-third-party-test.log && \
    { echo "FAIL: Third-party API breaking changes detected"; exit 1; }

echo "PASS: Third-party library compatibility verified"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-8-third-party-test.log`

---

### 8.4.6 WebSocket Configuration

**REQ-P8-9.3: Migrate WebSocket Configuration**
**Applicability:** [CONDITIONAL] Only applies if application uses WebSocket/STOMP messaging.
**Action:** Update WebSocket configuration for Spring Boot 3.x compatibility.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P8-9.3 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
2. Run detection command: `grep -rq "WebSocketMessageBrokerConfigurer\|@EnableWebSocketMessageBroker\|WebSocketConfigurer\|@EnableWebSocket" src/ */src/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-8-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Replace `extends AbstractWebSocketMessageBrokerConfigurer` with `implements WebSocketMessageBrokerConfigurer`
2. Add explicit `setAllowedOrigins()` on STOMP endpoints (Spring Boot 3.x requires it)
3. For multi-module: search all modules for WebSocket config classes

**Before:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}
```

**After:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

**Verification:**
```
# Verify no deprecated abstract class
grep -r "AbstractWebSocketMessageBrokerConfigurer" src/ */src/ 2>/dev/null --include="*.java"
# Should return empty

# Verify WebSocket endpoint responds
curl -v http://localhost:8080/ws/info
```

**Artifact:** N/A — code changes (verified by Verification commands above)

---

### REQ-P8-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to Phase 9 (Build & Test) until completed.**

**Action:** Verify all Phase 8 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**

1. Check applicability file exists (proves all detection steps were run):
   ```
   test -f ${ARTIFACTS_DIR}/phase-8-applicability.md && echo "PASS: phase-8-applicability.md" || echo "FAIL: missing phase-8-applicability.md"
   ```
2. Content validation — confirm applicability file has detection proof:
   ```
   grep -qE "Detection Output|APPLICABLE|NOT APPLICABLE" ${ARTIFACTS_DIR}/phase-8-applicability.md && echo "PASS: Applicability has detection proof" || echo "FAIL: Applicability missing detection proof"
   ```
3. Check conditional artifact based on applicability result:
   - Check REQ-P8-8.6: `grep -q "REQ-P8-8.6" ${ARTIFACTS_DIR}/phase-8-applicability.md && echo "PASS: REQ-P8-8.6 detection recorded" || echo "FAIL: REQ-P8-8.6 detection missing from applicability file"`
     - If applicability file contains "REQ-P8-8.6" + "APPLICABLE" (not "NOT APPLICABLE"): verify `test -f ${ARTIFACTS_DIR}/phase-8-third-party-test.log && echo "PASS" || echo "FAIL: Third-party testing was APPLICABLE but phase-8-third-party-test.log missing"`
     - If applicability file contains "REQ-P8-8.6" + "NOT APPLICABLE": phase-8-third-party-test.log not required — OK
4. Clean working directory check: `git status --porcelain docs/spring-boot-migration/`
   - If unexpected files appear, STOP and investigate before staging

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 8 at $(date -u)" > ${LOG_DIR}/phase-8-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-8] Complete Phase 8: API Adaptation"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-8-applicability.md` (ALWAYS — contains detection proof for all CONDITIONAL requirements)
- `${ARTIFACTS_DIR}/phase-8-third-party-test.log` (only if REQ-P8-8.6 APPLICABLE per applicability file)

**Decision:** Proceed to Phase 9 (Build & Test)

# Phase 9: Build & Test
**TODO:** Before starting, use TodoWrite to create one item per REQ-ID in this phase (scan all `**REQ-PN-*` headings).
**Forbidden Actions:**
- Disabling, deleting, or @Ignore-ing tests to achieve 100% pass rate
- Claiming tests pass without pasting visible evidence
**Advice:** Even if tests pass on the first iteration, still create iteration-1 log files. The artifacts prove the process was followed, not just that the outcome was achieved. Each iteration MUST have its own log file.

**Multi-Module Compilation Fix Order:** For multi-module projects, fix compilation errors starting from **leaf modules** (modules with no internal dependencies) and work upward to parent/aggregator modules. Use `mvn -pl <module-name> compile` to test individual modules. Fix all build and test failures through iterative test-fix cycles and validate migration success.
---

## 9.0 Prerequisites

```
# Verify Phase 8 completed
git log --all --grep="[phase-8] Complete Phase 8" || \
    { echo "ERROR: Phase 8 not completed"; exit 1; }
```

---

## 9.1 Iterative Build & Test Fixes

**REQ-P9-1: Fix All Build and Test Failures**
**Applicability:** [ALWAYS]
**Action:** Systematically fix all compilation errors, test failures, and runtime issues through iterative test-fix cycles. Use **Appendix A** to identify and resolve errors. Maximum 5 iterations — if failures persist after 5 cycles, STOP and create `${ARTIFACTS_DIR}/phase-9-escalation-report.md` listing remaining failures, error patterns, and Appendix A cross-references.
**Context:** The `N` in filenames below (e.g., `phase-9-compile-iteration-N.log`) is a PLACEHOLDER. Replace `N` with the actual iteration number: `1`, `2`, `3`, etc. Do NOT use literal `N` in filenames.

**Steps:**
1. For iteration N = 1..5 (stop early if passing):
   - Compile: `mvn -s maven-settings.xml clean compile 2>&1 | tee ${ARTIFACTS_DIR}/phase-9-compile-iteration-N.log` (Appendix A: A, D, E)
   - Unit tests: `mvn -s maven-settings.xml test 2>&1 | tee ${ARTIFACTS_DIR}/phase-9-test-iteration-N.log` (Appendix A: A, B, D)
   - Integration tests: `mvn -s maven-settings.xml verify 2>&1 | tee ${ARTIFACTS_DIR}/phase-9-integration-iteration-N.log` (Appendix A: C, D)
   - Smoke test (optional): `mvn -s maven-settings.xml spring-boot:run 2>&1 | tee ${ARTIFACTS_DIR}/phase-9-startup.log` (Appendix A: E)
   - If any step fails: fix the root cause and re-run in the next iteration (increment N)
2. Once compile+tests+verify pass, run full validation once: `mvn -s maven-settings.xml clean verify 2>&1 | tee ${ARTIFACTS_DIR}/phase-9-final-validation.log` (must exit 0)
3. Do NOT delete/disable/skip tests. If a test cannot be fixed after investigation, document it in `${ARTIFACTS_DIR}/phase-9-escalation-report.md` and escalate to the user.
4. For multi-module: run from repository root; the reactor build covers all modules.

**Verification:**
```
# Verify final build passes
mvn -s maven-settings.xml clean verify 2>&1 | tail -5
# Must show "BUILD SUCCESS"

# Verify no test shortcuts — for multi-module use */src/test/
grep -r "@Disabled\|@Ignore" src/test/ */src/test/ 2>/dev/null --include="*.java" | grep -v "// pre-existing" && \
    { echo "FAIL: Test shortcuts detected"; exit 1; }

echo "PASS: All build and test failures resolved"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-9-compile-iteration-*.log`, `${ARTIFACTS_DIR}/phase-9-test-iteration-*.log`, `${ARTIFACTS_DIR}/phase-9-integration-iteration-*.log`, `${ARTIFACTS_DIR}/phase-9-final-validation.log`

---

## 9.2 Post-Migration Cleanup

**REQ-P9-4.1: Remove Migration-Only Dependencies**
**Applicability:** [ALWAYS]
**Action:** Remove temporary dependencies and plugins added during migration that should not be present in production.

**Steps:**
1. Remove `spring-boot-properties-migrator` dependency from pom.xml (added in Phase 7)
2. Remove `rewrite-maven-plugin` plugin block from pom.xml (added in Phase 6)
3. Remove `junit-vintage-engine` dependency if all JUnit 4 tests have been migrated to JUnit 5
4. For multi-module: check each module's pom.xml for these dependencies

**Verification:**
```
# Verify properties migrator removed
mvn -s maven-settings.xml dependency:tree | grep "spring-boot-properties-migrator" && \
    { echo "FAIL: Properties migrator still present"; exit 1; }

# Verify OpenRewrite removed — for multi-module check all pom files
grep -r "openrewrite" pom.xml */pom.xml build.gradle 2>/dev/null && \
    { echo "FAIL: OpenRewrite still present"; exit 1; }

# Verify application still works after cleanup
mvn -s maven-settings.xml clean verify
```

**Artifact:** N/A — dependency removal (verified by Verification commands above)

---

**REQ-P9-4.2: Verify Observability Configuration**
**Applicability:** [CONDITIONAL] Only if project uses Micrometer/Actuator metrics.
**Action:** Verify that observability (Actuator/Micrometer) endpoints are functional after migration.

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P9-4.2 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-9-applicability.md
   ```
2. Run detection command: `grep -rq "spring-boot-starter-actuator\|micrometer-core" pom.xml */pom.xml build.gradle 2>/dev/null`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE" >> ${ARTIFACTS_DIR}/phase-9-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE" >> ${ARTIFACTS_DIR}/phase-9-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Start the application: `mvn -s maven-settings.xml spring-boot:run &`
2. Wait for startup, then verify metrics endpoint: `curl -s http://localhost:8080/actuator/metrics | python -m json.tool`
3. Verify health endpoint: `curl -s http://localhost:8080/actuator/health | python -m json.tool`
4. Stop the application

**Verification:**
```
curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"' && echo "PASS: Actuator health OK" || echo "FAIL: Actuator health check failed"
```

**Artifact:** N/A — runtime verification

---

## 9.3 Final Verification

**REQ-P9-5.1: Dependency Tree Comparison**
**Applicability:** [ALWAYS]
**Action:** Generate post-migration dependency tree and compare with Phase 0 baseline.

**Steps:**
1. Generate post-migration tree: `mvn -s maven-settings.xml dependency:tree -DoutputFile=${ARTIFACTS_DIR}/phase-9-dependency-tree-after.txt`
2. Diff with baseline: `diff ${ARTIFACTS_DIR}/phase-0-dependency-tree-before.txt ${ARTIFACTS_DIR}/phase-9-dependency-tree-after.txt > ${ARTIFACTS_DIR}/phase-9-dependency-tree-diff.txt`
3. Create `${ARTIFACTS_DIR}/phase-9-dependency-comparison.md` containing: Major Version Upgrades table (Dependency | Before | After), Namespace Changes table, Risk Assessment (OK/PASS/REVIEW per category)
4. Verify internal dependency consistency: `mvn dependency:tree | grep "com.gs.\|com.goldmansachs."` — all internal dependencies must be on Jakarta EE 9+ / Spring Boot 3.x compatible versions
5. For multi-module: run `mvn dependency:tree` from root to capture all modules

**Verification:**
```
test -f ${ARTIFACTS_DIR}/phase-9-dependency-tree-after.txt && echo "PASS: Post-migration dependency tree" || echo "FAIL: Missing dependency tree"
test -f ${ARTIFACTS_DIR}/phase-9-dependency-comparison.md && echo "PASS: Dependency comparison report" || echo "FAIL: Missing comparison report"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-9-dependency-tree-after.txt`, `${ARTIFACTS_DIR}/phase-9-dependency-tree-diff.txt`, `${ARTIFACTS_DIR}/phase-9-dependency-comparison.md`

---

**REQ-P9-5.2: Performance Baseline**
**Applicability:** [ALWAYS]
**Action:** Capture performance metrics for comparison with Phase 0 baseline.

**Steps:**
1. Start application: `mvn -s maven-settings.xml spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" > ${ARTIFACTS_DIR}/phase-9-startup.log 2>&1 &`
   - For multi-module: specify the bootable module with `mvn -pl <module> spring-boot:run ...` or `./gradlew :<module>:bootRun`
2. Wait for startup, then capture startup time: `grep "Started.*Application in" ${ARTIFACTS_DIR}/phase-9-startup.log`
3. Stop application
4. Create `${ARTIFACTS_DIR}/phase-9-performance-comparison.md` with: Performance Comparison table (Metric | Before | After | Delta), Conclusion (PASS/FAIL per metric)

**Verification:**
```
test -f ${ARTIFACTS_DIR}/phase-9-performance-comparison.md && echo "PASS: Performance comparison report" || echo "FAIL: Missing performance report"
```

**Artifact:** `${ARTIFACTS_DIR}/phase-9-performance-comparison.md`

---

**REQ-P9-5.3: Code Coverage Comparison**
**Applicability:** [CONDITIONAL] Only if JaCoCo or another coverage tool is configured.
**Action:** Generate code coverage report and compare with pre-migration baseline.

**Steps:**
1. Generate coverage report: `mvn -s maven-settings.xml clean verify jacoco:report`
2. If no pre-migration coverage baseline exists, document "N/A — no pre-migration coverage baseline" and proceed
3. Append a **Code Coverage** section to `${ARTIFACTS_DIR}/phase-9-performance-comparison.md` with table (Metric | Before | After | Status)
4. For multi-module: ensure `spring-boot-maven-plugin` (repackage goal) is only in modules that produce executable JARs; library modules should NOT have it. Run `mvn -s maven-settings.xml clean package` and verify all module JARs are produced correctly.

**Verification:**
```
grep -q "Coverage" ${ARTIFACTS_DIR}/phase-9-performance-comparison.md && echo "PASS: Coverage comparison documented" || echo "WARNING: Coverage comparison not found in report"
```

**Artifact:** Coverage section appended to `${ARTIFACTS_DIR}/phase-9-performance-comparison.md`

---

**REQ-P9-5.4: API Contract Compatibility Check**
**Applicability:** [CONDITIONAL] Only applies if the application exposes REST APIs consumed by other services.
**Action:** Verify that javax-to-jakarta namespace migration does not break REST API serialization contracts for downstream consumers.
**Context:** The javax→jakarta namespace change (Phase 6) can break API contracts if REST endpoints return objects containing `jakarta.*` types that downstream consumers deserialize using old `javax.*` class names. Risk levels: LOW (standard DTOs) → MEDIUM (`@JsonTypeInfo(use = Id.CLASS)`) → HIGH (error responses with `exception.getClass().getName()`).

**Detection:**
1. Record detection header:
   ```
   echo "--- REQ-P9-5.4 Detection Output ---" >> ${ARTIFACTS_DIR}/phase-9-applicability.md
   ```
2. Run detection command: `grep -rq "@RestController\|@Controller.*@ResponseBody" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java"`
3. If the command found matches → APPLICABLE. Run:
   ```
   echo "Result: APPLICABLE - REST API detected - contract compatibility check required" >> ${ARTIFACTS_DIR}/phase-9-applicability.md
   ```
   Proceed to the **Steps** section below.
4. If the command found NO matches → NOT APPLICABLE. Run:
   ```
   echo "Result: NOT APPLICABLE (No REST API endpoints)" >> ${ARTIFACTS_DIR}/phase-9-applicability.md
   ```
   Skip the **Steps** section and proceed to the next REQ.

**Steps:**
1. Scan for jakarta types in API response/request objects: `grep -rn "@RestController" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java" -l` then check returned types for `jakarta.` references. Output to `${ARTIFACTS_DIR}/phase-9-api-contract-check.txt`
2. Scan for risky JSON serialization patterns: `grep -rn "@JsonTypeInfo\|@JsonSubTypes\|getClass().getName()\|getClass().getCanonicalName()" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java" | tee -a ${ARTIFACTS_DIR}/phase-9-api-contract-check.txt`
3. Audit exception handlers for class name leakage: `grep -rn "exception.getClass\|error.*getClass\|cause.*getName" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java" | tee -a ${ARTIFACTS_DIR}/phase-9-api-contract-check.txt`
4. Check for javax↔jakarta adapter/wrapper classes: `grep -rn "class.*javax.*Adapter\|class.*Jakarta.*Wrapper" src/main/java/ */src/main/java/ 2>/dev/null --include="*.java" | tee -a ${ARTIFACTS_DIR}/phase-9-api-contract-check.txt`
5. If issues found: for `@JsonTypeInfo(use = Id.CLASS)` switch to `Id.NAME` with explicit `@JsonSubTypes`; for exception class names use string constants or ProblemDetail (REQ-P8-8.2)

**Verification:**
1. Verify artifact exists: `test -f ${ARTIFACTS_DIR}/phase-9-api-contract-check.txt && echo "PASS" || echo "FAIL: API contract check artifact missing"`
2. Count findings: `wc -l < ${ARTIFACTS_DIR}/phase-9-api-contract-check.txt`
3. If count > 0 → REVIEW REQUIRED: Review `${ARTIFACTS_DIR}/phase-9-api-contract-check.txt` and fix any contract-breaking patterns before proceeding.
4. If count = 0 → PASS: No contract-breaking patterns detected.

**Artifact:** `${ARTIFACTS_DIR}/phase-9-api-contract-check.txt`

---

## 9.4 Migration Documentation

**REQ-P9-6.1: Create Migration Summary**
**Applicability:** [ALWAYS]
**Action:** Create final migration report at `docs/spring-boot-migration/MIGRATION_SUMMARY.md`.

**Steps:**
1. Create `docs/spring-boot-migration/MIGRATION_SUMMARY.md` with the following sections populated from actual migration data:
   - **Version Upgrades** — table (Component | Before | After) for JDK, Spring Boot, Spring Framework, Hibernate
   - **Test Results** — table (Metric | Before Phase 0 | After Phase 9 | Status) for total tests, passing, duration, coverage, iterations
   - **Key Changes** — list of all migration changes applied (namespace count, security, config properties, dependencies, test shortcuts = 0)
   - **Migration Outcome** — checklist: all tests passing, app starts, no regressions, performance stable, no test shortcuts, iterations within limit
   - **Rollback** — use `git revert` or restore from a known-good checkpoint. Do NOT use `git reset --hard`.
   - **Post-Migration Tasks** — immediate (monitor logs, test with production data, verify integrations) and 1-2 week follow-ups
   - **References** — Spring Boot 3.0 Migration Guide, Spring Security 6.x Migration, internal guide version

**Verification:**
1. Verify file exists: `test -f docs/spring-boot-migration/MIGRATION_SUMMARY.md && echo "PASS: Migration summary exists" || echo "FAIL: Migration summary missing"`
2. Verify required sections are present:
   ```
   grep -q "Version Upgrades" docs/spring-boot-migration/MIGRATION_SUMMARY.md && echo "PASS: Section 'Version Upgrades' present" || echo "FAIL: Section 'Version Upgrades' missing"
   grep -q "Test Results" docs/spring-boot-migration/MIGRATION_SUMMARY.md && echo "PASS: Section 'Test Results' present" || echo "FAIL: Section 'Test Results' missing"
   grep -q "Key Changes" docs/spring-boot-migration/MIGRATION_SUMMARY.md && echo "PASS: Section 'Key Changes' present" || echo "FAIL: Section 'Key Changes' missing"
   grep -q "Migration Outcome" docs/spring-boot-migration/MIGRATION_SUMMARY.md && echo "PASS: Section 'Migration Outcome' present" || echo "FAIL: Section 'Migration Outcome' missing"
   ```

**Artifact:** `docs/spring-boot-migration/MIGRATION_SUMMARY.md`

---

## 9.5 Pull Request Creation

**REQ-P9-7.1: Create Pull Request**
**Applicability:** [ALWAYS]
**Action:** Create a comprehensive pull request for code review.

**Steps:**
1. Use this PR title:
   ```
   [Migration] Spring Boot 2.7.x → ${TARGET_SPRING_BOOT_VERSION} + JDK 11 → ${TARGET_JDK_VERSION}
   ```
2. Generate the PR description from `docs/spring-boot-migration/MIGRATION_SUMMARY.md` and actual migration data. Must include:
   - **Summary** — files changed, lines added/removed, test count/status, iteration count
   - **Version Upgrades** — table (Component | Before | After)
   - **Key Changes** — namespace migration count, security updates, config properties, test status, no shortcuts
   - **Verification** — build/test/startup/performance status
   - **Documentation** — link to `docs/spring-boot-migration/MIGRATION_SUMMARY.md`
   - **Rollback** — use `git revert` (or restore from a known-good checkpoint). Do NOT use `git reset --hard`.
   - **Post-Merge Actions** — monitor logs, test with production data, verify integrations
3. Push the migration branch: `git push origin <migration-branch>`
4. Create the PR via your Git provider (GitHub/GitLab/Bitbucket)

**Verification:**
```
git log --oneline -1
# Confirm PR exists in your Git provider UI
```

**Artifact:** N/A — PR is created in Git provider

---

### REQ-P9-SG: Stop Gate Verification

**MANDATORY — Do NOT proceed to PR review and deployment until completed.**

**Action:** Verify all Phase 9 artifacts exist and have valid content, then commit everything together.

**Inline Verification (run each command — ALL must pass):**

1. Check mandatory artifacts:
   ```
   ls ${ARTIFACTS_DIR}/phase-9-compile-iteration-*.log >/dev/null 2>&1 && echo "PASS: phase-9-compile-iteration-*.log" || echo "FAIL: missing phase-9-compile-iteration-*.log"
   ls ${ARTIFACTS_DIR}/phase-9-test-iteration-*.log >/dev/null 2>&1 && echo "PASS: phase-9-test-iteration-*.log" || echo "FAIL: missing phase-9-test-iteration-*.log"
   ls ${ARTIFACTS_DIR}/phase-9-integration-iteration-*.log >/dev/null 2>&1 && echo "PASS: phase-9-integration-iteration-*.log" || echo "FAIL: missing phase-9-integration-iteration-*.log"
   test -f ${ARTIFACTS_DIR}/phase-9-final-validation.log && echo "PASS: phase-9-final-validation.log" || echo "FAIL: missing phase-9-final-validation.log"
   test -f ${ARTIFACTS_DIR}/phase-9-dependency-tree-after.txt && echo "PASS: phase-9-dependency-tree-after.txt" || echo "FAIL: missing phase-9-dependency-tree-after.txt"
   test -f ${ARTIFACTS_DIR}/phase-9-dependency-tree-diff.txt && echo "PASS: phase-9-dependency-tree-diff.txt" || echo "FAIL: missing phase-9-dependency-tree-diff.txt"
   test -f ${ARTIFACTS_DIR}/phase-9-dependency-comparison.md && echo "PASS: phase-9-dependency-comparison.md" || echo "FAIL: missing phase-9-dependency-comparison.md"
   test -f docs/spring-boot-migration/MIGRATION_SUMMARY.md && echo "PASS: MIGRATION_SUMMARY.md" || echo "FAIL: missing MIGRATION_SUMMARY.md"
   ```
2. Check applicability file exists (proves all conditional detection steps were run):
   ```
   test -f ${ARTIFACTS_DIR}/phase-9-applicability.md && echo "PASS: phase-9-applicability.md" || echo "FAIL: missing phase-9-applicability.md"
   ```
3. Check conditional artifacts based on applicability results:
   - Check REQ-P9-4.2: `grep -q "REQ-P9-4.2" ${ARTIFACTS_DIR}/phase-9-applicability.md && echo "PASS: REQ-P9-4.2 detection recorded" || echo "FAIL: REQ-P9-4.2 detection missing from applicability file"`
   - Check REQ-P9-5.4: `grep -q "REQ-P9-5.4" ${ARTIFACTS_DIR}/phase-9-applicability.md && echo "PASS: REQ-P9-5.4 detection recorded" || echo "FAIL: REQ-P9-5.4 detection missing from applicability file"`
     - If applicability file contains "REQ-P9-5.4" + "APPLICABLE" (not "NOT APPLICABLE"): verify `test -f ${ARTIFACTS_DIR}/phase-9-api-contract-check.txt && echo "PASS" || echo "FAIL: API contract check was APPLICABLE but phase-9-api-contract-check.txt missing"`
     - If applicability file contains "REQ-P9-5.4" + "NOT APPLICABLE": phase-9-api-contract-check.txt not required — OK
4. Content validation:
   ```
   grep -qE "BUILD SUCCESS|Tests run:" ${ARTIFACTS_DIR}/phase-9-final-validation.log && echo "PASS: Final validation has build/test output" || echo "FAIL: Final validation may be placeholder"
   ```
5. Clean working directory check: `git status --porcelain docs/spring-boot-migration/`
   - If unexpected files appear, STOP and investigate before staging

**If ALL checks pass:**
```
# Write stop gate log
echo "ALL CHECKS PASSED - Phase 9 at $(date -u)" > ${LOG_DIR}/phase-9-stopgate.log

# Review working tree (must only contain intended phase changes)
git status --porcelain
# If unexpected files appear, STOP and investigate before staging

# Stage and commit all phase work
git add docs/spring-boot-migration/
git add -A
git commit -m "[phase-9] Complete Phase 9: Build & Test"
```

**If ANY check fails:** Fix the issue and re-run the verification commands. Do NOT proceed until all pass.

**Artifacts Required:**
- `${ARTIFACTS_DIR}/phase-9-applicability.md` (ALWAYS — contains detection proof for REQ-P9-4.2 and REQ-P9-5.4)
- `${ARTIFACTS_DIR}/phase-9-compile-iteration-*.log`
- `${ARTIFACTS_DIR}/phase-9-test-iteration-*.log`
- `${ARTIFACTS_DIR}/phase-9-integration-iteration-*.log`
- `${ARTIFACTS_DIR}/phase-9-final-validation.log`
- `${ARTIFACTS_DIR}/phase-9-dependency-tree-after.txt`
- `${ARTIFACTS_DIR}/phase-9-dependency-tree-diff.txt`
- `${ARTIFACTS_DIR}/phase-9-dependency-comparison.md`
- `${ARTIFACTS_DIR}/phase-9-api-contract-check.txt` (only if REQ-P9-5.4 APPLICABLE per applicability file)
- `docs/spring-boot-migration/MIGRATION_SUMMARY.md`
- `${ARTIFACTS_DIR}/phase-9-performance-comparison.md` (optional)

**Decision:** Migration COMPLETE - Proceed to PR review and deployment

# APPENDIX

---

## APPENDIX A: Common Migration Issues

> **Reference** — Consult this appendix during Phase 9 test fixing. Not required during Phases 0-8.

**Use this reference during migration to identify and resolve common Spring Boot 2.x → 3.x issues. Referenced throughout the migration guide.**

### **A.1 Test Context & Configuration Issues**

| Issue | Error Pattern | Resolution |
|-------|---------------|------------|
| Missing Spring Boot configuration | `Unable to find @SpringBootConfiguration` | Add `@SpringBootTest(classes = Application.class)` to test class |
| Bean creation failures | `BeanCreationException: Error creating bean...` | Review bean configurations for Spring Boot 3.x compatibility; check constructor injection vs field injection |
| Namespace imports in tests | `ClassNotFoundException: javax.*` | Replace `javax.*` imports with `jakarta.*` in test files |
| JUnit 4 to JUnit 5 migration | `@RunWith`, `@Before`, `@After` not found | Migrate to JUnit 5: `@ExtendWith(SpringExtension.class)`, `@BeforeEach`, `@AfterEach` |
| Test property source issues | `Could not resolve placeholder` | Verify `@TestPropertySource` or `application-test.yml` configuration |

### **A.2 Spring Security Test Changes**

| Issue | Error Pattern | Resolution |
|-------|---------------|------------|
| CSRF required in MockMvc tests | `403 Forbidden`, `401 Unauthorized` on POST/PUT/DELETE | Add `.with(csrf())` to MockMvc request: `mockMvc.perform(post(...).with(csrf()))` |
| Security config in tests | `WebSecurityConfigurerAdapter` not found | Replace with `@Bean SecurityFilterChain` pattern in test configuration |
| Request matcher API change | `antMatchers()` method not found | Replace `.antMatchers()` with `.requestMatchers()` |
| Authorization API change | `.authorizeRequests()` deprecated | Use `.authorizeHttpRequests()` instead |

### **A.3 JPA/Hibernate Test Issues**

| Issue | Error Pattern | Resolution |
|-------|---------------|------------|
| Schema generation failures | `Schema validation failed`, `Table not found` | Update `@Entity` mappings; verify `spring.jpa.hibernate.ddl-auto` setting |
| HQL/JPQL query syntax errors | `QuerySyntaxException`, `unexpected token` | Update queries for Hibernate 6.x syntax changes |
| Collection initialization errors | `NullPointerException` on entity collections | Initialize collections in entity: `private List<Item> items = new ArrayList<>();` |
| ID generation strategy issues | `IdentifierGenerationException` | Explicitly define `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| Criteria API changes | `createCriteria()` not found | Use JPA Criteria API instead of deprecated Hibernate API |
| Test database dialect | `Dialect resolution failed` | Explicitly set: `spring.jpa.database-platform=org.hibernate.dialect.H2Dialect` |

### **A.4 API & Method Signature Changes**

| Issue | Error Pattern | Resolution |
|-------|---------------|------------|
| Removed Spring methods | `NoSuchMethodError` | Consult Spring Boot 3.x migration guide for replacement API |
| Changed method signatures | `NoSuchMethodError` (wrong parameters) | Update method calls to match new Spring Framework 6.x signatures |
| Mockito verification failures | `Wanted but not invoked`, `Argument mismatch` | Update to Mockito 5.x verification syntax; check argument matchers |
| Jackson serialization changes | JSON serialization/deserialization errors | Update DTOs or add `@JsonIgnoreProperties(ignoreUnknown = true)` |
| RestTemplate/WebClient changes | API not found, deprecated warnings | Update to new client API or migrate deprecated methods |

### **A.5 Application Startup Issues**

| Issue | Error Pattern | Resolution |
|-------|---------------|------------|
| Missing jakarta namespace | `ClassNotFoundException: javax.servlet.*`, `javax.persistence.*` | Search entire codebase for missed `javax.*` imports and replace with `jakarta.*` |
| Auto-configuration failures | `BeanCreationException` during startup | Review `@EnableAutoConfiguration` exclusions; check dependency compatibility |
| Port already in use | `Port 8080 is already in use` | Kill existing process: `netstat -ano | findstr :8080` then `taskkill /PID <pid> /F` (Windows) |
| Database connection failures | `Cannot create PoolableConnectionFactory` | Verify database configuration; check driver compatibility with Hibernate 6.x |
| Actuator endpoint errors | `404 Not Found` on `/actuator/*` | Verify `management.endpoints.web.exposure.include` configuration; some endpoints renamed (e.g., `httptrace` → `httpexchanges`) |
| Bean circular dependency | `The dependencies of some beans...form a cycle` | Refactor bean dependencies or use `@Lazy` injection |

### **A.6 Additional Common Issues**

| Issue | Error Pattern | Resolution |
|-------|---------------|------------|
| Dependency version conflicts | `NoSuchMethodError`, `ClassNotFoundException` at runtime | Run `mvn dependency:tree` and resolve version conflicts; ensure Spring Boot BOM manages versions |
| Test resource loading issues | `FileNotFoundException`, resource not found | Check `src/test/resources` path; verify classpath configuration |
| Testcontainers compatibility | `Testcontainers` startup failures | Verify Testcontainers version >= 1.17.0; use `@ServiceConnection` for auto-configuration (Spring Boot 3.1+) |

### **A.7 Extended Troubleshooting Guide**

| Issue | Cause | Solution |
|-------|-------|----------|
| **Internal dependency not found** | Maven repo access issue | Check corporate Maven settings, verify network access |
| **Internal starter version mismatch** | Outdated starter version | Use Phase 4 resolution strategy: exact → minor → greater |
| **Bytecode check fails** | JAR not accessible | Mark for test in Phase 9, verify during build |
| **JobBuilderFactory not found** | Spring Batch 5.0 removed factory | Use `new JobBuilder(name, jobRepository)` directly |
| **StepBuilderFactory not found** | Spring Batch 5.0 removed factory | Use `new StepBuilder(name, jobRepository)` directly |
| **HttpMethod cannot be used in switch** | HttpMethod is now a class, not enum | Use if-else with `.equals()` instead |
| **CommonsMultipartResolver not found** | Removed in Spring Framework 6.0 | Use StandardServletMultipartResolver (auto-configured) |
| **@TypeDef not found** | Removed in Hibernate 6 | Use `@JdbcTypeCode(SqlTypes.*)` instead |
| **spring.redis.* properties not working** | Namespace moved | Change to `spring.data.redis.*` |
| **spring.config.use-legacy-processing error** | Property removed in 3.0 | Remove property, use `spring.config.import` |
| **Log timestamp format changed** | ISO 8601 default in 3.0 | Configure `logging.pattern.dateformat` to revert if needed |
| **Image banner not displaying** | Image banners removed in 3.0 | Use `banner.txt` text banner instead |
| **Spring Cloud Sleuth ClassNotFoundException** | Sleuth deprecated | Migrate to Micrometer Tracing |

---

## APPENDIX B: Transient Failure Patterns Reference Guide

> **Reference** — Consult during Phase 1 (transient failure fixing) and Phase 9 (test fixing).

**Purpose:** Pattern matching guide to categorize and fix transient test failures.

---

**Pattern Category 1: Timeout-Related Failures**

**Indicators:** `SocketTimeoutException`, `@Timeout` failures, `await().atMost()` timeout

**Fix Strategy:**
```
// BEFORE
@Test
void testExternalApi() {
    Response response = httpClient.call("https://api.example.com", Duration.ofSeconds(5));
    assertEquals(200, response.status());
}

// AFTER - Environment-aware timeout
@Test
void testExternalApi() {
    Duration timeout = System.getenv("CI") != null
        ? Duration.ofSeconds(30)
        : Duration.ofSeconds(5);
    Response response = httpClient.call("https://api.example.com", timeout);
    assertEquals(200, response.status());
}
```

---

**Pattern Category 2: Infrastructure-Related Failures**

**Indicators:** `BindException: Port already in use`, `Connection refused`, `FileNotFoundException`

**Fix Strategy:**
```
// BEFORE - Fixed port
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = "server.port=8080")
public class IntegrationTest {
    // Fails if port 8080 in use
}

// AFTER - Random port
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class IntegrationTest {
    @LocalServerPort
    private int port; // Uses random available port
}
```

---

**Pattern Category 3: Flaky Test Failures**

**Indicators:** Random order failures, intermittent failures, test isolation issues

**Fix Strategy:**
```
// BEFORE - Brittle timing
@Test
void testAsyncOperation() {
    asyncService.processData();
    Thread.sleep(1000); // Brittle - might fail
    assertEquals("PROCESSED", asyncService.getStatus());
}

// AFTER - Proper waiting
@Test
void testAsyncOperation() {
    asyncService.processData();
    await().atMost(Duration.ofSeconds(5))
           .until(() -> asyncService.getStatus().equals("PROCESSED"));
    assertEquals("PROCESSED", asyncService.getStatus());
}
```

---

**Pattern Category 4: Environment Dependencies**

**Timezone Independence:**
```
@BeforeAll
static void setTimezone() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
}
```

**Fixed Clock:**
```
@Test
void testExpirationLogic() {
    Clock fixedClock = Clock.fixed(
        Instant.parse("2024-01-01T10:00:00Z"),
        ZoneOffset.UTC
    );
    Token token = new Token(fixedClock);
    token.setExpiresAt(LocalDateTime.now(fixedClock).plusHours(1));
    assertTrue(token.isValid(fixedClock));
}
```

**Locale Independence:**
```
@BeforeAll
static void setLocale() {
    Locale.setDefault(Locale.US);
}
```

**Path Independence:**
```
// BEFORE - Platform-dependent
String path = "config/application.properties"; // Fails on Windows

// AFTER - Platform-independent
Path path = Paths.get("config", "application.properties");
```

---

**Pattern Category 5: External Infrastructure Dependencies**

**Embedded Kafka:**
```
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"test-topic"})
public class KafkaIntegrationTest {
    // Kafka runs in-memory for tests
}
```

**H2 In-Memory Database:**
```
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
public class RepositoryTest {
    // H2 runs in-memory, no external dependency
}
```

**MockRestServiceServer:**
```
@SpringBootTest
public class ExternalApiTest {
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testExternalCall() {
        mockServer.expect(requestTo("https://api.example.com/data"))
                  .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        String result = restTemplate.getForObject("https://api.example.com/data", String.class);
        assertEquals("{\"status\":\"ok\"}", result);
        mockServer.verify();
    }
}
```

**Testcontainers:**
```
@SpringBootTest
@Testcontainers
public class DatabaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

**Pattern Category 6: Test Logic Issues**

**Incorrect Assertions:**
```
// BEFORE
assertEquals("john", user.getName()); // Wrong - email ≠ name

// AFTER
assertEquals("john@example.com", user.getEmail());
assertNotNull(user.getId());
```

**Wrong Expected Values:**
```
// BEFORE
assertEquals(8.5, result); // Wrong - 100 * 0.08 = 8.0

// AFTER
assertEquals(8.0, result, 0.001); // Correct with delta
```

---

## APPENDIX C: Session Resumption Protocol

> **Reference** — Use only if resuming a migration from a previous session.

If resuming from a broken or interrupted session:
1. Check the last completed phase: `ls docs/spring-boot-migration/verification-logs/phase-*-stopgate.log`
2. The highest-numbered stop gate log indicates the last completed phase
3. Resume from the NEXT phase (e.g., if `phase-3-stopgate.log` exists, resume at Phase 4)
4. Verify the last phase's artifacts are intact before proceeding
5. Re-read the current phase's requirements before executing

## APPENDIX D: Rollback Strategy

> **Reference** — Use only if rollback is needed.

**Per-Phase Rollback:** `git log --oneline | grep "\[phase-"` to find commits, then `git revert <commit-hash>` to rollback.

**Emergency Full Rollback:** `git checkout <original-branch>` to return to pre-migration state.

**Clean State Verification:** All phases verify `git status --porcelain` returns empty before proceeding. Uncommitted changes block the stop gate.

**END OF APPENDIX**

---
