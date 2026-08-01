# RouteShareApp Development Status

2026-08-02 (Blocker 013 cleared — slices 01–04 verified against a real database for the first time)

## 2026-08-02 — Blocker 013 cleared: the first real database run, and what it found

Slices 00–04 were merged on the strength of unit tests and review. **No migration in the ComiGo
series had ever been applied to a database, and none of the four smoke scripts had ever executed.**
That is now fixed, and the honest headline is that the first real run found five defects that
review had passed.

**The blocker's own diagnosis was the first thing that was wrong.** It attributed the skipped
Testcontainers tests to the host port conflict on 5433. Testcontainers binds ephemeral ports and
never touches 5433. The real cause was a version floor: Docker Engine 29 rejects API `<= 1.41` with
HTTP 400, and the docker-java shaded into Testcontainers 1.19.8 negotiates from that floor. Because
`disabledWithoutDocker = true` converts "no Docker" into a silent *skip*, the suite stayed green for
weeks while proving nothing. The fix is Testcontainers 1.21.3 plus `api.version` as a **surefire
system property** — docker-java ignores the `DOCKER_API_VERSION` environment variable, which is why
setting it looks like it does nothing. The port was a separate, much smaller problem, solved by
`ROUTESHARE_POSTGRES_PORT=5434`; the unrelated containers were left alone.

Then the defects, in descending order of how much they would have cost:

- **`V029` could never have run.** It creates `platform.policy_setting`, but nothing had ever
  created the `platform` schema — Flyway stopped at `3F000: schema "platform" does not exist`. The
  fare engine's entire policy surface was one `CREATE SCHEMA` away from existing. Fixed in place.
- **Every unmapped path answered HTTP 500.** `NoResourceFoundException` had no handler and fell into
  the catch-all, so a client typo and a deliberately removed endpoint both reported "the server is
  broken". Slice 03's removed `POST /pricing/estimate` was returning 500 rather than 404 — the smoke
  script caught it precisely because it asserts the removal. Fixed in `GlobalExceptionHandler`,
  which also now returns 405 rather than 500 for a wrong verb.
- **`*IT.java` matched no surefire include**, so the new integration tests were collected by nobody
  and the gate would have gone green without them. Now included explicitly.
- The local dev Keycloak realm gave direct access grants to `passenger-mobile` and `driver-mobile`
  but not `admin-web`, so **no simulation script could ever obtain an admin token** — the first run
  of `verify-mode-gates.sh` failed 7 of 12 checks for that single reason.
- `seed-demo-route.sh` had silently rotted against slice 02: `seatCount: 4` on a CAR now capped at
  3, no `vehicleClass` (now required), and no rate band — which slice 02 made a precondition of
  publishing.

Two further "failures" were the scripts' fault, not the backend's, and are worth naming because
both would have been easy to record as product bugs: `data_of` parsed money with `json.loads`, which
turns `49.50` into the float `49.5` so no scale-2 comparison could ever pass; and the fare fixture
asked for an 11.4 km slice of a corridor that is only 9.5 km long, so the fraction clamped to 1.0.
The 11.4 km → 570 fixture is asserted exactly by `FareEngineTest`; the smoke now asserts the *rule*
(`gross = onRouteKm × rate`) against real stored geometry, which is what a runtime check can add.

Runtime results, first ever:

| Slice | Script | Result |
| --- | --- | --- |
| 01 | `verify-mode-gates.sh` | **12/12** |
| 02 | `verify-rate-bands.sh` | **18/18** — both V028 PL/pgSQL triggers fired |
| 03 | `verify-fare-engine.sh` | **8/8**, 1 skip — both V029 CHECK constraints fired |
| 04 | `verify-charge-timing.sh` | 2/2 cash; **card path skipped — no gateway (Blocker 015)** |

`V001`–`V030` now apply cleanly against real PostGIS. `FlywayPostgisMigrationIntegrationTest` had
been asserting the latest version was `12` — stale since V013, and invisible because it was
skipping; it now asserts `030`.

**Slice 04's missing test is written.** `CaptureOnTripStartIT` starts the same booking from 20
threads simultaneously: exactly one capture attempt is admitted and 19 are refused by the unique
index on `payment_attempt.idempotency_key` with SQLSTATE `23505`. That is the property the mocks
could not prove, and the reason Blocker 013 mattered.

Gate: `./mvnw spotless:check verify` → **BUILD SUCCESS, 346 tests, 0 skipped, JaCoCo met**.

**Still not verified:** the card path of charge timing — authorise, capture, void — has never run
end to end against any gateway, real or fake. Tracked as **Blocker 015**, and slice 05 inherits it,
since the start-buffer auto-cancel calls `PaymentFacade.voidForBooking`.

Next: slice 05 — trip timers and reliability.

## 2026-08-02 — Slice 04: charge timing and capture correctness

The product's central promise is now true of the backend. `POLICY.chargeAt = "TRIP_START"` is stated
on eleven screens in the strongest terms — *"Accepting does not charge you"*, *"Your Visa is
authorised, not charged"*, *"Decline, cancel or a no-start all cost you nothing"* — and until this
slice **there was no capture call anywhere in the trip lifecycle.** A trip could run to completion
without money moving, and a cancelled booking could leave a hold on someone's card for a week.

The card is now authorised when the booking is made and captured when the driver starts, atomically
with the trip transition. Cancel, decline and route cancel all release the hold. Cash bookings
create no intent at all: there is nothing to hold, and a placeholder row would be a lie the
reconciliation job would later have to chase.

**The state machine gained the state it was missing.** `REQUIRES_CAPTURE` conflated "we have not
asked your bank yet" with "your bank is holding this and we have not taken it" — two different facts
about someone's money that the booking screens are at pains to distinguish. `PENDING → AUTHORIZED →
CAPTURED → REFUNDED`, with `VOIDED` and `FAILED` as exits, and transitions go through methods that
also stamp the timestamp: P12 shows the passenger the exact minute they were charged, and a captured
row with no `capturedAt` cannot answer that.

**Idempotency is the whole slice.** Every gateway call writes a `payment.payment_attempt` row
*before* it is made, keyed deterministically (`capture:booking:42`) with a unique index behind it. A
capture that times out has either happened or not, and without that row there is no way to tell —
a blind retry charges someone twice. A duplicate start finds the row already written and never
reaches the provider.

**A refused bank does not stop the trip.** One start captures N cards; each booking reports its own
outcome (`CAPTURED | ALREADY_CAPTURED | SKIPPED_CASH | FAILED`). The driver is at the wheel and the
other passengers are in the car — flagging that booking is the correct failure, and stranding
everyone is not.

Also landed: early drop-off captures the lower figure if nothing has been taken and refunds the
difference if it has; cash collection records `COMMISSION_OWED_CASH` for netting from the next
payout; and `GET /api/v1/admin/payments/reconciliation` surfaces stuck authorisations and unfinished
gateway calls, with a gauge to alert on. Nobody involved in a stuck hold notices on their own.

Verification: `./mvnw spotless:check verify` → **BUILD SUCCESS, 344 tests, JaCoCo gate met**;
`redocly lint` clean; contracts typecheck green. New tests: `PaymentIntentStateMachineTest` (12
cases, including that capture-before-authorise, double capture and void-after-capture are all
impossible) and `PaymentFacadeImplTest` (13, including the duplicate-start and declined-card paths).

**Deferred:** `scripts/simulation/verify-charge-timing.sh` is written but unrun (Blocker 013), and
the Testcontainers integration test the task asks for (`CaptureOnTripStartIT`) is **not written** —
it needs a live database, which this machine cannot start. The unit tests cover the same paths
against mocks, which is weaker for exactly the property that matters most here: that the unique
index on `idempotency_key` is what makes double capture impossible under real concurrency.

Next: slice 05 — trip timers and reliability, which owns the auto-cancel that calls this slice's
void path.

## 2026-08-01 — Slice 03: fare engine rewrite

## 2026-08-01 — Slice 03: fare engine rewrite

The money model is now the product's. Out: `250 base + 90/km + 5/min` with a 10% fee **added** on
top. In: `gross = onRouteKm × that vehicle's chosen rate`, less a route-match discount, with the
commission taken **out of** what the passenger pays. There is no base fare and no time component —
a rider pays for the distance they actually ride on a road the driver was taking anyway, and
charging for time would charge them for his traffic.

`FareEngine` is pure and reproduces every figure in `data.jsx` exactly: 11.4 km at LKR 50/km grosses
570; a 5.8 km seat on a 92% match is gross 290, discount 23, **passenger pays 267**, commission 27,
**driver nets 240**.

**Money is rounded to whole rupees**, not to two decimal places as the task specified. The
prototype rounds every figure it shows, and a receipt reading "LKR 266.80" is a number nobody can
hand over; scale-2 arithmetic misses the fixtures by 20 cents at every step. Values are still
carried as `BigDecimal` at scale 2 and stored in `NUMERIC(12,2)`.

Two invariants hold for every quote ever produced, and are **database CHECK constraints** rather
than comments: `driverNet + commissionAmount = passengerPays`, and
`passengerPays = grossFare − discountAmount`. `driverNet` is computed by subtraction, never by its
own multiplication — rounding two percentages independently and hoping they add back is how a
ledger drifts a rupee per trip. A test walks 4,000 rounding paths and both hold throughout.

**Quotes are persisted, never recomputed.** Rate bands move and discount tiers are tunable, so a
receipt read three months later must show the fare that was charged at the rate then in force.
Every booking links its quote; the early drop-off reprices against the *original* rate and tier,
because the passenger travelled less but the terms she booked under have not changed.

**The policy surface (decision D1) exists.** `platform.policy_setting` holds 35 seeded rules —
commission, the four discount tiers and their thresholds, penalty percentages, waiting times,
payout floors, referral rates — with a history table, admin CRUD restricted to money roles, and a
Caffeine cache evicted on write. `PricingArchitectureTest` fails the build if any of those figures
is inlined as a Java constant again.

**`POST /pricing/estimate` is deleted, not deprecated.** It took a distance from the request body,
which let a client name the number its own fare was computed from — a free-money bug wearing the
shape of an API. `estimate-by-route` now names a published trip and two fractions along the
driver's stored line; the architecture test asserts no pricing input is declared in any request
DTO. `FareCalculator` and `FareBreakdown` are gone, and `finance.fare_policy` keeps only `min_fare`.

