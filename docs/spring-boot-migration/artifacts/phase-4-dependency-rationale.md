# Phase 4: Dependency Compatibility Rationale

## Date: 2026-04-01
## Target Spring Boot: 3.2.2
## Target JDK: 17

## Dependencies Upgraded

### 1. MyBatis Spring Boot Starter
| Property | Value |
|----------|-------|
| Old Version | 2.2.2 |
| New Version | 3.0.3 |
| Reason | MyBatis Spring Boot Starter 2.x uses javax namespace; 3.x is required for Jakarta/Spring Boot 3.x |
| BOM Managed | No (explicit version) |
| JDK 17 Compatible | Yes |

### 2. Netflix DGS GraphQL Framework
| Property | Value |
|----------|-------|
| Old Version | graphql-dgs-spring-boot-starter 4.9.21 |
| New Version | graphql-dgs-spring-graphql-starter (via DGS BOM 8.2.0) |
| Reason | DGS 4.x is incompatible with Spring Boot 3.x; DGS 8.x uses Spring GraphQL and Jakarta namespace |
| BOM Managed | Yes (via graphql-dgs-platform-dependencies 8.2.0) |
| JDK 17 Compatible | Yes |
| Note | Starter artifact changed from `graphql-dgs-spring-boot-starter` to `graphql-dgs-spring-graphql-starter` |

### 3. DGS CodeGen Plugin
| Property | Value |
|----------|-------|
| Old Version | 5.0.6 |
| New Version | 6.0.3 |
| Reason | CodeGen 5.x generates code incompatible with DGS 8.x |
| BOM Managed | No (Gradle plugin) |
| JDK 17 Compatible | Yes |

### 4. JJWT (JSON Web Token)
| Property | Value |
|----------|-------|
| Old Version | 0.11.2 |
| New Version | 0.12.5 |
| Reason | 0.12.x has improved JDK 17 compatibility and API improvements |
| BOM Managed | No (explicit version) |
| JDK 17 Compatible | Yes |
| Note | API changes: `signWith(SignatureAlgorithm, key)` deprecated; use `signWith(key, algorithm)` |

### 5. Rest Assured
| Property | Value |
|----------|-------|
| Old Version | 4.5.1 |
| New Version | 5.4.0 |
| Reason | Rest Assured 4.x uses javax namespace; 5.x uses Jakarta namespace for Spring Mock MVC |
| BOM Managed | No (explicit version) |
| JDK 17 Compatible | Yes |

### 6. SQLite JDBC
| Property | Value |
|----------|-------|
| Old Version | 3.36.0.3 |
| New Version | 3.45.1.0 |
| Reason | Updated for JDK 17 compatibility and bug fixes |
| BOM Managed | No (explicit version) |
| JDK 17 Compatible | Yes |

### 7. Joda-Time
| Property | Value |
|----------|-------|
| Old Version | 2.10.13 |
| New Version | 2.12.7 |
| Reason | Latest stable release with JDK 17 compatibility improvements |
| BOM Managed | No (explicit version) |
| JDK 17 Compatible | Yes |
| Note | Consider migrating to java.time in future; not required for this migration |

### 8. MyBatis Spring Boot Starter Test
| Property | Value |
|----------|-------|
| Old Version | 2.2.2 |
| New Version | 3.0.3 |
| Reason | Must match main mybatis-spring-boot-starter version |
| BOM Managed | No (explicit version) |
| JDK 17 Compatible | Yes |

## Dependencies NOT Upgraded (BOM-Managed)

These dependencies have no explicit versions and are managed by Spring Boot 3.2.2 BOM:
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-hateoas`
- `spring-boot-starter-security`
- `flyway-core`
- `lombok`
- `spring-security-test`
- `spring-boot-starter-test`

## JDK Incompatible Dependencies Check (REQ-P4-1.1)

| Dependency | Found | Action |
|-----------|-------|--------|
| log4j:log4j:1.x | No | N/A |
| junit:junit:4.x | No | N/A (already using JUnit 5) |
| powermock | No | N/A |
| javax.xml.bind:jaxb-api | No | N/A |
| javax.annotation:javax.annotation-api | No | N/A |

No JDK-incompatible dependencies found.

## Summary

| Metric | Count |
|--------|-------|
| Dependencies upgraded | 8 |
| Dependencies retained (BOM-managed) | 8 |
| Escalations | 0 |
