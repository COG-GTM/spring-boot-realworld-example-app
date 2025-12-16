# Environment Parity Documentation

This document describes how development, staging, and production environments should be configured to maintain parity during the Java 11 to Java 21 migration.

## Overview

Maintaining environment parity is critical during the migration to ensure that code behaves consistently across all environments. This document outlines the configuration requirements for each environment.

## Current Environment Configuration (Pre-Migration)

### Java Version
- **Development**: Java 11 (OpenJDK or Zulu)
- **CI/CD**: Java 11 (Zulu distribution via GitHub Actions)
- **Staging**: Java 11
- **Production**: Java 11

### Build Tool
- **Gradle**: 8.5 (upgraded from 7.4 in Phase 1)

### Spring Boot Version
- **Version**: 2.7.18 (upgraded from 2.6.3 in Phase 1)

## Phase 1 Environment Configuration (Current)

After Phase 1, all environments should be configured as follows:

### Development Environment

```bash
# Required Java version
java -version
# Should output: openjdk version "11.x.x" or similar

# Gradle wrapper version (automatically managed)
./gradlew --version
# Should output: Gradle 8.5

# Build and test
./gradlew clean build
```

### CI/CD Environment (GitHub Actions)

The `.github/workflows/gradle.yml` file should be configured as:

```yaml
- name: Set up JDK 11
  uses: actions/setup-java@v2
  with:
    distribution: zulu
    java-version: '11'
```

### Staging Environment

- **Java**: OpenJDK 11 or Zulu 11
- **Memory**: Minimum 512MB heap
- **Database**: SQLite (same as development)

### Production Environment

- **Java**: OpenJDK 11 or Zulu 11
- **Memory**: Recommended 1GB+ heap
- **Database**: SQLite (or configured database)

## Phase 2 Environment Configuration (Future - Spring Boot 3.x)

After Phase 2, environments will need to support Spring Boot 3.x:

### Development Environment

```bash
# Java 17 minimum required for Spring Boot 3.x
java -version
# Should output: openjdk version "17.x.x" or higher
```

### CI/CD Environment

```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    distribution: zulu
    java-version: '17'
```

## Phase 3 Environment Configuration (Future - Java 21)

After Phase 3, all environments should run Java 21:

### Development Environment

```bash
# Required Java version
java -version
# Should output: openjdk version "21.x.x"

# Verify build
./gradlew clean build
```

### CI/CD Environment

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: zulu
    java-version: '21'
```

### Staging Environment

- **Java**: OpenJDK 21 or Zulu 21
- **Memory**: Minimum 512MB heap
- **JVM Options**: Consider enabling virtual threads if applicable

### Production Environment

- **Java**: OpenJDK 21 or Zulu 21
- **Memory**: Recommended 1GB+ heap
- **JVM Options**: 
  - Consider enabling virtual threads: `--enable-preview` (if using preview features)
  - Review GC settings for Java 21

## Environment Variables

### Required Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JAVA_HOME` | Path to JDK installation | System dependent |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |

### Optional Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | `8080` |
| `LOGGING_LEVEL_ROOT` | Root logging level | `INFO` |

## Database Configuration

The application uses SQLite by default. For environment parity:

### Development
```properties
# application.properties
spring.datasource.url=jdbc:sqlite:dev.db
```

### Staging
```properties
spring.datasource.url=jdbc:sqlite:staging.db
```

### Production
```properties
# Configure appropriate database for production
spring.datasource.url=jdbc:sqlite:prod.db
# Or use a different database like MySQL/PostgreSQL
```

## Verification Checklist

Before deploying to any environment, verify:

- [ ] Java version matches the expected version for the current phase
- [ ] Gradle wrapper version is 8.5
- [ ] All tests pass: `./gradlew clean test`
- [ ] Application starts successfully: `./gradlew bootRun`
- [ ] Health endpoint responds (if configured)
- [ ] GraphQL endpoint is accessible
- [ ] REST API endpoints are functional

## Troubleshooting

### Common Issues

1. **Java version mismatch**
   - Verify `JAVA_HOME` points to the correct JDK
   - Check `java -version` output

2. **Gradle wrapper issues**
   - Delete `.gradle` directory and re-run
   - Verify `gradle-wrapper.properties` has correct version

3. **Dependency conflicts**
   - Run `./gradlew dependencies` to check dependency tree
   - Look for version conflicts in the output

4. **Spring Boot startup failures**
   - Check application logs for detailed error messages
   - Verify all required environment variables are set

## Migration Testing Strategy

### Before Migration
1. Document current behavior in all environments
2. Create baseline performance metrics
3. Ensure all tests pass

### During Migration
1. Test each phase in development first
2. Deploy to staging and run full test suite
3. Perform smoke tests on critical functionality
4. Monitor for any regressions

### After Migration
1. Compare performance metrics with baseline
2. Verify all functionality works as expected
3. Monitor production for any issues

## Contact

For environment configuration questions, contact the development team.

## Version History

| Version | Date | Description |
|---------|------|-------------|
| 1.0 | 2025-12-16 | Initial environment parity documentation for Phase 1 |
