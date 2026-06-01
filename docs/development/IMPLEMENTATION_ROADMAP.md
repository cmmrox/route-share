# RouteShareApp Implementation Roadmap

## Purpose

This file tracks the long-term implementation roadmap. It is more stable than `DEVELOPMENT_STATUS.md` and should be updated when phases/tasks are added, completed, deferred, or changed.

## Status Values

- `NOT_STARTED`
- `IN_PROGRESS`
- `PARTIALLY_COMPLETED`
- `COMPLETED`
- `BLOCKED`
- `DEFERRED`

---

## Phase 00 — Project Architecture and File Structure

Status: `COMPLETED`

Tracking Setup: `COMPLETED`

Goal: Create a clean, scalable repository structure before application code is written.

Tasks:

- [x] Create/verify root application structure.
- [x] Create `apps/api` for Spring Boot backend.
- [ ] Create `apps/passenger-mobile` for passenger Expo app.
- [ ] Create `apps/driver-mobile` for driver Expo app.
- [ ] Create `apps/admin-web` for Next.js admin web.
- [ ] Create `packages/api-contracts` for generated/shared OpenAPI clients later.
- [ ] Create `packages/shared-types` if needed for shared TypeScript contracts.
- [x] Create `infra/` folders for local infrastructure.
- [x] Create `scripts/` for developer scripts.
- [x] Create/update root `README.md` with project layout.
- [x] Ensure docs are organized under `docs/`.

Deliverable:

- Clean repository skeleton exists.
- New developers can understand the layout from `README.md`.

---

## Phase 01 — Local Development Environment

Status: `COMPLETED`

Goal: Make all required local infrastructure runnable consistently.

Tasks:

- [x] Create Docker Compose setup.
- [x] Add PostgreSQL 16+ with PostGIS.
- [x] Add Keycloak with `routeshare` realm import.
- [x] Add Redis.
- [x] Add Redpanda/Kafka-compatible broker.
- [x] Add MinIO for object storage.
- [x] Add `.env.example` with non-secret defaults.
- [ ] Add startup scripts:
  - `scripts/dev-up.sh`
  - `scripts/dev-down.sh`
  - `scripts/dev-logs.sh`
- [x] Add health check documentation.

Deliverable:

- Local infrastructure can start with one command.
- No secrets are committed.

---

## Phase 02 — Backend Modular Monolith Foundation

Status: `COMPLETED`

Goal: Build the Spring Boot backend foundation with security, migrations, API docs, tests, and clean module boundaries.

Tasks:

- [x] Scaffold Spring Boot 3 / Java 21 app in `apps/api`.
- [x] Add Maven/Gradle configuration.
- [x] Add module-oriented packages.
- [x] Add Flyway.
- [x] Add PostgreSQL/PostGIS connection.
- [ ] Add first migrations:
  - `V001__create_extensions.sql`
  - `V002__create_module_schemas.sql`
  - `V003__create_audit_and_idempotency_tables.sql`
- [x] Add Spring Security OAuth2 Resource Server.
- [x] Add Keycloak JWT role converter.
- [x] Add common API error model.
- [x] Add validation framework.
- [ ] Add structured logging conventions.
- [x] Add springdoc/OpenAPI endpoint.
- [ ] Add Testcontainers integration tests.
- [x] Add health endpoint.

Deliverable:

- Backend starts locally.
- Migrations run.
- PostGIS is available.
- Security rejects invalid JWTs.
- OpenAPI docs are served.

---

## Phase 03 — Identity, Passenger, Driver, KYC, Vehicle

Refactor checkpoint: Backend persistence layering/JPA refactor completed on 2026-06-01. Services contain business logic only; repositories live under infrastructure and use Spring Data JPA/JpaRepository; JdbcTemplate removed from main sources; Spotless/google-java-format and architecture tests are in place.

Status: `PARTIALLY_COMPLETED`

Goal: Implement core identity projection and profile management.

Tasks:

- [x] Implement `identity.app_user` sync/projection.
- [x] Implement `GET /api/v1/auth/me`.
- [x] Implement passenger profile APIs.
- [x] Implement saved places APIs.
- [x] Implement trusted contacts APIs.
- [x] Implement driver application APIs.
- [x] Implement driver KYC document metadata APIs.
- [x] Implement vehicle APIs.
- [x] Implement vehicle document metadata APIs.
- [x] Implement admin verification APIs.

Deliverable:

- Authenticated users can have passenger and/or driver profiles.
- Admin can approve/reject driver and vehicle verification.

---

