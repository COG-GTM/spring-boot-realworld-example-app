---
name: migrate-a-c-module-to-rust-using-parallel-child-sessions
description: "Converted from Devin playbook: Migrate a C++ Module to Rust Using Parallel Child Sessions"
triggers:
  - user
  - model
---

# Migrate a C++ Module to Rust Using Parallel Child Sessions

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Migrate a C++ Module to Rust Using Parallel Child Sessions (playbook-b0e3b1356b984672a5e71e5879ac532c), macro `!cpp_to_rust_parallel`

## Procedure

# Migrate a C++ Module to Rust Using Parallel Child Sessions

## Overview
Port a C++ module (multiple sub-components plus a shared types foundation) to a Rust crate, parallelizing the work across child Devin sessions wherever the dependency graph allows. This session is the orchestrator: it does the shared foundation and pre-wires every module slot itself, then fans independent sub-components out to parallel child sessions, merging their per-component PRs into a single integration branch. The goal is a behavior-faithful (bit-for-bit where applicable) Rust port that compiles, passes parity tests, and is clippy/fmt clean.

## What's Needed From User
- Repo and starting branch (e.g. `COG-GTM/ecam-module`, branch `devin/ecam-module-init`).
- The C++ source layout: the shared types header, and each sub-component's header/source/test files.
- The target dependency relationships between sub-components (or confirmation that the orchestrator should derive them from the code).
- Any required public API shape or parity expectations (e.g. "match the C++ rounding exactly").

## Procedure
1. Read the C++ source to map every sub-component to its files and build the dependency graph (what depends on the shared types and on which other sub-components).
2. Group sub-components into phases: Phase 1 = shared types + leaf modules everything depends on; later phases = components that depend on earlier phases. Independent components in the same phase run in parallel.
3. In THIS session, scaffold the Rust crate (e.g. `rust/`, crate name matching the module) and implement Phase 1 yourself: shared types and the leaf modules. Add unit/parity tests for them.
4. Pre-wire every remaining sub-component: create an empty stub file per module (`rust/src/<component>.rs`) and declare each via `pub mod <component>;` in `lib.rs`, plus any `Cargo.toml` deps. This prevents parallel children from conflicting on shared files.
5. Run `cargo test`, `cargo clippy`, and `cargo fmt --check` to confirm Phase 1 is green, then open an umbrella PR from the integration branch `devin/rust-migration` into the starting branch.
6. For each independent Phase 2 component, launch a child session (via the managing-child-sessions skill) using the per-component brief in "Advice and Pointers". Launch all independent components in one batch.
7. As each child PR (base = `devin/rust-migration`) lands, review it for parity and single-file scope, then merge it into `devin/rust-migration`. Pull the merged branch locally so later phases see prior work.
8. Once a component's dependencies are merged, launch the next phase's child sessions (e.g. `frame_pipeline` after `color_correction`). Repeat steps 6–7 until all components are ported.
9. After all components are merged into `devin/rust-migration`, run the full `cargo test`/`clippy`/`fmt --check` on the integration branch and update the umbrella PR description with the final component status.
10. Deliver the umbrella PR (`devin/rust-migration` → starting branch) with all sub-component PRs linked.

## Specifications
- One Rust source file per sub-component under `rust/src/`, each declared in `lib.rs`; no two child sessions ever edit the same file.
- Each ported component reproduces the C++ behavior faithfully — match documented parity tests exactly (including rounding/clamping), using the Phase 1 types (`crate::types`, `crate::pixel_format`, `crate::frame`).
- Final integration branch passes `cargo test`, `cargo clippy` (no warnings), and `cargo fmt --check`.
- Validation: on `devin/rust-migration` with all component PRs merged, `cargo test && cargo clippy -- -D warnings && cargo fmt --check` all succeed, and Rust parity tests cover the same cases as the C++ tests.
- Deliverable: one umbrella PR into the starting branch, with each per-component PR linked from it.

## Advice and Pointers
- Per-component child session brief (template — fill in `<component>`):
  - Repo + clone URL; base off `devin/rust-migration` (NOT main); create branch `devin/rust-<component>`; open PR with base = `devin/rust-migration`.
  - Scope: edit ONLY `rust/src/<component>.rs` (already a declared stub). Do NOT modify `lib.rs`, `Cargo.toml`, or other module files — parallel sessions own those.
  - What to port: the C++ header + source + the parity test file; reproduce behavior exactly. Read Phase 1 modules (`rust/src/types.rs`, `pixel_format.rs`, `frame.rs`) first.
  - Required Rust API: spell out the public types/functions so the integration surface is stable.
  - Put impl AND tests (`#[cfg(test)] mod tests`) inside the single component file; run `cargo test`/`clippy`/`fmt` before opening the PR.
- Choose the integration branch name once (`devin/rust-migration`) and reuse it everywhere; all per-component PRs target it, not the starting branch.
- Prefer launching the maximum set of truly-independent components in parallel; only serialize across real dependency edges.
- Keep the umbrella PR description as a live status board of which components are merged.

## Forbidden Actions
- Do not let a child session edit `lib.rs`, `Cargo.toml`, or any file other than its assigned component — this causes merge conflicts across parallel work.
- Do not point per-component PRs at the starting branch or `main`; they must target `devin/rust-migration`.
- Do not launch a component's child session before its dependencies are merged into the integration branch.
- Do not "approximate" C++ behavior where a parity test specifies exact values — match the reference exactly.
- Do not squash all components into one giant session; use parallel child sessions per the dependency graph.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
