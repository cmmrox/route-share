# ComiGo Unified App — Backend Production Plan

Date: 2026-07-31 (updated 2026-08-01 — live-booking architecture researched and right-sized to target scale)
Status: `APPROVED — scope decisions locked`
Owner: backend
Companion: [`00-prototype-gap-analysis.md`](00-prototype-gap-analysis.md)
Decoded specification: [`docs/source-assets/comigo-prototype/`](../../../../source-assets/comigo-prototype/) (28 JSX modules)

## 0. Locked decisions

| # | Decision | Consequence |
| --- | --- | --- |
| D1 | The prototype `POLICY` block **is** the commercial spec of record | Build to those exact figures, and expose every one of them as **runtime-configurable policy** so business can tune without a deploy |
| D2 | Rate bands are **admin-typed min/max per vehicle**; the four factors are stored as displayed justification, not a scoring engine | Slice 02 drops from L to M; a scoring engine can replace it later behind the same contract |
| D3 | **All four** new subsystems are in this release: penalties & dues, referral & rewards, live/en-route booking, booking chat | Slices 06, 10, 11 and 13 are all committed — nothing deferred. Live booking additionally required a new foundation slice 12 (see §2.4) |
| D4 | `apps/passenger-mobile` is **harvested** into a new `apps/mobile` (design system, API client, auth + profile features, Maestro harness carry over); `apps/driver-mobile` stub is deleted | Verified work and the green QA suites are kept; screens are rebuilt to the ComiGo prototype |
| D5 | Calls are **direct dial from the device**. No ComiGo relay, no telephony provider, no number masking. | The prototype's "Hide my number" toggles (D35, S28) are **cut**. Backend instead owns a counterparty-phone disclosure rule — see §6.1 |
| D7 | **Target scale: 500 trips/day, 200 concurrent, 300 ceiling.** ~19 req/sec against ≤300 live rows. | There is no scale problem. H3, Kafka and sharding are **declined** with recorded revisit thresholds; the whole complexity budget goes to accuracy instead |
| D6 | The app is **not released**. No real users, no real bookings, no real payments anywhere. | Migrations may change column meaning **in place**. No expand/migrate/contract pairs, no backfill scripts, no dual-read period. Breaking changes are free until launch |

## 1. What changed and why this plan exists

The team has decided to ship **one mobile application** containing both the passenger and driver
experience, replacing the planned two-app split. The `ComiGo Prototype (Standalone).html` supplied on
2026-07-31 is the new specification: ~157 screen states across passenger, driver and shared surfaces,
with an explicit machine-readable policy block (`POLICY` in `data.jsx`) that fixes every commercial rule.

The existing backend is substantial and mostly survives — identity, routing/PostGIS matching, trips,
payments gateway, notifications, admin, storage and the maps cost controls are all production-grade.
What does **not** survive is the **money model**, and four whole subsystems are simply absent.

**Headline: ~65 backend capabilities need work. 45 do not exist at all. 2 are built to a different
rule than the prototype states. 18 have foundations but no product behaviour.**

## 2. The three decisions that shape everything else

### 2.1 The fare engine is a rewrite, not an extension

Today: `total = 250 base + 90/km + 5/min`, then a 10% platform fee is **added on top**.

Prototype: `gross = onRouteKm × vehicle.ratePerKm` → `price = gross − routeMatchDiscount`, and the 10%
commission is taken **out of** that price (driver nets 90%). There is no base fare and no time component.
The per-km rate is **per vehicle**, chosen by the driver inside an admin-assessed min–max band.

Nothing about the current `FareCalculator`, `finance.fare_policy` (base/per_km/per_min/min_fare) or the
`FareBreakdown` record maps onto this. Every screen that shows money — search results, ride detail,
checkout, receipt, driver ledger, earnings, payout — reads the new shape. This is the critical path.

### 2.2 A single account must carry both roles on one token

`PhoneOtpAccessTokenAuthenticationFilter` hardcodes `ROLE_PASSENGER`, and all 10 driver endpoints are
`hasRole('DRIVER')`. In a one-app world the same signed-in user switches modes, so the token must carry
whatever roles the account actually holds. Mode becomes a **client** concept; the server authorises on
role + resource ownership + gate state. This is a blocker, not a nice-to-have.

### 2.3 Four new subsystems, not features

