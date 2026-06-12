---
name: daily-pipeline-alert-review
description: "Converted from Devin playbook: Daily Pipeline Alert & Review"
triggers:
  - user
  - model
---

# Daily Pipeline Alert & Review

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Daily Pipeline Alert & Review (playbook-3a02f9a7f3d7493ba79600a5dc20f214)

## Procedure

# Daily Pipeline Alert & Review

## Overview
This playbook runs a daily pipeline review for the user's target accounts. It reads the Pipeline Generation Master Tracker, identifies overdue and upcoming actions, builds the daily 60-touch plan per the 30-Touch Enterprise Outreach Strategy, prepares a batched alert summary, **composes ALL copy inline (LinkedIn DMs, CRs, InMails, cold emails, follow-ups, Day 2 value-adds, organic engagement comment angles)**, and surfaces the full plan with copy to the user. This is the canonical daily deliverable {REP_NAME} receives. It does NOT execute LinkedIn outreach itself.

**OWNERSHIP CHANGE :** Per the user's directive, this playbook now OWNS all copy composition for the Daily 60-Touch Plan. The Daily LinkedIn Outreach Automation playbook (`{LINKEDIN_OUTREACH_PLAYBOOK_ID}`) is downgraded to verify-and-write only (LinkedIn login + inbox/Sent/Connections sweep + profile-verification of new CRs + manual-activity detection + State Tracker write). All copy and plan composition lives here permanently.

**Slack removed :** Per the user's directive at 15:55 UTC, Slack delivery has been permanently removed from Phase 3. The alert is delivered via session `message_user` only. Do NOT attempt slack-remote MCP authorization or chat post.

**Pre-validation hardened ( REVISION 2, 17:21 UTC):** After 7 false items in the May 8 plan that should have been caught during Phase 2.5, this playbook now mandates explicit cross-references against Sent invitations, DECLINED list (3-week cooldown), InMail Sent log, employer-current verification, and a PERMANENT EXCLUSION LIST before any new CR / InMail / cold-email / organic-engagement proposal. See Phase 2.5 Step 1 (Pre-Validation, hardened) and the PERMANENT EXCLUSION LIST section.

## What's Needed From User
- No input needed, this runs automatically on schedule
- The user may provide updates via message (new acceptances, meetings booked, messaging changes)
- The user may need to approve a 2FA push for LinkedIn login during Phase 0 verification (see step 8)

