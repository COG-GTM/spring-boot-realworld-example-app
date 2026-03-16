# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

---

# Table of Contents

- [GraphQL Support](#new-graphql-support)
- [How it Works](#how-it-works)
  - [Architecture Overview](#architecture-overview)
  - [Project Structure](#project-structure)
  - [Domain Model](#domain-model)
- [REST API Endpoints](#rest-api-endpoints)
  - [Authentication](#authentication-endpoints)
  - [User](#user-endpoints)
  - [Profiles](#profile-endpoints)
  - [Articles](#article-endpoints)
  - [Comments](#comment-endpoints)
  - [Tags](#tag-endpoints)
  - [Favorites](#favorite-endpoints)
- [Security](#security)
- [Database](#database)
  - [Schema](#database-schema)
  - [Migrations](#migrations)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running Locally](#running-locally)
  - [Docker](#try-it-out-with-docker)
  - [Using with a Frontend](#try-it-out-with-a-realworld-frontend)
- [Testing](#run-test)
- [Code Format](#code-format)
- [Tech Stack](#tech-stack)
- [Help](#help)

---

# *NEW* GraphQL Support  

Following some DDD principles. REST or GraphQL is just a kind of adapter. And the domain layer will be consistent all the time. So this repository implement GraphQL and REST at the same time.

The GraphQL schema is https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/src/main/resources/schema/schema.graphqls and the visualization looks like below.

![](graphql-schema.png)

And this implementation is using [dgs-framework](https://github.com/Netflix/dgs-framework) which is a quite new java graphql server framework.

## GraphQL Operations

The GraphQL API exposes the following operations:

**Queries:**
- `article(slug)` - Fetch a single article by slug
- `articles(first, after, last, before, authoredBy, favoritedBy, withTag)` - List/filter articles with cursor-based pagination
- `me` - Get the currently authenticated user
- `feed(first, after, last, before)` - Get the article feed for the authenticated user
- `profile(username)` - Get a user profile
- `tags` - List all tags

**Mutations:**
- `createUser`, `login`, `updateUser` - User management
- `followUser`, `unfollowUser` - Follow/unfollow profiles
- `createArticle`, `updateArticle`, `deleteArticle` - Article CRUD
- `favoriteArticle`, `unfavoriteArticle` - Favorite articles
- `addComment`, `deleteComment` - Comment management

The GraphQL datafetchers are located in `src/main/java/io/spring/graphql/` and include:

| Datafetcher | Description |
|---|---|
| `ArticleDatafetcher` | Resolves article queries and nested fields |
| `ArticleMutation` | Handles article create/update/delete/favorite mutations |
| `CommentDatafetcher` | Resolves comment queries |
| `CommentMutation` | Handles comment add/delete mutations |
| `MeDatafetcher` | Resolves the current authenticated user |
| `ProfileDatafetcher` | Resolves profile queries and nested profile fields |
| `RelationMutation` | Handles follow/unfollow mutations |
| `TagDatafetcher` | Resolves tag queries |
| `UserMutation` | Handles user create/login/update mutations |

---

# How it Works

The application uses Spring Boot (Web, Mybatis).

* Use the idea of Domain Driven Design to separate the business term and infrastructure term.
* Use MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* Use [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern to separate the read model and write model.

## Architecture Overview

The application follows a layered architecture inspired by Domain-Driven Design (DDD) and CQRS:

```
+----------------------------------------------------------+
|                    Adapter Layer                         |
|  +---------------------+  +---------------------------+  |
|  |   REST Controllers  |  |   GraphQL Datafetchers    |  |
|  |   (api/)            |  |   (graphql/)              |  |
|  +--------+------------+  +------------+--------------+  |
|           |                            |                 |
+-----------+----------------------------+-----------------+
|                  Application Layer                       |
|  +---------------------+  +---------------------------+  |
|  |   Query Services    |  |   Command Services        |  |
|  |   (read model)      |  |   (write model)           |  |
|  +--------+------------+  +------------+--------------+  |
|           |                            |                 |
+-----------+----------------------------+-----------------+
|                    Core / Domain Layer                   |
|  +-------------+ +----------+ +----------+ +---------+  |
|  |   Article   | |   User   | | Comment  | |Favorite |  |
|  |   + Tag     | | +Follow  | |          | |         |  |
|  +-------------+ +----------+ +----------+ +---------+  |
|          ^ Repository Interfaces                        |
+----------+-----------------------------------------------+
|                 Infrastructure Layer                     |
|  +----------------+  +-------------+  +--------------+   |
|  | MyBatis Mappers|  | Read Service|  |  JWT Service |   |
|  | (write)        |  | (queries)   |  |              |   |
|  +----------------+  +-------------+  +--------------+   |
|                          |                               |
|                    +-----+------+                        |
|                    |   SQLite   |                        |
|                    +------------+                        |
+----------------------------------------------------------+
```

## Project Structure

And the code is organized as this:

1. `api` is the web layer implemented by Spring MVC
2. `core` is the business model including entities and services
3. `application` is the high-level services for querying the data transfer objects
4. `infrastructure`  contains all the implementation classes as the technique details

```
src/main/java/io/spring/
|-- RealWorldApplication.java        # Spring Boot entry point
|-- JacksonCustomizations.java       # JSON serialization config
|-- MyBatisConfig.java               # MyBatis configuration
|-- Util.java                        # Utility helpers
|
|-- api/                             # REST API layer (Spring MVC controllers)
|   |-- ArticleApi.java              #   Single article CRUD (GET/PUT/DELETE /articles/{slug})
|   |-- ArticlesApi.java             #   Article listing & creation (GET/POST /articles)
|   |-- ArticleFavoriteApi.java      #   Favorite/unfavorite (POST/DELETE /articles/{slug}/favorite)
|   |-- CommentsApi.java             #   Comments CRUD (GET/POST/DELETE /articles/{slug}/comments)
|   |-- CurrentUserApi.java          #   Current user (GET/PUT /user)
|   |-- ProfileApi.java              #   User profiles & follow (GET/POST/DELETE /profiles/{username})
|   |-- TagsApi.java                 #   Tags listing (GET /tags)
|   |-- UsersApi.java                #   Registration & login (POST /users, POST /users/login)
|   |-- exception/                   #   Custom exception classes & handlers
|   |   |-- CustomizeExceptionHandler.java
|   |   |-- ErrorResource.java
|   |   |-- ErrorResourceSerializer.java
|   |   |-- FieldErrorResource.java
|   |   |-- InvalidAuthenticationException.java
|   |   |-- InvalidRequestException.java
|   |   |-- NoAuthorizationException.java
|   |   +-- ResourceNotFoundException.java
|   +-- security/                    #   Security configuration
|       |-- JwtTokenFilter.java      #     JWT authentication filter
|       +-- WebSecurityConfig.java   #     Spring Security config
|
|-- application/                     # Application services (CQRS query/command)
|   |-- ArticleQueryService.java     #   Article read operations
|   |-- CommentQueryService.java     #   Comment read operations
|   |-- ProfileQueryService.java     #   Profile read operations
|   |-- TagsQueryService.java        #   Tags read operations
|   |-- UserQueryService.java        #   User read operations
|   |-- CursorPager.java             #   Cursor-based pagination (for GraphQL)
|   |-- CursorPageParameter.java     #   Cursor page parameters
|   |-- DateTimeCursor.java          #   DateTime-based cursor implementation
|   |-- Node.java                    #   Pagination node interface
|   |-- Page.java                    #   Offset-based pagination (for REST)
|   |-- PageCursor.java              #   Page cursor wrapper
|   |-- article/                     #   Article command services & params
|   |   |-- ArticleCommandService.java
|   |   |-- NewArticleParam.java
|   |   |-- UpdateArticleParam.java
|   |   |-- DuplicatedArticleConstraint.java
|   |   +-- DuplicatedArticleValidator.java
|   |-- data/                        #   Data transfer objects (read models)
|   |   |-- ArticleData.java
|   |   |-- ArticleDataList.java
|   |   |-- ArticleFavoriteCount.java
|   |   |-- CommentData.java
|   |   |-- ProfileData.java
|   |   |-- UserData.java
|   |   +-- UserWithToken.java
|   +-- user/                        #   User command services & params
|       |-- UserService.java
|       |-- RegisterParam.java
|       |-- UpdateUserCommand.java
|       |-- UpdateUserParam.java
|       |-- DuplicatedEmailConstraint.java
|       |-- DuplicatedEmailValidator.java
|       |-- DuplicatedUsernameConstraint.java
|       +-- DuplicatedUsernameValidator.java
|
|-- core/                            # Domain layer (entities, repositories, services)
|   |-- article/
|   |   |-- Article.java             #   Article entity
|   |   |-- ArticleRepository.java   #   Article repository interface
|   |   +-- Tag.java                 #   Tag entity
|   |-- comment/
|   |   |-- Comment.java             #   Comment entity
|   |   +-- CommentRepository.java   #   Comment repository interface
|   |-- favorite/
|   |   |-- ArticleFavorite.java     #   ArticleFavorite entity
|   |   +-- ArticleFavoriteRepository.java
|   |-- user/
|   |   |-- User.java                #   User entity
|   |   |-- UserRepository.java      #   User repository interface
|   |   +-- FollowRelation.java      #   Follow relationship value object
|   +-- service/
|       |-- AuthorizationService.java #  Authorization logic
|       +-- JwtService.java          #   JWT service interface
|
|-- graphql/                         # GraphQL adapter layer (Netflix DGS)
|   |-- ArticleDatafetcher.java
|   |-- ArticleMutation.java
|   |-- CommentDatafetcher.java
|   |-- CommentMutation.java
|   |-- MeDatafetcher.java
|   |-- ProfileDatafetcher.java
|   |-- RelationMutation.java
|   |-- SecurityUtil.java
|   |-- TagDatafetcher.java
|   |-- UserMutation.java
|   +-- exception/                   #   GraphQL-specific exception handling
|
+-- infrastructure/                  # Infrastructure layer (implementations)
    |-- mybatis/
    |   |-- DateTimeHandler.java     #   Joda DateTime type handler for MyBatis
    |   |-- mapper/                  #   MyBatis mapper interfaces (write)
    |   |   |-- ArticleMapper.java
    |   |   |-- ArticleFavoriteMapper.java
    |   |   |-- CommentMapper.java
    |   |   +-- UserMapper.java
    |   +-- readservice/             #   MyBatis read service interfaces (read/CQRS)
    |       |-- ArticleReadService.java
    |       |-- ArticleFavoritesReadService.java
    |       |-- CommentReadService.java
    |       |-- TagReadService.java
    |       |-- UserReadService.java
    |       +-- UserRelationshipQueryService.java
    |-- repository/                  #   Repository implementations
    |   |-- MyBatisArticleRepository.java
    |   |-- MyBatisArticleFavoriteRepository.java
    |   |-- MyBatisCommentRepository.java
    |   +-- MyBatisUserRepository.java
    +-- service/
        +-- DefaultJwtService.java   #   JWT service implementation (jjwt)
```

## Domain Model

The application models a blogging platform (a Medium clone) with the following core entities:

- **User** - Registered users with email, username, password, bio, and image. Users can follow other users.
- **Article** - Blog posts with title, slug, description, body, and timestamps. Articles belong to a user (author) and can have tags.
- **Tag** - Labels that can be attached to articles for categorization.
- **Comment** - Comments on articles, belonging to both a user (author) and an article.
- **ArticleFavorite** - A many-to-many relationship tracking which users have favorited which articles.
- **FollowRelation** - A many-to-many relationship tracking which users follow which other users.

---

# REST API Endpoints

All endpoints follow the [RealWorld API spec](https://realworld-docs.netlify.app/specifications/backend/endpoints/).

## Authentication Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/users` | Register a new user | No |
| `POST` | `/users/login` | Login (returns JWT token) | No |

## User Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/user` | Get current user | Required |
| `PUT` | `/user` | Update current user | Required |

## Profile Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/profiles/{username}` | Get a user profile | Optional |
| `POST` | `/profiles/{username}/follow` | Follow a user | Required |
| `DELETE` | `/profiles/{username}/follow` | Unfollow a user | Required |

## Article Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/articles` | List articles (with filters) | Optional |
| `GET` | `/articles/feed` | Get followed users' articles | Required |
| `GET` | `/articles/{slug}` | Get a single article | Optional |
| `POST` | `/articles` | Create an article | Required |
| `PUT` | `/articles/{slug}` | Update an article | Required |
| `DELETE` | `/articles/{slug}` | Delete an article | Required |

**Query parameters for `GET /articles`:**
- `tag` - Filter by tag
- `author` - Filter by author username
- `favorited` - Filter by favorited by username
- `offset` - Pagination offset (default: 0)
- `limit` - Pagination limit (default: 20)

## Comment Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/articles/{slug}/comments` | Get comments for an article | Optional |
| `POST` | `/articles/{slug}/comments` | Add a comment to an article | Required |
| `DELETE` | `/articles/{slug}/comments/{id}` | Delete a comment | Required |

## Tag Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/tags` | Get all tags | No |

## Favorite Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/articles/{slug}/favorite` | Favorite an article | Required |
| `DELETE` | `/articles/{slug}/favorite` | Unfavorite an article | Required |

---

# Security

Integration with Spring Security and add other filter for jwt token process.

The secret key is stored in `application.properties`.

**How authentication works:**

1. Users register or log in via `/users` or `/users/login` to receive a JWT token.
2. The token is passed in subsequent requests via the `Authorization` header: `Token <jwt-token>`.
3. `JwtTokenFilter` intercepts requests, validates the token using `DefaultJwtService` (backed by the [jjwt](https://github.com/jwtk/jjwt) library), and sets the authenticated user in the Spring Security context.
4. Controllers access the current user via Spring's `@AuthenticationPrincipal` annotation.

**Configuration** (in `application.properties`):
- `jwt.secret` - The HMAC signing key for JWT tokens
- `jwt.sessionTime` - Token expiration time in seconds (default: 86400 = 24 hours)

---

# Database

It uses a ~~H2 in-memory database~~ sqlite database (for easy local test without losing test data after every restart), can be changed easily in the `application.properties` for any other database.

## Database Schema

The application uses the following tables:

| Table | Description |
|-------|-------------|
| `users` | Stores user accounts (id, username, password, email, bio, image) |
| `articles` | Stores articles (id, user_id, slug, title, description, body, timestamps) |
| `article_favorites` | Many-to-many: users who favorited articles |
| `follows` | Many-to-many: user follow relationships |
| `tags` | Stores tag names |
| `article_tags` | Many-to-many: tags assigned to articles |
| `comments` | Stores comments (id, body, article_id, user_id, timestamps) |

## Migrations

Database migrations are managed by [Flyway](https://flywaydb.org/) and are located in:

```
src/main/resources/db/migration/
+-- V1__create_tables.sql
```

Flyway runs automatically on application startup, applying any pending migrations to the configured database.

**Test database:** Tests use an in-memory SQLite database (configured in `application-test.properties`) so that tests are isolated and do not affect the development database.

---

# Getting Started

## Prerequisites

- **Java 11** or higher installed
- **Gradle** (included via the Gradle Wrapper, no separate installation needed)

## Running Locally

```bash
./gradlew bootRun
```

To test that it works, open a browser tab at http://localhost:8080/tags .  
Alternatively, you can run

```bash
curl http://localhost:8080/tags
```

You should see a JSON response containing a list of tags:

```json
{"tags": []}
```

# Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

# Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

# Run test

The repository contains a lot of test cases to cover both api test and repository test.

```bash
./gradlew test
```

Test categories include:
- **API tests** - Integration tests for REST endpoints using Spring MockMvc and REST Assured
- **Repository tests** - Tests for MyBatis data access layer
- **Application service tests** - Tests for query and command services
- **Core tests** - Unit tests for domain entities and logic

Tests use the `application-test.properties` profile which configures an in-memory SQLite database to ensure test isolation.

# Code format

Use spotless for code format.

```bash
# Check formatting
./gradlew spotlessCheck

# Apply formatting
./gradlew spotlessJavaApply
```

The project uses [Google Java Format](https://github.com/google/google-java-format) via the [Spotless](https://github.com/diffplug/spotless) Gradle plugin. Generated code in the `build/` directory is excluded from formatting checks.

---

# Tech Stack

| Technology | Purpose |
|---|---|
| [Spring Boot 2.6](https://spring.io/projects/spring-boot) | Application framework |
| [Spring MVC](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html) | REST API layer |
| [Spring Security](https://spring.io/projects/spring-security) | Authentication & authorization |
| [MyBatis](https://mybatis.org/mybatis-3/) | SQL mapping / data persistence |
| [Netflix DGS Framework](https://netflix.github.io/dgs/) | GraphQL server |
| [SQLite](https://www.sqlite.org/) | Default database (easily swappable) |
| [Flyway](https://flywaydb.org/) | Database migrations |
| [jjwt](https://github.com/jwtk/jjwt) | JWT token generation & validation |
| [Joda-Time](https://www.joda.org/joda-time/) | Date/time handling |
| [Lombok](https://projectlombok.org/) | Boilerplate code reduction |
| [REST Assured](https://rest-assured.io/) | API testing |
| [JUnit 5](https://junit.org/junit5/) | Test framework |
| [Spotless](https://github.com/diffplug/spotless) + [Google Java Format](https://github.com/google/google-java-format) | Code formatting |

---

# Help

Please fork and PR to improve the project.
