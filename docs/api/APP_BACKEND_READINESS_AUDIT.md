# App Backend Readiness Audit — Passenger, Driver, Admin

Last Updated: 2026-06-02 01:45 +0530

## Purpose

Before starting Phase 07 Passenger Mobile App, Phase 08 Driver Mobile App, or Phase 09 Admin Web App, this audit checks the business requirement, supplied passenger/driver designs, app-facing OpenAPI contracts, and implemented Spring Boot controllers.

Source inputs:

- `docs/source-assets/rideshare-business-requirement.pdf`
- `docs/source-assets/Route-Based Ride-Sharing-Platform-designs.zip`
- `docs/implementation/07-PASSENGER-MOBILE-APP.md`
- `docs/implementation/08-DRIVER-MOBILE-APP.md`
- `docs/implementation/09-ADMIN-OPS-REPORTING.md`
- `docs/api/passenger-app.openapi.json`
- `docs/api/driver-app.openapi.json`
- `docs/api/admin-web.openapi.json`
- Spring Boot controllers under `apps/api/src/main/java/com/routeshare/**/controller`

## Executive verdict

Backend foundation through Phase 06 is complete, but **backend development is not fully complete for starting all passenger/driver/admin apps end-to-end**.

Do **not** start full mobile/admin UI implementation yet if the goal is to avoid blocked screens. Add a backend closure phase first:

- `Phase 06.5 — App Backend Readiness Closure`

This phase should implement or explicitly remove/defer every remaining app-facing contract endpoint that the MVP screens require.

## Current implemented foundation

Already implemented and verified:

- Auth identity projection: `GET /api/v1/auth/me`.
- Passenger profile, saved places, trusted contacts.
- Driver application/profile, document metadata, vehicles create/list, vehicle document metadata.
- Admin driver/vehicle review foundation through equivalent paths.
- Route publish/create, route search, occurrence generation foundation, route bucket matching.
- Booking create/list/detail/cancel/history/current, idempotency, seat inventory, status history.
- Driver route list/detail/cancel/share-link.
- Driver trips list/detail/booking requests/start/complete/pre-trip/arrived-pickup.
- Passenger boarded/no-show/drop-off by driver.
- Passenger payment intent foundation, capture/void/refund, cash collection, receipt, fare ledger.
- Driver earnings summary/transactions, MVP commission and settlement-balance read models.
- Phase 06 realtime foundation: driver location ingestion, Redis latest cache, event outbox, WebSocket/STOMP fanout, passenger live-state, admin live trips.

## Contract comparison summary

Approximate app-facing OpenAPI vs implemented-controller comparison:

- Passenger missing/deferred contract operations: `24`.
- Driver missing/deferred contract operations: `28`.
- Admin missing/deferred contract operations: `43`.

Some are acceptable later-phase hardening, but several are required before building the corresponding app screens from the current implementation plans.

## Passenger backend gaps before Phase 07

### Must close before full Passenger App

- `GET /api/v1/app/config`
  - Needed for mobile feature flags, map/payment config, support URLs, and rollout-safe UI behavior.
- `POST /api/v1/passenger/bookings/{bookingId}/early-drop-off`
  - Business requirement explicitly supports leaving before the driver final destination and recalculating fare by actual traveled distance.
- `POST /api/v1/passenger/bookings/{bookingId}/share` and/or `POST /api/v1/passenger/bookings/{bookingId}/share-link`
  - Safety requirement: trusted-contact trip sharing.
- `POST /api/v1/passenger/sos-events`
  - Safety requirement and passenger design safety surface.
- `POST /api/v1/passenger/bookings/{bookingId}/rating`
  - MVP requirement includes ratings/reviews.
- Support ticket flow:
  - `POST /api/v1/passenger/support/tickets`
  - `GET /api/v1/passenger/support/tickets`
  - `GET /api/v1/passenger/support/tickets/{ticketId}`
  - `POST /api/v1/passenger/support/tickets/{ticketId}/messages`
- Notifications/push foundation:
  - `GET /api/v1/passenger/notifications`
  - `POST /api/v1/passenger/notifications/{notificationId}/read`
  - `GET /api/v1/passenger/notification-preferences`
  - `PUT /api/v1/passenger/notification-preferences`
  - `POST /api/v1/passenger/push-registrations`
- Card/payment-method foundation if the Passenger App includes card selection before launch:
  - `GET /api/v1/passenger/payment-methods`
  - `POST /api/v1/passenger/payment-methods`
  - `DELETE /api/v1/passenger/payment-methods/{paymentMethodId}`
  - `POST /api/v1/passenger/payment-methods/{paymentMethodId}/default`

