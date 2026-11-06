// Developer: V.Viswa Vamsi
package nightfall;

import nightfall.data.AudioManager;
import nightfall.data.HighScoreStore;
import nightfall.data.SettingsManager;
import nightfall.entities.City;
import nightfall.entities.Explosion;
import nightfall.entities.Interceptor;
import nightfall.entities.LaunchBattery;
import nightfall.entities.Missile;
import nightfall.entities.Particle;
import nightfall.systems.CollisionSystem;
import nightfall.systems.WaveManager;
import nightfall.ui.Theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core game simulation and state machine. Deliberately free of any Swing
 * dependency so it can run (and be tested) headless. All rendering reads the
 * world through the getters at the bottom of this class.
 */
public class GameEngine {

    // ---- World layout -------------------------------------------------------
    public static final double WORLD_WIDTH = 1600;
    public static final double GROUND_Y = 780;
    public static final int MAX_AMMO = LaunchBattery.MAX_AMMO;
    public static final int MAX_PARTICLES = 1100;

    // ---- Gameplay tuning ----------------------------------------------------
    public static final double INTERCEPTOR_SPEED = 950;
    public static final double INTERMISSION_TIME = 2.6;
    private static final double COMBO_WINDOW = 1.5;
    private static final int COMBO_MAX = 5;
    private static final double SHAKE_DECAY = 2.2;
    private static final double SHAKE_CAP = 2.0;
    private static final double FLASH_DECAY = 3.0;
    private static final double MAX_DT = 0.05;

    private static final double[] CITY_X = {300, 560, 800, 1040, 1300};
    private static final double[] BATTERY_X = {130, 800, 1470};
    private static final long CITY_SEED = 0x5EED5EED5EEDL;

    // ---- World state --------------------------------------------------------
    private final List<Missile> missiles = new ArrayList<>();
    private final List<Interceptor> interceptors = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<City> cities = new ArrayList<>();
    private final List<LaunchBattery> batteries = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    // ---- Session state ------------------------------------------------------
    private GameState state = GameState.MENU;
    private final Random rng;
    private final WaveManager waveManager;
    private final SettingsManager settings;
    private final HighScoreStore highScores;
    private final AudioManager audio;

    private int score = 0;
    private int lastSavedScore = 0;
    private int wave = 1;
    private int combo = 1;
    private double comboTimer = 0;
    private double shake = 0;
    private double impactFlash = 0;
    private double intermissionTimer = 0;
    private double time = 0;

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    public GameEngine(SettingsManager settings, HighScoreStore highScores, AudioManager audio) {
        this(settings, highScores, audio, System.nanoTime());
    }

    public GameEngine(SettingsManager settings, HighScoreStore highScores, AudioManager audio, long seed) {
        this.settings = settings;
        this.highScores = highScores;
        this.audio = audio;
        this.rng = new Random(seed);
        this.waveManager = new WaveManager(rng);
        this.waveManager.setSettings(settings);
    }

    // ------------------------------------------------------------------
    // State transitions
    // ------------------------------------------------------------------

    /** Resets the whole world and starts a fresh run on wave 1. */
    public void startNewGame() {
        clearWorld();
        for (int i = 0; i < CITY_X.length; i++) {
            cities.add(new City(CITY_X[i], GROUND_Y, CITY_SEED + i * 977));
        }
        for (double x : BATTERY_X) {
            batteries.add(new LaunchBattery(x, GROUND_Y));
        }
        score = 0;
        lastSavedScore = 0;
        wave = 1;
        combo = 1;
        comboTimer = 0;
        shake = 0;
        impactFlash = 0;
        intermissionTimer = 0;
        waveManager.setSettings(settings);
        waveManager.beginWave(1);
        state = GameState.PLAYING;
    }

    /** Returns to the main menu, dropping the current run. */
    public void toMenu() {
        clearWorld();
        score = 0;
        wave = 1;
        combo = 1;
        comboTimer = 0;
        shake = 0;
        impactFlash = 0;
        intermissionTimer = 0;
        state = GameState.MENU;
        audio(AudioManager.SFX_MENU_OPEN);
    }

    /** PLAYING <-> PAUSED toggle; ignored in any other state. */
    public void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    // ------------------------------------------------------------------
    // Main loop
    // ------------------------------------------------------------------

    /** Advances the simulation by {@code dt} seconds (clamped for stability). */
    public void update(double dt) {
        if (dt <= 0) {
            return;
        }
        if (dt > MAX_DT) {
            dt = MAX_DT;
        }
        time += dt;

        switch (state) {
            case PLAYING -> updatePlaying(dt);
            case WAVE_INTERMISSION -> updateIntermission(dt);
            default -> { /* menu / paused / terminal states: world stays as is */ }
        }
        if (state == GameState.PAUSED) {
            return; // a true freeze: no effects, no decay
        }

        updateExplosions(dt);
        updateParticles(dt);
        updateFloatingTexts(dt);
        cleanup();
        decayEffects(dt);
    }

