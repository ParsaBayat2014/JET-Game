package com.toosarax.jetjo.game;

import android.graphics.*;
import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.data.Catalog;

/** Perspective ground plane, extruded hazards and layered characters; bounded reusable Canvas geometry. */
public final class FlightPainter {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final RectF rect=new RectF();
    private final int[] order=new int[96];
    private Shader roadShade,skyShade;
    private float oldW,oldH;
    private int oldAccent;
    private static final int[] POWERS={0xff7beaff,0xffc5a1ff,0xffffd77f,0xff8cffb9};
    private static final int[] RAINBOW={0xffff9c87,0xffffd77f,0xff8cffb9,0xff6ae4ff,0xffb9a3ff,0xffff9ccd};
    private float x(float w,float lane,float y,FlightSnapshot f){return FlightPerspective.screenX(w,lane,y,f.clock,f.reducedMotion);}
    private float y(float h,float depth){return FlightPerspective.screenY(h,depth);}
    private int shade(int color,float factor){return 0xff000000|((int)(((color>>16)&255)*factor)<<16)|((int)(((color>>8)&255)*factor)<<8)|(int)((color&255)*factor);}
    private void polygon(Canvas c,int color,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4){
        p.setColor(color);p.setStyle(Paint.Style.FILL);path.reset();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.lineTo(x4,y4);path.close();c.drawPath(path,p);
    }
    public void draw(Canvas c,FlightSnapshot f,float w,float h,int accent){
        if(w<=0||h<=0)return;
        if(oldW!=w||oldH!=h||oldAccent!=accent){
            oldW=w;oldH=h;oldAccent=accent;
            roadShade=new LinearGradient(0,h*.10f,0,h,new int[]{0xff172943,0xff111c32,0xff080e1d},null,Shader.TileMode.CLAMP);
            skyShade=new LinearGradient(0,0,0,h*.38f,new int[]{0x10040917,0x78040917,0x00040917},null,Shader.TileMode.CLAMP);
        }
        p.setShader(skyShade);c.drawRect(0,0,w,h*.4f,p);p.setShader(null);
        p.setColor(f.highContrast?0xbb07101e:0x5007101e);c.drawRect(0,h*.10f,w,h,p);
        drawScenery(c,f,w,h,accent);
        drawRoad(c,f,w,h,accent);
        int count=Math.min(order.length,f.count);
        for(int i=0;i<count;i++){int j=i;while(j>0&&f.y[order[j-1]]>f.y[i]){order[j]=order[j-1];j--;}order[j]=i;}
        boolean drawnPilot=false;
        for(int i=0;i<count;i++){
            int at=order[i];if(f.y[at]<-.35f||f.y[at]>1.1f)continue;
            if(!drawnPilot&&f.y[at]>GameConfig.PLAYER_Y){drawPilot(c,f,w,h);drawnPilot=true;}
            drawEntity(c,f,w,h,at);
        }
        if(!drawnPilot)drawPilot(c,f,w,h);
        if(f.bossActive||f.bossWarning>0)drawGuardian(c,f,w,h,accent);
        if(f.overdrive>0&&!f.reducedMotion)drawSpeed(c,f,w,h);
        p.setShader(null);p.setAlpha(255);p.setStyle(Paint.Style.FILL);
    }
    private void drawScenery(Canvas c,FlightSnapshot f,float w,float h,int accent){
        float travel=f.reducedMotion?0:f.clock*f.speed*.32f;
        for(int side=0;side<2;side++)for(int i=0;i<11;i++){
            float depth=(i*.115f+travel)%1.22f-.24f,s=FlightPerspective.scale(depth);
            float xx=x(w,side==0?-1.04f:3.04f,depth,f),yy=y(h,depth);
            float width=w*.11f*s,height=h*(.11f+(i%3)*.035f)*s,ex=width*.32f;
            if(drawThemedSide(c,f.world,xx,yy,width,height,ex,accent,i))continue;
            int tint=i%3==0?accent:0xff4864a5;
            polygon(c,shade(tint,.18f),xx-width*.5f,yy-height,xx+width*.5f,yy-height,xx+width*.5f,yy,xx-width*.5f,yy);
            polygon(c,shade(tint,.10f),xx+width*.5f,yy-height,xx+width*.5f+ex,yy-height-ex,xx+width*.5f+ex,yy-ex,xx+width*.5f,yy);
            polygon(c,shade(tint,.32f),xx-width*.5f,yy-height,xx-width*.5f+ex,yy-height-ex,xx+width*.5f+ex,yy-height-ex,xx+width*.5f,yy-height);
            p.setColor((tint&0xffffff)|0x98000000);p.setStrokeWidth(Math.max(1,w*.003f*s));
            c.drawLine(xx-width*.5f,yy-height,xx-width*.5f,yy,p);
            for(int row=1;row<=3;row++){p.setColor((tint&0xffffff)|0x80000000);c.drawLine(xx-width*.26f,yy-height+row*height*.22f,xx+width*.25f,yy-height+row*height*.22f,p);}
        }
    }
    private boolean drawThemedSide(Canvas c,int world,float x,float floor,float width,float height,float depth,int accent,int index){
        if(world==0||world==8){
            p.setColor(0xff19392e);c.drawRect(x-width*.11f,floor-height*.6f,x+width*.11f,floor,p);
            p.setColor(0xff1c5844);rect.set(x-width*.7f,floor-height,x+width*.7f,floor-height*.29f);c.drawOval(rect,p);
            p.setColor(0xff23715a);rect.set(x-width*.51f,floor-height*1.07f,x+width*.46f,floor-height*.53f);c.drawOval(rect,p);
            p.setColor(accent);c.drawCircle(x-width*.24f,floor-height*.7f,Math.max(1,width*.025f),p);return true;
        }
        if(world==2||world==9){
            p.setColor(0xff386f7b);p.setStrokeWidth(Math.max(1,width*.13f));c.drawLine(x,floor,x,floor-height*.8f,p);
            c.drawLine(x,floor-height*.22f,x-width*.43f,floor-height*.59f,p);c.drawLine(x,floor-height*.44f,x+width*.49f,floor-height*.90f,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(0x8866dfe5);p.setStrokeWidth(Math.max(1,width*.03f));
            c.drawCircle(x-width*.3f,floor-height*1.03f,width*.24f,p);c.drawCircle(x+width*.31f,floor-height*.41f,width*.15f,p);p.setStyle(Paint.Style.FILL);return true;
        }
        if(world==5||world==14||world==13){
            int tint=world==13?0xffd76b47:world==14?0xffa0d1ec:0xff9690e3;
            polygon(c,shade(tint,.7f),x,floor-height,x+width*.59f,floor-height*.28f,x+width*.38f,floor,x-width*.42f,floor);
            polygon(c,shade(tint,.38f),x,floor-height,x-width*.64f,floor-height*.29f,x-width*.42f,floor,x,floor-height*.10f);
            p.setColor(world==13?0xffffb865:0xffdcf5ff);p.setStrokeWidth(Math.max(1,width*.03f));c.drawLine(x,floor-height,x+width*.20f,floor-height*.45f,p);return true;
        }
        if(world==3||world==7||world==10){
            if(index%2==0){p.setColor(0xff684e6a);c.drawRect(x-width*.07f,floor-height*.85f,x+width*.07f,floor,p);p.setColor(index%4==0?0xffb0769b:0xff788fba);c.drawCircle(x,floor-height*.74f,width*.63f,p);p.setColor(0xffe3b5c9);c.drawCircle(x-width*.15f,floor-height*.83f,width*.17f,p);}
            else {prism(c,x,floor,width*1.05f,height*.61f,index%3==0?0xff9f769c:0xffb09464);p.setColor(0xffe6c7a6);c.drawCircle(x-width*.2f,floor-height*.63f,width*.11f,p);c.drawCircle(x+width*.2f,floor-height*.63f,width*.11f,p);}return true;
        }
        if(world==1||world==12){
            prism(c,x,floor-height*.32f,width*1.55f,height*.23f,world==12?0xffad7390:0xffb78857);
            p.setColor(0xffdabd8a);p.setStrokeWidth(Math.max(1,width*.04f));c.drawLine(x,floor-height*.55f,x,floor-height*.95f,p);p.setColor(accent);c.drawCircle(x,floor-height*.95f,width*.25f,p);return true;
        }
        return false;
    }
    private void drawRoad(Canvas c,FlightSnapshot f,float w,float h,int accent){
        float top=-.35f,bottom=1.1f;
        path.reset();path.moveTo(x(w,-.5f,top,f),y(h,top));path.lineTo(x(w,2.5f,top,f),y(h,top));path.lineTo(x(w,2.5f,bottom,f),y(h,bottom));path.lineTo(x(w,-.5f,bottom,f),y(h,bottom));path.close();
        p.setShader(roadShade);c.drawPath(path,p);p.setShader(null);
        int edge=f.overdrive>0?0xffffcf62:accent;
        for(int lane=0;lane<4;lane++){
            float value=lane-.5f;p.setColor(lane==0||lane==3?edge:f.highContrast?0xff8096b5:0xff344c70);
            p.setStrokeWidth(lane==0||lane==3?w*.007f:w*.003f);c.drawLine(x(w,value,top,f),y(h,top),x(w,value,bottom,f),y(h,bottom),p);
        }
        float scroll=f.reducedMotion?0:(f.clock*f.speed*.42f)%.11f;
        for(int i=0;i<13;i++){
            float yy=-.32f+i*.11f+scroll,s=FlightPerspective.scale(yy);
            p.setColor(f.highContrast?0xff526887:0xff273e5b);p.setStrokeWidth(Math.max(1,w*.002f*s));
            c.drawLine(x(w,-.5f,yy,f),y(h,yy),x(w,2.5f,yy,f),y(h,yy),p);
        }
        // The selected route is a perspective ribbon, leaving collectible shapes readable.
        polygon(c,f.overdrive>0?0x18ffdb78:0x126cfac5,x(w,f.targetLane-.40f,top,f),y(h,top),x(w,f.targetLane+.40f,top,f),y(h,top),x(w,f.targetLane+.40f,bottom,f),y(h,bottom),x(w,f.targetLane-.40f,bottom,f),y(h,bottom));
        p.setColor(0xffd8f8ed);p.setStrokeWidth(w*.004f);
        for(int i=0;i<3;i++){float yy=.40f+i*.20f,xx=x(w,f.targetLane,yy,f),sy=y(h,yy),s=w*.014f*FlightPerspective.scale(yy);c.drawLine(xx-s,sy+s*.8f,xx,sy,p);c.drawLine(xx,sy,xx+s,sy+s*.8f,p);}
    }
    private void prism(Canvas c,float xx,float floor,float width,float height,int color){
        float ex=width*.23f,dy=height*.23f;
        p.setColor(0x52000000);rect.set(xx-width*.58f,floor-height*.04f,xx+width*.85f,floor+height*.15f);c.drawOval(rect,p);
        polygon(c,shade(color,.38f),xx-width*.5f,floor-height,xx+width*.5f,floor-height,xx+width*.5f,floor,xx-width*.5f,floor);
        polygon(c,shade(color,.19f),xx+width*.5f,floor-height,xx+width*.5f+ex,floor-height-dy,xx+width*.5f+ex,floor-dy,xx+width*.5f,floor);
        polygon(c,shade(color,.72f),xx-width*.5f,floor-height,xx-width*.5f+ex,floor-height-dy,xx+width*.5f+ex,floor-height-dy,xx+width*.5f,floor-height);
        p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,width*.035f));rect.set(xx-width*.5f,floor-height,xx+width*.5f,floor);c.drawRect(rect,p);p.setStyle(Paint.Style.FILL);
    }
    private void drawEntity(Canvas c,FlightSnapshot f,float w,float h,int at){
        float s=FlightPerspective.scale(f.y[at]),xx=x(w,f.lane[at],f.y[at],f),floor=y(h,f.y[at]),size=w*.075f*s;
        int type=f.type[at];
        if(type==0){
            float height=size*(f.pattern[at]==4?1.6f:1.2f);prism(c,xx,floor,size*2.0f,height,0xffff728f);
            p.setColor(0xffffe5ed);p.setStrokeWidth(Math.max(1,size*.08f));c.drawLine(xx-size*.30f,floor-height*.72f,xx+size*.30f,floor-height*.28f,p);c.drawLine(xx+size*.30f,floor-height*.72f,xx-size*.30f,floor-height*.28f,p);
        }else if(type==6){
            prism(c,xx,floor,size*2.05f,size*.65f,0xffffbb69);
            p.setColor(0xfffff5ca);p.setStrokeWidth(Math.max(1,size*.09f));c.drawLine(xx-size*.3f,floor-size*.24f,xx,floor-size*.48f,p);c.drawLine(xx,floor-size*.48f,xx+size*.3f,floor-size*.24f,p);
        }else if(type==7){
            float clearance=size*1.25f;prism(c,xx-size*.84f,floor,size*.24f,size*2.1f,0xffcf9eff);prism(c,xx+size*.84f,floor,size*.24f,size*2.1f,0xffcf9eff);
            prism(c,xx,floor-clearance,size*2.08f,size*.68f,0xffa78eff);
            p.setColor(0xfff3e3ff);p.setStrokeWidth(Math.max(1,size*.09f));c.drawLine(xx-size*.3f,floor-clearance-size*.48f,xx,floor-clearance-size*.24f,p);c.drawLine(xx,floor-clearance-size*.24f,xx+size*.3f,floor-clearance-size*.48f,p);
        }else if(type==1){
            float yy=floor-size*.50f,bob=f.reducedMotion?0:(float)Math.sin(f.clock*4+at)*size*.10f;yy+=bob;
            p.setColor(0x28000000);rect.set(xx-size*.7f,floor-size*.06f,xx+size*.7f,floor+size*.16f);c.drawOval(rect,p);
            polygon(c,0xff55bcac,xx,yy-size*.7f,xx+size*.57f,yy,xx,yy+size*.75f,xx-size*.52f,yy);
            polygon(c,0xffd3ffee,xx,yy-size*.7f,xx+size*.57f,yy,xx,yy,xx-size*.52f,yy);
            polygon(c,0xff98f4c9,xx,yy,xx+size*.57f,yy,xx,yy+size*.75f,xx,yy+size*.3f);
        }else if(type==8){
            p.setColor(0xffffd773);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,size*.18f));rect.set(xx-size,floor-size*2.1f,xx+size,floor-size*.1f);c.drawOval(rect,p);p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(size*1.05f);c.drawText("»",xx,floor-size*.7f,p);
        }else if(type>=2&&type<=5){
            int color=POWERS[type-2];prism(c,xx,floor,size*1.25f,size*1.15f,color);
            p.setColor(0xfff9fffd);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(size*.95f);c.drawText(Catalog.POWER_ICONS[type-2],xx,floor-size*.27f,p);
        }
    }
    private void drawPilot(Canvas c,FlightSnapshot f,float w,float h){
        if(!f.alive)return;
        float s=FlightPerspective.scale(GameConfig.PLAYER_Y),xx=x(w,f.playerLane,GameConfig.PLAYER_Y,f),floor=y(h,GameConfig.PLAYER_Y),radius=w*.072f*s;
        float jump=f.jumpHeight*h*.11f,hover=h*.018f,py=floor-hover-jump;
        p.setColor(0x90000000);rect.set(xx-radius*.85f,floor-radius*.28f,xx+radius*.85f,floor+radius*.15f);c.drawOval(rect,p);
        if(!f.reducedMotion)drawTrail(c,f,xx,py,radius);
        if(f.overdrive>0||f.powers[0]>0){p.setColor(f.overdrive>0?0xffffd778:0xff7beaff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,w*.004f));rect.set(xx-radius*1.4f,py-radius*1.8f,xx+radius*1.4f,py+radius*.9f);c.drawOval(rect,p);p.setStyle(Paint.Style.FILL);}
        c.save();c.rotate(f.reducedMotion?0:(f.targetLane-f.playerLane)*-13,xx,py);
        if(f.duckTime>0){c.translate(xx,py+radius*.3f);c.scale(1.16f,.52f);c.translate(-xx,-py);}
        Art.ship(c,p,path,xx,py,radius,f.skin,f.clock,!f.reducedMotion);c.restore();
        p.setAlpha(255);p.setStyle(Paint.Style.FILL);
    }
    private void drawTrail(Canvas c,FlightSnapshot f,float x,float y,float radius){
        for(int i=12;i>0;i--){
            float yy=y+radius*(.80f+i*.24f),xx=x+(float)Math.sin(f.clock*6-i*.40f)*radius*.16f,rr=radius*.15f*(1-i*.045f);
            p.setColor(f.overdrive>0?0xffffd779:f.trail==4?RAINBOW[i%6]:Catalog.COLORS[f.skin]);p.setAlpha(Math.max(10,140-i*10));
            if(f.trail==2||f.trail==9){float shift=f.trail==9?(float)Math.sin(f.clock*6-i*.6f)*radius*.5f:radius*.32f;c.drawCircle(xx-shift,yy,rr,p);c.drawCircle(xx+shift,yy,rr,p);}
            else if(f.trail==6||f.trail==11){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,radius*.045f));rect.set(xx-rr*(f.trail==11?3:1.4f),yy-rr,xx+rr*(f.trail==11?3:1.4f),yy+rr);c.drawOval(rect,p);p.setStyle(Paint.Style.FILL);}
            else if(f.trail==7){c.drawRect(xx-rr,yy-rr,xx+rr,yy+rr,p);}
            else if(f.trail==8){path.reset();path.moveTo(xx,yy+rr);path.cubicTo(xx-rr*2,yy-rr*.1f,xx-rr*.7f,yy-rr*1.5f,xx,yy-rr*.55f);path.cubicTo(xx+rr*.7f,yy-rr*1.5f,xx+rr*2,yy-rr*.1f,xx,yy+rr);path.close();c.drawPath(path,p);}
            else if(f.trail==5){path.reset();path.moveTo(xx,yy-rr);path.lineTo(xx+rr,yy);path.lineTo(xx,yy+rr);path.lineTo(xx-rr,yy);path.close();c.drawPath(path,p);}
            else {if(f.trail==3||f.trail==10)xx+=(float)Math.sin(i*4+f.clock*2)*radius*.55f;c.drawCircle(xx,yy,rr*(f.trail==1?1.5f:1),p);}
        }p.setAlpha(255);
    }
    private void drawGuardian(Canvas c,FlightSnapshot f,float w,float h,int accent){
        float yy=h*.17f,xx=w*.5f,rr=w*.12f;
        if(!f.reducedMotion)xx+=(float)Math.sin(f.clock*1.4f)*w*.08f;
        int color=f.bossWarning>0?0xffffaf65:0xffdb94ff;
        prism(c,xx,yy+rr*.35f,rr*1.6f,rr*.7f,color);
        polygon(c,shade(accent,.65f),xx-rr*.65f,yy,xx-rr*1.5f,yy-rr*.3f,xx-rr*1.4f,yy+rr*.15f,xx-rr*.6f,yy+rr*.18f);
        polygon(c,shade(accent,.65f),xx+rr*.65f,yy,xx+rr*1.5f,yy-rr*.3f,xx+rr*1.4f,yy+rr*.15f,xx+rr*.6f,yy+rr*.18f);
        p.setColor(0xfffff2fe);c.drawCircle(xx,yy-rr*.03f,rr*.20f,p);p.setColor(0xff552459);c.drawCircle(xx,yy-rr*.03f,rr*.10f,p);
        p.setColor(0xff30253c);rect.set(w*.30f,h*.065f,w*.70f,h*.078f);c.drawRoundRect(rect,4,4,p);
        p.setColor(color);rect.right=rect.left+(w*.40f*Math.max(0,Math.min(1,f.bossHealth)));c.drawRoundRect(rect,4,4,p);
    }
    private void drawSpeed(Canvas c,FlightSnapshot f,float w,float h){
        p.setColor(0x88ffe3a0);p.setStrokeWidth(w*.0035f);
        for(int i=0;i<16;i++){float phase=(f.clock*(1.2f+i*.015f)+i*.137f)%1,yy=h*(.18f+phase*.78f);int side=i%2;float xx=side==0?w*(.02f+.11f*(1-phase)):w*(.98f-.11f*(1-phase));c.drawLine(xx,yy,xx+(side==0?-1:1)*w*.035f,yy+h*(.03f+phase*.10f),p);}
    }
}
