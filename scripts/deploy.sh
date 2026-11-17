#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${1:?Provide image tag}"
export COMPOSE_PROJECT_NAME=support-practice-training
previous="$(cat .current-image 2>/dev/null || true)"
export APP_IMAGE="$1"
docker compose pull app
if ! { docker compose up -d --no-build --wait --wait-timeout 180 && python3 scripts/smoke.py; }; then
  if [ -n "$previous" ]; then
    export APP_IMAGE="$previous"
    docker compose up -d --no-build --wait --wait-timeout 180
    python3 scripts/smoke.py
  fi
  exit 1
fi
printf '%s\n' "$previous" > .rollback-image
printf '%s\n' "$1" > .current-image
