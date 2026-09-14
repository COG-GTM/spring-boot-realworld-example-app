# Architecture

This document describes how the backend is layered and how a request flows through it. It is grounded in the code under `src/main/java/io/spring`.

## Layers

```
 +-----------+   +-------------+
 |    api    |   |   graphql   |      adapters (Spring MVC / Netflix DGS)
 +-----------+   +-------------+
        \             /
         v           v
      +------------------+
      |   application    |      command services, query services, DTOs, validators
      +------------------+
                |
                v
      +------------------+       +------------------+
      |       core       | <---- |  infrastructure  |   MyBatis repositories/read services, JWT
      +------------------+       +------------------+
```

| Package | Contents |
| --- | --- |
| `io.spring.api` | REST controllers (`UsersApi`, `CurrentUserApi`, `ProfileApi`, `ArticlesApi`, `ArticleApi`, `ArticleFavoriteApi`, `CommentsApi`, `TagsApi`), `api.security` (`WebSecurityConfig`, `JwtTokenFilter`), `api.exception` (`CustomizeExceptionHandler` and the exception types used by both adapters) |
| `io.spring.graphql` | DGS components: `ArticleDatafetcher`, `CommentDatafetcher`, `MeDatafetcher`, `ProfileDatafetcher`, `TagDatafetcher`, `ArticleMutation`, `CommentMutation`, `RelationMutation`, `UserMutation`, `SecurityUtil`, `graphql.exception.GraphQLCustomizeExceptionHandler` |
| `io.spring.core` | Domain entities `User`, `FollowRelation`, `Article`, `Tag`, `Comment`, `ArticleFavorite`; repository interfaces `UserRepository`, `ArticleRepository`, `CommentRepository`, `ArticleFavoriteRepository`; `service.AuthorizationService` (ownership checks) and `service.JwtService` (interface) |
| `io.spring.application` | Write side: `article.ArticleCommandService`, `user.UserService` plus `*Param` / `*Command` inputs and bean-validation constraints (`DuplicatedArticleConstraint`, `DuplicatedEmailConstraint`, `DuplicatedUsernameConstraint`, `UpdateUserConstraint`). Read side: `ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `TagsQueryService`, `UserQueryService` returning DTOs from `application.data` (`ArticleData`, `ArticleDataList`, `CommentData`, `ProfileData`, `UserData`, `UserWithToken`). Pagination: `Page` (offset), `CursorPager`, `CursorPageParameter`, `PageCursor`, `DateTimeCursor`, `Node` |
| `io.spring.infrastructure` | `repository.MyBatis*Repository` (implement `core` repositories), `mybatis.mapper.*Mapper` (write SQL), `mybatis.readservice.*ReadService` (read SQL), `mybatis.DateTimeHandler` (Joda `DateTime` type handler), `service.DefaultJwtService` (jjwt HS512) |
| `io.spring` (root) | `RealWorldApplication`, `JacksonCustomizations` (ISO-8601 UTC serializer for Joda `DateTime`), `MyBatisConfig` (`@EnableTransactionManagement`), `Util` |

`core` has no Spring or SQL dependencies. `infrastructure` implements the `core` interfaces; `application` depends on `core` and, for read services, on `infrastructure.mybatis.readservice`.

## CQRS-lite

**Write path** — adapter → `application` command service (or the controller directly for simple cases such as favorites, follows and comments) → domain entity method → `core` repository interface → `infrastructure.repository.MyBatis*Repository` → `mybatis.mapper.*Mapper`. Repositories deal only in domain objects.

Example (`PUT /articles/{slug}`, `ArticleApi.updateArticle`):

```
articleRepository.findBySlug(slug)                 // load entity
AuthorizationService.canWriteArticle(user, article) // guard
articleCommandService.updateArticle(article, param) // Article.update(...) + articleRepository.save(article)
articleQueryService.findBySlug(newSlug, user)       // re-read as DTO for the response
```

**Read path** — adapter → `application/*QueryService` → `infrastructure.mybatis.readservice.*ReadService` (MyBatis `@Mapper` interfaces backed by `src/main/resources/mapper/*ReadService.xml`) → DTOs from `application.data`. Query services enrich DTOs with the caller-specific fields (`favorited`, `following`, `favoritesCount`) using `ArticleFavoritesReadService` and `UserRelationshipQueryService`.

Shared MyBatis result maps for the DTOs live in `src/main/resources/mapper/TransferData.xml` (namespace `transfer.data`).

## Pagination

* REST uses offset pagination: `Page(offset, limit)` with query parameters `offset` (default 0) and `limit` (default 20). List responses are `{ "articles": [...], "articlesCount": n }`.
* GraphQL uses Relay-style cursor pagination. `CursorPager` / `CursorPageParameter` implement keyset paging over `createdAt`; `DateTimeCursor` serializes the cursor as epoch milliseconds. Each connection field accepts `first`/`after` (forward) or `last`/`before` (backward); exactly one of `first` or `last` must be supplied. The query services fetch `limit + 1` rows to compute `hasNextPage` / `hasPreviousPage`.

## Security

* `WebSecurityConfig` (extends `WebSecurityConfigurerAdapter`): stateless sessions, CSRF disabled, CORS enabled for all origins with methods `HEAD, GET, POST, PUT, DELETE, PATCH` and headers `Authorization, Cache-Control, Content-Type`. Unauthenticated access to protected endpoints returns `401`.
* Public routes: `OPTIONS *`, `/graphql`, `/graphiql`, `POST /users`, `POST /users/login`, `GET /articles/**`, `GET /profiles/**`, `GET /tags`. `GET /articles/feed` and everything else require authentication.
* `JwtTokenFilter` runs before `UsernamePasswordAuthenticationFilter`. It reads `Authorization: Token <jwt>` (any scheme word works — the filter takes the second whitespace-separated token), resolves the JWT subject to a `User` via `UserRepository`, and sets it as the authentication principal. Controllers receive it through `@AuthenticationPrincipal User`.
* Because `/graphql` is `permitAll`, GraphQL resolvers authenticate themselves through `SecurityUtil.getCurrentUser()` and throw `graphql.exception.AuthenticationException` where a user is required (the JWT filter still populates the principal for `/graphql` requests). Note that only `InvalidAuthenticationException` (bad login) is mapped to a typed `UNAUTHENTICATED` error; `AuthenticationException` falls through to the DGS default handler.
* `DefaultJwtService` signs tokens with HS512 using `jwt.secret`; `sub` is the user id, expiry is `jwt.sessionTime` seconds. Passwords are hashed with BCrypt.
* `AuthorizationService`: an article may be modified/deleted only by its author; a comment may be deleted by the comment author or the article author.

## Validation and errors

* Request bodies are Lombok/Jackson classes annotated with `@JsonRootName` (Jackson `UNWRAP_ROOT_VALUE` is enabled, so REST payloads are wrapped as `{"user": {...}}`, `{"article": {...}}`, `{"comment": {...}}`).
* Bean validation runs on controller parameters (`@Valid`) and again on `@Validated` services (`ArticleCommandService`, `UserService`) so that GraphQL mutations get the same checks. Custom constraints check duplicate emails/usernames/article titles against the repositories.
* REST: `CustomizeExceptionHandler` maps validation failures and `InvalidAuthenticationException` to `422` with a body `{"errors": {"field": ["message", ...]}}` (serialized by `ErrorResourceSerializer`). `ResourceNotFoundException` → `404`, `NoAuthorizationException` → `403`.
* GraphQL: `GraphQLCustomizeExceptionHandler` maps `InvalidAuthenticationException` to an `UNAUTHENTICATED` typed error and `ConstraintViolationException` to a `BAD_REQUEST` error with per-field extensions. `createUser` additionally returns validation failures as data via the `UserResult = UserPayload | Error` union.

## Persistence

* SQLite via `sqlite-jdbc`. Runtime datasource `jdbc:sqlite:dev.db` (file in the project root; deleted by `./gradlew clean`), test profile `jdbc:sqlite::memory:`.
* Schema is created by Flyway from `src/main/resources/db/migration/V1__create_tables.sql`: tables `users`, `articles`, `article_favorites`, `follows`, `tags`, `article_tags`, `comments`. Ids are UUID strings generated in the domain constructors.
* MyBatis settings live in `application.properties` (`map-underscore-to-camel-case`, mapper XML under `mapper/*.xml`, type handlers in `io.spring.infrastructure.mybatis`).
* Joda `DateTime` columns are converted by `DateTimeHandler` and serialized as ISO-8601 UTC strings by `JacksonCustomizations`.

## GraphQL code generation

`build.gradle` applies `com.netflix.dgs.codegen`. `generateJava` reads `src/main/resources/schema/*.graphqls` and writes `io.spring.graphql.types.*` and `io.spring.graphql.DgsConstants` to `build/generated`. These are compile-time dependencies of the `graphql` package, so run `./gradlew compileJava` (or `generateJava`) before opening the project in an IDE; do not hand-edit generated sources. Spotless excludes `build/generated/**`.

## Tests

`src/test/java/io/spring`:

* `api/*ApiTest` — REST Assured (`spring-mock-mvc`) tests with `@WebMvcTest`-style slices and mocked services; `TestWithCurrentUser` sets up an authenticated user.
* `application/**` — query service tests against the real MyBatis layer (`DbTestBase`: `@MybatisTest`, profile `test`, in-memory SQLite).
* `infrastructure/**` — repository and `DefaultJwtService` tests.
* `core/article/ArticleTest` — slug generation.
* `RealworldApplicationTests` — context load.

Run with `./gradlew test`.
