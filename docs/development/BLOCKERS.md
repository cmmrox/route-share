# RouteShareApp Blockers

Last Updated: 2026-06-01 21:35 +0530

## Purpose

This file tracks anything that blocks or slows implementation.

Blocker Status Values:

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `DEFERRED`

---

## Active Blockers

### Blocker 003 — Full backend completion still requires larger product workflows

Status: `OPEN`
Severity: `MEDIUM`

Description:

The backend foundation is verified, but the full product backend still needs several complete workflows:

- Upload/storage integration for KYC/document binaries.
- Deeper route matching integration/performance tests with realistic volumes.
- Richer booking status lifecycle beyond initial confirmation/cancel/reject/complete transitions.
- Trip passenger state transitions and settlement/payment lifecycle.
- Realtime WebSocket updates and event streaming/outbox.
- Admin management/reporting APIs.
- Integration/security tests.

Impact:

- Current backend can run and validate the core foundation, but it is not yet a production-complete RouteShareApp backend.

Recommended Action:

- Continue implementation in small verified slices: Phase 05 booking/trip/fare/payment lifecycle, then realtime/event/admin workflows. Add realistic-volume route matching performance tests before scale claims.

---

## Resolved Blockers

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
