# Architecture

The application follows a layered, DDD-inspired design with a CQRS-style split between the write model (domain entities + repositories) and the read model (query services returning DTOs). REST and GraphQL are thin adapters over the same application layer.

```
          HTTP (REST)                 HTTP (POST /graphql)
               │                              │
     io.spring.api (Spring MVC)     io.spring.graphql (Netflix DGS)
               │                              │
               └──────────────┬───────────────┘
                              │
                 io.spring.application
      ┌───────────────────────┴───────────────────────┐
  write side                                      read side
  ArticleCommandService, UserService          *QueryService → DTOs (application.data)
      │                                               │
  io.spring.core (entities, repository           io.spring.infrastructure.mybatis.readservice
  interfaces, AuthorizationService)              (read-side MyBatis mappers)
      │
  io.spring.infrastructure.repository
  (MyBatis*Repository → mybatis.mapper)
                              │
                     SQLite (Flyway schema)
```

## Layers

### `core` — domain

- Entities: `Article` (with `Tag`s), `Comment`, `ArticleFavorite`, `User`, `FollowRelation`. IDs are random UUID strings generated in constructors.
- `Article.toSlug(title)` derives the slug; `Article.update(...)` and `User.update(...)` only apply non-empty values.
- Repository interfaces: `ArticleRepository`, `CommentRepository`, `ArticleFavoriteRepository`, `UserRepository`.
- Domain services: `AuthorizationService` (article writes allowed for the author; comment deletes for the comment author or article author) and the `JwtService` interface.

### `application` — use cases and read model

- Write side: `ArticleCommandService` (create/update) and `UserService` (register/update). Both are `@Validated`; parameter objects (`NewArticleParam`, `UpdateArticleParam`, `RegisterParam`, `UpdateUserParam`) carry Bean Validation constraints, including custom validators for duplicate article titles, emails and usernames, and the class-level `UpdateUserConstraint`.
- Read side: `ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `TagsQueryService`, `UserQueryService`. They return DTOs from `application.data` (`ArticleData`, `ArticleDataList`, `CommentData`, `ProfileData`, `UserData`, `UserWithToken`) and enrich them with per-user state (`favorited`, `favoritesCount`, `following`) in batch queries.
- Pagination primitives: `Page` (offset/limit, limit ≤ 100) used by REST, and `CursorPageParameter`/`CursorPager`/`DateTimeCursor` (limit ≤ 1000) used by GraphQL.

### `infrastructure` — technical details

- `repository/MyBatis*Repository` implement the core repositories on top of the write-side mappers in `mybatis/mapper` (`ArticleMapper`, `CommentMapper`, `ArticleFavoriteMapper`, `UserMapper`). `MyBatisArticleRepository.save` inserts missing tags and article/tag links in one transaction.
- `mybatis/readservice/*` are read-side mapper interfaces backing the query services.
- SQL lives in `src/main/resources/mapper/*.xml`; `TransferData.xml` holds the shared result maps for DTOs.
- `mybatis/DateTimeHandler` maps Joda `DateTime` to/from UTC timestamps.
- `service/DefaultJwtService` issues and parses HS512 JWTs with JJWT using `jwt.secret` and `jwt.sessionTime`.

### `api` — REST adapter

Controllers: `UsersApi`, `CurrentUserApi`, `ProfileApi`, `ArticlesApi`, `ArticleApi`, `ArticleFavoriteApi`, `CommentsApi`, `TagsApi`. They resolve the caller via `@AuthenticationPrincipal User`, use repositories/command services for writes, and return query-service DTOs wrapped in a root key.

`exception/CustomizeExceptionHandler` turns validation failures into RealWorld-style `{"errors": {field: [messages]}}` responses with status 422 (`ErrorResourceSerializer` performs the grouping).

### `graphql` — GraphQL adapter

Schema-first with Netflix DGS. The `generateJava` Gradle task generates types and `DgsConstants` from `src/main/resources/schema` into package `io.spring.graphql` (under `build/generated`).

- Datafetchers: `ArticleDatafetcher` (queries `article`, `articles`, `feed`; `Profile.articles/favorites/feed`; `ArticlePayload.article`; `Comment.article`), `CommentDatafetcher` (`Article.comments`, `CommentPayload.comment`), `ProfileDatafetcher`, `MeDatafetcher`, `TagDatafetcher`.
- Mutations: `UserMutation`, `RelationMutation`, `ArticleMutation`, `CommentMutation`.
- Mutations return an empty payload and pass the domain object through DGS *local context*; the nested payload datafetcher then loads the read-model DTO. Parent fetchers likewise pass a `slug → ArticleData` / `id → CommentData` map as local context so child fields (`author`, `comments`) can resolve without re-querying the article.
- `SecurityUtil.getCurrentUser()` reads the principal set by `JwtTokenFilter`.
- `exception/GraphQLCustomizeExceptionHandler` maps auth and validation exceptions to typed GraphQL errors.

## Security

`WebSecurityConfig` (Spring Security, `WebSecurityConfigurerAdapter`) configures a stateless filter chain: CSRF disabled, CORS enabled, `HttpStatusEntryPoint(401)` for unauthenticated access, and `JwtTokenFilter` registered before `UsernamePasswordAuthenticationFilter`. The filter validates the token and loads the `User` entity as the authentication principal. Passwords are hashed with `BCryptPasswordEncoder`. See [API.md](API.md#authentication) for the path rules.

## Persistence

- SQLite file database `dev.db` by default; in-memory SQLite for tests.
- Flyway applies `db/migration/V1__create_tables.sql` on startup.
- The schema has unique constraints on `users.username`, `users.email` and `articles.slug`, but no foreign keys; relational integrity is maintained by the application.

## Serialization

`JacksonCustomizations` registers a serializer that writes Joda `DateTime` as ISO-8601 UTC. GraphQL datafetchers format timestamps the same way.

## Testing

`src/test/java/io/spring`:

- `api/*ApiTest` — controller tests with `@WebMvcTest` + REST Assured MockMvc; `TestWithCurrentUser` stubs an authenticated user.
- `application/**` — query service tests against the database (`DbTestBase`).
- `infrastructure/**` — MyBatis repository tests and `DefaultJwtServiceTest`.
- `core/article/ArticleTest` — slug generation.

Run with `./gradlew test`.
