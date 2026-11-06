// Developer: V.Viswa Vamsi
package nightfall.entities;

/**
 * A defensive missile battery. The player has three (left, centre, right),
 * each with a limited stock of interceptors and a short firing cooldown.
 */
public class LaunchBattery {

    public static final int MAX_AMMO = 10;
    public static final double COOLDOWN = 0.14;
    public static final double HALF_WIDTH = 34;

    public final double x;
    public final double y;

    public int ammo = MAX_AMMO;
    public double cooldown = 0;
    public boolean alive = true;
    public double flash = 0;

    public LaunchBattery(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean canFire() {
        return alive && ammo > 0 && cooldown <= 0;
    }

    public void fire() {
        ammo--;
        cooldown = COOLDOWN;
        flash = 0.12;
    }

    public void resupply() {
        ammo = MAX_AMMO;
    }

    public void kill() {
        if (alive) {
            alive = false;
            ammo = 0;
            flash = 0.5;
        }
    }

    public boolean containsX(double px) {
        return Math.abs(px - x) <= HALF_WIDTH;
    }

    public void update(double dt) {
        if (cooldown > 0) {
            cooldown = Math.max(0, cooldown - dt);
        }
        if (flash > 0) {
            flash = Math.max(0, flash - dt);
        }
    }
}
