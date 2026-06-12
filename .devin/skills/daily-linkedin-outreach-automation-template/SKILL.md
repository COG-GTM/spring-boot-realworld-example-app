---
name: daily-linkedin-outreach-automation-template
description: "Converted from Devin playbook: Daily LinkedIn Outreach Automation (Template)"
triggers:
  - user
  - model
---

# Daily LinkedIn Outreach Automation (Template)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Daily LinkedIn Outreach Automation (Template) (playbook-90beac0371ba4d4693ade73ddfa2d58b)

## Procedure

# Daily LinkedIn Outreach Automation

## Overview
This playbook runs daily at 8 AM Central time (13:00 UTC). It supports the user's pipeline generation across their target accounts.

**CRITICAL: This is a READ-ONLY + DRAFT-ONLY playbook. Devin does NOT send any LinkedIn messages, connection requests, or InMails. Devin logs into LinkedIn to check acceptances, responses, and profile data, then drafts all outreach copy for the user to review and manually send.**

The session reads state from BOTH the "Pipeline Generation Master Tracker" (source of truth) and the "LinkedIn Outreach State Tracker" (granular daily logs), performs read-only LinkedIn checks, drafts all outreach copy, updates both trackers, and sends the user a daily report with all pre-written copy ready for manual sending.

## What's Needed From User
- LinkedIn credentials (LINKEDIN_EMAIL, LINKEDIN_PASSWORD) must be saved as secrets
- User may provide updates on new acceptances, meetings booked, or messaging changes via message
- User manually sends all connection requests, messages, and InMails on LinkedIn after reviewing Devin's drafts

## Knowledge Notes & IDs
- **Pipeline Generation Master Tracker**: {PIPELINE_TRACKER_NOTE_ID} (source of truth for all contact stages, alerts, copy bank, metrics)
- **LinkedIn Outreach State Tracker**: {STATE_TRACKER_NOTE_ID} (granular daily action logs)
- **Email Style Guide & Personal Connection Triggers**: {STYLE_GUIDE_NOTE_ID}
- **Org-wide Anti-AI Messaging Rules**: {ANTI_AI_RULES_NOTE_ID}
- **Org-wide Pre-Approved Customer List**: {PRE_APPROVED_CUSTOMERS_NOTE_ID}
- **Org-wide Use Case Mapping**: {USE_CASE_MAPPING_NOTE_ID}
- **Org-wide Email Style Guide**: {EMAIL_STYLE_GUIDE_NOTE_ID}
- **Daily Report**: Devin session only (no Slack)

<phase name="State Loading & Login" id="1">
## Phase 1: State Loading & LinkedIn Login

