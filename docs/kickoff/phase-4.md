# Kickoff — Phase 4 (Pre-Trade Risk / Price Check)

Paste the block below into a fresh agent session to start Phase 4. It is
self-contained: it carries the project context, architecture, current state,
decisions, and next steps. Nothing from prior chat history is needed.

---

```
You are an agent working on the HKEX Connect Binary Trading OMS in
/home/alex/order-management-system (Java 21 + Maven multi-module).

=====================================================================
0. WHAT THIS SYSTEM IS
=====================================================================
An Order Management System (OMS) replacing Fidessa for the HASE stock trading
flow (Hong Kong Connect, XHKG). It ingests orders over FIX 5.0 SP2 from GFIX
(the single upstream gateway), applies pre-trade price checks and queue
management, and routes orders to HKEX over the OCG-C BINARY trading protocol.

Two crucial facts:
- HKEX's venue interface is BINARY, not FIX. FIX is upstream only.
- OCG-C carries NO market data. Reference/market data comes separately from
  OMD-C (Orion Market Data Platform). Best bid/ask, reference price, bands and
  trading phase all come from OMD-C.

=====================================================================
1. STARTUP — DO THIS FIRST, IN ORDER
=====================================================================
1. Read AGENTS.md (auto-loaded) — conventions, build/test, toolchain, git rules.
2. Read PROGRESS.md — live status, phase table, next actions, open questions.
3. Read DESIGN.md §2 (locked decisions), §5/§5.1 (reference data + phase-aware
   model), §6 (codec), §7 (session), §8 (order lifecycle + FIX mapping).
   UR.md §5 (phase-aware price checking). TESTING.md §2 (mocks),
   TEST_PLAN.md L4 (price-check tests).
4. Run `mvn -q test` and confirm it is green. Expect 114 tests
   (30 codec + 28 session + 34 order + 15 reference + 7 fix).
5. Skim the code in oms-reference/ and oms-order/ (you will extend/consume it).

=====================================================================
2. MODULES (dependencies point DOWNWARD; siblings do not depend on each other)
=====================================================================
  codegen            generates oms-codec enums from dictionary/*.yaml
  oms-codec          OCG-C binary codec (Wire, Crc32c, Dictionary, Message,
                     MessageCodec) + 52 enums. Byte-exact; do not hand-edit.
  oms-session        OCG-C session: LookupClient, PasswordCipher (RSA/OAEP),
                     SessionEngine (heartbeat ladder, sequence reconciliation,
                     resend/gap-fill, dedupe), SessionMessages.
  oms-order          Order domain (OrderState, OrderEvent, Order,
                     OrderStateMachine, OrderRepository,
                     ClientOrderIdAllocator, IngressValidator,
                     ExecutionConsistency), mapping (ExecTypeMapper,
                     OrderTranslator, TransactionTime), OrderManager.
  oms-reference      Phase-aware reference cache (TradingPhase, VcmState,
                     InstrumentState, PriceBand, SpreadTable,
                     SecurityReference, ReferenceUpdate,
                     PhaseAwareReferenceCache, ReferenceDataSource,
                     ScriptedReferenceDataSource, StaticSpreadTable).
  oms-fix            QuickFIX/J 2.3.1 adapter (FixMapping, FixOrderApplication,
                     FixAcceptor, FixOrderHandler, FixTags).
  oms-test-harness   Mock OCG-C server (reusable; depends only on oms-codec).

Dependency arrows:
  oms-fix -> oms-order -> oms-codec
  oms-fix -> (quickfixj)
  oms-session -> oms-codec   (test -> oms-test-harness)
  oms-reference -> (nothing; standalone)

Decoupling pattern used throughout: collaborators are injected as narrow
functional interfaces (e.g. OrderManager takes Consumer<Message> outbound;
SessionEngine takes Consumer<byte[]> sink + Clock). Keep it that way.

=====================================================================
3. CURRENT STATE (as of the last session)
=====================================================================
Phase status:
  0 OCG-C codec + dictionary + codegen ............ DONE (30 tests)
  1 OCG-C session + Mock OCG-C ................... DONE (28 tests)
  2 Order flow + FIX 5.0 SP2 + FIX<->OCG-C ....... DONE (34 tests)
  3 Reference data (source-agnostic cache) ....... DONE (15 tests)
  4 Pre-Trade Risk (price check) ................. NEXT  <-- your task
  5 Queue management ............................. todo
  6 Persistence (Chronicle WAL + Postgres) ....... todo
  7 Active/standby failover + CoD ................ todo
  8 Testing & HKEX certification ................. todo
  9 Stock Connect (SZ/SH) ........................ deferred

Git: branch `master`; last commits:
  4de782e docs(omd-c): dictionary sourcing plan + prototype subset (LOCAL ONLY,
          push to GitHub failed with a transient server error — retry later)
  2edd25c docs(price-check): trading-phase rules + Phase 4 kickoff
  794a336 feat(order): order flow, FIX acceptor, FIX<->OCG-C mapping (PR #1)
  63e6dfa feat(session): OCG-C session + Mock OCG-C

Repo remote: git@github.com:AlexC6219/order-management-system.git

=====================================================================
4. LOCKED DECISIONS (DESIGN.md §2)
=====================================================================
Java 21 + Maven · QuickFIX/J (upstream FIX 5.0 SP2) · IBM MQ (GSOPS via GFIX) ·
codegen OCG-C codec (source of truth = dictionary/*.yaml) · OMD-C reference data
(direct) · Chronicle WAL · Postgres archive · active/standby · orders-only v1
(Quote/TradeCapture/OBO feature-flagged) · CoD 300s · Spring Boot = admin-only,
NOT in the hot path.

Reference-data source decision: OMD-C PRIMARY; Refinitiv (LSEG) deferred. The
reference layer is source-agnostic (ReferenceDataSource) so a Refinitiv adapter
could be added later.

=====================================================================
5. YOUR TASK — PHASE 4: PRE-TRADE RISK (PRICE CHECK)
=====================================================================
Linear parent ALE-11. Goal: validate an order/amend against reference data
BEFORE routing, so bad orders never consume OCG-C throttle (DESIGN.md §4.2,
§13; UR.md FR-3). On failure: reject LOCALLY and surface a FIX reject — with NO
OCG-C round-trip. Acceptance: TEST_PLAN.md L4 tests pass.

5a. PREREQUISITE — extend oms-reference (Option A).
oms-reference currently lacks: previousClose, nominalPrice, bestBid/bestAsk, and
a session + sub-period model. Extend ReferenceUpdate + SecurityReference +
PhaseAwareReferenceCache to carry them, and capture the Stage-2 band at the
order-input boundary. KEEP the "Stage 1 / Stage 2" names (industry standard).

5b. TRADING SESSIONS, SUB-PERIODS AND PRICE LIMITS (DESIGN.md §5.1)
"Stage 1 / Stage 2" are price-limit REGIMES, not time periods.

Pre-Opening Session (POS):
  09:00-09:15 Order input     Stage 1: +/-15% of previous close   input/cancel/amend
  09:15-09:20 No-cancellation Stage 2: within [highest bid, lowest ask] recorded
                              at the END of order input           input only
  09:20-09:22 Random matching Stage 2 (same frozen band)          input only
  09:22-09:30 Blocking        no input; unfilled at-auction limit orders carry to CTS
  Order types: at-auction order, at-auction limit order.

Continuous Trading Session (CTS):
  09:30-12:00, 13:00-16:00  Reference price = median of 5 snapshot nominal prices
  in the last minutes of CTS; VCM +/-10/15/20% by index tier; cooling-off bands.
  input/cancel/amend.

Closing Auction Session (CAS):
  16:00-16:01 Ref price fixing  no input/cancel/amend (carry forward if within limit)
  16:01-16:06 Order input       Stage 1: +/-5% of reference price   input/cancel/amend
  16:06-16:08 No-cancellation   Stage 2: within [highest bid, lowest ask] captured
                                at 16:06                            input only
  16:08-16:10 Random closing    Stage 2 (same frozen band)          input only
  Order types: at-auction order, at-auction limit order.

5c. ADDITIONAL CONTROLS
  9-times rule (CTS): reject any order whose price deviates by 9x or more from the
    current Nominal Price (or Previous Close). E.g. nominal 10.00 => buy >= 90.00
    and sell <= 1.11 are rejected.
  Short-sell rules (Side = 5 SellShort):
    POS/CAS: at-auction limit orders only (pure at-auction orders rejected).
    POS: exempt from traditional tick rule (single-price auction) but must satisfy
      the +/-15% POS limit.
    CTS: traditional tick rule applies (price cannot be below the current best ask).
    CAS: short-sale price cannot be lower than the CAS reference price (fixed 16:01).
    Exemptions: market-maker/hedging (POS/CTS); designated index arbitrage,
      stock-futures hedging, options hedging (CAS).

5d. CHECKS (TEST_PLAN.md L4)
  on-tick (spread table); price band (OCG codes 16/101/102); reference-present
  (19); notional (20); quantity (13); market-vs-limit price rule. Phase-aware rule
  selection; VCM cooling-off; no-cancellation blocks amend/cancel.

5e. OVERRIDE
  Execution Instructions (entitlement-gated) may skip price checks and/or notional.

5f. INTEGRATION SEAM (recommended)
  New module `oms-risk` (depends on oms-order + oms-reference). A composition /
  intake layer calls PriceCheck BEFORE OrderManager.submit/amend; on reject it
  builds a FIX reject (ExecutionReport for new; OrderCancelReject for amend) and
  sends it upstream — the order never reaches OCG-C. Keep oms-order free of
  oms-reference and QuickFIX.

5g. LOCAL REJECT -> FIX
  Map local rejects to FIX OrdRejReason (e.g. off-tick -> 17 Invalid price
  increment; band -> 16; ref-missing -> 19; notional -> 20; qty -> 13).

=====================================================================
6. OMD-C REFERENCE DATA — STATUS
=====================================================================
The OMD-C dictionary is PUBLICLY published: "HKEX Orion Market Data Platform -
Securities Market & Index Datafeed Products (OMD-C) Binary Interface
Specifications" v1.45 (2026-01-09) on hkex.com.hk. A prototype dictionary subset
and a plan are in:
  docs/omd-c-dictionary-plan.md
  dictionary/omd-c/{fields,messages}.yaml
Useful extracts: packet header PktSize/MsgCount/Filler/SeqNum; per-message
MsgSize/MsgType; little-endian; implied decimals; TradingSessionSubID values map
directly to the POS/CTS/CAS sub-periods above; the full order book is NOT
available during POS/CAS auctions (so the Stage-2 top-of-book must be captured
before the no-cancellation period).

BLOCKER = LICENSING (not availability): the spec is under the HKEX Market Data
Vendor/End-User/ASP Licence Agreement. Production use + feed credentials need
HASE's EP/market-data licence or a BSS vendor. Tracked in Linear ALE-77.
For Phase 4, use the MOCK source (ScriptedReferenceDataSource) + fixtures.

=====================================================================
7. OPEN DECISIONS TO CONFIRM WITH THE USER
=====================================================================
- Market-order notional: use reference price, or skip for market orders?
- Which sessions permit market orders?
- Override source: FIX ExecInst (18) vs OCG-C executionInstructions.
- Notional / max-qty thresholds (benchmark placeholders).
- Stage-2 capture: cache captures the band automatically on the
  order-input -> no-cancellation transition, vs an explicit published update.
- Short-sell exemption entitlement source (TBD).

=====================================================================
8. CONVENTIONS / CONSTRAINTS
=====================================================================
- Do not hand-edit generated code; change codegen/ and regenerate.
- Any codec change needs a round-trip test + a golden vector.
- Design docs are the source of truth; if behaviour and DESIGN.md disagree, stop
  and resolve first.
- Match existing style; prefer clarity.
- Keep the trading path plain Java (no Spring on the hot path).
- Only commit when asked. Conventional Commits. Stage explicitly (no target/).
- Modules must not create dependency cycles.

=====================================================================
9. WHEN DONE (session discipline)
=====================================================================
- `mvn -q test` green.
- Update PROGRESS.md (current phase -> Phase 5; last session's work; next actions).
- Commit with a Conventional Commit message (e.g. `feat(price-check): ...`).
- Reflect status in Linear (ALE-11 + children).
- If a Phase 5 kickoff is needed, add docs/kickoff/phase-5.md.

=====================================================================
10. LINEAR
=====================================================================
Project "HKEX Connect Binary Trading OMS", team ALE. Phase parents: ALE-5 (Docs),
ALE-6 (Phase 0) ... ALE-12 (Phase 9), ALE-7 (Testing). Label `agent-added` marks
agent-proposed scope. Linear is the backlog; PROGRESS.md + git are the source of
truth for current state.
```

---

## Reference (not part of the paste block)

- Full phase rules: `DESIGN.md` §5.1, `UR.md` §5.
- OMD-C plan: `docs/omd-c-dictionary-plan.md`.
- Test plan: `TEST_PLAN.md` L4; testing strategy: `TESTING.md` §2.
- Open questions / risks: `PROGRESS.md` → "Open questions / risks".
