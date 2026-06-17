# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/06-profile-avatar-verification-saved-places-and-trusted-contacts.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Profile persists after save and relaunch.
- Oversized/unsupported avatar files are rejected; upload failure can retry.
- Saved place CRUD works and deleted places disappear from search.
- Trusted contacts update Share/SOS flows.
- Denied contacts/photos/location permissions have manual fallbacks.
- Empty/invalid names, phones, and coordinates show safe validation errors.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.

## Maestro automation

- Required YAML: `qa/maestro/passenger-mobile/regression/task06-profile-verification-saved-places-trusted-contacts.yaml`.
- Shared smoke may also cover this task when linked explicitly: `qa/maestro/passenger-mobile/smoke/auth-profile-smoke.yaml`.
- Run with `scripts/qa-passenger-android.sh <yaml-path>` on emulator/device.
- Every Maestro failure blocks task closure until fixed and rerun to pass, unless a concrete external blocker is recorded.
