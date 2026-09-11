# UI_SPEC — E-Commerce

**Status**: Approved  
**Last updated**: 2026-09-11  
**Owner**: Project Supervisor  
**Applies to**: T001, T002, T003, T004, T005, T006, T008, T011, T012, and T015

> This is the canonical visual and interaction contract for the Angular application. `PRD.md`
> defines product behavior, `PROJECT_SPEC.md` defines technical boundaries, backend/OpenAPI
> contracts define available data and operations, and this document defines how approved behavior
> is presented. A frontend agent must stop rather than inventing fields, permissions, transitions,
> or screens that are absent from those sources.

---

## 1. Product UI Direction

The application uses **Path A — Lean Material UI**:

- Angular Material is the only external UI component library.
- Angular CDK and Angular animation packages required by the compatible Material release are
  permitted dependencies; they do not authorize a second visual component system.
- The application has one light theme and one responsive shell shared by all roles.
- Desktop is the primary experience; mobile is supported as a compatibility target.
- Visual design stays restrained so backend behavior, database design, security, and delivery
  evidence remain the portfolio focus.
- ASCII wireframes define hierarchy and behavior, not pixel-perfect artwork.

### Deliberate exclusions

- No Web Awesome, Bootstrap, Tailwind, PrimeNG, or second component library.
- No dark theme, theme switcher, gradient decoration, elaborate animation, or branded illustration.
- No Storybook or custom general-purpose design-system package.
- No product-image storage, URL field, uploader, transformation, or CDN integration.
- No mobile-only components or workflows.
- No live-search debounce/cancellation system.
- No guest cart replay or automatic add-to-cart after authentication.

---

## 2. Authority and Contract Rules

When sources differ, use this order:

1. `PRD.md` — approved user-visible behavior and scope.
2. Backend implementation plus generated OpenAPI — fields, validation, authorization, status,
   pagination, errors, and available operations.
3. `PROJECT_SPEC.md` — security, architecture, and integration boundaries.
4. This `UI_SPEC.md` — layout, interaction, presentation, and responsive behavior.
5. The current `tasks/TASK_GUIDE_Txxx.md` — slice-specific acceptance and evidence.

If backend behavior and generated OpenAPI disagree, stop and return the mismatch to the owning
backend task. The frontend must not choose which conflicting contract to trust.

The UI must not:

- invent an API field, status, role, permission, filter, or state transition;
- infer payment success from the Stripe browser return;
- expose an action that the authenticated user cannot perform according to the backend contract;
- treat hidden navigation or controls as authorization;
- reproduce backend business rules as an independent source of truth; or
- display secrets, tokens, webhook payloads, stack traces, or personal data outside the approved
  user-owned or administrator view.

If a backend contract is not implemented yet, the UI guide defines hierarchy and state behavior
only. Exact form controls and table columns are finalized from that task's OpenAPI contract.

---

## 3. Foundation

### 3.1 Name and tone

- Visible application name: **E-Commerce**.
- Language: English only.
- Voice: concise, direct, and neutral; do not use playful marketing copy.
- Buttons start with a verb: `Add to cart`, `Save product`, `Cancel order`.
- Headings name the object or task: `Products`, `Your cart`, `Order details`.

### 3.2 Semantic colors

Implementation must use Angular Material's supported theming API for the installed Angular version.
These semantic values define intent; feature components must not introduce new color literals.

| Token | Reference value | Usage |
|---|---:|---|
| Primary | `#1565c0` | Main actions, active navigation, links |
| Primary strong | `#0d47a1` | Hover/pressed emphasis |
| Surface | `#ffffff` | Page and card surfaces |
| Surface muted | `#f5f7fa` | Page background, grouped regions |
| Text | `#1f2937` | Primary text |
| Text muted | `#4b5563` | Supporting text |
| Border | `#d1d5db` | Decorative dividers only (not control boundaries) |
| Control border | `#6b7280` | Form-control outlines and other boundaries needed to identify a control (≥3:1) |
| Success | `#2e7d32` | Confirmed/completed feedback |
| Warning | `#b45309` | Pending, low-stock, caution text (≥4.5:1 on Surface) |
| Warning fill | `#ed6c02` | Warning icons and fills only; never text on a light surface |
| Error | `#b3261e` | Validation, failed, destructive feedback |
| Focus | `#005fcc` | Visible keyboard focus indicator |

