# Kickoff — Phase 4 (Pre-Trade Risk / Price Check)

Paste the block below into a fresh agent session to start Phase 4.

> One file per phase under `docs/kickoff/`. Keep them self-contained so a new
> session needs nothing from prior chat history.

---

```
You are working on the HKEX Connect Binary Trading OMS in
/home/alex/order-management-system.

STARTUP (do this first, in order):
1. Read AGENTS.md (auto-loaded) and then PROGRESS.md for live status, next
   actions and open questions.
2. Confirm the current phase in PROGRESS.md. Phases 0-3 are done (codec, session,
   order flow, reference data); Phase 4 is next. Reactor is 114 tests green.
3. Read DESIGN.md §5 and §5.1 (reference data, phase-aware model, 9-times rule,
   short-sell rules), TESTING.md §2 (Mock OMD-C), TEST_PLAN.md L4, and the code
   in oms-reference/ and oms-order/.
4. Run `mvn -q test` to confirm the starting point is green (expect 114 tests).

TASK: Implement Phase 4 — Pre-Trade Risk (price check).

Scope (Linear parent ALE-11):
- PriceCheck consuming the Phase 3 cache (oms-reference): on-tick, price band
  (OCG codes 16/101/102), reference-present (19), notional (20), quantity (13),
  market-vs-limit price rule.
- Phase-aware rule selection by session + sub-period:
  * POS order input:  Stage 1 ±15% of previous close.
  * CAS order input:  Stage 1 ±5% of reference price.
  * POS/CAS no-cancellation + random matching/closing: Stage 2 = within
    [highest bid, lowest ask] captured at the END of order input (frozen).
  * CTS: VCM ±10/15/20% by index tier; cooling-off bands.
  * Blocking / reference-price-fixing: no input; no-cancellation / random: input
    only (amend/cancel blocked).
- 9-times rule (CTS): reject if price deviates 9x or more from Nominal Price
  (or Previous Close). E.g. nominal 10.00 => buy >= 90.00, sell <= 1.11 rejected.
- Short-sell rules (Side=5): POS/CAS at-auction limit orders only (pure
  at-auction rejected); POS exempt from traditional tick rule but within ±15%;
  CTS traditional tick rule (not below best ask); CAS price not below the CAS
  reference price (fixed 16:01). Exemptions per DESIGN.md §5.1.
- Execution Instructions override (entitlement-gated).
- Local reject -> FIX reject (ExecutionReport for new; OrderCancelReject for
  amend), with NO OCG-C round-trip.
- Table-driven L4 tests (phase transitions, boundary values).

PREREQUISITE — extend the reference model (Option A). oms-reference today lacks
previousClose, nominalPrice, bestBid/bestAsk, and a session/sub-period model.
Extend ReferenceUpdate + SecurityReference + PhaseAwareReferenceCache to carry:
ClosingPrice, NominalPrice, BestBidAsk, and session + sub-period; capture the
Stage-2 band at the order-input boundary. KEEP the "Stage 1 / Stage 2" names
(industry standard; do not rename).

Acceptance: the TEST_PLAN.md L4 tests pass.

CONSTRAINTS:
- oms-order stays free of QuickFIX, and preferably of oms-reference; place the
  risk gate in an intake/composition layer (recommended) or a neutral seam.
- Locally rejected orders must never reach OCG-C.
- Any codec change needs a round-trip test + golden vector.

WHEN DONE (session discipline):
- `mvn -q test` green.
- Update PROGRESS.md (current phase -> Phase 5; last session's work; next actions).
- Commit with a Conventional Commit message (e.g. `feat(price-check): ...`).
- Reflect status in Linear (mark ALE-11 + children done as appropriate).
```

---

## Context (self-contained)

### Trading sessions, sub-periods and price limits

Full detail: `DESIGN.md` §5.1. Summary:

| Session | Sub-period | Price limit | Actions |
| --- | --- | --- | --- |
| Pre-Opening (POS) 09:00–09:15 | Order input | Stage 1: ±15% previous close | input/cancel/amend |
| POS 09:15–09:20 | No-cancellation | Stage 2: [highest bid, lowest ask] captured at 09:15 | input only |
| POS 09:20–09:22 | Random matching | Stage 2 (same band) | input only |
| POS 09:22–09:30 | Blocking | — | none (carry to CTS) |
| Continuous (CTS) 09:30–16:00 | — | VCM ±10/15/20%; cooling-off | input/cancel/amend |
| Closing (CAS) 16:00–16:01 | Reference price fixing | — | none |
| CAS 16:01–16:06 | Order input | Stage 1: ±5% reference | input/cancel/amend |
| CAS 16:06–16:08 | No-cancellation | Stage 2: [highest bid, lowest ask] captured at 16:06 | input only |
| CAS 16:08–16:10 | Random closing | Stage 2 (same band) | input only |

**Stage 1 / Stage 2 are price-limit regimes, not time periods** (industry
standard; keep the names).

### Static vs hot data

- **Static / cold** (survives feed outage): spread-table code → tick, previous
  close (Closing 62), board lot, market segment, VCM flag.
- **Slow / dynamic**: reference price (43), nominal price (40), trading
  session/sub-period (20), instrument state (21), VCM state/band (23).
- **Hot**: best bid/ask (top of book) — needed to capture the Stage-2 band at the
  order-input boundary.

### Reference data source

OMD-C primary; Refinitiv deferred. Source-agnostic (`ReferenceDataSource`); the
wire client is deferred (`ALE-77`) pending the OMD-C dictionary. Phase 4 uses the
mock/scripted source + fixtures.

### Decisions already made

- Module: `oms-risk` (pure; depends on `oms-order` + `oms-reference`).
- Integration: intake/composition layer calls `PriceCheck` before `OrderManager`
  (keeps `oms-order` free of `oms-reference`).
- POS/CAS/CTS all in scope for v1.
- Keep the Stage 1 / Stage 2 naming.

### Open decisions to confirm with the user

- Market-order notional: use reference price, or skip for market orders?
- Which sessions permit market orders?
- Override source: FIX `ExecInst (18)` vs OCG-C `executionInstructions`.
- Notional / max-qty thresholds (benchmark placeholders).
- Off-tick reject: map to FIX `OrdRejReason 17` (no OCG code exists).

### Highest-risk areas

- The Stage-2 band capture timing (order-input boundary) and freezing.
- Sub-period vs session modelling.
- Phase-aware rule selection correctness (POS vs CAS percentages).
