---
name: add-opentelemetry-logging-to-application
description: "Converted from Devin playbook: Add OpenTelemetry Logging to Application"
triggers:
  - user
  - model
---

# Add OpenTelemetry Logging to Application

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Add OpenTelemetry Logging to Application (playbook-41158215d7b841a0aeec02e1fcb12481), macro `!opentel`

## Procedure

# Playbook: Add OpenTelemetry Logging to Application

## Overview
Add OpenTelemetry-style tracing and logging to an application to enable observability of critical operations. This playbook covers both **frontend JavaScript applications** (vanilla JS or with build systems like React, Vue, Angular) and **Java applications** (Spring Boot or standard Java). Follow the appropriate section based on your repository type.

## What's Needed From User
- Repository URL and branch to work on
- **For Frontend**: (Optional) List of critical operations to instrument - if not provided, these will be automatically identified
- **For Frontend**: (Optional) Preferred exporter type: console logging (default) or OTLP exporter for production
- **For Java**: Build tool in use (Maven or Gradle)
- **For Java**: Spring Boot version (if applicable) - determines which integration approach to use
- **For Java**: Java version (Java 17+ recommended for Spring Boot 3.x)
- **For Java**: (Optional) Target observability backend (e.g., Jaeger, Zipkin, or OTLP collector endpoint)
- **For Java**: (Optional) Specific classes or methods to prioritize for instrumentation

## Procedure

### Step 1: Clone and Explore the Codebase

1. Clone the repository and explore the codebase structure
2. Determine the application type:
   - **Frontend**: Look for `index.html`, `package.json`, JavaScript/TypeScript files
   - **Java**: Look for `pom.xml` (Maven) or `build.gradle` (Gradle)
3. If the repository is a **frontend application**, proceed to **Section A: Frontend Instrumentation**
4. If the repository is a **Java application**, proceed to **Section B: Java Instrumentation**

---

## Section A: Frontend Instrumentation

Follow these steps for JavaScript/TypeScript frontend applications.

### A1. Identify the project type and entry point
- Determine if the project uses a build system (npm/yarn with bundler) or is vanilla JavaScript
- Locate the main application entry point and JavaScript/TypeScript files
- Check for existing logging or telemetry implementations

### A2. Identify critical operations to instrument
- If the user provided a list of operations, use those
- If not provided, analyze the codebase to identify critical operations by searching for:
  - **Authentication flows**: Functions/handlers with names containing "login", "logout", "auth", "signIn", "signOut", "register"
  - **Form submissions**: Event listeners for "submit" events, form handler functions
  - **API calls**: `fetch()`, `axios`, `XMLHttpRequest`, or other HTTP client usage
  - **Financial/data operations**: Functions handling transfers, payments, orders, or CRUD operations
  - **User actions**: Click handlers on important buttons (submit, delete, save, update, create)
  - **Navigation/routing**: Route change handlers, page transition functions
- Prioritize operations that: modify data, involve user authentication, handle sensitive information, or are core to the application's purpose
- Document the identified operations before proceeding with instrumentation

### A3. Set up OpenTelemetry based on project type
- **For projects with build systems (npm/yarn):** Install OpenTelemetry packages via npm:
  ```bash
  npm install @opentelemetry/api @opentelemetry/sdk-trace-web @opentelemetry/resources @opentelemetry/exporter-trace-otlp-http
  ```
- **For vanilla JavaScript projects:** Implement a lightweight self-contained tracing class that outputs OpenTelemetry-compatible JSON to the console (CDN-based OpenTelemetry bundles often have issues with global variable exposure)

### A4. Create the tracer initialization code
- Configure the tracer provider with service name and version
- Set up the appropriate span exporter (ConsoleSpanExporter for testing, OTLP exporter for production)
- Initialize the tracer at application startup before any instrumented code runs

### A5. Instrument critical operations with spans
- Wrap each critical operation in a span using `tracer.startSpan(operationName)`
- Add relevant attributes to spans (e.g., user ID, transaction amount, operation status)
- Set span status to OK or ERROR based on operation outcome
- Record exceptions when errors occur using `span.recordException(error)`
- Always call `span.end()` when the operation completes (success or failure)

### A6. Ensure spans include OpenTelemetry-compliant fields
- `traceId`: 32-character hex string for distributed tracing correlation
- `spanId`: 16-character hex string for span identification
- `name`: Operation name (e.g., "user.login", "transaction.transfer")
- `startTime` and `endTime`: ISO 8601 timestamps
- `duration`: Time elapsed in milliseconds
- `status`: OK, ERROR, or UNSET
- `attributes`: Key-value pairs with operation-specific data

### A7. Run the application locally and verify logging works
- Start the application using the appropriate dev server (e.g., `npm run dev`, `npx serve`) or by opening the HTML file directly
- Open browser developer tools (F12) and navigate to the Console tab
- Perform each instrumented operation (login, transactions, form submissions, etc.)
- Verify that spans appear in the console with green "[OTEL SPAN]" prefix followed by the operation name

