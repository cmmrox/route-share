---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 14 — Money Operations: Payouts, Ledger and Adjustments

**Goal:** Pay drivers every Friday above a LKR 1000 floor, show them a ledger that reconciles to the rupee, and close the fare-adjustment loop with a real decision and a dispute window.

**Depends on:** 06, 11.
**Blocks:** nothing.

## Objective

`finance.payout_batch` exists but has no cadence, no floor, no held balance and no driver-facing endpoint.
The prototype is specific: **weekly, every Friday, minimum LKR 1000, and anything below rolls into next
week — nothing is lost, it only waits.** D33b exists purely to explain that to a driver under the floor.

The ledger (D26) has a convention the current implementation does not follow: gross fare in, commission
out, as two rows. Penalties appear negative, compensation appears positive under its own kind, and the
wallet balance is the sum of everything except settled payouts.

Fare adjustments (D29) currently stop at "request submitted" — there is no decision, no approval path,
no rejection reason, and no 48-hour dispute.

Screens closed by this slice: D25 earnings, D26 ledger, D27 payout account, D29–D29c fare adjustment,
D33/D33b weekly payout, P17c adjusted receipt, plus the admin finance surfaces behind all of them.

## Scope

In scope:

- Weekly Friday payout batch: eligibility, floor, held balance, netting, statement.
- Driver wallet balance and the ledger with all six kinds.
- Cash-commission netting from the payout.
- Rewards withdrawals joining the same batch (slice 11 queues them).
- Fare-adjustment decision workflow with three outcomes and a 48-hour dispute.
- Admin finance surfaces for all of the above.

Out of scope:

- Penalty assessment — slice 06.
- Referral accrual — slice 11.
- The payment gateway payout rail itself; batches are produced and marked paid by operations, exactly as the prototype describes ("A ComiGo admin runs the batch for every driver. There is no on-demand withdrawal.").

## Source material / references

- `docs/source-assets/comigo-prototype/driver-money.jsx` — D25 earnings + wallet card, D26 ledger with all six kinds, D27 payout account + rules, D29/D29a/D29b/D29c adjustment states, D33/D33b payout eligible and held.
- `docs/source-assets/comigo-prototype/data.jsx` — `LEDGER`, `ledgerBalance`, `PAYOUT`, `payoutEligible`, `WEEK_DAYS`, `weekTotal`, `POLICY.payoutCadence/payoutDay/payoutMinimum`, `FARE_ADJUST`.
- `docs/source-assets/comigo-prototype/passenger-trip.jsx` — P17c adjusted receipt and its 48-hour dispute line.
- `docs/source-assets/comigo-prototype/driver-live.jsx` — D23 cash collected, D24 penalty deducted from the next trip.
- Current code: `finance/**`, `payment/service/impl/PaymentServiceImpl.java`, `payment/controller/DriverEarningsController.java`, `admin/controller/AdminFinanceController.java`.

## Architecture and design notes

**The ledger is the source of truth; the wallet is a projection.** Balance is
`SUM(amount) WHERE kind <> 'PAYOUT'` over settled rows — D26 states it explicitly: the payout row settles
an earlier balance and is not part of what is owed now. Storing a mutable balance column invites drift
between what the driver sees and what the batch pays.

**Gross in, commission out.** A trip writes two rows: `+grossFare` and `−commission`. Writing the net as a
single row and then also showing the fee double-counts, which is exactly the bug the prototype's comment
warns about.

**Six kinds, distinguishable in the UI:** `FARE`, `FEE`, `ADJUSTMENT`, `PENALTY`, `COMPENSATION`,
`PAYOUT`. Compensation is not fare income and must never be summed into "earned from driving".

**The batch is idempotent by period.** A batch is keyed on `(period_start, period_end)`; running it twice
must produce one batch. Items are keyed on `(batch_id, driver_profile_id)`. Below-floor drivers are
recorded as *held* with their balance, not omitted — D33b's "held, not lost" is a data statement.

**Netting order matters and is stated in D27:** penalties and fees come off before the balance is paid.
So the item computation is `fares − commission − penalties + compensation + adjustments − cashCommissionOwed`,
and if that is below the floor the whole item is held.

**Fare adjustment is a two-sided object.** The driver requests, an admin decides against the recorded GPS
route, and on approval the passenger's card is charged the difference and the driver's ledger credited.
The passenger then has 48 hours to dispute — a window that must be enforced server-side, since P17c
promises it.

**Rejected adjustments change nothing.** D29c is explicit: "Nothing changes on your earnings for this
trip." No ledger row is written on rejection, only the decision record.

## API contracts involved

Driver:

```
GET  /api/v1/driver/wallet                     -> balance, floor, eligible, nextPayoutDate, shortfall
GET  /api/v1/driver/ledger?kind=&from=&to=&page=&size=
GET  /api/v1/driver/earnings/summary           -> week total, by-day bars, trips, passengers, per-trip
GET  /api/v1/driver/payouts                    -> history with statements
GET  /api/v1/driver/payouts/{id}               -> the trips behind it
POST /api/v1/driver/bookings/{id}/fare-adjustments   { reasonCode, extraKm, amount, explanation }
GET  /api/v1/driver/fare-adjustments/{id}
```

