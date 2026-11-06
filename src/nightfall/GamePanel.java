// Developer: V.Viswa Vamsi
package nightfall;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import nightfall.ui.Backgrounds;
import nightfall.ui.HUDRenderer;
import nightfall.ui.MenuRenderer;
import nightfall.ui.Theme;

/**
 * Swing surface that owns the fixed-step game loop, letterbox transform and
 * per-frame rendering. All drawing happens in the 1600x900 logical space of
 * Theme; gameplay state is only mutated inside update(), never during paint.
 */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GameEngine engine;
    private final InputController input;
    private final Timer timer;

    private double lastNanos = System.nanoTime();
    private double animationTime;
    private Runnable fullscreenAction;

    public GamePanel(GameEngine engine) {
        this.engine = engine;
        this.input = new InputController(engine);

        setPreferredSize(new Dimension(Theme.WIDTH, Theme.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);

        addMouseListener(input);
        addMouseMotionListener(input);
        // Keep the cursor visible for aiming but never rely on focus alone.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        timer = new Timer(16, e -> tick());
        timer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // getRootPane() is null until the panel is attached to a window, so key
        // bindings must be installed here rather than in the constructor.
        installKeyBindings();
    }

    /** The input controller (mouse aiming / click routing). */
    public InputController getInput() {
        return input;
    }

    /** GameWindow installs the F11 fullscreen toggle here. */
    public void setFullscreenAction(Runnable fullscreenAction) {
        this.fullscreenAction = fullscreenAction;
    }

    private void installKeyBindings() {
        InputMap im = getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        bind(im, am, "pause", KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
        bind(im, am, "pause", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        bind(im, am, "restart", KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
        bind(im, am, "confirm", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        bind(im, am, "confirm", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        bind(im, am, "fullscreen", KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
    }

    private void bind(InputMap im, ActionMap am, String action, KeyStroke stroke) {
        im.put(stroke, action);
        if (am.get(action) == null) {
            am.put(action, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if ("fullscreen".equals(action)) {
                        if (fullscreenAction != null) {
                            fullscreenAction.run();
                        }
                    } else if ("pause".equals(action)) {
                        input.handleKey(KeyEvent.VK_P);
                    } else if ("restart".equals(action)) {
                        input.handleKey(KeyEvent.VK_R);
                    } else {
                        input.handleKey(KeyEvent.VK_ENTER);
                    }
                }
            });
        }
    }

    // ======================================================================
    // Game loop
    // ======================================================================

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        if (dt > 0.05) {
            dt = 0.05;
        }
        if (dt < 0) {
            dt = 0;
        }

        engine.update(dt);
        animationTime += dt;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            double scale = Math.min(w / (double) Theme.WIDTH, h / (double) Theme.HEIGHT);
            double offX = (w - Theme.WIDTH * scale) / 2.0;
            double offY = (h - Theme.HEIGHT * scale) / 2.0;
            input.setViewport(scale, offX, offY);

            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.translate(offX, offY);
            g2.scale(scale, scale);
            g2.clipRect(0, 0, Theme.WIDTH, Theme.HEIGHT);

            renderFrame(g2, engine, animationTime, input.getMouseX(), input.getMouseY());
        } finally {
            g2.dispose();
        }
    }

    /**
     * Draws one full logical frame: background always, world when a run is
     * active, HUD while in play, then the active screen overlay. Static so the
     * self-test can render without a window.
     */
    static void renderFrame(Graphics2D g2, GameEngine engine, double time, int mx, int my) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GameState state = engine.getState();
        boolean art = Backgrounds.draw(g2, state);
        if (art) {
            if (state == GameState.PLAYING || state == GameState.WAVE_INTERMISSION) {
                // Keep the ground legible under gameplay artwork.
                HUDRenderer.drawGroundOverlay(g2, 0.65f);
            }
            HUDRenderer.drawScanlines(g2);
        } else {
            HUDRenderer.drawBackground(g2, time);
        }

        boolean inRun = state == GameState.PLAYING || state == GameState.PAUSED
                || state == GameState.WAVE_INTERMISSION
                || state == GameState.GAME_OVER || state == GameState.VICTORY;

        if (inRun) {
            double shake = engine.getShake();
            int dx = (int) Math.round((Math.random() * 2 - 1) * 7 * shake);
            int dy = (int) Math.round((Math.random() * 2 - 1) * 7 * shake);
            g2.translate(dx, dy);
            HUDRenderer.renderWorld(g2, engine, time);
            g2.translate(-dx, -dy);
        }

        if (state == GameState.PLAYING || state == GameState.PAUSED
                || state == GameState.WAVE_INTERMISSION) {
            HUDRenderer.renderHud(g2, engine, time, mx, my);
        }

        switch (state) {
            case MENU -> MenuRenderer.renderMenu(g2, engine, time, mx, my);
            case PAUSED -> MenuRenderer.renderPause(g2, engine, mx, my);
            case GAME_OVER -> MenuRenderer.renderGameOver(g2, engine, mx, my);
            case VICTORY -> MenuRenderer.renderVictory(g2, engine, mx, my);
            default -> {
            }
        }
    }

    /** Letterbox rectangle in window pixels for the current component size. */
    Rectangle logicalBounds() {
        int w = getWidth();
        int h = getHeight();
        double scale = Math.min(w / (double) Theme.WIDTH, h / (double) Theme.HEIGHT);
        double offX = (w - Theme.WIDTH * scale) / 2.0;
        double offY = (h - Theme.HEIGHT * scale) / 2.0;
        return new Rectangle((int) offX, (int) offY,
                (int) Math.round(Theme.WIDTH * scale), (int) Math.round(Theme.HEIGHT * scale));
    }

    /** Stops the loop (window closing / headless teardown). */
    public void shutdown() {
        timer.stop();
    }

    /** True when running in an environment without a display. */
    static boolean headlessEnvironment() {
        return GraphicsEnvironment.isHeadless();
    }
}