- **Penalties & dues** — every penalty splits 50/50 between the victim and the platform. This touches
  cancellation, no-show, driver lateness and driver cancellation, and it produces both negative and
  **positive** ledger lines. There is no penalty concept in the codebase today.
- **Referral & rewards** — a referral graph with earning windows (12 months / 50 trips), paid out of
  commission, feeding a shared balance that a rider spends as credit and a driver withdraws to a bank.
- **Live (en-route) booking** — a second, structurally different request type: joinable moving trips,
  a hard server-side "driver is still behind the rider's pickup" filter, a ~45-second prompt, and
  capture-on-accept instead of capture-on-start.
- **Booking chat** — booking-scoped threads that open on confirmation and close 24 h after drop-off.

### 2.4 Live booking needs a real location pipeline underneath it

Confirmed 2026-08-01: live en-route booking ships in this release. Researching how Uber and comparable
platforms actually achieve it showed the feature cannot sit on raw GPS — urban GPS error routinely exceeds
**50 m**, more than a Colombo block, which is enough to place a driver on the wrong side of a junction and
offer a seat he has already passed.

Uber solves the equivalent problem with HMM map matching over the whole road network plus 3D shadow
matching, because an Uber driver has no published route. **A ComiGo driver does** — it is already a PostGIS
`LineString`. That turns "which road is he on" into "how far along this one line is he": a single
`ST_LineLocatePoint`, no external API, and more accurate for this question than general map matching.

### 2.5 Two problems, and only one of them is GPS

The requirement is that a rider and a driver actually **meet**. That decomposes into two problems needing
different answers, and conflating them is the classic mistake:

- **Matching** — *is he still behind her pickup, on his route?* Tolerates ~50 m error, because a safety margin absorbs it. Solved by geometry in slice **12**.
- **Rendezvous** — *can they physically find each other at the kerb?* Here 50 m is fatal — wrong side of Galle Road, past a junction, outside a different shop. **No amount of filtering fixes this, because the error is in the map pin, not the filter.** Solved by **named pickup points** (slice **09**), two-way position in the final 500 m (slice **12**), and the plate and colour already in the prototype.

The prototype already knew this. Its chat fixture reads *"I'll be at the Rajagiriya junction bus halt, not
the roundabout. Silver Alto."* — a landmark and a description, not a coordinate. Slice 09 makes that a
system feature instead of something a driver has to think to type.

### 2.6 Right-sized to 300 concurrent trips, not to Uber's

At the target ceiling the joinable query filters **at most 300 rows** at **~19 requests/second**. That is
not a scale problem, so H3, Kafka and sharding are declined — each would add a dependency and a body of
tests in exchange for no measurable gain. A GiST index with `ST_DWithin` is *faster* here than an H3 cell
lookup, and needs no Postgres extension.

Every declined technique carries a recorded revisit threshold, so the decision is deliberate rather than
forgotten. The budget freed goes into accuracy: **approach mode** raises sampling to 1–2 s in the final
500 m — precision exactly where the pickup succeeds or fails, which Uber cannot afford at millions of
trips but ComiGo can at 300.

So slice **12** builds the pipeline — four-stage GPS filtering, dead reckoning, off-route detection,
approach mode, hybrid WebSocket/FCM delivery — and slice **13** builds the booking on top of it.
Full design, scale arithmetic and every declined alternative:
[`docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md`](../../../../architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md).

## 3. Target backend architecture

The modular-monolith rules in `docs/architecture/BACKEND-MODULAR-MONOLITH-SERVICE-IMPL-FACADE.md` stay
exactly as they are — controller → service interface → impl, cross-module only through facades, no
cross-module repository/entity imports, enforced by `PersistenceArchitectureTest`. Every new module
below follows that shape.

### 3.1 New modules

| Module | Owns | Facade exposes |
| --- | --- | --- |
| `com.routeshare.penalty` | penalty assessments, 50/50 split, passenger dues ledger, dispute records | `assess(kind, bookingId)`, `outstandingDuesFor(appUserId)`, `settleDues(bookingId)` |
| `com.routeshare.rewards` | referral graph, referral earnings accrual, shared rewards ledger, spend + withdraw | `creditFor(appUserId)`, `applyRideCredit(bookingId, amount)`, `accrueReferral(tripId)` |
| `com.routeshare.chat` | booking-scoped threads, messages, open/close window, quick replies | `openForBooking(bookingId)`, `closeAfterDropOff(bookingId)` |
| `com.routeshare.reliability` | driver + passenger counters, calendar-month windows, gates (deactivation, prepay) | `driverSnapshot(id)`, `passengerSnapshot(id)`, `recordMissedStart(...)`, `isDriverDeactivated(id)` |
| `com.routeshare.scheduling` | the job runner: expiry, buffers, waits, monthly reset, weekly payout, chat close | — (internal) |

