# Architecture

This document describes how the backend in `src/main/java/io/spring` is organized. It reflects the code as it exists today; see the [README](../README.md) for setup and the [API reference](API.md) for endpoints.

## Layers

The code follows a Domain-Driven-Design-inspired layering. REST and GraphQL are two adapters over the same application and domain code.

```
           HTTP (REST)                    HTTP (GraphQL, POST /graphql)
               │                                     │
     io.spring.api (Spring MVC)        io.spring.graphql (Netflix DGS)
               └──────────────┬──────────────────────┘
                              │
                  io.spring.application
     command services (writes)   │   query services (reads)
                              │
                     io.spring.core
        entities + repository interfaces + domain services
                              │
                 io.spring.infrastructure
   MyBatis repositories, MyBatis read services, JWT implementation
                              │
                    SQLite (via Flyway schema)
```

| Package | Responsibility | Key types |
| --- | --- | --- |
| `api` | REST controllers, request/response mapping, error handling, security config | `ArticlesApi`, `ArticleApi`, `CommentsApi`, `UsersApi`, `CurrentUserApi`, `ProfileApi`, `ArticleFavoriteApi`, `TagsApi`; `api.security.WebSecurityConfig`, `JwtTokenFilter`; `api.exception.CustomizeExceptionHandler` |
| `graphql` | DGS data fetchers (queries + nested fields) and mutations | `ArticleDatafetcher`, `CommentDatafetcher`, `ProfileDatafetcher`, `MeDatafetcher`, `TagDatafetcher`, `ArticleMutation`, `CommentMutation`, `UserMutation`, `RelationMutation`; `graphql.exception.GraphQLCustomizeExceptionHandler` |
| `application` | Use cases. Command services validate input and drive the domain; query services build read-model DTOs | `article.ArticleCommandService`, `user.UserService`, `ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `UserQueryService`, `TagsQueryService`; DTOs in `application.data` |
| `core` | Domain entities, repository interfaces, domain services | `Article`, `Tag`, `Comment`, `ArticleFavorite`, `User`, `FollowRelation`; `*Repository` interfaces; `AuthorizationService`, `JwtService` |
| `infrastructure` | Technical implementations of `core` interfaces and read-side queries | `repository.MyBatis*Repository`, `mybatis.mapper.*`, `mybatis.readservice.*`, `mybatis.DateTimeHandler`, `service.DefaultJwtService` |

## CQRS: separate write and read paths

**Write side.** Controllers and mutations call command services (`ArticleCommandService`, `UserService`) or repositories directly. Repositories (`core.*Repository`) are implemented in `infrastructure.repository` on top of MyBatis mappers (`infrastructure.mybatis.mapper`, XML in `src/main/resources/mapper/*Mapper.xml`). They load and save domain entities.

**Read side.** Queries skip the domain model. Query services in `application` call MyBatis read services (`infrastructure.mybatis.readservice`, XML in `src/main/resources/mapper/*ReadService.xml`). These map SQL results straight into DTOs such as `ArticleData`, `CommentData`, `ProfileData` and `UserData`. Query services then add per-viewer fields (`favorited`, `favoritesCount`, `following`) through `ArticleFavoritesReadService` and `UserRelationshipQueryService`.

Both REST and GraphQL use the same query services. Two pagination styles are available:

- **Offset/limit** (`application.Page`), used by REST. Defaults are `offset=0` and `limit=20`, with a maximum `limit` of 100.
- **Cursor** (`CursorPageParameter`, `CursorPager`, `DateTimeCursor`), used by GraphQL. The cursor is the item timestamp in epoch milliseconds: `updatedAt` for articles and `createdAt` for comments. The default page size is 20 and the maximum is 1000. The service fetches `limit + 1` rows to compute `hasNextPage`/`hasPreviousPage`.

## Validation

Input objects use Bean Validation:

- `RegisterParam`: `email` (not blank, valid email, unique via `@DuplicatedEmailConstraint`), `username` (not blank, unique via `@DuplicatedUsernameConstraint`), `password` (not blank).
- `UpdateUserParam`: `email` must be a valid email. `UserService` checks email/username uniqueness against other users through `UpdateUserConstraint`.
- `NewArticleParam`: `title` (not blank, and the derived slug must be unique via `@DuplicatedArticleConstraint`), `description` and `body` (not blank).
- `CommentsApi.NewCommentParam`: `body` must not be blank.

Command services are annotated with `@Validated`, so violations raise `ConstraintViolationException`. REST returns 422 with an `errors` map. GraphQL `createUser` returns the `Error` union member, and other GraphQL operations return a `BAD_REQUEST` GraphQL error.

## Domain notes

- **Slugs.** `Article.toSlug(title)` lowercases the title and replaces runs of whitespace and some punctuation (`&`, `?`, `,`, `.`, curly quotes, full-width forms) with `-`. Changing the title in an update regenerates the slug.
- **Tags.** Tags are stored once in `tags` and linked through `article_tags`. `MyBatisArticleRepository` inserts missing tags when an article is saved. Tags cannot be changed after creation: `UpdateArticleParam` and the GraphQL `UpdateArticleInput` only accept `title`, `description` and `body`.
- **Authorization.** `AuthorizationService.canWriteArticle` lets only the author update or delete an article. `canWriteComment` lets the comment author or the article author delete a comment. Failed checks return 403 (`NoAuthorizationException`) in REST.
- **IDs.** Entities use random UUID strings as primary keys.

## Persistence

- **Database.** SQLite, configured by `spring.datasource.url` (`jdbc:sqlite:dev.db` by default and an in-memory database in tests).
- **Schema.** A single Flyway migration, `src/main/resources/db/migration/V1__create_tables.sql`, creates the tables `users`, `articles`, `article_favorites`, `follows`, `tags`, `article_tags` and `comments`.
- **MyBatis.** Mapper XML files are loaded from `classpath:mapper/*.xml`. `DateTimeHandler` maps Joda `DateTime` to SQL timestamps, and underscore-to-camelCase mapping is enabled.
- **Transactions.** `MyBatisConfig` enables `@EnableTransactionManagement`, and `MyBatisArticleRepository.save` is `@Transactional`.

## Security

- `WebSecurityConfig` sets up stateless Spring Security with CSRF disabled and CORS open to all origins. The allowed methods are `HEAD, GET, POST, PUT, DELETE, PATCH`, and the allowed headers are `Authorization, Cache-Control, Content-Type`.
- Public routes are `OPTIONS *`, `/graphql`, `/graphiql`, `POST /users`, `POST /users/login`, and `GET` on `/articles/**`, `/profiles/**` and `/tags`. `GET /articles/feed` requires authentication, as does every other route.
- `JwtTokenFilter` reads `Authorization: Token <jwt>` (any scheme word is accepted, since the filter only takes the second whitespace-separated part). It validates the token with `DefaultJwtService` (HS512, `jwt.secret`, `jwt.sessionTime` seconds), loads the `User`, and sets it as the authentication principal.
- An unauthenticated request to a protected REST route gets `401` with an empty body (`HttpStatusEntryPoint`).
- `/graphql` itself is public, so each GraphQL resolver enforces authentication. Mutations that need a user throw `graphql.exception.AuthenticationException`, which is not mapped by `GraphQLCustomizeExceptionHandler` and therefore surfaces as an `INTERNAL` GraphQL error. `me` and `updateUser` return `null` for anonymous callers.
- Passwords are hashed with `BCryptPasswordEncoder`.

## GraphQL code generation

The `com.netflix.dgs.codegen` Gradle plugin (`generateJava` task) generates Java types and constants (`io.spring.graphql.types.*`, `io.spring.graphql.DgsConstants`) from `src/main/resources/schema/schema.graphqls`. Generated sources go to `build/generated` and are not checked in, so build once (for example with `./gradlew compileJava`) before opening the project in an IDE.

## Tests

Tests live in `src/test/java/io/spring`:

- `api/`: MockMvc + REST Assured controller tests with mocked services.
- `application/`: query service tests against the in-memory test database (extending `infrastructure.DbTestBase`, `test` profile).
- `core/`: domain unit tests.
- `infrastructure/`: MyBatis repository and JWT service tests.

There are currently no dedicated GraphQL tests.
