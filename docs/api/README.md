# RouteShareApp OpenAPI / Swagger Documents

These files define the initial API contracts for each RouteShareApp application:

- `mobile-app.openapi.json` — **ComiGo unified mobile app API** (passenger + driver, one account)
- `admin-web.openapi.json` — Admin web app API

`passenger-app.openapi.json` and `driver-app.openapi.json` were merged into `mobile-app.openapi.json`
on 2026-08-01 and deleted (Decision 011). ComiGo ships one mobile application, so it has one client
contract. The `/api/v1/passenger/**` and `/api/v1/driver/**` paths are retained — they are role-scoped
resource namespaces, not app boundaries.

Every operation in the mobile contract carries an `x-routeshare-status` extension:

| Value | Meaning |
| --- | --- |
| `IMPLEMENTED` | Live in `apps/api` today |
| `PLANNED_SLICE_NN` | Specified; built in that slice of the ComiGo backend plan |
| `INTERNAL_NOT_FOR_CLIENTS` | Implemented, but outside the mobile client surface |
| `CUT` | Deliberately removed from the product, with a reason |

`API_BACKEND_RECONCILIATION.md` is generated from that field. Regenerate it rather than editing it.

All documents use OpenAPI 3.1 JSON. Swagger UI, Swagger Editor, Redocly, Stoplight, Postman, and springdoc tooling can import these JSON files directly.

Authentication model:

- Keycloak owns login, users, sessions, roles, and JWT issuing.
- APIs use `Authorization: Bearer <access_token>`.
- Retry-safe mutations include the `Idempotency-Key` header.

These are contract-first planning documents. They should be refined together with DTOs, validation rules, and domain state-machine tests during implementation.

## 2026-06-01 API contract audit

The passenger, driver, and admin OpenAPI files were reviewed against the business requirement PDF and the supplied passenger/driver design ZIP. Missing product APIs were added to the contract files, and the detailed findings are tracked in `API_GAP_ANALYSIS.md`.

Important: these OpenAPI files are product/client contracts. Some newly added paths are not implemented in `apps/api` yet. Before mobile/admin implementation, reconcile generic backend resource endpoints with the app-specific contract endpoints and generate typed clients under `packages/api-contracts`.

- `APP_BACKEND_READINESS_AUDIT.md` — final passenger/driver/admin backend readiness audit before app implementation phases.
