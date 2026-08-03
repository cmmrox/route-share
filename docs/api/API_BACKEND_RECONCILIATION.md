# API Backend Reconciliation — ComiGo Mobile Contract

Generated: 2026-08-01 (slice 00 — repo reset and contract rewrite)
Last regenerated: 2026-08-03 (slice 10 — chat, notifications, safety and support)

Source of truth: `docs/api/mobile-app.openapi.json`. This document is derived from its
`x-routeshare-status` extension — regenerate it rather than editing by hand.

## 1. What changed

`passenger-app.openapi.json` and `driver-app.openapi.json` are **merged and deleted**. ComiGo ships
one mobile application containing both experiences (Decision 011), so it has one client contract.
`admin-web.openapi.json` is unaffected.

The `/api/v1/passenger/**` and `/api/v1/driver/**` namespaces are **retained**: they are role-scoped
resource namespaces, not app boundaries. Mode is a client concept; the server authorises on role,
resource ownership and gate state.

## 2. Status summary

| Status | Operations | Meaning |
| --- | --- | --- |
| `IMPLEMENTED` | 188 | Live in `apps/api` today |
| `PLANNED_SLICE_07` | 1 | Remaining alternatives read contract |
| `PLANNED_SLICE_09` | 1 | Remaining driver share metadata read |
| `PLANNED_SLICE_11` | 6 | Specified; built in slice 11 |
| `PLANNED_SLICE_12` | 6 | Specified; built in slice 12 |
| `PLANNED_SLICE_13` | 8 | Specified; built in slice 13 |
| `PLANNED_SLICE_14` | 8 | Specified; built in slice 14 |
| `PLANNED_SLICE_15` | 6 | Specified; built in slice 15 |
| `INTERNAL_NOT_FOR_CLIENTS` | 7 | Implemented, outside the mobile surface |
| `CUT` | 2 | Deliberately removed from the product |
| **Total** | **233** | across 195 paths, 161 schemas |

## 3. Screen coverage

All **157** app screens in `docs/source-assets/comigo-prototype/prototype-nav.jsx`
map to at least one contract operation. The 9 reference boards (B01–B05, S14, X01, X02, V00) are
design specifications, not app screens, and are excluded.

| Group | Screens | Covered |
| --- | --- | --- |
| Onboarding & sign-in | 9 | 9/9 |
| Passenger · find a ride | 8 | 8/8 |
| Passenger · book & pay | 13 | 13/13 |
| Passenger · during & after | 19 | 19/19 |
| Shared · account & history | 5 | 5/5 |
| Shared · inbox & prefs | 3 | 3/3 |
| Shared · verification | 5 | 5/5 |
| Shared · support & safety | 6 | 6/6 |
| Mode switching & gates | 7 | 7/7 |
| Passenger · identity & verification | 11 | 11/11 |
| Shared · referral & rewards | 5 | 5/5 |
| Passenger · joining a moving trip | 5 | 5/5 |
| Passenger · when the driver is late | 2 | 2/2 |
| Driver · becoming one | 7 | 7/7 |
| Driver · publishing trips | 27 | 27/27 |
| Driver · running the trip | 14 | 14/14 |
| Driver · money | 11 | 11/11 |
| **Total** | **157** | **157/157** |

## 4. Findings from the merge

Reconciling two independently-maintained contracts against the running backend surfaced five real
defects. All are fixed in the merged document.

### 4.1 `SosRequest.tripId` had two different types

The driver contract declared `tripId` as a **uuid string**; the passenger contract declared it as
**int64**. `trip.trip.trip_id` is `BIGINT`, so the driver contract was wrong and a generated driver
client would have failed at runtime. The passenger shape is now canonical.

### 4.2 22 implemented endpoints were absent from both contracts

Including the entire Places and directions surface the search screens depend on, the presigned
document-upload lifecycle, and stored route geometry. Twelve are now declared as `IMPLEMENTED`; ten
were generic endpoints duplicating role-scoped ones and are marked `INTERNAL_NOT_FOR_CLIENTS`.

### 4.3 Two contract endpoints were never implemented

`POST /api/v1/driver/documents` and `POST /api/v1/driver/vehicles/{vehicleId}/documents` were direct
uploads superseded by the presigned `upload-url` → `submit` lifecycle in Phase 06.6. Marked `CUT`.

### 4.4 `nullable` was used throughout, which is invalid in OpenAPI 3.1

Both documents declared `openapi: 3.1.0` while using the 3.0 `nullable` keyword — 17 occurrences,
silently ignored by 3.1 tooling. Converted to type unions (`["string", "null"]`).

### 4.5 Path parameter names disagree between contract and backend

The contract uses `{savedPlaceId}`, `{contactId}` and `{routeId}` where the controllers use `{id}`
and `{ruleId}`. Harmless at runtime — path parameters are positional — but a generated client takes
its argument names from the contract. Left as-is; the contract names are the better ones.

## 5. New in this slice

**Slice 10 — chat, notifications, safety and support.** All fourteen operations originally stamped
for Slice 10 are now `IMPLEMENTED`, and the reconciliation also adds three live operations omitted
from the original planned inventory:

- `GET /api/v1/sos-events/{id}`
- `POST /api/v1/support/tickets/{ticketId}/attachments/upload-url`
- `POST /api/v1/support/tickets/{ticketId}/attachments/{attachmentId}/submit`

