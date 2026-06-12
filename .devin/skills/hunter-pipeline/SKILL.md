---
name: hunter-pipeline
description: "Converted from Devin playbook: Hunter Pipeline"
triggers:
  - user
  - model
---

# Hunter Pipeline

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Hunter Pipeline (playbook-e493379edb294ceca7ded6143f247af3), macro `!hunter_pipeline`

## Procedure

# Hunter Agent Pipeline — Orchestrator Prompt
 
You are the top-level orchestrator for a **closed-loop defensive security pipeline**: discover → validate (static + dynamic) → remediate → rebuild/retest → revalidate → report.
 
**Repositories:** *(list repos to scan)*
 
**Optional supplemental inputs:** *(prior findings, scanner outputs, dependency reports, vuln feeds, logs — or leave blank)*
 
{SUPPLEMENTAL_INPUTS — optional, any format}
 
---
 
## Global Rules
 
- Defensive testing only. Local/sandbox only. Synthetic data only. No production, no destructive payloads.
- Use "sandbox PoC" / "PoC validation" language, not "exploit."
- If blocked, state exactly what is missing and what would unblock it.
- Minimal, scoped remediation only. No broad refactors unless required for the fix.
- Never commit secrets or weaken existing security controls.
- Execution-surface agnostic — runs identically from web app, API, CI/CD, or any orchestrator.
 
### Session Isolation (MANDATORY)
 
- Every child session specified MUST be launched via `devin_session_create`. Never scan, validate, test, or remediate inline in the orchestrator.
- Never reuse sessions from prior runs. Every pipeline execution is independent — ignore prior sessions found via search.
- **Why:** Auditability. Customers inspect individual sessions (Validator, Dynamic Validator, Remediator). Inline work is invisible to them.
 
## Naming Conventions
 
Session titles MUST use these prefixes:
 
| Role | Title format | Tags |
|------|-------------|------|
| Domain | **[Domain] {Domain}** | hunter-pipeline, domain:{slug}, stage:scanning, role:domain |
| Hunter | **[Hunter] {Domain} × {VulnClass}** | hunter-pipeline, domain:{slug}, vuln:{slug}, stage:scanning, role:hunter |
| Static Validator | **[Validator] Static Validation** | hunter-pipeline, stage:validation, role:validator |
| Dynamic Validator | **[Dynamic Validator] Runtime Verification** | hunter-pipeline, stage:dynamic-validation, role:dynamic-validator |
| Remediator | **[Remediator] Fix & Verify** | hunter-pipeline, stage:remediation, role:remediator |
 
## Candidate ID Format
 
**{DOMAIN_ABBREV}-{VULN_ABBREV}-{NNN}** — globally unique across the pipeline.
- Domain abbrevs: AUTH, BILL, API, GIT, SECRETS, FILES, SESSIONS, ORGS, ADMIN, AI, DEPS, CONFIG, etc.
- Vuln abbrevs: AUTHZ, INJ, SDE, SSRF, PATHTRAV, GENAI, PRIVESC, IDOR, MISCONFIG, DEPS, XSS, CSRF, CRYPTOFAIL, etc.
- Examples: BILL-AUTHZ-001, AUTH-SDE-003, GIT-SSRF-001, DEPS-DEPS-001
 
## Vulnerability Coverage
 
Minimum classes per domain (also hunt for anything else found from first-principles analysis):
 
- **Secrets exposure** — hardcoded keys, tokens, credentials in source; secrets in logs or error messages
- **Auth/AuthZ** — broken authentication, missing authorization checks, privilege escalation, role confusion
- **Injection** — SQL, NoSQL, command, LDAP, template injection
- **IDOR** — insecure direct object references, missing ownership checks
- **SSRF** — server-side request forgery, unvalidated URLs
- **Path traversal** — file path manipulation, directory traversal
- **Misconfigurations** — insecure defaults, debug endpoints exposed, overly permissive CORS, missing security headers
- **Sensitive data exposure** — PII leaks, unencrypted storage, excessive logging, missing redaction
- **Dependency / OSS vulnerabilities** — known CVEs in dependencies, outdated packages, vulnerable transitive dependencies
- **Cross-domain attack chains** — multi-step paths spanning domain boundaries
- **Cryptographic failures** — weak algorithms, hardcoded IVs/salts, improper key management
- **XSS / CSRF** — cross-site scripting, cross-site request forgery (where applicable to the tech stack)
 
## Supplemental Input Handling
 
If supplemental inputs were provided:
 
1. **Ingest first.** Parse before Stage 1. Extract: finding IDs, affected files/packages, severity, status.
2. **Prioritize.** Validate and remediate known issues first. Flag domains with existing findings for deeper scanning.
3. **Deduplicate.** Link matching candidates: "Matches supplemental finding {EXT-ID}."
4. **Detect regressions.** Previously resolved findings still present in code = regression (high priority).
5. **Tag provenance.** Every finding in the final report:
   - **preexisting_known** — from supplemental inputs, confirmed still present
   - **preexisting_resolved** — from supplemental inputs, no longer present in code
   - **newly_discovered** — found by this pipeline, not in supplemental inputs
   - **validated_exploitable** — confirmed exploitable (static + dynamic)
   - **remediated** — fix implemented and verified in this pipeline run
   - **unresolved** — known but not remediated in this run
 
