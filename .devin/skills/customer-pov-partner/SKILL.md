---
name: customer-pov-partner
description: "Converted from Devin playbook: customer_pov_partner"
triggers:
  - user
  - model
---

# customer_pov_partner

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: customer_pov_partner (playbook-e1398bf512ae4796af0aa31d52f5d218), macro `!customer_pov_partner`

## Procedure

Customer POV

## Overview

Generate a co-branded customer Point of View (POV) document showing how the Cognition Platform addresses a specific company's technology priorities and broader business initiatives. The playbook deeply researches the target customer's strategic direction (investor presentations, earnings calls, annual reports), maps their stated priorities to concrete Devin use cases grounded in real outcome data, and delivers a polished PDF (max 5 pages) with dual branding.

The document does NOT include a summary of the target company or a description of Devin/Cognition. It jumps straight into the value proposition: what this customer is trying to accomplish and how the Cognition Platform delivers measurable results for those exact priorities.

The internal data used in this document must focus exclusively on **before/after comparisons, time saved, efficiency gains, and cost savings** — not on raw platform usage metrics like total sessions, number of users, ACUs consumed, active engineers, or adoption ramp timelines.

## What's Needed From User

- **Target customer name and industry** (e.g., "AbbVie, bio pharma")
- **Technology focus areas** the customer cares about (e.g., "accelerating AWS cloud migration")
- **Partner call notes** (optional) — upload as a file attachment. Notes from partner interlock calls help prioritize which use cases matter most to the customer.
- **Customer logo file** (optional) — if you have the customer's logo, attach it. Otherwise Devin will attempt to find it from public sources.
- **Industry preference for proof points** (optional) — e.g., "prioritize healthcare customer references"
- **Partner branding** (optional) — if a channel partner (e.g., Ahead, Cognizant, Accenture) is involved, provide the partner name, logo file, brand colors, and website URL. When specified, the document will use partner + customer branding instead of Cognition + customer branding. If no logo file is provided, Devin will search for it online.

## Procedure

1. **Deep-dive the customer's strategic direction.** This is the foundation of the entire document. Research extensively:

   **Investor Day presentations and strategy decks:**
   - Search for "[Company] Investor Day [year]" and "[Company] strategy presentation [year]" to find slide decks and transcripts
   - Look for technology-specific segments: CTO/CIO presentations, "Technology Transformation" or "Digital Strategy" sections
   - Extract specific initiatives with names, timelines, and investment figures (e.g., "$2B cloud modernization program over 3 years")
   - Note any stated KPIs or targets (e.g., "reduce time-to-market by 40%", "migrate 80% of workloads to cloud by 2027")

   **Annual reports and 10-K filings:**
   - Technology investment sections, risk factors mentioning technical debt or legacy systems
   - Capital expenditure breakdowns showing IT spend trends
   - Statements about developer productivity, engineering headcount, or build-vs-buy strategy

   **Earnings call transcripts:**
   - Search for recent quarterly calls on seekingalpha.com, fool.com, or the company's IR page
   - Look for C-suite commentary on: IT modernization, cost optimization, development velocity, talent challenges, AI adoption
   - Note any specific technology vendors or platforms mentioned (AWS, Azure, GCP, ServiceNow, Salesforce, etc.)

   **Press releases and news:**
   - Technology partnerships, platform migrations, acquisitions of tech companies
   - Hiring trends (are they scaling engineering? consolidating?)
   - Industry-specific regulatory changes driving technology needs

   **If a Salesforce/CRM MCP integration is configured**, also query it for account history, prior engagement notes, and existing opportunity data.

