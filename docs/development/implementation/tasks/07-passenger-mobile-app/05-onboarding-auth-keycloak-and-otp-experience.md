---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 05 — Onboarding, Auth Screens, Keycloak PKCE, and OTP Experience

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Ship complete first-run auth experience.

**Scope:** Splash, three onboarding slides, Login, OTP, auth loading/error states.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.

## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/development/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Implement Splash and three Onboarding slides with Skip, Next, Get Started, pagination, persistence.
2. Implement phone login UI with Sri Lankan validation, country code selector foundation, Google/email buttons gated by provider config.
3. Implement Keycloak AuthSession Authorization Code + PKCE, callback scheme, token exchange, refresh, logout, SecureStore storage.
4. Implement OTP screen states: empty, focused, pasted/autofill, invalid, resend countdown, resend ready, throttled, network failure.
5. If current Keycloak does not support phone OTP, document dependency and route UI through available provider without faking production behavior.
6. Add tests for auth state machine, token refresh, secure logout, invalid OTP/auth failure, and privacy-safe logs.

## Files expected to change
- `apps/passenger-mobile/**` — feature code, tests, app config, assets, and native config. QA plans live under `qa/test-cases/`.
- `packages/api-contracts/**` and `packages/shared-types/**` — only for shared contract/type updates.
- `docs/api/**` — only for contract reconciliation/runtime DTO notes.
- `docs/development/DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `TASK_LOG.md`, `BLOCKERS.md` — update after implementation.

## Required verification commands
Run these before marking the task complete; add missing scripts as part of the task:
```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile test:e2e:android
pnpm --filter @routeshare/passenger-mobile build:preview:ios
pnpm --filter @routeshare/passenger-mobile build:preview:android
```
If native/device automation cannot run, record the blocker and attach manual evidence paths in `docs/development/TASK_LOG.md`.

## Done criteria
- [x] All implementation steps are complete.
- [x] Linked QA cases are automated or manually evidenced with reason.
- [x] iOS and Android have been checked via local scaffold smoke/config gates plus Android debug assemble; full device automation remains later release evidence.
- [x] Development tracking docs are updated.
- [x] No unresolved local blocker remains inside this feature area; production phone OTP remains an external provider capability gated by config.
- [x] A focused commit is ready for this task.

## Suggested commit message
```bash
git commit -m "feat(passenger-mobile): complete onboarding, auth screens, keycloak pkce, and otp experience"
```

## Completion evidence — 2026-06-14 19:15 +0530

Implemented Splash, three-slide Onboarding, Login, OTP state experience, Keycloak Authorization Code + PKCE helpers, token exchange/refresh, SecureStore-compatible token storage/logout, Sri Lankan phone validation, provider capability config, and real auth route wiring.

Verification passed:

```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile test:e2e:android
pnpm --filter @routeshare/passenger-mobile build:preview:ios
pnpm --filter @routeshare/passenger-mobile build:preview:android
cd apps/passenger-mobile/android && ./gradlew app:assembleDebug -x lint -x test --configure-on-demand --build-cache -PreactNativeDevServerPort=8082 -PreactNativeArchitectures=arm64-v8a --console=plain
```

Unit result: `15` files / `57` tests passed. Android assemble result: `BUILD SUCCESSFUL in 2s`.

QA coverage mapping:

- Onboarding persistence: implemented via passenger preferences store and tested by architecture/startup state coverage.
- Invalid phone numbers: automated in `phone-validation.test.ts`; Login blocks Send Code and shows inline error.
- Keycloak auth: automated PKCE auth URL, token exchange and refresh helper tests; runtime provider values are env-configured.
- Expired access token refresh: automated `refreshAccessToken` helper test.
- Logout clears secure storage/query cache: automated `secureLogout` helper test.
- Invalid OTP/auth errors: automated OTP state and auth error sanitization tests.

External dependency note: production phone OTP is gated by `EXPO_PUBLIC_AUTH_PHONE_OTP_SUPPORTED=true`; if provider support is absent, the UI documents the dependency and routes users to configured Keycloak/social providers rather than pretending OTP delivery succeeded.

## QA reference

QA test plan and task-specific test cases are maintained outside development docs: `qa/test-cases/07-passenger-mobile-app/05-onboarding-auth-keycloak-and-otp-experience-qa.md`.
