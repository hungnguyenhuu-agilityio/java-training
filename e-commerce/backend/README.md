# E-Commerce Backend

Spring Boot REST API for the e-commerce practice application. The backend will
provide the product catalog, authentication and authorization, shopping cart,
checkout, order history, and operational endpoints used by the frontend.

> **Current status:** this directory contains the initial Spring Boot scaffold.
> Domain features, API endpoints, database configuration, and Liquibase
> changelogs have not been implemented yet.

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

## Prerequisites

- JDK 21
- A running MySQL instance for database-backed development

You do not need to install Maven; the repository includes Maven Wrapper scripts.

## Project structure

```text
backend/
├── .mvn/wrapper/                  # Maven Wrapper configuration
├── src/main/java/com/example/ecommerce/
│   └── ECommerceApplication.java # Application entry point
├── src/main/resources/
│   └── application.properties    # Spring configuration
├── src/test/java/com/example/ecommerce/
│   └── ECommerceApplicationTests.java
├── mvnw                           # Maven Wrapper for Linux and macOS
├── mvnw.cmd                       # Maven Wrapper for Windows
└── pom.xml                        # Dependencies and build configuration
```

## Configuration

Only the application name is configured in the initial scaffold. Before the
application can connect to MySQL, add the datasource settings and the first
Liquibase changelog. Prefer environment variables for credentials.

Common Spring environment variables are:

| Variable | Example | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/ecommerce` | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | `ecommerce` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `change-me` | Database password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | Validate entities against the migrated schema |
| `SPRING_PROFILES_ACTIVE` | `local` | Select a Spring profile once profiles are added |

Do not commit real passwords or tokens. Local values can be exported in the
shell or supplied through an ignored local configuration file.

Tests use an in-memory H2 database and do not require a local MySQL instance.
Liquibase is disabled in the test profile until the first changelog is added.

## Run locally

From the `backend` directory:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The default HTTP port is `8080`. Startup currently requires completing the
database and Liquibase configuration described above.

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

API paths and payloads should be documented after their controllers are
implemented. OpenAPI/Swagger UI is not available in the current scaffold.

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
