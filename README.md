# Spring Boot RealWorld Example App

A production-ready Spring Boot implementation of the [RealWorld](https://github.com/gothinkster/realworld) specification, demonstrating a Medium.com clone backend with both REST and GraphQL APIs.

![RealWorld Example App](example-logo.png)

[![Java CI](https://github.com/COG-GTM/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/COG-GTM/spring-boot-realworld-example-app/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Docker](#docker)
- [Contributing](#contributing)
- [License](#license)

## Overview

This codebase demonstrates a fully-featured backend application built with Spring Boot and MyBatis, implementing CRUD operations, JWT authentication, pagination, and more. The application follows Domain-Driven Design (DDD) principles and uses the Command Query Responsibility Segregation (CQRS) pattern to separate read and write operations.

The backend serves as a reference implementation for the RealWorld specification, which defines a standardized API for building Medium-like applications. Frontend developers can connect any RealWorld-compliant frontend to this backend without modification.

## Features

The application provides a complete set of features for a social blogging platform:

**User Management** handles registration, authentication via JWT tokens, and profile management including bio and avatar updates. **Content Publishing** supports creating, reading, updating, and deleting articles with automatic slug generation from titles. **Social Features** enable users to follow other users, favorite articles, and comment on articles. **Content Discovery** allows browsing articles by tags, filtering by author or favorited users, and accessing personalized feeds.

Both REST and GraphQL APIs are available, sharing the same underlying domain logic. The GraphQL implementation uses Netflix's DGS Framework and supports cursor-based pagination for efficient data fetching.

## Architecture

The application follows a four-layer architecture with clear separation of concerns:

```
src/main/java/io/spring/
├── api/                    # REST controllers and security filters
├── application/            # Use case orchestration and DTOs
├── core/                   # Domain entities and repository interfaces
├── graphql/                # GraphQL resolvers and data fetchers
└── infrastructure/         # MyBatis implementations and services
```

**API Layer** exposes HTTP endpoints via Spring MVC controllers and handles request validation. **Application Layer** orchestrates use cases through query and command services, implementing the CQRS pattern. **Core Layer** contains pure business logic with domain entities like User, Article, Comment, and Tag, along with repository interfaces. **Infrastructure Layer** provides concrete implementations using MyBatis for data persistence and services for JWT token handling and password encryption.

The Data Mapper pattern is implemented through MyBatis, which maps between domain objects and the SQLite database. Flyway manages database migrations, with the schema defined in `src/main/resources/db/migration/`.

## Requirements

- Java 11 or higher
- Gradle 7.4+ (wrapper included)

## Getting Started

Clone the repository and run the application using the Gradle wrapper:

```bash
# Clone the repository
git clone https://github.com/COG-GTM/spring-boot-realworld-example-app.git
cd spring-boot-realworld-example-app

# Run the application
./gradlew bootRun
```

The server starts on port 8080. Verify it's running by fetching the tags endpoint:

```bash
curl http://localhost:8080/tags
```

Or open http://localhost:8080/tags in your browser.

### Connecting a Frontend

The backend API is available at `http://localhost:8080` (note: there is no `/api` prefix). Any RealWorld-compliant frontend can connect to this backend by configuring its API base URL accordingly.

## API Documentation

### REST API

The REST API follows the [RealWorld API specification](https://github.com/gothinkster/realworld/tree/main/api). Key endpoints include:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Register a new user |
| POST | `/users/login` | Authenticate and receive JWT token |
| GET | `/user` | Get current user (requires auth) |
| PUT | `/user` | Update current user (requires auth) |
| GET | `/profiles/:username` | Get user profile |
| POST | `/profiles/:username/follow` | Follow a user (requires auth) |
| DELETE | `/profiles/:username/follow` | Unfollow a user (requires auth) |
| GET | `/articles` | List articles with optional filters |
| GET | `/articles/feed` | Get personalized feed (requires auth) |
| POST | `/articles` | Create an article (requires auth) |
| GET | `/articles/:slug` | Get a single article |
| PUT | `/articles/:slug` | Update an article (requires auth) |
| DELETE | `/articles/:slug` | Delete an article (requires auth) |
| POST | `/articles/:slug/favorite` | Favorite an article (requires auth) |
| DELETE | `/articles/:slug/favorite` | Unfavorite an article (requires auth) |
| GET | `/articles/:slug/comments` | Get article comments |
| POST | `/articles/:slug/comments` | Add a comment (requires auth) |
| DELETE | `/articles/:slug/comments/:id` | Delete a comment (requires auth) |
| GET | `/tags` | Get all tags |

Authentication uses JWT tokens passed in the `Authorization` header with the format `Token <jwt>`. Tokens expire after 24 hours.

### GraphQL API

The GraphQL endpoint is available at `http://localhost:8080/graphql`. An interactive GraphiQL IDE is accessible at `http://localhost:8080/graphiql` for exploring the schema and testing queries.

![GraphQL Schema](graphql-schema.png)

The schema supports queries for articles, profiles, tags, and the current user, along with mutations for user registration, authentication, article management, and comments. Cursor-based pagination is available for article and comment lists.

Example query:

```graphql
query {
  articles(first: 10) {
    edges {
      node {
        title
        slug
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

## Project Structure

```
spring-boot-realworld-example-app/
├── src/
│   ├── main/
│   │   ├── java/io/spring/
│   │   │   ├── api/                 # REST controllers
│   │   │   │   ├── security/        # JWT filter and security config
│   │   │   │   └── exception/       # Exception handlers
│   │   │   ├── application/         # Query/command services and DTOs
│   │   │   ├── core/                # Domain entities and repositories
│   │   │   │   ├── article/         # Article, Tag entities
│   │   │   │   ├── comment/         # Comment entity
│   │   │   │   ├── favorite/        # ArticleFavorite entity
│   │   │   │   └── user/            # User, FollowRelation entities
│   │   │   ├── graphql/             # GraphQL resolvers
│   │   │   └── infrastructure/      # MyBatis implementations
│   │   └── resources/
│   │       ├── db/migration/        # Flyway migrations
│   │       ├── mapper/              # MyBatis XML mappers
│   │       ├── schema/              # GraphQL schema
│   │       └── application.properties
│   └── test/                        # Unit and integration tests
├── build.gradle                     # Gradle build configuration
├── gradlew                          # Gradle wrapper (Unix)
├── gradlew.bat                      # Gradle wrapper (Windows)
├── Dockerfile                       # Container image definition
└── docker-compose.yml               # Local development with Docker
```

## Testing

The repository includes comprehensive tests covering API endpoints, repository operations, and service logic:

```bash
# Run all tests
./gradlew test
```

Tests use JUnit 5 with Spring Boot Test for integration testing and REST Assured for API testing. The test suite validates both REST and GraphQL endpoints along with repository persistence.

### Code Formatting

The project uses Spotless with Google Java Format for consistent code style:

```bash
# Apply code formatting
./gradlew spotlessJavaApply

# Check formatting without applying changes
./gradlew spotlessCheck
```

## Docker

Build and run the application in a container:

```bash
# Build the Docker image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app

# Run the container
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be available at `http://localhost:8081`.

Alternatively, use Docker Compose for local development:

```bash
docker-compose up
```

## Configuration

Key configuration options in `src/main/resources/application.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `spring.datasource.url` | Database connection URL | `jdbc:sqlite:dev.db` |
| `jwt.secret` | Secret key for JWT signing | (configured) |
| `jwt.sessionTime` | JWT token expiration in seconds | `86400` (24 hours) |
| `image.default` | Default user avatar URL | (configured) |

The application uses SQLite by default for easy local development. To use a different database, update the datasource configuration and add the appropriate JDBC driver dependency.

## Contributing

Contributions are welcome. Please fork the repository and submit a pull request with your changes. Ensure all tests pass and code follows the project's formatting standards before submitting.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

_Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team._
