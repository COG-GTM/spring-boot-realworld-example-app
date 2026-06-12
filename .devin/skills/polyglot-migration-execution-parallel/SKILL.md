---
name: polyglot-migration-execution-parallel
description: "Converted from Devin playbook: Polyglot Migration Execution (Parallel)"
triggers:
  - user
  - model
---

# Polyglot Migration Execution (Parallel)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Polyglot Migration Execution (Parallel) (playbook-2977d45fb3db4a87ae0d8c8edee21c42)

## Procedure

# Polyglot Migration Execution (Parallel)

## Overview
Execute the modernization of all 12 services in COG-GTM/piggymetrics by orchestrating parallel child Devin sessions. Each child session migrates one service independently.

## What's Needed From User
- Repository: COG-GTM/piggymetrics
- Migration plan from the scoping playbook (optional — defaults below are used if not provided)

<phase name="Pre-Flight Checks" id="1">
## Pre-Flight Checks

1. Clone `COG-GTM/piggymetrics` and verify all 12 services are present
2. Confirm the current tech stack versions by checking pom.xml and .csproj files
3. Verify DeepWiki index is available at https://deepwiki.com/COG-GTM/piggymetrics
4. Create a tracking branch: `git checkout -b devin/migration-orchestrator`

<verification>
- Repository cloned and all 12 services confirmed
- Current versions verified (Java 8, Spring Boot 1.5.x, .NET Core 2.1)
- DeepWiki index accessible
</verification>
</phase>

<phase name="Infrastructure Migration" id="2">
## Phase 2: Infrastructure Services Migration

Migrate infrastructure services FIRST since all other services depend on them. Create child sessions for:

### Child Session 1: Config Server
Prompt: "Migrate the config service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 to Spring Boot 3.2/Java 21. Update pom.xml dependencies, migrate javax to jakarta namespace, update Spring Cloud Config dependencies to 2024.x. Update Dockerfile base image. Ensure all shared config files in resources/shared/ are preserved. Create a PR with your changes."

### Child Session 2: Service Registry (Eureka)
Prompt: "Migrate the registry service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 to Spring Boot 3.2/Java 21. Update Eureka Server dependencies to Spring Cloud 2024.x, migrate javax to jakarta, update pom.xml and Dockerfile. Create a PR with your changes."

### Child Session 3: Auth Service
Prompt: "Migrate the auth-service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 to Spring Boot 3.2/Java 21. Update Spring Security OAuth2 (deprecated) to Spring Authorization Server. Migrate javax to jakarta. Update MongoDB driver. Update pom.xml and Dockerfile. Create a PR with your changes."

Use `devin_session_create` to launch all 3 child sessions in parallel, then use `devin_session_gather` to wait for completion.

<verification>
- 3 child sessions created for Config, Registry, and Auth services
- All 3 sessions completed successfully
- PRs created for each service migration
</verification>
</phase>

<phase name="Core Java Services Migration" id="3">
## Phase 3: Core Java Services Migration (Parallel)

Once infrastructure is migrated, migrate the core business services in parallel:

### Child Session 4: Account Service
Prompt: "Migrate account-service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 to Spring Boot 3.2/Java 21. Replace Hystrix with Resilience4j, replace Ribbon with Spring Cloud LoadBalancer, migrate Feign clients to Spring Cloud 2024.x, migrate javax to jakarta, update MongoDB driver, migrate JUnit 4 tests to JUnit 5. Update pom.xml and Dockerfile. Create a PR."

### Child Session 5: Statistics Service
Prompt: "Migrate statistics-service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 to Spring Boot 3.2/Java 21. Replace Hystrix with Resilience4j, replace Ribbon with Spring Cloud LoadBalancer, migrate Feign clients, migrate javax to jakarta, update MongoDB driver, migrate JUnit 4 to JUnit 5. Update pom.xml and Dockerfile. Create a PR."

### Child Session 6: Notification Service
Prompt: "Migrate notification-service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 to Spring Boot 3.2/Java 21. Replace Hystrix with Resilience4j, replace Ribbon with Spring Cloud LoadBalancer, migrate Feign clients, migrate javax to jakarta, update MongoDB driver, migrate JUnit 4 to JUnit 5. Update pom.xml and Dockerfile. Create a PR."

### Child Session 7: Monitoring Service
Prompt: "Migrate the monitoring service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 to Spring Boot 3.2/Java 21. Replace Hystrix Dashboard with Spring Boot Actuator + Micrometer. Remove Turbine dependency. Update pom.xml and Dockerfile. Create a PR."

