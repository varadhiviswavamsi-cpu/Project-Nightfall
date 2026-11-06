// Developer: V.Viswa Vamsi
package nightfall.data;

/**
 * Player preferences for PROJECT NIGHTFALL. Kept fully in memory for the M1
 * milestone; persistence on top of this same API arrives in M4.
 */
public class SettingsManager {

    /** Difficulty preset; scales incoming warhead speed. PRO is the baseline. */
    public enum Difficulty {
        NOOB,
        PRO,
        GOD;

        /** Multiplier applied to every enemy missile speed. */
        public double enemySpeedScale() {
            switch (this) {
                case NOOB:
                    return 0.45; // significantly slower — easy to intercept
                case GOD:
                    return 2.0;  // significantly faster — nearly impossible
                default: // PRO
                    return 1.0;  // baseline — current behaviour
            }
        }
    }

    public boolean sfxOn = true;
    public boolean particlesOn = true;
    public boolean screenShakeOn = true;
    public float volume = 0.8f;
    public Difficulty difficulty = Difficulty.PRO;

    public SettingsManager() {
    }
}
