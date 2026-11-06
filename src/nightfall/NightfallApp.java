// Developer: V.Viswa Vamsi
package nightfall;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import nightfall.data.AudioManager;
import nightfall.data.HighScoreStore;
import nightfall.data.SettingsManager;
import nightfall.ui.Theme;

/**
 * Entry point for PROJECT NIGHTFALL.
 *
 * <p>Running with no arguments boots the Swing window. The headless flags let
 * CI/verification render frames without a display:
 * <ul>
 *   <li>{@code --selftest}   runs the deterministic asset-free self test</li>
 *   <li>{@code --frames N}   renders N headless frames to a PNG and exits</li>
 * </ul>
 */
public final class NightfallApp {

    private NightfallApp() {
    }

    public static void main(String[] args) {
        boolean selftest = false;
        int frames = 0;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--selftest".equals(a)) {
                selftest = true;
            } else if ("--frames".equals(a) && i + 1 < args.length) {
                try {
                    frames = Math.max(0, Integer.parseInt(args[i + 1]));
                } catch (NumberFormatException ignored) {
                    frames = 0;
                }
                i++;
            }
        }

        if (selftest) {
            System.exit(SelfTest.run());
        }

        if (frames > 0) {
            System.exit(renderFrames(frames));
        }

        launchWindow();
    }

    /** Builds the shared systems and shows the game window on the EDT. */
    private static void launchWindow() {
        SettingsManager settings = new SettingsManager();
        HighScoreStore highScores = new HighScoreStore();
        AudioManager audio = new AudioManager(settings);
        audio.loadAsync();

        GameEngine engine = new GameEngine(settings, highScores, audio);

        javax.swing.SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(engine);
            window.show();
        });
    }

    /**
     * Headless smoke render: drives the engine through a handful of frames and
     * writes a single PNG frame. Returns a process exit code.
     */
    private static int renderFrames(int count) {
        try {
            SettingsManager settings = new SettingsManager();
            HighScoreStore highScores = new HighScoreStore();
            GameEngine engine = new GameEngine(settings, highScores, null, 12345L);
            engine.startNewGame();

            for (int i = 0; i < count; i++) {
                engine.update(1.0 / 60.0);
            }

            BufferedImage img = new BufferedImage(Theme.WIDTH, Theme.HEIGHT,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            try {
                GamePanel.renderFrame(g2, engine, count / 60.0, 300, 400);
            } finally {
                g2.dispose();
            }

            java.io.File out = new java.io.File("nightfall_frames.png");
            ImageIO.write(img, "png", out);
            System.out.println("Rendered " + count + " frames -> " + out.getPath());
            return 0;
        } catch (Throwable t) {
            System.err.println("Headless frame render failed: " + t);
            return 1;
        }
    }
}
