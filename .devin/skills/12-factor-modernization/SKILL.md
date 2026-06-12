---
name: 12-factor-modernization
description: "Converted from Devin playbook: 12 Factor Modernization"
triggers:
  - user
  - model
---

# 12 Factor Modernization

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: 12 Factor Modernization (playbook-404c466ff4484fa2b819ea0e4dfaf95e), macro `!modernization`

## Procedure

**Objective**
Develop a modernization plan to transform a legacy application architecture into a modern, cloud-native system aligned with the 12-Factor App methodology.

---

### Research Procedure

1. **Define Scope and Audit the Legacy Architecture**

   * Identify the target system(s) for modernization.
   * Catalog technologies used: programming language, framework, databases, deployment pipeline, monitoring tools, etc.
   * Map current architecture (monolith, service-based, etc.) and deployment environments (on-premises, cloud, hybrid).
   * Document key constraints: data residency, compliance, business-critical SLAs.

2. **Review the 12-Factor App Principles**

   * Study the official 12factor.net guide and supporting materials:

     * [https://12factor.net/](https://12factor.net/)
     * Blog posts, whitepapers, and talks on applying these in real-world systems.
   * For each factor, gather examples of legacy anti-patterns vs modern implementations.

3. **Assess Gaps Between Current State and 12-Factor Model**

   * For each factor:

     * Evaluate the legacy system’s current compliance or deviation.
     * Document blockers (e.g., hardcoded configs, tight coupling, non-portable services).
     * Note any existing components that already align with best practices.

4. **Research Technology Options and Migration Paths**

   * For each identified gap, investigate tools or techniques to modernize:

     * Config: use environment variables, secrets managers (e.g., AWS Secrets Manager, Vault).
     * Dependencies: refactor toward dependency injection, language-native package managers.
     * Dev/prod parity: consider containerization with Docker, infrastructure as code.
     * Logs: evaluate centralized logging (e.g., ELK, Datadog, CloudWatch).
     * Statelessness: explore state externalization to services like Redis, S3.
   * Identify language/framework-native support for 12-Factor adherence (Spring Boot, Django, Node.js, etc.).

5. **Create a Modernization Roadmap**

   * Break modernization into clear phases:

     * Discovery
     * Foundational refactors (e.g., config handling, logging)
     * Modularization and service decomposition
     * Infrastructure and CI/CD upgrades
     * Observability improvements
   * Sequence steps to allow for incremental rollout and testing.

6. **Validate Migration Risks and Mitigation Plans**

   * For each modernization phase:

     * Identify potential risks (data loss, availability impact, developer ramp-up).
     * Propose mitigation strategies (shadow deployments, blue/green, feature flags).
   * Research and list examples of successful 12-factor migrations in similar stacks.

7. **Output: Deliverables from the Research Phase**

   * A system architecture gap analysis matrix (current vs 12-Factor).
   * A technology/tool selection list with trade-offs for each factor.
   * A phased migration plan with dependencies and milestones.
   * A risk register with mitigation strategies.
   * Reference examples or case studies.

---

**Advice & Pointers**

* Focus first on factors that bring the most operational ROI (e.g., logs, config, statelessness).
* Modernization does not need to happen all at once—use an incremental strategy.
* Use feature flags or canary rollouts to manage risk during deployment transitions.
* Prioritize automation wherever possible—CI/CD, infra-as-code, testing, monitoring.

---

**Forbidden Actions**

* Do not propose implementation changes in this playbook—this is a planning-only task.
* Do not skip analysis for any of the 12 factors—even if some seem irrelevant initially.
* Do not assume a complete rewrite—research should account for both refactor and replatform options.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
