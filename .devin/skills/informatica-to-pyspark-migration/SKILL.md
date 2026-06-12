---
name: informatica-to-pyspark-migration
description: "Converted from Devin playbook: Informatica to PySpark Migration"
triggers:
  - user
  - model
---

# Informatica to PySpark Migration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Informatica to PySpark Migration (playbook-fd82a99249d4445b8c8bc7715d0863db)

## Procedure

# Informatica to PySpark Migration Playbook

## Overview

This playbook guides the migration of Informatica PowerCenter ETL workflows to PySpark jobs. It covers end-to-end migration including: analyzing source Informatica XML exports, generating equivalent PySpark code with shared utilities, building a local test database with synthetic data for full functional and performance testing, applying performance monitoring and fault tolerance best practices, and validating that no business or technical logic is lost.

## What's Needed From User

- **Repository URL** containing Informatica XML workflow exports (e.g., `XML/Pay_Calendar`, `XML/COMPTIME`)
- **List of Informatica workflows** to migrate, or "all" if the entire repository should be migrated
- **Oracle connection details** for the target database (host, port, service name) — or confirmation to use a local Oracle XE Docker container for testing
- **Informatica parameter files** (e.g., `.iparms` files) and any shell scripts used for pre/post-load operations
- **Any attached analysis/best-practices documents** describing known data quality gaps or optimization issues in the source Informatica jobs
- **Target environment details**: standalone PySpark, Databricks, EMR, or other Spark platform

<phase name="Source Analysis & Migration Planning" id="1">
## Phase 1: Source Analysis & Migration Planning

Analyze every Informatica XML workflow export in the repository to build a complete inventory and migration plan.

1. Clone the repository and list all XML workflow files plus any shell scripts, SQL scripts, and parameter files
2. For each Informatica workflow XML, extract and document:
   - Workflow name, server, schedule type (ONDEMAND vs RUNFOREVER)
   - Session sequence and dependencies (which sessions must succeed before the next runs)
   - Source tables/files and target tables for each mapping
   - All transformations: Source Qualifier SQL overrides, Filters, Expressions, Lookups (note the connection object — some may use different DBs), Aggregators, Routers, Update Strategies, Sequences
   - Workflow variables (`$$WF_*`) and session parameters (`$$PP_END_YEAR`, `$$PP_NUM`)
   - Pre/Post session SQL commands
   - Email notification tasks and recipient lists
   - Reject file (`.bad`) configurations
   - Credential file references and connection objects (e.g., `INFO_TARGET`, `$Source`, `INFO_NATE`)
3. Rank workflows by complexity (Low → Very High) based on: number of sessions, number of lookups, number of target tables, use of RUNFOREVER, cross-database lookups, shell script dependencies
4. Define the migration order from simplest to most complex — migrate and validate simple jobs first to establish patterns before tackling complex ones
5. Create a migration inventory table:

   | Job # | Workflow Name | Complexity | Sessions | Lookups | Targets | Special Notes |
   |-------|--------------|------------|----------|---------|---------|---------------|

6. Identify shared patterns across workflows that should become common utility modules:
   - Pay period lookup (CURR_PP_FLAG='Y')
   - Parameter validation (IS_NUMBER checks + ABORT)
   - Counter/error table writes (COUNTER_TBL, ERROR_TBL)
   - Email message building with environment prefix
   - Credential management

<verification>
- All Informatica XML files have been parsed and documented
- A complexity-ranked migration order table has been created
- Shared patterns across workflows have been identified
- All connection objects, credential files, and parameter files are inventoried
- The migration plan is presented to the user for confirmation before proceeding
</verification>
</phase>

<phase name="Optimization & Data Quality Gap Analysis" id="2">
## Phase 2: Optimization & Data Quality Gap Analysis

Analyze the source Informatica jobs against industry best practices for performance, fault tolerance, and data quality. This analysis informs what the PySpark target jobs must fix or improve upon.

