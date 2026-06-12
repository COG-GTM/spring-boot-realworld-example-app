---
name: upgrade-java-application-from-java-9-and-below-to-java-11
description: "Converted from Devin playbook: Upgrade Java Application from Java 9 and Below to Java 11"
triggers:
  - user
  - model
---

# Upgrade Java Application from Java 9 and Below to Java 11

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Upgrade Java Application from Java 9 and Below to Java 11 (playbook-9a5af3fe5a23418db91bb8933201846a), macro `!upgrade_java11`

## Procedure

Playbook: Upgrade Java Application from Java 9 and Below to Java 11

## Overview

Upgrade a Java application running on Java 9 or earlier (Java 6, 7, 8, or 9) to Java 11. This covers build configuration, dependency upgrades, handling of removed Java EE modules (JAXB, JAX-WS, CORBA), test framework migration (PowerMock to Mockito), Spring/Hibernate configuration updates, and source code fixes for removed APIs. Applies to both Spring Boot and non-Spring-Boot applications using Maven or Gradle.

## What's Needed From User

- **Repository URL** or access to the codebase
- **Current Java version** (e.g., Java 7, 8, or 9) — or Devin will detect it
- **Build tool**: Maven or Gradle — or Devin will detect it
- **Whether tests must pass**: Confirm if all existing tests must pass, or if known failures are acceptable
- **Database access** (if applicable): If the project uses Liquibase/Flyway, confirm whether to skip DB plugins during build (e.g., `-Dliquibase.should.run=false`) or provide a database connection

## Procedure

1. **Assess the project.** Read `pom.xml` or `build.gradle` to determine the current Java version, all dependency versions, and build plugins. Note any version ranges (e.g., `[4.3.18,)`) that need pinning. Search the codebase for usage of removed APIs: `javax.xml.bind`, `javax.xml.ws`, `sun.misc.*`, and `sun.reflect.*`. Check JVM launch scripts, Dockerfiles, and CI configs for obsolete flags (`-XX:MaxPermSize`, `-Xincgc`, `-XX:+PrintGCDetails`).

2. **Update Java version and compiler configuration.**
   - Maven: Set `<java.version>11</java.version>`. Update `maven-compiler-plugin` to 3.8.1+ and replace `<source>`/`<target>` with `<release>11</release>`.
   - Gradle: Set `sourceCompatibility = '11'` and `targetCompatibility = '11'`. Use `options.release.set(11)` if on Gradle 6.6+.
   - Pin all version ranges to fixed versions for reproducible builds.

3. **Upgrade dependencies for Java 11 compatibility.** Use this reference table — only update dependencies the project actually uses:

   | Dependency | Target Version | Notes |
   |---|---|---|
   | Spring Framework | 5.3.x | 4.x does not officially support Java 11 |
   | Spring Boot | 2.6.x+ | Aligns with Spring 5.1+ |
   | Hibernate ORM | 5.6.x | Remove separate `hibernate-entitymanager` — it's merged into `hibernate-core` in 5.x |
   | Jetty | 9.4.12+ or 10.x | 10.x is built for Java 11 |
   | Tomcat (embedded) | 9.0.x+ | |
   | Jackson | 2.13.x+ | |
   | SLF4J | 1.7.28+ or 2.0.x | |
   | JUnit 4 | 4.13.2 | |
   | Mockito | 4.x+ | Use `mockito-core` (not `mockito-all`, which is discontinued) |
   | MySQL Connector | 8.0.33 | Driver class changed to `com.mysql.cj.jdbc.Driver` |
   | Hibernate Validator | 6.2.x | |
   | ActiveMQ | 5.16.x+ | |
   | Liquibase | 4.x | |
   | maven-shade-plugin | 3.2.0+ | |
   | maven-surefire-plugin | 2.22.2+ or 3.x | Needed for Java 11 module handling |

