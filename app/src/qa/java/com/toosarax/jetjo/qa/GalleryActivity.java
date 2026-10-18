package com.toosarax.jetjo.qa;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.core.GameModes;
import com.toosarax.jetjo.data.Catalog;
import com.toosarax.jetjo.data.Profile;
import com.toosarax.jetjo.game.Art;
import com.toosarax.jetjo.game.FlightGLView;
import com.toosarax.jetjo.game.FlightPainter;
import com.toosarax.jetjo.game.FlightSnapshot;
import com.toosarax.jetjo.game.HeroView;
import com.toosarax.jetjo.game.SceneAssets;
import com.toosarax.jetjo.ui.Ui;
import com.toosarax.jetjo.ui.Words;
import java.util.ArrayList;
import java.util.List;

/**
 * QA-only native promotional capture scenes. Uses production meshes, painter, art and world assets.
 * Curated frozen gameplay fixtures never run purchases, records, progression or a network match.
 */
public final class GalleryActivity extends Activity {
    private static final String TAG="JetGallery";
    private static final String[] NAMES={"01-jet","02-action-3d","03-guardian-turbo","04-together-3d","05-candy-25d","06-worlds-crew"};
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<CanvasFlight> canvases=new ArrayList<>();
    private Profile profile;
    private Ui ui;
    private FlightGLView gl;
    private LinearLayout page;
    private int sceneIndex,generation;
    private boolean destroyed,automatic;
    private final int mint=0xff8cffd7,gold=0xffffd77f,pink=0xffffa5d5,white=0xffedf8ff,muted=0xffa7bdcf;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        profile=new Profile(this);profile.language="fa";profile.sound=false;profile.music=false;profile.haptic=false;
        profile.reducedMotion=true;profile.highContrast=false;profile.skin=0;profile.trail=3;profile.zone=0;
        ui=new Ui(this,new Words(profile));
        automatic=!getIntent().hasExtra("gallery_scene");
        show(Math.max(0,Math.min(5,getIntent().getIntExtra("gallery_scene",0))));
    }
    private int dp(float v){return ui.dp(v);}
    private TextView text(String value,float size,int color,boolean bold){
        TextView view=ui.text(value,size,color,bold);view.setGravity(Gravity.CENTER);return view;
    }
    private GradientDrawable panel(int color,int accent,int radius){
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(radius));shape.setStroke(dp(1),accent);return shape;
    }
    private void add(LinearLayout parent,View view,int height,int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));p.bottomMargin=dp(margin);parent.addView(view,p);
    }
    private void weighted(LinearLayout parent,View view,float weight,int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,weight);p.bottomMargin=dp(margin);parent.addView(view,p);
    }
    private LinearLayout col(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);return c;}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setBaselineAligned(false);return r;}
    private TextView pill(String value,int accent){
        TextView v=text(value,11,accent,true);v.setPadding(dp(13),dp(7),dp(13),dp(7));v.setBackground(panel(0xdd112638,0x444da79d,24));return v;
    }
    private void pills(LinearLayout parent,String first,String second,int accent){
        LinearLayout r=row();r.setGravity(Gravity.CENTER);
        r.addView(pill(first,accent));TextView gap=text("  ",12,accent,false);r.addView(gap);r.addView(pill(second,white));add(parent,r,-2,16);
    }
    private void heading(String badge,String title,String subtitle,int accent){
        TextView b=text(badge,11,accent,true);b.setLetterSpacing(.10f);add(page,b,-2,9);
        add(page,text(title,30,white,true),-2,7);add(page,text(subtitle,12,muted,false),-2,17);
    }
    private void caption(String title,String body,int accent){
        LinearLayout card=col();card.setPadding(dp(18),dp(12),dp(18),dp(13));card.setBackground(panel(0xeb102435,0xff2a465b,19));
        add(card,text(title,17,accent,true),-2,5);add(card,text(body,11,muted,false),-2,0);add(page,card,-2,0);
    }
    private void cleanup(){
        if(gl!=null){gl.releaseScene();gl=null;}for(CanvasFlight c:canvases)c.release();canvases.clear();
    }
    private void show(int index){
        if(destroyed)return;cleanup();sceneIndex=index;int expected=++generation;
        FrameLayout root=new FrameLayout(this);root.addView(new Backdrop(this,index),new FrameLayout.LayoutParams(-1,-1));
        page=col();page.setPadding(dp(24),dp(27),dp(24),dp(24));root.addView(page,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
        if(index==0)brand();else if(index==1)action(false);else if(index==2)action(true);else if(index==3)together();else if(index==4)candy();else collection();
        Log.i(TAG,"SCENE "+index+" "+NAMES[index]);
        if(gl==null)main.postDelayed(()->ready(expected),2600);
        else{
            final FlightGLView currentRenderer=gl;
            currentRenderer.setReadyListener(()->main.postDelayed(()->ready(expected),500));
            currentRenderer.setFailureListener(()->{if(!destroyed&&expected==generation&&gl==currentRenderer)Log.e(TAG,"FAILED "+index+" "+currentRenderer.failureReason());});
            currentRenderer.resumeScene();
        }
        if(automatic&&index<5){
            page.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){
                @Override public void onLayoutChange(View view,int l,int t,int r,int b,int ol,int ot,int or,int ob){
                    if(r<=l||b<=t)return;view.removeOnLayoutChangeListener(this);
                    main.postDelayed(()->{if(!destroyed&&expected==generation)show(index+1);},25000);
                }
            });
        }
    }
    private void ready(int expected){
        if(!destroyed&&expected==generation)Log.i(TAG,"READY "+sceneIndex+" "+NAMES[sceneIndex]+" "+(gl==null?"Canvas":"GLES2"));
    }
    private void brand(){
        pills(page,"نسخهٔ ۲","آفلاین + دونفره",mint);
        TextView title=text("جت",76,white,true);add(page,title,-2,1);
        add(page,text("پرواز کن. جاخالی بده. بدرخش.",19,mint,true),-2,8);
        add(page,text("یک سفر رنگی، پر از سرعت و رفیق",12,muted,false),-2,0);
        FrameLayout hero=new FrameLayout(this);hero.addView(new HeroView(this,0,false,true),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout.LayoutParams crew=new FrameLayout.LayoutParams(-1,dp(115),Gravity.BOTTOM);hero.addView(new CrewStrip(this,new int[]{18,19,20,33},false),crew);
        weighted(page,hero,1,20);
        TextView start=text("شروع پرواز اکشن   ›",18,0xff071b1d,true);start.setPadding(dp(12),dp(16),dp(12),dp(16));
        start.setBackground(panel(mint,0xffb9fff1,18));add(page,start,-2,15);
        caption("از تک‌نفره تا یک رقابت دوستانه","پرواز سه‌بعدی • جهان‌های رنگی • شخصیت‌های تازه",white);
    }
    private GameEngine fixture(int variant){
        GameEngine e=new GameEngine(87231+variant);e.mode=GameModes.STUNT;e.start(false);e.seconds=variant==2?27.5f:13.3f;
        e.speed=.36f;e.points=variant==2?8420:3240;e.combo=variant==2?18:9;e.boostFuel=.72f;
        e.playerLane=1;e.targetLane=1;e.lives=3;
        e.spawn(GameEngine.JUMP_BARRIER,0,.61f);e.spawn(GameEngine.ENERGY,1,.62f);e.spawn(GameEngine.ENERGY,1,.48f);
        e.spawn(GameEngine.DUCK_BEAM,2,.33f);e.spawn(GameEngine.ENERGY,1,.33f);e.spawn(GameEngine.BOOST_RING,1,.06f);
        e.spawn(GameEngine.ENERGY,1,-.10f);e.spawn(GameEngine.WALL,2,-.10f);e.spawn(GameEngine.WALL,0,-.25f);
        e.spawn(GameEngine.SHIELD,2,.06f);e.spawn(GameEngine.ENERGY,1,-.28f);
        if(variant==2){e.overdrive=3.1f;e.power[0]=4;e.bossActive=true;e.bossPhase=2;e.bossHealth=.68f;e.bossWarning=0;}
        if(variant==3){e.playerLane=.4f;e.targetLane=0;e.jumpTime=GameConfig.JUMP_SECONDS*.57f;}
        if(variant==4){e.playerLane=1.6f;e.targetLane=2;e.duckTime=.6f;e.overdrive=2;}
        return e;
    }
    private FrameLayout stage(boolean use3d,GameEngine e,int world,int skin,float clock){
        FrameLayout frame=new FrameLayout(this);profile.skin=skin;profile.zone=world;
        if(use3d){gl=new FlightGLView(this,profile);gl.setWorld(world);frame.addView(gl,new FrameLayout.LayoutParams(-1,-1));gl.publish(e,clock);}
        else{CanvasFlight flight=new CanvasFlight(this,e,world,skin,clock);canvases.add(flight);frame.addView(flight,new FrameLayout.LayoutParams(-1,-1));}
        FrameLayout.LayoutParams status=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);status.topMargin=dp(14);
        frame.addView(pill(use3d?"سه‌بعدی":"۲٫۵بعدی",use3d?mint:pink),status);
        FrameLayout.LayoutParams points=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);points.bottomMargin=dp(17);
        TextView hud=text(e.bossActive?"توربو روشن  •  کمبو ۱۸":"۳٬۲۴۰   •   کمبو ۹",14,e.bossActive?gold:white,true);
        hud.setPadding(dp(15),dp(8),dp(15),dp(8));hud.setBackground(panel(0xc5091727,0x334f8b9f,16));frame.addView(hud,points);
        return frame;
    }
    private void controls(LinearLayout parent,int accent,boolean pair){
        LinearLayout r=row();String[] labels={"پرش  ↑","خم‌شدن  ↓","توربو  ⚡"};
        for(int i=0;i<labels.length;i++){TextView b=text(labels[i],pair?11:13,i==2?gold:accent,true);b.setPadding(dp(4),dp(13),dp(4),dp(13));b.setBackground(panel(0xeb102537,0xff34516b,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(i>0)p.leftMargin=dp(7);r.addView(b,p);}add(parent,r,-2,12);
    }
    private void action(boolean guardian){
        heading(guardian?"T U R B O   /   G U A R D I A N":"F L I G H T   /   3 D",
                guardian?"توربو بزن، نگهبان را شکست بده":"هیجان از همین‌جا شروع می‌شود",
                guardian?"سپر، سرعت و یک چالش بزرگ‌تر":"پرش، جاخالی و حلقه‌های انرژی",guardian?gold:mint);
        GameEngine e=fixture(guardian?2:1);
        weighted(page,stage(true,e,guardian?1:0,guardian?14:0,guardian?27.5f:13.3f),1,15);
        controls(page,guardian?gold:mint,false);
        caption(guardian?"سرعت بیشتر، امتیاز بیشتر":"در مسیر انرژی بمان",
                guardian?"سه نگهبان در پرواز اکشن • قدرت‌های ویژه":"مانع کوتاه را بپر؛ از زیر مانع بلند رد شو",guardian?gold:mint);
    }
    private void together(){
        heading("T W O   P I L O T S","دونفره، کنار هم","رقابت یا همکاری؛ انتخاب با شماست",pink);
        LinearLayout names=row();
        TextView a=pill("لومو  •  بازیکن ۱",pink),b=pill("بوبو  •  بازیکن ۲",mint);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.rightMargin=dp(8);names.addView(a,np);names.addView(b,new LinearLayout.LayoutParams(0,-2,1));add(page,names,-2,12);
        profile.skin=18;profile.zone=4;gl=new FlightGLView(this,profile);FrameLayout field=new FrameLayout(this);
        field.addView(gl,new FrameLayout.LayoutParams(-1,-1));field.addView(new Divider(this),new FrameLayout.LayoutParams(-1,-1));
        GameEngine left=fixture(3),right=fixture(4);
        gl.publishPair(FlightSnapshot.capture(left,18,3,19,true,false,4),FlightSnapshot.capture(right,33,6,19,true,false,4),.5f);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);hp.topMargin=dp(12);
        field.addView(pill("پرواز اکشن  •  سه‌بعدی",white),hp);weighted(page,field,1,14);
        LinearLayout both=row();LinearLayout lc=col(),rc=col();LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.rightMargin=dp(12);both.addView(lc,bp);both.addView(rc,new LinearLayout.LayoutParams(0,-2,1));controls(lc,pink,true);controls(rc,mint,true);add(page,both,-2,0);
        caption("دو خلبان، دو کنترل مستقل","اکشن روی یک گوشی • رقابت امتیازی با وای‌فای محلی",white);
    }
    private void candy(){
        heading("W O R L D S   /   2 . 5 D","سفر به کهکشان آب‌نبات","یک جهان تازه، یک حال‌وهوای تازه",pink);
        GameEngine e=fixture(1);e.jumpTime=.52f;e.playerLane=1.25f;e.targetLane=2;e.power[0]=5;
        weighted(page,stage(false,e,3,28,13.3f),1,15);controls(page,pink,false);
        caption("رنگی، روان و پر از شخصیت","نمای ۲٫۵بعدی با عمق و سایه • مناسب پروازهای طولانی",pink);
    }
    private void collection(){
        heading("Y O U R   N E X T   A D V E N T U R E","هر بار، یک کشف تازه","۱۶ جهان • ۳۶ شخصیت • ۴۸ مرحله",mint);
        LinearLayout grid=col();int[] worlds={0,3,2,5,8,12};
        for(int y=0;y<3;y++){
            LinearLayout line=row();for(int x=0;x<2;x++){
                int world=worlds[y*2+x];FrameLayout card=new FrameLayout(this);card.setBackground(panel(0xff15283d,0xff375469,18));card.setClipToOutline(true);
                card.addView(new SceneAssets.Preview(this,world),new FrameLayout.LayoutParams(-1,-1));
                TextView label=text(SceneAssets.nameFa(world),12,white,true);label.setPadding(dp(5),dp(9),dp(5),dp(9));label.setBackgroundColor(0xcc0b1727);
                card.addView(label,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);if(x==0)cp.rightMargin=dp(10);line.addView(card,cp);
            }
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,0,1);rp.bottomMargin=dp(10);grid.addView(line,rp);
        }
        weighted(page,grid,2.2f,10);
        add(page,text("رفیق پروازت را پیدا کن",17,white,true),-2,0);
        weighted(page,new CrewStrip(this,new int[]{18,19,20,33,29},true),.80f,8);
        LinearLayout campaign=col();campaign.setBackground(panel(0xd6112536,0xff2b4861,19));campaign.setPadding(dp(10),dp(10),dp(10),dp(10));
        add(campaign,text("سفر مرحله‌ای  •  ۴۸ چالش",16,gold,true),-2,5);
        weighted(campaign,new CampaignRoute(this),1,0);weighted(page,campaign,.90f,0);
    }
    @Override protected void onPause(){if(gl!=null)gl.pauseScene();super.onPause();}
    @Override protected void onResume(){super.onResume();if(gl!=null)gl.resumeScene();}
    @Override protected void onDestroy(){destroyed=true;generation++;main.removeCallbacksAndMessages(null);cleanup();super.onDestroy();}

    @android.annotation.SuppressLint("ViewConstructor")
    private final class CanvasFlight extends View{
        private final SceneAssets assets;private final FlightPainter painter=new FlightPainter();private final FlightSnapshot frame;private final int world;
        CanvasFlight(Context c,GameEngine engine,int world,int skin,float clock){super(c);this.world=world;assets=new SceneAssets(c);assets.select(world);frame=FlightSnapshot.capture(engine,skin,3,clock,true,false,world);}
        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();postDelayed(this::invalidate,400);postDelayed(this::invalidate,1200);postDelayed(this::invalidate,2300);}
        @Override protected void onDraw(Canvas c){assets.draw(c,getWidth(),getHeight(),frame.clock,true);painter.draw(c,frame,getWidth(),getHeight(),SceneAssets.color(world));}
        void release(){assets.release();}
    }
    @android.annotation.SuppressLint("ViewConstructor")
    private final class CrewStrip extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();private final int[] skins;private final boolean tiles;
        CrewStrip(Context c,int[] skins,boolean tiles){super(c);this.skins=skins;this.tiles=tiles;}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight(),cell=w/skins.length;RectF r=new RectF();
            for(int i=0;i<skins.length;i++){
                if(tiles){p.setColor(0xbb152b3e);r.set(i*cell+dp(4),dp(7),(i+1)*cell-dp(4),h-dp(7));c.drawRoundRect(r,dp(17),dp(17),p);}
                Art.ship(c,p,path,(i+.5f)*cell,h*.44f,Math.min(cell*.31f,h*.26f),skins[i],0,false);
            }
        }
    }
    @android.annotation.SuppressLint("ViewConstructor")
    private final class CampaignRoute extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        CampaignRoute(Context c){super(c);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight(),r=Math.min(w*.030f,h*.19f);p.setStrokeWidth(dp(2));p.setColor(0xff34596b);
            for(int i=0;i<7;i++){float x1=w*(.08f+i*.12f),y1=h*(i%2==0?.40f:.64f),x2=w*(.08f+(i+1)*.12f),y2=h*((i+1)%2==0?.40f:.64f);c.drawLine(x1,y1,x2,y2,p);}
            for(int i=0;i<8;i++){
                float x=w*(.08f+i*.12f),y=h*(i%2==0?.40f:.64f);p.setColor(i<3?mint:i==3?gold:0xff27495b);c.drawCircle(x,y,r,p);
                p.setStyle(Paint.Style.STROKE);p.setColor(i==3?0x88ffd77f:0xff527486);c.drawCircle(x,y,r+dp(3),p);p.setStyle(Paint.Style.FILL);
                if(i<3){p.setColor(0xff163c37);p.setStrokeWidth(dp(2));c.drawLine(x-r*.42f,y,x-r*.02f,y+r*.35f,p);c.drawLine(x-r*.02f,y+r*.35f,x+r*.45f,y-r*.38f,p);}
            }
        }
    }
    @android.annotation.SuppressLint("ViewConstructor")
    private final class Divider extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Divider(Context c){super(c);}
        @Override protected void onDraw(Canvas c){p.setColor(0xff224757);c.drawRect(getWidth()/2f-dp(1),0,getWidth()/2f+dp(1),getHeight(),p);}
    }
    @android.annotation.SuppressLint("ViewConstructor")
    private final class Backdrop extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int index;
        Backdrop(Context c,int index){super(c);this.index=index;}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();int top=index==4?0xff241732:index==2?0xff1f2335:0xff122637;
            p.setShader(new LinearGradient(0,0,w,h,new int[]{top,0xff091426,0xff07111e},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            int accent=index==4?pink:index==2?gold:mint;
            for(int i=0;i<54;i++){float x=(i*73%997)/997f*w,y=(i*137%991)/991f*h;p.setColor((accent&0xffffff)|((i%3==0?85:35)<<24));c.drawCircle(x,y,dp(i%7==0?1.5f:.7f),p);}
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor((accent&0xffffff)|0x11000000);
            for(int i=0;i<4;i++)c.drawCircle(w*.84f,h*.12f,dp(70+i*26),p);p.setStyle(Paint.Style.FILL);
        }
    }
}