Passenger:

```
GET  /api/v1/passenger/bookings/{id}/fare-adjustment
POST /api/v1/passenger/fare-adjustments/{id}/dispute   { reason }
```

Admin:

```
GET  /api/v1/admin/fare-adjustments?status=PENDING
POST /api/v1/admin/fare-adjustments/{id}/decide  { decision, note, approvedAmount? }
POST /api/v1/admin/settlements/payout-batches/run { periodStart, periodEnd }   -> idempotent
GET  /api/v1/admin/settlements/payout-batches/{id}/items?held=true
POST /api/v1/admin/settlements/payout-batches/{id}/mark-paid
GET  /api/v1/admin/finance/ledger-reconciliation
```

Adjustment reason codes (D29): `ROAD_CLOSED_DETOUR`, `PASSENGER_EXTENDED_TRIP`, `EXTENDED_WAIT`, `OTHER`.

New errors: `PAYOUT_BELOW_MINIMUM`, `BATCH_ALREADY_RUN`, `ADJUSTMENT_ALREADY_DECIDED`,
`DISPUTE_WINDOW_CLOSED`, `LEDGER_OUT_OF_BALANCE`.

## Database / migration changes

**`V040__money_operations.sql`**

- New `payment.driver_ledger_entry` (driver-scoped, distinct from the booking-scoped `fare_ledger_entry`):
  `id`, `driver_profile_id FK`, `kind TEXT CHECK (kind IN ('FARE','FEE','ADJUSTMENT','PENALTY','COMPENSATION','PAYOUT'))`,
  `amount NUMERIC(12,2) NOT NULL`, `label TEXT`, `sublabel TEXT`,
  `booking_id FK NULL`, `trip_id FK NULL`, `penalty_id FK NULL`, `payout_batch_item_id FK NULL`,
  `occurred_at`, `idempotency_key TEXT UNIQUE`.
  Signed amounts; unique key prevents double-posting.
- `finance.payout_batch` — add `period_start DATE`, `period_end DATE`, `cadence TEXT DEFAULT 'WEEKLY'`,
  `minimum_amount NUMERIC(12,2)`, `run_at`, `run_by_app_user_id`,
  `UNIQUE (period_start, period_end)`.
- `finance.payout_batch_item` — add `driver_profile_id FK`, `gross_fares NUMERIC(12,2)`,
  `commission NUMERIC(12,2)`, `penalties NUMERIC(12,2)`, `compensation NUMERIC(12,2)`,
  `adjustments NUMERIC(12,2)`, `cash_commission_owed NUMERIC(12,2)`, `rewards_withdrawal NUMERIC(12,2)`,
  `net_payable NUMERIC(12,2)`, `held BOOLEAN NOT NULL DEFAULT false`, `held_reason TEXT`,
  `UNIQUE (payout_batch_id, driver_profile_id)`.
- New `payment.fare_adjustment`:
  `id`, `booking_id FK`, `requested_by_app_user_id`, `reason_code TEXT`, `extra_km NUMERIC(8,2)`,
  `requested_amount NUMERIC(12,2)`, `explanation TEXT`,
  `status TEXT CHECK (status IN ('PENDING','APPROVED','REJECTED','DISPUTED','REVERSED'))`,
  `approved_amount NUMERIC(12,2) NULL`, `decided_at`, `decided_by_app_user_id`, `decision_note`,
  `dispute_window_ends_at`, `disputed_at`, `dispute_reason`,
  partial unique on `booking_id WHERE status IN ('PENDING','APPROVED')`.
- Index `idx_driver_ledger_balance ON payment.driver_ledger_entry(driver_profile_id, occurred_at) WHERE kind <> 'PAYOUT'`.

## Configuration / environment changes

- Policy settings: `PAYOUT_CADENCE` (`WEEKLY`), `PAYOUT_DAY` (`FRIDAY`), `PAYOUT_MINIMUM` (1000), `ADJUSTMENT_DISPUTE_WINDOW_HOURS` (48).
- New scheduler job on slice 05's infrastructure: `weekly-payout-batch`, Fridays, leader-elected. The job **prepares** the batch; marking it paid stays a human action, per D33's "an admin runs the batch".

## UI / UX requirements

Backend slice. The contract must supply:

- D25 — week total, per-day bars, wallet balance against the floor with a progress ratio, next payout date, and the three summary tiles.
- D26 — every row with kind, label, sublabel, timestamp and signed amount, filterable, plus the wallet balance excluding payout rows.
- D27 — the payout account, the fixed schedule, the three rules (minimum, card-fares-only, penalties-first) and the last payout.
- D29 — reason options and the amount derivation; D29a/b/c — the three outcome states with the reviewer's note.
- D33 / D33b — scheduled vs held, the shortfall, the progress bar, and how payouts work.
- P17c — the adjusted receipt line, who requested, who approved, and the dispute window remaining.

