# ![RealWorld Example App using Java and Spring Boot](example-logo.png)

> ### Spring Boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

This codebase demonstrates a fully fledged backend for "Conduit" (a Medium.com clone) built with Spring Boot and MyBatis, including CRUD operations, JWT authentication, pagination, and more. It exposes the same domain through **two adapters**: a RealWorld-compatible **REST API** and a **GraphQL API** (Netflix DGS). A React + TypeScript client lives in [`frontend/`](frontend/README.md).

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

More detailed docs:

- [docs/API.md](docs/API.md) — REST endpoint and GraphQL operation reference
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — layers, persistence, security and pagination internals
- [frontend/README.md](frontend/README.md) — React client

## Tech stack

Versions are taken from [`build.gradle`](build.gradle) and [`gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties).

| Area | Library / tool | Version |
| --- | --- | --- |
| Language | Java (source/target compatibility) | 11 |
| Build | Gradle wrapper | 7.4 |
| Framework | Spring Boot (web, validation, security, hateoas) | 2.6.3 |
| GraphQL | Netflix DGS starter / DGS codegen plugin | 4.9.21 / 5.0.6 |
| Persistence | MyBatis Spring Boot starter | 2.2.2 |
| Database | SQLite (`org.xerial:sqlite-jdbc`) | 3.36.0.3 |
| Migrations | Flyway (managed by Spring Boot) | — |
| Auth | JJWT (`jjwt-api` / `jjwt-impl` / `jjwt-jackson`), HS512 | 0.11.2 |
| Dates | Joda-Time | 2.10.13 |
| Boilerplate | Lombok | managed by Spring Boot |
| Tests | JUnit 5, Spring Boot Test, REST Assured (`spring-mock-mvc`), MyBatis test starter | REST Assured 4.5.1 |
| Formatting | Spotless + google-java-format | Spotless 6.2.1 |

## Getting started

You'll need **JDK 11** installed (the build targets Java 11).

    ./gradlew bootRun

The API listens on http://localhost:8080. To check that it works:

    curl http://localhost:8080/tags
    # {"tags":[]}

- GraphQL endpoint: `POST http://localhost:8080/graphql`
- GraphiQL IDE: http://localhost:8080/graphiql — currently renders a blank page: the bundled DGS GraphiQL page loads unpinned `https://unpkg.com/react/umd/...` scripts, which now resolve to React 19 where the UMD builds no longer exist (404). Use `curl` (see [GraphQL](#graphql)) or a standalone GraphQL client instead.

The entry point of the REST API is http://localhost:8080, **not** http://localhost:8080/api as some RealWorld frontend documentation suggests.

### Try it out with Docker

You'll need Docker installed.

    ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
    docker run -p 8081:8080 spring-boot-realworld-example-app

The container then serves the API on http://localhost:8081.

### Run the React frontend

With the backend running on port 8080:

    cd frontend
    npm install
    npm run dev   # http://localhost:3000

See [frontend/README.md](frontend/README.md) for details and known issues.

## Configuration

All settings live in [`src/main/resources/application.properties`](src/main/resources/application.properties):

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:sqlite:dev.db` | SQLite file created in the working directory (deleted by `./gradlew clean`) |
| `jwt.secret` | committed sample key | HMAC-SHA512 signing key for JWTs — override it for any real deployment |
| `jwt.sessionTime` | `86400` | Token lifetime in seconds (24h) |
| `image.default` | `https://static.productionready.io/images/smiley-cyrus.jpg` | Avatar assigned to newly registered users |
| `spring.jackson.deserialization.UNWRAP_ROOT_VALUE` | `true` | Unwraps RealWorld request bodies such as `{"user": {...}}` |
| `mybatis.*` | see file | Mapper XML location (`mapper/*.xml`), type handlers, camel-case mapping |

Any of these can be overridden the usual Spring Boot way, e.g. `JWT_SECRET=... ./gradlew bootRun` or `--jwt.secret=...`.

Tests use [`application-test.properties`](src/main/resources/application-test.properties), which switches to an in-memory SQLite database (`jdbc:sqlite::memory:`).

### Database

The schema is created by Flyway from [`src/main/resources/db/migration/V1__create_tables.sql`](src/main/resources/db/migration/V1__create_tables.sql) (tables `users`, `articles`, `article_favorites`, `follows`, `tags`, `article_tags`, `comments`). SQLite is used for easy local testing without losing data between restarts; switching to another database means changing the datasource properties, adding the JDBC driver, and checking the MyBatis SQL (e.g. `limit #{offset}, #{limit}`) for dialect compatibility.

## API overview

Authenticated requests send the JWT returned by register/login as:

    Authorization: Token <jwt>

### REST

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/users` | – | Register |
| POST | `/users/login` | – | Login |
| GET | `/user` | required | Current user |
| PUT | `/user` | required | Update current user |
| GET | `/profiles/{username}` | optional | Get profile |
| POST / DELETE | `/profiles/{username}/follow` | required | Follow / unfollow |
| GET | `/articles?tag=&author=&favorited=&offset=&limit=` | optional | List articles (offset paging) |
| GET | `/articles/feed?offset=&limit=` | required | Articles by followed users |
| POST | `/articles` | required | Create article |
| GET | `/articles/{slug}` | optional | Get article |
| PUT / DELETE | `/articles/{slug}` | required (author) | Update / delete article |
| POST / DELETE | `/articles/{slug}/favorite` | required | Favorite / unfavorite |
| GET | `/articles/{slug}/comments` | optional | List comments |
| POST | `/articles/{slug}/comments` | required | Add comment |
| DELETE | `/articles/{slug}/comments/{id}` | required (comment or article author) | Delete comment |
| GET | `/tags` | – | All tags |

### GraphQL

The schema is defined in [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls); Java types for it are generated at build time by the DGS codegen plugin (`generateJava` task, package `io.spring.graphql`).

- **Queries:** `article`, `articles`, `feed`, `me`, `profile`, `tags`
- **Mutations:** `createUser`, `login`, `updateUser`, `followUser`, `unfollowUser`, `createArticle`, `updateArticle`, `favoriteArticle`, `unfavoriteArticle`, `deleteArticle`, `addComment`, `deleteComment`
- List fields use Relay-style connections (`edges`, `pageInfo`) with `first`/`after` or `last`/`before` cursor pagination.

![GraphQL schema](graphql-schema.png)

Example:

    curl -X POST http://localhost:8080/graphql \
      -H 'Content-Type: application/json' \
      -d '{"query":"{ tags articles(first: 10) { edges { cursor node { slug title } } pageInfo { hasNextPage endCursor } } }"}'

See [docs/API.md](docs/API.md) for request/response shapes, error formats and more examples.

## How it works

Following some DDD principles, REST and GraphQL are just adapters over the same domain and application layers.

* Domain Driven Design separates business terms from infrastructure terms.
* MyBatis implements the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* [CQRS](https://martinfowler.com/bliki/CQRS.html) separates the read model (query services returning DTOs) from the write model (domain entities + repositories).
* Spring Security with a custom `JwtTokenFilter` provides stateless JWT authentication.

### Project structure

```
src/main/java/io/spring
├── RealWorldApplication.java   # Spring Boot entry point
├── JacksonCustomizations.java  # ISO-8601 UTC serialization for Joda DateTime
├── MyBatisConfig.java          # enables transaction management
├── api/                        # REST adapter (Spring MVC controllers)
│   ├── exception/              # error resources + @RestControllerAdvice
│   └── security/               # WebSecurityConfig, JwtTokenFilter
├── graphql/                    # GraphQL adapter (DGS datafetchers & mutations)
│   └── exception/              # GraphQL exception handler
├── application/                # query services, command services, DTOs, pagination
│   ├── article/                # ArticleCommandService, params, validators
│   ├── user/                   # UserService, params, validators
│   └── data/                   # read-model DTOs (ArticleData, ProfileData, ...)
├── core/                       # domain entities, repository interfaces, domain services
│   ├── article/ comment/ favorite/ user/
│   └── service/                # AuthorizationService, JwtService
└── infrastructure/             # technical implementations
    ├── mybatis/mapper/         # write-side MyBatis mappers
    ├── mybatis/readservice/    # read-side MyBatis mappers
    ├── repository/             # MyBatis*Repository implementations of core repositories
    └── service/                # DefaultJwtService (JJWT)

src/main/resources
├── application.properties
├── db/migration/               # Flyway migrations
├── mapper/                     # MyBatis XML SQL mappings
└── schema/schema.graphqls      # GraphQL schema

frontend/                       # React 18 + TypeScript + Vite client
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for a walkthrough of each layer.

## Run tests

The suite (68 JUnit 5 tests) covers REST API tests (MockMvc + REST Assured), query service tests, MyBatis repository tests and the JWT service.

    ./gradlew test

## Code format

Spotless enforces google-java-format on all `*.java` files (generated sources excluded):

    ./gradlew spotlessJavaCheck   # verify
    ./gradlew spotlessJavaApply   # fix

## Continuous integration

[`.github/workflows/gradle.yml`](.github/workflows/gradle.yml) runs `./gradlew clean test` on JDK 11 (Zulu) for every push and pull request. Note: it still pins `actions/cache@v2`, which GitHub has deprecated and now rejects, so the job fails before running any step until the action is bumped (e.g. to `actions/cache@v4`).

## Help

Please fork and PR to improve the project.