    private void updatePlaying(double dt) {
        waveManager.update(dt, this);
        updateMissiles(dt);
        updateInterceptors(dt);
        resolveCollisions();
        updateCitiesAndBatteries(dt);
        updateCombo(dt);
        cleanup();

        // Losing every city outranks a wave clear so the run ends immediately.
        if (aliveCities() == 0) {
            gameOver();
            return;
        }
        if (waveManager.waveCleared(this)) {
            if (wave >= WaveManager.FINAL_WAVE) {
                victory();
            } else {
                beginIntermission();
            }
        }
    }

    private void updateIntermission(double dt) {
        updateCombo(dt);
        intermissionTimer -= dt;
        if (intermissionTimer <= 0) {
            wave++;
            waveManager.beginWave(wave);
            state = GameState.PLAYING;
        }
    }

    // ------------------------------------------------------------------
    // Entities
    // ------------------------------------------------------------------

    private void updateMissiles(double dt) {
        // Descending walk: SPLITTER children are appended and skip this frame.
        for (int i = missiles.size() - 1; i >= 0; i--) {
            Missile m = missiles.get(i);
            m.update(dt);
            if (m.type == Missile.Type.SPLITTER && !m.hasSplit && m.y >= m.midY()) {
                m.hasSplit = true;
                split(m);
            }
            if (m.reachedGround && !m.destroyed) {
                impact(m);
            }
        }
    }

    /** Releases a 2-3 warhead MIRV cluster spread +-60..180px around the target. */
    private void split(Missile parent) {
        int children = 2 + rng.nextInt(2);
        for (int i = 0; i < children; i++) {
            double spread = 60 + rng.nextDouble() * 120;
            double dir;
            if (children == 3 && i == 2) {
                dir = rng.nextBoolean() ? 1 : -1;
            } else {
                dir = (i % 2 == 0) ? 1 : -1;
            }
            double jitter = rng.nextDouble() * 40 - 20;
            double tx = clamp(parent.targetX + dir * spread + jitter, 20, WORLD_WIDTH - 20);
            missiles.add(new Missile(parent.x, parent.y, tx, GROUND_Y,
                    parent.speed * 1.05, Missile.Type.STANDARD));
        }
        emitBurst(parent.x, parent.y, 10, parent.color, 90, 0.5);
    }

    /** A warhead landing: city first, then battery, then open ground. */
    private void impact(Missile m) {
        double ix = m.x;
        audio(AudioManager.SFX_EXPLOSION);

        for (City c : cities) {
            if (c.alive && c.containsX(ix)) {
                c.hit();
                explosions.add(new Explosion(ix, GROUND_Y, 90, 0.7, Theme.RED, true));
                emitBurst(ix, GROUND_Y, 18, Theme.RED, 170, 0.7);
                addShake(1.0);
                impactFlash = 1.0;
                return;
            }
        }
        for (LaunchBattery b : batteries) {
            if (b.alive && b.containsX(ix)) {
                b.kill();
                explosions.add(new Explosion(ix, GROUND_Y, 90, 0.7, Theme.RED, true));
                emitBurst(ix, GROUND_Y, 18, Theme.RED, 170, 0.7);
                addShake(0.8);
                impactFlash = 1.0;
                return;
            }
        }
        explosions.add(new Explosion(ix, GROUND_Y, 90, 0.7, Theme.AMBER, true));
        emitBurst(ix, GROUND_Y, 14, Theme.AMBER, 150, 0.6);
        addShake(0.3);
        impactFlash = Math.max(impactFlash, 0.5);
    }

