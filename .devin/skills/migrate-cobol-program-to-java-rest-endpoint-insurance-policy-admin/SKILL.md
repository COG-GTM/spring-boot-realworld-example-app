---
name: migrate-cobol-program-to-java-rest-endpoint-insurance-policy-admin
description: "Converted from Devin playbook: Migrate COBOL Program to Java REST Endpoint (insurance-policy-admin)"
triggers:
  - user
  - model
---

# Migrate COBOL Program to Java REST Endpoint (insurance-policy-admin)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Migrate COBOL Program to Java REST Endpoint (insurance-policy-admin) (playbook-c65e6a2e043b484da22b4a56d2ce85f9)

## Procedure

# Migrate COBOL Program to Java REST Endpoint

## Overview

Migrate a COBOL program from `cobol/programs/` in the `milind-cognition/insurance-policy-admin` repository into the existing `java-facade` Spring Boot 1.5.22 application as a REST endpoint. This playbook replicates the COBOL program's exact business logic (paragraph flow, data limits, error messages) as a Java service backed by JdbcTemplate, then documents the migration.

## What's Needed From User

- Which COBOL program to migrate (e.g., `POLNEW`, `POLRNW`, `POLEND`, `UNDWRT`, `PREMBAT`)
- Any deviations from the COBOL logic (e.g., additional fields, different limits)
- Whether the endpoint is read-only (GET) or write (POST/PUT)

<phase name="Analyze COBOL Program" id="1">
## Analyze the COBOL Program

Read and fully understand the COBOL source before writing any Java code.

1. Read the COBOL program file at `cobol/programs/<PROGRAM>.cbl`. Identify:
   - **Program ID and CICS transaction code** (in IDENTIFICATION DIVISION and `EXEC CICS RETURN TRANSID`)
   - **Copybooks** — all `COPY` statements (e.g., `POLICY-RECORD.cpy`, `CUSTOMER-RECORD.cpy`). Read each from `cobol/copybooks/`.
   - **Working-Storage variables** — error messages, limits, flags, counters
   - **PROCEDURE DIVISION paragraph flow** — list every paragraph in execution order from the main logic
2. Read the DDL in `sql/ddl/create-tables.sql` for every table the program accesses (look for `EXEC SQL` blocks with SELECT/INSERT/UPDATE).
3. Read the existing Java models in `java-facade/src/main/java/com/acme/insurance/pas/model/` to identify which DB2 tables already have Java representations.
4. Read the existing `repository/PolicyRepository.java` to understand which queries and row mappers already exist.
5. Create a mapping document (in your notes, not committed) that maps each COBOL paragraph to a planned Java method.

**Existing Java models (as of POLQRY migration):**
- `Policy.java` → `ACMEINS.POLICIES`
- `Coverage.java` → `ACMEINS.COVERAGES`
- `Customer.java` → `ACMEINS.POLICY_HOLDERS`
- `PolicyInquiryResponse.java` → combined DTO

**Existing repository methods:**
- `findByPolicyNumber(String)` → SELECT from POLICIES
- `findCoveragesByPolicyNumber(String)` → SELECT from COVERAGES ORDER BY SEQUENCE_NUM
- `findCustomerById(String)` → SELECT from POLICY_HOLDERS

Only create TODO items for the current phase.

<verification>
- Every paragraph in the PROCEDURE DIVISION has been identified and its purpose understood
- All DB2 tables accessed by the program have been identified with their DDL reviewed
- All COBOL copybooks referenced by the program have been read
- A paragraph-to-Java-method mapping has been planned
- Existing Java models and repository methods that can be reused have been identified
- Any hardcoded limits (e.g., max rows, rate caps) have been noted
- All error messages in the COBOL program have been catalogued
</verification>
</phase>

<phase name="Implement Java Code" id="2">
## Implement the Java Code

Create or modify Java files following the established patterns in the codebase.

