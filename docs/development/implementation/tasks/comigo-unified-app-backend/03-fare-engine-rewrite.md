---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 03 — Fare Engine Rewrite

**Goal:** Replace the base-fare + per-minute + fee-on-top calculator with the prototype's model: distance on route × that vehicle's rate, less a route-match discount, with commission taken out of the fare rather than added to it.

**Depends on:** 02.
**Blocks:** 04, 06, 07, 09, 11, 13, 14. Every money surface in the product.

## Objective

Today: `total = 250 base + 90/km + 5/min`, then a 10% platform fee is **added**. The passenger pays
subtotal + fee and the driver's share is never expressed.

Prototype: `gross = onRouteKm × vehicle.ratePerKm`, `discount = gross × matchDiscountPct(match)`,
`price = gross − discount`. The 10% commission comes **out of** `price`, so the driver nets 90%. There is
no base fare and no time component. This is the number on P04, P07, P08, P09, P17, D13, D16, D25 and D26,
and none of them can be built until it is right.

This slice also introduces the runtime policy surface that decision D1 requires, because the discount
tiers, commission rate and every other figure must be tunable without a deploy.

## Scope

In scope:

- `platform.policy_setting` — the typed, runtime-configurable home for every `POLICY` value.
- New fare engine: rate-band pricing, match-discount tiers, commission-inside, min-fare floor.
- `FareQuote` v2 persisted per booking so a fare can always be re-explained after the fact.
- Reprice every read surface: search results, ride detail, seat select, checkout, receipt, driver trip detail, driver earnings, ledger.
- Retire `base_fare` / `per_min` semantics from `finance.fare_policy` and the admin surface that edits them.
- Delete `pricing/domain/FareCalculator` and its callers.

Out of scope:

- **When** money moves — slice 04.
- Dues, credits and penalty lines on the fare — slices 06 and 11 add them as separate quote lines.
- Early-drop recalculation — the engine exposes `repriceForActualDistance()` here; the allowance rule is slice 05.

## Source material / references

- `docs/source-assets/comigo-prototype/data.jsx` — `FARE_POLICY`, `POLICY`, `matchDiscountPct`, `fareAtRate`, `driverNet`, `RIDES`, `MY_TRIP`, `SEAT_FARE`, `SEAT_NET`.
- `docs/source-assets/comigo-prototype/passenger-discover.jsx` — P07 `FareBreakdown` lines and footnote.
- `docs/source-assets/comigo-prototype/passenger-book.jsx` — P09 checkout breakdown across all five variants.
- `docs/source-assets/comigo-prototype/passenger-trip.jsx` — P17 receipt (card / cash / adjusted).
- `docs/source-assets/comigo-prototype/driver-supply.jsx` — D13 "fare for the full route", `you keep per seat`.
- `docs/source-assets/comigo-prototype/driver-money.jsx` — D26 ledger convention: gross in, commission out.
- Current code: `pricing/**`, `payment/service/impl/PaymentServiceImpl.java` (commission split), `routing/service/impl/RouteServiceImpl` (search enrichment), `finance/entity/FarePolicyEntity.java`.

## Architecture and design notes

**The formula, exactly.**

```
onRouteMeters   = ST_Length of the matched substring of the driver's route line   (already computed)
grossFare       = round( onRouteKm × vehicle.chosenRatePerKm )                    × seats
discountPct     = match ≥ 95 → 10 | ≥ 75 → 8 | ≥ 45 → 5 | else 2.5               (policy)
discountAmount  = round( grossFare × discountPct / 100 )
passengerPays   = grossFare − discountAmount
commission      = round( passengerPays × commissionPct / 100 )                    (policy, 10%)
driverNet       = passengerPays − commission
```

Two invariants that must hold as tests, not comments:

1. `driverNet + commission == passengerPays` — always, for every rounding path.
2. `passengerPays` is derived only from server-side data. The client never sends a distance or a fare.
   `POST /pricing/estimate` accepting a client distance is removed.

**Commission is inside.** The passenger sees one price. The driver sees the same price and what they keep.
The ledger convention from D26 is *gross fare in, commission out* as two rows — not a net row, which
would double-count when the fee row is also present.

**`FareQuote` is persisted, not recomputed.** A receipt shown three months later must show the fare that
was actually charged, at the rate that was actually in force, with the discount tier that actually
applied. Rate bands change; recomputing would rewrite history. Every booking stores its quote.

**The policy surface.** One table, typed accessor, seeded from the prototype:

```java
policy.decimal(PolicyKey.COMMISSION_PCT)          // 10
policy.decimal(PolicyKey.MATCH_DISCOUNT_TIER_95)  // 10
policy.integer(PolicyKey.PICKUP_WAIT_MIN)         // 5
```

No `POLICY` figure may be inlined as a Java constant anywhere. Enforced by an architecture test that
fails on magic numbers in the pricing, penalty and reliability packages.

**Rounding.** All money is `BigDecimal`, scale 2, `HALF_UP`, LKR. Discount and commission round
independently, then `driverNet` is computed by subtraction so invariant 1 cannot drift.

