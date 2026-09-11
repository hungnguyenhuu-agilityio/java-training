# PROJECT_SPEC.md
**Last updated**: 2026-09-11
**Version**: 1.3

> **Scope of this document**: How to build the product safely. Product intent, personas, user stories, functional requirements, non-functional requirements, and success metrics live in `PRD.md`.

---

## Project Identity

- **Name**: E-Commerce Java Practice
- **Repositories**: `gitlab` (`git@gitlab.asoft-python.com:hung.nguyenhuu/java-training.git`) and `github` (`git@github-agilityio:hungnguyenhuu-agilityio/java-training.git`)
- **Primary technology**: Java 21, Spring Boot 4.1.1, Angular 22.1, MySQL 8.4.11
- **Type**: Full-stack web application and senior Java/Spring portfolio project
- **Local/CI runtime**: Docker Compose
- **Deployment targets**: Railway backend and Vercel frontend
- **Delivery automation**: GitHub Actions
- **Stakeholders**: Hung Nguyen Huu, technical reviewers, and prospective clients

---

## Architecture Summary

The backend is a package-enforced modular monolith organized by business capability: identity, catalog, cart, inventory, ordering, payment, and a deliberately small shared kernel. Each module applies Clean Architecture internally, with domain and application logic independent of Spring MVC, JPA, Stripe, and deployment details. Spring Modulith and focused architecture tests enforce module APIs, acyclic dependencies, and inward dependency direction.

The Angular application uses standalone components and feature-oriented boundaries for authentication, catalog, cart, checkout, orders, and administration. A Vercel same-origin `/api` proxy fronts the Railway backend so short-lived access tokens can remain in memory while rotated refresh tokens use secure, HttpOnly cookies.

`UI_SPEC.md` is the canonical visual and interaction contract. It defines a lean, light-only Angular Material interface with one role-aware shell, desktop-first responsive behavior, task-scoped ASCII wireframes, and WCAG 2.2 AA as the accessibility target. Backend/OpenAPI contracts remain authoritative for fields, validation, permissions, and state transitions.

MySQL is the single transactional store. Liquibase owns all schema changes. Stripe participates through a provider-neutral payment port; no database transaction may remain open across provider calls. Checkout and refund consistency use local ACID transitions, idempotency, verified webhooks, compensation, and reconciliation.

---

## Module Boundaries

| Module | Owns | May expose |
|---|---|---|
| `identity` | Users, roles, credentials, refresh sessions | Authentication and current-user application contracts |
| `catalog` | Categories, products, prices, activation | Public catalog queries and protected catalog-management contracts |
| `cart` | Active customer carts and cart items | Cart commands and cart snapshot queries |
| `inventory` | Warehouses, warehouse-scoped stock, safety thresholds, allocation, and expiring reservations | Allocate, reserve, release, commit-reservation, stock-maintenance, and low-stock-scan contracts |
| `ordering` | Orders, order items, selected-warehouse and shipping snapshots, fulfillment state/history | Checkout/order queries, administrator transition contracts, and confirmed-order facts |
| `payment` | Payment sessions, payment/refund state, Stripe event handling | Provider-neutral payment and refund application contracts |
| `notification` | Deduplicated notification requests and delivery attempts | Confirmed-order and low-stock notification application contracts |
| `sharedkernel` | Stable cross-module primitives only | Identifiers, money/currency, and domain-event primitives when genuinely shared |

Module roots expose intentional application contracts. Domain types, persistence adapters, controllers, and provider implementations remain internal unless a reviewed dependency requires otherwise. `sharedkernel` is not a utilities package.

---

## Design Rules

