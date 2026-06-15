# RouteShareApp Task Log

## Purpose

This file records completed implementation and documentation tasks. Each entry should include date, task, status, files changed, verification, and next step.

---

## 2026-05-31

### Task: Create database architecture Mermaid diagram

Status: `COMPLETED`

Files Created:

- `docs/database/routeshare-database-architecture.mmd`

Verification:

- File saved on Mac project path.
- Verified file size and line count during creation.

Notes:

- Diagram models one PostgreSQL/PostGIS database with multiple module schemas.
- Keycloak is outside this database; RouteShareApp stores only `identity.app_user.keycloak_subject` mapping.

---

### Task: Create OpenAPI/Swagger documents for applications

Status: `COMPLETED`

Files Created:

- `docs/api/README.md`
- `docs/api/passenger-app.openapi.json`
- `docs/api/driver-app.openapi.json`
- `docs/api/admin-web.openapi.json`

Verification:

- Validated JSON with `python3 -m json.tool`.
- File sizes at creation:
  - Passenger API: 55,781 bytes
  - Driver API: 70,143 bytes
  - Admin Web API: 73,834 bytes

Notes:

- Documents use OpenAPI 3.1.
- Authentication uses Keycloak JWT bearer tokens.
- Retry-safe mutations include `Idempotency-Key` where appropriate.

---

### Task: Confirm implementation start order

Status: `COMPLETED`

Decision:

- Start with `00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md`.
- Then `01-LOCAL-DEVELOPMENT-ENVIRONMENT.md`.
- Then `02-BACKEND-MODULAR-MONOLITH-FOUNDATION.md`.

Notes:

- Backend feature work should not start before project structure and local infrastructure are established.

---

### Task: Capture clean-code and maintainability requirement

Status: `COMPLETED`

Requirement:

- Code must follow industry standards: clean, maintainable, human-readable, SOLID, layered, reusable, properly logged, and commented where helpful.

Notes:

- Requirement is documented in `QUALITY_STANDARDS.md`.

---

### Task: Create development tracking system

Status: `COMPLETED`

Files To Create:

- `docs/development/DEVELOPMENT_STATUS.md`
- `docs/development/IMPLEMENTATION_ROADMAP.md`
- `docs/development/TASK_LOG.md`
- `docs/development/DECISION_LOG.md`
- `docs/development/REQUIREMENTS_CHANGE_LOG.md`
- `docs/development/BLOCKERS.md`
- `docs/development/QUALITY_STANDARDS.md`
- `docs/development/SESSION_SUMMARIES/2026-05-31-session-summary.md`

Verification:

- Files copied to `docs/development/`.
- Verified file list with `find docs/development -type f`.
- Verified line counts with `wc -l`: 963 total lines across tracking files.


---

## 2026-06-01

### Task: Backend foundation, hardening, and runtime verification

Status: `COMPLETED`

Files Created/Updated:

- `apps/api/src/main/java/com/routeshare/**` — Spring Boot backend modules for identity, passenger, driver, vehicle, routing, booking, trip, location, pricing, payment, admin, common security/errors/web.
- `apps/api/src/main/resources/db/migration/V001__create_extensions.sql`
- `apps/api/src/main/resources/db/migration/V002__create_module_schemas.sql`
- `apps/api/src/main/resources/db/migration/V003__create_foundation_tables.sql`
- `apps/api/src/test/java/com/routeshare/**` — domain and service tests.
- `infra/docker-compose/docker-compose.yml`
- `.env.example`
- local `.docker/` config directory used by scripts; ignored from Git
- `scripts/dev-up.sh`, `scripts/dev-down.sh`, `scripts/dev-logs.sh`

Implementation Notes:

- Added local infrastructure for PostgreSQL/PostGIS, Redis, Redpanda, Keycloak, and MinIO.
- Added Java 21 Spring Boot modular monolith foundation.
- Implemented JWT identity projection and `GET /api/v1/auth/me`.
- Implemented passenger profile, driver application/profile, vehicle, admin driver review, pricing, route publishing, booking, trip transition, location update, and payment intent foundation endpoints.
- Hardened Keycloak role conversion to only trust realm roles and `api-monolith` resource roles.
- Replaced fragile vehicle creation with deterministic `INSERT ... RETURNING`.
- Replaced route coordinate arrays with explicit coordinate DTO validation.
- Tightened route publishing to approved drivers and approved owned vehicles.
- Moved route, booking, payment, location, and trip business logic out of controllers into application services.
- Prevented client-controlled payment amount/currency; payment amount is now derived server-side from booking fare and currency defaults to `LKR`.
- Fixed Spring Boot runtime constructor wiring for services with secondary test constructors.

Verification:

- Maven tests: `BUILD SUCCESS`; `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`.
- API runtime health: `GET /actuator/health` returned HTTP 200 and `{"status":"UP"}`.
- Docker infrastructure: compose services running; PostgreSQL container healthy.
- Database: Flyway migrations count `3`; app table count `13`; PostGIS enabled check `t`.

Next Step:

- Continue full backend completion with document/KYC flows, saved places/trusted contacts, vehicle review, route matching/search, richer booking/trip/payment state, realtime websocket/event outbox, admin APIs, and integration/security tests.


---

### Task: Fix independent review blockers after backend hardening

Status: `COMPLETED`

Files Updated:

- `apps/api/src/main/java/com/routeshare/booking/application/BookingService.java`
- `apps/api/src/main/java/com/routeshare/booking/api/dto/BookingRequest.java`
- `apps/api/src/main/java/com/routeshare/location/application/LocationService.java`
- `apps/api/src/main/java/com/routeshare/location/api/LocationController.java`
- `apps/api/src/main/java/com/routeshare/location/api/dto/LocationUpdateRequest.java`
- `apps/api/src/main/java/com/routeshare/payment/application/PaymentService.java`
- `apps/api/src/main/java/com/routeshare/pricing/domain/FareCalculator.java`
- `apps/api/src/main/java/com/routeshare/pricing/api/PricingController.java`
- `apps/api/src/main/java/com/routeshare/routing/application/RouteService.java`
- `apps/api/src/main/java/com/routeshare/trip/application/TripService.java`
- `apps/api/src/main/java/com/routeshare/identity/infrastructure/AppUserRepository.java`
- `apps/api/src/main/resources/db/migration/V004__add_backend_hardening_constraints.sql`

Implementation Notes:

- Booking now computes and persists `fare_estimate` from route length using the shared fare calculator.
- Payment intent creation remains server-derived and now reuses an existing active intent where present.
- Added a database partial unique index to prevent multiple active payment intents per booking.
- Added a positive booking fare estimate database constraint.
- Route publishing now rejects past/current departure times and rejects requested seats above approved vehicle capacity.
- Location update authorization now checks that the trip belongs to the driver profile and is in an active trip status.
- Trip transition now uses a transaction and row lock for status transition.
- Local `identity.app_user.local_status` is enforced after token projection.
- DTO coordinate/location/booking fields were formatted and strengthened with wrapper types and validation annotations.

Verification:

- Maven tests: `BUILD SUCCESS`; `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`.
- Runtime API health: `GET /actuator/health` returned HTTP 200 and `{"status":"UP"}`.
- Flyway latest version: `004`.
- Flyway migration count: `4`.

Next Step:

- Add integration/security tests and continue product-complete backend features: KYC/document flows, saved places/trusted contacts, vehicle verification, route matching/search, richer booking/trip/payment workflows, realtime/event outbox, and admin APIs.


---

### Task: Backend JPA/layering refactor and formatting enforcement

Status: `COMPLETED`

Files Updated:

- `apps/api/pom.xml`
- `apps/api/src/main/java/com/routeshare/**/application/*.java`
- `apps/api/src/main/java/com/routeshare/**/infrastructure/*.java`
- `apps/api/src/main/java/com/routeshare/common/config/ClockConfig.java`
- `apps/api/src/test/java/com/routeshare/architecture/PersistenceArchitectureTest.java`
- `apps/api/src/test/java/com/routeshare/**` service tests updated to mock repositories instead of JdbcTemplate.

Implementation Notes:

