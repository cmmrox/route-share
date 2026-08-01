# RouteShareApp Decision Log

## Purpose

This file records important architecture, product, technical, and process decisions. It should explain what was decided, why, and what alternatives were rejected.

Decision Status Values:

- `PROPOSED`
- `ACCEPTED`
- `SUPERSEDED`
- `REJECTED`

---

## Decision 001 — Use Keycloak for identity and authentication

Date: 2026-05-31
Status: `ACCEPTED`

Decision:

- Use Keycloak for RouteShareApp authentication, user sessions, roles, tokens, and user management.
- Use one Keycloak user per real person.
- A user may have passenger and driver roles/profiles.

Reason:

- Avoid duplicate passenger/driver accounts for the same person.
- Centralize auth/session/role management.
- Let backend focus on business state.

Backend Owns:

- Passenger profile.
- Driver profile.
- KYC status.
- Vehicle records.
- Bookings/trips/payments/settlement.
- Local business suspension/status.

---

## Decision 002 — Use one PostgreSQL/PostGIS database with multiple schemas

Date: 2026-05-31
Status: `ACCEPTED`

Decision:

- Use one PostgreSQL/PostGIS database named `routeshare` for MVP and early production.
- Use schemas by module, such as `identity`, `passenger`, `driver`, `routing`, `booking`, `trip`, `payment`, and `settlement`.

Reason:

- Strong consistency is required for booking, seat reservation, trip lifecycle, fare, payment, and settlement.
- Multiple databases from day one would add unnecessary complexity.

Rejected Alternative:

- Multiple independent databases from the beginning.

Reason Rejected:

- Too complex for MVP.
- Harder transaction boundaries.
- Slower implementation.

---

## Decision 003 — Use Spring Boot modular monolith backend

Date: 2026-05-31
Status: `ACCEPTED`

Decision:

- Build backend as a Spring Boot modular monolith.
- Use clean module/package boundaries.
- Keep future service extraction possible.

Reason:

- Faster and safer for MVP.
- Simpler local development and deployment.
- Easier transactional consistency.

---

## Decision 004 — Use Redis only for live/latest temporary location state

Date: 2026-05-31
Status: `ACCEPTED`

Decision:

- Store latest active trip/driver location in Redis with TTL.
- Persist only selected/auditable samples to PostgreSQL.

Reason:

- Avoid high-frequency GPS writes directly to PostgreSQL.
- Keep live UI responsive.
- Preserve important samples for audit and fare validation.

---

## Decision 005 — Maintain repository-based development tracking files

Date: 2026-05-31
Status: `ACCEPTED`

Decision:

- Keep development progress, roadmap, task logs, blockers, decisions, requirements changes, and quality standards inside `docs/development/`.

Reason:

- Development will continue across multiple sessions.
- Project progress must not depend only on chat history.
- Future sessions can resume by reading the repository status files.
---

## Decision 006 — Use service/impl plus facade module structure

Date: 2026-06-01
Status: `ACCEPTED`

Decision:

- Use a familiar Spring Boot package structure: `controller`, `dto`, `mapper`, `service`, `service/impl`, `facade`, `facade/impl`, `domain`, `entity`, `repository`, `event`, and `config`.
- Do not use `port/in` and `port/out` package names for this project.
- Use facades as the public cross-module API so modules remain extractable into microservices later.
- Use MapStruct for mapper classes with a shared mapper config.
- Enforce boundaries with architecture tests.

Reason:

- The team is more familiar with Spring service interfaces and service implementations.
- The code remains learner-friendly and easy to navigate.
- Facades preserve clean module boundaries without introducing unfamiliar architecture terminology.
- Future extraction to microservices remains practical because callers depend on module public APIs instead of repository/entity internals.

Rejected Alternative:

- Hexagonal `application/port/in` and `application/port/out` package naming.

Reason Rejected:

- Correct but less familiar for the current team.
- Slower onboarding and higher cognitive overhead for the MVP.


---

## Decision 007 — Enable Java 21 virtual threads for backend request handling

Date: 2026-06-01
Status: `ACCEPTED`

Decision:

- Enable Spring Boot virtual threads with `spring.threads.virtual.enabled=true`.
- Keep using Spring MVC and Spring Data JPA instead of switching to reactive programming for this backend.
- Bound database concurrency through HikariCP settings in `application.yml`.

Reason:

