# Spring Boot 3.2 / Java 17 Upgrade Plan

> **This document is a self-contained Devin session prompt.** Hand it to an autonomous AI coding agent to scope out and execute the upgrade end-to-end.

---

## Objective

Upgrade the Spring Boot RealWorld example application from **Java 11 / Spring Boot 2.6.3** to **Java 17 / Spring Boot 3.2.x**. Ensure all tests pass and the application compiles and runs successfully after the upgrade.

---

## Phase 1 — Gradle Build File Updates (`build.gradle`)

Current state of `build.gradle` (lines 1–11):

```groovy
plugins {
    id 'org.springframework.boot' version '2.6.3'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    id "com.netflix.dgs.codegen" version "5.0.6"
    id "com.diffplug.spotless" version "6.2.1"
}

version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'
targetCompatibility = '11'
```

### Instructions

1. Change `sourceCompatibility` and `targetCompatibility` from `'11'` to `'17'`.
2. Change `org.springframework.boot` plugin version from `'2.6.3'` to `'3.2.0'` (or latest 3.2.x).
3. Change `io.spring.dependency-management` plugin version from `'1.0.11.RELEASE'` to `'1.1.4'`.
4. Change `com.netflix.dgs.codegen` plugin version from `"5.0.6"` to `"6.2.1"` or latest 6.x compatible with Spring Boot 3.
5. Update `com.diffplug.spotless` to latest 6.x (e.g., `"6.25.0"`).

### Dependency Version Changes

