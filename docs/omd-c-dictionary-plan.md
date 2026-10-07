# OMD-C Data Dictionary — sourcing, structure & codegen plan

> **Status: prototype / draft.** Derived from the **public** HKEX interface
> specification. **Licence caveat:** the spec is governed by an HKEX *Market Data
> Vendor / End-User / ASP Licence Agreement*; production use and connection
> credentials require the licence (via HASE as EP, or a BSS vendor). Confirm
> before relying on this in production.

## 1. Sourcing

- **Spec:** *HKEX Orion Market Data Platform – Securities Market & Index Datafeed
  Products (OMD-C) Binary Interface Specifications*, **v1.45** (dated 2026-01-09),
  published on `hkex.com.hk` (Market Data Services → Infrastructure → OMD-C).
  Companions: **OMD-C Developers Guide**, **OMD-C Connectivity Guide**.
- **OCG-C:** *Interface Specifications HKEX Orion Central Gateway – Securities
  Market (OCG-C) Binary Trading Protocol* (v3.1/v3.2) is also public.
- **Operational bits** (multicast IP/ports, credentials) are circulated
  "through a separate medium" via HKEX/EP onboarding — not in the public PDF.
- **BSS vendors:** HKEX publishes a *List of Broker Supplied Systems (BSS)
  Vendors*; they can supply the package + **HKEX Offline Simulator** (Phase 8).
- **Decision:** OMD-C primary; Refinitiv deferred (`DESIGN.md` §5).

## 2. Wire structure (spec §3.2–§3.3)

```
Packet:
  PktSize   Uint16  2   size of packet (incl. this field)
  MsgCount  Uint8   1   number of messages in packet
  Filler    String  1
  SeqNum    Uint32  4   sequence number of first message in packet
  ...                   (packet header continues)
Message (each, sequential in packet):
  MsgSize   Uint16  2   size of message (incl. header)
  MsgType   Uint16  2   message type code
  ...                   fields at fixed offsets
```

- **Data types:** `Uint8/16/32/64`, `Int16/32/64` — **little-endian**; `String`
  ASCII left-aligned, space-padded; `Binary` Unicode (Chinese). Prices carry an
  **implied decimal** count (e.g. 3 for closing price).
- **Control/retransmission:** Heartbeat, Sequence Reset (100), DR Signal (105),
  Logon (101), Logon Response (102), Retransmission Request (201)/Response (202),
  Refresh Complete (203). This is a **second binary protocol** with its own
  recovery model (`DESIGN.md` §15).

## 3. Message catalogue (subset we consume)

| Message | Type | Drives |
| --- | --- | --- |
| Security Definition | 11 | spread table code, market code, instrument/product type |
| Trading Session Status | 20 | session + **sub-period** (`TradingSessionSubID`) |
| Security Status | 21 | halt / suspension |
| VCM Trigger | 23 | VCM state / band |
| Nominal Price | 40 | nominal price (CTS reference median input) |
| Indicative Equilibrium Price | 41 | IEP (POS/CAS) |
| Reference Price | 43 | reference price |
| Closing Price | 62 | previous close (POS Stage-1 base) |
| Aggregate Order Book Update | 53 | top of book (Stage-2 capture) |
| Add / Modify / Delete Order | 30/31/32 | order book |

> Spec note (§3.9): *"The full order book information is not available in
> Pre-Opening Auction Session and Closing Auction Session."* → the Stage-2
> top-of-book must be **captured before** the auction no-cancellation period,
> consistent with "recorded at the end of order input".

## 4. Extracted field tables (subset)

### Security Definition (11) — total length ≥ 32+

| Offset | Field | Format | Len | Notes |
| --- | --- | --- | --- | --- |
| 0 | MsgSize | Uint16 | 2 | |
| 2 | MsgType | Uint16 | 2 | = 11 |
| 4 | SecurityCode | Uint32 | 4 | 5-digit code, 1..99999 |
| 8 | MarketCode | String | 4 | MAIN / GEM / NASD / ETS |
| 12 | ISINCode | String | 12 | |
| 24 | InstrumentType | String | 4 | BOND / EQTY / TRST / WRNT |
| 28 | ProductType | Uint8 | 1 | 1 ordinary, 2 pref, … 3 DW, 11 CBBC, 99 other |
| 29 | Filler | String | 1 | |
| 30 | SpreadTableCode | String | 2 | 01 / 03 / 04 / 05 / 06 … |
| … | (more fields follow, e.g. FreeText) | | | |

