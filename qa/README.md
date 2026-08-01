# RouteShare QA

This folder owns QA planning, repeatable test cases, executable automation flows, and local/generated QA evidence.

## Folder policy

Committed:

- `qa/test-cases/<feature-plan-name>/` — QA plans and task-level test cases that match development implementation tasks.
- `qa/maestro/<app>/<suite>/*.yaml` — repeatable executable Maestro automation flows.
- `qa/README.md` and focused README files — QA organization and execution policy.

Not committed:

- `qa/reports/` — generated screenshots, logs, XML reports, UI dumps, and run evidence.
- `qa/runs/` — local daily run notes or temporary QA summaries.
- `artifacts/` — disposable generated PDFs, logs, exports, and one-off reports.

Important development status from QA should be summarized in shared development docs such as `docs/development/DEVELOPMENT_STATUS.md`, `docs/development/TASK_LOG.md`, or `docs/development/BLOCKERS.md`; do not commit daily running logs.

## Automation structure

Executable mobile E2E automation lives under `qa/maestro/`, separated by app and suite:

```text
qa/maestro/
  mobile/
    smoke/        # fast critical-path checks for every mobile QA pass
    regression/   # broader feature journeys as screens stabilize
    release/      # release-candidate store-readiness journeys
```

Current executable flows:

```text
qa/maestro/mobile/smoke/auth-profile-smoke.yaml
qa/maestro/mobile/smoke/home-search-route-discovery-smoke.yaml
qa/maestro/mobile/regression/task07-home-search-route-discovery.yaml
```

Manual/functional QA specifications remain in `qa/test-cases/`. When a mobile task has a runnable screen, navigation path, native permission, provider-backed flow, or release-pipeline behavior, add or update the executable `.yaml` flow under the matching `qa/maestro/mobile/<suite>/` folder and link it from the test-case document.

## Mobile task Maestro rule

Every mobile implementation task must have task-mapped Maestro automation unless the task has no runnable mobile surface yet. The implementation task file and the matching QA test-case file must both name the required YAML path.

Use these path conventions:

```text
qa/maestro/mobile/smoke/<critical-path-name>-smoke.yaml
qa/maestro/mobile/regression/taskNN-<task-slug>.yaml
qa/maestro/mobile/release/taskNN-<task-slug>-release.yaml
```

The flow can be a shared smoke only when the task explicitly links that smoke flow and states what behavior it covers. Otherwise create or update a task-specific regression flow.

A mobile task is not finished until:

- the relevant Maestro YAML exists or is updated;
- it runs on emulator/device through `scripts/qa-passenger-android.sh` or the documented platform command;
- failures found by Maestro are fixed;
- the flow is rerun until it passes, or a concrete external blocker is recorded in `docs/development/BLOCKERS.md`;
- generated evidence is saved under ignored `qa/reports/<timestamp>/`;
- the pass/blocker summary is promoted into `docs/development/DEVELOPMENT_STATUS.md` or `docs/development/TASK_LOG.md`.

## Tool check

```bash
scripts/qa-check-tools.sh
```

Expected tools on the Mac:

- Android SDK `adb` and `emulator`
- Pixel_9 AVD / connected `emulator-5554`
- Maestro CLI
- Node + pnpm

## Run an already-installed app smoke

```bash
scripts/qa-passenger-android.sh
# or directly:
maestro --device emulator-5554 test qa/maestro/mobile/smoke/auth-profile-smoke.yaml
maestro --device emulator-5554 test qa/maestro/mobile/smoke/home-search-route-discovery-smoke.yaml
maestro --device emulator-5554 test qa/maestro/mobile/regression/task07-home-search-route-discovery.yaml
```

Outputs screenshots and JUnit/log files under ignored `qa/reports/<timestamp>/`.

## Build/install passenger debug app then smoke

```bash
scripts/qa-passenger-dev-run.sh
```

This sets adb reverses so the emulator can reach the local API on `localhost:8080` and Metro on `8082`. For Expo dev-client Android emulator runs, prefer `EXPO_PUBLIC_API_BASE_URL=http://10.0.2.2:8080` when Metro bundles the runtime environment; adb reverse remains useful for native/device traffic and localhost assumptions.

## OTP QA convention

The passenger app must not contain or autofill a hard-coded OTP bypass.

For local/backend-only QA, the backend can allow Notify.lk demo sender behavior when this environment variable is explicitly set:

```bash
NOTIFY_LK_ALLOW_DEMO_SENDER_FOR_OTP=true
```

When enabled for local QA, SMS is not sent and the backend accepts `000000` for the generated challenge. Keep this disabled outside local QA/dev, and keep `.env.example` safe for public commits.
