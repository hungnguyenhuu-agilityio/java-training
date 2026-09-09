# TASK_GUIDE — T001: Modular Runtime, Database, and Full-Stack Foundation
**Date**: 2026-09-08
**Complexity Level**: C2
**Risk Level**: Medium
**Priority**: P0
**Execution Type**: AFK
**Assigned agent**: Common-Infrastructure-Agent
**Agent guide**: `agents/common-infrastructure.md`

## Mandatory Startup (Do Not Skip)

Read `PROJECT_SPEC.md`, `memory/MEMORY.md`, this guide, `agents/common-infrastructure.md`, `agents/general-agent-template.md`, and `memory/codebase-map.md` if present. Apply C2 process. Run `migration-safety` before creating or applying Liquibase changes.

## Requirement (Pillar 1 — Adapt the requirement)

Establish the reproducible runtime and architectural foundation required by every later vertical slice.

**Restated intent**: A developer or CI runner can build, test, and start the Spring Boot, Angular, and MySQL stack, with Liquibase and executable modular-boundary checks.

**Out of scope**: Business features, production deployment, secrets, and broad shared abstractions.

**Requirement Refs**: NFR-004, NFR-005, NFR-009, NFR-012.

### Requirement Fidelity Gate

- [x] Intent approved through Stage 2 plan
- [x] Terms align with `PROJECT_SPEC.md`
- [x] Acceptance criteria trace to the requirement
- [x] Requirement references exist in `PRD.md`

## Dependencies & Reachability

**Depends on**: None  
**Entry point**: `docker compose up --build`

## Acceptance Criteria

| # | Criterion | Traces to requirement |
|---|---|---|
| 1 | One documented command starts backend, frontend, and MySQL with health-aware dependencies. | NFR-009 |
| 2 | Liquibase exclusively creates the baseline schema; ORM mutation is disabled. | NFR-004 |
| 3 | Automated architecture tests reject cycles and outward domain/application dependencies. | NFR-005 |
| 4 | Backend and frontend build and test independently in CI-friendly mode. | NFR-012 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Clean checkout with prerequisites | Builds and starts the complete local stack | Docker smoke test |
| Illegal module dependency fixture | Architecture test fails | Automated test |

```bash
docker compose config && (cd backend && ./mvnw test) && (cd frontend && npm test -- --watch=false && npm run build)
```

## UI / Design Acceptance Criteria

| Evidence | Verification method | Expected result |
|---|---|---|
| Visual regression | Automated screenshot of root shell | Stable scaffold without overflow |
| Design-system compliance | CSS/token audit | No ad-hoc feature styling introduced |
| Responsiveness | Screenshots at 375px, 768px, 1280px | Root shell renders without clipping |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T001.md` at Stage 4/5.

## Demonstration

**BEFORE**: Only independent Spring Boot and Angular scaffolds exist; no Compose runtime, baseline Liquibase schema, or module-boundary suite exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: `docs/ddr/0001-package-enforced-modular-monolith.md`.  
**Vital slice**: Bootable stack plus one architecture-boundary test.  
**Cut list**: No feature modules beyond empty boundary roots; no deployment automation.

## Edge Case Checklist

- [ ] MySQL is not ready when the backend starts.
- [ ] Liquibase and Hibernate disagree about schema ownership.
- [ ] Docker and host-mode configuration diverge.

## Files to Change (Predicted)

`docker-compose.yml`, backend build/config/changelog/module packages/tests, frontend environment/proxy shell, and CI-neutral verification configuration.

## Files Must NOT Touch

`PRD.md`, accepted DDRs, and feature implementations assigned to T002–T014.

## Test Plan

Run backend context, architecture, Liquibase, frontend unit/build, Compose configuration, and startup smoke checks.

## Completion Checklist

- [ ] Implementation and new tests complete
- [ ] Migration-safety, code-review, security-review, and verify gates pass
- [ ] Reviewer records evidence in `tasks/TASK_REVIEW_T001.md`
- [ ] Supervisor updates Kanban