2. **Map broader business initiatives to Cognition Platform outcomes.** Based on the research, identify the customer's top business-level priorities and articulate a specific hypothesis for how the Cognition Platform delivers measurable before/after improvements for each one. Frame every hypothesis around **time saved, efficiency gained, or cost reduced** — not platform usage:

   | Business Initiative | Example Hypothesis |
   |---|---|
   | **Reducing IT spend / cost optimization** | Devin handles routine engineering tasks (bug fixes, test writing, dependency updates) at a fraction of the cost of additional headcount, freeing senior engineers for high-value architecture work. Enterprises see cost per merged PR as low as $20-50 vs. industry estimates of $500+ for equivalent manual effort. |
   | **Speeding development / time-to-market** | Devin parallelizes development work — multiple tasks running simultaneously. Tasks that previously took a developer 4-8 hours (e.g., writing test suites, fixing CI failures, implementing boilerplate endpoints) are completed in under an hour, compressing release cycles. |
   | **Improving customer satisfaction / reliability** | Devin-generated test suites increase code coverage, and automated bug fixes reduce mean time to resolution. Enterprises using Devin for test generation see PR merge rates above 50%, meaning real production-quality code reaching main branches. |
   | **Cloud migration / modernization** | Devin performs service-by-service migration with consistent patterns, reducing manual effort from weeks to days per service. Before Devin: engineers spend 2-3 weeks per service migration. After Devin: migration tasks complete in 1-2 days with human review. |
   | **Talent shortage / engineering scaling** | Devin augments existing teams, effectively multiplying engineering output without proportional headcount growth. Before: team bottlenecked on routine work. After: routine tasks offloaded to Devin, engineers focus on architecture and design. |
   | **Security and compliance** | Devin automates vulnerability remediation and compliance-related code changes at scale, with every change going through standard PR review. Before: security backlog grows for weeks. After: remediation PRs opened within hours of vulnerability detection. |
   | **Technical debt reduction** | Devin systematically addresses refactoring, dependency upgrades, and code quality improvements that teams deprioritize. Before: tech debt accumulates quarter over quarter. After: continuous automated cleanup with measurable reduction in legacy code. |

   For each initiative, the hypothesis should be specific enough that the customer recognizes their own stated goal, and framed as a clear before/after transformation.

3. **Research the customer's technology stack.** Understand what they build with and how Devin fits as a co-worker in their existing environment:
   - Search job postings on LinkedIn, levels.fyi, and the company careers page for programming languages, frameworks, and tools
   - Check GitHub organization (if public) for languages and repo activity
   - Look for conference talks by their engineers on YouTube, InfoQ, or tech blogs
   - Review technology partnership announcements (e.g., "strategic partnership with AWS" = likely heavy AWS usage)
   - Check StackShare, BuiltWith, or Wappalyzer for publicly known tech stack components

   Then articulate how Devin integrates:
   - **Source control**: Devin works directly in their GitHub/GitLab/Bitbucket repos, opening PRs through standard workflows
   - **CI/CD**: Devin's PRs trigger their existing CI pipelines — no separate review process needed
   - **Cloud platforms**: Devin generates IaC (Terraform, CloudFormation, Pulumi) for their specific cloud provider
   - **Languages and frameworks**: Devin works across their stack (list the specific languages found in research)
   - **Security tools**: Devin's changes go through existing SAST/DAST scanners and code review policies
   - Frame Devin as a co-worker that uses the same tools, follows the same processes, and submits work for the same review standards as any human engineer on the team

