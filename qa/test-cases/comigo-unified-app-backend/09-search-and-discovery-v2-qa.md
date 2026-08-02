# QA — Task 09: Search and Discovery v2

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/09-search-and-discovery-v2.md`

## Scope

Trip-start radius semantics at 5/10/20 km with a 20 km ceiling, the filtered-out count, match
tiers, the enriched result payload, server-side sorts, the commuter dashboard, and QR/short links.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V036` (slice 08 took `V035`; this slice is `V036`).
- A seeded corridor with drivers starting at 2, 6, 14 and 25 km from the test pickup point.

## Automated test coverage

Five of the seven classes the plan named are folded into `TripStartRadiusIT`. They all need the
same seeded corridor and the same live PostGIS, and splitting them would have meant standing four
more containers up to assert four facts about one query.

- `TripStartRadiusIT` (10) — the predicate is trip-start distance, not pickup proximity (`09-1`–`09-3`);
  the filtered-out count is exact and reconciles (`09-6`); `startsKmAway` is projected (`09-7`);
  eligibility is applied inside the query (`09-13`); paging is stable (`09-10`/`09-11`); the GIST
  index is used against 5,000 rows (`09-19`); and the origin-point trigger holds.
  Covers what the plan called `FilteredOutCountTest`, `SearchSortStabilityTest`, `SearchEligibilityIT`
  and `SearchQueryPlanIT`.
- `MatchTierTest` (6) — thresholds shared with the discount selector, boundaries agreeing at every
  band, and a mapping with no default arm so a new band cannot be silently absorbed.
- `PickupPointResolutionTest` (6) — curated beats persisted beats route label beats Places; a
  persisted corner writes nothing new; the raw fallback always yields a usable point.
- `MatchingSettingsServiceImplTest` (6) — the 20 km product ceiling, a default that must be one of
  the offered chips, and an offered radius above the maximum.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 09-1 | Radius 20 km | Drivers at 2, 6 and 14 km returned; 25 km excluded |
| 09-2 | Radius 10 km | Drivers at 2 and 6 km returned |
| 09-3 | Radius 5 km | Driver at 2 km returned |
| 09-4 | Radius 25 km requested | 400 `RADIUS_EXCEEDS_MAXIMUM` |
| 09-5 | Radius 7 km requested | 400 `RADIUS_NOT_ALLOWED` |
| 09-6 | `filteredOutByRadius` at radius 10 | Exactly 2 |
| 09-7 | `startsKmAway` on each result | Present and matching the seeded distance |
| 09-8 | Tier for 100% match | `FULL_ROUTE`, and discount 10% |
| 09-9 | Tier for 74% match | `PART_OF_ROUTE`, and discount 5% |
| 09-10 | Sort `CHEAPEST` across two pages | No duplicate, no omission |
| 09-11 | Sort `SOONEST` with tied departures | Stable tiebreak |
| 09-12 | Result payload completeness | Every P04 card field present without client arithmetic |
| 09-13 | Ineligible trip and paging | Excluded inside the query; `totalMatching` consistent |
| 09-14 | Usual commute set, then read | Match count returned with the best match's driver, price and departure |
| 09-15 | Short link generated | 10-char code, resolves to the occurrence |
| 09-16 | Short link revoked | 404, not 410 |
| 09-17 | QR requested | PNG rendered and cached; second request served from cache |
| 09-18 | Driver origin coordinates in any response | Never — distance and label only |
| 09-19 | Query plan | GIST index seek, not a sequential scan |
| 09-20 | Resolve a coordinate beside a curated point | Curated point returned, `source=CURATED` |
| 09-21 | Resolve a coordinate with no curated point nearby | Nearest transit stop / POI via Places, `source=DERIVED` |
| 09-22 | Second rider resolves the same corner | Reuses the persisted derived row; no second Places call |
| 09-23 | Resolve where Places returns nothing usable | Raw coordinate with a generated label; booking still succeeds |
| 09-24 | Booking created | Carries `pickupPointId` with label, description and side hint |
| 09-25 | Admin creates a curated point overlapping a derived one | Curated wins on subsequent resolves |
| 09-26 | Places call volume | At **booking** time only — never per location ping |
| 09-27 | Resolution chain order | curated → persisted derived → route label → Places → raw; Places is genuinely last, hit rates instrumented per tier |
| 09-28 | Coordinate matching an existing route's origin label | Resolved from the route label; **no Places call** |
| 09-29 | Place Details field mask | Essentials only; a Pro-tier field **fails the build** |
| 09-30 | 100 bookings across 20 distinct corners | ≤20 Places calls; the remaining 80 served from persisted rows |
| 09-31 | Curated seed loaded | Launch-corridor pickups resolve with zero Places calls |

## Run of record — 2026-08-03

`scripts/simulation/verify-search-v2.sh` → **41 passed, 0 failed, 0 skipped**, on PostgreSQL 5434 /
API 8088 against `routeshare_comigo`, with `V036` applied cleanly to the live database.
`scripts/simulation/seed-pickup-points.sh` → 40 curated landmarks, idempotent on a second run.

Defects the run found, all fixed:

- **In the migration, not the script**: `idx_route_plan_origin` was specified on the geometry, but
  the search filters over geography. The index was silently ineligible and the planner fell back to
  a sequential scan. Nothing failed — the query would simply have decayed as the route table grew.
- The seed loop read landmarks from stdin while `sim_psql` shells out to `docker exec -i`, which
  consumes stdin: it seeded exactly one of forty and stopped without complaining.
- The smoke's fixture copied a bucket cell from an existing row instead of computing it, attaching
  the trips to somebody else's corridor. Every search returned nothing, which read as a radius
  failure rather than a fixture that was never reachable.
- The original `09-6` assertion ("exactly one more") was really asserting how many times the script
  had been run, since the count is corridor-wide and earlier runs leave their trips behind. Replaced
  with the two invariants that do hold: the totals reconcile at every radius, and tightening the
  radius never removes fewer trips.

## Deviations from the task file

- **No new policy settings.** The radius lives in `routing.matching_settings` alone; V036 deletes
  the orphaned `SEARCH_RADIUS_KM` row V029 seeded and nothing read. Match-tier thresholds are the
  discount thresholds — `MatchTier` derives from `MatchDiscountTier` rather than duplicating 95/75/45.
- **`V036`, not `V035`** — slice 08 took that number.
- **A derived pickup point is labelled by its address, not a landmark name.** `displayName` is a
  Pro-tier Places field and one Pro field re-prices the entire request. Real landmark names come
  from the curated tier, which is what `seed-pickup-points.sh` exists for.

## Manual checks

- Capture `EXPLAIN ANALYZE` for the search query at each radius and attach it.
- Confirm the rate limit on `ride-searches` engages under a scripted burst.
- Measure the pickup-point tier hit rates over a simulated day and confirm Places usage trends toward zero as the library fills.
- Confirm a `HIDDEN` or `MATCHED` photo never appears in a search result.

## Evidence to collect

- `scripts/simulation/verify-search-v2.sh` output.
- `EXPLAIN ANALYZE` plans at radius 5, 10 and 20.
- p95 latency measurement against the seeded dataset.

## Pass/fail criteria

Pass when: the radius filters on trip-start distance at exactly the three allowed values with a 20 km
ceiling; the filtered-out count is exact; tiers agree with discounts; sorts page stably; and the query
uses the GIST index.

Fail on: a sequential scan on the search path, a filtered-out count that disagrees, a tier that
contradicts the applied discount, or any raw driver coordinate in a response.
