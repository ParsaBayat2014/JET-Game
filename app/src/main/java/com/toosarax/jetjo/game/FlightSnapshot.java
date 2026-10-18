package com.toosarax.jetjo.game;

import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.multiplayer.LanProtocol;

/** A complete detached render frame. Arrays are privately created and read-only for render consumers. */
public final class FlightSnapshot {
    public final int count, skin, trail, world, targetLane, combo, bossPhase;
    public final int[] type, lane, style, pattern;
    public final float[] y, powers;
    public final float playerLane, speed, clock, seconds, overdrive, charge, jumpHeight, duckTime, boostFuel, bossHealth, bossWarning;
    public final boolean alive, reducedMotion, highContrast, bossActive;

    private FlightSnapshot(int skin, int trail, int world, float clock, boolean reducedMotion, boolean highContrast,
                           float playerLane, int targetLane, float speed, float seconds, boolean alive,
                           float overdrive, float charge, int combo, float[] powers,
                           float jumpHeight, float duckTime, float boostFuel,
                           boolean bossActive, int bossPhase, float bossHealth, float bossWarning,
                           int[] type, int[] lane, int[] style, int[] pattern, float[] y) {
        this.skin = skin; this.trail = trail; this.world = world; this.clock = clock; this.reducedMotion = reducedMotion; this.highContrast = highContrast;
        this.playerLane = playerLane; this.targetLane = targetLane; this.speed = speed; this.seconds = seconds; this.alive = alive;
        this.overdrive = overdrive; this.charge = charge; this.combo = combo; this.powers = powers;
        this.jumpHeight = jumpHeight; this.duckTime = duckTime; this.boostFuel = boostFuel;
        this.bossActive = bossActive; this.bossPhase = bossPhase; this.bossHealth = bossHealth; this.bossWarning = bossWarning;
        this.type = type; this.lane = lane; this.style = style; this.pattern = pattern; this.y = y; count = y.length;
    }
    public static FlightSnapshot capture(GameEngine engine, int skin, int trail, float clock, boolean reducedMotion, boolean highContrast) {
        return capture(engine, skin, trail, clock, reducedMotion, highContrast, 0);
    }
    /** Call synchronously from the thread that owns GameEngine, then publish the returned frame. */
    public static FlightSnapshot capture(GameEngine engine, int skin, int trail, float clock, boolean reducedMotion, boolean highContrast, int world) {
        int count = 0; for (GameEngine.Entity entity : engine.entities) if (entity.active) count++;
        count = Math.min(count, GameConfig.MAX_ENTITIES);
        int[] type = new int[count], lane = new int[count], style = new int[count], pattern = new int[count]; float[] y = new float[count]; int i = 0;
        for (GameEngine.Entity entity : engine.entities) if (entity.active && i < count) {
            type[i] = entity.type; lane[i] = entity.lane; style[i] = entity.style; pattern[i] = entity.pattern; y[i] = entity.y; i++;
        }
        return new FlightSnapshot(skin, trail, world, clock, reducedMotion, highContrast, engine.playerLane, engine.targetLane, engine.speed, engine.seconds, engine.alive,
                engine.overdrive, engine.charge, engine.combo, engine.power.clone(), engine.jumpHeight(), engine.duckTime, engine.boostFuel,
                engine.bossActive, engine.bossPhase, engine.bossHealth, engine.bossWarning, type, lane, style, pattern, y);
    }
    public static FlightSnapshot fromPilot(LanProtocol.Pilot pilot, float clock, int world, boolean reducedMotion, boolean highContrast) {
        int count = Math.min(pilot.items.length, GameConfig.MAX_ENTITIES); int[] type = new int[count], lane = new int[count], style = new int[count], pattern = new int[count]; float[] y = new float[count];
        for (int i = 0; i < count; i++) { LanProtocol.Item item = pilot.items[i]; type[i] = item.type; lane[i] = item.lane; style[i] = item.style; y[i] = item.y; }
        return new FlightSnapshot(pilot.skin, 0, world, clock, reducedMotion, highContrast, pilot.lane, Math.round(pilot.lane), Math.min(.58f, .24f + clock * .0024f) * .8f, clock, true,
                pilot.overdrive, pilot.charge, pilot.combo, pilot.powers.clone(), 0, 0, 1, false, 0, 0, 0, type, lane, style, pattern, y);
    }
}
