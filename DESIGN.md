# HKEX Connect Binary Trading OMS — Design Document

Replacement for the incumbent **Fidessa** order management system for the
HASE stock trading flow. The OMS ingests orders from upstream channels over
FIX 5.0 SP2 / MQ, performs **price checking** and **queue management**, and
routes orders to **HKEX** using the **Orion Central Gateway — Securities Market
(OCG-C) Binary Trading Protocol**.

> Current scope: **Hong Kong Connect** (HKEX securities, `Security Exchange =
> XHKG`). Shenzhen / Shanghai Stock Connect designed for but deferred.

---

## 1. Purpose & Scope

This document captures the architecture and key design decisions for a
low-latency, session-oriented OMS that:

1. Accepts order flow from upstream broker gateways (RBP / GFIX / GSOPS).
2. Applies pre-trade controls — price band, tick size, notional and quantity
   checks — before anything reaches the exchange.
3. Manages an outbound order queue per exchange session, including message-rate
   throttling, in-flight request exclusivity and ID chaining.
4. Speaks the **OCG-C binary protocol** to HKEX (not FIX — see §3).
5. Maintains a real-time order/execution state store plus an archived audit
   trail.

**Out of scope (for now):** SZ/SH Stock Connect venue adapters, quote/market
making, off-exchange Trade Capture reporting, and OBO cancel (designed but
disabled behind feature flags).


---

## 2. Locked Decisions

| Area | Decision |
| --- | --- |
| Language / runtime | Java 21 LTS, Maven |
| Upstream FIX | QuickFIX/J acceptor, FIX 5.0 SP2 (FIXT 1.1) |
| GSOPS transport | IBM MQ (JMS) |
| OCG-C codec | Generated from data dictionary (codegen) |
| OMD-C client | In-house, shared codegen |
| Real-time store + WAL | Chronicle Queue + in-memory maps, snapshots |
| Archive | Postgres (replaces Sybase) |
| Admin / monitoring | Spring Boot (off hot path only) |
| Availability | Active/standby per Comp ID |
| Reference data | OMD-C direct, full phase-aware |

### 2.1 Background: upstream sources

GFIX is the single FIX gateway and the **only direct upstream** to the OMS. RBP
and GSOPS both terminate into GFIX; the OMS never talks to them directly.

```
RBP  ──(native FIX 5.0 SP2)──┐
                              ├──▶ GFIX ──(native FIX 5.0 SP2)──▶ OMS
GSOPS ──(FIX via IBM MQ)─────┘
```

| Link | Transport | Notes |
| ---- | --------- | ----- |
| RBP → GFIX | Native FIX 5.0 SP2 (TCP) | Migrated/retail order-entry channel; session-based for latency + synchronous acks |
| GSOPS → GFIX | FIX over IBM MQ (JMS) | Ops/staff, non-migrated; durable, asynchronous delivery without a persistent FIX session |
| GFIX → OMS | Native FIX 5.0 SP2 (TCP) | Single consolidated FIX session |

### 2.2 What Fidessa does today (and what we must replace)

- **Price check** — validates order price/quantity against reference data and
  market controls before routing.
- **Queue management** — serialises order flow to the exchange session,
  respects message-rate entitlements, keeps one in-flight amend/cancel per
  order, and tracks the order chain.
- **Order lifecycle** — owns the order state machine and reconciles exchange
  Execution Reports back to the upstream FIX session.

---

## 3. Critical Clarification — OCG-C is Binary, not FIX

The HASE flow diagram draws `Fidessa -> HKEx (FIX)`. In reality the HKEX
**Orion Central Gateway – Securities Market (OCG-C)** interface is a **binary
protocol** with FIX-5.0-SP2-like *semantics* but a compact, bitmap-driven wire
format. There is no FIX message on the wire to HKEX.

Consequences for the design:

- The OMS terminates FIX **upstream only**.
- A dedicated **OCG-C Binary Gateway** component translates internal order
  events into OCG-C binary messages and decodes OCG-C responses back into
  internal/FIX events.
- A venue-abstraction layer lets a future Stock Connect (SZ/SH) or FIX-based
  venue be added without touching the core.

A second, equally important clarification: **OCG-C carries no market data or
market status**. Reference data, prices, price limits and trading state must be
consumed separately from **OMD-C** (Orion Market Data Platform — Securities
Market). See §5.

