---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 01 — Auth Unification and Mode Gates

**Goal:** Let one signed-in account act as both passenger and driver on a single token, and make every access decision explain itself as data the app can render.

**Depends on:** 00.
**Blocks:** 02–15. No driver endpoint is reachable by a phone-OTP user until this lands.

## Objective

`common/security/PhoneOtpAccessTokenAuthenticationFilter` grants a hardcoded `ROLE_PASSENGER` to every
phone-OTP session, and all ten driver endpoints are `@PreAuthorize("hasRole('DRIVER')")`. In the two-app
world that was fine. In one app it means **a phone-OTP user can never drive.** This task fixes that, and
replaces the bare role check with a composite gate that distinguishes "you are not a driver", "we are
still reviewing you", "fix a document", "you are deactivated", and "your account is suspended" — five
states the prototype renders as five different screens (S07, S08, S09, D34, S13).

## Scope

In scope:

- Real roles on phone-OTP tokens, sourced from `identity.app_user`.
- Realm-role grant on driver approval, revoke on deactivation.
- A composite driver gate: role **and** approved profile **and** not deactivated.
- A separate publishing gate: KYC complete **and** an approved vehicle **and** (from slice 02) a rate band.
- Suspension blocks both modes; driver deactivation blocks only driver endpoints.
- Gate reasons surfaced as data on `/me/context`, and as typed error bodies on 403.
- `last_active_mode` persisted so the app reopens where the user left off.

Out of scope:

- The rate-band component of the publishing gate — stubbed to `true` here, wired in slice 02.
- Passenger verification level — stubbed to `NONE` here, wired in slice 08.
- Driver deactivation *triggering* (3 missed starts) — that is slice 05. This task builds the gate and the
  manual admin path; the automatic trigger calls into it later.

## Source material / references

- `docs/source-assets/comigo-prototype/shell-modes.jsx` — S07 (no chip), S08 (pending), S09 (rejected), S10/S11 (conflicts), S12 (publish gate), S13 (suspended).
- `docs/source-assets/comigo-prototype/driver-money.jsx` — D34 driver deactivated.
- `docs/source-assets/comigo-prototype/shell.jsx` — the mode chip states `approved | pending | rejected | none`.
- Current code: `common/security/*`, `identity/service/impl/AuthMeServiceImpl.java`, `identity/keycloak/KeycloakRealmRoleAdapter.java`, `admin/service/impl/AdminUserServiceImpl.java`, `driver/service/impl/DriverVerificationService`.

## Architecture and design notes

**Two token issuers, one authority model.** Keycloak JWTs already carry realm roles through
`KeycloakJwtRoleConverter`. The phone-OTP path mints its own token. Both must end up with the same
authorities for the same user, so the OTP filter stops inventing a role and instead reads the account's
roles from `identity.app_user` (projection-cached, as `IdentityFacade.upsertFromToken` already is).

**Gates are data, not exceptions.** A 403 with an empty body forces the client to guess which of five
screens to show. Every gated response carries `{code, message, actionPath}`, and the same structure
appears pre-emptively in `/me/context.driver.gates[]` so the app can render S08/S09/S12 *before* the user
taps something that fails.

**Gate codes** (stable, contract-visible):

| Code | Meaning | Screen |
| --- | --- | --- |
| `DRIVER_PROFILE_MISSING` | never applied | S07 |
| `DRIVER_REVIEW_PENDING` | application under review | S08 |
| `DRIVER_APPLICATION_REJECTED` | a KYC doc was rejected | S09 |
| `DRIVER_DEACTIVATED` | reliability deactivation, admin reinstatement required | D34 |
| `ACCOUNT_SUSPENDED` | blocks both modes | S13 |
| `DOCUMENT_REJECTED` / `DOCUMENT_MISSING` / `DOCUMENT_EXPIRED` | publish blocked, per document | S12 |
| `VEHICLE_NOT_APPROVED` | no approved vehicle | S12 |
| `RATE_BAND_NOT_SET` | approved vehicle with no band (slice 02) | D40 |

