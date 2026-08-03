# RouteShareApp Production External Services Matrix

Last updated: 2026-06-14 — provider decisions recorded

Purpose: keep production provider dependencies explicit so implementation does not fake provider-backed behavior. A feature is production-ready only when the corresponding provider is selected, configured, integrated, tested, and documented here.

Status legend:

- `LOCAL/DEV PRESENT` — local development infrastructure or code abstraction exists, but production provider/credentials are not finalized.
- `PROVIDER NEEDED` — production vendor/account/API keys are required before the feature can be marked release-ready.
- `DECISION NEEDED` — business/vendor choice is not final.
- `IMPLEMENTATION NEEDED` — code integration still needs to be built after provider selection.
- `CAN IMPLEMENT VIA PORT` — safe to implement behind an abstraction without choosing the final provider.


## Provider decisions recorded 2026-06-14

The product direction is **public production release**, not a throwaway MVP. Implementation must build real provider-backed integrations as part of the application work, while still using clean ports/adapters and test doubles for automated tests.

Selected providers:

- SMS/OTP: **Notify.lk**
- Maps/routing/places: **Google Maps Platform**
- Card payments: **Cybersource**
- Push notifications: **Firebase Cloud Messaging** through `expo-notifications`
- Monitoring/error tracking: **Sentry**

Deferred/not needed for now unless the implementation reaches that dependency:

- Production object storage provider
- Production email provider
- Production hosting/domain/TLS details
- Production database/Redis/Redpanda hosting details
- Secrets manager choice

Implementation rule: for the selected providers above, future feature tasks should implement the real provider adapter when that feature is built, not only placeholders. If credentials are not yet available, add the provider adapter contract/config shape, keep the feature gated, and list the exact credential required.

## Executive summary

| Area | Production service | Current repo status | Why it is needed | Release impact |
|---|---|---|---|---|
| Identity/authz | Keycloak | Local Keycloak is planned/present in docs; passenger app has PKCE helper foundation | One user can be passenger and/or driver; roles/admin sessions/JWTs | Mandatory |
| Phone verification/login | **Notify.lk** SMS/OTP gateway | Provider selected; credentials/API docs still needed; passenger app gates phone OTP until enabled | Send and verify real phone OTPs | Mandatory for public release phone login |
| Maps/routing/places | **Google Maps Platform** | Provider selected; API keys/project restrictions still needed | Pickup/drop, route creation, route matching, live maps | Mandatory |
| Push notifications | **Firebase Cloud Messaging** / Expo notifications | Provider selected; Firebase project/service credentials still needed | Booking/trip/payment/KYC/SOS alerts | Mandatory for public release UX |
| Card payments | **Cybersource** | Provider selected; merchant credentials/capture/refund/webhook details still needed | Preauth, capture final fare, void/refund, reconciliation | Mandatory for card payments/public release |
| Object storage | S3-compatible storage | Local MinIO present/planned; production provider not selected | KYC docs, licence, vehicle docs/photos, profile photos, support evidence | Mandatory for KYC/driver onboarding |
| Email | SES/SendGrid/Mailgun/Brevo | Provider not selected | Admin invites, receipts, support/KYC updates, ops alerts | Strongly recommended; required for admin/support polish |
| Crash/error monitoring | **Sentry** | Provider selected; DSNs/auth token/projects still needed | Mobile/backend/admin runtime error visibility | Mandatory for public release |
| Product analytics | Firebase Analytics/PostHog/Mixpanel | Mentioned in docs; not finalized | Onboarding/search/booking/payment funnel visibility | Recommended for MVP |
| Metrics/observability | Prometheus/Grafana/OpenTelemetry/log stack | Hardening docs mention stack; production stack not finalized | Backend health, latency, failures, live tracking/payment monitoring | Mandatory for serious production |
| Database | Production PostgreSQL + PostGIS | Local Postgres/PostGIS exists/planned; production hosting undecided | System of record and geospatial matching | Mandatory |
| Realtime/cache | Production Redis | Local Redis exists/planned; production hosting undecided | Latest locations, OTP/rate limits, live state, cache | Mandatory |
| Event streaming | Redpanda/Kafka | Local Redpanda planned/present; Phase 06 event foundation noted | Location/trip/payment/notification event pipeline | Recommended for MVP; mandatory as live load grows |
| Secrets | Secret manager or disciplined env management | `.env.example` exists with local-only placeholders | Protect provider keys, DB passwords, signing/client secrets | Mandatory |
| Mobile distribution | Expo EAS + app-store accounts | EAS config/scaffold exists; cloud submissions blocked by credentials | Production Android/iOS builds, signing, release channels | Mandatory for public release |
| Hosting/domain/TLS | DNS + HTTPS/WSS reverse proxy | Not finalized | Public API/admin/auth URLs, callbacks/webhooks, secure mobile traffic | Mandatory |

