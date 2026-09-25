#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
workflow_path="$(cd "${project_root}/.." && pwd)/.github/workflows/ci-release.yml"
temporary_directory="$(mktemp -d)"
trap 'rm -rf "${temporary_directory}"' EXIT

"${project_root}/scripts/verify-ci-policy.sh" "${workflow_path}"

expect_rejected() {
  local fixture_path="$1"
  local description="$2"
  if "${project_root}/scripts/verify-ci-policy.sh" "${fixture_path}" >/dev/null 2>&1; then
    echo "Policy test failed: ${description} was accepted." >&2
    exit 1
  fi
}

sed "s/github.event_name == 'push' && github.ref == 'refs\/heads\/main'/always()/" \
  "${workflow_path}" >"${temporary_directory}/unsafe-event.yml"
expect_rejected "${temporary_directory}/unsafe-event.yml" 'an unrestricted deployment event'

sed '/^  deploy-production:/i\  deploy-preview:' \
  "${workflow_path}" >"${temporary_directory}/second-deploy.yml"
expect_rejected "${temporary_directory}/second-deploy.yml" 'a second deployment path'

sed "/- 'e-commerce\/\*\*'/d" \
  "${workflow_path}" >"${temporary_directory}/missing-paths.yml"
expect_rejected "${temporary_directory}/missing-paths.yml" 'a trigger without the e-commerce paths filter'

sed 's/railway" up/railway" up --detach/' \
  "${workflow_path}" >"${temporary_directory}/detached-railway.yml"
expect_rejected "${temporary_directory}/detached-railway.yml" 'a detached Railway deployment'

sed 's/needs: \[ci-policy, /needs: [/' \
  "${workflow_path}" >"${temporary_directory}/missing-policy-gate.yml"
expect_rejected "${temporary_directory}/missing-policy-gate.yml" 'a deployment that skips the CI policy gate'

sed -e '/^  deploy-production:/a\    env:' \
  -e '/^  deploy-production:/a\      LEAKED_TOKEN: ${{ secrets.RAILWAY_TOKEN }}' \
  "${workflow_path}" >"${temporary_directory}/job-level-secret.yml"
expect_rejected "${temporary_directory}/job-level-secret.yml" 'a job-level secret environment'

sed "s/github.event_name == 'workflow_dispatch' && github.ref == 'refs\/heads\/main'/always()/" \
  "${workflow_path}" >"${temporary_directory}/unsafe-rollback-event.yml"
expect_rejected "${temporary_directory}/unsafe-rollback-event.yml" 'an unrestricted rollback event'

awk '/^  [A-Za-z0-9_-]+:/ { job = $1 } job == "rollback-production:" { sub(/environment: production/, "environment: staging") } { print }' \
  "${workflow_path}" >"${temporary_directory}/unprotected-rollback.yml"
expect_rejected "${temporary_directory}/unprotected-rollback.yml" 'a rollback outside the protected production environment'

awk '/^  [A-Za-z0-9_-]+:/ { job = $1 } !(job == "rollback-production:" && /smoke-production\.sh/) { print }' \
  "${workflow_path}" >"${temporary_directory}/unverified-rollback.yml"
expect_rejected "${temporary_directory}/unverified-rollback.yml" 'a rollback without production smoke checks'

sed '0,/uses: actions\/checkout@[0-9a-f]\{40\}/s//uses: actions\/checkout@v4/' \
  "${workflow_path}" >"${temporary_directory}/tag-pinned-action.yml"
expect_rejected "${temporary_directory}/tag-pinned-action.yml" 'an action pinned to a mutable tag'

sed '0,/uses: actions\/checkout@\([0-9a-f]\{7\}\)[0-9a-f]\{33\}/s//uses: actions\/checkout@\1/' \
  "${workflow_path}" >"${temporary_directory}/short-sha-action.yml"
expect_rejected "${temporary_directory}/short-sha-action.yml" 'an action pinned to a short SHA'

sed '0,/run: npm ci$/s//run: npm install --global @railway\/cli@5.52.0 vercel@59.15.1/' \
  "${workflow_path}" >"${temporary_directory}/global-npm-install.yml"
expect_rejected "${temporary_directory}/global-npm-install.yml" 'a global npm install of deployment CLIs'

sed 's/deploy --prod --yes$/deploy --prod --yes --token "$VERCEL_TOKEN"/' \
  "${workflow_path}" >"${temporary_directory}/token-argv.yml"
expect_rejected "${temporary_directory}/token-argv.yml" 'a Vercel token passed on the command line'

sed -e '/^  deploy-production:/,/^  rollback-production:/{/run: |$/{N;s/run: |\n *\("[^"]*railway" up.*\)/run: \1/}}' \
  "${workflow_path}" >"${temporary_directory}/invalid-yaml-quoted-run.yml"
expect_rejected "${temporary_directory}/invalid-yaml-quoted-run.yml" 'a workflow with a broken quoted run: line (invalid YAML)'

echo "CI policy rejection fixtures passed."