- Aggregate roots enforce state transitions and invariants; controllers and JPA callbacks do not contain business workflows.
- Repositories exist at aggregate persistence boundaries, not automatically for every table.
- API DTOs, JPA entities, and domain objects are separate when their responsibilities differ; mappings stay at adapters.
- Application use cases own local transaction boundaries.
- Stripe is implemented behind a `PaymentGateway` output port.
- Checkout/refund coordination uses a process manager; external failures are handled through explicit compensation and reconciliation.
- Durable events are used only where post-commit recovery is required. Synchronous module API calls remain the default for simple local coordination.
- Product identity remains global to `catalog`; physical quantity belongs to an inventory item identified by warehouse and product.
- Checkout selects one active warehouse that can fulfill the complete order. It maximizes the lowest remaining stock among the requested products after reservation and breaks ties by ascending warehouse code; split fulfillment and customer warehouse selection are prohibited.
- Schedulers and manual debug adapters invoke the same application use cases. Manual operational triggers must be absent from production.
- Low-stock alerts are transition-based: emit once on entry into the low state, suppress while unchanged, reset after recovery, and allow a later downward crossing to alert again.
- Customer confirmation reacts to an `OrderConfirmed` fact created only after verified payment; notification failure cannot reverse the order transition.
- GoF State classes, generic CRUD layers, generic repositories, mediator frameworks, and speculative interfaces are prohibited unless a task guide names the concrete variability they solve.
- Catalog filter composition may use the Specification pattern when combinations justify it.
- Every architectural rule must have an executable test where technically feasible.
- UI-bearing tasks must follow `UI_SPEC.md`; agents may not invent fields, permissions, state transitions, screen behavior, or an additional component library.

---

## Database Direction

The current `dbdiagram.io` file is an input, not a migration-ready final schema. Before Liquibase implementation it must be revised to include:

- payment, refund, warehouse, warehouse-inventory, inventory-reservation, notification-request, processed-webhook-event, and refresh-session persistence;
- provider references, ISO currency, payment/refund states, request fingerprints, expiries, and optimistic versions;
- the approved order lifecycle and immutable structured shipping snapshot;
- positive quantity, non-negative stock/money, uniqueness, and state-supporting constraints;
- explicit foreign-key actions that preserve order history;
- UTC timestamp semantics and consistent precision; and
- warehouse/product uniqueness and constraints ensuring reserved quantity remains between zero and on-hand quantity;
- indexes supporting catalog filters, warehouse allocation, low-stock scans, reservation expiry, notification deduplication, provider lookups, webhook deduplication, and order history.

The exact table layout must pass the migration-safety gate before implementation.

---

## Critical Constraints

- Spring Boot 4.1.1 is authoritative; older 3.5.16 documentation references are stale.
- Java remains at version 21 unless a later documented decision changes it.
- Liquibase is the only production schema mutation mechanism; Hibernate schema auto-update is prohibited.
- Never hold a database transaction open while calling Stripe or any external service.
- Never trust the Stripe browser return as proof of payment or refund.
- Webhooks require raw-payload signature verification, replay protection, durable deduplication, and out-of-order handling.
- Access tokens must not be persisted in browser storage. Refresh tokens must be hashed, rotated, HttpOnly, Secure, revocable, and protected against CSRF and reuse.
- Referenced products and categories must not be hard-deleted through the product API.
- Secrets, tokens, webhook bodies, and personal shipping data must not leak through source control, Problem Details, logs, metrics, traces, or reports.
- Business metrics must use bounded outcome/type tags and must not use customer, order, product, warehouse, email, or other entity identifiers as tags.
- Concurrency correctness must be proven against the real database behavior; virtual threads do not replace locking, isolation, bounded concurrency, or backpressure.
- No production endpoint may create arbitrary simulated traffic. Any manual scan adapter must be profile-gated outside production and authorized.
- The full-stack local and CI workflow must remain reproducible with Docker Compose.
- GitHub Actions runs CI for pull requests and pushes, but deployment is reachable only from a successful protected-`main` merge. That single release path deploys the Railway backend and Vercel frontend, then runs post-deployment smoke checks; feature branches and pull requests never deploy.
- Production deployment jobs must consume protected GitHub environment secrets, serialize releases, report partial Railway/Vercel failure as a failed release, and retain an actionable rollback path compatible with applied Liquibase migrations.
- Implementation cannot start without an approved `TASK_GUIDE_Txxx.md` and test-first acceptance criteria.

