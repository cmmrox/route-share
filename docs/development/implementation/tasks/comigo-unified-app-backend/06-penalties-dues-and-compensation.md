---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 06 — Penalties, Dues and Compensation

**Goal:** Price every broken promise, split each penalty 50/50 between the person let down and the platform, and carry a cash passenger's unpaid fees to their next booking.

**Depends on:** 04, 05.
**Blocks:** 07, 11, 14.

## Objective

`POLICY.penaltyRecipient = "SPLIT"` is the rule that makes half the product's screens coherent. A penalty
is never just a fee: it produces a **negative** line for the person who caused it and a **positive** line
for the person it cost. D26 makes this explicit — a driver's ledger contains "No-show compensation · your
50% share" as income of a different kind from trip earnings.

There is no penalty concept anywhere in the codebase. Slice 05 fires the events; this slice prices them,
splits them, moves the money, and handles the one case where money cannot be taken — a cash passenger with
no card, whose fee rides along to the next booking (P25, P09d).

## Scope

In scope:

- Penalty assessment for all five kinds, priced as a percentage of the fare.
- The 50/50 split, with the victim's half rounded and the platform taking the remainder.
- Collection paths: net from an existing capture, charge a card, or record as dues.
- Passenger dues ledger, applied at the next checkout before payment.
- Compensation credit to the victim, landing in the shared rewards balance (slice 11 owns the balance; this slice writes into it through a facade and a stub until then).
- Penalty disputes with an admin decision.
- Driver penalty deduction from the next completed trip's earnings, never billed separately.

Out of scope:

- The rewards balance itself and its withdrawal path — slice 11.
- Payout netting mechanics — slice 14.
- Fare-adjustment disputes (a different object) — slice 14.

## Source material / references

- `docs/source-assets/comigo-prototype/data.jsx` — `POLICY.paxCancelAfterStartPct/noShowPenaltyPct/driverLatePenaltyPct/lateCancelPenaltyPct/penaltyVictimPct/penaltyPlatformPct`, `noShowPenalty`, `paxCancelPenalty`, `driverLatePenalty`, `victimShare`, `platformShare`, `PAX_DUES`, `duesTotal`.
- `docs/source-assets/comigo-prototype/passenger-penalties.jsx` — P25 dues, P26 cancel-after-start, P27 no-show.
- `docs/source-assets/comigo-prototype/passenger-book.jsx` — P22 driver cancelled (victim credit), P09d dues at checkout.
- `docs/source-assets/comigo-prototype/driver-live.jsx` — D21 release a no-show, framing and the driver's 50% share.
- `docs/source-assets/comigo-prototype/driver-supply.jsx` — D30/D31 driver cancellation penalty, split to riders.
- `docs/source-assets/comigo-prototype/driver-late.jsx` — D41 driver was late.
- `docs/source-assets/comigo-prototype/driver-money.jsx` — D26 ledger, the `comp` kind.

## Architecture and design notes

**Five penalty kinds, one engine.**

| Kind | Payer | Victim | Rate | Base | Trigger |
| --- | --- | --- | --- | --- | --- |
| `PASSENGER_CANCEL_AFTER_START` | passenger | driver | 20% | passenger fare | cancel after `trip.STARTED` |
| `PASSENGER_NO_SHOW` | passenger | driver | 25% | passenger fare | pickup-wait expiry (05) |
| `DRIVER_LATE` | driver | passenger | 20% | driver net for that seat | free cancel taken after grace (05) |
| `DRIVER_LATE_CANCELLATION` | driver | all booked passengers | 20% | trip's expected net | cancel inside 12 h |
| `DRIVER_MISSED_START` | driver | all booked passengers | — | — | reliability only, no fee (D32b: "no earnings for you") |

**The split, exactly as `data.jsx` does it:** `victimShare = round(fee × 50 / 100)`,
`platformShare = fee − victimShare`. Subtraction, not a second rounding, so the halves always re-add to
the whole fee. A `CHECK` constraint enforces it.

**Multiple victims split the victim half.** D31 says the driver's penalty is shared "between them as ride
credit". So `victimShare` is divided by the number of affected bookings, with the remainder going to the
first victim by booking id — deterministic, and totalling exactly.

**Collection has three paths, in order of preference:**

1. **Net from an existing capture** — the money is already held, so refund the difference (P27: "The rest of the fare comes back to your Visa").
2. **Charge the stored card** — passenger cancelled after start with no capture to net against.
3. **Record as dues** — cash passenger with no card. Carried to the next booking (P25).

A driver's penalty is never charged. It is a negative ledger row deducted from the next completed trip's
earnings (D24, D31) — billing a driver directly is explicitly ruled out by the copy.

**Compensation is not trip income.** It gets its own ledger kind (`COMPENSATION`) and its own icon in
D26, because folding it into fares would overstate what a driver earned from driving.

**Dues block nothing.** P25 and P09d show dues added to the next booking's total, not preventing it. The
only hard gate is the prepay flag from slice 05 at 2 no-shows in a month.

## API contracts involved

Passenger:

```
GET  /api/v1/passenger/dues                    -> P25 {items[], total, settled}
GET  /api/v1/passenger/penalties               -> history with kind, amount, split, dispute state
POST /api/v1/passenger/penalties/{id}/dispute  -> {reason, note}
```

Driver:

```
GET  /api/v1/driver/penalties                  -> both directions: charged and compensated
POST /api/v1/driver/penalties/{id}/dispute
```

Admin:

```
GET  /api/v1/admin/penalties?kind=&status=
GET  /api/v1/admin/penalty-disputes?status=OPEN
POST /api/v1/admin/penalty-disputes/{id}/decide  { decision, note, reverseAmount? }
```

Changed: booking creation response and `POST /passenger/payments/intents` include an
`appliedDues[]` block; the checkout total from slice 03's quote gains a `dues` line.
`GET /api/v1/passenger/bookings/{id}/cancellation-terms` (slice 05) now returns the exact penalty that
would apply, priced — so P26 never guesses.

`PenaltyResponse`: `id`, `kind`, `bookingId`, `fareBase`, `percent`, `feeAmount`, `victimShare`,
`platformShare`, `payerRole`, `victimRole`, `collection{method, status, settledAt}`, `disputeState`,
`assessedAt`, `explanation`.

New errors: `PENALTY_ALREADY_DISPUTED`, `DISPUTE_WINDOW_CLOSED`, `DUES_SETTLEMENT_FAILED`.

## Database / migration changes

**`V033__penalties_dues_and_compensation.sql`** — V032 was taken by slice 05's trip materialisation.

- New `penalty.penalty_assessment`:
  `penalty_id`, `kind TEXT CHECK (...5 kinds...)`, `booking_id FK NULL`, `trip_id FK NULL`,
  `payer_app_user_id FK`, `fare_base NUMERIC(12,2)`, `percent NUMERIC(5,2)`,
  `fee_amount NUMERIC(12,2) CHECK (fee_amount >= 0)`,
  `victim_share NUMERIC(12,2)`, `platform_share NUMERIC(12,2)`,
  `status TEXT CHECK (status IN ('ASSESSED','SETTLED','WAIVED','REVERSED'))`,
  `collection_method TEXT CHECK (collection_method IN ('NETTED','CARD_CHARGE','DUES','EARNINGS_DEDUCTION'))`,
  `assessed_at`, `settled_at`, `policy_version TEXT`,
  `CHECK (victim_share + platform_share = fee_amount)`,
  `UNIQUE (kind, booking_id)` — one assessment per kind per booking, the idempotency guard.
- New `penalty.penalty_beneficiary`:
  `id`, `penalty_id FK`, `beneficiary_app_user_id FK`, `booking_id FK NULL`, `amount NUMERIC(12,2)`,
  `credited_at`, `credit_reference TEXT`.
  Sum of `amount` per penalty must equal `victim_share` — asserted by a deferred constraint trigger.
- New `penalty.passenger_due`:
  `id`, `app_user_id FK`, `penalty_id FK`, `amount NUMERIC(12,2)`, `reason TEXT`,
  `origin_booking_id FK`, `status TEXT CHECK (status IN ('OUTSTANDING','SETTLED','WAIVED'))`,
  `created_at`, `settled_at`, `settled_booking_id FK NULL`.
  Index on `(app_user_id) WHERE status = 'OUTSTANDING'`.
- New `penalty.penalty_dispute`:
  `id`, `penalty_id FK`, `raised_by_app_user_id`, `reason TEXT`, `note TEXT`,
  `status TEXT CHECK (status IN ('OPEN','UPHELD','REVERSED'))`, `raised_at`, `decided_at`,
  `decided_by_app_user_id`, `decision_note`, `reversed_amount NUMERIC(12,2)`,
  partial unique on `penalty_id WHERE status = 'OPEN'`.
- `payment.fare_ledger_entry` — extend `entry_type` with `PENALTY_CHARGE`, `PENALTY_DEDUCTION`,
  `COMPENSATION`, `DUES_SETTLEMENT`.
- `booking.booking` — add `applied_dues_amount NUMERIC(12,2) DEFAULT 0`.

## Configuration / environment changes

- All percentages read from `platform.policy_setting` (slice 03): `PAX_CANCEL_AFTER_START_PCT`, `NO_SHOW_PENALTY_PCT`, `DRIVER_LATE_PENALTY_PCT`, `LATE_CANCEL_PENALTY_PCT`, `PENALTY_VICTIM_PCT`, `PENALTY_PLATFORM_PCT`.
- `ROUTESHARE_PENALTY_DISPUTE_WINDOW_HOURS` (default `48`) — policy setting.

## UI / UX requirements

Backend slice. The contract must supply every figure these screens name:

- P25 / P25b — each due with what, why, when, which trip, amount, and the total; and the settled empty state.
- P26 / P27 — the fee, the percentage, what comes back to the card, the driver's share, ComiGo's share, and the effect on her record.
- P22 — that the card was never charged **and** the credit she receives from his penalty.
- P09d — the due added to this checkout, with its origin trip and date.
- D21 — the fee, the driver's 50%, ComiGo's 50%, and that his reliability is untouched.
- D30 / D31 — the penalty, the share reaching each named passenger, and the platform's share.
- D41 — the fee he pays and the half that reaches her.
- D26 — penalty rows negative, compensation rows positive, with distinct kinds.

