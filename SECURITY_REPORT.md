# Security Vulnerability Report — `spring-boot-realworld-example-app`

**Repository:** `ankehao-demo/spring-boot-realworld-example-app`
**Branch / ref:** `master` (commit `1b03279e`)
**Date:** 2026-06-25
**Engagement type:** **Detection & reporting only** — no application source, build, or configuration code was modified, and no remediation changes were made.

This report consolidates the output of three scan domains run as parallel security scans:

| Domain | Tooling | Intermediate report |
|---|---|---|
| Backend SCA (Gradle / Java dependencies) | Trivy 0.71.2 (`fs --scanners vuln` over a resolved Gradle lockfile) | `security-reports/backend-sca.md` |
| SAST + secrets | Semgrep 1.168.0 (`p/security-audit`, `p/owasp-top-ten`, `p/secrets`, `p/java`, `--config=auto`), gitleaks 8.18.4 | `security-reports/sast-secrets.md` |
| Frontend SCA (npm) | **N/A** | — |

> **Frontend scan — Not Applicable.** The task specified a `frontend/` directory (npm: `axios`, `vite`, `eslint`, …). There is **no `frontend/` directory and no `package.json` anywhere on `master`** — this repository is backend-only (Gradle/Java). The frontend scan was therefore skipped (confirmed with the requester). `git ls-tree -r master | grep -i -E 'frontend|package.json'` returns nothing.

---

## Executive summary

| Severity | Dependency CVEs (SCA) | SAST / Secrets | **Total** |
|---|---|---|---|
| Critical | 16 | 1 | **17** |
| High | 53 | 3 | **56** |
| Medium | 42 | 1 | **43** |
| Low | 15 | 1 | **16** |
| Informational | 0 | 1 (positive) | **1** |
| **Total** | **126** | **7** | **133** |

**Headline issues**

1. **Hardcoded JWT signing secret** committed to `src/main/resources/application.properties:9` — anyone with repo/history access can forge valid auth tokens for any user. *(Critical)*
2. **End-of-life Spring Boot 2.6.3** drags in vulnerable transitive versions of Spring Framework (5.3.15 — incl. **Spring4Shell CVE-2022-22965**), Spring Security (5.6.1 — multiple auth-bypass CVEs), Tomcat (9.0.56 — RCE/request-smuggling), Jackson, SnakeYAML and Logback. A single Spring Boot upgrade clears the large majority of the 126 dependency CVEs. *(16 Critical / 53 High among them)*
3. **CORS allow-all origins** and **globally disabled CSRF** in `WebSecurityConfig.java`, plus **user-enumeration / non-constant-time login** in both the REST and GraphQL login paths. *(High / Medium)*

**Positive finding:** MyBatis mappers under `src/main/resources/mapper/` use only parameterized `#{...}` bindings — **no `${...}` string substitution anywhere** — so no SQL-injection exposure was detected.

---

# Findings grouped by severity

## CRITICAL (17)

### SEC-1 (Critical) — Hardcoded JWT signing secret committed to source
| Field | Value |
|---|---|
| Type | Secret / hardcoded credential (gitleaks `generic-api-key`) |
| Location | `src/main/resources/application.properties:9` (`jwt.secret`) |
| Value | redacted — `nRvy…6wA`, ~88 chars, entropy 5.46 (full value not reproduced) |
| Impact | The HMAC signing key for all JWTs (`DefaultJwtService`). Anyone with repo/history read access can forge valid tokens for any `sub`, fully bypassing authentication. Also exposed throughout git history. |
| Remediation | Externalize to env/secret manager (`jwt.secret=${JWT_SECRET}`); **rotate immediately** (assume compromised); purge from git history (BFG / `git filter-repo`); use a per-environment random ≥256-bit key. |

