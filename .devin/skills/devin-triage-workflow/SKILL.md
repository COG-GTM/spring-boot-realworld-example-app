---
name: devin-triage-workflow
description: "Converted from Devin playbook: devin-triage-workflow"
triggers:
  - user
  - model
---

# devin-triage-workflow

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: devin-triage-workflow (playbook-bc62e83758d74a25ad253b9de3374ac2), macro `!devintriageworkflow`

## Procedure

# Production Alert Triage Playbook

You are an expert SRE triaging a production alert. Follow these phases precisely.

## Context Provided
- Alert Name: {{alertName}}
- Severity: {{severity}}
- Description: {{description}}
- Repository: {{targetRepo}}
- Logs: {{logs}}

---

## Phase 1: Alert Analysis

Parse the alert and document:
1. What is the immediate symptom?
2. What is the likely root cause?
3. What is the user impact?
4. What is the urgency level?

---

## Phase 2: Codebase Analysis

1. Clone the repository
2. Search for the error in the codebase
3. Trace the stack to find the root cause
4. Document the exact file and line

---

## Phase 3: Triage Decision

Decide:
- **Code Fix**: Clear bug with straightforward fix → Proceed to Phase 4
- **Config Issue**: Environment/config problem → Document and notify
- **External Issue**: Third-party service problem → Document and notify owner
- **Escalate**: Complex/risky/unclear → Create ticket and notify human

---

## Phase 4: Implement Fix (if Code Fix)

1. Create a minimal fix
2. Add proper error handling
3. Write unit tests
4. Create a Pull Request with:
   - Clear title: `fix(component): description`
   - Problem and solution description
   - Testing checklist
   - Link to this session

---

## Phase 5: JIRA Ticket

Create a JIRA ticket with:
- Summary: [Sev{{severity}}] {{alertName}}
- Description: Full context, root cause, fix details
- Links: PR, session, logs
- Labels: devin-triage, auto-generated

---

## Phase 6: Slack Notification

Post to {{slackChannel}}:
- Alert name and severity
- Root cause summary
- Links: PR, JIRA, Session

---

## Phase 7: Wrap Up

Provide final summary:
- What was the issue?
- What was the fix?
- What artifacts were created?
- What are the recommendations?

---

Begin triage now.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
