---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Release Readiness Checklist — ComiGo Unified App Backend

This is the gate between "all 16 slices are merged" and "this backend can carry real money for real
people". Every item is a hard requirement. Nothing here is a nice-to-have.

## 1. Per-slice completion

- [ ] All 16 task files have their `## Done criteria` fully ticked.
- [ ] `docs/api/API_BACKEND_RECONCILIATION.md` shows **zero** paths still marked `PLANNED_SLICE_NN`.
- [ ] Every screen in `docs/source-assets/comigo-prototype/prototype-nav.jsx` maps to a contract path that returns real data.
- [ ] `00-prototype-gap-analysis.md` re-run: all 45 MISSING and 2 MISMATCH entries closed; every PARTIAL either closed or explicitly deferred with a dated decision in `DECISION_LOG.md`.

## 2. Money correctness

The single most important section. A fare error is a refund; a capture error is a chargeback.

- [ ] Every money figure in `docs/source-assets/comigo-prototype/data.jsx` reproduces exactly from the running system.
- [ ] `driverNet + commission == passengerPays` holds as a database constraint and a property test.
- [ ] `victimShare + platformShare == feeAmount` holds as a database constraint and a property test.
- [ ] Beneficiary amounts sum exactly to the victim share for multi-victim penalties.
- [ ] No capture is possible before `trip.status = STARTED`, except live bookings where capture is on accept.
- [ ] Duplicate `start` calls, retried after timeout, produce exactly one capture per booking.
- [ ] Cancel / decline / expiry / auto-cancel all void the authorisation and charge zero.
- [ ] Referral cost never exceeds the commission on the trip that generated it.
- [ ] Rewards credit never exceeds the fare and never produces a negative payable.
- [ ] Driver ledger is append-only; no update or delete path exists.
- [ ] Wallet balance projection reconciles to the ledger for every seeded driver.
- [ ] Payout batch is idempotent per period; a double run produces one batch.
- [ ] Below-floor balances are held with their amount intact, never dropped.
- [ ] `GET /api/v1/admin/finance/ledger-reconciliation` returns clean against the full seeded dataset.

## 3. Authorization and privacy

- [ ] One account with both roles reaches both namespaces on a single token; a passenger-only account reaches none of the driver namespace.
- [ ] All eight gate codes fire on the right conditions, on `/me/context` and on 403.
- [ ] Suspension outranks every driver gate; deactivation blocks driving only and leaves payouts intact.
- [ ] Role revocation invalidates the identity projection cache immediately.
- [ ] Counterparty phone disclosure obeys every rule in plan §6.1: confirmed only, reciprocal, revoked 24 h after drop-off and on any terminal state, every read audited.
- [ ] Contact-disclosure volume alerting is live (number-harvesting detection).
- [ ] Chat is readable only by the two booking participants; admin reads require a reason and are audited.
- [ ] Pre-release rating disclosure is impossible at the query level.
- [ ] NIC images are reachable only by owner and verification agents, via short-lived presigned URLs, never in a list response.
- [ ] NIC number, gender and `HIDDEN` photo URLs appear in no counterparty-facing payload — verified by contract test.
- [ ] `showRatingPublicly` and photo visibility are enforced server-side on every read path including search.
- [ ] Eligibility (women-only, verified-only) is enforced in the search query and at booking, never client-side.
- [ ] A penetration pass over cross-tenant access: another user's booking, trip, vehicle, ledger, chat, verification, payout.

## 4. Scheduler and time correctness

- [ ] ShedLock leader election proven with a two-instance integration test.
- [ ] All thirteen jobs registered, running, and recording to `scheduling.job_run`.
- [ ] Every job is idempotent — a re-run over the same window changes nothing.
- [ ] All expiry decisions use the injected `Clock`; no client timestamp influences any deadline — including a device-supplied `capturedAt`.
- [ ] Every GPS trace fixture replays green; the urban-canyon trace produces strictly monotonic fractions.
- [ ] `STALE` and `OFF_ROUTE` trips are never offerable — fail-closed proven, not assumed.
- [ ] Approach mode opens at 500 m, raises both cadences, and **deletes the rider's position when the session closes** — no path returns it outside an open session.
- [ ] Pickup points resolve to a landmark with a description; curated beats derived; derived rows are reused rather than re-fetched.
- [ ] Live candidates over the detour cap (8 min / 3 km) are filtered before the driver is prompted.
- [ ] Alerting fires when any job misses three consecutive ticks.
- [ ] The live-request sweep (5 s) keeps pace under seeded load.
- [ ] Monthly counter reset verified across a month boundary with a shifted clock.

## 5. Data and migrations

- [ ] `V027` → `V042` apply cleanly from an empty database in one pass.
- [ ] They also apply cleanly on top of a database at `V026` (the pre-plan state).
- [ ] `scripts/simulation/seed-demo-route.sh` regenerates a complete demo dataset on the new schema.
- [ ] Every new table has its ownership/authorization relationship documented and enforced.
- [ ] Every constraint named in the task files exists in the database, verified by an introspection test.
- [ ] **The pre-launch breaking-change window is closed**: record the launch date in `DECISION_LOG.md` and state that all subsequent migrations are forward-only and additive (plan §9 risk 5).

## 6. Performance

