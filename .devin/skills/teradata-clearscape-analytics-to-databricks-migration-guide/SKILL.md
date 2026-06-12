---
name: teradata-clearscape-analytics-to-databricks-migration-guide
description: "Converted from Devin playbook: Teradata ClearScape Analytics to Databricks Migration Guide"
triggers:
  - user
  - model
---

# Teradata ClearScape Analytics to Databricks Migration Guide

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Teradata ClearScape Analytics to Databricks Migration Guide (playbook-5c05a16d80a54b489d70027abe405c46), macro `!teradata_to_databricks`

## Procedure

# Teradata ClearScape Analytics → Databricks Migration Guide

## Overview

This playbook guides the analysis of a Teradata ClearScape Analytics environment and produces a structured migration guide for moving to Databricks. It covers all 62 analytics skills across ML, time series, signal processing, text analytics, statistical testing, and advanced pattern detection — mapping each to its Databricks equivalent and identifying gaps, risks, and a prioritized migration order.

## What's Needed From User

- Link to the Teradata analytics skills repository (or access to the ClearScape Analytics skill definitions)
- Any specific Teradata workloads or functions they rely on most heavily (optional — helps with prioritization)
- Whether they use UAF (Unbounded Array Framework) for time series or signal processing
- Target Databricks environment details (optional — e.g., runtime version, Unity Catalog, Mosaic AI availability)

<phase name="Inventory & Pattern Analysis" id="1">
## Phase 1: Inventory & Pattern Analysis

Catalog every Teradata analytical capability and identify the execution framework each one uses.

1. Clone or access the Teradata analytics skills repository
2. List all skill directories and categorize them (ML, time series, DSP, text, statistical, data quality, BYOM, vector search, advanced analytics)
3. Read representative SKILL.md files from each category to understand the function signatures, parameters, and SQL patterns
4. Read representative SQL scripts (model_training.sql, workflow templates, etc.) to capture the exact Teradata-specific syntax
5. Classify every skill by its execution framework:
   - **SQLE** — `SELECT * FROM TD_Function(ON table USING ...)` pattern
   - **UAF** — `EXECUTE FUNCTION INTO VOLATILE ART(...)` with `SERIES_SPEC` pattern
   - **MLE** — `SELECT * FROM Function(ON table PARTITION BY ... ORDER BY ... USING ...)` pattern
   - **BYOM** — `ONNXPredict` / `PMMLPredict` with model table DIMENSION joins
6. Document all Teradata-specific SQL constructs found: `CREATE MULTISET TABLE`, `SELECT TOP N`, `DBC.ColumnsV`, `VOLATILE TABLE`, `PRIMARY INDEX`, `FETCH FIRST N ROWS ONLY`, `QUALIFY`, etc.
7. Note version and licensing dependencies (e.g., Teradata 20.x for SHAP/SMOTE/HNSW, UAF licensing for time series/DSP)

<verification>
- All analytics skills have been identified and listed
- Each skill has been classified by framework (SQLE, UAF, MLE, BYOM)
- Representative SQL scripts have been read and Teradata-specific syntax patterns documented
- Version and licensing dependencies have been captured
</verification>
</phase>

<phase name="Capability Mapping & Gap Analysis" id="2">
## Phase 2: Capability Mapping & Gap Analysis

Map every Teradata function to its Databricks/Spark equivalent and identify gaps.

1. For each category, create a mapping table with columns: Teradata Function | Databricks Equivalent | Migration Complexity | Notes
2. Map **ML functions** (TD_XGBoost, TD_GLM, TD_KMeans, etc.) to Spark MLlib equivalents
3. Map **data preprocessing** functions (TD_ScaleFit, TD_OneHotEncoding, TD_SMOTE, etc.) to Spark ML feature transformers
4. Map **model evaluation** functions (TD_ROC, TD_SHAP, evaluators) to Spark evaluators and Python libraries
5. Map **time series** functions (TD_ARIMAFORECAST, TD_ACF, TD_PACF, etc.) — note that these require `pandas_udf` + `statsmodels`/`pmdarima` since Spark has no native time series analytics
6. Map **signal processing** functions (TD_DFFT, TD_IDFFT, TD_CONVOLVE, etc.) — note these require `scipy.signal` via UDFs with no native Spark DSP
7. Map **text analytics** (TD_TFIDF, TD_WordEmbeddings, etc.) to Spark ML feature extractors and Hugging Face
8. Map **statistical tests** (TD_ANOVA, TD_ChiSq, etc.) to `scipy.stats` via UDFs
9. Map **data profiling** (TD_UnivariateStatistics, TD_Histogram, etc.) to native Spark/Databricks profiling
10. Map **advanced analytics** (TD_nPath, TD_Sessionize, TD_Attribution, ChangePointDetection) — flag nPath as having NO equivalent
11. Map **BYOM** (ONNXPredict, PMMLPredict) to MLflow model serving
12. Map **vector search** (HNSW) to Databricks Vector Search
13. Document the SQL dialect differences table: Teradata SQL construct → Spark SQL equivalent
14. Identify the critical architectural differences: in-database compute vs. distributed Spark, AMP parallelism vs. Spark partitions, Fit/Transform SQL pattern vs. ML Pipelines, model tables vs. MLflow artifacts

