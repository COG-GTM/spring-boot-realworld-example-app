---
name: testing-realworld-api
description: Test the Spring Boot RealWorld (Conduit) API end-to-end. Use when verifying dependency upgrades, API changes, or runtime behavior of the backend.
---

# Testing the RealWorld API

## Prerequisites

- Java 17 (Gradle 7.6.x does not support Java 21; set `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`)
- No external services required (uses SQLite local file DB)
- No credentials needed (JWT secret is in `application.properties`)

## Running the App Locally

```bash
cd /home/ubuntu/repos/spring-boot-realworld-example-app
rm -f dev.db  # Fresh database
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew bootRun --no-daemon
```

App starts on `http://localhost:8080`. Wait for `Started RealWorldApplication` in logs.

## Key API Endpoints

### REST API

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | /users | No | Register user |
| POST | /users/login | No | Login, get JWT |
| GET | /user | Yes | Get current user |
| POST | /articles | Yes | Create article |
| GET | /articles/:slug | No | Get article |
| GET | /tags | No | List tags (good health check) |

### GraphQL

- Endpoint: `POST /graphql`
- Schema: `src/main/resources/schema/schema.graphqls`
- Key mutations: `createUser`, `login`, `createArticle`
- Key queries: `articles`, `tags`, `profile`

## Auth Flow

- Register: `POST /users` with `{"user":{"email":"...","username":"...","password":"..."}}`
- Response includes JWT token
- Use token: `Authorization: Token <jwt>` header
- JSON payloads must be wrapped in root name (Jackson UNWRAP_ROOT_VALUE enabled)

## Testing Strategy (Shell-Only, No Recording)

Since this is a pure backend API, test via curl commands:

1. **Startup test**: `GET /tags` returns 200
2. **Auth test**: Register → Login → Authenticated GET /user
3. **GraphQL test**: `createUser` mutation + `articles` query
4. **CRUD test**: Create article → Read back → Verify timestamps

## Common Pitfalls

- **Gradle 7.x + Java 21**: Fails with `Unsupported class file major version 65`. Must use Java 17.
- **DGS 5.x + Spring Boot 2.7**: `graphql-java` version conflict (`RuntimeWiring.transform` missing). Keep DGS at 4.9.21 for Boot 2.7.
- **REST-Assured + Spring Boot 2.7 BOM**: Boot 2.7's dependency management downgrades `rest-assured-common` and `spring-commons` to 4.5.1. Fix with `resolutionStrategy.eachDependency` forcing all `io.rest-assured` modules to the desired version.
- **Spotless 6.25.0**: Uses stricter google-java-format; run `./gradlew spotlessApply` after upgrades.

## Build & Test Commands

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew clean build --no-daemon  # Full build + tests
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew test --no-daemon          # Tests only
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew spotlessCheck --no-daemon  # Lint check
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew spotlessApply --no-daemon  # Auto-fix formatting
```
