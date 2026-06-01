# RouteShareApp Development Status

Last Updated: 2026-06-02 00:42 +0530

## Purpose

This file is the first file to read before continuing RouteShareApp development. It shows the current phase, completed work, active work, pending work, blockers, verification status, and the next recommended task.

## Current State

- Current Phase: `READY_TO_START_PHASE_06`
- Current Milestone: `MILESTONE_PRE_PHASE_06_CLOSURE`
- Current Active Task: `Pre-Phase-06 backend/API closure completed; next task is Phase 06 realtime/location foundation`
- Status: `PRE_PHASE_06_BACKEND_API_READY_VERIFIED`
- Repository Git Status: `Clean after latest commit feat: complete pre-phase 06 backend gate`

## Estimated Progress

- Completed known implementation tasks: 80
- Total known high-level tasks: 95+
- Estimated overall progress: 63%

> Progress is estimated from known tasks and will change as requirements are added or split into smaller implementation tasks. Phases 00 through 05.5 are now closed for the Phase 06 gate. Later product areas such as realtime websockets, notifications/support/SOS, real payment-provider integration, full mobile/admin UI implementation, observability, and production hardening remain in their own later phases.

## Completed So Far

- [x] Keycloak/auth architecture documented.
- [x] One Keycloak user can act as passenger and/or driver.
- [x] Backend owns business profiles by Keycloak subject.
- [x] PostgreSQL/PostGIS multi-schema architecture documented.
- [x] OpenAPI/Swagger planning documents created under `docs/api/`.
- [x] Development tracking files created under `docs/development/`.
- [x] Repository skeleton exists with backend, app, infrastructure, scripts, and docs folders.
- [x] Local Docker infrastructure created for PostgreSQL/PostGIS, Redis, Redpanda, Keycloak, and MinIO.
- [x] Dev scripts patched for non-interactive SSH shells: PATH, `DOCKER_CONFIG`, and `docker-compose` usage.
- [x] Spring Boot 3 / Java 21 backend scaffolded in `apps/api`.
- [x] Flyway migrations created and applied.
- [x] Module schemas and foundation tables created for identity, passenger, driver, vehicle, routing, booking, trip, location, pricing, payment, and common idempotency.
- [x] OAuth2 resource-server security configured.
- [x] Keycloak role converter hardened to trust `api-monolith` resource roles only.
- [x] Common API response and error handling added.
- [x] Identity projection/upsert from JWT implemented.
- [x] `GET /api/v1/auth/me` implemented.
- [x] Passenger profile API implemented.
- [x] Driver application/profile API implemented.
- [x] Vehicle create/list API implemented with deterministic `INSERT ... RETURNING` creation.
- [x] Admin driver review API implemented with enum-backed status validation.
- [x] Pricing estimate domain/service endpoint implemented.
- [x] Route publishing endpoint implemented with explicit coordinate DTO validation.
- [x] Route publishing hardened to require approved driver profile and approved vehicle ownership.
- [x] Booking endpoint moved into application service with transactional seat decrement.
- [x] Trip transition endpoint moved into application service with ownership/admin authorization and state machine validation.
- [x] Location update endpoint moved into application service with current-driver ownership and timestamp freshness validation.
- [x] Payment intent endpoint moved into application service and no longer accepts client-controlled amount/currency; amount is derived from booking fare and currency defaults server-side to LKR.
- [x] Unit tests added for payment service and route service hardening.
- [x] Existing domain tests pass.
- [x] Runtime API health verified after the latest hardening patches.
- [x] Added Flyway V004 hardening constraints for active payment intent uniqueness and positive booking fare estimates.
- [x] Booking now derives and stores fare estimates during booking creation.
- [x] Route publishing validates future departure time and requested seats against approved vehicle capacity.
- [x] Location updates now verify the active trip belongs to the current driver profile.
- [x] Trip transitions now run transactionally and lock the trip status row before transition.
- [x] Suspended/deleted local app users are blocked after JWT identity projection.
- [x] Saved places CRUD APIs implemented and verified.
- [x] Trusted contacts CRUD APIs implemented and verified.
- [x] Driver document metadata APIs implemented and verified.
- [x] Vehicle document metadata APIs implemented and verified.
- [x] Admin vehicle review API implemented and verified.
- [x] Backend persistence refactored away from service-layer queries/JdbcTemplate into Spring Data JPA repositories under infrastructure.
- [x] Lombok-backed JPA entities/repositories added for backend persistence boundaries.
- [x] Spotless/google-java-format configured and applied to backend Java sources.
- [x] Architecture tests added to prevent JdbcTemplate in main sources, database APIs/SQL in application services, and repositories outside infrastructure.

