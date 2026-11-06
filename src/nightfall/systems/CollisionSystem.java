// Developer: V.Viswa Vamsi
package nightfall.systems;

import nightfall.entities.Explosion;
import nightfall.entities.Missile;

/**
 * Pure geometry tests between live warheads and blast zones. Deliberately has
 * no knowledge of the engine so it can be reasoned about (and reused) on its own.
 */
public final class CollisionSystem {

    private CollisionSystem() {
    }

    /** True when the missile sits inside the explosion's current radius. */
    public static boolean hits(Missile m, Explosion e) {
        if (m == null || e == null) {
            return false;
        }
        if (!m.alive || e.chain || !e.alive) {
            return false;
        }
        double dx = m.x - e.x;
        double dy = m.y - e.y;
        double r = e.radius();
        return dx * dx + dy * dy <= r * r;
    }
}
