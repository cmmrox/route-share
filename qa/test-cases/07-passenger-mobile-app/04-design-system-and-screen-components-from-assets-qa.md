# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/04-design-system-and-screen-components-from-assets.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Tokens match source-assets terracotta, teal, warm background, ink, semantic, match-tier colors.
- Every interactive primitive has role/label/hint and disabled/selected state.
- Small Android and iPhone SE layouts do not clip bottom CTAs.
- Dark mode contrast remains readable.
- SeatPlan supports driver/taken/free/selected/disabled states accessibly.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.

## Maestro automation

- Required YAML: `qa/maestro/passenger-mobile/regression/task04-design-system-screen-components.yaml`.
- Run with `scripts/qa-passenger-android.sh qa/maestro/passenger-mobile/regression/task04-design-system-screen-components.yaml` on emulator/device.
- Every Maestro failure blocks task closure until fixed and rerun to pass, unless a concrete external blocker is recorded.
