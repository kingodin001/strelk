import pro.strelk.game.GameEngine;
import pro.strelk.game.LevelFactory;

public final class EngineSmokeTest {
  public static void main(String[] args) {
    for(int n=1;n<=1200;n++) {
      LevelFactory.Level lvl=LevelFactory.at(n);
      if(lvl.shots<1||lvl.angles.length+lvl.shots>19||lvl.angles.length<0)throw new AssertionError("invalid "+n);
    }
    if(LevelFactory.at(49).angles.length!=3 || LevelFactory.at(49).shots!=6)throw new AssertionError("level 49 shape");
    GameEngine g=new GameEngine(1);
    if(g.remaining()!=5||!g.shoot()||g.shoot())throw new AssertionError("shoot locking");
    for(int i=0;i<50;i++)g.tick(.01);
    if(g.remaining()!=4 && g.state()!=GameEngine.State.LOST)throw new AssertionError("flight missing");
    if(GameEngine.angularDistance(0,2*Math.PI-.03)>.031)throw new AssertionError("angle wrap");
    System.out.println("1200 generated levels and gameplay smoke tests passed");
  }
}