## 1. Keycloak — authentication and authorization

Current repo evidence:

- `docs/development/IMPLEMENTATION_ROADMAP.md` marks local Keycloak and Keycloak JWT role converter as foundation items.
- `docs/development/implementation/11-AUTH-KEYCLOAK-USER-MANAGEMENT.md` documents the one-user-many-roles model.
- Passenger mobile Task 05 added Keycloak PKCE/token/refresh helper foundation.

Why needed:

- The same real user can be `PASSENGER`, `DRIVER`, or both.
- Admin/support/finance/verification roles need central RBAC.
- Backend must trust JWT roles and map Keycloak subject to local app profiles.

Implementation needed:

- Production/staging Keycloak deployment.
- Realm `routeshare`.
- Clients: `passenger-mobile`, `driver-mobile`, `admin-web`, `api-monolith`.
- Roles: `PASSENGER`, `DRIVER`, `ADMIN`, `SUPPORT_AGENT`, `VERIFICATION_AGENT`, `FINANCE_ADMIN`, `OPS_ADMIN`, `SUPER_ADMIN`.
- Mobile OIDC Authorization Code + PKCE end-to-end login.
- Backend Spring Security resource-server validation and role converter.
- Admin user-management flow.

Provider decision needed:

- Self-hosted Keycloak vs managed Keycloak provider.

Release rule:

- Do not mark login/role switching production-ready until tested against staging/prod Keycloak, not only local mocks/helpers.

## 2. SMS/OTP gateway

Current repo evidence:

- Passenger Task 05 explicitly gates phone OTP behind `EXPO_PUBLIC_AUTH_PHONE_OTP_SUPPORTED`.
- `docs/development/BLOCKERS.md` says production phone OTP requires Keycloak/provider support.
- Current mobile OTP tests cover validation/state only, not real SMS delivery.

Why needed:

- Real phone number verification and phone OTP login require outbound SMS and server-side verification.

Implementation needed:

- Backend `SmsProviderPort`.
- OTP request/verify endpoints or Keycloak-compatible OTP integration.
- OTP hashing, expiry, resend cooldown, retry limits, rate limits, audit logs.
- Delivery-status logging if provider supports it.
- Mobile UI should stay gated until provider is enabled.

Selected provider: **Notify.lk**.

Implementation notes:

- Use Notify.lk only through `SmsProviderPort`; do not call it directly from controllers/screens.
- Keep `FakeSmsProvider` for local automated tests only.
- Add provider request/response logging with phone-number masking.
- Add retry/rate-limit handling around provider errors.
- Notify.lk endpoint/auth format confirmed from `https://developer.notify.lk/`: `/api/v1/send` accepts `user_id`, `api_key`, `sender_id`, `to`, and `message`; `/api/v1/status` returns account active/balance data.
- Backend adapter implemented under `apps/api/src/main/java/com/routeshare/identity/provider/NotifyLkSmsGateway.java`; mobile calls backend OTP endpoints and never sees the Notify.lk API key.

Provider data needed:

