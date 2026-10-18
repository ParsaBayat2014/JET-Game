"""Desktop visual QA of the real FlightPainter + Art Java sources.

This is a Java2D adapter preview, not an Android screenshot or a device test.
Generated adapter sources live in an isolated .art-qa/flight-render directory.
Requires the repository JDK17; no additional Java packages or Android emulator.
"""
from pathlib import Path
import argparse, os, re, subprocess, json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / ".art-qa/flight-render"
SOURCE = ROOT / "app/src/main/java"
PACKAGE = SOURCE / "com/toosarax/jetjo"
OUT = ROOT / "docs/previews"

STUBS = {
"android/graphics/Shader.java": r"""package android.graphics;
public class Shader { public enum TileMode {CLAMP,REPEAT,MIRROR} public java.awt.Paint value; }
""",
"android/graphics/LinearGradient.java": r"""package android.graphics;
import java.awt.*; import java.awt.geom.*;
public class LinearGradient extends Shader {
 public LinearGradient(float x1,float y1,float x2,float y2,int[] colors,float[] positions,TileMode mode) {
  if(x1==x2&&y1==y2)y2=y1+1; java.awt.Color[] values=new java.awt.Color[colors.length];for(int i=0;i<colors.length;i++)values[i]=new java.awt.Color(colors[i],true);
  if(positions==null){positions=new float[colors.length];for(int i=0;i<colors.length;i++)positions[i]=(float)i/(colors.length-1);}
  value=new LinearGradientPaint(new Point2D.Float(x1,y1),new Point2D.Float(x2,y2),positions,values);
 }
 public LinearGradient(float x1,float y1,float x2,float y2,int start,int end,TileMode mode) {this(x1,y1,x2,y2,new int[]{start,end},null,mode);}
}
""",
"android/graphics/RadialGradient.java": r"""package android.graphics;
import java.awt.*; import java.awt.geom.*;
public class RadialGradient extends Shader {
 public RadialGradient(float x,float y,float r,int[] colors,float[] positions,TileMode mode) {
  java.awt.Color[] values=new java.awt.Color[colors.length];for(int i=0;i<colors.length;i++)values[i]=new java.awt.Color(colors[i],true);
  if(positions==null){positions=new float[colors.length];for(int i=0;i<colors.length;i++)positions[i]=(float)i/(colors.length-1);}
  value=new RadialGradientPaint(new Point2D.Float(x,y),r,positions,values);
 }
 public RadialGradient(float x,float y,float r,int start,int end,TileMode mode) {this(x,y,r,new int[]{start,end},null,mode);}
}
""",
"android/graphics/Typeface.java": r"""package android.graphics;
public class Typeface {
 public static final int NORMAL=0,BOLD=1; public int style;public java.awt.Font font;
 public static final Typeface DEFAULT=new Typeface(0),DEFAULT_BOLD=new Typeface(1);
 public Typeface(int style){this.style=style;}public static Typeface createFromFile(String path){try{Typeface t=new Typeface(1);t.font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,new java.io.File(path));return t;}catch(Exception e){throw new IllegalArgumentException("font asset",e);}} public static Typeface create(String name,int style){return new Typeface(style);}
}
""",
"android/graphics/RectF.java": r"""package android.graphics;
public class RectF {
 public float left,top,right,bottom;
 public RectF(){} public RectF(float l,float t,float r,float b){set(l,t,r,b);}
 public void set(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;}
 public float width(){return right-left;} public float height(){return bottom-top;}
 public float centerX(){return (left+right)/2;} public float centerY(){return (top+bottom)/2;}
}
""",
"android/graphics/Color.java": r"""package android.graphics;
public final class Color {
 public static final int WHITE=0xffffffff,BLACK=0xff000000,TRANSPARENT=0;
 public static int alpha(int c){return c>>>24;} public static int red(int c){return (c>>16)&255;}
 public static int green(int c){return (c>>8)&255;} public static int blue(int c){return c&255;}
 public static int argb(int a,int r,int g,int b){return a<<24|r<<16|g<<8|b;}
 public static int rgb(int r,int g,int b){return argb(255,r,g,b);}
}
""",
"android/graphics/Paint.java": r"""package android.graphics;
public class Paint {
 public enum Style {FILL,STROKE,FILL_AND_STROKE} public enum Join {ROUND,MITER,BEVEL} public enum Cap {BUTT,ROUND,SQUARE} public enum Align {LEFT,CENTER,RIGHT}
 public static final int ANTI_ALIAS_FLAG=1,FILTER_BITMAP_FLAG=2;
 public int color=0xff000000;public float width=1,textSize=16;public Style style=Style.FILL;public Join join=Join.MITER;public Cap cap=Cap.BUTT;public Align align=Align.LEFT;public Typeface face=Typeface.DEFAULT;public Shader shader;
 public Paint(){}public Paint(int flags){}
 public void setColor(int c){color=c;}public int getColor(){return color;}
 public void setAlpha(int a){color=(color&0xffffff)|Math.max(0,Math.min(255,a))<<24;}public int getAlpha(){return color>>>24;}
 public void setStrokeWidth(float w){width=w;}public float getStrokeWidth(){return width;}
 public void setStyle(Style s){style=s;}public void setStrokeJoin(Join j){join=j;}public void setStrokeCap(Cap c){cap=c;}
 public void setShader(Shader s){shader=s;}public Shader getShader(){return shader;}
 public void setTextSize(float s){textSize=s;}public float getTextSize(){return textSize;}public void setTextAlign(Align a){align=a;}public void setTypeface(Typeface t){face=t;}
 public float measureText(String s){java.awt.Font font=face!=null&&face.font!=null?face.font.deriveFont(textSize):new java.awt.Font("Dialog",face!=null&&face.style==1?1:0,Math.max(1,(int)textSize));return (float)font.getStringBounds(s,new java.awt.font.FontRenderContext(null,true,true)).getWidth();}
 public void setAntiAlias(boolean b){}public void setDither(boolean b){}
}
""",
"android/graphics/Matrix.java": r"""package android.graphics;
public class Matrix {
 public final java.awt.geom.AffineTransform value=new java.awt.geom.AffineTransform();
 public void setScale(float x,float y){value.setToScale(x,y);}
}
""",
"android/graphics/Path.java": r"""package android.graphics;
public class Path {
 public final java.awt.geom.Path2D.Float shape=new java.awt.geom.Path2D.Float();
 public void reset(){shape.reset();}public void moveTo(float x,float y){shape.moveTo(x,y);}public void lineTo(float x,float y){shape.lineTo(x,y);}
 public void quadTo(float a,float b,float c,float d){shape.quadTo(a,b,c,d);}public void cubicTo(float a,float b,float c,float d,float e,float f){shape.curveTo(a,b,c,d,e,f);}
 public void close(){shape.closePath();}
 public void transform(Matrix matrix,Path destination){java.awt.Shape transformed=matrix.value.createTransformedShape(shape);destination.shape.reset();destination.shape.append(transformed,false);}
}
""",
"android/graphics/Canvas.java": r"""package android.graphics;
import java.awt.*;import java.awt.geom.*;import java.util.ArrayDeque;
public class Canvas {
 public Graphics2D g;private final ArrayDeque<Graphics2D> stack=new ArrayDeque<>();
 public Canvas(Graphics2D g){this.g=g;}
 public int save(){stack.push(g);g=(Graphics2D)g.create();return stack.size();}public void restore(){g.dispose();g=stack.pop();}
 public void restoreToCount(int n){while(stack.size()>=n)restore();}
 public void translate(float x,float y){g.translate(x,y);}public void scale(float x,float y){g.scale(x,y);}public void rotate(float a){g.rotate(Math.toRadians(a));}
 public void rotate(float a,float x,float y){g.rotate(Math.toRadians(a),x,y);}
 public boolean clipRect(float l,float t,float r,float b){g.clip(new Rectangle2D.Float(l,t,r-l,b-t));return true;}
 public void drawColor(int c){g.setColor(new java.awt.Color(c,true));Rectangle clip=g.getClipBounds();if(clip!=null)g.fill(clip);}
 private void shape(Shape s,Paint p){
  java.awt.Composite original=g.getComposite();
  if(p.shader!=null){g.setPaint(p.shader.value);g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,p.getAlpha()/255f));}
  else g.setColor(new java.awt.Color(p.color,true));
  if(p.style==Paint.Style.STROKE||p.style==Paint.Style.FILL_AND_STROKE){
   g.setStroke(new BasicStroke(Math.max(.001f,p.width),p.cap==Paint.Cap.ROUND?BasicStroke.CAP_ROUND:p.cap==Paint.Cap.SQUARE?BasicStroke.CAP_SQUARE:BasicStroke.CAP_BUTT,p.join==Paint.Join.ROUND?BasicStroke.JOIN_ROUND:p.join==Paint.Join.BEVEL?BasicStroke.JOIN_BEVEL:BasicStroke.JOIN_MITER));g.draw(s);}
  if(p.style!=Paint.Style.STROKE)g.fill(s);
  g.setComposite(original);
 }
 public void drawCircle(float x,float y,float r,Paint p){shape(new Ellipse2D.Float(x-r,y-r,2*r,2*r),p);}
 public void drawOval(float l,float t,float r,float b,Paint p){shape(new Ellipse2D.Float(l,t,r-l,b-t),p);}public void drawOval(RectF r,Paint p){drawOval(r.left,r.top,r.right,r.bottom,p);}
 public void drawRect(float l,float t,float r,float b,Paint p){shape(new Rectangle2D.Float(l,t,r-l,b-t),p);}public void drawRect(RectF r,Paint p){drawRect(r.left,r.top,r.right,r.bottom,p);}
 public void drawRoundRect(float l,float t,float r,float b,float rx,float ry,Paint p){shape(new RoundRectangle2D.Float(l,t,r-l,b-t,2*rx,2*ry),p);}public void drawRoundRect(RectF r,float rx,float ry,Paint p){drawRoundRect(r.left,r.top,r.right,r.bottom,rx,ry,p);}
 public void drawArc(float l,float t,float r,float b,float start,float sweep,boolean useCenter,Paint p){shape(new Arc2D.Float(l,t,r-l,b-t,-start,-sweep,useCenter?Arc2D.PIE:Arc2D.OPEN),p);}
 public void drawPath(Path path,Paint p){shape(path.shape,p);}
 public void drawLine(float x1,float y1,float x2,float y2,Paint p){Paint.Style old=p.style;p.style=Paint.Style.STROKE;shape(new Line2D.Float(x1,y1,x2,y2),p);p.style=old;}
 public void drawText(String text,float x,float y,Paint p){
  g.setColor(new java.awt.Color(p.color,true));g.setFont(p.face!=null&&p.face.font!=null?p.face.font.deriveFont(p.textSize):new java.awt.Font("Dialog",p.face!=null&&p.face.style==1?Font.BOLD:Font.PLAIN,Math.max(1,(int)p.textSize)));
  float w=g.getFontMetrics().stringWidth(text);g.drawString(text,x-(p.align==Paint.Align.CENTER?w/2:p.align==Paint.Align.RIGHT?w:0),y);
 }
}
""",
}

