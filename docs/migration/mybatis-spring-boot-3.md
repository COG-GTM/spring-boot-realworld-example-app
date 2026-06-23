# MyBatis Migration Notes — Spring Boot 3.4.1 / Java 21

Component 4 of the Spring Boot 2.6.3 / Java 11 → Spring Boot 3.4.1 / Java 21 migration.

Target: `mybatis-spring-boot-starter` 2.2.2 → 3.0.4 (pulls in MyBatis 3.5.x and
`mybatis-spring` 3.0.x, which is built for Spring 6 / Jakarta).

## Summary

No MyBatis source or XML changes are required. The MyBatis core API (`org.apache.ibatis.*`)
is unchanged between the 3.x line shipped with starter 2.2.2 and starter 3.0.4, and the
application uses only stable, supported constructs. Verification findings below.

## Verification

### XML mappers (`src/main/resources/mapper/*.xml`)
- All 11 mappers declare the MyBatis 3.0 DTD:
  `PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd"`.
  This DTD is still bundled with and valid for MyBatis 3.5.x — no change needed.
- No deprecated constructs are used (no `parameterMap`, `statementType` overrides,
  `flushCache`/`databaseId` quirks, etc.). Only `<select>/<insert>/<update>/<delete>`,
  `<sql>`/`<include>`, `<resultMap>`, and dynamic tags (`<if>`, `<foreach>`, `<where>`) are
  used, all stable across MyBatis 3.x.
- Namespaces match their interfaces (or the standalone `transfer.data` namespace in
  `TransferData.xml`), loaded via `mybatis.mapper-locations`.

### Mapper scanning (`@Mapper` annotations)
- There is no `@MapperScan` and no MyBatis `@Configuration` class. Mappers in
  `io.spring.infrastructure.mybatis.mapper` and
  `io.spring.infrastructure.mybatis.readservice` are each annotated with
  `@org.apache.ibatis.annotations.Mapper` and discovered by
  `mybatis-spring-boot-autoconfigure`'s `AutoConfiguredMapperScannerRegistrar`.
- This auto-configuration mechanism is unchanged in starter 3.0.4 and works under
  Spring Boot 3 — no change needed.

### Type handler (`io.spring.infrastructure.mybatis.DateTimeHandler`)
- Implements `org.apache.ibatis.type.TypeHandler<DateTime>` and is annotated with
  `@MappedTypes(DateTime.class)`. The `TypeHandler` interface signature
  (`setParameter` + the three `getResult` overloads), `@MappedTypes`, and `JdbcType` are
  unchanged in MyBatis 3.5.x.
- Uses only `java.sql.*`, `java.util.*`, and `org.joda.time.DateTime` — there are **no**
  `javax.*` imports, so the Jakarta rename component does not affect this file.
- Registered via `mybatis.type-handlers-package`; package scanning of `@MappedTypes`
  handlers is unchanged.

### `application.properties` mybatis.* keys (owned by the configuration component — NOT edited here)
All existing keys remain valid for MyBatis Spring Boot Starter 3.x. None were renamed or
removed; **no required change** for the configuration component on MyBatis's behalf:

| Key | Status |
| --- | --- |
| `mybatis.configuration.cache-enabled=true` | valid |
| `mybatis.configuration.default-statement-timeout=3000` | valid |
| `mybatis.configuration.map-underscore-to-camel-case=true` | valid |
| `mybatis.configuration.use-generated-keys=true` | valid |
| `mybatis.type-handlers-package=io.spring.infrastructure.mybatis` | valid |
| `mybatis.mapper-locations=mapper/*.xml` | valid |

## Assumptions about other components
- `build.gradle` (build component) bumps `mybatis-spring-boot-starter` and
  `mybatis-spring-boot-starter-test` 2.2.2 → 3.0.4.
- The Jakarta component handles any `javax.* → jakarta.*` renames elsewhere; no MyBatis
  file requires such a rename.
- Joda-Time stays on the classpath (handled by the build component), so `DateTimeHandler`
  continues to compile and register.

## Cannot compile until other components land
- This branch contains no compilable code changes, but the project as a whole will not
  build until the build/Jakarta/security/DGS components are merged. MyBatis itself needs
  no further work.