### Trading Session Status (20)

| Offset | Field | Format | Len | Notes |
| --- | --- | --- | --- | --- |
| 0 | MsgSize | Uint16 | 2 | |
| 2 | MsgType | Uint16 | 2 | = 20 |
| 4 | MarketCode | String | 4 | MAIN / GEM / NASD / ETS |
| 8 | Filler | String | 1 | |
| 9 | TradingSessionSubID | Uint8 | 1 | see below |

`TradingSessionSubID` values (from the spec — maps directly to our sub-period model):

```
100 Not Yet Open (NO)
POS:  1 Order Input (OI)   101 No Cancellation (NW)   108 Random Matching (RM)
      2 Order Matching (MA)  7 Blocking (BL)
CTS:  3 Continuous Trading (CT)
CAS:  105 Reference Price Fixing (RP)   5 Order Input (OI)
      106 No Cancellation (NW)  107 Random Close (RC)   4 Order Matching (MA)
Other: 102 Exchange Intervention …
```

### Security Status (21)

| Offset | Field | Format | Len | Notes |
| --- | --- | --- | --- | --- |
| 0 | MsgSize | Uint16 | 2 | |
| 2 | MsgType | Uint16 | 2 | = 21 |
| 4 | SecurityCode | Uint32 | 4 | 1..99999 |
| 8 | SuspensionIndicator | Uint8 | 1 | 2 = Trading Halt/Suspend, 3 = Resume |
| 9 | Filler | String | 3 | |

### Closing Price (62) — total length 16

| Offset | Field | Format | Len | Notes |
| --- | --- | --- | --- | --- |
| 0 | MsgSize | Uint16 | 2 | |
| 2 | MsgType | Uint16 | 2 | = 62 |
| 4 | SecurityCode | Uint32 | 4 | 1..99999 |
| 8 | ClosingPrice | Int32 | 4 | current-day close, **3 implied decimals** |
| 12 | NumberOfTrades | Uint32 | 4 | |

*(Reference Price (43), Nominal Price (40), Indicative Equilibrium Price (41),
VCM Trigger (23) and the order-book messages (30/31/32/53) are catalogued but not
yet field-extracted — continue from the same spec sections.)*

## 5. Codegen / implementation plan

1. **Dictionary:** add `dictionary/omd-c/fields.yaml` + `messages.yaml` (mirror the
   OCG-C shape), starting with the subset above.
2. **Codegen:** extend `codegen/` to emit OMD-C enums/types (e.g.
   `MarketCode`, `InstrumentType`, `ProductType`, `TradingSessionSubID`), reusing
   the existing generator framework.
3. **Codec:** new module `oms-omd` (or extend `oms-codec`) with an `OmdCodec`
   (packet header + message framing + field decode), analogous to `MessageCodec`.
   Byte-exact → golden vectors + round-trip tests (AGENTS.md).
4. **Client:** `OmdClient` — multicast join, packet decode, retransmission
   (201/202) + refresh (203) recovery; publishes `ReferenceUpdate`s into
   `PhaseAwareReferenceCache`.
5. **Mapping OMD-C → `ReferenceUpdate`:**
   `11 → SecurityDefinition`, `43 → ReferencePrice`, `23 → Vcm/Band`,
   `40/41 → NominalPrice`, `62 → previousClose`, `20 → MarketPhase(session,
   subPeriod)`, `21 → InstrumentStateChange`, `30/53 → BestBidAsk`.
6. **Phase model:** map `TradingSessionSubID` to `(session, subPeriod)` and use it
   for the Stage-1/Stage-2 / blocking rules (Phase 4).

## 6. Action required (licensing)

Confirm HASE (as EP / market-data licensee) holds or authorises use of the OMD-C
spec for this build, and obtain the current spec + feed credentials via HKEX
onboarding or a BSS vendor. Tracked in Linear `ALE-77`.
