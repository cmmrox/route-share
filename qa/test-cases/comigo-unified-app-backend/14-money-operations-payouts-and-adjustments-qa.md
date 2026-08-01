# QA — Task 14: Money Operations: Payouts, Ledger and Adjustments

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/14-money-operations-payouts-and-adjustments.md`

## Scope

The driver ledger with all six kinds, the wallet projection, the weekly Friday batch with the LKR
1000 floor and held balances, cash-commission netting, and the fare-adjustment decision workflow with its
48-hour dispute.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V040`.
- Seeded drivers: one above the floor, one below, one with penalties and compensation, one with a queued rewards withdrawal.

## Automated test coverage

- `DriverLedgerConventionTest` — gross in, commission out, never a net-only row.
- `WalletBalanceProjectionTest` — balance excludes payout rows and reconciles.
- `PayoutBatchIdempotencyIT` — a double run produces one batch.
- `PayoutFloorAndHeldTest` — below-floor items held with the balance intact.
- `NettingOrderTest` — penalties and fees before payment, cash commission netted.
- `FareAdjustmentLifecycleIT` — request, three outcomes, effects.
- `DisputeWindowTest` — 48 hours enforced server-side.
- `LedgerAppendOnlyTest` — no update or delete path exists.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 14-1 | Completed trip | Two rows: `+gross` FARE and `−commission` FEE |
| 14-2 | Net-only row anywhere | Never — would double-count against the fee row |
| 14-3 | Penalty | Negative `PENALTY` row |
| 14-4 | Compensation | Positive `COMPENSATION` row, distinct from FARE |
| 14-5 | Wallet balance | Sum of all rows excluding `PAYOUT` |
| 14-6 | D26 ledger reproduction | Matches `data.jsx` row for row and total |
| 14-7 | Friday batch run | Items created per eligible driver |
| 14-8 | Same period run twice | One batch (`BATCH_ALREADY_RUN`) |
| 14-9 | Driver at LKR 740 | Item `held=true`, balance intact, shortfall reported |
| 14-10 | Driver at LKR 8,420 | Item payable |
| 14-11 | Netting order | Penalties and fees deducted before the floor test |
| 14-12 | Cash commission owed | Netted from the payout |
| 14-13 | Queued rewards withdrawal | Included in the same batch item |
| 14-14 | Mark paid | `PAYOUT` ledger row written; balance drops; audited |
| 14-15 | Mark paid without confirmation payload | Refused |
| 14-16 | Non-finance role runs a batch | 403 |
| 14-17 | Driver reads another driver's ledger | 403 |
| 14-18 | Ledger row update or delete attempted | Refused at the database level |
| 14-19 | Fare adjustment requested | `PENDING`; nothing charged |
| 14-20 | Adjustment approved for LKR 120 | Passenger charged the difference; driver credited `ADJUSTMENT`; both notified |
| 14-21 | Adjustment rejected | **No ledger row**; reviewer note stored and returned verbatim |
| 14-22 | Second adjustment on the same booking while one is pending | Refused |
| 14-23 | Passenger disputes at 47 h | Accepted |
| 14-24 | Passenger disputes at 49 h | `DISPUTE_WINDOW_CLOSED` |
| 14-25 | Dispute upheld | Charge reversed; ledger corrected by a compensating row, not an edit |
| 14-26 | Reconciliation with an induced imbalance | Detected and reported |

## Manual checks

- Reproduce D26's ledger by hand from the seeded trips and compare row by row.
- Confirm payout account numbers are masked in every response and absent from all logs.
- Confirm the missed-Friday-batch alert fires when the job is disabled for a cycle.

## Evidence to collect

- `scripts/simulation/verify-money-operations.sh` output.
- Batch and item extracts for both an eligible and a held driver.
- Reconciliation report before and after the induced imbalance.

## Pass/fail criteria

Pass when: the ledger follows gross-in/commission-out with six distinct kinds; the wallet projection
reconciles; the batch is idempotent per period; below-floor balances are held not lost; the netting order
matches D27; adjustments have three real outcomes with a server-enforced dispute window; and the ledger is
provably append-only.

Fail on: any net-only fare row, any balance that disagrees with the ledger, a double-run batch, a dropped
below-floor balance, or any mutable ledger row.
