# TASK_GUIDE — T002: Public Product Catalog Journey
**Date**: 2026-09-11
**Complexity Level**: C2
**Risk Level**: Medium
**Priority**: P0
**Execution Type**: AFK
**Assigned agents**: Backend-Implementer (lead), Frontend-Implementer
**Agent guides**: `agents/backend.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read `PROJECT_SPEC.md`, `memory/MEMORY.md`, this guide, both assigned agent guides, `agents/general-agent-template.md`, and `memory/codebase-map.md` if present. Apply C2 process. Run `migration-safety` before schema work.

## Requirement (Pillar 1 — Adapt the requirement)

Deliver public product discovery from persisted catalog records through API and Angular UI.

**Restated intent**: Guests can browse active products with pagination, search, sorting, category filtering, and a product-detail route.  
**Out of scope**: Administration, inventory quantity, authentication, cart, and external search engines.  
**Requirement Refs**: US-001, US-002, FR-001, FR-002, FR-010, FR-011, FR-012, NFR-013.

### Requirement Fidelity Gate

- [x] Intent and domain terms approved
- [x] Each criterion traces to an approved requirement
- [x] All references exist in `PRD.md`

## Dependencies & Reachability

**Depends on**: T001 — runnable modular stack and migrations  
**Entry point**: `GET /api/catalog/products`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Public list supports bounded pagination, text search, approved sorting, and category filtering over active products. | FR-001 |
| 2 | Public detail returns an active product and hides inactive or unknown products using safe Problem Details. | FR-002, FR-010 |
| 3 | Angular catalog and detail routes expose loading, empty, validation, and failure states. | FR-011 |
| 4 | The generated OpenAPI contract accurately describes catalog list/detail operations, bounded query parameters, success schemas, and RFC 7807 failure responses; the reference dataset meets catalog latency targets. | FR-012, FR-010, NFR-013 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Mixed active/inactive catalog | Only matching active records appear in stable order | Backend integration + UI test |
| Invalid page/sort or inactive ID | Actionable safe failure or not-found result | Negative tests |
| Generated OpenAPI document | Catalog paths, parameters, schemas, public access, and Problem Details match the implemented contract | OpenAPI contract test |

```bash
(cd backend && ./mvnw test -Dtest='*Catalog*Test,*Catalog*IT') && (cd frontend && npm test -- --watch=false --include='src/app/features/catalog/**/*.spec.ts' && npm run build)
```

## UI / Design Acceptance Criteria

**UI specification**: [`UI_SPEC.md` §7 — T002 Public Catalog](../UI_SPEC.md#t002-public-catalog)

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Automated catalog/detail screenshots | Stable approved states |
| Design-system compliance | Token and component audit | Shared tokens/components used |
| Responsiveness | 375px, 768px, 1280px screenshots | Grid/detail adapt without overflow |

### Evidence

> **Moved.** See `tasks/TASK_REVIEW_T002.md`.

## Demonstration

> **Moved.** See `tasks/TASK_REVIEW_T002.md`.

## Approach

**Pattern reference**: `UI_SPEC.md`, `frontend/README.md` feature layout, and DDR-0001 module boundary.
**Vital slice**: Paginated list plus detail.  
**Cut list**: No inventory counts, autocomplete, facets, external search service, generated Angular client, or annotations that merely repeat inferable Java/validation metadata.

## Edge Case Checklist

- [ ] Search normalization and MySQL collation differ.
- [ ] Page becomes empty after filtering.
- [ ] Product deactivates between list and detail.
- [ ] Pagination or generic response wrappers produce incomplete or misleading OpenAPI schemas.
- [ ] Public catalog operations accidentally inherit a bearer-auth requirement.
- [ ] Controller advice generates runtime Problem Details that disagree with the documented error schema.

## Files to Change (Predicted)

Catalog module domain/application/adapters/migration/tests and Angular catalog feature/routes/tests.

## Files Must NOT Touch

Inventory, ordering, payment, identity, and deployment workflows.

## Test Plan

Domain, repository, MVC, generated OpenAPI contract, performance-fixture, Angular component/service, and responsive browser tests. Prefer structural assertions on important paths, parameters, security, and schemas over snapshotting the entire generated document.

## Completion Checklist

- [ ] Implementation and new tests complete
- [ ] Migration-safety, code-review, security-review, and verify gates pass
- [ ] UI evidence and reviewer evidence recorded in `TASK_REVIEW_T002.md`
- [ ] Supervisor updates Kanban
