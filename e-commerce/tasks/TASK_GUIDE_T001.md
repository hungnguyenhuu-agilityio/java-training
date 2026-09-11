# TASK_GUIDE — T001: Modular Runtime, Database, UI Shell, and Main-Only CI/CD Foundation
**Date**: 2026-09-11
**Complexity Level**: C3
**Risk Level**: High
**Priority**: P0
**Execution Type**: HITL
**Assigned agents**: Common-Infrastructure-Agent (lead), Frontend-Implementer
**Agent guides**: `agents/common-infrastructure.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read `PROJECT_SPEC.md`, `UI_SPEC.md`, `memory/MEMORY.md`, this guide, both assigned agent guides, `agents/general-agent-template.md`, and `memory/codebase-map.md` if present. Apply C3 decomposition and adversarial-verification rules. Run `migration-safety` before creating or applying Liquibase changes. Obtain human confirmation for GitHub branch protection, protected environment secrets, and Railway/Vercel project configuration; never print or commit secret values.

## Requirement (Pillar 1 — Adapt the requirement)

Establish the reproducible runtime, database, architecture, and delivery-automation foundation required by every later vertical slice.

**Restated intent**: A developer or CI runner can build, test, and start the Spring Boot, Angular, and MySQL stack with Liquibase, executable modular-boundary checks, and generated OpenAPI foundations. GitHub Actions runs CI without deployment for pull requests and non-`main` pushes; after all required checks pass, a merge into protected `main` triggers the single production release path for Railway and Vercel followed by smoke checks.

**Out of scope**: Business features, staging/preview environments, deployment from feature branches or pull requests, a second production release path, complete feature-level end-to-end/performance/security suites, and broad shared abstractions.

**Requirement Refs**: US-011, FR-011, FR-012, NFR-004, NFR-005, NFR-009, NFR-010, NFR-011, NFR-012.

### Requirement Fidelity Gate

- [x] Intent approved through Stage 2 plan
- [x] Terms align with `PROJECT_SPEC.md`
- [x] Acceptance criteria trace to the requirement
- [x] Requirement references exist in `PRD.md`

## Dependencies & Reachability

**Depends on**: None  
**Entry points**: `docker compose up --build`; GitHub Actions `pull_request`/`push` events with deployment gated to `refs/heads/main`

## Acceptance Criteria

| # | Criterion | Traces to requirement |
|---|---|---|
| 1 | One documented command starts backend, frontend, and MySQL with health-aware dependencies. | NFR-009 |
| 2 | Liquibase exclusively creates the baseline schema; ORM mutation is disabled. | NFR-004 |
| 3 | Automated architecture tests reject cycles and outward domain/application dependencies. | NFR-005 |
| 4 | Backend and frontend build and test independently in CI-friendly mode. | NFR-012 |
| 5 | A pinned Springdoc WebMVC dependency generates a valid OpenAPI document in test/local profiles; interactive documentation is disabled or access-controlled outside approved profiles. | FR-012 |
| 6 | GitHub Actions runs backend, frontend, architecture, migration, and Compose smoke gates for pull requests and pushes; failed required checks block the protected `main` merge path. | NFR-009, NFR-010, NFR-012 |
| 7 | Pull requests and non-`main` pushes cannot reach deployment jobs or production secrets. | NFR-010 |
| 8 | One successful protected-`main` merge triggers exactly one serialized production release path that deploys the Railway backend and Vercel frontend only after CI succeeds. | US-011, NFR-010 |
| 9 | The release fails if either target deployment or the post-deployment health/integration smoke checks fail; logs and artifacts contain no protected values. | NFR-010, NFR-011 |
| 10 | A documented baseline rollback restores the last known-good application release, and Liquibase changes are verified as backward-compatible with that rollback path. | NFR-011 |
| 11 | The Angular starter screen is replaced by the approved light-only Angular Material theme and shared role-aware shell foundation without implementing feature behavior. | FR-011 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Clean checkout with prerequisites | Builds and starts the complete local stack | Docker smoke test |
| Illegal module dependency fixture | Architecture test fails | Automated test |
| Booted backend in the test profile | OpenAPI JSON loads with project metadata and no actuator operations | MVC integration test |
| Pull request or non-`main` push | CI runs and deployment jobs remain skipped without production-secret access | Workflow policy test plus GitHub run evidence |
| Successful protected-`main` merge | Required CI passes, one serialized Railway/Vercel release runs, and post-deployment smoke checks pass | GitHub deployment run and target health evidence |
| Failed target deployment or smoke check | Release reports failure and the documented last-known-good rollback is executable | Failure-path test plus rollback verification |
| Shared shell at required viewports and keyboard input | Approved navigation hierarchy, focus behavior, Material theme, and no overflow | Component/browser accessibility and screenshot tests |

```bash
docker compose config && (cd backend && ./mvnw test) && (cd frontend && npm test -- --watch=false && npm run build) && ./scripts/verify-ci-policy.sh
```

## UI / Design Acceptance Criteria

**UI specification**: [`UI_SPEC.md` §6 — T001 Shared Shell and Material Foundation](../UI_SPEC.md#t001-shared-shell)

| Evidence | Verification method | Expected result |
|---|---|---|
| Visual regression | Automated screenshot of root shell | Stable scaffold without overflow |
| Design-system compliance | CSS/token audit | No ad-hoc feature styling introduced |
| Responsiveness | Screenshots at 375px, 768px, 1280px | Root shell renders without clipping |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T001.md` at Stage 4/5.

