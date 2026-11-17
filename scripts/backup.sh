#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p backups
target="backups/support-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker compose exec -T db pg_dump -U support -d support -Fc > "$target"
test -s "$target"
echo "$target"
