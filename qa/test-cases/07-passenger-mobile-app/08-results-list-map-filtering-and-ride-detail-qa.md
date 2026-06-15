# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/08-results-list-map-filtering-and-ride-detail.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- 100/92/74/58% matches render correct colors, badges, groups.
- Map/list toggles preserve selected result.
- Filters produce deterministic ordering and clear empty filtered state.
- Ride detail passes complete booking handoff data to Seat Selection.
- Placeholder/readiness backend shapes either normalize or show actionable contract error.
- Screen reader reads card driver, fare, match, time, seats, CTA.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.
