# Database migrations

The database schema is owned by [Flyway](https://flywaydb.org/) (Community Edition, version
managed by Spring Boot, currently 8.0.5). Nothing else creates or alters tables: Spring's SQL
initializer is switched off (`spring.sql.init.mode=never`) and MyBatis only reads and writes rows.

On startup Spring Boot runs `flyway.migrate()` against the configured datasource before any
MyBatis mapper is used, so an application instance always runs against a schema at the latest
version shipped in its jar.

## Layout

| Path | Purpose |
| --- | --- |
| `src/main/resources/db/migration/V1__create_tables.sql` | **Baseline** (version 1). The schema as it existed when migrations were introduced. |
| `src/main/resources/db/migration/V2__add_lookup_indexes.sql` | First incremental migration: non-unique indexes on the columns the MyBatis mappers filter, join and sort by. |
| `src/test/resources/db/schema/baseline-v1.sql` | Snapshot of the schema produced by the baseline. Used to check that an unmanaged database is really at version 1 before baselining it. |
| `src/test/resources/db/schema/current.sql` | Snapshot of the schema after all migrations. Reviewable diff of every schema change. |
| `src/test/java/io/spring/infrastructure/migration/FlywayMigrationTest.java` | Migration tests: fresh install, V1 to latest upgrade, baselining an unmanaged database, schema snapshots, checksum pinning, `clean` disabled. |
| `src/test/java/io/spring/infrastructure/migration/FlywayConfigurationTest.java` | Asserts the Spring-configured Flyway settings below. |

## Configuration

`src/main/resources/application.properties`:

| Property | Value | Why |
| --- | --- | --- |
| `spring.flyway.locations` | `classpath:db/migration` | Single source of migrations. |
| `spring.flyway.baseline-version` | `1` | An unmanaged database that is baselined is recorded as being at V1, so V1 is skipped and V2+ are applied. |
| `spring.flyway.baseline-description` | `baseline of pre-migration schema` | Makes baselined databases recognisable in `flyway_schema_history`. |
| `spring.flyway.baseline-on-migrate` | `false` | An unmanaged, non-empty database makes startup fail instead of being silently baselined. Baselining is an explicit, one-off operator action (see below). |
| `spring.flyway.validate-on-migrate` | `true` | Startup fails if an applied migration was edited, removed or failed. |
| `spring.flyway.out-of-order` | `false` | Versions are applied strictly in order. |
| `spring.flyway.clean-disabled` | `true` | `clean` drops every object in the schema; it is never allowed. |

Any of these can be overridden at runtime with the usual Spring mechanisms, e.g.
`--spring.flyway.baseline-on-migrate=true` or `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`.

### Gradle tasks

The Flyway Gradle plugin is configured in `build.gradle` with the same settings, pointed at the
local development database (`dev.db` in the project root) and at the migration sources on disk, so
it works without building the jar:

    ./gradlew flywayInfo       # show applied / pending migrations
    ./gradlew flywayValidate   # verify applied migrations match the ones on disk
    ./gradlew flywayMigrate    # apply pending migrations
    ./gradlew flywayBaseline   # mark an unmanaged database as being at version 1
    ./gradlew flywayRepair     # remove failed entries / realign checksums (use with care)

To target another database, pass `-Pflyway.url=...` (and `-Pflyway.user=...`,
`-Pflyway.password=...` if needed), e.g.

    ./gradlew flywayInfo -Pflyway.url=jdbc:sqlite:/var/data/realworld.db

`flywayClean` is disabled and fails.

## Upgrade path

First find out which of the following cases a database is in:

    ./gradlew flywayInfo -Pflyway.url=<jdbc url>

Always take a backup before migrating a database that holds data you care about. For SQLite that
is a file copy while the application is stopped: `cp dev.db dev.db.bak`.

### 1. New / empty database

Nothing to do. Start the application (`./gradlew bootRun`) or run `./gradlew flywayMigrate`; all
migrations are applied in order.

### 2. Database already managed by Flyway (history table present, at V1)

This is any `dev.db` created by earlier versions of this project, which already ran
`V1__create_tables.sql` through Flyway. `flywayInfo` shows V1 as `Success` and newer versions as
`Pending`.

