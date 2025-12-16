# Java 11 to Java 21 Migration Plan

## Overview

This document catalogs the breaking changes and required updates for migrating the Spring Boot RealWorld example application from Java 11 to Java 21, including the upgrade from Spring Boot 2.6.3 to Spring Boot 3.x.

## Current State (Baseline)

| Component | Current Version |
|-----------|-----------------|
| Java | 11 |
| Spring Boot | 2.6.3 |
| Gradle | 7.4 |
| MyBatis Starter | 2.2.2 |
| Netflix DGS | 4.9.21 |
| JJWT | 0.11.2 |
| SQLite JDBC | 3.36.0.3 |
| RestAssured | 4.5.1 |
| DGS Codegen Plugin | 5.0.6 |
| Spotless Plugin | 6.2.1 |
| Spring Dependency Management Plugin | 1.0.11.RELEASE |

**Starting Commit Hash:** `ee17e31aafe733d98c4853c8b9a74d7f2f6c924a`

**Build Status:** All 68 tests passing (verified on feature/java21-migration branch)

## Target State

| Component | Target Version |
|-----------|----------------|
| Java | 21 |
| Spring Boot | 3.2.0 |
| Gradle | 8.5 |
| MyBatis Starter | 3.0.3 |
| Netflix DGS | 8.2.0 |
| JJWT | 0.12.3 |
| SQLite JDBC | 3.44.1.0 |
| RestAssured | 5.3.2 |
| DGS Codegen Plugin | 6.0.3 |
| Spotless Plugin | 6.22.0 |
| Spring Dependency Management Plugin | 1.1.4 |

---

## 1. Jakarta EE Migration (javax to jakarta)

Spring Boot 3 uses Jakarta EE 9+ instead of Java EE, which means all `javax.*` packages must be replaced with `jakarta.*` equivalents.

### 1.1 Validation API Changes

The following files use `javax.validation.*` and must be updated to `jakarta.validation.*`:

| File | Imports to Change |
|------|-------------------|
| `src/main/java/io/spring/application/user/DuplicatedEmailConstraint.java` | `javax.validation.Constraint`, `javax.validation.Payload` |
| `src/main/java/io/spring/application/user/RegisterParam.java` | `javax.validation.constraints.Email`, `javax.validation.constraints.NotBlank` |
| `src/main/java/io/spring/application/article/NewArticleParam.java` | `javax.validation.constraints.NotBlank` |
| `src/main/java/io/spring/application/user/UpdateUserParam.java` | `javax.validation.constraints.Email` |
| `src/main/java/io/spring/application/article/DuplicatedArticleValidator.java` | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `src/main/java/io/spring/application/user/UserService.java` | `javax.validation.Constraint`, `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext`, `javax.validation.Valid` |
| `src/main/java/io/spring/application/user/DuplicatedUsernameValidator.java` | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `src/main/java/io/spring/application/article/DuplicatedArticleConstraint.java` | `javax.validation.Constraint`, `javax.validation.Payload` |
| `src/main/java/io/spring/api/ArticleApi.java` | `javax.validation.Valid` |
| `src/main/java/io/spring/application/user/DuplicatedUsernameConstraint.java` | `javax.validation.Constraint`, `javax.validation.Payload` |
| `src/main/java/io/spring/application/user/DuplicatedEmailValidator.java` | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `src/main/java/io/spring/api/ArticlesApi.java` | `javax.validation.Valid` |
| `src/main/java/io/spring/api/CurrentUserApi.java` | `javax.validation.Valid` |
| `src/main/java/io/spring/api/CommentsApi.java` | `javax.validation.Valid`, `javax.validation.constraints.NotBlank` |
| `src/main/java/io/spring/api/UsersApi.java` | `javax.validation.Valid`, `javax.validation.constraints.Email`, `javax.validation.constraints.NotBlank` |
| `src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java` | `javax.validation.ConstraintViolation`, `javax.validation.ConstraintViolationException` |
| `src/main/java/io/spring/graphql/UserMutation.java` | `javax.validation.ConstraintViolationException` |
| `src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java` | `javax.validation.ConstraintViolation`, `javax.validation.ConstraintViolationException` |
| `src/main/java/io/spring/application/article/ArticleCommandService.java` | `javax.validation.Valid` |

### 1.2 Servlet API Changes

The following file uses `javax.servlet.*` and must be updated to `jakarta.servlet.*`:

| File | Imports to Change |
|------|-------------------|
| `src/main/java/io/spring/api/security/JwtTokenFilter.java` | `javax.servlet.FilterChain`, `javax.servlet.ServletException`, `javax.servlet.http.HttpServletRequest`, `javax.servlet.http.HttpServletResponse` |

### 1.3 Crypto API (No Change Required)

The following file uses `javax.crypto.*` which is part of the JDK and does NOT need to be changed:

| File | Imports (No Change) |
|------|---------------------|
| `src/main/java/io/spring/infrastructure/service/DefaultJwtService.java` | `javax.crypto.SecretKey`, `javax.crypto.spec.SecretKeySpec` |

---

## 2. Dependency Compatibility Updates