**Suspension vs deactivation are different.** D34 is explicit: a deactivated driver "can still ride as a
passenger — nothing about your rider account changes", and money already earned still pays out. S13
suspension stops everything. Conflating them would either strand earnings or over-punish.

**Mode conflicts (S10, S11) are client behaviour, not server gates.** The server does not block switching
mode mid-trip; it exposes `activeTrip` on `/me/context` and the app renders the confirmation sheet. The
notification suppression in S11 is slice 10.

## API contracts involved

- `GET /api/v1/me/context` — extended: real `driver.gates[]`, `driver.canPublish`, `account.status` with reason/caseRef, `activeModeDefault` from the persisted value.
- `PUT /api/v1/me/active-mode` — new. Body `{mode: PASSENGER|DRIVER}`. Persists `last_active_mode`. 409 with `DRIVER_*` gate code if the mode is not available.
- `POST /api/v1/driver/reinstatement-requests` — new. Body `{message}`. Creates a support ticket of type `DRIVER_REINSTATEMENT` and records the request. Serves D34's primary action.
- `GET /api/v1/driver/reinstatement-requests` — new. Caller's own requests + status.
- `POST /api/v1/admin/drivers/{driverProfileId}/deactivate` — new. Body `{reason, caseRef}`. Revokes the realm role.
- `POST /api/v1/admin/drivers/{driverProfileId}/reinstate` — new. Restores the role, clears the deactivation, audits.
- Error envelope extended: all 403s from gated endpoints return `{error: {code, message, actionPath}}`.
- `GET /api/v1/auth/me` — marked deprecated in the contract; unchanged behaviour.

## Database / migration changes

**`V027__auth_unification_and_driver_gates.sql`**

- `identity.app_user` — add `last_active_mode TEXT CHECK (last_active_mode IN ('PASSENGER','DRIVER'))`, nullable.
- `identity.app_user_status_history` — add `case_ref TEXT`, `reason TEXT` if not already present in a usable shape (S13 shows "Case #SL-40912" and a reason paragraph).
- New `driver.driver_deactivation`:
  `driver_deactivation_id`, `driver_profile_id FK`, `reason TEXT NOT NULL`, `case_ref TEXT NOT NULL`,
  `deactivated_at`, `deactivated_by_app_user_id`, `reinstated_at`, `reinstated_by_app_user_id`,
  partial unique index on `driver_profile_id WHERE reinstated_at IS NULL` (one active deactivation only).
- New `driver.driver_reinstatement_request`:
  `id`, `driver_profile_id FK`, `deactivation_id FK`, `message TEXT`, `status TEXT CHECK (status IN ('OPEN','APPROVED','REJECTED'))`, `created_at`, `decided_at`, `decided_by_app_user_id`, `decision_note`.
- Index `idx_driver_deactivation_active ON driver.driver_deactivation(driver_profile_id) WHERE reinstated_at IS NULL`.

No backfill: unreleased app (decision D6).

## Configuration / environment changes

None. Keycloak realm roles already exist in `infra/keycloak/import`; confirm `DRIVER` is present and
assignable by the admin client used by `KeycloakRealmRoleAdapter`.

## UI / UX requirements

Backend slice. The contract must let the app render, without extra calls:

- S07 — no mode chip, two become-a-driver entry points.
- S08 — "in review since …", document count.
- S09 — which document was rejected and the reviewer's reason.
- S12 — the blocker list with per-document action labels.
- S13 — reason, case ref, suspended-at, appeal action.
- D34 — the deactivation reason, that riding still works, that earnings still pay out, and the reinstatement action.

## Implementation steps

