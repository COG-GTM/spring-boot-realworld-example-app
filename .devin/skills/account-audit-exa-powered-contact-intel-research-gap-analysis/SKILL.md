---
name: account-audit-exa-powered-contact-intel-research-gap-analysis
description: "Converted from Devin playbook: Account Audit — Exa-Powered Contact, Intel & Research Gap Analysis"
triggers:
  - user
  - model
---

# Account Audit — Exa-Powered Contact, Intel & Research Gap Analysis

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Account Audit — Exa-Powered Contact, Intel & Research Gap Analysis (playbook-2e9418d5af8b4570a6733fb237fc98ca), macro `!audit_account`

## Procedure

# Account Audit — Exa-Powered Contact, Intel & Research Gap Analysis

## Overview
Run a comprehensive audit of a target account using Exa MCP to identify gaps in contacts, company intel, and deep research. Produces a structured gap report with actionable recommendations and new contacts ready for pipeline addition.

## What's Needed From User
- Account name (e.g., "Cleveland Clinic", "Mayo Clinic", "all")
- Whether to run across all accounts or a single account
- Any specific functions or seniority levels to prioritize (optional - defaults to full coverage)

<phase name="Load Account Context" id="1">
## Load Account Context

1. Read the account's existing knowledge note using `devin_knowledge_manage` with `action: "get"`
2. Read the Pipeline Generation Master Tracker knowledge note for current pipeline status
3. Extract and catalog:
   - All existing contacts (name, title, function, stage)
   - All documented initiatives and their status
   - All documented leadership with current roles
   - Any noted research gaps or TBD items
   - Last updated date for the account note
4. Create a working checklist of what's already known vs. what needs verification

<verification>
- Account knowledge note loaded and parsed
- Existing contacts catalogued with titles and stages
- Current initiatives and leadership documented
- Research gaps from the note identified
- Working checklist created
</verification>
</phase>

<phase name="Contact Coverage Audit" id="2">
## Contact Coverage Audit

Run Exa searches to discover contacts across all key engineering functions. For each function, compare results against the existing contact list.

### Required Searches (run in parallel where possible)

For the target account, run these `web_search_exa` queries with `numResults: 10`:

1. **Engineering Leadership**: `category:people director OR VP engineering at [Company]`
2. **Platform Engineering**: `category:people platform engineering at [Company]`
3. **DevOps / SRE**: `category:people DevOps OR SRE at [Company]`
4. **Data Engineering**: `category:people data engineering at [Company]`
5. **Security / Cybersecurity**: `category:people cybersecurity OR information security at [Company]`
6. **AI / ML**: `category:people AI OR machine learning engineering at [Company]`
7. **Software Engineering Directors**: `category:people software engineering director at [Company]`
8. **Cloud Infrastructure**: `category:people cloud infrastructure at [Company]`

### For Each Result
- Check if the person already exists in the account's contact list
- If NEW: record name, title, LinkedIn URL, connection count, and which function they fill
- If EXISTING: verify title hasn't changed (detect departures or promotions)
- Flag anyone with <500 LinkedIn connections as "email-only" per Anti-AI Messaging Rules

### Gap Analysis
- Identify functions with zero or thin coverage (e.g., "Data Engineering: 0 contacts" or "Security: 1 contact, need 3+")
- Identify seniority gaps (e.g., "No Director-level contacts in Platform Eng")
- Flag any known contacts who may have departed (title mismatch, company mismatch)

<verification>
- All 8 function searches completed
- New contacts identified and catalogued with LinkedIn URLs
- Existing contacts verified (no departures or title changes missed)
- Function coverage gaps documented
- Seniority gaps documented
- Connection counts checked for LinkedIn viability
</verification>
</phase>

<phase name="Company Intel Refresh" id="3">
## Company Intel Refresh

Run Exa searches to verify and update company intelligence.

### Required Searches

1. **Company Profile**: `web_search_exa` with `category:company [Company] technology initiatives`
2. **Initiative Tracking**: `web_search_exa` with `"[Company] [initiative_1] OR [initiative_2] OR [initiative_3]"` (use top 3 initiatives from the account note)
3. **Leadership Changes**: `web_search_exa` with `"[Company] CTO OR CIO OR VP Engineering new hire OR appointed OR departed 2025 2026"`
4. **Recent News**: `web_search_exa` with `"[Company] technology engineering 2026"` to find anything published in 2026
5. **Partnership / Vendor Updates**: `web_search_exa` with `"[Company] partnership OR vendor OR platform launch 2025 2026"`

### For Each Result
- Compare against what's in the account note
- Flag NEW information not currently in the note (new initiatives, leadership changes, partnerships)
- Flag STALE information in the note that may be outdated
- Use `web_fetch_exa` on the 2-3 most promising URLs to get full article content

### Gap Analysis
- List initiatives that are confirmed still active vs. potentially completed or abandoned
- List any new initiatives not in the current note
- List leadership changes (new hires, departures, promotions)
- List any news that changes the account's strategic landscape

<verification>
- All 5 company intel searches completed
- New initiatives and news identified
- Stale or outdated information flagged
- Leadership changes detected
- Top URLs fetched for full content verification
</verification>
</phase>

<phase name="Deep Research Gap Analysis" id="4">
## Deep Research Gap Analysis

For each priority contact (Wave 1 / Tier 1 or Stage 3+), assess the depth of existing research and fill gaps.

