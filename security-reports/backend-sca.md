# Backend Software Composition Analysis (SCA) Report

**Repository:** `ankehao-demo/spring-boot-realworld-example-app`
**Branch/ref:** `master` (commit `1b03279e`)
**Stack:** Spring Boot 2.6.3 + Gradle + Java 11 (RealWorld example backend)
**Scanner:** Trivy 0.71.2 (`--scanners vuln`), vulnerability DB updated 2026-06-25
**Scan method:** Detection-only. Resolved the full transitive dependency graph via a throwaway Gradle dependency lockfile (`gradle.lockfile`, generated with an external init script so `build.gradle` was never modified), then ran `trivy fs --scanners vuln gradle.lockfile`. The lockfile captures BOM-managed transitive versions (Spring, Tomcat, Jackson, etc.) that a raw `build.gradle` scan cannot see. No application source/build code was changed and no PR was opened.

> Note: a plain `trivy fs .` against the repo root reports `num=0` because there is no committed lockfile and Trivy does not resolve Gradle BOM versions on its own; resolving the lockfile first is what surfaces the transitive CVEs below.

## Summary

A total of **126 vulnerabilities** were detected across **32 dependencies** (124 in compile/runtime scope; 2 in test-only scope, flagged `(test-scope)`).

| Severity | Count |
|---|---|
| CRITICAL | 16 |
| HIGH | 53 |
| MEDIUM | 42 |
| LOW | 15 |
| **Total** | **126** |

**Root cause:** the project is pinned to **Spring Boot 2.6.3**, which is **End-of-Life / out of OSS support**. Its BOM drags in vulnerable transitive versions of `spring-framework` (5.3.15), `spring-security` (5.6.1), `tomcat-embed` (9.0.56), `jackson` (2.13.1), `snakeyaml` (1.29), `logback` (1.2.10) and more. The single highest-leverage remediation is to upgrade Spring Boot to a supported 3.3.x/3.4.x line (or at minimum the latest 2.7.x), which clears the large majority of the findings transitively. Direct dependencies (`sqlite-jdbc`, `graphql-dgs`, etc.) should be bumped alongside.

## All detected CVEs (grouped by severity)

Sorted by severity, then dependency. `Fixed Version` lists the first fixed release(s) reported by the advisory; pick the one on your target branch line.

### CRITICAL (16)
| Severity | Dependency | Current Version | CVE | Fixed Version | Note |
|---|---|---|---|---|---|
| CRITICAL | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-24813 | 11.0.3, 10.1.35, 9.0.99 | tomcat: Potential RCE and/or information disclosure and/or information corruption with partial PUT |
| CRITICAL | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-41293 | 9.0.118, 10.1.55, 11.0.22 | tomcat-coyote: Apache Tomcat: HTTP/2 request headers not validated |
| CRITICAL | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-43512 | 9.0.118, 10.1.55, 11.0.22 | tomcat-coyote: Apache Tomcat: Authentication bypass via digest authentication |
| CRITICAL | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-43515 | 9.0.118, 10.1.55, 11.0.22 | tomcat-coyote: tomcat: Improper Authorization allows security bypass |
| CRITICAL | org.springframework.boot:spring-boot-starter-web | 2.6.3 | CVE-2022-22965 | 2.5.12, 2.6.6 | spring-framework: RCE via Data Binding on JDK 9+ |
| CRITICAL | org.springframework.security:spring-security-config | 5.6.1 | CVE-2023-34034 | 5.6.12, 5.7.10, 5.8.5, 6.0.5, 6.1.2 | spring-security-webflux: path wildcard leads to security bypass |
| CRITICAL | org.springframework.security:spring-security-core | 5.6.1 | CVE-2022-22978 | 5.5.7, 5.6.4, 5.4.11 | springframework: Authorization Bypass in RegexRequestMatcher |
| CRITICAL | org.springframework.security:spring-security-core | 5.6.1 | CVE-2022-31692 | 5.7.5, 5.6.9 | spring-security: Authorization rules can be bypassed via forward or include dispatcher types in Spring Security |
| CRITICAL | org.springframework.security:spring-security-web | 5.6.1 | CVE-2022-22978 | 5.5.7, 5.6.4, 5.4.11 | springframework: Authorization Bypass in RegexRequestMatcher |
| CRITICAL | org.springframework.security:spring-security-web | 5.6.1 | CVE-2024-38821 | 5.7.13, 5.8.15, 6.2.7, 6.0.13, 6.1.11, 6.3.4 | Spring-WebFlux: Authorization Bypass of Static Resources in WebFlux Applications |
| CRITICAL | org.springframework.security:spring-security-web | 5.6.1 | CVE-2026-22732 | 6.5.9, 7.0.4 | Spring Security: Spring Security: Security policy bypass and information disclosure due to unwritten HTTP headers |
| CRITICAL | org.springframework:spring-beans | 5.3.15 | CVE-2022-22965 | 5.2.20.RELEASE, 5.3.18 | spring-framework: RCE via Data Binding on JDK 9+ |
| CRITICAL | org.springframework:spring-web | 5.3.15 | CVE-2016-1000027 | 6.0.0 | spring: HttpInvokerServiceExporter readRemoteInvocation method untrusted java deserialization |
| CRITICAL | org.springframework:spring-webflux | 5.3.15 | CVE-2022-22965 | 5.2.20.RELEASE, 5.3.18 | spring-framework: RCE via Data Binding on JDK 9+ |
| CRITICAL | org.springframework:spring-webmvc | 5.3.15 | CVE-2022-22965 | 5.2.20.RELEASE, 5.3.18 | spring-framework: RCE via Data Binding on JDK 9+ |
| CRITICAL | org.springframework:spring-webmvc | 5.3.15 | CVE-2023-20860 | 6.0.7, 5.3.26 | springframework: Security Bypass With Un-Prefixed Double Wildcard Pattern |

