package pro.strelk.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

/** Independently drawn minimalist shooting game. No artwork or code copied from the reference APK. */
public final class GameView extends View {
    private enum Screen { HOME, PLAY, WIN, LOSE, SHOP, SETTINGS, TROPHIES, LEVELS }
    private static final float W=420, H=900, CENTER_X=210, CENTER_Y=387;
    private static final int BG=0xfffff8f2, BAR=0xffede8e2, BLACK=0xff080808,
            MUTED=0xff827e79, GREEN=0xff1eaa7b, GRAY=0xffd9d3cc, WHITE=Color.WHITE;
    private static final int[] ARROW_COLORS={BLACK,0xff353a48,0xffa35025,0xff187167};
    private static final String[] SKIN_NAMES={"КЛАССИКА", "СТАЛЬ", "МЕДЬ", "ИЗУМРУД"};
    private static final int[] SKIN_PRICES={0,250,500,750};
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GameStorage storage;
    private final AdsController ads;
    private final ToneGenerator tones;
    private GameEngine engine;
    private Screen screen=Screen.HOME;
    private long lastFrame=0;
    private boolean paused=false, doubled=false;
    private float scale=1, ox=0, oy=0;
    private String toast="";
    private long toastUntil=0;
    private int levelPage=0;

    public GameView(Context context,GameStorage storage,AdsController ads) {
        super(context);
        this.storage=storage;this.ads=ads;
        this.engine=new GameEngine(storage.level());
        ToneGenerator t;
        try{t=new ToneGenerator(AudioManager.STREAM_MUSIC,35);}catch(Exception ex){t=null;}
        tones=t;
        setFocusable(true);
    }
    public void setPaused(boolean value){paused=value;lastFrame=0;invalidate();}
    public boolean back(){
        if(screen==Screen.PLAY || screen==Screen.SHOP || screen==Screen.LEVELS || screen==Screen.TROPHIES || screen==Screen.SETTINGS || screen==Screen.WIN || screen==Screen.LOSE) {
            screen=Screen.HOME;invalidate();return true;
        }
        return false;
    }
    private void sound(int mode){
        if(tones==null || !storage.sound())return;
        try{tones.startTone(mode==2?ToneGenerator.TONE_PROP_ACK:mode==1?ToneGenerator.TONE_PROP_NACK:ToneGenerator.TONE_PROP_BEEP,55);}catch(Exception ignored){}
    }
    private void message(String s){toast=s;toastUntil=System.currentTimeMillis()+1750;invalidate();}
    private void startLevel(int number){engine.start(number);screen=Screen.PLAY;sound(0);invalidate();}

    @Override protected void onDraw(Canvas actual){
        super.onDraw(actual);
        long now=System.nanoTime();if(lastFrame==0)lastFrame=now;
        double dt=Math.min(.08,(now-lastFrame)/1e9);lastFrame=now;
        if(!paused && screen==Screen.PLAY)engine.tick(dt);
        if(screen==Screen.PLAY && engine.state()!=GameEngine.State.PLAYING){
            if(engine.state()==GameEngine.State.WON){storage.win(engine.level().index);doubled=false;screen=Screen.WIN;sound(2);}
            else{screen=Screen.LOSE;sound(1);}
        }
        float vw=getWidth(),vh=getHeight();
        scale=Math.min(vw/W,vh/H);ox=(vw-W*scale)/2;oy=(vh-H*scale)/2;
        actual.drawColor(BG);Canvas c=actual;
        c.save();c.translate(ox,oy);c.scale(scale,scale);
        switch(screen){
            case HOME:drawHome(c);break;
            case PLAY:drawPlay(c);break;
            case WIN:drawResult(c,true);break;
            case LOSE:drawResult(c,false);break;
            case SHOP:drawShop(c);break;
            case SETTINGS:drawSettings(c);break;
            case TROPHIES:drawTrophies(c);break;
            case LEVELS:drawLevels(c);break;
        }
        if(System.currentTimeMillis()<toastUntil){box(c,44,802,376,855,20,BLACK);text(c,toast,210,837,17,WHITE,true);}
        c.restore();
        if(!paused)postInvalidateDelayed(16);
    }

