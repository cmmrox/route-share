# RouteShareApp Session Summary — 2026-06-01

Updated: 2026-06-01 20:17 +0530

## Summary

Continued the backend implementation after an API-limit interruption. The backend foundation now runs locally against Docker infrastructure, Maven tests pass, and several security/ownership/payment/validation hardening issues from the independent review were addressed.

## Completed

- Verified project state on `macbook-hermes` under `/Users/cmmrox/Personal/Projects/RouteShareApp`.
- Verified local Docker infrastructure for PostgreSQL/PostGIS, Redis, Redpanda, Keycloak, and MinIO.
- Verified Flyway/PostGIS/database table state.
- Verified Spring Boot health endpoint after restart.
- Added/confirmed backend modules under `apps/api`.
- Hardened vehicle creation to use deterministic insert return behavior.
- Hardened payment intent creation so clients cannot control amount/currency.
- Hardened route publish validation using explicit coordinate DTOs and approved driver/vehicle requirements.
- Moved controller-heavy logic for route, booking, payment, location, and trip into application services.
- Added service/domain tests for route and payment hardening.
- Fixed Spring Boot constructor wiring after service extraction.
- Updated development tracking docs.

## Verification

- Backend test command:

```bash
cd /Users/cmmrox/Personal/Projects/RouteShareApp/apps/api
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw test
```

Result:

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Runtime health:

```text
GET http://localhost:8080/actuator/health -> 200 {"status":"UP"}
```

Database checks:

```text
Flyway migrations: 5
App tables: 14
PostGIS enabled: t
```

## Current State

- Backend foundation: verified and running.
- Full backend: still in progress.
- Git: repository exists on branch `main`; initial commit still pending and files are untracked.

## Next Recommended Work

1. Implement route matching/search with PostGIS candidate filtering and scoring.
2. Expand booking/trip/payment lifecycle and idempotency.
3. Add WebSocket/realtime location pipeline and event outbox.
4. Add Spring Boot integration/security tests.
5. Prepare initial Git baseline commit after reviewing local-only generated files.


## Additional Review Fixes

After an independent code review, I fixed these blockers:

- Booking-created payment flow: booking now stores a positive fare estimate derived from route length.
- Duplicate payment intent risk: payment service reuses active intents and Flyway V004 adds a partial unique index for active intents per booking.
- Route publish validation: departure time must be future and requested seats must fit the approved owned vehicle.
- Location ownership: update must match an active trip owned by the driver profile.
- Trip transitions: run in a transaction with row locking.
- Local user status: suspended/deleted app users are denied after JWT projection.

