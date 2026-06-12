---
name: ghost-notes-for-gtm
description: "Converted from Devin playbook: Ghost Notes for GTM"
triggers:
  - user
  - model
---

# Ghost Notes for GTM

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Ghost Notes for GTM (playbook-b7a6d9bd71804190b0021f0ddab6de10), macro `!ghost_notes`

## Procedure

## Overview

Create a document where a ghost note can be sent from Cognition Executives, this will pull relevant emails, names, job titles, companies from the installed Exa MCP server

## Procedure

1. Scan parameters for context: Which Cognition executive this should be sent in the voice of, and what is the compelling event? For example, if Google Cloud Next is coming up, write ghost notes for the relevant leaders to receive emails from Jeff
2. Use Exa MCP to get emails: Use the Exa MCP server to look up the company name,  get the relevant job titles and Names, and get the emails for those folks
3. Create a document: Create a document so the emails can be easily sent by the relevant Cognition executives
4. Send this document back to the user 

## Advice & Pointers

Find useful job titles such as CIO, CTO, VP of Engineering, Head of AI, or other Engineering leaders who would be interested in the Cognition platform 

## Forbidden actions

Don't send emails yet

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
