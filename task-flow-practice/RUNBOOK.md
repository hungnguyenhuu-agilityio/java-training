# RUNBOOK — TaskFlow
**Last updated**: 2026-06-09

> Operational runbook for local development deployment. Written by the `ship` skill after Stage 5. Every command is copy-pasteable; every check has a pass condition.

---

## Service Identity

- **Name**: TaskFlow
- **Repo**: /home/hungnguyenhuu/workspace/training/me/java/java-training/task-flow-practice
- **Deployment target**: Local — backend `http://localhost:8080`, frontend `http://localhost:4200`
- **Tech**: Java 21 + Angular 18 + MySQL 8 (Docker port 3307)
- **Owner / on-call**: Hung Nguyen Huu (solo learner)

---

## Prerequisites

| Requirement | Check command | Pass condition |
|-------------|---------------|----------------|
| Docker running | `docker ps` | No error |
| MySQL container up | `docker ps \| grep 3307` | Container listed |
| Java 21 | `java -version` | `openjdk 21` or similar |
| Maven | `mvn -version` | `Apache Maven 3.x` |
| Node / npm | `node -v && npm -v` | Version printed |

---

## Deploy Procedure

### Step 1 — Pre-deploy checks
```bash
# Clean working tree
git -C /home/hungnguyenhuu/workspace/training/me/java/java-training/task-flow-practice \
  status

# Confirm on develop with all tasks merged
git log --oneline -6
```
Pass condition: working tree clean, latest merge commit visible.

### Step 2 — Ensure MySQL container is healthy
```bash
docker ps | grep 3307
# or, if using a named container:
# docker start taskflow-mysql
```
Pass condition: container is running and port 3307 mapped.

### Step 3 — Apply DB migrations (first run or after schema changes)
```bash
# Connect to MySQL and run the migration file
mysql -h 127.0.0.1 -P 3307 -u root -p taskflow_db \
  < /home/hungnguyenhuu/workspace/training/me/java/java-training/task-flow-practice/db/migrations/V1__init.sql
```
Pass condition: no SQL errors printed.

### Step 4 — Build and start the backend
```bash
cd /home/hungnguyenhuu/workspace/training/me/java/java-training/task-flow-practice/taskflow
mvn clean package -q -DskipTests
mvn exec:java -pl taskflow-http \
  -Dexec.mainClass="com.taskflow.http.Server"
```
Pass condition: console prints `TaskFlow server started on port 8080`.

### Step 5 — Start the frontend
```bash
cd /home/hungnguyenhuu/workspace/training/me/java/java-training/task-flow-practice/frontend
npm install
npx ng serve
```
Pass condition: console prints `Application bundle generation complete` and `http://localhost:4200`.

### Step 6 — Post-deploy health check
```bash
# Backend liveness
curl -fsS http://localhost:8080/api/health 2>/dev/null || \
  curl -fsS -o /dev/null -w "%{http_code}" http://localhost:8080/api/tasks \
    -H "Authorization: Bearer invalid" 
# Expected: 401 (server is up and auth filter is active)
```
Pass condition: HTTP response received (401 or 200 — any response means the server is running).

Open browser → `http://localhost:4200` → redirected to `/login` → login succeeds → `/dashboard` loads with stats charts.

---

## Rollback Procedure

### Trigger conditions
- Post-deploy health check returns no response (server not started)
- `ng serve` fails to compile (`ng build` error)
- DB migration fails with SQL error
- Login returns 500 or dashboard stats show wrong data

### Reverse steps
1. **Stop the backend**: `Ctrl+C` in the backend terminal (or `kill $(lsof -ti:8080)`)
2. **Stop the frontend**: `Ctrl+C` in the frontend terminal
3. **Revert to previous commit**:
   ```bash
   git -C /home/hungnguyenhuu/workspace/training/me/java/java-training/task-flow-practice \
     checkout <previous-good-commit-sha>
   ```
4. **Re-run Steps 4–5** of the deploy procedure above with the reverted code.
5. **Verify rollback**: repeat the post-deploy health check — same pass condition.

> No down-migration is needed for v1.0: the single `V1__init.sql` is the baseline and contains no breaking schema changes relative to prior state.

---

## Health Checks

| Check | Command | Pass condition |
|-------|---------|----------------|
| Backend liveness | `curl -o /dev/null -w "%{http_code}" http://localhost:8080/api/tasks -H "Authorization: Bearer x"` | `401` |
| Frontend liveness | Open `http://localhost:4200` in browser | Redirects to `/login` |
| Dashboard stats | `curl http://localhost:8080/api/dashboard/stats -H "Authorization: Bearer $TOKEN"` | JSON with `byStatus` and `byPriority` keys |
| File upload dir | `ls uploads/` | Directory exists (may be empty) |

---

## Common Failure Modes & Remediation

| Symptom | Likely cause | Remediation |
|---------|-------------|-------------|
| Backend fails to start: `Connection refused` to MySQL | MySQL container not running | `docker start <container>` then retry Step 4 |
| Backend starts but all API calls return 500 | `V1__init.sql` not applied | Run Step 3, then restart backend |
| Angular `ng serve` fails: `Cannot find module` | `npm install` not run | `cd frontend && npm install` |
| API calls return `NetworkError` in browser | CORS mismatch or backend not running | Confirm backend is on port 8080; check `CorsFilter.java` for `localhost:4200` |
| Login returns 401 immediately | BCrypt hash mismatch or wrong endpoint | Check `POST /api/auth/login` returns token; verify `Authorization: Bearer <token>` header |
| `uploads/` not found on first file upload | Directory not auto-created | `mkdir -p uploads/` in project root, then restart backend |

---

## Release Log

| Version / Tag | Date | Scope (Task IDs) | Deployer | Outcome |
|---------------|------|------------------|----------|---------|
| v1.0.0 | 2026-06-09 | T001–T016 (all tasks) | Hung Nguyen Huu | Success |
