# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/02-expo-app-scaffold-dev-tooling-release-pipeline.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Fresh checkout can run install, lint, typecheck, and tests.
- Local backend config connects to `GET /api/v1/app/config`.
- Production build hides debug menus, mock switches, and local endpoints.
- iOS/Android permissions match actual features only.
- Error boundary shows friendly recovery and captures redacted error context.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.

## Maestro automation

- Required YAML: `qa/maestro/passenger-mobile/smoke/app-launch-smoke.yaml`.
- Required YAML: `qa/maestro/passenger-mobile/regression/task02-expo-app-scaffold-dev-tooling-release-pipeline.yaml`.
- Run with `scripts/qa-passenger-android.sh <yaml-path>` on emulator/device.
- Every Maestro failure blocks task closure until fixed and rerun to pass, unless a concrete external blocker is recorded.
