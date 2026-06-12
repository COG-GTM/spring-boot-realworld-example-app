---
name: account-plan-generator
description: "Converted from Devin playbook: Account Plan Generator"
triggers:
  - user
  - model
---

# Account Plan Generator

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Account Plan Generator (playbook-9545d731dbe045609f6c02ddac98499f), macro `!account_plan_generator`

## Procedure

Objective
Generate a 19-slide Cognition-branded PowerPoint account plan (.pptx) for a target enterprise account. Apply the standardized 19-slide structure with Cognition's teal-only brand spec. Output is ready for upload to Google Drive (auto-converts to Google Slides on drop-in).

Step 0 — Ask which account
Before doing anything else, ask me the questions below using content_type="user_question" (free-text responses):

Account name (e.g., a global bank, insurer, automaker, retailer, pharma, telco, hyperscaler customer)
HQ region / AE region of responsibility (e.g., "Benelux, globally responsible for HQ-in-region accounts")
Any specific spokes / subsidiaries / business units to prioritize (optional)
Anything to avoid / context I should know (optional — e.g., a pain point they have already publicly solved, an exec who has left, a stack they have already migrated off)
Then proceed without further questions.

Step 1 — Research the account
Use web search, DeepWiki, and any available MCP tools (Exa, Linear, internal knowledge) to gather:

Financials (latest annual report / most recent Q earnings)

Total revenue, net profit, key profitability ratio (cost/income for banks, op margin for industrials, gross margin for software, etc.)
Total assets / market cap, headcount
Revenue mix by segment
Subsidiaries, business units, and regional entities (DEEP RESEARCH)

List ALL major BUs, JVs, fintech/innovation arms, shared-service hubs, regional entities
Tier them: Tier 1 (largest tech orgs / clear Devin fit), Tier 2 (sizeable but secondary), Tier 3 (smaller / innovation / opportunistic)
For each: scale (revenue or headcount), estimated tech staff, why it matters to Devin
Strategic priorities & challenges

Public transformation/cost programmes (named, with dollar/euro amounts and timelines)
Tech-debt indicators (legacy languages, mainframe presence, monolith refactor backlogs)
Regulatory deadlines (industry-specific — e.g., DORA/Basel/PSD3 for banks, ISO 26262/AUTOSAR for auto, FDA/HIPAA for pharma & health, NIS2/CRA for EU tech, etc.)
Talent / cost pressures
Sustainability / ESG mandates if relevant
"Why now" catalysts

Recent earnings momentum, AI/GenAI announcements, cloud migration programmes
Regulatory deadlines hitting in next 6–18 months
Public hiring patterns (engineering scarcity, hub growth)
Recent leadership changes / CTO appointments
Technology landscape

