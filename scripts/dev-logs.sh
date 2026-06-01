#!/usr/bin/env bash
set -euo pipefail
export PATH="/usr/local/bin:/opt/homebrew/bin:/opt/homebrew/sbin:$PATH"
cd "$(dirname "$0")/.."
mkdir -p .docker
export DOCKER_CONFIG="$PWD/.docker"
docker-compose --env-file .env.example -f infra/docker-compose/docker-compose.yml logs -f "$@"
