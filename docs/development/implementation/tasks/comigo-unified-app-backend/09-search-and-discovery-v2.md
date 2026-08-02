---
phase: comigo-unified-app-backend
release_standard: production-ready-per-task
---

# Task 09 — Search and Discovery v2

**Goal:** Filter on where the driver's trip *starts*, at the radii the product actually offers, and return results rich enough that the client never computes a fare, a tier, or a reason.

**Depends on:** 03, 08.
**Blocks:** 12, 13.

## Objective

`matching_settings` defaults to a **1 km** radius measured on *pickup proximity*. The prototype specifies
**5 / 10 / 20 km**, measured on **where the driver's trip starts**, with 20 km as a hard ceiling and an
explicit product reason: "further than that and a driver is making a trip for you, not sharing one".
That is a twentyfold difference in magnitude on a different geometric predicate.

P04 also states what the radius removed — "6 more drivers start further than 20 km away" — because a
silently short list reads as "no drivers", which is a different and much worse message.

## Scope

In scope:

- Radius semantics: trip-start proximity, options 5/10/20 km, ceiling 20 km, measured from the typed pickup point.
- The filtered-out count.
- Match tiers returned by the server (Full ≥95, Most ≥75, Part ≥45).
- Enriched result payload: rate/km, gross, discount, price, `startsKmAway`, seats free, eligibility flags, overlap sentence, vehicle class and band.
- Server-side sorts: best match, cheapest, leaves soonest.
- Commuter dashboard: saved usual commute and its live match count.
- Trip QR code and short link.
- **Named pickup points** — resolving a raw coordinate to a landmark with a human description, which is what makes the rendezvous work (slice 12 consumes it).

Out of scope:

- The "Riding now" joinable tab — slice 13.
- Eligibility rules themselves — slice 08; this slice applies the predicate it built.
- Fare arithmetic — slice 03; this slice calls the facade.

## Source material / references

- `docs/source-assets/comigo-prototype/passenger-discover.jsx` — P01 home, P02 commuter dashboard, P03 search + radius chips, P04 list + filtered-out card, P05 grouped tiers, P06 map, P07 detail.
- `docs/source-assets/comigo-prototype/data.jsx` — `POLICY.searchRadiusKm/searchRadiusOptions`, `RIDES` (the full result shape), `RADIUS_FILTERED_OUT`, `USUAL_COMMUTE`, `matchTier` thresholds.
- `docs/source-assets/comigo-prototype/driver-supply.jsx` — D14 published + QR + short link.
- Current code: `routing/service/impl/RouteServiceImpl`, `routing/domain/RouteMatchScorer`, `routing/entity/MatchingSettingsEntity`, `routing/entity/RouteBucketCellEntity`, `admin/controller/AdminMatchingSettingsController`.

## Architecture and design notes

**The predicate changes, not just the number.** Today the radius filters candidates by how close the
*pickup point* is to the driver's route. The new rule filters by the distance from the rider's pickup
point to the driver's **trip origin** — `ST_Distance(pickup, ST_StartPoint(route_line))`. Pickup
proximity remains a *scoring* input in `RouteMatchScorer`; it stops being the *filter*.

**Both numbers are needed.** `startsKmAway` is shown on every result card (P04), so the query must
project the distance it filtered on, not just use it in a WHERE clause.

**Filtered-out count is a second aggregate**, computed in the same query with a windowed count of
candidates that passed every predicate except the radius. Running two round trips would let the two
numbers disagree.

**Tiers are server-derived** from the same `matchPercent` the discount tier uses, so a rider can never see
"Full route" beside an 8% discount. One thresholds table, two consumers.

**Sorting moves server-side** because results will page. `bestMatch` = match desc, price asc;
`cheapest` = price asc, match desc; `soonest` = departure asc. Ties broken by occurrence id for stable
paging.

**Bucket cells still carry the heavy lifting.** `route_bucket_cell` pre-indexes corridors; the radius
change alters which cells are probed, not the mechanism. Expect an index change, not a rewrite.

**Named pickup points are the fix for the rendezvous problem.** A raw coordinate is not an instruction.
In Colombo a 50 m GPS error puts the pin on the wrong side of Galle Road or outside a different shop, and
no amount of filtering helps because the error is in the pin. The prototype already knows this — its chat
fixture reads *"I'll be at the Rajagiriya junction bus halt, not the roundabout. Silver Alto."* That is a
landmark and a description, and this slice makes it a system feature rather than something a driver has to
think to type.

Three tiers, layered:

| Tier | Source | When |
| --- | --- | --- |
| **1 — Curated** | operator-maintained landmark list per corridor | overrides everything; admin screen |
| **2 — Derived** | nearest transit stop / notable POI from Google Places at booking | **the launch default** — zero curation, works day one |
| **3 — Learned** | points where pickups actually succeed, promoted after repeated use | later, once usage data exists |

