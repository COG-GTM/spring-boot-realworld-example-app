---
name: net-upgrades
description: "Converted from Devin playbook: .NET Upgrades"
triggers:
  - user
  - model
---

# .NET Upgrades

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: .NET Upgrades (playbook-e35d1e179bb54c15819cf7ee877f9aff)

## Procedure

## 🔧 Prerequisites

1. **Ensure .NET 6/7/8 SDK is installed** on your local and CI environments.

   ```bash
   dotnet --list-sdks
   ```

2. **Clone the repository and create a dedicated upgrade branch:**

   ```bash
   git checkout -b upgrade/dotnet-to-<target-version>
   ```

3. **Ensure the application is building and tests are passing in .NET Core 3.1 before beginning.**

---

## 🚀 Upgrade Procedure

### Step 1: Audit and Prepare

* Confirm current version:
  Inspect `global.json`, `*.csproj`, `Dockerfile`, `.yml`, or `.ps1` for version references.
* Create a checklist for locations to update:

  * `global.json`
  * `TargetFramework` in `.csproj`
  * Docker base image (e.g., `mcr.microsoft.com/dotnet/aspnet`)
  * CI pipeline config (GitHub Actions, Azure DevOps, etc.)

---

### Step 2: Update Framework and SDK References

* Update `global.json` if present:

  ```json
  {
    "sdk": {
      "version": "8.0.100"
    }
  }
  ```

* Update all `.csproj` files:

  ```xml
  <TargetFramework>net8.0</TargetFramework>
  ```

* Update Dockerfiles and CI/CD config to use the correct version of the .NET SDK/runtime.

---

### Step 3: Review and Fix Breaking Changes

* Use the [.NET Upgrade Assistant](https://learn.microsoft.com/en-us/dotnet/core/upgrade/upgrade-assistant-overview) or manually follow:

  * [Breaking changes .NET 6](https://learn.microsoft.com/en-us/dotnet/core/compatibility/6.0)
  * [Breaking changes .NET 7](https://learn.microsoft.com/en-us/dotnet/core/compatibility/7.0)
  * [Breaking changes .NET 8](https://learn.microsoft.com/en-us/dotnet/core/compatibility/8.0)

Examples to watch for:

* Changes in nullable reference types behavior.
* Removed or renamed APIs.
* Startup.cs → Program.cs migration (for ASP.NET Core apps).
* Blazor/WebAssembly support updates if applicable.

---

### Step 4: Upgrade Dependencies

* Run:

  ```bash
  dotnet list package --outdated
  ```
* Upgrade NuGet packages to versions compatible with your target framework.
* Check release notes and GitHub issues for any breaking changes in core packages (e.g., EntityFrameworkCore, Newtonsoft.Json).

---

### Step 5: Build and Run Tests

* Build the project:

  ```bash
  dotnet build
  ```
* Run all tests (unit, integration, e2e):

  ```bash
  dotnet test
  ```
* Address any compiler errors, test failures, or warnings.

---

### Step 6: Validate Application Behavior

* Run the app locally and verify:

  * Startup
  * Endpoints
  * Key features (auth, database access, external APIs)

* Perform manual or automated regression testing.

---

### Step 7: Security and Performance Checks

* Run:

  ```bash
  dotnet list package --vulnerable
  dotnet build --no-incremental
  ```

* Resolve vulnerabilities or outdated packages.

* If applicable, rerun SAST (e.g., Veracode, Checkmarx) or QA tools (e.g., SonarQube) to revalidate.

---

### Step 8: Update CI/CD Pipelines

* Update `.yml`, `.yaml`, or pipeline templates to use correct .NET SDK version.
* Rebuild containers or runners if pinned to previous versions.
* Ensure clean builds and test results.

---

### Step 9: Commit and Open Pull Request

* Commit with clear description:

  ```
  chore: upgrade project to .NET 8.0
  ```
* Push your branch and open a PR summarizing:

  * Updated SDK and TFM
  * Key breaking changes resolved
  * Any dependency or config changes
  * Test coverage status

---

## 🧠 Advice & Pointers

* ✅ Upgrade dependencies **after** the TFM change to avoid mismatch.
* ✅ Use dotnet CLI instead of Visual Studio where possible to maintain environment parity.
* ✅ Consider enabling nullable reference types and implicit usings for better long-term maintainability.
* ✅ Split PRs by layers (API, data, tests) if the upgrade introduces large changes.

---

## 🚫 Forbidden Actions

* 🚫 Do not upgrade framework version without running full test suite.
* 🚫 Do not ignore failing CI steps or warnings in the console.
* 🚫 Do not push directly to main — always use PRs for review and rollback capability.
* 🚫 Do not mix unrelated refactors or cleanup with upgrade commits.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
