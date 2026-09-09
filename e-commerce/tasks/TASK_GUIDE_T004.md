# TASK_GUIDE — T004: Protected Catalog Administration
**Date**: 2026-09-08
**Complexity Level**: C2
**Risk Level**: High
**Priority**: P0
**Execution Type**: AFK
**Assigned agents**: Backend-Implementer (lead), Frontend-Implementer
**Agent guides**: `agents/backend.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project, memory, task, general, backend, frontend, and codebase-map files. Apply C2 process; run `migration-safety` if schema changes.

## Requirement (Pillar 1 — Adapt the requirement)

Give administrators controlled category/product maintenance without bypassing catalog invariants.

**Restated intent**: Authorized administrators can create, edit, deactivate, and reactivate catalog records while referenced history remains safe.  
**Out of scope**: Stock quantities, warehouse operations, hard deletion, bulk import, and media hosting.  
**Requirement Refs**: US-007, FR-008, FR-010, FR-011, NFR-001.

### Requirement Fidelity Gate

- [x] Intent approved and terms aligned
- [x] Criteria trace to all refs
- [x] Refs exist in PRD

## Dependencies & Reachability

**Depends on**: T002 — catalog model/query; T003 — administrator authorization  
**Entry point**: `POST /api/admin/products`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | ADMIN can create/edit valid categories and products; CUSTOMER and anonymous callers cannot. | FR-008, NFR-001 |
| 2 | Deactivation hides public products while preserving referenced history; active categories with active products cannot deactivate. | FR-008 |
| 3 | Angular admin routes expose permitted actions and safe validation/error states. | FR-011, FR-010 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Authorized valid update | Persisted change appears publicly when active | API/UI integration |
| Unauthorized, duplicate, or invalid deactivation | Rejected without partial change | Security/negative tests |

```bash
(cd backend && ./mvnw test -Dtest='*CatalogAdmin*Test,*CatalogAdmin*IT') && (cd frontend && npm test -- --watch=false --include='src/app/features/admin/catalog/**/*.spec.ts' && npm run build)
```

## UI / Design Acceptance Criteria

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Automated list/form/state screenshots | Stable admin workflow |
| Design-system compliance | Token/control audit | Shared components used |
| Responsiveness | 375px, 768px, 1280px | Tables/forms remain operable |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T004.md` at Stage 4/5.

## Demonstration

**BEFORE**: No protected catalog command API or Angular catalog-administration workflow exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: T002 catalog contracts and `PROJECT_SPEC.md` aggregate rules.  
**Vital slice**: Create/edit/deactivate/reactivate through one admin journey.  
**Cut list**: No hard delete, bulk tools, stock editing, or uploads.

## Edge Case Checklist

- [ ] Slug/SKU uniqueness races.
- [ ] Category deactivation conflicts with active product.
- [ ] Stale administrator edit overwrites a newer change.

## Files to Change (Predicted)

Catalog commands/admin web adapters/tests and Angular admin catalog feature/guards/tests.

## Files Must NOT Touch

Inventory quantity, ordering/payment, authentication internals, and deployment.

## Test Plan

Aggregate, authorization, persistence concurrency, MVC Problem Details, Angular forms/guards, and browser tests.

## Completion Checklist

- [ ] Implementation and tests complete
- [ ] Code-review, security-review, blast-radius if sensitive data appears, and verify pass
- [ ] UI/reviewer evidence recorded in `TASK_REVIEW_T004.md`
- [ ] Supervisor updates Kanban
