# Workflow And Git

## Contents

- Start workflow
- Documentation and implementation planning
- QA workflow
- Branch naming
- Commit messages
- Review and merge rules
- Known workflow gaps

## Start Workflow

Before nontrivial work:

1. Run `git status --short`.
2. Read `docs/development/DEVELOPMENT_STATUS.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, and `docs/development/BLOCKERS.md`.
3. Read the relevant task plan under `docs/development/implementation/tasks/<feature-plan-name>/`.
4. Read the matching QA file under `qa/test-cases/<feature-plan-name>/`.
5. Inspect affected source, tests, API contracts, migrations, config, and scripts.

Respect user changes in the dirty worktree. Do not revert, overwrite, or restage unrelated changes.

## Documentation And Implementation Planning

The project uses living docs as the cross-session memory system.

Core docs:

- `docs/development/DEVELOPMENT_STATUS.md`: first status read; current phase, active task, verification, next task.
- `docs/development/IMPLEMENTATION_ROADMAP.md`: long-range phase/task status.
- `docs/development/TASK_LOG.md`: completed task history.
- `docs/development/BLOCKERS.md`: active/deferred/resolved blockers.
- `docs/development/DECISION_LOG.md`: architecture/product decisions.
- `docs/development/REQUIREMENTS_CHANGE_LOG.md`: requirements changes.
- `docs/development/QUALITY_STANDARDS.md`: code quality and architecture standards.
- `docs/development/REPOSITORY_ORGANIZATION_PLAN.md`: folder policy and commit hygiene.
- `docs/development/IMPLEMENTATION_PLANNING_STANDARD.md`: required task-folder structure.

Implementation task standard:

- High-level plans live in `docs/development/implementation/`.
- Feature execution plans live in `docs/development/implementation/tasks/<feature-plan-name>/`.
- Each feature folder has `README.md`, numbered task files, and often `release-readiness-checklist.md`.
- Each task must be production-slice complete and link to matching QA.
- Do not leave status/roadmap docs stale after implementation or verification.

Update docs after task completion:

- Always update `DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, and `TASK_LOG.md`.
- Update `BLOCKERS.md` when blockers open, close, or change evidence.
- Update `DECISION_LOG.md` for architecture/product decisions.
- Update `REQUIREMENTS_CHANGE_LOG.md` for scope/requirement changes.
- Update `docs/api/*` and `API_BACKEND_RECONCILIATION.md` for API contract/backend changes.

## QA Workflow

QA structure:

- Committed QA specs: `qa/test-cases/<feature-plan-name>/`.
- Committed executable flows: `qa/maestro/<app>/<suite>/*.yaml`.
- Ignored generated evidence: `qa/reports/`, `qa/runs/`, `artifacts/`.

Mobile task automation rule:

- Every mobile implementation task must name its matching Maestro YAML path in both the development task `## QA reference` and the QA test-case file.
- If the task changes a runnable mobile screen, navigation path, native permission, provider-backed mobile flow, or release pipeline behavior, the Maestro flow must be created or updated in the same task.
- The task is not complete until the relevant Maestro flow runs on emulator/device, failures are fixed, and the flow is rerun until it passes or an explicit blocker is recorded.
- Generated screenshots, UI dumps, logs, and JUnit XML stay under ignored `qa/reports/<timestamp>/`; only concise status summaries move into living docs.
- Pure TypeScript/client tasks without a runnable mobile surface may document a temporary exception, but the first task that creates a runnable app surface must add emulator/device Maestro coverage for that path.

Use wrappers when possible:

```bash
scripts/qa-check-tools.sh
scripts/qa-passenger-android.sh
scripts/qa-passenger-dev-run.sh
```

Current passenger mobile executable flows:

```bash
maestro --device emulator-5554 test qa/maestro/passenger-mobile/smoke/auth-profile-smoke.yaml
maestro --device emulator-5554 test qa/maestro/passenger-mobile/smoke/home-search-route-discovery-smoke.yaml
maestro --device emulator-5554 test qa/maestro/passenger-mobile/regression/task07-home-search-route-discovery.yaml
```

For mobile work, lint/typecheck/unit tests are necessary but not enough when native maps, location, notifications, camera/photo, secure storage, deep links, or permissions are touched. Capture Android/iOS or explicitly record environment blockers.

## Branch Naming

Observed repo state only documents `main`; no repo-local branch policy file exists. Use this inferred operating convention unless the project owner gives a different branch name:

```text
^(main|codex/[a-z0-9][a-z0-9-]*(?:/[a-z0-9][a-z0-9-]*)?)$
```

Examples:

- `main`
- `codex/passenger-task-08-results`
- `codex/backend-maps-place-search`
- `codex/docs-operating-skill`

Do not create long-lived release branches or non-`codex/` work branches without explicit owner direction. Gap: add a repo-local `CONTRIBUTING.md` or branch policy if the team wants this enforced beyond Codex sessions.

## Commit Messages

Commit history follows Conventional Commits with optional scopes. Use this format:

```text
<type>(<optional-scope>): <imperative summary>
```

Inferred regex:

```text
^(feat|fix|docs|test|refactor|perf|build|ci|chore|revert)(\([a-z0-9-]+\))?!?: .+
```

Observed examples:

- `feat(booking): enforce idempotency keys`
- `feat(location): complete phase 06 realtime foundation`
- `fix: keep OTP demo behavior backend-only`
- `test: enforce backend coverage gate`
- `docs: organize development and QA structure`

Suggested scopes:

- Backend module scopes: `identity`, `passenger`, `driver`, `vehicle`, `routing`, `booking`, `trip`, `location`, `pricing`, `payment`, `admin`, `maps`.
- App/package scopes: `passenger-mobile`, `api-contracts`, `qa`, `infra`, `docs`.

Task files often include a suggested commit message; prefer that when present.

Before committing:

```bash
git status --short --ignored
git add -n .
```

Stage only source, safe config examples, repeatable scripts/flows, QA test cases, and durable docs. Never stage `.env`, generated QA reports, build outputs, local native generated folders, `.docker/`, `.expo/`, `.hermes/runtime/`, or artifacts.

## Review And Merge Rules

No CI workflow files were found under `.github/` during skill creation. Treat local verification and living-doc evidence as the current quality gate.

Before marking a task complete or preparing a PR:

- Re-read relevant task done criteria and QA pass/fail criteria.
- For mobile tasks, confirm the task-mapped Maestro YAML exists or was updated, ran on emulator/device, and any failures were fixed and rerun.
- Run backend/mobile/contract/QA commands appropriate to changed files.
- Confirm architecture tests still protect backend boundaries when package rules change.
- Confirm no secrets or generated artifacts are staged.
- Summarize verification results in living docs if they change current status.
- Keep commits focused by production-ready task slice.
- For reviews, lead with bugs/regressions/security/test gaps and cite files/lines.

## Known Workflow Gaps

Create or update these docs if the project owner wants stronger process enforcement:

- `CONTRIBUTING.md` with branch, commit, review, and local verification policy.
- CI workflow docs or `.github/workflows/*` when CI is introduced.
- Release/deployment runbook for backend, mobile, admin, secrets, and migrations.
- Production incident/rollback runbook.
- Dependency approval/security review policy.
