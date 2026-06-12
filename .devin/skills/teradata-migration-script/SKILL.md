---
name: teradata-migration-script
description: "Converted from Devin playbook: Teradata Migration Script"
triggers:
  - user
  - model
---

# Teradata Migration Script

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Teradata Migration Script (playbook-7d9b9cffc89c460aa562357d4ddac5d1), macro `!teradata_migration`

## Procedure

# Teradata Migration Script

## Overview

This playbook guides the migration of Teradata on-premises data warehouse artifacts (DDL, stored procedures, BTEQ scripts, macros, load utilities) into a modern dbt Core project targeting Snowflake, Databricks, or Postgres. It covers SQL syntax translation, stored procedure decomposition into modular dbt models, test generation, CI/CD setup, and data validation.

## What's Needed From User

- **Repository URL**: GitHub repository containing the Teradata source artifacts (DDL, stored procedures, BTEQ, TPT, FastLoad scripts) and/or an existing dbt project scaffold
- **Target platform**: Snowflake, Databricks, or Postgres (for local dev/CI)
- **Teradata source directory**: Path within the repo where original Teradata SQL files live (e.g., `teradata/`)
- **dbt project directory**: Path where the migrated dbt project should be created or already exists (e.g., `dbt_project/`)
- **(Optional) Database credentials**: Connection details if running the migration end-to-end against a live target
- **(Optional) Priority tables or domains**: If the user wants to migrate a subset first (e.g., "start with customer and transaction tables only")

<phase name="Discovery & Assessment" id="1">
## Phase 1: Discovery & Assessment

Inventory and understand all Teradata artifacts in the repository before writing any migration code.

1. Clone the repository and explore its directory structure. Identify directories containing Teradata artifacts (DDL, stored procedures, BTEQ scripts, macros, TPT jobs, FastLoad/MultiLoad scripts, scheduled jobs). If the user has provided a live Teradata connection, also query DBC system views (`DBC.TablesV`, `DBC.StoredProcsV`, `DBC.MacrosV`, `DBC.IndicesV`, `DBC.All_RI_ChildrenV`) to get a complete inventory including object counts, dependencies, and data volumes
2. Read every Teradata DDL file and catalogue all objects:
   - Tables (note SET vs MULTISET, PRIMARY INDEX columns, PARTITION BY RANGE_N definitions, FALLBACK/JOURNAL settings, COMPRESS values)
   - Views and staging views
   - Indexes and statistics definitions
3. Read every stored procedure and document:
   - Input parameters and control flow (cursors, loops, error handlers)
   - Teradata-specific constructs used: `ACTIVITY_COUNT`, `MERGE INTO`, `LOCK ROW FOR ACCESS`, `COLLECT STATISTICS`, `PERIOD` data types, `ZEROIFNULL`/`NULLIFZERO`, `QUALIFY`, `CSUM`/`MAVG`/`MDIFF`
   - Business logic steps (SCD Type 2 handling, incremental loads, upserts, balance calculations)
4. Read BTEQ, TPT, FastLoad, and MultiLoad scripts to understand data ingestion patterns
5. Read any existing migration guide or documentation in the repo
6. If a dbt project scaffold already exists, read `dbt_project.yml`, `profiles.yml.example`, `packages.yml`, existing models, macros, seeds, snapshots, and tests
7. Produce a migration scope summary listing: total object counts by type, Teradata-specific constructs requiring translation, stored procedure decomposition map (each procedure mapped to planned dbt models), and any identified risks or complexities

Only ever create the todo list for the current phase.

<verification>
- All Teradata DDL files have been read and catalogued
- All stored procedures have been analyzed and decomposed into planned dbt model mappings
- All BTEQ/TPT/FastLoad/MultiLoad scripts have been reviewed
- A migration scope summary has been produced and shared with the user
- Teradata-specific constructs requiring translation have been identified
</verification>
</phase>

<phase name="SQL Syntax Translation & dbt Model Creation" id="2">
## Phase 2: SQL Syntax Translation & dbt Model Creation

Translate Teradata SQL into dbt-compatible SQL and create the dbt project structure.

