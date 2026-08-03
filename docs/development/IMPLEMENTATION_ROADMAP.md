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

## Implementation Planning Standard

All future high-level feature plans must follow `docs/development/IMPLEMENTATION_PLANNING_STANDARD.md`.

Required pattern:

```text
docs/development/implementation/tasks/<feature-plan-name>/
```

Each feature folder must include a high-level `README.md` and one numbered task file per production-ready implementation slice. Every task file must include architecture/design notes, API/database/configuration impacts, development steps, a link to the matching `qa/test-cases/` file, verification commands, done criteria, and expected file changes.

A task is complete only when that feature slice is ready to ship to production. Do not split a user-visible production feature so that one task implements only the happy path and another later task adds required API wiring, error handling, linked QA coverage, authorization, database behavior, or release readiness.

---

## Phase 00 — Project Architecture and File Structure

Status: `COMPLETED`

Tracking Setup: `COMPLETED`

Goal: Create a clean, scalable repository structure before application code is written.

Tasks:

- [x] Create/verify root application structure.
- [x] Create `apps/api` for Spring Boot backend.
- [x] Create placeholder `apps/passenger-mobile` for passenger Expo app.
- [x] Create placeholder `apps/driver-mobile` for driver Expo app.
- [x] Create placeholder `apps/admin-web` for Next.js admin web.
- [x] Create placeholder `packages/api-contracts` for generated/shared OpenAPI clients later.
- [x] Create placeholder `packages/shared-types` if needed for shared TypeScript contracts.
- [x] Add real TypeScript workspace/package setup and generated contract inventory package before mobile/admin implementation.
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
- [x] Add startup scripts:
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
- [x] Add first migrations:
  - `V001__create_extensions.sql`
  - `V002__create_module_schemas.sql`
  - `V003__create_foundation_tables.sql`
- [x] Add Spring Security OAuth2 Resource Server.
- [x] Add Keycloak JWT role converter.
- [x] Add common API error model.
- [x] Add validation framework.
- [x] Add structured logging conventions.
- [x] Add springdoc/OpenAPI endpoint.
- [x] Add Testcontainers integration tests.
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

Status: `COMPLETED`

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

Status: `COMPLETED_FOR_PHASE_06_GATE`

Goal: Implement the transactional core for ride sharing.

Tasks:

- [x] Implement booking creation with explicit `Idempotency-Key` replay handling backed by `common.idempotency_key`.
- [x] Store matched pickup/drop route fractions on bookings.
- [x] Move booking inventory reservation from route plans to route occurrences.
- [x] Implement seat reservation and no-overbooking guarantees for route occurrences.
- [x] Implement booking status history, including initial and cancel/reject/complete transition audit rows.
- [x] Implement manual booking approve/decline if route requires it.
- [x] Implement trip start/complete/cancel state machine.
- [x] Implement passenger boarded/no-show/drop-off state machine.
- [x] Implement fare estimate.
- [x] Implement immutable fare ledger.
- [x] Implement payment intent abstraction.
- [x] Implement cash collection records.
- [x] Implement driver earnings ledger/read model foundation.
- [x] Implement platform commission ledger/read model foundation.
- [x] Implement settlement balance read model foundation.

Deliverable:

- Booking and trip lifecycle works safely with fare/payment/settlement records.

---

## Phase 05.5 — API Contract Reconciliation and App-Facing Backend Aliases

Status: `COMPLETED_FOR_PHASE_06_GATE`

Goal: Reconcile passenger/driver/admin OpenAPI contracts with implemented Spring Boot controllers before mobile/admin apps are wired.

Tasks:

