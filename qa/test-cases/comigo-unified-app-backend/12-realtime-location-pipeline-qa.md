# QA — Task 12: Real-Time Location Pipeline

Status: `PASSED` — 2026-08-04

Execution: full Maven verification passed 635 tests with zero failures/errors/skips and JaCoCo
85.55%. The permanent trace/runtime gate passed 8/8 on V039; the 300-row GiST candidate query
measured 0.397 ms p95 and the location pipeline added zero Google-cache keys. The application
started against the upgraded Slice 11 audit database and reported `UP`. Mobile OpenAPI lint and
the TypeScript contract check passed. Maestro is not applicable to this backend-only slice.

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/12-realtime-location-pipeline.md`
Architecture: `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md`

## Scope

GPS ingest hardening, the four refinement filters, dead-reckoning extrapolation, off-route detection, the
GiST-indexed `trip_progress` projection, **approach mode** (two-way position in the final 500 m), and
hybrid real-time delivery. Out of scope: live requests and joinable search (slice 13), named pickup-point
resolution (slice 09), **H3** and any road-network map-matching engine — all declined with thresholds in
the architecture doc.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V039`.
- PostGIS only — **no additional Postgres extension is required** (Decision 017).
- **Recorded GPS trace fixtures** committed as test resources — this is the core of the suite:
  - `trace-clear-sky.json` — good signal along a known route
  - `trace-urban-canyon.json` — ~60 m scatter through Colombo Fort
  - `trace-tunnel-gap.json` — 90 seconds with no fix
  - `trace-loop-route.json` — a route that doubles back along the same corridor
  - `trace-detour.json` — a genuine 200 m deviation and return
  - `trace-jump.json` — a single sample 5 km away and back (classic GPS spike)
- Injected `Clock`; **no test may sleep in real time**.

## Automated test coverage

- `LocationFilterChainTest` — accuracy gate, speed gate, projection, monotonic clamp, in order and in isolation.
- `RouteProjectorTest` — fraction and perpendicular offset against known PostGIS fixtures.
- `LoopRouteDisambiguationTest` — a self-intersecting route never reports a fraction behind the last accepted one.
- `DeadReckonerTest` — extrapolation from fraction, speed and elapsed time; capped at the limit.
- `StalenessSweepIT` — confidence demotion on the leader-elected scheduler.
- `CandidateTripsNearIT` — `ST_DWithin` over the GiST index returns the correct set and uses the index.
- `ApproachSessionIT` — opens at 500 m, raises cadence, serves both positions, deletes the rider's position on close.
- `LocationLoadIT` — 300 concurrent trips sustained, p95 joinable-query latency inside budget.
- `TraceReplayIT` — every fixture above, end to end.
- `RealtimeDeliverySelectionTest` — WebSocket when a channel exists, FCM high-priority otherwise.
- `LocationIngestIdempotencyTest` — duplicate and out-of-order batches.

## Maestro automation

Not applicable to this backend slice. The device-side foreground service, adaptive cadence and battery
behaviour are owned by the mobile feature plan; that plan's flows must be driven by the
`GET /api/v1/driver/location-policy` contract defined here and must link back to this QA file.

## Test cases

### Ingest

| # | Case | Expected |
| --- | --- | --- |
| 12-1 | Batch of 5 ordered samples | All accepted; `trip_progress` reflects the last |
| 12-2 | Same batch replayed | Zero new rows; idempotent on `(trip_id, sample_id)` |
| 12-3 | Batch arriving out of order after a later batch | Handled without moving the fraction backward |
| 12-4 | Batch for a trip the caller does not drive | 403 |
| 12-5 | Batch for a trip not in a running state | `TRIP_NOT_RUNNING` |
| 12-6 | Oversized batch (200 samples) | Rejected |
| 12-7 | `capturedAt` two hours in the future | Clamped to server time; never moves a deadline |

### The four filters

| # | Case | Expected |
| --- | --- | --- |
| 12-8 | Sample with `accuracyMeters = 80` | Rejected `ACCURACY_TOO_LOW` |
| 12-9 | Sample with `accuracyMeters = 45` | Accepted |
| 12-10 | `trace-jump.json` — 5 km displacement in 4 s | Rejected `IMPLAUSIBLE_SPEED`; fraction unchanged |
| 12-11 | Sample 200 m perpendicular to the route | Flagged `OFF_ROUTE`, not silently accepted |
| 12-12 | Sample 40 m perpendicular | Accepted; within corridor |
| 12-13 | Fraction 0.02 lower than the last, single sample | Rejected `BACKWARD_PROGRESS` |
| 12-14 | Fraction lower, confirmed by a second consecutive sample | Accepted — genuine wrong turn |
| 12-15 | `trace-urban-canyon.json` | Fractions strictly monotonic despite ~60 m scatter |
| 12-16 | Rejection response | Each rejected sample returned with a typed reason, never silently dropped |

### Confidence and extrapolation

| # | Case | Expected |
| --- | --- | --- |
| 12-17 | Fresh accepted sample | `confidence = MATCHED` |
| 12-18 | 10 s after the last sample | `EXTRAPOLATED`; fraction advanced along the route by speed × elapsed |
| 12-19 | 25 s after (past the 20 s cap) | `STALE`; fraction no longer advancing |
| 12-20 | `trace-tunnel-gap.json` (90 s gap) | Extrapolates, then `STALE`, then recovers to `MATCHED` on the first good fix |
| 12-21 | Trip at `STALE` | **Not offerable** — fails closed |
| 12-22 | Extrapolation never overruns the route end | Fraction clamped at 1.0 |

