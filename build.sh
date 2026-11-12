#!/usr/bin/env bash
# PROJECT NIGHTFALL - build script
# Compiles all Java sources under src/ into output/.
set -euo pipefail

cd "$(dirname "$0")"

rm -rf output
mkdir -p output

find src -name '*.java' > output/sources.txt

echo "Compiling $(wc -l < output/sources.txt | tr -d ' ') source files..."
javac -d output @output/sources.txt

echo "Build complete -> output/"
