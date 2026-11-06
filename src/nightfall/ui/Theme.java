// Developer: V.Viswa Vamsi
package nightfall.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Central palette, layout constants and fonts for PROJECT NIGHTFALL.
 * All rendering uses a fixed 1600x900 logical coordinate space that the
 * GamePanel letterboxes onto the real window.
 */
public final class Theme {

    private Theme() {
    }

    // ---- Logical resolution -------------------------------------------------
    public static final int WIDTH = 1600;
    public static final int HEIGHT = 900;
    public static final int GROUND_Y = 780;

    // ---- Palette ------------------------------------------------------------
    public static final Color VOID = new Color(4, 6, 16);
    public static final Color DEEP = new Color(8, 12, 30);
    public static final Color DEEP2 = new Color(12, 18, 42);
    public static final Color CYAN = new Color(64, 232, 255);
    public static final Color CYAN_DIM = new Color(38, 140, 158);
    public static final Color BLUE = new Color(74, 132, 255);
    public static final Color AMBER = new Color(255, 186, 74);
    public static final Color RED = new Color(255, 78, 84);
    public static final Color GREEN = new Color(96, 240, 150);
    public static final Color WHITE = new Color(232, 244, 255);
    public static final Color GREY = new Color(140, 156, 178);
    public static final Color PANEL = new Color(10, 16, 34, 220);
    public static final Color PANEL_LINE = new Color(64, 232, 255, 90);

    // ---- Fonts --------------------------------------------------------------
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 96);
    public static final Font H1 = new Font("SansSerif", Font.BOLD, 44);
    public static final Font H2 = new Font("SansSerif", Font.BOLD, 30);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 22);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 16);
    public static final Font HUD = new Font("Monospaced", Font.BOLD, 24);
    public static final Font HUD_SMALL = new Font("Monospaced", Font.PLAIN, 16);

    /** Turns a base colour into a translucent version (0..1 alpha). */
    public static Color alpha(Color c, float a) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255f)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), al);
    }

    public static boolean headless() {
        return GraphicsEnvironment.isHeadless();
    }
}
