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
