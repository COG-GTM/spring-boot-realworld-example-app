---
name: apac-enterprise-outreach-playbook
description: "Converted from Devin playbook: APAC Enterprise Outreach Playbook"
triggers:
  - user
  - model
---

# APAC Enterprise Outreach Playbook

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: APAC Enterprise Outreach Playbook (playbook-5f582572d9ba44918796be6c07b4955c), macro `!apac_outreach`

## Procedure

# APAC Enterprise Outreach Playbook

## Overview
Generate personalized, research-backed email sequences (3-email cadence) and LinkedIn InMail campaigns (5 per contact, biweekly) for Cognition.ai sales reps targeting APAC enterprise prospects. All outputs are compiled into a **.docx file** with formatted tables. Each outreach is presented in a document table with Message Type, To, Subject, Email Body, and Source Reference & Explainability columns. The user will sanity-check before each send. Emails are signed "Regards, Louis."

## What's Needed From User
- **Prospect Contact File** (.xlsx or .csv) with columns: `Name`, `Title`, `Account`, `LinkedIn`, `Colleagues`, `Email`
- **Devin Proof Points file** (.xlsx) containing customer case studies, stats, and referenceability status
- **Pitch Decks** (PDF/PPTX) with company-specific positioning, use cases, and security content
- Any special instructions per account (e.g., "Devin is being evaluated at Standard Chartered for SpringBoot upgrades")
- Email cadence dates (default: Email 1 on specified start date, Email 2 at +4 business days, Email 3 at +4 more business days)
- LinkedIn InMail start date and frequency (default: every 2 weeks)

Only ever create the todo list for the current phase.

---

<phase name="Research and Intelligence Gathering" id="1">
## Phase 1: Research and Intelligence Gathering

### Step 1: Parse and Organize Contacts

1. Read the Prospect Contact File. Extract all non-empty rows from ALL sheets into a structured list: `name`, `title`, `account`, `linkedin_url`, `colleagues`, `email`
2. Group contacts by `account` (company)
3. For each contact, classify the **persona type** based on title:
   - **C-Suite** (CEO, CTO, CIO, CISO, CIOO, CFO, COO): Care about strategic outcomes, cost savings, competitive advantage, board-level metrics
   - **Technologist** (VP Eng, MD Software, Head of Engineering, Assistant MD, CDO, CDIO): Care about engineering velocity, migration timelines, technical debt, developer experience
   - **Security** (CISO, Head of Security, Chief Security Officer): Care about time-to-exploit compression, vulnerability remediation speed, compliance posture, audit readiness
   - **Innovation** (Chief Innovation Officer, Head of Innovation): Care about new capability deployment, AI-native engineering, competitive differentiation

### Step 2: Company Research

For each unique company, research and document with source URLs:

**2a. Company Fundamentals**
- Use `web_search_exa` with `category:company [Company Name]` for structured company data
- Use `web_search_exa` with `"[Company Name] annual report 2025 2026 financial results"` for financials
- Capture: revenue, headcount, technology budget, number of technologists, strategic priorities

**2b. Financial Objectives**
- Use `web_search_exa` with `"[Company Name] financial objectives OR cost savings OR efficiency target"`
- Identify: cost reduction programs, technology investment priorities, digital transformation budgets

**2c. Technology Imperatives**
- Use `web_search_exa` with `"[Company Name] technology strategy OR digital transformation OR engineering modernization"`
- Look for: legacy migration programs, cloud adoption, platform modernization, framework upgrades
- Map findings to Devin use cases (migrations, security remediation, test coverage, technical debt)

**2d. Press Announcements**
- Use `web_search_exa` with `"[Company Name] press release 2025 2026 technology OR engineering OR AI"`
- Look for: new hires in tech leadership, partnerships, product launches, strategic pivots
- **Verify every URL**: Use `web_fetch_exa` on each URL before including. Confirm the page loads and the content matches what you are attributing to it

**2e. Tech Blogs and Relevant Announcements**
- Use `web_search_exa` with `"[Company Name] engineering blog OR tech blog OR developer blog"`
- Look for: posts about architecture decisions, framework choices, migration stories, AI adoption

Create a **Company Research Summary** for each account:
```
## [Company Name] Research Summary
**Fundamentals**: [Revenue, headcount, tech team size]
**Financial Objectives**: [Cost programs, efficiency targets] - Source: [URL]
**Technology Imperatives**: [Key initiatives] - Source: [URL]
**Press**: [Recent announcements] - Source: [URL]
**Tech Content**: [Blog posts, talks] - Source: [URL]
**Devin Use Case Alignment**: [Matched use cases from research]
```

