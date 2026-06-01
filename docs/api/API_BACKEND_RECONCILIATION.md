# API Backend Reconciliation

Last Updated: 2026-06-01 23:20 +0530

## Purpose

This document reconciles the app-facing OpenAPI contracts in `docs/api/` with the Spring Boot backend controllers in `apps/api`.

Status values:

- `IMPLEMENTED` — backend exposes the contract path and delegates to business services.
- `ALIASED_THIS_SLICE` — implemented during the API reconciliation slice as an app-specific endpoint over existing service logic.
- `GENERIC_IMPLEMENTED` — backend has equivalent generic endpoint; app-specific contract still needs an alias or contract mapping decision.
- `PARTIAL` — some backend foundation exists, but contract behavior is not complete.
- `MISSING` — not implemented yet.
- `DEFERRED` — intentionally future/later MVP slice.

## Canonical endpoint decision

Decision for mobile/admin development:

- Keep domain services/facades generic internally.
- Expose stable app-facing controllers where they simplify clients:
  - Passenger: `/api/v1/passenger/...`
  - Driver: `/api/v1/driver/...`
  - Admin: `/api/v1/admin/...`
- Existing generic endpoints may remain for internal/admin/debug use, but generated app clients should target the app-facing contracts after reconciliation.

## Implemented in this slice

New app-facing backend aliases/controllers were added with TDD:

- `POST /api/v1/passenger/ride-searches` → `RouteService.search`
- `POST /api/v1/passenger/bookings` → `BookingService.book` with `Idempotency-Key`
- `POST /api/v1/passenger/bookings/{bookingId}/cancel` → `BookingService.transition(CANCELLED)`
- `POST /api/v1/passenger/payments/intents` → `PaymentService.createIntent`
- `POST /api/v1/driver/routes` → `RouteService.publish`
- `POST /api/v1/driver/trips/{tripId}/start` → `TripService.transition(STARTED)`
- `POST /api/v1/driver/trips/{tripId}/complete` → `TripService.transition(COMPLETED)`
- `POST /api/v1/driver/trips/{tripId}/passengers/{bookingId}/board` → `TripService.transitionPassengerState(BOARDED)`
- `POST /api/v1/driver/trips/{tripId}/passengers/{bookingId}/no-show` → `TripService.transitionPassengerState(NO_SHOW)`
- `POST /api/v1/driver/trips/{tripId}/passengers/{bookingId}/drop-off` → `TripService.transitionPassengerState(DROPPED_OFF)`

## Passenger contract reconciliation

### Passenger paths implemented or aliased

- `GET /api/v1/auth/me` — `IMPLEMENTED`
- `GET /api/v1/passenger/profile` — `IMPLEMENTED`
- `PUT /api/v1/passenger/profile` — `IMPLEMENTED`
- `GET /api/v1/passenger/saved-places` — `IMPLEMENTED`
- `POST /api/v1/passenger/saved-places` — `IMPLEMENTED`
- `PUT /api/v1/passenger/saved-places/{savedPlaceId}` — `IMPLEMENTED` as path variable `id`; contract name differs only.
- `DELETE /api/v1/passenger/saved-places/{savedPlaceId}` — `IMPLEMENTED` as path variable `id`; contract name differs only.
- `GET /api/v1/passenger/trusted-contacts` — `IMPLEMENTED`
- `POST /api/v1/passenger/trusted-contacts` — `IMPLEMENTED`
- `PUT /api/v1/passenger/trusted-contacts/{contactId}` — `IMPLEMENTED` as path variable `id`; contract name differs only.
- `DELETE /api/v1/passenger/trusted-contacts/{contactId}` — `IMPLEMENTED` as path variable `id`; contract name differs only.
- `POST /api/v1/passenger/ride-searches` — `ALIASED_THIS_SLICE`; stateless search returns current `RouteSearchResponse` list directly.
- `POST /api/v1/passenger/bookings` — `ALIASED_THIS_SLICE`; delegates to occurrence booking with idempotency.
- `POST /api/v1/passenger/bookings/{bookingId}/cancel` — `ALIASED_THIS_SLICE`; delegates to booking status transition.
- `POST /api/v1/passenger/payments/intents` — `ALIASED_THIS_SLICE`; delegates to payment intent creation.