1. Add `AccountRoleService` in `identity` returning the effective role set for an `app_user_id` (Keycloak realm roles ∪ derived roles from profile existence).
2. Rewrite `PhoneOtpAccessTokenAuthenticationFilter` to resolve authorities through that service instead of the hardcoded list. Cache with the existing identity projection cache; invalidate on role change.
3. Add `driver.deactivation` + `reinstatement_request` entities, repositories, and `DriverGateService` in the `driver` module computing the gate list for a user.
4. Expose `DriverFacade.gatesFor(appUserId)` and `DriverFacade.isDeactivated(appUserId)`; no cross-module repository access.
5. Create the composite authorization: a `@DriverAccess` meta-annotation (`@PreAuthorize("@driverGuard.canDrive(authentication)")`) and `@DriverPublishAccess` (`@driverGuard.canPublish(...)`). Replace all ten `hasRole('DRIVER')` usages.
6. Implement a `GateDeniedException` → 403 handler emitting `{code, message, actionPath}` through the existing error advice.
7. Grant the `DRIVER` realm role from the driver-approval path in `AdminDriverReviewController`; revoke it on deactivation. Both audited via the existing `audit.audit_action`.
8. Add admin deactivate/reinstate endpoints and the driver-side reinstatement request endpoints.
9. Add `PUT /api/v1/me/active-mode` and persist `last_active_mode`.
10. Extend `AppContextService` from slice 00 with the real gate list, `canPublish`, and account suspension detail.
11. Ensure suspension (existing `AppUserRepository.upsertFromToken` enforcement) short-circuits before driver gates, so a suspended driver sees S13 and not S08.

## Files expected to change

- `apps/api/.../common/security/**` — OTP filter, new `DriverGuard`, gate annotations, error advice.
- `apps/api/.../identity/**` — `AccountRoleService`, `last_active_mode`, cache invalidation.
- `apps/api/.../driver/**` — deactivation + reinstatement entities/repos/service/facade/controllers, `DriverGateService`.
- `apps/api/.../admin/**` — deactivate/reinstate endpoints + audit.
- `apps/api/.../platform/**` — `AppContextService` extension.
- `apps/api/src/main/resources/db/migration/V027__auth_unification_and_driver_gates.sql`.
- `apps/api/src/test/java/**` — guard tests, gate-derivation tests, OTP-token role tests.
- `docs/api/mobile-app.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/01-auth-unification-and-mode-gates-qa.md`

Maestro: not applicable — no mobile surface changes in this slice. Backend runtime smoke is required
instead (see verification commands). The mobile gate screens are exercised in the mobile feature plan.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest=DriverGuardTest,PhoneOtpRoleResolutionTest,DriverGateServiceTest,AppContextServiceTest test
```

Runtime smoke against the local stack — one OTP account must reach both a passenger and a driver endpoint
after approval, and be refused with the right code before it:

```bash
bash scripts/simulation/verify-mode-gates.sh
```

## Security, privacy, and observability checks

- Privilege escalation is the risk in this slice. Assert in tests that a passenger-only account cannot reach a driver endpoint by any of: forged mode header, direct path call, or a stale token minted before deactivation.
- Role changes must invalidate the identity projection cache immediately — a revoked driver holding a cached role set is a live authorization hole.
- OTP tokens must not outlive a suspension. Verify the suspension check runs on every request, not only at token mint.
- Gate messages are user-facing text; they must not contain internal reviewer notes or admin identities.
- Metrics: `routeshare_gate_denied_total{code}` and `routeshare_role_grant_total{role,action}`. A spike in `DRIVER_DEACTIVATED` denials is the first sign slice 05's trigger is misfiring.
- Audit every grant, revoke, deactivate and reinstate with actor, subject, reason and case ref.

## Done criteria

- [ ] A phone-OTP account with an approved driver profile can call both passenger and driver endpoints on one token.
- [ ] All ten former `hasRole('DRIVER')` sites use the composite guard.
- [ ] All eight gate codes are produced by the right conditions and returned both on `/me/context` and on 403.
- [ ] Suspension outranks every driver gate.
- [ ] Deactivation blocks driving, leaves riding and payouts untouched.
- [ ] Admin deactivate/reinstate and driver reinstatement request work end to end and are audited.
- [ ] `last_active_mode` persists and is returned.
- [ ] Cache invalidation on role change is proven by test.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): unify passenger/driver auth on one token with explainable mode gates"
```
