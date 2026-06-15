# Selected Provider Implementation Guide

Last updated: 2026-06-14

This guide records how RouteShareApp should implement the selected production providers while building toward a public release. It complements `PRODUCTION_EXTERNAL_SERVICES.md`.

## Product direction

RouteShareApp should be built as a **real public-release application**, not a mock MVP. Local fakes are allowed only for automated tests and local development when explicitly gated. Production feature completion requires real provider integration evidence.

## Selected providers

- SMS/OTP: Notify.lk
- Maps/routing/places: Google Maps Platform
- Card payments: Cybersource
- Push notifications: Firebase Cloud Messaging
- Monitoring/error tracking: Sentry

## Cross-cutting implementation pattern

For every provider:

1. Define a backend/mobile-facing port/interface first.
2. Keep provider SDK/API calls inside infrastructure adapters.
3. Keep local/test fakes separate and clearly named `Fake*` or `InMemory*`.
4. Add config validation that fails closed in production when required credentials are missing.
5. Add masked structured logs and never log secrets, tokens, card data, OTP values, or full phone numbers.
6. Add provider-specific integration tests behind environment flags so CI can skip them without pretending production readiness.
7. Update `BLOCKERS.md`, `DEVELOPMENT_STATUS.md`, and the current implementation task file whenever credentials are missing.

## Notify.lk SMS/OTP

Use for:

- Phone OTP login/verification.
- Optional transactional SMS alerts if push fails or for critical account events.

Backend design:

```text
SmsProviderPort
- sendOtp(phoneNumber, message, correlationId)
- sendTransactionalSms(phoneNumber, message, correlationId)
- getDeliveryStatus(providerMessageId) optional
```

Implementation requirements:

- Implemented REST endpoint: `GET https://app.notify.lk/api/v1/send` with `user_id`, `api_key`, `sender_id`, `to` in `947XXXXXXXX` format, and `message`.
- Notify.lk account status endpoint from docs: `GET https://app.notify.lk/api/v1/status`.
- Notify.lk docs warn not to send OTP content through `NotifyDEMO`; RouteShare blocks demo sender OTP unless an explicit local override is set.
- Store OTP server-side only as a hash with expiry.
- Add resend cooldown, max attempts, per-phone/IP/device rate limits, and audit logs.
- Mask phone numbers in logs and API errors.
- Mobile must never verify OTP locally; it calls backend verify endpoint.

Credentials/config needed:

```text
NOTIFY_LK_API_BASE_URL
NOTIFY_LK_API_USER_ID or API key/account id
NOTIFY_LK_API_KEY / token
NOTIFY_LK_SENDER_ID
NOTIFY_LK_OTP_TEMPLATE_ID optional
NOTIFY_LK_ALLOW_DEMO_SENDER_FOR_OTP=false by default
NOTIFY_LK_OTP_MESSAGE_TEMPLATE
NOTIFY_LK_ENABLED=true/false
```

## Google Maps Platform

Use for:

- Mobile maps in passenger and driver apps.
- Admin live map.
- Places autocomplete.
- Geocoding/reverse geocoding.
- Directions/route polylines.
- Distance/duration estimates.
- Roads/Snap to Roads if used for map matching.

Backend ports:

```text
PlacesPort
GeocodingPort
DirectionsPort
DistanceMatrixPort
RoadsPort / MapMatchingPort
```

Mobile/admin config:

- Android Maps SDK key restricted to Android package/signing certificate.
- iOS Maps SDK key restricted to iOS bundle identifier.
- Web/admin Maps JavaScript key restricted to admin domain.
- Backend server key restricted by IP/service and enabled only for server APIs.

Credentials/config needed:

```text
GOOGLE_MAPS_SERVER_API_KEY
EXPO_PUBLIC_GOOGLE_MAPS_ANDROID_API_KEY
EXPO_PUBLIC_GOOGLE_MAPS_IOS_API_KEY
NEXT_PUBLIC_GOOGLE_MAPS_WEB_API_KEY
GOOGLE_MAPS_ENABLED=true/false
```

Likely APIs to enable:

