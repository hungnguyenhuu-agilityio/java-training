# PROJECT_KANBAN.md
**Last updated**: 2026-09-25

> Compact task board. Full context lives in `PROJECT_SPEC.md`; permanent execution contracts live in `tasks/TASK_GUIDE_Txxx.md`.

## Board

### Todo
- [ ] **T003** — Registration, login, and secure session lifecycle | Backend + Frontend | C3 | Risk: High | P0
- [ ] **T004** — Protected catalog administration | Backend + Frontend | C2 | Risk: High | P0
- [ ] **T005** — Authenticated shopping-cart journey | Backend + Frontend | C2 | Risk: Medium | P0
- [ ] **T006** — Warehouse-scoped inventory administration | Backend + Frontend | C3 | Risk: High | P0
- [ ] **T007** — Warehouse allocation and atomic reservation | Backend-Implementer | C3 | Risk: High | P0
- [ ] **T008** — Stripe checkout and verified payment confirmation | Backend + Frontend | C3 | Risk: High | P0
- [ ] **T009** — Concurrent reservation and no-overselling evidence | QA-Automation-Agent | C3 | Risk: High | P0
- [ ] **T010** — Asynchronous confirmed-order notification request | Backend-Implementer | C2 | Risk: Medium | P1
- [ ] **T011** — Customer order history and ownership protection | Backend + Frontend | C2 | Risk: High | P0
- [ ] **T012** — Administrator fulfillment, cancellation, and refund | Backend + Frontend | C3 | Risk: High | P1
- [ ] **T013** — Low-stock scanning and transition-based alerting | Backend-Implementer | C2 | Risk: Medium | P1
- [ ] **T014** — Secured operational metrics and manual controls | Backend-Implementer | C2 | Risk: High | P1
- [ ] **T015** — Full-stack CI/CD hardening and release readiness | QA + Common Infrastructure | C3 | Risk: High | P0

### In Progress

- [ ] **T001** — Modular runtime, database, UI shell, and main-only CI/CD foundation | Common Infrastructure + Frontend | C3 | Risk: High | P0 — 🔄 2026-09-14: backend namespace changed to `com.training.ecommerce`, Maven group to `com.training`, shared module renamed to `shareddomain`, and module detection limited to the eight explicit capability roots. Playwright and its CI/visual assets were removed by user direction; F16–F19 have Angular regression coverage, but fresh real-browser accessibility/responsiveness evidence is now explicitly unverified. Backend verify passes (18 run, 1 Docker-gated skip), frontend passes (8 tests + production build), CI policy passes, and all Compose services are container-healthy; host-loopback smoke is environment-blocked in this managed session. Targeted Stage 4 review, HTML reports, O7 human setup, and real release/rollback evidence remain. Not Done. 🔄 2026-09-25: code-review round 3 fixed a P1 MySQL healthcheck race (cold-start backend crash); host + nginx `/api` smoke now PASS on two cold starts. Real-browser F16–F19 + 375/768/1280 evidence PASS via easy-ui-mcp Playwright container. Open: P2 Escape-test gap, HTML reports; O7 + real release/rollback deferred by user 2026-09-25, O7, real release/rollback. See `tasks/TASK_REVIEW_T001.md`

### Ready for Review

None.

### Done

- [x] **T002** — Public product catalog journey | Backend + Frontend | C2 | Risk: Medium | P0 — ✅ 2026-09-25 Done (Stage 5). Evidence 9/9 pass in `tasks/TASK_REVIEW_T002.md`: backend verify 74/0/3, frontend 28 tests + build, NFR-013 target p95 92 ms, real-browser 42/42 at 375/768/1280, `/verify` PASS through the `/api` proxy. Stage 4: code-review P1×2 + P2 fixed (P3×4 optional open), security-review no vulnerabilities, `/api` proxy prefix strip fixed (user decision), 2 visual defects fixed. Reports: `reports/code-review_develop_20260925T155300.html`, `reports/security-review_develop_20260925T155312.html`, `reports/delivery-report_develop_20260925T101714.html`. **Uncommitted in working tree — user commits.**

## Blocked

| Task | Reason | Waiting on |
|---|---|---|
| — | No non-task blockers recorded | — |

## Stage Tracker

| Stage | Status |
|---|---|
| 0.5 Brainstorming | ✅ Done |
| 1 Environment Setup | ✅ Done |
| 1.5 Sub-Agent Architecture | ✅ Done |
| 2 Planning (/plan) | ✅ Done |
| 3 Execution | 🔄 In Progress |
| 4 Review | 🔄 In Progress (T002 ✅; T001 HTML reports open) |
| 5 Integration & Verify | 🔄 In Progress (T002 ✅) |
