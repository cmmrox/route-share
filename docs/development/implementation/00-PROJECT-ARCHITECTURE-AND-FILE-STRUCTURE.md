# RouteShareApp Project Architecture and File/Folder Structure

> **Architecture decision:** RouteShareApp is a modular monorepo. The backend is a Spring Boot modular monolith first, with strict internal module boundaries so selected modules can be extracted later only when scale, compliance, or team ownership justifies it.
>
> **Implementation style:** TDD where practical, domain-first, backend-enforced business rules, typed API contracts for app clients, clean documentation, and no generated/local artifacts in commits.
>
> **Backend package style:** Use the approved Spring Boot-friendly module layout: `controller`, `dto`, `mapper`, `service`, `service/impl`, `facade`, `facade/impl`, `domain`, `entity`, `repository`, `event`, and `config` as applicable. Do **not** use `port/in` or `port/out` package names in this codebase.

## 1. Canonical repository shape

```text
RouteShareApp/
├── README.md
├── .gitignore
├── .env.example                         # safe placeholders only; never real secrets
├── package.json                         # root workspace scripts
├── pnpm-workspace.yaml
├── pnpm-lock.yaml
├── apps/
│   ├── api/                             # Spring Boot 3 / Java 21 backend modular monolith
│   │   ├── pom.xml
│   │   ├── src/main/java/com/routeshare/
│   │   │   ├── RouteShareApplication.java
│   │   │   ├── common/
│   │   │   ├── identity/
│   │   │   ├── passenger/
│   │   │   ├── driver/
│   │   │   ├── vehicle/
│   │   │   ├── routing/
│   │   │   ├── booking/
│   │   │   ├── trip/
│   │   │   ├── location/
│   │   │   ├── pricing/
│   │   │   ├── payment/
│   │   │   ├── appreadiness/
│   │   │   ├── platform/
│   │   │   └── admin/
│   │   ├── src/main/resources/application.yml
│   │   ├── src/main/resources/db/migration/
│   │   └── src/test/java/com/routeshare/
│   ├── passenger-mobile/                # Expo React Native passenger app
│   │   ├── package.json
│   │   ├── app.json
│   │   ├── eas.json
│   │   ├── src/
│   │   │   ├── api/
│   │   │   ├── application/
│   │   │   ├── design-system/
│   │   │   ├── features/
│   │   │   └── screens/
│   │   └── scripts/
│   ├── driver-mobile/                   # driver app workspace placeholder
│   └── admin-web/                       # admin web workspace placeholder
├── packages/
│   ├── api-contracts/                   # shared TypeScript API contract/client package
│   └── shared-types/                    # shared TypeScript types placeholder
├── infra/
│   ├── docker-compose/docker-compose.yml
│   └── keycloak/import/routeshare-realm.json
├── scripts/                             # local dev and QA helpers
├── qa/
│   ├── README.md
│   ├── test-cases/                      # committed manual/functional QA specifications
│   ├── maestro/                         # committed repeatable Maestro flows by app/suite
│   │   └── passenger-mobile/
│   │       ├── smoke/
│   │       ├── regression/
│   │       └── release/
│   └── reports/                         # ignored generated QA evidence
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── database/
│   ├── development/
│   │   ├── implementation/              # phase plans and task plans
│   │   │   └── tasks/<feature-plan-name>/
│   │   ├── DEVELOPMENT_STATUS.md
│   │   ├── IMPLEMENTATION_ROADMAP.md
│   │   └── maintenance/status docs only; QA files live under qa/
│   └── source-assets/
└── artifacts/                           # ignored generated/disposable reports/logs/exports
```

## 2. What belongs where

- `apps/` contains deployable applications only.
- `apps/api` owns all backend runtime code, migrations, backend tests, and backend configuration.
- `apps/passenger-mobile` owns passenger app UI, app state, mobile API adapters, tests, and app-specific scripts.
- `packages/` contains reusable workspace packages shared across applications.
- `infra/` contains reproducible local infrastructure definitions and imports. Runtime volumes/data must not be committed.
- `scripts/` contains cross-project helper scripts that are safe to run repeatedly.
- `qa/maestro/<app>/<suite>` contains repeatable QA flows. `qa/reports` is generated evidence and is ignored.
- `docs/development/implementation/tasks/<feature-plan-name>` contains task-level future feature execution plans.
- QA test cases live under `qa/test-cases/`; generated/daily QA runs are local-only and ignored.
- `docs/development/maintenance` contains repository/process maintenance decisions.
- `artifacts/` is ignored and should only hold disposable generated reports, PDFs, logs, screenshots, and tool output.