- Added Spring Data JPA and Lombok-based persistence model.
- Refactored service layer so application services contain business logic and call repositories instead of embedding SQL/database APIs.
- Removed JdbcTemplate usage from main Java sources.
- Kept native repository queries only inside infrastructure where PostGIS, upsert, insert-returning, or atomic seat reservation requires database-specific SQL.
- Added architecture tests enforcing persistence boundaries.
- Added Spotless/google-java-format and applied formatting.
- Added a `Clock` bean for runtime wiring after constructor-based testability changes.

Verification:

- `./mvnw spotless:apply spotless:check test`: `BUILD SUCCESS`; `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`.
- Spotless result: `106` Java files clean.
- Runtime API health: `GET /actuator/health` returned HTTP 200 and `{"status":"UP"}`.
- Independent architecture review: `passed: true`, no blockers.

Next Step:

- Continue with route search/matching and richer backend workflows after this architecture baseline.


---

### Task: Backend service/impl + facade architecture refactor

Status: `COMPLETED`

Files Created/Updated:

- `docs/architecture/BACKEND-MODULAR-MONOLITH-SERVICE-IMPL-FACADE.md`
- `docs/development/QUALITY_STANDARDS.md`
- `docs/development/DECISION_LOG.md`
- `docs/implementation/00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md`
- `apps/api/pom.xml`
- `apps/api/src/main/java/com/routeshare/common/mapper/RouteShareMapperConfig.java`
- `apps/api/src/main/java/com/routeshare/**/controller/**`
- `apps/api/src/main/java/com/routeshare/**/dto/**`
- `apps/api/src/main/java/com/routeshare/**/mapper/**`
- `apps/api/src/main/java/com/routeshare/**/service/**`
- `apps/api/src/main/java/com/routeshare/**/facade/**`
- `apps/api/src/main/java/com/routeshare/**/entity/**`
- `apps/api/src/main/java/com/routeshare/**/repository/**`
- `apps/api/src/test/java/com/routeshare/architecture/PersistenceArchitectureTest.java`
- `apps/api/src/test/java/com/routeshare/**` service tests updated for facades and MapStruct mappers.

Implementation Notes:

- Accepted the learner-friendly Spring Boot modular monolith structure instead of `port/in` and `port/out` packages.
- Standardized implemented modules around `controller`, `dto`, `mapper`, `service`, `service/impl`, `facade`, `facade/impl`, `domain`, `entity`, and `repository` packages.
- Added narrow module facades for cross-module communication so future microservice extraction can be done by changing facade implementations.
- Refactored cross-module service dependencies to use facades instead of another module's repository/entity/impl internals.
- Added MapStruct and shared `RouteShareMapperConfig` for mapper consistency.
- Expanded architecture tests to enforce the new structure and boundaries.
- Updated architecture, quality standards, implementation structure, and decision log documents.

Verification:

- Command: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw spotless:apply test -q`.
- Result: `BUILD SUCCESS`; `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`.
- Architecture tests passed.
- Runtime health was not re-run because Docker is unavailable in the current Mac shell; previous backend foundation runtime health was green.

Next Step:

- Continue Phase 04 backend implementation with route search/matching using the approved service/impl + facade architecture.


---

### Task: Enable Java 21 virtual threads for backend performance

Status: `COMPLETED`

Files Updated:

- `apps/api/src/main/resources/application.yml`
- `apps/api/src/test/java/com/routeshare/architecture/VirtualThreadConfigurationTest.java`
- `docs/architecture/BACKEND-MODULAR-MONOLITH-SERVICE-IMPL-FACADE.md`
- `docs/development/QUALITY_STANDARDS.md`
- `docs/development/DECISION_LOG.md`
- `docs/development/DEVELOPMENT_STATUS.md`

Implementation Notes:

- Enabled Spring Boot virtual threads with `spring.threads.virtual.enabled=true`.
- Added bounded HikariCP settings so virtual-thread concurrency does not create unbounded database pressure.
- Added an architecture test to ensure virtual threads remain enabled and the database pool remains bounded.
- Documented the virtual thread policy and operational caution that database connections remain the limiting resource.

Verification:

- Command: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw spotless:apply test -q`.
- Result: `BUILD SUCCESS`; virtual thread configuration test passed.

Next Step:

- Continue backend feature implementation with route search/matching and keep future async/event code on Spring-managed execution.


---

### Task: Implement Phase 04 route search and matching foundation

Status: `COMPLETED`

Files Created/Updated:

- `apps/api/src/main/java/com/routeshare/routing/controller/RouteController.java`
- `apps/api/src/main/java/com/routeshare/routing/service/RouteService.java`
- `apps/api/src/main/java/com/routeshare/routing/service/impl/RouteServiceImpl.java`
- `apps/api/src/main/java/com/routeshare/routing/repository/RoutePlanRepository.java`
- `apps/api/src/main/java/com/routeshare/routing/domain/RouteMatchCandidate.java`
- `apps/api/src/main/java/com/routeshare/routing/domain/RouteMatchScore.java`
- `apps/api/src/main/java/com/routeshare/routing/domain/RouteMatchScorer.java`
- `apps/api/src/main/java/com/routeshare/routing/dto/request/RouteSearchRequest.java`
- `apps/api/src/main/java/com/routeshare/routing/dto/response/RouteSearchResponse.java`
- `apps/api/src/test/java/com/routeshare/routing/domain/RouteMatchScorerTest.java`

Implementation Notes:

- Added authenticated `POST /api/v1/routes/search` for passenger route search.
- Added request validation for pickup/drop-off coordinates, future requested departure time, seat count, optional search radii, time window, and result limit.
- Added PostGIS-backed candidate filtering for published routes by departure window, available seats, pickup/drop proximity, and same-direction route fraction order.
- Added exact overlap distance calculation with `ST_LineLocatePoint`, `ST_LineSubstring`, and geography distance/length measurements.
- Added route match scoring with weighted overlap, pickup proximity, drop-off proximity, and UI-facing explanation text.

Verification:

- TDD red check: `RouteMatchScorerTest` first failed because `RouteMatchScorer` did not exist.
- `./mvnw spotless:apply spotless:check test -q` passed.
- Full Maven tests passed: `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`.
- Runtime health before final docs update: `GET /actuator/health -> 200 {"status":"UP"}`.
- PostgreSQL/PostGIS smoke query verified same-direction candidate search and overlap calculation inside a rolled-back transaction.
- Docker infrastructure check: Postgres healthy; Flyway latest version `005` successful.

Next Step:

- Continue Phase 04 with route schedule rules, route occurrence generation, H3/bucket indexing, route matching integration tests with Testcontainers/PostGIS, and performance-oriented query plan checks.


---

### Task: Complete Phase 04 route publishing and matching

Status: `COMPLETED`

Files Created/Updated:

- `apps/api/src/main/resources/db/migration/V006__add_route_schedule_occurrence_and_bucket_cells.sql`
- `apps/api/src/main/java/com/routeshare/routing/domain/RouteSchedulePolicy.java`
- `apps/api/src/main/java/com/routeshare/routing/domain/RouteBucketCellGenerator.java`
- `apps/api/src/main/java/com/routeshare/routing/entity/RouteScheduleRuleEntity.java`
- `apps/api/src/main/java/com/routeshare/routing/entity/RouteOccurrenceEntity.java`
- `apps/api/src/main/java/com/routeshare/routing/entity/RouteBucketCellEntity.java`
- `apps/api/src/main/java/com/routeshare/routing/repository/RouteScheduleRuleRepository.java`
- `apps/api/src/main/java/com/routeshare/routing/repository/RouteOccurrenceRepository.java`
- `apps/api/src/main/java/com/routeshare/routing/repository/RouteBucketCellRepository.java`
- `apps/api/src/main/java/com/routeshare/routing/repository/RoutePlanRepository.java`
- `apps/api/src/main/java/com/routeshare/routing/service/impl/RouteServiceImpl.java`
- `apps/api/src/test/java/com/routeshare/routing/domain/RouteSchedulePolicyTest.java`
- `apps/api/src/test/java/com/routeshare/routing/domain/RouteBucketCellGeneratorTest.java`

Implementation Notes:

