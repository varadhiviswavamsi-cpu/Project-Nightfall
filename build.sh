#!/usr/bin/env bash
# PROJECT NIGHTFALL - build script
# Compiles all Java sources under src/ into out/.
set -euo pipefail

cd "$(dirname "$0")"

rm -rf out
mkdir -p out

find src -name '*.java' > out/sources.txt

echo "Compiling $(wc -l < out/sources.txt | tr -d ' ') source files..."
javac -d out @out/sources.txt

echo "Build complete -> out/"
