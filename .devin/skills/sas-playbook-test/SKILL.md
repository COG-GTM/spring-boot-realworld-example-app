---
name: sas-playbook-test
description: "Converted from Devin playbook: SAS Playbook -Test"
triggers:
  - user
  - model
---

# SAS Playbook -Test

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SAS Playbook -Test (playbook-43389185c65e451cb05801b33d5f4ad7), macro `!sas_playbook_-test`

## Procedure

# SAS Repository Reverse-Engineering Playbook

> **Purpose.** A single, end-to-end playbook for reverse-engineering a SAS repository / system and forward-engineering it to Python (or another modern target). It combines two layers:
> - **Part A — System inventory** (the *macro* view): global assets, libraries, interfaces, the shared macro library, the program inventory, cross-job dependency/lineage, scheduling, dead code, and the overall migration program plan.
> - **Part B — Per-job capture** (the *micro* view): one full reverse-engineering pass per SAS program, deep enough to reproduce its runtime *behavior*, not just its syntax.
>
> Fill **one copy of Part A per system** (repo / application area) and **one copy of Part B per program**. Part A's program inventory (§A6) links out to each Part B document.
>
> **Goal of the capture.** Behavioral completeness — record not just what the code says, but what SAS actually *does* at runtime (defaults, implicit conversions, BY-group state, missing-value semantics, sort collation, rounding). Migrating from observed behavior is far safer than migrating from source alone.
>
> **How to use.** Replace every `‹…›` placeholder. Delete sections that genuinely don't apply — but record *why* ("no PROC SQL in this job" is itself useful information). Log anything you can't resolve under **Open Questions**. `[ ]` = unchecked checkbox.
>
> **Legend.** 🔴 = migration risk / likely behavioral difference vs. Python · ⚙️ = automatable (can be extracted by a parser/script) · 👤 = needs human/SME judgment.

---

## How to run this playbook (recommended workflow)

