# Test Coverage Improvement Plan

Backend: Spring Boot "RealWorld/Conduit" API (Gradle + MyBatis + JWT + Netflix DGS GraphQL).

This plan is generated from **real JaCoCo measurements**, not estimates. Coverage
tooling (the `jacoco` Gradle plugin + `jacocoTestReport`) was added so SonarQube can
ingest `build/reports/jacoco/test/jacocoTestReport.xml` — the same XML these numbers
come from, so the before/after figures below match what the quality gate reads.

## How to reproduce

```bash
./gradlew clean test jacocoTestReport
# XML (Sonar ingests this): build/reports/jacoco/test/jacocoTestReport.xml
# HTML (human view):        build/reports/jacoco/test/html/index.html
```

## Current baseline (measured)

| Metric | Covered / Total | Coverage |
|---|---|---|
| **Line** | 738 / 2167 | **34.1%** |
| Instruction | 3477 / 10488 | 33.2% |
| Branch | 146 / 890 | 16.4% |
| Method | 347 / 879 | 39.5% |
| Class | 86 / 159 | 54.1% |

> **Big caveat — generated code dominates the miss count.** The Netflix DGS codegen
> emits `io.spring.graphql.types.*` (767 lines) and `io.spring.graphql.DgsConstants*`
> (~36 lines) into `build/generated`. These are **767+ uncovered lines that no one
> should write tests for.** Excluding them, hand-written source coverage is
> **~738 / 1364 ≈ 54%**. The single highest-leverage action for the quality gate is to
> **exclude generated code from coverage** (see "Recommended Sonar/JaCoCo config"),
> then attack the GraphQL data-fetcher layer.

## Per-package coverage, sorted by priority

Priority = uncovered lines weighted by business criticality (paths that mutate data or
authenticate users rank highest; generated DTOs rank lowest).

| # | Package | Current % | Target % | Uncovered lines | Business criticality | Notes on tests needed |
|---|---|---:|---:|---:|---|---|
| 1 | `io.spring.graphql` (data fetchers & mutations) | 1.9% | 70% | 467 | **Critical** — GraphQL read/write API: create/update/delete article, comments, follow, favorite, current user | DGS query/mutation tests via `DgsQueryExecutor` (or `@SpringBootTest` + `WebGraphQlTester`). Cover happy paths + auth (`SecurityUtil.getCurrentUser`) + not-found/unauthorized branches for `ArticleDatafetcher` (193 lines, 0%), `ArticleMutation`, `CommentDatafetcher`, `CommentMutation`, `UserMutation`, `RelationMutation`, `MeDatafetcher`, `ProfileDatafetcher`. |
| 2 | `io.spring.graphql.types` *(generated)* | 0.0% | n/a (exclude) | 767 | N/A — DGS codegen output | **Do not write tests.** Exclude from JaCoCo/Sonar. Removing this from the denominator alone lifts overall line coverage from ~34% to ~54%. |
| 3 | `io.spring.application` (read-model query services) | 76.4% | 90% | 51 | **High** — CQRS read model feeding all list/detail endpoints | Unit tests for `CommentQueryService` (21 miss) and `ArticleQueryService` (20 miss): pagination (cursor + offset), favorited/following flags, empty results. Cover `CursorPager`/`CursorPageParameter` edge cases (first/last page, empty cursor). |
| 4 | `io.spring.graphql.exception` | 3.2% | 80% | 60 | **Medium** — GraphQL error surface (validation → client errors) | Test `GraphQLCustomizeExceptionHandler` mapping of `ConstraintViolation`/`InvalidRequest`/auth exceptions to DGS `GraphQLError` entries; assert error extensions/classification. |
| 5 | `io.spring.application.article` (article command DTOs/service) | 27.6% | 85% | 21 | **High** — write path: `ArticleCommandService` (7%), `NewArticleParam` validation | Unit-test `ArticleCommandService` create/update; bean-validation tests for `NewArticleParam`/`UpdateArticleParam` (required fields, `DuplicatedArticleValidator`). |
| 6 | `io.spring.api.exception` (REST error handling) | 72.6% | 90% | 23 | **Medium** — REST `@ControllerAdvice`, error serialization | Cover `CustomizeExceptionHandler` (15 miss) branches: 422 validation body, 404, 401; `InvalidRequestException` construction; `FieldErrorResource` serialization. MockMvc tests hitting invalid payloads. |
| 7 | `io.spring.application.user` (user command DTOs/service) | 76.9% | 90% | 15 | **Medium** — registration/profile update validation | Cover `UserService` update (6 miss), `RegisterParam` (4 miss), `UpdateUserValidator` uniqueness branches (email/username already taken vs. unchanged). |
| 8 | `io.spring` (root: app bootstrap + config) | 76.5% | 80% | 4 | **Low** — `RealWorldApplication`, `CursorPageBase`/config wiring | Mostly covered by `contextLoads`. Remaining lines are config/main; low value, leave or add a trivial config assertion. |
| 9 | `io.spring.core.article` (domain) | 91.1% | 95% | 4 | **High** (domain invariants) but nearly done | Add tests for the 4 missed `Article` lines (slug/update edge cases). |
| 10 | `io.spring.infrastructure.mybatis` (type handlers) | 63.6% | 85% | 4 | **Medium** — `DateTimeHandler` (SQLite ↔ Joda) | Unit-test `DateTimeHandler` get/set null + non-null round-trip. |
| 11 | `io.spring.infrastructure.repository` | 94.4% | 95% | 3 | **High** (persistence) but nearly done | Fill 3 missed lines in repository impls; existing MyBatis*RepositoryTest cover the rest. |
| 12 | `io.spring.core.user` (domain) | 94.3% | 95% | 2 | **High** (domain) but nearly done | 2 missed `User` lines (e.g. equals/update). |
| 13 | `io.spring.core.service` (`AuthorizationService`/`JwtService` iface) | 66.7% | 90% | 1 | **Critical** (authz) — small surface | 1 missed line; add `canWrite`/authorization negative-case test. |
| 14 | `io.spring.api.security` | 98.4% | 98% | 1 | **Critical** (JWT filter) — effectively done | Optional: 1 missed branch in the JWT filter. |
| 15 | `io.spring.application.data` (DTO projections) | 98.4% | 98% | 1 | **Low** (data holders) — done | No action needed. |
| 16 | `io.spring.api` (REST controllers) | 96.5% | 97% | 5 | **Critical** but well covered | Small gaps only; existing `*ApiTest` suite covers controllers. |
| 17 | `io.spring.infrastructure.service` | 100.0% | 100% | 0 | High | None. |
| 18 | `io.spring.core.comment` | 100.0% | 100% | 0 | Medium | None. |
| 19 | `io.spring.core.favorite` | 100.0% | 100% | 0 | Medium | None. |
| — | `io.spring.infrastructure.mybatis.mapper` / `readservice` | n/a | n/a | 0 | N/A — interfaces only (no executable lines) | Exercised indirectly via repository tests. |