4. **Select industry-specific use cases.** Choose 4-6 use cases that directly map to the customer's stated priorities from steps 1-3. Make each use case specific to their industry, not generic:

   **Instead of generic use cases, tailor to the industry:**

   *Financial Services:*
   - Regulatory compliance automation (SOX, Basel III/IV, DORA) — Devin implements required code changes across services when regulations update
   - Core banking modernization — migrating COBOL/mainframe logic to modern microservices
   - Real-time fraud detection pipeline development — building and testing streaming data processors
   - PCI-DSS compliance remediation — scanning and fixing security findings across the codebase

   *Healthcare / Pharma:*
   - HIPAA-compliant API development — generating secure endpoints with proper PHI handling
   - Clinical trial data pipeline automation — building ETL pipelines for regulatory submissions
   - FDA 21 CFR Part 11 compliance — implementing audit trails and electronic signature workflows
   - EHR/EMR integration development — building HL7 FHIR adapters and interoperability layers

   *Insurance:*
   - Policy administration system modernization — migrating legacy rating engines to cloud-native
   - Claims processing automation — building API integrations between legacy and modern systems
   - Actuarial model implementation — converting actuarial models into production code
   - State-by-state regulatory filing automation — generating required code changes per jurisdiction

   *Technology / SaaS:*
   - Multi-tenant architecture scaling — refactoring for horizontal scalability
   - API versioning and backward compatibility — automating breaking change detection and migration
   - Automated SDK generation — building client libraries across languages from API specs
   - Infrastructure cost optimization — right-sizing Terraform/CloudFormation configurations

   *Retail / E-commerce:*
   - Omnichannel integration — connecting in-store, mobile, and web systems
   - Payment gateway migration — switching processors while maintaining PCI compliance
   - Inventory management system modernization — replacing legacy ERP integrations
   - Peak traffic preparation — load testing automation and performance optimization

   *Manufacturing / Industrial:*
   - IoT data pipeline development — ingesting and processing sensor data at scale
   - MES/SCADA integration — bridging OT and IT systems with modern APIs
   - Supply chain visibility — building real-time tracking and analytics dashboards
   - Legacy PLC/HMI modernization — wrapping proprietary protocols in standard interfaces

   For each selected use case, write:
   - **The customer's specific challenge** (grounded in research from step 1, e.g., "[Company] stated in their 2025 Investor Day that migrating their core banking platform is a $500M, 5-year initiative")
   - **How the Cognition Platform addresses it** (specific to their stack and scale)
   - **Before/after proof point** from anonymized enterprise data (see step 5)

