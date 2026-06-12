---
name: migrate-mainframe-cobol-entity-to-java-spring-boot
description: "Converted from Devin playbook: Migrate Mainframe COBOL Entity to Java Spring Boot"
triggers:
  - user
  - model
---

# Migrate Mainframe COBOL Entity to Java Spring Boot

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Migrate Mainframe COBOL Entity to Java Spring Boot (playbook-e1518d5ad6a44687aad7ff0de0fc0650)

## Procedure

# Migrate Mainframe COBOL Entity to Java Spring Boot

## Overview

Migrate a single COBOL/VSAM entity (data layer) from a mainframe application to a Java Spring Boot + JPA project. This covers reading the COBOL copybook, creating the JPA entity, repository, service, Flyway migrations, seed data parsing, and unit tests. Each session should target one entity at a time (e.g., Account, Card, Customer, Transaction).

## What's Needed From User

- **Entity name** to migrate (e.g., "Account", "Card", "Customer", "Transaction")
- **Copybook path** — the `.cpy` file defining the COBOL record layout (e.g., `app/cpy/CVACT01Y.cpy`)
- **COBOL program references** — one or more `.cbl` files that read/write this entity, showing the business operations to mirror (e.g., `CBACT01C.cbl` for sequential read, `COACTVWC.cbl` for keyed read, `CBTRN02C.cbl` for update)
- **Seed data file** — the ASCII flat file with sample records (e.g., `app/data/ASCII/acctdata.txt`)
- **Target directory** — where the Java project lives or should be created (e.g., `java-migration/`)
- **Package name** — Java package root (e.g., `com.carddemo`)
- Whether the Spring Boot project already exists from a prior migration or needs to be created fresh

<phase name="COBOL Analysis" id="1">
## COBOL Analysis

Understand the COBOL record layout and business operations before writing any Java code.

1. **Read the COBOL copybook** to extract the full record layout — field names, PIC clauses, byte offsets, and total record length. Pay close attention to:
   - `PIC 9(n)` = unsigned numeric (n bytes)
   - `PIC S9(n)V99` = signed zoned decimal with 2 implied decimals, occupying n+2 bytes total (the sign and decimal point are implicit, NOT stored as extra bytes)
   - `PIC S9(n)V99 COMP-3` = signed packed decimal, occupying `ceil((n+3)/2)` bytes — each byte holds two digits except the last half-byte which holds the sign. If the seed data is ASCII, packed decimal fields will contain non-printable binary data
   - `PIC S9(n) COMP` or `COMP-4` = binary integer, occupying 2 bytes (n<=4), 4 bytes (n<=9), or 8 bytes (n<=18)
   - `PIC X(n)` = alphanumeric string of n bytes
   - `FILLER` fields at the end of the record (do not map these to Java, but count their bytes)
   - Compute the cumulative byte offset for each field

2. **Cross-reference the copybook with the seed data file.** Read the first few lines of the seed data and verify that the byte offsets from step 1 produce sensible values (dates parse as `YYYY-MM-DD`, numeric fields contain digits, etc.). If something doesn't align, re-examine the PIC clauses — the most common pitfall is miscounting zoned decimal byte width.

3. **Read the referenced COBOL programs** to identify the business operations performed on this entity: keyed READ (CICS), sequential READ (batch), REWRITE (update), WRITE (insert), DELETE. Note which operations exist — these will map to Spring Data JPA repository and service methods.

<verification>
- Byte offsets are documented for every field in the copybook
- First seed record parses correctly at every offset (dates look like dates, numbers look like numbers)
- COBOL business operations (READ, REWRITE, etc.) are identified from the .cbl files
</verification>
</phase>

<phase name="Java Implementation" id="2">
## Java Implementation

Create the Spring Boot project structure, entity, repository, service, and migrations.

1. **Create or update the Spring Boot project structure.** If a `pom.xml` doesn't already exist in the target directory, create a new Spring Boot project with dependencies: `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `h2` (dev), `postgresql` (prod), `flyway-core`, `lombok`, `spring-boot-starter-test`. If the project already exists, skip this step.

2. **Create the JPA entity class** mapping each copybook field to a Java field:
   - `PIC 9(n)` where n <= 18 -> `Long` (use `@Id` for the primary key)
   - `PIC S9(n)V99` (zoned or COMP-3) -> `BigDecimal` with `@Column(precision = n+2, scale = 2)`
   - `PIC S9(n) COMP/COMP-4` -> `Long` (or `Integer` if n <= 9)
   - `PIC X(n)` holding dates in `YYYY-MM-DD` format -> `LocalDate`
   - `PIC X(n)` for general text -> `String` with `@Column(length = n)`
   - `PIC X(1)` for status flags -> `String` with `@Column(length = 1)`
   - Add Javadoc on each field referencing the original COBOL field name and PIC clause
   - Use Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

3. **Create the Spring Data JPA repository interface** extending `JpaRepository<Entity, KeyType>`. Add custom finder methods that mirror the COBOL access patterns identified in Phase 1 (e.g., `findByActiveStatus`, `findByGroupId`).

4. **Create the service class** with `@Transactional` methods mirroring the COBOL operations:
   - `findById()` — mirrors keyed READ
   - `save()/update()` — mirrors REWRITE
   - `findAll()` — mirrors sequential READ
   - Add any entity-specific business logic methods found in the COBOL programs

