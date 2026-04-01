# Phase 0: Pre-Flight Analysis

## Migration Parameters

| Parameter | Value |
|-----------|-------|
| Target Spring Boot Version | 3.2.2 |
| Target JDK Version | 17 |
| Intermediate Spring Boot Version | 2.7.18 |
| Artifacts Directory | docs/spring-boot-migration/artifacts |
| Verification Log Directory | docs/spring-boot-migration/verification-logs |

---

## REQ-P0-1.1: Build Tool Configuration

| Property | Value |
|----------|-------|
| Build Tool | Gradle |
| Gradle Version | 7.4 |
| Root Configuration File | build.gradle |
| Wrapper Script | gradlew |
| Multi-module | No (single module project) |
| No settings.gradle | Confirmed - single module |

**Decision Logic:** Gradle files exist (build.gradle, gradlew), no pom.xml found. Result: **GRADLE**.

**Gradle Details:**
- Build time: 2022-02-08
- Kotlin: 1.5.31
- Groovy: 3.0.9
- JVM: 17.0.13 (OpenJDK)

---

## REQ-P0-1.2: Custom Build Settings

| Check | Result |
|-------|--------|
| init.gradle files in project root | Not found |
| gradle.properties (repository/proxy/credentials) | Not found |
| ~/.gradle/init.d/ | Not found |
| Custom repositories in build.gradle | mavenCentral() only |

**Conclusion:** No custom build settings detected. Standard Maven Central repository is used. No special settings flags needed for build commands.

---

## REQ-P0-2.1: Dependency Snapshot Summary

### Key Dependencies

| Dependency | Version | Scope | Managed by BOM? |
|-----------|---------|-------|-----------------|
| org.springframework.boot:spring-boot-starter-web | 2.6.3 | implementation | Yes (Spring Boot BOM) |
| org.springframework.boot:spring-boot-starter-validation | 2.6.3 | implementation | Yes |
| org.springframework.boot:spring-boot-starter-hateoas | 2.6.3 | implementation | Yes |
| org.springframework.boot:spring-boot-starter-security | 2.6.3 | implementation | Yes |
| org.mybatis.spring.boot:mybatis-spring-boot-starter | 2.2.2 | implementation | No |
| com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter | 4.9.21 | implementation | No |
| org.flywaydb:flyway-core | (BOM managed) | implementation | Yes |
| io.jsonwebtoken:jjwt-api | 0.11.2 | implementation | No |
| io.jsonwebtoken:jjwt-impl | 0.11.2 | runtimeOnly | No |
| io.jsonwebtoken:jjwt-jackson | 0.11.2 | runtimeOnly | No |
| joda-time:joda-time | 2.10.13 | implementation | No |
| org.xerial:sqlite-jdbc | 3.36.0.3 | implementation | No |
| org.projectlombok:lombok | (BOM managed) | compileOnly/annotationProcessor | Yes |
| io.rest-assured:rest-assured | 4.5.1 | testImplementation | No |
| io.rest-assured:spring-mock-mvc | 4.5.1 | testImplementation | No |
| org.springframework.security:spring-security-test | (BOM managed) | testImplementation | Yes |
| org.springframework.boot:spring-boot-starter-test | 2.6.3 | testImplementation | Yes |
| org.mybatis.spring.boot:mybatis-spring-boot-starter-test | 2.2.2 | testImplementation | No |

### Plugins

| Plugin | Version |
|--------|---------|
| org.springframework.boot | 2.6.3 |
| io.spring.dependency-management | 1.0.11.RELEASE |
| java | (Gradle built-in) |
| com.netflix.dgs.codegen | 5.0.6 |
| com.diffplug.spotless | 6.2.1 |

### Raw Output File Locations

- Compile classpath dependencies: `phase-0-dependency-tree-before.txt`
- Runtime classpath dependencies: `phase-0-dependency-list-before.txt`
- Spring Boot dependency insight: `phase-0-spring-boot-deps-before.txt`
- Full dependencies (effective-pom equivalent): `phase-0-effective-pom-before.xml`

---

## REQ-P0-3.1: Version Baseline

### Version Table

| Component | Current Version | Target Version |
|-----------|----------------|----------------|
| Spring Boot | 2.6.3 | 3.2.2 |
| JDK | 11 | 17 |
| Spring Framework | 5.3.x (via Boot 2.6.3) | 6.1.x (via Boot 3.2.2) |
| Spring Security | 5.6.x (via Boot 2.6.3) | 6.2.x (via Boot 3.2.2) |
| Hibernate | 5.6.x (via Boot 2.6.3) | 6.4.x (via Boot 3.2.2) |
| Gradle | 7.4 | 7.4+ (compatible) |

### Upgrade Path Assessment

- Current Spring Boot version: **2.6.3** (< 2.7.0)
- **Phase 2 intermediate upgrade to 2.7.18 will be REQUIRED** before migrating to 3.x
- Single module project - no cross-module version conflicts
- All Spring Boot starters are BOM-managed (no explicit versions)
- Several third-party dependencies have explicit versions that will need compatibility checking

### Major Migration Concerns

1. **javax to jakarta namespace migration** - Required for Spring Boot 3.x
2. **MyBatis version** - mybatis-spring-boot-starter 2.2.2 needs upgrade for Spring Boot 3.x compatibility
3. **Netflix DGS GraphQL** - Version 4.9.21 needs upgrade for Spring Boot 3.x
4. **SQLite JDBC** - May need version update for JDK 17 compatibility
5. **Rest Assured** - Version 4.5.1 may need upgrade for Jakarta namespace
6. **JJWT** - Version 0.11.2 may need upgrade
7. **Joda-Time** - Consider migration to java.time (optional)
8. **Spring Security** - WebSecurityConfigurerAdapter removed in Spring Security 6.x
9. **Flyway** - May need database-specific module for Spring Boot 3.x
