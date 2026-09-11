# PRD — E-Commerce Java Practice
**Last updated**: 2026-09-08
**Status**: Approved — warehouse-scoped inventory amendment added
**Owner**: Hung Nguyen Huu

> **Scope of this document**: What to build and why. Technical decisions, architecture, agent configuration, and task state belong in `PROJECT_SPEC.md`.

---

## Overview

This project delivers a complete e-commerce experience for customers and administrators while serving as an evidence-backed demonstration of senior Java, Spring, database, Angular, testing, and delivery skills. It replaces isolated framework exercises with one coherent system whose customer journeys, security boundaries, data integrity, architecture, and deployment can all be demonstrated and verified.

---

## Personas

| ID | Name | Role | Pain Point |
|----|------|------|-----------|
| P1 | Customer | Shopper | Needs a clear and reliable way to discover products, manage a cart, place an order once, and review personal orders. |
| P2 | Administrator | Store operator | Needs controlled workflows for maintaining the catalog and managing orders without bypassing business rules. |
| P3 | Technical Reviewer | Senior-level evaluator | Needs concrete evidence that the developer can turn Java and Spring knowledge into a well-designed, secure, tested, observable, and deployable full-stack system. |
| P4 | Prospective Client | Delivery evaluator | Needs confidence that the developer can deliver a usable product across backend, frontend, database, and cloud operations. |

---

## User Stories

| ID | Story | Persona |
|----|-------|---------|
| US-001 | As a customer, I want to browse, search, sort, and filter products so that I can find relevant items. | P1 |
| US-002 | As a customer, I want to view product details so that I can make an informed choice. | P1 |
| US-003 | As a guest, I want to register and sign in so that I can use protected shopping features. | P1 |
| US-004 | As a customer, I want to add, change, remove, and clear cart items so that I can prepare an order. | P1 |
| US-005 | As a customer, I want checkout to create no more than one order and report stock conflicts clearly so that retries are safe. | P1 |
| US-005A | As a customer, I want to pay securely during checkout and receive an accurate payment result so that my order can proceed to fulfillment. | P1 |
| US-005B | As a customer, I want the system to allocate my complete order to an eligible warehouse without exposing internal fulfillment decisions so that stock can be reserved reliably. | P1 |
| US-006 | As a customer, I want to see only my own orders and their status histories so that I can track purchases securely. | P1 |
| US-007 | As an administrator, I want to maintain categories and products so that the catalog remains accurate. | P2 |
| US-008 | As an administrator, I want to review orders and apply only valid status changes so that fulfillment state remains trustworthy. | P2 |
| US-008A | As an administrator, I want to maintain warehouses, warehouse stock, and safety thresholds so that fulfillment availability remains accurate. | P2 |
| US-009 | As a user, I want consistent and useful error responses so that failures can be understood and corrected. | P1, P2 |
| US-010 | As a technical reviewer, I want traceable architecture, tests, API documentation, and operational evidence so that I can assess engineering competence. | P3 |
| US-010A | As a technical reviewer, I want deterministic evidence of inventory behavior under contention and recoverable asynchronous operations so that I can assess concurrency and reliability engineering. | P3 |
| US-011 | As a prospective client, I want a working deployed application so that I can evaluate end-to-end delivery capability. | P4 |

---

## Functional Requirements

Each functional requirement traces to at least one user story.