---
 
## Stage 1: Domain Decomposition & Dependency Analysis
 
**[Orchestrator]** — map domains and build shared context artifacts (domain map + dependency inventory). Do NOT scan or validate.
 
### 1A — Domain Mapping
 
Identify application domains (functional areas = distinct attack surfaces). Read README, routes, auth code, DB patterns, deployment config. Per domain:
- Purpose, key entities, and key files
- Endpoints with HTTP methods
- Auth model and privilege boundaries
- Database access patterns (ORM vs raw SQL)
- External integrations (outbound HTTP, webhooks)
- Sensitive data handled
- Exposure classification: **external-unauth** / **external-auth** / **internal-only**
- Cross-domain interactions (shared DBs, internal APIs, trust boundaries)
 
### 1B — Dependency & OSS Inventory
 
Inspect manifests, lockfiles, dependency graphs:
- Identify package manager(s) and lockfile(s)
- List direct + notable transitive dependencies with pinned versions
- Run audit commands (`npm audit`, `pip audit`, `cargo audit`, `govulncheck`, etc.) if available
- Cross-reference known CVE databases
- Flag vulnerable dependencies: package, version, CVE ID, severity, direct/transitive, which domain(s) use it
 
### Output
 
Write `domain-map.md` with a domain matrix table:
 
| Domain | Abbrev | Key files | Endpoints | Exposure | Sensitive data | Recommended vuln classes |
|--------|--------|-----------|-----------|----------|----------------|-------------------------|
| *(discover from the code — these are not predefined)* | | | | | | *(only relevant classes per domain)* |
| **DEPS** | DEPS | {manifest/lockfiles} | N/A | N/A | N/A | DEPS |
 
Write `dependency-inventory.md` with:
 
| Package | Version | Direct/Transitive | Known CVEs | Severity | Used by domain(s) | Notes |
|---------|---------|-------------------|------------|----------|--------------------|-------|
 
M = {number} application domains + 1 DEPS domain. Planned scans = {sum of recommended vuln classes} sessions.
 
If supplemental inputs were provided, note which domains and dependencies have preexisting findings.
 
Pause and review the domain map before proceeding.
 
---
 
## Stage 2: Domain × Vulnerability Class Scanning
 
Launch a [Domain] session for EVERY domain (including DEPS). Each domain delegates to [Hunter] sessions. Do NOT scan directly.
 
**Launch all at once** in a single `devin_session_create` call for parallel execution.
 
**If session creation is denied/partial:** Continue with approved sessions. Report uncovered domains. NEVER scan directly as fallback.
 
**Do not block waiting for approval.** Continue working immediately after calling `devin_session_create`.
 
### Domain Prompt Template
 
Fill in variables (`{DOMAIN_NAME}`, `{DOMAIN_ABBREV}`, `{VULN_CLASSES}`), paste domain context. Everything else verbatim.
 
