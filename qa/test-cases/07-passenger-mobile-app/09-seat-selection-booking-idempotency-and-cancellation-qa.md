# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/09-seat-selection-booking-idempotency-and-cancellation.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Single-seat booking sends correct DTO and idempotency key.
- Over-selecting seats is blocked; valid multi-seat succeeds.
- Network timeout retry reuses same idempotency key and creates no duplicate booking.
- Rapid CTA taps start only one mutation.
- Cancellation success updates status; forbidden cancellation keeps booking active with policy message.
- Seat cells are accessible and taken seats cannot be selected.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.
