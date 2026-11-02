# ![RealWorld Example App using Spring Boot](example-logo.png)

> ### Spring Boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

This repository is a backend for the RealWorld "Conduit" blogging platform (users, profiles, follows, articles, tags, favorites and comments). The same domain is exposed through **two adapters** that share one application/domain layer:

- a **REST API** that follows the [RealWorld API spec](https://realworld-docs.netlify.app/specifications/backend/introduction/), and
- a **GraphQL API** built with [Netflix DGS](https://netflix.github.io/dgs/).

A small React + TypeScript client lives in [`frontend/`](frontend/README.md).

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

## Contents

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [API overview](#api-overview)
- [How it works](#how-it-works)
- [Project structure](#project-structure)
- [Testing and code format](#testing-and-code-format)
- [Frontend](#frontend)
- [Further documentation](#further-documentation)

## Tech stack

Versions are taken from [`build.gradle`](build.gradle) and the Gradle wrapper.

| Area | Technology | Version |
| --- | --- | --- |
| Language | Java (source/target compatibility) | 11 |
| Build | Gradle wrapper | 7.4 |
| Framework | Spring Boot (Web, Validation, Security, HATEOAS) | 2.6.3 |
| GraphQL | Netflix DGS starter / DGS codegen plugin | 4.9.21 / 5.0.6 |
| Persistence | MyBatis Spring Boot starter | 2.2.2 |
| Database | SQLite (`org.xerial:sqlite-jdbc`) | 3.36.0.3 |
| Migrations | Flyway (version managed by Spring Boot) | — |
| Auth | JJWT (`jjwt-api` / `jjwt-impl` / `jjwt-jackson`), HS512 | 0.11.2 |
| Dates | Joda-Time | 2.10.13 |
| Boilerplate | Lombok (version managed by Spring Boot) | — |
| Tests | JUnit 5, Spring Boot Test, REST Assured (`spring-mock-mvc`), MyBatis test starter | REST Assured 4.5.1 |
| Formatting | Spotless + google-java-format | Spotless 6.2.1 |

## Getting started

### Prerequisites

- **JDK 11.** The build targets Java 11 and CI uses Zulu JDK 11. Compiling and running tests also works on JDK 17, but `./gradlew build` / `spotlessJavaCheck` fail on JDK 17+ because the google-java-format version used by Spotless 6.2.1 cannot access the `jdk.compiler` internals. Point `JAVA_HOME` at a JDK 11 install when running a full build.
- No database server is needed: the app uses a local SQLite file.

### Run the application

```bash
./gradlew bootRun
```

The server listens on **http://localhost:8080**. On first start Flyway creates the schema in `./dev.db` (SQLite file in the project root). `./gradlew clean` deletes `dev.db`.

Check that it works:

```bash
curl http://localhost:8080/tags
# {"tags":[]}
```

### Build and run a jar

```bash
./gradlew bootJar
java -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar
```

### Docker

Spring Boot's buildpack task is available:

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

Note: Spring Boot 2.6.3's `bootBuildImage` talks to the Docker Engine API v1.24, which recent Docker Engine releases no longer accept. If the task fails with an API version error, build the jar with `bootJar` and run it with `java -jar` (or wrap it in your own Dockerfile). There is no Dockerfile in this repository.

### Use it with a RealWorld frontend

The API root is **http://localhost:8080**, **not** `http://localhost:8080/api` as some frontend docs suggest. CORS is open to all origins (see `WebSecurityConfig`).

## Configuration

All settings live in [`src/main/resources/application.properties`](src/main/resources/application.properties):

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:sqlite:dev.db` | Database location. Tests override it with `jdbc:sqlite::memory:` via `application-test.properties`. |
| `spring.jackson.deserialization.UNWRAP_ROOT_VALUE` | `true` | REST request bodies are wrapped in a root object (`{"user": {...}}`, `{"article": {...}}`). |
| `jwt.secret` | a checked-in development key | HS512 signing key for JWTs. **Override it for any non-local deployment** (e.g. `JWT_SECRET=... ./gradlew bootRun` via Spring's relaxed binding, or `--jwt.secret=...`). |
| `jwt.sessionTime` | `86400` | Token lifetime in seconds (24 h). |
| `image.default` | `https://static.productionready.io/images/smiley-cyrus.jpg` | Avatar assigned to newly registered users. |
| `mybatis.*` | see file | Mapper XML location (`mapper/*.xml`), camel-case mapping, Joda `DateTime` type handler package. |

The schema is defined by a single Flyway migration, [`db/migration/V1__create_tables.sql`](src/main/resources/db/migration/V1__create_tables.sql) (tables `users`, `articles`, `article_favorites`, `follows`, `tags`, `article_tags`, `comments`). Switching to another database means changing the datasource properties, adding the JDBC driver, and checking the MyBatis SQL (it uses `limit offset, count` syntax).

## API overview

A full reference with request/response shapes is in [`docs/API.md`](docs/API.md).

### Authentication

Register or log in to obtain a JWT, then send it on every request that needs a user:

```
Authorization: Token <jwt>
```

`JwtTokenFilter` takes the second whitespace-separated part of the header, so `Bearer <jwt>` is accepted too. The token subject is the user id. Sessions are stateless.

### REST endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/users` | – | Register |
| POST | `/users/login` | – | Log in |
| GET | `/user` | required | Current user |
| PUT | `/user` | required | Update current user |
| GET | `/profiles/{username}` | optional | Get profile |
| POST | `/profiles/{username}/follow` | required | Follow user |
| DELETE | `/profiles/{username}/follow` | required | Unfollow user |
| GET | `/articles?tag=&author=&favorited=&offset=0&limit=20` | optional | List articles (newest first) |
| GET | `/articles/feed?offset=0&limit=20` | required | Articles by followed users |
| POST | `/articles` | required | Create article |
| GET | `/articles/{slug}` | optional | Get article |
| PUT | `/articles/{slug}` | required (author) | Update article |
| DELETE | `/articles/{slug}` | required (author) | Delete article |
| POST | `/articles/{slug}/favorite` | required | Favorite article |
| DELETE | `/articles/{slug}/favorite` | required | Unfavorite article |
| GET | `/articles/{slug}/comments` | optional | List comments |
| POST | `/articles/{slug}/comments` | required | Add comment |
| DELETE | `/articles/{slug}/comments/{id}` | required (comment or article author) | Delete comment |
| GET | `/tags` | – | List tags |

`limit` is capped at 100. Validation errors return `422` with `{"errors": {"<field>": ["<message>"]}}`; missing/invalid auth returns `401`, writing someone else's resource returns `403`, unknown resources return `404`.

### GraphQL

- Endpoint: `POST http://localhost:8080/graphql` (same `Authorization` header).
- GraphiQL: `http://localhost:8080/graphiql` (served by DGS). The page loads its UI scripts from a CDN; if it renders blank, those scripts failed to load, so use `curl`/any GraphQL client against `/graphql` instead.
- Schema: [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls). Java types and `DgsConstants` are generated into `build/generated` by the DGS codegen `generateJava` task (package `io.spring.graphql`) on every build.

![](graphql-schema.png)

| Queries | Mutations |
| --- | --- |
| `article(slug)`, `articles(first, after, last, before, authoredBy, favoritedBy, withTag)`, `feed(first, after, last, before)`, `me`, `profile(username)`, `tags` | `createUser`, `login`, `updateUser`, `followUser`, `unfollowUser`, `createArticle`, `updateArticle`, `favoriteArticle`, `unfavoriteArticle`, `deleteArticle`, `addComment`, `deleteComment` |

List fields (`articles`, `feed`, `Profile.articles/favorites/feed`, `Article.comments`) use Relay-style cursor connections; pass either `first`/`after` or `last`/`before`. Cursors are epoch-millisecond timestamps, and omitting both `first` and `last` is an error. `me` must be sent with an `Authorization` header. `createUser` returns the `UserResult` union (`UserPayload` or `Error` with per-field messages).

Example:

```bash
curl -s http://localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"{ articles(first: 5) { edges { cursor node { slug title author { username } } } pageInfo { hasNextPage endCursor } } }"}'
```

## How it works

The code follows Domain-Driven Design ideas: REST and GraphQL are just two adapters over the same application and domain layers. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for details.

- **DDD layering** separates business terms (`core`) from technical details (`infrastructure`).
- **[Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html)** with MyBatis: SQL lives in XML mappers under `src/main/resources/mapper`, domain objects stay persistence-agnostic.
- **[CQRS](https://martinfowler.com/bliki/CQRS.html)-style** separation: writes go through repositories and command services on domain entities; reads go through dedicated read services that return DTOs (`ArticleData`, `ProfileData`, ...).
- **Security**: Spring Security with a stateless JWT filter (`JwtTokenFilter`), BCrypt password hashing, and ownership checks in `AuthorizationService`.

## Project structure

```
.
├── build.gradle                     # Plugins, dependencies, Spotless, DGS codegen config
├── frontend/                        # React + TypeScript + Vite client (see frontend/README.md)
├── docs/                            # API and architecture reference
├── src/main/java/io/spring
│   ├── RealWorldApplication.java    # Spring Boot entry point
│   ├── JacksonCustomizations.java   # ISO-8601 UTC serialization of Joda DateTime
│   ├── MyBatisConfig.java           # Enables transaction management
│   ├── api/                         # REST controllers (Spring MVC)
│   │   ├── exception/               # Error payloads and @RestControllerAdvice
│   │   └── security/                # WebSecurityConfig, JwtTokenFilter
│   ├── graphql/                     # DGS data fetchers and mutations
│   │   └── exception/               # GraphQL error mapping
│   ├── application/                 # Use-case services
│   │   ├── *QueryService.java       # Read side (articles, comments, profiles, tags, users)
│   │   ├── article/, user/          # Command services, params, custom validators
│   │   ├── data/                    # Read-model DTOs
│   │   └── Page, CursorPager, ...   # Offset and cursor pagination helpers
│   ├── core/                        # Domain model
│   │   ├── article/, comment/, favorite/, user/   # Entities + repository interfaces
│   │   └── service/                 # AuthorizationService, JwtService interface
│   └── infrastructure/
│       ├── mybatis/mapper/          # Write-side MyBatis mappers
│       ├── mybatis/readservice/     # Read-side MyBatis query interfaces
│       ├── repository/              # MyBatis implementations of core repositories
│       └── service/                 # DefaultJwtService (JJWT)
├── src/main/resources
│   ├── application.properties
│   ├── db/migration/                # Flyway SQL
│   ├── mapper/                      # MyBatis XML mappers
│   └── schema/schema.graphqls       # GraphQL schema
└── src/test/java/io/spring          # API, application, domain and repository tests
```

## Testing and code format

```bash
./gradlew test                 # JUnit 5 test suite (in-memory SQLite)
./gradlew spotlessJavaCheck    # verify formatting (JDK 11)
./gradlew spotlessJavaApply    # apply google-java-format (JDK 11)
./gradlew build                # compile, Spotless check, tests, jar
```

Tests cover the REST controllers (`src/test/java/io/spring/api`, MockMvc + REST Assured with mocked services), the query services, the `Article` entity and the MyBatis repositories. There are no automated tests for the GraphQL layer.

CI: [`.github/workflows/gradle.yml`](.github/workflows/gradle.yml) runs `./gradlew clean test` on JDK 11 for every push and pull request.

## Frontend

[`frontend/`](frontend/) contains a React 18 + TypeScript + Vite + Tailwind client that talks to the REST API. Start the backend first, then:

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

See [`frontend/README.md`](frontend/README.md) for details and known gaps.

## Further documentation

- [`docs/API.md`](docs/API.md) — REST and GraphQL endpoint reference
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — layers, request flow, persistence, pagination and security
- [RealWorld spec](https://realworld-docs.netlify.app/)

## Help

Please fork and PR to improve the project.
