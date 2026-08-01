---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 07 — Booking Depth: Seats, Approval Modes and Expiry

**Goal:** Turn a seat count into a real booking — a named seat, an approval mode chosen per trip, a request that expires, a trip that freezes once someone books, and cancellation windows with consequences.

**Depends on:** 03, 05.
**Blocks:** 10, 13.

## Objective

`booking.seats` is an integer. The prototype books a *seat* — front or back — because that is the only
distinction that changes the ride (P08). A trip is either instant-book or approve-each-request (D13), a
request expires after 30 minutes (D16), a published trip freezes the moment its first seat sells (D09,
D15), and cancelling inside 12 hours costs the driver money and reliability (D30, D31).

None of that exists. This slice makes a booking a first-class object with a lifecycle the screens can
actually render, and adds counterparty phone disclosure per plan §6.1 now that direct dial replaced the
relay.

## Scope

In scope:

- Named seat inventory per route occurrence; seat selection and assignment.
- Per-trip approval mode (`INSTANT` / `APPROVE_EACH`), defaulted from driving preferences (slice 08).
- 30-minute request expiry with an `EXPIRED` terminal state.
- Trip freeze on first booking; edit permitted until then.
- Typed seat-race conflict.
- Two-open-requests-at-once rule.
- Driver cancellation windows (free > 12 h, penalised inside) with reason codes.
- Alternatives list on decline / cancel / auto-cancel.
- Counterparty phone disclosure on confirmed bookings, per plan §6.1.

Out of scope:

- Live/en-route requests and their 45-second expiry — slice 13.
- Seat resale after early drop-off — slice 13, which owns mid-trip inventory.
- Chat opening on confirmation — slice 10 (this slice emits `booking.confirmed`).

## Source material / references

- `docs/source-assets/comigo-prototype/passenger-book.jsx` — P08 seat select, P11–P14, P22, P24.
- `docs/source-assets/comigo-prototype/data.jsx` — `seatSlots`, `POLICY.editLocksOnFirstBooking`, `driverCancelFreeHours`, `MY_TRIP.alternatives`.
- `docs/source-assets/comigo-prototype/driver-supply.jsx` — D09 freeze banner, D13 who-can-book, D15 trip detail, D16 requests + expiry, D16e lapsed, D30/D30b/D31/D31b cancellation.
- `docs/source-assets/comigo-prototype/primitives.jsx` — `SeatPlan` (front seat beside the driver, rear row).
- Plan §6.1 — counterparty phone disclosure rules.

## Architecture and design notes

**Seats are named, not drawn.** `seatSlots(capacity)` produces slot 1 = "Front seat · Beside the driver"
and the rest "Back seat · Rear row". Inventory is per `route_occurrence`, one row per slot, so a booking
holds specific slots and the race is resolved by a unique constraint rather than a counter decrement.

**Every seat is the same price.** P08 says so explicitly. Seat choice must never touch the fare — asserted
by test, because a seat-priced fare is a support nightmare and contradicts the screen.

**Approval mode lives on the occurrence, defaulted from the account.** D35 sets the account default; D13
lets the driver override per trip. Instant-book confirms immediately; approve-each creates a `REQUESTED`
booking with `expires_at = now + 30 min`.

**Freeze is a computed rule, not a flag to maintain.** A trip is editable while
`bookedSeats == 0 AND status = PUBLISHED`. Storing a boolean invites drift when a booking is cancelled;
D09 shows a still-editable trip precisely because nobody has booked yet.

**Cancellation window is priced by slice 06.** This slice decides *which* window applies and collects the
reason; the penalty amount comes from `PenaltyFacade`. The two must not both know the percentage.

**Phone disclosure (plan §6.1).** Because calls are direct dial, `GET /bookings/{id}/contact` returns the
counterparty's number only when the booking is `CONFIRMED`, reciprocally, revoked 24 h after drop-off and
immediately on any terminal state. Every read is audited. This is deliberately one service method so a
relay could replace it later without touching callers.

## API contracts involved

Passenger:

```
GET  /api/v1/passenger/route-occurrences/{id}/seats      -> slots, taken/free, capacity, class
POST /api/v1/passenger/bookings                           -> now takes seatSlotIds[]; 409 SEATS_TAKEN
GET  /api/v1/passenger/bookings/{id}/alternatives         -> P13/P22/P24 list
GET  /api/v1/passenger/bookings/{id}/contact              -> §6.1 disclosure
```

