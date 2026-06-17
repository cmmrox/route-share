---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 07 — Home, Search, Location, and Route Discovery

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Ship pickup/dropoff/time/seat search start flow.

**Scope:** Home Map A, Home Dashboard B, Search Places, time/seat pickers, location states.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.

## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/development/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Stage prerequisites / blockers

This is a production application stage, not an MVP/POC stage. Because Task 07 owns Home Map A, location permission/current-location handling, Search Places, suggestions, and coordinate-based route discovery, real Google Maps Platform configuration is a prerequisite for marking the task production-release-complete.

Required values before real map/place implementation and native QA can be completed:

```env
GOOGLE_MAPS_ENABLED=true
GOOGLE_MAPS_SERVER_API_KEY=...
EXPO_PUBLIC_GOOGLE_MAPS_ANDROID_API_KEY=...
EXPO_PUBLIC_GOOGLE_MAPS_IOS_API_KEY=...
```

Rules:

- If these keys/provider access are missing, stop and record an active blocker instead of shipping fake maps, fake geocoding, or placeholder search as complete.
- Fallback/manual search may exist only as permission-denied/offline/error handling, not as a substitute for the required production map/place implementation.
- After adding or changing Android/iOS native map keys, rebuild/reinstall the Expo dev client before device QA because `app.config.ts` injects native map configuration at build/prebuild time.

## Implementation steps

1. Implement Home Map A with real Google Maps rendering, Safety chip, account/menu, destination CTA, quick saved chips, frequent/recent rows; requires configured Google Maps keys for production completion.
2. Implement Home Dashboard B behind explicit config/feature flag with commute card, stats, quick places, ride suggestions.
3. Implement location permission request, denied/unavailable handling, current location, manual pickup fallback.
4. Implement Search Places with pickup/drop fields, swap, saved places, real Places/geocoding-backed suggestions, recent searches, keyboard-safe layout; if Google Maps/Places credentials are missing, record a blocker.
5. Implement time picker and seat count picker; validate future times and seat limits.
6. Build ride search request adapter to actual backend DTO: coordinates, `requestedDepartureTime`, `seats`, radius/window/limit.
7. Persist recent searches with clear/delete and privacy-safe retention.

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
- [ ] All production implementation steps are complete, including real Google Maps/Places integration.
- [ ] Linked QA cases are automated or manually evidenced with real Google Maps/Places runtime evidence or an explicit blocker.
- [ ] iOS and Android have been checked for this feature area.
- [x] Development tracking docs are updated with the Google Maps prerequisite/blocker.
- [ ] No unresolved blocker remains inside this feature area.
- [ ] A focused commit is ready for this task.

## Suggested commit message
```bash
git commit -m "feat(passenger-mobile): complete home, search, location, and route discovery"
```

## QA reference

QA test plan and task-specific test cases are maintained outside development docs: `qa/test-cases/07-passenger-mobile-app/07-home-search-location-and-route-discovery-qa.md`. Repeatable automation lives in `qa/maestro/passenger-mobile/smoke/home-search-route-discovery-smoke.yaml` and `qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml`; run on emulator/device and fix-rerun until pass before closing the task, with generated evidence kept in ignored `qa/reports/`.
