# RouteShareApp Blockers

Last Updated: 2026-06-16 (Android Task 07 Search-screen device QA green)

## Purpose

This file tracks anything that blocks or slows implementation.

Blocker Status Values:

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `DEFERRED`

---

## Active Blockers

### Blocker 016 — Slice 04's card-capture checks have been skipping since they were written

Status: `OPEN`
Severity: `MEDIUM`

Description:

`scripts/simulation/verify-charge-timing.sh` guards its capture checks with
`if [ -n "$TRIP_ID" ]` and otherwise logs `SKIP: no trip row for the booked route`. Nothing in the
application created a `trip.trip` row until slice 05's `V032` work, so that branch was taken on
every run the script has ever had — including the run recorded under Blocker 013 as slice 04's
evidence. The skip was logged but never promoted into the tracking docs, so slice 04 reads as
verified when three of its checks never executed.

A second defect in the same script was found while writing slice 05's: it posts to
`/api/v1/driver/trips/{tripId}/transitions`, which does not exist. The real endpoint is
`/api/v1/driver/trips/{tripId}/start`. That would have failed loudly had the checks ever run.

Impact:

- "Trip start captures every card, exactly once" and "a repeated start captures nothing further"
  are unverified at runtime, despite `CaptureOnTripStartIT` proving the database cannot admit a
  double capture.
- The recorded 2/2 result for slice 04 overstates what was actually exercised.

Resolution:

Fix the endpoint path in `verify-charge-timing.sh` and re-run it now that bookings materialise
trips. Both defects are the script's, not the backend's; no product change is expected. Worth doing
before slice 06 prices anything against those paths.

---

### Blocker 015 — The card path of charge timing has never been exercised at runtime

Status: `OPEN`
Severity: `MEDIUM`

Description:

With Blocker 013 cleared, `scripts/simulation/verify-charge-timing.sh` now runs — but it verifies
only the cash path (2/2 checks) and then reports
`SKIP: no stored card on this stack; card-path checks not run`. There is no payment gateway on the
local stack, so no stored card exists to authorise against.

This means slice 04's headline behaviour — authorise at booking, capture at trip start, void on
cancel — is proven by unit tests and by `CaptureOnTripStartIT` (which proves the database cannot
admit a double capture), but **the authorise → capture → void sequence has never run end to end
against anything**, not even a fake gateway.

Impact:

- The eight card-path checks in
  `qa/test-cases/comigo-unified-app-backend/04-charge-timing-and-capture-qa.md` are unverified.
- Slice 05's start-buffer auto-cancel calls `PaymentFacade.voidForBooking`, so it inherits the same
  gap: the void will be asserted against a mock, not observed. Confirmed on 2026-08-02 when slice 05
  closed — `verify-trip-timers.sh` asserts that auto-cancel *captures* nothing, which it can check
  against real rows, but the void itself is still only proven by unit tests.

Update 2026-08-02: the project owner reports the Cybersource sandbox is temporarily unavailable and
will supply sandbox credentials (merchant ID, key ID, REST shared secret) when it returns. The
gated local fake adapter was considered for slice 05 and deliberately left out of scope to keep that
slice to the timers; it remains the cheaper of the two resolutions.

Resolution:

Either stand up a local fake gateway adapter behind the existing provider port (enough to let the
scripted flow store a card and move an authorisation through its states), or complete the
Cybersource sandbox credentials tracked under Blocker 011 and point the local stack at it. The
former is cheaper and does not depend on a third party; it must be clearly gated so a fake can never
be selected outside local profiles.

---

### Blocker 014 — `Vehicle` contract field names do not match the API

Status: `OPEN`
Severity: `MEDIUM`

Description:

`docs/api/mobile-app.openapi.json` describes `Vehicle` with `year`, `passengerSeatCapacity` and
`verificationStatus`. `VehicleResponse` returns `manufactureYear`, `seatCount` and `status`. A client
generated from the contract would read three fields that never arrive.

Found during slice 02 while adding `classKey`, `bandStatus` and `chosenRatePerKm` to the same schema
(those three do match). The drift predates the slice — it survived slice 00's reconciliation because
that pass compared paths and operations, not property names.

Impact: `GET/POST /api/v1/driver/vehicles` only. No runtime failure today, since no generated client
is in use yet.