### HIGH (53)
| Severity | Dependency | Current Version | CVE | Fixed Version | Note |
|---|---|---|---|---|---|
| HIGH | ch.qos.logback:logback-classic | 1.2.10 | CVE-2023-6378 | 1.3.12, 1.4.12, 1.2.13 | logback: serialization vulnerability in logback receiver |
| HIGH | ch.qos.logback:logback-core | 1.2.10 | CVE-2023-6378 | 1.3.12, 1.4.12, 1.2.13 | logback: serialization vulnerability in logback receiver |
| HIGH | com.fasterxml.jackson.core:jackson-core | 2.13.1 | CVE-2025-52999 | 2.15.0 | com.fasterxml.jackson.core/jackson-core: jackson-core Potential StackoverflowError |
| HIGH | com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2020-36518 | 2.13.2.1, 2.12.6.1 | jackson-databind: denial of service via a large depth of nested objects |
| HIGH | com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2022-42003 | 2.12.7.1, 2.13.4.2 | jackson-databind: deep wrapper array nesting wrt UNWRAP_SINGLE_VALUE_ARRAYS |
| HIGH | com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2022-42004 | 2.12.7.1, 2.13.4 | jackson-databind: use of deeply nested arrays |
| HIGH | com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-54512 | 2.18.8, 3.1.4, 2.21.4 | jackson-databind contains the general-purpose data-binding functionali ... |
| HIGH | com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-54513 | 2.18.8, 2.21.4, 3.1.4 | jackson-databind: Jackson-databind: Security bypass allows arbitrary code execution |
| HIGH | com.google.protobuf:protobuf-java | 3.9.0 | CVE-2021-22569 | 3.16.1, 3.18.2, 3.19.2 | protobuf-java: potential DoS in the parsing procedure for binary data |
| HIGH | com.google.protobuf:protobuf-java | 3.9.0 | CVE-2022-3509 | 3.16.3, 3.19.6, 3.20.3, 3.21.7 | protobuf-java: Textformat parsing issue leads to DoS |
| HIGH | com.google.protobuf:protobuf-java | 3.9.0 | CVE-2022-3510 | 3.16.3, 3.19.6, 3.20.3, 3.21.7 | protobuf-java: Message-Type Extensions parsing issue leads to DoS |
| HIGH | com.google.protobuf:protobuf-java | 3.9.0 | CVE-2024-7254 | 3.25.5, 4.27.5, 4.28.2 | protobuf: StackOverflow vulnerability in Protocol Buffers |
| HIGH | com.graphql-java:graphql-java | 17.3 | CVE-2022-37734 | 17.4, 18.3 | graphql-java: DoS by malicious query |
| HIGH | com.graphql-java:graphql-java | 17.3 | CVE-2023-28867 | 0.0.0-2023-03-20T01-49-44-80e3135, 17.5, 18.4, 19.4, 20.1 | graphql-java: crafted GraphQL query causes stack consumption |
| HIGH | com.graphql-java:graphql-java | 17.3 | CVE-2024-40094 | 19.11, 20.9, 21.5 | graphql-java: Allocation of Resources Without Limits or Throttling in GraphQL Java |
| HIGH | net.minidev:json-smart | 2.4.7 | CVE-2023-1370 | 2.4.9 | json-smart: Uncontrolled Resource Consumption vulnerability in json-smart (Resource Exhaustion) |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2022-42252 | 8.5.83, 9.0.68, 10.0.27, 10.1.1 | tomcat: request smuggling |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2022-45143 | 8.5.84, 9.0.69, 10.1.2 | tomcat: JsonErrorReportValve injection |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-24998 | 10.1.5, 11.0.0-M5, 8.5.88, 9.0.71 | FileUpload: FileUpload DoS with excessive parts |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-46589 | 11.0.0-M11, 10.1.16, 9.0.83, 8.5.96 | tomcat: HTTP request smuggling via malformed trailer headers |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2024-34750 | 11.0.0-M21, 10.1.25, 9.0.90 | tomcat: Improper Handling of Exceptional Conditions |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2024-50379 | 11.0.2, 10.1.34, 9.0.98 | tomcat: RCE due to TOCTOU issue in JSP compilation |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2024-56337 | 11.0.2, 10.1.34, 9.0.98 | tomcat: Incomplete fix for CVE-2024-50379 - RCE due to TOCTOU issue in JSP compilation |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-48988 | 11.0.8, 10.1.42, 9.0.106 | tomcat: Apache Tomcat DoS in multipart upload |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-48989 | 11.0.10, 10.1.44, 9.0.108 | tomcat: http/2 "MadeYouReset" DoS attack through HTTP/2 control frames |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-52520 | 11.0.9, 10.1.43, 9.0.107 | tomcat: Apache Tomcat denial of service |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-53506 | 9.0.107, 10.1.43, 11.0.9 | tomcat: Apache Tomcat denial of service |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-55752 | 11.0.11, 10.1.45, 9.0.109 | tomcat: org.apache.tomcat/tomcat-catalina: Apache Tomcat: Directory traversal via rewrite with possible RCE |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-24880 | 9.0.116, 10.1.52, 11.0.20 | Apache Tomcat: Apache Tomcat: HTTP Request/Response Smuggling via invalid chunk extension |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-34483 | 9.0.116, 10.1.54, 11.0.21 | Apache Tomcat: Apache Tomcat: Information disclosure due to improper encoding in JsonAccessLogValve |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-41284 | 9.0.118, 10.1.55, 11.0.22 | Allocation of Resources Without Limits or Throttling vulnerability in  ... |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-42498 | 9.0.118, 10.1.55, 11.0.22 | tomcat-coyote: Apache Tomcat: Information disclosure due to HTTP Authentication Header exposure during WebSocket authentication. |
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-43513 | 9.0.118, 10.1.55, 11.0.22 | Improper Handling of Case Sensitivity vulnerability in LockOutRealm in ... |
| HIGH | org.assertj:assertj-core | 3.21.0 | CVE-2026-24400 | 3.27.7 | assertj: AssertJ: Information disclosure and denial of service via XML External Entity (XXE) (test-scope) |
| HIGH | org.springframework.boot:spring-boot | 2.6.3 | CVE-2025-22235 | 3.3.11, 3.4.5 | org.springframework.boot/spring-boot: Spring Boot EndpointRequest.to() creates wrong matcher if actuator endpoint is not exposed |
| HIGH | org.springframework.boot:spring-boot | 2.6.3 | CVE-2026-40973 | 4.0.6, 3.5.14 | Spring Boot: Spring Boot: Arbitrary Code Execution and Session Hijacking via predictable temporary directory |
| HIGH | org.springframework.boot:spring-boot-autoconfigure | 2.6.3 | CVE-2023-20883 | 3.0.7, 2.7.12, 2.6.15, 2.5.15 | spring-boot: Spring Boot Welcome Page DoS Vulnerability |
| HIGH | org.springframework.security:spring-security-core | 5.6.1 | CVE-2024-22257 | 5.7.12, 5.8.11, 6.1.8, 6.2.3 | spring-security: Broken Access Control With Direct Use of AuthenticatedVoter |
| HIGH | org.springframework.security:spring-security-crypto | 5.6.1 | CVE-2025-22228 | 6.3.8, 6.4.4, 6.2.10, 6.1.14, 6.0.16, 5.8.18, 5.7.16 | spring-security-core: Spring Security BCryptPasswordEncoder does not enforce maximum password length |
| HIGH | org.springframework:spring-beans | 5.3.15 | CVE-2022-22970 | 5.2.22.RELEASE, 5.3.20 | springframework: DoS via data binding to multipartFile or servlet part |
| HIGH | org.springframework:spring-context | 5.3.15 | CVE-2022-22968 | 5.3.19, 5.2.21.RELEASE | Framework: Data Binding Rules Vulnerability |
| HIGH | org.springframework:spring-core | 5.3.15 | CVE-2025-41249 | 6.2.11 | org.springframework/spring-core: Spring Framework Annotation Detection Vulnerability |
| HIGH | org.springframework:spring-expression | 5.3.15 | CVE-2023-20863 | 6.0.8, 5.3.27, 5.2.24.RELEASE | springframework: Spring Expression DoS Vulnerability |
| HIGH | org.springframework:spring-web | 5.3.15 | CVE-2024-22243 | 6.1.4, 6.0.17, 5.3.32 | springframework: URL Parsing with Host Validation |
| HIGH | org.springframework:spring-web | 5.3.15 | CVE-2024-22259 | 6.1.5, 6.0.18, 5.3.33 | springframework: URL Parsing with Host Validation |
| HIGH | org.springframework:spring-web | 5.3.15 | CVE-2024-22262 | 5.3.34, 6.0.19, 6.1.6 | springframework: URL Parsing with Host Validation |
| HIGH | org.springframework:spring-webflux | 5.3.15 | CVE-2024-38816 | 6.1.13 | spring-webmvc: Path Traversal Vulnerability in Spring Applications Using RouterFunctions and FileSystemResource |
| HIGH | org.springframework:spring-webflux | 5.3.15 | CVE-2024-38819 | 6.1.14 | org.springframework:spring-webmvc: Path traversal vulnerability in functional web frameworks |
| HIGH | org.springframework:spring-webmvc | 5.3.15 | CVE-2024-38816 | 6.1.13 | spring-webmvc: Path Traversal Vulnerability in Spring Applications Using RouterFunctions and FileSystemResource |
| HIGH | org.springframework:spring-webmvc | 5.3.15 | CVE-2024-38819 | 6.1.14 | org.springframework:spring-webmvc: Path traversal vulnerability in functional web frameworks |
| HIGH | org.xerial:sqlite-jdbc | 3.36.0.3 | CVE-2023-32697 | 3.41.2.2 | sqlite-jdbc: Remote code execution when JDBC url is attacker controlled |
| HIGH | org.yaml:snakeyaml | 1.29 | CVE-2022-1471 | 2.0 | SnakeYaml: Constructor Deserialization Remote Code Execution |
| HIGH | org.yaml:snakeyaml | 1.29 | CVE-2022-25857 | 1.31 | snakeyaml: Denial of Service due to missing nested depth limitation for collections |

