# TASK_GUIDE — T006: Warehouse-Scoped Inventory Administration
**Date**: 2026-09-08
**Complexity Level**: C3
**Risk Level**: High
**Priority**: P0
**Execution Type**: AFK
**Assigned agents**: Backend-Implementer (lead), Frontend-Implementer
**Agent guides**: `agents/backend.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read project/spec/memory/task/general/backend/frontend/codebase-map files. Apply C3 decomposition and brainstorming. Run `migration-safety` before schema work.

## Requirement (Pillar 1 — Adapt the requirement)

Implement warehouse-scoped stock administration without expanding into full warehouse management.

**Restated intent**: ADMIN manages warehouse identity, activation, per-product on-hand quantity, and safety threshold while catalog products remain global.  
**Out of scope**: WAREHOUSE_MANAGER, customer warehouse selection, bins, receiving, picking, packing, procurement, transfers, routing, and order splitting.  
**Requirement Refs**: US-008A, FR-008A, FR-010, FR-011, FR-012, NFR-001, NFR-003.

### Requirement Fidelity Gate

- [x] Warehouse-scoped-not-WMS boundary approved
- [x] Criteria trace to all refs
- [x] Terms align with DDR-0003 and glossary

## Dependencies & Reachability

**Depends on**: T002 — global products; T003 — ADMIN authorization  
**Entry point**: `POST /api/admin/warehouses`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | ADMIN can create/edit/activate/deactivate warehouses and manage non-negative on-hand and safety threshold per product. | FR-008A |
| 2 | Warehouse/product uniqueness and `0 <= reserved <= on-hand` are enforced at domain and database boundaries. | NFR-003 |
| 3 | Deactivation rejects active reservations or assigned non-terminal orders without partial change. | FR-008A |
| 4 | Angular admin workflow exposes inventory state without WMS-only operations. | FR-011 |
| 5 | Generated OpenAPI describes warehouse/inventory administration operations, numeric constraints, bearer authorization, and Problem Details without exposing excluded WMS operations. | FR-012, FR-010, NFR-001 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| ADMIN maintains warehouse inventory | Persisted state and derived availability are correct | Integration/UI test |
| Unauthorized, negative, duplicate, or unsafe deactivate | Typed safe rejection | Security/constraint tests |

```bash
(cd backend && ./mvnw test -Dtest='*Warehouse*Test,*Warehouse*IT,*InventoryAdmin*IT') && (cd frontend && npm test -- --watch=false --include='src/app/features/admin/inventory/**/*.spec.ts' && npm run build)
```

## UI / Design Acceptance Criteria

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Warehouse/inventory screenshots | Stable list/form/error states |
| Design-system compliance | Token/control audit | Existing admin patterns used |
| Responsiveness | 375px, 768px, 1280px | Tables/forms remain operable |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T006.md` at Stage 4/5.

## Demonstration

**BEFORE**: The schema sketch placed stock on products before reconciliation; no executable warehouse inventory domain, admin API/UI, migration, or test exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: `docs/ddr/0003-warehouse-scoped-inventory.md`.  
**Vital slice**: Warehouse CRUD plus one product stock/threshold workflow.  
**Cut list**: Every full-WMS capability listed above.

## Edge Case Checklist

- [ ] Concurrent quantity edits conflict.
- [ ] On-hand is reduced below reserved.
- [ ] Product or warehouse deactivates during maintenance.
- [ ] Warehouse code uniqueness differs by collation.

## Files to Change (Predicted)

Inventory module domain/application/admin adapters/migrations/tests and Angular admin inventory feature/routes/tests.

## Files Must NOT Touch

Catalog product ownership, customer checkout UX, payment, and WMS-only concepts.

## Test Plan

Domain invariants, migration constraints, authorization/MVC, generated OpenAPI structural assertions, concurrency, Angular forms, and responsive browser tests.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Migration-safety, code-review, security-review, and verify pass
- [ ] UI/reviewer evidence recorded in `TASK_REVIEW_T006.md`
- [ ] Supervisor updates Kanban
