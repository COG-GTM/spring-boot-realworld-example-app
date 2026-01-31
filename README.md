# ![RealWorld Example App](example-logo.png)

A full-stack Spring Boot application implementing the [RealWorld](https://github.com/gothinkster/realworld) specification with both REST and GraphQL APIs.

[![Java CI](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Running with Docker](#running-with-docker)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [GraphQL Support](#graphql-support)
- [Database](#database)
- [Security](#security)
- [Testing](#testing)
- [Code Formatting](#code-formatting)
- [Contributing](#contributing)
- [License](#license)

## Overview

This codebase demonstrates a fully-featured application built with Spring Boot and MyBatis, including CRUD operations, authentication, routing, pagination, and more. It adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API, making it compatible with any RealWorld frontend implementation.

The application follows Domain Driven Design (DDD) principles to separate business logic from infrastructure concerns, and implements the Command Query Responsibility Segregation (CQRS) pattern to separate read and write models.

## Features

This implementation includes support for both REST and GraphQL APIs, allowing you to choose the interface that best fits your needs. The REST API follows the RealWorld specification while the GraphQL API provides a flexible query interface using Netflix's [DGS Framework](https://github.com/Netflix/dgs-framework).

The application uses MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence, providing clean separation between domain objects and database operations. Authentication is handled through JWT tokens integrated with Spring Security.

## Requirements

- Java 11 or higher
- Gradle (wrapper included)

## Quick Start

Clone the repository and run the application:

```bash
# Clone the repository
git clone https://github.com/gothinkster/spring-boot-realworld-example-app.git
cd spring-boot-realworld-example-app

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`. You can verify it's running by visiting `http://localhost:8080/tags` in your browser or using curl:

```bash
curl http://localhost:8080/tags
```

## Running with Docker

If you prefer to run the application in a container:

```bash
# Build the Docker image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app

# Run the container
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be available at `http://localhost:8081`.

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
│   └── security/           # Security filters and configuration
├── application/            # Query services and DTOs (read model)
├── core/                   # Domain model and business logic
│   ├── article/
│   ├── comment/
│   ├── favorite/
│   ├── service/
│   └── user/
├── graphql/                # GraphQL resolvers and data fetchers
└── infrastructure/         # Technical implementations
    ├── mybatis/            # MyBatis mappers and type handlers
    ├── repository/         # Repository implementations
    └── service/            # Infrastructure services
```

The code organization follows DDD principles where `api` serves as the web layer, `core` contains the business model including entities and services, `application` provides high-level services for querying data transfer objects, and `infrastructure` contains all implementation classes for technical details.

## API Documentation

The REST API entry point is at `http://localhost:8080`. Note that this differs from some frontend documentation which may suggest `http://localhost:8080/api`.

The API implements the full RealWorld specification including endpoints for user registration and authentication, user profile management, article CRUD operations, comments on articles, favoriting articles, following users, and tag listing.

For detailed API documentation, refer to the [RealWorld API Spec](https://github.com/gothinkster/realworld/tree/master/api).

## GraphQL Support

The application provides a GraphQL API alongside the REST API. Both interfaces share the same domain layer, demonstrating how DDD principles allow different adapters to coexist.

The GraphQL schema is located at `src/main/resources/schema/schema.graphqls` and supports queries for articles, user profiles, tags, and feeds, as well as mutations for user management, article operations, and comments.

![GraphQL Schema](graphql-schema.png)

The GraphQL endpoint is available at `http://localhost:8080/graphql` with an interactive GraphiQL interface at `http://localhost:8080/graphiql`.

## Database

The application uses SQLite for persistence, which allows for easy local development without losing test data between restarts. The database file (`dev.db`) is created automatically in the project root.

To use a different database, modify the connection settings in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlite:dev.db
spring.datasource.driver-class-name=org.sqlite.JDBC
```

Database migrations are managed by Flyway and located in `src/main/resources/db/migration`.

## Security

Authentication is implemented using JWT tokens integrated with Spring Security. The JWT secret key and session duration are configured in `application.properties`:

```properties
jwt.secret=your-secret-key
jwt.sessionTime=86400
```

For production deployments, ensure you change the default secret key to a secure value.

## Testing

The repository includes comprehensive test coverage for both API and repository layers:

```bash
# Run all tests
./gradlew test
```

## Code Formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format for consistent code style:

```bash
# Apply code formatting
./gradlew spotlessJavaApply

# Check formatting without applying changes
./gradlew spotlessCheck
```

## Contributing

Contributions are welcome. Please fork the repository and submit a pull request with your changes. Ensure that all tests pass and code formatting is applied before submitting.

For more information on how this project works with other frontends and backends, visit the [RealWorld](https://github.com/gothinkster/realworld) repository.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

_Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team._
