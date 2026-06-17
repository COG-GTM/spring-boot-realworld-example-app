# Spring Boot 2.6.3 → 3.4.x Migration Plan

## Current State

| Component | Current Version |
|---|---|
| Java | 11 (`sourceCompatibility = '11'`) |
| Spring Boot | 2.6.3 |
| Spring Dependency Management plugin | 1.0.11.RELEASE |
| Gradle wrapper | 7.4 |
| Netflix DGS Codegen plugin | 5.0.6 |
| Spotless plugin | 6.2.1 |
| MyBatis Spring Boot Starter | 2.2.2 |
| Netflix DGS | 4.9.21 |
| Flyway | (managed by Boot BOM) |
| jjwt (jsonwebtoken) | 0.11.2 |
| Joda-Time | 2.10.13 |
| SQLite JDBC | 3.36.0.3 |
| Lombok | (managed by Boot BOM) |
| REST Assured | 4.5.1 |
| Database | SQLite (file `dev.db`, in-memory for tests) |

## Target State

| Component | Target Version |
|---|---|
| Java | 17 (minimum for Boot 3.x) |
| Spring Boot | 3.4.1 (latest stable 3.4.x) |
| Spring Dependency Management plugin | 1.1.7 |
| Gradle wrapper | 8.11.1 (latest 8.x) |
| Netflix DGS Codegen plugin | 7.0.3 |
| Spotless plugin | 7.0.2 |
| MyBatis Spring Boot Starter | 3.0.4 |
| Netflix DGS | 9.1.3 (Spring Boot 3 compatible) |
| Flyway | (managed by Boot BOM) |
| jjwt (jsonwebtoken) | 0.12.6 |
| Joda-Time | 2.10.13 (unchanged, optional modernization) |
| SQLite JDBC | 3.47.1.0 |
| Lombok | (managed by Boot BOM) |
| REST Assured | 5.5.0 |

---

## Codebase Inventory

### Source Files (src/main/java) — 53 files

**Root config (4 files):**
- `io/spring/JacksonCustomizations.java` — Joda `DateTime` serializer
- `io/spring/MyBatisConfig.java` — `@EnableTransactionManagement`
- `io/spring/RealWorldApplication.java` — `@SpringBootApplication` entry point
- `io/spring/Util.java` — utility

**API / Web layer (12 files):**
- `api/ArticleApi.java` — `javax.validation.Valid`
- `api/ArticleFavoriteApi.java`
- `api/ArticlesApi.java` — `javax.validation.Valid`
- `api/CommentsApi.java` — `javax.validation.Valid`, `javax.validation.constraints.NotBlank`
- `api/CurrentUserApi.java` — `javax.validation.Valid`
- `api/ProfileApi.java`
- `api/TagsApi.java`
- `api/UsersApi.java` — `javax.validation.Valid`, `javax.validation.constraints.Email`, `javax.validation.constraints.NotBlank`
- `api/exception/` — 6 files (CustomizeExceptionHandler uses `javax.validation.ConstraintViolation*`)
- `api/security/JwtTokenFilter.java` — `javax.servlet.*` imports
- `api/security/WebSecurityConfig.java` — extends `WebSecurityConfigurerAdapter` (removed in Spring Security 6)

**Application / query layer (16 files):**
- `application/article/ArticleCommandService.java` — `javax.validation.Valid`
- `application/article/DuplicatedArticleConstraint.java` — `javax.validation.Constraint`, `javax.validation.Payload`
- `application/article/DuplicatedArticleValidator.java` — `javax.validation.ConstraintValidator*`
- `application/article/NewArticleParam.java` — `javax.validation.constraints.NotBlank`
- `application/article/UpdateArticleParam.java`
- `application/user/DuplicatedEmailConstraint.java` — `javax.validation.Constraint`, `javax.validation.Payload`
- `application/user/DuplicatedEmailValidator.java` — `javax.validation.ConstraintValidator*`
- `application/user/DuplicatedUsernameConstraint.java` — `javax.validation.Constraint`, `javax.validation.Payload`
- `application/user/DuplicatedUsernameValidator.java` — `javax.validation.ConstraintValidator*`
- `application/user/RegisterParam.java` — `javax.validation.constraints.Email`, `javax.validation.constraints.NotBlank`
- `application/user/UpdateUserParam.java` — `javax.validation.constraints.Email`
- `application/user/UserService.java` — `javax.validation.Constraint`, `javax.validation.ConstraintValidator*`, `javax.validation.Valid`
- Other data/query classes (no javax imports)