### MEDIUM (42)
| Severity | Dependency | Current Version | CVE | Fixed Version | Note |
|---|---|---|---|---|---|
| MEDIUM | ch.qos.logback:logback-core | 1.2.10 | CVE-2024-12798 | 1.5.13, 1.3.15 | logback-core: arbitrary code execution via JaninoEventEvaluator |
| MEDIUM | ch.qos.logback:logback-core | 1.2.10 | CVE-2025-11226 | 1.5.19, 1.3.16 | ch.qos.logback/logback-core: Conditional abitrary code execution in logback-core |
| MEDIUM | com.fasterxml.jackson.core:jackson-core | 2.13.1 | GHSA-72hv-8253-57qq | 2.21.1, 2.18.6 | jackson-core: Number Length Constraint Bypass in Async Parser Leads to Potential DoS Condition |
| MEDIUM | com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-50193 | 2.14.0 | jackson-databind contains the general-purpose data-binding functionali ... |
| MEDIUM | com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-54514 | 2.18.8, 2.21.4, 3.1.4 | jackson-databind contains the general-purpose data-binding functionali ... |
| MEDIUM | com.fasterxml.jackson.core:jackson-databind | 2.13.1 | CVE-2026-54515 | 3.1.4, 2.18.9, 2.21.5 | jackson-databind contains the general-purpose data-binding functionali ... |
| MEDIUM | com.google.guava:guava | 30.0-jre | CVE-2023-2976 | 32.0.0-android | guava: insecure temporary directory creation |
| MEDIUM | com.google.protobuf:protobuf-java | 3.9.0 | CVE-2022-3171 | 3.21.7, 3.20.3, 3.19.6, 3.16.3 | protobuf-java: timeout in parser leads to DoS |
| MEDIUM | com.jayway.jsonpath:json-path | 2.6.0 | CVE-2023-51074 | 2.9.0 | json-path: stack-based buffer overflow in Criteria.parse method |
| MEDIUM | org.apache.commons:commons-lang3 | 3.12.0 | CVE-2025-48924 | 3.18.0 | commons-lang/commons-lang: org.apache.commons/commons-lang3: Uncontrolled Recursion vulnerability in Apache Commons Lang |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-41080 | 8.5.93, 9.0.80, 10.1.13, 11.0.0-M11 | tomcat: Open Redirect vulnerability in FORM authentication |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-42795 | 10.1.14, 9.0.81, 8.5.94, 11.0.0-M12 | tomcat: improper cleaning of recycled objects could lead to information leak |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-44487 | 11.0.0-M12, 10.1.14, 9.0.81, 8.5.94 | HTTP/2: Multiple HTTP/2 enabled web servers are vulnerable to a DDoS attack (Rapid Reset Attack) |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2023-45648 | 11.0.0-M12, 10.1.14, 9.0.81, 8.5.94 | tomcat: incorrectly parsed http trailer headers can cause request smuggling |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2024-24549 | 8.5.99, 9.0.86, 10.1.19, 11.0.0-M17 | Tomcat: HTTP/2 header handling DoS |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-49124 | 11.0.8, 10.1.42, 9.0.106 | Apache Tomcat installer for Windows has an untrusted search path vulnerability |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-49125 | 11.0.8, 10.1.42, 9.0.106 | tomcat: Apache Tomcat: Security constraint bypass for pre/post-resources |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-66614 | 11.0.15, 10.1.50, 9.0.113 | tomcat: Client certificate verification bypass due to virtual host mapping |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-25854 | 9.0.116, 10.1.53, 11.0.20 | Apache Tomcat: Apache Tomcat: Open Redirect vulnerability via LoadBalancerDrainingValve |
| MEDIUM | org.apache.tomcat.embed:tomcat-embed-websocket | 9.0.56 | CVE-2024-23672 | 11.0.0-M17, 10.1.19, 9.0.86, 8.5.99 | Tomcat: WebSocket DoS with incomplete closing handshake |
| MEDIUM | org.springframework.hateoas:spring-hateoas | 1.4.1 | CVE-2023-34036 | 1.5.5, 2.0.5, 2.1.1 | Spring HATEOAS vulnerable to Improper Neutralization of HTTP Headers for Scripting Syntax |
| MEDIUM | org.springframework.security:spring-security-core | 5.6.1 | CVE-2022-22976 | 5.5.7, 5.6.4 | springframework: BCrypt skips salt rounds for work factor of 31 |
| MEDIUM | org.springframework.security:spring-security-core | 5.6.1 | CVE-2024-38827 | 5.7.14, 5.8.16, 6.0.14, 6.1.12, 6.2.8, 6.3.5 | spring-security: authorization bypass for case sensitive comparisons |
| MEDIUM | org.springframework:spring-context | 5.3.15 | CVE-2024-38820 | 6.1.14 | The fix for CVE-2022-22968 made disallowedFieldspatterns in DataBinder ... |
| MEDIUM | org.springframework:spring-expression | 5.3.15 | CVE-2022-22950 | 5.3.17, 5.2.20.RELEASE | spring-expression: Denial of service via specially crafted SpEL expression |
| MEDIUM | org.springframework:spring-expression | 5.3.15 | CVE-2023-20861 | 6.0.7, 5.3.26, 5.2.23.RELEASE | springframework: Spring Expression DoS Vulnerability |
| MEDIUM | org.springframework:spring-expression | 5.3.15 | CVE-2024-38808 | 5.3.39 | spring-expression: Denial of service when processing a specially crafted Spring Expression Language expression |
| MEDIUM | org.springframework:spring-messaging | 5.3.15 | CVE-2022-22971 | 5.3.20, 5.2.22.RELEASE | springframework: DoS with STOMP over WebSocket |
| MEDIUM | org.springframework:spring-web | 5.3.15 | CVE-2024-38809 | 5.3.38, 6.0.23, 6.1.12 | org.springframework:spring-web: Spring Framework DoS via conditional HTTP request |
| MEDIUM | org.springframework:spring-web | 5.3.15 | CVE-2024-38820 | 6.1.14 | The fix for CVE-2022-22968 made disallowedFieldspatterns in DataBinder ... |
| MEDIUM | org.springframework:spring-webflux | 5.3.15 | CVE-2026-22737 | 7.0.6, 6.2.17 | Spring Framework: Spring Framework: Information disclosure via Java scripting engine enabled template views |
| MEDIUM | org.springframework:spring-webflux | 5.3.15 | CVE-2026-22745 | 7.0.7, 6.2.18 | spring-webflux: Spring MVC and Spring WebFlux: Denial of Service via slow static resource resolution on Windows |
| MEDIUM | org.springframework:spring-webmvc | 5.3.15 | CVE-2024-38828 | 5.3.42 | org.springframework:spring-webmvc: DoS via Spring MVC controller method with byte[] parameter |
| MEDIUM | org.springframework:spring-webmvc | 5.3.15 | CVE-2025-41242 | 6.2.10 | org.springframework/spring-webmvc: Spring Framework MVC path traversal vulnerability |
| MEDIUM | org.springframework:spring-webmvc | 5.3.15 | CVE-2026-22737 | 7.0.6, 6.2.17 | Spring Framework: Spring Framework: Information disclosure via Java scripting engine enabled template views |
| MEDIUM | org.springframework:spring-webmvc | 5.3.15 | CVE-2026-22745 | 7.0.7, 6.2.18 | spring-webflux: Spring MVC and Spring WebFlux: Denial of Service via slow static resource resolution on Windows |
| MEDIUM | org.springframework:spring-websocket | 5.3.15 | CVE-2025-41254 | 6.2.12 | org.springframework/spring-core: Spring Framework STOMP CSRF Vulnerability |
| MEDIUM | org.yaml:snakeyaml | 1.29 | CVE-2022-38749 | 1.31 | snakeyaml: Uncaught exception in org.yaml.snakeyaml.composer.Composer.composeSequenceNode |
| MEDIUM | org.yaml:snakeyaml | 1.29 | CVE-2022-38750 | 1.31 | snakeyaml: Uncaught exception in org.yaml.snakeyaml.constructor.BaseConstructor.constructObject |
| MEDIUM | org.yaml:snakeyaml | 1.29 | CVE-2022-38751 | 1.31 | snakeyaml: Uncaught exception in java.base/java.util.regex.Pattern$Ques.match |
| MEDIUM | org.yaml:snakeyaml | 1.29 | CVE-2022-38752 | 1.32 | snakeyaml: Uncaught exception in java.base/java.util.ArrayList.hashCode |
| MEDIUM | org.yaml:snakeyaml | 1.29 | CVE-2022-41854 | 1.32 | dev-java/snakeyaml: DoS via stack overflow |

