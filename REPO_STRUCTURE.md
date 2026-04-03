# Repository Structure Summary

## Overview

**spring-boot-realworld-example-app** is a [RealWorld](https://github.com/gothinkster/realworld) spec implementation using **Spring Boot 2.6.3**, **MyBatis**, **SQLite**, and **Netflix DGS (GraphQL)**. It exposes both REST and GraphQL APIs for a Medium-like blogging platform (articles, comments, tags, users, favorites, follows).

| Metric | Value |
|---|---|
| Java source files | 93 (main) + 23 (test) = 116 total |
| Lines of Java code | 3,888 (main) + 2,082 (test) = 5,970 total |
| Java version | 11 |
| Build tool | Gradle (Groovy DSL) |
| Database | SQLite (dev.db), with Flyway migrations |
| Code formatter | Spotless (Google Java Format) |

---

## Directory Layout

```
.
├── build.gradle                  # Gradle build config (plugins, deps, tasks)
├── gradlew / gradlew.bat         # Gradle wrapper scripts
├── gradle/wrapper/               # Gradle wrapper JAR + properties
├── LICENSE
├── README.md
├── example-logo.png
├── graphql-schema.png
└── src/
    ├── main/
    │   ├── java/io/spring/
    │   │   ├── RealWorldApplication.java      # Spring Boot entry point
    │   │   ├── JacksonCustomizations.java      # JSON serialization config
    │   │   ├── MyBatisConfig.java              # MyBatis configuration
    │   │   ├── Util.java                       # Utility helpers
    │   │   ├── api/                            # REST controllers (Spring MVC)
    │   │   ├── application/                    # Query services, DTOs, commands (CQRS read side)
    │   │   ├── core/                           # Domain model (entities, repository interfaces)
    │   │   ├── graphql/                        # GraphQL data fetchers & mutations (DGS)
    │   │   └── infrastructure/                 # MyBatis mappers, repository impls, JWT service
    │   └── resources/
    │       ├── application.properties          # App config (SQLite, JWT, MyBatis)
    │       ├── application-test.properties     # Test profile overrides
    │       ├── db/migration/V1__create_tables.sql  # Flyway DB schema
    │       ├── mapper/                         # MyBatis XML mapper files
    │       └── schema/schema.graphqls          # GraphQL schema definition
    └── test/
        └── java/io/spring/
            ├── api/                            # REST API integration tests
            ├── application/                    # Query service tests
            ├── core/                           # Domain model unit tests
            └── infrastructure/                 # Repository & service tests
```

---

## Architecture (DDD + CQRS)

The codebase follows Domain-Driven Design principles with a CQRS (Command Query Responsibility Segregation) pattern:

### 1. `api/` - Web Layer (REST)
REST controllers built with Spring MVC. Each controller handles a specific resource:

| Controller | Endpoint(s) |
|---|---|
| `ArticleApi` | Single article CRUD (`/articles/{slug}`) |
| `ArticlesApi` | Article listing & creation (`/articles`) |
| `ArticleFavoriteApi` | Favorite/unfavorite (`/articles/{slug}/favorite`) |
| `CommentsApi` | Comments on articles (`/articles/{slug}/comments`) |
| `CurrentUserApi` | Current user profile (`/user`) |
| `ProfileApi` | User profiles & follow/unfollow (`/profiles/{username}`) |
| `TagsApi` | Tag listing (`/tags`) |
| `UsersApi` | Registration & login (`/users`) |

**Sub-packages:**
- `api/exception/` - Custom exception classes and global exception handler
- `api/security/` - JWT filter and Spring Security configuration

### 2. `core/` - Domain Model
Pure domain entities and repository interfaces (no framework dependencies):

| Entity | Description |
|---|---|
| `Article` | Blog article with title, slug, body, description, tags |
| `Tag` | Tag/label for articles |
| `Comment` | Comment on an article |
| `User` | User account with username, email, password, bio, image |
| `FollowRelation` | Follow relationship between users |
| `ArticleFavorite` | User-article favorite relationship |

Repository interfaces: `ArticleRepository`, `CommentRepository`, `UserRepository`, `ArticleFavoriteRepository`

Service interfaces: `JwtService`, `AuthorizationService`

### 3. `application/` - Application Services (CQRS Read Side)
High-level query services that assemble data transfer objects for the API layer:

- `ArticleQueryService` - Fetches articles with author profiles, favorites, tags
- `CommentQueryService` - Fetches comments with author profiles
- `ProfileQueryService` - Fetches user profiles with follow status
- `TagsQueryService` - Fetches all tags
- `UserQueryService` - Fetches user data

**Sub-packages:**
- `application/data/` - DTOs: `ArticleData`, `CommentData`, `ProfileData`, `UserData`, `UserWithToken`, etc.
- `application/article/` - Article command service, validation, and param objects
- `application/user/` - User registration/update params, validators, `UserService`
- Pagination support: `CursorPager`, `CursorPageParameter`, `Page`, `PageCursor`, `Node`, `DateTimeCursor`

### 4. `graphql/` - GraphQL Layer (Netflix DGS)
GraphQL data fetchers and mutations, providing an alternative API to the REST layer:

| Class | Purpose |
|---|---|
| `ArticleDatafetcher` | Article queries |
| `ArticleMutation` | Article create/update/delete/favorite |
| `CommentDatafetcher` | Comment queries |
| `CommentMutation` | Comment add/delete |
| `MeDatafetcher` | Current user query |
| `ProfileDatafetcher` | Profile queries |
| `RelationMutation` | Follow/unfollow mutations |
| `TagDatafetcher` | Tags query |
| `UserMutation` | User registration/login/update |
| `SecurityUtil` | Auth helper for GraphQL context |

**Sub-package:** `graphql/exception/` - GraphQL-specific exception handling

### 5. `infrastructure/` - Technical Implementation
Concrete implementations of domain interfaces:

- `infrastructure/repository/` - MyBatis-backed repository implementations (`MyBatisArticleRepository`, `MyBatisUserRepository`, etc.)
- `infrastructure/mybatis/mapper/` - MyBatis Java mapper interfaces (`ArticleMapper`, `UserMapper`, etc.)
- `infrastructure/mybatis/readservice/` - MyBatis read-only services for CQRS queries (`ArticleReadService`, `CommentReadService`, etc.)
- `infrastructure/mybatis/DateTimeHandler.java` - Custom Joda DateTime type handler
- `infrastructure/service/DefaultJwtService.java` - JWT token generation/validation using jjwt

---

## Database Schema

SQLite database (`dev.db`) with Flyway migration. 7 tables:

| Table | Description |
|---|---|
| `users` | User accounts (id, username, password, email, bio, image) |
| `articles` | Blog articles (id, user_id, slug, title, description, body, timestamps) |
| `comments` | Article comments (id, body, article_id, user_id, timestamps) |
| `tags` | Tag definitions (id, name) |
| `article_tags` | Article-tag join table |
| `article_favorites` | User-article favorites join table |
| `follows` | User-user follow relationships |

---

## Key Dependencies

| Dependency | Purpose |
|---|---|
| Spring Boot 2.6.3 | Application framework |
| Spring Security | Authentication & authorization |
| MyBatis 2.2.2 | SQL data mapper / ORM |
| Netflix DGS 4.9.21 | GraphQL server framework |
| Flyway | Database migration management |
| SQLite (via JDBC) | Embedded database |
| jjwt 0.11.2 | JWT token handling |
| Joda-Time 2.10.13 | Date/time utilities |
| Lombok | Boilerplate reduction (annotations) |
| Spotless + Google Java Format | Code formatting |
| REST Assured 4.5.1 | API testing |

---

## Test Structure

23 test files covering:

| Test Area | Files | What's Tested |
|---|---|---|
| `api/` | 9 | REST endpoint integration tests (articles, comments, users, profiles, favorites) |
| `application/` | 4 | Query service logic (articles, comments, profiles, tags) |
| `core/` | 1 | Domain entity behavior (Article) |
| `infrastructure/` | 7 | Repository persistence, JWT service, DB base class |

---

## Quick Commands

```bash
./gradlew bootRun              # Start the app (http://localhost:8080)
./gradlew test                 # Run all tests
./gradlew spotlessJavaApply    # Format code (Google Java Format)
./gradlew clean                # Clean build + delete dev.db
```
