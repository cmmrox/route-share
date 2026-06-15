# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/01-passenger-api-contract-reconciliation-and-typed-client.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Public config loads without Authorization header and returns LK/LKR feature flags.
- Protected calls without token produce typed `UnauthorizedError` and no crash.
- Backend `{success:true,data}` envelope is unwrapped only in the client layer.
- Ride search, booking, profile, places, contacts, and payment DTO mismatches are tested.
- Logs redact Authorization, refresh tokens, OTPs, full phone/card data, and precise location payloads.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.
