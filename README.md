<<<<<<< HEAD
# PROJECT NIGHTFALL

**LAST LINE OF DEFENSE**

A polished, offline arcade defense game built with Java Swing / Java2D. Enemy
missiles rain down from the night sky toward your cities — intercept them with
expanding blast zones before they reach the ground. No internet connection, no
external services, no build tools required beyond the JDK.

---

## Requirements

- **JDK 21 or newer** (developed and tested on JDK 26). Any modern JDK with
  `javac` and `java` on your `PATH` works.
- macOS, Linux, or Windows.

Check your install:

```sh
javac -version
java -version
```

---

## Build

From the project root:

```sh
./build.sh
```

This compiles every `.java` file under `src/` into the `out/` directory.

If the script is not executable yet, either make it executable or run it with
bash:

```sh
chmod +x build.sh run.sh
# or:
bash build.sh
```

---

## Standalone App (double-clickable installers)

The game can be packaged into self-contained native installers that bundle the
JRE, so recipients do **not** need Java installed. Run `./release.sh` to build
the executable JAR, then use `jpackage` (bundled with the JDK) on each target
OS to create installers:

| Platform | Installer | Build command |
|----------|-----------|---------------|
| macOS | `.dmg` / `.pkg` / `.app` | `jpackage --input build --main-jar ProjectNightfall.jar --name "Project NightFall" --app-version 1.0 --icon build/ProjectNightfall.icns --type dmg --dist dist/macos` |
| Windows | `.exe` | `jpackage --input build --main-jar ProjectNightfall.jar --name "Project NightFall" --app-version 1.0 --icon build/ProjectNightfall.ico --type exe --dist dist/windows` |
| Linux | `.deb` / `.rpm` | `jpackage --input build --main-jar ProjectNightfall.jar --name "Project NightFall" --app-version 1.0 --icon build/ProjectNightfall.png --type deb --dist dist/linux` |

See `release.sh` for the full build script and the exact `jpackage` invocations.
The JAR embeds all audio and image assets, so the installed app is fully
self-contained.

---

## Install on Windows Laptop

### Option 1: Native `.exe` Installer (Recommended — No Java Needed)

The native installer bundles the Java Runtime, so the recipient does not need
to install Java.

**To build the installer** (must be done on a Windows machine with JDK
installed):

```powershell
jpackage --input build --main-jar ProjectNightfall.jar `
  --name "Project NightFall" `
  --app-version 1.0 `
  --icon build/ProjectNightfall.ico `
  --type exe `
  --dist dist/windows
```

This produces `dist/windows/Project NightFall Setup.exe`.

**To install:**
1. Copy `Project NightFall Setup.exe` to the Windows laptop.
2. Double-click it and follow the installer prompts.
3. Launch from the Start Menu shortcut or desktop icon — no Java installation
   required.

### Option 2: Portable JAR (Java Required)

1. Install Java (JDK 21 or newer) from https://adoptium.net.
2. Copy `build/ProjectNightfall.jar` and `run.bat` to a folder on the laptop.
3. Double-click `run.bat`, or run from Command Prompt:

   ```powershell
   java -jar ProjectNightfall.jar
   ```

---

## Install on Linux Laptop

### Option 1: Native `.deb` / `.rpm` Package (Recommended — No Java Needed)

The native package bundles the Java Runtime, so the recipient does not need
to install Java.

**To build the package** (must be done on a Linux machine with JDK installed):

Debian / Ubuntu / Mint:

```bash
jpackage --input build --main-jar ProjectNightfall.jar \
  --name "Project NightFall" \
  --app-version 1.0 \
  --icon build/ProjectNightfall.png \
  --type deb \
  --dist dist/linux
```

Fedora / RHEL / openSUSE:

```bash
jpackage --input build --main-jar ProjectNightfall.jar \
  --name "Project NightFall" \
  --app-version 1.0 \
  --icon build/ProjectNightfall.png \
  --type rpm \
  --dist dist/linux
