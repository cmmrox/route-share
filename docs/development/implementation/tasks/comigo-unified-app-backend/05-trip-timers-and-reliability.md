---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 05 — Trip Timers and Reliability

**Goal:** Build the four clocks the product runs on — start buffer, pickup wait, driver-late grace, early-drop allowance — plus the reliability counters they feed, and the leader-elected scheduler that makes any of it safe.

**Depends on:** 04.
**Blocks:** 06, 07, 12, 15.

## Objective

Sixteen screens are driven by a timer that no backend job currently runs. The prototype is precise about
each one, and about what happens when it expires — who is charged, whose record it affects, and what the
third occurrence in a month costs.

This is also where the scheduler itself gets built. Eleven time-driven behaviours across the plan need it,
and without leader election a two-instance deploy double-cancels trips and double-charges no-show fees.

## Scope

In scope:

- **Scheduler infrastructure**: ShedLock on Postgres, a `scheduling` module, job registry, per-job metrics and failure alerting.
- **Start buffer** — 10 min from departure, one 10-min extension, then auto-cancel (D32, D32c, D32b, P24, P35).
- **Pickup wait** — 5 min from GPS arrival, one 5-min extension, then seat release as a no-show (D19, D19b, P38, P38b, D21, P27).
- **Driver-late grace** — 10 min past the passenger's promised pickup time unlocks a free cancel (P34, D41).
- **Early-drop allowance** — 2 fare-adjusted drops per calendar month, 3rd onward the fare stands (P16, P16b, D22b).
- **Reliability counters** — driver and passenger, per calendar month, with monthly reset.
- **Driver deactivation trigger** at 3 missed starts, calling slice 01's deactivation path.
- **Passenger prepay flag** at 2 no-shows in a month.

Out of scope:

- The penalty *amounts* charged when these clocks expire — slice 06 owns assessment and the 50/50 split. This slice fires typed events; slice 06 consumes them.
- Seat resale after a release — slice 07.
- Rating aggregates — slice 15.

## Source material / references

- `docs/source-assets/comigo-prototype/data.jsx` — `POLICY.startBufferMin/startExtendMin/startExtendLimit/missedStartLimit/pickupWaitMin/pickupWaitExtendMin/pickupWaitExtendLimit/driverLateGraceMin/earlyDropAdjustedPerMonth`, `DRIVER_RELIABILITY`, `PAX_RELIABILITY`, `EARLY_DROP`.
- `docs/source-assets/comigo-prototype/driver-supply.jsx` — D32/D32b/D32c start buffer, all three states.
- `docs/source-assets/comigo-prototype/driver-live.jsx` — D19/D19b arrived + extension, D21 release a no-show.
- `docs/source-assets/comigo-prototype/driver-late.jsx` — D41 driver was late.
- `docs/source-assets/comigo-prototype/passenger-status.jsx` — P38/P38b rider's side of the wait, P39 rider reliability.
- `docs/source-assets/comigo-prototype/driver-money.jsx` — D28 reliability panel, D34 deactivation.
- `docs/source-assets/comigo-prototype/passenger-trip.jsx` — P16/P16b early-drop allowance.
- Current code: `trip/**`, `location/**` (GPS arrival), `common/event/**` (outbox + relay).

## Architecture and design notes

**The two clocks are genuinely different**, and P35 exists to say so. The start buffer runs from the
trip's departure time and protects the *driver* from auto-cancellation. The driver-late grace runs from
*this passenger's* promised pickup time and protects *her*. A trip that left on time can still be twenty
minutes from her corner. Modelling them as one timer would produce exactly the bug P35 was drawn to
prevent — the extension is his protection, not an obligation on her.

**Pickup wait starts on GPS arrival, not on a tap.** D19 says "started automatically on arrival". A
driver-triggered clock lets a no-show be manufactured two streets away. Arrival is detected from the
existing `location.location_sample` stream against the booking's pickup point, with a geofence radius and
a dwell requirement to avoid a drive-past triggering it.

