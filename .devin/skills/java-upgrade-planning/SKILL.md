---
name: java-upgrade-planning
description: "Converted from Devin playbook: Java Upgrade Planning"
triggers:
  - user
  - model
---

# Java Upgrade Planning

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Java Upgrade Planning (playbook-e54c6d87faa64c0e859ed1648a03a754), macro `!java_upgrade_planning`

## Procedure

# Java Upgrade Planning Playbook

## Objective

Analyze the current state of a Java project and generate a structured upgrade plan. This plan should identify technical changes, risk areas, and actionable steps for incrementally upgrading the Java version and associated ecosystem dependencies.

> This playbook **does not perform the upgrade**. It generates a plan that will guide future Devin sessions.

---

## Procedure

### 1. Audit Current Environment

* **Inspect Java Version:**

  * Check source compatibility in `pom.xml`, `build.gradle`, or `.java-version`
  * Confirm JDK version used in CI/CD and Dockerfiles
* **Identify Build Tool:**

  * Maven, Gradle, Bazel, etc.
* **Log Output:**

  * Document detected Java version
  * Note build tool version and format (e.g., Maven 3.8.7)

### 2. Inventory the Codebase

* **Identify Project Type:**

  * Standalone Java, Spring Boot, Jakarta EE, Android, etc.
* **Detect Modules:**

  * List all submodules (multi-module Maven or Gradle builds)
* **Dependency Snapshot:**

  * Extract all dependencies and versions (e.g., `mvn dependency:tree`, `gradle dependencies`)
* **Third-Party Libraries:**

  * Flag outdated libraries or those known to have Java compatibility constraints

### 3. Determine Upgrade Target

* Prompt for or recommend target Java version (e.g., 17, 21, LTS)

* For the chosen version:

  * Retrieve official JDK migration guide (e.g., JDK 8 → 17: [https://docs.oracle.com/en/java/javase/](https://docs.oracle.com/en/java/javase/))
  * Identify major language and runtime changes
  * Link to key resources such as:

    * [JDK Migration Guides](https://docs.oracle.com/en/java/javase/migrate/)
    * [JEP Index](https://openjdk.org/jeps/0)

### 4. Compatibility Research

* **For each dependency**, determine:

  * Minimum Java version required
  * Whether an upgrade or replacement is needed
  * Note deprecated APIs or removed modules used in your codebase (e.g., `javax`, Nashorn)
* **Create a compatibility matrix**:

  ```
  | Component            | Current Version | Java 17 Support | Action Required |
  |----------------------|------------------|------------------|------------------|
  | Spring Boot          | 2.1.3            | ❌               | Upgrade to 2.6+  |
  | Hibernate            | 5.2.11           | ✅               | None             |
  | javax.xml.bind       | Java 8 only      | ❌               | Replace with Jakarta XML Binding |
  ```

### 5. Risk Assessment

* Identify areas most likely to break:

  * Reflection usage
  * Deprecated or removed APIs (e.g., `SecurityManager`, `sun.misc`)
  * Libraries that rely on internal JDK classes
* Highlight files/packages where usage is detected

### 6. Define Upgrade Milestones

Create a plan divided into **tasks or sprints**:

#### Sprint 1: Infrastructure & CI Prep

* Add support for target Java version in CI workflows
* Install the JDK locally and configure build tools to support it
* Ensure dependency resolution is not broken under the new Java version (build-only test)

#### Sprint 2: Code Compatibility Analysis

* Use `jdeps` or `japicmp` to analyze usage of deprecated/removed APIs
* Generate a list of required code changes

#### Sprint 3: Dependency Upgrades

* Upgrade libraries to versions compatible with the new JDK
* Replace deprecated ones if no compatible version exists

#### Sprint 4: Dual Build Support (optional)

* If needed, support both old and new JDKs temporarily (e.g., via toolchains or CI matrix)

#### Sprint 5: Full Migration & Tests

* Configure the project to compile and run with the new JDK
* Fix compile-time and runtime issues
* Run full test suite and address failures

#### Sprint 6: Post-Upgrade Cleanup

* Remove backward compatibility shims
* Modernize syntax (e.g., switch expressions, records)
* Update documentation

### 7. Output Final Upgrade Plan

The result of this playbook should be a structured plan (e.g., markdown or task list) containing:

* ✅ Java upgrade target
* ✅ Dependency upgrade requirements
* ✅ Known blockers and risks
* ✅ Suggested sprint/task breakdown
* ✅ Linked documentation for migration references
* ✅ Summary of toolchain impact (CI, Docker, etc.)

---

## Advice & Pointers

* Use tools like [Revapi](https://revapi.org/) or [JDeps](https://docs.oracle.com/en/java/javase/tools/jdeps.html) for compatibility inspection
* For Spring projects, consult [Spring Compatibility Matrix](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Versions)
* For Jakarta EE, use Eclipse Transformer if migrating from `javax.*` to `jakarta.*`
* Track breaking changes across versions using the OpenJDK [JEP Index](https://openjdk.org/jeps/0)

---

## Forbidden Actions

* 🚫 Do not attempt to make code changes during this planning step
* 🚫 Do not run refactoring scripts or formatters yet
* 🚫 Do not remove any deprecated libraries without a compatibility check
* 🚫 Do not skip CI/Docker review—Java upgrades often break infrastructure first

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
