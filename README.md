# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

---

## Table of Contents

- [GraphQL Support](#graphql-support)
- [How it Works](#how-it-works)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [Database](#database)
- [Getting Started](#getting-started)
- [Try it out with Docker](#try-it-out-with-docker)
- [Try it out with a RealWorld Frontend](#try-it-out-with-a-realworld-frontend)
- [Running Tests](#running-tests)
- [Code Format](#code-format)
- [Tech Stack](#tech-stack)
- [Help](#help)

---

## GraphQL Support

Following some DDD principles, REST or GraphQL is just a kind of adapter. The domain layer remains consistent regardless of which API style is used. This repository implements both GraphQL and REST simultaneously.

The GraphQL schema is defined in [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls) and the visualization looks like below.

![](graphql-schema.png)

This implementation uses [dgs-framework](https://github.com/Netflix/dgs-framework), a Netflix-developed Java GraphQL server framework built on top of Spring Boot.

### GraphQL Queries

| Query | Description |
|-------|-------------|
| `article(slug)` | Fetch a single article by slug |
| `articles(first, after, last, before, authoredBy, favoritedBy, withTag)` | List/filter articles with cursor-based pagination |
| `me` | Get the currently authenticated user |
| `feed(first, after, last, before)` | Get the article feed for the authenticated user |
| `profile(username)` | Get a user profile |
| `tags` | List all tags |

### GraphQL Mutations

| Mutation | Description |
|----------|-------------|
| `createUser(input)` | Register a new user |
| `login(email, password)` | Authenticate and receive a token |
| `updateUser(changes)` | Update the current user's profile |
| `followUser(username)` / `unfollowUser(username)` | Follow or unfollow a user |
| `createArticle(input)` / `updateArticle(slug, changes)` / `deleteArticle(slug)` | Article CRUD operations |
| `favoriteArticle(slug)` / `unfavoriteArticle(slug)` | Favorite or unfavorite an article |
| `addComment(slug, body)` / `deleteComment(slug, id)` | Comment operations |

---

## How it Works

The application uses Spring Boot (Web, MyBatis).

* Use the idea of Domain Driven Design to separate the business term and infrastructure term.
* Use MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* Use [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern to separate the read model and write model.

---

## Project Structure

The code is organized into four main layers:

```
src/main/java/io/spring/
├── api/                          # Web layer (Spring MVC REST controllers)
│   ├── ArticleApi.java           #   Single article operations (GET/PUT/DELETE /articles/{slug})
│   ├── ArticlesApi.java          #   Article listing & creation (GET/POST /articles)
│   ├── ArticleFavoriteApi.java   #   Favorite/unfavorite (POST/DELETE /articles/{slug}/favorite)
│   ├── CommentsApi.java          #   Comment CRUD (GET/POST/DELETE /articles/{slug}/comments)
│   ├── CurrentUserApi.java       #   Current user operations (GET/PUT /user)
│   ├── ProfileApi.java           #   User profiles & follow (GET/POST/DELETE /profiles/{username})
│   ├── TagsApi.java              #   Tag listing (GET /tags)
│   ├── UsersApi.java             #   Registration & login (POST /users, /users/login)
│   ├── exception/                #   Custom exception handlers and error resources
│   └── security/                 #   JWT filter and Spring Security configuration
│
├── application/                  # Application services (query services, DTOs, commands)
│   ├── ArticleQueryService.java  #   Read-side article queries (CQRS read model)
│   ├── CommentQueryService.java  #   Read-side comment queries
│   ├── ProfileQueryService.java  #   Read-side profile queries
│   ├── TagsQueryService.java     #   Read-side tag queries
│   ├── UserQueryService.java     #   Read-side user queries
│   ├── Page.java                 #   Offset-based pagination
│   ├── CursorPager.java          #   Cursor-based pagination (for GraphQL)
│   ├── article/                  #   Article command service, params, validators
│   ├── data/                     #   Data transfer objects (ArticleData, UserData, etc.)
│   └── user/                     #   User command service, params, validators
│
├── core/                         # Domain model (entities, repository interfaces, services)
│   ├── article/                  #   Article & Tag entities, ArticleRepository interface
│   ├── comment/                  #   Comment entity, CommentRepository interface
│   ├── favorite/                 #   ArticleFavorite entity, ArticleFavoriteRepository interface
│   ├── user/                     #   User & FollowRelation entities, UserRepository interface
│   └── service/                  #   Domain services (AuthorizationService, JwtService interface)
│
├── graphql/                      # GraphQL layer (DGS data fetchers and mutations)
│   ├── ArticleDatafetcher.java   #   Article-related GraphQL resolvers
│   ├── ArticleMutation.java      #   Article-related GraphQL mutations
│   ├── CommentDatafetcher.java   #   Comment-related GraphQL resolvers
│   ├── CommentMutation.java      #   Comment-related GraphQL mutations
│   ├── MeDatafetcher.java        #   Current user GraphQL resolver
│   ├── ProfileDatafetcher.java   #   Profile-related GraphQL resolvers
│   ├── RelationMutation.java     #   Follow/unfollow GraphQL mutations
│   ├── TagDatafetcher.java       #   Tag-related GraphQL resolvers
│   ├── UserMutation.java         #   User registration/login GraphQL mutations
│   ├── SecurityUtil.java         #   GraphQL authentication utilities
│   └── exception/                #   GraphQL-specific exception handling
│
├── infrastructure/               # Infrastructure implementations (technical details)
│   ├── mybatis/
│   │   ├── DateTimeHandler.java  #   MyBatis type handler for Joda DateTime
│   │   ├── mapper/               #   MyBatis mapper interfaces (ArticleMapper, UserMapper, etc.)
│   │   └── readservice/          #   MyBatis read service implementations (CQRS read side)
│   ├── repository/               #   MyBatis repository implementations
│   └── service/                  #   Infrastructure service implementations (DefaultJwtService)
│
├── JacksonCustomizations.java    # Jackson serialization configuration
├── MyBatisConfig.java            # MyBatis configuration
├── RealWorldApplication.java     # Spring Boot application entry point
└── Util.java                     # Shared utility methods
```

### Key Design Decisions

- **CQRS Pattern**: Write operations go through domain entities and repository interfaces in `core/`, while read operations use dedicated query services in `application/` backed by MyBatis read services in `infrastructure/`. This separates the command (write) model from the query (read) model.
- **Domain Driven Design**: The `core/` package contains pure domain logic with no framework dependencies. Repository interfaces are defined in the domain layer and implemented in `infrastructure/`.
- **Dual API Support**: Both REST (`api/`) and GraphQL (`graphql/`) layers delegate to the same `application/` and `core/` layers, demonstrating that the transport layer is simply an adapter.
- **Data Mapper Pattern**: MyBatis XML mappers (in `src/main/resources/mapper/`) handle the mapping between domain objects and database rows, keeping the domain model free of persistence annotations.

---

## API Endpoints

### Authentication

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `POST` | `/users` | Register a new user | No |
| `POST` | `/users/login` | Login (returns JWT) | No |

### User

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/user` | Get current user | Yes |
| `PUT` | `/user` | Update current user | Yes |

### Profiles

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/profiles/:username` | Get a user profile | No |
| `POST` | `/profiles/:username/follow` | Follow a user | Yes |
| `DELETE` | `/profiles/:username/follow` | Unfollow a user | Yes |

### Articles

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/articles` | List articles (with optional filters) | No |
| `GET` | `/articles/feed` | Get articles from followed users | Yes |
| `GET` | `/articles/:slug` | Get a single article | No |
| `POST` | `/articles` | Create an article | Yes |
| `PUT` | `/articles/:slug` | Update an article | Yes |
| `DELETE` | `/articles/:slug` | Delete an article | Yes |

### Favorites

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `POST` | `/articles/:slug/favorite` | Favorite an article | Yes |
| `DELETE` | `/articles/:slug/favorite` | Unfavorite an article | Yes |

### Comments

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/articles/:slug/comments` | Get comments for an article | No |
| `POST` | `/articles/:slug/comments` | Add a comment to an article | Yes |
| `DELETE` | `/articles/:slug/comments/:id` | Delete a comment | Yes |

### Tags

| Method | URL | Description | Auth Required |
|--------|-----|-------------|:---:|
| `GET` | `/tags` | Get all tags | No |

### GraphQL

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/graphql` | GraphQL endpoint (queries and mutations) |

---

## Security

Integration with Spring Security with a custom JWT token filter for authentication.

- **Password hashing**: Passwords are hashed using BCrypt via Spring Security's `BCryptPasswordEncoder`.
- **JWT tokens**: Tokens are signed with the HS512 algorithm using the `jjwt` library. The secret key and session duration are configured in `application.properties`.
- **Stateless sessions**: The API is fully stateless; no server-side sessions are used.
- **CORS**: Cross-Origin Resource Sharing is configured to allow all origins with common HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`).
- **Public endpoints**: Registration, login, article listing, profile viewing, and tag listing are publicly accessible. All other endpoints require a valid JWT token in the `Authorization` header using the `Token` scheme (e.g., `Authorization: Token <jwt>`).

---

## Database

The application uses a SQLite database (file: `dev.db`) for easy local development without losing data between restarts. The database is managed using [Flyway](https://flywaydb.org/) for schema migrations.

### Schema

The database consists of the following tables:

| Table | Description |
|-------|-------------|
| `users` | User accounts (id, username, email, password, bio, image) |
| `articles` | Published articles (id, user_id, slug, title, description, body, timestamps) |
| `article_favorites` | Many-to-many relationship between users and favorited articles |
| `follows` | User follow relationships |
| `tags` | Tag definitions |
| `article_tags` | Many-to-many relationship between articles and tags |
| `comments` | Article comments (id, body, article_id, user_id, timestamps) |

### Switching Databases

You can switch to a different database by updating `application.properties`:

```properties
# Example: Switch to MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/realworld
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=yourpassword
```

You will also need to add the appropriate JDBC driver dependency to `build.gradle`.

---

## Getting Started

### Prerequisites

- Java 11 or higher

### Running the Application

```bash
./gradlew bootRun
```

To verify it works, open a browser tab at http://localhost:8080/tags or run:

```bash
curl http://localhost:8080/tags
```

### Example API Usage

**Register a new user:**

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"user": {"username": "jacob", "email": "jake@jake.jake", "password": "jakejake"}}'
```

**Login:**

```bash
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"user": {"email": "jake@jake.jake", "password": "jakejake"}}'
```

**Create an article (with token from login response):**

```bash
curl -X POST http://localhost:8080/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Token <your-jwt-token>" \
  -d '{"article": {"title": "How to train your dragon", "description": "Ever wonder how?", "body": "You have to believe", "tagList": ["reactjs", "angularjs", "dragons"]}}'
```

**List articles:**

```bash
curl http://localhost:8080/articles
```

**List articles with filters:**

```bash
# Filter by tag
curl "http://localhost:8080/articles?tag=dragons"

# Filter by author
curl "http://localhost:8080/articles?author=jacob"

# Pagination
curl "http://localhost:8080/articles?limit=10&offset=0"
```

---

## Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The API will be available at http://localhost:8081.

---

## Try it out with a RealWorld Frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

---

## Running Tests

The repository contains a comprehensive test suite covering both API integration tests and repository-level tests.

```bash
./gradlew test
```

---

## Code Format

Use [Spotless](https://github.com/diffplug/spotless) with [Google Java Format](https://github.com/google/google-java-format) for consistent code formatting.

**Check formatting:**

```bash
./gradlew spotlessCheck
```

**Apply formatting:**

```bash
./gradlew spotlessJavaApply
```

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| [Spring Boot 2.6](https://spring.io/projects/spring-boot) | Application framework |
| [Spring Security](https://spring.io/projects/spring-security) | Authentication and authorization |
| [MyBatis](https://mybatis.org/mybatis-3/) | SQL mapping and persistence (Data Mapper pattern) |
| [DGS Framework](https://netflix.github.io/dgs/) | Netflix GraphQL server framework |
| [Flyway](https://flywaydb.org/) | Database migration management |
| [SQLite](https://www.sqlite.org/) | Default embedded database |
| [JJWT](https://github.com/jwtk/jjwt) | JSON Web Token creation and validation |
| [Joda-Time](https://www.joda.org/joda-time/) | Date and time handling |
| [Lombok](https://projectlombok.org/) | Boilerplate code reduction |
| [REST Assured](https://rest-assured.io/) | REST API testing |
| [JUnit 5](https://junit.org/junit5/) | Testing framework |
| [Spotless](https://github.com/diffplug/spotless) | Code formatting (Google Java Format) |

---

## Help

Please fork and PR to improve the project.