### Step 3: Person Research

For each contact:

**3a. LinkedIn Profile Deep Dive**
- Use `web_search_exa` with `category:people [Full Name] [Company]` to pull LinkedIn profile data
- Browse their LinkedIn profile directly if Exa results are insufficient
- Look for: recent posts, articles, shared content, endorsements, activity

**3b. Employment Background**
- Map their career history from LinkedIn
- Cross-reference previous employers against the **Devin Pre-Approved Customer List**: Citi, Microsoft, Infosys, Anduril, Goldman Sachs, Ramp, Mercedes-Benz, Cognizant, Cisco, Santander, Dell, Nu, Booking.com, Zillow, Mercado Libre, Cloudflare, NVIDIA, MongoDB, Mitsubishi Corporation, athenahealth
- If a previous employer is a Devin customer, note it as a conversation hook
- **Stanford Alumni Check**: If the person attended Stanford University, note this. You can mention "Cognition was founded by Stanford alumni" as a natural connection point

**3c. Social Media and AI-Related Content**
- Use `web_search_exa` with `"[Full Name] AI OR artificial intelligence OR developer productivity OR engineering efficiency"`
- Look for: conference talks, podcast appearances, blog posts, LinkedIn articles
- Note any public positions on AI, automation, or developer tools

**3d. Devin Value Translation** based on persona:

| Persona | Primary Value Angle |
|---------|-------------------|
| **CISO / Security** | Time-to-exploit compression, autonomous vulnerability remediation at scale, 20x faster than manual (1.5 min vs 30 min per CVE), zero-backlog posture, audit-ready transcripts |
| **CTO / CIO / CIOO / CDO** | Engineering capacity multiplier without headcount growth (Goldman Sachs: 12,000 engineers to output of 14,400), full SDLC acceleration, vendor license exits |
| **CEO** | P&L impact, cost savings at scale ($1.7M/yr Oracle savings per market at a Fortune 50 retailer), competitive advantage through engineering velocity |
| **CFO** | Cost reduction, licensing savings, engineering efficiency ROI, headcount optimization |
| **Technologist (VP/MD/Head of Eng)** | Migration acceleration (8 months to 8 days for COBOL, 6x faster .NET to Java), technical debt paydown, 10,000+ engineering hours saved/month |
| **Innovation Officer** | New capability deployment, AI-native engineering, parallel autonomous agent execution |

Create a **Person Research Card** for each contact:
```
## [Name] - [Title] at [Company]
**LinkedIn**: [URL]
**Career Arc**: [Key career moves, notable trajectory]
**Previous Employer Devin Customer?**: [Yes/No - which company]
**Stanford Alumni?**: [Yes/No]
**AI/Tech Posts**: [Any relevant public content] - Source: [URL]
**Colleague to Reference**: [From contact file, if present]
**Persona Type**: [C-Suite / Technologist / Security / Innovation]
**Primary Devin Value Angle**: [From table above]
**Strongest Personalization Hook**: [The single best angle from research]
```

### Step 4: Establish Triggers

A trigger is a specific, timely event or condition that makes outreach relevant RIGHT NOW. Good triggers signal a moment of change where the prospect is open to new solutions.

**Trigger Categories (priority order):**

1. **Company-Level Triggers**: Recent earnings mentioning cost optimization or tech investment, announced technology transformation programs, new tech leadership hires, regulatory changes, vendor contract renewals/expirations (Oracle, IBM, SAS, Ab Initio, Teradata), security incidents or increased compliance requirements

2. **Role-Level Triggers**: New role or promotion (within last 6 months), team expansion or restructuring, published content about challenges, conference talks on relevant topics

3. **Industry-Level Triggers**: Regulatory deadlines (Basel III.1, ISO 20022, PCI DSS), industry-wide tech shifts (mainframe decommissioning, Java/Spring Boot standardization), competitive moves (peer companies adopting AI engineering tools)

**Trigger Research Process:**
- Use `web_search_exa` with `"[Company Name] [trigger category] 2025 2026"` for each category
- Verify every trigger source URL with `web_fetch_exa`
- For Standard Chartered contacts specifically: use internal context that Devin is being evaluated for SpringBoot auto-framework upgrades and IBM/Oracle license cost exits

