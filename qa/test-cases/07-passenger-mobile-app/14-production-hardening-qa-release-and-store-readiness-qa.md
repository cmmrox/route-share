# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/14-production-hardening-qa-release-and-store-readiness.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Full happy path fresh install → booking → live trip → receipt → rating passes.
- Permission allow/deny/revoke matrix passes.
- Offline/slow/backend failure/token expiry recover safely with no duplicate mutations.
- Screen reader can complete core booking and SOS flows.
- Bundle/log/crash inspection finds no secrets or sensitive leaks.
- iOS and Android preview/prod candidate builds install and pass smoke tests.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.

## Maestro automation

- Required release YAML: `qa/maestro/passenger-mobile/release/task14-production-hardening-store-readiness-release.yaml`.
- Rerun every committed passenger smoke/regression flow before release closure.
- Run with `scripts/qa-passenger-android.sh <yaml-path>` on emulator/device.
- Every Maestro failure blocks task closure until fixed and rerun to pass, unless a concrete external blocker is recorded.
