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

You'll need Java 17 installed.

    ./gradlew bootRun

To test that it works, open a browser tab at http://localhost:8080/tags .  
Alternatively, you can run

    curl http://localhost:8080/tags

# Java 17 Migration

This project has been updated to require Java 17 as a prerequisite. Java 17 is a Long Term Support (LTS) release that provides improved performance, security, and language features compared to Java 11.

## Migration Changes

The following changes were made during the Java 17 migration process:

### Build Configuration
- **Gradle compatibility**: Updated `sourceCompatibility` and `targetCompatibility` from Java 11 to Java 17 in `build.gradle`
- **Build tools**: Ensured Gradle version compatibility with Java 17

### CI/CD Pipeline
- **GitHub Actions**: Updated the CI workflow to use Java 17 instead of Java 11 in `.github/workflows/gradle.yml`
- **Build environment**: Updated the JDK setup action to use Java 17 distribution

### Dependency Compatibility
- **Spring Boot**: The current Spring Boot version 2.6.3 fully supports Java 17
- **Framework compatibility**: All major dependencies (MyBatis, Netflix DGS, JWT libraries) are compatible with Java 17
- **Future-proofing**: Aligns with Spring Framework 6 and Spring Boot 3 baseline requirements

### Benefits of Java 17
- **Long Term Support**: Java 17 is an LTS release supported until September 2029
- **Performance improvements**: Enhanced garbage collection and runtime optimizations
- **Language features**: Access to modern Java language features and APIs
- **Security**: Latest security updates and improvements
- **Spring ecosystem**: Better alignment with future Spring Framework versions

### Migration Verification
To verify the Java 17 migration:
1. Ensure Java 17 is installed: `java -version`
2. Run the application: `./gradlew bootRun`
3. Execute tests: `./gradlew test`
4. Check code formatting: `./gradlew spotlessJavaApply`

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
