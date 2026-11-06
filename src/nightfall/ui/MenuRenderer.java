// Developer: V.Viswa Vamsi
package nightfall.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import nightfall.GameEngine;
import nightfall.data.SettingsManager;

/**
 * Draws the menu overlays (main menu, pause, game over, victory) and exposes
 * the button hit rectangles that InputController dispatches clicks against.
 * Everything renders in the fixed 1600x900 logical space of Theme.
 */
public final class MenuRenderer {

    private MenuRenderer() {
    }

    // ---- button geometry (shared with InputController) ---------------------
    public static final int BTN_X = 590;
    public static final int BTN_W = 420;
    public static final int BTN_H = 60;

    /** Main menu: Play and Quit (M1 scope; other screens arrive later). */
    public static final Rectangle MENU_PLAY = new Rectangle(BTN_X, 470, BTN_W, BTN_H);
    public static final Rectangle MENU_QUIT = new Rectangle(BTN_X, 550, BTN_W, BTN_H);

    /** Pause overlay. */
    public static final Rectangle PAUSE_RESUME = new Rectangle(BTN_X, 400, BTN_W, BTN_H);
    public static final Rectangle PAUSE_RESTART = new Rectangle(BTN_X, 480, BTN_W, BTN_H);
    public static final Rectangle PAUSE_MENU = new Rectangle(BTN_X, 560, BTN_W, BTN_H);

    /** Game over overlay. */
    public static final Rectangle OVER_RESTART = new Rectangle(BTN_X, 500, BTN_W, BTN_H);
    public static final Rectangle OVER_MENU = new Rectangle(BTN_X, 580, BTN_W, BTN_H);

    /** Victory overlay. */
    public static final Rectangle VICTORY_PLAY_AGAIN = new Rectangle(BTN_X, 500, BTN_W, BTN_H);
    public static final Rectangle VICTORY_MENU = new Rectangle(BTN_X, 580, BTN_W, BTN_H);

    /** Main-menu difficulty selector (small row of three, below the main buttons). */
    public static final int DIFF_BTN_W = 100;
    public static final int DIFF_BTN_H = 38;
    public static final Rectangle DIFF_NOOB = new Rectangle(620, 630, DIFF_BTN_W, DIFF_BTN_H);
    public static final Rectangle DIFF_PRO = new Rectangle(740, 630, DIFF_BTN_W, DIFF_BTN_H);
    public static final Rectangle DIFF_GOD = new Rectangle(860, 630, DIFF_BTN_W, DIFF_BTN_H);

    // ======================================================================
    // Main menu
    // ======================================================================

    /** Full-screen title screen with Play and Quit. */
    public static void renderMenu(Graphics2D g, GameEngine engine, double time, int mx, int my) {
        setup(g);
        dim(g, 0.55f);

        float pulse = 0.8f + 0.2f * (float) Math.sin(time * 2.2);

        g.setFont(Theme.HUD);
        centered(g, "P R O J E C T", 800, 215, Theme.alpha(Theme.GREY, 0.9f));

        g.setFont(Theme.TITLE);
        centered(g, "NIGHTFALL", 800 + 4, 330 + 4, Theme.alpha(Color.BLACK, 0.65f));
        centered(g, "NIGHTFALL", 800, 330, Theme.alpha(Theme.CYAN, pulse));

        g.setStroke(new BasicStroke(2f));
        g.setColor(Theme.alpha(Theme.AMBER, 0.85f));
        g.drawLine(560, 358, 1040, 358);

        g.setFont(Theme.H2);
        centered(g, "LAST LINE OF DEFENSE", 800, 402, Theme.AMBER);

        drawButton(g, MENU_PLAY, "PLAY", mx, my, Theme.CYAN);
        drawButton(g, MENU_QUIT, "QUIT", mx, my, Theme.RED);

        // Difficulty selector (small row of three).
        drawDiffButton(g, DIFF_NOOB, "NOOB", mx, my, engine.getSettings().difficulty == SettingsManager.Difficulty.NOOB);
        drawDiffButton(g, DIFF_PRO, "PRO", mx, my, engine.getSettings().difficulty == SettingsManager.Difficulty.PRO);
        drawDiffButton(g, DIFF_GOD, "GOD", mx, my, engine.getSettings().difficulty == SettingsManager.Difficulty.GOD);

        g.setFont(Theme.SMALL);
        centered(g, "MOUSE  AIM / FIRE      P OR ESC  PAUSE      F11  FULLSCREEN",
                800, 848, Theme.alpha(Theme.GREY, 0.85f));

        credit(g);
    }

