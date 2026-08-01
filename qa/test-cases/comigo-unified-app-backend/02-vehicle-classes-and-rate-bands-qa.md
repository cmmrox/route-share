# QA — Task 02: Vehicle Classes and Rate Bands

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/02-vehicle-classes-and-rate-bands.md`

## Scope

Vehicle classes with seat caps and default bands, per-vehicle admin-typed bands with factor
notes, driver rate selection, one open re-assessment request, and the `RATE_BAND_NOT_SET` publish gate.
Out of scope: using the rate to price anything (slice 03).

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V028`.


## Automated test coverage

- `VehicleClassSeatCapTest` — seat count validated against the class cap.
- `RateBandConstraintTest` — band inside class enforced by the database trigger, chosen rate inside band by CHECK.
- `RateBandLifecycleTest` — `NOT_SET → PENDING_ASSESSMENT → ACTIVE → UNDER_REVIEW`.
- `RateReviewRequestUniquenessTest` — one open request per vehicle, tested concurrently.
- `RatePositionTest` — bottom/middle/top derivation.
- `DriverGuardPublishTest` — publishing blocked without an active band.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 02-1 | Four classes seeded | CAR 3/38–62, SUV 4/46–74, VAN 6/40–68, THREE_WHEELER 2/26–42 |
| 02-2 | Add a CAR with 4 seats | 400 `SEATS_EXCEED_CLASS_CAP` |
| 02-3 | Vehicle approved | Band row created in `PENDING_ASSESSMENT` |
| 02-4 | Publish with a `PENDING_ASSESSMENT` band | 403 `RATE_BAND_NOT_SET`; appears on `/me/context` |
| 02-5 | Admin sets band 41–58 on a CAR | Accepted; status `ACTIVE`; chosen defaults to midpoint |
| 02-6 | Admin sets band 30–70 on a CAR (outside 38–62) | Rejected **by the database**, not only the service |
| 02-7 | Driver chooses 50 | Accepted |
| 02-8 | Driver chooses 65 | 400 `RATE_OUTSIDE_BAND` |
| 02-9 | Driver attempts to set their own band | 403 — the highest-value escalation in this slice |
| 02-10 | Driver reads another driver's band | 403 |
| 02-11 | Two review requests | Second refused `RATE_REVIEW_ALREADY_OPEN` |
| 02-12 | Band `UNDER_REVIEW` | Existing band stays live and priceable |
| 02-13 | Review approved with a new band | New band active; audit shows before and after |
| 02-14 | D40 payload | Class range, submitted date, review days, and the driver's other active vehicle |

## Manual checks

- Confirm the band-within-class trigger fires on both INSERT and UPDATE.
- Confirm every assessment and review decision is audited with old and new `rateMin`/`rateMax`.
- Confirm the pending-assessment gauge increments and decrements as vehicles move through.

## Evidence to collect

- `scripts/simulation/verify-rate-bands.sh` output.
- Database extract showing the trigger rejection for case 02-6.
- Audit rows for cases 02-5 and 02-13.

## Pass/fail criteria

Pass when: every class cap and band constraint is enforced at the database level; a driver cannot set
their own band; exactly one review request can be open; and publishing is blocked without an active band.

Fail on: case 02-6 or 02-9 succeeding, or a band constraint enforced only in Java.