**Extensions are single, spendable, and must disappear from the UI.** D32c shows the button replaced by a
disabled "Extension already used" rather than failing on tap. So the API returns `extensionsRemaining`,
not just a success/failure on use.

**Timers are database state, not in-memory.** Each is a row with `expires_at`, so a restart cannot lose a
clock and any instance can process any expiry. The scheduler only *finds* expired rows; the transition
logic is the same service method a manual action calls.

**Counters are per calendar month**, reset on the 1st, and read from an event log rather than incremented
in place — so a correction is possible and the D28/P39 panels can show what happened, not just a number.

**Scheduler jobs registered in this slice:**

| Job | Tick | Finds | Action |
| --- | --- | --- | --- |
| `start-buffer-expiry` | 1 min | trips past buffer (+ extension) not started | auto-cancel, void, `trip.autocancelled` |
| `pickup-wait-expiry` | 1 min | waits past 5 (+5) min | release seat, `booking.noshow` |
| `driver-late-grace` | 1 min | confirmed bookings past promised pickup + 10 min, driver not arrived | unlock free cancel, notify, `booking.driver_late` |
| `monthly-counter-reset` | daily 00:05 | — | close the month, open the next |

The remaining seven jobs are registered by slices 07, 10, 11, 12 (two) 13 and 14 against the same infrastructure.

## API contracts involved

Driver:

```
POST /api/v1/driver/trips/{tripId}/start-extension        -> one 10-min extension
GET  /api/v1/driver/trips/{tripId}/start-window           -> {departsAt, expiresAt, extensionsRemaining, secondsRemaining}
POST /api/v1/driver/trips/{tripId}/passengers/{bookingId}/wait-extension   -> one 5-min extension
GET  /api/v1/driver/trips/{tripId}/passengers/{bookingId}/wait-window
POST /api/v1/driver/trips/{tripId}/passengers/{bookingId}/release-seat     -> no-show release
GET  /api/v1/driver/reliability                            -> D28 panel
```

Passenger:

```
GET  /api/v1/passenger/bookings/{id}/pickup-window        -> P38/P38b countdown + consequences
GET  /api/v1/passenger/bookings/{id}/cancellation-terms   -> whether a free cancel is unlocked and why
GET  /api/v1/passenger/reliability                        -> P39 panel
GET  /api/v1/passenger/early-drop-allowance               -> {month, used, allowance, remaining}
```

Admin: `GET /api/v1/admin/reliability/deactivations`, and the existing deactivate/reinstate from slice 01
now also receive automatic deactivations.

`DriverReliabilityResponse`: `month`, `missedStarts{count,limit}`, `lateCancellations{count,limit}`,
`startExtensionsUsed`, `onTimeStartPct`, `acceptancePct`, `deactivationRisk{remaining}`.

`PassengerReliabilityResponse`: `month`, `completionPct`, `noShows{count,prepayThreshold}`,
`lateCancels`, `onTimeAtPickupPct`, `prepayRequired`.

New errors: `EXTENSION_ALREADY_USED`, `WAIT_NOT_STARTED`, `START_WINDOW_EXPIRED`,
`EARLY_DROP_ALLOWANCE_EXHAUSTED`, `DRIVER_DEACTIVATED`.

## Database / migration changes

**`V031__trip_timers_and_reliability.sql`**

- New `scheduling.shedlock` — standard ShedLock table (`name PK`, `lock_until`, `locked_at`, `locked_by`).
- New `scheduling.job_run` — `id`, `job_name`, `started_at`, `finished_at`, `status`, `processed_count`, `error`. Job observability without scraping logs.
- New `trip.trip_start_window`:
  `trip_id PK FK`, `departs_at`, `buffer_expires_at`, `extension_used BOOLEAN DEFAULT false`,
  `extended_expires_at`, `resolved_at`, `resolution TEXT CHECK (resolution IN ('STARTED','AUTO_CANCELLED','CANCELLED'))`.
  Index on `(resolved_at, COALESCE(extended_expires_at, buffer_expires_at))` for the sweeper.
