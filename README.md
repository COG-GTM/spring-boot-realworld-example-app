# Spring Boot RealWorld Example App

A production-ready Spring Boot implementation of the
[RealWorld](https://github.com/gothinkster/realworld) specification that
demonstrates modern backend development practices including Domain Driven Design
(DDD), Command Query Responsibility Segregation (CQRS), and dual API support
(REST + GraphQL).

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

![RealWorld Example App](example-logo.png)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Docker Support](#docker-support)
- [Frontend Integration](#frontend-integration)
- [Contributing](#contributing)
- [License](#license)

## Overview

This codebase serves as a comprehensive example of building a full-stack
application backend using Spring Boot. It implements a Medium.com clone with
features like user authentication, article management, social interactions, and
more. The application demonstrates best practices in:

- **Domain Driven Design (DDD)** - Clear separation between business logic and infrastructure
- **CQRS Pattern** - Separate read and write models for optimal performance
- **Dual API Support** - Both REST and GraphQL endpoints for maximum flexibility
- **Security** - JWT-based authentication with Spring Security
- **Testing** - Comprehensive test coverage for all layers

## Features

- 🔐 **User Management** - Registration, authentication, and profile management
- 📝 **Article System** - Create, read, update, and delete articles with
  rich text support
- 🏷️ **Tagging** - Organize articles with tags and filter by categories
- 👥 **Social Features** - Follow users, favorite articles, and personalized feeds
- 💬 **Comments** - Engage with articles through threaded comments
- 🔍 **Search & Filtering** - Find articles by author, tags, or favorites
- 📄 **Pagination** - Cursor-based pagination for optimal performance
- 🚀 **Dual APIs** - Choose between REST or GraphQL based on your needs

## Quick Start

### Prerequisites

- **Java 11** or higher
- **Gradle** (wrapper included)

### Installation & Running

1. **Clone the repository**

   ```bash
   git clone https://github.com/COG-GTM/spring-boot-realworld-example-app.git
   cd spring-boot-realworld-example-app
   ```

2. **Run the application**

   ```bash
   ./gradlew bootRun
   ```

3. **Verify it's working**

   ```bash
   # Test the REST API
   curl http://localhost:8080/tags
   
   # Or open in browser
   open http://localhost:8080/tags
   ```

The application will start on `http://localhost:8080` with an SQLite
database for local development.

## Project Structure

The application follows Domain Driven Design principles with clear layer separation:

```text
src/main/java/io/spring/
├── api/                    # REST controllers and web layer
│   ├── ArticlesApi.java    # Article management endpoints
│   ├── UsersApi.java       # User authentication endpoints
│   ├── ProfileApi.java     # User profile endpoints
│   └── security/           # JWT authentication filters
├── application/            # Application services (CQRS query side)
│   ├── ArticleQueryService.java
│   ├── CommentQueryService.java
│   └── UserQueryService.java
├── core/                   # Domain model and business logic
│   ├── article/            # Article domain entities
│   ├── user/               # User domain entities
│   └── favorite/           # Favorite relationships
├── graphql/                # GraphQL resolvers and mutations
│   ├── ArticleMutation.java
│   ├── UserMutation.java
│   └── datafetcher/        # GraphQL data fetchers
└── infrastructure/         # Technical implementation details
    ├── repository/         # MyBatis repository implementations
    └── service/            # JWT and other technical services

src/main/resources/
├── mapper/                 # MyBatis XML mappers
├── schema/schema.graphqls  # GraphQL schema definition
└── db/migration/           # Database migration scripts
```

### Key Components

- **Controllers** (`api/`) - Handle HTTP requests and responses
- **Services** (`application/`) - Orchestrate business operations and queries
- **Domain Model** (`core/`) - Business entities and repository interfaces
- **GraphQL** (`graphql/`) - GraphQL resolvers and schema implementation
- **Infrastructure** (`infrastructure/`) - Data access and technical concerns

## API Documentation

### REST API

The REST API follows the
[RealWorld API specification](https://github.com/gothinkster/realworld/tree/master/api).
Key endpoints include:

- `GET /api/articles` - List articles with filtering
- `POST /api/articles` - Create new article
- `GET /api/articles/{slug}` - Get specific article
- `POST /api/users/login` - User authentication
- `GET /api/profiles/{username}` - Get user profile

### GraphQL API

The GraphQL API provides a flexible alternative to REST with a comprehensive schema:

![GraphQL Schema](graphql-schema.png)

**GraphQL Endpoint**: `http://localhost:8080/graphql`

**Key Operations**:

- Query articles with custom filtering and pagination
- Mutations for creating/updating articles and users
- Nested queries for related data (author, comments, etc.)

View the complete schema at [`src/main/resources/schema/schema.graphqls`](src/main/resources/schema/schema.graphqls).

## Development

### Requirements

- **Java**: 11 or higher
- **Spring Boot**: 2.6.3
- **Database**: SQLite (development), easily configurable for production databases
- **Build Tool**: Gradle with wrapper

### Key Dependencies

- **Spring Boot Starter Web** - REST API framework
- **Spring Security** - Authentication and authorization
- **MyBatis** - Data persistence with SQL mapping
- **Netflix DGS** - GraphQL framework
- **JWT** - Token-based authentication
- **Flyway** - Database migrations

### Configuration

The application uses `application.properties` for configuration:

```properties
# Database (SQLite for development)
spring.datasource.url=jdbc:sqlite:dev.db

# JWT Configuration
jwt.secret=your-secret-key
jwt.sessionTime=86400

# MyBatis Configuration
mybatis.mapper-locations=mapper/*.xml
```

### Code Formatting

The project uses Spotless for consistent code formatting:

```bash
# Apply code formatting
./gradlew spotlessJavaApply

# Check formatting
./gradlew spotlessJavaCheck
```

## Testing

The repository includes comprehensive tests covering all application layers:

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

**Test Categories**:

- **API Tests** - REST endpoint integration tests
- **Repository Tests** - Data access layer tests
- **Service Tests** - Business logic unit tests
- **GraphQL Tests** - GraphQL resolver tests

## Docker Support

### Build and Run with Docker

```bash
# Build Docker image
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app

# Run container
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be available at `http://localhost:8081`.

## Frontend Integration

This backend is designed to work with any
[RealWorld frontend implementation](https://github.com/gothinkster/realworld).

**Important**: The API base URL is `http://localhost:8080` (not
`http://localhost:8080/api` as some frontend documentation suggests).

**Supported Frontends**:

- React
- Angular
- Vue.js
- And many more...

## Contributing

We welcome contributions! Here's how to get started:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Make your changes** following the existing code style
4. **Run tests** (`./gradlew test`)
5. **Format code** (`./gradlew spotlessJavaApply`)
6. **Commit changes** (`git commit -m 'Add amazing feature'`)
7. **Push to branch** (`git push origin feature/amazing-feature`)
8. **Open a Pull Request**

### Development Guidelines

- Follow Domain Driven Design principles
- Maintain separation between read and write models (CQRS)
- Write tests for new functionality
- Use proper Spring Boot annotations and patterns
- Keep the GraphQL schema in sync with REST endpoints

## License

This project is licensed under the MIT License - see the
[LICENSE](LICENSE) file for details.

---

_Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team._