### New Models (only if needed)
For each DB2 table accessed by the program that does NOT already have a Java model:
1. Create a class in `model/` with package `com.acme.insurance.pas.model`
2. Use old-style Java bean pattern: private fields, no-arg constructor, getters/setters
3. **No Lombok, no records, no var, no text blocks** — Java 8 only
4. Map DB2 types to Java types:
   - `CHAR(n)` / `VARCHAR(n)` → `String`
   - `DATE` → `java.util.Date`
   - `TIMESTAMP` → `java.util.Date`
   - `DECIMAL(m,n)` → `java.math.BigDecimal`
   - `SMALLINT` / `INTEGER` → `int`

### Response DTO (if program combines multiple tables)
Create a DTO in `model/` that holds all the data the COBOL program assembles before its display/output paragraph.

### Repository Layer
Edit `repository/PolicyRepository.java`:
1. Add a `private static final String` SQL constant for each new query. Qualify tables with `ACMEINS.` schema prefix. Use uppercase column names.
2. Add a `RowMapper` inner class for each new model, following the pattern of `PolicyRowMapper` / `CoverageRowMapper`:
   - `.trim()` on CHAR fields
   - Null checks on nullable VARCHAR fields
   - `rs.getBigDecimal()` for DECIMAL, `rs.getDate()` for DATE, `rs.getInt()` for integers
3. Add query methods. For single-row queries: query into List, return first element or null if empty.

### Service Layer
Create a `@Service` class in `service/` that replicates the COBOL paragraph flow:
1. `@Autowired PolicyRepository policyRepository`
2. Main method mirrors the COBOL `0000-MAIN-LOGIC` paragraph sequence exactly
3. Input validation throws `IllegalArgumentException` with the same error message as the COBOL program
4. Return `null` for not-found conditions
5. Preserve all data limits (e.g., `subList(0, 20)` for max-20 coverage cap)

### Controller Endpoint
Edit `controller/PolicyController.java`:
1. `@Autowired` the new service
2. Add endpoint method:
   - Read-only programs → `@GetMapping`
   - Write programs → `@PostMapping` or `@PutMapping`
   - URL: under `/api/v1/policies/` following REST conventions
3. Error handling:
   - Catch `IllegalArgumentException` → `HttpStatus.BAD_REQUEST`
   - Null response → `HttpStatus.NOT_FOUND`
   - Success → `HttpStatus.OK`

### H2 Schema/Seed Data (if new tables)
- Add H2-compatible DDL to `src/main/resources/schema.sql`
- Add seed data to `src/main/resources/data.sql` using `MERGE INTO ... KEY (...) VALUES (...)`

Only create TODO items for the current phase.

<verification>
- All new model classes follow the existing bean pattern (no Lombok, Java 8 compatible)
- SQL constants use uppercase column names and ACMEINS schema prefix
- RowMapper inner classes handle nulls and trim CHAR fields
- Service method replicates the exact COBOL paragraph sequence
- All COBOL error messages are preserved in the Java code
- All hardcoded limits are preserved (e.g., max row counts)
- Controller catches IllegalArgumentException as 400 and null as 404
- Code compiles: `mvn package -DskipTests` passes
</verification>
</phase>

<phase name="Test" id="3">
## Test the Implementation

### Integration Tests
Edit `src/test/java/com/acme/insurance/pas/controller/PolicyControllerTests.java`:
1. Follow the existing pattern: `@RunWith(SpringRunner.class)`, `@SpringBootTest`, `@AutoConfigureMockMvc`
2. Add tests for:
   - **Happy path** — Full response with correct data from H2 seed data. Use `jsonPath()` to assert specific field values.
   - **Not found** — 404 for non-existent records
   - **Validation error** — 400 for invalid input (empty/blank values)
3. For URL-encoded characters, use `new URI(...)` instead of string templates
4. All existing tests must still pass

### Run Tests
```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
cd java-facade
mvn test
```

### Manual Verification
```bash
mvn spring-boot:run -Dspring.profiles.active=local &
# Wait for startup, then test with curl
curl -s http://localhost:8080/api/v1/policies/<test-policy>/... | python -m json.tool
# Kill the server when done
```

Only create TODO items for the current phase.

