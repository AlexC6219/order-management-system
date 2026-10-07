# Progress / Handoff

The single living "where are we / what next" document. **Update this at the end
of every session** (see "Session discipline" at the bottom). A fresh agent
should read `AGENTS.md` → this file → pick up the "Next actions".

_Last updated: 2026-10-06_

---

## Current phase

**Phase 4 — Pre-Trade Risk** (next up). Phase 3 (Reference Data) is complete
(source-agnostic, per decision B2). Full Phase 4 scope, acceptance and decisions:
`docs/kickoff/phase-4.md`.

---

## Phase status

| Phase | Scope | State |
| --- | --- | --- |
| 0 | OCG-C binary codec + dictionary + codegen | ✅ **done** (30 tests green) |
| 1 | Session state machine (Lookup/Logon/heartbeat/sequence/recovery) + Mock OCG-C | ✅ **done** (28 tests green) |
| 2 | Order flow (order state machine, QuickFIX/J FIX 5.0 SP2, FIX↔OCG-C mapping) | ✅ **done** (34 tests green) |
| 3 | Reference data (source-agnostic cache; OMD-C wire client deferred) | ✅ **done** (15 tests green) |
| 4 | Pre-trade risk (price check) | ⏳ **next** |
| 5 | Queue management (throttle, ID chaining, in-flight exclusivity) | todo |
| 6 | Persistence (Chronicle WAL + Postgres) | todo |
| 7 | Active/standby failover + CoD | todo |
| 8 | Testing & certification (mock harness → HKEX conformance) | todo |
| 9 | Stock Connect (SZ/SH) | deferred |

---

## Locked decisions (full detail: `DESIGN.md` §2)

Java 21 + Maven · QuickFIX/J (upstream FIX 5.0 SP2) · IBM MQ (GSOPS via GFIX) ·
codegen OCG-C codec (single source of truth = `dictionary/*.yaml`) · OMD-C direct
reference data · Chronicle WAL · Postgres archive · active/standby · orders-only
v1 (Quote/TradeCapture/OBO feature-flagged) · CoD 300s · Spring Boot =
admin-only (not in the hot path).

---

## Last session's work

- **Phase 3 reference data** (decision **B2** — source-agnostic) in new module
  `oms-reference`:
  - Domain: `TradingPhase` (POS/CAS/CTS + no-cancel/random-match/blocking, with
    `permitsAmendOrCancel`), `VcmState`, `InstrumentState`, `PriceBand`,
    `SpreadTable`, `SecurityReference`.
  - `ReferenceUpdate` (sealed) mirrors the OMD-C messages in DESIGN.md §5.
  - `PhaseAwareReferenceCache` — per-security `{spread_table, reference_price,
    band, vcm_state, trading_phase}`; market-wide phase, per-security instrument
    state.
  - `ReferenceDataSource` interface + `ScriptedReferenceDataSource` (tests/demos)
    + `StaticSpreadTable` (HKEX spread-table file fallback).
  - **15 tests green** (`PhaseAwareReferenceCacheTest(6)`, `TradingPhaseTest(3)`,
    `PriceBandTest(2)`, `StaticSpreadTableTest(2)`,
    `ScriptedReferenceDataSourceTest(2)`).
  - **Deferred:** the real `OmdClient` + dictionary codegen — no OMD-C data
    dictionary is available (see open questions).
  - Docs: recorded the source decision (OMD-C primary, Refinitiv deferred) in
    `DESIGN.md` §5 and `UR.md` §7.
  - **114 tests green** reactor-wide (30 codec + 28 session + 34 order +
    15 reference + 7 fix).
- **Trading-phase model captured** (docs only, this session): the full
  session/sub-period rules are now in `DESIGN.md` §5.1 and `UR.md` §5 — POS /
  CTS / CAS sub-periods, Stage 1/Stage 2 price-limit regimes (industry-standard
  names retained), the **9-times rule** (CTS) and the **short-sell rules**
  (POS/CTS/CAS). `AGENTS.md` and `docs/kickoff/phase-4.md` updated for the next
  session.
- **Phase 2 order flow** (prior session) — `oms-order`, `oms-fix`,
  `oms-test-harness`; 34 + 7 tests.
- **Phase 0/1** unchanged.

### Deviations / notes

- `oms-reference` is deliberately source-agnostic (B2): no OMD-C wire decode
  until the dictionary lands. A Refinitiv adapter can also plug into
  `ReferenceDataSource` later.
- `ScriptedReferenceDataSource` lives in `oms-reference` main (not
  `oms-test-harness`) since it is dependency-free and useful for demos; can move
  later.
- `oms-order` stays free of QuickFIX; all FIX handling lives in `oms-fix`.

---

## Next actions (start here)

Phase 4 — Pre-trade risk (price check). Parent: `ALE-11`. Consumes the Phase 3
cache; no HKEX dependency (Mock OMD-C / fixtures). Full brief:
`docs/kickoff/phase-4.md`.

- [ ] **Prerequisite — extend the reference model (Option A):** add
      `previousClose`, `nominalPrice`, `bestBid`/`bestAsk` and a session +
      sub-period model to `oms-reference`; capture the Stage-2 band at the
      order-input boundary. Keep the Stage 1 / Stage 2 names.
- [ ] `oms-risk` module: `PriceCheck` consuming the cache — on-tick, price band
      (16/101/102), reference-present (19), notional (20), quantity (13),
      market-vs-limit price rule
