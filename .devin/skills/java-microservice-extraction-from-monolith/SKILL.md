---
name: java-microservice-extraction-from-monolith
description: "Converted from Devin playbook: Java Microservice Extraction from Monolith"
triggers:
  - user
  - model
---

# Java Microservice Extraction from Monolith

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Java Microservice Extraction from Monolith (playbook-878a10ceddec411e982e43b19cbda385)

## Procedure

# Playbook: Java Microservice Extraction from Monolith

## Overview

Extract a service module from a Java monolith into a standalone Spring Boot microservice. The extracted service will run independently with its own database (using H2 in-memory for development) while the monolith communicates with it via HTTP instead of direct method calls. This playbook stops before deployment.

## What's Needed From User

- **Repository URL**: The monolith repository to work with
- **Service to extract**: Name of the service module to extract (e.g., "Consumer Service", "Order Service")
- **Service location details**: 
  - Path to the service's domain classes (e.g., `ftgo-consumer-service/`)
  - Path to the service's API module if it exists (e.g., `ftgo-consumer-service-api/`)
  - Location of shared domain entities that need to be moved
- **Integration points**: Which other services in the monolith call this service and how (method names, parameters)
- **Database schema**: Location of existing database migrations or schema definitions

## Procedure

1. **Explore the codebase to understand the service structure**
   - Identify the service's domain classes, controllers, and configuration
   - Find all dependencies the service has on other modules
   - Identify all callers of the service within the monolith
   - Locate the database schema for tables owned by this service
   - Check for foreign key constraints that reference or are referenced by the service's tables

2. **Create a new git branch for the extraction work**

3. **Create the standalone application module**
   - Create a new module directory (e.g., `{service-name}-application/`)
   - Create `build.gradle` with dependencies:
     - The existing service module
     - The service's API module (if exists)
     - Common/shared modules
     - `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-actuator`
     - `com.h2database:h2` for in-memory database
     - `org.flywaydb:flyway-core` for migrations
   - Add the new module to `settings.gradle`

4. **Create the Spring Boot main application class**
   - Use `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`
   - Add `@EntityScan` and `@EnableJpaRepositories` pointing to the service's domain package
   - Import the service's web configuration class

5. **Configure application properties**
   - Set a unique server port (e.g., 8082) to avoid conflicts with the monolith
   - Configure H2 in-memory database with `DB_CLOSE_DELAY=-1` to persist data during runtime
   - Set `spring.jpa.hibernate.ddl-auto=none` and enable Flyway
   - Enable H2 console for debugging
   - Expose actuator health endpoints

6. **Create Flyway database migrations**
   - Create `src/main/resources/db/migration/V1__create_{service}_db.sql`
   - Copy the relevant table definitions from the monolith's schema
   - Include the `hibernate_sequence` table if using `@GeneratedValue`
   - Remove MySQL-specific syntax (e.g., `engine = InnoDB`) for H2 compatibility

7. **Move domain entities to the service module**
   - Copy entity classes from the shared domain module to the service's domain package
   - Update package declarations and imports
   - Ensure the entity uses the service's own repository interface
   - Keep the original entity in the shared module if other services still need it

8. **Create the HTTP client proxy in the calling service**
   - Create a new `client` package in the calling service
   - Create a proxy class (e.g., `{ServiceName}ServiceProxy`) that:
     - Takes a `RestTemplate` and base URL in the constructor
     - Implements methods matching the original service interface
     - Makes HTTP calls to the extracted service's REST endpoints
     - Maps HTTP 404 responses to the appropriate `NotFoundException`
     - Wraps other HTTP errors in appropriate domain exceptions
   - Create a configuration class that:
     - Reads the service URL from properties (with localhost default)
     - Configures `RestTemplate` with connect and read timeouts (e.g., 5 seconds)
     - Creates the proxy bean

9. **Update the calling service to use the HTTP client**
   - Modify the calling service's configuration to inject the proxy instead of the direct service
   - Update the service class constructor to accept the proxy interface
   - Remove the direct dependency on the extracted service module from `build.gradle`
   - Add the service API module as a dependency (for shared DTOs and exceptions)

10. **Move exception classes to the API module for shared access**
    - If exceptions are thrown by the service and caught by callers, move them to the API module
    - Update imports in both the service and calling modules

11. **Run lint checks and verify the build compiles**
    - Run `./gradlew compileJava` (excluding any problematic test modules)
    - Fix any compilation errors before proceeding

12. **Run tests to verify functionality**
    - Run `./gradlew test` (excluding end-to-end tests that require full deployment)
    - Fix any test failures

13. **Commit changes and create a PR**
    - Stage all changes and commit with a descriptive message
    - Push the branch and create a pull request
    - Wait for CI checks to pass

14. **Test locally that both services can run and communicate**
    - Start the extracted microservice: `./gradlew :{service-application}:bootRun`
    - Verify it starts on the configured port
    - Test the REST endpoints directly (e.g., create and retrieve entities)
    - If the monolith needs to call the service, start it and test the integration

## Specifications

- The extracted microservice runs as a standalone Spring Boot application on a unique port
- The microservice uses H2 in-memory database with Flyway migrations
- The monolith communicates with the extracted service via HTTP REST calls
- HTTP client includes timeout configuration (5 second connect/read timeouts)
- HTTP client properly maps error responses to domain exceptions
- Both services can run simultaneously without port conflicts
- All existing tests pass after the extraction
- **Do NOT proceed with**: Docker configuration, Kubernetes manifests, CI/CD pipeline setup, or actual deployment
- **Validation**: Start the extracted microservice, create a test entity via its REST API, then verify the monolith can successfully call the service via HTTP

## Advice and Pointers

- **Port selection**: Use ports in the 808x range (8081, 8082, etc.) to avoid conflicts
- **Entity duplication**: It's acceptable to have the entity in both the shared domain module and the service module during transition; this maintains backward compatibility
- **H2 compatibility**: Remove MySQL-specific syntax like `engine = InnoDB` from migrations when using H2
- **Exception handling**: The HTTP client should translate HTTP status codes to domain exceptions so callers don't need to change their error handling logic
- **Timeout configuration**: Make timeouts configurable via properties with sensible defaults
- **Logging**: Add logging to the HTTP client for debugging communication issues
- **Validation methods**: If the original service has validation methods that don't return data, the HTTP client can call a GET endpoint to verify the entity exists

## Forbidden Actions

- Do not deploy the microservice to any environment
- Do not create Docker or Kubernetes configuration
- Do not modify the monolith's deployment scripts
- Do not remove the original service from the monolith if other parts still depend on it directly
- Do not create real database instances; use H2 in-memory for the extracted service

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