4. **Add standalone replacements for removed Java EE modules.** Java 11 removed these from the JDK (JEP 320) — add as explicit dependencies only if the project uses them:
   - **JAXB**: `javax.xml.bind:jaxb-api:2.3.1` + `org.glassfish.jaxb:jaxb-runtime:2.3.9`
   - **JAX-WS**: `javax.xml.ws:jaxws-api:2.3.1` + `com.sun.xml.ws:jaxws-rt:2.3.7`
   - **javax.annotation**: `javax.annotation:javax.annotation-api:1.3.2`
   - **javax.activation**: `javax.activation:activation:1.1.1`

5. **Update Spring and Hibernate configuration.**
   - Update Spring ORM XML class references: `hibernate4` → `hibernate5` (e.g., `org.springframework.orm.hibernate4.LocalSessionFactoryBean` → `org.springframework.orm.hibernate5.LocalSessionFactoryBean`).
   - Update database driver class names: `com.mysql.jdbc.Driver` → `com.mysql.cj.jdbc.Driver`.
   - Remove the `hibernate-entitymanager` dependency and update any `org.hibernate.ejb` imports in source code.

6. **Migrate test frameworks.**
   - Replace `mockito-all` with `mockito-core`.
   - Remove all PowerMock dependencies (`powermock-module-junit4`, `powermock-api-mockito2`, etc.) and migrate to Mockito's built-in `mockStatic()`:
     - Remove `@RunWith(PowerMockRunner.class)` and `@PrepareForTest` → use `@RunWith(MockitoJUnitRunner.class)`.
     - Replace `PowerMockito.mockStatic(Foo.class)` → `try (MockedStatic<Foo> m = Mockito.mockStatic(Foo.class)) { ... }`.
     - Replace `PowerMockito.when(Foo.bar())` → `m.when(() -> Foo.bar()).thenReturn(...)`.
     - Replace `PowerMockito.verifyStatic(Foo.class)` → `m.verify(() -> Foo.bar())`.
   - Fix Mockito 4.x package changes:
     - `org.mockito.runners.MockitoJUnitRunner` → `org.mockito.junit.MockitoJUnitRunner`
     - `org.mockito.Matchers` → `org.mockito.ArgumentMatchers`
     - `Matchers.anyListOf(X.class)` → `Mockito.anyList()`
   - If `mockStatic` doesn't work with `mockito-core` alone, add `mockito-inline`.

7. **Fix removed/deprecated APIs in source code.**
   - `sun.misc.BASE64Encoder/Decoder` → `java.util.Base64.getEncoder()/getDecoder()`
   - `sun.reflect.Reflection.getCallerClass()` → `StackWalker` API
   - Any single `_` used as a variable name → rename it (reserved keyword since Java 9)
   - Replace any other `sun.*` internal API usages with their official replacements

8. **Update JVM flags and CI configuration.**
   - Remove obsolete flags: `-XX:MaxPermSize`, `-XX:PermSize`, `-Xincgc`, `-XX:+CMSIncrementalMode`, `-XX:+UseCMSCompactAtFullCollection`, `-XX:+CMSFullGCsBeforeCompaction`
   - Update GC logging: `-XX:+PrintGCDetails` / `-XX:+PrintGC` → `-Xlog:gc*`
   - If libraries need reflective access, add `--add-opens` flags as a last resort (prefer upgrading the library instead)
   - Update the JDK version in CI pipeline configs (e.g., `.github/workflows`, `Jenkinsfile`, `.gitlab-ci.yml`) to Java 11

9. **Compile the project and fix all errors.** Run `mvn clean compile` (Maven) or `./gradlew compileJava` (Gradle). Skip DB plugins if needed with `-Dliquibase.should.run=false` or `-Dflyway.skip=true`. Iterate until compilation succeeds with zero errors.