### 2.1 Core Dependencies

| Dependency | Current | Target | Notes |
|------------|---------|--------|-------|
| `org.mybatis.spring.boot:mybatis-spring-boot-starter` | 2.2.2 | 3.0.3 | Required for Spring Boot 3 compatibility |
| `com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter` | 4.9.21 | 8.2.0 | Major version upgrade with API changes |
| `io.jsonwebtoken:jjwt-api` | 0.11.2 | 0.12.3 | API changes in builder pattern |
| `io.jsonwebtoken:jjwt-impl` | 0.11.2 | 0.12.3 | Must match jjwt-api version |
| `io.jsonwebtoken:jjwt-jackson` | 0.11.2 | 0.12.3 | Must match jjwt-api version |
| `org.xerial:sqlite-jdbc` | 3.36.0.3 | 3.44.1.0 | Updated for Java 21 compatibility |
| `joda-time:joda-time` | 2.10.13 | 2.12.5 | Consider migrating to java.time API |

### 2.2 Test Dependencies

| Dependency | Current | Target | Notes |
|------------|---------|--------|-------|
| `io.rest-assured:rest-assured` | 4.5.1 | 5.3.2 | Required for Jakarta EE compatibility |
| `io.rest-assured:json-path` | 4.5.1 | 5.3.2 | Must match rest-assured version |
| `io.rest-assured:xml-path` | 4.5.1 | 5.3.2 | Must match rest-assured version |
| `io.rest-assured:spring-mock-mvc` | 4.5.1 | 5.3.2 | Must match rest-assured version |
| `org.mybatis.spring.boot:mybatis-spring-boot-starter-test` | 2.2.2 | 3.0.3 | Must match mybatis-spring-boot-starter |

### 2.3 Gradle Plugins

| Plugin | Current | Target | Notes |
|--------|---------|--------|-------|
| `org.springframework.boot` | 2.6.3 | 3.2.0 | Major version upgrade |
| `io.spring.dependency-management` | 1.0.11.RELEASE | 1.1.4 | Required for Spring Boot 3 |
| `com.netflix.dgs.codegen` | 5.0.6 | 6.0.3 | Required for DGS 8.x compatibility |
| `com.diffplug.spotless` | 6.2.1 | 6.22.0 | Required for Java 21 compatibility |

---

## 3. Configuration Changes

### 3.1 build.gradle Changes

```groovy
// BEFORE
plugins {
    id 'org.springframework.boot' version '2.6.3'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    id "com.netflix.dgs.codegen" version "5.0.6"
    id "com.diffplug.spotless" version "6.2.1"
}

version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'
targetCompatibility = '11'

// AFTER
plugins {
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'java'
    id "com.netflix.dgs.codegen" version "6.0.3"
    id "com.diffplug.spotless" version "6.22.0"
}

version = '0.0.1-SNAPSHOT'
sourceCompatibility = '21'
targetCompatibility = '21'
```

### 3.2 gradle-wrapper.properties Changes

```properties
# BEFORE
distributionUrl=https\://services.gradle.org/distributions/gradle-7.4-bin.zip

# AFTER
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

### 3.3 CI/CD Configuration (.github/workflows/gradle.yml)

```yaml
# BEFORE
- name: Set up JDK 11
  uses: actions/setup-java@v2
  with:
    distribution: zulu
    java-version: '11'