Status must never be communicated by color alone; pair it with text and, where useful, an icon.

Angular Material derives tonal palettes, so reference values are targets: use the theme's supported
override/system-variable API to match them, and accept Material-generated tones only where the
contrast minimums in this table and §5.5 still hold.

### 3.3 Typography, spacing, and shape

- Use a system sans-serif stack; do not load a remote font.
- Prefer text labels. When an icon adds meaning, use an SVG bundled with the application and
  registered locally; do not load an icon font or icon script from a CDN.
- Base body size: 16px; supporting text must remain at least 14px.
- Use a 4px spacing base with 8, 12, 16, 24, and 32px as the normal scale.
- Default control/card radius: 8px.
- Prefer borders and spacing over decorative shadows; reserve elevation for toolbar, menus, dialogs,
  and overlays.
- Keep readable content within a 1200px centered container with 24px desktop and 16px compact
  horizontal padding.

### 3.4 Breakpoints and density

| Range | Intent | Required behavior |
|---|---|---|
| `<600px` | Compact reference | Drawer navigation, one-column forms, reduced grid columns, scrollable wide tables |
| `600–959px` | Medium reference | Wrapped toolbar/actions and two-column layouts where content fits |
| `>=960px` | Primary desktop | Full toolbar navigation, normal-density tables/forms, multi-column catalog |

Required screenshot widths remain 375px, 768px, and 1280px. Mobile only needs functional,
readable compatibility: do not create separate card implementations for desktop tables. Hide a
nonessential table column only when its value remains available from the row's detail route.

---

## 4. Shared Application Shell

The root route `/` redirects to `/products`.

### 4.1 Desktop

```text
+--------------------------------------------------------------------------------+
| E-Commerce | Products | [Cart] [Orders] [Admin ▾]       [Account ▾ / Log in] |
+--------------------------------------------------------------------------------+
|                                                                                |
|  Breadcrumbs when below a top-level page                                       |
|  Page title                                      Primary page action           |
|                                                                                |
|  Page content                                                                  |
|                                                                                |
+--------------------------------------------------------------------------------+
```

### 4.2 Compact reference

```text
+--------------------------------------+
| [Menu]  E-Commerce        [Account] |
+--------------------------------------+
| Page title                           |
| Page content                         |
+--------------------------------------+

Drawer: Products / Cart / Orders / Admin / Account actions
Only authorized destinations are rendered.
```

### 4.3 Role and capability visibility

| Capability | Guest | Customer | Administrator |
|---|---:|---:|---:|
| Products | Show | Show | Show |
| Login / Register | Show | Hide | Hide |
| Cart / Checkout / Orders | Hide | Show | Show only if the account also holds `CUSTOMER` |
| Admin menu | Hide | Hide | Show |
| Logout | Hide | Show | Show |

Visibility is decided from the roles in the backend's authenticated-user response (T003):
`CUSTOMER` unlocks cart, checkout, and customer orders; `ADMIN` unlocks the admin menu. There is no
separate capability API. The UI must not assume `ADMIN` implies `CUSTOMER`.

Hiding is a usability rule only; the backend must still reject unauthorized requests. Route guards
behave consistently:

- A guest opening any protected route is redirected to `/login` without a return URL.
- An authenticated user lacking the required role sees the generic forbidden page (with a link to
  `/products`) and no protected data is requested or displayed.
- A backend `403` on an allowed-looking route shows the same forbidden page; a `401` after the one
  permitted refresh-and-retry follows the §8 refresh-failure rule.

---

## 5. Shared Interaction and State Patterns

### 5.1 Feedback

| Situation | Presentation |
|---|---|
| Field validation | Inline beneath the control, associated programmatically with that control |
| Form-level rejection | Alert above the form, with field errors also placed inline |
| Page/load/server failure | Persistent page-level alert with a retry action when retry is safe |
| Successful mutation | Short snackbar; keep the resulting state visible on the page |
| Destructive administrator action | Confirmation dialog naming the object and consequence |
| Conflict/stale state | Persistent alert; refresh affected data before another attempt |
| Not found | Page-level not-found state with a safe navigation action |
| Forbidden | Generic forbidden state without protected object details |

