# TASK_GUIDE — T012: Administrator Fulfillment, Cancellation, and Refund
**Date**: 2026-09-08
**Complexity Level**: C3
**Risk Level**: High
**Priority**: P1
**Execution Type**: AFK
**Assigned agents**: Backend-Implementer (lead), Frontend-Implementer
**Agent guides**: `agents/backend.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/general/backend/frontend/codebase-map files. Apply C3 decomposition/brainstorming. Run `migration-safety` for schema changes and plan payment-data security/blast-radius review.

## Requirement (Pillar 1 — Adapt the requirement)

Deliver protected administrator order review, legal fulfillment transitions, and recoverable paid-order cancellation/refund.

**Restated intent**: ADMIN can advance valid states and request idempotent refunds, but cancellation and stock restoration occur only after verified refund confirmation.  
**Out of scope**: Returns, partial refunds, disputes, post-shipment cancellation, and customer cancellation.  
**Requirement Refs**: US-008, FR-009, FR-010, FR-011, FR-012, NFR-001, NFR-006B, NFR-006C.

### Requirement Fidelity Gate

- [x] State and refund truth rules approved
- [x] Criteria trace to every ref
- [x] Terms align with DDR-0002

## Dependencies & Reachability

**Depends on**: T004 — ADMIN; T008 — payment/refund adapter; T011 — order queries  
**Entry point**: `POST /api/admin/orders/{orderId}/transitions`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | ADMIN lists/details orders and may apply only `CONFIRMED→SHIPPED→DELIVERED`. | FR-009, NFR-001 |
| 2 | Pending cancellation releases reservation atomically; paid confirmed cancellation starts idempotent refund without holding a DB transaction over network. | FR-009, NFR-006C |
| 3 | Only verified refund confirmation marks `CANCELLED` and restores stock once. | FR-009, NFR-006B |
| 4 | Angular admin UI exposes only currently valid actions and pending/failure states. | FR-011 |
| 5 | Generated OpenAPI describes administrator order queries/transitions/refund initiation, allowed inputs, bearer authorization, and Problem Details while keeping provider callback details out of public interactive documentation. | FR-012, FR-010, NFR-001 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Legal transitions/refund confirmation | Exact state/history/stock effects once | Integration/UI test |
| Illegal role/state, duplicate or delayed refund | Rejected or reconciled without duplicate restoration | Security/failure test |

```bash
(cd backend && ./mvnw test -Dtest='*OrderAdmin*Test,*Refund*IT,*Fulfillment*IT') && (cd frontend && npm test -- --watch=false --include='src/app/features/admin/orders/**/*.spec.ts' && npm run build)
```

## UI / Design Acceptance Criteria

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Admin order/refund screenshots | Stable state-specific actions |
| Design-system compliance | Token/dialog audit | Shared destructive-action patterns |
| Responsiveness | 375px, 768px, 1280px | Tables/details/actions remain usable |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T012.md` at Stage 4/5.

## Demonstration

**BEFORE**: No administrator fulfillment transition, refund coordination, verified cancellation/restock flow, or admin order UI exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: DDR-0002 and T008 webhook processing.  
**Vital slice**: Confirmed paid order → refund pending → verified cancellation/restock.  
**Cut list**: No returns, partial refunds, disputes, or post-shipment cancellation.

## Edge Case Checklist

- [ ] Refund request succeeds but response is lost.
- [ ] Duplicate/out-of-order refund webhook.
- [ ] Ship command races cancellation.
- [ ] Stock restoration races warehouse deactivation.

## Files to Change (Predicted)

Ordering admin transitions, payment refund coordination/webhook handling, inventory restoration, tests, and Angular admin order feature/tests.

## Files Must NOT Touch

Customer order mutation, returns, partial refunds, or catalog rules.

## Test Plan

State-machine, authorization, provider fault, webhook replay/order, stock idempotency, generated OpenAPI structural assertions, Angular action visibility, and browser tests.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Migration-safety if applicable, code-review, security-review, blast-radius, and verify pass
- [ ] UI/reviewer evidence recorded in `TASK_REVIEW_T012.md`
- [ ] Supervisor updates Kanban
