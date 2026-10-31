package pro.strelk.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.Locale;

/** All artwork is generated procedurally; no assets copied from Arrow. */
public final class GameView extends View {
    private enum Screen { HOME, PLAY, WIN, LOSE, SHOP, SETTINGS }
    private static final float W=420, H=900, HUB_X=210, HUB_Y=420;
    private static final int BG=0xff081522, PANEL=0xff12283b, WHITE=0xfff2f9ff,
            MUTED=0xffa6baca, MINT=0xff25e0be, ORANGE=0xffffbd59, RED=0xfffb677f;
    private static final int[] ARROW_COLORS={MINT,ORANGE,0xffac93ff,RED};
    private static final String[] SKIN_NAMES={"Неон","Солнечная","Космос","Пламя"};
    private static final int[] SKIN_PRICES={0,250,500,750};
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GameStorage storage;
    private final AdsController ads;
    private final ToneGenerator tones;
    private GameEngine engine;
    private Screen screen=Screen.HOME;
    private long lastFrame=0;
    private boolean paused=false;
    private boolean doubleClaimed=false;
    private float drift=0;
    private String toast="";
    private long toastUntil=0;
    private float scale=1, ox=0, oy=0;

    public GameView(Context context, GameStorage storage, AdsController ads) {
        super(context);
        this.storage=storage; this.ads=ads;
        this.engine=new GameEngine(storage.level());
        ToneGenerator t;
        try { t=new ToneGenerator(AudioManager.STREAM_MUSIC,45); }
        catch(Exception ignored) { t=null; }
        this.tones=t;
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        setFocusable(true);
    }

    public void setPaused(boolean value) { paused=value; lastFrame=0; invalidate(); }
    public boolean back() {
        if(screen != Screen.HOME) { screen=Screen.HOME; sound(0); invalidate(); return true; }
        return false;
    }
    private void sound(int which) {
        if(tones == null || !storage.sound())return;
        try { tones.startTone(which==2 ? ToneGenerator.TONE_PROP_ACK : which==1 ? ToneGenerator.TONE_PROP_NACK : ToneGenerator.TONE_PROP_BEEP, 75); }
        catch(Exception ignored){}
    }
    private void message(String value) { toast=value; toastUntil=System.currentTimeMillis()+1700; invalidate(); }

    @Override protected void onDraw(Canvas actual) {
        super.onDraw(actual);
        long now=System.nanoTime();
        if(lastFrame==0) lastFrame=now;
        double delta=Math.min(.08,(now-lastFrame)/1e9);
        lastFrame=now;
        if(!paused) { drift+=(float)delta*.25f; if(screen==Screen.PLAY) engine.tick(delta); }
        if(screen==Screen.PLAY && engine.state()!=GameEngine.State.PLAYING) {
            if(engine.state()==GameEngine.State.WON) {
                storage.win(engine.level().index);
                doubleClaimed=false;
                screen=Screen.WIN;
                sound(2);
            } else {
                screen=Screen.LOSE;
                sound(1);
            }
        }
        float vw=getWidth(),vh=getHeight();
        scale=Math.min(vw/W,vh/H); ox=(vw-W*scale)/2; oy=(vh-H*scale)/2;
        actual.drawColor(BG);
        Canvas c=actual;
        c.save(); c.translate(ox,oy); c.scale(scale,scale);
        background(c);
        switch(screen) {
            case HOME: drawHome(c); break;
            case PLAY: drawPlay(c); break;
            case WIN: drawResult(c,true); break;
            case LOSE: drawResult(c,false); break;
            case SHOP: drawShop(c); break;
            case SETTINGS: drawSettings(c); break;
        }
        if(System.currentTimeMillis()<toastUntil) {
            rect(c,47,796,373,847,18,PANEL);
            txt(c,toast,210,829,16,WHITE,true);
        }
        c.restore();
        if(!paused) postInvalidateDelayed(16);
    }

