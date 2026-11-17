# Security exceptions

Time-boxed Snyk ignores in effect for the Java 17 / Spring Boot 3 upgrade. Each one lives in the
root `.snyk` policy file. No other ignores, severity thresholds or org-policy changes are approved.

| CVE | Snyk id | Package | Approved by | Added | Expires |
| --- | --- | --- | --- | --- | --- |
| CVE-2026-47890 (GHSA-j9f9-w8pj-32f8) | `SNYK-JAVA-ORGSPRINGFRAMEWORK-19267094` | `org.springframework:spring-webmvc` 6.2.19 | Joel O'Shea | 2026-11-17 | 2027-02-15 |

## CVE-2026-47890

- **Issue:** stream corruption when Server-Sent Events (SSE) are rendered with view fragments
  (GHSA rates it critical; Snyk lists it as medium, 5.9, Cross-site Scripting).
- **Affected:** Spring Framework 6.2.0 - 6.2.19 and 7.0.0 - 7.0.8. Spring Boot 3.5.16 manages
  spring-webmvc 6.2.19.
- **Fix:** none on the 6.2 line. The only fix is Spring Framework 7.0.9, which needs Spring Boot 4.
- **Why the app is not exposed:** it has no SSE, view-fragment or view-rendering code. There is no
  `SseEmitter`, `ServerSentEvent`, `FragmentsRendering`, `XsltView` or `ViewResolver` in `src`;
  every endpoint returns JSON.
- **Scope:** only `SNYK-JAVA-ORGSPRINGFRAMEWORK-19267094`, path `*`.
- **Expiry:** 2027-02-15 (90 days). By then, upgrade to Spring Boot 4 / Spring Framework 7.0.9+
  or ask for a new decision. Do not extend the ignore without approval.
