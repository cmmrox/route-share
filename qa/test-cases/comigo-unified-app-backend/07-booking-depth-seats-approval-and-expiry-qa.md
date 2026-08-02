# QA — Task 07: Booking Depth: Seats, Approval Modes and Expiry

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/07-booking-depth-seats-approval-and-expiry.md`

## Scope

Named seat inventory, approval modes, 30-minute request expiry, trip freeze, typed seat-race
conflicts, the two-open-requests rule, driver cancellation windows, alternatives, and counterparty phone
disclosure per plan §6.1.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V034`.
- Concurrency harness available for the seat-race cases.

## Automated test coverage

- `SeatInventoryConcurrencyIT` — two bookings, last seat, one winner.
- `RequestExpiryJobIT` — expiry at 30 min voids and releases.
- `TripFreezeTest` — editable at zero bookings, frozen at one.
- `OccurrenceCancellationTest` — window selection, reason codes, priced penalty, rider notification.
- `OpenRequestLimitTest` — third open request refused.
- `ContactDisclosureAuthorizationTest` — the full negative matrix.
- `SeatPriceIndependenceTest` — fare identical for every slot.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 07-1 | Seat map for a CAR occurrence | 3 slots: front (beside the driver), two back (rear row) |
| 07-2 | Two concurrent bookings for the last seat | One 201, one 409 `SEATS_TAKEN` with the closest alternative |
| 07-3 | Front vs back seat fare | Identical |
| 07-4 | Instant-book occurrence | Booking confirmed and authorised immediately |
| 07-5 | Approve-each occurrence | `REQUESTED` with `expiresAt` 30 min out |
| 07-6 | Request at 29 min | Still open, `secondsRemaining` accurate |
| 07-7 | Request at 31 min | `EXPIRED`; authorisation voided; seat released |
| 07-8 | Third open request | `TOO_MANY_OPEN_REQUESTS` |
| 07-9 | Edit an occurrence with zero bookings | Allowed |
| 07-10 | Edit an occurrence with one booking | 409 `TRIP_FROZEN` |
| 07-11 | Cancel at 26 h out | No penalty; riders notified |
| 07-12 | Cancel at 3 h out | Penalty assessed via slice 06; riders notified with their share |
| 07-13 | Cancel with no reason code | 400 |
| 07-14 | Alternatives on a declined booking | Other drivers on the corridor, ranked by match then price |
| 07-15 | Contact on a `REQUESTED` booking | `CONTACT_NOT_AVAILABLE` |
| 07-16 | Contact on a `CONFIRMED` booking, both directions | Number returned to both, audited |
| 07-17 | Contact 25 h after drop-off | `CONTACT_NOT_AVAILABLE` |
| 07-18 | Contact after cancellation | `CONTACT_NOT_AVAILABLE` |
| 07-19 | Another passenger on the same trip requests a number | 403 |
| 07-20 | A driver of a different trip requests a number | 403 |
| 07-21 | Seat holds after every terminal path | Zero leaked holds |

## Manual checks

- Run the seat-race case at least 20 times; a single double-allocation is a fail.
- Verify every contact disclosure wrote an audit row, including repeated reads.
- Confirm the contact-disclosure volume alert fires under a scripted harvesting pattern.

## Run record — 2026-08-02 (slice 07 close)

`./mvnw spotless:check verify` → BUILD SUCCESS, 500 tests, 0 skipped, JaCoCo met.
`scripts/simulation/verify-booking-depth.sh` → **50 passed, 0 failed, 0 skipped** against
PostgreSQL 5434 / API 8088 on `routeshare_comigo`.

Automated coverage as built: `TripFreezeTest` (freeze predicate and the seat plan),
`SeatInventoryConcurrencyIT` (twenty riders at one seat — one hold, nineteen 23505),
`RequestExpiryJobTest`, `OpenRequestLimitTest`, `OccurrenceCancellationTest`,
`ContactDisclosureAuthorizationTest` (12 cases, every negative path in 07-15 to 07-20).

Found by the run and fixed: a missing required header answered 500 rather than 400; a rolled-back
transaction left the identity projection cache pointing at an `app_user` row that no longer existed,
so a rider whose first-ever request lost the seat race could never book again; and an admin could
not act on a driver's trip, which is the support case the endpoint exists for.

Note on 07-2: the smoke script proves the race sequentially (one 200, one typed `SEATS_TAKEN`); the
twenty-way concurrent proof is `SeatInventoryConcurrencyIT`, which is where a real index can be put
under real contention.

## Evidence to collect

- `scripts/simulation/verify-booking-depth.sh` output.
- Seat-race repetition results.
- `booking.contact_disclosure_audit` extract for the run.

## Pass/fail criteria

Pass when: the seat race always produces exactly one winner; expiry voids and releases; freeze blocks
edits; cancellation windows price correctly; and **every** negative contact-disclosure case in 07-15
through 07-20 is refused.

Fail on: any double seat allocation, any leaked seat hold, or any contact disclosure outside the §6.1
rules — that last one is a privacy incident, not a bug.
