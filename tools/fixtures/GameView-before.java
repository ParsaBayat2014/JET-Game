package com.toosarax.jetjo.game;

import android.content.Context;
import android.graphics.*;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import com.toosarax.jetjo.core.*;
import com.toosarax.jetjo.data.*;
import java.util.Random;

/** Hardware accelerated Canvas, fixed simulation step, bounded pools, vsync scheduling. */
// Constructed programmatically with explicit game dependencies; never inflated from XML.
@android.annotation.SuppressLint("ViewConstructor")
public final class GameView extends View implements Choreographer.FrameCallback {
    public interface Host {void pulse();void event(int event,int detail);void finished();}
    public final GameEngine engine;
    private final Profile profile;
    private final Host host;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final RectF rect=new RectF();
    private final float[] px=new float[160],py=new float[160],vx=new float[160],vy=new float[160],life=new float[160];
    private final int[] pc=new int[160];
    private final Random random=new Random(17);
    private boolean ticking,reported,swiped;
    private long last;
    private float accumulator,uiTimer,clock,shake,downX;
    private int particleCursor;
    private Shader background;
    private final SceneAssets scene;
    public GameView(Context c,Profile profile,boolean tutorial,int mode,int stageIndex,long seed,Host host){
        super(c);this.profile=profile;this.host=host;engine=new GameEngine(seed);scene=new SceneAssets(c);
        scene.select(mode==GameModes.CAMPAIGN?SceneAssets.campaignWorld(StageCatalog.get(stageIndex).world):(profile.skin>=16&&profile.skin<18)?6:(profile.skin>=13&&profile.skin<16)?1:(profile.skin>=10&&profile.skin<13)?0:profile.zone);
        engine.listener=(event,detail,lane,y)->{
            burst(lane,y,event==GameEngine.DEATH?0xffff758d:Catalog.COLORS[profile.skin],event==GameEngine.DEATH?40:12);
            if(event==GameEngine.HIT||event==GameEngine.DEATH)shake=profile.reducedMotion?0:.18f;
            host.event(event,detail);
        };
        engine.mode=mode;engine.stageIndex=stageIndex;engine.relaxed=profile.relaxed;engine.start(tutorial);setFocusable(true);
        setContentDescription(profile.language.equals("fa")?"مسیر پرواز؛ برای حرکت مسیر چپ، وسط یا راست را لمس کنید":"Flight track. Tap a lane or swipe to steer.");
    }
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){background=new LinearGradient(0,0,w,h,new int[]{0xff080d19,(profile.skin>=16&&profile.skin<18)?0xff302713:(profile.skin>=13&&profile.skin<16)?0xff351c20:(profile.skin>=10&&profile.skin<13)?0xff102d3b:profile.zone==1?0xff23162f:profile.zone==2?0xff271e20:0xff0b242b},null,Shader.TileMode.CLAMP);}
    public void resume(){if(ticking)return;ticking=true;last=0;accumulator=0;Choreographer.getInstance().postFrameCallback(this);}
    public void pause(){ticking=false;Choreographer.getInstance().removeFrameCallback(this);}
    @Override protected void onDetachedFromWindow(){pause();scene.release();super.onDetachedFromWindow();}
    @Override public void doFrame(long nanos){
        if(!ticking)return;
        float dt=last==0?0:Math.min(.1f,(nanos-last)/1_000_000_000f);last=nanos;clock+=dt;accumulator+=dt;
        while(accumulator>=GameConfig.STEP){engine.update(GameConfig.STEP);accumulator-=GameConfig.STEP;}
        for(int i=0;i<life.length;i++)if(life[i]>0){life[i]-=dt;px[i]+=vx[i]*dt;py[i]+=vy[i]*dt;}
        shake=Math.max(0,shake-dt);uiTimer+=dt;
        if(uiTimer>=.12f){uiTimer=0;host.pulse();if(!ticking)return;}
        if(!engine.alive&&!reported){reported=true;postDelayed(()->{if(isAttachedToWindow())host.finished();},480);}
        invalidate();if(ticking)Choreographer.getInstance().postFrameCallback(this);
    }
    private void burst(float lane,float y,int color,int count){
        if(profile.reducedMotion)return;
        for(int n=0;n<count;n++){int i=particleCursor++%life.length;px[i]=lane;py[i]=y;vx[i]=(random.nextFloat()-.5f)*2.5f;vy[i]=(random.nextFloat()-.5f)*.4f;life[i]=.25f+random.nextFloat()*.4f;pc[i]=color;}
    }
    private float lane(float x){return getWidth()*(.20f+.30f*x);}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight();scene.draw(c,w,h,clock,profile.reducedMotion);
        p.setColor(profile.highContrast?0xb3080d19:0x88080d19);c.drawRect(0,0,w,h,p);
        if(shake>0){c.save();c.translate((float)Math.sin(clock*100)*w*.005f,0);}
        p.setColor(profile.highContrast?0xff678793:0xff284956);p.setStrokeWidth(w*.002f);
        float offset=profile.reducedMotion?0:(clock*engine.speed*.6f)% .12f;
        for(float y=offset;y<1;y+=.12f)c.drawLine(w*.055f,h*y,w*.945f,h*y,p);
        for(int i=0;i<4;i++){float x=w*(.05f+.3f*i);c.drawLine(x,0,x,h,p);}
        for(int i=0;i<28;i++){float x=((i*137.3f)%1000)/1000*w;float y=((i*.073f+(profile.reducedMotion?0:clock*.025f))%1)*h;p.setColor(i%3==0?0xff3c656f:0xff253849);c.drawCircle(x,y,i%3==0?2:1,p);}
        p.setColor(engine.overdrive>0?0x25f9d779:0x148cffb9);c.drawRect(lane(engine.targetLane)-w*.145f,0,lane(engine.targetLane)+w*.145f,h,p);
        if(engine.waveWarning>0&&!profile.reducedMotion){
            p.setColor(0xffa2ffd0);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(w*.055f);c.drawText("↓",lane(engine.safeLane),h*.08f,p);
        }
        for(GameEngine.Entity e:engine.entities){
            if(!e.active)continue;float x=lane(e.lane),y=e.y*h,s=w*.063f;
            if(e.type==GameEngine.WALL){
                rect.set(x-w*.103f,y-h*.022f,x+w*.103f,y+h*.022f);p.setColor(0x30ff718a);c.drawRoundRect(rect,6,6,p);p.setColor(0xffff718a);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRoundRect(rect,6,6,p);p.setStyle(Paint.Style.FILL);
                p.setStrokeWidth(w*.006f);
                if(e.pattern==GameEngine.GUARDIAN){p.setColor(0xffffc779);c.drawLine(x-s,y-s*.5f,x,y,p);c.drawLine(x,y,x+s,y-s*.5f,p);}
                if(e.pattern==GameEngine.METEORS){p.setColor(0xffffa287);c.drawCircle(x,y,s*.58f,p);}
                if(e.style==1){c.drawCircle(x,y,s*.4f,p);}else if(e.style==2){c.drawLine(x-s*.8f,y,x+s*.8f,y,p);}
                c.drawLine(x-s*.32f,y-s*.24f,x+s*.32f,y+s*.24f,p);c.drawLine(x+s*.32f,y-s*.24f,x-s*.32f,y+s*.24f,p);
            }else if(e.type==GameEngine.ENERGY){
                p.setColor(0x228cffb9);c.drawCircle(x,y,s*.85f,p);p.setColor(0xffcaffde);path.reset();path.moveTo(x,y-s*.45f);path.lineTo(x+s*.3f,y);path.lineTo(x,y+s*.45f);path.lineTo(x-s*.3f,y);path.close();c.drawPath(path,p);
            }else{
                int type=e.type-2;int[] colors=POWER_COLORS;p.setColor(colors[type]);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(x,y,s,p);p.setStyle(Paint.Style.FILL);
                p.setColor((colors[type]&0xffffff)|0x25000000);c.drawCircle(x,y,s*.88f,p);p.setColor(colors[type]);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(s*.95f);c.drawText(Catalog.POWER_ICONS[type],x,y+s*.34f,p);
            }
        }
        for(int i=0;i<life.length;i++)if(life[i]>0){p.setColor(pc[i]);p.setAlpha((int)(Math.min(1,life[i]*2)*255));c.drawCircle(lane(px[i]),py[i]*h,w*.004f,p);}p.setAlpha(255);
        if(engine.alive){
            float x=lane(engine.playerLane),y=h*GameConfig.PLAYER_Y;
            drawTrail(c,w,h);
            if(engine.overdrive>0){p.setColor(0x48ffd77f);c.drawCircle(x,y,w*.13f,p);p.setStyle(Paint.Style.STROKE);p.setColor(0xffffdf86);p.setStrokeWidth(3);c.drawCircle(x,y,w*.095f,p);p.setStyle(Paint.Style.FILL);}
            if(engine.power[0]>0){p.setStyle(Paint.Style.STROKE);p.setColor(POWER_COLORS[0]);p.setStrokeWidth(2);c.drawCircle(x,y,w*.09f,p);p.setStyle(Paint.Style.FILL);}
            if(engine.power[1]>0){p.setColor(0x257deaff);c.drawCircle(x,y,w*.25f,p);}
            if(engine.invulnerable<=0 || ((int)(clock*15))%2==0)Art.ship(c,p,path,x,y,w*.055f,profile.skin,clock,!profile.reducedMotion);
        }
        if(shake>0)c.restore();
        p.setShader(null);p.setStyle(Paint.Style.FILL);
    }
    private void drawTrail(Canvas c,float w,float h){
        if(profile.reducedMotion)return;
        float x=lane(engine.playerLane),y=h*GameConfig.PLAYER_Y,s=w*.009f;
        int color=Catalog.COLORS[profile.skin];
        for(int i=1;i<=12;i++){
            float yy=y+i*h*.012f;float sway=(float)Math.sin(clock*5-i*.28f)*w*.007f;
            p.setColor(profile.trail==4?RAINBOW[i%RAINBOW.length]:color);p.setAlpha(Math.max(15,115-i*8));
            if(profile.trail==2){c.drawCircle(x-w*.018f+sway,yy,s*(1-i*.055f),p);c.drawCircle(x+w*.018f-sway,yy,s*(1-i*.055f),p);}
            else if(profile.trail==3){c.drawCircle(x+(float)Math.sin(i*2.7f+clock*2)*w*.022f,yy,s*.45f,p);}
            else if(profile.trail==5){float rr=s*(1-i*.045f);path.reset();path.moveTo(x+sway,yy-rr);path.lineTo(x+sway+rr,yy);path.lineTo(x+sway,yy+rr);path.lineTo(x+sway-rr,yy);path.close();c.drawPath(path,p);}
            else if(profile.trail==6){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,w*.002f));c.drawCircle(x+sway,yy,s*(.8f+i*.09f),p);p.setStyle(Paint.Style.FILL);}
            else if(profile.trail==7){float r=s*(1-i*.045f);float xx=x+((i%3)-1)*w*.013f;c.drawRect(xx-r,yy-r,xx+r,yy+r,p);}
            else if(profile.trail==8){float r=s*(1-i*.045f),xx=x+sway;path.reset();path.moveTo(xx,yy+r);path.cubicTo(xx-r*2,yy-r*.1f,xx-r*.7f,yy-r*1.5f,xx,yy-r*.55f);path.cubicTo(xx+r*.7f,yy-r*1.5f,xx+r*2,yy-r*.1f,xx,yy+r);path.close();c.drawPath(path,p);}
            else if(profile.trail==9){float phase=clock*5-i*.55f,xx=(float)Math.sin(phase)*w*.025f;c.drawCircle(x+xx,yy,s*.7f,p);p.setColor(RAINBOW[(i+2)%RAINBOW.length]);p.setAlpha(Math.max(15,115-i*8));c.drawCircle(x-xx,yy,s*.7f,p);}
            else if(profile.trail==10){float xx=x+(float)Math.sin(clock*2+i*4.1f)*w*.045f;p.setColor(0xffd8ff9b);p.setAlpha(Math.max(20,130-i*8));c.drawCircle(xx,yy,s*.55f,p);p.setAlpha(Math.max(8,40-i*2));c.drawCircle(xx,yy,s*2,p);}
            else if(profile.trail==11){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,w*.002f));rect.set(x-w*.015f-i*w*.002f,yy-s*.5f,x+w*.015f+i*w*.002f,yy+s*.5f);c.drawOval(rect,p);p.setStyle(Paint.Style.FILL);}
            else c.drawCircle(x+sway,yy,s*(profile.trail==1?1.8f:1.15f)*(1-i*.055f),p);
        }p.setAlpha(255);
    }
    private static final int[] RAINBOW={0xffff9c87,0xffffd77f,0xff8cffb9,0xff6ae4ff,0xffb9a3ff,0xffff9ccd};

    private static final int[] POWER_COLORS={0xff7beaff,0xffc5a1ff,0xffffd77f,0xff8cffb9};
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!ticking||!engine.alive)return true;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();swiped=false;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && Math.abs(e.getX()-downX)>getWidth()*.12f){engine.moveTo(engine.targetLane+(e.getX()>downX?1:-1));downX=e.getX();swiped=true;return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){if(!swiped)engine.moveTo(Math.max(0,Math.min(2,(int)(e.getX()/getWidth()*3))));performClick();return true;}
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