- RouteShareApp has many I/O-heavy request paths: PostgreSQL/PostGIS queries, authentication, payment, notification, document, and future external integration flows.
- Virtual threads improve scalability for blocking I/O while preserving the learner-friendly Spring Boot programming model.
- Java 21 and Spring Boot 3.3 support this directly.

Operational Note:

- Virtual threads are lightweight, but database connections are still limited. Tune `ROUTESHARE_DB_POOL_MAX_SIZE` based on measured PostgreSQL capacity and production load tests.

---

## Decision 008 — Require task-mapped Maestro automation for mobile implementation tasks

Date: 2026-06-16
Status: `ACCEPTED`

Decision:

- Every mobile implementation task must name its required Maestro YAML path in both the development task file and matching QA test-case file.
- If the task changes a runnable mobile screen, navigation path, native permission, provider-backed mobile flow, or release-pipeline behavior, the Maestro flow must be created or updated during that same task.
- A mobile task cannot be marked complete until the relevant Maestro flow runs on emulator/device, failures are fixed, and the flow is rerun until it passes, unless a concrete external blocker is recorded.

Reason:

- Passenger mobile work must be verified as a real app flow, not only as unit tests or static screenshots.
- Each production-ready task slice needs repeatable QA evidence that can be rerun after fixes.
- Keeping the YAML path in both implementation and QA docs prevents automation from becoming detached from the task definition.

Operational Note:

- Generated Maestro evidence stays under ignored `qa/reports/<timestamp>/`.
- Concise pass/blocker summaries belong in `DEVELOPMENT_STATUS.md`, `TASK_LOG.md`, or `BLOCKERS.md`.

---

## Decision 009 — Maintain Claude Code and Codex project-local operating guidance

Date: 2026-06-16
Status: `ACCEPTED`

Decision:

- Keep the RouteShare developer operating skill in both project-local skill locations:
  - `.claude/skills/routeshare-dev-skill/`
  - `.agents/skills/routeshare-dev-skill/`
- Keep root persistent guidance in both tool-specific files:
  - `CLAUDE.md`
  - `AGENTS.md`
- Future updates to the developer operating skill must update both mirrors and validate both folders. Future durable operating-guidance updates must keep `CLAUDE.md` and `AGENTS.md` aligned.

Reason:

- The same RouteShare development rules should apply whether work is performed through Claude Code or Codex.
- Keeping the skill in the repository makes the operating rules portable with the project instead of depending only on user-global skill folders.
- Root instruction files help each tool load the project rules before deciding which skill to use.

Operational Note:

- Claude Code uses root `CLAUDE.md` as project guidance; Codex reads root `AGENTS.md` as project instructions. Keep both concise and route development work to `routeshare-dev-skill`.
- If the two skill mirrors drift, copy the intended source copy over the stale mirror, then rerun skill validation for both.

---

## Decision 010 — Google Maps cost-control architecture (session tokens, Essentials masks, DB-served geometry, Redis caches, rate limits)

Date: 2026-07-21
Status: `ACCEPTED`

Decision:

- All Google Maps Platform usage stays proxied through the backend and is cost-controlled in one place:
  1. Places autocomplete/details carry a client-generated session token so Google bills a search interaction as one session.
  2. Place Details requests only Essentials-tier fields (`id,formattedAddress,location`); `displayName` (Pro tier, ~3× price) is never requested — the client keeps the suggestion label.
  3. Matched-ride map polylines are served from the stored PostGIS `route_line` (`ST_LineSubstring` between matched fractions) via `GET /api/v1/passenger/route-occurrences/{id}/geometry`; the Directions API is fallback-only for unmatched pairs.
  4. Provider responses are cached in Redis (place details 24 h by placeId; Distance Matrix 7 d by ~110 m-rounded coordinates; Directions 7 d by ~11 m-rounded coordinates) — within Google's 30-day caching terms.
  5. Google-billed proxy endpoints are per-user rate limited (autocomplete 40/min, details 20/min, directions 20/min) via the existing Redis limiter.
  6. Google adapters use a small cooldown breaker (3 consecutive failures → 30 s skip) and degrade to haversine/straight-line/stored-geometry fallbacks.
- The identity token projection (`IdentityFacade.upsertFromToken`) is cached in-process (Caffeine, 5 min, claims-aware, invalidated on admin suspend/activate) so authenticated reads no longer write `identity.app_user` per request.

Why:

- Google API spend was the dominant variable cost; matching was already Google-free (PostGIS), so the remaining spend was Places/details/directions/distance-matrix. Session tokens + Essentials masks + caches + DB geometry remove most billable calls without any UX change, mirroring the hybrid pattern used by regional ride-hailing apps (self-served routing data + Google kept only for POI search).
- The per-request identity upsert was the main self-inflicted write amplification ahead of driver-app GPS ingestion (Phase 08).

Alternatives Rejected:

- Self-hosted OSRM/Valhalla now — deferred (documented as the next lever if Places/Routes spend grows; port-based adapters make it a drop-in later).
- Keeping Google Directions for ride-detail maps — rejected: the stored driver route is both free and more truthful.
- Immediate migration to the Routes API (`computeRoutes`) — deferred to its own slice; legacy Directions/Distance Matrix continue to work for existing customers but are Legacy-status, so the migration is tracked as follow-up work.

Operational Note:

- Local QA helpers live in `scripts/simulation/` (`seed-demo-route.sh`, `verify-cost-controls.sh`); the latter proves each control against the live stack and must stay green before provider-related releases.
- New tuning env vars are documented in `.env.example` (cache TTLs, breaker, projection-cache TTL, per-endpoint limits).

---

## Decision 011 — Ship one mobile application instead of separate passenger and driver apps

Date: 2026-07-31
Status: `ACCEPTED`

Decision:

- Replace the planned two-app split with a single ComiGo mobile application containing both experiences.
- `apps/passenger-mobile` is harvested into a new `apps/mobile`; the empty `apps/driver-mobile` stub is deleted.
- Collapse `passenger-app.openapi.json` and `driver-app.openapi.json` into one `mobile-app.openapi.json`; `admin-web.openapi.json` is unaffected.
- Keep the `/api/v1/passenger/**` and `/api/v1/driver/**` path namespaces — they are role-scoped resource namespaces, not app boundaries.
- Mode becomes a client concept. The server authorizes on role, resource ownership and gate state.

Reason:

- Team decision following review of the `ComiGo Prototype (Standalone).html` specification, in which one account switches between riding and driving without signing out ("Same account, no second app").
- The prototype's mode-switch contract requires an in-progress trip to survive a switch, which is impractical across two installed applications.

Alternatives rejected:

- Two apps sharing a backend — contradicts the prototype's core mode-switch behaviour.
- Renaming the path namespaces to a single `/api/v1/me/**` — churn across 70+ endpoints, and loses the authorization signal the path carries.

Consequence:

- `PhoneOtpAccessTokenAuthenticationFilter`'s hardcoded `ROLE_PASSENGER` becomes a blocker and is fixed in slice 01.
- Plan: `docs/development/implementation/tasks/comigo-unified-app-backend/`.

---

## Decision 012 — The ComiGo prototype POLICY block is the commercial spec of record

Date: 2026-07-31
Status: `ACCEPTED`

Decision:

- Every commercial rule and figure in `docs/source-assets/comigo-prototype/data.jsx` (`POLICY`, `FARE_POLICY`) is authoritative: 10% commission inside the fare, route-match discount tiers 10/8/5/2.5%, 20%/25% penalties split 50/50, 12-hour driver cancellation window, 10-minute start buffer, 5-minute pickup wait, 10-minute driver-late grace, 2 fare-adjusted early drop-offs per calendar month, 3 missed starts per month, weekly Friday payouts with a LKR 1000 floor, 1%/2% referral over 12 months or 50 trips.
- Every one of those values is stored in a new `platform.policy_setting` table and read through a typed accessor. No policy figure may be inlined as a Java constant; an architecture test enforces this.

Reason:

- The prototype states each rule once and every screen reads it, so the figures are internally consistent and directly renderable.
- Business needs to tune percentages, windows and floors without a deploy.

Consequence:

- The existing `FareCalculator` (base 250 + 90/km + 5/min with a 10% fee added on top) is deleted; `finance.fare_policy` loses `base_fare`, `per_km` and `per_min`.
- Fare quotes are persisted per booking so a receipt always reproduces the fare that was actually charged.

---

## Decision 013 — Vehicle rate bands are admin-typed, not computed

Date: 2026-07-31
Status: `ACCEPTED`

Decision:

- An admin types the min and max per-km rate for each vehicle, constrained to the vehicle class band.
- The four assessment factors (wear/tyres, insurance, fuel, service) are stored as displayed justification with signed deltas, not as inputs to a scoring engine.
- The driver chooses any rate inside the band; a vehicle with no active band cannot publish.