### Dependency CVEs — CRITICAL (16)
| Dependency | Current | CVE | Fixed Version | Note |
|---|---|---|---|---|
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-24813 | 9.0.99 / 10.1.35 / 11.0.3 | Potential RCE / info disclosure via partial PUT |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-41293 | 9.0.118 / 10.1.55 / 11.0.22 | HTTP/2 request headers not validated |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-43512 | 9.0.118 / 10.1.55 / 11.0.22 | Auth bypass via digest authentication |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-43515 | 9.0.118 / 10.1.55 / 11.0.22 | Improper authorization allows security bypass |
| org.springframework.boot:spring-boot-starter-web | 2.6.3 | CVE-2022-22965 | 2.5.12 / 2.6.6 | **Spring4Shell** — RCE via data binding on JDK 9+ |
| org.springframework.security:spring-security-config | 5.6.1 | CVE-2023-34034 | 5.6.12 / 5.7.10 / 5.8.5 / 6.0.5 / 6.1.2 | WebFlux path wildcard security bypass |
| org.springframework.security:spring-security-core | 5.6.1 | CVE-2022-22978 | 5.4.11 / 5.5.7 / 5.6.4 | Authorization bypass in RegexRequestMatcher |
| org.springframework.security:spring-security-core | 5.6.1 | CVE-2022-31692 | 5.6.9 / 5.7.5 | Authz bypass via forward/include dispatcher types |
| org.springframework.security:spring-security-web | 5.6.1 | CVE-2022-22978 | 5.4.11 / 5.5.7 / 5.6.4 | Authorization bypass in RegexRequestMatcher |
| org.springframework.security:spring-security-web | 5.6.1 | CVE-2024-38821 | 5.7.13 / 5.8.15 / 6.0.13 / 6.1.11 / 6.2.7 / 6.3.4 | Authz bypass of static resources (WebFlux) |
| org.springframework.security:spring-security-web | 5.6.1 | CVE-2026-22732 | 6.5.9 / 7.0.4 | Security policy bypass / info disclosure (unwritten headers) |
| org.springframework:spring-beans | 5.3.15 | CVE-2022-22965 | 5.2.20.RELEASE / 5.3.18 | **Spring4Shell** — RCE via data binding |
| org.springframework:spring-web | 5.3.15 | CVE-2016-1000027 | 6.0.0 | Untrusted Java deserialization (HttpInvokerServiceExporter) |
| org.springframework:spring-webflux | 5.3.15 | CVE-2022-22965 | 5.2.20.RELEASE / 5.3.18 | **Spring4Shell** — RCE via data binding |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2022-22965 | 5.2.20.RELEASE / 5.3.18 | **Spring4Shell** — RCE via data binding |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2023-20860 | 5.3.26 / 6.0.7 | Security bypass with un-prefixed double wildcard |

---

## HIGH (56)

### SEC-2 (High) — CORS misconfiguration: allow-all origins
| Field | Value |
|---|---|
| Location | `src/main/java/io/spring/api/security/WebSecurityConfig.java:70` |
| Code | `configuration.setAllowedOrigins(asList("*"));` |
| Impact | Every origin permitted for `/**`. `Authorization` header allowed cross-origin; a malicious site can drive authenticated requests if it can obtain/replay a token. (`setAllowCredentials(false)` on line 75 limits cookie theft.) |
| Remediation | Replace `*` with an explicit allowlist (or tightly scoped `setAllowedOriginPatterns`); restrict methods/headers to what is required. |

### SEC-3 (High) — CSRF protection globally disabled
| Field | Value |
|---|---|
| Location | `src/main/java/io/spring/api/security/WebSecurityConfig.java:38` (`.disable()` on line 39) |
| Code | `http.csrf().disable()` |
| Detector | Semgrep `java.spring.security.audit.spring-csrf-disabled` |
| Impact | CSRF disabled application-wide. Largely defensible for a stateless `Authorization`-header JWT API (it does set `SessionCreationPolicy.STATELESS`), but becomes exploitable if cookie/session auth or state-changing form endpoints are ever added. |
| Remediation | Document the stateless rationale; prefer disabling CSRF narrowly rather than globally; re-enable (e.g. `CookieCsrfTokenRepository`) if cookie auth is introduced. |

