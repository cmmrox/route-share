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