1. **Read the Pipeline Generation Master Tracker** knowledge note ({PIPELINE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` -> `get`. This is the source of truth for:
   - Current pipeline stages for all contacts across all 7 accounts
   - ALERTS & TASKS QUEUE (what actions are due/overdue)
   - Pre-written copy from the COPY BANK
   - Exclusion list
   - Performance metrics
   - Any MANUAL MODE contacts (if the user flags active deals)
2. **Read the LinkedIn Outreach State Tracker** knowledge note ({STATE_TRACKER_NOTE_ID}) to load granular daily action logs, messaging performance data, and InMail queue
3. **Read the user's Email Style Guide** ({STYLE_GUIDE_NOTE_ID}) for personal connection triggers and MedTech-specific social proof
4. **Read the org-wide Anti-AI Messaging Rules** ({ANTI_AI_RULES_NOTE_ID}) for banned phrases, dash rules, and LinkedIn channel qualification
5. Check if there are any new messages from the user with updates (new acceptances, negative responses, messaging changes, new contacts to add/remove, confirmations of messages he sent manually since last session)
6. Open Chrome and navigate to linkedin.com
7. Log into LinkedIn using the saved LINKEDIN_EMAIL and LINKEDIN_PASSWORD credentials
8. If a CAPTCHA appears, message the user immediately and ask him to complete it via the Desktop tab. Wait for confirmation before proceeding.
9. Verify you're on the LinkedIn feed and logged in as the user

<verification>
- Pipeline Generation Master Tracker has been read and ALERTS & TASKS QUEUE parsed
- LinkedIn Outreach State Tracker knowledge note has been read and current state is loaded
- Email Style Guide and Anti-AI Messaging Rules have been read
- Any user messages/updates have been incorporated (including confirmation of manually sent messages)
- Any MANUAL MODE contacts identified and excluded from automation
- LinkedIn login is successful and feed is visible
- If CAPTCHA appeared, user has completed it
</verification>
</phase>

<phase name="Check Acceptances & Responses" id="2">
## Phase 2: Check for New Acceptances & Responses (READ-ONLY)

**This phase is entirely read-only on LinkedIn. Do NOT click any send, connect, or reply buttons.**

1. Navigate to LinkedIn "My Network" > "Manage invitations" > "Sent" to check which connection requests have been accepted
2. For each account in scope, check the status of pending connection requests
3. Navigate to LinkedIn Messaging to check for any new responses from contacts
4. Flag any NEGATIVE responses immediately, add the contact to the exclusion list in BOTH trackers and stop all outreach to them
5. Record all new acceptances with timestamps
6. For each new acceptance, check the Pipeline Generation Master Tracker's ALERTS & TASKS QUEUE to determine which follow-up message is due and when (respecting the timing rules: Day 2 value-add, Day 5-7 discovery hook, Day 10-14 soft meeting ask)
7. Pull any pre-written copy from the Master Tracker's COPY BANK for contacts with actions due
8. Update the running state with all findings
9. **Check message threads** to verify whether the user has manually sent any messages since the last session. If a message was sent, update the tracker to reflect the send date and advance the contact's stage accordingly.

**Important timing rules for follow-ups (the user sends manually):**
- Day 2 after acceptance: Draft value-add message (references their work, asks a genuine question)
- Day 5-7 after acceptance: Draft discovery hook (ties to specific project/initiative)
- Day 10-14 after acceptance: Draft soft meeting ask (only if engagement signals are positive)
- Never draft more than 1 message for the same person per day
- Never queue follow-ups for weekends

<verification>
- All sent connection requests have been checked for acceptance status
- LinkedIn messages have been checked for new responses
- Any negative responses have been flagged and contacts added to exclusion list in BOTH trackers
- New acceptances have been recorded with dates
- Follow-up queue has been updated based on acceptance timing and Master Tracker ALERTS & TASKS QUEUE
- Pre-written copy from COPY BANK has been pulled for all due actions
- Message threads checked to verify the user's manually sent messages since last session
- NO buttons were clicked on LinkedIn (read-only)
</verification>
</phase>

<phase name="Draft Daily Outreach Copy" id="3">
## Phase 3: Draft Daily Outreach Copy (NO SENDING)

**CRITICAL: Devin does NOT send anything on LinkedIn. All copy is drafted and presented to the user for manual sending.**

Perform the following research and drafting actions:

### 3a. Profile Research (Read-Only Browsing)
For contacts in the pre-outreach or active phase:
- Visit target contact profiles to gather fresh intel (recent posts, job changes, activity)
- Spend time understanding each contact's recent activity and interests
- Note any personal connection triggers (sports, alma mater, dogs, etc.) that match the user's hooks
- This also creates "the user viewed your profile" notifications as a warm-up
- **Do NOT click Connect, Message, or any action buttons**

### 3b. Draft Follow-Up Messages
For contacts who have accepted connection requests and are due for a follow-up:
- **First check the Pipeline Generation Master Tracker COPY BANK** for pre-written personalized copy
- If personalized copy exists, include it in the daily report
- If no pre-written copy exists, draft a message that:
  - References something specific about the contact (from their LinkedIn profile/posts)
  - Ties back to a known project/initiative at their company
  - Does NOT ask for a meeting in the first message
  - Is conversational and informal in tone
  - Follows the Anti-AI Messaging Rules (no dashes, no banned phrases)
  - Uses the user's personal connection triggers from the Email Style Guide where applicable
- Draft max 3-5 follow-up messages per day, staggered across accounts
- Label each draft with the contact name, account, and recommended send timing

### 3c. Draft New Connection Requests
For contacts who haven't been reached yet:
- Draft deep-researched personalized connection request (<300 characters)
- **Check connection count first**: under 500 connections = draft email instead of CR
- Draft max 3-4 per day, staggered across accounts (don't recommend blasting one account)
- Prioritize bottom-up: Directors before VPs
- Label each draft with the contact name, LinkedIn URL, and recommended send timing

### 3d. Organic Engagement Recommendations
Suggest organic engagement actions the user can take:
- Identify 2-3 recent posts from target contacts that the user could like or comment on
- Draft 1-2 thoughtful comments for relevant posts about AI, DevOps, cloud migration, MedTech, or life sciences
- Include the post URL and suggested comment text

### 3e. InMail Drafts
For high-priority contacts who haven't accepted connection requests after 7+ days:
- Draft InMail copy for the user to send manually
- Include the pre-written InMail copy from the Master Tracker COPY BANK or draft new copy
- Label with contact name, LinkedIn URL, and priority level

**Anti-AI copy rules (applied to ALL drafts):**
- No dashes (em, en, or hyphens used as dashes) in any copy
- No banned phrases ("Curious:", "keeping pace", "leverage", "delighted", "thrilled", "fascinating")
- Each message must be structurally unique, no templates repeated
- CRs under 300 characters
- Discovery hooks 2-4 sentences max
- Use contractions, parenthetical asides
- Should feel like a 2-3 minute write, not 30 minutes

<verification>
- Profile research was read-only (no buttons clicked on LinkedIn)
- All follow-up drafts follow Anti-AI Messaging Rules
- All CR drafts are under 300 characters
- Each draft is labeled with contact name, account, LinkedIn URL, and recommended timing
- Connection count checked for CR candidates (under 500 = email draft instead)
- No two drafts use the same message structure
- Personal connection triggers used where applicable
- NO messages, CRs, or InMails were sent by Devin
</verification>
</phase>

<phase name="Update State & Report" id="4">
## Phase 4: Update State & Send Daily Report

### 4a. Update the Pipeline Generation Master Tracker
Update the Pipeline Generation Master Tracker knowledge note ({PIPELINE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` -> `update` with:
- Updated `Last Updated` timestamp and `Updated By` session ID
- **Stage transitions**: Move any contacts that changed stage based on LinkedIn checks (e.g., 2-Requested -> 3-Connected for new acceptances)
- **Manually sent messages confirmed**: If the user confirmed he sent messages, update Last Touch dates and Next Action
- **New acceptances** with dates in ACTIVE CONTACT DETAIL
- **Negative responses**, add to EXCLUSION LIST, move to Stage X
- **Overdue action count** recalculated in ALERTS & TASKS QUEUE
- **New drafted copy** added to COPY BANK for the user to use
- **Account dashboard metrics** (acceptance rates, response rates, stage summaries)
- **GLOBAL METRICS** section updated
- **MESSAGE PERFORMANCE METRICS** updated with latest acceptance/response rates
- **Profile views completed**, update Stage 0 -> Stage 1 transitions

### 4b. Update the LinkedIn Outreach State Tracker
Update the "LinkedIn Outreach State Tracker" knowledge note ({STATE_TRACKER_NOTE_ID}) with:
- Updated "Last Updated" date and session identifier
- New entries in the Daily Action Log (noting all actions were read-only + drafting)
- Updated account status summaries
- Updated Message Performance Tracking
- Updated "What's Working" and "What's NOT Working" sections based on new data
- Any new entries in the Exclusion List
- Updated InMail Queue
- Updated Messaging Adaptation Notes if patterns are emerging

### 4c. Send Daily Report to the user via message_user
Send a message to the user (using message_user) with the following structure:

```
LinkedIn Daily Report -- [Date]

NEW ACCEPTANCES:
- [Name] @ [Account], accepted [date]. Next: [follow-up action + timing]

PROFILE VIEWS COMPLETED:
- [count] profiles viewed across [accounts]

DRAFT CONNECTION REQUESTS READY TO SEND ([count]):
---
[Contact Name] @ [Account]
LinkedIn: [URL]
Draft CR: "[pre-written CR text, under 300 chars]"
Recommended send: [timing]
---

DRAFT FOLLOW-UP MESSAGES READY TO SEND ([count]):
---
[Contact Name] @ [Account]
Message type: [Day 2 / Discovery Hook / Soft Ask]
Draft: "[full message text]"
Recommended send: [timing]
---

DRAFT INMAIL COPY ([count]):
---
[Contact Name] @ [Account]
LinkedIn: [URL]
Draft InMail: "[full InMail text]"
---

ENGAGEMENT RECOMMENDATIONS:
- Like: [Contact]'s post about [topic] ([URL])
- Comment: [Contact]'s post about [topic] ([URL])
  Suggested comment: "[draft comment]"

NEGATIVE RESPONSES:
- [None / Name + details + action taken]

MANUAL MODE ACCOUNTS:
- [Status update for any manual mode accounts, or "None currently"]

MESSAGING PERFORMANCE:
- Connection request acceptance rate: [X]% ([accepted]/[sent])
- Best performing hook type: [type] ([rate]%)
- Follow-up response rate: [X]%

TOMORROW'S QUEUE:
- [Planned drafts for next session based on Master Tracker ALERTS & TASKS QUEUE]

ADAPTATION NOTES:
- [Any messaging changes recommended based on performance data]

ACTION REQUIRED FROM DAVID:
- Send [count] CRs manually on LinkedIn
- Send [count] follow-up messages manually on LinkedIn
- Send [count] InMails manually on LinkedIn
- Engage with [count] posts (like/comment)
- Confirm what you sent so next session can update trackers
```

### 4d. Email Daily Report to the user
After sending the report via message_user, also email it to david.mitev@cognition.ai:
1. Open a new browser tab and navigate to https://mail.google.com/mail/?view=cm&to=david.mitev@cognition.ai
2. If not logged into Gmail, log in using LINKEDIN_EMAIL credentials (or navigate to Gmail compose directly)
3. Set the subject line to: "LinkedIn Pipeline Report -- [Date]"
4. Paste the same daily report content into the email body
5. Send the email
6. If Gmail login fails or is unavailable, fall back to sending only via message_user and note the email delivery failure in the report

### 4e. Handle Edge Cases
- If LinkedIn presented a CAPTCHA during login, note this in the report
- If any contacts appear to have left their company, flag them for removal and update BOTH trackers
- If a contact responds with interest in a meeting, flag as HIGH PRIORITY in the report and update Master Tracker stage to 7-Meeting Scheduled
- If messaging performance drops below 10% acceptance rate for any message type, recommend messaging changes

<verification>
- Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) has been updated with all stage transitions, metrics, and drafted copy
- LinkedIn Outreach State Tracker ({STATE_TRACKER_NOTE_ID}) has been updated with daily action log
- Daily report has been sent to the user via message_user with ALL drafted copy presented inline for manual sending
- Daily report has been emailed to david.mitev@cognition.ai (or failure noted)
- All edge cases have been handled and reported
- InMail drafts include pre-written copy for the user to send
- Manual mode account status included in report (if any)
- Messaging adaptation notes have been updated if new patterns emerged
- Report includes clear ACTION REQUIRED section telling the user what to send manually
</verification>
</phase>

## Specifications
- **THIS IS A READ-ONLY + DRAFT-ONLY PLAYBOOK. DEVIN DOES NOT SEND ANY MESSAGES, CONNECTION REQUESTS, OR INMAILS.**
- Each daily session should take approximately 1-2 hours (shorter since no sending/waiting)
- LinkedIn browsing is read-only (profile views, checking messages/acceptances, reading posts)
- All outreach copy is drafted and presented to the user for manual sending
- Business hours only: 8 AM - 5 PM Central time (no weekends)
- MANUAL MODE contacts are excluded from all outreach drafting
- Negative responses trigger immediate outreach stop + exclusion in BOTH trackers
- Messaging is adapted over time based on acceptance/response rate data
- Connection requests must be under 300 characters
- Never draft more than 1 message for the same person per day
- Daily report must be sent both via message_user AND emailed to david.mitev@cognition.ai
- BOTH the Pipeline Generation Master Tracker AND the LinkedIn Outreach State Tracker must be updated every session
- Check connection count before drafting LinkedIn CR: under 500 = draft email instead
- the user must confirm what he manually sent so trackers can be updated in the next session

## Advice and Pointers
- The Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) is the single source of truth for pipeline stages, alerts, and copy
- The LinkedIn Outreach State Tracker ({STATE_TRACKER_NOTE_ID}) has more granular daily action logs and messaging performance
- the user's Email Style Guide ({STYLE_GUIDE_NOTE_ID}) has personal connection triggers (alma mater TBD, Bears/Vikings/Cowboys, reading, photography, fitness, dogs, travel) and MedTech-specific social proof
- Bottom-up approach: prioritize Directors and Managers before VPs and C-suite
- If a contact's LinkedIn shows they've left the company, do NOT draft outreach, flag for review and update both trackers
- Profile viewing warm-up should happen for 3-5 days before recommending connection requests to a new account
- Stagger CR recommendations across accounts, don't recommend 4 to the same account in one day
- FDA-regulated software is a common thread across most of the user's accounts. Vulnerability remediation, compliance documentation, and test coverage are strong Devin use cases.
- When crafting new message drafts, match the informal, conversational tone from the Anti-AI Messaging Rules
- Each session should check LinkedIn message threads to see if the user manually sent any of the previously drafted messages, and update trackers accordingly

## Forbidden Actions
- **Do NOT send any LinkedIn messages, connection requests, InMails, or any other outreach. DRAFT ONLY.**
- **Do NOT click "Connect", "Message", "Send", "Reply", or any action button on LinkedIn (except for navigation and profile viewing)**
- Do NOT touch any MANUAL MODE contacts
- Do NOT exceed daily profile view limits (30-40 max)
- Do NOT skip updating the Pipeline Generation Master Tracker at session end, this is the source of truth
- Do NOT use dashes (em, en, or hyphens as dashes) in any drafted copy
- Do NOT draft identical/template messages for multiple contacts
- Do NOT draft more than 1 message for the same person per day
- Do NOT draft outreach for contacts who responded negatively
- Do NOT draft CRs without a personalized note
- Do NOT draft LinkedIn CRs for contacts with under 500 connections (draft email instead)
- Do NOT force push or rush if LinkedIn shows any warning signs

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
