---
name: customer-story-use-case-extraction-from-raw-materials
description: "Converted from Devin playbook: Customer Story & Use-Case Extraction from Raw Materials"
triggers:
  - user
  - model
---

# Customer Story & Use-Case Extraction from Raw Materials

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Customer Story & Use-Case Extraction from Raw Materials (playbook-7f291ca5a7df42fcba4b5ef99491d072), macro `!customer_story`

## Procedure

# Customer Story & Use-Case Extraction from Raw Materials

## Overview
Transform raw customer engagement materials — Granola call notes, pilot readouts, QBR decks, presentations, and any other artifacts — into two polished deliverables: (1) a rough-draft **Customer Story** narrative showcasing how a customer deployed Devin across their organization with clear ROI, and (2) a structured **Use-Case & ROI Breakdown** file cataloging every distinct use-case with its challenge, solution, outcome, and ROI proof points. The story should be ready for Cognition's marketing, sales, and customer success teams to refine and publish.

**Reference examples:** Study [Itaú](https://devin.ai/customers/itau) and the DeNA case study for structure and tone. Both lead with headline metrics, frame the customer's strategic mandate, show multi-function breadth (not just engineering), and include concrete workflow details alongside named stakeholder quotes.

## What's Needed From User

### Required Inputs
- **Customer name** and industry vertical
- **Raw materials** — provide as many of the following as available (links, files, or pasted text):
  - Granola call transcripts or notes
  - Pilot readout documents
  - QBR (Quarterly Business Review) readout documents
  - Presentations or slide decks
  - Slack threads, email threads, or other written communication
  - Usage/consumption data exports (CSV, screenshots, dashboards)

### Optional Inputs
- **Target audience** for the story (e.g., marketing blog, sales deck, executive brief, case study PDF)
- **Preferred tone** (e.g., formal enterprise, conversational blog, technical deep-dive). Default: professional and narrative-driven.
- **Specific use-cases to highlight** if focusing on a subset
- **Customer-approved quotes** or permission notes (what can/cannot be attributed)
- **Devin pilot metrics** (ACU consumption, PR counts, session counts)
- **Competitor context** — what tools or processes Devin replaced

<phase name="Ingest and Extract" id="1">
## Phase 1: Ingest and Extract

Only create the TODO list for this phase.

### Step 1: Catalog all materials
Read every document, transcript, and artifact provided. For each source, create an inventory entry:

| # | Source Type | Date | Key Participants | One-Line Summary |
|---|---|---|---|---|
| 1 | Granola Call | YYYY-MM-DD | Names | Summary |

This becomes the **Source Index** included in both final deliverables.

### Step 2: Extract raw evidence
Scan every source and extract evidence into these categories. Track the source reference for each item.

| Category | What to extract |
|---|---|
| **Use-cases** | Every distinct way Devin was used (e.g., "migrating Java 8 to 17", "writing unit tests for legacy code", "triaging Sentry alerts"). Note the source. |
| **Challenges** | Pain points before Devin or during early adoption. Look for "we struggled with...", "the bottleneck was...", "our team spent X hours on..." |
| **Solutions** | How Devin addressed each challenge — workflow details, integrations, prompt strategies, playbooks, deployment patterns. |
| **Outcomes and Metrics** | Quantitative or qualitative results: time saved, PRs merged, tickets closed, developer satisfaction, cycle-time reduction, cost savings. |
| **ROI Proof Points** | Hard numbers: "reduced migration time from 6 weeks to 3 days", "40% of PRs now authored by Devin", "$X saved per quarter". |
| **Deployment Details** | How Devin was rolled out — which teams, which repos, phased vs. big-bang, training, governance. |
| **Quotes and Testimonials** | Direct quotes from stakeholders. Note who said it, their role, and the source document. |
| **Timeline / Milestones** | Key dates — pilot start, expansion, QBR milestones, renewal decisions. |
| **Blockers and Lessons Learned** | What didn't work initially and how it was resolved. |
| **Competitive Context** | What tools, processes, or manual workflows Devin replaced. What was the status quo before Devin? |

### Step 3: Identify distinct use-cases
Group extracted evidence into discrete use-cases. Each should represent a single coherent problem-solution pair. Aim for 3-8 use-cases. **Rank use-cases by strength of ROI evidence** — use-cases with hard metrics and direct quotes should be ranked highest; these will be featured in the narrative story.

For each use-case, define:
- **Use-Case Name** — short descriptive title (e.g., "Automated Legacy Java Migration")
- **Challenge** — the specific pain point (2-4 sentences)
- **Solution** — how Devin was applied (2-4 sentences)
- **Outcome** — the result achieved (2-4 sentences)
- **ROI Proof Points** — bullet list of quantified metrics with source attribution
- **Supporting Quote** (if available) — a direct stakeholder quote

<verification>
- Every provided source document has been read and cataloged in the Source Index
- Raw evidence has been extracted across all ten categories (use-cases, challenges, solutions, outcomes, ROI, deployment, quotes, timeline, blockers, competitive context)
- At least 3 distinct use-cases have been identified and defined with challenge/solution/outcome/ROI structure
- Every extracted data point has a source attribution
</verification>
</phase>

<phase name="Draft Customer Story" id="2">
## Phase 2: Draft Customer Story

Only create the TODO list for this phase.

### Step 4: Build the narrative arc
Structure the customer story using this arc:

1. **Headline Metrics** — Open with 3-6 bold headline stats at the top, before any prose. Examples: "6x faster Perl→Go migration", "70% of vulnerabilities auto-remediated", "100% org-wide deployment". These are the first thing a reader sees.
2. **About the Company** — Compact sidebar: industry, HQ, size, one line on what they do. Keep it to 2-3 sentences.
3. **The Strategic Mandate** — What organizational imperative drove this? Frame at the business level. Examples: DeNA's "AI All-In" mandate to operate with half the workforce on existing business; Itaú's belief that "speed is the ultimate competitive advantage." Start with why this matters to the company before mentioning Devin.
4. **Why Devin / The Decision** — What did they try before? What ceiling did they hit? Be explicit about what alternatives couldn't do and why Devin broke through. Example: DeNA found IDE assistants gave limited gains and local agents couldn't parallelize — Devin ran in the cloud with persistent state.
5. **Use-Cases in Action** — Walk through 2-4 of the highest-ranked use-cases. Each use-case gets its own subheading and follows a tight **Challenge → Solution → Result** structure. Include concrete workflow details (e.g., DeNA's 3-step DeepWiki→AskDevin→Devin execution pipeline, or Itaú's CI/CD-integrated vulnerability remediation). Tie each technical advantage to an economic or operational consequence.
6. **Aggregate Results** — Summarize quantitative impact across all use-cases. Include both per-use-case metrics and overall organizational metrics (e.g., Itaú's "20-30% throughput increase, 10-15% cycle time reduction").
7. **What's Next** — Expansion plans, new use-cases, deeper integration. Frame as forward momentum.
8. **Customer Voice** — Close with a strong stakeholder quote that encapsulates the value. Choose a quote that captures the organizational shift, not just a feature endorsement.

### Step 5: Write the story
Write the full customer story in markdown following the arc above.

**Writing guidelines (incorporating Anna's PMM standards):**
- Third-person narrative ("The engineering team at Company X...")
- Anchor every claim to data or quotes from the source materials
- Target 1,000-2,000 words (flag if the user needs shorter/longer)
- Use subheadings for each arc section
- Embed quotes naturally within the narrative
- **Bold** key metrics and ROI figures for skim-readability
- Do NOT fabricate any data — only use evidence from source materials
- Mark gaps with `[NEEDS INPUT: description of what's missing]`
- Mark anything potentially sensitive with `[CONFIRM CLEARANCE]`

**Tone and style (from Anna's PMM guidelines — follow these strictly):**
- Start each section with why it matters before dropping product names or features
- Every technical description needs a clear "so-what" for the buyer — tie technical advantages to economic or operational consequences
- Say it plainly: "too expensive to scale" not "cost that doesn't scale." No jargon or invented shorthand.
- Competitive differentiation must be explicit: not just "here's what Devin does" but "here's what alternatives couldn't do and why"
- Prefer fewer, bigger ideas over long lists of differentiators
- Challenge vague adjectives — be specific about what makes something hard
- Shorter is better for section openers — one confident sentence beats a paragraph of setup
- Do NOT use choppy sentence fragments for emphasis — reads as AI-written
- Cut preambles and section headers that don't add information — go straight to substance
- Don't pad arguments with negative comparisons ("not X, not Y") — state what it is
- When describing what a team does, be specific about the actual work, not abstract

Save as `{Customer_Name}_Customer_Story_Draft.md` and present it directly to the user in the session.

<verification>
- Customer story follows the 8-part narrative arc (Headline Metrics through Customer Voice)
- Story is between 1,000-2,000 words
- Every metric and quote in the story is traceable to a source document
- Headline metrics appear at the top of the document before any prose (3-6 bold stats)
- Each use-case section follows Challenge → Solution → Result structure with concrete workflow details
- Competitive differentiation is explicit — the story explains what alternatives couldn't do
- All gaps are flagged with [NEEDS INPUT] placeholders
- Any potentially sensitive information is flagged with [CONFIRM CLEARANCE]
- The story reads as a cohesive narrative, not a bulleted list
- Tone follows Anna's PMM guidelines: no choppy fragments, no jargon, substance over preamble
</verification>
</phase>

<phase name="Build Use-Case Breakdown and Deliver" id="3">
## Phase 3: Build Use-Case and ROI Breakdown and Deliver

Only create the TODO list for this phase.

### Step 6: Create the Use-Case and ROI Breakdown file
Create a structured document following this template:

---

**Use-Case and ROI Breakdown: {Customer Name}**

**Source Index**

| # | Source Type | Date | Summary |
|---|---|---|---|
| 1 | Granola Call | 2025-10-15 | Initial pilot kickoff discussion |

**Use-Case 1: {Use-Case Name}**

*Challenge:* 2-4 sentences describing the pain point.

*Solution:* 2-4 sentences describing how Devin was applied, with workflow specifics.

*Outcome:* 2-4 sentences describing the result.

*ROI Proof Points:*
- Metric 1 — Source: source reference
- Metric 2 — Source: source reference

*Supporting Quote:* "Quote" — Name, Title

(Repeat for each use-case)

**Aggregate ROI Summary**

| Metric | Value | Source |
|---|---|---|
| Total PRs authored by Devin | X | Pilot readout |
| Developer hours saved per week | Y | QBR deck |

**Deployment Overview**
- Teams using Devin: list
- Repositories: count or list
- Deployment model: phased rollout, team-by-team, etc.
- Governance: approval workflows, playbook usage, etc.
- Timeline: key milestones

---

Save as `{Customer_Name}_UseCase_ROI_Breakdown.md` and present it directly to the user in the session.

### Step 7: Cross-check and validate
- Verify every metric in the story also appears in the Use-Case Breakdown (and vice versa)
- Verify every quote is attributed to a named person with their role
- Verify the Source Index covers all provided materials
- Flag any claims relying on a single ambiguous source with `[VERIFY]`

### Step 8: Deliver to the user
Present both files directly in the session and send them as attachments. Include a summary message with:
- Number of use-cases identified
- Top 3 strongest ROI proof points
- Any gaps or `[NEEDS INPUT]` / `[VERIFY]` placeholders that need attention
- Suggested next steps (e.g., "Get customer approval on quotes", "Fill in Q4 metrics")

<verification>
- Use-Case Breakdown file contains all identified use-cases with challenge/solution/outcome/ROI structure
- Every ROI proof point has a source attribution
- Aggregate ROI Summary table is populated
- Deployment Overview section is complete
- Cross-check confirms consistency between the Customer Story and Use-Case Breakdown
- Both files have been presented to the user in the session with a summary of findings and next steps
</verification>
</phase>

## Specifications

### Customer Story Draft
- Format: Markdown (.md), presented directly to user in session
- Length: 1,000-2,000 words (adjustable per user request)
- Opens with 3-6 bold headline metrics before any prose (like Itaú and DeNA stories)
- Follows the 8-part narrative arc (Headline Metrics, About the Company, Strategic Mandate, Why Devin, Use-Cases in Action, Aggregate Results, What's Next, Customer Voice)
- Each use-case structured as Challenge → Solution → Result with concrete workflow details
- Every metric traceable to a source document
- Placeholders (`[NEEDS INPUT]`) for gaps; `[CONFIRM CLEARANCE]` for sensitive items
- Tone follows Anna's PMM standards: plain language, substance-first, explicit competitive differentiation, no AI-sounding fragments

### Use-Case and ROI Breakdown
- Format: Markdown (.md), presented directly to user in session
- Each use-case has: Challenge, Solution, Outcome, ROI Proof Points (with source), Supporting Quote
- Includes Source Index table mapping all ingested materials
- Includes Aggregate ROI Summary table
- Includes Deployment Overview section

### Validation Criteria
- Zero fabricated data points or quotes
- Every claim traceable to a provided source
- Cross-referenced consistency between both deliverables
- All `[VERIFY]`, `[NEEDS INPUT]`, and `[CONFIRM CLEARANCE]` flags clearly visible

## Advice and Pointers

- **Lead with ROI**: The most compelling stories open with bold headline metrics at the top — before any prose. DeNA leads with "6x faster Perl→Go migration, 1 day→5 minutes for complex data analysis." Itaú leads with "6x faster .NET→Java migration, 70% of vulnerabilities auto-remediated."
- **Frame the strategic mandate**: Both great stories connect Devin adoption to a company-level imperative. DeNA's "AI All-In" mandate and Itaú's belief that "speed is the ultimate competitive advantage" give the reader business context that makes the use-cases feel inevitable rather than incidental.
- **Show what alternatives couldn't do**: DeNA explicitly covers the ceiling of IDE assistants and local agents. Don't just say Devin was chosen — explain what it replaced and why those tools fell short. This is required per Anna's PMM standards.
- **Use-cases over features**: Don't describe what Devin *can* do; describe what the customer *did* with Devin and what happened as a result. Use Challenge → Solution → Result structure for each (like both reference stories).
- **Include concrete workflow details**: DeNA's 3-step workflow (DeepWiki→AskDevin→Devin execution) and 80/15/5 work breakdown. Itaú's CI/CD pipeline integration for vulnerability remediation. These specifics make the story credible and actionable.
- **Show breadth beyond engineering**: Both reference stories show Devin across multiple functions — DeNA's analytics and sales teams, Itaú's security, documentation, architecture, and testing. This demonstrates organizational transformation, not just developer tooling.
- **Quotes make stories human**: Prioritize extracting quotes from Granola call transcripts — unscripted conversations contain the most candid assessments. Attribute every quote to a named person with their title.
- **QBR decks contain polished metrics**: Usually the most reliable source for validated numbers since they've been reviewed by both sides.
- **Pilot readouts show the journey**: They capture before/after arcs. Look for "week 1 vs. week 8" comparisons.
- **Include challenges**: A story claiming "everything was perfect" isn't credible. Early struggles and how they were overcome build trust. DeNA's story mentions 80/15/5 — acknowledging the human refinement needed.
- **Translate internal jargon**: Convert ACUs, session IDs, and Cognition-internal terms into business language (per Anna: say it plainly).
- **Rank use-cases before writing**: Feature the 2-4 use-cases with the strongest quantitative ROI evidence in the narrative. List all use-cases in the breakdown file.
- **Sparse materials are okay**: If only 1-2 sources are available, still follow the full process but flag more `[NEEDS INPUT]` placeholders. A thinner story with clear gaps is more useful than a padded one with vague claims.

## Forbidden Actions

- Do NOT fabricate metrics, quotes, or data points not present in the source materials
- Do NOT attribute quotes to people without evidence from the sources
- Do NOT include confidential customer information that hasn't been cleared for external use — flag with `[CONFIRM CLEARANCE]`
- Do NOT use Cognition-internal jargon (ACUs, session IDs, enterprise IDs) in customer-facing story drafts — translate to business language
- Do NOT deliver a bulleted list as the "customer story" — it must read as a narrative
- Do NOT merge distinct use-cases into one vague mega-use-case — keep them specific and separate
- Do NOT skip the cross-check validation step — consistency between deliverables is required

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