### LOW (15)
| Severity | Dependency | Current Version | CVE | Fixed Version | Note |
|---|---|---|---|---|---|
| LOW | ch.qos.logback:logback-core | 1.2.10 | CVE-2024-12801 | 1.5.13, 1.3.15 | logback-core: SaxEventRecorder vulnerable to Server-Side Request Forgery (SSRF) attacks |
| LOW | ch.qos.logback:logback-core | 1.2.10 | CVE-2026-1225 | 1.5.25 | ch.qos.logback/logback-core: Malicious logback.xml configuration file allows instantiation of arbitrary classes |
| LOW | com.google.guava:guava | 30.0-jre | CVE-2020-8908 | 32.0.0-android | guava: local information disclosure via temporary directory created with unsafe permissions |
| LOW | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-46701 | 9.0.105, 10.1.41, 11.0.7 | tomcat: Apache Tomcat: Security constraint bypass for CGI scripts |
| LOW | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-55754 | 11.0.11, 10.1.45, 9.0.109 | org.apache.tomcat/tomcat-juli: tomcat: Apache Tomcat: console manipulation |
| LOW | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2025-61795 | 11.0.12, 10.1.47, 9.0.110 | tomcat: org.apache.tomcat/tomcat-catalina: Apache Tomcat: Denial of service |
| LOW | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-24733 | 11.0.15, 10.1.50, 9.0.113 | tomcat: security constraint bypass with HTTP/0.9 |
| LOW | org.apache.tomcat.embed:tomcat-embed-core | 9.0.56 | CVE-2026-43514 | 9.0.118, 10.1.55, 11.0.22 | tomcat-coyote: Apache Tomcat: Information disclosure via AJP secret timing discrepancy |
| LOW | org.springframework:spring-context | 5.3.15 | CVE-2025-22233 | 6.2.7, 6.1.20 | CVE-2024-38820 ensured Locale-independent, lowercase conversion for bo ... |
| LOW | org.springframework:spring-webflux | 5.3.15 | CVE-2026-22735 | 7.0.6, 6.2.17 | org.springframework/spring-webmvc: org.springframework/spring-webflux: Spring MVC and WebFlux: Stream corruption vulnerability when using Server-Sent Events |
| LOW | org.springframework:spring-webflux | 5.3.15 | CVE-2026-22740 | 7.0.7, 6.2.18 | spring-webflux: Spring WebFlux: Denial of Service via temporary file accumulation |
| LOW | org.springframework:spring-webflux | 5.3.15 | CVE-2026-22741 | 7.0.7, 6.2.18 | Spring MVC: Spring WebFlux: Spring MVC and Spring WebFlux: Denial of Service via cache poisoning |
| LOW | org.springframework:spring-webmvc | 5.3.15 | CVE-2026-22735 | 7.0.6, 6.2.17 | org.springframework/spring-webmvc: org.springframework/spring-webflux: Spring MVC and WebFlux: Stream corruption vulnerability when using Server-Sent Events |
| LOW | org.springframework:spring-webmvc | 5.3.15 | CVE-2026-22741 | 7.0.7, 6.2.18 | Spring MVC: Spring WebFlux: Spring MVC and Spring WebFlux: Denial of Service via cache poisoning |
| LOW | org.xmlunit:xmlunit-core | 2.8.4 | CVE-2024-31573 | 2.10.0 | org.xmlunit/xmlunit-core: XMLUnit Insecure Defaults when Processing XSLT Stylesheets (test-scope) |

