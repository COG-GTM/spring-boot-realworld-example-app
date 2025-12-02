# Java 8 to 11 Migration Notes

This document summarizes the changes made to migrate the Spring Boot RealWorld Example App to Java 11.

## Build Configuration Changes

### Gradle Wrapper Upgrade
- Upgraded from Gradle 7.4 to Gradle 7.6.4 (latest 7.x series)
- Gradle 8.x is not compatible with Spring Boot 2.6.x due to plugin API changes
- The new wrapper provides better Java 11+ toolchain support and improved performance

### Plugin Updates
- Updated `io.spring.dependency-management` from 1.0.11.RELEASE to 1.0.15.RELEASE
- Updated `com.diffplug.spotless` from 6.2.1 to 6.25.0

### Java Toolchain Configuration
Added explicit Java toolchain configuration in `build.gradle`:
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}
```

This ensures consistent Java 11 compilation across different development environments using Gradle's toolchain feature.

## CI/CD Changes

### GitHub Actions Workflow
Updated `.github/workflows/gradle.yml`:
- Upgraded `actions/checkout` from v2 to v4
- Upgraded `actions/setup-java` from v2 to v4
- Changed JDK distribution from Zulu to Temurin (Eclipse Adoptium)
- Enabled built-in Gradle caching via `cache: gradle`
- Simplified workflow by removing manual cache configuration

## Removed JDK Modules Analysis

The following Java EE modules were removed in Java 11 and were checked for usage:
- `javax.xml.bind` (JAXB) - Not used
- `javax.xml.ws` (JAX-WS) - Not used
- `javax.activation` - Not used
- `javafx` - Not used
- `javax.annotation` - Not used (Spring provides its own)
- CORBA (`org.omg`) - Not used
- Nashorn JavaScript engine - Not used

No additional dependencies were required as the project does not use any of these removed modules.

## Runtime Considerations

### JVM Options
No special JVM options are required for this migration. The application runs on the classpath (no JPMS module-info.java).

### GC and Logging
Java 11 uses G1 as the default garbage collector. If you need GC logging, use the unified logging format:
```bash
-Xlog:gc*:file=gc.log:time,uptime,level,tags
```

### TLS/Security
Java 11 enables TLS 1.3 by default. If you encounter issues with legacy endpoints, you can temporarily pin to TLS 1.2:
```bash
-Djdk.tls.client.protocols=TLSv1.2
```

## Dependencies

All existing dependencies are compatible with Java 11:
- Spring Boot 2.6.3
- MyBatis Spring Boot Starter 2.2.2
- Netflix DGS Framework 4.9.21
- Flyway Core
- JJWT 0.11.2
- SQLite JDBC 3.36.0.3
- Lombok
- Rest Assured 4.5.1

## Testing

All existing tests pass on Java 11. The test framework uses JUnit 5 (JUnit Platform).

## Known Issues

None identified during migration.

## Follow-up Recommendations

1. Consider upgrading to Spring Boot 3.x in a future migration (requires Java 17+)
2. Consider migrating from Joda-Time to java.time API
3. Monitor for any illegal reflective access warnings in logs and address by upgrading affected libraries
