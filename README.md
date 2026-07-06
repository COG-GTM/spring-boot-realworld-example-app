# ![RealWorld Example App using Spring Boot and MyBatis](example-logo.png)

> ### Spring Boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged backend application built with **Spring Boot + MyBatis** including CRUD operations, authentication, routing, pagination, and more.

It exposes the same domain through **both a REST API and a GraphQL API**. For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Java (source/target compatibility **11**; also compiles on JDK 17) |
| Framework | Spring Boot **2.6.3** (Web, Security, Validation, HATEOAS) |
| Persistence | [MyBatis](https://mybatis.org/mybatis-3/) (`mybatis-spring-boot-starter` 2.2.2) |
| GraphQL | [Netflix DGS](https://netflix.github.io/dgs/) (`graphql-dgs-spring-boot-starter` 4.9.21) + DGS codegen plugin 5.0.6 |
| Database | [SQLite](https://www.sqlite.org/) (`org.xerial:sqlite-jdbc` 3.36.0.3) |
| Migrations | [Flyway](https://flywaydb.org/) |
| Auth | JWT via [jjwt](https://github.com/jwtk/jjwt) 0.11.2 (HS512) + Spring Security |
| Date/time | Joda-Time 2.10.13 |
| Boilerplate | Lombok |
| Build | Gradle **7.4** (wrapper) |
| Formatting | [Spotless](https://github.com/diffplug/spotless) 6.2.1 with google-java-format |
| Testing | JUnit 5, [REST Assured](https://rest-assured.io/) 4.5.1, spring-security-test, mybatis test starter |

## How it works

The application follows **Domain-Driven Design** with a **CQRS-lite** split between the read and write models. REST (`io.spring.api`) and GraphQL (`io.spring.graphql`) are just *adapters* over a shared domain, so the domain layer stays consistent regardless of the protocol.

* [Domain Driven Design](https://en.wikipedia.org/wiki/Domain-driven_design) is used to separate business terms from infrastructure terms.
* MyBatis implements the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* The [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern separates the read model from the write model.

### Project structure

All source lives under `src/main/java/io/spring/`:

| Package | Responsibility |
| --- | --- |
| `api` | REST adapter — Spring MVC controllers, security (`WebSecurityConfig`, `JwtTokenFilter`), REST exception handling. |
| `graphql` | GraphQL adapter — Netflix DGS datafetchers and mutations. |
| `core` | Domain model — entities, repository **interfaces**, and pure domain services (`AuthorizationService`, `JwtService`). No Spring, no SQL. |
| `application` | Read-side query services, DTOs (`application/data/*`), command/param objects, validators, and cursor-pagination helpers. |
| `infrastructure` | Technical implementations — MyBatis repositories/read-services and the JWT service. |

Dependency direction: `api`/`graphql` → `application` → `core` ← `infrastructure`. `core` depends on nothing else; `infrastructure` implements the `core` interfaces.

**CQRS-lite rule:**

* **Write path:** adapter → `application/*CommandService` (or `UserService`) → domain entity method → `core` `*Repository` interface → `infrastructure/repository/MyBatis*Repository`. Repositories deal in **domain objects** (e.g. `core.article.Article`).
* **Read path:** adapter → `application/*QueryService` → `infrastructure/mybatis/readservice/*ReadService` (`@Mapper`) → returns **DTOs** from `application/data/*` (e.g. `ArticleData`).

Other notable resources:

* `src/main/resources/mapper/*.xml` — MyBatis SQL mapping files.
* `src/main/resources/schema/schema.graphqls` — GraphQL schema (DGS codegen generates `io.spring.graphql.types.*` into `build/generated` at build time).
* `src/main/resources/db/migration/` — Flyway migrations.

## Getting started

You'll need **Java 11+** installed (the project also compiles and runs on JDK 17).

    ./gradlew bootRun

To check that it works, open a browser tab at <http://localhost:8080/tags>, or run:

    curl http://localhost:8080/tags

The REST API entry point is `http://localhost:8080` — **not** `http://localhost:8080/api` as some frontend documentation suggests.

## Database

The app uses an on-disk **SQLite** database at `dev.db` (so local test data survives restarts). The schema is created and versioned by **Flyway** migrations in `src/main/resources/db/migration/`. Tests run against an in-memory SQLite database (`application-test.properties`). The datasource can be changed in `src/main/resources/application.properties` for any other database.

> `./gradlew clean` deletes `dev.db`.

## Security

Authentication is stateless and JWT-based. `JwtTokenFilter` reads the `Authorization` header, extracts the token (format `Authorization: Token <jwt>`), validates it via `JwtService`, and populates the Spring `SecurityContext`. `WebSecurityConfig` disables CSRF, enables CORS, and permits unauthenticated access to registration/login, public GETs (`/articles/**`, `/profiles/**`, `/tags`) and the GraphQL endpoints; everything else requires authentication. The JWT secret and session lifetime are configured in `application.properties` (`jwt.secret`, `jwt.sessionTime`).

## REST API

Base URL: `http://localhost:8080`. Request/response bodies are JSON and root-wrapped (e.g. `{"user": {...}}`, `{"article": {...}}`) per the RealWorld spec. Endpoints marked 🔒 require a valid JWT.

| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| POST | `/users` | Register a new user | |
| POST | `/users/login` | Log in, returns a user with JWT | |
| GET | `/user` | Get the current user | 🔒 |
| PUT | `/user` | Update the current user | 🔒 |
| GET | `/profiles/{username}` | Get a profile | |
| POST | `/profiles/{username}/follow` | Follow a user | 🔒 |
| DELETE | `/profiles/{username}/follow` | Unfollow a user | 🔒 |
| GET | `/articles` | List articles (filters: `tag`, `author`, `favorited`, `offset`, `limit`) | |
| GET | `/articles/feed` | Articles from followed users | 🔒 |
| POST | `/articles` | Create an article | 🔒 |
| GET | `/articles/{slug}` | Get an article | |
| PUT | `/articles/{slug}` | Update an article | 🔒 |
| DELETE | `/articles/{slug}` | Delete an article | 🔒 |
| POST | `/articles/{slug}/favorite` | Favorite an article | 🔒 |
| DELETE | `/articles/{slug}/favorite` | Unfavorite an article | 🔒 |
| GET | `/articles/{slug}/comments` | List comments on an article | |
| POST | `/articles/{slug}/comments` | Add a comment | 🔒 |
| DELETE | `/articles/{slug}/comments/{id}` | Delete a comment | 🔒 |
| GET | `/tags` | List all tags | |

List endpoints use offset/limit pagination (`offset` defaults to `0`, `limit` to `20`).

## GraphQL API

The same domain is exposed through GraphQL, implemented with the [dgs-framework](https://github.com/Netflix/dgs-framework).

* Schema: [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls)
* GraphQL endpoint: `POST http://localhost:8080/graphql`
* Interactive explorer (GraphiQL): <http://localhost:8080/graphiql>

Unlike REST, the article/comment connections in GraphQL use **cursor-based (keyset) pagination** (`first`/`after`/`last`/`before`, returning `edges` and `pageInfo`). The schema visualization looks like this:

![GraphQL schema](graphql-schema.png)

## Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.

    ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
    docker run -p 8081:8080 spring-boot-realworld-example-app

## Try it out with a RealWorld frontend

Point any [RealWorld frontend](https://github.com/gothinkster/realworld) at `http://localhost:8080` (again, **not** `/api`).

## Run tests

The repository contains many test cases covering both the API and the repository layers.

    ./gradlew test

## Code format

Spotless (google-java-format) enforces formatting; the build fails if code is unformatted. Apply it before committing:

    ./gradlew spotlessJavaApply

To verify without changing files:

    ./gradlew spotlessJavaCheck

## Help

Please fork and open a PR to improve the project.
