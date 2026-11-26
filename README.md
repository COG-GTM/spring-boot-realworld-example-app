# ![RealWorld Example App using Spring Boot](example-logo.png)

> Spring Boot + MyBatis backend for the [RealWorld](https://github.com/gothinkster/realworld) spec, with a REST API and a GraphQL API on the same domain model.

It implements Conduit, a Medium-style blogging platform: users, profiles, follows, articles, comments, favorites, tags, and paginated feeds.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 (Gradle toolchain) |
| Framework | Spring Boot 3.5, Spring Security 6 |
| GraphQL | [Netflix DGS](https://github.com/Netflix/dgs-framework) 10 on Spring for GraphQL, with DGS codegen |
| Persistence | MyBatis 3, SQLite, Flyway migrations |
| Auth | Stateless JWT (jjwt 0.12) |
| Build | Gradle 8.14 wrapper, Spotless with google-java-format |
| Tests | JUnit 5, Rest-Assured 5, Spring Boot Test |

## Getting started

You need Java 17. The Gradle wrapper downloads Gradle itself.

```bash
./gradlew bootRun
```

The app starts on http://localhost:8080. Check it with:

```bash
curl http://localhost:8080/tags
```

The API is served at the root (`http://localhost:8080`), **not** under `/api` as some RealWorld frontend docs suggest.

### Docker

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

## APIs

### REST

Follows the [RealWorld API spec](https://realworld-docs.netlify.app/specifications/backend/endpoints/). Send the token as `Authorization: Token <jwt>`.

| Endpoint | Auth |
|---|---|
| `POST /users`, `POST /users/login` | Public |
| `GET /user`, `PUT /user` | Required |
| `GET /profiles/{username}` | Public |
| `POST`/`DELETE /profiles/{username}/follow` | Required |
| `GET /articles`, `GET /articles/{slug}` | Public |
| `GET /articles/feed` | Required |
| `POST /articles`, `PUT`/`DELETE /articles/{slug}` | Required |
| `GET /articles/{slug}/comments` | Public |
| `POST /articles/{slug}/comments`, `DELETE /articles/{slug}/comments/{id}` | Required |
| `POST`/`DELETE /articles/{slug}/favorite` | Required |
| `GET /tags` | Public |

Trailing slashes are accepted (e.g. `/tags/`).

### GraphQL

- Endpoint: `POST /graphql` with a JSON body (`{"query": "...", "variables": {...}}`).
- GraphiQL: http://localhost:8080/graphiql
- Schema: [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls)

Authenticated queries and mutations (e.g. `me`, `feed`, `createArticle`) use the same `Authorization: Token <jwt>` header. Tokens issued by REST work in GraphQL and the other way round.

![GraphQL schema](graphql-schema.png)

Java types for the schema are generated into `build/generated` by `./gradlew generateJava`, which runs automatically before compilation.

## Architecture

The code follows Domain-Driven Design and CQRS. REST and GraphQL are thin adapters over the same domain layer.

| Package | Role |
|---|---|
| `io.spring.api` | REST controllers, security config, JWT filter |
| `io.spring.graphql` | DGS data fetchers, mutations, GraphQL error handling |
| `io.spring.application` | Query services and command services that return DTOs |
| `io.spring.core` | Domain entities and repository interfaces |
| `io.spring.infrastructure` | MyBatis mappers, repository implementations, JWT service |

- MyBatis implements the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern; SQL lives in `src/main/resources/mapper/*.xml`.
- [CQRS](https://martinfowler.com/bliki/CQRS.html) separates read models (`*QueryService`, read services) from write models (repositories).

## Configuration

Settings live in `src/main/resources/application.properties`.

| Property | Purpose |
|---|---|
| `spring.datasource.url` | Database URL (default `jdbc:sqlite:dev.db`) |
| `jwt.secret` | HMAC signing key. Must be at least 32 bytes (256 bits) or the app fails to start. Override it outside local development, e.g. with the `JWT_SECRET` environment variable. |
| `jwt.sessionTime` | Token lifetime in seconds (default 86400) |

### Database

By default the app uses a SQLite file, `dev.db`, in the working directory, so data survives restarts. Flyway applies the migrations in `src/main/resources/db/migration` on startup.

To switch databases, change the datasource properties and add the JDBC driver. Flyway 10+ ships database support as separate modules, so also add the matching one (e.g. `org.flywaydb:flyway-mysql` or `org.flywaydb:flyway-database-postgresql`).

Tests and `bootRun` share `dev.db`. Don't run `./gradlew clean build` or `test` while the app is running, or the app may fail with `SQLITE_READONLY_DBMOVED`.

## Development

| Task | Command |
|---|---|
| Run tests | `./gradlew test` |
| Full build (format check, compile, tests) | `./gradlew build` |
| Format code | `./gradlew spotlessJavaApply` |
| Regenerate GraphQL types | `./gradlew generateJava` |

The build fails if code isn't formatted, so run `spotlessJavaApply` before committing.

## Contributing

Fork the repo and open a PR.

## License

See [LICENSE](LICENSE).
