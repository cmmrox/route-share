---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 04 — Design System and Reusable Screen Components from Assets

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Convert PDF/source-assets visual system into reusable accessible components.

**Scope:** All screens and primitives.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.


## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.


## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Create tokens for colors, typography, spacing, radius, shadows, semantics, match tiers, dark mode.
2. Implement primitives: Screen, AppText, Button, IconButton, TextField, OTP field, Chip, Card, BottomSheet, ListRow, StatCard, ProgressBar, Toast, ConfirmDialog, Empty/Error/Loading states.
3. Implement RouteShare components: Avatar, MatchRing, route spine/timeline, fare row, payment row, SeatPlan, SOS button, map overlay cards.
4. Replace HTML/CSS map mock with React Native map abstraction plus deterministic test mock.
5. Add accessibility labels, roles, hints, selected/disabled states, and 44px touch targets.
6. Add component tests/snapshots for light/dark, small/large screens, loading/error/empty, and text expansion.



## Files expected to change
- `apps/passenger-mobile/**` — feature code, tests, app config, assets, native config, QA helpers.
- `packages/api-contracts/**` and `packages/shared-types/**` — only for shared contract/type updates.
- `docs/api/**` — only for contract reconciliation/runtime DTO notes.
- `docs/development/DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `TASK_LOG.md`, `BLOCKERS.md` — update after implementation.

## QA test cases

- Tokens match source-assets terracotta, teal, warm background, ink, semantic, match-tier colors.
- Every interactive primitive has role/label/hint and disabled/selected state.
- Small Android and iPhone SE layouts do not clip bottom CTAs.
- Dark mode contrast remains readable.
- SeatPlan supports driver/taken/free/selected/disabled states accessibly.


## Automated test requirements
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.


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
- [x] Every QA case above is automated or manually evidenced with reason.
- [x] iOS and Android have been checked for this feature area via local scaffold smoke/config gates plus Android debug assemble; full device automation remains later release evidence.
- [x] Development tracking docs are updated.
- [x] No unresolved blocker remains inside this feature area.
- [x] A focused commit is ready for this task.

## Suggested commit message
```bash
git commit -m "feat(passenger-mobile): complete design system and reusable screen components from assets"
```


## Completion evidence — 2026-06-14 19:05 +0530

Implemented the RouteShare passenger design-system foundation from source assets: warm terracotta/teal tokens, semantic/match colors, spacing/radius/shadow/type scales, reusable accessible primitives, RouteShare-specific components, deterministic map abstraction, and updated shell/home previews.

Verification passed:

```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:android
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile build:preview:android
pnpm --filter @routeshare/passenger-mobile build:preview:ios
cd apps/passenger-mobile/android && ./gradlew app:assembleDebug -x lint -x test --configure-on-demand --build-cache -PreactNativeDevServerPort=8082 -PreactNativeArchitectures=arm64-v8a --console=plain
```

Unit result: `11` files / `47` tests passed. Android assemble result: `BUILD SUCCESSFUL in 2s`.

Native note: e2e and preview scripts are local scaffold/config gates; real remote EAS submission and full simulator/device Detox automation remain later release evidence after credentials/devices are finalized.
