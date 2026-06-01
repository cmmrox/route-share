# RouteShareApp Blockers

Last Updated: 2026-06-01 12:43 +0530

## Purpose

This file tracks anything that blocks or slows implementation.

Blocker Status Values:

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `DEFERRED`

---

## Active Blockers

### Blocker 002 — Initial Git commit is pending

Status: `OPEN`
Severity: `MEDIUM`

Description:

The project root is now a Git repository on branch `main`, but latest status still shows project contents as untracked (`?? apps/`, `?? docs/`, `?? infra/`, etc.).

Impact:

- `git diff` cannot show normal tracked-file diffs until files are added.
- Verification and implementation can continue, but change review and rollback are weaker without an initial commit.

Recommended Action:

- Review generated project files for local-only secrets before staging.
- Add appropriate files to Git and create the first baseline commit.
- Keep local dev credentials redacted/local-only.

---

### Blocker 003 — Full backend completion still requires larger product workflows

Status: `OPEN`
Severity: `MEDIUM`

Description:

The backend foundation is verified, but the full product backend still needs several complete workflows:

- Upload/storage integration for KYC/document binaries.
- Route search and route matching.
- Booking idempotency and richer status lifecycle.
- Trip passenger state transitions and settlement/payment lifecycle.
- Realtime WebSocket updates and event streaming/outbox.
- Admin management/reporting APIs.
- Integration/security tests.

Impact:

- Current backend can run and validate the core foundation, but it is not yet a production-complete RouteShareApp backend.

Recommended Action:

- Continue implementation in small verified slices, starting with Phase 04 route matching/search and richer backend workflows.

---

## Resolved Blockers

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
