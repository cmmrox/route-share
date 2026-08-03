# QA — Task 05: Trip Timers and Reliability

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/05-trip-timers-and-reliability.md`

## Scope

The scheduler infrastructure, the four clocks (start buffer, pickup wait, driver-late grace,
early-drop allowance), reliability counters, monthly reset, driver deactivation and the passenger prepay
flag. Out of scope: the penalty amounts these clocks trigger (slice 06).

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V032` (`V031` timing/reliability plus the materialisation repair).
- Injected `Clock` available for time travel; **no test may sleep in real time**.
- Two API instances running for the leader-election case.

## Automated test coverage

As built (the names below replace the ones this file planned; the coverage is the same or wider):

- `SchedulerLeaderElectionIT` — eight simulated instances, one execution.
- `TripStartWindowTest` — the start-buffer arithmetic on explicit instants.
- `TripStartWindowSweepTest` — auto-cancel at expiry, and the refusals: a started, boarding or
  manually-cancelled trip is never auto-cancelled.
- `TripLifecycleServiceImplTest` + `TripMaterialisationIT` — one trip per occurrence, proven under
  twenty concurrent first-bookings against a real database.
- `ArrivalDetectorTest` — geofence plus dwell; a drive-past does not trigger, and dwell does not
  accumulate across two passes.
- `PickupWaitTest` / `PickupWaitServiceImplTest` — the wait, its single extension, the no-show
  release, and the two refusals (early release, and a driver acting on a trip that is not his).
- `DriverLateGraceServiceImplTest` — free cancel at the grace, cancellation terms, and that a
  driver already detected at the pickup does not unlock one.
- `ReliabilityCounterTest` — counters as projections of the event log, rebuild, and a month
  boundary with the prior month left readable.
- `EarlyDropAllowanceServiceImplTest` — 1st and 2nd adjusted, 3rd not and not recorded.
- `ReliabilityGateServiceImplTest` — third missed start deactivates driving only.
- `ApplicationContextLoadsIT` — the graph wires, and every clock has a registered job with a unique
  name. Added after a bean cycle passed a fully green suite.

Not written as separate integration tests: `StartBufferExpiryIT`, `PickupWaitExpiryIT` and
`DriverLateGraceIT`. Their behaviour is covered by the sweep unit tests above on an injected clock,
and end to end by the runtime smoke script, which drives real expiries through the live scheduler
rather than calling the sweep directly.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 05-1 | Two instances, one tick | Job body executes once |
| 05-2 | Scheduler restart mid-window | Clock survives; deadline unchanged |
| 05-3 | Departure + 9 min, not started | Nothing yet |
| 05-4 | Departure + 11 min, not started | Auto-cancelled; all bookings voided; nobody charged |
| 05-5 | Extension taken at +9 min | Deadline moves to +20 min exactly once |
| 05-6 | Second extension attempted | `EXTENSION_ALREADY_USED`; `extensionsRemaining=0` |
| 05-7 | Auto-cancel notification | Every rider notified with alternatives |
| 05-8 | GPS arrival within geofence for the dwell period | Pickup wait starts; passenger notified |
| 05-9 | Drive-past through the geofence | No wait started |
| 05-10 | Driver taps arrived while 2 km away | Refused — arrival is GPS-derived |
| 05-11 | Wait + 5 min | Extension available |
| 05-12 | Wait + 10 min after extension | Seat released; `NO_SHOW` recorded; `booking.noshow` emitted |
| 05-13 | Promised pickup + 11 min, driver not arrived | Free cancel unlocked; `cancellation-terms` says so |
| 05-14 | Start buffer expired but grace not | Two independent clocks, independent outcomes (P35) |
| 05-15 | Free cancel taken after grace | Nothing recorded against the passenger |
| 05-16 | 1st and 2nd early drop in a month | Fare adjusted |
| 05-17 | 3rd early drop | Seat released, fare stands, `EARLY_DROP_ALLOWANCE_EXHAUSTED` surfaced as data not an error |
| 05-18 | 3rd missed start in a month | Driver deactivated; riding works; pending payout intact |
| 05-19 | 2nd no-show in a month | `prepayRequired=true` on `/me/context` |
| 05-20 | Month boundary crossed | Counters reset; prior month readable |

## Manual checks

- Kill the scheduler mid-sweep and confirm no deadline is lost and no action is applied twice on restart.
- Confirm every automatic action writes an audit row naming the job, the rule and the computed deadline.
- Attempt to influence a deadline with a client-supplied timestamp; confirm it is ignored.

## Evidence to collect

- `scripts/simulation/verify-trip-timers.sh` output with the injected clock trace.
- `scheduling.job_run` extract for the run.
- Audit rows for one auto-cancel, one seat release and one deactivation.

## Execution record

**2026-08-02 — `scripts/simulation/verify-trip-timers.sh`: 42 passed, 0 failed, 0 skipped.**

Run against PostgreSQL on host port 5434 (database `routeshare_comigo`) with the API on 8088 and the
scheduler on a 15s tick. Every clock was driven to a real expiry and swept by the live leader-elected
job; deadlines were moved by rewriting the stored row, never by waiting. The deactivation case was
driven to three genuine auto-cancels rather than asserted from a seeded counter, so the trigger
itself was exercised.

Case coverage from that run:

| # | Result | Note |
| --- | --- | --- |
| 05-1 | covered | `SchedulerLeaderElectionIT`, eight instances |
| 05-2 | covered | deadlines are rows; the API was restarted mid-slice and windows survived |
| 05-3 / 05-4 | PASS | expiry auto-cancelled, trip CANCELLED, nobody captured |
| 05-5 / 05-6 | PASS | deadline moved once, measured from the buffer; second attempt 409 |
| 05-7 | partial | passengers are notified via `NotificationFacade`; alternatives are slice 07's |
| 05-8 / 05-9 | PASS | dwell started a wait; a drive-past did not |
| 05-10 | PASS | no endpoint starts a wait; acting on one that never started is 404 |
| 05-11 / 05-12 | PASS | seat released, `NO_SHOW` recorded, `booking.noshow` published |
| 05-13 | PASS | free cancel unlocked; `cancellation-terms` says free, why, and nothing recorded |
| 05-14 | PASS | the grace deadline is asserted to differ from the start-buffer deadline |
| 05-15 | PASS | `recordedAgainstPassenger=false` on a driver-late cancel |
| 05-16 / 05-17 | PASS | allowance readable; exhaustion returned as data on a 200 |
| 05-18 | PASS | deactivated at the limit, account still ACTIVE, future occurrences withdrawn |
| 05-19 | covered | `prepayRequired` reads the same counter; unit-covered, not driven to 2 no-shows at runtime |
| 05-20 | covered | `ReliabilityCounterTest` crosses the boundary on an injected clock |

**Not verified.** 05-4's "all bookings voided" is asserted against mocks only: no gateway exists on
the local stack, so the void cannot be observed. The runtime check asserts the weaker, checkable
property — that auto-cancel captured nothing. Tracked as **Blocker 015**, which stays OPEN.

05-19 and 05-20 are unit-covered but were not driven end to end at runtime; the script does not
manufacture a second no-show for one passenger or cross a month boundary. Recorded here rather than
counted as passes.

## Pass/fail criteria

Pass when: leader election holds; all four clocks fire at the right moment from server time only;
extensions are single and visible; the two clocks in 05-14 behave independently; counters project
correctly and reset monthly; and deactivation touches driving only.

Fail on: any duplicated job execution, any deadline influenced by client input, any automatic penalty
without an audit trail, or a test that passes only because it slept.
