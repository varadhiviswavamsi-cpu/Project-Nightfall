// Developer: V.Viswa Vamsi
package nightfall.data;

/**
 * Top-10 high score table. Held in memory for M1; file persistence lands in
 * M4 behind this same interface.
 */
public class HighScoreStore {

    public static final int MAX_ENTRIES = 10;

    private final int[] scores = new int[MAX_ENTRIES];
    private int count = 0;

    public HighScoreStore() {
    }

    /** Inserts a score keeping the table sorted best-first, capped at top 10. */
    public void add(int score) {
        if (score <= 0) {
            return;
        }
        if (count == MAX_ENTRIES && score <= scores[count - 1]) {
            return;
        }
        int i = count;
        if (count < MAX_ENTRIES) {
            count++;
        }
        while (i > 0 && scores[i - 1] < score) {
            scores[i] = scores[i - 1];
            i--;
        }
        scores[i] = score;
    }

    /** Best score in the table, or 0 when empty. */
    public int best() {
        return count == 0 ? 0 : scores[0];
    }

    public int size() {
        return count;
    }

    /** Score at rank index (0 = best), or 0 when out of range. */
    public int get(int index) {
        if (index < 0 || index >= count) {
            return 0;
        }
        return scores[index];
    }
}