def method_body(source, marker):
    start = source.index(marker)
    brace = source.index("{", start)
    depth = 1
    cursor = brace + 1
    while depth:
        char = source[cursor]
        depth += (char == "{") - (char == "}")
        cursor += 1
    return source[start:cursor]

def before_source():
    original = (ROOT / "tools/fixtures/GameView-before.java").read_text(encoding="utf-8")
    draw = method_body(original, "@Override protected void onDraw(Canvas c)")
    draw = draw.replace("@Override protected ", "public ")
    draw = re.sub(r"scene\.draw\(c,w,h,clock,profile\.reducedMotion\);", "", draw)
    trail = method_body(original, "private void drawTrail(")
    return """import android.graphics.*;import com.toosarax.jetjo.core.*;import com.toosarax.jetjo.game.Art;import com.toosarax.jetjo.data.Catalog;
public class BeforePainter {
 public final GameEngine engine;
 public float width,height,clock=14.4f,shake=0;
 public Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); public Path path=new Path();public RectF rect=new RectF();
 public float[] px=new float[160],py=new float[160],life=new float[160];public int[] pc=new int[160];
 public static class PreviewProfile {public boolean highContrast,reducedMotion;public int skin=18,trail=4;}
 public PreviewProfile profile=new PreviewProfile();
 public BeforePainter(GameEngine engine,float w,float h,int skin,boolean contrast){this.engine=engine;width=w;height=h;profile.skin=skin;profile.highContrast=contrast;}
 private float getWidth(){return width;}private float getHeight(){return height;}
 private float lane(float x){return width*(.20f+.30f*x);}
 private static final int[] POWER_COLORS={0xff7beaff,0xffc5a1ff,0xffffd77f,0xff8cffb9};
 private static final int[] RAINBOW={0xffff9c87,0xffffd77f,0xff8cffb9,0xff6ae4ff,0xffb9a3ff,0xffff9ccd};
""" + draw + "\n" + trail + "\n}"