Build a **Trigger Matrix** for each contact:
```
| Contact | Trigger Type | Trigger Detail | Source URL | Relevance to Devin |
|---------|-------------|----------------|------------|-------------------|
| [Name]  | [Category]  | [Specific event] | [Verified URL] | [How Devin addresses this] |
```

Each contact must have at least 2-3 triggers. Pick the strongest for Email 1.

### Step 5: Extract and Organize Proof Points

Read the Devin Proof Points file and the pitch decks. Build a lookup of:
- Customer name, industry, use case, headline stat, one-liner, referenceability status, source link
- Banking-specific licensing takeout metrics (from SCB deck)
- Security remediation metrics (from SCB deck)
- APAC banking use cases (from APAC Banking Pitch Deck)

Organize proof points by persona relevance so they are ready for email drafting.

<verification>
- All contacts have been parsed from ALL sheets, grouped by account, and classified by persona type
- Company Research Summary exists for every unique account with verified source URLs
- Person Research Card exists for every contact with career arc, value angle, and personalization hook
- Stanford alumni status checked for every contact
- Trigger Matrix has at least 2-3 triggers per contact, each with a verified source URL
- Proof points are organized and ready for drafting, with referenceability status noted
- All source URLs have been verified with web_fetch_exa
</verification>
</phase>

<phase name="Draft Outreach Sequences" id="2">
## Phase 2: Draft Outreach Sequences

### Cold Email Best Practices (Mandatory Pre-Draft Rules)

Before writing ANY email, apply these data-backed rules:

**Research Sources:**
1. **Gong Study (85M emails)**: 1-4 word subject lines have highest open rates. All lowercase increases opens by 11%. Avoid "AI," "ROI," "Platform" in subject lines. Optimal initial email body is 25-50 words. Interest-based CTAs outperform time-based CTAs. Personalized emails have 5x the reply rate
2. **Hunter 2025 Report (31M emails)**: 3-message sequences increase replies by 106% vs single email. Personalization with 2+ custom attributes yields +56% reply rate
3. **WarmupInbox**: 6-10 word subject lines get 21% open rate vs 14% for 11-15 words. 47% of recipients decide to open based on subject line alone
4. **NuReply**: Under 50 characters for mobile compatibility. Including recipient's company name increases opens by 15-22%. Question-based and data-point subject lines perform best
5. **Leadfeeder (B2B Triggers)**: Trigger events signal moments of change. Timing + relevance + personalization = highest response rates

**Subject Line Rules:**
- 1-4 words, all lowercase
- Include company name or a specific reference when possible
- No buzzwords (AI, ROI, platform, solution)
- No clickbait, no "Re:" or "Fwd:" tricks
- Professional but curiosity-inducing
- Examples: `quick question, [company]`, `[company] engineering capacity`, `spring boot at [company]`

**Email Body Rules:**
- 5-6 sentences maximum. Skimmable in under 10 seconds
- **Format with paragraph spacing**: Each logical section (opener, personalization, CTA, PS) should be its own paragraph with a blank line between them. The email must be easily digestible for an executive scanning on mobile
- NEVER use em dashes or en dashes. Use commas, periods, or restructure sentences
- NEVER use: "leverage," "delighted," "thrilled," "excited to share," "I hope this finds you well," "just wanted to reach out," "streamline," "robust," "cutting-edge," "game-changer," "paradigm," "impressive," "ambitious," "fascinating"
- Vary sentence length. Mix short punchy with longer
- Confident but not stiff. Brief but not cold
- Should feel like it took 3 minutes to write, not 30
- Parenthetical asides are fine and add personality
- No company overview. No "about us." No feature lists
- Sign off: "Regards, Louis"

**Email Structure (3-Part Skeleton):**
1. **Opener** (1 sentence): Trigger reference + 3-name social proof drop. Skip "I hope this finds you well" and go straight into showing you have done your homework
2. **Personalization** (1-2 sentences MAX): Research-backed connection between their world and Devin value. Reference a colleague if one is listed in the Colleagues column (only if present). If ex-Stanford, mention Cognition was founded by Stanford alumni as a natural connection
3. **CTA** (1 sentence): Low-pressure, interest-based. "Would it make sense to send over a 2-slide breakdown of how this applies to [Company]? Also open to jumping on a call."

**PS Line Rules:**
- Short and hard-hitting. Social proof in the PS, not main body
- For Standard Chartered contacts: mention Devin is already being evaluated in WRB and Global Engineering, create polite FOMO
- For other companies: one-liner emphasizing relevant value and impact with a specific metric