```
RBP/GFIX/GSOPS --FIX/MQ--> [ OMS core ] --OCG-C binary--> HKEX OCG-C --> OTP-C
                                  ^
                     OMD-C (market + reference + trading state)
```

---

## 4. System Context & Architecture

### 4.1 Context diagram

```
                       +-------------------------------------------------+
                       |                    OMS                          |
   +-----------+  FIX  |  +--------------+   +------------------------+   |
   |   RBP     |---+    |  | Upstream FIX |   |  Order Manager         |   |
   +-----------+   |    |  | Gateway      |-->|  (state machine,       |   |
                   |    |  | (QuickFIX/J  |   |   order repository,    |   |
   +-----------+ FIX/MQ|  |  acceptor)    |   |   ID allocator)        |   |
   |  GSOPS    |---+    |  +--------------+   +-----------+------------+   |
   +-----------+   |    |        ^  ^                   |                    |
                   |    |        |  | (GFIX is the sole |                    |
   +-----------+ FIX    |        |  |  FIX upstream)    |                    |
   |   GFIX    |-------|--------+  |                   |                    |
   +-----------+        |           |                   |                    |
                        |  +--------------+   +-------v------------+       |
                        |  | OMD-C Client |   |  Pre-Trade Risk    |       |
                        |  | (multicast)  |-->|  (price check)     |       |
                        |  +------+-------+   +-------+------------+       |
                        |         | Reference &      |                    |
                        |         | Price/State      |                    |
                        |         | Cache            |                    |
                        |         v                  |                    |
                        |  +--------------+   +-------v------------+       |
                        |  | Queue Manager|   | (one session FIFO, |       |
                        |  |              |   |  throttle, chain)  |       |
                        |  +--------------+   +-------+------------+       |
                        |                             |                    |
                        |                     +-------v------------+       |
                        |                     | OCG-C Binary       |       |
                        |                     | Gateway (Lookup,   |       |
                        |                     | Session, Codec,    |       |
                        |                     | Resend/Recovery)   |       |
                        |                     +-------+------------+       |
                        |                             |                    |
                        |        +--------------------v---------------+    |
                        |        | Chronicle WAL + snapshots | Postgres |    |
                        |        +--------------------------------------+    |
                        +-------------------------------------------------+
                                              | OCG-C binary (TCP/IP)
                                              v
                                     HKEX OCG-C  ->  OTP-C
```

### 4.2 Components

| Component | Responsibility |
| --------- | -------------- |
| **Upstream FIX Gateway** | QuickFIX/J acceptor for the single GFIX session. GFIX consolidates RBP (native FIX) and GSOPS (FIX over IBM MQ) upstream of the OMS. Session sequencing, validation, `ClOrdID` intake. |
| **Order Manager** | Authoritative order state machine, order/execution repository, ID allocation and chain tracking. |
| **OMD-C Client** | Connects to the OMD-C multicast/streaming feed; channel subscription; refresh/retransmission; decodes reference, price and status messages. |
| **Reference & Price/State Cache** | Per-security spread tables, tick sizes, reference price, VCM state, trading phase; phase-aware. |
| **Pre-Trade Risk (Price Check)** | Price-on-tick, price band (per phase), notional, max-qty and limit-price validation; honour `Execution` override instructions. |
| **Queue Manager** | Per-session outbound queue, MPS token-bucket throttle, one-in-flight amend/cancel enforcement, order-chain rules. |
| **OCG-C Binary Gateway** | Lookup Service client, session lifecycle (logon/heartbeat/logout), binary codec, sequence management, resend/recovery, cancel-on-disconnect. |
| **Persistence** | Chronicle Queue WAL + snapshots (hot, few trading days); async archive to Postgres for audit/reporting. |
| **Monitoring / Admin / Replay** | Dashboards, session state, order search, drop-copy replay, kill switch / mass cancel. |

---

## 5. Reference Data & Trading State (OMD-C)

OCG-C does not disseminate market data or market status. The OMS therefore
subscribes to **OMD-C** for everything the price check and state model need.

| Need | OMD-C message | Detail |
| ---- | ------------- | ------ |
| Tick size / spread table | `Security Definition (11)` → `Spread Table Code` | Codes 01/03/04/05/06 |
| Reference price (dynamic) | `Reference Price (43)` | Last automatch trade 5 min ago |
| Price band / VCM limits | `VCM Trigger (23)` | ±10/15/20% by index tier |
| Previous close (POS base) | `Closing Price (62)` | POS Stage-1 limit base |
| Nominal price | `Nominal Price (40)` | |
| Trading phase | `Trading Session Status (20)` | POS / CAS / CTS |
| Instrument state | `Security Status (21)` | halt, suspension, etc. |