### A8. Validate OpenTelemetry format compliance for each span
- **Required fields check:** Expand each span log entry and confirm it contains:
  - `traceId`: 32-character hexadecimal string (e.g., "b2820622a1b2c3d4e5f6...")
  - `spanId`: 16-character hexadecimal string (e.g., "9d869b2d1a2b3c4d")
  - `name`: Operation name matching the instrumented function (e.g., "user.login")
  - `startTime`: ISO 8601 timestamp (e.g., "2024-01-15T10:30:00.000Z")
  - `endTime`: ISO 8601 timestamp after startTime
  - `duration`: Positive number in milliseconds (e.g., "15ms")
  - `status`: "OK" for successful operations, "ERROR" for failures
  - `attributes`: Object containing operation-specific key-value pairs
- **Success path test:** Perform a successful operation and verify status is "OK"
- **Failure path test:** Trigger an error condition (e.g., invalid login) and verify status is "ERROR" with appropriate error attributes
- **Attributes validation:** Confirm attributes contain meaningful data (e.g., for login: username; for transfer: amount, sender, receiver)
- For OTLP exporters, verify spans are being sent to the configured backend by checking the observability platform

### A9. Proceed to Step 2: Commit and Create PR

---

## Section B: Java Instrumentation

Follow these steps for Java/Spring Boot applications.

### B1. Explore the codebase structure
- Identify the build tool (pom.xml for Maven, build.gradle for Gradle)
- Determine the Spring Boot version (if applicable)
- Locate the main application class, controllers, services, and repositories
- Check for existing logging framework (SLF4J, Log4j, Logback)

### B2. Add OpenTelemetry dependencies to the build file
- For Maven (pom.xml), add version property and dependencies:
  ```xml
  <properties>
    <opentelemetry.version>1.40.0</opentelemetry.version>
  </properties>
  ```
- Core dependencies: `opentelemetry-api`, `opentelemetry-sdk`, `opentelemetry-exporter-otlp`, `opentelemetry-sdk-extension-autoconfigure`
- For Spring Boot 3.x: Add `micrometer-tracing-bridge-otel` for native integration
- Add `opentelemetry-instrumentation-annotations` for `@WithSpan` and `@SpanAttribute`
- Add `spring-boot-starter-actuator` for metrics endpoints
- Add `spring-boot-starter-aop` if using AOP-based repository tracing

### B3. Configure OpenTelemetry in application.properties (or application.yml)
- Set service name: `spring.application.name=your-service-name`
- Enable tracing: `management.tracing.enabled=true`
- Configure OTLP endpoint: `management.otlp.tracing.endpoint=http://localhost:4318/v1/traces`
- Set sampling rate: `management.tracing.sampling.probability=1.0` (100% for dev/test)
- Enable log correlation pattern: `logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]`
- Expose actuator endpoints: `management.endpoints.web.exposure.include=health,info,metrics`

### B4. Create OpenTelemetry configuration class
- Create a `@Configuration` class that provides a `Tracer` bean
- Enable AspectJ auto-proxy if using AOP: `@EnableAspectJAutoProxy`

### B5. Instrument the Controller layer
- Add SLF4J Logger to each controller
- Add `@WithSpan` annotations to REST endpoint methods
- Use `@SpanAttribute` on method parameters to capture request data
- Add structured logging at entry/exit points with relevant context

### B6. Instrument the Service layer
- Add SLF4J Logger to each service class
- Add `@WithSpan` annotations to business logic methods
- Use `Span.current()` to add custom attributes and events
- Add span events for critical execution branches (e.g., "Balance validated", "Transaction created")
- Set span status on success/failure: `span.setStatus(StatusCode.OK)` or `span.setStatus(StatusCode.ERROR)`
- Log at appropriate levels: INFO for operations, DEBUG for details, ERROR for exceptions

### B7. Instrument the Repository layer
- For Spring Data JPA repositories (interfaces), create an AOP aspect to trace repository calls
- The aspect should capture method names, arguments, and execution time
- Alternatively, rely on auto-instrumentation if using OpenTelemetry Java Agent

### B8. Update the main application class
- Add `@EnableAspectJAutoProxy` if using AOP
- Add startup logging to confirm OpenTelemetry is configured

### B9. Verify the implementation by running the application locally
- Build the application: `mvn clean compile` or `./gradlew build`
- Start the application: `mvn spring-boot:run` or `./gradlew bootRun`
- Make test API calls using curl or a REST client
- Verify logs show trace IDs in the format: `[service-name,traceId,spanId]`
- Verify structured log messages appear at controller, service, and repository layers
- Check that span attributes and events are logged correctly

### B10. Proceed to Step 2: Commit and Create PR

---

### Step 2: Commit Changes and Create PR

1. Stage all modified files
2. Write a descriptive commit message explaining the OpenTelemetry instrumentation added
3. Push to a feature branch and create a PR

## Specifications

### Frontend Specifications
- All critical operations (user-provided or auto-identified) must have span instrumentation
- Span output must be OpenTelemetry-compliant with traceId, spanId, timestamps, duration, status, and attributes
- Console logging must be visually distinguishable (colored output with "[OTEL SPAN]" prefix)
- The implementation must not break existing application functionality
- For vanilla JS apps, use a self-contained tracing implementation rather than CDN bundles (which have reliability issues)

