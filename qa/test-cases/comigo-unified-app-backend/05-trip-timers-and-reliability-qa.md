# QA — Task 05: Trip Timers and Reliability

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/05-trip-timers-and-reliability.md`

## Scope

The scheduler infrastructure, the four clocks (start buffer, pickup wait, driver-late grace,
early-drop allowance), reliability counters, monthly reset, driver deactivation and the passenger prepay
flag. Out of scope: the penalty amounts these clocks trigger (slice 06).

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V031`.
- Injected `Clock` available for time travel; **no test may sleep in real time**.
- Two API instances running for the leader-election case.

## Automated test coverage

- `SchedulerLeaderElectionIT` — two instances, one execution.
- `StartBufferExpiryIT` — auto-cancel at 10 min, extension moves it once, charges nobody.
- `PickupWaitExpiryIT` — release at 5 (+5) min from detected arrival.
- `DriverLateGraceIT` — free cancel unlocked at 10 min past the promised pickup.
- `ArrivalDetectionTest` — geofence plus dwell; a drive-past does not trigger.
- `ReliabilityCounterTest` — counters as projections of the event log; monthly reset across a boundary.
- `EarlyDropAllowanceTest` — 1st and 2nd adjusted, 3rd not.
- `DeactivationTriggerTest` — third missed start deactivates driving only.

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

## Pass/fail criteria

Pass when: leader election holds; all four clocks fire at the right moment from server time only;
extensions are single and visible; the two clocks in 05-14 behave independently; counters project
correctly and reset monthly; and deactivation touches driving only.

Fail on: any duplicated job execution, any deadline influenced by client input, any automatic penalty
without an audit trail, or a test that passes only because it slept.
