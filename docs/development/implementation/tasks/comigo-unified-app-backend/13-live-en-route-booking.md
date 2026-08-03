---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 13 — Live (En-Route) Booking

**Goal:** Let a rider join a trip that is already moving — but only from behind the driver, only for about forty-five seconds, and with the card captured on acceptance because there is no start left to charge at.

**Depends on:** 04, 07, 09, **12** (the location pipeline and named pickup points it stands on).
**Blocks:** nothing.

## Objective

ComiGo has **two** request types and the prototype is emphatic they are not variants of each other:

> SCHEDULED — the trip is published, hasn't left, the driver approves at his leisure and the card is
> charged when he starts. LIVE — the trip is already running with a free seat. The driver is at the
> wheel, so the decision has to fit in one glance and a few seconds, and the card is charged the moment
> he accepts.

The hard rule: **a seat is only offerable while the driver is still behind the rider's pickup point.**
Once he passes it the request is void — "enforced on the server, never shown as a warning the driver has
to think about". Nine screens depend on this and none of it exists.

## Scope

In scope:

- Joinable-trip search: running trips with a free seat whose driver has not yet passed the rider's pickup.
- Live request with a ~45-second expiry.
- Driver prompt payload sized for one glance at the wheel.
- Capture on accept, reusing slice 04's capture path.
- Pickup-passed invalidation, continuous while the request is open.
- Mid-trip seat resale after an early drop-off.
- Per-trip mute for live requests.
- Lapse handling on both sides.

Out of scope:

- Scheduled requests — slice 07.
- The early drop-off fare recalculation itself — slices 03 and 04; this slice reuses it and puts the freed seat back on sale.

## Source material / references

- `docs/source-assets/comigo-prototype/live-join.jsx` — the whole file, including the design rationale in its header comment. P36/P36b joinable list, P37/P37b/P37c rider states, D16b driver prompt, D16c pickup-passed lapse.
- `docs/source-assets/comigo-prototype/driver-supply.jsx` — D16b mid-trip request banner, D16e request lapsed because the seat sold.
- `docs/source-assets/comigo-prototype/driver-live.jsx` — D18b seat free from a stop, D22b early drop returning the seat to sale.
- `docs/source-assets/comigo-prototype/data.jsx` — `LIVE_TRIPS`, `LIVE_PASSED_COUNT`, `LIVE_REQUEST`, `POLICY.chargeAtWhenEnRoute`, `ENROUTE_RIDE`.
- Current code: `location/**` (live position), `routing/**` (matching), `payment/**` (capture), `booking/**`.

## Architecture and design notes

**Slice 12 already answers "where is he".** This slice does not touch GPS, filtering, projection or H3.
It reads `location.trip_progress` — a fraction plus a confidence level — through `TripProgressFacade`, and
spends its effort on the booking itself. The geometry rationale and its research sources are in
`docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md`.

**"Behind the pickup" is a route-fraction comparison, not a straight-line distance.** The driver's live
position projects onto the route line as a fraction; the rider's pickup projects as another. The request
is valid only while `driverFraction < pickupFraction - SAFETY_MARGIN`. Comparing raw distances would offer
a seat to someone the driver has already passed on a route that loops — slice 12's monotonic clamp is what
makes the loop case correct.

**Confidence gates offerability, and it fails closed.** A trip is offerable only at confidence `MATCHED`
or `EXTRAPOLATED`. `STALE` and `OFF_ROUTE` are never offerable: an unknown position can never be *proven*
to be behind a pickup, and guessing in the rider's favour is exactly the failure the prototype forbids.

**Candidate lookup is PostGIS, not H3.** At a ceiling of 300 concurrent trips the joinable query filters
at most 300 rows, so a GiST index on `trip_progress.last_position` with `ST_DWithin` is faster than an H3
cell lookup and needs no Postgres extension (Decision 017). `TripProgressFacade.candidateTripsNear` is the
only entry point.

**A detour cap protects the driver and the passengers already on board.** UberX Share limits added detour
to 8 minutes. The prototype shows `+2 min · +0.4 km` on the driver prompt but states no ceiling — so
nothing would stop a request adding fifteen minutes to a driver already carrying three passengers with
their own promised arrival times. Candidates exceeding `LIVE_MAX_ADDED_MINUTES` (8) or
`LIVE_MAX_ADDED_KM` (3) are filtered **before the prompt reaches him** — the same principle as the
behind-pickup rule: never make him decline something he should not have been offered.

