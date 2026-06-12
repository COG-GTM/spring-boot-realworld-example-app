---
name: event-account-prospect-research
description: "Converted from Devin playbook: Event Account Prospect Research"
triggers:
  - user
  - model
---

# Event Account Prospect Research

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Event Account Prospect Research (playbook-7d44c787fdce495a918c8472a7310d7d), macro `!event_prospects`

## Procedure

# Event Account Prospect Research

## Overview
Given an account (company) name, this playbook finds confirmed attendees from the the event registrant list, identifies the job roles represented, and then researches likely prospects from that account who may be attending. The final deliverable is a prospect list with names, titles, and LinkedIn profile URLs presented directly in the session.

## What's Needed From User
- **Account name**: The company/organization to research (e.g., "Salesforce", "Acme Corp")
- **Google Sheet URL**: {EVENT_SPREADSHEET_URL} (default — user can override if the list is updated)

<phase name="Account Lookup" id="1">
## Account Lookup

1. Download the the event attendee list as CSV:
   ```
   curl -sL "{EVENT_SPREADSHEET_EXPORT_URL}" -o /tmp/gcn_attendees.csv
   ```
2. Search for the user's account name in column A (Company name). Use case-insensitive matching and also try common variations (e.g., "Inc.", "Corp", "LLC", abbreviations). For example, if the user says "Acme Corp", also check for "Acme Corporation", "Acme", etc.
3. Extract all matching rows and note:
   - The exact company name variations found
   - Each unique **Job Role** (column B) and how many registrants hold that role
   - Each unique **Front End Country** (column C)
4. Present a summary to the user:
   - Total number of confirmed registrants from the account
   - Breakdown by Job Role (e.g., "Executive: 5, Enterprise Architect and Developer: 3, Application Developer: 2")
   - Countries represented
5. If **zero matches** are found, inform the user immediately and ask if they want to try a different spelling or a related company name. Do not proceed to Phase 2.

<verification>
- The CSV was successfully downloaded and parsed
- The account name was searched with case-insensitive matching and common variations
- A summary of matching registrants (count, job roles, countries) was presented to the user
- If no matches found, the user was notified and asked for alternate names
</verification>
</phase>

<phase name="Prospect Research" id="2">
## Prospect Research

Using the job roles identified in Phase 1, research real people at the account who are likely attendees or relevant contacts.

1. For each job role found in the attendee list, build targeted Google search queries to find people at the account with matching or similar titles. Use queries like:
   - `site:linkedin.com/in "[Company Name]" "[Job Role keyword]"`
   - `"[Company Name]" "[role keyword]" "Google Cloud" OR "GCP" site:linkedin.com`
   - Vary the role keywords based on the Job Role category:
     - **Executive** → search for "VP", "Director", "CTO", "CIO", "SVP", "Head of", "Chief" at the company
     - **Enterprise Architect and Developer** → search for "Enterprise Architect", "Solutions Architect", "Cloud Architect", "Principal Engineer"
     - **Application Developer** → search for "Software Engineer", "Developer", "Engineering Manager", "Dev Lead"
     - **DevOps** → search for "DevOps Engineer", "SRE", "Platform Engineer", "Infrastructure"
     - **Data Engineer** → search for "Data Engineer", "Data Platform", "Analytics Engineer"
     - **Data Scientist** → search for "Data Scientist", "ML Engineer", "AI/ML", "Machine Learning"
     - **Cybersecurity** → search for "Security Engineer", "CISO", "InfoSec", "Security Architect"
     - **SysAdmins and Operations** → search for "Systems Administrator", "IT Operations", "Cloud Operations"
     - **Sales** → search for "Account Executive", "Sales Director", "Sales Engineer"
     - **Marketing** → search for "Marketing Director", "Product Marketing", "Growth"
     - **Operations** → search for "VP Operations", "Director Operations", "Cloud Operations"
     - **Data Analyst** → search for "Data Analyst", "Business Intelligence", "BI Analyst", "Analytics Manager"
     - **Startup Founder** → search for "Founder", "Co-Founder", "CEO" at the company
     - **Alliance / Partnership** → search for "Partner Manager", "Alliance Manager", "BD", "Channel", "Partnerships"
     - **Investor / Financial Analyst** → search for "Investment", "Analyst", "Finance Director", "CFO"
     - **Industry Analyst** → search for "Analyst", "Research Director", "Principal Analyst"
     - **Low / No-Code Developer** → search for "Citizen Developer", "Automation", "No-Code", "Low-Code", "RPA"
     - **Other** / unlisted → search for the company name + "Google Cloud Next" for any public attendee mentions
