---
name: cobol-modernization-and-discovery-planning
description: "Converted from Devin playbook: COBOL Modernization and Discovery Planning"
triggers:
  - user
  - model
---

# COBOL Modernization and Discovery Planning

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: COBOL Modernization and Discovery Planning (playbook-0cf47f9516314a43b9ebc0dfbe839f8c), macro `!cobol-discover`

## Procedure

## Overview

Analyze a COBOL application repository to produce a comprehensive program inventory, copybook dependency map, proposed modernization boundaries with justifications, a behavioral baseline for evaluation gates, and a structured migration backlog with Jira tickets. All outputs are written to Confluence as durable, human-reviewable pages. This playbook is designed for mainframe COBOL modernization — batch programs, copybooks, JCL job streams, MQSeries integrations, VSAM files, CICS transactions, and DB2 access.

## What's Needed From User

- **Repository URL** — the COBOL repo to analyze
- **Target architecture context** (optional) — if an existing modern port or reference implementation exists (e.g., a Python or Java port already in the repo), provide it so proposed boundaries can be validated against working code
- **Jira project key** — the Jira project where migration tickets should be created (e.g. "CD" at https://cog-gtm.atlassian.net/jira/software/projects/CD/boards/1355)
- **Confluence space** — the Confluence space where discovery outputs should be written (e.g. "CD" at https://cog-gtm.atlassian.net/wiki/spaces/CD/overview)

## Procedure

### Phase 1: Inventory the COBOL Codebase

1. **Clone and inventory the repository.** Count and classify all files: COBOL programs (`.cbl`, `.cob`), copybooks (`.cpy`), JCL (`.jcl`), BMS maps (`.bms`), SQL/DDL (`.sql`), and any existing modern ports (`.py`, `.java`, `.cs`). Compute LOC for each COBOL program. Identify the build/compile system and any test infrastructure.

2. **Map every COBOL program.** For each program, document:
   - **Program ID** (from IDENTIFICATION DIVISION)
   - **COPY statements** — which copybooks it includes and what data structures they define
   - **File assignments** (SELECT...ASSIGN) — input/output files, VSAM files, MQ queues
   - **Record layouts** — every FD/01-level with PIC definitions, exact byte lengths, and REDEFINES relationships
   - **Processing paragraphs** — the paragraph call hierarchy (PERFORM chains) showing the execution flow
   - **Business rules** — every IF/EVALUATE block in validation/comparison paragraphs, with the exact condition logic
   - **Error messages** — exact literal strings (including embedded apostrophes like `TODAY''S`)
   - **Counters/statistics** — WORKING-STORAGE counters displayed at program completion
   
   Produce a table: Program | Copybooks | Input Files | Output Files | Business Rules Count | LOC.

3. **Map every copybook.** For each copybook, document:
   - **Record name** and total byte length
   - **Every field**: name, level number, byte offset (0-indexed), length, PIC type
   - **All REDEFINES** relationships — which fields overlay which byte ranges
   - **All 88-level condition names** and their values
   - **Which programs COPY it** — the usage graph
   
   Flag copybooks used by multiple programs — these are the shared data contracts of the mainframe system.

4. **Analyze cross-program dependencies.** Identify:
   - Programs that share copybooks (shared data structures)
   - Programs that read/write the same files or VSAM datasets (shared data stores)
   - Programs that communicate via MQSeries queues (message-based coupling)
   - JCL job streams that chain multiple programs (batch orchestration dependencies)
   - CICS transaction routing between programs (online coupling)
   
   For each dependency, record: Source Program → Target Program → Type (shared copybook, shared file, MQ queue, JCL chain, CICS call). Flag the highest-coupling points.

5. **Produce a dependency graph.** Format as an adjacency list showing directional dependencies between programs. Highlight circular dependencies and shared copybooks that create implicit coupling.

6. **Write Confluence page: "COBOL Program Inventory & Dependency Graph."** Create a page in the specified Confluence space (https://cog-gtm.atlassian.net/wiki/spaces/CD/overview) containing: LOC summary, technology stack (COBOL dialect, MQ version, VSAM/DB2, CICS), program inventory table, copybook inventory table, dependency adjacency list, and coupling hot spots.

### Phase 2: Propose Modernization Boundaries

7. **Propose modernization boundaries.** For each proposed modern component, document:
   - **Source COBOL programs** included
   - **Copybooks** that become data models (dataclasses, POJOs, structs)
   - **Business rules** to preserve (reference the specific COBOL paragraphs)
   - **I/O contracts** — input record format, output record format, byte lengths
   - **Target stack recommendation** (Python, Java/Spring Boot, web service, microservice) with justification
   - **Extraction order** — least-coupled programs first
   
   Flag programs that share copybooks or data stores — these may need to be ported together or require a shared library.

8. **Diff against existing ports (if provided).** If modern ports already exist in the repo (e.g., a Python implementation), compare the proposed boundaries against the existing port. Verify: Do the existing ports cover all business rules? Do the byte offsets match? Are the error messages identical? Note any discrepancies.

9. **Write Confluence page: "Proposed Modernization Boundaries."** Create a page containing: proposed component list with justifications, source-to-target mapping, extraction order recommendation, diff against existing ports (if applicable), and risk register.

### Phase 3: Capture Behavioral Baseline

10. **Capture the behavioral baseline.** This establishes what "correct" looks like for every modernized component:
   - **Record Layout Contracts:** For every input/output file, document the exact byte layout — field names, offsets, lengths, data types. These are the contracts that must be preserved.
   - **Business Rule Catalog:** For every validation/comparison paragraph, document each rule as: Condition → Action → Error Message. Include the exact COBOL source reference.
   - **Sample I/O Baseline:** If sample input data and expected output data exist in the repo, run any existing implementation (COBOL or port) and capture the exact output. This becomes the regression test.
   - **Test Suite Baseline:** If unit tests exist (e.g., for a Python port), run them and record: total tests, pass count, fail count.
   - **Counter Semantics:** Document what counters the program displays at completion (messages read, records written, etc.) — operations teams depend on these.

11. **Write Confluence page: "Behavioral Baseline."** Create a page in the Confluence space (https://cog-gtm.atlassian.net/wiki/spaces/CD/overview) containing: record layout contract tables, business rule catalog, sample I/O results, test suite counts, counter definitions. This page is the ground truth for every migration ticket.

### Phase 4: Design and Create Backlog

12. **Design a migration backlog.** Create ticket descriptions covering:
   - **Foundation tickets** — project scaffolding per target stack, CI pipelines, test infrastructure, shared library for common copybook models
   - **Porting tickets** — one per COBOL program to be modernized, specifying: source `.cbl` file, copybooks to model, business rules to implement (with COBOL paragraph references), record layouts with byte offsets, exact error message strings to preserve, target language/framework, acceptance criteria referencing the behavioral baseline
   - **Integration tickets** — web service wrappers, API endpoints, database persistence, message queue adapters, end-to-end validation against legacy output
   
   Each ticket must reference specific COBOL source files, target component name, acceptance criteria, and links to the relevant Confluence pages.

13. **Present the backlog to the user for confirmation.** Send a message listing all proposed tickets with titles, categories (foundation/porting/integration), descriptions, and referenced COBOL files. Wait for explicit approval before proceeding.

14. **Create Jira tickets.** After user confirmation, create each ticket in the specified Jira project (https://cog-gtm.atlassian.net/jira/software/projects/CD/boards/1355):
   - Create a parent Epic for the overall COBOL modernization effort.
   - Create each ticket as a Task linked to the Epic.
   - Apply labels: `cobol-modernization` on all tickets, plus `foundation`, `porting`, or `integration` to indicate category.
   - Each ticket description must include: the specific COBOL files, copybooks, and byte layouts, the target component, the business rules to preserve, the acceptance criteria, and links to the Confluence pages.

15. **Write Confluence page: "Modernization Roadmap."** Create a page containing: executive summary, porting order with timeline estimates, Jira ticket summary table with links, link to the Behavioral Baseline page, and the Jira board URL (https://cog-gtm.atlassian.net/jira/software/projects/CD/boards/1355).

### Phase 5: Validate

16. **Validate the outputs.** Verify every COBOL program and copybook is accounted for in at least one backlog ticket. Verify no circular dependencies exist between proposed modern components. Verify the extraction order respects the dependency graph. Verify all Jira tickets were created successfully. Verify the behavioral baseline covers every business rule. Verify all four Confluence pages were created successfully.

## Specifications

- All outputs are written to Confluence as separate pages in the specified space (https://cog-gtm.atlassian.net/wiki/spaces/CD/overview)
- Four Confluence pages are created: COBOL Program Inventory & Dependency Graph, Proposed Modernization Boundaries, Behavioral Baseline, Modernization Roadmap
- Every COBOL program and copybook must appear in the inventory
- Every proposed modern component must reference specific COBOL source files and preserve specific business rules
- Backlog tickets must include exact byte offsets, error message strings, and COBOL paragraph references
- The behavioral baseline must document every record layout and every business rule from the COBOL source
- Jira tickets must be created in the specified project after user confirmation
- Validation: cross-check proposed boundaries against the copybook dependency graph to confirm no implicit coupling is missed

## Advice and Pointers

- Start with the copybooks — they are the data contracts of the mainframe system. Programs that share a copybook share a data structure.
- The COBOL source is always the authoritative spec, not any existing port. Ports may have introduced bugs.
- REDEFINES is the most commonly mishandled construct — it means the same bytes can be read as different field structures. Map these to computed properties, not inheritance.
- Business rules are buried in processing paragraphs (names like `COMPARE-FIELDS`, `VALIDATE-DATA`, `EDIT-CHECK`). Extract every IF/EVALUATE block.
- Preserve error message strings character-for-character — operations teams grep for these exact strings in logs.
- JCL defines the batch orchestration: which programs run in what order, which datasets they share. This maps to your extraction sequence.
- WORKING-STORAGE counters (messages read, records written) are operational monitoring — they must be preserved in the modern implementation.
- 88-level condition names are boolean constants — they map to enum values or named constants in modern languages.

## Forbidden Actions

- Do not modify any source code in the repository
- Do not create Jira tickets without explicit user confirmation — always present the proposed backlog and wait for approval first
- Do not propose boundaries that would create circular component dependencies
- Do not skip the copybook analysis — copybooks define the data contracts and are the foundation of all boundary decisions
- Do not skip the behavioral baseline — without it, there is no way to verify the modernization preserves correct behavior
- Do not skip writing Confluence pages — discovery outputs must be persisted in the team's documentation, not just in the Devin session
- Do not treat an existing port (Python, Java) as the authoritative spec — always reference the original COBOL source

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
