---
name: value-add-follow-up-for-zero-response-contacts
description: "Converted from Devin playbook: Value-Add Follow-Up for Zero-Response Contacts"
triggers:
  - user
  - model
---

# Value-Add Follow-Up for Zero-Response Contacts

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Value-Add Follow-Up for Zero-Response Contacts (playbook-19950fd7c03c49518327b2a76e64ac85)

## Procedure

# Value-Add Follow-Up Playbook for Zero-Response Contacts

## Purpose
Repeatable process for creating personalized value-add follow-up messages for contacts who have received initial outreach (connection requests, discovery hooks, emails, soft meeting asks) with zero responses. Uses external research to provide a genuinely useful reason to re-engage rather than "just following up."

## When to Use
- Contact has been in MONITORING for 7+ days with zero response
- Contact received cold email 7+ days ago with zero response
- Contact accepted LinkedIn connection but went silent after 2-3 touches
- Any contact where "just checking in" or "circling back" would feel pushy

## Step 1: Audit the Knowledge Base (Do This First)

1. Read the Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) to get current status of all contacts across all accounts
2. Read the LinkedIn Outreach State Tracker ({STATE_TRACKER_NOTE_ID}) for detailed execution logs and last-touch dates
3. Read each account's individual knowledge note for account-specific context
4. Identify contacts with zero responses whose response windows have closed (7+ days since last touch)

**Categorize contacts into:**
- READY NOW: 7+ days since last touch, zero response
- APPROACHING READY: 5-6 days, prep copy now
- STILL IN WINDOW: Under 5 days, do not touch
- GO QUIET: 3+ touches with zero response AND 14+ days stale

## Step 2: Exa Research (Per Account)

For each account, run 2-3 Exa searches:

**Search 1 (Account-specific news):**
```
Query: "[Company name] technology engineering AI [relevant keywords] 2025 2026"
Results: 10
```

**Search 2 (Industry-specific articles):**
```
Query: "[industry] engineering team capacity bottleneck AI tools 2025 2026"
Results: 8
```

**Search 3 (Cognition case studies):**
```
Query: "cognition.ai Devin case study enterprise customer results 2025 2026"
Results: 8
```

**What you're looking for:**
- Breaking company news (partnerships, product launches, acquisitions, leadership changes)
- Industry reports with specific statistics (percentages, benchmarks, survey findings)
- Published articles or interviews featuring the company's executives
- Relevant competitor or peer organization news
- Cognition case studies that map to the contact's domain

**Quality filter:** Each article must have at least ONE specific stat, quote, or finding that can anchor a follow-up message. Generic "AI is transforming X" articles are not useful.

## Step 3: Map Articles to Contacts

For each zero-response contact, find the ONE best article based on:

1. **Role relevance:** Does the article speak to their specific function (QA, infrastructure, security, data, etc.)?
2. **Recency:** Prefer articles from the last 30-60 days. Fresh news > old reports.
3. **Specificity:** Articles with named stats, quotes, or findings beat general trend pieces.
4. **Different angle:** The article must provide a DIFFERENT hook from the original outreach. Do not reuse the same topic.
5. **Conversation starter:** The article should naturally lead to a question the contact can answer from their own experience.

## Step 4: Write Follow-Up Messages

### For LinkedIn Messages (connected contacts):

**Structure:** 2-3 short paragraphs. Reference the specific article. Ask ONE question tied to their role/experience.

**Template pattern (vary across messages):**
- Pattern A: [Name], [article reference + specific stat]. [Bridge to their world]. [Question about their experience].
- Pattern B: [Name], [article reference]. [What caught your eye and why]. [Question tied to their role].
- Pattern C: [Name], [recent company news]. [Observation about what it means for their function]. [Question].

### For Email Follow-Ups (cold email non-responders):

**Structure:** Subject line (1-4 words, lowercase). 3-5 sentences total. Different angle from original email. ONE article/stat reference. Interest CTA.

**Template pattern:**
```
Subject: [2-3 words, lowercase, no AI/ROI/Platform]

Hi [Name],

[1-2 sentences referencing specific article/stat/news tied to their company or role].

[1 sentence bridging to engineering capacity or tooling question].

[Interest CTA — "Worth comparing notes?" / "Curious if your team is seeing the same" / "If that resonates, happy to chat."]

Joseph
```

