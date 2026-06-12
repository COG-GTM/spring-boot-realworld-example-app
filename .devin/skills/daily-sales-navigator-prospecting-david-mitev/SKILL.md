---
name: daily-sales-navigator-prospecting-david-mitev
description: "Converted from Devin playbook: Daily Sales Navigator Prospecting (David Mitev)"
triggers:
  - user
  - model
---

# Daily Sales Navigator Prospecting (David Mitev)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Daily Sales Navigator Prospecting (David Mitev) (playbook-9b619007702e4cfd84171f9abcac0534)

## Procedure

# Daily Sales Navigator Prospecting

## Overview
This playbook runs daily at 4:30 PM Central. It logs into LinkedIn Sales Navigator to perform two core prospecting functions:
1. **Monitor existing contacts** for new posts, role changes, and engagement signals
2. **Identify 5 new high-value contacts** (C-level to VP) at target accounts who would find Devin/Cognition messaging relevant

All contact discovery is enriched using the **Exa MCP** (`exa-mcp-server`) to tie persona interests, company initiatives, and public signals to Cognition's value proposition.

**Operating Mode: READ-ONLY / SURFACE-ONLY.** Devin does NOT send any LinkedIn messages, connection requests, InMails, or profile views beyond what is needed for research. David executes all LinkedIn actions manually after reviewing Devin's findings.

## What's Needed From User
- LinkedIn credentials (`LINKEDIN_EMAIL`, `LINKEDIN_PASSWORD`) must be saved as secrets
- 2FA approval from David (push notification on his LinkedIn mobile app) when prompted
- David may provide updates on target accounts, new verticals, or priority shifts via message

## Knowledge Notes & IDs
- **David's Outreach Style Guide & Personal Connection Triggers**: `note-794caf9e0b7a4ba2a23f11185ab91372`
- **Org-wide Anti-AI Messaging Rules**: `note-f438713b823e4909a6b9fb6ec479a8d9`
- **Org-wide Pre-Approved Customer List**: `note-123639b7a4d24189be20537cd942bf18`
- **Devin Use Case Mapping**: `note-79f42532eb13491f8c1d38cdaa9621c6`
- **Devin Customer Proof Points**: `note-aa41f355095d46418ff0b2be4ab97095`
- **Org-wide Email Style Guide**: `note-852aecfc7a724219b56fd62be89ed9b6`
- **Exa MCP Prospecting Guide**: `note-e919cdd69d40431ca10f73e26930cfcf`
- **Gold Standard Outreach Copy Pattern**: `note-c87eceae73ad4400b23b8c8722727846`
- **Gong Cold Email Best Practices**: `note-80af3a885af0439492d562e94f1c605a`
- **30-Touch Enterprise Outreach Strategy**: `note-705345edd3844e7093389fd9f03fe173` (reference for tiering logic and sequencing)
- **LinkedIn credentials**: `LINKEDIN_EMAIL` and `LINKEDIN_PASSWORD` secrets

## Target Accounts & Verticals

David covers MedTech, Life Sciences, and Supply Chain/Distribution:

| Account | Vertical | Primary Social Proof |
|---|---|---|
| Abbott | MedTech | athenahealth, Microsoft, NVIDIA |
| Boston Scientific | MedTech | athenahealth, Microsoft, NVIDIA |
| Medtronic (MiniMed) | MedTech | athenahealth, Microsoft, NVIDIA |
| Stryker | MedTech | athenahealth, Microsoft, NVIDIA |
| Thermo Fisher Scientific | Life Sciences | athenahealth, Microsoft, NVIDIA |
| Medline | Supply Chain / Distribution | Lowe's, Booking.com, Microsoft |

## Contact Discovery Criteria