### 3.2 Extended modules

| Module | Additions |
| --- | --- |
| `vehicle` | `vehicle_class`, rate band (`rate_min`, `rate_max`, `rate_chosen`, `band_status`, `set_by`, `set_at`), band factors, rate-review requests |
| `pricing` | **rewritten** engine: rate-band fare, match-discount tiers, commission-inside; quote/breakdown v2 |
| `routing` | radius on trip-start with 5/10/20 km options + 20 km ceiling, match tiers, eligibility filters (women-only / verified-only), joinable-live query, filtered-out count, named pickup points |
| `booking` | request type (`SCHEDULED` \| `LIVE`), approval mode, expiry states, seat assignment (front/back), dues application, rewards credit application, freeze-on-first-booking |
| `trip` | start buffer + extension + auto-cancel, pickup wait timers + extension + auto-release, driver-late grace, mid-trip seat resale |
| `payment` | capture-on-start, capture-on-accept, penalty postings, ledger kinds (`ADJUSTMENT`, `PENALTY`, `COMPENSATION`, `PAYOUT`), dues settlement, cash-commission netting |
| `finance` | weekly Friday payout batch with LKR 1000 floor + held balance, penalty split accounting |
| `driver` | driving preferences, deactivation + reinstatement requests |
| `passenger` | identity verification levels + camera-only capture, photo visibility, gender |
| `rating` | tags, one reply per review, mutual publish gating, passenger aggregate, histogram |
| `notification` | category × channel matrix, broadcast kind, badge summary, driving-mode suppression |
| `support` | attachments |
| `location` | **rewritten**: route-constrained matching, GPS filter chain, dead reckoning, off-route detection, `trip_progress` projection (GiST), approach sessions, WebSocket/SSE channel |
| `safety` | SOS auto-context + trusted-contact alerting |
| `platform` | app config/feature flags, user settings (theme, language, privacy, receipts) |
| `admin` | rate-band assessment, fare-adjustment decisions, penalty disputes, reinstatement review, referral ops |

### 3.3 The scheduler is new infrastructure

Thirteen time-driven behaviours are specified across the plan. Today the only scheduled work is the outbox
relay.

| Job | Cadence | Screens |
| --- | --- | --- |
| Scheduled request expiry (30 min) | 1 min tick | D16, P11 |
| Live request expiry (~45 s) | 5 s tick | P37c, D16d |
| Start-buffer auto-cancel (10 + 10 min) | 1 min tick | D32b, P24 |
| Pickup-wait auto-release (5 + 5 min) | 1 min tick | D21, P27 |
| Driver-late grace evaluation (10 min) | 1 min tick | P34, D41 |
| Chat auto-close (24 h after drop-off) | hourly | P23, D36 |
| Monthly reliability + early-drop reset | daily 00:05 | D28, P16, D32b |
| Weekly payout batch (Friday, floor 1000) | weekly | D33, D27 |
| Referral window expiry (12 mo / 50 trips) | daily | P32 |
| Live pickup-passed sweep | 5 s tick | D16c |
| Rating release on window close | hourly | P18, D24 |
| Location staleness sweep + approach close | 10 s tick | P36, D16c, D19 |
| Location sample retention | daily | — |

**These must be leader-elected**, not naive `@Scheduled`, or a two-instance deploy double-charges people.
Recommendation: ShedLock on Postgres (already the system of record) — smallest addition that is correct.
The infrastructure lands in slice 05; later slices register against it.

## 4. Data model plan (Flyway V027 → V041)

Existing series ends at `V026`. New series is additive except one deliberate pricing cutover.