### Passenger remaining gaps

- `GET /api/v1/passenger/ride-searches/{searchId}/results` — `DEFERRED`; current search is stateless and not persisted.
- `GET /api/v1/passenger/ride-searches/{searchId}/results/{resultId}` — `DEFERRED`; needs persisted search/result detail or contract change.
- `GET /api/v1/passenger/bookings` — `IMPLEMENTED_THIS_SLICE`; passenger-owned booking list projection.
- `GET /api/v1/passenger/bookings/{bookingId}` — `IMPLEMENTED_THIS_SLICE`; passenger-owned booking detail projection.
- `GET /api/v1/passenger/trips/current` — `IMPLEMENTED_THIS_SLICE`; active passenger booking/trip projection.
- `GET /api/v1/passenger/trips/{tripId}/live-state` — `PHASE_06`; intentionally starts with realtime/live-state implementation.
- `POST /api/v1/passenger/bookings/{bookingId}/early-drop-off` — `DEFERRED_AFTER_PHASE_06`; related to fare adjustment/UX hardening, not a Phase 06 blocker.
- `GET /api/v1/passenger/bookings/{bookingId}/receipt` — `IMPLEMENTED`; ledger-derived receipt foundation.
- `GET /api/v1/passenger/trips/history` — `IMPLEMENTED_THIS_SLICE`; completed/cancelled/rejected trip history projection.
- Payment method endpoints — `DEFERRED_PROVIDER_INTEGRATION`; cash/mock payment lifecycle foundation is implemented.
- Verification/avatar upload endpoints — `DEFERRED_UPLOAD_HARDENING`; metadata foundations exist, signed binary upload is later.
- Notifications/push/preferences — `DEFERRED_LATER_PHASE`; notification module is not a pre-Phase-06 blocker.
- SOS/share/support/rating endpoints — `DEFERRED_LATER_PHASE`; not a pre-Phase-06 blocker.
- `GET /api/v1/app/config` — `DEFERRED_LOW_RISK`; useful later, not a pre-Phase-06 blocker.

## Driver contract reconciliation

### Driver paths implemented or aliased

- `GET /api/v1/auth/me` — `IMPLEMENTED`
- `POST /api/v1/driver/application` — `IMPLEMENTED`
- `GET /api/v1/driver/profile` — `IMPLEMENTED`
- `POST /api/v1/driver/documents` — `IMPLEMENTED` as document metadata, not signed binary upload.
- `GET /api/v1/driver/documents` — `IMPLEMENTED`
- `POST /api/v1/driver/vehicles` — `IMPLEMENTED`
- `GET /api/v1/driver/vehicles` — `IMPLEMENTED`
- `POST /api/v1/driver/vehicles/{vehicleId}/documents` — `IMPLEMENTED` as document metadata, not signed binary upload.
- `GET /api/v1/driver/vehicles/{vehicleId}/documents` — `IMPLEMENTED`
- `POST /api/v1/driver/routes` — `ALIASED_THIS_SLICE`; delegates to route publish/create foundation.
- `POST /api/v1/driver/trips/{tripId}/start` — `ALIASED_THIS_SLICE`
- `POST /api/v1/driver/trips/{tripId}/complete` — `ALIASED_THIS_SLICE`
- `POST /api/v1/driver/trips/{tripId}/passengers/{bookingId}/board` — `ALIASED_THIS_SLICE`
- `POST /api/v1/driver/trips/{tripId}/passengers/{bookingId}/no-show` — `ALIASED_THIS_SLICE`
- `POST /api/v1/driver/trips/{tripId}/passengers/{bookingId}/drop-off` — `ALIASED_THIS_SLICE`
- `POST /api/v1/location/updates` — `IMPLEMENTED` as initial ingestion; Phase 06 realtime pipeline remains.

### Driver remaining gaps