**Minimum fare.** `finance.fare_policy.min_fare` is the one field of that table that survives; a very
short overlap must not produce a fare below it. `base_fare`, `per_km` and `per_min` are retired.

## API contracts involved

Changed:

```
POST /api/v1/pricing/estimate-by-route     -> FareQuoteResponse v2   (kept, reshaped)
POST /api/v1/pricing/estimate               -> REMOVED               (accepted a client distance)
POST /api/v1/passenger/ride-searches        -> results carry the v2 quote per result
GET  /api/v1/passenger/bookings/{id}        -> booking carries its persisted quote
GET  /api/v1/passenger/bookings/{id}/receipt-> receipt built from the persisted quote
GET  /api/v1/driver/trips/{tripId}          -> per-passenger gross/commission/net
GET  /api/v1/driver/earnings/summary        -> net-only figures, stated as net
GET  /api/v1/driver/earnings/transactions   -> gross-in / commission-out rows
```

`FareQuoteResponse` v2:

| Field | Notes |
| --- | --- |
| `currency` | `LKR` |
| `onRouteDistanceMeters`, `onRouteDistanceKm` | server-derived |
| `ratePerKm`, `vehicleClassKey`, `classBand{min,max}` | drives P07's rate explanation |
| `seats` | |
| `grossFare` | |
| `matchPercent`, `matchTier`, `discountPercent`, `discountAmount` | drives the discount line |
| `passengerPays` | the single headline number |
| `commissionPercent`, `commissionAmount`, `driverNet` | shown to drivers only |
| `minFareApplied` | boolean |
| `quotedAt`, `policyVersion` | reproducibility |

Admin: `finance.fare_policy` edit endpoints lose `baseFare`/`perKm`/`perMin`; new
`GET/PUT /api/v1/admin/policy-settings` exposes the policy surface with audit.

New error: `RATE_BAND_NOT_SET` when a route's vehicle has no active band — a trip must not be publishable
or priceable without one.

## Database / migration changes

**`V029__fare_engine_rewrite.sql`**

- New `platform.policy_setting`:
  `policy_key TEXT PRIMARY KEY`, `value TEXT NOT NULL`, `value_type TEXT CHECK (value_type IN ('INT','DECIMAL','BOOLEAN','STRING'))`,
  `description TEXT`, `updated_at`, `updated_by_app_user_id`.
  Seeded with every `POLICY` and `FARE_POLICY` value from `data.jsx`.
- New `platform.policy_setting_history` — `policy_key`, `old_value`, `new_value`, `changed_at`, `changed_by_app_user_id`. Price rules need a paper trail.
- New `pricing.fare_quote` v2 (replaces the existing thin table):
  `fare_quote_id`, `booking_id FK NULL`, `route_occurrence_id FK NULL`, `vehicle_id FK`,
  `on_route_distance_m NUMERIC(12,2)`, `rate_per_km NUMERIC(6,2)`, `seats INT`,
  `gross_fare NUMERIC(12,2)`, `match_percent NUMERIC(5,2)`, `match_tier TEXT`,
  `discount_percent NUMERIC(5,2)`, `discount_amount NUMERIC(12,2)`,
  `passenger_pays NUMERIC(12,2)`, `commission_percent NUMERIC(5,2)`,
  `commission_amount NUMERIC(12,2)`, `driver_net NUMERIC(12,2)`,
  `min_fare_applied BOOLEAN`, `currency TEXT DEFAULT 'LKR'`, `quoted_at`, `policy_version TEXT`,
  `CHECK (driver_net + commission_amount = passenger_pays)`,
  `CHECK (passenger_pays = gross_fare - discount_amount)`.
  The two CHECKs make invariant 1 a database guarantee.
- `booking.booking` — add `fare_quote_id FK`. `fare_estimate` retained as a denormalised copy of
  `passenger_pays` for existing queries, kept in sync by the service.
- `finance.fare_policy` — drop `base_fare`, `per_km`, `per_min`. Keep `min_fare`, `currency`, `active`.
- Old `pricing.fare_quote` rows and existing `booking.fare_estimate` values are dev-only; recreated by
  `scripts/simulation/seed-demo-route.sh` (decision D6).

## Configuration / environment changes

- All former pricing constants move out of `application.yml` into `platform.policy_setting`.
- `ROUTESHARE_POLICY_CACHE_TTL_SECONDS` (default `60`) — policy reads are hot; cached in Caffeine, evicted on write.

## UI / UX requirements

Backend slice. The contract must supply every line each screen draws, with no client arithmetic:

- P07 — "On-route distance · X km at LKR Y/km" + "Route-match discount · N% overlap" + total + footnote naming the commission and the early-drop rule.
- P08 — per-seat price, per-km rate, and the class band for context.
- P09 (and P09b/c/d/e) — the same breakdown plus the variant-specific lines added by later slices.
- P17 / P17b / P17c — booked distance, discount, early-drop refund, route adjustment, paid total.
- D13 — full-route fare per seat and "you keep per seat" after the fee.
- D16 — fare and net for an inbound request.
- D26 — gross fare row and a separate negative commission row.

## Implementation steps

