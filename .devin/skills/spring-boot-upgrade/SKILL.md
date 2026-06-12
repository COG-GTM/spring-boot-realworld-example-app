---
name: spring-boot-upgrade
description: "Converted from Devin playbook: Spring Boot Upgrade"
triggers:
  - user
  - model
---

# Spring Boot Upgrade

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Spring Boot Upgrade (playbook-a80d05485f5946eaa196e382aca897b2), macro `!spring_boot_upgrade`

## Procedure

## Overview

This playbook outlines a structured and safe process for upgrading Spring Boot applications across versions. The upgrade is performed incrementally, resolving deprecated features, aligning dependencies, and ensuring tests pass at each step.


## Upgrade Process

### 1. Prepare the Repository

* Clone the repository:

  ```bash
  git clone https://your_git_repo
  cd your_repo
  git checkout -b springboot-upgrade
  ```
* Confirm the current Spring Boot version from `build.gradle`, `pom.xml`, or `spring-boot-starter-parent`.

### 2. Determine Upgrade Path

* Refer to the [Spring Boot Release Notes](https://github.com/spring-projects/spring-boot/releases).
* List intermediate upgrade steps (e.g., `2.5 → 2.6 → 2.7 → 3.0`).
* Review the [Migration Guide](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#migration) for each target version.

### 3. Upgrade to Next Version (Repeat Steps 3–8 for Each Version Step)

#### 3.1 Update Spring Boot Version

* Modify version in `pom.xml` or `build.gradle`:

  * For Maven:

    ```xml
    <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>2.7.0</version>
    </parent>
    ```
  * For Gradle:

    ```groovy
    implementation 'org.springframework.boot:spring-boot-starter:2.7.0'
    ```

#### 3.2 Align Dependency Versions

* Upgrade dependent Spring libraries and starters.
* Check for compatibility issues with third-party libraries using:

  ```bash
  ./gradlew dependencies --warning-mode=all
  mvn dependency:tree
  ```

#### 3.3 Resolve Breaking Changes

* Use the official migration guide to resolve deprecated or removed APIs.
* Replace outdated config keys in `application.yml` or `application.properties`.

#### 3.4 Update Tooling

* Update the Spring Boot Maven/Gradle Plugin version.
* Align Java version (e.g., Spring Boot 3.x requires Java 17+).

#### 3.5 Execute Tests

* Run all unit, integration, and smoke tests:

  ```bash
  ./gradlew test
  mvn test
  ```

#### 3.6 Address Failing Tests

* Use logs and Spring deprecation notices to fix issues.
* Search the changelog for deprecated API usage or behavioral changes.

#### 3.7 Check for Vulnerabilities

* Run:

  ```bash
  ./gradlew dependencyCheckAnalyze
  mvn org.owasp:dependency-check-maven:check
  ```

#### 3.8 Commit & Document Changes

* Commit changes with version-specific message:

  ```bash
  git commit -am "Upgrade Spring Boot to 2.7.0"
  ```

## Final Steps After Last Version

### 4. Full Regression Test

* Run full regression suite and e2e tests.
* Confirm application compiles, starts, and runs correctly.

### 5. Create a PR for Review

* Ensure all changes are committed
* Create a PR with detailed overview on the changes made

## Advice & Pointers

✅ Always upgrade one major version at a time.
✅ Use Spring Boot Actuator to validate service health post-upgrade.
✅ Spring Boot 3+ requires migration to Jakarta namespaces — plan accordingly.
✅ Enable stricter compiler flags and warnings to detect deprecated APIs.

## Forbidden Actions

🚫 Do not skip intermediate Spring Boot versions.
🚫 Do not ignore failing tests — even if they seem unrelated.
🚫 Avoid unnecessary refactors during upgrade steps.
🚫 Never force push upgrade branches — preserve commit history for traceability.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
