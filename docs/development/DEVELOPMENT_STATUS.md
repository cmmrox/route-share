# RouteShareApp Development Status

2026-06-15 09:25 +0530

## Purpose

This file is the first file to read before continuing RouteShareApp development. It shows the current phase, completed work, active work, pending work, blockers, verification status, and the next recommended task.

## Current State

- Implementation Planning Standard: `docs/development/IMPLEMENTATION_PLANNING_STANDARD.md` defines the required `docs/development/implementation-tasks/<feature-plan-name>/` structure and production-ready task-file rules.
- Current Phase: `PHASE_07_PASSENGER_MOBILE_APP_STARTED`
- Current Milestone: `MILESTONE_PASSENGER_TYPED_API_CLIENT_READY`
- Current Active Task: `Task 01 passenger API contract reconciliation and typed client implemented; native app scaffold/preview evidence continues in Task 02`
- Status: `PASSENGER_API_CLIENT_CORE_VERIFIED`
- Repository Git Status: `Working tree has Phase 07 Task 01 passenger mobile/API contract changes plus prior planning/source-asset changes`

## Estimated Progress

- Completed known implementation tasks: 81
- Total known high-level tasks: 95+
- Estimated overall progress: 64%

> Progress is estimated from known tasks and will change as requirements are added or split into smaller implementation tasks. Phases 00 through 06 are now closed for the Phase 07 gate. Later product areas such as realtime websockets, notifications/support/SOS, real payment-provider integration, full mobile/admin UI implementation, observability, and production hardening remain in their own later phases.


## Latest Verification Update — 2026-06-15 09:25 +0530

Status: `PASSENGER_OTP_KEYCLOAK_AND_ANDROID_SMOKE_VERIFIED_WITH_CONFIG_WARNINGS`

Completed in this verification pass:

- Backend phone OTP verification now links/creates a Keycloak user and assigns the `PASSENGER` realm role.
- Phone OTP access token subject now uses the Keycloak user id instead of a local `phone:+94...` subject.
- Local live smoke verified Keycloak user creation, role assignment, `/api/v1/auth/me`, and local `identity.app_user.keycloak_subject` mapping.
- Passenger mobile Login, OTP, Profile Setup, Home, and Account screens were adjusted closer to supplied passenger design references and first-run flow requirements.
- OTP resend now stores and verifies against the latest backend `verificationId` and has an active countdown.
- Local/dev passenger app config now enables phone OTP by default because the backend provider path is implemented; staging/production still require explicit provider enablement.

Verification evidence:

- Backend focused tests passed: `KeycloakPhoneVerifiedIdentityServiceTest`, `PhoneOtpServiceImplTest`.
- Backend full Maven test run completed with no Surefire failures/errors; Testcontainers Docker availability warning remains environment-dependent.
- Passenger mobile `lint`, `typecheck`, and `test` passed: 16 test files, 62 tests.
- Passenger mobile E2E scaffold/config gates passed for iOS and Android.
- Passenger mobile preview build config gates passed for iOS and Android.
- Expo iOS export bundled successfully from `apps/passenger-mobile/index.ts`.
- Android emulator `emulator-5554` built, installed, and rendered the app through Metro on port 8081.
- Android screenshots captured for onboarding and corrected Login screen; Login no longer shows the stale red Phone OTP blocker.

Remaining known issues:

- `expo-doctor` reports one CNG/native-folder warning: native `android/` exists while app config contains prebuild-managed fields.
- Android dev-client shows a non-fatal Expo CLI websocket warning toast in development; the app still renders.
- Many later passenger screens remain placeholder-level and still need exact supplied-design implementation: search, results, ride detail, seat selection, payment, booked/waiting, in-trip, receipt, rating, history, safety/SOS, share trip, notifications, and support.

## Completed So Far

