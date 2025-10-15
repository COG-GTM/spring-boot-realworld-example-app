# Migration Guide: Java 11 to Java 17

## Overview
This document describes the migration of the Spring Boot RealWorld application from Java 11 to Java 17, completed in PR #70.

**Migration Date**: October 2025  
**Previous Version**: Java 11, Spring Boot 2.6.3  
**Current Version**: Java 17, Spring Boot 2.7.18  

## Summary of Changes

### 1. Java Runtime Upgrade
- **Java Version**: 11 → 17
- **Updated in**: `build.gradle` (sourceCompatibility and targetCompatibility)
- **CI/CD**: Updated `.github/workflows/gradle.yml` to use JDK 17 with Zulu distribution

### 2. Spring Boot Framework Upgrade
- **Spring Boot**: 2.6.3 → 2.7.18
- **Spring Dependency Management**: 1.0.11.RELEASE → 1.1.4
- **Rationale**: Spring Boot 2.7.18 is the latest 2.x version with long-term support and Java 17 compatibility

### 3. Build Tool Plugin Updates
- **Netflix DGS Codegen**: 5.0.6 → 6.0.3
- **Spotless**: 6.2.1 → 6.23.3

### 4. Dependency Updates

#### MyBatis Spring Boot Starter
- **Version**: 2.2.2 → 2.3.2
- **Note**: Version 3.0.3 was attempted but incompatible with Spring Boot 2.7.18. Version 2.3.2 is the latest compatible version.

#### Netflix DGS (GraphQL)
- **Version**: 4.9.21 (kept at original version)
- **Important**: Explicitly added `platform('com.netflix.graphql.dgs:graphql-dgs-platform:4.9.21')` to prevent Gradle from auto-upgrading to 5.5.1
- **Rationale**: DGS versions 5.x+ require graphql-java 19.2+, but Spring Boot 2.7.18 provides graphql-java 18.5, causing NoSuchMethodError. Pinning the platform BOM prevents version conflicts.

#### JJWT (JWT Library)
- **Version**: 0.11.2 → 0.12.3
- **Breaking Changes**: 
  - `Jwts.parserBuilder()` → `Jwts.parser()`
  - `parseClaimsJws()` → `parseSignedClaims()`
  - Key generation now uses `Keys.hmacShaKeyFor(byte[])`
- **Updated in**: `DefaultJwtService.java`, `DefaultJwtServiceTest.java`

#### REST Assured (Testing)
- **Version**: 4.5.1 → 5.1.1
- **Breaking Changes**: 
  - Static imports updated (e.g., `RestAssuredMockMvc.when()` explicitly imported)
  - Method signatures slightly changed in some test utilities
- **Note**: Version 5.4.0 was attempted but had compatibility issues. Version 5.1.1 is stable with Spring Boot 2.7.18.
- **Updated in**: Test files including `ArticleApiTest.java`, `ProfileApiTest.java`, `ListArticleApiTest.java`, `CommentsApiTest.java`

#### SQLite JDBC
- **Version**: 3.36.0.3 → 3.44.1.0

### 5. Joda Time to java.time Migration

**Removed Dependency**: `joda-time:2.10.13` completely removed from `build.gradle`

**Migration Pattern**:
- `org.joda.time.DateTime` → `java.time.Instant`
- `new DateTime()` → `Instant.now()`
- `ISODateTimeFormat.dateTime().withZoneUTC().print()` → `DateTimeFormatter.ISO_INSTANT.format()`

**Files Modified**:

*Core Domain Entities*:
- `Article.java`: DateTime fields → Instant fields (createdAt, updatedAt)
- `Comment.java`: DateTime fields → Instant fields

*Application Layer*:
- `ArticleData.java`: DateTime → Instant
- `CommentData.java`: DateTime → Instant
- `ArticleQueryService.java`: Joda Time → java.time
- `CommentQueryService.java`: Joda Time → java.time
- `DateTimeCursor.java`: Complete rewrite to use java.time API

*Infrastructure Layer*:
- `DateTimeHandler.java`: MyBatis type handler updated for java.time
- `CommentReadService.java`: Query result handling updated
- `DefaultJwtService.java`: JWT date handling updated

*GraphQL Layer*:
- `ArticleDatafetcher.java`: Date formatting updated
- `CommentDatafetcher.java`: Date formatting updated