- New `trip.pickup_wait`:
  `id`, `trip_id FK`, `booking_id FK UNIQUE`, `arrived_at`, `expires_at`, `extension_used BOOLEAN`,
  `extended_expires_at`, `resolved_at`, `resolution TEXT CHECK (resolution IN ('BOARDED','NO_SHOW','CANCELLED'))`.
- New `trip.driver_late_grace`:
  `id`, `booking_id FK UNIQUE`, `promised_pickup_at`, `grace_expires_at`, `unlocked_at`, `resolved_at`,
  `resolution TEXT CHECK (resolution IN ('PICKED_UP','FREE_CANCELLED','EXPIRED'))`.
- New `reliability.monthly_counter`:
  `id`, `app_user_id FK`, `role TEXT CHECK (role IN ('DRIVER','PASSENGER'))`, `period_month DATE`,
  `missed_starts INT DEFAULT 0`, `late_cancellations INT DEFAULT 0`, `start_extensions_used INT DEFAULT 0`,
  `no_shows INT DEFAULT 0`, `late_cancels INT DEFAULT 0`, `early_drops_adjusted INT DEFAULT 0`,
  `trips_completed INT DEFAULT 0`, `trips_booked INT DEFAULT 0`, `on_time_events INT DEFAULT 0`,
  `on_time_opportunities INT DEFAULT 0`, `UNIQUE (app_user_id, role, period_month)`.
- New `reliability.reliability_event` — append-only log: `id`, `app_user_id`, `role`, `event_type`,
  `occurred_at`, `booking_id`, `trip_id`, `metadata JSONB`. Counters are projections of this.
- `booking.booking` — add `promised_pickup_at TIMESTAMPTZ` (per-passenger, distinct from trip departure).

## Configuration / environment changes

- `ROUTESHARE_SCHEDULER_ENABLED` (default `true`), `ROUTESHARE_SCHEDULER_TICK_SECONDS` (default `60`).
- `ROUTESHARE_PICKUP_ARRIVAL_GEOFENCE_METERS` (default `120`), `ROUTESHARE_PICKUP_ARRIVAL_DWELL_SECONDS` (default `30`) — arrival detection tuning; not policy, so these stay as properties.
- All durations and limits (`startBufferMin`, `startExtendMin`, `pickupWaitMin`, `driverLateGraceMin`, `missedStartLimit`, `earlyDropAdjustedPerMonth`, `prepayThreshold`) read from `platform.policy_setting` per slice 03.
- New dependency: `net.javacrumbs.shedlock:shedlock-spring` + `shedlock-provider-jdbc-template`.

## UI / UX requirements

Backend slice. The contract must supply countdowns and consequences without client-side policy knowledge:

- D32 / D32c / D32b — seconds remaining, whether an extension is available, what happens at zero, how many missed starts this month and how many remain before deactivation.
- D19 / D19b — seconds remaining, extension availability, what a release costs the passenger and credits the driver.
- P38 / P38b — the same clock from the rider's side, the fee, its split, and her no-show count this month.
- P34 / P35 — whether a free cancel is unlocked, why, and that nothing is recorded against her.
- P16 / P16b — allowance used this month and whether this drop will be adjusted.
- D28 / P39 — the full reliability panels.
- D34 — the three misses with dates, routes and rider counts.

## Implementation steps

1. Add the `scheduling` module: ShedLock config, a `ScheduledJob` interface, a registry, `job_run` recording, per-job metrics and failure alerting. Prove leader election with a two-instance test.
2. Add `trip.trip_start_window`, created when a trip is published/generated; resolved on start or cancel.
3. Implement start-buffer expiry: auto-cancel the trip, void via slice 04, record a `MISSED_START` reliability event, notify all booked passengers with alternatives, emit `trip.autocancelled`.
4. Implement the single start extension with `extensionsRemaining` in the response.
5. Add arrival detection: a `location` module listener matching samples against the next pickup point, with geofence + dwell, creating `trip.pickup_wait` and notifying the passenger ("Notified you're here").
6. Implement pickup-wait expiry: release the seat, mark the passenger trip state `NO_SHOW`, record a `NO_SHOW` reliability event, emit `booking.noshow` for slice 06 to price.
7. Implement the single wait extension.
8. Add `trip.driver_late_grace` seeded from `booking.promised_pickup_at`; on expiry unlock the free cancel, notify her, emit `booking.driver_late`.
9. Implement `cancellation-terms` returning whether a cancel is free right now and the reason code — this is the single source both P26 and P34 read, so the client never decides.
10. Add the early-drop allowance check in `reliability`, consumed by the early-drop endpoint from slice 04: within allowance → reprice; beyond → seat still released, fare stands.
11. Build `reliability.reliability_event` + counter projection + monthly reset job; expose the driver and passenger panels.
12. Wire the deactivation trigger at `missedStarts == limit`, calling slice 01's deactivation with reason and case ref; notify, and cancel that driver's future published occurrences.
13. Wire the passenger prepay flag at `noShows == threshold`, exposed on `/me/context` for the app to warn.