1. **Performance Monitoring**: Check every session's `Collect performance data` and `Write performance data to repository` settings. If disabled (common), flag as CRITICAL — the PySpark equivalent must include built-in metrics collection (timing, row counts, throughput per transformation)
2. **Fault Tolerance & Recovery**: Check `Enable Recovery`, `Rollback Transactions on Errors`, `Stop on errors`, `Enable HA recovery`, `Automatically recover terminated tasks`. Map each to PySpark equivalents:
   - `Enable Recovery` → Spark checkpointing
   - `Rollback on Errors` → Transaction rollback in JDBC writes
   - `Stop on errors` → Configurable error threshold with job abort
   - HA recovery → Spark driver restart / YARN application retry
3. **Memory & Buffer Tuning**: Check `DTM buffer size`, `Maximum Memory`, `High Precision`. Map to:
   - DTM buffer → `spark.sql.shuffle.partitions`
   - Max Memory → `spark.executor.memory` / `spark.driver.memory`
   - High Precision → `DecimalType` with appropriate precision/scale for financial data
4. **Parallelism**: Check `Dynamic Partitioning`, `Number of Partitions`, `Session on Grid`. Map to Spark parallelism settings
5. **Lookup Optimization**: Check `Dynamic Lookup Cache`, `Recache if Stale`, cache sizes, `Multiple Match Policy`. Flag "Use Any Value" as non-deterministic — PySpark must use deterministic join logic (e.g., first match by a defined ordering)
6. **Commit Strategy**: Check `Commit Type`, `Commit Interval`, `Rollback on Errors`. Map to PySpark batch write sizes and transaction management
7. **Error Logging**: Check `Error Log Type`, `Log Source Row Data`. PySpark must implement row-level error capture to a dedicated error table
8. **Data Quality Dimensions** — assess each workflow against these 7 dimensions:
   - **Completeness**: Source row count validation, mandatory field null checks, completeness thresholds (>= N% rows processed)
   - **Validity**: Parameter range validation, date format validation, domain validation
   - **Uniqueness**: Duplicate detection before load, PK validation
   - **Consistency**: Cross-table consistency, referential integrity, idempotency
   - **Accuracy**: Range validation, domain validation, cross-field validation, lookup determinism
   - **Timeliness**: Data freshness validation, SLA monitoring, late-arriving data handling
   - **Integrity**: Checksum validation, row count reconciliation (source read = target written + errors + rejects)
9. Produce a gap matrix (job × dimension) showing Covered / Partial / Missing for each
10. If the user provided a best-practices or data quality analysis document, cross-reference its findings with your analysis and incorporate all recommendations

<verification>
- Every session's fault tolerance and performance settings have been extracted from the XML
- A PySpark equivalence mapping exists for each Informatica setting
- A data quality gap matrix has been produced covering all 7 dimensions for each job
- Prioritized recommendations (config-only vs code changes vs architecture changes) have been documented
- Findings are presented to the user before proceeding to implementation
</verification>
</phase>

<phase name="PySpark Implementation" id="3">
## Phase 3: PySpark Implementation

Build the PySpark migration package with shared utilities and per-job implementations. Follow the migration order from Phase 1 (simplest to most complex).

### Project Structure

Create the following directory structure:
```
pyspark_migration/
├── __init__.py
├── requirements.txt          # pyspark, oracledb, python-dotenv, pytest, pytest-cov, delta-spark
├── Dockerfile
├── docker-compose.yml
├── common/
│   ├── __init__.py
│   ├── config.py             # Dataclass-based config replacing parameter files
│   ├── db_manager.py         # Oracle JDBC read/write with transaction management
│   ├── spark_session.py      # SparkSession factory with tuned defaults
│   ├── email_service.py      # Email notifications replacing Informatica email tasks
│   ├── counter_error.py      # COUNTER_TBL / ERROR_TBL management
│   └── logging_utils.py      # Session-level and job-level metrics tracking
├── jobs/
│   ├── __init__.py
│   └── job<N>_<name>.py      # One file per workflow, migrated in order
├── scripts/
│   ├── __init__.py
│   ├── run_job.py            # CLI entry point for running individual jobs
│   ├── setup_oracle_schema.py # Creates all tables + loads synthetic data
│   └── run_all_jobs.py       # Execute all jobs end-to-end
└── tests/
    ├── __init__.py
    ├── test_common_utilities.py
    └── test_job<N>_<name>.py  # One test file per job
```

