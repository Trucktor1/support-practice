#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
before="$(docker compose exec -T db psql -U support -d support -Atc 'SELECT count(*) FROM tickets')"
docker compose down
docker compose up -d --no-build --wait --wait-timeout 180
after="$(docker compose exec -T db psql -U support -d support -Atc 'SELECT count(*) FROM tickets')"
test "$before" = "$after"
backup="$(bash scripts/backup.sh)"
bash scripts/restore-test.sh "$backup"
echo "PASS persistence"
