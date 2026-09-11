# TASK_GUIDE — T011: Customer Order History and Ownership Protection
**Date**: 2026-09-08
**Complexity Level**: C2
**Risk Level**: High
**Priority**: P0
**Execution Type**: AFK
**Assigned agents**: Backend-Implementer (lead), Frontend-Implementer
**Agent guides**: `agents/backend.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/general/backend/frontend/codebase-map files. Apply C2 process and plan security/blast-radius review.

## Requirement (Pillar 1 — Adapt the requirement)

Expose authenticated customers' immutable order snapshots and status histories without cross-customer access.

**Restated intent**: A customer can list and inspect only their orders, including payment/fulfillment state and selected-warehouse reference needed for history, through safe API and Angular routes.  
**Out of scope**: Administrator transitions, cancellation/refund commands, editing snapshots, and warehouse selection.  
**Requirement Refs**: US-006, FR-007, FR-010, FR-011, FR-012, NFR-001.

### Requirement Fidelity Gate

- [x] Intent and ownership boundary approved
- [x] Criteria trace to refs
- [x] Terms align with order/payment glossary

## Dependencies & Reachability

**Depends on**: T008 — paid checkout/order lifecycle  
**Entry point**: `GET /api/orders`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Customer list is paginated and returns only the authenticated owner's orders. | FR-007, NFR-001 |
| 2 | Detail includes immutable item/price/shipping snapshots and ordered status history without internal/sensitive data. | FR-007 |
| 3 | Another customer's order is not distinguishable from an unknown inaccessible order. | NFR-001, FR-010 |
| 4 | Angular list/detail/loading/empty/error routes preserve ownership behavior. | FR-011 |
| 5 | Generated OpenAPI describes paginated order history/detail schemas, bearer authorization, ownership-safe failures, and Problem Details without exposing internal payment or warehouse data. | FR-012, FR-010, NFR-001 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Two customers with orders | Each sees only owned records and correct history | Security integration/UI test |
| Guessed foreign order ID | Safe non-disclosing response | Adversarial test |

```bash
(cd backend && ./mvnw test -Dtest='*OrderQuery*Test,*OrderOwnership*IT') && (cd frontend && npm test -- --watch=false --include='src/app/features/orders/**/*.spec.ts' && npm run build)
```

## UI / Design Acceptance Criteria

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Order list/detail screenshots | Stable timeline and snapshot states |
| Design-system compliance | Token/component audit | Shared list/detail/error patterns |
| Responsiveness | 375px, 768px, 1280px | History remains readable without overflow |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T011.md` at Stage 4/5.

## Demonstration

**BEFORE**: No customer order list/detail API, ownership oracle, status-history mapping, or Angular order-history route exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: T003 ownership and T008 order contracts.  
**Vital slice**: Owned list → owned detail/history.  
**Cut list**: No admin actions, customer cancellation, invoice, or warehouse UI.

## Edge Case Checklist

- [ ] Enumeration through IDs or pagination metadata.
- [ ] Order history has same-timestamp transitions.
- [ ] Catalog/product changes rewrite historical presentation.

## Files to Change (Predicted)

Ordering query contracts/web/persistence/tests and Angular orders routes/components/services/tests.

## Files Must NOT Touch

Order transition rules, refund commands, inventory mutation, and payment webhook behavior.

## Test Plan

Ownership/security integration, snapshot mapping, pagination, generated OpenAPI structural assertions, Angular states, and responsive browser tests.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Code-review, security-review, blast-radius, and verify pass
- [ ] UI/reviewer evidence recorded in `TASK_REVIEW_T011.md`
- [ ] Supervisor updates Kanban
