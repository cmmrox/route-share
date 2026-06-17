# Architecture And Code Rules

## Contents

- Repo-derived stack and layout
- Backend rules
- Passenger mobile rules
- API contracts and shared packages
- Database, migrations, and data correctness
- Security, privacy, and external integrations
- Verification commands
- Dependency review
- Performance rules
- Online-verified references

## Repo-Derived Stack And Layout

This repository is a pnpm monorepo with:

- `apps/api`: Spring Boot 3.3.6, Java 21, Maven Wrapper, Spring MVC, Spring Security OAuth2 Resource Server, Spring Data JPA, Flyway, PostgreSQL/PostGIS, Redis, WebSocket, MapStruct, Lombok, JaCoCo, Spotless/google-java-format.
- `apps/passenger-mobile`: Expo React Native, React 19, TypeScript, React Navigation, TanStack Query, Zustand, Expo Location/SecureStore/Notifications/ImagePicker, react-native-maps, Vitest, ESLint flat config, EAS config.
- `apps/driver-mobile` and `apps/admin-web`: placeholders only today.
- `packages/api-contracts`: lightweight TypeScript endpoint inventory from OpenAPI docs.
- `infra/docker-compose`: local PostgreSQL/PostGIS, Redis, Redpanda, Keycloak, MinIO.
- `docs/` and `qa/`: living implementation, API, architecture, status, and QA docs.

## Backend Rules

Follow the accepted modular monolith shape documented in `docs/architecture/BACKEND-MODULAR-MONOLITH-SERVICE-IMPL-FACADE.md`:

```text
com.routeshare.<module>/
  controller/
  dto/request/
  dto/response/
  mapper/
  service/
  service/impl/
  facade/
  facade/impl/
  domain/
  entity/
  repository/
  event/
  config/
```

Non-negotiable boundaries enforced by `PersistenceArchitectureTest`:

- Controllers call services and never import repositories/entities.
- Service interfaces live in `service`; implementations live in `service/impl`.
- Service implementations keep business logic and transaction boundaries, but do not contain SQL, `JdbcTemplate`, `EntityManager`, or native query logic.
- Repositories/entities are internal to the owning module.
- Cross-module calls go through facade/service interfaces and DTOs, not foreign repositories/entities/impl classes.
- MapStruct mappers use `@Mapper(config = RouteShareMapperConfig.class)` and do not hold business rules.
- Keep Spring MVC/JPA style; Java virtual threads are enabled, so do not add reactive code or unmanaged thread pools just for concurrency.

Backend naming:

- Java packages stay under `com.routeshare.<module>`.
- DTOs are `*Request` and `*Response`.
- JPA classes are `*Entity`.
- Repositories are `*Repository`.
- Service APIs are `<Module>Service`; implementations are `<Module>ServiceImpl`.
- Facades are `<Module>Facade`; implementations are `<Module>FacadeImpl`.
- Tests mirror the package under `apps/api/src/test/java/com/routeshare/...`.

## Passenger Mobile Rules

Current passenger app conventions:

- Screens live in `src/screens/*.screen.tsx` and should stay thin.
- Domain logic belongs in `src/features/<feature>/`.
- Runtime/app shell state belongs in `src/application/`.
- HTTP access, adapters, and shared API types belong in `src/api/`.
- Reusable tokens/components belong in `src/design-system/`.
- Keep `@typescript-eslint/no-explicit-any` and `consistent-type-imports` clean.
- Validate with TypeScript and Zod-style feature validators before calling backend APIs.
- Keep sensitive data in secure storage, not AsyncStorage or plain app state.
- Use backend proxy endpoints for provider secrets such as Google Places server keys; never put server secrets in Expo public env.
- For native map/location/push changes, rebuild and verify real dev-client behavior; config-only gates are not production proof.

React Native and Expo security rules:

- Treat every `EXPO_PUBLIC_*` value as public and bundle-visible.
- Put real secrets in backend env, EAS secrets, or a future secret manager.
- Request permissions only in user-driven flows and include denied/unavailable UI states.
- For deep links into protected screens, preserve auth/profile gating behavior in `src/application/navigation.ts` and startup state.

## API Contracts And Shared Packages

- API contracts live in `docs/api/*.openapi.json` and are product/client contracts.
- Before wiring UI to backend data, compare OpenAPI paths with actual controllers/DTOs and update `docs/api/API_BACKEND_RECONCILIATION.md`.
- App-facing aliases are preferred for stable passenger/driver/admin client paths.
- Update `packages/api-contracts/src/index.ts` when endpoint inventory changes.
- Retry-safe mutations need idempotency keys where the docs/API require them.

## Database, Migrations, And Data Correctness

