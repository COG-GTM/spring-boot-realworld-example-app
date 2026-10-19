# ![RealWorld Example App using Java and Spring Boot](example-logo.png)

[![Java CI](https://github.com/ankehao-demo/spring-boot-realworld-example-app/actions/workflows/gradle.yml/badge.svg)](https://github.com/ankehao-demo/spring-boot-realworld-example-app/actions/workflows/gradle.yml)

> ### Spring Boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc.) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

This repository is a backend for "Conduit", a Medium-style blogging platform, built with Spring Boot and MyBatis. It covers CRUD operations, JWT authentication, pagination, following users and favoriting articles. The same domain is exposed through **two adapters**: a REST API that follows the RealWorld spec, and a GraphQL API built with [Netflix DGS](https://github.com/Netflix/dgs-framework).

The repository also contains a React + TypeScript client in [`frontend/`](frontend/README.md) that talks to the REST API.

For details see:

- [docs/API.md](docs/API.md): REST endpoint and GraphQL operation reference
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): layers, request flow, persistence and security
- [frontend/README.md](frontend/README.md): the React client

## Tech stack

| Area | Library / tool | Version (from `build.gradle`) |
| --- | --- | --- |
| Language | Java | 11 (`sourceCompatibility = '11'`) |
| Build | Gradle wrapper | 7.4 |
| Framework | Spring Boot (Web, Validation, HATEOAS, Security) | 2.6.3 |
| GraphQL | Netflix DGS starter / DGS codegen plugin | 4.9.21 / 5.0.6 |
| Persistence | MyBatis Spring Boot starter | 2.2.2 |
| Database | SQLite (`org.xerial:sqlite-jdbc`) | 3.36.0.3 |
| Migrations | Flyway (version managed by Spring Boot) | - |
| Auth | JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`), HS512 | 0.11.2 |
| Dates | Joda-Time | 2.10.13 |
| Boilerplate | Lombok | managed by Spring Boot |
| Tests | JUnit 5, Spring Boot Test, REST Assured (`spring-mock-mvc`), MyBatis test starter | REST Assured 4.5.1 |
| Formatting | Spotless + google-java-format | Spotless 6.2.1 |

## Getting started

### Prerequisites

- JDK 11 (the build targets Java 11, and CI runs on Zulu JDK 11)
- Node.js 18 or newer, only if you want to run the frontend

### Run the backend

```bash
./gradlew bootRun
```

The API listens on http://localhost:8080. On startup Flyway applies `src/main/resources/db/migration/V1__create_tables.sql` to a SQLite file called `dev.db` in the working directory. The file is git-ignored, it keeps its data across restarts, and `./gradlew clean` deletes it.

Check that it works:

```bash
curl http://localhost:8080/tags
# {"tags":[]}
```

The API is served from the root path: use `http://localhost:8080`, **not** `http://localhost:8080/api` as some RealWorld frontend docs suggest.

### Run the frontend (optional)

```bash
cd frontend
npm install
npm run dev     # http://localhost:3000, calls the API at VITE_API_BASE_URL (default http://localhost:8080)
```

See [frontend/README.md](frontend/README.md) for details and known issues.

### Run with Docker

The build has no Dockerfile. Instead, the Spring Boot Gradle plugin can build an image with Cloud Native Buildpacks:

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
curl http://localhost:8081/tags
```

> **Known issue:** with Spring Boot 2.6.3, `bootBuildImage` talks to Docker using API version 1.24 and the `paketobuildpacks/builder:base` builder. Recent Docker Engines reject that API version (Docker 29 needs at least 1.40), and the task fails with `Docker API call to 'localhost/v1.24/images/create?...' failed with status code 400`. It only works on an older Docker Engine until Spring Boot is upgraded. Instead, build the executable jar with `./gradlew bootJar` and run `java -jar build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar` (for example in a JDK 11 base image). Don't use the `-plain.jar`.

If the image does build, the SQLite file is created inside the container, so data is lost when the container is removed.

## Configuration

All settings live in `src/main/resources/application.properties`:

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:sqlite:dev.db` | Database location. Tests use `application-test.properties` (`jdbc:sqlite::memory:`) |
| `spring.jackson.deserialization.UNWRAP_ROOT_VALUE` | `true` | Request bodies are wrapped, e.g. `{"user": {...}}`, `{"article": {...}}` |
| `jwt.secret` | a hard-coded sample key | HS512 signing key for tokens. **Replace it for any real deployment** (e.g. `JWT_SECRET=... ./gradlew bootRun` via Spring's relaxed binding, or `--jwt.secret=...`) |
| `jwt.sessionTime` | `86400` | Token lifetime in seconds (24 h) |
| `image.default` | `https://static.productionready.io/images/smiley-cyrus.jpg` | Avatar assigned to new users. The URL currently returns 404 |
| `mybatis.*` | see file | Mapper XML location (`mapper/*.xml`), type handlers, camel-case mapping |

To use another database, change the `spring.datasource.*` properties and add the JDBC driver. The SQL in the Flyway migration and the mapper XML files is written for SQLite. For example, `limit #{offset}, #{limit}` is MySQL/SQLite syntax, so check the SQL before switching.

## API overview

All endpoints accept and return JSON. Authenticated requests send `Authorization: Token <jwt>`. For request and response shapes and error formats see [docs/API.md](docs/API.md).

### REST

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/users` | - | Register |
| POST | `/users/login` | - | Log in |
| GET | `/user` | required | Current user |
| PUT | `/user` | required | Update current user |
| GET | `/profiles/{username}` | optional | Get profile |
| POST / DELETE | `/profiles/{username}/follow` | required | Follow / unfollow |
| GET | `/articles?tag=&author=&favorited=&offset=&limit=` | optional | List articles, newest first (`limit` defaults to 20, max 100) |
| GET | `/articles/feed?offset=&limit=` | required | Articles by followed users |
| POST | `/articles` | required | Create article |
| GET | `/articles/{slug}` | optional | Get article |
| PUT / DELETE | `/articles/{slug}` | required (author) | Update / delete article |
| POST / DELETE | `/articles/{slug}/favorite` | required | Favorite / unfavorite |
| GET | `/articles/{slug}/comments` | optional | List comments |
| POST | `/articles/{slug}/comments` | required | Add comment |
| DELETE | `/articles/{slug}/comments/{id}` | required (comment or article author) | Delete comment |
| GET | `/tags` | - | All tags |

### GraphQL

- Endpoint: `POST http://localhost:8080/graphql`
- Schema: [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls). At build time the DGS codegen plugin generates the Java types and the `DgsConstants` class into the `io.spring.graphql` package (under `build/generated`).
- Queries: `article`, `articles`, `feed`, `me`, `profile`, `tags`
- Mutations: `createUser`, `login`, `updateUser`, `followUser`, `unfollowUser`, `createArticle`, `updateArticle`, `favoriteArticle`, `unfavoriteArticle`, `deleteArticle`, `addComment`, `deleteComment`
- Lists use Relay-style cursor connections (`first`/`after` or `last`/`before`), and you must pass `first` or `last`.

```bash
curl -s http://localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"{ tags articles(first: 5) { edges { cursor node { slug title } } pageInfo { hasNextPage endCursor } } }"}'
```

The schema visualization below is a static image and may lag behind `schema.graphqls`:

![GraphQL schema](graphql-schema.png)

> The bundled GraphiQL page at `/graphiql` returns 200 but renders blank: it loads unpinned React UMD bundles from unpkg, and those URLs now return 404. Use `curl` or a standalone GraphQL client against `/graphql` instead.

## Project structure

```
.
├── build.gradle                      # Dependencies, Spotless, DGS codegen (generateJava)
├── src/main/java/io/spring
│   ├── RealWorldApplication.java     # Spring Boot entry point
│   ├── JacksonCustomizations.java    # Joda DateTime -> ISO-8601 UTC strings
│   ├── MyBatisConfig.java            # Enables transaction management
│   ├── api/                          # REST adapter: controllers, exception handlers, security (JWT filter, CORS)
│   ├── graphql/                      # GraphQL adapter: DGS data fetchers and mutations, GraphQL error handling
│   ├── application/                  # Use cases: command services + validation, query services, DTOs, pagination
│   ├── core/                         # Domain: Article, Comment, User, FollowRelation, ArticleFavorite, repository interfaces
│   └── infrastructure/               # MyBatis mappers, read services, repository implementations, JWT implementation
├── src/main/resources
│   ├── application.properties
│   ├── db/migration/                 # Flyway SQL migrations
│   ├── mapper/                       # MyBatis XML mappers
│   └── schema/schema.graphqls        # GraphQL schema
├── src/test/java/io/spring           # API (MockMvc), query-service, repository and domain tests
├── frontend/                         # React + TypeScript + Vite client
└── .github/workflows/gradle.yml      # CI: ./gradlew clean test on JDK 11
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for how the layers fit together.

## Tests

```bash
./gradlew test
```

The suite has 68 tests: REST API tests using MockMvc/REST Assured with mocked services, MyBatis repository and query-service tests against in-memory SQLite, and domain tests. The GraphQL adapter has no tests yet.

`./gradlew build` also runs `spotlessCheck`, so the build fails on unformatted code.

## Code format

The project uses [Spotless](https://github.com/diffplug/spotless) with google-java-format:

```bash
./gradlew spotlessJavaApply   # reformat
./gradlew spotlessJavaCheck   # verify
```

## Continuous integration

`.github/workflows/gradle.yml` ("Java CI") runs `./gradlew clean test` on JDK 11 for every push and pull request.

## Contributing

Please fork the repository and open a pull request. Run `./gradlew spotlessJavaApply test` before submitting.

## License

[MIT](LICENSE)