1. Add `platform.policy_setting` + `PolicySettingService` with a typed accessor and Caffeine cache; seed from `data.jsx` values; add history + admin CRUD with audit.
2. Add an architecture test forbidding numeric literals for policy values in `pricing`, `penalty`, `reliability` packages.
3. Write `pricing/domain/MatchDiscountTier` (95/75/45 bands) and `pricing/domain/FareEngine` implementing the formula above, pure and fully unit-tested.
4. Add `pricing.fare_quote` v2 entity/repository and `FareQuoteService` — `quote(...)` and `persistFor(bookingId, quote)`.
5. Expose `PricingFacade.quoteForMatch(routeOccurrenceId, vehicleId, onRouteMeters, matchPercent, seats)`.
6. Rewrite `RouteServiceImpl` search enrichment to call the facade per candidate, replacing the current `FareCalculator` usage; return `matchTier` alongside the quote.
7. Persist the quote at booking creation and link it on `booking.fare_quote_id`; `fare_estimate` mirrors `passenger_pays`.
8. Rebuild receipt assembly from the persisted quote instead of recomputing.
9. Rewrite the driver earnings summary/transactions to the gross-in / commission-out convention.
10. Update `PaymentServiceImpl`'s commission split to read the persisted quote rather than recomputing from `CommissionProperties`, so payment and pricing can never disagree.
11. Add `repriceForActualDistance(quoteId, actualMeters)` returning a new quote — used by slice 05's early drop-off, unused here.
12. Delete `pricing/domain/FareCalculator`, `FareBreakdown`, `POST /pricing/estimate`, and the `base/per_km/per_min` admin fields.
13. Regenerate contract + `packages/api-contracts`.

## Files expected to change

- `apps/api/.../platform/**` — policy settings, history, admin CRUD.
- `apps/api/.../pricing/**` — `FareEngine`, `MatchDiscountTier`, quote entity/repo/service/facade; deletions.
- `apps/api/.../routing/service/impl/RouteServiceImpl.java` — search enrichment.
- `apps/api/.../booking/**` — quote persistence + link.
- `apps/api/.../payment/**` — commission read from quote; receipt assembly.
- `apps/api/.../finance/**` — fare policy field removal.
- `apps/api/.../admin/**` — policy settings endpoints, fare-policy admin surface change.
- `apps/api/src/main/resources/db/migration/V029__fare_engine_rewrite.sql`.
- `apps/api/src/test/java/**` — engine unit tests, invariant property tests, architecture test, search/receipt integration tests.
- `docs/api/mobile-app.openapi.json`, `docs/api/admin-web.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/03-fare-engine-rewrite-qa.md`

Maestro: not applicable — no mobile surface in this slice. The mobile plan reruns
`qa/maestro/mobile/regression/task08-results-list-map-filtering-ride-detail.yaml` after the app consumes
v2 quotes; that rerun is tracked there, not here.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='FareEngineTest,MatchDiscountTierTest,FareQuoteInvariantTest,PolicySettingTest,PricingArchitectureTest' test
```

```bash
bash scripts/simulation/verify-fare-engine.sh
```

The smoke script must reproduce the prototype fixtures exactly: an 11.4 km full route at LKR 50/km →
gross 570, and a 5.8 km 92%-match seat at LKR 50/km → gross 290, discount 23, price 267, driver net 240.
A mismatch against `data.jsx` fails the build.

## Security, privacy, and observability checks

- Price is server-authoritative. Add a test asserting that no pricing input is read from the request body — a client-supplied distance or rate is a free-money bug.
- Only `ADMIN`/`SUPER_ADMIN`/`FINANCE_ADMIN` may write policy settings; every write audited with old and new value.
- `driverNet` and `commissionAmount` must never appear in a passenger-facing response; assert by contract test.
- Log every quote at DEBUG with inputs and outputs; never log at INFO — quote volume equals search volume.
- Metrics: `routeshare_fare_quotes_total{tier}`, `routeshare_min_fare_applied_total`, and a histogram of `passengerPays`. A sudden shift in tier distribution means matching changed, not pricing.
- Alert on any `fare_quote` insert rejected by the CHECK constraints — that is an arithmetic bug reaching the database.

## Done criteria

- [ ] `FareEngine` reproduces every money figure in `data.jsx` exactly.
- [ ] Both invariants hold as database constraints and as property tests.
- [ ] Every `POLICY` value lives in `platform.policy_setting`; the architecture test forbids inlining.
- [ ] `FareCalculator`, `FareBreakdown` and `POST /pricing/estimate` are deleted.
- [ ] `finance.fare_policy` no longer exposes base/per-km/per-min, and the admin surface matches.
- [ ] Search, ride detail, seat select, checkout, receipt, driver trip detail, earnings and ledger all read the v2 quote.
- [ ] Payment commission is read from the persisted quote, not recomputed.
- [ ] Quotes are immutable once persisted; re-reading an old booking shows the original figures.
- [ ] `./mvnw spotless:check verify` green, JaCoCo 80% held.
- [ ] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): rewrite the fare engine to rate-band pricing with commission inside the fare"
```