### Title Targeting (Bias Senior)
Prioritize in this order:
1. **C-level**: CTO, CIO, CISO, CDO, Chief Digital Officer, Chief Data Officer
2. **SVP/EVP**: SVP Engineering, EVP Technology, SVP Digital, SVP R&D
3. **VP**: VP Engineering, VP Platform, VP Software Development, VP Digital, VP IT, VP Data, VP AI/ML, VP Cloud, VP Infrastructure, VP Quality Engineering
4. **Senior Director**: Senior Director Engineering, Senior Director Platform, Senior Director Digital Transformation (include only if C-level/VP coverage is thin for that account)

### Relevance Filters
A contact is relevant if **any 2** of these are true:
- Their org has a publicly known technology initiative (migration, modernization, AI adoption, platform build)
- Their title maps to a Devin use case (see Use Case Mapping note)
- They have publicly spoken/written about engineering productivity, AI, automation, or modernization
- They have 500+ LinkedIn connections AND post regularly (active on LinkedIn)
- There is a personal connection trigger (UIUC, Big Ten, sports, reading, fitness, photography, dogs, travel)

### Disqualification Rules
- Fewer than 500 LinkedIn connections (not viable for LinkedIn outreach; flag for email-only)
- Already in David's pipeline or tracker (cross-reference before surfacing)
- On any exclusion or go-quiet list
- At a company division with no engineering/technology relevance

<phase name="State Loading & Login" id="1">
## Phase 1: State Loading & Sales Navigator Login

1. **Read David's Outreach Style Guide** (`note-794caf9e0b7a4ba2a23f11185ab91372`) for personal connection triggers, MedTech social proof, and use case angles.
2. **Read the Exa MCP Prospecting Guide** (`note-e919cdd69d40431ca10f73e26930cfcf`) for query patterns and research workflow.
3. **Read the Pre-Approved Customer List** (`note-123639b7a4d24189be20537cd942bf18`) and **Devin Customer Proof Points** (`note-aa41f355095d46418ff0b2be4ab97095`) for social proof assets.
4. **Read the Use Case Mapping** (`note-79f42532eb13491f8c1d38cdaa9621c6`) for mapping prospect priorities to Devin capabilities.
5. Check if David has sent any messages with updates (new accounts, priority shifts, contacts to avoid).
6. Open Chrome and navigate to `linkedin.com/sales` (Sales Navigator).
7. Log into LinkedIn using the saved `LINKEDIN_EMAIL` and `LINKEDIN_PASSWORD` credentials.
8. If a CAPTCHA or 2FA challenge appears, message David immediately and ask him to complete it via mobile / Desktop tab. Wait for confirmation before proceeding.
9. Verify you are on the Sales Navigator home page and logged in.

<verification>
- David's Style Guide, Exa Guide, Pre-Approved Customer List, Proof Points, and Use Case Mapping have all been read
- Any user messages/updates have been incorporated
- LinkedIn Sales Navigator login is successful and the Sales Navigator home page is visible
- If CAPTCHA or 2FA appeared, David has completed it
</verification>
</phase>

<phase name="Monitor Existing Contacts" id="2">
## Phase 2: Monitor Existing Contacts for Activity & Role Changes

This phase checks David's existing Sales Navigator saved leads/accounts for signals that create outreach opportunities.

1. **Navigate to Sales Navigator Alerts/Notifications feed.** This shows recent activity from saved leads: job changes, posts, shares, company news.
2. **Scan for role changes:**
   - Any saved lead who changed titles (promotion, lateral move, new company)
   - Any saved lead who moved to a NEW company (potential new account or lost contact)
   - Record: Name, Old Title, New Title, Old Company, New Company, Date of change
3. **Scan for relevant posts and shares:**
   - Any saved lead who posted or shared content related to: engineering productivity, AI adoption, legacy modernization, platform engineering, cloud migration, digital transformation, developer tools, automation, or hiring/scaling engineering teams
   - For each relevant post: record the Name, Post summary (2-3 sentences), Post URL, and why it is relevant to Devin/Cognition messaging
   - Ignore posts about: personal milestones unrelated to tech (birthdays, anniversaries), generic industry news with no engineering angle, HR/culture posts with no technology tie
