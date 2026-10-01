# Progress / Handoff

The single living "where are we / what next" document. **Update this at the end
of every session** (see "Session discipline" at the bottom). A fresh agent
should read `AGENTS.md` → this file → pick up the "Next actions".

_Last updated: 2026-10-01_

---

## Current phase

**Phase 1 — OCG-C Session** (not started; next up)

---

## Phase status

| Phase | Scope | State |
| --- | --- | --- |
| 0 | OCG-C binary codec + dictionary + codegen | ✅ **done** (30 tests green) |
| 1 | Session state machine (Lookup/Logon/heartbeat/sequence/recovery) + Mock OCG-C | ⏳ **next** |
| 2 | Order flow (order state machine, QuickFIX/J FIX 5.0 SP2, FIX↔OCG-C mapping) | todo |
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

- **Phase 0 codec** built and tested:
  - `dictionary/fields.yaml` (124 fields + header/trailer), `dictionary/messages.yaml` (27 types).
  - `oms-codec`: `Crc32c`, `Wire`, `Dictionary`, `Message`, `MessageCodec`.
  - `codegen`: `Main` → 52 typed enums (regenerable).
  - **30 tests green**: `Crc32cTest`, `WireTest`, `MessageCodecTest`, `DictionaryTest`, `CodecRoundTripTest`.
- **Docs**: `README.md`, `UR.md`, `DESIGN.md`, `TESTING.md`, `TEST_PLAN.md`.
- **Linear**: project + 11 phase parents + children backfilled; test-plan layers L0–L7 added.
- **Correction found by tests**: the spec has **27** message types (0–18, 21–28), not 28.

---

## Next actions (start here)

Phase 1 — OCG-C session. Linear issues `ALE-29`, `ALE-28`, `ALE-35`, `ALE-36`,
`ALE-40`, `ALE-48` (session) + `ALE-43` (Mock OCG-C server). Parent: `ALE-8`.

- [ ] Lookup Service client (primary→mirror→backup cycle, 5s retry)
- [ ] Logon + RSA-2048 password (PKCS#1/OAEP, big-endian→base64, UTC time prefix)
- [ ] Heartbeat / Test-Request ladder (20s / 3 intervals)
- [ ] Sequence tracking + Logon reconciliation (N>S / N==S / N<S)
- [ ] Resend / gap-fill (+ gap-fill skip list, PossDup/PossResend)
- [ ] Logout
- [ ] Mock OCG-C server (codec + self-generated RSA test keypair + script engine)

Acceptance bar: the `TEST_PLAN.md` L2 tests, driven by the Mock OCG-C server.

---

## Verify

```bash
mvn -q test     # expect 30 tests green (Phase 0)
```

---

## Open questions / risks

- **`Length` field convention** (includes STX + its own bytes?) — only verifiable
  against HKEX golden vectors / Offline Simulator. Mock is self-consistent, so
  not a Phase 1 blocker.
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
