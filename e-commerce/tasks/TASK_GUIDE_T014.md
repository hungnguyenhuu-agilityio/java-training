# TASK_GUIDE — T014: Secured Operational Metrics and Manual Controls
**Date**: 2026-09-08
**Complexity Level**: C2
**Risk Level**: High
**Priority**: P1
**Execution Type**: AFK
**Assigned agent**: Backend-Implementer
**Agent guide**: `agents/backend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/backend/general/codebase-map files. Apply C2 process and plan security/blast-radius review.

## Requirement (Pillar 1 — Adapt the requirement)

Expose secured health and bounded-cardinality business metrics, with manual operational controls absent from production.

**Restated intent**: Operators and reviewers can observe reservation, stock, notification, and job outcomes without exposing sensitive Actuator data, entity identifiers, or workload-generation controls.  
**Out of scope**: Production traffic simulator, arbitrary thread counts, per-entity tags, external monitoring vendor, and performance optimization.  
**Requirement Refs**: US-010A, NFR-002, NFR-008, NFR-008A.

### Requirement Fidelity Gate

- [x] Security/cardinality boundaries approved
- [x] Criteria trace to refs
- [x] Existing Actuator/Micrometer direction preserved

## Dependencies & Reachability

**Depends on**: T009 — concurrency outcome taxonomy; T010 — notification outcomes; T013 — job outcomes/manual scan  
**Entry point**: `GET /actuator/metrics`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Health and approved metric endpoints are authenticated/authorized according to environment; sensitive Actuator endpoints are not public. | NFR-008 |
| 2 | Counters distinguish reservation success/out-of-stock/conflict, notification outcomes, and timers measure scheduled jobs. | NFR-008A |
| 3 | Tag allowlist excludes customer/order/product/warehouse/email IDs and other unbounded or sensitive values. | NFR-002, NFR-008A |
| 4 | Manual low-stock trigger exists only outside production, is authorized, bounded, and cannot create arbitrary traffic. | NFR-008 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Anonymous/ADMIN and dev/prod profiles | Exact endpoint/control exposure matrix holds | Security integration test |
| Representative operations | Expected bounded metric names/tags/counts/timers | Meter-registry test |

```bash
cd backend && ./mvnw test -Dtest='*ActuatorSecurity*IT,*BusinessMetrics*Test,*ManualOperations*IT'
```

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T014.md` at Stage 4/5.

## Demonstration

**BEFORE**: Actuator is a dependency, but approved endpoint security, business meters, tag allowlist, and profile-gated manual controls do not exist.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: `backend/pom.xml` Actuator dependency and PRD NFR-008A.  
**Vital slice**: Secured health/metrics plus reservation and job instrumentation.  
**Cut list**: No monitoring vendor, dashboards, per-entity tags, traffic endpoint, or optimization.

## Edge Case Checklist

- [ ] Production profile accidentally creates manual controller bean.
- [ ] Exception paths bypass outcome counters.
- [ ] URI or exception tags become unbounded.
- [ ] Health details expose database/configuration secrets.

## Files to Change (Predicted)

Actuator/security/profile configuration, metric instrumentation/adapters/tests, and non-production manual operations adapter.

## Files Must NOT Touch

Production workload controls, secrets, core inventory invariants, and external monitoring configuration.

## Test Plan

Endpoint exposure matrix, role/profile tests, meter assertions, cardinality allowlist, redaction, and manual-trigger bounds.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Code-review, security-review, blast-radius, and verify pass
- [ ] Reviewer evidence recorded in `TASK_REVIEW_T014.md`
- [ ] Supervisor updates Kanban
