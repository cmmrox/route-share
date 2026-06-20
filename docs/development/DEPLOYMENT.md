# RouteShare API — Deployment & Operations

Last updated: 2026-06-21 (Phase 06.6-J)

This is the production-readiness guide for the Spring Boot backend (`apps/api`). It covers building
the image, running against the project's own services, configuration, migrations, backup/restore,
runbooks, and exactly which credentials flip each gated integration live.

## 1. Build

Multi-stage Dockerfile at `apps/api/Dockerfile` (Maven build → Temurin 21 JRE, non-root, container
healthcheck on the Actuator liveness probe):

```bash
docker build -t routeshare/api:latest apps/api
```

CI alternative (build the jar, then a thin image): `./mvnw -DskipTests package` then a copy-only
Dockerfile — the committed Dockerfile builds inside Docker so it is reproducible without a local JDK.

## 2. Run against the project services

The backing services live in `infra/docker-compose/docker-compose.yml` (Postgres+PostGIS, Redis,
Redpanda, Keycloak with the `routeshare` realm import, MinIO). The production overlay adds the API:

```bash
cd infra/docker-compose
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Local dev (services only, run the API from your IDE/Maven against them):

```bash
scripts/dev-up.sh           # start services
# API defaults already point at localhost; override the DB port:
ROUTESHARE_JDBC_URL=jdbc:postgresql://localhost:5433/routeshare ./mvnw -pl apps/api spring-boot:run
```

If a port is already taken, stop the conflicting process (`lsof -ti tcp:8080 | xargs kill`) or
`scripts/dev-down.sh`, then bring the project stack back up — do not start a second/parallel stack.

## 3. Configuration & secrets

- `.env.example` — local defaults. `staging.env.example` / `prod.env.example` — deployment templates.
- Never commit real secrets; inject via the orchestrator / secret manager (Doppler, AWS Secrets
  Manager, Vault). Mobile keys must be platform-restricted.
- Production profile: `SPRING_PROFILES_ACTIVE=json` enables structured JSON logs (`logback-spring.xml`).

### Gated integrations — what to set to go live

Every external integration is built and ships **disabled/fail-safe**. Flip the flag + supply creds:

| Integration | Flag | Required credentials | Behaviour when off |
|---|---|---|---|
| Card payments (Cybersource) | `CYBERSOURCE_ENABLED=true` | `CYBERSOURCE_ENVIRONMENT`, `MERCHANT_ID`, `KEY_ID`, `SHARED_SECRET`, `WEBHOOK_SECRET` | Cash-only; card ops fail closed |
| Object storage (KYC/docs) | `OBJECT_STORAGE_ENABLED=true` | endpoint, bucket, access/secret key | Upload/download return 412 |
| Push (FCM) | `PUSH_NOTIFICATIONS_ENABLED=true` | `FIREBASE_PROJECT_ID`, `FIREBASE_SERVICE_ACCOUNT_JSON` | Notifications persist; push logged only |
| Maps (Google) | `GOOGLE_MAPS_ENABLED=true` | `GOOGLE_MAPS_SERVER_API_KEY` | Fare/ETA use haversine estimate |
| SMS/OTP (Notify.lk) | `NOTIFY_LK_ENABLED=true` | user id, api key, approved sender id | OTP send gated (no fake success) |
| Event streaming (Kafka) | `ROUTESHARE_EVENTS_KAFKA_ENABLED=true` | broker reachable at `ROUTESHARE_KAFKA_BOOTSTRAP_SERVERS` | Outbox drains to logs |
| Error monitoring (Sentry) | set `SENTRY_DSN_BACKEND` | backend DSN | Disabled |
| Keycloak role propagation | `ROUTESHARE_KEYCLOAK_USER_SYNC_ENABLED=true` (default) | admin username/password | Admin role update returns 412 |

Keycloak realm `routeshare` (import: `infra/keycloak/import/routeshare-realm.json`) already defines
the managed roles (PASSENGER, DRIVER, ADMIN, SUPPORT_AGENT, VERIFICATION_AGENT, FINANCE_ADMIN,
OPS_ADMIN, SUPER_ADMIN) and clients (api-monolith, passenger-mobile, driver-mobile, admin-web).

## 4. Database migrations

- Flyway runs automatically on startup (`classpath:db/migration`, forward-only, currently V001–V023).
- Validated end-to-end against Postgres 16 + PostGIS on the project stack (`flyway_schema_history`
  at v023).
- Pipeline: migrations ship inside the jar; a deploy = new image → app boots → Flyway applies pending
  versions in a transaction before serving traffic. Never edit an applied migration; always add a new
  `VNNN__*.sql`. Use the readiness probe (below) to gate traffic until migration + startup complete.

## 5. Health, probes, metrics

- `GET /actuator/health` → overall; `…/health/liveness`, `…/health/readiness` (readiness includes
  db + redis). Wire k8s/LB liveness→liveness, readiness→readiness.
- `GET /actuator/prometheus` (authenticated) — Micrometer/Prometheus metrics, tagged
  `application=routeshare-api`. Scrape via an authenticated job or expose on an internal mgmt port.
- Structured JSON logs under the `json` profile for log-stack ingestion. Sentry captures backend
  exceptions when `SENTRY_DSN_BACKEND` is set.

## 6. Backup & restore (drill)

PostgreSQL is the system of record. Recommended: managed Postgres with PITR; otherwise scheduled
`pg_dump`.

```bash
# Backup (custom format)
docker exec routeshare-postgres pg_dump -U routeshare -Fc routeshare > routeshare-$(date +%F).dump
# Restore into a fresh database
docker exec -i routeshare-postgres pg_restore -U routeshare -d routeshare --clean --if-exists < routeshare-YYYY-MM-DD.dump
```

Drill quarterly: restore the latest dump into a scratch DB, boot the API against it, confirm
`/actuator/health/readiness` is UP and `flyway_schema_history` matches the release. Object storage
(KYC docs) and Redis (ephemeral cache/rate-limit/live-location) are backed up/again per their
providers; Redis loss is non-fatal (caches rebuild, rate-limit windows reset).

## 7. Runbooks (quick)

- **API won't start — "Ambiguous mapping":** two controllers map the same route. `RequestMappingUniquenessTest`
  guards this in CI; if it ever reaches prod, check the failing path in the boot log and remove the dup.
- **Readiness DOWN, liveness UP:** a dependency (db/redis) is unreachable — check `…/health/readiness`
  components; the pod should not receive traffic until green.
- **429s spiking:** rate limits (`routeshare.rate-limit.*`) — tune per-action limits or investigate abuse;
  limiter fails open if Redis is down (you'll see `rate_limit_store_unavailable` warns).
- **Payments failing after enabling Cybersource:** verify creds + that the webhook endpoint
  `/api/v1/payments/webhooks/cybersource` is reachable and `CYBERSOURCE_WEBHOOK_SECRET` matches.
- **Events not flowing:** with `ROUTESHARE_EVENTS_KAFKA_ENABLED=false` the outbox relay only logs;
  enable it + point at the broker. Unsent rows stay `PENDING`/`FAILED` in `common.event_outbox` for retry.

## 8. Hosting checklist

Public API/admin/auth over HTTPS/WSS behind a reverse proxy (Caddy/Nginx) with WebSocket support;
DNS + TLS; least-privilege DB role; managed Redis/Redpanda; secrets in a manager; EAS/app-store
credentials for mobile. See `PRODUCTION_EXTERNAL_SERVICES.md` for the full provider matrix.