Fallback: a static HKEX-published spread-table file loaded at startup, so tick
validation survives a market-data outage (the exchange's own reject codes
16/19/20/101/102 remain the ultimate backstop).

### 5.1 Phase-aware model

The Reference & Price/State Cache maintains, per security, the tuple
`{spread_table, reference_price, band, vcm_state, trading_phase}`. Price check
keys its validation off the current phase:

| Phase | Reference price | Rule |
| --- | --- | --- |
| POS | previous close | Stage 1 ±15%; Stage 2 best bid/ask; 9-times rule; short-sell tick rule |
| CAS | disseminated reference | Stage 1/Stage 2 structure |
| CTS | last automatch 5 min ago | VCM ±10/15/20%; cooling-off bands |
| No-cancellation / Random Matching / Blocking | — | amend/cancel not permitted |

---

## 6. OCG-C Binary Codec

### 6.1 Wire layout

```
Offset  Field                          Type                Bytes
0       Start of Message (STX 0x02)    UInt8                1
1       Length                         UInt16               2
3       Message Type                   UInt8                1
4       Sequence Number                UInt32               4
8       PossDup                        UInt8                1
9       PossResend                     UInt8                1
10      Comp ID                        Alphanumeric Fixed  12
22      Body Fields Presence Map       Bitmap Fixed        32  (256 bits)
54      ... body ...                   per presence map
        Checksum                       UInt32               4  (trailer)
```

- Header = 54 bytes fixed. `Length` covers header + body + trailer. The exact
  convention (whether STX and the length bytes are included) must be pinned via
  golden vectors — treat as a codec test-matrix item.
- Trailer `Checksum` = CRC32C, polynomial `0x1EDC6F41`, computed over
  **header + body only**, rendered as a 32-bit unsigned integer.

### 6.2 Presence map

- 32 bytes / 256 bits, **MSB-first**; bit position 0 = most-significant bit of
  byte 0. Fields are serialized in bit order.
- **Bit position is per message type** — a field can sit at a different position
  in different messages. The codegen must not share field→bit maps.
- Repeating blocks: `[count UInt16][nested presence map 2B][fields...]` repeated
  `count` times; nested blocks recurse.

### 6.3 Data-type gotchas

| Type | Gotcha |
| --- | --- |
| `Byte` | ASCII character, not a number: `Exec Type` `'0' '4' '5' '8' 'C' 'F' 'H' 'L' 'X' 'Y'`; `Gap Fill` `'N'/'Y'`; `Exchange Trade Type` `'M'…'O'`. |
| `Decimal` | 8-byte signed little-endian integer, ×10⁸ implied. |
| Integers | All `UInt*/Int*` little-endian. Exception: RSA ciphertext (Logon) is big-endian, then base64. |
| Alphanumeric fixed `n` | Null-terminated, length includes null; empty → first byte null. `Password`/`New Password` = 450 B. |
| Alphanumeric variable | 2-byte `UInt16` length prefix (includes null); empty → null at third byte. |
| `Transaction Time` | `YYYYMMDD-HH:MM:SS.ssssss`, UTC, alphanumeric fixed (25). |

### 6.4 Content-level validation (encode-time)

- `Client Order ID`: numeric 1..99,999,999, no leading zeros (initial launch).
- `Security ID` / `Broker ID` / `Trade Report ID`: leading zeros rejected.
- `Text` from client: ≤10 printable chars, no punctuation.
- `Side` = 1/2/5; `Order Type` = 1/2; `TIF` = 0/3/4/9.
- Message-type numbering has gaps (no 19/20); use the literal table.

### 6.5 Supported message types

| Type | Message | Type | Message |
| ---- | ------- | ---- | ------- |
| 0 | Heartbeat | 14 | Mass Cancel Request |
| 1 | Test Request | 15 | Order Mass Cancel Report |
| 2 | Resend Request | 16 | Quote |
| 3 | Reject | 17 | Quote Cancel |
| 4 | Sequence Reset | 18 | Quote Status Report |
| 5 | Logon | 21 | Trade Capture Report |
| 6 | Logout | 22 | Trade Capture Report Ack |
| 7 | Lookup Request | 23 | OBO Cancel Request |
| 8 | Lookup Response | 24 | OBO Mass Cancel Request |
| 9 | Business Message Reject | 25 | Throttle Entitlement Request |
| 10 | Execution Report | 26 | Throttle Entitlement Response |
| 11 | New Order | 27 | Party Entitlements Request |
| 12 | Amend Request | 28 | Party Entitlements Report |
| 13 | Cancel Request | | |

