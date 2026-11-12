# PROJECT NIGHTFALL — Developer Guide

**Developer: V.Viswa Vamsi**

This document explains how every source file interconnects, how each component
works, the technical approach behind the game, and which sounds are used.

---

## 1. High-Level Architecture

PROJECT NIGHTFALL is a classic missile-command–style arcade defense game built
entirely with standard Java (Swing / AWT / Java2D). No external libraries, no
build tools beyond `javac`, no network access.

The design splits cleanly into three layers:

```
┌─────────────────────────────────────────────────────────────┐
│  WINDOW / INPUT LAYER (Swing)                               │
│  NightfallApp → GameWindow → GamePanel → InputController    │
└───────────────────────────┬─────────────────────────────────┘
                            │ fixed-step update + draw each frame
┌───────────────────────────▼─────────────────────────────────┐
│  SIMULATION CORE (pure Java, no Swing — headless-safe)      │
│  GameEngine (state machine + world)                         │
│    ├── systems/WaveManager     (enemy spawn scheduling)     │
│    ├── systems/CollisionSystem (blast-zone hit tests)       │
│    ├── entities/*              (Missile, Interceptor, …)    │
│    └── data/*                  (Settings, Scores, Audio)    │
└───────────────────────────┬─────────────────────────────────┘
                            │ read-only state for rendering
┌───────────────────────────▼─────────────────────────────────┐
│  RENDERING LAYER (static classes, no instance state)        │
│  ui/Theme (logical 1600×900 canvas)                         │
│  ui/Backgrounds → ui/HUDRenderer → ui/MenuRenderer          │
└─────────────────────────────────────────────────────────────┘
```

**Key principle:** `GameEngine` never imports Swing. This is what makes the
26-check headless self-test suite possible (`./run.sh --selftest`). Rendering
reads engine state through public getters; the engine never knows about pixels.

---

## 2. Boot & Main Loop

### NightfallApp (entry point)
- Parses CLI flags: `--selftest` (runs `SelfTest.run()`, exits with pass/fail
  code) and `--frames N` (renders N headless frames to
  `nightfall_frames.png`, then exits).
- Normal launch builds `SettingsManager`, `HighScoreStore`, and
  `AudioManager` on the EDT, constructs a `GameEngine`, calls
  `audio.loadAsync()` (daemon thread preload of WAVs), then opens a
  `GameWindow`.

### GameWindow (JFrame host)
- Undecorated-capable `JFrame` titled "PROJECT NIGHTFALL";
  `EXIT_ON_CLOSE`; resizable; packs and centers.
- Wires the panel's fullscreen toggle (`F11`) and quit action (window close).
- On `windowClosing` it shuts down the panel's game loop cleanly.

### GamePanel (rendering surface + loop)
- Extends `JPanel`; double-buffered; logical size fixed at 1600×900
  (`Theme.WIDTH/HEIGHT`).
- A Swing `Timer` drives a fixed-step loop: it accumulates wall-clock time and
  calls `GameEngine.update(dt)` in fixed increments (capped at 50 ms of
  catch-up per frame to avoid spiral-of-death), then repaints.
- **Letterboxing:** the 1600×900 logical canvas is scaled to fit the actual
  panel size, preserving aspect ratio. The scale and offsets are pushed to
  `InputController.setViewport(scale, offX, offY)` every frame so mouse
  coordinates convert correctly, and the `Graphics2D` transform is applied
  before drawing.
- **Key bindings** (via `InputMap`/`ActionMap`, not raw key events):
  - `P` / `Escape` → pause toggle
  - `R` → restart
  - `Enter` / `Space` → confirm (also plays the UI click sound)
  - `F11` → fullscreen toggle
- **Static `renderFrame(Graphics2D, GameEngine, time, mouseX, mouseY)`** is
  used both by the live panel and by headless tests/`--frames` rendering.
  Draw order: `Backgrounds` → (world) `HUDRenderer` → (overlays)
  `MenuRenderer`.

### InputController (mouse + keyboard dispatch)
- Implements `MouseListener`/`MouseMotionListener`; stores logical mouse
  coordinates (window pixels converted through the viewport transform).
- `click()` is the single funnel for "activate whatever is under the cursor":
  in `PLAYING` it calls `engine.fireAt(x, y)`; in menu/pause/over states it
  hit-tests the static button rectangles exposed by `MenuRenderer` and runs
  the matching transition (`startNewGame`, `resume`, `restart`, `quit`, …).
- `handleKey(int)` maps the bound keys to engine/menu actions; the `P`/`Esc`
  path always plays a click sound before toggling pause.
