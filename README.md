# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# *NEW* GraphQL Support  

Following some DDD principles. REST or GraphQL is just a kind of adapter. And the domain layer will be consistent all the time. So this repository implement GraphQL and REST at the same time.

The GraphQL schema is https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/src/main/resources/schema/schema.graphqls and the visualization looks like below.

![](graphql-schema.png)

And this implementation is using [dgs-framework](https://github.com/Netflix/dgs-framework) which is a quite new java graphql server framework.
# How it works

The application uses Spring Boot (Web, Mybatis).

* Use the idea of Domain Driven Design to separate the business term and infrastructure term.
* Use MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* Use [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern to separate the read model and write model.

And the code is organized as this:

1. `api` is the web layer implemented by Spring MVC
2. `core` is the business model including entities and services
3. `application` is the high-level services for querying the data transfer objects
4. `infrastructure`  contains all the implementation classes as the technique details

# Security

Integration with Spring Security and add other filter for jwt token process.

The secret key is stored in `application.properties`.

# Rate limiting

Every route is rate limited by a servlet filter (fixed one-minute window, in-memory). Unauthenticated
requests are limited per client IP (first `X-Forwarded-For` entry, else the remote address);
authenticated requests are limited per API token. When a limit is hit the API responds with
`429 Too Many Requests`, a `Retry-After` header (seconds) and the body
`{"error": "rate_limited", "retry_after_seconds": N}`. The health check endpoint is never limited.

The limits are configured in `application.properties` (or the equivalent environment variables):

| Property                            | Env var                            | Default            | Description                                   |
|-------------------------------------|------------------------------------|--------------------|-----------------------------------------------|
| `ratelimit.enabled`                 | `RATELIMIT_ENABLED`                | `true`             | Turn the limiter on/off                       |
| `ratelimit.anonymous-per-minute`    | `RATELIMIT_ANONYMOUS_PER_MINUTE`   | `60`               | Requests per minute per client IP             |
| `ratelimit.authenticated-per-minute`| `RATELIMIT_AUTHENTICATED_PER_MINUTE` | `600`            | Requests per minute per API token             |
| `ratelimit.health-path`             | `RATELIMIT_HEALTH_PATH`            | `/actuator/health` | Path excluded from rate limiting              |

# Database

It uses a ~~H2 in-memory database~~ sqlite database (for easy local test without losing test data after every restart), can be changed easily in the `application.properties` for any other database.

# Getting started

You'll need Java 11 installed.

    ./gradlew bootRun

To test that it works, open a browser tab at http://localhost:8080/tags .  
Alternatively, you can run

    curl http://localhost:8080/tags

# Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.
	
    ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
    docker run -p 8081:8080 spring-boot-realworld-example-app

# Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

# Run test

The repository contains a lot of test cases to cover both api test and repository test.

    ./gradlew test

# Code format

Use spotless for code format.

    ./gradlew spotlessJavaApply

# Help

Please fork and PR to improve the project.
