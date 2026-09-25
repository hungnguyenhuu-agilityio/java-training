#!/usr/bin/env bash
set -euo pipefail

backend_url="http://localhost:${BACKEND_HOST_PORT:-18080}"
frontend_url="http://localhost:${FRONTEND_HOST_PORT:-14200}"

curl --fail --silent --show-error --retry 12 --retry-delay 2 --retry-all-errors "${backend_url}/actuator/health" >/dev/null
curl --fail --silent --show-error --retry 12 --retry-delay 2 --retry-all-errors "${frontend_url}/health" >/dev/null
curl --fail --silent --show-error --retry 12 --retry-delay 2 --retry-all-errors "${frontend_url}/api/actuator/health" >/dev/null

echo "Backend, frontend, and frontend /api proxy health checks passed."
