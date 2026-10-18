package com.toosarax.jetjo.game;

import com.toosarax.jetjo.core.GameConfig;
import org.junit.Test;
import static org.junit.Assert.*;

public final class FlightPerspectiveTest {
    @Test public void nearItemsGrowAndMoveTowardThePlayer(){
        float previous=-1,scale=0;
        for(int i=0;i<160;i++){float y=-.35f+i*.009f;float position=FlightPerspective.screenY(960,y),size=FlightPerspective.scale(y);assertTrue(position>=previous);assertTrue(size>=scale);previous=position;scale=size;}
    }
    @Test public void collisionPlaneKeepsThePilotInLowerQuarter(){
        float position=FlightPerspective.screenY(1000,GameConfig.PLAYER_Y);assertTrue(position>720&&position<820);
        float upper=FlightPerspective.screenY(1000,GameConfig.PLAYER_Y-.045f),lower=FlightPerspective.screenY(1000,GameConfig.PLAYER_Y+.045f);assertTrue(upper<position&&lower>position);
    }
    @Test public void lanesAreOrderedAndNeverOverlapAtHitPlane(){
        for(int i=0;i<100;i++){float time=i*.2f;float a=FlightPerspective.screenX(480,0,.82f,time,false),b=FlightPerspective.screenX(480,1,.82f,time,false),c=FlightPerspective.screenX(480,2,.82f,time,false);assertTrue(a<b&&b<c);assertTrue(b-a>100);assertEquals(b-a,c-b,.001f);}
    }
    @Test public void reducedMotionRemovesRoadBend(){assertEquals(FlightPerspective.screenX(540,1,.1f,1,true),FlightPerspective.screenX(540,1,.1f,20,true),0);}
    @Test public void touchesChooseVisibleLaneCentersAndMidpoints(){for(float width:new float[]{180,320,540})for(float time:new float[]{0,2,14}){float left=FlightPerspective.screenX(width,0,GameConfig.PLAYER_Y,time,false),middle=FlightPerspective.screenX(width,1,GameConfig.PLAYER_Y,time,false),right=FlightPerspective.screenX(width,2,GameConfig.PLAYER_Y,time,false);assertEquals(0,FlightPerspective.laneForScreenX(width,left,time,false));assertEquals(1,FlightPerspective.laneForScreenX(width,middle,time,false));assertEquals(2,FlightPerspective.laneForScreenX(width,right,time,false));assertEquals(0,FlightPerspective.laneForScreenX(width,(left+middle)/2-1,time,false));assertEquals(1,FlightPerspective.laneForScreenX(width,(left+middle)/2+1,time,false));assertEquals(1,FlightPerspective.laneForScreenX(width,(middle+right)/2-1,time,false));assertEquals(2,FlightPerspective.laneForScreenX(width,(middle+right)/2+1,time,false));}}
    @Test public void invalidDepthRemainsFiniteAndBounded(){for(float value:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1000,1000}){assertTrue(Float.isFinite(FlightPerspective.depth(value)));assertTrue(FlightPerspective.scale(value)>=.14f);assertTrue(FlightPerspective.depth(value)<1.3f);}}
}
