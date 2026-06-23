# Spring Boot 2.6.3 → 3.2.12 / Java 11 → 21 — Migration Approach Comparison

This document compares three independent strategies for migrating
`spring-boot-realworld-example-app` from **Spring Boot 2.6.3 on Java 11** to
**Spring Boot 3.2.12 on Java 21**. Each strategy was implemented on its own
branch and produced a working build (`./gradlew clean build` + `./gradlew test`
green on Java 21, all **68 tests** passing).

| Branch | Approach | PR |
|--------|----------|----|
| `upgrade/incremental` | Staged: 2.6.3 → 2.7.18 → 3.0.13 → 3.2.12, validated at each step | [#576](https://github.com/COG-GTM/spring-boot-realworld-example-app/pull/576) |
| `upgrade/direct-jump` | Single pass: 2.6.3 → 3.2.12 in one commit | [#577](https://github.com/COG-GTM/spring-boot-realworld-example-app/pull/577) |
| `upgrade/openrewrite` | Automated (OpenRewrite recipes) + manual cleanup | [#578](https://github.com/COG-GTM/spring-boot-realworld-example-app/pull/578) |

All three branches converge on the **same final state**: Spring Boot `3.2.12`,
`io.spring.dependency-management 1.1.7`, `source/targetCompatibility = '21'`,
Gradle wrapper `8.5`, DGS `8.7.1`, DGS codegen `6.3.0`/`7.0.3`, JJWT `0.12.6`,
MyBatis starter `3.0.4`, rest-assured `5.5.0`, sqlite-jdbc `3.47.1.0`,
joda-time `2.13.0`, spotless `6.25.0`.

---

## Comparison Matrix

| Metric | Incremental (`#576`) | Direct Jump (`#577`) | OpenRewrite (`#578`) |
|--------|----------------------|----------------------|----------------------|
| **Manual code changes** (hand-edited source LOC, excl. `build.gradle` version bumps) | ~84 | ~108 | **~18** (after OpenRewrite ran) |
| **Commits / steps** | 3 (one per SB stage) | 1 | 3 (auto run → manual cleanup → plugin removal) |
| **Total diff vs `master`** | 25 files, +107 / −103 | 28 files, +132 / −134 | 36 files, +326 / −269 |
| **Test result** | 68/68 pass; **also green at each intermediate stage** (2.7.18, 3.0.13, 3.2.12) | 68/68 pass; green only after fixes | 68/68 pass; green after manual cleanup |
| **First-attempt green?** | No — small fixes within each stage, but each stage ended green before moving on | No — **3 rounds** of build/test failures after first compile | No — OpenRewrite output did not compile/run until manual DGS/JJWT/GraphQL fixes |
| **Risk** | **Lowest** — every step independently validated; easy to bisect a regression | **Highest** — all-or-nothing; failures stack and are harder to localize | **Medium** — automation is deterministic, but the manual residue is the genuinely hard part |
| **Effort** | **Highest** — multiple validate/commit cycles, build/test run ~4× | Medium — one pass, but debugging a big simultaneous change is slow | **Lowest** — automation absorbs the mechanical bulk (javax→jakarta + Security rewrite) |
| **Code cleanliness** | Clean, coherent per-stage history | Clean single consistent state | Functionally clean; OpenRewrite touches more files (broad reformatting/imports) → larger, noisier diff |

> **Note on the diff sizes.** OpenRewrite produces the *largest* diff (+326/−269,
> 36 files) even though it has the *fewest manual* lines, because the recipes
> reformat and rewrite imports across many files mechanically. The
> hand-written branches keep the diff tighter because a human only touches what
> is strictly necessary.

---

## What each approach actually had to do

The migration decomposes into **mechanical bulk** (cheap, automatable) and a
**hard core** (manual in *every* approach):

### Mechanical bulk — where approaches differ
- `javax.* → jakarta.*` across ~21 files (servlet, validation, annotation).
  *Note:* `javax.crypto.*` in `DefaultJwtService` is JDK, not Jakarta EE, and
  must stay.
- Spring Security 6: drop `WebSecurityConfigurerAdapter`, expose
  `@Bean SecurityFilterChain`, `antMatchers → requestMatchers`, lambda DSL.
- Spring Boot / dependency-management plugin version bumps; `source/targetCompatibility`.

**OpenRewrite automated all of the above** via
`UpgradeSpringBoot_3_2` + `UpgradeToJava21`. The incremental and direct-jump
branches did every one of these by hand.

### Hard core — manual in all three approaches
These have **no OpenRewrite recipe** and were the real time sink everywhere:

1. **Netflix DGS framework jump (4.9.21 → 8.7.1).** Switch artifact to
   `graphql-dgs-spring-graphql-starter`, bump codegen plugin, and rewrite
   `GraphQLCustomizeExceptionHandler` from the removed synchronous
   `onException(...)` to `CompletableFuture<…> handleException(...)`.
2. **DGS codegen `PageInfo` change.** Codegen 6.x/7.x generates
   `*Connection.pageInfo(io.spring.graphql.types.PageInfo)` instead of accepting
   `graphql.relay.PageInfo`; `ArticleDatafetcher`/`CommentDatafetcher` had to be
   adjusted (typeMapping or building the generated type).
3. **Relay-connection startup conflict.** Under SB 3.2, DGS's
   `ConnectionTypeDefinitionConfigurer` and Spring GraphQL's
   `SchemaMappingInspector` reject the app's hand-defined plural `*Connection`
   types (`No node type for 'ArticlesConnection'`). Fixed by disabling both:
   `dgs.springgraphql.pagination.enabled=false` and
   `spring.graphql.schema.inspection.enabled=false`.
4. **JJWT 0.11 → 0.12 API.** `parserBuilder()→parser()`,
   `setSigningKey→verifyWith`, `parseClaimsJws→parseSignedClaims`,
   `getBody→getPayload`, `setSubject/setExpiration→subject/expiration`. JJWT 0.12
   also enforces a ≥512-bit key for HS512, which broke the test's short secret
   (fixed via a longer secret / `Keys.hmacShaKeyFor`).
5. **spotless on Gradle 8.5.** Strict implicit task-dependency validation
   rejects scanning the project root; spotless target was narrowed to
   `src/**/*.java`. The old spotless `6.2.1` also crashes under JDK 17+, so it
   was bumped to `6.25.0`.
6. **Spring Framework 6 signature change.**
   `CustomizeExceptionHandler.handleMethodArgumentNotValid` parameter
   `HttpStatus → HttpStatusCode`.

---

## Build/CI status

There is **no repository build workflow** in this fork, so "passing" means a
clean local `./gradlew clean build` + `./gradlew test`. All three branches were
independently re-built and re-tested on Java 21 and are green (68/68).

The PR checks that *do* run are org-level gates:
- ✅ `license/snyk`, ✅ `Devin Review`
- ❌ `security/snyk` — **fails on all three PRs, and this is pre-existing, not
  introduced by the migration.** `master` (SB 2.6.3, EOL) reports **73 issues
  incl. Critical RCEs** (e.g. Spring4Shell `SNYK-JAVA-ORGSPRINGFRAMEWORK-2436751`).
  After the upgrade the count drops to **45** (Snyk `--severity-threshold=high`).
  The remaining findings are CVEs in Spring Boot 3.2.12's own managed transitive
  deps (tomcat-embed, spring-security, spring-graphql). Clearing them would
  require moving past the user-specified target (SB 3.2.x) to a newer 3.3/3.4/3.5
  line, which is out of scope for this migration.

---

## Recommendation

For **this codebase**, **OpenRewrite (`upgrade/openrewrite`, #578)** is the best
primary strategy:

- It eliminates essentially all of the mechanical, error-prone bulk
  (`javax→jakarta` across 21 files and the full Spring Security 6 rewrite) — the
  changes most tedious and easiest to get subtly wrong by hand — reducing manual
  source edits to **~18 LOC**.
- The manual residue it cannot handle (Netflix DGS, JJWT, the GraphQL relay
  conflicts) is **identical work in every approach**, so OpenRewrite strictly
  dominates on the parts it *can* automate without making the hard parts worse.

Caveats that matter:
- **The hard core dominates total effort regardless of approach.** The DGS 8 /
  Spring-GraphQL relay-connection issues and the JJWT key-strength change were
  the real time sink in all three branches. OpenRewrite shrinks the *cheap* part,
  not the *expensive* part.
- OpenRewrite's diff is the **largest and noisiest** (broad reformatting), which
  can complicate code review on bigger codebases.

Secondary guidance:
- **Incremental (#576)** is the **safest** choice and the right default for
  large or business-critical codebases where validating at each Spring Boot
  version (and being able to bisect regressions) outweighs the extra cycles. For
  a codebase this small (~116 files) its safety benefit is marginal relative to
  its cost.
- **Direct Jump (#577)** is viable and fast when you are confident, but it had
  the **most manual LOC (~108)** and required **3 separate debug rounds**, making
  failures the hardest to localize. Best reserved for small codebases or
  throwaway spikes.

**Bottom line:** use **OpenRewrite to do the mechanical migration**, then expect
to hand-fix DGS/JJWT/GraphQL exactly as you would in any approach. Fall back to
the **incremental** path when the codebase is large enough that per-version
validation is worth the extra time.

---

## Appendix: deviations from the prescribed plan (versions that don't exist)

Several versions named in the original plan are not resolvable in public
registries; the closest compatible versions were used and noted in each PR:

- `com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter:7.6.0` → does not
  exist; the incremental branch used `graphql-dgs-spring-boot-starter` at the
  same coordinates/version for the 3.0 stage.
- DGS codegen plugin `6.4.0` → does not exist (latest 6.x is `6.3.0`); branches
  used `6.3.0` (or `7.0.3`, which aligns with DGS 8.x).
- OpenRewrite plugin `7.3.1` / `rewrite-recipe-bom:2.25.0` → used `6.29.4` /
  `2.23.2`. OpenRewrite also cannot run on Gradle 8.5/Java 21, so `rewriteRun`
  was executed on Gradle 7.6.x/Java 17 before bumping the wrapper.
- `org.flywaydb:flyway-database-sqlite` → has no version under SB 3.2's managed
  Flyway 9.22.3 (it is a Flyway 10.x artifact) and was **not needed** — Flyway
  9.22.3 has built-in SQLite support.
