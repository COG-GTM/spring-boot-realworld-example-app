# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with **Spring Boot + MyBatis** including CRUD operations, authentication, routing, pagination, and more.

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

---

## Table of Contents

- [Features](#features)
- [GraphQL Support](#graphql-support)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [REST API Endpoints](#rest-api-endpoints)
- [GraphQL API](#graphql-api)
- [Authentication & Security](#authentication--security)
- [Database](#database)
- [Running with Docker](#running-with-docker)
- [Using with a RealWorld Frontend](#using-with-a-realworld-frontend)
- [Testing](#testing)
- [Code Formatting](#code-formatting)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- Full **REST API** adhering to the [RealWorld](https://github.com/gothinkster/realworld) spec
- **GraphQL API** running side-by-side with REST on the same domain layer
- **JWT-based authentication** (stateless, no session)
- CRUD for **Articles**, **Comments**, **Tags**, **Users**, and **Profiles**
- Article **favoriting** and user **following**
- Article **feed** (personalized timeline of followed authors)
- **Pagination** support for article listings
- **CQRS** (Command Query Responsibility Segregation) pattern
- **Domain-Driven Design** (DDD) architecture
- Database migrations with **Flyway**
- Code formatting with **Spotless** (Google Java Format)

---

## GraphQL Support

Following DDD principles, REST and GraphQL are simply different adapters over the same consistent domain layer. This repository implements both GraphQL and REST simultaneously.

The GraphQL schema is defined in [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls) and the visualization looks like this:

![GraphQL Schema](graphql-schema.png)

This implementation uses [Netflix DGS Framework](https://github.com/Netflix/dgs-framework), a GraphQL server framework for Spring Boot built by Netflix.

**GraphQL endpoints:**

| Endpoint     | Description                    |
|--------------|--------------------------------|
| `/graphql`   | GraphQL query/mutation handler |
| `/graphiql`  | Interactive GraphQL IDE        |

---

## How It Works

The application uses **Spring Boot** with the following key design principles:

- **Domain-Driven Design (DDD)** to cleanly separate business logic from infrastructure concerns.
- **[Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern** via MyBatis for persistence, keeping domain objects free of persistence logic.
- **[CQRS](https://martinfowler.com/bliki/CQRS.html) pattern** to separate the read model (queries) from the write model (commands), improving clarity and scalability.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Clients                           │
│            (REST / GraphQL / Frontend)              │
└──────────────┬──────────────────┬───────────────────┘
               │                  │
       ┌───────▼───────┐  ┌──────▼────────┐
       │   REST API    │  │  GraphQL API  │
       │ (Spring MVC)  │  │ (Netflix DGS) │
       └───────┬───────┘  └──────┬────────┘
               │                  │
       ┌───────▼──────────────────▼───────┐
       │        Application Layer         │
       │  (Query Services, Command Svc)   │
       └───────────────┬─────────────────┘
                       │
       ┌───────────────▼─────────────────┐
       │          Core / Domain          │
       │  (Entities, Repositories,       │
       │   Services, Business Rules)     │
       └───────────────┬─────────────────┘
                       │
       ┌───────────────▼─────────────────┐
       │       Infrastructure            │
       │  (MyBatis Mappers, Repository   │
       │   Impls, JWT Service)           │
       └───────────────┬─────────────────┘
                       │
               ┌───────▼───────┐
               │   SQLite DB   │
               └───────────────┘
```

---

## Project Structure

```
src/main/java/io/spring/
├── api/                          # Web layer (REST controllers)
│   ├── ArticlesApi.java          #   POST /articles, GET /articles, GET /articles/feed
│   ├── ArticleApi.java           #   GET/PUT/DELETE /articles/{slug}
│   ├── ArticleFavoriteApi.java   #   POST/DELETE /articles/{slug}/favorite
│   ├── CommentsApi.java          #   POST/GET/DELETE /articles/{slug}/comments
│   ├── CurrentUserApi.java       #   GET/PUT /user
│   ├── UsersApi.java             #   POST /users, POST /users/login
│   ├── ProfileApi.java           #   GET /profiles/{username}, follow/unfollow
│   ├── TagsApi.java              #   GET /tags
│   ├── exception/                #   Custom exception handlers & error resources
│   └── security/                 #   JWT filter & Spring Security config
│
├── application/                  # Application services (CQRS query side)
│   ├── ArticleQueryService.java  #   Read-side queries for articles
│   ├── CommentQueryService.java  #   Read-side queries for comments
│   ├── ProfileQueryService.java  #   Read-side queries for profiles
│   ├── TagsQueryService.java     #   Read-side queries for tags
│   ├── UserQueryService.java     #   Read-side queries for users
│   ├── article/                  #   Article command service & params
│   ├── user/                     #   User service, registration & update params
│   └── data/                     #   Data transfer objects (DTOs)
│
├── core/                         # Domain layer (entities & repository interfaces)
│   ├── article/                  #   Article, Tag entities & ArticleRepository
│   ├── comment/                  #   Comment entity & CommentRepository
│   ├── favorite/                 #   ArticleFavorite entity & repository
│   ├── user/                     #   User, FollowRelation & UserRepository
│   └── service/                  #   JwtService, AuthorizationService
│
├── graphql/                      # GraphQL layer (DGS datafetchers & mutations)
│   ├── ArticleDatafetcher.java
│   ├── ArticleMutation.java
│   ├── CommentDatafetcher.java
│   ├── CommentMutation.java
│   ├── ProfileDatafetcher.java
│   ├── RelationMutation.java
│   ├── UserMutation.java
│   ├── TagDatafetcher.java
│   ├── MeDatafetcher.java
│   ├── SecurityUtil.java
│   └── exception/                #   GraphQL-specific exception handling
│
└── infrastructure/               # Infrastructure / implementation details
    ├── mybatis/                   #   MyBatis type handlers
    │   ├── mapper/               #   MyBatis mapper interfaces
    │   └── readservice/          #   Read-side MyBatis query services
    ├── repository/               #   MyBatis-backed repository implementations
    └── service/                  #   DefaultJwtService implementation
```

```
src/main/resources/
├── application.properties         # Main configuration (datasource, JWT, MyBatis)
├── application-test.properties    # Test configuration (in-memory SQLite)
├── db/migration/
│   └── V1__create_tables.sql      # Flyway database migration
├── mapper/                        # MyBatis XML mapper files
│   ├── ArticleMapper.xml
│   ├── ArticleReadService.xml
│   ├── CommentMapper.xml
│   ├── CommentReadService.xml
│   ├── UserMapper.xml
│   ├── UserReadService.xml
│   └── ...
└── schema/
    └── schema.graphqls            # GraphQL schema definition
```

---

## Tech Stack

| Technology                                                                 | Purpose                         |
|----------------------------------------------------------------------------|---------------------------------|
| [Spring Boot 2.6](https://spring.io/projects/spring-boot)                 | Application framework           |
| [Spring Security](https://spring.io/projects/spring-security)             | Authentication & authorization  |
| [Spring MVC](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html) | REST API controllers |
| [MyBatis](https://mybatis.org/mybatis-3/)                                  | SQL mapping / Data Mapper ORM   |
| [Netflix DGS Framework](https://netflix.github.io/dgs/)                   | GraphQL server                  |
| [SQLite](https://www.sqlite.org/)                                          | Embedded database               |
| [Flyway](https://flywaydb.org/)                                           | Database migrations             |
| [JJWT](https://github.com/jwtk/jjwt)                                      | JWT token generation & parsing  |
| [Lombok](https://projectlombok.org/)                                      | Boilerplate code reduction      |
| [JUnit 5](https://junit.org/junit5/)                                      | Testing framework               |
| [REST Assured](https://rest-assured.io/)                                   | REST API testing                |
| [Spotless](https://github.com/diffplug/spotless)                          | Code formatting (Google Java)   |
| [Joda-Time](https://www.joda.org/joda-time/)                              | Date/time handling              |
| Java 11                                                                    | Language runtime                |

---

## Getting Started

### Prerequisites

- **Java 11** (JDK) installed and on your `PATH`

### Run the Application

```bash
./gradlew bootRun
```

The server will start on **http://localhost:8080**.

### Verify It Works

Open a browser tab at http://localhost:8080/tags, or run:

```bash
curl http://localhost:8080/tags
```

You should see a JSON response with a `tags` array.

---

## REST API Endpoints

All endpoints follow the [RealWorld API Spec](https://realworld-docs.netlify.app/specifications/backend/endpoints/).

### Authentication

| Method | Endpoint          | Description          | Auth Required |
|--------|-------------------|----------------------|:------------:|
| POST   | `/users`          | Register a new user  | No           |
| POST   | `/users/login`    | Login (get JWT)      | No           |

### User

| Method | Endpoint | Description            | Auth Required |
|--------|----------|------------------------|:------------:|
| GET    | `/user`  | Get current user       | Yes          |
| PUT    | `/user`  | Update current user    | Yes          |

### Profiles

| Method | Endpoint                        | Description       | Auth Required |
|--------|---------------------------------|-------------------|:------------:|
| GET    | `/profiles/{username}`          | Get a profile     | No           |
| POST   | `/profiles/{username}/follow`   | Follow a user     | Yes          |
| DELETE | `/profiles/{username}/follow`   | Unfollow a user   | Yes          |

### Articles

| Method | Endpoint                            | Description                    | Auth Required |
|--------|-------------------------------------|--------------------------------|:------------:|
| GET    | `/articles`                         | List articles (with filters)   | No           |
| GET    | `/articles/feed`                    | Get feed (followed authors)    | Yes          |
| POST   | `/articles`                         | Create an article              | Yes          |
| GET    | `/articles/{slug}`                  | Get a single article           | No           |
| PUT    | `/articles/{slug}`                  | Update an article              | Yes          |
| DELETE | `/articles/{slug}`                  | Delete an article              | Yes          |

**Query parameters** for `GET /articles`:
- `tag` - Filter by tag
- `author` - Filter by author username
- `favorited` - Filter by user who favorited
- `offset` - Offset for pagination (default: `0`)
- `limit` - Limit for pagination (default: `20`)

### Favorites

| Method | Endpoint                         | Description           | Auth Required |
|--------|----------------------------------|-----------------------|:------------:|
| POST   | `/articles/{slug}/favorite`      | Favorite an article   | Yes          |
| DELETE | `/articles/{slug}/favorite`      | Unfavorite an article | Yes          |

### Comments

| Method | Endpoint                             | Description            | Auth Required |
|--------|--------------------------------------|------------------------|:------------:|
| GET    | `/articles/{slug}/comments`          | Get article comments   | No           |
| POST   | `/articles/{slug}/comments`          | Add a comment          | Yes          |
| DELETE | `/articles/{slug}/comments/{id}`     | Delete a comment       | Yes          |

### Tags

| Method | Endpoint | Description   | Auth Required |
|--------|----------|---------------|:------------:|
| GET    | `/tags`  | Get all tags  | No           |

---

## GraphQL API

The GraphQL endpoint is available at `/graphql`, and an interactive IDE (GraphiQL) is available at `/graphiql`.

### Example Queries

**Get an article by slug:**
```graphql
query {
  article(slug: "my-article") {
    title
    description
    body
    tagList
    author {
      username
      bio
      image
    }
    createdAt
    favoritesCount
  }
}
```

**List articles with pagination:**
```graphql
query {
  articles(first: 10, after: "cursor") {
    edges {
      node {
        title
        slug
        description
        author {
          username
        }
      }
      cursor
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

### Example Mutations

**Register a new user:**
```graphql
mutation {
  createUser(input: {
    email: "user@example.com"
    username: "newuser"
    password: "password123"
  }) {
    ... on UserPayload {
      user {
        email
        username
        token
      }
    }
    ... on Error {
      message
      errors {
        key
        value
      }
    }
  }
}
```

**Create an article:**
```graphql
mutation {
  createArticle(input: {
    title: "My New Article"
    description: "A brief description"
    body: "The full article body..."
    tagList: ["spring", "java"]
  }) {
    article {
      slug
      title
    }
  }
}
```

For the full schema, see [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls).

---

## Authentication & Security

This application uses **JWT (JSON Web Token)** based authentication with **Spring Security**:

- **Stateless sessions** - No server-side session storage; all auth state is carried in the JWT.
- **BCrypt password hashing** - Passwords are hashed using BCrypt before storage.
- **JWT token filter** - A custom `JwtTokenFilter` extracts and validates the JWT from the `Authorization` header on every request.
- **CORS enabled** - Cross-Origin Resource Sharing is configured to allow all origins.

### How Authentication Works

1. **Register** via `POST /users` with `email`, `username`, and `password`.
2. **Login** via `POST /users/login` with `email` and `password` to receive a JWT token.
3. **Include the token** in subsequent requests via the `Authorization` header:
   ```
   Authorization: Token <your-jwt-token>
   ```

### Configuration

JWT settings are in `application.properties`:

| Property          | Description                          | Default                |
|-------------------|--------------------------------------|------------------------|
| `jwt.secret`      | Secret key for signing JWT tokens    | (set in config)        |
| `jwt.sessionTime` | Token validity duration (seconds)    | `86400` (24 hours)     |

### Public vs. Protected Endpoints

| Access     | Endpoints                                                                    |
|------------|-----------------------------------------------------------------------------|
| **Public** | `POST /users`, `POST /users/login`, `GET /articles/**`, `GET /profiles/**`, `GET /tags`, `/graphql`, `/graphiql` |
| **Protected** | All other endpoints (require valid JWT)                                  |

---

## Database

The application uses **SQLite** as its database for easy local development without external database setup. Data persists across restarts in a `dev.db` file in the project root.

- **Tests** use an in-memory SQLite database (configured in `application-test.properties`).
- **Database migrations** are managed by [Flyway](https://flywaydb.org/) and located in `src/main/resources/db/migration/`.

### Database Schema

The database contains the following tables:

| Table               | Description                              |
|---------------------|------------------------------------------|
| `users`             | User accounts (username, email, password, bio, image) |
| `articles`          | Published articles (title, slug, description, body) |
| `comments`          | Comments on articles                     |
| `tags`              | Tag definitions                          |
| `article_tags`      | Many-to-many link between articles and tags |
| `article_favorites` | Many-to-many link between users and favorited articles |
| `follows`           | User follow relationships                |

### Switching Databases

To use a different database (e.g., MySQL, PostgreSQL), update the following in `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/realworld
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=your_username
spring.datasource.password=your_password
```

You will also need to add the appropriate JDBC driver dependency in `build.gradle`.

---

## Running with Docker

### Prerequisites

- [Docker](https://www.docker.com/) installed

### Build and Run

```bash
# Build the Docker image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app

# Run the container (maps container port 8080 to host port 8081)
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The API will be available at **http://localhost:8081**.

---

## Using with a RealWorld Frontend

This backend is designed to work with any [RealWorld](https://github.com/gothinkster/realworld) frontend implementation.

> **Important:** The entry point address of the backend API is at **http://localhost:8080**, **not** http://localhost:8080/api as some frontend documentation may suggest.

To get started:
1. Start this backend: `./gradlew bootRun`
2. Clone any [RealWorld frontend](https://codebase.show/projects/realworld) (e.g., React, Angular, Vue)
3. Configure the frontend's API base URL to `http://localhost:8080`
4. Start the frontend and begin using the app

---

## Testing

The repository contains a comprehensive test suite covering both API integration tests and repository/data layer tests.

### Run All Tests

```bash
./gradlew test
```

### Test Configuration

- Tests use an **in-memory SQLite database** (`jdbc:sqlite::memory:`) so they run fast and don't affect your local development database.
- API tests use [REST Assured](https://rest-assured.io/) with Spring MockMvc for end-to-end HTTP testing.
- The test profile is configured in `src/main/resources/application-test.properties`.

---

## Code Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with [Google Java Format](https://github.com/google/google-java-format) for consistent code styling.

### Check Formatting

```bash
./gradlew spotlessCheck
```

### Apply Formatting

```bash
./gradlew spotlessJavaApply
```

---

## Contributing

Contributions are welcome! Please fork this repository and submit a pull request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Format your code (`./gradlew spotlessJavaApply`)
4. Run the tests (`./gradlew test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