2. For each prospect found, collect:
   - **Full name**
   - **Job title** (as listed on LinkedIn)
   - **LinkedIn profile URL**
3. Only create a TODO list for the current phase.
4. Aim to find **3-5 prospects per job role** represented in the attendee list. Prioritize:
   - People who have publicly mentioned Google Cloud Next or GCP in their profile/posts
   - Senior / decision-maker titles (Director+, VP+, C-suite)
   - People based in the same countries as the confirmed registrants
5. De-duplicate results — the same person should not appear twice even if they match multiple role categories.

<verification>
- Web searches were performed for each job role category found in Phase 1
- At least 3 prospects were identified (or a clear explanation if fewer exist)
- Each prospect has a name, title, and LinkedIn URL
- Results are de-duplicated
</verification>
</phase>

<phase name="Deliver Results" id="3">
## Deliver Results

1. Compile the final prospect list into a clean markdown table with these columns:

   | # | Name | Title | LinkedIn | Job Role Match | Country |
   |---|------|-------|----------|----------------|---------|

   - **Name**: Full name
   - **Title**: Current job title from LinkedIn
   - **LinkedIn**: Full LinkedIn profile URL
   - **Job Role Match**: Which GCN attendee Job Role category they align with
   - **Country**: Their location (if available)

2. Add a summary section above the table:
   - Account name
   - Total confirmed the target event registrants found
   - Number of prospects identified
   - Job roles covered

3. Present the full deliverable directly in the session as a message to the user. Also save it to a markdown file and attach it so the user has a downloadable copy.

4. Ask the user if they want to:
   - Research additional prospects for any specific role
   - Look up another account
   - Refine the search (e.g., focus on a specific country or seniority level)

<verification>
- A formatted markdown table of prospects was presented in the session
- Each prospect entry includes name, title, LinkedIn URL, role match, and country
- A summary with account stats is included above the table
- The deliverable was also saved as an attached markdown file
- The user was asked about next steps
</verification>
</phase>

## Specifications
- The deliverable must be a clean, formatted markdown table presented directly in the Devin session
- Every prospect must have a valid LinkedIn profile URL (not a guess)
- Prospects should be real, currently employed at the account
- The search should prioritize people likely to attend a cloud technology conference (technical leaders, architects, engineers, IT decision-makers)
- Minimum 5 prospects total per account (if the company is large enough); adjust expectations for smaller companies

## Advice and Pointers
- The Google Sheet job roles are broad categories (e.g., "Executive", "Application Developer"), not specific titles. Use them as directional guidance for the type of person to search for.
- Some company names in the sheet have variations (e.g., "Accenture" vs "accenture" vs "Accenture Federal Services"). Always do case-insensitive partial matching.
- When searching LinkedIn via Google, the `site:linkedin.com/in` operator is the most reliable way to find individual profiles.
- If the account is very large (e.g., Google, Accenture with 10+ registrants across many roles), focus on the top 3-4 most represented job roles to keep the output actionable.
- Cross-reference prospect countries with the countries of confirmed registrants to increase relevance.

## Forbidden Actions
- Do not log into LinkedIn directly or attempt to scrape LinkedIn pages — use Google search with `site:linkedin.com` only
- Do not fabricate prospect names or LinkedIn URLs — every entry must come from a real search result
- Do not include prospects who have clearly left the company (check for "Former" in their LinkedIn snippet)
- Do not proceed past Phase 1 if zero account matches are found in the sheet

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