1. Create a new branch for the migration work
2. If no dbt project exists, scaffold one with `dbt_project.yml`, `profiles.yml.example`, and `packages.yml` including:
   - `dbt-labs/dbt_utils` (surrogate keys, date spine, etc.)
   - `calogica/dbt_expectations` or `metaplane/dbt_expectations` (statistical tests)
   - `dbt-labs/codegen` (optional, for generating base models)
3. **Staging layer** — Create one staging model per source table:
   - Define sources in `_staging__sources.yml` with freshness checks
   - Apply deduplication for SET tables (use `ROW_NUMBER()` + `QUALIFY` for Snowflake, subquery wrapper for Databricks/Postgres)
   - Cast Teradata data types to target equivalents (see translation rules below)
   - Replace `CHARACTER SET LATIN NOT CASESPECIFIC` → standard VARCHAR
   - Replace `FORMAT 'YYYY-MM-DD'` → `TO_DATE()` / `TO_CHAR()` as needed
   - Replace `COMPRESS (...)` → remove (handled automatically by target platform)
4. **Intermediate layer** — Decompose stored procedures into intermediate models:
   - Replace cursor loops with set-based `MERGE` or CTE-based operations
   - Replace `VOLATILE TABLE` declarations with CTEs or ephemeral models
   - Translate Teradata functions using these rules:
     - `ZEROIFNULL(x)` → `COALESCE(x, 0)`
     - `NULLIFZERO(x)` → `NULLIF(x, 0)`
     - `QUALIFY ROW_NUMBER() OVER (...) = 1` → keep as-is for Snowflake; wrap in subquery for Databricks AND Postgres
     - `CSUM(col, order_col)` → `SUM(col) OVER (ORDER BY order_col ROWS UNBOUNDED PRECEDING)`
     - `MAVG(col, n, order_col)` → `AVG(col) OVER (ORDER BY order_col ROWS BETWEEN n-1 PRECEDING AND CURRENT ROW)`
     - `MDIFF(col, n, order_col)` → `col - LAG(col, n) OVER (ORDER BY order_col)`
     - `date1 - date2` (integer days) → `{{ datediff('date2', 'date1', 'day') }}` (dbt cross-database macro)
     - `HASHROW(cols)` → `{{ dbt_utils.generate_surrogate_key(['col1', 'col2']) }}`
     - `LOCK ROW FOR ACCESS` → remove entirely
     - `COLLECT STATISTICS` → remove entirely
     - `ACTIVITY_COUNT` → remove (not applicable in dbt)
     - `PERIOD(DATE)` → split into `valid_from DATE, valid_to DATE` columns
     - `NORMALIZE ON` → custom gap-and-island SQL
     - `LIKE ANY (list)` → keep as-is for Snowflake; convert to multiple `LIKE` with `OR` for Databricks/Postgres
     - `LIKE ALL (list)` → keep as-is for Snowflake; convert to multiple `LIKE` with `AND` for Databricks/Postgres
     - `OREPLACE(str, from, to)` → `REPLACE(str, from, to)`
     - `OTRANSLATE(str, from, to)` → `TRANSLATE(str, from, to)`
     - `ADD_MONTHS(date, n)` → `{{ dateadd('month', n, 'date') }}` (dbt cross-database macro)
     - `CAST(x AS DATE FORMAT 'YYYYMMDD')` → Snowflake: `TO_DATE(x, 'YYYYMMDD')` / Databricks: `TO_DATE(CAST(x AS STRING), 'yyyyMMdd')`
     - `SAMPLE n` → `TABLESAMPLE` or `LIMIT`
     - `TOP n` → `LIMIT n`
   - Use `{{ ref('model_name') }}` for all inter-model references
   - Use `{{ source('schema', 'table') }}` for raw source references
5. **Marts layer** — Create dimension and fact models:
   - Use `{{ dbt_utils.generate_surrogate_key() }}` for surrogate keys
   - Apply incremental materialization (`materialized='incremental'`) for large fact tables with appropriate `unique_key` and `incremental_strategy`
   - Map Teradata `PRIMARY INDEX` → Snowflake `CLUSTER BY` / Databricks `ZORDER BY` (add as config)
   - Map `PARTITION BY RANGE_N` → Databricks `PARTITIONED BY` (Snowflake handles automatically)
