---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 12 — Real-Time Location Pipeline

**Status:** `COMPLETED` — 2026-08-04

**Completion evidence:** V039 migrates cleanly from V001 and the preserved Slice 11 audit database;
full Maven verification passes 635 tests with zero failures/errors/skips and JaCoCo 85.55%;
`verify-location-pipeline.sh` passes 8/8 with six recorded trace families, GiST use, no H3 or
Google-cache growth, and a 300-live-row candidate-query p95 of 0.397 ms. Mobile OpenAPI lint and
the TypeScript contract check are clean. Maestro is not applicable to this backend-only slice.

**Goal:** Turn noisy phone GPS into a trustworthy answer to two questions — how far along his own published route is this driver, and can these two people actually find each other at the kerb.

**Depends on:** 04, 05, 09 (named pickup points).
**Blocks:** 13 (live booking cannot exist without it).

## Objective

Live en-route booking rests on the behind-pickup rule, and the rendezvous rests on both people finding a
kerb. Raw GPS supports neither: urban error routinely exceeds **50 metres**, more than a Colombo block.

Two problems, and they need different answers:

- **Problem A — matching.** *Is this driver still behind her pickup, on his route?* A comparison of two fractions along a known line. Tolerates ~50 m error because a safety margin absorbs it. Solved by geometry.
- **Problem B — rendezvous.** *Can they physically find each other?* Here 50 m is fatal — wrong side of Galle Road, past a junction, outside a different shop. **No amount of filtering fixes this**, because the error is in the map pin, not the filter. Solved by named pickup points (slice 09), two-way position in the final approach, and the plate and colour already in the prototype.

The design is **right-sized to 300 concurrent trips** (Decision 017): ~19 requests/second against at most
300 live rows. That is not a scale problem, so the entire complexity budget goes to accuracy.

Architecture and sources: `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md`.

## Scope

In scope:

- Ingest hardening: batched, ordered, idempotent, out-of-order tolerant.
- The four refinement filters: accuracy gate, speed gate, route projection, monotonic clamp.
- Dead-reckoning extrapolation with an explicit confidence level.
- Off-route detection and fail-closed behaviour.
- **Derived ETA** — remaining route distance ÷ observed speed, with no Google call.
- A `location.trip_progress` projection with a **GiST-indexed** last position.
- **Approach mode**: within 500 m of a pickup — 1–2 s sampling, two-way position, named-point card.
- Hybrid delivery: WebSocket/SSE when foregrounded, FCM high-priority otherwise.
- The server-driven adaptive sampling contract.
- Slice 05's arrival detection re-pointed at filtered samples.

Out of scope:

- Live requests and joinable search — slice 13.
- Named pickup-point resolution itself — slice 09; this slice consumes it.
- **H3 indexing** — declined at this scale (Decision 017). PostGIS `ST_DWithin` over ≤300 rows is faster and carries no extension dependency.
- Road-network map matching (Valhalla/OSRM), 3D shadow matching, Kafka. All declined with thresholds in the architecture doc.

## Source material / references

- `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md` — the design, the scale arithmetic, and every declined alternative with its revisit threshold.
- `docs/source-assets/comigo-prototype/live-join.jsx` — the behind-pickup rule as a server guarantee; D16c's explanation of a lapsed request.
- `docs/source-assets/comigo-prototype/data.jsx` — `CHAT`, whose fixture ("the Rajagiriya junction bus halt, not the roundabout. Silver Alto.") is the product already solving Problem B with a landmark.
- `docs/source-assets/comigo-prototype/passenger-trip.jsx` — P15 live trip, P21 public view ("updated 4 seconds ago").
- `docs/source-assets/comigo-prototype/driver-live.jsx` — D18/D18b, D19 waiting at the pickup.
- Current code: `location/**`, `routing/entity/RoutePlanEntity.java` (`route_line`), `trip/**`, `maps/**`.

## Architecture and design notes

**Route-constrained matching.** An Uber driver has no published route, so Uber must map-match against the
whole road network. A ComiGo driver publishes his, already a PostGIS `LineString`. So
`ST_LineLocatePoint(route_line, point)` yields the fraction directly and `ST_Distance` the perpendicular
offset — one call, no external API, and more accurate for this question because the search space is one
line rather than thousands of candidate segments.

**The four filters, in order.**

