# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/COG-GTM/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/COG-GTM/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# *NEW* GraphQL Support  

Following some DDD principles. REST or GraphQL is just a kind of adapter. And the domain layer will be consistent all the time. So this repository implement GraphQL and REST at the same time.

The GraphQL schema is https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/src/main/resources/schema/schema.graphqls and the visualization looks like below.

![](graphql-schema.png)

And this implementation is using [dgs-framework](https://github.com/Netflix/dgs-framework) which is a quite new java graphql server framework.
# How it works

The application uses Java 21 and Spring Boot 3.5 (Web, Security, MyBatis, Netflix DGS for GraphQL).

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

You'll need Java 21 installed (the Gradle toolchain is pinned to 21).

    ./gradlew bootRun

GraphiQL is available at http://localhost:8080/graphiql and the GraphQL endpoint at http://localhost:8080/graphql .

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

# Java 11 → 21 migration

The project was migrated from Java 11 / Spring Boot 2.6 to Java 21 / Spring Boot 3.5. Summary of what changed:

| Component | Before | After |
|---|---|---|
| Java | 11 | 21 (Gradle toolchain) |
| Gradle wrapper | 7.4 | 8.14.3 |
| Spring Boot | 2.6.3 | 3.5.9 (Spring Framework 6.2, Spring Security 6.5) |
| Spring Dependency Management plugin | 1.0.11 | 1.1.7 |
| Netflix DGS | 4.9.21 (`graphql-dgs-spring-boot-starter`) | 10.2.1 (`graphql-dgs-spring-graphql-starter`, on top of Spring for GraphQL) |
| DGS codegen plugin | 5.0.6 | 8.6.0 |
| MyBatis Spring Boot starter | 2.2.2 | 3.0.4 |
| JJWT | 0.11.2 | 0.12.6 |
| sqlite-jdbc | 3.36.0.3 | 3.49.1.0 |
| joda-time | 2.10.13 | 2.13.1 |
| REST Assured | 4.5.1 | managed by Spring Boot BOM (5.5.x) |
| Spotless plugin | 6.2.1 | 8.10.2 |

Breaking changes handled:

* `javax.servlet.*` / `javax.validation.*` → `jakarta.*` (Jakarta EE 10).
* Spring Security 6: `WebSecurityConfigurerAdapter` replaced by a `SecurityFilterChain` bean using the lambda DSL and `authorizeHttpRequests`/`requestMatchers`; permitted paths are unchanged.
* `ResponseEntityExceptionHandler#handleMethodArgumentNotValid` now takes `HttpStatusCode`.
* JJWT 0.12 builder/parser API (`subject`, `expiration`, `parser().verifyWith(...)`, `parseSignedClaims`).
* graphql-java 22+: `DataFetcherExceptionHandler#onException` → asynchronous `handleException`.
* DGS codegen no longer maps `PageInfo` to `graphql.relay.PageInfo` automatically; an explicit `typeMapping` keeps the previous behaviour.
* The GraphQL HTTP endpoint is now served by Spring for GraphQL, which decodes requests with the application's Jackson `ObjectMapper`. Because the REST API enables `UNWRAP_ROOT_VALUE`, `GraphQLHttpConfig` registers a dedicated converter for `/graphql` with unwrapping disabled.
* `@MockBean` (deprecated for removal) → `@MockitoBean` in tests.
* Spotless now targets `src/**/*.java` only (Gradle 8 rejects the previous implicit dependency on compiler outputs).

# Help

Please fork and PR to improve the project.
