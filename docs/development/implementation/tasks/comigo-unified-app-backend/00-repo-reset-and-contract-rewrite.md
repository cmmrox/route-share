---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 00 — Repo Reset and Contract Rewrite

**Goal:** Collapse the two-app repository into one mobile app, and replace the two client contracts with a single mode-aware `mobile-app.openapi.json` that can describe all ~157 ComiGo screens.

**Depends on:** nothing. This is the first slice.
**Blocks:** every other slice — they all consume this contract.

## Objective

Everything downstream is built against the API contract, so the contract must be correct before a single endpoint is written. This task produces the target contract and the app skeleton that will consume it, and removes the structural assumption that passenger and driver are separate products.

## Scope

In scope:

- Harvest `apps/passenger-mobile` into a new `apps/mobile` (decision D4): design system, `src/api` client, `features/auth`, `features/profile`, `app.config.ts`, native Android project, Maestro harness, test setup.
- Delete the `apps/driver-mobile` stub.
- Merge `docs/api/passenger-app.openapi.json` + `driver-app.openapi.json` into `docs/api/mobile-app.openapi.json`.
- Extend that contract to cover every screen in the gap analysis, including endpoints not yet implemented — this is a contract-first document, and unimplemented paths are marked as such.
- Add `GET /api/v1/me/context` to the contract and implement it (the only backend code in this slice).
- Regenerate `packages/api-contracts`.
- Rewrite `docs/api/API_BACKEND_RECONCILIATION.md` against the merged contract.

Out of scope:

- Any screen implementation in `apps/mobile` beyond what is being carried over intact.
- Any behaviour change to existing backend endpoints. Renaming or reshaping existing responses happens in the slice that owns that domain.

## Source material / references

- `docs/source-assets/comigo-prototype/` — 28 decoded JSX modules, the specification of record. `prototype-nav.jsx` is the full screen registry; `data.jsx` is the policy and payload shape.
- `00-prototype-gap-analysis.md` — the per-screen capability register this contract must satisfy.
- `docs/api/passenger-app.openapi.json`, `docs/api/driver-app.openapi.json`, `docs/api/admin-web.openapi.json`.
- `packages/api-contracts/src/index.ts` (676 lines, 51 passenger paths currently inventoried).
- Live `/api-docs` from the running API, and the actual controller DTOs under `apps/api/src/main/java/com/routeshare/**/controller/`.
- `docs/development/IMPLEMENTATION_PLANNING_STANDARD.md` §"API and backend planning rule".

## Architecture and design notes

**Path namespaces stay.** `/api/v1/passenger/**` and `/api/v1/driver/**` are role-scoped resource namespaces, not app boundaries. One app calling both is normal; renaming them would be pure churn across 70+ endpoints and would lose the authorization signal the path currently carries.

**Mode is a client concept.** The server never receives "I am in driver mode". It authorizes on role plus resource ownership plus gate state. The tab set and home surface swap on the client; the token does not change.

**`/me/context` is the shell's single read.** S07–S14 collectively need: available modes, driver status and why it is gated, verification level, suspension state, an active-trip pointer for the resume bar, outstanding dues, rewards balance, and tab badge counts. Served separately that is 8+ calls on every cold start and every mode switch. One endpoint, cacheable per user for a few seconds, replaces all of it.

**Unimplemented paths are declared, not hidden.** Every path the prototype needs goes into the contract now with an `x-routeshare-status` extension of `IMPLEMENTED`, `PLANNED_SLICE_NN`, or `CUT`. The reconciliation doc is generated from that field, so contract drift becomes visible instead of discovered during mobile work.

## API contracts involved

Merged file: `docs/api/mobile-app.openapi.json`.

New endpoint implemented in this slice:

```
GET /api/v1/me/context
```

Response shape (`AppContextResponse`):

