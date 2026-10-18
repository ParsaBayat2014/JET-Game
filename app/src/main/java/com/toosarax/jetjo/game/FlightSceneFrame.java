package com.toosarax.jetjo.game;

/** One coherent detached GL publication; a pair cannot mix pilots from different UI frames. */
public final class FlightSceneFrame {
    public final FlightSnapshot left, right;
    public final float leftFraction;
    private FlightSceneFrame(FlightSnapshot left, FlightSnapshot right, float fraction) {
        if (left == null) throw new IllegalArgumentException("Left flight frame required");
        this.left = left; this.right = right;
        leftFraction = right == null ? 1 : Float.isFinite(fraction) ? Math.max(.25f, Math.min(.75f, fraction)) : .5f;
    }
    public static FlightSceneFrame single(FlightSnapshot frame) { return new FlightSceneFrame(frame, null, 1); }
    public static FlightSceneFrame pair(FlightSnapshot left, FlightSnapshot right, float fraction) {
        if (right == null) throw new IllegalArgumentException("Right flight frame required"); return new FlightSceneFrame(left, right, fraction);
    }
    public boolean paired() { return right != null; }
    public int leftWidth(int width) { return !paired() || width < 2 ? Math.max(0, width) : Math.max(1, Math.min(width - 1, Math.round(width * leftFraction))); }
    public int rightWidth(int width) { return Math.max(0, width - leftWidth(width)); }
}