Latest verification remains green:

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
GET /actuator/health -> 200 {"status":"UP"}
Flyway latest version -> 005
```


## JPA / Layering Refactor Checkpoint

User requested a cleanup before continuing feature work: use Lombok where possible, fix Java formatting, remove database queries from application services, stop using JdbcTemplate, and prefer Spring Data JPA/JPA repositories with persistence separated from business logic.

Completed:

- Added Spring Data JPA/Lombok-backed infrastructure entities and repositories.
- Removed JdbcTemplate from main Java sources.
- Removed `EntityManager`, `createNativeQuery`, JdbcTemplate, and SQL strings from application services.
- Kept database-specific native SQL only inside infrastructure repository methods where needed for PostGIS, upsert, insert-returning, and atomic seat reservation.
- Added `PersistenceArchitectureTest` enforcing persistence boundaries.
- Added Spotless/google-java-format and applied formatting.
- Added `ClockConfig` so runtime constructor injection works with testable `Clock` usage.

Latest verification:

```text
./mvnw spotless:apply spotless:check test -> BUILD SUCCESS
Spotless: 106 Java files clean
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
GET /actuator/health -> 200 {"status":"UP"}
Independent architecture review -> passed: true
```

## Service/Impl + Facade Architecture Refactor Checkpoint

User approved replacing the unfamiliar `port/in` and `port/out` package naming with a learner-friendly Spring Boot modular monolith structure.

Completed:

- Added architecture document: `docs/architecture/BACKEND-MODULAR-MONOLITH-SERVICE-IMPL-FACADE.md`.
- Updated `QUALITY_STANDARDS.md`, `DECISION_LOG.md`, and implementation architecture docs.
- Added MapStruct dependency/config and `common/mapper/RouteShareMapperConfig.java`.
- Added/updated MapStruct mappers for driver, passenger, and vehicle flows.
- Added module facades and facade implementations for cross-module calls.
- Refactored services to avoid another module's repository/entity/impl internals.
- Expanded architecture tests to enforce the service/impl + facade architecture.

Latest verification:

```text
./mvnw spotless:apply test -q -> BUILD SUCCESS
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
Architecture tests -> passed
Runtime health -> not re-run because Docker is unavailable in the current Mac shell
```

Current next step:

- Continue feature implementation with route search/matching and richer booking/trip/payment flows, using the approved service/impl + facade module structure.

## Virtual Threads Checkpoint

User asked to leverage virtual threads for better backend performance.

Completed:

- Enabled Spring Boot virtual threads in `apps/api/src/main/resources/application.yml`.
- Added bounded HikariCP pool configuration to avoid overwhelming PostgreSQL with too many concurrent virtual-thread database calls.
- Added `VirtualThreadConfigurationTest` to protect this configuration.
- Documented virtual thread policy in architecture, quality standards, decision log, task log, and development status.

Verification:

```text
./mvnw spotless:apply test -q -> BUILD SUCCESS
Virtual thread configuration test -> passed
```



## Phase 04 Route Search / Matching Foundation Checkpoint

Completed at: 2026-06-01 19:49 +0530

Implemented:

- `POST /api/v1/routes/search` authenticated route search endpoint.
- Route search request/response DTOs.
- PostGIS candidate filtering for published routes using time window, available seats, pickup/drop proximity, and same-direction line fractions.
- Exact overlap distance calculation foundation using `ST_LineLocatePoint`, `ST_LineSubstring`, and geography distance/length.
- Route match scoring domain with overlap, pickup proximity, drop-off proximity, weighted score, and explanation.
- TDD test coverage for same-direction rejection and stronger match ranking.

Verification:

```text
./mvnw spotless:apply spotless:check test -q -> BUILD SUCCESS
./mvnw test -> Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
GET /actuator/health -> 200 {"status":"UP"}
PostGIS smoke query -> one same-direction candidate found; overlap calculation returned 94351.60m; transaction rolled back
Flyway latest version -> 005 successful
```

Current next step:

- Continue Phase 04 with route schedule rules, route occurrence generation, H3/bucket indexing, and integration/performance tests for matching queries.


## Phase 04 Completion Checkpoint

Completed at: 2026-06-01 20:17 +0530

Completed:

- Route schedule rule foundation for one-time published routes.
- Route occurrence generation at publish time.
- Route bucket-cell indexing foundation for broad candidate filtering without requiring the H3 PostgreSQL extension yet.
- Search query now includes bucket-cell prefilter, then exact PostGIS distance, direction, overlap, and scoring.
- Flyway V006 creates `routing.route_schedule_rule`, `routing.route_occurrence`, and `routing.route_bucket_cell` with indexes.

Verification:

```text
./mvnw spotless:apply spotless:check test -q -> BUILD SUCCESS
./mvnw test -> Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
GET /actuator/health -> 200 {"status":"UP"}
Flyway latest version -> 006 successful
New routing tables -> present
PostGIS route occurrence + bucket-cell smoke query -> one same-direction candidate, overlap 94351.60m, transaction rolled back
```

Third-party keys/config:

- No third-party key was required for Phase 04 backend completion.
- Google Maps Platform key is needed later for app-side maps/place autocomplete/directions route generation.
- Firebase/FCM is needed later for notification delivery.

Current next step:

- Start Phase 05: booking/trip/fare/payment lifecycle hardening.


## Phase 05 started — booking occurrence inventory slice

- Committed completed Phase 04 work first: `75fdbd6 feat(routing): add route occurrence matching foundation`.
- Started Phase 05 by moving booking inventory from abstract `route_plan` seats to concrete `route_occurrence` seats.
- Added Flyway V007 to store `route_occurrence_id`, `pickup_route_fraction`, and `dropoff_route_fraction` on bookings.
- Updated route search output to include `routeOccurrenceId`, `pickupRouteFraction`, and `dropoffRouteFraction` so the passenger app can hand matched route data into booking creation.
- Booking fare estimate now uses matched segment distance from the stored fractions.
- Verification passed: `BookingServiceTest`, full `spotless:apply spotless:check test`, runtime health, Flyway V007, and DB column checks.

Next recommended work:

1. Add booking status history.
2. Add explicit HTTP `Idempotency-Key` handling backed by `common.idempotency_key`.
3. Continue passenger boarded/no-show/drop-off and route-occurrence-aware trip lifecycle work.
