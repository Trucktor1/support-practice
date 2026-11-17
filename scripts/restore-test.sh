#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${1:?Provide backup path}"
docker compose exec -T db dropdb -U support --if-exists support_restore
docker compose exec -T db createdb -U support support_restore
docker compose exec -T db pg_restore -U support -d support_restore --exit-on-error < "$1"
for table in tickets categories status_history; do
  live="$(docker compose exec -T db psql -U support -d support -Atc "SELECT md5(COALESCE(string_agg(row_to_json(t)::text, '' ORDER BY id),'')) FROM $table t")"
  restored="$(docker compose exec -T db psql -U support -d support_restore -Atc "SELECT md5(COALESCE(string_agg(row_to_json(t)::text, '' ORDER BY id),'')) FROM $table t")"
  test "$live" = "$restored"
done
echo "PASS restore checksums"
