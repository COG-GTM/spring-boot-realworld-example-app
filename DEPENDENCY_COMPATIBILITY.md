# Dependency Compatibility Matrix for Java 21 Migration

This document provides a comprehensive compatibility matrix for all dependencies in the spring-boot-realworld-example-app project, documenting their Java 21 compatibility status and required upgrades.

## Overview

The migration from Java 11 to Java 21 requires careful consideration of all dependencies. This document outlines the current versions, Java 21 compatible versions, and any breaking changes or migration notes for each dependency.

## Compatibility Matrix

### Core Framework Dependencies

| Dependency | Current Version | Java 21 Compatible Version | Status | Breaking Changes |
|------------|-----------------|---------------------------|--------|------------------|
| Spring Boot | 2.7.18 | 3.2.x or 3.3.x | Requires Upgrade | Major - Jakarta EE migration |
| Spring Framework | 5.3.x (managed) | 6.1.x (managed by Boot 3.x) | Requires Upgrade | Major - Jakarta EE migration |
| Spring Security | 5.8.x (managed) | 6.2.x (managed by Boot 3.x) | Requires Upgrade | Major - API changes |

### Build Plugins

| Plugin | Current Version | Java 21 Compatible Version | Status | Notes |
|--------|-----------------|---------------------------|--------|-------|
| Gradle | 8.5 | 8.5+ | Compatible | Already upgraded in Phase 1 |
| Spring Boot Gradle Plugin | 2.7.18 | 3.2.x or 3.3.x | Requires Upgrade | Must match Spring Boot version |
| Spring Dependency Management | 1.1.4 | 1.1.4+ | Compatible | Works with Spring Boot 3.x |
| Netflix DGS Codegen | 5.12.4 | 6.x or 7.x | Requires Upgrade | For Spring Boot 3 compatibility |
| Spotless | 6.25.0 | 6.25.0+ | Compatible | Already upgraded in Phase 1 |

### Persistence Dependencies

| Dependency | Current Version | Java 21 Compatible Version | Status | Breaking Changes |
|------------|-----------------|---------------------------|--------|------------------|
| MyBatis Spring Boot Starter | 2.2.2 | 3.0.3+ | Requires Upgrade | Minor - Package changes |
| MyBatis | 3.5.x (managed) | 3.5.14+ | Compatible | None expected |
| SQLite JDBC | 3.36.0.3 | 3.45.0.0+ | Recommended Upgrade | None - Java 21 compatible |
| Flyway Core | 8.x (managed) | 10.x (managed by Boot 3.x) | Requires Upgrade | Major - API changes |
| HikariCP | 4.x (managed) | 5.x (managed by Boot 3.x) | Requires Upgrade | Minor |

### GraphQL Dependencies

| Dependency | Current Version | Java 21 Compatible Version | Status | Breaking Changes |
|------------|-----------------|---------------------------|--------|------------------|
| Netflix DGS Spring Boot Starter | 5.5.1 | 7.x or 8.x | Requires Upgrade | Major - Spring Boot 3 required |
| graphql-java | 19.2 | 21.x or 22.x | Requires Upgrade | Minor - API changes |
| Apollo Federation | 2.1.0 (transitive) | 4.x | Requires Upgrade | Major - API changes |

### Security Dependencies

| Dependency | Current Version | Java 21 Compatible Version | Status | Breaking Changes |
|------------|-----------------|---------------------------|--------|------------------|
| JJWT API | 0.11.2 | 0.12.5+ | Recommended Upgrade | Minor - Deprecations |
| JJWT Impl | 0.11.2 | 0.12.5+ | Recommended Upgrade | Minor - Deprecations |
| JJWT Jackson | 0.11.2 | 0.12.5+ | Recommended Upgrade | Minor - Deprecations |
| Spring Security Test | 5.8.x (managed) | 6.2.x (managed by Boot 3.x) | Requires Upgrade | Major - API changes |

### Utility Dependencies

| Dependency | Current Version | Java 21 Compatible Version | Status | Breaking Changes |
|------------|-----------------|---------------------------|--------|------------------|
| Joda-Time | 2.10.13 | 2.12.5+ | Compatible | None - Consider java.time migration |
| Lombok | 1.18.x (managed) | 1.18.30+ | Compatible | None - Java 21 support added |

### Test Dependencies

