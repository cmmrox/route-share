# AGENTS.md

## Repository Expectations

- Use `$routeshare-dev-skill` before changing RouteShareApp code, docs, QA, configuration, scripts, API contracts, migrations, or project-local skills.
- Read `docs/development/DEVELOPMENT_STATUS.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, and `docs/development/BLOCKERS.md` at the start of development tasks.
- Keep `.claude/skills/routeshare-dev-skill/` and `.agents/skills/routeshare-dev-skill/` synchronized when the developer operating skill changes.
- Keep `CLAUDE.md` and `AGENTS.md` aligned when changing durable repository operating guidance.
- For mobile tasks, create or update the task-mapped Maestro YAML, run it on emulator/device, fix failures, and rerun until pass before closing the task.
- Keep generated QA evidence under ignored `qa/reports/`; promote only durable summaries into development tracking docs.