**Social Proof Selection (Banking/Financial Services):**
- Financial Services 3-name drop: Citi, Goldman Sachs, Santander (+ Nu for fintech)
- General enterprise: Microsoft, NVIDIA, Goldman Sachs
- Energy/Commodities: Microsoft, NVIDIA, Mitsubishi Corporation
- NEVER fabricate customer relationships. Only use the pre-approved list: Citi, Microsoft, Infosys, Anduril, Goldman Sachs, Ramp, Mercedes-Benz, Cognizant, Cisco, Santander, Dell, Nu, Booking.com, Zillow, Mercado Libre, Cloudflare, NVIDIA, Exa, MongoDB, Mitsubishi Corporation, athenahealth

**Key Proof Points (pick most relevant per persona):**
- Citi: 30,000 merged PRs/month across 40,000 developers
- Goldman Sachs: 12,000 engineers achieving output of 14,400 (20% capacity gain)
- Itau: 75% of 17,000+ tech employees using Devin daily, 6x faster .NET to Java, ~70% security vulns auto-remediated
- Mercedes-Benz: 200K lines COBOL, 8 months to 8 days
- Nubank: 8-12x efficiency gain, 18-month project compressed to weeks
- Security benchmark: 20x faster (1.5 min vs 30 min per vulnerability)
- Fortune 50 Retailer: $1.7M/year Oracle license savings per market

### Draft Email Sequences

For each contact, draft a 3-email sequence:

**Email 1: First Outreach (June 4)**
- Subject: 1-4 words, all lowercase, curiosity-inducing
- Body: Opener (trigger + 3-name social proof), Personalization (1-2 sentences connecting their situation to Devin), Value Prop (1 sentence, exact outcome focused on destination not mechanism), CTA (low-pressure binary question or offer an asset), Close ("Regards, Louis"), PS (hard-hitting social proof)
- For Standard Chartered contacts: mention Devin starting with WRB and Global Engineering, frame wanting their department to accrue value, polite FOMO
- For other companies: PS emphasizes relevant value and impact
- If Colleagues column has a name: reference naturally ("I've been speaking with [Colleague]'s team about...")
- If ex-Stanford: weave in "Cognition was founded by Stanford alumni" naturally

**Email 2: Bump Reply (4 business days after Email 1, June 10)**
- Same thread (reply, no new subject)
- 3-4 sentences (follow-ups with 4+ sentences perform 15x better per Gong)
- Add one NEW proof point or angle not in Email 1
- Do NOT repeat the original pitch
- Soft CTA: "Thought this might be relevant, happy to share more if useful."

**Email 3: New Insight (4 business days after Email 2, June 16)**
- NEW email thread with NEW subject line
- Lead with a different trigger or insight not used in Emails 1 or 2
- Share a specific, concrete insight relevant to their role and company
- Frame as genuinely useful information, not a pitch
- CTA: "Is this something your team is thinking about? Happy to send over specifics."
- Close: "Regards, Louis"
- PS: Different social proof from Email 1

### Draft LinkedIn InMails

Create 5 LinkedIn InMails per contact, sent every 2 weeks starting June 10.

**InMail Rules:**
- Do NOT mention or reference any connection request
- Do NOT use dashes (em, en, or hyphens as dashes)
- Each InMail must be distinct across contacts and touches
- Focus on demonstrating understanding of their world
- Under 300 words
- Vary angle across 5 InMails: different trigger, proof point, or value angle each time
- **Format with paragraph spacing**: Each section should be its own paragraph, easily scannable

**InMail Structure:**
1. Hook (1 sentence): Something specific to them (a post, career move, company initiative)
2. Insight (2-3 sentences): Relevant pattern, case study outcome, or industry observation
3. CTA (1 sentence): Low-pressure, curiosity-based, varied across touches

**Sequencing:**
- InMail 1 (June 10): Strongest personal trigger + most relevant case study
- InMail 2 (June 24): Industry trend or peer company pattern + different proof point
- InMail 3 (July 8): New company-specific insight (recent news, blog) + security/compliance angle if relevant
- InMail 4 (July 22): Specific metric from a similar company + ask about their specific initiative
- InMail 5 (August 5): Breakup style. Short, direct. "Last note from me on this. If timing isn't right, totally understand. If [specific challenge] comes up, happy to be a resource."

