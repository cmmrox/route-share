# QA — Task 03: Fare Engine Rewrite

## Related implementation task

`docs/development/implementation/tasks/comigo-unified-app-backend/03-fare-engine-rewrite.md`

## Scope

The rate-band fare engine, match-discount tiers, commission-inside, persisted `FareQuote` v2, the
`platform.policy_setting` surface, and the repricing of every read surface. Out of scope: when money moves
(slice 04) and dues/credit lines (slices 06, 11).

## Preconditions

- Common preconditions from `README.md` in this folder.
- Migration series applied through `V029`.


## Automated test coverage

- `FareEngineTest` — reproduces every figure in `data.jsx`.
- `MatchDiscountTierTest` — 95/75/45 boundaries, including exact-boundary values.
- `FareQuoteInvariantTest` — property test over 0…1,000,000 for both invariants.
- `PolicySettingTest` — typed accessor, cache, eviction on write, history rows.
- `PricingArchitectureTest` — no numeric literals for policy values in pricing/penalty/reliability.
- `QuoteImmutabilityTest` — an old booking re-read shows its original figures after a rate change.

## Maestro automation

Not applicable — this slice has no mobile surface. Verification is automated tests plus the
runtime smoke script named below. When this behaviour later reaches a screen, the device evidence is
owned by the mobile feature plan and must link back to this QA file.

## Test cases

| # | Case | Expected |
| --- | --- | --- |
| 03-1 | 11.4 km full route at LKR 50/km | gross 570 |
| 03-2 | 5.8 km, 92% match, LKR 50/km | gross 290, discount 23, price 267, net 240 |
| 03-3 | 6.2 km, 100% match, LKR 52/km | gross 322, discount 32, price 290 |
| 03-4 | 4.5 km, 74% match, LKR 46/km | gross 207, discount 10, price 197 |
| 03-5 | Match exactly 95 | 10% tier, not 8% |
| 03-6 | Match exactly 75 | 8% tier |
| 03-7 | Match exactly 45 | 5% tier |
| 03-8 | Match 44 | 2.5% tier |
| 03-9 | Invariant `net + commission == pays` | Holds for every case above |
| 03-10 | Very short overlap | Min fare applied; `minFareApplied=true` |
| 03-11 | Vehicle with no active band | 409 `RATE_BAND_NOT_SET` |
| 03-12 | Client sends a distance in the body | Ignored or rejected — no pricing input from the client |
| 03-13 | `POST /pricing/estimate` | 404 — endpoint removed |
| 03-14 | Passenger response contains `driverNet` | Never — contract test |
| 03-15 | Rate band changed after booking | Receipt still shows the original quote |
| 03-16 | Constraint violation attempt (hand-crafted bad quote) | Database rejects the insert |

## Manual checks

- Diff every money figure across search, ride detail, seat select, checkout, receipt, driver trip detail, earnings and ledger against `data.jsx`. Any disagreement is a fail.
- Confirm `FareCalculator`, `FareBreakdown` and the old estimate endpoint are deleted, not deprecated.
- Confirm the admin fare-policy surface no longer offers base fare or per-minute.

## Automated test coverage status (2026-08-01)

All green under `./mvnw spotless:check verify` (319 tests). `FareEngineTest` asserts the `data.jsx`
fixtures directly and sweeps ~4,000 rounding paths for both invariants; `MatchDiscountTierTest`
pins the band edges; `PolicySettingTest` covers typed reads, cache eviction on write, history and
type validation; `PricingArchitectureTest` fails the build on an inlined policy figure, on a
surviving `FareCalculator`/`FareBreakdown`, or on any pricing input declared in a request DTO.

**Collected 2026-08-02 — Blocker 013 cleared.** `scripts/simulation/verify-fare-engine.sh` ran
against the live local stack: **8 passed, 0 failed, 1 skipped** (quote immutability — no booking on
the seeded stack). Both `pricing.fare_quote` CHECK constraints fired: the database refused a quote
whose commission does not split the fare. Evidence: `qa/reports/20260802-015420-comigo-slices-01-04-smoke/verify-fare-engine.log`.

Two things the first run found. **`V029` could never have run at all** — it creates
`platform.policy_setting` but nothing had ever created the `platform` schema, so Flyway stopped at
`3F000`; fixed in place. And `POST /pricing/estimate`, which this slice removed, was answering **500
rather than 404**, because `NoResourceFoundException` had no handler in `GlobalExceptionHandler` and
fell through to the catch-all — a defect affecting every unmapped path in the API, not just this one.

The 11.4 km → 570 fixture cannot be reproduced here: the seeded Fort → Nugegoda corridor is ~9.5 km,
so the requested fraction clamps to 1.0. That fixture is asserted exactly by `FareEngineTest`; the
smoke now asserts the rule `gross = onRouteKm x rate` against real stored geometry and a real
assessed band, which is what a runtime check adds over the unit test.

## Evidence to collect

- `scripts/simulation/verify-fare-engine.sh` output showing the fixture comparison.
- Property-test report for the invariants.
- Screenshot or extract of the policy-setting table as seeded.

## Pass/fail criteria

Pass when: every figure in `data.jsx` reproduces exactly; both invariants hold as constraints and
properties; no policy value is inlined in Java; and no pricing input is accepted from a client.

Fail on: any figure disagreeing by even one rupee, any constraint enforced only in Java, or any
passenger-facing payload exposing driver net.
