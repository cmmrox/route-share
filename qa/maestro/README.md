# Maestro mobile automation

This folder contains committed, executable Maestro E2E flows. Generated output from running these flows belongs in ignored `qa/reports/`, not beside the flow files.

Every mobile implementation task must either create/update a task-mapped Maestro flow or document a temporary no-runnable-surface exception in both the development task and QA test-case file. Do not mark a mobile task complete with only unit tests, typecheck, or manual screenshots when Maestro automation can exercise the feature.

## Structure

```text
qa/maestro/
  passenger-mobile/
    smoke/        # fast critical path: launch, auth, profile, basic app health
    regression/   # feature journeys mapped to `qa/test-cases/07-passenger-mobile-app/`
    release/      # release-candidate verification journeys
```

## Task mapping

Use task-numbered regression names for mobile feature slices:

```text
qa/maestro/passenger-mobile/regression/taskNN-<task-slug>.yaml
```

Use smoke flows only for critical paths that intentionally cover multiple tasks:

```text
qa/maestro/passenger-mobile/smoke/<critical-path-name>-smoke.yaml
```

When a flow is shared, every covered task file and QA case must link the shared YAML path and state which task behavior it verifies.

Task completion requires a fix-rerun loop: run the flow on emulator/device, fix every app/test issue discovered, rerun until pass, and store generated evidence under ignored `qa/reports/<timestamp>/`. If an emulator, provider credential, native build, or device issue prevents execution, record the blocker in `docs/development/BLOCKERS.md` and summarize the closest valid evidence in `docs/development/TASK_LOG.md`.

## Current flows

```bash
maestro --device emulator-5554 test qa/maestro/passenger-mobile/smoke/auth-profile-smoke.yaml
maestro --device emulator-5554 test qa/maestro/passenger-mobile/smoke/home-search-route-discovery-smoke.yaml
maestro --device emulator-5554 test qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml
```

Preferred wrapper, because it configures adb reverse ports and saves screenshots/JUnit/log output under ignored `qa/reports/<timestamp>/`:

```bash
scripts/qa-passenger-android.sh
```

Build/install the debug passenger app first, then run the smoke flow:

```bash
scripts/qa-passenger-dev-run.sh
```

## Seeded inventory and cost-control verification

The Task 08 regression needs a searchable route in the departure window. Seed and verify with the
local simulation helpers (see `scripts/simulation/README.md`):

```bash
scripts/simulation/seed-demo-route.sh 40        # Colombo Fort -> Nugegoda, departs now+40 min
scripts/simulation/verify-cost-controls.sh      # session tokens, Redis caches, geometry, 429 limit
```

Environment notes for the emulator suite:

- Build the debug dev client with `-PreactNativeDevServerPort=8082` (Metro runs on host 8082).
- Start Metro with `EXPO_PUBLIC_API_BASE_URL=http://<mac-LAN-IP>:8080` — the emulator's
  `10.0.2.2:8080` may be intercepted by unrelated local containers bound to host `127.0.0.1:8080`.
- Run `verify-cost-controls.sh` after manual search QA (its rate-limit burst exhausts the sim
  passenger's autocomplete quota for one minute).