### SEC-4 (High) — User enumeration / non-constant-time login (REST)
| Field | Value |
|---|---|
| Location | `src/main/java/io/spring/api/UsersApi.java:49-57` |
| Impact | `passwordEncoder.matches(...)` (expensive BCrypt) is short-circuited and runs **only when the email exists**; a non-existent email returns measurably faster, enabling account enumeration via a timing side-channel (error messages are identical). |
| Remediation | Perform a dummy BCrypt verification against a fixed hash when the user is not found so timing is constant; keep a single generic failure response; add rate limiting / lockout. |

### Dependency CVEs — HIGH (53)
| Dependency | Current | CVE | Fixed Version | Note |
|---|---|---|---|---|
| ch.qos.logback:logback-classic | 1.2.10 | CVE-2023-6378 | 1.2.13 / 1.3.12 / 1.4.12 | Serialization vuln in logback receiver |
| ch.qos.logback:logback-core | 1.2.10 | CVE-2023-6378 | 1.2.13 / 1.3.12 / 1.4.12 | Serialization vuln in logback receiver |
| com.fasterxml.jackson.core:jackson-core | 2.13.1 | CVE-2025-52999 | 2.15.0 | Potential StackoverflowError |
| com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2020-36518 | 2.12.6.1 / 2.13.2.1 | DoS via deep nested objects |
| com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2022-42003 | 2.12.7.1 / 2.13.4.2 | Deep wrapper array nesting (UNWRAP_SINGLE_VALUE_ARRAYS) |
| com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2022-42004 | 2.12.7.1 / 2.13.4 | Use of deeply nested arrays |
| com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-54512 | 2.18.8 / 2.21.4 / 3.1.4 | data-binding vuln |
| com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-54513 | 2.18.8 / 2.21.4 / 3.1.4 | Security bypass allows arbitrary code execution |
| com.google.protobuf:protobuf-java | 3.9.0 | CVE-2021-22569 | 3.16.1 / 3.18.2 / 3.19.2 | DoS parsing binary data |
| com.google.protobuf:protobuf-java | 3.9.0 | CVE-2022-3509 | 3.16.3 / 3.19.6 / 3.20.3 / 3.21.7 | TextFormat parsing DoS |
| com.google.protobuf:protobuf-java | 3.9.0 | CVE-2022-3510 | 3.16.3 / 3.19.6 / 3.20.3 / 3.21.7 | Message-type extensions parsing DoS |
| com.google.protobuf:protobuf-java | 3.9.0 | CVE-2024-7254 | 3.25.5 / 4.27.5 / 4.28.2 | StackOverflow in Protocol Buffers |
| com.graphql-java:graphql-java | 17.3 | CVE-2022-37734 | 17.4 / 18.3 | DoS by malicious query |
| com.graphql-java:graphql-java | 17.3 | CVE-2023-28867 | 17.5 / 18.4 / 19.4 / 20.1 | Stack consumption from crafted query |
| com.graphql-java:graphql-java | 17.3 | CVE-2024-40094 | 19.11 / 20.9 / 21.5 | Resource allocation without limits |
| net.minidev:json-smart | 2.4.7 | CVE-2023-1370 | 2.4.9 | Uncontrolled resource consumption |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2022-42252 | 8.5.83 / 9.0.68 / 10.0.27 / 10.1.1 | Request smuggling |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2022-45143 | 8.5.84 / 9.0.69 / 10.1.2 | JsonErrorReportValve injection |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-24998 | 8.5.88 / 9.0.71 / 10.1.5 / 11.0.0-M5 | FileUpload DoS (excessive parts) |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-46589 | 8.5.96 / 9.0.83 / 10.1.16 / 11.0.0-M11 | Request smuggling via malformed trailer headers |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2024-34750 | 9.0.90 / 10.1.25 / 11.0.0-M21 | Improper handling of exceptional conditions |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2024-50379 | 9.0.98 / 10.1.34 / 11.0.2 | RCE via TOCTOU in JSP compilation |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2024-56337 | 9.0.98 / 10.1.34 / 11.0.2 | Incomplete fix for CVE-2024-50379 (JSP RCE) |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-48988 | 9.0.106 / 10.1.42 / 11.0.8 | DoS in multipart upload |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-48989 | 9.0.108 / 10.1.44 / 11.0.10 | HTTP/2 "MadeYouReset" DoS |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-52520 | 9.0.107 / 10.1.43 / 11.0.9 | DoS |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-53506 | 9.0.107 / 10.1.43 / 11.0.9 | DoS |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-55752 | 9.0.109 / 10.1.45 / 11.0.11 | Directory traversal via rewrite, possible RCE |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-24880 | 9.0.116 / 10.1.52 / 11.0.20 | Request/response smuggling via invalid chunk extension |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-34483 | 9.0.116 / 10.1.54 / 11.0.21 | Info disclosure (JsonAccessLogValve encoding) |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-41284 | 9.0.118 / 10.1.55 / 11.0.22 | Resource allocation without limits |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-42498 | 9.0.118 / 10.1.55 / 11.0.22 | Info disclosure (auth header during WebSocket auth) |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-43513 | 9.0.118 / 10.1.55 / 11.0.22 | Case-sensitivity flaw in LockOutRealm |
| org.assertj:assertj-core *(test-scope)* | 3.21.0 | CVE-2026-24400 | 3.27.7 | XXE info disclosure / DoS |
| org.springframework.boot:spring-boot | 2.6.3 | CVE-2025-22235 | 3.3.11 / 3.4.5 | EndpointRequest.to() wrong matcher |
| org.springframework.boot:spring-boot | 2.6.3 | CVE-2026-40973 | 3.5.14 / 4.0.6 | RCE / session hijack via predictable temp dir |
| org.springframework.boot:spring-boot-autoconfigure | 2.6.3 | CVE-2023-20883 | 2.5.15 / 2.6.15 / 2.7.12 / 3.0.7 | Welcome Page DoS |
| org.springframework.security:spring-security-core | 5.6.1 | CVE-2024-22257 | 5.7.12 / 5.8.11 / 6.1.8 / 6.2.3 | Broken access control (AuthenticatedVoter) |
| org.springframework.security:spring-security-crypto | 5.6.1 | CVE-2025-22228 | 5.7.16 / 5.8.18 / 6.0.16 / 6.1.14 / 6.2.10 / 6.3.8 / 6.4.4 | BCrypt does not enforce max password length |
| org.springframework:spring-beans | 5.3.15 | CVE-2022-22970 | 5.2.22.RELEASE / 5.3.20 | DoS via data binding to multipartFile/servlet part |
| org.springframework:spring-context | 5.3.15 | CVE-2022-22968 | 5.2.21.RELEASE / 5.3.19 | Data binding rules vulnerability |
| org.springframework:spring-core | 5.3.15 | CVE-2025-41249 | 6.2.11 | Annotation detection vulnerability |
| org.springframework:spring-expression | 5.3.15 | CVE-2023-20863 | 5.2.24.RELEASE / 5.3.27 / 6.0.8 | SpEL DoS |
| org.springframework:spring-web | 5.3.15 | CVE-2024-22243 | 5.3.32 / 6.0.17 / 6.1.4 | URL parsing with host validation |
| org.springframework:spring-web | 5.3.15 | CVE-2024-22259 | 5.3.33 / 6.0.18 / 6.1.5 | URL parsing with host validation |
| org.springframework:spring-web | 5.3.15 | CVE-2024-22262 | 5.3.34 / 6.0.19 / 6.1.6 | URL parsing with host validation |
| org.springframework:spring-webflux | 5.3.15 | CVE-2024-38816 | 6.1.13 | Path traversal (RouterFunctions + FileSystemResource) |
| org.springframework:spring-webflux | 5.3.15 | CVE-2024-38819 | 6.1.14 | Path traversal in functional web frameworks |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2024-38816 | 6.1.13 | Path traversal (RouterFunctions + FileSystemResource) |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2024-38819 | 6.1.14 | Path traversal in functional web frameworks |
| org.xerial:sqlite-jdbc | 3.36.0.3 | CVE-2023-32697 | 3.41.2.2 | **RCE when JDBC URL is attacker-controlled** |
| org.yaml:snakeyaml | 1.29 | CVE-2022-1471 | 2.0 | Constructor deserialization RCE |
| org.yaml:snakeyaml | 1.29 | CVE-2022-25857 | 1.31 | DoS (missing nested-depth limit) |

