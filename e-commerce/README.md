# E-Commerce Java Practice

A hands-on full-stack practice project for building a secure, observable e-commerce application with Angular and Spring Boot.

The project is planned as an eight-day, six-sprint exercise for one developer. It covers the complete customer journey—from account registration and product discovery to checkout and order history—while applying modern Java and Spring development practices.

## Project Goals

- Build a RESTful backend with Spring Boot and Java 21.
- Build a standalone-component frontend with Angular and TypeScript.
- Implement authentication and role-based authorization with JWT.
- Support product catalog, shopping cart, checkout, and order management workflows.
- Manage the database schema through Liquibase migrations.
- Provide consistent RFC 7807 API error responses.
- Add automated tests for critical business and security flows.
- Package and run the complete application with containers.
- Expose health and application metrics for production observability.

## Technology Stack

### Backend

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Security | Spring Security 6.5, JWT, Lambda DSL |
| Persistence | Spring Data JPA |
| Validation | Spring Validation |
| API documentation | SpringDoc OpenAPI |
| Database migrations | Liquibase |
| Observability | Spring Boot Actuator, Micrometer |
| Build tool | Maven |

### Frontend

| Area | Technology |
| --- | --- |
| Framework | Angular, latest stable version |
| Language | TypeScript |
| Architecture | Standalone components |

### Infrastructure and Tooling

- MySQL 8.4.11
- Docker and Docker Compose
- Git
- Postman

## Core Features

### Product Catalog

- Category and product management
- Product listing with pagination, sorting, and filtering
- Category navigation
- Product details
- Basic product administration

### Authentication and Authorization

- Customer registration and login
- JWT-based authentication
- Password hashing
- `CUSTOMER` and `ADMIN` roles
- Protected API endpoints and Angular routes
- Role-aware user interface

### Shopping Cart and Orders

- Shopping cart management
- Checkout and order placement
- Transactional inventory updates
- Idempotent order operations
- Order status history
- Customer order history
- Basic administrator order view

### Quality and Operations

- RFC 7807 Problem Details responses
- Unit, slice, integration, and frontend tests
- OpenAPI documentation
- Health and metrics endpoints
- Virtual threads and graceful shutdown
- Ahead-of-time compilation readiness
- Container images built with Cloud Native Buildpacks

## Planned Architecture

```text
+--------------------+       HTTP / JSON       +-------------------------+
| Angular Frontend   | <---------------------> | Spring Boot REST API    |
|                    |       JWT bearer         |                         |
| Catalog and Auth   |                          | Security and Services   |
| Cart and Orders    |                          | JPA and Validation      |
+--------------------+                          +------------+------------+
                                                               |
                                                               | JDBC
                                                               v
                                                  +-------------------------+
                                                  | MySQL 8.4.11            |
                                                  | Liquibase-managed schema|
                                                  +-------------------------+
```

## Delivery Plan

| Sprint | Duration | Scope | Definition of Done |
| --- | ---: | --- | --- |
| 1. Setup and foundations | 1 day | Spring Boot and Angular setup, profiles, routing, HTTP client, MySQL, Liquibase, and exception-handling skeleton | Both applications run and communicate successfully. |
| 2. Product catalog | 2 days | Product and category domain models, CRUD APIs, persistence, migrations, catalog pages, and admin product form | Users can browse the catalog end to end. |
| 3. Security and authentication | 1 day | Registration, login, JWT, roles, endpoint protection, route guards, and role-aware UI | Authentication and authorization work across the frontend and backend. |
| 4. Cart and orders | 2 days | Cart APIs, checkout, transactional stock handling, order history, status tracking, and related pages | The Browse → Cart → Checkout → Orders journey works. |
| 5. Testing | 1 day | Backend unit, slice, security integration, and frontend unit tests | Critical flows are covered and all tests run with a single CI-friendly command. |
| 6. Production readiness | 1 day | Buildpacks, Actuator, Micrometer, graceful shutdown, virtual threads, and Docker Compose | The complete application runs with Docker Compose and is ready to demonstrate. |

## Expected Repository Structure

The implementation should keep the frontend and backend independently buildable while supporting a single containerized development workflow.

```text
e-commerce/
├── backend/              # Spring Boot application
├── frontend/             # Angular application
├── docker-compose.yml    # Local full-stack environment
├── requirement.txt       # Original project requirements
└── README.md
```

## Getting Started

T001 provides the first runnable local foundation. Business journeys are added by later tasks.

### Prerequisites

- JDK 21
- Maven 3.9 or Maven Wrapper
- Node.js supported by the selected Angular version
- npm
- Docker with Docker Compose

### Run with Docker Compose

From the `e-commerce` directory:

```bash
docker compose up --build
```