# AFTER
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: '21'
```

Also update `actions/checkout` and `actions/cache` to v4 for compatibility.

---

## 4. Spring Boot 3.x API Changes

### 4.1 Spring Security Changes

Spring Security 6.x (bundled with Spring Boot 3) has significant API changes:

1. **WebSecurityConfigurerAdapter is removed** - Must use component-based security configuration
2. **antMatchers() is replaced with requestMatchers()** - Update security configuration
3. **authorizeRequests() is replaced with authorizeHttpRequests()** - Update method calls

The `WebSecurityConfig.java` file will need to be refactored to use the new lambda-based DSL.

### 4.2 Spring MVC Changes

1. **Trailing slash matching disabled by default** - May affect URL routing
2. **PathPatternParser is the default** - AntPathMatcher behavior changes

### 4.3 Hibernate/JPA Changes (if applicable)

While this project uses MyBatis, be aware that Hibernate 6.x has significant changes if JPA is added later.

---

## 5. Netflix DGS Framework Changes

### 5.1 Package Changes

DGS 8.x has moved to Spring Boot 3 and Jakarta EE:
- GraphQL Java version upgraded
- Some deprecated APIs removed
- DataFetcher interface changes

### 5.2 Code Generation Changes

The DGS codegen plugin 6.x generates code compatible with Jakarta EE and the new DGS APIs.

---

## 6. Risk Assessment

### 6.1 High Risk Areas

| Area | Risk Level | Description | Mitigation |
|------|------------|-------------|------------|
| Jakarta EE Migration | HIGH | 19 files need javax to jakarta changes | Automated find/replace with verification |
| Spring Security | HIGH | Complete API rewrite required | Careful refactoring with test coverage |
| Netflix DGS | HIGH | Major version upgrade with breaking changes | Review DGS migration guide, test GraphQL endpoints |
| JJWT API | MEDIUM | Builder pattern changes | Update DefaultJwtService.java |

### 6.2 Medium Risk Areas

| Area | Risk Level | Description | Mitigation |
|------|------------|-------------|------------|
| MyBatis | MEDIUM | Version 3.x has some API changes | Review MyBatis Spring Boot 3 migration guide |
| RestAssured | MEDIUM | Test framework changes | Update test imports and verify all tests pass |
| Gradle | MEDIUM | Gradle 8.x deprecation warnings | Address deprecated features |

### 6.3 Low Risk Areas

| Area | Risk Level | Description | Mitigation |
|------|------------|-------------|------------|
| SQLite JDBC | LOW | Minor version update | Should be transparent |
| Joda-Time | LOW | Minor version update | Consider future migration to java.time |
| Spotless | LOW | Plugin update for Java 21 | Should work with updated version |

---

## 7. Migration Phases

### Phase 1: Preparation and Planning (Current)
- Create migration branch
- Document breaking changes
- Verify current build passes
- Create this migration plan
- Fix CI workflow deprecated GitHub Actions (actions/checkout, actions/setup-java, actions/cache upgraded from v2 to v4)

**Note:** The CI workflow was failing due to deprecated `actions/cache@v2` which GitHub has disabled. This was fixed as part of Phase 1 to ensure CI can run for subsequent migration phases. The Java version remains at 11 in CI for now.

### Phase 2: Core Build System Upgrade
- Update Gradle wrapper to 8.5
- Update Spring Boot to 3.2.0
- Update dependency management plugin
- Update Java version to 21
- Update CI/CD configuration

### Phase 3: Jakarta EE Migration
- Replace all javax.validation imports with jakarta.validation
- Replace all javax.servlet imports with jakarta.servlet
- Verify compilation

### Phase 4: Dependency Updates
- Update MyBatis to 3.0.3
- Update Netflix DGS to 8.2.0
- Update JJWT to 0.12.3
- Update RestAssured to 5.3.2
- Update other dependencies

### Phase 5: Spring Security Refactoring
- Refactor WebSecurityConfig to use new DSL
- Update security matchers
- Test authentication/authorization

### Phase 6: Testing and Validation
- Run all unit tests
- Run all integration tests
- Test GraphQL endpoints
- Test REST endpoints
- Performance testing

### Phase 7: Documentation and Cleanup
- Update README with new requirements
- Remove deprecated code
- Final code review

---

## 8. Rollback Plan

If critical issues are discovered during migration:

1. The original code is preserved on the `master` branch
2. Each phase should be committed separately for easy rollback
3. CI/CD pipeline will catch regressions early
4. Feature flags can be used for gradual rollout if needed

---

## 9. References

- [Spring Boot 3.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Spring Security 6.0 Migration Guide](https://docs.spring.io/spring-security/reference/migration/index.html)
- [Netflix DGS Migration Guide](https://netflix.github.io/dgs/migration/)
- [MyBatis Spring Boot Starter 3.0 Release Notes](https://github.com/mybatis/spring-boot-starter/releases)
- [JJWT 0.12.0 Release Notes](https://github.com/jwtk/jjwt/releases)

---

## 10. Appendix: Files Requiring Changes

### A. Java Files with javax Imports (19 files)

1. `src/main/java/io/spring/api/ArticleApi.java`
2. `src/main/java/io/spring/api/ArticlesApi.java`
3. `src/main/java/io/spring/api/CommentsApi.java`
4. `src/main/java/io/spring/api/CurrentUserApi.java`
5. `src/main/java/io/spring/api/UsersApi.java`
6. `src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java`
7. `src/main/java/io/spring/api/security/JwtTokenFilter.java`
8. `src/main/java/io/spring/application/article/ArticleCommandService.java`
9. `src/main/java/io/spring/application/article/DuplicatedArticleConstraint.java`
10. `src/main/java/io/spring/application/article/DuplicatedArticleValidator.java`
11. `src/main/java/io/spring/application/article/NewArticleParam.java`
12. `src/main/java/io/spring/application/user/DuplicatedEmailConstraint.java`
13. `src/main/java/io/spring/application/user/DuplicatedEmailValidator.java`
14. `src/main/java/io/spring/application/user/DuplicatedUsernameConstraint.java`
15. `src/main/java/io/spring/application/user/DuplicatedUsernameValidator.java`
16. `src/main/java/io/spring/application/user/RegisterParam.java`
17. `src/main/java/io/spring/application/user/UpdateUserParam.java`
18. `src/main/java/io/spring/application/user/UserService.java`
19. `src/main/java/io/spring/graphql/UserMutation.java`
20. `src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java`

### B. Configuration Files

1. `build.gradle`
2. `gradle/wrapper/gradle-wrapper.properties`
3. `.github/workflows/gradle.yml`

---

*Document created: December 16, 2025*
*Migration Branch: feature/java21-migration*
*Starting Commit: ee17e31aafe733d98c4853c8b9a74d7f2f6c924a*
