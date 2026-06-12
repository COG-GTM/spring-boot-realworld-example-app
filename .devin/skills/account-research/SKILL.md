---
name: account-research
description: "Converted from Devin playbook: Account Research"
triggers:
  - user
  - model
---

# Account Research

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Account Research (playbook-0f29f7cd2a0347eeab27188eb482aecb), macro `!account_research`

## Procedure

## Overview
Research a target company's large-scale engineering and technology initiatives by surveying public sources — official blogs, press releases, SEC filings, earnings calls, LinkedIn, Medium, Substack, and X. Produce a structured markdown report identifying initiatives that involve heavy engineering work (migrations, refactors, upgrades, data platform builds, etc.), the leaders behind them, and citations for every claim.

**Pipeline Tracker Integration**: If this research is for one of the user's pipeline accounts, this playbook also reads and updates the Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) with any new contacts, intel, or stage changes discovered during research.

## What's Needed From User
- **Company name** (and any known subsidiaries or brand names to search)
- **Job categories of interest** — e.g., language migrations, large refactors, version upgrades, codebase restructuring, test coverage improvements, large-scale lint/warning fixes, data warehouse migrations, ETL development, data cleaning/preprocessing. Use the list below as a default if the user doesn't specify:
  - Language migrations
  - Large refactors
  - Version upgrades
  - Codebase restructuring
  - Improve test coverage
  - Large-scale lint or warning fixes
  - Data warehouse migrations
  - ETL development
  - Data cleaning and preprocessing
- **Optional**: specific divisions, products, or technology areas to focus on
- **Optional**: time horizon (e.g., "initiatives from the last 2 years")

## Knowledge Notes & IDs
- **Pipeline Generation Master Tracker**: {PIPELINE_TRACKER_NOTE_ID} (read/update if researching a pipeline account)
- **Slack Channel**: {SLACK_CHANNEL} ({SLACK_CHANNEL_ID})

## Procedure

### Step 0: Check Pipeline Tracker (if applicable)
Before starting research, check if the target company is one of the user's pipeline accounts. If yes:
1. Read the Pipeline Generation Master Tracker knowledge note ({PIPELINE_TRACKER_NOTE_ID}) via `devin_knowledge_manage` → `get`
2. Review existing contacts, stages, and intel for this account
3. Note any gaps in contact coverage or intelligence that this research should fill
4. Keep track of any new contacts or intel discovered during research to update the tracker at the end

### Step 1: Search the company's official engineering/technology blog
Navigate to the company's website and look for an engineering blog, tech blog, or developer blog. Read recent posts for mentions of large-scale technical initiatives. If no dedicated blog exists, note that and move on.

### Step 2: Search Google for the company's technology initiatives
Run targeted queries combining the company name with terms like "engineering blog," "technology initiatives," "cloud migration," "modernization," "digital transformation," "data platform," and the specific job categories of interest. Review the top results from the company's own domain and from reputable tech/business outlets.

### Step 3: Check earnings calls and SEC filings
Search for the company's most recent quarterly earnings call transcripts (via Seeking Alpha, Motley Fool, or the company's investor relations page) and annual reports (10-K filings). Look for executive commentary on technology investments, transformation programs, and IT cost savings. These are high-signal sources for executive-visibility initiatives.

### Step 4: Search press releases and partnership announcements
Check the company's newsroom/press release page and search Google News for recent partnership announcements with technology vendors (cloud providers, consultancies, platform vendors). These often reveal large infrastructure or modernization programs.

### Step 5: Search LinkedIn for individual engineer posts and profiles
Run Google searches scoped to linkedin.com combining the company name with engineering role keywords (e.g., "engineer," "developer," "data engineer") and technology keywords (e.g., "cloud migration," "mainframe," "modernization," "ETL," "data warehouse"). Look for:
- Articles or posts by employees describing internal projects (prefer posts with concrete artifacts like slides, conference links, or repo references)
- Profile descriptions that reveal technologies and initiatives being worked on
- Posts by industry analysts commenting on the company's technology work
- **Guardrail**: Treat LinkedIn as a secondary source only. Never use a single LinkedIn profile as the sole basis for claiming an initiative exists — always corroborate with an official or independent source. Verify employment dates and roles before attributing leadership.

### Step 6: Search Medium, Substack, and X for individual posts
Search these platforms for posts by current or former employees, or industry analysts, discussing the company's engineering work. Use queries like `"[Company]" site:medium.com engineer OR migration OR modernization`.

### Step 7: Check additional high-signal sources
Search for:
- **Investor Day / Capital Markets Day presentations** on the company's investor relations page — these often contain multi-year technology roadmaps
- **Conference talks** on YouTube by the company's CTO/CIO/VP Eng at events like AWS re:Invent, Google Cloud Next, KubeCon, Snowflake Summit, Databricks Summit
- **Cloud provider customer stories** (AWS, GCP, Azure case study pages) featuring the company
- **Job postings** on the company's careers page — technology stack and migration keywords in job descriptions are signals for active initiatives (treat as secondary source, corroborate before including)
- **Company GitHub/GitLab organizations** — open-source repos and release notes can reveal active modernization work

