# TASK_REVIEW — T002: Public Product Catalog Journey

> Sibling of `tasks/TASK_GUIDE_T002.md`. Everything here is **filled by the reviewer at Stage
> 4/5** — it is deliberately NOT in the guide, because the implementing agent re-reads the guide on
> every turn and never fills these two sections.
>
> Consumers resolve each section **guide first, this file second** (`.claude/hooks/lib/guide_sections.py`):
> a legacy guide that still carries these sections inline keeps working unchanged, and a stray
> review file can never override an inline section.

---

## Evidence

| Check | Result | Notes / output snippet |
|-------|--------|------------------------|
| **New test(s) cover Acceptance Criteria (file paths pasted)** | ☑ pass | Written in T002 — backend: `backend/src/test/java/com/training/ecommerce/catalog/domain/CatalogSearchCriteriaTest.java` (10), `…/catalog/adapter/persistence/JdbcCatalogReadRepositoryTest.java` (13, MySQL Testcontainers), `…/catalog/adapter/web/CatalogControllerTest.java` (19), `…/catalog/adapter/web/CatalogOpenApiContractTest.java` (9), `…/catalog/CatalogPerformanceTest.java` (NFR-013 levels; smoke+light in verify, target/stress opt-in) + `CatalogMySqlTestSupport.java`; frontend: `frontend/src/app/features/catalog/catalog.service.spec.ts`, `…/product-list/product-list.component.spec.ts`, `…/product-detail/product-detail.component.spec.ts`, `app.spec.ts` updated. Supervisor run 2026-09-25T10:12Z: `CatalogSearchCriteriaTest 10/0/0`, `CatalogPerformanceTest 5 run/0 fail/2 skip`, `CatalogControllerTest 19/0/0`, `CatalogOpenApiContractTest 9/0/0`, `JdbcCatalogReadRepositoryTest 13/0/0`; frontend `Test Files 4 passed (4) · Tests 28 passed (28)` — pass. |
| Verification command run | ☑ pass | 2026-09-25T10:12:09Z `cd backend && ./mvnw -B verify` → `Tests run: 74, Failures: 0, Errors: 0, Skipped: 3` · `BUILD SUCCESS` (skips: T001 Docker-gated migration lifecycle test + opt-in NFR-013 target/stress; target run separately → p95 92 ms). 10:12:56Z `cd frontend && npx ng test --watch=false` → `28 passed (28)`; `npx ng build` → `Application bundle generation complete`; `bash scripts/verify-ci-policy.sh` → `CI workflow policy verified…`; `bash scripts/test-ci-policy.sh` → `CI policy rejection fixtures passed.` — pass. |
| Negative cases hold | ☑ pass | Real browser, all 3 widths (2026-09-25T10:08:43Z): inactive product (id 21), product in inactive category (id 1), unknown id 999999 → `detail-not-found`; `sort=bogus` → 400 rendered as `catalog-invalid-filter` with `sort: must be one of …`; `q=zzz-no-match` → `catalog-filtered-empty`; forced 500 on list and detail → server-failure state, Retry recovers — pass. Backend negatives covered by `CatalogControllerTest`/`JdbcCatalogReadRepositoryTest`/`CatalogSearchCriteriaTest`. |
| verify | ☑ pass | `/verify` run by user request, 2026-09-25T10:14Z, cold start on the Compose stack (recipe saved as `skills/verifier-stack/SKILL.md`). API through the nginx `/api` proxy: categories, search (`q=HAMMER` case-insensitive), category+price_desc, newest page 1, detail → 200; inactive product 21 / inactive-category product 1 → 404 problem+json; `/products/abc`, `size=51`, `categoryId=abc`, 101-char `q` → 400 with `violations`; `q=%`/`q=_` → 0 results (wildcards escaped); `page=99` → empty content, real totals; `POST` → 403. UI in real Chromium: 42/42 probe checks at 375/768/1280; double-click Next → 1 request, page 2 of 2; browser Back restores page 1; typing sends no request, Enter applies; Back undoes a search. Feature confirmed working — pass. Notes: `page=-1&categoryId=abc` reports only the type-mismatch violation (P3); non-GET 403 has an empty body, not problem+json (T003 auth scope). |
| Review scope bounded to the change's blast radius (affected set, not whole repo) | ☑ pass | code-review + security-review covered the uncommitted T002 diff only: catalog backend module, `configuration/{ProblemDetailsExceptionHandler,ProblemResponse,FieldViolation,SecurityConfiguration}`, `frontend/src/app/features/catalog/**`, `app.{config,routes,spec}.ts`, plus direct config (`application*.properties`, `nginx.conf`, `vercel.json`, smoke scripts). Other modules untouched, skipped. See Stage 4 sections below. |
| Full smoke suite still green (no regression) | ☑ pass | Fresh Compose stack (`docker compose up -d --build --wait`, all 3 healthy) → `bash scripts/smoke-stack.sh` → `Backend, frontend, and frontend /api proxy (catalog) health checks passed.` (2026-09-25T10:08Z, after the proxy fix now exercises `/api/catalog/categories` through nginx) — pass. |
| **UI: Visual regression (diff or verdict pasted)** | ☑ pass | Real Chromium (Playwright 1.61.1, easy-ui-mcp container, method as T001) against the Compose production build at :14200 with seeded sample data. 21 screenshots: list, filtered-empty, invalid-filter, server-failure, detail, detail-long-name, detail-not-found × 375/768/1280 in `reports/T002-browser-evidence-20260925/` (local; `reports/` gitignored). Supervisor verdict: matches UI_SPEC §7 wireframes (filters row + Apply filters, card = placeholder/name/price+ISO/View details, `Previous · Page N of M · Next`; detail breadcrumb `Products / name`, placeholder, name, price, description). First pass FAILED and was fixed before this verdict: broken-image icon from `<img src="">` and browser-default breadcrumb link (Visual-fix round). Caveat: one-off evidence, not an automated baseline (in-repo Playwright removed by user, see T001); placeholder `--color-surface-muted` barely contrasts with the page background (P3, decorative). — pass |
| **UI: Design-system compliance (tokens/colors/typography verified)** | ☑ pass | In-browser DOM audit at each width: every button/link-button is Material (`mdc-button`), 0 bare inputs outside `mat-form-field`, 0 inline styles, tokens `--color-error #b3261e`, `--color-surface-muted #f5f7fa`, `--color-text-muted #4b5563` resolve; breadcrumb link colour == computed `var(--color-primary)`; body font from theme. Static: no hex/rgb literals in `features/catalog/**/*.css` (grep empty); all three tokens defined in `styles.scss`. — pass |
| **UI: Responsiveness at target viewports** | ☑ pass | 375/768/1280: grid columns 1/2/4 (UI_SPEC §3.4: reduced columns <600, multi-column ≥960), `overflowX:false` and no element past the viewport edge on list and detail (incl. 60-char product name), filters wrap, detail placeholder `max-width:280px; width:100%`. 42/42 probe checks PASS — output in Demonstration → UI evidence. — pass |