---

## Known Risk Areas

| Area | Risk | Reason | Expected location |
|---|---|---|---|
| Authentication/session rotation | High | Credential lifecycle, CSRF, concurrent refresh, and cross-origin deployment behavior | `backend/.../identity`, `frontend/src/app/core` |
| Stripe webhooks and refunds | High | Untrusted signed input, duplicate/out-of-order events, and cross-system recovery | `backend/.../payment` |
| Checkout/inventory | High | Concurrent reservations, overselling, expiry races, money snapshots, and idempotency | `backend/.../cart`, `inventory`, `ordering` |
| Warehouse allocation | High | Cross-item availability, deterministic selection, concurrent reservations, and deactivation races | `backend/.../inventory`, `ordering` |
| Asynchronous notifications/jobs | Medium | Duplicate events, missed work, overlapping schedules, retry storms, and false success messages | `backend/.../ordering`, `inventory`, `notification` |
| Database migrations | High | New schema, constraints, production compatibility, and rollback safety | `backend/src/main/resources/db/changelog` |
| Authorization/ownership | High | Customer data isolation and administrator boundaries | Backend application/web adapters and Angular guards |
| Vercel/Railway integration | Medium | Cookie, proxy, CORS, secret, webhook, and rollback configuration | `frontend` deployment config, Docker, GitHub Actions |
| Catalog querying | Medium | Filtering correctness and performance at the approved reference dataset | `backend/.../catalog` |
| Architecture enforcement | Medium | Module leakage and framework dependencies entering domain code | Backend package layout and architecture tests |

---

## Candidate Domain Model

No domain-model source files exist in the current scaffold. The PRD and selected architecture imply these initial candidates, pending Stage 1 confirmation:

- Aggregates: `User`, `RefreshSession`, `Product`, `Category`, `Cart`, `Warehouse`, `WarehouseInventory`, `InventoryReservation`, `Order`, `Payment`, `Refund`, `NotificationRequest`.
- Entities: `CartItem`, `OrderItem`, `OrderStatusHistory` where aggregate rules justify entity identity.
- Value objects: `EmailAddress`, `Money`, `Currency`, `Quantity`, `Sku`, `OrderCode`, `ShippingAddress`, `IdempotencyKey`, provider identifiers.
- Services/policies: checkout coordination, warehouse allocation, catalog filtering, order transitions, reservation expiry, low-stock detection, notification deduplication, payment reconciliation, and refund coordination.

The list is conceptual; aggregate ownership and boundaries are finalized during Stage 2 planning, before schema or implementation tasks begin.

---

## Sub-Agent Team

Approved in Stage 1.5. Codex is the primary CLI; Claude is reserved for selected brainstorming, review, or specialist sessions.

| Agent | Role | Guide | CLI spawn template |
|---|---|---|---|
| Common-Infrastructure-Agent | Worktrees, Docker, CI/CD, migrations, Railway/Vercel setup | `agents/common-infrastructure.md` | `codex exec -C <worktree> --sandbox workspace-write "Task Txxx. Read PROJECT_SPEC.md, the task guide, agents/common-infrastructure.md, and memory/MEMORY.md; execute only the guide."` |
| Backend-Implementer | Spring modules, domain behavior, APIs, persistence, Stripe adapter | `agents/backend.md` | `codex exec -C <worktree> --sandbox workspace-write "Task Txxx. Read PROJECT_SPEC.md, the task guide, agents/backend.md, and memory/MEMORY.md; execute the guide test-first."` |
| Frontend-Implementer | Angular features, session handling, API integration, UX | `agents/frontend.md` | `codex exec -C <worktree> --sandbox workspace-write "Task Txxx. Read PROJECT_SPEC.md, the task guide, agents/frontend.md, and memory/MEMORY.md; execute the guide test-first."` |
| QA-Automation-Agent | Independent acceptance, integration, browser, performance, and failure testing | `agents/qa.md` | `codex exec -C <worktree> --sandbox workspace-write "Task Txxx. Read PROJECT_SPEC.md, the task guide, agents/qa.md, and memory/MEMORY.md; verify the guide independently."` |

