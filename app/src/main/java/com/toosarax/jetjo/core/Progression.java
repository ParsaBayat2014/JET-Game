package com.toosarax.jetjo.core;

public final class Progression {
    private Progression() {}
    public static long threshold(int level) {long n=Math.max(0,level-1); return n*180+n*n*40;}
    public static int level(long xp) {int l=1; while(l<100 && xp>=threshold(l+1)) l++; return l;}
    public static int rewardXp(long score,int energy,float seconds) {return Math.max(5,(int)(score/20+energy*2+seconds));}
    /** Original earned and purchased indices remain stable when migrating saved selections. */
    public static final int[] SKIN_LEVELS={1,1,2,3,4,5,7,9,12,15,0,0,0,0,0,0,0,0,3,4,5,6,7,8,9,10,12,14,16,18,20,23,26,30,34,38};
    public static final int[] SKIN_COSTS={0,0,80,140,220,320,460,620,800,1000,0,0,0,0,0,0,0,0,150,190,240,290,350,410,480,560,650,760,880,1010,1150,1330,1530,1760,1990,2200};
    public static boolean isPaidSkin(int index) {return index>=10&&index<18;}
    public static int skinLevel(int index) {return index>=0&&index<SKIN_LEVELS.length?SKIN_LEVELS[index]:Integer.MAX_VALUE;}
    public static int skinCost(int index) {return index>=0&&index<SKIN_COSTS.length?SKIN_COSTS[index]:Integer.MAX_VALUE;}
    public static int zoneCount(int level) {return level>=7?3:level>=3?2:1;}
}
