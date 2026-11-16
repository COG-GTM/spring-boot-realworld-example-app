# Architecture

## Layers

Code under `src/main/java/io/spring` is split into four layers. REST and GraphQL are separate adapters that share everything below them.

| Layer | Package | Contents |
| --- | --- | --- |
| Adapters | `api`, `graphql` | Spring MVC controllers (`*Api`), DGS data fetchers and mutations, security config (`api.security`), exception mapping (`api.exception`, `graphql.exception`) |
| Application | `application` | Command services (`article.ArticleCommandService`, `user.UserService`), query services (`ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `TagsQueryService`, `UserQueryService`), request params and custom validators, read-model DTOs (`application.data`), pagination types (`Page`, `CursorPageParameter`, `CursorPager`, `DateTimeCursor`, `PageCursor`, `Node`) |
| Domain | `core` | Entities (`article.Article`, `article.Tag`, `comment.Comment`, `favorite.ArticleFavorite`, `user.User`, `user.FollowRelation`), repository interfaces, `service.AuthorizationService` (ownership checks), `service.JwtService` interface |
| Infrastructure | `infrastructure` | `repository.MyBatis*Repository` (implement the `core` repositories), `mybatis.mapper` (write-side mapper interfaces), `mybatis.readservice` (read-side mapper interfaces), `mybatis.DateTimeHandler` (Joda `DateTime` type handler, UTC), `service.DefaultJwtService` (JJWT) |

## Write side and read side

A light CQRS split over one database:

- **Writes**: the adapter loads an entity through a `core` repository (e.g. `ArticleRepository.findBySlug`), checks `AuthorizationService`, then calls a command service or entity method and saves through the repository. `save` is an upsert: `MyBatisArticleRepository` and `MyBatisUserRepository` insert when the id is unknown and update otherwise. A new article also inserts any new tags and its `article_tags` rows; updates only touch the article row.
- **Reads**: query services call read-service mappers that select straight into DTOs (`ArticleData`, `CommentData`, `ProfileData`, `UserData`), then add per-user fields (`favorited`, `favoritesCount`, `following`) via `ArticleFavoritesReadService` and `UserRelationshipQueryService`. Shared result maps live in `mapper/TransferData.xml` (namespace `transfer.data`).

Validation runs in two places: `@Valid` on REST request bodies, and `@Validated` services (`ArticleCommandService`, `UserService`) whose parameters carry custom constraints (`@DuplicatedArticleConstraint`, `@DuplicatedEmailConstraint`, `@DuplicatedUsernameConstraint`, and `@UpdateUserConstraint` on `UpdateUserCommand`). Service-level failures raise `ConstraintViolationException`, which `CustomizeExceptionHandler` (REST, `422`) and `GraphQLCustomizeExceptionHandler` (GraphQL, `BAD_REQUEST`) turn into field errors.

## Request flow

1. `JwtTokenFilter` (added before `UsernamePasswordAuthenticationFilter`) reads `Authorization`, gets the user id from `JwtService.getSubFromToken`, loads the `User` and sets it as the authentication principal.
2. `WebSecurityConfig` applies the access rules listed in [API.md](API.md#access-rules-websecurityconfig). Sessions are stateless, CSRF is disabled, unauthenticated access to protected paths returns `401` (`HttpStatusEntryPoint`), and CORS allows all origins.
3. REST controllers get the user via `@AuthenticationPrincipal User` (`null` for anonymous calls to public endpoints) and wrap results in a root-key map. GraphQL resolvers use `SecurityUtil.getCurrentUser()`.
4. Exceptions: `ResourceNotFoundException` → 404, `NoAuthorizationException` → 403, validation errors and `InvalidAuthenticationException` → 422.

## GraphQL

- Schema-first with Netflix DGS. The `generateJava` Gradle task (DGS codegen, configured in `build.gradle`) generates types and `DgsConstants` in package `io.spring.graphql` under `build/generated` from `src/main/resources/schema`.
- Resolvers: `ArticleDatafetcher`, `CommentDatafetcher`, `ProfileDatafetcher`, `MeDatafetcher`, `TagDatafetcher` (queries and nested fields); `UserMutation`, `ArticleMutation`, `CommentMutation`, `RelationMutation` (mutations).
- Mutations return an empty payload plus a DGS `localContext` (the saved `Article`, `Comment` or `User`); child resolvers such as `ArticlePayload.article`, `CommentPayload.comment` and `UserPayload.user` use it to load the full data. Connection resolvers pass a slug/id → DTO map as `localContext` so `Article.author` and `Comment.author` can resolve without re-querying the article.
- Cursor pagination: cursors are epoch milliseconds (`DateTimeCursor`). Article queries fetch `limit + 1` rows and the extra row sets `hasNextPage`/`hasPreviousPage`.

## Persistence

- SQLite via `org.xerial:sqlite-jdbc`. Default URL `jdbc:sqlite:dev.db`; the `test` profile uses `jdbc:sqlite::memory:`.
- Flyway manages the schema; there is a single migration, `db/migration/V1__create_tables.sql`.
- MyBatis: mapper XML in `src/main/resources/mapper`, underscore-to-camelCase mapping, type handlers from `io.spring.infrastructure.mybatis`, 3000 ms statement timeout.
- Primary keys are random UUID strings created in the domain constructors.

| Table | Columns | Keys |
| --- | --- | --- |
| `users` | `id, username, password, email, bio, image` | PK `id`; `username`, `email` unique |
| `articles` | `id, user_id, slug, title, description, body, created_at, updated_at` | PK `id`; `slug` unique |
| `article_favorites` | `article_id, user_id` | PK `(article_id, user_id)` |
| `follows` | `user_id, follow_id` | none (duplicates avoided in `MyBatisUserRepository.saveRelation`) |
| `tags` | `id, name` | PK `id` |
| `article_tags` | `article_id, tag_id` | none |
| `comments` | `id, body, article_id, user_id, created_at, updated_at` | PK `id` |

No foreign keys are declared, and deletes do not cascade.

## Tests

68 tests under `src/test/java/io/spring`:

- `api/*ApiTest`: `@WebMvcTest` controller tests with REST Assured MockMvc; services and repositories are mocked and `TestWithCurrentUser` stubs a logged-in user.
- `application/**`: query-service tests against MyBatis and in-memory SQLite.
- `infrastructure/**`: repository tests (`DbTestBase`, `@MybatisTest` with the `test` profile), a transaction test, and `DefaultJwtServiceTest`.
- `core/article/ArticleTest`: slug generation.
- `RealworldApplicationTests`: context load.

There are no tests for the GraphQL resolvers.

## Known issues

Observed in the current code:

- **GraphQL `feed` without a token** throws `NullPointerException`: `/graphql` is public and `ArticleDatafetcher.getFeed` passes a `null` user to `ArticleQueryService.findUserFeedWithCursor`, which calls `user.getId()`.
- **GraphQL `me` without a header** fails because `MeDatafetcher.getMe` declares `@RequestHeader("Authorization")` as required.
- **`Article.comments` ignores the page size**: `CommentReadService.findByArticleIdWithCursor` has no `limit`, and `CommentQueryService` removes only one row.
- **Article cursors mix columns**: `ArticleData.getCursor()` uses `updatedAt`, while the cursor SQL filters and sorts on `created_at`.
- **`updated_at` is never written on update**: `ArticleMapper.update` does not set it, so `updatedAt` stays equal to `createdAt` after `PUT /articles/{slug}`.
- **Joined-row limits**: `findArticlesOfAuthors` (REST feed) and `findArticlesOfAuthorsWithCursor` apply `limit` to rows joined with tags, so articles with several tags take several slots; `findArticlesOfAuthors` also has no `ORDER BY`. `CommentReadService.findByArticleId` (REST comments) has no `ORDER BY` either.
- **REST create without `tagList`** fails with `500`: `NewArticleParam.tagList` stays `null` and `new Article(...)` calls `new HashSet<>(tagList)`. GraphQL `ArticleMutation` substitutes an empty list.
- **Updating a title** regenerates the slug without the duplicate check used on create; a clash hits the `articles.slug` unique constraint.
- **Deleting an article** leaves its comments, favorites and `article_tags` rows behind, and `GET /tags` keeps returning orphaned tags.
- **Comment `updatedAt`** is mapped from `created_at` (`TransferData.xml`); `Comment` has no `updatedAt` field.
- **REST responses** include non-spec `id`/`cursor` fields on articles and `cursor` on comments.
