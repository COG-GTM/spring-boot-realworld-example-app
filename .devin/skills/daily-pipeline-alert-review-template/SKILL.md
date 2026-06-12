---
name: daily-pipeline-alert-review-template
description: "Converted from Devin playbook: Daily Pipeline Alert & Review (Template)"
triggers:
  - user
  - model
---

# Daily Pipeline Alert & Review (Template)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Daily Pipeline Alert & Review (Template) (playbook-3fcd01aca36b4dd89cde9e8fabc086e1)

## Procedure

# Daily Pipeline Alert & Review (Template)

## Overview
This is a lightweight READ + ALERT playbook that runs daily at 8:30 AM Central time (13:30 UTC), 30 minutes after the main outreach session. It cross-references tracker state, verifies LinkedIn inbox data, generates a prioritized alert summary, and updates both trackers.

**This playbook does NOT execute any LinkedIn outreach actions. It is read-only + alert generation.**

It serves as a safety net to catch missed updates, stale data, and ensure the user has a clear picture of what needs attention across all 7 accounts.

## What's Needed From User
- LinkedIn credentials (LINKEDIN_EMAIL, LINKEDIN_PASSWORD) must be saved as secrets
- User may provide updates on messages sent, meetings booked, or status changes

## Knowledge Notes & IDs
- **Pipeline Generation Master Tracker**: {PIPELINE_TRACKER_NOTE_ID}
- **LinkedIn Outreach State Tracker**: {STATE_TRACKER_NOTE_ID}
- **Email Style Guide & Personal Connection Triggers**: {STYLE_GUIDE_NOTE_ID}
- **Daily Report**: Devin session only (no Slack)

<phase name="Cross-Reference & Verify" id="0">
## Phase 0: Cross-Reference Recent Sessions & LinkedIn Inbox Verification

**This phase is MANDATORY and must be completed before generating alerts.**