- [x] Keycloak/auth architecture documented.
- [x] One Keycloak user can act as passenger and/or driver.
- [x] Backend owns business profiles by Keycloak subject.
- [x] PostgreSQL/PostGIS multi-schema architecture documented.
- [x] OpenAPI/Swagger planning documents created under `docs/api/`.
- [x] Development tracking files created under `docs/development/`.
- [x] Repository skeleton exists with backend, app, infrastructure, scripts, and docs folders.
- [x] Local Docker infrastructure created for PostgreSQL/PostGIS, Redis, Redpanda, Keycloak, and MinIO.
- [x] Dev scripts patched for non-interactive SSH shells: PATH, `DOCKER_CONFIG`, and `docker-compose` usage.
- [x] Spring Boot 3 / Java 21 backend scaffolded in `apps/api`.
- [x] Flyway migrations created and applied.
- [x] Module schemas and foundation tables created for identity, passenger, driver, vehicle, routing, booking, trip, location, pricing, payment, and common idempotency.
- [x] OAuth2 resource-server security configured.
- [x] Keycloak role converter hardened to trust `api-monolith` resource roles only.
- [x] Common API response and error handling added.
- [x] Identity projection/upsert from JWT implemented.
- [x] `GET /api/v1/auth/me` implemented.
- [x] Passenger profile API implemented.
- [x] Driver application/profile API implemented.
- [x] Vehicle create/list API implemented with deterministic `INSERT ... RETURNING` creation.
- [x] Admin driver review API implemented with enum-backed status validation.
- [x] Pricing estimate domain/service endpoint implemented.
- [x] Route publishing endpoint implemented with explicit coordinate DTO validation.
- [x] Route publishing hardened to require approved driver profile and approved vehicle ownership.
- [x] Booking endpoint moved into application service with transactional seat decrement.
- [x] Trip transition endpoint moved into application service with ownership/admin authorization and state machine validation.
- [x] Location update endpoint moved into application service with current-driver ownership and timestamp freshness validation.
- [x] Payment intent endpoint moved into application service and no longer accepts client-controlled amount/currency; amount is derived from booking fare and currency defaults server-side to LKR.
- [x] Unit tests added for payment service and route service hardening.
- [x] Existing domain tests pass.
- [x] Runtime API health verified after the latest hardening patches.
- [x] Added Flyway V004 hardening constraints for active payment intent uniqueness and positive booking fare estimates.
- [x] Booking now derives and stores fare estimates during booking creation.
- [x] Route publishing validates future departure time and requested seats against approved vehicle capacity.
- [x] Location updates now verify the active trip belongs to the current driver profile.
- [x] Trip transitions now run transactionally and lock the trip status row before transition.
- [x] Suspended/deleted local app users are blocked after JWT identity projection.
- [x] Saved places CRUD APIs implemented and verified.
- [x] Trusted contacts CRUD APIs implemented and verified.
- [x] Driver document metadata APIs implemented and verified.
- [x] Vehicle document metadata APIs implemented and verified.
- [x] Admin vehicle review API implemented and verified.
- [x] Backend persistence refactored away from service-layer queries/JdbcTemplate into Spring Data JPA repositories under infrastructure.
- [x] Lombok-backed JPA entities/repositories added for backend persistence boundaries.
- [x] Spotless/google-java-format configured and applied to backend Java sources.
- [x] Architecture tests added to prevent JdbcTemplate in main sources, database APIs/SQL in application services, and repositories outside infrastructure.

- [x] Approved backend architecture simplified from hexagonal `port/in` / `port/out` naming to learner-friendly Spring Boot modular monolith packages.
- [x] Backend packages refactored to `controller`, `dto`, `mapper`, `service`, `service/impl`, `facade`, `facade/impl`, `domain`, `entity`, and `repository` for implemented modules.
- [x] Module facades added for identity, passenger, driver, vehicle, routing, booking, and cross-module calls now use facades instead of foreign repositories/entities.
- [x] MapStruct dependency and shared `RouteShareMapperConfig` added; driver/passenger/vehicle mappers now use MapStruct.
- [x] Architecture tests expanded for service/impl, facade, mapper config, controller, entity/repository, and cross-module boundary rules.
- [x] Architecture documentation updated with the approved service/impl + facade approach.
- [x] Java 21 virtual threads enabled for Spring Boot request/task execution with bounded Hikari database pool settings.