| Migration | Slice | Content |
| --- | --- | --- |
| `V027` | 01 | `identity.last_active_mode`, suspension reason/case-ref, `driver.driver_deactivation`, `driver.driver_reinstatement_request` |
| `V028` | 02 | `vehicle.vehicle_class` (seeded), `class_key` on vehicle, `vehicle_rate_band`, band factors, rate-review requests |
| `V029` | 03 | `platform.policy_setting` + history, `pricing.fare_quote` v2 with money invariants as CHECKs, retire base/per-km/per-min from `finance.fare_policy` |
| `V030` | 04 | payment `PENDING`/`AUTHORIZED` states, `payment.payment_attempt`, booking payment method/status, cash-commission ledger type |
| `V031` | 05 | `scheduling.shedlock` + `job_run`, `trip.trip_start_window`, `trip.pickup_wait`, `trip.driver_late_grace`, `reliability.*` |
| `V032` | 06 | `penalty.penalty_assessment` (split CHECK), `penalty_beneficiary`, `passenger_due`, `penalty_dispute`, new ledger types |
| `V033` | 07 | `route_occurrence_seat`, `booking_seat` (race guard), approval mode, request expiry, occurrence cancellation, contact-disclosure audit |
| `V034` | 08 | `driver.driving_preference`, occurrence eligibility columns, passenger verification level/gender/photo visibility, verification sessions + steps |
| `V035` | 09 | matching-settings radius semantics (5/10/20 km, trip-start basis), `route_plan.origin_point` + GIST, `usual_commute`, occurrence share codes, `routing.pickup_point` |
| `V036` | 10 | `chat.*`, notification category×channel matrix, SOS context columns, support attachments, `platform.user_setting`, `account_request` |
| `V037` | 11 | `rewards.referral_code`, `referral_edge`, `rewards_ledger`, `withdrawal`, applied-credit columns |
| `V038` | 12 | `location.location_sample` refinement columns, `location.trip_progress` (GiST), `location.approach_session`, `location.realtime_channel`, sample partitioning + retention. **No new extension** |
| `V039` | 13 | `booking.live_request`, `route_occurrence_seat_release`, per-trip live-request mute, booking request type |
| `V040` | 14 | `payment.driver_ledger_entry`, payout batch cadence/floor/held items, `payment.fare_adjustment` |
| `V041` | 15 | rating tags/window/release, `review_reply`, `rating_aggregate`, tag vocabulary + counts |

Per **D6** the app is unreleased, so `V029` and `V035` change column meaning **in place** — no
expand/migrate/contract pairs, no backfills, no dual-read window. Dev and QA databases are recreated
from the full migration series; any seeded demo rows are regenerated by
`scripts/simulation/seed-demo-route.sh`.

This freedom expires at launch. Every migration from the first real user onward is forward-only and
additive, and `DECISION_LOG.md` should record that cut-over date when it arrives.

## 5. API contract plan

Order of work is contract-first, as requested: **contract → reconcile → implement**.

1. **Collapse to two client contracts.** `docs/api/passenger-app.openapi.json` +
   `driver-app.openapi.json` → **`mobile-app.openapi.json`**; `admin-web.openapi.json` stays.
   Path namespaces `/api/v1/passenger/**` and `/api/v1/driver/**` are **kept** — they are role-scoped
   resource namespaces, not app boundaries, and collapsing them would be churn for no gain.
2. **Add a shell contract.** One `GET /api/v1/me/context` returning everything the app shell needs on
   cold start and after a mode switch: modes, driver status + gate reasons, verification level,
   suspension state, active trip pointer, outstanding dues, rewards balance, tab badge counts. Today the
   shell would need 8+ calls; S07–S14 all read from this.
3. **Contract completeness pass.** Walk all ~157 screens and assert each field on screen has a source
   field in the contract. The gap analysis is the checklist; the exit criterion is *no screen renders a
   value the contract cannot supply*.
4. **Regenerate** `packages/api-contracts` and keep `docs/api/API_BACKEND_RECONCILIATION.md` current.

Contract conventions that stay: `Authorization: Bearer`, `Idempotency-Key` on retry-safe mutations,
`ApiResponse<T>` envelope, RFC7807-style typed errors (extended with `SEATS_TAKEN`, `REQUEST_EXPIRED`,
`PICKUP_PASSED`, `RATE_BAND_NOT_SET`, `DRIVER_DEACTIVATED`, `DUES_OUTSTANDING`).

## 6. Authentication & authorization plan

