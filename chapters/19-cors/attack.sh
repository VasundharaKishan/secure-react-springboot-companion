#!/usr/bin/env bash
#
# Chapter 19 — CORS wildcard with credentials
#
# Send a request with Origin: https://evil.example.
# Inspect the Access-Control-Allow-Origin header in the response.
# - Vulnerable config: header echoes "https://evil.example" + Allow-Credentials: true.
#   Browsers will let evil.example read the response.
# - Fixed config: header is absent (or set to a permitted origin), browsers block.

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

ATTACKER_ORIGIN='https://evil.example.com'

bold "[1/2] Send a request claiming to come from ${ATTACKER_ORIGIN}"
gray "  GET ${API}/api/products/search?q=keyboard"
gray "  Origin: ${ATTACKER_ORIGIN}"
RESPONSE=$(curl -sSi \
  -H "Origin: ${ATTACKER_ORIGIN}" \
  "${API}/api/products/search?q=keyboard")
echo "$RESPONSE" | head -15
echo

ALLOW_ORIGIN=$(echo "$RESPONSE" | grep -i '^access-control-allow-origin:' || true)
ALLOW_CREDS=$(echo "$RESPONSE" | grep -i '^access-control-allow-credentials:' || true)

bold "[2/2] Inspect"
echo "  ${ALLOW_ORIGIN:-(no Access-Control-Allow-Origin header)}"
echo "  ${ALLOW_CREDS:-(no Access-Control-Allow-Credentials header)}"
echo

if echo "$ALLOW_ORIGIN" | grep -qi "evil.example"; then
  bold "ATTACK SUCCEEDED — server reflects attacker origin AND allows credentials."
  echo "  A malicious page on https://evil.example can now run:"
  echo "    fetch('${API}/api/me', { credentials: 'include' }).then(r => r.json())"
  echo "  …and read every API response from the victim's session."
  exit 1
elif [ -z "$ALLOW_ORIGIN" ]; then
  bold "ATTACK BLOCKED — server returned no CORS header for unknown origin."
  echo "  Browser will refuse to deliver the response to evil.example."
else
  bold "Access-Control-Allow-Origin set to a non-attacker value:"
  echo "  $ALLOW_ORIGIN"
  echo "  Browser will block the cross-origin read."
fi
