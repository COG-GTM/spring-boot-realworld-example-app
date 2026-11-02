# Architecture

The backend is a single Spring Boot application organised in DDD-style layers. REST and GraphQL are two adapters on top of the same application and domain code.

```
           HTTP                        HTTP
            │                            │
   ┌────────▼────────┐          ┌────────▼────────┐
   │ api (REST, MVC) │          │ graphql (DGS)   │      adapters
   └────────┬────────┘          └────────┬────────┘
            └─────────────┬──────────────┘
                 ┌────────▼─────────┐
                 │   application    │   query services (read)  /  command services (write)
                 └────────┬─────────┘
                 ┌────────▼─────────┐
                 │      core        │   entities, repository interfaces, AuthorizationService
                 └────────┬─────────┘
                 ┌────────▼─────────┐
                 │  infrastructure  │   MyBatis mappers/read services, repositories, JWT
                 └────────┬─────────┘
                       SQLite (Flyway schema)
```

## Layers

### `io.spring.api` — REST adapter

Spring MVC `@RestController`s: `UsersApi`, `CurrentUserApi`, `ProfileApi`, `ArticlesApi`, `ArticleApi`, `ArticleFavoriteApi`, `CommentsApi`, `TagsApi`. Controllers receive the current user via `@AuthenticationPrincipal User` (the domain `User` placed in the security context by `JwtTokenFilter`), call application services or repositories, and wrap results in the RealWorld root keys.

- `api/security`: `WebSecurityConfig` (stateless session, CSRF off, CORS for all origins, route rules, BCrypt `PasswordEncoder`) and `JwtTokenFilter`.
- `api/exception`: `CustomizeExceptionHandler` maps bean-validation errors to `422 {"errors": {...}}`; `ResourceNotFoundException` (404) and `NoAuthorizationException` (403) are annotated with `@ResponseStatus`.

### `io.spring.graphql` — GraphQL adapter

`@DgsComponent` classes resolve the schema in `src/main/resources/schema/schema.graphqls`:

- Queries/field resolvers: `ArticleDatafetcher`, `CommentDatafetcher`, `ProfileDatafetcher`, `MeDatafetcher`, `TagDatafetcher`.
- Mutations: `UserMutation`, `ArticleMutation`, `CommentMutation`, `RelationMutation`.
- `SecurityUtil.getCurrentUser()` reads the user from the Spring Security context (the same JWT filter applies to `/graphql`).
- `exception/GraphQLCustomizeExceptionHandler` maps validation and login errors to typed GraphQL errors.

Parent resolvers pass the loaded read models to child resolvers through DGS `localContext` (e.g. a map of slug → `ArticleData` so `Article.author` and `Article.comments` can be resolved without re-querying the article).

The GraphQL Java types (`io.spring.graphql.types.*`) and `DgsConstants` are generated at build time by the `com.netflix.dgs.codegen` plugin (`generateJava` task in `build.gradle`).

### `io.spring.application` — use cases

Read side (CQRS-style queries returning DTOs from `application/data`):

- `ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `UserQueryService`, `TagsQueryService`.
- They call MyBatis read services and then enrich results with per-user state (`favorited`, `favoritesCount`, `following`).

Write side:

- `article/ArticleCommandService` (create/update) with `NewArticleParam`, `UpdateArticleParam` and the `@DuplicatedArticleConstraint` validator.
- `user/UserService` (register/update) with `RegisterParam`, `UpdateUserParam`, `UpdateUserCommand` and validators for duplicate email/username.
- Simpler writes (follow, favorite, comment, delete) are done directly by the adapters through core repositories.

Pagination helpers: `Page` (offset/limit for REST, limit capped at 100) and `CursorPageParameter` / `CursorPager` / `DateTimeCursor` (cursor paging for GraphQL; one extra row is fetched to detect another page).

### `io.spring.core` — domain

- `article`: `Article` (generates UUID id and slug, tracks timestamps), `Tag`, `ArticleRepository`.
- `comment`: `Comment`, `CommentRepository`.
- `favorite`: `ArticleFavorite`, `ArticleFavoriteRepository`.
- `user`: `User`, `FollowRelation`, `UserRepository`.
- `service`: `AuthorizationService` (article: only the author may write; comment: the comment author or the article author may delete) and the `JwtService` interface.

### `io.spring.infrastructure` — technical implementations

- `repository/MyBatis*Repository`: implement the core repository interfaces with the write-side mappers in `mybatis/mapper` (`ArticleMapper`, `CommentMapper`, `UserMapper`, `ArticleFavoriteMapper`). `MyBatisArticleRepository.save` inserts new articles and their tags (creating missing tags) or updates existing ones, in a transaction.
- `mybatis/readservice`: query interfaces used by the application query services (`ArticleReadService`, `CommentReadService`, `UserReadService`, `TagReadService`, `ArticleFavoritesReadService`, `UserRelationshipQueryService`).
- `mybatis/DateTimeHandler`: Joda `DateTime` ↔ SQL timestamp type handler.
- `service/DefaultJwtService`: JJWT implementation of `JwtService` (HS512, subject = user id, expiry from `jwt.sessionTime`).

SQL for both mappers and read services lives in `src/main/resources/mapper/*.xml`; `TransferData.xml` holds result maps shared by the read services (`articleData`, `commentData`, `profileData`).

## Request flow examples

**`POST /articles` (REST)**: `JwtTokenFilter` authenticates → `ArticlesApi.createArticle` → `ArticleCommandService.createArticle` validates `NewArticleParam` and builds an `Article` → `MyBatisArticleRepository.save` writes `articles`, `tags`, `article_tags` → `ArticleQueryService.findById` reads back an `ArticleData` for the response.

**`{ article(slug) { author { username } comments(first: 10) { ... } } }` (GraphQL)**: `ArticleDatafetcher.findArticleBySlug` loads `ArticleData` and stores it in `localContext` → `ProfileDatafetcher.getAuthor` and `CommentDatafetcher.articleComments` read it from there and call `CommentQueryService.findByArticleIdWithCursor`.

## Persistence

- SQLite file `dev.db` by default; in-memory SQLite in tests.
- Schema managed by Flyway (`src/main/resources/db/migration/V1__create_tables.sql`). There are no foreign-key constraints; integrity is maintained by the application.
- IDs are UUID strings generated in the domain objects.

## Security

- Passwords are hashed with BCrypt on registration.
- JWTs are signed with `jwt.secret` from `application.properties`; the checked-in value is a development key and must be overridden outside local development.
- Authorization (ownership) checks are done in the adapters via `AuthorizationService` before updates/deletes.

## Tests

`src/test/java/io/spring` contains:

- `api/*ApiTest`: `@WebMvcTest` controller tests with mocked services/repositories (REST Assured MockMvc).
- `application/*QueryServiceTest`: query services against an in-memory database.
- `infrastructure/*RepositoryTest`, `DefaultJwtServiceTest`: MyBatis repositories and JWT handling.
- `core/article/ArticleTest`: slug generation and entity behaviour.

The GraphQL adapter has no dedicated tests.
