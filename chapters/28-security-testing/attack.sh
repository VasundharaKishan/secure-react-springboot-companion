#!/usr/bin/env bash
#
# Chapter 28 — Demonstrating the @WithMockUser blind spot
#
# 1. Place the VULNERABLE AdminController (no @PreAuthorize, from Ch 11)
# 2. Place the VULNERABLE AdminControllerTest (only the happy-path test)
# 3. Run `mvn test` — green
# 4. Place the FIXED test (adds the negative case)
# 5. Run `mvn test` — RED, because the missing @PreAuthorize lets a USER
#    in but the test now asserts they should get 403
#
# This shows how a green test suite can mask a critical security bug.

set -euo pipefail
REPO=${REPO:-/Users/e141057/Desktop/work/clade_play_area/security-book-code}
BACKEND="$REPO/backend"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

bold "[1/4] Stage the vulnerable AdminController (no @PreAuthorize)"
cp "$REPO/chapters/11-authorization-fundamentals/vulnerable/AdminController.java" \
   "$BACKEND/src/main/java/com/securitybook/app/admin/AdminController.java"
echo

bold "[2/4] Stage the vulnerable test (happy path only)"
mkdir -p "$BACKEND/src/test/java/com/securitybook/app/admin"
cp "$REPO/chapters/28-security-testing/vulnerable/AdminControllerTest.java" \
   "$BACKEND/src/test/java/com/securitybook/app/admin/AdminControllerTest.java"
echo

bold "[3/4] Run the test suite — should be GREEN despite the missing role check"
gray "  cd $BACKEND && mvn -B -q -Dtest=AdminControllerTest test"
( cd "$BACKEND" && mvn -B -q -Dtest=AdminControllerTest test 2>&1 | tail -10 ) || true
echo

bold "[4/4] Replace with the FIXED test (adds the negative case) and re-run"
cp "$REPO/chapters/28-security-testing/fixed/AdminControllerTest.java" \
   "$BACKEND/src/test/java/com/securitybook/app/admin/AdminControllerTest.java"
gray "  cd $BACKEND && mvn -B -q -Dtest=AdminControllerTest test"
RESULT=$( cd "$BACKEND" && mvn -B -q -Dtest=AdminControllerTest test 2>&1 | tail -15 )
echo "$RESULT"
echo

if echo "$RESULT" | grep -q "BUILD FAILURE\|FAILED"; then
  bold "PROOF — the new negative case caught the missing @PreAuthorize."
  echo "  This is what your test suite should look like for every role-based endpoint."
else
  bold "Tests passed — controller might already be the fixed version."
fi

echo
echo "Restore the canonical (FIXED) AdminController:"
echo "  cp chapters/11-authorization-fundamentals/fixed/AdminController.java \\"
echo "     backend/src/main/java/com/securitybook/app/admin/AdminController.java"
