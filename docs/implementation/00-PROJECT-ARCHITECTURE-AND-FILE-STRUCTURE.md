# RouteShareApp Project Architecture and Full File/Folder Structure

> **Architecture decision:** Build RouteShareApp as a **modular monolith first**, with strict internal module boundaries so each module can be extracted into a microservice later if scale/team needs justify it.
>
> **Implementation style:** TDD where practical, domain-first, backend-enforced business rules, mobile UI consumes typed API contracts, no GPS-firehose writes directly to PostgreSQL.

> **Backend package style:** Use the approved Spring Boot-friendly module layout: `controller`, `dto`, `mapper`, `service`, `service/impl`, `facade`, `facade/impl`, `domain`, `entity`, `repository`, `event`, and `config`. Do **not** use `port/in` or `port/out` package names.

## 1. Target repository shape

```text
RouteShareApp/
├── README.md
├── .gitignore
├── .editorconfig
├── .env.example
├── package.json                         # root workspace scripts for mobile/web/shared packages
├── pnpm-workspace.yaml
├── turbo.json                           # optional later for workspace task orchestration
├── docs/
│   ├── architecture/
│   │   ├── RouteShareApp_ARCHITECTURE.md
│   │   ├── RouteShareApp_ARCHITECTURE_DIAGRAM.html
│   │   ├── 00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md
│   │   ├── ADR-0001-modular-monolith-first.md
│   │   ├── ADR-0002-postgresql-postgis.md
│   │   ├── ADR-0003-location-event-pipeline.md
│   │   └── ADR-0004-react-native-expo-dev-build.md
│   ├── implementation/
│   │   ├── 00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md
│   │   ├── 01-FOUNDATION-SETUP.md
│   │   ├── 02-BACKEND-MODULAR-MONOLITH-FOUNDATION.md
│   │   ├── 03-IDENTITY-PASSENGER-DRIVER-KYC.md
│   │   ├── 04-ROUTE-PUBLISHING-AND-MATCHING.md
│   │   ├── 05-BOOKING-TRIP-FARE-PAYMENT.md
│   │   ├── 06-REALTIME-LOCATION-TRACKING.md
│   │   ├── 07-PASSENGER-MOBILE-APP.md
│   │   ├── 08-DRIVER-MOBILE-APP.md
│   │   ├── 09-ADMIN-OPS-REPORTING.md
│   │   └── 10-HARDENING-RELEASE-ROADMAP.md
│   ├── api/
│   │   ├── openapi.yaml
│   │   ├── websocket-events.md
│   │   └── error-codes.md
│   ├── product/
│   │   ├── passenger-app-requirements.md
│   │   ├── driver-app-requirements.md
│   │   ├── admin-requirements.md
│   │   └── state-machines.md
│   ├── database/
│   │   ├── schema-overview.md
│   │   ├── indexes-and-partitioning.md
│   │   └── seed-data.md
│   └── source-assets/
│       ├── rideshare-business-requirement.pdf
│       └── Route-Based Ride-Sharing-Platform-designs.zip
├── apps/
│   ├── passenger-mobile/
│   │   ├── app.json
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   ├── babel.config.js
│   │   ├── src/
│   │   │   ├── app/
│   │   │   │   ├── AppRoot.tsx
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── RootNavigator.tsx
│   │   │   │   │   ├── AuthNavigator.tsx
│   │   │   │   │   └── PassengerTabs.tsx
│   │   │   │   └── providers/
│   │   │   │       ├── QueryProvider.tsx
│   │   │   │       ├── AuthProvider.tsx
│   │   │   │       └── RealtimeProvider.tsx
│   │   │   ├── features/
│   │   │   │   ├── auth/
│   │   │   │   ├── profile/
│   │   │   │   ├── saved-places/
│   │   │   │   ├── ride-search/
│   │   │   │   ├── ride-detail/
│   │   │   │   ├── seat-selection/
│   │   │   │   ├── booking/
│   │   │   │   ├── payment/
│   │   │   │   ├── in-trip/
│   │   │   │   ├── trip-history/
│   │   │   │   ├── ratings/
│   │   │   │   ├── safety/
│   │   │   │   ├── notifications/
│   │   │   │   └── support/
│   │   │   ├── shared/
│   │   │   │   ├── api/
│   │   │   │   ├── components/
│   │   │   │   ├── hooks/
│   │   │   │   ├── theme/
│   │   │   │   ├── utils/
│   │   │   │   └── validation/
│   │   │   └── assets/
│   │   └── tests/
│   ├── driver-mobile/
│   │   ├── app.json
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   ├── babel.config.js
│   │   ├── src/
│   │   │   ├── app/
│   │   │   │   ├── AppRoot.tsx
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── RootNavigator.tsx
│   │   │   │   │   ├── AuthNavigator.tsx
│   │   │   │   │   └── DriverTabs.tsx
│   │   │   │   └── providers/
│   │   │   ├── features/
│   │   │   │   ├── auth/
│   │   │   │   ├── kyc/
│   │   │   │   ├── vehicles/
│   │   │   │   ├── route-create/
│   │   │   │   ├── schedule/
│   │   │   │   ├── booking-requests/
│   │   │   │   ├── trip-operation/
│   │   │   │   ├── live-location/
│   │   │   │   ├── earnings/
│   │   │   │   ├── payout/
│   │   │   │   ├── ratings/
│   │   │   │   ├── safety/
│   │   │   │   ├── notifications/
│   │   │   │   └── support/
│   │   │   ├── shared/
│   │   │   └── assets/
│   │   └── tests/
│   └── admin-web/
│       ├── package.json
│       ├── next.config.ts
│       ├── tsconfig.json
│       ├── src/
│       │   ├── app/
│       │   │   ├── login/
│       │   │   ├── dashboard/
│       │   │   ├── drivers/
│       │   │   ├── passengers/
│       │   │   ├── trips/
│       │   │   ├── live-map/
│       │   │   ├── payments/
│       │   │   ├── disputes/
│       │   │   ├── reports/
│       │   │   └── settings/
│       │   ├── features/
│       │   ├── shared/
│       │   └── lib/
│       └── tests/
├── services/
│   └── api-monolith/
│       ├── README.md
│       ├── pom.xml
│       ├── Dockerfile
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/routeshare/
│       │   │   │   ├── RouteShareApplication.java
│       │   │   │   ├── common/
│       │   │   │   │   ├── config/
│       │   │   │   │   ├── security/
│       │   │   │   │   ├── web/
│       │   │   │   │   ├── errors/
│       │   │   │   │   ├── events/
│       │   │   │   │   ├── money/
│       │   │   │   │   ├── geo/
│       │   │   │   │   └── idempotency/
│       │   │   │   ├── identity/
│       │   │   │   │   ├── api/
│       │   │   │   │   ├── application/
│       │   │   │   │   ├── domain/
│       │   │   │   │   └── infrastructure/
│       │   │   │   ├── passenger/
│       │   │   │   ├── driver/
│       │   │   │   ├── vehicle/
│       │   │   │   ├── routing/
│       │   │   │   ├── matching/
│       │   │   │   ├── booking/
│       │   │   │   ├── trip/
│       │   │   │   ├── location/
│       │   │   │   ├── pricing/
│       │   │   │   ├── payment/
│       │   │   │   ├── settlement/
│       │   │   │   ├── notification/
│       │   │   │   ├── safety/
│       │   │   │   ├── support/
│       │   │   │   ├── ratings/
│       │   │   │   └── admin/
│       │   │   └── resources/
│       │   │       ├── application.yml
│       │   │       ├── application-local.yml
│       │   │       └── db/migration/
│       │   └── test/java/com/routeshare/
│       │       ├── common/
│       │       ├── identity/
│       │       ├── passenger/
│       │       ├── driver/
│       │       ├── vehicle/
│       │       ├── routing/
│       │       ├── matching/
│       │       ├── booking/
│       │       ├── trip/
│       │       ├── location/
│       │       ├── pricing/
│       │       ├── payment/
│       │       ├── settlement/
│       │       ├── notification/
│       │       ├── safety/
│       │       ├── support/
│       │       ├── ratings/
│       │       └── admin/
│       └── scripts/
├── packages/
│   ├── api-client/
│   │   ├── package.json
│   │   └── src/
│   ├── api-contracts/
│   │   ├── package.json
│   │   ├── openapi.yaml
│   │   └── src/generated/
│   ├── design-system/
│   │   ├── package.json
│   │   └── src/
│   ├── shared-types/
│   │   ├── package.json
│   │   └── src/
│   └── validation/
│       ├── package.json
│       └── src/
├── infrastructure/
│   ├── docker-compose/
│   │   ├── docker-compose.local.yml
│   │   ├── postgres/init-postgis.sql
│   │   ├── keycloak/
│   │   │   ├── realm-routeshare.json
│   │   │   └── themes/
│   │   ├── redpanda/
│   │   └── redis/
│   ├── env/
│   │   ├── local.env.example
│   │   ├── staging.env.example
│   │   └── prod.env.example
│   ├── k8s/                                # later, not MVP day one
│   └── terraform/                          # later, if cloud infra is provisioned
└── scripts/
    ├── dev-start.sh
    ├── generate-api-client.sh
    ├── test-all.sh
    └── seed-local-data.sh
```

