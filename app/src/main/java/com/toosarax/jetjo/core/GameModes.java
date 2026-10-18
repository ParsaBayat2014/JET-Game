package com.toosarax.jetjo.core;

/** Stable persisted mode ids. Never reorder them when adding a mode. */
public final class GameModes {
    private GameModes() {}
    public static final int SURVIVAL = 0, SPRINT = 1, DAILY = 2;
    public static final int ZEN = 3, CAMPAIGN = 4, COIN_RUSH = 5;
    public static final int STUNT = 6, COUNT = 7;
    public static boolean valid(int mode) { return mode >= 0 && mode < COUNT; }
    public static boolean hasEconomyRewards(int mode) { return valid(mode) && mode != ZEN; }
    public static String name(int mode, boolean fa) {
        switch (mode) {
            case STUNT: return fa ? "پرواز اکشن" : "Action Flight";
            case SPRINT: return fa ? "سرعتی ۹۰" : "90 Sprint";
            case DAILY: return fa ? "مسیر روز" : "Daily Circuit";
            case ZEN: return fa ? "پرواز آرام" : "Zen Flight";
            case CAMPAIGN: return fa ? "سفر کهکشانی" : "Galaxy Adventure";
            case COIN_RUSH: return fa ? "شکار گنج" : "Treasure Rush";
            default: return fa ? "بقا" : "Survival";
        }
    }
}