| Field | Type | Source |
| --- | --- | --- |
| `subject`, `displayName`, `phone`, `photoUrl` | string | `identity.app_user`, `passenger_profile` |
| `availableModes` | `["PASSENGER","DRIVER","ADMIN"]` | existing `AuthMeService` logic |
| `activeModeDefault` | enum | last used, else `PASSENGER` |
| `driver.status` | `NONE\|DRAFT\|PENDING_REVIEW\|APPROVED\|REJECTED\|SUSPENDED\|DEACTIVATED` | `driver_profile.verification_status` |
| `driver.gates[]` | `{code, message, actionPath}` | derived — drives S08, S09, S12 |
| `driver.canPublish` | boolean | KYC complete **and** ≥1 approved vehicle **and** that vehicle has a rate band (slice 02) |
| `passenger.verificationLevel` | `NONE\|PENDING\|VERIFIED\|REJECTED` | slice 08 fills this; `NONE` until then |
| `account.status` | `ACTIVE\|SUSPENDED` + `reason`, `caseRef`, `suspendedAt` | `app_user_status_history` — drives S13 |
| `activeTrip` | `{kind: RIDING\|DRIVING, bookingId?, tripId?, etaMinutes?, label}` \| null | drives P01b, D08b, S10 |
| `money.outstandingDues` | amount | slice 06; `0` until then |
| `money.rewardsBalance` | amount | slice 11; `0` until then |
| `badges` | `{home: bool, trips: int, inbox: int, account: bool}` | per the S14 board rules |

Fields owned by later slices are present in the contract from day one and return safe zero values until
their slice lands. That keeps the mobile shell stable instead of reshaping its context object 6 times.

Contract additions declared but **not** implemented here — full path list with `PLANNED_SLICE_NN` markers
covering: rate bands (02), fare quote v2 (03), penalties/dues (06), seat selection + approval mode (07),
driving preferences + passenger verification (08), search v2 (09), chat + settings (10), referral/rewards
(11), live booking (12), payouts/adjustments (13), ratings v2 (14).

## Database / migration changes

None. `/me/context` is a read across existing tables.

Note for slice 01: `identity.app_user` has no `last_active_mode` column. `activeModeDefault` returns
`PASSENGER` for everyone in this slice; persisting the preference is picked up in slice 01 alongside the
auth changes, since both touch the same table.

## Configuration / environment changes

- `apps/mobile/app.config.ts` carries over from `apps/passenger-mobile` with the app name/slug/scheme changed to ComiGo. Google Maps keys, API base URL and Sentry DSN wiring are unchanged.
- `pnpm-workspace.yaml` — replace the two app entries with `apps/mobile`.
- Root `package.json` scripts referencing `@routeshare/passenger-mobile` are renamed to `@routeshare/mobile`.
- No new secrets.

## UI / UX requirements

No new screens. The carried-over app must still build and run: splash, onboarding, login, OTP, profile
setup, home, search, results, ride detail, saved places, account, verification, trusted contacts continue
to work against the same endpoints, under the new package name.

Screens will be rebuilt to the ComiGo prototype in the mobile plan, which is a separate feature folder.
This slice only guarantees the app is not broken by the move.

## Implementation steps

1. `git mv apps/passenger-mobile apps/mobile`; rename the package to `@routeshare/mobile`; update `pnpm-workspace.yaml`, root scripts, `.detoxrc.json`, `eas.json`, `vitest.config.ts`, `tsconfig.json` paths.
2. Delete `apps/driver-mobile`. Record in `DECISION_LOG.md` that driver-mobile never had content beyond a README.
3. Rename `qa/maestro/passenger-mobile/` → `qa/maestro/mobile/` and `qa/test-cases/07-passenger-mobile-app/` references accordingly; keep the existing task07/task08 regression flows runnable.
4. Verify the moved app still passes `lint | typecheck | test` and still builds an Android debug APK.
5. Merge the two OpenAPI documents. Where passenger and driver define the same schema (booking, trip, vehicle, notification), keep one component and reference it from both. Resolve name collisions by module prefix.
6. Walk `prototype-nav.jsx` screen by screen. For each screen, list the data it renders and assert a contract path + field supplies it. Record misses.
7. Add every missing path to the merged contract with `x-routeshare-status: PLANNED_SLICE_NN`, using the gap analysis to assign slices.
8. Add `AppContextResponse` and `GET /api/v1/me/context` with `x-routeshare-status: IMPLEMENTED`.
9. Implement the endpoint: new `platform/controller/AppContextController` + `platform/service/AppContextService(+Impl)`, composing existing facades (`PassengerFacade`, `DriverFacade`, `BookingFacade`, `TripService`, `NotificationFacade`). No new repositories — everything goes through facades per the modular-monolith rules.
10. Extend `AuthMeService` logic into the new service rather than duplicating it; keep `/auth/me` working for backward compatibility and mark it deprecated in the contract.
11. Regenerate `packages/api-contracts/src/index.ts` from the merged document; ensure `typecheck` passes.
12. Rewrite `docs/api/API_BACKEND_RECONCILIATION.md` grouped by slice, generated from `x-routeshare-status`.
13. Delete `docs/api/passenger-app.openapi.json` and `driver-app.openapi.json`; leave `admin-web.openapi.json` untouched.

