# QA — ---

## Related implementation task

- `docs/development/implementation/tasks/07-passenger-mobile-app/10-payment-methods-payment-intents-receipts-and-trip-history.md`

## Scope

This file owns the QA requirements, test cases, manual checks, and evidence expectations for the related implementation task. Keep implementation details in the development task file and keep QA execution evidence in ignored `qa/reports/` or `qa/runs/` unless a concise status summary is promoted into shared development status docs.

## QA test cases

- Switching payment methods updates selected state and CTA.
- Payment intent request never sends client-controlled amount unless backend contract explicitly requires it.
- Card failure/wallet insufficient/backend 500 show retry/change-method and create no duplicate payment.
- Receipt displays exact server total/distance/driver/vehicle/trip id.
- Trip history filters and summary are correct.
- Logs/screenshots contain only masked payment details.

## Automated test coverage
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.

## Maestro automation

- Required YAML: `qa/maestro/passenger-mobile/regression/task10-payments-receipts-trip-history.yaml`.
- Run with `scripts/qa-passenger-android.sh qa/maestro/passenger-mobile/regression/task10-payments-receipts-trip-history.yaml` on emulator/device.
- Every Maestro failure blocks task closure until fixed and rerun to pass, unless a concrete external blocker is recorded.
