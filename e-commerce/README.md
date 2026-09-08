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

> The application source code and runtime configuration will be added during Sprint 1. The commands below describe the intended developer workflow after initialization.

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

### Run the Backend Locally

```bash
cd backend
./mvnw spring-boot:run
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

Once implemented, local API documentation and operational endpoints should be available through SpringDoc and Actuator. Exact URLs may vary with the final application configuration.

| Purpose | Conventional endpoint |
| --- | --- |
| OpenAPI specification | `/v3/api-docs` |
| Swagger UI | `/swagger-ui.html` |
| Application health | `/actuator/health` |
| Application metrics | `/actuator/metrics` |

Actuator endpoints must be exposed deliberately, and sensitive operational data must not be publicly accessible.

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

