---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 04 — Charge Timing and Capture Correctness

**Goal:** Make the product's central promise true — the card is authorised at booking and captured when the driver starts the trip, never before, and released without a charge if the trip never happens.

**Depends on:** 03.
**Blocks:** 05, 06, 07, 11, 12, 13, 14.

## Objective

`POLICY.chargeAt = "TRIP_START"` is stated on eleven screens, in the strongest possible terms: "Accepting
does not charge you", "Your Visa is authorised, not charged", "Decline, cancel or a no-start all cost you
nothing", "Starting charges 2 cards".

The backend does not do this. There is **no `capture(...)` call anywhere in `trip/service/impl`** — the
only capture paths are a manual admin endpoint and `finalizeBookingFare`. So today a trip can start and
complete without any money moving, and a cancelled booking can leave an authorisation hanging.

This slice wires the money to the trip lifecycle and makes every path idempotent, because a retried
"start trip" tap must not double-charge three passengers.

## Scope

In scope:

- Authorise at booking creation (card) / no-op (cash).
- Capture **all** confirmed bookings on `trip.start`, atomically with the state transition.
- Void authorisations on: passenger cancel before start, driver cancel, decline, auto-cancel, expiry.
- Cash bookings: no authorisation, commission recorded as owed and netted from payout.
- Partial capture on early drop-off, using slice 03's `repriceForActualDistance`.
- Idempotency and reconciliation for every money movement.
- The `AUTHORIZED` payment state, which the current lifecycle lacks.

Out of scope:

- Capture-on-accept for en-route bookings — slice 13, which reuses the capture path built here.
- Penalty charges — slice 06.
- Applying dues and rewards credit at checkout — slices 06 and 11.
- The start-buffer auto-cancel *trigger* — slice 05. This slice provides the void path it calls.

## Source material / references

- `docs/source-assets/comigo-prototype/data.jsx` — `POLICY.chargeAt`, `chargeAtWhenEnRoute`.
- `docs/source-assets/comigo-prototype/passenger-book.jsx` — P09 banner copy, P11 "Nothing charged — and not on acceptance either", P12 "charged at 6:15 PM", P22 "never charged", P24 "the authorisation is released".
- `docs/source-assets/comigo-prototype/driver-supply.jsx` — D15 "Starting the trip charges every booked card", D13 footer.
- `docs/source-assets/comigo-prototype/driver-live.jsx` — D17 "Starting charges 3 cards", D23 cash collection, D22b early drop.
- Current code: `payment/service/impl/PaymentServiceImpl.java`, `payment/gateway/**`, `trip/service/impl/**`, `booking/service/impl/**`, `common/entity/IdempotencyKeyEntity.java`.

## Architecture and design notes

**The state machine gains a state.** `payment_intent.status` today is
`REQUIRES_CAPTURE | CAPTURED | VOIDED | REFUNDED | FAILED`. `REQUIRES_CAPTURE` conflates "not yet
authorised" with "authorised, awaiting capture". Add `AUTHORIZED`, so:

```
PENDING → AUTHORIZED → CAPTURED → REFUNDED
   ↓          ↓
 FAILED    VOIDED
```

**Capture is driven by the trip, not the booking.** One `POST /driver/trips/{id}/start` captures N cards.
It must be one transaction over the trip transition plus N capture attempts, with per-booking outcomes
recorded — and a partial failure must not roll back the trip. A driver whose passenger's bank declines
cannot be prevented from driving.

So: the trip transition and the *intent to capture* commit together; the gateway calls happen through the
existing transactional outbox, with a `payment.capture.requested` event per booking. A failed capture
marks that booking `PAYMENT_FAILED` and raises an operations notification; it does not stop the trip.

**Idempotency is the whole slice.** Every capture, void and refund goes through
`common.idempotency_key` with a deterministic key (`capture:booking:{id}:attempt:{n}`), and the gateway
reference is stored before the call so a timeout can be reconciled rather than retried blindly.

