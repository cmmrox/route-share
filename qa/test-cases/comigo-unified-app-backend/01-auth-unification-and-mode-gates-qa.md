# QA — Task 01: Auth Unification and Mode Gates

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/01-auth-unification-and-mode-gates.md`

## Scope

Real roles on phone-OTP tokens, the composite driver gate, role grant/revoke on approval and
deactivation, reinstatement requests, `last_active_mode`, and gate reasons as data. Out of scope: the
rate-band component of `canPublish` (slice 02) and the automatic deactivation trigger (slice 05).

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V027`.


## Automated test coverage

- `PhoneOtpRoleResolutionTest` — OTP token carries the account's real roles, not a hardcoded one.
- `DriverGuardTest` — the composite gate across every combination of role, profile status and deactivation.
- `DriverGateServiceTest` — all eight gate codes from the right conditions.
- `AppContextServiceTest` — gates surfaced on context.
- `RoleCacheInvalidationTest` — a revoked role stops working immediately.
- `SuspensionPrecedenceTest` — suspension outranks every driver gate.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 01-1 | OTP user, passenger only, calls a driver endpoint | 403 with `DRIVER_PROFILE_MISSING` |
| 01-2 | OTP user, driver approved, calls a driver endpoint | 200 — the core fix |
| 01-3 | Same token, same request, calls a passenger endpoint | 200 — both roles on one token |
| 01-4 | Driver pending review | 403 `DRIVER_REVIEW_PENDING` |
| 01-5 | Driver application rejected | 403 `DRIVER_APPLICATION_REJECTED` with the rejected document |
| 01-6 | Driver deactivated | Driver endpoints 403 `DRIVER_DEACTIVATED`; passenger endpoints 200 |
| 01-7 | Deactivated driver's pending payout | Unaffected and still visible |
| 01-8 | Account suspended | Both namespaces 403 `ACCOUNT_SUSPENDED`; driver gates not shown |
| 01-9 | Publish with a rejected KYC document | 403 `DOCUMENT_REJECTED` with the document named |
| 01-10 | Publish with no approved vehicle | 403 `VEHICLE_NOT_APPROVED` |
| 01-11 | Role revoked mid-session | Next request fails; cache does not serve the stale role |
| 01-12 | Token minted before deactivation | Rejected — the check runs per request, not at mint |
| 01-13 | Forged `X-Mode: DRIVER` header on a passenger-only account | Ignored; still 403 |
| 01-14 | `PUT /me/active-mode` to an unavailable mode | 409 with the blocking gate code |
| 01-15 | Reinstatement request, then a second while open | Second refused |
| 01-16 | Admin deactivate then reinstate | Realm role revoked then restored; both audited |

## Manual checks

- In Keycloak, confirm the `DRIVER` realm role is granted on approval and removed on deactivation for the same subject.
- Confirm every gate message is user-safe: no reviewer names, no internal notes, no document ids.
- Confirm `audit.audit_action` rows exist for each grant, revoke, deactivate and reinstate with actor and case ref.

## Evidence to collect

- `scripts/simulation/verify-mode-gates.sh` output.
- Keycloak role state before and after approval and deactivation.
- Audit table extract for the run.

## Pass/fail criteria

Pass when: one account reaches both namespaces on one token after approval; all eight gate codes fire
correctly; suspension outranks driver gates; deactivation leaves riding and payouts intact; and no
privilege-escalation case in 01-11 through 01-13 succeeds.

Fail on: any escalation case succeeding, any stale cached role being honoured, or any gate returning an
empty 403 body.
