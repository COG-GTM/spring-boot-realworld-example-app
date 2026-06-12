---
name: flask-version-upgrade
description: "Converted from Devin playbook: Flask Version Upgrade"
triggers:
  - user
  - model
---

# Flask Version Upgrade

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Flask Version Upgrade (playbook-7eef2fdb27134c9885d9f1a8afc6183a)

## Procedure

### Overview

This playbook describes how to upgrade a Flask application from any older version to a newer one (e.g., Flask 1.1.x → 2.x or 3.x). The goal is to ensure API compatibility, address deprecations, and validate runtime correctness and test coverage throughout the upgrade.

### Upgrade Procedure

#### 1. Audit Current Flask Version and Dependencies

* Identify current Flask version:

  ```sh
  pip show flask
  ```
* Search for hardcoded version references in:

  * `requirements.txt`
  * `Pipfile` / `pyproject.toml`
  * `Dockerfile`
  * CI configs (e.g., GitHub Actions, GitLab CI)

#### 2. Review Flask Release Notes

* Go through release notes between your current and target versions:

  * [https://flask.palletsprojects.com/en/latest/changes/](https://flask.palletsprojects.com/en/latest/changes/)
* Note breaking changes, removals, and deprecations (e.g., `flask.ext.*`, `app.errorhandler` behavior).

#### 3. Check Third-Party Extension Compatibility

* Review Flask extensions (`flask_sqlalchemy`, `flask_login`, etc.) for support under the target Flask version.
* Check PyPI, GitHub issues, or documentation for:

  * Compatibility matrix
  * Required version upgrades
  * Deprecated APIs

#### 4. Update Version Constraints

* Update Flask and extensions in your dependency files:

  * `requirements.txt`

    ```
    flask>=3.0
    flask_sqlalchemy>=3.1
    ```
  * Or if using poetry:

    ```toml
    flask = "^3.0"
    ```

#### 5. Upgrade Locally

* Create a virtual environment:

  ```sh
  python3 -m venv venv
  source venv/bin/activate
  pip install -U pip
  pip install -r requirements.txt
  ```
* Reinstall any native extensions if needed.

#### 6. Run Application and Smoke Test

* Run the Flask dev server and validate startup:

  ```sh
  flask run
  ```
* Manually test routes for early regressions.

#### 7. Run Full Test Suite

* Execute your test suite:

  ```sh
  pytest  # or unittest or tox
  ```
* Fix issues caused by deprecated or changed behavior.

#### 8. Fix Breaking Changes

* Common upgrade fixes:

  * Replace `flask.ext.foo` with `flask_foo`
  * Switch from `app.env` to `app.debug` or `current_app.env`
  * Update Jinja2 filters/macros if changed

#### 9. Update CI/CD and Docker

* Ensure your CI pipeline uses the new Python and Flask version.
* Update Dockerfiles:

  ```Dockerfile
  FROM python:3.12
  RUN pip install flask==3.0
  ```

#### 10. Deploy to Staging and Monitor

* Deploy to a staging environment.
* Validate logs, performance, and API endpoints.
* Confirm expected behavior and no regressions.

#### 11. Deploy to Production

* Push changes to production after passing staging validation.
* Monitor metrics: response times, error logs, memory usage.

### Advice & Best Practices

* Run `pip check` to validate dependency conflicts.
* Use `ruff`, `pyupgrade`, and `black` to modernize syntax.
* Migrate to Python 3.11+ to take advantage of newer Flask capabilities.
* Always pin dependencies in `requirements.txt` to avoid surprise upgrades.

### Forbidden Actions

🚫 Do not commit `.pyc`, `.venv/`, or temporary debug code.
🚫 Never upgrade Flask without validating all third-party extensions.
🚫 Do not ignore deprecation warnings—future versions may remove support.
🚫 Avoid committing failing tests or skipping tests unless verified obsolete.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
