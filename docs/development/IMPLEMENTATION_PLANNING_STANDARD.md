# RouteShareApp Implementation Planning Standard

## Purpose

This document defines the required planning structure for every future RouteShareApp feature implementation. It exists so a high-level feature plan can be executed across multiple sessions by humans or agents without losing context, quality gates, architecture decisions, or release readiness expectations.

Use this standard before implementing any new backend, mobile, web, infrastructure, or production-hardening feature.

## Core rule

Every high-level feature must have a dedicated implementation-task folder:

```text
docs/development/implementation/tasks/<feature-plan-name>/
```

The feature folder is the source of truth for implementation execution. It must contain a high-level feature index and one task file per production-ready implementation slice.

## Required folder structure

```text
docs/development/implementation/
  README.md
  00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md
  01-FOUNDATION-SETUP.md
  ...
  tasks/
    <feature-plan-name>/
      README.md
      01-task-name.md
      02-task-name.md
      03-task-name.md
      ...
      release-readiness-checklist.md

qa/test-cases/
  <feature-plan-name>/
    README.md
    01-task-name-qa.md
    02-task-name-qa.md
    03-task-name-qa.md
```

Naming rules:

- Use lowercase kebab-case for `<feature-plan-name>`.
- Prefix implementation task files with zero-padded numbers: `01-`, `02-`, `03-`.
- Keep QA test case files under `qa/test-cases/<feature-plan-name>/` with the same task prefix and a `-qa.md` suffix.
- Development task files link to QA files; detailed QA cases do not live in `docs/development/`.
- Keep task ordering deterministic and implementation-friendly.

Example:

```text
docs/development/implementation/tasks/07-passenger-mobile-app/
  README.md
  01-passenger-api-contract-reconciliation-and-typed-client.md
  02-expo-app-scaffold-dev-tooling-release-pipeline.md
  ...
  release-readiness-checklist.md

qa/test-cases/07-passenger-mobile-app/
  README.md
  01-passenger-api-contract-reconciliation-and-typed-client-qa.md
  02-expo-app-scaffold-dev-tooling-release-pipeline-qa.md
```

## README.md requirements

Each feature folder must include `README.md` with:

- Feature name and phase/milestone.
- Goal.
- Architecture summary.
- Tech stack and main dependencies.
- Source material reviewed.
- Backend/API/database/design dependencies.
- Known blockers, assumptions, and product decisions.
- Ordered task sequence with links to task files.
- Release quality rule for every task.
- Recommended execution approach.

## Task file requirements

Each `NN-task-name.md` file must be a complete implementation plan for one production-ready feature slice.

Required sections:

```markdown
# Task NN — Task Title

## Objective
## Scope
## Source material / references
## Architecture and design notes
## API contracts involved
## Database / migration changes
## Configuration / environment changes
## UI / UX requirements
## Implementation steps
## Files expected to change
## QA reference
## Verification commands
## Security, privacy, and observability checks
## Done criteria
## Suggested commit message
```

If a section does not apply, keep the section and explicitly state `Not applicable for this task`.

## Production-ready task rule

A task must be scoped so that once it is completed, that feature area is ready to ship to production.

Do not split a single user-visible feature so that:

- one task creates only partial UI and another task later wires the real API;
- one task adds the happy path and another task later adds required error states;
- one task adds development code and another task later adds required QA/test coverage;
- one task changes the database and another task later adds the required repository/service/API behavior;
- one task creates API endpoints and another task later adds required authorization, validation, idempotency, or audit behavior.

Later tasks may build different feature areas, but must not be required to make an earlier task safe or releasable.

## Required task completeness criteria

Each task must include and complete all relevant items below:

- Functional happy path.
- Loading, empty, error, retry, timeout, offline, and permission-denied states where relevant.
- Authorization and ownership checks.
- Validation and typed request/response handling.
- Database migration and rollback/compatibility notes where relevant.
- API contract updates or reconciliation where relevant.
- Observability: useful logs/metrics/errors without leaking secrets or private data.
- Automated tests: unit, integration, contract, component, and/or E2E as appropriate.
- Manual QA cases for platform/device/native behavior that cannot be fully automated.
- iOS and Android QA for mobile tasks.
- Browser/responsive QA for web tasks.
- Runtime smoke checks for backend/infrastructure tasks.
- Development tracking updates.
- Focused commit ready after verification.

## API and backend planning rule

Before any mobile or web app UI is wired to backend data:

1. Inventory the relevant OpenAPI/Swagger contract under `docs/api/`.
2. Compare the contract with actual backend controllers and runtime DTOs.
3. Document mismatches in an API reconciliation document.
4. Decide whether to update backend, update contract, or add a client adapter.
5. Generate or update typed clients before screens consume the API.
6. Add contract tests and runtime smoke tests for critical paths.

Do not build screens directly against aspirational or stale contracts.

## Database planning rule

Any task with persistence impact must include:

- Migration file names and schemas/tables affected.
- New indexes, constraints, foreign keys, enum values, and uniqueness rules.
- Ownership/authorization relationship changes.
- Data backfill or compatibility strategy if existing data is affected.
- Repository/service changes required by the migration.
- Integration tests or migration smoke tests.

## QA split rule

Detailed QA test cases, manual QA steps, device/platform matrices, and evidence requirements belong under `qa/test-cases/<feature-plan-name>/`, not under `docs/development/`.

Each development task should contain only a compact `## QA reference` section that links to the matching QA file.

QA files should use this structure:

```markdown
# QA — Task NN: Task Title

## Related implementation task
## Scope
## Preconditions
## Automated test coverage
## Test cases
## Manual checks
## Evidence to collect
## Pass/fail criteria
```

Daily execution logs, screenshots, XML reports, and run artifacts must not be committed. Keep them under ignored `qa/reports/` or local-only ignored `qa/runs/`.

## Verification command requirements

Every task must list exact commands to run and expected outcomes.

Examples:

```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile test:e2e:android
```

```bash
cd apps/api
./mvnw spotless:check test
```

If a command cannot run in the current environment, record the blocker in `docs/development/BLOCKERS.md` and provide the closest valid evidence instead.

## Development tracking updates

After completing a task, update the relevant tracking docs:

- `docs/development/DEVELOPMENT_STATUS.md`
- `docs/development/IMPLEMENTATION_ROADMAP.md`
- `docs/development/TASK_LOG.md`
- `docs/development/BLOCKERS.md` if blockers changed
- `docs/development/DECISION_LOG.md` if architecture/product decisions changed
- `docs/development/REQUIREMENTS_CHANGE_LOG.md` if requirements changed

Do not leave roadmap/status docs saying work is pending after the implementation and verification are complete.

## Commit rule

Each completed task should be committed separately with a focused message.

Example:

```bash
git add <changed files>
git commit -m "feat(passenger-mobile): complete app shell navigation"
```

Planning-only changes should use:

```bash
git commit -m "docs: add implementation tasks for <feature>"
```

## Review checklist before starting implementation

- [ ] Feature folder exists under `docs/development/implementation/tasks/<feature-plan-name>/`.
- [ ] Matching QA folder exists under `qa/test-cases/<feature-plan-name>/`.
- [ ] `README.md` describes the feature plan and task sequence.
- [ ] Every task file is independently production-release-ready.
- [ ] API, database, configuration, and architecture impacts are documented.
- [ ] QA cases and verification commands are explicit.
- [ ] Task scope does not defer required production behavior to a later task.
- [ ] Existing status/roadmap docs reference the feature task folder.
