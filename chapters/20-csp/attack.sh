#!/usr/bin/env bash
#
# Chapter 20 — CSP misconfiguration
#
# Inspect the Content-Security-Policy header on the response. Flag the
# specific dangerous directives:
#   - 'unsafe-inline' in script-src
#   - 'unsafe-eval' in script-src
#   - * (wildcard) in any directive
#
# Without these, a stored XSS payload (Chapter 15) cannot execute even
# if it lands in the DOM, because the browser refuses to run inline scripts.

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }

bold "[1/2] Fetch the CSP header"
CSP=$(curl -sI "${API}/api/products/search?q=keyboard" \
  | grep -i '^content-security-policy:' | sed 's/^[^:]*: //' | tr -d '\r')

if [ -z "$CSP" ]; then
  echo "  No Content-Security-Policy header at all — see Chapter 21."
  exit 1
fi
echo "  ${CSP}"
echo

bold "[2/2] Check for dangerous directives"
PROBLEMS=()

if echo "$CSP" | grep -q "'unsafe-inline'.*script-src\|script-src[^;]*'unsafe-inline'"; then
  PROBLEMS+=("script-src includes 'unsafe-inline' — XSS payloads execute")
fi
if echo "$CSP" | grep -q "'unsafe-eval'"; then
  PROBLEMS+=("'unsafe-eval' present — prototype-pollution chains succeed")
fi
if echo "$CSP" | grep -qE "(script-src|connect-src|object-src)[^;]*\*"; then
  PROBLEMS+=("wildcard (*) in a sensitive directive — any origin is trusted")
fi
if ! echo "$CSP" | grep -q "frame-ancestors"; then
  PROBLEMS+=("no frame-ancestors — clickjacking still possible")
fi

if [ ${#PROBLEMS[@]} -eq 0 ]; then
  bold "ATTACK SURFACE MINIMIZED — CSP rejects inline script and unsafe eval."
else
  bold "ATTACK SURFACE OPEN — ${#PROBLEMS[@]} CSP weakness(es) found:"
  for p in "${PROBLEMS[@]}"; do echo "  ✗ $p"; done
  echo
  echo "  Combined with stored XSS (Chapter 15), the page is fully exploitable."
  exit 1
fi
