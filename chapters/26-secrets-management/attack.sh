#!/usr/bin/env bash
#
# Chapter 26 — Secret in React production bundle
#
# Build the frontend with the chapter's .env.production in place, then
# grep the resulting JS bundle for the secret. If the secret string is in
# the bundle, every browser receives it.

set -euo pipefail

REPO=${REPO:-/Users/e141057/Desktop/work/clade_play_area/security-book-code}
FRONTEND="$REPO/frontend"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

bold "[1/3] Confirm chapter env file is in place"
ls -la "$FRONTEND/.env.production" 2>/dev/null || {
  echo "  Copy chapters/26-secrets-management/vulnerable/.env.production (or fixed/) to $FRONTEND/.env.production first."
  exit 2
}
echo
gray "  $(cat "$FRONTEND/.env.production")"
echo

bold "[2/3] Build the production bundle"
( cd "$FRONTEND" && npm run build 2>&1 | tail -5 )
echo

bold "[3/3] Search the bundle for secret-looking strings"
HITS=$(grep -rE 'sk_live_|SG\.|VITE_STRIPE_SECRET|VITE_SENDGRID|VITE_DB_ADMIN' "$FRONTEND/dist/assets/" 2>/dev/null || true)
if [ -n "$HITS" ]; then
  bold "ATTACK SUCCEEDED — secrets found in production bundle:"
  echo "$HITS" | head -10
  echo
  echo "  Anyone who downloads /assets/index-*.js can read these. Free, no auth needed."
  exit 1
else
  bold "ATTACK BLOCKED — no secret-shaped strings in the bundle."
  echo "  (Either the secret never reached the bundle, or you're using the fixed .env.production.)"
fi