Before any spawn, replace `Txxx`, `<worktree>`, and `the task guide` with the exact task ID, worktree path, and `tasks/TASK_GUIDE_Txxx.md` path. Before the first Claude session, remind the user about the deferred project-local Git guardrail.

---

## Tasks

Stage 2 decomposes the approved product into 15 dependency-ordered tracer-bullet slices. The compact state is in `PROJECT_KANBAN.md`; each permanent execution contract is in `tasks/TASK_GUIDE_Txxx.md`.

| Range | Milestone | Outcome |
|---|---|---|
| T001–T005 | Foundation and customer preparation | Runnable modular stack, main-only CI/CD foundation, public catalog, secure identity, catalog administration, and cart |
| T006–T010 | Warehouse-safe paid checkout | Warehouse inventory, allocation/reservation, Stripe confirmation, concurrency proof, and notification request |
| T011–T014 | Order operations and observability | Owned order history, administrator fulfillment/refunds, low-stock alerts, and secured metrics |
| T015 | Delivery gate | Independent full-stack CI/CD hardening, rollback exercise, and release evidence verification |

---

## Memory / Insights

| Date | Insight | Source |
|---|---|---|
| 2026-09-08 | Product direction prioritizes demonstrable Clean Architecture, OOP, database design, purposeful patterns, Angular delivery, and operational evidence. | Phase 0 / `STRATEGY.md` |
| 2026-09-08 | Path A selected: a package-enforced modular monolith, not Maven layer modules or microservices. | Stage 0.5 / `BRAINSTORMING_LOG.md` |
| 2026-09-08 | Payment and fulfillment are separate state machines; Stripe webhooks own payment truth. | Requirement grilling |
| 2026-09-08 | Inventory is reserved before Stripe redirect and released on payment failure or expiry; successful payment commits the reservation. | Requirement grilling |
| 2026-09-08 | Low-stock alert suppression is state-based rather than time-based; the user delegated this choice to the Supervisor's recommended standard due to limited domain experience. | Requirement grilling |
| 2026-09-08 | Stage 2 produced 15 dependency-ordered task guides: 13 AFK and 2 HITL; warehouse/concurrency work is integrated after its real catalog, identity, cart, and payment prerequisites. | Stage 2 planning |
| 2026-09-08 | The Claude destructive-Git guardrail is deferred by user request; remind the user before opening the first Claude worktree/session. | Stage 1 |
| 2026-09-11 | T001 now establishes CI for pushes/pull requests and the only production deployment path: a successful protected-`main` merge deploys Railway and Vercel once, followed by smoke checks. T015 independently hardens that pipeline and proves rollback readiness. | Stage 2 revision / user decision |
| 2026-09-11 | Path A selected for UI: a lean light-only Angular Material interface, shared role-aware shell, desktop-first responsive evidence, backend-owned contracts, English copy, backend-defined currency, and WCAG 2.2 AA. | UI brainstorming and terminology grilling |
| 2026-09-11 | UI_SPEC review: role-based visibility (`CUSTOMER` unlocks cart/checkout/orders, `ADMIN` does not imply it), guest→login / missing-role→forbidden guards, AA-compliant warning and control-border tokens, bounded 2s/30s payment-result polling, inactive cart-line handling, single-currency stop rule, and baselines committed after each task's Stage 4. | Supervisor UI_SPEC review / user approval |