- Added route schedule rules for the MVP one-time route publishing flow.
- Added concrete route occurrence generation at publish time.
- Added route bucket-cell indexing foundation as an H3-compatible abstraction path without requiring the PostgreSQL H3 extension yet.
- Integrated bucket-cell prefiltering into route search before exact PostGIS proximity/direction/overlap checks.
- Kept external maps/directions providers out of this backend slice; route coordinates are accepted from API clients and matched with PostGIS.

Verification:

- TDD red checks were run for schedule policy and bucket-cell generation before implementation.
- `./mvnw spotless:apply spotless:check test -q` passed.
- Full Maven tests passed: `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`.
- Spring Boot runtime restarted and health verified: `GET /actuator/health -> 200 {"status":"UP"}`.
- Flyway latest version: `006`, success `true`.
- New tables verified with `to_regclass`: `routing.route_schedule_rule`, `routing.route_occurrence`, `routing.route_bucket_cell`.
- PostGIS smoke query with route occurrence and bucket-cell prefilter returned one same-direction candidate and overlap `94351.60m`; transaction rolled back.

Third-Party Configuration:

- No Google Maps, Firebase/FCM, or other third-party key was required to finish Phase 04 backend foundation.
- Google Maps Platform key will be needed later for app-side map display/place search/directions polyline UX.
- Firebase/FCM setup will be needed later for push notifications.

Next Step:

- Start Phase 05 with booking/trip/fare/payment lifecycle hardening. First recommended slice: reserve seats against `route_occurrence`, add booking idempotency, store matched pickup/drop fractions from search into booking, and add booking status history.


## 2026-06-01 20:38 +0530 — Phase 05 booking occurrence inventory slice

Files changed:

- `apps/api/src/main/resources/db/migration/V007__move_booking_inventory_to_route_occurrences.sql`
- `apps/api/src/main/java/com/routeshare/booking/dto/request/BookingRequest.java`
- `apps/api/src/main/java/com/routeshare/booking/entity/BookingEntity.java`
- `apps/api/src/main/java/com/routeshare/booking/repository/BookingRepository.java`
- `apps/api/src/main/java/com/routeshare/booking/service/impl/BookingServiceImpl.java`
- `apps/api/src/main/java/com/routeshare/routing/facade/RouteReservation.java`
- `apps/api/src/main/java/com/routeshare/routing/facade/RoutingFacade.java`
- `apps/api/src/main/java/com/routeshare/routing/facade/impl/RoutingFacadeImpl.java`
- `apps/api/src/main/java/com/routeshare/routing/repository/RouteOccurrenceRepository.java`
- `apps/api/src/main/java/com/routeshare/routing/repository/RoutePlanRepository.java`
- `apps/api/src/main/java/com/routeshare/routing/dto/response/RouteSearchResponse.java`
- `apps/api/src/test/java/com/routeshare/booking/service/impl/BookingServiceTest.java`

Implemented:

- Wrote a failing booking service test first for booking against a route occurrence and storing matched fractions.
- Added `RouteReservation` and moved booking seat reservation through `routing.route_occurrence`.
- Added booking columns for `route_occurrence_id`, `pickup_route_fraction`, and `dropoff_route_fraction`.
- Updated route search response/query to expose occurrence id and matched fractions needed by the booking request.
- Fare estimate now uses the matched segment distance from pickup/drop route fractions instead of always charging the full route length.

Verification:

- RED: `./mvnw -q -Dtest=BookingServiceTest test` failed because `RouteReservation` did not exist yet.
- GREEN: `./mvnw -q -Dtest=BookingServiceTest test` passed.
- Full backend: `./mvnw spotless:apply spotless:check test -q` passed.
- Runtime health: `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}`.
- Flyway: latest migration version `007`, success `true`.
- DB columns verified on `booking.booking`: `route_occurrence_id`, `pickup_route_fraction`, `dropoff_route_fraction`.

Next step:

- Continue Phase 05 with booking status history and explicit idempotency-key handling.


## 2026-06-01 21:09 +0530 — Phase 05 booking status history foundation

Files changed:

- `apps/api/src/main/resources/db/migration/V008__add_booking_status_history.sql`
- `apps/api/src/main/java/com/routeshare/booking/entity/BookingStatusHistoryEntity.java`
- `apps/api/src/main/java/com/routeshare/booking/repository/BookingStatusHistoryRepository.java`
- `apps/api/src/main/java/com/routeshare/booking/service/impl/BookingServiceImpl.java`
- `apps/api/src/test/java/com/routeshare/booking/service/impl/BookingServiceTest.java`

Implemented:

- Added immutable booking status history table with `from_status`, `to_status`, changed-by user, reason, and timestamp.
- Booking creation now records initial `CONFIRMED` status history after successful route-occurrence seat reservation and booking insert.
- Added repository and entity under the booking module, keeping service code free of SQL.

Verification:

- RED: `./mvnw -q -Dtest=BookingServiceTest test` failed because `BookingStatusHistoryRepository` did not exist.
- GREEN: `./mvnw -q -Dtest=BookingServiceTest test` passed after implementation.
- Full backend: `./mvnw spotless:apply spotless:check test -q` passed with Java 21.
- Runtime health: `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}`.
- Flyway: latest migration version `008`, success `true`.
- DB table verified: `booking.booking_status_history` with expected status/audit columns.

Next step:

- Continue Phase 05 with explicit HTTP `Idempotency-Key` handling backed by `common.idempotency_key`.


## 2026-06-01 21:35 +0530 — Phase 05 booking idempotency key slice

Files changed:

- `apps/api/src/main/java/com/routeshare/common/entity/IdempotencyKeyEntity.java`
- `apps/api/src/main/java/com/routeshare/common/repository/IdempotencyKeyRepository.java`
- `apps/api/src/main/java/com/routeshare/booking/controller/BookingController.java`
- `apps/api/src/main/java/com/routeshare/booking/service/BookingService.java`
- `apps/api/src/main/java/com/routeshare/booking/service/impl/BookingServiceImpl.java`
- `apps/api/src/test/java/com/routeshare/booking/service/impl/BookingServiceTest.java`

Implemented:

- Wrote failing booking service tests first for duplicate `Idempotency-Key` replay and successful response persistence.
- Added `common.idempotency_key` JPA entity/repository wrapper around the existing Flyway table.
- `POST /api/v1/bookings` now requires the `Idempotency-Key` header.
- Booking creation reserves an idempotency row for operation `booking:create`, hashes the request body, stores the successful JSON response, and replays matching completed responses without reserving seats or inserting a second booking.
- Reusing the same key with a different request body now fails explicitly.

Verification:

- RED: `./mvnw -q -Dtest=BookingServiceTest test` failed because `IdempotencyKeyRepository` did not exist.
- GREEN: `./mvnw -q -Dtest=BookingServiceTest test` passed after implementation.
- Full backend: `./mvnw spotless:apply spotless:check test -q` passed with Java 21.
- Runtime health: `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}`.
- Flyway: latest migration version `008`, success `true`.
- DB table verified: `common.idempotency_key`.

Next step:

- Continue Phase 05 with booking cancel/reject/complete transitions writing status history, then passenger boarded/no-show/drop-off route-occurrence-aware trip states.


## 2026-06-01 21:51 +0530 — Phase 05 booking status-transition slice

Files changed:

- `apps/api/src/main/java/com/routeshare/booking/dto/request/BookingStatusTransitionRequest.java`
- `apps/api/src/main/java/com/routeshare/booking/controller/BookingController.java`
- `apps/api/src/main/java/com/routeshare/booking/service/BookingService.java`
- `apps/api/src/main/java/com/routeshare/booking/service/impl/BookingServiceImpl.java`
- `apps/api/src/main/java/com/routeshare/booking/repository/BookingRepository.java`
- `apps/api/src/main/java/com/routeshare/booking/repository/BookingStatusHistoryRepository.java`
- `apps/api/src/test/java/com/routeshare/booking/service/impl/BookingServiceTest.java`

Implemented:

