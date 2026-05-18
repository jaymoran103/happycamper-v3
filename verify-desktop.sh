#!/usr/bin/env bash
set -euo pipefail

# verify-desktop.sh
# Flexible verification script for the multi-module migration.
# Beyond basic build, offers options to run tests, launch the desktop JAR for a
# smoke test, or launch via a named preset (test-classpath, exec:java).

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
cd "$BASEDIR"

RUN_TESTS=false
RUN_JAR=false
PRESET=""

usage() {
  cat <<EOF
Usage: $0 [OPTIONS]
Options:
  -t                run tests
  -j                launch the desktop (JAR for default, exec:java for preset)
  -p, --preset N    launch with preset <N> (implies test classpath; pairs with -j)
  -h, --help        show this help message

Examples:
  $0                       # just build, no tests or smoke run
  $0 -t                    # build + tests only
  $0 -j                    # build + launch production JAR (no preset)
  $0 -j -p demo-small      # build + launch via preset (mvn exec:java with test classpath)
  $0 -t -j -p demo-small   # full pass: tests + preset launch
EOF
}

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
    -p|--preset)
      if [[ $# -lt 2 || -z "${2:-}" || "$2" == -* ]]; then
        echo "ERROR: --preset requires a name (e.g. -p demo-small)"
        exit 1
      fi
      PRESET="$2"
      shift 2
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
  echo "[1/3] Running full test suite..."
  mvn clean verify
else
  echo "[1/3] Skipping tests..."
fi

# Phase 2: Build the desktop module JAR. This is the only step guaranteed to run.
echo "[2/3] Building the desktop module JAR without tests..."
mvn -pl happycamper-desktop -am clean package -DskipTests

DESKTOP_JAR="happycamper-desktop/target/happycamper-desktop-2.2.jar"
if [[ ! -f "$DESKTOP_JAR" ]]; then
  echo "ERROR: desktop JAR not found: $DESKTOP_JAR"
  exit 1
fi

# Phase 3: Optionally launch
if [[ "$RUN_JAR" == true ]]; then
  if [[ -n "$PRESET" ]]; then
    echo "[3/3] Launching desktop via preset '$PRESET' (mvn exec:java, test classpath)..."
    mvn -pl happycamper-desktop -am test-compile -q
    mvn -pl happycamper-desktop exec:java -Dhappycamper.preset="$PRESET"
  else
    echo "[3/3] Launching desktop JAR (no preset)..."
    java -jar "$DESKTOP_JAR"
  fi
else
  if [[ -n "$PRESET" ]]; then
    echo "[3/3] -p '$PRESET' specified without -j; build-only mode. Re-run with -j to launch."
  else
    echo "[3/3] Skipping desktop launch."
  fi
fi

echo "Desktop verification completed successfully."
