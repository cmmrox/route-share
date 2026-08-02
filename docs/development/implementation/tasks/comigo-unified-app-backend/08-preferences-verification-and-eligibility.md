---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 08 — Preferences, Verification and Eligibility

**Goal:** Let a driver say who may ride with them, let a passenger prove who they are, and make the server — not the client — decide which trips a given rider is allowed to see.

**Depends on:** 01, 03.
**Blocks:** 09, 13.

## Objective

Two of the driving preferences narrow who may book, and both are safety features rather than filters:
women-only trips (offered only to NIC-verified female drivers) and verified-riders-only trips. The
prototype is emphatic that these are enforced **server-side**: "Riders see this on your trip before they
book, so nobody wastes a request", and a rider who is not eligible must never receive the trip at all.

Passenger verification is the other half. It is explicitly **not** a booking gate — it is a ranking
signal, a badge, and the key that unlocks verified-only trips. Every capture is taken with the in-app
camera; a gallery photo is not accepted, because the whole value of the selfie-with-NIC is that it could
not have been assembled beforehand.

## Scope

In scope:

- Driving preferences: gender policy, verified-only, approve-each-request, mid-trip bookings, early-drop requests, chat enabled.
- Gender on the passenger profile, derived from NIC at verification.
- Passenger identity verification: four captures, camera-only, levels `NONE|PENDING|VERIFIED|REJECTED`.
- Profile photo visibility `PUBLIC | MATCHED | HIDDEN`, with the driver's photo always visible to a confirmed rider.
- Eligibility enforcement in search and at booking.
- Verification as a ranking signal in the driver's request list.

Out of scope:

- Search result shaping and radius — slice 09 consumes the eligibility predicate built here.
- Driver KYC documents — already implemented; this slice only reads their state.
- The "hide my number" toggle — cut by decision D5.

## Source material / references

- `docs/source-assets/comigo-prototype/driver-supply.jsx` — D35 driving preferences, D13 per-trip who-can-ride.
- `docs/source-assets/comigo-prototype/passenger-identity.jsx` — P28 why verify, P29a–d captures, P30/P30b/P30c photo visibility, P31a–c outcomes.
- `docs/source-assets/comigo-prototype/data.jsx` — `VERIFY_STEPS`, `PAX_VERIFY`, `DRIVER_PREFS`, `VERIFIED_PAX_SHARE`, `verifiedRidesShare`, `POLICY.verifyCameraOnly`, `verifyExpires`.
- `docs/source-assets/comigo-prototype/passenger-discover.jsx` — P07 women-only and verified-only banners on ride detail.
- Current code: `passenger/entity/PassengerDocumentEntity.java`, `passenger/controller/PassengerAppReadinessController.java`, `storage/**`.

## Architecture and design notes

**Eligibility is a server predicate with three inputs**: the trip's gender policy, the trip's
verified-only flag, and the rider's profile. It is applied in exactly two places — the search query and
the booking guard — and both call the same `EligibilityService` so they cannot disagree. A trip the rider
cannot book must not appear in results; P36's copy about not wasting requests depends on it.

**Women-only is gated twice.** A driver may only *set* it if their own NIC verification says female
(D35: "Your NIC verifies you as female"), and a rider may only *book* it if hers does. Setting it without
verification returns `WOMEN_ONLY_NOT_AVAILABLE`.

**Gender comes from verification, not self-declaration.** It is written by the verification decision, is
not user-editable, and is never exposed on any public profile — only used as an eligibility input.

**Camera-only is enforced by the upload contract.** The presigned upload request carries a
`captureSource` of `CAMERA`, and the client attests per capture with a capture timestamp and a
short-lived, server-issued capture session id bound to that step. This is deterrence, not proof — a
determined client can lie — so the review step remains human and the field is recorded for the reviewer.

**Verification never blocks booking.** Assert it by test: a `NONE`-level rider can complete a booking on
any trip that is not verified-only. P31a's copy ("Book, pay and ride as normal") is a product promise.