    private void header(Canvas c,String title,boolean icons){
        rect(c,0,0,420,68,BAR);
        // Back chevron uses our own simple vector paths.
        line(c,33,18,17,34,BLACK,2.4f);line(c,17,34,33,50,BLACK,2.4f);
        text(c,title,icons?160:210,44,23,BLACK,true);
        if(icons){
            navIcon(c,284,"trophy");navIcon(c,326,"shop");navIcon(c,368,"levels");navIcon(c,399,"settings");
        }
    }
    private void navIcon(Canvas c,float x,String what){
        circle(c,x,34,18.0f,BLACK);
        switch(what){
            case "trophy":
                line(c,x-6,25,x-5,36,WHITE,2);line(c,x+6,25,x+5,36,WHITE,2);
                line(c,x-6,25,x+6,25,WHITE,2);line(c,x-6,36,x+6,36,WHITE,2);
                line(c,x,37,x,43,WHITE,2);line(c,x-6,44,x+6,44,WHITE,2);break;
            case "shop":
                line(c,x-9,25,x-6,25,WHITE,2);line(c,x-6,25,x-3,38,WHITE,2);
                line(c,x-3,38,x+8,38,WHITE,2);line(c,x+8,38,x+11,28,WHITE,2);
                line(c,x-4,28,x+7,28,WHITE,2);circle(c,x-1,43,2,WHITE);circle(c,x+8,43,2,WHITE);break;
            case "levels":
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.4f);p.setColor(WHITE);
                c.drawRoundRect(x-12,28,x+12,43,7,7,p);p.setStyle(Paint.Style.FILL);
                line(c,x-7,34,x-1,34,WHITE,2);line(c,x-4,31,x-4,37,WHITE,2);
                circle(c,x+5,34,1.6f,WHITE);circle(c,x+8,38,1.6f,WHITE);break;
            default:
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(WHITE);c.drawCircle(x,34,9,p);
                c.drawCircle(x,34,4,p);p.setStyle(Paint.Style.FILL);break;
        }
    }
    private void drawHome(Canvas c){
        header(c,"STRELK",true);
        text(c,"ТОЧНОСТЬ РЕШАЕТ ВСЁ",210,156,20,BLACK,true);
        text(c,"ВЫБЕРИ МОМЕНТ ДЛЯ ВЫСТРЕЛА",210,185,13,MUTED,false);
        // Real current-level preview, not a large decorative dashboard.
        previewTarget(c,210,367);
        text(c,"Lv "+storage.level()+"/"+LevelFactory.TOTAL,210,580,28,BLACK,true);
        primaryButton(c,72,629,348,695,"ИГРАТЬ");
        secondaryButton(c,72,715,348,770,"ВЫБОР УРОВНЯ");
        text(c,"1200 уровней",210,845,16,MUTED,false);
    }
    private void previewTarget(Canvas c,float x,float y){
        circle(c,x,y,66,BLACK);text(c,"9",x,y+15,46,WHITE,true);
        for(int i=0;i<3;i++){
            double a=(Math.PI*.92)+i*.27;
            attachedArrow(c,x,y,a,i+1,BLACK);
        }
        shotCircle(c,x,635,4,BLACK);
    }
    private void drawPlay(Canvas c){
        header(c,"Lv "+engine.level().index+"/"+LevelFactory.TOTAL,true);
        drawBoard(c);
        drawQueue(c);
    }
    private void drawBoard(Canvas c){
        List<Double> pins=engine.angles();
        for(int i=0;i<pins.size();i++){
            attachedArrow(c,CENTER_X,CENTER_Y,pins.get(i)+engine.rotation(),i+1,ARROW_COLORS[storage.skin()]);
        }
        circle(c,CENTER_X,CENTER_Y,69,BLACK);
        // The original minimal contrast is kept, but all shapes/fonts are drawn in our own code.
        text(c,String.valueOf(Math.max(0,engine.remaining()+pins.size())),CENTER_X,CENTER_Y+16,49,WHITE,true);
        double f=engine.flightProgress();
        if(f>=0){
            // The numbered projectile moves towards the bottom of the hub.
            float y=(float)(615-(615-(CENTER_Y+155))*f);
            float ballY=y+12;
            line(c,CENTER_X,CENTER_Y+69,CENTER_X,ballY-17,ARROW_COLORS[storage.skin()],1.8f);
            arrowHead(c,CENTER_X,ballY-27,0,-1,ARROW_COLORS[storage.skin()]);
            shotCircle(c,CENTER_X,ballY,pins.size()+1,ARROW_COLORS[storage.skin()]);
        }
    }
    private void attachedArrow(Canvas c,float cx,float cy,double angle,int number,int color){
        float sx=cx+(float)Math.cos(angle)*69,sy=cy+(float)Math.sin(angle)*69;
        float far=193;
        float bx=cx+(float)Math.cos(angle)*far,by=cy+(float)Math.sin(angle)*far;
        float ex=cx+(float)Math.cos(angle)*(far-16),ey=cy+(float)Math.sin(angle)*(far-16);
        line(c,sx,sy,ex,ey,color,1.8f);
        // Small arrowhead ON the existing shaft, pointed inward toward the center.
        float ahx=cx+(float)Math.cos(angle)*98,ahy=cy+(float)Math.sin(angle)*98;
        arrowHead(c,ahx,ahy,(float)-Math.cos(angle),(float)-Math.sin(angle),color);
        shotCircle(c,bx,by,number,color);
    }
    private void drawQueue(Canvas c){
        int remaining=engine.remaining();
        if(remaining<=0 || engine.flightProgress()>=0)return;
        int start=engine.angles().size()+1;
        float spacing=39;
        float y0=625;
        // Keep all ammunition visible even in later levels.
        int shown=Math.min(7,remaining);
        for(int i=0;i<shown;i++)shotCircle(c,210,y0+i*spacing,start+i,ARROW_COLORS[storage.skin()]);
        if(remaining>shown)text(c,"+"+(remaining-shown),210,887,15,MUTED,true);
    }
    private void shotCircle(Canvas c,float x,float y,int number,int color){
        circle(c,x,y,17.4f,color);
        text(c,String.valueOf(number),x,y+6.5f,22,WHITE,true);
    }
    private void arrowHead(Canvas c,float x,float y,float dx,float dy,int color){
        // Arrow is deliberately small, unlike the previous oversized neon arrow.
        float px=-dy,py=dx;
        Path path=new Path();path.moveTo(x+dx*11,y+dy*11);
        path.lineTo(x-dx*3+px*4.6f,y-dy*3+py*4.6f);
        path.lineTo(x-dx*3-px*4.6f,y-dy*3-py*4.6f);path.close();
        p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawPath(path,p);
    }
    private void drawResult(Canvas c,boolean win){
        header(c,win?"УРОВЕНЬ ПРОЙДЕН":"НЕ ПОЛУЧИЛОСЬ",false);
        circle(c,210,281,80,BLACK);text(c,win?"✓":"×",210,313,100,WHITE,true);
        text(c,win?"УРОВЕНЬ "+engine.level().index+" ПРОЙДЕН":"СТРЕЛЫ СТОЛКНУЛИСЬ",210,437,22,BLACK,true);
        if(win){
            text(c,"+"+(20+Math.min(80,engine.level().index/15))+" МОНЕТ",210,488,23,BLACK,true);
            primaryButton(c,51,587,369,651,"СЛЕДУЮЩИЙ УРОВЕНЬ");
            if(ads.readyForReward() && !doubled)secondaryButton(c,51,669,369,734,"×2 МОНЕТЫ ЗА РЕКЛАМУ");
        }else{
            text(c,"Попробуй попасть между стрелами",210,497,15,MUTED,false);
            primaryButton(c,51,587,369,651,"ПОВТОРИТЬ");
            if(ads.readyForReward())secondaryButton(c,51,669,369,734,"ПРОДОЛЖИТЬ ЗА РЕКЛАМУ");
        }
        secondaryButton(c,51,778,369,833,"НА ГЛАВНУЮ");
    }
    private void drawShop(Canvas c){
        header(c,"МАГАЗИН",false);
        text(c,"СТИЛЬ СТРЕЛЫ",210,128,25,BLACK,true);
        text(c,"МОИ МОНЕТЫ: "+storage.coins(),210,165,16,MUTED,true);
        for(int i=0;i<4;i++){
            float y=205+i*125;
            outline(c,30,y,390,y+105,12,GRAY,1);
            line(c,67,y+39,67,y+73,ARROW_COLORS[i],2);
            arrowHead(c,67,y+39,0,-1,ARROW_COLORS[i]);
            shotCircle(c,67,y+80,i+1,ARROW_COLORS[i]);
            text(c,SKIN_NAMES[i],235,y+43,18,BLACK,true);
            String label=storage.skin()==i?"ВЫБРАНО":storage.ownsSkin(i)?"ВЫБРАТЬ":"◈ "+SKIN_PRICES[i];
            text(c,label,235,y+76,16,MUTED,true);
        }
        if(ads.readyForReward())primaryButton(c,52,739,368,800,"+150 МОНЕТ ЗА РЕКЛАМУ");
        else text(c,"БОНУСЫ ЗА ПОБЕДЫ",210,825,15,MUTED,false);
    }
    private void drawSettings(Canvas c){
        header(c,"НАСТРОЙКИ",false);
        text(c,"ПАРАМЕТРЫ ИГРЫ",210,180,25,BLACK,true);
        primaryButton(c,48,300,372,363,storage.sound()?"ЗВУК: ВКЛ":"ЗВУК: ВЫКЛ");
        outline(c,48,412,372,530,13,GRAY,1.5f);
        text(c,"ПРОЙДЕНО УРОВНЕЙ",210,454,17,MUTED,true);
        text(c,String.valueOf(Math.max(0,storage.level()-1)),210,497,32,BLACK,true);
        text(c,"РЕКЛАМНЫЕ БОНУСЫ ДОБРОВОЛЬНЫ",210,736,13,MUTED,false);
    }
    private void drawTrophies(Canvas c){
        header(c,"ДОСТИЖЕНИЯ",false);
        circle(c,210,250,75,BLACK);text(c,"★",210,281,75,WHITE,true);
        text(c,"ПОБЕД: "+storage.wins(),210,418,28,BLACK,true);
        text(c,"МОНЕТ: "+storage.coins(),210,466,22,MUTED,true);
        text(c,"ВПЕРЕДИ ЕЩЁ "+(LevelFactory.TOTAL-storage.level()+1)+" УРОВНЕЙ",210,523,16,MUTED,true);
        primaryButton(c,54,669,366,733,"ИГРАТЬ");
    }
    private void drawLevels(Canvas c){
        header(c,"ВЫБОР УРОВНЯ",false);
        int pageSize=25;int start=levelPage*pageSize+1;
        text(c,"УРОВНИ "+start+"–"+Math.min(1200,start+pageSize-1),210,139,18,BLACK,true);
        for(int j=0;j<25;j++){
            int row=j/5,col=j%5,level=start+j;
            int cx=54+col*78,cy=209+row*91;
            boolean unlocked=level<=storage.level();
            circle(c,cx,cy,29,unlocked?BLACK:GRAY);
            text(c,String.valueOf(level),cx,cy+6,18,unlocked?WHITE:MUTED,true);
        }
        secondaryButton(c,40,720,195,778,"← НАЗАД");
        secondaryButton(c,225,720,380,778,"ВПЕРЁД →");
        text(c,"ДОСТУПНЫ УЖЕ ПРОЙДЕННЫЕ УРОВНИ",210,827,13,MUTED,false);
    }
    private void primaryButton(Canvas c,float x1,float y1,float x2,float y2,String label){
        box(c,x1,y1,x2,y2,10,BLACK);text(c,label,(x1+x2)/2,(y1+y2)/2+7,20,WHITE,true);
    }
    private void secondaryButton(Canvas c,float x1,float y1,float x2,float y2,String label){
        outline(c,x1,y1,x2,y2,9,BLACK,2);text(c,label,(x1+x2)/2,(y1+y2)/2+6,16,BLACK,true);
    }
    private void rect(Canvas c,float x1,float y1,float x2,float y2,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRect(x1,y1,x2,y2,p);}
    private void box(Canvas c,float x1,float y1,float x2,float y2,float radius,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(x1,y1,x2,y2,radius,radius,p);}
    private void outline(Canvas c,float x1,float y1,float x2,float y2,float radius,int color,float stroke){
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(stroke);c.drawRoundRect(x1,y1,x2,y2,radius,radius,p);p.setStyle(Paint.Style.FILL);
    }
    private void circle(Canvas c,float x,float y,float r,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(x,y,r,p);}
    private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float stroke){p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(stroke);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String text,float x,float y,float size,int color,boolean bold){
        p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
        p.setTypeface(Typeface.create(bold?"sans-serif-black":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(text,x,y,p);
    }
    private boolean hit(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}
    private boolean headerClick(float x,float y){
        if(y>68)return false;
        if(x<54){back();return true;}
        if(x>=263){
            if(x<305)screen=Screen.TROPHIES;
            else if(x<347)screen=Screen.SHOP;
            else if(x<382){levelPage=(storage.level()-1)/25;screen=Screen.LEVELS;}
            else screen=Screen.SETTINGS;
            sound(0);return true;
        }
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()!=MotionEvent.ACTION_UP)return true;
        float x=(event.getX()-ox)/scale,y=(event.getY()-oy)/scale;
        if(x<0||x>W||y<0||y>H)return true;
        if(headerClick(x,y)){invalidate();return true;}
        switch(screen){
            case HOME:
                if(hit(x,y,72,623,348,702))startLevel(storage.level());
                else if(hit(x,y,72,707,348,777)){levelPage=(storage.level()-1)/25;screen=Screen.LEVELS;}
                break;
            case PLAY:
                if(engine.shoot())sound(0);
                break;
            case WIN:
                if(hit(x,y,51,579,369,659)){
                    if(engine.level().index>=LevelFactory.TOTAL){screen=Screen.HOME;message("ВСЕ УРОВНИ ПРОЙДЕНЫ");}
                    else{startLevel(storage.level());ads.showInterstitialAfterLevel(storage);}
                }else if(hit(x,y,51,663,369,740)&&!doubled){
                    if(ads.showReward(()->{if(!doubled){doubled=true;storage.earn(20+Math.min(80,engine.level().index/15));message("МОНЕТЫ ЗАЧИСЛЕНЫ");}}))sound(0);
                }else if(hit(x,y,51,769,369,845))screen=Screen.HOME;
                break;
            case LOSE:
                if(hit(x,y,51,579,369,659))startLevel(engine.level().index);
                else if(hit(x,y,51,663,369,740)){
                    if(ads.showReward(()->{engine.continueAfterReward();screen=Screen.PLAY;message("ПРОДОЛЖАЙ");}))sound(0);
                }else if(hit(x,y,51,769,369,845))screen=Screen.HOME;
                break;
            case SHOP:
                for(int i=0;i<4;i++){
                    int yy=205+i*125;
                    if(hit(x,y,30,yy,390,yy+105)){
                        if(storage.ownsSkin(i)){storage.selectSkin(i);sound(0);}
                        else if(storage.buySkin(i,SKIN_PRICES[i])){message("СТРЕЛА ОТКРЫТА");sound(2);}
                        else message("НЕ ХВАТАЕТ МОНЕТ");break;
                    }
                }
                if(hit(x,y,52,739,368,800))ads.showReward(()->{storage.earn(150);message("+150 МОНЕТ");});
                break;
            case SETTINGS:
                if(hit(x,y,48,299,372,363)){storage.sound(!storage.sound());sound(0);}
                break;
            case TROPHIES:
                if(hit(x,y,54,669,366,733))startLevel(storage.level());
                break;
            case LEVELS:
                if(hit(x,y,40,711,195,786))levelPage=Math.max(0,levelPage-1);
                else if(hit(x,y,225,711,380,786))levelPage=Math.min(47,levelPage+1);
                else if(hit(x,y,24,173,407,617)){
                    int col=Math.round((x-54)/78f), row=Math.round((y-209)/91f);
                    if(col>=0&&col<5&&row>=0&&row<5){
                        int lvl=levelPage*25+row*5+col+1;
                        if(lvl<=storage.level())startLevel(lvl);else message("УРОВЕНЬ ЗАКРЫТ");
                    }
                }
                break;
        }
        invalidate();return true;
    }
}