## Confirmation of statically-flagged dependencies

Explicit verification of the six items raised during static review of `build.gradle`.

### 1. `org.xerial:sqlite-jdbc:3.36.0.3` — CONFIRMED VULNERABLE
- **Scanner flagged:** YES. **CVE-2023-32697** — **HIGH** — "sqlite-jdbc: Remote code execution when JDBC url is attacker controlled."
- **Expectation match:** matches the expected CVE-2023-32697.
- **Fixed version:** `3.41.2.2`.
- **Remediation:** upgrade `org.xerial:sqlite-jdbc` to `3.41.2.2` or later (current 3.36.0.3 is a direct dependency in `build.gradle`).

### 2. Spring Boot Gradle plugin `org.springframework.boot` 2.6.3 — CONFIRMED VULNERABLE (EOL)
- **Scanner flagged:** YES, extensively. Spring Boot 2.6.x reached end of OSS support, and the 2.6.3 BOM pins vulnerable transitives. Representative findings:
  - `spring-boot` / `spring-boot-autoconfigure` 2.6.3 — CVE-2025-22235 (HIGH), CVE-2026-40973 (HIGH), CVE-2023-20883 (HIGH, Welcome Page DoS).
  - `spring-web`/`spring-beans`/`spring-webmvc`/`spring-webflux` 5.3.15 — incl. **CVE-2022-22965 "Spring4Shell" RCE (CRITICAL)**, CVE-2016-1000027 (CRITICAL), CVE-2023-20860 (CRITICAL), plus many HIGH/MEDIUM URL-parsing & path-traversal issues.
  - `spring-security-*` 5.6.1 — CVE-2022-22978 (CRITICAL, RegexRequestMatcher auth bypass), CVE-2022-31692 (CRITICAL), CVE-2023-34034 (CRITICAL), CVE-2024-38821 (CRITICAL), CVE-2026-22732 (CRITICAL), plus HIGH/MEDIUM auth-bypass issues.
  - `tomcat-embed-core` 9.0.56 — CVE-2025-24813 (CRITICAL RCE), CVE-2026-41293/43512/43515 (CRITICAL), and ~25 more HIGH/MEDIUM/LOW (request smuggling, JSP RCE TOCTOU CVE-2024-50379/56337, DoS, etc.).
  - `snakeyaml` 1.29 — CVE-2022-1471 (HIGH RCE), `jackson-databind` 2.13.1 — multiple HIGH, `logback` 1.2.10 — CVE-2023-6378 (HIGH).
