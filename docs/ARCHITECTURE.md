# Architecture notes

## Layers

The code under `src/main/java/io/spring` is split into four layers. REST and GraphQL are two separate adapters that share everything below them.

| Layer | Package | Contents |
| --- | --- | --- |
| Adapters | `api`, `graphql` | Spring MVC controllers (`*Api`), DGS data fetchers/mutations, security config, exception handlers |
| Application | `application` | Command services (`article.ArticleCommandService`, `user.UserService`), query services (`ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `TagsQueryService`, `UserQueryService`), request params and custom validators, read-model DTOs in `application.data`, pagination types (`Page`, `CursorPageParameter`, `CursorPager`, `DateTimeCursor`) |
| Domain | `core` | Entities (`article.Article`, `article.Tag`, `comment.Comment`, `favorite.ArticleFavorite`, `user.User`, `user.FollowRelation`), repository interfaces, `service.AuthorizationService` (ownership checks) and the `service.JwtService` interface |
| Infrastructure | `infrastructure` | MyBatis repository implementations (`repository.MyBatis*Repository`), write mappers (`mybatis.mapper`), read services (`mybatis.readservice`), `mybatis.DateTimeHandler` (Joda `DateTime` type handler), `service.DefaultJwtService` (JJWT) |

## Write side vs read side

Writes and reads use different models (a light CQRS split; both use the same database):

- **Writes:** an adapter loads a domain entity through a `core` repository interface (e.g. `ArticleRepository.findBySlug`), checks permissions with `AuthorizationService`, then calls a command service or entity method and saves through the repository. `MyBatisArticleRepository.save` inserts a new article together with any new tags and its article-tag links; for an existing article it only updates the article row (tags cannot be changed after creation).
- **Reads:** query services call MyBatis read services that select straight into DTOs (`ArticleData`, `CommentData`, `ProfileData`, `UserData`), then enrich them with per-user data (`favorited`, `favoritesCount`, `following`) using `ArticleFavoritesReadService` and `UserRelationshipQueryService`. Shared result maps live in `mapper/TransferData.xml` (namespace `transfer.data`).

Validation happens in two places: `@Valid` on controller request bodies, and `@Validated` services (`ArticleCommandService`, `UserService`) whose parameters carry custom constraints (`@DuplicatedArticleConstraint`, `@DuplicatedEmailConstraint`, `@DuplicatedUsernameConstraint`, and the `UpdateUserCommand` check for email/username taken by another user). Service-level failures surface as `ConstraintViolationException`, which `CustomizeExceptionHandler` (REST) and `GraphQLCustomizeExceptionHandler` (GraphQL) turn into field errors.

## Request flow (REST)

1. `JwtTokenFilter` (registered before `UsernamePasswordAuthenticationFilter` in `WebSecurityConfig`) reads `Authorization`, asks `JwtService.getSubFromToken` for the user id, loads the `User` from `UserRepository` and stores it as the authentication principal.
2. `WebSecurityConfig` decides access: `OPTIONS`, `/graphql`, `/graphiql`, `POST /users` and `/users/login`, and `GET` on `/articles/**`, `/profiles/**`, `/tags` are public, except `GET /articles/feed`; everything else needs an authenticated user. Failures return `401` via `HttpStatusEntryPoint`. Sessions are stateless and CSRF is off.
3. Controllers receive the user with `@AuthenticationPrincipal User` (it is `null` on public endpoints without a token) and wrap results in a root-key map such as `{"article": ...}`.
4. Exceptions map to status codes: `ResourceNotFoundException` → 404, `NoAuthorizationException` → 403, validation errors and `InvalidAuthenticationException` → 422.

## GraphQL

- Schema-first with Netflix DGS: `src/main/resources/schema/schema.graphqls` is compiled by the `generateJava` Gradle task (DGS codegen) into types and `DgsConstants` under `io.spring.graphql` in `build/generated`.
- Resolvers: `ArticleDatafetcher`, `CommentDatafetcher`, `ProfileDatafetcher`, `MeDatafetcher`, `TagDatafetcher` (queries and nested fields); `UserMutation`, `ArticleMutation`, `CommentMutation`, `RelationMutation` (mutations).
- `SecurityUtil.getCurrentUser()` reads the principal set by `JwtTokenFilter`.
- Mutations return an empty payload plus a DGS `localContext` (e.g. the saved `Article` or `User`); nested resolvers such as `ArticlePayload.article` and `UserPayload.user` use that context to load the full data.
- Connections use cursor pagination (`CursorPageParameter`); cursors are epoch milliseconds. Article queries fetch `limit + 1` rows and the query service uses the extra row to set `hasNextPage`/`hasPreviousPage`. The comment cursor query has no SQL `limit` (see [Known issues](#known-issues)).

## Persistence

- SQLite via `org.xerial:sqlite-jdbc`; default URL `jdbc:sqlite:dev.db`, the `test` profile uses `jdbc:sqlite::memory:`.
- Schema is managed by Flyway; there is a single migration, `src/main/resources/db/migration/V1__create_tables.sql`.
- MyBatis settings (`application.properties`): mappers from `mapper/*.xml`, underscore-to-camelCase mapping, type handlers from `io.spring.infrastructure.mybatis`, statement timeout 3000 ms.
- All primary keys are random UUID strings generated in the domain constructors.

Tables created by `V1__create_tables.sql`:

| Table | Columns | Keys |
| --- | --- | --- |
| `users` | `id, username, password, email, bio, image` | PK `id`; `username`, `email` unique |
| `articles` | `id, user_id, slug, title, description, body, created_at, updated_at` | PK `id`; `slug` unique |
| `article_favorites` | `article_id, user_id` | PK `(article_id, user_id)` |
| `follows` | `user_id, follow_id` | none (duplicates avoided in `MyBatisUserRepository.saveRelation`) |
| `tags` | `id, name` | PK `id` |
| `article_tags` | `article_id, tag_id` | none |
| `comments` | `id, body, article_id, user_id, created_at, updated_at` | PK `id` |

No foreign-key constraints are declared.

## Tests

`src/test/java/io/spring`:

- `api/*ApiTest` — `@WebMvcTest` controller tests with REST Assured MockMvc; services and repositories are mocked, and `TestWithCurrentUser` stubs an authenticated user.
- `application/**` — query-service tests against MyBatis and the in-memory SQLite database.
- `infrastructure/**` — repository tests (`DbTestBase`) and `DefaultJwtServiceTest`.
- `core/article/ArticleTest` — slug generation.
- `RealworldApplicationTests` — context load.

## Known issues

Behaviour observed in the current code that callers should be aware of:

- `Article.comments` (GraphQL) does not respect `first`/`last`: `CommentReadService.findByArticleIdWithCursor` has no `limit`, and `CommentQueryService` removes only one row, so a page can contain more items than requested.
- The GraphQL `feed` query throws a `NullPointerException` (returned as an `INTERNAL` error) when called without a token, because `/graphql` is public and the resolver does not check for a user.
- Article cursors are built from `updatedAt` (`ArticleData.getCursor`) while the cursor SQL filters and sorts on `created_at`, so paging can skip or repeat edited articles.
- `GET /articles/feed` (`findArticlesOfAuthors`) and `GET /articles/{slug}/comments` (`findByArticleId`) have no `ORDER BY`, so their order is not guaranteed.
- REST article and comment responses include an extra `cursor` field (and articles an `id`) that are not part of the RealWorld spec.
- Updating an article's title regenerates the slug without the duplicate-title check that creation has; a clash violates the `articles.slug` unique constraint.