6. **Macros** — Create reusable dbt macros for recurring Teradata patterns (e.g., `qualify_dedup`, `cast_teradata_date`, `zeroifnull`)
7. **Snapshots** — Convert SCD Type 2 cursor-based logic to `dbt snapshot` blocks using `strategy='check'` or `strategy='timestamp'`
8. **Seeds** — Add any reference data CSVs to `seeds/`

Only ever create the todo list for the current phase.

<verification>
- All staging models have been created with proper source definitions
- All stored procedures have been decomposed into intermediate and/or mart models
- Teradata-specific SQL constructs have been translated to cross-database or target-specific equivalents
- All models use `{{ ref() }}` and `{{ source() }}` correctly
- dbt_project.yml is properly configured with materializations and schemas
- packages.yml includes required packages
</verification>
</phase>

<phase name="Testing & Validation" id="3">
## Phase 3: Testing & Validation

Add comprehensive tests and validate the migration locally.

1. **Schema tests** — Add `_models.yml` files for every model layer with:
   - `unique` and `not_null` on primary/surrogate keys
   - `accepted_values` for enum-like columns (status fields, type codes, flags)
   - `relationships` tests between fact and dimension tables
2. **Custom data tests** — Create tests in `tests/` for business rules:
   - Balance invariants (e.g., savings accounts should not have negative balances)
   - Score ranges (e.g., probability of default between 0 and 1)
   - Reconciliation counts (source vs target row counts by date)
3. **dbt_expectations tests** — Add statistical distribution tests for numeric columns (value ranges, means, standard deviations)
4. **Source freshness** — Configure `freshness` in source YAML with `warn_after` and `error_after` thresholds
5. **Local execution** — Run the full pipeline locally:
   - Start Postgres via `docker-compose up -d` (if a docker-compose.yml exists)
   - Set up `~/.dbt/profiles.yml` for the local target
   - Run `dbt deps && dbt seed && dbt run && dbt test`
   - Fix any failures — common issues include:
     - `datediff` bare function calls → must use `{{ datediff() }}` Jinja macro for Postgres
     - `generate_surrogate_key` returns text → cannot use `abs()` directly on it in Postgres (need hex-to-int conversion)
     - Ambiguous column names in joins → qualify with table alias
     - Deprecated packages → check for renamed/removed packages on dbt hub
6. Iterate until all models pass and all tests pass

Only ever create the todo list for the current phase.

<verification>
- Schema tests exist for all models (unique, not_null, accepted_values, relationships)
- Custom business rule tests exist and pass
- `dbt run` completes successfully with all models built
- `dbt test` completes successfully with all tests passing
- Any Postgres-specific compatibility issues have been resolved
</verification>
</phase>

<phase name="CI/CD & PR Delivery" id="4">
## Phase 4: CI/CD & PR Delivery

Set up CI, create the PR, and deliver the migration.

1. **CI workflow** — Create or update `.github/workflows/dbt_ci.yml`:
   - Spin up a Postgres service container
   - Install dbt-postgres and dbt-core
   - Create a CI-specific `profiles.yml`
   - Run `dbt deps` → `dbt seed` → `dbt run` → `dbt test` → `dbt docs generate`
   - Ensure no `DBT_PROFILES_DIR` env var conflicts with `working-directory` settings
   - Add a step to create the raw schema and load sample data if needed
2. **Commit and push** all migration artifacts (models, tests, macros, seeds, snapshots, CI workflow, packages.yml, documentation)
3. **Create a PR** with a clear description covering:
   - Summary of what was migrated (object counts, domains covered)
   - Teradata constructs translated and how
   - Test results (model count, test count, pass/fail)
   - Known limitations or portability notes (e.g., Postgres-specific casts that differ from Snowflake)
   - Review checklist for the human reviewer
4. **Wait for CI** to pass. If CI fails, read the job logs, fix the issues, and push again
5. **Share the PR link** and any relevant output files with the user

Only ever create the todo list for the current phase.

