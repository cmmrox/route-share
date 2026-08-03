---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 11 — Referral and Rewards

**Goal:** Pay people for who they bring, out of ComiGo's commission and nobody else's earnings, into one shared balance a rider spends as credit and a driver withdraws to a bank.

**Depends on:** 03, 06.
**Blocks:** 14 (the Friday batch carries withdrawals).

## Objective

The signup screen (S05) carries a referral-code field literally tagged **"NEEDS BACKEND"** in the
prototype. Nothing behind it exists.

The rule is precise and slightly unusual: the rate depends on what the *referee* does, not on what the
referrer is. They ride, you get 1% of the fare they pay. They drive, you get 2% of what they keep. It runs
for 12 months or their first 50 trips, whichever ends first, and it is paid **out of the 10% commission**
— never out of a driver's earnings and never off a friend's fare.

The balance it feeds is shared across both roles and also receives penalty compensation from slice 06. A
rider spends it as ride credit with no floor; a driver moves it to a bank account once it clears LKR 1000,
in the Friday batch that already exists.

## Scope

In scope:

- Referral codes, links, and attribution at signup.
- The referral edge with its 12-month / 50-trip window.
- Accrual on each completed trip by a referee, at 1% or 2% depending on their role in that trip.
- Commission-sourced accounting, so platform revenue absorbs the cost.
- The shared rewards ledger, replacing slice 06's temporary stub.
- Ride credit applied at checkout, with an opt-out.
- Bank withdrawal above the LKR 1000 floor, queued into the weekly payout.
- Referee's first-ride discount.

Out of scope:

- The payout batch mechanics themselves — slice 14.
- Penalty assessment — slice 06; this slice only receives its compensation credits.

## Source material / references

- `docs/source-assets/comigo-prototype/passenger-identity.jsx` — P32 / D37 referral, P33 / D38 / D38b rewards balance.
- `docs/source-assets/comigo-prototype/data.jsx` — `POLICY.referralPaxPct/referralDriverPct/referralWindowMonths/referralMaxTrips/referralPaidFrom/refereeFirstRideDiscount/rewardsBankMinimum`, `REFERRAL`, `REWARDS_ROWS`, `rewardsBalance`, `rewardsWithdrawable`, `referralTripsLeft`.
- `docs/source-assets/comigo-prototype/shell.jsx` — S05 profile setup, the referral-code field.
- `docs/source-assets/comigo-prototype/passenger-book.jsx` — P09e credit applied at checkout with a "Don't use" action.
- `docs/source-assets/comigo-prototype/shared-account.jsx` — S15/S16 rewards rows.

## Architecture and design notes

**One balance, two exits.** There is no separate rider wallet and driver wallet. The same ledger is spent
as ride credit (no floor) or withdrawn to a bank (floor LKR 1000, Friday batch). Modelling two balances
would immediately raise "why can't I spend my driver rewards on a ride", which the prototype answers by
having one.

**Accrual is per completed trip, not per signup.** P32 is explicit: "You start earning on their first
completed trip — a sign-up on its own pays nothing." So the trigger is `trip.completed` with a settled
fare, and the accrual reads that trip's persisted quote from slice 03.

**Rate depends on the referee's role in that specific trip.** A referee who both rides and drives earns
their referrer 1% on their rides and 2% on their drives. Keying the rate off the edge rather than the
event would be wrong for exactly the person the product most wants.

**The window is whichever ends first**, and both bounds must be checked at accrual time, not by a nightly
job alone — a referee could complete their 50th trip at any moment. The job exists to close expired edges
so the UI can show "0 still earning", not to enforce the rule.

**Paid from commission is an accounting statement, not a comment.** Every accrual writes a
`REFERRAL_PAYOUT` row against platform revenue and a `REFERRAL_EARNING` credit to the referrer. Platform
net for that trip is `commission − referralCost`. If referral cost could ever exceed commission on a
trip, the accrual is capped at the commission and the shortfall is recorded — otherwise a promotion could
silently make a trip loss-making.

**Attribution is one-time and immutable.** A user has at most one referrer, set at signup, never editable,
and self-referral is rejected on code, phone and device.

