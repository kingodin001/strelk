import pro.strelk.game.GameEngine;
import pro.strelk.game.LevelFactory;

public final class EngineSmokeTest {
    public static void main(String[] args) {
        for (int level=1; level<=1200; level++) {
            LevelFactory.Level c = LevelFactory.at(level);
            if(c.shots<1 || c.shots+c.angles.length>19 || c.angles.length<2) throw new AssertionError("invalid level "+level);
        }
        GameEngine g = new GameEngine(1);
        if(g.remaining()!=4 || !g.shoot() || g.shoot()) throw new AssertionError("shoot locking failed");
        for(int i=0;i<100;i++) g.tick(.01);
        if(g.remaining()!=3 && g.state()!=GameEngine.State.LOST) throw new AssertionError("flight missing");
        if(GameEngine.angularDistance(0,Math.PI*2-.03)>.031) throw new AssertionError("wrapping failed");
        System.out.println("All 1200 levels and engine smoke tests passed.");
    }
}