### Can be deferred or contract-adjusted for first UI slice

- `GET /api/v1/passenger/ride-searches/{searchId}/results`
- `GET /api/v1/passenger/ride-searches/{searchId}/results/{resultId}`

Current backend search is stateless and returns results from `POST /api/v1/passenger/ride-searches`. Either implement persisted search/results or update the Passenger App plan to keep selected result state locally and avoid those endpoints initially.

- Passenger avatar/verification upload:
  - `POST /api/v1/passenger/profile/avatar-upload`
  - `GET /api/v1/passenger/verification/status`
  - `POST /api/v1/passenger/verification/documents`

These are useful for fraud reduction, but can be deferred if launch does not require passenger KYC.

## Driver backend gaps before Phase 08

### Must close before full Driver App

- Driver verification status:
  - `GET /api/v1/driver/verification-status`
  - Needed to gate route publishing/trip operation in the app.
- Driver KYC/document submission lifecycle:
  - `PUT /api/v1/driver/kyc/identity`
  - `PUT /api/v1/driver/kyc/licence`
  - `POST /api/v1/driver/documents/{documentId}/submit`
  - Metadata APIs exist, but app screens need explicit submit/review-state behavior.
- Vehicle detail/update/delete:
  - `GET /api/v1/driver/vehicles/{vehicleId}`
  - `PUT /api/v1/driver/vehicles/{vehicleId}`
  - `DELETE /api/v1/driver/vehicles/{vehicleId}`
- Vehicle document submit lifecycle:
  - `POST /api/v1/driver/vehicles/{vehicleId}/documents/{documentId}/submit`
- Route publish contract mismatch:
  - Contract includes `POST /api/v1/driver/routes/{routeId}/publish` but backend currently treats `POST /api/v1/driver/routes` as create/publish foundation. Either add explicit publish endpoint or simplify the contract/app flow.
- Recurring route management:
  - `POST /api/v1/driver/recurring-routes`
  - `GET /api/v1/driver/recurring-routes`
  - `PUT /api/v1/driver/recurring-routes/{routeId}`
  - `DELETE /api/v1/driver/recurring-routes/{routeId}`
  - `POST /api/v1/driver/recurring-routes/{routeId}/generate-occurrences`
  - Business requirement calls recurring trips a major differentiator.
- Driver payout profile:
  - `GET /api/v1/driver/payout-profile`
  - `PUT /api/v1/driver/payout-profile`
  - Needed for earnings/payout screens, even if real payout execution is later.
- Driver ratings:
  - `GET /api/v1/driver/ratings`
- Driver SOS/support/notifications:
  - `POST /api/v1/driver/sos-events`
  - `GET /api/v1/driver/notifications`
  - `POST /api/v1/driver/notifications/{notificationId}/read`
  - `GET /api/v1/driver/notification-preferences`
  - `PUT /api/v1/driver/notification-preferences`
  - `POST /api/v1/driver/push-registrations`
  - `POST /api/v1/driver/support/tickets`
  - `GET /api/v1/driver/support/tickets`
  - `GET /api/v1/driver/support/tickets/{ticketId}`
  - `POST /api/v1/driver/support/tickets/{ticketId}/messages`

### Can be deferred only if the Driver App scope is reduced

- Full background location hardening/performance tests beyond Phase 06 foundation.
- Real payout batch execution.
- Advanced ratings analytics.

## Admin backend gaps before Phase 09

Admin backend is the least complete relative to the Admin Web contract. Phase 09 cannot be built end-to-end from current APIs without a backend closure slice.

### Must close before full Admin Web

- Dashboard:
  - `GET /api/v1/admin/dashboard`
- User management:
  - `GET /api/v1/admin/users`
  - `GET /api/v1/admin/users/{appUserId}`
  - `POST /api/v1/admin/users/{appUserId}/suspend`
  - `POST /api/v1/admin/users/{appUserId}/activate`
  - `PUT /api/v1/admin/users/{appUserId}/roles`
  - `GET /api/v1/admin/users/{appUserId}/status-history`
- Driver/vehicle verification dashboard under contract paths:
  - `GET /api/v1/admin/driver-applications`
  - `GET /api/v1/admin/driver-applications/{driverId}`
  - `POST /api/v1/admin/driver-applications/{driverId}/review` currently has an equivalent non-contract path `/api/v1/admin/drivers/{id}/review`; either alias or update contract.
  - `POST /api/v1/admin/driver-documents/{documentId}/review`
  - `GET /api/v1/admin/vehicles`
  - `GET /api/v1/admin/vehicles/{vehicleId}`
  - `POST /api/v1/admin/vehicle-documents/{documentId}/review`
  - `POST /api/v1/admin/documents/{documentId}/download-url`
