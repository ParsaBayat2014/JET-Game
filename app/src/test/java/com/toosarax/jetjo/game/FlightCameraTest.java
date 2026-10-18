package com.toosarax.jetjo.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlightCameraTest {
    @Test public void tapsOnVisibleLanesSelectTheLaneAcrossPortraitAndShortViewports() {
        for (float aspect : new float[]{.25f,.28f,.4f,.75f, 1, 1.5f, 2}) for (float camera : new float[]{-.36f, 0, .36f}) {
            int width = 600, height = Math.round(width / aspect);
            for (int lane = 0; lane < 3; lane++) {
                float projected = FlightCamera.laneScreenX(lane, width / (float)height, camera);
                assertTrue(projected > 0 && projected < 1);
                assertEquals(lane, FlightCamera.laneForScreenX(projected * width, width, height, camera));
            }
        }
    }
    @Test public void midpointBoundariesTrackTheRenderedLaneSpacing() {
        for (float aspect : new float[]{.25f,.28f,.4f,.75f, 1, 1.5f, 2}) for (float camera : new float[]{-.36f, 0, .36f}) {
            int width = 600, height = Math.round(width / aspect); float actualAspect = width / (float)height;
            for (int lane = 0; lane < 2; lane++) {
                float a = FlightCamera.laneScreenX(lane, actualAspect, camera), b = FlightCamera.laneScreenX(lane + 1, actualAspect, camera); assertTrue(a < b);
                float boundary = (a + b) * .5f * width;
                assertEquals(lane, FlightCamera.laneForScreenX(boundary - 1, width, height, camera));
                assertEquals(lane + 1, FlightCamera.laneForScreenX(boundary + 1, width, height, camera));
            }
        }
    }
    @Test public void projectionMatchesIndependentKnownCameraCoordinates() {
        assertEquals(.5f, FlightCamera.laneScreenX(1, 1, 0), .00001f);
        assertEquals(.33117f, FlightCamera.laneScreenX(0, 1, -.36f), .0001f);
        assertEquals(.66883f, FlightCamera.laneScreenX(2, 1, .36f), .0001f);
        assertEquals(.38745f, FlightCamera.laneScreenX(0, 1.5f, -.36f), .0001f);
        assertEquals(.61255f, FlightCamera.laneScreenX(2, 1.5f, .36f), .0001f);
        // The old screen-thirds mapping incorrectly selected the middle at this visible left lane.
        assertEquals(0, FlightCamera.laneForScreenX(.38745f * 600, 600, 400, -.36f));
    }
    @Test public void reducedMotionStopsCameraFollowAndInvalidLayoutReturnsMiddle() {
        assertEquals(0, FlightCamera.cameraOffset(2, true), 0); assertEquals(.36f, FlightCamera.cameraOffset(2, false), 0);
        assertEquals(-.36f, FlightCamera.cameraOffset(0, false), 0); assertEquals(0, FlightCamera.cameraOffset(Float.NaN, false), 0);
        assertEquals(1, FlightCamera.laneForScreenX(Float.NaN, 600, 400, 0)); assertEquals(1, FlightCamera.laneForScreenX(100, 0, 400, 0));
        assertEquals(0, FlightCamera.laneForScreenX(-100, 600, 400, 0)); assertEquals(2, FlightCamera.laneForScreenX(800, 600, 400, 0));
    }
    @Test public void splitPortraitFovPreservesHorizontalLaneVisibility() {
        assertEquals(58,FlightCamera.verticalFov(.56f),.0001f);
        assertEquals(58,FlightCamera.verticalFov(2),.0001f);
        assertTrue(FlightCamera.verticalFov(.28f)>FlightCamera.verticalFov(.4f));
        assertTrue(FlightCamera.verticalFov(.4f)>58);
        assertTrue(FlightCamera.verticalFov(.25f)<110);
        assertEquals(FlightCamera.laneScreenX(0,.56f,0),FlightCamera.laneScreenX(0,.28f,0),.0001f);
        assertEquals(FlightCamera.laneScreenX(2,.56f,0),FlightCamera.laneScreenX(2,.25f,0),.0001f);
    }
}
