# QA — Task 08: Preferences, Verification and Eligibility

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/08-preferences-verification-and-eligibility.md`

## Scope

Driving preferences, women-only and verified-only eligibility enforced server-side, passenger
identity verification with camera-only capture, and profile photo visibility.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V034`.
- Test accounts: verified female rider, verified male rider, unverified rider, verified female driver, verified male driver.

## Automated test coverage

- `EligibilityServiceTest` — the full rider × trip matrix.
- `EligibilitySearchIT` — ineligible trips absent from search, not filtered client-side.
- `PhotoVisibilityMatrixTest` — PUBLIC/MATCHED/HIDDEN × viewer × booking state, including the driver asymmetry.
- `CameraOnlyEnforcementTest` — non-camera capture source refused at the schema level.
- `WomenOnlyGateTest` — set gate and book gate.
- `VerificationNeverBlocksBookingTest` — an unverified rider books an ordinary trip.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 08-1 | Unverified rider searches; a verified-only trip exists | Trip absent from results |
| 08-2 | Same rider books that trip by id | 403 `NOT_ELIGIBLE_VERIFIED_ONLY` |
| 08-3 | Male rider searches; a women-only trip exists | Trip absent |
| 08-4 | Male rider books it by id | 403 `NOT_ELIGIBLE_WOMEN_ONLY` |
| 08-5 | Verified female rider | Women-only trip visible and bookable |
| 08-6 | Unverified rider, ordinary trip | Books successfully — verification is never a gate |
| 08-7 | Male driver sets women-only | 403 `WOMEN_ONLY_NOT_AVAILABLE` |
| 08-8 | Unverified female driver sets women-only | Refused |
| 08-9 | Verified female driver sets women-only | Accepted |
| 08-10 | Upload with `captureSource=GALLERY` | Refused `CAPTURE_SOURCE_NOT_ALLOWED` |
| 08-11 | Upload without a session id | Refused |
| 08-12 | Session expired | `VERIFICATION_SESSION_EXPIRED` |
| 08-13 | Second submission while pending | `VERIFICATION_ALREADY_PENDING` |
| 08-14 | Verification approved | Level `VERIFIED`; gender written; badge on `/me/context` |
| 08-15 | Verification rejected on one step | Only that step marked; reason returned |
| 08-16 | Photo `HIDDEN`, driver views after confirmation | No photo URL emitted at all |
| 08-17 | Photo `MATCHED`, before confirmation | No URL |
| 08-18 | Photo `MATCHED`, after confirmation | URL returned to the confirmed driver only |
| 08-19 | Driver's own photo, confirmed rider | Always returned — the asymmetry |
| 08-20 | Driver's photo in search results | Never |
| 08-21 | Gender in any rider-facing payload | Never |
| 08-22 | NIC number in any payload | Never |
| 08-23 | Verified rider in a driver's request list | Sorted above unverified |

## Manual checks

- Attempt to read another user's NIC image via a presigned URL after expiry; confirm refusal.
- Confirm search omission does not leak the reason (a rider must not be able to enumerate a driver's policy).
- Confirm verification decisions are audited with per-step outcomes and the gender written.

## Evidence to collect

- `scripts/simulation/verify-eligibility.sh` output.
- Contract-test report for cases 08-21 and 08-22.
- Verification decision audit rows.

## Pass/fail criteria

Pass when: every ineligible trip is absent from search and refused at booking; verification never blocks
an ordinary booking; camera-only is enforced at the schema level; the photo visibility matrix including
the driver asymmetry is exact; and gender and NIC never leave the server.

Fail on: any eligibility decision made client-side, any NIC or gender leak, or any `HIDDEN` photo URL
appearing in a payload.
