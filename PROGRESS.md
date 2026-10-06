# Progress / Handoff

The single living "where are we / what next" document. **Update this at the end
of every session** (see "Session discipline" at the bottom). A fresh agent
should read `AGENTS.md` → this file → pick up the "Next actions".

_Last updated: 2026-10-05_

---

## Current phase

**Phase 3 — Reference Data** (next up). Phase 2 (Order Flow) is complete.

---

## Phase status

| Phase | Scope | State |
| --- | --- | --- |
| 0 | OCG-C binary codec + dictionary + codegen | ✅ **done** (30 tests green) |
| 1 | Session state machine (Lookup/Logon/heartbeat/sequence/recovery) + Mock OCG-C | ✅ **done** (28 tests green) |
| 2 | Order flow (order state machine, QuickFIX/J FIX 5.0 SP2, FIX↔OCG-C mapping) | ✅ **done** (41 tests green) |
| 3 | Reference data (OMD-C client, phase-aware cache) | ⏳ **next** |
| 4 | Pre-trade risk (price check) | todo |
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

- **Phase 2 order flow** built and tested across new modules:
  - `oms-test-harness` — `MockOcgServer` promoted out of `oms-session` test scope
    (depends only on `oms-codec`; no module cycle) so later phases can reuse it.
  - `oms-order` (pure domain, no QuickFIX):
    - `OrderState`, `OrderEvent`, `Order`, `OrderStateMachine` (terminal-state
      protection; Trade Cancel on `FILLED` busts but does **not** reinstate
      leaves).
    - `ClientOrderIdAllocator` (1..99,999,999, daily reset), `OrderRepository`,
      `OrderChain` aliasing.
    - `IngressValidator` (DESIGN §6.4), upstream-ClOrdID dedupe,
      `ExecutionConsistency` (`leaves = qty − cum`).
    - `ExecTypeMapper` (OCG Exec Type → internal event), `OrderTranslator`
      (New/Amend/Cancel/MassCancel build + ER decode), `TransactionTime`.
    - `OrderManager` orchestrator (submit/amend/cancel/massCancel, ER
      reconciliation with Exec-ID dedupe).
  - `oms-fix` (QuickFIX/J 2.3.1):
    - `FixMapping` — FIX 5.0 SP2 ↔ internal, `OrderCancelReject (9)` synthesis
      from `'X'`/`'Y'`.
    - `FixOrderApplication` (QuickFIX `Application`), `FixAcceptor`.
    - **L3 integration** `FixOrderFlowL3Test`: live Mock GFIX (QuickFIX/J
      initiator) → OMS acceptor → OCG-C New Order → Execution Report → FIX.
  - `oms-session` — `SessionEngine.send(Message)` + `setInboundHandler` for the
    business-message path.
  - **99 tests green** (30 codec + 28 session + 34 order + 7 fix).
- **Phase 1** (prior session): session state machine + Mock OCG-C; hardening
  (`sessionStatus`, Comp ID validation, bounded dedupe) — 28 tests.
- **Phase 0**: unchanged, 30 tests green.

### Deviations / notes

- `oms-order` stays free of QuickFIX; all FIX handling lives in `oms-fix`.
- Optional repeating (`multi`) OCG-C fields (`executionInstructions`,
  `orderRestrictions`) are deliberately omitted in v1 (codec group support is
  separate work).
- `ALE-70` (QuickFIX persistent store) deferred to Phase 6 per decision D17.
- Mock GFIX currently lives in `oms-fix` test scope; may move to
  `oms-test-harness` when Phase 3 needs it.

---

## Next actions (start here)

Phase 3 — Reference data. Parent: `ALE-14`. OMD-C client + phase-aware
Reference/Price/State cache (DESIGN.md §5; UR.md §5). No HKEX dependency.

- [ ] OMD-C message decode (shared codegen) for Security Definition, Reference
      Price, VCM Trigger, Closing Price, Trading Session Status, Security Status
- [ ] Phase-aware cache: per-security `{spread_table, reference_price, band,
      vcm_state, trading_phase}`
- [ ] Static spread-table fallback file
- [ ] Mock OMD-C publisher + phase-transition scenarios
- [ ] L4 preparation fixtures

Acceptance bar: the `TEST_PLAN.md` L4 inputs (Phase 4 consumes this cache).

---

## Verify

```bash
mvn -q test     # expect 99 tests green (30 codec + 28 session + 34 order + 7 fix)
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
- Reference data source confirmed as OMD-C direct; feed specifics TBD in Phase 3.

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
