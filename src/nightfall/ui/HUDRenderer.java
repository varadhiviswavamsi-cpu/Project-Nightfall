// Developer: V.Viswa Vamsi
package nightfall.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Random;

import nightfall.GameEngine;
import nightfall.entities.City;
import nightfall.entities.Explosion;
import nightfall.entities.Interceptor;
import nightfall.entities.LaunchBattery;
import nightfall.entities.Missile;
import nightfall.entities.Particle;

/**
 * Draws the starfield background, the world entities and the tactical HUD.
 * Everything is rendered in the fixed 1600x900 logical space of Theme.
 */
public final class HUDRenderer {

    /** Pause button in the top-right corner of the HUD (logical space). */
    public static final Rectangle PAUSE_BUTTON =
            new Rectangle(Theme.WIDTH - 130, 6, 110, 20);

    private HUDRenderer() {
    }

    // ---- deterministic decoration tables ----------------------------------
    private static final int STAR_COUNT = 170;
    private static final double[] STAR_X = new double[STAR_COUNT];
    private static final double[] STAR_Y = new double[STAR_COUNT];
    private static final double[] STAR_BRIGHT = new double[STAR_COUNT];
    private static final double[] STAR_PHASE = new double[STAR_COUNT];

    private static final int BUILDING_COUNT = 46;
    private static final double[] BLD_X = new double[BUILDING_COUNT];
    private static final double[] BLD_W = new double[BUILDING_COUNT];
    private static final double[] BLD_H = new double[BUILDING_COUNT];
    private static final int[] BLD_WIN = new int[BUILDING_COUNT];

    static {
        Random r = new Random(20240101L);
        for (int i = 0; i < STAR_COUNT; i++) {
            STAR_X[i] = r.nextDouble() * Theme.WIDTH;
            STAR_Y[i] = r.nextDouble() * (Theme.GROUND_Y - 60);
            STAR_BRIGHT[i] = 0.25 + r.nextDouble() * 0.75;
            STAR_PHASE[i] = r.nextDouble() * Math.PI * 2;
        }
        double x = -20;
        for (int i = 0; i < BUILDING_COUNT && x < Theme.WIDTH + 40; i++) {
            double w = 34 + r.nextDouble() * 56;
            double h = 30 + r.nextDouble() * 110;
            BLD_X[i] = x;
            BLD_W[i] = w;
            BLD_H[i] = h;
            BLD_WIN[i] = r.nextInt();
            x += w + 4 + r.nextDouble() * 18;
        }
    }

    // ======================================================================
    // Background
    // ======================================================================

    /** Sky, stars, horizon glow, ground grid, skyline and scanlines. */
    public static void drawBackground(Graphics2D g, double time) {
        drawProceduralSky(g, time);
        drawGroundOverlay(g, 1f);
        drawScanlines(g);
    }

    /** Sky gradient, horizon glow, twinkling stars and distant skyline. */
    public static void drawProceduralSky(Graphics2D g, double time) {
        g.setComposite(AlphaComposite.SrcOver);

        // Sky gradient.
        g.setPaint(new GradientPaint(0, 0, Theme.VOID, 0, Theme.GROUND_Y, Theme.DEEP2));
        g.fillRect(0, 0, Theme.WIDTH, Theme.GROUND_Y);

        // Horizon glow.
        g.setPaint(new RadialGradientPaint(
                Theme.WIDTH / 2f, Theme.GROUND_Y, 620f,
                new float[]{0f, 1f},
                new Color[]{Theme.alpha(Theme.CYAN, 0.10f), Theme.alpha(Theme.CYAN, 0f)}));
        g.fillRect(0, Theme.GROUND_Y - 420, Theme.WIDTH, 420);

        // Stars (twinkling).
        for (int i = 0; i < STAR_COUNT; i++) {
            double tw = 0.55 + 0.45 * Math.sin(time * 1.6 + STAR_PHASE[i]);
            float a = (float) (STAR_BRIGHT[i] * tw);
            g.setColor(Theme.alpha(Theme.WHITE, a));
            int s = STAR_BRIGHT[i] > 0.8 ? 2 : 1;
            g.fillRect((int) STAR_X[i], (int) STAR_Y[i], s, s);
        }

        // Distant skyline behind the playfield.
        drawSkyline(g, time);
    }