- `GET /api/v1/driver/verification-status` — `DEFERRED_APP_PHASE`; profile/document/vehicle foundations exist.
- `PUT /api/v1/driver/kyc/identity` — `DEFERRED_UPLOAD_HARDENING`; current driver application/document metadata foundation exists.
- `PUT /api/v1/driver/kyc/licence` — `DEFERRED_UPLOAD_HARDENING`; current driver document metadata foundation exists.
- `POST /api/v1/driver/documents/{documentId}/submit` — `DEFERRED_UPLOAD_HARDENING`; metadata APIs exist, binary upload lifecycle later.
- `GET /api/v1/driver/vehicles/{vehicleId}` / `PUT` / `DELETE` — `DEFERRED_APP_PHASE`; create/list and admin review foundation exist.
- `POST /api/v1/driver/vehicles/{vehicleId}/documents/{documentId}/submit` — `DEFERRED_UPLOAD_HARDENING`.
- `GET /api/v1/driver/routes` / `{routeId}` / `POST /api/v1/driver/routes/{routeId}/cancel` / `POST /api/v1/driver/routes/{routeId}/share-link` — `IMPLEMENTED`.
- Recurring route CRUD/generation endpoints — `DEFERRED_APP_PHASE`; one-time route occurrence foundation exists for Phase 06 gate.
- `GET /api/v1/driver/trips` / `{tripId}` / `{tripId}/booking-requests` — `IMPLEMENTED_THIS_SLICE`; driver trip and booking request projections.
- Booking approve/decline — `IMPLEMENTED_THIS_SLICE`; driver-owned booking status transitions with authorization and status history.
- Pre-trip checklist / arrived pickup — `IMPLEMENTED`.
- Cash collection / fare adjustment / earnings — `IMPLEMENTED`; payout profile is `DEFERRED_PROVIDER_INTEGRATION`.
- Ratings, notifications, SOS, support — `DEFERRED_LATER_PHASE`; not a pre-Phase-06 blocker.

## Admin contract reconciliation

### Admin paths implemented

- `GET /api/v1/auth/me` — `IMPLEMENTED`
- `POST /api/v1/admin/drivers/{id}/review` — `IMPLEMENTED`; equivalent to driver application review but path differs from contract.
- `POST /api/v1/admin/vehicles/{id}/review` — `IMPLEMENTED`; vehicle review path added in contract as `/api/v1/admin/vehicles/{vehicleId}/review`.

### Admin deferred/later-phase gaps

Admin web backend has the finance projections needed before Phase 06. The following deeper admin product workflows are deferred to Phase 09/Admin or Phase 10 hardening:

- dashboard
- user search/detail/suspend/activate/status history/roles
- driver application list/detail under contract path
- driver/vehicle document review paths
- private document preview/download URL
- trips/live trips/trip detail/location trail/admin cancel
- bookings list/detail/status history
- commission/fare policy management
- payments/refunds/void/events — payment list/detail/events/capture/void/refund foundation is implemented; advanced review is deferred
- cash collection discrepancies — cash collection projection is implemented; discrepancy workflow deferred
- settlement balances/payout batches — settlement-balance read model is implemented; payout batch execution deferred
- finance adjustments
- support tickets/messages
- SOS event detail/resolve
- broadcasts
- reports/export
- audit action logs

## Next backend implementation recommendation

1. Add passenger booking list/detail projections so the passenger app can show booking status, current trip, history, and receipt later.
2. Add driver route list/detail/cancel and driver trip list/detail projections.
3. Implement manual booking approve/decline with driver authorization, not through the passenger cancel/complete endpoint.
4. Phase 05 payment lifecycle, cash collection, earnings, commission, and settlement-balance read models are implemented for the Phase 06 gate.
5. Phase 06 realtime can start after committing this verified closure.


## Implemented in continuation slice before Phase 06

Additional Phase 05/05.5 APIs implemented before starting Phase 06 realtime:

- `GET /api/v1/passenger/bookings`
- `GET /api/v1/passenger/bookings/{bookingId}`
- `GET /api/v1/passenger/trips/current`
- `GET /api/v1/passenger/trips/history`
- `GET /api/v1/driver/routes`
- `GET /api/v1/driver/routes/{routeId}`
- `POST /api/v1/driver/routes/{routeId}/cancel`
- `GET /api/v1/driver/trips`
- `GET /api/v1/driver/trips/{tripId}`
- `GET /api/v1/driver/trips/{tripId}/booking-requests`
- `POST /api/v1/driver/bookings/{bookingId}/approve`
- `POST /api/v1/driver/bookings/{bookingId}/decline`