- A `setQuitAction(Runnable)` hook lets `GameWindow` close the frame when the
  player chooses Quit.

---

## 3. Simulation Core

### GameEngine (state machine + world)
Owns the entire mutable world and the nine-state application flow:

| State                | Meaning                                          |
|----------------------|--------------------------------------------------|
| `MENU`               | Title screen with Play / How to Play / Quit      |
| `HOW_TO_PLAY`        | Instructions overlay                             |
| `SETTINGS`           | Sound / volume / difficulty controls             |
| `PLAYING`            | Active wave, live simulation                     |
| `PAUSED`             | Simulation frozen; resume/restart/menu buttons   |
| `WAVE_INTERMISSION`  | 2.6 s breather; resupply + bonus score           |
| `GAME_OVER`          | All cities lost                                  |
| `VICTORY`            | Final wave cleared; choose PLAY AGAIN or Main Menu |
| `HIGH_SCORES`        | Top-10 table                                     |

Notable constants: `WORLD_WIDTH = 1600`, `GROUND_Y = 780`, `MAX_AMMO = 10`
(per battery), `INTERCEPTOR_SPEED = 950`, `COMBO_WINDOW = 1.5 s`,
`COMBO_MAX = 5`, `INTERMISSION_TIME = 2.6 s`, particle cap 1100.

Gameplay rules:
- Five cities at fixed X positions; three launch batteries (`130, 800, 1470`).
- Enemy missiles fall from off-screen top toward a target X on the ground.
  When a missile reaches the ground, `impact()` resolves city hit → battery
  hit → open ground (in that priority order), spawning an explosion, particles,
  screen shake, and an impact flash.
- The player fires interceptors (`fireAt`); the playfield is split into three
  vertical thirds, each normally served by the battery in that third, with a
  nearest-ready fallback. Interceptors detonate at the cursor point, creating a
  blast zone.
- `resolveCollisions()` (via `CollisionSystem.hits`) kills any live warhead
  whose position falls inside an active explosion. Each kill scores
  `25 × wave × combo`, advances the combo (capped at ×5, decaying after 1.5 s
  without a kill), and spawns a visual chain blast + floating "+points" text.
- A wave is cleared when the spawn schedule is finished and the sky is empty;
  the engine then enters `WAVE_INTERMISSION`, awards `100 × alive cities + 5 ×
  remaining ammo` bonus, and resupplies all live batteries.
- After wave 10 is cleared, the engine enters `VICTORY`. The victory screen
  offers **PLAY AGAIN** (restarts `startNewGame()` on Wave 1) and **MAIN MENU**
  (`toMenu()`). The endless continuation feature has been removed.
- Losing all cities triggers `GAME_OVER`. Both end states persist the score
  through `HighScoreStore` (once per run).
- Audio hooks: menu-open, click (via InputController), launch, explosion/kill,
  wave, victory, game-over (see §6).
- A private `audio(String)` helper is null-safe so headless tests can pass
  `audio = null`.

### WaveManager (spawn scheduling)
- `beginWave(n)` computes the schedule: `5 + 2n` warheads, spawn interval
  `max(0.22, 1.0 − 0.06n)` seconds with ±jitter, first-wave delay 0.6 s
  (later waves 0.9 s).
- Enemy speed rises with wave (`min(250, 70 + 7×wave)`) scaled by the selected
  difficulty (`NOOB 0.45× / PRO 1.0× / GOD 2.0×`).
- Type mix escalates: FAST warheads appear from wave 2 (chance up to 38 %),
  SPLITTERs from wave 3 (chance up to 30 %).
- Targeting is ~70 % biased toward a still-live city or battery, the rest
  random across the skyline, so the whole front stays dangerous.
- `waveCleared(engine)` is true when spawning is done and no missiles remain —
  this is what advances the engine through intermission.

### CollisionSystem
- Single static utility: `hits(Missile, Explosion)` — a point-in-circle test
  of the missile center against the explosion's current animated radius.
  Ignores dead and already-detonated (chain) missiles.

### Entities (mutable world objects, package `entities`)
| Entity          | Role / notable behaviour                                        |
|-----------------|-----------------------------------------------------------------|
| `Missile`       | Enemy warhead. Types STANDARD / FAST / SPLITTER; precomputed fall vector; SPLITTER breaks into STANDARD children mid-flight; leaves a trail. |
| `Interceptor`   | Player projectile. Constant-speed straight flight to a fixed target point; detonates on arrival (flag consumed by engine). |
| `Explosion`     | Expanding blast zone. Radius grows to 40 % of max, holds to 80 %, then fades; `chain` marks visual-only blasts. |
| `City`          | Destructible skyline segment. Deterministic procedural skyline (LCG-seeded); `hit()` starts a burn-flash; `containsX` for impact tests. |
| `LaunchBattery` | Player turret. 10-round magazine, 0.14 s cooldown, `resupply()` between waves, killable by impacts. |
| `Particle`      | Lightweight debris/spark with drag + gravity; capped count for performance. |