1. Read the Pipeline Generation Master Tracker knowledge note ({PIPELINE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` -> `get`
2. Read the LinkedIn Outreach State Tracker knowledge note ({STATE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` -> `get`
3. Check if there are any messages from the user with updates (confirmations of manually sent messages, new meetings, status changes)
4. Cross-reference the two trackers for consistency:
   - Do acceptance rates match between State Tracker and Master Tracker?
   - Are all go-quiet and exclusion decisions captured in both?
   - Are all manually sent actions (confirmed by the user) correctly reflected?
   - No actions listed as "due" that have already been completed?
5. Check recent Devin sessions (last 48-72 hours) for any decisions not yet propagated to trackers
6. Identify any stale contacts (14+ days with no activity)
7. Log into LinkedIn and verify the inbox:
   - Open LinkedIn Messaging
   - For every DUE/OVERDUE LinkedIn-channel action, verify against the actual conversation thread
   - Check if the user manually sent any messages that haven't been tracked yet
   - **Do NOT click Send, Reply, Connect, or any action button. Read-only.**
8. Document any discrepancies found

<verification>
- Both trackers have been read and cross-referenced
- All recent sessions (last 48-72 hours) have been reviewed for decisions not yet in trackers
- Acceptance rates match between State Tracker and Master Tracker
- All go-quiet and exclusion decisions from individual sessions are captured
- All manually sent actions (confirmed by the user) are correctly reflected
- No actions listed as "due" that have already been completed
- LinkedIn inbox has been opened and every DUE/OVERDUE LinkedIn-channel action has been verified against the actual conversation thread
- Any discrepancies have been documented and corrections prepared
</verification>
</phase>

<phase name="Load State" id="1">
## Phase 1: Load Current State

1. Read the Pipeline Generation Master Tracker knowledge note ({PIPELINE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` -> `get` (may already be loaded from Phase 0)
2. Read the LinkedIn Outreach State Tracker knowledge note ({STATE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` -> `get` (may already be loaded from Phase 0)
3. Check if the user has sent any messages with updates (new acceptances, meetings booked, negative responses, stage changes, confirmations of manually sent messages)
4. Incorporate any corrections identified in Phase 0 into the working data, including all LinkedIn-inbox verification corrections
5. Parse the ALERTS & TASKS QUEUE section to identify:
   - HIGH PRIORITY actions (due within 24-48 hrs)
   - MEDIUM PRIORITY actions (due within 3-7 days)
   - OVERDUE actions (past due date)
   - Manual mode actions (for informational awareness only)
6. Calculate days since last touch for all active contacts

<verification>
- Pipeline Generation Master Tracker has been read and parsed
- LinkedIn Outreach State Tracker has been read
- All active contacts have been identified with current stages
- Overdue actions count has been calculated
- Any user messages/updates have been incorporated
- Phase 0 corrections (including LinkedIn-inbox verification) have been incorporated into working data
</verification>
</phase>

<phase name="Generate Alerts" id="2">
## Phase 2: Generate Alert Summary

1. Categorize all actions into priority buckets:
   - **OVERDUE** (red): Actions past their due date
   - **DUE TODAY** (orange): Actions due today
   - **DUE THIS WEEK** (yellow): Actions due within 7 days
   - **UPCOMING** (blue): Actions due next week
   - **MONITORING** (grey): Actions sent but not yet responded to, include send date and response-window close date

2. For each HIGH PRIORITY contact (Stage 4 with discovery hooks due):
   - Pull the pre-written personalized copy from the COPY BANK
   - Mark it as READY FOR REVIEW
   - Include the full message text in the alert

3. For each MEDIUM PRIORITY contact (InMail eligible):
   - Prepare InMail recommendation with pre-written template
   - Note that InMails must be sent manually by the user

4. For Manual Mode contacts (if any):
   - Include status update but clearly mark as MANUAL, NO AUTOMATION

5. Calculate and include performance metrics:
   - Overall acceptance rate (current vs previous)
   - Follow-up response rate
   - Best/worst performing accounts
   - Stage distribution changes

6. Identify any stale contacts (14+ days with no activity) and flag for review

7. If Phase 0 found discrepancies, include a "DATA CORRECTIONS" section documenting what was wrong, what was fixed, and whether it came from session cross-reference or LinkedIn-inbox verification

8. Compose the batched alert message:
```
Pipeline Daily Alert -- [Date]

[DATA CORRECTIONS (if any from Phase 0)]

OVERDUE ([count]):
- [Contact] @ [Account] -- [action] was due [date]

DUE TODAY ([count]):
- [Contact] @ [Account] -- [action type]
  Copy ready for review: [yes/no]

DUE THIS WEEK ([count]):
- [Contact] @ [Account] -- [action type] due [date]

MONITORING ([count]):
- [Contact] @ [Account] -- [action type] sent [date], response window closes [date]

COPY READY FOR REVIEW ([count]):
[Full pre-written messages for the user to review/approve]

INMAIL RECOMMENDATIONS ([count]):
- [Contact] @ [Account] -- [pre-written InMail]

MANUAL MODE:
- [Status update, or "None currently"]

PERFORMANCE:
- Acceptance rate: [X]% ([accepted]/[sent])
- Response rate: [X]%
- Meetings: [count]
- Active contacts: [count]

NEXT SESSION ACTIONS:
- [What the next daily outreach session should prioritize]

ACTION REQUIRED FROM DAVID:
- [Specific actions the user needs to take manually]
```

<verification>
- All overdue actions have been identified and categorized
- Pre-written copy has been pulled for all HIGH PRIORITY contacts
- InMail recommendations have been prepared for MEDIUM PRIORITY contacts
- Manual mode section included (or noted as "None currently")
- Performance metrics are calculated and included
- Any Phase 0 discrepancies (including LinkedIn-inbox verification corrections) are documented in the alert
- Alert message is composed and ready to post
- No contact appears in DUE TODAY / OVERDUE if Phase 0 verified the message was already sent
</verification>
</phase>

<phase name="Post & Update" id="3">
## Phase 3: Post Alert & Update Tracker

1. **Send to the user via message_user**: Send the full alert to the user directly. Include:
   - The complete alert summary
   - All pre-written copy ready for review (surfaced inline, not just referenced)
   - Any recommendations for messaging adjustments
   - Clear action items: what the user needs to send manually
   - Any data corrections from Phase 0

2. **Update the Pipeline Generation Master Tracker**:
   - Update `Last Updated` timestamp
   - Update `Updated By` with this session's ID
   - Apply any corrections identified in Phase 0 (including LinkedIn-inbox verification corrections)
   - Recalculate due dates and overdue counts
   - Update performance metrics if any new data was incorporated
   - Write back via `devin_knowledge_manage` -> `update` ({PIPELINE_TRACKER_NOTE_ID})

3. **Update the LinkedIn Outreach State Tracker** if any changes were incorporated:
   - Apply any corrections from Phase 0
   - Update daily action log
   - For every LinkedIn-inbox-verified send, update the contact's Last Touch date and Follow-ups Sent field
   - Write back via `devin_knowledge_manage` -> `update` ({STATE_TRACKER_NOTE_ID})

<verification>
- Alert has been sent to the user via message_user with all copy surfaced for review
- Pipeline Generation Master Tracker has been updated with new timestamps, metrics, and Phase 0 corrections
- LinkedIn Outreach State Tracker has been updated if changes were made
- All deliverables have been presented directly to the user in the session
</verification>
</phase>

## Specifications
- This playbook is READ + ALERT only. It does NOT execute any LinkedIn actions (except read-only LinkedIn inbox verification in Phase 0)
- All copy must be surfaced for the user's review. Never auto-send.
- If Slack is configured in the future, alerts can be posted there too
- Performance metrics should compare to previous session's numbers when available
- Phase 0 (Cross-Reference) is MANDATORY and must be completed before generating alerts
- LinkedIn-inbox verification (Phase 0) is MANDATORY for every DUE/OVERDUE LinkedIn-channel action

## Advice and Pointers
- The Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) is the single source of truth for pipeline state
- The actual LinkedIn inbox is the single source of truth for whether a message was sent. Always trust the inbox over the tracker when they disagree.
- The LinkedIn Outreach State Tracker ({STATE_TRACKER_NOTE_ID}) has more granular daily action logs
- When calculating days since, use the current date vs the Last Touch date in the tracker
- InMail threshold is 7 days after connection request sent with no acceptance
- Discovery hooks are due Day 5-7 after the Day 2 value-add message was sent
- Soft meeting asks are due Day 10-14 after the discovery hook (if no response)
- CRITICAL: Always cross-reference individual session decisions against the Master Tracker BEFORE generating alerts (Phase 0)
- CRITICAL: LinkedIn inbox search from within the messaging inbox is unreliable. Use the top-level global search bar to find contacts, then click Message to open the thread.
- Go-quiet threshold: After 3 messages with no response, stop outreach
- 14-day stale threshold: Contacts with no activity for 14+ days should be flagged for go-quiet or retry decision

## Forbidden Actions
- Do NOT send any LinkedIn messages, connection requests, or InMails
- Do NOT click any action buttons on LinkedIn (Send, Connect, Reply, etc.)
- Do NOT modify outreach copy without surfacing for the user's review
- Do NOT skip Phase 0 cross-reference
- Do NOT skip LinkedIn-inbox verification for DUE/OVERDUE actions
- Do NOT generate alerts based on stale tracker data without cross-referencing

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