<verification>
- Email 1, 2, and 3 exist for every contact in the list
- 5 LinkedIn InMails exist for every contact
- Every subject line is 1-4 words, all lowercase, no buzzwords
- Every email body is 5-6 sentences max
- Email bodies are formatted with paragraph spacing (easily digestible for execs)
- Zero em dashes, en dashes, or banned phrases in any copy
- Every trigger referenced is specific, timely, and has a verified source
- Social proof uses only pre-approved customers
- Each email in a sequence uses a DIFFERENT trigger/angle/proof point
- CTA is interest-based, not time-based in every email
- PS line is present and distinct between Email 1 and Email 3
- Standard Chartered contacts have WRB/Global Engineering FOMO messaging
- Colleagues are referenced ONLY when present in the Colleagues column
- Stanford alumni connections noted where applicable
- Every email is signed "Regards, Louis"
- Copy reads like a human wrote it in 3 minutes, not 30
</verification>
</phase>

<phase name="Quality Review and Delivery" id="3">
## Phase 3: Quality Review and Delivery

### Compile Output as .docx File

Generate a **.docx file** using `python-docx` with all outputs formatted as professional document tables.

**Document Structure:**
1. **Title Page**: "APAC Enterprise Outreach Campaign" with date, contact count, account summary
2. **Executive Summary**: Number of contacts, accounts covered, email sequences drafted, InMails drafted
3. **Company Research Summaries** (one section per account)
4. **Contact Research Cards** (one per contact)
5. **Trigger Matrix** (all contacts in one table)
6. **Outreach Sequences** (tables per contact, all message types)

### Document Table Format

For each contact, create a table with these columns:

| Message Type | To | Subject | Email Body | Source Reference & Explainability |
|---|---|---|---|---|
| InMail 1 (June 10) | [Name] | N/A | [body] | [trigger, proof point, angle explanation + source URLs] |
| InMail 2 (June 24) | [Name] | N/A | [body] | [trigger, proof point, angle explanation + source URLs] |
| InMail 3 (July 8) | [Name] | N/A | [body] | [trigger, proof point, angle explanation + source URLs] |
| InMail 4 (July 22) | [Name] | N/A | [body] | [trigger, proof point, angle explanation + source URLs] |
| InMail 5 (August 5) | [Name] | N/A | [body] | [trigger, proof point, angle explanation + source URLs] |
| Email 1: First Outreach (June 4) | [Name] <[email]> | [subject] | [body with paragraph spacing] | [trigger, social proof, PS explanation + source URLs] |
| Email 2: Bump (June 10) | [Name] <[email]> | (reply thread) | [body] | [new proof point explanation + source URLs] |
| Email 3: New Insight (June 16) | [Name] <[email]> | [new subject] | [body with paragraph spacing] | [trigger, social proof, PS explanation + source URLs] |

**Email Body Formatting in .docx:**
- Each paragraph of the email body should be a separate paragraph in the cell
- Add spacing between opener, personalization, CTA, and PS sections
- PS line on its own line
- Must be easily digestible for an exec reading quickly

### Final Quality Checklist

Run this checklist across ALL drafts before generating the .docx:
- [ ] Every reference to a number or insight has a cited, verifiable source
- [ ] All source URLs have been verified (load correctly, content matches attribution)
- [ ] Persona-appropriate value angle used (security for CISO, strategic for C-suite, technical for engineers)
- [ ] No two emails in a sequence use the same trigger, angle, or proof point
- [ ] No two InMails repeat the same angle or proof point
- [ ] All copy passes the "read it out loud" test: sounds like a person, not a tool
- [ ] Standard Chartered-specific rules applied to all Standard Chartered contacts
- [ ] Non-Standard Chartered contacts have relevant PS lines with specific metrics
- [ ] Stanford alumni connections mentioned where applicable
- [ ] Email bodies are formatted with clear paragraph spacing

### Generate .docx and Deliver to User

1. Use `python-docx` to generate the .docx file with all tables
2. Send the complete .docx file to the user using `message_user` with the file as an attachment
3. Present it directly in the session so the user can review before any send
4. Wait for user approval before considering the task complete

<verification>
- Complete .docx output document exists with all sections
- Document tables have Message Type, To, Subject, Email Body, Source Reference & Explainability for every outreach
- Email bodies in .docx are formatted with paragraph spacing
- Final quality checklist has been run and all items pass
- .docx file has been sent to the user via message_user with attachment
- User has been asked to review before any sending
</verification>
</phase>

---

