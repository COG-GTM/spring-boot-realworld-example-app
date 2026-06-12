---
name: java-11-upgrade-discover-plan-and-execute-orchestrator
description: "Converted from Devin playbook: Java 11 Upgrade — Discover, Plan, and Execute Orchestrator"
triggers:
  - user
  - model
---

# Java 11 Upgrade — Discover, Plan, and Execute Orchestrator

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Java 11 Upgrade — Discover, Plan, and Execute Orchestrator (playbook-acc83ab6643343cdb9cf61a39c8c9059)

## Procedure

# Java 11 Upgrade — Discover, Plan, and Execute Orchestrator

## Overview

Discover which COG-GTM repositories need upgrading to Java **11** using the org knowledge base, create a Jira epic and child tickets for the top 3 priority repos, then spin up managed Devin sessions for each.

## CRITICAL RULES

**1. TARGET VERSION IS JAVA 11.** The target Java version is ALWAYS Java 11. NEVER Java 17, NEVER Java 21, NEVER any other version. Every ticket, every session, every prompt MUST say "Java 11". If the user's message says "Java version upgrade" without specifying a target, the target is Java 11.

**2. DO NOT include specific pom.xml changes in child session prompts.** Let the child sessions figure out the changes on their own using the attached playbook. Keep child prompts short and generic.

**3. Create sessions EXACTLY ONCE.** Call `devin_session_create` a single time with all sessions in one batch. Do NOT call it multiple times.

**4. ONLY UPGRADE 3 REPOS.** Even if the inventory lists more repos needing upgrades, only create tickets and sessions for the top 3 priority repos (lowest Java version first).

## Repository Exclusion List

**DO NOT create tickets or sessions for these repos — they are already on Java 11+:**
- Springboot-BankApp (Java 17)
- spring-boot-realworld-example-app (Java 11)
- demos-coghealth-ehr-api (Java 11)
- Aplicacion-de-Banca-Spring-Boot (Java 11)
- BankWS (Java 11)
- ph-ee-operations-app (Java 11)
- ph-ee-exporter (Java 11/17)
- FraudApplication (Java 17)
- Moneybag-BE (Java 17)
- CMS.gov-ab2d (Java 17)
- ph-ee-importer-es (Java 17)
- Securities-Trade-Processing-System (Java 21)
- NSA-ghidra (Java 21)
- ehrbase (Java 25)
- CMS.gov-beneficiary-fhir-data (Java 25)

If the user lists ANY of the above repos, silently exclude them.

<phase name="Discover Repositories" id="1">
## Phase 1: Discover Repositories

Look up the Java version inventory knowledge note to identify which COG-GTM repositories are currently on Java 9 and under.

Present the full list of repos that need upgrading to the user.

Then state that you will proceed with the **top 3 priority repos** (lowest Java version first):
1. COG-GTM/DropwizardEmployee (Java 6 → 11)
2. COG-GTM/Hadoop-Health-Insurance-ETL (Java 8 → 11)
3. COG-GTM/spring-boot-jpa-oracle-project (Java 8 → 11)

Only ever create the todo list for the current phase.

<verification>
- Java version inventory knowledge note has been referenced
- Full list of repos on Java 9 and under has been presented to user
- Top 3 priority repos identified (lowest Java version first)
- No repos from the exclusion list are included
</verification>
</phase>

<phase name="Create Jira Epic and Tickets" id="2">
## Phase 2: Create Jira Epic and Tickets

Use these pre-cached values directly. Do NOT call `getVisibleJiraProjects` or `getJiraProjectIssueTypesMetadata`:

- **Cloud ID**: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`
- **Project Key**: `AB`
- **Epic issue type**: `Epic`
- **Task issue type**: `Task`

### Step 1: Create the Epic

Call `createJiraIssue`:
```
cloudId: e395c468-f9ea-4f8f-adae-0ea6d2eb6970
projectKey: AB
issueTypeName: Epic
summary: "Java 11 Upgrade - Multi-Repo Initiative"
description: "Upgrade all targeted repositories to Java 11."
contentFormat: markdown
```

### Step 2: Create one Task per repository

For EACH of the 3 repos:
```
cloudId: e395c468-f9ea-4f8f-adae-0ea6d2eb6970
projectKey: AB
issueTypeName: Task
summary: "Java 11 Upgrade: <org/repo> (Java <current_version> → 11)"
description: "Upgrade <org/repo> from Java <current_version> to Java 11."
parent: <epic_key>
contentFormat: markdown
```

**REMINDER: The target is Java 11 in EVERY ticket summary and description.**

Only ever create the todo list for the current phase.

<verification>
- Epic has been created with "Java 11" in the title
- Exactly 3 Tasks created (one per repo)
- Each Task says "Java 11" (not Java 17 or any other version)
- All ticket keys captured for Phase 3
</verification>
</phase>

<phase name="Create Managed Devin Sessions" id="3">
## Phase 3: Create Managed Devin Sessions

Create ALL child sessions in a **single batch call**. Call `devin_session_create` **EXACTLY ONCE**.

For each repository/ticket:
- **playbook_id**: `playbook-9a5af3fe5a23418db91bb8933201846a`
- **tags**: `["java-11-upgrade", "<ticket_key>", "<repo_name>"]`

### Child Session Prompt (use this EXACT template — keep it short)

```
Jira ticket: <TICKET_KEY>

Upgrade <org/repo> from Java <current_version> to Java 11.

Repository: https://github.com/<org/repo>
Build tool: Maven

Follow the attached playbook to complete this upgrade. Target version: Java 11.
```

**DO NOT add specific pom.xml changes, DO NOT add "Specific Changes Required" sections, DO NOT reference knowledge notes in the prompt. Keep it short. The child playbook handles everything.**

Only ever create the todo list for the current phase.

<verification>
- devin_session_create was called EXACTLY ONCE (not multiple times)
- Each session prompt says "Java 11" (not Java 17)
- Each session prompt is SHORT (no specific file changes listed)
- Each session has playbook-9a5af3fe5a23418db91bb8933201846a attached
- Session URLs captured
</verification>
</phase>

<phase name="Report Results" id="4">
## Phase 4: Report Results

Post a summary:

```
**Jira Epic:** [<epic_key>](<epic_url>)

| Ticket | Repository | Devin Session |
|--------|-----------|---------------|
| [<key>](<url>) | <org/repo> | [Session](<session_url>) |
```

Only ever create the todo list for the current phase.

<verification>
- Summary posted with all links
- Epic, ticket, and session links are correct
</verification>
</phase>

## Specifications

- Target Java version: **11** (NEVER 17, NEVER 21)
- Only 3 repos maximum
- Child sessions use playbook `playbook-9a5af3fe5a23418db91bb8933201846a`
- Sessions created in a SINGLE batch call
- Child prompts are SHORT — no specific file changes
- Total orchestration time under 3 minutes

## Forbidden Actions

- Do NOT upgrade to any version other than Java 11
- Do NOT call `getVisibleJiraProjects` or `getJiraProjectIssueTypesMetadata`
- Do NOT create sessions one at a time
- Do NOT call `devin_session_create` more than once
- Do NOT include specific pom.xml changes in child prompts
- Do NOT reference knowledge notes in child prompts
- Do NOT create tickets for repos on the exclusion list
- Do NOT create tickets for more than 3 repos
- Do NOT run any code or clone repositories

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
