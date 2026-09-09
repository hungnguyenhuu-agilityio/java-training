# TASK_GUIDE — T005: Authenticated Shopping-Cart Journey
**Date**: 2026-09-08
**Complexity Level**: C2
**Risk Level**: Medium
**Priority**: P0
**Execution Type**: AFK
**Assigned agents**: Backend-Implementer (lead), Frontend-Implementer
**Agent guides**: `agents/backend.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/general/backend/frontend files and codebase map. Apply C2 process; run `migration-safety` before schema work.

## Requirement (Pillar 1 — Adapt the requirement)

Deliver one authenticated cart per customer through API and Angular UI.

**Restated intent**: A customer can add, update, remove, and clear valid active products while seeing authoritative quantities, unit prices, and totals.  
**Out of scope**: Guest carts, promotions, inventory holds, checkout, and payment.  
**Requirement Refs**: US-004, FR-004, FR-010, FR-011, NFR-001.

### Requirement Fidelity Gate

- [x] Intent approved and terms aligned
- [x] Criteria trace to refs
- [x] Refs exist in PRD

## Dependencies & Reachability

**Depends on**: T002 — catalog; T003 — authenticated ownership  
**Entry point**: `POST /api/cart/items`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Authenticated customer can add/update/remove/clear only their cart with positive bounded quantities. | FR-004, NFR-001 |
| 2 | Cart returns authoritative product/price snapshots for display and handles inactive/missing products safely. | FR-004, FR-010 |
| 3 | Angular cart shows items/totals and prevents invalid or duplicate UI submissions. | FR-011 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Valid customer operations | One active cart reflects each operation and total | Integration/browser test |
| Other owner, invalid quantity, inactive product | Safe rejection without mutation | Negative/security tests |

```bash
(cd backend && ./mvnw test -Dtest='*Cart*Test,*Cart*IT') && (cd frontend && npm test -- --watch=false --include='src/app/features/cart/**/*.spec.ts' && npm run build)
```

## UI / Design Acceptance Criteria

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Automated cart state screenshots | Stable empty/populated/error states |
| Design-system compliance | Token/control audit | Shared controls and error patterns |
| Responsiveness | 375px, 768px, 1280px | Items and totals remain readable |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T005.md` at Stage 4/5.

## Demonstration

**BEFORE**: No cart domain, persistence, API, Angular cart journey, or ownership test exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: T002 product DTOs and T003 ownership contracts.  
**Vital slice**: Add → update → remove/clear with total.  
**Cut list**: No guest merge, promotion, save-for-later, or reservation.

## Edge Case Checklist

- [ ] Two tabs update the same item.
- [ ] Product deactivates while in cart.
- [ ] Client-displayed price becomes stale.

## Files to Change (Predicted)

Cart module migration/domain/application/web/persistence/tests and Angular cart feature/navigation/tests.

## Files Must NOT Touch

Warehouse stock, order/payment behavior, and catalog ownership.

## Test Plan

Cart aggregate, ownership, persistence/MVC, Angular service/component, and browser-flow tests.

## Completion Checklist

- [ ] Implementation and tests complete
- [ ] Migration-safety, code-review, security-review, and verify pass
- [ ] UI/reviewer evidence recorded in `TASK_REVIEW_T005.md`
- [ ] Supervisor updates Kanban
