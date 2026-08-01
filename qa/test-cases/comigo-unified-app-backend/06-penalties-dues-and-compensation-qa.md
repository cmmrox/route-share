# QA — Task 06: Penalties, Dues and Compensation

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/06-penalties-dues-and-compensation.md`

## Scope

Assessment of all five penalty kinds, the 50/50 split, three collection paths, the passenger
dues ledger, compensation credits and disputes. Out of scope: the rewards balance itself (slice 11) and
payout netting (slice 13).

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V032`.
- Slice 05's events available to trigger assessments.

## Automated test coverage

- `PenaltyPolicyTest` — kind → rate → base, against `data.jsx` figures.
- `PenaltySplitPropertyTest` — halves always re-add, 0…1,000,000.
- `MultiVictimDistributionTest` — beneficiary amounts sum exactly to the victim share.
- `PenaltyIdempotencyTest` — concurrent triggers assess once.
- `DuesLifecycleIT` — accrue, carry, apply at checkout, settle on capture.
- `PenaltyDisputeTest` — 48-hour window, decision, reversal.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 06-1 | No-show on a LKR 197 fare | fee 49, victim 25, platform 24 |
| 06-2 | Passenger cancel after start on LKR 267 | fee 53, victim 27, platform 26 |
| 06-3 | Driver late-cancel on LKR 429 expected net | fee 86, 43 shared across riders, 43 platform |
| 06-4 | Split re-adds | `victimShare + platformShare == feeAmount` in every case |
| 06-5 | Two victims, odd victim share | Remainder to the first by booking id; sum exact |
| 06-6 | Same trigger fired twice | One assessment (unique constraint) |
| 06-7 | Concurrent triggers | One assessment |
| 06-8 | Card passenger, capture exists | Netted; remainder refunded |
| 06-9 | Card passenger, no capture | Card charged |
| 06-10 | Cash passenger | Recorded as dues, no charge attempted |
| 06-11 | Driver penalty | Ledger deduction only; never billed |
| 06-12 | Dues at next checkout | Line appears with origin trip and date; total includes it |
| 06-13 | Dues with an outstanding balance | Booking still succeeds — dues never block |
| 06-14 | Dues settled on capture | Marked settled with the settling booking id |
| 06-15 | Compensation row | Kind `COMPENSATION`, not folded into fares |
| 06-16 | Dispute at 47 hours | Accepted |
| 06-17 | Dispute at 49 hours | `DISPUTE_WINDOW_CLOSED` |
| 06-18 | Dispute upheld | Charge reversed; ledger corrected by a compensating row |
| 06-19 | User disputes another user's penalty | 403 |
| 06-20 | Beneficiary payload | First name only; no contact detail |

## Manual checks

- Reconcile a full cycle by hand: fee charged, victim credited, platform share retained. The three must sum.
- Attempt to insert a beneficiary row breaking the sum constraint; confirm the database refuses.
- Read a penalty explanation as a support agent would; confirm it is sufficient to answer a complaint without database access.

## Evidence to collect

- `scripts/simulation/verify-penalties.sh` output with the arithmetic comparison.
- Ledger extract showing a negative penalty row and a positive compensation row.
- Dispute decision audit rows.

## Pass/fail criteria

Pass when: every prototype figure reproduces; splits always re-add; multi-victim distribution is exact
and deterministic; assessment is idempotent under concurrency; dues carry and settle without blocking;
and disputes respect the 48-hour window.

Fail on: any arithmetic disagreement with `data.jsx`, any double assessment, any dues blocking a booking,
or compensation summed into trip earnings.
