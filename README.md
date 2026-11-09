# ![RealWorld Example App using Java and Spring](example-logo.png)

> ### Spring Boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc.) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

This codebase demonstrates a fully fledged backend for "Conduit" (a Medium-style social blogging site) built with Spring Boot and MyBatis, including CRUD operations, JWT authentication, routing, pagination, and more. The same domain is exposed through **both a REST API and a GraphQL API**. A React single-page app that consumes the REST API lives in [`frontend/`](frontend/README.md).

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [API overview](#api-overview)
- [How it works](#how-it-works)
- [Project structure](#project-structure)
- [Configuration](#configuration)
- [Testing and code format](#testing-and-code-format)
- Further docs: [`docs/API.md`](docs/API.md) (endpoint reference) and [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) (architecture notes)

## Tech stack

Versions are taken from [`build.gradle`](build.gradle) and [`gradle/wrapper/gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties).

| Area | Technology |
| --- | --- |
| Language | Java 11 (`sourceCompatibility`/`targetCompatibility = '11'`) |
| Build | Gradle 7.4 (wrapper) |
| Framework | Spring Boot 2.6.3 (Web, Validation, HATEOAS, Security) |
| GraphQL | [Netflix DGS](https://github.com/Netflix/dgs-framework) 4.9.21, DGS codegen Gradle plugin 5.0.6 |
| Persistence | MyBatis Spring Boot Starter 2.2.2, SQLite (`sqlite-jdbc` 3.36.0.3), Flyway (version managed by Spring Boot) |
| Auth | Spring Security + JJWT 0.11.2 (HS512-signed tokens), BCrypt password hashing |
| Dates | Joda-Time 2.10.13 |
| Boilerplate | Lombok |
| Formatting | Spotless 6.2.1 with google-java-format |
| Tests | JUnit 5, Spring Boot Test, REST Assured 4.5.1 (spring-mock-mvc), MyBatis test starter |
| Frontend (optional) | React 18, TypeScript, Vite 5, Tailwind CSS — see [`frontend/README.md`](frontend/README.md) |

## Getting started

### Prerequisites

- **JDK 11.** The build targets Java 11, and the Spotless/google-java-format version pinned in this build only works on JDK 11 (on newer JDKs `./gradlew test` works but `spotlessJavaCheck` — and therefore `./gradlew build` — fails). If several JDKs are installed, point `JAVA_HOME` at JDK 11.
- No database server is needed: the app uses a local SQLite file.

### Run the backend

    ./gradlew bootRun

The API listens on http://localhost:8080. On first start Flyway creates the schema in `dev.db`, a SQLite file in the working directory (the project root for `bootRun`). It is ignored by git, and `./gradlew clean` deletes it.

Check that it works:

    curl http://localhost:8080/tags
    # {"tags":[]}

Or build an executable jar and run it:

    ./gradlew bootJar
    java -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar

### Run with Docker

Spring Boot's buildpack support can build an OCI image (requires Docker):

    ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
    docker run -p 8081:8080 spring-boot-realworld-example-app

The API is then reachable on http://localhost:8081. Note: the buildpack client in Spring Boot 2.6.3 uses an old Docker Engine API version, which recent Docker Engine releases reject; if `bootBuildImage` fails, use the `bootJar` + `java -jar` route above.

### Run the bundled React frontend

    cd frontend
    npm install
    npm run dev

The frontend runs on http://localhost:3000 and calls the backend at `http://localhost:8080` (override with `VITE_API_BASE_URL`). See [`frontend/README.md`](frontend/README.md).

### Use another RealWorld frontend

The API is served from the root, i.e. http://localhost:8080, **not** http://localhost:8080/api as some RealWorld frontend documentation suggests.

## API overview

Authenticated requests send the JWT returned by register/login in the header

    Authorization: Token <jwt>

(the filter only checks that the header has two space-separated parts, so `Bearer <jwt>` also works). Request and response bodies use the RealWorld envelope style (`{"user": {...}}`, `{"article": {...}}`, ...). The full request/response reference, including error formats and status codes, is in [`docs/API.md`](docs/API.md).

### REST

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/users` | – | Register (returns `201`) |
| POST | `/users/login` | – | Login |
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
| DELETE | `/articles/{slug}` | required (author) | Delete article (`204`) |
| POST | `/articles/{slug}/favorite` | required | Favorite article |
| DELETE | `/articles/{slug}/favorite` | required | Unfavorite article |
| GET | `/articles/{slug}/comments` | optional | List comments |
| POST | `/articles/{slug}/comments` | required | Add comment (`201`) |
| DELETE | `/articles/{slug}/comments/{id}` | required (comment or article author) | Delete comment (`204`) |
| GET | `/tags` | – | List all tags |

"Optional" auth means the endpoint is public, but a valid token fills in per-user fields such as `favorited` and `following`. `limit` is capped at 100.

### GraphQL

DGS serves GraphQL at `POST /graphql` and the GraphiQL IDE at http://localhost:8080/graphiql. Both paths are open in the Spring Security config; resolvers that need a user check the token themselves. The schema is [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls), and the DGS codegen plugin generates the Java types into `build/generated` (package `io.spring.graphql`) on every build.

- **Queries:** `article(slug)`, `articles(first, after, last, before, authoredBy, favoritedBy, withTag)`, `feed(first, after, last, before)`, `me`, `profile(username)`, `tags`.
- **Mutations:** `createUser`, `login`, `updateUser`, `followUser`, `unfollowUser`, `createArticle`, `updateArticle`, `favoriteArticle`, `unfavoriteArticle`, `deleteArticle`, `addComment`, `deleteComment`.
- List fields are Relay-style connections (`edges { cursor node }`, `pageInfo`) paged by a creation-time cursor. Pass `first` (with optional `after`) or `last` (with optional `before`).
- `createUser` returns the `UserResult` union (`UserPayload | Error`), so validation errors come back as data rather than as GraphQL errors.

Example:

```graphql
mutation {
  login(email: "jake@jake.jake", password: "jakejake") {
    user { username token }
  }
}

query {
  articles(first: 10, withTag: "dragons") {
    edges { cursor node { slug title favoritesCount author { username } } }
    pageInfo { hasNextPage endCursor }
  }
}
```

A visualization of the schema (it may lag behind the `.graphqls` file, which is the source of truth):

![GraphQL schema](graphql-schema.png)

## How it works

The application follows Domain-Driven Design ideas: REST and GraphQL are just two adapters over the same application and domain layers.

- **Domain layer** (`core`) holds entities (`Article`, `Comment`, `User`, `FollowRelation`, `ArticleFavorite`), repository interfaces, and the `AuthorizationService`/`JwtService` contracts.
- **[CQRS](https://martinfowler.com/bliki/CQRS.html)-style split:** writes go through domain repositories and command services (`ArticleCommandService`, `UserService`); reads go through query services (`ArticleQueryService`, `ProfileQueryService`, ...) that return flat DTOs (`ArticleData`, `ProfileData`, ...) from dedicated MyBatis read mappers.
- **Persistence** uses MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern; SQL lives in XML files under `src/main/resources/mapper`.
- **Security:** `JwtTokenFilter` reads the `Authorization` header, validates the token with `DefaultJwtService`, loads the user and puts it in the Spring Security context. Sessions are stateless and CSRF is disabled.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the request flow, the database schema, and more detail.

## Project structure

```
.
├── build.gradle                      # Gradle build: dependencies, Spotless, DGS codegen
├── frontend/                         # React + Vite SPA that consumes the REST API
├── docs/                             # API reference and architecture notes
└── src
    ├── main
    │   ├── java/io/spring
    │   │   ├── RealWorldApplication.java   # Spring Boot entry point
    │   │   ├── JacksonCustomizations.java  # Joda DateTime -> ISO-8601 (UTC) JSON
    │   │   ├── MyBatisConfig.java          # enables transaction management
    │   │   ├── api/                        # REST controllers
    │   │   │   ├── exception/              # error resources + @RestControllerAdvice
    │   │   │   └── security/               # WebSecurityConfig, JwtTokenFilter
    │   │   ├── graphql/                    # DGS data fetchers and mutations
    │   │   │   └── exception/              # GraphQL exception handler
    │   │   ├── application/                # query services, pagination, DTOs
    │   │   │   ├── article/                # ArticleCommandService, params, validators
    │   │   │   ├── user/                   # UserService, params, validators
    │   │   │   └── data/                   # read-model DTOs (ArticleData, ProfileData, ...)
    │   │   ├── core/                       # domain entities, repository interfaces, services
    │   │   └── infrastructure
    │   │       ├── mybatis/mapper/         # write-side MyBatis mappers
    │   │       ├── mybatis/readservice/    # read-side MyBatis mappers
    │   │       ├── repository/             # MyBatis implementations of core repositories
    │   │       └── service/                # DefaultJwtService (JJWT)
    │   └── resources
    │       ├── application.properties      # main config
    │       ├── application-test.properties # in-memory SQLite for the "test" profile
    │       ├── db/migration/               # Flyway migrations
    │       ├── mapper/                     # MyBatis XML mappers
    │       └── schema/schema.graphqls      # GraphQL schema
    └── test/java/io/spring                 # API, query-service, repository and JWT tests
```

## Configuration

All settings live in [`src/main/resources/application.properties`](src/main/resources/application.properties) and can be overridden in the usual Spring Boot ways (environment variables, `--key=value` arguments, profiles):

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:sqlite:dev.db` | Database. The `test` profile uses `jdbc:sqlite::memory:`. |
| `jwt.secret` | committed sample value | HS512 signing key. **Override it for any non-local deployment.** |
| `jwt.sessionTime` | `86400` | Token lifetime in seconds (24h). |
| `image.default` | `https://static.productionready.io/images/smiley-cyrus.jpg` | Avatar assigned to new users. |
| `mybatis.*` | see file | Mapper locations, type handlers, camelCase mapping. |

Switching to another database means changing the datasource properties, adding the JDBC driver to `build.gradle`, and checking that the Flyway migration and MyBatis SQL (which uses `limit offset, count` syntax) are compatible.

## Testing and code format

Run the test suite (API tests with MockMvc/REST Assured, query-service tests, MyBatis repository tests, and JWT tests):

    ./gradlew test

Format code with Spotless (google-java-format) before committing; `./gradlew build` runs `spotlessJavaCheck`:

    ./gradlew spotlessJavaApply

CI ([`.github/workflows/gradle.yml`](.github/workflows/gradle.yml)) runs `./gradlew clean test` on JDK 11 for every push and pull request.

## Help

Please fork and open a PR to improve the project.