---

## MEDIUM (43)

### SEC-5 (Medium) — User enumeration / non-constant-time login (GraphQL)
| Field | Value |
|---|---|
| Location | `src/main/java/io/spring/graphql/UserMutation.java:58-66` |
| Impact | Same short-circuit pattern as SEC-4, via the GraphQL `login` mutation: `encryptService.matches(...)` runs only when the email exists, producing a user-enumeration timing side-channel. |
| Remediation | Same as SEC-4 — constant-time dummy hash when the user is missing; uniform error; rate limiting. Ideally centralize auth so REST and GraphQL share one hardened implementation. |

### Dependency CVEs — MEDIUM (42)
| Dependency | Current | CVE | Fixed Version | Note |
|---|---|---|---|---|
| ch.qos.logback:logback-core | 1.2.10 | CVE-2024-12798 | 1.3.15 / 1.5.13 | ACE via JaninoEventEvaluator |
| ch.qos.logback:logback-core | 1.2.10 | CVE-2025-11226 | 1.3.16 / 1.5.19 | Conditional arbitrary code execution |
| com.fasterxml.jackson.core:jackson-core | 2.13.1 | GHSA-72hv-8253-57qq | 2.18.6 / 2.21.1 | Number-length constraint bypass → DoS |
| com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-50193 | 2.14.0 | data-binding vuln |
| com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-54514 | 2.18.8 / 2.21.4 / 3.1.4 | data-binding vuln |
| com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-54515 | 2.18.9 / 2.21.5 / 3.1.4 | data-binding vuln |
| com.google.guava:guava | 30.0-jre | CVE-2023-2976 | 32.0.0-android | Insecure temp directory creation |
| com.google.protobuf:protobuf-java | 3.9.0 | CVE-2022-3171 | 3.16.3 / 3.19.6 / 3.20.3 / 3.21.7 | Parser timeout DoS |
| com.jayway.jsonpath:json-path | 2.6.0 | CVE-2023-51074 | 2.9.0 | Stack buffer overflow in Criteria.parse |
| org.apache.commons:commons-lang3 | 3.12.0 | CVE-2025-48924 | 3.18.0 | Uncontrolled recursion |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-41080 | 8.5.93 / 9.0.80 / 10.1.13 / 11.0.0-M11 | Open redirect in FORM auth |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-42795 | 8.5.94 / 9.0.81 / 10.1.14 / 11.0.0-M12 | Info leak via improper object recycling |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-44487 | 8.5.94 / 9.0.81 / 10.1.14 / 11.0.0-M12 | HTTP/2 Rapid Reset DDoS |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-45648 | 8.5.94 / 9.0.81 / 10.1.14 / 11.0.0-M12 | Request smuggling via trailer headers |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2024-24549 | 8.5.99 / 9.0.86 / 10.1.19 / 11.0.0-M17 | HTTP/2 header handling DoS |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-49124 | 9.0.106 / 10.1.42 / 11.0.8 | Untrusted search path (Windows installer) |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-49125 | 9.0.106 / 10.1.42 / 11.0.8 | Security constraint bypass (pre/post-resources) |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-66614 | 9.0.113 / 10.1.50 / 11.0.15 | Client cert verification bypass |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-25854 | 9.0.116 / 10.1.53 / 11.0.20 | Open redirect via LoadBalancerDrainingValve |
| org.apache.tomcat.embed:tomcat-embed-websocket | 9.0.56 | CVE-2024-23672 | 8.5.99 / 9.0.86 / 10.1.19 / 11.0.0-M17 | WebSocket DoS (incomplete closing handshake) |
| org.springframework.hateoas:spring-hateoas | 1.4.1 | CVE-2023-34036 | 1.5.5 / 2.0.5 / 2.1.1 | Improper neutralization of HTTP headers |
| org.springframework.security:spring-security-core | 5.6.1 | CVE-2022-22976 | 5.5.7 / 5.6.4 | BCrypt skips salt rounds at work factor 31 |
| org.springframework.security:spring-security-core | 5.6.1 | CVE-2024-38827 | 5.7.14 / 5.8.16 / 6.0.14 / 6.1.12 / 6.2.8 / 6.3.5 | Authz bypass for case-sensitive comparisons |
| org.springframework:spring-context | 5.3.15 | CVE-2024-38820 | 6.1.14 | DataBinder disallowedFields case-sensitivity |
| org.springframework:spring-expression | 5.3.15 | CVE-2022-22950 | 5.2.20.RELEASE / 5.3.17 | SpEL DoS |
| org.springframework:spring-expression | 5.3.15 | CVE-2023-20861 | 5.2.23.RELEASE / 5.3.26 / 6.0.7 | SpEL DoS |
| org.springframework:spring-expression | 5.3.15 | CVE-2024-38808 | 5.3.39 | SpEL DoS |
| org.springframework:spring-messaging | 5.3.15 | CVE-2022-22971 | 5.2.22.RELEASE / 5.3.20 | DoS with STOMP over WebSocket |
| org.springframework:spring-web | 5.3.15 | CVE-2024-38809 | 5.3.38 / 6.0.23 / 6.1.12 | DoS via conditional HTTP request |
| org.springframework:spring-web | 5.3.15 | CVE-2024-38820 | 6.1.14 | DataBinder disallowedFields case-sensitivity |
| org.springframework:spring-webflux | 5.3.15 | CVE-2026-22737 | 6.2.17 / 7.0.6 | Info disclosure via scripting-engine template views |
| org.springframework:spring-webflux | 5.3.15 | CVE-2026-22745 | 6.2.18 / 7.0.7 | DoS via slow static resource resolution (Windows) |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2024-38828 | 5.3.42 | DoS via byte[] controller parameter |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2025-41242 | 6.2.10 | MVC path traversal |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2026-22737 | 6.2.17 / 7.0.6 | Info disclosure via scripting-engine template views |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2026-22745 | 6.2.18 / 7.0.7 | DoS via slow static resource resolution (Windows) |
| org.springframework:spring-websocket | 5.3.15 | CVE-2025-41254 | 6.2.12 | STOMP CSRF vulnerability |
| org.yaml:snakeyaml | 1.29 | CVE-2022-38749 | 1.31 | Uncaught exception (Composer) |
| org.yaml:snakeyaml | 1.29 | CVE-2022-38750 | 1.31 | Uncaught exception (BaseConstructor) |
| org.yaml:snakeyaml | 1.29 | CVE-2022-38751 | 1.31 | Uncaught exception (regex Pattern) |
| org.yaml:snakeyaml | 1.29 | CVE-2022-38752 | 1.32 | Uncaught exception (ArrayList.hashCode) |
| org.yaml:snakeyaml | 1.29 | CVE-2022-41854 | 1.32 | DoS via stack overflow |

