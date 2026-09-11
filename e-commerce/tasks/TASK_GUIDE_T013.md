# TASK_GUIDE — T013: Low-Stock Scanning and Transition-Based Alerting
**Date**: 2026-09-08
**Complexity Level**: C2
**Risk Level**: Medium
**Priority**: P1
**Execution Type**: AFK
**Assigned agent**: Backend-Implementer
**Agent guide**: `agents/backend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/backend/general/codebase-map files. Apply C2 process; run `migration-safety` before alert-state schema changes.

## Requirement (Pillar 1 — Adapt the requirement)

Detect warehouse inventory entering low-stock state through one scheduler/manual application use case and create deduplicated notification requests.

**Restated intent**: Scan every 30 minutes, alert once on transition to available-at-or-below threshold, suppress while low, reset after recovery, and re-alert on a later downward crossing. The user delegated this standard rule to the Supervisor.  
**Out of scope**: Replenishment, repeated time-based reminders, production manual trigger, real email, and daily sales reports.  
**Requirement Refs**: US-008A, US-010A, FR-015, NFR-007, NFR-008A.

### Requirement Fidelity Gate

- [x] Supervisor default explicitly recorded and user approved delegation
- [x] Criteria trace to refs
- [x] Low-stock term aligns with glossary

## Dependencies & Reachability

**Depends on**: T006 — warehouse inventory; T010 — notification request  
**Entry point**: `LowStockScanUseCase`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Scheduler invokes the scan every 30 minutes; authorized non-production manual adapter invokes the identical use case. | FR-015 |
| 2 | Available stock at/below threshold creates one alert only on entry into low state. | FR-015 |
| 3 | Unchanged scans suppress duplicates; recovery resets state; later downward crossing creates a new alert. | FR-015 |
| 4 | Concurrent/overlapping scans remain idempotent and record bounded outcome/timing telemetry. | NFR-008A |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Normal→low→unchanged→recovered→low sequence | Exactly two logical alert requests | State/integration test |
| Overlapping scheduled/manual scans | One transition effect and no duplicates | Concurrency test |

```bash
cd backend && ./mvnw test -Dtest='*LowStock*Test,*LowStock*IT'
```

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T013.md` at Stage 4/5.

## Demonstration

**BEFORE**: No low-stock state machine, scheduled scan, non-production manual adapter, alert deduplication, or recovery/re-arm test exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: `memory/glossary.md` low-stock state and T010 request contract.  
**Vital slice**: State transition scan → deduplicated test notification.  
**Cut list**: No replenishment, time-based repeat reminders, production trigger, or email.

## Edge Case Checklist

- [ ] Threshold changes create or clear state unexpectedly.
- [ ] Scheduler runs on two application instances.
- [ ] Manual and scheduled scans overlap.
- [ ] Warehouse/product deactivates during scan.

## Files to Change (Predicted)

Inventory low-stock use case/state persistence/scheduler/non-production adapter/tests, notification integration, and migrations.

## Files Must NOT Touch

Production debug exposure, replenishment, external email, daily reports, and frontend.

## Test Plan

State-table tests, scheduler/manual equivalence, overlap/idempotency, recovery/re-arm, authorization/profile, and metric tests.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Migration-safety, code-review, security-review, and verify pass
- [ ] Reviewer evidence recorded in `TASK_REVIEW_T013.md`
- [ ] Supervisor updates Kanban
