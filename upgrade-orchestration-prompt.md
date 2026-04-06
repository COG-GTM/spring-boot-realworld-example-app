# Java 17 + Spring Boot 3 Upgrade Orchestration Prompt

## Section 1: Overview

This is a **master orchestration prompt** for upgrading [`COG-GTM/spring-boot-realworld-example-app`](https://github.com/COG-GTM/spring-boot-realworld-example-app) from **Java 11 / Spring Boot 2.6.3** to **Java 17 / Spring Boot 3.2+**.

The upgrade is broken into 9 phases with explicit dependency chains. This prompt uses **Devin Child Sessions** to maximize parallelism — launching independent work immediately while tracking blocked work as Jira tasks (or GitHub issues) for future execution.

### Dependency Graph

```
Phase 1 (Build Toolchain) ──┬──> Phase 2 (javax → jakarta) ──┬──> Phase 3 (Security Rewrite)
                             │                                 ├──> Phase 5 (DGS Upgrade)
                             │                                 ├──> Phase 6 (JJWT Update)
                             │                                 ├──> Phase 8 (Test Deps)
                             │                                 │
Phase 4 (Joda-Time) ─────────┼─────────────────────────────────┼──> Phase 7 (MyBatis/Flyway/SQLite)
                             │                                 │         (blocked on Phase 2 + Phase 4)
                             │                                 │
                             └─────────────────────────────────┴──> Phase 9 (Validation)
                                                                        (blocked on ALL phases)
```

---

## Section 2: Immediate Child Sessions (Non-Blocked Work — Launch Right Away)

These child sessions should be launched immediately because they have no blocking dependencies.

---

### Child Session 1: Phase 1 — Build Toolchain Update

**Branch:** `upgrade/phase1-build-toolchain`
**Blocked on:** Nothing — can start immediately

#### Instructions

1. **Upgrade Gradle wrapper** to 8.5+:
   ```bash
   ./gradlew wrapper --gradle-version=8.5
   ```

2. **Update `build.gradle` plugins** (lines 1-11):
   | Plugin | Current Version | Target Version |
   |--------|----------------|----------------|
   | `org.springframework.boot` | `2.6.3` | `3.2.5` |
   | `io.spring.dependency-management` | `1.0.11.RELEASE` | `1.1.4` |
   | `com.netflix.dgs.codegen` | `5.0.6` | `6.2.1` |
   | `com.diffplug.spotless` | `6.2.1` | `6.25.0` |

3. **Update Java compatibility** (lines 10-11):
   ```groovy
   sourceCompatibility = '17'
   targetCompatibility = '17'
   ```

4. **Update `build.gradle` dependencies** (lines 33-57):
   | Dependency | Current | Target |
   |-----------|---------|--------|
   | `mybatis-spring-boot-starter` | `2.2.2` | `3.0.3` |
   | `graphql-dgs-spring-boot-starter:4.9.21` | (artifact change) | `graphql-dgs-spring-graphql-starter:8.5.0` |
   | `jjwt-api` | `0.11.2` | `0.12.5` |
   | `jjwt-impl` | `0.11.2` | `0.12.5` |
   | `jjwt-jackson` | `0.11.2` | `0.12.5` |
   | `sqlite-jdbc` | `3.36.0.3` | `3.45.3.0` |
   | `rest-assured` (all modules) | `4.5.1` | `5.4.0` |
   | `mybatis-spring-boot-starter-test` | `2.2.2` | `3.0.3` |

5. **Remove** the `joda-time:joda-time:2.10.13` dependency line entirely.

6. **Commit** all changes to branch `upgrade/phase1-build-toolchain`.

> **NOTE:** The project will **NOT** compile after this phase — that is expected. Compilation will be fixed by subsequent phases.

---

### Child Session 2: Phase 4 — Joda-Time to java.time Migration

**Branch:** `upgrade/phase4-joda-time` (based off `main`, NOT off Phase 1's branch)
**Blocked on:** Nothing — can start immediately (this is purely a code-level refactor that doesn't depend on dependency versions to compile)

#### Instructions

Replace all `org.joda.time.DateTime` usages with `java.time.ZonedDateTime` or `java.time.Instant` across ~16 files.

##### Key files to modify:

1. **`src/main/java/io/spring/JacksonCustomizations.java`**
   - Remove the custom Joda `DateTimeSerializer`
   - Register `JavaTimeModule` from `jackson-datatype-jsr310` instead

2. **`src/main/java/io/spring/graphql/ArticleDatafetcher.java`**
   - Replace `org.joda.time.format.ISODateTimeFormat` with `java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME`

3. **All domain/core classes using `DateTime`:**
   - `Article.java`
   - `Comment.java`
   - `ArticleData.java`
   - `CommentData.java`
   - `DateTimeCursor.java`

4. **`src/main/java/io/spring/infrastructure/mybatis/DateTimeHandler.java`**
   - Rewrite the MyBatis type handler for `java.time` types

5. **All query service classes referencing Joda-Time**

6. **All test files referencing Joda-Time:**
   - `ArticleApiTest.java`
   - `ArticlesApiTest.java`
   - `ArticleQueryServiceTest.java`
   - `TestHelper.java`

7. **Remove** the `joda-time` import from `build.gradle` (coordinate with Phase 1 branch).

8. **Commit** all changes to branch `upgrade/phase4-joda-time`.

---

## Section 3: Blocked Child Sessions — Create Jira Tasks

For each of the following blocked phases, launch a child session that creates a **Jira task** (or GitHub issue if Jira is not configured) with full implementation details so the work can be tracked and picked up once dependencies are met.

---

### Child Session 3: Create Jira Task for Phase 2 — javax to jakarta Namespace Migration

**Blocked on:** Phase 1 (build toolchain must be updated first)

#### Jira Task Title: "Migrate javax to jakarta namespace"

#### Task Description

##### Find-and-replace `javax.servlet` to `jakarta.servlet`:
- `src/main/java/io/spring/api/security/JwtTokenFilter.java` (lines 8-11)
- Any other servlet-using files

##### Find-and-replace `javax.validation` to `jakarta.validation` in ~15+ files:

**Parameter/validation classes:**
- `RegisterParam.java`
- `UpdateUserParam.java`
- `NewArticleParam.java`

**Validator constraint files:**
- `DuplicatedArticleConstraint.java` / `DuplicatedArticleValidator.java`
- `DuplicatedEmailConstraint.java` / `DuplicatedEmailValidator.java`
- `DuplicatedUsernameConstraint.java` / `DuplicatedUsernameValidator.java`

**API controllers:**
- `UsersApi.java`
- `CommentsApi.java`
- `ArticleApi.java`
- `ArticlesApi.java`
- `CurrentUserApi.java`

**Service files:**
- `UserService.java`
- `ArticleCommandService.java`

**Exception handlers:**
- `CustomizeExceptionHandler.java`
- `GraphQLCustomizeExceptionHandler.java`

> **IMPORTANT:** `javax.crypto.*` in `DefaultJwtService.java` does **NOT** change — it is a JDK package, not Jakarta.

##### Acceptance Criteria
- All `javax.servlet` and `javax.validation` imports replaced with `jakarta.*` equivalents
- Code compiles against Spring Boot 3.2 dependencies

---

### Child Session 4: Create Jira Task for Phase 3 — Spring Security Rewrite

**Blocked on:** Phase 2

#### Jira Task Title: "Rewrite WebSecurityConfig for Spring Security 6"

#### Task Description

Rewrite `src/main/java/io/spring/api/security/WebSecurityConfig.java` (currently 83 lines):

1. **Remove** `extends WebSecurityConfigurerAdapter`
2. **Replace** `configure(HttpSecurity)` override with:
   ```java
   @Bean
   SecurityFilterChain securityFilterChain(HttpSecurity http)
   ```
3. **Replace** all `antMatchers(...)` with `requestMatchers(...)`
4. **Use lambda-based DSL:**
   ```java
   http.csrf(csrf -> csrf.disable())
   http.cors(Customizer.withDefaults())
   ```
5. **Replace** `authorizeRequests()` with `authorizeHttpRequests()`

##### Full rewritten class skeleton:

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtTokenFilter jwtTokenFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS).permitAll()
                .requestMatchers(HttpMethod.GET, "/articles/feed").authenticated()
                .requestMatchers(HttpMethod.POST, "/users", "/users/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/articles/**", "/profiles/**", "/tags").permitAll()
                .requestMatchers("/graphql", "/graphiql/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }
}
```

##### Acceptance Criteria
- Security config compiles and all endpoint authorization rules are preserved
- No use of deprecated `WebSecurityConfigurerAdapter`, `antMatchers`, or `authorizeRequests`

---

### Child Session 5: Create Jira Task for Phase 5 — Netflix DGS Framework Upgrade

**Blocked on:** Phase 2

#### Jira Task Title: "Upgrade Netflix DGS to 8.x for Spring Boot 3"

#### Task Description

- Dependency already changed in Phase 1 from `graphql-dgs-spring-boot-starter` to `graphql-dgs-spring-graphql-starter`
- Review all **9 DGS component files** in `src/main/java/io/spring/graphql/` for API compatibility
- Verify the following annotations and classes still work with DGS 8.x:
  - `@DgsComponent`
  - `@DgsQuery`
  - `@DgsMutation`
  - `@DgsData`
  - `InputArgument`
  - `DgsDataFetchingEnvironment`
- May need to add the following to `application.properties`:
  ```properties
  spring.graphql.graphiql.enabled=true
  spring.graphql.path=/graphql
  ```
- Codegen task in `build.gradle` (lines 69-72) should remain compatible but verify

##### Acceptance Criteria
- GraphQL endpoint responds correctly
- GraphiQL UI loads
- All datafetchers resolve correctly

---

### Child Session 6: Create Jira Task for Phase 6 — JJWT API Update

**Blocked on:** Phase 2

#### Jira Task Title: "Update JJWT API from 0.11.x to 0.12.x"

#### Task Description

Update `src/main/java/io/spring/infrastructure/service/DefaultJwtService.java` (lines 17-54).

Replace deprecated JJWT 0.11.x methods with 0.12.x equivalents:

| Old (0.11.x) | New (0.12.x) |
|--------------|-------------|
| `Jwts.builder().setSubject(...)` | `Jwts.builder().subject(...)` |
| `.setExpiration(...)` | `.expiration(...)` |
| `Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)` | `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` |
| `claimsJws.getBody().getSubject()` | `claimsJws.getPayload().getSubject()` |
| `SignatureAlgorithm.HS512` enum | `Jwts.SIG.HS512` |

##### Acceptance Criteria
- JWT token creation and parsing works
- Auth flow (register/login) succeeds end-to-end

---

### Child Session 7: Create Jira Task for Phase 7 — MyBatis + Flyway + SQLite

**Blocked on:** Phase 2 + Phase 4 (for DateTimeHandler)

#### Jira Task Title: "Verify MyBatis 3, Flyway 9, SQLite compatibility"

#### Task Description

1. **MyBatis:** Starter already bumped to `3.0.3` in Phase 1; verify all 6 XML mapper files still work:
   - `src/main/resources/mapper/*.xml`
   - `src/main/java/io/spring/MyBatisConfig.java` should remain compatible

2. **Flyway:** Auto-upgrades to 9.x+ via Spring Boot dependency management; verify existing migration works:
   - `src/main/resources/db/migration/V1__create_tables.sql`
   - **RISK:** Flyway 9+ may have dropped built-in SQLite support — may need to add `org.flywaydb:flyway-database-sqlite` dependency

3. **SQLite JDBC:** Already bumped to `3.45.3.0` in Phase 1

4. **DateTimeHandler:** `DateTimeHandler.java` must be updated to use `java.time` types (depends on Phase 4 decisions about `ZonedDateTime` vs `Instant`)

##### Acceptance Criteria
- Application starts successfully
- Flyway migration runs without errors
- All database operations (CRUD) work correctly

---

### Child Session 8: Create Jira Task for Phase 8 — Test Dependency Updates

**Blocked on:** Phase 2

#### Jira Task Title: "Update test dependencies for Spring Boot 3"

#### Task Description

1. **Rest Assured** already bumped to `5.4.0` in Phase 1
2. Review all test files in `src/test/java/io/spring/` for `javax.*` imports (should be handled by Phase 2)
3. Verify `spring-mock-mvc` module works with Spring 6's MockMvc
4. `mybatis-spring-boot-starter-test` already bumped to `3.0.3`
5. Ensure all test base classes (`TestWithCurrentUser`, `DbTestBase`) work with updated dependencies

##### Acceptance Criteria
- `./gradlew test` passes with all tests green
- No remaining `javax.*` imports in test files

---

### Child Session 9: Create Jira Task for Phase 9 — Integration Testing & Validation

**Blocked on:** ALL phases (1-8)

#### Jira Task Title: "Integration testing and validation of full upgrade"

#### Task Description

1. **Build verification:**
   ```bash
   ./gradlew clean build
   ```
   Fix any remaining compilation errors.

2. **Test suite:**
   ```bash
   ./gradlew test
   ```
   Fix any test failures.

3. **Runtime verification:**
   ```bash
   ./gradlew bootRun
   ```
   Manually verify:
   - REST endpoints: `GET /tags` returns tag list
   - GraphQL: GraphiQL UI loads at `/graphiql`
   - Full auth flow: register -> login -> create article -> comment

4. **Code formatting:**
   ```bash
   ./gradlew spotlessCheck
   ```

5. **Build cleanup:**
   - Verify `clean` task still deletes `dev.db` (build.gradle lines 63-67)

##### Acceptance Criteria
- Full application boots on Java 17
- All tests pass (`./gradlew test`)
- All REST and GraphQL endpoints are functional
- Code formatting passes (`./gradlew spotlessCheck`)

---

## Section 4: Summary Table

| # | Phase | Type | Blocked On | Branch / Task |
|---|-------|------|------------|---------------|
| 1 | Build Toolchain | Immediate work | None | `upgrade/phase1-build-toolchain` |
| 2 | Joda-Time -> java.time | Immediate work | None | `upgrade/phase4-joda-time` |
| 3 | javax -> jakarta | Jira task | Phase 1 | JIRA: "Migrate javax to jakarta namespace" |
| 4 | Security Rewrite | Jira task | Phase 2 | JIRA: "Rewrite WebSecurityConfig for Spring Security 6" |
| 5 | DGS Upgrade | Jira task | Phase 2 | JIRA: "Upgrade Netflix DGS to 8.x for Spring Boot 3" |
| 6 | JJWT Update | Jira task | Phase 2 | JIRA: "Update JJWT API from 0.11.x to 0.12.x" |
| 7 | MyBatis/Flyway/SQLite | Jira task | Phase 2 + 4 | JIRA: "Verify MyBatis 3, Flyway 9, SQLite compatibility" |
| 8 | Test Deps | Jira task | Phase 2 | JIRA: "Update test dependencies for Spring Boot 3" |
| 9 | Validation | Jira task | ALL | JIRA: "Integration testing and validation of full upgrade" |