## Files expected to change

- `apps/mobile/**` (moved from `apps/passenger-mobile/**`), `apps/driver-mobile/` (deleted).
- `apps/api/src/main/java/com/routeshare/platform/**` — new context controller/service.
- `apps/api/src/test/java/com/routeshare/platform/**` — context tests.
- `docs/api/mobile-app.openapi.json` (new), `passenger-app.openapi.json` + `driver-app.openapi.json` (deleted), `API_BACKEND_RECONCILIATION.md` (rewritten).
- `packages/api-contracts/src/index.ts`.
- `pnpm-workspace.yaml`, root `package.json`, `qa/maestro/mobile/**`.
- `docs/development/DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `DECISION_LOG.md`, `TASK_LOG.md`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/00-repo-reset-and-contract-rewrite-qa.md`

Maestro: the carried-over flows must still pass after the move —
`qa/maestro/mobile/regression/task07-home-search-route-discovery.yaml` and
`qa/maestro/mobile/regression/task08-results-list-map-filtering-ride-detail.yaml`.
Both already exist; they are **updated** (paths/package name) and **rerun**, not created.
Any failure blocks task closure until fixed and green.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
pnpm --filter @routeshare/api-contracts typecheck && pnpm --filter @routeshare/mobile lint && pnpm --filter @routeshare/mobile typecheck && pnpm --filter @routeshare/mobile test
```

```bash
npx @redocly/cli lint docs/api/mobile-app.openapi.json
```

## Security, privacy, and observability checks

- `/me/context` returns only the caller's own data; no id is accepted as a parameter.
- Suspension reason and case ref are the operator-facing text already stored — confirm no internal notes leak into it.
- No phone numbers of other users appear in this payload. Counterparty disclosure is slice 07 and follows §6.1 of the plan.
- Log a single structured line per context read with user id and computed gate codes; never log the payload.
- Metric `routeshare_app_context_reads_total` tagged by `driverStatus` and `accountStatus` — this is the earliest signal that gating logic is wrong at scale.

## Done criteria

- [ ] `apps/mobile` builds, lints, typechecks, tests, and produces an Android debug APK.
- [ ] `apps/driver-mobile` removed; no dangling references in workspace config or docs.
- [ ] `mobile-app.openapi.json` lints clean and every screen in `prototype-nav.jsx` has a mapped path.
- [ ] Every contract path carries an `x-routeshare-status` value.
- [ ] `GET /api/v1/me/context` implemented, tested, and returning correct gates for: no driver profile, pending, rejected, approved, suspended, deactivated.
- [ ] `packages/api-contracts` regenerated and typechecking.
- [ ] `API_BACKEND_RECONCILIATION.md` rewritten.
- [ ] Both carried-over Maestro regressions rerun green.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(mobile,api): unify the two apps into apps/mobile and rewrite the client contract"
```

## Implementation result — 2026-08-01

Status: `COMPLETE_CORE_VERIFIED_DEVICE_QA_DEFERRED`
Branch: `feat/comigo-unified-app-slice-00`

Completed:

- [x] `apps/passenger-mobile` → `apps/mobile` (`@routeshare/mobile`); design system, API client, auth and profile features, native project and Maestro harness carried over intact.
- [x] `apps/driver-mobile` deleted (README-only stub).
- [x] `qa/maestro/passenger-mobile` → `qa/maestro/mobile`; `scripts/qa-passenger-*.sh` → `scripts/qa-mobile-*.sh`; workspace and root scripts updated.
- [x] App display name is now **ComiGo**; iOS usage strings rewritten for the ComiGo product.
- [x] `mobile-app.openapi.json` created — **186 paths, 221 operations, 85 schemas, lints clean (0 errors, 0 warnings)**.
- [x] Every operation stamped `x-routeshare-status`: 120 `IMPLEMENTED`, 91 `PLANNED_SLICE_NN`, 7 `INTERNAL_NOT_FOR_CLIENTS`, 3 `CUT`.
- [x] **All 157 prototype screens map to at least one contract operation**, verified programmatically against `prototype-nav.jsx`.
- [x] `GET /api/v1/me/context` implemented — controller, service, DTO, 15 tests.
- [x] `IdentityFacade` extended with `upsertFromTokenAllowingSuspended` and `latestStatusChange`.
- [x] `packages/api-contracts` regenerated with typed `EndpointStatus`; `apps/mobile/src/api/contracts.ts` and its tests updated.
- [x] `API_BACKEND_RECONCILIATION.md` regenerated from `x-routeshare-status`.
- [x] `passenger-app.openapi.json`, `driver-app.openapi.json` and `PASSENGER_MOBILE_CONTRACT_RECONCILIATION.md` deleted; `docs/api/README.md` rewritten.

Defects found by the merge (all fixed):

1. `SosRequest.tripId` was **uuid** in the driver contract and **int64** in the passenger one; the column is `BIGINT`. A generated driver client would have failed at runtime.
2. **22 implemented endpoints appeared in neither contract**, including the whole Places/directions surface the search screens depend on and the presigned document-upload lifecycle.
3. Two contract endpoints were never implemented (direct document uploads, superseded by the presigned lifecycle) — marked `CUT`.
4. **17 uses of `nullable`** — OpenAPI 3.0 syntax, invalid in the 3.1 documents both files declared, silently ignored by 3.1 tooling.
5. Path parameter names disagree between contract and controllers (`{savedPlaceId}` vs `{id}`). Harmless at runtime; contract names kept as the better ones.

Verification:

- Backend `spotless:check verify` → **BUILD SUCCESS, 218 tests, JaCoCo 80% gate met**.
- Mobile `lint | typecheck | test` → **18 files / 86 tests**.
- `@routeshare/api-contracts` typecheck → green.
- `redocly lint docs/api/mobile-app.openapi.json` → **zero errors, zero warnings**.

**Build environment note:** the API targets **Java 21**. A JDK 17 default fails with a Lombok
`TypeTag :: UNKNOWN` error, which does not name the real cause. Use
`JAVA_HOME=~/.sdkman/candidates/java/21.0.9-amzn`.

Deferred, with reasons:

- [ ] **Maestro device reruns** — `task07-home-search-route-discovery.yaml` and `task08-results-list-map-filtering-ride-detail.yaml` were moved and their references updated, but rerunning needs a booted emulator, the live Docker stack and Google Maps keys. No code path they cover was changed by this slice; the app builds, typechecks and passes its unit suite. Run before closing the slice for release.
- [ ] **Keycloak client consolidation** — the realm still carries separate `passenger-mobile` and `driver-mobile` public clients, a two-app assumption. Slice 01 owns auth and should replace them with one `comigo-mobile` client. Left untouched here to avoid breaking the working dev auth flow mid-move.
- [ ] **Native identity rename** (slug, scheme, bundle identifier, android package) — requires regenerating the committed native projects, which is blocked on the prebuild-vs-native-owned decision in Blocker 009. Pinned by test so the change is deliberate when it happens. Phase 09 work.

Raised: **Blocker 012** — `admin-web.openapi.json` has the same `nullable`/3.1 defect (8 struct errors),
pre-existing since `0bd5fdf` and out of this slice's scope.