**Core domain (9 files):** No javax imports. Uses Joda-Time `DateTime`.

**Infrastructure (16 files):**
- `infrastructure/service/DefaultJwtService.java` — `javax.crypto.SecretKey`, `javax.crypto.spec.SecretKeySpec`
- `infrastructure/mybatis/DateTimeHandler.java` — Joda-Time type handler
- MyBatis mappers and read services — no javax imports

**GraphQL layer (12 files):**
- `graphql/UserMutation.java` — `javax.validation.ConstraintViolationException`
- `graphql/exception/GraphQLCustomizeExceptionHandler.java` — `javax.validation.ConstraintViolation*`
- Other datafetchers — no javax imports

### Test Files (src/test/java) — 23 files
No `javax.*` imports in test files. Tests use:
- `@WebMvcTest` (7 API test classes)
- `@SpringBootTest` (2 classes)
- `@MybatisTest` (via `DbTestBase`, 5 repository test classes)
- `@MockBean` (23 occurrences across test files)
- REST Assured `spring-mock-mvc` module

### Configuration Files
- `application.properties` — SQLite datasource, JWT config, MyBatis config
- `application-test.properties` — SQLite in-memory
- `db/migration/V1__create_tables.sql` — Flyway migration
- `schema/schema.graphqls` — GraphQL schema
- `mapper/*.xml` — 11 MyBatis mapper XML files

---

## javax.* Import Inventory (40 occurrences in 21 files)

### Category 1: `javax.validation.*` → `jakarta.validation.*` (34 occurrences in 19 files)

| File | Imports |
|---|---|
| `api/ArticleApi.java` | `javax.validation.Valid` |
| `api/ArticlesApi.java` | `javax.validation.Valid` |
| `api/CommentsApi.java` | `javax.validation.Valid`, `javax.validation.constraints.NotBlank` |
| `api/CurrentUserApi.java` | `javax.validation.Valid` |
| `api/UsersApi.java` | `javax.validation.Valid`, `javax.validation.constraints.Email`, `javax.validation.constraints.NotBlank` |
| `api/exception/CustomizeExceptionHandler.java` | `javax.validation.ConstraintViolation`, `javax.validation.ConstraintViolationException` |
| `application/article/ArticleCommandService.java` | `javax.validation.Valid` |
| `application/article/DuplicatedArticleConstraint.java` | `javax.validation.Constraint`, `javax.validation.Payload` |
| `application/article/DuplicatedArticleValidator.java` | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `application/article/NewArticleParam.java` | `javax.validation.constraints.NotBlank` |
| `application/user/DuplicatedEmailConstraint.java` | `javax.validation.Constraint`, `javax.validation.Payload` |
| `application/user/DuplicatedEmailValidator.java` | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `application/user/DuplicatedUsernameConstraint.java` | `javax.validation.Constraint`, `javax.validation.Payload` |
| `application/user/DuplicatedUsernameValidator.java` | `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext` |
| `application/user/RegisterParam.java` | `javax.validation.constraints.Email`, `javax.validation.constraints.NotBlank` |
| `application/user/UpdateUserParam.java` | `javax.validation.constraints.Email` |
| `application/user/UserService.java` | `javax.validation.Constraint`, `javax.validation.ConstraintValidator`, `javax.validation.ConstraintValidatorContext`, `javax.validation.Valid` |
| `graphql/UserMutation.java` | `javax.validation.ConstraintViolationException` |
| `graphql/exception/GraphQLCustomizeExceptionHandler.java` | `javax.validation.ConstraintViolation`, `javax.validation.ConstraintViolationException` |