| Filter | Rule | Removes |
| --- | --- | --- |
| Accuracy gate | drop `accuracyMeters > 50` | fixes whose own error exceeds a block |
| Speed gate | drop if implied speed > 40 m/s | the jump-across-the-city-and-back |
| Route projection | flag `OFF_ROUTE` if offset > 80 m | points not on this route |
| Monotonic clamp | reject a lower fraction unless two consecutive samples confirm | residual jitter |

The monotonic clamp is the highest-value rule and exists **only because the route is known**. It also
disambiguates self-intersecting routes: of the candidate fractions a loop produces, take the one nearest
to and not behind the previous accepted fraction.

**Confidence is a first-class output.** `MATCHED` → `EXTRAPOLATED` (≤20 s) → `STALE`; plus `OFF_ROUTE`.
The last two **fail closed** — a position that cannot be proven behind a pickup is never offered.
Extrapolation exists so a rider's list does not flicker every time a ping is late.

**Candidate lookup is PostGIS, not H3.** At ≤300 live trips a GiST index on `last_position` with
`ST_DWithin` resolves in well under a millisecond — faster than H3, since there is no cell arithmetic,
no `kRing` expansion and no join, and it needs no Postgres extension. Revisit threshold recorded in the
architecture doc: sustained concurrency above 5,000, or joinable-query p95 above 50 ms.

**Approach mode is where small scale buys precision.** Uber's 4–6 s cadence is a cost optimisation for
millions of trips. At 300 concurrent, raising the final 500 m to 1–2 s costs ~25 req/sec even if 10% of
trips are approaching at once — and it lands precision exactly on the ninety seconds that decide whether
the pickup works. Battery is still the constraint, which is why it is a short burst, not a global rate.

**Reject bad fixes on the device.** A fix the phone already knows is poor should not consume a request, a
row, or server CPU. `FusedLocationProvider` fuses GPS, Wi-Fi, cell and inertial sensors — the same class
of sensor fusion Uber uses, free from the OS. iOS uses `kCLLocationAccuracyBestForNavigation` in-trip.

**Two-way position is privacy-bounded to the approach window.** Within 500 m of a pickup both sides see
each other. Outside it, the rider's position is never shared with the driver. This is the narrowest
window that makes the rendezvous work.

**Hybrid delivery.** Android Doze defers WebSocket traffic; FCM high-priority wakes the radio. WebSocket
when foregrounded (instant, no FCM quota), FCM high-priority otherwise. High priority is reserved for
live offers, SOS and trip-critical alerts.

**ETA is derived, never bought.** `remainingRouteMeters` comes from
`ST_Length(ST_LineSubstring(route_line, currentFraction, 1.0))`; `observedSpeed` is an
exponentially-smoothed average of `speed_mps` over the last few minutes. `eta = remaining ÷ observed`.

This is **free and more accurate than a Google estimate**, because it uses the traffic that specific
driver is actually sitting in rather than a generic model of that road — a driver crawling through
Borella at 8 km/h gets an ETA reflecting 8 km/h. Fallback order: observed speed → corridor historical
median → cached Distance Matrix. The third tier is almost never reached.

**Everything is server-authoritative.** The device reports observations; the server decides fraction,
confidence and offerability. A client cannot assert a position.

## API contracts involved

Driver (existing path, hardened):

```
POST /api/v1/driver/trips/{tripId}/location-updates
  { samples: [ { sampleId, capturedAt, lat, lng, accuracyMeters,
                 speedMps, bearingDegrees, batteryPct? } ] }
  -> { accepted, rejected: [{sampleId, reason}], progress: {...}, policy: {...} }
```

Typed rejection reasons, returned rather than silently dropped: `ACCURACY_TOO_LOW`, `IMPLAUSIBLE_SPEED`,
`OFF_ROUTE`, `BACKWARD_PROGRESS`, `DUPLICATE`, `OUT_OF_ORDER`.

New:

```
GET  /api/v1/driver/trips/{tripId}/progress          -> TripProgressResponse
GET  /api/v1/driver/location-policy                  -> the adaptive sampling contract
GET  /api/v1/driver/trips/{tripId}/approach          -> active approach: named point, rider position, distance
POST /api/v1/passenger/bookings/{id}/approach-position { lat, lng, accuracyMeters }
GET  /api/v1/passenger/bookings/{id}/approach        -> driver position, ETA, named point, plate
GET  /api/v1/passenger/trips/{tripId}/live-state     -> extended with confidence + updatedSecondsAgo
GET  /api/v1/realtime/token                          -> short-lived channel token
WS   /api/v1/realtime                                -> server→client push while foregrounded
```