### 5.2 Loading and empty states

- Disable only the action currently submitting; preserve readable form values.
- A submitting button uses a progress indicator and stable label such as `Saving…`.
- Initial page loads show a labeled progress indicator or simple skeleton matching final structure.
- Empty states include a heading, one-sentence explanation, and at most one permitted next action.
- Do not show an empty-state message before the first request resolves.
- Prevent duplicate checkout, refund, cancellation, and status-transition submissions.

### 5.3 Forms

- Use reactive forms and backend/OpenAPI constraints as the validation source.
- Mark required fields in the visible label or supporting text.
- Validate on blur and submit; do not display every error before interaction.
- Preserve safe user input after recoverable server failures.
- Use dedicated admin form pages; dialogs are limited to confirmation.
- Put the primary action first and a text/secondary cancel action beside it.

### 5.4 Lists and tables

- Server-side pagination/filter/sort controls map directly to supported backend parameters.
- Use stable labels rather than exposing enum formatting verbatim.
- Desktop uses Angular Material tables for operational lists.
- Compact widths retain the same table in a horizontally scrollable region; do not build a second
  mobile card component.
- Row actions use visible text when space permits and an accessible overflow menu otherwise.

### 5.5 Accessibility

Target WCAG 2.2 AA for supported flows:

- full keyboard operation and logical focus order;
- visible focus indicators;
- one page-level `h1` with correctly nested headings;
- persistent labels for all form controls;
- programmatic association of errors and helper text;
- status announcements for asynchronous success/failure where focus does not move;
- focus trapping/restoration for dialogs and drawer;
- sufficient contrast and no color-only meaning; and
- minimum 44px target size for primary compact-screen actions where practical.

Use Angular Material's accessible primitives before creating custom interactive controls.

### 5.6 Currency, dates, and identifiers

- Display monetary values using the ISO currency returned by the backend and `Intl` formatting.
- Perform no exchange-rate lookup or frontend currency conversion.
- Display user-facing dates consistently in the browser locale while preserving backend UTC values.
- Order codes and stable business references may be displayed; internal database IDs should not be
  presented unless the backend contract explicitly treats them as user-facing.

---

<a id="t001-shared-shell"></a>
## 6. T001 — Shared Shell and Material Foundation

T001 replaces the Angular starter screen with the shared shell, theme, semantic tokens, base page
container, and routing outlet. It installs/configures Angular Material but does not create feature
screens or a general wrapper component for every Material primitive.

Required proof:

- desktop toolbar and compact drawer hierarchy;
- anonymous navigation state;
- placeholder route content without Angular starter branding;
- visible keyboard focus and skip-to-content behavior;
- no overflow at 375px, 768px, or 1280px; and
- no external font, icon CDN, or second UI library.

---

<a id="t002-public-catalog"></a>
## 7. T002 — Public Catalog

### Routes

- `/products` — catalog list.
- `/products/:id` — product detail.

### Catalog wireframe

```text
Products
[Search text________________] [Category ▾] [Sort ▾] [Apply filters]

+----------------+  +----------------+  +----------------+
| Placeholder    |  | Placeholder    |  | Placeholder    |
| Product name   |  | Product name   |  | Product name   |
| Price + ISO    |  | Price + ISO    |  | Price + ISO    |
| [View details] |  | [View details] |  | [View details] |
+----------------+  +----------------+  +----------------+

[Previous] Page N of M [Next]
```

- Filters apply only when `Apply filters` is activated.
- Search, category, sort, and page use URL query parameters.
- Treat placeholder thumbnails as decorative (`alt=""`) because the adjacent product name already
  carries the meaningful content.
- T002 does not introduce cart controls. T005 adds the approved guest/authenticated cart actions
  after identity and cart contracts exist.
- The page must cover loading, filtered-empty, initial-empty, invalid-filter, server-failure, and
  inactive/not-found detail states.

### Product detail wireframe

```text
Products / Product name

[Placeholder image]   Product name
                      Price + ISO currency
                      Backend-defined public details
                      [Cart action added by T005]
```

