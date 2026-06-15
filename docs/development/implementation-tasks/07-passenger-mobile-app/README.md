# Phase 07 — Passenger Mobile App Production Implementation Tasks

> Each task is a complete public-release-ready feature slice. Do not split a user-visible feature so half is done in one task and finished later.

## Reviewed inputs

## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

## Task sequence
01. [x] [Task 01 — Passenger API Contract Reconciliation and Typed Client](./01-passenger-api-contract-reconciliation-and-typed-client.md) — core typed client verified; native evidence deferred to Task 02 scaffold
02. [Task 02 — Expo App Scaffold, Dev Tooling, and Release Pipeline](./02-expo-app-scaffold-dev-tooling-release-pipeline.md)
03. [Task 03 — App Shell, Navigation, State, and Offline Foundation](./03-app-shell-navigation-state-and-offline-foundation.md)
04. [Task 04 — Design System and Reusable Screen Components from Assets](./04-design-system-and-screen-components-from-assets.md)
05. [Task 05 — Onboarding, Auth Screens, Keycloak PKCE, and OTP Experience](./05-onboarding-auth-keycloak-and-otp-experience.md)
06. [Task 06 — Profile, Avatar, Verification, Saved Places, and Trusted Contacts](./06-profile-avatar-verification-saved-places-and-trusted-contacts.md)
07. [Task 07 — Home, Search, Location, and Route Discovery](./07-home-search-location-and-route-discovery.md)
08. [Task 08 — Results List, Results Map, Filtering, and Ride Detail](./08-results-list-map-filtering-and-ride-detail.md)
09. [Task 09 — Seat Selection, Booking Idempotency, and Cancellation](./09-seat-selection-booking-idempotency-and-cancellation.md)
10. [Task 10 — Payment Methods, Payment Intents, Receipts, and Trip History](./10-payment-methods-payment-intents-receipts-and-trip-history.md)
11. [Task 11 — Booked Waiting, Live Trip Realtime Tracking, and Share Trip](./11-booked-waiting-live-trip-realtime-and-share-trip.md)
12. [Task 12 — Exit Early, SOS, Safety, and Emergency Flows](./12-exit-early-sos-safety-and-emergency-flows.md)
13. [Task 13 — Ratings, Notifications, Support, and Account Settings](./13-ratings-notifications-support-and-account-settings.md)
14. [Task 14 — Production Hardening, QA Release, and Store Readiness](./14-production-hardening-qa-release-and-store-readiness.md)


## Critical conclusions
- `apps/passenger-mobile` is README-only, so implementation starts with scaffold.
- Backend passenger paths exist, but several are readiness placeholders; mobile must isolate them behind adapters and honest UX copy.
- Runtime DTOs differ from OpenAPI in places; Task 01 must happen before UI wiring.
- Every task must update development tracking docs and include QA evidence.
