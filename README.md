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

## Integration tests against PostgreSQL

`./gradlew test` runs against an in-memory SQLite database. A separate integration test suite in
`src/integrationTest` boots the whole application (Flyway migrations, MyBatis mappers, REST API over
real HTTP) against a PostgreSQL database started with [Testcontainers](https://testcontainers.com/).

Requirements: a running Docker daemon (Docker Desktop, Colima, Rancher Desktop or a native Docker
Engine) that your user can access. No local PostgreSQL install is needed; the container is started
and removed automatically.

    ./gradlew integrationTest

Run a single class or method:

    ./gradlew integrationTest --tests 'io.spring.integration.RealWorldApiPostgresIT'
    ./gradlew integrationTest --tests '*ArticleQueryServicePostgresIT.should_paginate*'

The suite uses the `postgres:15-alpine` image by default. Use `-PpostgresImage` to test another
PostgreSQL version or to pull from a registry mirror (for example when Docker Hub rate-limits you):

    ./gradlew integrationTest -PpostgresImage=postgres:16-alpine
    ./gradlew integrationTest -PpostgresImage=mirror.gcr.io/library/postgres:15-alpine

The integration tests are not part of `./gradlew test` or `./gradlew build`, so the default build
still works without Docker. Run both suites with `./gradlew test integrationTest`. Test reports are
written to `build/reports/tests/integrationTest/index.html`.

To add a test, extend `io.spring.integration.PostgresIntegrationTest` and name the class `*IT`. All
subclasses share one PostgreSQL container and Spring context, and every table is truncated before
each test.

# Code format

Use spotless for code format.

    ./gradlew spotlessJavaApply

# Help

Please fork and PR to improve the project.
