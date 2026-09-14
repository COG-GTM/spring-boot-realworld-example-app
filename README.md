# ![RealWorld Example App using Spring Boot](example-logo.png)

> ### Spring Boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API — exposed over both REST and GraphQL.

This codebase demonstrates a fully fledged backend built with Spring Boot + MyBatis including CRUD operations, JWT authentication, pagination (offset for REST, cursor-based for GraphQL), validation and more.

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

## Tech stack

| Component | Version / detail |
| --- | --- |
| Java | source/target compatibility **11** (builds and runs fine on JDK 17) |
| Build | Gradle **7.4** (wrapper included) |
| Spring Boot | **2.6.3** (Web, Validation, HATEOAS, Security) |
| Persistence | MyBatis Spring Boot Starter **2.2.2**, SQLite (`sqlite-jdbc` 3.36.0.3), Flyway migrations |
| GraphQL | Netflix [DGS framework](https://netflix.github.io/dgs/) **4.9.21** + `com.netflix.dgs.codegen` **5.0.6** |
| Auth | Spring Security + JWT (`jjwt` 0.11.2, HS512) |
| Misc | Lombok, Joda-Time 2.10.13 |
| Tests | JUnit 5, REST Assured 4.5.1 (`spring-mock-mvc`), `mybatis-spring-boot-starter-test` |
| Formatting | Spotless 6.2.1 with google-java-format |

## Getting started

You'll need JDK 11 or newer installed (`JAVA_HOME` must point at it).

    ./gradlew bootRun

The server starts on http://localhost:8080. To test that it works:

    curl http://localhost:8080/tags

GraphiQL is available at http://localhost:8080/graphiql and the GraphQL endpoint at `POST http://localhost:8080/graphql`.

On first start Flyway creates the schema (`src/main/resources/db/migration/V1__create_tables.sql`) in a SQLite file named `dev.db` in the project root. The file is git-ignored and deleted by `./gradlew clean`.

### Build, test and format

    ./gradlew compileJava compileTestJava   # compile (also generates GraphQL types)
    ./gradlew test                          # run the test suite (see Database below)
    ./gradlew spotlessJavaApply             # format code (run before committing)
    ./gradlew spotlessJavaCheck             # verify formatting
    ./gradlew clean build                   # full build

Note: the GraphQL Java types (`io.spring.graphql.types.*`, `io.spring.graphql.DgsConstants`) are **generated** into `build/generated` by the DGS codegen plugin from `src/main/resources/schema/schema.graphqls`. They only exist after `./gradlew generateJava` (run automatically by `compileJava`); never edit them by hand. Spotless excludes the generated sources.

### Try it out with Docker

You'll need Docker installed.

    ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
    docker run -p 8081:8080 spring-boot-realworld-example-app

### Try it out with a RealWorld frontend

The entry point address of the backend API is http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests. CORS is enabled for all origins.

## How it works

The application follows Domain-Driven Design with a CQRS-lite split between the write model (domain entities + repositories) and the read model (query services returning DTOs). REST and GraphQL are thin adapters over the same domain layer.

The code under `src/main/java/io/spring` is organized as:

| Package | Role |
| --- | --- |
| `api` | REST adapter — Spring MVC controllers, security (`JwtTokenFilter`, `WebSecurityConfig`) and REST exception handling |
| `graphql` | GraphQL adapter — Netflix DGS data fetchers and mutations |
| `core` | Domain model — entities (`User`, `Article`, `Comment`, `ArticleFavorite`, `FollowRelation`), repository interfaces, `AuthorizationService`, `JwtService` interface |
| `application` | Read-side query services, DTOs (`application/data`), command/param objects, custom validators, pagination helpers |
| `infrastructure` | MyBatis repository implementations and read services, `DefaultJwtService` |

Intended dependency direction: `api` / `graphql` → `application` → `core` ← `infrastructure`. In practice the read side leaks: `application` query services import `infrastructure.mybatis.readservice.*` interfaces directly.

Other resources:

* `src/main/resources/mapper/*.xml` — MyBatis SQL mappings (write mappers and read services)
* `src/main/resources/schema/schema.graphqls` — GraphQL schema
* `src/main/resources/db/migration/` — Flyway migrations
* `src/main/resources/application.properties` — datasource, JWT and MyBatis settings

See [docs/architecture.md](docs/architecture.md) for a deeper walk-through and [docs/api.md](docs/api.md) for the full REST and GraphQL endpoint reference.

## API overview

### REST

All endpoints are served from the root (no `/api` prefix). Authentication uses `Authorization: Token <jwt>` headers.

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/users` | – | Register |
| POST | `/users/login` | – | Login |
| GET / PUT | `/user` | required | Current user / update current user |
| GET | `/profiles/{username}` | optional | Get profile |
| POST / DELETE | `/profiles/{username}/follow` | required | Follow / unfollow |
| GET | `/articles` | optional | List articles (`tag`, `author`, `favorited`, `offset`, `limit`) |
| GET | `/articles/feed` | required | Feed of followed authors (`offset`, `limit`) |
| POST | `/articles` | required | Create article |
| GET / PUT / DELETE | `/articles/{slug}` | optional / required / required | Get / update / delete article |
| POST / DELETE | `/articles/{slug}/favorite` | required | Favorite / unfavorite |
| GET | `/articles/{slug}/comments` | optional | List comments |
| POST | `/articles/{slug}/comments` | required | Add comment |
| DELETE | `/articles/{slug}/comments/{id}` | required | Delete comment |
| GET | `/tags` | – | List tags |

### GraphQL

The schema lives in [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls) and is visualized below.

![](graphql-schema.png)

Queries: `article`, `articles`, `feed`, `me`, `profile`, `tags`.
Mutations: `createUser`, `login`, `updateUser`, `followUser`, `unfollowUser`, `createArticle`, `updateArticle`, `deleteArticle`, `favoriteArticle`, `unfavoriteArticle`, `addComment`, `deleteComment`.

Article and comment lists use Relay-style cursor pagination (`first`/`after` or `last`/`before`; at least one of `first`/`last` is required; if both are given `first` wins). Requests to `/graphql` accept the same `Authorization: Token <jwt>` header as REST.

## Security

Spring Security is configured as stateless with CSRF disabled. `JwtTokenFilter` reads the `Authorization` header, validates the JWT (HS512, subject = user id) and puts the matching `User` into the security context. Tokens are issued on register/login and expire after `jwt.sessionTime` seconds (default 86400).

The signing secret and session time are read from `application.properties` (`jwt.secret`, `jwt.sessionTime`). Override them for any non-local deployment.

## Database

Runtime uses a file-based SQLite database (`dev.db`) so data survives restarts; REST API tests are `@WebMvcTest` slices with mocked services (no database); repository/query-service tests (`DbTestBase`, `ArticleRepositoryTransactionTest`) activate the `test` profile and use in-memory SQLite (`application-test.properties`); `RealworldApplicationTests` (`@SpringBootTest` without a profile) boots against the default `dev.db` datasource. The datasource can be switched to any other JDBC database in `application.properties`, but note the Flyway migration and some MyBatis SQL are written against SQLite.

## Help

Please fork and PR to improve the project.