- Wrote failing booking service tests first for cancelling a confirmed booking and for rejecting an invalid terminal-state transition.
- Added `PATCH /api/v1/bookings/{bookingId}/status` with `BookingStatusTransitionRequest`.
- Added booking status transition validation for `REQUESTED -> CONFIRMED/REJECTED/CANCELLED`, `CONFIRMED -> CANCELLED/COMPLETED`, and terminal `CANCELLED/REJECTED/COMPLETED` states.
- Locked the passenger-owned booking row before status transition.
- Updated booking status and wrote a `booking.booking_status_history` row in the same transaction.

Verification:

- RED: `./mvnw -q -Dtest=BookingServiceTest test` failed because `BookingStatusTransitionRequest` did not exist.
- GREEN: `./mvnw -q -Dtest=BookingServiceTest test` passed after implementation.
- Full backend: `./mvnw spotless:apply spotless:check test -q` passed with Java 21.
- Runtime health: `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}` after restarting the API with the new code.
- Flyway: latest migration version `008`, success `true`.
- DB table verified: `booking.booking_status_history`.

Next step:

- Continue Phase 05 with route-occurrence-aware passenger trip states: boarded, no-show, and drop-off.

## 2026-06-01 22:12 +0530 — Phase 05 passenger trip-state slice

Implemented the route-occurrence-aware passenger trip-state slice.

Changes:
- Added `trip.route_occurrence_id` and `trip.passenger_trip_state` via Flyway V009.
- Added passenger trip statuses: `WAITING_PICKUP`, `BOARDED`, `NO_SHOW`, and `DROPPED_OFF`.
- Added passenger state transition rules: waiting pickup may become boarded or no-show; boarded may become dropped off; no-show and dropped-off are terminal.
- Added `PATCH /api/v1/trips/{tripId}/passengers/{bookingId}/state` for driver/admin passenger state updates.
- Passenger state rows are created only for confirmed bookings that share the trip route plan and route occurrence.
- Added domain and service tests for boarding, no-show rejection after boarding, drop-off flow, and terminal-state protection.

Verification:
- RED: `PassengerTripStateMachineTest,TripServiceImplTest` initially failed because passenger trip state classes did not exist.
- GREEN: targeted `PassengerTripStateMachineTest,TripServiceImplTest` passed.
- Full backend: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw spotless:apply spotless:check test -q` passed.
- Runtime health: `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}`.
- Flyway latest version: `009`, success `true`.
- Verified `trip.passenger_trip_state` exists and `trip.trip.route_occurrence_id` exists.

Next:
- Continue Phase 05 with immutable fare ledger and payment capture/void/refund lifecycle foundation.

## 2026-06-01 22:24 +0530 — Phase 05 immutable fare ledger slice

Implemented the immutable fare ledger foundation for booking payment intent creation.

Changes:
- Added `payment.fare_ledger_entry` via Flyway V010.
- Added `FareLedgerEntryEntity` and `FareLedgerRepository`.
- Payment intent creation now records a `BOOKING_FARE_ESTIMATE` ledger row with booking amount and currency before creating or replaying an active intent.
- Ledger insertion is idempotent per `(booking_id, entry_type)` using `ON CONFLICT DO NOTHING` so repeated intent calls do not duplicate fare rows.
- Added TDD coverage for ledger recording on new and replayed active payment intents.

Verification:
- RED: `PaymentServiceTest` failed because `FareLedgerRepository` did not exist.
- GREEN: targeted `PaymentServiceTest` passed.
- Full backend: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw spotless:apply spotless:check test -q` passed.
- Runtime health: `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}`.
- Flyway latest version: `010`, success `true`.
- Verified `payment.fare_ledger_entry` exists.

Next:
- Continue Phase 05 with payment capture/void/refund transitions, then cash collection, earnings, commission, and settlement ledgers.


## 2026-06-01 22:49 +0530 — Requirements/design/API contract audit and roadmap hardening

Completed a documentation and API-contract audit against the business requirement PDF, passenger app designs, driver app designs, existing OpenAPI files, implementation roadmap, and current backend source snapshot.

Changes:
- Expanded `docs/api/passenger-app.openapi.json` from 19 to 36 paths to cover missing passenger product flows: app config, passenger verification uploads, payment methods/intents, receipts, trip history, early drop-off, managed trip sharing, notification preferences/push registration/read state, and support ticket messages.
- Expanded `docs/api/driver-app.openapi.json` from 29 to 48 paths to cover missing driver product flows: KYC identity/licence, document/vehicle document listing and submit, route share links, recurring route CRUD, pre-trip checklist, arrived pickup, cash collection, fare adjustment, earnings transactions, payout read, ratings, driver SOS, notifications, push, and support messages.
- Expanded `docs/api/admin-web.openapi.json` from 26 to 49 paths to cover missing admin operations: user detail/status history/roles, vehicle review list/detail, private document preview, booking detail/history, trip cancel/location trail, fare policy, payment events/void, cash collections, finance adjustments, support ticket handling, SOS detail, broadcasts, and report export.
- Added `docs/api/API_GAP_ANALYSIS.md` to record which APIs are product contracts versus backend-implemented APIs.
- Updated `docs/api/README.md` with the 2026-06-01 API contract audit note.
- Updated `docs/development/IMPLEMENTATION_ROADMAP.md` to add open API contract gates before passenger, driver, and admin app implementation and to correct placeholder folder/dev-script status.
- Added `Blocker 006` in `docs/development/BLOCKERS.md` for API contract/backend reconciliation before mobile/admin implementation.

Verification:
- Validated all three OpenAPI JSON files with `python3 -m json.tool`.
- Verified updated OpenAPI path counts:
  - Passenger: 36 paths.
  - Driver: 48 paths.
  - Admin: 49 paths.
- Verified roadmap API contract gates and Blocker 006 are present with `grep`.

Next:
- Reconcile each OpenAPI path with current Spring Boot controllers.
- Decide whether to expose app-specific aliases or update contracts to canonical generic backend resource endpoints.
- Generate typed clients under `packages/api-contracts` after backend coverage is agreed.


## 2026-06-01 23:22 +0530 — API reconciliation first backend alias slice

Implemented the first backend API contract reconciliation slice so passenger/driver clients can start targeting stable app-facing paths for core flows already supported by backend services.

Changes:

- Added `docs/api/API_BACKEND_RECONCILIATION.md`.
- Added passenger ride-search alias controller: `POST /api/v1/passenger/ride-searches`.
- Added passenger booking alias controller: `POST /api/v1/passenger/bookings` and `POST /api/v1/passenger/bookings/{bookingId}/cancel`.
- Added passenger payment alias controller: `POST /api/v1/passenger/payments/intents`.
- Added driver route alias controller: `POST /api/v1/driver/routes`.
- Added driver trip alias controller: `POST /api/v1/driver/trips/{tripId}/start`, `complete`, and passenger `board`, `no-show`, `drop-off`.
- Added controller unit tests proving aliases delegate to existing application services.
- Updated roadmap/status/blocker tracking for Phase 05.5 API reconciliation.

Verification:

- RED: targeted controller tests failed before production alias controllers existed.
- GREEN: targeted controller tests passed after implementation.
- Full backend: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw -q spotless:check test` passed.
- Runtime health: previous API process was restarted and `GET /actuator/health` returned HTTP 200 / `{"status":"UP"}`.

Next:

- Add passenger booking list/detail/current-trip/history projections.
- Add driver route list/detail/cancel and driver trip list/detail projections.
- Implement driver-authorized manual booking approve/decline.


## 2026-06-01 23:43 +0530 — Continue all previous implementation phases before Phase 06

Implemented another Phase 05/05.5 backend API reconciliation slice before starting Phase 06 realtime work.

Implemented APIs:

- `GET /api/v1/passenger/bookings`
- `GET /api/v1/passenger/bookings/{bookingId}`
- `GET /api/v1/passenger/trips/current`
- `GET /api/v1/passenger/trips/history`
- `GET /api/v1/driver/routes`
- `GET /api/v1/driver/routes/{routeId}`
- `POST /api/v1/driver/routes/{routeId}/cancel`
- `GET /api/v1/driver/trips`
- `GET /api/v1/driver/trips/{tripId}`
- `GET /api/v1/driver/trips/{tripId}/booking-requests`
- `POST /api/v1/driver/bookings/{bookingId}/approve`
- `POST /api/v1/driver/bookings/{bookingId}/decline`

Verification:

- Targeted controller tests passed.
- Full backend command passed: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw -q spotless:apply spotless:check test`.
- Runtime health passed after restart: HTTP 200 `{"status":"UP"}`.