- [x] Phase 04 route schedule rules, route occurrences, and route bucket indexing committed.
- [x] Route search now exposes route occurrence identity and matched pickup/drop route fractions for booking handoff.
- [x] Booking creation now reserves seats against `routing.route_occurrence` instead of abstract `routing.route_plan`.
- [x] Booking rows now store `route_occurrence_id`, `pickup_route_fraction`, and `dropoff_route_fraction`.
- [x] Booking creation now writes initial `CONFIRMED` status history to `booking.booking_status_history`.
- [x] Booking creation now requires an explicit HTTP `Idempotency-Key` and replays completed matching responses from `common.idempotency_key` without reserving seats twice.
- [x] Booking status transitions for cancel/reject/complete are implemented with same-transaction status history rows.
- [x] Passenger boarded/no-show/drop-off state machine implemented per booking on the concrete route occurrence.
- [x] Immutable fare ledger foundation records booking fare estimates before payment intent creation/replay.
- [x] Payment capture, void, refund, driver cash collection, and passenger receipt foundation implemented.
- [x] Driver earnings summary/transactions, MVP platform commission, and settlement-balance read models implemented.
- [x] Route share link/QR payload foundation implemented.
- [x] Driver pre-trip checklist, arrived-at-pickup, and fare-adjustment request endpoints implemented.
- [x] Admin payment list/detail/events and cash collection projections implemented.
- [x] Lightweight TypeScript workspace and `packages/api-contracts` endpoint inventory generated.
- [x] Structured logging conventions documented.
- [x] Testcontainers Flyway/PostGIS migration smoke test added; it auto-skips when the Java Docker client cannot connect.

- [x] Passenger/driver/admin OpenAPI contracts audited against the business requirement PDF and supplied passenger/driver designs.
- [x] Missing product APIs added to `docs/api/passenger-app.openapi.json`, `docs/api/driver-app.openapi.json`, and `docs/api/admin-web.openapi.json`.
- [x] API gap analysis documented in `docs/api/API_GAP_ANALYSIS.md`.
- [x] Roadmap now has explicit API contract gates before passenger mobile, driver mobile, and admin web implementation.
- [x] Blocker 006 added for API contract/backend reconciliation.
- [x] API backend reconciliation document created at `docs/api/API_BACKEND_RECONCILIATION.md`.
- [x] First app-facing backend alias controllers implemented with TDD:
  - Passenger ride search create alias.
  - Passenger booking create/cancel aliases.
  - Passenger payment intent alias.
  - Driver route create alias.
  - Driver trip start/complete and passenger board/no-show/drop-off aliases.
- [x] Targeted alias controller tests and full backend `spotless:check test` passed.

## In Progress

- [x] Passenger mobile Task 01 typed API client implemented and verified for lint/typecheck/unit tests. Native E2E/preview evidence is deferred to Task 02 Expo scaffold.

- [x] Phase 06 realtime location and WebSocket foundation is complete. Phase 07 Passenger Mobile is in progress.

## Completed Phase-Gate Closure

- [x] Phase 05 booking, trip, fare, payment, MVP earnings/commission/settlement-balance read models are implemented for the Phase 06 gate.
- [x] Phase 05.5 app-facing backend/API reconciliation is complete enough for the Phase 06 gate. Realtime-dependent, notification/support/SOS, real provider, and UI/client implementation items are explicitly deferred to later phases.

## Pending Roadmap Summary

- [x] Phase 00 — Project architecture and file structure.
- [x] Phase 01 — Local development environment.
- [x] Phase 02 — Backend modular monolith foundation.
- [x] Phase 03 — Identity, passenger, driver, KYC/document metadata, vehicle, saved places, trusted contacts, and vehicle review foundation APIs are implemented.
- [x] Phase 04 — Route publishing and route matching. Route search, schedule rules, route occurrences, and bucket-cell broad filtering are implemented and committed.
- [x] Phase 05 — Booking, trip lifecycle, fare, payment, settlement. Booking occurrence inventory, idempotency, status history, passenger trip states, immutable fare ledger, payment capture/void/refund, cash collection, receipt foundation, driver earnings, MVP commission, and settlement-balance read models are implemented for the Phase 06 gate.
- [x] Phase 06 — Realtime location and WebSocket updates.
- [x] Phase 06.5 — App Backend Readiness Closure completed.
- [~] Phase 07 — Passenger mobile app in progress: Tasks 01–05 complete; Task 06 profile setup and verification next.
- [ ] Phase 08 — Driver mobile app.
- [ ] Phase 09 — Admin web app.
- [ ] Phase 10 — Hardening, observability, performance, deployment readiness.

## Latest Verification

- Passenger mobile Task 01:
  - Command: `pnpm --filter @routeshare/passenger-mobile lint` — passed.
  - Command: `pnpm --filter @routeshare/passenger-mobile typecheck` — passed.
  - Command: `pnpm --filter @routeshare/passenger-mobile test` — passed (`3` files, `23` tests).
  - Native E2E/preview commands intentionally fail with a documented blocker until Task 02 creates the Expo/native scaffold.

