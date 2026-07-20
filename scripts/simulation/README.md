# RouteShare Simulation / QA Helper Scripts

Local-stack helper scripts for seeding demo data and verifying the Google-API cost controls.
They target the **local development stack only** (demo OTP `000000`, local Postgres/Redis
containers) and must never be pointed at staging or production.

## Prerequisites

- Local stack running: Postgres (`routeshare-postgres`), Redis (`routeshare-redis`), Keycloak.
- Backend API on `http://localhost:8080` started with `NOTIFY_LK_ALLOW_DEMO_SENDER_FOR_OTP=true`
  (and `GOOGLE_MAPS_ENABLED=true` + a server key for the Google-dependent checks).
- `curl`, `python3`, `docker` on the PATH.

Override defaults with env vars: `ROUTESHARE_API_BASE`, `ROUTESHARE_POSTGRES_CONTAINER`,
`ROUTESHARE_REDIS_CONTAINER`, `ROUTESHARE_SIM_DRIVER_PHONE`, `ROUTESHARE_SIM_PASSENGER_PHONE`.

## Scripts

| Script | Purpose |
| --- | --- |
| `lib.sh` | Shared helpers (demo-OTP login, API/psql wrappers). Sourced by the others. |
| `seed-demo-route.sh [offset-min]` | Seeds a demo driver + approved vehicle and publishes a road-shaped Colombo Fort → Nugegoda route (default departure now+90 min). Prints `routePlanId` / `routeOccurrenceId`. Safe to rerun (new route each run). |
| `verify-cost-controls.sh` | End-to-end verification of the cost controls: autocomplete session-token pass-through, place-details Redis cache (`maps:place:*`), ride search over the seeded route, stored route geometry (`source=route_plan`, zero Google cost), distance-matrix cache (`maps:dm:*`), and the per-user autocomplete rate limiter (expects HTTP 429 past the limit). Exits non-zero on any failed check. |

## Typical flow

```bash
scripts/simulation/seed-demo-route.sh          # seed a searchable route
scripts/simulation/verify-cost-controls.sh     # verify caching / session tokens / rate limits
```

Notes:

- The rate-limiter burst intentionally exhausts the passenger's autocomplete quota for the
  current minute — run it **after** any manual/emulator search QA, or wait 60 s.
- Approval steps (`driver_profile`, `vehicle`) are local SQL flips replacing the admin review
  flow; that is acceptable for local QA only.
- Without Google keys the scripts still validate the degradation paths (412 on places,
  `HAVERSINE_ESTIMATE` pricing, stored-geometry polyline).
