---
name: sas-to-python
description: "Converted from Devin playbook: SAS to Python"
triggers:
  - user
  - model
---

# SAS to Python

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SAS to Python (playbook-8249acfb0a9d4fe1a3b100fe4b13dd0a), macro `!sas_to_python`

## Procedure

# Playbook: SAS to Python Migration

## Overview

This playbook outlines how to systematically migrate analytics workflows, data pipelines, or statistical models from SAS to Python, ensuring parity in logic, data, and results throughout the migration.

## User Inputs

* URL or directory containing SAS project files (`.sas`, `.egp`, datasets)
* Target environment for Python execution (e.g., local, cloud, CI/CD)
* Output formats to validate (e.g., CSV, Excel, plots)
* Python package/tool preferences (e.g., pandas, DuckDB, Airflow)

## Procedure

1. **Clone the SAS Project Repository**

   * Use Git if under source control, or collect `.sas`, `.egp`, and data files manually.

2. **Inventory SAS Artifacts**

   * Catalog scripts, stored processes, macros, and data inputs/outputs.
   * Use a tracking table to capture file type, purpose, and dependencies.

3. **Set Up Python Environment**

   * Create a virtual environment and install analytics packages:

     ```bash
     python -m venv venv
     source venv/bin/activate
     pip install pandas numpy scipy matplotlib seaborn statsmodels jupyter
     pip install sas7bdat openpyxl pyreadstat
     ```

4. **Translate SAS Logic to Python**

   * Parse SAS code structure:

     * `DATA` steps → `pandas`
     * `PROC SQL` → `pandas.merge`, `DuckDB`, or `sqlite3`
     * `PROC MEANS` → `df.describe()` or `df.agg()`
     * `MACRO` → Python `def` or class
   * Rewrite logic using idiomatic Python and modular functions.

5. **Validate Output and Parity**

   * Compare Python outputs to SAS results.
   * Use `pandas.testing.assert_frame_equal()` for numerical verification.
   * Add unit tests as needed.

6. **Rebuild Workflows**

   * Replace EG flows or batch jobs with:

     * `Jupyter Notebooks`
     * CLI tools with `argparse`
     * Scheduled jobs using `cron` or `Airflow`

7. **CI/CD and Deployment**

   * Commit scripts to Git.
   * Configure CI pipelines (GitHub Actions, GitLab CI).
   * Package deployable components using `Flask`, `FastAPI`, or `streamlit`.

## Output Specification

* Functionally equivalent Python scripts for each SAS job
* Validated outputs (CSV, Excel, plots) matching SAS results
* Testable Python modules with unit coverage
* Reproducible workflow automation (Jupyter, Airflow, CLI)

## Forbidden Actions

* 🚫 Hardcoding file paths — use `pathlib.Path()`
* 🚫 Blind logic translation — always validate functional intent
* 🚫 Mixing SAS and Python within the same production workflow
* 🚫 Assuming statistical equivalence without validation

## Advice and Pointers

* Use `jupyter nbconvert` or `papermill` to automate reports
* Prefer `DuckDB` or `pandasql` when migrating `PROC SQL` logic
* Validate handling of edge cases like nulls and datetime formats
* Keep a mapping document of each SAS → Python transformation
* Modularize logic to ease testing and reuse

## Examples

### SAS Logic

```sas
DATA report;
  SET sales;
  WHERE region = 'West';
RUN;

PROC MEANS DATA=report;
  VAR revenue;
RUN;
```

### Python Equivalent

```python
import pandas as pd

sales = pd.read_csv("sales.csv")
report = sales[sales["region"] == "West"]

print(report["revenue"].describe())
```

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