- [ ] Phase-aware rules: POS Stage 1 ±15% / CAS Stage 1 ±5% / Stage 2
      [highest bid, lowest ask]; CTS VCM ±10/15/20% + cooling-off; blocking /
      reference-price-fixing = no input; no-cancellation / random = input only
- [ ] **9-times rule** (CTS) and **short-sell rules** (POS/CTS/CAS)
- [ ] `Execution Instructions` override entitlement seam
- [ ] Local reject → FIX reject (ExecutionReport / OrderCancelReject), no OCG-C
      round-trip; wired via an intake/composition layer
- [ ] Table-driven L4 tests (phase transitions, boundary values)

Acceptance bar: the `TEST_PLAN.md` L4 tests.

---

## Verify

```bash
mvn -q test     # expect 114 tests green (30 codec + 28 session + 34 order + 15 reference + 7 fix)
```

---

## Open questions / risks

- **`Length` field convention** (includes STX + its own bytes?) — only verifiable
  against HKEX golden vectors / Offline Simulator. Mock is self-consistent, so
  not currently a blocker.
- **OAEP digest/MGF1 hash** — session pins SHA-256/SHA-256 in `PasswordCipher`
  (one constant each). Revisit if HKEX golden vectors disagree.
- **Repeating blocks** (Throttle/Party Entitlements responses) modelled in the
  dictionary but not encoded/decoded yet — not needed until admin queries.
- **Codegen emits enums only** (no typed POJOs) — data-driven codec works; POJO
  generation is an optional follow-up.
- From HKEX: MPS entitlement, CoD delay, Comp ID / Submitting Broker ID scheme.
- **OMD-C data dictionary/spec is not available** — so the real `OmdClient` wire
  decode is deferred. Phase 3 built the source-agnostic cache (B2); the adapter
  drops in when the dictionary lands. Source decision recorded: OMD-C primary,
  Refinitiv deferred (`DESIGN.md` §5, `UR.md` §7).
- **Best bid/ask source** — the Stage-2 band is `[highest bid, lowest ask]`
  captured at the end of order input. *Which* OMD-C message carries top-of-book
  is unknown until the OMD-C dictionary lands (`ALE-77`); model it via the mock
  for now.
- **Stage-2 capture** — cache captures the band automatically on the
  `order-input → no-cancellation` transition (vs an explicit published update);
  to confirm when building Phase 4.
- **Sub-period modelling** — `oms-reference` currently has a flat `TradingPhase`;
  Phase 4 needs session + sub-period. Extension pending.
- **9-times rule scope** — documented as CTS; confirm whether it also applies
  elsewhere, and whether the base is Nominal Price or Previous Close.
- **Short-sell exemptions** — the market-maker/hedging and index-arbitrage
  exemptions need an entitlement source (TBD).

---

## Agent-added planning notes (pending review)

Items proposed by the opencode agent, marked `agent-added` in Linear; revisit
and ratify/trim when unfolding the relevant phase.

- **Phase 1 (OCG-C Session, `ALE-8`)** — fixed immediately: `sessionStatus`
  handling, Comp ID validation, bounded dedupe window. Logged as backlog
  follow-ups: `ALE-74` resend-request coalescing, `ALE-75` checksum-failure
  handling, `ALE-76` trading-day rollover.
- **Phase 2 (Order Flow, `ALE-13`)** — implemented in this session: `ALE-67`
  ingress ClOrdID dedupe, `ALE-68` ingress field validation, `ALE-69`
  cumQty/leaves consistency check, `ALE-46` stale/superseded ER handling +
  layering rule. `ALE-70` (QuickFIX/J persistent store) deferred to Phase 6 (D17).
- **Phase 3 (Reference Data, `ALE-14`)** — delivered source-agnostic (B2);
  `ALE-77` (OMD-C wire client + dictionary codegen) added as the deferred
  adapter, pending the OMD-C dictionary.
- **Phase 5 (Queue Management, `ALE-15`)** — beyond the original MPS throttle /
  ID chaining / in-flight exclusivity scope, added: `ALE-61` priority queue
  (cancels before new), `ALE-62` bounded queue + backpressure, `ALE-63` adaptive
  backoff on over-rate reject, `ALE-64` per-order causality + sequence
  reservation, `ALE-65` in-flight timeout + reconciliation, `ALE-66` staleness/
  expiry. Deferred notes: WAL-backed queue (Phase 6), circuit breaker (Phase 7),
  dead-letter + fair queueing (Phase 8).
- **Phase 6 (Persistence, `ALE-10`)** — `ALE-73` persist & restore session
  sequence numbers across restart.
- **Phase 7 (Failover, `ALE-9`)** — `ALE-71` reconnect backoff + jitter, `ALE-72`
  monotonic clock for timers.

---

## Linear

Project: **HKEX Connect Binary Trading OMS** — team `ALE`.
Phase parents: `ALE-5` (Docs), `ALE-6` (Phase 0) … `ALE-12` (Phase 9), `ALE-7`
(Testing). Test layers under `ALE-7`. Label `agent-added` marks agent-proposed
scope. Treat Linear as the **backlog**; this file + git are the **source of
truth for current state**.

---

## Session discipline

At the end of each session:

1. Ensure `mvn -q test` is green.
2. Update this file (Current phase, Phase status, Last session's work, Next actions).
3. Commit (`git add <paths>` then a Conventional Commit message).
4. Reflect status in Linear (mark issues done / move the next ones to In Progress).