Reason:

- Ships the D39/D40 product surface without agreeing a scoring rubric first.
- The contract is identical either way, so a scoring engine can replace the manual step later without a client change.

Alternatives rejected:

- Backend-computed bands from vehicle attributes — larger slice, and requires agreed per-factor scoring that does not exist yet.
- Flat per-class bands with no per-vehicle assessment — removes D39's explanation of why a driver's ceiling is what it is, and D40 entirely.

---

## Decision 014 — Calls are direct dial; number masking is cut

Date: 2026-07-31
Status: `ACCEPTED`

Decision:

- Passenger and driver call each other directly from the device. No ComiGo relay and no telephony provider.
- The prototype's "Hide my number" toggles (D35, S28) are removed from the product.
- The backend instead owns a counterparty phone disclosure rule: numbers are released only on a `CONFIRMED` booking, reciprocally, revoked 24 hours after drop-off and immediately on any terminal state, with every read audited.

Reason:

- Team decision. No telephony provider with Sri Lankan numbers is configured, and none of the other subsystems depends on one.

Accepted risk:

- Two strangers keep each other's personal mobile numbers permanently after one shared trip, and ComiGo cannot recall them. The relay existed to prevent exactly that.
- Mitigation: the disclosure rules above, plus alerting on per-user disclosure volume to detect number harvesting. The rule is implemented as a single service method so a relay could be introduced later without re-cutting every Call button.

---

## Decision 015 — Pre-launch migrations may change column meaning in place

Date: 2026-07-31
Status: `ACCEPTED`

Decision:

- The application is not released. There are no real users, bookings or payments in any environment.
- Migrations `V027`–`V040` may therefore change the meaning of existing columns directly, without expand/migrate/contract pairs, backfill scripts or dual-read periods.
- This applies specifically to `V029` (fare model cutover) and `V035` (search radius semantics, from 1 km pickup-proximity to 5/10/20 km trip-start distance).

Reason:

- Confirmed by the product owner. Dev and QA databases are recreated from the full migration series and reseeded by `scripts/simulation/seed-demo-route.sh`.

Consequence and expiry:

- **This freedom ends at launch.** The launch date must be recorded here, and from that point every migration is forward-only and additive. A post-launch migration written in the pre-launch style would destroy real financial records, which are retained for 7 years.

---

## Decision 016 — Route-constrained map matching for live en-route booking

Date: 2026-08-01
Status: `ACCEPTED`
Architecture: `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md`

Context:

Live en-route booking was confirmed in scope. It rests entirely on one server-side question — is this
driver still behind that rider's pickup point? — and raw phone GPS cannot answer it. Urban GPS error
routinely exceeds 50 m, more than a Colombo city block, which is enough to place a driver on the wrong
side of a junction.

Research reviewed: Uber's H3 spatial index and its published dispatch/location architecture (adaptive
4–6 s in-trip sampling, WebSocket streaming, HMM map matching in CatchME, 3D shadow matching, DISCO offer
rotation with an ~8 s timeout, Ringpop consistent hashing); Google Roads API snap-to-roads limits and
pricing; open-source map matching (Valhalla Meili, OSRM, GraphHopper); Android FCM message priority and
Doze behaviour; Android location/battery guidance; and what is publicly documented about PickMe's
event-driven platform and its use of Google Directions/Places.

Decision:

- Use **route-constrained map matching**, not road-network map matching. A ComiGo driver publishes his
  route before departure and it is already stored as a PostGIS `LineString`, so
  `ST_LineLocatePoint(route_line, point)` yields the route fraction directly.
- Apply four filters in order: accuracy gate (50 m), speed gate (40 m/s), route-corridor projection
  (80 m), and a **monotonic progress clamp** that also disambiguates self-intersecting routes.
- Extrapolate between pings by dead reckoning (20 s cap) and expose an explicit confidence level —
  `MATCHED`, `EXTRAPOLATED`, `STALE`, `OFF_ROUTE`. `STALE` and `OFF_ROUTE` **fail closed**: a position
  that cannot be proven to be behind a pickup is never offered.
- Index running trips by **H3** cell (res 7 coarse, res 9 fine) so joinable search is an integer index
  lookup rather than a scan. `h3-pg` in Postgres, `uber/h3-java` on the JVM.
- Deliver the 45-second driver offer over **WebSocket when the app is foregrounded and FCM high-priority
  otherwise**, because Android Doze defers WebSocket traffic while high-priority FCM wakes the radio.