def after_source():
    """Extract the actual current GameView drawing and its overlay code."""
    original = (PACKAGE / "game/GameView.java").read_text(encoding="utf-8")
    draw = method_body(original, "@Override protected void onDraw(Canvas canvas)")
    draw = draw.replace("@Override protected ", "public ")
    draw = draw.replace("scene.draw(canvas,width,height,clock,profile.reducedMotion);", "")
    draw = draw.replace("SceneAssets.color(scene.selected())", "accent")
    return """import android.graphics.*;import com.toosarax.jetjo.core.*;import com.toosarax.jetjo.game.*;
public class AfterPainter {
 private final GameEngine engine;private final FlightSnapshot frame;private final float width,height,clock;
 private final Object gl=null;private final FlightPainter painter=new FlightPainter();
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF rect=new RectF();private final Typeface font;
 private final float[] px=new float[160],py=new float[160],life=new float[160];private final int[] colors=new int[160];
 private float shake,flash,calloutTime;private String callout;private final int accent;
 private static class ProfileFixture {boolean reducedMotion;}private final ProfileFixture profile=new ProfileFixture();
 private static class WordsFixture {String t(String en,String fa){return en;}String n(long n){return Long.toString(n);}}private final WordsFixture words=new WordsFixture();
 private static class Metrics {float density=1.5f,scaledDensity=1.5f;}private static class ResourcesFixture {Metrics getDisplayMetrics(){return new Metrics();}}
 private ResourcesFixture getResources(){return new ResourcesFixture();}private float getWidth(){return width;}private float getHeight(){return height;}
 public AfterPainter(String root,GameEngine engine,FlightSnapshot frame,float width,float height,int accent,String callout){
  this.engine=engine;this.frame=frame;this.width=width;this.height=height;this.accent=accent;this.clock=frame.clock;this.callout=callout;calloutTime=callout.isEmpty()?0:.65f;profile.reducedMotion=frame.reducedMotion;
  font=Typeface.createFromFile(root+"/app/src/main/assets/fonts/bold.ttf");
 }
""" + draw + "\n}"