- API base URL, API user id/key, approved production sender ID, template approval requirements, pricing, delivery status API. Current account screenshot shows `NotifyDEMO` only; production OTP requires a RouteShare-approved sender ID or explicit non-production override.

Release rule:

- No fake fixed OTP or mocked SMS success in production builds.

## 3. Maps, places, geocoding, directions, and route snapping

Current repo evidence:

- Architecture/implementation docs recommend Google Maps Platform for MVP.
- Passenger mobile includes map-related dependencies/config hooks.
- Backend phases use PostGIS for exact matching but still require provider-generated routes/geocoding.

Why needed:

- Pickup/drop selection.
- Place autocomplete.
- Driver route creation.
- Passenger route search.
- Route polyline/directions.
- Distance/duration estimates.
- Live trip map.
- Admin live map.
- Optional snap-to-road/map matching.

Implementation needed:

- Backend ports: `GeocodingPort`, `PlacesPort`, `DirectionsPort`, `DistanceMatrixPort`, `MapMatchingPort`/`RoadsPort`.
- Mobile Android/iOS map keys.
- Admin web map key.
- API key restrictions by platform/bundle/package/domain.

Selected provider:

- **Google Maps Platform** for production/public release.

Required Google APIs likely:

- Maps SDK for Android
- Maps SDK for iOS
- Maps JavaScript API
- Places API
- Geocoding API
- Directions API
- Distance Matrix API
- Roads API / Snap to Roads if used

Release rule:

- Route search/matching can use local fixtures for tests, but production pickup/drop/route creation requires real map provider keys and quota monitoring.

## 4. Push notifications

Current repo evidence:

- Passenger/driver/admin contracts and docs include notification-related APIs.
- Expo notifications are part of the planned mobile stack.

Why needed:

- Booking request/confirmation.
- Driver accepted/declined.
- Trip started/arriving/completed.
- Passenger cancellation.
- KYC approval/rejection.
- Payment/receipt/payout updates.
- SOS/support escalation.

Implementation needed:

- Firebase project.
- FCM server credentials/service account.
- Mobile token registration and refresh.
- Backend `PushNotificationPort` and delivery log.
- Notification preferences and read/unread state.
- Foreground/background click-routing in mobile apps.

Selected provider:

- **Firebase Cloud Messaging** via `expo-notifications`.

Release rule:

- Production notification flows require real device token delivery tests, not only local API/unit tests.

## 5. Payment gateway

Current repo evidence:

- Backend Phase 05 has payment intent, capture, void, refund, cash collection, fare ledger, receipts, driver earnings and settlement read models.
- `CybersourcePaymentGateway` implements tokenization, authorization, capture, void, refund and
  signed webhook handling behind the provider port.
- A default-off `LocalFakePaymentGateway` exists only to exercise the same lifecycle in local QA.
  It is not a provider emulator or production readiness evidence.

Why needed:

- Card payment preauthorization.
- Capture final fare after actual/matched trip distance.
- Void authorization for cancellations.
- Refund/partial refund.
- Gateway webhooks and reconciliation.
- Driver settlement and platform commission accounting.

Implementation needed:

- `PaymentGatewayPort` already conceptually required.
- Provider adapter after vendor selection.
- Webhook signature verification.
- Idempotency mapping to provider idempotency keys.
- Admin reconciliation screens/reports.
- PCI-safe design; never store raw card data.

Selected provider:

- **Cybersource**.

Implementation notes:

- Integrate through `PaymentGatewayPort`.
- Use Cybersource REST APIs/server-side SDK from the backend only.
- Keep all card handling PCI-safe; do not store raw PAN/CVV in RouteShare systems.
- Confirm auth/preauthorization, capture, void, refund, webhook/security-key setup, and Sri Lankan acquiring/bank configuration before enabling production card payments.

Provider decision criteria:

- True authorize/preauth support.
- Later capture support.
- Void/refund API.
- Webhooks.
- Sandbox quality.
- Sri Lankan entity/acquiring support.
- Fees and settlement timing.

