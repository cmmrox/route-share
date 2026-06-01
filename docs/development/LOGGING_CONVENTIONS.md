# RouteShareApp Logging Conventions

## Goals

- Logs should explain business milestones and operational failures without exposing secrets or private user data.
- Use structured key/value context where supported by the logging backend.
- Keep logs useful for debugging booking, trip, payment, verification, and realtime flows.

## Required Practices

- Never log JWTs, refresh tokens, passwords, object-storage signed URLs, payment provider secrets, bank details, full document contents, or precise location history dumps.
- Prefer IDs and public correlation values over personal data.
- Include operation names for important state transitions, for example `booking.transition`, `trip.transition`, `payment.capture`, and `driver.verification.review`.
- Log rejected state transitions at `WARN` with reason and safe identifiers.
- Log recoverable external integration failures at `WARN`; unrecoverable server failures at `ERROR`.
- Keep normal high-volume operations, especially future location ingestion, at `DEBUG` or sampled `INFO` only.
- Add request/correlation IDs before production deployment hardening.

## Current Configuration

`apps/api/src/main/resources/application.yml` sets the application logger namespace `com.routeshare` to `INFO`. Phase 10 observability will add richer structured appenders, tracing, metrics, and dashboards.
