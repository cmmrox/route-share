# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/12-exit-early-sos-safety-and-emergency-flows.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Stay on Ride returns to In Trip without mutation.
- Drop Me Here calls endpoint once and routes to completion/pending state clearly.
- SOS early release cancels; full 3-second hold creates event.
- Police action opens dialer/fallback safely.
- No trusted contacts state guides user without blocking police/copy alternatives.
- SOS screen passes accessibility and contrast checks.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.