- TypeScript contract package:
  - Command: `pnpm install && pnpm --filter @routeshare/api-contracts typecheck`.
  - Result: passed.
- Maven backend tests and formatting:
  - Command: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw spotless:apply spotless:check test -q`
  - Result: `BUILD SUCCESS`.
- Virtual thread configuration:
  - `spring.threads.virtual.enabled=true` configured in `application.yml`.
  - HikariCP pool bounds configured with `ROUTESHARE_DB_POOL_MAX_SIZE`, `ROUTESHARE_DB_POOL_MIN_IDLE`, and `ROUTESHARE_DB_CONNECTION_TIMEOUT_MS`.
- Architecture verification:
  - `PersistenceArchitectureTest` passes.
  - Enforces no `JdbcTemplate` in main sources, no SQL/low-level database APIs in service implementations, repositories under `repository`, entities under `entity`, service implementations under `service/impl`, facades under `facade/impl`, controllers not importing repositories/entities, MapStruct shared mapper config usage, and no cross-module repository/entity/impl imports.
- Runtime health:
  - `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}` after the pre-Phase-06 closure.
- Database migration:
  - Latest Flyway migration is version `012`, success `true` in the running app migration history.
  - Verified `payment.fare_ledger_entry`, `routing.route_share_link`, and `trip.pre_trip_checklist` exist in the running database.
  - Verified `common.idempotency_key` exists and is used by booking create replay handling.
  - Verified `booking.booking_status_history` exists and is used for initial and transition audit rows; `booking.booking` has occurrence/fraction columns; `trip.passenger_trip_state` exists; `trip.trip` has `route_occurrence_id`; and `payment.fare_ledger_entry` exists.
- Git status:
  - Latest committed backend slice before this work is the prior Phase 05 baseline.
  - Pre-Phase-06 closure changes are committed in the latest pre-Phase-06 gate commit.

## Blockers / Risks

- No active runtime blocker for the backend foundation.
- Git baseline exists; pre-Phase-06 closure changes are committed; current working tree is clean.
- Full product is not complete yet, but phases 00 through 05.5 are complete for the Phase 06 gate. Remaining major areas are Phase 06+ or later hardening/app phases.
- Current tests are mostly unit-level. Add Spring Boot integration/security tests before relying on these APIs as production-ready.
- Dev infrastructure exposes local ports and uses local-only development credentials; do not reuse these settings for production.

## Next Recommended Task

Continue Phase 07 with Task 06 — profile setup and verification, now that Task 05 onboarding/auth/OTP foundation is complete.

## Update Rule

After every completed implementation task, update:

- `DEVELOPMENT_STATUS.md`
- `IMPLEMENTATION_ROADMAP.md`
- `TASK_LOG.md`

If relevant, also update:

- `DECISION_LOG.md`
- `REQUIREMENTS_CHANGE_LOG.md`
- `BLOCKERS.md`
- `SESSION_SUMMARIES/YYYY-MM-DD-session-summary.md`


## 2026-06-01 23:43 +0530 — Phase 05/05.5 continuation before Phase 06

Completed additional core backend API reconciliation before Phase 06:

- Passenger booking list/detail/current trip/history projections.
- Driver route list/detail/cancel endpoints.
- Driver trip list/detail and booking request projections.
- Driver booking approve/decline commands with driver-owned authorization path and booking status history reuse.

Verification:

- Targeted controller tests passed.
- Full backend `spotless:apply spotless:check test` passed.
- API restarted and `/actuator/health` returned HTTP 200.

Remaining before/around Phase 06:

- Payment lifecycle capture/void/refund/cash/earnings/settlement.
- Receipt/final fare endpoints.
- Share link/QR, pre-trip checklist, arrived pickup, notifications/support/SOS/admin depth.


## 2026-06-02 00:35 +0530 — Audit cleanup before commit

The earlier docs still showed stale incomplete states for phases before Phase 06 even though the implementation had been added. This audit corrected those stale statuses:

- Phase 00 TypeScript workspace/package setup is now present with root `package.json`, `pnpm-workspace.yaml`, `packages/api-contracts/package.json`, and `tsconfig.json`.
- Phase 02 migration list, structured logging convention, and Testcontainers migration smoke coverage are now tracked.
- Phase 03 is marked completed for its foundation scope.
- Phase 05 and Phase 05.5 are marked completed for the Phase 06 gate.
- Deferred product workflows are now labeled as Phase 06+ or later-phase work instead of pre-Phase-06 blockers.


## 2026-06-02 02:35 +0530 — Phase 06 completed

Phase 06 realtime location foundation is complete and verified. Latest work adds driver location ingestion, Redis latest-location cache, auditable sample/outbox persistence, WebSocket/STOMP fanout, passenger live-state endpoint, and admin live trip feed.


## 2026-06-02 01:45 +0530 — App backend readiness audit

Audited the business requirement, passenger/driver designs, app implementation plans, OpenAPI contracts, and backend controllers before starting Passenger Mobile, Driver Mobile, or Admin Web. Result: Phase 06 backend foundation is complete, but app-facing backend is not fully complete for end-to-end app implementation. Created `docs/api/APP_BACKEND_READINESS_AUDIT.md` and added recommended `Phase 06.5 — App Backend Readiness Closure` to the roadmap.

Summary gaps found: Passenger 24 missing/deferred contract operations, Driver 28, Admin 43. Some can be deferred or feature-flagged, but safety/support/notifications/ratings/early-drop-off/recurring routes/KYC submit/admin operations should be closed before full app phases.


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

## 2026-06-14 01:32 +0530 — Phase 07 Task 02 Expo passenger app scaffold completed

Passenger Mobile Task 02 is complete for the runnable scaffold/dev-tooling scope. `@routeshare/passenger-mobile` now starts as an Expo React Native TypeScript app with React Navigation, provider composition, environment profiles, strict TypeScript, ESLint/Prettier, Vitest tests, Expo Doctor, EAS preview config, Detox config, and local preview/e2e smoke gates.

Verified:

- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed (`6` files / `28` tests).
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `pnpm run doctor` passed Expo Doctor `21/21` checks.
- `pnpm exec expo export --platform web --output-dir /tmp/routeshare-passenger-web-export` passed and rendered the current scaffold UI.

Next step: Task 03 app shell/navigation/state/offline foundation.


## 2026-06-14 18:20 +0530 — Phase 07 Task 03 app shell/navigation/state/offline foundation completed

Passenger Mobile Task 03 is complete for the app-shell foundation scope. The app now has typed route contracts for public/protected passenger routes, startup route-guard state logic, offline-aware query/mutation policy, persisted preference defaults/validation, expanded auth state, deep-link prefixes/config, an offline banner, and placeholder shell screens for the full passenger route map.

Verified:

- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed (`9` files / `40` tests).
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.

Notes:

- Task 03 uses placeholder shell screens for route coverage; user-visible production screen designs start in Task 04.
- Real cloud EAS submissions and full device/simulator Detox flows remain later release-evidence work when credentials/devices are finalized.

Next step: continue with Task 04 — design system and reusable screen components from source assets.


## 2026-06-14 19:05 +0530 — Phase 07 Task 04 design system and reusable screen components completed

Passenger Mobile Task 04 is complete for the reusable design-system foundation scope. Added source-asset-matched warm RouteShare tokens, dark palette tokens, semantic/match-tier colors, spacing/radius/shadow/type scales, reusable accessible primitives, RouteShare-specific components, a deterministic map backdrop abstraction, and a redesigned app shell/home preview using those components.

Verified:

- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed (`11` files / `47` tests).
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.
- Android native debug assemble passed: `./gradlew app:assembleDebug -x lint -x test --configure-on-demand --build-cache -PreactNativeDevServerPort=8082 -PreactNativeArchitectures=arm64-v8a --console=plain`.

Notes: real EAS cloud submissions and full device/simulator Detox flows remain later release evidence once credentials/devices are finalized.

Next step: continue with Task 05 — onboarding/auth Keycloak and OTP experience.


## 2026-06-14 19:15 +0530 — Phase 07 Task 05 onboarding/auth completed

Passenger Mobile Task 05 is complete for the first-run auth experience foundation. Implemented real Splash, three-slide Onboarding, Login, and OTP screens; Sri Lankan mobile validation; Keycloak Authorization Code + PKCE URL/token/refresh helpers; secure token persistence/logout helpers; OTP state machine coverage for empty/focused/paste/invalid/resend/throttle/network states; and auth route wiring so public auth routes no longer use the generic shell placeholder.

Verification passed:

- `pnpm --filter @routeshare/passenger-mobile lint`
- `pnpm --filter @routeshare/passenger-mobile typecheck`
- `pnpm --filter @routeshare/passenger-mobile test` (`15` files / `57` tests)
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android`
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios`
- `pnpm --filter @routeshare/passenger-mobile build:preview:android`
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios`
- Android debug assemble passed with Gradle: `BUILD SUCCESSFUL in 2s`.