- [x] Approved backend architecture simplified from hexagonal `port/in` / `port/out` naming to learner-friendly Spring Boot modular monolith packages.
- [x] Backend packages refactored to `controller`, `dto`, `mapper`, `service`, `service/impl`, `facade`, `facade/impl`, `domain`, `entity`, and `repository` for implemented modules.
- [x] Module facades added for identity, passenger, driver, vehicle, routing, booking, and cross-module calls now use facades instead of foreign repositories/entities.
- [x] MapStruct dependency and shared `RouteShareMapperConfig` added; driver/passenger/vehicle mappers now use MapStruct.
- [x] Architecture tests expanded for service/impl, facade, mapper config, controller, entity/repository, and cross-module boundary rules.
- [x] Architecture documentation updated with the approved service/impl + facade approach.
- [x] Java 21 virtual threads enabled for Spring Boot request/task execution with bounded Hikari database pool settings.

- [x] Phase 04 route schedule rules, route occurrences, and route bucket indexing committed.
- [x] Route search now exposes route occurrence identity and matched pickup/drop route fractions for booking handoff.
- [x] Booking creation now reserves seats against `routing.route_occurrence` instead of abstract `routing.route_plan`.
- [x] Booking rows now store `route_occurrence_id`, `pickup_route_fraction`, and `dropoff_route_fraction`.
- [x] Booking creation now writes initial `CONFIRMED` status history to `booking.booking_status_history`.
- [x] Booking creation now requires an explicit HTTP `Idempotency-Key` and replays completed matching responses from `common.idempotency_key` without reserving seats twice.
- [x] Booking status transitions for cancel/reject/complete are implemented with same-transaction status history rows.
- [x] Passenger boarded/no-show/drop-off state machine implemented per booking on the concrete route occurrence.
- [x] Immutable fare ledger foundation records booking fare estimates before payment intent creation/replay.
- [x] Payment capture, void, refund, driver cash collection, and passenger receipt foundation implemented.
- [x] Driver earnings summary/transactions, MVP platform commission, and settlement-balance read models implemented.
- [x] Route share link/QR payload foundation implemented.
- [x] Driver pre-trip checklist, arrived-at-pickup, and fare-adjustment request endpoints implemented.
- [x] Admin payment list/detail/events and cash collection projections implemented.
- [x] Lightweight TypeScript workspace and `packages/api-contracts` endpoint inventory generated.
- [x] Structured logging conventions documented.
- [x] Testcontainers Flyway/PostGIS migration smoke test added; it auto-skips when the Java Docker client cannot connect.

- [x] Passenger/driver/admin OpenAPI contracts audited against the business requirement PDF and supplied passenger/driver designs.
- [x] Missing product APIs added to `docs/api/passenger-app.openapi.json`, `docs/api/driver-app.openapi.json`, and `docs/api/admin-web.openapi.json`.
- [x] API gap analysis documented in `docs/api/API_GAP_ANALYSIS.md`.
- [x] Roadmap now has explicit API contract gates before passenger mobile, driver mobile, and admin web implementation.
- [x] Blocker 006 added for API contract/backend reconciliation.
- [x] API backend reconciliation document created at `docs/api/API_BACKEND_RECONCILIATION.md`.
- [x] First app-facing backend alias controllers implemented with TDD:
  - Passenger ride search create alias.
  - Passenger booking create/cancel aliases.
  - Passenger payment intent alias.
  - Driver route create alias.
  - Driver trip start/complete and passenger board/no-show/drop-off aliases.
- [x] Targeted alias controller tests and full backend `spotless:check test` passed.

## In Progress

- [ ] Phase 06 realtime location and WebSocket foundation is the next implementation phase.

## Completed Phase-Gate Closure

- [x] Phase 05 booking, trip, fare, payment, MVP earnings/commission/settlement-balance read models are implemented for the Phase 06 gate.
- [x] Phase 05.5 app-facing backend/API reconciliation is complete enough for the Phase 06 gate. Realtime-dependent, notification/support/SOS, real provider, and UI/client implementation items are explicitly deferred to later phases.

## Pending Roadmap Summary

- [x] Phase 00 — Project architecture and file structure.
- [x] Phase 01 — Local development environment.
- [x] Phase 02 — Backend modular monolith foundation.
- [x] Phase 03 — Identity, passenger, driver, KYC/document metadata, vehicle, saved places, trusted contacts, and vehicle review foundation APIs are implemented.
- [x] Phase 04 — Route publishing and route matching. Route search, schedule rules, route occurrences, and bucket-cell broad filtering are implemented and committed.
- [x] Phase 05 — Booking, trip lifecycle, fare, payment, settlement. Booking occurrence inventory, idempotency, status history, passenger trip states, immutable fare ledger, payment capture/void/refund, cash collection, receipt foundation, driver earnings, MVP commission, and settlement-balance read models are implemented for the Phase 06 gate.
- [ ] Phase 06 — Realtime location and WebSocket updates.
- [ ] Phase 07 — Passenger mobile app (blocked by passenger OpenAPI/backend reconciliation gate).
- [ ] Phase 08 — Driver mobile app (blocked by driver OpenAPI/backend reconciliation gate).
- [ ] Phase 09 — Admin web app (blocked by admin OpenAPI/backend reconciliation gate).
- [ ] Phase 10 — Hardening, observability, performance, deployment readiness.

