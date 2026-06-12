---
name: daily-linkedin-outreach-automation
description: "Converted from Devin playbook: Daily LinkedIn Outreach Automation"
triggers:
  - user
  - model
---

# Daily LinkedIn Outreach Automation

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Daily LinkedIn Outreach Automation (playbook-1359f37caea94e9290a57220b57c4aab)

## Procedure

# Daily LinkedIn Outreach Automation

## Overview
This playbook runs daily at 8 AM Arizona time (3 PM UTC). It supports the user's pipeline generation across their target accounts. Accounts may be designated as MANUAL MODE (active deal, do NOT auto-prospect).

**Operating Mode : MODE A, READ-ONLY / SURFACE-ONLY.** Devin sessions do NOT send any LinkedIn DMs, connection requests, profile views, likes, comments, or InMails. The user executes all LinkedIn actions manually.

**OWNERSHIP CHANGE : VERIFY-AND-WRITE ONLY.** Per the user's directive at 15:58 UTC, this playbook NO LONGER composes the Daily 60-Touch Plan or drafts any copy. Plan composition and all copy drafting (LinkedIn DMs, CRs, InMails, cold emails, follow-ups, Day 2 value-adds, organic engagement comment angles) have moved permanently to the **Daily Pipeline Alert & Review** playbook (`{PIPELINE_ALERT_PLAYBOOK_ID}`).

This playbook is now responsible for the verify-and-write loop only:
1. Log into LinkedIn with the user's credentials.
2. Run the LinkedIn inbox sweep (Focused, Other, Connections, InMail tabs).
3. Run the Connections list sweep to detect new acceptances.
4. Run the Sent invitations sweep to detect new manual CRs by the user.
5. Profile-verify every new CR (employer, title, connection count, posting cadence). Verify autonomously, do not delegate to {REP_NAME} what is accessible from LinkedIn.
6. Profile-verify every contact on the HOLD profile-verify backlog.
7. Verify every DUE/OVERDUE LinkedIn-channel action against the actual conversation thread.
8. Detect manual sends {REP_NAME} did outside of Devin.
9. Detect inbound responses (positive, negative, neutral). Add negatives to exclusion list immediately.
10. Write all findings back to the LinkedIn Outreach State Tracker.
11. Send a brief verify-and-write report to the user documenting what was found and what was written.

This playbook does NOT:
- Compose the Daily 60-Touch Plan (owned by Daily Pipeline Alert & Review).
- Draft any LinkedIn DM copy, CR copy, InMail copy, cold email copy, follow-up copy, Day 2 value-add copy, or organic engagement comment angles.
- Run a Contact History Audit beyond what is needed to verify a thread (the full audit lives in Daily Pipeline Alert & Review Phase 2.5).
- Surface a daily plan or alert summary to {REP_NAME} (Daily Pipeline Alert & Review owns the canonical daily deliverable).

This Mode A change was driven by repeated detection of concurrent manual activity on the user's account during Devin sessions. Two-way coordination was creating double-touch risk and tracker drift. Single source of execution ({REP_NAME} manual) eliminates that. The ownership change consolidates copy composition into Daily Pipeline Alert & Review so the daily plan with copy appears in one place every day.

## What's Needed From User
- LinkedIn credentials (LINKEDIN_EMAIL, LINKEDIN_PASSWORD) must be saved as secrets so Devin can log in to read inbox + Sent invitations + Connections + profiles
- 2FA approval from the user (push notification on his LinkedIn mobile app) when prompted
- User may provide updates on new acceptances, meetings booked, manual sends, or messaging changes via message

