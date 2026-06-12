---
name: scheduled-security-scan-snyk-mcp
description: "Converted from Devin playbook: Scheduled Security Scan (Snyk MCP)"
triggers:
  - user
  - model
---

# Scheduled Security Scan (Snyk MCP)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Scheduled Security Scan (Snyk MCP) (playbook-21b169def5c34cc4a952667fa216d248), macro `!scheduled_security_scan`

## Procedure

# Scheduled Security Scan (Snyk MCP)

## Overview
This playbook runs a comprehensive Snyk security scan using the Snyk MCP integration, analyzes vulnerabilities by severity, and creates fix PRs for the top 5 most critical findings. It is designed to run on a schedule as an always-on security guardian.

## What's Needed From User
- Repository to scan (provided via prompt or schedule configuration)
- Snyk MCP integration must be enabled for the session

<phase name="Authentication & Setup" id="1">
## Phase 1: Authentication & Setup

1. Run `snyk_auth` to authenticate with Snyk via the MCP integration
2. Confirm authentication is successful and note the authenticated account
3. Run `snyk_trust` on the repository root folder to allow scanning
4. Confirm the folder is trusted
5. Identify the project type by examining build files (pom.xml, package.json, build.gradle, requirements.txt, etc.)

<verification>
- Snyk MCP authentication completed successfully
- Repository folder is trusted for scanning
- Project type and build system identified
</verification>
</phase>

<phase name="Comprehensive Security Scan" id="2">
## Phase 2: Comprehensive Security Scan

Run all applicable Snyk scan types for comprehensive coverage:

1. **SCA Scan (Software Composition Analysis)**: Run `snyk_sca_scan` with the repository path (absolute path required) and `unmanaged: true`. This scans dependencies for known vulnerabilities.
2. **SAST Scan (Static Application Security Testing)**: Run `snyk_code_scan` on the repository to find code-level vulnerabilities.
3. **IaC Scan (Infrastructure as Code)**: If Dockerfiles, Terraform files, Kubernetes manifests, or other IaC files exist, run `snyk_iac_scan`.
4. **Container Scan**: If container images are available, run `snyk_container_scan`.
5. **SBOM Scan**: If an SBOM file exists, run `snyk_sbom_scan`.

For each scan, capture:
- Total number of vulnerabilities found
- Breakdown by severity (Critical, High, Medium, Low)
- Package names and versions affected
- CVE identifiers where available

<verification>
- SCA scan completed and results captured
- SAST code scan completed and results captured
- IaC scan completed (if applicable) and results captured
- All scan results documented with severity breakdowns
</verification>
</phase>

<phase name="Analysis & Reporting" id="3">
## Phase 3: Analysis & Reporting

1. Compile a unified vulnerability report across all scan types
2. Organize findings into three sections:
   - **Critical Findings**: All critical and high severity vulnerabilities
   - **Infrastructure Findings**: IaC issues, Dockerfile problems, CI/CD misconfigurations
   - **Snyk MCP Integration Assessment**: Scan coverage summary and tool performance
3. For each Critical/High finding, document:
   - Vulnerability title and CVE ID
   - Affected package and current version
   - Fixed version (if available)
   - Severity level and CVSS score
   - Brief description of the risk
4. Create a prioritized remediation plan ranking vulnerabilities by severity (Critical first, then High) and by CVSS score within each severity level
5. Present the full report to the user within the session

<verification>
- Unified vulnerability report compiled across all scan types
- Findings organized by severity with CVE details
- Remediation plan created with prioritized actions
- Report presented to user in the session interface
</verification>
</phase>

<phase name="Automated Remediation" id="4">
## Phase 4: Automated Remediation

**IMPORTANT: Only create fix PRs for the top 5 most critical vulnerabilities.** Rank by severity (Critical first, then High) and by CVSS score within each severity level. Do NOT attempt to fix all vulnerabilities — only the top 5.

For each of the top 5 vulnerabilities that has a known fix:

1. Create a dedicated branch for the fix (one branch per vulnerability)
2. Apply the fix:
   - For dependency vulnerabilities: Update the version in the manifest file (pom.xml, package.json, build.gradle, etc.)
   - For code vulnerabilities: Apply the recommended code fix
   - For IaC issues: Update the configuration file
3. Commit the change with a descriptive message including the CVE ID
4. Create a PR with:
   - Title: `fix: [Package] Upgrade [package-name] from [old-version] to [new-version] to resolve [CVE-ID]`
   - Description including: vulnerability details, severity, CVE link, what was changed, and the Snyk scan reference
5. **One PR per vulnerability — never group multiple vulnerabilities into a single PR**
6. **Always reset to a clean master/main state between each PR** — stash or discard changes before checking out a new branch

IMPORTANT: Always push PRs to the forked repo (e.g., `COG-GTM/<REPO_NAME>`), never to the source/upstream repo.

<verification>
- Exactly 5 (or fewer if fewer than 5 have known fixes) PRs have been created
- Each PR targets the single most critical vulnerability by CVSS score
- Each PR has a clear title with CVE reference
- Each PR description includes vulnerability details and remediation explanation
- All PRs target the correct forked repository
- No PR groups multiple vulnerabilities together
</verification>
</phase>

## Specifications
- All scan paths must be absolute paths
- Use `unmanaged: true` for all SCA scans
- Maximum 5 fix PRs per scan run — prioritize by severity and CVSS score
- One PR per vulnerability — never group fixes
- PRs must target the forked repo (COG-GTM/<REPO_NAME>), not the upstream
- Present scan reports directly in the session interface, not just saved to filesystem

## Advice and Pointers
- Start with SCA scan as it typically finds the most vulnerabilities
- If `snyk_trust` hangs, the trust confirmation server may need an HTTP POST to its `/trust` endpoint
- For Java/Maven projects, ensure Maven is available for accurate dependency resolution
- Focus remediation on the top 5 vulnerabilities with known fixes — skip those without available patches
- When upgrading dependencies, check for breaking changes between major versions
- Always ensure a clean git working directory (git checkout master && git checkout -- .) before creating each new fix branch

## Forbidden Actions
- Do not skip authentication — `snyk_auth` must run first
- Do not create more than 5 fix PRs per run
- Do not group multiple vulnerabilities into a single PR
- Do not push PRs to upstream/source repositories — always use the forked repo
- Do not suppress or ignore vulnerabilities without documented justification
- Do not modify test files to make tests pass after dependency upgrades

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