Start the new version of the application, or run `./gradlew flywayMigrate`. Only the pending
migrations are applied; V1 is left untouched (its checksum is unchanged, so validation passes).

### 3. Unmanaged database (tables present, no `flyway_schema_history`)

This is a database whose tables were created outside Flyway: by hand, by a copied script, or by a
schema bootstrap from another tool. Starting the application fails with:

    Found non-empty schema(s) "main" but no schema history table.
    Use baseline() or set baselineOnMigrate to true to initialize the schema history table.

That is intentional. To bring it under Flyway:

1. **Stop the application and back up the database.**
2. **Check that the schema matches the baseline.** Baselining tells Flyway "this database is
   already at version 1"; it does not create or change anything, so the schema must really be V1:

       sqlite3 dev.db "select sql || ';' from sqlite_master \
         where sql is not null and name not like 'flyway_schema_history%' \
         order by case type when 'table' then 0 else 1 end, name;" > /tmp/actual.sql
       diff -wB <(grep -v '^--' src/test/resources/db/schema/baseline-v1.sql) /tmp/actual.sql

   No output means the schema is exactly the baseline. If the diff shows differences, fix the
   database by hand (or write a one-off script) until it matches before continuing; otherwise V2+
   will run against a schema they were not written for.
3. **Baseline it**, either with Gradle:

       ./gradlew flywayBaseline -Pflyway.url=jdbc:sqlite:/path/to/db

   or by starting the application once with baselining enabled:

       ./gradlew bootRun --args='--spring.flyway.baseline-on-migrate=true'

   Do not leave `baseline-on-migrate` enabled permanently.
4. **Migrate**: start the application normally or run `./gradlew flywayMigrate`. `flywayInfo`
   now shows version 1 as `Baseline` and later versions as `Success`.

### 4. Failed migration

If a migration fails part-way, Flyway records it as failed and refuses to start. SQLite runs each
migration in a transaction, so a failed migration normally leaves no partial changes. Fix the
cause, run `./gradlew flywayRepair` to remove the failed entry, then migrate again. On databases
without transactional DDL (e.g. MySQL) restore from the backup instead.

### Rolling back

Migrations are forward-only (Flyway undo migrations are a paid feature). To revert a schema change,
write a new migration that undoes it. To roll back an application release, restore the database
backup taken before the upgrade. An older jar will still start against a newer schema (Flyway
ignores applied migrations it does not know about), so only rely on that when the newer
migrations were backwards-compatible.

## Writing a new migration

1. Add `src/main/resources/db/migration/V<next>__<description>.sql`, e.g.
   `V3__add_article_view_count.sql`. Use the next integer version; descriptions use
   underscores for spaces.
2. **Never edit, rename or delete a migration that has been merged.** Databases that already ran
   it would fail validation on startup. `FlywayMigrationTest` pins the checksum of every released
   migration and fails if one changes; add the new migration's checksum to `RELEASED_CHECKSUMS`
   (the failing assertion prints it).
3. Regenerate `src/test/resources/db/schema/current.sql` from a freshly migrated database (keep
   the header comment) and commit it together with the migration, so the schema change is visible
   in review:

       rm -f /tmp/fresh.db
       ./gradlew flywayMigrate -Pflyway.url=jdbc:sqlite:/tmp/fresh.db
       sqlite3 /tmp/fresh.db "select sql || ';' || char(10) from sqlite_master \
         where sql is not null and name not like 'flyway_schema_history%' \
         order by case type when 'table' then 0 else 1 end, name;"

4. Prefer additive, backwards-compatible changes (new tables, nullable columns, indexes) so the
   previous release keeps working during a rollout. Split destructive changes (drop / rename
   column) into an additive migration now and a cleanup migration in a later release.
5. SQLite has limited `ALTER TABLE` support. Changing a column type or constraint requires the
   create-new-table / copy / drop / rename pattern inside the migration.
6. Run `./gradlew test`.

## Other databases

The migrations use plain SQL and are only tested against SQLite. If the project moves to
another database, or a vendor-specific statement is needed, move to per-vendor directories
(`spring.flyway.locations=classpath:db/migration/{vendor}`) and keep version numbers aligned
across vendors.
