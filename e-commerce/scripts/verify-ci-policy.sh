#!/usr/bin/env bash
set -euo pipefail

scripts_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
workflow_path="${1:-${scripts_directory}/../../.github/workflows/ci-release.yml}"

if [[ ! -f "${workflow_path}" ]]; then
  echo "CI policy violation: workflow not found: ${workflow_path}" >&2
  exit 1
fi

fail() {
  echo "CI policy violation: $1" >&2
  exit 1
}

python3 -c 'import yaml' >/dev/null 2>&1 || fail 'python3 with PyYAML is required to parse the workflow'
python3 -c 'import sys, yaml; yaml.safe_load(open(sys.argv[1]))' "${workflow_path}" >/dev/null 2>&1 \
  || fail "workflow is not valid YAML (GitHub would reject it): ${workflow_path}"

require_pattern() {
  grep -Eq "$1" "${workflow_path}" || fail "$2"
}

reject_pattern() {
  if grep -Eq -e "$1" "${workflow_path}"; then
    fail "$2"
  fi
}

require_count() {
  local count
  count="$(grep -Ec "$1" "${workflow_path}" || true)"
  [[ "${count}" -eq "$2" ]] || fail "$3 (expected $2, found ${count})"
}

job_block() {
  awk -v job="$1:" '/^  [A-Za-z0-9_-]+:/ { inside = ($1 == job) } inside' "${workflow_path}"
}

require_in_job() {
  job_block "$1" | grep -Eq "$2" || fail "$1: $3"
}

require_pattern '^  pull_request:' 'pull requests must run CI'
require_pattern '^  push:' 'pushes must run CI'
reject_pattern '^  pull_request_target:' 'pull_request_target can expose protected context to untrusted code'
require_count "^      - 'e-commerce/\*\*'$" 2 'pull_request and push must filter on e-commerce/**'
require_count "^      - '\.github/workflows/ci-release\.yml'$" 2 'pull_request and push must filter on the workflow file'
require_pattern '^  ci-policy:' 'CI policy job is required'
require_pattern 'bash scripts/test-ci-policy\.sh' 'CI policy job must run the policy tests'
require_pattern '^  backend:' 'backend CI job is required'
require_pattern '^  frontend:' 'frontend CI job is required'
require_pattern '^  migration:' 'real MySQL migration lifecycle job is required'
require_pattern '^  compose-smoke:' 'Compose smoke job is required'
reject_pattern 'npm install --global|npm i -g|npm install -g' 'deployment CLIs must come from the committed tools/deploy lockfile, not a global npm install'
reject_pattern 'uses:[[:space:]]*[^[:space:]#]+@([^0-9a-f[:space:]#]|[0-9a-f]{0,39}([[:space:]#]|$)|[0-9a-f]{41,})' 'every action must be pinned to a full 40-character commit SHA'
reject_pattern '--token' 'CLI tokens must be read from the environment, never passed as argv (--token)'
tools_directory="${scripts_directory}/../tools/deploy"
grep -q '"@railway/cli": "[0-9]*\.[0-9]*\.[0-9]*"' "${tools_directory}/package.json" || fail 'Railway CLI must be pinned to an exact version in tools/deploy/package.json'
grep -q '"vercel": "[0-9]*\.[0-9]*\.[0-9]*"' "${tools_directory}/package.json" || fail 'Vercel CLI must be pinned to an exact version in tools/deploy/package.json'
[[ -f "${tools_directory}/package-lock.json" ]] || fail 'tools/deploy/package-lock.json is required'
reject_pattern 'railway"? up.*--detach' 'Railway deployment must wait for the deployment result (no --detach)'
reject_pattern '^    env:' 'job-level env exposes values to every step; scope secrets to the steps that need them'

require_count '^  deploy-[[:alnum:]_-]+:' 1 'exactly one production release job is allowed'
require_count '^  rollback-[[:alnum:]_-]+:' 1 'exactly one production rollback job is allowed'
require_pattern '^  workflow_dispatch:' 'manual dispatch is required for the rollback path'
require_pattern 'rollback_sha:' 'rollback must take the known-good commit SHA as input'

require_in_job deploy-production "if: github\.event_name == 'push' && github\.ref == 'refs/heads/main'$" 'deployment must be restricted to main push events'
require_in_job deploy-production 'needs: \[ci-policy, backend, frontend, migration, compose-smoke\]' 'deployment must wait for every CI gate'
require_in_job rollback-production "if: github\.event_name == 'workflow_dispatch' && github\.ref == 'refs/heads/main'$" 'rollback must be restricted to manual dispatch from main'
require_in_job rollback-production 'git merge-base --is-ancestor' 'rollback SHA must already be part of main'

for release_job in deploy-production rollback-production; do
  require_in_job "${release_job}" 'environment: production' 'must use the protected production environment'
  require_in_job "${release_job}" 'group: production-release' 'must share the serialized production concurrency group'
  require_in_job "${release_job}" 'cancel-in-progress: false' 'an in-progress production release must not be cancelled'
  require_in_job "${release_job}" 'working-directory: e-commerce/tools/deploy' 'deployment CLIs must be installed from tools/deploy'
  require_in_job "${release_job}" 'run: npm ci' 'deployment CLIs must be installed with npm ci from the lockfile'
  require_in_job "${release_job}" 'railway"? up --service' 'Railway deployment command is required'
  require_in_job "${release_job}" 'vercel"? deploy --prod' 'Vercel production deployment command is required'
  require_in_job "${release_job}" 'scripts/smoke-production\.sh' 'post-deployment smoke checks are required'
done

smoke_script="${scripts_directory}/smoke-production.sh"
grep -q 'curl --fail' "${smoke_script}" || fail 'post-deployment smoke checks must fail closed'
grep -q '/api/actuator/health' "${smoke_script}" || fail 'post-deployment smoke checks must cover the frontend /api proxy'

first_secret_line="$(grep -n 'secrets\.' "${workflow_path}" | head -1 | cut -d: -f1 || true)"
first_release_line="$(grep -nE '^  (deploy|rollback)-production:' "${workflow_path}" | head -1 | cut -d: -f1)"
if [[ -n "${first_secret_line}" && "${first_secret_line}" -le "${first_release_line}" ]]; then
  fail 'protected secrets must only appear inside the production release and rollback jobs'
fi

echo "CI workflow policy verified: CI on pull requests/pushes; one main-only protected release path; one main-only rollback path."
