# TASK_GUIDE — T010: Asynchronous Confirmed-Order Notification Request
**Date**: 2026-09-08
**Complexity Level**: C2
**Risk Level**: Medium
**Priority**: P1
**Execution Type**: AFK
**Assigned agent**: Backend-Implementer
**Agent guide**: `agents/backend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/backend/general/codebase-map files. Apply C2 process; run `migration-safety` before persistence changes.

## Requirement (Pillar 1 — Adapt the requirement)

Create a recoverable asynchronous notification request from the verified `OrderConfirmed` fact.

**Restated intent**: Confirming an order commits independently of notification processing, while duplicates create at most one logical request and delivery can retry through a test adapter.  
**Out of scope**: Real email, templates, multiple channels, daily reports, and notification UI.  
**Requirement Refs**: US-010A, FR-014, NFR-007, NFR-008A.

### Requirement Fidelity Gate

- [x] Notification milestone and test-adapter scope approved
- [x] Criteria trace to refs
- [x] `OrderConfirmed` terminology aligns with glossary

## Dependencies & Reachability

**Depends on**: T008 — verified payment confirms the order  
**Entry point**: `OrderConfirmed`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Verified order confirmation durably creates one deduplicated notification request after the order transition. | FR-014 |
| 2 | Notification failure or process crash does not roll back or block confirmed order state. | FR-014 |
| 3 | Duplicate facts and retries produce at most one logical delivery; retry state is observable. | FR-014, NFR-008A |
| 4 | A test adapter proves payload intent without external delivery. | FR-014 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Duplicate `OrderConfirmed` facts | One request and one logical test delivery | Integration test |
| Adapter failure/crash boundary | Order remains confirmed and request remains retryable | Fault-injection test |

```bash
cd backend && ./mvnw test -Dtest='*OrderConfirmed*Test,*Notification*IT'
```

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T010.md` at Stage 4/5.

## Demonstration

**BEFORE**: No durable confirmed-order notification request, deduplication behavior, retry state, or test delivery adapter exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: DDR-0001 durable-event restraint and T008 confirmed fact.  
**Vital slice**: Confirmed fact → durable request → test adapter.  
**Cut list**: No SMTP/provider, HTML template, generic multichannel framework, or daily report.

## Edge Case Checklist

- [ ] Event transaction commits but consumer crashes.
- [ ] Same fact is published concurrently twice.
- [ ] Retry overlaps a slow first attempt.
- [ ] Payload or telemetry exposes personal data.

## Files to Change (Predicted)

Ordering confirmed event publication, notification module/request persistence/test adapter/tests, and migrations.

## Files Must NOT Touch

External email configuration, payment truth rules, frontend, and daily reporting.

## Test Plan

Transaction-boundary, deduplication, retry, crash/failure, payload-minimization, and metrics tests.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Migration-safety, code-review, security-review, blast-radius if PII persists, and verify pass
- [ ] Reviewer evidence recorded in `TASK_REVIEW_T010.md`
- [ ] Supervisor updates Kanban