---

## Stage 4 — findings from real-browser UI evidence (2026-09-25, Supervisor)

| Sev | File | Finding | Status |
|---|---|---|---|
| P1 | `frontend/nginx.conf:15`, `frontend/vercel.json` | Same-origin `/api` proxies stripped the prefix; backend serves `/api/catalog/**` → proxied calls got 403, catalog UI could not load in the real stack. HTTP-mocked unit tests could not see it. | Fixed (common-infrastructure): proxies pass `/api` through; smoke scripts check `/api/catalog/categories`; user decision in `memory/decisions.md`. |
| P2 | `product-list.component.html:76`, `product-detail.component.html:31` | `<img src="">` rendered a broken-image icon on every card/detail (was P3 in code-review; promoted on visual evidence). | Fixed (frontend-developer): decorative `<div class="thumbnail" aria-hidden="true">`. |
| P2 | `product-detail.component.html` breadcrumb | Link used browser-default blue/underline, not tokens. | Fixed: `.breadcrumb-link { color: var(--color-primary) }`, global `:focus-visible` applies. |
| P3 | `product-*.component.css` `.thumbnail` | `--color-surface-muted` placeholder barely contrasts with page background. | Open (optional, decorative). |

## Demonstration

> Anchors what this task delivered to an observable before/after pair. BEFORE has no `N/A` path:
> if the task changes executable code, BEFORE is a pasted, timestamped terminal capture taken
> **before any implementation commit exists**; if it does not (docs, templates, skill-instruction
> text), BEFORE is the **verbatim prior content** of what changed — a quoted excerpt, not a command.

**BEFORE** (backend half; captured by backend-developer agent in worktree
`.claude/worktrees/agent-a18f1fdbaa65b37f6` at HEAD `0c3d411`, clean working tree, before any T002 code change):

```text
$ date -u; git rev-parse --short HEAD; git status --short
2026-09-25T07:21:46Z
0c3d411
(no output — clean tree)

$ find backend/src -path '*catalog*'
backend/src/main/java/com/training/ecommerce/catalog
backend/src/main/java/com/training/ecommerce/catalog/package-info.java

$ grep -rn 'api/catalog' backend/src frontend/src || echo 'no matches for api/catalog'
no matches for api/catalog

$ date -u; (cd backend && ./mvnw -B test -Dtest='*Catalog*Test,*Catalog*IT' -Dsurefire.failIfNoSpecifiedTests=false) | tail; date -u
2026-09-25T07:21:50Z
[INFO] --- compiler:3.15.0:testCompile (default-testCompile) @ ecommerce ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 9 source files with javac [debug parameters release 21] to target/test-classes
[INFO]
[INFO] --- surefire:3.5.6:test (default-test) @ ecommerce ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.714 s
[INFO] Finished at: 2026-09-25T14:21:54+07:00
2026-09-25T07:21:54Z
```

Surefire printed no `Tests run:` line: zero catalog tests exist. The catalog module holds only its
`package-info.java` module marker, and no `/api/catalog` route exists anywhere.