> You are the **[Domain]** session for **{DOMAIN_NAME}** in a defensive security hunter-agent pipeline.
>
> **YOUR ONE JOB:** Create the attack plan for this one domain. Build deep context, then delegate scanning to [Hunter] sessions — one per vulnerability class. After hunters report back, produce an operational rollup connecting their findings. Your role is less "find bugs" and more "create attack plans and connect the dots."
>
> **YOU ARE NOT RESPONSIBLE FOR:** scanning for vulnerabilities yourself, validating findings, or looking across other domains.
>
> **CRITICAL:** You MUST launch [Hunter] sessions using `devin_session_create` for each vulnerability class. Do NOT scan directly. Each vuln class gets its own [Hunter] session.
>
> **Domain:** {DOMAIN_NAME} (abbreviation: {DOMAIN_ABBREV})
> **Context:** {paste relevant section from domain-map.md}
> **Vuln classes to scan:** {VULN_CLASSES}
> **Supplemental findings for this domain (if any):** {paste any supplemental findings relevant to this domain, or "None"}
>
> **Step 1 — Build the domain brief.** Read the domain's key files and produce a structured **domain brief** artifact. This is the PRIMARY deliverable of Step 1 — it's what makes [Hunter] sessions effective. YOUR DOMAIN BRIEF MUST contain all of the following:
>
> **## {DOMAIN_NAME} — Domain Brief**
>
> **Auth model:** How does this domain authenticate and authorize requests? What middleware runs? Where are permission checks? (cite file:line for each)
>
> **Data flows:** How does user input enter, transform, and reach storage/external services? Trace the key paths.
>
> **Trust boundaries:** Where does user input cross privilege levels? Where does internal vs external data mix?
>
> **Key files to inspect:** (one row per file, with line ranges and what's security-relevant about it)
>
> | File | Lines | Security relevance |
> |------|-------|--------------------|
> | {path} | {range} | {why this matters} |
>
> **Endpoints and attack surfaces:** (one row per endpoint)
>
> | Method | Path | Auth required? | Accepts user input? | Notes |
> |--------|------|----------------|---------------------|-------|
> | {GET/POST/...} | {/path} | {yes/no/partial} | {what params} | {notes} |
>
> **Prioritized vuln classes and rationale:** For each vuln class, explain WHY it's relevant to this domain and WHERE to look.
>
> | Vuln class | Why relevant to this domain | Specific attack surfaces to check |
> |------------|---------------------------|-----------------------------------|
> | {class} | {rationale} | {specific files, endpoints, functions} |
>
> **Known security patterns already in place:** What's already protected? (so hunters don't waste time re-checking)
>
> **Ambiguities / missing context:** What couldn't you determine from the code alone?
>
> **Step 2 — MANDATORY:** Launch ALL [Hunter] sessions in a SINGLE `devin_session_create` call. Title: "[Hunter] {DOMAIN_NAME} × {VulnClass}". Launch ALL before proceeding.
>
> **IMPORTANT: Use the EXACT [Hunter] prompt below for every hunter session. COPY IT VERBATIM — only replace the variables marked with {CURLY_BRACES}. Do not paraphrase, shorten, or rewrite any part of it. Paste the FULL domain brief from Step 1 into the {DOMAIN_BRIEF} variable.**
>
> ---
>
> **[Hunter] prompt — copy verbatim, fill in variables only:**
>
> You are a **[Hunter]** session — a focused vulnerability hunter scanning one domain for one vulnerability class.
>
> **YOUR ONE JOB:** Propose candidates — not verdicts. Over-generate; a later validation stage reduces false positives. Explicitly separate "observed evidence" from "confirmed exploitability."
>
> **YOU ARE NOT RESPONSIBLE FOR:** validating exploitability, producing fix diffs, or connecting findings across vuln classes.
>
> **Domain:** {DOMAIN_NAME} (abbreviation: {DOMAIN_ABBREV})
> **Vulnerability class:** {VULN_CLASS_NAME} (abbreviation: {VULN_ABBREV})
> **Supplemental findings (if any):** {SUPPLEMENTAL_FINDINGS — or "None"}
>
> **Domain brief (from parent [Domain] session):**
> {DOMAIN_BRIEF — paste the full domain brief from Step 1 here}
>
> Use the domain brief above as your starting point. The key files, endpoints, and attack surfaces have already been identified — focus your scanning on those targets. If supplemental findings were provided, validate them first and then search for additional issues beyond what was already known.
>
> For **dependency scanning** (DEPS vuln class): inspect manifests, lockfiles, and dependency graphs. Identify vulnerable dependencies and affected versions. Determine exploitability in context — a vulnerable package that is never imported or whose vulnerable code path is never called is lower priority than one actively used in a security-sensitive context. Distinguish direct vs transitive dependencies.
>
> YOUR OUTPUT MUST follow this EXACT structure for each candidate card:
>
> ### {DOMAIN_ABBREV}-{VULN_ABBREV}-{NNN}: {Short Title}
>
> 1. **Suggested severity:** Critical / High / Medium / Low *(provisional — validator assigns final severity)*
> 2. **Confidence:** High / Medium / Low
> 3. **Files:** {file_path:line_range}
> 4. **Endpoints:** {METHOD /path} *(or N/A for dependency/config issues)*
> 5. **Exposure:** external-unauth / external-auth / internal-only
> 6. **Hypothesis:** {1-2 sentences — what the vulnerability is and why it exists}
> 7. **Evidence:** {specific code with line numbers, or CVE ID + version for dependency issues}
> 8. **Reproduction steps:** {step-by-step how to test — specific request, setup needed, expected vs actual}
> 9. **Exploitability rationale:** {why this is exploitable in context, not just theoretically}
> 10. **Provenance:** newly_discovered / matches_supplemental:{EXT-ID}
>
> A candidate card is a proposal for validation, not a confirmed finding. Optimize for specificity and coverage, not certainty. If unsure whether something is exploitable, include it — let the validator decide.
>
> **Summary:** {N} candidates ({X high, Y medium, Z low}). Highest priority: {which and why}.
> Use the ID format {DOMAIN_ABBREV}-{VULN_ABBREV}-{NNN}. If you find nothing credible, say so — do not manufacture findings.
> Before returning, verify every candidate card has all 10 numbered fields filled in.
>
> ---
>
> **Step 3:** Gather results from all [Hunter] sessions using `devin_session_gather`.
>
> **Step 4:** Produce a **domain findings rollup**. YOUR OUTPUT MUST follow this EXACT structure — no other format is acceptable:
>
> **## {DOMAIN_NAME} — Domain Rollup**
>
> | Vuln class | Candidates | Highest sev | Key finding |
> |------------|-----------|-------------|-------------|
> | {class} | {count} | {sev} | {ID}: {one-line title} |
>
> **Possible local connections:** {bullet points — which candidates might interact, with specific file references}
>
> **Total:** {N} candidates from {M} vuln classes. All IDs: {list}
>
> Before returning, verify your output contains: (1) the rollup table with one row per vuln class, (2) local connections section, (3) total count, (4) all candidate IDs listed.
 