---

## LOW (16)

### SEC-6 (Low) — Hardcoded JWT + dummy signing key in test sources
| Field | Value |
|---|---|
| Location | `src/test/java/io/spring/infrastructure/service/DefaultJwtServiceTest.java:16` (dummy key), `:38` (sample/expired JWT) |
| Detector | gitleaks (`jwt`) |
| Impact | Low — test fixtures (throwaway signing key and an already-expired token used to assert rejection), not production credentials. Flagged for completeness. |
| Remediation | Acceptable as test fixtures; optionally generate keys/tokens dynamically in test setup. |

### Dependency CVEs — LOW (15)
| Dependency | Current | CVE | Fixed Version | Note |
|---|---|---|---|---|
| ch.qos.logback:logback-core | 1.2.10 | CVE-2024-12801 | 1.3.15 / 1.5.13 | SSRF in SaxEventRecorder |
| ch.qos.logback:logback-core | 1.2.10 | CVE-2026-1225 | 1.5.25 | Malicious logback.xml instantiates arbitrary classes |
| com.google.guava:guava | 30.0-jre | CVE-2020-8908 | 32.0.0-android | Local info disclosure via temp dir permissions |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-46701 | 9.0.105 / 10.1.41 / 11.0.7 | Security constraint bypass for CGI scripts |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-55754 | 9.0.109 / 10.1.45 / 11.0.11 | Console manipulation (tomcat-juli) |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-61795 | 9.0.110 / 10.1.47 / 11.0.12 | Denial of service |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-24733 | 9.0.113 / 10.1.50 / 11.0.15 | Security constraint bypass with HTTP/0.9 |
| org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-43514 | 9.0.118 / 10.1.55 / 11.0.22 | Info disclosure via AJP secret timing |
| org.springframework:spring-context | 5.3.15 | CVE-2025-22233 | 6.1.20 / 6.2.7 | Locale-independent lowercase conversion follow-up |
| org.springframework:spring-webflux | 5.3.15 | CVE-2026-22735 | 6.2.17 / 7.0.6 | Stream corruption via SSE |
| org.springframework:spring-webflux | 5.3.15 | CVE-2026-22740 | 6.2.18 / 7.0.7 | DoS via temp file accumulation |
| org.springframework:spring-webflux | 5.3.15 | CVE-2026-22741 | 6.2.18 / 7.0.7 | DoS via cache poisoning |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2026-22735 | 6.2.17 / 7.0.6 | Stream corruption via SSE |
| org.springframework:spring-webmvc | 5.3.15 | CVE-2026-22741 | 6.2.18 / 7.0.7 | DoS via cache poisoning |
| org.xmlunit:xmlunit-core *(test-scope)* | 2.8.4 | CVE-2024-31573 | 2.10.0 | Insecure XSLT defaults |

