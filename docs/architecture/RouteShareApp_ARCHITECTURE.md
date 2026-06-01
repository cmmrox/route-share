# RouteShareApp Architecture Document

**Project:** Route-Based Ride Sharing Platform  
**Folder:** `/Users/cmmrox/Personal/Projects/RouteShareApp`  
**Prepared for:** CMMROX  
**Phase:** Architecture review before database design  
**Version:** 0.1

---

## 1. Executive Summary

RouteShareApp is a planned-route ride sharing platform. Unlike Uber-style on-demand dispatch, drivers publish routes they already intend to travel, and passengers book full or partial overlapping segments of those routes.

The platform should be split into three applications:

- `apps/passenger-mobile`: mobile app for searching, booking, paying, tracking, receipts, support, SOS, and ratings.
- `apps/driver-mobile`: mobile app for KYC, vehicle management, route publishing, booking approval, trip operation, earnings, and payouts.
- `apps/admin-web`: admin portal plus live API backend for mobile apps.

For the backend, a microservice-oriented Spring Boot architecture is recommended, with PostgreSQL + PostGIS as the system of record, Kafka/Redpanda for event streaming, Redis for ephemeral realtime state, and a dedicated geospatial route-matching stack.

The most critical technical areas are:

1. Route overlap search and ranking.
2. Realtime location ingestion, road snapping, and smooth map rendering.
3. Correct trip, booking, payment, and fare state machines.
4. Safety, auditability, fraud control, and support operations.

---

## 2. Inputs Reviewed

### Business requirements PDF
Reviewed requirements include:

- Passenger registration, login, saved places, ride search, booking, realtime tracking, partial trip exit, receipts, ratings, history.
- Driver KYC, vehicle verification, one-time/recurring route creation, seat availability, booking approval, trip start, boarding/drop-off, earnings, payouts.
- Admin verification, user management, live trip monitoring, fare/commission rules, disputes, reports, fraud monitoring.
- Route matching for full route, partial route, nearby route, and high-overlap scenarios.
- Dynamic fare calculation based on actual passenger distance travelled.
- Card pre-authorization and final capture, plus cash trip commission accounting.
- Recurring trips and generated trip occurrences.
- Safety features: identity verification, live trip sharing, SOS, ratings, support.

### UI design upload
Reviewed design/source files indicate the following screen areas:

**Passenger app:** splash/onboarding, login/OTP/profile setup, home, search, saved places, ride results list/map, ride details, seat selection, payment, booked state, in-trip tracking, exit early, receipt, rate driver, trip history, account, SOS, share trip, notifications, support.

**Driver app:** splash/onboarding, login, KYC steps, vehicle add/list, home/dashboard, trip list/schedule, route publishing wizard, booking mode, published confirmation, trip details, booking requests, pre-trip checklist, live trip, boarding/drop-off, completion, earnings, payout setup, ratings, account, notifications, SOS, support, leaderboard.

---

## 3. Recommended Repository / Folder Structure

```text
RouteShareApp/
├── apps/passenger-mobile/          # Passenger mobile app
│   └── README.md
├── apps/driver-mobile/             # Driver mobile app
│   └── README.md
├── apps/admin-web/                 # Admin web app
│   └── README.md
├── docs/
│   ├── architecture/
│   │   ├── RouteShareApp_ARCHITECTURE.md
│   │   └── RouteShareApp_ARCHITECTURE_DIAGRAM.html
│   └── source-assets/             # Uploaded requirement/design files for reference
└── README.md
```

---

## 4. High-Level Architecture

### 4.1 Application layer

- **Passenger mobile app**: React Native or Flutter.
- **Driver mobile app**: React Native or Flutter.
- **Admin web app**: Next.js or React under `apps/admin-web`.
- **Backend APIs**: Spring Boot 3 / Java 21 microservices.
- **Realtime APIs**: Spring WebFlux/WebSocket gateway for trip and location updates.

### 4.2 Core backend platform