- [x] Create `docs/api/API_BACKEND_RECONCILIATION.md`.
- [x] Decide canonical public API approach: stable app-facing paths delegate to shared backend services/facades.
- [x] Add `POST /api/v1/passenger/ride-searches` alias over route search.
- [x] Add `POST /api/v1/passenger/bookings` alias over booking create with `Idempotency-Key`.
- [x] Add `POST /api/v1/passenger/bookings/{bookingId}/cancel` alias over booking cancel transition.
- [x] Add `POST /api/v1/passenger/payments/intents` alias over payment intent creation.
- [x] Add `POST /api/v1/driver/routes` alias over route publish/create.
- [x] Add driver trip operation aliases for start, complete, board, no-show, and drop-off.
- [x] Add passenger booking list/detail/current-trip/history projections.
- [x] Add driver route list/detail/cancel endpoints.
- [x] Add driver trip list/detail and booking request projections.
- [x] Implement manual booking approve/decline with driver authorization.
- [x] Add admin path reconciliation for review/list/detail APIs needed before Phase 06.
- [x] Keep `docs/api/API_BACKEND_RECONCILIATION.md` updated after every API slice.

Deliverable:

- Passenger/driver/admin generated clients can target implemented or explicitly deferred backend APIs without guessing.

---

## Phase 06 — Realtime Location

Status: `COMPLETED`

Goal: Add live driver/passenger/admin tracking without overloading PostgreSQL.

Tasks:

- [x] Implement location update ingestion API.
- [x] Validate freshness, accuracy, speed, and impossible jumps.
- [x] Store latest active trip location in Redis.
- [x] Persist selected/auditable samples to PostgreSQL.
- [x] Publish location events to Redpanda/Kafka.
- [x] Implement WebSocket/STOMP updates.
- [x] Implement passenger live trip state.
- [x] Implement admin live trip feed.

Deliverable:

- Passenger/admin can track active trips live.

---

## Phase 06.5 — App Backend Readiness Closure

Status: `COMPLETED`

Goal: Close or explicitly defer every app-facing backend API required by Passenger Mobile, Driver Mobile, and Admin Web before starting full UI implementation.

