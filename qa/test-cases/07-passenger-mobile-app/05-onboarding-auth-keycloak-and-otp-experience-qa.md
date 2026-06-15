# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/05-onboarding-auth-keycloak-and-otp-experience.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Onboarding persists after completion.
- Invalid phone numbers block Send Code with inline error.
- Keycloak test passenger can authenticate and `auth/me` succeeds.
- Expired access token refreshes silently when refresh token is valid.
- Logout clears SecureStore and query cache; back cannot reveal protected screens.
- Invalid OTP/auth errors do not expose provider internals.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.