## Knowledge Notes & IDs
- **Pipeline Generation Master Tracker**: {PIPELINE_TRACKER_NOTE_ID} (source of truth for all contact stages, alerts, copy bank, metrics)
- **LinkedIn Outreach State Tracker**: {STATE_TRACKER_NOTE_ID} (granular daily action logs, written by THIS playbook)
- **Daily Pipeline Alert & Review playbook**: {PIPELINE_ALERT_PLAYBOOK_ID} (owns plan composition + copy drafting; this playbook defers to it)
- **30-Touch Enterprise Outreach Strategy**: {OUTREACH_STRATEGY_NOTE_ID} (reference only; Daily Pipeline Alert & Review owns plan composition)
- **Gmail Sent Folder Checker**: {GMAIL_CHECKER_NOTE_ID} (script for Gmail audit, used here for completeness; Daily Pipeline Alert & Review also runs the audit)
- **LinkedIn credentials**: `LINKEDIN_EMAIL` and `LINKEDIN_PASSWORD` secrets
- **Gmail credentials**: `GMAIL_ADDRESS` and `GMAIL_APP_PASSWORD` secrets

<phase name="State Loading & Login" id="1">
## Phase 1: State Loading & LinkedIn Login

1. **Read the Pipeline Generation Master Tracker** knowledge note ({PIPELINE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` action `get`. Extract the ALERTS & TASKS QUEUE, EXCLUSION LIST, GO QUIET LIST, ACTIVE CONTACT DETAIL roster, and the HOLD profile-verify backlog.
2. **Read the LinkedIn Outreach State Tracker** knowledge note ({STATE_TRACKER_NOTE_ID}) to load granular daily action logs and the InMail queue.
3. Check if there are any new messages from the user with updates (new acceptances, negative responses, manual sends {REP_NAME} executed since the last session).
4. **Concurrent-session check.** Check `devin_session_search` for any Daily Pipeline Alert & Review session running for the same day. If one is running, coordinate: this playbook owns LinkedIn login, inbox/Sent/Connections sweep, profile verification, and the State Tracker write. The Daily Pipeline Alert & Review session inherits findings from the State Tracker. Do not duplicate State Tracker writes.
5. Open Chrome and navigate to linkedin.com.
6. Log into LinkedIn using the saved LINKEDIN_EMAIL and LINKEDIN_PASSWORD credentials.
7. If a CAPTCHA or 2FA challenge appears, message the user immediately and ask him to complete it via mobile / Desktop tab. Wait for confirmation before proceeding.
8. Verify you're on the LinkedIn feed and logged in as the user Terrell.

<verification>
- Pipeline Generation Master Tracker has been read; ALERTS & TASKS QUEUE, EXCLUSION LIST, GO QUIET LIST, ACTIVE CONTACT DETAIL, HOLD profile-verify backlog all loaded
- LinkedIn Outreach State Tracker has been read; current state loaded
- Concurrent-session check has been performed; this playbook owns the LinkedIn channel for the cycle
- LinkedIn login is successful and feed is visible
- If CAPTCHA or 2FA appeared, the user has completed it
</verification>
</phase>

<phase name="LinkedIn Inbox & Sent Sweep" id="2">
## Phase 2: LinkedIn Inbox, Connections, and Sent Sweep (READ-ONLY)

1. Navigate to LinkedIn `My Network` > `Connections` and read the Connections list directly. Acceptances are identified from the Connections list, NOT inferred from a reduction in the Sent invitations count.
2. Cross-reference Connections list against the Master Tracker Stage 2-Requested roster. Move newly accepted contacts from Stage 2-Requested to Stage 4-Messaged with the acceptance date.
3. Navigate to LinkedIn `My Network` > `Manage invitations` > `Sent` to verify still-pending CRs and detect any new outbound CRs the user sent manually.
4. **Profile-verify every new CR detected in the Sent list** before adding it to an account roster:
   - Open the contact's LinkedIn profile.
   - Confirm employer matches tracker assumption. Do NOT infer account assignment from adjacent names in the Sent invitations view (this caused a previous misattribution where a contact was assigned to the wrong account).
   - Confirm title matches tier-and-phase assumption.
   - Confirm connection count is >=500 (LinkedIn channel eligibility per LinkedIn Channel Qualification rule). If <500, flag in State Tracker for routing to email channel.
   - Scan recent Activity for posts in the last 30 days; informs tier classification.
   - Verify autonomously. Do NOT delegate verification work to {REP_NAME} that is accessible from LinkedIn.
5. **Profile-verify every contact on the HOLD profile-verify backlog**: open each profile and resolve account assignment + title + connection count + posting cadence. Update State Tracker `Profile-Verified` to Y. Do not roll the contact forward to the next session if the profile is accessible.
6. Navigate to LinkedIn Messaging. Sweep Focused, Other, Connections, and InMail tabs:
   - Detect any new positive, negative, or neutral responses from contacts.
   - Flag any NEGATIVE responses immediately. Add the contact to the EXCLUSION LIST in BOTH trackers and stop all outreach.
   - Flag any inbound prospecting messages from non-target sources (other vendors). Note in the State Tracker; do not engage on {REP_NAME}'s behalf.
7. **Verify every DUE/OVERDUE LinkedIn-channel action against the actual conversation thread**:
   - Use the top-level LinkedIn search bar (`I'm looking for...`) to locate each contact's profile. Do NOT use messaging-inbox search; it is unreliable.
   - Click `Message` to open the conversation pop-out.
   - Scroll to the most recent message. Check sender, timestamp, content.
   - If {REP_NAME} sent a matching message within the tracker's due window, mark the action complete and move the contact from DUE to MONITORING.
   - If the most recent message is from the contact (not {REP_NAME}) and the tracker doesn't reflect it, flag in State Tracker as a missed response.
8. **Detect manual sends by the user since the last session.** Inspect message timestamps, sent invitations, and inbox threads for sends {REP_NAME} did outside of Devin. Log to State Tracker so trackers absorb them.
9. **Detect manual CRs sent by the user.** Cross-reference the current Sent list against the prior session's Sent list snapshot. Any net-new CR that does not appear in any Devin session's send log was sent manually.
10. Record all findings in a structured Phase 2 results object: new acceptances (with dates), new manual CRs (with profile-verified employer/title/connection count/posting), new responses (with sender + content + sentiment), missed responses, manual sends, inbound prospecting messages.

<verification>
- LinkedIn My Network > Connections list has been read directly (NOT inferred from Sent reduction)
- All sent connection requests have been checked for acceptance status against the Connections list
- Every new CR detected in Sent has been profile-verified for employer + title + connection count + posting cadence
- Every contact on the HOLD profile-verify backlog has been profile-verified or rolled forward only if profile genuinely cannot be opened
- LinkedIn messages have been swept across all 4 tabs for new responses
- Any negative responses have been flagged and contacts added to EXCLUSION LIST in BOTH trackers
- Every DUE/OVERDUE LinkedIn-channel action has been verified against the actual conversation thread
- Manual activity by the user since last session has been detected and logged
- Inbound prospecting messages flagged but not engaged
</verification>
</phase>

<phase name="Update State Tracker" id="3">
## Phase 3: Update LinkedIn Outreach State Tracker (WRITE-ONLY)

This is the only knowledge-note write this playbook performs. The Pipeline Generation Master Tracker write is owned by the Daily Pipeline Alert & Review session (which inherits from this State Tracker).

1. Update the LinkedIn Outreach State Tracker ({STATE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` `update` with a SESSION ADDENDUM at the top of the file containing:
   - Updated `Last Updated` timestamp and `Updated By` session ID
   - **Inbox Sweep Findings**: new responses (positive, negative, neutral), missed responses
   - **Connections Sweep Findings**: new acceptances with dates, stage transitions (2-Requested to 4-Messaged)
   - **Sent Sweep Findings**: new manual CRs by {REP_NAME}, with profile-verified employer/title/connection count/posting cadence
   - **Profile Verify Updates**: every contact on the HOLD profile-verify backlog with verified employer/title/connection count/posting; update `Profile-Verified` to Y
   - **Manual Activity Detected**: sends {REP_NAME} did outside of Devin, with timestamps
   - **Negative Responses**: contacts added to EXCLUSION LIST
   - **Inbound Prospecting**: non-target inbound messages flagged but not engaged
   - **Conversation Thread Verification**: every DUE/OVERDUE LinkedIn-channel action with thread-verified status (already sent, still due, missed response received)
2. Do NOT write to the Pipeline Generation Master Tracker. The Daily Pipeline Alert & Review session owns that write and inherits the State Tracker findings via Phase 0 step 7 of that playbook.

<verification>
- LinkedIn Outreach State Tracker has been updated with the full SESSION ADDENDUM
- Pipeline Generation Master Tracker has NOT been written to from this session (Daily Pipeline Alert & Review owns that write)
- Every profile-verified contact has Profile-Verified set to Y
- Every newly accepted contact has been moved from Stage 2-Requested to Stage 4-Messaged
- Every negative responder has been added to EXCLUSION LIST
</verification>
</phase>

<phase name="Send Verify-and-Write Report" id="4">
## Phase 4: Send Verify-and-Write Report to {REP_NAME}

This playbook does NOT send a daily 60-touch plan or copy to the user. Daily Pipeline Alert & Review owns that deliverable. This playbook sends a brief verify-and-write report so {REP_NAME} knows what was checked, what was found, and what was written.

Report format (via `message_user`):

```
LinkedIn Verify-and-Write Report, [Date] [Time UTC]

Session: [session ID]
Mode: A read-only verify-and-write only
State Tracker: written
Master Tracker: deferred to Daily Pipeline Alert & Review session (per  ownership change)

INBOX SWEEP
- New positive responses: [count]
  - [Name] @ [Account]: [content summary]
- New negative responses: [count]
  - [Name] @ [Account]: added to EXCLUSION LIST
- New neutral responses: [count]
  - [Name] @ [Account]: [content summary]
- Missed responses (in tracker as no-reply but reply found): [count]
  - [Name] @ [Account]: [date + content]
- Inbound prospecting (non-target): [count]

CONNECTIONS SWEEP
- New acceptances: [count]
  - [Name] @ [Account]: accepted [date]
- Total connections: [count]

SENT INVITATIONS SWEEP
- Pending CRs: [count]
- New manual CRs by the user since last session: [count]
  - [Name] @ [Account]: title [title], connections [count], posting [Y/N], profile-verified [Y/N]
- Stale CRs (>=14 days pending): [count]
  - [Name] @ [Account]: pending [days] days

PROFILE VERIFY UPDATES
- HOLD backlog cleared this session: [count]
  - [Name] @ [Account]: verified [employer + title + connection count + posting]
- Still on HOLD (profile not openable): [count]
  - [Name] @ [Account]: [reason]

CONVERSATION THREAD VERIFICATION
- DUE/OVERDUE LinkedIn actions verified: [count]
  - [Name] @ [Account]: [verdict: ALREADY SENT date | STILL DUE | MISSED RESPONSE date]

MANUAL ACTIVITY OBSERVED
- LinkedIn DMs you sent outside Devin: [count]
  - [Name] @ [Account]: [content summary] [timestamp]
- Manual CRs you sent: [count] (also in Sent sweep)

EDGE CASES
- 2FA prompts: [count, resolved]
- Captcha prompts: [count, resolved]
- Profiles unreachable: [count]
- Anything else: [details]

NEXT
- The Daily Pipeline Alert & Review session [session ID, if running] is composing today's full plan with copy. You should receive that deliverable in that session.
- If no Daily Pipeline Alert & Review session has run today yet, message [the user or schedule] to trigger it. This playbook does not compose the plan.
```

<verification>
- Verify-and-write report has been sent to the user via `message_user`
- Report does NOT include drafted copy (this playbook does not draft copy)
- Report does NOT include a daily plan (this playbook does not compose plans)
- Report includes inbox sweep, connections sweep, Sent sweep, profile verify updates, conversation thread verification, manual activity, and edge cases
- {REP_NAME} has been pointed to the Daily Pipeline Alert & Review session for the canonical daily plan with copy
</verification>
</phase>

## Specifications
- This playbook is VERIFY-AND-WRITE ONLY . It does NOT compose the Daily 60-Touch Plan and does NOT draft copy.
- Mode A applies: 0 LinkedIn actions executed by Devin (no DMs, no CRs, no profile views beyond verification, no likes, no comments, no InMails). {REP_NAME} executes all LinkedIn actions manually.
- Plan composition + copy drafting + Master Tracker write are owned by the Daily Pipeline Alert & Review playbook (`{PIPELINE_ALERT_PLAYBOOK_ID}`).
- This playbook owns: LinkedIn login, inbox sweep, Connections sweep, Sent invitations sweep, profile verification of new CRs, profile verification of the HOLD backlog, conversation thread verification of DUE/OVERDUE actions, manual-activity detection, State Tracker write, verify-and-write report.
- Concurrent-session coordination: if a Daily Pipeline Alert & Review session is running for the same day, this playbook still owns the LinkedIn channel + State Tracker write. The other session inherits findings from the State Tracker.
- Profile verification is mandatory and must be done autonomously from the LinkedIn profile. Do NOT delegate verification work to {REP_NAME} that is accessible from LinkedIn.
- If GMAIL_APP_PASSWORD is rejected, log the failure and proceed with LinkedIn-only verification. The Daily Pipeline Alert & Review session also runs the Gmail audit; do not block on it here.
- Slack delivery is REMOVED permanently . This playbook never posted to Slack and continues to not post.

## Advice and Pointers
- The LinkedIn inbox is the single source of truth for whether a LinkedIn message was sent. Always trust the inbox over the tracker when they disagree.
- The contact's LinkedIn profile is the single source of truth for account assignment, title, connection count, posting cadence. Always verify directly from the profile, never infer from adjacent names in the Sent invitations view.
- LinkedIn inbox search from within the messaging inbox is unreliable. Use the top-level global search bar.
- A new CR with no profile verification stays on HOLD. Do not roll forward without resolving.
- If 2FA push fails or times out, send a non-blocking message to {REP_NAME} explaining and skip the LinkedIn-dependent steps for this session. Document in State Tracker.
- If the Daily Pipeline Alert & Review session is also running, that session inherits LinkedIn findings via Phase 0 step 7 of its playbook. Do not write the same findings to the State Tracker more than once per cycle.
- The 30-Touch Enterprise Outreach Strategy note is reference only here. Plan composition lives in Daily Pipeline Alert & Review.

## Forbidden Actions
- Do NOT execute any LinkedIn actions (no DMs, no CRs, no profile views beyond verification, no likes, no comments, no InMails). Verification-only profile views are the ONLY exception.
- Do NOT compose the Daily 60-Touch Plan. That deliverable is owned by Daily Pipeline Alert & Review.
- Do NOT draft any LinkedIn DM, CR, InMail, cold email, follow-up, Day 2 value-add, or organic engagement copy. Copy drafting is owned by Daily Pipeline Alert & Review.
- Do NOT write to the Pipeline Generation Master Tracker. That write is owned by Daily Pipeline Alert & Review.
- Do NOT post to Slack. Slack delivery has been REMOVED permanently per {REP_NAME}'s  directive.
- Do NOT delegate verification work to {REP_NAME} that is accessible from LinkedIn (message thread content, contact employer, contact title, contact connection count, contact posting activity). Verify autonomously.
- Do NOT modify MANUAL MODE account contacts in any tracker.
- Do NOT engage with inbound prospecting messages on {REP_NAME}'s behalf. Flag for {REP_NAME}'s discretion only.
- Do NOT skip the profile verification step for new CRs or for the HOLD backlog. The a previous session a contact misattribution traces directly to skipping profile verification.
- Do NOT use the messaging-inbox search bar to locate contacts; use the top-level global search bar.
- Do NOT block the session on 2FA, captcha, or Gmail audit failure. Log and proceed.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
