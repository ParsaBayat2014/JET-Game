package com.toosarax.jetjo.game;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import com.toosarax.jetjo.core.*;
import com.toosarax.jetjo.data.*;
import com.toosarax.jetjo.ui.Words;
import java.util.Random;

/** Fixed-step simulation with either a perspective Canvas scene or a detached GLES scene underneath. */
@android.annotation.SuppressLint("ViewConstructor")
public final class GameView extends View implements Choreographer.FrameCallback {
    public interface Host {void pulse();void event(int event,int detail);void finished();}
    public final GameEngine engine;
    private final Profile profile;
    private final Host host;
    private final Words words;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final FlightPainter painter=new FlightPainter();
    private final SceneAssets scene;
    private final Typeface font;
    private final Random random=new Random(17);
    private final float[] px=new float[160],py=new float[160],vx=new float[160],vy=new float[160],life=new float[160];
    private final int[] colors=new int[160];
    private FlightSnapshot frame;
    private FlightGLView gl;
    private boolean ticking,reported,horizontal,vertical;
    private long last;
    private float accumulator,uiTimer,clock,shake,flash,downX,downY,calloutTime;
    private int cursor,primaryPointer=-1;
    private String callout="";
    public GameView(Context context,Profile profile,boolean tutorial,int mode,int stageIndex,long seed,Host host){
        super(context);this.profile=profile;this.host=host;words=new Words(profile);font=Typeface.createFromAsset(context.getAssets(),"fonts/bold.ttf");
        engine=new GameEngine(seed);scene=new SceneAssets(context);
        int world=mode==GameModes.CAMPAIGN?SceneAssets.campaignWorld(StageCatalog.get(stageIndex).world):(profile.skin>=16&&profile.skin<18)?6:(profile.skin>=13&&profile.skin<16)?1:(profile.skin>=10&&profile.skin<13)?0:profile.zone;
        scene.select(world);
        engine.listener=(event,detail,lane,y)->{
            burst(lane,y,event==GameEngine.HIT||event==GameEngine.DEATH?0xffff718f:event==GameEngine.BOSS_BREAK?0xffffd779:Catalog.COLORS[profile.skin],event==GameEngine.DEATH||event==GameEngine.BOSS_BREAK?40:8);
            if(event==GameEngine.HIT||event==GameEngine.DEATH){shake=profile.reducedMotion?0:.20f;flash=profile.reducedMotion?0:.20f;}
            if(event==GameEngine.COMBO)announce(words.t("CHAIN ×","کمبو ×")+words.n(engine.multiplier()),.70f);
            else if(event==GameEngine.NEAR)announce(words.t("CLOSE CALL +35","عبور نزدیک ۳۵+"),.65f);
            else if(event==GameEngine.OVERDRIVE)announce(words.t("TURBO!","توربو!"),.85f);
            else if(event==GameEngine.STUNT)announce(detail==GameEngine.BOOST_RING?words.t("FUEL +23%","سوخت ۲۳٪+"):words.t("PERFECT +150","عالی ۱۵۰+"),.65f);
            else if(event==GameEngine.BOSS)announce(words.t("GUARDIAN ","نگهبان ")+words.n(detail),1.0f);
            else if(event==GameEngine.BOSS_BREAK)announce(words.t("GUARDIAN DOWN +1500","نگهبان شکست خورد ۱۵۰۰+"),1.2f);
            else if(event==GameEngine.POWER)announce(words.t(Catalog.POWERS[detail]),.7f);
            host.event(event,detail);
        };
        engine.mode=mode;engine.stageIndex=stageIndex;engine.relaxed=profile.relaxed;engine.start(tutorial);
        frame=FlightSnapshot.capture(engine,profile.skin,profile.trail,0,profile.reducedMotion,profile.highContrast,world);
        setFocusable(true);
        setContentDescription(words.t("Tap a lane or swipe sideways to steer. In Action Flight, swipe up to jump and down to duck.","مسیر را لمس کن یا چپ و راست بکش. در پرواز اکشن، بالا بکش تا بپری و پایین بکش تا خم شوی."));
    }
    public void use3d(FlightGLView renderer){
        FlightGLView previous=gl;gl=renderer;
        if(previous!=null&&previous!=renderer){previous.pauseScene();previous.releaseScene();}
        if(gl!=null){gl.setWorld(scene.selected());gl.publish(engine,clock);if(ticking)gl.resumeScene();}
        frame=FlightSnapshot.capture(engine,profile.skin,profile.trail,clock,profile.reducedMotion,profile.highContrast,scene.selected());invalidate();
    }
    /** 0: perspective Canvas, 1: waiting for GPU, 2: first 3D frame has rendered successfully. */
    public int graphicsStatus(){return gl==null?0:gl.sceneReady()?2:1;}
    public void resume(){if(ticking)return;ticking=true;last=0;accumulator=0;if(gl!=null)gl.resumeScene();Choreographer.getInstance().postFrameCallback(this);}
    public void pause(){ticking=false;clearGesture();Choreographer.getInstance().removeFrameCallback(this);if(gl!=null)gl.pauseScene();}
    @Override protected void onDetachedFromWindow(){pause();scene.release();if(gl!=null){gl.releaseScene();gl=null;}super.onDetachedFromWindow();}
    @Override public void doFrame(long nanos){
        if(!ticking)return;
        float dt=last==0?0:Math.min(.1f,(nanos-last)/1_000_000_000f);last=nanos;clock+=dt;accumulator+=dt;
        while(accumulator>=GameConfig.STEP){engine.update(GameConfig.STEP);accumulator-=GameConfig.STEP;}
        for(int i=0;i<life.length;i++)if(life[i]>0){life[i]-=dt;px[i]+=vx[i]*dt;py[i]+=vy[i]*dt;}
        shake=Math.max(0,shake-dt);flash=Math.max(0,flash-dt);calloutTime=Math.max(0,calloutTime-dt);
        if(gl!=null)gl.publish(engine,clock);
        if(gl==null||!gl.sceneReady())frame=FlightSnapshot.capture(engine,profile.skin,profile.trail,clock,profile.reducedMotion,profile.highContrast,scene.selected());
        uiTimer+=dt;if(uiTimer>=.12f){uiTimer=0;host.pulse();if(!ticking)return;}
        if(!engine.alive&&!reported){reported=true;postDelayed(()->{if(isAttachedToWindow())host.finished();},480);}
        invalidate();if(ticking)Choreographer.getInstance().postFrameCallback(this);
    }
    private void announce(String value,float seconds){callout=value;calloutTime=seconds;}
    private void burst(float lane,float y,int color,int count){
        if(profile.reducedMotion)return;
        for(int n=0;n<count;n++){int i=cursor++%life.length;px[i]=lane;py[i]=y;vx[i]=(random.nextFloat()-.5f)*2.2f;vy[i]=(random.nextFloat()-.65f)*.32f;life[i]=.3f+random.nextFloat()*.35f;colors[i]=color;}
    }
    @Override protected void onDraw(Canvas canvas){
        float width=getWidth(),height=getHeight();if(width<=0||height<=0)return;
        if(gl==null||!gl.sceneReady()){
            scene.draw(canvas,width,height,clock,profile.reducedMotion);
            canvas.save();if(shake>0)canvas.translate((float)Math.sin(clock*100)*width*.007f,0);
            painter.draw(canvas,frame,width,height,SceneAssets.color(scene.selected()));
            for(int i=0;i<life.length;i++)if(life[i]>0){paint.setColor(colors[i]);paint.setAlpha((int)(Math.min(1,life[i]*2)*255));canvas.drawCircle(FlightPerspective.screenX(width,px[i],py[i],clock,profile.reducedMotion),FlightPerspective.screenY(height,py[i]),width*.004f,paint);}
            paint.setAlpha(255);canvas.restore();
        }
        if(flash>0){paint.setColor(0xffff6585);paint.setAlpha((int)(flash*170));canvas.drawRect(0,0,width,height,paint);paint.setAlpha(255);}
        if(calloutTime>0&&!engine.tutorial){
            paint.setColor(0xc4081020);rect.set(width*.12f,height*.275f,width*.88f,height*.355f);canvas.drawRoundRect(rect,12,12,paint);
            paint.setTypeface(font);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.max(16*getResources().getDisplayMetrics().scaledDensity,width*.044f));
            float max=width*.72f;while(paint.measureText(callout)>max&&paint.getTextSize()>10*getResources().getDisplayMetrics().scaledDensity)paint.setTextSize(paint.getTextSize()-1);
            paint.setColor(engine.overdrive>0?0xffffd779:0xffe2fff2);canvas.drawText(callout,width*.5f,height*.325f,paint);
        }
        if(engine.mode==GameModes.STUNT&&!engine.tutorial&&engine.seconds<3){
            paint.setTypeface(font);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(width*.20f);paint.setColor(0xffedfff7);canvas.drawText(words.n(3-(int)engine.seconds),width*.5f,height*.52f,paint);
            paint.setTextSize(width*.044f);canvas.drawText(words.t("UP: JUMP · DOWN: DUCK","بالا: پرش · پایین: خم‌شدن"),width*.5f,height*.59f,paint);
        }
    }
    private void clearGesture(){primaryPointer=-1;horizontal=vertical=false;}
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_CANCEL||!ticking||!engine.alive){clearGesture();return true;}
        if(action==MotionEvent.ACTION_DOWN){
            primaryPointer=event.getPointerId(0);downX=event.getX();downY=event.getY();horizontal=vertical=false;return true;
        }
        if(primaryPointer<0)return true;
        int index=event.findPointerIndex(primaryPointer);
        if(index<0){clearGesture();return true;}
        if(action==MotionEvent.ACTION_MOVE){
            float x=event.getX(index),dx=x-downX,dy=event.getY(index)-downY;
            if(!vertical&&!horizontal&&engine.mode==GameModes.STUNT&&Math.abs(dy)>Math.max(30*getResources().getDisplayMetrics().density,getHeight()*.09f)&&Math.abs(dy)>Math.abs(dx)*1.25f){
                if(dy<0)engine.jump();else engine.duck();vertical=true;return true;
            }
            if(!vertical&&Math.abs(dx)>getWidth()*.12f){engine.moveTo(engine.targetLane+(dx>0?1:-1));downX=x;horizontal=true;}
            return true;
        }
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
            if(event.getPointerId(event.getActionIndex())!=primaryPointer)return true;
            if(!vertical&&!horizontal&&getWidth()>0)engine.moveTo(gl!=null&&gl.sceneReady()?gl.laneForScreenX(event.getX(index),getWidth(),getHeight()):FlightPerspective.laneForScreenX(getWidth(),event.getX(index),clock,profile.reducedMotion));
            clearGesture();performClick();return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