    /** Ground plane, horizon line and perspective grid at the given opacity. */
    public static void drawGroundOverlay(Graphics2D g, float opacity) {
        if (opacity <= 0f) {
            return;
        }
        g.setComposite(AlphaComposite.SrcOver);

        // Ground.
        g.setPaint(new GradientPaint(0, Theme.GROUND_Y, Theme.alpha(Theme.DEEP, opacity),
                0, Theme.HEIGHT, Theme.alpha(Theme.VOID, opacity)));
        g.fillRect(0, Theme.GROUND_Y, Theme.WIDTH, Theme.HEIGHT - Theme.GROUND_Y);
        g.setColor(Theme.alpha(Theme.CYAN, 0.55f * opacity));
        g.fillRect(0, Theme.GROUND_Y, Theme.WIDTH, 2);

        // Perspective grid on the ground.
        g.setColor(Theme.alpha(Theme.CYAN, 0.10f * opacity));
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1f));
        for (int i = 1; i <= 4; i++) {
            double f = i / 4.0;
            double yy = Theme.GROUND_Y + f * f * (Theme.HEIGHT - Theme.GROUND_Y);
            g.drawLine(0, (int) yy, Theme.WIDTH, (int) yy);
        }
        for (int i = -8; i <= 8; i++) {
            int bx = Theme.WIDTH / 2 + i * 170;
            g.drawLine(Theme.WIDTH / 2, Theme.GROUND_Y + 2, bx, Theme.HEIGHT);
        }
        g.setStroke(old);
    }

    /** Subtle CRT scanline overlay. */
    public static void drawScanlines(Graphics2D g) {
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(Theme.alpha(Color.BLACK, 0.05f));
        for (int y = 0; y < Theme.HEIGHT; y += 3) {
            g.drawLine(0, y, Theme.WIDTH, y);
        }
    }

    private static void drawSkyline(Graphics2D g, double time) {
        for (int i = 0; i < BUILDING_COUNT; i++) {
            if (BLD_W[i] <= 0) {
                continue;
            }
            double x = BLD_X[i];
            double h = BLD_H[i];
            double top = Theme.GROUND_Y - h;
            g.setColor(new Color(6, 9, 22));
            g.fillRect((int) x, (int) top, (int) BLD_W[i], (int) h);

            // Sparse lit windows with a slow deterministic shimmer.
            int seed = BLD_WIN[i];
            for (int wy = 0; wy < h - 12; wy += 14) {
                for (int wx = 4; wx < BLD_W[i] - 8; wx += 12) {
                    seed = seed * 1103515245 + 12345;
                    if (((seed >> 16) & 15) < 7) {
                        continue;
                    }
                    double ph = ((seed >> 8) & 255) / 255.0 * Math.PI * 2;
                    double s = 0.5 + 0.5 * Math.sin(time * 0.7 + ph);
                    g.setColor(Theme.alpha(Theme.AMBER, (float) (0.10 + 0.28 * s)));
                    g.fillRect((int) x + wx, (int) top + 6 + wy, 5, 6);
                }
            }
        }
    }

    // ======================================================================
    // World entities
    // ======================================================================

    public static void renderWorld(Graphics2D g, GameEngine engine, double time) {
        for (City c : engine.getCities()) {
            drawCity(g, c, time);
        }
        for (LaunchBattery b : engine.getBatteries()) {
            drawBattery(g, b, time);
        }
        for (Particle p : engine.getParticles()) {
            drawParticle(g, p);
        }
        for (Interceptor in : engine.getInterceptors()) {
            drawInterceptor(g, in);
        }
        for (Missile m : engine.getMissiles()) {
            drawMissile(g, m, time);
        }
        for (Explosion e : engine.getExplosions()) {
            drawExplosion(g, e);
        }
        for (GameEngine.FloatingText ft : engine.getFloatingTexts()) {
            drawFloatingText(g, ft);
        }
    }

    static void drawCity(Graphics2D g, City c, double time) {
        double base = c.y;
        if (!c.alive) {
            // Rubble.
            double[] sk = c.skyline();
            for (int i = 0; i < sk.length; i++) {
                double h = Math.min(sk[i] * 0.35, 16);
                double bx = c.x - 60 + i * 20;
                g.setColor(new Color(18, 20, 30));
                g.fillRect((int) bx, (int) (base - h), 18, (int) h);
            }
            g.setColor(Theme.alpha(Theme.RED, 0.25f + 0.15f * (float) Math.sin(time * 3)));
            g.fillRect((int) (c.x - 60), (int) base - 3, 120, 3);
            return;
        }

        // Platform.
        g.setColor(new Color(14, 22, 46));
        g.fillRect((int) (c.x - 64), (int) base - 8, 128, 8);
        g.setColor(Theme.alpha(Theme.CYAN_DIM, 0.8f));
        g.fillRect((int) (c.x - 64), (int) base - 8, 128, 2);

        // Buildings.
        double[] sk = c.skyline();
        for (int i = 0; i < sk.length; i++) {
            double h = sk[i];
            double bx = c.x - 60 + i * 20;
            g.setColor(new Color(16, 26, 54));
            g.fillRect((int) bx, (int) (base - 8 - h), 18, (int) h);
            g.setColor(Theme.alpha(Theme.CYAN_DIM, 0.35f));
            g.drawRect((int) bx, (int) (base - 8 - h), 18, (int) h);
            // Lit windows.
            for (int wy = 4; wy < h - 4; wy += 8) {
                double ph = (i * 7 + wy) * 0.7;
                double s = 0.5 + 0.5 * Math.sin(time * 1.1 + ph);
                g.setColor(Theme.alpha((i + wy) % 3 == 0 ? Theme.AMBER : Theme.CYAN,
                        (float) (0.30 + 0.45 * s)));
                g.fillRect((int) bx + 3, (int) (base - 8 - h) + wy, 4, 4);
                g.fillRect((int) bx + 11, (int) (base - 8 - h) + wy, 4, 4);
            }
        }

        // Hit flash.
        if (c.flash > 0) {
            g.setColor(Theme.alpha(Theme.RED, (float) Math.min(0.7, c.flash)));
            g.fillRect((int) (c.x - 66), (int) (base - 90), 132, 92);
        }
    }

    static void drawBattery(Graphics2D g, LaunchBattery b, double time) {
        double x = b.x;
        double y = b.y;
        if (!b.alive) {
            g.setColor(new Color(24, 26, 34));
            g.fillPolygon(new int[]{(int) x - 30, (int) x + 30, (int) x + 22, (int) x - 22},
                    new int[]{(int) y, (int) y, (int) y - 8, (int) y - 8}, 4);
            g.setColor(Theme.alpha(Theme.RED, 0.5f));
            g.fillRect((int) x - 3, (int) y - 14, 6, 6);
            return;
        }

        // Base.
        g.setColor(new Color(16, 30, 58));
        g.fillPolygon(new int[]{(int) x - 34, (int) x + 34, (int) x + 24, (int) x - 24},
                new int[]{(int) y, (int) y, (int) y - 16, (int) y - 16}, 4);
        g.setColor(Theme.alpha(Theme.CYAN, 0.85f));
        g.fillRect((int) x - 34, (int) y - 2, 68, 2);

        // Dome + barrel.
        g.setColor(new Color(22, 44, 82));
        g.fillOval((int) x - 14, (int) y - 34, 28, 24);
        g.setColor(Theme.CYAN_DIM);
        g.drawOval((int) x - 14, (int) y - 34, 28, 24);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.alpha(Theme.CYAN, 0.9f));
        g.drawLine((int) x, (int) y - 24, (int) x, (int) y - 44);
        g.setStroke(new BasicStroke(1f));

        // Ready glow.
        if (b.canFire()) {
            float pulse = 0.45f + 0.35f * (float) Math.sin(time * 4 + x);
            g.setColor(Theme.alpha(Theme.GREEN, pulse));
            g.fillOval((int) x - 3, (int) y - 40, 6, 6);
        }

        if (b.flash > 0) {
            g.setColor(Theme.alpha(Theme.CYAN, (float) Math.min(0.7, b.flash)));
            g.fillOval((int) x - 36, (int) y - 44, 72, 46);
        }
    }

    private static void drawParticle(Graphics2D g, Particle p) {
        if (!p.alive()) {
            return;
        }
        float a = (float) p.alpha();
        if (a <= 0.02f) {
            return;
        }
        g.setColor(Theme.alpha(p.color, a));
        int s = Math.max(1, (int) p.size);
        g.fillOval((int) (p.x - s / 2.0), (int) (p.y - s / 2.0), s, s);
    }

    private static void drawInterceptor(Graphics2D g, Interceptor in) {
        if (!in.alive) {
            return;
        }
        // Trail.
        int n = in.trailCount();
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 1; i < n; i++) {
            float t = i / (float) n;
            g.setColor(Theme.alpha(Theme.CYAN, t * 0.85f));
            int x1 = (int) in.trailX(i - 1), y1 = (int) in.trailY(i - 1);
            int x2 = (int) in.trailX(i), y2 = (int) in.trailY(i);
            g.drawLine(x1, y1, x2, y2);
        }
        g.setStroke(old);

        // Head.
        g.setColor(Theme.alpha(Theme.CYAN, 0.35f));
        g.fillOval((int) in.x - 7, (int) in.y - 7, 14, 14);
        g.setColor(Theme.WHITE);
        g.fillOval((int) in.x - 3, (int) in.y - 3, 6, 6);
    }

    private static void drawMissile(Graphics2D g, Missile m, double time) {
        if (!m.alive) {
            return;
        }
        // Trail.
        int n = m.trailCount();
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 1; i < n; i++) {
            float t = i / (float) n;
            g.setColor(Theme.alpha(m.color, t * 0.75f));
            g.drawLine((int) m.trailX(i - 1), (int) m.trailY(i - 1),
                    (int) m.trailX(i), (int) m.trailY(i));
        }
        g.setStroke(old);

        // Target pip (flickers once close).
        if (!m.reachedGround) {
            float pa = (float) (0.25 + 0.2 * Math.sin(time * 6));
            g.setColor(Theme.alpha(Theme.RED, pa));
            g.drawOval((int) m.targetX - 7, (int) m.targetY - 7, 14, 14);
            g.drawLine((int) m.targetX - 12, (int) m.targetY, (int) m.targetX + 12, (int) m.targetY);
            g.drawLine((int) m.targetX, (int) m.targetY - 12, (int) m.targetX, (int) m.targetY + 12);
        }

        // Warhead glow + core.
        g.setColor(Theme.alpha(m.color, 0.30f));
        g.fillOval((int) m.x - 9, (int) m.y - 9, 18, 18);
        g.setColor(m.color);
        g.fillOval((int) m.x - 4, (int) m.y - 4, 8, 8);
        g.setColor(Theme.WHITE);
        g.fillOval((int) m.x - 2, (int) m.y - 2, 4, 4);
    }

    static void drawExplosion(Graphics2D g, Explosion e) {
        if (!e.alive) {
            return;
        }
        double r = e.radius();
        if (r <= 0.5) {
            return;
        }
        float p = (float) e.progress();
        float fade = p < 0.8f ? 1f : Math.max(0f, (1f - p) / 0.2f);

        // Outer glow.
        float gr = (float) Math.max(1, r);
        g.setPaint(new RadialGradientPaint(
                (float) e.x, (float) e.y, gr,
                new float[]{0f, 0.55f, 1f},
                new Color[]{Theme.alpha(e.color, 0.85f * fade),
                        Theme.alpha(e.color, 0.30f * fade),
                        Theme.alpha(e.color, 0f)}));
        g.fillOval((int) (e.x - r), (int) (e.y - r), (int) (r * 2), (int) (r * 2));

        // Ring.
        g.setColor(Theme.alpha(Theme.WHITE, 0.55f * fade));
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2f));
        int ring = (int) (r * 0.82);
        g.drawOval((int) e.x - ring, (int) e.y - ring, ring * 2, ring * 2);
        g.setStroke(old);

        // Core.
        double core = Math.max(4, r * 0.35);
        g.setColor(Theme.alpha(Theme.WHITE, 0.9f * fade));
        g.fillOval((int) (e.x - core), (int) (e.y - core), (int) (core * 2), (int) (core * 2));
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static void drawFloatingText(Graphics2D g, GameEngine.FloatingText ft) {
        float a = (float) ft.alpha();
        if (a <= 0.02f) {
            return;
        }
        g.setFont(Theme.HUD);
        g.setColor(Theme.alpha(ft.color, a));
        String s = ft.text;
        int w = g.getFontMetrics().stringWidth(s);
        g.drawString(s, (int) ft.x - w / 2, (int) ft.y);
    }

    // ======================================================================
    // HUD
    // ======================================================================

    public static void renderHud(Graphics2D g, GameEngine engine, double time, int mx, int my) {
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Top bar.
        int barH = 58;
        g.setColor(Theme.PANEL);
        g.fillRect(0, 0, Theme.WIDTH, barH);
        g.setColor(Theme.PANEL_LINE);
        g.fillRect(0, barH, Theme.WIDTH, 2);

        // Score.
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.GREY);
        g.drawString("SCORE", 28, 22);
        g.setFont(Theme.HUD);
        g.setColor(Theme.WHITE);
        g.drawString(String.format("%08d", engine.getScore()), 28, 48);

        // Best (small, right of score).
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.alpha(Theme.GREY, 0.9f));
        g.drawString("BEST " + String.format("%07d", engine.getBest()), 260, 48);

        // Wave.
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.GREY);
        g.drawString("WAVE", Theme.WIDTH / 2 - 70, 22);
        g.setFont(Theme.HUD);
        g.setColor(Theme.CYAN);
        String wave = String.format("%02d / %02d", engine.getWave(), nightfall.systems.WaveManager.FINAL_WAVE);
        g.drawString(wave, Theme.WIDTH / 2 - g.getFontMetrics().stringWidth(wave) / 2, 48);

        // Cities.
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.GREY);
        g.drawString("CITIES", Theme.WIDTH - 400, 22);
        List<City> cities = engine.getCities();
        for (int i = 0; i < cities.size(); i++) {
            int cx = Theme.WIDTH - 400 + i * 26;
            g.setColor(cities.get(i).alive ? Theme.GREEN : Theme.alpha(Theme.RED, 0.5f));
            g.fillRoundRect(cx, 32, 20, 16, 4, 4);
        }

        // Threat.
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.GREY);
        g.drawString("THREAT", Theme.WIDTH - 220, 22);
        int threat = engine.getThreat();
        g.setColor(Theme.alpha(Theme.RED, 0.25f));
        g.fillRect(Theme.WIDTH - 220, 34, 180, 12);
        float tf = (float) Math.min(1.0, threat / 24.0);
        g.setColor(Theme.alpha(Theme.RED, 0.9f));
        g.fillRect(Theme.WIDTH - 220, 34, (int) (180 * tf), 12);
        g.setColor(Theme.alpha(Theme.RED, 0.9f));
        g.drawRect(Theme.WIDTH - 220, 34, 180, 12);
        g.setColor(Theme.WHITE);
        g.drawString(String.valueOf(threat), Theme.WIDTH - 34, 45);

        // Difficulty mode (left of cities bar).
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.GREY);
        g.drawString("MODE", Theme.WIDTH - 500, 22);
        g.setFont(Theme.HUD);
        g.setColor(Theme.WHITE);
        String diffName = engine.getSettings() != null ? engine.getSettings().difficulty.name() : "PRO";
        g.drawString(diffName, Theme.WIDTH - 500, 48);

        // Pause button (top right, only while playing).
        if (engine.getState() == nightfall.GameState.PLAYING) {
            drawPauseButton(g, PAUSE_BUTTON.contains(mx, my));
        }

        // Combo bar (below the top bar, left side).
        int combo = engine.getCombo();
        double frac = engine.comboFraction();
        if (combo > 1 || frac > 0) {
            int cw = 240;
            int cx0 = 28, cy0 = barH + 14;
            g.setFont(Theme.HUD);
            g.setColor(combo >= 5 ? Theme.AMBER : Theme.GREEN);
            g.drawString("x" + combo, cx0, cy0 + 24);
            g.setColor(Theme.alpha(Theme.GREEN, 0.25f));
            g.fillRect(cx0 + 56, cy0 + 8, cw, 12);
            g.setColor(combo >= 5 ? Theme.AMBER : Theme.GREEN);
            g.fillRect(cx0 + 56, cy0 + 8, (int) (cw * Math.max(0, Math.min(1, frac))), 12);
        }

        // Bottom: battery ammo pips.
        g.setFont(Theme.HUD_SMALL);
        List<LaunchBattery> bats = engine.getBatteries();
        for (LaunchBattery b : bats) {
            int bx = (int) b.x;
            int by = Theme.HEIGHT - 34;
            if (!b.alive) {
                g.setColor(Theme.alpha(Theme.RED, 0.9f));
                g.drawString("OFFLINE", bx - 34, by + 4);
                continue;
            }
            for (int i = 0; i < LaunchBattery.MAX_AMMO; i++) {
                int px = bx - 60 + i * 13;
                if (i < b.ammo) {
                    g.setColor(Theme.alpha(Theme.AMBER, 0.95f));
                } else {
                    g.setColor(Theme.alpha(Theme.GREY, 0.25f));
                }
                g.fillRect(px, by, 9, 14);
            }
            g.setColor(Theme.alpha(Theme.GREY, 0.9f));
            String label = b.ammo + "/" + LaunchBattery.MAX_AMMO;
            g.drawString(label, bx - 24, by - 6);
        }

        // Developer credit.
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.alpha(Theme.GREY, 0.45f));
        g.drawString("V.VISWA VAMSI", 28, 892);

        // Intermission banner.
        if (engine.getState() == nightfall.GameState.WAVE_INTERMISSION) {
            g.setFont(Theme.H2);
            String s = "WAVE " + engine.getWave() + " CLEARED  —  NEXT WAVE IN "
                    + String.format("%.1f", Math.max(0, engine.getIntermissionTimer())) + "s";
            int w = g.getFontMetrics().stringWidth(s);
            int px = Theme.WIDTH / 2 - w / 2 - 30;
            g.setColor(Theme.PANEL);
            g.fillRoundRect(px, 110, w + 60, 56, 14, 14);
            g.setColor(Theme.alpha(Theme.CYAN, 0.7f));
            g.drawRoundRect(px, 110, w + 60, 56, 14, 14);
            g.setColor(Theme.GREEN);
            g.drawString(s, Theme.WIDTH / 2 - w / 2, 148);
        }

        // Impact flash vignette.
        double flash = engine.getImpactFlash();
        if (flash > 0.01) {
            g.setColor(Theme.alpha(Theme.RED, (float) Math.min(0.35, flash * 0.35)));
            g.fillRect(0, 0, Theme.WIDTH, Theme.HEIGHT);
        }

        // Crosshair.
        drawCrosshair(g, engine, mx, my, time);
    }

    private static void drawCrosshair(Graphics2D g, GameEngine engine, int mx, int my, double time) {
        if (mx < 0 || my < 0) {
            return;
        }
        boolean canFire = false;
        for (LaunchBattery b : engine.getBatteries()) {
            if (b.alive && b.canFire()) {
                canFire = true;
                break;
            }
        }
        Color c = canFire ? Theme.CYAN : Theme.RED;
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2f));
        g.setColor(Theme.alpha(c, 0.9f));
        int r = 16;
        g.drawOval(mx - r, my - r, r * 2, r * 2);
        g.drawLine(mx - 26, my, mx - 8, my);
        g.drawLine(mx + 8, my, mx + 26, my);
        g.drawLine(mx, my - 26, mx, my - 8);
        g.drawLine(mx, my + 8, mx, my + 26);
        // Sweep dot.
        double ang = time * 3.0;
        int sx = mx + (int) (Math.cos(ang) * r);
        int sy = my + (int) (Math.sin(ang) * r);
        g.setColor(Theme.alpha(c, 1f));
        g.fillOval(sx - 2, sy - 2, 4, 4);
        g.setStroke(old);
    }

    /** Small pause button in the top-right corner of the HUD. */
    static void drawPauseButton(Graphics2D g, boolean hovered) {
        g.setColor(hovered ? Theme.alpha(Theme.CYAN, 0.15f) : Theme.PANEL);
        g.fillRoundRect(PAUSE_BUTTON.x, PAUSE_BUTTON.y, PAUSE_BUTTON.width,
                PAUSE_BUTTON.height, 8, 8);
        g.setColor(Theme.PANEL_LINE);
        g.drawRoundRect(PAUSE_BUTTON.x, PAUSE_BUTTON.y, PAUSE_BUTTON.width,
                PAUSE_BUTTON.height, 8, 8);
        g.setFont(Theme.HUD_SMALL);
        g.setColor(Theme.WHITE);
        String s = "PAUSE";
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, PAUSE_BUTTON.x + (PAUSE_BUTTON.width - sw) / 2,
                PAUSE_BUTTON.y + 15);
    }
}
