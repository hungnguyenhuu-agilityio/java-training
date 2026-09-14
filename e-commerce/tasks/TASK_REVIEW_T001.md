# TASK_REVIEW — T001 (Stage 4 code-review, round 1)
**Date**: 2026-09-11
**Reviewer**: Supervisor (`code-review` skill)
**Verdict**: BLOCKED — 1 P0, 7 P1 open. Rework assigned to Common-Infrastructure-Agent (R-INFRA) and Frontend-Implementer (R-FE).
**Round 1 rework status (2026-09-11)**: R-FE complete (F8, F12–F14). R-INFRA F1, F3–F7, F9–F11 fixed (backend 15 run / 0 fail / 1 skipped; CI policy + 9 rejection fixtures pass); F2 written but not proven on a running stack. Paused by user after the step-limit hook stopped R-INFRA — see "Open items (paused)" at the end.

## Baseline evidence at review time

- Backend `./mvnw -B test`: Tests run 10, Failures 0, Errors 0, Skipped 1 (`MySqlMigrationLifecycleTests`, CI-only) — BUILD SUCCESS
- Frontend `npm test -- --watch=false`: 7 passed (1 file)
- `scripts/test-ci-policy.sh`: policy verified + rejection fixtures passed

## Supervisor decisions (user-approved 2026-09-11)

1. The workflow moves to the git repository root `java-training/.github/workflows/`; `e-commerce/.github/` is removed.
2. The Liquibase baseline keeps all 20 tables in T001 (no reduction to a skeleton).

## Findings to resolve

### R-INFRA — Common-Infrastructure-Agent

| ID | Sev | File | Finding | Required fix / verifiable goal |
|---|---|---|---|---|
| F1 | P0 | `e-commerce/.github/workflows/ci-release.yml` | Git root is `java-training/`; GitHub never loads this workflow, so AC6–AC9 cannot run. | Move to `java-training/.github/workflows/ci-release.yml`; set working dirs to `e-commerce/backend`, `e-commerce/frontend`, run Compose/scripts from `e-commerce`; add `paths: ['e-commerce/**', '.github/workflows/ci-release.yml']` filters to `push` and `pull_request`; `cache-dependency-path: e-commerce/frontend/package-lock.json`; update `verify-ci-policy.sh` default path + `test-ci-policy.sh` to the new location; remove `e-commerce/.github/`. Policy scripts must still pass. |
| F2 | P1 | `ci-release.yml:111` | Smoke check calls `${FRONTEND_HEALTH_URL}/api/actuator/health`, but no `/api` proxy exists (no `vercel.json`, nginx has none). Production release always fails. | Add `/api/` proxy in `frontend/nginx.conf` → `http://backend:8080/` and a `frontend/vercel.json` rewrite `/api/:path*` → backend origin (origin as a documented placeholder the human sets; no secret committed) plus SPA fallback. Extend `scripts/smoke-stack.sh` to curl `http://localhost:4200/api/actuator/health`. |
| F3 | P1 | `ci-release.yml:103` | `railway up --detach` returns before the deployment is live; smoke may hit the old instance and report green. | Remove `--detach` (CLI waits for build/deploy and fails on failure). Add a policy check rejecting `--detach`. |
| F4 | P1 | `backend/src/main/resources/application.properties` | Railway injects `PORT`; app is fixed on 8080. | Add `server.port=${PORT:8080}`. |
| F5 | P1 | `ci-release.yml` | CI policy scripts are never executed in CI. | Add `ci-policy` job running `scripts/test-ci-policy.sh`; add it to `deploy-production.needs`; update policy script's required `needs` pattern. |
| F6 | P1 | `ArchitectureRulesTests.java:17` | No illegal-dependency fixture; layer rule is vacuous today. | Add a test that imports a test-only fixture violating the domain→adapter/framework rule and asserts the rule throws `AssertionError`. |
| F7 | P1 | rollback (AC9/AC10) | Rollback is prose only. | Add a `workflow_dispatch` rollback path that redeploys a given known-good SHA to Railway + Vercel using the same `production` environment/concurrency group and runs the same smoke checks; the policy script must treat it as the rollback path (not a second release path) and still reject preview/PR deploys. Update README runbook. Real execution evidence is human-gated. |
| F9 | P2 | `ci-release.yml:86` | Deploy secrets are job-level env, visible to `npm install --global`. | Scope secrets to the deploy/smoke steps only. |
| F10 | P2 | `application.properties:3-5` | Production silently falls back to default DB credentials. | Move defaults to `application-local.properties`; base config uses `${SPRING_DATASOURCE_*}` without defaults. Compose/tests must still work. |
| F11 | P2 | `LiquibaseBaselineMigrationTests.java:65` | Hard-coded user ids 1/2 in a shared H2 context. | Use generated keys. |

### R-FE — Frontend-Implementer

| ID | Sev | File | Finding | Required fix / verifiable goal |
|---|---|---|---|---|
| F12 | P1 | `frontend/*` | AC11 / UI_SPEC §6 require Angular Material installed and themed; none present. | Install Angular Material (+ CDK) compatible with the installed Angular; light-only theme via supported theming API with UI_SPEC token overrides; shell uses Material toolbar + sidenav (compact drawer). Existing 7 spec behaviors must keep passing (adapt selectors, not intent). No external font/icon CDN. |
| F8 | P1 | Evidence (Gate 6) | No screenshot/visual-regression or responsiveness evidence at 375/768/1280. | Add automated browser screenshot + no-horizontal-overflow checks at 375, 768, 1280px for the root shell; paste output. |
| F13 | P2 | `app.css` / `styles.css` | Tokens duplicated in `:host` and `:root`. | Single definition. |
| F14 | P2 | `app.html:802` | Menu lacks `aria-controls`, Escape close, focus handoff. | Covered by Material sidenav; add a spec for Escape close. |

## Not in scope for rework

- Splitting `001-baseline-schema.sql` into per-table changesets (advisory P2; not assigned).
- P3: SHA-pinning actions, Compose MySQL host port, ArchUnit version vs Modulith BOM.

## Evidence (to be filled after rework, round 2)

