---
name: bbva-migration-scoping
description: "Converted from Devin playbook: BBVA Migration Scoping"
triggers:
  - user
  - model
---

# BBVA Migration Scoping

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: BBVA Migration Scoping (playbook-72d7e5710bc14c3e9e6f08852f369de0)

## Procedure

# Polyglot Migration Scoping

## Overview
Analyze the COG-GTM/piggymetrics polyglot distributed system (12 services: 9 Java + 3 .NET) and produce a comprehensive migration plan with dependency mapping, task breakdown, and parallel execution strategy. Once the plan is approved by the user, automatically kick off the migration by creating parallel child sessions with the execution playbook.

## What's Needed From User
- Repository: COG-GTM/piggymetrics (default)
- Any custom migration targets or constraints (optional)

<phase name="Service Discovery" id="1">
## Service Discovery & Inventory

1. Clone `COG-GTM/piggymetrics`
2. Identify all services by scanning for `pom.xml` (Java) and `*.csproj` (.NET) files
3. For each service document: name, purpose, port, language, framework version, database, Docker config
4. Create a service inventory table

<verification>
- All 12 services identified and cataloged
- Java 8 and Spring Boot 1.5.x versions confirmed from pom.xml files
- .NET Core 2.1 confirmed from .csproj files
- Service inventory table is complete
</verification>
</phase>

<phase name="Dependency Mapping" id="2">
## Dependency Mapping

1. Map REST API dependencies: Java Feign clients (`@FeignClient`), .NET RestSharp calls, Zuul routes
2. Map infrastructure dependencies: Eureka registration, Spring Cloud Config, RabbitMQ, Hystrix
3. Map database dependencies: dedicated MongoDB instances per service
4. Produce a Mermaid dependency graph showing service-to-service calls, infrastructure connections, database ownership

<verification>
- All inter-service REST API calls documented
- Infrastructure dependencies mapped for each service
- Database ownership identified per service
- Mermaid dependency graph created
</verification>
</phase>

<phase name="Migration Task Breakdown" id="3">
## Migration Task Breakdown

For each of the 12 services, produce a detailed migration task list:

**Java Services (9):** Java 8→21, Spring Boot 1.5→3.x, javax→jakarta, Zuul→Gateway, Hystrix→Resilience4j, Ribbon→LoadBalancer, MongoDB driver updates, JUnit 4→5

**.NET Services (3):** .NET Core 2.1→.NET 8, Newtonsoft→System.Text.Json, RestSharp→HttpClientFactory, Swashbuckle 3→6, Polly 6→8, Docker image updates

<verification>
- Every service has a specific task list with before/after versions
- Breaking changes identified (not just version bumps)
- Infrastructure services have special migration notes
</verification>
</phase>

<phase name="Migration Plan" id="4">
## Migration Plan Document

Produce a markdown migration plan with:
1. **Executive Summary** — scope, complexity per service, risk assessment
2. **Service Inventory Table** — all 12 services with current and target versions
3. **Architecture Diagram** — Mermaid diagram of current service topology
4. **Migration Order** — Phase 1: Infrastructure → Phase 2: Auth → Phase 3: Leaf services (parallel) → Phase 4: Core services → Phase 5: Gateway
5. **Per-Service Task Breakdown** — detailed checklist per service
6. **Risk Matrix** — breaking changes, rollback strategy
7. **Parallel Execution Plan** — which services can migrate simultaneously

Present the complete plan directly in the session.

After presenting the plan, ask the user: **"Migration plan is ready. Shall I kick off the parallel migration of all 12 services now?"**

Wait for user approval before proceeding to the next phase.

<verification>
- Migration plan contains all 7 sections
- Each service has a complexity rating
- Migration order accounts for dependency constraints
- Parallel execution opportunities identified
- User has been asked for approval to proceed with execution
</verification>
</phase>

<phase name="Execute Migration" id="5">
## Execute Migration via Parallel Child Sessions

**IMPORTANT: Only proceed to this phase after user approves execution.**

Using `devin_session_create`, launch child sessions in waves based on the dependency order from the migration plan. Each child session receives the execution playbook AND the specific context gathered during scoping.

### Wave 1: Infrastructure (must complete first)
Create 3 child sessions using `devin_session_create` with `playbook_id: "playbook-2977d45fb3db4a87ae0d8c8edee21c42"` and `repos: ["COG-GTM/piggymetrics"]`:

1. **Config Server**: Include in the prompt the exact current Spring Boot version, Spring Cloud version, and any shared config files discovered in Phase 1-2. Prompt should say: "You are migrating the config service. Current: Spring Boot {version}, Java 8, Spring Cloud {version}. Target: Spring Boot 3.2, Java 21, Spring Cloud 2024.x. Dependencies discovered: {list services that depend on config}. Update pom.xml, migrate javax→jakarta, update Dockerfile. Create a PR."

2. **Service Registry (Eureka)**: Include discovered Eureka configuration and list of registered services. Prompt: "You are migrating the registry service. Current: Spring Boot {version}, Eureka Server {version}. Target: Spring Boot 3.2, Java 21, Spring Cloud 2024.x. Services that register: {list from Phase 2}. Update pom.xml, migrate javax→jakarta, update Dockerfile. Create a PR."