**Photo visibility has an asymmetry, deliberately.** A rider may hide her photo from everyone including
her driver; a driver cannot — "she is getting into your car and has to know it's you" (D35). So the
driver's photo is always returned to a confirmed rider, and never in search.

**Ranking signal.** Verified riders sort above unverified ones in the driver's request list. This is a
sort key, not a filter, and belongs in the request-list query from slice 07.

## API contracts involved

Driver:

```
GET  /api/v1/driver/preferences
PUT  /api/v1/driver/preferences
     { genderPolicy, verifiedRidersOnly, approveEachRequest, midTripBookings, earlyDropRequests, chatEnabled }
GET  /api/v1/driver/preferences/eligibility-impact   -> D35's "cost you 3 requests last week", verified rider share
```

Per-trip override at publish time: `POST /api/v1/driver/routes` and the occurrence update accept
`genderPolicy` and `verifiedRidersOnly`, defaulted from preferences.

Passenger:

```
GET  /api/v1/passenger/verification                 -> level, steps[], benefits[], verifiedOn, rejection reason
POST /api/v1/passenger/verification/sessions        -> capture session id + the four required steps
POST /api/v1/passenger/verification/steps/{key}/upload-url  { captureSource: CAMERA, sessionId }
POST /api/v1/passenger/verification/steps/{key}/submit
POST /api/v1/passenger/verification/submit          -> moves to PENDING
GET  /api/v1/passenger/profile/photo-visibility
PUT  /api/v1/passenger/profile/photo-visibility     { visibility }
```

Admin: `POST /api/v1/admin/passenger-verifications/{id}/decide { decision, gender?, rejectedSteps[], note }`.

New errors: `WOMEN_ONLY_NOT_AVAILABLE`, `NOT_ELIGIBLE_WOMEN_ONLY`, `NOT_ELIGIBLE_VERIFIED_ONLY`,
`CAPTURE_SOURCE_NOT_ALLOWED`, `VERIFICATION_SESSION_EXPIRED`, `VERIFICATION_ALREADY_PENDING`.

`/me/context` — `passenger.verificationLevel` now real; `driver.canSetWomenOnly` added.

## Database / migration changes

**`V035__preferences_verification_eligibility.sql`** — `V034` was taken by slice 07's booking depth.

- New `driver.driving_preference`:
  `driver_profile_id PK FK`, `gender_policy TEXT CHECK (gender_policy IN ('ANYONE','WOMEN_ONLY')) DEFAULT 'ANYONE'`,
  `verified_riders_only BOOLEAN DEFAULT false`, `approve_each_request BOOLEAN DEFAULT true`,
  `mid_trip_bookings BOOLEAN DEFAULT true`, `early_drop_requests BOOLEAN DEFAULT true`,
  `chat_enabled BOOLEAN DEFAULT true`, `updated_at`.
- `routing.route_occurrence` — add `gender_policy TEXT DEFAULT 'ANYONE'`, `verified_riders_only BOOLEAN DEFAULT false`. Copied from preferences at generation, overridable until frozen.
- `passenger.passenger_profile` — add
  `verification_level TEXT CHECK (verification_level IN ('NONE','PENDING','VERIFIED','REJECTED')) DEFAULT 'NONE'`,
  `verified_at`, `gender TEXT CHECK (gender IN ('FEMALE','MALE','UNSPECIFIED')) DEFAULT 'UNSPECIFIED'`,
  `photo_visibility TEXT CHECK (photo_visibility IN ('PUBLIC','MATCHED','HIDDEN')) DEFAULT 'MATCHED'`.
- New `passenger.verification_session`:
  `id`, `app_user_id FK`, `status TEXT CHECK (status IN ('OPEN','SUBMITTED','APPROVED','REJECTED','EXPIRED'))`,
  `created_at`, `expires_at`, `submitted_at`, `decided_at`, `decided_by_app_user_id`, `decision_note`.