    private void background(Canvas c) {
        p.setShader(new LinearGradient(0,0,W,H,new int[]{0xff071727,0xff102b3d,0xff081522},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,W,H,p); p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f); p.setColor(0x252de3cf);
        for(int i=0;i<12;i++) {
            float x=(i*73+25)%470-25, y=(i*151+50)%940-20;
            c.drawCircle(x+8f*(float)Math.sin(drift+i),y,14+(i%3)*13,p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x1025e0be);
        c.drawCircle(390,185,170,p);
        c.drawCircle(-80,790,180,p);
    }
    private void header(Canvas c,String title) {
        txt(c,"‹",39,79,47,WHITE,true);
        txt(c,title,210,74,26,WHITE,true);
        pill(c,321,43,403,85,21,PANEL);
        txt(c,"◆ "+storage.coins(),362,70,14,ORANGE,true);
    }
    private void drawHome(Canvas c) {
        pill(c,34,47,386,98,24,PANEL);
        txt(c,"◆  STRELK",210,80,28,MINT,true);
        txt(c,"ТОЧНЫЙ ВЫСТРЕЛ",210,149,19,MUTED,true);
        drawWheel(c,210,360,1.02f,true);
        txt(c,"1200 уровней точности",210,590,20,WHITE,true);
        txt(c,"Попади в цель. Не зацепи стрелы.",210,619,14,MUTED,true);
        button(c,45,666,375,740,"ИГРАТЬ  •  УРОВЕНЬ "+storage.level(),MINT,BG);
        button(c,45,756,205,814,"МАГАЗИН",PANEL,WHITE);
        button(c,218,756,375,814,"НАСТРОЙКИ",PANEL,WHITE);
        txt(c,"Играй без интернета",210,858,12,MUTED,false);
    }
    private void drawPlay(Canvas c) {
        header(c,"УРОВЕНЬ "+engine.level().index);
        pill(c,40,111,380,158,19,PANEL);
        txt(c,"ОСТАЛОСЬ СТРЕЛ",125,142,15,MUTED,true);
        txt(c,String.valueOf(engine.remaining()),324,145,28,ORANGE,true);
        drawWheel(c,HUB_X,HUB_Y,1.0f,false);
        txt(c,"НАЖМИ, ЧТОБЫ ВЫСТРЕЛИТЬ",210,685,17,WHITE,true);
        drawIncomingArrow(c);
        txt(c,"Стрелы вращаются вместе с целью",210,799,14,MUTED,false);
        button(c,38,826,382,870,"НАЧАТЬ ЗАНОВО",PANEL,WHITE);
    }
    private void drawWheel(Canvas c,float cx,float cy,float mult,boolean demo) {
        float radius=151*mult;
        double rotation=demo?drift*1.9:engine.rotation();
        List<Double> arrows=engine.angles();
        // Underlay target rings create depth without clutter.
        circle(c,cx,cy,198*mult,0x0925e0be);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.6f); p.setColor(0x3325e0be);
        c.drawCircle(cx,cy,197*mult,p); c.drawCircle(cx,cy,111*mult,p);
        p.setStyle(Paint.Style.FILL);
        for(int i=0;i<arrows.size();i++) {
            double a=arrows.get(i)+rotation;
            drawArrow(c,cx+(float)Math.cos(a)*86*mult,cy+(float)Math.sin(a)*86*mult,
                    cx+(float)Math.cos(a)*radius,cy+(float)Math.sin(a)*radius,ARROW_COLORS[storage.skin()]);
        }
        circle(c,cx,cy,64*mult,0xff10283c);
        p.setColor(MINT);p.setStrokeWidth(4*mult);p.setStyle(Paint.Style.STROKE);
        c.drawCircle(cx,cy,60*mult,p);p.setStyle(Paint.Style.FILL);
        txt(c,demo?"◆":String.valueOf(engine.level().index),cx,cy+9*mult,30*mult,WHITE,true);
        if(demo) {
            drawArrow(c,cx,cy+193*mult,cx,cy+312*mult,ORANGE);
        }
    }
    private void drawIncomingArrow(Canvas c) {
        float progress=(float)engine.flightProgress();
        if(progress>=0) {
            float bottom=748-(748-503)*progress;
            drawArrow(c,210,bottom,210,bottom+106,MINT);
        } else {
            drawArrow(c,210,715,210,816,ARROW_COLORS[storage.skin()]);
            for(int i=0;i<Math.min(4,engine.remaining()-1);i++) {
                circle(c,140+i*34,758,4,0xff4b7380);
            }
        }
    }
    private void drawArrow(Canvas c,float sx,float sy,float ex,float ey,int color) {
        float dx=ex-sx,dy=ey-sy;
        float length=(float)Math.sqrt(dx*dx+dy*dy);
        if(length<1)return;
        float ux=dx/length,uy=dy/length;
        float px=-uy,py=ux;
        p.setColor(0xff06101c);p.setStrokeWidth(8);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(sx,sy,ex,ey,p);
        p.setColor(color);p.setStrokeWidth(4.5f);
        c.drawLine(sx,sy,ex,ey,p);
        // Arrowhead points inward toward the rotating hub at (sx,sy).
        Path head=new Path();
        head.moveTo(sx-ux*13,sy-uy*13);
        head.lineTo(sx+ux*15+px*11,sy+uy*15+py*11);
        head.lineTo(sx+ux*15-px*11,sy+uy*15-py*11);head.close();
        p.setColor(color);c.drawPath(head,p);
        p.setStrokeWidth(5);
        c.drawLine(ex-ux*17+px*10,ey-uy*17+py*10,ex-ux*3,ey-uy*3,p);
        c.drawLine(ex-ux*17-px*10,ey-uy*17-py*10,ex-ux*3,ey-uy*3,p);
        p.setStrokeCap(Paint.Cap.BUTT);
    }
    private void drawResult(Canvas c,boolean win) {
        header(c,win?"УРОВЕНЬ ПРОЙДЕН":"ПОПРОБУЙ ЕЩЁ");
        circle(c,210,273,108,win?0x2225e0be:0x22fb677f);
        circle(c,210,273,75,win?MINT:RED);
        txt(c,win?"✓":"×",210,298,90,BG,true);
        txt(c,win?"ОТЛИЧНЫЙ ВЫСТРЕЛ!":"СТРЕЛЫ СТОЛКНУЛИСЬ",210,423,23,WHITE,true);
        if(win) {
            txt(c,"Уровень "+engine.level().index+" пройден",210,467,18,MUTED,false);
            txt(c,"+"+(20+Math.min(80,engine.level().index/15))+" монет",210,511,28,ORANGE,true);
            button(c,40,587,380,661,"СЛЕДУЮЩИЙ УРОВЕНЬ",MINT,BG);
            if(!doubleClaimed && ads.readyForReward()) button(c,40,675,380,740,"×2 МОНЕТЫ ЗА РЕКЛАМУ",PANEL,WHITE);
        } else {
            txt(c,"Найди свободный сектор и поймай момент",210,479,16,MUTED,false);
            button(c,40,585,380,659,"ПОВТОРИТЬ",MINT,BG);
            if(ads.readyForReward()) button(c,40,674,380,740,"ПРОДОЛЖИТЬ ЗА РЕКЛАМУ",PANEL,WHITE);
        }
        button(c,40,758,380,813,"НА ГЛАВНУЮ",PANEL,MUTED);
    }
    private void drawShop(Canvas c) {
        header(c,"АРСЕНАЛ СТРЕЛ");
        txt(c,"Выбирай свой стиль",210,153,18,MUTED,false);
        for(int i=0;i<4;i++) {
            int top=179+i*130, bottom=top+113;
            pill(c,35,top,385,bottom,22,PANEL);
            drawArrow(c,91,top+22,91,top+92,ARROW_COLORS[i]);
            txt(c,SKIN_NAMES[i],194,top+45,20,WHITE,true);
            String action=storage.skin()==i?"ВЫБРАНА":storage.ownsSkin(i)?"ВЫБРАТЬ":SKIN_PRICES[i]+" ◆";
            txt(c,action,194,top+78,15,storage.skin()==i?MINT:ORANGE,true);
        }
        if(ads.readyForReward()) button(c,45,733,375,797,"+150 МОНЕТ ЗА РЕКЛАМУ",MINT,BG);
        else txt(c,"Монеты даются за пройденные уровни",210,766,14,MUTED,false);
        txt(c,"Без случайных платных покупок",210,852,13,MUTED,false);
    }
    private void drawSettings(Canvas c) {
        header(c,"НАСТРОЙКИ");
        txt(c,"Комфортная игра",210,183,24,WHITE,true);
        button(c,45,275,375,342,storage.sound()?"ЗВУК: ВКЛ":"ЗВУК: ВЫКЛ",PANEL,WHITE);
        pill(c,45,391,375,530,20,PANEL);
        txt(c,"ТВОЙ ПРОГРЕСС",210,433,16,MINT,true);
        txt(c,"Побед: "+storage.wins(),210,474,22,WHITE,true);
        txt(c,"Открыт уровень: "+storage.level(),210,507,16,MUTED,false);
        txt(c,"Разработано с нуля для STRELK",210,722,14,MUTED,false);
        txt(c,"Рекламные награды — только по желанию",210,754,13,MUTED,false);
    }
    private void button(Canvas c,float x1,float y1,float x2,float y2,String label,int bg,int fg) {
        pill(c,x1,y1+4,x2,y2+4,18,0x48000000);
        pill(c,x1,y1,x2,y2,18,bg);
        txt(c,label,(x1+x2)/2,(y1+y2)/2+7,18,fg,true);
    }
    private void rect(Canvas c,float x1,float y1,float x2,float y2,float r,int color){
        p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x1,y1,x2,y2,r,r,p);
    }
    private void pill(Canvas c,float x1,float y1,float x2,float y2,float r,int color){rect(c,x1,y1,x2,y2,r,color);}
    private void circle(Canvas c,float x,float y,float rad,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(x,y,rad,p);}
    private void txt(Canvas c,String str,float x,float y,float size,int color,boolean bold){
        p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.FILL);
        p.setTypeface(bold?Typeface.create("sans-serif-medium",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
        c.drawText(str,x,y,p);
    }
    private boolean inside(float x,float y,float x1,float y1,float x2,float y2){return x>=x1&&x<=x2&&y>=y1&&y<=y2;}

    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        float x=(e.getX()-ox)/scale, y=(e.getY()-oy)/scale;
        if(x<0||x>W||y<0||y>H)return true;
        if(screen!=Screen.HOME && inside(x,y,0,28,83,113)) { back(); return true; }
        switch(screen) {
            case HOME:
                if(inside(x,y,35,653,385,748)) { engine.start(storage.level());screen=Screen.PLAY;sound(0); }
                else if(inside(x,y,40,747,213,821)) { screen=Screen.SHOP;sound(0); }
                else if(inside(x,y,215,747,386,821)) { screen=Screen.SETTINGS;sound(0); }
                break;
            case PLAY:
                if(inside(x,y,33,818,386,884)) { engine.start(engine.level().index);sound(0); }
                else if(engine.shoot()) { sound(0); }
                break;
            case WIN:
                if(inside(x,y,40,578,380,668)) {
                    int next=storage.level();
                    if(engine.level().index>=LevelFactory.TOTAL){screen=Screen.HOME;message("Все уровни пройдены!");}
                    else { engine.start(next); screen=Screen.PLAY;ads.showInterstitialAfterLevel(storage); }
                } else if(inside(x,y,40,668,380,746) && !doubleClaimed) {
                    if(ads.showReward(()->{
                        if(!doubleClaimed) {doubleClaimed=true;storage.earn(20+Math.min(80,engine.level().index/15));message("Бонус за рекламу получен!");}
                    })) sound(0);
                } else if(inside(x,y,40,747,380,823)) screen=Screen.HOME;
                break;
            case LOSE:
                if(inside(x,y,40,577,380,667)){engine.start(engine.level().index);screen=Screen.PLAY;}
                else if(inside(x,y,40,669,380,747)) {
                    if(ads.showReward(()->{engine.continueAfterReward();screen=Screen.PLAY;message("Ещё один шанс!");}))sound(0);
                }
                else if(inside(x,y,40,747,380,823))screen=Screen.HOME;
                break;
            case SHOP:
                for(int i=0;i<4;i++) {
                    int top=179+i*130;
                    if(inside(x,y,35,top,385,top+113)) {
                        if(storage.ownsSkin(i)) {storage.selectSkin(i);sound(0);}
                        else if(storage.buySkin(i,SKIN_PRICES[i])){sound(2);message("Новая стрела открыта!");}
                        else {sound(1);message("Не хватает монет");}
                        break;
                    }
                }
                if(inside(x,y,45,725,375,805)) {
                    if(ads.showReward(()->{storage.earn(150);sound(2);message("+150 монет!");}))sound(0);
                }
                break;
            case SETTINGS:
                if(inside(x,y,45,272,375,345)){storage.sound(!storage.sound());sound(0);}
                break;
        }
        invalidate();return true;
    }
}
