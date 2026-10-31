package pro.strelk.game;

import java.util.Random;

/** Original deterministic configurations: inspired by timing puzzles, not extracted from reference game. */
public final class LevelFactory {
    public static final int TOTAL=1200;
    private LevelFactory(){}
    public static Level at(int request){
        int n=Math.max(1,Math.min(TOTAL,request));
        Random rng=new Random(0x4A25FE19L+n*17713L);
        int starting=n<4?0:n<15?1:n<36?2:n<101?3:Math.min(8,3+(n-100)/150);
        int shots=n<4?5:n<18?6:n<36?7:n<90?6:Math.min(11,7+(n-90)/200);
        shots=Math.min(shots,19-starting);
        double[] angles=new double[starting];
        // Start pins spread far enough apart for hit-boxes to fit.
        double offset=(rng.nextDouble()-.5)*Math.PI;
        for(int i=0;i<starting;i++)angles[i]=Math.PI*2*i/Math.max(1,starting)+offset+(rng.nextDouble()-.5)*.11;
        int tier=(n-1)/20;
        double speed=Math.min(2.9,.54+tier*.042+(n%8)*.034)*(n%9>=5?-1:1);
        int mode=n<=8?0:n<=45?(n/12)%2:n%4;
        return new Level(n,shots,angles,speed,mode);
    }
    public static final class Level {
        public final int index,shots,movementMode;
        public final double[] angles;
        public final double speed;
        public Level(int index,int shots,double[] angles,double speed,int movementMode){
            this.index=index;this.shots=shots;this.angles=angles;this.speed=speed;this.movementMode=movementMode;
        }
    }
}