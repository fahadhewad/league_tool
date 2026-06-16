#!/usr/bin/env bash
# Run the backend locally. Loads ../.env (git-ignored) so RIOT_API_KEY etc. are available.
set -euo pipefail
cd "$(dirname "$0")"

if [ -f ../.env ]; then
  set -a
  # shellcheck disable=SC1091
  . ../.env
  set +a
fi

if [ -z "${RIOT_API_KEY:-}" ]; then
  echo "WARNING: RIOT_API_KEY is not set. Live Riot calls will fail with 401/403." >&2
fi

exec mvn spring-boot:run
