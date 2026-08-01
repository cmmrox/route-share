# QA — ComiGo Unified App Backend

Companion to `docs/development/implementation/tasks/comigo-unified-app-backend/`.

Detailed test cases, manual steps and evidence requirements for the 16 backend slices live here. The
development task files carry only a compact `## QA reference` pointing at these documents.

## Files

| QA file | Task |
| --- | --- |
| [00-repo-reset-and-contract-rewrite-qa.md](00-repo-reset-and-contract-rewrite-qa.md) | Repo reset and contract rewrite |
| [01-auth-unification-and-mode-gates-qa.md](01-auth-unification-and-mode-gates-qa.md) | Auth unification and mode gates |
| [02-vehicle-classes-and-rate-bands-qa.md](02-vehicle-classes-and-rate-bands-qa.md) | Vehicle classes and rate bands |
| [03-fare-engine-rewrite-qa.md](03-fare-engine-rewrite-qa.md) | Fare engine rewrite |
| [04-charge-timing-and-capture-correctness-qa.md](04-charge-timing-and-capture-correctness-qa.md) | Charge timing and capture |
| [05-trip-timers-and-reliability-qa.md](05-trip-timers-and-reliability-qa.md) | Trip timers and reliability |
| [06-penalties-dues-and-compensation-qa.md](06-penalties-dues-and-compensation-qa.md) | Penalties, dues and compensation |
| [07-booking-depth-seats-approval-and-expiry-qa.md](07-booking-depth-seats-approval-and-expiry-qa.md) | Booking depth |
| [08-preferences-verification-and-eligibility-qa.md](08-preferences-verification-and-eligibility-qa.md) | Preferences, verification, eligibility |
| [09-search-and-discovery-v2-qa.md](09-search-and-discovery-v2-qa.md) | Search and discovery v2 |
| [10-chat-notifications-safety-and-support-qa.md](10-chat-notifications-safety-and-support-qa.md) | Chat, notifications, safety, support |
| [11-referral-and-rewards-qa.md](11-referral-and-rewards-qa.md) | Referral and rewards |
| [12-realtime-location-pipeline-qa.md](12-realtime-location-pipeline-qa.md) | Real-time location pipeline |
| [13-live-en-route-booking-qa.md](13-live-en-route-booking-qa.md) | Live en-route booking |
| [14-money-operations-payouts-and-adjustments-qa.md](14-money-operations-payouts-and-adjustments-qa.md) | Money operations |
| [15-ratings-and-reviews-v2-qa.md](15-ratings-and-reviews-v2-qa.md) | Ratings and reviews v2 |

## Standing rules for this feature

**These are backend slices.** Only slice 00 touches a mobile surface, and only by moving the existing app.
Every other slice is verified by automated tests plus a runtime smoke script against the live Docker
stack — not by tapping through an emulator. Where a slice's behaviour eventually appears on a screen, the
device evidence is owned by the mobile feature plan and linked from there.

**The prototype is the oracle for money.** `docs/source-assets/comigo-prototype/data.jsx` contains the
exact figures every money screen displays. Any slice touching fares, penalties, referrals, payouts or
ledgers must reproduce those figures exactly. A test that merely asserts "some number came back" does not
close a money slice.

**Runtime smoke scripts are part of the deliverable**, not an afterthought. Each lives at
`scripts/simulation/verify-*.sh`, runs against Postgres + Redis + Keycloak + API, and is referenced by
its slice. They exist because unit tests cannot prove Flyway applied, a scheduler elected a leader, or a
gateway timeout reconciled.

**Slice 12 is verified by replaying recorded GPS traces, not by writing new ones.** Six fixtures — clear
sky, urban canyon, tunnel gap, loop route, detour, and a single-sample spike — are committed as test
resources and form the permanent regression suite for the filter chain. Any change to filtering re-runs
all six.

**Google spend is a gate, not a metric.** Any slice touching maps, places, routing, ETA or detour must
run `scripts/simulation/verify-cost-controls.sh` before and after and compare Redis `maps:*` key counts.
An unexplained increase fails the slice. The Place Details field mask must stay at Essentials — a single
Pro-tier field triples the per-request price, and a contract test enforces it.

**Timer slices need a controllable clock.** Slice 05 onward depends on the injected `Clock` from
`common/config/ClockConfig`. Never verify a timer by waiting in real time; a test that sleeps is a test
that flakes.

**Evidence stays out of git.** Run artefacts, logs, screenshots and reports go under ignored
`qa/reports/<timestamp>-<slug>/`. Only durable summaries are promoted into `docs/development/TASK_LOG.md`.

## Preconditions common to every slice

```bash
docker compose -f infra/docker-compose/docker-compose.yml up -d
cd apps/api && ./mvnw spring-boot:run
```

- Postgres with PostGIS, Redis and Keycloak healthy.
- Flyway applied to the head of the migration series for the slice under test.
- A seeded demo dataset from `scripts/simulation/seed-demo-route.sh`.
- Provider gates default to off (`CYBERSOURCE_ENABLED=false`, `PUSH_NOTIFICATIONS_ENABLED=false`) unless a
  case explicitly exercises the real provider path.
