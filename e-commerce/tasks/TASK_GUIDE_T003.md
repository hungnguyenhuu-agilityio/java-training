# TASK_GUIDE — T003: Registration, Login, and Secure Session Lifecycle
**Date**: 2026-09-11
**Complexity Level**: C3
**Risk Level**: High
**Priority**: P0
**Execution Type**: AFK
**Assigned agents**: Backend-Implementer (lead), Frontend-Implementer
**Agent guides**: `agents/backend.md`, `agents/frontend.md`

## Mandatory Startup (Do Not Skip)

Read all mandatory project/memory/guide files and `memory/codebase-map.md`. Apply C3 decomposition and brainstorming. Run `migration-safety` before schema work and plan security/blast-radius review.

## Requirement (Pillar 1 — Adapt the requirement)

Implement registration, login, authorization, refresh rotation, silent renewal, replay response, and logout across the same-origin frontend proxy.

**Restated intent**: Users authenticate without browser-stored tokens; backend authorization, ownership, CSRF protection, rotation, and family revocation remain authoritative.  
**Out of scope**: Social login, MFA, password reset, and warehouse-specific roles.  
**Requirement Refs**: US-003, FR-003, FR-010, FR-011, FR-012, NFR-001, NFR-002, NFR-002A.

### Requirement Fidelity Gate

- [x] Intent and terms approved
- [x] Criteria cover every referenced security requirement
- [x] All references exist in `PRD.md`

## Dependencies & Reachability

**Depends on**: T001 — runtime and database foundation  
**Entry point**: `POST /api/auth/login`

## Acceptance Criteria

| # | Criterion | Trace |
|---|---|---|
| 1 | Registration and login use adaptive password hashing and issue an in-memory access token plus Secure HttpOnly refresh cookie. | FR-003, NFR-002 |
| 2 | Refresh rotates hashed tokens; reuse revokes the family; concurrent refresh has one defined safe result. | NFR-002A |
| 3 | Reload silently refreshes, one expired request retries once, and failure/logout clears authentication state. | FR-003, FR-011 |
| 4 | Backend roles, ownership, CSRF, and safe Problem Details are enforced independently of the UI. | NFR-001, FR-010 |
| 5 | Generated OpenAPI accurately describes registration, login, refresh, and logout contracts, including bearer authorization, refresh-cookie/CSRF requirements, and safe Problem Details without secret-bearing examples. | FR-012, FR-010, NFR-002 |
| 6 | The authenticated-user response exposes the account's roles; Angular derives navigation and guard visibility from them (guest → `/login`, missing role → forbidden page) per `UI_SPEC.md` §4.3. | FR-003, FR-011, NFR-001 |

## Evaluation & Acceptance

| Given | Expect | Check |
|---|---|---|
| Valid login and reload | Session restores without local/session storage tokens | Security integration + browser test |
| Replayed refresh token or cross-site request | Family revoked or request rejected without secret leakage | Adversarial tests |

```bash
(cd backend && ./mvnw test -Dtest='*Identity*Test,*Auth*IT,*Security*IT') && (cd frontend && npm test -- --watch=false --include='src/app/{core,features/auth}/**/*.spec.ts' && npm run build)
```

## UI / Design Acceptance Criteria

**UI specification**: [`UI_SPEC.md` §8 — T003 Authentication and Session UI](../UI_SPEC.md#t003-authentication)

| Evidence | Method | Expected result |
|---|---|---|
| Visual regression | Automated login/register/error screenshots | Stable states without secret exposure |
| Design-system compliance | Token/form audit | Approved controls and errors |
| Responsiveness | 375px, 768px, 1280px | Forms remain usable and readable |

### Evidence

To be filled by the independent reviewer in `tasks/TASK_REVIEW_T003.md` at Stage 4/5.

## Demonstration

**BEFORE**: No identity domain, authentication endpoints, secure refresh lifecycle, auth state, interceptor, or guarded route exists.  
**AFTER**: To be captured from the verified implementation.  
**DELTA**: To be derived from the before/after evidence.  
**WITNESS**: To be supplied by automated tests and a running-system check.

## Approach

**Pattern reference**: `PROJECT_SPEC.md` authentication constraints.  
**Vital slice**: Login → protected request → rotation → logout.  
**Cut list**: No OAuth, MFA, password reset, or persistent access-token storage.

## Edge Case Checklist

- [ ] Two tabs refresh the same family concurrently.
- [ ] Cookie/proxy/CORS configuration differs by environment.
- [ ] Email normalization disagrees with database uniqueness.
- [ ] Errors or logs expose credentials or tokens.

## Files to Change (Predicted)

Identity module, security/proxy configuration, identity migrations/tests, Angular auth/core state/interceptor/guards/routes/tests.

## Files Must NOT Touch

Catalog/inventory/order business rules and deployment credentials.

## Test Plan

Domain, persistence, MVC/security, generated OpenAPI structural assertions, CSRF/replay/concurrency, Angular state/guard/interceptor, and critical browser-flow tests.

## Completion Checklist

- [ ] Implementation and independent tests complete
- [ ] Migration-safety, code-review, security-review, blast-radius, and verify pass
- [ ] UI and reviewer evidence recorded in `TASK_REVIEW_T003.md`
- [ ] Supervisor updates Kanban