## Files expected to change

- `apps/api/.../scheduling/**` — new module.
- `apps/api/.../trip/**` — start window, pickup wait, late grace, extensions, release-seat.
- `apps/api/.../location/**` — arrival detection listener.
- `apps/api/.../reliability/**` — new module: events, counters, panels, gates.
- `apps/api/.../booking/**` — `promised_pickup_at`, cancellation terms.
- `apps/api/.../driver/**` — deactivation trigger integration.
- `apps/api/.../platform/**` — `/me/context` gains `prepayRequired`.
- `apps/api/src/main/resources/db/migration/V031__trip_timers_and_reliability.sql`.
- `apps/api/pom.xml` — ShedLock.
- `apps/api/src/test/java/**` — clock-controlled timer tests (using the existing `ClockConfig`), leader-election test, arrival-detection tests, counter projection tests, deactivation trigger test.
- `docs/api/mobile-app.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/05-trip-timers-and-reliability-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='StartBufferExpiryIT,PickupWaitExpiryIT,DriverLateGraceIT,ReliabilityCounterTest,SchedulerLeaderElectionIT,ArrivalDetectionTest,EarlyDropAllowanceTest' test
```

```bash
bash scripts/simulation/verify-trip-timers.sh
```

The smoke script drives a seeded trip through each clock with an injected clock: buffer expiry
auto-cancels and charges nobody; the extension moves the deadline exactly once; GPS arrival starts the
wait; wait expiry releases the seat; the grace unlocks a free cancel; the third missed start deactivates
the driver and leaves riding intact.

## Security, privacy, and observability checks

- Timers move money, so clock manipulation is an attack surface. All expiry decisions use server time via the injected `Clock`; no client timestamp is ever trusted.
- Arrival detection must be resistant to spoofed location. Require dwell, and record the samples that triggered arrival so a disputed no-show is investigable.
- A driver must not be able to trigger a no-show release before the clock expires; assert at service level.
- Every automatic action (auto-cancel, seat release, deactivation) writes an audit row with the job name, the rule and the computed deadline — an automated penalty with no trail is indefensible in a support ticket.
- Metrics per job: `routeshare_job_runs_total{job,status}`, `routeshare_job_processed_total{job}`, `routeshare_job_duration_seconds{job}`, plus `routeshare_autocancels_total`, `routeshare_noshow_releases_total`, `routeshare_driver_deactivations_total`.
- Alert if any job has not completed successfully within 3 ticks — a silently dead sweeper strands riders and money.

## Done criteria

- [x] ShedLock-backed scheduler runs exactly once across instances; proven by an integration test.
- [x] All four clocks implemented with database-backed deadlines and single extensions.
- [x] Pickup wait starts from detected GPS arrival, not a driver tap. No endpoint can start one.
- [x] Start buffer and driver-late grace are separate clocks with separate consequences; asserted
      at runtime that their deadlines differ.
- [x] Auto-cancel charges nobody — **partially**: verified at runtime that it *captures* nobody, but
      the void itself is asserted against mocks, since no gateway exists locally (Blocker 015).
      Riders are notified; **alternatives are not offered**, as seat resale is slice 07's.
