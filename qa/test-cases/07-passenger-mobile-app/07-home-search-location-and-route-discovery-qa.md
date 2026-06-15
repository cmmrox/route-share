# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/07-home-search-location-and-route-discovery.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Home loads greeting, saved places, recent routes, and map/dashboard without flicker.
- Location denied still allows manual pickup search.
- Pickup/drop swap changes labels and coordinates.
- Past time is blocked; future scheduled search sends correct DTO.
- Empty/no-results/backend-error states offer Retry/Edit Search.
- Search request contains valid lat/lng/time/seats and no stale route context.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.
