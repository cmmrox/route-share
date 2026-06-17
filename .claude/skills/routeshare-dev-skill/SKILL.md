---
name: routeshare-dev-skill
description: Project-specific developer operating skill for RouteShareApp. Use whenever working in this repository or its app/code/docs/QA/config surfaces, including Spring Boot API work, Expo React Native passenger app work, future driver/admin app work, API contract reconciliation, Flyway/PostGIS migrations, Keycloak/OTP/auth, Google Maps/Places, Notify.lk, Cybersource, Firebase push, Sentry, pnpm workspace/package changes, Maven backend changes, QA/Maestro flows, implementation planning, living-doc updates, branch/commit/PR preparation, code review, test failure fixes, security/privacy-sensitive changes, or any task that touches apps/, packages/, docs/, infra/, qa/, scripts/, .env.example, CLAUDE.md, AGENTS.md, .claude/skills/, or .agents/skills/. When updating this skill, update both project-local mirrors at .claude/skills/routeshare-dev-skill/ and .agents/skills/routeshare-dev-skill/, and keep CLAUDE.md plus AGENTS.md aligned.
---

# RouteShareApp Developer Operating

## Core Operating Rules

- Treat project living docs as the source of truth; inspect them before changing code.
- Preserve the dirty worktree; never revert user changes or commit secrets/local artifacts.
- Keep features production-slice complete: API, validation, ownership/auth, persistence, errors, tests, QA, docs, and config gates move together.
- For mobile tasks, require task-mapped Maestro automation, emulator/device execution, evidence capture, and fix-rerun loops before closing the task.
- Keep the Claude Code and Codex project-local mirrors synchronized: `.claude/skills/routeshare-dev-skill/` and `.agents/skills/routeshare-dev-skill/`; validate both after updates.
- Keep root persistent guidance aligned: `CLAUDE.md` for Claude Code and `AGENTS.md` for Codex.
- Follow backend modular-monolith boundaries and passenger mobile thin-screen/feature-module boundaries.
- Run the narrowest meaningful verification first, then the documented full gate for completed tasks.

## Start Every Task

1. Check `git status --short` and identify user changes before editing.
2. Read `docs/development/DEVELOPMENT_STATUS.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, and `docs/development/BLOCKERS.md`.
3. Pick the relevant reference below and read the linked project docs before touching files.
4. Inspect affected source, tests, API contracts, migrations, scripts, and QA cases.
5. Decide the verification commands before edits; update living docs when status, blockers, roadmap, QA, contracts, or decisions change.

## Reference Map

- Code, architecture, security, dependencies, testing, performance: read `references/architecture-and-code-rules.md`.
- Planning, docs workflow, QA evidence, git branches, commits, review/merge: read `references/workflow-and-git.md`.
- System orientation, app areas, auth/data/integrations/deployment context: read `references/project-context.md`.
- Backend changes: read `docs/architecture/BACKEND-MODULAR-MONOLITH-SERVICE-IMPL-FACADE.md`, `docs/development/QUALITY_STANDARDS.md`, then inspect `apps/api/pom.xml`, `apps/api/src/main/java/com/routeshare/`, `apps/api/src/test/java/com/routeshare/`, and `apps/api/src/main/resources/db/migration/`.
- Passenger mobile changes: read `docs/development/implementation/tasks/07-passenger-mobile-app/README.md`, the matching task file, and matching `qa/test-cases/07-passenger-mobile-app/*-qa.md`; inspect `apps/passenger-mobile/package.json`, `app.config.ts`, `src/application/`, `src/api/`, `src/features/`, `src/screens/`, and `src/design-system/`.
- API/client-contract changes: read `docs/api/README.md`, `docs/api/API_BACKEND_RECONCILIATION.md`, and relevant `docs/api/*.openapi.json`; inspect controllers/DTOs plus `packages/api-contracts/src/index.ts`.
- QA changes: read `qa/README.md` and `qa/maestro/README.md`; mobile task files and QA cases must name the required Maestro YAML path, and generated evidence stays under ignored `qa/reports/`.
- External-provider or secret/config changes: read `docs/development/PRODUCTION_EXTERNAL_SERVICES.md`, `.env.example`, `apps/api/src/main/resources/application.yml`, and `apps/passenger-mobile/app.config.ts`.