Ship tier 2 with the schema ready for tier 1; tier 3 follows.

**Cost containment matters here** — resolved naively this is the plan's single largest new Google line
item: 500 trips × 2 points/day ≈ 30,000 Place Details calls/month, roughly **$150**, enough on its own to
break the $200 monthly credit. Four measures reduce steady state to a few hundred calls:

1. **Route labels first.** Every published route already carries `origin_label` and `destination_label`, resolved through Places when the driver created it. Those landmark names are already paid for — check them before calling anything.
2. **Seed the curated tier up front.** Load ~200 Colombo landmarks (junctions, bus halts, stations, well-known buildings) once. A one-time effort that covers most launch-corridor pickups permanently.
3. **Persist every derived point and reuse it.** Colombo corridors have a finite number of sensible stopping places; the second rider at the same corner costs nothing, and the hit rate climbs toward 100% as the library fills.
4. **Resolve once per unique location at booking time.** Never per search keystroke, never per location ping.

Field mask stays at **Essentials** (`id,formattedAddress,location`). A single Pro-tier field would upgrade
the whole request to Pro pricing — the same trap the July 2026 work avoided by dropping `displayName`.

**Commuter dashboard is a saved search, not a new domain.** `USUAL_COMMUTE` is the rider's most-used
origin/destination pair with a habitual time; the match count is the same search executed with a small
window. Reuse, do not duplicate.

## API contracts involved

```
POST /api/v1/passenger/ride-searches
  { origin{lat,lng,label}, destination{...}, departAfter, seats,
    radiusKm: 5|10|20, sort: BEST_MATCH|CHEAPEST|SOONEST, page, size }
```

`RideSearchResponse`:

| Field | Notes |
| --- | --- |
| `results[]` | see below |
| `totalMatching` | before the radius filter |
| `filteredOutByRadius` | P04's card |
| `radiusKm`, `maxRadiusKm` | echo + ceiling |
| `page`, `size`, `hasMore` | |

`RideSearchResult`: `routeOccurrenceId`, `driver{displayName, ratingAverage, ratingCount, tripCount, photoUrl?}`,
`vehicle{make, model, colour, registration, classKey, classLabel, classBand{min,max}}`,
`ratePerKm`, `quote{...FareQuoteResponse v2}`, `matchPercent`, `matchTier`,
`onRouteDistanceKm`, `startsKmAway`, `seatsFree`, `departAt`, `arriveAt`,
`overlapSummary`, `womenOnly`, `verifiedRidersOnly`, `approvalMode`.

New:

```
GET  /api/v1/passenger/commute            -> saved usual commute + live match count (P02)
PUT  /api/v1/passenger/commute            -> set/clear
GET  /api/v1/driver/route-occurrences/{id}/share  -> { shortCode, shortUrl, qrPngUrl, qrSvg }
POST /api/v1/passenger/pickup-points/resolve      { lat, lng }
  -> { pickupPointId, label, description, sideHint, lat, lng, source: CURATED|DERIVED|LEARNED }
GET  /api/v1/admin/pickup-points?corridor=
POST /api/v1/admin/pickup-points                  -> curated tier 1 entry
PUT  /api/v1/admin/pickup-points/{id}
```

Admin: `PUT /api/v1/admin/matching-settings` now validates against the 20 km ceiling and the allowed option set.

New errors: `RADIUS_NOT_ALLOWED`, `RADIUS_EXCEEDS_MAXIMUM`.

## Database / migration changes

**`V036__search_discovery_v2.sql`** — `V035` was taken by slice 08's eligibility work.

- `routing.matching_settings` — replace radius semantics:
  drop `default_search_radius_meters`, `max_search_radius_meters`;
  add `default_trip_start_radius_m INT NOT NULL DEFAULT 20000`,
  `max_trip_start_radius_m INT NOT NULL DEFAULT 20000`,
  `allowed_trip_start_radii_m INT[] NOT NULL DEFAULT '{5000,10000,20000}'`,
  `CHECK (default_trip_start_radius_m <= max_trip_start_radius_m)`.
  In-place per decision D6.
- `routing.route_plan` — add `origin_point geometry(Point,4326)` populated as `ST_StartPoint(route_line)`, with a `GIST` index. Computing `ST_StartPoint` per row per query is the difference between an index seek and a scan.
- New `routing.pickup_point`:
  `id`, `label TEXT NOT NULL`, `description TEXT`, `side_hint TEXT`,
  `position geometry(Point,4326) NOT NULL`,
  `source TEXT CHECK (source IN ('CURATED','DERIVED','LEARNED'))`,
  `google_place_id TEXT`, `success_count INT DEFAULT 0`, `active BOOLEAN DEFAULT true`,
  `created_at`, `created_by_app_user_id`,
  `GIST` index on `position`, and a unique index on `google_place_id` where not null.
  `success_count` is what tier 3 will later promote on.
