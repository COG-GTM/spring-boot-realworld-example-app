# ![RealWorld Example App using Java and Spring Boot](example-logo.png)

> ### Spring Boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

This repository is a backend for the RealWorld "Conduit" social blogging app (users, profiles, follows, articles, comments, favorites, tags). It exposes the same domain over **two adapters**: a REST API that follows the RealWorld spec, and a GraphQL API built with Netflix [DGS](https://github.com/Netflix/dgs-framework). A React + TypeScript client lives in [`frontend/`](frontend/README.md).

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [API overview](#api-overview)
- [Project structure](#project-structure)
- [Configuration](#configuration)
- [Testing and code format](#testing-and-code-format)
- [Further docs](#further-docs)

## Tech stack

Versions are taken from [`build.gradle`](build.gradle) and the Gradle wrapper.

| Area | Technology |
| --- | --- |
| Language | Java 11 (`sourceCompatibility = '11'`) |
| Build | Gradle 7.4 (wrapper), Spring Boot Gradle plugin 2.6.3, `io.spring.dependency-management` 1.0.11.RELEASE |
| Web / security | Spring Boot 2.6.3 starters: web, validation, hateoas, security |
| GraphQL | Netflix DGS `graphql-dgs-spring-boot-starter` 4.9.21, DGS codegen plugin 5.0.6 |
| Persistence | MyBatis Spring Boot starter 2.2.2, SQLite (`sqlite-jdbc` 3.36.0.3), Flyway (version managed by Spring Boot) |
| Auth | JWT via `jjwt` 0.11.2 (HS512), BCrypt password hashing |
| Other | Joda-Time 2.10.13, Lombok |
| Tests | JUnit 5, Spring Boot Test, Spring Security Test, REST Assured 4.5.1 (`spring-mock-mvc`), MyBatis test starter 2.2.2 |
| Formatting | Spotless 6.2.1 with Google Java Format |
| Frontend | React 18, TypeScript, Vite 5, Tailwind CSS, Axios (see [`frontend/README.md`](frontend/README.md)) |

## Getting started

### Prerequisites

- JDK 11 (the build targets Java 11; CI uses Zulu 11). Newer JDKs are not tested with these plugin versions.
- No database server is needed: the app uses a local SQLite file.

### Run the backend

```bash
./gradlew bootRun
```

The API is served at http://localhost:8080. On first start Flyway creates the schema in `dev.db` in the current working directory (`./gradlew clean` deletes it).

Check that it works:

```bash
curl http://localhost:8080/tags
# {"tags":[]}
```

### Build an executable jar

```bash
./gradlew bootJar
java -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar
```

### Run with Docker

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The API is then available on http://localhost:8081. `bootBuildImage` (Spring Boot 2.6.3) talks to the Docker daemon using Docker Engine API v1.24; recent Docker Engine releases have dropped support for API versions that old, in which case use the `bootJar` route above instead.

### Use it with a RealWorld frontend

The API root is http://localhost:8080, **not** http://localhost:8080/api as some RealWorld frontend docs suggest. CORS is open to all origins.

To run the bundled React client:

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000, talks to http://localhost:8080 by default
```

## API overview

Both adapters call the same application services and domain model. A full reference with request/response shapes is in [docs/API.md](docs/API.md).

### Authentication

`POST /users` and `POST /users/login` (or the GraphQL `createUser` / `login` mutations) return a JWT. Send it on later requests as:

```
Authorization: Token <jwt>
```

The token subject is the user id; tokens expire after `jwt.sessionTime` seconds (default 86400 = 24h). Sessions are stateless.

### REST endpoints

Request and response bodies are wrapped in a root key (`{"user": {...}}`, `{"article": {...}}`, ...), as in the RealWorld spec.

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/users` | – | Register (returns 201) |
| POST | `/users/login` | – | Log in |
| GET | `/user` | required | Current user |
| PUT | `/user` | required | Update current user |
| GET | `/profiles/{username}` | optional | Get profile |
| POST | `/profiles/{username}/follow` | required | Follow user |
| DELETE | `/profiles/{username}/follow` | required | Unfollow user |
| GET | `/articles` | optional | List articles; query params `tag`, `author`, `favorited`, `offset` (default 0), `limit` (default 20, max 100) |
| GET | `/articles/feed` | required | Articles from followed users; `offset`, `limit` |
| POST | `/articles` | required | Create article |
| GET | `/articles/{slug}` | optional | Get article |
| PUT | `/articles/{slug}` | required (author) | Update article |
| DELETE | `/articles/{slug}` | required (author) | Delete article (204) |
| POST | `/articles/{slug}/favorite` | required | Favorite article |
| DELETE | `/articles/{slug}/favorite` | required | Unfavorite article |
| GET | `/articles/{slug}/comments` | optional | List comments |
| POST | `/articles/{slug}/comments` | required | Add comment (201) |
| DELETE | `/articles/{slug}/comments/{id}` | required (comment or article author) | Delete comment (204) |
| GET | `/tags` | – | List tags |

Validation errors return `422` with `{"errors": {"<field>": ["<message>"]}}`; bad login credentials return `422` with `{"message": "invalid email or password"}`; missing/invalid auth on a protected route returns `401`.

### GraphQL

- Endpoint: `POST /graphql` (open in Spring Security; resolvers enforce auth themselves).
- Schema: [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls). Java types and constants are generated into `build/generated` by the DGS codegen `generateJava` task (package `io.spring.graphql`).
- GraphiQL UI: `/graphiql` (DGS default).

Queries: `article`, `articles`, `me`, `feed`, `profile`, `tags`. Mutations: `createUser`, `login`, `updateUser`, `followUser`, `unfollowUser`, `createArticle`, `updateArticle`, `favoriteArticle`, `unfavoriteArticle`, `deleteArticle`, `addComment`, `deleteComment`. List fields (`articles`, `feed`, `Profile.articles|favorites|feed`, `Article.comments`) are Relay-style connections paged with `first`/`after` or `last`/`before`; one of `first` or `last` is required. Cursors are epoch-millisecond timestamps.

```bash
curl -s http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ articles(first: 5) { edges { cursor node { slug title author { username } } } pageInfo { hasNextPage endCursor } } }"}'
```

![GraphQL schema visualization](graphql-schema.png)

## Project structure

The code follows DDD-style layering with a CQRS-like split between write-side repositories and read-side query services. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for details.

```
src/main/java/io/spring/
├── RealWorldApplication.java     # Spring Boot entry point
├── JacksonCustomizations.java    # Joda DateTime -> ISO-8601 UTC JSON
├── MyBatisConfig.java            # enables transaction management
├── api/                          # REST adapter (Spring MVC controllers)
│   ├── exception/                # error resources + @RestControllerAdvice
│   └── security/                 # WebSecurityConfig, JwtTokenFilter, CORS
├── graphql/                      # GraphQL adapter (DGS data fetchers & mutations)
│   └── exception/                # GraphQL exception handler
├── application/                  # use cases: query services, command services, DTOs, pagination
│   ├── article/                  # ArticleCommandService, params, duplicate-title validator
│   ├── user/                     # UserService, params, duplicate email/username validators
│   └── data/                     # read-model DTOs (ArticleData, ProfileData, UserData, ...)
├── core/                         # domain model and repository/service interfaces
│   ├── article/  comment/  favorite/  user/
│   └── service/                  # AuthorizationService, JwtService
└── infrastructure/               # technical implementations
    ├── mybatis/mapper/           # write-side MyBatis mappers
    ├── mybatis/readservice/      # read-side MyBatis query interfaces
    ├── repository/               # MyBatis*Repository implementations of core repositories
    └── service/DefaultJwtService.java

src/main/resources/
├── application.properties        # datasource, JWT, MyBatis settings
├── db/migration/V1__create_tables.sql  # Flyway schema
├── mapper/*.xml                  # MyBatis SQL
└── schema/schema.graphqls        # GraphQL schema

src/test/java/io/spring/          # API (MockMvc/REST Assured), query service, repository and JWT tests
frontend/                         # React + TypeScript client (Vite)
```

## Configuration

All settings are in [`src/main/resources/application.properties`](src/main/resources/application.properties) and can be overridden the usual Spring Boot way (environment variables, `--key=value` arguments, profile files).

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:sqlite:dev.db` | Database location (relative to the working directory) |
| `jwt.secret` | a checked-in development key | HS512 signing key. **Override it in any shared or deployed environment**, e.g. `JWT_SECRET=... ./gradlew bootRun` |
| `jwt.sessionTime` | `86400` | Token lifetime in seconds |
| `image.default` | `https://static.productionready.io/images/smiley-cyrus.jpg` | Avatar assigned to newly registered users |
| `spring.jackson.deserialization.UNWRAP_ROOT_VALUE` | `true` | Unwraps `{"user": {...}}`-style request bodies |

The schema is written for SQLite; switching to another database means changing the datasource properties, adding its JDBC driver, and checking the SQL in `db/migration` and `mapper/*.xml`.

## Testing and code format

```bash
./gradlew test                 # 68 tests: API, application query services, MyBatis repositories, JWT
./gradlew spotlessJavaCheck    # verify formatting (part of ./gradlew build)
./gradlew spotlessJavaApply    # apply Google Java Format
./gradlew clean build          # compile, format check, tests, jars
```

CI ([`.github/workflows/gradle.yml`](.github/workflows/gradle.yml)) is configured to run `./gradlew clean test` on JDK 11 for every push and pull request. Note: the workflow still pins `actions/cache@v2`, which GitHub has deprecated, so the job currently fails before reaching the Gradle step until the action is bumped (e.g. to `actions/cache@v4`).

## Further docs

- [docs/API.md](docs/API.md) – REST and GraphQL reference with example payloads
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) – layers, request flow, persistence, auth and pagination
- [frontend/README.md](frontend/README.md) – React client

## License

See [LICENSE](LICENSE).
