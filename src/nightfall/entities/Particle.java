// Developer: V.Viswa Vamsi
package nightfall.entities;

import java.awt.Color;

/** A short lived spark emitted by explosions and impacts. */
public class Particle {

    private static final double DRAG = 0.985;
    private static final double GRAVITY = 140.0;

    public double x;
    public double y;
    public double vx;
    public double vy;
    public double life;
    public final double maxLife;
    public final Color color;
    public final double size;

    public Particle(double x, double y, double vx, double vy, double life, Color color, double size) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = color;
        this.size = size;
    }

    public boolean alive() {
        return life > 0;
    }

    public double alpha() {
        if (maxLife <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, life / maxLife));
    }

    public void update(double dt) {
        if (life <= 0) {
            return;
        }
        vy += GRAVITY * dt;
        vx *= DRAG;
        vy *= DRAG;
        x += vx * dt;
        y += vy * dt;
        life -= dt;
    }
}