### Category 2: `javax.servlet.*` → `jakarta.servlet.*` (4 occurrences in 1 file)

| File | Imports |
|---|---|
| `api/security/JwtTokenFilter.java` | `javax.servlet.FilterChain`, `javax.servlet.ServletException`, `javax.servlet.http.HttpServletRequest`, `javax.servlet.http.HttpServletResponse` |

### Category 3: `javax.crypto.*` — NO CHANGE NEEDED (2 occurrences in 1 file)

| File | Imports |
|---|---|
| `infrastructure/service/DefaultJwtService.java` | `javax.crypto.SecretKey`, `javax.crypto.spec.SecretKeySpec` |

> **Note:** `javax.crypto` is part of the JDK itself (not Jakarta EE) and is NOT renamed. These imports stay as-is.

---

## Child Session Definitions

### Child Session 1: Java & Build Configuration Upgrade

**Scope:** Update `build.gradle` and Gradle wrapper to support Spring Boot 3.4.x.

**Files to modify:**
- `build.gradle`
- `gradle/wrapper/gradle-wrapper.properties`

**Exact changes:**

1. **`build.gradle` line 2:** `id 'org.springframework.boot' version '2.6.3'` → `id 'org.springframework.boot' version '3.4.1'`
2. **`build.gradle` line 3:** `id 'io.spring.dependency-management' version '1.0.11.RELEASE'` → `id 'io.spring.dependency-management' version '1.1.7'`
3. **`build.gradle` line 5:** `id "com.netflix.dgs.codegen" version "5.0.6"` → `id "com.netflix.dgs.codegen" version "7.0.3"`
4. **`build.gradle` line 6:** `id "com.diffplug.spotless" version "6.2.1"` → `id "com.diffplug.spotless" version "7.0.2"`
5. **`build.gradle` line 10-11:** `sourceCompatibility = '11'` / `targetCompatibility = '11'` → `sourceCompatibility = '17'` / `targetCompatibility = '17'`
6. **`build.gradle` line 38:** `'mybatis-spring-boot-starter:2.2.2'` → `'mybatis-spring-boot-starter:3.0.4'`
7. **`build.gradle` line 39:** `'graphql-dgs-spring-boot-starter:4.9.21'` → `'com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter:9.1.3'` (note: artifact name changed for Boot 3)
8. **`build.gradle` line 41:** `'jjwt-api:0.11.2'` → `'jjwt-api:0.12.6'`
9. **`build.gradle` lines 42-43:** `'jjwt-impl:0.11.2'`, `'jjwt-jackson:0.11.2'` → `'jjwt-impl:0.12.6'`, `'jjwt-jackson:0.12.6'`
10. **`build.gradle` line 45:** `'sqlite-jdbc:3.36.0.3'` → `'sqlite-jdbc:3.47.1.0'`
11. **`build.gradle` line 50:** `'rest-assured:4.5.1'` → `'rest-assured:5.5.0'`
12. **`build.gradle` line 51:** `'json-path:4.5.1'` → `'json-path:5.5.0'`
13. **`build.gradle` line 52:** `'xml-path:4.5.1'` → `'xml-path:5.5.0'`
14. **`build.gradle` line 53:** `'spring-mock-mvc:4.5.1'` → `'spring-mock-mvc:5.5.0'`
15. **`build.gradle` line 56:** `'mybatis-spring-boot-starter-test:2.2.2'` → `'mybatis-spring-boot-starter-test:3.0.4'`
16. **`gradle/wrapper/gradle-wrapper.properties`:** `gradle-7.4-bin.zip` → `gradle-8.11.1-bin.zip`

**Dependencies:** None (executes first).

**Acceptance criteria:**
- `./gradlew dependencies` resolves successfully
- All plugin versions are compatible with Spring Boot 3.4.1
- Gradle wrapper downloads and runs on the updated version

---

### Child Session 2: Jakarta EE Namespace Migration (`javax` → `jakarta`)

**Scope:** Replace all `javax.validation.*` and `javax.servlet.*` imports with `jakarta.*` equivalents across 20 source files.

