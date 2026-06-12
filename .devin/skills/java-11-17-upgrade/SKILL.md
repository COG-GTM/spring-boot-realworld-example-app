---
name: java-11-17-upgrade
description: "Converted from Devin playbook: Java 11-17 Upgrade"
triggers:
  - user
  - model
---

# Java 11-17 Upgrade

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Java 11-17 Upgrade (playbook-2cd87fdb85734689bebcee8eb5655935), macro `!java_11_17`

## Procedure

# Playbook: Java 11 to Java 17 Upgrade

## Overview

This playbook provides step-by-step instructions for upgrading a Java 11 project to Java 17. For Spring Boot projects, this typically requires upgrading to Spring Boot 3.x, which includes migrating from javax.* to jakarta.* namespaces and updating related dependencies.

## What's Needed From User

First question is MANDATORY to ask user what the version of Java is either a) 11 or b) 8

- Repository URL or access to the Java 11 project
- Confirmation of the project's framework (Spring Boot, plain Java, etc.)
- Any specific constraints or requirements for the migration

## Procedure

1. **Create a New Branch**
   - Check out a new branch for the upgrade task.
   ```bash
   git checkout -b upgrade-java-17-$(date +%s)
   ```

2. **Analyze Current Project State**
   - Verify the project is currently using Java 11
   - Identify the build tool (Maven or Gradle)
   - For Spring Boot projects, note the current Spring Boot version
   - Scan for javax.* imports that will need migration to jakarta.*
   ```bash
   grep -r "import javax\." src/main/java --include="*.java" | head -50
   ```

3. **Verify Dependency Compatibility**
   - For Spring Boot projects: Spring Boot 3.x requires Java 17 minimum
   - Check if Springfox Swagger is used (must be replaced with SpringDoc OpenAPI for Spring Boot 3.x)
   - Verify Lombok, H2, and other dependencies are compatible with Java 17
   - Document any dependencies that need version updates

4. **Update Project Build Configuration**
   - **For Maven**: Update `pom.xml`:
     ```xml
     <properties>
         <java.version>17</java.version>
         <maven.compiler.source>17</maven.compiler.source>
         <maven.compiler.target>17</maven.compiler.target>
         <maven.compiler.release>17</maven.compiler.release>
     </properties>
     ```
   - **For Gradle**: Update `build.gradle`:
     ```groovy
     sourceCompatibility = '17'
     targetCompatibility = '17'
     ```

5. **Update Spring Boot Version (if applicable)**
   - Upgrade Spring Boot Parent to 3.2.x or later
   - This is required because Spring Boot 2.x does not fully support Java 17 features
   ```xml
   <parent>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-parent</artifactId>
       <version>3.2.5</version>
   </parent>
   ```

6. **Migrate javax.* to jakarta.* Namespaces**
   - Spring Boot 3.x uses Jakarta EE 9+ which renamed all javax.* packages
   - Update all imports in entity/model classes:
     - `javax.persistence.*` to `jakarta.persistence.*`
     - `javax.validation.*` to `jakarta.validation.*`
     - `javax.servlet.*` to `jakarta.servlet.*`
     - `javax.annotation.*` to `jakarta.annotation.*`

7. **Update Spring Security Configuration (if applicable)**
   - `WebSecurityConfigurerAdapter` is removed in Spring Security 6.x
   - Migrate to component-based configuration using `SecurityFilterChain` bean:
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           http.authorizeHttpRequests(auth -> auth
               .requestMatchers("/h2-console/**").permitAll()
               .anyRequest().authenticated())
               .csrf(csrf -> csrf.disable())
               .headers(headers -> headers.frameOptions(frame -> frame.disable()));
           return http.build();
       }
   }
   ```

8. **Migrate Swagger/OpenAPI (if applicable)**
   - Replace Springfox with SpringDoc OpenAPI (Springfox is incompatible with Spring Boot 3.x)
   - Remove Springfox dependencies and add:
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.3.0</version>
   </dependency>
   ```
   - Update Swagger configuration to use OpenAPI bean instead of Docket
   - Swagger UI path changes from `/swagger-ui.html` to `/swagger-ui/index.html`

9. **Update Build Tools and Plugins**
   - Update Maven Wrapper to 3.9.x if used
   - Update Maven Compiler Plugin to 3.12.x with `<release>17</release>`
   - Update test plugins (surefire, failsafe) to 3.2.x for Java 17 compatibility

10. **Update CI/CD Pipeline**
    - Update CI configuration files to use Java 17
    - For GitHub Actions, update `java-version` to '17'

11. **Run Tests and Verify**
    - Compile the project to catch any migration issues:
    ```bash
    mvn clean compile
    ```
    - Run the full test suite:
    ```bash
    mvn test
    ```
    - Start the application and verify endpoints work correctly

12. **Update Documentation**
    - Update README.md with Java 17 requirement
    - Document any breaking changes or migration notes

## Specifications

- All javax.* imports migrated to jakarta.* (for Spring Boot 3.x projects)
- Spring Security configured with new SecurityFilterChain API (if applicable)
- All unit tests pass without errors
- Application builds and runs successfully with Java 17
- CI/CD pipeline passes with updated configuration
- Swagger/OpenAPI documentation accessible (if applicable)

## Advice and Pointers

- For complex migrations, consider a phased approach: analysis first, then build config, then code changes, then testing
- The javax.* to jakarta.* migration is the most significant change for Spring Boot projects
- Spring Security 6.x removes `WebSecurityConfigurerAdapter` entirely - use `SecurityFilterChain` beans instead
- Springfox Swagger is abandoned and incompatible with Spring Boot 3.x - use SpringDoc OpenAPI instead
- Test thoroughly after migration as some behavioral changes may occur with Hibernate 6.x (used by Spring Boot 3.x)

## Forbidden Actions

- Do not skip the javax.* to jakarta.* migration for Spring Boot 3.x projects
- Do not use Springfox with Spring Boot 3.x (it will not work)
- Do not use WebSecurityConfigurerAdapter with Spring Security 6.x (it has been removed)

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