| Change | Why |
| --- | --- |
| Phone-OTP token carries the account's **real** roles from `identity.app_user` | Blocker — otherwise no OTP user can ever drive |
| Grant `DRIVER` realm role on driver-application **approval**, revoke on deactivation | S08/S09/D34 gating |
| Replace bare `hasRole('DRIVER')` with a composite gate: role **and** `driver_profile.status = APPROVED` **and** not deactivated | S08, S09, D34 |
| Publishing gate is separate from driving gate: blocked by rejected/missing KYC docs **or** missing vehicle rate band | S12, D40 |
| Suspension blocks both modes; driver deactivation blocks only driver endpoints | S13 vs D34 |
| Ownership checks on every booking/trip/vehicle/document path (already the pattern — extend to new modules) | — |
| Gate reasons returned as data on `/me/context`, never as opaque 403s | S07–S12 render the reason |

### 6.1 Counterparty phone disclosure (replaces number masking)

Per decision D5 calls are direct dial, so the app must be given a real phone number to dial. Today
**no booking or trip response exposes a phone number at all** — this is new backend surface, not
removed work, and it is the single place where a privacy mistake becomes permanent.

Rules the backend enforces:

| Rule | Detail |
| --- | --- |
| Disclosure trigger | Only once a booking reaches `CONFIRMED`. Never on search results, ride detail, or a pending request — a driver browsing requests must not collect numbers |
| Reciprocity | Both sides get the other's number, or neither does. No one-way disclosure |
| Scope | Passenger sees only the driver of their own confirmed booking; driver sees only passengers holding a confirmed seat on the trip they are running |
| Revocation | Withdrawn 24 h after drop-off, on the same clock that closes the booking chat |
| Cancellation | Withdrawn immediately on cancel, decline, no-show release or auto-cancel |
| Audit | Every disclosure read is logged (who, whose number, which booking) — this is the trail a harassment report is investigated from |
| Emergency | `119` and the ComiGo safety line are static, always available, and never subject to these rules |

Product consequence to accept knowingly: passengers and drivers will hold each other's personal mobile
numbers after a shared trip, and ComiGo cannot recall them. The prototype's relay existed to prevent
exactly that. If harassment reports appear later, reinstating masking means adding a provider **and**
re-cutting every Call button, so the toggles are cut from the UI but the disclosure rule above is
deliberately built as a single service method that a relay could replace without touching callers.

## 7. Delivery plan

Sixteen slices. Each is production-complete in the house sense: API + validation + authz + persistence +
errors + tests + docs + config gates move together. Sizes are relative (S/M/L/XL), not calendar estimates.