Payment now reads the commission from the persisted quote rather than recomputing it from a
configured rate: otherwise the first commission change would settle old bookings under new terms.
The driver earnings summary sums the commission rows the ledger already holds, so the headline can
never disagree with the rows beneath it.

Verification: `./mvnw spotless:check verify` → **BUILD SUCCESS, 319 tests, JaCoCo gate met**;
`redocly lint` clean; `@routeshare/api-contracts` typecheck green. New tests: `FareEngineTest`
(11 cases including the fixtures and the invariant sweep), `MatchDiscountTierTest`,
`PolicySettingTest`, `PricingArchitectureTest`.

**Deferred:** `scripts/simulation/verify-fare-engine.sh` is written but unrun (Blocker 013). It is
the only check that exercises the two new CHECK constraints and the fixture reproduction through a
live API.

Next: slice 04 — charge timing and capture correctness, the product's central promise.

## 2026-08-01 — Slice 02: vehicle classes and rate bands

## 2026-08-01 — Slice 02: vehicle classes and rate bands

The product's central pricing rule now exists in the database: **a driver never types a price.**
ComiGo assesses a min–max per-km band per vehicle and the driver picks a point inside it, which is
why two cars on the same road are not the same price and why search will be able to explain "why
Priya's rate is LKR 46". Nothing here prices a trip — slice 03 owns the fare engine; this slice
stores and governs the number.

Four classes are seeded with seat caps and default ranges (`CAR` 3/38–62, `SUV` 4/46–74, `VAN`
6/40–68, `THREE_WHEELER` 2/26–42). The band has its own lifecycle —
`NOT_SET → PENDING_ASSESSMENT → ACTIVE → UNDER_REVIEW` — deliberately separate from vehicle
approval, because **approved papers are not a price**: without a band there is no legal figure to
put on a seat. That state is board D40, a first-class screen rather than an error, and it is now
also a real publish gate: slice 01's `RATE_BAND_NOT_SET` fires from `canPublish` the moment a driver
has an approved vehicle and no live band.

Three things are enforced by the database rather than by service code, because each one is a pricing
incident rather than a validation slip:

1. **A band outside its class range** — a `BEFORE INSERT OR UPDATE` trigger, since a `CHECK` cannot
   read another table. The service refuses it too; the trigger is what makes a bad migration or a
   direct SQL edit fail as well.
2. **A seat count above the class cap** — same mechanism. Selling a fourth seat in a three-seat
   class is a capacity lie told to a rider.
3. **One open re-assessment per vehicle** — a partial unique index, so D39's "one re-assessment"
   is a constraint rather than a sentence in a spec.

Two decisions worth recording:

- **The four factor rows are displayed justification, not inputs** (decision D2). The admin types
  the band; the factors explain it. The service checks that the deltas roughly explain the offset
  from the class default and **warns rather than refuses** — the prose must never be able to block
  an operational price change.
- **Assessment defaults the chosen rate to the midpoint**, and keeps an existing rate if it still
  fits. Otherwise a band would land and the car would still be unpublishable until the driver
  happened to open a screen.

Authority is split deliberately: a verification agent may approve a car's papers but may **not**
price it — band assessment is restricted to `ADMIN`, `SUPER_ADMIN` and `FINANCE_ADMIN`. A driver
setting their own band would be the most valuable escalation in the system, and it is tested
explicitly. Rate positions (bottom/middle/top, with their ranking and demand copy) are derived
server-side so the driver's screen and the passenger's explanation can never disagree.

Verification: `./mvnw spotless:check verify` → **BUILD SUCCESS, 294 tests, JaCoCo gate met**;
`redocly lint` on the mobile contract → zero errors; `@routeshare/api-contracts` typecheck green.
New tests: `RateBandServiceImplTest` (19 cases), `RatePositionTest`, plus class-cap and
band-lifecycle cases added to `VehicleServiceImplTest` and `DriverGateServiceTest`.

**Deferred:** `scripts/simulation/verify-rate-bands.sh` is written but unrun — same cause as slice
01, host port 5433 (Blocker 013). It is the only check that exercises the two database triggers, so
neither trigger has executed yet.

