@echo off
REM PROJECT NIGHTFALL - Windows launcher
REM Runs the game from the executable JAR.

cd /d "%~dp0"

if not exist "ProjectNightfall.jar" (
    echo ERROR: ProjectNightfall.jar not found.
    echo Please ensure the JAR is in the same folder as this script.
    pause
    exit /b 1
)

java -jar ProjectNightfall.jar %*