Release rule:

- Card payments cannot be marked production-ready until the selected gateway is integrated and webhook/reconciliation tests pass. Cash-only pilot can proceed with internal ledger/cash commission receivable if business accepts it.

## 6. Object storage for documents/images

Current repo evidence:

- Local MinIO appears in architecture/roadmap docs.
- KYC/document upload/storage remains listed as an external dependency/blocker.

Why needed:

- Driver NIC/passport images.
- Selfie/driver photo.
- Driving licence.
- Vehicle registration/revenue licence/insurance.
- Vehicle photos.
- Profile photos.
- Support/dispute attachments.

Implementation needed:

- Backend `ObjectStoragePort`.
- Presigned upload/download URLs.
- Metadata tables only in PostgreSQL.
- Admin signed document preview with authorization checks.
- Malware/content-type/size checks where practical.
- Retention/deletion policy.

Recommended providers:

- Production: AWS S3 or Cloudflare R2.
- Local/dev: MinIO.

Release rule:

- KYC/driver onboarding cannot be production-ready until document upload, secure read, and admin review flows use real storage.

## 7. Email provider

Current repo evidence:

- Email fields and support/admin communication are present in API contracts/docs, but provider is not selected.

Why needed:

- Admin invites/password/account notices.
- Receipts if required.
- Driver verification updates.
- Support/dispute messages.
- Operational alerts.

Implementation needed:

- `EmailProviderPort`.
- Template rendering.
- Delivery logging.
- Domain verification/SPF/DKIM.

Recommended providers:

- Amazon SES.
- Alternatives: SendGrid, Mailgun, Brevo.

Release rule:

- Admin/support email workflows require provider verification and at least staging delivery tests.

## 8. Sentry/error monitoring

Current repo evidence:

- Passenger app config includes Sentry hooks/env.
- Hardening roadmap requires Sentry/error monitoring.

Why needed:

- Mobile crash visibility.
- Backend exception visibility.
- Admin web runtime error visibility.
- Release regression detection.

Implementation needed:

- Sentry projects/DSNs for passenger, driver, admin, backend.
- Source map upload for mobile/web.
- Release/environment tagging.
- PII filtering.

Selected provider:

- **Sentry** for passenger mobile, driver mobile, admin web, and backend.

Release rule:

- Production release should not ship without Sentry enabled and verified for each released app/service.

## 9. Product analytics

Current repo evidence:

- App readiness/docs mention analytics; provider not finalized.

Why needed:

- Understand onboarding, search, booking conversion, cancellations, payment failure points, retention.

Implementation needed:

- Event taxonomy.
- Privacy rules.
- Mobile/web analytics SDK.
- Backend business metrics where needed.

Recommended providers:

- Firebase Analytics for MVP.
- PostHog/Mixpanel later if needed.

Release rule:

- Not a blocker for a closed technical pilot, but important for real public MVP learning.

## 10. Observability: metrics, logs, traces

Current repo evidence:

- Hardening roadmap includes OpenTelemetry, Prometheus, Grafana, structured logs and critical metrics.

Why needed:

- Route search latency.
- Booking conflict/overbooking detection.
- Location ingestion delay.
- WebSocket disconnects.
- Payment failures.
- OTP abuse/rate limits.
- Backend/database health.

Implementation needed:

- Spring Boot Actuator/Micrometer.
- Prometheus scrape endpoint.
- Grafana dashboards.
- Structured JSON logs.
- OpenTelemetry trace export.
- Alerting rules.

Release rule:

- Minimum health/metrics/logging must exist before production; full tracing can be hardened incrementally.

## 11. Production PostgreSQL + PostGIS

Current repo evidence:

- Local PostgreSQL/PostGIS infrastructure and Flyway migrations are core to phases 00-06.

Why needed:

- System of record for profiles, routes, bookings, trips, fares, payments, KYC metadata, admin audit, support, ratings.
- PostGIS is required for proximity and route overlap matching.

Implementation needed:

- Production database hosting.
- Backups and restore drill.
- Migration pipeline.
- Connection pool config.
- Monitoring.
- Least-privilege DB credentials.

Release rule:

- Production cannot run on local/dev database credentials or without backup policy.

## 12. Production Redis

Current repo evidence:

- Local Redis and Phase 06 latest-location cache are in docs/status.

Why needed:

- Latest active trip/driver location.
- OTP/rate-limit counters.
- Live state cache.
- Short-lived cache/session support.

Implementation needed:

- Managed or self-hosted Redis.
- Auth/TLS where possible.
- TTL policy.
- Monitoring and eviction policy.

Release rule:

- Live trip and OTP/rate-limit behavior require production Redis or an explicitly accepted temporary alternative.

## 13. Redpanda/Kafka event streaming

Current repo evidence:

- Local Redpanda/Kafka-compatible broker is part of the roadmap.
- Phase 06 says location events are published to Redpanda/Kafka.

Why needed:

- Location raw/matched events.
- Trip events.
- Booking events.
- Payment events.
- Notification events.
- Audit events.

Implementation needed:

- Production Redpanda/Kafka service.
- Topic definitions.
- Producer/consumer configs.
- Retry/dead-letter policy.
- Idempotent consumers.
- Outbox pattern where needed.

Release rule:

- Small MVP can keep some events in-process if documented, but live location/payment/notification scale requires a real broker.

## 14. Secrets management

Current repo evidence:

- `.env.example` has local placeholders and local-only development credentials.

Why needed:

- Protect Keycloak secrets, DB/Redis passwords, SMS keys, map keys, Firebase service account, payment credentials, storage credentials, Sentry tokens, email credentials.

Implementation needed:

- `staging.env.example` and `prod.env.example` placeholders.
- Secret loading strategy for deployment.
- No secrets in Git/mobile source.
- Rotate local leaked/dev credentials before production.

Provider options:

- Environment variables for first VPS deployment.
- Doppler, AWS Secrets Manager, HashiCorp Vault, cloud secret manager later.

Release rule:

- Production credentials must not be committed; mobile public keys must be platform-restricted.

## 15. Mobile build/distribution

Current repo evidence:

- Passenger mobile has EAS config and preview config gates.
- `BLOCKERS.md` says real EAS preview submissions remain blocked until EAS credentials/distribution targets are finalized.

Why needed:

- Android/iOS production builds.
- App signing credentials.
- Release channels/profiles.
- Internal testing/TestFlight/Play Store distribution.

Implementation needed:

- Expo account/project.
- EAS project credentials.
- Android package and signing.
- Google Play Console.
- Apple Developer account/TestFlight if iOS is targeted.
- Staging/production environment profiles.

Release rule:

- Local build/scaffold tests are not a substitute for real store/internal testing builds.

## 16. Hosting, domain, DNS, TLS, and reverse proxy

Current repo evidence:

- Production endpoint/domain is not finalized in current visible config.

Why needed:

- Public API URL.
- Admin web URL.
- Keycloak auth URL.
- WebSocket WSS URL.
- SMS/payment webhook callback URLs.
- HTTPS security.

Implementation needed:

- Domain/DNS.
- TLS certificates.
- Reverse proxy/load balancer with WebSocket support.
- Production API/admin/auth hostnames.
- Firewall/security groups.

Recommended simple MVP stack:

- Cloudflare DNS.
- Caddy or Nginx.
- Let’s Encrypt TLS.
- Docker Compose or simple container deployment first.

Release rule:

- Production apps must call HTTPS/WSS endpoints, not local IPs or dev URLs.

## Implementation policy going forward

Provider-backed features must use this rule:

1. Add a port/interface first when the final vendor is not selected.
2. Add local/dev fake adapter only where clearly marked as local/dev.
3. Gate UI flows with explicit capability flags until real provider config exists.
4. Update this matrix, `BLOCKERS.md`, and task files when provider credentials are obtained.
5. Do not claim production readiness until real provider integration is tested.

## Decisions and credentials still required from the user

Selected and no longer open:

1. SMS provider: **Notify.lk**.
2. Maps provider: **Google Maps Platform**.
3. Payment provider: **Cybersource**.
4. Push provider: **Firebase Cloud Messaging**.
5. Monitoring provider: **Sentry**.

Still needed before real production integrations can be enabled:

1. Notify.lk account/API credentials, sender ID, and API docs/account endpoint details.
2. Google Cloud project, enabled Maps APIs, billing, and restricted Android/iOS/web/server API keys.
3. Cybersource merchant account/sandbox credentials, key ID/secret or key material, webhook secret, and confirmation of auth/capture/void/refund support.
4. Firebase project, Android `google-services.json`, iOS `GoogleService-Info.plist` when iOS is in scope, and backend service account credentials.
5. Sentry org/projects/DSNs/auth token for passenger, driver, admin, and backend.
6. Later/deferred: object storage provider, hosting target, domain name/hostnames, production DB/Redis/Redpanda hosting, and secrets manager choice.

## Next implementation slices recommended

1. Add provider-port packages/config for SMS, maps, payment, storage, push, email.
2. Expand `.env.example`/staging/prod env examples with all external service placeholders.
3. Keep passenger mobile Task 06+ flows provider-gated where backend/provider support is missing.
4. Implement Keycloak staging login end-to-end before marking auth complete.
5. Implement Google Maps provider before final route discovery/search UI.
6. Implement object storage before driver KYC or profile avatar production claims.
7. Implement SMS provider before enabling phone OTP in production.
8. Implement payment gateway only after preauth/capture/refund support is confirmed by the selected vendor.

## 2026-06-21 — Phase 06.6 implementation status (backend adapters built + gated)

All selected providers now have **real backend adapters behind ports + capability flags** (Phase
06.6 A–J). The backend boots cleanly against the project Docker stack with everything disabled; each
integration goes live by flipping its flag and supplying credentials. See
`docs/development/DEPLOYMENT.md` §3 for the exact flag → credential table.

| Provider | Adapter | Flag to enable |
|---|---|---|
| Notify.lk SMS/OTP | `NotifyLkSmsGateway` (pre-existing) + Redis OTP rate limiting | `NOTIFY_LK_ENABLED` |
| Google Maps | `RouteMetricsAdapter` (Distance Matrix → fare/ETA) + `GooglePlaceSearchServiceImpl` | `GOOGLE_MAPS_ENABLED` |
| Cybersource | `CybersourcePaymentGateway` (authorize/capture/void/refund/tokenize + webhook) | `CYBERSOURCE_ENABLED` |
| Local payment QA only | `LocalFakePaymentGateway` (opaque in-memory lifecycle; never staging/production) | `ROUTESHARE_LOCAL_FAKE_PAYMENT_ENABLED` |
| Firebase FCM | `FcmPushAdapter` + notification domain | `PUSH_NOTIFICATIONS_ENABLED` |
| Object storage | `S3ObjectStorageAdapter` (presigned KYC/doc lifecycle) | `OBJECT_STORAGE_ENABLED` |
| Sentry | Spring Boot starter (backend) | `SENTRY_DSN_BACKEND` |
| Kafka/Redpanda | outbox relay + `KafkaEventSender` | `ROUTESHARE_EVENTS_KAFKA_ENABLED` |
| Keycloak | realm import + admin role propagation (`KeycloakRealmRoleAdapter`) | `ROUTESHARE_KEYCLOAK_USER_SYNC_ENABLED` |

Still required from the business to flip to live: the actual credentials/accounts listed above
(sandbox first), production Postgres/Redis/Redpanda hosting, object-storage bucket, domain/DNS/TLS,
and EAS/app-store credentials for the mobile apps.