In-scope for HK Connect v1: `0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15` plus
`25/26` (throttle). Quote and Trade Capture messages are deferred but modelled.

---

## 7. OCG-C Session State Machine

### 7.1 Connection & timers

```
          +-- lookup loop: P-primary → P-mirror → B-primary → B-mirror → cycle
          |   (5s delay on failure)
          v
      CONNECTING --Lookup Request(7)--> Lookup Response(8): primary/secondary IP:port
          v
      LOGGING_IN --Logon(5)--> wait up to 60s for Logon reply
          |   (on reject/logout: reconnect delay 10s)
          v
      ACTIVE <-- Heartbeat(0) every 20s idle
          |   <-- Test Request(1) after 3 missed intervals (60s)
          |   <-- Logout if no reply ~3 intervals
          v
      LOGGING_OUT --Logout(6)--> await Logout reply (60s)
```

- Sequence numbers init to **1 each trading day**; starting >1 is dropped.
- Lookup messages always carry sequence 1, independent of session sequence.

### 7.2 Sequence reconciliation on Logon

Both sides track **Next Expected** (inbound) and **Next To Send** (outbound).
The client's `Next Expected Message Sequence` is mandatory and drives replay:

```
Client sends Logon(NextExpected = N); OCG-C compares to Next To Send (S):
    N > S  → Logout, terminate (manual intervention)
    N == S → resume from S
    N < S  → gap-fill from N up to the logon seq, skip logon via gap-fill
```

- Never issue a Resend Request for the Logon sequence itself.
- During a resend, a second Resend Request from the client terminates the
  session — coalesce resend requests client-side.

### 7.3 Recovery

- Gap (received > expected) → Resend Request; queue out-of-order messages;
  drop messages outside the requested range.
- Resend modes: single (`start==end`), range, all-after (`end=0`).
- Gap-fill skip list: `Logon, Logout, Heartbeat, Test Request, Resend Request,
  Sequence Reset`. Everything else is replayed.
- `PossDup=1` → same seq + content may have been sent; `PossResend=1` → same
  content under a different seq (dedupe by `Execution ID`).
- Sequence Reset: gap-fill `'Y'` (either side) vs reset `'N'` (**OCG-C only**).
  Client cannot reset sequence to 1 via Logon — that is a manual HKEX desk call.

### 7.4 Password lifecycle

