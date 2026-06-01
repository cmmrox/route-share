# @routeshare/api-contracts

Lightweight contract inventory generated from `docs/api/*.openapi.json` after the pre-Phase-06 backend reconciliation.

This package is intentionally simple until a full OpenAPI TypeScript generator is added. It provides stable endpoint inventories for Passenger, Driver, and Admin client wiring readiness checks.

Regenerate by re-reading `docs/api/passenger-app.openapi.json`, `docs/api/driver-app.openapi.json`, and `docs/api/admin-web.openapi.json` into `src/index.ts`.