---

<a id="t003-authentication"></a>
## 8. T003 — Authentication and Session UI

### Routes

- `/login`
- `/register`

```text
Log in
[Email_______________________]
[Password____________________]
[Log in]
New customer? [Create account]

[Persistent form/server alert when needed]
```

- Forms use only backend-defined fields and validation.
- Successful login navigates to `/products`.
- Registration success follows the backend-defined session result: navigate to `/products` if a
  session is issued; otherwise navigate to `/login` with a success message.
- Silent refresh has no blocking full-page flash after the initial shell is available.
- Refresh failure clears client authentication state and navigates to `/login`.
- Do not render or log access tokens, refresh tokens, cookie values, or credential details.

---

<a id="t004-catalog-administration"></a>
## 9. T004 — Catalog Administration

### Route family

- `/admin/products`
- `/admin/products/new` and `/admin/products/:id/edit` when the backend exposes those operations
- `/admin/categories`
- `/admin/categories/new` and `/admin/categories/:id/edit` when exposed

```text
Products                                      [Add product]
[Backend-supported filters]                   
| Name | Category | Price | Status | Actions |
| ...  | ...      | ...   | ...    | Edit ▾ |

Product form
[Backend/OpenAPI-defined fields]
[Save product] [Cancel]
```

- Fields, constraints, statuses, and permitted mutations come from the backend/OpenAPI contract.
- Deactivate/reactivate uses a confirmation dialog; there is no hard-delete UI.
- When a category cannot be deactivated, show the safe backend conflict near the page action.
- Placeholder imagery remains display-only; no product image field is added.

---

<a id="t005-shopping-cart"></a>
## 10. T005 — Shopping Cart

### Route

- `/cart`

```text
Your cart
| Product | Unit price | Quantity | Line total | Remove |

                                      Total: amount + ISO currency
                                      [Proceed to checkout]
[Clear cart]
```

- Quantity updates use backend-supported bounds and expose pending/error state per row.
- T005 adds catalog/detail cart actions: guests see `Sign in to add`, which navigates to `/login`
  without a return URL; after login, navigate to `/products` and do not replay the pending action.
- Authenticated users see `Add to cart` only when their backend-reported roles include `CUSTOMER`.
- Removing one item acts immediately and reports success/failure without a dialog. Clearing a
  populated cart requires confirmation. Routine quantity edits never require confirmation.
- Price/availability conflicts remain visible until the cart refreshes.
- A line whose product the backend reports as inactive or missing shows `No longer available`
  (text plus warning styling), offers only `Remove`, and `Proceed to checkout` stays disabled with
  an explanation until such lines are removed.
- The cart total displays the single ISO currency returned by the backend. If a cart response ever
  contains more than one currency, stop and return the mismatch to the owning backend task; do not
  sum, convert, or split totals in the UI.
- Empty state action returns to `/products`.

---

<a id="t006-inventory-administration"></a>
## 11. T006 — Warehouse and Inventory Administration

### Route family

- `/admin/warehouses`
- `/admin/warehouses/new` and `/admin/warehouses/:id/edit` when exposed
- `/admin/inventory`

```text
Inventory
[Warehouse ▾] [Search product] [Apply filters]
| Product | On hand | Reserved | Available | Safety threshold | Actions |

Edit inventory item
[Backend/OpenAPI-defined quantity and threshold controls]
[Save inventory] [Cancel]
```

- Do not expose internal warehouse-allocation ranking as a customer choice.
- Status, quantities, constraints, and actions follow the backend contract.
- Low-stock indication uses warning text plus color.
- Warehouse deactivation conflict explains that active reservations/orders prevent the action
  without exposing customer or order details unnecessarily.

---

<a id="t008-checkout-payment"></a>
## 12. T008 — Checkout and Payment Result

### Routes

- `/checkout`
- `/checkout/result` or the concrete callback route established by the frontend/backend contract

### Checkout wireframe

```text
Checkout
+--------------------------------------+  +---------------------------+
| Shipping details                     |  | Order summary             |
| [Backend-defined shipping fields]    |  | Items                     |
|                                      |  | Total + ISO currency      |
| [Continue to payment]                |  |                           |
+--------------------------------------+  +---------------------------+
```