---

## INFORMATIONAL (1)

### SEC-7 (Informational, positive) — MyBatis mappers use safe parameterized bindings
**Result: PASS — no SQL injection exposure detected.**

All MyBatis mapper XML files under `src/main/resources/mapper/` use parameterized `#{...}` bindings (sent as JDBC `PreparedStatement` placeholders). There are **no `${...}` string-substitution usages** anywhere under that directory — `${...}` performs raw interpolation and is the primary MyBatis SQL-injection vector.

```
$ grep -rn '\${' src/main/resources/mapper/ || echo "no \${...} found"
no ${...} found
```

Mappers inspected: `ArticleFavoriteMapper.xml`, `ArticleFavoritesReadService.xml`, `ArticleMapper.xml`, `ArticleReadService.xml`, `CommentMapper.xml`, `CommentReadService.xml`, `TagReadService.xml`, `TransferData.xml`, `UserMapper.xml`, `UserReadService.xml`, `UserRelationshipQueryService.xml`. User-controlled values (slug, tag, author, favoritedBy, ids, pagination) all flow through `#{...}` parameters.

**Recommendation:** maintain this convention. If `${...}` is ever needed (e.g. dynamic `ORDER BY`), restrict it to a server-side allowlist of known-safe tokens — never user input.

---

## Confirmation of the six statically-flagged backend dependencies