Driver:

```
PUT  /api/v1/driver/route-occurrences/{id}/approval-mode  { mode }
GET  /api/v1/driver/trips/{tripId}/booking-requests       -> now includes expiresAt, secondsRemaining
POST /api/v1/driver/routes/{routeId}/occurrences/{id}/cancel { reasonCode, note }
GET  /api/v1/driver/routes/{routeId}/occurrences/{id}/cancellation-terms -> window, penalty, affected riders
GET  /api/v1/driver/bookings/{id}/contact                 -> §6.1 disclosure
```

Cancellation reason codes (D30): `VEHICLE_PROBLEM`, `UNWELL`, `PLANS_CHANGED`, `WRONG_DETAILS`, `OTHER`.

New errors: `SEATS_TAKEN`, `SEAT_ALREADY_HELD`, `REQUEST_EXPIRED`, `TRIP_FROZEN`,
`TOO_MANY_OPEN_REQUESTS`, `CONTACT_NOT_AVAILABLE`.

`BookingDetailResponse` adds `seats[]{slotId, label, sub}`, `approvalMode`, `expiresAt`,
`secondsRemaining`, `contactAvailable`.

## Database / migration changes

**`V033__booking_depth.sql`**

- New `routing.route_occurrence_seat`:
  `id`, `route_occurrence_id FK`, `slot_index INT`, `label TEXT`, `sub_label TEXT`,
  `UNIQUE (route_occurrence_id, slot_index)`. Generated on occurrence creation from the vehicle's class cap.
- New `booking.booking_seat`:
  `id`, `booking_id FK`, `route_occurrence_seat_id FK`,
  `UNIQUE (route_occurrence_seat_id) WHERE released_at IS NULL` — the race guard,
  `held_at`, `released_at`.
- `routing.route_occurrence` — add `approval_mode TEXT NOT NULL DEFAULT 'APPROVE_EACH' CHECK (approval_mode IN ('INSTANT','APPROVE_EACH'))`.
- `booking.booking` — add `expires_at TIMESTAMPTZ NULL`, `expired_at TIMESTAMPTZ NULL`.
- `booking.booking` — extend the status CHECK with `EXPIRED`; same for `booking_status_history.to_status`.
- New `routing.route_occurrence_cancellation`:
  `id`, `route_occurrence_id FK UNIQUE`, `cancelled_by_app_user_id`, `reason_code TEXT`, `note TEXT`,
  `hours_before_departure NUMERIC(6,2)`, `within_free_window BOOLEAN`, `penalty_id FK NULL`, `cancelled_at`.
- New `booking.contact_disclosure_audit`:
  `id`, `booking_id FK`, `reader_app_user_id`, `subject_app_user_id`, `read_at`.
- Index `idx_booking_expiring ON booking.booking(expires_at) WHERE status = 'REQUESTED'`.

## Configuration / environment changes

- Policy settings: `SCHEDULED_REQUEST_EXPIRY_MINUTES` (30), `DRIVER_CANCEL_FREE_HOURS` (12), `MAX_OPEN_PASSENGER_REQUESTS` (2), `CONTACT_DISCLOSURE_HOURS_AFTER_DROPOFF` (24).
- New scheduler job registered against slice 05's infrastructure: `scheduled-request-expiry`, 1-minute tick.

## UI / UX requirements

Backend slice. The contract must supply:

- P08 — capacity, which slots are taken, labels, and the same price for every seat.
- P11 — seconds remaining on the request and that two may be open at once.
- P13 / P22 / P24 — the alternatives list with driver, departure, match and price.
- P14 — a typed `SEATS_TAKEN` with the closest alternative attached.
- D09 / D15 — whether the trip is still editable, and the request count with time remaining.
- D16 — per-request expiry countdown; D16e — the lapsed reason and who took the seat.
- D30 / D30b — hours to departure, which window applies, the priced penalty, the affected riders by first name, and the reason options.

## Implementation steps