3. **Auth Service**: Include OAuth2 configuration details and list of services that authenticate. Prompt: "You are migrating auth-service. Current: Spring Boot {version}, Spring Security OAuth2 {version}. Target: Spring Boot 3.2, Java 21, Spring Authorization Server. Services that depend on auth: {list from Phase 2}. Update pom.xml, migrate OAuth2→Spring Authorization Server, javax→jakarta, update Dockerfile. Create a PR."

Use `devin_session_gather` to wait for Wave 1 to complete before proceeding.

### Wave 2: All remaining services (parallel)
Once Wave 1 completes, create 9 child sessions simultaneously using `devin_session_create`:

4. **Account Service**: "Migrate account-service. Current: {versions from Phase 1}. Depends on: {deps from Phase 2}. Replace Hystrix→Resilience4j, Ribbon→LoadBalancer, update Feign clients, javax→jakarta, MongoDB driver, JUnit 4→5. Create a PR."

5. **Statistics Service**: "Migrate statistics-service. Current: {versions}. Depends on: {deps}. Replace Hystrix→Resilience4j, Ribbon→LoadBalancer, update Feign, javax→jakarta, MongoDB, JUnit 4→5. Create a PR."

6. **Notification Service**: "Migrate notification-service. Current: {versions}. Depends on: {deps}. Replace Hystrix→Resilience4j, Ribbon→LoadBalancer, update Feign, javax→jakarta, MongoDB, JUnit 4→5. Create a PR."

7. **Monitoring Service**: "Migrate monitoring service. Current: {versions}. Replace Hystrix Dashboard→Spring Boot Actuator + Micrometer. Remove Turbine dependency. Create a PR."

8. **Turbine Stream Service**: "Migrate turbine-stream-service. Current: {versions}. Replace Netflix Turbine→Micrometer metrics aggregation. Update Spring Cloud Stream, RabbitMQ deps. Create a PR."

9. **API Gateway**: "Migrate gateway service. Current: {versions}. Zuul routes: {routes from Phase 2}. Replace Zuul→Spring Cloud Gateway. Migrate all routing config. Preserve static content serving. Create a PR."

10. **Fraud Detection (.NET)**: "Migrate fraud-detection-service from .NET Core 2.1→.NET 8. Current deps: {from Phase 1}. Calls: {from Phase 2}. Update TargetFramework→net8.0, Newtonsoft→System.Text.Json, RestSharp→HttpClientFactory, Swashbuckle 3→6, Polly 6→8, MongoDB.Driver→latest, Docker images→mcr.microsoft.com/dotnet/aspnet:8.0. Migrate to minimal hosting. Create a PR."

11. **Compliance Service (.NET)**: "Migrate compliance-service from .NET Core 2.1→.NET 8. Current deps: {from Phase 1}. Update TargetFramework→net8.0, Newtonsoft→System.Text.Json, Swashbuckle 3→6, NLog→latest, MongoDB.Driver→latest, Docker→mcr.microsoft.com/dotnet/aspnet:8.0. Migrate to minimal hosting. Create a PR."

12. **Currency Exchange (.NET)**: "Migrate currency-exchange-service from .NET Core 2.1→.NET 8. Current deps: {from Phase 1}. Update TargetFramework→net8.0, Newtonsoft→System.Text.Json, Swashbuckle 3→6, Caching→latest, Docker→mcr.microsoft.com/dotnet/aspnet:8.0. Migrate to minimal hosting. Create a PR."

Use `devin_session_gather` to wait for all Wave 2 sessions.

### Summary Report
After all child sessions complete:
1. Collect all PR links from child sessions using `devin_session_interact` with `action: "get"`
2. Report to the user:
   - Total services migrated: 12 (9 Java + 3 .NET)
   - List each PR with service name and link
   - Any failures or issues encountered
   - Recommended next steps (merge order, integration testing)

<verification>
- Wave 1 (3 infrastructure sessions) created and completed
- Wave 2 (9 remaining sessions) created and completed
- All 12 child sessions produced PRs
- Summary report with all PR links sent to user
</verification>
</phase>

## Specifications
- Use DeepWiki (https://deepwiki.com/COG-GTM/piggymetrics) as reference if available
- All version numbers verified from actual source files
- Migration targets: Java 21, Spring Boot 3.2+, .NET 8, Spring Cloud 2024.x
- Replace `{versions}`, `{deps}`, `{routes}` placeholders in child session prompts with actual data discovered in Phases 1-4

## Advice and Pointers
- The key value of this playbook is that scoping context flows into execution — each child session gets the exact versions, dependencies, and config discovered during analysis
- Only create a TODO list for the current phase
- Use `devin_session_gather` with timeout_seconds=600 for each wave

## Forbidden Actions
- Do not launch Wave 2 before Wave 1 completes
- Do not skip any of the 12 services
- Do not proceed to Phase 5 without explicit user approval
- Do not use placeholder versions — always use actual discovered values

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