4. **Scan for company news alerts:**
   - Any saved account with news about technology investments, acquisitions, leadership changes, regulatory changes, or digital transformation initiatives
   - Record: Company, News summary, Source URL, Relevance to Devin
5. **Cross-reference with Exa for deeper context on any high-signal findings:**
   - For contacts who changed roles: run `web_search_exa` with `category:people [Name] [New Company]` to get full context on their new role
   - For contacts who posted relevant content: run `web_fetch_exa` on the post URL (if available) to extract full content, then run `web_search_exa` with `"[Company] [topic from post]"` to find related company initiatives
   - For company news: run `web_search_exa` with `"[Company] [initiative keyword] 2025 2026"` to find corroborating coverage

<verification>
- Sales Navigator alerts/notifications feed has been reviewed
- All role changes among saved leads have been recorded
- All relevant posts and shares have been recorded with summaries and URLs
- All relevant company news alerts have been recorded
- Exa cross-referencing has been performed for high-signal findings
- Irrelevant posts (personal milestones, generic news) have been filtered out
</verification>
</phase>

<phase name="Identify New Contacts" id="3">
## Phase 3: Identify 5 New High-Value Contacts

This phase uses Sales Navigator search + Exa MCP to discover 5 new contacts who would find Devin/Cognition messaging relevant.

### Step 1: Sales Navigator Lead Search
1. Navigate to Sales Navigator Lead Search.
2. For each target account (Abbott, Boston Scientific, Medtronic, Stryker, Thermo Fisher, Medline), run a search with these filters:
   - **Company**: Target account name
   - **Seniority Level**: CXO, VP, Director (prioritize CXO and VP)
   - **Function**: Engineering, Information Technology, Operations (adjust based on account)
   - **Posted on LinkedIn**: Past 30 days (to ensure they are active)
   - **Connection degree**: 2nd and 3rd degree (not already connected)
3. Review results and shortlist candidates whose titles match the Title Targeting criteria.
4. For each shortlisted candidate, open their Sales Navigator profile and record:
   - Full name
   - Current title and company
   - Number of LinkedIn connections
   - Posting frequency (last 3 posts with dates)
   - Education (check for UIUC, Big Ten, or D1 school)
   - Interests/hobbies visible on profile (sports, reading, photography, fitness, dogs, travel)
   - Mutual connections
   - Any recent role change

### Step 2: Exa Deep Research on Shortlisted Contacts
For each shortlisted contact (aim for 8-10 candidates to narrow down to 5):

1. **Person research**: Run `web_search_exa` with `category:people [Full Name] [Company]` to find:
   - Conference talks, blog posts, podcasts, interviews
   - Technical interests and expertise areas
   - Career trajectory and previous roles
   - Public statements about engineering priorities

2. **Company initiative research**: Run `web_search_exa` with:
   - `"[Company] technology initiative 2025 2026"` to find active engineering programs
   - `"[Company] engineering modernization OR migration OR platform OR AI"` to find specific projects
   - `category:company [Company]` for structured company data

3. **Use Exa to tie persona to company initiative**: For each contact, answer:
   - What specific company initiative does this person likely own or influence?
   - Which Devin use case maps to that initiative? (Reference the Use Case Mapping note)
   - What social proof customer is most relevant? (Reference the Pre-Approved Customer List)
   - Is there a personal connection trigger? (UIUC, Big Ten, sports, etc.)

