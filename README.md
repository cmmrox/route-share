# RouteShareApp

RouteShareApp is a route-based ride-sharing platform with passenger, driver, and admin experiences.

## Repository layout

- `apps/api` — Spring Boot 3 / Java 21 backend modular monolith.
- `apps/passenger-mobile` — future Expo passenger app.
- `apps/driver-mobile` — future Expo driver app.
- `apps/admin-web` — future Next.js admin/ops web app.
- `packages/api-contracts` — generated/shared API contracts later.
- `packages/shared-types` — shared TypeScript types later.
- `infra/docker-compose` — local infrastructure: PostgreSQL/PostGIS, Redis, Redpanda, Keycloak, MinIO.
- `scripts` — developer helper scripts.
- `docs` — architecture, implementation, database, API, and development tracking documents.

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