Resolution:

Decide which side is authoritative — the contract reads better, the API is what exists — then change
one and reconcile in a single commit. Worth doing before the mobile feature plan wires D06/D07, and
worth a property-level sweep of the whole contract at the same time, since this class of drift would
not be caught by the checks slice 00 ran.

---

### Blocker 013 — Local stack will not start: host port 5433 is taken

Status: `RESOLVED` 2026-08-02
Severity: `LOW`

Description (as originally recorded):

`scripts/dev-up.sh` fails at `routeshare-postgres` with *Bind for 0.0.0.0:5433 failed: port is already
allocated*. The port is published by an unrelated project's container on this machine
(`cryptopilot-db-postgres-1`, running since well before this work).

**The original diagnosis was wrong on the point that mattered.** The port conflict was real, but it
was never what stopped the Testcontainers tests: Testcontainers binds ephemeral ports and never
touches 5433. Those tests were failing for an unrelated reason that the port story masked, and
because `@Testcontainers(disabledWithoutDocker = true)` turns "cannot reach Docker" into a silent
*skip*, the suite stayed green and nobody was told.

Two independent causes, both now fixed:

1. **Docker API version floor.** Docker Engine 29.1.3 rejects API `<= 1.41` with HTTP 400 (verified
   directly against the socket: `v1.32` → 400, `v1.41` → 400, `v1.44` → 200). The docker-java
   shaded into Testcontainers `1.19.8` negotiates from that floor, so every Testcontainers test had
   been skipping. Fixed by `testcontainers.version` → `1.21.3` **and** by passing `api.version` as a
   surefire system property — docker-java reads the system property, *not* the `DOCKER_API_VERSION`
   environment variable, which is why setting the env var appears to do nothing.
2. **The host port**, which was always only a `dev-up.sh` problem. `ROUTESHARE_POSTGRES_PORT` was
   already parameterised in `infra/docker-compose/docker-compose.yml`; local `.env` now uses `5434`
   (5432 is taken by `odoo-db`, 5433 by `cryptopilot-db-postgres-1`). The unrelated containers were
   left running.

What the first real run then found — none of which was visible under review:

- **`V029` could never have run.** It creates `platform.policy_setting` but no migration ever
  created the `platform` schema; Flyway failed at `SQL State 3F000, schema "platform" does not
  exist`. Fixed in place (Decision 015; the migration had never been applied anywhere).
- `V001`–`V030` now apply cleanly against real PostGIS, including slice 02's two PL/pgSQL triggers
  and slice 03's two fare-quote CHECK constraints — all four fired correctly in the smoke runs.
- `FlywayPostgisMigrationIntegrationTest` asserted the latest version was `12`; it had been
  silently skipping since V013 and now asserts `030`.
- **Unmapped paths returned HTTP 500, not 404.** `NoResourceFoundException` had no handler and fell
  through to the catch-all. Every client typo — and every endpoint deliberately removed, such as
  slice 03's `POST /pricing/estimate` — answered "the server is broken". Fixed in
  `GlobalExceptionHandler`.
- The local dev Keycloak realm gave `passenger-mobile` and `driver-mobile` direct access grants but
  omitted them for `admin-web`, so no simulation script could ever obtain an admin token. Aligned.
- `seed-demo-route.sh` predated slice 02: it sent `seatCount: 4` for a CAR whose class cap is now 3,
  omitted the now-required `vehicleClass`, and never assessed a rate band — which slice 02 made a
  precondition of publishing.
- Two smoke-script assertions were unreachable by construction rather than wrong about the backend:
  `data_of` ran money through `json.loads`, turning `49.50` into the float `49.5`; and the fare
  fixture asked for an 11.4 km slice of a corridor that is only ~9.5 km long.

Slice 04's `CaptureOnTripStartIT` is now written and passing: 20 threads capture the same booking
simultaneously, exactly one is admitted and 19 are refused by the unique index on
`payment_attempt.idempotency_key` with SQLSTATE `23505`. That is the property the mocks could not
prove.

Resolution / current state:

```bash
bash scripts/dev-up.sh                      # Postgres on 5434
cd apps/api && ./mvnw spotless:check verify # Testcontainers now runs rather than skipping
```

