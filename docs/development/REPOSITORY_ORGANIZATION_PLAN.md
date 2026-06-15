# RouteShareApp Repository Organization Plan

## Goal

Keep the monorepo easy to understand, safe to commit, and predictable for future backend, mobile, web, infrastructure, QA, and documentation work.

## Canonical organization

```text
docs/development/
  DEVELOPMENT_STATUS.md
  IMPLEMENTATION_ROADMAP.md
  IMPLEMENTATION_PLANNING_STANDARD.md
  QUALITY_STANDARDS.md
  BLOCKERS.md
  DECISION_LOG.md
  REQUIREMENTS_CHANGE_LOG.md
  TASK_LOG.md
  REPOSITORY_ORGANIZATION_PLAN.md
  implementation/
    README.md
    00-PROJECT-ARCHITECTURE-AND-FILE-STRUCTURE.md
    01-FOUNDATION-SETUP.md
    ...
    tasks/
      <feature-plan-name>/
        README.md
        NN-task-name.md

qa/
  README.md
  test-cases/
    <feature-plan-name>/
      README.md
      NN-task-name-qa.md
  maestro/
    *.yaml
  reports/    # ignored generated evidence
  runs/       # ignored local daily run notes
```

## Rules

1. `docs/development/` contains development plans, status, roadmap, decisions, blockers, requirements, and implementation task plans only.
2. High-level implementation plans live in `docs/development/implementation/`.
3. Per-feature implementation tasks live in `docs/development/implementation/tasks/<feature-plan-name>/`.
4. QA plans and task-level test cases live in `qa/test-cases/<feature-plan-name>/`.
5. Development task files link to QA files; detailed QA cases do not live inside `docs/development/`.
6. Repeatable QA automation flows are committed under `qa/maestro/`.
7. Daily QA run logs, screenshots, generated reports, XML, PDFs, and temporary summaries are not committed. Keep them under ignored `qa/reports/`, `qa/runs/`, or `artifacts/`.
8. Important completion status from QA is summarized into `docs/development/DEVELOPMENT_STATUS.md`, `docs/development/TASK_LOG.md`, or `docs/development/BLOCKERS.md` so other developers see the current application state without noisy run logs.
9. Session-summary folders are not used. Durable information belongs in task logs, status, roadmap, decisions, blockers, or requirements docs.
10. Never commit secrets or local state: `.env`, `.env.*` except `.env.example`, `.DS_Store`, `.hermes/runtime`, `.docker`, `node_modules`, build outputs, Expo `.expo`, native generated `android/ios`, and generated QA/artifact output.

## Commit hygiene checklist

Before committing:

```bash
git status --short --ignored
git add -n .
```

Confirm only source, safe config examples, repeatable scripts/flows, QA test cases, and durable docs are staged.
