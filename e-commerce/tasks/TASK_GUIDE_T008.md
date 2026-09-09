# TASK_GUIDE — T008: Stripe Checkout and Verified Payment Confirmation
**Date**: 2026-09-08
**Complexity Level**: C3
**Risk Level**: High
**Priority**: P0
**Execution Type**: HITL
**Assigned agents**: Backend-Implementer (lead), Frontend-Implementer
**Agent guides**: `agents/backend.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read mandatory project/memory/task/general/backend/frontend/codebase-map files. Apply C3 decomposition and brainstorming. Run `migration-safety`; plan security and payment-data blast-radius review.

## Requirement (Pillar 1 — Adapt the requirement)

Complete checkout through a provider-neutral port and Stripe-hosted session, using verified webhook facts as payment truth.

**Restated intent**: Local order/payment/reservation state remains recoverable across provider timeouts, retries, duplicate/out-of-order webhooks, browser returns, cancellation, and expiry.  
**Out of scope**: Inline card collection, multiple providers, notification delivery, partial capture/refund, and fulfillment administration.  
**Requirement Refs**: US-005A, FR-006A, FR-006B, FR-010, FR-011, NFR-006A, NFR-006B, NFR-006C.

### Requirement Fidelity Gate

- [x] Provider-neutral and webhook-truth rules approved
- [x] Criteria trace to all payment requirements
- [x] Terms align with DDR-0002

## Dependencies & Reachability

**Depends on**: T003 — identity; T007 — local order/reservation  
**Entry point**: `POST /api/checkout/session`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Application depends on `PaymentGateway`; Stripe types remain in its adapter and no DB transaction spans a provider call. | NFR-006A, NFR-006C |
| 2 | Session/refund commands use deterministic idempotency and interrupted calls reconcile without duplicate effects. | FR-006B |
| 3 | Raw-body signatures, replay protection, durable deduplication, and out-of-order handling guard webhooks. | NFR-006B |
| 4 | Only a verified successful webhook commits reservation and confirms order; browser return only informs UX. | FR-006A |
| 5 | Angular checkout submits once, redirects, and displays pending/confirmed/cancelled/error states safely. | FR-011 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Duplicate/out-of-order signed fixtures | One legal payment/order/stock transition | Integration test |
| Invalid signature or lost provider response | Rejection or recoverable state without duplicate charge/order | Security/failure test |

```bash
(cd backend && ./mvnw test -Dtest='*Payment*Test,*Stripe*IT,*CheckoutSession*IT') && (cd frontend && npm test -- --watch=false --include='src/app/features/checkout/**/*.spec.ts' && npm run build)
```

## UI / Design Acceptance Criteria

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Checkout/result screenshots | Stable pending/success/cancel/error states |
| Design-system compliance | Form/token audit | Shared controls and safe errors |
| Responsiveness | 375px, 768px, 1280px | Checkout remains usable |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T008.md` at Stage 4/5.

## Demonstration

**BEFORE**: No payment domain, Stripe adapter, hosted checkout flow, verified webhook processor, or checkout result UI exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: `docs/ddr/0002-provider-neutral-payment-transactions.md`.  
**Vital slice**: Reservation → hosted session → verified success fixture → confirmed order.  
**Cut list**: No alternate provider, inline card UI, partial payment/refund, or customer notification.

## Edge Case Checklist

- [ ] Provider creates session but response is lost.
- [ ] Webhook is duplicated concurrently or arrives before browser return.
- [ ] Payment succeeds at reservation-expiry boundary.
- [ ] Raw-body parsing invalidates signature verification.

## Files to Change (Predicted)

Payment module/Stripe adapter/migrations/webhook/config/tests, checkout coordination, and Angular checkout/result routes/tests.

## Files Must NOT Touch

Stripe secrets in source, catalog administration, notification delivery, and deployment release logic.

## Test Plan

Gateway contract, signed webhook fixtures, real-DB state transitions, fault injection, Angular submission/return, and browser sandbox smoke tests.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Migration-safety, code-review, security-review, blast-radius, and verify pass
- [ ] UI/reviewer evidence recorded in `TASK_REVIEW_T008.md`
- [ ] Supervisor updates Kanban
