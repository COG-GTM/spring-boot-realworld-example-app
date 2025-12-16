# Java 21 Migration Rollback Procedures

This document outlines the rollback procedures for the Java 11 to Java 21 migration of the spring-boot-realworld-example-app.

## Overview

The migration is being performed in phases to minimize risk and allow for easy rollback at any stage. This document provides step-by-step instructions for reverting changes if issues are encountered during or after the migration.

## Phase 1: Migration Foundation (Current Phase)

### Changes Made in Phase 1

Phase 1 includes the following changes that prepare the build environment for Java 21:

1. Gradle wrapper upgraded from 7.4 to 8.5
2. Spring Boot upgraded from 2.6.3 to 2.7.18
3. Plugin versions updated for Gradle 8.5 compatibility:
   - io.spring.dependency-management: 1.0.11.RELEASE to 1.1.4
   - com.netflix.dgs.codegen: 5.0.6 to 5.12.4
   - com.diffplug.spotless: 6.2.1 to 6.25.0
4. Netflix DGS runtime upgraded from 4.9.21 to 5.5.1
5. graphql-java explicitly set to 19.2 for DGS compatibility
6. Code changes to GraphQL datafetchers for PageInfo type compatibility
7. Spotless configuration updated to target only src directory

### Rollback Procedure for Phase 1

To rollback Phase 1 changes, follow these steps:

1. **Checkout the master branch:**
   ```bash
   git checkout master
   ```

2. **Delete the migration branch (if no longer needed):**
   ```bash
   git branch -D feature/java21-migration
   ```

3. **Verify the build works:**
   ```bash
   ./gradlew clean build
   ```

### Partial Rollback (Specific Changes)

If you need to rollback specific changes while keeping others:

#### Rollback Gradle Wrapper Only
```bash
# Revert gradle-wrapper.properties
git checkout master -- gradle/wrapper/gradle-wrapper.properties
# Re-download the old Gradle wrapper
./gradlew wrapper
```

#### Rollback build.gradle Changes Only
```bash
git checkout master -- build.gradle
```

#### Rollback GraphQL Code Changes Only
```bash
git checkout master -- src/main/java/io/spring/graphql/ArticleDatafetcher.java
git checkout master -- src/main/java/io/spring/graphql/CommentDatafetcher.java
```

## Phase 2: Spring Boot 3.x Upgrade (Future)

### Expected Changes
- Spring Boot upgrade from 2.7.x to 3.x
- Jakarta EE namespace migration (javax.* to jakarta.*)
- MyBatis Spring Boot Starter upgrade to 3.x
- Netflix DGS upgrade to Spring Boot 3 compatible version

### Rollback Procedure
To rollback Phase 2 changes:
```bash
git checkout feature/java21-migration-phase1 -- .
./gradlew clean build
```

## Phase 3: Java 21 Upgrade (Future)

### Expected Changes
- sourceCompatibility and targetCompatibility changed to '21'
- CI/CD pipeline updated to use JDK 21
- Any Java 21 specific code changes

### Rollback Procedure
To rollback Phase 3 changes:
```bash
# Revert Java version in build.gradle
git checkout feature/java21-migration-phase2 -- build.gradle
# Revert CI/CD changes
git checkout feature/java21-migration-phase2 -- .github/workflows/gradle.yml
./gradlew clean build
```

## Emergency Rollback

In case of critical issues in production, perform a complete rollback:

1. **Revert to the last known good commit:**
   ```bash
   git log --oneline  # Find the last good commit hash
   git revert <commit-hash>
   ```

2. **Or reset to master (destructive):**
   ```bash
   git checkout master
   git branch -D feature/java21-migration
   ```

3. **Redeploy the application using the previous configuration.**

## Testing After Rollback

After any rollback, verify the application works correctly:

1. **Run the full test suite:**
   ```bash
   ./gradlew clean test
   ```

2. **Build the application:**
   ```bash
   ./gradlew clean build
   ```

3. **Start the application locally and verify basic functionality:**
   ```bash
   ./gradlew bootRun
   ```

4. **Test critical endpoints:**
   - User registration and login
   - Article creation and retrieval
   - GraphQL queries

## Contact

If you encounter issues during rollback, contact the development team for assistance.

## Version History

| Version | Date | Description |
|---------|------|-------------|
| 1.0 | 2025-12-16 | Initial rollback procedures for Phase 1 |
