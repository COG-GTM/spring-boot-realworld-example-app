# Spring Boot RealWorld Example App

A fully-featured backend implementation of the [RealWorld](https://github.com/gothinkster/realworld) spec using Spring Boot, MyBatis, and GraphQL, demonstrating real-world patterns including CRUD operations, authentication, routing, and pagination.

![RealWorld Example App](example-logo.png)

[![Java CI](https://github.com/COG-GTM/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/COG-GTM/spring-boot-realworld-example-app/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [GraphQL Support](#graphql-support)
- [Database](#database)
- [Security](#security)
- [Testing](#testing)
- [Code Formatting](#code-formatting)
- [Docker](#docker)
- [Contributing](#contributing)
- [License](#license)

## Overview

This codebase demonstrates a production-ready backend application built with Spring Boot and MyBatis. It implements the RealWorld API specification, which provides a standardized API for building "Conduit" - a Medium.com clone. The application supports both REST and GraphQL interfaces, making it a versatile reference implementation for modern backend development patterns.

## Features

The application provides a complete backend for a blogging platform with the following capabilities:

**User Management** includes user registration, authentication via JWT tokens, profile management, and the ability to follow other users.

**Article Management** covers creating, reading, updating, and deleting articles. Articles support tags, favorites, and pagination. Users can view their personalized feed of articles from authors they follow.

**Comments** allow users to add and remove comments on articles.

**Tags** provide a way to categorize and filter articles.

## Requirements

- Java 11 or higher
- Gradle 7.x (included via wrapper)

## Getting Started

Clone the repository and navigate to the project directory:

```bash
git clone https://github.com/COG-GTM/spring-boot-realworld-example-app.git
cd spring-boot-realworld-example-app
```

Run the application using the Gradle wrapper:

```bash
./gradlew bootRun
```

The application starts on `http://localhost:8080`. Verify it's running by accessing the tags endpoint:

```bash
curl http://localhost:8080/tags
```

Or open `http://localhost:8080/tags` in your browser.

## Project Structure

The codebase follows Domain-Driven Design (DDD) principles to separate business logic from infrastructure concerns:

```
src/main/java/io/spring/
├── api/                    # REST controllers (Spring MVC web layer)
│   ├── ArticleApi.java
│   ├── ArticlesApi.java
│   ├── CommentsApi.java
│   ├── CurrentUserApi.java
│   ├── ProfileApi.java
│   ├── TagsApi.java
│   ├── UsersApi.java
│   ├── exception/          # Exception handlers
│   └── security/           # Security filters and configuration
├── application/            # Application services and DTOs
├── core/                   # Domain models and business logic
│   ├── article/
│   ├── comment/
│   ├── favorite/
│   ├── service/
│   └── user/
├── graphql/                # GraphQL resolvers and data fetchers
└── infrastructure/         # Technical implementations (MyBatis mappers, etc.)
```

**api** - The web layer implemented with Spring MVC, handling HTTP requests and responses.

**core** - The domain layer containing business entities and services, independent of any framework.

**application** - High-level services implementing the CQRS pattern to separate read and write operations.

**infrastructure** - Implementation details including MyBatis mappers for the Data Mapper persistence pattern.

**graphql** - GraphQL resolvers using Netflix's DGS framework.

## API Documentation

The REST API follows the [RealWorld API Spec](https://github.com/gothinkster/realworld/tree/master/api). The base URL is `http://localhost:8080` (not `/api` as some frontend documentation suggests).

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Register a new user |
| POST | `/users/login` | Login and receive JWT token |
| GET | `/user` | Get current user |
| PUT | `/user` | Update current user |

### Profile Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profiles/:username` | Get user profile |
| POST | `/profiles/:username/follow` | Follow a user |
| DELETE | `/profiles/:username/follow` | Unfollow a user |

### Article Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/articles` | List articles (supports filtering) |
| GET | `/articles/feed` | Get feed of followed users |
| GET | `/articles/:slug` | Get single article |
| POST | `/articles` | Create article |
| PUT | `/articles/:slug` | Update article |
| DELETE | `/articles/:slug` | Delete article |
| POST | `/articles/:slug/favorite` | Favorite article |
| DELETE | `/articles/:slug/favorite` | Unfavorite article |

### Comment Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/articles/:slug/comments` | Get comments for article |
| POST | `/articles/:slug/comments` | Add comment |
| DELETE | `/articles/:slug/comments/:id` | Delete comment |

### Tag Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/tags` | Get all tags |

## GraphQL Support

The application implements GraphQL alongside REST, following DDD principles where both interfaces are adapters to the same domain layer. GraphQL support is powered by [Netflix DGS Framework](https://github.com/Netflix/dgs-framework).

The GraphQL endpoint is available at `http://localhost:8080/graphql`.

![GraphQL Schema](graphql-schema.png)

### Example Queries

Fetch articles with pagination:

```graphql
query {
  articles(first: 10) {
    edges {
      node {
        title
        slug
        description
        author {
          username
        }
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

Create a new article:

```graphql
mutation {
  createArticle(input: {
    title: "My Article"
    description: "Article description"
    body: "Article content"
    tagList: ["spring", "java"]
  }) {
    article {
      slug
      title
    }
  }
}
```

The complete GraphQL schema is available at [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls).

## Database

The application uses SQLite for persistence, making it easy to run locally without external database setup. The database file (`dev.db`) is created automatically on first run.

To switch to a different database, modify `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlite:dev.db
spring.datasource.driver-class-name=org.sqlite.JDBC
```

Database migrations are managed by Flyway and run automatically on startup.

## Security

Authentication is handled via JWT (JSON Web Tokens). The implementation uses Spring Security with a custom JWT filter for token validation.

The JWT secret and session duration are configured in `application.properties`:

```properties
jwt.secret=your-secret-key
jwt.sessionTime=86400
```

For production deployments, ensure you change the default secret key to a secure, randomly generated value.

## Testing

The project includes comprehensive tests covering both API endpoints and repository operations. Run the test suite with:

```bash
./gradlew test
```

Tests use REST Assured for API testing and Spring Boot Test for integration testing.

## Code Formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format for consistent code style. Format your code before committing:

```bash
./gradlew spotlessJavaApply
```

Check formatting without applying changes:

```bash
./gradlew spotlessCheck
```

## Docker

Build and run the application using Docker:

```bash
# Build the Docker image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app

# Run the container
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be available at `http://localhost:8081`.

## Contributing

Contributions are welcome. Please fork the repository and submit a pull request with your changes. Ensure all tests pass and code is properly formatted before submitting.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

_Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team._
