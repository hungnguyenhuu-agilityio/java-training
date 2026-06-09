# TaskFlow — Backend

Raw Java 21 REST API with no web framework. Built on the JDK's built-in `HttpServer`, HikariCP for connection pooling, and raw JDBC `PreparedStatement` for all database access.

---

## Module structure

```
taskflow/                       ← Maven parent (pom.xml manages versions)
├── taskflow-domain/            ← POJOs + enums, zero external dependencies
├── taskflow-persistence/       ← DbConnection (HikariCP) + all *Dao classes
├── taskflow-service/           ← Business logic, Java Streams, TokenStore
├── taskflow-http/              ← Handlers, Server, AuthFilter, CorsFilter
└── taskflow-util/              ← CsvExporter, FileUploadUtil, JsonUtil
```

Dependency direction is strictly one-way: `http → service → persistence → domain`. `util` is used by `http` and `service`. No circular dependencies.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.8+ |
| MySQL | 8 (Docker recommended, port 3307) |

---

## Database setup

```bash
# Start MySQL container
docker run --name taskflow-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=taskflow_db \
  -p 3307:3306 -d mysql:8

# Apply the schema (idempotent — safe to re-run)
mysql -h 127.0.0.1 -P 3307 -u root -proot taskflow_db \
  < ../db/migrations/V1__init.sql
```

The schema creates five tables: `users`, `projects`, `tasks`, `comments`, `attachments`.

---

## Running the server

```bash
cd taskflow

# Build all modules (skip tests for speed)
mvn clean package -q -DskipTests

# Start the server
mvn exec:java -pl taskflow-http \
  -Dexec.mainClass="com.taskflow.http.Server"
```

Server starts on **`http://localhost:8080`** and logs to stdout.

To run tests:
```bash
mvn test
```

---

## Configuration

All configuration is in `taskflow-persistence/src/main/resources/db.properties` (or equivalent — check `DbConnection.java` for the property names):

| Property | Default | Description |
|----------|---------|-------------|
| `db.url` | `jdbc:mysql://localhost:3307/taskflow_db` | JDBC URL |
| `db.username` | `root` | DB user |
| `db.password` | `root` | DB password |
| `db.pool.size` | `10` | HikariCP max pool size |

---

## API endpoints

All routes require `Authorization: Bearer <token>` except `/auth/register` and `/auth/login`.

### Auth
| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/auth/register` | `{ email, username, password }` | `201 { id, email, username }` |
| POST | `/auth/login` | `{ email, password }` | `200 { token }` |

### Tasks
| Method | Path | Notes |
|--------|------|-------|
| GET | `/tasks` | Optional `?status=TODO&priority=HIGH&projectId=1` |
| POST | `/tasks` | Body: `{ title, description?, status, priority, dueDate?, projectId? }` |
| PUT | `/tasks/{id}` | Full replacement body |
| DELETE | `/tasks/{id}` | `204` on success |
| GET | `/tasks/export/csv` | Streams RFC 4180 CSV |

### Projects
| Method | Path | Notes |
|--------|------|-------|
| GET | `/projects` | All projects for authenticated user |
| POST | `/projects` | `{ name, description? }` |
| PUT | `/projects/{id}` | `{ name, description? }` |
| DELETE | `/projects/{id}` | `409` if project has tasks |

### Comments
| Method | Path | Notes |
|--------|------|-------|
| GET | `/tasks/{id}/comments` | All comments on a task |
| POST | `/tasks/{id}/comments` | `{ body }` |
| DELETE | `/tasks/{taskId}/comments/{id}` | `204` on success |

### Attachments
| Method | Path | Notes |
|--------|------|-------|
| GET | `/tasks/{id}/attachments` | List metadata |
| POST | `/tasks/{id}/attachments` | `multipart/form-data`, field name `file`, max 10 MB |

### Dashboard
| Method | Path | Response |
|--------|------|----------|
| GET | `/dashboard/stats` | `{ byStatus: { TODO: n, ... }, byPriority: { LOW: n, ... } }` |

---

## Module responsibilities

### `taskflow-domain`
Pure POJOs and enums: `User`, `Task`, `Project`, `Comment`, `Attachment`, `TaskStatus`, `TaskPriority`. No dependencies. Shared by all other modules.

### `taskflow-persistence`
- `DbConnection` — HikariCP pool initialised from properties file. Throws on startup if MySQL is unreachable.
- `*Dao` classes — all SQL via `PreparedStatement`. No string concatenation. Each DAO owns one table.

### `taskflow-service`
- `TaskService`, `ProjectService`, `UserService`, `CommentService` — business logic, validation, Java Streams operations.
- `TokenStore` — `ConcurrentHashMap<String, TokenEntry>` with 24-hour TTL. Cleanup runs hourly via `ScheduledExecutorService`.
- `NotificationScheduler` — background thread, logs tasks due within 24h every hour.
- `StatsService` — aggregates task counts by status and priority.

### `taskflow-http`
- `Server` — entry point. Registers all contexts, sets thread pool, creates `uploads/` dir, installs JVM shutdown hook.
- `AuthFilter` — validates `Authorization: Bearer <token>` on every protected context.
- `CorsFilter` — adds `Access-Control-Allow-Origin: http://localhost:4200` to every response.
- `*Handler` classes — one class per entity group; parse request body, delegate to service, write JSON response.

### `taskflow-util`
- `CsvExporter` — writes tasks to a `Writer` as RFC 4180 CSV.
- `FileUploadUtil` — parses `multipart/form-data`, sanitises filenames (UUID prefix), writes to `uploads/`.
- `JsonUtil` — shared `ObjectMapper` singleton with `JavaTimeModule`.

---

## Key constraints

- **No Spring, no Hibernate, no Lombok** — raw Java 21 only
- **All SQL uses `PreparedStatement`** — no string-concatenated queries
- **Passwords hashed with BCrypt** — never stored in plain text
- **Token check-and-remove uses `computeIfPresent`** — avoids TOCTOU race on the `ConcurrentHashMap`
- **MySQL must be running before the app starts** — HikariCP fails fast on init if the DB is unreachable
- **`uploads/` created on startup** — `Server.java` calls `Files.createDirectories(Path.of("uploads"))` before any handler is registered
