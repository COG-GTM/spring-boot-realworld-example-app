# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with **Spring Boot + MyBatis** including CRUD operations, authentication, routing, pagination, and more.

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

---

## Table of Contents

- [GraphQL Support](#graphql-support)
- [How It Works](#how-it-works)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [Database](#database)
- [Getting Started](#getting-started)
- [Docker](#try-it-out-with-docker)
- [Using a RealWorld Frontend](#try-it-out-with-a-realworld-frontend)
- [Testing](#testing)
- [Code Formatting](#code-formatting)
- [Tech Stack](#tech-stack)
- [Contributing](#contributing)

---

## GraphQL Support

Following DDD (Domain-Driven Design) principles, REST and GraphQL are treated as adapter layers. The domain layer remains consistent regardless of which API style is used. This repository implements **both GraphQL and REST** simultaneously.

The GraphQL schema is defined in [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls) and the visualization looks like below:

![GraphQL Schema](graphql-schema.png)

This implementation uses the [Netflix DGS Framework](https://github.com/Netflix/dgs-framework), a GraphQL server framework for Spring Boot.

### GraphQL Operations

**Queries:**

| Query | Description |
|-------|-------------|
| `article(slug)` | Fetch a single article by slug |
| `articles(first, after, last, before, authoredBy, favoritedBy, withTag)` | List/filter articles with cursor-based pagination |
| `me` | Get the currently authenticated user |
| `feed(first, after, last, before)` | Get the authenticated user's article feed |
| `profile(username)` | Get a user's profile |
| `tags` | List all tags |

**Mutations:**

| Mutation | Description |
|----------|-------------|
| `createUser(input)` | Register a new user |
| `login(email, password)` | Authenticate a user |
| `updateUser(changes)` | Update user profile |
| `followUser(username)` / `unfollowUser(username)` | Follow/unfollow a user |
| `createArticle(input)` / `updateArticle(slug, changes)` / `deleteArticle(slug)` | Article CRUD |
| `favoriteArticle(slug)` / `unfavoriteArticle(slug)` | Favorite/unfavorite an article |
| `addComment(slug, body)` / `deleteComment(slug, id)` | Comment operations |

---

## How It Works

The application uses **Spring Boot** with **MyBatis** for persistence and follows several architectural patterns:

* **Domain-Driven Design (DDD)** — Separates business logic from infrastructure concerns.
* **Data Mapper Pattern** — Uses MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern, keeping domain objects independent of the database.
* **CQRS (Command Query Responsibility Segregation)** — Separates the [read model from the write model](https://martinfowler.com/bliki/CQRS.html) for cleaner separation of concerns.

---

## Project Structure

The codebase is organized into four main layers under `io.spring`:

```
src/main/java/io/spring/
├── api/                          # Web layer (REST controllers)
│   ├── ArticleApi.java           #   Single article operations (GET/PUT/DELETE /articles/{slug})
│   ├── ArticlesApi.java          #   Article listing & creation (GET/POST /articles)
│   ├── ArticleFavoriteApi.java   #   Favorite/unfavorite (POST/DELETE /articles/{slug}/favorite)
│   ├── CommentsApi.java          #   Comment CRUD (GET/POST/DELETE /articles/{slug}/comments)
│   ├── CurrentUserApi.java       #   Current user (GET/PUT /user)
│   ├── ProfileApi.java           #   User profiles & follow (GET/POST/DELETE /profiles/{username})
│   ├── TagsApi.java              #   Tags listing (GET /tags)
│   ├── UsersApi.java             #   Registration & login (POST /users, POST /users/login)
│   ├── exception/                #   Custom exception handlers & error resources
│   └── security/                 #   JWT filter & Spring Security configuration
│
├── application/                  # Application services (query/read side of CQRS)
│   ├── ArticleQueryService.java  #   Article read operations
│   ├── CommentQueryService.java  #   Comment read operations
│   ├── ProfileQueryService.java  #   Profile read operations
│   ├── TagsQueryService.java     #   Tags read operations
│   ├── UserQueryService.java     #   User read operations
│   ├── CursorPager.java          #   Cursor-based pagination support
│   ├── Page.java                 #   Offset-based pagination support
│   ├── article/                  #   Article command service & DTOs
│   ├── data/                     #   Data transfer objects (ArticleData, UserData, etc.)
│   └── user/                     #   User command service & DTOs
│
├── core/                         # Domain layer (entities, repositories, services)
│   ├── article/                  #   Article & Tag entities, ArticleRepository interface
│   ├── comment/                  #   Comment entity, CommentRepository interface
│   ├── favorite/                 #   ArticleFavorite entity, ArticleFavoriteRepository interface
│   ├── service/                  #   AuthorizationService, JwtService interfaces
│   └── user/                     #   User & FollowRelation entities, UserRepository interface
│
├── graphql/                      # GraphQL adapter layer (DGS datafetchers & mutations)
│   ├── ArticleDatafetcher.java   #   Article queries
│   ├── ArticleMutation.java      #   Article mutations
│   ├── CommentDatafetcher.java   #   Comment queries
│   ├── CommentMutation.java      #   Comment mutations
│   ├── MeDatafetcher.java        #   Current user query
│   ├── ProfileDatafetcher.java   #   Profile queries
│   ├── RelationMutation.java     #   Follow/unfollow mutations
│   ├── TagDatafetcher.java       #   Tags query
│   ├── UserMutation.java         #   User registration/login/update mutations
│   └── exception/                #   GraphQL-specific exception handling
│
├── infrastructure/               # Infrastructure implementations
│   ├── mybatis/                  #   MyBatis mappers, read services, type handlers
│   ├── repository/               #   MyBatis-backed repository implementations
│   └── service/                  #   JWT & encryption service implementations
│
├── JacksonCustomizations.java    # Custom Jackson serialization config
├── MyBatisConfig.java            # MyBatis configuration
├── RealWorldApplication.java     # Application entry point
└── Util.java                     # Utility methods
```

```
src/main/resources/
├── application.properties        # Main application configuration
├── application-test.properties   # Test-specific configuration
├── db/migration/                 # Flyway database migrations
│   └── V1__create_tables.sql     #   Initial schema (users, articles, comments, tags, etc.)
├── mapper/                       # MyBatis XML mapper files
└── schema/
    └── schema.graphqls           # GraphQL schema definition
```

---

## API Endpoints

### Authentication

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `POST` | `/users` | Register a new user | No |
| `POST` | `/users/login` | Login | No |

### User

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/user` | Get current user | Yes |
| `PUT` | `/user` | Update current user | Yes |

### Profiles

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/profiles/{username}` | Get a user's profile | No |
| `POST` | `/profiles/{username}/follow` | Follow a user | Yes |
| `DELETE` | `/profiles/{username}/follow` | Unfollow a user | Yes |

### Articles

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/articles` | List articles (with optional filters: `tag`, `author`, `favorited`, `offset`, `limit`) | No |
| `GET` | `/articles/feed` | Get articles from followed users | Yes |
| `POST` | `/articles` | Create an article | Yes |
| `GET` | `/articles/{slug}` | Get a single article | No |
| `PUT` | `/articles/{slug}` | Update an article | Yes |
| `DELETE` | `/articles/{slug}` | Delete an article | Yes |

### Favorites

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `POST` | `/articles/{slug}/favorite` | Favorite an article | Yes |
| `DELETE` | `/articles/{slug}/favorite` | Unfavorite an article | Yes |

### Comments

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/articles/{slug}/comments` | Get comments for an article | No |
| `POST` | `/articles/{slug}/comments` | Add a comment to an article | Yes |
| `DELETE` | `/articles/{slug}/comments/{id}` | Delete a comment | Yes |

### Tags

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/tags` | Get all tags | No |

---

## Security

The application integrates with **Spring Security** and uses **JWT (JSON Web Token)** authentication:

- **JWT Filter** (`JwtTokenFilter`) — Extracts and validates the JWT token from the `Authorization` header on each request.
- **Security Configuration** (`WebSecurityConfig`) — Configures endpoint access rules, disables CSRF (stateless API), and registers the JWT filter.
- **Token Generation** — Uses the [jjwt](https://github.com/jwtk/jjwt) library for token creation and validation.
- **Password Encoding** — User passwords are hashed before storage.

The JWT secret key and session duration are configured in `application.properties`:

```properties
jwt.secret=<your-secret-key>
jwt.sessionTime=86400
```

Authenticated endpoints require an `Authorization` header with the format:

```
Authorization: Token <jwt-token>
```

---

## Database

The application uses an **SQLite** database for easy local development without losing data after restarts. The database file (`dev.db`) is created in the project root directory at runtime.

### Schema

The database schema is managed by [Flyway](https://flywaydb.org/) and includes the following tables:

| Table | Description |
|-------|-------------|
| `users` | User accounts (username, email, password hash, bio, image) |
| `articles` | Published articles (slug, title, description, body, timestamps) |
| `article_favorites` | Many-to-many relationship between users and favorited articles |
| `follows` | User follow relationships |
| `tags` | Available tags |
| `article_tags` | Many-to-many relationship between articles and tags |
| `comments` | Article comments |

### Switching Databases

To use a different database, update `application.properties`:

```properties
spring.datasource.url=jdbc:sqlite:dev.db
spring.datasource.driver-class-name=org.sqlite.JDBC
```

Replace these with the appropriate JDBC URL and driver for your target database (e.g., PostgreSQL, MySQL, H2).

---

## Getting Started

### Prerequisites

- **Java 11** or higher

### Running the Application

```bash
./gradlew bootRun
```

The application starts on port **8080** by default.

### Verify It Works

Open a browser tab at http://localhost:8080/tags or run:

```bash
curl http://localhost:8080/tags
```

### Example: Register a User

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"user": {"username": "jacob", "email": "jake@jake.jake", "password": "jakejake"}}'
```

### Example: Login

```bash
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"user": {"email": "jake@jake.jake", "password": "jakejake"}}'
```

### Example: Create an Article (Authenticated)

```bash
curl -X POST http://localhost:8080/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Token <your-jwt-token>" \
  -d '{"article": {"title": "How to train your dragon", "description": "Ever wonder how?", "body": "It takes a Jacobian", "tagList": ["dragons", "training"]}}'
```

---

## Try it out with [Docker](https://www.docker.com/)

### Prerequisites

- Docker installed

### Build and Run

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be available at http://localhost:8081.

---

## Try it out with a RealWorld Frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

You can connect any [RealWorld frontend](https://github.com/gothinkster/realworld#frontends) to this backend by pointing the API base URL to `http://localhost:8080`.

---

## Testing

The repository contains comprehensive test coverage for both API endpoints and repository operations.

### Test Structure

```
src/test/java/io/spring/
├── api/                            # REST API integration tests
│   ├── ArticleApiTest.java         #   Single article endpoint tests
│   ├── ArticlesApiTest.java        #   Article creation tests
│   ├── ArticleFavoriteApiTest.java #   Favorite/unfavorite tests
│   ├── CommentsApiTest.java        #   Comment CRUD tests
│   ├── CurrentUserApiTest.java     #   Current user endpoint tests
│   ├── ListArticleApiTest.java     #   Article listing/filtering tests
│   ├── ProfileApiTest.java         #   Profile endpoint tests
│   ├── UsersApiTest.java           #   Registration & login tests
│   └── TestWithCurrentUser.java    #   Base class for authenticated tests
├── application/                    #   Application service tests
├── core/                           #   Domain model tests
├── infrastructure/                 #   Repository implementation tests
├── RealworldApplicationTests.java  #   Application context loading test
└── TestHelper.java                 #   Shared test utilities
```

### Running Tests

```bash
./gradlew test
```

The tests use [REST Assured](https://rest-assured.io/) with Spring MockMvc for API testing, providing readable and expressive test assertions.

---

## Code Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with [Google Java Format](https://github.com/google/google-java-format) for consistent code style.

### Check Formatting

```bash
./gradlew spotlessCheck
```

### Apply Formatting

```bash
./gradlew spotlessJavaApply
```

---

## Tech Stack

| Technology | Purpose |
|-----------|---------|
| [Spring Boot 2.6](https://spring.io/projects/spring-boot) | Application framework |
| [Spring Security](https://spring.io/projects/spring-security) | Authentication & authorization |
| [Spring HATEOAS](https://spring.io/projects/spring-hateoas) | Hypermedia support |
| [MyBatis](https://mybatis.org/mybatis-3/) | SQL mapper / persistence |
| [Netflix DGS Framework](https://netflix.github.io/dgs/) | GraphQL server |
| [Flyway](https://flywaydb.org/) | Database migration |
| [SQLite](https://www.sqlite.org/) | Default database |
| [jjwt](https://github.com/jwtk/jjwt) | JWT token generation & validation |
| [Lombok](https://projectlombok.org/) | Boilerplate code reduction |
| [Joda-Time](https://www.joda.org/joda-time/) | Date/time handling |
| [REST Assured](https://rest-assured.io/) | API testing |
| [Spotless](https://github.com/diffplug/spotless) | Code formatting |
| [JUnit 5](https://junit.org/junit5/) | Testing framework |

---

## Contributing

Please fork and PR to improve the project.
