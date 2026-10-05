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
  - **21 tests green** in `oms-session`: `SessionEngineTest(13)`, `PasswordCipherTest(2)`,
    `LookupClientTest(2)`, `MockOcgSessionTest(4)`. Full reactor **51 tests green**.
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

Phase 2 — Order flow. Parent: `ALE-9`. Build the order state machine, upstream
FIX acceptor (QuickFIX/J FIX 5.0 SP2), and FIX↔OCG-C mapping (DESIGN.md §8).

- [ ] Order state machine (`PENDING_NEW → NEW → PARTIALLY_FILLED → FILLED`,
      amend/cancel transients) + repository/ID allocator
- [ ] Exec Type → FIX 5.0 SP2 mapping; `OrderCancelReject (9)` synthesis
- [ ] QuickFIX/J acceptor for the single GFIX session; `ClOrdID` intake
- [ ] Mock GFIX client (initiator) for L3 tests
- [ ] L3 integration: Mock GFIX → OMS → Mock OCG-C → fills → FIX

Acceptance bar: the `TEST_PLAN.md` L3 tests, driven by Mock GFIX + Mock OCG-C.

---

## Verify

```bash
mvn -q test     # expect 51 tests green (30 codec + 21 session)
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

## Linear

Project: **HKEX Connect Binary Trading OMS** — team `ALE`.
Phase parents: `ALE-5` (Docs), `ALE-6` (Phase 0) … `ALE-12` (Phase 9), `ALE-7`
(Testing). Test layers under `ALE-7`. Treat Linear as the **backlog**; this file
+ git are the **source of truth for current state**.

---

## Session discipline

At the end of each session:

1. Ensure `mvn -q test` is green.
2. Update this file (Current phase, Phase status, Last session's work, Next actions).
3. Commit (`git add <paths>` then a Conventional Commit message).
4. Reflect status in Linear (mark issues done / move the next ones to In Progress).
