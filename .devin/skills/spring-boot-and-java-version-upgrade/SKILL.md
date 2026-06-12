---
name: spring-boot-and-java-version-upgrade
description: "Converted from Devin playbook: Spring Boot and Java Version Upgrade"
triggers:
  - user
  - model
---

# Spring Boot and Java Version Upgrade

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Spring Boot and Java Version Upgrade (playbook-f6e200aaca1841ff9111ead79fba3c90)

## Procedure

# Spring Boot and Java Version Upgrade Playbook

## Overview
This playbook guides the upgrade of Spring Boot applications from version 2.x to 3.x and Java from version 17 to 21. It covers both Maven and Gradle build systems, handles single and multi-module projects, and ensures all changes are thoroughly tested before deployment.

## What's Needed From User
- Repository URL or local path to the Spring Boot project
- Target Spring Boot 3.x version (e.g., 3.2.0, 3.3.0)
- Confirmation that the project currently builds on Java 17 (if not, perform an intermediate upgrade to Java 17 first)

## Procedure

### Phase 1: Assessment and Preparation

1. Clone the repository and create an upgrade branch named `upgrade/spring-boot-{target-version}-java-21`

2. Identify the build system by checking for `pom.xml` (Maven) or `build.gradle`/`build.gradle.kts` (Gradle). Check if this is a multi-module project (parent POM with modules, or `settings.gradle` with multiple includes).

3. Run existing tests to establish a baseline - all tests should pass before making changes:
   - Maven: `./mvnw verify`
   - Gradle: `./gradlew check`

4. Document the current versions and identify Spring ecosystem dependencies that may need version alignment (Spring Cloud, Spring Data, Spring Security, Spring Session, etc.)

### Phase 2: Update Build Configuration

Follow steps 5a–8a for Maven projects OR steps 5b–7b for Gradle projects (not both).

**For Maven projects:**

5a. Update the Spring Boot parent version in `pom.xml`:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>{target-spring-boot-version}</version>
    <relativePath/>
</parent>
```

6a. Update the Java version property:
```xml
<properties>
    <java.version>21</java.version>
</properties>
```

7a. If maven-compiler-plugin is explicitly configured, update it:
```xml
<configuration>
    <source>21</source>
    <target>21</target>
</configuration>
```

8a. For multi-module Maven projects: ensure all child modules inherit the updated parent or explicitly set `java.version`. Check each module's `pom.xml` for any overridden versions.

**For Gradle projects:**

5b. Update the Spring Boot plugin version. Look for existing configuration in `build.gradle` or `build.gradle.kts`:
```groovy
plugins {
    id 'org.springframework.boot' version '{target-spring-boot-version}'
    id 'io.spring.dependency-management' version '1.1.4'
}
```

6b. Find and update the existing Java version configuration (may be in `java {}` block, toolchains, or `compileJava` task):
```groovy
java {
    sourceCompatibility = '21'
    targetCompatibility = '21'
}
```

7b. For multi-module Gradle projects: ensure Java and Boot versions are configured in a shared `subprojects {}` or `allprojects {}` block in the root `build.gradle`, or update each module consistently.

### Phase 3: Spring Boot 2.x to 3.x Migration

8. **Jakarta namespace migration (critical):** Search the codebase for `import javax.` and migrate each occurrence to the corresponding `jakarta.*` package:
   ```bash
   grep -r "import javax\." --include="*.java" .
   ```
   Common migrations include servlet, persistence, validation, annotation, transaction, and inject packages.

9. **Spring Security migration (if applicable):** Update to the new configuration style:
   - Replace `WebSecurityConfigurerAdapter` with `SecurityFilterChain` beans:
   ```java
   @Bean
   SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
       http
           .authorizeHttpRequests(auth -> auth
               .requestMatchers("/public/**").permitAll()
               .anyRequest().authenticated()
           )
           .formLogin(Customizer.withDefaults());
       return http.build();
   }
   ```
   - Replace `antMatchers()` with `requestMatchers()`
   - Replace `authorizeRequests()` with `authorizeHttpRequests()`
   - Replace `@EnableGlobalMethodSecurity` with `@EnableMethodSecurity`

10. **Spring ecosystem version alignment:** If the project uses Spring Cloud, Spring Data, or Spring Session, align their versions with the chosen Spring Boot 3.x release using the official compatibility matrix.

11. **Database dependencies:** Update database drivers if needed:
    - MySQL: use `com.mysql:mysql-connector-j` instead of deprecated `mysql:mysql-connector-java`

### Phase 4: Update Container and CI Configuration

12. If the project has a Dockerfile, update base images to Java 21:
    - Maven builder: `maven:3.9-eclipse-temurin-21`
    - Gradle builder: `gradle:8-jdk21`
    - Runtime: `eclipse-temurin:21-jre-alpine`

13. Update CI/CD workflow files to use Java 21. For GitHub Actions:
    ```yaml
    - uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    ```
    For GitLab CI, Jenkins, or other systems, update their respective JDK configuration.

### Phase 5: Build and Test

14. Clean and rebuild the project:
    - Maven: `./mvnw clean compile`
    - Gradle: `./gradlew clean build`

15. Fix any compilation errors from the Jakarta namespace migration or Spring Security refactoring

16. Run the full test suite including integration tests:
    - Maven: `./mvnw verify`
    - Gradle: `./gradlew check`

17. Run the application locally to verify it starts correctly:
    - Maven: `./mvnw spring-boot:run`
    - Gradle: `./gradlew bootRun`
    - Or with docker-compose if available:
      ```bash
      docker-compose build && docker-compose up -d
      docker-compose ps  # Verify health status
      ```

18. Verify the application is accessible - check key endpoints respond correctly

19. Clean up Docker resources if used: `docker-compose down`

### Phase 6: Create Pull Request

20. Commit all changes with a descriptive message

21. Push the branch and create a pull request

22. Wait for CI checks to pass and address any failures

## Specifications

**Success Criteria:**
- All existing tests pass after the upgrade (unit and integration)
- Application builds and starts correctly
- CI/CD pipeline passes all checks

**Deliverables:**
- Pull request with all version upgrades
- Verified local testing results
- CI checks passing

## Advice and Pointers

- **Jakarta namespace migration is the most common source of errors.** Always search for `javax.` imports rather than assuming a fixed list.

- **Spring Security 6.x changes are significant.** Look for `WebSecurityConfigurerAdapter` and refactor to `SecurityFilterChain` beans.

- **Hibernate 6 (used in Boot 3.x) is stricter.** Pay attention to JPA tests if you have custom Hibernate dialects or native queries.

- **Spring ecosystem compatibility matters.** Verify Spring Cloud/Data/Session version compatibility with your target Spring Boot version.

- **For large codebases**, consider OpenRewrite recipes: `org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0`

## Forbidden Actions

- Do not skip running tests before and after the upgrade
- Do not push directly to main/master branch
- Do not ignore compilation errors or test failures
- Do not use deprecated openjdk Docker images - use eclipse-temurin
- Do not attempt this upgrade if the project is not already on Java 17

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
