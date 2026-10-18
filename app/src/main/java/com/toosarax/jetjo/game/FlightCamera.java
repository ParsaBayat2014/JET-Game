package com.toosarax.jetjo.game;

/** Android-free camera contract used by both perspective rendering and tap hit testing. */
public final class FlightCamera {
    private FlightCamera() {}
    public static final float FOV_DEGREES = 58, CAMERA_HEIGHT = 6.2f, CAMERA_Z = 10;
    public static final float AIM_HEIGHT = .7f, AIM_Z = -18, AIM_X_FACTOR = .32f;
    public static final float LANE_SPACING = 2.3f, PLAYER_HEIGHT = .82f;
    private static final float TAN_HALF_FOV = (float)Math.tan(Math.toRadians(FOV_DEGREES * .5));
    /** Narrow split-screen viewports must still contain the three collision lanes. */
    public static float verticalFov(float aspect) {
        aspect = Float.isFinite(aspect) ? Math.max(.25f, aspect) : 1;
        return (float)Math.toDegrees(2 * Math.atan(TAN_HALF_FOV * Math.max(1, .56f / aspect)));
    }
    public static float cameraOffset(float playerLane, boolean reducedMotion) {
        if (reducedMotion || !Float.isFinite(playerLane)) return 0;
        return (Math.max(0, Math.min(2, playerLane)) - 1) * .36f;
    }
    /** Horizontal projection of a lane at the player's collision plane z=0, in viewport fractions. */
    public static float laneScreenX(int lane, float aspect, float cameraOffset) {
        lane = Math.max(0, Math.min(2, lane)); aspect = Float.isFinite(aspect) ? Math.max(.25f, aspect) : 1;
        cameraOffset = Float.isFinite(cameraOffset) ? Math.max(-.36f, Math.min(.36f, cameraOffset)) : 0;
        float fx = cameraOffset * (AIM_X_FACTOR - 1), fy = AIM_HEIGHT - CAMERA_HEIGHT, fz = AIM_Z - CAMERA_Z;
        float length = (float)Math.sqrt(fx * fx + fy * fy + fz * fz); fx /= length; fy /= length; fz /= length;
        // right = normalize(cross(forward, worldUp)); projection uses OpenGL's right-handed view.
        float rightLength = (float)Math.sqrt(fx * fx + fz * fz), rx = -fz / rightLength, rz = fx / rightLength;
        float dx = (lane - 1) * LANE_SPACING - cameraOffset, dy = PLAYER_HEIGHT - CAMERA_HEIGHT, dz = -CAMERA_Z;
        float depth = dx * fx + dy * fy + dz * fz;
        float tan = (float)Math.tan(Math.toRadians(verticalFov(aspect) * .5f));
        return .5f + (dx * rx + dz * rz) / (2 * depth * tan * aspect);
    }
    public static int laneForScreenX(float pixelX, int width, int height, float cameraOffset) {
        if (!Float.isFinite(pixelX) || width <= 0 || height <= 0) return 1;
        float fraction = Math.max(0, Math.min(1, pixelX / width)), aspect = width / (float)height;
        int nearest = 0; float distance = Float.MAX_VALUE;
        for (int lane = 0; lane < 3; lane++) {
            float candidate = Math.abs(fraction - laneScreenX(lane, aspect, cameraOffset));
            if (candidate < distance) { distance = candidate; nearest = lane; }
        }
        return nearest;
    }
}
