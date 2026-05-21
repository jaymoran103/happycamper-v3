#!/usr/bin/env bash
set -euo pipefail

# run-web.sh
# Install all modules to ~/.m2 (so happycamper-web can resolve happycamper-core)
# and then launch the Spring Boot service on http://localhost:8080.

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
cd "$BASEDIR"

SKIP_INSTALL=false

usage() {
  cat <<EOF
Usage: $0 [OPTIONS]
Options:
  -s            skip the install step (use when core has not changed since last install)
  -h, --help    show this help message

Examples:
  $0            full install + run (safe default)
  $0 -s         skip install, just run (fast loop when iterating on web only)
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s) SKIP_INSTALL=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1"; usage; exit 1 ;;
  esac
done

if [[ "$SKIP_INSTALL" == "false" ]]; then
  echo "==> Installing all modules to ~/.m2 (skip tests)"
  mvn install -DskipTests -B -ntp
fi

echo "==> Launching happycamper-web on http://localhost:8080 (Ctrl+C to stop)"
mvn -pl happycamper-web spring-boot:run -B -ntp
