# Security exceptions

Time-boxed Snyk ignores for the Java 17 / Spring Boot 3 upgrade. Each one is an entry in the
root `.snyk` policy file with path `*`. Joel O'Shea approved all of them on 2026-11-17. There are
no fixes on any Spring Boot 3.x line; every fix needs Spring Boot 4 (Spring Framework 7.0.9,
Spring Security 7.0.7, Spring for GraphQL 2.0.5). No other ignores, wildcard ids, severity
thresholds or org-policy changes are approved.

**Expiry: 2027-02-15** (all entries). By then, upgrade to Spring Boot 4 or ask for a new decision.
Do not extend an ignore without approval.

| Snyk ID | CVE | Package | Severity (Snyk) | Exposed | Why | Expires |
| --- | --- | --- | --- | --- | --- | --- |
| `SNYK-JAVA-ORGSPRINGFRAMEWORK-19267094` | CVE-2026-47890 | `org.springframework:spring-webmvc` 6.2.19 | Medium 5.9 (GHSA: critical) | No | SSE stream corruption with view fragments. The app has no `SseEmitter`, `ServerSentEvent`, `FragmentsRendering`, `XsltView` or `ViewResolver`; every endpoint returns JSON. | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORKGRAPHQL-19267447` | CVE-2026-59286 | `org.springframework.graphql:spring-graphql` 1.4.6 | Critical 9.0 | **Yes** | GraphiQL loads its scripts from a CDN. GraphiQL is enabled and public: `GET /graphiql` returns 307 to `/graphiql?path=/graphql`, which serves the GraphiQL page loading from `esm.sh`, and `WebSecurityConfig` permits `/graphiql`. | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORKGRAPHQL-19267450` | CVE-2026-59288 | `org.springframework.graphql:spring-graphql` 1.4.6 | High 8.3 | **Yes** | CSRF through the GraphiQL page. GraphiQL is enabled and public at `/graphiql` (see above). | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORKGRAPHQL-19267462` | CVE-2026-59289 | `org.springframework.graphql:spring-graphql` 1.4.6 | High 8.2 | No | Needs Connection fields backed by Spring Data auto-registration (`QueryByExampleDataFetcher`, `QuerydslDataFetcher`). There is no Spring Data; the Connection fields are hand-written DGS data fetchers over MyBatis. | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORKGRAPHQL-19267466` | CVE-2026-59287 | `org.springframework.graphql:spring-graphql` 1.4.6 | High 8.7 | No | Needs `WebSocketGraphQlClient` with keepAlive. The app is a server only and uses no GraphQL client. | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORK-19267077` | CVE-2026-47885 | `org.springframework:spring-web` 6.2.19 | High 8.2 | No | Needs `PartEventHttpMessageReader` with `maxInMemorySize` -1. That reader is WebFlux-only (WebFlux is not on the runtime classpath), and `maxInMemorySize` is never set. | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORK-19267079` | CVE-2026-47889 | `org.springframework:spring-web` 6.2.19 | High 8.3 | No | Affects `JettyCoreServerHttpResponse` cookies. The app runs on Tomcat, not Jetty, and sets no cookies: auth is a stateless JWT in the `Authorization` header. | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORK-19267433` | CVE-2026-47883 | `org.springframework:spring-webmvc` 6.2.19 | Medium 5.3 | **Unclear** | Open redirect in `UrlHandlerFilter` with broadly matching patterns. `TrailingSlashConfig` does register `UrlHandlerFilter.trailingSlashHandler("/**")`, but in `wrapRequest()` mode, which never redirects. Probes (`//evil.example/`, `/%2F%2Fevil.example/`, `/\evil.example/`, `/articles//evil.example/`) returned 400 with no `Location` header. | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORKSECURITY-19267391` | CVE-2026-47842 | `org.springframework.security:spring-security-crypto` 6.5.11 | High 7.1 | No | Needs `AesBytesEncryptor` in CBC mode with a null or default IV generator. The app uses no `AesBytesEncryptor` or `Encryptors`, only `BCryptPasswordEncoder`. | 2027-02-15 |
| `SNYK-JAVA-ORGSPRINGFRAMEWORKSECURITY-19267441` | CVE-2026-59276 | `org.springframework.security:spring-security-core` 6.5.11 | High 8.2 | No | Timing attack in `DigestAuthenticationFilter`, `KeyBasedPersistenceTokenService`, the Password4j encoders and `InMemoryOAuth2AuthorizationService`. The app uses none of them: passwords use BCrypt and tokens use JJWT. | 2027-02-15 |

## Exposed: revisit

These are ignored as approved, but the affected feature is reachable in this app:

- **CVE-2026-59286** (`SNYK-JAVA-ORGSPRINGFRAMEWORKGRAPHQL-19267447`, critical) and
  **CVE-2026-59288** (`SNYK-JAVA-ORGSPRINGFRAMEWORKGRAPHQL-19267450`, high): the bundled GraphiQL
  page is enabled and public at `/graphiql` and loads its scripts from `esm.sh`. Snyk's
  workaround is to disable it (`spring.graphql.graphiql.enabled=false`, or DGS's
  `dgs.graphql.graphiql.enabled=false`) or to serve a vendored copy. Both changes would alter the
  `/graphiql` behaviour.
- **CVE-2026-47883** (`SNYK-JAVA-ORGSPRINGFRAMEWORK-19267433`, medium), unclear:
  `UrlHandlerFilter` is mapped to `/**`. The probes found no redirect, since the filter runs in
  `wrapRequest()` mode, but the advisory gives no exploit detail to confirm this.
