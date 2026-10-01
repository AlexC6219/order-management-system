# HKEX Connect Binary Trading OMS

Order Management System for the HASE stock trading flow. It replaces the
incumbent **Fidessa** system: it ingests orders from upstream channels over
FIX/MQ, performs **price checking** and **queue management**, and routes orders
to **HKEX** via the **OCG-C Binary Trading Protocol**.

> **Current scope:** Hong Kong Connect (HKEX securities, `XHKG`). Shenzhen and
> Shanghai Stock Connect are designed for but deferred.

## Important note on the venue protocol

The HASE flow diagram shows `Fidessa -> HKEx (FIX)`, but HKEX's OCG-C interface
is a **proprietary binary protocol**, not FIX. FIX is used **upstream only**.
The OMS therefore includes a dedicated OCG-C Binary Gateway that translates
internal order events to/from the binary wire format.

## Components

| Component | Responsibility |
| --------- | -------------- |
| Upstream FIX Gateway | FIX acceptor for RBP/GFIX; MQ consumer for GSOPS |
| Order Manager | Order state machine, repository, ID allocation/chaining |
| Reference & Price Cache | Instrument static data, tick tables, price bands, reference prices |
| Pre-Trade Risk | Price-on-tick, price band, notional and quantity checks |
| Queue Manager | Per-session outbound queue, MPS throttling, in-flight exclusivity |
| OCG-C Binary Gateway | Lookup Service, session lifecycle, binary codec, recovery |
| Persistence | In-memory real-time store + WAL; Sybase archive/audit |

## Full design

See [DESIGN.md](./DESIGN.md) for the complete architecture, message flows, FIX ↔
OCG-C field mappings, OCG-C binary codec notes, session/recovery rules, data
model, roadmap and open questions.

## Status

Planning/design phase. No source code yet.
