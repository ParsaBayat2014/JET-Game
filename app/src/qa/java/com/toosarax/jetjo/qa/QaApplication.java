package com.toosarax.jetjo.qa;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.toosarax.jetjo.SharedDeviceActivity;
import com.toosarax.jetjo.core.DuelEngine;
import com.toosarax.jetjo.data.Profile;
import com.toosarax.jetjo.game.FlightGLView;
import com.toosarax.jetjo.game.SharedDuelView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** QA-only lifecycle observer drives the actual activity through its public Android views. */
public final class QaApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private final Handler main=new Handler(Looper.getMainLooper());
    private WeakReference<SceneSmokeActivity> owner=new WeakReference<>(null);
    private Activity child;
    private int token,caseIndex=10;
    private boolean running,started,compact,overrideApplied,expectedExit;
    private String phase="",overrideFailure="";
    private interface Check {boolean ready();}
    @Override public void onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this);}
    void begin(SceneSmokeActivity activity){
        owner=new WeakReference<>(activity);running=true;compact=false;phase="NORMAL";started=false;expectedExit=false;
        Profile isolated=new Profile(activity);isolated.language="en";isolated.tutorial=true;isolated.render3d=true;isolated.sound=false;isolated.music=false;isolated.haptic=false;isolated.save();
        activity.startActivity(new Intent(activity,SharedDeviceActivity.class));
    }
    private SceneSmokeActivity report(){return owner.get();}
    private boolean valid(int expected){return running&&token==expected&&child!=null&&!child.isFinishing()&&report()!=null;}
    private void require(boolean result,String reason){if(!result)throw new IllegalStateException(reason);}
    private void later(int expected,long delay,Runnable action){main.postDelayed(()->{if(valid(expected))guard(action);},delay);}
    private void guard(Runnable action){try{action.run();}catch(RuntimeException error){fail(error.getMessage());}}
    private void await(int expected,Check condition,long timeout,Runnable success,String timeoutReason){
        long deadline=SystemClock.uptimeMillis()+timeout;
        Runnable poll=new Runnable(){@Override public void run(){if(!valid(expected))return;try{if(condition.ready()){success.run();return;}if(SystemClock.uptimeMillis()>=deadline){fail(timeoutReason);return;}main.postDelayed(this,80);}catch(RuntimeException error){fail(error.getMessage());}}};main.post(poll);
    }
    private View root(){return child.getWindow().getDecorView();}
    private <T extends View> T find(View view,Class<T> type){
        if(type.isInstance(view))return type.cast(view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){T found=find(group.getChildAt(i),type);if(found!=null)return found;}}return null;
    }
    private void collect(View view,List<View> list){list.add(view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i),list);}}
    private List<View> views(){List<View> list=new ArrayList<>();collect(root(),list);return list;}
    private Button buttonText(String fragment){for(View view:views())if(view instanceof Button&&((Button)view).getText().toString().contains(fragment))return(Button)view;return null;}
    private Button buttonDescription(String exact){for(View view:views())if(view instanceof Button&&view.getContentDescription()!=null&&view.getContentDescription().toString().equals(exact))return(Button)view;return null;}
    private boolean visible(View view){Rect bounds=new Rect();return view!=null&&view.isShown()&&view.getWidth()>0&&view.getHeight()>0&&view.getGlobalVisibleRect(bounds)&&bounds.width()>=view.getWidth()-2&&bounds.height()>=view.getHeight()-2;}
    private boolean allTextContains(String fragment){for(View view:views())if((view instanceof TextView&&((TextView)view).getText().toString().contains(fragment))||(view.getContentDescription()!=null&&view.getContentDescription().toString().contains(fragment)))return true;return false;}
    /** Scroll the real lobby to the existing control, then activate its installed listener. */
    private void clickLobby(int expected,String fragment,Runnable after){
        Button control=buttonText(fragment);require(control!=null,"Missing actual lobby button: "+fragment);
        control.requestRectangleOnScreen(new Rect(0,0,control.getWidth(),control.getHeight()),true);
        later(expected,200,()->{require(visible(control),"Lobby control is not visible after scrolling: "+fragment);require(control.performClick(),"Lobby click rejected: "+fragment);later(expected,200,after);});
    }
    private void startNormal(){
        int expected=++token;caseIndex=10;
        await(expected,()->root().getWidth()>0&&buttonText("3D meshes")!=null&&buttonText("3D meshes").getHeight()>0,4500,()->clickLobby(expected,"3D meshes",()->clickLobby(expected,"Action duel",()->{
            await(expected,()->find(root(),SharedDuelView.class)!=null&&find(root(),SharedDuelView.class).scene3dReady(),7500,()->later(expected,700,()->testActual3d(expected)),"Actual SharedDeviceActivity did not mount a ready 3D scene");
        })),"Actual activity lobby failed its first layout");
    }
    private void assertControls(SharedDuelView track){
        require(visible(track)&&track.getHeight()>0,"Actual activity track viewport collapsed");
        for(int player=1;player<=2;player++){
            for(int lane=1;lane<=3;lane++)require(visible(buttonDescription("Player "+player+" · lane "+lane)),"Lane button outside actual window");
            for(String action:new String[]{"Jump","Duck","Turbo"})require(visible(buttonDescription("Player "+player+" · "+action)),"Action button outside actual window: "+action);
        }
        require(visible(buttonText("Pause"))||visible(buttonText("Ⅱ")),"Actual pause control is not visible");
    }
    private void clickAction(String description){Button button=buttonDescription(description);require(visible(button)&&button.isEnabled(),"Actual action button unavailable: "+description);require(button.performClick(),"Actual action listener did not handle click");}
    private void testActual3d(int expected){
        SharedDuelView track=find(root(),SharedDuelView.class);FlightGLView graphics=find(root(),FlightGLView.class);
        require(track!=null&&graphics!=null&&track.scene3dReady(),"Actual 3D scene absent");
        require(track.getParent()==graphics.getParent(),"GL and interactive track mounted into different containers");
        ViewGroup parent=(ViewGroup)track.getParent();require(parent.indexOfChild(graphics)<parent.indexOfChild(track),"GL surface covers interactive track");
        DuelEngine duel=track.duel;require(duel.actionFlight&&duel.remainingSeconds()>85&&duel.remainingSeconds()<=100,"Actual Action duel is not the 100-second mode");
        require(allTextContains("3D")&&allTextContains("seconds")&&allTextContains("Fuel"),"Actual action HUD labels missing");assertControls(track);report().testPointers(track,duel);
        clickAction("Player 1 · Jump");require(duel.players[0].jumpTime>0&&duel.players[1].jumpTime==0,"Actual Jump button ownership incorrect");
        clickAction("Player 2 · Duck");require(duel.players[1].duckTime>0&&duel.players[0].duckTime==0,"Actual Duck button ownership incorrect");
        clickAction("Player 1 · Turbo");require(duel.players[0].overdrive>0&&duel.players[1].overdrive==0,"Actual Turbo button ownership incorrect");
        Button pause=buttonText("Pause");if(pause==null)pause=buttonText("Ⅱ");require(pause!=null&&pause.performClick(),"Actual pause click failed");float time=duel.players[0].seconds;
        later(expected,250,()->{require(duel.isPaused()&&duel.players[0].seconds==time,"Actual pause overlay did not freeze both tracks");Button resume=buttonText("Continue together");require(visible(resume)&&resume.performClick(),"Actual resume control missing");
            later(expected,250,()->{require(!duel.isPaused()&&duel.players[0].seconds>time,"Actual activity did not resume");report().record(10,"PASS","Real lobby click, GL/track mount, HUD, 12 buttons, pointer ownership and pause/resume.");caseIndex=11;leaveToLobby(expected,()->startCanvas(expected));});
        });
    }
    private void leaveToLobby(int expected,Runnable after){
        Button pause=buttonText("Pause");if(pause==null)pause=buttonText("Ⅱ");require(pause!=null&&pause.performClick(),"Cannot open actual leave overlay");
        later(expected,150,()->{Button leave=buttonText("Leave unfinished match");require(visible(leave)&&leave.performClick(),"Actual leave-to-lobby control failed");later(expected,200,()->{require(find(root(),SharedDuelView.class)==null,"Old track remained mounted after actual leave");after.run();});});
    }
    private void startCanvas(int expected){
        clickLobby(expected,"2.5D perspective",()->clickLobby(expected,"Action team",()->{
            await(expected,()->find(root(),SharedDuelView.class)!=null&&find(root(),SharedDuelView.class).getHeight()>0,4500,()->later(expected,1200,()->{
                SharedDuelView track=find(root(),SharedDuelView.class);require(find(root(),FlightGLView.class)==null&&!track.preparing3d(),"2.5D lobby selector left GL mounted");
                require(track.duel.actionFlight&&track.duel.cooperative,"Actual Action team button launched the wrong mode");
                require(allTextContains("2.5D")&&allTextContains("Guardians")&&allTextContains("/ 4"),"Actual Canvas/team HUD missing");assertControls(track);
                report().testPointers(track,track.duel);require(report().canvasHasContent(track),"Actual Canvas activity view rendered blank");
                report().record(11,"PASS","Real leave/lobby/Canvas selector, action-team target 4, HUD, controls and Canvas pixels.");caseIndex=12;
                expectedExit=true;phase="COMPACT_PENDING";child.finish();
            }),"Actual Canvas activity viewport failed to lay out");
        }));
    }
    private void startCompact(){
        int expected=++token;caseIndex=12;
        if(!overrideApplied||Math.abs(child.getResources().getConfiguration().fontScale-1.6f)>.01f){
            report().record(12,"NOT TESTED","Activity-local font override unavailable: "+overrideFailure);exitToReport();return;
        }
        child.getWindow().setLayout(Math.round(320*child.getResources().getDisplayMetrics().density),Math.round(480*child.getResources().getDisplayMetrics().density));
        await(expected,()->root().getWidth()>0&&buttonText("2.5D perspective")!=null&&buttonText("2.5D perspective").getHeight()>0,4500,()->clickLobby(expected,"2.5D perspective",()->clickLobby(expected,"Action duel",()->{
            await(expected,()->find(root(),SharedDuelView.class)!=null&&find(root(),SharedDuelView.class).getHeight()>0,4500,()->later(expected,1000,()->{
                SharedDuelView track=find(root(),SharedDuelView.class);float density=child.getResources().getDisplayMetrics().density;
                require(root().getWidth()/density<=322&&root().getHeight()/density<=482,"Android did not apply the child-only 320x480dp window");
                assertControls(track);report().testPointers(track,track.duel);require(report().canvasHasContent(track),"Compact actual activity Canvas blank");
                report().record(12,"PASS","Only QA child context fontScale 1.6 and window320x480dp; real HUD/actions and nonzero track.");exitToReport();
            }),"Compact large-font actual track viewport collapsed");
        })),"Compact actual lobby failed its first layout");
    }
    private void fail(String reason){
        if(!running||report()==null)return;main.removeCallbacksAndMessages(null);token++;
        report().record(caseIndex,"FAIL",reason==null?"Actual activity test failed":reason);
        for(int index=caseIndex+1;index<13;index++)report().record(index,"NOT TESTED","Blocked by the preceding actual-activity case.");exitToReport();
    }
    private void exitToReport(){expectedExit=true;phase="REPORT_PENDING";main.removeCallbacksAndMessages(null);token++;if(child!=null)child.finish();else complete();}
    private void complete(){running=false;main.removeCallbacksAndMessages(null);unregisterActivityLifecycleCallbacks(this);SceneSmokeActivity activity=report();if(activity!=null&&!activity.isFinishing())activity.actualActivityFinished();child=null;owner.clear();}
    @Override public void onActivityPreCreated(Activity activity,Bundle saved){
        if(!running||!compact||!(activity instanceof SharedDeviceActivity))return;
        try{Configuration override=new Configuration();override.fontScale=1.6f;override.screenWidthDp=320;override.screenHeightDp=480;override.smallestScreenWidthDp=320;activity.applyOverrideConfiguration(override);overrideApplied=true;}
        catch(RuntimeException error){overrideApplied=false;overrideFailure=error.getClass().getSimpleName()+": "+error.getMessage();}
    }
    @Override public void onActivityResumed(Activity activity){
        if(!running)return;
        if(activity==report()){
            if(phase.equals("COMPACT_PENDING")){compact=true;started=false;child=null;expectedExit=false;phase="COMPACT";overrideApplied=false;overrideFailure=Build.VERSION.SDK_INT<29?"Pre-create lifecycle callback requires Android29+":"Override was not applied";activity.startActivity(new Intent(activity,SharedDeviceActivity.class));}
            else if(phase.equals("REPORT_PENDING"))complete();return;
        }
        if(activity instanceof SharedDeviceActivity&&!started){child=activity;started=true;expectedExit=false;main.post(()->guard(compact?this::startCompact:this::startNormal));}
    }
    @Override public void onActivityDestroyed(Activity activity){if(running&&activity==child&&!expectedExit)fail("Actual child activity was destroyed before QA finished");}
    @Override public void onActivityCreated(Activity activity,Bundle state){}
    @Override public void onActivityStarted(Activity activity){}
    @Override public void onActivityPaused(Activity activity){}
    @Override public void onActivityStopped(Activity activity){}
    @Override public void onActivitySaveInstanceState(Activity activity,Bundle state){}
}