- **API Gateway / BFF**: single entry point for mobile and admin clients.
- **Auth & Identity Service**: user accounts, roles, sessions, OTP/social login, RBAC.
- **Passenger Profile Service**: saved places, preferences, passenger verification.
- **Driver & Vehicle Service**: driver KYC, documents, vehicle verification, payout setup.
- **Route Publishing Service**: one-time and recurring driver routes.
- **Route Matching Service**: full/partial overlap search and ranking.
- **Booking & Trip Service**: booking lifecycle, trip lifecycle, seat capacity, boarding/drop-off.
- **Location Ingestion Service**: receives GPS updates from mobile apps.
- **Map Matching / Route Progress Service**: road-snaps GPS, computes route progress and travelled distance.
- **Realtime Gateway Service**: WebSocket/SSE fanout to passenger, driver, and admin.
- **Pricing & Fare Service**: estimates, final fare calculation, commissions.
- **Payment & Wallet Service**: cash/card, preauth/capture/refunds, wallet later.
- **Settlement & Payout Service**: driver balances, payouts, cash commission collection.
- **Notification Service**: push/SMS/email/WhatsApp templates and delivery.
- **Safety & Support Service**: SOS, trip sharing, disputes, reports.
- **Ratings & Reviews Service**: post-trip ratings and quality signals.
- **Admin & Reporting Service**: management, monitoring, analytics read APIs.

---

## 5. Technology Choices

### 5.1 Mobile apps

**Recommendation:** React Native with TypeScript.

Reasoning:

- Existing uploaded UI designs are JSX/React-like, so React Native is a natural continuation.
- One team can share UI patterns, validation logic, design tokens, and API client code.
- Supports native location APIs, background location, push notifications, maps, and animation.

Alternative: Flutter is also strong for map-heavy apps, but using React Native reduces design conversion effort.

### 5.2 Backend services

**Recommendation:** Spring Boot 3 + Java 21.

Use:

- Spring Web MVC for normal REST APIs.
- Spring WebFlux for realtime gateway and high-concurrency streaming endpoints.
- Spring Security + OAuth2/JWT.
- Spring Data JPA for transactional services.
- jOOQ or native SQL for geospatial/search-heavy services where query control matters.
- Flyway for database migrations.

### 5.3 Database and storage

- **PostgreSQL + PostGIS**: authoritative relational and geospatial database.
- **Redis**: latest live location, short-lived locks, OTP/session/cache, websocket fanout coordination.
- **Kafka or Redpanda**: event backbone for location, trip, payment, notification, audit events.
- **S3-compatible object storage**: KYC/vehicle documents, profile images, support attachments.
- **OpenSearch/Elasticsearch**: optional later for admin search, logs, and support indexing.
- **ClickHouse/Apache Pinot/BigQuery**: optional analytics store for high-volume reports.

### 5.4 Maps, routing, and geospatial stack

Options:

- **Launch speed:** Google Maps Platform or Mapbox for maps, directions, geocoding, Places, Roads/Map Matching.
- **Cost/control:** OSRM or Valhalla self-hosted for routing and map matching; OpenStreetMap data; MapLibre for map rendering.

Recommended staged approach:

1. MVP: use Google Maps/Mapbox for reliability and faster launch.
2. Growth phase: move route matching/routing workloads to OSRM/Valhalla where cost and customization matter.
3. Keep PostGIS as the authoritative store regardless of provider.

---

## 6. Microservice Architecture

### 6.1 API Gateway / Mobile BFF

**Language:** Java 21 / Spring Cloud Gateway or Kotlin + Spring  
**Database:** none or Redis for rate limits/session caches

Responsibilities:

- Auth token validation.
- Rate limiting.
- Request routing.
- API versioning.
- Client-specific response shaping.
- Correlation IDs and tracing.

### 6.2 Auth & Identity Service

**Language:** Java 21 / Spring Boot  
**Database:** PostgreSQL

Responsibilities:

- Passenger/driver/admin accounts.
- OTP/email/social login.
- JWT/session issuance.
- Roles and permissions.
- Account status and security events.

Recommended option: Keycloak can be used for identity, with a thin internal user-profile service around it.

### 6.3 Driver & Vehicle Service

Responsibilities:

- Driver application lifecycle.
- KYC status.
- Vehicle records, photos, document verification.
- Seat capacity and vehicle features.
- Payout account profile.

### 6.4 Route Publishing Service

Responsibilities:

- Create one-time routes.
- Create recurring routes.
- Generate concrete route occurrences.
- Store route geometry, encoded polyline, schedule, seats, price/km, booking mode.
- Publish `RoutePublished`, `RouteOccurrenceCreated`, `RouteCancelled` events.

Important concepts:

- `driver_route_template`: recurring or reusable route definition.
- `route_occurrence`: actual trip instance for a specific date/time.
- `route_geometry`: PostGIS LineString plus encoded polyline.
- `route_h3_cells`: normalized H3 cells covering the route corridor.

### 6.5 Route Matching Service

Responsibilities:

- Find full and partial route matches.
- Support pickup/drop radius, time windows, available seats, and detour rules.
- Rank results by overlap percentage, pickup proximity, drop proximity, ETA, price, rating, and reliability.

This service should be optimized separately from the booking/trip transaction service.

### 6.6 Booking & Trip Service

Responsibilities:

- Seat reservation and overbooking prevention.
- Booking state machine.
- Trip lifecycle state machine.
- Passenger boarded/drop-off status.
- Driver start/end trip.
- Idempotent state transitions.

Use database transactions and/or distributed locks for seat capacity updates. Do not rely only on frontend state.

### 6.7 Location Ingestion Service

Responsibilities:

- Receive location updates from driver and passenger apps.
- Validate location accuracy, timestamps, speed, bearing, trip status.
- Reject impossible jumps and stale events.
- Publish raw location events to Kafka.
- Update Redis latest-location cache.

Location payload should include:

```json
{
  "tripId": "...",
  "actorType": "DRIVER|PASSENGER",
  "actorId": "...",
  "lat": 6.9271,
  "lng": 79.8612,
  "accuracyMeters": 12,
  "speedMps": 8.4,
  "bearingDegrees": 142,
  "deviceTimestamp": "2026-05-23T10:30:00Z",
  "batteryLevel": 0.73,
  "networkType": "4g"
}
```

### 6.8 Map Matching / Route Progress Service

Responsibilities:

- Snap noisy GPS points to road or planned route.
- Compute route fraction/progress.
- Detect route deviation.
- Compute canonical distance travelled.
- Produce matched location events for UI and fare calculation.

Important: raw GPS, backend matched location, and client-smoothed animation must be separate concepts.

### 6.9 Realtime Gateway Service

Responsibilities:

- WebSocket/SSE connections for passenger, driver, and admin.
- Trip-specific subscriptions.
- Push latest matched driver location, trip state, ETA, passenger boarding/drop-off updates.
- Backpressure and reconnect handling.

Recommended protocols:

- WebSocket for mobile live tracking.
- SSE for admin dashboards if one-way streaming is enough.
- Push notifications for background/offline events.

### 6.10 Pricing & Fare Service

Responsibilities:

- Estimate fare before booking based on planned overlap.
- Finalize fare based on actual matched distance travelled while passenger is onboard.
- Apply commission.
- Produce immutable fare ledger records.

MVP pricing:

```text
fare = actual_passenger_distance_km * route_price_per_km
platform_commission = fare * commission_rate
net_driver_earning = fare - platform_commission
```

Future pricing:

- Minimum fare.
- Time component.
- Peak multipliers.
- Discounts/promos.
- Cancellation/no-show penalties.

### 6.11 Payment & Wallet Service

Responsibilities:

- Card pre-authorization before trip start.
- Final card capture after distance is known.
- Refunds/voids.
- Cash trip recording.
- Wallet and promotions later.
- Payment audit logs.

For Sri Lanka, payment gateway choices should be evaluated separately based on acquiring support, settlement rules, card pre-auth support, fees, and legal/compliance requirements.

### 6.12 Settlement & Payout Service

Responsibilities:

- Driver balances.
- Platform commission accounting.
- Cash commission receivable tracking.
- Payout batches.
- Payout status and reconciliation.

### 6.13 Safety & Support Service

Responsibilities:

- SOS button.
- Trusted contact trip share links.
- Incident/dispute reporting.
- Support ticketing.
- Fraud and abuse flags.

### 6.14 Notification Service

Responsibilities:

- Booking confirmations.
- Trip start/arrival/drop-off/completion.
- Payment receipts.
- Driver KYC status.
- Cancellation/delay notifications.
- Push + SMS fallback for critical messages.

---

## 7. Route Matching Architecture

Route matching is the platform’s core differentiator. The system should not depend on exact pickup/drop equality. It must support route overlap, proximity tolerance, and ranking.

### 7.1 Two-phase matching

#### Phase 1: Candidate filtering

Use fast approximate filters:

- City/region.
- Trip status: published/scheduled.
- Departure time window.
- Available seats.
- H3 cells intersecting the passenger route corridor.
- PostGIS `ST_DWithin` for pickup/drop proximity to driver route.

#### Phase 2: Exact validation and ranking

Use geometry and routing logic:

- Locate passenger pickup/drop points along driver LineString.
- Validate direction: pickup fraction must be before drop-off fraction.
- Extract route substring for passenger’s segment.
- Compute overlap distance and percentage.
- Compare passenger requested route vs driver covered route.
- Apply walking/pickup radius limits.
- Rank by business score.

