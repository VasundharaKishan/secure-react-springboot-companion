#!/usr/bin/env bash
#
# Chapter 31 — Password hash in logs
#
# Trigger an error path that includes a User in the response or log
# message. Inspect the server log for bcrypt hashes.
#
# The simplest reproducer: hit a non-existent user endpoint that returns
# the User principal in its 500 message.

set -euo pipefail
API="${API:-http://localhost:8080}"
APP_LOG="${APP_LOG:-/tmp/app.log}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

if [ ! -f "$APP_LOG" ]; then
  echo "  Server log not found at $APP_LOG."
  echo "  Start the backend with:  java -jar backend/target/security-book-app-0.1.0.jar > /tmp/app.log 2>&1 &"
  echo "  …or set APP_LOG to your log path before running."
  exit 2
fi

bold "[1/3] Login as alice to obtain her User principal"
LOGIN=$(curl -sS -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"alice123!"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo

bold "[2/3] Trigger a request whose default error path may log the principal"
gray "  GET ${API}/api/admin/users  (alice has USER role — 403 from the @PreAuthorize fix)"
curl -sS -o /dev/null -H "Authorization: Bearer ${TOKEN}" "${API}/api/admin/users" || true
sleep 1
echo

bold "[3/3] Search the log for bcrypt-shaped strings"
HITS=$(grep -E '\$2[aby]\$[0-9]{2}\$[A-Za-z0-9./]{53}' "$APP_LOG" || true)

if [ -n "$HITS" ]; then
  bold "ATTACK SUCCEEDED — bcrypt hashes appear in the server log:"
  echo "$HITS" | head -5
  echo
  echo "  Anyone with log access (Datadog, Splunk, the on-call engineer)"
  echo "  now has crackable hashes. At cost-12 a desktop GPU still does"
  echo "  thousands of guesses per second per hash."
  exit 1
else
  bold "ATTACK BLOCKED — no bcrypt hashes in the log."
  echo "  Either @ToString was excluded for passwordHash, or no error path"
  echo "  serialized the User principal."
fi