| ID | Requirement | Traces to |
|----|-------------|-----------|
| FR-001 | The system must provide a publicly accessible, paginated product catalog with text search, sorting, and category filtering. | US-001 |
| FR-002 | The system must provide a public product-detail view for active products. | US-002 |
| FR-003 | The system must allow customer registration and login and must protect authenticated and administrator capabilities by role. It must use a short-lived JWT access token held only in application memory and a rotating refresh token in a `Secure`, `HttpOnly` cookie through a same-origin frontend `/api` proxy. A page reload performs silent refresh; expiry permits one refresh-and-retry, and failure returns the user to login. Logout revokes the refresh session and clears client authentication state. Tokens must not be stored in browser local storage. | US-003 |
| FR-004 | An authenticated customer must be able to add, update, remove, and clear items in one active cart, with quantities and totals visible. | US-004 |
| FR-005 | Checkout must require recipient name, phone, address line 1, city or province, and ISO country code; address line 2 and postal code are optional. The order must retain these values as an immutable shipping snapshot. Checkout must also validate cart contents, create item and price snapshots, select one eligible active warehouse, and reserve all required stock atomically at that warehouse before redirecting to Stripe. Each reservation must expire, failed or expired Checkout sessions must release it, and a verified successful payment must convert it into committed inventory consumption. If no single warehouse can fulfill the complete order, checkout must be rejected without partial completion. | US-005, US-005B |
| FR-005B | Warehouse allocation must be an internal, deterministic policy based on active warehouses and sufficient available stock. Among eligible warehouses, select the warehouse whose requested products have the highest minimum remaining stock after reservation; break ties by ascending warehouse code. Customers do not select warehouses, and one order must not be split across warehouses. | US-005B |
| FR-006 | Retrying the same checkout operation must not create duplicate orders. | US-005 |
| FR-006A | Checkout must create a Stripe-hosted Checkout session, redirect the customer to Stripe, and return the customer to the Angular application after completion or cancellation. The backend must record provider-neutral payment and refund states and may confirm their outcomes only from authenticated Stripe webhooks, never from the browser return. | US-005A |
| FR-006B | A repeated payment request or provider callback must not create a duplicate charge, payment record, order, or order-state transition. | US-005, US-005A |
| FR-007 | An authenticated customer must be able to list and inspect only their own orders, including recorded status changes. | US-006 |
| FR-008 | An administrator must be able to create, edit, deactivate, and reactivate categories and products through protected workflows. Referenced catalog records must never be hard-deleted through the product API. Deactivated products disappear from public discovery but remain visible in historical order snapshots, and a category cannot be deactivated while it contains active products. | US-007 |
| FR-008A | An administrator must be able to create, edit, activate, and deactivate warehouses and maintain product quantities and safety-stock thresholds per warehouse. A warehouse with active reservations or assigned non-terminal orders must not be deactivated. Full warehouse operations such as bins, receiving, picking, packing, replenishment, transfers, and routing are excluded. | US-008A |
| FR-009 | An administrator must be able to list and inspect orders. Orders follow `PENDING_PAYMENT → CONFIRMED → SHIPPED → DELIVERED`. A `PENDING_PAYMENT` order may be cancelled by releasing its stock reservation. Only an administrator may cancel a paid `CONFIRMED` order before shipment; the system must request an idempotent Stripe refund and may mark the order `CANCELLED` and restore stock only after verified refund confirmation. `SHIPPED` and `DELIVERED` orders cannot be cancelled, and returns are out of scope. | US-008 |
| FR-010 | API failures must use RFC 7807 Problem Details and must include actionable field violations where relevant. | US-009 |
| FR-011 | The frontend must provide public, authenticated, and administrator routes matching each supported workflow and must present authorization and validation failures safely. | US-001, US-003, US-004, US-006, US-007, US-008, US-009 |
| FR-012 | The backend API must publish usable OpenAPI documentation for implemented business endpoints. | US-010 |
| FR-013 | The deployed frontend must communicate successfully with the deployed backend across the critical Browse to Cart to Checkout to Orders journey. | US-011 |
| FR-014 | After a verified payment advances an order to `CONFIRMED`, the system must create an asynchronous, deduplicated confirmation-notification request. Notification processing must not block or roll back the order transition. The first slice requires a test adapter; external email delivery is deferred. | US-005A, US-010A |
| FR-015 | The system must detect warehouse inventory at or below its configured safety threshold through a repeatable scan every 30 minutes. The same application use case must support an authorized manual invocation outside production. The system must create one alert when stock transitions to at-or-below the threshold, suppress repeated scans while stock remains low, reset after stock recovers above the threshold, and permit a new alert on a later downward crossing. | US-008A, US-010A |

---

## Non-Functional Requirements

