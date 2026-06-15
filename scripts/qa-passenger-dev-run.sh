#!/usr/bin/env bash
set -euo pipefail
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$HOME/.maestro/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"
cd "$(dirname "$0")/.."
DEVICE="${ANDROID_SERIAL:-emulator-5554}"
APP_DIR="$PWD/apps/passenger-mobile"
if ! adb devices | grep -q "^$DEVICE[[:space:]]*device"; then
  echo "Android device $DEVICE is not connected. Start the Pixel_9 emulator first." >&2
  adb devices >&2
  exit 1
fi
adb -s "$DEVICE" reverse tcp:8080 tcp:8080 || true
adb -s "$DEVICE" reverse tcp:8082 tcp:8082 || true
(
  cd "$APP_DIR"
  EXPO_PUBLIC_APP_ENV=simulator EXPO_PUBLIC_API_BASE_URL=http://10.0.2.2:8080 EXPO_PUBLIC_AUTH_PHONE_OTP_SUPPORTED=true EXPO_NO_METRO_WORKSPACE_ROOT=1 pnpm exec expo run:android --port 8082 --variant debug
)
exec "$PWD/scripts/qa-passenger-android.sh" "${1:-qa/maestro/passenger-mobile/smoke/auth-profile-smoke.yaml}"
