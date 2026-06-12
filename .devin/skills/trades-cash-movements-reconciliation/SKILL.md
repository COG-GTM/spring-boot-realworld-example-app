---
name: trades-cash-movements-reconciliation
description: "Converted from Devin playbook: Trades ↔ Cash Movements Reconciliation"
triggers:
  - user
  - model
---

# Trades ↔ Cash Movements Reconciliation

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Trades ↔ Cash Movements Reconciliation (playbook-e2cca823a3f24a7ca748b44f2252087c), macro `!reconcile_trades_cash`

## Procedure

Playbook: Trades ↔ Cash Movements Reconciliation

## Overview
Reconcile a batch of trades against a batch of bank/custodian cash movements: link every trade to its corresponding cash entry, flag partial/suspect matches, and surface anything unmatched. Output is a CSV/report package suitable for ops review. Designed around tribal-knowledge edge cases common in trade ops (counterparty name variants, netted settlements, fee-adjusted wires, non-trade cash noise).

## What's Needed From User
- **Trades file** (CSV). Required columns (or equivalents — ask if names differ): `trade_id`, `trade_date`, `settlement_date`, `security`, `quantity`, `price`, `direction` (BUY/SELL), `counterparty`, `gross_amount`, `expected_cash`, `currency`, `status`.
- **Cash movements file** (CSV). Required columns (or equivalents): `txn_id`, `value_date`, `amount`, `currency`, `counterparty_name`, `reference`, `description`, `account`.
- **Rule confirmations** (batch-ask up front — offer sensible defaults so user can say "defaults"):
  1. Matching-key priority order (default: direct `trade_id` in cash `reference` → `trade_id` in cash `description` → heuristic `counterparty + currency + amount + settle-window`).
  2. Fee tolerance for "net of fees" / "fees deducted" rows (default: ≤ $500 OR ≤ 0.5% of gross, auto-match only when the description mentions fee/net/netted, else flag).
  3. Netted-settlement handling (`NET-####` refs, "N trades") — auto-decompose up to N legs or flag for manual review (default: decompose up to 3 legs).
  4. Non-trade cash rows (INCOMING/OUTGOING PAYMENT, INTEREST PAYMENT, MISC SETTLEMENT, standalone FEE PAYMENT, bare WIRE TRANSFER, ACCOUNT TRANSFER) — exclude + report separately or include in match pool (default: exclude, list in report).
  5. Settlement window + sign convention (default: value_date in `[trade_date, settle_date + 5 days]`, BUY → negative cash / SELL → positive cash, sign mismatch flagged as warning).
  6. Output format (default: CSVs — `matched`, `flagged`, `unmatched_trades`, `unmatched_cash`, `non_trade_cash` — plus `summary.md`).
  7. Where the tool should live (default: deliver script + outputs as attachments; no repo commit unless requested).

## Procedure
1. Load both files into dataframes. Normalize date formats (trade side typically `YYYY-MM-DD`, cash side often `MM/DD/YYYY`) and confirm column mappings if names differ from expected.
2. **Profile the data** before writing any matching code. Report back: row counts, date ranges, currency mix, `status` value set, duplicate IDs, null counts, direction/sign convention, settlement-day distribution by asset class, and the set of unique counterparty strings on each side.
3. **Ask the user the batch of rule-confirmation questions** (see "What's Needed From User") in a single `message_user` call. Only block once; make it cheap to answer with "defaults".
4. **Build an explicit `CP_CANONICAL_MAP`** covering every counterparty variant found in *both* files. Don't rely on substring heuristics — hard-code every variant, with a single canonical name per legal entity. Emit a `[WARN]` line for any unseen variant at runtime so future batches don't silently collapse.
5. **Classify non-trade cash rows** using an exact-match (not substring) allow-list of bare descriptions (e.g., `"WIRE TRANSFER"` ≠ `"Wire transfer - Apple Inc"`). Set these aside and exclude from the match pool.
6. **Apply matching in strict priority order** (short-circuit as soon as a trade is matched):
   - Stage 1: cash `reference` matches `^TRD-\d{4}-\d{6}$` and maps to a known `trade_id` → direct match.
   - Stage 2: cash `description` contains a `TRD-YYYY-######` that maps to a known `trade_id` → direct match.
   - Stage 3: heuristic on unmatched pairs — same canonical counterparty + same currency + amount within fee tolerance + `value_date` inside window. Exact amount → `heuristic_exact`. Within fee tolerance AND description mentions fee/net → `heuristic_fee`. Within fee tolerance but no fee wording → **flag, don't auto-match**. Sign mismatch → flag, don't auto-match.
   - Stage 4: netted settlements (`NET-####` refs) — find subset of same-CP + same-currency unmatched trades (in window) whose `expected_cash` sums to the cash `amount` within fee tolerance, up to the agreed max legs. If ambiguous or no subset found → flag.