| ID | Requirement | Category |
|----|-------------|----------|
| NFR-001 | Authentication, authorization, ownership, and administrator boundaries must be enforced by the backend; the frontend must not be treated as a security boundary. | Security |
| NFR-002 | Passwords must be stored using an approved adaptive password hash, secrets must remain outside committed client or server configuration, and errors and telemetry must not expose sensitive data. | Security |
| NFR-002A | Refresh-token rotation must detect replay, revoke the affected token family, and preserve CSRF protection for cookie-authenticated refresh and logout operations. | Security |
| NFR-003 | Checkout, warehouse allocation, inventory reservations, monetary totals, uniqueness, and order history must remain consistent under validation failures, retries, concurrent requests, and reservation expiry. Available stock at each warehouse must never become negative or be oversold. | Data integrity |
| NFR-004 | Every schema change must be reproducible through versioned migrations, and production schema creation must not depend on automatic ORM mutation. | Database lifecycle |
| NFR-005 | Domain and application behavior must remain independent of web, persistence, and framework details, with dependencies directed inward and architecture boundaries covered by automated checks. | Maintainability |
| NFR-006 | Object-oriented design and design patterns must solve named domain or integration problems; speculative abstractions and pattern-count optimization are prohibited. | Design quality |
| NFR-006A | Payment use cases and domain rules must depend on a provider-neutral contract so that Stripe can be replaced without changing core business behavior. | Maintainability |
| NFR-006B | Payment-provider callbacks must be authenticated, replay-safe, idempotent, and processed without trusting browser-reported payment state. | Security |
| NFR-006C | Database transactions must not remain open across Stripe network calls. Each local state transition must be atomic, while cross-system consistency must use idempotency, explicit compensation, and reconciliation for interrupted or out-of-order payment flows. | Transaction management |
| NFR-007 | Each delivery task must add automated tests tracing to its acceptance criteria; backend, frontend, integration, security, and critical browser-flow suites must pass before completion. Backend and frontend must each maintain at least 80% line and 70% branch coverage. Domain rules, authorization, checkout, inventory reservation, payment, refund, and idempotency modules must each maintain at least 90% branch coverage. Critical backend domain modules must run mutation testing, with the enforceable mutation-score threshold set from the first recorded baseline. | Quality |
| NFR-008 | Health and application metrics must be available for operations without publicly exposing sensitive actuator data. | Observability |
| NFR-008A | Business telemetry must distinguish successful reservations, out-of-stock results, concurrency or retry conflicts, notification outcomes, and scheduled-job duration without using customer, order, product, warehouse, email, or other unbounded identifiers as metric tags. | Observability |
| NFR-009 | Docker Compose must provide a reproducible full-stack environment usable locally and in CI. | Portability |
| NFR-010 | GitHub Actions must build and test the system and support controlled deployment of the backend to Railway and the frontend to Vercel. | Delivery |
| NFR-011 | Deployment must include a documented and verified rollback procedure. | Reliability |
| NFR-012 | The frontend and backend must remain independently buildable and testable through CI-friendly commands. | Developer experience |
| NFR-013 | Against a reference dataset of 10,000 products, 100 categories, and 100,000 historical orders under 50 concurrent users, catalog and product-detail APIs must achieve p95 latency of at most 300 ms, non-payment write APIs at most 500 ms, and checkout-session creation at most 1 second excluding Stripe network time. Server error rate must remain below 1%, with no overselling or duplicate orders. | Performance |
| NFR-014 | Against one warehouse with 50 available units, 100 concurrent unique one-unit reservation attempts with no expiry during the run must produce exactly 50 successful reservations and 50 typed out-of-stock results; availability must settle at 0 and must never become negative. Infrastructure timeouts and unrelated failures invalidate the evidence run rather than counting as out-of-stock results. | Concurrency evidence |

---

## Success Metrics / KPIs

| Metric | Baseline | Target | How measured |
|--------|----------|--------|--------------|
| Approved requirement completion | Initial scaffolds only | 100% of approved requirements and acceptance criteria demonstrated | Requirement traceability and task Evidence tables |
| Automated quality gate | Scaffold tests only | All required backend, frontend, integration, security, and browser-flow suites pass at agreed thresholds | GitHub Actions results and test reports |
| End-to-end delivery | Not deployed | Frontend and backend deploy successfully and complete the critical customer journey | Vercel and Railway deployment evidence plus smoke tests |
| Operational readiness | Not configured | Health, metrics, and rollback checks pass without sensitive-data exposure | Deployment verification and rollback exercise |

---

## Out of Scope

The following are explicitly excluded unless approved through a later scope change:

- Promotions, discount rules, and coupons
- Product reviews and ratings
- Wishlists
- Multi-vendor marketplace capabilities
- Native mobile applications
- Full warehouse management: bins, aisles, receiving, picking, packing, procurement, replenishment, inter-warehouse transfers, shipment routing, and multi-warehouse order splitting
- Customer warehouse or pickup-location selection
- A production traffic-simulation endpoint or arbitrary remote workload controls
- External email delivery and daily HTML sales reports in the first warehouse/concurrency slice
- Features not required by the approved customer, administrator, reviewer, or deployment journeys

---

## Open Questions / Assumptions

No unresolved product assumptions remain. The warehouse-allocation ranking was approved by the user. The low-stock suppression rule follows the Supervisor's recommended standard after the user delegated the choice due to limited domain experience.