- Trip operations:
  - `GET /api/v1/admin/trips`
  - `GET /api/v1/admin/trips/{tripId}`
  - `POST /api/v1/admin/trips/{tripId}/cancel`
  - `GET /api/v1/admin/trips/{tripId}/location-trail`
  - `GET /api/v1/admin/trips/live` is implemented.
- Booking operations:
  - `GET /api/v1/admin/bookings`
  - `GET /api/v1/admin/bookings/{bookingId}`
  - `GET /api/v1/admin/bookings/{bookingId}/status-history`
- Fare/commission configuration:
  - `GET /api/v1/admin/commission-rules`
  - `POST /api/v1/admin/commission-rules`
  - `PUT /api/v1/admin/commission-rules/{ruleId}`
  - `GET /api/v1/admin/fare-policies`
  - `POST /api/v1/admin/fare-policies`
  - `PUT /api/v1/admin/fare-policies/{policyId}`
- Settlements/finance operations:
  - `GET /api/v1/admin/settlements/driver-balances`
  - `GET /api/v1/admin/settlements/payout-batches`
  - `POST /api/v1/admin/settlements/payout-batches`
  - `POST /api/v1/admin/settlements/payout-batches/{batchId}/mark-paid`
  - `POST /api/v1/admin/finance/adjustments`
  - Payment list/detail/events/capture/void/refund and cash collection projections exist, but payout batch/discrepancy workflow does not.
- Support/SOS/notifications:
  - `GET /api/v1/admin/support/tickets`
  - `GET /api/v1/admin/support/tickets/{ticketId}`
  - `PUT /api/v1/admin/support/tickets/{ticketId}`
  - `POST /api/v1/admin/support/tickets/{ticketId}/messages`
  - `GET /api/v1/admin/safety/sos-events`
  - `GET /api/v1/admin/safety/sos-events/{sosEventId}`
  - `POST /api/v1/admin/safety/sos-events/{sosEventId}/resolve`
  - `POST /api/v1/admin/notifications/broadcasts`
- Reports/audit:
  - `GET /api/v1/admin/reports/summary`
  - `POST /api/v1/admin/reports/export`
  - `GET /api/v1/admin/audit/actions`

## Recommended Phase 06.5 implementation plan

Implement backend readiness in these slices before starting full app UI work:

### Slice A — shared app platform APIs

- App config endpoint.
- Push registration.
- Notification inbox/read/preferences.
- Support tickets/messages shared model.
- SOS events shared model.
- Audit event helper for admin mutations.

### Slice B — passenger app readiness

- Passenger early drop-off and fare adjustment/finalization foundation.
- Passenger share-trip/share-link endpoint.
- Passenger rating endpoint.
- Passenger payment methods/card placeholder abstraction if card UI remains in MVP.
- Optional: persisted ride-search result detail, or update contract/app plan to use stateless results.

### Slice C — driver app readiness

- Verification-status projection.
- Explicit KYC identity/licence APIs.
- Document submit endpoints.
- Vehicle detail/update/delete.
- Explicit route publish endpoint or contract simplification.
- Recurring route CRUD/generate occurrences.
- Payout profile read/update.
- Driver ratings, SOS, notifications, support.

### Slice D — admin web readiness

- Dashboard summary.
- User list/detail/status/role APIs.
- Driver application list/detail and contract-compatible review aliases.
- Driver/vehicle document review and document download URL.
- Vehicle list/detail.
- Admin trip list/detail/cancel/location-trail.
- Admin booking list/detail/status-history.
- Fare policy and commission rule APIs.
- Settlement driver balances and payout batch lifecycle.
- Admin support/SOS/notification/report/audit APIs.

## Go/no-go decision

- Passenger app can start only as a **reduced first UI slice** if missing safety/support/notifications/rating/early-drop-off/payment-method screens are stubbed or hidden behind feature flags.
- Driver app can start only as a **reduced first UI slice** if KYC submit, recurring routes, payout profile, support/SOS/notifications/ratings are stubbed or hidden.
- Admin web should **not** start end-to-end yet because most admin contract paths are still missing.

Recommended decision: complete `Phase 06.5 — App Backend Readiness Closure` first, then start Phase 07/08/09 with fewer blockers.
