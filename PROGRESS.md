# Progress / Handoff

The single living "where are we / what next" document. **Update this at the end
of every session** (see "Session discipline" at the bottom). A fresh agent
should read `AGENTS.md` → this file → pick up the "Next actions".

_Last updated: 2026-10-05_

---

## Current phase

**Phase 2 — Order Flow** (next up). Phase 1 (OCG-C Session) is complete.

---

## Phase status

| Phase | Scope | State |
| --- | --- | --- |
| 0 | OCG-C binary codec + dictionary + codegen | ✅ **done** (30 tests green) |
| 1 | Session state machine (Lookup/Logon/heartbeat/sequence/recovery) + Mock OCG-C | ✅ **done** (21 tests green) |
| 2 | Order flow (order state machine, QuickFIX/J FIX 5.0 SP2, FIX↔OCG-C mapping) | ⏳ **next** |
| 3 | Reference data (OMD-C client, phase-aware cache) | todo |
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

- **Phase 1 session** built and tested in new module `oms-session`:
  - `SequenceTracker`, `LogonReconciliation`, `SessionState`/`SessionTermination`.
  - `LookupClient` (preference-ordered endpoints, 5s retry, cycling; seq 1).
  - `PasswordCipher` — JDK-native RSA-2048/OAEP (SHA-256 digest + MGF1), UTC
    `YYYYMMDDHHMMSS` prefix, big-endian, base-64. No BouncyCastle.
  - `SessionEngine` — heartbeat ladder (20s / 3 intervals / ~3 more), Logon
    reconciliation N>S / N==S / N<S, gap detection + queued out-of-order drain,
    Resend Request (single/range/all-after), gap-fill skip list, PossDup/
    PossResend dedupe by Execution ID, Sequence Reset, Logout.
  - `MockOcgServer` (test scope) — TCP server on the codegen'd codec, self-generated
    RSA test keypair, Lookup/Logon validation, sequence tracking, scripted
    Execution Report replay, fault hooks (reject, checksum corruption, disconnect,
    double-logon termination, mid-resend drops).
  - **21 tests green** in `oms-session` at first cut: `SessionEngineTest(13)`,
    `PasswordCipherTest(2)`, `LookupClientTest(2)`, `MockOcgSessionTest(4)`.
- **Phase 1 hardening** (same session, after review):
  - `SessionEngine.onLogonReply` now classifies the Logon reply's `Session Status`
    (`LogonStatus`); refused → `LOGON_REJECTED`, password change required →
    `PASSWORD_CHANGE_REQUIRED`, due-to-expire accepted. New return type
    `LogonOutcome`.
  - Inbound **Comp ID validation**; bounded (1024) **execution-ID dedupe window**
    replacing the single-value guard.
  - **27 tests green** in `oms-session`: `SessionEngineTest(18)`,
    `PasswordCipherTest(2)`, `LookupClientTest(2)`, `MockOcgSessionTest(5)`.
    Full reactor **57 tests green**.
- **Phase 0**: unchanged, 30 tests green.

### Deviations / notes

- Transport framing between client and mock uses a 4-byte length prefix,
  independent of the OCG-C `Length` field, whose exact convention remains open.
  The mock and client agree, so Phase 1 is unaffected.
- `onLogonReply` takes the peer's Next To Send explicitly (it is not carried in
  the Logon reply wire format); the engine infers it when the reply arrives via
  `onMessage`.

---

## Next actions (start here)

Phase 2 — Order flow. Parent: `ALE-13`. Build the order state machine, upstream
FIX acceptor (QuickFIX/J FIX 5.0 SP2), and FIX↔OCG-C mapping (DESIGN.md §8).

- [ ] Order state machine (`PENDING_NEW → NEW → PARTIALLY_FILLED → FILLED`,
      amend/cancel transients) + repository/ID allocator
- [ ] Exec Type → FIX 5.0 SP2 mapping; `OrderCancelReject (9)` synthesis
- [ ] QuickFIX/J acceptor for the single GFIX session; `ClOrdID` intake
- [ ] Mock GFIX client (initiator) for L3 tests
- [ ] L3 integration: Mock GFIX → OMS → Mock OCG-C → fills → FIX
- [ ] Agent-added items on `ALE-13` (`ALE-67`–`ALE-70`, `ALE-46` clarification) —
      review before starting

Acceptance bar: the `TEST_PLAN.md` L3 tests, driven by Mock GFIX + Mock OCG-C.

---

## Verify

```bash
mvn -q test     # expect 57 tests green (30 codec + 27 session)
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
- **Phase 2 (Order Flow, `ALE-13`)** — `ALE-67` ingress ClOrdID dedupe, `ALE-68`
  ingress field validation, `ALE-69` cumQty/leaves consistency check, `ALE-70`
  QuickFIX/J persistent store; `ALE-46` extended with stale/superseded ER
  handling + the "dedupe must not affect session sequence accounting" rule.
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
