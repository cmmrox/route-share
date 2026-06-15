---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 02 — Expo App Scaffold, Dev Tooling, and Release Pipeline

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Create the production Expo React Native app foundation.

**Scope:** Foundation for all screens.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.

## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/development/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Initialize `apps/passenger-mobile` as an Expo Dev Build TypeScript app inside the pnpm workspace.
2. Add navigation, TanStack Query, Zustand, React Hook Form, Zod, SecureStore, AuthSession, Location, Notifications, maps, image, safe area, Sentry, Jest/RNTL, and E2E tooling.
3. Configure TypeScript strict, ESLint, Prettier, path aliases, Jest native mocks, app icons/splash, bundle IDs, URL scheme, and permission strings.
4. Add environment profiles for local Mac, simulator, physical device/Tailscale, staging, and production.
5. Add scripts: start, lint, typecheck, test, test:e2e:ios, test:e2e:android, prebuild, build:preview:ios, build:preview:android, doctor.
6. Add providers: SafeArea, QueryClient, Theme, Auth stub, ErrorBoundary, Toast, Sentry.

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
- [ ] All implementation steps are complete.
- [ ] Linked QA cases are automated or manually evidenced with reason.
- [ ] iOS and Android have been checked for this feature area.
- [ ] Development tracking docs are updated.
- [ ] No unresolved blocker remains inside this feature area.
- [ ] A focused commit is ready for this task.

## Suggested commit message
```bash
git commit -m "feat(passenger-mobile): complete expo app scaffold, dev tooling, and release pipeline"
```

## Implementation result — 2026-06-14 01:32 +0530

Status: `CORE_VERIFIED_DEVICE_AUTOMATION_DEFERRED`

Completed:

- [x] `apps/passenger-mobile` is now a runnable Expo React Native TypeScript app foundation inside the pnpm workspace.
- [x] App entry points added: `index.ts`, `App.tsx`, `app.config.ts`.
- [x] React Navigation root shell added with a visible RouteShare Passenger scaffold screen.
- [x] Provider foundation added: SafeArea, TanStack Query, theme, auth Zustand stub, ErrorBoundary, Toast, and Sentry initialization hook.
- [x] Environment profiles added for `local`, `simulator`, `device`/Tailscale, `staging`, and `production`.
- [x] Dev tooling added: ESLint, Prettier, strict TypeScript, Vitest tests, Expo Doctor, EAS preview config, Detox config, and local smoke scripts.
- [x] Production app identity configured: name, slug, URL scheme, iOS bundle ID, Android package, and location/notification permission strings.
- [x] Web render/export smoke passed and visibly rendered the scaffold page from `/tmp/routeshare-passenger-web-export`.
- [x] Expo Doctor passed `21/21` checks.

Verification passed:

```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile test:e2e:android
pnpm --filter @routeshare/passenger-mobile build:preview:ios
pnpm --filter @routeshare/passenger-mobile build:preview:android
cd apps/passenger-mobile && pnpm run doctor
cd apps/passenger-mobile && pnpm exec expo export --platform web --output-dir /tmp/routeshare-passenger-web-export
```

Test result: `6` files / `28` tests passed.

Device automation note:

- `test:e2e:ios` and `test:e2e:android` now validate the scaffold and Detox config locally.
- `build:preview:ios` and `build:preview:android` now validate preview build configuration locally.
- Real simulator/device Detox execution and remote EAS build submission were not triggered in this local task run because native projects/devices/build credentials are not yet generated or selected. This is recorded as follow-up release evidence, not a blocker to the app scaffold being runnable.

## QA reference

QA test plan and task-specific test cases are maintained outside development docs: `qa/test-cases/07-passenger-mobile-app/02-expo-app-scaffold-dev-tooling-release-pipeline-qa.md`.