- Use one page, not a Material stepper.
- Required shipping fields and validation come from the backend contract.
- `Continue to payment` disables after one submission and remains guarded against double submit.
- The browser return displays `Payment processing` until verified backend state says otherwise.
- While pending, the result page re-reads backend order/payment state every 2 seconds for at most
  30 seconds, stopping as soon as a non-pending state is returned. After 30 seconds it stops
  polling and shows `Still processing` with a `Refresh status` button (one read per activation) and
  a link to the order detail. Polling stops when the user leaves the page; status changes are
  announced through a live region.
- Result states are pending, confirmed, cancelled/expired, stock conflict, and safe failure.
- Never label an order paid or confirmed from browser query parameters alone.

---

<a id="t011-customer-orders"></a>
## 13. T011 — Customer Orders

### Routes

- `/orders`
- `/orders/:id`

```text
Orders
| Order | Placed | Total | Payment | Fulfillment | View |

Order details
[Status summary]
[Item and price snapshots]
[Shipping snapshot]
[Status history]
```

- Empty history links to `/products`.
- Status labels come from backend-defined states and are presented in customer-readable language.
- An inaccessible order uses the backend's safe not-found/forbidden behavior without leaking owner
  information.
- Preserve historical product, price, shipping, payment, and selected-warehouse snapshots exactly
  as the authorized contract exposes them.

---

<a id="t012-order-administration"></a>
## 14. T012 — Order Administration

### Routes

- `/admin/orders`
- `/admin/orders/:id`

```text
Orders
[Backend-supported filters] [Apply filters]
| Order | Customer reference | Payment | Fulfillment | Total | View |

Order details
[Authorized order information]
[Only backend-permitted transition actions]
[Status / refund history]
```

- Render only transitions the backend reports or contract permits for the current state.
- Cancellation, refund, and irreversible fulfillment actions require a confirmation dialog naming
  the action and consequence.
- A submitted refund/cancellation remains pending until verified backend state advances; do not
  infer provider success from a client response.
- Conflict responses refresh the order before offering another action.

---

<a id="t015-ui-evidence"></a>
## 15. T015 — Cross-Application UI Evidence

T015 does not redesign screens. It verifies the implemented UI against this specification:

- critical Browse → Sign in → Cart → Checkout → Payment result → Orders journey;
- administrator catalog, inventory, fulfillment, cancellation, and refund journeys;
- guest, customer, administrator, forbidden, loading, empty, validation, conflict, and server-error
  states relevant to each route;
- screenshots at 375px, 768px, and 1280px;
- keyboard navigation, focus behavior, accessible names, form-error association, dialog focus, and
  non-color status meaning; and
- absence of secret/PII leakage in UI errors, screenshots, browser logs, and test artifacts.

Visual regression baselines must be created from an independently reviewed implementation, not from
the Angular starter screen or an unapproved intermediate design. Each UI-bearing task records its
screenshots as review evidence and commits them as baselines only after it passes Stage 4; T015
compares the final application against those committed baselines and re-baselines only through a
reviewed change.

---

## 16. Definition of UI Done

A UI-bearing task is not complete until:

- its task guide links to the matching section in this document;
- implemented fields/actions match the backend/OpenAPI contract;
- required loading, empty, success, validation, forbidden/not-found, conflict, and server-failure
  states are covered where applicable;
- role-ineligible navigation/actions are hidden and backend authorization is independently tested;
- keyboard, focus, label, error, and contrast expectations pass;
- screenshots at 375px, 768px, and 1280px are recorded or explicitly justified N/A;
- visual regression, Angular Material/token compliance, and responsiveness Evidence rows are filled;
  and
- frontend unit/component tests plus the task's required browser checks pass.

## 17. Deferred Decisions

These decisions remain intentionally deferred until a requirement earns them:

- product imagery and media management;
- dark mode and alternate branding;
- localization beyond English or currency conversion;
- advanced mobile-specific navigation or list presentations;
- saved searches, live search, autocomplete, or faceted navigation;
- Storybook and a separately versioned component library; and
- custom analytics dashboards or data-visualization components.
