# ComiGo Unified App — Prototype vs Backend Gap Analysis

Date: 2026-07-31
Source of record: `ComiGo Prototype (Standalone).html` (decoded to 28 JSX modules, ~157 screen states, 9 spec boards)
Backend audited: `apps/api` @ `0fe3149` — 456 main Java files, 65 test files, 20 modules, Flyway V001–V026

## 1. What the prototype actually is

It is not a set of mockups. It is a **specification with an executable policy engine**. `data.jsx` contains a
`POLICY` object that every screen reads from, so the business rules are stated once and are unambiguous:

| Rule | Value | Screens |
| --- | --- | --- |
| Card charge timing (scheduled trip) | authorize at booking, **capture at trip start** | P09, P11, P12, D15, D17 |
| Card charge timing (trip already moving) | **capture at driver accept** | P09c, P37, D16b |
| Commission | 10%, **inside** the fare (driver nets 90%) | all money screens |
| Fare formula | `gross = onRouteKm × vehicle.ratePerKm` → `price = gross − routeMatchDiscount` | P07, P08, P09, D13 |
| Route-match discount | ≥95% → 10%, ≥75% → 8%, ≥45% → 5%, else 2.5% | P07, P09 |
| Per-km rate | driver picks inside an **admin-set min–max band per vehicle** | D39, D39b, D39c, D40 |
| Vehicle classes | Car 38–62 (3 seats), SUV 46–74 (4), Van 40–68 (6), Tuk 26–42 (2) | D07, D13, P07 |
| Search radius | filters on where the **driver's trip starts**, options 5/10/20 km, ceiling 20 km | P03, P04 |
| Passenger cancel after start | 20% of fare | P26 |
| Passenger no-show | 25% of fare | P27, D21, P38 |
| Driver late to pickup | 10 min grace → free passenger cancel, 20% driver penalty | P34, D41 |
| Driver cancels published trip | free >12 h out, 20% penalty inside 12 h | D30, D30b, D31 |
| **Every penalty splits 50/50** | half to the victim (as credit), half to platform | P22, P26, P27, D21, D31, D41 |
| Pickup wait | 5 min + one 5-min extension, clock starts on GPS arrival | D19, D19b, P38, P38b |
| Start buffer | 10 min + one 10-min extension, then auto-cancel | D32, D32b, D32c, P24, P35 |
| Missed starts | 3 per calendar month → driver profile deactivated | D32b, D34 |
| Early drop-off | fare recalculated on actual distance, **2× per calendar month** | P16, P16b, D22b |
| Payouts | **weekly, Friday, LKR 1000 floor**, below floor rolls over | D25, D27, D33, D33b |
| Referral | 1% of a referred rider's fare, 2% of a referred driver's net; 12 months or 50 trips; paid **out of commission** | P32, D37 |
| Rewards balance | shared; rider spends as credit (no floor), driver withdraws ≥ LKR 1000 in the Friday batch | P33, D38, D38b |
| Verification | never a booking gate — a ranking signal + unlocks verified-only trips; **in-app camera only** | P28–P31c |
| Chat | scoped to one booking; opens on confirm, closes 24 h after drop-off | P23, D36 |
| Reviews | mutual, named, published together, **one reply each** | P18, D24, D28, P39 |
| Request expiry | scheduled 30 min; live (at the wheel) ~45 s | D16, D16b, P37 |

## 2. Screen inventory

| Group | Count | Prefixes |
| --- | --- | --- |
| Onboarding & sign-in | 9 | S01–S06 |
| Passenger · discovery | 8 | P01–P07 |
| Passenger · book & pay | 13 | P08–P14, P22, P24 |
| Passenger · during & after | 19 | P15–P21, P23, P25–P27, P38, P39 |
| Passenger · identity & verification | 11 | P28–P31c |
| Passenger · joining a moving trip | 5 | P36–P37c |
| Passenger · driver is late | 2 | P34, P35 |
| Shared · account/history/settings | 5 | S15–S18 |
| Shared · inbox & prefs | 3 | S22, S22b, S23 |
| Shared · verification | 5 | S19–S21c |
| Shared · support & safety | 6 | S24–S28 |
| Mode switching & gates | 7 | S07–S13 |
| Shared · referral & rewards | 5 | P32, P33, D37, D38, D38b |
| Driver · becoming one | 7 | D01–D07 |
| Driver · publishing | 27 | D08–D16e, D30–D32c, D35, D39–D40 |
| Driver · running the trip | 14 | D17–D24, D36, D41 |
| Driver · money | 11 | D25–D29c, D33, D33b, D34 |
| **Total app states** | **~157** | |
| Reference boards (specs, not screens) | 9 | B01–B05, S14, X01, X02, V00 |

