package pro.strelk.game;

import java.util.Random;

/** Original deterministic level generation; no original game data/assets. */
public final class LevelFactory {
    public static final int TOTAL = 1200;
    private LevelFactory() {}

    public static Level at(int requested) {
        int index = Math.max(1, Math.min(TOTAL, requested));
        Random r = new Random(0x51A713L + index * 7919L);
        int tier = (index - 1) / 30;
        int starters = index == 1 ? 2 : Math.min(9, 2 + index / 14);
        int shots = index == 1 ? 4 : Math.min(12, 4 + index / 19 + (index % 5 == 0 ? 1 : 0));
        // Keep room for every arrow. Fine timing is the challenge, not impossible boards.
        if (starters + shots > 19) shots = 19 - starters;
        double[] angles = new double[starters];
        // Distribute pins with jitter, but guarantee a free angle to start at least some levels.
        double offset = Math.PI * 0.12 + r.nextDouble() * Math.PI * 0.3;
        for (int i = 0; i < starters; i++) {
            angles[i] = (Math.PI * 2 * i / starters + offset + (r.nextDouble() - .5) * .13);
        }
        double speed = Math.min(3.15, .70 + Math.min(tier, 35) * .064 + (index % 6) * .075);
        int direction = (index / 4) % 2 == 0 ? 1 : -1;
        int mode = index < 9 ? 0 : (index / 9) % 4;
        return new Level(index, shots, angles, speed * direction, mode);
    }

    public static final class Level {
        public final int index, shots, movementMode;
        public final double[] angles;
        public final double speed;
        public Level(int index, int shots, double[] angles, double speed, int movementMode) {
            this.index = index; this.shots = shots; this.angles = angles;
            this.speed = speed; this.movementMode = movementMode;
        }
    }
}
