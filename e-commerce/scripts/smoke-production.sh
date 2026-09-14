#!/usr/bin/env bash
# Post-deployment smoke checks shared by the production release and rollback jobs.
set -euo pipefail

: "${BACKEND_HEALTH_URL:?BACKEND_HEALTH_URL is required}"
: "${FRONTEND_HEALTH_URL:?FRONTEND_HEALTH_URL (the frontend origin) is required}"

curl --fail --silent --show-error --retry 12 --retry-delay 10 --retry-all-errors "${BACKEND_HEALTH_URL}" >/dev/null
curl --fail --silent --show-error --retry 12 --retry-delay 10 --retry-all-errors "${FRONTEND_HEALTH_URL}" >/dev/null
curl --fail --silent --show-error --retry 12 --retry-delay 10 --retry-all-errors "${FRONTEND_HEALTH_URL%/}/api/actuator/health" >/dev/null

echo "Production backend, frontend, and /api proxy health checks passed."
