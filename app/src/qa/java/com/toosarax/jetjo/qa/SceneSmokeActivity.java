package com.toosarax.jetjo.qa;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.toosarax.jetjo.core.DuelEngine;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.core.GameModes;
import com.toosarax.jetjo.data.Profile;
import com.toosarax.jetjo.game.FlightGLView;
import com.toosarax.jetjo.game.FlightPainter;
import com.toosarax.jetjo.game.FlightSnapshot;
import com.toosarax.jetjo.game.GameView;
import com.toosarax.jetjo.game.SceneAssets;
import com.toosarax.jetjo.game.SharedDuelView;
import com.toosarax.jetjo.multiplayer.LanProtocol;
import com.toosarax.jetjo.multiplayer.LanSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** QA-only launcher. Runs actual Android views and TCP; never records economy or purchases. */
public final class SceneSmokeActivity extends Activity {
    private static final String TAG="JetQA";
    private static final String[] LABELS={"Solo 3D first frame", "Shared score 3D + multi-touch",
        "Shared action 3D + independent controls", "Shared co-op Canvas 2.5D",
        "Shared action Canvas 2.5D", "Pause / resume / detach lifecycle",
        "Android TCP host + join", "LAN paired 3D (62/38)",
        "LAN Canvas + input / pause", "LAN real 75-second finish + cleanup",
        "Actual activity action 3D + HUD", "Actual activity Canvas selector + team",
        "Actual activity 320x480dp / font 1.6", "Atomic profile / economy regression", "Audio focus / closed callbacks", "Original v1 profile migration"};
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<String> results=new ArrayList<>();
    private FrameLayout scene;
    private LinearLayout report;
    private ScrollView reportScroll;
    private TextView current;
    private Profile profile;
    private GameView solo;
    private SharedDuelView shared;
    private FlightGLView gl;
    private LanCanvas lanCanvas;
    private LanSession lanHost,lanGuest;
    private LanProtocol.Snapshot lanState;
    private int graphicsCase,token,networkToken,completed,failed,notTested,currentNetworkCase=6;
    private boolean destroyed,done,handoff,activityChecksStarted,persistenceChecksStarted,lifecycleOkay=true,hostConnected,guestConnected,hostFinished,guestFinished,networkFailed;
    private long touchStart;
    private interface Check { boolean ready(); }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        // QA captures must not include unrelated notification content on the emulator.
        try{sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_CLOSE_SYSTEM_DIALOGS));}catch(RuntimeException ignored){}
        if(com.toosarax.jetjo.BuildConfig.GALLERY_CAPTURE){done=true;startActivity(new android.content.Intent(this,GalleryActivity.class));finish();return;}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        profile=new Profile(this);profile.language="en";profile.relaxed=true;profile.skin=18;profile.zone=0;profile.sound=false;profile.music=false;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(10),dp(12),dp(10));root.setBackgroundColor(0xff07101e);
        TextView title=text("JET · Android render + socket QA",19,0xffe3fff2);root.addView(title);
        current=text("Starting isolated QA build…",12,0xffffdc86);root.addView(current);
        reportScroll=new ScrollView(this);report=new LinearLayout(this);report.setOrientation(LinearLayout.VERTICAL);reportScroll.addView(report);
        root.addView(reportScroll,new LinearLayout.LayoutParams(-1,dp(210)));
        scene=new FrameLayout(this);root.addView(scene,new LinearLayout.LayoutParams(-1,0,1));
        TextView scope=text("One Android device / emulator. Two physical phones are NOT tested.",11,0xff94a6bf);root.addView(scope);
        setContentView(root);main.postDelayed(this::nextGraphics,200);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setPadding(0,dp(3),0,dp(3));return view;}
    void record(int index,String status,String detail){
        String line=(index+1)+". "+status+" · "+LABELS[index]+"\n   "+detail;
        results.add(line);completed++;if(status.equals("FAIL"))failed++;if(status.equals("NOT TESTED"))notTested++;
        report.addView(text(line,11,status.equals("PASS")?0xff9af6d0:status.equals("FAIL")?0xffff8b92:0xffffdc86));Log.i(TAG,line.replace('\n',' '));
    }
    private void require(boolean okay,String reason){if(!okay)throw new IllegalStateException(reason);}
    private void later(int expected,long delay,Runnable action){main.postDelayed(()->{if(!destroyed&&!done&&token==expected)action.run();},delay);}
    private void await(int expected,Check check,long timeout,Runnable success,Runnable timeoutAction){
        long deadline=SystemClock.uptimeMillis()+timeout;
        Runnable poll=new Runnable(){@Override public void run(){if(destroyed||done||token!=expected)return;if(check.ready())success.run();else if(SystemClock.uptimeMillis()>=deadline)timeoutAction.run();else main.postDelayed(this,80);}};
        main.post(poll);
    }
    private void clearScene(){
        if(solo!=null){solo.pause();solo=null;}if(shared!=null){shared.pause();shared=null;}
        if(gl!=null){gl.releaseScene();gl=null;}if(lanCanvas!=null){lanCanvas.release();lanCanvas=null;}
        if(scene!=null)scene.removeAllViews();
    }
    private void mount(View view){scene.addView(view,new FrameLayout.LayoutParams(-1,-1));}
    private void nextGraphics(){
        if(destroyed||done)return;clearScene();
        if(graphicsCase==5){record(5,lifecycleOkay?"PASS":"FAIL",lifecycleOkay?"Clocks frozen while paused; input ownership cleared; removed GL scenes released.":"A pause or detached-view check failed.");startNetwork();return;}
        int index=graphicsCase++,expected=++token;current.setText("Running "+LABELS[index]);
        if(index==0)soloCase(expected,index);else sharedCase(expected,index,index<=2,index==2||index==4,index==3);
    }
    private String gpuStatus(){return gl==null?"No GPU scene":gl.failureReason().isEmpty()?"First GLES2 frame confirmed":gl.failureReason();}
    private void soloCase(int expected,int index){
        gl=new FlightGLView(this,profile);mount(gl);
        solo=new GameView(this,profile,false,GameModes.STUNT,0,927,new GameView.Host(){public void pulse(){}public void event(int event,int detail){}public void finished(){}});
        solo.use3d(gl);mount(solo);
        // Exercise the waiting renderer before attaching a surface can produce its first frame.
        solo.layout(0,0,dp(320),dp(480));
        if(!gl.sceneReady()&&!canvasHasContent(solo)){finishGraphics(expected,index,"FAIL","Solo waiting-GPU Canvas was blank");return;}
        solo.resume();
        await(expected,()->solo!=null&&solo.getWidth()>0&&gl.sceneReady(),6500,()->later(expected,1500,()->{
            try{
                require(solo.graphicsStatus()==2,"GameView did not report a real GLES frame");
                dispatch(solo,MotionEvent.ACTION_DOWN,0,new int[]{7},new float[]{2},new float[]{solo.getHeight()*.7f});
                dispatch(solo,MotionEvent.ACTION_UP,0,new int[]{7},new float[]{2},new float[]{solo.getHeight()*.7f});
                require(solo.engine.targetLane==0,"Solo edge tap did not select left lane");
                testSoloPointers(solo);
                require(solo.engine.jump(),"Solo action input rejected");
                dispatch(solo,MotionEvent.ACTION_DOWN,0,new int[]{7},new float[]{2},new float[]{solo.getHeight()*.7f});
                float time=solo.engine.seconds;int pausedLane=solo.engine.targetLane;solo.pause();
                later(expected,220,()->{
                    try{require(solo.engine.seconds==time,"Paused solo clock advanced");solo.resume();
                        dispatch(solo,MotionEvent.ACTION_MOVE,0,new int[]{7},new float[]{solo.getWidth()-2},new float[]{solo.getHeight()*.7f});
                        dispatch(solo,MotionEvent.ACTION_UP,0,new int[]{7},new float[]{solo.getWidth()-2},new float[]{solo.getHeight()*.7f});
                        require(solo.engine.targetLane==pausedLane,"Solo pause retained a stale gesture");
                        later(expected,350,()->{try{require(solo.engine.seconds>time,"Solo clock did not resume");finishGraphics(expected,index,"PASS","Waiting-GPU Canvas; real GLES frame; primary pointer ownership; stale paused gesture cleared; jump and resume.");}catch(RuntimeException error){finishGraphics(expected,index,"FAIL",error.getMessage());}});
                    }catch(RuntimeException error){finishGraphics(expected,index,"FAIL",error.getMessage());}
                });
            }catch(RuntimeException error){finishGraphics(expected,index,"FAIL",error.getMessage());}
        }),()->finishGraphics(expected,index,"FAIL",gpuStatus()));
    }
    private void testSoloPointers(GameView view){
        float width=view.getWidth(),y=view.getHeight()*.7f;
        view.engine.moveTo(1);
        dispatch(view,MotionEvent.ACTION_DOWN,0,new int[]{7},new float[]{width*.5f},new float[]{y});
        dispatch(view,MotionEvent.ACTION_POINTER_DOWN,1,new int[]{7,41},new float[]{width*.5f,2},new float[]{y,y});
        // Keep the primary in the center when it lifts; the remaining finger must not steer.
        dispatch(view,MotionEvent.ACTION_POINTER_UP,0,new int[]{7,41},new float[]{width*.5f,2},new float[]{y,y});
        int lane=view.engine.targetLane;
        dispatch(view,MotionEvent.ACTION_MOVE,0,new int[]{41},new float[]{width-2},new float[]{y});
        dispatch(view,MotionEvent.ACTION_UP,0,new int[]{41},new float[]{width-2},new float[]{y});
        require(view.engine.targetLane==lane,"Secondary finger inherited the solo gesture");
        // Pointer array order is not stable: movement must follow ID 7, not index zero.
        view.engine.moveTo(1);
        dispatch(view,MotionEvent.ACTION_DOWN,0,new int[]{7},new float[]{width*.5f},new float[]{y});
        dispatch(view,MotionEvent.ACTION_POINTER_DOWN,1,new int[]{7,41},new float[]{width*.5f,2},new float[]{y,y});
        dispatch(view,MotionEvent.ACTION_MOVE,0,new int[]{41,7},new float[]{2,width*.5f},new float[]{y,y});
        require(view.engine.targetLane==1,"Solo gesture followed pointer index instead of ID");
        dispatch(view,MotionEvent.ACTION_CANCEL,0,new int[]{41,7},new float[]{2,width*.5f},new float[]{y,y});
    }
    private void sharedCase(int expected,int index,boolean useGl,boolean action,boolean cooperative){
        DuelEngine duel=new DuelEngine(927,cooperative,true,action);
        if(useGl){gl=new FlightGLView(this,profile);mount(gl);}
        shared=new SharedDuelView(this,profile,new int[]{18,35},duel,new SharedDuelView.Host(){public void pulse(){}public void finished(){}});
        if(useGl)shared.use3d(gl);mount(shared);shared.resume();
        await(expected,()->shared!=null&&shared.getWidth()>0&&(!useGl||shared.scene3dReady()),6500,()->later(expected,1000,()->{
            try{
                testPointers(shared,duel);
                if(action){require(duel.jump(0),"Player 1 jump rejected");require(duel.duck(1),"Player 2 duck rejected");require(duel.players[1].jumpTime==0&&duel.players[0].duckTime==0,"Action ownership leaked");require(duel.boost(0),"Player 1 turbo rejected");require(duel.players[1].overdrive==0,"Turbo affected both players");
                    duel.players[0].spawn(GameEngine.JUMP_BARRIER,1,.25f);duel.players[1].spawn(GameEngine.DUCK_BEAM,1,.25f);
                }
                later(expected,1200,()->{
                    try{
                        if(useGl)require(shared.scene3dReady(),"Paired GPU frame lost readiness");else require(canvasHasContent(shared),"Canvas renderer produced a blank frame");
                        shared.pause();float time=duel.players[0].seconds;int lane=duel.players[0].targetLane;
                        require(!duel.jump(1)&&!duel.boost(1),"Paused action input accepted");
                        dispatch(shared,MotionEvent.ACTION_MOVE,0,new int[]{7},new float[]{2},new float[]{shared.getHeight()*.7f});
                        later(expected,220,()->{
                            try{require(time==duel.players[0].seconds,"Paused shared clock advanced");require(lane==duel.players[0].targetLane,"Paused touch altered lane");shared.resume();
                                dispatch(shared,MotionEvent.ACTION_MOVE,0,new int[]{7},new float[]{2},new float[]{shared.getHeight()*.7f});require(lane==duel.players[0].targetLane,"Pause retained stale pointer ownership");
                                later(expected,300,()->{try{require(duel.players[0].seconds>time,"Shared clock did not resume");finishGraphics(expected,index,"PASS",(useGl?"Real paired GLES2":"Actual Canvas pixels")+"; two pointers, cross-boundary ownership, pause/resume"+(action?", isolated jump/duck/turbo.":"."));}catch(RuntimeException error){finishGraphics(expected,index,"FAIL",error.getMessage());}});
                            }catch(RuntimeException error){lifecycleOkay=false;finishGraphics(expected,index,"FAIL",error.getMessage());}
                        });
                    }catch(RuntimeException error){finishGraphics(expected,index,"FAIL",error.getMessage());}
                });
            }catch(RuntimeException error){finishGraphics(expected,index,"FAIL",error.getMessage());}
        }),()->finishGraphics(expected,index,"FAIL",useGl?gpuStatus():"Shared view layout timed out"));
    }
    private void finishGraphics(int expected,int index,String status,String detail){
        if(token!=expected||destroyed||done)return;record(index,status,detail);if(status.equals("FAIL"))lifecycleOkay=false;
        GameEngine run=solo==null?null:solo.engine;DuelEngine duel=shared==null?null:shared.duel;
        float time=run!=null?run.seconds:duel!=null?duel.players[0].seconds:0;FlightGLView prior=gl;
        clearScene();later(expected,180,()->{if((run!=null&&run.seconds!=time)||(duel!=null&&duel.players[0].seconds!=time)||(prior!=null&&prior.sceneReady()))lifecycleOkay=false;nextGraphics();});
    }
    void testPointers(SharedDuelView view,DuelEngine duel){
        float width=view.getWidth(),left=2,right=width-2,middle=width*.5f-2,y=view.getHeight()*.70f;
        dispatch(view,MotionEvent.ACTION_DOWN,0,new int[]{7},new float[]{left},new float[]{y});
        dispatch(view,MotionEvent.ACTION_POINTER_DOWN,1,new int[]{7,41},new float[]{left,right},new float[]{y,y});
        require(duel.players[0].targetLane==0&&duel.players[1].targetLane==2,"Two-pointer lane selection failed");
        dispatch(view,MotionEvent.ACTION_MOVE,0,new int[]{7,41},new float[]{right,left},new float[]{y,y});
        require(duel.players[0].targetLane==2&&duel.players[1].targetLane==0,"Crossing halves reassigned pointer ownership");
        dispatch(view,MotionEvent.ACTION_POINTER_UP,1,new int[]{7,41},new float[]{right,left},new float[]{y,y});
        dispatch(view,MotionEvent.ACTION_POINTER_DOWN,1,new int[]{7,9},new float[]{left,width*.25f},new float[]{y,y});
        dispatch(view,MotionEvent.ACTION_MOVE,0,new int[]{7,9},new float[]{middle,left},new float[]{y,y});
        require(duel.players[0].targetLane==2&&duel.players[1].targetLane==0,"Second finger stole occupied player controls");
        dispatch(view,MotionEvent.ACTION_CANCEL,0,new int[]{7,9},new float[]{middle,left},new float[]{y,y});
        dispatch(view,MotionEvent.ACTION_DOWN,0,new int[]{7},new float[]{right},new float[]{y});
        require(duel.players[1].targetLane==2,"Cancelled pointer was not reusable");
        dispatch(view,MotionEvent.ACTION_CANCEL,0,new int[]{7},new float[]{right},new float[]{y});
    }
    private void dispatch(View view,int action,int actionIndex,int[] ids,float[] xs,float[] ys){
        MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        long now=SystemClock.uptimeMillis();if(action==MotionEvent.ACTION_DOWN)touchStart=now;
        for(int i=0;i<ids.length;i++){props[i]=new MotionEvent.PointerProperties();props[i].id=ids[i];props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=xs[i];coords[i].y=ys[i];coords[i].pressure=1;coords[i].size=.1f;}
        int combined=action|(actionIndex<<MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        MotionEvent event=MotionEvent.obtain(touchStart,now,combined,ids.length,props,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        try{view.dispatchTouchEvent(event);}finally{event.recycle();}
    }
    boolean canvasHasContent(View view){
        if(view.getWidth()<=0||view.getHeight()<=0)return false;
        Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        try{view.draw(new Canvas(bitmap));int first=bitmap.getPixel(1,1),different=0;for(int y=0;y<bitmap.getHeight();y+=Math.max(1,bitmap.getHeight()/30))for(int x=0;x<bitmap.getWidth();x+=Math.max(1,bitmap.getWidth()/30))if(bitmap.getPixel(x,y)!=first)different++;return different>80;}finally{bitmap.recycle();}
    }

    private void startNetwork(){
        int expected=++token,connection=++networkToken;currentNetworkCase=6;
        current.setText("Checking Android private IPv4 for real TCP…");
        Thread discovery=new Thread(()->{
            List<String> addresses=LanSession.localAddresses();
            main.post(()->{
                if(destroyed||done||token!=expected||networkToken!=connection)return;
                if(addresses.isEmpty()){skipNetwork("NOT TESTED","No private IPv4 on this Android device.");return;}
                try{lanHost=LanSession.host(addresses.get(0),profile.zone,18,main::post,networkListener(connection,true));}
                catch(IOException|RuntimeException error){skipNetwork("FAIL","TCP bind failed: "+error.getMessage());return;}
                await(expected,()->hostConnected&&guestConnected&&lanState!=null&&lanState.tick>0,12000,()->{
                    record(6,"PASS","Real sockets on Android private IPv4; host and guest received snapshots.");currentNetworkCase=7;network3d(expected);
                },()->skipNetwork("FAIL","Host / guest handshake or first snapshot timed out."));
            });
        },"jet-qa-network-discovery");discovery.setDaemon(true);discovery.start();
    }
    private LanSession.Listener networkListener(int connection,boolean host){
        return new LanSession.Listener(){
            private boolean valid(){return !destroyed&&!done&&!networkFailed&&networkToken==connection;}
            public void room(String address,int port,String pin){
                if(!valid()||!host)return;
                try{lanGuest=LanSession.join(address,port,pin,35,main::post,networkListener(connection,false));}
                catch(IOException|RuntimeException error){skipNetwork("FAIL","Android TCP join failed: "+error.getMessage());}
            }
            public void connected(boolean hosting){if(!valid())return;if(host)hostConnected=true;else guestConnected=true;}
            public void snapshot(LanProtocol.Snapshot state){
                if(!valid())return;
                if(host){if(state.finished)hostFinished=true;}else{lanState=state;if(state.finished)guestFinished=true;renderLan(state);}
            }
            public void failed(String reason){if(valid()&&!hostFinished&&!guestFinished)skipNetwork("FAIL","Android TCP session ended: "+reason);}
        };
    }
    private void renderLan(LanProtocol.Snapshot state){
        if(gl!=null){gl.setWorld(state.world);gl.publishPair(FlightSnapshot.fromPilot(state.pilots[0],state.seconds,state.world,false,false),FlightSnapshot.fromPilot(state.pilots[1],state.seconds,state.world,false,false),.62f);}
        if(lanCanvas!=null){lanCanvas.state=state;lanCanvas.invalidate();}
    }
    private void network3d(int expected){
        clearScene();current.setText("Running LAN paired GLES2 with actual received snapshots…");
        gl=new FlightGLView(this,profile);mount(gl);renderLan(lanState);gl.resumeScene();
        await(expected,()->gl!=null&&gl.sceneReady(),7000,()->later(expected,2000,()->{
            if(gl==null||!gl.sceneReady()){skipNetwork("FAIL","LAN GPU readiness was lost.");return;}
            record(7,"PASS","Received TCP pilots rendered in one real EGL surface, 62/38 split.");currentNetworkCase=8;networkCanvas(expected);
        }),()->{
            record(7,"FAIL",gpuStatus());currentNetworkCase=8;networkCanvas(expected);
        });
    }
    private void networkCanvas(int expected){
        clearScene();current.setText("Running LAN Canvas and remote input / pause acknowledgement…");
        lanCanvas=new LanCanvas();mount(lanCanvas);renderLan(lanState);
        lanHost.input(0,false);lanGuest.input(2,false);
        await(expected,()->lanState!=null&&lanState.pilots[0].lane<.05f&&lanState.pilots[1].lane>1.95f,5000,()->{
            lanGuest.input(2,true);
            await(expected,()->lanState!=null&&lanState.paused,5000,()->{
                float time=lanState.seconds;later(expected,400,()->{
                    if(lanState==null||!lanState.paused||lanState.seconds!=time){skipNetwork("FAIL","Remote pause did not freeze the real host clock.");return;}
                    lanGuest.input(2,false);
                    await(expected,()->lanState!=null&&!lanState.paused&&lanState.seconds>time+.1f,5000,()->later(expected,1600,()->{
                        try{require(canvasHasContent(lanCanvas),"LAN Canvas frame was blank");record(8,"PASS","Actual received frames; isolated lanes; guest pause froze host, then resumed.");currentNetworkCase=9;networkFinish(expected);}
                        catch(RuntimeException error){skipNetwork("FAIL",error.getMessage());}
                    }),()->skipNetwork("FAIL","Remote resume was not acknowledged."));
                });
            },()->skipNetwork("FAIL","Remote pause was not acknowledged."));
        },()->skipNetwork("FAIL","Host / guest lane inputs were not acknowledged."));
    }
    private void networkFinish(int expected){
        progressNetwork(expected);
        await(expected,()->hostFinished&&guestFinished&&lanState!=null&&lanState.finished,110000,()->{
            try{
                require(lanState.seconds==LanProtocol.MATCH_SECONDS,"Terminal TCP duration mismatch");
                LanSession host=lanHost,guest=lanGuest;host.close();guest.close();
                await(expected,()->host.isClosed()&&guest.isClosed()&&lanWorkersStopped(),5000,()->{
                    record(9,"PASS","75 real simulation seconds; both final callbacks, terminal ACK, socket workers stopped.");finishReport();
                },()->skipNetwork("FAIL","Session workers did not stop after final result cleanup."));
            }catch(RuntimeException error){skipNetwork("FAIL",error.getMessage());}
        },()->skipNetwork("FAIL","Real 75-second TCP match did not finish within 110 seconds."));
    }
    private void progressNetwork(int expected){
        if(destroyed||done||networkFailed||token!=expected||currentNetworkCase!=9)return;
        float seconds=lanState==null?0:lanState.seconds;
        current.setText("Real Android TCP match: "+Math.round(seconds)+" / 75 seconds. Waiting for both final results…");
        later(expected,500,()->progressNetwork(expected));
    }
    private boolean lanWorkersStopped(){
        for(Thread thread:Thread.getAllStackTraces().keySet())if(thread.isAlive()&&(thread.getName().equals("jetjo-lan-session")||thread.getName().equals("jetjo-lan-reader")))return false;
        return true;
    }
    private void skipNetwork(String status,String detail){
        if(done||destroyed||networkFailed)return;networkFailed=true;networkToken++;token++;
        for(int index=currentNetworkCase;index<10;index++)record(index,status,index==currentNetworkCase?detail:"Blocked by the previous network case: "+detail);
        finishReport();
    }
    private void finishReport(){
        if(!activityChecksStarted&&completed>=10&&!destroyed){
            activityChecksStarted=true;handoff=true;token++;networkToken++;main.removeCallbacksAndMessages(null);clearScene();
            if(lanHost!=null)lanHost.close();if(lanGuest!=null)lanGuest.close();
            current.setText("Opening the actual SharedDeviceActivity for UI / selector / layout QA…");
            ((QaApplication)getApplication()).begin(this);return;
        }
        if(done||destroyed)return;
        if(completed>=13&&!persistenceChecksStarted){
            persistenceChecksStarted=true;
            try{record(13,"PASS",ProfileAudioAudit.profile(this));}catch(Exception error){record(13,"FAIL",error.getClass().getSimpleName()+": "+error.getMessage());}
            try{record(14,"PASS",ProfileAudioAudit.audio(this));}catch(Exception error){record(14,"FAIL",error.getClass().getSimpleName()+": "+error.getMessage());}
            try{record(15,"PASS",LegacyProfileAudit.run(this));}catch(Exception error){record(15,"FAIL",error.getClass().getSimpleName()+": "+error.getMessage());}
        }
        done=true;token++;networkToken++;main.removeCallbacksAndMessages(null);clearScene();
        if(lanHost!=null)lanHost.close();if(lanGuest!=null)lanGuest.close();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String summary="QA COMPLETE · "+(completed-failed-notTested)+" PASS · "+failed+" FAIL · "+notTested+" NOT TESTED";
        current.setText(summary);current.setTextColor(failed==0&&notTested==0?0xff9af6d0:0xffffdc86);
        report.removeAllViews();for(String line:results){String first=line.split("\\n",2)[0];report.addView(text(first,11,first.contains("FAIL")?0xffff8b92:first.contains("NOT TESTED")?0xffffdc86:0xff9af6d0));}
        reportScroll.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(400)));
        TextView detail=text(summary+"\n\nAndroid "+android.os.Build.VERSION.RELEASE+" / "+android.os.Build.MODEL+"\n\nActual game views, GLES first-frame checks and Android TCP sockets.\n\nTwo physical phones, Bluetooth and store purchases: NOT TESTED.",16,0xffe3fff2);
        scene.addView(detail,new FrameLayout.LayoutParams(-1,-1));Log.i(TAG,summary);
    }
    void actualActivityFinished(){handoff=false;finishReport();}
    /** Uses the same actual scene assets/painters as production, with received detached TCP pilots. */
    private final class LanCanvas extends View {
        private final SceneAssets assets=new SceneAssets(SceneSmokeActivity.this);
        private final FlightPainter[] painters={new FlightPainter(),new FlightPainter()};
        private LanProtocol.Snapshot state;
        LanCanvas(){super(SceneSmokeActivity.this);}
        void release(){assets.release();}
        @Override protected void onDraw(Canvas canvas){
            LanProtocol.Snapshot snapshot=state;if(snapshot==null)return;float width=getWidth(),height=getHeight(),split=width*.62f;
            assets.select(snapshot.world);assets.draw(canvas,width,height,snapshot.seconds,false);
            for(int player=0;player<2;player++){
                float left=player==0?0:split,trackWidth=player==0?split:width-split;canvas.save();canvas.translate(left,0);canvas.clipRect(0,0,trackWidth,height);
                painters[player].draw(canvas,FlightSnapshot.fromPilot(snapshot.pilots[player],snapshot.seconds,snapshot.world,false,false),trackWidth,height,SceneAssets.color(snapshot.world));canvas.restore();
            }
        }
    }
    @Override protected void onPause(){if(solo!=null)solo.pause();if(shared!=null)shared.pause();if(gl!=null)gl.pauseScene();super.onPause();}
    @Override protected void onStop(){super.onStop();if(!done&&!destroyed&&!handoff){token++;networkToken++;activityChecksStarted=true;for(int index=completed;index<LABELS.length;index++)record(index,"NOT TESTED","QA activity left the foreground before this case completed.");finishReport();}}
    @Override protected void onDestroy(){destroyed=true;token++;networkToken++;main.removeCallbacksAndMessages(null);clearScene();if(lanHost!=null)lanHost.close();if(lanGuest!=null)lanGuest.close();super.onDestroy();}
}
