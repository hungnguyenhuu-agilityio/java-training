# E-Commerce Backend

Spring Boot REST API for the e-commerce practice application. The backend will
provide the product catalog, authentication and authorization, shopping cart,
checkout, order history, and operational endpoints used by the frontend.

> **Current status:** T001 provides the runtime, Liquibase baseline, module boundaries, health, and
> generated OpenAPI foundation. Domain features and business API endpoints remain planned.

## Technology stack

- Java 21
- Spring Boot 4.1.1
- Spring MVC
- Spring Data JPA
- Spring Security and OAuth 2.0 Resource Server
- Jakarta Bean Validation
- Liquibase
- MySQL
- Spring Boot Actuator
- Maven Wrapper
- Lombok
- Spring Modulith and ArchUnit
- Springdoc OpenAPI 3.1.1

## Prerequisites

- JDK 21
- A running MySQL instance for database-backed development

You do not need to install Maven; the repository includes Maven Wrapper scripts.

## Project structure

```text
backend/
├── .mvn/wrapper/                  # Maven Wrapper configuration
├── src/main/java/com/training/ecommerce/
│   └── ECommerceApplication.java # Application entry point
├── src/main/resources/
│   └── application.properties    # Spring configuration
├── src/test/java/com/training/ecommerce/
│   └── ECommerceApplicationTests.java
├── mvnw                           # Maven Wrapper for Linux and macOS
├── mvnw.cmd                       # Maven Wrapper for Windows
└── pom.xml                        # Dependencies and build configuration
```

## Configuration

Runtime datasource settings use environment variables, Liquibase owns schema creation, and
Hibernate validates rather than mutates the schema.

Common Spring environment variables are:

| Variable | Example | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:13306/ecommerce` | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | `ecommerce` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `change-me` | Database password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | Validate entities against the migrated schema |
| `SPRING_PROFILES_ACTIVE` | `local` | Select a Spring profile once profiles are added |
| `PORT` | `8080` | HTTP port; injected by Railway, defaults to `8080` |

The three `SPRING_DATASOURCE_*` variables are required outside the `local` profile; only
`application-local.properties` supplies host-mode defaults. Those defaults target the Compose MySQL
published on host port `13306` (or `MYSQL_HOST_PORT`) with the development-only password
`ecommerce-local`, so a host-mode run never reaches another MySQL on port `3306`. Start just the
database with `docker compose up --detach --wait mysql` from the repository root.

Do not commit real passwords or tokens. Local values can be exported in the
shell or supplied through an ignored local configuration file.

Tests use an in-memory H2 database and do not require a local MySQL instance.
Tests enable Liquibase against an isolated H2 database in MySQL compatibility mode.

## Run locally

From the `backend` directory:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The default HTTP port is `8080`. Start MySQL first or use `docker compose up --build` from the
repository root; Compose publishes the backend at `http://localhost:18080` and MySQL on host port
`13306` (override with `BACKEND_HOST_PORT` / `MYSQL_HOST_PORT`).

## Build and test

Run the test suite:

```bash
./mvnw test
```

Create the executable JAR:

```bash
./mvnw clean package
```

Run the packaged application:

```bash
java -jar target/ecommerce-0.0.1-SNAPSHOT.jar
```

Create an OCI container image with Spring Boot Buildpacks:

```bash
./mvnw spring-boot:build-image
```

## Planned API areas

The backend is intended to expose REST endpoints for:

- registration, login, and JWT-based authorization;
- categories and products;
- customer shopping carts;
- checkout and transactional inventory updates;
- customer order history and administrator order management; and
- health and metrics through Actuator.

OpenAPI paths and payloads are generated as controllers are implemented. JSON API docs and Swagger
UI are available only in the `local` profile; production defaults disable both.

## Development guidelines

- Keep controllers focused on HTTP concerns and place business rules in
  services.
- Use DTOs at the API boundary instead of exposing persistence entities.
- Manage every schema change with Liquibase; do not rely on Hibernate to modify
  production schemas.
- Return consistent RFC 7807 Problem Details for API errors.
- Enforce authorization in the backend even when the frontend hides protected
  actions.
- Add tests for business rules, persistence, validation, and security boundaries.

## Related documentation

See the [repository README](../README.md) for the full-stack goals, architecture,
delivery plan, and frontend workflow.
