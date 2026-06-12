---
name: abu-dhabi-open-data-process
description: "Converted from Devin playbook: Abu Dhabi open data process"
triggers:
  - user
  - model
---

# Abu Dhabi open data process

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Abu Dhabi open data process (playbook-a38dd415a781412c8492dadaf88ec4d1), macro `!abudhabidata`

## Procedure

## Procedure

Ask the user to provide the URL from a dataset in the Abu Dhabi open data repositories here `https://data.abudhabi/opendata`, and only there. For example `https://data.abudhabi/opendata/dataset/detail?id=9182ecec-c7a1-4546-8c23-e5e16243dcc5`

Download the dataset, it will be usually in a .zip format in a separate folder named after the dataset.

Name the folder using the a pattern "analysis_" + "name of data set_" + some UUID

Within that folder, create a python virtual environment and install pandas

Open the dataset and loaded up into Pandas. 

Ask the user what sort of summary statistics they would like to be produced

Then create a simple web UI where the user and explore such statistics

## Advice & Pointers

Please use arabic language for the UI

## Forbidden actions

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