**Files to modify (20 files):**

**`javax.validation.*` → `jakarta.validation.*` (19 files):**
1. `src/main/java/io/spring/api/ArticleApi.java` (line 15)
2. `src/main/java/io/spring/api/ArticlesApi.java` (line 10)
3. `src/main/java/io/spring/api/CommentsApi.java` (lines 17-18)
4. `src/main/java/io/spring/api/CurrentUserApi.java` (line 12)
5. `src/main/java/io/spring/api/UsersApi.java` (lines 18-20)
6. `src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java` (lines 10-11)
7. `src/main/java/io/spring/application/article/ArticleCommandService.java` (line 6)
8. `src/main/java/io/spring/application/article/DuplicatedArticleConstraint.java` (lines 8-9)
9. `src/main/java/io/spring/application/article/DuplicatedArticleValidator.java` (lines 5-6)
10. `src/main/java/io/spring/application/article/NewArticleParam.java` (line 5)
11. `src/main/java/io/spring/application/user/DuplicatedEmailConstraint.java` (lines 5-6)
12. `src/main/java/io/spring/application/user/DuplicatedEmailValidator.java` (lines 4-5)
13. `src/main/java/io/spring/application/user/DuplicatedUsernameConstraint.java` (lines 5-6)
14. `src/main/java/io/spring/application/user/DuplicatedUsernameValidator.java` (lines 4-5)
15. `src/main/java/io/spring/application/user/RegisterParam.java` (lines 4-5)
16. `src/main/java/io/spring/application/user/UpdateUserParam.java` (line 4)
17. `src/main/java/io/spring/application/user/UserService.java` (lines 7-10)
18. `src/main/java/io/spring/graphql/UserMutation.java` (line 21)
19. `src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java` (lines 20-21)

**`javax.servlet.*` → `jakarta.servlet.*` (1 file):**
20. `src/main/java/io/spring/api/security/JwtTokenFilter.java` (lines 8-11)

**DO NOT modify:**
- `infrastructure/service/DefaultJwtService.java` — `javax.crypto.*` is JDK standard, not Jakarta EE

**Exact transformation rule:**
```
import javax.validation.  →  import jakarta.validation.
import javax.servlet.     →  import jakarta.servlet.
```

**Dependencies:** Session 1 must complete first (Jakarta EE 10 jars come from Spring Boot 3.x starters).

**Acceptance criteria:**
- Zero `javax.validation` or `javax.servlet` imports remain in `src/main/java/`
- `javax.crypto.*` imports are untouched
- `./gradlew compileJava` succeeds (after Session 1 changes are applied)

---

### Child Session 3: Spring Security Migration

**Scope:** Migrate from `WebSecurityConfigurerAdapter` (removed in Spring Security 6) to the component-based `SecurityFilterChain` bean approach.

**Files to modify:**
- `src/main/java/io/spring/api/security/WebSecurityConfig.java`

**Current pattern (lines 22-65):**
```java
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .cors().and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS).permitAll()
            // ...
            .anyRequest().authenticated();
        http.addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
```

**Target pattern:**
```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public JwtTokenFilter jwtTokenFilter() { return new JwtTokenFilter(); }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/graphiql").permitAll()
                .requestMatchers("/graphql").permitAll()
                .requestMatchers(HttpMethod.GET, "/articles/feed").authenticated()
                .requestMatchers(HttpMethod.POST, "/users", "/users/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/articles/**", "/profiles/**", "/tags").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() { /* unchanged */ }
}
```

**Key changes:**
1. Remove `extends WebSecurityConfigurerAdapter`
2. Remove `@Override protected void configure(HttpSecurity)` → use `@Bean SecurityFilterChain`
3. `.csrf().disable()` → `.csrf(csrf -> csrf.disable())` (lambda DSL)
4. `.cors().and()` → `.cors(Customizer.withDefaults())`
5. `.authorizeRequests()` → `.authorizeHttpRequests()`
6. `.antMatchers()` → `.requestMatchers()`
7. Return `http.build()`
8. Add import `org.springframework.security.config.Customizer`
9. Add import `org.springframework.security.web.SecurityFilterChain`
10. Remove import `WebSecurityConfigurerAdapter`

