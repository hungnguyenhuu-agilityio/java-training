# TASK_GUIDE — T009: Concurrent Reservation and No-Overselling Evidence
**Date**: 2026-09-08
**Complexity Level**: C3
**Risk Level**: High
**Priority**: P0
**Execution Type**: AFK
**Assigned agent**: QA-Automation-Agent
**Agent guide**: `agents/qa.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/QA/general/codebase-map files. Apply C3 brainstorming and independent-oracle rules. Do not modify feature code.

## Requirement (Pillar 1 — Adapt the requirement)

Create independent, deterministic evidence that warehouse reservations cannot oversell under real database contention.

**Restated intent**: Against 50 units and 100 unique simultaneous one-unit attempts, exactly 50 reserve, 50 return typed out-of-stock, availability settles at zero, and unrelated infrastructure errors invalidate the run.  
**Out of scope**: Production traffic endpoints, arbitrary remote workloads, performance tuning, and implementation fixes.  
**Requirement Refs**: US-010A, NFR-003, NFR-007, NFR-014.

### Requirement Fidelity Gate

- [x] Oracle counts and invalid-run rule approved
- [x] Criteria trace to all refs
- [x] Real-MySQL requirement explicit

## Dependencies & Reachability

**Depends on**: T007 — reservation command and persistence  
**Entry point**: `ConcurrentReservationEvidenceIT`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Test creates one warehouse/product with 50 available units and issues 100 unique synchronized one-unit attempts. | NFR-014 |
| 2 | Exactly 50 succeed and 50 fail specifically out-of-stock; final availability is zero and never observed negative. | NFR-003, NFR-014 |
| 3 | Duplicate identities are tested separately and consume no extra stock. | NFR-003 |
| 4 | Timeout, connection, or unexpected failures fail the evidence run rather than inflate out-of-stock counts. | NFR-014 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Real MySQL and synchronized attempts | Exact 50/50/0 outcome across repeated runs | Automated integration test |
| Injected infrastructure failure | Suite fails with distinct classification | Negative oracle test |

```bash
cd backend && ./mvnw test -Dtest='ConcurrentReservationEvidenceIT,ReservationIdempotencyEvidenceIT'
```

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T009.md` at Stage 4/5.

## Demonstration

**BEFORE**: No independent real-MySQL concurrency oracle proves the 50-success, 50-out-of-stock, zero-remaining invariant.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: NFR-014 and T007 public application command.  
**Vital slice**: Exact 100-attempt evidence case.  
**Cut list**: No HTTP simulator, production endpoint, or lock-strategy optimization.

## Edge Case Checklist

- [ ] Test start is not truly synchronized.
- [ ] Connection pool limits masquerade as stock failure.
- [ ] Reused idempotency keys reduce unique attempts.
- [ ] Reservation expiry occurs during the run.

## Files to Change (Predicted)

Backend test fixtures/integration suites and test-only container/configuration files.

## Files Must NOT Touch

Production feature code, controllers, production profiles, and deployment configuration.

## Test Plan

Repeat exact oracle, idempotency control, infrastructure-failure classification, and invariant sampling against MySQL.

## Completion Checklist

- [ ] Independent tests written and pass repeatedly
- [ ] Code-review, security-review, and verify pass
- [ ] Reviewer evidence recorded in `TASK_REVIEW_T009.md`
- [ ] Supervisor updates Kanban