## 3. Backend modular monolith package rule

Every backend domain module should use only the applicable folders from this approved shape:

```text
<module>/
├── controller/       # REST controllers only
├── dto/              # request/response DTOs
├── mapper/           # DTO/domain/entity mapping
├── service/          # service interfaces / use-case boundaries
├── service/impl/     # service implementations and transactions
├── facade/           # cross-module boundary interfaces
├── facade/impl/      # cross-module boundary implementations
├── domain/           # value objects, rules, state machines
├── entity/           # JPA entities owned by the module
├── repository/       # persistence repositories owned by the module
├── event/            # event payloads/publishers/listeners
└── config/           # module-specific configuration
```

Rules:

- Controllers never expose JPA entities.
- Controllers call services/facades; they do not own business rules.
- Cross-module calls go through facades/services or explicit events, not direct repository access.
- Each module owns its schema/tables conceptually, even when all modules run in one PostgreSQL database.
- No module writes directly into another module's tables.
- Events use explicit versioned payloads when they become durable integration contracts.
- Keep implementation simple: no speculative microservices, queues, caches, abstractions, or generic frameworks until a real requirement justifies them.

## 4. Backend schemas by module

```text
identity      Keycloak subject mapping, app user projection, OTP challenge state
passenger     passenger profiles, saved places, trusted contacts
appreadiness  app-facing workflow/readiness read models
platform      app config and app-platform endpoints
admin         admin/ops review endpoints and read models
driver        driver profiles, KYC, document status
vehicle       vehicles, vehicle documents, review status
routing       route plans, schedules, occurrences, route bucket cells
booking       bookings, booking status history, idempotent booking behavior
trip          trip lifecycle, passenger trip states, checklist/transition state
location      latest location cache, WebSocket publishing, selected location samples
pricing       fare estimates and fare calculation rules
payment       payment intents, fare ledger entries, receipts, cash/adjustment lifecycle
settlement    future driver balances, platform commission receivables, payouts
notification  future provider templates, delivery logs, retries
safety        future SOS events, trip share links, emergency flows
support       future support tickets, disputes, attachments
ratings       future ratings, reviews, aggregates
```

## 5. API and client contract rule

Before mobile or web UI consumes backend behavior:

1. Review the relevant OpenAPI/Swagger document in `docs/api/`.
2. Compare it with actual backend controllers/DTOs in `apps/api`.
3. Record gaps in the applicable reconciliation document under `docs/api/`.
4. Update the backend, contract, or client adapter deliberately.
5. Add or update typed client code in `packages/api-contracts` or the app-local API adapter.
6. Add tests for critical happy, error, auth, validation, timeout/offline, and ownership paths.

## 6. QA and artifact policy

Committed:

- Repeatable QA scripts: `scripts/qa-*.sh`.
- Repeatable Maestro flows: `qa/maestro/<app>/<suite>/*.yaml`.
- QA test-case plans: `qa/test-cases/**/*.md`.

Ignored/generated:

- `qa/reports/` screenshots, logs, XML, UI dumps, and run evidence.
- `artifacts/` disposable PDFs, logs, generated exports, one-off reports.
- `.hermes/runtime/`, `.docker/`, `.DS_Store`, `.env`, `node_modules`, backend `target`, Expo `.expo`, generated native `android/ios` outputs.

Promote only durable project status into git. Development completion/status belongs in `docs/development/`; reusable QA cases belong in `qa/test-cases/`. Do not commit daily run logs, screenshots, XML reports, or generated binary reports.

## 7. Extraction path to microservices later

Start as one deployable Spring Boot app. Extract later only when operational evidence justifies it:

1. `location` + realtime gateway if GPS/WebSocket load becomes isolated and high volume.
2. `routing`/matching if geospatial query CPU becomes an isolated bottleneck.
3. `payment`/`settlement` if compliance, audit, or provider isolation requires a boundary.
4. `notification` if provider retries/throughput need a separate worker.
5. Keep `identity`, `booking`, `trip`, and `pricing` tightly consistent until their boundaries are proven.

## 8. MVP deployment shape

```text
Passenger App ┐
Driver App    ├── OIDC login ──> Keycloak
Admin Web     ┘

Passenger App ┐
Driver App    ├── HTTPS/WSS with JWT ──> apps/api modular monolith ──> PostgreSQL + PostGIS
Admin Web     ┘                                      │                 Redis
                                                     │                 Redpanda/Kafka-compatible broker
                                                     └── external providers: maps, payment, SMS/push, object storage, observability
```