## 3. Backend today — what already exists and is production-grade

Verified by reading source, migrations and controllers. This is a strong base; most of it survives.

| Module | State | Notes |
| --- | --- | --- |
| identity | ✅ | Phone OTP (Notify.lk), Keycloak realm roles, `app_user`, suspend/activate + history, `/auth/me` already returns `availableAppModes` |
| passenger | ✅ | profile, saved places, trusted contacts, documents (presigned lifecycle) |
| driver | ✅ | profile, KYC docs, payout profile, verification-status derivation |
| vehicle | ✅ | vehicle + documents + admin review |
| routing | ✅ | PostGIS `route_plan` LineString, `route_occurrence`, recurring schedule rules, bucket cells, matching settings, share links, search |
| booking | ✅ | book (idempotent), status history, driver approve/decline, cancel, early drop-off, receipt, trip share |
| trip | ✅ | state machine, pre-trip checklist, arrived/start/complete, board/no-show/drop-off, passenger trip states |
| payment | ✅ | Cybersource gateway (authorize/capture/void/refund/tokenize), tokenized payment methods, signed webhooks, fare ledger, cash collection, fare-adjustment request, driver earnings |
| notification | ✅ | notifications + preferences + push registrations, FCM adapter, delivery logs |
| support / safety / rating | ✅ | tickets w/ reopen-on-reply, SOS events, per-booking rating |
| location | ✅ | driver location updates, live state, PostGIS trail |
| maps | ✅ | Places autocomplete/details, Directions, Distance Matrix, Redis caching, session tokens, per-user rate limits, cooldown breaker |
| admin | ✅ | users, docs, finance (commission/fare policy/settlements/payout batches/adjustments), ops, reports + CSV, broadcasts, safety, support, matching settings |
| storage | ✅ | S3 presigned upload/download, fail-closed adapter |
| common | ✅ | event outbox + relay, idempotency keys, Redis rate limiter, Caffeine identity cache, architecture tests |

## 4. Gap register

Legend — **MISSING**: nothing exists. **MISMATCH**: exists but built to a different rule. **PARTIAL**: foundations exist, product behaviour missing.

### 4.1 Money & pricing — the largest block

| # | Capability | Screens | State | Evidence |
| --- | --- | --- | --- | --- |
| M1 | Per-vehicle rate band (min/max/chosen, set by admin) | D39, D39b, D39c, D40, P07, P08 | **MISSING** | `vehicle.vehicle` has no rate columns; no band endpoints |
| M2 | Vehicle class (maxSeats + default band) | D07, D13, P07 | **MISSING** | `seat_count` is free 1–12, no class |
| M3 | Band factors (age / insurance / fuel / service, signed deltas) | D39, D40 | **MISSING** | — |
| M4 | Driver picks rate inside band; rate re-assessment request (3-day SLA) | D39, D40 | **MISSING** | — |
| M5 | Fare formula `km × rate − matchDiscount`, commission **inside** | P07, P09, D13 | **MISMATCH** | `FareCalculator` = base 250 + 90/km + 5/min, **fee added on top** |
| M6 | Route-match discount tiers (10/8/5/2.5%) | P07, P09, P17 | **MISSING** | no discount concept in `FareBreakdown` |
| M7 | Capture at trip start | P11, P12, D15, D17 | **MISSING** | no `capture(...)` call anywhere in `trip/service/impl` |
| M8 | Capture at driver accept (en-route) | P09c, P37b, D16b | **MISSING** | — |
| M9 | Penalty domain, all split 50/50 victim/platform | P22, P26, P27, D21, D31, D41 | **MISSING** | no penalty table, no split accounting |
| M10 | Passenger outstanding dues carried to next booking (cash passengers) | P25, P25b, P09d | **MISSING** | — |
| M11 | Referral graph + earnings (1%/2%, 12 mo / 50 trips, from commission) | P32, D37, S05 | **MISSING** | S05 is literally tagged "NEEDS BACKEND" in the prototype |
| M12 | Shared rewards balance; rider spends as credit, driver withdraws ≥1000 | P33, D38, D38b, P09e | **MISSING** | — |
| M13 | Weekly (Friday) payout with LKR 1000 floor + held balance | D25, D27, D33, D33b | **PARTIAL** | `finance.payout_batch` exists; no cadence, floor, held state, or driver-facing endpoint |
| M14 | Ledger kinds: fare / fee / adjust / penalty / **comp** / payout | D26 | **PARTIAL** | only `BOOKING_FARE_ESTIMATE`, `PAYMENT_CAPTURED`, `PLATFORM_COMMISSION`, `DRIVER_EARNING`, `FARE_FINALIZED` |
| M15 | Early-drop allowance 2 per calendar month, 3rd = fare stands | P16, P16b, D22b | **PARTIAL** | recalculation exists; no allowance counter |
| M16 | Fare-adjustment admin decision + 48 h dispute window | D29–D29c, P17c | **PARTIAL** | request exists; no decision workflow or dispute |
| M17 | Cash trip commission netted off next payout | D23, D27 | **PARTIAL** | cash collection recorded; netting rule not modelled |