**Frontend Validation:** Run the application locally and perform the following verification steps:
1. Execute each instrumented operation (both success and failure paths)
2. Capture screenshots or screen recordings of the browser console showing the OpenTelemetry spans
3. Verify each span contains: traceId (32 hex chars), spanId (16 hex chars), name, startTime, endTime, duration, status (OK/ERROR), and relevant attributes
4. Confirm success operations show status "OK" and failure operations show status "ERROR" with error details
5. Share the console output evidence with the user to demonstrate the instrumentation is working correctly

### Java Specifications
- All controller endpoints have `@WithSpan` annotations and structured logging
- All service methods have tracing with span attributes and events for critical branches
- Repository layer has tracing (via AOP aspect or auto-instrumentation)
- application.properties contains OpenTelemetry configuration
- Log output includes trace IDs for correlation
- Application compiles and runs successfully with instrumentation active

**Java Validation:** Run the application locally and execute test API calls. Verify that:
1. Application starts without errors and shows "OpenTelemetry tracing is enabled" message
2. Console logs show trace IDs in the format `[service-name,traceId,spanId]`
3. Structured log messages appear for each layer (controller, service, repository)
4. Different API calls produce different trace IDs, but logs within the same request share the same trace ID

Example verification commands:
```bash
# Start the application
mvn spring-boot:run

# In another terminal, make test API calls
curl -X POST "http://localhost:8080/api/endpoint?param=value"
curl -X GET "http://localhost:8080/api/endpoint/123"
```

Example of successful log output showing trace correlation:
```
2026-01-08T00:05:41.169Z  INFO [my-service,abc123def456,span789] c.e.controller.MyController : Processing request: id=123
2026-01-08T00:05:41.170Z  INFO [my-service,abc123def456,span790] c.e.service.MyService : Executing business logic: id=123
2026-01-08T00:05:41.175Z DEBUG [my-service,abc123def456,span791] c.e.aspect.RepositoryTracingAspect : Repository call: findById(123)
2026-01-08T00:05:41.235Z  INFO [my-service,abc123def456,span790] c.e.service.MyService : Operation completed successfully
```
Note: The trace ID (`abc123def456`) is consistent across all log lines for the same request.

## Advice and Pointers

### Frontend-Specific Advice
- For vanilla JavaScript applications without a build system, CDN-based OpenTelemetry bundles often fail to expose global variables correctly. Use a lightweight self-contained `ConsoleSpanExporter` class instead that outputs OpenTelemetry-compatible JSON.
- Always initialize the tracer before any instrumented code runs - place initialization at the top of the main script file.
- Use descriptive span names following the pattern `domain.operation` (e.g., "user.login", "transaction.transfer", "ui.update").
- Include both success and failure paths in instrumentation - set status to ERROR and record exceptions when operations fail.
- For async operations, ensure `span.end()` is called in both `.then()` and `.catch()` handlers, or use try/finally blocks.

### Java-Specific Advice
- **Spring Boot 3.x vs 2.x**: Spring Boot 3.x has native Micrometer Tracing support. Use `micrometer-tracing-bridge-otel` for seamless integration. For Spring Boot 2.x, use Spring Cloud Sleuth with OpenTelemetry exporter.
- **Version compatibility**: Ensure OpenTelemetry version is compatible with your Spring Boot version. For Spring Boot 3.4+, use OpenTelemetry 1.40.0 or later.
- **Structured logging**: Use SLF4J placeholders (`{}`) instead of string concatenation for better performance and cleaner logs:
  ```java
  // Good
  logger.info("Processing request: id={}, type={}", id, type);
  // Avoid
  logger.info("Processing request: id=" + id + ", type=" + type);
  ```
- **Span attributes**: Add meaningful attributes that help with debugging and analysis:
  - Request identifiers (account numbers, transaction IDs)
  - Operation types (DEPOSIT, WITHDRAWAL, TRANSFER)
  - Amounts and balances (for financial applications)
  - Status codes and error messages
- **Exception handling**: Always record exceptions on spans:
  ```java
  catch (Exception e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
  }
  ```
- **Testing with a collector**: For full observability, run a local collector like Jaeger:
  ```bash
  docker run -p 4318:4318 -p 16686:16686 jaegertracing/all-in-one
  ```
  Then view traces at http://localhost:16686

## Forbidden Actions

### Frontend
- Do not rely solely on CDN-based OpenTelemetry bundles for vanilla JavaScript projects without testing that globals are properly exposed
- Do not skip the verification step - always run the app and confirm spans appear in the console
- Do not leave spans without calling `span.end()` - this causes memory leaks and incomplete telemetry
- Do not instrument trivial operations that would create excessive noise in telemetry data

### Java
- Do not hardcode trace IDs or span IDs - let the framework generate them
- Do not log sensitive data (passwords, API keys, PII) in span attributes or log messages
- Do not set sampling rate to 0 in production without understanding the implications
- Do not remove existing logging - augment it with trace correlation

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