## Implementation steps

1. Create `payment.driver_ledger_entry` and post to it from every money event already built: trip completion (fare + fee), penalties and compensation (slice 06), adjustments (this slice), cash commission owed (slice 04), payouts (this slice).
2. Implement the wallet balance projection and the ledger query with filters and stable paging.
3. Implement the weekly batch: compute the period, aggregate per driver in the netting order above, include queued rewards withdrawals from slice 11, apply the floor, mark below-floor items `held` with their balance, write a `PAYOUT` ledger row only on mark-paid.
4. Make the batch idempotent on `(period_start, period_end)` and items idempotent on `(batch_id, driver_profile_id)`; test a double run.
5. Register `weekly-payout-batch` to prepare on Fridays; keep mark-paid manual and audited.
6. Implement the payout statement — the trips and rows behind each item — for D27 and the emailed statement.
7. Implement the fare-adjustment request (replacing the current stub), the admin decision with three outcomes, and the approval effects: charge the passenger the difference through slice 04's capture path, credit the driver's ledger as `ADJUSTMENT`, notify both sides.
8. On rejection write no ledger row; store the reviewer's note, which D29c renders verbatim.
9. Implement the 48-hour passenger dispute with a server-enforced window; a upheld dispute reverses the charge and the credit.
10. Implement `ledger-reconciliation` for admin: any driver whose ledger sum disagrees with their batch history, and any orphaned ledger row.
11. Rewrite `DriverEarningsController`'s summary and transactions onto the new ledger, removing the old ad-hoc aggregation.

## Files expected to change

- `apps/api/.../payment/**` — driver ledger, fare adjustments, earnings rewrite.
- `apps/api/.../finance/**` — batch cadence, floor, held items, statements, reconciliation.
- `apps/api/.../rewards/**` — withdrawal handoff into the batch (interface already defined in slice 11).
- `apps/api/.../scheduling/**` — the weekly job.
- `apps/api/.../admin/**` — adjustment decisions, batch run/mark-paid, reconciliation.
- `apps/api/src/main/resources/db/migration/V040__money_operations.sql`.
- `apps/api/src/test/java/**` — ledger convention tests, balance projection tests, batch idempotency and floor tests, netting order tests, adjustment lifecycle and dispute-window tests, reconciliation tests.
- `docs/api/mobile-app.openapi.json`, `docs/api/admin-web.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/14-money-operations-payouts-and-adjustments-qa.md`

Maestro: not applicable — no mobile surface in this slice.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='DriverLedgerConventionTest,WalletBalanceProjectionTest,PayoutBatchIdempotencyIT,PayoutFloorAndHeldTest,NettingOrderTest,FareAdjustmentLifecycleIT,DisputeWindowTest,LedgerReconciliationTest' test
```

```bash
bash scripts/simulation/verify-money-operations.sh
```

The smoke must reproduce D26's ledger exactly from seeded trips — gross rows, negative fee rows, a
negative penalty, a positive compensation, and a wallet balance that equals the sum excluding the payout
row — then run the batch twice and prove one batch, and run a below-floor driver and prove a held item
with the balance intact.

## Security, privacy, and observability checks

- Money leaves the platform here, so authorization is absolute: only `FINANCE_ADMIN`/`ADMIN`/`SUPER_ADMIN` may run or mark-pay a batch, and every action is audited with actor, period, totals and item count.
- Mark-paid is irreversible in practice; require an explicit confirmation payload and record it.
- A driver may read only their own wallet, ledger, payouts and statements — test cross-tenant access explicitly.
- Payout account numbers stay masked in every response (the existing masking already does this); never log them.
- Ledger rows are append-only. No update or delete path may exist; corrections are compensating `ADJUSTMENT` rows. Enforce with a database rule and a test.
- Metrics: `routeshare_payout_batches_total{status}`, `routeshare_payout_items_total{held}`, `routeshare_payout_amount_total`, `routeshare_ledger_reconciliation_failures` gauge, `routeshare_fare_adjustments_total{status}`.
- Alert on any reconciliation failure and on a Friday batch that did not run.

## Done criteria

- [ ] Driver ledger follows the gross-in / commission-out convention with all six kinds.
- [ ] Wallet balance is a projection excluding payout rows and reconciles to D26 exactly.
- [ ] Weekly Friday batch prepares automatically, is idempotent per period, and is marked paid by an audited human action.
- [ ] The LKR 1000 floor holds balances rather than dropping them, with a visible shortfall.
- [ ] Netting order matches D27: penalties and fees before payment, cash commission netted.
- [ ] Rewards withdrawals ride the same batch.
- [ ] Fare adjustments have three real outcomes; approval charges and credits, rejection changes nothing.
- [ ] The 48-hour passenger dispute is enforced server-side and can reverse the charge.
- [ ] Ledger is append-only; corrections are compensating rows.
- [ ] Reconciliation endpoint detects induced imbalances in test.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): add weekly payouts with floor, driver ledger and fare-adjustment decisions"
```
