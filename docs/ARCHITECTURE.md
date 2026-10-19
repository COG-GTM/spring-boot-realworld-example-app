# Architecture

The backend follows a layered, DDD-inspired design. REST and GraphQL are two **adapters** over the same application and domain layers, and writes and reads take separate paths (a lightweight CQRS split).

```
            HTTP
   ┌─────────┴──────────┐
   │ api (REST)          │ graphql (DGS)
   │ controllers         │ data fetchers / mutations
   └─────────┬──────────┘
             ▼
   application
   ├─ command side: ArticleCommandService, UserService (+ Bean Validation)
   └─ query side:   *QueryService → DTOs in application/data
             │                         │
             ▼                         ▼
   core (domain)                 infrastructure/mybatis/readservice
   entities + repository         (read-model SQL straight to DTOs)
   interfaces                          │
             │                         │
             ▼                         │
   infrastructure/repository  ─────────┤
   (MyBatis*Repository → mappers)      │
             ▼                         ▼
                 SQLite (Flyway schema)
```

## Packages (`src/main/java/io/spring`)

| Package | Responsibility |
| --- | --- |
| `api` | Spring MVC controllers (`UsersApi`, `CurrentUserApi`, `ProfileApi`, `ArticlesApi`, `ArticleApi`, `ArticleFavoriteApi`, `CommentsApi`, `TagsApi`) |
| `api.security` | `WebSecurityConfig` (stateless, CSRF disabled, CORS, route rules) and `JwtTokenFilter` |
| `api.exception` | Exception types and `CustomizeExceptionHandler`, which builds the `{"errors": {...}}` response bodies |
| `graphql` | DGS components: `ArticleDatafetcher`, `CommentDatafetcher`, `ProfileDatafetcher`, `MeDatafetcher`, `TagDatafetcher`, plus `ArticleMutation`, `CommentMutation`, `UserMutation`, `RelationMutation`. `SecurityUtil` reads the current user |
| `graphql.exception` | `GraphQLCustomizeExceptionHandler` |
| `application` | Query services (`ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `TagsQueryService`, `UserQueryService`) and pagination types (`Page`, `CursorPageParameter`, `CursorPager`, `DateTimeCursor`) |
| `application.article`, `application.user` | Command services, input params and custom validators (duplicate title/email/username) |
| `application.data` | Read-model DTOs: `ArticleData`, `ArticleDataList`, `CommentData`, `ProfileData`, `UserData`, `UserWithToken` |
| `core.*` | Entities `Article`, `Tag`, `Comment`, `User`, `FollowRelation`, `ArticleFavorite`. Repository interfaces. `AuthorizationService` (ownership rules) and the `JwtService` interface |
| `infrastructure.repository` | `MyBatis*Repository` implementations of the core repositories |
| `infrastructure.mybatis.mapper` | Write-side mapper interfaces (XML in `resources/mapper`) |
| `infrastructure.mybatis.readservice` | Read-side mapper interfaces that return DTOs directly |
| `infrastructure.service` | `DefaultJwtService` (JJWT, HS512) |

## Request flow

**Write** (e.g. `POST /articles`): the controller receives `NewArticleParam`, then `ArticleCommandService.createArticle` runs, annotated `@Validated`/`@Valid` so a `ConstraintViolationException` becomes 422. It builds a `core.article.Article` and calls `ArticleRepository.save`, implemented by `MyBatisArticleRepository`, which inserts the tags, the article-tag links and the article. The controller then re-reads the result through `ArticleQueryService` to build the response DTO.

**Read** (e.g. `GET /articles`): `ArticleQueryService` asks `ArticleReadService` for matching ids, loads `ArticleData` rows, and then fills in the per-user fields (`favorited`, `favoritesCount`, `author.following`) through `ArticleFavoritesReadService` and `UserRelationshipQueryService`.

GraphQL mutations call the same command services and repositories. Resolvers hand domain objects or DTOs to child resolvers through DGS `localContext` (e.g. `ArticleMutation` puts the `Article` in `localContext`, and `ArticleDatafetcher.getArticle` resolves `ArticlePayload.article` from it).

## Pagination

- REST uses offset pagination (`Page`: `offset` ≥ 0, `limit` defaults to 20 and is capped at 100). The SQL uses `limit #{offset}, #{limit}`.
- GraphQL uses cursor pagination on `created_at`. The cursor is the epoch-millis string from `DateTimeCursor`. The query services fetch `limit + 1` rows to compute `hasNextPage`/`hasPreviousPage`.

## Persistence

- SQLite via `org.xerial:sqlite-jdbc`. The schema comes from the single Flyway migration `db/migration/V1__create_tables.sql` (`users`, `articles`, `article_favorites`, `follows`, `tags`, `article_tags`, `comments`). There are no foreign-key constraints.
- MyBatis XML mappers live in `resources/mapper`. `TransferData.xml` defines the shared result maps (`transfer.data.articleData`, `commentData`, `profileData`, `favoriteCount`) that the read services reuse.
- `DateTimeHandler` maps Joda `DateTime` to SQL timestamps. `JacksonCustomizations` serializes `DateTime` as ISO-8601 UTC.
- IDs are random UUID strings generated in the entity constructors.

## Security

- `WebSecurityConfig` runs stateless (`SessionCreationPolicy.STATELESS`), with CSRF disabled and a `401` entry point.
- Permitted without auth: `OPTIONS *`, `/graphql`, `/graphiql`, `POST /users` and `/users/login`, and `GET /articles/**`, `/profiles/**`, `/tags`, except `GET /articles/feed`. Everything else requires authentication.
- `JwtTokenFilter` reads `Authorization: <scheme> <token>`, validates the token with `JwtService`, loads the `User` by the `sub` claim, and stores it as the principal. Controllers receive it with `@AuthenticationPrincipal User`.
- Ownership checks live in `core.service.AuthorizationService`: only the author can edit or delete an article, and the comment author or the article author can delete a comment.
- CORS allows any origin (`*`) for `HEAD, GET, POST, PUT, DELETE, PATCH` with the `Authorization`, `Cache-Control` and `Content-Type` headers. Credentials aren't allowed.
- Passwords are hashed with `BCryptPasswordEncoder`.

## GraphQL code generation

The `com.netflix.dgs.codegen` plugin's `generateJava` task reads `src/main/resources/schema` and generates the types (`io.spring.graphql.types.*`) and `io.spring.graphql.DgsConstants` under `build/generated`. These classes are not checked in, so run a Gradle build before an IDE can resolve them.

## Tests (`src/test/java/io/spring`)

- `api/*ApiTest`: `@WebMvcTest` with REST Assured MockMvc and mocked services/repositories (`TestWithCurrentUser` stubs the JWT user)
- `application/*`: query-service tests against in-memory SQLite (`@MybatisTest`)
- `infrastructure/*`: MyBatis repository tests and `DefaultJwtServiceTest`
- `core/article/ArticleTest`: slug generation
- There are no GraphQL tests.
