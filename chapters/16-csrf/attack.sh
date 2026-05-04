#!/usr/bin/env bash
#
# Chapter 16 — State-changing GET endpoint
#
# 1. Register a throwaway user (so we don't kill alice/bob from seed)
# 2. Show that visiting GET /api/account/delete in the vulnerable build
#    deletes the account.
# 3. Show that the same call against the fixed build returns 405 (Method
#    Not Allowed) because only DELETE is mapped.

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

EMAIL="csrf-target-$(date +%s)@example.com"
PASS="csrf123!"

bold "[1/3] Register throwaway victim ${EMAIL}"
REG=$(curl -sS -X POST "${API}/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASS}\",\"role\":\"USER\"}")
echo "  $REG"
echo

bold "[2/3] Login and capture the token"
LOGIN=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASS}\"}")
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo

bold "[3/3] Fire GET /api/account/delete with the bearer token"
gray "  GET ${API}/api/account/delete"
RESP=$(curl -sS -o /tmp/r.json -w '%{http_code}' \
  -H "Authorization: Bearer ${TOKEN}" \
  "${API}/api/account/delete")
echo "  HTTP ${RESP}"
echo "  Body: $(cat /tmp/r.json)"
echo

if [ "$RESP" = "200" ]; then
  bold "ATTACK SUCCEEDED — account deleted via a GET request."
  echo
  echo "  Real-world impact: an attacker need only put this URL in"
  echo "  an <img> tag, an open-graph preview, or a browser bookmark."
  echo "  Anyone authenticated who triggers the URL load loses their account."
  exit 1
elif [ "$RESP" = "405" ] || [ "$RESP" = "404" ]; then
  bold "ATTACK BLOCKED — endpoint refuses GET."
  echo "  Server only accepts DELETE for destructive actions."
elif [ "$RESP" = "401" ] || [ "$RESP" = "403" ]; then
  bold "ATTACK BLOCKED — server rejected the call."
else
  bold "Unexpected response (${RESP})."
fi