<verification>
- Every Teradata function has been mapped to a Databricks equivalent or flagged as a gap
- Migration complexity has been rated for each category
- SQL dialect differences are documented
- Architectural differences between Teradata and Databricks are clearly described
</verification>
</phase>

<phase name="Migration Guide Assembly & Delivery" id="3">
## Phase 3: Migration Guide Assembly & Delivery

Compile findings into a structured, actionable migration guide and deliver to the user.

1. Write the migration guide as a markdown file with these sections:
   - **Executive Summary** — key takeaways in 3-5 bullets
   - **The Three Frameworks** — SQLE/UAF/MLE/BYOM breakdown with skill counts and migration paths
   - **Critical SQL Syntax Differences** — with before/after code examples for each pattern (table-operator, UAF EXECUTE FUNCTION, MLE PARTITION BY, Teradata-specific constructs)
   - **Capability-by-Capability Migration Map** — organized by category with Teradata Function → Databricks Equivalent tables
   - **Architectural Differences** — in-database vs. distributed, Fit/Transform patterns, metadata systems, table types
   - **Migration Priority Recommendations** — 4-tier system (Tier 1: quick wins → Tier 4: re-architecture)
   - **Key Risks and Mitigations** — UAF gap, nPath gap, performance model shift, SQL rewrite volume, model serialization
   - **What Gets Better on Databricks** — MLflow, Vector Search, Python ecosystem, GPU support, AutoML
   - **Recommended Migration Approach** — step-by-step strategy
2. Ensure all code examples show both Teradata and Databricks syntax side by side
3. Present the completed migration guide directly to the user within the session
4. Ask if the user wants to drill deeper into any specific category or adjust priorities

<verification>
- Migration guide markdown file has been created with all required sections
- Code examples show Teradata and Databricks syntax side by side
- The guide has been presented to the user in the session
- Migration priorities are clearly ranked in tiers
</verification>
</phase>

## Specifications

- The deliverable is a comprehensive markdown migration guide
- Every Teradata ClearScape Analytics function must be mapped or flagged as a gap
- Migration complexity must be rated per category (Low / Medium / High / Highest)
- Priorities must be organized into actionable tiers
- Code examples must show real Teradata SQL and real Databricks/PySpark equivalents
- The guide should be presentable to engineering leadership as a migration planning document

## Advice and Pointers

- The three execution frameworks (SQLE, UAF, MLE) are the most important structural concept — they determine migration strategy more than individual function names
- UAF is the highest-risk area: 18 skills with no Databricks equivalent. The `EXECUTE FUNCTION INTO VOLATILE ART` + `SERIES_SPEC` pattern must be replaced with `pandas_udf` + Python libraries
- TD_nPath is Teradata's most unique function (regex-style sequential event pattern matching) — flag it prominently as a gap
- BYOM and Vector Search are quick wins — MLflow and Databricks Vector Search are mature and may be upgrades
- Don't just list function mappings — explain the architectural shift (in-database compute → distributed Spark, model tables → MLflow artifacts)
- When the user specifies which workloads they rely on most, adjust the priority tiers accordingly

## Forbidden Actions

- Do not attempt to actually migrate any code or write Databricks implementations
- Do not make up Databricks equivalents that don't exist — flag gaps honestly
- Do not skip reading actual SQL scripts; surface-level SKILL.md reading misses critical syntax patterns
- Do not present the guide only as a file path — deliver the content directly to the user in the session

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