**Reconciliation job.** Any `AUTHORIZED` intent on a trip that has already started, or any
`REQUIRES_CAPTURE`/`AUTHORIZED` intent older than the authorisation validity window, is surfaced to
operations. Silent stuck authorisations are how riders get their money held for a week.

**Cash never authorises.** A cash booking creates no intent. Commission on a cash fare is recorded as a
`PLATFORM_COMMISSION` ledger row owed by the driver and netted from the next payout — D23 and D27 both
state this.

## API contracts involved

Changed:

```
POST /api/v1/passenger/bookings              -> authorises (card) and returns paymentStatus
POST /api/v1/driver/trips/{tripId}/start     -> captures every confirmed booking; returns per-booking outcomes
POST /api/v1/passenger/bookings/{id}/cancel  -> voids if not yet captured
POST /api/v1/driver/bookings/{id}/decline    -> voids
POST /api/v1/driver/routes/{routeId}/cancel  -> voids every booking on the occurrence
POST /api/v1/passenger/bookings/{id}/early-drop-off -> captures the repriced amount
POST /api/v1/driver/bookings/{id}/cash-collected     -> records commission owed
```

`TripStartResponse` adds `captures[]{bookingId, status: CAPTURED|FAILED|SKIPPED_CASH, amount, failureCode?}`.

`PassengerBookingDetailResponse` adds `payment{method, status, authorizedAt, capturedAt, amount, last4}` —
P11/P12/P22/P24 all render this.

New admin: `GET /api/v1/admin/payments/reconciliation` — stuck authorisations and failed captures.

New errors: `PAYMENT_AUTHORIZATION_FAILED`, `PAYMENT_ALREADY_CAPTURED`, `CAPTURE_NOT_PERMITTED_BEFORE_START`.

## Database / migration changes

**`V030__charge_timing_and_capture.sql`**

- `payment.payment_intent` — extend the status CHECK to include `PENDING` and `AUTHORIZED`; add
  `authorized_at`, `captured_at`, `voided_at`, `failure_code TEXT`, `failure_message TEXT`,
  `payment_method_id FK NULL`, `attempt_count INT NOT NULL DEFAULT 0`.
- `booking.booking` — add `payment_method TEXT CHECK (payment_method IN ('CARD','CASH'))`,
  `payment_status TEXT`, `captured_at`.
- New `payment.payment_attempt`:
  `id`, `payment_intent_id FK`, `operation TEXT CHECK (operation IN ('AUTHORIZE','CAPTURE','VOID','REFUND'))`,
  `idempotency_key TEXT NOT NULL`, `provider_reference TEXT`, `amount NUMERIC(12,2)`,
  `status TEXT CHECK (status IN ('STARTED','SUCCEEDED','FAILED'))`, `failure_code`, `started_at`,
  `finished_at`, `UNIQUE (idempotency_key)`.
  Written **before** the gateway call, so a timeout leaves a reconcilable record.
- `payment.fare_ledger_entry` — extend `entry_type` CHECK with `COMMISSION_OWED_CASH`.
- Index `idx_payment_intent_stuck ON payment.payment_intent(status, created_at) WHERE status IN ('PENDING','AUTHORIZED')`.

## Configuration / environment changes

- `ROUTESHARE_PAYMENT_AUTH_VALIDITY_HOURS` (default `168`) — how long an authorisation is assumed good; drives the reconciliation alert.
- `ROUTESHARE_PAYMENT_CAPTURE_MAX_ATTEMPTS` (default `3`).
- Existing `CYBERSOURCE_ENABLED` unchanged; with it off, the cash-only fallback path must still exercise the full state machine so tests are meaningful.

## UI / UX requirements

Backend slice. The contract must let the app state, truthfully and specifically:

- P09 — "Your card is charged when he starts the trip at 6:15 PM."
- P11 — authorised, not charged; nothing moves on acceptance.
- P12 — the exact capture time.
- P22 / P24 — never charged, authorisation released, LKR 0.
- P17 — charged at, and the refunded amount on an early drop.
- D15 / D17 — how many cards a start will capture, and for how much.
- D23 — the cash amount to ask for and what the driver keeps after the fee.

## Implementation steps

