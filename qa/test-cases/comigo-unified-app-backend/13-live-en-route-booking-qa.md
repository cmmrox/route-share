# QA — Task 13: Live (En-Route) Booking

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/13-live-en-route-booking.md`

## Scope

Joinable-trip search, the behind-pickup rule, 45-second live requests, capture on accept,
pickup-passed invalidation, per-trip mute, and mid-trip seat resale for the remaining leg.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V039`.
- A running trip with injectable location samples along a known route, including a looping section.
- Injected `Clock` for the 45-second expiry cases.

## Automated test coverage

- `BehindPickupPredicateTest` — route-fraction comparison, not straight-line distance.
- `LoopRouteFractionTest` — a looping route does not offer a seat already passed.
- `LiveRequestExpiryIT` — 45-second sweep releases and notifies.
- `CaptureOnAcceptIT` — capture fires on accept, exactly once.
- `PartialSeatReleaseIT` — a freed seat is offerable only for the remaining leg.
- `AcceptVsExpiryConcurrencyIT` — exactly one outcome under a race.
- `LiveMuteTest` — per-trip mute, defaulted from preferences.
- `StaleFractionFailsClosedTest` — an unknown position is never offerable.
- `DetourCapTest` — candidates over 8 minutes or 3 km added are filtered before the prompt.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 13-1 | Driver 1.4 km before the rider's pickup | Trip appears in joinable results |
| 13-2 | Driver 0.5 km past the pickup | Trip absent; counted in `passedCount` |
| 13-3 | Looping route, driver geographically near but past by fraction | Absent — the loop case |
| 13-4 | Last position 90 s old | Absent — fails closed |
| 13-5 | `passedCount` on the empty state | Matches the seeded passed drivers |
| 13-6 | Request created | State `WAITING`, `expiresAt` 45 s out, seat held |
| 13-7 | Second concurrent live request by the same rider | Refused |
| 13-8 | Driver passes the pickup while the request is open | State `PICKUP_PASSED`; rider told; seat released; nothing charged |
| 13-9 | No answer for 45 s | `EXPIRED`; nothing charged; nothing recorded against either party |
| 13-10 | Driver accepts at 40 s | Booking created; **captured immediately** |
| 13-11 | Capture on accept called twice | One capture |
| 13-12 | Accept and expiry race | Exactly one wins; state consistent; no orphaned hold |
| 13-13 | Accept when the capture fails | Booking flagged; trip continues |
| 13-14 | Driver declines | Seat released; nothing charged |
| 13-15 | Rider withdraws | Seat released |
| 13-16 | Trip muted | New requests refused `LIVE_REQUESTS_MUTED` |
| 13-17 | Mute default | Follows the driver's `midTripBookings` preference |
| 13-18 | Request against a scheduled (not started) trip | `TRIP_NOT_RUNNING` |
| 13-19 | Early drop at Kirulapone frees a seat | Offerable for Kirulapone → Fort only |
| 13-20 | Rider boarding before Kirulapone requests that seat | Not offered |
| 13-21 | Driver prompt payload | Net kept, added km/min, km ahead, on board, seats free after — and nothing else |
| 13-22 | Prompt passenger identity | First name + initial; photo per slice 08 visibility |
| 13-23 | Behind-pickup bypass attempt via direct request id | Refused `PICKUP_ALREADY_PASSED` |
| 13-24 | Candidate adding 12 minutes of detour | Absent from joinable results; driver never prompted |
| 13-25 | Candidate adding 6 minutes | Offered |
| 13-26 | Candidate adding 5 km | Absent |
| 13-27 | Any H3 usage in this slice | None — candidate lookup goes through `TripProgressFacade` |
| 13-28 | Joinable search of 50 candidates, Redis `maps:*` before vs after | **Identical — zero Google calls**; detour is geometry ÷ observed speed |
| 13-29 | Detour minutes vs a manual Directions comparison | Within tolerance, and derived without calling Google |

## Manual checks

- Drive the seeded trip along its full route with injected samples and watch a rider's joinable list change at the exact fraction crossing.
- Confirm the pickup-passed sweep keeps pace: no request survives more than one tick past the crossing.
- Confirm capture-on-accept appears in the ledger with `captured_on = DRIVER_ACCEPT`.

## Evidence to collect

- `scripts/simulation/verify-live-booking.sh` output with the fraction trace.
- Ledger extract showing a live booking captured on accept.
- Sweep latency measurement for the 5-second jobs.

## Pass/fail criteria

Pass when: the behind-pickup rule is enforced server-side by route fraction, is correct on loops, and
fails closed on stale positions; requests expire at 45 s; accept captures exactly once; accept-vs-expiry
races resolve cleanly; and a freed seat sells only for its remaining leg.

Fail on: any seat offered to a rider the driver has passed, any double capture, any orphaned seat hold, or
a sweep that falls behind its tick.
