# ![RealWorld Example App using Spring Boot](example-logo.png)

[![Java CI](https://github.com/ankehao-demo/spring-boot-realworld-example-app/actions/workflows/gradle.yml/badge.svg)](https://github.com/ankehao-demo/spring-boot-realworld-example-app/actions/workflows/gradle.yml)

> Spring Boot + MyBatis implementation of the [RealWorld](https://github.com/gothinkster/realworld) "Conduit" backend (CRUD, JWT auth, pagination, social features), exposed through **both a REST API and a GraphQL API**, plus a React + TypeScript frontend in [`frontend/`](frontend/README.md).

## Contents

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [API overview](#api-overview)
- [Project structure](#project-structure)
- [Testing and code style](#testing-and-code-style)
- [Further documentation](#further-documentation)

## Tech stack

Versions below are taken from [`build.gradle`](build.gradle), [`gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties) and [`frontend/package.json`](frontend/package.json).

### Backend

| Concern | Library / tool | Version |
| --- | --- | --- |
| Language | Java | 11 (source/target compatibility) |
| Build | Gradle wrapper | 7.4 |
| Framework | Spring Boot (Web, Validation, HATEOAS, Security) | 2.6.3 |
| Persistence | MyBatis Spring Boot Starter | 2.2.2 |
| Database | SQLite (`org.xerial:sqlite-jdbc`) | 3.36.0.3 |
| Migrations | Flyway (version managed by Spring Boot) | — |
| GraphQL | Netflix DGS (`graphql-dgs-spring-boot-starter`) + DGS codegen plugin | 4.9.21 / 5.0.6 |
| Auth | JJWT (`jjwt-api`/`impl`/`jackson`) | 0.11.2 |
| Dates | Joda-Time | 2.10.13 |
| Boilerplate | Lombok | managed by Spring Boot |
| Formatting | Spotless (google-java-format) | 6.2.1 |
| Tests | JUnit 5, Spring Boot Test, REST Assured, MyBatis test starter | REST Assured 4.5.1 |

### Frontend (`frontend/`)

React 18, TypeScript 5, Vite 5, React Router 6, Axios, Tailwind CSS 3. See [`frontend/README.md`](frontend/README.md).

## Getting started

### Prerequisites

- **JDK 11** for the backend. The build targets Java 11 and uses Gradle 7.4, which does not support running on JDK 18+; if your default JDK is newer, point `JAVA_HOME` at a JDK 11 installation.
- **Node.js 18+ and npm** only if you want to run the frontend (Vite 5 requires Node 18 or newer).

### Run the backend

```bash
./gradlew bootRun
```

The API listens on **http://localhost:8080** (no `/api` prefix). On first start Flyway creates the schema in a local SQLite file, `dev.db`, in the working directory. Data persists across restarts; `./gradlew clean` deletes `dev.db`.

Check that it works:

```bash
curl http://localhost:8080/tags
# {"tags":[]}
```

GraphiQL is available at http://localhost:8080/graphiql and the GraphQL endpoint is `POST http://localhost:8080/graphql`.

### Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server runs at **http://localhost:3000** and calls the backend at `VITE_API_BASE_URL` (default `http://localhost:8080`). See [`frontend/README.md`](frontend/README.md) for details and current known issues.

### Build a runnable jar

```bash
./gradlew bootJar
java -jar build/libs/*-0.0.1-SNAPSHOT.jar   # jar name follows the project directory name
```

### Run with Docker

Requires Docker. Spring Boot's buildpack support builds the image; no `Dockerfile` is needed.

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The API is then available at http://localhost:8081. The SQLite file lives inside the container, so data is lost when the container is removed.

### Use another RealWorld frontend

Any RealWorld-compliant frontend can be pointed at `http://localhost:8080` (**not** `http://localhost:8080/api`, as some frontend docs suggest). CORS is open to all origins.

## Configuration

Runtime configuration lives in [`src/main/resources/application.properties`](src/main/resources/application.properties). Any property can be overridden the usual Spring Boot way (environment variables, `--property=value` arguments, profile-specific files).

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:sqlite:dev.db` | Database location. |
| `spring.jackson.deserialization.UNWRAP_ROOT_VALUE` | `true` | Request bodies use RealWorld root wrappers (`{"user": {...}}`, `{"article": {...}}`). |
| `image.default` | `https://static.productionready.io/images/smiley-cyrus.jpg` | Avatar assigned to newly registered users. |
| `jwt.secret` | committed sample value | HS512 signing key for JWTs. **Override this outside local development** (e.g. `JWT_SECRET=... ./gradlew bootRun`). |
| `jwt.sessionTime` | `86400` | Token lifetime in seconds (24 h). |
| `mybatis.*` | see file | Mapper XML location (`mapper/*.xml`), type handlers, camelCase mapping. |

Tests use [`application-test.properties`](src/main/resources/application-test.properties), which switches to an in-memory SQLite database.

## API overview

Both APIs share the same application and domain layers. Authenticated requests send the JWT returned by register/login in the header:

```
Authorization: Token <jwt>
```

### REST

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/users` | – | Register |
| `POST` | `/users/login` | – | Log in |
| `GET` | `/user` | required | Current user |
| `PUT` | `/user` | required | Update current user |
| `GET` | `/profiles/{username}` | optional | Get profile |
| `POST` / `DELETE` | `/profiles/{username}/follow` | required | Follow / unfollow |
| `GET` | `/articles` | optional | List articles (`tag`, `author`, `favorited`, `offset`, `limit`) |
| `GET` | `/articles/feed` | required | Articles by followed users (`offset`, `limit`) |
| `POST` | `/articles` | required | Create article |
| `GET` | `/articles/{slug}` | optional | Get article |
| `PUT` / `DELETE` | `/articles/{slug}` | required (author) | Update / delete article |
| `POST` / `DELETE` | `/articles/{slug}/favorite` | required | Favorite / unfavorite |
| `GET` | `/articles/{slug}/comments` | optional | List comments |
| `POST` | `/articles/{slug}/comments` | required | Add comment |
| `DELETE` | `/articles/{slug}/comments/{id}` | required (comment or article author) | Delete comment |
| `GET` | `/tags` | – | List tags |

### GraphQL

Schema: [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls).

- **Queries:** `article`, `articles`, `feed`, `me`, `profile`, `tags`
- **Mutations:** `createUser`, `login`, `updateUser`, `followUser`, `unfollowUser`, `createArticle`, `updateArticle`, `favoriteArticle`, `unfavoriteArticle`, `deleteArticle`, `addComment`, `deleteComment`

List fields (`articles`, `feed`, `Profile.articles`/`favorites`/`feed`, `Article.comments`) are Relay-style connections with cursor pagination. Exactly one of `first` or `last` must be provided.

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ articles(first: 5) { edges { node { slug title author { username } } } pageInfo { hasNextPage endCursor } } }"}'
```

A simplified view of the schema:

![GraphQL schema](graphql-schema.png)

See [`docs/API.md`](docs/API.md) for request/response formats, status codes and pagination details.

## Project structure

```
.
├── build.gradle                      # Gradle build (Spring Boot, DGS codegen, Spotless)
├── src/main/java/io/spring
│   ├── RealWorldApplication.java     # Spring Boot entry point
│   ├── JacksonCustomizations.java    # Joda DateTime JSON serialization
│   ├── MyBatisConfig.java            # @EnableTransactionManagement
│   ├── Util.java                     # String helper
│   ├── api/                          # REST controllers (Spring MVC)
│   │   ├── exception/                # Error types + global exception handler
│   │   └── security/                 # Spring Security config + JWT filter
│   ├── graphql/                      # DGS data fetchers and mutations
│   │   └── exception/                # GraphQL error mapping
│   ├── application/                  # Use-case layer
│   │   ├── article/, user/           # Command services, params, custom validators
│   │   ├── data/                     # Read-model DTOs (ArticleData, ProfileData, ...)
│   │   └── *QueryService.java        # Read-side services + paging helpers
│   ├── core/                         # Domain model
│   │   ├── article/, comment/, favorite/, user/   # Entities + repository interfaces
│   │   └── service/                  # AuthorizationService, JwtService interface
│   └── infrastructure/               # Technical implementations
│       ├── mybatis/mapper/           # Write-side MyBatis mappers
│       ├── mybatis/readservice/      # Read-side MyBatis query interfaces
│       ├── repository/               # MyBatis-backed repositories
│       └── service/                  # DefaultJwtService (JJWT)
├── src/main/resources
│   ├── application.properties
│   ├── db/migration/                 # Flyway migrations
│   ├── mapper/                       # MyBatis XML mappings
│   └── schema/schema.graphqls        # GraphQL schema (input for DGS codegen)
├── src/test/java/io/spring           # API, application, domain and infrastructure tests
├── frontend/                         # React + Vite single-page app
└── docs/                             # Architecture notes and API reference
```

Java types for the GraphQL schema (e.g. `io.spring.graphql.types.*`, `DgsConstants`) are generated at build time by the DGS codegen plugin (`generateJava` task) into `build/generated`.

## Testing and code style

```bash
./gradlew test               # Run backend tests
./gradlew spotlessJavaCheck  # Verify formatting (google-java-format)
./gradlew spotlessJavaApply  # Reformat Java sources
```

CI ([`.github/workflows/gradle.yml`](.github/workflows/gradle.yml)) runs `./gradlew clean test` on JDK 11 for every push and pull request. The frontend is not built in CI.

## Further documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) – layers, CQRS read/write split, persistence, security
- [`docs/API.md`](docs/API.md) – REST and GraphQL reference
- [`frontend/README.md`](frontend/README.md) – frontend setup and features

## Contributing

Fork the repository and open a pull request. Run `./gradlew spotlessJavaApply test` before submitting.