### 7.2 Suggested PostGIS functions

- `ST_DWithin`: candidate radius searches with spatial indexes.
- `ST_LineLocatePoint`: find pickup/drop fraction along driver route.
- `ST_LineSubstring`: extract passenger segment from driver route.
- `ST_Length`: measure route segment distance after projecting to a meter-based coordinate system or using geography.

### 7.3 H3 usage

H3 should be used for scalable broad filtering, not final correctness.

Recommended uses:

- Route corridor indexing.
- City/region sharding.
- Heatmaps and demand analytics.
- Fast overlap candidate search.

### 7.4 Ranking formula concept

```text
score =
  0.30 * route_overlap_score +
  0.20 * pickup_proximity_score +
  0.15 * dropoff_proximity_score +
  0.15 * time_compatibility_score +
  0.10 * price_score +
  0.10 * driver_quality_score
```

Ranking should be explainable in the UI:

- `90% route match`
- `Pickup 300m from route`
- `Arrives near your destination`
- `LKR 292 estimated`

---

## 8. Realtime Tracking Architecture

### 8.1 What makes Uber-like tracking feel accurate

Public engineering information and general mobility best practice point to these principles:

- Use road-snapped/map-matched locations instead of raw GPS for display and route progress.
- Store raw GPS separately for audit/debugging.
- Use an event pipeline for high-frequency updates.
- Use client-side interpolation/animation for smooth vehicle movement.
- Detect bad GPS points, impossible jumps, stale timestamps, and low-confidence matches.
- Measure ETA accuracy and improve continuously from historical trip data.

### 8.2 Location update frequency

Suggested mobile behavior:

- Active trip: every 1–3 seconds or every 5–15 meters.
- Near pickup/drop-off: increase frequency.
- Background/no active trip: reduce frequency strongly.
- Use adaptive throttling based on battery, network, speed, and app state.

### 8.3 Event pipeline

```text
Mobile GPS
  → Location Ingestion API
  → Kafka topic: location.raw
  → Map Matching / Route Progress Service
  → Kafka topic: location.matched
  → Redis latest location cache
  → Realtime Gateway WebSocket
  → Passenger/Driver/Admin apps
```

### 8.4 Billing and fare distance

Never calculate billing from client-smoothed marker animation.

Use:

- Explicit trip/boarding/drop-off states.
- Backend matched route progress.
- Passenger onboard time window.
- Immutable distance/fare ledger records.
- Raw GPS and matched GPS retained for dispute audits.

---

## 9. Data Ownership by Service

Each microservice should own its data model. For MVP, all schemas may live in one PostgreSQL cluster, but each service should have separate schemas and migrations.

Suggested schemas:

- `identity`: users, roles, auth audit.
- `driver`: driver profiles, KYC, vehicles, payout profiles.
- `passenger`: passenger profiles, saved places.
- `routing`: route templates, occurrences, route geometries, route H3 cells.
- `booking`: bookings, trip lifecycle, passenger states.
- `location`: raw/matched location events or metadata; high-volume partitions.
- `pricing`: fare estimates, final fares, commission rules, fare ledger.
- `payment`: payment intents, captures, refunds, cash records, settlements.
- `support`: incidents, tickets, SOS events, disputes.
- `notification`: notification templates and delivery logs.
- `ratings`: reviews and rating aggregates.

Database design should be done after architecture approval.

---

## 10. Security, Safety, and Compliance

Minimum requirements:

- OAuth2/JWT access tokens with refresh-token rotation.
- Role-based access control for passenger, driver, admin, support.
- Encryption in transit via TLS.
- Encryption at rest for sensitive fields and document storage.
- PII minimization in logs and analytics.
- KYC/vehicle document access controls.
- Audit logs for admin actions, trip state changes, payment changes, and support decisions.
- Rate limiting and fraud detection for OTP, login, booking, payment, and support APIs.
- Secure emergency/SOS workflow with fast access to live trip data.
- PCI-sensitive card handling delegated to a compliant payment provider; avoid storing raw card data.

---

## 11. Deployment Architecture

### MVP / early launch

- Dockerized Spring Boot services.
- Managed PostgreSQL with PostGIS.
- Managed Redis.
- Kafka-compatible broker: Redpanda or managed Kafka.
- Object storage: S3-compatible.
- Kubernetes is optional for MVP but useful if many services launch together.

### Recommended environments

