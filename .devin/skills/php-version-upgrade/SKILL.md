---
name: php-version-upgrade
description: "Converted from Devin playbook: PHP Version Upgrade"
triggers:
  - user
  - model
---

# PHP Version Upgrade

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: PHP Version Upgrade (playbook-1745717396d04c59a5763e74253caac8)

## Procedure

## Overview

This playbook outlines a safe, step-by-step process to upgrade your PHP application across major versions while ensuring compatibility, stability, and performance. Follow the official [PHP migration guides](https://www.php.net/manual/en/migration80.php) during each upgrade phase.

## Upgrade Process

### 1. Audit Current Environment

* Confirm the current PHP version using:

  ```bash
  php -v
  ```
* Check for hardcoded PHP versions in:

  * `.php-version`, `composer.json`, Dockerfiles, CI/CD configurations.
* List all PHP extensions and installed modules:

  ```bash
  php -m
  ```

### 2. Inventory & Assess Dependencies

* Run:

  ```bash
  composer show
  ```
* Check each dependency's compatibility with the target PHP version.
* Review `composer.lock` and validate:

  * Deprecated packages
  * Known issues with newer PHP versions

### 3. Review Backward Incompatibilities

* Read PHP’s official migration guides for each version you are upgrading through:

  * [PHP 7.4 → 8.0](https://www.php.net/manual/en/migration80.incompatible.php)
  * [PHP 8.0 → 8.1](https://www.php.net/manual/en/migration81.incompatible.php)
  * [PHP 8.1 → 8.2](https://www.php.net/manual/en/migration82.incompatible.php)

### 4. Update PHP Version in Dev & CI Environments

* Install the new PHP version using a version manager or container:

  * `brew`, `apt`, `dnf`, `asdf`, `Docker`, etc.
* Update Dockerfiles, CI build runners, or GitHub Actions workflows.
* Example Docker base image:

  ```Dockerfile
  FROM php:8.2-apache
  ```

### 5. Update `composer.json`

* Update the platform PHP version constraint:

  ```json
  "config": {
    "platform": {
      "php": "8.2"
    }
  }
  ```

* Run:

  ```bash
  composer update --lock
  ```

### 6. Scan for Deprecated or Incompatible Code

* Use tools like:

  * [PHPCompatibility for PHP\_CodeSniffer](https://github.com/PHPCompatibility/PHPCompatibility)
  * [phpstan](https://phpstan.org/)
  * [Psalm](https://psalm.dev/)

### 7. Run Full Test Suite

* Execute your PHPUnit or integration tests:

  ```bash
  ./vendor/bin/phpunit
  ```
* Fix any failures, especially those related to:

  * Type hinting changes
  * Deprecated functions
  * Error handling differences

### 8. Lint & Static Analysis

* Run:

  ```bash
  php -l path/to/your/files
  phpstan analyse
  psalm
  ```

### 9. Validate in Staging

* Deploy to a staging environment running the new PHP version.
* Confirm all major workflows work:

  * Web pages render correctly
  * Background jobs run
  * Logs remain clean
  * Monitoring shows no regressions

### 10. Deploy to Production

* Monitor error logs, CPU, memory, and response times.
* Watch for subtle issues that escaped staging.

## Advice & Pointers

* Upgrade incrementally through each PHP major version (7.3 → 7.4 → 8.0 → 8.2).
* Use a container-based local dev environment for quick PHP version switching.
* Make use of strict type hints and attributes available in PHP 8+.
* Modernize syntax using tools like Rector ([https://github.com/rectorphp/rector](https://github.com/rectorphp/rector)).

## Forbidden Actions

🚫 Do not skip major versions without checking all migration notes.
🚫 Do not hardcode PHP version dependencies inconsistently across environments.
🚫 Do not ignore failing tests or deprecation warnings.
🚫 Do not mix old and new PHP features without validating runtime behavior.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