## Phase 04 — Route Publishing and Matching

Status: `COMPLETED`

Goal: Build the RouteShareApp product core: planned route publishing and passenger route matching.

Tasks:

- [x] Implement route template creation.
- [x] Implement route geometry storage.
- [x] Implement route schedule rules.
- [x] Implement route occurrence generation.
- [x] Implement route publish/cancel lifecycle.
- [x] Implement route H3/bucket indexing strategy.
- [x] Implement passenger ride search.
- [x] Implement broad candidate filtering.
- [x] Implement PostGIS exact overlap scoring foundation.
- [x] Implement match result ranking and explanation foundation.

Deliverable:

- Driver can publish a route.
- Passenger can search and receive route-sharing matches.

---

## Phase 05 — Booking, Trip, Fare, Payment, Settlement

Status: `PARTIALLY_COMPLETED`

Goal: Implement the transactional core for ride sharing.

Tasks:

- [x] Implement booking creation with idempotency.
- [x] Store matched pickup/drop route fractions on bookings.
- [x] Move booking inventory reservation from route plans to route occurrences.
- [x] Implement seat reservation and no-overbooking guarantees for route occurrences.
- [ ] Implement booking status history.
- [ ] Implement manual booking approve/decline if route requires it.
- [x] Implement trip start/complete/cancel state machine.
- [ ] Implement passenger boarded/no-show/drop-off state machine.
- [x] Implement fare estimate.
- [ ] Implement immutable fare ledger.
- [ ] Implement payment intent abstraction.
- [ ] Implement cash collection records.
- [ ] Implement driver earnings ledger.
- [ ] Implement platform commission ledger.
- [ ] Implement settlement balance.

Deliverable:

- Booking and trip lifecycle works safely with fare/payment/settlement records.

---

## Phase 06 — Realtime Location

Status: `NOT_STARTED`

Goal: Add live driver/passenger/admin tracking without overloading PostgreSQL.

Tasks:

- [ ] Implement location update ingestion API.
- [ ] Validate freshness, accuracy, speed, and impossible jumps.
- [ ] Store latest active trip location in Redis.
- [ ] Persist selected/auditable samples to PostgreSQL.
- [ ] Publish location events to Redpanda/Kafka.
- [ ] Implement WebSocket/STOMP updates.
- [ ] Implement passenger live trip state.
- [ ] Implement admin live trip feed.

Deliverable:

- Passenger/admin can track active trips live.

---

## Phase 07 — Passenger Mobile App

Status: `NOT_STARTED`

Goal: Build passenger app against stable backend APIs.

Tasks:

- [ ] Scaffold Expo React Native app.
- [ ] Add Keycloak login with PKCE.
- [ ] Add secure token storage.
- [ ] Add profile flow.
- [x] Add saved places/trusted contacts.
- [ ] Add ride search UI.
- [ ] Add booking flow.
- [ ] Add active trip tracking UI.
- [ ] Add SOS/share trip.
- [ ] Add ratings/support.

---

## Phase 08 — Driver Mobile App

Status: `NOT_STARTED`

Goal: Build driver app against stable backend APIs.

Tasks:

- [ ] Scaffold Expo React Native app.
- [ ] Add Keycloak login with PKCE.
- [ ] Add driver application/KYC flow.
- [ ] Add vehicle registration flow.
- [ ] Add route creation/publish flow.
- [ ] Add booking request flow.
- [ ] Add trip operation flow.
- [ ] Add background/foreground location updates.
- [ ] Add earnings/payout profile screens.

---

## Phase 09 — Admin Web App

Status: `NOT_STARTED`

Goal: Build admin operations web app.

Tasks:

- [ ] Scaffold Next.js admin app.
- [ ] Add Keycloak login and admin role guards.
- [ ] Add dashboard.
- [ ] Add user search/suspend/activate.
- [ ] Add driver verification.
- [ ] Add vehicle verification.
- [ ] Add live trips.
- [ ] Add payment/settlement screens.
- [ ] Add support/safety screens.
- [ ] Add reports/audit screens.

---

## Phase 10 — Hardening, Observability, Deployment Readiness

Status: `NOT_STARTED`

Goal: Prepare for reliable staging/production use.

Tasks:

- [ ] Add structured application logs.
- [ ] Add metrics.
- [ ] Add tracing where useful.
- [ ] Add Sentry/error monitoring.
- [ ] Add load tests for location and matching.
- [ ] Add security review.
- [ ] Add deployment docs.
- [ ] Add backup/restore docs.
- [ ] Add operational runbooks.