`TripProgressResponse`: `tripId`, `routeFraction`, `confidence`, `matchedAt`, `updatedSecondsAgo`,
`speedMps`, `bearingDegrees`, `offRoute`, `remainingDistanceMeters`, `etaSeconds`.

`LocationPolicyResponse`: `intervalSeconds`, `priority`, `batchSize`, `mode` (`IDLE|PUBLISHED|IN_TRIP|APPROACH|LOW_BATTERY`), `reason`.

`ApproachResponse`: `active`, `pickupPoint{label, description, sideHint, lat, lng}`,
`counterparty{lat, lng, updatedSecondsAgo}`, `distanceMeters`, `etaSeconds`, `vehicle{make, colour, plate}`.

New errors: `TRIP_NOT_RUNNING`, `LOCATION_SAMPLE_REJECTED`, `REALTIME_TOKEN_EXPIRED`,
`APPROACH_NOT_ACTIVE`.

## Database / migration changes

**`V039__realtime_location_pipeline.sql`**

No Postgres extension is required — PostGIS is already installed.

- `location.location_sample` — add `sample_id TEXT`, `accuracy_meters NUMERIC(8,2)`, `speed_mps NUMERIC(6,2)`, `bearing_degrees NUMERIC(6,2)`, `battery_pct SMALLINT`, `accepted BOOLEAN NOT NULL DEFAULT true`, `rejection_reason TEXT`, `route_fraction NUMERIC(9,8)`, `route_offset_meters NUMERIC(8,2)`, `UNIQUE (trip_id, sample_id)`.
- New `location.trip_progress` — one row per running trip, the hot projection everything reads:
  `trip_id PK FK`, `route_fraction NUMERIC(9,8) NOT NULL`,
  `confidence TEXT NOT NULL CHECK (confidence IN ('MATCHED','EXTRAPOLATED','STALE','OFF_ROUTE'))`,
  `matched_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ NOT NULL`,
  `speed_mps NUMERIC(6,2)`, `bearing_degrees NUMERIC(6,2)`,
  `off_route_since TIMESTAMPTZ`, `last_position geometry(Point,4326) NOT NULL`.
- New `location.approach_session` — the privacy-bounded two-way window:
  `id`, `trip_id FK`, `booking_id FK`, `opened_at`, `closed_at`,
  `rider_position geometry(Point,4326)`, `rider_position_at TIMESTAMPTZ`,
  `UNIQUE (booking_id) WHERE closed_at IS NULL`.
  The rider's position lives **only** here and is deleted when the session closes.
- New `location.realtime_channel` — `id`, `app_user_id FK`, `connection_id`, `connected_at`, `last_seen_at`, `transport TEXT CHECK (transport IN ('WS','SSE'))`.
- Indexes:
  `idx_trip_progress_position ON location.trip_progress USING GIST (last_position)` — **the joinable-search index**;
  `idx_trip_progress_confidence ON location.trip_progress(confidence, route_fraction)`;
  `idx_trip_progress_updated ON location.trip_progress(updated_at)`;
  `idx_approach_open ON location.approach_session(trip_id) WHERE closed_at IS NULL`.
- Monthly partitioning plus a retention job on `location.location_sample` — ~225k rows/day, ~8 GB/year before retention. The trail must outlive fare-adjustment review (slice 14) and disputes.

## Configuration / environment changes

Policy settings (Decision 012): `LOCATION_ACCURACY_MAX_METERS` (50), `LOCATION_MAX_SPEED_MPS` (40),
`ROUTE_CORRIDOR_METERS` (80), `ROUTE_REVERSAL_TOLERANCE_FRACTION` (0.005),
`EXTRAPOLATION_MAX_SECONDS` (20), `LIVE_FRACTION_STALENESS_SECONDS` (60),
`OFF_ROUTE_GRACE_SECONDS` (60), `APPROACH_RADIUS_METERS` (500),
`APPROACH_SAMPLE_INTERVAL_SECONDS` (2).

