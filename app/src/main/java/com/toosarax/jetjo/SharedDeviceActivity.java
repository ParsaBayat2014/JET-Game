package com.toosarax.jetjo;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.toosarax.jetjo.audio.Feedback;
import com.toosarax.jetjo.billing.Entitlements;
import com.toosarax.jetjo.core.*;
import com.toosarax.jetjo.data.*;
import com.toosarax.jetjo.game.*;
import com.toosarax.jetjo.ui.*;

/** Same-phone party modes. Two fingers can steer independently; no purchase or network required. */
public final class SharedDeviceActivity extends Activity implements SharedDuelView.Host {
    public static final int REQUEST_CODE=7201;
    public static void start(Activity activity){activity.startActivityForResult(new Intent(activity,SharedDeviceActivity.class),REQUEST_CODE);}
    private Profile profile;
    private Entitlements entitlements;
    private Words words;
    private Ui ui;
    private Feedback audio;
    private FrameLayout root;
    private SharedDuelView track;
    private DuelEngine duel;
    private TextView clock;
    private TextView renderStatus;
    private final TextView[] score=new TextView[2];
    private final int[] skins={0,1};
    private boolean cooperative,paused,reported,use3d,actionFlight,compactHud,gentle=true;
    private final Button[][] actions=new Button[2][3];
    private View pauseOverlay;
    private int matches,wins,coopWins;
    private String t(String en,String fa){return words.t(en,fa);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);profile=new Profile(this);words=new Words(profile);ui=new Ui(this,words);
        entitlements=new Entitlements(this);audio=new Feedback(this,profile);use3d=profile.render3d;
        root=new FrameLayout(this);root.setBackgroundColor(Ui.BG);setContentView(root);
        if(state!=null){matches=Math.max(0,state.getInt("matches"));wins=Math.max(0,Math.min(matches,state.getInt("wins")));coopWins=Math.max(0,Math.min(matches,state.getInt("coopWins")));for(int i=0;i<2;i++){int skin=state.getInt("skin"+i,i);if(skin>=0&&skin<Catalog.SKINS.length&&available(skin))skins[i]=skin;}cooperative=state.getBoolean("cooperative");actionFlight=state.getBoolean("actionFlight");gentle=state.getBoolean("gentle",true);profile.zone=Math.max(0,Math.min(SceneAssets.COUNT-1,state.getInt("world",profile.zone)));use3d=state.getBoolean("use3d",use3d);publishStats();}lobby();
    }
    private Button button(String label,boolean primary,Runnable run){return ui.button(label,primary,()->{audio.play(Feedback.CLICK);run.run();});}
    private void addButton(LinearLayout parent,String label,boolean primary,Runnable run){ui.add(parent,button(label,primary,run),-2,10);}
    private LinearLayout page(String title,String detail){
        if(track!=null){track.pause();track=null;}getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root.removeAllViews();pauseOverlay=null;paused=false;
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout content=ui.column();content.setPadding(ui.dp(20),ui.dp(20),ui.dp(20),ui.dp(24));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        ui.label(content,title,28,Ui.TEXT,12);ui.label(content,detail,14,Ui.MUTED,16);return content;
    }
    private boolean available(int skin){return !Progression.isPaidSkin(skin)||entitlements.skin(skin);}
    private void cycle(int player,int direction){
        for(int step=1;step<=Catalog.SKINS.length;step++){
            int candidate=Math.floorMod(skins[player]+step*direction,Catalog.SKINS.length);
            if(available(candidate)){skins[player]=candidate;break;}
        }lobby();
    }
    private void lobby(){
        LinearLayout content=page(t("JET · Two players","جت · دو نفره"),t("Share one phone. Player 1 steers on the left, player 2 on the right. Free ships are all available here.","یک گوشی، دو بازیکن. بازیکن ۱ نیمهٔ چپ و بازیکن ۲ نیمهٔ راست را کنترل می‌کند. همهٔ شخصیت‌های رایگان اینجا قابل انتخاب‌اند."));
        for(int player=0;player<2;player++){
            final int index=player;LinearLayout card=ui.card(content);
            ui.label(card,t("Player ","بازیکن ")+words.n(player+1)+" · "+words.t(Catalog.SKINS[skins[player]]),18,player==0?Ui.LIME:Ui.GOLD,10);
            ui.add(card,new HeroView(this,skins[player],true,profile.reducedMotion),110,10);
            ui.pair(card,button(t("‹ Previous","قبلی ›"),false,()->cycle(index,-1)),button(t("Next ›","‹ بعدی"),false,()->cycle(index,1)));
        }
        ui.pair(content,button(t("3D meshes","رندر سه‌بعدی"),use3d,()->{use3d=true;lobby();}),button(t("2.5D perspective","نمای ۲٫۵بعدی"),!use3d,()->{use3d=false;lobby();}));
        ui.pair(content,button(t("Gentle · both pilots","ملایم · هر دو خلبان"),gentle,()->{gentle=true;lobby();}),button(t("Standard · both pilots","استاندارد · هر دو خلبان"),!gentle,()->{gentle=false;lobby();}));
        ui.label(content,String.format(java.util.Locale.ROOT,t("Selected world: %s · build %s","جهان انتخابی: %s · ساخت %s"),profile.language.equals("fa")?SceneAssets.nameFa(profile.zone):SceneAssets.nameEn(profile.zone),words.n(BuildConfig.VERSION_CODE)),12,Ui.MUTED,12);
        ui.add(content,new SceneAssets.Preview(this,profile.zone),115,10);
        ui.pair(content,button(t("‹ Previous world","جهان قبلی ›"),false,()->{profile.zone=Math.floorMod(profile.zone-1,SceneAssets.COUNT);lobby();}),button(t("Next world ›","‹ جهان بعدی"),false,()->{profile.zone=(profile.zone+1)%SceneAssets.COUNT;lobby();}));
        addButton(content,t("⚡ Action duel · 100 seconds","⚡ رقابت اکشن · ۱۰۰ ثانیه"),true,()->launch(false,true));
        addButton(content,t("✦ Action team · Defeat 4 guardians together","✦ تیم اکشن · با هم ۴ نگهبان را شکست بدهید"),true,()->launch(true,true));
        addButton(content,t("⚔ Score duel · 75 seconds","⚔ رقابت امتیازی · ۷۵ ثانیه"),false,()->launch(false,false));
        addButton(content,t("✦ Team mission · Collect 80 sparks together","✦ مأموریت تیمی · با هم ۸۰ جرقه بگیرید"),false,()->launch(true,false));
        ui.label(content,t("Same route, same handling. Each action pilot has separate jump, duck and turbo controls. Collisions reduce score and break the combo; nobody is eliminated. Local party modes do not grant credits or XP.","مسیر و قدرت برابر. هر خلبان اکشن دکمه‌های پرش، خم‌شدن و توربو دارد. برخورد امتیاز را کم می‌کند و کمبو را می‌شکند؛ هیچ‌کس حذف نمی‌شود. حالت دونفره اعتبار یا تجربه نمی‌دهد."),14,Ui.MUTED,14);
        addButton(content,t("Home","خانه"),false,this::finish);
    }
    private void launch(boolean team,boolean action){
        if(track!=null)track.pause();root.removeAllViews();pauseOverlay=null;paused=false;reported=false;cooperative=team;
        actionFlight=action;duel=new DuelEngine(System.nanoTime(),team,gentle,action);audio.selectTrack(SceneAssets.musicTrack(profile.zone));
        for(GameEngine pilot:duel.players)pilot.listener=(event,detail,lane,y)->{if(event==GameEngine.COLLECT)audio.play(Feedback.COLLECT);else if(event==GameEngine.COMBO||event==GameEngine.OVERDRIVE)audio.play(Feedback.COMBO);else if(event==GameEngine.HIT)audio.play(Feedback.HIT);else if(event==GameEngine.POWER)audio.play(Feedback.SHIELD+detail);else if(event==GameEngine.ACTION)audio.play(detail==0?Feedback.JUMP:detail==1?Feedback.DUCK:Feedback.TURBO);else if(event==GameEngine.STUNT)audio.play(Feedback.STUNT);else if(event==GameEngine.BOSS)audio.play(Feedback.GUARDIAN);else if(event==GameEngine.BOSS_BREAK)audio.play(Feedback.GUARDIAN_BREAK);};
        LinearLayout content=ui.column();content.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);root.addView(content,new FrameLayout.LayoutParams(-1,-1));
        compactHud=getResources().getConfiguration().screenHeightDp<680||getResources().getConfiguration().fontScale>1.3f;
        LinearLayout header=ui.column();header.setPadding(ui.dp(compactHud?8:12),ui.dp(compactHud?6:10),ui.dp(compactHud?8:12),ui.dp(compactHud?4:6));
        // Keep pause beside the clock at large font sizes, preserving vertical space for both flight viewports.
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);top.setGravity(Gravity.CENTER_VERTICAL);
        clock=ui.text("",compactHud?12:14,Ui.TEXT,true);clock.setMaxLines(2);clock.setEllipsize(TextUtils.TruncateAt.END);top.addView(clock,new LinearLayout.LayoutParams(0,-2,1));
        Button pause=button(compactHud?"Ⅱ":t("Ⅱ Pause","Ⅱ مکث"),false,this::pauseMatch);pause.setContentDescription(t("Pause both flights","توقف هر دو پرواز"));
        pause.setMinimumHeight(ui.dp(48));pause.setMinHeight(ui.dp(48));pause.setPadding(ui.dp(8),ui.dp(4),ui.dp(8),ui.dp(4));
        LinearLayout.LayoutParams pauseParams=new LinearLayout.LayoutParams(compactHud?ui.dp(48):-2,ui.dp(48));pauseParams.setMarginStart(ui.dp(8));top.addView(pause,pauseParams);ui.add(header,top,-2,compactHud?4:8);
        LinearLayout scores=new LinearLayout(this);scores.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);scores.setBaselineAligned(false);
        for(int player=0;player<2;player++){
            score[player]=ui.text("",compactHud?12:14,player==0?Ui.LIME:Ui.GOLD,true);score[player].setGravity(Gravity.CENTER);score[player].setMaxLines(2);score[player].setEllipsize(TextUtils.TruncateAt.END);
            scores.addView(score[player],new LinearLayout.LayoutParams(0,-2,1));
        }ui.add(header,scores,-2,compactHud?4:6);renderStatus=ui.text("",compactHud?10:11,Ui.MUTED,false);renderStatus.setMaxLines(1);renderStatus.setEllipsize(TextUtils.TruncateAt.END);ui.add(header,renderStatus,-2,0);ui.add(content,header,-2,0);
        FrameLayout flight=new FrameLayout(this);content.addView(flight,new LinearLayout.LayoutParams(-1,0,1));
        track=new SharedDuelView(this,profile,skins,duel,this);final SharedDuelView activeTrack=track;
        if(use3d){
            FlightGLView graphics=new FlightGLView(this,profile);flight.addView(graphics,new FrameLayout.LayoutParams(-1,-1));activeTrack.use3d(graphics);
            graphics.setReadyListener(()->{if(track==activeTrack)pulse();});
            graphics.setFailureListener(()->{if(track!=activeTrack)return;activeTrack.use3d(null);flight.removeView(graphics);pulse();Toast.makeText(this,t("The lighter 2.5D view is active for this match. You can retry 3D in the lobby.","نمای سبک‌تر ۲٫۵بعدی برای این مسابقه فعال شد. از اتاق می‌توانید سه‌بعدی را دوباره امتحان کنید."),Toast.LENGTH_LONG).show();});
        }
        flight.addView(track,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout controls=new LinearLayout(this);controls.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);controls.setMotionEventSplittingEnabled(true);
        controls.setPadding(ui.dp(3),ui.dp(compactHud?4:6),ui.dp(3),ui.dp(compactHud?4:6));
        for(int player=0;player<2;player++)for(int lane=0;lane<3;lane++){
            final int pilot=player,target=lane;
            Button control=button(lane==0?"‹":lane==1?"◇":"›",false,()->{if(!paused&&duel!=null)duel.move(pilot,target);});
            control.setMinimumHeight(ui.dp(48));control.setMinHeight(ui.dp(48));control.setPadding(0,ui.dp(6),0,ui.dp(6));control.setContentDescription(t("Player ","بازیکن ")+words.n(player+1)+" · "+t("lane ","مسیر ")+words.n(lane+1));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,ui.dp(compactHud?48:54),1);params.setMargins(ui.dp(2),0,ui.dp(2),0);controls.addView(control,params);
        }
        ui.add(content,controls,-2,0);
        for(int player=0;player<2;player++)for(int actionId=0;actionId<3;actionId++)actions[player][actionId]=null;
        if(actionFlight){
            LinearLayout actionRow=new LinearLayout(this);actionRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);actionRow.setMotionEventSplittingEnabled(true);actionRow.setBaselineAligned(false);actionRow.setPadding(ui.dp(3),0,ui.dp(3),ui.dp(compactHud?4:7));
            for(int player=0;player<2;player++)for(int actionId=0;actionId<3;actionId++){
                final int pilot=player,actionIndex=actionId;String label=actionId==0?"↑":actionId==1?"↓":"⚡";
                Button control=button(label,actionId==2,()->{if(paused||duel==null)return;if(actionIndex==0)duel.jump(pilot);else if(actionIndex==1)duel.duck(pilot);else duel.boost(pilot);pulse();});
                control.setTextSize(compactHud?14:16);control.setMinimumHeight(ui.dp(48));control.setMinHeight(ui.dp(48));control.setPadding(0,ui.dp(5),0,ui.dp(5));control.setContentDescription(String.format(java.util.Locale.ROOT,t("Player %s · %s","بازیکن %s · %s"),words.n(player+1),actionId==0?t("Jump","پرش"):actionId==1?t("Duck","خم‌شدن"):t("Turbo","توربو")));
                actions[player][actionId]=control;LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,ui.dp(48),1);params.setMargins(ui.dp(2),0,ui.dp(2),0);actionRow.addView(control,params);
            }ui.add(content,actionRow,-2,0);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);pulse();track.resume();
    }
    @Override public void pulse(){
        if(duel==null||track==null)return;
        if(renderStatus!=null){renderStatus.setText(String.format(java.util.Locale.ROOT,"%s · %s · %s",t("build ","ساخت ")+words.n(BuildConfig.VERSION_CODE),track.scene3dReady()?"3D":track.preparing3d()?t("Preparing 3D…","آماده‌سازی سه‌بعدی…"):"2.5D",profile.language.equals("fa")?SceneAssets.nameFa(profile.zone):SceneAssets.nameEn(profile.zone)));renderStatus.setContentDescription(renderStatus.getText());}
        clock.setText(String.format(java.util.Locale.ROOT,t("%s seconds%s","%s ثانیه%s"),words.n((long)Math.ceil(duel.remainingSeconds())),cooperative?String.format(java.util.Locale.ROOT,actionFlight?t(" · Guardians %s / %s"," · نگهبان %s / %s"):" · ◇ %s / %s",words.n(actionFlight?duel.combinedBosses():duel.combinedEnergy()),words.n(actionFlight?DuelEngine.TEAM_GUARDIAN_TARGET:DuelEngine.TEAM_TARGET)):""));
        clock.setContentDescription(clock.getText());
        for(int player=0;player<2;player++){
            GameEngine run=duel.players[player];String detail=String.format(java.util.Locale.ROOT,actionFlight?t("P%s · %s\nCombo %s · Fuel %s%%","ب%s · %s\nکمبو %s · سوخت %s٪"):t("P%s · %s\nCombo %s · ◇ %s","ب%s · %s\nکمبو %s · ◇ %s"),words.n(player+1),words.n(run.score()),words.n(run.combo),words.n(actionFlight?Math.round(run.boostFuel*100):run.energy));
            score[player].setContentDescription(detail);
            score[player].setText(compactHud?String.format(java.util.Locale.ROOT,actionFlight?t("P%s · %s\n×%s · ⚡%s%%","ب%s · %s\n×%s · ⚡%s٪"):t("P%s · %s\n×%s · ◇%s","ب%s · %s\n×%s · ◇%s"),words.n(player+1),words.n(run.score()),words.n(run.combo),words.n(actionFlight?Math.round(run.boostFuel*100):run.energy)):detail);
            if(actionFlight){
                if(actions[player][2]!=null){actions[player][2].setEnabled(!paused&&run.boostFuel>=GameConfig.BOOST_COST&&run.overdrive<=0);actions[player][2].setAlpha(actions[player][2].isEnabled()?1:.5f);}
                if(actions[player][0]!=null)actions[player][0].setAlpha(run.jumpTime>0||run.duckTime>0?.5f:1);
                if(actions[player][1]!=null)actions[player][1].setAlpha(run.jumpTime>0||run.duckTime>0?.5f:1);
            }
        }
        audio.gameIntensity(duel.players[0].overdrive>0||duel.players[1].overdrive>0,duel.players[0].bossActive||duel.players[1].bossActive);
    }
    @Override public void finished(){
        if(reported||duel==null||!duel.finished())return;reported=true;
        matches++;if(cooperative){if(duel.teamWon())coopWins++;}else if(duel.winner()==0)wins++;
        publishStats();
        audio.play(Feedback.LEVEL);
        String title=cooperative?(duel.teamWon()?t("Team mission complete!","مأموریت تیمی کامل شد!"):t("One more try together","با هم دوباره امتحان کنید"))
                :duel.winner()<0?t("A draw!","مساوی!"):t("Player ","بازیکن ")+words.n(duel.winner()+1)+t(" wins!"," برنده شد!");
        LinearLayout content=page(title,cooperative?String.format(java.util.Locale.ROOT,actionFlight?t("Guardians defeated: %s / %s","نگهبان‌های شکست‌خورده: %s / %s"):t("Combined sparks: %s / %s","جرقه‌های مشترک: %s / %s"),words.n(actionFlight?duel.combinedBosses():duel.combinedEnergy()),words.n(actionFlight?DuelEngine.TEAM_GUARDIAN_TARGET:DuelEngine.TEAM_TARGET)):t("Same route. Two different flights.","یک مسیر؛ دو پرواز متفاوت."));
        for(int player=0;player<2;player++){
            GameEngine run=duel.players[player];LinearLayout card=ui.card(content);
            ui.label(card,t("Player ","بازیکن ")+words.n(player+1)+" · "+words.t(Catalog.SKINS[skins[player]]),18,Ui.TEXT,8);
            ui.label(card,words.n(run.score()),34,player==0?Ui.LIME:Ui.GOLD,8);
            ui.label(card,t("Sparks ","جرقه ")+words.n(run.energy)+t(" · Best combo "," · بهترین کمبو ")+words.n(run.bestCombo)+t(" · Hits "," · برخورد ")+words.n(run.hits),14,Ui.MUTED,0);
            if(actionFlight)ui.label(card,String.format(java.util.Locale.ROOT,t("Stunts %s · Guardians %s","حرکات نمایشی %s · نگهبان‌ها %s"),words.n(run.stunts),words.n(run.bossesDefeated)),14,Ui.MUTED,0);
        }
        addButton(content,t("Rematch","دوباره بازی کنیم"),true,()->launch(cooperative,actionFlight));
        addButton(content,t("Choose ships & mode","انتخاب شخصیت و حالت"),false,this::lobby);addButton(content,t("Home","خانه"),false,this::finish);
    }
    private void publishStats(){if(matches>0)setResult(RESULT_OK,new Intent().putExtra("localMatches",matches).putExtra("localWins",wins).putExtra("localCoopWins",coopWins));}
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putInt("matches",matches);state.putInt("wins",wins);state.putInt("coopWins",coopWins);
        state.putInt("skin0",skins[0]);state.putInt("skin1",skins[1]);state.putBoolean("cooperative",cooperative);state.putBoolean("use3d",use3d);state.putBoolean("actionFlight",actionFlight);state.putBoolean("gentle",gentle);state.putInt("world",profile.zone);
    }
    private void pauseMatch(){
        if(track==null||paused||reported)return;paused=true;track.pause();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xf2080d19);pauseOverlay=scroll;root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout content=ui.column();content.setGravity(Gravity.CENTER);content.setPadding(ui.dp(24),ui.dp(24),ui.dp(24),ui.dp(24));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        ui.label(content,t("Both flights are paused","هر دو پرواز متوقف‌اند"),24,Ui.TEXT,20);
        addButton(content,t("Continue together","با هم ادامه بدهیم"),true,()->{root.removeView(pauseOverlay);pauseOverlay=null;paused=false;getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);track.resume();});
        addButton(content,t("Leave unfinished match","خروج از بازی ناتمام"),false,this::lobby);
    }
    @Override public void onBackPressed(){if(track!=null&&!reported){if(!paused)pauseMatch();else lobby();}else finish();}
    @Override protected void onResume(){super.onResume();if(audio!=null)audio.resume();}
    @Override protected void onPause(){if(track!=null&&!reported)pauseMatch();if(audio!=null)audio.pause();super.onPause();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus&&track!=null&&!reported)pauseMatch();}
    @Override protected void onDestroy(){if(track!=null)track.pause();if(audio!=null)audio.release();super.onDestroy();}
}