- `booking.booking` — add `pickup_point_id FK NULL`, `dropoff_point_id FK NULL`.
- New `passenger.usual_commute`:
  `app_user_id PK FK`, `origin_label`, `origin geometry(Point,4326)`, `destination_label`,
  `destination geometry(Point,4326)`, `habitual_time TIME`, `updated_at`.
- New `routing.route_occurrence_share`:
  `id`, `route_occurrence_id FK UNIQUE`, `short_code TEXT UNIQUE`, `created_at`, `revoked_at`.
  Short code is a 10-character base32 slug; QR is rendered from the short URL.
- Index `idx_route_plan_origin ON routing.route_plan USING GIST ((origin_point::geography))`.
  **Indexed as geography, not geometry as specified.** A radius in metres means `ST_DWithin` over
  geography, and a plain geometry index is silently ineligible for it — the planner falls back to a
  sequential scan over every published route and nothing fails, the query just decays as the table
  grows. Found by `TripStartRadiusIT`, which asserts the plan against 5,000 rows.
- A `BEFORE INSERT OR UPDATE OF route_line` trigger keeps `origin_point` in step with the line.
  **Added during implementation.** Step 1 says "keep it in sync on route creation", which every
  caller would have had to remember; a route line and an origin point that disagree would mis-file
  a trip silently and nothing downstream could detect it.
- Drop the now-unused pickup-radius index if one exists.

## Configuration / environment changes

- **Neither group of policy settings was added, deliberately.** The radius had three candidate
  homes — `routing.matching_settings` (which already has an admin screen and validation), the
  `SEARCH_RADIUS_KM` row V029 seeded and nothing ever read, and the three new keys specified here.
  `matching_settings` wins because it is the only one with an operator surface, and V036 deletes the
  orphaned policy row. The match-tier thresholds are the same three numbers as
  `MATCH_DISCOUNT_THRESHOLD_HIGH/MID/LOW`; a second copy is precisely how a rider sees "Full route"
  beside an 8% discount, so `MatchTier` is derived from `MatchDiscountTier`. That is what this
  slice's own design note asks for — "one thresholds table, two consumers".
- `ROUTESHARE_SHORT_LINK_BASE_URL` (default `https://comigo.lk/r/`).
- `ROUTESHARE_RATE_LIMIT_RIDE_SEARCH_PER_MIN` (default `30`) — search is the hottest query in the
  product and the easiest to abuse.
- QR rendering: add `com.google.zxing:core` + `javase`. Rendered server-side and cached; no external service.

## UI / UX requirements

Backend slice. The contract must supply:

- P03 — the three radius options, which is selected, the ceiling, and the explanatory copy inputs.
- P04 — every field on a ride card without arithmetic, plus the filtered-out count and its reason.
- P05 — a tier per result so grouping needs no thresholds client-side.
- P06 — the same results with coordinates for pins and a cheapest-fare summary.
- P07 — the class band and the driver's position in it, for the rate explanation.
- P02 — the usual commute, its match count, and the best match's driver, percentage, price and departure.
- P07 / P08 / P12 — the resolved pickup point's label, description and side-of-road hint, so the booking shows a landmark rather than a coordinate.
- D14 — short URL and QR for the published trip.

## Implementation steps

1. Add `origin_point` with a GIST index; backfill from `route_line`; keep it in sync on route creation.
2. Rewrite the candidate query: filter `ST_DWithin(origin_point, :pickup, :radiusMeters)`, project `startsKmAway`, and compute `filteredOutByRadius` as a windowed count in the same statement.
3. Update `matching_settings` semantics and the admin validation; reject radii outside the allowed set.
4. Add `routing/domain/MatchTier` reading thresholds from policy settings; return the tier on every result and reuse it in slice 03's discount selection so the two cannot diverge.
5. Enrich the result projection with vehicle class, band, rate, eligibility flags, seats free and the overlap summary sentence.
6. Apply slice 08's `EligibilityService` predicate inside the query, not as a post-filter, so paging counts are correct.
7. Implement the three server-side sorts with stable tiebreakers and keyset-friendly ordering.
8. Add `passenger.usual_commute` with get/put and a match-count query reusing the search path.
9. Add short codes and server-rendered QR for occurrences; cache the PNG in Redis.
9a. Implement `PickupPointService.resolve(lat,lng)` with the cost-ordered chain: **curated match → persisted derived match → existing route origin/destination label → Places (Essentials mask, cached) → raw coordinate with a generated label**. Persist every Places result so it is never fetched twice. Instrument the hit rate per tier.
9b. Attach the resolved point to bookings; expose admin CRUD for the curated tier; ship a seed script loading ~200 Colombo landmarks.
10. Re-run `EXPLAIN ANALYZE` on the search query against a seeded corridor dataset and record the plan in the QA evidence — this query is the hottest path in the product.