### For Each Priority Contact (top 5-10 per account)

1. **Name Search**: `web_search_exa` with `"[Full Name] [Company]"` — find articles, talks, interviews
2. **Content Search**: `web_search_exa` with `"[Full Name] engineering OR technology OR leadership blog OR conference OR interview"` — find thought leadership
3. **Deep Dive**: Use `web_fetch_exa` on any promising URLs to extract personalization hooks

### Assess Research Depth
For each contact, rate their research profile:
- **Rich** (3+ personalization hooks: articles, talks, personal interests, career highlights)
- **Moderate** (1-2 hooks: basic career history + one personal detail)
- **Thin** (LinkedIn profile only, no public content found)

### Gap Analysis
- List contacts with "Thin" research profiles that need manual LinkedIn deep dives
- List contacts with outdated research (e.g., references to old role or old company)
- Identify any contacts with rich public content that hasn't been captured in the account note
- Cross-reference previous employers against the pre-approved Devin customer list for warm intro angles

<verification>
- Priority contacts searched (minimum top 5 per account)
- Research depth rated for each contact
- Thin-profile contacts flagged for manual research
- Cross-employer customer list check completed
- New personalization hooks documented
</verification>
</phase>

<phase name="Generate Gap Report" id="5">
## Generate Gap Report

Compile all findings into a structured gap report and deliver to the user.

### Report Structure

```markdown
# [Account Name] — Exa Audit Gap Report
**Audit Date**: [date]
**Audited By**: Devin session [session_id]

## Executive Summary
- X new contacts discovered
- Y company intel updates found
- Z research gaps identified
- Overall account health: [Strong / Moderate / Needs Refresh]

## Contact Coverage Gaps
| Function | Current Count | New Contacts Found | Gap Status |
|---|---|---|---|
| Engineering Leadership | X | Y | [Covered / Gap] |
| Platform Engineering | X | Y | [Covered / Gap] |
| ... | ... | ... | ... |

### New Contacts Discovered
| Name | Title | LinkedIn | Connections | Channel | Notes |
|---|---|---|---|---|---|
| ... | ... | ... | ... | LinkedIn/Email | ... |

### Departure / Title Change Alerts
| Name | Old Title | New Info | Action Needed |
|---|---|---|---|
| ... | ... | ... | ... |

## Company Intel Updates
### New Information
- [bullet list of new initiatives, news, partnerships]

### Stale Information (needs update in account note)
- [bullet list of outdated items in current note]

### Leadership Changes
- [bullet list of new hires, departures, promotions]

## Deep Research Gaps
### Contacts Needing More Research
| Name | Current Depth | Gap | Recommended Action |
|---|---|---|---|
| ... | Thin | No public content | Manual LinkedIn deep dive |

### New Personalization Hooks Found
| Name | Hook Type | Detail | Source |
|---|---|---|---|
| ... | Conference talk | "..." | [URL] |

### Cross-Employer Customer List Matches
| Name | Previous Employer | Devin Customer? | Warm Intro Angle |
|---|---|---|---|
| ... | ... | Yes/No | ... |

## Recommended Actions (Priority Ordered)
1. [Highest priority action]
2. [Second priority]
3. ...
```

1. Generate the report as a markdown file
2. Present the report directly in the session for the user to review
3. Send the report to the user via `message_user` with the file attached
4. Offer to update the account knowledge note with the new findings

<verification>
- Gap report generated as markdown file
- Report covers all three audit areas (contacts, intel, research)
- Report includes actionable recommendations
- Report delivered to user
- Offer made to update account knowledge note
</verification>
</phase>

## Specifications
- Each audit should produce a standalone gap report markdown file
- New contacts must include LinkedIn URLs and connection counts where available
- All Exa-sourced URLs must be verified with `web_fetch_exa` before including in the report
- Contact coverage should span at minimum 8 engineering functions
- Research depth assessment required for all Stage 3+ and Wave 1 / Tier 1 contacts
- Report must be delivered directly in the session (not just saved to filesystem)

## Advice and Pointers
- Run Exa searches in parallel (batch independent queries) for speed
- When auditing "all" accounts, process them sequentially to avoid overwhelming context
- The pre-approved Devin customer list for cross-employer checks: Citi, Microsoft, Infosys, Anduril, Goldman Sachs, Ramp, Mercedes-Benz, U.S. Army, Cognizant, Cisco, U.S. Navy, Rivian, Volkswagen Group Technologies, Santander, Dell, Lowe's, Nu, Booking.com, Zillow, Mercado Libre, Cloudflare, NVIDIA, Exa, MongoDB, Mitsubishi Corporation, athenahealth
- Connection count threshold: <500 = email only, >=500 = LinkedIn viable
- If running across all accounts, produce individual reports per account plus a cross-account summary
- Healthcare accounts: also search for clinical informatics, EHR, and health IT contacts
- Pharma accounts: also search for regulatory, clinical data, and R&D engineering contacts

## Forbidden Actions
- Do NOT update account knowledge notes without user approval
- Do NOT send any outreach messages as part of the audit
- Do NOT fabricate contact information — only report what Exa returns
- Do NOT include unverified URLs in the gap report
- Do NOT touch Ascension contacts or outreach (MANUAL MODE)
- Do NOT assume a contact has departed just because Exa returns a different title — flag for verification

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
