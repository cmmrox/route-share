#!/usr/bin/env bash
set -euo pipefail
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$HOME/.maestro/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"
cd "$(dirname "$0")/.."
DEVICE="${ANDROID_SERIAL:-emulator-5554}"
FLOW="${1:-qa/maestro/passenger-auth-profile-smoke.yaml}"
REPORT_DIR="${ROUTESHARE_QA_REPORT_DIR:-$PWD/qa/reports/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$REPORT_DIR"
if ! adb devices | grep -q "^$DEVICE[[:space:]]*device"; then
  echo "Android device $DEVICE is not connected. Available devices:" >&2
  adb devices >&2
  exit 1
fi
adb -s "$DEVICE" reverse tcp:8080 tcp:8080 || true
adb -s "$DEVICE" reverse tcp:8082 tcp:8082 || true
adb -s "$DEVICE" reverse tcp:9000 tcp:9000 || true
adb -s "$DEVICE" shell input keyevent KEYCODE_WAKEUP || true
adb -s "$DEVICE" exec-out screencap -p > "$REPORT_DIR/before.png" || true
maestro --device "$DEVICE" test "$FLOW" --format junit --output "$REPORT_DIR/maestro-junit.xml" | tee "$REPORT_DIR/maestro.log"
for png in qa-*.png; do
  if [[ -f "$png" ]]; then
    mv "$png" "$REPORT_DIR/$png"
  fi
done
adb -s "$DEVICE" exec-out screencap -p > "$REPORT_DIR/after.png" || true
echo "QA report: $REPORT_DIR"
