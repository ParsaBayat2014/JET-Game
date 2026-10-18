package com.toosarax.jetjo.core;

/** Two isolated seeded runs. Shared-device input and pause never alter economy or progression. */
public final class DuelEngine {
    public static final int TEAM_TARGET = 80, TEAM_GUARDIAN_TARGET = 4;
    public final GameEngine[] players = new GameEngine[2];
    public final boolean cooperative, actionFlight;
    private boolean paused;
    public DuelEngine(long seed, boolean cooperative, boolean gentle) {
        this(seed, cooperative, gentle, false);
    }
    public DuelEngine(long seed, boolean cooperative, boolean gentle, boolean actionFlight) {
        this.cooperative = cooperative; this.actionFlight = actionFlight;
        for (int i=0;i<2;i++) {
            players[i] = new GameEngine(seed);
            players[i].mode = actionFlight ? GameModes.STUNT : GameModes.COIN_RUSH;
            players[i].partyMode = actionFlight;
            players[i].relaxed = gentle;
            players[i].start(false);
        }
    }
    public void move(int player, int lane) {
        if (player<0 || player>=2 || paused || finished()) return;
        players[player].moveTo(lane);
    }
    private boolean acceptsAction(int player) { return actionFlight && player>=0 && player<players.length && !paused && !finished(); }
    public boolean jump(int player) { return acceptsAction(player) && players[player].jump(); }
    public boolean duck(int player) { return acceptsAction(player) && players[player].duck(); }
    public boolean boost(int player) { return acceptsAction(player) && players[player].boost(); }
    public void update(float step) {
        if (paused || finished()) return;
        for (GameEngine player : players) player.update(step);
    }
    public void setPaused(boolean value) { paused=value; }
    public boolean isPaused() { return paused; }
    public boolean finished() { return !players[0].alive && !players[1].alive; }
    public int combinedEnergy() { return players[0].energy + players[1].energy; }
    public int combinedBosses() { return players[0].bossesDefeated + players[1].bossesDefeated; }
    public boolean teamWon() { return finished() && (actionFlight ? combinedBosses()>=TEAM_GUARDIAN_TARGET : combinedEnergy()>=TEAM_TARGET); }
    /** -2 unfinished, -1 tied, or the winning player's index. */
    public int winner() {
        if (!finished()) return -2;
        long left=players[0].score(), right=players[1].score();
        return left==right ? -1 : left>right ? 0 : 1;
    }
    public float remainingSeconds() {
        return Math.max(0,(actionFlight ? GameConfig.STUNT_SECONDS : GameConfig.COIN_RUSH_SECONDS)-players[0].seconds);
    }
}