    private void updateInterceptors(double dt) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            Interceptor it = interceptors.get(i);
            it.update(dt);
            if (it.detonated) {
                interceptors.remove(i);
                explosions.add(new Explosion(it.x, it.y, 120, 0.85, Theme.CYAN, false));
                emitBurst(it.x, it.y, 12, Theme.CYAN, 140, 0.6);
                audio(AudioManager.SFX_EXPLOSION);
            }
        }
    }

    /** Warhead vs blast zone; kills award score and spawn visual chain blasts. */
    private void resolveCollisions() {
        for (int i = missiles.size() - 1; i >= 0; i--) {
            Missile m = missiles.get(i);
            if (!m.alive) {
                continue;
            }
            for (int j = explosions.size() - 1; j >= 0; j--) {
                if (CollisionSystem.hits(m, explosions.get(j))) {
                    m.kill();
                    onKill(m);
                    break;
                }
            }
        }
    }

    private void onKill(Missile m) {
        int gain = 25 * wave * combo;
        score += gain;
        floatingTexts.add(new FloatingText(m.x, m.y, "+" + gain, Theme.GREEN));
        comboTimer = COMBO_WINDOW;
        combo = Math.min(combo + 1, COMBO_MAX);
        explosions.add(new Explosion(m.x, m.y, 55, 0.5, Theme.AMBER, true));
        emitBurst(m.x, m.y, 14, m.color, 150, 0.6);
        audio(AudioManager.SFX_EXPLOSION);
    }

    private void updateCitiesAndBatteries(double dt) {
        for (City c : cities) {
            c.update(dt);
        }
        for (LaunchBattery b : batteries) {
            b.update(dt);
        }
    }

    private void updateCombo(double dt) {
        if (combo > 1) {
            comboTimer -= dt;
            if (comboTimer <= 0) {
                combo = 1;
                comboTimer = 0;
            }
        }
    }

    private void updateExplosions(double dt) {
        for (Explosion e : explosions) {
            e.update(dt);
        }
    }

    private void updateParticles(double dt) {
        for (Particle p : particles) {
            p.update(dt);
        }
    }

    private void updateFloatingTexts(double dt) {
        for (FloatingText f : floatingTexts) {
            f.update(dt);
        }
    }

    private void cleanup() {
        missiles.removeIf(m -> !m.alive);
        explosions.removeIf(e -> !e.alive);
        particles.removeIf(p -> !p.alive());
        floatingTexts.removeIf(f -> f.life <= 0);
        interceptors.removeIf(it -> !it.alive && !it.detonated);
    }

    private void decayEffects(double dt) {
        shake = Math.max(0, shake - SHAKE_DECAY * dt);
        impactFlash = Math.max(0, impactFlash - FLASH_DECAY * dt);
    }

    // ------------------------------------------------------------------
    // Flow helpers
    // ------------------------------------------------------------------

    private void beginIntermission() {
        state = GameState.WAVE_INTERMISSION;
        intermissionTimer = INTERMISSION_TIME;
        score += 100 * aliveCities() + 5 * totalAmmo();
        resupply();
        audio(AudioManager.SFX_WAVE);
    }

    private void victory() {
        state = GameState.VICTORY;
        saveScore();
        audio(AudioManager.SFX_VICTORY);
    }

    private void gameOver() {
        state = GameState.GAME_OVER;
        saveScore();
        audio(AudioManager.SFX_GAME_OVER);
    }

    private void saveScore() {
        if (score > lastSavedScore) {
            highScores.add(score);
            lastSavedScore = score;
        }
    }

    private void resupply() {
        for (LaunchBattery b : batteries) {
            if (b.alive) {
                b.resupply();
            }
        }
    }

    private void clearWorld() {
        missiles.clear();
        interceptors.clear();
        explosions.clear();
        particles.clear();
        cities.clear();
        batteries.clear();
        floatingTexts.clear();
    }

    // ------------------------------------------------------------------
    // Player actions
    // ------------------------------------------------------------------

    /** Fires an interceptor at the given logical coordinate. True on success. */
    public boolean fireAt(double x, double y) {
        if (state != GameState.PLAYING) {
            return false;
        }
        LaunchBattery battery = chooseBattery(x);
        if (battery == null) {
            return false;
        }
        double tx = clamp(x, 20, WORLD_WIDTH - 20);
        double ty = clamp(y, 20, GROUND_Y - 10);
        interceptors.add(new Interceptor(battery.x, battery.y - 26, tx, ty, INTERCEPTOR_SPEED));
        battery.fire();
        audio(AudioManager.SFX_LAUNCH);
        return true;
    }

    /** Picks the battery whose third of the screen the cursor is in, else the nearest ready one. */
    private LaunchBattery chooseBattery(double x) {
        int region = (int) (x / (WORLD_WIDTH / 3));
        region = Math.max(0, Math.min(batteries.size() - 1, region));

        if (region >= 0 && region < batteries.size() && batteries.get(region).canFire()) {
            return batteries.get(region);
        }
        LaunchBattery best = null;
        double bestDist = Double.MAX_VALUE;
        for (LaunchBattery b : batteries) {
            if (b.canFire()) {
                double d = Math.abs(b.x - x);
                if (d < bestDist) {
                    bestDist = d;
                    best = b;
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Effects
    // ------------------------------------------------------------------

    private void emitBurst(double x, double y, int count, Color color, double speed, double life) {
        if (settings != null && !settings.particlesOn) {
            return;
        }
        for (int i = 0; i < count; i++) {
            if (particles.size() >= MAX_PARTICLES) {
                return;
            }
            double angle = rng.nextDouble() * Math.PI * 2;
            double v = speed * (0.4 + 0.6 * rng.nextDouble());
            particles.add(new Particle(
                    x, y,
                    Math.cos(angle) * v, Math.sin(angle) * v,
                    life * (0.6 + 0.4 * rng.nextDouble()),
                    color,
                    2 + rng.nextDouble() * 2));
        }
    }

    private void addShake(double amount) {
        if (settings != null && settings.screenShakeOn) {
            shake = Math.min(SHAKE_CAP, shake + amount);
        }
    }

    private void audio(String key) {
        if (audio != null) {
            audio.play(key);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ------------------------------------------------------------------
    // Spawning (used by WaveManager and tests)
    // ------------------------------------------------------------------

    public Missile spawnMissile(double startX, double startY, double targetX, double targetY,
                                double speed, Missile.Type type) {
        Missile m = new Missile(startX, startY, targetX, targetY, speed, type);
        missiles.add(m);
        return m;
    }

    // ------------------------------------------------------------------
    // Debug / self-test hooks
    // ------------------------------------------------------------------

    public void debugSpawnInterceptorAt(double x, double y) {
        interceptors.add(new Interceptor(x, y, x, y, 0));
    }

    public void debugAddExplosion(double x, double y, double radius) {
        Explosion e = new Explosion(x, y, radius, 5.0, Theme.CYAN, false);
        e.life = e.maxLife * 0.5; // progress 0.5 -> already at full radius
        explosions.add(e);
    }

    public Missile debugSpawnMissileAt(double x, double y, Missile.Type type) {
        return spawnMissile(x, y, x, GROUND_Y, 0, type);
    }

    public void debugClearMissiles() {
        missiles.clear();
    }

    public void debugKillAllCities() {
        for (City c : cities) {
            c.hit();
        }
    }

    public void debugForceWaveSpawnComplete() {
        waveManager.forceComplete();
    }

    public void debugSetWave(int n) {
        wave = Math.max(1, n);
        waveManager.beginWave(wave);
        waveManager.forceComplete();
    }

    // ------------------------------------------------------------------
    // Read access for renderers
    // ------------------------------------------------------------------

    public List<Missile> getMissiles() {
        return missiles;
    }

    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public List<Explosion> getExplosions() {
        return explosions;
    }

    public List<Particle> getParticles() {
        return particles;
    }

    public List<City> getCities() {
        return cities;
    }

    public List<LaunchBattery> getBatteries() {
        return batteries;
    }

    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public int getScore() {
        return score;
    }

    public int getWave() {
        return wave;
    }

    public int getCombo() {
        return combo;
    }

    /** 0..1 remaining life of the current combo window (for the HUD bar). */
    public double comboFraction() {
        return Math.max(0, Math.min(1, comboTimer / COMBO_WINDOW));
    }

    public double getShake() {
        return shake;
    }

    public double getImpactFlash() {
        return impactFlash;
    }

    public int aliveCities() {
        int n = 0;
        for (City c : cities) {
            if (c.alive) {
                n++;
            }
        }
        return n;
    }

    public int totalAmmo() {
        int n = 0;
        for (LaunchBattery b : batteries) {
            if (b.alive) {
                n += b.ammo;
            }
        }
        return n;
    }

    /** Warheads still in the sky plus those queued to spawn (HUD threat meter). */
    public int getThreat() {
        return missiles.size() + waveManager.remaining();
    }

    public int getBest() {
        return highScores.best();
    }

    public double getTime() {
        return time;
    }

    public double getIntermissionTimer() {
        return intermissionTimer;
    }

    public SettingsManager getSettings() {
        return settings;
    }

    public AudioManager getAudio() {
        return audio;
    }

    public HighScoreStore getHighScores() {
        return highScores;
    }

    // ------------------------------------------------------------------
    // Floating score text
    // ------------------------------------------------------------------

    /** A short lived "+points" label that drifts upward and fades. */
    public static class FloatingText {
        public double x;
        public double y;
        public double life;
        public final double maxLife;
        public final String text;
        public final Color color;

        public FloatingText(double x, double y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.maxLife = 1.0;
            this.life = 1.0;
        }

        public void update(double dt) {
            y -= 40 * dt;
            life -= dt;
        }

        public double alpha() {
            if (maxLife <= 0) {
                return 0;
            }
            return Math.max(0, Math.min(1, life / maxLife));
        }
    }
}
