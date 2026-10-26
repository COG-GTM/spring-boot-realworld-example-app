# Architecture

The application keeps a single domain/application core and puts REST and GraphQL in front of it as interchangeable adapters. Persistence uses MyBatis (Data Mapper) over SQLite, and reads and writes go through separate paths (CQRS-style).

## Layers

| Package | Role | Depends on |
| --- | --- | --- |
| `io.spring.api` | REST adapter: Spring MVC controllers (`*Api`), request params, error mapping, Spring Security config and JWT filter | `application`, `core` |
| `io.spring.graphql` | GraphQL adapter: DGS `@DgsComponent` data fetchers and mutations, GraphQL exception handler | `application`, `core`, generated `io.spring.graphql.types` |
| `io.spring.application` | Use cases. Write side: `ArticleCommandService`, `UserService` with validated param objects. Read side: `*QueryService` classes returning DTOs from `application.data`. Offset (`Page`) and cursor (`CursorPageParameter`, `CursorPager`) pagination | `core`, read-service interfaces in `infrastructure.mybatis.readservice` |
| `io.spring.core` | Domain entities (`Article`, `Tag`, `Comment`, `User`, `FollowRelation`, `ArticleFavorite`), repository interfaces, `AuthorizationService`, `JwtService` interface | nothing framework-specific besides Lombok/Joda |
| `io.spring.infrastructure` | MyBatis mappers and `MyBatis*Repository` implementations of the core repositories, read services, `DefaultJwtService`, Joda `DateTimeHandler` type handler | `core`, `application.data` |

## Request flow

**Write (e.g. `POST /articles` or the `createArticle` mutation)**

1. `JwtTokenFilter` authenticates the caller (REST: `@AuthenticationPrincipal User`; GraphQL: `SecurityUtil.getCurrentUser()`).
2. The adapter builds a param object (`NewArticleParam`) and calls `ArticleCommandService.createArticle`. The service is `@Validated`, so bean validation (including the custom `@DuplicatedArticleConstraint`) runs before the method body.
3. The service creates the `Article` entity (id = random UUID, slug from title) and calls `ArticleRepository.save`.
4. `MyBatisArticleRepository` inserts missing tags, the article–tag links and the article (`@Transactional`).
5. The adapter re-reads the result through `ArticleQueryService` to build the response DTO.

Authorization checks for update/delete live in `core.service.AuthorizationService` and are called from the adapters (`canWriteArticle`: author only; `canWriteComment`: comment author or article author).

**Read (e.g. `GET /articles`)**

1. `ArticleQueryService` asks `ArticleReadService` for matching article ids (filtered by tag / author / favorited-by, ordered by `created_at` desc), then loads `ArticleData` rows with author profile and tags.
2. It enriches the DTOs in bulk: favorite counts (`ArticleFavoritesReadService`), and — when a user is present — `favorited` and `author.following` (`UserRelationshipQueryService`).

GraphQL resolvers pass the loaded `ArticleData` map as DGS `localContext` so nested fields (`Article.author`) can be resolved without re-querying the article.

## Persistence

- Schema: a single Flyway migration, `src/main/resources/db/migration/V1__create_tables.sql`, with tables `users`, `articles`, `article_favorites`, `follows`, `tags`, `article_tags`, `comments`. Ids are UUID strings; no foreign-key constraints are declared.
- SQL lives in `src/main/resources/mapper/*.xml`:
  - Write side: `ArticleMapper.xml`, `CommentMapper.xml`, `UserMapper.xml`, `ArticleFavoriteMapper.xml` (bound to `infrastructure.mybatis.mapper.*`).
  - Read side: `ArticleReadService.xml`, `CommentReadService.xml`, `TagReadService.xml`, `UserReadService.xml`, `UserRelationshipQueryService.xml`, `ArticleFavoritesReadService.xml` (bound to `infrastructure.mybatis.readservice.*`).
  - `TransferData.xml` holds shared result maps (`articleData`, `profileData`, `commentData`, `favoriteCount`).
- `application.properties` enables `map-underscore-to-camel-case`, registers `io.spring.infrastructure.mybatis` type handlers (Joda `DateTime`) and turns on SQL debug logging for the mappers and `ArticleReadService`.
- `MyBatisConfig` enables Spring transaction management.

## Security

- `WebSecurityConfig` (extends `WebSecurityConfigurerAdapter`): CSRF disabled, stateless sessions, `401` entry point, CORS allowing any origin with methods `HEAD, GET, POST, PUT, DELETE, PATCH` and headers `Authorization, Cache-Control, Content-Type`. Route rules are listed in [API.md](API.md#authentication).
- `JwtTokenFilter` runs before `UsernamePasswordAuthenticationFilter` and puts the domain `User` in the security context.
- `DefaultJwtService` signs tokens with HS512 using `jwt.secret`; the subject is the user id and expiry is `jwt.sessionTime` seconds.
- Passwords are hashed with `BCryptPasswordEncoder` on registration.

## Pagination

- REST uses offset pagination (`Page`: default limit 20, max 100) and returns a total count.
- GraphQL uses cursor pagination (`CursorPageParameter`: default 20, max 1000). The service fetches `limit + 1` rows to compute `hasNextPage` / `hasPreviousPage`. Cursors are `DateTimeCursor`s serialized as epoch milliseconds: `ArticleData.getCursor()` uses `updatedAt`, `CommentData.getCursor()` uses `createdAt`.

## JSON

- Request bodies are unwrapped from their root key via `UNWRAP_ROOT_VALUE` and `@JsonRootName` on param classes.
- `JacksonCustomizations` serializes Joda `DateTime` as ISO-8601 UTC strings.

## GraphQL code generation

The DGS codegen plugin (`generateJava` task, configured in `build.gradle`) reads `src/main/resources/schema` and generates types (`io.spring.graphql.types.*`) and `DgsConstants` into `build/generated`. These classes are not checked in; run any Gradle compile task to create them before opening the project in an IDE.

## Tests

`src/test/java/io/spring` contains:

- `api/*ApiTest` – MockMvc / REST Assured tests per controller with mocked services (`TestWithCurrentUser` sets up an authenticated user).
- `application/**` – query service tests against SQLite via `@MybatisTest` (`DbTestBase`).
- `infrastructure/**` – repository, transaction and JWT service tests.
- `core/article/ArticleTest` – slug generation.