Source audit: `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

Tasks:

- [x] Shared app platform APIs: app config, notifications, push registrations, support tickets/messages, SOS events, audit helper.
- [x] Passenger readiness: early drop-off/fare finalization, share-trip/share-link, rating, payment methods/card placeholder, optional persisted search result detail or contract simplification.
- [x] Driver readiness: verification status, KYC identity/licence, document submit lifecycle, vehicle detail/update/delete, explicit publish or contract simplification, recurring route CRUD/generation, payout profile, ratings/support/SOS/notifications.
- [x] Admin readiness: dashboard, users, verification queues, document review/download, trips/bookings, fare/commission config, settlements/payout batches, support/SOS/notifications/reports/audit.
- [x] Regenerate API contract inventory and verify all non-deferred app-facing paths are implemented.

Deliverable:

- Phase 07/08/09 can start without backend-blocked screens, or reduced app scopes are explicitly feature-flagged.

## Phase 06.6 — Backend Production Hardening

Status: `COMPLETED`

Context: A production-readiness audit (2026-06-18) found that the Phase 06.5 "readiness closure" was a facade — ~50 app-facing endpoints (notifications, push, support, SOS, ratings, payment methods, recurring routes, payout, and the entire admin suite) were backed by a single generic `app_backend.workflow_item` table with untyped responses, and the only real external integrations were Notify.lk SMS + Google Places (payments were `mock_`-only, no FCM/object-storage/Sentry/Kafka). This phase rebuilds those into real domain modules with real, credential-gated provider integrations. Delivered on branch `feat/backend-production-hardening` as phased verified commits.

Tasks:

- [x] Phase A — Eventing backbone (transactional outbox + relay + Kafka sender) + observability (Prometheus/Sentry/structured logs/health probes) + staging/prod env templates.
- [x] Phase B — Object storage adapter (S3/MinIO `ObjectStoragePort`) + real KYC/document upload→submit→signed-download lifecycle for driver, vehicle, and passenger documents (V017). Admin review/signed-download lands in Phase G.
- [x] Phase C — Cybersource payment gateway (`PaymentGatewayPort` + real `CybersourcePaymentGateway` authorize/capture/void/refund/tokenize + HTTP-signature, cash fallback), signature-verified idempotent webhook, real payment-methods domain (tokenized), configurable commission + PLATFORM_COMMISSION/DRIVER_EARNING ledger entries (V018). Admin settlement/payout-batch/finance-adjustment land in Phase G.
- [x] Phase D — Real notifications domain (notification/preference/push_registration/delivery_log, V019), `PushNotificationPort` with FCM adapter + logging fallback, typed passenger/driver inbox/preferences/push-registration endpoints, `NotificationFacade` for cross-module sends. Removed workflow_item notification shells (and fixed a duplicate payment-methods mapping from Phase C).
- [x] Phase E — Real support (support_ticket/support_message state machine), safety (sos_event raise + domain event + confirmation notification), and rating (per-booking, unique, driver aggregate) domains (V020). Passenger/driver controllers replace the workflow_item shells; admin manage/resolve lands in Phase G.
- [x] Phase F — Real recurring routes (RECURRING schedule rules + multi-occurrence generation with bucket cells, reusing the routing matching infra; create/list/pause/cancel/generate-more) and real driver payout profile (bank/wallet, validation, masked, re-verification on change) (V021). Replaced the workflow_item shells.
- [~] Phase G — Admin suite rebuild. **G-1 done (V022):** real audit log (`audit.audit_action` + `AdminAuditService` recording every admin mutation), real user management (list/detail/suspend/activate/status-history on `identity.app_user` — suspension is enforced via `upsertFromToken`), and real admin support + SOS management wired to the Phase E tables (status/reply/resolve with audit + raiser notification). Role granularity via method security on each controller. **G-2 done (V023):** real admin finance — commission rules + fare policies CRUD, finance adjustments, settlement driver-balances (DRIVER_EARNING accrued minus PAID payout items), and payout batches (create with items → total, mark-paid) in a new `finance` schema, all audited, served by `AdminFinanceController` (admin module, exempt from cross-module rule, reads finance + payment repos). **G-3 done (V022 reuse):** real admin dashboard + reports/summary via `AdminDashboardController`; real document review + signed download for driver/vehicle/passenger documents via `AdminDocumentController`. **G-4 done:** notification broadcasts (`AdminBroadcastController` fan-out via the notification facade); **Keycloak realm-role propagation** through the existing project Keycloak admin client (`KeycloakRealmRoleAdapter` → `IdentityFacade.setRealmRoles` → `AdminUserController` PUT roles); admin trips/bookings/driver-applications/vehicles read projections + booking status-history + admin trip-cancel + report-export request (`AdminOpsController`). Also fixed a latent duplicate `/reports/summary` mapping from G-3. Only `driver-applications/{id}/review` and `trips/{id}/location-trail` remain on the facade (location trail needs PostGIS coordinate extraction, landing with Phase H). **Phase G (admin suite) is functionally complete.**
- [x] Phase H — `RouteMetricsPort` (Google Distance Matrix when configured, haversine fallback) feeds server-side distance+duration into a time-aware `FareCalculator` (base + per-km + per-min); new `POST /api/v1/pricing/estimate-by-route` prices from pickup/drop-off coordinates instead of a client-supplied distance. Trip **location-trail** now returns real PostGIS-extracted coordinates (`AdminOpsController`), retiring the last admin facade read endpoint.
- [~] Phase I — **Done:** live boot validated against the project Docker stack (Flyway V016–V023 applied; full context starts); `RequestMappingUniquenessTest` (deterministic ambiguous-mapping guard that fixed 3 latent duplicates); **Redis fixed-window rate limiting** (`RateLimiter`/`RedisRateLimiter`, fail-open) on OTP request/verify, payment-intent, and SOS. **Remaining:** route-matching index/perf review + N+1 audit, a Testcontainers integration/authz suite, and a location/matching load smoke.
- [x] Phase J — Deployment readiness: multi-stage `apps/api/Dockerfile` (non-root, Actuator healthcheck) + `.dockerignore`; `infra/docker-compose/docker-compose.prod.yml` overlay (API wired to the project services, all provider flags fail-safe-defaulted); `docs/development/DEPLOYMENT.md` (build/run, gated-integration flag→credential table, migration pipeline, health/probes/metrics, backup/restore drill, runbooks, hosting checklist); confirmed the Keycloak realm import already carries the managed roles+clients; reconciled `PRODUCTION_EXTERNAL_SERVICES.md`.
- [x] Phase K — Backend gap closure (final audit). Closed the seven remaining real-feature gaps so no `workflow_item` shell remains for any product flow: (1) early drop-off actual-distance fare recalc + capture (V024); (2) lifecycle notification triggers across booking/trip/payment; (3) tokenized/time-boxed/revocable trip share-links + unauthenticated public status + trusted-contact SMS (V025); (4) real driver verification-status (profile + KYC docs + approved vehicle) + KYC presigned uploads; (5) admin-configurable matching parameters (V026, `/api/v1/admin/matching-settings`); (6) real admin analytics reports + CSV export (`/api/v1/admin/reports/{type}` + `/export`); (7) passenger avatar/verification on the real document lifecycle, admin driver-application review consolidated onto `AdminDriverReviewController`, ride-search GET shells removed (stateless search), and OpenAPI/contract drift reconciled. Live-boot verified against the project Docker stack: Flyway advanced V023→V026, `matching_settings` seeded, endpoints respond (health UP, public token 404, admin 401), `./mvnw spotless:check verify` green (180 tests, JaCoCo 80% gate).

Deliverable:

- Every passenger/driver/admin API backed by real domain modeling + real (gated) provider integrations, with tests, performance work, and observability — genuinely production-releasable.

---

## Phase 07 — Passenger Mobile App

Status: `IN_PROGRESS`

API Contract Gate: `PASSENGER_CORE_VERIFIED` — `docs/api/passenger-app.openapi.json` has been expanded from requirements/designs; backend must implement or intentionally map/defer each path before mobile screens are wired.

Goal: Build passenger app against stable backend APIs.

Tasks:

- [x] Reconcile passenger OpenAPI contract with implemented backend endpoints and generate typed client.
- [x] Scaffold Expo React Native app.
- [x] Add Keycloak login with PKCE. *(Task 05 complete; backend-owned Notify.lk OTP verified in dev.)*
- [x] Add secure token storage and refresh/logout handling. *(Task 05 foundation complete.)*
- [x] Add profile/avatar/optional verification flow. *(Task 06 complete; verification remains readiness-only until backend document review endpoints are added.)*
- [x] Add saved places/trusted contacts UI wired to backend. *(Task 06 complete.)*
- [ ] Add ride search/results/detail UI.
- [ ] Add booking flow with idempotency key handling.
- [ ] Add payment method, payment intent, receipt, and trip history UI.
- [ ] Add active trip tracking UI with realtime fallback polling.
- [ ] Add early drop-off/get-off-early flow.
- [ ] Add SOS/share trip.
- [ ] Add notifications, push registration, preferences, ratings, and support.

---

## Phase 08 — Driver Mobile App

Status: `NOT_STARTED`

API Contract Gate: `OPEN_FOR_PHASE_08_DRIVER_RECHECK` — `docs/api/driver-app.openapi.json` has been expanded from requirements/designs; backend must implement or intentionally map/defer each path before mobile screens are wired.

Goal: Build driver app against stable backend APIs.

Tasks:

- [ ] Reconcile driver OpenAPI contract with implemented backend endpoints and generate typed client.
- [x] Scaffold Expo React Native app.
- [x] Add Keycloak login with PKCE. *(Task 05 complete; backend-owned Notify.lk OTP verified in dev.)*
- [ ] Add driver application/KYC/document upload flow.
- [ ] Add vehicle registration/document flow.
- [ ] Add route creation/publish/list/detail/share/recurring flow.
- [ ] Add booking request approve/decline flow.
- [ ] Add pre-trip checklist, arrived pickup, boarded/no-show/drop-off, and complete trip flow.
- [ ] Add cash collection/payment status/fare-adjustment UI.
- [ ] Add foreground/background location updates with battery strategy.
- [ ] Add earnings, ratings, payout profile, notifications, SOS, and support screens.

---

## Phase 09 — Admin Web App

Status: `NOT_STARTED`

API Contract Gate: `OPEN_FOR_PHASE_09_ADMIN_RECHECK` — `docs/api/admin-web.openapi.json` has been expanded from requirements/designs/product ops gaps; backend must implement or intentionally map/defer each path before admin web screens are wired.

Goal: Build admin operations web app.

Tasks:

- [ ] Reconcile admin OpenAPI contract with implemented backend endpoints and generate typed client.
- [ ] Scaffold Next.js admin app.
- [ ] Add Keycloak login and admin role guards.
- [ ] Add dashboard.
- [ ] Add user search/detail/status/roles.
- [ ] Add driver and vehicle verification, private document preview, and review history.
- [ ] Add booking/trip operations including live trips, trip detail, selected location trail, and admin cancel/interrupt.
- [ ] Add payment, cash collection, finance adjustment, settlement, fare/commission policy screens.
- [ ] Add support ticket and SOS safety screens.
- [ ] Add notifications/broadcasts, reports/export, and audit screens.

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


### Payment lifecycle gap closure before Phase 06 — updated 2026-06-01 23:52 +0530

- [x] Add payment capture transition and immutable ledger entry.
- [x] Add payment void transition and immutable ledger entry.
- [x] Add payment refund transition and negative immutable ledger entry.
- [x] Add driver cash collection endpoint and ledger entry.
- [x] Add passenger booking receipt endpoint from ledger.
- [x] Add admin payment list/detail/event projections.
- [x] Add earnings summary/transactions with commission/settlement rules.


### Pre-Phase-06 closure checklist — completed 2026-06-02 00:08 +0530

- [x] Passenger booking/trip projections.
- [x] Driver route/trip projections and manual booking approve/decline.
- [x] Payment capture/void/refund/cash collection/receipt foundation.
- [x] Driver route share link/QR payload foundation.
- [x] Driver pre-trip checklist.
- [x] Driver arrived-at-pickup operational event.
- [x] Driver fare adjustment request ledger entry.
- [x] Admin payment list/detail/event projections.
- [x] Admin cash collection projection.
- [x] Driver earnings summary/transactions with MVP commission/settlement math.
- [x] Lightweight `packages/api-contracts` contract inventory generated from OpenAPI.
- [x] Full backend tests and runtime health passed.

Phase 06 may start next: realtime location ingestion/cache/events/WebSocket foundation.


### Phase status audit cleanup — 2026-06-02 00:35 +0530

- [x] Rechecked all roadmap sections before Phase 06 for stale incomplete markers.
- [x] Phase 00, 01, 02, 03, 04, 05, and 05.5 now show complete/complete-for-gate status consistently.
- [x] Remaining unchecked work starts at Phase 06 or belongs to later mobile/admin/hardening phases.


## Phase 07 Task 01 update — 2026-06-14 00:16 +0530

- Passenger mobile contract reconciliation is documented in `docs/api/PASSENGER_MOBILE_CONTRACT_RECONCILIATION.md`.
- `@routeshare/passenger-mobile` now has a typed API client, endpoint modules, DTO adapters, base URL config, and unit tests.
- Native E2E/preview verification remains for Task 02 because the Expo/native scaffold is not implemented yet.

## Phase 07 Task 02 update — 2026-06-14 01:32 +0530

- Passenger mobile Expo scaffold is implemented and runnable.
- Added React Navigation shell, provider stack, environment profiles, app config, EAS/Detox config, lint/typecheck/test/doctor scripts, and local preview/e2e smoke gates.
- Web export/render smoke passed from `/tmp/routeshare-passenger-web-export`.
- Task 03 may now start on real passenger screen flows.


## Phase 07 Task 03 update — 2026-06-14 18:20 +0530

- Passenger mobile app shell/navigation/state/offline foundation is implemented and verified.
- Added full typed passenger route map, deep-link config, startup route guard, preference defaults/validation, offline-aware query/mutation policy, expanded auth store, offline banner, and route placeholder shell screens.
- Verification passed: lint, typecheck, unit tests (`9` files / `40` tests), local iOS/Android e2e smoke gates, and local iOS/Android preview config gates.
- Next: Task 04 design system and reusable screen components from source assets.


## Phase 07 Task 04 update — 2026-06-14 19:05 +0530

- Passenger mobile design-system foundation is implemented and verified.
- Added `src/design-system/` tokens, reusable primitives, RouteShare components, deterministic map backdrop abstraction, and source-asset-matched home/shell previews.
- Verification passed: lint, typecheck, unit tests (`11` files / `47` tests), local iOS/Android e2e smoke gates, local iOS/Android preview config gates, and Android debug assemble.
- Next: Task 05 onboarding/auth Keycloak and OTP experience.


## Phase 07 Task 05 update — 2026-06-14 19:15 +0530

- Passenger mobile onboarding/auth foundation is implemented and verified.
- Added auth feature modules for Sri Lankan phone validation, OTP state machine, Keycloak PKCE token exchange/refresh helpers, secure token storage/logout helpers, and provider capability config.
- Added real Splash, Onboarding, Login, and OTP screens and wired auth routes in `RootShell`.
- Verification passed: lint, typecheck, unit tests (`15` files / `57` tests), local iOS/Android e2e smoke gates, local iOS/Android preview config gates, and Android debug assemble.
- Phone OTP remains provider-dependent and is explicitly gated by config rather than faked.
- Next: Task 06 profile setup and verification.


## Phase 07 Task 06 update — 2026-06-15 02:43 +0530

Completed passenger profile and safety prerequisite management. Added profile setup, account summary, saved places, trusted contacts, and verification readiness screens; wired route registration and deep links for these screens; expanded typed passenger DTOs/adapters/API modules for profile, saved places, trusted contacts, and verification status; added profile feature validation, avatar preparation/upload simulation, backend body adapters, primary/default preference helpers, and Task 06 unit coverage.

Verification passed:

- `pnpm --filter @routeshare/passenger-mobile lint`
- `pnpm --filter @routeshare/passenger-mobile typecheck`
- `pnpm --filter @routeshare/passenger-mobile test`
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios`
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android`
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios`
- `pnpm --filter @routeshare/passenger-mobile build:preview:android`
- Unit suite result: `16` files / `62` tests.