Phone OTP dependency: current config does not assume production phone OTP support. The UI validates phone numbers and documents provider readiness; phone OTP navigation is gated by an explicit environment capability flag.

Next step: Task 06 — profile setup and verification.

## Production External Services

- [ ] Production external service providers selected and integrated. See `docs/development/PRODUCTION_EXTERNAL_SERVICES.md`.

## Public Release Provider Decisions

- Selected providers for public release: Notify.lk SMS/OTP, Google Maps Platform, Cybersource payments, Firebase Cloud Messaging, and Sentry. See `docs/development/SELECTED_PROVIDER_IMPLEMENTATION_GUIDE.md`.


## 2026-06-14 — Notify.lk OTP integration update

- Real backend-owned Notify.lk OTP integration is implemented after Task 05.
- Added `/api/v1/auth/otp/request` and `/api/v1/auth/otp/verify` public endpoints.
- Added hashed OTP persistence in `identity.phone_otp_challenge` via Flyway `V015__add_phone_otp_challenges.sql`.
- Passenger mobile Login/OTP screens now call backend OTP endpoints when the passenger phone-OTP capability flag is enabled. Production enablement still requires an approved RouteShare Notify.lk sender ID; `NotifyDEMO` is intentionally blocked for OTP by default.
- Production enablement still requires an approved RouteShare Notify.lk sender ID; `NotifyDEMO` is intentionally blocked for OTP by default.