**The detour is computed from geometry, not from Google.** `addedMeters` is the difference between the
driver's current remaining route length and the length including the rider's pickup and drop-off — pure
`ST_LineSubstring` / `ST_Length` arithmetic on a line already in the database.
`addedMinutes = addedMeters ÷ observedSpeed`, using slice 12's smoothed speed for that trip.

A Directions call per candidate would have been the obvious implementation and the expensive one: at 300
concurrent trips with riders searching, that is thousands of calls per hour. The geometric version is
free **and more accurate**, because it reflects the traffic that driver is actually in right now rather
than a generic estimate for that road.

**The check runs three times**: when building the joinable list, when the request is created, and
continuously while it is open. D16c exists precisely because a driver can pass the pickup during the
forty-five seconds the prompt is on screen, and the rider must be told why the money she half-expected to
leave never did.

**Capture on accept is the only safe design.** A moving trip has no future start event. Slice 04 already
built the capture path; this slice calls it from the accept handler instead of from trip start. The rider
is told before she asks — P37's banner is "not held, not authorised — taken".

**Forty-five seconds is a server deadline.** The prompt's countdown is cosmetic; expiry is a row with
`expires_at` swept by the scheduler, exactly like slice 07's thirty-minute expiry but on a faster tick.

**The driver prompt is a different payload from the scheduled request.** It carries what he keeps, the
added distance and minutes, how far ahead the pickup is, who is on board, and how many seats remain after
— and nothing else. No fare breakdown, no policy paragraph, no scrolling. The API shape follows the
screen, because the screen was designed for a person driving a car.

**Mute is per trip, not per account** (D16c: "opt-out per trip and in your driving preferences"). The
account default comes from slice 08's `midTripBookings`.

**The freed seat is a partial-route seat.** After an early drop-off at Kirulapone, the seat is on sale for
"Kirulapone → Colombo Fort" only. So inventory is not simply incremented — the released seat gets a
validity window along the route, and joinable search must respect it.

## API contracts involved

Passenger:

```
POST /api/v1/passenger/live-searches
     { origin{lat,lng,label}, destination{...}, seats }
  -> { results[], passedCount }        // P36 / P36b

POST /api/v1/passenger/live-requests
     { routeOccurrenceId, origin, destination, seats }   (Idempotency-Key)
  -> { requestId, state: WAITING, expiresAt, secondsRemaining, quote }
GET  /api/v1/passenger/live-requests/{id}
DELETE /api/v1/passenger/live-requests/{id}              // withdraw
```

`LiveTripResult`: `routeOccurrenceId`, `driver{...}`, `vehicle{...}`, `currentPlaceLabel`,
`pickupInMinutes`, `pickupEta`, `arriveEta`, `aheadKm`, `seatsFree`, `freedByReason`, `quote{...}`,
`matchPercent`, `verifiedDriver`.

Driver:

```
GET  /api/v1/driver/trips/{tripId}/live-requests/current  -> the D16b prompt payload
POST /api/v1/driver/live-requests/{id}/accept             (Idempotency-Key)
POST /api/v1/driver/live-requests/{id}/decline
POST /api/v1/driver/trips/{tripId}/live-requests/mute     { muted }
```

`LiveRequestPrompt`: `requestId`, `passenger{firstNameInitial, verified, rating, rideCount, photoUrl?}`,
`fromLabel`, `toLabel`, `seats`, `seatLabels[]`, `fareAmount`, `driverNet`, `addedKm`, `addedMinutes`,
`aheadKm`, `aheadMinutes`, `onBoard`, `seatsFreeAfter`, `expiresAt`, `secondsRemaining`.

New errors: `PICKUP_ALREADY_PASSED`, `LIVE_REQUEST_EXPIRED`, `LIVE_REQUESTS_MUTED`,
`TRIP_NOT_RUNNING`, `SEAT_NO_LONGER_AVAILABLE`.

## Database / migration changes

**`V040__live_en_route_booking.sql`**

- New `booking.live_request`:
  `id`, `route_occurrence_id FK`, `trip_id FK`, `passenger_app_user_id FK`,
  `pickup geometry(Point,4326)`, `dropoff geometry(Point,4326)`,
  `pickup_route_fraction NUMERIC(9,8)`, `dropoff_route_fraction NUMERIC(9,8)`,
  `seats INT`, `fare_quote_id FK`,
  `state TEXT CHECK (state IN ('WAITING','ACCEPTED','DECLINED','EXPIRED','WITHDRAWN','PICKUP_PASSED'))`,
  `created_at`, `expires_at`, `resolved_at`, `booking_id FK NULL`,
  index on `(state, expires_at)` for the sweeper,
  partial unique on `(passenger_app_user_id) WHERE state = 'WAITING'` — one live request at a time.
