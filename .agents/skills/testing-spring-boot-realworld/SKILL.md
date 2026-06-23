---
name: testing-spring-boot-realworld
description: Test the Spring Boot RealWorld example app end-to-end. Use when verifying build, API, or dependency upgrade changes.
---

# Testing the Spring Boot RealWorld Example App

## Prerequisites
- Java 17+ installed (`java -version`)
- No external services or credentials needed — app uses SQLite (file-based DB)

## Quick Commands

| Action | Command |
|--------|---------|
| Run tests | `./gradlew clean test` |
| Boot the app | `./gradlew bootRun` |
| Lint / format | `./gradlew spotlessJavaApply` |
| Build | `./gradlew clean build` |
| Check Gradle version | `./gradlew --version` |

## Test Suite
- ~68 test methods across ~21 test files
- Test types: `@WebMvcTest` API slice tests, `@MybatisTest` repository integration tests, application service tests, unit tests, smoke test
- Test results XML at `build/test-results/test/*.xml`
- Get test count: `find build/test-results/test -name "*.xml" -exec grep -h 'tests=' {} \; | grep -oP 'tests="\K[0-9]+' | paste -sd+ | bc`

## API Endpoints for Manual Verification (no auth required)
- `GET /tags` — returns `{"tags": [...]}`
- `GET /articles` — returns `{"articles": [...], "articlesCount": N}`

## API Endpoints Requiring Auth
- `POST /users` — register: `{"user":{"email":"...","password":"...","username":"..."}}` → returns `{"user":{...,"token":"JWT"}}` (HTTP 201)
- Use the returned JWT as `Authorization: Token <jwt>` header for authenticated endpoints

## Bytecode Version Verification
To confirm Java target version after compilation:
```bash
./gradlew compileJava
javap -verbose build/classes/java/main/io/spring/api/TagsApi.class | grep "major version"
```
- Java 11 = major version 55
- Java 17 = major version 61

## App Startup Verification
- Boot with `./gradlew bootRun`
- Wait for `Started RealWorldApplication` in logs
- App runs on port 8080 by default
- SQLite DB file created at `./dev.db`
- `./gradlew clean` deletes `dev.db`

## Common Issues
- The app uses Lombok — if annotation processing fails, check that Lombok version is >= 1.18.22 for Java 17 compatibility (Spring Boot 2.6.3 BOM provides 1.18.22)
- Gradle deprecation warnings about Gradle 8.0 compatibility are expected with Gradle 7.x and Spring Boot 2.6.x plugins — these are not errors
- If illegal reflective access errors appear under Java 17, add `--add-opens` JVM args in `build.gradle` test block

## Testing Strategy
This is a pure backend API (no frontend UI). All testing is shell-based:
1. Verify bytecode version with `javap`
2. Run full test suite with `./gradlew clean test`
3. Boot app and curl endpoints to verify HTTP responses
4. No GUI recording needed — collect shell output as evidence