1. **Discovery (⚙️).** Run the extraction tooling in [§A15](#a15-extraction-tooling-checklist-️) to populate the system inventory programmatically: file crawl, `LIBNAME`/`FILENAME` scan, macro/`%include` graph, data-lineage DAG, construct-frequency scan, `PROC CONTENTS` / `DICTIONARY` dumps, `PROC FORMAT CNTLOUT=`, scheduler export.
2. **System inventory (Part A).** Complete §A0–§A11 to establish the macro picture. This determines migration *order* and *blast radius*.
3. **Classify & prioritize (👤).** Score every program in §A6, decide waves in §A13.
4. **Per-job deep dive (Part B).** For each program (in wave order), fill a Part B document. Capture **every** executable step (§B7); cross-check that step count reconciles with the program's DATA/PROC count.
5. **Macro-resolution (🔴, most valuable artifact).** Run representative jobs with `OPTIONS MPRINT MLOGIC SYMBOLGEN;` and analyze the *resolved* code, not the macro source.
6. **Plan the target (👤).** Lock the system architecture (§A12) and per-job forward-engineering notes (§B16).
7. **Validate (🔴).** Build the golden-dataset harness (§B15) and reconcile Python vs. SAS before decommissioning each job (§A13.1).

---

# Part A — System / Repository Inventory

> One copy per SAS *system* (or repo / application area). This is the macro view that sits above the per-job documents: global assets, cross-job dependencies, shared infrastructure, and the migration program plan. Fill a Part B (per-job) document for each program; this part ties them together.

## A0. Document control

| Field | Value |
|---|---|
| System / application name | ‹…› |
| Compiled by | ‹…› |
| Date | ‹YYYY-MM-DD› |
| SAS platform & version | ‹SAS 9.4 M7 / Viya 4 / Grid› |
| OS / host | ‹Linux / Windows / z/OS mainframe 🔴› |
| Status | [ ] Discovery [ ] Mapped [ ] Planned |

---

## A1. Scope & environment 👤⚙️

| Field | Value |
|---|---|
| Business domains covered | ‹…› |
| # programs (.sas) | ‹n› |
| # autocall macros | ‹n› |
| # stored processes / EG projects | ‹n› |
| # scheduled jobs | ‹n› |
| Total LOC | ‹n› |
| SAS products/modules used | ‹Base, STAT, ETS, ACCESS to Oracle, CONNECT, IML, EG, DI Studio 🔴› |
| Interactive vs. batch split | ‹…› |
| Repos / source-control state | [ ] Git [ ] Server filesystem only [ ] Mixed 🔴 |
| Environments | ‹prod / uat / dev — how they differ› 🔴 |
| Excluded from scope (and why) | ‹…› |

---

## A2. Global configuration & shared state 🔴⚙️

These affect *every* job and must be replicated once in the Python platform.

| Asset | Location | Contents / purpose | Migration target |
|---|---|---|---|
| `autoexec.sas` | ‹path› | global options, libnames, autocall setup | startup/config module |
| Config / `.cfg` files | ‹path› | system options, paths | env config |
| Global `OPTIONS` (encoding, locale, yearcutoff, sortseq, …) | | behavioral defaults 🔴 | settings module |
| `FMTSEARCH` / format catalogs | ‹paths› | shared custom formats 🔴 | shared lookup tables |
| `SASAUTOS` autocall path(s) | ‹paths› | shared macros | shared utils package |
| Stored macro variables / control tables | ‹…› | runtime params, watermarks | config/DB |
| Credentials / auth method | ‹auth domain, `.authinfo`, LDAP› 🔴 | DB/file access | secrets manager |

---

## A3. Library (LIBNAME) registry 🔴⚙️

Every libref used anywhere, with the canonical resolution per environment.

| Libref | Engine | Prod path/conn | UAT | Dev | Read/Write | Used by (# jobs) | Data volume | Python target (DB schema / parquet / lake) |
|---|---|---|---|---|---|---|---|---|
| `claims` | BASE | ‹…› | | | RW | ‹n› | ‹GB› | |
| `db` | ORACLE | ‹…› | | | R | ‹n› | | |

> Flag every **environment-specific path/credential** — these are the most common migration breakages. 🔴

---

## A4. External system & data interfaces 🔴⚙️

| Interface | Type (DB / flat file / FTP / API / email / mainframe / pipe / shell `X`) | Direction | Counterparty | Format | Frequency | Auth | Jobs involved | Notes |
|---|---|---|---|---|---|---|---|---|
| Oracle DWH | DB | in/out | DWH team | — | daily | LDAP | ‹…› | pass-through SQL 🔴 |
| Partner extract | FTP/CSV | out | external | CSV | daily | SFTP key | ‹…› | |
| Mainframe feed | flat (EBCDIC) | in | core system | fixed-width | daily | | ‹…› | 🔴 EBCDIC/packed decimal |

> Inventory **shell-outs** (`X`, `SYSTEM()`, `%SYSEXEC`, pipes) and **FTP/email** explicitly — these become OS/integration code, not data logic. 🔴

---

## A5. Shared macro library 🔴👤⚙️

Autocall and `%include` macros reused across jobs. Migrating these once (as shared Python utils) pays off repeatedly — but a change ripples everywhere.

| Macro | Source path | Used by (# jobs / which) | Purpose | Generates code dynamically? | Complexity | Python plan (shared util) |
|---|---|---|---|---|---|---|
| `%load_table` | ‹…› | ‹12 jobs› | generic loader | yes 🔴 | high | core util |
| `%fiscal_period` | ‹…› | ‹many› | date/calendar logic | no | med | date util |
| `%assert` / `%check_rc` | ‹…› | ‹many› | error handling | no | low | helper |

---

## A6. Program inventory & classification ⚙️👤

The master list. One row per program; link to its Part B (per-job) document.

| # | Program | Path | Domain | Class (ETL / report / model / utility / orchestration / dead?) | LOC | # DATA | # PROC | Macro depth | Ext deps | Per-job doc link | Complexity | Risk | Wave |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `etl_load_claims.sas` | ‹…› | Claims | ETL | 420 | 6 | 4 | deep 🔴 | Oracle | [link] | High | High | 2 |
| 2 | `rpt_monthly.sas` | ‹…› | Claims | Report | 120 | 1 | 3 | none | — | [link] | Low | Low | 1 |
| … | | | | | | | | | | | | | |

---

## A7. Dependency & data-lineage map 🔴⚙️

The single highest-value system artifact: it determines migration order and blast radius.

### A7.1 Job execution DAG
> Describe / attach the predecessor→successor graph (scheduler dependencies + `%include`/macro calls).

```text
JOB_EXTRACT → JOB_LOAD_CLAIMS → JOB_FACT → { JOB_RPT, JOB_BI_FEED }
```

### A7.2 Data-lineage table (dataset-level)

| Producer job | Output dataset/table | Consumed by job(s) | Transformation type | Shared/global? |
|---|---|---|---|---|
| ‹JOB_LOAD› | `claims.raw` | ‹JOB_FACT› | filter+clean | |
| ‹JOB_FACT› | `claims.fact_daily` | ‹JOB_RPT, BI› | join+agg | yes |

### A7.3 Hidden coupling 🔴
> Global macro vars, WORK datasets passed between jobs in one session, shared option state, FMTSEARCH dependencies. List anything that breaks when jobs run as independent Python processes.

| Coupling | Set by | Read by | Risk |
|---|---|---|---|
| `&watermark` global | JOB_A | JOB_B | 🔴 breaks if split |

---

## A8. Custom formats & lookup catalogs (system-wide) 🔴⚙️

| Catalog / format | Location | # formats | Used by (jobs) | Business meaning | Export status (`CNTLOUT=`) | Python target |
|---|---|---|---|---|---|---|
| `fmtlib.formats` | ‹…› | ‹40› | ‹many› | region/product/age mappings | [ ] exported | lookup tables |

---

## A9. Scheduling & orchestration 👤⚙️

| Field | Value |
|---|---|
| Scheduler(s) | ‹Control-M / Autosys / cron / SAS MC / Airflow› |
| # scheduled jobs / flows | ‹n› |
| Calendar logic | ‹business days, holidays, month-end, fiscal› 🔴 |
| Inter-job triggers | ‹file-watch, predecessor, time› |
| Failure/retry policy | ‹…› |
| SLAs | ‹…› |
| Manual / ad-hoc runbooks | ‹…› |
| Orchestration migration target | ‹Airflow / Dagster / Step Functions / cron› |

---

## A10. Dead code, redundancy & retirement candidates ⚙️👤

| Item | Type | Evidence (no refs / never scheduled / superseded / commented-out) | Recommendation |
|---|---|---|---|
| `old_load_v1.sas` | program | not in scheduler, no callers | retire |
| duplicated dedup logic | pattern | appears in 5 jobs | consolidate into shared util |

> Don't port what you can delete. Quantify: ‹X% of programs appear unreferenced›. 👤 confirm with SME before dropping.

---

## A11. System-wide SAS→Python construct frequency 🔴⚙️

Tally usage of behavior-bearing constructs across the repo. High-frequency hard constructs are where you standardize a reusable mapping pattern first.

| Construct | Occurrences | # jobs | Migration difficulty | Standard Python pattern decided |
|---|---|---|---|---|
| `MERGE … BY` | ‹n› | ‹n› | med 🔴 | pandas merge w/ indicator |
| BY-group `FIRST./LAST.`+`RETAIN` | ‹n› | ‹n› | high 🔴 | groupby+cumulative/transform |
| `PROC SQL` pass-through | ‹n› | ‹n› | med 🔴 | native SQL via connector |
| `PROC SQL` remerge | ‹n› | ‹n› | high 🔴 | groupby.transform broadcast |
| `LAG()`/`DIF()` | ‹n› | ‹n› | med 🔴 | shift (mind conditional exec) |
| `PROC TRANSPOSE` | ‹n› | ‹n› | low | pivot/melt |
| `PROC MEANS/SUMMARY` | ‹n› | ‹n› | low | groupby.agg |
| Hash objects / `hiter` | ‹n› | ‹n› | high 🔴 | dict/merge |
| `DATA _NULL_` + `PUT`/`FILE` | ‹n› | ‹n› | med 🔴 | templated writer |
| Custom-format-as-lookup `PUT(x,$f.)` | ‹n› | ‹n› | low | dict map |
| Macro dynamic codegen | ‹n› | ‹n› | high 🔴 | parameterized functions |
| `PROC FCMP` UDFs | ‹n› | ‹n› | med | Python functions |
| Statistical PROCs (`REG`/`LOGISTIC`/…) | ‹n› | ‹n› | high 🔴 | statsmodels/scikit (parity risk) |
| Mainframe/EBCDIC/packed input | ‹n› | ‹n› | high 🔴 | custom decoder |

---

## A12. Target Python architecture (system) 👤

| Field | Value |
|---|---|
| Primary compute engine | ‹pandas / polars / DuckDB / PySpark / SQL warehouse pushdown› |
| Storage / data lake | ‹parquet on S3 / Snowflake / Postgres / Delta› |
| Orchestration | ‹Airflow / Dagster / Prefect› |
| Shared utils package | ‹name; houses ported macros, date logic, lookups, IO› |
| Config / secrets | ‹approach› |
| Testing framework | ‹pytest + golden-dataset harness› |
| CI/CD | ‹…› |
| Code-generation / transpile tooling considered? | ‹SAS2PY tools, custom parser, manual› 🔴 |

---

## A13. Migration program plan ⚙️👤

| Wave | Theme | Jobs (count) | Rationale | Dependencies cleared first | Target date | Owner |
|---|---|---|---|---|---|---|
| 0 | Foundations | — | shared utils, formats, libname→connector mapping, test harness | — | ‹…› | |
| 1 | Leaf / low-complexity | ‹n› | quick wins, prove the harness | wave 0 | | |
| 2 | Core ETL | ‹n› | high value, medium risk | wave 1 | | |
| 3 | Complex / statistical / macro-heavy | ‹n› | hardest, needs SME | waves 1–2 | | |
| N | Decommission SAS | — | parallel-run sign-off then cutover | all | | |

### A13.1 Parallel-run / cutover strategy 👤
> ‹How long do SAS and Python run side-by-side? Reconciliation gate to decommission each job. Rollback plan.›

---

## A14. Risks, assumptions & open questions 👤

| # | Risk / assumption / question | Impact | Owner | Status | Mitigation/resolution |
|---|---|---|---|---|---|
| 1 | ‹Mainframe EBCDIC feeds — decoding parity› | High | | open | |
| 2 | ‹Statistical PROC results must match to N decimals› | High | | open | |
| 3 | ‹Undocumented business rules in macro layer› | High | | open | |

---

## A15. Extraction tooling checklist ⚙️

What to run/script to populate this inventory programmatically (vs. by hand).

- [ ] Recursive file crawl → program inventory (path, LOC, mtime).
- [ ] Regex/parser pass for `LIBNAME`/`FILENAME` → library & interface registry.
- [ ] Parser for `%macro`/`%include`/autocall refs → macro dependency graph.
- [ ] Parser for `SET`/`MERGE`/`UPDATE`/`PROC SQL FROM`/`CREATE TABLE` → data lineage DAG.
- [ ] Construct-frequency scan (regex catalog of the constructs in §A11).
- [ ] `PROC CONTENTS` / `DICTIONARY.TABLES`+`.COLUMNS` dump → schema truth for all datasets.
- [ ] `PROC FORMAT CNTLOUT=` for every catalog → formats as data.
- [ ] Scheduler config export → job DAG.
- [ ] Run representative jobs with `OPTIONS MPRINT MLOGIC SYMBOLGEN;` → macro-resolved code.
- [ ] Cross-reference unreferenced programs → dead-code list.

---
---

# Part B — Per-Job Reverse-Engineering Capture

> Fill out **one copy of this part per SAS program/job**. It captures every detail needed to (a) understand what the job does and (b) forward-engineer it to Python with confidence. Aim for *behavioral* completeness — record not just what the code says, but what SAS actually *does* at runtime (defaults, implicit conversions, BY-group state, missing-value semantics).
>
> Replace `‹…›` placeholders. Delete sections that genuinely don't apply (but record *why*). Anything you can't resolve, log under **Open Questions** (§B18).

## B0. Document control

| Field | Value |
|---|---|
| Template version | 1.0 |
| Job analyzed by | ‹name› |
| Date analyzed | ‹YYYY-MM-DD› |
| Reviewed by (SME) | ‹name› |
| Review date | ‹YYYY-MM-DD› |
| Analysis status | [ ] Draft  [ ] Peer-reviewed  [ ] SME-signed-off |
| Confidence in understanding (1–5) | ‹n› |

---

## B1. Job identity & metadata ⚙️

| Field | Value |
|---|---|
| Program file name | ‹etl_load_claims.sas› |
| Full repo path | ‹/sas/prod/etl/etl_load_claims.sas› |
| Logical job name (in scheduler) | ‹JOB_CLAIMS_DAILY› |
| Business domain / subject area | ‹Claims, Finance, Risk, …› |
| One-line purpose | ‹…› |
| Owner / team | ‹…› |
| Original author | ‹…› |
| Created date | ‹…› |
| Last modified date / by | ‹…› |
| Lines of code (total / executable) | ‹n / n› |
| Version control? | [ ] Yes (commit/path) [ ] No — only on server |
| Criticality | [ ] Critical [ ] High [ ] Medium [ ] Low |
| Regulatory / audit relevance | ‹e.g. SOX, HIPAA, financial reporting› |
| Frequency of execution | ‹daily / weekly / monthly / ad-hoc / event-driven› |

**Plain-language summary (👤, 3–6 sentences):**
> ‹What does this job accomplish in business terms? What would break if it stopped running?›

---

## B2. Invocation & execution context ⚙️👤

| Field | Value |
|---|---|
| How is it launched? | [ ] Batch (`sas program.sas`) [ ] Scheduler [ ] Stored process [ ] Interactive [ ] Called by another program (`%include`/`%macro`) [ ] EG/Studio flow |
| Scheduler / orchestrator | ‹cron / Autosys / Control-M / LSF / Airflow / SAS Mgmt Console / Tivoli› |
| Schedule / cron expression | ‹0 5 * * 1-5› |
| Trigger type | [ ] Time [ ] File-arrival [ ] Predecessor job [ ] Manual |
| Upstream predecessor jobs | ‹…› |
| Downstream successor jobs | ‹…› |
| Typical run time / SLA | ‹e.g. 12 min, must finish by 06:00› |
| Concurrency | [ ] Single instance [ ] Parallel partitions (by region/date/…) |
| Runs as (service account) | ‹…› |
| Config file(s) / autoexec used | ‹autoexec.sas, env.cfg› |

### B2.1 Parameters passed in 🔴⚙️
SAS jobs are commonly parameterized via `-sysparm`, env vars, `%let` overrides, or a control table.

| Parameter | Source (`&SYSPARM`, env var, control table, hard-coded) | Default | Example value | How consumed in code |
|---|---|---|---|---|
| ‹as_of_date› | `-sysparm` | none | `20260601` | `%let dt=%scan(&sysparm,1,%str(,));` |
| ‹region› | env var `REGION` | `ALL` | `EMEA` | … |

> 🔴 **Watch:** date parameters are often strings parsed with `%sysfunc(inputn(...))`. Capture the exact format and timezone assumption.

---

## B3. Environment, system options & global state 🔴⚙️

Record options that change *results* (not just logging). These often differ from Python defaults.

| Option / setting | Value in job | Effect | Python equivalent / note |
|---|---|---|---|
| `OPTIONS` statements present | ‹list verbatim› | | |
| Encoding / `ENCODING=` | ‹wlatin1, utf-8› | char width, byte vs. char | pandas reads as utf-8 by default — 🔴 mismatch risk |
| `LOCALE` / language | ‹EN_US› | sorting, date words | |
| `MISSING=` char | ‹.› | how missing displays/sorts | |
| `YEARCUTOFF=` | ‹1920› | 2-digit year window | 🔴 affects date parsing |
| `DATESTYLE` / `DATE` format defaults | ‹MMDDYY / DDMMYY› | ambiguous date parse | 🔴 |
| `FIRSTOBS=` / `OBS=` | ‹1 / MAX› | row subsetting (esp. if test-limited!) | 🔴 hidden row caps |
| `COMPRESS=` | ‹YES/CHAR/BINARY/NO› | storage only | n/a |
| `SORTSEQ` / collation | ‹LINGUISTIC, ASCII, EBCDIC› | BY-group order, joins | 🔴 changes ties & joins |
| `NOREPLACE` / `REPLACE` | | overwrite behavior | |
| `MERGENOBY` | ‹NOWARN/ERROR› | silent bad merges | 🔴 |
| `VALIDVARNAME` | ‹V7 / ANY / UPCASE› | column naming | affects name mapping |
| `DKRICOND` / `DKROCOND` | | drop/keep/rename error level | |
| `BUFSIZE` / `BUFNO` / memory opts | | performance only | n/a |
| Numeric precision / `ROUND` usage | | | 🔴 SAS floats are 8-byte; see §B13 |

**Macro-controlled options** (options set conditionally via macro logic): ‹…›

---

## B4. Library & file references (LIBNAME / FILENAME) 🔴⚙️

### B4.1 LIBNAMEs

| Libref | Engine | Path / connection | Access | Options (`SCHEMA=`, `PRESERVE_TAB_NAMES`, `READBUFF`, etc.) | Resolves to (env-specific?) | Python target |
|---|---|---|---|---|---|---|
| `claims` | BASE | `/data/prod/claims` | RW | `COMPRESS=YES` | prod vs. dev path differs 🔴 | parquet folder / DB schema |
| `db` | ORACLE/ODBC/Teradata/Postgres | `user=… path=…` | R | `SCHEMA=DWH READ_ISOLATION_LEVEL=…` | credential source? 🔴 | SQLAlchemy / connector |
| `fmtlib` | BASE (format catalog) | `/sas/fmts` | R | `FMTSEARCH` member | see §B9 | mapping dicts |

> Record **every** way a path can vary by environment (prod/uat/dev). Hard-coded paths are migration landmines. 🔴

### B4.2 FILENAMEs (external files, pipes, emails, URLs)

| Fileref | Type (disk/pipe/FTP/URL/email/temp) | Path / command | Direction | Encoding / LRECL / RECFM | Notes |
|---|---|---|---|---|---|
| `inraw` | disk | `/feeds/claims_&dt..dat` | input | `LRECL=2000 RECFM=V` | fixed-width? delimited? |
| `cmd` | pipe | `'gzip -dc file.gz'` | input | | 🔴 shell dependency |
| `mail` | email | EMAIL engine | output | | becomes SMTP in Python |

---

## B5. Macro layer 🔴👤⚙️

The macro layer is usually the hardest part to port because it generates code at runtime.

### B5.1 Macro variables

| Macro var | Defined by (`%let`, `CALL SYMPUT(X)`, `INTO:`, `%global/%local`, autoexec) | Scope | Purpose | Value derivation | 🔴 dynamic? |
|---|---|---|---|---|---|
| `&dt` | `%let` from sysparm | global | as-of date | parsed string | |
| `&maxid` | `select max(id) into :maxid` | local | watermark | from DB | yes |
| `&n` | `call symputx('n',_n_)` | global | row count | from data step | yes |

> 🔴 Distinguish `CALL SYMPUT` (resolves *after* the data step) from `CALL SYMPUTX` and `INTO:`. Timing bugs here are common and must be replicated faithfully.

### B5.2 Macro definitions (`%macro … %mend`) and includes

| Macro / include | Source (inline / autocall lib / `%include` path) | Parameters (positional/keyword + defaults) | What it does | Generates SAS code dynamically? | Recursion / nesting | Notes |
|---|---|---|---|---|---|---|
| `%load_table` | autocall `/sas/macros` | `lib=, tbl=, where=` | generic loader | yes (builds PROC SQL) 🔴 | calls `%clean` | |
| `%do_region` | inline | `region` | loops regions | `%do %until` | | |

### B5.3 Macro control flow 🔴
Document each construct — these become Python loops/conditionals, but execution timing differs.

- `%IF / %THEN / %ELSE` conditions: ‹…›
- `%DO` loops (iterative / `%do %while` / `%do %until` / list): ‹…›
- Code-generating loops (emit N copies of a step): ‹…›
- `%GOTO` / labels: ‹…›
- Macro quoting functions used (`%STR`, `%NRSTR`, `%BQUOTE`, `%SUPERQ`, `%UNQUOTE`): ‹…› 🔴 (quoting edge cases)
- `&SYSxxx` automatic vars referenced (`&SYSERR`, `&SYSRC`, `&SQLOBS`, `&SYSDATE`, `&SYSCC`, `&SYSLAST`): ‹…›
- `%SYSFUNC` / `%SYSCALL` invocations of DATA-step functions in macro context: ‹…›

### B5.4 Macro → generated SAS (👤)
> Paste a representative *resolved* version of any macro-generated code (run with `OPTIONS MPRINT;`). Migrating from resolved code is far safer than reading the macro source.
```sas
‹MPRINT output sample›
```

---

## B6. Inputs — data sources consumed 🔴⚙️

One row per distinct input dataset / table / file. Be exhaustive — include lookup tables, format catalogs, control tables.

| # | Source name | Type (SAS dataset / DB table / view / flat file / format cat) | Libref/fileref | Location resolved | Read mode (full / WHERE-filtered / indexed / sample) | Filter / WHERE / `(obs=)` | Approx rows | Approx cols | Refresh cadence | Owner of source | Python source |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `claims.raw` | SAS dataset | `claims` | `/data/prod/claims/raw.sas7bdat` | WHERE | `dt=&dt` | 5M | 40 | daily | upstream JOB_X | parquet |
| 2 | `db.dim_member` | DB view | `db` | Oracle DWH.DIM_MEMBER | full | — | 2M | 25 | daily | DWH team | SQL |

### B6.1 Input schema detail (per key source) 🔴⚙️
For each *important* input, capture the column contract. SAS stores no separation between int/float (all numeric = 8-byte float) and char length is fixed — record both.

**Source: `‹claims.raw›`**

| Column | SAS type (num/char) | Length | Format | Informat | Label | Nullable / missing rep | Business meaning | Python dtype | 🔴 notes |
|---|---|---|---|---|---|---|---|---|---|
| `clm_id` | num | 8 | — | — | Claim ID | never | identifier | int64 | 🔴 stored as float → precision >2^53 |
| `clm_dt` | num | 8 | `DATE9.` | — | Claim date | `.` | event date | datetime64 | 🔴 SAS epoch 1960-01-01 |
| `amt` | num | 8 | `DOLLAR12.2` | — | Amount | `.A/.B` special missing | currency | float64 | 🔴 special missings |
| `member` | char | `$20` | — | — | Member code | `' '` blank | key | string | trailing-blank semantics |

> Capture **special missing values** (`.A`–`.Z`, `._`) explicitly — they carry meaning and have no native pandas analogue. 🔴

---

## B7. Processing logic — step-by-step 🔴👤⚙️

Walk the program in execution order. Create one **Step** block per DATA step or PROC. This is the heart of the document.

### Step template (copy per step)

```
### Step ‹N›: ‹DATA work.x / PROC SQL / PROC SORT …›  (lines ‹a–b›)
```

| Field | Value |
|---|---|
| Step type | ‹DATA / PROC SQL / PROC SORT / PROC MEANS / PROC TRANSPOSE / %macro call / …› |
| Inputs (datasets/tables) | ‹…› |
| Output(s) | ‹work.x (temp) / perm dataset / report / file› |
| Purpose (one line) | ‹…› |
| Row-count effect | ‹1:1 / filter / aggregate / explode / join fan-out› |
| Key columns added/changed/dropped | ‹…› |
| Deterministic? | [ ] Yes [ ] No (depends on input order / RNG / time) 🔴 |

**Logic narrative (👤):** ‹plain-language description of the transformation›

#### B7.x DATA step internals (fill when step is a DATA step) 🔴
Capture every behavior-bearing construct:

- **Input combination:** [ ] `SET` (stack/append) [ ] `SET` with `BY` (interleave) [ ] `MERGE … BY` (join) [ ] `UPDATE`/`MODIFY` [ ] `SET … POINT=` (random access) [ ] `SET … NOBS=` [ ] `SET … END=`
  - Merge type & match: ‹inner-ish? one-to-many? `IN=` flags used?› 🔴 *SAS MERGE is not SQL join — no Cartesian on dup keys; last-wins on overlap.*
  - `IN=` variable logic: ‹`if a and b` / `if a` …›
- **BY-group processing:** BY vars = ‹…›; uses `FIRST.var` / `LAST.var`? ‹…› 🔴 (group-boundary logic → pandas `groupby` + transform)
- **RETAIN:** variables ‹…›; initial values ‹…› 🔴 (carries state across rows — order-dependent)
- **Accumulation:** sum statement (`x+1`), `RETAIN`+manual, `LAG()`/`DIF()` 🔴 (LAG is a queue, not "previous row" — depends on conditional execution!)
- **Arrays:** `ARRAY` defs ‹…›; `DO OVER` / `DO i=1 TO dim()`; temporary arrays; `_TEMPORARY_` 🔴
- **Iteration:** `DO` / `DO WHILE` / `DO UNTIL` / iterative `DO`; explicit `OUTPUT`; `DELETE`; `STOP`; `LINK`/`RETURN`
- **Conditionals:** `IF/THEN/ELSE`, `SELECT/WHEN/OTHERWISE`, subsetting `IF`, `WHERE` (compile-time vs. exec-time difference 🔴)
- **Column ops:** `KEEP` `DROP` `RENAME` `LENGTH` `FORMAT` `INFORMAT` `LABEL` `ATTRIB` — note data-step vs. dataset-option placement 🔴
- **Type handling:** implicit char↔num conversion (note any — SAS auto-converts and logs a NOTE) 🔴; `INPUT()`/`PUT()` explicit conversions; `BEST.`/`Z.`/`COMMA.` formats
- **Functions used (categorize):**
  - String: `SUBSTR` (note: `SUBSTR` on LHS = replace!), `SCAN`, `INDEX`, `TRANWRD`, `COMPRESS`, `CATX`, `STRIP`, `PROPCASE`, `PRX*` (regex) 🔴
  - Date/time: `INTNX`, `INTCK`, `DATEPART`, `TIMEPART`, `MDY`, `TODAY`, `DATDIF`/`YRDIF` (basis arg! 🔴), `INTNX` alignment arg
  - Numeric: `ROUND` (banker's? no — SAS rounds half-up 🔴), `INT`, `MOD`, `SUM` (ignores missing 🔴), `MEAN` (ignores missing)
  - Lookup: `hash` objects / `hiter`, format-as-lookup (`PUT(x,$fmt.)`) 🔴, arrays-as-lookup
  - Special: `_N_`, `_ERROR_`, `_INFILE_`, `END=`, `NOBS=`
- **Missing-value semantics:** how are `.`/blank treated in arithmetic & comparison? (SAS: missing `<` any number 🔴) Special missings? Propagation rules?
- **I/O within step:** `INFILE`/`INPUT` (column/list/formatted/named input; `@`/`@@`/`/` line holders; `MISSOVER`/`TRUNCOVER`/`DSD` 🔴), `FILE`/`PUT` (report writing), `DATA _NULL_` side effects (macro vars, files, emails) 🔴
- **Hash objects / DOW loop / other advanced:** ‹…› 🔴

#### B7.y PROC step internals (fill per PROC)
Common ones (add the proc-specific knobs that change results):

- **PROC SQL:** joins (type, keys, fan-out), subqueries, `HAVING`, `GROUP BY` + remerge (🔴 SAS remerges aggregates back to detail — non-ANSI!), `CASE`, `COALESCE`, `CALCULATED`, integer vs. float division, `SELECT INTO :macrovar` (single & list with `SEPARATED BY`), `CREATE TABLE`/`VIEW`/`INDEX`, dictionary tables (`DICTIONARY.COLUMNS`), explicit/implicit **pass-through** (🔴 runs on DB — capture native SQL & DB dialect), `NOPROMPT`, `_METHOD`/`_TREE` if tuned.
- **PROC SORT:** `BY` (asc/desc), `NODUPKEY` vs `NODUP` (🔴 different!), `DUPOUT=`, `EQUALS`/`NOEQUALS` (stability 🔴), tagsort, `OUT=`.
- **PROC MEANS / SUMMARY:** `CLASS` vs `BY`, `VAR`, `WAYS`/`TYPES`, stats requested (`N NMISS MEAN SUM MIN MAX STD P25 …`), `OUTPUT OUT= … / autoname`, `_TYPE_`/`_FREQ_` columns 🔴, `MISSING` option (include missing as class level 🔴), `NWAY`.
- **PROC FREQ:** `TABLES a*b / list out=`, `WEIGHT`, missing handling, `OUT=`.
- **PROC TRANSPOSE:** `BY`, `ID`, `VAR`, `PREFIX`, `IDLABEL`, `_NAME_`/`_LABEL_` cols 🔴 (long↔wide → pandas pivot/melt).
- **PROC FORMAT:** see §B9 (custom formats — often act as lookup tables 🔴).
- **PROC APPEND / DATASETS:** `FORCE` (🔴 schema-mismatch coercion), modify/index/rename ops.
- **PROC REPORT / TABULATE / PRINT / ODS:** reporting output (define columns, compute blocks, breaks) → maps to formatting/Excel/PDF in Python.
- **PROC FCMP:** user-defined functions 🔴 (port as Python funcs).
- **Statistical PROCs** (`REG`, `LOGISTIC`, `GLM`, `MEANS` stats, `RANK`, `STDIZE`, `EXPAND`, `TIMESERIES`): record method, options, and output stats needed (🔴 algorithmic parity is hard — statsmodels/scikit/scipy choices matter).

> Repeat Step blocks until every line of executable code is accounted for. Cross-check: total steps should reconcile with the program's PROC/DATA count.

---

## B8. Outputs — what the job produces 🔴⚙️

| # | Output name | Type (SAS dataset / DB table / flat file / report / email / format cat) | Destination resolved | Write mode (create / replace / append / update / upsert) | Schema (cols/types) | Consumers (downstream jobs/reports) | Retention | Python target |
|---|---|---|---|---|---|---|---|---|
| 1 | `claims.fact_daily` | SAS dataset | `/data/prod/claims/` | replace | see schema | JOB_RPT, BI | 7 yrs | parquet/DB |
| 2 | extract file | CSV | `/out/claims_&dt..csv` | create | … | external partner | 30 d | to_csv |

**Output schema contract (per key output)** — same columns as §B6.1. The Python rewrite must reproduce these *exactly* (names, types, lengths, formats, ordering if downstream depends on it 🔴).

---

## B9. Formats, informats & lookup catalogs 🔴⚙️

Custom formats (`PROC FORMAT`) frequently encode business mappings and act as join-free lookups. Capture each.

| Format/informat name | Type (char `$`/num/picture/multilabel) | Defined where (this job / catalog `fmtlib`) | Domain → mapping | `OTHER` handling | Used as (display / lookup / binning) | Python equivalent |
|---|---|---|---|---|---|---|
| `$region.` | char | `fmtlib` | `'01'='EMEA' …` | `'UNKNOWN'` | lookup via `PUT(code,$region.)` | dict / map |
| `agegrp.` | num picture/range | this job | `low-17='<18' 18-64='18-64' …` | | binning | `pd.cut` 🔴 |
| `mlf.` | multilabel | catalog | overlapping ranges | | rollups | 🔴 hard |

> 🔴 If formats live in a catalog (`.sas7bcat`), record the **catalog location & FMTSEARCH order** — missing formats cause silent fallbacks or errors. Export with `PROC FORMAT CNTLOUT=` to get them as data.

---

## B10. Control flow, error handling & conditional execution 🔴👤

| Aspect | Detail |
|---|---|
| Step-to-step gating | ‹runs Step 4 only if `&rows>0`?› |
| Return-code checks | ‹`%if &syserr ne 0 %then %abort;`, `&sqlrc`, `&syscc`› 🔴 |
| `ABORT` / `STOP` / `%ABORT` usage | ‹cancel vs abend; return codes passed to scheduler› |
| Restart / re-run safety | [ ] Idempotent [ ] Not (appends, watermarks) 🔴 |
| Locking / contention | ‹dataset locks, DB transactions› |
| Logging / audit writes | ‹writes to log/control table?› |
| Conditional branches | ‹holiday calendar, month-end logic, region loops› |
| Side effects | ‹emails, file moves, `X`/`SYSTEM` shell calls 🔴, FTP› |

---

## B11. Data quality, business rules & edge cases 🔴👤

The rules that *aren't obvious from syntax* but matter for parity.

- **Explicit validations:** ‹range checks, referential checks, reject logic, error datasets›
- **Implicit business rules embedded in code:** ‹e.g. "negative amounts zeroed", "cap at 9999", "if missing region default EMEA"›
- **Dedup logic & tie-breaking:** ‹which record wins on dup key? sort-order dependent? 🔴›
- **Date/period logic:** ‹month-end, fiscal calendar, holiday handling, as-of vs. effective dates›
- **Rounding / precision rules:** ‹where/how amounts are rounded — and at which step 🔴›
- **Known edge cases / quirks:** ‹leap years, DST, negative durations, empty input behavior›
- **"Magic numbers" / hard-coded values:** ‹thresholds, codes, cutoffs — list each with meaning›
- **Suspected bugs / undocumented behavior the rewrite must preserve (or fix — flag for decision):** ‹…› 👤

---

## B12. Dependencies & lineage ⚙️

| Direction | Object | Type | Notes |
|---|---|---|---|
| Upstream data | ‹claims.raw› | dataset | produced by ‹JOB_X› |
| Upstream code | ‹%load_table› | autocall macro | shared — changes affect many jobs 🔴 |
| Upstream config | ‹autoexec, fmt catalog, control table› | | |
| Downstream data | ‹claims.fact_daily› | dataset | consumed by ‹JOB_RPT, BI tool› |
| Shared with other jobs | ‹work datasets? global macro vars?› | | 🔴 hidden coupling |

> Note any **global macro variables or work datasets** this job reads that another job created in the same session/flow — these are invisible coupling that breaks when jobs are split. 🔴

---

## B13. SAS → Python behavioral gotchas checklist 🔴

Tick every one that applies to this job; each is a place Python will differ unless handled.

- [ ] **Numeric = 8-byte float only** — integer IDs > 2^53 lose precision; use `int64`/`string` in Python.
- [ ] **Dates are numbers from 1960-01-01**; datetimes are seconds. Convert with epoch offset.
- [ ] **Missing `.` sorts below all numbers** and is ignored by `SUM`/`MEAN` but propagates in `+`. (pandas `NaN` differs.)
- [ ] **Special missing values** `.A`–`.Z`, `._` carry meaning.
- [ ] **Character variables are fixed-length, blank-padded**; comparisons ignore trailing blanks; truncation is silent on assignment if target `LENGTH` is short.
- [ ] **Default variable length 8** for numerics; first-seen length for char (assignment order matters 🔴).
- [ ] **Automatic type conversion** char↔num with a log NOTE (not an error).
- [ ] **PROC SQL remerge** — aggregate without GROUP BY remerges to detail rows (non-ANSI).
- [ ] **Integer-looking division** & `ROUND` half-up (not banker's rounding).
- [ ] **MERGE semantics** ≠ SQL join (no fan-out on dup BY keys; last-value-wins on same-named cols).
- [ ] **BY-group `FIRST./LAST.` + RETAIN** — order-dependent state machine.
- [ ] **`LAG()`/`DIF()`** are queues affected by conditional execution, not "previous row".
- [ ] **`WHERE` vs subsetting `IF`** — WHERE filters before the step sees the row (and can't see computed vars).
- [ ] **`PUT(x, fmt.)` used as lookup** — depends on format catalog availability.
- [ ] **Format/informat-driven parsing** (`YEARCUTOFF`, locale, `MMDDYY` vs `DDMMYY`).
- [ ] **Sort collation** (`SORTSEQ`/linguistic vs ASCII) affects ties, joins, and "first" records.
- [ ] **Implicit `OUTPUT`** at end of DATA step vs explicit `OUTPUT` (multiple rows out).
- [ ] **`OBS=`/`FIRSTOBS=` row caps** possibly left in from testing.
- [ ] **Pass-through SQL** runs on the DB engine with its own dialect/precision.
- [ ] **Encoding mismatch** (wlatin1 ↔ utf-8) for non-ASCII text.
- [ ] **Other:** ‹…›

---

## B14. Performance & scale ⚙️👤

| Field | Value |
|---|---|
| Input data volume (rows / GB) | ‹…› |
| Typical wall-clock runtime | ‹…› |
| Memory footprint | ‹…› |
| Indexes / keys relied on | ‹…› |
| Parallelism in SAS | ‹MP CONNECT / threaded PROCs / partitions› |
| Bottleneck step(s) | ‹…› |
| Python engine recommendation | [ ] pandas [ ] polars [ ] DuckDB [ ] PySpark [ ] SQL pushdown — ‹why› |

---

## B15. Validation & test plan 🔴⚙️

How we'll prove the Python version matches SAS.

| Field | Value |
|---|---|
| Golden input snapshot location | ‹frozen copy of inputs for date ‹dt›› |
| Golden output snapshot location | ‹SAS output for same inputs› |
| Reconciliation grain | ‹row-level diff / key-level / aggregate totals› |
| Tolerance | ‹exact / ±0.01 on amounts / float epsilon 🔴› |
| Row-count checks | ‹input→output expected counts› |
| Column checksums / control totals | ‹sum(amt), distinct(key), hash of sorted output› |
| Edge-case test inputs | ‹empty file, all-missing, dup keys, leap-year, special missings› |
| Sign-off owner | ‹…› |

```text
Reconciliation method (describe): ‹e.g. join SAS vs Python output on clm_id, assert all cols equal within tolerance, list mismatches›
```

---

## B16. Forward-engineering notes (Python target) 👤

| Field | Value |
|---|---|
| Recommended Python structure | ‹module/function layout; one func per step? class? config-driven?› |
| Library mapping summary | ‹SET→read, MERGE→merge, PROC SQL→SQL/duckdb, PROC MEANS→groupby.agg, TRANSPOSE→pivot/melt, FORMAT→dict› |
| Constructs with no clean equivalent | ‹DOW loop, hash with hiter, multilabel format, remerge — and chosen workaround› 🔴 |
| Parameterization plan | ‹CLI args / config / env — replacing sysparm & macro vars› |
| Reusable shared code | ‹which autocall macros become shared Python utils› |
| Orchestration target | ‹Airflow/Dagster/cron — replacing scheduler dependency› |
| Decisions needed from SME | ‹list› 👤 |

---

## B17. Migration assessment ⚙️👤

| Dimension | Score / value | Notes |
|---|---|---|
| LOC | ‹n› | |
| # DATA steps / # PROCs | ‹n / n› | |
| Macro depth / dynamic codegen | ‹none / shallow / deep› 🔴 | |
| Rare/hard constructs present | ‹list from §B13› | |
| External/DB dependencies | ‹n› | |
| Statistical/algorithmic content | ‹none / some / heavy› 🔴 | |
| **Complexity** | [ ] Low [ ] Medium [ ] High [ ] Very high | |
| **Migration risk** | [ ] Low [ ] Medium [ ] High | |
| **Estimated effort** | ‹person-days› | |
| **Migration wave / priority** | ‹wave 1 (leaf, simple) … wave N› | |
| Recommended approach | [ ] Rewrite [ ] Auto-transpile + fix [ ] Retire (dead) [ ] Re-platform to SQL | |

---

## B18. Open questions & assumptions 👤

| # | Question / assumption | Owner | Status | Resolution |
|---|---|---|---|---|
| 1 | ‹Is `OBS=100` in §B3 a leftover test cap?› | ‹SME› | open | |
| 2 | ‹Does downstream depend on output column order?› | | | |

---

## B19. Raw artifacts attached / linked

- [ ] Original `.sas` source (path/commit)
- [ ] `MPRINT`/`MLOGIC`/`SYMBOLGEN` log sample (macro-resolved code) 🔴 *most valuable artifact*
- [ ] `PROC CONTENTS` of each input/output (schema truth)
- [ ] `PROC FORMAT CNTLOUT=` export of custom formats
- [ ] Sample input & output data (golden snapshot)
- [ ] Scheduler job definition

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
