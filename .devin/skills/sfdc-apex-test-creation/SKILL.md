---
name: sfdc-apex-test-creation
description: "Converted from Devin playbook: SFDC Apex Test Creation"
triggers:
  - user
  - model
---

# SFDC Apex Test Creation

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SFDC Apex Test Creation (playbook-68476b5d0d08493aa68a557e3ae55add), macro `!sfdc_apex_tests`

## Procedure

Title: SFDX create apex tests

Procedure:

Authenticate: echo "$SFDX_AUTH_URL" | sf org login sfdx-url --sfdx-url-stdin

Verify org: sf org display --verbose

Scaffold project: sf project generate --name ApexDemo --output-dir ~/ApexWork

Add files under force-app/main/default

Deploy: sf project deploy start --wait 10

Run tests: sf apex run test [args]

Open org: sf org open

UI verify:

verify that the triggers are performing as expected

Capture screenshots for report

Summarize results and code paths

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
