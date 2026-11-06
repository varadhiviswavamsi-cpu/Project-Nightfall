#!/usr/bin/env bash
# PROJECT NIGHTFALL - run script
# Builds the project if needed, then launches the game.
# Any arguments are passed through to the app (e.g. --selftest).
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -d out ]; then
  ./build.sh
fi

exec java -cp out nightfall.NightfallApp "$@"
