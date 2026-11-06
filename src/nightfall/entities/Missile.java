// Developer: V.Viswa Vamsi
package nightfall.entities;

import java.awt.Color;

/**
 * An enemy warhead falling from the top of the screen toward a fixed ground
 * target. Three behaviour variants exist: STANDARD, FAST and SPLITTER (which
 * breaks into a cluster of STANDARD children near the mid point of its fall).
 */
public class Missile {

    public enum Type {
        STANDARD,
        FAST,
        SPLITTER
    }

    private static final int TRAIL_LEN = 26;

    public double x;
    public double y;
    public final double startX;
    public final double startY;
    public final double targetX;
    public final double targetY;
    public final double speed;
    public final Type type;
    public final Color color;

    public boolean alive = true;
    public boolean reachedGround = false;
    public boolean destroyed = false;
    public boolean hasSplit = false;

    private final double dirX;
    private final double dirY;
    private final double[] trailX = new double[TRAIL_LEN];
    private final double[] trailY = new double[TRAIL_LEN];
    private int trailHead = 0;
    private int trailCount = 0;

    public Missile(double startX, double startY, double targetX, double targetY,
                   double speed, Type type) {
        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
        this.type = type;

        double dx = targetX - startX;
        double dy = targetY - startY;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            len = 1;
        }
        this.dirX = dx / len;
        this.dirY = dy / len;

        switch (type) {
            case FAST -> color = new Color(255, 120, 96);
            case SPLITTER -> color = new Color(206, 120, 255);
            default -> color = new Color(255, 196, 120);
        }
    }

    public void update(double dt) {
        if (!alive) {
            return;
        }
        pushTrail();
        x += dirX * speed * dt;
        y += dirY * speed * dt;
        if (y >= targetY) {
            y = targetY;
            reachedGround = true;
            alive = false;
        }
    }

    /** Y position at which a SPLITTER releases its cluster. */
    public double midY() {
        return (startY + targetY) * 0.5;
    }

    public void kill() {
        destroyed = true;
        alive = false;
    }

    private void pushTrail() {
        trailX[trailHead] = x;
        trailY[trailHead] = y;
        trailHead = (trailHead + 1) % TRAIL_LEN;
        if (trailCount < TRAIL_LEN) {
            trailCount++;
        }
    }

    public int trailCount() {
        return trailCount;
    }

    public double trailX(int i) {
        int idx = (trailHead - trailCount + i + TRAIL_LEN * 2) % TRAIL_LEN;
        return trailX[idx];
    }

    public double trailY(int i) {
        int idx = (trailHead - trailCount + i + TRAIL_LEN * 2) % TRAIL_LEN;
        return trailY[idx];
    }
}
