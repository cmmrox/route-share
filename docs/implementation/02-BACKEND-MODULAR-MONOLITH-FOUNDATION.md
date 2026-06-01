# Stage 02 — Backend Modular Monolith Foundation Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Scaffold the Spring Boot modular monolith with clean module boundaries, database migrations, Keycloak-based security, error handling, idempotency, and testing infrastructure.

**Architecture:** One Spring Boot deployable, many internal modules. Each module has `api`, `application`, `domain`, and `infrastructure` packages.

**Tech Stack:** Spring Boot 3, Java 21, Maven, Flyway, PostgreSQL/PostGIS, Testcontainers, JUnit 5, Spring Security OAuth2 Resource Server, Keycloak.

---

## Acceptance criteria

- `services/api-monolith` builds and starts.
- Health endpoint works.
- Flyway migrations run locally.
- Common error response format exists.
- Module boundary conventions are documented and testable.

## Tasks

### Task 1: Create Spring Boot app

**Files:**
- Create: `services/api-monolith/pom.xml`
- Create: `services/api-monolith/src/main/java/com/routeshare/RouteShareApplication.java`
- Create: `services/api-monolith/src/main/resources/application.yml`

**Verification:** `./mvnw test` or `mvn test` passes.

### Task 2: Add common web/error layer

**Files:**
- Create: `common/web/ApiResponse.java`
- Create: `common/errors/ApiError.java`
- Create: `common/errors/GlobalExceptionHandler.java`
- Create tests for validation and domain errors.

**Rule:** All errors return stable `code`, `message`, `correlationId`, and optional `fieldErrors`.

### Task 3: Add database and Flyway

**Files:**
- Create: `src/main/resources/db/migration/V001__create_extensions.sql`
- Create: `src/main/resources/db/migration/V002__create_module_schemas.sql`

**Schemas:** `identity`, `passenger`, `driver`, `vehicle`, `routing`, `matching`, `booking`, `trip`, `location`, `pricing`, `payment`, `settlement`, `notification`, `safety`, `support`, `ratings`, `admin`.

**Verification:** App starts with local PostgreSQL and migrations succeed.

### Task 4: Add Keycloak security integration

**Files:**
- Create: `common/security/SecurityConfig.java`
- Create: `common/security/CurrentUser.java`
- Create: `common/security/CurrentUserProvider.java`
- Create: `common/security/KeycloakJwtRoleConverter.java`
- Create: `common/security/RouteShareRoles.java`

**Final auth decision:** Use **Keycloak** for authentication, authorization, identity/user management, realm roles, client roles, groups, sessions, and token issuance. The backend is a Spring Security OAuth2 Resource Server that validates Keycloak JWTs.

**Required Keycloak concepts:**
- Realm: `routeshare`
- Clients: `passenger-mobile`, `driver-mobile`, `admin-web`, `api-monolith`
- Realm roles: `PASSENGER`, `DRIVER`, `ADMIN`, `SUPPORT_AGENT`, `VERIFICATION_AGENT`, `FINANCE_ADMIN`, `OPS_ADMIN`, `SUPER_ADMIN`
- One Keycloak user can have both `PASSENGER` and `DRIVER` roles.
- Passenger/driver-specific business data remains in backend module tables, keyed by Keycloak subject/user id.

**Verification:** Backend rejects missing/invalid JWTs, maps Keycloak roles correctly, and exposes `/api/v1/auth/me` from token claims plus local profile status.

### Task 5: Add idempotency support

**Files:**
- Create: `common/idempotency/IdempotencyKey.java`
- Create: `common/idempotency/IdempotencyService.java`
- Create migration for `common_idempotency_keys` or schema-specific idempotency table.

**Used by:** booking confirmation, trip state transitions, payment preauth/capture, event consumers.

### Task 6: Add event abstraction

**Files:**
- Create: `common/events/DomainEvent.java`
- Create: `common/events/EventPublisher.java`
- Create: `common/events/InMemoryEventPublisher.java`
- Later adapter: Redpanda/Kafka.

**Rule:** Domain/application code publishes via interface only.