The admin contract adds the reason-required audited chat read and the account-request queue. The
role-split notification paths remain for one release as deprecated compatibility endpoints; the
client surface is now `/api/v1/notifications`, `/api/v1/notification-preferences` and
`/api/v1/badges`.

Both OpenAPI 3.1 documents validate. While touching the admin contract, its eight pre-existing
OpenAPI 3.0 `nullable` keywords were converted to 3.1 type unions; this changes no runtime shape.

Current inventory: **188 implemented, 2 older planned, 34 planned in slices 11–15, 7 internal and
2 cut**, for 233 operations total.

**Slice 06 — penalties, dues and compensation.** The five operations planned for this slice are now
`IMPLEMENTED` and carry real schemas in place of the `{"type": "object"}` placeholders they were
stamped with in slice 00:

- `GET /api/v1/passenger/dues` → `Dues` (P25, and P25b's empty state as an explicit `settled` flag)
- `GET /api/v1/passenger/penalties`, `GET /api/v1/driver/penalties` → `Penalty[]`
- `POST /api/v1/passenger/penalties/{penaltyId}/dispute`,
  `POST /api/v1/driver/penalties/{penaltyId}/dispute` → `Penalty`, with `409` carrying
  `PENALTY_ALREADY_DISPUTED` or `DISPUTE_WINDOW_CLOSED`

`Penalty` states every figure P26, P27, D21, D30, D31 and D41 name — base, percentage, fee and both
halves — so no screen has to derive one and risk deriving it differently from the ledger.
`amountForViewer` is negative when the penalty charged the caller and positive when it paid them,
which is how D26 renders both directions from one list. `beneficiaries[]` carries a **first name and
an amount and nothing else**; a surname, phone or email on a fee notice would be a disclosure.

Changed rather than added:

- `CancellationTerms` (slice 05) gains `fareBase`, `penaltyAmount`, `penaltyVictimShare` and
  `penaltyPlatformShare`, so P26 states a figure instead of a percentage.
- `Booking` gains `appliedDues`, `appliedDuesAmount` and `totalDue` for P09d's carried-over fees.

`admin-web.openapi.json` gains `GET /api/v1/admin/penalties`, `GET /api/v1/admin/penalty-disputes`
and `POST /api/v1/admin/penalty-disputes/{disputeId}/decide`.

**Correction carried from slice 05:** eleven of its operations were added to the contract without an
`x-routeshare-status` stamp, so they counted as neither implemented nor planned. They are stamped
`IMPLEMENTED` and the tables above are regenerated from the document.

## 5.1 New in slice 00

`GET /api/v1/me/context` — the app shell's single read. Serves screens S07–S14 in one request:
available modes, driver status with gate reasons, verification level, suspension detail with reason
and case reference, the active-trip pointer for the resume bar, outstanding dues, rewards balance and
tab badges. Served separately that is eight or more calls on every cold start.

Two behaviours worth knowing:

- A **suspended** caller is answered, not refused. Every business endpoint rejects a suspended account, but the shell must render S13 with the reason and the appeal route, so this endpoint resolves the user without the ACTIVE guard and reports status as data.
- Fields owned by later slices return **zero values, never null**, so the mobile shell is stable from day one.

`GET /api/v1/auth/me` is deprecated in favour of it, and still works.

## 6. Planned operations by slice

| Slice | Ops | Area |
| --- | --- | --- |
| 07 | 1 | Remaining alternatives read |
| 09 | 1 | Remaining driver share metadata read |
| 11 | 6 | Referral and rewards |
| 12 | 6 | Real-time location pipeline |
| 13 | 8 | Live en-route booking |
| 14 | 8 | Money operations |
| 15 | 6 | Ratings v2 |

## 7. Endpoints outside the mobile client surface

Implemented, but the app must use the role-scoped equivalent instead.

| Operation | Use instead |
| --- | --- |
| `POST /api/v1/bookings` | `/api/v1/passenger/bookings` |
| `PATCH /api/v1/bookings/{bookingId}/status` | `/api/v1/driver/bookings/{bookingId}/approve|decline` |
| `POST /api/v1/payments/intents` | `/api/v1/passenger/payments/intents` |
| `POST /api/v1/routes` | `/api/v1/driver/routes` |
| `POST /api/v1/routes/search` | `/api/v1/passenger/ride-searches` |
| `POST /api/v1/trips/{id}/transition` | `the role-scoped trip operations under /api/v1/driver/trips` |
| `PATCH /api/v1/trips/{tripId}/passengers/{bookingId}/state` | `/api/v1/driver/trips/{tripId}/passengers/{bookingId}/*` |

## 8. Cut

| Operation | Reason |
| --- | --- |
| `POST /api/v1/driver/documents` | Superseded by the presigned lifecycle: POST /upload-url -> POST /{documentId}/submit. |
| `POST /api/v1/driver/vehicles/{vehicleId}/documents` | Superseded by the presigned lifecycle on the same resource. |
| `POST /api/v1/pricing/estimate` | Accepted a client-supplied distance; replaced by estimate-by-route in slice 03. |

## 9. Verification

```bash
npx @redocly/cli lint docs/api/mobile-app.openapi.json
pnpm --filter @routeshare/api-contracts typecheck
```

The document lints clean with zero errors and zero warnings.