Runtime smoke results are recorded in `qa/reports/` and summarised per slice in
`DEVELOPMENT_STATUS.md`. **One gap remains and is tracked as Blocker 015**: the card path in
`verify-charge-timing.sh` still skips, because there is no payment gateway on the local stack.

---

### Blocker 012 — `admin-web.openapi.json` fails OpenAPI 3.1 validation

Status: `OPEN`
Severity: `LOW`

Description:

`docs/api/admin-web.openapi.json` declares `openapi: 3.1.0` but uses the OpenAPI **3.0** `nullable`
keyword, which is invalid in 3.1 and silently ignored by 3.1 tooling. `redocly lint` reports 8 struct
errors. Pre-existing since commit `0bd5fdf` (Phase 06.6-K); untouched by the ComiGo slice 00 work,
which fixed the same class of defect in the merged mobile contract.

Impact:

- A generated admin client would treat nullable fields as non-nullable.
- Low urgency: `apps/admin-web` is still a README stub, so nothing consumes this contract yet.

Recommended Action:

- Apply the same `nullable` → type-union conversion used on `mobile-app.openapi.json`, and add
  `redocly lint` for both documents to the verification gate.
- Do it when admin-web implementation starts, or as a standalone chore.


### Blocker 011 — Google Maps Platform keys required for Task 07 production map/search

Status: `IN_PROGRESS`
Severity: `HIGH`

Description:

Task 07 includes Home Map A, current location, Search Places, suggestions, and coordinate-based route discovery. This is a production RouteShare application stage, so fake maps, placeholder geocoding, or manual-only search cannot be treated as a complete implementation.

Required configuration:

```env
GOOGLE_MAPS_ENABLED=true
GOOGLE_MAPS_SERVER_API_KEY=...
EXPO_PUBLIC_GOOGLE_MAPS_ANDROID_API_KEY=...
EXPO_PUBLIC_GOOGLE_MAPS_IOS_API_KEY=...
```

Impact:

- Task 07 cannot be marked production-release-complete until Google Maps/Places credentials are supplied, implemented, and verified on real Android/iOS dev-client builds.
- Task 08 Results Map and Task 11 live trip tracking will depend on the same map foundation.

Recommended Action:

- Obtain/configure Google Maps Platform server, Android, and iOS keys with the required APIs enabled and appropriate restrictions.
- Rebuild/reinstall the Expo dev client after setting native Android/iOS map keys.
- Capture Android and iOS runtime evidence under ignored `qa/reports/` before closing Task 07.

Progress 2026-06-16 09:50 +0530:

- Google Maps keys were supplied and stored in local `.env`; values are intentionally not committed or printed.
- Backend passenger place-search proxy endpoints were implemented for Google Places autocomplete/details using the server key.
- Passenger mobile API and Search screen now call backend Places endpoints and require selected Google-backed places to resolve coordinates before ride search.
- Direct Google Places API smoke returned HTTP 200 with suggestions.
- Remaining to close: rebuild/reinstall Android/iOS dev clients with native map keys and capture device runtime evidence.

### Blocker 009 — Passenger mobile Expo native/prebuild config needs a decision

Status: `OPEN`
Severity: `MEDIUM`

Description:

`expo-doctor` reports that native project folders are present while `app.config.ts` still contains fields normally synced by Expo prebuild/CNG. EAS Build will not automatically sync fields such as scheme, orientation, userInterfaceStyle, ios, android, and plugins when native folders are committed and treated as source.

Impact:

- Local Android debug build can run, but preview/release build configuration may drift if native projects are not explicitly synchronized.
- The team must decide whether the passenger app is managed/prebuild-generated or native-folder-owned.

Recommended Action:

- Choose one path:
  - Managed/CNG path: remove generated native folders from source and use `expo prebuild` during build generation.
  - Native-owned path: keep native folders and manually synchronize all `app.config.ts` native settings into Android/iOS projects.
- Re-run `pnpm exec expo-doctor` after the decision.

### Blocker 010 — Passenger mobile later screens remain placeholder-level

Status: `OPEN`
Severity: `HIGH`

Description:

Login, OTP, Profile Setup, Home, and Account have been aligned closer to supplied designs, but many later passenger screens are still placeholder-level rather than exact implementations of the supplied UI/flow.

