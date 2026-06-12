---
name: run-functional-backend-tests-cog-gtm-mobile-banking-app
description: "Converted from Devin playbook: Run Functional Backend Tests — COG-GTM/mobile-banking-app"
triggers:
  - user
  - model
---

# Run Functional Backend Tests — COG-GTM/mobile-banking-app

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Run Functional Backend Tests — COG-GTM/mobile-banking-app (playbook-d21b85f9e5dd4f52bd42a0f0dd004aad)

## Procedure

# Run Functional Backend Tests for mobile-banking-app

## Objective
Execute the full functional backend test suite for the Flutter mobile banking app and report results.

## Steps

1. Clone the repo: `COG-GTM/mobile-banking-app`
2. Set up Flutter:
   ```bash
   export PATH="/home/ubuntu/flutter/bin:$PATH"
   ```
   If Flutter is not installed, install it:
   ```bash
   git clone https://github.com/flutter/flutter.git -b stable --depth 1 /home/ubuntu/flutter
   export PATH="/home/ubuntu/flutter/bin:$PATH"
   echo 'export PATH="/home/ubuntu/flutter/bin:$PATH"' >> ~/.bashrc
   ```
3. Install dependencies:
   ```bash
   cd /home/ubuntu/repos/mobile-banking-app
   flutter pub get
   ```
4. Run the full test suite:
   ```bash
   flutter test
   ```
5. Report test results to the user:
   - Total tests run
   - Number passed / failed
   - Any failure details with file and line numbers
   - Overall pass/fail status

## Expected Results
- 119 tests across 6 test files should all pass
- Test files:
  - `test/view_models/view_model_test.dart` — 10 tests (ViewModel state management)
  - `test/repo/repository_test.dart` — 36 tests (Repository theme color resolution)
  - `test/json/data_integrity_test.dart` — 27 tests (Data layer integrity)
  - `test/utils/styles_and_assets_test.dart` — 22 tests (Styles & asset constants)
  - `test/utils/size_config_test.dart` — 12 tests (SizeConfig & Layouts utilities)
  - `test/widgets/navigation_test.dart` — 12 tests (BottomNav routing)

## On Failure
If any tests fail, investigate the failures and report:
- Which test(s) failed
- The error message and stack trace
- Whether the failure is a test issue or app code issue
- Suggested fix

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