### Step 3: Final Selection (5 Contacts)
From the shortlist, select the 5 strongest candidates based on:
1. **Title seniority** (C-level > VP > Sr. Director)
2. **Initiative alignment** (contact's work maps directly to a Devin use case with Exa evidence)
3. **LinkedIn activity** (500+ connections, posts in last 30 days)
4. **Personal connection triggers** (bonus points, not required)
5. **Account coverage balance** (spread across accounts; don't pick 5 from the same company)

Disqualify any contact who:
- Has fewer than 500 connections
- Is already in David's pipeline (check with David if unsure)
- Is at a division with no engineering/technology function
- Has no Exa evidence tying them to a relevant initiative

<verification>
- Sales Navigator Lead Search has been run for each target account
- At least 8-10 candidates have been shortlisted from Sales Navigator
- Exa deep research has been performed on each shortlisted candidate (person + company initiative)
- Each candidate has a mapped Devin use case and social proof customer
- Exactly 5 contacts have been selected with clear rationale
- All 5 have 500+ connections and are active on LinkedIn
- Personal connection triggers have been checked for all 5
- Account coverage is balanced (not all from one company)
</verification>
</phase>

<phase name="Build Contact Intelligence Briefs" id="4">
## Phase 4: Build Contact Intelligence Briefs

For each of the 5 new contacts, build a structured intelligence brief that David can use for outreach.

### Brief Format (per contact)

```
## [Full Name] | [Title] | [Company]

**LinkedIn**: [Sales Navigator profile URL]
**Connections**: [count]
**Recent Activity**: [Active/Moderate/Low] ([last post date])
**Education**: [School] [Personal trigger tier if applicable]

### Why This Contact
[2-3 sentences explaining why this person is a strong prospect. Reference their specific role, the company initiative they likely influence, and why Devin is relevant to their world.]

### Company Initiative (Exa-sourced)
[1-2 sentences describing the specific company initiative this person maps to, with source URL from Exa research.]

### Devin Use Case Match
[The specific Devin use case(s) that map to this person's priorities, from the Use Case Mapping note.]

### Recommended Social Proof
[The 3 pre-approved customers most relevant to this contact's industry/role, plus the single case study with deployment detail for the personalization paragraph.]

### Personal Connection Triggers
[Any triggers found: alma mater, sports, hobbies, mutual connections. "None found" if none.]

### Suggested Outreach Angle
[1-2 sentences describing how David should approach this person. Reference the Gold Standard Copy Pattern: Paragraph 1 (specific career arc detail), Paragraph 2 (pattern insight + single social proof with deployment detail), CTA (names their org/team/initiative). Do NOT draft full copy here; just describe the angle.]
```

<verification>
- All 5 contacts have complete intelligence briefs in the format above
- Every brief includes a Sales Navigator profile URL
- Every brief includes Exa-sourced company initiative with a source URL
- Every brief maps to a specific Devin use case
- Every brief includes 3 recommended social proof customers from the pre-approved list
- Personal connection triggers have been checked for each contact
- Suggested outreach angles follow the Gold Standard Copy Pattern structure
- No dashes (em, en, or hyphen-as-dash) appear in any of the brief text
</verification>
</phase>

<phase name="Compile & Deliver Daily Report" id="5">
## Phase 5: Compile & Deliver Daily Prospecting Report

Compile all findings into a single daily report and deliver to David via `message_user`.

### Report Structure

```
Sales Navigator Prospecting Report, [Date] [Time CT]

Session: [session ID]
Mode: Read-only / Surface-only

---

SECTION 1: EXISTING CONTACT SIGNALS

Role Changes: [count]
[For each: Name | Old Title @ Old Company -> New Title @ New Company | Date | Outreach implication]

Relevant Posts: [count]
[For each: Name | Post summary (2-3 sentences) | Why relevant | Suggested response angle]

Company News: [count]
[For each: Company | News summary | Source URL | Relevance to outreach]

---

SECTION 2: 5 NEW CONTACTS

[Full intelligence brief for each contact, using the format from Phase 4]

---

SECTION 3: ACCOUNT COVERAGE SUMMARY

| Account | Existing Leads (Sales Nav) | New Contacts Today | Total Signals Today |
|---|---|---|---|
| Abbott | [count] | [count] | [count] |
| Boston Scientific | [count] | [count] | [count] |
| Medtronic | [count] | [count] | [count] |
| Stryker | [count] | [count] | [count] |
| Thermo Fisher | [count] | [count] | [count] |
| Medline | [count] | [count] | [count] |

---

NEXT STEPS
[Any recommended actions: contacts to add to pipeline, posts to engage with, role changes to act on, accounts needing more coverage]
```

Deliver this report via `message_user` with `block_on_user=true`. Also attach it as a `.md` file so David can reference it later.

<verification>
- Daily report includes all 3 sections: Existing Contact Signals, 5 New Contacts, Account Coverage Summary
- Report has been sent to David via message_user with the .md file attached
- All URLs in the report are real and sourced from Sales Navigator or Exa (no fabricated links)
- No dashes (em, en, or hyphen-as-dash) appear anywhere in the report
- Report follows the Anti-AI Messaging Rules in any suggested angles or copy
</verification>
</phase>

## Specifications
- This playbook is READ-ONLY on LinkedIn Sales Navigator. No messages, CRs, InMails, likes, comments, or profile views beyond research are executed.
- All contact discovery must be enriched with Exa MCP before surfacing to David. Do not surface contacts with only a Sales Navigator profile; always run Exa research to find the initiative tie.
- Title bias is toward seniority: C-level > VP > Sr. Director. Do not surface Director-level or below unless C-level/VP coverage is exhausted for an account.
- Every contact must have 500+ LinkedIn connections to be surfaced for LinkedIn outreach. Contacts with fewer than 500 connections should be flagged as email-only candidates.
- Outreach angles must follow the Gold Standard Copy Pattern (note-c87eceae73ad4400b23b8c8722727846): specific career arc detail in P1, single social proof with deployment detail in P2, CTA that names their org/team/initiative.
- All social proof must come from the pre-approved customer list (note-123639b7a4d24189be20537cd942bf18). Never fabricate customer relationships.
- Personal connection triggers (UIUC, Big Ten, sports, reading, photography, fitness, dogs, travel) should be checked for every contact and noted in the brief.
- 5 new contacts per day minimum. Spread across accounts; do not over-index on one company.
- This playbook complements (does not replace) the Daily LinkedIn Outreach Automation playbook. That playbook handles inbox sweeps, tracker writes, and outreach execution support. This playbook handles upstream prospecting and contact discovery.

## Advice and Pointers
- Sales Navigator's alerts feed is the fastest way to surface role changes and post activity for saved leads. Check it before running manual searches.
- Exa `category:people` queries work best with natural language descriptions like "VP of Engineering at Abbott medical devices" rather than keyword strings.
- When Exa returns a LinkedIn profile URL, use `web_fetch_exa` to pull the full profile content including career history, education, and skills.
- If Sales Navigator search returns too many results, add the "Posted on LinkedIn in Past 30 days" filter to focus on active prospects.
- David's strongest personal trigger is UIUC (Tier 1). Big Ten schools are Tier 2. Always check education.
- For MedTech accounts, the strongest Devin use cases are: vulnerability remediation at scale, regression test coverage expansion, compliance documentation generation, and FDA-regulated software testing.
- For Medline (supply chain), use cases shift to: platform modernization, CI/CD at scale, infrastructure-as-code, and test coverage expansion.
- If 2FA push fails or times out, send a non-blocking message to David and skip the LinkedIn-dependent steps. Deliver an Exa-only report with whatever research can be done without Sales Navigator access.

## Forbidden Actions
- Do NOT send any LinkedIn messages, connection requests, InMails, likes, comments, or endorsements. This playbook is read-only.
- Do NOT fabricate LinkedIn profile URLs. Only include URLs directly sourced from Sales Navigator or Exa.
- Do NOT fabricate customer relationships. Only cite customers from the pre-approved list.
- Do NOT surface contacts with fewer than 500 connections for LinkedIn outreach without flagging them as email-only.
- Do NOT use dashes (em, en, or hyphen-as-dash) in any outreach angle, brief text, or report copy.
- Do NOT use banned phrases from the Anti-AI Messaging Rules (note-f438713b823e4909a6b9fb6ec479a8d9).
- Do NOT surface contacts who are already on an exclusion list, go-quiet list, or active in David's pipeline without flagging the conflict.
- Do NOT skip Exa enrichment. Every new contact must have Exa-sourced initiative research before being included in the report.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
