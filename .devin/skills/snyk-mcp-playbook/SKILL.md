---
name: snyk-mcp-playbook
description: "Converted from Devin playbook: Snyk MCP playbook "
triggers:
  - user
  - model
---

# Snyk MCP playbook 

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Snyk MCP playbook  (playbook-e3b98cd624e54cdda2ce2ee0de591f79), macro `!snyk_mcp_scan`

## Procedure

## Overview

This playbook performs a comprehensive security audit using the **Snyk MCP (Model Context Protocol) integration**. It automatically executes all available Snyk security scanning tools and generates a structured report with actionable remediation plans. The Snyk MCP server provides security scanning tools: please use all for the security scan. Should any fail, due to Oauth, authentication, browser failure, writing to disk, etc -> Please use the CLI to ensure a thorough test scan is executed.

Core Scanning Tools:

snyk_code_scan - Static Application Security Testing (SAST) for code vulnerabilities

snyk_sca_scan - Software Composition Analysis for dependency vulnerabilities

snyk_iac_scan - Infrastructure as Code security scanning

snyk_container_scan - Container image vulnerability scanning

snyk_sbom_scan - SBOM (Software Bill of Materials) vulnerability analysis

## Output Files

After execution, this playbook should generate:

- Executive summary with metrics
- Critical findings with exact locations and Devin instructions
- Structured findings of High, Medium and Low Vulnerabilities
- Infrastructure findings (Dockerfiles, workflows, IaC status)
- Snyk MCP integration assessment
- Synthesized remediation plan with prioritized tasks

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
