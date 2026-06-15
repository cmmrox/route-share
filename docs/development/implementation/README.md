# RouteShareApp Implementation Plans

This folder contains the staged implementation plan for building RouteShareApp with a **modular monolith first** approach.

## Files

1. `00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md` — full target project architecture and folder/file structure.
2. `01-FOUNDATION-SETUP.md` — monorepo, tooling, local infra, ADRs.
3. `02-BACKEND-MODULAR-MONOLITH-FOUNDATION.md` — Spring Boot modular monolith foundation.
4. `03-IDENTITY-PASSENGER-DRIVER-KYC.md` — auth, passenger profile, driver KYC, vehicle management.
5. `04-ROUTE-PUBLISHING-AND-MATCHING.md` — driver route publishing and passenger route search/matching.
6. `05-BOOKING-TRIP-FARE-PAYMENT.md` — bookings, trip state, fare, payment, settlement.
7. `06-REALTIME-LOCATION-TRACKING.md` — GPS ingestion, Redis latest state, WebSocket tracking, map matching.
8. `07-PASSENGER-MOBILE-APP.md` — passenger app implementation plan.
9. `08-DRIVER-MOBILE-APP.md` — driver app implementation plan.
10. `09-ADMIN-OPS-REPORTING.md` — admin web, verification, operations, reports.
11. `10-HARDENING-RELEASE-ROADMAP.md` — QA, performance, security, observability, release.
12. `11-AUTH-KEYCLOAK-USER-MANAGEMENT.md` — finalized Keycloak authentication/authorization and one-user-many-roles model.

## Recommended execution order

Follow the numeric order. Do not start mobile feature integration before the relevant backend API contracts exist. Keep modules separated internally even though the first deployable backend is one Spring Boot app.
