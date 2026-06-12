---
name: cobol-modernization-orchestrated-execution
description: "Converted from Devin playbook: COBOL Modernization Orchestrated Execution"
triggers:
  - user
  - model
---

# COBOL Modernization Orchestrated Execution

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: COBOL Modernization Orchestrated Execution (playbook-3dc012f9a9694f7db3e6de9499291e2f), macro `!cobol_modernization_orchestrated_execution`

## Procedure

## Overview

Orchestrate the full COBOL modernization by reading the Jira backlog and Confluence artifacts produced by `!cobol-discover`, determining the correct execution order based on copybook dependencies and JCL job chains, and fanning out child Devin sessions in waves. This playbook turns Devin into an autonomous program manager for COBOL-to-modern migration: it reads the plan, sequences the work, delegates to specialist child sessions, monitors progress, and reports results.

## What's Needed From User

- **Jira project key** — the project containing migration tickets (e.g. "CD" at https://cog-gtm.atlassian.net/jira/software/projects/CD/boards/1355)
- **Confluence space** — the space containing discovery outputs (e.g. "CD" at https://cog-gtm.atlassian.net/wiki/spaces/CD/overview)
- **Source repository** — the COBOL repo being modernized
- **Target repository** — the repo where modernized code lands (can be the same repo if porting in-place)

## Procedure

### Phase 1: Read the Plan

1. **Ingest the Jira backlog.** Query the Jira project (https://cog-gtm.atlassian.net/jira/software/projects/CD/boards/1355) for all open tickets under the COBOL modernization Epic. For each ticket, capture: key, summary, description, labels, status, acceptance criteria, referenced COBOL files, target language/framework, and linked Confluence pages. Exclude tickets already in "Done" status.

2. **Read the Confluence artifacts.** Fetch the following pages from the Confluence space (https://cog-gtm.atlassian.net/wiki/spaces/CD/overview):
   - **Modernization Roadmap** — for the recommended porting order and timeline
   - **COBOL Program Inventory & Dependency Graph** — for cross-program and copybook dependencies that dictate sequencing
   - **Proposed Modernization Boundaries** — for the mapping of COBOL programs to target modern components
   - **Behavioral Baseline** — for record layouts, business rule catalog, sample I/O, and test counts that every child session must validate against

3. **Classify each ticket.** Based on the ticket's labels and content:
   - Tickets labeled `porting` describing COBOL program modernization → child session will port the specific COBOL program to the target stack, preserving byte-accurate record parsing, all business rules, exact error messages, and counter semantics
   - Tickets labeled `foundation` (scaffolding, CI, test infrastructure, shared copybook models) → child session will implement the infrastructure component
   - Tickets labeled `integration` (web service wrappers, API endpoints, database persistence, end-to-end validation) → child session will build the integration layer

### Phase 2: Plan the Execution Order

4. **Build the dependency graph from COBOL structure.** Using the Confluence Program Inventory and copybook dependency map:
   - **Shared copybook models must be ported first** — if multiple COBOL programs COPY the same copybook, the modern data model for that copybook must exist before any program that uses it can be ported
   - **Programs that share VSAM files or MQ queues** cannot be ported simultaneously — the shared data store creates an implicit dependency
   - **JCL job chains define execution order** — if JCL runs Program A before Program B, and B reads A's output, then A must be ported first
   - **Leaf programs** (those with no inbound dependencies from other programs being ported) can be ported in parallel

5. **Group tickets into execution waves.** Each wave contains tickets that are independent of each other and whose dependencies from prior waves are satisfied:
   - **Wave 1 (Foundation):** Project scaffolding, CI pipelines, shared copybook data models (dataclasses/POJOs for common copybooks), test infrastructure. These are independent and run in parallel.
   - **Wave 2 (Leaf Program Ports):** Port COBOL programs that have no inbound dependencies from other programs being modernized. Each gets its own child session. These run in parallel.
   - **Wave 3 (Dependent Program Ports):** Port programs that depend on Wave 2 outputs (e.g., programs that read files produced by Wave 2 programs). These run in parallel within the wave.
   - **Wave 4 (Core Program Ports):** Port the most-coupled programs — those that share copybooks, VSAM files, or MQ queues with multiple other programs.
   - **Wave 5 (Integration):** Web service wrappers, API endpoints, database persistence, message queue adapters, end-to-end validation against legacy output. These need ported programs to exist.

6. **Present the execution plan to the user.** Send a message showing:
   - The wave structure with ticket assignments
   - Which COBOL programs are ported in each wave and why (dependency reasoning)
   - Estimated parallelism (how many child sessions per wave)
   - Any tickets excluded and why
   
   **Wait for the user to confirm before proceeding.** The user may want to adjust wave assignments, prioritize certain programs, or change the target stack for specific tickets.

### Phase 3: Execute Waves

7. **Execute each wave sequentially.** For each wave, starting with Wave 1:

   a. **Create child sessions for all tickets in the wave.** For each ticket, create a child Devin session with a self-contained prompt including:
      - The Jira ticket key and full description
      - The source COBOL file(s) to port (these are the authoritative spec)
      - The copybooks used by those programs, with complete field-level byte layouts from the Behavioral Baseline
      - Every business rule to implement (from the business rule catalog), with the COBOL paragraph reference
      - Exact error message strings to preserve (quoted verbatim, including embedded apostrophes)
      - Composite key construction formulas
      - Counter/statistics variables to preserve
      - Input record format and byte length, output record format and byte length
      - Target language/framework
      - Sample input/output data for regression testing (if available from the Behavioral Baseline)
      - Acceptance criteria: unit tests for every business rule, output format compliance test, counter semantics test
      - Links to Confluence pages for additional context
   
   b. **Wait for all child sessions in the wave to complete.** Use `devin_session_gather` to block until all sessions settle.
   
   c. **Inspect results.** For each completed child session:
      - Check if a PR was created
      - Verify the session addressed all business rules listed in the ticket
      - Check for any reported failures or blockers
   
   d. **Report wave results to the user.** Send a summary:
      - Which tickets completed successfully (with PR links)
      - Which tickets failed (with failure reasons)
      - Whether it's safe to proceed to the next wave
   
   e. **Gate check before next wave.** Only proceed to the next wave if:
      - All critical tickets in the current wave completed successfully, OR
      - The user explicitly approves proceeding despite failures
      - If a foundation ticket failed (Wave 1), do NOT proceed to porting — the shared models and infrastructure must be stable first

8. **Repeat Step 7 for each subsequent wave** until all waves are complete or the user halts execution.

### Phase 4: Report

9. **Compile the final status report.** After all waves complete, produce:
   - **Execution summary table:** Ticket Key | COBOL Program | Target Stack | Wave | Status | PR URL
   - **Business rule coverage:** How many rules were implemented and tested across all child sessions
   - **Tickets completed / failed / skipped** with counts and details
   - **Total child sessions created and total PRs opened**
   - **Recommendations:** Merge order for PRs, any manual follow-up needed, any programs that need re-porting

10. **Update the Confluence Modernization Roadmap.** Add an "Execution Log" section to the roadmap page (https://cog-gtm.atlassian.net/wiki/spaces/CD/overview) documenting: which tickets were completed, PR links, timestamps, and any issues encountered.

11. **Update Jira tickets.** For any tickets whose child sessions completed but whose Jira status wasn't updated, update the ticket status in the Jira project (https://cog-gtm.atlassian.net/jira/software/projects/CD/boards/1355) and add a comment with the outcome and PR link.

## Child Session Prompt Template

When creating child sessions for COBOL porting tickets, each prompt must include:

```
Jira Ticket: [TICKET-KEY]
Summary: [ticket summary]

Source (authoritative spec): [repo]/[path to .cbl file]
Copybooks: [list of .cpy files used, with byte layouts]
Target: [language/framework]

Record Layouts (from Behavioral Baseline):
- Input: [record name], [byte length]
  [field table: name | offset | length | PIC type]
- Output: [record name], [byte length], [format description]

Business Rules (from COBOL paragraph [paragraph name]):
- Rule 1: IF [condition] THEN [action] — error message: "[exact string]"
- Rule 2: ...

Composite Key: [construction formula, e.g., CORP|CUSTOMER|JOB|]
Counters: [list of counters to preserve]

Sample Data:
- Input: [path to sample input file]
- Expected Output: [path to expected output file]

Acceptance Criteria:
- Unit test for each business rule (independent, non-exclusive)
- Output record length compliance test
- Counter semantics match legacy program
- Error messages character-for-character identical to COBOL source

Confluence References:
- Behavioral Baseline: [page URL]
- Program Inventory: [page URL]
```

## Specifications

- All tickets must be classified and wave-ordered before execution begins
- Tickets are executed in dependency-ordered waves — never port a dependent program before its prerequisites
- Each child session receives a self-contained prompt with all COBOL-specific context (byte layouts, business rules, error messages)
- The user must confirm the execution plan before any child sessions are created
- Wave results are reported to the user after each wave completes
- Execution halts if foundation (Wave 1) tickets fail — shared models must be stable before porting begins
- The user can halt execution at any wave boundary
- The COBOL source is always the authoritative spec passed to child sessions, not any existing port

## Advice and Pointers

- The Confluence Modernization Roadmap is the primary source of truth for execution order — it was produced by `!cobol-discover` and reviewed by the team
- Copybook dependencies are the key sequencing constraint for COBOL — if two programs share a copybook, the shared data model must be built in Wave 1 (foundation) before either program is ported
- For porting tickets, the most important context to include in the child session prompt is: the exact byte offsets from the copybooks, the exact error message strings from the COBOL source, and the exact counter variable names
- Always include sample input/output data in child session prompts when available — this gives the child session a concrete regression test
- If a child session takes longer than expected, check its status rather than terminating — COBOL porting with byte-accurate validation is complex
- Keep the user informed at every wave boundary — this is a long-running orchestration and visibility matters
- COBOL programs with REDEFINES are the hardest to port correctly — ensure the child session prompt explicitly documents the REDEFINES byte overlays

## Forbidden Actions

- Do not create child sessions before the user confirms the execution plan
- Do not proceed to porting waves if foundation tickets have failed — the shared data models must be stable
- Do not modify any code directly — all code changes happen in child sessions
- Do not skip the Confluence and Jira ingestion steps — the orchestrator must read the plan before executing it
- Do not run all tickets simultaneously without wave ordering — dependency violations will cause incorrect ports
- Do not terminate child sessions that are still making progress
- Do not pass an existing port (Python, Java) as the authoritative spec to child sessions — always pass the original COBOL source
- Do not skip including byte layouts and error message strings in child session prompts — these are the most common sources of porting errors

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
