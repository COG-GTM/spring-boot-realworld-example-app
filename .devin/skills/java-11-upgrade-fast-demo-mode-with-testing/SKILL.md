---
name: java-11-upgrade-fast-demo-mode-with-testing
description: "Converted from Devin playbook: Java 11 Upgrade — Fast Demo Mode (with Testing)"
triggers:
  - user
  - model
---

# Java 11 Upgrade — Fast Demo Mode (with Testing)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Java 11 Upgrade — Fast Demo Mode (with Testing) (playbook-5585a9bb93054e32bcec3de668dbd441)

## Procedure

# Java 11 Upgrade — Fast Demo Mode

## Overview

Speed-optimized Java 11 upgrade playbook for demo use. Exact changes are provided in the session prompt or in the "Java 11 Upgrade — Prescriptive Changes Per Repo" knowledge note — apply them directly without assessment. Compile-only verification, PR immediately, then end-to-end testing after.

## What's Needed From User

- Repository URL (provided in prompt)
- Jira ticket reference (provided in prompt)

<phase name="Apply Changes and Compile" id="1">
## Apply Changes and Compile

**SPEED IS CRITICAL.** Do not explore, assess, or review the codebase. Apply changes immediately.

1. Clone the repository if not already cloned.
2. Create a branch: `devin/$(date +%s)-upgrade-java11`
3. Read the session prompt for the **EXACT CHANGES** section. If exact changes are listed in the prompt, apply them directly. If not, reference the "Java 11 Upgrade — Prescriptive Changes Per Repo" knowledge note for this specific repository.
4. Open only the files that need modification (typically just `pom.xml` and 1-2 source files). Do NOT read or explore other files.
5. Apply ALL changes in a single editing pass. Do not make partial changes and test incrementally.
6. Run `mvn clean compile -T 2C` to verify compilation succeeds.
7. If compilation fails, read the error output, fix the specific issue, and retry. Maximum 2 retry attempts.

Only ever create the todo list for the current phase.

<verification>
- All specified pom.xml changes have been applied
- All specified source code changes have been applied (if any)
- `mvn clean compile` succeeds with zero errors
</verification>
</phase>

<phase name="Create PR" id="2">
## Create PR

1. Stage and commit all changes with message: `chore: upgrade to Java 11`
2. Push the branch to the **forked repo** (e.g., `COG-GTM/<REPO_NAME>`). Never push to the source/upstream repo.
3. Create a PR using the git_create_pr tool with `--repo COG-GTM/<REPO_NAME>`. Title: `Upgrade to Java 11: <repo_name>`
4. Report the PR URL to the user immediately.
5. Include in the report: Jira ticket reference, list of changes made, compilation status.

Only ever create the todo list for the current phase.

<verification>
- PR has been created on the correct forked repo
- PR URL has been reported to the user
- Jira ticket is referenced in the PR or report
</verification>
</phase>

<phase name="End-to-End Testing" id="3">
## End-to-End Testing

Now that the PR is created, verify the upgrade works end-to-end.

1. Run the full test suite if tests exist: `mvn clean test -T 2C`
   - If no Java test classes exist in the project, note that and skip to step 3.
2. If tests fail, fix the failures and push updated commits to the PR branch.
3. Run `mvn clean package -T 2C` to produce the final artifact (JAR/WAR).
4. If the application can be started (e.g., Spring Boot, Dropwizard), start it and verify it boots successfully:
   - For Dropwizard: `java -jar target/<artifact>.jar server <config>.yml`
   - For Spring Boot: `java -jar target/<artifact>.jar`
   - Verify the application starts without errors and responds to a basic health/endpoint check.
5. Report testing results to the user: tests passed/failed, artifact built, application boots.

Only ever create the todo list for the current phase.

<verification>
- Tests have been run (or confirmed no tests exist)
- Final artifact (JAR/WAR) has been built successfully
- Application has been started and verified (if applicable)
- Testing results have been reported to the user
</verification>
</phase>

## Specifications

- Phase 1-2 target time: **under 4 minutes** (compile + PR)
- Phase 3 is follow-up testing after the PR already exists
- Apply changes exactly as specified — no additional changes, no dependency exploration
- Always push PRs to the forked repo (COG-GTM/<REPO_NAME>), never to the source repo

## Advice and Pointers

- The session prompt should contain an "EXACT CHANGES" section with line-by-line instructions for this specific repo
- If the prompt doesn't have exact changes, the "Java 11 Upgrade — Prescriptive Changes Per Repo" knowledge note has detailed per-repo instructions
- Speed is the #1 priority for Phases 1-2 — minimize thinking time, apply changes immediately
- For Maven builds, use `-T 2C` flag for parallel builds
- Most repos have NO Java tests, so the test phase will be quick
- For Dropwizard apps, the server command needs a YAML config file (usually `example.yml` or `config.yml`)

## Forbidden Actions

- Do NOT assess, review, or explore the codebase before making changes
- Do NOT read files that are not being modified (during Phases 1-2)
- Do NOT upgrade dependencies beyond what is specified in the prompt/knowledge note
- Do NOT modify test assertions to make tests pass — fix the underlying issue
- Do NOT skip tests by adding @Ignore or @Disabled annotations

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