| # | Slice | Size | Depends on | Exit criteria |
| --- | --- | --- | --- | --- |
| [**00**](00-repo-reset-and-contract-rewrite.md) | **Repo reset + contract rewrite** — harvest `apps/passenger-mobile` into `apps/mobile`, delete the `apps/driver-mobile` stub, merge OpenAPI to `mobile-app.openapi.json`, add `/me/context`, regenerate `packages/api-contracts` | M | — | Contract covers all 157 screens; `API_BACKEND_RECONCILIATION.md` regenerated; design system + API client + Maestro harness carried over intact |
| [**01**](01-auth-unification-and-mode-gates.md) | **Auth unification** — real roles on OTP tokens, composite driver gate, role grant/revoke on approval/deactivation, `/me/context` | M | 00 | One account calls both passenger and driver endpoints; gate reasons render S07–S13 |
| [**02**](02-vehicle-classes-and-rate-bands.md) | **Vehicle classes + rate bands** — class, admin-typed band, factor notes, driver rate pick, review requests | M | 01 | D06/D07/D39/D39b/D39c/D40 fully served; publishing blocked without a band |
| [**03**](03-fare-engine-rewrite.md) | **Fare engine rewrite** — rate-band pricing, match-discount tiers, commission-inside, quote/breakdown v2, search + checkout + receipt repriced | XL | 02 | P07/P08/P09/D13/P17/D26 all agree to the rupee; old `FareCalculator` deleted |
| [**04**](04-charge-timing-and-capture-correctness.md) | **Charge-timing correctness** — authorize on booking, **capture on trip start**, void on cancel/auto-cancel, cash path | L | 03 | P11/P12/P22/P24/D15/D17 hold; no capture before start; idempotent under retry |
| [**05**](05-trip-timers-and-reliability.md) | **Trip timers + reliability** — start buffer + extension + auto-cancel, pickup wait + extension + auto-release, driver-late grace, counters, monthly reset, deactivation + reinstatement | XL | 04 + scheduler | D19/D19b/D21/D32/D32b/D32c/D34/D41/P24/P27/P34/P35/P38/P38b/P39/D28 |
| [**06**](06-penalties-dues-and-compensation.md) | **Penalties & dues** — assessment, 50/50 split, comp lines, passenger dues carried to next booking, disputes | L | 04, 05 | P25/P26/P27/P22/D21/D31/D41 reconcile; ledger balances |
| [**07**](07-booking-depth-seats-approval-and-expiry.md) | **Booking depth** — seat identity, approval mode, 30-min expiry, freeze-on-first-booking, typed conflicts, alternatives, driver cancel windows + reasons | L | 03, 05 | P08/P13/P14/D09/D16/D30/D30b/D31/D31b |
| [**08**](08-preferences-verification-and-eligibility.md) | **Preferences, verification & eligibility** — driving preferences, women-only, verified-only, passenger identity levels + camera-only, photo visibility, search enforcement | L | 01, 03 | P07/P28–P31c/D35/D13; ineligible trips never leave the server |
| [**09**](09-search-and-discovery-v2.md) | **Search & discovery v2** — trip-start radius 5/10/20 km, filtered-out count, match tiers, enriched result payload, server-side sorts, commuter dashboard, QR/short links, **named pickup points** | M | 03, 08 | P02/P03/P04/P05/P06/D14; a pickup resolves to a landmark, not a coordinate |
| [**10**](10-chat-notifications-safety-and-support.md) | **Chat + notifications + safety + support** — booking chat with close window, category×channel prefs, broadcast kind, badge counts, driving-mode suppression, ticket attachments, SOS context + trusted-contact alerts, settings | L | 01, 07 | P23/D36/S11/S18/S22/S23/S24–S28 |
| [**11**](11-referral-and-rewards.md) | **Referral & rewards** — referral graph, 1%/2% accrual from commission, window expiry, shared balance, ride credit at checkout, bank withdraw in the Friday batch | L | 03, 06 | S05/P32/P33/D37/D38/D38b/P09e |
| [**12**](12-realtime-location-pipeline.md) | **Real-time location pipeline** — route-constrained matching, four-stage GPS filtering, dead reckoning, off-route detection, approach mode, hybrid WS/FCM delivery | L | 04, 05, 09 | `location.trip_progress` answers "where is he on his route" with a confidence level; approach mode makes the rendezvous work; zero added Google cost; proven at 300 concurrent |
| [**13**](13-live-en-route-booking.md) | **Live (en-route) booking** — joinable search, behind-pickup filter, 45-second prompt, capture-on-accept, mid-trip seat resale, lapse handling | XL | 04, 07, 09, 12 | P36/P36b/P37/P37b/P37c/D16b/D16c/D16d/D16e/D18b/D22b |
| [**14**](14-money-operations-payouts-and-adjustments.md) | **Money operations** — weekly Friday payout with floor + held balance, ledger kinds, fare-adjustment decisions + 48 h dispute, cash-commission netting, admin finance surfaces | M | 06, 11 | D25/D26/D27/D29–D29c/D33/D33b |
| [**15**](15-ratings-and-reviews-v2.md) | **Ratings v2** — mutual publish gating, tags, one reply, histograms, passenger aggregate | M | 05 | P18/P39/D24/D28 |

Slices 00–04 are the critical path; nothing else is trustworthy until the money moves at the right
moment for the right amount. 08–15 can parallelise across two workstreams once 04 lands, except that
13 cannot start before 12.

### 7.1 Google API cost is a design constraint

Governing rule (Decision 018): **never call Google for something the database already knows, and never
call it per-ping.**

Two paths in an earlier draft would have quietly become the largest line items and were rewritten:
**ETA** and **live-request detour minutes** are now derived from stored route geometry ÷ the trip's own
observed speed — free, and *more accurate* than a Google estimate, because they reflect the traffic that
driver is actually sitting in. **Named pickup points** use a cost-ordered chain with Places genuinely last.

| At 500 rides/day | Calls/day | Cost/day |
| --- | --- | --- |
| Place Details (Essentials, ~1.5 sessions/ride) | 750 | $3.75 |
| Autocomplete (in session) | ~4,000 | $0.00 |
| Distance Matrix (cache misses) | ~200 | $0.41 |
| Directions (new route plans only) | ~30 | $0.15 |
| Pickup points (after warm-up) | ~20 | $0.10 |
| **Location pipeline** — GPS, ETA, detour, matching | **0** | **$0.00** |
| **Total** | | **≈ $4.41/day ≈ $132/month** |

