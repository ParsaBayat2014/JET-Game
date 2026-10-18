package com.toosarax.jetjo;
import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import com.toosarax.jetjo.audio.Feedback;
import com.toosarax.jetjo.billing.*;
import com.toosarax.jetjo.core.*;
import com.toosarax.jetjo.data.*;
import com.toosarax.jetjo.game.*;
import com.toosarax.jetjo.ui.*;

public final class MainActivity extends Activity implements GameView.Host,MyketBilling.Listener {
    private Profile profile; private Words w; private Ui ui; private Feedback audio; private MyketBilling billing;
    private FrameLayout root; private LinearLayout page; private View overlay; private GameView game;
    private TextView scoreText,comboText,powerText,runTitle,renderText; private Button jumpButton,duckButton,boostButton;
    private String screen="splash",feedbackText="";
    private boolean paused,settled;
    private int mode,tutorialStep,earned,levelBefore,achBefore,selectedStage;
    private long previousBest,feedbackUntil;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String t(String en,String fa){return w.t(en,fa);}
    private String n(long v){return w.n(v);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);profile=new Profile(this);w=new Words(profile);ui=new Ui(this,w);audio=new Feedback(this,profile);billing=new MyketBilling(this,this);
        if(Progression.isPaidSkin(profile.skin)&&!billing.entitlements.skin(profile.skin))profile.skin=0;
        root=new FrameLayout(this);root.setBackgroundColor(Ui.BG);setContentView(root);
        LinearLayout splash=ui.column();splash.setGravity(Gravity.CENTER);root.addView(splash,new FrameLayout.LayoutParams(-1,-1));
        ui.add(splash,new HeroView(this,profile.skin,true,profile.reducedMotion),180,15);
        TextView title=ui.text(t("JET","جت"),42,Ui.LIME,true);title.setGravity(Gravity.CENTER);ui.add(splash,title,-2,8);
        TextView sub=ui.text(t("2.0 · Play together","۲٫۰ · با هم بازی کنیم"),12,Ui.MUTED,false);sub.setGravity(Gravity.CENTER);ui.add(splash,sub,-2,0);
        handler.postDelayed(()->{if(!isFinishing()){if(profile.tutorial)menu();else onboarding();if(billing.configured())billing.connect();}},550);
    }
    private Button button(String title,boolean primary,Runnable action){return ui.button(title,primary,()->{audio.play(Feedback.CLICK);action.run();});}
    private void btn(LinearLayout p,String title,boolean primary,Runnable action){ui.add(p,button(title,primary,action),-2,10);}
    private void heading(LinearLayout p,String title){ui.add(p,ui.text(title,28,Ui.TEXT,true),-2,10);}
    private void body(LinearLayout p,String title){ui.label(p,title,14,Ui.MUTED,14);}
    private void base(String name,String title,String subtitle){
        if(game!=null){game.pause();game=null;}audio.gameIntensity(false,false);screen=name;paused=false;overlay=null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);root.removeAllViews();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));page=ui.column();page.setPadding(ui.dp(22),ui.dp(20),ui.dp(22),ui.dp(24));scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        if(!name.equals("menu")&&!name.equals("onboarding"))btn(page,t("‹ Home","خانه ›"),false,this::menu);
        if(!title.isEmpty())heading(page,title);if(!subtitle.isEmpty())body(page,subtitle);
    }
    private void menu(){
        if(profile.resetDay())profile.save();base("menu","","");ui.label(page,t("JET / GALAXY EXPEDITION","جت / سفر کهکشانی"),11,Ui.LIME,14);
        ui.pair(page,ui.text(t("PILOT LEVEL ","سطح خلبان ")+n(profile.level()),14,Ui.TEXT,true),ui.text("◇ "+n(profile.coins),16,Ui.GOLD,true));
        if(billing.entitlements.owned(Products.SUPPORTER))ui.label(page,t("✦ FOUNDING PILOT","✦ خلبان حامی"),12,Ui.GOLD,8);
        TextView title=ui.text(t("JET","جت"),58,Ui.TEXT,true);title.setTextDirection(w.rtl()?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR);title.setLineSpacing(-ui.dp(10),1);ui.add(page,title,-2,0);
        ui.add(page,new HeroView(this,profile.skin,false,profile.reducedMotion),140,5);
        ui.label(page,t("JUMP. DODGE. BOOST. TAKE ON THE GUARDIANS.","بپر، جاخالی بده، توربو بزن و نگهبان‌ها را شکست بده."),12,Ui.MUTED,15);
        ui.label(page,t("Version 2 · Build ","نسخهٔ ۲ · ساخت ")+n(BuildConfig.VERSION_CODE),11,Ui.MUTED,4);
        ui.label(page,w.t(Cosmetics.BADGES[profile.badge]),12,Ui.GOLD,10);
        LinearLayout stats=ui.card(page);ui.label(stats,t("ACTION FLIGHT BEST","رکورد پرواز اکشن"),11,Ui.MUTED,3);ui.label(stats,n(profile.stuntHigh),30,Ui.TEXT,6);
        int level=profile.level();long start=Progression.threshold(level),end=Progression.threshold(level+1);
        ui.progress(stats,(int)(profile.xp-start),(int)(end-start));ui.label(stats,n(profile.xp-start)+" / "+n(end-start)+" XP",11,Ui.MUTED,0);
        btn(page,t("⚡ ACTION FLIGHT","⚡ شروع پرواز اکشن"),true,()->startRun(false,GameModes.STUNT));
        btn(page,t("★ Campaign · 48 destinations","★ سفر داستانی · ۴۸ مقصد"),true,this::campaign);
        btn(page,t("Seven ways to fly","هفت جور پرواز"),false,this::modes);
        btn(page,t("⚔ Two players · Phone or Wi-Fi","⚔ دو نفره · یک گوشی یا وای‌فای"),true,this::multiplayer);
        ui.pair(page,button(t("Hangar","آشیانه"),false,this::hangar),button(t("Missions","مأموریت‌ها"),false,this::missions));
        ui.pair(page,button(t("Collections","فروشگاه"),false,this::shop),button(t("Achievements","دستاوردها"),false,this::achievements));
        ui.pair(page,button(t("Worlds & music","جهان‌ها و موسیقی"),false,this::worlds),button(t("Flight log","آمار پرواز"),false,this::stats));
        btn(page,t("View: ","نمای بازی: ")+(profile.render3d?t("3D","سه‌بعدی"):t("2.5D","۲٫۵بعدی")),false,()->{profile.render3d=!profile.render3d;profile.save();menu();});
        btn(page,t("Settings","تنظیمات"),false,this::settings);
        if(!profile.storageError.isEmpty())body(page,t("Progress could not be saved. Check free storage.","ذخیرهٔ پیشرفت با مشکل مواجه شد؛ فضای خالی دستگاه را بررسی کنید."));
    }
    private void multiplayer(){
        base("multiplayer",t("JET · Play together","جت · با هم بازی کنیم"),t("Two players, equal ships, no external game server.","دو بازیکن با قدرت برابر، بدون سرور بیرونی."));
        LinearLayout shared=ui.card(page);heading(shared,t("One phone, two pilots","یک گوشی، دو خلبان"));
        body(shared,t("Two independent tracks in 3D or 2.5D. Race for sparks, collect 80 as a team, or play 100-second Action Flight with jumps, ducking, Turbo and guardians. Every free character is available.","دو مسیر مستقل با نمای سه‌بعدی یا ۲٫۵بعدی. برای جرقه‌ها رقابت کنید، با هم ۸۰ جرقه بگیرید، یا پرواز اکشن ۱۰۰ ثانیه‌ای با پرش، خم‌شدن، توربو و نگهبان‌ها بازی کنید. همهٔ شخصیت‌های رایگان قابل انتخاب‌اند."));
        btn(shared,t("Start on this phone","شروع روی همین گوشی"),true,()->SharedDeviceActivity.start(this));
        LinearLayout wireless=ui.card(page);heading(wireless,t("Two phones · Wi-Fi / hotspot","دو گوشی · وای‌فای / هات‌اسپات"));
        body(wireless,t("Connect both phones to the same Wi-Fi, or enable a hotspot on one and connect the other. Share the local IP and room PIN. Internet is not required.","هر دو گوشی را به یک وای‌فای وصل کنید، یا هات‌اسپات یکی را روشن کنید و دیگری را به آن وصل کنید. آدرس محلی و رمز اتاق را وارد کنید. اینترنت لازم نیست."));
        btn(wireless,t("Open local multiplayer","ورود به دونفرهٔ محلی"),true,()->LocalMultiplayerActivity.start(this));
        body(page,t("Choose 3D or 2.5D in either lobby. Spark rounds last 75 seconds; same-phone action rounds last 100. Nobody is eliminated. Keep both apps open during Wi-Fi play.","در هر دو اتاق نمای سه‌بعدی یا ۲٫۵بعدی انتخاب کنید. دور جرقه‌ها ۷۵ ثانیه و اکشن روی یک گوشی ۱۰۰ ثانیه است؛ هیچ‌کس حذف نمی‌شود. در بازی وای‌فای هر دو برنامه را باز نگه دارید."));
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==SharedDeviceActivity.REQUEST_CODE&&resultCode==RESULT_OK&&data!=null){
            int count=Math.max(0,Math.min(1000,data.getIntExtra("localMatches",0)));
            profile.duelRuns+=count;
            profile.duelWins+=Math.max(0,Math.min(count,data.getIntExtra("localWins",0)));
            profile.coopWins+=Math.max(0,Math.min(count,data.getIntExtra("localCoopWins",0)));profile.save();
        }
    }
    private void onboarding(){
        base("onboarding",t("A spark. An endless journey.","یک جرقه. یک سفر بی‌پایان."),t("Tap a lane. Collect energy. Keep flying.","مسیر را لمس کن. انرژی جمع کن. پرواز را ادامه بده."));
        ui.add(page,new HeroView(this,0,false,profile.reducedMotion),230,18);
        body(page,t("Avoid × barriers. Collect glowing diamonds. Two quick moves, then you're ready.","از مانع‌های × دور شو و لوزی‌های نورانی را بگیر. در پرواز اکشن، بالا بکش تا بپری و پایین بکش تا خم شوی."));
        btn(page,t("LET'S FLY","بزن بریم"),true,()->startRun(true,GameModes.STUNT));
        btn(page,"فارسی / English",false,()->{profile.language=w.rtl()?"en":"fa";w.refresh();profile.save();onboarding();});
        btn(page,t("Main menu","منوی اصلی"),false,this::menu);
    }
    private void modes(){
        base("modes",t("Choose your flight","پروازت را انتخاب کن"),t("Free game modes, each with its own personal best.","حالت‌های رایگان با رکوردهای جداگانه."));
        modeCard(t("⚡ Action Flight","⚡ پرواز اکشن"),t("100 seconds. Jump hurdles, duck beams, collect fuel and defeat three guardians.","۱۰۰ ثانیه؛ از مانع کوتاه بپر، از زیر تیرک بگذر، سوخت بگیر و سه نگهبان را شکست بده."),GameModes.STUNT,profile.stuntHigh);
        modeCard(t("∞ Survival","∞ بقا"),t("An endless circuit. How far can you go?","یک مسیر بی‌پایان. تا کجا دوام می‌آوری؟"),0,profile.high);
        modeCard(t("90 Sprint","۹۰ سرعتی"),t("Survive 90 seconds and maximize your score.","۹۰ ثانیه دوام بیاور و بیشترین امتیاز را بگیر."),1,profile.sprintHigh);
        modeCard(t("◈ Daily circuit","◈ مسیر روز"),t("Two minutes. A repeatable route for every day.","دو دقیقه؛ هر روز یک مسیر ثابت برای بهتر شدن."),2,profile.challengeDate.equals(profile.day)?profile.dailyHigh:0);
        modeCard(t("◇ Energy Rush","◇ هجوم انرژی"),t("75 seconds, showers of sparks. Collisions break your chain, but the flight continues.","۷۵ ثانیه باران جرقه. برخورد زنجیره را می‌شکند، ولی پرواز ادامه دارد."),GameModes.COIN_RUSH,profile.rushHigh);
        modeCard(t("☁ Zen flight","☁ پرواز آرام"),t("Three calm minutes. No losing, no economy rewards. Just fly and enjoy.","سه دقیقه آرامش؛ بدون باخت یا پاداش اقتصادی. فقط پرواز کن و لذت ببر."),GameModes.ZEN,0);
        btn(page,t("★ Campaign map","★ نقشه سفر"),true,this::campaign);
    }
    private void modeCard(String name,String detail,int m,long best){LinearLayout c=ui.card(page);heading(c,name);body(c,detail);body(c,t("Best: ","رکورد: ")+n(best));btn(c,t("Launch","پرواز"),true,()->startRun(false,m));}
    private void startRun(boolean training,int selectedMode){
        if(profile.resetDay())profile.save();
        if(!training&&!profile.tutorial){startRun(true,selectedMode);return;}if(game!=null)game.pause();
        mode=selectedMode;screen="play";paused=false;settled=false;tutorialStep=0;feedbackText="";overlay=null;
        levelBefore=profile.level();achBefore=profile.achievementCount();previousBest=profile.best(mode);
        root.removeAllViews();LinearLayout layout=ui.column();layout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);root.addView(layout,new FrameLayout.LayoutParams(-1,-1));
        boolean compact=getResources().getConfiguration().screenHeightDp<680||getResources().getConfiguration().fontScale>1.3f;
        LinearLayout hud=ui.column();hud.setPadding(ui.dp(compact?10:20),ui.dp(compact?6:14),ui.dp(compact?10:20),ui.dp(4));ui.add(layout,hud,-2,0);
        runTitle=ui.text("",12,Ui.LIME,true);runTitle.setMaxLines(2);runTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(runTitle,new LinearLayout.LayoutParams(0,-2,1));
        Button pause=button(compact?"Ⅱ":t("Ⅱ Pause","Ⅱ مکث"),false,this::pauseGame);pause.setContentDescription(t("Pause flight","توقف پرواز"));pause.setMinimumHeight(ui.dp(48));pause.setMinHeight(ui.dp(48));pause.setPadding(ui.dp(8),ui.dp(4),ui.dp(8),ui.dp(4));
        LinearLayout.LayoutParams pauseParams=new LinearLayout.LayoutParams(compact?ui.dp(48):ui.dp(115),ui.dp(48));pauseParams.setMarginStart(ui.dp(8));top.addView(pause,pauseParams);ui.add(hud,top,-2,compact?4:8);
        renderText=ui.text("",compact?10:11,Ui.MUTED,false);renderText.setMaxLines(1);renderText.setEllipsize(android.text.TextUtils.TruncateAt.END);ui.add(hud,renderText,-2,2);
        scoreText=ui.text("0",mode==GameModes.ZEN||compact?24:32,Ui.TEXT,true);scoreText.setMaxLines(1);scoreText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if(Build.VERSION.SDK_INT>=26)scoreText.setAutoSizeTextTypeUniformWithConfiguration(16,mode==GameModes.ZEN||compact?24:32,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        ui.add(hud,scoreText,-2,1);comboText=ui.text("",compact?11:13,Ui.GOLD,false);comboText.setMaxLines(2);comboText.setEllipsize(android.text.TextUtils.TruncateAt.END);ui.add(hud,comboText,-2,compact?3:5);
        powerText=ui.text("",compact?10:12,Ui.MUTED,false);powerText.setMaxLines(compact?2:3);powerText.setEllipsize(android.text.TextUtils.TruncateAt.END);powerText.setMinHeight(ui.dp(compact?24:42));ui.add(hud,powerText,-2,0);
        long seed=mode==2?profile.day.hashCode()*341873128712L:System.nanoTime();
                game=new GameView(this,profile,training,mode,selectedStage,seed,this);
        FrameLayout flight=new FrameLayout(this);layout.addView(flight,new LinearLayout.LayoutParams(-1,0,1));
        final GameView activeGame=game;
        if(profile.render3d){
            FlightGLView graphics=new FlightGLView(this,profile);flight.addView(graphics,new FrameLayout.LayoutParams(-1,-1));game.use3d(graphics);
            graphics.setReadyListener(()->{if(game==activeGame)pulse();});
            graphics.setFailureListener(()->{if(game!=activeGame)return;activeGame.use3d(null);flight.removeView(graphics);message("3D could not start. The lighter 2.5D view is active for this flight.","سه‌بعدی اجرا نشد؛ نمای سبک‌تر ۲٫۵بعدی در این پرواز فعال است.");pulse();});
        }
        flight.addView(game,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout controls=new LinearLayout(this);controls.setPadding(ui.dp(12),ui.dp(8),ui.dp(12),ui.dp(8));
        for(int i=0;i<3;i++){final int lane=i;Button b=button(i==0?"‹":i==1?"◇":"›",false,()->{if(game!=null&&!paused)game.engine.moveTo(lane);});
            b.setContentDescription(i==0?t("Left lane","مسیر چپ"):i==1?t("Middle lane","مسیر وسط"):t("Right lane","مسیر راست"));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ui.dp(52),1);lp.setMargins(ui.dp(4),0,ui.dp(4),0);controls.addView(b,lp);}
        ui.add(layout,controls,-2,0);
        jumpButton=duckButton=boostButton=null;
        if(mode==GameModes.STUNT&&!training){
            LinearLayout actions=new LinearLayout(this);actions.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);actions.setPadding(ui.dp(10),0,ui.dp(10),ui.dp(7));
            jumpButton=button(t("↑ Jump","↑ پرش"),false,()->{if(game!=null&&!paused)game.engine.jump();});
            duckButton=button(t("↓ Duck","↓ خم‌شدن"),false,()->{if(game!=null&&!paused)game.engine.duck();});
            boostButton=button(t("⚡ Turbo","⚡ توربو"),true,()->{if(game!=null&&!paused)game.engine.boost();});
            if(compact){jumpButton.setText("↑");duckButton.setText("↓");}jumpButton.setContentDescription(t("Jump","پرش"));duckButton.setContentDescription(t("Duck","خم‌شدن"));boostButton.setContentDescription(t("Turbo","توربو"));
            for(Button action:new Button[]{jumpButton,duckButton,boostButton}){action.setTextSize(compact?12:13);action.setMinimumHeight(ui.dp(48));action.setMinHeight(ui.dp(48));action.setPadding(ui.dp(3),ui.dp(7),ui.dp(3),ui.dp(7));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,ui.dp(49),1);params.setMargins(ui.dp(3),0,ui.dp(3),0);actions.addView(action,params);}
            ui.add(layout,actions,-2,0);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);pulse();game.resume();
    }
    @Override public void pulse(){
        if(game==null||!screen.equals("play"))return;GameEngine e=game.engine;audio.gameIntensity(e.overdrive>0,e.bossActive);
        if(e.tutorial){
            runTitle.setText(t("FLIGHT SCHOOL","آموزش پرواز"));
            if(tutorialStep==0&&e.moves>=2){tutorialStep=1;e.spawn(GameEngine.ENERGY,e.targetLane,.44f);}
            if(tutorialStep==1&&e.energy>=1){profile.tutorial=true;profile.save();startRun(false,mode);return;}
            if(tutorialStep==1){boolean orb=false;for(GameEngine.Entity entity:e.entities)if(entity.active)orb=true;if(!orb)e.spawn(GameEngine.ENERGY,e.targetLane,.4f);}
            scoreText.setText(tutorialStep==0?t("Move twice","دو بار جابه‌جا شو"):t("Catch the spark","جرقه را بگیر"));
            comboText.setText(tutorialStep==0?t("Tap the left and right lanes","مسیر چپ و راست را لمس کن"):t("Move into the glowing diamond's lane","به مسیر لوزی نورانی برو"));
            powerText.setText(t("Tap or swipe • one hand is all you need","لمس یا کشیدن • فقط با یک دست"));return;
        }
        String runName=mode==GameModes.STUNT?t("ACTION FLIGHT","پرواز اکشن"):mode==GameModes.CAMPAIGN?t("STAGE ","مرحله ")+n(selectedStage+1):mode==GameModes.ZEN?t("ZEN","آرام"):mode==GameModes.COIN_RUSH?t("ENERGY RUSH","هجوم انرژی"):mode==GameModes.DAILY?t("DAILY","روزانه"):mode==GameModes.SPRINT?t("SPRINT","سرعتی"):t("SURVIVAL","بقا");
        runTitle.setText(String.format(java.util.Locale.ROOT,"%s · %s",runName,n((int)(e.durationSeconds()>0?Math.max(0,e.durationSeconds()-e.seconds):e.seconds))));
        int graphics=game.graphicsStatus();String graphicsLabel=graphics==2?t("3D active","سه‌بعدی فعال"):graphics==1?t("Preparing 3D","آماده‌سازی سه‌بعدی"):t("2.5D view","نمای ۲٫۵بعدی");renderText.setText(String.format(java.util.Locale.ROOT,t("%s · Build %s","%s · ساخت %s"),graphicsLabel,n(BuildConfig.VERSION_CODE)));
        scoreText.setText(mode==GameModes.ZEN?t("Find your flow","در آرامش پرواز کن"):n(e.score()));comboText.setText(String.format(java.util.Locale.ROOT,t("COMBO %s · ×%s · ◇ %s · ⚡%s%%%s","کمبو %s · ×%s · ◇ %s · ⚡%s%%%s"),n(e.combo),n(e.multiplier()),n(e.energy),n(Math.round((mode==GameModes.STUNT?e.boostFuel:e.charge)*100)),e.overdrive>0?t(" OVERDRIVE!"," فراسرعت!"):""));
        if(mode==GameModes.STUNT&&boostButton!=null){boostButton.setText(String.format(java.util.Locale.ROOT,t("⚡ %s%%","⚡ %s٪"),n(Math.round(e.boostFuel*100))));boostButton.setEnabled(e.boostFuel>=GameConfig.BOOST_COST&&e.overdrive<=0);boostButton.setAlpha(boostButton.isEnabled()?1:.55f);jumpButton.setAlpha(e.jumpTime>0||e.duckTime>0?.55f:1);duckButton.setAlpha(e.jumpTime>0||e.duckTime>0?.55f:1);}
        StringBuilder text=new StringBuilder();
        for(int i=0;i<4;i++)if(e.power[i]>0)text.append(Catalog.POWER_ICONS[i]).append(" ").append(w.t(Catalog.POWERS[i])).append(" ").append(n((int)Math.ceil(e.power[i]))).append("   ");
        if(SystemClock.uptimeMillis()<feedbackUntil)text.append(feedbackText);
        if(mode==GameModes.STUNT)text.append(t("Stunts ","حرکت عالی ")).append(n(e.stunts)).append(t(" · Guardians "," · نگهبان ")).append(n(e.bossesDefeated)).append(" / ").append(n(3)).append("  ♥ ").append(n(e.lives));
        else if(mode==GameModes.CAMPAIGN)text.append(t("Goal ","هدف ")).append(n(e.objectiveProgress())).append(" / ").append(n(e.objectiveTarget())).append("  ♥ ").append(n(e.lives));
        else if(mode!=GameModes.ZEN&&mode!=GameModes.COIN_RUSH)text.append(" ♥ ").append(n(e.lives));
        if(text.length()==0)text.append(t("Collect sparks → Charge Overdrive • Near miss → +35","جمع‌کردن جرقه ← شارژ فراسرعت • عبور نزدیک ← ۳۵+"));powerText.setText(text.toString());
    }
    @Override public void event(int event,int detail){
        if(event==GameEngine.COLLECT)audio.play(Feedback.COLLECT);else if(event==GameEngine.COMBO)audio.play(Feedback.COMBO);
        else if(event==GameEngine.POWER)audio.play(Feedback.SHIELD+detail);else if(event==GameEngine.HIT)audio.play(Feedback.HIT);else if(event==GameEngine.DEATH)audio.play(Feedback.OVER);else if(event==GameEngine.COMPLETE)audio.play(Feedback.LEVEL);
        else if(event==GameEngine.NEAR){feedbackText=t("CLOSE CALL!","عبور میلی‌متری!");feedbackUntil=SystemClock.uptimeMillis()+900;audio.play(Feedback.COLLECT);}
        else if(event==GameEngine.ACTION)audio.play(detail==2?Feedback.TURBO:detail==0?Feedback.JUMP:Feedback.DUCK);
        else if(event==GameEngine.STUNT)audio.play(Feedback.STUNT);
        else if(event==GameEngine.BOSS)audio.play(Feedback.GUARDIAN);
        else if(event==GameEngine.BOSS_BREAK)audio.play(Feedback.GUARDIAN_BREAK);
        else if(event==GameEngine.OVERDRIVE){feedbackText=t("OVERDRIVE ×2","فراسرعت ×۲");feedbackUntil=SystemClock.uptimeMillis()+1800;audio.play(Feedback.COMBO);}
    }
    @Override public void finished(){
        if(game==null||settled||game.engine.alive)return;settled=true;GameEngine run=game.engine;game.pause();earned=profile.record(run);
        if(profile.level()>levelBefore)audio.play(Feedback.LEVEL);else if(profile.achievementCount()>achBefore)audio.play(Feedback.ACHIEVEMENT);results(run);
    }
    private void results(GameEngine run){
        long score=run.score();base("results",run.won?t("Circuit complete","مسیر کامل شد"):t("Flight complete","پرواز تمام شد"),(run.mode!=GameModes.CAMPAIGN&&run.mode!=GameModes.ZEN&&score>previousBest)?t("A NEW PERSONAL BEST","یک رکورد تازه"):t("One more flight. One new possibility.","یک پرواز دیگر، یک فرصت تازه."));
        LinearLayout c=ui.card(page);
        if(run.mode==GameModes.CAMPAIGN){ui.label(c,starsText(run.won?run.stageStars():0),40,Ui.GOLD,10);body(c,t("Stage ","مرحله ")+n(run.stageIndex+1)+t(" · Goal: "," · هدف: ")+n(run.objectiveProgress())+" / "+n(run.objectiveTarget()));}
        if(run.mode==GameModes.STUNT)body(c,t("Perfect moves: ","حرکت‌های عالی: ")+n(run.stunts)+t(" · Guardians defeated: "," · نگهبان‌های شکست‌خورده: ")+n(run.bossesDefeated)+" / "+n(3));
        ui.label(c,run.mode==GameModes.ZEN?t("A peaceful flight","یک پرواز آرام"):n(score),run.mode==GameModes.ZEN?28:52,Ui.LIME,8);body(c,t("Final score","امتیاز نهایی"));
        ui.pair(c,ui.text(n((int)run.seconds)+t(" seconds"," ثانیه"),16,Ui.TEXT,true),ui.text("◇ "+n(run.energy),16,Ui.GOLD,true));
        body(c,t("Best combo: ","بهترین کمبو: ")+n(run.bestCombo)+t(" · Close calls: "," · عبور نزدیک: ")+n(run.nearMisses));
        body(c,t("XP earned: +","تجربهٔ دریافت‌شده: +")+n(earned)+t(" · Pilot level "," · سطح خلبان ")+n(profile.level()));
        if(profile.level()>levelBefore)body(c,t("LEVEL UP! New hangar unlocks await.","ارتقای سطح! آیتم‌های تازهٔ آشیانه را ببین."));
        if(profile.achievementCount()>achBefore)body(c,t("New achievements unlocked!","دستاورد تازه باز شد!"));
        if(!profile.storageError.isEmpty())body(c,t("Saving failed. Free some device storage before closing.","ذخیره انجام نشد؛ پیش از بستن برنامه فضای دستگاه را خالی کنید."));
        if(run.mode==GameModes.CAMPAIGN&&run.won&&run.stageIndex+1<StageCatalog.COUNT)btn(page,t("NEXT DESTINATION →","مقصد بعدی ←"),true,()->startStage(run.stageIndex+1));
        if(run.mode==GameModes.CAMPAIGN)btn(page,t("Campaign map","نقشه سفر"),false,this::campaign);
        btn(page,t("FLY AGAIN","پرواز دوباره"),true,()->startRun(false,mode));ui.pair(page,button(t("Missions","مأموریت‌ها"),false,this::missions),button(t("Hangar","آشیانه"),false,this::hangar));btn(page,t("Home","خانه"),false,this::menu);
    }
    private void pauseGame(){
        if(game==null||paused||settled)return;paused=true;game.pause();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ScrollView pausedScroll=new ScrollView(this);pausedScroll.setFillViewport(true);pausedScroll.setVerticalScrollBarEnabled(false);pausedScroll.setBackgroundColor(0xf2080d19);
        overlay=pausedScroll;root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout pauseContent=ui.column();pauseContent.setGravity(Gravity.CENTER);pauseContent.setPadding(ui.dp(28),ui.dp(24),ui.dp(28),ui.dp(24));pausedScroll.addView(pauseContent,new ScrollView.LayoutParams(-1,-2));
        heading(pauseContent,t("Take a breath.","یک نفس تازه کن."));body(pauseContent,t("Your flight is paused.","پرواز متوقف است."));btn(pauseContent,t("Resume flight","ادامهٔ پرواز"),true,this::resumeGame);
        btn(pauseContent,t("Restart","شروع دوباره"),false,()->confirm(t("Restart this flight?","پرواز از ابتدا شروع شود؟"),t("This unfinished run will be discarded.","امتیاز این دور ناتمام ذخیره نمی‌شود."),()->startRun(game.engine.tutorial,mode)));
        btn(pauseContent,t("Return home","بازگشت به خانه"),false,()->confirm(t("Leave this flight?","از این پرواز خارج می‌شوید؟"),t("This unfinished run will be discarded.","امتیاز این دور ناتمام ذخیره نمی‌شود."),this::menu));
    }
    private void resumeGame(){if(game==null)return;if(overlay!=null)root.removeView(overlay);overlay=null;paused=false;getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);game.resume();}
    private void hangar(){
        base("hangar",t("Your hangar","آشیانهٔ تو"),t("36 characters. Identical handling. Your signature.","۳۶ شخصیت با عملکرد یکسان و ظاهر مخصوص تو."));body(page,"◇ "+n(profile.coins)+t(" energy credits"," اعتبار انرژی"));
        ui.pair(page,button(t("Trails & badges","رد نور و نشان‌ها"),false,this::customize),button(t("Environments","محیط‌ها"),false,this::worlds));
        for(int i=0;i<Catalog.SKINS.length;i++){final int id=i;LinearLayout c=ui.card(page);ui.add(c,new HeroView(this,i,true,profile.reducedMotion),100,4);heading(c,w.t(Catalog.SKINS[i]));boolean owned=Progression.isPaidSkin(i)?billing.entitlements.skin(i):profile.owned[i];
            if(owned)btn(c,profile.skin==i?t("Selected ✓","انتخاب‌شده ✓"):t("Equip","انتخاب"),profile.skin==i,()->{profile.skin=id;profile.save();hangar();});
            else if(!Progression.isPaidSkin(i)){body(c,t("Level ","سطح ")+n(Progression.skinLevel(i))+" · ◇ "+n(Progression.skinCost(i)));
                Button unlock=button(t("Unlock with energy","باز کردن با انرژی"),false,()->{if(profile.unlock(id)){audio.play(Feedback.ACHIEVEMENT);hangar();}else if(!profile.storageError.isEmpty())message("Could not save. Free some device storage and retry.","ذخیره انجام نشد؛ فضای دستگاه را خالی کنید و دوباره تلاش کنید.");});
                unlock.setEnabled(profile.level()>=Progression.skinLevel(i)&&profile.coins>=Progression.skinCost(i));unlock.setAlpha(unlock.isEnabled()?1:.45f);ui.add(c,unlock,-2,0);
            }else btn(c,w.t(Products.NAMES[Products.packForSkin(i)]),false,this::shop);
        }
        btn(page,t("Explore 16 worlds","کاوش ۱۶ جهان"),false,this::worlds);
    }
    private void missions(){
        if(profile.resetDay())profile.save();base("missions",t("Daily missions","مأموریت‌های روزانه"),t("Five objectives. Fresh rewards each day.","پنج هدف، پاداش‌های تازه در هر روز."));body(page,t("Local reset date: ","تاریخ شروع روز: ")+profile.day);
        for(int i=0;i<5;i++){final int id=i;LinearLayout c=ui.card(page);heading(c,w.t(Catalog.MISSIONS[i]));int target=profile.target(i);body(c,n(Math.min(profile.daily[i],target))+" / "+n(target)+" · ◇ "+n(profile.reward(i)));ui.progress(c,(int)Math.min(profile.daily[i],target),target);
            Button b=button(profile.claimed[i]?t("Claimed ✓","دریافت‌شده ✓"):t("Claim reward","دریافت پاداش"),!profile.claimed[i]&&profile.daily[i]>=target,()->{if(profile.claim(id)){audio.play(Feedback.ACHIEVEMENT);missions();}else if(!profile.storageError.isEmpty())message("Could not save. Free some device storage and retry.","ذخیره انجام نشد؛ فضای دستگاه را خالی کنید و دوباره تلاش کنید.");});b.setEnabled(!profile.claimed[i]&&profile.daily[i]>=target);b.setAlpha(b.isEnabled()?1:.45f);ui.add(c,b,-2,0);}
    }
    private void achievements(){
        base("achievements",t("Achievements","دستاوردها"),n(profile.achievementCount())+" / "+n(Catalog.ACHIEVEMENTS.length)+t(" unlocked · 25 energy each"," بازشده · پاداش هرکدام ۲۵ انرژی"));
        for(int i=0;i<Catalog.ACHIEVEMENTS.length;i++){LinearLayout c=ui.card(page);ui.label(c,(profile.achievements[i]?"✓ ":"◇ ")+w.t(Catalog.ACHIEVEMENTS[i]),19,profile.achievements[i]?Ui.LIME:Ui.TEXT,7);body(c,w.t(Catalog.ACH_DESC[i]));}
    }
    private void stats(){
        base("stats",t("Flight log","آمار پرواز"),t("Every flight leaves a mark.","هر پرواز اثری به جا می‌گذارد."));
        String[] names={t("Flights","پروازها"),t("Best survival score","رکورد بقا"),t("Best sprint score","رکورد سرعتی"),t("Total energy","کل انرژی"),t("Obstacles passed","موانع ردشده"),t("Close calls","عبورهای نزدیک"),t("Best combo","بهترین کمبو"),t("Longest flight / seconds","طولانی‌ترین پرواز / ثانیه")};
        long[] values={profile.runs,profile.high,profile.sprintHigh,profile.totalEnergy,profile.totalPassed,profile.totalNear,profile.bestCombo,(long)profile.longest};
        body(page,t("Campaign stars: ","ستاره‌های سفر: ")+n(profile.starCount())+" / "+n(StageCatalog.COUNT*3)+t(" · Completed destinations: "," · مقصدهای کامل‌شده: ")+n(profile.clearedStages()));
        body(page,t("Action best: ","رکورد اکشن: ")+n(profile.stuntHigh)+t(" · Perfect moves: "," · حرکت‌های عالی: ")+n(profile.totalStunts)+t(" · Guardians defeated: "," · نگهبان‌های شکست‌خورده: ")+n(profile.guardiansDefeated));
        body(page,t("Overdrives: ","فراسرعت‌ها: ")+n(profile.totalOverdrives)+t(" · Energy Rush best: "," · رکورد هجوم انرژی: ")+n(profile.rushHigh));
        body(page,t("Local matches: ","بازی‌های دونفره: ")+n(profile.duelRuns)+t(" · Player 1 wins: "," · بردهای بازیکن ۱: ")+n(profile.duelWins)+t(" · Team victories: "," · موفقیت‌های تیمی: ")+n(profile.coopWins));
        for(int i=0;i<names.length;i++){LinearLayout c=ui.card(page);body(c,names[i]);ui.label(c,n(values[i]),28,Ui.LIME,0);}
    }
    private void shop(){base("shop",t("The collection","کلکسیون"),t("Permanent cosmetics. Same skill, your signature.","ظاهرهای دائمی؛ مهارت یکسان، امضای مخصوص تو."));storeContent();if(!billing.busy()&&billing.state!=MyketBilling.State.READY)billing.connect();}
    private void storeContent(){
        body(page,storeStatus());
        for(int i=0;i<3;i++){final String sku=Products.IDS.get(i);final int index=i;LinearLayout c=ui.card(page);ui.add(c,new HeroView(this,i==0?10:i==1?13:16,false,profile.reducedMotion),135,5);heading(c,w.t(Products.NAMES[i]));body(c,w.t(Products.DESCRIPTIONS[i]));
            String price=billing.price(sku);boolean owned=billing.entitlements.owned(sku);
            Button buy=button(owned?t("Owned ✓ · Open hangar","خریداری‌شده ✓ · آشیانه"):price==null?t("Price unavailable","قیمت در دسترس نیست"):t("Buy · ","خرید · ")+price,!owned,()->{
                if(owned){hangar();return;}parentGate(()->confirm(w.t(Products.NAMES[index]),w.t(Products.DESCRIPTIONS[index])+"\n\n"+t("One-time payment: ","پرداخت یک‌باره: ")+billing.price(sku),()->billing.buy(sku)));});
            buy.setEnabled(owned||billing.canBuy(sku));buy.setAlpha(buy.isEnabled()?1:.45f);ui.add(c,buy,-2,0);}
        Button restore=button(t("Restore purchases / Reconnect","بازیابی خریدها / اتصال دوباره"),false,()->{if(billing.state==MyketBilling.State.PURCHASING)billing.recover();else if(billing.state==MyketBilling.State.READY)billing.restore();else billing.connect();});restore.setEnabled(!billing.busy()||billing.state==MyketBilling.State.PURCHASING);ui.add(page,restore,-2,12);
        body(page,t("Use the same Myket account to restore after reinstalling. Resetting progress does not erase purchases.","برای بازیابی پس از نصب دوباره از همان حساب مایکت استفاده کنید. پاک‌کردن پیشرفت، خریدها را حذف نمی‌کند."));
    }
    private String storeStatus(){
        switch(billing.state){
            case NOT_CONFIGURED:return t("Purchases are not enabled in this build. Free modes are available.","پرداخت در این نسخه هنوز فعال نشده است. حالت‌های رایگان در دسترس‌اند.");
            case STORE_MISSING:return t("Install or update Myket, sign in, then reconnect.","مایکت را نصب یا به‌روز کنید، وارد حساب شوید و اتصال دوباره را بزنید.");
            case CONNECTING:case SYNCING:return t("Connecting to Myket…","در حال اتصال به مایکت…");
            case PURCHASING:return t("Complete or cancel your purchase in Myket.","خرید را در مایکت تکمیل یا لغو کنید.");
            case READY:return t("Connected. Prices are provided by Myket.","متصل به مایکت. قیمت‌ها از فروشگاه دریافت می‌شوند.");
            default:return t("Store unavailable. Your free game is ready.","فروشگاه در دسترس نیست. بازی رایگان آماده است.");
        }
    }
    @Override public void changed(){
        if(isFinishing()||root==null)return;
        if(Progression.isPaidSkin(profile.skin)&&!billing.entitlements.skin(profile.skin)){profile.skin=0;profile.save();}
        if(screen.equals("shop")){base("shop",t("The collection","کلکسیون"),t("Permanent cosmetics. Same skill, your signature.","ظاهرهای دائمی؛ مهارت یکسان، امضای مخصوص تو."));storeContent();}
    }
    @Override public void message(String en,String fa){if(!isFinishing())Toast.makeText(this,t(en,fa),Toast.LENGTH_LONG).show();}
    private void settings(){
        base("settings",t("Settings","تنظیمات"),t("Make the circuit yours.","پرواز را مطابق سلیقه‌ات تنظیم کن."));
        toggle(t("3D view (off: 2.5D)","نمای سه‌بعدی (خاموش: ۲٫۵بعدی)"),profile.render3d,()->profile.render3d=!profile.render3d);
        body(page,t("3D adds depth and a following camera; 2.5D is the lighter perspective option. Both views are available in the two-player lobbies.","سه‌بعدی عمق و دوربین دنبال‌کننده دارد؛ ۲٫۵بعدی گزینهٔ سبک‌تر با پرسپکتیو است. هر دو نما در اتاق‌های دونفره هم قابل انتخاب‌اند."));
        toggle(t("Gentle difficulty","سختی ملایم"),profile.relaxed,()->profile.relaxed=!profile.relaxed);
        body(page,t("Gentle flight offers more hearts and a slower pace. Zen has no game over.","پرواز ملایم فرصت‌های بیشتر و سرعت کمتر دارد. پرواز آرام باخت ندارد."));
        toggle(t("High contrast track","مسیر با کنتراست بالا"),profile.highContrast,()->profile.highContrast=!profile.highContrast);
        toggle(t("Music","موسیقی"),profile.music,()->profile.music=!profile.music);toggle(t("Sound effects","جلوه‌های صوتی"),profile.sound,()->profile.sound=!profile.sound);
        toggle(t("Haptic feedback","بازخورد لرزشی"),profile.haptic,()->profile.haptic=!profile.haptic);toggle(t("Reduced motion","کاهش حرکت و افکت"),profile.reducedMotion,()->profile.reducedMotion=!profile.reducedMotion);
        btn(page,t("Language: English → فارسی","زبان: فارسی ← English"),false,()->{profile.language=w.rtl()?"en":"fa";w.refresh();profile.save();settings();});
        btn(page,t("Soundtrack room","اتاق موسیقی"),false,this::musicRoom);
        btn(page,t("Flight guide","راهنمای پرواز"),false,this::guide);
        btn(page,t("Replay tutorial","مرور آموزش"),false,()->startRun(true,GameModes.STUNT));btn(page,t("About & privacy","دربارهٔ بازی و حریم خصوصی"),false,this::about);
        btn(page,t("Reset game progress","پاک‌کردن پیشرفت بازی"),false,()->confirm(t("Reset your progress?","پیشرفت بازی پاک شود؟"),t("Scores, energy and free unlocks will be erased. Purchased collections remain.","رکوردها، انرژی و آیتم‌های رایگان پاک می‌شوند. مجموعه‌های خریداری‌شده محفوظ می‌مانند."),()->{profile.reset();settings();}));
    }
    private void toggle(String name,boolean value,Runnable mutation){btn(page,name+"  "+(value?t("ON","روشن"):t("OFF","خاموش")),false,()->{mutation.run();profile.save();audio.settingsChanged();settings();});}
    private void about(){
        base("about",t("About JET","دربارهٔ جت"),"Version "+BuildConfig.VERSION_NAME+" · "+BuildConfig.VERSION_CODE);ui.add(page,new HeroView(this,profile.skin,true,profile.reducedMotion),150,10);
        heading(page,t("One more flight.","فقط یک پرواز دیگر."));
        body(page,t("An arcade game by ToosArax. Original vector artwork and synthesized audio. Persian font: Vazirmatn (SIL OFL).","یک بازی آرکید از ToosArax. طراحی برداری و صداهای تولیدشده برای بازی. قلم فارسی وزیرمتن با مجوز SIL OFL."));
        heading(page,t("Privacy","حریم خصوصی"));body(page,t("Progress and signed purchase receipts stay in private device storage. No game analytics or advertising. Myket handles payment under its own privacy policy. Purchases require Myket and internet; solo and same-phone gameplay work offline. Local Wi-Fi play exchanges game state only with the paired phone, without an external server.","پیشرفت و رسیدهای امضاشده در فضای خصوصی دستگاه ذخیره می‌شوند. بازی تبلیغات یا آمارگیری ندارد. مایکت پرداخت را طبق سیاست حریم خصوصی خود انجام می‌دهد. خرید به مایکت و اینترنت نیاز دارد؛ تک‌نفره و بازی روی یک گوشی آفلاین‌اند. دونفرهٔ وای‌فای وضعیت بازی را فقط با گوشی متصل‌شده ردوبدل می‌کند و سرور بیرونی ندارد."));
        btn(page,t("Game page & support on Myket","صفحهٔ بازی و پشتیبانی در مایکت"),false,()->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://myket.ir/app/com.neonrush.arcade")));}catch(ActivityNotFoundException e){message("No browser is installed.","مرورگری نصب نیست.");}});
        btn(page,t("Font license","مجوز قلم"),false,()->{try(java.io.InputStream in=getAssets().open("licenses/Vazirmatn-OFL.txt")){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[2048];int count;while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);new AlertDialog.Builder(this).setTitle("Vazirmatn · SIL OFL").setMessage(bytes.toString("UTF-8")).setPositiveButton(t("Close","بستن"),null).show();}catch(Exception e){message("License unavailable.","مجوز باز نشد.");}});
    }
    private String starsText(int count){return count==3?"★ ★ ★":count==2?"★ ★ ☆":count==1?"★ ☆ ☆":"☆ ☆ ☆";}
    private void startStage(int index){
        if(!profile.stageAvailable(index)){message("Complete the previous destination first.","ابتدا مقصد قبلی را کامل کن.");return;}
        selectedStage=index;startRun(false,GameModes.CAMPAIGN);
    }
    private void campaign(){
        base("campaign",t("Your galaxy adventure","سفر کهکشانی تو"),t("48 destinations, 12 chapters, 144 stars. Every chapter is free.","۴۸ مقصد، ۱۲ فصل، ۱۴۴ ستاره. تمام فصل‌ها رایگان‌اند."));
        body(page,"★ "+n(profile.starCount())+" / "+n(StageCatalog.COUNT*3)+t(" · Destinations cleared: "," · مقصدهای کامل‌شده: ")+n(profile.clearedStages()));
        for(int world=0;world<StageCatalog.WORLD_COUNT;world++){
            heading(page,StageCatalog.worldName(world,w.rtl()));
            ui.add(page,new SceneAssets.Preview(this,SceneAssets.campaignWorld(world)),130,12);
            for(int j=0;j<4;j++){
                final int index=world*4+j;StageCatalog.Stage stage=StageCatalog.get(index);
                LinearLayout card=ui.card(page);ui.label(card,n(index+1)+"  "+stage.title(w.rtl()),19,Ui.TEXT,7);
                body(card,stage.objectiveName(w.rtl())+" · "+n(stage.target)+t(" · "," · ")+n((long)stage.duration)+t(" seconds"," ثانیه"));
                ui.label(card,starsText(profile.stars[index]),23,Ui.GOLD,12);
                Button launch=button(profile.stars[index]>0?t("Fly again","پرواز دوباره"):profile.stageAvailable(index)?t("Start destination","شروع مقصد"):t("Complete previous destination","مقصد قبلی را کامل کن"),profile.stageAvailable(index),()->startStage(index));
                launch.setEnabled(profile.stageAvailable(index));launch.setAlpha(launch.isEnabled()?1:.45f);ui.add(card,launch,-2,0);
            }
        }
    }
    private void worlds(){
        base("worlds",t("Sixteen worlds await","شانزده جهان در انتظار تو"),t("Original illustrated places with their own mood and soundtrack.","محیط‌های تصویری اختصاصی با حال‌وهوا و موسیقی متفاوت."));
        btn(page,t("Soundtrack room","اتاق موسیقی"),false,this::musicRoom);
        for(int i=0;i<SceneAssets.COUNT;i++){
            final int index=i;LinearLayout card=ui.card(page);
            ui.add(card,new SceneAssets.Preview(this,i),170,12);
            heading(card,w.rtl()?SceneAssets.nameFa(i):SceneAssets.nameEn(i));
            boolean open=profile.sceneAvailable(i);
            body(card,open?t("Ready for your next flight","آمادهٔ پرواز بعدی"):t("Unlock at pilot level ","باز شدن در سطح خلبان ")+n(Catalog.SCENE_LEVELS[i]));
            Button select=button(profile.zone==i?t("Selected ✓","انتخاب‌شده ✓"):t("Select world","انتخاب جهان"),profile.zone==i,()->{
                if(!profile.sceneAvailable(index))return;
                profile.zone=index;profile.musicTrack=SceneAssets.musicTrack(index);profile.save();audio.settingsChanged();worlds();
            });
            select.setEnabled(open);select.setAlpha(open?1:.45f);ui.add(card,select,-2,0);
        }
    }
    private void musicRoom(){
        base("music",t("The soundtrack room","اتاق موسیقی"),t("Twelve original pieces. Choose your flight's rhythm.","دوازده قطعهٔ اختصاصی. ریتم پروازت را انتخاب کن."));
        btn(page,profile.music?t("Music: ON","موسیقی: روشن"):t("Music: OFF","موسیقی: خاموش"),false,()->{profile.music=!profile.music;profile.save();audio.settingsChanged();musicRoom();});
        for(int i=0;i<SceneAssets.trackCount();i++){final int track=i;
            LinearLayout card=ui.card(page);heading(card,w.rtl()?SceneAssets.trackNameFa(i):SceneAssets.trackNameEn(i));
            body(card,t("Original instrumental • plays offline","موسیقی بی‌کلام اختصاصی • پخش آفلاین"));
            btn(card,profile.musicTrack==i?t("Playing selection ✓","قطعهٔ انتخاب‌شده ✓"):t("Listen & select","گوش‌دادن و انتخاب"),profile.musicTrack==i,()->{profile.musicTrack=track;profile.music=true;profile.save();audio.settingsChanged();musicRoom();});
        }
    }
    private void customize(){
        base("customize",t("Your signature","امضای پرواز تو"),t("Earn trails with pilot levels and badges with campaign stars.","رد نور را با سطح خلبان و نشان را با ستاره‌های سفر باز کن."));
        heading(page,t("Engine trails","رد موتور"));
        for(int i=0;i<Cosmetics.TRAILS.length;i++){final int index=i;LinearLayout card=ui.card(page);
            heading(card,w.t(Cosmetics.TRAILS[i]));boolean open=profile.trailAvailable(i);
            body(card,open?t("Ready to equip","آمادهٔ انتخاب"):t("Pilot level ","سطح خلبان ")+n(Catalog.TRAIL_LEVELS[i]));
            Button select=button(profile.trail==i?t("Equipped ✓","انتخاب‌شده ✓"):t("Equip trail","انتخاب رد نور"),profile.trail==i,()->{if(profile.trailAvailable(index)){profile.trail=index;profile.save();customize();}});
            select.setEnabled(open);select.setAlpha(open?1:.45f);ui.add(card,select,-2,0);
        }
        heading(page,t("Pilot badges","نشان خلبان"));
        for(int i=0;i<Cosmetics.BADGES.length;i++){final int index=i;LinearLayout card=ui.card(page);heading(card,w.t(Cosmetics.BADGES[i]));boolean open=profile.badgeAvailable(i);
            body(card,"★ "+n(Catalog.BADGE_STARS[i]));
            Button select=button(profile.badge==i?t("Equipped ✓","انتخاب‌شده ✓"):t("Equip badge","انتخاب نشان"),profile.badge==i,()->{if(profile.badgeAvailable(index)){profile.badge=index;profile.save();customize();}});
            select.setEnabled(open);select.setAlpha(open?1:.45f);ui.add(card,select,-2,0);
        }
    }
    private void guide(){
        base("guide",t("Your flight handbook","دفترچهٔ پرواز"),t("A few tips, a lot of possibilities.","چند نکته، کلی راه برای بهتر شدن."));
        LinearLayout c=ui.card(page);heading(c,t("Steer","هدایت"));
        body(c,t("Tap a lane, swipe left or right, or use the three buttons. A glowing lane shows your destination.","مسیر را لمس کن، چپ و راست بکش یا از سه دکمه استفاده کن. مسیر روشن، مقصد حرکت را نشان می‌دهد."));
        c=ui.card(page);heading(c,t("Action Flight","پرواز اکشن"));body(c,t("Swipe up or tap Jump for orange hurdles. Swipe down or tap Duck for purple beams. Gold rings refill turbo fuel; activate Turbo to smash obstacles. During guardians, clear action gates and collect their rings to drain their health. An empty lane always lets you avoid an action gate.","برای مانع نارنجی بالا بکش یا پرش را بزن. برای تیرک بنفش پایین بکش یا خم‌شدن را بزن. حلقهٔ طلایی سوخت توربو را پر می‌کند؛ با توربو موانع را بشکن. هنگام ورود نگهبان، حرکت‌های عالی و گرفتن حلقه‌ها انرژی او را کم می‌کند. همیشه یک مسیر خالی برای جاخالی‌دادن وجود دارد."));
        c=ui.card(page);heading(c,t("Chain your sparks","جرقه‌ها را زنجیر کن"));
        body(c,t("Collect sparks without long gaps to grow your combo. Energy charges Overdrive: a short bonus burst that can smash barriers. Close calls add points.","جرقه‌ها را پشت سر هم جمع کن تا کمبو بالا برود. انرژی، فراسرعت را شارژ می‌کند: یک فرصت کوتاه برای امتیاز بیشتر و شکستن موانع. عبور نزدیک هم امتیاز می‌دهد."));
        c=ui.card(page);heading(c,t("Choose your pace","ریتمت را انتخاب کن"));
        body(c,t("Gentle difficulty gives you more hearts and slower flying. Zen is a peaceful three-minute flight with no game over or economy rewards. Campaign stars unlock badges.","سختی ملایم فرصت‌های بیشتر و سرعت کمتر دارد. حالت آرام یک پرواز سه‌دقیقه‌ای بدون باخت و پاداش اقتصادی است. ستاره‌های مراحل، نشان‌های تازه را باز می‌کنند."));
        for(int i=0;i<4;i++){c=ui.card(page);heading(c,Catalog.POWER_ICONS[i]+" "+w.t(Catalog.POWERS[i]));
            String[][] explanations={{"Blocks one collision.","یک برخورد را دفع می‌کند."},{"Pulls nearby sparks from other lanes.","جرقه‌های نزدیک مسیرهای دیگر را جذب می‌کند."},{"Slows the circuit while real time continues.","مسیر را کند می‌کند؛ زمان واقعی ادامه دارد."},{"Doubles score for a short time.","برای مدتی امتیاز را دوبرابر می‌کند."}};
            body(c,w.t(explanations[i])+" "+n((int)GameConfig.POWER_SECONDS[i])+t(" seconds."," ثانیه."));
        }
        btn(page,t("Interactive flight school","آموزش تعاملی پرواز"),true,()->startRun(true,GameModes.STUNT));
    }
    /** A simple deliberate step before paid cosmetics; does not establish anyone's age. */
    private void parentGate(Runnable next){
        int left=7+(int)(Math.random()*5),right=6+(int)(Math.random()*5);int answer=left+right;
        LinearLayout content=ui.column();content.setPadding(ui.dp(22),ui.dp(10),ui.dp(22),ui.dp(10));
        body(content,t("Paid collections are optional. Ask a parent before making a purchase.","مجموعه‌های پولی اختیاری‌اند. پیش از خرید از والدین اجازه بگیر."));
        body(content,n(left)+" + "+n(right)+" = ?");
        EditText input=new EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setTextColor(Ui.TEXT);input.setTypeface(ui.regular);ui.add(content,input,56,0);
        new AlertDialog.Builder(this).setTitle(t("Before a purchase","پیش از خرید")).setView(content).setNegativeButton(t("Cancel","لغو"),null).setPositiveButton(t("Continue","ادامه"),(d,id)->{
            int value=0;String raw=input.getText().toString().trim();boolean valid=!raw.isEmpty()&&raw.length()<4;
            for(int i=0;i<raw.length()&&valid;i++){int digit=Character.digit(raw.charAt(i),10);if(digit<0)valid=false;else value=value*10+digit;}
            if(valid&&value==answer)next.run();else message("Answer does not match. You can keep playing for free.","پاسخ درست نیست. می‌توانی رایگان بازی را ادامه بدهی.");
        }).show();
    }

    private void confirm(String title,String detail,Runnable yes){new AlertDialog.Builder(this).setTitle(title).setMessage(detail).setNegativeButton(t("Cancel","لغو"),null).setPositiveButton(t("Continue","ادامه"),(d,which)->yes.run()).show();}
    @Override public void onBackPressed(){if(screen.equals("play")){if(!paused)pauseGame();else resumeGame();}else if(screen.equals("menu"))confirm(t("Exit game?","از بازی خارج می‌شوید؟"),t("Your progress is saved.","پیشرفت ذخیره شده است."),this::finish);else menu();}
    @Override protected void onResume(){super.onResume();if(profile!=null){Profile saved=new Profile(this);profile.render3d=saved.render3d;profile.zone=saved.zone;profile.relaxed=saved.relaxed;if(screen.equals("menu"))menu();}if(audio!=null)audio.resume();}
    @Override protected void onPause(){if(game!=null&&!settled)pauseGame();if(audio!=null)audio.pause();if(profile!=null)profile.save();super.onPause();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus&&game!=null&&!paused&&!settled)pauseGame();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(game!=null)game.pause();if(billing!=null)billing.close();if(audio!=null)audio.release();super.onDestroy();}
}

