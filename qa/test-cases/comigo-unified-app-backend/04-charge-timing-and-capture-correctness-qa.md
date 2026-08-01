# QA — Task 04: Charge Timing and Capture Correctness

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/04-charge-timing-and-capture-correctness.md`

## Scope

Authorise at booking, capture at trip start, void on every non-start path, cash handling,
partial capture on early drop-off, and idempotency across all of it. Out of scope: capture-on-accept for
live bookings (slice 12) and penalty charges (slice 06).

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V030`.


## Automated test coverage

- `PaymentIntentStateMachineTest` — guarded transitions including the new `PENDING`/`AUTHORIZED`.
- `CaptureOnTripStartIT` — Testcontainers; one start captures N bookings exactly once.
- `PaymentIdempotencyTest` — retried capture after a simulated gateway timeout reconciles rather than recharges.
- `VoidOnCancelTest` — cancel, decline, route cancel and auto-cancel all void.
- `CashCommissionTest` — cash creates no intent and records commission owed.
- `PartialCaptureIT` — early drop-off captures the repriced amount.
- `ConcurrentStartCaptureIT` — duplicate concurrent starts produce one capture per booking.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 04-1 | Card booking created | Intent `AUTHORIZED`, nothing captured |
| 04-2 | Driver approves | Still `AUTHORIZED` — approval never charges |
| 04-3 | Trip start with 3 card bookings | 3 captures, exactly once each |
| 04-4 | Trip start called twice | No further captures |
| 04-5 | Two concurrent start calls | Exactly one capture per booking |
| 04-6 | Capture attempted before start (direct service call) | Refused `CAPTURE_NOT_PERMITTED_BEFORE_START` |
| 04-7 | Passenger cancels before start | Void; charged 0 |
| 04-8 | Driver declines | Void |
| 04-9 | Driver cancels the occurrence | Every booking voided |
| 04-10 | One card declines at start | That booking flagged `PAYMENT_FAILED`; trip still starts; others captured |
| 04-11 | Gateway timeout during capture | Attempt row exists; reconciliation finds it; no double charge on retry |
| 04-12 | Cash booking | No intent created; `payment_method=CASH` |
| 04-13 | Cash collected | `COMMISSION_OWED_CASH` ledger row written |
| 04-14 | Early drop-off before capture | Captures the lower repriced amount |
| 04-15 | Early drop-off after capture | Refunds the difference |
| 04-16 | Stuck authorisation older than the validity window | Appears in `/admin/payments/reconciliation` |
| 04-17 | Passenger response content | No gateway reference, no raw failure detail |

## Manual checks

- Grep the full log output of a capture run for PAN, CVV or token fragments. Any hit is a fail.
- Confirm `payment.payment_attempt` rows are written **before** the gateway call, not after.
- Confirm the reconciliation alert fires with a deliberately stranded authorisation.

## Automated test coverage status (2026-08-02)

Green under `./mvnw spotless:check verify` (344 tests). `PaymentIntentStateMachineTest` pins the
transitions that must be impossible — capture before authorise, capture twice, void after capture.
`PaymentFacadeImplTest` covers authorise-on-booking, cash creating no intent, start capturing every
booking once, a retried start capturing nothing further, a duplicate call stopped by the idempotency
key, a declined card flagging only its own booking, void on cancel, and both early-drop-off paths.

**Two gaps, both Blocker 013.** `CaptureOnTripStartIT` is **not written**: the property it would
prove is the unique index on `payment_attempt.idempotency_key` under real concurrency, and there is
no database here to hold it. `scripts/simulation/verify-charge-timing.sh` exists and is
syntax-checked but has never run. Until both are done, "exactly once under concurrency" is designed
for and unit-tested, not demonstrated.

## Evidence to collect

- `scripts/simulation/verify-charge-timing.sh` output.
- Ledger and intent extracts for a full booking → start → complete cycle.
- Log scrub confirmation for the capture run.

## Pass/fail criteria

Pass when: nothing is captured before trip start; a start captures every card exactly once even under
duplicate and concurrent calls; every non-start path voids and charges zero; a failed capture never stops
a trip; and a gateway timeout reconciles without double charging.

Fail on: any double capture, any pre-start capture, any orphaned authorisation with no attempt row, or
any secret in the logs.
