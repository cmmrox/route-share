# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/11-booked-waiting-live-trip-realtime-and-share-trip.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Booked screen displays booking state after booking success.
- Relaunch during active trip resumes Booked/In Trip.
- WebSocket updates first; HTTP polling fallback works when WS disconnects.
- Offline active trip shows last-known stale state.
- Share link can be copied/shared and errors retry.
- Loading another passenger trip id is rejected safely.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.