**Ride credit is applied automatically with an opt-out** (P09e: "We use it on every booking until it runs
out — turn it off here if you'd rather save it"). The applied amount is capped at the fare, never creates
a negative total, and is released if the booking is cancelled before capture.

## API contracts involved

```
GET  /api/v1/me/referral                 -> code, link, invited, joined, rows[], totals
POST /api/v1/me/referral/claim           { code }   -> only during signup window
GET  /api/v1/me/rewards                  -> balance, rows[], withdrawable, floor, shortfall
POST /api/v1/me/rewards/withdrawals      -> queues into the next Friday batch
GET  /api/v1/me/rewards/withdrawals
PUT  /api/v1/me/rewards/auto-apply       { enabled }
```

`ReferralResponse.rows[]`: `who` (first name + initial), `role`, `joinedAt`, `trips`, `tripsLeft`,
`earned`. Never a full name, never contact details.

`RewardsResponse`: `balance`, `bankMinimum`, `withdrawable`, `shortfall`, `autoApply`,
`rows[]{occurredAt, kind: REFERRAL|COMPENSATION|SPEND|WITHDRAWAL, label, sublabel, amount}`.

Changed: booking creation and the checkout quote gain an `appliedCredit` line; `POST /passenger/bookings`
accepts `useRewardsCredit: boolean` defaulting to the user's `autoApply`.

Signup: `POST /api/v1/passenger/profile` accepts an optional `referralCode`.

New errors: `REFERRAL_CODE_INVALID`, `REFERRAL_SELF_NOT_ALLOWED`, `REFERRAL_ALREADY_ATTRIBUTED`,
`REFERRAL_WINDOW_CLOSED`, `REWARDS_BELOW_BANK_MINIMUM`, `WITHDRAWAL_ALREADY_QUEUED`.

## Database / migration changes

**`V038__referral_and_rewards.sql`**

- New `rewards.referral_code`:
  `app_user_id PK FK`, `code TEXT UNIQUE NOT NULL`, `created_at`. Code generated from the display name plus entropy, uppercase, ambiguity-free alphabet.
- New `rewards.referral_edge`:
  `id`, `referrer_app_user_id FK`, `referee_app_user_id FK UNIQUE`, `code TEXT`,
  `attributed_at`, `window_expires_at`, `max_trips INT`, `trips_counted INT DEFAULT 0`,
  `status TEXT CHECK (status IN ('ACTIVE','EXPIRED_WINDOW','EXPIRED_TRIPS','REVOKED'))`,
  `CHECK (referrer_app_user_id <> referee_app_user_id)`.
  The `UNIQUE` on referee enforces one-time attribution; the `CHECK` blocks self-referral at the schema level.
- New `rewards.rewards_ledger`:
  `id`, `app_user_id FK`, `kind TEXT CHECK (kind IN ('REFERRAL','COMPENSATION','SPEND','WITHDRAWAL','ADJUSTMENT'))`,
  `amount NUMERIC(12,2) NOT NULL`, `label TEXT`, `sublabel TEXT`,
  `source_booking_id FK NULL`, `source_penalty_id FK NULL`, `referral_edge_id FK NULL`,
  `withdrawal_id FK NULL`, `occurred_at`, `idempotency_key TEXT UNIQUE`.
  Signed amounts; balance is `SUM(amount)`. The unique idempotency key makes double-accrual impossible.
- New `rewards.withdrawal`:
  `id`, `app_user_id FK`, `amount NUMERIC(12,2) CHECK (amount > 0)`,
  `status TEXT CHECK (status IN ('QUEUED','BATCHED','PAID','FAILED','CANCELLED'))`,
  `payout_batch_id FK NULL`, `requested_at`, `batched_at`, `paid_at`, `failure_reason`,
  partial unique on `app_user_id WHERE status IN ('QUEUED','BATCHED')` — one open withdrawal at a time.
- `passenger.passenger_profile` — add `rewards_auto_apply BOOLEAN NOT NULL DEFAULT true`.
- `booking.booking` — add `applied_credit_amount NUMERIC(12,2) NOT NULL DEFAULT 0`.
- `payment.fare_ledger_entry` — extend `entry_type` with `REFERRAL_PAYOUT` (platform-side cost).
- Index `idx_referral_edge_active ON rewards.referral_edge(referee_app_user_id) WHERE status = 'ACTIVE'`.

## Configuration / environment changes

- Policy settings: `REFERRAL_PAX_PCT` (1), `REFERRAL_DRIVER_PCT` (2), `REFERRAL_WINDOW_MONTHS` (12), `REFERRAL_MAX_TRIPS` (50), `REFEREE_FIRST_RIDE_DISCOUNT` (150), `REWARDS_BANK_MINIMUM` (1000).
- `ROUTESHARE_REFERRAL_LINK_BASE_URL` (default `https://comigo.lk/j/`).
- New scheduler job on slice 05's infrastructure: `referral-window-expiry`, daily.

## UI / UX requirements

Backend slice. The contract must supply:

- P32 / D37 — total earned, the two rates with their conditions, the link and code, the invited list with role chips and trips-remaining progress, and the three small-print rules.
- P33 / D38 / D38b — balance, the two summary tiles, the ledger rows with kind icons, the floor, the shortfall, and the correct primary action per mode and eligibility.
- P09e — the credit line on checkout with the remaining balance and the opt-out.
- S05 — referral code accepted at signup and validated.
- S15 / S16 — the account rows' subtitles: joined count, earned total, balance and withdraw/spend hint.

## Implementation steps

1. Generate a referral code per user on profile creation; expose code and link.
2. Implement claim at signup with validation: exists, not self, referee not already attributed, and a signup-window check.
3. Create the edge with `window_expires_at = now + 12 months` and `max_trips = 50`.
4. Subscribe to `trip.completed`; for each completed booking, resolve whether the participant is a referee with an active edge; determine their role in that trip; accrue at the correct rate from the persisted quote; increment `trips_counted`; close the edge if either bound is reached.
5. Cap accrual at that trip's commission; record any shortfall for finance visibility.
6. Write the paired rows — `REFERRAL_PAYOUT` on the platform side, `REFERRAL` credit to the referrer — under one idempotency key derived from `(edgeId, bookingId)`.
7. Replace slice 06's temporary compensation stub with real `COMPENSATION` rows in this ledger.
8. Implement `autoApply` and credit application at booking: cap at the fare, write a `SPEND` row, release it if the booking is voided before capture.
9. Implement withdrawals: reject below the floor, one open at a time, `QUEUED` until slice 14's Friday batch picks it up; `PAID` closes it.
10. Implement the referee's first-ride discount as a one-time credit granted on attribution, visible as a ledger row.
11. Register `referral-window-expiry` to close edges past 12 months so the UI's "still earning" count is honest.

## Files expected to change

- `apps/api/.../rewards/**` — new module: codes, edges, ledger, withdrawals, accrual listener.
- `apps/api/.../penalty/**` — replace the compensation stub with the real facade call.
- `apps/api/.../booking/**` — credit application and release.
- `apps/api/.../payment/**` — `REFERRAL_PAYOUT` ledger entries and commission capping.
- `apps/api/.../passenger/**` — referral code at signup, `rewards_auto_apply`.
- `apps/api/.../scheduling/**` — window expiry job.
- `apps/api/src/main/resources/db/migration/V038__referral_and_rewards.sql`.
- `apps/api/src/test/java/**` — accrual rate tests, window/trip bound tests, self-referral tests, idempotency tests, credit application and release tests, commission-cap test.
- `docs/api/mobile-app.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/11-referral-and-rewards-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='ReferralAccrualTest,ReferralWindowBoundsTest,SelfReferralGuardTest,RewardsLedgerIdempotencyTest,CreditApplicationIT,CommissionCapTest,WithdrawalFloorTest' test
```

```bash
bash scripts/simulation/verify-referral-rewards.sh
```

The smoke must reproduce the prototype's figures: a referred driver completing a drive with LKR 1,240
kept accrues 25 to the referrer; a referred rider paying LKR 290 accrues 3; the 51st trip accrues nothing;
an edge past 12 months accrues nothing; a withdrawal below LKR 1000 is refused.

## Security, privacy, and observability checks

- Referral fraud is the risk. Self-referral is blocked at the schema level; also check phone-number reuse and reject codes claimed after the signup window. Log attempts.
- The invited list is other people's data. Return first name plus initial only — never a full name, phone, email or exact join time beyond a date.
- Balance is money. Every ledger write carries a unique idempotency key; concurrent accrual attempts for the same `(edge, booking)` must produce exactly one row — test with parallel triggers.
- A credit must never exceed the fare or produce a negative payable; assert with a property test.
- Withdrawals must not be requestable twice; the partial unique index is the guard and must be tested concurrently.
- Metrics: `routeshare_referral_attributions_total`, `routeshare_referral_accruals_total{refereeRole}`, `routeshare_referral_cost_amount`, `routeshare_rewards_balance_total` gauge, `routeshare_withdrawals_total{status}`.
- Alert if referral cost exceeds a configured share of commission over a rolling window — that is a promotion running away.

## Done criteria

- [x] Codes generated, links returned, attribution one-time and immutable, self-referral impossible.
- [x] Accrual fires on completed trips at 1% for a referee's rides and 2% of net for their drives.
- [x] Both window bounds enforced at accrual, and expired edges closed by the daily job.
- [x] Referral cost is booked against commission and capped so no trip goes loss-making.
- [x] One shared ledger serves both roles; slice 06's compensation writes into it.
- [x] Ride credit auto-applies with an opt-out, caps at the fare, and releases on void.
- [x] Withdrawals respect the LKR 1000 floor, allow one open request, and queue for the Friday batch.
- [x] Referee first-ride discount granted once and visible.
- [x] Every prototype figure for P32/P33/D37/D38/D38b reproduces exactly.
- [x] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [x] Tracking docs updated; focused commit ready.

## Implementation evidence — 2026-08-03

Slice 10 already occupied V037, so this implementation uses
`V038__referral_and_rewards.sql`. A clean local database and Testcontainers both migrated from
V001 through V038.

- Focused Slice 11 tests and the penalty-compensation regression are green.
- Full Maven verification is green with 609 tests, 0 skipped, and 84.33% instruction coverage.
- OpenAPI validation and `@routeshare/api-contracts` type-checking are green.
- `verify-referral-rewards.sh` passed 28/28 checks on the live local stack. It reproduced LKR 3
  from a rider fare of LKR 290 and LKR 25 from driver net of LKR 1,240; it also proved
  first-ride credit, both edge bounds, idempotency, commission-side accounting, the withdrawal
  floor, one-open-withdrawal rule, and the schema self-referral guard.

## Suggested commit message

```bash
git commit -m "feat(api): add referral attribution, commission-funded accrual and a shared rewards balance"
```