1. Generate `route_occurrence_seat` rows on occurrence creation from the vehicle class cap; expose the seat map.
2. Add `booking_seat` with the partial unique index; booking creation holds slots in the same transaction as the booking insert.
3. Translate the unique-violation into a typed `SEATS_TAKEN` 409 carrying the closest alternative (P14).
4. Assert by test that fare is independent of slot choice.
5. Add `approval_mode` on the occurrence, defaulted from driving preferences (stub to `APPROVE_EACH` until slice 08); instant-book confirms and authorises immediately, approve-each sets `expires_at`.
6. Register the `scheduled-request-expiry` job: expire, void the authorisation via slice 04, release held seats, notify both sides, emit `booking.expired`.
7. Add the two-open-requests rule as a service-level guard with `TOO_MANY_OPEN_REQUESTS`.
8. Implement the freeze rule as a computed predicate; block occurrence edits with `TRIP_FROZEN`.
9. Implement occurrence cancellation: compute hours to departure, pick the window, collect the reason, call `PenaltyFacade` for the priced penalty when inside 12 h, void every booking, notify every rider with alternatives.
10. Implement `cancellation-terms` for the driver, mirroring the passenger version from slice 05.
11. Implement the alternatives query — other occurrences on the same corridor, near the same time, ranked by match then price; reuse the search path from `routing`.
12. Implement `GET /bookings/{id}/contact` per §6.1 with the audit row, the reciprocity check, the confirmed-only rule, and the 24-hour revocation.

## Files expected to change

- `apps/api/.../routing/**` — seat generation, approval mode, occurrence cancellation, alternatives query.
- `apps/api/.../booking/**` — seat holds, expiry, open-request guard, contact disclosure + audit.
- `apps/api/.../scheduling/**` — the expiry job.
- `apps/api/.../penalty/**` — consumed for cancellation pricing (no change expected).
- `apps/api/src/main/resources/db/migration/V033__booking_depth.sql`.
- `apps/api/src/test/java/**` — concurrent seat-race test, expiry job test, freeze predicate tests, cancellation window tests, contact disclosure authorization tests.
- `docs/api/mobile-app.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/07-booking-depth-seats-approval-and-expiry-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='SeatInventoryConcurrencyIT,RequestExpiryJobIT,TripFreezeTest,OccurrenceCancellationTest,ContactDisclosureAuthorizationTest,OpenRequestLimitTest' test
```

```bash
bash scripts/simulation/verify-booking-depth.sh
```

The smoke must prove: two concurrent bookings for the last seat produce exactly one success and one
typed `SEATS_TAKEN`; a request expires at 30 minutes and voids; an occurrence with one booking rejects
edits; cancelling at 3 h assesses a penalty and at 26 h does not.

## Security, privacy, and observability checks

- **Phone disclosure is the highest-risk item in this slice.** Test every negative path: pending request, declined booking, cancelled booking, 25 hours after drop-off, a driver on a different trip, and a passenger requesting another passenger's number. All must return `CONTACT_NOT_AVAILABLE`.
- Disclosure reads are audited with reader, subject and booking. Confirm the audit row is written even when the response is served from cache.
- Seat holds must be released on every terminal path; a leaked hold silently removes inventory. Add a reconciliation query and alert on held seats with a terminal booking.
- Cancellation reason text is free-form; treat as untrusted, length-limited, and never rendered as HTML in admin surfaces.
- Metrics: `routeshare_seat_race_conflicts_total`, `routeshare_requests_expired_total`, `routeshare_occurrence_cancellations_total{window}`, `routeshare_contact_disclosures_total`.
- Alert on contact disclosures per user exceeding a daily threshold — number harvesting is the abuse this design invites.

## Done criteria

- [ ] Named seat inventory generated per occurrence; bookings hold specific slots.
- [ ] Seat race resolves to exactly one winner with a typed 409 carrying an alternative.
- [ ] Seat choice provably does not affect fare.
- [ ] Approval mode per occurrence; instant confirms, approve-each expires at 30 minutes.
- [ ] Expiry voids the authorisation and releases seats.
- [ ] Two-open-requests rule enforced.
- [ ] Freeze on first booking blocks edits and is computed, not stored.
- [ ] Cancellation windows, reason codes, priced penalties and rider notification all work.
- [ ] Alternatives returned for decline, cancel and auto-cancel.
- [ ] Contact disclosure obeys every rule in plan §6.1 and is audited.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): add named seats, approval modes, request expiry and cancellation windows"
```
