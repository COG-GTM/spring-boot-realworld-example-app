---
name: jira-ticket-triage
description: "Converted from Devin playbook: Jira Ticket Triage"
triggers:
  - user
  - model
---

# Jira Ticket Triage

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Jira Ticket Triage (playbook-e6e98b28f1d048cf8aad1cfad4796c80)

## Procedure

# Jira Ticket Triage Playbook

## Overview
Analyze a Jira ticket (bug or task) to assess solvability, provide a confidence level, and propose a solution. Determine whether the ticket can be resolved fully autonomously (implement, test, and open a PR without needing additional clarifications or access beyond what's provided).

## What's Needed From User
- Full Jira ticket description (copy-pasted)
- Any relevant links mentioned in the ticket (repo URLs, documentation, etc.)
- Access to the codebase if the ticket references one

## Procedure
1. Parse the ticket to extract key information: title, description, acceptance criteria (if any), labels, priority, and linked resources
2. Identify the ticket type (bug or task) and understand the core ask
   - For bugs: note expected vs actual behavior, reproduction steps, and environment/version info
   - For tasks: note the deliverable and any constraints
3. Assess context completeness using this checklist:
   - Is the problem/goal clearly defined?
   - Are reproduction steps provided (for bugs)?
   - Is expected vs actual behavior documented (for bugs)?
   - Are success criteria specified? If not, can reasonable criteria be inferred?
   - Is the repo accessible?
   - Is the change localized to a single area/module?
   - If the ticket spans multiple components or is large in scope, note that it may need decomposition into subtasks
4. If the ticket references a codebase, investigate (do not implement):
   - Identify the owning service/module
   - Locate where the current behavior lives (entry points, call sites)
   - Find existing tests and how to run them
   - Check for config/env dependencies
   - Look for similar prior changes or patterns
   - If repo access is missing, note exactly what's needed
5. Research any unfamiliar technologies, APIs, or patterns mentioned
6. Formulate a proposed solution with specific steps
7. Evaluate confidence level using the criteria below
8. Deliver the triage report to the user

## Specifications

### Confidence Level Criteria
Assess these factors to determine confidence:
- Requirements clear? (problem defined, success criteria known or inferable)
- Repo/resources accessible?
- Reproducible? (for bugs: repro steps + expected/actual documented)
- Localized change? (single area, limited blast radius)
- Validation path known? (tests exist or can be written, manual verification possible)

**HIGH**: All or most factors pass. Clear requirements, small scope, accessible resources, known validation path.

**MEDIUM**: Some factors pass but 1-2 key unknowns exist. Likely solvable with reasonable assumptions.

**LOW**: One or more critical factors fail (no repo access, unclear requirements, large/risky scope, no way to validate).

### Triage Report Format
Provide the following:

**Ticket Summary**: One-sentence description of what the ticket is asking for

**Confidence Level**: HIGH / MEDIUM / LOW

**Confidence Rationale**: Brief explanation citing which factors passed/failed

**Assumptions**: Any presumptions made due to missing information (label as inferred)

**Proposed Solution**: Step-by-step approach to resolve the ticket

**Risks / Blast Radius**: What could break, affected areas, data/user impact

**Validation Plan**: How to verify the fix (specific tests, manual checks, repro before/after)

**Blockers/Questions** (if any): What needs clarification before proceeding

**Recommendation**: Proceed autonomously, request more information, or escalate

## Advice and Pointers
- When assessing scope, consider ripple effects (tests, documentation, related components)
- For bugs, prioritize understanding the root cause before proposing a fix
- If acceptance criteria are missing, derive candidate criteria and label them as inferred
- A ticket with vague requirements but small, isolated scope may still be HIGH confidence if the solution is obvious
- A well-documented ticket with large scope should be MEDIUM or LOW due to complexity
- Investigation (reading code, searching logs, identifying tests) is encouraged; writing code changes is not

## Forbidden Actions
- Do not start implementing the solution during triage (investigation is OK, PRs are not)
- Do not assume access to resources that haven't been confirmed
- Do not assign HIGH confidence to tickets with ambiguous scope or missing critical context

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