5. **Build before/after proof points from internal customer data.** This is the backbone of credibility. All internal data must be presented as **before/after comparisons showing time saved, efficiency gains, and cost savings** — never as raw platform usage metrics.

   **5a. Query outcome-focused data from Redshift.** Use the Redshift MCP with tool `execute_sql` and param `query` (e.g., `mcp_tool_call server="redshift" tool_name="execute_sql" {"query": "..."}` or via `mcp-cli tool call execute_sql -s redshift --input '{"query": "..."}'`). All queries use the `analytics` schema.

   **Get PR throughput and merge quality (the primary outcome metric):**
   ```sql
   WITH enterprise_orgs AS (
     SELECT org_id FROM analytics.dim_orgs
     WHERE enterprise_name = '<ENTERPRISE_NAME>'
   )
   SELECT
     COUNT(DISTINCT pr_url) AS prs_created,
     COUNT(DISTINCT CASE WHEN pr_state = 'merged' THEN pr_url END) AS prs_merged,
     ROUND(100.0 * COUNT(DISTINCT CASE WHEN pr_state = 'merged' THEN pr_url END)
       / NULLIF(COUNT(DISTINCT pr_url), 0), 1) AS merge_rate_pct
   FROM analytics.pull_requests
   WHERE org_id IN (SELECT org_id FROM enterprise_orgs);
   ```

   **Get cost per merged PR (the key ROI metric):**
   ```sql
   WITH enterprise_orgs AS (
     SELECT org_id FROM analytics.dim_orgs
     WHERE enterprise_name = '<ENTERPRISE_NAME>'
   ),
   session_cost AS (
     SELECT
       c.devin_id,
       c.total_internal_cost
     FROM analytics.consumption_by_devin c
     WHERE c.org_id IN (SELECT org_id FROM enterprise_orgs)
   ),
   merged_prs AS (
     SELECT DISTINCT devin_id
     FROM analytics.pull_requests
     WHERE org_id IN (SELECT org_id FROM enterprise_orgs)
       AND pr_state = 'merged'
   )
   SELECT
     ROUND(SUM(sc.total_internal_cost) / NULLIF(COUNT(DISTINCT mp.devin_id), 0), 2) AS cost_per_merged_pr_usd
   FROM session_cost sc
   LEFT JOIN merged_prs mp ON sc.devin_id = mp.devin_id;
   ```

   **Get use case distribution (maps to which task types deliver results):**
   ```sql
   WITH enterprise_orgs AS (
     SELECT org_id FROM analytics.dim_orgs
     WHERE enterprise_name = '<ENTERPRISE_NAME>'
   )
   SELECT
     category,
     COUNT(DISTINCT devin_id) AS task_count,
     ROUND(100.0 * COUNT(DISTINCT devin_id) / SUM(COUNT(DISTINCT devin_id)) OVER(), 1) AS pct_of_tasks
   FROM analytics.dim_combined_sessions
   WHERE org_id IN (SELECT org_id FROM enterprise_orgs)
     AND category IS NOT NULL
   GROUP BY category
   ORDER BY task_count DESC;
   ```
   Task categories: `bug_fixing`, `feature_development`, `unit_test_generation`, `refactoring_and_optimization`, `migrations_and_upgrades`, `ci_cd_and_devops`, `code_review_and_analysis`, `documentation_and_content`, `code_quality_and_security`, `data_and_automation`, `research_and_exploration`.

   **Find top enterprise customers by outcome quality for proof points:**
   ```sql
   WITH enterprise_prs AS (
     SELECT
       COALESCE(o.enterprise_name, o.org_name) AS enterprise_name,
       COUNT(DISTINCT p.pr_url) AS prs_created,
       COUNT(DISTINCT CASE WHEN p.pr_state = 'merged' THEN p.pr_url END) AS prs_merged
     FROM analytics.pull_requests p
     JOIN analytics.dim_orgs o ON p.org_id = o.org_id
     WHERE o.enterprise_id IS NOT NULL
     GROUP BY COALESCE(o.enterprise_name, o.org_name)
   )
   SELECT
     enterprise_name,
     prs_created,
     prs_merged,
     ROUND(100.0 * prs_merged / NULLIF(prs_created, 0), 1) AS merge_rate_pct
   FROM enterprise_prs
   WHERE prs_created >= 50
   ORDER BY prs_merged DESC
   LIMIT 20;
   ```

   **5b. Query enterprise contract data from BigQuery if available.** Use the BigQuery MCP (`cognition-bigquery`) with tool `execute_sql` and param `sql` (e.g., `mcp_tool_call server="cognition-bigquery" tool_name="execute_sql" {"sql": "..."}` or via `mcp-cli tool call execute_sql -s cognition-bigquery --input '{"sql": "..."}'`):
   ```sql
   SELECT
     team_name,
     rillet_contract_seats,
     users_in_team,
     total_contract_value,
     sku,
     credits_per_user_per_month,
     earliest_contract_start,
     latest_contract_end
   FROM exafunction.analytics.enterprise_customers
   WHERE LOWER(team_name) LIKE '%<customer_keyword>%'
   LIMIT 10;
   ```

   **5c. Search for public case studies as supplementary evidence only.** Check devin.ai/customers and cognition.ai for published outcomes. These can be cited by name since they are public, but internal data should be the primary source.

   **5d. Construct before/after comparisons for each use case.** For each use case, build a clear two-column comparison:

   | Dimension | Before Devin | After Devin |
   |---|---|---|
   | **Time per task** | Use industry benchmarks for the customer's context (e.g., "average bug fix cycle: 4-8 hours", "migration per service: 2-3 weeks", "test suite creation: 1-2 days") | Use internal PR data to estimate Devin completion time. Frame as: "Completed in under 1 hour" or "Reduced from weeks to days" |
   | **Cost per task** | Estimate using industry developer cost data (e.g., "fully-loaded engineer cost: $150-250/hr" in their geography) | Use cost-per-merged-PR from internal data (typically $20-80). Present the ratio: "10-20x cost reduction per completed task" |
   | **Throughput** | "Team completes X PRs/week with current headcount" (use industry benchmarks or public engineering blog data) | "Devin adds Y merged PRs/week at Z% merge rate — equivalent to adding N engineers' PR output without additional headcount" |
   | **Quality / review burden** | "Every task requires full manual implementation and review" | "Devin produces review-ready PRs that pass CI. Engineers review rather than write — shifting from creation to oversight" |

   - Maintain a source list for traceability — note which data came from internal queries vs. public sources
   - All dollar amounts should use real internal cost data, not estimates
   - Frame efficiency gains as multipliers: "5x faster", "10x cheaper", "equivalent output of N additional engineers"

   **Important data quality notes:**
   - Use `is_cognition_session_in_customer_org = false` on `dim_sessions` to exclude Cognition employee sessions
   - For PR counts, always use `analytics.pull_requests`, NOT `dim_sessions.gh_pr` (which undercounts — `dim_sessions.gh_pr` captures at most one PR per session, while a single session can produce multiple PRs)
   - Enterprise customers may have multiple orgs — always aggregate at the enterprise level using `COALESCE(enterprise_id, org_id)`
   - **PR merge data depends on GitHub integration being properly configured.** If the enterprise hasn't set up the integration, `pr_state` and `pr_merged_at` will be incomplete or missing. Suspiciously low merge rates (<1%) with significant PR creation rates (>30%) usually indicate missing integration, not poor quality. When this happens, use PR creation rate as the productivity proxy instead of merge rate, and note the caveat explicitly. Always validate PR merge data with the customer before drawing conclusions.