| Dependency | Current Version | Java 21 Compatible Version | Status | Breaking Changes |
|------------|-----------------|---------------------------|--------|------------------|
| REST Assured | 4.5.1 | 5.4.0+ | Recommended Upgrade | Minor - API improvements |
| REST Assured JSON Path | 4.5.1 | 5.4.0+ | Recommended Upgrade | Minor |
| REST Assured XML Path | 4.5.1 | 5.4.0+ | Recommended Upgrade | Minor |
| REST Assured Spring Mock MVC | 4.5.1 | 5.4.0+ | Recommended Upgrade | Minor |
| JUnit 5 | 5.8.x (managed) | 5.10.x (managed by Boot 3.x) | Compatible | None |
| MyBatis Spring Boot Starter Test | 2.2.2 | 3.0.3+ | Requires Upgrade | Minor |

## Detailed Migration Notes

### Spring Boot 2.x to 3.x Migration (Phase 2)

The Spring Boot upgrade is the most significant change and requires:

1. **Jakarta EE Migration**: All `javax.*` packages must be changed to `jakarta.*`
   - `javax.persistence` → `jakarta.persistence`
   - `javax.validation` → `jakarta.validation`
   - `javax.servlet` → `jakarta.servlet`

2. **Minimum Java Version**: Spring Boot 3.x requires Java 17 minimum, Java 21 recommended

3. **Configuration Property Changes**: Some properties have been renamed or removed

4. **Security Configuration**: Spring Security 6.x has significant API changes
   - `WebSecurityConfigurerAdapter` is removed
   - Use component-based security configuration

### Netflix DGS Migration

The DGS framework upgrade requires:

1. **Version Compatibility**:
   - DGS 7.x+ is required for Spring Boot 3.x
   - DGS 8.x is the latest stable version

2. **Code Changes**:
   - Some annotation changes may be required
   - GraphQL schema handling may have minor changes

3. **Generated Code**: Re-run code generation after upgrade

### MyBatis Migration

MyBatis Spring Boot Starter 3.x changes:

1. **Package Changes**: Some internal packages have changed
2. **Configuration**: Minor configuration property changes
3. **Mapper Scanning**: May require annotation updates

### JJWT Migration

JJWT 0.12.x changes:

1. **Builder API**: Some builder methods have been deprecated
2. **Parser API**: New parser builder pattern
3. **Algorithm Constants**: Some constants renamed

### Joda-Time Consideration

While Joda-Time is compatible with Java 21, consider migrating to `java.time`:

1. **Benefits**: Native Java API, no external dependency
2. **Effort**: Medium - requires code changes throughout
3. **Recommendation**: Plan for Phase 4 or separate migration

## Recommended Upgrade Path

### Phase 2: Spring Boot 3.x Upgrade

```groovy
plugins {
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'java'
    id "com.netflix.dgs.codegen" version "6.0.0"
    id "com.diffplug.spotless" version "6.25.0"
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-hateoas'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    implementation 'com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter:7.6.0'
    implementation 'org.flywaydb:flyway-core'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
    implementation 'joda-time:joda-time:2.12.5'
    implementation 'org.xerial:sqlite-jdbc:3.45.0.0'

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
```

### Phase 3: Java 21 Upgrade

```groovy
sourceCompatibility = '21'
targetCompatibility = '21'
```

Update CI/CD to use Java 21:
```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: zulu
    java-version: '21'
```

## Alternative Libraries

If any dependency doesn't support Java 21, consider these alternatives:

| Original | Alternative | Notes |
|----------|-------------|-------|
| Joda-Time | java.time (built-in) | Native Java 8+ API |
| Netflix DGS | Spring for GraphQL | Official Spring project |
| JJWT | Nimbus JOSE+JWT | Alternative JWT library |

## Risk Assessment

| Dependency | Risk Level | Mitigation |
|------------|------------|------------|
| Spring Boot 3.x | High | Thorough testing, phased rollout |
| Netflix DGS | Medium | Test GraphQL endpoints extensively |
| MyBatis | Low | Minor configuration changes |
| JJWT | Low | API mostly compatible |
| SQLite JDBC | Low | Direct upgrade |
| Lombok | Low | Already Java 21 compatible |

## Testing Strategy

1. **Unit Tests**: Run full test suite after each dependency upgrade
2. **Integration Tests**: Test all API endpoints
3. **GraphQL Tests**: Verify all GraphQL queries and mutations
4. **Security Tests**: Verify authentication and authorization
5. **Performance Tests**: Compare performance before and after migration

## References