## Demonstration

**BEFORE**: Only independent Spring Boot and Angular scaffolds exist; no Compose runtime, baseline Liquibase schema, module-boundary suite, CI gate, or controlled deployment path exists.
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: `docs/ddr/0001-package-enforced-modular-monolith.md`.  
**Vital slice**: Bootable stack, baseline schema, one architecture-boundary test, generated OpenAPI infrastructure, CI on every push/pull request, and one protected-`main` Railway/Vercel deployment path.
**Cut list**: No feature modules beyond empty boundary roots; no staging/preview deployments, exhaustive schema annotations, generated clients, contract-first code generation, or final feature-level release certification.

## Edge Case Checklist

- [ ] MySQL is not ready when the backend starts.
- [ ] Liquibase and Hibernate disagree about schema ownership.
- [ ] Docker and host-mode configuration diverge.
- [ ] Swagger/OpenAPI or actuator endpoints are unintentionally public in a production-like profile.
- [ ] A Springdoc upgrade changes the generated schema without a failing test.
- [ ] A pull request or non-`main` push accidentally starts deployment or can read production secrets.
- [ ] A direct push bypasses the intended protected-`main` merge path.
- [ ] Deployment starts before every required CI job succeeds.
- [ ] Railway succeeds while Vercel fails, but the release is reported as successful.
- [ ] Concurrent merges deploy out of order instead of serializing or cancelling stale releases.
- [ ] A smoke-test failure leaves the release green or without an actionable rollback path.
- [ ] A Liquibase migration prevents the previous application version from being restored.

## Files to Change (Predicted)

`docker-compose.yml`, backend build/config/changelog/module packages/tests, Springdoc metadata/profile configuration, frontend environment/proxy shell, `.github/workflows/`, CI policy tests/scripts, Railway/Vercel deployment configuration, and rollback/runbook documentation.

## Files Must NOT Touch

`PRD.md`, accepted DDRs, and feature implementations assigned to T002–T014.

## Test Plan

Run backend context, architecture, Liquibase, OpenAPI MVC integration, frontend unit/build, Compose configuration/startup, workflow-policy tests, event-isolation checks, a protected-`main` deployment, post-deployment smoke checks, secret-exposure checks, and a baseline rollback verification.

## Completion Checklist

- [ ] Implementation and new tests complete
- [ ] Migration-safety, code-review, security-review, blast-radius, and verify gates pass
- [ ] Human-controlled GitHub/Railway/Vercel settings and protected secrets are configured without exposing values
- [ ] CI-only and protected-`main` deployment evidence is recorded
- [ ] Reviewer records evidence in `tasks/TASK_REVIEW_T001.md`
- [ ] Supervisor updates Kanban
