# RouteShareApp API Contract Gap Analysis

Last Updated: 2026-06-01 22:49 +0530

## Purpose

This document records the API-contract audit against:

- `docs/source-assets/rideshare-business-requirement.pdf`
- `docs/source-assets/Route-Based Ride-Sharing-Platform-designs.zip`
- passenger design screens
- driver design screens
- current backend implementation under `apps/api`

The OpenAPI JSON files in this folder are **product contracts for mobile/admin clients**. Some endpoints are planned contract endpoints and are not yet implemented by the Spring Boot backend. Backend implementation must either add these endpoints or deliberately document a different canonical endpoint before mobile/admin development starts.

## Current contract update

Updated files:

- `passenger-app.openapi.json`
- `driver-app.openapi.json`
- `admin-web.openapi.json`

Each file now contains `x-routeShareContractStatus` noting that the spec includes planned product endpoints found during this audit.

## Passenger API gaps added

Business/design coverage added for:

- app config required by mobile clients
- passenger avatar and optional verification document upload flow
- saved place update
- trusted contact update
- payment method CRUD and default selection
- passenger payment intent creation/replay
- early drop-off / get-off-early flow
- receipt endpoint after completion
- explicit trip history endpoint
- managed trip sharing endpoint
- notification read state, preferences, and push registration
- support ticket detail and message thread

Still to reconcile with backend:

- Current backend has generic `/api/v1/routes/search`, `/api/v1/bookings`, `/api/v1/payments/intents`; passenger contract uses app-specific `/api/v1/passenger/...` paths.
- Decide whether to add app-specific aliases or update mobile clients to generic resource endpoints.

## Driver API gaps added

Business/design coverage added for:

- app config required by mobile clients
- explicit KYC identity and licence APIs
- driver document listing
- vehicle detail/delete and vehicle document listing/submit
- route share link / QR endpoint
- recurring route list/create/update/pause contract
- pre-trip checklist and arrived-at-pickup action
- cash collection and audited fare-adjustment request
- earnings transaction ledger
- payout profile read
- ratings/reviews summary
- driver SOS event
- notification read state, preferences, and push registration
- driver support ticket list/detail/messages

Still to reconcile with backend:

- Current backend route/trip/booking controllers are generic and partial.
- Manual booking approve/decline, route list/detail/cancel, recurring route management, cash collection, earnings, settlement, and realtime flows are not yet fully implemented.

## Admin API gaps added

Business/design/product coverage added for:

- admin user detail, status history, and role/group projection update
- vehicle review list/detail and implemented-style vehicle review endpoint
- private document download/preview signed URL
- booking detail and status history
- admin trip cancel/interrupt and selected location trail
- fare policy management
- payment detail, payment events, void authorization
- cash collection discrepancy list
- audited finance adjustment/reversal
- support ticket detail/update/messages
- SOS event detail
- broadcast notification creation
- report export job

Still to reconcile with backend:

- Current backend admin implementation is minimal: driver review and vehicle review only.
- Full dashboard, user management, support, finance, safety, audit, reporting, and settlement APIs remain implementation work.

## Canonical API decision needed before mobile implementation

The docs currently contain both app-specific product paths and generic backend resource paths.

Decision required:

1. Keep generic backend resource APIs and update app contracts to point to them, or
2. Add app-specific controller aliases that delegate to shared services/facades.

Recommendation: keep generic backend service boundaries internally, but expose stable app-specific contracts where they simplify passenger/driver/admin clients. Generate TypeScript clients from these OpenAPI files after backend paths are implemented or mapped.

## Verification checklist before marking API contracts ready

- [ ] Backend exposes every path in each app contract, or contract marks path as future/deferred.
- [ ] Springdoc export is compared against these app contracts.
- [ ] `packages/api-contracts` generates TypeScript clients from the contracts.
- [ ] Passenger/driver/admin apps consume generated clients, not hand-written duplicated URLs.
- [ ] CI fails on contract drift.
- [ ] Security tests cover role access for every app/admin path.