## 2. Modular monolith package rule

Every backend module uses this internal structure:

```text
<module>/
├── api/               # REST controllers, request/response DTOs only
├── application/       # use-cases, transactions, orchestration
├── domain/            # entities, value objects, domain services, state machines
└── infrastructure/    # JPA/jOOQ repositories, provider clients, event adapters
```

Rules:

- Controllers never expose JPA entities.
- Cross-module calls go through application services or events, not direct repository access.
- Each module owns its schema/migrations conceptually, even if all run in one DB.
- Events use explicit versioned payloads, e.g. `TripStartedV1`, `PassengerBoardedV1`.
- No module writes directly into another module's tables.

## 3. Backend schemas by module

```text
identity     Keycloak subject mapping, app user profile projection, local account status, app auth audit
passenger    passenger profiles, saved places, trusted contacts
driver       driver profiles, KYC, document status
vehicle      vehicles, features, documents
routing      route templates, route occurrences, route geometry, route H3 cells
matching     match search logs, ranking/debug metadata where needed
booking      bookings, seat reservations, passenger trip states
trip         trip lifecycle, boarding/drop-off records
location     raw/matched location samples, partitioned audit records
pricing      fare estimates, fare rules, fare ledger
payment      payment intents, captures, cash records, refunds
settlement   driver balances, platform commission receivables, payouts
notification templates, delivery logs
safety       SOS events, trip share links
support      support tickets, disputes, attachments
ratings      ratings, reviews, aggregates
admin        audit logs, admin views/read models
```

## 4. Extraction path to microservices later

Start as one deployable Spring Boot app. Extract later only when needed:

1. `location` + `realtime` first if GPS load grows.
2. `matching` next if geospatial query CPU becomes isolated bottleneck.
3. `payment/settlement` if compliance or team ownership requires separation.
4. `notification` if provider throughput/retry logic becomes noisy.
5. Keep `identity`, `booking`, `trip`, `pricing` tightly consistent until boundaries are proven.

## 5. MVP deployment shape

```text
Passenger App ┐
Driver App    ├── OIDC login ──> Keycloak
Admin Web     ┘
Passenger App ┐
Driver App    ├── HTTPS/WSS with JWT ──> api-monolith ──> PostgreSQL + PostGIS
Admin Web     ┘                                 │          Redis
                                      │          Redpanda/Kafka-compatible broker
                                      └── external providers: maps, payment, SMS/push, object storage
```
