# RouteShareApp Development Status

Last Updated: 2026-06-01 21:51 +0530

## Purpose

This file is the first file to read before continuing RouteShareApp development. It shows the current phase, completed work, active work, pending work, blockers, verification status, and the next recommended task.

## Current State

- Current Phase: `PHASE_05_BOOKING_TRIP_FARE_PAYMENT_SETTLEMENT`
- Current Milestone: `MILESTONE_05_BOOKING_STATUS_TRANSITIONS`
- Current Active Task: `Phase 05 booking status transitions implemented; next is route-occurrence-aware passenger trip states`
- Status: `PHASE_05_BOOKING_STATUS_TRANSITIONS_VERIFIED`
- Repository Git Status: `Phase 05 booking status-transition working changes ready to commit`

## Estimated Progress

- Completed known implementation tasks: 63
- Total known high-level tasks: 80+
- Estimated overall progress: 55%

> Progress is estimated from known tasks and will change as requirements are added or split into smaller implementation tasks. The backend foundation now runs locally and has verified tests, but the full product backend still needs route matching, richer booking/trip/payment workflows, document/KYC upload flows, notifications, realtime websockets, admin management, observability, and production hardening.

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

## In Progress

- [ ] Complete the full backend beyond the foundation/MVP slice.
- [ ] Expand backend tests to cover API/security/integration behavior with Spring context and PostgreSQL/PostGIS.
- [ ] Add route matching/search, richer booking/trip/payment workflows, realtime websocket location updates, notification/event outbox, and admin management APIs.

## Pending Roadmap Summary

- [x] Phase 00 — Project architecture and file structure.
- [x] Phase 01 — Local development environment.
- [x] Phase 02 — Backend modular monolith foundation.
- [x] Phase 03 — Identity, passenger, driver, KYC/document metadata, vehicle, saved places, trusted contacts, and vehicle review foundation APIs are implemented.
- [x] Phase 04 — Route publishing and route matching. Route search, schedule rules, route occurrences, and bucket-cell broad filtering are implemented and committed.
- [~] Phase 05 — Booking, trip lifecycle, fare, payment, settlement. Booking now reserves route occurrences, stores matched fractions, records initial and transition status history, and has explicit `Idempotency-Key` replay safety; passenger trip states, payment, and settlement remain.
- [ ] Phase 06 — Realtime location and WebSocket updates.
- [ ] Phase 07 — Passenger mobile app.
- [ ] Phase 08 — Driver mobile app.
- [ ] Phase 09 — Admin web app.
- [ ] Phase 10 — Hardening, observability, performance, deployment readiness.

## Latest Verification

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
  - `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}` after the booking status-transition slice.
- Database migration:
  - Latest Flyway migration is version `008`, success `true`.
  - Verified `common.idempotency_key` exists and is used by booking create replay handling.
  - Verified `booking.booking_status_history` exists and is used for initial and transition audit rows; `booking.booking` has occurrence/fraction columns.
- Git status:
  - Latest committed backend slice before this work is `4cbe840 feat(booking): record booking status history`.
  - Phase 05 booking status-transition slice is implemented and verified but not committed yet.

## Blockers / Risks

- No active runtime blocker for the backend foundation.
- Git baseline exists; current working tree only contains the verified Phase 05 booking idempotency slice pending commit.
- Full backend is not complete yet. Remaining major areas are tracked in the roadmap and blockers file.
- Current tests are mostly unit-level. Add Spring Boot integration/security tests before relying on these APIs as production-ready.
- Dev infrastructure exposes local ports and uses local-only development credentials; do not reuse these settings for production.

## Next Recommended Task

Continue Phase 05 with the next booking/trip safety slice:

1. Move trip lifecycle from route-plan identity toward route-occurrence identity where needed.
2. Add passenger boarded/no-show/drop-off state machine.
3. Continue payment intent, immutable ledger, cash collection, commission, and settlement slices.

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