Impact:

- The passenger app cannot yet be called fully implemented from creation through the full ride lifecycle.
- Exact user journey QA is limited until Search, Results, Ride Detail, Seat Selection, Payment, Booked/Waiting, In-Trip, Receipt, Rating, History, Safety/SOS, Share Trip, Notifications, and Support are implemented.

Recommended Action:

- Continue Phase 07 screen implementation in route-flow order: Search -> Results -> Ride Detail -> Seat Selection -> Payment -> Booked/Waiting -> In-Trip -> Receipt -> Rating.
- Keep running lint/typecheck/unit tests and Android emulator screenshots after each screen slice.

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


### Blocker 008 — Passenger mobile native E2E and preview build commands need Expo scaffold

Status: `DEFERRED`
Severity: `MEDIUM`

Description:

Task 01 implemented the pure TypeScript passenger API client and contract reconciliation before UI work. The required native commands currently fail through `apps/passenger-mobile/scripts/native-blocker.mjs` because the Expo/native app scaffold, iOS/Android device automation, and preview build pipeline belong to Task 02.

Impact:

- Task 01 typed client can be linted, typechecked, and unit-tested.
- Full iOS/Android E2E and preview release evidence cannot be produced until Task 02 replaces the blocker scripts with real Expo/native commands.

Recommended Action:

- In Task 02, scaffold the Expo app and replace `test:e2e:ios`, `test:e2e:android`, `build:preview:ios`, and `build:preview:android` with real commands.

### Blocker 003 — Full backend completion still requires larger product workflows

Status: `DEFERRED`
Severity: `MEDIUM`

Description:

The backend foundation is verified, but the full product backend still needs several complete workflows:

- Upload/storage integration for KYC/document binaries.
- Deeper route matching integration/performance tests with realistic volumes.
- Realtime WebSocket updates and location event outbox foundation are implemented in Phase 06.
- Notifications/support/SOS, advanced admin management/reporting APIs, and real provider payout batches are later product/hardening phases.
- Integration/security tests.

Impact:

- Current backend can run and validate the core foundation, but it is not yet a production-complete RouteShareApp backend.

Recommended Action:

- Phase 06 realtime/event foundation is complete. Add realistic-volume route matching and realtime fanout performance tests before scale claims.

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


## Blocker 007 — App backend readiness gaps before full UI phases

Status: `RESOLVED`

