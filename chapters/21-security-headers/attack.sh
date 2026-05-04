#!/usr/bin/env bash
#
# Chapter 21 — Missing security headers (clickjacking surface)
#
# 1. curl -I against the API to see what headers are returned
# 2. Specifically check for:
#    - Strict-Transport-Security (HSTS)
#    - Content-Security-Policy   (frame-ancestors clause)
#    - X-Frame-Options
#    - X-Content-Type-Options
# 3. Optionally serve the bundled clickjack.html locally and try iframing
#    a page from this app. If headers are missing, the iframe loads.

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

bold "[1/2] Inspect response headers"
gray "  curl -sI ${API}/api/products/search?q=keyboard"
HEADERS=$(curl -sI "${API}/api/products/search?q=keyboard")
echo "$HEADERS"
echo

bold "[2/2] Check each defensive header"
EXPECTED=(
  'strict-transport-security'
  'content-security-policy'
  'x-frame-options'
  'x-content-type-options'
)

MISSING=()
for h in "${EXPECTED[@]}"; do
  if echo "$HEADERS" | grep -qi "^${h}:"; then
    printf '  \033[32m✓\033[0m %s\n' "$h"
  else
    printf '  \033[31m✗\033[0m %s  (MISSING)\n' "$h"
    MISSING+=("$h")
  fi
done
echo

if [ ${#MISSING[@]} -eq 0 ]; then
  bold "ATTACK SURFACE MINIMIZED — all defensive headers present."
  echo "  Browsers will refuse to iframe this site (clickjacking blocked),"
  echo "  upgrade requests to HTTPS (no SSL strip), and not sniff content type."
else
  bold "ATTACK SURFACE OPEN — ${#MISSING[@]} defensive header(s) missing."
  echo "  - Without X-Frame-Options / frame-ancestors: clickjacking is possible."
  echo "  - Without HSTS: SSL strip + downgrade attacks succeed on first visit."
  echo "  - Without nosniff: uploaded files can be sniffed as HTML and execute."
  echo
  echo "  See chapters/21-security-headers/clickjack-poc.html for an iframe demo."
  exit 1
fi
