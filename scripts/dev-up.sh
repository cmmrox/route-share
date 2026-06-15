#!/usr/bin/env bash
set -euo pipefail
export PATH="/usr/local/bin:/opt/homebrew/bin:/opt/homebrew/sbin:$PATH"
cd "$(dirname "$0")/.."
mkdir -p .docker
export DOCKER_CONFIG="$PWD/.docker"
ENV_FILE=".env.example"
if [[ -f .env ]]; then
  ENV_FILE=".env"
fi
if docker compose version >/dev/null 2>&1; then
  docker compose --env-file "$ENV_FILE" -f infra/docker-compose/docker-compose.yml up -d
else
  docker-compose --env-file "$ENV_FILE" -f infra/docker-compose/docker-compose.yml up -d
fi