Resolved by Phase 06.5 backend readiness closure. Passenger, Driver, Admin, and App Config app-facing endpoint mappings are implemented and verified. See `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

Impact:

- Full Passenger Mobile, Driver Mobile, and Admin Web implementation would hit backend-blocked screens.
- Admin Web has the largest gap and should not start end-to-end until backend admin APIs are closed.

Recommended resolution:

- Add and complete `Phase 06.5 — App Backend Readiness Closure`, or explicitly reduce/feature-flag app scopes before starting UI implementation.

## Blocker 008 — Passenger mobile native scaffold missing

Status: `RESOLVED_FOR_SCAFFOLD` as of 2026-06-14 01:32 +0530

Task 02 replaced the Task 01 explicit native blockers with a runnable Expo scaffold, Detox config, EAS preview config, and local smoke gates.

Resolved evidence:

- Expo app config loads.
- Expo Doctor passes `21/21` checks.
- Web export/render smoke passes.
- `test:e2e:ios`, `test:e2e:android`, `build:preview:ios`, and `build:preview:android` now pass local scaffold/config gates.

Remaining release-evidence follow-up:

- Run real simulator/device Detox tests after native projects/devices are generated.
- Submit real EAS preview builds after EAS project credentials and distribution targets are finalized.


## Task 03 status 2026-06-14 18:20 +0530

No new blocker was opened for Task 03. Local lint/typecheck/unit test, iOS/Android scaffold e2e smoke gates, and iOS/Android preview config gates passed. Real EAS cloud submissions and full device/simulator Detox execution remain later release-evidence follow-up once credentials/devices are finalized, consistent with Blocker 008's remaining notes.


## Task 04 status 2026-06-14 19:05 +0530

No new blocker was opened for Task 04. Local lint/typecheck/unit test, iOS/Android scaffold e2e smoke gates, iOS/Android preview config gates, and Android native debug assemble passed. Real EAS cloud submissions and full device/simulator Detox execution remain later release-evidence follow-up once credentials/devices are finalized, consistent with Blocker 008's remaining notes.


## Task 05 status 2026-06-14 19:15 +0530

No local implementation blocker remains for Task 05. Lint, typecheck, unit tests, iOS/Android scaffold e2e smoke gates, iOS/Android preview config gates, and Android debug assemble all passed.

External dependency noted: production phone OTP requires backend/provider readiness. The app does not fake OTP delivery; phone OTP is gated by an explicit environment capability flag with a user-facing provider dependency message otherwise. Full real-device Detox and remote EAS evidence remain later release evidence once credentials/devices are finalized.

## Blocker 011 — Production external service provider decisions

Status: Partially resolved — providers selected for SMS/maps/payments/push/monitoring; credentials and real integrations still required. Not blocking local/dev implementation that stays behind explicit provider ports and capability flags.

Production release requires selecting/configuring/testing the external providers documented in `docs/development/PRODUCTION_EXTERNAL_SERVICES.md`:

- **Notify.lk** credentials/API details for real phone verification/login.
- **Google Maps Platform** project/API keys for maps/places/geocoding/directions/route snapping.
- **Cybersource** merchant credentials/webhook setup and confirmed auth/preauth, capture, void, refund support.
- S3-compatible object storage provider for KYC/document/profile/support files remains deferred until needed.
- **Firebase Cloud Messaging** Firebase project/service credentials for push notifications.
- **Sentry** DSNs/auth token/projects for mobile/backend/admin monitoring.
- Production PostgreSQL/PostGIS, Redis, Redpanda/Kafka, secrets management, EAS/app-store credentials, domain/DNS/TLS hosting.

Implementation rule: do not fake provider-backed production success. Use ports/adapters and local/dev fakes only when gated and clearly documented.


## Blocker 012 — Notify.lk production OTP sender approval

Status: `OPEN_FOR_PRODUCTION`, not blocking local backend/mobile integration.

RouteShare now has a real Notify.lk backend adapter and backend-owned OTP request/verify flow. The current Notify.lk screenshots show active sender ID `NotifyDEMO`; Notify.lk docs warn not to send OTP content through the demo sender.

Required before enabling production phone OTP:

- Obtain/approve a RouteShare-branded Notify.lk sender ID.
- Set backend environment variables: `NOTIFY_LK_ENABLED=true`, `NOTIFY_LK_API_USER_ID`, `NOTIFY_LK_API_KEY`, and `NOTIFY_LK_SENDER_ID`.
- Keep `NOTIFY_LK_ALLOW_DEMO_SENDER_FOR_OTP=false` in production.
- Set the passenger app phone-OTP capability flag only for environments where backend Notify.lk credentials and sender are ready.


## Task 06 status 2026-06-15 02:43 +0530

No local implementation blocker remains for Task 06. Lint, typecheck, unit tests, iOS/Android scaffold e2e smoke gates, and iOS/Android preview config gates passed.

Remaining release-evidence follow-ups are covered by existing blockers: full real-device/simulator Detox automation and remote EAS submissions. Production verification document review and avatar binary storage still require provider/backend storage/document endpoints before public-release enablement; the Task 06 UI labels those paths as readiness/local shells rather than fake production success.


Android native rebuild progress 2026-06-16 10:05 +0530:

- Android native Maps SDK key wiring was added to the checked-in native project.
- Android debug build completed successfully.
- Rebuilt APK installed successfully on emulator.
- Emulator UI dump confirmed a native Google Map view renders in the app shell.
- Remaining blocker scope: authenticated Search screen device QA for Google Places autocomplete/details and coordinate-backed ride search evidence.

Android Search-screen device QA progress 2026-06-16 (later):

- Backend startup bug fixed: `GooglePlaceSearchService` was missing `@Autowired` on its production constructor, so the API failed to start; the Places proxy now boots.
- Full Android regression (`qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml`) now PASSES end-to-end on `emulator-5554` against the real Google Places integration. The SearchResults handoff carried real resolved coordinates (pickup `6.9336686, 79.8500469`; dropoff `6.8649081, 79.8996789`). Evidence under ignored `qa/reports/20260616-224636/`.
- Maestro flows corrected to drive the real Google-Places suggestion-selection UX (tap "Search pickup/destination places" → tap `📍` suggestion → "Search shared rides"); `hideKeyboard` removed because Android Maestro sends BACK.
- Remaining to close Blocker 011: iOS simulator/device runtime evidence for the same map/place/search path.


## 2026-06-21 — Phase 07 Task 08 device-QA blocker (results list/map/grouped + ride detail)

Task 08 implementation is complete and statically verified: `pnpm --filter
@routeshare/passenger-mobile typecheck | lint | test` (18 files / 80 tests) all pass, plus the
Android e2e scaffold gate and preview-build config gate. The Maestro regression flow is authored at
`qa/maestro/passenger-mobile/regression/task08-results-list-map-filtering-ride-detail.yaml`.

Open blocker — full device QA could not be run in this environment:

1. No emulator/device was attached (`adb devices` empty); a `Pixel_9` AVD exists but was not booted here.
2. The ride-card and ride-detail assertions require **seeded PUBLISHED driver-route inventory**
   whose corridor matches the Colombo Fort → Nugegoda search window. Without a published
   route+occurrence+bucket cells (which need an approved driver + approved vehicle), the results
   screen correctly renders the "No rides match yet" empty state and the card/detail steps are not
   reachable. The list/map/grouped/filter UI itself is reachable without inventory.

To close: boot the emulator with Metro on 8082 + API on 8080 (Google Maps enabled, demo OTP), seed a
matching published route, run the task08 flow, and save evidence under ignored `qa/reports/`.

## 2026-06-21 (later) — Task 08 device QA RESOLVED (green on emulator)

The Task 08 device-QA blocker is cleared. Ran the full Maestro regression on the Pixel_9 Android
emulator against the live stack (API 8080 + Metro 8082 + Postgres/Keycloak/Redis, Google Maps
enabled, demo OTP) with a seeded PUBLISHED Colombo Fort → Nugegoda route:

- `qa/maestro/passenger-mobile/regression/task08-results-list-map-filtering-ride-detail.yaml` →
  **1/1 Flow Passed (2m35s)**. Evidence: `qa/reports/20260621-111049-task08-final-green/`
  (results-list, filtered, map, grouped, ride-detail screenshots + maestro-junit.xml).
- Real backend data rendered: 100% match, driver "Saman Fernando", "Toyota Aqua · CAB-7788",
  "3 seats left", computed fare **LKR 1,206**, across list / map / grouped views and ride detail.

Findings fixed during the run (not app defects): the map caption was rendered behind the native
Google map surface (moved it above the map); Maestro regex assertions are full-string matches
(anchored the map/Continue patterns with `.*`); the ride-detail Continue button needed
scroll-to-visible. Earlier transient failures were emulator-environment noise (Maestro driver
startup timeout; a Digital Wellbeing system ANR over the onboarding screen; a stale Metro instance
on 8082 serving an old bundle — replaced with a fresh Metro from the working tree).

Note: seeding inventory directly is a QA convenience; a production-faithful alternative is to publish
a route via the driver API. iOS device evidence remains a later release-evidence follow-up.

## 2026-06-21 (later) — Passenger regression suite green on emulator (Tasks 07 + 08)

Re-ran both passenger regression flows on the Pixel_9 emulator against the live stack in one session:
- Task 07 (`task07-home-search-route-discovery.yaml`): 1/1 Passed (2m4s).
- Task 08 (`task08-results-list-map-filtering-ride-detail.yaml`): 1/1 Passed (2m43s);
  earlier authoritative run with full screenshots: `qa/reports/20260621-111049-task08-final-green/`.

Hardened the Task 07 flow login/OTP steps with `scrollUntilVisible` (keyboard-robust), matching Task 08.
Note: the standalone Task 07 failures before this were the OTP request **rate limit** (5/hour per number)
being exhausted by repeated QA runs on +94770000042 — the app correctly displayed
"Too many requests. Please try again later." For repeatable QA the local backend was run with
`ROUTESHARE_RATE_LIMIT_ENABLED=false`; production keeps the limiter on (fail-open).