**BEFORE** (frontend half):

Command (run 2026-09-25T07:21:44Z, before any T002 implementation change, frontend half):

```
$ cd frontend && npm test -- --watch=false --include='src/app/features/catalog/**/*.spec.ts'
> frontend@0.0.0 test
> ng test --watch=false --include=src/app/features/catalog/**/*.spec.ts

An exception occurred while getting runner-specific build options:
Error: No tests found matching the following patterns:
- Included: src/app/features/catalog/**/*.spec.ts

Please check the 'test' target configuration in your project's 'angular.json' file.
    at getVitestBuildOptions (.../node_modules/@angular/build/src/builders/unit-test/runners/vitest/build-options.js:187:15)
    at async execute (.../node_modules/@angular/build/src/builders/unit-test/builder.js:305:13)
    at async handleAsyncIterator (.../node_modules/@angular-devkit/architect/src/api.js:38:28)
```

`frontend/src/app/app.routes.ts` contents at this point (verbatim):

```ts
import { Component } from '@angular/core';
import { RouterLink, Routes } from '@angular/router';

@Component({
  selector: 'app-products-placeholder',
  template: `
    <section aria-labelledby="products-title">
      <h1 id="products-title">Products</h1>
      <p>The product catalog will be available in the next delivery slice.</p>
    </section>
  `,
})
export class ProductsPlaceholder {}

@Component({
  selector: 'app-login-placeholder',
  template: `
    <section aria-labelledby="login-title">
      <h1 id="login-title">Log in</h1>
      <p>Authentication will be available in a later delivery slice.</p>
    </section>
  `,
})
export class LoginPlaceholder {}

@Component({
  selector: 'app-not-found-placeholder',
  template: `
    <section aria-labelledby="not-found-title">
      <h1 id="not-found-title">Page not found</h1>
      <p>The requested page does not exist.</p>
      <a routerLink="/products">Return to products</a>
    </section>
  `,
  imports: [RouterLink],
})
export class NotFoundPlaceholder {}

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  { path: 'products', component: ProductsPlaceholder, title: 'Products | E-Commerce' },
  { path: 'login', component: LoginPlaceholder, title: 'Log in | E-Commerce' },
  { path: '**', component: NotFoundPlaceholder, title: 'Page not found | E-Commerce' },
];
```

No `src/app/features/catalog/` directory exists.

**AFTER** (backend NFR-013, Supervisor re-run 2026-09-25T08:45Z, load avg 7.6 on 8 cores):

```
$ ./mvnw -B -q test -Dtest=CatalogPerformanceTest -Dcatalog.load.level=target
NFR-013 [target: 50 users, think time 1000 ms] 10000 products, 100 categories, requests=1200, failures=0
NFR-013 [target: 50 users, think time 1000 ms] list-deep-page n= 200 p50=  36 ms p95=  92 ms max= 439 ms
NFR-013 [target: 50 users, think time 1000 ms] list-default   n= 200 p50=  31 ms p95=  88 ms max= 440 ms
NFR-013 [target: 50 users, think time 1000 ms] list-search    n= 200 p50=  46 ms p95= 123 ms max= 433 ms
NFR-013 [target: 50 users, think time 1000 ms] all            n=1200 p50=  22 ms p95=  92 ms max= 440 ms
exit 0
```

Load levels per `memory/decisions.md` 2026-09-25: smoke/light in `verify`; target opt-in (above); stress report-only (p95 ~670 ms, zero failures). Backend `./mvnw verify`: 74 run, 0 failures, 3 skipped (T001 gated test + opt-in target/stress).

**AFTER** (frontend):

Command (run 2026-09-25T08:04:55Z, frontend half, after implementation):

```
$ cd frontend && npm test -- --watch=false --include='src/app/features/catalog/**/*.spec.ts'
> frontend@0.0.0 test
> ng test --watch=false --include=src/app/features/catalog/**/*.spec.ts

 Test Files  3 passed (3)
      Tests  16 passed (16)
```

Full suite (`npm test -- --watch=false`): 4 test files, 25 tests passed, no unhandled errors.
`npm run build`: succeeds, emits lazy chunks `product-list-component` (180.00 kB raw / 33.83 kB
transfer) and `product-detail-component` (3.37 kB raw / 1.37 kB transfer) under
`frontend/dist/frontend`.

`frontend/src/app/app.routes.ts` now lazy-loads `./features/catalog/catalog.routes` for the
`products` path (list at `/products`, detail at `/products/:id`); `ProductsPlaceholder` removed.

**DELTA** (whole task, Supervisor, 2026-09-25): BEFORE — no catalog module, no `/api/catalog/**`
endpoints, `/products` was a static placeholder, no detail route, and the `/api` proxy stripped the
prefix. AFTER — public read API `GET /api/catalog/products` (search, category, 5 sorts, 0-based
pages ≤ 50), `/products/{id}` and `/categories`, showing only active products in active categories,
RFC 7807 errors with `violations`; Angular `/products` list + `/products/:id` detail with
URL-driven filters and all loading/empty/invalid/failure/not-found states; nginx/Vercel proxies pass
`/api` through. Observed: backend verify 74/0/3, frontend 28/28, NFR-013 target p95 92 ms,
real-browser 42/42 at 375/768/1280, `/verify` PASS through the proxy.

