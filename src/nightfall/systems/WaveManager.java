// Developer: V.Viswa Vamsi
package nightfall.systems;

import nightfall.GameEngine;
import nightfall.data.SettingsManager;
import nightfall.entities.LaunchBattery;
import nightfall.entities.Missile;

import java.util.List;
import java.util.Random;

/**
 * Spawns enemy warheads for a wave: how many, how fast, how often and of which
 * types. Difficulty and enemy variety both escalate with the wave number.
 * The final story wave is {@link #FINAL_WAVE}.
 */
public class WaveManager {

    public static final int FINAL_WAVE = 10;

    private final Random rng;

    private int wave = 1;
    private int toSpawn = 0;
    private int spawned = 0;
    private double spawnTimer = 0.6;
    private double spawnInterval = 1.0;
    private boolean spawnComplete = true;

    public WaveManager(Random rng) {
        this.rng = rng;
    }

    /** Prepares the spawn schedule for a wave number. */
    public void beginWave(int wave) {
        this.wave = Math.max(1, wave);
        this.toSpawn = 5 + this.wave * 2;
        this.spawned = 0;
        this.spawnInterval = Math.max(0.22, 1.0 - this.wave * 0.06);
        this.spawnTimer = this.wave == 1 ? 0.6 : 0.9;
        this.spawnComplete = false;
    }

    /** Advances the spawn schedule and emits missiles into the engine. */
    public void update(double dt, GameEngine engine) {
        if (spawnComplete) {
            return;
        }
        spawnTimer -= dt;
        if (spawnTimer > 0) {
            return;
        }
        if (spawned >= toSpawn) {
            spawnComplete = true;
            return;
        }
        spawn(engine);
        spawned++;
        spawnTimer = spawnInterval * (0.7 + 0.6 * rng.nextDouble());
    }

    /** True once everything for this wave has spawned and the sky is clear. */
    public boolean waveCleared(GameEngine engine) {
        return spawnComplete && engine.getMissiles().isEmpty();
    }

    /** Marks the spawn schedule as finished (used by tests and debug hooks). */
    public void forceComplete() {
        spawnComplete = true;
    }

    public int wave() {
        return wave;
    }

    public int spawnedCount() {
        return spawned;
    }

    public int remaining() {
        return Math.max(0, toSpawn - spawned);
    }

    public boolean isSpawnComplete() {
        return spawnComplete;
    }

    // ------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------

    private void spawn(GameEngine engine) {
        double speed = chooseSpeed();
        Missile.Type type = chooseType();

        double startX = 60 + rng.nextDouble() * (GameEngine.WORLD_WIDTH - 120);
        double targetX = chooseTargetX(engine);

        engine.spawnMissile(startX, -20, targetX, GameEngine.GROUND_Y, speed, type);
    }

    /** Base fall speed, scaled by the selected difficulty preset. */
    private double chooseSpeed() {
        double base = Math.min(250, 70 + wave * 7);
        SettingsManager settings = engineSettings();
        if (settings != null && settings.difficulty != null) {
            base *= settings.difficulty.enemySpeedScale();
        }
        return base;
    }

    private Missile.Type chooseType() {
        if (wave >= 3) {
            double splitterChance = Math.min(0.30, (wave - 2) * 0.07);
            if (rng.nextDouble() < splitterChance) {
                return Missile.Type.SPLITTER;
            }
        }
        if (wave >= 2) {
            double fastChance = Math.min(0.38, (wave - 1) * 0.06);
            if (rng.nextDouble() < fastChance) {
                return Missile.Type.FAST;
            }
        }
        return Missile.Type.STANDARD;
    }

    /**
     * About 70% of warheads aim at something that still exists (a live city or
     * battery); the rest land randomly to keep the whole skyline dangerous.
     */
    private double chooseTargetX(GameEngine engine) {
        boolean biasTarget = rng.nextDouble() < 0.7;

        double[] options = new double[8];
        int n = 0;

        if (biasTarget) {
            for (int i = 0; i < engine.getCities().size() && n < options.length; i++) {
                if (engine.getCities().get(i).alive) {
                    options[n++] = engine.getCities().get(i).x;
                }
            }
            List<LaunchBattery> batteries = engine.getBatteries();
            for (int i = 0; i < batteries.size() && n < options.length; i++) {
                if (batteries.get(i).alive) {
                    options[n++] = batteries.get(i).x;
                }
            }
        }

        if (n == 0) {
            return 60 + rng.nextDouble() * (GameEngine.WORLD_WIDTH - 120);
        }
        return options[rng.nextInt(n)];
    }

    /** The live settings object is owned by the engine; may be null in tests. */
    private SettingsManager engineSettings() {
        return settingsRef;
    }

    /** Settings are handed to the wave manager after construction. */
    private SettingsManager settingsRef;

    public void setSettings(SettingsManager settings) {
        this.settingsRef = settings;
    }
}