- Adopt Uber's published sampling cadence: 4 s in trip, 3 s near a pickup, 10 s under 15% battery, 30 s
  published-but-not-started, nothing when idle; batched 3–5 samples per request.
- Build this as its own slice (12) so live booking (13) is about the booking, not about geometry.

Reason:

Uber must search the whole road network because an Uber driver has no published route. ComiGo's drivers
do, which collapses the hard problem into projection onto a single known line — cheaper, and *more*
accurate for this specific question because the search space is one line instead of thousands of
candidate segments.

Alternatives rejected:

- **Google Roads API snap-to-roads** — 100-point cap per request, per-request cost on every ping, and it
  would reverse the July 2026 Google cost-optimisation work. At 500 concurrent trips, snapping every ping
  would be roughly 450,000 Google calls per hour; the chosen design adds zero.
- **Self-hosted Valhalla/OSRM/GraphHopper map matching** — deferred, not rejected outright. Only needed
  when a driver leaves his published corridor, which the design handles by failing closed. Build it if
  production evidence shows sustained off-route driving is common.
- **3D shadow matching** — requires a 3D model of Colombo and 20–100 ms per fix. Enormous effort for a
  problem route projection already solves.
- **Ringpop / consistent hashing across nodes** — built for Uber's scale. One Postgres with correct
  indexes carries Colombo. Revisit at a documented throughput threshold, not on principle.
- **System-dispatched offer rotation (DISCO-style, 8 s, next candidate)** — ComiGo's product is
  rider-initiated: she chooses which driver to ask. There is no candidate queue to rotate through, which
  is also why the window is 45 s rather than 8 s: no fallback driver follows.

Consequence:

- New slice 12 inserted; former slices 12/13/14 renumbered to 13/14/15. Migration series extends to `V041`.
- `h3-pg` becomes a deployment requirement on the Postgres image, checked at boot.
- `location.location_sample` becomes the highest-volume table in the system and needs partitioning and a
  retention policy.
- Recorded GPS trace fixtures (urban canyon, tunnel gap, loop, detour, spike) become the permanent
  regression suite for the filter chain.

---

## Decision 017 — Right-size the location architecture to actual target scale

Date: 2026-08-01
Status: `ACCEPTED`
Supersedes parts of: Decision 016 (H3 indexing)
Architecture: `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md`

Context:

The product owner supplied concrete targets: **500 trips per day, ~200 concurrent, 300 as the design
ceiling**, with accuracy as the stated priority because "passenger cannot find driver and driver cannot
find passenger" is the failure that matters.

The arithmetic settles most of the design:

| Metric | Value |
| --- | --- |
| Samples/sec at 4 s cadence | 300 ÷ 4 = **75/sec** |
| HTTP requests/sec, batched 4 | **~19/sec** |
| Rows scanned per joinable query | **≤ 300** |
| Samples/day | ~225,000 |
| Storage | ~22 MB/day, ~8 GB/year |

Decision:

1. **Drop H3.** At ≤300 live rows a GiST index on `location.trip_progress.last_position` with
   `ST_DWithin` is faster than an H3 cell lookup — no cell arithmetic, no `kRing` expansion, no join — and
   requires no Postgres extension and no JVM library. Revisit threshold: **sustained concurrency above
   5,000, or joinable-query p95 above 50 ms.** Both are instrumented and alerted.
2. **Confirm Kafka and sharding declined**, with thresholds: >10,000 events/sec sustained, or a single
   Postgres saturating.
3. **Separate the two problems.** *Matching* (is he behind her pickup) tolerates ~50 m error and is solved
   by geometry. *Rendezvous* (can they find each other) is fatal at 50 m and is **not** a filtering problem
   — the error is in the map pin.
4. **Named pickup points** (slice 09) solve the rendezvous problem. Three tiers: curated (admin), derived
   (Google Places at booking time), learned (promoted from successful pickups). Ship tier 2 with the schema
   ready for tier 1. Places is called at booking, never per ping.
5. **Approach mode** (slice 12): within 500 m of a pickup, raise sampling to 1–2 s on both devices and open
   a two-way position window. The rider's position is stored only in an open approach session and
   **deleted when it closes** — the narrowest window that makes the rendezvous work.