| # | Dependency (in `build.gradle`) | Scanner verdict | Detail / fix |
|---|---|---|---|
| 1 | `org.xerial:sqlite-jdbc:3.36.0.3` | **CONFIRMED VULNERABLE** | CVE-2023-32697 (HIGH, JDBC-URL RCE). Fix: **3.41.2.2+**. |
| 2 | Spring Boot plugin `org.springframework.boot:2.6.3` (EOL) | **CONFIRMED VULNERABLE (extensive)** | BOM pins vulnerable spring-framework 5.3.15 (Spring4Shell), spring-security 5.6.1, tomcat-embed 9.0.56, jackson 2.13.1, snakeyaml 1.29, logback 1.2.10. Fix: upgrade to supported **3.4.x/3.3.x** (Java 17 + Jakarta) or interim **2.7.x**. |
| 3 | `com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter:4.9.21` | **CONFIRMED VULNERABLE (transitive)** | No direct CVE on the starter, but pulls `graphql-java:17.3` → CVE-2022-37734, CVE-2023-28867, CVE-2024-40094 (all HIGH). Fix: upgrade DGS so graphql-java ≥ 19.11/20.9/21.5. |
| 4 | `org.mybatis.spring.boot:mybatis-spring-boot-starter:2.2.2` | **NOT FLAGGED** | No CVE in Trivy DB. No scanner-driven action; will need 3.0.x line when migrating to Spring Boot 3. |
| 5 | `io.jsonwebtoken:jjwt-api/impl/jackson:0.11.2` | **NOT FLAGGED** | No CVE matched. Maintenance-only upgrade to 0.12.x suggested. |
| 6 | `joda-time:joda-time:2.10.13` | **NOT FLAGGED** | No CVE matched. Long-term, migrate to `java.time`; not a security finding. |