### 4.2 Booking, seats and the second request type

| # | Capability | Screens | State |
| --- | --- | --- | --- |
| B1 | Named seat selection (front / back), seat identity on the booking | P08, D16b | **MISSING** — `booking.seats` is a count only |
| B2 | Per-trip approval mode (instant vs approve-each) | P09, P09b, D13, D35 | **MISSING** |
| B3 | Scheduled request 30-min expiry + `EXPIRED` state | D16, P11 | **MISSING** |
| B4 | **LIVE (en-route) booking**: joinable-trip search, server-side "driver still behind pickup" filter, ~45 s prompt, capture on accept | P36–P37c, D16b–D16d | **MISSING** — whole subsystem |
| B5 | Seat freed by early drop-off goes back on sale for remaining leg | D18b, D22b, P16 | **MISSING** |
| B6 | Typed seat-race conflict (`SEATS_TAKEN`) | P14 | **PARTIAL** — inventory is transactional; no typed 409 contract |
| B7 | "Two open requests at once" rule | P11 | **MISSING** |
| B8 | Request lapsed because the seat sold meanwhile | D16e | **MISSING** |
| B9 | Booking-scoped chat (opens on confirm, closes 24 h after drop-off, quick replies, support-readable) | P23, D36 | **MISSING** — whole subsystem |
| B10 | Masked calling / number relay | D35, S28 | **MISSING** — needs a telephony provider decision |
| B11 | Trip freeze on first booking (editable until then) | D09, D14, D15 | **MISSING** |
| B12 | Alternatives list on decline / cancel / auto-cancel | P13, P22, P24, P34, P35 | **MISSING** |

### 4.3 Trip lifecycle, timers and reliability

| # | Capability | Screens | State |
| --- | --- | --- | --- |
| T1 | Start buffer 10 min + one 10-min extension + auto-cancel | D32, D32b, D32c, P24, P35 | **MISSING** |
| T2 | Missed-start counter, 3/month → driver deactivation + reinstatement request | D32b, D34 | **MISSING** |
| T3 | Pickup wait 5 + 5 min, clock from GPS arrival, auto-release seat | D19, D19b, P38, P38b | **PARTIAL** — `arrived-pickup` exists, no timers |
| T4 | Driver-late 10-min grace → free passenger cancel + 20% driver penalty | P34, D41 | **MISSING** |
| T5 | Driver reliability metrics (late cancels, missed starts, on-time-start %, acceptance %) | D28, D31 | **MISSING** |
| T6 | Passenger reliability (completion %, no-shows, on-time-at-kerb %, prepay at 2 no-shows/month) | P39, P38 | **MISSING** |
| T7 | Driver cancel windows + reason codes + consequences | D30, D30b, D31, D31b | **PARTIAL** — route cancel exists, no window/penalty/reason |
| T8 | Monthly counter reset (calendar month) | D28, D32b, P16 | **MISSING** |

### 4.4 Eligibility, preferences, verification

