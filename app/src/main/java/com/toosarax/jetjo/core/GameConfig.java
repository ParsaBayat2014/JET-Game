package com.toosarax.jetjo.core;

/** All gameplay tuning uses seconds and normalized lane coordinates. */
public final class GameConfig {
    private GameConfig() {}
    public static final float STEP = 1f / 60f;
    public static final float PLAYER_Y = .82f;
    public static final float START_SPEED = .24f, MAX_SPEED = .58f;
    public static final float COMBO_WINDOW = 3.2f;
    public static final float[] POWER_SECONDS = {8, 9, 7, 10};
    public static final int MAX_ENTITIES = 96, MAX_PARTICLES = 160;
    public static final float SPRINT_SECONDS = 90, DAILY_SECONDS = 120;
    public static final float ZEN_SECONDS = 180, COIN_RUSH_SECONDS = 75;
    public static final float STUNT_SECONDS = 100, JUMP_SECONDS = .9f, DUCK_SECONDS = .9f;
    public static final float ACTION_COOLDOWN_SECONDS = 1.04f, BOOST_COST = .5f;
    public static final float OVERDRIVE_SECONDS = 4f, HIT_GRACE_SECONDS = 1.15f;
    public static final float NEAR_DISTANCE = 1.03f;
}
