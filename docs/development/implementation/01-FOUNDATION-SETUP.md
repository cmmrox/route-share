# Stage 01 — Foundation Setup Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Create the monorepo foundation, developer tooling, local infrastructure, documentation locations, and quality gates.

**Architecture:** Monorepo with separate deployable apps, one backend modular monolith, shared TypeScript packages, and local Docker Compose dependencies.

**Tech Stack:** pnpm workspace, Expo React Native, Next.js, Spring Boot 3 Java 21, PostgreSQL/PostGIS, Redis, Redpanda, Docker Compose.

---

## Acceptance criteria

- Root workspace exists with `apps/`, `packages/`, `docs/`, `infra/`, `scripts/`, and `qa/`.
- Local Compose starts PostgreSQL/PostGIS, Redis, and Redpanda.
- Root README explains modular monolith decision.
- Basic lint/test scripts exist even before feature code.

## Tasks

### Task 1: Create repository skeleton

**Files:**
- Create directories listed in `00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md`.
- Create: `.editorconfig`, `.gitignore`, `.env.example`, `README.md`.

**Steps:**
1. Create folders.
2. Add root README with project overview and modular-monolith rule.
3. Verify with `find . -maxdepth 3 -type d`.
4. Commit: `docs: establish routeshare monorepo skeleton`.

### Task 2: Configure workspace tooling

**Files:**
- Create: `package.json`
- Create: `pnpm-workspace.yaml`
- Create: `turbo.json` optional, can be minimal.

**Root scripts:**
- `dev:passenger`
- `dev:driver`
- `dev:admin`
- `dev:api`
- `test`
- `lint`
- `typecheck`

**Verification:** `pnpm install` and `pnpm -r list` should recognize workspace packages.

### Task 3: Add local infrastructure

**Files:**
- Create/update: `infra/docker-compose/docker-compose.yml`
- Create/update local PostGIS initialization through `infra/docker-compose/docker-compose.yml` and Flyway migrations under `apps/api/src/main/resources/db/migration/`

**Services:**
- PostgreSQL 16 with PostGIS
- Redis
- Redpanda

**Verification:**
- `./scripts/dev-up.sh`
- Confirm DB accepts `CREATE EXTENSION IF NOT EXISTS postgis; SELECT PostGIS_Version();`

### Task 4: Add environment conventions

**Files:**
- Create/update: `.env.example` with safe placeholders only
- Update: `.env.example`

**Rules:**
- No real secrets.
- Separate app API URL, DB URL, Redis URL, Kafka brokers, map provider key placeholder, payment sandbox placeholder.

### Task 5: Add ADRs

**Files:**
- Create: `docs/architecture/ADR-0001-modular-monolith-first.md`
- Create: `docs/architecture/ADR-0002-postgresql-postgis.md`
- Create: `docs/architecture/ADR-0003-location-event-pipeline.md`
- Create: `docs/architecture/ADR-0004-react-native-expo-dev-build.md`

**Verification:** README links to ADRs.