### Child Session 8: Turbine Stream Service
Prompt: "Migrate turbine-stream-service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 to Spring Boot 3.2/Java 21. Replace Netflix Turbine with Micrometer metrics aggregation. Update Spring Cloud Stream and RabbitMQ dependencies. Update pom.xml and Dockerfile. Create a PR."

### Child Session 9: API Gateway
Prompt: "Migrate the gateway service in COG-GTM/piggymetrics from Spring Boot 1.5.9/Java 8 with Netflix Zuul to Spring Boot 3.2/Java 21 with Spring Cloud Gateway. Replace all Zuul routing configuration with Spring Cloud Gateway routes. Migrate static content serving. Update pom.xml and Dockerfile. Create a PR."

Launch all 6 child sessions in parallel with `devin_session_create`, then `devin_session_gather` to wait.

<verification>
- 6 child sessions created for core Java services
- All 6 sessions completed successfully
- PRs created for each service migration
</verification>
</phase>

<phase name=".NET Services Migration" id="4">
## Phase 4: .NET Services Migration (Parallel)

Migrate all 3 .NET services simultaneously:

### Child Session 10: Fraud Detection Service
Prompt: "Migrate fraud-detection-service in COG-GTM/piggymetrics from .NET Core 2.1 to .NET 8. Update the .csproj TargetFramework to net8.0. Replace Newtonsoft.Json with System.Text.Json. Replace RestSharp with HttpClientFactory/IHttpClientFactory. Update Swashbuckle to 6.x. Update Polly to 8.x. Update MongoDB.Driver to latest. Replace microsoft/dotnet:2.1 Docker images with mcr.microsoft.com/dotnet/aspnet:8.0 and sdk:8.0. Migrate Program.cs to minimal hosting model. Create a PR."

### Child Session 11: Compliance Service
Prompt: "Migrate compliance-service in COG-GTM/piggymetrics from .NET Core 2.1 to .NET 8. Update .csproj TargetFramework to net8.0. Replace Newtonsoft.Json with System.Text.Json. Update Swashbuckle to 6.x. Update NLog to latest. Update MongoDB.Driver to latest. Replace Docker images with mcr.microsoft.com/dotnet/aspnet:8.0. Migrate to minimal hosting model. Create a PR."

### Child Session 12: Currency Exchange Service
Prompt: "Migrate currency-exchange-service in COG-GTM/piggymetrics from .NET Core 2.1 to .NET 8. Update .csproj TargetFramework to net8.0. Replace Newtonsoft.Json with System.Text.Json. Update Swashbuckle to 6.x. Update Microsoft.Extensions.Caching.Memory to latest. Replace Docker images with mcr.microsoft.com/dotnet/aspnet:8.0. Migrate to minimal hosting model. Create a PR."

Launch all 3 child sessions with `devin_session_create`, then `devin_session_gather`.

<verification>
- 3 child sessions created for .NET services
- All 3 sessions completed successfully
- PRs created for each .NET service migration
</verification>
</phase>

<phase name="Integration & Summary" id="5">
## Phase 5: Integration Verification & Summary

1. Collect all PRs from child sessions
2. Update docker-compose.yml with any new port or image changes
3. Verify no conflicting dependency versions across services
4. Produce a migration summary report:
   - Total services migrated: 12
   - Java services: 9 (Java 8→21, Spring Boot 1.5→3.2)
   - .NET services: 3 (.NET Core 2.1→.NET 8)
   - PRs created with links
   - Any issues encountered
5. Send the summary to the user

<verification>
- All 12 service migration PRs collected and listed
- docker-compose.yml updated if needed
- Migration summary report produced and sent to user
</verification>
</phase>

## Specifications
- All child sessions target repo: COG-GTM/piggymetrics
- Each child session creates its own feature branch and PR
- Use existing playbooks where applicable: `@playbook:playbook-f6e200aaca1841ff9111ead79fba3c90` for Spring Boot upgrades, `@playbook:playbook-e35d1e179bb54c15819cf7ee877f9aff` for .NET upgrades

## Advice and Pointers
- Infrastructure services (Config, Registry) MUST complete before launching core service migrations
- .NET services can migrate in parallel with Java core services since they have no cross-stack build dependencies
- Only create a TODO list for the current phase
- Use `devin_session_gather` with appropriate timeouts (600s per phase)

## Forbidden Actions
- Do not migrate services sequentially — use parallel child sessions
- Do not push directly to master
- Do not skip any of the 12 services

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
