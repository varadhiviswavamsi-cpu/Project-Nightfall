// Developer: V.Viswa Vamsi
package nightfall.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

import nightfall.GameState;

/**
 * State-specific full-canvas artwork. Each state lazily resolves one PNG from
 * assets/images (classpath first, then the working directory) and draws it
 * cover-fit into the 1600x900 logical space. Loading failures are silent: the
 * caller falls back to the procedural sky whenever this class reports false.
 */
public final class Backgrounds {

    private static final Map<GameState, BufferedImage> IMAGES = new EnumMap<>(GameState.class);
    private static final Map<GameState, Boolean> ATTEMPTED = new EnumMap<>(GameState.class);

    private Backgrounds() {
    }

    /**
     * Draws the artwork for the given state over the whole canvas, applying a
     * per-state dimming scrim on top. Returns false when no image is available
     * so the caller can keep the procedural background.
     */
    public static synchronized boolean draw(Graphics2D g, GameState state) {
        BufferedImage img = imageFor(state);
        if (img == null) {
            return false;
        }

        int iw = img.getWidth();
        int ih = img.getHeight();
        if (iw <= 0 || ih <= 0) {
            return false;
        }

        double scale = Math.max(Theme.WIDTH / (double) iw, Theme.HEIGHT / (double) ih);
        int dw = (int) Math.ceil(iw * scale);
        int dh = (int) Math.ceil(ih * scale);
        int dx = (Theme.WIDTH - dw) / 2;
        int dy = (Theme.HEIGHT - dh) / 2;

        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, dx, dy, dw, dh, null);

        float scrim = scrimAlpha(state);
        if (scrim > 0f) {
            g.setColor(Theme.alpha(Color.BLACK, scrim));
            g.fillRect(0, 0, Theme.WIDTH, Theme.HEIGHT);
        }
        return true;
    }

    /** Artwork filename for a state, or null when the state has no artwork. */
    static String assetName(GameState state) {
        return switch (state) {
            case MENU, HIGH_SCORES -> "menu.png";
            case PLAYING, WAVE_INTERMISSION -> "gameplay.png";
            case PAUSED, SETTINGS, HOW_TO_PLAY -> "pause.png";
            case VICTORY -> "victory.png";
            case GAME_OVER -> "gameover.png";
        };
    }

    /**
     * Black overlay strength per state, tuned against the existing menu dim
     * values so artwork never fights the overlay text.
     */
    static float scrimAlpha(GameState state) {
        return switch (state) {
            case MENU -> 0.20f;
            case HIGH_SCORES -> 0.50f;
            case PLAYING -> 0.45f;
            case WAVE_INTERMISSION -> 0.50f;
            case PAUSED -> 0.15f;
            case SETTINGS, HOW_TO_PLAY -> 0.60f;
            case GAME_OVER, VICTORY -> 0.15f;
        };
    }

    private static BufferedImage imageFor(GameState state) {
        if (Boolean.TRUE.equals(ATTEMPTED.get(state))) {
            return IMAGES.get(state);
        }
        ATTEMPTED.put(state, Boolean.TRUE);

        String name = assetName(state);
        if (name == null) {
            return null;
        }
        BufferedImage img = load(name);
        if (img != null) {
            IMAGES.put(state, img);
        }
        return img;
    }

    private static BufferedImage load(String name) {
        // Classpath location used when running from a packaged jar.
        URL url = Backgrounds.class.getResource("/images/" + name);
        if (url != null) {
            try (InputStream in = url.openStream()) {
                return ImageIO.read(in);
            } catch (IOException ignored) {
                // fall through to filesystem probing
            }
        }
        // Filesystem locations used by ./run.sh from the project root.
        String[] candidates = {
            "assets/images/" + name,
            name,
            "../assets/images/" + name
        };
        for (String path : candidates) {
            try {
                java.io.File f = new java.io.File(path);
                if (f.isFile()) {
                    return ImageIO.read(f);
                }
            } catch (IOException ignored) {
                // try the next candidate
            }
        }
        return null;
    }

    /** Cover-fit destination size for an image, exposed for reuse/tests. */
    static int[] coverFit(int iw, int ih) {
        double scale = Math.max(Theme.WIDTH / (double) iw, Theme.HEIGHT / (double) ih);
        return new int[]{
            (int) Math.ceil(iw * scale),
            (int) Math.ceil(ih * scale)
        };
    }
}