Next recommended work before/alongside Phase 06 readiness:

- Payment lifecycle: capture, void, refund, cash collection, earnings, commission, settlements.
- Receipt/final fare endpoints.
- Route share link/QR, pre-trip checklist, arrived pickup, notifications, support, SOS, and remaining admin APIs.


## 2026-06-01 23:52 +0530 — Payment lifecycle continuation before Phase 06

Continued previous backend implementation phases before starting Phase 06.

Implemented:

- `POST /api/v1/admin/payments/{paymentIntentId}/capture`
- `POST /api/v1/admin/payments/{paymentIntentId}/void`
- `POST /api/v1/admin/payments/{paymentIntentId}/refund`
- `POST /api/v1/driver/bookings/{bookingId}/cash-collected`
- `GET /api/v1/passenger/bookings/{bookingId}/receipt`
- `V011__expand_payment_lifecycle_ledger.sql`

TDD/verification:

- Wrote `PaymentLifecycleServiceTest` first and confirmed it failed because lifecycle DTO/service/repository methods were missing.
- Implemented DTOs, service methods, repository transition/ledger helpers, controller endpoints, and migration.
- Targeted payment lifecycle tests passed.
- Full backend verification passed: `./mvnw -q spotless:apply spotless:check test`.
- Runtime health passed after restart: HTTP 200 `{"status":"UP"}`.

Next concrete pre-Phase-06 options:

1. Driver operational APIs: route share link/QR, pre-trip checklist, arrived-at-pickup, fare adjustment.
2. Admin financial projections: payment list/detail/events and cash collection review.
3. Earnings/settlement read models: driver earnings summary/transactions, platform commission, settlement balance.

## 2026-06-02 00:08 +0530 — Pre-Phase-06 closure: driver ops, admin finance, earnings, contract inventory

Completed remaining practical backend/API work before Phase 06 realtime/location work:

Driver operational APIs:

- `POST /api/v1/driver/routes/{routeId}/share-link`
- `POST /api/v1/driver/trips/{tripId}/pre-trip-checklist`
- `POST /api/v1/driver/trips/{tripId}/arrived-pickup`
- `POST /api/v1/driver/bookings/{bookingId}/fare-adjustment-request`

Admin finance projections:

- `GET /api/v1/admin/payments`
- `GET /api/v1/admin/payments/{paymentIntentId}`
- `GET /api/v1/admin/payments/{paymentIntentId}/events`
- `GET /api/v1/admin/cash-collections`

Driver earnings/read models:

- `GET /api/v1/driver/earnings/summary`
- `GET /api/v1/driver/earnings/transactions`
- Uses ledger-derived gross earnings, 10% MVP platform commission, and settlement balance.

Schema/contracts:

- Added `V012__pre_phase06_operational_finance.sql` for pre-trip checklist, arrived-pickup event, route share link, and fare adjustment ledger entry type.
- Generated lightweight contract inventory in `packages/api-contracts/src/index.ts` from the Passenger/Driver/Admin OpenAPI files.

Verification:

- RED test first: `PrePhase06ControllerContractTest` initially failed because driver earnings controller, fare adjustment DTO, and pre-trip checklist DTO did not exist.
- Targeted `PrePhase06ControllerContractTest` passed after implementation.
- Full backend verification passed: `./mvnw -q spotless:apply spotless:check test`.
- Runtime API smoke passed after restart: `/actuator/health` HTTP 200.
- OpenAPI JSON syntax validation passed for Passenger, Driver, and Admin contracts.
- Backend source route check found implemented controller mappings for share-link, pre-trip-checklist, arrived-pickup, fare-adjustment-request, earnings summary/transactions, admin payments/events, and cash collections.

Status: previous backend/API phases are now closed enough to begin Phase 06. Remaining deeper financial work, such as real provider integration and settlement payouts, is later hardening and not a blocker for Phase 06 realtime foundation.

## 2026-06-02 00:42 +0530 — Phase completion audit, verification, and commit preparation

Completed the requested audit of all phases before Phase 06 and fixed stale incomplete markers that were still visible in the tracking docs.

Audit findings and corrections:

- Phase 00 had a stale unchecked TypeScript workspace/client setup item. Added root `package.json`, `pnpm-workspace.yaml`, `packages/api-contracts/package.json`, and `packages/api-contracts/tsconfig.json`; generated contract inventory is typecheckable.
- Phase 02 had stale unchecked migration/logging/Testcontainers items. Existing migrations are now documented accurately through `V012`; logging conventions are documented in `docs/development/LOGGING_CONVENTIONS.md`; added `FlywayPostgisMigrationIntegrationTest` as Testcontainers migration smoke coverage with automatic skip when Java Testcontainers cannot connect to Docker.
- Phase 03 had a stale partial-completion marker even though the foundation scope was done; marked completed.
- Phase 05 had a stale partial-completion marker with stale unchecked manual approve/decline, cash collection, earnings, commission, and settlement-balance items; updated to `COMPLETED_FOR_PHASE_06_GATE`.
- Phase 05.5 had a stale in-progress marker; updated to `COMPLETED_FOR_PHASE_06_GATE`.
- API reconciliation stale `MISSING` markers for receipt, route share link, pre-trip checklist, arrived pickup, cash collection, fare adjustment, earnings, admin payments, and admin cash collections were updated to implemented/complete-for-gate.
- Remaining non-implemented items were explicitly classified as Phase 06+ or later-phase scope, not pre-Phase-06 blockers.

Verification:

- `pnpm install` completed and `pnpm --filter @routeshare/api-contracts typecheck` passed.
- Backend `./mvnw -q spotless:apply spotless:check test` passed.
- Testcontainers migration smoke test is present; on this Mac Java Testcontainers cannot connect to Docker Desktop's socket and therefore auto-skips under `disabledWithoutDocker = true` during the full suite.
- Runtime restart passed: `/actuator/health` returned HTTP 200 with `{"status":"UP"}`.
- Running database latest migration check passed: `flyway_schema_history` latest version `012`, success `true`.
- Verified key pre-Phase-06 tables exist in the running database: `payment.fare_ledger_entry`, `routing.route_share_link`, and `trip.pre_trip_checklist`.

Verdict: phases 00 through 05.5 are complete for the Phase 06 gate and committed.

## 2026-06-02 00:45 +0530 — Pre-Phase-06 closure committed

Committed all verified pre-Phase-06 work:

- Commit: `latest commit `feat: complete pre-phase 06 backend gate``
- Working tree: clean immediately after commit.
- Phase 06 may start from this commit.


## 2026-06-02 01:15 +0530 — Phase 06 realtime location foundation completed

Implemented Phase 06 backend foundation:

- Driver active-trip location ingestion endpoint: `POST /api/v1/driver/trips/{tripId}/location-updates`.
- Location validation for timestamp freshness, accuracy, speed, and impossible jumps against latest cached snapshot.
- Redis latest-location cache with 30 second TTL.
- Auditable PostgreSQL sample persistence and `location.location_event_outbox` for event-stream handoff.
- WebSocket/STOMP endpoint `/ws` with trip topic `/topic/trips/{tripId}/location` and admin topic `/topic/admin/trips/live`.
- Passenger live trip state endpoint: `GET /api/v1/passenger/trips/{tripId}/live-state`.
- Admin live trip feed endpoint: `GET /api/v1/admin/trips/live`.

Verification recorded after implementation: targeted Phase 06 tests pass, full backend tests pass, runtime health passes, Redis ping passes, and Flyway migration `013` succeeds.


## 2026-06-02 01:45 +0530 — App backend readiness audit before app phases

Reviewed business requirements, design ZIP inventory, app implementation plans, OpenAPI contracts, and implemented Spring Boot controller mappings. Created `docs/api/APP_BACKEND_READINESS_AUDIT.md`. Added `Phase 06.5 — App Backend Readiness Closure` to the roadmap and opened Blocker 007 because full Passenger/Driver/Admin apps are not yet backend-ready end-to-end.