- `local`: Docker Compose for developers.
- `dev`: shared backend for integration.
- `staging`: production-like data shape and payment sandbox.
- `prod`: production with monitoring, backups, alerts, and security controls.

### Observability

- OpenTelemetry traces across services.
- Prometheus metrics.
- Grafana dashboards.
- Loki/ELK logs.
- Sentry or equivalent mobile/backend error tracking.

Critical metrics:

- Route search latency.
- Match success rate.
- Average overlap percentage.
- Booking conversion.
- Seat overbooking conflicts.
- Location update delay.
- GPS/map-match confidence.
- WebSocket connection count and reconnects.
- ETA error vs actual arrival.
- Payment authorization/capture failures.
- Trip cancellation/no-show rates.

---

## 12. MVP Boundary

### Include in MVP

- Passenger/driver registration and login.
- Driver KYC and vehicle record flow.
- One-time and recurring route publishing.
- Passenger route search with full/partial results.
- Booking and seat management.
- Trip start/live tracking/boarding/drop-off/completion.
- Cash payment and one card gateway.
- Fare calculation by actual matched distance.
- Commission calculation.
- Basic admin portal.
- Ratings, receipts, history, support, SOS.

### Defer until later

- Multi-leg journeys using more than one driver.
- Corporate commute products.
- Subscriptions/monthly passes.
- Wallet, loyalty, referrals, promotions.
- AI recommendations.
- Fully custom routing infrastructure if commercial APIs are enough for launch.

---

## 13. Key Risks and Mitigations

### Route matching complexity

**Risk:** Incorrect full/partial matching creates bad passenger experiences.  
**Mitigation:** Build route-matching service with testable algorithms, PostGIS, H3 candidate filtering, and scenario-based automated tests.

### Location accuracy and smoothness

**Risk:** Raw GPS causes jumpy vehicle movement and wrong fare distance.  
**Mitigation:** Use road snapping/map matching, confidence scores, client-side animation, and backend canonical distance calculation.

### Overbooking

**Risk:** Concurrent bookings exceed seats.  
**Mitigation:** Use transactional seat reservations and idempotency keys; re-check capacity before confirmation.

### Payment final amount differs from estimate

**Risk:** Passenger disputes when actual fare changes.  
**Mitigation:** Clear UI wording, preauth buffer policy, immutable fare ledger, receipt breakdown.

### Driver/passenger trust and safety

**Risk:** Fraud, disputes, unsafe rides.  
**Mitigation:** KYC, vehicle verification, live trip sharing, SOS, support tickets, ratings, audit logs.

---

## 14. Public Research References

- Uber H3 spatial index: https://www.uber.com/us/en/blog/h3/
- H3 official docs: https://h3geo.org/docs/
- Uber routing engine engineering: https://www.uber.com/fr/en/blog/engineering-routing-engine/
- Uber real-time event processing example: https://www.uber.com/us/en/blog/real-time-exactly-once-ad-event-processing/
- PostGIS spatial querying: https://postgis.net/docs/using_postgis_query.html
- PostGIS `ST_DWithin`: https://postgis.net/docs/ST_DWithin.html
- PostGIS `ST_LineLocatePoint`: https://postgis.net/docs/ST_LineLocatePoint.html
- PostGIS `ST_LineSubstring`: https://postgis.net/docs/ST_LineSubstring.html
- OSRM Match API: https://project-osrm.org/docs/v5.24.0/api/#match-service
- Valhalla map matching API: https://valhalla.github.io/valhalla/api/map-matching/api-reference/
- Google Roads Snap to Roads: https://developers.google.com/maps/documentation/roads/snap
- Mapbox Navigation location tracking: https://docs.mapbox.com/android/navigation/guides/device-location/

---

## 15. Architecture Decision Summary

Recommended initial stack:

- Mobile: React Native + TypeScript.
- Admin web: Next.js + TypeScript.
- Backend: Spring Boot 3 + Java 21 microservices.
- Realtime: Spring WebFlux/WebSocket.
- Database: PostgreSQL + PostGIS.
- Cache/realtime state: Redis.
- Event streaming: Kafka or Redpanda.
- Geospatial indexing: H3 + PostGIS.
- Routing/map matching: Google/Mapbox for MVP, OSRM/Valhalla as future/self-hosted option.
- Infrastructure: Docker, Kubernetes later, CI/CD, OpenTelemetry, Prometheus/Grafana.

After this architecture is reviewed and approved, the next step should be database/domain design: entities, schemas, state machines, indexes, partitioning, and migration plan.