### Data layer (package `data`)
- **SettingsManager** — in-memory session settings: SFX on/off, master volume,
  particle toggle, screen-shake toggle, difficulty enum (`NOOB`, `PRO`,
  `GOD`). (Persistence is a planned later milestone.)
- **HighScoreStore** — top-10 int table, sorted best-first; `add/best/size`.
- **AudioManager** — see §6.

---

## 4. Rendering Layer

All rendering is stateless static code in package `ui`, driven every frame
after the engine update.

### Theme
Single source of truth for the logical layout: 1600×900 canvas, `GROUND_Y`,
the full colour palette (VOID/DEEP backgrounds, CYAN/AMBER/RED/GREEN accents,
PANEL surfaces), and the font set (TITLE 96, H1 44, …). Every renderer pulls
colours and fonts from here — nothing hard-codes a hex value or point size
elsewhere.

### Backgrounds
Per-state backdrop painter. Lazily loads one PNG per `GameState` from
`assets/images` (classpath, then working directory), cover-fits it into the
1600×900 canvas, and applies a state-specific dimming scrim so UI text stays
legible. If an image is missing it returns `false` and the caller falls back
to the procedural night sky (gradient + horizon glow + starfield).

### HUDRenderer
Draws the in-game world and tactical HUD:
- Deterministic starfield (seeded `Random`) and a background building strip
  behind the play area.
- Cities (with damage flash), batteries (ammo bars, cooldown state), missile
  trails, interceptor trails, explosions, particles, and floating score text.
- HUD chrome: score, best score, wave counter, city-status icons, threat meter
  (live warheads + still-queued spawns), **difficulty mode** (NOOB/PRO/GOD),
  combo bar with timer fraction, the developer credit, and a clickable **PAUSE**
  button in the top-right corner (only during `PLAYING`).

### MenuRenderer
Draws the non-gameplay overlays and owns the shared button geometry used by
`InputController` for hit-testing (static rects for MENU, PAUSED, GAME_OVER,
VICTORY screens). Buttons are hover-aware (cursor position passed into the
render call). The title/menu screens also display a NOOB/PRO/GOD difficulty
selector row below Play/Quit, and every gameplay overlay (pause, game-over,
victory) shows the current `MODE: X` label. The developer credit appears at the
bottom of every overlay.

The victory screen uses **PLAY AGAIN** (maps to `startNewGame()`) instead of an
endless continuation.

Draw order inside `GamePanel.renderFrame`: background → world+HUD → overlay.
The overlay only draws in non-`PLAYING` states (or during intermission).

---

## 5. Technical Approach

- **Fixed logical resolution, letterboxed rendering.** All simulation and UI
  math happens in a fixed 1600×900 space; the panel scales this to any window
  size. Mouse input is inverse-transformed through the same scale/offset, so
  aiming is pixel-accurate at any window size or in fullscreen.
- **Headless-safe core.** `GameEngine` and all simulation packages avoid AWT
  windowing APIs (only `java.awt.Color` is used). This enables:
  - `./run.sh --selftest` — 26 deterministic checks (state transitions,
    firing, scoring/combo maths, wave progression, victory, game-over, pause
    freeze, settings retention, headless render smoke-test). Exit code 0 on
    full pass.
  - `./run.sh --frames N` — offscreen frame capture for debugging.
- **Static rendering.** Renderers are stateless static classes; all mutable
  state lives in the engine or entities. This keeps frame drawing free of
  lifecycle bugs and makes headless rendering trivial.
- **Fixed-step loop with capped catch-up.** The Swing timer accumulates real
  time and steps the simulation in fixed increments, capped so a long stall
  cannot freeze the UI or explode the step size.
- **Input via ActionMap.** Key handling uses Swing `InputMap`/`ActionMap`
  bindings rather than raw `keyPressed` dispatch, avoiding IME/repeat issues.
- **Graceful degradation.** Missing background images, missing sounds, or a
  headless environment never throw; the game simply falls back (procedural
  background, silent audio) or skips the subsystem.
- **No external dependencies.** Standard JDK only (developed on JDK 21+;
  tested through JDK 26). `build.sh` is a thin `javac` wrapper; `run.sh`
  compiles if needed and forwards CLI args.

---

## 6. Sound Design