| Row | Result |
|---|---|
| New test(s) cover acceptance criteria | ☑ PASS (R-INFRA continuation). The full output is under Demonstration → AFTER (R-INFRA continuation). **Backend** (2026-09-11T10:08:21Z) `./mvnw -B test`: `Tests run: 16, Failures: 0, Errors: 0, Skipped: 1` (`MySqlMigrationLifecycleTests`, CI-only), `BUILD SUCCESS`. It includes `RuntimeConfigurationTests` (5: PORT binding, required datasource outside local, local defaults on 13306/`ecommerce-local`, `MYSQL_HOST_PORT` override), `ArchitectureRulesTests` (3, incl. illegal-dependency fixture), `LiquibaseBaselineMigrationTests` (4), `OpenApiIntegrationTests`, `ApiDocumentationExposureTests`. **CI policy** (10:08:36Z) `bash scripts/test-ci-policy.sh`: `CI workflow policy verified … one main-only protected release path; one main-only rollback path.` / `CI policy rejection fixtures passed.` exit=0 (10 rejection fixtures, incl. missing `test:visual`). Negative probe: `CI policy violation: frontend job must run the Playwright visual regression suite`, exit=1. **Compose stack** (10:13:40Z) `docker compose up --build --detach --wait --wait-timeout 180 && ./scripts/smoke-stack.sh`: mysql/backend/frontend all `(healthy)` on 13306/18080/14200, `Backend, frontend, and frontend /api proxy health checks passed.`, `:14200/api/actuator/health` → `HTTP/1.1 200`, `Server: nginx/1.29.8`, `{"status":"UP"}`, logged by the T001 frontend container. Torn down with `docker compose down --volumes`. Not run locally: the GitHub-hosted `npm run test:visual` CI step and real release/rollback runs (human-gated, O7). |
| Visual regression | ☑ PASS (R-FE, 2026-09-11T08:56:45Z). `npm run test:visual` (Playwright 1.61.0, Chromium 1228) compared against committed baselines `frontend/e2e/shell.visual.ts-snapshots/{root-shell-375,root-shell-768,root-shell-1280,compact-drawer-375}-linux.png` (created 08:56Z with `--update-snapshots`, then re-run without it): `✓ root shell at 375px …` `✓ … 768px …` `✓ … 1280px …` `✓ compact drawer opens, closes with Escape, and restores focus at 375px` `✓ exposes the approved semantic color foundation through the Material theme` `✓ skip link is the first visible focus target and moves focus to main` — `6 passed (4.7s)`. Full output: Demonstration → AFTER (frontend). Note: baselines are Linux-rendered (`-linux` suffix); per UI_SPEC §15 they become committed baselines only after this task passes Stage 4. |
| Design-system compliance | ☑ PASS (R-FE, 2026-09-11T08:57:04Z). Token audit: `grep -rnE "#[0-9a-fA-F]{3,8}\b\|rgba?\(\|hsla?\(" src` → 13 matches, all in `src/styles.scss:6-18` (the single `:root` token block; F13 duplication in `app.css :host` removed); `--color-*:` definitions per file → `src/styles.scss:13` only. Theme: `mat.theme` (light, `mat.$blue-palette`, system font stack, density 0) + `mat.theme-overrides` mapping `--mat-sys-*` to the tokens; browser check resolves `--mat-sys-primary` = `rgb(21, 101, 192)` (#1565c0), `--mat-sys-on-surface` = `rgb(31, 41, 55)`, `--mat-sys-background` = `rgb(245, 247, 250)`, `color-scheme: light`. External assets: `grep -rniE "https?://\|@import\|googleapis\|material-icons\|material-symbols\|<link" src` → only `src/index.html:8 <link rel="icon" href="favicon.ico">` (local). UI libraries: `npm ls --depth=0` → `@angular/cdk@22.1.6`, `@angular/material@22.1.6` only. Shell uses Material primitives directly (`mat-toolbar`, `mat-sidenav`, `mat-nav-list`, `mat-button`); no wrapper components, no feature screens. |
| Responsiveness | ☑ PASS (R-FE, 2026-09-11T08:56:45Z). Same run: at 375/768/1280px each test asserts `/` → `/products`, `h1 Products` visible, brand visible, menu button visible only `<600px` and primary navigation visible only `>=600px`, and no horizontal overflow on `html` and `mat-sidenav-content` (`scrollWidth <= clientWidth`, expected `[]`); drawer-open state at 375px also asserts no overflow. Negative probe proving the check detects overflow: `negative probe (600px child at 375px viewport) detected: ["HTML: 608 > 375"]`. Screenshots reviewed visually: no clipping at any width. |

## Demonstration (rework round 1)

### BEFORE (R-INFRA, captured prior to any rework file change)

Command: `cd java-training && git rev-parse --show-toplevel && ls -a .github e-commerce/.github 2>&1; cd e-commerce && bash scripts/test-ci-policy.sh; grep -n 'PORT\|datasource' backend/src/main/resources/application.properties`

```text
Captured: 2026-09-11T08:52:12Z
/home/hungnguyenhuu/workspace/training/me/java/java-training
ls: cannot access '.github': No such file or directory
e-commerce/.github:
.
..
workflows
CI workflow policy verified: CI on pull requests/pushes; one main-only protected release path.
CI policy rejection fixtures passed.
exit=0
3:spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/ecommerce}
4:spring.datasource.username=${SPRING_DATASOURCE_USERNAME:ecommerce}
5:spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:ecommerce}
```

Observations: no workflow at the git root (GitHub never loads it); the policy check passes against the wrong location; no `server.port=${PORT...}`; datasource credentials fall back to defaults in the base profile.

### BEFORE (R-INFRA continuation)

Command: `cd e-commerce && grep -n 'ports:' -A2 docker-compose.yml; grep -n 'localhost' scripts/smoke-stack.sh; grep -n 'playwright\|test:visual' ../.github/workflows/ci-release.yml || echo 'no visual step'`

```text
Captured: 2026-09-11T10:06:59Z
9:    ports:
10-      - "3306:3306"
11-    volumes:
--
28:    ports:
29-      - "8080:8080"
30-    depends_on:
--
43:    ports:
44-      - "4200:8080"
45-    depends_on:
4:curl --fail --silent --show-error --retry 12 --retry-delay 2 --retry-all-errors http://localhost:8080/actuator/health >/dev/null
5:curl --fail --silent --show-error --retry 12 --retry-delay 2 --retry-all-errors http://localhost:4200/health >/dev/null
6:curl --fail --silent --show-error --retry 12 --retry-delay 2 --retry-all-errors http://localhost:4200/api/actuator/health >/dev/null
no visual step
```

Observations: host ports 3306/8080 are hard-coded and collide with another local project, so the stack cannot start alongside it and a check on `:8080` can reach the wrong process. The smoke script hard-codes the same ports. CI never runs the Playwright visual suite.

### AFTER (R-INFRA continuation — O1, O2, O3)

**Backend** — `cd e-commerce/backend && ./mvnw -B test`

```text
Captured: 2026-09-11T10:08:21Z
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1 -- in com.example.ecommerce.MySqlMigrationLifecycleTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.example.ecommerce.ECommerceApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.example.ecommerce.OpenApiIntegrationTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.example.ecommerce.ArchitectureRulesTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.example.ecommerce.LiquibaseBaselineMigrationTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.example.ecommerce.RuntimeConfigurationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.example.ecommerce.ApiDocumentationExposureTests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

**CI policy** — `bash scripts/test-ci-policy.sh`, plus a negative probe with `npm run test:visual` removed

```text
Captured: 2026-09-11T10:08:36Z
CI workflow policy verified: CI on pull requests/pushes; one main-only protected release path; one main-only rollback path.
CI policy rejection fixtures passed.
exit=0
CI policy violation: frontend job must run the Playwright visual regression suite
negative-probe exit=1
```

`docker compose config` resolves the defaults to host ports 18080 (backend), 14200 (frontend) and 13306 (MySQL). The container targets stay 8080 and 3306.

**Compose stack, first run (failed)** — 2026-09-11T10:08:38Z: `container e-commerce-frontend-1 is unhealthy`. The smoke checks passed from the host, but `--wait` failed. Diagnosis from `docker inspect .State.Health.Log`: `wget: can't connect to remote host: Connection refused`. Inside the container, busybox `wget` resolves `localhost` to `::1`, while nginx listens only on IPv4 `8080` (`wget http://127.0.0.1:8080/health` → `ok`; `wget http://localhost:8080/health` → refused). Fix: the Compose frontend healthcheck now uses `http://127.0.0.1:8080/health`, the same URL as the Dockerfile `HEALTHCHECK`. The stack was torn down with `docker compose down --volumes` before the re-run.

**Compose stack, re-run** — `docker compose up --build --detach --wait --wait-timeout 180 && ./scripts/smoke-stack.sh`

```text
Captured: 2026-09-11T10:13:40Z
compose config ok
 Container e-commerce-mysql-1 Healthy
 Container e-commerce-backend-1 Healthy
 Container e-commerce-frontend-1 Healthy
up exit=0
NAME                    IMAGE                 STATUS                    PORTS
e-commerce-backend-1    e-commerce-backend    Up 21 seconds (healthy)   0.0.0.0:18080->8080/tcp, [::]:18080->8080/tcp
e-commerce-frontend-1   e-commerce-frontend   Up 5 seconds (healthy)    80/tcp, 0.0.0.0:14200->8080/tcp, [::]:14200->8080/tcp
e-commerce-mysql-1      mysql:8.4.11          Up 37 seconds (healthy)   33060/tcp, 0.0.0.0:13306->3306/tcp, [::]:13306->3306/tcp
Backend, frontend, and frontend /api proxy health checks passed.
smoke exit=0
--- direct responses ---
{"groups":["liveness","readiness"],"status":"UP"} <- :18080/actuator/health 200
ok <- :14200/health 200
HTTP/1.1 200 / Server: nginx/1.29.8 / {"groups":["liveness","readiness"],"status":"UP"}   (:14200/api/actuator/health)
--- attribution ---
mysql: c15b99696be1 3306/tcp -> 0.0.0.0:13306
backend: 6472e606ff58 8080/tcp -> 0.0.0.0:18080
frontend: fc5280c7b357 8080/tcp -> 0.0.0.0:14200
backend-1  | ... c.e.ecommerce.ECommerceApplication : Started ECommerceApplication in 10.657 seconds
frontend-1  | 192.168.16.1 - - [11/Sep/2026:10:14:20 +0000] "GET /api/actuator/health HTTP/1.1" 200 60 "-" "curl/8.5.0" "-"
--- teardown ---
 Volume e-commerce_mysql-data Removed
 Network e-commerce_default Removed
docker compose ps -> (no containers)
```

Attribution: `docker port` shows the T001 containers own 13306, 18080 and 14200. The frontend nginx access log records the `/api/actuator/health` request as proxied, and the backend log shows this container's `Started ECommerceApplication`. The other project's containers on 3306 and 8080 were never touched.

**DELTA**: Host ports went from hard-coded `3306/8080/4200` to `${MYSQL_HOST_PORT:-13306}`, `${BACKEND_HOST_PORT:-18080}` and `${FRONTEND_HOST_PORT:-14200}`, and `smoke-stack.sh` reads the same variables. The local profile's host-mode datasource moved from `localhost:3306` with password `ecommerce` to `localhost:${MYSQL_HOST_PORT:13306}` with `ecommerce-local`, matching Compose. The Compose frontend healthcheck moved from `localhost` (IPv6, refused) to `127.0.0.1`: before, `--wait` failed; after, all 3 containers are healthy. The CI `frontend` job now runs `npx playwright install --with-deps chromium`, `npm run test:visual` and, on failure only, uploads `e-commerce/frontend/test-results/`. None of these steps has secrets in scope. The policy gained `require_pattern 'npm run test:visual'` and fixture #10 (`missing-visual-tests.yml`). Backend tests went from 15 to 16 run (0 fail, 1 skipped CI-only). The earlier "`:8080/actuator/health` passed" reading is superseded by the attributed run above.
**WITNESS**: `RuntimeConfigurationTests` › `should_provide_host_mode_datasource_defaults_in_the_local_profile` (updated) and `should_follow_the_compose_mysql_host_port_override_in_the_local_profile` (new). `scripts/test-ci-policy.sh` fixture "a frontend job without visual regression tests". Running-system check: the Compose re-run above (AC1: one command, health-aware `depends_on`, frontend `/api` proxy proven through nginx).

## Demonstration (rework round 1 — frontend)

### BEFORE (captured before any frontend file change)

`grep` printed no lines: `@angular/material` and `@angular/cdk` are absent from `package.json`.

```text
Captured: 2026-09-11T08:52:22Z
$ grep -n '@angular/material\|@angular/cdk' package.json; npm test -- --watch=false 2>&1 | tail -8
 RUN  v4.1.11 /home/hungnguyenhuu/workspace/training/me/java/java-training/e-commerce/frontend


 Test Files  1 passed (1)
      Tests  7 passed (7)
   Start at  15:52:24
   Duration  1.04s (transform 46ms, setup 196ms, import 70ms, tests 231ms, environment 439ms)

```

### AFTER (frontend)

```text
Captured: 2026-09-11T08:57:15Z
$ grep -n '@angular/material\|@angular/cdk' package.json    (captured 2026-09-11T08:58:06Z)
15:    "@angular/cdk": "~22.1.5",
20:    "@angular/material": "~22.1.5",
$ npm ls @angular/material @angular/cdk --depth=0   → @angular/cdk@22.1.6, @angular/material@22.1.6

$ npm test -- --watch=false && npm run build
 Test Files  1 passed (1)
      Tests  7 passed (7)
   Start at  15:57:17
   Duration  1.14s (transform 40ms, setup 191ms, import 219ms, tests 293ms, environment 340ms)

Initial chunk files | Names         |  Raw size | Estimated transfer size
main-YQYG2VIL.js    | main          | 363.03 kB |                84.51 kB
styles-MLSBU6B5.css | styles        |   9.73 kB |                 1.45 kB
                    | Initial total | 372.76 kB |                85.97 kB
Application bundle generation complete. [1.687 seconds] - 2026-09-11T08:57:20.541Z
(no budget warnings: initial 372.76 kB < 500 kB warning; app.css below 4 kB anyComponentStyle)
```

```text
Captured: 2026-09-11T08:56:45Z
$ npm run test:visual
Running 6 tests using 1 worker

  ✓  1 e2e/shell.visual.ts:20:7 › root shell at 375px renders without horizontal overflow (323ms)
  ✓  2 e2e/shell.visual.ts:20:7 › root shell at 768px renders without horizontal overflow (281ms)
  ✓  3 e2e/shell.visual.ts:20:7 › root shell at 1280px renders without horizontal overflow (299ms)
  ✓  4 e2e/shell.visual.ts:42:5 › compact drawer opens, closes with Escape, and restores focus at 375px (317ms)
  ✓  5 e2e/shell.visual.ts:59:5 › exposes the approved semantic color foundation through the Material theme (202ms)
  ✓  6 e2e/shell.visual.ts:97:5 › skip link is the first visible focus target and moves focus to main (312ms)

  6 passed (4.7s)
```

**DELTA**: Material/CDK absent → 22.1.6 installed and themed (light, token overrides). Unit suite 7 → 7: the jsdom token spec moved to the browser suite (jsdom does not load global styles, which is what forced the F13 duplication), and a new Escape-close/focus-restore spec was added. New browser suite: 6 tests, 4 committed screenshot baselines.
**WITNESS**: `src/app/app.spec.ts` › "closes compact navigation with Escape and returns focus to the menu control" (F14); `e2e/shell.visual.ts` (F8, F12 theme, F13 single token source).

## Open items (paused 2026-09-11 by user — resume before code-review round 2)

| # | Item | Owner | Notes |
|---|---|---|---|
| O1 | Compose host-port clash | Common-Infrastructure | Round-1 smoke run failed: `Bind for 0.0.0.0:3306 failed: port is already allocated` (another local project's `orderly-db-version-4` MySQL). Host 8080 is also held by that project's Java app, so R-INFRA's "`:8080/actuator/health` passed" reading hit the wrong process and is **not valid evidence**. **User decision**: T001 Compose uses different host ports and must coexist with the other project (do not stop it). Make host ports non-default/overridable (e.g. `${MYSQL_HOST_PORT}`, `${BACKEND_HOST_PORT}`, `${FRONTEND_HOST_PORT}`) and have `scripts/smoke-stack.sh` + README use the same values. |
| O2 | F2 live proof | Common-Infrastructure | After O1: `docker compose up --build --detach --wait` + `scripts/smoke-stack.sh` (incl. frontend `/api/actuator/health`), paste output. |
| O3 | Visual tests in CI | Common-Infrastructure | Add to `frontend` job: `npx playwright install --with-deps chromium`, `npm run test:visual`, upload report on failure. |
| O4 | AFTER evidence (R-INFRA) | Common-Infrastructure | Paste AFTER/DELTA/WITNESS and fill "New test(s) cover acceptance criteria" row. |
| O5 | Supervisor review of R-INFRA extras | Supervisor | `.invalid` placeholder guard in both release jobs, `scripts/smoke-production.sh`, rollback job SHA-ancestry check; local password mismatch (`application-local.properties` `ecommerce` vs Compose `ecommerce-local`). |
| O6 | Toolbar link colour | User | Material default muted text vs primary blue — UI_SPEC silent. |
| O7 | Human-gated setup | User | Real Railway origin in `frontend/vercel.json`; branch protection requiring `ci-policy`, `backend`, `frontend`, `migration`, `compose-smoke`; `production` environment reviewers + secrets; Railway `SPRING_DATASOURCE_*`; Vercel root/output dir; confirm `railway up` (no `--detach`) waits for healthy deploy; one real release + rollback run. |

Then: code-review round 2 → security-review (High risk) → blast-radius → migration-safety → verify.

### Open-items status (Supervisor, 2026-09-11)

- O1–O4: done by R-INFRA continuation (evidence above). O1 also aligned `application-local.properties` host-mode defaults to `localhost:${MYSQL_HOST_PORT:13306}` / `ecommerce-local`. O2 surfaced and fixed a real defect: Compose frontend healthcheck used `localhost` (busybox resolves `::1`; nginx listens IPv4 only) → now `127.0.0.1`.
- O5: Supervisor review accepted — `.invalid` origin guard, shared `scripts/smoke-production.sh`, rollback SHA regex + `git merge-base --is-ancestor` on `main`. Local password mismatch resolved via O1.
- O6: open (user) — toolbar link colour.
- O7 addendum: workflow uses `paths` filters (`e-commerce/**`, workflow file). If these jobs become required status checks on `main`, PRs that touch no matching path never report them and stay blocked — use a path-independent gate job or require checks only via rulesets scoped accordingly.

## Stage 4 code-review — round 2 (Supervisor, 2026-09-11)

**Verdict**: PASS — 0 P0, 0 P1, 0 P2 open; 1 new P3. F1–F14 and O1–O5 verified resolved.

Independent re-run by Supervisor (not agent-reported), 2026-09-11 ~10:16Z:

| Check | Result |
|---|---|
| `backend ./mvnw -B test` | `Tests run: 16, Failures: 0, Errors: 0, Skipped: 1` — `BUILD SUCCESS` (skip = CI-only MySQL Testcontainers lifecycle) |
| `frontend npm test -- --watch=false` | `7 passed (7)` |
| `frontend npm run build` | `Application bundle generation complete.` — no budget warnings |
| `frontend npm run test:visual` | `6 passed (7.0s)` |
| `bash scripts/test-ci-policy.sh` | `CI workflow policy verified … one main-only protected release path; one main-only rollback path.` / `CI policy rejection fixtures passed.` |
| `docker compose up --build --detach --wait` + `docker compose ps` | mysql `13306->3306 (healthy)`, backend `18080->8080 (healthy)`, frontend `14200->8080 (healthy)` — T001 containers own the ports |
| `scripts/smoke-stack.sh` | `Backend, frontend, and frontend /api proxy health checks passed.` |
| `docker compose down --volumes` | exit 0 |

New finding:
- P3 — `frontend/.dockerignore` omits `e2e/`, `test-results/`, `playwright-report/` (build-context bloat only).

Still open outside code-review: O6 (toolbar link colour, user), O7 (human-gated GitHub/Railway/Vercel setup + real release/rollback run). Next gates: security-review → blast-radius → migration-safety → verify.

## Stage 4 security-review (Supervisor inline, 2026-09-11)

Built-in `security-review` could not run: it diffs `origin/HEAD...` (ref absent) and only committed changes, while T001 is uncommitted. Performed inline over the same scope: `.github/workflows/ci-release.yml`, `SecurityConfiguration`, actuator/springdoc config, Dockerfiles, Compose, nginx, `vercel.json`, datasource config, Liquibase baseline.

**Verdict**: no High. 2 Medium, 2 Low open — fix-or-accept decision pending with user.

| ID | Sev | Location | Finding | Recommended fix |
|---|---|---|---|---|
| S1 | Medium | `docker-compose.yml:10,29,44` | Host ports publish on `0.0.0.0`: MySQL (known dev root/app passwords) and the `local`-profile backend (Swagger UI + api-docs enabled) are reachable from the LAN. | Bind to loopback: `"127.0.0.1:${MYSQL_HOST_PORT:-13306}:3306"` (same for backend/frontend); smoke script unchanged. |
| S2 | Medium | `ci-release.yml` deploy/rollback jobs | Supply chain in the job that holds production secrets: actions pinned by tag (`@v4`), and `npm install --global @railway/cli vercel` resolves unpinned transitive deps whose install scripts run before the secret-scoped steps and can tamper with the runner (PATH/binaries) to capture tokens later in the job. | SHA-pin `actions/*`; install CLIs from a committed lockfile (`npm ci` in a tools dir) with `--ignore-scripts` where the CLI permits, or use the vendors' official actions pinned by SHA. |
| S3 | Low | `ci-release.yml:136,187` | `vercel deploy --token "$VERCEL_TOKEN"` puts the token in argv (process list). Masked in logs. | Drop `--token`; the Vercel CLI reads `VERCEL_TOKEN` from env. |
| S4 | Low | `backend/Dockerfile:10` | `curl` installed in the runtime image only for healthchecks; extra tooling for post-exploitation. | Accept for T001, or use a Java-based/`wget`-less healthcheck later. |

Verified safe:
- Deploy only on `push` to `refs/heads/main`; rollback only on `workflow_dispatch` from `main`; no `pull_request_target`; `permissions: contents: read`; secrets scoped to deploy/smoke steps and absent from PR/non-main jobs.
- Rollback input passed via `env` (no `${{ }}` interpolation into shell → no script injection); full-SHA regex + `git merge-base --is-ancestor`.
- Actuator: `Exposing 1 endpoint` (health only), `show-details=never`; `anyRequest().authenticated()` with no login mechanism → deny-by-default. No generated default password in logs (`grep -c 'Using generated security password'` → `0`).
- Springdoc api-docs + Swagger UI disabled by default; enabled only in `local`; `ApiDocumentationExposureTests` proves 404 under `production`.
- Production datasource has no credential defaults (`RuntimeConfigurationTests`); `.invalid` Vercel origin guard blocks releases until configured; no secret values committed.
- Schema stores `password_hash` / `token_hash` (no plaintext credentials/tokens).
- Info: rollback can redeploy any `main` ancestor, including pre-security-fix SHAs — rely on `production` environment required reviewers (O7).

## Stage 4 migration-safety (Supervisor, 2026-09-11)

**Verdict**: GO. Reversible: yes (tested). Downtime: no (greenfield). Data-loss risk: none (no pre-existing data). Blocking gaps: none.

Evidence — MySQL 8.4.11 Testcontainers lifecycle, run locally by Supervisor (first real execution; previously CI-only and skipped):
`./mvnw -B -DmysqlMigrationTest=true -Dtest=MySqlMigrationLifecycleTests test` → `MySQL migration lifecycle: up->rollback->up completed in 5587 ms; table counts 20->0->20.` / `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`. H2 lifecycle also green in `LiquibaseBaselineMigrationTests`.

| Checklist item | Result |
|---|---|
| Managed by migration tool | ☑ Liquibase formatted SQL via `db.changelog-master.yaml`; `ddl-auto=validate` everywhere (PROJECT_SPEC: "Liquibase is the only production schema mutation mechanism; Hibernate schema auto-update is prohibited.") |
| In scope | ☑ AC2 baseline; full 20-table scope user-approved 2026-09-11 (`memory/decisions.md`) |
| Destructive ops inventoried | ☑ Only `--rollback DROP TABLE` ×20 (child→parent order verified). README runbook forbids Liquibase rollback in production. |
| Tested rollback | ☑ up→down→up on H2 and MySQL 8.4.11 |
| Expand-contract / coexistence | ☑ N/A — greenfield, no prior app version reads these tables |
| Zero-downtime ordering | ☑ Purely additive; applied at first app startup |
| Locks on large tables | ☑ N/A — empty tables |
| Backfill | ☑ N/A — no data transformation |
| Dry-run on prod-like data | ☑ N/A with rationale — production DB is empty at first release; runtime recorded (5.6 s on MySQL) |
| No silent data loss | ☑ No existing data affected |

Advisory (non-blocking, user chose to keep one baseline): the 20 tables are one changeset and MySQL DDL is non-transactional — if the first production apply fails midway, recovery is drop-and-recreate the empty database before retry. Future tasks must add new changesets, never edit `001-baseline-schema`.

## Stage 4 blast-radius (Supervisor, 2026-09-11)

> Planning estimates and law-sourced maximums only — not legal advice; qualified counsel/DPIA required before handling real personal data.

**Executive summary.** T001 introduces *storage shape* for personal data but no code that ingests, reads, logs, or transmits it, and the project is a practice system (PRD/PROJECT_SPEC: "E-Commerce Java Practice"; no declared jurisdiction, no real users; NFR-013 reference data is synthetic). Current realistic breach impact is therefore **≈ $0 / no notifiable personal data**. The highest *present* exposure is not customer data but **production deployment credentials in CI (S2)** and **LAN-reachable local MySQL/Swagger (S1)**. If the system later goes live with real customers, T003 (identity) and T007–T009 (checkout/payment) become the sensitive-data paths and must be re-assessed.

**Sensitive data inventory (schema-level, `backend/src/main/resources/db/changelog/changes/001-baseline-schema.sql`)**

| Field(s) | Tier | Protection at rest | Notes |
|---|---|---|---|
| `users.email` + `users.password_hash` | T3 | Hash column (algorithm set by T003); no field encryption | Credential-stuffing value if hash weak |
| `refresh_sessions.token_hash`, `family_id` | T3 | Hashed token | Session hijack only if raw tokens leak elsewhere |
| `orders.shipping_name/phone/address_line1/2/city/region/postal_code/country_code` | T3 (phone + address) | None (plaintext) | Immutable snapshot → long retention |
| `users.full_name` | T4 | None | |
| `payments.provider_session_id/provider_payment_id`, `refunds.provider_refund_id` | T4 | None | Stripe references; **no PAN/card data stored** (T2 absent) |
| `idempotency_keys.response_body` (TEXT) | T3 (potential) | None | May echo order/PII responses — T007+ must redact or TTL-purge |
| `order_status_histories.note`, `orders.note` | T4 (free text) | None | Free text can carry PII |
| CI secrets `RAILWAY_TOKEN`, `VERCEL_TOKEN`, `*_ORG/PROJECT_ID` | Credential (infra) | GitHub `production` environment secrets | Full production takeover if exfiltrated |
| Compose `MYSQL_ROOT_PASSWORD=root-local`, `ecommerce-local` | Credential (dev) | Plaintext, dev-only | Exposed via S1 |

**Data flow (T001 state)**

```mermaid
flowchart LR
  B[Browser] -->|HTTPS| V[Vercel static SPA]
  V -->|rewrite /api/*| R[Railway Spring Boot]
  B -.local.-> N[nginx :14200] -->|/api/| R2[backend :18080 local profile]
  R --> DB[(MySQL — 20 tables, empty)]
  R2 --> DBL[(Compose MySQL :13306)]
  GH[GitHub Actions production env] -->|RAILWAY_TOKEN| R
  GH -->|VERCEL_TOKEN| V
  LAN((LAN host)) -.S1 0.0.0.0.-> DBL
  LAN -.S1.-> R2
```
No PII flows exist yet: no controllers, repositories, or log statements touch the inventoried fields; actuator exposes health only; springdoc disabled outside `local`.

**Top 5 exposure vectors** (score = tier multiplier × likelihood(1–3) × scale(1–3) × completeness(1–3); practice-scale)

| # | Vector | Evidence | Score | Rationale |
|---|---|---|---|---|
| 1 | CI supply chain → production token theft (S2) | `ci-release.yml` `npm install --global` + tag-pinned actions in `production` jobs | credential ×5 · L2 · S3 · C3 = **90** | Token = full control of prod backend/frontend and DB via Railway |
| 2 | LAN access to local MySQL/Swagger (S1) | `docker-compose.yml:10,29` `0.0.0.0` | ×3 · L2 · S1 · C3 = **18** | Dev data only; shared networks |
| 3 | Future plaintext shipping PII in `orders` | schema `orders.shipping_*` | ×3 · L1 · S3 · C3 = **27** (latent) | Activated by T007; long-lived snapshot |
| 4 | Future `idempotency_keys.response_body` PII echo | schema | ×3 · L1 · S2 · C2 = **12** (latent) | Activated by T007/T008 |
| 5 | Rollback redeploys pre-fix SHA | `rollback-production` any `main` ancestor | ×3 · L1 · S2 · C2 = **12** | Mitigated by environment reviewers (O7) |

**Regulatory + financial impact**

| Jurisdiction | Law-sourced maximum (statute) | Applies now? | Realistic planning range |
|---|---|---|---|
| EU/EEA | GDPR Art. 83(5): up to €20M or 4% worldwide annual turnover (higher); Art. 83(4): up to €10M or 2% for security/notification failures (Art. 32–34) | No — no real data subjects | $0 now; if live, practice-scale small-business range is dominated by notification/forensics, not max fines |
| California | CCPA Civ. Code § 1798.155: up to $2,500 per violation / $7,500 intentional; § 1798.150 private action $100–$750 per consumer per incident (base statutory amounts; CPI-adjusted periodically) | No | $0 now |
| Vietnam (developer location, if users there) | Decree 13/2023/ND-CP on personal data protection — administrative penalties per implementing regulations | No | $0 now; counsel to confirm current penalty schedule |
| Payment (PCI DSS) | Contractual, not statutory | No — Stripe-hosted checkout, no PAN stored | Keep SAQ-A scope by never storing card data |

**Hardening roadmap** (ordered by (impact × severity) / effort)

1. **S2** SHA-pin actions; install deploy CLIs from a lockfile/official pinned actions — high impact, low effort. *(T001 fix candidate)*
2. **S1** Bind Compose ports to `127.0.0.1` — medium impact, trivial effort. *(T001 fix candidate)*
3. **S3** Remove `--token` argv for Vercel — low impact, trivial effort. *(T001 fix candidate)*
4. O7: `production` environment required reviewers + branch protection — high impact, human-only.
5. T003: enforce strong adaptive password hashing (Argon2id/bcrypt) and never log raw refresh tokens — add to T003 AC review.
6. T007/T008: redact or TTL-purge `idempotency_keys.response_body`; define retention for shipping snapshots — add to those guides at pickup.

## User decisions after Stage 4 gates (2026-09-11)

- S1, S2, S3: **fix now** in T001 (R-INFRA security follow-up).
- S4 (`curl` in backend runtime image): **accepted** for T001.
- O6: **closed** — keep Material default toolbar link colour; screenshot baselines unchanged.

### BEFORE (security follow-up S1–S3)

Captured: 2026-09-11T10:29:12Z

```
$ grep -n 'uses:\|npm install --global\|--token\|vercel deploy' .github/workflows/ci-release.yml
29:      - uses: actions/checkout@v4
39:      - uses: actions/checkout@v4
40:      - uses: actions/setup-java@v4
54:      - uses: actions/checkout@v4
55:      - uses: actions/setup-node@v4
70:        uses: actions/upload-artifact@v4
84:      - uses: actions/checkout@v4
85:      - uses: actions/setup-java@v4
100:      - uses: actions/checkout@v4
123:      - uses: actions/checkout@v4
130:      - uses: actions/setup-node@v4
134:        run: npm install --global @railway/cli@5.52.0 vercel@59.15.1
147:        run: vercel deploy --prod --yes --token "$VERCEL_TOKEN"
162:      - uses: actions/checkout@v4
181:      - uses: actions/setup-node@v4
185:        run: npm install --global @railway/cli@5.52.0 vercel@59.15.1
198:        run: vercel deploy --prod --yes --token "$VERCEL_TOKEN"
$ grep -n 'ports:' -A1 e-commerce/docker-compose.yml
9:    ports:
10-      - "${MYSQL_HOST_PORT:-13306}:3306"
--
28:    ports:
29-      - "${BACKEND_HOST_PORT:-18080}:8080"
--
43:    ports:
44-      - "${FRONTEND_HOST_PORT:-14200}:8080"
```

### AFTER (security follow-up S1–S3)

Captured: 2026-09-11T10:32:19Z

Changes: S1 compose ports bound to 127.0.0.1; S2 actions pinned to peeled tag SHAs (lightweight tags, no `^{}`), deploy CLIs from `tools/deploy` lockfile via `npm ci` (install scripts kept: `@railway/cli` postinstall fetches the binary — with `--ignore-scripts` `railway --version` fails with the "install the CLI directly" fallback), rollback installs the locked CLIs before checking out the rollback SHA; S3 `--token` argv removed. Policy + 4 new rejection fixtures (tag pin, short SHA, global npm install, `--token`); the policy was run red against the unfixed workflow first (`CI policy violation: deployment CLIs must come from the committed tools/deploy lockfile, not a global npm install`).

```
$ grep -n 'uses:\|npm install --global\|--token\|vercel deploy' .github/workflows/ci-release.yml
29:      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
39:      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
40:      - uses: actions/setup-java@cf277c60eb25467037889841efdb72551f06f6c3 # v4.9.1
54:      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
55:      - uses: actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020 # v4.4.0
70:        uses: actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02 # v4.6.2
84:      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
85:      - uses: actions/setup-java@cf277c60eb25467037889841efdb72551f06f6c3 # v4.9.1
100:      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
123:      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
130:      - uses: actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020 # v4.4.0
148:        run: "$GITHUB_WORKSPACE/e-commerce/tools/deploy/node_modules/.bin/vercel" deploy --prod --yes
163:      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262 # v4.4.0
166:      - uses: actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020 # v4.4.0
200:        run: "$GITHUB_WORKSPACE/e-commerce/tools/deploy/node_modules/.bin/vercel" deploy --prod --yes
$ bash scripts/test-ci-policy.sh
CI workflow policy verified: CI on pull requests/pushes; one main-only protected release path; one main-only rollback path.
CI policy rejection fixtures passed.
exit=0
$ docker compose config | grep -B2 -A3 published
        host_ip: 127.0.0.1
        target: 8080
        published: "18080"
        protocol: tcp
  frontend:
    build:
--
        host_ip: 127.0.0.1
        target: 8080
        published: "14200"
        protocol: tcp
  mysql:
    environment:
--
        host_ip: 127.0.0.1
        target: 3306
        published: "13306"
        protocol: tcp
    volumes:
      - type: volume
$ docker compose up --build --detach --wait --wait-timeout 180 && ./scripts/smoke-stack.sh; docker compose down --volumes
compose up: all services healthy
e-commerce-frontend-1 80/tcp, 127.0.0.1:14200->8080/tcp
e-commerce-backend-1 127.0.0.1:18080->8080/tcp
e-commerce-mysql-1 33060/tcp, 127.0.0.1:13306->3306/tcp
Backend, frontend, and frontend /api proxy health checks passed.
smoke exit=0
 Volume e-commerce_mysql-data Removed 
 Network e-commerce_default Removed 
$ (cd tools/deploy && npm ci && npx --no-install railway --version && npx --no-install vercel --version)

added 313 packages in 5s
railway 5.52.0

Vercel CLI 59.15.1
59.15.1
exit=0
```

## Supervisor verification of S1–S3 (2026-09-11)

- S1 ☑ `docker compose config` → `host_ip: 127.0.0.1` ×3.
- S3 ☑ no `--token` in workflow; policy rejects it.
- S2 ☐ **REGRESSION — new P0 (F15)**: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci-release.yml'))"` →
  `yaml.parser.ParserError: while parsing a block mapping … line 136, column 9 / expected <block end>, but found '<scalar>' … line 141, column 84`.
  Cause: `run: "$GITHUB_WORKSPACE/…/railway" up --service "$RAILWAY_SERVICE"` (lines 141, 148, 193, 200) — a YAML double-quoted scalar cannot be followed by more text. GitHub would reject the whole workflow (no CI, no deploy). `scripts/test-ci-policy.sh` still passed because it is grep-based and never parses YAML.
  Required: rewrite those steps as block scalars (`run: |`) or unquoted paths; add a real YAML parse gate to `verify-ci-policy.sh` (fail on parse error) with a rejection fixture. Returned to R-INFRA.

### AFTER (F15 — workflow YAML validity)

Captured: 2026-09-11T10:34:37Z

Test-first: added a PyYAML parse gate to `verify-ci-policy.sh` plus a fixture that turns the Railway block scalar back into a quoted `run:` line. Before the workflow fix, the gate rejected the file: `CI policy violation: workflow is not valid YAML (GitHub would reject it): …/.github/workflows/ci-release.yml` (exit 1). Then rewrote the four deploy/redeploy `run:` steps as `run: |` block scalars; the loosened patterns still match, so they are unchanged.

```
$ grep -n -A1 'run: |$' .github/workflows/ci-release.yml | grep -B1 'tools/deploy/node_modules'
141:        run: |
142-          "$GITHUB_WORKSPACE/e-commerce/tools/deploy/node_modules/.bin/railway" up --service "$RAILWAY_SERVICE"
--
149:        run: |
150-          "$GITHUB_WORKSPACE/e-commerce/tools/deploy/node_modules/.bin/vercel" deploy --prod --yes
--
195:        run: |
196-          "$GITHUB_WORKSPACE/e-commerce/tools/deploy/node_modules/.bin/railway" up --service "$RAILWAY_SERVICE"
--
203:        run: |
204-          "$GITHUB_WORKSPACE/e-commerce/tools/deploy/node_modules/.bin/vercel" deploy --prod --yes
$ python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci-release.yml'))" && echo OK
OK
$ bash scripts/test-ci-policy.sh
CI workflow policy verified: CI on pull requests/pushes; one main-only protected release path; one main-only rollback path.
CI policy rejection fixtures passed.
exit=0
```

### Supervisor re-verification of F15 / S2 (2026-09-11)

- ☑ `python3 -c "import yaml; …safe_load(…ci-release.yml)"` → `YAML OK; jobs: ci-policy, backend, frontend, migration, compose-smoke, deploy-production, rollback-production`
- ☑ Deploy/redeploy steps are `run: |` block scalars (lines 141, 149, 195, 203).
- ☑ `bash scripts/test-ci-policy.sh` → `CI workflow policy verified …` / `CI policy rejection fixtures passed.` (exit 0); `verify-ci-policy.sh:17-18` parse gate present.
- Residual risk (accepted, fail-closed): the `ci-policy` job requires PyYAML on `ubuntu-latest`; if absent it fails with `python3 with PyYAML is required to parse the workflow` rather than skipping. Confirm on first GitHub run.

**Security follow-up status**: S1 ☑, S2 ☑ (after F15), S3 ☑, S4 accepted. Stage 4 complete → Stage 5 verify.

## Stage 5 `/verify` — runtime observation (user-invoked, 2026-09-11)

**Verdict: FAIL** — stack, proxy, Liquibase, profile gating, and responsive shell verified at the running surfaces; the skip-to-content behavior required by UI_SPEC §6 fails on real routes. T001 returns to Frontend-Implementer.

Method: cold start (no `verifier-*`/`run-*` skill). Drove the Compose stack (`127.0.0.1:13306/18080/14200`) over HTTP and the nginx-served production Angular build with Playwright Chromium (animations enabled, no reduced motion).

Observed PASS:
- `compose up --wait` exit 0 in 30 s; ports `127.0.0.1:` only; LAN `192.168.0.103:18080` and `:13306` → `000` (unreachable).
- `GET :18080/actuator/health` → `200 {"groups":["liveness","readiness"],"status":"UP"}`; `GET :14200/api/actuator/health` (nginx proxy) → same 200.
- `GET /actuator/env` → 403; `POST /actuator/health` → 403; frontend `/api/actuator/metrics` → 403.
- Local profile: `/v3/api-docs` → 200 (`openapi 3.1.0`, title `E-Commerce API`, `paths []`); `/swagger-ui.html` → 302.
- Liquibase: 20 business tables, `DATABASECHANGELOG` = 1 row (`001-baseline-schema EXECUTED`); after `docker compose restart backend` still 1 row.
- Production profile image with datasource env: `/actuator/health` 200, `/actuator/health/liveness` 200, `/v3/api-docs` 403, `/swagger-ui.html` 403. Without datasource env: startup aborts, `Caused by: java.lang.IllegalArgumentException: 'url' must start with "jdbc"`.
- Shell at 375/768/1280: `/` → `/products`, h1 `Products`, `overflowX:false`, menu button only at 375, primary nav only ≥768, `consoleErrors=0`, `externalRequests=0`. Screenshots: scratchpad `v-shell-{375,768,1280}.png`, `v-drawer-375.png`.
- Drawer opens (`aria-expanded=true`); tapping backdrop closes it (`drawerVisible=false aria-expanded=false`). Hard reload `/login` via nginx → h1 `Log in`. Primary-nav `Log in` at 1280 → `/login`.

Observed FAIL / findings:
- **F16 (P1, FAIL)** Skip link navigates away instead of skipping. On `/login`, Tab → focus `"Skip to content"`, Enter → navigations `["/#main-content","/products#main-content"]`, final h1 `Products` (1280px and 375px). Cause: `<base href="/">` resolves `href="#main-content"` to `/#main-content`, and the root route redirects to `/products`. The Playwright visual test only exercises `/products`, where the redirect lands on the same page, so it passed. Screenshot: `v-skiplink-1280.png`.
- **F17 (P2)** Escape does not close the compact drawer when pressed right after it opens with animations enabled: `B1 open, wait for visible, Escape → drawerVisible=true aria-expanded=true focus="Products"`. The open drawer then covers the Menu button, so it can only be closed via backdrop. `e2e/shell.visual.ts` passes because `reducedMotion: 'reduce'`. Escape after focus has entered the drawer was not exercised (probe aborted).
- **F18 (P2)** Unknown route `/nope` renders an empty shell: url `/`, `h1=(none)`, `main` text empty — no wildcard/not-found route.
- **F19 (P3)** Document title is still the scaffold default `Frontend` on every route.
- Note: real servlet returns **403** (not 404) for disabled/unknown backend paths because the `/error` dispatch is behind `anyRequest().authenticated()`; `ApiDocumentationExposureTests` asserts 404 under MockMvc. Not published either way; response code differs from the test's claim.
- Env: verification probe script hung after an aborted click (browser not closed in error path) — Supervisor killed it and tore the stack down; the other project's containers were not touched.

## 2026-09-14 continuation — package structure and Playwright removal

Status: **PARTIAL / NOT DONE**. This section supersedes the current-status implications of the historical Playwright, visual-regression, responsiveness, and CI-policy evidence above; the earlier output remains as an audit trail.

User-directed removal:

- Removed `@playwright/test`, the `test:visual` package script, Playwright configuration, browser specs, Linux screenshot baselines, generated-result ignore entries, GitHub browser-install/test/artifact steps, and the CI-policy requirement/negative fixture tied to that runner.
- `npm ls @playwright/test playwright playwright-core --all` reports an empty tree, and the corresponding package directories are absent. `package-lock.json` still contains only Vitest's optional `@vitest/browser-playwright` peer metadata; it does not install Playwright.
- The user-level browser cache under `~/.cache/ms-playwright` was inspected but not deleted because it is outside this repository's scope. It can be removed separately after confirming that no other checkout uses it.

Frontend continuation for Stage 5 findings:

- F16: skip-link activation now prevents the anchor's default navigation before focusing `<main>`, with a regression test proving `/login` remains `/login`.
- F17: Escape is handled at the shell container, awaits drawer closure, and restores focus to the native menu button.
- F18: the wildcard route renders `Page not found` without redirecting away from the requested URL.
- F19: products, login, and not-found routes define explicit `E-Commerce` document titles.
- `npm test -- --watch=false` → 1 file, 8 tests passed. `npm run build` → success, 373.78 kB initial bundle. These jsdom/component checks do not replace real-browser behavior or screenshot evidence; fresh browser accessibility, animation, overflow, responsive, and visual checks are **NOT VERIFIED** after Playwright removal.

Broader targeted refresh:

- `./mvnw --batch-mode verify` → `Tests run: 18, Failures: 0, Errors: 0, Skipped: 1`, `BUILD SUCCESS`; the skipped test is the separately enabled real-MySQL Testcontainers lifecycle gate.
- `bash scripts/test-ci-policy.sh` → policy verified and rejection fixtures passed. `docker compose config --quiet` → exit 0. `git diff --check` → exit 0 before this evidence update.
- `docker compose up --build --detach --wait --wait-timeout 180` rebuilt the renamed backend and Playwright-free frontend; MySQL, backend, and frontend all reached healthy. Container-local probes returned backend `{"groups":["liveness","readiness"],"status":"UP"}` and frontend `ok`.
- `./scripts/smoke-stack.sh` could not connect to the published `127.0.0.1:18080` from this managed session (exit 7), even though `docker compose ps/port` showed `127.0.0.1:13306`, `:18080`, and `:14200`. Therefore host-path and nginx `/api` smoke are **ENVIRONMENT-BLOCKED / NOT VERIFIED**, not passed. The scoped stack and `mysql-data` volume were removed successfully with `docker compose down --volumes`.

Remaining gates: targeted Stage 4 review of the namespace/module/UI changes; fresh browser-level evidence using an approved replacement or manual protocol; HTML review reports; GitHub branch protection and production-environment setup; real protected-main release and rollback evidence.
