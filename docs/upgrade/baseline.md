# Baseline test results on Java 11

Baseline for the Java 17 / Spring Boot 3 upgrade, recorded before any upgrade changes.
Later phases should compare against these numbers.

## Environment

| Item | Value |
| --- | --- |
| Branch | `upgrade/java17-boot3` at `9194bfac` (= `master` + restored CI workflow and a Spotless-only reformat of `DefaultJwtServiceTest`) |
| JDK | Zulu 11.0.33 (CI uses `actions/setup-java` with `distribution: zulu`, `java-version: 11`) |
| Gradle | 7.4 (wrapper) |
| Spring Boot | 2.6.3 |
| Command | `./gradlew clean test` (CI runs `./gradlew clean spotlessCheck test`) |

## Result

`BUILD SUCCESSFUL` — **68 tests in 20 test classes, 0 failures, 0 errors, 0 skipped.**

`./gradlew clean test` was run three times in a row with identical results (68/68 passing
each time), and `./gradlew spotlessCheck` passes. No `@Disabled` / `@Ignore` tests exist.

## Tests per class

| Package | Class | Tests |
| --- | --- | ---: |
| `io.spring` | `RealworldApplicationTests` | 1 |
| `api` | `ArticleApiTest` | 6 |
| `api` | `ArticleFavoriteApiTest` | 2 |
| `api` | `ArticlesApiTest` | 3 |
| `api` | `CommentsApiTest` | 5 |
| `api` | `CurrentUserApiTest` | 6 |
| `api` | `ListArticleApiTest` | 3 |
| `api` | `ProfileApiTest` | 3 |
| `api` | `UsersApiTest` | 7 |
| `application` | `article.ArticleQueryServiceTest` | 9 |
| `application` | `comment.CommentQueryServiceTest` | 2 |
| `application` | `profile.ProfileQueryServiceTest` | 1 |
| `application` | `tag.TagsQueryServiceTest` | 1 |
| `core` | `article.ArticleTest` | 5 |
| `infrastructure` | `article.ArticleRepositoryTransactionTest` | 1 |
| `infrastructure` | `article.MyBatisArticleRepositoryTest` | 3 |
| `infrastructure` | `comment.MyBatisCommentRepositoryTest` | 1 |
| `infrastructure` | `favorite.MyBatisArticleFavoriteRepositoryTest` | 2 |
| `infrastructure` | `service.DefaultJwtServiceTest` | 3 |
| `infrastructure` | `user.MyBatisUserRepositoryTest` | 4 |

| Package | Classes | Tests |
| --- | ---: | ---: |
| `io.spring` (context load) | 1 | 1 |
| `api` (RestAssured MockMvc) | 8 | 35 |
| `application` (query services, SQLite) | 4 | 13 |
| `core` (domain) | 1 | 5 |
| `infrastructure` (MyBatis repositories on SQLite, JWT) | 6 | 14 |
| **Total** | **20** | **68** |

`src/test` has 23 Java files; the other 3 are test support classes with no tests:
`TestHelper`, `api.TestWithCurrentUser`, `infrastructure.DbTestBase`.

## Gradle test report summary

| Run | Tests | Failures | Errors | Skipped | Build time |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1 (cold dependency cache) | 68 | 0 | 0 | 0 | 42s |
| 2 | 68 | 0 | 0 | 0 | 15s |
| 3 | 68 | 0 | 0 | 0 | 15s |

Summed test time across classes is about 2.4s. The slowest class is `ArticleApiTest` (~1.1s, mostly MockMvc start-up).

## Known failures, flakiness and gaps

- **Failing tests:** none. **Flaky tests:** none seen in three consecutive runs.
- **`master` Spotless:** `master` fails `spotlessCheck` on `DefaultJwtServiceTest` (a line too long).
  That was fixed on the integration branch by the CI restore step; tests on `master` are otherwise the
  same code as here.
- **No GraphQL tests:** nothing under `src/test` covers the DGS datafetchers/mutations in
  `io.spring.graphql`. Breakage in the GraphQL layer during the upgrade will not show up in this suite.
- **Compiler notes (not failures):** `GraphQLCustomizeExceptionHandler` uses a deprecated API and unchecked
  operations (`javac` notes during `compileJava`).
- **Local environment only:** Maven Central returned HTTP 429 when resolving the Spotless plugin's dependencies
  on the Devin VM. Pointing Gradle at a Maven Central mirror in a user-level init script fixed it. This does not
  affect CI or the repository.
