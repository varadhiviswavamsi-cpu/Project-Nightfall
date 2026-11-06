#!/usr/bin/env bash
# PROJECT NIGHTFALL - release build script
# Compiles Java sources, embeds assets, and packages a standalone executable JAR.
# After running this script, use jpackage (see instructions below) to build
# native installers for Windows (.exe), macOS (.dmg/.pkg), and Linux (.deb/.rpm).
set -euo pipefail

cd "$(dirname "$0")"

echo "================================================"
echo "  PROJECT NIGHTFALL - Release Build"
echo "================================================"

# ---- Step 1: Compile Java sources ----------------------------------------
rm -rf build out
mkdir -p build/classes/assets/audio build/classes/images build/META-INF dist

echo "[1/4] Compiling Java sources..."
find src -name '*.java' > build/sources.txt
javac -d build/classes @build/sources.txt
echo "      Compiled $(wc -l < build/sources.txt | tr -d ' ') source files."

# ---- Step 2: Embed assets into classpath ---------------------------------
echo "[2/4] Embedding assets..."
cp assets/audio/*.wav build/classes/assets/audio/
cp assets/images/*.png build/classes/images/
echo "      Audio: $(ls build/classes/assets/audio/*.wav | wc -l) WAV files"
echo "      Images: $(ls build/classes/images/*.png | wc -l) PNG files"

# ---- Step 3: Create JAR manifest -----------------------------------------
echo "[3/4] Creating JAR manifest..."
cat > build/META-INF/MANIFEST.MF <<'EOF'
Manifest-Version: 1.0
Main-Class: nightfall.NightfallApp
EOF

# ---- Step 4: Package executable JAR --------------------------------------
echo "[4/4] Packaging ProjectNightfall.jar..."
cd build/classes
jar cfm ../ProjectNightfall.jar ../META-INF/MANIFEST.MF nightfall assets images
cd ..

echo ""
echo "================================================"
echo "  Build complete -> build/ProjectNightfall.jar"
echo "================================================"
echo ""
echo "To run the JAR directly (requires Java on target machine):"
echo "  java -jar build/ProjectNightfall.jar"
echo ""
echo "------------------------------------------------"
echo "NATIVE INSTALLER BUILD INSTRUCTIONS (jpackage)"
echo "------------------------------------------------"
echo ""
echo "Run jpackage on EACH target OS to produce double-clickable installers:"
echo ""
echo "  macOS (this machine):"
echo "    jpackage --input build --main-jar ProjectNightfall.jar \\"
echo "      --name \"Project NightFall\" --app-version 1.0 \\"
echo "      --icon build/ProjectNightfall.icns --type dmg --dist dist/macos"
echo ""
echo "  Windows (run on Windows):"
echo "    jpackage --input build --main-jar ProjectNightfall.jar \\"
echo "      --name \"Project NightFall\" --app-version 1.0 \\"
echo "      --icon build/ProjectNightfall.ico --type exe --dist dist/windows"
echo ""
echo "  Linux (run on Linux):"
echo "    jpackage --input build --main-jar ProjectNightfall.jar \\"
echo "      --name \"Project NightFall\" --app-version 1.0 \\"
echo "      --icon build/ProjectNightfall.png --type deb --dist dist/linux"
echo "    jpackage --input build --main-jar ProjectNightfall.jar \\"
echo "      --name \"Project NightFall\" --app-version 1.0 \\"
echo "      --icon build/ProjectNightfall.png --type rpm --dist dist/linux"
echo ""
echo "Fallback launch scripts (require Java installed):"
echo "  ./run.sh        (macOS / Linux)"
echo "  run.bat         (Windows)"