Policy settings also cover ETA derivation: `SPEED_SMOOTHING_WINDOW_SECONDS` (180), `CORRIDOR_FALLBACK_SPEED_KMH` (22).

Properties: `ROUTESHARE_LOCATION_SAMPLE_RETENTION_DAYS` (90), `ROUTESHARE_REALTIME_ENABLED` (true),
`ROUTESHARE_REALTIME_TOKEN_TTL_SECONDS` (300).

Dependencies: Spring WebSocket (already on the Boot classpath, to be enabled). **No new database
extension and no new spatial library** — this is the direct consequence of declining H3.

New scheduler jobs on slice 05's infrastructure:
`location-staleness-sweep` (10 s tick) — demotes confidence, flags sustained off-route, closes stale
approach sessions. `location-sample-retention` (daily).

## UI / UX requirements

Backend slice. The contract must supply:

- P15 / P21 — position, ETA and an honest freshness figure ("updated 4 seconds ago"); never a stale position shown as current.
- D18 / D18b — the driver's own progress and next stop.
- **D19 / P38** — during approach: the named pickup point with its description and side-of-road hint, the counterparty's live position, distance and ETA, and the vehicle plate and colour.
- The sampling cadence, so battery behaviour is tuned server-side without an app release.
- Confidence, so the app renders a degraded state instead of a confidently wrong pin.

## Implementation steps

1. Extend `location.location_sample`; add `location.trip_progress` with its GiST index; add `location.approach_session`; set up partitioning.
2. Harden ingest: idempotent on `(trip_id, sample_id)`, tolerant of out-of-order batches, returning typed rejections and the current sampling policy in the same response.
3. Implement `location/domain/LocationFilterChain` — accuracy gate → speed gate → route projection → monotonic clamp. Pure, fully unit-tested against recorded noisy traces.
4. Implement `RouteProjector` over `ST_LineLocatePoint` / `ST_Distance`, including loop disambiguation.
5. Implement `DeadReckoner` — extrapolate along the route from last fraction, speed and elapsed time, capped and clamped at 1.0.
6. Maintain `trip_progress` on every accepted sample, including the smoothed observed speed.
6a. Implement `EtaCalculator` — remaining route length ÷ smoothed speed, with the corridor-median and cached-Distance-Matrix fallbacks. **Assert by test that no Google call occurs on the ETA path.**
7. Register `location-staleness-sweep`; demote confidence, flag off-route, close orphaned approach sessions.
8. Implement approach mode: open a session when the driver comes within `APPROACH_RADIUS_METERS` of the next pickup, raise both devices' cadence via the policy endpoint, accept the rider's position, serve both sides, and close on boarding, drop-off or trip end — **deleting the rider's position on close**.
9. Expose `TripProgressFacade.progressFor(tripId)` and `TripProgressFacade.candidateTripsNear(point, radiusMeters)` — the only surfaces slice 13 consumes. `candidateTripsNear` uses `ST_DWithin` against the GiST index.
10. Re-point slice 05's arrival detection at accepted samples rather than raw ones; re-run its tests.
11. Implement the WebSocket/SSE channel with short-lived token auth, a connection registry and per-user fan-out.
12. Implement `RealtimeDeliveryService`: WebSocket if a live channel exists, else FCM high-priority, reserved for live offers, SOS and trip-critical alerts.
13. Implement `GET /driver/location-policy` with the five modes and their intervals.
14. Add the retention job.
15. Build the trace replay harness: recorded GPS traces (tunnel gap, urban-canyon scatter, loop, detour, spike, clear sky) replayable in tests. This becomes the permanent regression suite for the filters.

## Files expected to change

- `apps/api/.../location/**` — filters, projector, dead reckoner, progress projection, approach sessions, ingest hardening, realtime channel, staleness sweep, retention.
- `apps/api/.../trip/**` — arrival detection re-pointed at accepted samples; approach open/close hooks.
- `apps/api/.../booking/**` — rider approach-position endpoint, scoped to the booking.
- `apps/api/.../notification/**` — `RealtimeDeliveryService` and FCM priority selection.
- `apps/api/.../scheduling/**` — staleness sweep, retention job.
- `apps/api/src/main/resources/db/migration/V039__realtime_location_pipeline.sql`.
- `apps/api/pom.xml` — WebSocket enablement only.
- `apps/api/src/test/java/**` — filter chain, trace replay, loop disambiguation, extrapolation, staleness, approach lifecycle, `ST_DWithin` candidate query, realtime delivery selection, load test at 300 concurrent trips.
- `docs/api/mobile-app.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/12-realtime-location-pipeline-qa.md`

