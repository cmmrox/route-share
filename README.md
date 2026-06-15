# RouteShareApp

RouteShareApp is a route-based ride-sharing platform with passenger, driver, and admin experiences.

## Repository layout

- `apps/api` — Spring Boot 3 / Java 21 backend modular monolith.
- `apps/passenger-mobile` — Expo React Native passenger app.
- `apps/driver-mobile` — driver app workspace placeholder.
- `apps/admin-web` — admin/ops web workspace placeholder.
- `packages/api-contracts` — shared TypeScript API contract/client package.
- `packages/shared-types` — shared TypeScript types package placeholder.
- `infra/docker-compose` — local infrastructure: PostgreSQL/PostGIS, Redis, Redpanda, Keycloak, MinIO.
- `infra/keycloak` — local Keycloak realm/import configuration.
- `scripts` — developer and QA helper scripts.
- `qa` — committed QA automation flows and wrappers; generated reports are ignored under `qa/reports/`.
- `docs` — architecture, implementation, API, development tracking, QA summaries, and source reference material.

See `docs/development/implementation/00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md` for the canonical file/folder rules and `docs/development/REPOSITORY_ORGANIZATION_PLAN.md` for commit hygiene rules.

## Backend quick start

```bash
cp .env.example .env
./scripts/dev-up.sh
cd apps/api
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH ./mvnw spring-boot:run
```

Backend health:

```bash
curl http://localhost:8080/actuator/health
```

## Passenger mobile quick checks

```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
```

## Commit hygiene

Before committing, run:

```bash
git status --short --ignored
git add -n .
```

Only source, configuration examples, repeatable scripts/flows, and documentation should be staged. Keep `.env`, `node_modules`, build outputs, `qa/reports/`, `.hermes/runtime/`, `.docker/`, `.DS_Store`, and generated `artifacts/` out of git.
