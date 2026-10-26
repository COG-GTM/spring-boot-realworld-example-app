---
name: clm-scan-fix
description: Remediate blocking CLM (Sonatype IQ policy) violations in a Maven/Spring Boot service by pinning patched dependency versions, keeping the framework major version unchanged. Use when .gs/clm-report.json shows policyAction FAIL or a pipeline fails at the CLM stage.
---

# CLM scan fix

Goal: clear every `FAIL` violation in `.gs/clm-report.json` with the **smallest** version bumps that
stay inside the current framework major. Never upgrade Spring Boot / JDK here — that is a different
playbook with its own review.

## Footprint
`pom.xml` (properties / dependencyManagement only), Java files that must change because a bumped
library changed its API, `README.md` (if versions are documented).

## Steps
1. Read `.gs/clm-report.json`. List each violation with `action: FAIL` -> component, CVE,
   `remediation`. Ignore `WAIVED`; mention `WARN` in the PR but do not block on it.
2. Create branch `platform/01-clm-fix` from the base branch given to you.
3. For each FAIL violation prefer, in order:
   - Overriding the Spring Boot managed version property in `<properties>`
     (e.g. `<snakeyaml.version>`, `<tomcat.version>`, `<spring-framework.version>`).
   - Bumping the explicit `<version>` of a directly declared dependency.
   - Adding a `<dependencyManagement>` entry for a transitive dependency.
   Pick the lowest version that satisfies the remediation text and still exists on Maven Central.
4. Known API fallout to check:
   - SnakeYAML 2.x: `new Yaml()` on untrusted input must become
     `new Yaml(new SafeConstructor(new LoaderOptions()))`; `Yaml.loadAs` of app classes needs a
     `Constructor(Class, LoaderOptions)`. Spring Boot 2.7.11+ is compatible with SnakeYAML 2.x.
   - commons-text 1.10+: `StringSubstitutor.createInterpolator()` no longer enables `script:`,
     `dns:`, `url:` lookups; plain `new StringSubstitutor(map)` is unaffected.
   - Tomcat 9.0.86+ / Spring 5.3.34+: no source changes expected; rerun the full test suite.
5. Validate: `mvn -B -ntp clean verify` with the JDK in `.java-version`, then
   `mvn dependency:tree -Dincludes=<groupId>:<artifactId>` for each fixed component to prove the
   resolved version.
6. Do **not** hand-edit `.gs/clm-report.json` — the pipeline regenerates it. In the PR description
   add a table: violation id, component, old -> new version, CVE.
7. Commit as `PLAT-UPG bug: remediate CLM violations <ids>` and open a PR titled
   `[platform 1/N] CLM remediation: <components>`.

## Done when
All FAIL violations have a pinned fix, `mvn verify` is green, PR is open, and you have reported the
branch, PR URL, the list of bumped components, and the test summary.
