# TaskFlow

A full-stack task management web app built as a Java learning project. The backend is raw Java 21 with no frameworks — no Spring, no Hibernate — to maximise understanding of the core language and platform. The frontend is Angular 18.

---

## What it does

- Register and log in with email/password (BCrypt + UUID session tokens)
- Create and manage **Tasks** with status, priority, and due dates
- Group tasks into **Projects**
- Leave **Comments** on tasks
- Attach files to tasks (up to 10 MB each)
- View tasks in a **Kanban board** and toggle status with one click
- **Filter tasks** by status, priority, and project; search by title
- Export tasks as **CSV**
- **Dashboard** with task counts by status and priority (CSS bar charts)
- Background thread notifies (logs) tasks due within the next 24 hours

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Backend language | Java 21 |
| HTTP server | `com.sun.net.httpserver.HttpServer` (built-in JDK) |
| Connection pool | HikariCP 5.1 |
| JSON | Jackson 2.17 |
| Password hashing | BCrypt (`org.mindrot:jbcrypt`) |
| Database | MySQL 8 (Docker, port 3307) |
| Frontend | Angular 18 (standalone components) |
| Build | Maven 3 (multi-module) |

No Spring. No Hibernate. No Lombok.

---

## Project layout

```
task-flow-practice/
├── taskflow/               ← Maven multi-module backend
│   ├── taskflow-domain/    ← POJOs and enums (zero dependencies)
│   ├── taskflow-persistence/ ← HikariCP + all *Dao classes (raw JDBC)
│   ├── taskflow-service/   ← Business logic and Java Streams
│   ├── taskflow-http/      ← HTTP handlers, Server bootstrap, AuthFilter
│   └── taskflow-util/      ← CsvExporter, FileUploadUtil, JsonUtil
├── frontend/               ← Angular 18 workspace
├── db/
│   └── migrations/
│       └── V1__init.sql    ← Full schema (idempotent)
├── uploads/                ← File attachments (created on first run)
├── README.md               ← This file
└── RUNBOOK.md              ← Deployment and ops runbook
```

---

## Quick start

### 1. Start MySQL

```bash
docker run --name taskflow-mysql -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=taskflow_db -p 3307:3306 -d mysql:8
```

### 2. Apply the schema

```bash
mysql -h 127.0.0.1 -P 3307 -u root -proot taskflow_db \
  < db/migrations/V1__init.sql
```

### 3. Run the backend

```bash
cd taskflow
mvn clean package -q -DskipTests
mvn exec:java -pl taskflow-http -Dexec.mainClass="com.taskflow.http.Server"
# Server starts on http://localhost:8080
```

### 4. Run the frontend

```bash
cd frontend
npm install
npx ng serve
# App available at http://localhost:4200
```

Open `http://localhost:4200` — you will be redirected to `/login`. Register an account and explore.

---

## API overview

All routes are prefixed with `/api` by the Angular proxy. The backend receives them without the prefix.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Log in, returns `{ token }` |
| GET | `/tasks` | List tasks (optional `?status=&priority=&projectId=`) |
| POST | `/tasks` | Create task |
| PUT | `/tasks/{id}` | Update task |
| DELETE | `/tasks/{id}` | Delete task |
| GET | `/tasks/export/csv` | Download tasks as CSV |
| GET | `/tasks/{id}/comments` | List comments on a task |
| POST | `/tasks/{id}/comments` | Add a comment |
| DELETE | `/tasks/{taskId}/comments/{id}` | Delete a comment |
| GET | `/tasks/{id}/attachments` | List attachments |
| POST | `/tasks/{id}/attachments` | Upload a file (multipart/form-data) |
| GET | `/projects` | List projects |
| POST | `/projects` | Create project |
| PUT | `/projects/{id}` | Update project |
| DELETE | `/projects/{id}` | Delete project (409 if tasks exist) |
| GET | `/dashboard/stats` | Task counts by status and priority |

All endpoints except `/auth/register` and `/auth/login` require `Authorization: Bearer <token>`.

---

## Learning goals

This project was built to practise:

- Multi-module Maven project structure
- Raw JDBC with `PreparedStatement` (no ORM)
- Java Streams for filtering, grouping, and mapping
- Java I/O: multipart file upload, CSV streaming
- `ScheduledExecutorService` for background work
- `ConcurrentHashMap`-backed session store with TTL
- Angular 18 standalone components, reactive forms, HTTP interceptors, auth guards
- Full vertical-slice feature delivery (domain → DAO → service → handler → UI)

See `RUNBOOK.md` for the full deployment and troubleshooting guide.
