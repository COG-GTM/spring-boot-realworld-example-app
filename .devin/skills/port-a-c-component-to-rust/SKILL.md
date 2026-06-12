---
name: port-a-c-component-to-rust
description: "Converted from Devin playbook: Port a C++ Component to Rust"
triggers:
  - user
  - model
---

# Port a C++ Component to Rust

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Port a C++ Component to Rust (playbook-4dc8e1de93e04c888f22e3b6d459581e), macro `!port_cpp_to_rust`

## Procedure

# Port a C++ Component to Rust

## Overview
Reimplement a C++ component (a class, module, or sub-system) in Rust with
**functional parity** to the original, verified against the C++ component's own
tests. Favors incremental, one-component-at-a-time migration: each session ports
a single bounded component, reuses already-ported modules it depends on, edits a
minimal set of files, and opens a focused PR.

## What's Needed From User
- **Repo** and the **C++ component** to port (the source/header pair and, ideally,
  the test file that defines its behavior).
- **Target Rust location**: which crate/module the port goes into, and whether the
  target file already exists (e.g. a pre-declared stub) or must be created.
- **Base branch** for the work and PR (e.g. a shared `…/rust-migration` branch vs.
  `main`), and any **scope limits** (files you may edit, dependencies allowed).
- Names of **already-ported dependencies** this component builds on, if any.

If any of these are unspecified, infer them from the repo (README/CONTRIBUTING,
existing Rust crate layout, prior migration PRs) and confirm assumptions before
writing code.

<phase name="Setup & Context" id="1">
## Phase 1 — Setup & Context

1. Clone the repo. Read the README / CONTRIBUTING and any migration docs to learn
   the migration strategy, target toolchain/edition, and conventions.
2. Check out the correct **base branch** (the user-specified one, often NOT
   `main`), pull the latest, and create a feature branch off it
   (e.g. `devin/rust-<component>`).
3. Read the C++ you are porting: the header (public API/contract) and the source
   (algorithms, edge cases, error handling). List every public symbol and
   behavior that must be reproduced.
4. Read the C++ **test file** for the component — this is the functional-parity
   contract. Enumerate its cases (including any end-to-end paths).
5. Read the Rust side you build on: the crate's shared types/error enums, the
   module the target file lives in, an **already-ported module** to copy its
   conventions, and the public API of any dependency you wrap.

<verification>
- On a feature branch created from the correct base branch.
- You can name every C++ public symbol/behavior to reproduce and every case the
  C++ test file covers.
- You know the exact Rust APIs (types, traits, errors) you must reuse, and the
  conventions an existing ported module follows.
</verification>
</phase>

<phase name="Implement in Rust" id="2">
## Phase 2 — Implement in Rust

Stay within the agreed scope — typically edit only the target module file(s).

1. Define the Rust API mirroring the C++ contract, expressed idiomatically
   (traits instead of abstract classes, `Result<T, E>` instead of error
   codes/exceptions, ownership/borrows instead of raw pointers, `enum`s for
   tagged unions). Keep the public surface aligned with the C++ one.
2. Reproduce behavior faithfully: same algorithms, numeric results, edge handling,
   and error conditions (map each C++ failure path to the crate's error type).
   Be careful with C++/Rust differences — integer overflow/wrapping, implicit
   conversions, signedness, and floating-point ordering.
3. Reuse existing crate types and already-ported modules rather than duplicating
   them. Add no new dependencies unless explicitly allowed.
4. Port the C++ tests into Rust (e.g. a `#[cfg(test)] mod tests` in the same file,
   or the crate's test layout), covering the same scenarios including end-to-end
   paths. Translate the cases — don't invent a weaker set.
5. Add a brief doc comment noting the file is ported from its C++ counterpart,
   matching existing modules' style.

<verification>
- Only the agreed files are modified (`git status` confirms scope; build/manifest
  and unrelated modules untouched unless explicitly required).
- No unapproved new dependencies.
- The Rust tests cover the same scenarios as the C++ reference tests.
</verification>
</phase>

<phase name="Verify, PR & CI" id="3">
## Phase 3 — Verify, PR & CI

Run the crate's checks (from the crate root):

1. `cargo test` — all tests pass (pre-existing + newly ported).
2. `cargo clippy --all-targets` — clean (treat warnings as failures).
3. `cargo fmt --check` — clean (run `cargo fmt` first if needed).
4. If a behavioral/performance-parity benchmark or harness exists, run it and
   confirm results match the C++ baseline within tolerance.
5. Commit, push the feature branch, and open a PR with the **correct base branch**
   (the migration branch, not `main`, when that's the convention).
6. Wait for CI to pass; investigate and fix any failures before reporting done.

<verification>
- `cargo test`, `cargo clippy --all-targets`, and `cargo fmt --check` all pass.
- Any parity benchmark matches the C++ baseline.
- PR is open against the correct base branch and CI is green.
</verification>
</phase>

## Specifications
- Functional parity with the C++ component and its reference tests.
- Minimal, scoped diff — only the files needed for this component.
- PR targets the agreed base branch.

## Advice and Pointers
- The C++ test file is the source of truth for behavior — port its cases first,
  then make them pass.
- Translate C++ idioms into idiomatic Rust (RAII→ownership, virtual→trait,
  exceptions/error codes→`Result`, `union`/variant→`enum`), but keep results
  bit-for-bit / numerically identical where the tests demand it.
- Copy conventions from an already-ported module (doc comments, error handling,
  test placement) so the port fits the codebase.
- Watch the classic C++→Rust traps: overflow semantics, implicit numeric
  conversions, signed/unsigned mixing, array/index bounds, and uninitialized
  memory assumptions that don't carry over.
- If the target file is a pre-declared stub (already in `lib.rs`/`mod.rs`), don't
  re-declare it.

## Forbidden Actions
- Do NOT base your work on or open the PR against `main` when a dedicated
  migration/base branch is specified.
- Do NOT edit files outside the agreed scope (e.g. `lib.rs`, `Cargo.toml`, other
  modules) unless explicitly required.
- Do NOT add new dependencies without approval.
- Do NOT weaken or skip the ported tests to make CI pass.
- Do NOT open the PR against an upstream repo when the work belongs on a fork.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
