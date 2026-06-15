# Maestro mobile automation

This folder contains committed, executable Maestro E2E flows. Generated output from running these flows belongs in ignored `qa/reports/`, not beside the flow files.

## Structure

```text
qa/maestro/
  passenger-mobile/
    smoke/        # fast critical path: launch, auth, profile, basic app health
    regression/   # feature journeys mapped to `qa/test-cases/07-passenger-mobile-app/`
    release/      # release-candidate verification journeys
```

## Current flow

```bash
maestro --device emulator-5554 test qa/maestro/passenger-mobile/smoke/auth-profile-smoke.yaml
```

Preferred wrapper, because it configures adb reverse ports and saves screenshots/JUnit/log output under ignored `qa/reports/<timestamp>/`:

```bash
scripts/qa-passenger-android.sh
```

Build/install the debug passenger app first, then run the smoke flow:

```bash
scripts/qa-passenger-dev-run.sh
```
