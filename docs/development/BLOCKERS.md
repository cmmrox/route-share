# RouteShareApp Blockers

Last Updated: 2026-06-02 00:35 +0530

## Purpose

This file tracks anything that blocks or slows implementation.

Blocker Status Values:

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `DEFERRED`

---

## Active Blockers

No active blocker prevents starting Phase 06 after the verified pre-Phase-06 commit.

## Resolved Blockers

### Blocker 006 — API contracts expanded but backend implementation is not reconciled

Status: `RESOLVED`
Severity: `HIGH`

Description:

The passenger, driver, and admin OpenAPI contracts have been expanded after reviewing the business requirement PDF and supplied mobile designs. These contracts now include required product APIs for payment methods, receipts, early drop-off, KYC upload details, recurring routes, cash collection, earnings, admin finance/support/safety/reporting, and other screens. The current backend implementation only covers a subset and often uses generic resource paths such as `/api/v1/routes`, `/api/v1/bookings`, `/api/v1/trips`, and `/api/v1/payments`.

Impact:

- Passenger/driver/admin app development can drift or be blocked if it starts before endpoint names and response shapes are reconciled.
- Generated TypeScript clients cannot be treated as implementation-ready until backend coverage is verified.

Recommended Action:

- Before Phase 07/08/09 app implementation, reconcile every path in `docs/api/*.openapi.json` with Spring Boot controllers.
- Either implement app-specific aliases, update contracts to canonical generic endpoints, or mark paths as deferred/future.
- Add contract drift checks and generate clients under `packages/api-contracts`.

Progress 2026-06-01 23:22 +0530:

- First app-facing backend alias slice implemented and verified for passenger ride search, passenger booking create/cancel, passenger payment intent, driver route create, and driver trip/passenger-state operations.
- Earlier remaining gaps for list/detail/projection endpoints, manual booking approval, admin finance operations, payment lifecycle, and generated contract inventory are now implemented for the Phase 06 gate. Realtime begins in Phase 06; notifications/support/SOS are later phases.

---

## Deferred / Later-Phase Risks

### Blocker 003 — Full backend completion still requires larger product workflows

Status: `DEFERRED`
Severity: `MEDIUM`

Description:

The backend foundation is verified, but the full product backend still needs several complete workflows:

- Upload/storage integration for KYC/document binaries.
- Deeper route matching integration/performance tests with realistic volumes.
- Realtime WebSocket updates and event streaming/outbox are Phase 06.
- Notifications/support/SOS, advanced admin management/reporting APIs, and real provider payout batches are later product/hardening phases.
- Integration/security tests.

Impact:

- Current backend can run and validate the core foundation, but it is not yet a production-complete RouteShareApp backend.

Recommended Action:

- Phase 05 booking/trip/fare/payment lifecycle is complete for the Phase 06 gate. Continue with Phase 06 realtime/event work next; add realistic-volume route matching performance tests before scale claims.

---

## Historical Resolved Blockers

### Blocker 002 — Initial Git commit is pending

Status: `RESOLVED`
Severity: `MEDIUM`

Description:

Earlier tracking showed the project contents were untracked and no initial baseline commit existed. The project now has a baseline commit on branch `main`; latest Phase 04 work is present as normal tracked/untracked working-tree changes for review.

Resolution:

- Baseline commit exists.
- Use normal `git diff`/`git status` for current Phase 04 changes before committing.

---

### Blocker 001 — Repository is not initialized as Git repository

Status: `RESOLVED`
Severity: `LOW`

Description:

Earlier tracking stated the project was not initialized as Git. Latest check shows the project is a Git repository on branch `main`.

Resolution:

- Replace this with Blocker 002: initial commit is still pending.

---

### Blocker 004 — Runtime startup failure after service extraction

Status: `RESOLVED`
Severity: `HIGH`

Description:

After extracting controller logic into services, Spring Boot startup initially failed because services with secondary test constructors had multiple constructors and no explicit autowired production constructor.

Resolution:

- Added explicit `@Autowired` to production constructors for affected services.
- Re-ran Maven tests successfully.
- Restarted API and verified `/actuator/health` returned HTTP 200.

---

### Blocker 005 — Service layer contained persistence queries/JdbcTemplate-style access

Status: `RESOLVED`
Severity: `HIGH`

Description:

The backend needed stricter layering: application services should contain business logic only, with persistence isolated under infrastructure repositories. JdbcTemplate should not be used in main code, and JPA repositories should be preferred.

Resolution:

- Refactored persistence into Spring Data JPA `JpaRepository` interfaces and Lombok-backed entities/projections under `*/infrastructure`.
- Removed JdbcTemplate from `src/main/java`.
- Removed `EntityManager`, `createNativeQuery`, JdbcTemplate, and SQL strings from `*/application` services.
- Added `PersistenceArchitectureTest` to enforce these boundaries.
- Added Spotless/google-java-format and verified formatting.
- Verified `./mvnw spotless:apply spotless:check test` and runtime `/actuator/health` are green.


## Phase 05 booking occurrence slice

Status: no third-party blocker.

Notes:

- Booking occurrence reservation and matched-fraction persistence are local backend/database work.
- Payment provider selection remains a later blocker for real preauthorization/capture/refund flows.


## Phase 05 booking status history slice

Status: no third-party blocker.

Notes:

- Initial booking status history is a local database/backend feature and is verified with Flyway V008.
- Explicit booking idempotency handling is implemented using the existing `common.idempotency_key` table.


## Phase 05 booking idempotency slice

Status: no third-party blocker.

Notes:

- Booking `Idempotency-Key` handling is local backend/database work and uses `common.idempotency_key`.
- Real payment-provider idempotency remains a later provider-specific concern once the gateway is selected.


## Phase 05 booking status-transition slice

Status: no third-party blocker.

Notes:

- Cancel/reject/complete booking transitions are local backend/database work and now write `booking.booking_status_history` rows in the same transaction as the status update.
- Seat release/refund/settlement side effects remain future workflow slices and should be implemented with explicit tests.


Progress 2026-06-01 23:43 +0530:

- Passenger booking/trip projections, driver route/trip projections, booking requests, and driver approve/decline are implemented and verified.
- Blocker 006 is now resolved for the Phase 06 gate. Realtime-dependent APIs start in Phase 06; notifications/support/SOS and full contract drift automation are later phases.


Payment progress 2026-06-01 23:52 +0530:

- Capture, void, refund, driver cash collection, and passenger receipt foundation are implemented and verified.
- Admin payment list/detail/events, cash collection review, driver earnings, platform commission, and settlement balance are implemented for the Phase 06 gate. Provider-specific payment gateway integration remains deferred hardening.


Blocker 006 closed 2026-06-02 00:08 +0530:

The API contract/backend mismatch that blocked Phase 06 has been reduced to non-blocking future hardening. Core Passenger/Driver/Admin pre-Phase-06 endpoints are implemented, tested, smoke-verified, and tracked. Real payment provider integration, settlement payout operations, and advanced finance review workflows remain later hardening tasks, not Phase 06 blockers.


Final audit 2026-06-02 00:35 +0530:

- No blocker remains open for phases 00 through 05.5.
- Phase 06 may start after committing the verified working tree.
- Deferred risks are intentionally later-phase scope, not incomplete pre-Phase-06 work.
