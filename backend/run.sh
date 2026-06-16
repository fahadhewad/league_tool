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

# Map the .env POSTGRES_* values onto the Spring datasource if not already set explicitly.
# The backend defaults to Postgres (Flyway-managed schema); bring up infra/docker-compose first.
: "${SPRING_DATASOURCE_URL:=jdbc:postgresql://${POSTGRES_HOST:-localhost}:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-leaguetool}}"
: "${SPRING_DATASOURCE_USERNAME:=${POSTGRES_USER:-leaguetool}}"
: "${SPRING_DATASOURCE_PASSWORD:=${POSTGRES_PASSWORD:-leaguetool}}"
export SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD

exec mvn spring-boot:run