Verification: targeted controller tests passed; full `spotless:apply spotless:check test` passed; API restarted and `/actuator/health` returned HTTP 200.


## Payment lifecycle continuation before Phase 06

Implemented at 2026-06-01 23:52 +0530:

- `POST /api/v1/admin/payments/{paymentIntentId}/capture` — `IMPLEMENTED_THIS_SLICE`; transitions `REQUIRES_CAPTURE -> CAPTURED` and writes `PAYMENT_CAPTURED` ledger entry.
- `POST /api/v1/admin/payments/{paymentIntentId}/void` — `IMPLEMENTED_THIS_SLICE`; transitions `REQUIRES_CAPTURE -> VOIDED` and writes `PAYMENT_VOIDED` ledger entry.
- `POST /api/v1/admin/payments/{paymentIntentId}/refund` — `IMPLEMENTED_THIS_SLICE`; transitions `CAPTURED -> REFUNDED` and writes negative `PAYMENT_REFUNDED` ledger entry.
- `POST /api/v1/driver/bookings/{bookingId}/cash-collected` — `IMPLEMENTED_THIS_SLICE`; verifies driver-owned booking fare and writes `CASH_COLLECTED` ledger entry.
- `GET /api/v1/passenger/bookings/{bookingId}/receipt` — `IMPLEMENTED_THIS_SLICE`; returns fare estimate, paid/refunded/cash totals, balance due, and ledger line items.

Migration added:

- `V011__expand_payment_lifecycle_ledger.sql` extends ledger entry types and permits negative refund rows.

Verification:

- RED test first: `PaymentLifecycleServiceTest` failed before DTO/service/repository methods existed.
- GREEN: `PaymentLifecycleServiceTest` passed after implementation.
- Full backend verification passed: `./mvnw -q spotless:apply spotless:check test`.
- Runtime health passed after restart: HTTP 200 `{"status":"UP"}`.

## 2026-06-02 00:08 +0530 — Pre-Phase-06 closure: driver ops, admin finance, earnings, contract inventory

Completed remaining practical backend/API work before Phase 06 realtime/location work:

Driver operational APIs:

- `POST /api/v1/driver/routes/{routeId}/share-link`
- `POST /api/v1/driver/trips/{tripId}/pre-trip-checklist`
- `POST /api/v1/driver/trips/{tripId}/arrived-pickup`
- `POST /api/v1/driver/bookings/{bookingId}/fare-adjustment-request`

Admin finance projections:

- `GET /api/v1/admin/payments`
- `GET /api/v1/admin/payments/{paymentIntentId}`
- `GET /api/v1/admin/payments/{paymentIntentId}/events`
- `GET /api/v1/admin/cash-collections`

Driver earnings/read models:

- `GET /api/v1/driver/earnings/summary`
- `GET /api/v1/driver/earnings/transactions`
- Uses ledger-derived gross earnings, 10% MVP platform commission, and settlement balance.

Schema/contracts:

- Added `V012__pre_phase06_operational_finance.sql` for pre-trip checklist, arrived-pickup event, route share link, and fare adjustment ledger entry type.
- Generated lightweight contract inventory in `packages/api-contracts/src/index.ts` from the Passenger/Driver/Admin OpenAPI files.

Verification:

- RED test first: `PrePhase06ControllerContractTest` initially failed because driver earnings controller, fare adjustment DTO, and pre-trip checklist DTO did not exist.
- Targeted `PrePhase06ControllerContractTest` passed after implementation.
- Full backend verification passed: `./mvnw -q spotless:apply spotless:check test`.
- Runtime API smoke passed after restart: `/actuator/health` HTTP 200.
- OpenAPI JSON syntax validation passed for Passenger, Driver, and Admin contracts.
- Backend source route check found implemented controller mappings for share-link, pre-trip-checklist, arrived-pickup, fare-adjustment-request, earnings summary/transactions, admin payments/events, and cash collections.

Status: previous backend/API phases are complete for the Phase 06 gate. Remaining deeper financial work, such as real provider integration and settlement payouts, is later hardening and not a blocker for Phase 06 realtime foundation.


