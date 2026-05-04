#!/usr/bin/env bash
#
# Chapter 11 — Missing @PreAuthorize on admin endpoint
#
# 1. Login as alice (USER role — not ADMIN)
# 2. Hit GET /api/admin/users with her token
#    - Vulnerable controller: 200 + every user's email and role
#    - Fixed controller:      403

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

bold "[1/2] Login as alice (USER role)"
LOGIN=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"alice123!"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
ROLE=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['role'])")
gray "  Logged in as alice (role=${ROLE})"
echo

bold "[2/2] Try to list all users (admin-only endpoint)"
gray "  GET ${API}/api/admin/users"
RESPONSE=$(curl -sS -o /tmp/resp.json -w '%{http_code}' \
  -H "Authorization: Bearer ${TOKEN}" \
  "${API}/api/admin/users")
echo "  HTTP ${RESPONSE}"
BODY=$(cat /tmp/resp.json)
echo "  Body (first 500 chars): ${BODY:0:500}"
echo

if [ "$RESPONSE" = "403" ]; then
  bold "ATTACK BLOCKED — @PreAuthorize rejected the non-admin caller."
elif [ "$RESPONSE" = "200" ]; then
  COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
  bold "ATTACK SUCCEEDED — alice (USER) listed ${COUNT} users including admins."
  echo "  This is privilege escalation by absence-of-check."
  exit 1
else
  bold "Unexpected response (${RESPONSE})."
  exit 2
fi