**Dependencies:** Session 1 must complete first (Spring Security 6.x comes with Boot 3.x).

**Acceptance criteria:**
- `WebSecurityConfigurerAdapter` import is gone
- No `antMatchers` or `authorizeRequests` calls remain
- Security filter chain bean is correctly configured
- All endpoints maintain the same authorization rules as before

---

### Child Session 4: Spring MVC & Web Layer Migration

**Scope:** Update Spring MVC patterns that changed in Spring Framework 6.x / Spring Boot 3.x.

**Files to modify:**
- `src/main/java/io/spring/api/exception/CustomizeExceptionHandler.java`

**Change 1: `ResponseEntityExceptionHandler.handleMethodArgumentNotValid` signature change**

In Spring Framework 6.x, the signature of the `handleMethodArgumentNotValid` method changed — the `HttpStatus` parameter was replaced with `HttpStatusCode`:

**Current (line 63-67):**
```java
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException e,
    HttpHeaders headers,
    HttpStatus status,
    WebRequest request) {
```

**Target:**
```java
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException e,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request) {
```

Update imports: add `import org.springframework.http.HttpStatusCode;` (may replace `HttpStatus` import if it was only used here — but `HttpStatus.UNPROCESSABLE_ENTITY` is still used elsewhere, so keep both).

**Change 2: Trailing slash matching (informational)**

Spring Boot 3.x disables trailing slash matching by default. If the API consumers rely on `/articles/` matching `/articles`, a `WebMvcConfigurer` bean should be added. However, the current codebase does not seem to depend on trailing slashes, so this is **optional**.

**Dependencies:** Sessions 1 and 2 must complete first.

**Acceptance criteria:**
- `CustomizeExceptionHandler` compiles with Spring Framework 6.x
- `handleMethodArgumentNotValid` override has correct signature
- Application starts and error handling works correctly

---

### Child Session 5: Netflix DGS GraphQL Migration

**Scope:** Migrate Netflix DGS from v4 (Spring Boot 2.x) to v9 (Spring Boot 3.x compatible).

**Files to modify:**
- `build.gradle` (artifact name change — handled in Session 1)
- `src/main/java/io/spring/graphql/exception/GraphQLCustomizeExceptionHandler.java`

**Change 1: DGS starter artifact rename**

The DGS Spring Boot 3 starter uses a different artifact:
- Old: `com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter`
- New: `com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter`