## Knowledge Notes & IDs
- **Pipeline Generation Master Tracker**: {PIPELINE_TRACKER_NOTE_ID}
- **LinkedIn Outreach State Tracker**: {STATE_TRACKER_NOTE_ID}
- **30-Touch Enterprise Outreach Strategy**: {OUTREACH_STRATEGY_NOTE_ID} (canonical cadence, tiering, case study mapping, six-phase sequence A through F, Contact History Audit, reopen calendar, anti-pattern list)
- **Mandatory pre-validation rule**: {PREVALIDATION_RULE_NOTE_ID} (cross-reference before adding any contact to a touch list)
- **Gold Standard Outreach Copy Pattern (Blue Alozie Template)**: {GOLD_STANDARD_COPY_NOTE_ID} (8-item quality checklist; required reading before drafting any copy)
- **Anti-AI Messaging Rules + LinkedIn Channel Qualification + Tone Calibration + InMail Rule + No-Dashes Rule**: {ANTI_AI_RULES_NOTE_ID} (banned phrases, banned structures, voice calibration; required reading before drafting any copy)
- **No-dashes rule (reinforced)**: {NO_DASHES_NOTE_ID_1} and {NO_DASHES_NOTE_ID_2}
- **Devin Customer Proof Points**: {PROOF_POINTS_NOTE_ID} (verified case studies and metrics)
- **Devin Pre-Approved Customer List**: {PRE_APPROVED_CUSTOMERS_NOTE_ID} (verified customer names; no other customer may be cited)
- **Gmail Sent Folder Checker**: {GMAIL_CHECKER_NOTE_ID} (script and workflow for Gmail audit)
- **Slack Channel**: REMOVED  per the user's directive. Do not post to Slack.
- **LinkedIn credentials**: `LINKEDIN_EMAIL` and `LINKEDIN_PASSWORD` secrets (the user's account)
- **Gmail credentials**: `GMAIL_ADDRESS` and `GMAIL_APP_PASSWORD` secrets (for IMAP sent folder audit)

## PERMANENT EXCLUSION LIST

Maintain a permanent exclusion list of contacts who should never be included in any outreach. This list is populated as contacts are flagged by the user. Cross-reference this list BEFORE any new CR, InMail, cold email, profile view, or organic engagement is surfaced.

| Contact | Account (former) | Reason | Date Excluded | Verified By |
|---|---|---|---|---|
| _{Add contacts as they are flagged by the user}_ | | | | |


## Phase 0: Cross-Reference Individual Sessions Against Trackers (MANDATORY)

This phase ensures the Master Tracker is accurate BEFORE generating alerts. It was added Apr 9, 2026 after discovering that individual session decisions (go-quiet, stage changes, sent actions) were not propagating to the Master Tracker, causing stale or incorrect alerts. Extended Apr 20, 2026 to require LinkedIn-inbox verification,  to require LinkedIn profile-verification of new CRs.

1. Read the Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` `get`.
2. Read the LinkedIn Outreach State Tracker ({STATE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` `get`.
3. Use `devin_session_search` to find all recent sessions (last 48 to 72 hours) for the user's accounts and inspect for go-quiet decisions, stage changes, sent actions, new contacts, acceptances, manual activity {REP_NAME} performed outside of Devin.
4. Cross-reference findings against BOTH trackers. Use the MOST RECENT individual session decision as ground truth.
5. Document each discrepancy with contact name, what tracker shows, what actually happened, source session.
6. Send a non-blocking message to the user if significant discrepancies are found.

### Step 7 (concurrent-session coordination, MANDATORY)
If a Daily LinkedIn Outreach Automation session is already running for the same day (check `devin_session_search` for sessions tagged with that playbook), do NOT log into LinkedIn from this session. Inherit the concurrent session's inbox-sweep findings and profile-verify outcomes from the State Tracker. Document in the alert that LinkedIn verification was inherited from session ID X to avoid 2FA double-prompts and duplicate writes. The concurrent session owns the State Tracker write for that cycle.

**WARNING (added  17:21 UTC):** If the State Tracker body is corrupted (for example, contains a literal `file:///tmp/...` reference instead of actual content), do NOT inherit findings blindly. Treat concurrent-session inheritance as advisory not authoritative; verify high-stakes claims (new CRs, message thread sends, InMail sends, employer changes) by direct LinkedIn or Exa lookup in this session even if it triggers a 2FA push.

### Step 8: LinkedIn Inbox & Profile Verification (MANDATORY, expanded )

Devin session logs and tracker state can be out of date when {REP_NAME} sends LinkedIn messages manually, or when a session is flagged "NEVER WROTE BACK" (the agent did outreach but failed to update the trackers before exiting). The ONLY source of truth for whether a LinkedIn message was actually sent is the user's LinkedIn inbox. The ONLY source of truth for a new CR's account assignment is the contact's LinkedIn profile. **Do NOT delegate verification work that is accessible from LinkedIn to the user; verify it autonomously here.**

**How to run this step (when concurrent-session inheritance does not apply):**

1. Check for any prior outreach session flagged "NEVER WROTE BACK" since the last alert. If one exists, this step is especially critical, assume the tracker is missing sends and verify every LinkedIn-channel action in the queue.
2. Open Chrome and navigate to `https://www.linkedin.com/messaging/`. If not logged in, use `${LINKEDIN_EMAIL}` and `${LINKEDIN_PASSWORD}` secrets. If a 2FA push appears, send a non-blocking message to the user asking him to approve it on his mobile app, then wait ~30 seconds.
3. **Message thread verification.** For each contact in the DUE / OVERDUE LinkedIn action queue:
   - Use the top-level LinkedIn search bar (`I'm looking for...`) to locate the contact's profile. Do NOT use messaging-inbox search; it is unreliable.
   - Click `Message` to open the conversation pop-out.
   - Scroll to the most recent message and check who sent it (`{REP_NAME} Terrell, MBA` = sent by the user), the timestamp, and the content.
   - If {REP_NAME} sent a matching message within the tracker's due window or after the tracker's recorded `Last Touch`, the action is already complete. Note the exact date/time and move the contact from DUE to MONITORING.
   - If the most recent message is from the contact (not {REP_NAME}) and the tracker doesn't reflect it, flag as a missed response in Phase 2 corrections.
4. **Profile verification of new CRs.** For each new CR detected in the user's Sent invitations queue or Connections list since the last session, open the contact's LinkedIn profile and confirm:
   - **Current employer**: must match the account assignment in the tracker; do NOT infer from adjacent names in the Sent invitations view (this caused a previous misattribution where a contact was assigned to the wrong account).
   - **Current title**: confirm the role matches the tier-and-phase assumption.
   - **Connection count**: must be >=500 for LinkedIn channel eligibility (per LinkedIn Channel Qualification rule). If <500, route to email channel instead.
   - **Active posting cadence**: scan recent Activity for posts in the last 30 days; informs tier classification (Tier 1 requires active channel signal).
5. **Contact-detail verification for HOLD profile-verify backlog.** For each contact on the HOLD profile-verify backlog, open the LinkedIn profile and resolve account assignment + title + connection count + posting cadence in this session. Do not roll the contact forward to the next day if the profile is accessible from LinkedIn.
6. **Inbox sweep for the user's manual sends.** Open the Focused, Other, Connections, and InMail tabs in LinkedIn Messaging. Detect any send {REP_NAME} did manually outside of Devin since the last session. Log to Phase 2 corrections.
7. **Inbox sweep for inbound responses.** Detect any new positive, negative, or neutral responses from contacts. Flag negative responses immediately, add to exclusion list in BOTH trackers.
8. **InMail Sent log sweep (NEW ).** Open the InMail tab in LinkedIn Messaging. Capture every InMail sent in the last 14 days with contact name, date, and content theme. Required to detect already-sent InMails (this caught Beth Meese May 7 InMail too late in Revision 1, must catch in Phase 0 going forward).
9. **Sent invitations queue sweep (NEW ).** Open `https://www.linkedin.com/mynetwork/invitation-manager/sent/`. Capture every pending CR in the queue with contact name and account inference. Required to detect already-pending CRs before proposing new CRs (this caught Jay Hondadkatte too late in Revision 1, must catch in Phase 0 going forward).
10. **DECLINED CR list reconciliation (NEW ).** Cross-reference the State Tracker DECLINED list. Any contact whose CR was declined within the last 21 days is in mandatory CR cooldown. Document the cooldown clear date for each. Required to detect cooldown contacts before proposing new CRs (this caught Mark Lifson too late in Revision 1, must catch in Phase 0 going forward).
11. **Employer-current verification for organic-engagement targets and Tier 1 mid-sequence targets (NEW ).** For any contact proposed for Section E (organic engagement) or Section C (Tier 1 mid-sequence), if the last employer verification is more than 60 days old OR if The user has flagged the contact's employer status in any session, run an Exa profile lookup or LinkedIn profile read to confirm current employer. If the contact has left the targeted account, REMOVE entirely and add to PERMANENT EXCLUSION list (this caught {EXCLUDED_CONTACT} too late in Revision 1, must catch in Phase 0 going forward).
12. Record every correction in a Phase 0 findings list with contact name, account, tracker status (before), verified LinkedIn or Exa status (after), date and time of the send or change, and the conversation URL or profile URL.
13. Apply all verified corrections to the action queue BEFORE generating the alert in Phase 2.

### Step 9: Gmail Sent Folder Audit (MANDATORY when GMAIL_APP_PASSWORD is valid)

The LinkedIn inbox verification (step 8) catches LinkedIn messages {REP_NAME} sent manually. But email outreach sent directly from Gmail is invisible to Devin sessions unless explicitly checked. This step reads the user's Gmail Sent folder via IMAP to verify which email actions have already been completed.

1. Write the Gmail Sent Checker script to `/tmp/gmail_sent_checker.py` using the code from knowledge note "Gmail Sent Folder Checker" ({GMAIL_CHECKER_NOTE_ID}).
2. Run: `python3 /tmp/gmail_sent_checker.py --days 14 --format markdown --output /tmp/gmail_audit.md`.
3. If `GMAIL_APP_PASSWORD` is missing or rejected, send a non-blocking message asking {REP_NAME} to regenerate at https://myaccount.google.com/apppasswords and skip this step. Do NOT block the session.
4. Read the output report. Match emails to target accounts by recipient domain (the target account email domains configured for this pipeline).
5. For each contact in the tracker's email action queue, check if the email was already sent. Mark COMPLETE in Phase 0 corrections. Flag any RESPONSE RECEIVED replies. Discover untracked sends {REP_NAME} did manually.
6. Apply all Gmail-verified corrections to the action queue BEFORE generating the alert.

<verification>
- All recent sessions (last 48 to 72 hours) have been reviewed for decisions not yet in trackers
- Acceptance rates match between State Tracker and Master Tracker
- All go-quiet and exclusion decisions from individual sessions are captured
- All sent actions (InMails, emails, LinkedIn messages) are correctly reflected
- No actions listed as "due" that have already been completed
- LinkedIn inbox has been opened and every DUE/OVERDUE LinkedIn-channel action verified against actual conversation thread (step 8.3), OR concurrent-session coordination was applied and State Tracker findings inherited and the State Tracker body is not corrupted
- Every new CR in the Sent invitations queue has been profile-verified for employer + title + connection count (step 8.4), OR concurrent-session inheritance applied
- Every contact on the HOLD profile-verify backlog has been profile-verified or rolled forward only if profile genuinely cannot be opened
- InMail Sent log has been swept (step 8.8); no proposed InMail conflicts with an already-sent InMail in the last 7 days
- Sent invitations queue has been swept (step 8.9); no proposed new CR conflicts with an already-pending CR
- DECLINED CR list has been reconciled (step 8.10); no proposed new CR conflicts with a contact in 3-week post-decline cooldown
- Employer-current verification has been run for Section E and Section C contacts (step 8.11); no proposed organic engagement or Tier 1 mid-sequence touch goes to a contact who has left the targeted account
- Gmail Sent folder has been audited (step 9), OR audit failure logged with regen request to the user
- Any emails {REP_NAME} sent manually from Gmail that are NOT in the tracker have been discovered and added to corrections
- Any prior session marked "NEVER WROTE BACK" has triggered full LinkedIn verification of that session's expected outputs
- Any discrepancies have been documented and corrections prepared
- Cross-reference checklist from Master Tracker UPDATE PROTOCOL has been completed
</verification>
</phase>

<phase name="Load State" id="1">
## Phase 1: Load Current State

1. Read the Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` `get` (may already be loaded from Phase 0).
2. Read the LinkedIn Outreach State Tracker ({STATE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` `get` (may already be loaded from Phase 0).
3. Read the **30-Touch Enterprise Outreach Strategy** knowledge note ({OUTREACH_STRATEGY_NOTE_ID}). Required for Phase 2.5. Capture the per-account case study mapping, six-phase sequence (A through F), tier definitions (Tier 1 = 30 touches per 90 days, Tier 2 = 15 touches per 90 days, Tier 3 = 7 touches per 60 days), Contact History Audit rules, and the daily volume target (~60 touches: ~17 emails, ~8 LinkedIn DMs, ~4 new CRs, ~21 profile views, ~6 organic engagements).
4. Read the **Gold Standard Outreach Copy Pattern (Blue Alozie Template)** knowledge note ({GOLD_STANDARD_COPY_NOTE_ID}). Required for any copy drafted in Phase 2.5 step 8.
5. Read the **Anti-AI Messaging Rules** knowledge note ({ANTI_AI_RULES_NOTE_ID}). Required for any copy drafted in Phase 2.5 step 8.
6. Read the **Devin Customer Proof Points** ({PROOF_POINTS_NOTE_ID}) and **Devin Pre-Approved Customer List** ({PRE_APPROVED_CUSTOMERS_NOTE_ID}). Only customers on the pre-approved list may be cited.
7. Read account-specific knowledge notes for any accounts with active outreach.
8. Check if {REP_NAME} has sent any messages with updates (new acceptances, meetings booked, negative responses, stage changes).
9. Incorporate any corrections identified in Phase 0 into the working data, including LinkedIn-inbox verification, profile verification, InMail Sent sweep, Sent invitations sweep, DECLINED reconciliation, employer-current verification, and Gmail audit corrections.
10. Parse the ALERTS & TASKS QUEUE section to identify HIGH PRIORITY (24 to 48 hrs), MEDIUM PRIORITY (3 to 7 days), OVERDUE (past due), and MANUAL MODE account actions (informational only).
11. Build a tier-and-phase index of every active contact in the Master Tracker: Tier (1, 2, 3, Stage 1 provisional), Phase (A through F), Days since last touch, Touches Sent vs target, CR status (sent, accepted, declined, withdrawn, none).
12. Calculate days since last touch for all active contacts.
13. **Load PERMANENT EXCLUSION LIST** (top of this playbook). Any contact on this list is automatically excluded from every section of the plan.

<verification>
- Pipeline Generation Master Tracker has been read and parsed
- LinkedIn Outreach State Tracker has been read
- 30-Touch Enterprise Outreach Strategy has been read and the per-account case study mapping is loaded
- Gold Standard Outreach Copy Pattern has been read
- Anti-AI Messaging Rules + Tone Calibration + No-Dashes rule + InMail rule have been read
- Pre-Approved Customer List has been read
- All active contacts have been identified with current stages, tiers, phases, and days-since-last-touch
- Overdue actions count has been calculated
- Any user messages or updates have been incorporated
- Phase 0 corrections (including LinkedIn-inbox verification + profile verification + InMail Sent sweep + Sent invitations sweep + DECLINED reconciliation + employer-current verification + Gmail audit) have been incorporated into working data
- PERMANENT EXCLUSION LIST loaded
</verification>
</phase>

<phase name="Generate Alerts" id="2">
## Phase 2: Generate Alert Summary

1. Categorize all actions into priority buckets:
   - **OVERDUE** (red): past due
   - **DUE TODAY** (orange): due today
   - **DUE THIS WEEK** (yellow): due within 7 days
   - **UPCOMING** (blue): due next week
   - **MONITORING** (grey): sent but not yet responded; include send date and response-window close date
2. For each HIGH PRIORITY contact (Stage 4 with discovery hooks due), pull the pre-written personalized copy from the COPY BANK or note that copy must be drafted in Phase 2.5 step 8 if no pre-written copy exists.
3. For each MEDIUM PRIORITY contact (InMail eligible, CR pending past 7-day threshold), prepare InMail recommendation and note that copy must be drafted in Phase 2.5 step 8.
4. For MANUAL MODE account contacts, include status update but mark as MANUAL, NO AUTOMATION.
5. Calculate and include performance metrics (acceptance rate vs prior, response rate, best/worst performing accounts, stage distribution changes).
6. Identify any stale contacts (14+ days with no activity) and flag for review.
7. If Phase 0 found discrepancies, include a "DATA CORRECTIONS" section.
</phase>

<phase name="Build Daily 60-Touch Plan with Copy" id="2.5">
## Phase 2.5: Build Daily 60-Touch Plan + Compose Copy Inline (MANDATORY, hardened  17:21 UTC)

This phase produces the Daily 60-Touch Plan and **composes all copy inline**. As of , this playbook owns copy composition; the LinkedIn Outreach Automation playbook no longer drafts copy.

This phase is READ + ASSEMBLE + DRAFT only. It does NOT execute any touches. All copy is surfaced for the user's review per Phase 3.

### Step 1: Apply mandatory pre-validation (HARDENED  17:21 UTC, per {PREVALIDATION_RULE_NOTE_ID})

**Apply ALL of the following exclusions IN ORDER. Any contact failing any check is dropped from the plan with the verdict noted.**

#### A. PERMANENT EXCLUSION LIST check (NEW, mandatory first check)
Reject any contact on the PERMANENT EXCLUSION LIST at the top of this playbook. Verdict: REMOVED. As of  17:21 UTC: {EXCLUDED_CONTACT}, {EXCLUDED_CONTACT}, Lucas Glass.

#### B. Sent invitations queue check (NEW, mandatory before any new CR proposal)
For any contact proposed for a new CR (Section B SEND-CANDIDATE), verify the contact does NOT appear in the user's Sent invitations queue (from Phase 0 step 8.9). If a CR is already pending, verdict: PULL (CR already in flight, monitor for accept). Do NOT propose another CR.

#### C. DECLINED CR cooldown check (NEW, mandatory before any new CR proposal)
For any contact proposed for a new CR, verify the contact does NOT appear on the DECLINED list within the last 21 days (from Phase 0 step 8.10). If declined, verdict: HOLD (3-week cooldown post-decline). Document cooldown clear date and surface in Open Items for the user. Do NOT propose another CR until cooldown clears.

#### D. Connections list check
For any contact proposed for a new CR, verify the contact is NOT already in the user's Connections list (already-connected contacts should be in active sequence, not new-CR queue). If already connected, verdict: PULL or move to mid-sequence.

#### E. InMail Sent log check (NEW, mandatory before any new InMail proposal)
For any contact proposed for a new InMail (Section C InMail eligibility), verify the contact does NOT appear in the InMail Sent log within the last 7 days (from Phase 0 step 8.8). If an InMail is already in flight, verdict: PULL (InMail in flight, monitor for reply). Do NOT propose another InMail.

#### F. Employer-current check for organic engagement and Tier 1 mid-sequence (NEW, mandatory before any Section E or Section C touch)
For any contact proposed for Section E (organic engagement) or Section C (Tier 1 mid-sequence), verify current employer matches the targeted account (from Phase 0 step 8.11). If contact has left the targeted account, verdict: REMOVED. Add to PERMANENT EXCLUSION LIST.

#### G. Standard exclusions (existing rules, retained)
Reject any contact who is:
- On the Exclusion List (State Tracker)
- On the Go Quiet List (State Tracker)
- Flagged with an Email Bounce in the State Tracker
- Within the 3-week withdrawn-CR cooldown
- In MEETING DONE or MEETING SCHEDULED status (active deal contacts, not pipeline generation)
- A MANUAL MODE contact
- Fewer than 500 LinkedIn connections (route to email instead per LinkedIn Channel Qualification rule)

#### H. Already-sent action check (NEW, mandatory)
For any contact proposed in Section A (sends owed today), cross-reference Phase 0 LinkedIn message thread verification AND Gmail Sent folder audit. If the action was already sent within the active 7-day window, verdict: PULL (already sent, monitor for reply). Do NOT propose a re-send.

### Step 2: Run the Contact History Audit (per 30-Touch Strategy Phase 2.5 rules)

For every contact in the prospective send queue (after Step 1 pre-validation), extract from the Master Tracker plus LinkedIn-inbox verification:
- CR sent date, accepted/pending/declined, channel
- Every prior LinkedIn DM (date, sender, full text or theme summary, case study referenced, soft ask present)
- Every prior email (date, subject, theme, case study referenced, reply received)
- Every prior InMail (date, subject, theme, response received)
- Phase, Stage, Days In Sequence, Touches Sent, Last Touch Type, Last Touch Date, Days Since Last Touch, Case Study Assigned, Next LinkedIn, Next Email, Email Due
- Inbox-side verification: confirm last message date plus direction

Apply the validation rules from the 30-Touch strategy. Assign each prospective touch a verdict: SEND, SEND-REVISED, HOLD, PULL, REMOVED, or INVALID. Surface the audit table BEFORE any copy.

### Step 3: Build Section A, Sends owed today

Pull every action from Phase 2's OVERDUE and DUE TODAY buckets that has audit verdict SEND or SEND-REVISED. Group by channel: LinkedIn DMs (Touch 2 value-add, discovery hook, soft meeting ask), Cold emails Touch 1, the user's scheduled emails for today, any May +1 or +2 batch entries that can be safely pulled forward without violating "1 touch per person per day".

### Step 4: Build Section B, New CRs from latest audit waves

From the most recent Exa Audit Gap Reports (per account knowledge notes), select up to 4 net-new Wave 1 CR candidates with audit verdict SEND-CANDIDATE. Verify Step 1 pre-validation (PERMANENT EXCLUSION LIST + Sent invitations + DECLINED + Connections + Email Bounce + 3-week cooldown + MEETING + MANUAL MODE accounts + 500 connections), match to per-account case study mapping.

### Step 5: Build Section C, Tier 1 mid-sequence touches eligible today

From the tier-and-phase index, select Tier 1 contacts where Phase is B, C, D, or E, Days since last touch >=25, not in MONITORING with active response window, not in GO QUIET / EXCLUSION / MEETING / PERMANENT EXCLUSION. For each, recommend the next touch type per the six-phase sequence and match the case study to the account. Apply Step 1F (employer-current check) for any contact whose employer has not been verified within the last 60 days. Apply Step 1E (InMail Sent log check) for any InMail proposal.

Target: ~12 to 14 mid-sequence touches per day.

### Step 6: Build Section D, Profile views (~21)

Profile views are READ-ONLY pre-warming, always safe. Pre-warm Section A, B, C contacts plus the next ~5 Wave 1 audit candidates plus any Tier 1 in MONITORING with response window closing this week. Cap at 28 total. Exclude any contact on PERMANENT EXCLUSION LIST.

### Step 7: Build Section E, Organic engagement (~6)

Identify ~6 LinkedIn posts to like or comment on. Sources: Tier 1 contacts active in last 7 days, account leadership (CEO, CIO, CDO, CTO), MEETING SCHEDULED or MEETING DONE contacts who posted. Match comment angle to post topic and contact's current sequence phase. Apply Step 1F (employer-current check) MANDATORY for every Section E target; do NOT propose organic engagement for any contact whose current employer is not the targeted account.

### Step 8: Compose Copy Inline (MANDATORY, this playbook OWNS copy)

For every contact with audit verdict SEND or SEND-REVISED in Sections A, B, and C, AND for every SEND-CANDIDATE in Section B and Section F (tomorrow's send queue), draft inline copy. Section D (profile views) does not require copy. Section E (organic engagement) requires comment angles plus suggested seed phrasing.

Copy must follow the Gold Standard pattern ({GOLD_STANDARD_COPY_NOTE_ID}):
- Paragraph 1: Personal hook referencing a SPECIFIC career arc detail (not just current title)
- Paragraph 2: Pattern insight plus ONE social proof customer (matched from per-account case study mapping) with a SPECIFIC deployment detail
- Paragraph 3: Interest-based CTA naming their specific org/team/initiative

Copy must pass the 11-item Quality Checklist:
1. Does Paragraph 1 reference a SPECIFIC career arc detail from their profile?
2. Does Paragraph 2 contain exactly ONE social proof customer with a SPECIFIC deployment detail?
3. Does the CTA name their specific org/team/initiative?
4. Is this CTA framing different from other touches in the sequence?
5. Zero dashes (em, en, hyphen-as-dash)?
6. Zero banned phrases?
7. Customer from pre-approved list only?
8. Reads like 2 to 3 minute write, not 30 minutes?
9. **Angle rotation**: different angle, case study, or framing from the last 2 touches?
10. **Case study assigned**: matches the account's primary/secondary mapping?
11. **Channel rotation**: not 3+ same-channel touches in a row?

Copy must pass the Anti-AI Messaging Rules:
- No em dashes, en dashes, or hyphen-as-dashes anywhere
- No banned phrases: "Curious:", "keeping pace", "engineering surface area", "enormous", "must be significant/substantial", "fascinating", "impressive", "ambitious", "striking", "We've been helping [X] automate exactly that kind of [Y]", "Happy to share patterns/specifics/what's working if useful/relevant", "Quick question:" as transition, "No pressure either way", "figured it might be relevant"
- No template closers: "Will stay out of your inbox until you say otherwise", "Either way appreciate the connection", "final note", "last thought", "no worries either way". The 30-touch frame already handles re-engagement
- Conversational tone calibration: "gotta", "figure", "was wondering", "hell of a", thinking-out-loud phrasing, run-on thoughts, short punchy reactions
- No InMail may reference the CR send (per InMail rule)
- Pre-approved customers only (per Pre-Approved Customer List)
- No reference to former colleagues at an account if those colleagues have left (e.g., do NOT reference {EXCLUDED_CONTACT} in Providence copy after October 2025)

**Build-don't-restart final check.** Read the draft against the most recent prior touch. Confirm the draft references or builds on it (a thread {REP_NAME} already opened, an article they already saw, a specific claim {REP_NAME} made). If the draft reads as if {REP_NAME} has never spoken with this person, it is INVALID and must be rewritten with a bridge sentence that references the most recent prior touch ("following on the [topic] note from [date]", "saw you didn't have time on the original [thing], so").

For cold emails, include subject line + full body. Email format must be verified (do NOT draft if email format is unverified; surface as HOLD instead).

For CRs, include the personalized note (300 character limit) and identify the Day 2 case study angle for after acceptance.

For InMails, do NOT mention the CR send. Focus on showing value (relevant insight, case study, pattern observation tied to their role).

For organic engagement comments, provide direction not template, plus a suggested seed phrasing. {REP_NAME} applies his own voice.

### Step 9: Reconcile to ~60 and document gaps

Sum touches across A through E. Target band: 56 to 60. If under 56, expand Section C or D. If over 60, drop lowest-priority Section C touches (prefer keeping A, B, D intact).

Record constraints that prevented hitting 60 (pending data items, {REP_NAME} approvals, audit failures, profile-verify backlog, GO QUIET decisions overdue, contacts removed via PERMANENT EXCLUSION).

### Step 10: Compose the daily plan section in the alert

The daily report leads with the Contact History Audit table BEFORE any copy. Then Section A through E with copy inline for every SEND or SEND-CANDIDATE verdict. Copy is presented under each contact, not in a separate copy bank. Include a PERMANENT EXCLUSION LIST footer for visibility.

```
MAY [DATE], 2026 DAILY 60-TOUCH PLAN WITH COPY INLINE

CONTACT HISTORY AUDIT TABLE (Phase 2.5)
| Contact | Account | Last Touch | Days Since | Phase | Proposed Action | Verdict |
[One row per contact in today's prospective send queue. PULL/HOLD/REMOVED/INVALID rows include reason.]

Section A, Sends owed today ([count])
[For each SEND/SEND-REVISED verdict: contact, account, audit verdict, case study, builds-on note, then full copy inline.]

Section B, New CRs from latest audit Wave 1 ([count])
[For each SEND-CANDIDATE: contact, account, Day 2 case study, full CR personalized note (300 char limit) inline.]

Section C, Tier 1 mid-sequence touches eligible today ([count])
[For each SEND/SEND-REVISED: contact, account, phase, recommended touch, full copy inline (LinkedIn DM, InMail, cold email, etc.)]

Section D, Profile views ([count])
[Table: contact, account, pre-warm category. No copy needed.]

Section E, Organic engagement ([count])
[For each: contact, post topic, action (like/comment), comment angle, suggested seed phrasing.]

Section F, SEND-CANDIDATES TOMORROW (if applicable)
[For tomorrow's queue: full copy inline so {REP_NAME} can review now.]

PLAN GAPS AND CONSTRAINTS
[Pending data items, blocked contacts, audit failures, profile-verify backlog, PERMANENT EXCLUSION removals.]

PERMANENT EXCLUSION LIST (footer)
[Reproduced from playbook header so {REP_NAME} can see what is excluded today.]

DAILY 60-TOUCH PLAN TOTAL: [count] / 60
```

### Step 11: Compose the full batched alert message

Final alert structure (assembled in Phase 3):

```
Daily Pipeline Alert and Review, [Date]

[DATA CORRECTIONS (if any from Phase 0)]

ALERT SUMMARY
OVERDUE ([count]): [Contact @ Account, action, since date, copy status]
DUE TODAY ([count]): [Contact @ Account, action, copy status]
DUE THIS WEEK ([count]): [Contact @ Account, action, window]
MONITORING ([count]): [Contact @ Account, action sent date, response window closes date]
UPCOMING (next week)
STALE (14+ days)

PERFORMANCE METRICS
[Table: metric, current, prior, delta]

MAY [DATE] DAILY 60-TOUCH PLAN WITH COPY INLINE
[Full plan from Step 10 with copy inline for every SEND/SEND-CANDIDATE]

PERMANENT EXCLUSION LIST
[Reproduced from playbook header]

DEVIN REVIEW (Mode A Compliance)
[0 LinkedIn DMs, 0 CRs, 0 profile views, 0 likes, 0 comments, 0 InMails sent by Devin]

OPEN ITEMS FOR JOSEPH
[Numbered list of approvals, confirmations, decisions {REP_NAME} needs to make]
```

<verification>
- All overdue actions have been identified and categorized
- The Contact History Audit table has been built and leads the report (before any copy)
- Step 1 pre-validation applied to every contact in the plan, in order: A. PERMANENT EXCLUSION LIST, B. Sent invitations queue, C. DECLINED CR cooldown, D. Connections list, E. InMail Sent log, F. Employer-current, G. Standard exclusions, H. Already-sent action
- Per-account case study mapping applied to all Section B, C, F entries
- For every SEND or SEND-REVISED verdict in Sections A, B, C, F: full copy is drafted inline
- Every drafted copy passes the 11-item Quality Checklist
- Every drafted copy passes the Anti-AI Messaging Rules (no dashes, no banned phrases, no template closers, conversational tone, no references to departed colleagues)
- Every drafted copy uses a customer from the pre-approved list only
- Every drafted copy passes the build-don't-restart check (bridges from most recent prior touch)
- Plan total falls in the 56 to 60 touch band, OR the gap is documented in PLAN GAPS AND CONSTRAINTS
- Alert message is composed and ready to deliver
- No contact appears in DUE TODAY or OVERDUE if Phase 0 step 8 verified the message was already sent
- No contact appears in Section B as a new CR if a CR is already pending in Sent invitations
- No contact appears in Section B as a new CR if a CR was DECLINED in the last 21 days
- No contact appears in Section C as an InMail target if an InMail was sent in the last 7 days
- No contact appears in Section E as an organic engagement target if their current employer is not the targeted account
- No contact on PERMANENT EXCLUSION LIST appears anywhere in the plan
</verification>
</phase>

<phase name="Deliver Alert & Update Tracker" id="3">
## Phase 3: Deliver Alert & Update Tracker

1. **Slack: REMOVED**  per the user's directive. Do NOT attempt slack-remote MCP authorization or chat post.

2. **Send to the user via message_user**: Always send the full alert directly to the user. Include:
   - The complete alert summary with all categorized actions
   - The full Daily 60-Touch Plan from Phase 2.5 with all copy inline
   - The PERMANENT EXCLUSION LIST footer
   - Any recommendations for messaging adjustments
   - Clear action items: what {REP_NAME} needs to approve, confirm, or send manually
   - Any data corrections from Phase 0
   - Any plan gaps or constraints
   Attach the full plan as a markdown file (e.g. `/home/ubuntu/[date]_full_plan.md`) for easy reference and copy-paste.

3. **Update the Pipeline Generation Master Tracker**:
   - Update `Last Updated` timestamp
   - Update `Updated By` with this session's ID
   - Apply any corrections identified in Phase 0 (LinkedIn-inbox verification, profile verification, InMail Sent sweep, Sent invitations sweep, DECLINED reconciliation, employer-current verification, Gmail audit)
   - Recalculate due dates and overdue counts
   - Update performance metrics
   - Append a dated "DAILY 60-TOUCH PLAN WITH COPY" subsection (audit table + Section A through F + PLAN GAPS + PERMANENT EXCLUSION LIST footer) so the plan and copy persist across session boundaries
   - Add an entry to the DAILY ALERT LOG documenting the session, any corrections, the 60-touch plan total, and which contacts were LinkedIn-inbox verified, profile-verified, InMail-Sent-checked, Sent-invitations-checked, DECLINED-checked, or employer-checked
   - Write back via `devin_knowledge_manage` `update` ({PIPELINE_TRACKER_NOTE_ID})

4. **Update the LinkedIn Outreach State Tracker** if any changes were incorporated AND a concurrent Daily LinkedIn Outreach Automation session is NOT actively writing to it:
   - Apply any corrections from Phase 0
   - Update daily action log
   - For every LinkedIn-inbox-verified send, update the contact's Last Touch date and Follow-ups Sent field
   - For every profile-verified contact, update Profile-Verified to Y
   - For every contact added to PERMANENT EXCLUSION LIST, add to State Tracker Exclusion List with reason
   - Write back via `devin_knowledge_manage` `update` ({STATE_TRACKER_NOTE_ID})
   - If a concurrent session IS writing, skip and document in the Master Tracker that the State Tracker for this cycle is owned by the concurrent session
   - If the concurrent session State Tracker write is CORRUPTED (e.g. contains a literal `file:///tmp/...` reference instead of actual content), this session MUST take over the State Tracker write to repair the corruption

<verification>
- Slack post NOT attempted (REMOVED per the user's directive )
- Alert has been sent to the user via message_user with all copy AND the Daily 60-Touch Plan inline + attached as a markdown file
- PERMANENT EXCLUSION LIST footer is included in the alert
- Pipeline Generation Master Tracker has been updated with new timestamps, metrics, dated 60-touch plan section with copy, and Phase 0 corrections
- LinkedIn Outreach State Tracker has been updated if changes were made and no concurrent session conflict exists, OR repaired if the concurrent write was corrupted
- All deliverables have been presented directly to the user in the session
</verification>
</phase>

## Specifications
- This playbook is READ + ALERT + PLAN-ASSEMBLY + COPY COMPOSITION. It does NOT execute any LinkedIn actions. Read-only LinkedIn inbox verification + profile verification + InMail Sent sweep + Sent invitations sweep in Phase 0 step 8 is the ONLY exception.
- This playbook OWNS copy composition for the Daily 60-Touch Plan as of  (LinkedIn DMs, CRs, InMails, cold emails, follow-ups, Day 2 value-adds, organic engagement comment angles).
- All copy must be surfaced for the user's review, never auto-send.
- The Daily 60-Touch Plan with copy is built per the 30-Touch Enterprise Outreach Strategy ({OUTREACH_STRATEGY_NOTE_ID}) and persisted in the Master Tracker so it survives session boundaries.
- Mandatory pre-validation (Phase 2.5 Step 1, hardened  17:21 UTC) is applied to every contact in the plan in 8 stages (A through H). No exceptions.
- LinkedIn Channel Qualification rule (per {ANTI_AI_RULES_NOTE_ID}): no CRs to anyone with fewer than 500 LinkedIn connections. Route those to email.
- MANUAL MODE account contacts should be included in alerts for awareness only; do not suggest automated actions; do not include in the 60-touch plan.
- Phase 0 (Cross-Reference) is MANDATORY and must be completed before generating alerts.
- LinkedIn-inbox verification AND profile verification AND InMail Sent sweep AND Sent invitations sweep AND DECLINED reconciliation AND employer-current verification (Phase 0 step 8 sub-steps 3 to 11) are MANDATORY. Concurrent-session inheritance is acceptable IF the State Tracker body is not corrupted. **Do NOT delegate verification work to the user that is accessible from LinkedIn or Exa; verify it autonomously here.**
- Gmail Sent folder audit (Phase 0 step 9) is MANDATORY when `GMAIL_APP_PASSWORD` is valid. If rejected, log the failure and tell {REP_NAME} to regenerate. Do NOT block the session.
- All copy in the alert and in the 60-touch plan must pass: zero dashes (em, en, hyphen-as-dash), zero banned phrases, zero template closers, Gold Standard structure, pre-approved customer list only, build-don't-restart check, no references to departed colleagues.
- Slack delivery is REMOVED permanently. Alert delivery is via session `message_user` only.
- PERMANENT EXCLUSION LIST is loaded in Phase 1 and checked in Phase 2.5 Step 1A. Additions require {REP_NAME} directive OR verified employer-change OR verified "cannot be found on LinkedIn" repeated across two sessions. Removals require explicit {REP_NAME} directive only.

## Advice and Pointers
- The Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) is the single source of truth for pipeline state.
- **The actual LinkedIn inbox is the single source of truth for whether a LinkedIn message was sent**. Always trust the inbox over the tracker when they disagree.
- **The contact's LinkedIn profile is the single source of truth for account assignment, title, and connection count.** Always verify directly from the profile, never infer from adjacent names in the Sent invitations view.
- **the user's Gmail Sent folder is the single source of truth for whether an email was sent**. Always trust Gmail over the tracker when they disagree.
- **the user's LinkedIn InMail tab is the single source of truth for whether an InMail was sent**. Always trust the InMail tab over the tracker when they disagree.
- **the user's LinkedIn Sent invitations queue is the single source of truth for whether a CR is already pending**. Always trust the Sent invitations view over the tracker when they disagree.
- The LinkedIn Outreach State Tracker ({STATE_TRACKER_NOTE_ID}) has more granular daily action logs.
- Account-specific knowledge notes have deep research dossiers.
- The 30-Touch strategy note has the canonical per-account case study mapping. Do NOT improvise pairings.
- InMail threshold is 7 days after CR sent with no acceptance. Historic InMail response 0 of 31, recommend defer unless {REP_NAME} greenlights credit spend.
- Discovery hooks are due Day 5 to 7 after Day 2 value-add.
- Soft meeting asks are due Day 10 to 14 after the discovery hook (if no response).
- Tier 1 in active sequence: ~25 to 30 days between touches within 90-day window. Tier 2: ~6 days. Tier 3: ~8 to 9 days.
- Profile views and organic engagement are always safe; don't count against the daily-per-person touch cap.
- **CRITICAL**: Always cross-reference individual session decisions against the Master Tracker BEFORE generating alerts (Phase 0).
- **CRITICAL**: LinkedIn inbox search from within the messaging inbox is unreliable. Use the top-level global search bar.
- **CRITICAL**: When a Daily LinkedIn Outreach Automation session is running concurrently for the same day, do NOT log into LinkedIn from this session and do NOT write to the State Tracker. Inherit findings from the concurrent session instead. EXCEPTION: if the State Tracker body is corrupted, this session MUST verify directly and repair the State Tracker.
- **CRITICAL**: This playbook OWNS copy composition. The LinkedIn Outreach Automation playbook does NOT draft copy anymore. Do not delegate copy work to that playbook.
- **CRITICAL**: The PERMANENT EXCLUSION LIST is the first check in Phase 2.5 Step 1. Cross-reference EVERY proposed touch against this list before doing anything else.
- Go-quiet threshold under the 30-Touch framework: 10+ touches across 2+ channels over 60+ days with zero engagement. NOT 3 touches.
- 14-day stale threshold: contacts with no activity for 14+ days should be flagged for review against the 30-Touch sequence cadence (likely a missed touch, not a go-quiet candidate).

## Forbidden Actions
- Do NOT execute any LinkedIn actions (no profile views, no messages, no connection requests). Read-only inbox verification + profile verification + InMail Sent sweep + Sent invitations sweep in Phase 0 step 8 is the ONLY exception.
- Do NOT auto-send any copy. Always surface for the user's review.
- Do NOT post to Slack. Slack delivery has been REMOVED permanently per the user's  directive.
- Do NOT modify MANUAL MODE account contacts stages or actions. Do NOT include MANUAL MODE contacts in the 60-touch plan.
- Do NOT contact anyone on the exclusion list, go-quiet list, or PERMANENT EXCLUSION LIST.
- Do NOT include any contact with fewer than 500 LinkedIn connections in the LinkedIn or CR portions of the 60-touch plan.
- Do NOT modify account-specific knowledge notes. Only update the two tracker notes.
- Do NOT skip Phase 0. Cross-referencing individual sessions, LinkedIn-inbox verification, profile verification, InMail Sent sweep, Sent invitations sweep, DECLINED reconciliation, employer-current verification, and Gmail Sent folder audit are all mandatory before generating alerts (concurrent-session inheritance is acceptable for the LinkedIn step IF the State Tracker body is not corrupted; Gmail-failure logging is acceptable for the Gmail step).
- Do NOT skip Phase 2.5. The 60-touch plan with copy is the daily deliverable.
- Do NOT skip Phase 2.5 Step 1 pre-validation in any of its 8 stages (A through H). Each stage prevents a specific class of false-positive that has occurred in past plans.
- Do NOT delegate copy composition to the LinkedIn Outreach Automation playbook. That playbook is verify-and-write only as of .
- Do NOT delegate verification work to the user that is accessible from LinkedIn or Exa (message thread content, contact employer, contact title, contact connection count, contact posting activity, InMail send status, CR pending status, CR declined status). Verify autonomously.
- Do NOT propose a new CR for any contact whose CR is already pending in Sent invitations.
- Do NOT propose a new CR for any contact who declined a CR in the last 21 days.
- Do NOT propose a new InMail for any contact who already received an InMail in the last 7 days.
- Do NOT propose organic engagement for any contact whose current employer is not the targeted account.
- Do NOT use em dashes, en dashes, or hyphens-as-dashes anywhere in the alert or the 60-touch plan. This rule applies to every {REP_NAME} deliverable.
- Do NOT use any banned phrase from {ANTI_AI_RULES_NOTE_ID}.
- Do NOT use template closers ("either way appreciate the connection", "final note", "last thought", "no worries either way", "Will stay out of your inbox until you say otherwise", "Happy to send the link if useful", "figured it might be relevant").
- Do NOT cite any customer that is not on the pre-approved customer list.
- Do NOT pair a case study to an account in violation of the 30-Touch strategy's per-account case study mapping.
- Do NOT mention the CR send in any InMail (per InMail rule).
- Do NOT draft copy that fails the build-don't-restart check (must reference or build on the most recent prior touch).
- Do NOT reference former colleagues at an account (e.g., do NOT reference {EXCLUDED_CONTACT} in Providence copy after October 2025 since she left for Chartis).

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
