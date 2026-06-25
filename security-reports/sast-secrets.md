# SAST + Secrets Security Report

**Repository:** `ankehao-demo/spring-boot-realworld-example-app`
**Branch/ref:** `master`
**Scan type:** Detection-only (no source modifications, no PR)
**Date:** 2026-06-25
**Tooling:** gitleaks v8.18.4 (secrets), Semgrep v1.168.0 (SAST: `p/security-audit`, `p/owasp-top-ten`, `p/secrets`, `p/java`, `--config=auto`), manual code review for framework-specific findings.

---

## Summary

| Severity | Count |
|----------|-------|
| Critical | 1 |
| High | 3 |
| Medium | 1 |
| Low | 1 |
| Informational | 1 (positive verification) |

- **1 Critical** — hardcoded JWT signing secret committed to the repo.
- **3 High** — CORS allow-all origins, CSRF globally disabled, REST user-enumeration / non-constant-time login.
- **1 Medium** — GraphQL user-enumeration / non-constant-time login.
- **1 Low** — hardcoded JWT/test secret in test sources.
- **1 Informational (positive)** — MyBatis mappers use safe parameterized `#{...}` bindings; no SQL injection exposure detected.

---

## A) Secrets

### S-1 (Critical) — Hardcoded JWT signing secret in application.properties

- **File:** `src/main/resources/application.properties`
- **Line:** 9
- **Property:** `jwt.secret`
- **Value (redacted):** `nRvy…6wA` (Base64-like, ~88 chars, gitleaks entropy 5.46, rule `generic-api-key`) — full value intentionally NOT reproduced here.
- **Detector:** gitleaks (`generic-api-key`), fingerprint `src/main/resources/application.properties:generic-api-key:9`.
- **Why it matters:** This is the HMAC signing key for all JWTs (`DefaultJwtService`). Anyone with repo/history read access can forge arbitrary, valid authentication tokens for any user (`sub` claim) and fully bypass authentication. The secret is committed to version control, so it is also exposed in git history.
- **Remediation:**
  - Externalize the secret to an environment variable / secrets manager (e.g. `jwt.secret=${JWT_SECRET}`) and inject at runtime; never commit it.
  - **Rotate** the key immediately — assume it is compromised since it has been committed. Rotating invalidates all existing tokens.
  - Purge the value from git history (e.g. `git filter-repo` / BFG) if the history is shared.
  - Use a per-environment, randomly generated key of adequate length (≥256-bit for HS256/HS512).

### S-2 (Low) — Hardcoded JWT + dummy signing key in test sources

- **File:** `src/test/java/io/spring/infrastructure/service/DefaultJwtServiceTest.java`
- **Lines:** 16 (dummy signing key `"123123…"`), 38 (a sample/expired JWT `eyJhbGci…` — full value not reproduced).
- **Detector:** gitleaks (`jwt`), fingerprint `…DefaultJwtServiceTest.java:jwt:38`.
- **Why it matters:** Low risk — these are test fixtures (a throwaway signing key and an already-expired token used to assert rejection), not production credentials. Flagged for completeness.
- **Remediation:** Acceptable as test fixtures. Optionally generate keys/tokens dynamically in test setup to keep scanners quiet and avoid normalizing committed secrets.

**Other committed credentials:** `spring.datasource.username` / `spring.datasource.password` in `application.properties` (lines 3–4) are empty (local SQLite); no DB credentials committed. No other secrets found.

---

## B) SAST findings

### SAST-1 (High) — CORS misconfiguration: allow-all origins

- **File:** `src/main/java/io/spring/api/security/WebSecurityConfig.java`
- **Line:** 70
- **Code:** `configuration.setAllowedOrigins(asList("*"));`
- **Detector:** Manual review (not flagged by Semgrep registry rules in this run).
- **Severity:** High.
- **Why it matters:** Every origin is permitted for `/**`. Combined with the permissive method/header set (lines 71, 78), any website can issue cross-origin requests to the API. While `setAllowCredentials(false)` (line 75) mitigates cookie-based credential theft, this API is JWT/`Authorization`-header based and allows the `Authorization` header cross-origin, so a malicious site can drive authenticated requests if it can obtain/replay a token.
- **Remediation:** Replace the `*` wildcard with an explicit allowlist of trusted origins (e.g. `setAllowedOrigins(asList("https://app.example.com"))`), or use `setAllowedOriginPatterns` with tightly scoped patterns. Restrict methods/headers to what is actually required.

### SAST-2 (High) — CSRF protection globally disabled

- **File:** `src/main/java/io/spring/api/security/WebSecurityConfig.java`
- **Line:** 38 (`http.csrf()` → `.disable()` on line 39)
- **Code:** `http.csrf().disable()`
- **Detector:** Semgrep — `java.spring.security.audit.spring-csrf-disabled.spring-csrf-disabled` (severity WARNING), confirmed via `p/security-audit` and `--config=auto`.
- **Severity:** High.
- **Why it matters:** CSRF protection is turned off for the entire application. For a stateless, `Authorization`-header-only JWT API this is a common and largely defensible pattern (CSRF primarily threatens ambient/cookie credentials). However, if any session/cookie-based auth or state-changing form endpoints are ever introduced, disabling CSRF globally becomes exploitable. Flagged as configured because the disable is unconditional and global.
- **Remediation:** Confirm the app is exclusively token-in-header and stateless (it sets `SessionCreationPolicy.STATELESS`, line 46 — good). Prefer disabling CSRF narrowly rather than globally, and document the rationale. If any cookie-based auth is added, re-enable CSRF (e.g. `CookieCsrfTokenRepository`).