10. **Run the full test suite and fix failures.** Run `mvn clean test` (Maven) or `./gradlew test` (Gradle). Common failure causes:
    - Mockito 4.x strict stubbing (unnecessary stubs now fail by default — remove them or use `lenient()`)
    - CLDR locale data is now default — date/currency formatting may differ
    - Regex behavior change: negated character classes now negate nested classes too

11. **Build the final artifact and verify.** Run `mvn clean package` (Maven) or `./gradlew build` (Gradle). Verify the JAR/WAR is produced successfully.

12. **Create a PR** summarizing all changes: Java version bump, dependency upgrades, removed dependencies (PowerMock, hibernate-entitymanager), added dependencies (JAXB etc.), configuration updates, API replacements, test migrations, and JVM flag changes.

## Specifications

- The project compiles and builds with Java 11
- All existing tests pass (or user-acknowledged failures are documented in the PR)
- No version ranges remain — all dependencies pinned to specific versions
- No PowerMock dependencies remain (replaced by Mockito built-in features)
- No references to removed Java EE modules without standalone replacement dependencies
- No usage of removed JDK internal APIs (`sun.misc.*`, `sun.reflect.*`)
- No obsolete JVM flags in startup scripts, Dockerfiles, or CI configuration
- **Validation**: `mvn clean verify` / `./gradlew build` succeeds with Java 11 — zero compilation errors and all tests pass

## Advice and Pointers

- **Start with compilation, not tests.** Get the project compiling first, then fix test failures. This prevents chasing test issues caused by missing dependencies.
- **Do not adopt the Java module system (JPMS).** For this migration, keep the application on the classpath. Creating a `module-info.java` is a separate effort and not required to run on Java 11.
- **Hibernate 4 to 5 is the trickiest upgrade.** The merge of `hibernate-entitymanager` into `hibernate-core` can cause `ClassNotFoundException` if code imports from `org.hibernate.ejb`. Search for these imports proactively.
- **maven-shade-plugin uber JARs on Java 11:** If the project uses `ServicesResourceTransformer`, verify that `META-INF/services` files merge correctly — the ServiceLoader mechanism changed in Java 9+. Also filter out `module-info.class` files from dependencies to avoid conflicts: add `<exclude>module-info.class</exclude>` to the shade plugin's filters.
- **Spring Boot version jumps** (e.g., 2.0→2.6) involve config property renames and deprecations beyond just the Java version. Use `spring-boot-properties-migrator` as a runtime dependency during migration to detect renamed properties, then update `application.properties`/`application.yml` and remove the migrator.
- **Gradle plugin compatibility:** When upgrading Gradle-based projects, verify that all Gradle plugins (e.g., `spring-boot`, `spotless`, `dgs-codegen`) are compatible with both the new Java version and the Gradle version. Upgrade Gradle itself to 6.7+ for reliable Java 11 support.
- **Mockito 4.x strict stubbing** will fail tests with unnecessary stubs. Fix by removing the stub or wrapping with `lenient().when(...)`.
- **CLDR locale data is default starting Java 9.** If tests rely on specific locale formatting, use `-Djava.locale.providers=COMPAT,CLDR` as a temporary workaround.
- **Liquibase/Flyway plugins may fail** if no database is available. Skip with `-Dliquibase.should.run=false` or `-Dflyway.skip=true`.
- **Java 11 compiler minimum source level is 6** (and 6 is deprecated). If the project was on Java 5 or earlier source level, update to at least 6.

## Forbidden Actions

- Do not skip tests or mark them `@Ignore`/`@Disabled` to make the build pass — fix them or get explicit user approval.
- Do not add `--illegal-access=permit` as a permanent JVM flag — it masks real issues and was removed in Java 17.
- Do not downgrade dependencies to avoid compatibility work.
- Do not modify test assertions to match changed behavior without understanding why the behavior changed.
- Do not remove Liquibase/Flyway migration files — only skip the plugins during build if no DB is available.
- Do not create a `module-info.java` unless the user explicitly requests Java module system adoption.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
