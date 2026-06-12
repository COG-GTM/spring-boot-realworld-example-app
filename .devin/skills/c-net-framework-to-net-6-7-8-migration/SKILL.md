---
name: c-net-framework-to-net-6-7-8-migration
description: "Converted from Devin playbook: C# .NET Framework to .NET 6/7/8 Migration"
triggers:
  - user
  - model
---

# C# .NET Framework to .NET 6/7/8 Migration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: C# .NET Framework to .NET 6/7/8 Migration (playbook-6d493e26f706461ea2f7342a709ca91e), macro `!netcore`

## Procedure

## Overview

This playbook outlines a safe and systematic procedure for upgrading .NET Framework projects (e.g., 4.6, 4.7.2, etc.) to modern .NET (6, 7, or 8). It ensures application stability, test coverage, and compatibility throughout the process. The upgrade can be performed incrementally, especially for large solutions.

## Migration Process

### 1. **Preparation**

* **Clone the repository** and create a dedicated feature branch for the migration.
* **Audit the solution**:

  * Identify all .csproj files and projects in the solution.
  * Note third-party dependencies and NuGet packages.
  * Determine project types: web app, API, class library, WPF/WinForms, etc.
* **Confirm CI/CD environment** (Azure DevOps, GitHub Actions, etc.) and infrastructure dependencies (IIS, Windows services, etc.).

### 2. **Check Compatibility**

* Run the [.NET Upgrade Assistant](https://learn.microsoft.com/en-us/dotnet/upgrade-assistant/overview/) to evaluate project readiness:

  ```bash
  dotnet tool install -g upgrade-assistant
  upgrade-assistant analyze MyApp.sln
  ```
* Review the generated report for incompatible packages, project types, or deprecated APIs.

### 3. **Upgrade the Project Files**

* Use the upgrade assistant:

  ```bash
  upgrade-assistant upgrade MyApp.sln
  ```
* If upgrading manually:

  * Convert `.csproj` files to SDK-style format.
  * Replace `packages.config` with `PackageReference`.
  * Remove obsolete configurations from `.csproj` (e.g., `<AssemblyBinding>`, `<TargetFrameworkVersion>`).
  * Set `<TargetFramework>` to `net6.0`, `net7.0`, or `net8.0`.

### 4. **Update Dependencies**

* Upgrade NuGet packages to versions compatible with your target .NET version.
* Use:

  ```bash
  dotnet list package --outdated
  ```
* Check each package's compatibility on NuGet or via their GitHub repos.
* Remove or replace unsupported libraries.

### 5. **Refactor Code for Compatibility**

* Replace deprecated APIs:

  * e.g., `HttpContext.Current` → dependency injection
  * `ConfigurationManager.AppSettings` → `IConfiguration`
* Adjust for removed features:

  * Windows Communication Foundation (WCF), Web Forms, and other legacy tech are not supported.
  * Use alternatives like gRPC, SignalR, or ASP.NET Core MVC.

### 6. **Update Configuration**

* Migrate `web.config` settings to `appsettings.json` and `Program.cs`/`Startup.cs`.
* For ASP.NET MVC/Web API apps:

  * Convert to ASP.NET Core
  * Replace `Global.asax` with `Program.cs` and `Startup.cs`

### 7. **Rebuild and Resolve Errors**

* Rebuild each project and fix compilation issues.
* Use analyzers like [Roslynator](https://github.com/JosefPihrt/Roslynator) or [dotnet format](https://learn.microsoft.com/en-us/dotnet/core/tools/dotnet-format) to clean up code.

### 8. **Run Tests and Add Coverage**

* Ensure all unit and integration tests pass.
* If you lack tests, add basic coverage before proceeding.
* Validate functionality manually if automated tests are not available.

### 9. **Validate in CI/CD and Staging**

* Update build pipelines to use `dotnet build` and the correct SDK version.
* Replace MSBuild or legacy tasks with .NET CLI equivalents.
* Deploy to a staging environment.
* Monitor for regressions and broken dependencies.

### 10. **Finalize and Submit PR**

* Run a final test pass.
* Confirm app compiles, runs, and behaves as expected in the target runtime.
* Open a pull request with:

  * Summary of upgrade steps
  * Framework version(s) changed
  * Known trade-offs (e.g., dropped libraries, replaced components)

## Advice & Pointers

* Prefer `net8.0` if no blockers—it's the latest LTS.
* Use `dotnet monitor`, logging, and telemetry early to catch runtime issues.
* Watch for implicit behavior changes (e.g., encoding defaults, Kestrel configs).
* If needed, containerize upgraded apps with ASP.NET base images.

## Forbidden Actions

🚫 Do not mix legacy .NET Framework and .NET Core/6+ projects in the same solution unless via interop boundaries.
🚫 Do not upgrade without version control (commit frequently).
🚫 Do not ignore test failures—many .NET behavior changes are subtle but breaking.
🚫 Do not retain unused config files or obsolete project references.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