- New `passenger.verification_step`:
  `id`, `session_id FK`, `step_key TEXT CHECK (step_key IN ('NIC_FRONT','NIC_BACK','SELFIE_WITH_NIC','PROFILE_PHOTO'))`,
  `document_id FK NULL`, `capture_source TEXT CHECK (capture_source IN ('CAMERA'))`,
  `captured_at`, `status TEXT CHECK (status IN ('PENDING','SUBMITTED','APPROVED','REJECTED'))`,
  `rejection_reason TEXT`, `UNIQUE (session_id, step_key)`.
  The `capture_source` CHECK allowing only `CAMERA` is the schema-level expression of `POLICY.verifyCameraOnly`.
- `driver.driver_profile` — add `gender TEXT` (written by driver KYC review; needed for the women-only set gate).
- Index `idx_occurrence_eligibility ON routing.route_occurrence(gender_policy, verified_riders_only) WHERE status = 'PUBLISHED'`.
- New `routing.eligibility_denial`: `id`, `route_occurrence_id FK`, `app_user_id FK`, `reason`,
  `surface TEXT CHECK (surface IN ('SEARCH','BOOKING'))`, `denied_at`. **Added during implementation
  and not in the original plan.** D35's "cost you 3 requests last week" cannot be derived from
  anything else: a rider filtered out of search never makes a request, so the omission is the whole
  event and leaves no other trace. Without this table `eligibility-impact` would return zero for
  every driver forever.

## Configuration / environment changes

- Policy setting `VERIFICATION_SESSION_TTL_MINUTES` (default `30`). **Specified as an environment
  variable and implemented as a policy setting instead:** D1 forbids a rule the product states from
  also existing as a value in a file, and support needs to change it without a deploy.
- Policy setting `VERIFY_CAMERA_ONLY` (default `true`) — leaving a switch, since a support-assisted path may be needed later.
- Policy setting `VERIFIED_RIDES_SHARE_PCT` (default `34`) — P28's "how many more requests get accepted", stated once.
- `ROUTESHARE_VERIFICATION_REVIEW_SLA_HOURS` (default `24`) — an alerting threshold rather than a
  product rule, so this one stayed an env var. Drives the pending-over-SLA gauge.
- No new secrets; captures use the existing S3 presigned lifecycle.

## UI / UX requirements

Backend slice. The contract must supply:

- P28 — the three benefits with their copy, and the share of requests verified riders get accepted.
- P29a–d — the four steps with label, hint and guide shape, in order, with the session.
- P30/b/c — the three visibility options with their descriptions, and the current choice.
- P31a/b/c — level, per-step status, which step was rejected and why, and what verification unlocked.
- D35 — every preference with its current value, plus what verified-only cost in requests and the verified rider share on the driver's routes.
- D13 — the per-trip overrides and whether women-only is even offered to this driver.
- P07 — the women-only and verified-only banners with copy that matches the rider's own eligibility.

## Implementation steps

