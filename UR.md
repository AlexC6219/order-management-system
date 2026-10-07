# User & Business Requirements — HKEX Connect Binary Trading OMS

Replacement for the incumbent **Fidessa** OMS for the HASE stock trading flow.

> Current scope: **Hong Kong Connect** (HKEX securities, `Security Exchange =
> XHKG`). Shenzhen / Shanghai Stock Connect designed but deferred.

---

## 1. Goal

Provide a low-latency, session-oriented Order Management System that accepts
order flow from upstream broker channels, applies pre-trade controls, and routes
orders to HKEX via the **OCG-C Binary Trading Protocol** — replacing Fidessa's
price-check and queue-management functions.

## 2. Scope

### 2.1 In scope (v1)

- **Order handling**: New Order, Amend, Cancel, Mass Cancel, plus Execution
  Reports, Session Rejects, Business Message Rejects.
- **Upstream ingress**: a single consolidated FIX 5.0 SP2 session from GFIX, which
  terminates RBP (native FIX) and GSOPS (FIX over IBM MQ) upstream of the OMS.
- **Pre-trade price checking**: tick size, price band, notional and quantity
  validation, phase-aware (see §5).
- **Queue management**: per-session outbound queue, message-rate throttling,
  in-flight exclusivity, order ID chaining.
- **OCG-C session**: Lookup Service, Logon/heartbeat/Logout, sequence
  management and recovery, Cancel-on-Disconnect reconciliation.
- **Reference data**: consumed from OMD-C (direct) with a static spread-table
  file as fallback.

### 2.2 Deferred (feature-flagged)

Quote / market-making (`Quote`, `Quote Cancel`, `Quote Status Report`),
off-exchange `Trade Capture Report` / `Trade Capture Report Ack`, and On-Behalf-Of
cancel. These message types are still modelled in the codec and data dictionary.

## 3. Functional requirements

| # | Requirement |
| --- | --- |
| FR-1 | Accept FIX 5.0 SP2 order messages (NewOrderSingle, OrderCancelRequest, OrderCancelReplaceRequest) and return ExecutionReport / OrderCancelReject. |
| FR-2 | Accept a single consolidated FIX 5.0 SP2 session from GFIX (which ingests RBP natively and GSOPS over IBM MQ). |
| FR-3 | Validate order price/quantity against reference data before routing. |
| FR-4 | Allocate OCG `Client Order ID` (numeric 1..99,999,999, no leading zeros, unique per Submitting Broker ID per day) and track the order chain. |
| FR-5 | Serialise outbound order flow per OCG-C session with message-rate throttling. |
| FR-6 | Enforce one in-flight amend/cancel per order. |
| FR-7 | Translate internal order events to OCG-C binary messages and decode OCG-C responses back to internal/FIX events. |
| FR-8 | Reconcile Execution Reports against the internal order state machine. |
| FR-9 | Recover session sequence numbers and missed messages per OCG-C recovery rules. |
| FR-10 | Support Cancel-on-Disconnect reconciliation on reconnect. |
| FR-11 | Persist a durable audit trail of orders/executions to Postgres. |

## 4. Non-functional requirements

| Area | Target |
| --- | --- |
| Latency | Upstream FIX ack p99 < 5 ms; OCG-C encode/decode < 100 µs/message. |
| Availability | Active/standby across sites; 99.9%+ during trading. |
| Determinism | Ordered, single-writer processing per order and per session. |
| Recoverability | WAL replay + OCG-C gap-fill; no order loss on restart. |
| Security | RSA-encrypted OCG-C Logon, TLS on upstream FIX, RBAC, audit logging. |
| Observability | Structured logs, per-session metrics, order tracing, alerting. |

## 5. Phase-aware price checking

Price validation must be keyed off the current trading **session** and
**sub-period** (source: OMD-C). Full timings and rules: `DESIGN.md` §5.1.

| Session | Reference price | Price limit | Amend/cancel |
| --- | --- | --- | --- |
| POS (Pre-Opening) | previous close | Stage 1 ±15% (order input); Stage 2 within [highest bid, lowest ask] (no-cancellation, random matching) | allowed in order input only |
| CTS (Continuous) | median of 5 nominal snapshots | VCM ±10/15/20% by index tier; cooling-off bands | allowed |
| CAS (Closing Auction) | fixed reference | Stage 1 ±5% (order input); Stage 2 within [highest bid, lowest ask] (no-cancellation, random closing) | allowed in order input only |

Order types in POS/CAS: at-auction order, at-auction limit order. The reference
price-fixing sub-period (CAS) and blocking sub-period (POS) allow no input.

Additional controls:

- **9-times rule (CTS):** reject any order whose price deviates by 9× or more
  from the current Nominal Price (or Previous Close) — e.g. nominal 10.00 ⇒ buy
  ≥ 90.00 and sell ≤ 1.11 are rejected.
- **Short-sell rules** (`Side = 5`): POS/CAS allow only at-auction limit orders
  (pure at-auction orders rejected); POS is exempt from the traditional tick rule
  but must satisfy the ±15% POS limit; CTS applies the traditional tick rule (not
  below best ask); CAS short-sale price cannot be below the CAS reference price
  (fixed at 16:01). Exemptions: market-maker/hedging (POS/CTS); designated index
  arbitrage, stock-futures and options hedging (CAS).

## 6. Key configuration (external / benchmarked)

| Item | Value |
| --- | --- |
| Cancel-on-Disconnect delay | 300 s (configurable) |
| Message rate (MPS) | external config; query via Throttle Entitlement Request/Response |
| Heartbeat interval | 20 s |
| CoD trigger | 6 consecutive heartbeat periods, or abrupt disconnect |

## 7. Decisions (locked)

| Area | Decision |
| --- | --- |
| Language / runtime | Java 21 LTS, Maven |
| Upstream FIX | QuickFIX/J acceptor for GFIX, FIX 5.0 SP2 |
| GSOPS transport | FIX over IBM MQ (JMS), terminated at GFIX |
| OCG-C codec | generated from data dictionary |
| OMD-C client | in-house, shared codegen |
| Real-time store + WAL | Chronicle Queue |
| Archive | Postgres |
| Availability | Active/standby |
| Admin / monitoring | Spring Boot (off hot path) |

> **Reference-data source (2026-10-06):** OMD-C primary; Refinitiv (LSEG)
> deferred for HKEX pre-trade. The reference layer is source-agnostic so a
> Refinitiv adapter can be added later. See `DESIGN.md` §5.