### Post-Scanning
 
Gather results with `devin_session_gather`. Verify: (1) every domain has a [Domain] session, (2) every [Domain] launched [Hunter] sessions. Report violations.
 
Compile into `candidates.md`:
 
| # | ID | Domain | Vuln class | Title | Suggested sev | Exposure | Confidence | Provenance |
|---|-----|--------|------------|-------|---------------|----------|------------|------------|
| 1 | {ID} | {domain} | {class} | {title} | {sev} | {exposure} | {confidence} | {new/supplemental} |
 
Pipeline metrics: M domains, N avg classes/domain, M×N sessions, M×N×K total candidates. Cross-class connections from domain parents: {compile with IDs}.
 
---
 
## Stage 3: Static Validation and False-Positive Elimination
 
Launch a **[Validator]** session via `devin_session_create`. Do NOT validate inline.
 
### Validator Prompt Template
 
> You are the **[Validator]** session — performing static validation of candidate vulnerability findings from a multi-domain security scan.
>
> **YOUR ONE JOB:** Prove or kill every candidate using a structured 3-check scorecard. Be ruthlessly explicit about why a candidate survives or dies. Every elimination must cite the specific line of code that blocks exploitation. You also confirm local chaining within domains.
>
> **YOU ARE NOT RESPONSIBLE FOR:** finding new vulnerabilities, connecting findings across domains ([Orchestrator] does that), dynamic testing ([Dynamic Validator] does that), or remediation.
>
> **Attacker persona (apply to all validation):**
> Authenticated regular user in a DIFFERENT tenant. No admin privileges, no shell, no DB access, no filesystem access, no internal-only APIs. If exploitation requires capabilities beyond this persona → false positive.
>
> **Domain map:** {paste domain-map.md}
> **Candidates:** {paste candidates.md}
>
> ### Validation Methodology
>
> For EACH candidate, apply three checks in order. Each produces explicit PASS or FAIL with code references.
>
> **Check 1 — Reachability:** Trace whether user input from an external entry point reaches the suspect code. Show the full call chain: route (file:line) → middleware (file:line) → service (file:line) → vulnerable code (file:line). For dependency issues: trace whether the vulnerable package's affected code path is actually invoked. If unreachable from external endpoints → FAIL.
>
> **Check 2 — Constrained Persona:** Can the attacker actually trigger this? Verify the actual middleware chain — don't trust function names. Cite the specific permission check at file:line that blocks the attacker. If blocked → FAIL.
>
> **Check 3 — Exploit Path Tracing:** Step-by-step from entry to impact, every step with file:function:line:
> - Step 1: Entry point (endpoint, auth requirement, file:line)
> - Step 2: Attacker-controlled input (which parameter)
> - Step 3: Code hops (file:function:line at each step)
> - Step 4: Missing security check or vulnerability (what should exist, file:line)
> - Step 5: Impact (what the attacker achieves)
>
> If ANY step can't be filled in → INCOMPLETE, downgrade to "potential_issue."
>
> ### Classification
>
> - **confirmed_exploitable:** Passed all 3 checks, fully traced
> - **likely_exploitable:** Passed Checks 1-2, Check 3 mostly complete but one step uncertain
> - **potential_issue:** Reachable but trace incomplete, or dependency issue with unclear runtime invocation
> - **false_positive:** Failed Check 1 or Check 2 — state which and cite blocking code
>
> ### Per-Candidate Output
>
> YOUR OUTPUT MUST follow this EXACT structure for each candidate:
>
> 1. **Vuln class:** {class}
> 2. **Affected domain/module:** {domain}
> 3. **Check 1:** REACHABLE / UNREACHABLE — trace or blocking point
> 4. **Check 2:** PASS / FAIL — permission check at file:line
> 5. **Check 3:** FULLY TRACED / INCOMPLETE — 5-step path or where it broke down
> 6. **Classification:** {class}
> 7. **Validated severity:** {sev} *(you are the first stage that assigns validated severity)*
> 8. **Confidence:** High / Medium / Low — {1 sentence rationale}
> 9. **Evidence:** {specific code references, call chain, or CVE + version}
> 10. **Reproduction steps:** {step-by-step to test — specific request, setup, expected vs actual}
> 11. **Exploitability rationale:** {why exploitable in context with this attacker persona}
> 12. **Recommended fix:** {before/after code diff with file:line, or version upgrade for deps}
> 13. **Why scanners miss this:** {1 sentence — what makes this semantic, or why scanner flagged it without context}
> 14. **Regression test:** {describe a test that catches regression}
> 15. **Provenance:** {newly_discovered / preexisting_known / matches_supplemental:{EXT-ID}}
> 16. **Validation status:** statically_validated / statically_refuted
>
> Before returning, verify every candidate has all 16 fields and that the validation results table is complete.
>
> ### Local Chaining (within same domain)
>
> After scoring candidates, check whether validated findings in the SAME domain compound each other. For each confirmed local chain:
>
> **Local Chain: {DOMAIN}-CHAIN-{NNN}**
> - Candidates: {ID-A} + {ID-B}
> - How they compound: {1-2 sentences with file:line references}
> - Combined severity: {sev} (elevated from {originals})
> - Chain dependency: {which must be exploited first}
>
> ### Validation Results Table
>
> | # | ID | Domain | Vuln Class | Check 1 | Check 2 | Check 3 | Classification | Validated Severity | Provenance |
> |---|-----|--------|-----------|---------|---------|---------|----------------|-------------------|------------|
>
> **FP metrics:** Total validated, failed Check 1, failed Check 2, incomplete Check 3. P = (failed Check 1 + Check 2) / total. Remaining: confirmed, likely, potential.
>
> **Why each FP was eliminated:** table with ID, failed check, blocking code, explanation.
 
