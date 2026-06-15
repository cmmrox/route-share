#!/usr/bin/env bash
set -euo pipefail
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$HOME/.maestro/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"
printf 'node: '; node --version
printf 'pnpm: '; pnpm --version
printf 'adb: '; command -v adb; adb version | head -1
printf 'emulator: '; command -v emulator; emulator -version 2>/dev/null | head -1 || true
printf 'maestro: '; command -v maestro; maestro --version
printf 'devices:
'; adb devices
printf 'avds:
'; emulator -list-avds || true