    // ======================================================================
    // Pause overlay
    // ======================================================================

    /** Dimmed panel with Resume, Restart and Main Menu. */
    public static void renderPause(Graphics2D g, GameEngine engine, int mx, int my) {
        setup(g);
        dim(g, 0.62f);
        panel(g, 470, 280, 660, 360);

        g.setFont(Theme.H1);
        centered(g, "PAUSED", 800, 355, Theme.CYAN);

        g.setFont(Theme.SMALL);
        centered(g, "THE DEFENSE LINE IS HOLDING", 800, 388, Theme.alpha(Theme.GREY, 0.9f));

        drawButton(g, PAUSE_RESUME, "RESUME", mx, my, Theme.CYAN);
        drawButton(g, PAUSE_RESTART, "RESTART", mx, my, Theme.AMBER);
        drawButton(g, PAUSE_MENU, "MAIN MENU", mx, my, Theme.RED);

        drawModeLabel(g, engine.getSettings().difficulty);

        credit(g);
    }

    // ======================================================================
    // Game over overlay
    // ======================================================================

    /** Score report shown when every city has been destroyed. */
    public static void renderGameOver(Graphics2D g, GameEngine engine, int mx, int my) {
        setup(g);
        dim(g, 0.68f);
        panel(g, 470, 250, 660, 430);

        g.setFont(Theme.H1);
        centered(g, "GAME OVER", 800, 340, Theme.RED);

        g.setFont(Theme.SMALL);
        centered(g, "ALL CITIES LOST", 800, 378, Theme.alpha(Theme.RED, 0.85f));

        g.setFont(Theme.H2);
        centered(g, "SCORE   " + engine.getScore(), 800, 424, Theme.WHITE);
        centered(g, "BEST    " + engine.getBest(), 800, 464, Theme.AMBER);

        drawButton(g, OVER_RESTART, "RESTART", mx, my, Theme.CYAN);
        drawButton(g, OVER_MENU, "MAIN MENU", mx, my, Theme.RED);

        drawModeLabel(g, engine.getSettings().difficulty);

        credit(g);
    }

    // ======================================================================
    // Victory overlay
    // ======================================================================

    /** Shown after Wave 10 is cleared; offers to restart the game. */
    public static void renderVictory(Graphics2D g, GameEngine engine, int mx, int my) {
        setup(g);
        dim(g, 0.6f);
        panel(g, 470, 250, 660, 430);

        float pulse = 0.8f + 0.2f * (float) Math.sin(engine.getTime() * 3.0);

        g.setFont(Theme.H1);
        centered(g, "VICTORY", 800, 340, Theme.alpha(Theme.GREEN, pulse));

        g.setFont(Theme.SMALL);
        centered(g, "SECTOR HELD - WAVE 10 CLEARED", 800, 378, Theme.alpha(Theme.GREEN, 0.8f));

        g.setFont(Theme.H2);
        centered(g, "SCORE   " + engine.getScore(), 800, 424, Theme.WHITE);
        centered(g, "BEST    " + engine.getBest(), 800, 464, Theme.AMBER);

        drawButton(g, VICTORY_PLAY_AGAIN, "PLAY AGAIN", mx, my, Theme.CYAN);
        drawButton(g, VICTORY_MENU, "MAIN MENU", mx, my, Theme.RED);

        drawModeLabel(g, engine.getSettings().difficulty);

        credit(g);
    }

    // ======================================================================
    // Shared drawing helpers
    // ======================================================================

