# Testing & tooling

## Commands

```bash
./gradlew spotlessJavaApply   # google-java-format, 2-space indent — run before every commit
./gradlew test                # full suite
./gradlew test --tests '*CommentsApiTest*'
./gradlew bootRun             # http://localhost:8080/tags
```

Java 11. Formatting is enforced by Spotless — a PR that has not been formatted will not be accepted.

## Which test do I write?

| Change | Test | Base / example |
| --- | --- | --- |
| REST endpoint | `@WebMvcTest(<X>Api.class)` slice + RestAssuredMockMvc | `api/CommentsApiTest.java` |
| Query service / read model | plain unit test with mocked read services | `application/article/ArticleQueryServiceTest.java` |
| Mapper / repository SQL | `@MybatisTest` against SQLite | extend `infrastructure/DbTestBase` |
| Entity behaviour | plain JUnit 5, no Spring | `core/article/ArticleTest.java` |

**Every new or changed endpoint needs an API test covering: success (asserting the JSON shape),
missing/invalid auth, ownership failure where applicable, and validation failure.**

## API test shape

```java
@WebMvcTest(CommentsApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class CommentsApiTest extends TestWithCurrentUser {   // gives you `user`, `token`, mocked JwtService
  @MockBean private CommentQueryService commentQueryService; // mock everything below the controller
  @Autowired private MockMvc mvc;

  @BeforeEach
  public void setUp() throws Exception {
    RestAssuredMockMvc.mockMvc(mvc);
    super.setUp();
  }

  @Test
  public void should_create_comment_success() throws Exception {
    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(param)
        .when()
        .post("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(201)
        .body("comment.body", equalTo(commentData.getBody()));
  }
}
```

Conventions visible above, and expected in new tests:

- Method names are `should_<expected>_<condition>` in snake_case.
- Assert on the JSON path (`"comment.body"`), not just the status code — that is how the client
  contract is protected.
- Authenticate with the literal header `"Authorization", "Token " + token`.
- Build request bodies as nested `HashMap`s including the root key (`{"comment": {"body": ...}}`).
- Mock collaborators with `@MockBean` + `when(...).thenReturn(...)`; no real DB in API tests.
- Validation tests assert the error shape: `.body("errors.body[0]", equalTo("can't be empty"))`.

## Pull requests

- Format, then `./gradlew test`.
- Describe any change to a client-facing payload explicitly (see `03-client-contract.md`).
- Keep the diff scoped: no drive-by reformatting, no unrelated dependency bumps.