- [Spring Boot 3.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Netflix DGS Documentation](https://netflix.github.io/dgs/)
- [MyBatis Spring Boot Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [Java 21 Release Notes](https://openjdk.org/projects/jdk/21/)

## Known Security Vulnerabilities (Phase 1 Status)

### Fixed in Phase 1

The following security vulnerabilities were addressed in Phase 1 by upgrading dependencies:

| Vulnerability ID | Severity | Package | Fixed Version | Description |
|-----------------|----------|---------|---------------|-------------|
| SNYK-JAVA-COMGRAPHQLJAVA-7573314 | High | graphql-java | 19.11 | Allocation of Resources Without Limits or Throttling |
| SNYK-JAVA-COMGRAPHQLJAVA-5291199 | High | graphql-java | 19.11 | Stack-based Buffer Overflow |

### Remaining Vulnerabilities (Require Phase 2 - Spring Boot 3.x)

The following vulnerabilities require upgrading to Spring Boot 3.x and cannot be addressed in Phase 1:

#### Spring Framework Vulnerabilities (35 issues total)

| Vulnerability ID | Severity | Package | Current Version | Fix Requires |
|-----------------|----------|---------|-----------------|--------------|
| SNYK-JAVA-ORGSPRINGFRAMEWORKSECURITY-8309135 | Critical | spring-security-web | 5.7.11 | Spring Boot 3.x |
| SNYK-JAVA-ORGSPRINGFRAMEWORK-12008931 | High | spring-beans | 5.3.31 | Spring Boot 3.x |
| SNYK-JAVA-ORGSPRINGFRAMEWORK-12817817 | High | spring-core | 5.3.31 | Spring Boot 3.x |
| SNYK-JAVA-ORGSPRINGFRAMEWORK-6261586 | High | spring-web | 5.3.31 | Spring Boot 3.x |
| SNYK-JAVA-ORGSPRINGFRAMEWORK-6444790 | High | spring-web | 5.3.31 | Spring Boot 3.x |
| SNYK-JAVA-ORGSPRINGFRAMEWORK-7945490 | High | spring-webmvc | 5.3.31 | Spring Boot 3.x |
| SNYK-JAVA-ORGSPRINGFRAMEWORK-8230373 | High | spring-webmvc | 5.3.31 | Spring Boot 3.x |
| SNYK-JAVA-ORGSPRINGFRAMEWORKSECURITY-6457293 | High | spring-security-core | 5.7.11 | Spring Boot 3.x |

#### Tomcat Embed Vulnerabilities

| Vulnerability ID | Severity | Package | Current Version | Fix Requires |
|-----------------|----------|---------|-----------------|--------------|
| SNYK-JAVA-ORGAPACHETOMCATEMBED-13733966 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |
| SNYK-JAVA-ORGAPACHETOMCATEMBED-11799152 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |
| SNYK-JAVA-ORGAPACHETOMCATEMBED-10674391 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |
| SNYK-JAVA-ORGAPACHETOMCATEMBED-10676855 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |
| SNYK-JAVA-ORGAPACHETOMCATEMBED-10365122 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |
| SNYK-JAVA-ORGAPACHETOMCATEMBED-13746602 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |
| SNYK-JAVA-ORGAPACHETOMCATEMBED-9396739 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |
| SNYK-JAVA-ORGAPACHETOMCATEMBED-7430175 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |
| SNYK-JAVA-ORGAPACHETOMCATEMBED-8073090 | High | tomcat-embed-core | 9.0.83 | Spring Boot 3.x |

#### Other Vulnerabilities

| Vulnerability ID | Severity | Package | Current Version | Fix Requires |
|-----------------|----------|---------|-----------------|--------------|
| SNYK-JAVA-ORGAPACHECOMMONS-10734078 | High | commons-lang3 | 3.12.0 | REST Assured 5.x upgrade |

### Security Remediation Plan

1. **Phase 1 (Complete)**: Fixed graphql-java and DGS vulnerabilities
2. **Phase 2 (Spring Boot 3.x)**: Will address all Spring Framework, Spring Security, and Tomcat vulnerabilities
3. **Phase 3 (Java 21)**: No additional security changes expected

## Version History

| Version | Date | Description |
|---------|------|-------------|
| 1.0 | 2025-12-16 | Initial dependency compatibility matrix |
| 1.1 | 2025-12-16 | Added security vulnerabilities section with Phase 1 fixes and Phase 2 requirements |
