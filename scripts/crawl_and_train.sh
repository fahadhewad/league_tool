#!/usr/bin/env bash
# Crawl a bounded Match-V5 corpus, then retrain the ML model on it.
#
# Prereqs: backend running (with ADMIN_API_TOKEN set and a valid RIOT_API_KEY) + Postgres up.
# Loads ../.env if present. Configure via env or flags:
#
#   BACKEND_URL      backend base URL                (default http://localhost:8080)
#   ADMIN_API_TOKEN  admin token for /api/v1/admin/* (required)
#   PLATFORM         riot platform                   (default euw1)
#   SEED_RIOT_ID     "GameName#TAG" to seed the crawl from (resolved to a PUUID), or
#   SEED_PUUID       a PUUID to seed from directly
#   MAX_MATCHES      crawl bound                      (default 200)
#
# Example:
#   ADMIN_API_TOKEN=secret SEED_RIOT_ID='Agurin#EUW' MAX_MATCHES=300 scripts/crawl_and_train.sh
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] && { set -a; . ./.env; set +a; }

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
PLATFORM="${PLATFORM:-euw1}"
MAX_MATCHES="${MAX_MATCHES:-200}"

: "${ADMIN_API_TOKEN:?set ADMIN_API_TOKEN (must match the backend's admin.api-token)}"

# Resolve a seed PUUID from a Riot ID if one wasn't given directly.
if [ -z "${SEED_PUUID:-}" ]; then
  : "${SEED_RIOT_ID:?set SEED_PUUID or SEED_RIOT_ID (\"GameName#TAG\")}"
  game_name="${SEED_RIOT_ID%%#*}"
  tag_line="${SEED_RIOT_ID##*#}"
  echo "Resolving $SEED_RIOT_ID -> PUUID via $BACKEND_URL ..."
  profile="$(curl -fsS "$BACKEND_URL/api/v1/profiles/$PLATFORM/$(printf %s "$game_name" | jq -sRr @uri)/$tag_line")"
  SEED_PUUID="$(printf '%s' "$profile" | jq -r '.puuid')"
  [ -n "$SEED_PUUID" ] && [ "$SEED_PUUID" != "null" ] || { echo "Could not resolve PUUID" >&2; exit 1; }
fi

echo "Crawling up to $MAX_MATCHES matches from $SEED_PUUID ($PLATFORM) ..."
result="$(curl -fsS -X POST "$BACKEND_URL/api/v1/admin/crawl" \
  -H "X-Admin-Token: $ADMIN_API_TOKEN" \
  --data-urlencode "platform=$PLATFORM" \
  --data-urlencode "puuid=$SEED_PUUID" \
  --data-urlencode "maxMatches=$MAX_MATCHES")"
echo "Crawl result: $result"

echo "Retraining on the crawled corpus ..."
cd ml-service
PY="python3"; [ -x .venv/bin/python ] && PY=".venv/bin/python"
"$PY" -m training.train --source postgres --min-matches "${MIN_MATCHES:-200}"
echo "Done. Restart the ML service (or hit it) to pick up models/winprob_model.pkl."
