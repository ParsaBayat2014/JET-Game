package com.toosarax.jetjo.game;

/** Monotonic lane/depth projection shared by drawing and visual hit-plane alignment. */
public final class FlightPerspective {
    private FlightPerspective() {}
    public static float depth(float worldY){
        if(!Float.isFinite(worldY))worldY=-.35f;
        float t=Math.max(0,Math.min(1.13f,(worldY+.35f)/1.35f));return t*t;
    }
    public static int laneForScreenX(float width,float pixelX,float time,boolean reduced){
        if(width<=0||!Float.isFinite(pixelX))return 1;int nearest=1;float distance=Float.MAX_VALUE;
        for(int lane=0;lane<3;lane++){float delta=Math.abs(pixelX-screenX(width,lane,com.toosarax.jetjo.core.GameConfig.PLAYER_Y,time,reduced));if(delta<distance){distance=delta;nearest=lane;}}return nearest;
    }
    public static float screenY(float height,float worldY){return height*(.10f+.88f*depth(worldY));}
    public static float scale(float worldY){return .14f+.86f*depth(worldY);}
    public static float screenX(float width,float lane,float worldY,float time,boolean reduced){
        float depth=depth(worldY),curve=reduced?0:(float)Math.sin(time*.38f)*width*.035f*(1-Math.min(1,depth));
        return width*.5f+curve+(lane-1)*width*.30f*scale(worldY);
    }
}
