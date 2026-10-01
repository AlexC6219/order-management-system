# AGENTS.md

Instructions for opencode (and any other coding agent) working in this
repository. opencode loads this file automatically as project context.

## Project

**HKEX Connect Binary Trading OMS** — an Order Management System for the HASE
stock trading flow. It ingests orders over FIX/MQ, runs pre-trade price checks
and queue management, and routes orders to HKEX over the **OCG-C Binary Trading
Protocol**.

- Current scope: Hong Kong Connect (`XHKG`) only. Shenzhen/Shanghai deferred.
- HKEX's venue interface is **binary, not FIX**. FIX is upstream only.
- Start with `README.md` (components), then `DESIGN.md` (architecture, field
  maps, codec/session/recovery rules). `OCGC spec.md` is the authoritative wire
  spec.

## Where we are — READ THIS FIRST

- **Current phase:** Phase 1 — OCG-C Session. Phase 0 (codec) is done, 30 tests
  green.
- **Resume point:** read `PROGRESS.md` for live status, next actions, and open
  questions **before doing anything else**.
- **Plan / backlog:** Linear project *HKEX Connect Binary Trading OMS* (team
  `ALE`). Treat Linear as the backlog; `PROGRESS.md` + git are the source of
  truth for current state.
- **At the end of every session:** update `PROGRESS.md`, ensure `mvn -q test` is
  green, commit, and reflect status in Linear. See `PROGRESS.md` → "Session
  discipline".

## Build & test

Maven multi-module project (`codegen`, `oms-codec`), Java 21.

```bash
mvn -q test                 # unit tests
mvn -q verify               # full build + tests
mvn -q -DskipTests package  # compile only
mvn -q -pl oms-codec test   # single module
```

Read `TESTING.md` and `TEST_PLAN.md` before writing tests. The guiding principle
there: everything is mocked except the codegen'd codec, order state machine,
price-check logic, and persistence. Prefer deterministic, table-driven fixtures
and golden vectors over live dependencies.

## Conventions

- Do not edit generated code by hand; change the generator in `codegen/` and
  regenerate.
- Keep the binary codec byte-exact. Any codec change needs a round-trip test
  (encode → decode → encode) plus a golden vector.
- Design docs are the source of truth. If behavior and `DESIGN.md` disagree,
  stop and resolve the discrepancy first.
- Match existing style in the file you touch; prefer clarity over cleverness.

## Git best practices

### Committing

- Only commit when explicitly asked. Never commit secrets, credentials, keys, or
  generated `target/` output.
- Keep commits **small and focused**: one logical change per commit. Do not mix
  refactors with behavior changes.
- Before staging, review what you are about to commit:

  ```bash
  git status
  git diff
  git diff --staged
  ```

- Stage explicitly (`git add <paths>`) rather than `git add -A` when in doubt.
- Never rewrite published history, force-push, or amend a commit that failed
  CI/hooks unless explicitly asked.

### Commit messages

Follow Conventional Commits:

```
<type>(<scope>): <short imperative summary>

<body: what and why, not how. Wrap at ~72 cols.>
```

- Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`,
  `chore`, `revert`.
- Scope is the module or area, e.g. `codec`, `session`, `order`, `price-check`,
  `fixture`.
- Summary: imperative mood, lowercase, no trailing period, ≤ 72 chars.
- Body (when needed): explain **why** the change is made and any trade-offs.
  Reference the spec/DESIGN section or issue, e.g. `Refs DESIGN.md §8`.
- Breaking changes: add `!` after the type/scope and a `BREAKING CHANGE:` footer.

Examples:

```
feat(codec): add presence-map MSB ordering to encoder

Refs OCGC spec §3.2. Round-trips against the golden vectors in
oms-codec/src/test/resources/vectors.
```

```
fix(session): reject Logon when NextExpected < peer sequence

Previously accepted and silently reset, which masked sequence gaps.
```

### Branching

- Default branch is `master`; keep it releasable.
- Branch names: `<type>/<short-kebab-description>`, e.g.
  `feat/queue-manager`, `fix/logon-sequence-reset`.

### Pull requests

- One PR = one coherent change set. Keep it reviewable; split large work.
- PR description must contain:
  - **Summary** — what changed and why, in prose.
  - **Changes** — bullet list of the notable edits.
  - **Test plan** — exact commands run and results (e.g. `mvn -q verify`).
  - **Risk / rollback** — what could break and how to revert.
  - **References** — issue links or DESIGN/spec sections.
- Link the issue (`Closes #123`) when applicable.
- Ensure `mvn -q verify` passes before requesting review. Do not merge with a
  red build.
- Keep the PR title in Conventional Commit form; prefer squash-merge so the
  title becomes the commit on `master`.
- Reviewers should be told to focus on the codec byte layout and the
  order/session state machines, which are the highest-risk areas.