*JSON Serialization*:
- `JacksonCustomizations.java`: Updated to serialize Instant properly

*Test Files*:
- `TestHelper.java`: Test fixture generation updated
- `ArticleApiTest.java`: Test assertions and data setup updated
- `ArticlesApiTest.java`: Date handling in tests updated
- `ArticleQueryServiceTest.java`: Test data and assertions updated
- `DefaultJwtServiceTest.java`: JWT test cases updated

### 6. GitHub Actions Workflow Updates
- **Updated**: `.github/workflows/gradle.yml`
- **Changes**:
  - `actions/setup-java@v2` → `actions/setup-java@v3`
  - `actions/checkout@v2` → `actions/checkout@v4`
  - `actions/cache@v2` → `actions/cache@v4` (fixed deprecated version error)
  - Java version: 11 → 17

### 7. Documentation Updates
- **README.md**: Updated Java requirement from "Java 11" to "Java 17"

## Important Notes for Maintainers

### Netflix DGS Version Pinning
The explicit platform BOM import for DGS is critical:
```gradle
implementation platform('com.netflix.graphql.dgs:graphql-dgs-platform:4.9.21')
```
Without this, Gradle will automatically upgrade the platform to 5.5.1, which requires graphql-java 19.2+. Spring Boot 2.7.18 only provides graphql-java 18.5, causing runtime NoSuchMethodError exceptions.

If you need to upgrade DGS in the future:
1. Verify graphql-java version compatibility
2. Consider upgrading to Spring Boot 3.x first, which supports newer DGS versions

### Dependency Version Constraints
- **MyBatis 3.0.x**: Requires Spring Boot 3.x
- **DGS 5.x+**: Requires graphql-java 19.2+
- **REST Assured 5.4.x**: Has compatibility issues with Spring Boot 2.7.18

### Testing Strategy
All 68 tests pass with the migration. Key test areas:
- Article and comment CRUD operations
- JWT authentication and authorization
- GraphQL queries and mutations
- MyBatis database operations
- REST API endpoints

## Verification Steps

### Local Verification
1. Clean build: `./gradlew clean build`
2. Run all tests: `./gradlew test`
3. Code formatting: `./gradlew spotlessJavaApply`
4. Expected results: All 68 tests pass, build successful

### CI/CD Verification
- GitHub Actions workflow runs successfully
- All CI checks pass (build, test)

## Migration Checklist for Future Upgrades

When upgrading to newer Java versions (e.g., Java 21):
- [ ] Update `sourceCompatibility` and `targetCompatibility` in build.gradle
- [ ] Verify Spring Boot version supports target Java version
- [ ] Check all dependencies for Java version compatibility
- [ ] Update GitHub Actions workflow (setup-java version and java-version)
- [ ] Run full test suite locally before pushing
- [ ] Update README.md with new Java requirement
- [ ] Monitor CI/CD pipeline for any runtime issues
- [ ] Review dependency tree for version conflicts: `./gradlew dependencies`

## References
- [Spring Boot 2.7 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.7-Release-Notes)
- [Java 17 Migration Guide](https://docs.oracle.com/en/java/javase/17/migrate/getting-started.html)
- [Joda Time to java.time Migration](https://www.oracle.com/technical-resources/articles/java/jf14-date-time.html)
- [Netflix DGS Framework](https://netflix.github.io/dgs/)

## Troubleshooting

### Common Issues

**Issue**: Tests fail with NoSuchMethodError in Federation.java  
**Solution**: Verify DGS platform BOM is explicitly imported at version 4.9.21 in build.gradle

**Issue**: JWT tests fail with method not found errors  
**Solution**: Ensure JJWT 0.12.3 API changes are applied (`parser()` instead of `parserBuilder()`)

**Issue**: REST Assured tests fail to compile  
**Solution**: Verify static imports are updated to explicitly import `RestAssuredMockMvc.when()`

**Issue**: Date serialization/deserialization errors  
**Solution**: Check that all DateTime references have been migrated to Instant and JacksonCustomizations is properly configured

**Issue**: GitHub Actions fails with "deprecated version" error  
**Solution**: Ensure actions/cache and actions/checkout are updated to v4 in .github/workflows/gradle.yml