(Handled in Session 1's `build.gradle` changes.)

**Change 2: `DataFetcherExceptionHandler.onException` return type**

In DGS 7+/graphql-java 21+, the `onException` method returns `CompletableFuture<DataFetcherExceptionHandlerResult>` instead of `DataFetcherExceptionHandlerResult`:

**Current (line 31-32):**
```java
@Override
public DataFetcherExceptionHandlerResult onException(
    DataFetcherExceptionHandlerParameters handlerParameters) {
```

**Target:**
```java
@Override
public CompletableFuture<DataFetcherExceptionHandlerResult> onException(
    DataFetcherExceptionHandlerParameters handlerParameters) {
```

All return statements must wrap in `CompletableFuture.completedFuture(...)`.

**Change 3: DGS Codegen plugin update**

The codegen plugin version `5.0.6` → `7.0.3` to generate Jakarta-compatible types. The generated types under `io.spring.graphql.types` and `io.spring.graphql.DgsConstants` will be regenerated automatically by the build.

**Change 4: Verify DGS annotations compatibility**

DGS annotations (`@DgsComponent`, `@DgsData`, `@DgsQuery`, `@InputArgument`, `DgsDataFetchingEnvironment`) are unchanged in the new version. The 12 GraphQL source files using these annotations should compile without changes beyond the namespace migration.

**Dependencies:** Sessions 1 and 2 must complete first.

**Acceptance criteria:**
- DGS starter resolves correctly with the new artifact name
- `GraphQLCustomizeExceptionHandler.onException` returns `CompletableFuture`
- DGS codegen generates types successfully
- GraphQL endpoints respond correctly

---

### Child Session 6: Configuration Properties Migration

**Scope:** Review and update `application.properties` and `application-test.properties` for Spring Boot 3.x compatibility.

**Files to review/modify:**
- `src/main/resources/application.properties`
- `src/main/resources/application-test.properties`

**Current properties analysis:**

```properties
# Datasource — OK, no changes needed
spring.datasource.url=jdbc:sqlite:dev.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.datasource.username=
spring.datasource.password=

# Jackson — OK
spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true

# Custom properties — OK
image.default=https://static.productionready.io/images/smiley-cyrus.jpg
jwt.secret=nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA
jwt.sessionTime=86400

# MyBatis — OK, these property names are unchanged in mybatis-spring-boot-starter 3.x
mybatis.configuration.cache-enabled=true
mybatis.configuration.default-statement-timeout=3000
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.configuration.use-generated-keys=true
mybatis.type-handlers-package=io.spring.infrastructure.mybatis
mybatis.mapper-locations=mapper/*.xml

# Logging — OK
logging.level.io.spring.infrastructure.mybatis.readservice.ArticleReadService=DEBUG
logging.level.io.spring.infrastructure.mybatis.mapper=DEBUG
```

**Changes required:**

1. **Add `spring.flyway.enabled` check:** Flyway is on the classpath. Spring Boot 3.x changed some Flyway auto-configuration defaults. The current setup works with SQLite which has limited Flyway support. Verify Flyway still initializes correctly. If issues arise, consider `spring.flyway.enabled=false` and rely on MyBatis/manual schema setup, or upgrade Flyway to a version with SQLite support.

2. **No property renames needed:** The properties used (`spring.datasource.*`, `spring.jackson.*`, `mybatis.*`) are unchanged in Spring Boot 3.4.x. The app does not use `spring.redis.*`, `spring.elasticsearch.*`, or other renamed namespaces.

3. **Potential addition:** `spring.jpa.open-in-view=false` is not applicable here since the project uses MyBatis, not JPA.

**Dependencies:** Session 1 must complete first.

**Acceptance criteria:**
- Application starts with existing properties on Boot 3.4.x
- No warnings about deprecated or unknown properties in startup logs
- Flyway migration runs successfully (or is handled gracefully)

---

### Child Session 7: jjwt (JSON Web Token) API Migration

**Scope:** Update jjwt API usage from 0.11.x to 0.12.x which has breaking API changes.

**Files to modify:**
- `src/main/java/io/spring/infrastructure/service/DefaultJwtService.java`

**Current code (lines 26-45):**
```java
signatureAlgorithm = SignatureAlgorithm.HS512;
this.signingKey = new SecretKeySpec(secret.getBytes(), signatureAlgorithm.getJcaName());

// Token creation:
Jwts.builder()
    .setSubject(user.getId())
    .setExpiration(expireTimeFromNow())
    .signWith(signingKey)
    .compact();

// Token parsing:
Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
claimsJws.getBody().getSubject();
```

**Target code:**
```java
this.signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA512");

// Token creation (jjwt 0.12.x new builder API):
Jwts.builder()
    .subject(user.getId())
    .expiration(expireTimeFromNow())
    .signWith(signingKey)
    .compact();

// Token parsing (jjwt 0.12.x new parser API):
Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
claimsJws.getPayload().getSubject();
```

**Key changes:**
1. Remove `SignatureAlgorithm` enum usage — it's deprecated; `signWith(key)` infers algorithm from key type
2. Remove the `signatureAlgorithm` field entirely
3. `.setSubject()` → `.subject()`
4. `.setExpiration()` → `.expiration()`
5. `Jwts.parserBuilder()` → `Jwts.parser()`
6. `.setSigningKey()` → `.verifyWith()` (for `SecretKey`)
7. `.parseClaimsJws()` → `.parseSignedClaims()`
8. `.getBody()` → `.getPayload()`
9. `SecretKeySpec` constructor: use `"HmacSHA512"` string directly instead of `signatureAlgorithm.getJcaName()`

**Dependencies:** Session 1 must complete first (jjwt version upgrade).

**Acceptance criteria:**
- JWT token creation and parsing work correctly
- `DefaultJwtServiceTest` passes
- No deprecated API warnings from jjwt

---

### Child Session 8: Testing Migration

**Scope:** Update test dependencies and patterns for Spring Boot 3.x compatibility.

**Files to potentially modify:**
- All test files under `src/test/java/io/spring/`
- `build.gradle` (test dependency versions — handled in Session 1)

**Test inventory (23 test files):**

| Test Class | Annotation | Concerns |
|---|---|---|
| `RealworldApplicationTests` | `@SpringBootTest` | Full context load test |
| `ArticleApiTest` | `@WebMvcTest` | REST Assured MockMvc |
| `ArticleFavoriteApiTest` | `@WebMvcTest` | REST Assured MockMvc |
| `ArticlesApiTest` | `@WebMvcTest` | REST Assured MockMvc |
| `CommentsApiTest` | `@WebMvcTest` | REST Assured MockMvc |
| `CurrentUserApiTest` | `@WebMvcTest` | REST Assured MockMvc |
| `ListArticleApiTest` | `@WebMvcTest` | REST Assured MockMvc |
| `ProfileApiTest` | `@WebMvcTest` | REST Assured MockMvc |
| `UsersApiTest` | `@WebMvcTest` | REST Assured MockMvc |
| `ArticleQueryServiceTest` | `@SpringBootTest` (via DbTestBase) | MyBatis test |
| `CommentQueryServiceTest` | `@SpringBootTest` (via DbTestBase) | MyBatis test |
| `ProfileQueryServiceTest` | `@SpringBootTest` (via DbTestBase) | MyBatis test |
| `TagsQueryServiceTest` | `@SpringBootTest` (via DbTestBase) | MyBatis test |
| `ArticleTest` | Unit test | No Spring context |
| `ArticleRepositoryTransactionTest` | `@SpringBootTest` | Transaction test |
| `DbTestBase` | `@MybatisTest` | Base for DB tests |
| `MyBatisArticleRepositoryTest` | `@MybatisTest` (via DbTestBase) | Repository test |
| `MyBatisCommentRepositoryTest` | `@MybatisTest` (via DbTestBase) | Repository test |
| `MyBatisArticleFavoriteRepositoryTest` | `@MybatisTest` (via DbTestBase) | Repository test |
| `MyBatisUserRepositoryTest` | `@MybatisTest` (via DbTestBase) | Repository test |
| `DefaultJwtServiceTest` | Unit test | JWT test |
| `TestHelper` | Helper class | Test utilities |
| `TestWithCurrentUser` | Abstract base | `@MockBean` setup |

**Changes required:**

1. **`@MockBean` relocation (if needed):** In Spring Boot 3.4.x, `@MockBean` and `@SpyBean` are still available in `org.springframework.boot.test.mock.mockito` but may show deprecation warnings pointing to the new `spring-boot-testcontainers` approach. For now, these annotations still work and no changes are strictly required. If warnings appear, the imports remain at the same package.

2. **REST Assured 5.x upgrade:** REST Assured 5.x is Jakarta-compatible. The `spring-mock-mvc` module API is largely the same. The import paths do not change: `io.restassured.module.mockmvc.RestAssuredMockMvc` remains valid.

3. **No `javax` imports in test files:** Already confirmed — zero `javax.*` imports exist in test files.

4. **`@WebMvcTest` compatibility:** The annotation works the same in Spring Boot 3.x. The `@Import({WebSecurityConfig.class, ...})` pattern is still valid.

5. **`@MybatisTest` compatibility:** The `mybatis-spring-boot-starter-test:3.0.4` provides the `@MybatisTest` annotation compatible with Spring Boot 3.x.

6. **SQLite test compatibility:** The in-memory SQLite URL `jdbc:sqlite::memory:` works the same way. The upgraded SQLite JDBC driver (`3.47.1.0`) is backward-compatible.

**Dependencies:** Sessions 1-7 must all complete first.

**Acceptance criteria:**
- `./gradlew test` passes with all 23+ test files
- No test compilation errors
- No runtime test failures related to the migration

---

### Child Session 9 (Parent/Orchestrator): Integration Verification

**Scope:** After all child sessions complete, perform full integration verification.

**Steps:**
1. Run `./gradlew clean compileJava` — verify zero compilation errors
2. Run `./gradlew compileTestJava` — verify test compilation
3. Run `./gradlew test` — verify all tests pass
4. Run `./gradlew bootRun` — verify application starts
5. Test endpoints: `curl http://localhost:8080/tags`
6. Run `./gradlew spotlessCheck` — verify code format
7. Fix any remaining issues discovered during integration

**Dependencies:** All sessions (1-8) must complete first.

**Acceptance criteria:**
- Full clean build succeeds
- All tests pass
- Application boots and serves requests
- No deprecated API warnings (or they are documented and acceptable)

---

## Execution Order & Dependencies

```
Session 1: Build Config Upgrade
    │
    ├──► Session 2: Jakarta Namespace Migration  ──┐
    ├──► Session 3: Spring Security Migration    ──┤
    ├──► Session 4: Spring MVC Migration         ──┤
    ├──► Session 5: DGS GraphQL Migration        ──┤  (parallel)
    ├──► Session 6: Config Properties Migration  ──┤
    └──► Session 7: jjwt API Migration           ──┘
                                                   │
                                                   ▼
                                    Session 8: Testing Migration
                                                   │
                                                   ▼
                              Session 9: Integration Verification
```

| Session | Depends On | Can Parallelize With |
|---|---|---|
| 1 (Build Config) | — | — |
| 2 (Jakarta) | 1 | 3, 4, 5, 6, 7 |
| 3 (Security) | 1 | 2, 4, 5, 6, 7 |
| 4 (MVC) | 1, 2 | 3, 5, 6, 7 |
| 5 (DGS) | 1, 2 | 3, 4, 6, 7 |
| 6 (Config) | 1 | 2, 3, 4, 5, 7 |
| 7 (jjwt) | 1 | 2, 3, 4, 5, 6 |
| 8 (Testing) | 1-7 | — |
| 9 (Verification) | 1-8 | — |

> **Note:** Sessions 4 and 5 technically depend on Session 2 (they touch files that also need Jakarta imports). If running in parallel with Session 2, each session should handle its own files' Jakarta imports, or Session 2 should complete first to avoid merge conflicts.

---

## Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| SQLite + Flyway compatibility on Boot 3.x | Medium | Test Flyway migration; fall back to manual schema if needed |
| DGS v9 breaking changes beyond `onException` | Medium | Run full GraphQL endpoint tests; review DGS 9.x changelog |
| jjwt 0.12.x API changes missed | Low | Comprehensive unit test for JWT create/parse |
| Joda-Time compatibility | Low | Joda-Time is independent of Spring/Jakarta; no migration needed |
| Trailing slash behavior change | Low | Current API tests don't use trailing slashes |
| REST Assured 5.x mock-mvc module API changes | Low | MockMvc integration API is stable |
| `@MockBean` deprecation | Low | Still functional in Boot 3.4.x; can migrate later |

## Optional Future Improvements (Out of Scope)

These are not required for the Boot 3.x migration but are recommended follow-ups:

1. **Joda-Time → `java.time`**: Replace `org.joda.time.DateTime` with `java.time.Instant` or `java.time.ZonedDateTime` across 12 source files and the `DateTimeHandler` MyBatis type handler
2. **Java 21 upgrade**: After stabilizing on Java 17, upgrade to 21 for virtual threads and other improvements
3. **DGS → Spring for GraphQL**: Consider migrating from Netflix DGS to the official `spring-boot-starter-graphql` for tighter Spring integration
4. **SQLite → H2 or PostgreSQL**: SQLite has limited production use; consider switching to H2 for testing and PostgreSQL for production