**WITNESS**: frontend-developer agent (T002), 2026-09-25, via `npm test` / `npm run build` output
above; independent reviewer evidence recorded at Stage 4/5 (Evidence table, 9/9).

### Proxy-fix round BEFORE

Captured against the running Compose stack (all services healthy) before any T002 proxy-fix change:

```text
$ date -u
2026-09-25T10:03:16Z
$ curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:14200/api/catalog/categories
403
$ curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/catalog/categories
200
$ bash scripts/smoke-stack.sh
Backend, frontend, and frontend /api proxy health checks passed.
```

Confirms the reported defect: through the frontend nginx `/api/` proxy, `GET /api/catalog/categories`
returns `403` (arrives at backend as `/catalog/categories`, which Spring Security does not
recognize as the public catalog route) while the same request direct to the backend on `:18080`
returns `200`. `scripts/smoke-stack.sh` currently only checks `…/api/actuator/health` through the
proxy, which still works under the old stripping behavior, so it does not catch this defect.

### Proxy-fix round AFTER

Captured after rebuilding the frontend image (`docker compose up -d --build --wait frontend`) with
`frontend/nginx.conf` passing the URI through unchanged and `frontend/vercel.json` rewriting to
`.../api/:path*`, and after updating `scripts/smoke-stack.sh`, `scripts/smoke-production.sh`, and
`scripts/verify-ci-policy.sh` to check `/api/catalog/categories` instead of `/api/actuator/health`:

```text
$ date -u
2026-09-25T10:04:31Z
$ curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:14200/api/catalog/categories
200
$ curl -s http://127.0.0.1:14200/api/catalog/categories
[{"id":2,"name":"Kitchen","slug":"kitchen"},{"id":1,"name":"Tools","slug":"tools"}]
$ curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/catalog/categories
200
$ curl -s 'http://127.0.0.1:14200/api/catalog/products?size=1'
{"content":[{"id":32,"name":"Heavy-duty 20oz fiberglass claw hammer with shock-absorbing grip","slug":"long-name-hammer","price":24.99,"currency":"USD","category":{"id":1,"name":"Tools"}}],"page":0,"size":1,"totalElements":17,"totalPages":17}
$ bash scripts/smoke-stack.sh
Backend, frontend, and frontend /api proxy (catalog) health checks passed.
$ bash scripts/verify-ci-policy.sh
CI workflow policy verified: CI on pull requests/pushes; one main-only protected release path; one main-only rollback path.
$ bash scripts/test-ci-policy.sh
CI workflow policy verified: CI on pull requests/pushes; one main-only protected release path; one main-only rollback path.
CI policy rejection fixtures passed.
```

Through-proxy `GET /api/catalog/categories` now returns `200` with the proxied JSON array
(matching the direct-to-backend `:18080` response), and `GET /api/catalog/products?size=1` through
the proxy returns real seeded catalog data. Both CI policy checks and the full stack smoke test
remain green. Stack left running (not stopped, MySQL volume not wiped) as instructed.

## Stage 4 — code-review (2026-09-25, Supervisor)

Scope: uncommitted T002 diff (catalog backend module, `configuration/` problem-details + security change, `frontend/src/app/features/catalog/**`, `app.config.ts`, `app.routes.ts`, `app.spec.ts`). Entry point `GET /api/catalog/products` found in `CatalogController` — reachable. Reviewers: correctness, testing, maintainability, standards; conditional security (public endpoints, SQL), performance (DB queries), api (OpenAPI). Result: P0 0 · P1 2 · P2 1 · P3 4. No fixes applied by the Supervisor (Hard-Stop Gate 1) — P1s go back to frontend-developer.

| Sev | File:line | Finding | Conf. | Action |
|---|---|---|---|---|
| P1 | `frontend/.../product-detail/product-detail.component.ts:34` | `retry()` reloads `product()?.id`, which is `null` when the **first** load fails → the Retry button on the server-failure state does nothing. Spec never clicks Retry. | 100 | Keep the route id in a field; retry with it; add a spec that clicks Retry after a 500 and expects a second request. |
| P1 | `frontend/.../product-list/product-list.component.ts:118,127,136` | `applyFilters`/paging call `navigateToCurrentQuery()` **and** `loadProducts()`; the navigation re-emits `queryParamMap`, whose subscriber calls `loadProducts()` again → 2 requests per action, and with no cancellation an older response can land last (stale page vs `currentPage`). Hidden in specs because `ActivatedRoute` is a static `of(...)`. | 75 | Load only from the `queryParamMap` subscription (use `switchMap` to cancel superseded requests); make actions only navigate; spec with the real router asserting one request per action. |
| P2 | `frontend/.../product-list/product-list.component.html:47` | `track violation.field` — two violations on one field give duplicate track keys. | 75 | `track $index`. |
| P3 | `backend/.../catalog/adapter/web/CatalogController.java:55` | `@Size(max=100)` checks raw `q` before the domain trims it; padded 100-char search → 400. | 75 | Accept, or drop `@Size` and let the domain error map to 400. |
| P3 | `backend/.../configuration/ProblemDetailsExceptionHandler.java:50` | Every type mismatch says "must be a whole number"; fine for today's numeric params only. | 75 | Revisit when a non-numeric typed param appears. |
| P3 | `backend/.../configuration/ProblemDetailsExceptionHandler.java:53` | Catch-all `Exception` handler will turn future method-security `AccessDeniedException` into 500 (already in learnings for T003). | 75 | Map auth exceptions before the catch-all in T003. |
| P3 | `frontend/.../product-list/product-list.component.html:76`, `product-detail.component.html:31` | `<img src="" alt="">` placeholder triggers an error event per card. | 75 | Use a CSS placeholder block until images exist. |