<verification>
- All new tests pass
- All pre-existing tests still pass (no regressions)
- `mvn test` exits with BUILD SUCCESS
- Manual curl against H2 returns expected data matching COBOL program behavior
- Error cases (not found, invalid input) return correct HTTP status codes
</verification>
</phase>

<phase name="Document and Ship" id="4">
## Document the Migration and Create PR

### Migration README
Create or **append to** `java-facade/src/main/java/com/acme/insurance/pas/service/README.md`. Add a new section for this program with:

1. **Summary table** — Program ID, CICS transaction, Java class, I/O mechanism (COBOL vs Java)
2. **What the program does** — Plain English description of the business function and who uses it
3. **Data flow diagram** — ASCII art showing the paragraph-by-paragraph flow, with COBOL paragraph names on the left and Java method equivalents on the right
4. **Tables accessed** — Table name, operation (SELECT/INSERT/UPDATE), join key, purpose
5. **COBOL-to-Java mapping table** — Each paragraph → Java method/class with COBOL line numbers
6. **Copybook-to-model mapping** — Which copybooks map to which Java model classes
7. **Request/Response examples** — Concrete JSON with realistic data from the seed data
8. **Error responses** — Table: condition, COBOL error message, HTTP status code, response body
9. **Key differences from COBOL** — What intentionally changed (I/O, fields, error handling, session management)
10. **Files** — All new/modified files with one-line purpose

### Send README to User
After writing the README, send its content to the user as an attached file so they can review it in the session.

### Create PR
1. Stage only the relevant files (never `git add .`)
2. Commit with a message referencing the COBOL program name
3. Push and create a PR with:
   - Summary listing every file and its purpose
   - Review checklist for the human reviewer
   - Notes on intentional differences from the COBOL source

Only create TODO items for the current phase.

<verification>
- README section covers all 10 items listed above
- README has been sent to the user as an attachment
- ASCII data flow diagram accurately reflects the COBOL paragraph sequence
- Request/response examples use realistic data from the seed data
- PR has been created with a descriptive title and body
- PR body includes a review checklist
- All files are committed (no unstaged changes for this migration)
</verification>
</phase>

## Specifications

- **Java version**: 8 (no records, no var, no text blocks, no diamond inference on anonymous classes)
- **Spring Boot version**: 1.5.22
- **Bean style**: Old-style getters/setters, no Lombok
- **Database access**: JdbcTemplate with RowMapper (no JPA/Hibernate)
- **Schema prefix**: `ACMEINS.` on all table references in SQL
- **Test framework**: JUnit 4 with `SpringRunner`, `MockMvc`
- **Local profile**: `spring.profiles.active=local` uses H2 in-memory
- **JAVA_HOME**: `/usr/lib/jvm/java-8-openjdk-amd64`

## Advice and Pointers

- The COBOL paragraph flow IS the service method structure. Translate it 1:1 — do not reorganize the logic.
- COBOL `SQLCODE 100` = "not found" → return null from repository method → controller returns 404.
- COBOL `SQLCODE < 0` = DB2 error → in Java, JdbcTemplate throws `DataAccessException` which Spring handles.
- COBOL cursor-based fetches (OPEN → FETCH in loop → CLOSE) become a single `jdbcTemplate.query()` call with a `subList()` cap to match the COBOL row limit.
- COBOL BMS map SEND = Java HTTP response. CICS RECEIVE MAP = Java `@PathVariable` or `@RequestBody`.
- COBOL COMMAREA pseudo-conversational flow = Java stateless REST (no equivalent needed).
- Preserve the COBOL error message strings exactly (e.g., "POLICY NUMBER IS REQUIRED") — they serve as documentation of the original behavior.
- Check existing models/queries before creating new ones. The codebase grows with each migration.

## Forbidden Actions

- Do not use Lombok annotations
- Do not use Java features beyond Java 8 (no records, no var, no text blocks, no switch expressions)
- Do not use JPA/Hibernate — only JdbcTemplate
- Do not use `git add .` — stage files individually
- Do not modify existing tests to make new tests pass
- Do not skip the README documentation phase
- Do not reorganize the COBOL paragraph flow — mirror it exactly in the service layer
- Do not remove or rename existing endpoints — only add new ones

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
