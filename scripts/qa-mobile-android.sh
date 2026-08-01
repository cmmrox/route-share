#!/usr/bin/env bash
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$HOME/.maestro/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"

cd "$(dirname "$0")/.."

DEVICE="${ANDROID_SERIAL:-emulator-5554}"
FLOW="${1:-qa/maestro/mobile/smoke/auth-profile-smoke.yaml}"
REPORT_DIR="${ROUTESHARE_QA_REPORT_DIR:-$PWD/qa/reports/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$REPORT_DIR"

collect_after_artifacts() {
  local exit_code=$?
  for png in qa-*.png; do
    if [[ -f "$png" ]]; then
      mv "$png" "$REPORT_DIR/$png"
    fi
  done
  adb -s "$DEVICE" exec-out screencap -p > "$REPORT_DIR/after.png" || true
  adb -s "$DEVICE" shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1 || true
  adb -s "$DEVICE" pull /sdcard/window.xml "$REPORT_DIR/window.xml" >/dev/null 2>&1 || true
  echo "QA report: $REPORT_DIR"
  return "$exit_code"
}
trap collect_after_artifacts EXIT

if ! adb devices | grep -q "^$DEVICE[[:space:]]*device"; then
  echo "Android device $DEVICE is not connected. Available devices:" >&2
  adb devices >&2
  exit 1
fi

adb -s "$DEVICE" reverse tcp:8080 tcp:8080 || true
# The installed debug app requests Metro from device localhost:8081. On this Mac,
# Docker occupies host 8081, so Metro runs on host 8082 and device 8081 must map there.
adb -s "$DEVICE" reverse tcp:8081 tcp:${ROUTESHARE_METRO_HOST_PORT:-8082} || true
adb -s "$DEVICE" reverse tcp:8082 tcp:${ROUTESHARE_METRO_HOST_PORT:-8082} || true
adb -s "$DEVICE" reverse tcp:9000 tcp:9000 || true
adb -s "$DEVICE" shell input keyevent KEYCODE_WAKEUP || true
adb -s "$DEVICE" exec-out screencap -p > "$REPORT_DIR/before.png" || true

maestro --device "$DEVICE" test "$FLOW" --format junit --output "$REPORT_DIR/maestro-junit.xml" | tee "$REPORT_DIR/maestro.log"
