# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/03-app-shell-navigation-state-and-offline-foundation.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Fresh install starts onboarding; returning user skips it.
- Valid token lands on Home; expired token refreshes or routes to Login.
- Offline launch uses safe cached state and blocks unsafe mutations.
- Android back unwinds Home → Search → Results → Detail → Seat correctly.
- Deep links never bypass auth or ownership checks.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.

## Maestro automation

- Required YAML: `qa/maestro/passenger-mobile/regression/task03-app-shell-navigation-state-offline.yaml`.
- Run with `scripts/qa-passenger-android.sh qa/maestro/passenger-mobile/regression/task03-app-shell-navigation-state-offline.yaml` on emulator/device.
- Every Maestro failure blocks task closure until fixed and rerun to pass, unless a concrete external blocker is recorded.
