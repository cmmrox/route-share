# QA — Task 09: Search and Discovery v2

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/09-search-and-discovery-v2.md`

## Scope

Trip-start radius semantics at 5/10/20 km with a 20 km ceiling, the filtered-out count, match
tiers, the enriched result payload, server-side sorts, the commuter dashboard, and QR/short links.

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V035`.
- A seeded corridor with drivers starting at 2, 6, 14 and 25 km from the test pickup point.

## Automated test coverage

- `TripStartRadiusIT` — the predicate is trip-start distance, not pickup proximity.
- `FilteredOutCountTest` — the count equals the difference, computed in one query.
- `MatchTierTest` — thresholds shared with the discount selector.
- `SearchSortStabilityTest` — stable ordering across pages for all three sorts.
- `SearchEligibilityIT` — slice 08's predicate applied inside the query so paging counts are right.
- `SearchQueryPlanIT` — the GIST index is used.
- `PickupPointResolutionTest` — curated beats derived; derived persists and is reused; fallback labels a raw coordinate.

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