## Work broken into module-sized chunks

Each chunk is independently shippable. Chunks are ordered by return-on-effort.

### Chunk A — Exclude generated code (config only, ~0.5 day) — **do first**
- **Scope:** `io.spring.graphql.types.*`, `io.spring.graphql.DgsConstants*`.
- **Current → Target:** overall line **34.1% → ~54%** with zero new tests.
- **Tests needed:** none. See "Recommended Sonar/JaCoCo config" below.

### Chunk B — GraphQL data fetchers & mutations (~3–4 days)
- **Packages:** `io.spring.graphql`.
- **Current → Target:** **1.9% → 70%** (~330 lines recovered).
- **Tests needed:** DGS `DgsQueryExecutor`/`WebGraphQlTester` integration tests for each
  datafetcher/mutation; happy path + auth-required + not-found/forbidden branches; reuse
  the existing `TestWithCurrentUser`/`TestHelper` fixtures from the REST suite.

### Chunk C — Read-model query services (~1.5 days)
- **Packages:** `io.spring.application` (+ `CursorPager` helpers).
- **Current → Target:** **76.4% → 90%**.
- **Tests needed:** `CommentQueryService`/`ArticleQueryService` unit tests over pagination,
  favorited/following flags, empty/last-page cursor edges.

### Chunk D — Error handling (REST + GraphQL) (~1.5 days)
- **Packages:** `io.spring.graphql.exception`, `io.spring.api.exception`.
- **Current → Target:** **3.2% → 80%** and **72.6% → 90%**.
- **Tests needed:** exception-handler mapping tests (validation → 422/GraphQL error,
  not-found → 404, auth → 401); serializer tests for `ErrorResourceSerializer`/`FieldErrorResource`.

### Chunk E — Command services & validation (~1 day)
- **Packages:** `io.spring.application.article`, `io.spring.application.user`.
- **Current → Target:** **27.6% → 85%** and **76.9% → 90%**.
- **Tests needed:** `ArticleCommandService`/`UserService` unit tests; bean-validation tests
  for `NewArticleParam`/`RegisterParam` and the duplicate email/username/article validators.

### Chunk F — Domain & infra polish (~0.5 day)
- **Packages:** `core.article`, `core.user`, `core.service`, `infrastructure.mybatis`,
  `infrastructure.repository`, `api.security`.
- **Current → Target:** push each remaining package to ≥95%.
- **Tests needed:** small targeted tests for the handful of missed lines each
  (`DateTimeHandler` null-handling, `Article`/`User` edge cases, `AuthorizationService`
  negative case).

## Target after all chunks

| Stage | Overall line coverage |
|---|---|
| Baseline (raw, incl. generated) | 34.1% |
| After Chunk A (exclude generated) | ~54% |
| After Chunks B–F | **~85%+** |

## Recommended Sonar/JaCoCo config (follow-up, not in this PR)

This PR intentionally contains only the plan + the minimal `build.gradle` change that
adds JaCoCo. As a fast follow, exclude generated code so the quality gate reflects
hand-written coverage:

```gradle
// build.gradle — exclude DGS-generated code from the coverage report
jacocoTestReport {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                'io/spring/graphql/types/**',
                'io/spring/graphql/DgsConstants*'
            ])
        }))
    }
}
```

```properties
# sonar-project.properties (or Sonar Gradle config)
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
sonar.coverage.exclusions=**/graphql/types/**,**/DgsConstants*,**/*Application.java
```
