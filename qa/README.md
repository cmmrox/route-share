# RouteShare QA automation

This folder contains repeatable QA helpers for Hermes/Mac takeover testing.

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
```

Outputs screenshots and JUnit/log files under `qa/reports/<timestamp>/`.

## Build/install passenger debug app then smoke

```bash
scripts/qa-passenger-dev-run.sh
```

This sets adb reverses so the emulator can reach the local API on `localhost:8080` and Metro on `8082`.

## OTP QA convention

For local QA only, the backend can accept a static OTP when:

```bash
ROUTESHARE_OTP_DEV_BYPASS_ENABLED=true
ROUTESHARE_OTP_DEV_BYPASS_CODE=000000
```

Keep this disabled outside local QA/dev.