DRIVER = r"""import android.graphics.*;import com.toosarax.jetjo.core.*;import com.toosarax.jetjo.game.*;import com.toosarax.jetjo.data.Catalog;
import java.awt.Graphics2D;import java.awt.RenderingHints;import java.awt.Font;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
public class FlightPreview {
 static GameEngine setup(boolean overdrive){
  GameEngine e=new GameEngine(421);e.mode=GameModes.COIN_RUSH;e.start(false);e.seconds=24;e.speed=.32f;e.playerLane=1;e.targetLane=1;e.combo=12;e.bestCombo=12;e.energy=26;e.points=4280;e.charge=.64f;e.waveWarning=.7f;e.safeLane=1;e.boostFuel=.64f;e.stunts=8;e.lives=3;
  e.spawn(GameEngine.WALL,0,.28f);e.spawn(GameEngine.ENERGY,1,.28f);e.spawn(GameEngine.ENERGY,1,.42f);e.spawn(GameEngine.WALL,2,.54f);
  e.spawn(GameEngine.ENERGY,1,.56f);e.spawn(GameEngine.SHIELD,0,.68f);e.spawn(GameEngine.ENERGY,1,.71f);
  for(GameEngine.Entity item:e.entities)if(item.active){item.style=1;item.pattern=GameEngine.GATES;}
  if(overdrive){e.overdrive=3.2f;e.power[1]=3;}
  return e;
 }
 static void button(Graphics2D g,int x,int y,int width,int height,String label,boolean primary){
  g.setColor(new java.awt.Color(primary?0xff8cffb9:0xff111e2a,true));g.fillRoundRect(x,y,width,height,22,22);
  g.setColor(new java.awt.Color(primary?0xff080d19:0xffeff8f4,true));g.setFont(new Font("Dialog",Font.BOLD,20));
  int text=g.getFontMetrics().stringWidth(label);g.drawString(label,x+(width-text)/2,y+(height+g.getFontMetrics().getAscent()-g.getFontMetrics().getDescent())/2);
 }
 static void frame(String root,String file,boolean before,boolean overdrive,boolean duo,boolean contrast)throws Exception{frame(root,file,before,overdrive,duo,contrast,false);}
 static void frame(String root,String file,boolean before,boolean overdrive,boolean duo,boolean contrast,boolean countdown)throws Exception{
  int w=540,h=960,fieldTop=duo?188:230,fieldHeight=duo?620:506;
  BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
  g.setColor(new java.awt.Color(0xff080d19,true));g.fillRect(0,0,w,h);
  BufferedImage bg=ImageIO.read(new File(root+"/app/src/main/assets/content/worlds/forest.jpg"));float scale=Math.max(w/(float)bg.getWidth(),fieldHeight/(float)bg.getHeight());
  int bw=Math.round(bg.getWidth()*scale),bh=Math.round(bg.getHeight()*scale);java.awt.Shape previousClip=g.getClip();g.clipRect(0,fieldTop,w,fieldHeight);g.drawImage(bg,(w-bw)/2,fieldTop+(fieldHeight-bh)/2,bw,bh,null);g.setClip(previousClip);
  g.setFont(new Font("Dialog",Font.BOLD,19));g.setColor(new java.awt.Color(0xff8cffb9,true));g.drawString(duo?"LOCAL DUEL / 51":"ACTION FLIGHT / "+(countdown?"99":"76"),28,60);
  button(g,303,23,208,76,"II  Pause",false);
  g.setFont(new Font("Dialog",Font.BOLD,duo?29:44));g.setColor(new java.awt.Color(0xffeaf6ef,true));g.drawString(duo?"P1  4280        P2  3690":"4,280",28,148);
  g.setFont(new Font("Dialog",Font.PLAIN,17));g.setColor(new java.awt.Color(0xffffd779,true));g.drawString(duo?"Combo 12 / 26 sparks   |   Combo 9 / 21":"COMBO 12 / x3 / 26 sparks / Charge 64%",28,181);
  if(!duo){g.setColor(new java.awt.Color(0xff9baebd,true));g.drawString(overdrive?"Magnet 3 / Stunts 8 / Guardian 1 / Hearts 3":"Stunts 8 / Guardians 0 of 3 / Hearts 3",28,213);}
  Canvas c=new Canvas(g);c.save();c.translate(0,fieldTop);c.clipRect(0,0,w,fieldHeight);
  GameEngine e=setup(overdrive);
  if(!before&&!duo){e.mode=GameModes.STUNT;e.spawn(GameEngine.JUMP_BARRIER,0,.50f);e.spawn(GameEngine.DUCK_BEAM,2,.39f);e.spawn(GameEngine.BOOST_RING,1,.15f);if(overdrive){e.bossActive=true;e.bossPhase=1;e.bossHealth=.70f;}}
  if(countdown){e.seconds=1.2f;for(GameEngine.Entity item:e.entities)item.active=false;}
  int pilots=duo?2:1;
  for(int pilot=0;pilot<pilots;pilot++){
   c.save();c.translate(pilot*w/(float)pilots,0);c.clipRect(0,0,w/(float)pilots,fieldHeight);float width=w/(float)pilots;
   if(before)new BeforePainter(e,width,fieldHeight,pilot==0?18:19,contrast).onDraw(c);
   else { /* ACTUAL_PAINTER_CALL */ }
   c.restore();
  }
  c.restore();
  int controlsTop=duo?820:748,n=duo?6:3;
  for(int i=0;i<n;i++)button(g,16+i*(w-32)/n,controlsTop,(w-32)/n-8,72,i%3==0?"<":i%3==1?"o":">",false);
  if(!duo){button(g,16,832,160,72,"UP  Jump",false);button(g,186,832,160,72,"DOWN Duck",false);button(g,356,832,168,72,overdrive?"TURBO ACTIVE":"Turbo 64%",!overdrive);}
  g.setFont(new Font("Dialog",Font.PLAIN,11));g.setColor(new java.awt.Color(0xffa9bbc7,true));g.drawString("JAVA2D SOURCE PREVIEW / "+(before?"HISTORICAL DRAWING":"ACTUAL FLIGHTPAINTER + GAMEVIEW"),12,936);
  g.drawString("HUD / controls are static layout fixtures. No Android device execution.",12,952);
  g.dispose();ImageIO.write(image,"png",new File(file));
 }
 public static void main(String[] args)throws Exception{
  String root=args[0],out=args[1];frame(root,out+"/flight-before.png",true,false,false,false);
  frame(root,out+"/flight-after.png",false,false,false,false);frame(root,out+"/flight-after-overdrive.png",false,true,false,false);
  frame(root,out+"/flight-after-duo.png",false,false,true,false);frame(root,out+"/flight-after-contrast.png",false,false,false,true);
  frame(root,out+"/flight-after-countdown.png",false,false,false,false,true);
  System.out.println("PASS: actual FlightPainter/Art/current GameView overlay source, historical drawing. Desktop Java2D only.");
 }
}
"""
def painter_call():
    """Invoke shipped rendering source; the HUD outside it is a labelled fixture."""
    return """FlightSnapshot frame=FlightSnapshot.capture(e,pilot==0?18:19,4,14.4f,false,contrast,8);if(duo)new FlightPainter().draw(c,frame,width,fieldHeight,0xff72e484);else new AfterPainter(root,e,frame,width,fieldHeight,0xff72e484,countdown?"":overdrive?"GUARDIAN 1":"PERFECT +150").onDraw(c);"""