Verification: backend targeted tests, backend `spotless:apply spotless:check test`, passenger mobile `typecheck`, `lint`, and `test` passed.


## 2026-06-15 02:43 +0530 — Phase 07 Task 06 profile/safety prerequisites completed

Passenger Mobile Task 06 is complete for the app-side profile and safety prerequisite scope. Implemented real RouteShare screens for Profile Setup, Account, Saved Places, Trusted Contacts, and Verification readiness; registered these routes in the typed passenger navigator/deep links; expanded passenger mobile API modules and DTO adapters; and added profile feature modules for validation, avatar handling, backend body mapping, verification copy, and default/primary preference helpers.

Implemented behavior:

- Profile setup saves `fullName`, optional email through `preferences.email`, and `photoUrl` through the backend profile API adapter.
- Avatar flow validates JPG/PNG/WebP and max 5 MB, exposes progress/cancel/retry-friendly state, and uses initials fallback. Binary storage remains a local/readiness shell until storage/upload endpoints are added.
- Account menu links to profile, saved places, trusted contacts, and verification.
- Saved places support list/add/delete/default selection with manual coordinate/address fallback and offline/error/empty states.
- Trusted contacts support list/add/delete/primary selection with Sri Lankan mobile validation, contact-import permission copy, and SOS/share-trip explanation.
- Verification uses honest readiness-only copy because live passenger document review/upload backend endpoints are not available in this slice.

Verified:

- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed.
- Unit suite result: `16` files / `62` tests.

Next step: Task 07 — home, search, location, and route discovery.
## 2026-06-15 — Local QA/runtime cleanup, OTP bypass, Keycloak profile sync, avatar picker

- Removed duplicate `routeshare-postgres-alt` usage and normalized local Docker to `routeshare-postgres` on host port `5433` so it does not conflict with the existing Odoo Postgres on `5432`.
- Started and verified local RouteShare services: Postgres, Redis, Keycloak, and MinIO.
- Added local QA-only OTP bypass configuration: `ROUTESHARE_OTP_DEV_BYPASS_ENABLED=true` with static code `000000` in `.env`; `.env.example` keeps it disabled by default.
- Fixed passenger profile save so Keycloak standard user fields are synced from saved profile data: first name, last name, and email. Passenger-specific data such as `photoUrl` remains in RouteShare DB because the current Keycloak realm drops arbitrary custom attributes.
- Replaced the passenger profile image placeholder flow with real Expo image picker wiring and avatar preview.
- Installed Maestro on the Mac and added repeatable emulator QA scripts under `scripts/qa-*.sh` plus flows/reports under `qa/`.
- Verification completed: backend focused tests pass, backend `spotless:check test` exits 0, passenger mobile lint/typecheck/tests pass, and Maestro Android smoke passes on `emulator-5554`.