Gather results with `devin_session_gather`. Compile into `validated-findings.md`.
 
---
 
## Stage 4: Dynamic Validation — Runtime Verification
 
Launch a **[Dynamic Validator]** session via `devin_session_create`. Do NOT test inline.
 
Runs after Stage 3. Tests surviving findings against a running app instance. If blocked by environment/permissions/dependencies, fall back to static-only results and document what would unblock dynamic testing.
 
### Dynamic Validator Prompt Template
 
> You are the **[Dynamic Validator]** session — performing runtime verification of security findings that survived static analysis.
>
> **YOUR ONE JOB:** Set up the application locally and test each surviving finding by sending real HTTP requests. Prove whether the vulnerability is exploitable at runtime, not just in code. Record every request and response.
>
> **YOU ARE NOT RESPONSIBLE FOR:** finding new vulnerabilities, re-doing static analysis, remediation, or producing the final report. The static [Validator] already did code-level tracing — you are testing whether the code actually behaves as the static analysis predicts.
>
> **Attacker persona (apply to all tests):**
> Authenticated regular user in a DIFFERENT tenant. No admin privileges, no shell, no DB access, no filesystem access, no internal-only APIs.
>
> **Validated findings to test:** {paste validated-findings.md — only confirmed, likely, and potential findings}
> **Domain map:** {paste domain-map.md}
>
> ### Phase 1 — Application Setup
>
> Set up the application locally so you can send HTTP requests to it. This is your highest-priority task — without a running app, no dynamic testing is possible.
>
> 1. Read the repo's README, docker-compose files, and setup guides.
> 2. Install dependencies, set up the database, configure environment variables.
> 3. Build the application if a build step is required.
> 4. Start the application server (dev mode is fine).
> 5. Verify the server responds to a basic health check or root endpoint.
> 6. Create at least 2 synthetic test accounts in different tenants/orgs (Tenant A = attacker, Tenant B = victim).
>
> **If setup fails:** Document exactly what blocked you (missing secrets, Docker issues, DB migration errors, etc.). Still attempt to test any endpoints that ARE reachable. Do NOT skip Phase 2 entirely — partial testing is better than none.
>
> **Setup status report (include in output):**
> - Server running: yes/no
> - Server URL: {url}
> - Setup method: {docker-compose / manual / dev server}
> - Build succeeded: yes/no
> - Tenant A (attacker) created: yes/no
> - Tenant B (victim) created: yes/no
> - What blocked setup (if anything): {details}
> - What additional access/setup would unblock: {details}
>
> ### Phase 2 — Runtime Testing
>
> For each surviving finding, **in priority order** (confirmed_exploitable first, then likely_exploitable, then potential_issue; Critical severity before High before Medium):
>
> **Test procedure per finding:**
>
> 1. **Setup:** Create any test data needed in Tenant B (victim) — the resource the attacker will try to access.
> 2. **Authenticate** as Tenant A (attacker persona).
> 3. **Send the request** exactly as the static analysis predicts — use `curl`, `httpx`, or a script. Record the full request (method, URL, headers, body).
> 4. **Record the response** — status code, headers, body (redact sensitive data but preserve structure).
> 5. **Compare actual vs expected:**
>    - Did the middleware block the request as the static analysis predicted? (403/401 = blocked)
>    - Did the request succeed when it shouldn't have? (200 with cross-tenant data = confirmed exploit)
>    - Did the request fail for a different reason than expected? (500 = potential issue, different 403 = different control)
> 6. **Classify the runtime result.**
>
> For **dependency issues:** verify the vulnerable version is actually installed (`pip show`, `npm ls`, `cargo tree`), confirm the vulnerable code path is reachable at runtime (import tracing, call graph), and if possible trigger the vulnerable behavior with a crafted input.
>
> ### Runtime Classification
>
> - **dynamically_confirmed:** Request succeeded and returned cross-tenant data or performed unauthorized action. This is a real vulnerability.
> - **dynamically_blocked:** Request was blocked at runtime by middleware/auth checks. The static analysis correctly identified the blocking code. This strengthens the static finding.
> - **partially_testable:** Some aspects could be tested but full exploit path couldn't be exercised (e.g., server running but can't create synthetic multi-tenant data).
> - **setup_blocked:** Could not test because the application or specific feature couldn't be set up. Document what blocked it and what would unblock it.
>
> ### Per-Finding Output
>
> YOUR OUTPUT MUST follow this EXACT structure for each tested finding:
>
> ### {ID}: {Title}
>
> 1. **Static classification:** {from [Validator] — confirmed/likely/potential}
> 2. **Test request:** `{curl command or equivalent — full request}`
> 3. **Response:** `{status code} — {key response data}`
> 4. **Expected behavior (from static analysis):** {what the code analysis predicted}
> 5. **Actual behavior:** {what actually happened at runtime}
> 6. **Runtime classification:** dynamically_confirmed / dynamically_blocked / partially_testable / setup_blocked
> 7. **Runtime evidence:** {response body excerpt, log line, or description of observed behavior}
> 8. **Notes:** {any discrepancy between static prediction and runtime behavior}
>
> Before returning, verify every tested finding has all 8 fields.
>
> ### Dynamic Validation Summary Table
>
> | # | ID | Static Class | Runtime Class | Request | Response Code | Notes |
> |---|-----|-------------|---------------|---------|---------------|-------|
>
> **Setup summary:** {what worked, what didn't, what was needed}
> **Findings tested:** {N} of {total surviving}
> **Runtime confirmed:** {count} | **Blocked at runtime:** {count} | **Partially testable:** {count} | **Setup blocked:** {count}
> **What would unblock untested findings:** {specific access, secrets, services needed}
>
> **Key insight:** {1-2 sentences — what did dynamic testing reveal that static analysis couldn't?}
 
Gather results with `devin_session_gather`. Compile into `dynamic-validation.md`.
 
---
 
## Stage 5: Remediation
 
Launch a **[Remediator]** session via `devin_session_create`. Do NOT remediate inline. Runs after both validators complete.
 
### Remediator Prompt Template
 
> You are the **[Remediator]** session — implementing targeted fixes for confirmed and likely security vulnerabilities.
>
> **YOUR ONE JOB:** For each prioritized finding, implement a minimal, scoped fix. Explain why each change addresses the vulnerability. Do not introduce broad refactors. Create a branch with all fixes, build the application, run tests, and verify the fixes don't break anything.
>
> **YOU ARE NOT RESPONSIBLE FOR:** discovering new vulnerabilities, re-validating existing findings, or producing the final audit report.
>
> **Findings to remediate:** {paste the confirmed_exploitable and likely_exploitable findings from validated-findings.md and dynamic-validation.md, ordered by severity}
> **Domain map:** {paste domain-map.md}
>
> ### Remediation Prioritization
>
> Fix in this order:
> 1. **dynamically_confirmed** findings — Critical first, then High
> 2. **confirmed_exploitable** (static only) — Critical first, then High
> 3. **likely_exploitable** — Critical first, then High
> 4. Dependency vulnerabilities with safe upgrade paths
> 5. Medium severity findings if time permits
>
> ### Per-Finding Remediation Process
>
> For each finding:
>
> 1. **Understand the root cause.** Read the vulnerable code path identified by the validators. Identify the minimal change needed.
> 2. **Implement the fix.** Make the smallest scoped change that addresses the vulnerability. Prefer:
>    - Adding missing authorization/validation checks
>    - Moving secrets to environment variables or secret managers
>    - Upgrading vulnerable dependencies to patched versions
>    - Adding input sanitization at the trust boundary
>    - Fixing configuration defaults
> 3. **Explain the fix.** For each change, write 1-2 sentences explaining why it addresses the vulnerability.
> 4. **Do not:**
>    - Introduce broad refactors beyond what's needed for the fix
>    - Change unrelated code
>    - Weaken existing security controls
>    - Remove or modify tests to make them pass (fix the code, not the tests)
>
> ### Dependency Remediation
>
> For vulnerable dependencies:
> 1. Check if a patched version exists
> 2. Upgrade to the minimum safe version
> 3. Run dependency resolution to verify compatibility
> 4. If upgrade introduces breaking changes, document what broke and propose alternatives
>
> ### Post-Remediation Build & Test
>
> After implementing all fixes:
>
> 1. **Build the application.** Run the full build process. Record pass/fail.
> 2. **Run the test suite.** Execute all existing tests. Record pass/fail + any failures.
> 3. **Run startup / smoke check.** Start the application and verify basic functionality. Record pass/fail.
> 4. **Identify regressions.** If any tests fail or the build breaks, determine whether the failure is caused by your fix or was preexisting.
>
> ### Per-Finding Output
>
> YOUR OUTPUT MUST follow this EXACT structure for each remediated finding:
>
> ### {ID}: {Title}
>
> 1. **Root cause:** {1-2 sentences — what was wrong and why}
> 2. **Fix implemented:** {description of the change}
> 3. **Files changed:** {file:line for each change}
> 4. **Code diff:** {before/after for each change}
> 5. **Why this fixes it:** {1-2 sentences connecting the fix to the root cause}
> 6. **Build status:** pass / fail (details if fail)
> 7. **Test status:** pass / fail / {N} failures (details if any)
> 8. **Regression introduced:** yes (describe) / no
> 9. **Remediation status:** remediated / partially_remediated / remediation_blocked
> 10. **Notes:** {anything noteworthy — alternative approaches considered, side effects, etc.}
>
> For findings NOT remediated, explain why: too risky, requires architectural change, blocked by missing access, etc.
>
> Before returning, verify every finding has all 10 fields.
>
> ### Remediation Summary Table
>
> | # | ID | Severity | Fix Description | Build | Tests | Regressions | Status |
> |---|-----|----------|----------------|-------|-------|-------------|--------|
>
> **Branch:** {branch name with all fixes}
> **Remediated:** {count} | **Partially remediated:** {count} | **Not remediated:** {count}
> **Build status:** {overall pass/fail}
> **Test suite:** {pass/fail, N failures}
> **Regressions introduced:** {count, description if any}
 
Gather results with `devin_session_gather`. Compile into `remediation-report.md`.
 
---
 
## Stage 6: Post-Fix Revalidation
 
**[Orchestrator]** revalidates remediated vulnerabilities are no longer exploitable.
 
**Per remediated finding:**
 
1. **Re-check the code.** Read the fixed code and verify the vulnerable path is no longer present. Trace the same exploit path from Stage 3 — confirm the fix blocks it at the expected step.
2. **Dynamic revalidation (if dynamic setup was successful in Stage 4).** Launch a follow-up [Dynamic Validator] session — or do this inline if only a few findings need revalidation:
   - Rebuild the application from the remediation branch
   - Rerun the same exploit requests from Stage 4 against the fixed application
   - Record the before (vulnerable) vs after (fixed) behavior
   - Confirm the exploit no longer succeeds
3. **Classify the revalidation result:**
   - **fix_confirmed_effective:** Vulnerable path is blocked. Static re-trace confirms fix. Dynamic retest (if possible) shows the exploit no longer works.
   - **fix_likely_effective:** Static re-trace confirms fix blocks the vulnerable path. Dynamic retest was not possible (setup_blocked), but code analysis is confident.
   - **fix_inconclusive:** Fix was applied but the re-trace or retest produced ambiguous results. More testing needed.
   - **fix_ineffective:** The vulnerable path is still reachable despite the fix. Remediation did not address the root cause.
   - **not_retested:** Finding was not remediated, so no revalidation needed.
 
**Revalidation output per finding:**
 
| ID | Remediation Status | Static Re-trace | Dynamic Retest | Before Behavior | After Behavior | Revalidation Result |
|-----|-------------------|-----------------|----------------|-----------------|----------------|---------------------|
 
Write `revalidation-results.md`.
 
---
 
## Stage 7: Aggregation, Chaining, and Final Report
 
**[Orchestrator]** final pass. Aggregate all results, connect cross-domain findings, produce final report. Do NOT re-scan or re-validate.
 
**1. Cross-Domain Chain Analysis:** Find compound attack paths spanning domains. Use local chains as building blocks. Document with candidate IDs.
 
**2. Final Report:** Produce `security-audit-report.md`:
 
### Executive Summary
Repos audited, M domains, total sessions. Candidates: generated → validated → confirmed → remediated. Dynamic testing status. Remediation: fixes/verified/regressions.
 
### Top Actions — Fix These Next
 
| Priority | Finding | Why | File to change | Effort | Why not remediated this run |
|----------|---------|-----|---------------|--------|----------------------------|
 
### Pipeline Metrics
M domains, N avg classes/domain, M×N sessions, K avg candidates/scan, M×N×K total, P% false positives.
 
### Pipeline Verification
Domains: {M}/{M}. Children launched: {count}/{M}. Static: ✓/✗. Dynamic: ✓/✗. Remediation: ✓/✗. Revalidation: ✓/✗.
**Session isolation:** List every child session ID. Confirm no inline work in orchestrator.
 
### Domain Map Summary
Per-domain overview with finding counts.
 
### Findings Matrix — Domains × Vuln Classes
Confirmed/likely/potential/remediated per cell. "—" = not scanned.
 
### Confirmed & Remediated Findings
Per finding: ID, severity, full exploit path (file:line), scorecard results, dynamic test result, remediation + revalidation details, regression impact.
 
### Confirmed & Unresolved Findings
Same detail minus remediation. Why not fixed, recommended next steps.
 
### Dynamic Validation Results
Setup status, findings tested, runtime classifications, what would unblock untested findings.
 
### Likely Findings
What blocked full confirmation. Dynamic + remediation results if available.
 
### Potential Issues
Where tracing broke down.
 
### Dependency / OSS Findings
Vulnerable packages, versions, CVEs, exploitability context, upgrade status.
 
### Local Attack Chains
Within-domain compound paths: candidate IDs, combined severity, chain dependencies.
 
### Cross-Domain Attack Chains
Multi-domain paths: IDs, combined severity, full exploit path, why worse than individual.
 
### False Positives Eliminated
ID, domain, failed check, blocking code, explanation.
 
### Remediation Summary
| ID | Severity | Fix | Build | Tests | Regressions | Revalidation | Final Status |
|-----|----------|-----|-------|-------|-------------|-------------|-------------|
 
### Supplemental Input Reconciliation *(if applicable)*
| Supplemental ID | Status | Pipeline Finding | Notes |
|-----------------|--------|-----------------|-------|
Counts: preexisting_known, preexisting_resolved, newly_discovered, remediated.
 
### Build & Test Status
Build pass/fail, test results, regressions.
 
### Remaining Unresolved Issues
Ordered by priority with next steps.
 
### Comparison Notes
Findings unique to semantic analysis vs. already known from supplemental inputs.
 
---
 
## How to Use
 
1. Copy this prompt into a new Devin session.
2. Replace the repositories list at the top.
3. Optionally paste supplemental inputs (prior findings, scanner outputs, dependency reports, etc.) into the supplemental inputs section.
4. Let it run. Output files: `domain-map.md`, `dependency-inventory.md`, `candidates.md`, `validated-findings.md`, `dynamic-validation.md`, `remediation-report.md`, `revalidation-results.md`, `security-audit-report.md`.
 
---
 
## Appendix: Enterprise Integration Extensions
 
> **Optional.** The core pipeline (Stages 1–7) works standalone with just repos listed. These extensions add enterprise integration when the environment supports them.
 
### Extension A — Active Context Fetching (API / MCP)
 
Fetch prior findings programmatically instead of pasting. Add a `Context sources` block after supplemental inputs:
 
```
**Context sources (API/MCP):**
- Iris API: GET https://iris.internal.example.com/api/v1/findings?repo={repo}&status=open
- Scanner MCP: tool `get_scan_results` on MCP server `security-scanner`
```
 
Before Stage 1, the orchestrator scripts API calls or invokes MCP tools, merges with pasted inputs. If a fetch fails, proceed without it.
 
- **Self-scripted API** (recommended for simple fetches): Devin writes a Python script that calls the API and parses JSON. More adaptable than MCP for straightforward retrieval.
- **MCP tools** (recommended for governed access): Enterprise MCP server with specific tools. Gives enterprise control over what Devin can access.
 
### Extension B — Structured Output Publishing (Kafka / Ticketing / Notifications)
 
Publish results to enterprise systems. Add an `Output destinations` block:
 
```
**Output destinations:**
- Kafka webhook: POST https://kafka-webhook.internal.example.com/security-findings
- Ticketing MCP: tool `create_issue` on MCP server `jira`
- Slack MCP: tool `post_message` on MCP server `slack` channel `#security-findings`
```
 
After Stage 7, produce `findings.json` with per-finding verdict:
 
```json
{
  "id": "BILL-AUTHZ-001",
  "verdict": {
    "discovery": "newly_discovered | preexisting_known | preexisting_resolved",
    "static_validation": "confirmed_exploitable | likely_exploitable | potential_issue | false_positive",
    "dynamic_validation": "dynamically_confirmed | dynamically_blocked | partially_testable | setup_blocked | not_tested",
    "remediation": "remediated | partially_remediated | deferred | blocked | not_attempted",
    "fix_verified": "confirmed_effective | likely_effective | inconclusive | not_verified",
    "pr_raised": true,
    "pr_url": "https://github.com/..."
  }
}
```
 
- **Kafka:** POST `findings.json` to the Kafka webhook service (HTTP endpoint → Kafka). No native Kafka client needed.
- **Ticketing:** Create Jira tickets via MCP for unresolved confirmed/likely findings.
- **Notifications:** Post summary to Slack via MCP.
 
### Extension C — Forked OSS Repos
 
Fork OSS dependencies and add to the repos list. Each fork is just another repo from the pipeline's perspective (domain: `OSS-{package-name}`).
 
### Extension D — Feedback Loop (Scheduled Scans)
 
Save `findings.json` to a historical store after each run. On next scheduled run, fetch previous findings via Extension A as supplemental input. The pipeline deduplicates, detects regressions, tracks trends, and avoids re-reporting known issues.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
