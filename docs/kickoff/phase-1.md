# Kickoff — Phase 1 (OCG-C Session)

Paste the block below into a fresh agent session to start Phase 1.

> One file per phase under `docs/kickoff/`. Keep them self-contained so a new
> session needs nothing from prior chat history.

---

```
You are working on the HKEX Connect Binary Trading OMS in
/home/alex/order-management-system.

STARTUP (do this first, in order):
1. Read AGENTS.md (auto-loaded) and then PROGRESS.md for live status, next
   actions and open questions.
2. Confirm the current phase in PROGRESS.md. Phase 0 (OCG-C codec) is done with
   30 tests green; Phase 1 is next.
3. Read DESIGN.md §7 (OCG-C session state machine), TESTING.md §1.1 (Mock OCG-C),
   TEST_PLAN.md L2, and the OCGC spec §3–§5 (in "OCGC spec.md").
4. Run `mvn -q test` to confirm the starting point is green (expect 30 tests).

TASK: Implement Phase 1 — OCG-C Session.

Scope (Linear parent ALE-8):
- Lookup Service client (ALE-29): primary→mirror→backup-primary→backup-mirror→
  cycle, 5s retry; Lookup Request(7)/Response(8), seq always 1.
- Logon + RSA password (ALE-28): RSA-2048 PKCS#1/OAEP, big-endian→base64, UTC
  login-time prefix (YYYYMMDDHHMMSS); New Password; Session Status handling.
- Heartbeat / Test-Request ladder (ALE-35): 20s idle heartbeat, Test Request
  after 3 missed intervals, Logout after ~3 more.
- Sequence tracking + recovery (ALE-36): Next Expected / Next To Send (start 1);
  Logon reconciliation N>S / N==S / N<S; seq < expected → Logout unless PossDup.
- Resend / gap-fill (ALE-40): single/range/all-after; gap-fill skip list;
  PossDup vs PossResend (dedupe by Execution ID).
- Logout (ALE-48): request/reply, 60s timeout.
- Mock OCG-C server (ALE-43): TCP server on the existing codegen'd codec, with a
  self-generated RSA-2048 test keypair, sequence tracking and a scripted
  Execution Report replay + fault injection.

Acceptance: the TEST_PLAN.md L2 tests pass, driven by the Mock OCG-C server.

CONSTRAINTS:
- Build on the existing codec (oms-codec); do not hand-edit generated code
  (change codegen/ and regenerate).
- Any codec change needs a round-trip test plus a golden vector.
- Keep the Mock OCG-C server usable by later phases (it is the test harness).

WHEN DONE (session discipline):
- `mvn -q test` green.
- Update PROGRESS.md (current phase → Phase 2; last session's work; next actions).
- Commit with a Conventional Commit message (e.g. `feat(session): ...`).
- Reflect status in Linear (mark ALE-28/29/35/36/40/43/48 as done as appropriate).

If the working tree is dirty or tests are red, stop and report before starting.
```
