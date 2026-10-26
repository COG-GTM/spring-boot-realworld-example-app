---
name: springboot-3-to-4
description: Migrate a Spring Boot 3.5.x service to 4.0.x (Spring Framework 7, Security 7, Jackson 3, starter renames, test-starter split, MockitoBean) using OpenRewrite first, then manual fixes. Requires Boot 3.5.x and JDK 17+.
---

# Spring Boot 3.x -> 4.x

Preconditions (stop and report `blocked` if not met): Boot is 3.5.x (if 3.0-3.4, bump to 3.5.x
first in its own PR), JDK 17+, auth already on GS Auth (resource-server) — Security 7 removed the
pre-lambda DSL that legacy adapters rely on.

Target: latest **4.0.x** GA.

## Footprint
`pom.xml`, `src/main/**`, `src/test/**`, `application*.yml`, `README.md`.

## Steps
1. Create branch `platform/06-spring-boot-4`.
2. Mechanical pass (own commit):
   ```bash
   mvn -B -ntp org.openrewrite.maven:rewrite-maven-plugin:run \
     -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:RELEASE \
     -Drewrite.activeRecipes=org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0
   ```
3. Manual pass (own commit). Check each:
   - **Starter renames**: `spring-boot-starter-web` -> `spring-boot-starter-webmvc`;
     `spring-boot-starter-test` is split — add `spring-boot-starter-webmvc-test`,
     `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test` (or whichever slices
     the tests use) alongside the core test starter.
   - **Jackson 3**: `com.fasterxml.jackson.databind.ObjectMapper` -> `tools.jackson.databind.ObjectMapper`
     (`JsonMapper`); annotations stay in `com.fasterxml.jackson.annotation`. `JsonProcessingException`
     is now unchecked (`tools.jackson.core.JacksonException`). Libraries still on Jackson 2 need
     `jackson-databind` 2.x kept on the classpath (Boot 4 manages both).
   - **Tests**: `@MockBean`/`@SpyBean` -> `@MockitoBean`/`@MockitoSpyBean`.
   - **Spring Framework 7**: `HttpStatus` return types -> `HttpStatusCode` where overridden;
     `RestTemplate` still present but prefer `RestClient`; `@Nullable` -> JSpecify
     (`org.jspecify.annotations.Nullable`); `WebMvcConfigurer` path-matching options removed.
   - **Security 7**: only the lambda DSL compiles; `authorizeHttpRequests` required
     (`authorizeRequests` removed); `requestMatchers(String)` requires an MVC `HandlerMappingIntrospector`
     — fine in a webmvc app.
   - **Hibernate 7 / JPA 3.2**: `jakarta.persistence` 3.2; `@GeneratedValue(IDENTITY)` unchanged;
     `hibernate.dialect` auto-detected (remove explicit H2/Oracle dialect properties).
   - **Actuator**: `management.endpoints.web.exposure.include` unchanged; `info` contributors
     off by default.
   - **springdoc**: needs 3.x for Boot 4 (`springdoc-openapi-starter-webmvc-ui` 3.0.x).
4. Validate: `mvn -B -ntp clean verify`; boot the jar; curl `/actuator/health` (200) and a secured
   endpoint without a token (401).
5. README runtime row -> `Java 17, Spring Boot 4.0`. Commit and open PR
   `[platform 6/N] Spring Boot 4.0` with mechanical / manual / behaviour-change sections.

## Done when
Parent 4.0.x, no `com.fasterxml.jackson.databind` imports in app code, tests use `@MockitoBean`,
build green, app boots, PR open.
