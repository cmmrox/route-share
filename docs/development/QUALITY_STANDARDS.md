# RouteShareApp Quality Standards

## Purpose

This file defines the coding and architecture standards for RouteShareApp. All implementation should follow these standards unless the project owner explicitly approves an exception.

## Core Principles

All code must be:

- Clean.
- Maintainable.
- Human-readable.
- Testable.
- Modular.
- SOLID.
- Reusable.
- Secure.
- Properly validated.
- Properly logged.
- Commented where comments add real value.
- Designed according to industry standards.

## Architecture Principles

- Keep clear boundaries between layers.
- Keep business logic out of controllers.
- Keep persistence logic out of services except through repositories/ports.
- Keep external integrations behind adapters.
- Prefer simple, explicit code over clever code.
- Avoid duplication; extract reusable utilities where it improves clarity.
- Avoid premature overengineering.
- Design modules so they can be tested independently.

## Backend Layering Standard

Backend modules follow the accepted **modular monolith + service/impl + facade** style. Use business-module packages instead of global layer packages.

```text
module
├── controller          # REST controllers only
├── dto/request         # input DTOs + validation
├── dto/response        # output DTOs
├── mapper              # MapStruct mappers
├── service             # service interfaces
├── service/impl        # service implementations and transactions
├── facade              # small public API for other modules
├── facade/impl         # facade implementation
├── domain              # rules, enums, policies, state machines
├── entity              # JPA entities
├── repository          # persistence only
├── event               # internal events
└── config              # module config when needed
```

Rules:

- Controllers call services only.
- Controllers must not import repositories/entities.
- Services hold business logic and transaction boundaries.
- Services may use their own module repositories.
- Services must not import another module's repository, entity, or impl class.
- Cross-module access must go through facade/service interfaces and DTOs.
- Repositories/entities are internal module details.
- MapStruct mappers use `RouteShareMapperConfig`.
- Do not put business logic inside mappers.

See `docs/architecture/BACKEND-MODULAR-MONOLITH-SERVICE-IMPL-FACADE.md`.

## Spring Boot Standards

- Use constructor injection.
- Keep fields `private final` where possible.
- Do not expose JPA entities directly from controllers.
- Use DTOs for API input/output.
- Use Bean Validation for request validation.
- Use transactions at service/application layer.
- Keep controllers thin.
- Use explicit names for services and methods.
- Use `@PreAuthorize` or equivalent authorization guards for protected operations.
- Keep secrets and credentials out of code and logs.

## Virtual Threads Standard

- Java 21 virtual threads are enabled for the Spring Boot backend using `spring.threads.virtual.enabled=true`.
- Keep the code on the simple Spring MVC/JPA model; do not introduce reactive code only for concurrency.
- Keep blocking calls bounded by resource limits, especially the Hikari database connection pool.
- Do not create unmanaged `Thread`, `Executor`, or `ThreadPool` instances in application code.
- Prefer Spring-managed execution for future async/event processing.
- Virtual threads improve concurrency for blocking I/O, but they do not remove the need for database indexes, efficient queries, backpressure, and connection-pool monitoring.

## Database Standards

- Use Flyway migrations.
- Use schema-per-module in one PostgreSQL database.
- Use `bigint generated always as identity` for internal primary keys.
- Use `uuid` only for public/external identifiers where needed.
- Use `timestamptz` for timestamps.
- Use `numeric` for money.
- Use PostGIS geometry types for geospatial data.
- Add indexes for foreign keys and access paths.
- Do not use PostgreSQL `money` type.
- Do not use floating point types for financial values.

## Logging Standards

Use meaningful logs for important events.

Recommended log levels:

- `INFO` — important successful business milestones.
- `WARN` — suspicious or recoverable issues.
- `ERROR` — failures requiring attention.
- `DEBUG` — troubleshooting details for development/debugging.

Good log examples:

- Driver KYC application submitted.
- Booking confirmed.
- Seat reservation rejected due to insufficient available seats.
- Trip state transition rejected.
- Payment capture failed.
- Location update rejected due to stale timestamp or impossible jump.

Never log:

- Passwords.
- Access tokens.
- Refresh tokens.
- Full JWTs.
- Bank account numbers.
- Secret keys.
- Full identity documents.
- Sensitive personal data unless masked and necessary.

## Comments Standard

Comments should explain why something exists or why a non-obvious decision was made.

Avoid comments that simply repeat the code.

Bad:

```java
// Set name
user.setName(name);
```

Good:

```java
// Store route fractions so fare finalization can calculate the passenger segment
// without recomputing expensive PostGIS line-location operations during payment capture.
booking.assignRouteFractions(pickupFraction, dropoffFraction);
```

## Testing Standards

Use TDD for core behavior where possible:

1. Write failing test.
2. Run and verify expected failure.
3. Implement minimal code.
4. Run and verify pass.
5. Refactor.
6. Run full relevant test suite.

Core areas requiring strong tests:

- Authentication/authorization mapping.
- Passenger/driver profile rules.
- Driver KYC approval rules.
- Vehicle verification rules.
- Route matching and scoring.
- Booking state transitions.
- Seat reservation and no-overbooking logic.
- Trip lifecycle state machine.
- Passenger boarded/no-show/drop-off rules.
- Fare calculation.
- Payment lifecycle.
- Settlement ledger updates.
- Idempotency handling.
- Event/outbox handling.

## API Standards

- Use consistent URL naming.
- Use versioned APIs: `/api/v1/...`.
- Use consistent error response format.
- Use idempotency keys for retry-safe mutations.
- Use pagination for list endpoints.
- Keep API responses stable and documented in OpenAPI.
- Validate all client input.

## Review Checklist

Before any task is marked complete:

- [ ] Code is readable and clearly named.
- [ ] Layers are separated correctly.
- [ ] No unnecessary duplication.
- [ ] No secrets or sensitive data in code/logs.
- [ ] Tests exist for business logic.
- [ ] Relevant tests pass.
- [ ] API changes are documented.
- [ ] Development tracking files are updated.