### Common Utilities

1. **config.py**: Use `@dataclass` classes for `OracleConnectionConfig` (host, port, service_name, username, password, jdbc_url property), `SparkConfig` (executor_memory, driver_memory, shuffle_partitions, broadcast_threshold, checkpoint settings), `EmailConfig`, `PathConfig`. Load all values from environment variables with sensible defaults. Document which Informatica artifact each config replaces
2. **db_manager.py**: Implement `read_jdbc(table_or_query)` → DataFrame, `write_jdbc(df, table, mode)` with batch commit, `execute_sql(statement)` with proper rollback on error, `execute_sql_file(path)` that tracks errors across statements and rolls back on any failure. Do NOT call `df.count()` in logging after reads — this forces a full table scan
3. **spark_session.py**: Create SparkSession with Oracle JDBC driver on classpath, tuned shuffle partitions, broadcast threshold, and optional checkpoint directory
4. **email_service.py**: Send emails with environment prefix in subject (Dev/Test/Prod detection from config), support success and failure templates
5. **counter_error.py**: Write to COUNTER_TBL (process_name, counter_description, counter_value, pp_end_year, pp_num, cycle_id) and ERROR_TBL (process_name, error_message, source_key, error_date, pp_end_year, pp_num, cycle_id)
6. **logging_utils.py**: `SessionMetrics` class tracking start_time, end_time, duration, src_success_rows, src_failed_rows, tgt_success_rows, tgt_failed_rows, total_trans_errors, first_error_code, first_error_msg. `JobMetrics` class aggregating session metrics per workflow

### Per-Job Implementation

For each job, create a class (e.g., `PayCalendarJob`) that:

1. Documents the original Informatica workflow in the module docstring — include workflow name, session sequence, source/target tables, transformation logic with XML line references
2. Accepts `SparkSession`, `MigrationConfig`, `DatabaseManager`, `EmailService` via constructor
3. Implements a `run()` method that follows the exact Informatica session sequence — each session becomes a method
4. Uses workflow variables as instance attributes (replacing `$$WF_*` variables)
5. Implements all transformation logic faithfully:
   - `Source Qualifier` SQL override → `spark.read.jdbc()` with custom query or `db_manager.read_jdbc()`
   - `Filter` → `df.filter(condition)`
   - `Expression` → `df.withColumn(name, expr)`
   - `Lookup` → broadcast join (`df.join(lookup_df, keys, "left")`) — use deterministic match policy, NOT "any value"
   - `Aggregator` → `df.groupBy().agg()`
   - `Router` → multiple `df.filter()` branches
   - `Update Strategy (DD_UPDATE)` → JDBC `UPDATE` statement or `MERGE INTO`
   - `Sequence Generator` → `monotonically_increasing_id()` or window function `row_number()`
   - `$SESSSTARTTIME` → `datetime.now()` captured at session start
   - `$PMMappingName` → Python constant string
   - Reject files (`.bad`) → Write failed rows to a separate DataFrame or file
6. Includes proper error handling: catch exceptions per session, log metrics, send failure email, abort if a required predecessor failed
7. **Performance monitoring**: Every session method must record `SessionMetrics` (start/end time, row counts, errors)
8. **Fault tolerance**: Implement configurable error threshold (`stop_on_errors`), transaction rollback on write failures, optional Spark checkpointing for long-running jobs
9. For RUNFOREVER workflows: implement a polling loop with configurable sleep interval, graceful shutdown signal handling, and memory monitoring

### Informatica-to-PySpark Transformation Reference

When writing job code, use this mapping for all Informatica constructs:

| Informatica Construct | PySpark Equivalent |
|-----------------------|--------------------|
| Source Qualifier | `spark.read.jdbc()` with pushdown query |
| Filter | `df.filter()` / `df.where()` |
| Expression | `df.withColumn()` with `F.when()`, `F.lit()`, etc. |
| Lookup (cached) | Broadcast join: `df.join(F.broadcast(lookup_df), keys)` |
| Lookup (uncached) | Regular join or per-row JDBC query (avoid if possible) |
| Aggregator | `df.groupBy().agg()` |
| Router | Multiple `df.filter()` producing separate DataFrames |
| Update Strategy DD_UPDATE | JDBC UPDATE via `db_manager.execute_sql()` |
| Update Strategy DD_INSERT | `df.write.jdbc(mode="append")` |
| Sequence Generator | `F.monotonically_increasing_id()` or `F.row_number()` |
| Joiner | `df1.join(df2, on=keys, how="inner/left/right")` |
| Sorter | `df.orderBy()` |
| ABORT() | `raise RuntimeError(message)` after logging |
| $$WF_* variables | Instance attributes on the job class |
| $PMRepositoryServiceName | Environment config string |
| Reject file (.bad) | Write failed rows to CSV / Parquet side output |
| Pre/Post Session SQL | `db_manager.execute_sql()` before/after main logic |
| Email task | `email_service.send()` |
| Normalizer | `F.explode()` / `F.arrays_zip()` to normalize repeating groups into rows |
| Stored Procedure | JDBC callable statement via `db_manager.execute_sql()` (handle OUT params if needed) |
| Union | `df1.unionByName(df2, allowMissingColumns=True)` |
| Rank | Window functions: `F.rank()`, `F.dense_rank()`, `F.row_number()` over `Window.partitionBy().orderBy()` |
| Transaction Control | Conditional batching with explicit commit/rollback in `db_manager` |
| XML Generator/Parser | `spark-xml` library (`from_xml`/`to_xml`) or `F.from_json()`/`F.to_json()` |
| Java Transformation / External Procedure | PySpark UDF or Pandas UDF; for external services use HTTP call with retry/backoff |

<verification>
- All common utility modules are implemented and documented
- Each Informatica workflow has a corresponding PySpark job file
- Every transformation in every mapping has been faithfully translated (no business logic dropped)
- Each job class documents its Informatica source in the module docstring
- Performance monitoring (SessionMetrics) is integrated into every session method
- Fault tolerance (error thresholds, rollback, checkpointing) is implemented
- RUNFOREVER workflows have polling loop with graceful shutdown
- The code compiles and imports without errors: `python -c "from pyspark_migration.jobs import *"`
</verification>
</phase>

<phase name="Local Test Database & Synthetic Data" id="4">
## Phase 4: Local Test Database & Synthetic Data

Set up a local Oracle database via Docker and populate it with synthetic data for full testing.

1. **Start Oracle XE via Docker**:
   ```bash
   docker run -d --name oracle-xe -p 1521:1521 \
     -e ORACLE_PASSWORD=<password> \
     -e ORACLE_DATABASE=BIISDB \
     -e APP_USER=biis_user \
     -e APP_USER_PASSWORD=<password> \
     gvenzl/oracle-xe:21-slim
   ```
   Wait for the container to be healthy before proceeding.

2. **Create the schema setup script** (`scripts/setup_oracle_schema.py`):
   - Create ALL source and target tables referenced by every workflow (extract DDL from the Informatica XML — Source/Target definitions include column names, data types, precision, scale, and nullable flags)
   - Create COUNTER_TBL and ERROR_TBL with columns matching the Informatica pattern
   - Create any lookup reference tables

3. **Generate synthetic test data** with two tiers:
   - **Functional test data** (small, ~50-1000 rows per table): Covers all code paths — valid rows, invalid rows (nulls, bad dates, out-of-range values), edge cases (empty source, single row, duplicate keys), boundary values for filters and routers
   - **Performance test data** (large, 10K-1M+ rows per table): Realistic volume to stress-test joins, lookups, aggregations, and write throughput. Include enough variety to exercise broadcast join thresholds and shuffle behavior

