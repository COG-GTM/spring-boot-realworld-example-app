# RealWorld example app with Java and Spring Boot

![RealWorld example app](example-logo.png)

[![Java CI](https://github.com/ankehao-demo/spring-boot-realworld-example-app/actions/workflows/gradle.yml/badge.svg)](https://github.com/ankehao-demo/spring-boot-realworld-example-app/actions/workflows/gradle.yml)

This repository implements the [RealWorld](https://github.com/gothinkster/realworld) social blogging API with a Java backend and an optional React frontend. The backend exposes the same domain through REST and GraphQL adapters.

## Technology stack

The versions below come from the checked-in build files.

| Area | Technology |
| --- | --- |
| Runtime | Java 11 source/target compatibility; JDK 11 for the full build |
| Application | Spring Boot 2.6.3 |
| REST and security | Spring MVC, Spring Security, JWT |
| GraphQL | Netflix DGS 4.9.21, DGS code generation plugin 5.0.6 |
| Persistence | MyBatis 2.2.2, SQLite JDBC 3.36.0.3, Flyway |
| Build and formatting | Gradle wrapper 7.4, Spotless 6.2.1 |
| Optional frontend | React 18.2, TypeScript 5.2, Vite 5.2, Tailwind CSS 3.4 |

CI builds the backend with JDK 11. Use JDK 11 for the full Gradle workflow: the pinned Google Java Format version used by Spotless is not compatible with JDK 17's module access rules.

## Prerequisites

- JDK 11
- Docker, only for the container workflow
- Node.js 18+ and npm, only for the optional frontend

No external database is required.

## Build and run the backend

```bash
./gradlew compileJava compileTestJava
./gradlew bootRun
```

The backend listens on `http://localhost:8080`. REST routes are served from the root (for example, `/tags`), not from an `/api` prefix.

Verify the REST API:

```bash
curl http://localhost:8080/tags
```

Verify GraphQL:

```bash
curl http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  --data '{"query":"{ tags }"}'
```

The GraphiQL interface is available at `http://localhost:8080/graphiql`.

### Local database

The application stores local data in `dev.db` at the repository root. Flyway creates the schema from `src/main/resources/db/migration/` when the application starts.

`./gradlew clean` deletes `dev.db`. Do not use `clean` when you need to preserve local data.

Tests use an in-memory SQLite database through the `test` Spring profile.

## Run the optional frontend

Start the backend first, then open another terminal:

```bash
cd frontend
cp .env.example .env
npm ci
npm run dev
```

The frontend listens on `http://localhost:3000` and sends API requests to the URL configured by `VITE_API_BASE_URL` (default: `http://localhost:8080`).

See [frontend/README.md](frontend/README.md) for frontend-specific commands and structure.

## API overview

### REST

The REST adapter implements registration and login, current-user management, profiles and follows, article CRUD and feeds, favorites, comments, and tags.

Authenticated requests use the RealWorld authorization format:

```text
Authorization: Token <jwt>
```

See [docs/API.md](docs/API.md) for the endpoint table, query parameters, response conventions, and examples.

### GraphQL

GraphQL is served at `/graphql`, with the schema in [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls). It covers:

- user registration, login, current-user queries, and profile updates
- profile lookup, following, and unfollowing
- article lookup, filtered connections, personalized feeds, and article mutations
- favorites, comments, and tags

List fields use cursor connections. Supply either `first` with an optional `after` cursor or `last` with an optional `before` cursor.

The DGS plugin generates Java types and `DgsConstants` in `build/generated` during `generateJava`/compilation. Generated sources are not committed and must not be edited manually.

## Architecture

The backend uses domain-oriented layers and a CQRS-style split:

- REST controllers and GraphQL data fetchers are delivery adapters.
- Write operations use application command services, domain entities, repository interfaces, and MyBatis repository implementations.
- Read operations use application query services and MyBatis read mappers that project directly into response DTOs.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for request flows, package responsibilities, persistence details, and GraphQL code generation.

## Project structure

```text
.
├── build.gradle
├── frontend/                              # Optional React/Vite client
├── src/main/java/io/spring/
│   ├── api/                               # REST controllers, security, REST errors
│   ├── graphql/                           # DGS queries, mutations, field resolvers
│   ├── core/                              # Domain entities and repository contracts
│   ├── application/                       # Commands, queries, validation, read DTOs
│   └── infrastructure/                    # MyBatis and JWT implementations
├── src/main/resources/
│   ├── db/migration/                      # Flyway schema migrations
│   ├── mapper/                            # MyBatis XML mappings
│   ├── schema/                            # GraphQL schema
│   └── application.properties             # Local runtime configuration
└── src/test/java/io/spring/               # REST, application, repository, and JWT tests
```

## Quality checks

Format Java sources before committing:

```bash
./gradlew spotlessJavaApply
```

Run the same compile and test checks used during development:

```bash
./gradlew spotlessJavaCheck
./gradlew compileJava compileTestJava
./gradlew test
```

CI runs `./gradlew clean test` on JDK 11.

The frontend defines separate checks:

```bash
cd frontend
npm run lint
npm run build
```

These frontend checks are not green in the current codebase: linting has no project ESLint configuration, and the TypeScript build reports unused React imports.

## Container image

```bash
./gradlew bootBuildImage --imageName=spring-boot-realworld-example-app
docker run --rm -p 8081:8080 spring-boot-realworld-example-app
```

The API is then available at `http://localhost:8081`.

## Configuration and security

Local defaults are in `src/main/resources/application.properties`, including the SQLite location, JWT lifetime, and a development signing key. Override the JWT signing key outside local development; do not reuse the checked-in development value in a deployed environment.

The security filter accepts JWTs from the `Authorization` header. Public REST reads do not require a token, while feed and write routes do. The GraphQL HTTP endpoint is public so the data fetchers can apply authentication at field and mutation level.

## License

See [LICENSE](LICENSE).