## Specifications
- All emails signed "Regards, Louis"
- Only pre-approved customers cited as social proof
- Every number/insight referenced must have a verifiable source
- All outputs in .docx file with formatted document tables
- Email bodies formatted with paragraph spacing for exec readability
- If person is ex-Stanford, mention Cognition was founded by Stanford alumni

## Advice and Pointers
- Invest heavily in research but keep email output to 5-6 sentences. The prospect should feel "this person looked me up" without feeling surveilled
- For banking/financial services prospects, the strongest proof points are: Citi (30K PRs/month), Goldman Sachs (20% capacity gain), Itau (75% of 17K engineers daily), and the licensing takeout metrics from the SCB deck
- When in doubt between saying more and saying less, say less
- Parenthetical asides add personality and make copy feel human

## Forbidden Actions
- Do not use em dashes or en dashes in any copy
- Do not use banned words: leverage, delighted, thrilled, excited to share, I hope this finds you well, just wanted to reach out, streamline, robust, cutting-edge, game-changer, paradigm, impressive, ambitious, fascinating
- Do not fabricate customer relationships. Only cite pre-approved customers
- Do not include unverified URLs in source citations
- Do not reference colleagues unless they appear in the Colleagues column of the contact file
- Do not use "AI," "ROI," or "Platform" in subject lines
- Do not ask for meeting times in CTAs. Use interest-based CTAs only
- Do not skip URL verification with web_fetch_exa

## Reference: Banking-Specific Proof Points (From Attached Decks)

### Licensing Takeouts (SCB Intro Deck)
- Ab Initio to Databricks: $83M/yr line item targeted
- Teradata to AWS Iceberg: 1,300 databases unwinding in parallel
- Oracle to Postgres: $2.5M/yr realized licensing savings
- SAS to PySpark/Databricks: 450 Ab Initio graphs, ~10x faster than manual, 2,160 to 130 hrs
- COBOL to Java/Spring Boot: 200K lines, 8 months to 8 days
- .NET to Java/Spring Boot: 3 mo to 1 mo project duration, ~70% fewer engineering hours

### Security (SCB Intro Deck)
- 2,047 remediation sessions at Tier-1 global financial institution (600+ repos)
- 20x faster than manual remediation (1.5 min vs 30 min per vulnerability)
- 9,199 PRs in 4.5 weeks, 648 repos scanned, 90% valid findings
- 36x to 71x ROI ($3.3M to $6.6M pipeline / ~$93K compute)
- 50-70% reduction in audit/compliance prep time

### Banking Use Cases (APAC Banking Pitch Deck)
- Core Banking: ISO 20022 migration, Core Banking Modernization
- Digital Banking: Design System Migration, Nav Refactoring
- Risk and Compliance: Regulatory Change Remediation, FRTB/Basel III.1
- Platform/Infra: IaC Drift Remediation, CI/CD hardening, Cloud Migration
- Wholesale/Markets: Trade Reporting Reconciliation, EOD Batch Job Optimization

### Use Case Outcomes (SCB Intro Deck)
- Framework upgrades: 3 mo to 1 mo .NET to Java, ~70% fewer engineering hours
- Database modernizations: $2.5M/yr Oracle savings, 1,300 Teradata databases in parallel
- Test coverage: Below 50% to 90%+ in under a year
- Refactor/tech debt: 20-40% faster feature delivery after paydown

## Reference: Pre-Approved Customer List

Only cite these companies as Devin customers:
Citi, Microsoft, Infosys, Anduril, Goldman Sachs, Ramp, Mercedes-Benz, U.S. Army, Cognizant, Cisco, U.S. Navy, Rivian, Volkswagen Group Technologies, Santander, Dell, Lowe's, Nu, Booking.com, Zillow, Mercado Libre, Cloudflare, NVIDIA, Exa, MongoDB, Mitsubishi Corporation, athenahealth

**By Industry (for 3-Name Social Proof):**
- Financial Services: Citi, Goldman Sachs, Ramp, Santander, Nu
- Automotive: Mercedes-Benz, Rivian, Volkswagen Group Technologies
- Tech/Infrastructure: Microsoft, Cloudflare, NVIDIA, Exa, MongoDB, Dell, Cisco
- Defense/Government: Anduril, U.S. Army, U.S. Navy
- IT Services: Infosys, Cognizant
- Retail/E-Commerce: Lowe's, Booking.com, Zillow, Mercado Libre
- Conglomerate/Diversified: Mitsubishi Corporation
- Energy/Commodities: Microsoft, NVIDIA, Mitsubishi Corporation

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