5. **Create the Flyway schema migration** (`V<N>__create_<entity>_table.sql`). Map COBOL PIC clauses to SQL types:
   - `PIC 9(n)` or `PIC S9(n) COMP` -> `BIGINT`
   - `PIC S9(n)V99` (zoned or COMP-3) -> `DECIMAL(n+2, 2)`
   - `PIC X(n)` dates -> `DATE`
   - `PIC X(n)` text -> `VARCHAR(n)`
   - If a Flyway migration with this version already exists, use the next available version number.

6. **Parse the seed data and generate a Flyway seed migration** with INSERT statements. Write a Python helper to parse the fixed-width records using the byte offsets from Phase 1. For zoned decimal fields (non-COMP-3), handle the EBCDIC/ASCII trailing sign overpunch:
   - Positive signs: `{`=0, `A`=1, `B`=2, `C`=3, `D`=4, `E`=5, `F`=6, `G`=7, `H`=8, `I`=9
   - Negative signs: `}`=0, `J`=1, `K`=2, `L`=3, `M`=4, `N`=5, `O`=6, `P`=7, `Q`=8, `R`=9
   - Strip the sign character, replace with the digit, parse as long, divide by 100 for V99 fields, negate if negative
   - For COMP-3 (packed decimal) fields, read the raw bytes and decode: each byte contains two BCD digits, the last nibble is the sign (C=positive, D=negative, F=unsigned)
   - For COMP/COMP-4 (binary) fields, read the raw bytes as big-endian integers
   - Verify the first record's parsed values match expectations before generating all INSERT statements
   - **Note**: If the seed data is in ASCII text format (not raw EBCDIC binary), COMP-3 and COMP fields may not be present — these are typically only in binary VSAM exports. ASCII seed files usually contain only zoned decimal and display fields

7. **Create `application.yml`** (if it doesn't already exist) with H2 in-memory for default/dev profile and PostgreSQL for prod profile. Use environment variable substitution for prod credentials (`${DB_USERNAME:sa}`, `${DB_PASSWORD:}`). Enable Flyway and set `jpa.hibernate.ddl-auto: validate`.

8. **Optionally create a runtime seed data loader** (`@Configuration` class with a `CommandLineRunner` bean gated by `@Profile("seed")`) that reads the original flat file at runtime. Copy the seed data file to `src/main/resources/seed/` so it's on the classpath. This is secondary — the Flyway seed migration is the primary seeding mechanism.

<verification>
- JPA entity exists with all copybook fields mapped (excluding FILLER)
- Each entity field has Javadoc referencing the COBOL field name and PIC clause
- Repository, service, Flyway schema migration, and seed migration are created
- application.yml uses environment variable substitution for prod credentials (no hardcoded passwords)
</verification>
</phase>

<phase name="Testing and PR" id="3">
## Testing and PR

Validate everything works and deliver the PR.

1. **Write unit tests** using `@DataJpaTest` with H2:
   - Verify seed data loaded the expected number of records
   - Verify the first record's field values match the parsed seed data
   - Test CRUD operations (create, read, update, delete)
   - Test each custom repository finder method
   - Aim for at least 8 tests

2. **Run `mvn clean test`** and fix any failures. Common issues:
   - Flyway migration SQL syntax errors (reserved words, quoting)
   - Entity field type mismatches with the schema
   - Seed data parsing errors from incorrect byte offsets

3. **Commit, push, and create a PR.** In the PR description include:
   - Which COBOL entity was migrated and which copybook was used
   - Any field mapping decisions or ambiguities discovered
   - Byte offset documentation for the seed data parser
   - Test results summary
   - A checklist for the reviewer to verify field mappings and spot-check parsed values

<verification>
- All unit tests pass (`mvn clean test` exits with 0 failures)
- PR is created with a description covering entity mappings, decisions, and test results
- No hardcoded credentials in any committed file
</verification>
</phase>

## Specifications

- Each entity migration should be a self-contained PR
- All unit tests must pass before submitting the PR
- The JPA entity must have Javadoc referencing the original COBOL field name and PIC clause for traceability
- Zoned decimal parsing must handle both positive and negative sign overpunch characters
- Prod database credentials must never be hardcoded; use environment variable substitution
- Validation: run `mvn clean test` and confirm all tests pass with zero failures

## Advice and Pointers

- **Byte offset miscalculation is the #1 source of bugs.** Always cross-reference the copybook PIC sizes against the actual seed data before writing any Java code. `PIC S9(10)V99` occupies 12 bytes (10 + 2 decimal digits), not 14 — the sign and decimal point are implicit in zoned decimal format and do NOT take extra bytes.
- **The copybook is the source of truth for the record layout.** COBOL programs sometimes reference fields indirectly through MOVE or DISPLAY statements, which can mislead about field order or size. Always trust the copybook.
- **Map every field from the copybook in order, even if the data looks unexpected.** For example, a field named `ADDR-ZIP` might contain `A000000000` in the seed data. Don't skip fields or reorder them based on what the data "looks like."
- **Use `${DB_PASSWORD:}` syntax in `application.yml`** for all database passwords (including dev/H2). Even empty or placeholder passwords can trigger secret-scanning commit hooks.
- **FILLER fields** are padding — do not create Java entity fields for them, but count their bytes when computing total record length.
- **If the seed data resource is needed at runtime** (for the `@Profile("seed")` loader), copy the flat file into `src/main/resources/seed/` so it's on the classpath. Don't rely on relative paths to the original COBOL data directory.

## Forbidden Actions

- Do not skip reading the COBOL copybook and guess field layouts from the seed data alone
- Do not hardcode database credentials in `application.yml` for any profile
- Do not modify the original COBOL source files or seed data files
- Do not create a single monolithic migration for multiple entities — one entity per migration file

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