## Latest Verification

- TypeScript contract package:
  - Command: `pnpm install && pnpm --filter @routeshare/api-contracts typecheck`.
  - Result: passed.
- Maven backend tests and formatting:
  - Command: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw spotless:apply spotless:check test -q`
  - Result: `BUILD SUCCESS`.
- Virtual thread configuration:
  - `spring.threads.virtual.enabled=true` configured in `application.yml`.
  - HikariCP pool bounds configured with `ROUTESHARE_DB_POOL_MAX_SIZE`, `ROUTESHARE_DB_POOL_MIN_IDLE`, and `ROUTESHARE_DB_CONNECTION_TIMEOUT_MS`.
- Architecture verification:
  - `PersistenceArchitectureTest` passes.
  - Enforces no `JdbcTemplate` in main sources, no SQL/low-level database APIs in service implementations, repositories under `repository`, entities under `entity`, service implementations under `service/impl`, facades under `facade/impl`, controllers not importing repositories/entities, MapStruct shared mapper config usage, and no cross-module repository/entity/impl imports.
- Runtime health:
  - `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}` after the pre-Phase-06 closure.
- Database migration:
  - Latest Flyway migration is version `012`, success `true` in the running app migration history.
  - Verified `payment.fare_ledger_entry`, `routing.route_share_link`, and `trip.pre_trip_checklist` exist in the running database.
  - Verified `common.idempotency_key` exists and is used by booking create replay handling.
  - Verified `booking.booking_status_history` exists and is used for initial and transition audit rows; `booking.booking` has occurrence/fraction columns; `trip.passenger_trip_state` exists; `trip.trip` has `route_occurrence_id`; and `payment.fare_ledger_entry` exists.
- Git status:
  - Latest committed backend slice before this work is the prior Phase 05 baseline.
  - Pre-Phase-06 closure changes are committed in the latest pre-Phase-06 gate commit.

## Blockers / Risks

- No active runtime blocker for the backend foundation.
- Git baseline exists; pre-Phase-06 closure changes are committed; current working tree is clean.
- Full product is not complete yet, but phases 00 through 05.5 are complete for the Phase 06 gate. Remaining major areas are Phase 06+ or later hardening/app phases.
- Current tests are mostly unit-level. Add Spring Boot integration/security tests before relying on these APIs as production-ready.
- Dev infrastructure exposes local ports and uses local-only development credentials; do not reuse these settings for production.

## Next Recommended Task

Phase 06 realtime location foundation can start from the latest commit.

## Update Rule

After every completed implementation task, update:

- `DEVELOPMENT_STATUS.md`
- `IMPLEMENTATION_ROADMAP.md`
- `TASK_LOG.md`

If relevant, also update:

- `DECISION_LOG.md`
- `REQUIREMENTS_CHANGE_LOG.md`
- `BLOCKERS.md`
- `SESSION_SUMMARIES/YYYY-MM-DD-session-summary.md`


## 2026-06-01 23:43 +0530 — Phase 05/05.5 continuation before Phase 06

Completed additional core backend API reconciliation before Phase 06:

- Passenger booking list/detail/current trip/history projections.
- Driver route list/detail/cancel endpoints.
- Driver trip list/detail and booking request projections.
- Driver booking approve/decline commands with driver-owned authorization path and booking status history reuse.

Verification:

- Targeted controller tests passed.
- Full backend `spotless:apply spotless:check test` passed.
- API restarted and `/actuator/health` returned HTTP 200.

Remaining before/around Phase 06:

- Payment lifecycle capture/void/refund/cash/earnings/settlement.
- Receipt/final fare endpoints.
- Share link/QR, pre-trip checklist, arrived pickup, notifications/support/SOS/admin depth.


## 2026-06-02 00:35 +0530 — Audit cleanup before commit

The earlier docs still showed stale incomplete states for phases before Phase 06 even though the implementation had been added. This audit corrected those stale statuses:

- Phase 00 TypeScript workspace/package setup is now present with root `package.json`, `pnpm-workspace.yaml`, `packages/api-contracts/package.json`, and `tsconfig.json`.
- Phase 02 migration list, structured logging convention, and Testcontainers migration smoke coverage are now tracked.
- Phase 03 is marked completed for its foundation scope.
- Phase 05 and Phase 05.5 are marked completed for the Phase 06 gate.
- Deferred product workflows are now labeled as Phase 06+ or later-phase work instead of pre-Phase-06 blockers.