- **Remediation:** upgrade the `org.springframework.boot` plugin/BOM to a supported release — **3.4.x/3.3.x preferred** (requires Java 17 + Jakarta namespace migration), or at minimum the latest **2.7.x** as an interim step. This resolves the majority of CRITICAL/HIGH findings transitively.

### 3. `com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter:4.9.21` — CONFIRMED VULNERABLE (transitively)
- **Scanner flagged:** the DGS starter artifact itself has **no direct CVE** in the Trivy DB, BUT it pulls in **`com.graphql-java:graphql-java:17.3`**, which is flagged:
  - CVE-2022-37734 (HIGH, DoS via malicious query), CVE-2023-28867 (HIGH, stack consumption), CVE-2024-40094 (HIGH, resource allocation without limits).
- **Remediation:** upgrade the DGS BOM/starter to a newer release (e.g. 8.x/9.x line) so it brings in `graphql-java` ≥ 19.11/20.9/21.5; DGS version should be aligned with the Spring Boot upgrade above.

### 4. `org.mybatis.spring.boot:mybatis-spring-boot-starter:2.2.2` — NOT FLAGGED
- **Scanner flagged:** NO. Neither `mybatis-spring-boot-starter:2.2.2` nor its `mybatis-spring-boot-autoconfigure` transitive surfaced any CVE in the Trivy DB.
- **Remediation:** no scanner-driven action required. As general hygiene it will need to move to the 3.0.x line when migrating to Spring Boot 3 (Jakarta), but there is no known-CVE driver today.

