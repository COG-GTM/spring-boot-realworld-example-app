---
name: test-driven-development
description: >-
  Practice test-driven development (TDD) in this Spring Boot RealWorld codebase.
  Use this skill whenever you add or change behavior in the domain (core),
  application (read/query), API (REST controllers), or infrastructure
  (MyBatis repositories) layers. It encodes the test frameworks, layering, naming
  conventions, fixtures, and the red-green-refactor loop used in this repo so new
  tests match the existing style and run green.
---

# Test-Driven Development

Write the failing test first, make it pass with the smallest change, then refactor
— all while matching the conventions already established in `src/test`.

## The TDD loop

1. **Red** — Write one focused test that expresses the desired behavior. Run it and
   confirm it fails for the right reason (assertion failure, not a compile error or
   missing wiring).
2. **Green** — Write the minimum production code to make that test pass.
3. **Refactor** — Clean up production and test code while keeping the suite green.
4. Repeat per behavior, not per method. Each `@Test` asserts one behavior.

Run the suite with the Gradle wrapper (Java 17 toolchain):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && export PATH=$JAVA_HOME/bin:$PATH
./gradlew test                                   # full suite
./gradlew test --tests "io.spring.core.article.ArticleTest"        # one class
./gradlew test --tests "io.spring.api.*"                           # one package
```

Always format before committing — CI/Spotless enforces Google Java Format:

```bash
./gradlew spotlessJavaApply
./gradlew compileJava compileTestJava   # "lint" — must compile
```

## Pick the right test layer

This codebase tests each architectural layer with a distinct, minimal slice. Choose
the lightest slice that exercises the behavior — do not boot the whole app when a
plain unit test will do.

| Layer | What you're testing | Base / annotations | Example |
|-------|--------------------|--------------------|---------|
| Domain (`core`) | Pure business logic, no Spring | Plain JUnit 5, no context | `core/article/ArticleTest.java` |
| Service (infra) | A single service in isolation | Plain JUnit 5, `new` the class | `infrastructure/service/DefaultJwtServiceTest.java` |
| API (`api`) | REST controller + serialization + security | `@WebMvcTest` + `@Import` + `@MockBean` + RestAssured MockMvc | `api/ArticleApiTest.java` |
| Read model (`application`) | Query services over a real DB | extend `DbTestBase` (`@MybatisTest`) + `@Import` | `application/article/ArticleQueryServiceTest.java` |
| Repository (`infrastructure`) | MyBatis persistence round-trips | extend `DbTestBase` + `@Import` the repo | `infrastructure/user/MyBatisUserRepositoryTest.java` |
| Smoke | App context wiring | `@SpringBootTest` | `RealworldApplicationTests.java` |

Tests live under `src/test/java` mirroring the production package
(`io.spring.<layer>.<feature>`).

## Conventions (match these exactly)

- **Naming**: snake_case test methods with a `should_` prefix describing the behavior,
  e.g. `should_get_right_slug`, `should_404_if_article_not_found`,
  `should_save_and_fetch_user_success`.
- **Setup**: use `@BeforeEach public void setUp()` to build fixtures / shared state.
- **Frameworks**: JUnit 5 (`org.junit.jupiter.api.*`), Mockito (`@MockBean`,
  `when(...).thenReturn(...)`, `verify(...)`), RestAssured MockMvc for the API layer.
- **Assertions** follow the layer:
  - Domain: Hamcrest — `assertThat(article.getSlug(), is("a-new-title"))`.
  - Service / DB: JUnit `Assertions.assertEquals / assertTrue / assertNotNull`.
  - API: RestAssured fluent matchers — `.then().statusCode(200).body("article.slug", equalTo(slug))`.
- **Fixtures**: reuse the shared helpers instead of rebuilding objects inline:
  - `io.spring.TestHelper` — static factories like `articleDataFixture(seed, user)`
    and `getArticleDataFromArticleAndUser(article, user)`.
  - `io.spring.api.TestWithCurrentUser` — base class that mocks `UserRepository`,
    `UserReadService`, `JwtService` and a logged-in `user`/`token` via `userFixture()`.
    Extend it for authenticated controller tests and call `super.setUp()`.
  - `io.spring.infrastructure.DbTestBase` — `@MybatisTest` + `@ActiveProfiles("test")`
    + `@AutoConfigureTestDatabase(replace = NONE)` base for DB-backed tests.

## Patterns by layer

### Domain unit test (start here for business rules)

No Spring, no mocks — construct the entity and assert. Fast feedback for the red step.

```java
public class ArticleTest {
  @Test
  public void should_get_right_slug() {
    Article article = new Article("a new   title", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("a-new-title"));
  }
}
```

### API / controller test

Slice with `@WebMvcTest`, import only the security + Jackson config you need, mock the
collaborators with `@MockBean`, wire RestAssured to the `MockMvc`, then stub and assert.

```java
@WebMvcTest({ArticleApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ArticleApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;
  @MockBean private ArticleQueryService articleQueryService;

  @Override @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_404_if_article_not_found() throws Exception {
    when(articleQueryService.findBySlug(anyString(), any())).thenReturn(Optional.empty());
    RestAssuredMockMvc.when().get("/articles/not-exists").then().statusCode(404);
  }
}
```

For write endpoints, build the request body as a `Map`, `POST`/`PUT` it, assert the
status and JSON body, and `verify(...)` the command service was invoked.

### Read-model / repository test (real DB)

Extend `DbTestBase` and `@Import` the concrete MyBatis beans under test. Seed data
through the real repositories in `setUp()`, then assert through the query service. A
fresh SQLite test database is used; `clean` deletes `dev.db`.

```java
@Import({ArticleQueryService.class, MyBatisUserRepository.class, MyBatisArticleRepository.class})
public class ArticleQueryServiceTest extends DbTestBase {
  @Autowired private ArticleQueryService queryService;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@gmail.com", "aisensiy", "123", "", "");
    userRepository.save(user);
    // ... save an article ...
  }

  @Test
  public void should_fetch_article_success() {
    Optional<ArticleData> optional = queryService.findById(article.getId(), user);
    Assertions.assertTrue(optional.isPresent());
  }
}
```

## TDD checklist before opening a PR

- [ ] New behavior is covered by a test that was written to fail first.
- [ ] Test is in the correct layer/slice and the right package under `src/test/java`.
- [ ] Method name is `should_...` snake_case and asserts a single behavior.
- [ ] Reused `TestHelper` / `TestWithCurrentUser` / `DbTestBase` instead of duplicating setup.
- [ ] `./gradlew spotlessJavaApply` run; `./gradlew test` is green.
