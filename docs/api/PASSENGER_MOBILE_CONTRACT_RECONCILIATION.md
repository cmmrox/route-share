# Passenger Mobile Contract Reconciliation

Last Updated: 2026-06-14 00:16 +0530

## Scope

This reconciles the passenger mobile contract against `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, and current Spring Boot passenger/app-facing controllers. The passenger mobile typed client must hide runtime envelopes and DTO drift from screens.

## Status vocabulary

- `MATCHED` — OpenAPI and runtime shape are directly usable by the client.
- `RUNTIME_ENVELOPE` — Runtime wraps response in `ApiResponse<T>` while OpenAPI documents raw data; client unwraps centrally.
- `DTO_MISMATCH` — Runtime request/response fields differ from OpenAPI/design naming; client adapter normalizes.
- `READINESS_PLACEHOLDER` — Endpoint exists for app workflow readiness but is not the final production integration.
- `DEFERRED_PRODUCTION` — Not implemented for this production slice.

## Summary

- Passenger contract operations reconciled: `47`.
- Central client unwraps backend `ApiResponse<T>` envelopes.
- DTO adapters cover saved places, trusted contacts, ride search results, bookings, and payment intents.
- Booking creation requires an explicit `Idempotency-Key` from the typed module.
- Screens must use `apps/passenger-mobile/src/api` modules only; no raw screen-level `fetch`.

## Endpoint reconciliation

| Method | Path | Status | Runtime notes |
|---|---|---|---|
| `GET` | `/api/v1/app/config` | `MATCHED` | Public raw Map response; no Authorization header required. Client uses auth:false. |
| `GET` | `/api/v1/auth/me` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<AuthMeResponse>; client unwraps centrally. |
| `GET` | `/api/v1/passenger/profile` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<PassengerProfileResponse>; OpenAPI documents raw schema. |
| `PUT` | `/api/v1/passenger/profile` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<PassengerProfileResponse>; OpenAPI documents raw schema. |
| `POST` | `/api/v1/passenger/profile/avatar-upload` | `READINESS_PLACEHOLDER` | PassengerAppReadinessController stores workflow payload; not real binary storage/provider integration. |
| `GET` | `/api/v1/passenger/verification/status` | `READINESS_PLACEHOLDER` | Returns optional/passenger verification status placeholder. |
| `POST` | `/api/v1/passenger/verification/documents` | `READINESS_PLACEHOLDER` | Workflow payload placeholder; not full document storage/review. |
| `GET` | `/api/v1/passenger/saved-places` | `DTO_MISMATCH` | Runtime is ApiResponse<List<SavedPlaceResponse>> with id/label/address/latitude/longitude; client adapts to savedPlaceId/location. |
| `POST` | `/api/v1/passenger/saved-places` | `DTO_MISMATCH` | Runtime request/response uses flat latitude/longitude; client adapter normalizes response. |
| `GET` | `/api/v1/passenger/saved-places/{savedPlaceId}` | `DTO_MISMATCH` | Live controller path variable is {id}; OpenAPI/client use stable {savedPlaceId}. Spring path matches by position; response adapted. |
| `PUT` | `/api/v1/passenger/saved-places/{savedPlaceId}` | `DTO_MISMATCH` | Live controller path variable is {id}; OpenAPI/client use stable {savedPlaceId}; response adapted. |
| `DELETE` | `/api/v1/passenger/saved-places/{savedPlaceId}` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse deleted map; client unwraps centrally. |
| `GET` | `/api/v1/passenger/trusted-contacts` | `DTO_MISMATCH` | Runtime is ApiResponse<List<TrustedContactResponse>> with id/name/phone/relationship; client adapts to contactId/phoneNumber. |
| `POST` | `/api/v1/passenger/trusted-contacts` | `DTO_MISMATCH` | Runtime request uses phone; mobile model exposes phoneNumber adapter for responses. |
| `GET` | `/api/v1/passenger/trusted-contacts/{contactId}` | `DTO_MISMATCH` | Live controller path variable is {id}; OpenAPI/client use stable {contactId}; response adapted. |
| `PUT` | `/api/v1/passenger/trusted-contacts/{contactId}` | `DTO_MISMATCH` | Live controller path variable is {id}; OpenAPI/client use stable {contactId}; response adapted. |
| `DELETE` | `/api/v1/passenger/trusted-contacts/{contactId}` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse deleted map; client unwraps centrally. |
| `POST` | `/api/v1/passenger/ride-searches` | `DTO_MISMATCH` | Runtime returns ApiResponse<List<RouteSearchResponse>> and uses coordinate/requestedDepartureTime/seats DTO; OpenAPI is design-facing. |
| `GET` | `/api/v1/passenger/ride-searches/{searchId}/results` | `READINESS_PLACEHOLDER` | Readiness workflow list from AppReadinessService; production projection can replace later without screen raw fetch. |
| `GET` | `/api/v1/passenger/ride-searches/{searchId}/results/{resultId}` | `READINESS_PLACEHOLDER` | Readiness detail placeholder; production detail projection can replace later. |
| `POST` | `/api/v1/passenger/bookings` | `DTO_MISMATCH` | Runtime requires Idempotency-Key and occurrence/fraction booking request; client module requires idempotencyKey. |
| `GET` | `/api/v1/passenger/bookings` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<List<PassengerBookingSummaryResponse>>. |
| `GET` | `/api/v1/passenger/bookings/{bookingId}` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<PassengerBookingDetailResponse>. |
| `POST` | `/api/v1/passenger/bookings/{bookingId}/cancel` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse transition map. |
| `POST` | `/api/v1/passenger/bookings/{bookingId}/early-drop-off` | `READINESS_PLACEHOLDER` | Readiness workflow endpoint; later full trip/fare side effects may harden. |
| `GET` | `/api/v1/passenger/bookings/{bookingId}/receipt` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<ReceiptResponse>. |
| `POST` | `/api/v1/passenger/bookings/{bookingId}/share` | `READINESS_PLACEHOLDER` | Readiness workflow share payload. |
| `POST` | `/api/v1/passenger/bookings/{bookingId}/share-link` | `READINESS_PLACEHOLDER` | Readiness workflow share-link payload. |
| `POST` | `/api/v1/passenger/bookings/{bookingId}/rating` | `READINESS_PLACEHOLDER` | Readiness workflow rating payload. |
| `GET` | `/api/v1/passenger/trips/current` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<PassengerBookingDetailResponse|null>. |
| `GET` | `/api/v1/passenger/trips/history` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<List<PassengerBookingSummaryResponse>>. |
| `GET` | `/api/v1/passenger/trips/{tripId}/live-state` | `RUNTIME_ENVELOPE` | Runtime returns ApiResponse<PassengerLiveTripStateResponse>. |
| `GET` | `/api/v1/passenger/payment-methods` | `READINESS_PLACEHOLDER` | Readiness workflow records; no real provider/card vault yet. |
| `POST` | `/api/v1/passenger/payment-methods` | `READINESS_PLACEHOLDER` | Readiness workflow records; no real provider/card vault yet. |
| `DELETE` | `/api/v1/passenger/payment-methods/{paymentMethodId}` | `READINESS_PLACEHOLDER` | Marks workflow record deleted. |
| `POST` | `/api/v1/passenger/payment-methods/{paymentMethodId}/default` | `READINESS_PLACEHOLDER` | Marks workflow record default. |
| `POST` | `/api/v1/passenger/payments/intents` | `DTO_MISMATCH` | Runtime request is bookingId-centered PaymentIntentRequest; response is ApiResponse<Map>; client adapts id/amount/currency. |
| `GET` | `/api/v1/passenger/notifications` | `READINESS_PLACEHOLDER` | Readiness workflow list. |
| `POST` | `/api/v1/passenger/notifications/{notificationId}/read` | `READINESS_PLACEHOLDER` | Readiness workflow mark-read. |
| `GET` | `/api/v1/passenger/notification-preferences` | `READINESS_PLACEHOLDER` | Readiness preference map. |
| `PUT` | `/api/v1/passenger/notification-preferences` | `READINESS_PLACEHOLDER` | Readiness preference save. |
| `POST` | `/api/v1/passenger/push-registrations` | `READINESS_PLACEHOLDER` | Readiness push registration; provider push integration later. |
| `POST` | `/api/v1/passenger/sos-events` | `READINESS_PLACEHOLDER` | Readiness SOS event record; emergency escalation integration later. |
| `GET` | `/api/v1/passenger/support/tickets` | `READINESS_PLACEHOLDER` | Readiness workflow list. |
| `POST` | `/api/v1/passenger/support/tickets` | `READINESS_PLACEHOLDER` | Readiness workflow create. |
| `GET` | `/api/v1/passenger/support/tickets/{ticketId}` | `READINESS_PLACEHOLDER` | Readiness workflow detail. |
| `POST` | `/api/v1/passenger/support/tickets/{ticketId}/messages` | `READINESS_PLACEHOLDER` | Readiness workflow message. |

## Client implementation notes

- `apps/passenger-mobile/src/api/api-client.ts` owns bearer injection, timeout, retry behavior for configured transient failures, JSON parsing, typed HTTP errors, central envelope unwrap, and redacted logging.
- `apps/passenger-mobile/src/api/config.ts` resolves `EXPO_PUBLIC_API_BASE_URL` / `ROUTESHARE_API_BASE_URL` and defaults to `http://localhost:8080` for local development.
- `apps/passenger-mobile/src/api/modules.ts` exposes endpoint modules for app config, auth, profile, saved places, trusted contacts, ride search, bookings, payments, trips, notifications, support, and safety.
- `apps/passenger-mobile/src/api/adapters.ts` isolates known runtime/OpenAPI naming mismatches from UI code.

## Verification evidence

- `pnpm --filter @routeshare/passenger-mobile lint` — passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` — passed.
- `pnpm --filter @routeshare/passenger-mobile test` — passed: 3 files / 23 tests.
- Native E2E/preview commands are blocked until Task 02 creates the Expo/native app scaffold; see `docs/development/BLOCKERS.md`.
