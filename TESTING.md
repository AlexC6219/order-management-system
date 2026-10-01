# Testing & Certification Plan — HKEX Connect Binary Trading OMS

The OMS can be validated almost entirely **without HKEX or the upstream systems**,
by building simulators for both sides of every boundary. HKEX and GFIX are only
needed for the final certification mile.

> Principle: nothing is real in test except the code under test. The codegen'd
> codec, order state machine, price-check logic and persistence are exercised
> against mocks; only those four stay "real".

---

## 1. Mock Harness Components

### 1.1 Mock OCG-C server

A small TCP server speaking the OCG-C binary protocol, built on the **same
codegen'd codec** as the production gateway.

| Feature | Details |
| --- | --- |
| Lookup Service | Answers `Lookup Request (7)` with `Lookup Response (8)` (dummy primary/secondary IP:port), or a scripted reject with a `Lookup Reject Code`. |
| Logon | Validates `Comp ID` + password using a **self-generated RSA-2048 test keypair** (not HKEX's key); returns `Session Status` (Active / Password Changed / rejected). |
| Heartbeat / Test Request | 20 s interval, Test Request after 3 missed intervals, Logout after ~3 more — mirrors §4.3. |
| Sequence tracking | Maintains `Next Expected` / `Next To Send`; enforces the `NextExpected` >/==/< reconciliation on Logon (§5.3). |
| Gap-fill / Resend | Responds to `Resend Request (2)`; honours the gap-fill skip list. |
| Script engine | Replays scripted `Execution Report` / `Business Message Reject` / `Reject` streams against submitted orders — fills, cancels, rejects, trade-cancels, unsolicited cancels with `Exec Restatement Reason`. |
| Fault injection | Checksum corruption, abrupt disconnect, double-Logon termination, mid-resend drops, CoD simulation. |

The mock is deterministic and scriptable, so every session and order scenario is
repeatable in CI.

### 1.2 Mock GFIX client

A QuickFIX/J **initiator** acting as GFIX — the single FIX upstream.

- Sends `NewOrderSingle`, `OrderCancelReplaceRequest`, `OrderCancelRequest`.
- Consumes and asserts on `ExecutionReport (8)` and `OrderCancelReject (9)`.
- Used to verify the FIX acceptor, field mapping (§8 of DESIGN.md) and the
  upstream boundary (session sequencing, `ClOrdID` intake).

### 1.3 Mock OMD-C / static reference data

The reference/pricing source (OMD-C) is a separate HKEX dependency and is mocked
too. See §2.

---

## 2. Reference Data Mocking

Pricing data is **not part of the order-routing path**; it is the input to the
**pre-trade price check** that runs before queueing. That check needs three
things (OMD-C in production): spread table/tick size, reference price + price
band, and trading state.

The Reference & Price/State Cache is a pluggable source, so the same cache and
price-check code runs against a mock in test and OMD-C in production.

```
Mock GFIX (FIX) ──▶ OMS ──▶ [ price check ← reference data ] ──▶ Mock OCG-C ──▶ scripted fills
                       ▲
    Mock OMD-C / static reference fixtures ──┘
```

### 2.1 Static reference fixtures

Deterministic, table-driven inputs for pure price-check tests — no feed infra.

```yaml
# example fixture: reference_fixture.yaml
symbol: "00005"
spread_table_code: "01"          # → tick size table
reference_price: 78.50           # CTS dynamic reference / POS previous close
band: { lower: 70.65, upper: 86.35 }   # ±10% example
vcm_state: none                  # none | cooling_off
trading_phase: CTS               # POS | CAS | CTS
```

Used for boundary tests: on-tick/off-tick, band edges, notional threshold,
quantity, market-vs-limit price rules, reference-price-missing.

### 2.2 Mock OMD-C publisher

A scripted stream of the OMD-C messages the cache consumes, for phase-aware and
state-transition tests.

| OMD-C message | What it drives |
| --- | --- |
| `Security Definition (11)` | spread table code → tick size, board lot, market segment, VCM flag |
| `Reference Price (43)` | reference price |
| `VCM Trigger (23)` | price band, cooling-off window |
| `Closing Price (62)` | POS previous close |
| `Nominal Price (40)` | nominal price |
| `Trading Session Status (20)` | POS / CAS / CTS phase |
| `Security Status (21)` | halt / suspension |

Scenario examples:

- **POS → CTS transition**: assert the price check switches from previous-close
  Stage-1 (±15%) to VCM dynamic band (±10/15/20%).
- **VCM cooling-off**: a `VCM Trigger` narrows the band; assert orders outside
  the cooling band are rejected locally.
- **No-Cancellation state**: assert amends/cancels are blocked locally and CoD
  can't execute.

---

## 3. Test Tiers → Delivery Phases

| Tier | Harness | Phases covered | Needs HKEX? |
| --- | --- | --- | --- |
| Codec golden vectors | none (pure unit) | 0 | No |
| Session simulation | Mock OCG-C server | 1 | No |
| Order flow + FIX mapping | Mock GFIX + Mock OCG-C | 2 | No |
| Reference / price check | Mock OMD-C + fixtures | 3–4 | No |
| Queue / throttle / chaining | Mock OCG-C (scripted rejects) | 5 | No |
| Failover / CoD | Mock OCG-C (fault injection) | 7 | No |
| HKEX Offline Simulator | HKEX package | 8 (Phase 1) | Partial (vendor) |
| E2E + Rollout | HKEX test env | 8 | Yes |

---

## 4. Test Matrices

### 4.1 Codec

Round-trip encode→decode→encode; presence-map MSB ordering; null-termination;
empty fixed/variable field; repeating + nested blocks; `Byte`-as-ASCII
(`'0'` vs `0x00`); `Decimal` ×10⁸; integer endianness (little-endian ints,
big-endian RSA); CRC32C (poly `0x1EDC6F41`); `Length` field convention
(includes STX + length bytes?) — pinned via golden vectors.

### 4.2 Session

Logon happy path; sequence >1 rejected; `NextExpected` >/==/<; gap-fill; resend
modes (single/range/all-after); PossDup/PossResend dedupe; sequence-reset
gap-fill vs reset; heartbeat→test-request→logout ladder; reconnect timers
(5/10/60 s); checksum failure → drop without logout.

### 4.3 Order

New/amend/cancel/mass-cancel flows; cancel/replace (new `Order ID`); trade-cancel
no-reinstate; unsolicited-cancel `Exec Restatement Reason` (103/104/105/106/107);
in-flight exclusivity; ID chaining cap.

### 4.4 Price check (reference-data driven)

On-tick; price band (codes 16/101/102); notional (20); reference-price-missing
(19); quantity (13); market-order-no-price vs limit-order-requires-price;
phase-aware band selection (§2.2 scenarios).

### 4.5 FIX mapping

Exact FIX 5.0 SP2 output for every Exec Type; `OrderCancelReject (9)` synthesis
from `'X'`/`'Y'`; Pending statuses → FIX `A`/`6`/`E`.

### 4.6 Failover & chaos

Kill primary mid-order → standby Logon → gap-fill → reconcile; disconnect
mid-resend; double-Logon termination; CoD window reconciliation.

---

## 5. End-to-End Loop (zero external deps)

```
Mock GFIX (FIX initiator) ──▶ OMS ──▶ Mock OCG-C server
      ▲                            │
      └──── ExecutionReport ◀──────┘
   (reference data from Mock OMD-C / fixtures feeds the price check)
```

One scripted scenario covers the full lifecycle: submit order → price check →
queue/throttle → binary encode → mock accepts → mock emits `Exec Type=F` →
OMS maps back to FIX `ExecutionReport (8)` → assert. Runs in CI.

---

## 6. HKEX Conformance Path (the last ~10%)

1. **Offline Simulator (Phase 1)** — standalone Windows tool with a pre-defined
   test-case package; downloadable as a BSS vendor appointed by an EP (or during
   an EP application). Verifies message format independently, no HKEX connection.
2. **End-to-End (E2E)** — needs a testing line (SDNet/2 or HSTN) + testing
   Comp ID; results via the OTP-C E2E Test Portal.
3. **Rollout** — production-like weekend test; then BSS Declaration + rollout
   notification.

Only these phases need HKEX/GFIX; everything in §3–§5 is done with mocks first.