4. **Ensure synthetic data exercises every business rule**:
   - Include rows that should be filtered out (e.g., `RECORD_TYPE_FLAG != 'D'`, `fda_rec_type != '02'`)
   - Include rows that should trigger ABORT conditions (e.g., `CURR_PP_FLAG='Y'` count != 1, non-numeric `PP_END_YEAR`)
   - Include rows that should generate errors in ERROR_TBL
   - Include duplicate keys to test uniqueness handling
   - Include NULL values in mandatory fields to test completeness checks
   - Include out-of-range values (e.g., `PP_NUM > 26`) to test accuracy checks
   - For RUNFOREVER jobs: include `PROCESSED_FLAG` variations to test polling logic, and plan timed inserts of new rows during test runs to validate the polling loop picks up late-arriving data

5. **Cross-database lookup simulation**: If any workflow uses lookups against a separate database (e.g., `INFO_NATE` for `lkp_PS_JPM_JP_ITEMS`), create a second Oracle schema or user to simulate the cross-connection. Populate its lookup tables with matching and non-matching keys to validate cross-connection join behavior

6. **Multi-target write data**: For workflows that write to multiple target tables (e.g., Job 6 writing to `NWK_ACTION_PRIMARY_TBL`, `NWK_ACTION_SECONDARY_TBL`, `EHRP_RECS_TRACKING_TBL`), ensure synthetic input includes row distributions that drive data into each target table. After test runs, assert per-target row counts independently

7. **Flat file sources**: For workflows that read headerless flat files (e.g., COMPTIME CSV), generate test files matching the exact column layout. Include files with: correct data, missing columns, extra columns, empty file (zero bytes), and single-row file

8. **Post-SQL DELETE data**: For workflows with Post-SQL DELETEs (e.g., FDA_Leave deleting employees without `fda_rec_type='12'`), include employees both with and without the required record type. After test runs, verify the DELETE removed exactly the expected employees

9. **Verify schema setup**: Run the setup script and confirm all tables are created with correct column types by querying Oracle metadata

<verification>
- Oracle XE Docker container is running and accessible on port 1521
- All source, target, lookup, counter, and error tables exist with correct DDL
- Cross-database schemas are set up for workflows with cross-connection lookups
- Functional test data is loaded covering all code paths and edge cases
- Performance test data is loaded at 10K+ rows for high-volume tables
- Headerless flat file test data is generated with correct column layout
- Post-SQL DELETE scenarios include both qualifying and non-qualifying rows
- Multi-target write data includes distributions that drive rows into each target table
- Schema verification query confirms table structures match Informatica XML definitions
</verification>
</phase>

<phase name="Testing & Validation" id="5">
## Phase 5: Testing & Validation

Execute comprehensive testing at three levels: unit tests, integration tests against Oracle, and performance tests.

### Unit Tests (per job)

Write pytest-based unit tests for each job covering:

1. **Filter logic**: Verify filters produce correct row counts with known input data
2. **Date/numeric conversions**: Test valid dates, invalid dates, nulls, edge formats
3. **Expression logic**: Test all `IIF`, `DECODE`, string manipulation, sign parsing expressions
4. **Lookup joins**: Verify correct match with single match, multiple matches (deterministic), no match (null handling)
5. **Aggregations**: Verify `COUNT`, `SUM`, `MAX` produce expected results
6. **Parameter validation**: Test valid params, invalid params (non-numeric), null params — verify ABORT behavior
7. **Router branches**: Verify each branch receives the correct subset of rows
8. **Error handling**: Test that error thresholds trigger abort, that rollback occurs on write failure

Use PySpark's local mode (`local[*]`) with in-memory DataFrames for unit tests — no Oracle dependency.

### Integration Tests (against Oracle)

1. Run each job end-to-end against the local Oracle XE with functional test data
2. After each job execution, verify:
   - Target table row counts match expected values
   - COUNTER_TBL values match expected counts
   - ERROR_TBL contains expected error records
   - No unexpected rows in reject/error outputs