Notes: verification document submission and avatar binary storage are honest readiness/local shells until production storage/document-review APIs are implemented. Real device/simulator Detox and remote EAS submission remain later release evidence under the existing release-evidence blocker.

In progress: Task 07 — home, search, location, and route discovery. Production completion is blocked until Google Maps Platform keys are supplied, real map/place integration is verified, strict Android Maestro regression is green, and iOS runtime evidence is captured.

Next: obtain/configure Google Maps Platform keys for Task 07, rebuild the dev client, verify real map/place flows on device, stabilize Task 07 device QA, then Task 08 — results list/map filtering and ride detail.

## Phase 07 Task 08 update — 2026-06-21

Implemented Task 08 — results list, results map, grouped results, filtering, and ride detail.

- Backend search enrichment: `RouteSearchResponse` (and the matching candidate query) now carry
  `estimatedFare` (computed from the matched on-route distance via `FareCalculator`, ×seats),
  `matchedDistanceMeters`, `driverName`, and `vehicleMake/Model/Registration/SeatCount`, so the
  results/detail screens render real fare/driver/vehicle data instead of placeholders.
- New passenger feature module `features/ride-results` (RN-free, unit-tested): `toRideResultModel`
  normalization (match tier, fare/departure/seat/walk labels), `applyRideFilters` (min match,
  max fare, min seats, sort by best-match/price/departure/seats), and `groupRideResults` by overlap
  tier.