```

**To install:**

Debian / Ubuntu-based:

```bash
sudo apt install ./dist/linux/project-nightfall_1.0_amd64.deb
```

RPM-based:

```bash
sudo dnf install ./dist/linux/project-nightfall-1.0-1.x86_64.rpm
# or:
sudo rpm -i ./dist/linux/project-nightfall-1.0-1.x86_64.rpm
```

Launch "Project NightFall" from your application menu.

### Option 2: Portable JAR (Java Required)

1. Install Java:

   ```bash
   # Ubuntu / Debian:
   sudo apt update && sudo apt install openjdk-21-jre

   # Fedora:
   sudo dnf install java-21-openjdk
   ```

2. Copy `build/ProjectNightfall.jar` and `run.sh` to a folder.
3. Make executable and run:

   ```bash
   chmod +x run.sh
   ./run.sh
   ```

   Or directly:

   ```bash
   java -jar ProjectNightfall.jar
   ```

---

## Fallback Launch Scripts (Require Java on Target Machine)

```sh
./run.sh        # macOS / Linux
run.bat         # Windows (double-click)
```

---

## Run

```sh
./run.sh
```

`run.sh` builds automatically if `out/` does not exist, then launches the game.
Any arguments you pass are forwarded to the app (see **Command-line options**).

You can also run directly:

```sh
java -cp out nightfall.NightfallApp
```

---

## Controls

| Action            | Input                                              |
|-------------------|----------------------------------------------------|
| Aim               | Move the mouse                                     |
| Fire interceptor  | Left mouse button                                  |
| Pause / Resume    | `P` or `Esc`, or click the **PAUSE** button (top-right of the HUD while playing) |
| Restart (in game) | `R`                                                |
| Confirm / Select  | `Enter` or `Space`                                 |
| Toggle fullscreen | `F11`                                              |

Firing selects a battery based on where you aim: the playfield is split into
three vertical zones, each assigned to the nearest launch battery. If that
battery is out of ammo or cooling down, the game falls back to the nearest
battery that can fire.

---

## How to Play

- **Defend the cities.** Five cities line the horizon. Each enemy missile that
  reaches a city destroys part of it; lose all cities and it's game over.
- **Interceptors create blast zones.** Clicking launches a fast interceptor
  toward the cursor. It detonates at the target point, and its expanding
  explosion destroys any missile it touches.
- **Ammo is finite.** Each of the three batteries holds 10 interceptors. You
  resupply between waves — use shots wisely.
- **Build a combo.** Destroying missiles in quick succession raises your combo
  multiplier (up to x5). Higher waves and higher combos mean bigger scores.
- **Survive ten waves** to win. On the victory screen, choose **PLAY AGAIN** to
  start a fresh run on Wave 1, or **MAIN MENU** to return to the title screen.

### Threat types

- **Standard** — a steady, predictable descent.
- **Fast** — quick and thin, hard to track.
- **Splitter** — splits mid-flight into multiple warheads. Hit it early.

---

## Difficulty Modes

Three difficulty presets are selectable from the main menu (the selected mode is
shown on the HUD and on every gameplay overlay — pause, game-over, and victory):

| Mode   | Missile speed | Feel                                  |
|--------|---------------|---------------------------------------|
| **NOOB** | 0.45 × baseline | Significantly slower warheads — easy to intercept through all ten waves |
| **PRO**  | 1.0 × baseline | Current default speed — balanced challenge |
| **GOD**  | 2.0 × baseline | Nearly impossible — win only with fast reflexes and smart battery placement |

The mode you pick is registered in your settings and carried into the gameplay
page. It is **not** shown on the main menu itself, only in-game.

---

## Features

- Animated starfield, glowing horizon grid, and a living city skyline.
- Layered explosions, missile trails, impact flashes, and restrained screen
  shake.
- Tactical HUD with score, best score, wave counter, city status, threat meter,
  and combo bar.
- Menu, pause, game-over, and victory screens with hoverable buttons.
- Clickable **PAUSE** button in the top-right corner of the in-game HUD.
- **Difficulty mode** (NOOB / PRO / GOD) displayed on the gameplay HUD and every overlay; selected from the main menu.
- Persistent-free, self-contained: high scores and settings live in memory for
  the session.
- Optional sound effects (loaded from `assets/audio/` if present).

---

## Command-line options

| Option         | Effect                                                        |
|----------------|---------------------------------------------------------------|
| `--selftest`   | Run the headless self-test suite and exit (`0` = pass).       |
| `--frames N`   | Render `N` simulated frames headlessly to `nightfall_frames.png` and exit. |

Examples:

```sh
java -cp out nightfall.NightfallApp --selftest
java -cp out nightfall.NightfallApp --frames 120
```

---

## Project layout

```
Project NightFall/
  build.sh                 Compile src/ into out/
  release.sh               Compile, embed assets, package ProjectNightfall.jar
  run.sh                   Build if needed, then launch
  run.bat                  Windows fallback launcher (double-click)
  README.md                This file
  DEVELOPER.md             Developer guide
  assets/
    audio/                 Optional WAV sound effects
    images/                Optional per-state background PNGs
  build/                   Compiled classes + ProjectNightfall.jar (release output)
    ProjectNightfall.jar   Self-contained executable JAR
    ProjectNightfall.ico   Windows app icon (from logo)
    ProjectNightfall.icns  macOS app icon (from logo)
    ProjectNightfall.png   Linux app icon (from logo)
  dist/
    macos/                 Project NightFall-1.0.dmg / .pkg / .app
    windows/               Project NightFall.exe (built on Windows)
    linux/                 .deb / .rpm (built on Linux)
  src/nightfall/
    NightfallApp.java      Entry point + CLI handling
    GameWindow.java        JFrame host + fullscreen
    GamePanel.java         Rendering surface + game loop
    GameEngine.java        State machine + world simulation
    GameState.java         Application states
    InputController.java   Mouse/keyboard dispatch
    SelfTest.java          Headless test harness
    entities/              Missile, Interceptor, Explosion,
                           Particle, City, LaunchBattery
    systems/               WaveManager, CollisionSystem
    ui/                    Theme, HUDRenderer, MenuRenderer
    data/                  SettingsManager, HighScoreStore,
                           AudioManager
```

---

## Limitations

- **Session-only persistence.** High scores and settings are not written to
  disk; they reset when you quit.
- **Sound is optional.** If no audio files are present in `assets/audio/`,
  the game runs silently. Audio loading is asynchronous and never blocks play.
- **No online features.** Fully offline by design — no leaderboards, accounts,
  or networking.
- Rendering targets a fixed 1600×900 logical resolution, letterboxed to fit
  your window or display.

---

## License / assets

Gameplay, code, and generated visuals are original to this project. Any audio
files placed in `assets/audio/` are used under whatever terms apply to you.
