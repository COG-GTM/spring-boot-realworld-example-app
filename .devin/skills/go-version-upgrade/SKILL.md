---
name: go-version-upgrade
description: "Converted from Devin playbook: Go Version Upgrade"
triggers:
  - user
  - model
---

# Go Version Upgrade

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Go Version Upgrade (playbook-6929d9f66a24445fa7b52567445cdf59)

## Procedure

### Overview

This playbook outlines a step-by-step approach for upgrading a Go application from any older version to a newer one (e.g., Go 1.17 → 1.21). It ensures compatibility, addresses deprecations, and maintains build/test stability across development and CI environments.

### Upgrade Procedure

#### 1. Audit Current Go Version and Dependencies

* Identify the current Go version in use (`go version`, `go.mod`, CI config).
* Check for hardcoded Go versions in:

  * `go.mod`
  * Dockerfiles
  * CI configs (e.g., `.github/workflows`, `.gitlab-ci.yml`)
  * Build scripts

#### 2. Review Go Release Notes

* Visit [golang.org/doc/devel/release](https://golang.org/doc/devel/release.html) and read the notes for all intermediate versions.
* Document key changes that may affect your project (e.g., syntax, stdlib, compiler behavior).

#### 3. Update Go Version Locally

* Install the desired Go version using a version manager (`asdf`, `gvm`, `goenv`, or manual download).
* Switch to the new version and verify:

  ```sh
  go version
  ```

#### 4. Update go.mod

* Edit `go.mod`:

  ```go
  go 1.21
  ```
* Tidy and vendor the dependencies:

  ```sh
  go mod tidy
  go mod vendor  # if vendoring is used
  ```

#### 5. Run Full Test Suite

* Execute all tests:

  ```sh
  go test ./...
  ```
* Check for:

  * Deprecated API usage
  * Type changes
  * Failing tests

#### 6. Validate with `go vet`, `golint`, and `staticcheck`

* Run additional tooling to catch semantic issues:

  ```sh
  go vet ./...
  staticcheck ./...
  ```

#### 7. Update CI/CD Configuration

* Update CI pipelines to use the new Go version.

  * For GitHub Actions:

    ```yaml
    - name: Setup Go
      uses: actions/setup-go@v4
      with:
        go-version: '1.21'
    ```
  * For Docker, update the base image:

    ```Dockerfile
    FROM golang:1.21
    ```

#### 8. Build and Deploy to Staging

* Confirm successful build using the new Go version:

  ```sh
  go build ./...
  ```
* Deploy to staging and validate runtime behavior (logs, latency, memory, crashes).

#### 9. Monitor in Production

* After a successful staging run, deploy to production.
* Monitor for regressions via observability tools (e.g., Prometheus, Datadog, logs).
* Roll back if stability issues are found.

### Advice & Best Practices

* Upgrade one minor Go version at a time if jumping multiple versions.
* If your project uses cgo, validate compatibility with updated system libraries.
* Always commit `go.mod` and `go.sum` changes separately for clarity.
* Enable and monitor `GOEXPERIMENT` and `GODEBUG` flags for experimental features or debugging.

### Forbidden Actions

🚫 Do not mix Go versions in CI, Docker, or developer machines.
🚫 Avoid skipping test failures—fix root causes.
🚫 Do not upgrade without checking upstream libraries for version compatibility.
🚫 Never commit temporary or debug code used for upgrade testing.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