6. **Anonymize all customer information.** Internal data powers the ROI metrics, but identities must be protected:
   - **Replace all customer names** with generic industry descriptors: "Top 3 US Bank", "Top 5 Global Investment Bank", "Fortune 100 Healthcare Technology Company", "Leading European Fintech (5,000+ engineers)", "Top 10 US Pharmaceutical Company"
   - Even if the customer is publicly listed on cognition.ai or devin.ai, replace the name unless the user explicitly approves keeping it
   - **Remove** identifying details: internal application names, specific programming languages not publicly known, repository counts, internal tool names, team sizes that could narrow identification
   - **Remove** any application names from sessions or languages not publicly associated with that customer
   - **Generalize technical specifics**: ".NET to Java migration" → "cross-platform service migration"; "1,800 repositories" → "large multi-repo estate"
   - **Round or range-ify metrics**: "$47.23 cost per merged PR" → "under $50 per completed task"
   - **Re-read the final document** end-to-end to confirm no customer names, identifying details, or traceable specifics remain

7. **Build the POV as a styled PDF document (max 5 pages).** Structure the content for executive readers. The document should NOT contain a company overview or a Devin/Cognition product description. Jump straight into value:

   **Document structure (5 pages maximum):**
   - **Cover page** (page 1): Co-branded header with logos, document title (e.g., "Accelerating [Customer]'s Cloud Modernization with the Cognition Platform"), date
   - **Strategic Alignment** (page 2, ~half page): Map the customer's top 3-5 stated business initiatives (from Investor Day, earnings calls, annual report) directly to Cognition Platform capabilities. Use their own language and cite specific public statements. Frame as: "[Customer] has stated [goal]. Here's how the Cognition Platform delivers."
   - **Technology Stack Integration** (page 2, ~half page): Show how Devin works as a co-worker within their existing tools — their source control, CI/CD, cloud platform, languages. Not a feature list — a narrative about fitting into their engineering workflow.
   - **Recommended Use Cases with Before/After ROI** (pages 3-4): For each use case, present the industry-specific challenge, how Devin addresses it in their stack, and a clear before/after comparison table showing time saved, cost reduced, and efficiency gained. Use real anonymized data.
   - **Proof Points & Recommended Next Steps** (page 5): Anonymized before/after data from similar-industry enterprises — lead with strongest cost savings and time-to-completion metrics. Then a specific pilot scope suggestion tied to their priorities (e.g., "Start with [specific use case] across [suggested team/scope] for a 30-day pilot").

   **Formatting:**
   - Build as HTML first for layout control, then convert to PDF
   - Use tables or comparison charts for before/after ROI data
   - Keep the tone direct — no padding, no vague adjectives, no long preambles. Start each section with why it matters to the customer before naming product features. Be specific about what makes challenges hard ("legacy systems that require 2-3 weeks of manual migration per service" not "systems that are hard to modernize"). State what the platform does, not what it doesn't do.
   - Include `@media print` CSS: `page-break-inside: avoid` on section containers, `break-inside: avoid` on tables, `page-break-before: always` before major sections
   - Aggressively edit for brevity — 5 pages is a hard cap. Every sentence must earn its space. Prefer fewer, bigger ideas over long lists. Shorter section openers beat paragraphs of setup.