### Step 8: Cross-reference and verify findings
For each initiative identified:
- Classify each source as **Primary** (investor relations page, SEC filings, earnings call transcripts, official press releases/blog, official conference talks) or **Secondary** (media coverage, vendor case studies, LinkedIn/Medium/Substack/X posts, job postings)
- Require at least 1 Primary source OR 2 independent Secondary sources (not derived from the same press release)
- Confirm at least one concrete fact per initiative (date, dollar amount, partner name, or metric) with its citation
- If sources conflict on scope or status, include both citations and note the discrepancy
- Discard any initiative that cannot meet the sourcing threshold above

### Step 9: Identify project leaders
For each initiative, identify:
- The executive sponsor (C-suite or SVP level) from earnings calls, press releases, or official bios
- The technical lead or program owner, if identifiable from LinkedIn profiles or bylined articles
- Include LinkedIn profile URLs where available

### Step 10: Compile the markdown report
Write the report using the structure below, saved as `[Company]_Large_Scale_Engineering_Initiatives.md`. For each initiative include: overview, scope and scale, a table mapping relevant job categories, people in charge with titles and citations, and a numbered citation list with URLs.

### Step 11: Add a summary matrix
At the end of the report, include an initiative-to-job-type matrix (table with initiatives as rows and job categories as columns, marked with X where applicable) and an executive leadership summary table.

### Step 12: Update Pipeline Generation Master Tracker (if applicable)
If the target company is one of the user's pipeline accounts:
1. Read the current Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID})
2. Add any NEW contacts discovered during research to the appropriate account's ACTIVE CONTACT DETAIL or STAGE 0 POOLS
3. Update the ACCOUNT DASHBOARD metrics if new intelligence changes the picture
4. Update `Last Updated` timestamp and `Updated By` session ID
5. Write back via `devin_knowledge_manage` → `update`
6. Also update the account-specific knowledge note with new findings

### Step 13: Send the report to the user
Deliver the markdown file and provide a brief summary of findings (number of initiatives found, top leaders, any notable gaps in public information). If the Pipeline Generation Master Tracker was updated, note what was added.

## Report Structure

Each initiative section should follow this template:

```
## N. [Initiative Name]

**Status**: Active | Planned | Completed | Unknown
**Timeframe**: [First announced/known start] — [Latest activity mention]
**Key Partners/Vendors**: [Named external partners, if any]

### Overview
Brief description of the initiative and its strategic importance.

### Scope & Scale
- Bullet points with concrete metrics (dollar amounts, facility counts, timelines)

### Relevant Job Types
| Job Type | Relevance |
|---|---|
| **[Category]** | How this initiative maps to this job type |

### People in Charge
- **[Name]** — [Title], [Company] (role in this initiative)

### Citations
1. [Source Name] ([Date]): [Title](URL) — brief quote or description
```

## Specifications

- Every factual claim must have a citation with a URL
- Each initiative must map to at least two of the user's job categories of interest
- Include both executive leaders and, where discoverable, technical/engineering leads
- The report should be a single self-contained markdown file
- Discard initiatives that cannot be corroborated by at least one primary source or two independent secondary sources
- Clearly note when a company does not have a public engineering blog, or when information is limited
- If researching a pipeline account, the Pipeline Generation Master Tracker MUST be updated with new contacts/intel

## Advice and Pointers

- **Earnings calls are the highest-signal source** for executive-visibility initiatives — executives explicitly name and quantify their biggest programs there.
- **LinkedIn is useful for discovering initiatives not mentioned in official channels** — but always corroborate with an official source before including an initiative. Prefer LinkedIn posts/articles with concrete details over generic profile bullet points.
- When a company doesn't have a dedicated engineering blog, their "About" or "Newsroom" pages, conference talks (YouTube), and podcast appearances by CTOs/VPs of Engineering are good alternatives.
- For each initiative, try to find the internal program name (companies often brand their big initiatives, e.g., "Network of the Future," "Efficiency Reimagined") — this makes subsequent searches much more targeted.
- SEC filings (especially the 10-K "Information Technology" or "Risk Factors" sections) sometimes reveal technology modernization efforts that aren't publicized elsewhere.
- **Pipeline Tracker**: The Pipeline Generation Master Tracker ({PIPELINE_TRACKER_NOTE_ID}) is the single source of truth for the user's pipeline. When researching one of his 7 accounts, always check the tracker first for existing contacts and update it with new findings.

## Forbidden Actions

- Do not fabricate citations or URLs — every link must come from an actual source found during research
- Do not include initiatives that cannot be verified by at least one credible source
- Do not guess at people's titles or roles — only include leadership information that is directly sourced
- Do not modify the Pipeline Generation Master Tracker's structure or format — only add data within the existing sections

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
