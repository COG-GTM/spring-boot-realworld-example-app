# Spring Boot RealWorld Example App

![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Java CI](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A fully-featured Spring Boot application demonstrating real-world patterns including CRUD operations, authentication, routing, and pagination, adhering to the [RealWorld](https://github.com/gothinkster/realworld) specification.

This codebase showcases how to build a production-ready backend API using Spring Boot, MyBatis, and Domain-Driven Design principles. It supports both REST and GraphQL interfaces, making it an excellent reference for modern Java backend development.

## Table of Contents

- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [API Endpoints](#api-endpoints)
- [GraphQL Support](#graphql-support)
- [Database](#database)
- [Security](#security)
- [Testing](#testing)
- [Code Formatting](#code-formatting)
- [Docker](#docker)
- [Using with Frontend](#using-with-frontend)
- [Contributing](#contributing)
- [License](#license)

## Requirements

- Java 11 or higher
- Gradle 7.x (included via Gradle Wrapper)

## Quick Start

Clone the repository and run the application:

```bash
# Clone the repository
git clone https://github.com/gothinkster/spring-boot-realworld-example-app.git
cd spring-boot-realworld-example-app

# Run the application
./gradlew bootRun
```

The application starts on `http://localhost:8080`. Verify it's running:

```bash
curl http://localhost:8080/tags
```

Or open http://localhost:8080/tags in your browser.

## Project Structure

```
src/main/java/io/spring/
├── api/                    # REST API controllers (Spring MVC)
│   ├── ArticleApi.java
│   ├── ArticlesApi.java
│   ├── CommentsApi.java
│   ├── CurrentUserApi.java
│   ├── ProfileApi.java
│   ├── TagsApi.java
│   ├── UsersApi.java
│   ├── exception/          # Exception handlers
│   └── security/           # Security filters and JWT handling
├── application/            # Application services and DTOs
│   ├── ArticleQueryService.java
│   ├── CommentQueryService.java
│   ├── ProfileQueryService.java
│   └── data/               # Data transfer objects
├── core/                   # Domain layer (entities and business logic)
│   ├── article/            # Article domain
│   ├── comment/            # Comment domain
│   ├── favorite/           # Favorite domain
│   ├── service/            # Domain services
│   └── user/               # User domain
├── graphql/                # GraphQL resolvers and data fetchers
└── infrastructure/         # Technical implementations
    ├── mybatis/            # MyBatis mappers and configurations
    ├── repository/         # Repository implementations
    └── service/            # Infrastructure services
```

## Architecture

This application follows Domain-Driven Design (DDD) principles with a clear separation of concerns:

The **api** layer handles HTTP requests through Spring MVC controllers. It serves as the entry point for REST API calls and delegates business operations to the application layer.

The **core** layer contains the domain model, including entities like Article, Comment, and User, along with repository interfaces and domain services. This layer is framework-agnostic and focuses purely on business logic.

The **application** layer implements the CQRS (Command Query Responsibility Segregation) pattern, separating read operations (queries) from write operations (commands). Query services return DTOs optimized for specific use cases.

The **infrastructure** layer provides concrete implementations for repository interfaces using MyBatis as the Data Mapper pattern. This layer handles all database interactions and external service integrations.

## API Endpoints

The API follows the [RealWorld API Spec](https://github.com/gothinkster/realworld/tree/master/api). Key endpoints include:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Register a new user |
| POST | `/users/login` | Login and get JWT token |
| GET | `/user` | Get current user |
| PUT | `/user` | Update current user |
| GET | `/profiles/:username` | Get user profile |
| POST | `/profiles/:username/follow` | Follow a user |
| GET | `/articles` | List articles |
| POST | `/articles` | Create article |
| GET | `/articles/:slug` | Get article |
| PUT | `/articles/:slug` | Update article |
| DELETE | `/articles/:slug` | Delete article |
| POST | `/articles/:slug/comments` | Add comment |
| GET | `/tags` | Get all tags |

## GraphQL Support

This application implements both REST and GraphQL APIs following DDD principles, where the transport layer (REST or GraphQL) is simply an adapter to the same domain logic.

The GraphQL schema is defined in `src/main/resources/schema/schema.graphqls` and includes queries for articles, users, profiles, and tags, as well as mutations for creating and updating content.

![GraphQL Schema](graphql-schema.png)

The implementation uses Netflix's [DGS Framework](https://github.com/Netflix/dgs-framework), a modern GraphQL server framework for Java. Access the GraphQL endpoint at `http://localhost:8080/graphql`.

## Database

The application uses SQLite for local development, storing data in `dev.db`. This allows for easy testing without losing data between restarts. The database configuration can be changed in `src/main/resources/application.properties` to use any other database supported by JDBC.

Database migrations are managed by Flyway, with migration scripts located in `src/main/resources/db/migration/`.

## Security

Authentication is handled via JWT (JSON Web Tokens) integrated with Spring Security. The JWT secret and session duration are configured in `application.properties`:

```properties
jwt.secret=your-secret-key
jwt.sessionTime=86400
```

Protected endpoints require a valid JWT token in the Authorization header:

```
Authorization: Token <jwt-token>
```

## Testing

The project includes comprehensive tests covering both API endpoints and repository operations:

```bash
# Run all tests
./gradlew test
```

Tests use Spring Boot's testing support along with REST Assured for API testing and MyBatis test utilities for repository testing.

## Code Formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format for consistent code style:

```bash
# Apply code formatting
./gradlew spotlessJavaApply

# Check code formatting
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

## Using with Frontend

This backend is designed to work with any [RealWorld frontend](https://github.com/gothinkster/realworld). The API entry point is `http://localhost:8080` (not `/api` as some frontends may suggest).

For more information on how this works with other frontends and backends, visit the [RealWorld](https://github.com/gothinkster/realworld) repository.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests to help improve this project.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

_Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team._
