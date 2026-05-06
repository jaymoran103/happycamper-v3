#!/usr/bin/env bash
set -euo pipefail

# verify-desktop.sh
# Flexible verification script for the multi-module migration.
# Beyond basic build, offers options to run tests and/or launch the desktop JAR for a smoke test.

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
cd "$BASEDIR"

RUN_TESTS=false
RUN_JAR=false

# Usage function to display help message
usage() {
  cat <<EOF
Usage: $0 [OPTIONS]
Options:
  -t            run tests
  -j            launch the desktop JAR
  -h, --help    show this help message

Examples:
  $0           # just build, no tests or smoke run
  $0 -t        # build + tests only
  $0 -j        # build + smoke only
EOF
}

# Iterate over arguments, setting flags based on options.
# Exit on help or unknown options.
while [[ $# -gt 0 ]]; do
  case "$1" in
    -t|--tests)
      RUN_TESTS=true
      shift
      ;;
    -j|--jar)
      RUN_JAR=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

# Phase 1: Optionally run tests
if [[ "$RUN_TESTS" == true ]]; then
  echo "[1/3] Running full integration tests..."
  mvn clean test
else
  echo "[1/3] Skipping tests..."
fi

# Phase 2: Build the desktop module JAR. This is the only step guaranteed to run.
echo "[2/3] Building the desktop module JAR without tests..."
mvn -pl happycamper-desktop -am clean package -DskipTests

# Ensure the JAR was created successfully
DESKTOP_JAR="happycamper-desktop/target/happycamper-desktop-2.2.jar"
if [[ ! -f "$DESKTOP_JAR" ]]; then
  echo "ERROR: desktop JAR not found: $DESKTOP_JAR"
  exit 1
fi

# Phase 3: Optionally launch the desktop JAR
if [[ "$RUN_JAR" == true ]]; then
  echo "[3/3] Launching desktop JAR..."
  java -jar "$DESKTOP_JAR"
else
  echo "[3/3] Skipping desktop JAR launch."
fi

echo "Desktop verification completed successfully."