RSA-2048 encrypt (PKCS#1/OAEP, big-endian → base64) the password prefixed with
UTC login time `YYYYMMDDHHMMSS`. Logon reply `Session Status` communicates
Active / Password-changed / invalid / locked / expired, etc. Policy: 8 chars,
letters+digits, change-on-first-login, not in last 5, ≤1 change/day, 3 bad
attempts → lock, 90-day expiry.

### 7.5 Cancel-on-Disconnect (CoD)

Optional, per Comp ID, fixed intraday. Triggered by 6 consecutive heartbeat
periods of silence or abrupt disconnect (not HKEX-internal outage). Cancellation
fires after a configurable delay — benchmark default **300 s** — and can't
execute during No-Cancellation / Random-Matching / etc. states. On reconnect,
reconcile cancelled orders via `Exec Restatement Reason = 104`.

---

## 8. Order Lifecycle & FIX Mapping

### 8.1 Internal state machine

```
 PENDING_NEW ──accept──▶ NEW ─────────────▶ PARTIALLY_FILLED ──▶ FILLED
    │                    │   │                     │
  reject             amend│   │cancel           full └──▶ FILLED
    ▼                    ▼   ▼
 REJECTED         PENDING_REPLACE  PENDING_CANCEL
                      │ ack/reject       │ ack/reject
                      ▼                  ▼
              NEW/PART_FILLED/FILLED  CANCELLED  EXPIRED
```

Model the OCG-C transient statuses (Pending New 10 / Pending Cancel 6 / Pending
Amend 14) and map to FIX `A`/`6`/`E`. Branch amend handling: price-change or
qty-increase amend = **cancel/replace** (two reports, new `Order ID`); otherwise
a single `Exec Type=5` report on the same `Order ID`. Trade Cancel (`H`) must
**not reinstate leaves quantity**.

### 8.2 Exec Type → FIX 5.0 SP2 mapping

| OCG-C Exec Type (Byte) | Internal event | FIX 5.0 SP2 |
| --- | --- | --- |
| `'0'` New | `NEW` | ExecType `0` / OrdStatus `0` |
| `'8'` Reject | `REJECTED` | ExecType `8` / OrdStatus `8` |
| `'C'` Expire | `EXPIRED` | ExecType `C` / OrdStatus `C` |
| `'F'` Trade | `FILL` | ExecType `F` / OrdStatus `1`/`2` |
| `'4'` Cancel | `CANCELLED` | ExecType `4` / OrdStatus `4` |
| `'5'` Amend | `AMENDED` | ExecType `5` / OrdStatus `0/1/2` |
| `'H'` Trade Cancel | `BUST` | ExecType `H` |
| `'X'` Cancel Reject | `CXL_REJECT` | `OrderCancelReject (9)` |
| `'Y'` Amend Reject | `AMEND_REJECT` | `OrderCancelReject (9)` |

- OCG-C has no standalone cancel/amend-reject message; synthesize FIX
  `OrderCancelReject (9)` from `'X'`/`'Y'` Execution Reports.
- Unsolicited cancels carry `Exec Restatement Reason`: 103 mass-cancel, 104
  cancel-on-disconnect, 105/106 broker/EP suspended, 107 system cancel, 8 market
  ops, 6 halt/VCM. Propagate distinct reasons upstream.

---

## 9. Queue Manager, Throttling, ID Chaining

### 9.1 In-flight exclusivity

One outstanding amend/cancel per order (mirrors OCG-C reject code 3). The Queue
Manager queues (preferred for amend chains) or locally rejects the second
request.

### 9.2 Client Order ID allocation & chaining

- Numeric 1..99,999,999, no leading zeros, unique per Submitting Broker ID per
  day. Every New/Amend/Cancel consumes a fresh ID; `Original Client Order ID`
  links to the prior. Chain capped at 99,999,999.

### 9.3 Throttle

Per-Comp-ID MPS via token bucket. Query entitlement at runtime via
`Throttle Entitlement Request/Response (25/26)` — never hardcode (default
placeholder 50 MPS). OCG-C answers over-rate with `Business Message Reject (9)`
code 8; shape locally so this never happens; alert on near-limit utilisation.

---

## 10. Active/Standby Failover

```
        PRIMARY (live session)                 STANDBY (warm)
        ┌────────────────────┐                ┌────────────────────┐
        │ nextExpected= N    │   replicate    │ snapshot + WAL tail│
        │ nextToSend = M     │◀───────────────│ (N, M, live orders)│
        └─────────┬──────────┘  (chronicle)   └────────────────────┘
                  │ crash
                  ▼
        STANDBY promotes → Lookup → connect primary, then secondary
             → Logon(NextExpected = N_last_processed)
             → OCG-C gap-fills [N..S]  (§7.2 N < S branch)
             → reconcile replayed Execution Reports
```

1. Standby replays the same Chronicle WAL, so it knows the last processed
   sequence; `Next Expected` in the failover Logon is read from WAL, not guessed.
2. Never auto-reset sequence client-side; surface "manual intervention" state.
3. Promotion must complete within the CoD delay (300 s benchmark) or reconcile
   the resulting cancel reports.
4. Standby is "warm" (state-replicated) but wire-inactive; HKEX terminates both
   connections on a second Logon for the same Comp ID while active.

---

## 11. Persistence & Concurrency

### 11.1 Chronicle + Postgres

- Chronicle Queue WAL per session + per order-event stream (append-only);
  single-threaded appender aligned to the per-session writer.
- Snapshots: periodic serialization of in-memory `order`/`session`/`throttle`
  maps + sequence numbers. Recovery = snapshot + WAL tail, then reconnect + gap-fill.
- Postgres: async projection of orders/executions/audit for reporting.

### 11.2 Threading model (single-writer everywhere)

| Boundary | Concurrency |
| --- | --- |
| OCG-C session I/O | 1 thread/session: read, decode, CRC32C, sequence, dispatch |
| Order Manager | striped executor keyed by internal order id |
| Queue Manager | 1 producer/session |
| Chronicle appender | aligned to session writer |
| FIX acceptor | QuickFIX/J per-session thread model |

---

## 12. Non-Functional Requirements

| Area | Target |
| --- | --- |
| Latency | Upstream FIX ack p99 < 5 ms; OCG-C encode/decode < 100 µs/message |
| Throughput | Sized to Comp ID MPS entitlement; no internal bottleneck below it |
| Availability | Active/standby across sites; 99.9%+ during trading |
| Determinism | Ordered, single-writer per order and per session |
| Recoverability | WAL replay + OCG-C gap-fill; no order loss on restart |
| Security | RSA logon, TLS upstream, RBAC, audit logging, PII protection |
| Observability | Structured logs, per-session metrics, order tracing, alerting |

---

## 13. Testing & Certification

| Tier | Approach | Needs HKEX? |
| --- | --- | --- |
| Golden-vector codec | fixtures from spec §6.2.1 / §8 (round-trip, CRC32C, endianness) | No |
| In-house mock OCG-C | Lookup + session simulator (self-generated test RSA key) | No |
| HKEX Offline Simulator | Phase-1 conformance (BSS vendor package) | Partial |
| E2E + Rollout | testing line + Comp ID | Yes |

Specific matrices: codec (presence-map MSB ordering, null-termination, empty
variable field, `Byte`-as-ASCII, `Decimal`×10⁸, endianness, CRC32C); session
(logon, seq>1 reject, `NextExpected` >/==/<, gap-fill, resend modes, PossDup/
PossResend, sequence-reset, heartbeat ladder, reconnect timers); order
(new/amend/cancel/mass-cancel, cancel/replace, trade-cancel no-reinstate,
unsolicited-cancel reasons, in-flight exclusivity); price check (tick, band
16/101/102, notional 20, ref-missing 19, qty 13, market-vs-limit); FIX mapping
(exact 5.0 SP2 output, `OrderCancelReject` synthesis, Pending → `A/6/E`);
failover (kill primary mid-order → standby gap-fill); chaos (disconnect
mid-resend, checksum failure, double-login termination).

---

## 14. Delivery Roadmap

| Phase | Scope |
| --- | --- |
| 0 | Codegen framework + OCG-C data dictionary + golden vectors |
| 1 | OCG-C session (Lookup, Logon, heartbeat, sequence/recovery) |
| 2 | Order Manager + upstream FIX/MQ + FIX↔OCG-C mapping |
| 3 | OMD-C client + phase-aware Reference/Price/State cache |
| 4 | Price Check engine (tick, per-phase band, notional, qty) |
| 5 | Queue manager, throttling, ID chaining, in-flight exclusivity |
| 6 | Persistence (Chronicle WAL/snapshot, Postgres archive) |
| 7 | Active/standby failover + CoD reconciliation |
| 8 | Test tiers 1–2 (mock OCG-C) → HKEX Offline Simulator → E2E → Rollout |
| 9 | Stock Connect (SZ/SH) venue adapter — deferred |

---

## 15. Risks & Open Questions

**Risks**

- Upstream FIX profile is an assumption (FIX 5.0 SP2) until real specs land.
- Exact OCG-C data dictionary must be encoded precisely; a codec generator
  driven by the official dictionary reduces risk.
- OMD-C is a second binary protocol with its own refresh/recovery model.
- HKEX certification timeline and simulator availability.

**Open questions (business/external only)**

- Comp ID / Submitting Broker ID allocation scheme.
- MPS entitlement and CoD delay values (benchmarked until HKEX confirms).
- IBM MQ queue names and guarantees for GSOPS.
- Retention windows (Chronicle hot-store days vs Postgres archive period).
- `Length` field convention — pinned via golden vectors.

---

## 16. Glossary

| Term | Meaning |
| ---- | ------- |
| OCG-C | Orion Central Gateway – Securities Market (HKEX) |
| OTP-C | HKEX securities trading system behind OCG-C |
| OMD-C | Orion Market Data Platform – Securities Market (HKEX) |
| BSS | Broker Supplied System |
| Comp ID | Unique HKEX session identifier |
| MPS | Messages per second (throttle entitlement) |
| CoD | Cancel on Disconnect |
| ClOrdID | Client Order ID |
| TIF | Time In Force |
| POS / CAS / CTS | Pre-Opening / Closing Auction / Continuous Trading Session |
| VCM | Volatility Control Mechanism |
| WAL | Write-ahead log |