---

## Remediation priorities

1. **Immediate (Critical):** Rotate and externalize the JWT signing secret (SEC-1); purge it from git history.
2. **High-leverage dependency fix:** Upgrade **Spring Boot 2.6.3 → a supported 3.4.x/3.3.x line** (or interim 2.7.x). This transitively resolves the large majority of the 126 dependency CVEs (Spring Framework, Spring Security, Tomcat, Jackson, SnakeYAML, Logback). Bump `sqlite-jdbc` to ≥3.41.2.2 and the DGS starter (graphql-java ≥19.11/20.9/21.5) alongside.
3. **Hardening (High/Medium):** Replace CORS `*` with an explicit allowlist (SEC-2); document/scope the CSRF-disable decision (SEC-3); make REST and GraphQL login constant-time and add rate limiting (SEC-4, SEC-5).
4. **Continuous:** Integrate Trivy/OWASP Dependency-Check and Semgrep + gitleaks into CI to catch regressions.

---

## Methodology & caveats

- **Backend SCA:** Trivy resolved the full transitive graph via a throwaway Gradle lockfile (generated with an external init script; `build.gradle` was never modified) — a plain `trivy fs .` reports zero because Trivy does not resolve Gradle BOM versions without a lockfile.
- Findings reflect the Trivy vulnerability DB and Semgrep registry as of **2026-06-25**; some entries are future-dated CVE IDs present in the upstream advisory feed.
- "NOT FLAGGED" means no matching advisory exists for that exact coordinate/version — not a guarantee of zero vulnerabilities.
- 2 of the 126 dependency findings (`org.assertj:assertj-core`, `org.xmlunit:xmlunit-core`) are **test-scope** and do not ship in the runtime artifact.
- The `p/spring` Semgrep ruleset returned HTTP 404 (retired from the registry); `p/java`, `p/security-audit`, `p/owasp-top-ten`, `p/secrets`, and `--config=auto` were used instead, supplemented by manual review for the framework-specific config findings.
- **Detection-only engagement:** no application source, build, or configuration files were modified, and no remediation PR was created. The per-domain intermediate reports are under `security-reports/`.
