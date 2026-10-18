package com.toosarax.jetjo.game;

import android.content.Context;
import android.graphics.*;
import android.util.SparseIntArray;
import android.view.*;
import com.toosarax.jetjo.core.*;
import com.toosarax.jetjo.data.*;

/** Independent perspective tracks and simultaneous touch with persistent pointer ownership. */
@android.annotation.SuppressLint("ViewConstructor")
public final class SharedDuelView extends View implements Choreographer.FrameCallback {
    public interface Host {void pulse();void finished();}
    public final DuelEngine duel;
    private final Profile profile;
    private final int[] skins;
    private final Host host;
    private final SceneAssets scene;
    private final FlightPainter[] painters={new FlightPainter(),new FlightPainter()};
    private final FlightSnapshot[] frames=new FlightSnapshot[2];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SparseIntArray fingers=new SparseIntArray(4);
    private FlightGLView gl;
    private boolean ticking,reported;
    private long previousFrame;
    private float accumulator,clock,uiTime;
    public SharedDuelView(Context context,Profile profile,int[] skins,DuelEngine duel,Host host){
        super(context);this.profile=profile;this.skins=skins.clone();this.duel=duel;this.host=host;
        scene=new SceneAssets(context);scene.select(profile.zone);capture();setFocusable(true);
        setContentDescription(profile.language.equals("fa")?"دو مسیر مجزا؛ بازیکن یک نیمهٔ چپ و بازیکن دو نیمهٔ راست را لمس می‌کند":"Two separate tracks. Player one uses the left half, player two the right half.");
    }
    private void capture(){for(int i=0;i<2;i++)frames[i]=FlightSnapshot.capture(duel.players[i],skins[i],profile.trail,clock,profile.reducedMotion,profile.highContrast,profile.zone);if(gl!=null)gl.publishPair(frames[0],frames[1],.5f);}
    public void use3d(FlightGLView renderer){
        FlightGLView previous=gl;gl=renderer;if(previous!=null&&previous!=renderer){previous.pauseScene();previous.releaseScene();}
        capture();if(gl!=null&&ticking)gl.resumeScene();invalidate();
    }
    public boolean scene3dReady(){return gl!=null&&gl.sceneReady();}
    public boolean preparing3d(){return gl!=null&&!gl.sceneReady();}
    public String graphicsStatus(){return scene3dReady()?"3D":preparing3d()?"PREPARING_3D":"2.5D";}
    public void resume(){if(ticking||duel.finished())return;ticking=true;duel.setPaused(false);previousFrame=0;accumulator=0;if(gl!=null)gl.resumeScene();Choreographer.getInstance().postFrameCallback(this);}
    public void pause(){ticking=false;duel.setPaused(true);fingers.clear();Choreographer.getInstance().removeFrameCallback(this);if(gl!=null)gl.pauseScene();}
    @Override protected void onDetachedFromWindow(){pause();scene.release();if(gl!=null){gl.releaseScene();gl=null;}super.onDetachedFromWindow();}
    @Override public void doFrame(long nanos){
        if(!ticking)return;
        float elapsed=previousFrame==0?0:Math.min(.1f,(nanos-previousFrame)/1_000_000_000f);previousFrame=nanos;clock+=elapsed;accumulator+=elapsed;
        while(accumulator>=GameConfig.STEP){duel.update(GameConfig.STEP);accumulator-=GameConfig.STEP;}capture();
        uiTime+=elapsed;if(uiTime>=.12f){uiTime=0;host.pulse();if(!ticking)return;}
        if(duel.finished()&&!reported){reported=true;pause();post(()->{if(isAttachedToWindow())host.finished();});}
        invalidate();if(ticking)Choreographer.getInstance().postFrameCallback(this);
    }
    @Override protected void onDraw(Canvas canvas){
        float half=getWidth()*.5f,height=getHeight();
        if(!scene3dReady()){scene.draw(canvas,getWidth(),height,clock,profile.reducedMotion);for(int player=0;player<2;player++){canvas.save();canvas.translate(half*player,0);canvas.clipRect(0,0,half,height);painters[player].draw(canvas,frames[player],half,height,SceneAssets.color(profile.zone));canvas.restore();}}
        paint.setColor(0xffe3fff2);paint.setStrokeWidth(getResources().getDisplayMetrics().density*2);canvas.drawLine(half,0,half,height,paint);
    }
    private void steer(MotionEvent event,int index,int player){float half=getWidth()*.5f;if(half<=0)return;int lane=scene3dReady()?gl.laneForScreenX(event.getX(index),getWidth(),getHeight(),player):FlightPerspective.laneForScreenX(half,event.getX(index)-player*half,clock,profile.reducedMotion);duel.move(player,lane);}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!ticking||duel.finished())return true;int action=event.getActionMasked(),index=event.getActionIndex();
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            int player=event.getX(index)<getWidth()*.5f?0:1;boolean occupied=false;for(int i=0;i<fingers.size();i++)if(fingers.valueAt(i)==player)occupied=true;
            if(!occupied){fingers.put(event.getPointerId(index),player);steer(event,index,player);}return true;
        }
        if(action==MotionEvent.ACTION_MOVE){for(int i=0;i<event.getPointerCount();i++){int player=fingers.get(event.getPointerId(i),-1);if(player>=0)steer(event,i,player);}return true;}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){fingers.delete(event.getPointerId(index));if(action==MotionEvent.ACTION_UP)performClick();return true;}
        if(action==MotionEvent.ACTION_CANCEL)fingers.clear();return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
