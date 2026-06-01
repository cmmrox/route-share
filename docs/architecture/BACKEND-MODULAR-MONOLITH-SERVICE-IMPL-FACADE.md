# Backend Modular Monolith Architecture

Date: 2026-06-01
Status: Accepted

RouteShareApp backend uses a **modular monolith** first. Code is grouped by business module, not by global technical layers. The structure intentionally uses familiar Spring Boot naming (`service`, `service/impl`, `repository`, `entity`) instead of hexagonal `port/in` and `port/out` packages.

## Standard module shape

```text
com.routeshare.<module>/
├── controller/          # REST controllers only
├── dto/
│   ├── request/         # request DTOs + Bean Validation
│   └── response/        # response DTOs
├── mapper/              # MapStruct mappers
├── service/
│   ├── <Module>Service.java
│   └── impl/
│       └── <Module>ServiceImpl.java
├── facade/              # small public API for other modules
│   ├── <Module>Facade.java
│   └── impl/
│       └── <Module>FacadeImpl.java
├── domain/              # enums, policies, state machines, business rules
├── entity/              # JPA entities only
├── repository/          # Spring Data/JPA/native persistence only
├── event/               # internal domain/application events
└── config/              # module-specific config when needed
```

Not every module needs every folder on day one. Add folders when the module actually needs them.

## Dependency rules

- Controller calls service interfaces only.
- Controller must not import repositories or entities.
- Service interface lives in `service/`; implementation lives in `service/impl/`.
- Service implementation may use its own module repositories.
- Service implementation must not use another module's repository/entity/impl directly.
- Cross-module communication goes through `facade` interfaces or service interfaces.
- Repository/entity packages are internal to their owning module.
- Mapper package uses MapStruct with `RouteShareMapperConfig`.
- SQL/JPA low-level persistence code stays in repository/persistence classes, not services.

## Example cross-module call

Today inside the monolith:

```text
payment.service.impl.PaymentServiceImpl
  -> booking.facade.BookingFacade
      -> booking.repository.BookingRepository
```

Later, if booking becomes a service, only `BookingFacadeImpl` should change to call HTTP/Kafka/gRPC. Payment service business logic should stay mostly unchanged.

## Why this architecture

This keeps the code easy for Spring Boot developers while still protecting future microservice boundaries. It avoids unfamiliar `port/in` and `port/out` naming but keeps the same important idea: each module exposes a small public API and hides internal database/entity implementation.

## Enforcement

Architecture tests in `apps/api/src/test/java/com/routeshare/architecture/PersistenceArchitectureTest.java` enforce:

- no `JdbcTemplate` in main sources
- no SQL/EntityManager/native-query logic in service implementations
- repositories live under `repository`
- entities live under `entity`
- service interfaces and `service/impl` placement
- controllers do not import repositories/entities
- MapStruct mappers use shared mapper config
- facades live under `facade` / `facade/impl`
- no cross-module repository/entity/impl imports

## Virtual threads policy

RouteShareApp runs on Java 21 and enables Spring Boot virtual threads:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

This lets servlet request handling and Spring application task execution use lightweight virtual threads where Spring Boot supports them. It improves throughput for blocking I/O-heavy work such as database calls and external HTTP calls, while keeping the familiar Spring MVC/JPA programming model.

Important rules:

- Keep database access bounded by HikariCP; virtual threads are cheap, database connections are not.
- Do not create custom thread pools unless there is a clear need.
- If async/event processing is added, prefer Spring-managed executors so the virtual-thread setting can apply consistently.
- Monitor PostgreSQL connection pool usage, request latency, and CPU before raising database pool limits.