Compose waits for MySQL and backend health before starting dependants. The frontend is available at
`http://localhost:14200`, backend health at `http://localhost:18080/actuator/health`, and local-only
Swagger UI at `http://localhost:18080/swagger-ui.html`; MySQL is published on host port `13306`.
Override the host ports with `FRONTEND_HOST_PORT`, `BACKEND_HOST_PORT` and `MYSQL_HOST_PORT` (the
same variables are read by `scripts/smoke-stack.sh`). The Compose credentials are development-only
defaults and must never be reused outside a local disposable environment.

### Choose a Frontend Development Mode

| Mode | Frontend URL | Intended use |
| --- | --- | --- |
| Angular development server (`npm start`) | `http://localhost:4200` | Daily frontend work with fast rebuilds and browser reloads |
| Nginx in Docker Compose | `http://localhost:14200` | Production-like local runs and CI smoke testing of the compiled Angular output |

The Compose frontend image builds Angular and then uses Nginx to serve the generated static files.
This verifies that the production build, SPA route fallback, `/api` forwarding, health check, and
container networking work together. Production uses Vercel rather than this Nginx container, so
Compose is a close local approximation, not an exact copy of the production platform.

For daily frontend development, start only the dependencies in Compose and run Angular separately:

```bash
docker compose up --build --detach --wait mysql backend
cd frontend
npm ci
npm start
```

The Angular development server and Compose frontend can run at the same time because they use
different host ports (`4200` and `14200`), although normally only one frontend server is needed.
The Nginx and Vercel configurations forward `/api` requests, but `ng serve` does not currently have
a development `/api` proxy. Add that proxy with the first frontend-to-backend API integration rather
than assuming `/api` on port `4200` reaches the backend.

### Run the Backend Locally

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Run the Frontend Locally

```bash
cd frontend
npm install
npm start
```

### Run Tests

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm test
```

## API and Operational Endpoints

Local API documentation is generated from implemented controllers through Springdoc. API docs and
Swagger UI are disabled by default and enabled only by the `local` profile; tests enable JSON docs
without enabling the interactive UI.

| Purpose | Conventional endpoint |
| --- | --- |
| OpenAPI specification | `/v3/api-docs` |
| Swagger UI | `/swagger-ui.html` |
| Application health | `/actuator/health` |
| Application metrics | Not exposed by the T001 baseline |

Actuator endpoints must be exposed deliberately, and sensitive operational data must not be publicly accessible.

## Production release and rollback

The git repository root is `java-training/`, so the workflow lives at
`java-training/.github/workflows/ci-release.yml` and triggers only for changes under `e-commerce/**`
or to the workflow itself. The only production release path is its `deploy-production` job after a
successful merge to protected `main`. Pull requests and other branches run CI only. Railway and
Vercel credentials are protected `production` environment secrets and must never be committed or
printed. `scripts/test-ci-policy.sh` enforces these rules and runs in CI as the `ci-policy` job.

Before the first release, replace the placeholder `https://railway-backend-origin.invalid` in
`frontend/vercel.json` with the public Railway backend origin (not a secret). Release and rollback
jobs refuse to deploy while the placeholder remains. The Railway service must provide
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`; only the
`local` profile has datasource defaults. `FRONTEND_HEALTH_URL` is the frontend origin: smoke checks
also call `<origin>/api/catalog/categories` through the Vercel `/api` proxy.

### Rollback runbook

1. Stop merging to `main`.
2. Identify the last known-good commit: the full SHA of the most recent successful `deploy-production`
   run on `main`.
3. In GitHub Actions, run **CI and production release** via **Run workflow** on branch `main` with
   `rollback_sha` set to that SHA. The `rollback-production` job verifies the SHA is on `main`, uses the
   same protected `production` environment and `production-release` concurrency group, redeploys that
   commit to Railway and Vercel, and runs the same `scripts/smoke-production.sh` checks.
4. The rollback is complete only when that job is green; a red job means production is still unhealthy.

Never run Liquibase rollback in production during an application rollback: new changesets must stay
backward-compatible with the previous release. The baseline migration's `up -> rollback -> up`
lifecycle is exercised by the dedicated MySQL CI migration job.

## Roles

| Role | Intended capabilities |
| --- | --- |
| `CUSTOMER` | Browse products, manage a cart, place orders, and view personal order history |
| `ADMIN` | Manage products and categories and review orders |

## Completion Criteria

The project is complete when:

- The Angular frontend and Spring Boot backend work together end to end.
- Authentication, authorization, and protected resources behave correctly.
- Customers can browse products, manage their carts, check out, and review orders.
- Inventory updates during checkout are transactional.
- Database changes are reproducible through Liquibase.
- Critical workflows and security boundaries are covered by automated tests.
- Tests can be executed with a single CI-friendly command per application.
- Docker Compose starts the full application and MySQL database.
- Health and metrics are available without exposing sensitive information.
- API documentation and demonstration materials are ready.

## Source Repositories

- [GitLab](https://gitlab.asoft-python.com/hung.nguyenhuu/java-training)
- [GitHub](https://github.com/hungnguyenhuu-agilityio/java-training)

## Author

Hung Nguyen Huu
