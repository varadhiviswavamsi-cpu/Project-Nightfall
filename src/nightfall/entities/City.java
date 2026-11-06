// Developer: V.Viswa Vamsi
package nightfall.entities;

/**
 * One of the five protected cities sitting on the ground line. A city is alive
 * until a warhead reaches it; a fresh hit triggers a brief destruction flash.
 */
public class City {

    public static final double HALF_WIDTH = 60;

    public final double x;
    public final double y;
    private final double[] skyline;

    public boolean alive = true;
    public double flash = 0;

    public City(double x, double y, long seed) {
        this.x = x;
        this.y = y;
        this.skyline = buildSkyline(seed);
    }

    private double[] buildSkyline(long seed) {
        long s = seed;
        double[] heights = new double[6];
        for (int i = 0; i < heights.length; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            int r = (int) ((s >>> 33) & 0x7fffffff) % 44;
            heights[i] = 26 + r;
        }
        return heights;
    }

    public boolean containsX(double px) {
        return Math.abs(px - x) <= HALF_WIDTH;
    }

    public void hit() {
        if (alive) {
            alive = false;
            flash = 0.6;
        }
    }

    public void update(double dt) {
        if (flash > 0) {
            flash = Math.max(0, flash - dt);
        }
    }

    public double[] skyline() {
        return skyline;
    }
}
