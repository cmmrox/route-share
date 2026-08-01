# QA — Task 11: Referral and Rewards

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/11-referral-and-rewards.md`

## Scope

Referral codes and attribution, accrual at 1%/2% funded from commission, the 12-month / 50-trip
window, the shared rewards ledger, ride credit at checkout, and bank withdrawals above the floor.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V037`.
- Accounts: a referrer, a referee who rides, a referee who drives, and an unrelated account.

## Automated test coverage

- `ReferralAccrualTest` — rate chosen by the referee's role in that trip.
- `ReferralWindowBoundsTest` — both bounds, at the boundary.
- `SelfReferralGuardTest` — blocked on code, and at the schema level.
- `RewardsLedgerIdempotencyTest` — concurrent accrual for the same (edge, booking) produces one row.
- `CreditApplicationIT` — applied, capped, released on void.
- `CommissionCapTest` — accrual never exceeds the trip's commission.
- `WithdrawalFloorTest` — below floor refused; one open withdrawal at a time.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 11-1 | Signup with a valid code | Edge created; window 12 months; max 50 trips |
| 11-2 | Signup with own code | `REFERRAL_SELF_NOT_ALLOWED` |
| 11-3 | Second code claim by the same user | `REFERRAL_ALREADY_ATTRIBUTED` |
| 11-4 | Invalid code | `REFERRAL_CODE_INVALID` |
| 11-5 | Referee signs up but never travels | Referrer earns nothing |
| 11-6 | Referee completes a ride paying LKR 290 | Referrer accrues 3 (1%) |
| 11-7 | Referee completes a drive keeping LKR 1,240 | Referrer accrues 25 (2%) |
| 11-8 | Same referee rides and drives | Correct rate per trip, not per edge |
| 11-9 | 50th trip | Accrues; edge closes `EXPIRED_TRIPS` |
| 11-10 | 51st trip | No accrual |
| 11-11 | Trip at 12 months + 1 day | No accrual; edge `EXPIRED_WINDOW` |
| 11-12 | Concurrent accrual triggers | One ledger row |
| 11-13 | Accrual larger than the trip's commission | Capped; shortfall recorded |
| 11-14 | Ledger balance | Equals the sum of signed rows |
| 11-15 | Penalty compensation from slice 06 | Appears as kind `COMPENSATION` in the same ledger |
| 11-16 | Checkout with `autoApply` on | Credit line applied, capped at the fare |
| 11-17 | Credit larger than the fare | Applied amount equals the fare; balance retains the rest |
| 11-18 | Booking voided before capture | Credit released back to the balance |
| 11-19 | `autoApply` off | No credit applied |
| 11-20 | Withdrawal at LKR 800 | `REWARDS_BELOW_BANK_MINIMUM` |
| 11-21 | Withdrawal at LKR 1,400 | Queued for the next Friday batch |
| 11-22 | Second withdrawal while queued | `WITHDRAWAL_ALREADY_QUEUED` |
| 11-23 | Invited list payload | First name + initial only; no phone, email or full name |

## Manual checks

- Reconcile one accrual end to end: platform commission down, referrer balance up, driver earnings untouched.
- Confirm the referral-cost alert fires when cost exceeds the configured share of commission.
- Confirm a rider with a rewards balance and a driver profile sees one balance, not two.

## Evidence to collect

- `scripts/simulation/verify-referral-rewards.sh` output with the figure comparison.
- Ledger extract for a full accrual → spend → withdraw cycle.
- Commission-vs-referral-cost reconciliation for the run.

## Pass/fail criteria

Pass when: attribution is one-time and self-referral impossible; rates follow the referee's role per
trip; both window bounds close the edge; cost is capped at commission; credit caps at the fare and
releases on void; and the withdrawal floor and single-open-request rule hold.

Fail on: any double accrual, any accrual after a bound, any credit producing a negative payable, or a
driver's earnings reduced by someone else's referral.