- New screens `SearchResultsScreen` (list/map/grouped toggle, sort + match-threshold filter chips,
  ride cards with `MatchRing`/`Avatar`/fare/driver/vehicle/seats, accessible map card fallback,
  empty state) and `RideDetailScreen` (map header, driver card, route timeline, fare estimate,
  why-good-match, safety/policy, continue→seat-selection booking handoff). Registered in the
  navigator; `RideDetail`/`SeatSelection` params now carry the selected result + coordinates + seats.
- Ride search confirmed stateless: removed the redundant GET result/result-detail shells in the
  backend finalization commit; results are carried via navigation state.

Verification: `pnpm --filter @routeshare/passenger-mobile typecheck | lint | test` (18 files /
80 tests), Android e2e scaffold gate, and Android preview-build config gate all pass. Backend
`./mvnw spotless:check verify` green (180 tests).

Android device QA: GREEN — `task08-results-list-map-filtering-ride-detail.yaml` passed 1/1 (2m35s) on
the Pixel_9 emulator against the live stack with a seeded Colombo Fort → Nugegoda route (real data
across list/map/grouped + ride detail; fare LKR 1,206). Evidence:
`qa/reports/20260621-111049-task08-final-green/`. iOS device evidence is a later release-evidence
follow-up.

Next: Task 09 — seat selection, booking idempotency, and cancellation.