- [ ] Ride search uses the GIST index on `route_plan.origin_point`; plan captured in QA evidence.
- [ ] Live joinable search uses the **GiST index** on `location.trip_progress.last_position`; no sequential scan.
- [ ] **Load proven at the 300 concurrent-trip ceiling** (~19 req/s, ~25 with approach mode) with no connection-pool pressure.
- [ ] Joinable-query p95 inside budget; **above 50 ms is the recorded trigger to revisit H3**, not a silent failure.
- [ ] No H3 dependency, Postgres extension or column exists anywhere — asserted by schema introspection.
- [ ] The location pipeline adds **zero** Google API calls — proven by Redis `maps:*` key counts before and after.
- [ ] p95 latency budgets agreed and met for: ride search, live search, `/me/context`, booking creation, trip start.
- [ ] N+1 audit over the enriched search, trip detail and ledger queries.
- [ ] Load smoke at expected peak concurrency without connection-pool exhaustion (virtual threads are cheap, Postgres connections are not).
- [ ] Google API cost controls from the 2026-07-21 slice still hold: session tokens, Redis caches, stored-geometry polylines, per-user rate limits, cooldown breaker.

### 6.1 Google API cost gates

- [ ] **Place Details field mask is Essentials only** (`id,formattedAddress,location`). A contract test fails the build if a Pro-tier field is added — one Pro field upgrades the entire request to Pro pricing.
- [ ] Places session token flows end-to-end, client → proxy → Google; autocomplete inside a session bills $0.
- [ ] **The location pipeline adds zero Google calls** — proven by Redis `maps:*` key counts before and after a full trip simulation.
- [ ] **ETA is derived** from route geometry ÷ observed speed. No Google call on any ETA path.
- [ ] **Live-request detour minutes are derived** from route geometry ÷ observed speed. No Directions call per candidate.
- [ ] Pickup-point resolution follows the cost-ordered chain — curated → persisted derived → route label → Places → raw coordinate — with per-tier hit rates instrumented and Places genuinely last.
- [ ] The ~200-landmark curated seed is loaded for the launch corridors.
- [ ] Ride-detail polylines still come from stored PostGIS geometry, never Directions.
- [ ] Distance Matrix cache hit rate measured and reported; coordinate rounding still ~110 m, TTL 7 days.
- [ ] A billing dashboard exists with a per-SKU breakdown and an alert before the $200 monthly credit is exhausted.
- [ ] `scripts/simulation/verify-cost-controls.sh` extended to cover the new paths (location pipeline, ETA, detour, pickup points) and passing.
- [ ] Measured monthly Google spend at simulated target volume is within the modelled **~$132/month**; any overshoot is explained per SKU before release.
- [ ] PostGIS is the only spatial dependency; no additional Postgres extension is required.

## 7. Observability

- [ ] Every metric named across the 16 task files is emitted and scraped.
- [ ] Dashboards exist for: money movement, job health, gate denials, search supply, referral cost.
- [ ] Alerts configured for: stuck authorisations, failed captures, reconciliation failures, missed Friday batch, stalled jobs, SOS trusted-contact failures, referral cost runaway, contact-disclosure spikes.
- [ ] No secret, PAN, token, NIC number or full phone number appears in any log at any level.
- [ ] Sentry receives backend errors with user context scrubbed.

## 8. Provider gates

- [ ] Cybersource, FCM, S3, Sentry, Notify.lk and Google all fail safe when disabled, and the system degrades rather than errors.
- [ ] Credentials supplied and verified in staging for every provider intended to be live at launch.
- [ ] Cash-only path fully exercised with `CYBERSOURCE_ENABLED=false`.
- [ ] **Direct dial is confirmed as the shipped behaviour** (decision D5): no masking toggles remain in any contract or response, and emergency numbers are always available.

## 9. Verification gates

- [ ] `cd apps/api && ./mvnw spotless:check verify` — green, JaCoCo 80% held.
- [ ] `pnpm --filter @routeshare/api-contracts typecheck` — green.
- [ ] `npx @redocly/cli lint docs/api/mobile-app.openapi.json docs/api/admin-web.openapi.json` — clean.
- [ ] All `scripts/simulation/verify-*.sh` smoke scripts pass against a live stack.
- [ ] Testcontainers integration suite green, including every scheduler job.
- [ ] Full stack boots from `docker-compose.prod.yml` with all migrations applied and health probes green.

## 10. Documentation and handover

- [ ] `DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `BLOCKERS.md`, `DECISION_LOG.md`, `TASK_LOG.md` current.
- [ ] `docs/architecture/RouteShareApp_ARCHITECTURE.md` updated for the five new modules, the scheduler and the location pipeline.
- [ ] `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md` reflects what was actually built, including any tuned filter thresholds.
- [ ] `docs/development/DEPLOYMENT.md` updated with the new env vars, jobs and provider gates.
- [ ] `docs/development/PRODUCTION_EXTERNAL_SERVICES.md` reconciled.
- [ ] Runbooks written for: a failed Friday batch, a stuck authorisation, a disputed penalty, a stalled scheduler, a driver deactivated in error.
- [ ] `CLAUDE.md` and `AGENTS.md` aligned; both skill mirrors synchronized.

## 11. Explicitly out of scope for this backend plan

Recorded so nobody assumes these are done:

- The ComiGo mobile app screens themselves — a separate feature folder against `apps/mobile`.
- `apps/admin-web` — still a planned application with its own contract.
- Number masking / call relay — cut by decision D5.
- Moderation and takedown tooling for reviews and chat.
- Multi-currency.