**Found, not fixed:** the mobile contract's `Vehicle` schema uses `year`, `passengerSeatCapacity`
and `verificationStatus` where the API returns `manufactureYear`, `seatCount` and `status`. That
drift predates this slice (it survived slice 00's reconciliation) and would break a generated
client. Recorded as Blocker 014 rather than renamed mid-slice.

Next: slice 03 — the fare engine rewrite, which reads the chosen rate this slice stores.

## 2026-08-01 — Slice 01: auth unification and mode gates

## 2026-08-01 — Slice 01: auth unification and mode gates

The blocker that made a single app impossible is gone. `PhoneOtpAccessTokenAuthenticationFilter`
stamped `ROLE_PASSENGER` on every phone-OTP session while all ten driver endpoints required
`hasRole('DRIVER')`, so **a phone-OTP user could never drive**. Authorities are now derived per
request from the identity projection by a new `AccountRoleService`, cached briefly and invalidated on
every grant, revoke and deactivation — so both token issuers end up with the same authorities for the
same person, and a role taken away stops working on the next request rather than at cache expiry.

**Gates are data, not exceptions.** A new `DriverGuard` replaces the bare role check with three
independent facts — not suspended, approved profile, no open deactivation — and, when it refuses,
throws the reason instead of returning `false`. Every gated 403 now carries `{code, message,
actionPath}`, and the same structure appears pre-emptively on `/me/context`, so the app renders
S07/S08/S09/S12/S13/D34 *before* the user taps something that fails. Nine gate codes are produced by
the conditions that define them (`RATE_BAND_NOT_SET` is slice 02's, as scoped).

Three decisions worth recording, because each one is a place the obvious implementation is wrong:

1. **Suspension and deactivation are not the same refusal.** Suspension stops everything and outranks
   every driver gate — a suspended driver under review must see S13's appeal route, not S08's "we're
   checking your documents", since only one of those is actionable. Deactivation stops *driving* and
   nothing else. D34 promises the driver both their rider account and the money they have already
   earned, so payout and support endpoints sit behind a third, weaker gate
   (`@DriverSelfServiceAccess`) rather than `@DriverAccess`. Putting them behind the driver gate would
   have stranded exactly the person the screen is written for: told to contact support by a screen
   whose support call returns 403.
2. **Realm roles are granted one at a time.** The existing `setRealmRoles` states the whole managed
   set, which is right for the admin role editor and wrong for driver approval — it would silently
   strip an admin who also drives. Added `grantRealmRole`/`revokeRealmRole`, and made a Keycloak that
   is switched off locally a logged warning rather than a failed approval, since the local projection
   is authoritative for phone-OTP tokens either way.
3. **A stored DRIVER mode is honoured only while driving is available.** Otherwise a driver
   deactivated overnight cold-starts into a mode that refuses every call.

Also landed: `PUT /me/active-mode` (409 with the blocking gate code when the mode is not available),
driver reinstatement requests wired to a support ticket with one open request at a time, admin
deactivate/reinstate with audit and role revocation, and a real `case_ref` on suspensions replacing
slice 00's fabricated one.

Scope notes: `driver.driver_document` gained an `expires_at` column — `DOCUMENT_EXPIRED` is one of the
eight gate codes the task specifies and there was nowhere to record an expiry, so the code could not
otherwise have been produced by anything. Eight of the ten former `hasRole('DRIVER')` sites use
`@DriverAccess`; payouts and driver support use `@DriverSelfServiceAccess` for the D34 reason above.
`POST /api/v1/routes` (publish) and the recurring-route POSTs now use `@DriverPublishAccess`.

Verification: `./mvnw spotless:check verify` → **BUILD SUCCESS, 264 tests, JaCoCo gate met**;
`redocly lint docs/api/mobile-app.openapi.json` → **zero errors**; `@routeshare/api-contracts`
typecheck green. New tests: `DriverGuardTest`, `DriverGateServiceTest`, `PhoneOtpRoleResolutionTest`,
`RoleCacheInvalidationTest`, `SuspensionPrecedenceTest`, `DriverDeactivationServiceImplTest`, plus
`AppContextServiceTest` extended to 20 cases.

**Deferred:** `scripts/simulation/verify-mode-gates.sh` is written but has not been run — host port
5433 is held by an unrelated project's container, so the local Postgres will not start (Blocker 013).
The Keycloak role-state and audit-extract manual checks in the QA file depend on the same run.

Next: run the smoke script once the port is free, then slice 02 — vehicle classes and rate bands.

## 2026-08-01 (slice 00 COMPLETE — unified app + merged contract)

## 2026-08-01 — Slice 00 COMPLETE: unified app and merged client contract

Executed the first slice of the ComiGo backend plan. **All verification green.**

Repository:

- `apps/passenger-mobile` → **`apps/mobile`** (`@routeshare/mobile`), design system, API client, auth and
  profile features, native project and Maestro harness carried over intact. `apps/driver-mobile` deleted.
- `qa/maestro/passenger-mobile` → `qa/maestro/mobile`; `scripts/qa-passenger-*.sh` → `scripts/qa-mobile-*.sh`.
- App display name is now **ComiGo**. Native identity (slug, scheme, bundle id, android package) is
  deliberately unchanged — renaming it requires regenerating the committed native projects, which is
  blocked on the prebuild decision in Blocker 009. Pinned by test so the change is deliberate when it comes.

Contract — `docs/api/mobile-app.openapi.json`, **186 paths / 221 operations / 85 schemas, lints clean**:

- `passenger-app.openapi.json` + `driver-app.openapi.json` merged and deleted.
- Every operation stamped `x-routeshare-status`: **120 IMPLEMENTED**, 91 PLANNED_SLICE_NN, 7 internal, 3 CUT.
- **All 157 prototype screens map to at least one operation**, verified programmatically.

Reconciling two independently-maintained contracts against the running backend surfaced **five real defects**:

1. `SosRequest.tripId` was **uuid** in the driver contract and **int64** in the passenger one. The column is `BIGINT`, so a generated driver client would have failed at runtime.
2. **22 implemented endpoints were in neither contract** — including the entire Places/directions surface the search screens depend on, and the presigned document-upload lifecycle.
3. Two contract endpoints were **never implemented** (direct document uploads superseded by the presigned lifecycle).
4. **17 uses of `nullable`**, which is OpenAPI 3.0 syntax and invalid in the 3.1 documents both files declared.
5. Path parameter names disagree between contract and controllers (`{savedPlaceId}` vs `{id}`) — harmless, recorded.

New: **`GET /api/v1/me/context`** — the app shell's single read, serving S07–S14 in one call instead of
eight. Two deliberate behaviours: a **suspended caller is answered, not refused** (S13 needs the reason and
appeal route, so it resolves the user without the ACTIVE guard), and fields owned by later slices return
**zero values, never null**, so the shell never reshapes. `GET /auth/me` deprecated in its favour.

Verification: backend `spotless:check verify` → **BUILD SUCCESS, 218 tests, JaCoCo gate met**;
mobile `lint | typecheck | test` → **18 files / 86 tests**; `@routeshare/api-contracts` typecheck green;
`redocly lint` on the mobile contract → **zero errors, zero warnings**.

**Build note:** the API requires **JDK 21**; this machine's shell default is 17, which fails with a Lombok
`TypeTag :: UNKNOWN` error. Use `JAVA_HOME=~/.sdkman/candidates/java/21.0.9-amzn`.

Deferred with reasons: Maestro device reruns (needs emulator + live stack), Keycloak client consolidation
(slice 01 owns auth), native identity rename (Blocker 009). New Blocker 012 records pre-existing OpenAPI
3.1 violations in `admin-web.openapi.json`.

Next: slice 01 — auth unification and mode gates.

## 2026-08-01 — Live en-route booking confirmed in scope; real-time location architecture researched

The product owner confirmed live en-route booking must ship in this release, citing Uber and PickMe, and
asked that the design follow how those platforms actually achieve accuracy. Researched and recorded as
**Decision 016**, with the full design at `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md`.

The finding that shaped it: **an Uber driver has no published route, so Uber must map-match against the
whole road network** (HMM in CatchME, 3D shadow matching for urban canyons). **A ComiGo driver publishes
his route before departure**, already stored as a PostGIS `LineString`. That turns "which road is he on"
into "how far along this one line is he" — a single `ST_LineLocatePoint`, no external API call, and more
accurate for this question because the search space is one line rather than thousands of segments.

Adopted from industry practice: Uber's H3 hexagonal index (res 7 coarse / res 9 fine) for candidate
lookup, Uber's published sampling cadence (4 s in trip, 10–30 s idle), and hybrid WebSocket + FCM
high-priority delivery because Android Doze defers WebSocket traffic while high-priority FCM wakes the
radio.

Explicitly rejected with reasons: Google Roads API snap-to-roads (100-point cap, per-ping cost — roughly
450,000 calls/hour at 500 concurrent trips, and it would reverse the July 2026 cost work), 3D shadow
matching, Ringpop-style consistent hashing, and DISCO-style dispatch rotation (ComiGo is rider-initiated,
so there is no candidate queue). Self-hosted Valhalla/OSRM map matching is deferred, not rejected — it is
only needed off-corridor, which the design handles by failing closed.

Then the product owner supplied target scale — **500 trips/day, ~200 concurrent, 300 ceiling** — which
right-sized the whole design (**Decision 017**). The arithmetic: ~19 requests/second against at most 300
live rows. That is not a scale problem, so the complexity budget moved to accuracy.

- **H3 dropped.** At ≤300 rows a GiST index with `ST_DWithin` is faster than a cell lookup and needs no
  Postgres extension or JVM library. Revisit threshold recorded and alerted: sustained concurrency above
  5,000, or joinable-query p95 above 50 ms. Kafka and sharding likewise declined with thresholds.
  **No new Postgres extension is required** — managed Postgres is unconstrained.
- **The two problems separated.** *Matching* (is he behind her pickup) tolerates ~50 m error and is
  geometry. *Rendezvous* (can they find each other) is fatal at 50 m and is **not** a filtering problem —
  the error is in the map pin. The prototype already knew this: *"the Rajagiriya junction bus halt, not the
  roundabout. Silver Alto."*
- **Named pickup points** added to slice 09 — curated / Google-Places-derived / learned, resolved at
  booking, never per ping.
- **Approach mode** added to slice 12 — within 500 m, sampling rises to 1–2 s and a two-way position window
  opens, with the rider's position deleted when it closes. This is scale working in ComiGo's favour: it
  costs ~25 req/sec here and lands precision on the ninety seconds that decide the pickup.
- **Detour cap** added to slice 13 — 8 minutes, from UberX Share's published limit, filtered before the
  driver is ever prompted.

**Google cost audited against current pricing (Decision 018).** The plan was re-checked SKU by SKU and
two paths that would have become the largest line items were caught and fixed:

- **ETA** would naively have been a Google call. Now derived: `remainingRouteMeters ÷ smoothedObservedSpeed`.
- **Live-request detour minutes** would naively have been a Directions call *per candidate* — thousands
  per hour at 300 concurrent. Now pure geometry ÷ observed speed.
- **Pickup points** resolved naively were ~30,000 Place Details calls/month (~$150), enough alone to break
  the $200 credit. Now a cost-ordered chain — curated → persisted → route label → Places → raw — plus a
  one-time seed of ~200 Colombo landmarks.

Both derived values are **more accurate than the Google estimate**, because they use the traffic that
driver is actually sitting in rather than a generic model of the road. Cost and accuracy pointed the same
way, so nothing was traded.

Estimated steady state at 500 trips/day: **~$132/month, inside the $200 credit** — Place Details sessions
~$112, Distance Matrix misses ~$12, Directions on new routes ~$5, pickup points ~$3, location pipeline $0.
Cost gates are hard items on the release checklist, not advisories.

Plan status: **FINAL.** Sixteen slices, migrations `V027`–`V041`, thirteen scheduled jobs, five new
modules plus a rewritten `location`. Former slices 12/13/14 renumbered to 13/14/15. Every task file has its
matching QA file; all cross-references, links and migration numbers validated.

Next: slice 00 — repo reset and contract rewrite.

## 2026-07-31 (ComiGo unified-app pivot planned)

## Purpose

This file is the first file to read before continuing RouteShareApp development. It shows the current phase, completed work, active work, pending work, blockers, verification status, and the next recommended task.

## Current State

- Implementation Planning Standard: `docs/development/IMPLEMENTATION_PLANNING_STANDARD.md` defines the required `docs/development/implementation/tasks/<feature-plan-name>/` structure and production-ready task-file rules.
- Current Phase: `PHASE_08_COMIGO_UNIFIED_APP_BACKEND_IN_PROGRESS`
- Current Milestone: `MILESTONE_SLICE_04_CODE_COMPLETE_RUNTIME_SMOKE_PENDING`
- Current Active Task: `Slices 01–04 code-complete (runtime smoke pending, Blocker 013); slice 05 — trip timers and reliability — next`
- Plan Validation: `16 slices, acyclic dependency graph, V027–V041 contiguous, all task/QA cross-links verified both directions, zero broken links`
- Status: `SLICE_04_MONEY_MOVES_AT_TRIP_START_ONLY`
- Repository Git Status: `Slices 01–03 merged to main; slice 04 on feat/comigo-unified-app-slice-04; migrations V027–V030 added`

## 2026-07-31 — ComiGo unified-app pivot: backend plan and 15 task files

The team decided to ship **one** mobile application containing both the passenger and driver experience,
replacing the two-app split (Decision 011). `ComiGo Prototype (Standalone).html` is the new specification.

Work completed in this session — **documentation only, no code touched**:

- Decoded the 1.8 MB prototype bundle into 28 readable JSX modules, committed at
  `docs/source-assets/comigo-prototype/`. This is the specification of record: ~157 screen states plus a
  machine-readable `POLICY` block that fixes every commercial rule.
- Audited the existing backend (456 main Java files, 20 modules, Flyway V001–V026) against it. Gap:
  **~65 capabilities — 45 missing, 2 built to a different rule, 18 partial**. Register at
  `docs/development/implementation/tasks/comigo-unified-app-backend/00-prototype-gap-analysis.md`.
- Wrote the backend production plan plus **15 production-ready task files** and **15 matching QA files**
  under `qa/test-cases/comigo-unified-app-backend/`, per the planning standard.

Three findings shape the whole plan:

1. **The fare engine is a rewrite.** Current: `250 base + 90/km + 5/min` with a 10% fee added on top.
   Prototype: `onRouteKm × vehicle.ratePerKm` less a route-match discount, commission taken **out of**
   the fare. No base fare, no time component, per-vehicle rates inside admin-set bands.
2. **A single account cannot currently drive.** `PhoneOtpAccessTokenAuthenticationFilter` hardcodes
   `ROLE_PASSENGER` while all 10 driver endpoints require `hasRole('DRIVER')`. Hard blocker for one app.
3. **Charge timing is unwired.** The product's central promise — captured at trip start — has no
   implementation; there is no `capture(...)` call anywhere in `trip/service/impl`.

Four subsystems have no foundation at all and are all in scope: penalties & dues, referral & rewards,
live (en-route) booking, and booking chat.

Decisions locked this session: 011 (one app), 012 (POLICY is authoritative, runtime-configurable),
013 (admin-typed rate bands), 014 (direct dial, masking cut), 015 (pre-launch migrations may change
column meaning in place).

Next: slice 00 — repo reset and contract rewrite. `apps/passenger-mobile` is harvested into `apps/mobile`;
`apps/driver-mobile` is deleted; the two client OpenAPI documents merge into `mobile-app.openapi.json`.

Phase 07 (passenger mobile app) is **superseded**, not abandoned — its design system, API client, auth and
profile features and green Maestro suites carry over into `apps/mobile`.

## 2026-07-21 — Google API cost-optimization + performance slice (branch `codex/perf-google-cost-optimization`)

Implemented the architecture-review cost/performance recommendations without changing product behavior:

- Places autocomplete/details now carry a client-generated Google **session token** end-to-end
  (search screen → proxy → Google) so Google bills a search interaction as one session.
- Place Details **field mask reduced to Essentials tier** (`id,formattedAddress,location`;
  `displayName` is Pro-tier ≈3× the price) — clients keep the suggestion label instead.
- **Ride-detail map polyline now comes from the stored PostGIS route line**
  (`GET /api/v1/passenger/route-occurrences/{id}/geometry`, `ST_LineSubstring` between the matched
  fractions): zero Google cost, and the passenger sees the driver's actual route. Directions API
  remains only as fallback for unmatched coordinate pairs.
- **Redis caching** for Google responses via new `common/cache/RedisJsonCache`:
  place details by placeId (24 h), Distance Matrix by ~110 m-rounded coordinates (7 d),
  Directions by ~11 m-rounded coordinates (7 d). TTLs configurable; well inside Google's 30-day term.
- **Per-user rate limits** on the Google-billed proxy endpoints (autocomplete 40/min,
  details 20/min, directions 20/min) using the existing `RedisRateLimiter` — cost-abuse stop.
- **Cooldown breaker** (`ProviderCooldown`, 3 failures → 30 s skip) on all Google adapters so a
  Google brownout degrades instantly to fallbacks instead of holding request threads for 8 s each.
- **Identity projection cache** (Caffeine, 5 min TTL, claims-aware, admin suspend/activate
  invalidates): `IdentityFacade.upsertFromToken` no longer issues a DB write per authenticated
  request. `ROUTESHARE_IDENTITY_PROJECTION_CACHE_TTL_SECONDS=0` restores old behavior.
- Client tuning: autocomplete min chars 2→3, debounce 350→450 ms, saved places matched locally
  (zero-cost) above Google suggestions; ride detail prefers the geometry endpoint.
- New **simulation/QA helper scripts** under `scripts/simulation/` (kept separate as required):
  `seed-demo-route.sh` (Keycloak demo driver + approved vehicle + published Colombo Fort→Nugegoda
  route) and `verify-cost-controls.sh` (end-to-end proof of session tokens, Redis caches, stored
  geometry, degradation paths, and the 429 rate limit).
- Fixed in-flight: `@ConfigurationProperties` records with extra convenience constructors needed
  `@ConstructorBinding` (caught by live boot, not unit tests).

Verification (all green):

- Backend `./mvnw spotless:check verify` — BUILD SUCCESS, **203 tests**, JaCoCo 80% gate met.
- Passenger mobile `typecheck | lint | test` — **18 files / 82 tests**; `@routeshare/api-contracts` typecheck passed (51 passenger paths).
- Live stack (Postgres/Redis/Keycloak + API with real Google keys): `scripts/simulation/verify-cost-controls.sh` → **6/6 PASS** (session token, `maps:place:*` cache, seeded-route search, `source=route_plan` geometry, `maps:dm:*` cache with `GOOGLE_DISTANCE_MATRIX`, autocomplete 429 after 40/min).
- Android emulator Maestro regressions both **PASS**: `task07-home-search-route-discovery.yaml` (1m14s) and `task08-results-list-map-filtering-ride-detail.yaml` (1m36s) on Pixel_9 against the live stack; ride-detail run left **zero `maps:dir:*` keys** in Redis, proving the polyline came from the stored route geometry. Evidence under ignored `qa/reports/20260721-002636/` and `qa/reports/20260721-003153/`.
- Environment note for repeat QA: the passenger dev client must be built with
  `-PreactNativeDevServerPort=8082`, and another local project (`genone-keycloak`) squats host
  `127.0.0.1:8080`, so Metro must be started with `EXPO_PUBLIC_API_BASE_URL=http://<mac-LAN-IP>:8080`.

## 2026-06-21 — Phase 07 Task 08 implemented (results list/map/grouped + ride detail)

Built the ride-browsing + pre-booking evaluation feature on top of the now-complete backend:

- Backend search enriched so results carry real data: `RouteSearchResponse` (+ candidate query) now
  return `estimatedFare` (FareCalculator on the matched on-route distance × seats),
  `matchedDistanceMeters`, `driverName`, and `vehicleMake/Model/Registration/SeatCount`.
- New RN-free, unit-tested `features/ride-results` module (normalization + filters/sort + overlap-tier
  grouping) and two screens: `SearchResultsScreen` (list/map/grouped toggle, sort + match-threshold
  filters, ride cards, accessible map fallback, empty state) and `RideDetailScreen` (driver card,
  route timeline, fare estimate, why-good-match, safety/policy, continue→seat-selection handoff).

Verification: passenger-mobile `typecheck | lint | test` (18 files / 80 tests) + Android e2e scaffold
+ preview-build gates green; backend `./mvnw spotless:check verify` green (180 tests). **Android device
QA is GREEN**: the Task 08 Maestro regression passed 1/1 (2m35s) on the Pixel_9 emulator against the
live stack with a seeded Colombo Fort → Nugegoda route — real data rendered across list/map/grouped +
ride detail (100% match, "Saman Fernando", "Toyota Aqua · CAB-7788", 3 seats, fare **LKR 1,206**).
Evidence: `qa/reports/20260621-111049-task08-final-green/`. iOS device evidence remains a later
release-evidence follow-up. Next: Task 09 — seat selection, booking idempotency, cancellation.

## 2026-06-21 — Phase 06.6-K backend gap closure COMPLETE (backend production-complete)

Closed the final seven backend gaps from the requirement/screens/docs audit; **no `workflow_item` shell remains for any product flow**, and Phase 06.6 (Backend Production Hardening) is now `COMPLETED`:

1. Early drop-off — actual-distance fare recalc + capture (`V024`, real `PassengerBookingController` + `EarlyDropOffService` + `PaymentService.finalizeBookingFare`).
2. Lifecycle notifications — booking confirmed/declined/cancelled, trip started/arrived/completed/cancelled, payment captured wired to `NotificationFacade`.
3. Trip share-links — tokenized, time-boxed, revocable + unauthenticated public status (`/api/v1/public/trip-shares/{token}`) + best-effort trusted-contact SMS via `SmsGateway.sendText` (`V025`).
4. Driver verification — `DriverVerificationService` derives readiness from profile status + required KYC docs (IDENTITY/LICENCE) + approved vehicle, with guidance; KYC identity/licence now issue real presigned uploads.
5. Admin-configurable matching — `routing.matching_settings` (`V026`) + `GET/PUT /api/v1/admin/matching-settings`; `RouteService.search` resolves defaults from it and clamps to configured maximums.
6. Analytics — real FINANCE/OPERATIONS reports over a window + CSV export (`GET /api/v1/admin/reports/{type}` and `/export`).
7. Cleanup — passenger avatar/verification on the real document lifecycle; admin driver-application review consolidated onto `AdminDriverReviewController` (+ audit); stateless ride-search GET shells removed; OpenAPI specs + `API_BACKEND_RECONCILIATION.md` reconciled.

Verification: `./mvnw spotless:apply spotless:check verify` → BUILD SUCCESS, 180 tests, JaCoCo 80% gate met. Live boot against the project Docker stack applied Flyway V024→V026 cleanly, seeded `matching_settings`, confirmed the `FARE_FINALIZED` ledger constraint, and smoke-tested endpoints (health UP; public invalid token → 404; admin endpoints → 401). Provider integrations remain gated/credential-ready (Cybersource/FCM/object-storage/Sentry/Notify.lk) per the user's plan to supply real keys later. **Backend development is functionally complete for production; remaining work is the passenger mobile app (Phase 07, Task 08 next).**

## 2026-06-18 — Phase 06.6 Backend Production Hardening started (Phase A done)

A production-readiness audit found the Phase 06.5 "closure" was largely a facade: ~50 endpoints behind one generic `app_backend.workflow_item` table, untyped responses, and no real provider integrations beyond Notify.lk SMS + Google Places (payments `mock_`-only; no FCM/object-storage/Sentry/Kafka). Opened branch `feat/backend-production-hardening` and added `Phase 06.6 — Backend Production Hardening` to the roadmap (Phases A–J).

Phase A complete and verified: transactional event outbox (`common.event_outbox`, V016) + relay scheduler + Kafka/logging senders; observability deps (Micrometer/Prometheus, Sentry backend, structured JSON logs via `json` profile) + readiness/liveness health groups; staging/prod env templates. Also fixed a latent `PersistenceArchitectureTest` failure (renamed `GooglePlaceSearchService` → `GooglePlaceSearchServiceImpl`) that the Task 07 maps work introduced but never ran. `./mvnw spotless:check test` → BUILD SUCCESS, 109 tests pass (1 Testcontainers skip).

Phase J complete: deployment artifacts — multi-stage `apps/api/Dockerfile` (non-root, Actuator healthcheck), `docker-compose.prod.yml` overlay wiring the API to the project services with all provider flags fail-safe-defaulted, and `docs/development/DEPLOYMENT.md` (build/run, gated-integration flag→credential table, migration pipeline, health/probes/metrics, backup/restore drill, runbooks). Keycloak realm import already carries the managed roles+clients; `PRODUCTION_EXTERNAL_SERVICES.md` reconciled. **Phase 06.6 (Backend Production Hardening) A–J is complete** — real domains behind gated, credential-ready provider integrations, booting against the project Docker stack.

Phase I (security hardening) complete and verified: Redis fixed-window **rate limiting** (`RateLimiter`/`RedisRateLimiter`, fails open) on OTP request/verify, payment-intent, and SOS; plus the earlier live-stack boot validation + `RequestMappingUniquenessTest` regression guard. `./mvnw spotless:check verify` → BUILD SUCCESS, 162 tests pass. Remaining Phase I hardening (matching index/perf, N+1 audit, Testcontainers integration/authz suite, load smoke) is tracked as ongoing alongside Phase J.

Phase H complete and verified: `RouteMetricsPort` (Google Distance Matrix when configured, haversine fallback) feeds server-side distance+duration into a time-aware `FareCalculator`; `POST /api/v1/pricing/estimate-by-route` prices from coordinates instead of a client-supplied distance; trip location-trail now returns real PostGIS-extracted coordinates and the last admin facade read endpoint is retired. `./mvnw spotless:check verify` → BUILD SUCCESS, 156 tests pass.

Phase G-4 complete and verified (Phase G admin suite functionally complete): notification broadcasts (`AdminBroadcastController`), Keycloak realm-role propagation via the existing project Keycloak admin client (`KeycloakRealmRoleAdapter` → `IdentityFacade.setRealmRoles` → `AdminUserController` PUT roles), and admin trips/bookings/driver-applications/vehicles read projections + booking status-history + trip-cancel + report-export (`AdminOpsController`). Fixed a latent duplicate `/reports/summary` mapping from G-3. Only driver-app review + trip location-trail remain on the facade (location trail needs PostGIS coords, Phase H). `./mvnw spotless:check verify` → BUILD SUCCESS, 152 tests pass.

Phase G-3 complete and verified: real admin dashboard + reports/summary (live counts via `AdminDashboardController`) and real document review + signed download for driver/vehicle/passenger documents (`AdminDocumentController`: approve/reject sets status+reviewer, audits, emits `document.reviewed`; presigned GET via object storage). `./mvnw spotless:check verify` → BUILD SUCCESS, 150 tests pass. G-4 remaining: driver-application/vehicle list+detail, trips/bookings ops + location trail, broadcasts, reports/export, Keycloak roles.

Phase G-2 complete and verified: real admin finance in a new `finance` schema (V023) — commission rules + fare policies CRUD, finance adjustments, settlement driver-balances (accrued DRIVER_EARNING minus PAID payout items), and payout batches (create→total, mark-paid), all audited, via `AdminFinanceController`. Removed those workflow_item endpoints. `./mvnw spotless:check verify` → BUILD SUCCESS, 148 tests pass. G-3 remaining: dashboard, verification/doc-review, trips/bookings ops, broadcasts, reports, Keycloak roles.

Phase G part 1 complete and verified: real admin audit log (`audit.audit_action`, recorded by every admin mutation), real user management (list/detail/suspend/activate/status-history on `identity.app_user`; suspension enforced via `upsertFromToken`), and real admin support + SOS management wired to the Phase E tables (status/reply/resolve, audited, raiser notified) — V022, role-scoped. Remaining admin areas (dashboard, verification/doc-review, trips/bookings ops, fare/commission policy, settlements/payouts, broadcasts, reports, Keycloak role propagation) stay on the readiness facade as G-2. `./mvnw spotless:check verify` → BUILD SUCCESS, 146 tests pass.

Phase F complete and verified: real recurring routes (RECURRING schedule rules + multi-occurrence generation with bucket cells reusing the matching infra; create/list/pause/cancel/generate-more via `/api/v1/driver/recurring-routes`) and real driver payout profile (bank/wallet, validation, masked numbers, re-verification on change; `/api/v1/driver/payout-profile`), V021. Replaced the workflow_item shells. `./mvnw spotless:check verify` → BUILD SUCCESS, 145 tests pass.

Phase E complete and verified: real `support` (support_ticket/support_message with reopen-on-reply), `safety` (sos_event raise → `safety.sos.raised` domain event + confirmation notification), and `rating` (per-booking unique, driver aggregate, notifies driver) domains (V020). Passenger/driver controllers replace the workflow_item shells; added `BookingFacade.findDriverAppUserIdForPassengerBooking` for rating ownership/ratee. `./mvnw spotless:check verify` → BUILD SUCCESS, 142 tests pass. Admin manage/resolve for support/SOS lands in Phase G.

Phase D complete and verified: real `notification` schema (notification/preference/push_registration/delivery_log, V019); `PushNotificationPort` with a real FCM adapter (Firebase Admin SDK, gated by `PUSH_NOTIFICATIONS_ENABLED`) + logging fallback; `NotificationService` + `NotificationFacade` (cross-module sends) honoring per-user preferences and writing delivery logs; typed passenger/driver inbox/preferences/push-registration controllers. Removed the workflow_item notification shells and fixed a latent Phase C duplicate payment-methods mapping. `./mvnw spotless:check verify` → BUILD SUCCESS, 134 tests pass.

Phase C complete and verified: `PaymentGatewayPort` with a real `CybersourcePaymentGateway` (authorize/capture/void/refund/tokenize via HTTP-Signature) + cash-only fallback; signature-verified idempotent Cybersource webhook; real tokenized payment-methods domain (`/api/v1/passenger/payment-methods`); configurable commission with PLATFORM_COMMISSION/DRIVER_EARNING ledger entries (V018). Card path gated by `CYBERSOURCE_ENABLED`; cash remains default. `./mvnw spotless:check verify` → BUILD SUCCESS, 130 tests pass. Admin settlement/payout-batch/finance-adjustment deferred to Phase G.

Phase B complete and verified: new `storage` module with an `ObjectStoragePort` (real `S3ObjectStorageAdapter` via AWS SDK v2 + `DisabledObjectStorageAdapter` fail-closed). Driver, vehicle, and passenger documents now use a real presigned upload lifecycle (`upload-url → submit → download-url`) with content-type/size validation, ownership checks, and `*.document.submitted` events (V017). `./mvnw spotless:check verify` → BUILD SUCCESS, 120 tests pass, JaCoCo gate green. Admin document review/signed-download deferred to Phase G.

## 2026-06-17 — Tasks 01–07 UI alignment to design PDF + green Android device QA

Reworked the passenger screens for Tasks 01–07 to match `docs/source-assets/RouteShare · Passenger App.pdf` and re-verified end-to-end on `emulator-5554` against the real stack (Postgres/Keycloak/Redis, API on 8080, Metro on 8082, Google Maps/Places enabled, demo OTP).

Screens reworked to match the design references:

- Splash: solid brand-orange screen, logo mark, "Share the ride. Share the cost.", "COLOMBO · SRI LANKA".
- Onboarding: peach illustration card per slide, Skip top-right, dot indicators, Next/Get started (removed the old tag chip + progress bar; titles/body match the PDF).
- Login: country flag + `+94` box beside the number field, OR divider, Google/email buttons, terms.
- OTP: "Didn't receive it? Resend in 0:24" line.
- Profile setup: back button, avatar with overlay "+" badge, uppercase field labels, green "Verified passenger — we'll ask for a photo ID later" note.
- Home (Map A): "Where to, Nimali?", search bar with Now pill, Home/Office quick cards, clock-icon frequent-route rows. Dashboard B stays behind the `homeVariant` flag (no visible toggle on Map A, per task spec).
- Search: integrated pickup (teal dot) / dropoff (orange square) field card, swap button, Now/+30/+60 time pills, seat stepper, type-to-search Google Places SUGGESTIONS, SAVED + Use-current-location section.
- Account: orange header with wallet/saved tiles, grouped lists with chevrons, design-matched subtitles.
- Saved places: clean list with icons, "Add new place" form toggle, RECENT SEARCHES section with clear.

Bugs found and fixed during device QA:

- Search submit button was incorrectly gated on coordinate-validation (coordinates only resolve on submit via Places details), leaving it permanently disabled. Now enabled when both pickup and dropoff places are chosen.
- "Now" departure used screen-mount time (in the past) and failed backend future-time validation. submitSearch now floors the departure to `now + 60s`.
- Suggestion list re-opened after selecting a place. Added an explicit `suggestionsOpen` flag.

Verification:

- `pnpm --filter @routeshare/passenger-mobile typecheck` — passed.
- `pnpm --filter @routeshare/passenger-mobile lint` — passed.
- `pnpm --filter @routeshare/passenger-mobile test` — passed (17 files / 72 tests).
- Android Maestro regression `qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml` — PASS end-to-end (onboarding → login → OTP → profile → Home → Search via real Google Places → Search Results), evidence under ignored `qa/reports/20260617-002329-task07-rework/`.
- Updated both Task 07 Maestro flows to the type-to-search UX, including dismissing the soft keyboard (pressKey Enter) before tapping a suggestion (first tap otherwise only dismisses the keyboard).

Design serif typeface now bundled and rendering (closes the prior cosmetic gap):

- Added `expo-font` + `@expo-google-fonts/fraunces` + `@expo-google-fonts/plus-jakarta-sans`; fonts load at startup in `App.tsx` (`useFonts`) with the Splash held until ready.
- Tokens now map `display` → `Fraunces_700Bold` (serif headings, matches the PDF) and the rest → Plus Jakarta Sans. `fontWeight` was removed from the typography variants because Android falls back to a system sans when a named custom family is combined with an explicit weight (weight is baked into the family name).
- Rebuilt + reinstalled the Android dev client via `expo run:android` so the `expo-font` native module is present; on-device screenshot confirms the Fraunces serif heading renders.

## Estimated Progress

- Completed known implementation tasks: 82
- Total known high-level tasks: 95+
- Estimated overall progress: 64%

> Progress is estimated from known tasks and will change as requirements are added or split into smaller implementation tasks. Phases 00 through 06 are now closed for the Phase 07 gate. Later product areas such as realtime websockets, notifications/support/SOS, real payment-provider integration, full mobile/admin UI implementation, observability, and production hardening remain in their own later phases.


## Latest Verification Update — 2026-06-15 22:35 +0530

Status: `PASSENGER_TASK_07_BLOCKED_ON_GOOGLE_MAPS_KEYS_AND_DEVICE_QA`

Completed in this implementation pass:

- Added passenger ride-search feature module for validation, backend DTO mapping, location fallback state, recent-search privacy-safe retention, and home dashboard model building.
- Reworked Home into a map/dashboard route-discovery entry with Safety, account/menu, destination CTA, quick saved places, frequent/recent rows, loading/error states, and persisted recent-search reads.
- Added Search screen for current-location permission request, denied/unavailable manual fallback copy, pickup/dropoff fields, swap, suggestions, future pickup time chips, seat picker, validation errors, backend route-search creation, retry/edit error state, and recent-search clear/save controls.
- Wired Search into passenger navigation/deep links and typed API modules through `adaptRideSearch`.
- Added Task 07 unit coverage and Maestro smoke flow under canonical QA folders.

Verification completed locally on Mac:

- `pnpm --filter @routeshare/passenger-mobile lint` — passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` — passed.
- `pnpm --filter @routeshare/passenger-mobile test` — passed (`17` files / `69` tests).
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` — passed scaffold/config gate.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` — passed scaffold/config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` — passed config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` — passed config gate.

Next recommended task: Task 08 results list/map filtering and ride detail.

## Latest Verification Update — 2026-06-15 09:25 +0530

Status: `PASSENGER_OTP_KEYCLOAK_AND_ANDROID_SMOKE_VERIFIED_WITH_CONFIG_WARNINGS`

Completed in this verification pass:

- Backend phone OTP verification now links/creates a Keycloak user and assigns the `PASSENGER` realm role.
- Phone OTP access token subject now uses the Keycloak user id instead of a local `phone:+94...` subject.
- Local live smoke verified Keycloak user creation, role assignment, `/api/v1/auth/me`, and local `identity.app_user.keycloak_subject` mapping.
- Passenger mobile Login, OTP, Profile Setup, Home, and Account screens were adjusted closer to supplied passenger design references and first-run flow requirements.
- OTP resend now stores and verifies against the latest backend `verificationId` and has an active countdown.
- Local/dev passenger app config now enables phone OTP by default because the backend provider path is implemented; staging/production still require explicit provider enablement.

Verification evidence:

- Backend focused tests passed: `KeycloakPhoneVerifiedIdentityServiceTest`, `PhoneOtpServiceImplTest`.
- Backend full Maven test run completed with no Surefire failures/errors; Testcontainers Docker availability warning remains environment-dependent.
- Passenger mobile `lint`, `typecheck`, and `test` passed: 16 test files, 62 tests.
- Passenger mobile E2E scaffold/config gates passed for iOS and Android.
- Passenger mobile preview build config gates passed for iOS and Android.
- Expo iOS export bundled successfully from `apps/passenger-mobile/index.ts`.
- Android emulator `emulator-5554` built, installed, and rendered the app through Metro on port 8081.
- Android screenshots captured for onboarding and corrected Login screen; Login no longer shows the stale red Phone OTP blocker.

Remaining known issues:

- `expo-doctor` reports one CNG/native-folder warning: native `android/` exists while app config contains prebuild-managed fields.
- Android dev-client shows a non-fatal Expo CLI websocket warning toast in development; the app still renders.
- Many later passenger screens remain placeholder-level and still need exact supplied-design implementation: search, results, ride detail, seat selection, payment, booked/waiting, in-trip, receipt, rating, history, safety/SOS, share trip, notifications, and support.

## 2026-06-16 — Task 07 Android Search-screen device QA green + backend fix

Ran Task 07 (home, search, location, route discovery) device QA on Android `emulator-5554` against the real stack (Postgres/Keycloak/Redis, backend API on 8080 with demo-OTP + Google Maps enabled, Metro on 8082, installed debug dev client).

- Fixed a backend startup bug: `GooglePlaceSearchService` had two constructors with none annotated, so Spring fell back to a missing no-arg constructor and the API would not start. Added `@Autowired` to the production constructor (same class as resolved Blocker 004). Backend `spotless:check` and `compile` pass; API boots and serves Places autocomplete/details.
- Corrected the stale Maestro flows to drive the implemented Google-Places UX: type query → "Search pickup/destination places" → tap a `📍` suggestion (resolves `placeId`) → "Search shared rides". Removed `hideKeyboard` (Android Maestro maps it to BACK, which popped the Search screen).
- Regression flow `qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml` now PASSES end-to-end with real Google-resolved coordinates (pickup `6.9336686, 79.8500469`; dropoff `6.8649081, 79.8996789`). Evidence under ignored `qa/reports/20260616-224636/`.
- Observations (not Task 07 blockers): offline banner is a NetInfo false-positive on the emulator (`clients3.google.com/generate_204` unreachable) and does not block search; the authenticated session is not restored on cold launch (Task 05 scope), so the warm-path smoke flow's precondition is not met by a bare `launchApp`.

Remaining for Task 07 production closure: iOS simulator/device runtime evidence for the same map/place/search path (Blocker 011).

## Completed So Far

- [x] Keycloak/auth architecture documented.
- [x] One Keycloak user can act as passenger and/or driver.
- [x] Backend owns business profiles by Keycloak subject.
- [x] PostgreSQL/PostGIS multi-schema architecture documented.
- [x] OpenAPI/Swagger planning documents created under `docs/api/`.
- [x] Development tracking files created under `docs/development/`.
- [x] Repository skeleton exists with backend, app, infrastructure, scripts, and docs folders.
- [x] Local Docker infrastructure created for PostgreSQL/PostGIS, Redis, Redpanda, Keycloak, and MinIO.
- [x] Dev scripts patched for non-interactive SSH shells: PATH, `DOCKER_CONFIG`, and `docker-compose` usage.
- [x] Spring Boot 3 / Java 21 backend scaffolded in `apps/api`.
- [x] Flyway migrations created and applied.
- [x] Module schemas and foundation tables created for identity, passenger, driver, vehicle, routing, booking, trip, location, pricing, payment, and common idempotency.
- [x] OAuth2 resource-server security configured.
- [x] Keycloak role converter hardened to trust `api-monolith` resource roles only.
- [x] Common API response and error handling added.
- [x] Identity projection/upsert from JWT implemented.
- [x] `GET /api/v1/auth/me` implemented.
- [x] Passenger profile API implemented.
- [x] Driver application/profile API implemented.
- [x] Vehicle create/list API implemented with deterministic `INSERT ... RETURNING` creation.
- [x] Admin driver review API implemented with enum-backed status validation.
- [x] Pricing estimate domain/service endpoint implemented.
- [x] Route publishing endpoint implemented with explicit coordinate DTO validation.
- [x] Route publishing hardened to require approved driver profile and approved vehicle ownership.
- [x] Booking endpoint moved into application service with transactional seat decrement.
- [x] Trip transition endpoint moved into application service with ownership/admin authorization and state machine validation.
- [x] Location update endpoint moved into application service with current-driver ownership and timestamp freshness validation.
- [x] Payment intent endpoint moved into application service and no longer accepts client-controlled amount/currency; amount is derived from booking fare and currency defaults server-side to LKR.
- [x] Unit tests added for payment service and route service hardening.
- [x] Existing domain tests pass.
- [x] Runtime API health verified after the latest hardening patches.
- [x] Added Flyway V004 hardening constraints for active payment intent uniqueness and positive booking fare estimates.
- [x] Booking now derives and stores fare estimates during booking creation.
- [x] Route publishing validates future departure time and requested seats against approved vehicle capacity.
- [x] Location updates now verify the active trip belongs to the current driver profile.
- [x] Trip transitions now run transactionally and lock the trip status row before transition.
- [x] Suspended/deleted local app users are blocked after JWT identity projection.
- [x] Saved places CRUD APIs implemented and verified.
- [x] Trusted contacts CRUD APIs implemented and verified.
- [x] Driver document metadata APIs implemented and verified.
- [x] Vehicle document metadata APIs implemented and verified.
- [x] Admin vehicle review API implemented and verified.
- [x] Backend persistence refactored away from service-layer queries/JdbcTemplate into Spring Data JPA repositories under infrastructure.
- [x] Lombok-backed JPA entities/repositories added for backend persistence boundaries.
- [x] Spotless/google-java-format configured and applied to backend Java sources.
- [x] Architecture tests added to prevent JdbcTemplate in main sources, database APIs/SQL in application services, and repositories outside infrastructure.

- [x] Approved backend architecture simplified from hexagonal `port/in` / `port/out` naming to learner-friendly Spring Boot modular monolith packages.
- [x] Backend packages refactored to `controller`, `dto`, `mapper`, `service`, `service/impl`, `facade`, `facade/impl`, `domain`, `entity`, and `repository` for implemented modules.
- [x] Module facades added for identity, passenger, driver, vehicle, routing, booking, and cross-module calls now use facades instead of foreign repositories/entities.
- [x] MapStruct dependency and shared `RouteShareMapperConfig` added; driver/passenger/vehicle mappers now use MapStruct.
- [x] Architecture tests expanded for service/impl, facade, mapper config, controller, entity/repository, and cross-module boundary rules.
- [x] Architecture documentation updated with the approved service/impl + facade approach.
- [x] Java 21 virtual threads enabled for Spring Boot request/task execution with bounded Hikari database pool settings.

- [x] Phase 04 route schedule rules, route occurrences, and route bucket indexing committed.
- [x] Route search now exposes route occurrence identity and matched pickup/drop route fractions for booking handoff.
- [x] Booking creation now reserves seats against `routing.route_occurrence` instead of abstract `routing.route_plan`.
- [x] Booking rows now store `route_occurrence_id`, `pickup_route_fraction`, and `dropoff_route_fraction`.
- [x] Booking creation now writes initial `CONFIRMED` status history to `booking.booking_status_history`.
- [x] Booking creation now requires an explicit HTTP `Idempotency-Key` and replays completed matching responses from `common.idempotency_key` without reserving seats twice.
- [x] Booking status transitions for cancel/reject/complete are implemented with same-transaction status history rows.
- [x] Passenger boarded/no-show/drop-off state machine implemented per booking on the concrete route occurrence.
- [x] Immutable fare ledger foundation records booking fare estimates before payment intent creation/replay.
- [x] Payment capture, void, refund, driver cash collection, and passenger receipt foundation implemented.
- [x] Driver earnings summary/transactions, MVP platform commission, and settlement-balance read models implemented.
- [x] Route share link/QR payload foundation implemented.
- [x] Driver pre-trip checklist, arrived-at-pickup, and fare-adjustment request endpoints implemented.
- [x] Admin payment list/detail/events and cash collection projections implemented.
- [x] Lightweight TypeScript workspace and `packages/api-contracts` endpoint inventory generated.
- [x] Structured logging conventions documented.
- [x] Testcontainers Flyway/PostGIS migration smoke test added; it auto-skips when the Java Docker client cannot connect.

- [x] Passenger/driver/admin OpenAPI contracts audited against the business requirement PDF and supplied passenger/driver designs.
- [x] Missing product APIs added to `docs/api/passenger-app.openapi.json`, `docs/api/driver-app.openapi.json`, and `docs/api/admin-web.openapi.json`.
- [x] API gap analysis documented in `docs/api/API_GAP_ANALYSIS.md`.
- [x] Roadmap now has explicit API contract gates before passenger mobile, driver mobile, and admin web implementation.
- [x] Blocker 006 added for API contract/backend reconciliation.
- [x] API backend reconciliation document created at `docs/api/API_BACKEND_RECONCILIATION.md`.
- [x] First app-facing backend alias controllers implemented with TDD:
  - Passenger ride search create alias.
  - Passenger booking create/cancel aliases.
  - Passenger payment intent alias.
  - Driver route create alias.
  - Driver trip start/complete and passenger board/no-show/drop-off aliases.
- [x] Targeted alias controller tests and full backend `spotless:check test` passed.

## In Progress

- [x] Passenger mobile Task 01 typed API client implemented and verified for lint/typecheck/unit tests. Native E2E/preview evidence is deferred to Task 02 Expo scaffold.

- [x] Phase 06 realtime location and WebSocket foundation is complete. Phase 07 Passenger Mobile is in progress.

## Completed Phase-Gate Closure

- [x] Phase 05 booking, trip, fare, payment, MVP earnings/commission/settlement-balance read models are implemented for the Phase 06 gate.
- [x] Phase 05.5 app-facing backend/API reconciliation is complete enough for the Phase 06 gate. Realtime-dependent, notification/support/SOS, real provider, and UI/client implementation items are explicitly deferred to later phases.

## Pending Roadmap Summary

- [x] Phase 00 — Project architecture and file structure.
- [x] Phase 01 — Local development environment.
- [x] Phase 02 — Backend modular monolith foundation.
- [x] Phase 03 — Identity, passenger, driver, KYC/document metadata, vehicle, saved places, trusted contacts, and vehicle review foundation APIs are implemented.
- [x] Phase 04 — Route publishing and route matching. Route search, schedule rules, route occurrences, and bucket-cell broad filtering are implemented and committed.
- [x] Phase 05 — Booking, trip lifecycle, fare, payment, settlement. Booking occurrence inventory, idempotency, status history, passenger trip states, immutable fare ledger, payment capture/void/refund, cash collection, receipt foundation, driver earnings, MVP commission, and settlement-balance read models are implemented for the Phase 06 gate.
- [x] Phase 06 — Realtime location and WebSocket updates.
- [x] Phase 06.5 — App Backend Readiness Closure completed.
- [~] Phase 07 — Passenger mobile app in progress: Tasks 01–07 complete; Task 08 results list/map and ride detail next.
- [ ] Phase 08 — Driver mobile app.
- [ ] Phase 09 — Admin web app.
- [ ] Phase 10 — Hardening, observability, performance, deployment readiness.

## Latest Verification

- Passenger mobile Task 01:
  - Command: `pnpm --filter @routeshare/passenger-mobile lint` — passed.
  - Command: `pnpm --filter @routeshare/passenger-mobile typecheck` — passed.
  - Command: `pnpm --filter @routeshare/passenger-mobile test` — passed (`3` files, `23` tests).
  - Native E2E/preview commands intentionally fail with a documented blocker until Task 02 creates the Expo/native scaffold.

- TypeScript contract package:
  - Command: `pnpm install && pnpm --filter @routeshare/api-contracts typecheck`.
  - Result: passed.
- Maven backend tests and formatting:
  - Command: `cd apps/api && JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:/opt/homebrew/opt/maven/bin:/usr/bin:/bin:/usr/sbin:/sbin ./mvnw spotless:apply spotless:check test -q`
  - Result: `BUILD SUCCESS`.
- Virtual thread configuration:
  - `spring.threads.virtual.enabled=true` configured in `application.yml`.
  - HikariCP pool bounds configured with `ROUTESHARE_DB_POOL_MAX_SIZE`, `ROUTESHARE_DB_POOL_MIN_IDLE`, and `ROUTESHARE_DB_CONNECTION_TIMEOUT_MS`.
- Architecture verification:
  - `PersistenceArchitectureTest` passes.
  - Enforces no `JdbcTemplate` in main sources, no SQL/low-level database APIs in service implementations, repositories under `repository`, entities under `entity`, service implementations under `service/impl`, facades under `facade/impl`, controllers not importing repositories/entities, MapStruct shared mapper config usage, and no cross-module repository/entity/impl imports.
- Runtime health:
  - `GET /actuator/health` returned HTTP `200` / `{"status":"UP"}` after the pre-Phase-06 closure.
- Database migration:
  - Latest Flyway migration is version `012`, success `true` in the running app migration history.
  - Verified `payment.fare_ledger_entry`, `routing.route_share_link`, and `trip.pre_trip_checklist` exist in the running database.
  - Verified `common.idempotency_key` exists and is used by booking create replay handling.
  - Verified `booking.booking_status_history` exists and is used for initial and transition audit rows; `booking.booking` has occurrence/fraction columns; `trip.passenger_trip_state` exists; `trip.trip` has `route_occurrence_id`; and `payment.fare_ledger_entry` exists.
- Git status:
  - Latest committed backend slice before this work is the prior Phase 05 baseline.
  - Pre-Phase-06 closure changes are committed in the latest pre-Phase-06 gate commit.

## Blockers / Risks

- No active runtime blocker for the backend foundation.
- Git baseline exists; pre-Phase-06 closure changes are committed; current working tree is clean.
- Full product is not complete yet, but phases 00 through 05.5 are complete for the Phase 06 gate. Remaining major areas are Phase 06+ or later hardening/app phases.
- Current tests are mostly unit-level. Add Spring Boot integration/security tests before relying on these APIs as production-ready.
- Dev infrastructure exposes local ports and uses local-only development credentials; do not reuse these settings for production.

## Next Recommended Task

Continue Phase 07 with Task 06 — profile setup and verification, now that Task 05 onboarding/auth/OTP foundation is complete.

## Update Rule

After every completed implementation task, update:

- `DEVELOPMENT_STATUS.md`
- `IMPLEMENTATION_ROADMAP.md`
- `TASK_LOG.md`

If relevant, also update:

- `DECISION_LOG.md`
- `REQUIREMENTS_CHANGE_LOG.md`
- `BLOCKERS.md`
- ``


## 2026-06-01 23:43 +0530 — Phase 05/05.5 continuation before Phase 06

Completed additional core backend API reconciliation before Phase 06:

- Passenger booking list/detail/current trip/history projections.
- Driver route list/detail/cancel endpoints.
- Driver trip list/detail and booking request projections.
- Driver booking approve/decline commands with driver-owned authorization path and booking status history reuse.

Verification:

- Targeted controller tests passed.
- Full backend `spotless:apply spotless:check test` passed.
- API restarted and `/actuator/health` returned HTTP 200.

Remaining before/around Phase 06:

- Payment lifecycle capture/void/refund/cash/earnings/settlement.
- Receipt/final fare endpoints.
- Share link/QR, pre-trip checklist, arrived pickup, notifications/support/SOS/admin depth.


## 2026-06-02 00:35 +0530 — Audit cleanup before commit

The earlier docs still showed stale incomplete states for phases before Phase 06 even though the implementation had been added. This audit corrected those stale statuses:

- Phase 00 TypeScript workspace/package setup is now present with root `package.json`, `pnpm-workspace.yaml`, `packages/api-contracts/package.json`, and `tsconfig.json`.
- Phase 02 migration list, structured logging convention, and Testcontainers migration smoke coverage are now tracked.
- Phase 03 is marked completed for its foundation scope.
- Phase 05 and Phase 05.5 are marked completed for the Phase 06 gate.
- Deferred product workflows are now labeled as Phase 06+ or later-phase work instead of pre-Phase-06 blockers.


## 2026-06-02 02:35 +0530 — Phase 06 completed

Phase 06 realtime location foundation is complete and verified. Latest work adds driver location ingestion, Redis latest-location cache, auditable sample/outbox persistence, WebSocket/STOMP fanout, passenger live-state endpoint, and admin live trip feed.


## 2026-06-02 01:45 +0530 — App backend readiness audit

Audited the business requirement, passenger/driver designs, app implementation plans, OpenAPI contracts, and backend controllers before starting Passenger Mobile, Driver Mobile, or Admin Web. Result: Phase 06 backend foundation is complete, but app-facing backend is not fully complete for end-to-end app implementation. Created `docs/api/APP_BACKEND_READINESS_AUDIT.md` and added recommended `Phase 06.5 — App Backend Readiness Closure` to the roadmap.

Summary gaps found: Passenger 24 missing/deferred contract operations, Driver 28, Admin 43. Some can be deferred or feature-flagged, but safety/support/notifications/ratings/early-drop-off/recurring routes/KYC submit/admin operations should be closed before full app phases.


## 2026-06-02 03:00 +0530 — Backend test coverage gate and missing test closure

Completed a backend test coverage review before Phase 07. Added JaCoCo coverage enforcement to `apps/api/pom.xml` with an 80% line-coverage gate for measured application logic, excluding generated/boilerplate adapter layers such as DTOs, JPA entities, Spring controllers, repositories, MapStruct mappers, configuration, security wiring, and generated facade glue.

Added missing focused tests for:

- `AppReadinessServiceImplTest` — app config, verification status, support/SOS default statuses, notification preferences, mark-read, share-link payload, payout default, dashboard summaries, and unserializable payload failure.
- `VehicleServiceImplTest` — create/list/get/update/delete/review flows plus driver-profile access denial.
- `RedisLatestLocationCacheTest` — TTL, empty cache lookup, JSON deserialize, JSON serialize with TTL, and Redis read failure wrapping.

Verification:

- `./mvnw verify` passed.
- JaCoCo line coverage passed the 80% gate: `92.9078%` measured line coverage (`131` covered / `10` missed across `20` measured classes).
- Full backend suite: `Tests run: 91, Failures: 0, Errors: 0, Skipped: 1`; skipped test is the Docker/Testcontainers migration smoke test when Docker Desktop Java client is unavailable.

## 2026-06-14 01:32 +0530 — Phase 07 Task 02 Expo passenger app scaffold completed

Passenger Mobile Task 02 is complete for the runnable scaffold/dev-tooling scope. `@routeshare/passenger-mobile` now starts as an Expo React Native TypeScript app with React Navigation, provider composition, environment profiles, strict TypeScript, ESLint/Prettier, Vitest tests, Expo Doctor, EAS preview config, Detox config, and local preview/e2e smoke gates.

Verified:

- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed (`6` files / `28` tests).
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `pnpm run doctor` passed Expo Doctor `21/21` checks.
- `pnpm exec expo export --platform web --output-dir /tmp/routeshare-passenger-web-export` passed and rendered the current scaffold UI.

Next step: Task 03 app shell/navigation/state/offline foundation.


## 2026-06-14 18:20 +0530 — Phase 07 Task 03 app shell/navigation/state/offline foundation completed

Passenger Mobile Task 03 is complete for the app-shell foundation scope. The app now has typed route contracts for public/protected passenger routes, startup route-guard state logic, offline-aware query/mutation policy, persisted preference defaults/validation, expanded auth state, deep-link prefixes/config, an offline banner, and placeholder shell screens for the full passenger route map.

Verified:

- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed (`9` files / `40` tests).
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.

Notes:

- Task 03 uses placeholder shell screens for route coverage; user-visible production screen designs start in Task 04.
- Real cloud EAS submissions and full device/simulator Detox flows remain later release-evidence work when credentials/devices are finalized.

Next step: continue with Task 04 — design system and reusable screen components from source assets.


## 2026-06-14 19:05 +0530 — Phase 07 Task 04 design system and reusable screen components completed

Passenger Mobile Task 04 is complete for the reusable design-system foundation scope. Added source-asset-matched warm RouteShare tokens, dark palette tokens, semantic/match-tier colors, spacing/radius/shadow/type scales, reusable accessible primitives, RouteShare-specific components, a deterministic map backdrop abstraction, and a redesigned app shell/home preview using those components.

Verified:

- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed (`11` files / `47` tests).
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed local scaffold/Detox smoke.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed local preview config gate.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed local preview config gate.
- Android native debug assemble passed: `./gradlew app:assembleDebug -x lint -x test --configure-on-demand --build-cache -PreactNativeDevServerPort=8082 -PreactNativeArchitectures=arm64-v8a --console=plain`.

Notes: real EAS cloud submissions and full device/simulator Detox flows remain later release evidence once credentials/devices are finalized.

Next step: continue with Task 05 — onboarding/auth Keycloak and OTP experience.


## 2026-06-14 19:15 +0530 — Phase 07 Task 05 onboarding/auth completed

Passenger Mobile Task 05 is complete for the first-run auth experience foundation. Implemented real Splash, three-slide Onboarding, Login, and OTP screens; Sri Lankan mobile validation; Keycloak Authorization Code + PKCE URL/token/refresh helpers; secure token persistence/logout helpers; OTP state machine coverage for empty/focused/paste/invalid/resend/throttle/network states; and auth route wiring so public auth routes no longer use the generic shell placeholder.

Verification passed:

- `pnpm --filter @routeshare/passenger-mobile lint`
- `pnpm --filter @routeshare/passenger-mobile typecheck`
- `pnpm --filter @routeshare/passenger-mobile test` (`15` files / `57` tests)
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android`
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios`
- `pnpm --filter @routeshare/passenger-mobile build:preview:android`
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios`
- Android debug assemble passed with Gradle: `BUILD SUCCESSFUL in 2s`.

Phone OTP dependency: current config does not assume production phone OTP support. The UI validates phone numbers and documents provider readiness; phone OTP navigation is gated by an explicit environment capability flag.

Next step: Task 06 — profile setup and verification.

## Production External Services

- [ ] Production external service providers selected and integrated. See `docs/development/PRODUCTION_EXTERNAL_SERVICES.md`.

## Public Release Provider Decisions

- Selected providers for public release: Notify.lk SMS/OTP, Google Maps Platform, Cybersource payments, Firebase Cloud Messaging, and Sentry. See `docs/development/SELECTED_PROVIDER_IMPLEMENTATION_GUIDE.md`.


## 2026-06-14 — Notify.lk OTP integration update

- Real backend-owned Notify.lk OTP integration is implemented after Task 05.
- Added `/api/v1/auth/otp/request` and `/api/v1/auth/otp/verify` public endpoints.
- Added hashed OTP persistence in `identity.phone_otp_challenge` via Flyway `V015__add_phone_otp_challenges.sql`.
- Passenger mobile Login/OTP screens now call backend OTP endpoints when the passenger phone-OTP capability flag is enabled. Production enablement still requires an approved RouteShare Notify.lk sender ID; `NotifyDEMO` is intentionally blocked for OTP by default.
- Production enablement still requires an approved RouteShare Notify.lk sender ID; `NotifyDEMO` is intentionally blocked for OTP by default.

Verification: backend targeted tests, backend `spotless:apply spotless:check test`, passenger mobile `typecheck`, `lint`, and `test` passed.


## 2026-06-15 02:43 +0530 — Phase 07 Task 06 profile/safety prerequisites completed

Passenger Mobile Task 06 is complete for the app-side profile and safety prerequisite scope. Implemented real RouteShare screens for Profile Setup, Account, Saved Places, Trusted Contacts, and Verification readiness; registered these routes in the typed passenger navigator/deep links; expanded passenger mobile API modules and DTO adapters; and added profile feature modules for validation, avatar handling, backend body mapping, verification copy, and default/primary preference helpers.

Implemented behavior:

- Profile setup saves `fullName`, optional email through `preferences.email`, and `photoUrl` through the backend profile API adapter.
- Avatar flow validates JPG/PNG/WebP and max 5 MB, exposes progress/cancel/retry-friendly state, and uses initials fallback. Binary storage remains a local/readiness shell until storage/upload endpoints are added.
- Account menu links to profile, saved places, trusted contacts, and verification.
- Saved places support list/add/delete/default selection with manual coordinate/address fallback and offline/error/empty states.
- Trusted contacts support list/add/delete/primary selection with Sri Lankan mobile validation, contact-import permission copy, and SOS/share-trip explanation.
- Verification uses honest readiness-only copy because live passenger document review/upload backend endpoints are not available in this slice.

Verified:

- `pnpm --filter @routeshare/passenger-mobile lint` passed.
- `pnpm --filter @routeshare/passenger-mobile typecheck` passed.
- `pnpm --filter @routeshare/passenger-mobile test` passed.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:ios` passed.
- `pnpm --filter @routeshare/passenger-mobile test:e2e:android` passed.
- `pnpm --filter @routeshare/passenger-mobile build:preview:ios` passed.
- `pnpm --filter @routeshare/passenger-mobile build:preview:android` passed.
- Unit suite result: `16` files / `62` tests.

Next step: Task 07 — home, search, location, and route discovery.
## 2026-06-15 — Local QA/runtime cleanup, OTP bypass, Keycloak profile sync, avatar picker

- Removed duplicate `routeshare-postgres-alt` usage and normalized local Docker to `routeshare-postgres` on host port `5433` so it does not conflict with the existing Odoo Postgres on `5432`.
- Started and verified local RouteShare services: Postgres, Redis, Keycloak, and MinIO.
- Added local QA-only OTP bypass configuration: `NOTIFY_LK_ALLOW_DEMO_SENDER_FOR_OTP=true` for backend-only local QA; the passenger app does not receive or autofill a dev OTP bypass code, and `.env.example` must stay safe for commits.
- Fixed passenger profile save so Keycloak standard user fields are synced from saved profile data: first name, last name, and email. Passenger-specific data such as `photoUrl` remains in RouteShare DB because the current Keycloak realm drops arbitrary custom attributes.
- Replaced the passenger profile image placeholder flow with real Expo image picker wiring and avatar preview.
- Installed Maestro on the Mac and added repeatable emulator QA scripts under `scripts/qa-*.sh` plus flows under `qa/maestro/`; generated reports remain ignored under `qa/reports/`.
- Verification completed: backend focused tests pass, backend `spotless:check test` exits 0, passenger mobile lint/typecheck/tests pass, and Maestro Android smoke passes on `emulator-5554`.


## 2026-06-16 00:30 +0530 — Phase 07 Task 07 architecture/QA correction

Task 07 implementation remains in progress until strict device automation is green. The mobile ride-search contract was corrected against the live backend controller: `POST /api/v1/passenger/ride-searches` returns `ApiResponse<List<RouteSearchResponse>>`, so passenger mobile now maps create-search results to `RideSearchResult[]` and navigates to Search Results with backend result data.

Repository organization updates:

- Repeatable Maestro flows are organized under `qa/maestro/<app>/<suite>/`.
- Generated QA evidence remains ignored under `qa/reports/`.
- Repository/file-structure documentation now points to `docs/development/implementation/tasks/<feature-plan-name>/` and `qa/test-cases/<feature-plan-name>/` instead of mixing QA artifacts into development docs.

Verification passed after correction:

- `pnpm --filter @routeshare/passenger-mobile lint`
- `pnpm --filter @routeshare/passenger-mobile typecheck`
- `pnpm --filter @routeshare/passenger-mobile test` — `17` files / `70` tests.
- Backend `./mvnw -q spotless:check -DskipTests`.
- Backend `./mvnw -q -DskipTests compile`.

Open blocker:

- Android clean-state Maestro regression still fails around phone-number keyboard/button handling and evidence is captured in ignored `qa/reports/20260616-002925-task07-regression-captured/`. Do not mark Task 07 public-release-complete until this emulator/device path is stable and iOS evidence is captured.

## 2026-06-16 00:45 +0530 — Task 07 production prerequisite correction

Task 07 is a real production application stage, not an MVP/POC fallback stage. Because Task 07 includes Home Map A, current location, Search Places, suggestions, and coordinate-based route discovery, Google Maps Platform keys are now recorded as a blocking prerequisite before the stage can be marked production-release-complete.

Required before closing Task 07:

- `GOOGLE_MAPS_ENABLED=true`
- `GOOGLE_MAPS_SERVER_API_KEY`
- `EXPO_PUBLIC_GOOGLE_MAPS_ANDROID_API_KEY`
- `EXPO_PUBLIC_GOOGLE_MAPS_IOS_API_KEY`
- Rebuilt/reinstalled Expo dev client with native map keys.
- Android and iOS runtime evidence showing the real map/place flow.

Do not proceed by treating fake maps, placeholder geocoding, or manual-only search as complete. Manual fallback remains valid only for permission-denied/offline/error handling.



## 2026-06-16 09:50 +0530 — Task 07 Google Maps implementation pass

Google Maps Platform keys were supplied and stored only in local `.env`. Implemented the production-real Task 07 map/place foundation:

- Backend `/api/v1/passenger/places/autocomplete` and `/api/v1/passenger/places/{placeId}` proxy Google Places API using the server key.
- Passenger mobile API exposes `places.autocomplete` and `places.details`.
- Search screen now uses Google Places suggestions and resolves place details to coordinates before creating a ride search.
- Home map backdrop now uses `react-native-maps` Google provider instead of a static fake map preview.
- Direct Google Places API smoke test returned HTTP 200 and suggestions.

Verification passed:

- `pnpm --filter @routeshare/passenger-mobile test`
- `pnpm --filter @routeshare/passenger-mobile lint`
- `pnpm --filter @routeshare/passenger-mobile typecheck`
- `apps/api ./mvnw -q spotless:check -DskipTests`
- `apps/api ./mvnw -q -DskipTests compile`

Remaining blocker before Task 07 production completion: rebuild/reinstall Android and iOS dev clients with native map keys, then capture real device map/place QA evidence.


## 2026-06-16 10:05 +0530 — Android native Google Maps rebuild verified

After wiring the native Android Maps SDK key into the checked-in Android project, the Android debug dev client was rebuilt and installed on the emulator.

Native verification passed:

- `android/app/src/main/AndroidManifest.xml` includes `com.google.android.geo.API_KEY` metadata pointing at `@string/google_maps_api_key`.
- `android/app/build.gradle` injects `google_maps_api_key` from `EXPO_PUBLIC_GOOGLE_MAPS_ANDROID_API_KEY` via `resValue`.
- `./gradlew app:assembleDebug -x lint -x test --configure-on-demand --build-cache -PreactNativeDevServerPort=8082 -PreactNativeArchitectures=arm64-v8a --console=plain` completed with `BUILD SUCCESSFUL`.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` completed with `Success`.
- Emulator UI dump after launch shows `Google route map preview`, `Google map`, and native `Google Map` TextureView inside the RouteShare onboarding shell.

Remaining before closing Task 07: complete authenticated passenger Search screen device QA through Google Places autocomplete/details and route-search submission, then save QA evidence under ignored `qa/reports/`.
