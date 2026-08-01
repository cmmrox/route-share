# QA — Task 00: Repo Reset and Contract Rewrite

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/00-repo-reset-and-contract-rewrite.md`

## Scope

The move of `apps/passenger-mobile` to `apps/mobile`, deletion of `apps/driver-mobile`, the merged
`mobile-app.openapi.json`, and the new `GET /api/v1/me/context`. Out of scope: any new screen, any
behaviour change to an existing backend endpoint.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V026 (no new migration)`.


## Automated test coverage

- `AppContextServiceTest` — gate derivation for no-profile / pending / rejected / approved / suspended / deactivated.
- `AppContextControllerTest` — payload shape, own-data-only, zero defaults for not-yet-built fields.
- `packages/api-contracts` typecheck against the regenerated contract.
- `@redocly/cli lint` on the merged document.
- The carried-over mobile suite: `lint`, `typecheck`, `test`.

## Maestro automation

**Applicable.** Two existing flows are carried over and must be rerun after the move:

- `qa/maestro/mobile/regression/task07-home-search-route-discovery.yaml` — **update** (package/path rename), then run.
- `qa/maestro/mobile/regression/task08-results-list-map-filtering-ride-detail.yaml` — **update**, then run.

Run on a booted emulator against the live stack:

```bash
maestro test qa/maestro/mobile/regression/task07-home-search-route-discovery.yaml
```

Fix-rerun rule: any failure blocks task closure until fixed and the flow passes. A documented external
blocker (for example missing Google Maps keys) is the only permitted exception and must be recorded in
`docs/development/BLOCKERS.md`.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 00-1 | `apps/mobile` builds an Android debug APK after the move | Build succeeds; app launches to splash |
| 00-2 | Workspace references to `@routeshare/passenger-mobile` | None remain in `pnpm-workspace.yaml`, root scripts, `.detoxrc.json`, `eas.json` |
| 00-3 | `apps/driver-mobile` | Directory removed; no dangling references anywhere |
| 00-4 | Merged contract lints | `redocly lint` clean, zero errors |
| 00-5 | Screen coverage walk | Every screen id in `prototype-nav.jsx` maps to at least one contract path |
| 00-6 | `x-routeshare-status` present | Every path carries `IMPLEMENTED`, `PLANNED_SLICE_NN` or `CUT` |
| 00-7 | `/me/context` — no driver profile | `availableModes=[PASSENGER]`, gate `DRIVER_PROFILE_MISSING` |
| 00-8 | `/me/context` — driver pending | gate `DRIVER_REVIEW_PENDING`, `canPublish=false` |
| 00-9 | `/me/context` — driver approved | `availableModes` contains `DRIVER` |
| 00-10 | `/me/context` — suspended account | `account.status=SUSPENDED` with reason and case ref |
| 00-11 | `/me/context` — another user's id | No parameter accepted; only the caller's own data returned |
| 00-12 | Not-yet-built fields | `outstandingDues=0`, `rewardsBalance=0`, `verificationLevel=NONE` — safe defaults, not nulls |

## Manual checks

- Open the merged contract in Swagger UI and spot-check that passenger and driver schemas deduplicated cleanly (one `Booking`, not two).
- Confirm `docs/api/passenger-app.openapi.json` and `driver-app.openapi.json` are deleted and `admin-web.openapi.json` is untouched.
- Read `API_BACKEND_RECONCILIATION.md` end to end; it must be generated from `x-routeshare-status`, not hand-maintained.

## Evidence to collect

- Android debug APK build log.
- Both Maestro run outputs and screenshots.
- `redocly lint` output.
- The screen-coverage walk as a checklist artefact (screen id → contract path).

## Pass/fail criteria

Pass when: the moved app builds and both carried-over Maestro flows are green; the merged contract
lints clean and covers every screen; `/me/context` returns correct gates for all six account states and
leaks no other user's data.

Fail on: any dangling reference to the old app packages, any screen with no contract path, any
`/me/context` field returning null where a zero value is specified, or either Maestro flow red.