3. **Workflow sequencing tests**: Verify that sessions run in correct order and that a failed predecessor prevents subsequent sessions from running
4. **Email notification tests**: Verify email subject includes correct environment prefix, recipients match, and emails are sent on both success and failure
5. **Post-SQL tests**: Verify any post-session SQL (e.g., DELETE statements) executes correctly
6. **Data reconciliation**: For each job, verify the identity: `source_rows_read = target_rows_written + error_rows + rejected_rows`
7. **Idempotency test**: Run each job twice with the same parameters and verify results are consistent (use MERGE/UPSERT patterns where applicable)

### Performance Tests

1. Load the performance test data tier (10K-1M+ rows)
2. Run each job and capture:
   - Total duration
   - Per-session duration
   - Source rows/second throughput
   - Target rows/second throughput
   - Spark shuffle read/write bytes
   - Peak executor memory usage
3. Verify performance meets acceptable thresholds (define baseline from Informatica metrics if available)
4. Test with varying Spark configurations: different `shuffle_partitions`, `executor_memory`, `broadcast_threshold` values
5. For RUNFOREVER jobs: run for at least 10 minutes and monitor for memory leaks

### Data Quality Validation

For each job, run the following concrete checks with explicit pass/fail criteria:

1. **Completeness threshold**: Assert `tgt_success_rows >= src_success_rows * 0.95` (configurable). Test with a truncated source (e.g., 10 rows when 10,000 expected) and verify the job aborts. Test with a zero-row source and verify abort
2. **Mandatory field null checks**: Assert that SSN, PP_END_YEAR, and other business-required fields are NOT NULL before writing to target. Load test data with NULLs in these fields and verify they are routed to ERROR_TBL, not the target table
3. **Duplicate detection**: Insert duplicate source records (e.g., same SSN + PP_END_DATE in COMPTIME) and verify the job detects and handles them (dedup or reject to ERROR_TBL)
4. **PK uniqueness**: Attempt to INSERT rows that violate target table PK constraints. Verify the job catches the violation and rolls back rather than committing partial data
5. **Cross-table consistency**: After each job run, assert `COUNTER_TBL.COUNTER_VALUE == actual COUNT(*) from target table` for the same PP_END_YEAR + PP_NUM + CYCLE_ID
6. **Range/domain validation**: Insert rows with `PP_NUM = 27` (out of range 1-26), `PP_END_YEAR = 1900`, invalid SSN formats (e.g., `000xxxxxx`, `666xxxxxx`). Verify these are caught and routed to ERROR_TBL
7. **Data freshness**: Assert `MAX(EFFDT)` in source data falls within the expected pay period window. Test with stale data and verify a warning is logged
8. **SLA monitoring**: Assert total job duration < configured threshold. Test by setting an artificially low threshold and verifying the job flags the SLA breach
9. **Row count reconciliation**: For every job, assert `src_success_rows == tgt_success_rows + error_rows + rejected_rows`. This must balance exactly

Run all tests:
```bash
pytest pyspark_migration/tests/ -v --tb=short
```

<verification>
- All unit tests pass with `pytest` in local Spark mode
- All integration tests pass against Oracle XE with functional test data
- Performance tests complete within acceptable thresholds
- Data quality validation confirms all 7 dimensions are checked
- Row count reconciliation balances for every job (source = target + errors + rejects)
- Test coverage report shows >= 80% line coverage on job code
</verification>
</phase>

<phase name="Documentation, PR & Delivery" id="6">
## Phase 6: Documentation, PR & Delivery

Produce the migration deliverables and submit for review.

1. **Generate a Migration Guide document** (Word or Markdown) containing:
   - Executive summary with migration order table
   - Pre-migration baselining checklist (Informatica performance data collection, counter/error snapshots, target table row counts)
   - Job-by-job migration details: Informatica workflow description → PySpark implementation → transformation mapping → testing results
   - Performance comparison template tables (one per job) with Informatica (Before) and PySpark (After) columns for: duration, source/target row counts, errors, memory, shuffle partitions
   - Informatica-to-PySpark glossary
   - Optimization & data quality gap analysis findings from Phase 2
   - Rollback plan: keep all Informatica XML exports, backup credential files, backup parameter files, document RUNFOREVER stop/restart procedure

