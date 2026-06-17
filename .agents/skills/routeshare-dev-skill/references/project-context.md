# Project Context

## Contents

- Product
- Architecture
- Applications
- Auth and identity
- Data and persistence
- External integrations
- Local development and deployment
- Documentation map
- Gaps to fill

## Product

RouteShareApp is a route-based ride-sharing platform. Drivers publish planned routes they already intend to travel; passengers search and book full or partial overlapping route segments. The product scope includes passenger, driver, and admin experiences with booking, trip, payment, live location, safety, support, notifications, ratings, and operational review flows.

Read docs/architecture/RouteShareApp_ARCHITECTURE.md and inspect README.md, docs/source-assets/ when available, and docs/development/IMPLEMENTATION_ROADMAP.md before changing this.

## Architecture

The current backend is implemented as a Spring Boot modular monolith, not separate microservices yet. It deliberately uses business-module packages and Spring-friendly `controller`, `service`, `service/impl`, `facade`, `facade/impl`, `domain`, `entity`, and `repository` names. The repo keeps future service boundaries protected through facades and architecture tests.

The frontend workspace is a pnpm monorepo. The passenger app is the active mobile implementation; driver mobile and admin web are placeholders for later phases.

Read docs/architecture/BACKEND-MODULAR-MONOLITH-SERVICE-IMPL-FACADE.md and inspect apps/api/src/test/java/com/routeshare/architecture/PersistenceArchitectureTest.java, apps/api/pom.xml, pnpm-workspace.yaml, and package.json before changing this.

## Applications

`apps/api` contains implemented backend modules for identity, passenger, driver, vehicle, routing, booking, trip, location, pricing, payment, admin, app readiness, maps, and common infrastructure.

`apps/passenger-mobile` is an Expo React Native app. Implemented real screens currently include onboarding/auth/OTP/profile setup/home/search/account/saved places/trusted contacts/verification. Many later passenger routes are typed and linkable but still placeholder-level.

`apps/driver-mobile` and `apps/admin-web` are README placeholders. Backend APIs for driver/admin exist, but those client apps are later phases.

Read docs/development/DEVELOPMENT_STATUS.md and inspect apps/api/src/main/java/com/routeshare/, apps/passenger-mobile/src/, apps/driver-mobile/README.md, and apps/admin-web/README.md before changing this.

## Auth And Identity

Keycloak owns user identity, roles, sessions, and JWTs. The backend projects the Keycloak subject into local app users and business profiles. A single real user can act as passenger and/or driver. Spring Security protects API routes using OAuth2 Resource Server JWT validation, with custom Keycloak role conversion. Phone OTP has backend endpoints and a provider abstraction; local/demo OTP behavior must stay backend-only and disabled for production defaults.

Read docs/development/implementation/11-AUTH-KEYCLOAK-USER-MANAGEMENT.md, docs/api/README.md, and docs/development/PRODUCTION_EXTERNAL_SERVICES.md and inspect apps/api/src/main/java/com/routeshare/common/security/, apps/api/src/main/java/com/routeshare/identity/, infra/keycloak/import/routeshare-realm.json, apps/passenger-mobile/src/features/auth/, and apps/passenger-mobile/src/application/auth-store.ts before changing this.

## Data And Persistence

PostgreSQL with PostGIS is the system of record. The database uses module schemas for identity, passenger, driver, vehicle, routing, booking, trip, location, pricing/payment, and common idempotency. Flyway owns schema evolution. Redis stores latest live location/cache-like state. Redpanda/Kafka is local event infrastructure. MinIO is local object storage for future document/image workflows.

Important domain correctness areas include route occurrence inventory, booking idempotency, passenger trip states, trip state machines, fare ledger immutability, payment lifecycle, location freshness/impossible-jump validation, and provider-backed maps/places.

Read docs/database/routeshare-database-architecture.mmd, docs/development/QUALITY_STANDARDS.md, docs/development/implementation/04-ROUTE-PUBLISHING-AND-MATCHING.md, docs/development/implementation/05-BOOKING-TRIP-FARE-PAYMENT.md, and docs/development/implementation/06-REALTIME-LOCATION-TRACKING.md and inspect apps/api/src/main/resources/db/migration/, apps/api/src/main/java/com/routeshare/*/entity/, apps/api/src/main/java/com/routeshare/*/repository/, and infra/docker-compose/docker-compose.yml before changing this.

## External Integrations

Provider decisions recorded in project docs:

- Identity: Keycloak.
- SMS/OTP: Notify.lk.
- Maps/routing/places: Google Maps Platform.
- Card payments: Cybersource.
- Push notifications: Firebase Cloud Messaging through Expo notifications.
- Error monitoring: Sentry.
- Local object storage: MinIO; production object storage provider still undecided.

Production-ready provider work requires real credentials, restricted keys, backend-only secrets, local test doubles where appropriate, and device/runtime evidence where native behavior is involved. Do not replace required provider behavior with fake UI or manual-only flows and then mark a production task complete.

Read docs/development/PRODUCTION_EXTERNAL_SERVICES.md, docs/development/SELECTED_PROVIDER_IMPLEMENTATION_GUIDE.md, and docs/development/BLOCKERS.md and inspect .env.example, apps/api/src/main/resources/application.yml, apps/api/src/main/java/com/routeshare/identity/provider/, apps/api/src/main/java/com/routeshare/maps/, apps/passenger-mobile/app.config.ts, and apps/passenger-mobile/eas.json before changing this.

## Local Development And Deployment

Local infrastructure starts through scripts around Docker Compose:

- `scripts/dev-up.sh`
- `scripts/dev-down.sh`
- `scripts/dev-logs.sh`

Backend runs from `apps/api` with Java 21 and Maven Wrapper. Passenger app uses pnpm workspace filters and Expo dev-client/EAS config. `.env.example` is a safe template; real `.env` values and generated outputs are ignored.

There is no `.github` CI configuration at skill creation time. Local verification commands and living-doc evidence are currently the operational quality gates.

Read README.md, apps/passenger-mobile/README.md, qa/README.md, and docs/development/REPOSITORY_ORGANIZATION_PLAN.md and inspect scripts/, infra/docker-compose/docker-compose.yml, apps/api/pom.xml, apps/passenger-mobile/package.json, and apps/passenger-mobile/eas.json before changing this.

## Documentation Map

Living docs are part of the application, not afterthoughts:

- Architecture: `docs/architecture/`
- API contracts/reconciliation: `docs/api/`
- Database diagram: `docs/database/`
- Development status and process: `docs/development/`
- Implementation task plans: `docs/development/implementation/` and `docs/development/implementation/tasks/`
- QA specs and automation: `qa/test-cases/` and `qa/maestro/`
- Source design/requirement assets: `docs/source-assets/`

Durable status from QA reports must be summarized into development docs; raw run evidence stays ignored under `qa/reports/`.

Read docs/development/REPOSITORY_ORGANIZATION_PLAN.md and docs/development/IMPLEMENTATION_PLANNING_STANDARD.md and inspect docs/, qa/, and .gitignore before changing this.

## Gaps To Fill

Important missing or incomplete docs detected during skill creation:

- `CONTRIBUTING.md` for branch/commit/review/verification policy.
- `.github/workflows/*` or CI documentation for automated gates.
- Production deployment/runbook docs for backend, mobile, admin, migrations, secrets, and rollbacks.
- Dependency approval and vulnerability-review policy.
- Production secrets-manager decision.
- Driver mobile and admin web implementation task folders when those phases start.

Read docs/development/BLOCKERS.md and inspect docs/development/IMPLEMENTATION_ROADMAP.md before changing this.