## Implementation steps

1. Create the `penalty` module with entities, repositories, and `PenaltyService(+Impl)`.
2. Implement `penalty/domain/PenaltyPolicy` — kind → rate, base selector, victim resolver — reading rates from policy settings. Pure and unit-tested against `data.jsx` figures.
3. Implement `splitFee(fee)` returning `(victimShare, platformShare)` by subtraction; property-test that the halves always re-add for 0…1,000,000.
4. Implement multi-victim distribution with deterministic remainder allocation; property-test that beneficiary amounts sum to `victimShare` exactly.
5. Subscribe to slice 05's events (`booking.noshow`, `booking.driver_late`) and slice 04/07's cancel paths; assess idempotently via the `UNIQUE (kind, booking_id)` constraint.
6. Implement the three collection paths, each idempotent through `payment.payment_attempt` from slice 04.
7. Implement driver penalty deduction: write `PENALTY_DEDUCTION` against the driver's balance, netted at the next completed trip and again at payout (slice 14 consumes it).
8. Implement `PenaltyFacade.creditBeneficiary(...)` calling `RewardsFacade` — behind an interface with a temporary in-module implementation until slice 11 lands, so nothing blocks.
9. Implement the dues ledger; apply outstanding dues at booking creation, adding a `dues` line to the quote total and marking them settled on successful capture.
10. Implement disputes with the 48-hour window, admin decision, and reversal (refund or credit-back as appropriate).
11. Extend `cancellation-terms` from slice 05 to return the priced penalty.
12. Notifications for every assessment, credit and dispute decision.

## Files expected to change

- `apps/api/.../penalty/**` — new module.
- `apps/api/.../payment/**` — new ledger kinds, collection paths, dues settlement at capture.
- `apps/api/.../booking/**` — dues application at creation, cancellation terms.
- `apps/api/.../trip/**` — cancel-after-start hook.
- `apps/api/.../admin/**` — penalty and dispute endpoints.
- `apps/api/src/main/resources/db/migration/V033__penalties_dues_and_compensation.sql`.
- `apps/api/src/test/java/**` — policy tests against `data.jsx` figures, split property tests, multi-victim distribution tests, idempotency tests, dues lifecycle integration test.
- `docs/api/mobile-app.openapi.json`, `docs/api/admin-web.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/06-penalties-dues-and-compensation-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='PenaltyPolicyTest,PenaltySplitPropertyTest,MultiVictimDistributionTest,DuesLifecycleIT,PenaltyIdempotencyTest,PenaltyDisputeTest' test
```

```bash
bash scripts/simulation/verify-penalties.sh
```

The smoke script must reproduce the prototype's arithmetic: a no-show on a LKR 197 fare yields a 49
fee, 25 to the driver and 24 to ComiGo; a driver late-cancellation on LKR 429 expected net yields 86,
43 shared across two riders and 43 to ComiGo; and a repeated trigger assesses nothing further.

## Security, privacy, and observability checks

- Double-assessment is the primary risk; the `UNIQUE (kind, booking_id)` constraint is the guard and must be tested by concurrent triggers, not just sequential calls.
- A user must not be able to assess, waive or reverse their own penalty; only admin roles may decide disputes.
- Beneficiary identity is disclosed to the payer only as a first name (D31 names "Dinuka and Tharindu"); never expose a full identity or contact detail through a penalty payload.
- Every assessment, collection, credit and reversal audited with the rule, the policy version and the computed inputs — a penalty a support agent cannot explain is a refund.
- Metrics: `routeshare_penalties_total{kind,collection}`, `routeshare_penalty_disputes_total{status}`, `routeshare_dues_outstanding_amount` gauge. A rising dues gauge means cash penalties are not being recovered.
- Alert on any beneficiary-sum constraint violation — that is money created or destroyed.

## Done criteria

- [x] All five penalty kinds assessed at the right trigger, priced from policy settings.
- [x] The 50/50 split always re-adds to the fee; enforced by constraint and property test.
- [x] Multiple victims share the victim half exactly, deterministically.
- [x] Three collection paths implemented; drivers are deducted from earnings, never billed. **Netted and card-charge are unrun at runtime — no gateway (Blocker 015).**
- [x] Compensation is a distinct ledger kind, not folded into fares.
- [x] Dues are carried, shown at checkout and never block a booking. **Settlement on capture is implemented but unrun — it needs a captured card booking (Blocker 015).**
- [x] Disputes work within 48 hours with admin decision and reversal.
- [x] Every prototype money figure for P25/P26/P27/P22/D21/D30/D31/D41 reproduces exactly.
- [x] `./mvnw spotless:check verify` green, JaCoCo 80% held — 462 tests, 0 skipped.
- [x] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): add penalty assessment with 50/50 victim split, dues and compensation"
```