7. **Compute deltas** on every match: `amount_delta = cash.amount - trade.expected_cash`, `days_late = value_date - settlement_date`. Preserve in `matched.csv`.
8. **Write output artifacts** to a single output directory: `matched.csv`, `flagged.csv` (reason + detail), `unmatched_trades.csv`, `unmatched_cash.csv`, `non_trade_cash.csv`, and `summary.md` (stats, breakdown, parameters used, unmatched characterization by counterparty/asset class/description pattern).
9. **Validate results** before delivering: verify match-count + unmatched-count sums equal input row counts on each side; spot-check a handful of `direct_ref` rows (should be 0-delta); re-run and confirm output determinism.
10. **Deliver** the script + all CSVs + `summary.md` as attachments. Lead the final message with the match rate, matched gross-value percentage, and the shortlist of items that deserve human eyes (flagged + any large-dollar unmatched).

## Specifications
- **Deliverables**: `reconcile.py` (standalone CLI, `pandas` only), five CSVs, and `summary.md`, all attached to the final message.
- **Reconciliation engine**: every match record has a `match_type` ∈ {`direct_ref`, `direct_desc`, `heuristic_exact`, `heuristic_fee`, `netted`}; every flag record has `{trade_id?, txn_id, reason, detail}`.
- **Invariants**: no cash `txn_id` appears in more than one matched pair; no trade `trade_id` matches more than once; `len(matched_trades) + len(unmatched_trades) == len(input_trades)`; same for cash after excluding the non-trade pool.
- **Non-trade classification is exact-match only** on stripped-uppercase descriptions. Substring matching will incorrectly swallow legitimate trade wires like `Wire transfer - Apple Inc`.
- **Counterparty map is exhaustive for the input batch**: if the tool prints any `[WARN] unknown counterparty variant` lines, surface them in the final message and do not consider the run complete.
- **Validation**: assert row-count invariants and zero-delta on at least one `direct_ref` match before shipping results.

## Advice and Pointers
- Always profile the data before coding any matching logic — the edge cases (netted refs, fee wording, date-format drift, counterparty-name drift) tend to be visible only after you look at the actual values.
- Batch all clarification questions into one message with "defaults" as an option, so the user can unblock you in one round-trip.
- When the same trade has multiple plausible cash candidates in the heuristic stage, pick the smallest `|amount_delta|` and break ties by earliest `value_date`.
- Fee tolerance should be `max($ABS, PCT * gross)` — a flat $ only breaks for very small trades, a flat % only breaks for very large trades.
- `TRD-YYYY-######` IDs can appear in either the `reference` field *or* buried inside the `description` (e.g. `SEC SETTLE TRD-2026-000125`). Always extract from both.
- If the user mentions any custodian-internal reference scheme (e.g. `REF-######`) but has no mapping table, treat those refs as opaque and fall through to the heuristic stage.

## Forbidden Actions
- Do NOT modify either input file in place — treat inputs as read-only.
- Do NOT auto-match when signs don't agree with the direction convention (BUY/SELL ↔ cash sign). Always flag sign mismatches for human review.
- Do NOT use substring matching for non-trade classification — use exact (strip + upper) equality.
- Do NOT quietly collapse an unknown counterparty variant into an existing canonical. Warn and ask the user to extend the map.
- Do NOT commit generated outputs or CSVs to a repo unless the user explicitly asked for that; default is to attach them to the final message.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