## 2026-06-02 00:35 +0530 — stale gap audit before commit

Rechecked the reconciliation document after implementation. The earlier `MISSING` markers for receipt, route share link, pre-trip checklist, arrived pickup, cash collection, fare adjustment, earnings, admin payments, and admin cash collections were stale and are now marked implemented or complete for the Phase 06 gate.

Items still not implemented are explicitly deferred to Phase 06+ or later product phases:

- Phase 06: passenger live-state/realtime location pipeline.
- Later mobile/admin phases: notification/support/SOS/rating screens and related APIs.
- Later provider hardening: stored payment methods, signed upload/binary lifecycle, real provider settlement payout batches.

No remaining `MISSING` item in this document blocks starting Phase 06 after the current commit.

## 2026-06-02 00:42 +0530 — Phase completion audit, verification, and commit preparation

Completed the requested audit of all phases before Phase 06 and fixed stale incomplete markers that were still visible in the tracking docs.

Audit findings and corrections:

- Phase 00 had a stale unchecked TypeScript workspace/client setup item. Added root `package.json`, `pnpm-workspace.yaml`, `packages/api-contracts/package.json`, and `packages/api-contracts/tsconfig.json`; generated contract inventory is typecheckable.
- Phase 02 had stale unchecked migration/logging/Testcontainers items. Existing migrations are now documented accurately through `V012`; logging conventions are documented in `docs/development/LOGGING_CONVENTIONS.md`; added `FlywayPostgisMigrationIntegrationTest` as Testcontainers migration smoke coverage with automatic skip when Java Testcontainers cannot connect to Docker.
- Phase 03 had a stale partial-completion marker even though the foundation scope was done; marked completed.
- Phase 05 had a stale partial-completion marker with stale unchecked manual approve/decline, cash collection, earnings, commission, and settlement-balance items; updated to `COMPLETED_FOR_PHASE_06_GATE`.
- Phase 05.5 had a stale in-progress marker; updated to `COMPLETED_FOR_PHASE_06_GATE`.
- API reconciliation stale `MISSING` markers for receipt, route share link, pre-trip checklist, arrived pickup, cash collection, fare adjustment, earnings, admin payments, and admin cash collections were updated to implemented/complete-for-gate.
- Remaining non-implemented items were explicitly classified as Phase 06+ or later-phase scope, not pre-Phase-06 blockers.

Verification:

- `pnpm install` completed and `pnpm --filter @routeshare/api-contracts typecheck` passed.
- Backend `./mvnw -q spotless:apply spotless:check test` passed.
- Testcontainers migration smoke test is present; on this Mac Java Testcontainers cannot connect to Docker Desktop's socket and therefore auto-skips under `disabledWithoutDocker = true` during the full suite.
- Runtime restart passed: `/actuator/health` returned HTTP 200 with `{"status":"UP"}`.
- Running database latest migration check passed: `flyway_schema_history` latest version `012`, success `true`.
- Verified key pre-Phase-06 tables exist in the running database: `payment.fare_ledger_entry`, `routing.route_share_link`, and `trip.pre_trip_checklist`.

Verdict: phases 00 through 05.5 are complete for the Phase 06 gate and committed.


## 2026-06-02 01:15 +0530 — Phase 06 realtime location foundation completed

Implemented Phase 06 backend foundation:

- Driver active-trip location ingestion endpoint: `POST /api/v1/driver/trips/{tripId}/location-updates`.
- Location validation for timestamp freshness, accuracy, speed, and impossible jumps against latest cached snapshot.
- Redis latest-location cache with 30 second TTL.
- Auditable PostgreSQL sample persistence and `location.location_event_outbox` for event-stream handoff.
- WebSocket/STOMP endpoint `/ws` with trip topic `/topic/trips/{tripId}/location` and admin topic `/topic/admin/trips/live`.
- Passenger live trip state endpoint: `GET /api/v1/passenger/trips/{tripId}/live-state`.
- Admin live trip feed endpoint: `GET /api/v1/admin/trips/live`.

Verification recorded after implementation: targeted Phase 06 tests pass, full backend tests pass, runtime health passes, Redis ping passes, and Flyway migration `013` succeeds.
