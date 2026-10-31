package pro.strelk.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure Java simulation. Each arrow travels then attaches at the hub; collisions end the round. */
public final class GameEngine {
    public enum State { PLAYING, WON, LOST }
    public static final double TWO_PI = Math.PI * 2;
    public static final double HIT_TOLERANCE = 0.205;
    public static final double FLIGHT_TIME = .24;

    private final ArrayList<Double> localAngles = new ArrayList<>();
    private LevelFactory.Level level;
    private double rotation, seconds, flightRemaining;
    private int toLaunch;
    private int successfullyPlaced;
    private State state;

    public GameEngine(int levelNum) { start(levelNum); }

    public void start(int levelNum) {
        level = LevelFactory.at(levelNum);
        localAngles.clear();
        for (double x : level.angles) localAngles.add(normalize(x));
        rotation = 0;
        seconds = 0;
        flightRemaining = 0;
        toLaunch = level.shots;
        successfullyPlaced = 0;
        state = State.PLAYING;
    }

    public void tick(double delta) {
        if (state != State.PLAYING) return;
        delta = Math.min(Math.max(delta, 0), .08);
        seconds += delta;
        double spin = level.speed;
        switch (level.movementMode) {
            case 1: spin *= 1.0 + .45 * Math.sin(seconds * 1.45); break;
            case 2: spin *= (int)(seconds / 4.8) % 2 == 0 ? 1 : -1; break;
            case 3: spin *= .68 + .52 * Math.sin(seconds * .75) * Math.sin(seconds * .75); break;
            default: break;
        }
        rotation = normalize(rotation + spin * delta);
        if (flightRemaining > 0) {
            flightRemaining -= delta;
            if (flightRemaining <= 0) attach();
        }
    }

    public boolean shoot() {
        if (state != State.PLAYING || flightRemaining > 0 || toLaunch <= 0) return false;
        flightRemaining = FLIGHT_TIME;
        return true;
    }

    private void attach() {
        double incomingLocal = normalize(Math.PI / 2 - rotation);
        for (double existing : localAngles) {
            if (angularDistance(existing, incomingLocal) < HIT_TOLERANCE) {
                state = State.LOST;
                return;
            }
        }
        localAngles.add(incomingLocal);
        toLaunch--;
        successfullyPlaced++;
        if (toLaunch == 0) state = State.WON;
    }

    /** Rewarded continue: only call after the ad network confirms the reward event. */
    public void continueAfterReward() {
        if (state != State.LOST) return;
        // Rotate one troublesome arrow out to ensure the player has a fresh attempt;
        // already mounted arrows persist, and the failed projectile remains pending.
        state = State.PLAYING;
        flightRemaining = 0;
        rotation = normalize(rotation + .32);
    }

    /** Only purchasable with real earned coins or confirmed rewarded ad. */
    public boolean clearOneArrow() {
        if (state != State.PLAYING || localAngles.size() <= level.angles.length) return false;
        localAngles.remove(localAngles.size() - 1);
        return true;
    }

    public static double normalize(double a) {
        double x = a % TWO_PI;
        return x < 0 ? x + TWO_PI : x;
    }
    public static double angularDistance(double a, double b) {
        double d = Math.abs(normalize(a) - normalize(b));
        return Math.min(d, TWO_PI - d);
    }
    public List<Double> angles() { return Collections.unmodifiableList(localAngles); }
    public double rotation() { return rotation; }
    public double flightProgress() { return flightRemaining > 0 ? 1.0 - flightRemaining / FLIGHT_TIME : -1; }
    public int remaining() { return toLaunch; }
    public int placed() { return successfullyPlaced; }
    public State state() { return state; }
    public LevelFactory.Level level() { return level; }
}