Maestro: not applicable to the backend slice. The device-side foreground service, adaptive cadence,
approach-mode burst and battery behaviour are owned by the mobile feature plan and must be driven by the
`GET /api/v1/driver/location-policy` contract defined here.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='LocationFilterChainTest,RouteProjectorTest,LoopRouteDisambiguationTest,DeadReckonerTest,StalenessSweepIT,CandidateTripsNearIT,ApproachSessionIT,TraceReplayIT,RealtimeDeliverySelectionTest,LocationLoadIT' test
```

```bash
bash scripts/simulation/verify-location-pipeline.sh
```

The smoke replays recorded traces against a seeded route and must prove: a 60 m-scatter urban-canyon trace
produces monotonic fractions; a tunnel gap extrapolates then goes `STALE` rather than freezing; a 200 m
detour flags `OFF_ROUTE` and stops offering; a looping route never reports a fraction behind the last
accepted one; approach mode opens at 500 m and closes on boarding with the rider's position deleted; and a
simulated **300 concurrent trips** sustains ingest with p95 joinable-query latency inside budget.

## Security, privacy, and observability checks

- **Location is the most sensitive data ComiGo holds.** A driver's trail reveals home, workplace and routine. Retention is bounded; access is limited to the driver, the riders on that trip (live position only, never the trail), and admins with an audited reason.
- **The rider's position is the sharper edge.** It is shared only inside an open approach session, only with the driver of her own confirmed booking, and it is **deleted when the session closes**. Assert by test that no path returns a rider position outside an open session.
- Raw driver coordinates are never returned to a rider outside her own active trip. She gets a position, an ETA and a freshness figure — not a queryable history.
- The realtime channel authenticates per connection with a short-lived token, refuses a `tripId` the caller has no relationship to, and drops on suspension or deactivation.
- A client-supplied `capturedAt` is untrusted: clamp to server time when implausibly ahead, and never let it move a deadline (slice 05's rule).
- Sample ingest is the highest-volume authenticated endpoint — rate limit per trip and reject oversized batches.
- Metrics: `routeshare_location_samples_total{result}`, `routeshare_location_rejections_total{reason}`, `routeshare_trip_progress_confidence{level}` gauge, `routeshare_offroute_trips` gauge, `routeshare_approach_sessions_open` gauge, `routeshare_realtime_connections` gauge, `routeshare_realtime_delivery_total{channel}`, `routeshare_location_ingest_latency_seconds`, `routeshare_joinable_query_duration_seconds`.
- Alert on: the staleness sweep falling behind, off-route ratio spiking (a route-data problem, not a driver problem), rejection ratio spiking (a client shipping bad samples), approach sessions failing to close, and **joinable-query p95 above 50 ms — the documented trigger to revisit H3**.

## Done criteria

- [ ] Ingest is idempotent, order-tolerant, and returns typed rejections rather than silently dropping.
- [ ] All four filters implemented and proven against every recorded trace.
- [ ] Route projection returns fraction and offset with no external API call.
- [ ] Monotonic clamp resolves loops and never reports backward progress from jitter.
- [ ] Dead reckoning fills gaps to the cap; past it, `STALE` and everything fails closed.
- [ ] Off-route detected, graced, fail-closed; recovery resumes from the projected fraction.
- [ ] `candidateTripsNear` uses the GiST index; **no H3, no new Postgres extension, no new spatial library**.
- [ ] Approach mode opens at 500 m, raises both cadences, serves both positions, and deletes the rider's position on close.
- [ ] Staleness sweep runs leader-elected at a 10-second tick.
- [ ] Delivery picks WebSocket when foregrounded, FCM high-priority otherwise.
- [ ] Adaptive sampling policy served from the API with all five modes.
- [ ] Slice 05's arrival detection re-verified against filtered samples.
- [ ] Retention and partitioning in place.
- [ ] **Load proven at 300 concurrent trips** with p95 joinable-query latency inside budget.
- [ ] **Zero Google API calls added by this slice** — including ETA — asserted by the cost-control smoke comparing Redis `maps:*` key counts before and after.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): add route-constrained location pipeline with approach mode"
```
