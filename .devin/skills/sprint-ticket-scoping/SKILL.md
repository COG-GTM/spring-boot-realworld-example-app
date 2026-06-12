---
name: sprint-ticket-scoping
description: "Converted from Devin playbook: Sprint/Ticket Scoping"
triggers:
  - user
  - model
---

# Sprint/Ticket Scoping

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Sprint/Ticket Scoping (playbook-7b3308509ee444ceac2788f850e5371d), macro `!ticket_scoping`

## Procedure

# Playbook: Project Planning and Task Scoping

## Overview

This playbook provides a structured process for planning a new software project by breaking it into well-scoped tasks and subtasks. The goal is to produce a set of actionable work items that can be completed in individual sessions. Each task should represent a meaningful unit of work: not so large that it is vague or unmanageable, and not so small that it results in trivial one-line changes.

By following this playbook, project requirements are translated into actionable tasks and subtasks with clear outcomes, setting the project up for efficient execution.

Output: A written breakdown of all Tasks returned to the user.  Don't create the tickets, Don't edit code, just plan and scope and detail the plan to the user.

---

## What’s Needed From User

* **Project Repository Access**: A link or path to the code repository for the new project.
* **Project Context/Requirements**: Documentation, README, or specifications outlining the project’s goals and features.
* **Constraints or Standards (Optional)**: Coding standards, definitions of done, test coverage requirements, or other guidelines.
* **Confirmation Step**: Approval from the user before creating any tasks or subtasks in Jira.

---

## Procedure

### 1. Understand Project Goals and Context

* Review documentation and repository to grasp high-level goals.
* Summarize the project’s mission and list out major feature areas or modules.

### 2. Identify Major Feature Areas

* Break the project into high-level areas or modules.
* Document each with a brief description.

### 3. Break Features into Actionable Tasks

* Phrase tasks as clear, action-oriented items.
* Each task should represent a functional milestone, not just a one-line change.
* Example:

  * Instead of “Update build.gradle to use Java 21,” scope as:

    > “Upgrade build and CI pipeline to Java 21 — update build.gradle, CI workflows, and run initial tests to confirm the environment compiles and runs correctly.”

### 4. Create Subtasks for Granular Work

* Use subtasks for smaller, related components of a task (e.g., separate code changes, validation steps).
* Subtasks are for grouping related small steps, but should not be created if they represent trivial edits in isolation.

### 5. Scope Validation

* **Avoid Over-Fragmentation**: Do not split work into multiple tickets if the changes can be done in one pass (e.g., updating config, CI pipeline, and running tests).
* **Merge Micro-Tasks**: If a task is under \~2 hours, combine it with related steps.
* **Target Range**: Each task should reflect \~4–16 hours of meaningful work.
* **Functional Depth**: Tickets should drive towards a working milestone (e.g., “Java 21 runtime working and passing tests”), not just mechanical file edits.

### 6. Define Success Criteria and Dependencies

* Include acceptance criteria (tests passing, UI behaviors, API responses).
* Note dependencies, but avoid unnecessary artificial sequencing when related steps can be combined.

### 7. Review and Organize Tasks

* Ensure completeness and no overlaps.
* Group related tasks for clarity.
* Present plan to user for review.

### 8. Confirm Before Creating in Jira

* Do **not** create tasks or subtasks in Jira until explicitly confirmed by the user.
* Provide the proposed backlog in a structured format (markdown, JSON, or similar).
* Once approved, tasks and subtasks can then be created in Jira.

---

## Specifications (Post-Conditions)

* Feature areas broken down into actionable, milestone-based tasks with subtasks where useful.
* Each task includes clear descriptions, definitions of done, and acceptance criteria.
* Tasks properly scoped for efficiency: no trivial one-liners, no oversized vague items.
* Backlog reviewed and approved by the user before Jira creation.

---

## Best Practices for Task Sizing

* **Merge related changes**: If multiple steps are naturally performed together (e.g., config updates + test run), scope them into one task.
* **Focus on working outcomes**: Frame tickets around achieving a functional state, not just editing files.
* **Right-size for sessions**: Tasks should generally represent 4–16 hours of work.
* **Subtasks for clarity, not fragmentation**: Only create subtasks if they aid clarity and reflect real work, not trivial edits.
* **Reduce bottlenecks**: Avoid artificial chains of dependent micro-tasks where one well-scoped task can deliver the milestone.

---

## Forbidden Actions

* Do **NOT** create tasks in Jira without user confirmation.
* Do **NOT** start coding during planning.
* Do **NOT** create trivial subtasks (e.g., single-line changes) as standalone items.
* Do **NOT** leave tasks overly broad or undefined.
* Do **NOT** duplicate existing tasks provided by the user.
* Do **NOT** expose sensitive information (e.g., secrets, API keys).

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