### SAST-3 (High) — User enumeration / non-constant-time login (REST)

- **File:** `src/main/java/io/spring/api/UsersApi.java`
- **Lines:** 49–57
- **Code:**
  ```java
  Optional<User> optional = userRepository.findByEmail(loginParam.getEmail());
  if (optional.isPresent()
      && passwordEncoder.matches(loginParam.getPassword(), optional.get().getPassword())) {
    UserData userData = userQueryService.findById(optional.get().getId()).get();
    return ResponseEntity.ok(
        userResponse(new UserWithToken(userData, jwtService.toToken(optional.get()))));
  } else {
    throw new InvalidAuthenticationException();
  }
  ```
- **Detector:** Manual review.
- **Severity:** High.
- **Why it matters:** The password hash comparison (`passwordEncoder.matches`, an expensive BCrypt operation) runs **only when the email exists** (short-circuited by `optional.isPresent() && …`). When the email does not exist, the code skips BCrypt and returns immediately. This produces a measurable timing side-channel: a non-existent email responds noticeably faster than an existing email with a wrong password, allowing an attacker to enumerate valid accounts. (The error message is identical for both branches, so the leak is timing-based rather than message-based.)
- **Remediation:** Make the work constant regardless of account existence — when the user is not found, still perform a dummy BCrypt verification against a fixed dummy hash so response time does not depend on whether the email exists. Return a single generic authentication-failure response (already done) and consider rate limiting / lockout on repeated failures.

### SAST-4 (Medium) — User enumeration / non-constant-time login (GraphQL)

- **File:** `src/main/java/io/spring/graphql/UserMutation.java`
- **Lines:** 58–66
- **Code:**
  ```java
  Optional<User> optional = userRepository.findByEmail(email);
  if (optional.isPresent() && encryptService.matches(password, optional.get().getPassword())) {
    return DataFetcherResult.<UserPayload>newResult()
        .data(UserPayload.newBuilder().build())
        .localContext(optional.get())
        .build();
  } else {
    throw new InvalidAuthenticationException();
  }
  ```
- **Detector:** Manual review.
- **Severity:** Medium (same class as SAST-3, exposed via the GraphQL `login` mutation).
- **Why it matters:** Identical pattern to the REST login: `encryptService.matches(...)` (BCrypt) is short-circuited and only runs when the email exists. The GraphQL `login` mutation therefore has the same user-enumeration timing side-channel.
- **Remediation:** Same as SAST-3 — perform a constant-time dummy hash verification when the email is not found, return a uniform error, and apply rate limiting. Ideally centralize authentication logic so REST and GraphQL share one hardened implementation.

### Other Semgrep findings

No additional findings were surfaced by Semgrep beyond `spring-csrf-disabled` (SAST-2). The `p/spring` ruleset is no longer available in the Semgrep registry (HTTP 404); `p/java`, `p/security-audit`, `p/owasp-top-ten`, `p/secrets`, and `--config=auto` were used instead. Note: `gradlew` produced a (benign) parser syntax-error notice and was not analyzed as a target source file.

---

## C) SQL Injection — Positive Verification (Informational)

**Result: PASS — no SQL injection exposure detected in MyBatis mappers.**

All MyBatis mapper XML files under `src/main/resources/mapper/` use **parameterized `#{...}` bindings** (which MyBatis sends as JDBC `PreparedStatement` placeholders). There are **no `${...}` string-substitution usages** anywhere under that directory (`${...}` performs raw string interpolation and is the primary MyBatis SQL-injection vector).

**Evidence — grep for `${...}`:**
```
$ grep -rn '\${' src/main/resources/mapper/ || echo "no \${...} found"
no ${...} found
```

**Mapper files inspected:**
```
ArticleFavoriteMapper.xml        TagReadService.xml
ArticleFavoritesReadService.xml  TransferData.xml
ArticleMapper.xml                UserMapper.xml
ArticleReadService.xml           UserReadService.xml
CommentMapper.xml                UserRelationshipQueryService.xml
CommentReadService.xml
```

**Evidence — safe `#{...}` parameter bindings present (sample, `ArticleReadService.xml`):**
```
41:        where A.id = #{id}
45:        where A.slug = #{slug}
51:                T.name = #{tag}
54:                AND AU.username = #{author}
57:                AND AFU.username = #{favoritedBy}
61:        limit #{page.offset}, #{page.limit}
89:            #{id}
```

User-controlled values (slug, tag, author, favoritedBy, ids, pagination offsets/limits) all flow through `#{...}` prepared-statement parameters. No dynamic SQL is assembled via unsafe `${...}` interpolation.

**Recommendation:** Maintain this convention. If `${...}` is ever required (e.g. dynamic `ORDER BY` column/direction), restrict it to a server-side allowlist of known-safe tokens — never pass user input directly into `${...}`.

---

## Appendix — Commands run

```bash
# Secrets
gitleaks detect --source . --no-git --report-format json --report-path /tmp/gitleaks.json -v
#   → 2 findings: jwt.secret (application.properties:9), test JWT (DefaultJwtServiceTest.java:38)

# SAST
semgrep --config "p/security-audit" --config "p/owasp-top-ten" --config "p/secrets" --json -o /tmp/semgrep_sec.json .
semgrep --config "p/java" --json -o /tmp/semgrep_java.json .
semgrep --config=auto --metrics=on --json -o /tmp/semgrep_auto.json .
#   → spring-csrf-disabled @ WebSecurityConfig.java:38

# SQL injection verification
grep -rn '\${' src/main/resources/mapper/ || echo "no \${...} found"
#   → no ${...} found
```