### For C-Suite Follow-Ups:

**Structure:** Reply-style subject ("re: [original subject]"). Shorter than original email. ONE new data point or article. Tie back to original hook without repeating it.

## Step 5: Quality Verification

Run EVERY message through this checklist before delivery:

**Anti-AI Messaging Rules:**
- [ ] Zero dashes of any kind (em dashes, en dashes, hyphens used as dashes)
- [ ] No banned phrases ("Curious:" as transition, "keeping pace," "engineering surface area," "enormous," "impressive," "ambitious," "striking," "Happy to share patterns/specifics," "No pressure either way," "figured it might be relevant")
- [ ] Different structure across messages in same batch
- [ ] Feels like a 2-3 minute write (contractions, varied sentence length, conversational)

**Gong Standards:**
- [ ] Email subject lines: 1-4 words, lowercase
- [ ] No "AI," "ROI," or "Platform" in subject lines
- [ ] Interest CTAs (not time CTAs)
- [ ] No apologetic language ("just checking in," "circling back")

**Content Rules:**
- [ ] Each message references ONE specific article/stat/insight
- [ ] No repeated phrases across messages in same batch
- [ ] Pre-approved customer names only (Citi, Microsoft, Infosys, Anduril, Goldman Sachs, Ramp, Mercedes-Benz, U.S. Army, Cognizant, Cisco, U.S. Navy, Rivian, Volkswagen Group Technologies, Santander, Dell, Lowe's, Nu, Booking.com, Zillow, Mercado Libre, Cloudflare, NVIDIA, Exa, MongoDB, Mitsubishi Corporation, athenahealth)
- [ ] Cognition case studies NOT included in cold follow-ups (save for reply threads)
- [ ] LinkedIn messages not sent to contacts with <500 connections (use email instead)

## Step 6: Organize Delivery

Group messages by:
1. **SEND NOW** (response window closed, 7+ days)
2. **SEND SOON** (approaching ready, 5-6 days, include date)
3. **HOLD** (still in window, include target date)
4. **GO QUIET** (do not send, explain why)

Include timing table at the end of each account section.

## Step 7: Case Study Mapping (for Reply Threads)

Map relevant Cognition case studies to each contact for use ONLY when they respond:

| Case Study | Best For | Key Stat |
|-----------|---------|---------|
| Nubank ETL Migration | Data/analytics contacts | 12x efficiency, 20x cost savings |
| COBOL Modernization | Healthcare, enterprise apps, legacy | 73% cost reduction |
| Infosys Partnership | All (expanding to healthcare) | Largest agentic engineering deployment |
| Visma Cloud Migration | Cloud/infrastructure contacts | 50% cost reduction, 2x productivity |
| Citation Group | Engineering output contacts | 271 PRs merged, 80% merge rate |
| Linktree | Platform/product contacts | ~300 PRs authored, ~100 merged in 1 month |

## Reference: Knowledge Note IDs

- Pipeline Generation Master Tracker: {PIPELINE_TRACKER_NOTE_ID}
- LinkedIn Outreach State Tracker: {STATE_TRACKER_NOTE_ID}
- Gong Cold Email Best Practices: {GONG_EMAIL_NOTE_ID}
- Anti-AI Messaging Rules: {ANTI_AI_RULES_NOTE_ID}
- CTO-Tested Email Framework: {EMAIL_FRAMEWORK_NOTE_ID}
- Pre-Approved Customer List: {PRE_APPROVED_CUSTOMERS_NOTE_ID}
- {ACCOUNT_NAME} Account Knowledge: {ACCOUNT_KNOWLEDGE_NOTE_ID}
- {ACCOUNT_NAME} Account Knowledge: {ACCOUNT_KNOWLEDGE_NOTE_ID}
- (Load the relevant account knowledge notes for each target account)
- Account-specific knowledge notes (load the relevant account knowledge note for each target account)
- (Load the relevant account knowledge notes for each target account)

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