8. **Apply co-branding.** Add dual branding to the document header:
   - **Default (Cognition + Customer)**: Get the Cognition logo from the cognition-website repo at `public/major-brand-assets/Cognition_PrimaryLockup_Black.png`. If not available, download from cognition.ai/brand. Embed as base64 data URI. Devin color palette for accents: Purple #3969CA, Green #21C19A, Blue #0294DE. Cognition logo top-left, customer logo top-right.
   - **Partner mode (Partner + Customer)**: If a channel partner was specified, use the partner's logo and brand colors instead of Cognition's. Partner logo top-left, customer logo top-right.
   - **Customer branding**: Use provided logo or search Wikipedia Commons / customer website. Extract brand colors.
   - **Fallback**: If any logo can't be obtained as an image, render the entity name as styled text in their brand colors using a clean sans-serif font.

9. **Convert to PDF and review.** Generate the PDF using the naming convention `PARTNER_CUSTOMER_Cognition_POV.pdf` — substitute the actual partner and customer names. Use underscores between words; if the partner or customer name contains spaces, replace them with underscores (e.g., `Ahead_AbbVie_Cognition_POV.pdf`, `Cognizant_Blue_Cross_Blue_Shield_Cognition_POV.pdf`). Then visually inspect:
   - Run: `google-chrome --headless=new --disable-gpu --no-sandbox --print-to-pdf=/path/to/PARTNER_CUSTOMER_Cognition_POV.pdf --no-pdf-header-footer "file:///path/to/branded.html"`
   - If Chrome headless fails, fall back to Playwright: `pip3 install playwright && python3 -m playwright install chromium`, then use `page.pdf()`
   - Open the PDF in Chrome and verify:
     - Both logos render correctly
     - No orphaned section titles
     - Tables not split across pages
     - Clean page flow
     - **Document is 5 pages or fewer** — if it exceeds 5 pages, tighten copy and reduce whitespace until it fits
   - Fix CSS and regenerate until clean.

10. **Deliver.** Attach the PDF (named `PARTNER_CUSTOMER_Cognition_POV.pdf`). Include a brief summary: key use cases covered, proof points used, and any anonymization decisions. Do NOT attach the source HTML.

11. **Offer partner-branded version.** After delivering the document, ask the user:
    > "Would you like me to produce an additional version of this document with partner-specific branding? If so, provide the partner name, logo (or I can search for it), and any brand colors or style preferences. I'll regenerate the PDF with Partner + Customer co-branding instead of Cognition + Customer."
    - If the user provides a partner, regenerate by repeating steps 8-9 using partner branding mode.
    - Keep all content, data, and anonymization identical — only visual branding changes.
    - Deliver the partner-branded version as a separate PDF alongside the original.
    - The user may request multiple partner-branded versions. Produce each as a separate PDF.

## Specifications