Checked, no finding: SQL uses bound params only and a closed-enum ORDER BY; LIKE wildcards escaped; public = product AND category active on list + detail; `GET /api/catalog/**` is the only new permitAll; 500 body hides exception text; page beyond end skips the row query.

### Review-fix round BEFORE

Captured 2026-09-25T08:53:00Z, in the main checkout, before any review-fix code change (working tree
otherwise unchanged since Stage 4 review above):

```text
$ date -u && npx ng test --watch=false
Fri Sep 25 08:53:00 AM UTC 2026

❯ Building...
✔ Building...
Application bundle generation complete. [2.980 seconds] - 2026-09-25T08:53:04.665Z

 RUN  v4.1.11 /home/hungnguyenhuu/workspace/training/me/java/java-training/e-commerce/frontend

 Test Files  4 passed (4)
      Tests  25 passed (25)
   Start at  15:53:04
   Duration  3.65s (transform 426ms, setup 1.95s, import 1.89s, tests 2.73s, environment 4.02s)

Fri Sep 25 08:53:08 AM UTC 2026
```

All 25 pre-existing tests pass but do not cover: Retry after a first-load failure in
`product-detail.component.spec.ts` (P1 #1), or duplicate-request/cancellation behavior on
`applyFilters`/pagination in `product-list.component.spec.ts` (P1 #2, hidden because
`ActivatedRoute` is a static `of(...)`).

### Review-fix round AFTER

Changes (all inside `frontend/src/app/features/catalog/**`, per the guide's scope lock):

- `product-detail/product-detail.component.ts` — `retry()` now uses a stored `currentId` (set from
  the route id on every `paramMap` emission) instead of `product()?.id`, so it works when the first
  load failed and `product()` is still `null`.
- `product-detail/product-detail.component.spec.ts` — new spec: 500 on first load → click Retry →
  expect a second `GET /api/catalog/products/5` → flush success → ready state with product content.
- `product-list/product-list.component.ts` — `queryParamMap` is now the single source of loads via
  `switchMap` (cancels a superseded in-flight request); `listProducts` errors are handled with
  `catchError` inside that pipe. `applyFilters`/`goToPreviousPage`/`goToNextPage` now only navigate
  (`navigateToCurrentQuery()`); the resulting `queryParamMap` re-emission is what triggers the load.
  `retry()` now calls the extracted `fetchProducts()` directly (bypassing navigation), as permitted
  by the finding.
- `product-list/product-list.component.spec.ts` — added `setupNavigable()`, a controllable
  `ActivatedRoute.queryParamMap` (`BehaviorSubject`) plus a fake `Router.navigate` that merges
  requested query params back into that subject, closing the loop the way real navigation does.
  Rewrote the "applies filters" test on top of it (asserts navigate args, no longer needs a
  `navigateSpy`). Added two new specs: one asserting exactly one `/api/catalog/products` request per
  Apply-filters action and per Next-page action (`httpMock.match(...).length === 1`), and one
  asserting a first in-flight request is `cancelled` when a second queryParamMap emission (filter
  change) arrives before it resolves, and that the second response lands correctly (`state()` →
  `ready`).
- `product-list/product-list.component.html:47` — `track violation.field` → `track $index`.

New/changed test files:
- `frontend/src/app/features/catalog/product-detail/product-detail.component.spec.ts`
- `frontend/src/app/features/catalog/product-list/product-list.component.spec.ts`

Command (run 2026-09-25T08:54:58Z, catalog-scoped):

```text
$ npx ng test --watch=false --include='src/app/features/catalog/**/*.spec.ts'
 Test Files  3 passed (3)
      Tests  19 passed (19)
```

Full suite (run 2026-09-25T08:55:08Z):

```text
$ date -u && npx ng test --watch=false
Fri Sep 25 08:55:05 AM UTC 2026
 Test Files  4 passed (4)
      Tests  28 passed (28)
   Duration  3.07s (transform 288ms, setup 1.73s, import 1.47s, tests 2.48s, environment 3.01s)
```

Build (run immediately after, same session):

```text
$ npx ng build
Initial total            | 490.16 kB |               114.30 kB
Lazy chunk files:
  product-list-component   | 180.07 kB | 33.84 kB
  product-detail-component |   3.40 kB |  1.38 kB
Application bundle generation complete. [3.411 seconds] - 2026-09-25T08:55:15.576Z
Fri Sep 25 08:55:15 AM UTC 2026
```

25 → 28 tests (3 new: 1 retry-after-failure, 1 single-request-per-action, 1 cancel-superseded-request),
all green; build unaffected (chunk sizes essentially unchanged). Only the three flagged findings
(P1 ×2, P2 ×1) were addressed; P3 items were left untouched per instructions. No regressions
observed in the unrelated existing specs (loading/empty/filtered-empty/server-failure/invalid-filter/
page-label all still pass unmodified).

## Stage 4 — security-review (2026-09-25, Supervisor, Medium risk)

Built-in `security-review` could not run (`origin/HEAD` unset; T002 uncommitted), so the same review was done manually over the full T002 diff + `application*.properties` + `SecurityConfiguration`. **Verdict: no vulnerabilities.**
- SQL injection: all user input bound; ORDER BY from closed enum; LIKE wildcards escaped (`ESCAPE '!'`).
- AuthZ: only `GET /api/catalog/**` newly `permitAll`; everything else still authenticated; actuator health-only, details hidden.
- Data exposure: inactive product / inactive category → excluded from list, 404 on detail (indistinguishable from unknown id).
- Errors: 500 body fixed text; exception logged server-side only.
- Abuse bounds: size ≤ 50, q ≤ 100, page beyond end skips row query; unanchored LIKE measured under NFR-013 (target p95 92 ms). Rate limiting out of scope.
- Frontend: interpolation only (no `innerHTML`/`bypassSecurityTrust*`), no token/storage use.

Reports: `reports/code-review_develop_20260925T155300.html`, `reports/security-review_develop_20260925T155312.html`.

Review-fix round verified by Supervisor: P1 ×2 + P2 fixed by frontend-developer; independent re-run `npx ng test --watch=false` → 4 files, 28 tests passed. P3 ×4 left open (optional).

### Visual-fix round BEFORE

Captured 2026-09-25T10:07Z, in the main checkout, before any visual-fix code change. Verbatim
current lines:

`product-list/product-list.component.html` (card thumbnail):

```html
              <img class="thumbnail" src="" alt="" />
```

`product-detail/product-detail.component.html` (detail thumbnail):

```html
          <img class="thumbnail" src="" alt="" />
```

`product-detail/product-detail.component.html` (breadcrumb `<nav>` block):

```html
        <nav aria-label="Breadcrumb">
          <a routerLink="/products">Products</a>
          <span> / </span>
          <span>{{ product.name }}</span>
        </nav>
```

`.thumbnail` CSS — `product-list/product-list.component.css`:

```css
.thumbnail {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  background: var(--color-surface-muted);
}
```

`.thumbnail` CSS — `product-detail/product-detail.component.css`:

```css
.thumbnail {
  width: 280px;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  background: var(--color-surface-muted);
}
```

No breadcrumb-link style rule exists in either `.css` file today (the `<a>` renders with the
browser default blue/underline). Design tokens available in `frontend/src/styles.scss`:
`--color-primary: #1565c0`, `--color-focus: #005fcc`.

### Visual-fix round AFTER

Changes (all inside `frontend/src/app/features/catalog/**`):

- `product-list/product-list.component.html` — card thumbnail `<img class="thumbnail" src="" alt="" />`
  replaced with `<div class="thumbnail" aria-hidden="true"></div>`.
- `product-detail/product-detail.component.html` — detail thumbnail replaced the same way; breadcrumb
  `<nav>` gained a `class="breadcrumb"` and the `<a>` gained `class="breadcrumb-link"`.
- `product-list/product-list.component.css` — `.thumbnail` dropped `object-fit: cover` (no longer an
  `<img>`), kept `width: 100%`, `aspect-ratio: 4/3`, and `background: var(--color-surface-muted)`.
- `product-detail/product-detail.component.css` — `.thumbnail` now `width: 100%; max-width: 280px`
  (was a fixed `280px`, could overflow narrow viewports) plus the same aspect-ratio/background; added
  `.breadcrumb-link { color: var(--color-primary); text-decoration: none; }` with
  `.breadcrumb-link:hover { text-decoration: underline; }`. No hard-coded colours — reuses the
  existing `--color-primary` token from `styles.scss`; focus affordance is already provided globally
  by the `:focus-visible { outline: 3px solid var(--color-focus); }` rule in `styles.scss`, which
  applies to this link like every other interactive element in the shell.
- `product-list/product-list.component.spec.ts` and `product-detail/product-detail.component.spec.ts`
  — the two specs that previously asserted `img.getAttribute('alt') === ''` now assert no `<img>` is
  present and that `.thumbnail` is a `DIV` with `aria-hidden="true"`.

Full suite (run 2026-09-25T10:07:39Z):

```text
$ date -u && npx ng test --watch=false
Fri Sep 25 10:07:39 AM UTC 2026
 Test Files  4 passed (4)
      Tests  28 passed (28)
   Duration  4.64s (transform 630ms, setup 2.26s, import 3.07s, tests 4.24s, environment 3.54s)
```

Build (run 2026-09-25T10:07:50Z):

```text
$ date -u && npx ng build
Fri Sep 25 10:07:50 AM UTC 2026
Initial total            | 490.16 kB |               114.19 kB
Lazy chunk files:
  product-list-component   | 180.06 kB | 33.85 kB
  product-detail-component |   3.59 kB |  1.41 kB
Application bundle generation complete. [3.969 seconds] - 2026-09-25T10:07:55.252Z
Fri Sep 25 10:07:55 AM UTC 2026
```

28/28 tests still pass (same count as before this round — two existing specs updated in place, no
new spec added, per the instruction "add nothing else unless needed"); build succeeds, chunk sizes
essentially unchanged.

### UI evidence — real browser (Supervisor, 2026-09-25T10:08:43Z)

Stack: `docker compose up -d --build --wait` (frontend rebuilt after proxy + visual fixes), seeded local sample data (Tools/Kitchen active, Archived inactive; item 7 of each category inactive; one 60-char name) — 17 public products. Script: `reports/T002-browser-evidence-20260925/t002-browser.mjs`, run with `docker exec f5185617cfa8 node t002-browser.mjs` → exit 0.

```
PASS 375: list layout (h1, 12 cards, grid, pagination, no overflow) {"path":"/products","title":"Products | E-Commerce","h1":"Products","cards":12,"columns":1,"pageLabel":"Page 1 of 2","testIds":["compact-navigation","brand","apply-filters"],"overflowX":false,"overflowing":[],"decorativeImages":true}
PASS 375: design-system audit (Material controls, tokens, no inline styles) {"buttons":15,"nonMaterialButtons":[],"inlineStyles":[],"bareInputs":0,"bodyFont":"system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif","tokens":[["--color-error","#b3261e"],["--color-surface-muted","#f5f7fa"],["--color-text-muted","#4b5563"]]}
PASS 375: filters apply only on click, URL round-trips, one request {"pathBeforeApply":"","path":"/products?q=kitchen%20item&sort=name_asc&page=0","cards":8,"requests":1}
PASS 375: Next page {"path":"/products?sort=name_asc&page=1","pageLabel":"Page 2 of 2","cards":5,"requests":1}
PASS 375: filtered-empty state + deep link restores search {"testIds":["compact-navigation","brand","apply-filters","catalog-filtered-empty"],"searchValue":"zzz-no-match"}
PASS 375: invalid-filter state (400 violations rendered) {"testIds":["compact-navigation","brand","apply-filters","catalog-invalid-filter"],"invalidText":"Invalid filtersThe request contains invalid parameters.sort: must be one of name_asc, name_desc, price_asc, price_desc, newest"}
PASS 375: list server-failure + Retry recovers {"failed":true,"cardsAfterRetry":12}
PASS 375: detail page (name, breadcrumb, price, no overflow) {"path":"/products/32","title":"Product | E-Commerce","h1":"Heavy-duty 20oz fiberglass claw hammer with shock-absorbing grip","cards":0,"columns":0,"testIds":["compact-navigation","brand"],"overflowX":false,"overflowing":[],"decorativeImages":true,"breadcrumb":"Products / Heavy-duty 20oz fiberglass claw hammer with shock-absorbing grip","price":"$24.99","description":null,"breadcrumbColor":{"link":"rgb(21, 101, 192)","primary":"rgb(21, 101, 192)"}}
PASS 375: detail design-system audit {"buttons":0,"nonMaterialButtons":[],"inlineStyles":[],"bareInputs":0,"bodyFont":"system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif","tokens":[["--color-error","#b3261e"],["--color-surface-muted","#f5f7fa"],["--color-text-muted","#4b5563"]]}
PASS 375: detail not-found for inactive product (id 21) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 375: detail not-found for inactive category (id 1) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 375: detail not-found for unknown (id 999999) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 375: detail server-failure + Retry recovers {"detailFailed":true}
PASS 375: no unexpected console errors / external requests {"unexpectedConsole":[],"externalRequests":[]}
PASS 768: list layout (h1, 12 cards, grid, pagination, no overflow) {"path":"/products","title":"Products | E-Commerce","h1":"Products","cards":12,"columns":2,"pageLabel":"Page 1 of 2","testIds":["compact-navigation","brand","apply-filters"],"overflowX":false,"overflowing":[],"decorativeImages":true}
PASS 768: design-system audit (Material controls, tokens, no inline styles) {"buttons":15,"nonMaterialButtons":[],"inlineStyles":[],"bareInputs":0,"bodyFont":"system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif","tokens":[["--color-error","#b3261e"],["--color-surface-muted","#f5f7fa"],["--color-text-muted","#4b5563"]]}
PASS 768: filters apply only on click, URL round-trips, one request {"pathBeforeApply":"","path":"/products?q=kitchen%20item&sort=name_asc&page=0","cards":8,"requests":1}
PASS 768: Next page {"path":"/products?sort=name_asc&page=1","pageLabel":"Page 2 of 2","cards":5,"requests":1}
PASS 768: filtered-empty state + deep link restores search {"testIds":["compact-navigation","brand","apply-filters","catalog-filtered-empty"],"searchValue":"zzz-no-match"}
PASS 768: invalid-filter state (400 violations rendered) {"testIds":["compact-navigation","brand","apply-filters","catalog-invalid-filter"],"invalidText":"Invalid filtersThe request contains invalid parameters.sort: must be one of name_asc, name_desc, price_asc, price_desc, newest"}
PASS 768: list server-failure + Retry recovers {"failed":true,"cardsAfterRetry":12}
PASS 768: detail page (name, breadcrumb, price, no overflow) {"path":"/products/32","title":"Product | E-Commerce","h1":"Heavy-duty 20oz fiberglass claw hammer with shock-absorbing grip","cards":0,"columns":0,"testIds":["compact-navigation","brand"],"overflowX":false,"overflowing":[],"decorativeImages":true,"breadcrumb":"Products / Heavy-duty 20oz fiberglass claw hammer with shock-absorbing grip","price":"$24.99","description":null,"breadcrumbColor":{"link":"rgb(21, 101, 192)","primary":"rgb(21, 101, 192)"}}
PASS 768: detail design-system audit {"buttons":0,"nonMaterialButtons":[],"inlineStyles":[],"bareInputs":0,"bodyFont":"system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif","tokens":[["--color-error","#b3261e"],["--color-surface-muted","#f5f7fa"],["--color-text-muted","#4b5563"]]}
PASS 768: detail not-found for inactive product (id 21) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 768: detail not-found for inactive category (id 1) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 768: detail not-found for unknown (id 999999) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 768: detail server-failure + Retry recovers {"detailFailed":true}
PASS 768: no unexpected console errors / external requests {"unexpectedConsole":[],"externalRequests":[]}
PASS 1280: list layout (h1, 12 cards, grid, pagination, no overflow) {"path":"/products","title":"Products | E-Commerce","h1":"Products","cards":12,"columns":4,"pageLabel":"Page 1 of 2","testIds":["compact-navigation","brand","apply-filters"],"overflowX":false,"overflowing":[],"decorativeImages":true}
PASS 1280: design-system audit (Material controls, tokens, no inline styles) {"buttons":15,"nonMaterialButtons":[],"inlineStyles":[],"bareInputs":0,"bodyFont":"system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif","tokens":[["--color-error","#b3261e"],["--color-surface-muted","#f5f7fa"],["--color-text-muted","#4b5563"]]}
PASS 1280: filters apply only on click, URL round-trips, one request {"pathBeforeApply":"","path":"/products?q=kitchen%20item&sort=name_asc&page=0","cards":8,"requests":1}
PASS 1280: Next page {"path":"/products?sort=name_asc&page=1","pageLabel":"Page 2 of 2","cards":5,"requests":1}
PASS 1280: filtered-empty state + deep link restores search {"testIds":["compact-navigation","brand","apply-filters","catalog-filtered-empty"],"searchValue":"zzz-no-match"}
PASS 1280: invalid-filter state (400 violations rendered) {"testIds":["compact-navigation","brand","apply-filters","catalog-invalid-filter"],"invalidText":"Invalid filtersThe request contains invalid parameters.sort: must be one of name_asc, name_desc, price_asc, price_desc, newest"}
PASS 1280: list server-failure + Retry recovers {"failed":true,"cardsAfterRetry":12}
PASS 1280: detail page (name, breadcrumb, price, no overflow) {"path":"/products/32","title":"Product | E-Commerce","h1":"Heavy-duty 20oz fiberglass claw hammer with shock-absorbing grip","cards":0,"columns":0,"testIds":["compact-navigation","brand"],"overflowX":false,"overflowing":[],"decorativeImages":true,"breadcrumb":"Products / Heavy-duty 20oz fiberglass claw hammer with shock-absorbing grip","price":"$24.99","description":null,"breadcrumbColor":{"link":"rgb(21, 101, 192)","primary":"rgb(21, 101, 192)"}}
PASS 1280: detail design-system audit {"buttons":0,"nonMaterialButtons":[],"inlineStyles":[],"bareInputs":0,"bodyFont":"system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif","tokens":[["--color-error","#b3261e"],["--color-surface-muted","#f5f7fa"],["--color-text-muted","#4b5563"]]}
PASS 1280: detail not-found for inactive product (id 21) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 1280: detail not-found for inactive category (id 1) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 1280: detail not-found for unknown (id 999999) {"testIds":["compact-navigation","brand","detail-not-found"]}
PASS 1280: detail server-failure + Retry recovers {"detailFailed":true}
PASS 1280: no unexpected console errors / external requests {"unexpectedConsole":[],"externalRequests":[]}
```