1. Extend the payment intent state machine and entity; add `PENDING`/`AUTHORIZED` with guarded transitions.
2. Add `payment.payment_attempt` and route every gateway call through an `IdempotentGatewayCall` helper that writes the attempt row first.
3. On booking creation: card → `authorize()` for `passengerPays` from the persisted quote; cash → no intent, `payment_method = CASH`.
4. Add `PaymentFacade.captureForTripStart(tripId)` returning per-booking outcomes; publish `payment.capture.requested` per booking through the outbox.
5. Wire `TripServiceImpl.transition(SCHEDULED → STARTED)` to call it inside the same transaction as the state change.
6. Add `PaymentFacade.voidForBooking(bookingId, reason)`; wire it into passenger cancel (pre-start), driver decline, driver/route cancel, and expose it for slice 05's auto-cancel.
7. Early drop-off: call slice 03's `repriceForActualDistance`, then capture the lower amount; if already captured, refund the difference.
8. Cash: on `cash-collected`, write `COMMISSION_OWED_CASH` for the commission portion.
9. Add the reconciliation query + admin endpoint + a scheduled check (leader-elected; the scheduler lands properly in slice 05 — here use a single `@Scheduled` guarded by a config flag defaulted off in multi-instance until 05).
10. Notification on capture, capture failure and void, through `NotificationFacade`.
11. Contract + `packages/api-contracts` regeneration.

## Files expected to change

- `apps/api/.../payment/**` — state machine, attempts, facade methods, reconciliation, receipt payment block.
- `apps/api/.../trip/service/impl/TripServiceImpl.java` — capture on start.
- `apps/api/.../booking/service/impl/**` — authorise on create, void on cancel/decline.
- `apps/api/.../routing/service/impl/**` — void on route cancel.
- `apps/api/.../admin/**` — reconciliation endpoint.
- `apps/api/src/main/resources/db/migration/V030__charge_timing_and_capture.sql`.
- `apps/api/src/test/java/**` — state machine tests, idempotency tests, partial-failure tests, Testcontainers integration for start-captures-N.
- `docs/api/mobile-app.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/04-charge-timing-and-capture-correctness-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='PaymentIntentStateMachineTest,CaptureOnTripStartIT,PaymentIdempotencyTest,VoidOnCancelTest,CashCommissionTest' test
```

```bash
bash scripts/simulation/verify-charge-timing.sh
```

The smoke script must prove, against the live stack: booking authorises but does not capture; driver
approval does not capture; trip start captures every card exactly once; a repeated start request captures
nothing further; cancelling before start voids and charges zero; a declined capture leaves the trip
running and flags the booking.

## Security, privacy, and observability checks

- Double-charge is the top risk. Test: concurrent duplicate `start` calls, a retried start after timeout, and a start on an already-started trip must all result in exactly one capture per booking.
- Never log PAN, CVV or full tokens. Attempt rows store the provider reference only.
- A capture must be impossible before `trip.status = STARTED` — assert with a direct service-level test, not only through the controller.
- Passenger-facing responses must never expose the gateway reference or failure detail beyond a safe code.
- Metrics: `routeshare_payment_captures_total{result}`, `routeshare_payment_voids_total{reason}`, `routeshare_payment_capture_latency_seconds`, gauge of stuck authorisations. Alert on stuck > 0 for more than an hour.
- Every capture, void and refund audited with actor, booking, amount and idempotency key.

## Done criteria

- [ ] Card authorised at booking, captured at trip start, never earlier.
- [ ] En-route capture path exists and is unused (slice 13 turns it on).
- [ ] Voids fire on passenger cancel, decline, driver cancel, route cancel and auto-cancel.
- [ ] Early drop-off captures the repriced amount, or refunds the difference if already captured.
- [ ] Cash bookings create no intent and record commission owed.
- [ ] Every money movement is idempotent and reconcilable after a gateway timeout.
- [ ] A failed capture flags the booking without stopping the trip.
- [ ] Reconciliation surfaces stuck authorisations to operations.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): authorise on booking and capture on trip start, with idempotent money movement"
```