- [x] Reliability counters are projections of an append-only event log, reset monthly.
- [x] 3 missed starts deactivates driving only, leaving riding and pending payouts intact; verified
      at runtime, driven to three real auto-cancels.
- [x] 2 no-shows in a month sets the prepay flag on `/me/context` — unit-covered; **not driven to a
      second no-show at runtime**.
- [x] Early-drop allowance enforced; the 3rd drop releases the seat without repricing, surfaced as
      data on a 200 rather than an error.
- [x] `./mvnw spotless:check verify` green (423 tests, 0 skipped), JaCoCo held.
- [x] Tracking docs updated; focused commits ready.

## Suggested commit message

```bash
git commit -m "feat(api): add trip timers, reliability counters, and a leader-elected scheduler"
```

## Progress

### 2026-08-02 (later) — steps 5 to 13 landed: the slice is complete

All four clocks run, the panels are served, and the whole thing has been exercised against a real
database rather than only against mocks. `scripts/simulation/verify-trip-timers.sh` → **42 passed,
0 failed, 0 skipped**, including the deactivation trigger.

**The first thing found was that no clock could ever have fired.** Nothing in the application
created a `trip.trip` row at all: publication produced a route plan and a run of occurrences and
stopped there. The start window opened for nobody and the sweeper swept an empty table. It also
explains a quiet skip in slice 04's smoke script — `SKIP: no trip row for the booked route` — which
means slice 04's card-capture checks have never run either, and nothing recorded that.

A trip is now materialised when an occurrence takes its **first confirmed booking**, which is the
moment the clock first has stakes. Creating one per generated occurrence, as a literal reading of
step 2 suggests, would put every unbooked occurrence under the sweeper: each would auto-cancel and
record a missed start against a driver who did nothing wrong, so three days of an empty recurring
route would deactivate them. `V032` adds the partial unique index that arbitrates two passengers
taking the last two seats at once, and `TripMaterialisationIT` proves it under twenty threads.

Two live bugs were found alongside it, both of which would have fired the moment windows started
being created:

- `resolveStarted`/`resolveCancelled` were **dead code**. Nothing resolved a window on start, so a
  trip started at +5 was still auto-cancelled at +11 — voiding holds captured minutes earlier, on a
  car already moving. The start path now resolves in the same transaction, *and* the sweeper
  independently refuses to act on a trip whose own status says it has moved. One line in the start
  path is exactly what gets lost in a later refactor, and the cost of losing it is too high to rest
  on.
- `TripRepository.updateStatus` stamped `started_at`/`completed_at` from `Instant.now()` rather than
  the injected `Clock`, against this slice's own rule.

**Arrival detection (step 5)** is geofence *and* dwell, kept pure in `ArrivalDetector` so the rule a
disputed no-show turns on is readable without a database. The detector walks back from the newest
sample only while the driver stayed inside the fence, so dwell cannot accumulate across two passes
of a loop, and arrival is dated from *entering* the fence rather than from when the dwell completed
— dating it later would hand the driver back the seconds the dwell cost and shorten her wait.
Distance is computed by PostGIS on the geography type rather than re-derived in Java, so there is
one answer to the question a dispute turns on. The qualifying sample ids go into
`pickup_wait.triggered_by_samples`.

**The pickup wait (steps 6–7)** releases the seat, marks `NO_SHOW`, records the reliability event
and publishes `booking.noshow`. Two refusals matter more than the happy path, and both sit at
service level so the sweeper and the endpoint obey the same rule: a release before the deadline is
refused, and ownership is checked rather than merely the DRIVER role — without that, any driver on
the platform could read another driver's passenger's countdown, spend her extension, or release her
seat.

**The driver-late grace (step 8)** runs from her promised pickup, derived server-side from the
occurrence's departure and how far along his road she gets on. A driver already detected at her
pickup does not unlock a free cancel however late the clock reads. **`cancellation-terms` (step 9)**
is the single source both P26 and P34 read.

**The early-drop allowance (step 10)** adjusts twice a month. The third drop is not refused — she is
getting out of the car either way — the seat is released, the fare stands, and
`EARLY_DROP_ALLOWANCE_EXHAUSTED` comes back as data on a 200.

