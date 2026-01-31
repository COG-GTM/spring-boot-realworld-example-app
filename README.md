# Spring Boot RealWorld Example App

A fully-featured REST API and GraphQL backend implementation built with Spring Boot and MyBatis, demonstrating real-world patterns including CRUD operations, authentication, routing, and pagination.

[![Java CI](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

![RealWorld Example App](example-logo.png)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Running with Docker](#running-with-docker)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [GraphQL Support](#graphql-support)
- [Testing](#testing)
- [Code Formatting](#code-formatting)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)

## Overview

This codebase demonstrates a fully-fledged backend application built with Spring Boot and MyBatis that adheres to the [RealWorld](https://github.com/gothinkster/realworld) specification. The RealWorld project provides a standardized API spec that allows different frontend and backend implementations to be interchangeable, making it an excellent resource for learning and comparing different technology stacks.

The application implements a Medium.com-like blogging platform where users can publish articles, follow other users, favorite articles, and leave comments. It showcases production-ready patterns and best practices for building robust Spring Boot applications.

## Features

This implementation includes both REST API and GraphQL interfaces, allowing you to choose the approach that best fits your needs. The REST API follows the RealWorld specification exactly, while the GraphQL API provides a flexible alternative with relay-style pagination.

Key capabilities include user registration and authentication using JWT tokens, article management with full CRUD operations, a tagging system for categorizing content, user profiles with follow/unfollow functionality, article favoriting, and a commenting system. The architecture follows Domain-Driven Design principles with a clear separation between business logic and infrastructure concerns.

## Requirements

- Java 11 or higher
- Gradle (wrapper included)

## Getting Started

Clone the repository and navigate to the project directory. The application uses an embedded SQLite database, so no external database setup is required for local development.

To start the application:

```bash
./gradlew bootRun
```

The server will start on `http://localhost:8080`. You can verify it's running by fetching the tags endpoint:

```bash
curl http://localhost:8080/tags
```

Or open `http://localhost:8080/tags` in your browser.

## Running with Docker

If you prefer using Docker, you can build and run the application in a container:

```bash
# Build the Docker image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app

# Run the container
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be available at `http://localhost:8081`.

## Project Structure

The codebase follows Domain-Driven Design principles to maintain a clear separation between business logic and technical implementation details:

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
│   └── article/            # Article-related query services
├── core/                   # Domain layer (entities and business logic)
│   ├── article/            # Article aggregate
│   ├── comment/            # Comment aggregate
│   ├── favorite/           # Favorite functionality
│   ├── service/            # Domain services
│   └── user/               # User aggregate
├── graphql/                # GraphQL resolvers and data fetchers
└── infrastructure/         # Technical implementations
    └── mybatis/            # MyBatis mappers and repositories
```

The architecture implements the CQRS (Command Query Responsibility Segregation) pattern, separating read operations from write operations for better scalability and maintainability.

## API Endpoints

The REST API entry point is at `http://localhost:8080` (not `/api` as some frontend documentation suggests).

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Register a new user |
| POST | `/users/login` | Login and receive JWT token |
| GET | `/user` | Get current user |
| PUT | `/user` | Update current user |

### Articles

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/articles` | List articles (with optional filters) |
| GET | `/articles/feed` | Get articles from followed users |
| GET | `/articles/:slug` | Get a single article |
| POST | `/articles` | Create an article |
| PUT | `/articles/:slug` | Update an article |
| DELETE | `/articles/:slug` | Delete an article |

### Comments

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/articles/:slug/comments` | Get comments for an article |
| POST | `/articles/:slug/comments` | Add a comment |
| DELETE | `/articles/:slug/comments/:id` | Delete a comment |

### Profiles & Social

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profiles/:username` | Get a user profile |
| POST | `/profiles/:username/follow` | Follow a user |
| DELETE | `/profiles/:username/follow` | Unfollow a user |
| POST | `/articles/:slug/favorite` | Favorite an article |
| DELETE | `/articles/:slug/favorite` | Unfavorite an article |

### Tags

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/tags` | Get all tags |

## GraphQL Support

This implementation provides a GraphQL API alongside the REST API, demonstrating how both can coexist while sharing the same domain layer. The GraphQL implementation uses Netflix's [DGS Framework](https://github.com/Netflix/dgs-framework).

![GraphQL Schema](graphql-schema.png)

The GraphQL endpoint is available at `/graphql`. The schema supports queries for articles, users, profiles, and tags, as well as mutations for all write operations. Relay-style pagination is implemented for list queries.

The full schema is available at [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls).

## Testing

The repository includes comprehensive test coverage for both API endpoints and repository operations:

```bash
./gradlew test
```

## Code Formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format for consistent code style:

```bash
./gradlew spotlessJavaApply
```

## Configuration

Application settings are managed in `src/main/resources/application.properties`. Key configuration options include:

- **Database**: Uses SQLite by default (`dev.db`), which persists data between restarts. This can be changed to any other database by modifying the datasource configuration.
- **JWT Settings**: The secret key and session duration are configurable. For production deployments, ensure you change the default secret key.
- **MyBatis**: Mapper configurations and type handlers are set up for the persistence layer.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests to help improve this implementation.

For code contributions, please ensure your changes pass all tests and follow the project's code formatting standards by running `./gradlew spotlessJavaApply` before committing.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

For more information on how this works with other frontends and backends, visit the [RealWorld](https://github.com/gothinkster/realworld) repository.

_Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team._