1. Add `driver.driving_preference` with defaults; expose get/put; guard `WOMEN_ONLY` on the driver's own verified gender.
2. Copy preferences onto each generated occurrence; allow override until frozen (slice 07's predicate).
3. Add passenger verification level, gender and photo visibility columns.
4. Build the verification session + step model on the existing document upload lifecycle; issue a session, require `captureSource=CAMERA` and a `capturedAt`, bind uploads to the session and step.
5. Implement submit → `PENDING`, admin decide → `VERIFIED` (writing gender) or `REJECTED` with per-step reasons; notify.
6. Implement photo visibility with the driver asymmetry: resolve a photo URL through `PhotoVisibilityService.resolve(viewer, subject, bookingState)`, used by every profile-bearing response.
7. Build `EligibilityService.canBook(riderProfile, occurrence)` returning an allow/deny with a typed reason.
8. Apply it as a filter predicate in the search query (slice 09 consumes the same predicate) and as a guard at booking creation.
9. Add the verified-first sort key to the driver's booking-request list from slice 07.
10. Compute `eligibility-impact`: requests turned away by verified-only in the last 7 days, and the verified share of riders on this driver's corridors.
11. Extend `/me/context` with the real verification level and `canSetWomenOnly`.

## Files expected to change

- `apps/api/.../driver/**` — preferences entity/service/controller, gender on profile.
- `apps/api/.../passenger/**` — verification sessions/steps, level, gender, photo visibility, `PhotoVisibilityService`.
- `apps/api/.../routing/**` — occurrence eligibility columns, search predicate, `EligibilityService`.
- `apps/api/.../booking/**` — booking eligibility guard, request-list sort key.
- `apps/api/.../admin/**` — passenger verification decisions.
- `apps/api/.../platform/**` — `/me/context` additions.
- `apps/api/src/main/resources/db/migration/V035__preferences_verification_eligibility.sql`.
- `apps/api/src/test/java/**` — eligibility matrix tests, camera-only enforcement, photo visibility matrix, women-only set/book gates, verification-never-blocks-booking test.
- `docs/api/mobile-app.openapi.json`, `docs/api/admin-web.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/08-preferences-verification-and-eligibility-qa.md`

Maestro: not applicable — no mobile surface in this slice. The camera-only capture flow is exercised on
device in the mobile feature plan, which must link back to this task's `captureSource` contract.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='EligibilityServiceTest,EligibilitySearchIT,PhotoVisibilityMatrixTest,CameraOnlyEnforcementTest,WomenOnlyGateTest,VerificationNeverBlocksBookingTest,EligibilityPrivacyTest' test
```

```bash
ROUTESHARE_API_BASE=http://localhost:8088 ROUTESHARE_DB_NAME=routeshare_comigo bash scripts/simulation/verify-eligibility.sh
```

The smoke must prove: an unverified rider's search never returns a verified-only trip; a male rider's
search never returns a women-only trip; both receive a typed denial if they call booking directly with
the id; an unverified rider can still book an ordinary trip; a non-female driver cannot set women-only.

## Security, privacy, and observability checks

- **NIC images are the most sensitive data in the system.** They stay in the existing private bucket, reachable only by short-lived presigned URLs, readable only by the owner and verification agents. Never returned in a list response.
- The NIC number is stored encrypted and must never appear in any rider-facing payload (D02's own note).
- Gender is an eligibility input only. Assert by contract test that it appears in no public profile, no search result, and no booking response.
- Eligibility denial must not leak *why* a trip is hidden in search — a rider learning "this trip is women-only" from an absence is fine; enumerating a driver's policy through the API is not. Denials at booking may state the reason; search simply omits.
- Photo visibility must be evaluated server-side on every read. A `HIDDEN` photo URL must never be emitted, even to be ignored by the client.
- Audit every verification decision with the reviewer, the per-step outcomes and the gender written.
- Metrics: `routeshare_verification_submissions_total`, `routeshare_verification_decisions_total{outcome}`, `routeshare_eligibility_denials_total{reason}`, gauge of pending verifications older than the SLA.

## Done criteria

- [x] All six driving preferences stored, defaulted onto occurrences, overridable until frozen.
- [x] Women-only can only be set by a verified female driver and booked by a verified female rider — and the per-trip override checks the same gate, or it would be the way round it.
- [x] Verified-only trips are absent from an unverified rider's search and refused at booking with a typed reason. Search never says why.
- [x] Verification is provably not a booking gate (`VerificationNeverBlocksBookingTest`).
- [x] Four capture steps, camera-only at the schema level, session-bound, human-reviewed.
- [x] Photo visibility honours all three modes and the driver asymmetry.
- [x] Verified riders sort first in the driver's request list.
- [x] `eligibility-impact` returns real counts for D35, from `routing.eligibility_denial`.
- [x] `./mvnw spotless:check verify` green — 551 tests, 0 skipped, JaCoCo held.
- [x] Runtime: `verify-eligibility.sh` 50/50 against a live stack with `V035` applied.
- [x] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): add driving preferences, passenger verification and server-side eligibility"
```
