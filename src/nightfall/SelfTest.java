// Developer: V.Viswa Vamsi
package nightfall;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import nightfall.data.HighScoreStore;
import nightfall.data.SettingsManager;
import nightfall.entities.Missile;
import nightfall.ui.Theme;

/**
 * Headless self-test harness for PROJECT NIGHTFALL.
 *
 * <p>Runs a battery of deterministic checks against the simulation and the
 * renderer without opening a window or requiring any audio device. Invoked via
 * {@code java -cp output nightfall.NightfallApp --selftest}. Every check prints a
 * {@code [PASS]}/{@code [FAIL]} line and the process exit code reflects the
 * overall result (0 when everything passed).</p>
 */
public final class SelfTest {

    private static int passed;
    private static int failed;

    private SelfTest() {
    }

    /** Entry point; returns the process exit code (0 on success). */
    public static int run() {
        passed = 0;
        failed = 0;

        SettingsManager settings = new SettingsManager();
        HighScoreStore scores = new HighScoreStore();
        GameEngine engine = new GameEngine(settings, scores, null, 12345L);

        testInitialState(engine);
        testNewGame(engine);
        testFire(engine);
        testScoringAndCombo(engine);
        testWaveProgression(engine);
        testVictory(engine);
        testGameOver(engine);
        testPauseFreeze(engine);
        testSettingsAndScores(engine);
        testRender(engine);

        System.out.println("Self-test complete: " + passed + " passed, " + failed + " failed.");
        return failed == 0 ? 0 : 1;
    }

    private static void testInitialState(GameEngine engine) {
        engine.toMenu();
        check(engine.getState() == GameState.MENU, "initial state is MENU");
    }

    private static void testNewGame(GameEngine engine) {
        engine.startNewGame();
        check(engine.getState() == GameState.PLAYING, "startNewGame -> PLAYING");
        check(engine.getCities().size() == 5, "five cities present");
        check(engine.getBatteries().size() == 3, "three batteries present");
        check(engine.totalAmmo() == GameEngine.MAX_AMMO * 3, "all batteries fully supplied");
        check(engine.getScore() == 0, "score starts at zero");
        check(engine.getWave() == 1, "wave starts at one");
    }

    private static void testFire(GameEngine engine) {
        int before = engine.getBatteries().get(0).ammo;
        engine.fireAt(300, 400);
        check(engine.getInterceptors().size() == 1, "fireAt spawns an interceptor");
        check(engine.getBatteries().get(0).ammo == before - 1, "left battery consumed one round");
    }

    private static void testScoringAndCombo(GameEngine engine) {
        int base = engine.getScore();
        engine.debugAddExplosion(500, 300, 120);
        engine.debugSpawnMissileAt(500, 300, Missile.Type.STANDARD);
        engine.update(0.05);
        check(engine.getScore() == base + 25, "first kill scores 25 * wave * x1");
        check(engine.getCombo() == 2, "combo advances to x2 after first kill");

        engine.debugAddExplosion(520, 320, 120);
        engine.debugSpawnMissileAt(520, 320, Missile.Type.STANDARD);
        engine.update(0.05);
        check(engine.getScore() == base + 25 + 50, "second kill scores 25 * wave * x2");
        check(engine.getCombo() == 3, "combo advances to x3");
    }

    private static void testWaveProgression(GameEngine engine) {
        engine.debugClearMissiles();
        engine.debugForceWaveSpawnComplete();
        for (int i = 0; i < 80; i++) {
            engine.update(0.05);
        }
        check(engine.getState() == GameState.PLAYING || engine.getState() == GameState.WAVE_INTERMISSION,
                "wave completes into an intermission");
        for (int i = 0; i < 80; i++) {
            engine.update(0.05);
        }
        check(engine.getState() == GameState.PLAYING, "intermission returns to PLAYING");
        check(engine.getWave() == 2, "wave number advanced to two");
    }

    private static void testVictory(GameEngine engine) {
        engine.debugSetWave(10);
        engine.debugClearMissiles();
        engine.debugForceWaveSpawnComplete();
        for (int i = 0; i < 200; i++) {
            engine.update(0.05);
            if (engine.getState() == GameState.VICTORY) {
                break;
            }
        }
        check(engine.getState() == GameState.VICTORY, "clearing final wave reaches VICTORY");
    }

    private static void testGameOver(GameEngine engine) {
        engine.startNewGame();
        engine.debugKillAllCities();
        engine.update(0.05);
        check(engine.getState() == GameState.GAME_OVER, "losing all cities reaches GAME_OVER");
    }

    private static void testPauseFreeze(GameEngine engine) {
        engine.startNewGame();
        engine.togglePause();
        check(engine.getState() == GameState.PAUSED, "togglePause -> PAUSED");
        int ammo = engine.totalAmmo();
        engine.debugSpawnMissileAt(200, 200, Missile.Type.STANDARD);
        double beforeY = engine.getMissiles().get(0).y;
        engine.update(0.05);
        check(engine.getMissiles().get(0).y == beforeY, "missiles frozen while paused");
        check(engine.totalAmmo() == ammo, "ammo unchanged while paused");
        engine.togglePause();
        check(engine.getState() == GameState.PLAYING, "togglePause -> PLAYING again");
    }

    private static void testSettingsAndScores(GameEngine engine) {
        SettingsManager settings = engine.getSettings();
        settings.sfxOn = !settings.sfxOn;
        check(engine.getSettings().sfxOn == settings.sfxOn, "settings object is retained");

        HighScoreStore scores = engine.getHighScores();
        int before = scores.size();
        scores.add(1234);
        scores.add(9999);
        check(scores.size() == Math.min(before + 2, HighScoreStore.MAX_ENTRIES),
                "high scores accept new entries");
        check(scores.best() == 9999, "best score tracks the maximum");
    }

    private static void testRender(GameEngine engine) {
        engine.startNewGame();
        boolean ok = renderOnce(engine, 0.5, "PLAYING");
        engine.toMenu();
        ok &= renderOnce(engine, 0.5, "MENU");
        check(ok, "renderer draws PLAYING and MENU frames headlessly");
    }

    private static boolean renderOnce(GameEngine engine, double time, String label) {
        BufferedImage img = new BufferedImage(Theme.WIDTH, Theme.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            GamePanel.renderFrame(g, engine, time, 300, 400);
        } catch (Throwable t) {
            System.out.println("[FAIL] render " + label + " threw " + t);
            failed++;
            return false;
        } finally {
            g.dispose();
        }
        return img.getRGB(Theme.WIDTH / 2, Theme.HEIGHT / 2) != 0;
    }

    private static void check(boolean condition, String message) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + message);
        } else {
            failed++;
            System.out.println("[FAIL] " + message);
        }
    }
}
