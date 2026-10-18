package com.toosarax.jetjo.game;

import com.toosarax.jetjo.core.GameEngine;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlightSceneFrameTest {
    private FlightSnapshot frame(long seed,int skin) {
        GameEngine engine=new GameEngine(seed);engine.start(false);
        return FlightSnapshot.capture(engine,skin,0,0,false,false);
    }
    @Test public void publicationKeepsBothDetachedPilotsTogether() {
        GameEngine leftEngine=new GameEngine(7),rightEngine=new GameEngine(7);leftEngine.start(false);rightEngine.start(false);
        leftEngine.moveTo(0);rightEngine.moveTo(2);
        FlightSnapshot left=FlightSnapshot.capture(leftEngine,3,0,1,false,false),right=FlightSnapshot.capture(rightEngine,18,0,1,false,false);
        FlightSceneFrame pair=FlightSceneFrame.pair(left,right,.62f);
        leftEngine.moveTo(2);rightEngine.moveTo(0);
        assertTrue(pair.paired());assertSame(left,pair.left);assertSame(right,pair.right);
        assertEquals(0,pair.left.targetLane);assertEquals(2,pair.right.targetLane);
        assertEquals(3,pair.left.skin);assertEquals(18,pair.right.skin);
    }
    @Test public void oddAndSmallSurfacesHaveNoGapOrOverlap() {
        for(float fraction:new float[]{-.1f,.25f,.5f,.62f,.75f,2,Float.NaN,Float.POSITIVE_INFINITY}) {
            FlightSceneFrame pair=FlightSceneFrame.pair(frame(1,0),frame(2,1),fraction);
            for(int width:new int[]{0,1,2,3,319,320,321,601,1081}) {
                int left=pair.leftWidth(width),right=pair.rightWidth(width);
                assertEquals(width,left+right);assertTrue(left>=0&&right>=0);
                if(width>=2){assertTrue(left>0);assertTrue(right>0);}
                else assertEquals(0,right);
            }
        }
    }
    @Test public void narrowPairedViewportsContainAndSelectEveryCollisionLane() {
        for(float fraction:new float[]{.25f,.5f,.62f,.75f}) {
            FlightSceneFrame pair=FlightSceneFrame.pair(frame(1,0),frame(2,1),fraction);
            int width=601,height=920;
            for(int pilot=0;pilot<2;pilot++) {
                int viewportWidth=pilot==0?pair.leftWidth(width):pair.rightWidth(width);
                for(float camera:new float[]{-.36f,0,.36f})for(int lane=0;lane<3;lane++) {
                    float normalized=FlightCamera.laneScreenX(lane,viewportWidth/(float)height,camera);
                    assertTrue(normalized>0&&normalized<1);
                    assertEquals(lane,FlightCamera.laneForScreenX(normalized*viewportWidth,viewportWidth,height,camera));
                }
            }
        }
    }
    @Test public void singleViewportKeepsFullWidthAndInvalidFractionsAreBounded() {
        FlightSnapshot frame=frame(9,0);FlightSceneFrame single=FlightSceneFrame.single(frame);
        assertFalse(single.paired());assertSame(frame,single.left);assertNull(single.right);assertEquals(1,single.leftFraction,0);
        assertEquals(601,single.leftWidth(601));assertEquals(0,single.rightWidth(601));
        assertEquals(.25f,FlightSceneFrame.pair(frame,frame,-2).leftFraction,0);
        assertEquals(.75f,FlightSceneFrame.pair(frame,frame,2).leftFraction,0);
        assertEquals(.5f,FlightSceneFrame.pair(frame,frame,Float.NaN).leftFraction,0);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsMissingRightPilot(){FlightSceneFrame.pair(frame(1,0),null,.5f);}
    @Test(expected=IllegalArgumentException.class) public void rejectsMissingLeftPilot(){FlightSceneFrame.pair(null,frame(1,0),.5f);}
}