---

## Phase 08 — ComiGo Unified App Backend

Status: `IN_PROGRESS`
Added: 2026-07-31
Plan: `docs/development/implementation/tasks/comigo-unified-app-backend/`
QA: `qa/test-cases/comigo-unified-app-backend/`

Supersedes the separate passenger/driver app split (Decision 011). The `ComiGo Prototype (Standalone).html`
supplied on 2026-07-31 is the new specification — ~157 screen states with a machine-readable `POLICY`
block (Decision 012). Decoded source of record: `docs/source-assets/comigo-prototype/`.

Gap against the existing backend: ~65 capabilities, of which 45 do not exist, 2 are built to a different
rule, and 18 have foundations but no product behaviour. Full register in
`tasks/comigo-unified-app-backend/00-prototype-gap-analysis.md`.

| Slice | Title | Size | Status |
| --- | --- | --- | --- |
| 00 | Repo reset and contract rewrite | M | `COMPLETED` 2026-08-01 |
| 01 | Auth unification and mode gates | M | `COMPLETED` 2026-08-02 — runtime smoke 12/12 |
| 02 | Vehicle classes and rate bands | M | `COMPLETED` 2026-08-02 — runtime smoke 18/18; both V028 triggers fired |
| 03 | Fare engine rewrite | XL | `COMPLETED` 2026-08-02 — runtime smoke 8/8; both V029 CHECK constraints fired; V029 needed a missing `CREATE SCHEMA platform` |
| 04 | Charge timing and capture correctness | L | `PARTIALLY_COMPLETED` 2026-08-02 — `CaptureOnTripStartIT` written and passing; cash smoke 2/2. The card path has never run against any gateway (Blocker 015), and its capture checks have been silently skipping because no trip rows existed until slice 05 (Blocker 016) |
| 05 | Trip timers and reliability | XL | `COMPLETED` 2026-08-02 — all four clocks, panels, deactivation trigger and prepay flag; runtime smoke 42/42. V032 materialises trips at first confirmed booking, without which no clock could ever fire. Void on auto-cancel still mock-only (Blocker 015) |
| 06 | Penalties, dues and compensation | L | `COMPLETED` 2026-08-02 — all five kinds assessed, the 50/50 split enforced by CHECK and by property test, three collection paths, dues carried and disputes with a 48-hour window; runtime smoke 44/44. V033 adds the `penalty` schema. The two card-collection paths (netted, card-charge) remain unrun — no gateway (Blocker 015) |
| 07 | Booking depth: seats, approval modes, expiry | L | `COMPLETED` 2026-08-02 — named seats with the race settled by a partial unique index, approval modes, 30-minute request expiry, the computed freeze rule, driver cancellation windows and §6.1 phone disclosure; runtime smoke 50/50. V034. Found and fixed: 500 on a missing header, a rollback poisoning the identity cache, and admins locked out of a driver's trip |
| 08 | Preferences, verification and eligibility | L | `COMPLETED` 2026-08-02 — six driving preferences inherited by every trip, women-only gated twice, verified-only enforced in the search query and at the booking guard by one service, four camera-only captures under a human reviewer, photo visibility with the driver asymmetry, verified-first request ranking and a real `eligibility-impact` count; runtime smoke 50/50. V035. Verification is provably never a booking gate |
| 09 | Search and discovery v2 | M | `COMPLETED` 2026-08-03 — the radius now filters on where the driver's trip *starts*, at 5/10/20 km with a 20 km ceiling; filtered-out count in the same statement; tiers derived from the discount band; enriched cards needing no client arithmetic; three stable sorts; commuter dashboard; short link and server-rendered QR; three-tier pickup points with a cost-ordered chain. Runtime smoke 41/41. V036. Found and fixed: the specified GIST index was on the geometry while the query filters over geography, so it was silently ineligible |
| 10 | Chat, notifications, safety and support | L | `COMPLETED` 2026-08-03 — booking-scoped audited chat with a 24-hour post-drop-off close job; unified inbox, twelve-row channel matrix, broadcasts, truthful S14 badges and driving deferral; contextual SOS with trusted-contact delivery accounting; validated support attachments; persisted settings and account requests. Runtime smoke 33/33. V037 |
| 11 | Referral and rewards | L | `NOT_STARTED` |
| 12 | Real-time location pipeline | L | `NOT_STARTED` |
| 13 | Live (en-route) booking | XL | `NOT_STARTED` |
| 14 | Money operations: payouts, ledger, adjustments | M | `NOT_STARTED` |
| 15 | Ratings and reviews v2 | M | `NOT_STARTED` |