## Files expected to change

- `apps/api/.../routing/**` — origin point, query rewrite, `MatchTier`, matching settings, share codes.
- `apps/api/.../passenger/**` — usual commute.
- `apps/api/.../pricing/**` — tier reuse for discount selection.
- `apps/api/.../admin/**` — matching settings validation.
- `apps/api/src/main/resources/db/migration/V036__search_discovery_v2.sql`.
- `apps/api/pom.xml` — ZXing.
- `apps/api/src/test/java/**` — radius predicate tests, filtered-out count tests, tier tests, sort stability tests, eligibility-in-query tests, query plan assertion.
- `docs/api/mobile-app.openapi.json`, `docs/api/admin-web.openapi.json`, `packages/api-contracts/src/index.ts`.

## QA reference

`qa/test-cases/comigo-unified-app-backend/09-search-and-discovery-v2-qa.md`

Maestro: not applicable to this slice's backend work, but the existing
`qa/maestro/mobile/regression/task08-results-list-map-filtering-ride-detail.yaml` must be rerun once the
app consumes v2 results; that rerun is owned by the mobile feature plan and linked from the QA file here.

## Verification commands

```bash
cd apps/api && ./mvnw spotless:apply spotless:check verify
```

```bash
cd apps/api && ./mvnw -Dtest='TripStartRadiusIT,FilteredOutCountTest,MatchTierTest,SearchSortStabilityTest,SearchEligibilityIT,SearchQueryPlanIT' test
```

```bash
bash scripts/simulation/verify-search-v2.sh
```

The smoke must prove, against a seeded corridor: a driver starting 14 km away appears at radius 20 and
disappears at radius 10; the filtered-out count matches the difference exactly; tiers align with the
discount applied; sorts are stable across pages; and the query uses the GIST index rather than a
sequential scan.

## Security, privacy, and observability checks

- Search is unauthenticated-adjacent in cost terms: it is the most expensive query and the easiest to abuse. Keep the existing per-user rate limits and add one for `ride-searches` specifically.
- Do not return exact driver home coordinates. `startsKmAway` is a distance, and the origin label is a place name — never emit `origin_point` itself.
- Photo URLs in results must pass through slice 08's `PhotoVisibilityService`; a `HIDDEN` or `MATCHED` photo must not leak into a search response.
- Short codes must be unguessable (10 chars base32, ~50 bits) and revocable; a revoked code returns 404, not 410, so scanning cannot enumerate.
- Metrics: `routeshare_ride_searches_total{radiusKm,sort}`, `routeshare_search_results_returned`, `routeshare_search_filtered_out_by_radius`, `routeshare_search_duration_seconds` histogram. A rising filtered-out ratio is a supply problem, and the product should see it.
- Log the query plan cost at DEBUG only; alert if p95 search latency exceeds the agreed budget.

## Done criteria

- [x] Radius filters on trip-start distance, at 5/10/20 km, ceiling enforced at 20 km. An
      unoffered radius is **refused**, not clamped — clamping answers a question the rider did not ask.
- [x] `startsKmAway` projected per result; `filteredOutByRadius` computed in the same statement.
- [x] Match tiers returned and derived from the discount band itself, so they cannot diverge.
- [x] Result payload supplies every P04/P05/P06/P07 field with no client arithmetic.
- [x] Eligibility applied inside the query so paging counts describe the list the rider sees.
- [x] Three server-side sorts, every one of them ending on the occurrence id.
- [x] Usual commute stored; its match count runs the real search rather than a second query.
- [x] Short link and QR generated, cached and revocable. A revoked code answers **404, not 410**.
- [x] Pickup points resolve to a landmark with a description; curated wins; derived is persisted
      and reused; bookings carry the resolved point.
- [x] The resolution chain is cost-ordered and instrumented per tier; Places is genuinely last.
- [x] Field mask stays at Essentials — including the new nearby-search call. A landmark *name*
      lives in `displayName`, which is Pro-tier and would re-price the whole request, so a derived
      point is labelled by its address and real names come from the curated tier.
- [x] Search uses the GIST index; `TripStartRadiusIT` asserts the plan against 5,000 rows.
- [x] `./mvnw spotless:check verify` green — 576 tests, 0 skipped, JaCoCo held.
- [x] Runtime: `verify-search-v2.sh` 41/41 against a live stack with `V036` applied.
- [x] Tracking docs updated; focused commit ready.

## Suggested commit message

```bash
git commit -m "feat(api): search on trip-start radius with tiers, enriched results and server-side sorts"
```