Current dependencies (`build.gradle` lines 33–56):

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-hateoas'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:2.2.2'
    implementation 'com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter:4.9.21'
    implementation 'org.flywaydb:flyway-core'
    implementation 'io.jsonwebtoken:jjwt-api:0.11.2'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.2',
                'io.jsonwebtoken:jjwt-jackson:0.11.2'
    implementation 'joda-time:joda-time:2.10.13'
    implementation 'org.xerial:sqlite-jdbc:3.36.0.3'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'io.rest-assured:rest-assured:4.5.1'
    testImplementation 'io.rest-assured:json-path:4.5.1'
    testImplementation 'io.rest-assured:xml-path:4.5.1'
    testImplementation 'io.rest-assured:spring-mock-mvc:4.5.1'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter-test:2.2.2'
}
```

Apply these version bumps:

| Dependency | Current | Target |
|---|---|---|
| `mybatis-spring-boot-starter` | `2.2.2` | `3.0.3` |
| `mybatis-spring-boot-starter-test` | `2.2.2` | `3.0.3` |
| `graphql-dgs-spring-boot-starter` | `4.9.21` | `7.6.0` or latest 7.x+ (Jakarta namespace compatible). Also change the artifact to `com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter` if needed for Spring Boot 3 compatibility. |
| `jjwt-api`, `jjwt-impl`, `jjwt-jackson` | `0.11.2` | `0.12.5` |
| `rest-assured`, `json-path`, `xml-path`, `spring-mock-mvc` | `4.5.1` | `5.4.0` |
| `sqlite-jdbc` | `3.36.0.3` | `3.45.1.0` or latest 3.x |
| `joda-time` | `2.10.13` | Consider removing entirely and replacing usages with `java.time`, or upgrade to `2.12.7` |
| `flyway-core` | Managed by Spring Boot | Verify SQLite support. May need to add `org.flywaydb:flyway-database-sqlite` if Flyway 10.x modularized database support. |

---

## Phase 2 — `javax.*` → `jakarta.*` Namespace Migration

Spring Boot 3 uses Jakarta EE 9+ which renames the `javax.*` namespace to `jakarta.*`. Search the entire `src/` directory for all files importing `javax.servlet.*`, `javax.validation.*`, `javax.annotation.*`, `javax.transaction.*`, and `javax.persistence.*`, then rename them to their `jakarta.*` equivalents.

### Known Affected Files (20 files total — search for all)

1. **`src/main/java/io/spring/api/security/JwtTokenFilter.java`** — lines 8–11 currently import:
   ```java
   import javax.servlet.FilterChain;
   import javax.servlet.ServletException;
   import javax.servlet.http.HttpServletRequest;
   import javax.servlet.http.HttpServletResponse;
   ```
   Change to `jakarta.servlet.*`.

2. **`src/main/java/io/spring/application/user/RegisterParam.java`** — lines 4–5 currently import:
   ```java
   import javax.validation.constraints.Email;
   import javax.validation.constraints.NotBlank;
   ```
   Change to `jakarta.validation.constraints.*`.

3. **`src/main/java/io/spring/application/user/DuplicatedUsernameValidator.java`** — lines 4–5:
   ```java
   import javax.validation.ConstraintValidator;
   import javax.validation.ConstraintValidatorContext;
   ```
   Change to `jakarta.validation.*`.

4. **`src/main/java/io/spring/application/user/DuplicatedEmailValidator.java`** — lines 4–5:
   ```java
   import javax.validation.ConstraintValidator;
   import javax.validation.ConstraintValidatorContext;
   ```
   Change to `jakarta.validation.*`.

5. **`src/main/java/io/spring/application/user/DuplicatedUsernameConstraint.java`** — lines 5–6:
   ```java
   import javax.validation.Constraint;
   import javax.validation.Payload;
   ```
   Change to `jakarta.validation.*`.

6. **`src/main/java/io/spring/application/user/DuplicatedEmailConstraint.java`** — lines 5–6:
   ```java
   import javax.validation.Constraint;
   import javax.validation.Payload;
   ```
   Change to `jakarta.validation.*`.

7. **`src/main/java/io/spring/application/user/UserService.java`** — lines 7–10:
   ```java
   import javax.validation.Valid (and related)
   ```
   Change to `jakarta.validation.*`.

8. **`src/main/java/io/spring/application/user/UpdateUserParam.java`** — line 4:
   ```java
   import javax.validation.constraints.Email;
   ```
   Change to `jakarta.validation.constraints.*`.

9. **`src/main/java/io/spring/application/article/DuplicatedArticleConstraint.java`** — lines 8–9:
   ```java
   import javax.validation.Constraint;
   import javax.validation.Payload;
   ```
   Change to `jakarta.validation.*`.

10. **`src/main/java/io/spring/application/article/NewArticleParam.java`** — line 5:
    ```java
    import javax.validation.constraints.NotBlank;
    ```
    Change to `jakarta.validation.constraints.*`.

11. **`src/main/java/io/spring/application/article/DuplicatedArticleValidator.java`** — lines 5–6:
    ```java
    import javax.validation.ConstraintValidator;
    import javax.validation.ConstraintValidatorContext;
    ```
    Change to `jakarta.validation.*`.

12. **`src/main/java/io/spring/application/article/ArticleCommandService.java`** — line 6:
    ```java
    import javax.validation.Valid;
    ```
    Change to `jakarta.validation.*`.

13. **`src/main/java/io/spring/graphql/UserMutation.java`** — line 21:
    ```java
    import javax.validation.* (or related)
    ```
    Change to `jakarta.validation.*`.

14. **`src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java`** — lines 20–21:
    ```java
    import javax.validation.ConstraintViolation;
    import javax.validation.ConstraintViolationException;
    ```
    Change to `jakarta.validation.*`.

15. **`src/main/java/io/spring/api/ArticleApi.java`** — line 15:
    ```java
    import javax.validation.Valid;
    ```
    Change to `jakarta.validation.*`.

16. **`src/main/java/io/spring/api/UsersApi.java`** — lines 18–20:
    ```java
    import javax.validation.Valid (and related)
    ```
    Change to `jakarta.validation.*`.

17. **`src/main/java/io/spring/api/CommentsApi.java`** — lines 17–18:
    ```java
    import javax.validation.Valid (and related)
    ```
    Change to `jakarta.validation.*`.

18. **`src/main/java/io/spring/api/ArticlesApi.java`** — line 10:
    ```java
    import javax.validation.Valid;
    ```
    Change to `jakarta.validation.*`.

19. **`src/main/java/io/spring/api/CurrentUserApi.java`** — line 12:
    ```java
    import javax.validation.Valid;
    ```
    Change to `jakarta.validation.*`.

20. **`src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java`** — lines 10–11:
    ```java
    import javax.validation.ConstraintViolation;
    import javax.validation.ConstraintViolationException;
    ```
    Change to `jakarta.validation.*`.

> **Do NOT change** `javax.crypto.*` imports (e.g., in `src/main/java/io/spring/infrastructure/service/DefaultJwtService.java` lines 11–12). These are part of the JDK, not Jakarta EE.

---

## Phase 3 — Spring Security Rewrite

**File:** `src/main/java/io/spring/api/security/WebSecurityConfig.java`

Current code (lines 23–65) extends `WebSecurityConfigurerAdapter`, which was **removed** in Spring Security 6 (used by Spring Boot 3.2). The entire class must be rewritten:

1. Remove `extends WebSecurityConfigurerAdapter`.
2. Remove the `@Override protected void configure(HttpSecurity http)` method.
3. Add a `@Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception` method.
4. Replace `.antMatchers()` with `.requestMatchers()`.
5. Replace `.authorizeRequests()` with `.authorizeHttpRequests()`.
6. Use the lambda DSL style throughout.
7. The `jwtTokenFilter()` bean and `passwordEncoder()` bean can remain as `@Bean` methods.

### Target Implementation

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS).permitAll()
                .requestMatchers("/graphiql", "/graphql").permitAll()
                .requestMatchers(HttpMethod.GET, "/articles/feed").authenticated()
                .requestMatchers(HttpMethod.POST, "/users", "/users/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/articles/**", "/profiles/**", "/tags").permitAll()
                .anyRequest().authenticated()
            );
        http.addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

Also check if there are any other classes that reference `WebSecurityConfigurerAdapter` or use deprecated Spring Security APIs.

---

## Phase 4 — JJWT 0.11.x → 0.12.x API Migration

**File:** `src/main/java/io/spring/infrastructure/service/DefaultJwtService.java` (lines 18–49)

Current code uses deprecated/removed JJWT 0.11.x APIs. Apply these changes:

1. Remove `import io.jsonwebtoken.SignatureAlgorithm;` — the `SignatureAlgorithm` enum was removed.
2. Replace `signatureAlgorithm = SignatureAlgorithm.HS512;` with `Jwts.SIG.HS512` usage.
3. Replace `new SecretKeySpec(secret.getBytes(), signatureAlgorithm.getJcaName())` with `new SecretKeySpec(secret.getBytes(), "HmacSHA512")` or use `Keys.hmacShaKeyFor(secret.getBytes())`.
4. In `toToken()`:
   - `.setSubject(user.getId())` → `.subject(user.getId())`
   - `.setExpiration(expireTimeFromNow())` → `.expiration(expireTimeFromNow())`
   - `.signWith(signingKey)` → `.signWith(signingKey, Jwts.SIG.HS512)`
5. In `getSubFromToken()`:
   - `Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token)` → `Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token)`
   - `.getBody().getSubject()` → `.getPayload().getSubject()`

---

## Phase 5 — Netflix DGS GraphQL Upgrade

The DGS framework starter `4.9.21` is NOT compatible with Spring Boot 3. Upgrade to DGS 7.x+ or 8.x+.

- In `build.gradle`, change `com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter:4.9.21` to the Spring Boot 3 compatible artifact. DGS 7+ for Spring Boot 3 uses `com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter:7.6.0` (or latest).
- Update the codegen plugin from `5.0.6` to `6.x+`.
- Search all GraphQL-related source files under `src/main/java/io/spring/graphql/` for any DGS API changes (annotation changes, context object changes, etc.).
- Verify the DGS codegen Gradle task (defined at `build.gradle` lines 69–72) still generates code correctly after the upgrade.

---

## Phase 6 — Flyway + SQLite Compatibility

**File:** `src/main/resources/application.properties` (lines 1–2):

```properties
spring.datasource.url=jdbc:sqlite:dev.db
spring.datasource.driver-class-name=org.sqlite.JDBC
```

- Spring Boot 3.2 ships with Flyway 9.x or 10.x. Flyway 10.x modularized database support — SQLite may require adding an explicit `org.flywaydb:flyway-database-sqlite` dependency.
- Check the migration script at `src/main/resources/db/migration/V1__create_tables.sql` for compatibility.
- Verify that the SQLite dialect/driver works with the upgraded Flyway and Spring Boot versions.

---

## Phase 7 — Optional: Replace Joda-Time with `java.time`

The project uses `joda-time:joda-time:2.10.13`. Since Java 17 has mature `java.time` APIs, search for all usages of Joda-Time across the codebase and replace them with `java.time` equivalents (`Instant`, `LocalDateTime`, `ZonedDateTime`, `Duration`, etc.). Then remove the Joda-Time dependency from `build.gradle`. If this is too risky, at minimum upgrade Joda-Time to `2.12.7`.

---

## Phase 8 — Test Updates

- Update REST Assured from `4.5.1` to `5.4.0`. REST Assured 5.x has some package changes — verify all test imports.
- Run `./gradlew test` after each phase to catch regressions early.
- Check all test files under `src/test/java/` for `javax.*` imports and update them to `jakarta.*`.
- Verify MyBatis test starter `3.0.3` compatibility.

---

## Phase 9 — Verification

After all changes:

1. Run `./gradlew clean build` — ensure it compiles with zero errors.
2. Run `./gradlew test` — ensure all tests pass.
3. Run the application with `./gradlew bootRun` and verify it starts without errors.
4. Test key endpoints manually or note any remaining issues.

---

## Execution Order

The recommended order is:

1. **Phase 1** (build.gradle changes) — get it compiling first
2. **Phase 2** (javax → jakarta) — mechanical find-and-replace
3. **Phase 3** (Spring Security rewrite) — biggest code change
4. **Phase 4** (JJWT migration) — contained to one file
5. **Phase 5** (DGS upgrade) — may require exploration of DGS 7.x API
6. **Phase 6** (Flyway/SQLite) — verify after boot
7. **Phase 7** (Joda-Time, optional)
8. **Phase 8** (Test updates)
9. **Phase 9** (Full verification)

**Make a separate commit for each phase** so changes can be reviewed and reverted independently.
