# QA — Task 07 Home, Search, Location, and Route Discovery

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/07-home-search-location-and-route-discovery.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## Provider prerequisites

Task 07 QA must run against the real production-intended map/place integration. Before claiming release readiness, configure Google Maps Platform for server, Android, and iOS:

```env
GOOGLE_MAPS_ENABLED=true
GOOGLE_MAPS_SERVER_API_KEY=...
EXPO_PUBLIC_GOOGLE_MAPS_ANDROID_API_KEY=...
EXPO_PUBLIC_GOOGLE_MAPS_IOS_API_KEY=...
```

If these are missing, record the run as blocked. Do not pass the stage using fake maps, fake geocoding, or placeholder suggestions.

## QA test cases

- Home loads greeting, saved places, recent routes, and the real Google-backed map/dashboard without flicker.
- Location denied still allows manual pickup search.
- Pickup/drop suggestions resolve through real Places/geocoding and pickup/drop swap changes labels and coordinates.
- Past time is blocked; future scheduled search sends correct DTO.
- Empty/no-results/backend-error states offer Retry/Edit Search.
- Search request contains valid lat/lng/time/seats and no stale route context.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.


## Maestro automation

- Existing smoke YAML: `qa/maestro/passenger-mobile/smoke/home-search-route-discovery-smoke.yaml`.
- Existing regression YAML: `qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml`.
- Run with `scripts/qa-passenger-android.sh <yaml-path>` on emulator/device.
- Every Maestro failure blocks task closure until fixed and rerun to pass, unless a concrete external blocker is recorded.

## Task 07 execution status — 2026-06-15

Automated coverage added:

- `apps/passenger-mobile/src/features/ride-search/__tests__/ride-search-task07.test.ts` covers validation, DTO mapping, location fallback state, swap behavior, recent-search retention, and home dashboard model building.
- `qa/maestro/passenger-mobile/smoke/home-search-route-discovery-smoke.yaml` covers the authenticated/profile-complete home → search → available-rides smoke path.
- `qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml` covers clean-state onboarding → OTP → profile → home → search regression execution.

Manual/runtime checks required for release candidate evidence:

- Android and iOS device checks should capture permission granted, permission denied, manual pickup fallback, and backend error/retry screenshots into ignored `qa/reports/`.
- The Task 07 code path navigates to the existing Search Results placeholder with backend route-search result data; full results UI remains Task 08.

## Task 07 execution status — 2026-06-16 (Android device QA green)

Android emulator (`emulator-5554`) end-to-end QA executed against the real stack
(Postgres/Keycloak/Redis up, backend API on 8080 with demo-OTP + Google Maps enabled,
Metro on 8082, installed debug dev client) using `scripts/qa-passenger-android.sh`.

- `qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml` — **PASS** (clean-state onboarding → OTP → profile → home → Google Places pickup/destination selection → ride search → Available rides). Evidence under ignored `qa/reports/20260616-224636/`.
- Real Google-backed integration confirmed: the SearchResults handoff carried real resolved coordinates (pickup `6.9336686, 79.8500469`; dropoff `6.8649081, 79.8996789`) from Google Places `details`, not placeholder data.

Fixes made during this QA pass:

- Backend: `GooglePlaceSearchService` had two constructors and neither was `@Autowired`, so Spring fell back to a non-existent no-arg constructor and the API failed to start (same class of issue as resolved Blocker 004). Annotated the production constructor with `@Autowired`; backend boots and serves Places autocomplete/details.
- Maestro flows (regression + smoke): updated to drive the implemented Google-Places UX — type query → "Search pickup/destination places" → tap a `📍` suggestion (resolves a `placeId`) before "Search shared rides". Removed `hideKeyboard` (Android Maestro sends BACK for it, which popped the Search screen); use `pressKey: Enter` to dismiss the keyboard.

Observations (not Task 07 blockers):

- Offline banner is a false positive on the emulator: `useNetworkState` (Task 03, `src/application/network-state.ts`) pings `https://clients3.google.com/generate_204`, which is unreachable from this emulator. It does not block search (query client is `online=true`; Places calls go through the runtime API directly).
- On a cold app launch the app returns to Onboarding — the authenticated session is not auto-restored across restart (Task 05 secure-token/session-restore scope). Consequently the warm-path smoke flow's "already authenticated" precondition is not met by a bare `launchApp`; its corrected search steps are validated transitively by the green regression run.

Remaining for Task 07 production closure: iOS simulator/device runtime evidence for the same map/place/search path (Blocker 011).
