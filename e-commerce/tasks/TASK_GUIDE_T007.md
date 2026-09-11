# TASK_GUIDE — T007: Warehouse Allocation and Atomic Reservation
**Date**: 2026-09-08
**Complexity Level**: C3
**Risk Level**: High
**Priority**: P0
**Execution Type**: AFK
**Assigned agent**: Backend-Implementer
**Agent guide**: `agents/backend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/backend/general/codebase-map files. Apply C3 decomposition and brainstorming. Run `migration-safety` before schema work.

## Requirement (Pillar 1 — Adapt the requirement)

Turn an authenticated cart into one idempotent order and expiring reservation at one internally selected warehouse.

**Restated intent**: Checkout atomically snapshots the cart and selects the eligible warehouse maximizing the lowest post-reservation stock, ties by ascending warehouse code, without split fulfillment or overselling.  
**Out of scope**: Stripe calls, payment confirmation, customer warehouse choice, and multi-warehouse orders.  
**Requirement Refs**: US-005, US-005B, FR-005, FR-005B, FR-006, FR-010, FR-012, NFR-003, NFR-006C.

### Requirement Fidelity Gate

- [x] Allocation formula explicitly approved
- [x] Criteria trace to every requirement
- [x] Terms align with DDR-0002/0003 and glossary

## Dependencies & Reachability

**Depends on**: T005 — authenticated cart; T006 — warehouse inventory  
**Entry point**: `POST /api/checkout/reservations`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Eligible warehouses can fulfill every requested line; ranking maximizes the minimum remaining stock and ties by ascending code. | FR-005B |
| 2 | Order/item/shipping/warehouse snapshots and reservation lines commit atomically with stock holds. | FR-005, NFR-003 |
| 3 | Same actor/key/fingerprint returns the same result; key reuse with different payload is rejected. | FR-006 |
| 4 | No eligible warehouse rejects with typed out-of-stock and no partial state; expiry/cancel releases once. | FR-005, FR-010 |
| 5 | Generated OpenAPI describes checkout reservation input, idempotency requirements, success/out-of-stock schemas, authorization, and Problem Details without exposing warehouse-selection internals. | FR-012, FR-010 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Multiple eligible warehouses and ties | Exact approved warehouse selected reproducibly | Real-DB integration test |
| Concurrency, retry, invalid cart, or no eligible warehouse | No duplicate/partial order and no oversell | Adversarial test |

```bash
cd backend && ./mvnw test -Dtest='*Allocation*Test,*Reservation*Test,*CheckoutReservation*IT'
```

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T007.md` at Stage 4/5.

## Demonstration

**BEFORE**: No checkout allocation policy, warehouse-specific reservation transaction, order snapshot workflow, or idempotency test exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: DDR-0002 and DDR-0003.  
**Vital slice**: One complete cart allocated and reserved atomically.  
**Cut list**: No provider call, split allocation, customer selection, or generic allocation framework.

## Edge Case Checklist

- [ ] Two checkouts choose the same last stock concurrently.
- [ ] Best-ranked warehouse loses eligibility before lock acquisition.
- [ ] Reservation expires during another transition.
- [ ] Same key arrives with a different fingerprint.

## Files to Change (Predicted)

Inventory allocation/reservation contracts and persistence, ordering checkout process, migrations, web adapter, and real-MySQL integration tests.

## Files Must NOT Touch

Frontend warehouse selection, Stripe adapter, notifications, and WMS scope.

## Test Plan

Pure policy tests plus MySQL transaction/isolation/idempotency/expiry integration tests and generated OpenAPI structural assertions; do not rely on H2 for locking evidence.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Migration-safety, code-review, security-review, and verify pass
- [ ] Reviewer evidence recorded in `TASK_REVIEW_T007.md`
- [ ] Supervisor updates Kanban
