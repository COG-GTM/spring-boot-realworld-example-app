# ![RealWorld Example App using Spring Boot and MyBatis](example-logo.png)

> ### Spring Boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

This is a backend for "Conduit", the RealWorld social blogging app (users, profiles, follows, articles, tags, favorites and comments). It serves the same domain over two APIs:

- a **REST API** that follows the RealWorld spec, and
- a **GraphQL API** built with [Netflix DGS](https://github.com/Netflix/dgs-framework).

For other frontends and backends that implement the same spec, see the [RealWorld](https://github.com/gothinkster/realworld) repo.

More detail lives in [`docs/`](docs):

- [`docs/API.md`](docs/API.md): endpoint and GraphQL reference, auth and error formats, pagination
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): layers, request flow, persistence, tests and known issues

## Tech stack

Versions come from [`build.gradle`](build.gradle) and [`gradle/wrapper/gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties).

| Area | Library / tool | Version |
| --- | --- | --- |
| Language | Java (source/target compatibility) | 11 |
| Build | Gradle wrapper | 7.4 |
| Framework | Spring Boot (web, validation, security, HATEOAS) | 2.6.3 |
| GraphQL | Netflix DGS starter (`graphql-dgs-spring-boot-starter`) | 4.9.21 |
| GraphQL codegen | `com.netflix.dgs.codegen` Gradle plugin | 5.0.6 |
| Persistence | MyBatis Spring Boot starter | 2.2.2 |
| Database | SQLite (`org.xerial:sqlite-jdbc`) | 3.36.0.3 |
| Migrations | Flyway (`flyway-core`, version managed by Spring Boot) | |
| Auth | JJWT (`jjwt-api` / `jjwt-impl` / `jjwt-jackson`), HS512 | 0.11.2 |
| Dates | Joda-Time | 2.10.13 |
| Boilerplate | Lombok (version managed by Spring Boot) | |
| Formatting | Spotless Gradle plugin with google-java-format | 6.2.1 |
| Tests | JUnit 5, Spring Boot Test, Spring Security Test, REST Assured (MockMvc), MyBatis test starter | REST Assured 4.5.1 |

## Getting started

You need a **JDK 11**. Other JDKs may compile and run the app, but the Spotless check bundled with `./gradlew build` fails on JDK 16+ (google-java-format 1.x needs internal `javac` APIs that newer JDKs no longer expose), so stick to 11 for the full build.

Run the app:

```bash
./gradlew bootRun
```

It listens on http://localhost:8080 and creates/migrates a SQLite database file `dev.db` in the working directory. Check that it works:

```bash
curl http://localhost:8080/tags
# {"tags":[]}
```

Or build and run the jar (use the non-`-plain` jar, which is the executable one):

```bash
./gradlew bootJar
java -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar
```

`./gradlew clean` also deletes `./dev.db`.

### Docker

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The API is then on http://localhost:8081. `bootBuildImage` uses the Cloud Native Buildpacks support in Spring Boot 2.6.3, which talks to the Docker daemon with an old API version; recent Docker Engine releases reject it. If that happens, build the jar with `./gradlew bootJar` and run it with `java -jar` (or wrap it in your own Dockerfile).

### Using it with a RealWorld frontend

The API root is http://localhost:8080, **not** http://localhost:8080/api as some frontend docs suggest. CORS allows any origin (`*`) for `GET, POST, PUT, DELETE, PATCH, HEAD` and the `Authorization`, `Cache-Control` and `Content-Type` headers (see `WebSecurityConfig`).

### Bundled React frontend

[`frontend/`](frontend) contains a React 18 + TypeScript + Vite + Tailwind CSS client for the REST API. With the backend running on :8080:

```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

It calls `VITE_API_BASE_URL` (set in `frontend/.env`, default `http://localhost:8080`). Vite 5 needs Node.js 18 or newer. `npm run build` currently fails at the `tsc` step (unused `React` imports, TS6133), and `npm run lint` fails because there is no ESLint config file. See [`frontend/README.md`](frontend/README.md).

## Configuration

Settings live in [`src/main/resources/application.properties`](src/main/resources/application.properties):

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:sqlite:dev.db` | Database. The `test` profile (`application-test.properties`) uses `jdbc:sqlite::memory:` |
| `jwt.secret` | a fixed value committed to the repo | HS512 signing key for JWTs. **Override it outside local development**, e.g. `JWT_SECRET=... ./gradlew bootRun` via Spring's relaxed binding |
| `jwt.sessionTime` | `86400` | Token lifetime in seconds (24 h) |
| `image.default` | `https://static.productionready.io/images/smiley-cyrus.jpg` | Avatar assigned to newly registered users |
| `spring.jackson.deserialization.UNWRAP_ROOT_VALUE` | `true` | Request bodies are wrapped in a root key (`{"user": {...}}`, `{"article": {...}}`, ...) |
| `mybatis.*` | | Mapper XML location (`mapper/*.xml`), underscore-to-camelCase mapping, type handlers package, 3000 ms statement timeout |

The schema is created by Flyway from `src/main/resources/db/migration/V1__create_tables.sql`. To use another database, change the datasource settings and add its JDBC driver; the mapper SQL uses `limit offset, count` syntax, so check it against your database.

## API overview

All endpoints are relative to http://localhost:8080. Send the JWT as `Authorization: Token <jwt>`. See [`docs/API.md`](docs/API.md) for bodies, auth rules, errors and examples.

### REST

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/users` | no | Register (returns `201` and the user with a token) |
| POST | `/users/login` | no | Log in |
| GET | `/user` | yes | Current user |
| PUT | `/user` | yes | Update current user |
| GET | `/profiles/{username}` | optional | Get a profile |
| POST | `/profiles/{username}/follow` | yes | Follow a user |
| DELETE | `/profiles/{username}/follow` | yes | Unfollow a user |
| GET | `/articles` | optional | List articles; filters `tag`, `author`, `favorited`; paging `offset` (default 0), `limit` (default 20, max 100) |
| GET | `/articles/feed` | yes | Articles by followed users; `offset`, `limit` |
| POST | `/articles` | yes | Create an article |
| GET | `/articles/{slug}` | optional | Get an article |
| PUT | `/articles/{slug}` | yes (author) | Update an article |
| DELETE | `/articles/{slug}` | yes (author) | Delete an article (`204`) |
| POST | `/articles/{slug}/favorite` | yes | Favorite an article |
| DELETE | `/articles/{slug}/favorite` | yes | Unfavorite an article |
| GET | `/articles/{slug}/comments` | optional | List comments |
| POST | `/articles/{slug}/comments` | yes | Add a comment (`201`) |
| DELETE | `/articles/{slug}/comments/{id}` | yes (comment or article author) | Delete a comment (`204`) |
| GET | `/tags` | no | All tags |

### GraphQL

- Endpoint: `POST /graphql`. GraphiQL UI: http://localhost:8080/graphiql
- Schema: [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls). The `generateJava` task (DGS codegen) turns it into Java types under `io.spring.graphql` in `build/generated`, and runs automatically before `compileJava`.
- Queries: `article`, `articles`, `feed`, `me`, `profile`, `tags`
- Mutations: `createUser`, `login`, `updateUser`, `followUser`, `unfollowUser`, `createArticle`, `updateArticle`, `favoriteArticle`, `unfavoriteArticle`, `deleteArticle`, `addComment`, `deleteComment`
- Lists are Relay-style connections (`edges { cursor node }`, `pageInfo`) with cursor pagination; pass `first` (with optional `after`) or `last` (with optional `before`).

```bash
curl -s http://localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"{ articles(first: 5) { edges { node { slug title author { username } } } pageInfo { hasNextPage endCursor } } }"}'
```

A visualization of the schema (it may lag behind `schema.graphqls`, which is the source of truth):

![GraphQL schema](graphql-schema.png)

## How it works

REST and GraphQL are two adapters over the same application and domain code, following some DDD ideas:

- Domain-Driven Design keeps business terms (`core`) apart from technical details (`infrastructure`).
- MyBatis implements the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern; SQL lives in XML mappers.
- A light [CQRS](https://martinfowler.com/bliki/CQRS.html) split: writes go through domain repositories, reads go through query services that select straight into DTOs.
- Spring Security runs statelessly; `JwtTokenFilter` turns the `Authorization` header into the authenticated `User`.

## Project structure

```text
.github/workflows/gradle.yml      # "Java CI": ./gradlew clean test on JDK 11
docs/                             # API.md, ARCHITECTURE.md
frontend/                         # React + Vite client (see frontend/README.md)
gradle/, gradlew, build.gradle    # Gradle 7.4 wrapper and build

src/main/java/io/spring
├── RealWorldApplication.java     # Spring Boot entry point
├── JacksonCustomizations.java    # Joda DateTime -> ISO-8601 UTC JSON serializer
├── MyBatisConfig.java            # enables transaction management
├── Util.java
├── api/                          # REST adapter (Spring MVC)
│   ├── *Api.java                 # Users, CurrentUser, Profile, Articles, Article, ArticleFavorite, Comments, Tags
│   ├── exception/                # exceptions and @RestControllerAdvice error mapping
│   └── security/                 # WebSecurityConfig, JwtTokenFilter
├── graphql/                      # GraphQL adapter (DGS data fetchers and mutations, SecurityUtil)
│   └── exception/                # GraphQLCustomizeExceptionHandler, AuthenticationException
├── application/                  # query services, pagination types, command services
│   ├── article/                  # ArticleCommandService, NewArticleParam, UpdateArticleParam, duplicate-title validator
│   ├── user/                     # UserService, RegisterParam, UpdateUserParam/Command, duplicate email/username validators
│   └── data/                     # read-model DTOs (ArticleData, CommentData, ProfileData, UserData, ...)
├── core/                         # domain entities, repository interfaces, AuthorizationService, JwtService
│   ├── article/  comment/  favorite/  user/  service/
└── infrastructure/
    ├── mybatis/                  # DateTimeHandler, mapper/ (write mappers), readservice/ (read mappers)
    ├── repository/               # MyBatis*Repository implementations of core repositories
    └── service/                  # DefaultJwtService (JJWT)

src/main/resources
├── application.properties, application-test.properties
├── db/migration/V1__create_tables.sql   # Flyway schema
├── mapper/*.xml                         # MyBatis SQL
└── schema/schema.graphqls               # GraphQL schema

src/test/java/io/spring                  # API, application, core and infrastructure tests
```

## Tests and formatting

```bash
./gradlew test                 # 68 tests: MockMvc API tests, query-service and repository tests on SQLite
./gradlew spotlessJavaApply    # format all Java sources with google-java-format
./gradlew spotlessJavaCheck    # verify formatting (part of ./gradlew check / build)
./gradlew build                # compile, test, Spotless check, jars
```

Run the full build on JDK 11 (see [Getting started](#getting-started)).

CI: [`.github/workflows/gradle.yml`](.github/workflows/gradle.yml) runs `./gradlew clean test` on JDK 11 (Zulu) for every push and pull request. It does not run the Spotless check or build the frontend. It also pins `actions/checkout@v2`, `actions/setup-java@v2` and `actions/cache@v2`, and GitHub has retired `actions/cache@v2`, so the job fails during setup until those are updated.

## Help

Please fork and open a PR to improve the project.