**Panels and reset (step 11)**: D28/P39 read the counter, D34 reads the log itself. Percentages with
no opportunities behind them return `null` rather than `0%`.

**Deactivation (step 12)** lives with the counter rather than with the auto-cancel that happened to
record the third miss, so a missed start recorded from anywhere fires the same rule. It stops
driving only, and withdraws that driver's future published occurrences. **The prepay flag (step
13)** is read from the same counter rather than stored separately.

**Three defects that only a runtime run could find**, all fixed here:

- `ReliabilityService.counter()` created the month's row on read. Every countdown in this slice
  renders "no-shows this month" inside a read-only transaction, so the **first read by any user was
  a 500**. Green unit tests throughout — none of them used a real transaction.
- `reliability_event.metadata` and `pickup_wait.triggered_by_samples` are `jsonb` mapped as
  `String`; Hibernate bound varchar and Postgres refused the cast. The `metadata` bug has been in
  the merged code since the column was added and never fired only because every caller so far
  passed `null`.
- A **bean cycle**: `DriverFacade` gained a write needing `DriverDeactivationService`, which needs
  identity, which depends back on `DriverFacade`. The whole suite stayed green because every unit
  test builds its own collaborators; nothing ever asked Spring to wire the graph. Split onto
  `DriverDeactivationFacade` rather than papered over with `@Lazy`, and `ApplicationContextLoadsIT`
  now closes that hole in the gate.

Gate: `./mvnw spotless:check verify` → **BUILD SUCCESS, 423 tests, 0 skipped, JaCoCo met**.
Contract: `redocly lint docs/api/mobile-app.openapi.json` → valid. `pnpm run typecheck` in
`packages/api-contracts` → clean.

**Still not verified:** the card path — authorise → capture → void — has still never run against any
gateway, so the auto-cancel's void is asserted against mocks only. **Blocker 015 stays OPEN**; the
Cybersource sandbox was unavailable when this slice was built and the owner will supply credentials
when it is.

### 2026-08-02 — steps 1–4 landed: scheduler, the start-buffer clock, and the reliability log

Merged to `main`. The slice is **not complete** — three of the four clocks remain — but what is
merged is coherent and running: the scheduler sweeps, and the first clock it sweeps is real.

Done:

- `V031__trip_timers_and_reliability.sql` — **all** tables for all four clocks plus reliability, so
  later steps add behaviour rather than schema: `scheduling.shedlock`, `scheduling.job_run`,
  `trip.trip_start_window`, `trip.pickup_wait`, `trip.driver_late_grace`,
  `reliability.reliability_event`, `reliability.monthly_counter`, and
  `booking.promised_pickup_at`. Applied against real PostGIS; the API boots on it.
- `scheduling` module: ShedLock on Postgres, `ScheduledJob`, `JobRegistry` (discovers every job
  bean, one lock for the tick), `JobRunner` (records a `job_run` row and emits the three per-job
  metrics whatever the outcome), and `SchedulerHealthIndicator` (DOWN when any job has not
  succeeded within three ticks).
- `SchedulerLeaderElectionIT` — 8 simulated instances, each with its own `LockProvider` over the
  same database: exactly one executes. Plus `JobRunnerImplTest` (5 cases, including that a throwing
  job is recorded and does not propagate — otherwise one wedged sweep stops every other clock).
- Runtime: ShedLock acquires `routeshare-scheduler-tick` and writes the row on a live stack.

- **The start-buffer clock (steps 2–4)** — `trip.trip_start_window` opened from departure,
  `TripStartWindowService` with a single spendable extension, `StartBufferExpiryJob` registered
  against the scheduler, and `GET /api/v1/driver/trips/{tripId}/start-window` +
  `POST /api/v1/driver/trips/{tripId}/start-extension`. Auto-cancel voids every hold **before**
  marking the trip cancelled, so a part-way failure leaves holds released rather than a cancelled
  trip with live authorisations. It records a `MISSED_START` and increments
  `routeshare_autocancels_total`.