    private static void setup(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static void dim(Graphics2D g, float alpha) {
        g.setColor(Theme.alpha(Color.BLACK, alpha));
        g.fillRect(0, 0, Theme.WIDTH, Theme.HEIGHT);
    }

    /** Developer credit, centred near the bottom of every overlay. */
    private static void credit(Graphics2D g) {
        g.setFont(Theme.SMALL);
        centered(g, "DEVELOPED BY  V.VISWA VAMSI", Theme.WIDTH / 2, 878,
                Theme.alpha(Theme.GREY, 0.6f));
    }

    /** Translucent tactical panel with a glowing border and corner brackets. */
    private static void panel(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Theme.PANEL);
        g.fillRect(x, y, w, h);
        g.setStroke(new BasicStroke(2f));
        g.setColor(Theme.PANEL_LINE);
        g.drawRect(x, y, w, h);
        brackets(g, x, y, w, h, Theme.CYAN_DIM);
    }

    private static void brackets(Graphics2D g, int x, int y, int w, int h, Color c) {
        int len = 26;
        g.setStroke(new BasicStroke(3f));
        g.setColor(c);
        // top-left
        g.drawLine(x, y, x + len, y);
        g.drawLine(x, y, x, y + len);
        // top-right
        g.drawLine(x + w, y, x + w - len, y);
        g.drawLine(x + w, y, x + w, y + len);
        // bottom-left
        g.drawLine(x, y + h, x + len, y + h);
        g.drawLine(x, y + h, x, y + h - len);
        // bottom-right
        g.drawLine(x + w, y + h, x + w - len, y + h);
        g.drawLine(x + w, y + h, x + w, y + h - len);
    }

    /** Arcade button with hover highlight; accent drives the border tint. */
    private static void drawButton(Graphics2D g, Rectangle r, String label, int mx, int my,
            Color accent) {
        boolean hover = r.contains(mx, my);

        g.setColor(hover ? Theme.alpha(Theme.CYAN, 0.18f) : Theme.alpha(Theme.PANEL, 0.92f));
        g.fill(r);

        if (hover) {
            g.setColor(Theme.alpha(Theme.CYAN, 0.35f));
            g.fillRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8);
        }

        g.setStroke(new BasicStroke(hover ? 3f : 2f));
        g.setColor(hover ? Theme.CYAN : Theme.alpha(accent, 0.85f));
        g.draw(r);

        int tick = 14;
        g.setStroke(new BasicStroke(2f));
        g.setColor(hover ? Theme.WHITE : Theme.alpha(accent, 0.7f));
        g.drawLine(r.x, r.y, r.x + tick, r.y);
        g.drawLine(r.x, r.y, r.x, r.y + tick);
        g.drawLine(r.x + r.width, r.y + r.height, r.x + r.width - tick, r.y + r.height);
        g.drawLine(r.x + r.width, r.y + r.height, r.x + r.width, r.y + r.height - tick);

        g.setFont(Theme.H2);
        centered(g, label, r.x + r.width / 2, r.y + r.height / 2 + 11,
                hover ? Theme.WHITE : Theme.alpha(Theme.WHITE, 0.92f));
    }

    /** Centres text horizontally on cx at the current font's baseline. */
    private static void centered(Graphics2D g, String text, int cx, int baseline, Color color) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, cx - fm.stringWidth(text) / 2, baseline);
    }

    /** Small difficulty toggle button for the main menu; filled when selected. */
    private static void drawDiffButton(Graphics2D g, Rectangle r, String label, int mx, int my,
            boolean selected) {
        boolean hover = r.contains(mx, my);
        g.setColor(selected ? Theme.CYAN : Theme.alpha(Theme.PANEL, 0.92f));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
        g.setColor(selected ? Theme.WHITE : Theme.PANEL_LINE);
        g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);
        g.setFont(Theme.HUD_SMALL);
        g.setColor(selected ? Theme.WHITE : Theme.alpha(Theme.WHITE, 0.92f));
        int sw = g.getFontMetrics().stringWidth(label);
        g.drawString(label, r.x + (r.width - sw) / 2, r.y + 25);
    }

    /** Shows the current difficulty mode on a gameplay-related overlay panel. */
    private static void drawModeLabel(Graphics2D g, SettingsManager.Difficulty diff) {
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.alpha(Theme.GREY, 0.7f));
        String mode = diff == null ? "PRO" : diff.name();
        g.drawString("MODE: " + mode, Theme.WIDTH / 2 + 220, 330);
    }
}