6. **Detour cap** (slice 13): UberX Share limits added detour to 8 minutes. Adopt
   `LIVE_MAX_ADDED_MINUTES` (8) and `LIVE_MAX_ADDED_KM` (3), filtering candidates **before** the driver is
   prompted — the same principle as the behind-pickup rule.
7. **Reject bad fixes on the device**, before transmission. A fix the phone knows is poor should not
   consume a request, a row, or server CPU.

Reason:

Uber's H3 exists to avoid scanning millions of rows; ComiGo scans at most 300. Adopting it would have been
cargo-culting — a Postgres extension, a JVM dependency and a body of tests bought nothing measurable, and
the extension would have constrained the choice of managed Postgres. The complexity budget is better spent
on accuracy, and specifically on the rendezvous problem, which no amount of GPS filtering addresses.

Approach mode is the clearest example of scale working in ComiGo's favour: a 1–2 second cadence in the
final 500 m costs ~25 req/sec at this size, and lands precision exactly on the ninety seconds that decide
whether the pickup succeeds. Uber cannot afford that at millions of concurrent trips.

Consequence:

- Slice 12 loses `h3-pg` and `h3-java` and gains approach mode; it gets simpler **and** more accurate.
- **No new Postgres extension is required** — managed Postgres is unconstrained.
- Slice 09 gains named pickup points and `routing.pickup_point`.
- Slice 13 gains the detour cap.
- Load must be **proven at 300 concurrent trips**, not assumed; it is a done-criterion and a release gate.

---

## Decision 018 — Google API cost is a design constraint, not a post-hoc optimisation

Date: 2026-08-01
Status: `ACCEPTED`
Extends: Decision 010 (Google Maps cost-control architecture), 017 (right-sizing)
Cost model: `docs/architecture/REALTIME-LOCATION-AND-LIVE-MATCHING.md` §5.5–5.7

Context:

The product owner required that Google spend be minimised **without sacrificing performance or accuracy**.
An audit of the ComiGo backend plan against current Google Maps Platform pricing found two paths that
would have quietly become the largest line items, plus one new feature needing containment.

Governing rule:

> **Never call Google for something the database already knows, and never call it per-ping.**

Decisions:

1. **ETA is derived, not bought.** `remainingRouteMeters ÷ smoothedObservedSpeed`, from
   `ST_LineSubstring` on the stored route line and `trip_progress.speed_mps`. Free, and **more accurate**
   than a Google estimate because it reflects the traffic that specific driver is in right now rather than
   a generic model of that road.
2. **Live-request detour minutes are derived, not bought.** `addedMeters` is pure geometry on the stored
   line; `addedMinutes = addedMeters ÷ observedSpeed`. The naive implementation — a Directions call per
   candidate — would have been thousands of calls per hour at 300 concurrent trips.
3. **Pickup-point resolution is cost-ordered**: curated → persisted derived → existing route
   origin/destination label → Places (Essentials mask, cached) → raw coordinate with a generated label.
   Resolved naively this was ~30,000 Place Details calls/month (~$150) and would alone have broken the
   $200 monthly credit. Plus a one-time seed of ~200 Colombo landmarks.
4. **Place Details stays on the Essentials field mask.** A single Pro-tier field upgrades the *entire*
   request from $5 to $17 per 1,000 — the trap the July 2026 work avoided by dropping `displayName`.
   A contract test now fails the build if a Pro field is added.
5. **The location pipeline adds zero Google calls**, asserted by comparing Redis `maps:*` key counts
   before and after a full trip simulation. Snapping every ping would be ~450,000 calls/hour at 500
   concurrent trips.

Estimated steady state at 500 trips/day: **~$132/month, inside the $200 credit** — Place Details sessions
~$112, Distance Matrix misses ~$12, Directions on new route plans ~$5, pickup points ~$3, location
pipeline $0.

Reason:

Every one of these choices is *also* the more accurate choice. Derived ETA and detour beat Google's
generic road estimates because they use live observed speed. Route projection beats road-network map
matching because the search space is one known line. Cost and accuracy pointed the same way, so no
trade-off was required — which is why this is a design constraint rather than an optimisation pass.

Consequence:

- Cost gates added to the release-readiness checklist as hard requirements, not advisories.
- `scripts/simulation/verify-cost-controls.sh` extended to cover the location pipeline, ETA, detour and
  pickup-point paths.
- A billing dashboard with per-SKU breakdown and a pre-credit-exhaustion alert is a release requirement.
- Pricing must be re-verified against the billing console before launch; SKU rates and tiers change.
