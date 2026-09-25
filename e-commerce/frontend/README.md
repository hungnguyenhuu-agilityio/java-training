# E-Commerce Frontend

Angular client for the customer storefront and administrator workflows described in the [project requirements](../README.md).

The approved visual and interaction contract is [`UI_SPEC.md`](../UI_SPEC.md). Frontend work must use its Angular Material foundation, shared shell, route wireframes, state behavior, responsiveness, and accessibility rules rather than inventing task-local designs.

The frontend is independently buildable and uses Angular standalone components. It is expected to communicate with the Spring Boot REST API over HTTP/JSON and send a JWT bearer token for protected operations.

## Requirement Mapping

| Requirement area | Frontend responsibility | Planned user experience |
| --- | --- | --- |
| Product catalog | Display paginated products and support sorting, text search, and category filtering | Catalog and product-detail pages available without authentication |
| Authentication | Provide registration and login forms, retain the authenticated session, attach the JWT to protected requests, and handle token expiry | Guests can create an account or sign in; expired sessions return users to login |
| Authorization | Protect authenticated routes and restrict administrative screens to `ADMIN` users | `CUSTOMER` and `ADMIN` users see only actions allowed for their role |
| Shopping cart | Add, update, remove, and clear items while displaying quantities, prices, and totals | Authenticated customers can prepare an order and resolve validation or stock conflicts |
| Checkout | Collect and validate checkout details, submit the cart once, and show the resulting order | Customers can complete the Cart → Checkout → Order flow without duplicate submissions |
| Orders | List a customer's orders and display order details and status history | Customers can review only their own order history |
| Catalog administration | Provide category and product create/edit/delete forms | Administrators can maintain the catalog, with destructive actions confirmed |
| Order administration | List orders and expose valid status-management actions | Administrators can review orders and advance their status |
| API errors | Interpret RFC 7807 Problem Details, including field violations | Users receive useful messages for validation, authentication, authorization, not-found, and conflict responses |
| Quality | Unit-test components, services, guards, interceptors, and critical user flows | Frontend tests run through a CI-friendly npm command |

Custom payment forms (payment uses Stripe-hosted Checkout), promotions, reviews, wishlists, and other features not named in the project requirements are outside the frontend scope.

## Planned Routes

| Route | Access | Purpose |
| --- | --- | --- |
| `/products` | Public | Browse, search, sort, and filter the catalog |
| `/products/:id` | Public | View product details |
| `/register` | Guest | Create a customer account |
| `/login` | Guest | Authenticate and begin a session |
| `/cart` | `CUSTOMER` | Manage the active cart |
| `/checkout` | `CUSTOMER` | Validate checkout details and place an order |
| `/checkout/result` | `CUSTOMER` | Show backend-verified payment/order progress after the Stripe return |
| `/orders` | `CUSTOMER` | View the current user's order history |
| `/orders/:id` | `CUSTOMER` | View an accessible order |
| `/admin/products` | `ADMIN` | Manage products |
| `/admin/categories` | `ADMIN` | Manage categories |
| `/admin/warehouses` | `ADMIN` | Manage warehouses |
| `/admin/inventory` | `ADMIN` | Maintain warehouse-scoped inventory and thresholds |
| `/admin/orders` | `ADMIN` | Review orders and manage their status |

The route names are the intended frontend contract and may be refined when the corresponding feature is implemented. Authorization must also be enforced by the backend; route guards and hidden controls are usability measures, not security boundaries.

## Frontend Architecture

Keep application code organized by responsibility as the implementation grows:

```text
src/app/
├── core/       # API client, authentication state, guards, interceptors
├── shared/     # Reusable presentational components, directives, and pipes
├── features/
│   ├── auth/
│   ├── catalog/
│   ├── cart/
│   ├── checkout/
│   ├── orders/
│   └── admin/
├── app.config.ts
└── app.routes.ts
```

Prefer lazy-loaded feature routes, typed API models, reactive forms, and centralized HTTP/authentication behavior. Do not place secrets in Angular configuration: browser-delivered values are public by design.

## Current Status

The Angular 22.1 scaffold is initialized with routing and Vitest support. Feature routes, API integration, authentication, Angular Material, and application screens are not implemented yet; the tables above and `UI_SPEC.md` define the delivery target rather than the current feature set.

## Prerequisites

- A Node.js version supported by Angular 22
- npm 11 (the repository currently declares `npm@11.19.0`)
- The backend running locally for integration work

## Install and Run

From the `frontend` directory:

```bash
npm ci
npm start
```

Open `http://localhost:4200/`. The development server reloads when source files change.

To develop the frontend against the containerized local dependencies, start MySQL and the backend
from the repository root, then run Angular in this directory:

```bash
docker compose up --build --detach --wait mysql backend
cd frontend
npm ci
npm start
```

This serves Angular at `http://localhost:4200`, the backend at `http://localhost:18080`, and MySQL on
host port `13306`. It can coexist with the Compose Nginx frontend at `http://localhost:14200` because
the host ports differ, but running both frontend servers is usually unnecessary.

`ng serve` does not currently proxy `/api` requests. The Docker Nginx and Vercel configurations do;
the Angular development proxy should be added with the first frontend-to-backend API integration.

## Docker and Nginx

The frontend Docker image compiles Angular and serves the resulting static files with Nginx. This is
the production-like path used by Docker Compose and CI smoke tests: it checks the compiled output,
SPA route fallback, `/api` forwarding, container networking, and the `/health` endpoint. Production
uses Vercel to serve the frontend, so the Nginx container is for the portable Compose environment.

## Build

```bash
npm run build
```

Production artifacts are written to `dist/`.

## Test

```bash
npm test
```

The current project uses Vitest through the Angular test builder. Required coverage should include form validation, API services, JWT attachment, route guards, role-aware navigation, Problem Details mapping, cart totals, checkout submission protection, and the critical Browse → Cart → Checkout → Orders journey.

An end-to-end test runner is not configured in the scaffold. Add one before claiming browser-level completion of the critical flows.

## Definition of Done

The frontend portion is complete when:

- Every requirement area in the mapping table has an implemented, accessible UI flow.
- Public, authenticated, and administrator routes behave correctly for each role.
- API validation and RFC 7807 errors are presented without leaking sensitive data.
- The critical customer journey works against the Spring Boot backend.
- Unit and browser-level tests cover the critical business and authorization flows.
- `npm test` and `npm run build` pass from a clean install.