- New `routing.route_occurrence_seat_release`:
  `id`, `route_occurrence_seat_id FK`, `released_from_fraction NUMERIC(9,8)`,
  `released_at`, `reason TEXT CHECK (reason IN ('EARLY_DROP','NO_SHOW','CANCELLED'))`,
  `resold_booking_id FK NULL`.
  This is what makes a freed seat a partial-route seat rather than a whole one.
- `trip.trip` — add `live_requests_muted BOOLEAN NOT NULL DEFAULT false`.
  Position, fraction and confidence all live in `location.trip_progress` from slice 12; this
  slice must not duplicate them.
- `booking.booking` — add `request_type TEXT NOT NULL DEFAULT 'SCHEDULED' CHECK (request_type IN ('SCHEDULED','LIVE'))`,
  `captured_on TEXT CHECK (captured_on IN ('TRIP_START','DRIVER_ACCEPT'))`.
- Index `idx_live_request_open ON booking.live_request(route_occurrence_id) WHERE state = 'WAITING'` — the pickup-passed sweep joins open requests to `location.trip_progress` on every tick.

## Configuration / environment changes

- Policy settings: `LIVE_REQUEST_EXPIRY_SECONDS` (45), `LIVE_PICKUP_SAFETY_MARGIN_FRACTION` (0.01), `LIVE_MAX_ADDED_MINUTES` (8, from UberX Share's published cap), `LIVE_MAX_ADDED_KM` (3).
- Staleness, corridor and extrapolation limits belong to slice 12's policy settings and are not redefined here.
- New scheduler job on slice 05's infrastructure: `live-request-expiry`, 5-second tick.
- Second job: `live-pickup-passed-sweep`, 5-second tick, invalidating open requests the driver has passed.

## UI / UX requirements

Backend slice. The contract must supply:

- P36 — each joinable trip led by minutes-away, with current location, km behind, why the seat is free, price and rate; plus the count of drivers who have already passed.
- P36b — the empty state's passed count and its explanation.
- P37 — seconds remaining and the "charged if he says yes" warning inputs.
- P37b — the captured amount, pickup place and ETA, wait rules, and the actions.
- P37c — that nothing was charged, nothing recorded, and the next candidate behind the rider.
- D16b — the single-glance payload above, and nothing more.
- D16c — the pickup-passed explanation and how live requests work.
- D16e — the seat-sold lapse with who took it.
- D18b / D22b — the freed seat with its remaining leg.

## Implementation steps

1. Implement `LiveMatchService.joinable(origin, destination, seats)` over `TripProgressFacade.candidateTripsNear(pickup, radiusMeters)`, then filter on: free seat (including partial releases valid at the rider's pickup fraction), `routeFraction < pickupFraction - SAFETY_MARGIN`, confidence in (`MATCHED`, `EXTRAPOLATED`), **added detour within `LIVE_MAX_ADDED_MINUTES` / `LIVE_MAX_ADDED_KM`**, and slice 08's eligibility predicate. Return `passedCount` as the count excluded **solely** by the fraction test — trips excluded by staleness, detour or eligibility are not "drivers who passed you".
2. Assert by architecture test that this slice writes no location state of its own.
3. Price each candidate through slice 03's facade on the rider's on-route overlap.
4. Implement live request creation: re-check behind-pickup, hold the seat (slice 07's mechanism), persist the quote, set `expires_at = now + 45s`, push the prompt to the driver, refuse if muted.
4a. Compute `addedMeters` / `addedMinutes` geometrically per candidate (no Google call) and filter on the detour cap before any prompt is created. **Assert by test that the joinable path issues zero external API calls.**
5. Enforce one open live request per passenger via the partial unique index.
6. Implement the driver prompt payload, deliberately minimal, delivered through slice 12's `RealtimeDeliveryService` — WebSocket when the driver app is foregrounded, FCM **high priority** otherwise, so Android Doze cannot swallow a 45-second offer.
7. Implement accept: verify still `WAITING`, still behind pickup, seat still held → create the booking with `request_type = LIVE`, `captured_on = DRIVER_ACCEPT`, call slice 04's capture immediately, confirm, open chat via slice 10's event.
8. Implement decline and withdraw; release the held seat on every terminal path.
9. Register `live-request-expiry` (5 s) and `live-pickup-passed-sweep` (5 s); both release the seat, notify the rider with the right reason, and record the terminal state.
10. Implement per-trip mute, defaulted from slice 08's `midTripBookings`.
11. Implement seat release on early drop-off with `released_from_fraction`, making the seat joinable for the remaining leg only, and mark it resold when taken.
12. Implement D16e: when a scheduled request lapses because the seat sold, record the reason and who took it.

## Files expected to change

- `apps/api/.../booking/**` — live request entity/service/controllers, seat holds, terminal handling.
- `apps/api/.../routing/**` — joinable query, partial seat releases.
- `apps/api/.../trip/**` — muted flag, fraction tracking, early-drop seat release.
- `apps/api/.../location/**` — fraction listener.
- `apps/api/.../payment/**` — capture-on-accept entry point (path already built in slice 04).
- `apps/api/.../scheduling/**` — the two 5-second jobs.
- `apps/api/src/main/resources/db/migration/V040__live_en_route_booking.sql`.
- `apps/api/src/test/java/**` — behind-pickup tests including loop routes, expiry tests, capture-on-accept tests, partial seat release tests, concurrency tests on accept vs expiry.
- `docs/api/mobile-app.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/13-live-en-route-booking-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='BehindPickupPredicateTest,LoopRouteFractionTest,LiveRequestExpiryIT,CaptureOnAcceptIT,PartialSeatReleaseIT,AcceptVsExpiryConcurrencyIT,LiveMuteTest' test
```

```bash
bash scripts/simulation/verify-live-booking.sh
```

The smoke drives a seeded running trip along its route with injected location samples and must prove: a
rider ahead of the driver sees the trip; the same rider stops seeing it once the driver passes; an open
request is invalidated mid-flight when the driver passes the pickup; accept captures immediately and
exactly once; expiry at 45 seconds charges nothing and releases the seat; a seat freed at Kirulapone is
offerable only for the remaining leg.

## Security, privacy, and observability checks

- **The behind-pickup rule is a safety and fairness rule, and it is server-enforced or it is nothing.** Test it at the service layer with a crafted request id, not only through the search path.
- A stale position must fail closed. If `last_known_fraction_at` is older than the staleness window, the trip is not offerable — an unknown position must never be assumed to be behind.
- Location precision: the joinable list exposes a place label and a distance, never the driver's raw coordinates.
- The driver prompt shows the passenger's first name and initial only, and their photo only per slice 08's visibility rules.
- Accept and expiry race for the same request; the state transition must be a guarded update (`WHERE state = 'WAITING'`) so exactly one wins. Test concurrently — a lost race here double-captures or strands a seat.
- Capture-on-accept means money moves while someone is driving. Failures must not block the trip; they mark the booking and notify, exactly as slice 04 does.
- Metrics: `routeshare_live_searches_total`, `routeshare_live_requests_total{state}`, `routeshare_live_pickup_passed_total`, `routeshare_live_accept_latency_seconds`, `routeshare_partial_seat_resales_total`.
- Alert if the pickup-passed sweep falls behind its tick — a late sweep is a request that should have been void.

## Done criteria

- [ ] Joinable search returns only running trips with a valid free seat whose driver is still behind the rider's pickup, and reports the passed count.
- [ ] Behind-pickup is a route-fraction comparison, correct on looping routes, and fails closed on stale positions.
- [ ] Live requests expire at 45 seconds by server sweep and release the seat.
- [ ] Open requests are invalidated the moment the driver passes the pickup, with the D16c explanation.
- [ ] Accept captures immediately, exactly once, and never blocks the trip on failure.
- [ ] Accept vs expiry races resolve to exactly one outcome.
- [ ] One open live request per passenger.
- [ ] Per-trip mute works and defaults from driving preferences.
- [ ] Candidates exceeding the detour cap are filtered before the driver is prompted.
- [ ] Detour distance and minutes are derived from route geometry and observed speed; **zero Google calls on the joinable or prompt path**, asserted by the cost-control smoke.
- [ ] This slice writes no location state of its own; proven by architecture test.
- [ ] A seat freed by an early drop-off is resold for the remaining leg only.
- [ ] D16e records why a scheduled request lapsed and who took the seat.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): add live en-route booking with behind-pickup enforcement and capture on accept"
```