Inside the $200 monthly credit, so payable is **$0**. Per ride: **$0.0088** — about LKR 3. Roughly a **10×
reduction** against a naive implementation, before counting Roads API snapping, which would be ~225,000
calls/day on its own.

Full model, sensitivity analysis and estimate confidence:
[`REALTIME-LOCATION-AND-LIVE-MATCHING.md` §5.5–5.7](../../../../architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md).
Twelve cost gates are hard items on the release checklist.

## 8. Cross-cutting requirements

- **Idempotency** on every new mutation that money or seats depend on, reusing `common.idempotency_key`.
- **Outbox events** for every new state change (`penalty.assessed`, `reward.accrued`, `booking.expired`,
  `trip.autocancelled`, `driver.deactivated`, `chat.opened`) — the relay already exists.
- **Testing gate stays as-is**: `./mvnw spotless:check verify`, JaCoCo 80%. New money paths need
  property/edge tests (rounding, split-remainder, capture idempotency, allowance boundaries).
  Testcontainers integration tests for the scheduler jobs — timers are where correctness dies.
- **Observability**: counters for expiries, auto-cancels, auto-releases, capture failures, penalty
  assessments, payout batch outcomes. A silent scheduler failure must page, not accumulate.
- **Provider gates** unchanged and fail-safe: Cybersource, FCM, S3, Sentry, Notify.lk, Google.
  New: telephony provider for masked calling (§9), which nothing else may depend on.
- **Living docs**: `DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `BLOCKERS.md`, `DECISION_LOG.md`
  updated per slice, as the operating skill requires.

## 9. Remaining risks

Scope questions 1–3 and 6 from the original draft are resolved in §0. What is still live:

| # | Risk | Impact if wrong | Mitigation |
| --- | --- | --- | --- |
| 1 | **Direct dial exposes personal mobile numbers permanently** (D5). Two strangers keep each other's numbers after one shared trip, and there is no recall. | Harassment / unwanted contact reports, with no technical remedy short of adding a relay later | Disclosure rules in §6.1 — confirmed bookings only, reciprocal, revoked 24 h after drop-off, every read audited. Kept behind one service method so a relay can be swapped in without re-cutting the UI |
| 2 | `finance.fare_policy` (base/per_km/per_min/min_fare) is admin-editable today and becomes meaningless under the new model. | Admin UI shows dead controls | Retire those fields in slice 03 and replace with the runtime policy surface D1 requires |
| 3 | Scheduler without leader election in a multi-instance deploy. | Double captures, double penalties, double payouts | ShedLock on Postgres, landed with slice 05 before any timer job goes live |
| 4 | D1 requires every policy figure to be runtime-configurable, which is a wider surface than hardcoding. | Slice 03 and 05 grow | One `platform.policy_setting` table + typed accessor, seeded from the prototype values; admin CRUD lands with slice 14 |
| 5 | Breaking-change freedom (D6) ends silently at launch. | A post-launch migration written in the pre-launch style destroys real records | Record the launch date in `DECISION_LOG.md`; from then on migrations are forward-only and additive |

## 10. Working assumptions (proceeding on these unless corrected)

1. Path namespaces `/passenger/**` and `/driver/**` are retained; only the client contract files merge.
2. `apps/admin-web` remains a planned application and keeps its own OpenAPI contract.
3. Currency stays LKR only; no multi-currency.
4. Chat is built in-house on Postgres + the existing push channel (no third-party chat SDK).
5. Rounding rule: victim share rounded, platform takes the remainder, so split halves always re-add to
   the whole fee — as `data.jsx` does it.
6. Policy values are seeded from the prototype and read through a single typed accessor, never inlined
   as constants, so D1's runtime-configurability holds everywhere.

## 11. Immediate next step

All six decisions are locked and no pre-flight checks remain. Slice **00** (repo reset + contract
rewrite) is cleared to start. Each of the 15 slices gets its own `NN-task-name.md` file in this folder
plus a matching `qa/test-cases/comigo-unified-app-backend/` entry, per
`IMPLEMENTATION_PLANNING_STANDARD.md`.

The decoded prototype — 28 readable JSX modules, which are the actual specification rather than the
1.8 MB bundle — is committed at `docs/source-assets/comigo-prototype/`.