| # | Capability | Screens | State |
| --- | --- | --- | --- |
| E1 | Driving preferences (gender, verified-only, approve-each, mid-trip bookings, early-drop, chat, hide number) | D35, D13 | **MISSING** |
| E2 | Women-only trips, offered only to NIC-verified female drivers, enforced server-side in search + booking | P07, D13, D35 | **MISSING** — no `gender` on profile |
| E3 | Verified-riders-only trips hidden from unverified riders in search | P07, D35, P28 | **MISSING** |
| E4 | Passenger identity verification: 4 in-app-camera captures, levels NONE/PENDING/VERIFIED/REJECTED, camera-only enforcement | P28–P31c | **PARTIAL** — generic passenger documents exist |
| E5 | Profile-photo visibility PUBLIC / MATCHED / HIDDEN (driver's always visible after confirm) | P30, P30b, P30c, D35 | **MISSING** |
| E6 | Verification as a **ranking** signal in the driver's request list | P28, P31b | **MISSING** |

### 4.5 Search & discovery

| # | Capability | Screens | State |
| --- | --- | --- | --- |
| S1 | Radius filters on **trip start point**, options 5/10/20 km, hard ceiling 20 km | P03, P04 | **MISMATCH** — `matching_settings` defaults 1000 m / max 5000 m, and filters on pickup proximity |
| S2 | Report how many trips the radius removed | P04 | **MISSING** |
| S3 | Match tiers (Full ≥95 / Most ≥75 / Part ≥45) returned by the server | P05 | **MISSING** |
| S4 | Result payload: rate/km, gross, discount, price, `startsKmAway`, seats, verifiedOnly, womenOnly, overlap sentence, vehicle class | P04, P06 | **PARTIAL** — has fare, matched distance, driver name, vehicle basics |
| S5 | Server-side sort modes (best match / cheapest / leaves soonest) | P04 | **PARTIAL** |
| S6 | "Riding now" joinable-trips tab | P36 | **MISSING** (see B4) |
| S7 | Commuter dashboard: usual commute + live match count | P02 | **MISSING** |
| S8 | Trip QR code + short link | D14 | **PARTIAL** — `route_share_link` exists; no QR payload / short code |

### 4.6 Ratings & reviews

| # | Capability | Screens | State |
| --- | --- | --- | --- |
| R1 | Mutual rating, **both publish together** (or on window close) | P18, D24 | **PARTIAL** |
| R2 | One reply per review | D28, P39 | **MISSING** |
| R3 | Rating tags with counts | P18, D24, D28 | **MISSING** |
| R4 | Star-distribution histogram endpoint | D28, P39 | **MISSING** |
| R5 | Passenger rating aggregate | P39, D16 | **MISSING** — only `driver_profile.rating_average` exists |

### 4.7 Notifications, support, safety, shell

| # | Capability | Screens | State |
| --- | --- | --- | --- |
| N1 | Broadcast as an inbox kind + filters (All/Trips/Money/Account) + mark-all-read | S22, S22b | **PARTIAL** |
| N2 | Per-category × per-channel (push/SMS/in-app) preference matrix | S23 | **PARTIAL** |
| N3 | Tab badge counts endpoint (trips / inbox / home dot / account dot) | S14 | **MISSING** |
| N4 | Suppress passenger alerts while driving; queue as one card | S11 | **MISSING** |
| N5 | Support ticket attachments | S26 | **MISSING** |
| N6 | SOS auto-context (trip + vehicle + live location) + trusted-contact alerting | S27, S28 | **PARTIAL** |
| N7 | Suspension reason + case ref + appeal flow | S13 | **PARTIAL** |
| N8 | Driver deactivation + reinstatement request | D34 | **MISSING** |
| N9 | Settings: theme, language (en/si/ta), share-live-location, show-rating-publicly, download-my-data, delete-account, receipts-by-email | S18 | **MISSING** |
| N10 | Mode-switch gates (no driver access / pending / rejected / publish gate / suspended / conflicts) as a single server-computed context | S07–S13 | **PARTIAL** — `availableAppModes` exists |

### 4.8 The one hard blocker for a single app

`common/security/PhoneOtpAccessTokenAuthenticationFilter` grants a **hardcoded `ROLE_PASSENGER`** to every
phone-OTP session:

```java
new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")), jwt.getSubject());
```

Every driver endpoint is `@PreAuthorize("hasRole('DRIVER')")`. In a single app where the same signed-in
user switches modes, **a phone-OTP user can never reach any driver endpoint.** This must be fixed before
any unified-app work is meaningful.

## 5. Summary count

| Bucket | MISSING | MISMATCH | PARTIAL |
| --- | --- | --- | --- |
| Money & pricing | 11 | 1 | 5 |
| Booking & seats | 10 | 0 | 1 |
| Trip lifecycle | 6 | 0 | 2 |
| Eligibility & verification | 5 | 0 | 1 |
| Search & discovery | 4 | 1 | 3 |
| Ratings | 4 | 0 | 1 |
| Notifications / shell | 5 | 0 | 5 |
| **Total** | **45** | **2** | **18** |

Roughly **65 product capabilities** need backend work, of which four are new subsystems with no
foundation at all: **penalties + dues**, **referral + rewards**, **live (en-route) booking**, and
**booking chat**.