No production code was changed in this audit; documentation/tracking only.


## 2026-06-02 02:35 +0530 — Phase 06.5 backend readiness closure

Implemented all app-facing backend contract paths needed before Phase 07. Added shared app-readiness workflow persistence plus Passenger, Driver, Admin, and App Config controllers for app config, notifications, push registrations, support, SOS, ratings, payment-method placeholders, KYC/document submit compatibility, vehicle detail/update/delete, route publish alias, recurring-route compatibility, payout profile, admin dashboard/users/trips/bookings/fare/commission/settlement/support/SOS/reports/audit endpoints.

Verification: targeted Phase 06.5 contract test passed; full backend `spotless:apply spotless:check test` passed.


## 2026-06-02 03:00 +0530 — Backend test coverage gate and missing test closure

Completed a backend test coverage review before Phase 07. Added JaCoCo coverage enforcement to `apps/api/pom.xml` with an 80% line-coverage gate for measured application logic, excluding generated/boilerplate adapter layers such as DTOs, JPA entities, Spring controllers, repositories, MapStruct mappers, configuration, security wiring, and generated facade glue.

Added missing focused tests for:

- `AppReadinessServiceImplTest` — app config, verification status, support/SOS default statuses, notification preferences, mark-read, share-link payload, payout default, dashboard summaries, and unserializable payload failure.
- `VehicleServiceImplTest` — create/list/get/update/delete/review flows plus driver-profile access denial.
- `RedisLatestLocationCacheTest` — TTL, empty cache lookup, JSON deserialize, JSON serialize with TTL, and Redis read failure wrapping.

Verification:

- `./mvnw verify` passed.
- JaCoCo line coverage passed the 80% gate: `92.9078%` measured line coverage (`131` covered / `10` missed across `20` measured classes).
- Full backend suite: `Tests run: 91, Failures: 0, Errors: 0, Skipped: 1`; skipped test is the Docker/Testcontainers migration smoke test when Docker Desktop Java client is unavailable.

## 2026-06-13 23:34 +0530 — Implementation planning standard documented

Documented the project-wide implementation planning pattern requested for future RouteShareApp development.

Changes:

- Added `docs/development/IMPLEMENTATION_PLANNING_STANDARD.md` as the source of truth for feature-level implementation planning.
- Updated `docs/development/IMPLEMENTATION_ROADMAP.md` to require `docs/development/implementation-tasks/<feature-plan-name>/` for every high-level feature plan.
- Updated `docs/development/QUALITY_STANDARDS.md` with the release-slice planning standard.
- Updated `docs/development/DEVELOPMENT_STATUS.md` with a pointer to the new planning standard.

Rule captured:

- Every high-level feature gets its own implementation-task folder.
- Each task file must be a production-ready release slice with architecture/design notes, API/database/configuration impacts, development steps, QA test cases, verification commands, security/privacy checks, and done criteria.
- Do not split one user-visible production feature so required API wiring, error handling, QA, authorization, database behavior, or release readiness is left to a later task.

Verification:

- Confirmed the new standard file exists.
- Confirmed roadmap, quality standards, and development status reference the planning standard.
- No production application code changed.

## 2026-06-14 00:16 +0530 — Phase 07 Task 01 passenger typed API client and contract reconciliation

Started Passenger Mobile App implementation with Task 01: `01-passenger-api-contract-reconciliation-and-typed-client.md`.

Implemented:

- Added `@routeshare/passenger-mobile` package scripts for lint, typecheck, unit tests, and explicit native blocker commands.
- Added typed passenger API client under `apps/passenger-mobile/src/api/`:
  - `api-client.ts` for base request handling, bearer injection, timeout, JSON parsing, typed HTTP errors, central `ApiResponse<T>` unwrap, and redacted logging.
  - `config.ts` for `EXPO_PUBLIC_API_BASE_URL` / `ROUTESHARE_API_BASE_URL` resolution.
  - `contracts.ts` for generated-compatible passenger endpoint inventory from `@routeshare/api-contracts`.
  - `adapters.ts` for runtime/OpenAPI DTO drift normalization.
  - `modules.ts` for app config, auth, profile, saved places, trusted contacts, ride search, bookings, payments, trips, notifications, support, and safety.
- Added unit tests for envelope unwrap, bearer/public auth behavior, 401/403/409/500 typed errors, timeout, retry behavior, malformed JSON, redaction, base URL config, DTO adapters, idempotency-key booking create, and contract inventory sync.
- Added `docs/api/PASSENGER_MOBILE_CONTRACT_RECONCILIATION.md` covering all passenger operations with `MATCHED`, `RUNTIME_ENVELOPE`, `DTO_MISMATCH`, `READINESS_PLACEHOLDER`, and `DEFERRED_PRODUCTION` vocabulary.
- Updated `docs/api/passenger-app.openapi.json` and `packages/api-contracts/src/index.ts` to include live saved-place/trusted-contact detail endpoints discovered during review.

TDD / verification evidence:

- RED observed: initial `pnpm --filter @routeshare/passenger-mobile test` failed on reused `Response` body and incorrect expected passenger endpoint count.
- GREEN verified: `pnpm --filter @routeshare/passenger-mobile test` passed (`3` files, `23` tests), including embedded card-string redaction regression test.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile lint` passed.

Blocked/deferred verification:

- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` fails with the explicit native scaffold blocker.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` fails with the explicit native scaffold blocker.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` fails with the explicit native scaffold blocker.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` fails with the explicit native scaffold blocker.
- This is recorded as Blocker 008 and should be resolved in Task 02, where the Expo/native scaffold and release pipeline are the actual scope.

Next step:

- Continue with Task 02 — Expo app scaffold, dev tooling, and release pipeline.

## 2026-06-14 01:32 +0530 — Phase 07 Task 02 Expo app scaffold, dev tooling, and release pipeline

Implemented Task 02: `02-expo-app-scaffold-dev-tooling-release-pipeline.md`.

Implemented:

- Added Expo app entry points: `apps/passenger-mobile/index.ts`, `App.tsx`, `app.config.ts`, and `src/app.config.ts` test bridge.
- Added app foundation under `src/application/`: environment profiles, theme, auth Zustand stub, ErrorBoundary, Toast, providers, and navigation root shell.
- Added visible `src/screens/home.screen.tsx` scaffold screen with accessibility labels and no raw `fetch`/backend envelope parsing in screens.
- Added app/dev tooling: ESLint flat config, Prettier config, strict TypeScript aliases, EAS preview config, Detox config, web dependencies, Expo Doctor, local e2e smoke gate, and preview-build config gate.
- Added tests for app config, environment profile selection/production debug safety, and screen architecture guardrails.
- Added `.expo/` ignore rules to `.gitignore`.

TDD / verification evidence:

- RED observed: initial `pnpm --filter @routeshare/passenger-mobile test` failed because `app.config`, environment profiles, and screen files were missing.
- GREEN verified: `pnpm --filter @routeshare/passenger-mobile test` passed (`6` files / `28` tests).
- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `cd apps/passenger-mobile && pnpm run doctor` passed Expo Doctor `21/21` checks.
- `cd apps/passenger-mobile && pnpm exec expo export --platform web --output-dir /tmp/routeshare-passenger-web-export` passed; the exported app rendered the RouteShare Passenger scaffold UI.

Notes:

- Real remote EAS cloud build submission was not triggered by the verification scripts; scripts validate local preview configuration. Run EAS builds when credentials/project IDs are finalized.
- Real simulator/device Detox execution remains release-evidence follow-up once native projects and selected local devices are generated.

Next step:

- Start Task 03 for the first production passenger screen/flow slice.


## 2026-06-14 18:20 +0530 — Phase 07 Task 03 app shell, navigation, state, and offline foundation

Implemented Task 03: `03-app-shell-navigation-state-and-offline-foundation.md`.

Implemented:

- Added `src/application/navigation.ts` with the full typed passenger route map, public/protected route lists, required params for search/result/booking/trip/payment/share/pickup/dropoff flows, and app deep-link prefixes/config.
- Added `src/application/startup-state.ts` with a deterministic startup route guard covering splash, onboarding, missing/expired token, rejected auth/me, incomplete profile, offline cached home, and authenticated home states.
- Added `src/application/preferences.ts` with release-safe defaults and defensive validation/merge logic for onboarding completion, selected home variant, theme preference, and safe dev-only environment preference.
- Added `src/application/network-policy.ts` and `network-state.ts` for offline-aware query/cache/retry policy, safe-vs-unsafe mutation gating, app-state-triggered connectivity checks, and app-wide offline banner support.
- Expanded `auth-store.ts` with token expiry resolution, auth/me status, profile completeness, and session clearing semantics.
- Replaced the single-home root shell with a full app-shell navigator and placeholder route screens so later UI tasks can fill screen bodies without changing route contracts.
- Kept screens thin: no screen-level raw `fetch` or backend envelope parsing.

TDD / verification evidence:

- RED observed: targeted Task 03 tests failed because `startup-state`, `navigation`, and `network-state` modules were missing.
- GREEN verified: targeted Task 03 tests passed (`12` tests).
- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed (`9` files / `40` tests).
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.

Notes:

- This task intentionally provides placeholder shell route screens. Production visual components and source-asset-driven screen composition start in Task 04.
- Real remote EAS submissions and full simulator/device Detox automation remain release-evidence follow-up after project credentials/devices are finalized.

Next step:

- Start Task 04 — design system and reusable screen components from source assets.


## 2026-06-14 19:05 +0530 — Phase 07 Task 04 design system and reusable screen components from assets

Implemented Task 04: `04-design-system-and-screen-components-from-assets.md`.

Implemented:

- Added source-asset-derived design tokens under `apps/passenger-mobile/src/design-system/tokens.ts`: warm background, soft/surface/ink/line palette, terracotta accent, teal secondary, semantic success/warn/danger, match-tier colors, spacing, radius, shadow, and typography scales, including dark palette tokens.
- Added reusable accessible components under `apps/passenger-mobile/src/design-system/components.tsx`: `Screen`, `AppText`, `Button`, `IconButton`, `TextField`, `OtpField`, `Chip`, `Card`, `BottomSheet`, `ListRow`, `StatCard`, `ProgressBar`, `ToastView`, `ConfirmDialog`, `LoadingState`, `EmptyState`, `ErrorState`, `Avatar`, `MatchRing`, `RouteTimeline`, `FareRow`, `PaymentRow`, `SeatPlan`, `SosButton`, `MapBackdrop`, and `MapOverlayCard`.
- Added accessible roles, labels/hints, selected/disabled state support, and 44px minimum touch target guardrails for touchable primitives.
- Replaced the plain Task 03 shell copy with a warm RouteShare branded shell using the new design system.
- Updated the home screen preview to use the deterministic map backdrop, bottom sheet, quick chips, match preview, avatar, route timeline, stats, and SOS/menu controls.
- Added unit/architecture tests for token-source alignment, match-tier mapping, exported components, accessibility guardrails, and shell/home usage of the design system.

TDD / verification evidence:

- RED observed: Task 04 tests initially failed because design-system modules/components did not exist.
- GREEN verified: `pnpm --filter @routeshare/passenger-mobile test` passed (`11` files / `47` tests).
- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.
- Android debug assemble passed with Gradle: `BUILD SUCCESSFUL in 2s`.

Notes:

- The map is a deterministic React Native abstraction/mock suitable for tests and shell previews; full live-map behavior remains for later route-search/live-trip tasks.
- Real EAS cloud submission and full device/simulator Detox automation remain later release-evidence follow-up after credentials/devices are finalized.

Next step:

- Start Task 05 — onboarding/auth Keycloak and OTP experience.


## 2026-06-14 19:15 +0530 — Phase 07 Task 05 onboarding, auth screens, Keycloak PKCE, and OTP experience

Implemented Task 05: `05-onboarding-auth-keycloak-and-otp-experience.md`.

Implemented:

- Added real passenger auth screens:
  - `src/screens/splash.screen.tsx`
  - `src/screens/onboarding.screen.tsx`
  - `src/screens/login.screen.tsx`
  - `src/screens/otp.screen.tsx`
- Wired `Splash`, `Onboarding`, `Login`, `Otp`, and `Home` to real screen components in `src/application/root-shell.tsx`; remaining unfinished routes still use the branded shell placeholder.
- Added `src/features/auth/phone-validation.ts` for Sri Lankan mobile validation, normalization and display formatting.
- Added `src/features/auth/otp-state.ts` covering empty, focused, paste/autofill sanitization, invalid code, resend countdown, ready, throttled, network failure, submit and verified states.
- Added `src/features/auth/auth-session.ts` with Keycloak Authorization Code + PKCE auth URL generation, token exchange, refresh-token exchange and privacy-safe error sanitization.
- Added `src/features/auth/auth-storage.ts` with SecureStore-compatible persistence, restoration and secure logout/query-cache clearing helpers.
- Added `src/features/auth/provider-config.ts` documenting provider capabilities and gating phone OTP behind an explicit environment capability flag.
- Added auth TDD tests for phone validation, OTP state machine, token exchange/refresh, secure logout, privacy-safe auth errors and route/screen architecture.

TDD evidence:

- RED observed: new Task 05 tests initially failed due missing auth feature modules and missing real auth route/screen exports.
- GREEN verified with full passenger app test suite: `15` files / `57` tests passed.

Verification passed:

```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:android
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile build:preview:android
pnpm --filter @routeshare/passenger-mobile build:preview:ios
cd apps/passenger-mobile/android && ./gradlew app:assembleDebug -x lint -x test --configure-on-demand --build-cache -PreactNativeDevServerPort=8082 -PreactNativeArchitectures=arm64-v8a --console=plain
```

Results:

- Unit tests: `15` files / `57` tests passed.
- Android native debug assemble: `BUILD SUCCESSFUL in 2s`.
- iOS/Android e2e commands currently run local scaffold/Detox smoke gates. Full device/simulator automation remains later release evidence once devices are finalized.

Phone OTP dependency note:

- Current implementation does not fake production phone OTP. If `EXPO_PUBLIC_AUTH_PHONE_OTP_SUPPORTED` is not `true`, the phone login UI validates the phone number, shows the provider dependency, and asks the user to continue through configured Keycloak/social methods.

Next step:

- Start Task 06 — profile setup and verification.

## 2026-06-14 — Production external services matrix

- Added `docs/development/PRODUCTION_EXTERNAL_SERVICES.md` after auditing current RouteShareApp docs/config/source references for provider-backed production dependencies.
- Confirmed real production blockers/dependencies for Keycloak, SMS/OTP, maps, push notifications, payment gateway, object storage, email, Sentry, analytics, observability, PostgreSQL/PostGIS, Redis, Redpanda/Kafka, secrets, EAS/app-store distribution, and domain/TLS hosting.
- Clarified implementation policy: provider-backed behavior must be implemented through ports/adapters, local/dev fakes must remain explicitly gated, and production readiness cannot be claimed without real provider integration tests.
- Current unresolved vendor decisions: SMS provider, payment gateway with preauth/capture/refund support, production object storage, production hosting, Firebase/FCM project, Sentry projects, domain/DNS/TLS.

## 2026-06-14 — Provider decisions for public release

User selected the production providers and clarified that RouteShareApp should be built for real public release, not a mock MVP.

Selected providers:

- SMS/OTP: Notify.lk.
- Maps/routing/places: Google Maps Platform.
- Payment gateway: Cybersource.
- Push notifications: Firebase Cloud Messaging.
- Monitoring/error tracking: Sentry.

Updated docs/config:

- Updated `docs/development/PRODUCTION_EXTERNAL_SERVICES.md` with selected providers and release policy.
- Added `docs/development/SELECTED_PROVIDER_IMPLEMENTATION_GUIDE.md` with provider-specific ports, implementation requirements, and credential lists.
- Expanded `.env.example` with selected provider placeholders.
- Updated passenger `app.config.ts` to carry Google Maps and Firebase native config paths/keys from environment and expose provider capability flags through Expo config.

Open credential/config needs remain tracked; real provider adapters must be implemented during the relevant application feature slices before claiming public-release readiness.


## 2026-06-14 — Notify.lk real OTP backend integration

Implemented real backend-owned phone OTP delivery using Notify.lk REST docs from `https://developer.notify.lk/`.