- Maps SDK for Android
- Maps SDK for iOS
- Maps JavaScript API
- Places API
- Geocoding API
- Directions API
- Distance Matrix API
- Roads API / Snap to Roads if required

## Cybersource payments

Use for:

- Card authorization/preauthorization.
- Capture after final fare is known.
- Void authorization on cancellation.
- Refund/partial refund.
- Payment webhooks/reconciliation.

Backend-only port:

```text
PaymentGatewayPort
- authorize / preAuthorize
- capture
- voidAuthorization
- refund
- getPaymentStatus
- verifyWebhook
```

Implementation requirements:

- Use backend server-side integration only; mobile/admin must never receive merchant secret keys.
- Keep RouteShare PCI scope low: do not store card PAN/CVV.
- Prefer Cybersource hosted/tokenized payment flow where possible.
- Map Cybersource request IDs and reconciliation IDs into `payment` tables.
- Verify webhook signatures before mutating payment state.
- Keep immutable ledger entries for every capture/void/refund/cash/commission effect.
- Use idempotency keys for retryable payment operations.

Credentials/config needed:

```text
CYBERSOURCE_ENVIRONMENT=sandbox|production
CYBERSOURCE_MERCHANT_ID
CYBERSOURCE_KEY_ID
CYBERSOURCE_SHARED_SECRET or private key material depending integration mode
CYBERSOURCE_WEBHOOK_SECRET
CYBERSOURCE_CAPTURE_MODE=manual
CYBERSOURCE_ENABLED=true/false
```

Business/provider confirmations still needed:

- Acquiring bank/merchant account setup.
- Whether Cybersource account supports auth + later capture for RouteShare business model.
- Void/refund support and settlement timing.
- Webhook event list and signing setup.

## Firebase Cloud Messaging

Use for:

- Booking/trip/KYC/payment/SOS/support notifications.

Backend port:

```text
PushNotificationPort
- sendToUser
- sendToDevice
- sendTopic
```

Implementation requirements:

- Mobile registers device push token after permission grant.
- Backend stores token with platform, app, app version, locale, user id, and last-seen time.
- Handle token refresh and invalid-token cleanup.
- Add delivery log/audit records.
- Notification tap should deep-link to booking/trip/payment/support screens.

Credentials/config needed:

```text
FIREBASE_PROJECT_ID
FIREBASE_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS
EXPO_PUBLIC_FIREBASE_PROJECT_ID
GOOGLE_SERVICES_JSON path for Android
GOOGLE_SERVICE_INFO_PLIST path for iOS
PUSH_NOTIFICATIONS_ENABLED=true/false
```

## Sentry

Use for:

- Passenger mobile.
- Driver mobile.
- Admin web.
- Backend API.

Implementation requirements:

- Separate Sentry projects or environments per app/service.
- Enable release/environment tagging.
- Upload source maps for mobile/admin where applicable.
- Filter PII and never send tokens/card data/OTP/full phone numbers.
- Add backend correlation IDs to Sentry context.

Credentials/config needed:

```text
SENTRY_ORG
SENTRY_AUTH_TOKEN
SENTRY_DSN_PASSENGER
SENTRY_DSN_DRIVER
SENTRY_DSN_ADMIN
SENTRY_DSN_BACKEND
EXPO_PUBLIC_SENTRY_DSN
SENTRY_ENVIRONMENT=staging|production
SENTRY_ENABLED=true/false
```

## Implementation order for app work

- Passenger Task 06 profile/avatar: add storage abstraction if file upload becomes in-scope; otherwise document storage provider pending.
- Passenger Task 07 home/search/location: implement Google Maps config and provider-backed places/geocoding/directions where the screen requires it.
- Passenger Task 10 payment methods/payment intents: implement Cybersource adapter before marking card payment production-ready.
- Passenger Task 11 live trip: implement FCM notification registration if trip alerts are shown.
- Passenger Task 13 notifications/support/account: implement FCM end-to-end delivery and Sentry breadcrumbs/errors.
- Hardening/release: verify Sentry, EAS builds, production config validation, and provider integration smoke tests.