All sound effects are WAV files loaded by `AudioManager` from
`assets/audio/` (classpath first, then filesystem). Loading is asynchronous
on a daemon thread (`loadAsync()`); playback opens a short-lived `Clip` per
sound and closes it on stop. Master gain follows `SettingsManager.volume`.
Every failure is silent — audio is decorative and never crashes the loop.
Headless environments skip audio entirely.

| Key constant            | File                        | Trigger                                        |
|-------------------------|-----------------------------|------------------------------------------------|
| `SFX_CLICK`             | `button_click.wav`          | Any UI button press (menu, pause, confirm keys) |
| `SFX_MENU_OPEN`         | `main_page_opening.wav`     | Returning to the main menu (`toMenu`)           |
| `SFX_LAUNCH`            | `laser_gun_shot.wav`        | Player interceptor launch (`fireAt`)            |
| `SFX_EXPLOSION`         | `blast.wav`                 | Missile kills, interceptor detonation, impacts  |
| `SFX_WAVE`              | `wave.wav`                  | Wave-intermission start (file optional)         |
| `SFX_VICTORY`           | `victory.wav`               | Victory screen reached                          |
| `SFX_GAME_OVER`         | `game_over.wav`             | Game-over screen reached                        |

Notes:
- The fullscreen toggle (`F11`) is deliberately silent — it is a window
  operation, not a game button.
- The wave-intermission sound stays silent when `wave.wav` is absent; the
  game runs fine without it.

---

## 7. Project Layout

```
Project NightFall/
  DEVELOPER.md             This file
  README.md                Player-facing documentation
  build.sh                 Compile src/ into output/
  run.sh                   Build if needed, then launch (forwards args)
  assets/
    audio/                 WAV sound effects (see §6)
    images/                Optional per-state background PNGs
  src/nightfall/
    NightfallApp.java      Entry point + CLI handling
    GameWindow.java        JFrame host + fullscreen
    GamePanel.java         Fixed-step loop, letterboxing, key bindings
    GameEngine.java        State machine + world simulation
    GameState.java         Nine application states
    InputController.java   Mouse/keyboard dispatch + button hit-testing
    SelfTest.java          Headless test harness (26 checks)
    entities/              Missile, Interceptor, Explosion, Particle,
                           City, LaunchBattery
    systems/               WaveManager (spawning), CollisionSystem (hits)
    ui/                    Theme (palette/fonts), Backgrounds, HUDRenderer,
                           MenuRenderer (buttons + overlays)
    data/                  SettingsManager, HighScoreStore, AudioManager
```

---

## 8. Standalone App Packaging

Project NightFall can be distributed as self-contained native installers that
bundle the JRE, so end users do **not** need Java installed. The pipeline is:

1. `./release.sh` — compiles all Java sources, embeds audio + image assets on the
   classpath, and packages `build/ProjectNightfall.jar` (executable, double-clickable
   on any machine with Java).
2. `jpackage` (bundled with the JDK) — builds native installers per target OS:

| Platform | Output | Command (run on target OS) |
|----------|--------|---------------------------|
| macOS | `.dmg`, `.pkg`, `.app` | `jpackage --input build --main-jar ProjectNightfall.jar --name "Project NightFall" --app-version 1.0 --icon build/ProjectNightfall.icns --type dmg --dist dist/macos` |
| Windows | `.exe` | `jpackage --input build --main-jar ProjectNightfall.jar --name "Project NightFall" --app-version 1.0 --icon build/ProjectNightfall.ico --type exe --dist dist/windows` |
| Linux | `.deb`, `.rpm` | `jpackage --input build --main-jar ProjectNightfall.jar --name "Project NightFall" --app-version 1.0 --icon build/ProjectNightfall.png --type deb --dist dist/linux` |

App icons are derived from `Project Nightfall Assets/Project Nightfall logo.png`
and live in `build/` (`ProjectNightfall.ico`, `ProjectNightfall.icns`,
`ProjectNightfall.png`).

Fallback launch scripts for machines that have Java but no installer:

- `./run.sh` — macOS / Linux
- `run.bat` — Windows (double-click)

The JAR embeds all 6 WAV sound effects and 5 background PNGs, so the installed
app is fully self-contained — no external `assets/` folder is required.

---

## 9. Controls Reference

| Action            | Input                 |
|-------------------|-----------------------|
| Aim               | Mouse                 |
| Fire interceptor  | Left mouse button     |
| Pause / Resume    | `P` or `Esc`          |
| Restart           | `R`                   |
| Confirm / Select  | `Enter` or `Space`    |
| Toggle fullscreen | `F11`                 |

---

*PROJECT NIGHTFALL — developed by V.Viswa Vamsi.*
