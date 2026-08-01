---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 02 — Vehicle Classes and Rate Bands

**Goal:** Give every vehicle a class and an admin-assessed per-km min–max band, and let the driver choose a rate inside it. A vehicle with no band cannot publish.

**Depends on:** 01.
**Blocks:** 03 (the fare engine reads the vehicle's chosen rate).

## Objective

The prototype's central pricing rule is that **a driver never types a price**. ComiGo sets a min–max
per-km band per vehicle, and the driver picks a rate inside it. Two cars on the same road are therefore
not the same price, which is why search results show a per-km rate per driver and why ride detail can
explain "why Priya's rate is LKR 46".

Today `vehicle.vehicle` has no rate columns at all, and `seat_count` is a free 1–12 integer with no class.
This task builds the whole rate-band domain, including the state where an approved vehicle still cannot
publish because no band has been set (D40) — which the prototype treats as a first-class screen precisely
so drivers do not report it as a bug.

## Scope

In scope:

- Vehicle classes with default bands and seat caps.
- Per-vehicle band: `rate_min`, `rate_max`, `rate_chosen`, status, who set it and when.
- The four assessment factors stored as **displayed justification** (decision D2) — signed deltas with labels and detail text, typed by the admin, not computed.
- Admin band assessment endpoints.
- Driver: read the band, choose a rate inside it, request one re-assessment (3 working-day SLA).
- The `RATE_BAND_NOT_SET` publishing gate wired into slice 01's `canPublish`.

Out of scope:

- Using the rate to price anything — that is slice 03. This slice stores and governs the number.
- Any scoring engine that computes the band from vehicle attributes. D2 explicitly chose admin-typed.

## Source material / references

- `docs/source-assets/comigo-prototype/driver-rates.jsx` — D39 (band + factors + what the rate earns + positions), D40 (band being set).
- `docs/source-assets/comigo-prototype/data.jsx` — `VEHICLE_CLASSES`, `RATE_BAND`, `PENDING_VEHICLE`, `RATE_REVIEW`, `RATE_POSITIONS`, `fareAtRate`.
- `docs/source-assets/comigo-prototype/driver-become.jsx` — D06 vehicles, D07 add vehicle (seat cap by class).
- `docs/source-assets/comigo-prototype/passenger-discover.jsx` — P07 "why this driver's rate is X" band slider.
- Current code: `vehicle/**`, `admin/controller/AdminVehicleReviewController.java`.

## Architecture and design notes

**Class defaults, per-vehicle override.** The class supplies a starting band and a hard seat cap; the
admin's assessment produces the vehicle's actual band, which must sit inside the class band. Enforced as
a DB constraint, not just service validation — a band outside its class is a pricing incident.

| Class | Max passenger seats | Default band (LKR/km) |
| --- | --- | --- |
| `CAR` | 3 | 38–62 |
| `SUV` | 4 | 46–74 |
| `VAN` | 6 | 40–68 |
| `THREE_WHEELER` | 2 | 26–42 |

**Band status is its own lifecycle**, separate from vehicle approval:
`NOT_SET → PENDING_ASSESSMENT → ACTIVE`, with `UNDER_REVIEW` when a re-assessment is requested (the
current band stays live throughout — D39 says so explicitly).

**Approved papers ≠ publishable.** D40 exists because a driver whose registration and insurance are
approved still cannot publish without a band: there is no legal price to put on a seat. `canPublish` must
therefore check the band of the vehicle being used, not just vehicle approval.

**Factors are notes with numbers.** Per D2 the admin types `rate_min` and `rate_max` directly; the four
factor rows are stored so D39 can show the arithmetic that justified it. The service asserts the factor
deltas sum consistently with the offset from the class default and **warns** rather than rejects — the
displayed explanation must not be able to block an operational price change.

**Rate positions are informational.** `RATE_POSITIONS` (bottom/middle/top → ranking and demand text) is
static copy derived from the chosen rate's position in the band, computed server-side so both P07 and D39
tell the same story.

## API contracts involved

Driver:

```
GET   /api/v1/driver/vehicles/{vehicleId}/rate-band
PUT   /api/v1/driver/vehicles/{vehicleId}/rate-band/chosen-rate     { ratePerKm }
POST  /api/v1/driver/vehicles/{vehicleId}/rate-band/review-requests { reason, note }
GET   /api/v1/driver/vehicles/{vehicleId}/rate-band/review-requests
GET   /api/v1/driver/vehicle-classes
```

`RateBandResponse`: `vehicleId`, `vehicleLabel`, `classKey`, `classLabel`, `classBand{min,max}`,
`band{min,max}`, `chosenRate`, `status`, `setBy`, `setAt`, `factors[]{key,label,detail,delta}`,
`netEffect`, `position{key,label,rank,demand}`, `reviewRequest{status,requestedAt,slaDays}` | null.

Admin:

```
GET   /api/v1/admin/vehicles/{vehicleId}/rate-band
PUT   /api/v1/admin/vehicles/{vehicleId}/rate-band   { rateMin, rateMax, factors[], note }
GET   /api/v1/admin/rate-band-review-requests?status=OPEN
POST  /api/v1/admin/rate-band-review-requests/{id}/decide { decision, rateMin?, rateMax?, factors[], note }
```

Errors: `RATE_BAND_NOT_SET`, `RATE_OUTSIDE_BAND`, `BAND_OUTSIDE_CLASS`, `RATE_REVIEW_ALREADY_OPEN`,
`SEATS_EXCEED_CLASS_CAP`.

Changed: `POST/PUT /api/v1/driver/vehicles` now requires `vehicleClass` and validates `seatCount` against
the class cap. `GET /api/v1/driver/vehicles` returns band summary per vehicle (D06 shows band state).

## Database / migration changes

**`V028__vehicle_classes_and_rate_bands.sql`**

- New `vehicle.vehicle_class` (reference table, seeded):
  `class_key PK`, `label`, `max_passenger_seats`, `default_rate_min NUMERIC(6,2)`, `default_rate_max NUMERIC(6,2)`, `active BOOLEAN`.
  Seed the four rows above.
- `vehicle.vehicle` — add `class_key TEXT REFERENCES vehicle.vehicle_class(class_key)`.
  Backfill existing rows to `CAR` (D6: dev data only). Then `SET NOT NULL`.
  Add `CHECK (seat_count BETWEEN 1 AND 12)` replaced by a trigger/constraint asserting
  `seat_count <= max_passenger_seats` of its class.
- New `vehicle.vehicle_rate_band`:
  `vehicle_rate_band_id`, `vehicle_id FK UNIQUE`, `rate_min NUMERIC(6,2) NOT NULL`,
  `rate_max NUMERIC(6,2) NOT NULL`, `chosen_rate NUMERIC(6,2)`,
  `status TEXT CHECK (status IN ('NOT_SET','PENDING_ASSESSMENT','ACTIVE','UNDER_REVIEW'))`,
  `set_by_app_user_id`, `set_at`, `created_at`, `updated_at`,
  `CHECK (rate_min > 0 AND rate_max >= rate_min)`,
  `CHECK (chosen_rate IS NULL OR (chosen_rate >= rate_min AND chosen_rate <= rate_max))`.
- New `vehicle.vehicle_rate_band_factor`:
  `id`, `vehicle_rate_band_id FK`, `factor_key TEXT CHECK (factor_key IN ('AGE','INSURANCE','FUEL','SERVICE'))`,
  `label TEXT`, `detail TEXT`, `delta NUMERIC(6,2) NOT NULL`, `sort_order INT`,
  `UNIQUE (vehicle_rate_band_id, factor_key)`.
- New `vehicle.rate_band_review_request`:
  `id`, `vehicle_id FK`, `requested_by_app_user_id`, `reason TEXT`, `note TEXT`,
  `status TEXT CHECK (status IN ('OPEN','APPROVED','REJECTED'))`, `requested_at`, `decided_at`,
  `decided_by_app_user_id`, `decision_note`,
  partial unique index on `vehicle_id WHERE status = 'OPEN'` — one open request per vehicle (D39's "one re-assessment").
- Constraint enforcing band-inside-class: a `CHECK` cannot reference another table, so use a `BEFORE INSERT OR UPDATE` trigger `vehicle_rate_band_within_class()` raising on violation.

## Configuration / environment changes

- `ROUTESHARE_RATE_BAND_REVIEW_SLA_DAYS` (default `3`) — surfaced in D39's copy. Registered in the
  policy-setting surface introduced in slice 03 rather than as a raw env var if that lands first; until
  then an application property with the same key.
- No secrets.

## UI / UX requirements

Backend slice. The contract must supply, without client-side arithmetic:

- D39 — chosen rate, floor, ceiling, the four signed factors with detail text, net effect, what the rate earns per seat on a named route (gross / commission / net), and the three rate positions with their ranking and demand text.
- D39b/D39c — the same payload with `chosenRate` at `min` or `max`.
- D40 — band `PENDING_ASSESSMENT`, submitted date, expected review days, the class range for context, and a pointer to the driver's other publishable vehicle.
- D06/D07 — class-driven seat options and per-vehicle band state.
- P07 — the class band, the driver's rate, and its position, so the passenger slider renders honestly.

## Implementation steps

1. Add `VehicleClass` reference entity + repository + seed migration; expose `GET /driver/vehicle-classes`.
2. Add `class_key` to `VehicleEntity`; validate `seatCount <= class.maxPassengerSeats` on create/update; add `SEATS_EXCEED_CLASS_CAP`.
3. Add `VehicleRateBandEntity`, `VehicleRateBandFactorEntity`, `RateBandReviewRequestEntity` + repositories.
4. `RateBandService(+Impl)` in `vehicle`: `bandFor(vehicleId)`, `assess(vehicleId, cmd)`, `chooseRate(vehicleId, rate)`, `requestReview(vehicleId, cmd)`, `decideReview(id, cmd)`.
5. Position derivation: bottom third / middle / top third of the band → `RATE_POSITIONS` copy, computed in `vehicle/domain/RatePosition`.
6. On vehicle approval, create the band row in `PENDING_ASSESSMENT` so D40 has something to show.
7. Admin assessment sets `rate_min`/`rate_max`/factors, moves to `ACTIVE`, defaults `chosen_rate` to the midpoint, audits the change with before/after values.
8. Re-assessment: `UNDER_REVIEW` keeps the existing band live; a decision either writes a new band or rejects with a note.
9. `VehicleFacade.ratePerKmFor(vehicleId)` and `VehicleFacade.hasActiveBand(vehicleId)` — the only things slice 03 and the publish gate consume.
10. Wire `RATE_BAND_NOT_SET` into `DriverGuard.canPublish` from slice 01.
11. Notification on band set / review decided, through `NotificationFacade` (D40's "Notify me when it's set").

## Files expected to change

- `apps/api/.../vehicle/**` — entities, repositories, `RateBandService`, controllers, `VehicleFacade` additions, `domain/RatePosition`.
- `apps/api/.../admin/**` — admin band + review endpoints, audit.
- `apps/api/.../common/security/DriverGuard` — publish gate now checks the band.
- `apps/api/src/main/resources/db/migration/V028__vehicle_classes_and_rate_bands.sql`.
- `apps/api/src/test/java/**` — band constraint tests, class-cap tests, review-request uniqueness, position derivation.
- `docs/api/mobile-app.openapi.json`, `docs/api/admin-web.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/02-vehicle-classes-and-rate-bands-qa.md`

Maestro: not applicable — no mobile surface in this slice. Runtime smoke required (below).

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='RateBand*Test,VehicleClass*Test,DriverGuardPublishTest' test
```

```bash
bash scripts/simulation/verify-rate-bands.sh
```

The smoke script must prove: a newly approved vehicle is `PENDING_ASSESSMENT` and blocks publishing; an
admin band makes it `ACTIVE` and unblocks; a rate outside the band is rejected; a band outside its class
is rejected by the database, not only the service; a second open review request is refused.

## Security, privacy, and observability checks

- Only `ADMIN`/`SUPER_ADMIN`/`FINANCE_ADMIN` may set a band. A driver setting their own band is the single most valuable privilege escalation in this system — test it explicitly.
- Ownership: a driver may only read/choose rates on their own vehicles.
- Every band assessment and review decision audited with before/after `rateMin`, `rateMax`, actor and note.
- Rate changes are price changes: log at INFO with vehicle id, old and new chosen rate, and actor.
- Metrics: `routeshare_rate_band_assessments_total`, `routeshare_rate_band_review_requests_total{status}`, and a gauge of vehicles stuck in `PENDING_ASSESSMENT` — a growing gauge is drivers silently unable to earn.

## Done criteria

- [ ] Four classes seeded with caps and default bands; seat count validated against the cap.
- [ ] Band lifecycle `NOT_SET → PENDING_ASSESSMENT → ACTIVE → UNDER_REVIEW` implemented with DB constraints.
- [ ] Band-inside-class enforced at database level.
- [ ] Driver can read the band, choose a rate inside it, and request exactly one open re-assessment.
- [ ] Admin can assess, and decide review requests, with full audit.
- [ ] `RATE_BAND_NOT_SET` blocks publishing and appears in `/me/context`.
- [ ] D39, D39b, D39c, D40, D06, D07 and P07 payload fields are all supplied by the contract.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): add vehicle classes and admin-assessed per-km rate bands"
```
