# Test Plan — HKEX Connect Binary Trading OMS

Holistic test plan covering every layer of the system, from codec byte-exactness
through session recovery, order lifecycle, price checking, queue shaping,
persistence, failover and HKEX conformance.

> Complements `TESTING.md` (strategy + mocks) with concrete layers, techniques,
> entry/exit criteria and traceability. Each layer maps to a Linear issue under
> **Phase 8 — Testing & Certification**.

---

## 1. Test levels & ownership

| Level | Layer | Phase | Primary technique |
| --- | --- | --- | --- |
| L0 | Codec (byte-exact) | 0 | unit + golden vectors + property round-trip |
| L1 | Dictionary (source of truth) | 0 | structural validation |
| L2 | Session state machine | 1 | unit + mock OCG-C scenario |
| L3 | Order flow + FIX mapping | 2 | integration vs Mock GFIX + Mock OCG-C |
| L4 | Price check | 4 | table-driven + phase-aware fixtures |
| L5 | Queue / throttle / chaining | 5 | unit + rate-shaping assertions |
| L6 | Persistence recovery | 6 | crash/restart replay |
| L7 | Failover / CoD | 7 | chaos + fault injection |
| L8 | End-to-end + conformance | 8 | mock E2E loop → HKEX Simulator → E2E → Rollout |

---

## 2. Layer detail

### L0 — Codec (Phase 0) — ✅ in progress
**Objective:** prove messages encode/decode byte-for-byte to the wire format.

- CRC-32C known-answer (`"123456789"` → `0xE3069283`).
- Wire primitives: little-endian ints, `Decimal`×1e8, `Byte`-as-ASCII, null-terminated fixed/variable alphanumerics (incl. empty/truncation/overflow).
- Framing golden vectors (Heartbeat exact bytes; presence-map MSB-first bit ordering).
- Property round-trip: **all 27 message types**, every field populated, `encode → decode → encode` identity.
- Boundary values: min/max per integer type, negative `Decimal`/ints, empty `alpha-var`, full-length `alpha-fixed`.
- Negative: bad STX, bad checksum, short frame.

**Entry:** dictionary loaded. **Exit:** all above green.

### L1 — Dictionary (Phase 0) — ✅ in progress
**Objective:** the source of truth is internally consistent.

- `fields.yaml` has 124 body fields (+ 8 header + 1 trailer).
- `messages.yaml` has 27 types; type set == {0..18, 21..28} (no 19/20).
- Every field referenced by a message exists in the field dictionary.
- No duplicate bit position within a message.
- Codegen determinism: regenerated enums are byte-identical to checked-in output.

### L2 — Session (Phase 1)
**Objective:** correct, recoverable OCG-C session.

- Logon happy path; sequence >1 rejected; `NextExpected` >/==/< reconciliation.
- Heartbeat 20s → Test Request after 3 intervals → Logout after ~3 more.
- Resend modes (single/range/all-after); gap-fill skip list; PossDup/PossResend dedupe.
- Sequence Reset gap-fill vs reset (client reset rejected).
- Reconnect timers (5/10/60s); checksum failure → drop without Logout.
- **Mock OCG-C server** drives these deterministically (no HKEX).

### L3 — Order flow + FIX mapping (Phase 2)
**Objective:** correct order lifecycle and upstream translation.

- New/Amend/Cancel/Mass-Cancel; cancel/replace (new Order ID); trade-cancel no-reinstate.
- ExecType → FIX `ExecutionReport`/`OrderCancelReject` (incl. `'X'`/`'Y'` synthesis).
- Transient statuses (Pending New/Cancel/Amend) → FIX `A`/`6`/`E`.
- Unsolicited-cancel `Exec Restatement Reason` propagation.
- In-flight exclusivity + ID chaining.

### L4 — Price check (Phase 4)
**Objective:** correct, phase-aware pre-trade validation.

- On-tick, band (16/101/102), notional (20), ref-price-missing (19), qty (13).
- Phase transitions: POS (±15% prev close) → CTS (VCM dynamic) → CAS.
- VCM cooling-off narrows band; No-Cancellation blocks amend/cancel.
- Override (`Execution Instructions`) entitlement gating.

### L5 — Queue / throttle / chaining (Phase 5)
**Objective:** shaped, ordered, unique outbound flow.

- Token-bucket MPS (query via 25/26; no exchange throttle reject under load).
- Client Order ID uniqueness + chain tracking across cancel/replace.
- One amend/cancel in-flight per order.

### L6 — Persistence (Phase 6)
**Objective:** no order loss across restart.

- Snapshot + WAL tail replay rebuilds state; sequence numbers restored.
- Postgres archive is eventually-consistent and never blocks the hot path.

### L7 — Failover / CoD (Phase 7)
**Objective:** correct promotion and reconciliation.

- Kill primary mid-order → standby promotes → Logon with correct sequence → gap-fill.
- Double-Logon terminates both; no client-side sequence reset.
- CoD cancels after 300s unless re-logon; `Exec Restatement Reason = 104` reconciled.

### L8 — End-to-end + conformance (Phase 8)
**Objective:** demonstrate full loop, then pass HKEX.

- Mock E2E: `Mock GFIX → OMS → Mock OCG-C → fills → FIX` in CI, zero external deps.
- HKEX Offline Simulator (Phase 1) → End-to-End → Rollout → BSS declaration.

---

## 3. Cross-cutting

- **Property/fuzz:** encode arbitrary field sets; decode must never throw on valid frames; corruptions must fail cleanly (no silent success).
- **Determinism:** same input → identical bytes (stable for replay).
- **Performance guard:** encode/decode < 100 µs/message (benchmark, informational).
- **Traceability:** each test class references the FR it covers (see `REQUIREMENTS.md`).

## 4. Tooling

JUnit 5 (unit/integration), mock OCG-C/GFIX/OMD-C harness (L2/L3/L8), Chronicle/Postgres
Testcontainers (L6), scripted chaos runner (L7). All wired into `mvn test` in CI.

## 5. Exit criteria (per layer)

L0/L1 must be green before Phase 1 starts; L2 before Phase 2; and so on — no layer
advances on red. L8 is the final gate before HKEX certification.