Critical path is 00 → 01 → 02 → 03 → 04. Nothing downstream is trustworthy until money moves at the right
moment for the right amount. Slices 08–15 can parallelise across two workstreams once 04 lands, except
that 13 cannot start before 12.

New backend modules: `penalty`, `rewards`, `chat`, `reliability`, `scheduling`; `location` is rewritten.
Migrations: `V027`–`V041` (Decision 015 permits in-place semantic changes until launch).
Live-booking architecture (Decisions 016 + 017): `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md`.
Target scale: 500 trips/day, ~200 concurrent, 300 ceiling — H3, Kafka and sharding declined with recorded
revisit thresholds; PostGIS is the only spatial dependency.
Google cost is a design constraint (Decision 018): ETA and live-request detour are derived from stored
geometry and observed speed rather than bought; pickup-point resolution is cost-ordered with Places last;
the location pipeline adds zero calls. Modelled steady state ~$132/month, inside the $200 credit.

Phase exit: `tasks/comigo-unified-app-backend/release-readiness-checklist.md` fully ticked.

## Phase 09 — ComiGo Unified Mobile App

Status: `NOT_STARTED`
Added: 2026-07-31

The ~157 ComiGo screens built against `apps/mobile`. A separate feature folder, planned after the backend
contract stabilises at slice 00. Phase 07 (passenger mobile app) is superseded; its verified work — design
system, API client, auth and profile features, and the green Maestro suites — is harvested into
`apps/mobile` by slice 00 rather than discarded.