<verification>
- CI workflow exists and runs dbt deps, seed, run, test
- PR has been created with a descriptive summary
- CI checks pass on the PR
- PR link and migration summary have been shared with the user
</verification>
</phase>

## Specifications

- Every Teradata DDL file, stored procedure, BTEQ script, and macro in the source directory must be accounted for in the migrated dbt project
- All dbt models must build successfully against the target platform (or Postgres for CI)
- All dbt tests must pass
- Staging models must be materialized as views, intermediate models as ephemeral (or tables if needed for performance), and mart models as tables or incremental
- The PR must include a migration summary documenting what was translated and any known limitations
- Validation: `dbt run` and `dbt test` both succeed with zero failures in CI

## Advice and Pointers

- **Use dbt cross-database macros** (`{{ datediff() }}`, `{{ dateadd() }}`, `{{ type_string() }}`) instead of platform-specific SQL wherever possible. This avoids Postgres CI failures when the target is Snowflake or Databricks
- **`generate_surrogate_key` returns a text hash** — if you need to do integer arithmetic on it (e.g., modulo for bucketing), you must convert it. On Postgres: `('x' || substring(hash, 1, 8))::bit(32)::int`. On Snowflake: `to_number(substring(hash, 1, 8), 'XXXXXXXX')`
- **Teradata `QUALIFY` works natively in Snowflake** but not in Postgres or Databricks. For cross-database compatibility, always use the subquery pattern: `SELECT * FROM (SELECT *, ROW_NUMBER() OVER (...) AS rn FROM ...) WHERE rn = 1`. This ensures CI passes on Postgres even when the production target is Snowflake
- **`ZEROIFNULL(x)` works natively in Snowflake** but not in Postgres or Databricks. Always use `COALESCE(x, 0)` for cross-database compatibility
- **`PERIOD(DATE)` columns** have no equivalent in modern platforms. Split into `valid_from` and `valid_to` date columns
- **SET tables** enforce row-level uniqueness in Teradata. In dbt, handle this with explicit deduplication in staging models using `ROW_NUMBER()` partitioned by the primary index columns
- **Cursor loops in stored procedures** should always be converted to set-based SQL operations. dbt does not support procedural logic
- **Check `packages.yml` for deprecated packages** before running `dbt deps`. The `calogica/dbt_expectations` was renamed to `metaplane/dbt_expectations`. The standalone `dbt_date` package may be a transitive dependency of `dbt_expectations`
- **When fixing CI failures**, pay attention to `DBT_PROFILES_DIR` — if set globally in the workflow, it resolves relative to `working-directory`, which can cause double-nesting (e.g., `./dbt_project/dbt_project/`)
- **Data type conversion nuances**: Teradata `FLOAT` maps to `DOUBLE` in Databricks (not `FLOAT`). Teradata `TIME` has no native equivalent in Databricks — use `STRING` with formatted values. Teradata `TIMESTAMP WITH TIME ZONE` maps to `TIMESTAMP_TZ` in Snowflake but just `TIMESTAMP` in Databricks (timezone stored in session config). Teradata `CLOB` → Snowflake `VARCHAR(16777216)` / Databricks `STRING`
- **Teradata `VOLATILE TABLE`** declarations within stored procedures should be converted to CTEs within the dbt model or to ephemeral dbt models. Never create actual temporary tables in dbt
- **`MERGE INTO` patterns** from stored procedures map to dbt incremental models with `incremental_strategy='merge'` and a defined `unique_key`. Review the merge conditions carefully as Teradata MERGE may have multiple WHEN clauses
- **Performance consideration**: For large fact tables, benchmark query performance after migration. Track `dbt run` build times per model. If incremental models are used, verify that the incremental predicate (e.g., date-based) matches the original Teradata PPI partitioning strategy

## Forbidden Actions

- Do not delete or modify the original Teradata source files — they serve as reference documentation
- Do not hardcode database credentials in any committed file (use `profiles.yml.example` with placeholders and `~/.dbt/profiles.yml` for actual credentials)
- Do not skip test creation — every model must have at least `unique` and `not_null` tests on its primary key
- Do not use `dbt run --full-refresh` in CI without understanding the implications for incremental models
- Do not push directly to `main` — always create a feature branch and PR

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
