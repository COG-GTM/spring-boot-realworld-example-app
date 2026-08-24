# Backend architecture

## Overview

The backend presents one social-blogging domain through two delivery adapters:

```text
REST controllers ─────┐
                      ├── application commands and queries
GraphQL data fetchers ┘              │
                         ┌────────────┴────────────┐
                         │                         │
                 domain repositories       MyBatis read mappers
                         │                         │
                 MyBatis implementations ─────────┘
                                      │
                                   SQLite
```

The design is domain-oriented and uses a CQRS-style separation between writes and response-oriented reads. It is not event-sourced CQRS.

## Package responsibilities

### `io.spring.api`

The REST adapter contains:

- Spring MVC controllers
- stateless Spring Security configuration
- JWT extraction into the Spring Security context
- RealWorld-shaped validation and error responses

Controllers delegate writes to command services or domain repositories and use query services to build response DTOs.

### `io.spring.graphql`

The GraphQL adapter contains Netflix DGS:

- root query and mutation data fetchers
- nested field resolvers for profiles, articles, and comments
- Spring Security context helpers
- GraphQL exception conversion

Resolvers use `DataFetcherResult.localContext` to pass domain or read-model data to nested fields without exposing it in the schema.

### `io.spring.core`

The core package defines:

- `User`, `Article`, `Tag`, `Comment`, `ArticleFavorite`, and `FollowRelation`
- repository contracts for users, articles, comments, and favorites
- authorization and JWT service contracts

Entities own identity and basic state transitions. Article slugs are generated from titles, and article updates recalculate the slug when the title changes.

### `io.spring.application`

The application layer contains:

- write services such as `UserService` and `ArticleCommandService`
- read/query services for users, profiles, articles, comments, and tags
- request parameters and validation constraints
- REST read-model DTOs under `application.data`
- offset and cursor pagination helpers

Write services operate on domain entities and repository interfaces. Query services call MyBatis read-service mapper interfaces and enrich projections with viewer-specific `following` and `favorited` state.

### `io.spring.infrastructure`

Infrastructure provides:

- MyBatis implementations of domain repository interfaces
- MyBatis mapper and read-service interfaces backed by XML
- the Joda-Time MyBatis type handler
- the JJWT-based `JwtService` implementation

## Write path

An article creation request follows this path:

```text
REST or GraphQL adapter
  → ArticleCommandService
  → Article domain object
  → ArticleRepository
  → MyBatisArticleRepository
  → ArticleMapper XML
  → SQLite
```

After a write, adapters normally query the read model again so responses include author information, favorite counts, and viewer-specific state.

## Read path

An article list follows this path:

```text
REST or GraphQL adapter
  → ArticleQueryService
  → ArticleReadService / related read mappers
  → ArticleData projections
  → viewer-specific enrichment
  → REST JSON or GraphQL types
```

The read path does not load domain aggregates. MyBatis maps joined query results directly into `application.data` DTOs.

## Persistence

The runtime database is SQLite:

- local runtime URL: `jdbc:sqlite:dev.db`
- test URL: `jdbc:sqlite::memory:`
- schema management: Flyway
- initial migration: `src/main/resources/db/migration/V1__create_tables.sql`

The schema contains users, articles, tags, article-tag relations, favorites, follows, and comments. MyBatis SQL is split between command-side mapper XML and read-side projection XML under `src/main/resources/mapper/`.

The Gradle `clean` task deletes the local `dev.db`.

## Transactions

`MyBatisArticleRepository.save` is transactional because article creation can insert tags, article-tag relations, and the article record. Other repository methods rely on individual mapper operations.

## REST pagination

REST article lists use offset pagination through `Page`:

- default offset: `0`
- default limit: `20`
- maximum limit: `100`

## GraphQL pagination

GraphQL list fields use timestamp-based cursor pagination:

- `DateTimeCursor` serializes the timestamp as epoch milliseconds
- `CursorPageParameter` requests one extra row to determine page availability
- `CursorPager` produces edges and Relay-style `PageInfo`
- page size defaults to `20` and is capped at `1000`

## GraphQL code generation

The schema lives in `src/main/resources/schema/schema.graphqls`. The `com.netflix.dgs.codegen` Gradle plugin generates:

- schema types under `io.spring.graphql.types`
- `io.spring.graphql.DgsConstants`

Generated sources are written below `build/generated`, excluded from Spotless, and recreated by `generateJava` or Java compilation.

## Security model

`JwtTokenFilter` reads the `Authorization` header, asks `JwtService` for the token subject, loads the user, and sets a stateless Spring Security authentication.

REST route rules allow registration, login, public article/profile/tag reads, GraphQL, and GraphiQL without authentication. Other REST routes require an authenticated user. GraphQL mutations that modify protected resources obtain the current user inside their data fetchers.

## Tests

The test suite covers:

- REST controllers and security behavior
- application query services
- domain article behavior
- MyBatis repositories and transactions
- JWT creation and parsing

Tests use JUnit 5, Spring Boot Test, Mockito, Rest Assured Mock MVC, and in-memory SQLite.