2. **Present the Migration Guide to the user** within the session as an attachment

3. **Create a PR** with all migration code, tests, scripts, and documentation. The PR description should include:
   - Summary of what was migrated
   - File structure overview
   - Test execution results
   - Review and testing checklist for the human reviewer
   - Known caveats (e.g., IICS DTT ≠ PowerCenter workflows, Oracle execution validates approach but test production job classes separately)

4. **Commit the performance test results** (JSON or CSV) alongside the code so reviewers can verify test coverage

<verification>
- Migration Guide document is generated and presented to the user
- PR is created with all code, tests, scripts, and documentation
- PR description includes review checklist and test results
- Performance comparison templates are included in the documentation
- Rollback plan is documented
</verification>
</phase>

## Specifications

- Every Informatica transformation must have a documented PySpark equivalent — no business logic may be silently dropped
- Every PySpark job must include built-in performance monitoring (session-level metrics: timing, row counts, errors, throughput)
- Every PySpark job must implement fault tolerance: configurable error threshold, transaction rollback on write failures, optional checkpointing
- Data quality checks must cover all 7 DAMA dimensions: Completeness, Validity, Uniqueness, Consistency, Accuracy, Timeliness, Integrity
- Tests must include both functional tests (correctness with small data) and performance tests (throughput with large synthetic data)
- Row count reconciliation must balance for every job: `source_rows_read = target_rows_written + error_rows + rejected_rows`
- Lookup joins must use deterministic match logic — never "Use Any Value" / random selection
- Financial data fields must use `DecimalType` with appropriate precision/scale (not float/double)
- All credentials must be loaded from environment variables or a secrets manager — never hardcoded or in plain text files
- The migration must produce a before/after comparison artifact for each job

## Advice and Pointers

- Start with the simplest workflow to establish the common utility patterns, then reuse those patterns across all subsequent jobs. This matches the session's successful approach of migrating Pay_Calendar first
- When parsing Informatica XML, pay close attention to `ASSOCIATED_SOURCE_INSTANCE` attributes on transformations — these define the data flow lineage
- Informatica's `SESSIONEXTENSION` elements contain the session-level configs (commit interval, DTM buffer, error handling) — extract these systematically
- For Source Qualifier SQL overrides, check the `SQLQUERY` attribute — if present, use it as the JDBC pushdown query rather than reading the full table
- Oracle XE Docker (`gvenzl/oracle-xe:21-slim`) is the recommended local test database — it starts in ~30 seconds and supports all standard Oracle SQL
- When translating Informatica Lookups with `LookupPolicy = "Use Any Value"`, replace with a deterministic strategy: join with `row_number()` window to pick first match by a defined ordering (e.g., EFFDT DESC)
- For RUNFOREVER workflows (like EHRP2BIIS_UPDATE), implement signal handling (`signal.SIGTERM`, `signal.SIGINT`) for graceful shutdown rather than abrupt termination
- The `db_manager.read_jdbc()` method should NOT call `df.count()` for logging — this forces a full table scan and doubles I/O. Log row counts only after the DataFrame is materialized for a write operation
- When using explicit column selection in source joins with multiple tables, list columns explicitly to avoid `ORA-00918: column ambiguously defined` errors — never use `SELECT t1.*, t2.*` when tables share column names
- If the flat file source has no header, define an explicit `StructType` schema rather than inferring — this prevents `AnalysisException` when referencing named columns

## Forbidden Actions

- Do not skip any Informatica transformation during migration — every filter, expression, lookup, router, aggregator, and update strategy must be translated
- Do not use non-deterministic lookup match policies — always use deterministic join logic with explicit ordering
- Do not hardcode credentials in source code or plain text files
- Do not use `float` or `double` types for financial/monetary data — use `DecimalType`
- Do not call `df.count()` inside `read_jdbc` or similar read methods for logging purposes
- Do not use `SELECT *` in joins between tables that share column names
- Do not skip the local Oracle test database setup — all jobs must be validated against a real database, not just mock data
- Do not commit the PR without running all tests and including test results in the PR description

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
