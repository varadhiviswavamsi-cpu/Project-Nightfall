// Developer: V.Viswa Vamsi
package nightfall.entities;

import java.awt.Color;

/**
 * An expanding defensive blast. It grows quickly, holds, then fades. Collision
 * uses the current radius. Explosions flagged as {@code chain} are visual only
 * (secondary detonations that do not themselves destroy warheads).
 */
public class Explosion {

    public final double x;
    public final double y;
    public final double maxRadius;
    public final double maxLife;
    public final Color color;
    public final boolean chain;

    public double life;
    public boolean alive = true;

    public Explosion(double x, double y, double maxRadius, double maxLife, Color color, boolean chain) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.maxLife = maxLife;
        this.color = color;
        this.chain = chain;
        this.life = maxLife;
    }

    public void update(double dt) {
        if (!alive) {
            return;
        }
        life -= dt;
        if (life <= 0) {
            life = 0;
            alive = false;
        }
    }

    /** 0..1 progress through the explosion's life. */
    public double progress() {
        if (maxLife <= 0) {
            return 1;
        }
        return 1.0 - (life / maxLife);
    }

    /** Current blast radius (never below a small minimum so fresh blasts can hit). */
    public double radius() {
        double p = progress();
        double r;
        if (p < 0.4) {
            r = maxRadius * (p / 0.4);
        } else if (p < 0.8) {
            r = maxRadius;
        } else {
            double t = (p - 0.8) / 0.2;
            r = maxRadius * (1.0 - 0.25 * t);
        }
        return Math.max(6.0, r);
    }
}