Implemented:

- Added public backend endpoints:
  - `POST /api/v1/auth/otp/request`
  - `POST /api/v1/auth/otp/verify`
- Added `identity.phone_otp_challenge` Flyway migration with hashed OTP storage, status, attempt count, expiry, resend cooldown, and phone format constraint.
- Added `NotifyLkSmsGateway` provider adapter using Notify.lk `/api/v1/send` with `user_id`, `api_key`, `sender_id`, `to`, and `message` parameters.
- Added production guard against sending OTP through `NotifyDEMO` unless `NOTIFY_LK_ALLOW_DEMO_SENDER_FOR_OTP=true` is explicitly set for non-production testing.
- Added backend config placeholders for Notify.lk base URL, user id, API key, sender id, demo override, and OTP message template.
- Updated passenger mobile auth API module and Login/OTP screens to call backend OTP request/verify endpoints when the passenger phone-OTP capability flag is enabled.

Security/release notes:

- Notify.lk credentials remain backend-only; no mobile app secret exposure.
- OTP values are generated server-side, stored only as BCrypt hashes, expire after 5 minutes, and are limited to 5 verification attempts.
- The current Notify.lk account screenshots show `NotifyDEMO`; production/public release needs a RouteShare-approved sender ID before enabling real OTP in production.

Verification passed:

```bash
cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw -q -Dtest=NotifyLkSmsGatewayTest,PhoneOtpServiceImplTest test
cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw -q spotless:apply spotless:check test
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile test
```

Results:

- Backend targeted Notify.lk/OTP tests passed.
- Backend full Spotless/test suite passed; Testcontainers Docker environment warning was emitted and the migration smoke test auto-skipped as designed.
- Passenger mobile typecheck passed.
- Passenger mobile lint passed.
- Passenger mobile tests passed: `15` files / `57` tests.


## 2026-06-15 02:43 +0530 — Phase 07 Task 06 profile/avatar/verification/saved places/trusted contacts

Files changed:

- `apps/passenger-mobile/src/api/adapters.ts`, `modules.ts`, `types.ts` — profile/saved-place/trusted-contact DTO adapters and API calls.
- `apps/passenger-mobile/src/application/navigation.ts`, `root-shell.tsx`, navigation tests — Task 06 routes and screen registration.
- `apps/passenger-mobile/src/features/profile/**` — profile validation, avatar handling, verification copy, body mapping, and unit tests.
- `apps/passenger-mobile/src/screens/profile-setup.screen.tsx`, `account.screen.tsx`, `saved-places.screen.tsx`, `trusted-contacts.screen.tsx`, `verification.screen.tsx` — Task 06 user flows.
- Development tracking docs updated for Task 06 status and next Task 07.

Verification commands run:

```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile test:e2e:android
pnpm --filter @routeshare/passenger-mobile build:preview:ios
pnpm --filter @routeshare/passenger-mobile build:preview:android
```

Result: all passed. Unit suite result: `16` files / `62` tests. iOS/Android e2e and preview commands are local scaffold/config gates; full device/simulator automation and remote EAS submissions remain later release evidence.

Notes:

- No new local implementation blocker remains for Task 06.
- Verification document upload/review and avatar binary storage are intentionally honest readiness/local shells until backend storage/document APIs are available.
- Next task: Task 07 home/search/location/route discovery.

---

## 2026-06-15

### Task: Keycloak-source-of-truth OTP and passenger mobile QA continuation

Status: `COMPLETED_WITH_KNOWN_WARNINGS`

Files Updated:

- `apps/api/src/main/java/com/routeshare/identity/**` — phone OTP service, Keycloak phone identity integration, token subject handling, and OTP DTOs/providers.
- `apps/api/src/main/java/com/routeshare/common/security/**` — phone OTP token authentication support.
- `apps/api/src/main/resources/application.yml` — OTP/Keycloak provider configuration.
- `apps/api/src/main/resources/db/migration/V015__add_phone_otp_challenges.sql` — OTP challenge persistence.
- `apps/api/src/test/java/com/routeshare/identity/**` — focused OTP/Keycloak tests.
- `apps/passenger-mobile/src/features/auth/provider-config.ts` — local/dev phone OTP now enabled by default with explicit staging/production gate.
- `apps/passenger-mobile/src/screens/login.screen.tsx` — supplied-design-aligned mobile-number screen and removed stale provider blocker for local/dev.
- `apps/passenger-mobile/src/screens/otp.screen.tsx` — resend stores new verification id and countdown ticks.
- `apps/passenger-mobile/src/screens/profile-setup.screen.tsx` — first/last/email/referral design alignment.
- `apps/passenger-mobile/src/screens/home.screen.tsx` — destination/frequent-routes design alignment and removed API debug URL.
- `apps/passenger-mobile/src/screens/account.screen.tsx` — account menu/header design alignment.
- `apps/passenger-mobile/src/features/auth/__tests__/auth-architecture.test.ts` — updated auth provider guardrail.

Verification:

- Focused backend tests passed: `mvn -Dtest=KeycloakPhoneVerifiedIdentityServiceTest,PhoneOtpServiceImplTest test -q`.
- Live local smoke verified OTP -> Keycloak user -> `PASSENGER` role -> token subject equals Keycloak user id -> `/api/v1/auth/me` -> `identity.app_user.keycloak_subject` mapping.
- Passenger mobile passed:
  - `pnpm --filter @routeshare/passenger-mobile lint`
  - `pnpm --filter @routeshare/passenger-mobile typecheck`
  - `pnpm --filter @routeshare/passenger-mobile test` — 16 files / 62 tests passed.
- Passenger mobile config smoke passed:
  - `test:e2e:ios`
  - `test:e2e:android`
  - `build:preview:ios`
  - `build:preview:android`
- `pnpm exec expo export --platform ios` bundled successfully from `apps/passenger-mobile/index.ts`.
- Android `expo run:android --port 8081 --variant debug` built and installed successfully on `emulator-5554`.
- Android screenshots verified onboarding and Login rendering; corrected Login no longer shows the red Phone OTP unavailable card.

Known Warnings / Follow-up:

- `expo-doctor` has one warning: native folders exist while `app.config.ts` still contains CNG/prebuild-managed fields; decide whether to commit/sync native projects or return to managed prebuild flow.
- Android dev-client shows a non-fatal Expo CLI websocket warning toast in development; no fatal JS/native crash was observed after relaunch on Metro 8081.
- Later passenger app screens are still placeholder-level and need exact design implementation before claiming full passenger app completion.

Next Step:

- Resolve the Expo CNG/native-folder warning and continue implementing the remaining passenger screens from the supplied designs, starting with Search -> Results -> Ride Detail -> Seat Selection -> Payment.
## 2026-06-15 — Local QA/runtime cleanup, OTP bypass, Keycloak profile sync, avatar picker

- Removed duplicate `routeshare-postgres-alt` usage and normalized local Docker to `routeshare-postgres` on host port `5433` so it does not conflict with the existing Odoo Postgres on `5432`.
- Started and verified local RouteShare services: Postgres, Redis, Keycloak, and MinIO.
- Added local QA-only OTP bypass configuration: `ROUTESHARE_OTP_DEV_BYPASS_ENABLED=true` with static code `000000` in `.env`; `.env.example` keeps it disabled by default.
- Fixed passenger profile save so Keycloak standard user fields are synced from saved profile data: first name, last name, and email. Passenger-specific data such as `photoUrl` remains in RouteShare DB because the current Keycloak realm drops arbitrary custom attributes.
- Replaced the passenger profile image placeholder flow with real Expo image picker wiring and avatar preview.
- Installed Maestro on the Mac and added repeatable emulator QA scripts under `scripts/qa-*.sh` plus flows/reports under `qa/`.
- Verification completed: backend focused tests pass, backend `spotless:check test` exits 0, passenger mobile lint/typecheck/tests pass, and Maestro Android smoke passes on `emulator-5554`.