- Use Flyway SQL migrations under `apps/api/src/main/resources/db/migration/`.
- Continue the existing `VNNN__description.sql` sequence; do not rename applied migrations.
- One PostgreSQL database uses schema-per-module. Do not create cross-module shortcuts that bypass service/facade boundaries.
- Use `bigint generated always as identity` for internal IDs, UUID/public IDs only when needed, `timestamptz` for instants, `numeric` for money, and PostGIS geometry/geography where appropriate.
- Add indexes for foreign keys, ownership lookups, state transitions, and geospatial access paths.
- For route/location queries, prefer index-aware spatial predicates such as `ST_DWithin` with GiST indexes before exact scoring.
- Financial, booking, trip, payment, and seat-inventory changes need transaction tests and state-machine tests.

## Security, Privacy, And External Integrations

Auth model:

- Keycloak owns users, sessions, roles, and JWT issuing.
- Backend trusts bearer JWTs through Spring Security Resource Server and converts Keycloak realm plus `api-monolith` resource roles.
- Phone OTP access tokens are backend-issued and filtered separately from normal bearer JWT handling.
- One Keycloak subject can own passenger and/or driver business profiles.

Secrets and config:

- `.env.example` contains local-safe defaults only.
- `.env`, `.env.*`, `.docker/`, `.expo/`, `qa/reports/`, `artifacts/`, generated native folders, and build outputs stay uncommitted.
- Never log JWTs, refresh tokens, OTPs, SMS provider keys, Google API server keys, payment secrets, document contents, full bank details, or precise location history dumps.

Provider ports/adapters:

- Notify.lk stays behind `SmsGateway`/provider abstractions.
- Google Maps/Places server calls stay behind backend maps services; native map SDK keys need platform restrictions.
- Cybersource must stay backend-only behind a payment gateway abstraction with webhook signature/security validation when implemented.
- Firebase/Expo push, Sentry, object storage, email, production infra, and secret manager choices must follow `docs/development/PRODUCTION_EXTERNAL_SERVICES.md`.

## Verification Commands

Backend:

```bash
cd apps/api
./mvnw spotless:check test
./mvnw verify
```

Use `spotless:apply` only when formatting is intended:

```bash
cd apps/api
./mvnw spotless:apply spotless:check test
```

Runtime smoke:

```bash
./scripts/dev-up.sh
cd apps/api
./mvnw spring-boot:run
curl http://localhost:8080/actuator/health
```

Passenger mobile:

```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile test:e2e:android
pnpm --filter @routeshare/passenger-mobile build:preview:ios
pnpm --filter @routeshare/passenger-mobile build:preview:android
pnpm --filter @routeshare/passenger-mobile run doctor
```

Contracts:

```bash
pnpm --filter @routeshare/api-contracts typecheck
pnpm contracts:check
```

QA tools and Android flows:

```bash
scripts/qa-check-tools.sh
scripts/qa-passenger-android.sh
scripts/qa-passenger-dev-run.sh
```

Use focused tests while iterating, then run the relevant task gate before marking work complete. If an environment cannot run a gate, record the blocker and closest valid evidence in living docs.

## Dependency Review

- Inspect the package or Maven dependency's official docs, license, maintenance status, and native/build implications before adding it.
- Prefer existing stack choices and local helpers over new libraries.
- For mobile dependencies with native modules, update Expo/EAS config and test on Android/iOS dev clients.
- For backend dependencies, check Java 21/Spring Boot 3 compatibility and whether architecture tests need new guardrails.
- Update lockfiles intentionally and keep generated package/build output out of git.

## Performance Rules

Backend:

- Virtual threads help blocking I/O but do not remove database pool, index, timeout, and backpressure requirements.
- Keep Hikari pool bounds explicit and monitor before increasing limits.
- Use projections/read models for lists; avoid loading full graphs when only summaries are needed.
- Lock state rows and reserve inventory transactionally for booking/trip/payment mutations.
- Keep high-volume location ingestion logs sampled or DEBUG.

Mobile:

- Keep screen render paths light; derive display models in feature modules.
- Avoid unnecessary global store churn; keep server state in query/caching primitives when introduced.
- Keep map rendering verified on device, not only with static placeholders.
- Do not add hero/marketing layouts to operational screens; passenger app screens should optimize for task completion.

## Online-Verified References

Official/current references checked on 2026-06-16:

- Spring Boot Reference: `https://docs.spring.io/spring-boot/reference/`
- Spring Boot testing: `https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html`
- Spring Boot Actuator: `https://docs.spring.io/spring-boot/reference/actuator/index.html`
- Spring Boot virtual threads: `https://docs.spring.io/spring-boot/reference/features/spring-application.html`
- Oracle Java virtual threads: `https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html`
- Spring Security OAuth2 Resource Server JWT: `https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html`
- Spring Framework transaction management: `https://docs.spring.io/spring-framework/reference/data-access/transaction.html`
- Spring Data JPA reference: `https://docs.spring.io/spring-data/jpa/reference/`
- Flyway migrations: `https://documentation.red-gate.com/fd/migrations-271585107.html`
- Flyway versioned migrations: `https://documentation.red-gate.com/fd/versioned-migrations-273973333.html`
- Flyway validate migration naming: `https://documentation.red-gate.com/fd/flyway-validate-migration-naming-setting-277579047.html`
- PostgreSQL date/time types: `https://www.postgresql.org/docs/current/datatype-datetime.html`
- PostgreSQL wiki "Don't Do This": `https://wiki.postgresql.org/wiki/Don%27t_Do_This`
- PostGIS spatial indexes FAQ: `https://postgis.net/documentation/faq/spatial-indexes/`
- PostGIS `ST_DWithin`: `https://postgis.net/docs/ST_DWithin.html`
- MapStruct reference: `https://mapstruct.org/documentation/stable/reference/html/`
- Lombok constructors: `https://projectlombok.org/features/constructor`
- Maven Wrapper: `https://maven.apache.org/tools/wrapper/`
- JaCoCo Maven plugin: `https://www.eclemma.org/jacoco/trunk/doc/maven.html`
- JaCoCo check goal: `https://www.eclemma.org/jacoco/trunk/doc/check-mojo.html`
- Spotless Maven plugin: `https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md`
- Docker Compose environment variables: `https://docs.docker.com/compose/how-tos/environment-variables/set-environment-variables/`
- pnpm workspaces: `https://pnpm.io/workspaces`
- pnpm filtering: `https://pnpm.io/filtering`
- TypeScript TSConfig reference: `https://www.typescriptlang.org/tsconfig/`
- TypeScript compiler option guidance: `https://www.typescriptlang.org/docs/handbook/modules/guides/choosing-compiler-options.html`
- ESLint rules reference: `https://eslint.org/docs/latest/rules/`
- typescript-eslint `no-explicit-any`: `https://typescript-eslint.io/rules/no-explicit-any/`
- typescript-eslint `consistent-type-imports`: `https://typescript-eslint.io/rules/consistent-type-imports/`
- Vitest guide: `https://vitest.dev/guide/`
- Vitest config: `https://vitest.dev/config/`
- Expo environment variables: `https://docs.expo.dev/guides/environment-variables/`
- Expo EAS environment variables: `https://docs.expo.dev/eas/environment-variables/`
- Expo EAS build profiles: `https://docs.expo.dev/build/eas-json/`
- React Native security: `https://reactnative.dev/docs/security`
- React Navigation deep linking: `https://reactnavigation.org/docs/deep-linking/`
- React Navigation auth flow: `https://reactnavigation.org/docs/auth-flow/`
- TanStack Query React Native: `https://tanstack.com/query/v5/docs/framework/react/react-native`
- Expo Location: `https://docs.expo.dev/versions/latest/sdk/location/`
- Expo SecureStore: `https://docs.expo.dev/versions/latest/sdk/securestore/`
- Expo react-native-maps guide: `https://docs.expo.dev/versions/latest/sdk/map-view/`
- Expo push notifications: `https://docs.expo.dev/push-notifications/push-notifications-setup/`
- Sentry React Native Expo: `https://docs.sentry.io/platforms/react-native/manual-setup/expo/`
- Expo Sentry guide: `https://docs.expo.dev/guides/using-sentry/`
- Google Maps API security best practices: `https://developers.google.com/maps/api-security-best-practices`
- Google Cloud API key management: `https://docs.cloud.google.com/docs/authentication/api-keys`
- Google Places Autocomplete web service: `https://developers.google.com/maps/documentation/places/web-service/place-autocomplete`
- Google Maps reporting and monitoring: `https://developers.google.com/maps/reporting-and-monitoring/reporting`
- Google Maps cost management: `https://developers.google.com/maps/billing-and-pricing/manage-costs`
- Keycloak securing applications: `https://www.keycloak.org/docs/25.0.6/securing_apps/index.html`
- Notify.lk developer API: `https://developer.notify.lk/`
- Notify.lk endpoints: `https://developer.notify.lk/api-endpoints/`
- Cybersource Developer Center: `https://developer.cybersource.com/`
- Cybersource capture docs: `https://developer.cybersource.com/docs/cybs/en-us/payments/developer/gpn/rest/payments/payments-intro/payments-services-intro/payments-intro-processing-capture.html`
- Cybersource webhooks: `https://developer.cybersource.com/docs/cybs/en-us/webhooks/implementation/all/rest/webhooks.html`
- Firebase Cloud Messaging: `https://firebase.google.com/docs/cloud-messaging`
- OpenAPI Specification: `https://swagger.io/specification/`
- OpenAPI best practices: `https://learn.openapis.org/best-practices.html`
- OpenAPI security: `https://learn.openapis.org/specification/security.html`
- Maestro quickstart: `https://docs.maestro.dev/get-started/quickstart`
- Maestro docs: `https://docs.maestro.dev/`
- Conventional Commits: `https://www.conventionalcommits.org/en/v1.0.0/`
