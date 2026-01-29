# Spring Boot 2.6.3 to 3.x Upgrade Plan

This document provides a comprehensive, step-by-step guide for upgrading the `spring-boot-realworld-example-app` from Spring Boot 2.6.3 to Spring Boot 3.x. The application has both REST and GraphQL APIs, uses MyBatis for persistence, and follows a layered DDD architecture.

## Table of Contents

1. [Overview and Prerequisites](#1-overview-and-prerequisites)
2. [Breaking Changes to Address](#2-breaking-changes-to-address)
3. [Incremental Upgrade Strategy](#3-incremental-upgrade-strategy)
4. [Detailed Implementation Steps](#4-detailed-implementation-steps)
5. [Local Verification Tests](#5-local-verification-tests)
6. [Architecture Considerations](#6-architecture-considerations)
7. [Troubleshooting Guide](#7-troubleshooting-guide)
8. [Rollback Plan](#8-rollback-plan)

---

## 1. Overview and Prerequisites

### Current Technology Stack

| Component | Current Version | Target Version |
|-----------|-----------------|----------------|
| Spring Boot | 2.6.3 | 3.2.x or 3.3.x |
| Java | 11 | 17 (minimum) |
| Dependency Management Plugin | 1.0.11.RELEASE | 1.1.x |
| MyBatis Spring Boot Starter | 2.2.2 | 3.0.x |
| Netflix DGS Framework | 4.9.21 | 8.x or 9.x |
| DGS Codegen Plugin | 5.0.6 | 6.x |
| JJWT | 0.11.2 | 0.12.x |
| Rest-Assured | 4.5.1 | 5.x |

### Prerequisites

Before starting the upgrade, ensure you have:

1. Java 17 JDK installed locally
2. Gradle 8.x installed (or use the Gradle wrapper)
3. A clean git branch for the upgrade work
4. All tests passing on the current version

### Pre-Upgrade Verification

Run the following commands to ensure the current state is stable:

```bash
# Verify current tests pass
./gradlew clean test

# Verify the application builds
./gradlew build

# Note the current test count for later comparison
./gradlew test --info | grep -E "tests|passed|failed"
```

---

## 2. Breaking Changes to Address

### 2.1 Java Version Upgrade (Java 11 → Java 17)

Spring Boot 3.x requires Java 17 as the minimum version. This is a hard requirement.

**Files to modify:**

| File | Change Required |
|------|-----------------|
| `build.gradle` | Update `sourceCompatibility` and `targetCompatibility` from `'11'` to `'17'` |
| `.github/workflows/gradle.yml` | Update `java-version` from `'11'` to `'17'` |

**Potential Issues:**
- Deprecated Java APIs removed in Java 17
- Reflection access restrictions (strong encapsulation)
- Pattern matching and sealed classes may affect some code patterns

### 2.2 Package Migration (javax → jakarta)

Spring Boot 3.x uses Jakarta EE 9+ which requires migrating from `javax.*` to `jakarta.*` namespaces. This is one of the most significant changes.

**Important Note:** `javax.crypto.*` packages (used in `DefaultJwtService.java`) are part of Java SE, NOT Jakarta EE, and should NOT be changed.

**Files requiring javax.validation → jakarta.validation migration:**

| File | Lines | Imports to Change |
|------|-------|-------------------|
| `src/main/java/io/spring/application/article/DuplicatedArticleConstraint.java` | 8-9 | `javax.validation.Constraint`, `javax.validation.Payload` |
| `src/main/java/io/spring/application/article/DuplicatedArticleValidator.java` | 5-6 | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `src/main/java/io/spring/application/article/NewArticleParam.java` | 5 | `javax.validation.constraints.NotBlank` |
| `src/main/java/io/spring/application/article/ArticleCommandService.java` | 6 | `javax.validation.Valid` |
| `src/main/java/io/spring/application/user/DuplicatedUsernameValidator.java` | 4-5 | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `src/main/java/io/spring/application/user/DuplicatedEmailConstraint.java` | 5-6 | `javax.validation.Constraint`, `javax.validation.Payload` |
| `src/main/java/io/spring/application/user/DuplicatedUsernameConstraint.java` | 5-6 | `javax.validation.Constraint`, `javax.validation.Payload` |
| `src/main/java/io/spring/application/user/DuplicatedEmailValidator.java` | 4-5 | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `src/main/java/io/spring/application/user/UserService.java` | 7-10 | `javax.validation.Constraint`, `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext`, `javax.validation.Valid` |
| `src/main/java/io/spring/application/user/RegisterParam.java` | 4-5 | `javax.validation.constraints.Email`, `javax.validation.constraints.NotBlank` |
| `src/main/java/io/spring/application/user/UpdateUserParam.java` | 4 | `javax.validation.constraints.Email` |
| `src/main/java/io/spring/api/ArticleApi.java` | 15 | `javax.validation.Valid` |
| `src/main/java/io/spring/api/UsersApi.java` | 18-20 | `javax.validation.Valid`, `javax.validation.constraints.Email`, `javax.validation.constraints.NotBlank` |
| `src/main/java/io/spring/api/ArticlesApi.java` | 10 | `javax.validation.Valid` |
| `src/main/java/io/spring/api/CurrentUserApi.java` | 12 | `javax.validation.Valid` |
| `src/main/java/io/spring/api/CommentsApi.java` | 17-18 | `javax.validation.Valid`, `javax.validation.constraints.NotBlank` |
| `src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java` | 10-11 | `javax.validation.ConstraintViolation`, `javax.validation.ConstraintViolationException` |
| `src/main/java/io/spring/graphql/UserMutation.java` | 21 | `javax.validation.ConstraintViolationException` |
| `src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java` | 20-21 | `javax.validation.ConstraintViolation`, `javax.validation.ConstraintViolationException` |

**Files requiring javax.servlet → jakarta.servlet migration:**

| File | Lines | Imports to Change |
|------|-------|-------------------|
| `src/main/java/io/spring/api/security/JwtTokenFilter.java` | 8-11 | `javax.servlet.FilterChain`, `javax.servlet.ServletException`, `javax.servlet.http.HttpServletRequest`, `javax.servlet.http.HttpServletResponse` |

### 2.3 Spring Security Configuration Refactoring

Spring Security 6 (used in Spring Boot 3.x) removes `WebSecurityConfigurerAdapter`. The configuration must be refactored to use component-based security configuration.

**File to refactor:** `src/main/java/io/spring/api/security/WebSecurityConfig.java`

**Current Implementation (lines 21-65):**
```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
  // ...
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf()
        .disable()
        .cors()
        // ... uses antMatchers()
  }
}
```

**Required Changes:**

1. Remove `extends WebSecurityConfigurerAdapter`
2. Replace `configure(HttpSecurity http)` method with a `@Bean SecurityFilterChain` method
3. Replace `antMatchers()` with `requestMatchers()`
4. Update the lambda-based DSL (recommended for Spring Security 6)

**New Implementation Pattern:**
```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
  // ...
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers("/graphiql", "/graphql").permitAll()
            .requestMatchers(HttpMethod.GET, "/articles/feed").authenticated()
            .requestMatchers(HttpMethod.POST, "/users", "/users/login").permitAll()
            .requestMatchers(HttpMethod.GET, "/articles/**", "/profiles/**", "/tags").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
  }
}
```

### 2.4 Dependency Version Updates

**build.gradle changes:**

```groovy
plugins {
    // FROM:
    id 'org.springframework.boot' version '2.6.3'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id "com.netflix.dgs.codegen" version "5.0.6"
    
    // TO:
    id 'org.springframework.boot' version '3.2.5'  // or latest 3.2.x/3.3.x
    id 'io.spring.dependency-management' version '1.1.5'
    id "com.netflix.dgs.codegen" version "6.2.1"  // Spring Boot 3 compatible
}

dependencies {
    // FROM:
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:2.2.2'
    implementation 'com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter:4.9.21'
    implementation 'io.jsonwebtoken:jjwt-api:0.11.2'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.2',
                'io.jsonwebtoken:jjwt-jackson:0.11.2'
    implementation 'org.xerial:sqlite-jdbc:3.36.0.3'
    testImplementation 'io.rest-assured:rest-assured:4.5.1'
    testImplementation 'io.rest-assured:json-path:4.5.1'
    testImplementation 'io.rest-assured:xml-path:4.5.1'
    testImplementation 'io.rest-assured:spring-mock-mvc:4.5.1'
    testImplementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter-test:2.2.2'
    
    // TO:
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    implementation 'com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter:8.5.0'  // Note: different artifact name
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5',
                'io.jsonwebtoken:jjwt-jackson:0.12.5'
    implementation 'org.xerial:sqlite-jdbc:3.45.3.0'
    testImplementation 'io.rest-assured:rest-assured:5.4.0'
    testImplementation 'io.rest-assured:json-path:5.4.0'
    testImplementation 'io.rest-assured:xml-path:5.4.0'
    testImplementation 'io.rest-assured:spring-mock-mvc:5.4.0'
    testImplementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.3'
}
```

**Netflix DGS Important Note:**
The Netflix DGS framework changed its artifact name for Spring Boot 3 compatibility:
- Old: `graphql-dgs-spring-boot-starter`
- New: `graphql-dgs-spring-graphql-starter`

This is because DGS now integrates with Spring GraphQL instead of providing its own servlet infrastructure.

---

## 3. Incremental Upgrade Strategy

We recommend an incremental approach to minimize risk and make debugging easier:

### Phase 1: Upgrade to Spring Boot 2.7.x (Optional but Recommended)

Spring Boot 2.7.x includes deprecation warnings for APIs that will be removed in Spring Boot 3.x. This intermediate step helps identify issues early.

**Steps:**
1. Update Spring Boot version to 2.7.18 (latest 2.7.x)
2. Run tests and fix any deprecation warnings
3. Verify application functionality

### Phase 2: Java 17 Upgrade

**Steps:**
1. Update `build.gradle` Java version
2. Update CI workflow Java version
3. Verify compilation and tests pass

### Phase 3: Package Migration (javax → jakarta)

**Steps:**
1. Update all import statements
2. Verify validation annotations work
3. Verify servlet filters work

### Phase 4: Spring Security Refactoring

**Steps:**
1. Refactor `WebSecurityConfig.java`
2. Verify authentication/authorization works
3. Verify both REST and GraphQL endpoints

### Phase 5: Dependency Updates and Spring Boot 3.x

**Steps:**
1. Update all dependencies to Spring Boot 3 compatible versions
2. Update Spring Boot version
3. Run full test suite
4. Manual API testing

---

## 4. Detailed Implementation Steps

### Step 1: Create Upgrade Branch

```bash
git checkout -b feature/spring-boot-3-upgrade
```

### Step 2: (Optional) Upgrade to Spring Boot 2.7.x First

Edit `build.gradle`:
```groovy
plugins {
    id 'org.springframework.boot' version '2.7.18'
    // ... rest unchanged
}
```

Run verification:
```bash
./gradlew clean test
```

### Step 3: Update Java Version

Edit `build.gradle`:
```groovy
sourceCompatibility = '17'
targetCompatibility = '17'
```

Edit `.github/workflows/gradle.yml`:
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    distribution: zulu
    java-version: '17'
```

Run verification:
```bash
export JAVA_HOME=/path/to/java17
./gradlew clean test
```

### Step 4: Migrate javax to jakarta Packages

For each file listed in Section 2.2, replace:
- `import javax.validation.` → `import jakarta.validation.`
- `import javax.servlet.` → `import jakarta.servlet.`

**DO NOT change:**
- `import javax.crypto.` (these are Java SE packages, not Jakarta EE)

You can use the following sed commands (run from project root):
```bash
# For validation imports
find src -name "*.java" -exec sed -i 's/import javax\.validation\./import jakarta.validation./g' {} \;

# For servlet imports
find src -name "*.java" -exec sed -i 's/import javax\.servlet\./import jakarta.servlet./g' {} \;
```

**Verify the changes don't affect javax.crypto:**
```bash
grep -r "javax.crypto" src/
# Should still show DefaultJwtService.java using javax.crypto
```

### Step 5: Refactor Spring Security Configuration

Replace the contents of `src/main/java/io/spring/api/security/WebSecurityConfig.java`:

```java
package io.spring.api.security;

import static java.util.Arrays.asList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  @Bean
  public JwtTokenFilter jwtTokenFilter() {
    return new JwtTokenFilter();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers("/graphiql").permitAll()
            .requestMatchers("/graphql").permitAll()
            .requestMatchers(HttpMethod.GET, "/articles/feed").authenticated()
            .requestMatchers(HttpMethod.POST, "/users", "/users/login").permitAll()
            .requestMatchers(HttpMethod.GET, "/articles/**", "/profiles/**", "/tags").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(asList("*"));
    configuration.setAllowedMethods(asList("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
    configuration.setAllowCredentials(false);
    configuration.setAllowedHeaders(asList("Authorization", "Cache-Control", "Content-Type"));
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
```

### Step 6: Update Dependencies in build.gradle

Replace the full `build.gradle`:

```groovy
plugins {
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.5'
    id 'java'
    id "com.netflix.dgs.codegen" version "6.2.1"
    id "com.diffplug.spotless" version "6.25.0"
}

version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'
targetCompatibility = '17'

spotless {
    java {
        target project.fileTree(project.rootDir) {
            include '**/*.java'
            exclude 'build/generated/**/*.*', 'build/generated-examples/**/*.*'
        }
        googleJavaFormat()
    }
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-hateoas'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    implementation 'com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter:8.5.0'
    implementation 'org.flywaydb:flyway-core'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5',
                'io.jsonwebtoken:jjwt-jackson:0.12.5'
    implementation 'joda-time:joda-time:2.12.7'
    implementation 'org.xerial:sqlite-jdbc:3.45.3.0'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'io.rest-assured:rest-assured:5.4.0'
    testImplementation 'io.rest-assured:json-path:5.4.0'
    testImplementation 'io.rest-assured:xml-path:5.4.0'
    testImplementation 'io.rest-assured:spring-mock-mvc:5.4.0'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.3'
}

tasks.named('test') {
    useJUnitPlatform()
}

tasks.named('clean') {
    doFirst {
        delete './dev.db'
    }
}

tasks.named('generateJava') {
    schemaPaths = ["${projectDir}/src/main/resources/schema"]
    packageName = 'io.spring.graphql'
}
```

### Step 7: Handle DGS Framework Changes

The Netflix DGS framework has significant changes for Spring Boot 3. You may need to:

1. Update any DGS-specific configurations
2. Check if any DGS annotations have changed
3. Verify GraphQL schema generation still works

Run:
```bash
./gradlew generateJava
```

### Step 8: Run Full Test Suite

```bash
./gradlew clean test
```

---

## 5. Local Verification Tests

### 5.1 After Java 17 Upgrade

**Verification Command:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version  # Should show Java 17
./gradlew clean test
```

**Expected Result:** All tests pass, no compilation errors.

**What to check:**
- No `UnsupportedClassVersionError`
- No reflection access warnings (or handle with `--add-opens` if needed)

### 5.2 After javax → jakarta Migration

**Verification Tests:**

The following existing tests validate that validation annotations still work:

| Test File | What It Tests |
|-----------|---------------|
| `UsersApiTest.java` | Tests `@NotBlank`, `@Email` validation on user registration |
| `CurrentUserApiTest.java` | Tests validation on user update |
| `ArticleApiTest.java` | Tests `@Valid` annotation on article creation |
| `CommentsApiTest.java` | Tests `@NotBlank` validation on comments |

**Run specific validation tests:**
```bash
./gradlew test --tests "io.spring.api.UsersApiTest"
./gradlew test --tests "io.spring.api.CurrentUserApiTest"
```

**Key tests that validate servlet filters work:**
- `CurrentUserApiTest.should_get_401_without_token` - Tests JwtTokenFilter
- `CurrentUserApiTest.should_get_401_with_invalid_token` - Tests JwtTokenFilter
- `CurrentUserApiTest.should_get_current_user_with_token` - Tests JwtTokenFilter authentication

### 5.3 After Spring Security Refactoring

**Verification Tests:**

| Test | What It Validates |
|------|-------------------|
| `UsersApiTest.should_create_user_success` | POST /users is publicly accessible |
| `UsersApiTest.should_login_success` | POST /users/login is publicly accessible |
| `CurrentUserApiTest.should_get_401_without_token` | Protected endpoints require authentication |
| `CurrentUserApiTest.should_get_current_user_with_token` | Authenticated requests work |

**Run security-related tests:**
```bash
./gradlew test --tests "io.spring.api.*"
```

**Manual verification for GraphQL endpoints:**
```bash
# Start the application
./gradlew bootRun

# In another terminal, test GraphQL endpoint accessibility
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ tags }"}'

# Test GraphiQL is accessible (should return HTML)
curl http://localhost:8080/graphiql
```

### 5.4 After Dependency Updates (Full Verification)

**Full Test Suite:**
```bash
./gradlew clean test
```

**Build Verification:**
```bash
./gradlew build
./gradlew bootJar
```

**Docker Build Verification (if applicable):**
```bash
./gradlew bootBuildImage
```

**Manual API Testing:**

1. Start the application:
```bash
./gradlew bootRun
```

2. Test REST API endpoints:
```bash
# Test tags endpoint (public)
curl http://localhost:8080/tags

# Test user registration
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"user": {"username": "testuser", "email": "test@example.com", "password": "password123"}}'

# Test login
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"user": {"email": "test@example.com", "password": "password123"}}'

# Test authenticated endpoint (use token from login response)
curl http://localhost:8080/user \
  -H "Authorization: Token <your-token>"
```

3. Test GraphQL API:
```bash
# Query tags
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "query { tags }"}'

# Query articles
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "query { articles { edges { node { title slug } } } }"}'
```

---

## 6. Architecture Considerations

### Domain Layer (io.spring.core)

The domain layer has no framework dependencies and should NOT require any changes. This includes:
- `io.spring.core.article.*`
- `io.spring.core.comment.*`
- `io.spring.core.favorite.*`
- `io.spring.core.user.*`
- `io.spring.core.service.*` (interfaces only)

**Verification:** These packages should have zero `javax.*` or `jakarta.*` imports (except `javax.crypto` in implementations).

### Application Layer (io.spring.application)

This layer uses validation annotations and will require `javax.validation` → `jakarta.validation` migration.

### Infrastructure Layer (io.spring.infrastructure, io.spring.api)

This layer has the most changes:
- Security configuration refactoring
- Servlet filter package migration
- MyBatis configuration (should work automatically with updated starter)

### GraphQL Layer (io.spring.graphql)

The Netflix DGS framework changes are significant:
- New artifact name: `graphql-dgs-spring-graphql-starter`
- Integration with Spring GraphQL
- Potential annotation changes

**Files to review:**
- `UserMutation.java`
- `ArticleMutation.java`
- `CommentMutation.java`
- `RelationMutation.java`
- All `*Datafetcher.java` files

### Test Layer

Tests using `@WebMvcTest` should continue to work, but verify:
- `WebSecurityConfig` is properly imported
- Mock beans are correctly configured
- Security context is properly set up

---

## 7. Troubleshooting Guide

### Common Issues and Solutions

#### Issue 1: "Cannot resolve symbol 'WebSecurityConfigurerAdapter'"

**Cause:** Spring Security 6 removed this class.

**Solution:** Refactor to use `SecurityFilterChain` bean as shown in Step 5.

#### Issue 2: "Cannot resolve method 'antMatchers'"

**Cause:** `antMatchers()` was replaced with `requestMatchers()` in Spring Security 6.

**Solution:** Replace all `antMatchers()` calls with `requestMatchers()`.

#### Issue 3: "Package javax.validation does not exist"

**Cause:** Jakarta EE 9+ uses `jakarta.validation` namespace.

**Solution:** Replace all `javax.validation` imports with `jakarta.validation`.

#### Issue 4: DGS GraphQL errors

**Cause:** DGS changed its integration approach for Spring Boot 3.

**Solution:**
1. Use the correct artifact: `graphql-dgs-spring-graphql-starter`
2. Check DGS migration guide: https://netflix.github.io/dgs/

#### Issue 5: MyBatis mapping errors

**Cause:** MyBatis Spring Boot Starter 3.x may have configuration changes.

**Solution:**
1. Verify `@MapperScan` annotation is present
2. Check MyBatis configuration in `application.properties`/`application.yml`
3. Review MyBatis Spring Boot 3 migration guide

#### Issue 6: JJWT API changes

**Cause:** JJWT 0.12.x has API changes from 0.11.x.

**Solution:** Review `DefaultJwtService.java` for any deprecated method calls:
- `signWith(key, algorithm)` signature may have changed
- `parserBuilder()` API may have minor changes

#### Issue 7: Test failures with MockMvc

**Cause:** Spring Security test configuration changes.

**Solution:**
1. Ensure `@Import(WebSecurityConfig.class)` is present
2. Add `@AutoConfigureMockMvc` if needed
3. Check security context setup in tests

---

## 8. Rollback Plan

If the upgrade fails and you need to rollback:

### Immediate Rollback

```bash
# Discard all changes and return to main branch
git checkout main
git branch -D feature/spring-boot-3-upgrade
```

### Partial Rollback (keep some changes)

```bash
# Create a backup branch
git checkout -b feature/spring-boot-3-upgrade-backup

# Return to main and cherry-pick specific commits
git checkout main
git cherry-pick <commit-hash>
```

### CI/CD Rollback

If changes were merged and deployed:
1. Revert the merge commit
2. Deploy the reverted version
3. Investigate issues in a new branch

---

## Appendix A: Complete File Change Summary

### Files to Modify

| File | Change Type |
|------|-------------|
| `build.gradle` | Dependency versions, Java version |
| `.github/workflows/gradle.yml` | Java version |
| `WebSecurityConfig.java` | Complete refactoring |
| `JwtTokenFilter.java` | Import migration |
| `UserService.java` | Import migration |
| `DuplicatedArticleConstraint.java` | Import migration |
| `DuplicatedArticleValidator.java` | Import migration |
| `DuplicatedUsernameValidator.java` | Import migration |
| `DuplicatedEmailConstraint.java` | Import migration |
| `DuplicatedUsernameConstraint.java` | Import migration |
| `DuplicatedEmailValidator.java` | Import migration |
| `NewArticleParam.java` | Import migration |
| `ArticleCommandService.java` | Import migration |
| `RegisterParam.java` | Import migration |
| `UpdateUserParam.java` | Import migration |
| `ArticleApi.java` | Import migration |
| `UsersApi.java` | Import migration |
| `ArticlesApi.java` | Import migration |
| `CurrentUserApi.java` | Import migration |
| `CommentsApi.java` | Import migration |
| `CustomizeExceptionHandler.java` | Import migration |
| `UserMutation.java` | Import migration |
| `GraphQLCustomizeExceptionHandler.java` | Import migration |

### Files That Should NOT Change

| File | Reason |
|------|--------|
| `DefaultJwtService.java` | Uses `javax.crypto.*` which is Java SE, not Jakarta EE |
| All files in `io.spring.core.*` | Domain layer has no framework dependencies |
| MyBatis mapper XML files | Should work with updated starter |
| GraphQL schema files | Schema format unchanged |

---

## Appendix B: Useful Resources

- [Spring Boot 3.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Spring Security 6.0 Migration Guide](https://docs.spring.io/spring-security/reference/migration/index.html)
- [Netflix DGS Spring Boot 3 Support](https://netflix.github.io/dgs/spring-boot-3/)
- [MyBatis Spring Boot Starter 3.0 Release Notes](https://github.com/mybatis/spring-boot-starter/releases)
- [Jakarta EE 9 Migration](https://jakarta.ee/resources/jakarta-ee-9-migration-guide/)
