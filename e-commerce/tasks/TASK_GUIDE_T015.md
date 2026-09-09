# TASK_GUIDE — T015: Full-Stack Verification, CI, and Deployment Readiness
**Date**: 2026-09-08
**Complexity Level**: C3
**Risk Level**: High
**Priority**: P0
**Execution Type**: HITL
**Assigned agents**: QA-Automation-Agent (oracle), Common-Infrastructure-Agent
**Agent guides**: `agents/qa.md`, `agents/common-infrastructure.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/general/QA/infrastructure/codebase-map files. Apply C3 decomposition/brainstorming and independence rules. Do not conceal failing feature evidence with infrastructure retries.

## Requirement (Pillar 1 — Adapt the requirement)

Create the independent delivery gate proving the approved application builds, tests, runs, deploys, rolls back, and completes the critical customer/admin journeys.

**Restated intent**: One CI-friendly workflow verifies all task acceptance suites, coverage/mutation thresholds, Compose runtime, secured operations, deployed Browse→Cart→Checkout→Orders behavior, and rollback readiness.  
**Out of scope**: Implementing missing feature behavior, automatic production release, daily reports, and deferred email delivery.  
**Requirement Refs**: US-010, US-011, FR-013, NFR-007, NFR-009, NFR-010, NFR-011, NFR-012, NFR-013, NFR-014.

### Requirement Fidelity Gate

- [x] Delivery targets and quality thresholds approved
- [x] Criteria trace to all refs
- [x] Independent-oracle responsibility explicit

## Dependencies & Reachability

**Depends on**: T001–T014 — all feature and operational slices  
**Entry point**: `scripts/verify-build.sh`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | One CI-friendly command runs backend, frontend, integration, security, browser, concurrency, coverage, and required mutation suites. | NFR-007, NFR-012, NFR-014 |
| 2 | Docker Compose starts the complete stack from a clean environment and critical customer/admin journeys pass. | FR-013, NFR-009 |
| 3 | Reference dataset performance targets pass with no oversell, duplicates, or >1% server errors. | NFR-013 |
| 4 | GitHub Actions supports controlled Railway/Vercel deployment with protected secrets and explicit rollback procedure. | NFR-010, NFR-011 |
| 5 | Deployed smoke checks verify frontend/backend integration and secured health/metrics without leaking sensitive data. | US-011, FR-013 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Clean CI/local environment | All suites and evidence thresholds pass from one command | Independent gate run |
| Deployment or smoke failure | Pipeline stops and rollback procedure restores last known good version | Rollback exercise |

```bash
./scripts/verify-build.sh
```

## UI / Design Acceptance Criteria

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Automated critical-route screenshots/diffs | No unapproved regressions |
| Design-system compliance | Automated token/component audit | All feature screens comply |
| Responsiveness | Browser suite at 375px, 768px, 1280px | Critical journeys pass at each viewport |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T015.md` at Stage 4/5.

## Demonstration

**BEFORE**: No single independent command proves all suites, clean Compose startup, critical browser journeys, deployment readiness, and rollback.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: All preceding task acceptance commands and `templates/TASK_REVIEW_template.md`.  
**Vital slice**: Clean build → Compose smoke → critical browser flow → rollback proof.  
**Cut list**: No feature fixes by QA, auto-production release, external email, or daily report.

## Edge Case Checklist

- [ ] CI passes with cached/generated artifacts missing from clean checkout.
- [ ] H2 passes while MySQL locking/migrations fail.
- [ ] deployment health passes but proxy/cookie/webhook route fails.
- [ ] rollback omits database compatibility.
- [ ] secrets or PII appear in artifacts/logs/metrics.

## Files to Change (Predicted)

Independent smoke/browser/performance/security suites, `scripts/verify-build.sh`, Docker/CI/deployment configuration, rollback/runbook documentation, and review evidence files.

## Files Must NOT Touch

Feature production code except separately triaged fixes returned to owning tasks; production deployment must not execute automatically.

## Test Plan

Clean-environment build, all targeted/full suites, coverage/mutation, Compose, browser viewports, reference-load, security exposure, preview deployment, and rollback exercise.

## Completion Checklist

- [ ] Independent gate and all new tests pass
- [ ] Code-review, security-review, blast-radius, and verify pass
- [ ] UI/reviewer evidence recorded in `TASK_REVIEW_T015.md`
- [ ] Deployment/rollback evidence recorded
- [ ] Supervisor updates Kanban and only then invokes release planning
