# RouteShareApp OpenAPI / Swagger Documents

These files define the initial API contracts for each RouteShareApp application:

- `passenger-app.openapi.json` — Passenger mobile app API
- `driver-app.openapi.json` — Driver mobile app API
- `admin-web.openapi.json` — Admin web app API

All documents use OpenAPI 3.1 JSON. Swagger UI, Swagger Editor, Redocly, Stoplight, Postman, and springdoc tooling can import these JSON files directly.

Authentication model:

- Keycloak owns login, users, sessions, roles, and JWT issuing.
- APIs use `Authorization: Bearer <access_token>`.
- Retry-safe mutations include the `Idempotency-Key` header.

These are contract-first planning documents. They should be refined together with DTOs, validation rules, and domain state-machine tests during implementation.

## 2026-06-01 API contract audit

The passenger, driver, and admin OpenAPI files were reviewed against the business requirement PDF and the supplied passenger/driver design ZIP. Missing product APIs were added to the contract files, and the detailed findings are tracked in `API_GAP_ANALYSIS.md`.

Important: these OpenAPI files are product/client contracts. Some newly added paths are not implemented in `apps/api` yet. Before mobile/admin implementation, reconcile generic backend resource endpoints with the app-specific contract endpoints and generate typed clients under `packages/api-contracts`.
