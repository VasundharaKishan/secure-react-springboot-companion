#!/usr/bin/env bash
#
# Chapter 24 — File upload extension-only check
#
# 1. Build a malicious "image" — actually HTML with a <script> tag, named .png
# 2. POST it to /api/uploads
#    - Vulnerable controller: accepts (extension matches), serves with no content-type override
#    - Fixed controller: rejects (magic bytes are ASCII, not PNG header)
# 3. If accepted, GET it back and check whether the response body is HTML

set -euo pipefail
API="${API:-http://localhost:8080}"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }

bold "[1/3] Build malicious file (HTML disguised as PNG)"
TMPFILE=$(mktemp -t evil.XXXXXX.png)
cat > "$TMPFILE" <<'HTML'
<!doctype html>
<html><body>
<h1>If you can read this rendered, the upload defense failed.</h1>
<script>document.body.style.background='red'</script>
</body></html>
HTML
echo "  $TMPFILE"
echo "  First bytes: $(head -c 16 "$TMPFILE" | xxd | head -1)"
echo

bold "[2/3] Upload"
RESP=$(curl -sS -X POST "${API}/api/uploads" -F "file=@${TMPFILE};filename=evil.png")
echo "  $RESP"

ID=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || true)
if [ -z "$ID" ]; then
  bold "ATTACK BLOCKED — server rejected the file (magic-byte check)."
  rm -f "$TMPFILE"
  exit 0
fi
echo

bold "[3/3] Download and inspect"
DLRESP=$(curl -sSi "${API}/api/uploads/${ID}")
echo "$DLRESP" | head -10
echo

CTYPE=$(echo "$DLRESP" | grep -i '^content-type' | head -1)
NOSNIFF=$(echo "$DLRESP" | grep -i 'x-content-type-options' || true)

if echo "$CTYPE" | grep -qi 'image/'; then
  bold "ATTACK BLOCKED — server forced Content-Type to image/* on the way out."
  if [ -n "$NOSNIFF" ]; then echo "  Plus X-Content-Type-Options: nosniff is set."; fi
else
  bold "ATTACK SUCCEEDED — uploaded HTML served back without image content-type."
  echo "  Browsers will sniff this as text/html and execute the script."
  rm -f "$TMPFILE"
  exit 1
fi

rm -f "$TMPFILE"