- Final deliverable: PDF only (do not deliver HTML source)
- **Maximum 5 pages** — this is a hard constraint
- **PDF file naming convention: `PARTNER_CUSTOMER_Cognition_POV.pdf`** — use actual partner and customer names with underscores replacing spaces (e.g., `Ahead_AbbVie_Cognition_POV.pdf`)
- Co-branded with Cognition (or specified channel partner) and the target customer
- No company overview or Devin/Cognition product description in the document
- All non-public customer names replaced with generic industry descriptors
- All non-public technical details removed from customer references
- **All internal data presented as before/after comparisons** — time saved, efficiency gains, cost savings. Never raw platform metrics (sessions, users, ACUs, active engineers, adoption ramp)
- Before/after ROI comparison for at least the top 3 use cases, grounded primarily in real internal data
- Each use case must be industry-specific, not generic
- Strategic alignment section must reference specific public statements from the customer (Investor Day, earnings calls, annual reports)
- Technology stack integration section must reference the customer's actual tools and platforms
- Executive-friendly: concise, data-driven, specific to the target customer's context
- Validation: Visual inspection of the final PDF confirming branding, page breaks, 5-page limit, and completeness

## Advice and Pointers

- Build as HTML first, then convert to PDF. This gives full control over layout and print styles. But only deliver the PDF.
- Embed logos as base64 data URIs so the PDF renders correctly without external dependencies.
- If `--print-to-pdf` produces no output, try `--headless=new`. If that also fails, Playwright is a reliable fallback.
- The Devin color palette (Purple #3969CA, Green #21C19A, Blue #0294DE) is for accents and headers, not body text.
- When anonymizing, err on the side of caution. If unsure whether a detail is public, remove it.
- The strongest proof points are before/after comparisons: "Before Devin: 8 hours per bug fix. After Devin: 45 minutes including review." Lead with cost savings and time compression.
- For cost comparisons, use fully-loaded engineer cost ($150-250/hr depending on geography) vs. Devin cost-per-merged-PR from internal data. This creates compelling ROI ratios.
- For the best proof point enterprises, look for those with: high PR merge rates (>40%), low cost per merged PR, and diverse use case categories.
- The `category` field on `dim_combined_sessions` maps directly to use case types (e.g., `migrations_and_upgrades` for cloud migration).
- When researching Investor Day decks, look for the technology-specific breakout sessions — these often have the most actionable detail on engineering priorities.
- Job postings are one of the best signals for tech stack. Search "[Company] software engineer" on LinkedIn and look at required technologies.
- Frame every use case in terms of the customer's business outcomes, not Devin's features. The customer cares about their migration timeline, not that Devin can write code.
- When presenting tech stack integration, avoid a feature checklist. Instead, tell a story: "Your engineers push to GitHub, Devin picks up the task, works in the same repos, opens a PR that triggers your existing Jenkins pipeline, and a human reviewer approves it — same workflow, additional capacity."
- 5 pages forces ruthless editing. Every sentence must deliver value. Cut introductory fluff, merge sections where possible, and use tables over paragraphs for data.

## Forbidden Actions

- Do not include a company overview or Devin/Cognition product description in the document
- Do not include any real customer name in the final document unless the user explicitly approves
- Do not fabricate ROI numbers — all metrics must be grounded in real internal customer data or public case studies
- Do not produce the document as plain markdown — build as HTML, deliver as PDF
- Do not skip the visual page-break review
- Do not exceed 5 pages under any circumstances
- Do not use inflated or vague language ("revolutionary", "game-changing") — be specific about measured outcomes
- Do not use `dim_sessions.gh_pr` for PR counts — always use `analytics.pull_requests`
- Do not share raw internal query results — all internal data must be anonymized before inclusion
- Do not present generic use cases — every use case must be tailored to the customer's industry and stated priorities
- Do not deliver the HTML source file — PDF only
- **Do not include raw platform usage metrics** in the final document — no total sessions, number of users, ACUs consumed, active engineers, or adoption ramp timelines. All internal data must be framed as before/after outcomes: time saved, efficiency gains, cost savings

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
