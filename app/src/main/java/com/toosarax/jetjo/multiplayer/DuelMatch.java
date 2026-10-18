package com.toosarax.jetjo.multiplayer;

import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.core.GameModes;

/** Both tracks run on the host. Cosmetic, local rivalry; no profile or purchase rewards. */
public final class DuelMatch {
    private final GameEngine[] engines = new GameEngine[2];
    private final int world;
    private final int[] skins;
    private int tick;
    private float elapsed;
    private boolean paused;
    public DuelMatch(long seed, int world) {
        this(seed, world, 0, 1);
    }
    public DuelMatch(long seed, int world, int hostSkin, int guestSkin) {
        this.world = world;
        skins = new int[]{LanProtocol.freeSkin(hostSkin) ? hostSkin : 0, LanProtocol.freeSkin(guestSkin) ? guestSkin : 1};
        for (int i = 0; i < engines.length; i++) {
            engines[i] = new GameEngine(seed); engines[i].mode = GameModes.COIN_RUSH; engines[i].relaxed = true; engines[i].start(false);
        }
    }
    public boolean finished() { return elapsed >= LanProtocol.MATCH_SECONDS; }
    public void step(int hostLane, int guestLane, boolean pause) {
        paused = pause;
        if (pause || finished()) return;
        for (int i = 0; i < engines.length; i++) {
            GameEngine engine = engines[i]; engine.moveTo(i == 0 ? hostLane : guestLane); engine.update(GameConfig.STEP);
            // Coin Rush keeps both pilots flying: a crash costs points and breaks the combo.
        }
        tick++; elapsed = Math.min(LanProtocol.MATCH_SECONDS, engines[0].seconds);
    }
    public LanProtocol.Snapshot snapshot() { return LanProtocol.capture(tick, world, elapsed, paused, finished(), engines, skins); }
}