- **The reliability log (part of step 11)** — `reliability.reliability_event` (append-only) and
  `reliability.monthly_counter` as its projection, with `rebuild()` so the projection can be treated
  as the cache it is. The event→column mapping lives in the entity so the live path and a rebuild
  cannot disagree.
- `TripStartWindowTest` — 9 cases on explicit instants, no sleeping. Includes that the extension is
  measured from the buffer and not from the moment it was tapped: extending from "now" would let a
  driver who waits until 9:59 buy nearly twenty extra minutes.
- Runtime: the job registers, ticks under the leader lock and records `SUCCEEDED` runs.

Still to do — steps 5 to 13:

- Arrival detection from `location.location_sample` with geofence + dwell (step 5), and the pickup
  wait and its extension (steps 6–7). `trip.pickup_wait` exists and is unused.
- The driver-late grace seeded from `booking.promised_pickup_at` (step 8) and `cancellation-terms`
  (step 9). `trip.driver_late_grace` exists and is unused.
- The early-drop allowance (step 10), the monthly reset job and both reliability panels (step 11).
- The deactivation trigger at three missed starts (step 12) — `MISSED_START` events are being
  recorded, but nothing yet counts them and calls `DriverDeactivationService.deactivate`.
- The prepay flag on `/me/context` (step 13).
- `TripStartWindowService.open` is **not yet called from route publication**, so windows are only
  created by a direct call. Wiring it into the publish/generate path is the first thing step 5
  should do — without it the sweeper has nothing to find.
- The remaining endpoints, `docs/api/mobile-app.openapi.json`, `packages/api-contracts`, and
  `scripts/simulation/verify-trip-timers.sh`.

## Deviations from the plan as written

- **`shedlock-provider-jdbc`, not `shedlock-provider-jdbc-template`.** The task names the
  JdbcTemplate provider, but `PersistenceArchitectureTest` bans the `JdbcTemplate` type from main
  sources, and a library integration is not a good reason to weaken a standing architecture rule.
  The plain-JDBC provider takes a `DataSource` and behaves identically for our use.
- **One lock for the whole tick, not one per job.** The task's table implies a lock per job. The
  jobs are short and share a database, so per-job locks would multiply lock churn without allowing
  any useful overlap. Correctness does not rest on this: each row transition must refuse to apply
  twice regardless, because a lock can lapse under a long GC pause.
- **`PAX_PREPAY_NO_SHOW_THRESHOLD` added to `platform.policy_setting`.** The task refers to a
  `prepayThreshold` policy value; it was the only figure this slice needs that slice 03 had not
  already seeded.
- **`V032` was added, and trips are materialised at first confirmed booking.** The task assumed
  trips already existed and step 2 says only "created when a trip is published/generated". Nothing
  created them at all, and creating one per generated occurrence would deactivate any driver whose
  recurring route went unbooked for three days. Recorded in full in the progress note above.
- **`promised_pickup_at` is derived from route length and a configured average speed**
  (`routeshare.routing.average-speed-kmh`, default 30). The plan does not say where the promise
  comes from, and there is no stored per-route duration: `route_plan` carries geometry and length
  but no travel time. Calling Google Directions inside the booking transaction would put a network
  call and an API key (Blocker 011) on the booking path. The derived value is deterministic and
  server-side, which is the property that actually matters — no client timestamp reaches it. When a
  real duration is stored, this becomes a one-line change behind the same query.
- **The monthly reset runs on the scheduler's own tick and no-ops except on the 1st**, rather than
  being wired to a `daily 00:05` cron as the job table says. The sweeper already holds the leader
  lock; a second scheduling mechanism would be a second thing to get wrong about time zones.
- **Booking status is left alone on a no-show.** The seat returns to inventory and the passenger
  trip state says `NO_SHOW`, but `booking.status` stays `CONFIRMED` for slice 06 to settle, since
  slice 06 owns what a no-show costs and slice 07 owns resale.
- **`WAIT_NOT_EXPIRED` was added** alongside the errors the task lists. The task names
  `WAIT_NOT_STARTED` but not the code for the case that actually protects the passenger — a driver
  trying to release a seat while her clock is still running.