def preview(before_only=False, work=None):
    global WORK
    if work is not None:
        WORK = work.resolve()
    OUT.mkdir(parents=True, exist_ok=True)
    WORK.mkdir(parents=True, exist_ok=True)
    for name, content in STUBS.items():
        path = WORK / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
    (WORK / "BeforePainter.java").write_text(before_source(), encoding="utf-8")
    (WORK / "AfterPainter.java").write_text(after_source(), encoding="utf-8")
    driver = DRIVER.replace("/* ACTUAL_PAINTER_CALL */", "" if before_only else painter_call())
    if before_only:
        driver = re.sub(r'  frame\(root,out\+"/flight-after[^\n]*\n', "", driver)
        driver = driver.replace("actual FlightPainter/Art source and previous GameView drawing method", "previous GameView drawing method with actual Art source")
    (WORK / "FlightPreview.java").write_text(driver, encoding="utf-8")
    sources = list((WORK / "android").rglob("*.java")) + [WORK / "BeforePainter.java", WORK / "AfterPainter.java", WORK / "FlightPreview.java"]
    sources += [PACKAGE / ("core/" + name + ".java") for name in ("GameConfig", "GameModes", "StageCatalog", "GameEngine")]
    sources += [PACKAGE / "game/Art.java"]
    sources += [PACKAGE / ("game/" + name + ".java") for name in ("FlightPerspective", "FlightPainter", "FlightSnapshot")]
    sources += [PACKAGE / "data/Catalog.java", PACKAGE / "multiplayer/LanProtocol.java"]
    javac = Path(os.environ.get("JAVA_HOME", "D:/AndroidToolchain/jdk-17.0.20.1+1")) / "bin/javac.exe"
    java = javac.with_name("java.exe")
    classes = WORK / "classes"
    classes.mkdir(exist_ok=True)
    subprocess.run([str(javac), "-encoding", "UTF-8", "-d", str(classes), *map(str, sources)], check=True, cwd=ROOT)
    subprocess.run([str(java), "-Djava.awt.headless=true", "-cp", str(classes), "FlightPreview", str(ROOT), str(OUT)], check=True, cwd=ROOT)
    if before_only:
        print("Rendered previous GameView drawing source using desktop Java2D, no device claims.")
        return
    labels = [("flight-before.png", "Previous flat track"), ("flight-after.png", "Perspective flight"), ("flight-after-overdrive.png", "Overdrive"), ("flight-after-duo.png", "Same-phone duel")]
    sheet = Image.new("RGB", (1080, 2 * 992), (8, 13, 25))
    draw = ImageDraw.Draw(sheet)
    for i, (file, title) in enumerate(labels):
        x = (i % 2) * 540
        y = (i // 2) * 992
        draw.text((x + 14, y + 10), title + " - DESKTOP SOURCE PREVIEW", fill=(220, 239, 237))
        with Image.open(OUT / file) as image:
            sheet.paste(image, (x, y + 32))
    sheet.save(OUT / "flight-comparison.png")
    print(json.dumps({"renderer": "actual Java source with desktop Canvas adapter", "device_execution": False, "hud_and_controls": "static layout fixtures; current GameView gameplay overlays rendered from source", "outputs": [file for file, _ in labels] + ["flight-after-contrast.png", "flight-after-countdown.png", "flight-comparison.png"]}, indent=2))

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--before-only", action="store_true")
    parser.add_argument("--work-dir", type=Path, help="New isolated harness directory; proves no dependency on existing .art-qa snapshots/classes")
    args = parser.parse_args()
    preview(args.before_only, args.work_dir)
