# Stage 10 — Hardening, QA, Performance, Security, and Release Roadmap

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Prepare RouteShareApp for controlled pilot and later public launch.

**Architecture:** Keep MVP simple but verify correctness of route matching, seat capacity, location tracking, payment auditability, and safety workflows before real users.

**Tech Stack:** Automated tests, load tests, observability, Sentry, OpenTelemetry, CI/CD, Expo EAS, Docker deployment.

---

## Acceptance criteria

- Critical business rules are covered by automated tests.
- Route matching has scenario-based test suite.
- Booking cannot overbook under concurrency tests.
- Location ingestion has load and validation tests.
- Payment/fare/settlement ledgers are auditable.
- Mobile apps have release builds and crash reporting.

## Tasks

### Task 1: Test strategy

Backend:
- Unit tests for domain/state machines.
- Integration tests with Testcontainers PostGIS.
- API contract tests.
- Concurrency tests for seat capacity.

Mobile:
- Component tests for forms/components.
- Navigation smoke tests.
- Critical flow E2E later with Maestro/Detox.

### Task 2: Route matching scenario suite

Scenarios:
- full match
- partial 50%
- high overlap 90%
- nearby pickup within radius
- pickup outside radius
- wrong direction
- insufficient seats
- departure outside time window

### Task 3: Location performance tests

Test:
- 100 active trips
- 1,000 active trips later
- GPS update rate 1–3 seconds
- Redis TTL behavior
- WebSocket reconnect behavior

### Task 4: Payment and fare audit tests

Test:
- card preauth then capture lower final amount
- cash creates commission receivable
- passenger exits early
- refund/adjustment creates ledger reversal, not mutation

### Task 5: Security hardening

Checklist:
- TLS everywhere
- JWT refresh rotation
- PII minimization in logs
- document access controls
- rate limits for OTP/login/payment/booking
- admin RBAC
- audit logs
- no raw card storage

### Task 6: Observability

Add:
- OpenTelemetry traces
- Prometheus metrics
- Grafana dashboards
- structured logs
- Sentry for mobile/backend/admin

Critical metrics:
- route search latency
- match success rate
- booking conversion
- seat conflicts
- location delay
- WebSocket reconnects
- ETA error
- payment failure rate

### Task 7: Release pipeline

Backend:
- Docker image
- staging env
- DB migrations backup policy

Mobile:
- Expo EAS dev/staging/prod profiles
- Android internal testing
- iOS TestFlight later

Admin:
- staging deployment
- protected admin auth

### Task 8: Pilot launch

Pilot with limited drivers/routes:
- manually verified drivers
- limited service area
- cash plus one card sandbox/real gateway if approved
- support escalation process
- daily review of failed matches, cancellations, payment issues, safety events
