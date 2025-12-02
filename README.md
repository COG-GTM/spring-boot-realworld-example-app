# ![RealWorld Example App](example-logo.png)

A fully-featured REST API and GraphQL backend built with Spring Boot and MyBatis, implementing the [RealWorld](https://github.com/gothinkster/realworld) specification for a Medium-style blogging platform.

[![Java CI](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [GraphQL Support](#graphql-support)
- [Database](#database)
- [Testing](#testing)
- [Code Formatting](#code-formatting)
- [Docker](#docker)
- [Using with a Frontend](#using-with-a-frontend)
- [Contributing](#contributing)
- [License](#license)

## Overview

This codebase demonstrates a production-ready application built with Spring Boot and MyBatis, featuring CRUD operations, JWT authentication, routing, pagination, and more. The project follows Domain-Driven Design (DDD) principles and implements both REST and GraphQL APIs, allowing you to choose the interface that best fits your needs.

For more information on how this works with other frontends and backends, visit the [RealWorld](https://github.com/gothinkster/realworld) repository.

## Features

This application includes user registration and authentication with JWT tokens, article management with full CRUD operations, commenting system for articles, user profiles with follow/unfollow functionality, article favoriting, tag-based filtering, and feed pagination. Both REST and GraphQL endpoints are available for all operations.

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

The application starts on `http://localhost:8080`. Verify it's running:

```bash
curl http://localhost:8080/tags
```

Or open http://localhost:8080/tags in your browser.

## Project Structure

```
src/main/java/io/spring/
├── api/                    # REST API controllers (Spring MVC)
├── application/            # Application services and DTOs for queries
├── core/                   # Domain models and business logic
├── graphql/                # GraphQL resolvers and data fetchers
└── infrastructure/         # Database mappers, repositories, and security
```

The codebase follows Domain-Driven Design principles. The `api` layer handles HTTP requests via Spring MVC. The `core` layer contains domain entities and services, remaining independent of infrastructure concerns. The `application` layer implements the CQRS pattern, separating read and write models. The `infrastructure` layer provides MyBatis mappers implementing the Data Mapper pattern for persistence.

## API Documentation

The REST API follows the [RealWorld API Spec](https://github.com/gothinkster/realworld/tree/master/api). Key endpoints include:

| Endpoint | Description |
|----------|-------------|
| `POST /users` | Register a new user |
| `POST /users/login` | Authenticate user |
| `GET /user` | Get current user |
| `GET /profiles/:username` | Get user profile |
| `GET /articles` | List articles |
| `POST /articles` | Create article |
| `GET /articles/:slug` | Get article |
| `GET /tags` | Get all tags |

## GraphQL Support

The application implements GraphQL using the [Netflix DGS Framework](https://github.com/Netflix/dgs-framework). The GraphQL endpoint is available at `/graphql`, and the schema supports all operations available through the REST API.

![GraphQL Schema](graphql-schema.png)

The schema is defined in `src/main/resources/schema/schema.graphqls` and includes queries for articles, users, profiles, and tags, along with mutations for creating, updating, and deleting content.

## Database

The application uses SQLite by default for easy local development without losing data between restarts. The database file is `dev.db` in the project root.

To use a different database, modify `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlite:dev.db
spring.datasource.driver-class-name=org.sqlite.JDBC
```

## Testing

Run the test suite:

```bash
./gradlew test
```

The repository includes comprehensive tests covering both API endpoints and repository operations.

## Code Formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format for consistent code style:

```bash
./gradlew spotlessJavaApply
```

## Docker

Build and run with Docker:

```bash
# Build the Docker image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app

# Run the container
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be available at `http://localhost:8081`.

## Using with a Frontend

This backend works with any [RealWorld frontend](https://github.com/gothinkster/realworld#frontends). The API entry point is `http://localhost:8080` (not `/api` as some frontend documentation suggests).

## Contributing

Contributions are welcome. Please fork the repository and submit a pull request with your changes.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

_Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team._