### Off-route

| # | Case | Expected |
| --- | --- | --- |
| 12-23 | `trace-detour.json`, 30 s outside the corridor | Still within grace; not yet `OFF_ROUTE` |
| 12-24 | Same trace, 70 s outside | `OFF_ROUTE`; `off_route_since` set; offering stops |
| 12-25 | Driver rejoins the corridor | Resumes from the **projected** fraction, not the stale one |
| 12-26 | Off-route period | Samples still stored for the trail and fare-adjustment review |

### Candidate query and load

| # | Case | Expected |
| --- | --- | --- |
| 12-27 | `candidateTripsNear` with 300 live trips | Correct set returned; p95 well inside budget |
| 12-28 | Query plan | **GiST index seek** on `last_position`, not a sequential scan |
| 12-29 | Sustained 300 concurrent trips at 4 s cadence (~19 req/s) | Ingest keeps up; no connection-pool pressure |
| 12-30 | 10% of trips in approach mode simultaneously (~25 req/s) | Still comfortable; staleness sweep holds its 10 s tick |
| 12-31 | Any H3 dependency, extension or column | **None present** — asserted by test and by schema introspection |
| 12-31a | Full trip simulation, Redis `maps:*` key count before vs after | **Identical — zero Google calls added** |
| 12-31b | ETA on a running trip | Derived from geometry ÷ observed speed; no Google call on any path |
| 12-31c | ETA with no observed speed yet | Falls back to corridor median, then cached Distance Matrix — never a live Directions call |

### Approach mode

| # | Case | Expected |
| --- | --- | --- |
| 12-32 | Driver at 600 m from the next pickup | No approach session |
| 12-33 | Driver crosses 500 m | Session opens; both devices' policy switches to `APPROACH` at 1–2 s |
| 12-34 | Rider posts her position during an open session | Accepted; visible to that driver only |
| 12-35 | Rider posts her position with no open session | Refused `APPROACH_NOT_ACTIVE` |
| 12-36 | Driver of a different trip requests her position | 403 |
| 12-37 | Approach payload | Named pickup point with label, description and side hint; plate and colour; distance and ETA |
| 12-38 | Boarding confirmed | Session closes; **rider position row deleted** |
| 12-39 | Trip ends with the session still open | Staleness sweep closes it and deletes the position |
| 12-40 | Any path outside an open session | Returns no rider position, ever |

### Real-time delivery

| # | Case | Expected |
| --- | --- | --- |
| 12-41 | Driver app foregrounded with an open channel | Delivered over WebSocket; no FCM sent |
| 12-42 | No open channel | FCM **high priority** |
| 12-43 | Realtime token expired | Connection refused `REALTIME_TOKEN_EXPIRED` |
| 12-44 | Connection requesting a trip the user has no relationship to | Refused |
| 12-45 | User suspended mid-connection | Channel dropped |
| 12-46 | Non-critical notification | Never sent at FCM high priority |

### Privacy and retention

| # | Case | Expected |
| --- | --- | --- |
| 12-47 | Rider requests the driver's historical trail | 403 — she gets live position only |
| 12-48 | Rider requests live position on a trip she is not on | 403 |
| 12-49 | Admin reads a trail | Allowed with an audited reason |
| 12-50 | Samples past the retention window | Removed by the retention job |
| 12-51 | Any response to a rider | Contains position and freshness, never a queryable history |

## Manual checks

- Replay each trace fixture and plot the resulting fractions against time. The urban-canyon result must be visibly monotonic; if it saw-tooths, the clamp is wrong regardless of what the assertions say.
- Confirm **zero** Google API calls are added by this slice, ETA included — run `verify-cost-controls.sh` before and after and compare Redis `maps:*` key counts. Any increase is a fail.
- Measure ingest throughput at the **300 concurrent-trip ceiling** (~19 req/s, ~25 with approach mode) and confirm no connection-pool pressure.
- Confirm the staleness sweep keeps its 10-second tick under that load.
- Grep logs for raw coordinates at INFO or above — position data must not be routinely logged.

## Evidence to collect

- `scripts/simulation/verify-location-pipeline.sh` output.
- Fraction-vs-time plots for all six trace fixtures.
- `EXPLAIN ANALYZE` for `candidateTripsNear`, showing the GiST index seek.
- Redis `maps:*` key counts before and after, proving zero added Google cost.
- Throughput and sweep-latency measurements at simulated load.

## Pass/fail criteria

Pass when: ingest is idempotent and order-tolerant with typed rejections; all four filters behave
correctly on every recorded trace; loops never produce backward progress; extrapolation fills gaps and
then degrades to `STALE` rather than freezing a stale position as current; off-route is detected, graced
and fail-closed; the candidate query uses the GiST index and holds at 300 concurrent trips;
approach mode opens, serves both sides and deletes the rider's position on close; delivery picks the right
channel; and no rider can reach a driver's historical trail.

Fail on: any backward fraction from jitter, any stale position served as `MATCHED`, an off-route trip
still being offered, a sequential scan on the candidate query, a rider position surviving a closed
approach session, a Google API call added by this slice, or a rider able to read a trail.
