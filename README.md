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
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Docker](#try-it-out-with-docker)
- [Using a RealWorld Frontend](#try-it-out-with-a-realworld-frontend)
- [REST API Endpoints](#rest-api-endpoints)
- [GraphQL API](#graphql-api)
- [Security](#security)
- [Database](#database)
- [Testing](#testing)
- [Code Format](#code-format)
- [Contributing](#contributing)
- [License](#license)

---

## GraphQL Support

Following DDD (Domain-Driven Design) principles, REST and GraphQL are treated as interchangeable adapters over a consistent domain layer. This repository implements **both GraphQL and REST** simultaneously.

The GraphQL schema is defined at [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls) and the visualization looks like below:

![GraphQL Schema Visualization](graphql-schema.png)

This implementation uses [Netflix DGS Framework](https://github.com/Netflix/dgs-framework), a GraphQL server framework for Spring Boot.

---

## How It Works

The application uses **Spring Boot** with the following design principles:

* **Domain-Driven Design (DDD)** — Separates business logic from infrastructure concerns.
* **[Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern** — Implemented via MyBatis for persistence, keeping domain objects decoupled from the database.
* **[CQRS](https://martinfowler.com/bliki/CQRS.html) (Command Query Responsibility Segregation)** — Read and write models are separated for clarity and scalability.

---

## Project Structure

```
src/main/java/io/spring/
├── api/                        # Web layer (Spring MVC REST controllers)
│   ├── ArticleApi.java         #   Single article CRUD operations
│   ├── ArticlesApi.java        #   Article listing, feed, and creation
│   ├── ArticleFavoriteApi.java #   Favorite/unfavorite articles
│   ├── CommentsApi.java        #   Comment CRUD on articles
│   ├── CurrentUserApi.java     #   Get/update the authenticated user
│   ├── ProfileApi.java         #   User profile & follow/unfollow
│   ├── TagsApi.java            #   List all tags
│   ├── UsersApi.java           #   User registration & login
│   ├── exception/              #   Custom exception handlers & error resources
│   └── security/               #   JWT filter & Spring Security configuration
├── application/                # High-level query services and DTOs
│   ├── ArticleQueryService.java
│   ├── CommentQueryService.java
│   ├── ProfileQueryService.java
│   ├── TagsQueryService.java
│   ├── UserQueryService.java
│   ├── article/                #   Article command service & params
│   ├── data/                   #   Data transfer objects (UserData, ArticleData, etc.)
│   └── user/                   #   User command service & params
├── core/                       # Domain model (entities, repositories, services)
│   ├── article/                #   Article & Tag entities, ArticleRepository
│   ├── comment/                #   Comment entity, CommentRepository
│   ├── favorite/               #   ArticleFavorite entity & repository
│   ├── service/                #   AuthorizationService, JwtService interfaces
│   └── user/                   #   User & FollowRelation entities, UserRepository
├── graphql/                    # GraphQL layer (Netflix DGS datafetchers & mutations)
│   ├── ArticleDatafetcher.java
│   ├── ArticleMutation.java
│   ├── CommentDatafetcher.java
│   ├── CommentMutation.java
│   ├── MeDatafetcher.java
│   ├── ProfileDatafetcher.java
│   ├── RelationMutation.java
│   ├── TagDatafetcher.java
│   ├── UserMutation.java
│   └── exception/              #   GraphQL-specific error handling
├── infrastructure/             # Technical implementation details
│   ├── mybatis/                #   MyBatis mappers, read services, type handlers
│   ├── repository/             #   MyBatis-backed repository implementations
│   └── service/                #   DefaultJwtService (JJWT-based)
├── JacksonCustomizations.java  # Custom Jackson serialization config
├── MyBatisConfig.java          # MyBatis configuration
├── RealWorldApplication.java   # Spring Boot application entry point
└── Util.java                   # Shared utility methods

src/main/resources/
├── application.properties      # Main application configuration
├── application-test.properties # Test profile configuration
├── db/migration/               # Flyway database migration scripts
│   └── V1__create_tables.sql
├── mapper/                     # MyBatis XML mapper files
│   ├── ArticleMapper.xml
│   ├── ArticleReadService.xml
│   ├── CommentMapper.xml
│   ├── UserMapper.xml
│   └── ...
└── schema/
    └── schema.graphqls         # GraphQL schema definition

src/test/java/io/spring/        # Test suite (API & repository tests)
```

### Layer Responsibilities

| Layer | Package | Description |
|-------|---------|-------------|
| **Web (API)** | `io.spring.api` | REST controllers built with Spring MVC. Handles HTTP requests, input validation, and response formatting. |
| **Web (GraphQL)** | `io.spring.graphql` | GraphQL datafetchers and mutations built with Netflix DGS. |
| **Application** | `io.spring.application` | Query services (read-side of CQRS), command services (write-side), and DTOs. Orchestrates use cases. |
| **Core (Domain)** | `io.spring.core` | Pure domain model — entities (`Article`, `User`, `Comment`, `Tag`), repository interfaces, and domain service interfaces. No framework dependencies. |
| **Infrastructure** | `io.spring.infrastructure` | Concrete implementations of repository and service interfaces using MyBatis, JJWT, etc. |

---

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Framework | Spring Boot | 2.6.3 |
| Language | Java | 11 |
| Build Tool | Gradle | 7.x |
| ORM / Data Mapper | MyBatis | 2.2.2 |
| Database | SQLite | 3.36.0 |
| Database Migration | Flyway | (managed by Spring Boot) |
| Authentication | Spring Security + JWT (JJWT) | 0.11.2 |
| GraphQL | Netflix DGS Framework | 4.9.21 |
| Code Formatting | Spotless (Google Java Format) | 6.2.1 |
| Date/Time | Joda-Time | 2.10.13 |
| Testing | JUnit 5, REST Assured, Spring MockMvc | — |
| Boilerplate Reduction | Lombok | (managed by Spring Boot) |

---

## Prerequisites

- **Java 11** (JDK) — [Download](https://adoptium.net/)
- **Docker** (optional) — only needed if you want to run via container

---

## Getting Started

1. **Clone the repository:**

    ```bash
    git clone https://github.com/COG-GTM/spring-boot-realworld-example-app.git
    cd spring-boot-realworld-example-app
    ```

2. **Run the application:**

    ```bash
    ./gradlew bootRun
    ```

3. **Verify it is running:**

    Open a browser at http://localhost:8080/tags or run:

    ```bash
    curl http://localhost:8080/tags
    ```

    You should see a JSON response with a `tags` key.

---

## Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.

```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be accessible at http://localhost:8081.

---

## Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

You can pair this backend with any [RealWorld frontend](https://codebase.show/projects/realworld) (React, Angular, Vue, etc.) by pointing the frontend's API base URL to `http://localhost:8080`.

---

## REST API Endpoints

All endpoints follow the [RealWorld API Spec](https://realworld-docs.netlify.app/specifications/backend/endpoints/).

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/users` | Register a new user | No |
| `POST` | `/users/login` | Login (returns JWT token) | No |

### User

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/user` | Get current user | Yes |
| `PUT` | `/user` | Update current user | Yes |

### Profiles

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/profiles/:username` | Get a user profile | Optional |
| `POST` | `/profiles/:username/follow` | Follow a user | Yes |
| `DELETE` | `/profiles/:username/follow` | Unfollow a user | Yes |

### Articles

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/articles` | List articles (filterable) | Optional |
| `GET` | `/articles/feed` | Get feed (followed authors) | Yes |
| `GET` | `/articles/:slug` | Get a single article | Optional |
| `POST` | `/articles` | Create an article | Yes |
| `PUT` | `/articles/:slug` | Update an article | Yes |
| `DELETE` | `/articles/:slug` | Delete an article | Yes |

**Query parameters for `GET /articles`:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `tag` | String | Filter by tag |
| `author` | String | Filter by author username |
| `favorited` | String | Filter by user who favorited |
| `limit` | Integer | Number of results (default: 20) |
| `offset` | Integer | Offset for pagination (default: 0) |

### Favorites

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/articles/:slug/favorite` | Favorite an article | Yes |
| `DELETE` | `/articles/:slug/favorite` | Unfavorite an article | Yes |

### Comments

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/articles/:slug/comments` | Get comments for an article | Optional |
| `POST` | `/articles/:slug/comments` | Add a comment to an article | Yes |
| `DELETE` | `/articles/:slug/comments/:id` | Delete a comment | Yes |

### Tags

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/tags` | Get all tags | No |

### Example: Register and Create an Article

```bash
# Register a new user
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"user":{"username":"jake","email":"jake@example.com","password":"jakejake"}}'

# Use the returned token to create an article
curl -X POST http://localhost:8080/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Token <your-jwt-token>" \
  -d '{"article":{"title":"How to train your dragon","description":"Ever wonder how?","body":"You have to believe","tagList":["reactjs","angularjs","dragons"]}}'
```

---

## GraphQL API

The GraphQL endpoint is available at `/graphql`. An interactive GraphiQL playground is available at `/graphiql`.

### Queries

| Query | Description |
|-------|-------------|
| `article(slug: String!)` | Fetch a single article by slug |
| `articles(first, after, last, before, authoredBy, favoritedBy, withTag)` | List/filter articles with cursor-based pagination |
| `feed(first, after, last, before)` | Get the authenticated user's feed |
| `me` | Get the current authenticated user |
| `profile(username: String!)` | Get a user profile |
| `tags` | Get all tags |

### Mutations

| Mutation | Description |
|----------|-------------|
| `createUser(input: CreateUserInput)` | Register a new user |
| `login(email, password)` | Authenticate and get a token |
| `updateUser(changes: UpdateUserInput!)` | Update the current user |
| `followUser(username)` / `unfollowUser(username)` | Follow/unfollow a user |
| `createArticle(input: CreateArticleInput!)` | Create an article |
| `updateArticle(slug, changes: UpdateArticleInput!)` | Update an article |
| `deleteArticle(slug)` | Delete an article |
| `favoriteArticle(slug)` / `unfavoriteArticle(slug)` | Favorite/unfavorite |
| `addComment(slug, body)` | Add a comment |
| `deleteComment(slug, id)` | Delete a comment |

### Example GraphQL Query

```graphql
query {
  articles(first: 10, withTag: "dragons") {
    edges {
      node {
        title
        slug
        description
        author {
          username
        }
        tagList
        favoritesCount
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

---

## Security

The application integrates with **Spring Security** using a custom JWT token filter.

- **Stateless sessions** — No server-side session is stored; all authentication state is carried in the JWT.
- **JWT generation and validation** — Handled by the [JJWT](https://github.com/jwtk/jjwt) library.
- **Password encoding** — Uses BCrypt via Spring Security's `BCryptPasswordEncoder`.
- **Token format** — Sent via the `Authorization` header as `Token <jwt>`.
- **Public endpoints** — Registration (`POST /users`), login (`POST /users/login`), and all `GET` requests to `/articles/**`, `/profiles/**`, `/tags`, and `/graphql` are accessible without authentication.
- **CORS** — Configured to allow all origins with `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, and `HEAD` methods.

The JWT secret key and session duration are configured in `application.properties`:

```properties
jwt.secret=nRvyYC4soFxBdZ-F-5Nnzz5USXstR1YylsTd-mA0aKtI9HUlriGrtkf-TiuDapkLiUCogO3JOK7kwZisrHp6wA
jwt.sessionTime=86400
```

> **Note:** For production use, you should rotate this secret and load it from an environment variable or secrets manager.

---

## Database

The application uses an **SQLite** database file (`dev.db`) for easy local development without losing data between restarts.

### Schema

The database schema is managed by **Flyway** migrations located at `src/main/resources/db/migration/`. The initial migration (`V1__create_tables.sql`) creates the following tables:

| Table | Description |
|-------|-------------|
| `users` | User accounts (id, username, email, password, bio, image) |
| `articles` | Published articles (id, user_id, slug, title, description, body, timestamps) |
| `article_favorites` | Many-to-many: users who favorited an article |
| `follows` | Many-to-many: user follow relationships |
| `tags` | Tag definitions |
| `article_tags` | Many-to-many: tags associated with articles |
| `comments` | Comments on articles (id, body, article_id, user_id, timestamps) |

### Changing the Database

To switch to a different database (e.g., PostgreSQL, MySQL), update the datasource configuration in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/realworld
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=your_username
spring.datasource.password=your_password
```

You will also need to add the appropriate JDBC driver dependency to `build.gradle`.

### Cleaning the Database

Running `./gradlew clean` will delete the `dev.db` file, giving you a fresh database on the next startup.

---

## Testing

The repository includes a comprehensive test suite covering both API-level integration tests and repository-level data access tests.

```bash
# Run all tests
./gradlew test
```

Tests use:
- **JUnit 5** as the test framework
- **REST Assured** + **Spring MockMvc** for API endpoint testing
- **MyBatis Spring Boot Starter Test** for mapper/repository testing
- An **in-memory test profile** (`application-test.properties`) for test isolation

---

## Code Format

Code formatting is enforced using [Spotless](https://github.com/diffplug/spotless) with the [Google Java Format](https://github.com/google/google-java-format) style.

```bash
# Check formatting (fails if code is not formatted)
./gradlew spotlessCheck

# Auto-format all Java files
./gradlew spotlessJavaApply
```

---

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Ensure your code is formatted (`./gradlew spotlessJavaApply`)
4. Run the tests (`./gradlew test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