### 5. `io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson : 0.11.2` — NOT FLAGGED
- **Scanner flagged:** NO. None of the three jjwt artifacts at 0.11.2 matched a CVE in the Trivy DB.
- **Remediation:** no scanner-driven action required. Recommended as maintenance only: upgrade to the current 0.12.x line (API changes) to stay on a supported release; not a security blocker per this scan.

### 6. `joda-time:joda-time:2.10.13` — NOT FLAGGED
- **Scanner flagged:** NO. No CVE matched `joda-time:2.10.13` in the Trivy DB.
- **Remediation:** no scanner-driven action required. Long-term, consider migrating to `java.time` (JSR-310), but this is not a security finding.

## Methodology & caveats

- Findings reflect the Trivy vulnerability DB as of the scan date (2026-06-25); new advisories may appear later. Some entries are future-dated CVE IDs (e.g. `CVE-2026-*`) as present in the upstream advisory feed.
- "NOT FLAGGED" means no matching advisory exists in the Trivy DB for that exact coordinate/version; it is not a guarantee of zero vulnerabilities.
- 2 of the 126 findings (`org.assertj:assertj-core`, `org.xmlunit:xmlunit-core`) are test-only scope and do not ship in the runtime artifact.
- Detection-only engagement: no source/build files were modified and no PR was created. The temporary `gradle.lockfile` and `security-reports/` directory are not committed.
