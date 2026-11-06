// Developer: V.Viswa Vamsi
package nightfall.entities;

/**
 * A friendly defensive rocket fired from a launch battery toward the point
 * the player clicked. When it arrives it detonates into an {@link Explosion}.
 */
public class Interceptor {

    private static final int TRAIL_LEN = 18;

    public double x;
    public double y;
    public final double startX;
    public final double startY;
    public final double targetX;
    public final double targetY;

    public boolean alive = true;
    public boolean detonated = false;

    private final double dirX;
    private final double dirY;
    private final double speed;
    private final double[] trailX = new double[TRAIL_LEN];
    private final double[] trailY = new double[TRAIL_LEN];
    private int trailHead = 0;
    private int trailCount = 0;

    public Interceptor(double startX, double startY, double targetX, double targetY, double speed) {
        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;

        double dx = targetX - startX;
        double dy = targetY - startY;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            len = 1;
        }
        this.dirX = dx / len;
        this.dirY = dy / len;
    }

    public void update(double dt) {
        if (!alive) {
            return;
        }
        pushTrail();
        x += dirX * speed * dt;
        y += dirY * speed * dt;

        double rx = targetX - x;
        double ry = targetY - y;
        double rem = Math.hypot(rx, ry);
        if (rem <= speed * dt + 0.5) {
            x = targetX;
            y = targetY;
            detonated = true;
            alive = false;
        }
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