Cloud providers (AWS, GCP, Azure, OCI — be specific about which BU uses which)
Primary languages (Java, Python, C++, COBOL, C#, Go, etc.) and rough LoC if disclosed
CI/CD, data platform, ML/AI infra
Legacy systems and pain points
AI coding tools already deployed (Copilot, ChatGPT Enterprise, Cursor, Tabnine, Cody, internal LLM platforms) — this drives competitive positioning
Key people (use LinkedIn / public bios via web search)

Group CTO / CIO, Head of Engineering, Head of Platform Engineering
CTO of largest BU, Head of Innovation Lab / Hubs
Directors of Software Engineering in HQ region + key satellite regions
Public AI/engineering-transformation voices inside the org
Flag names with "to validate via Sales Navigator" where uncertain
Partner ecosystem

SIs they actively work with (Accenture, Thoughtworks, Deloitte, EPAM, Capgemini, McKinsey, regional specialists)
Cloud partnerships (announced press releases)
GitHub / Microsoft / Google AI relationships
Mark speculative partners with "?"
Competitive AI-coding landscape inside the account

Which AI tools are confirmed deployed (with source)
Where Devin sits in the stack ("autonomous agent layer above autocomplete" — not a Copilot replacement)
Internal build-vs-buy posture (some accounts have strong internal AI teams)
Open source presence

Their GitHub org(s) — material for DeepWiki angles
Any notable OSS projects they maintain
Cite sources on every content slide (annual report, press release, CTO interview, careers listing, etc.). Use real, verifiable numbers — no placeholders.

Step 2 — Build the PowerPoint
Use python-pptx. Apply the 19-slide flow below with the Cognition teal-only brand spec.

Global brand settings (NON-NEGOTIABLE)
Slide size: 13.333 × 7.5 in (16:9)
Font: Calibri throughout
Background: #1A1F2E (dark charcoal-navy) on every slide
Card background: #222A3A with border #333D50
Brand accent: #4ECDC4 (teal) — the ONLY accent color. Single-teal only; do not introduce additional accent colors regardless of source-material inspirations.
Headings: #FFFFFF white
Body: #A0AEC0 light gray
Footer/labels: #6B7A90 medium gray
Footer pattern (on slides 2–11 and 13–19; skip 1 and 12)
Left (9pt, #6B7A90): "Cognition"
Center (8pt, #6B7A90, ALL CAPS): "PROPRIETARY & CONFIDENTIAL"
Right (9pt, #6B7A90): page number
Thin divider line (#333D50) above footer
19-slide structure
#	Slide	Pattern
1	Title	Account name eyebrow + "Account Plan" hero (teal "Plan"), date, COGNITION footer mark
2	Table of Contents	Numbered grid 01–09, 5 left / 4 right
3	Mission & Vision	"OUR MISSION" strap + 3 numbered challenge cards (top transformation pain points specific to this account)
4	Partner Strategy	3 columns (Cloud / DevOps / SI) with named partners + KEY ACTIONS panels
5	Company Overview	5 stat cards (revenue, profit, key ratio, assets/scale, headcount) + KEY CHALLENGES / STRATEGIC OBJECTIVES / TECH STACK panels
6	TAM (3-tier ladder)	LAND $200–400K → LAND&EXPAND $1–2M → STRATEGIC $5–10M+ with ACV badges in teal
7	Executive Map	2×3 grid of 6 named executives with role badges + LinkedIn-validated titles
8	The Mandate	"transformation mandate is clear, execution is the gap" + 3 stat cards + AI-MANDATE pull-quote
9	Devin Multiplier	4 Challenge → Devin rows mapping account pain points to Devin capabilities
10	Why 3	Why Anything · Why Cognition · Why Now — three columns with EVIDENCE panels
11	Devin Pyramid	Tier 3 Strategic → Tier 2 Business → Tier 1 Functional value layers
12	Section divider	Thin teal bar + "Potential ways in" headline (no footer)
13	Business Unit Map	8 BU cards in 2-col grid with scale + Devin angle
14	Regional engineering divisions	9-row table (city, BU, function, headcount, Devin opportunity)
15	TAM by entity	8-row table + roll-up scenarios (LAND / EXPAND / STRATEGIC by spoke)
16	Other PG routes	8-row table (cloud partners, SIs, conferences, DeepWiki, GitHub angles)
17	HVT 1/3	8-column table — first 3 spokes (Spoke / Pain / Use Case / EB / Champion / Tech Eval / Hypothesis / Source)
18	HVT 2/3	8-column table — middle 3 spokes
19	HVT 3/3	8-column table — final 3 spokes
Content rules
Numbers must be real — pulled from the latest annual report / 10-K / press releases. Cite source on slide.
Names must be real — pulled from LinkedIn / official org pages. Mark "to validate via Sales Navigator" where uncertain.
Avoid generic SaaS pitches — every slide must reference account-specific facts (their tech stack, their CTO's stated priorities, their specific regulatory deadlines).
Use cases must map to disclosed pain points — do not pitch a modernization angle the account has already publicly resolved; do not pitch a regulation that does not apply to their industry.
Pricing assumptions — $2,000–$5,000 / seat / year. LAND = 50–100 seats, EXPAND = 300–500, STRATEGIC = 2,000+.
Quality checks
Verify background is #1A1F2E on every slide (NOT black, NOT bright navy)
Verify ONLY teal #4ECDC4 is used as accent (no other colors)
Verify footers are on slides 2–11 and 13–19 only
Verify all text fits without overflow — REMOVE low-value content rather than shrinking fonts below 9pt
Verify table rows alternate between #222A3A and a slightly lighter shade
Save as {AccountName}_Account_Plan.pptx
Step 3 — Deliver
Save the .pptx to /home/ubuntu/{account_slug}/{AccountName}_Account_Plan.pptx
Attach the file to a message_user call so I can download it
Include in the message:
Bullet list of the 19 slides built
5–8 key research-driven angles baked into the deck (account-specific — e.g., guardrails honoured, regulatory tailwinds used, specific transformation programmes referenced)
Any "to validate via Sales Navigator" callouts
One line confirming the file is ready to drag into Google Drive (it auto-converts to Slides)
No need to upload to Drive — I'll do that manually.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
