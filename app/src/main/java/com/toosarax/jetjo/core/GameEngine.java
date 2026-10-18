package com.toosarax.jetjo.core;

import java.util.Random;

/** Seeded Android-free fixed-step simulation with bounded, reusable entity storage. */
public final class GameEngine {
    public static final int WALL = 0, ENERGY = 1, SHIELD = 2, MAGNET = 3, SLOW = 4, DOUBLE = 5;
    public static final int JUMP_BARRIER = 6, DUCK_BEAM = 7, BOOST_RING = 8, MAX_TYPE = BOOST_RING;
    public static final int COLLECT = 1, COMBO = 2, POWER = 3, HIT = 4, DEATH = 5, NEAR = 6, OVERDRIVE = 7;
    public static final int COMPLETE = 8, WAVE = 9, SMASH = 10;
    public static final int ACTION = 11, STUNT = 12, BOSS = 13, BOSS_BREAK = 14;
    public static final int CRUISE = 0, SLALOM = 1, GATES = 2, BRAID = 3, METEORS = 4, GUARDIAN = 5;
    public interface Listener { void event(int event, int detail, float lane, float y); }
    public static final class Entity {
        public boolean active;
        public int type, lane, style, pattern, rowId;
        public boolean nearChecked;
        public float y, closestApproach;
    }
    public final Entity[] entities = new Entity[GameConfig.MAX_ENTITIES];
    public final float[] power = new float[4];
    public Listener listener;
    public boolean alive, tutorial, won, relaxed;
    /** Shared-device action races use score penalties instead of removing either pilot. */
    public boolean partyMode;
    public int mode, stageIndex, nearMisses, hits, lives, overdriveActivations, wallsBroken;
    public int waveIndex, safeLane = 1, pattern;
    public float overdrive, charge, waveWarning;
    public float jumpTime, duckTime, boostFuel, bossHealth, bossWarning;
    public int stunts, bossHits, bossPhase, bossesDefeated;
    public boolean bossActive;
    public float seconds, speed, playerLane = 1, comboTime, invulnerable;
    public int targetLane = 1, combo, bestCombo, energy, passed, powers, moves;
    public double points;
    private float spawnTimer, powerTimer;
    private float actionCooldown, bossEnd;
    private int nextBossPhase;
    private int lastGap = 1, slalomDirection = 1;
    private final long initialSeed;
    private final Random random;
    private long runSequence;

    public GameEngine(long seed) {
        initialSeed = seed; random = new Random(seed);
        for (int i = 0; i < entities.length; i++) entities[i] = new Entity();
    }
    public void start(boolean training) {
        runSequence++;
        mode = GameModes.valid(mode) ? mode : GameModes.SURVIVAL;
        stageIndex = Math.max(0, Math.min(StageCatalog.COUNT - 1, stageIndex));
        random.setSeed(mode == GameModes.CAMPAIGN ? StageCatalog.get(stageIndex).seed : initialSeed);
        for (Entity e : entities) e.active = false;
        for (int i = 0; i < power.length; i++) power[i] = 0;
        tutorial = training; alive = true; won = false; nearMisses = 0; overdrive = 0;
        seconds = 0; points = 0; speed = GameConfig.START_SPEED;
        playerLane = 1; targetLane = 1; combo = 0; bestCombo = 0; comboTime = 0;
        energy = 0; passed = 0; powers = 0; moves = 0; invulnerable = 0;
        hits = 0; wallsBroken = 0; overdriveActivations = 0; charge = 0;
        jumpTime = duckTime = actionCooldown = bossHealth = bossWarning = bossEnd = 0;
        stunts = bossHits = bossPhase = bossesDefeated = 0; bossActive = false; nextBossPhase = 1;
        boostFuel = mode == GameModes.STUNT ? .5f : 0;
        lives = mode == GameModes.CAMPAIGN || mode == GameModes.STUNT ? (relaxed ? 5 : 3) : (relaxed ? 3 : 1);
        if (mode == GameModes.ZEN || mode == GameModes.COIN_RUSH || (mode == GameModes.STUNT && partyMode)) lives = 0;
        waveIndex = 0; pattern = CRUISE; safeLane = 1; waveWarning = 0;
        spawnTimer = training ? 99 : 3; powerTimer = mode == GameModes.COIN_RUSH ? 6 : 9;
        lastGap = 1; slalomDirection = 1;
    }
    public void moveTo(int lane) {
        if (!alive) return;
        int next = Math.max(0, Math.min(2, lane));
        if (next != targetLane) moves++;
        targetLane = next;
    }
    public boolean jump() {
        if (!alive || mode != GameModes.STUNT || actionCooldown > 0 || duckTime > 0 || jumpTime > 0) return false;
        jumpTime = GameConfig.JUMP_SECONDS; actionCooldown = GameConfig.ACTION_COOLDOWN_SECONDS;
        emit(ACTION, 0, playerLane, GameConfig.PLAYER_Y); return true;
    }
    public boolean duck() {
        if (!alive || mode != GameModes.STUNT || actionCooldown > 0 || jumpTime > 0 || duckTime > 0) return false;
        duckTime = GameConfig.DUCK_SECONDS; actionCooldown = GameConfig.ACTION_COOLDOWN_SECONDS;
        emit(ACTION, 1, playerLane, GameConfig.PLAYER_Y); return true;
    }
    public boolean boost() {
        if (!alive || mode != GameModes.STUNT || boostFuel + .000001f < GameConfig.BOOST_COST || overdrive > 0) return false;
        boostFuel = Math.max(0, boostFuel - GameConfig.BOOST_COST); overdrive = GameConfig.OVERDRIVE_SECONDS;
        overdriveActivations++; emit(ACTION, 2, playerLane, GameConfig.PLAYER_Y);
        emit(OVERDRIVE, overdriveActivations, playerLane, GameConfig.PLAYER_Y); return true;
    }
    /** Arc drives both visible lift and hurdle clearance, so action timing matters. */
    public float jumpHeight() {
        return jumpTime <= 0 ? 0 : Math.max(0, (float)Math.sin((1 - jumpTime / GameConfig.JUMP_SECONDS) * Math.PI));
    }
    /** Identifies each start on this engine so a finished run is settled only once. */
    public long runSequence() { return runSequence; }
    public long score() { return (long) points; }
    public int multiplier() {
        return (1 + Math.min(4, combo / 5)) * (power[3] > 0 ? 2 : 1) * (overdrive > 0 ? 2 : 1);
    }
    public float durationSeconds() {
        if (tutorial) return 0;
        switch (mode) {
            case GameModes.SPRINT: return GameConfig.SPRINT_SECONDS;
            case GameModes.DAILY: return GameConfig.DAILY_SECONDS;
            case GameModes.ZEN: return GameConfig.ZEN_SECONDS;
            case GameModes.COIN_RUSH: return GameConfig.COIN_RUSH_SECONDS;
            case GameModes.STUNT: return GameConfig.STUNT_SECONDS;
            case GameModes.CAMPAIGN: return StageCatalog.get(stageIndex).duration;
            default: return 0;
        }
    }
    public int objectiveTarget() {
        return mode == GameModes.CAMPAIGN ? StageCatalog.get(stageIndex).target : (int) durationSeconds();
    }
    public int objectiveProgress() {
        if (mode != GameModes.CAMPAIGN) return (int) seconds;
        switch (StageCatalog.get(stageIndex).goal) {
            case StageCatalog.ENERGY: return energy;
            case StageCatalog.COMBO: return bestCombo;
            case StageCatalog.NEAR: return nearMisses;
            case StageCatalog.POWER: return powers;
            case StageCatalog.OVERDRIVE: return overdriveActivations;
            default: return (int) seconds;
        }
    }
    /** Stars are meaningful only on a completed campaign mission; failures return zero. */
    public int stageStars() {
        if (mode != GameModes.CAMPAIGN || !won || alive) return 0;
        return hits == 0 ? 3 : hits == 1 ? 2 : 1;
    }
    public String objectiveName(boolean fa) {
        return mode == GameModes.CAMPAIGN ? StageCatalog.get(stageIndex).objectiveName(fa)
                : StageCatalog.objectiveName(StageCatalog.SURVIVE, fa);
    }
    public void spawn(int type, int lane, float y) {
        if (type < WALL || type > MAX_TYPE || (type > DOUBLE && mode != GameModes.STUNT) || lane < 0 || lane > 2 || !Float.isFinite(y)) return;
        for (Entity e : entities) if (!e.active) {
            e.active = true; e.type = type; e.lane = lane; e.y = y;
            e.style = random.nextInt(3); e.nearChecked = false;
            e.pattern = pattern; e.rowId = waveIndex; e.closestApproach = Float.MAX_VALUE;
            return;
        }
    }
    private void emit(int event, int detail, float lane, float y) {
        if (listener != null) listener.event(event, detail, lane, y);
    }
    private int chooseGap() {
        if (pattern == SLALOM) {
            if (lastGap == 0) slalomDirection = 1;
            if (lastGap == 2) slalomDirection = -1;
            return lastGap + slalomDirection;
        }
        return Math.max(0, Math.min(2, lastGap + random.nextInt(3) - 1));
    }
    private void wallRow(int gap, float y, boolean doubleWall, int style) {
        int blocked = (gap + 1 + random.nextInt(2)) % 3;
        spawnWall(blocked, y, style);
        if (doubleWall) spawnWall(3 - gap - blocked, y, style);
    }
    private void spawnWall(int lane, float y, int style) {
        spawn(WALL, lane, y);
        // The active matching entity is the one just taken from the pool.
        for (Entity e : entities) if (e.active && e.type == WALL && e.lane == lane && e.y == y && e.rowId == waveIndex) {
            e.style = style; break;
        }
    }
    private void wave() {
        if (mode == GameModes.STUNT) { stuntWave(); return; }
        waveIndex++;
        int difficulty = mode == GameModes.CAMPAIGN ? StageCatalog.get(stageIndex).world : (int) (seconds / 22);
        if (mode == GameModes.ZEN) pattern = waveIndex % 3 == 0 ? SLALOM : CRUISE;
        else if (difficulty < 1) pattern = waveIndex % 4 == 0 ? SLALOM : CRUISE;
        else if (waveIndex % 8 == 0) pattern = GUARDIAN;
        else pattern = (waveIndex + difficulty) % 5;
        int gap = chooseGap(); lastGap = gap; safeLane = gap; waveWarning = .85f;
        boolean two = difficulty >= 1 && random.nextFloat() < .6f;
        if (mode == GameModes.COIN_RUSH && waveIndex % 5 == 0) {
            // A free treasure corridor rewards positioning and the magnet power-up.
            pattern = CRUISE;
            for (int lane = 0; lane < 3; lane++) {
                spawn(ENERGY, lane, -.08f); spawn(ENERGY, lane, -.18f); spawn(ENERGY, lane, -.28f);
            }
        } else if (pattern == BRAID) {
            wallRow(gap, -.08f, true, BRAID);
            spawn(ENERGY, gap, -.08f);
            int nextGap = Math.max(0, Math.min(2, gap + (gap == 0 ? 1 : gap == 2 ? -1 : random.nextBoolean() ? 1 : -1)));
            wallRow(nextGap, -.20f, true, BRAID);
            spawn(ENERGY, nextGap, -.20f);
            lastGap = nextGap;
        } else if (pattern == GUARDIAN) {
            // Three telegraphed gate rows keep the same safe lane throughout a burst.
            for (int row = 0; row < 3; row++) {
                float y = -.08f - row * .14f;
                wallRow(gap, y, true, GUARDIAN); spawn(ENERGY, gap, y);
            }
        } else if (pattern == METEORS) {
            wallRow(gap, -.08f, false, METEORS);
            wallRow(gap, -.20f, false, METEORS);
            spawn(ENERGY, gap, -.08f); spawn(ENERGY, gap, -.20f);
        } else {
            wallRow(gap, -.08f, pattern == GATES || two, pattern);
            spawn(ENERGY, gap, -.08f); spawn(ENERGY, gap, -.16f);
            if (mode == GameModes.COIN_RUSH) spawn(ENERGY, gap, -.24f);
        }
        emit(WAVE, pattern, gap, 0);
    }
    /** An empty marked lane is always available; action lanes offer a higher scoring route. */
    private void stuntWave() {
        waveIndex++; pattern = bossActive ? GUARDIAN : waveIndex % 3 == 0 ? SLALOM : CRUISE;
        int gap = chooseGap(); lastGap = gap; safeLane = gap; waveWarning = bossActive ? 1.35f : 1;
        int actionLane = (gap + (random.nextBoolean() ? 1 : 2)) % 3;
        int wallLane = 3 - gap - actionLane;
        if (bossActive || waveIndex % 3 != 0) spawnWall(wallLane, -.08f, bossActive ? GUARDIAN : METEORS);
        int action = (waveIndex + bossPhase) % 2 == 0 ? JUMP_BARRIER : DUCK_BEAM;
        spawn(action, actionLane, -.08f); spawn(ENERGY, actionLane, -.08f);
        spawn(ENERGY, gap, -.08f); spawn(ENERGY, gap, -.26f);
        if (bossActive || waveIndex % 3 == 0) spawn(BOOST_RING, gap, -.16f);
        emit(WAVE, pattern, gap, 0);
    }
    private void updateBoss() {
        bossWarning = 0;
        if (bossActive && seconds >= bossEnd) bossActive = false;
        if (nextBossPhase > 3) return;
        float entry = 22 + (nextBossPhase - 1) * 26;
        if (seconds >= entry) {
            bossPhase = nextBossPhase++; bossHealth = 1; bossActive = true; bossEnd = seconds + 14;
            emit(BOSS, bossPhase, playerLane, 0);
        } else if (seconds >= entry - 2) bossWarning = entry - seconds;
    }
    private void damageBoss(float amount, float lane, float y) {
        if (!bossActive || bossHealth <= 0) return;
        bossHits++; bossHealth = Math.max(0, bossHealth - amount);
        if (bossHealth <= .000001f) {
            bossHealth = 0; bossActive = false; bossesDefeated++; points += 1500;
            boostFuel = Math.min(1, boostFuel + .2f);
            if (!partyMode) lives = Math.min(relaxed ? 5 : 3, lives + 1);
            emit(BOSS_BREAK, bossPhase, lane, y);
        }
    }
    private void stuntCleared(Entity e) {
        e.active = false; passed++; stunts++; combo++; comboTime = GameConfig.COMBO_WINDOW;
        bestCombo = Math.max(bestCombo, combo); points += 150 * multiplier(); boostFuel = Math.min(1, boostFuel + .065f);
        if (overdrive > 0) wallsBroken++;
        if (e.pattern == GUARDIAN) damageBoss(.28f, e.lane, e.y);
        emit(STUNT, e.type, e.lane, e.y);
    }
    private float waveInterval() {
        if (mode == GameModes.STUNT) return (bossActive ? 1.55f : 1.8f) * (relaxed ? 1.12f : 1);
        float normal = Math.max(.88f, 1.35f - seconds * .004f);
        if (mode == GameModes.ZEN) normal = 1.9f;
        if (relaxed) normal *= 1.14f;
        // Reserve at least .16 normalized units between all successive obstacle rows.
        // Slow time scales both movement and the spawn clock by the same factor.
        return Math.max(normal, (pattern == GUARDIAN ? .46f : .30f) / speed);
    }
    private void addCharge(float amount, float lane, float y) {
        if (mode == GameModes.STUNT) { boostFuel = Math.min(1, boostFuel + amount * .3f); return; }
        if (overdrive > 0) return;
        charge = Math.min(1, charge + amount);
        if (charge >= .999f) {
            charge = 0; overdrive = GameConfig.OVERDRIVE_SECONDS;
            overdriveActivations++; emit(OVERDRIVE, overdriveActivations, lane, y);
        }
    }
    private void collision(Entity e) {
        e.active = false;
        if (overdrive > 0) {
            wallsBroken++; passed++; points += 60 * multiplier();
            if (mode == GameModes.STUNT && e.pattern == GUARDIAN) damageBoss(.15f, e.lane, e.y);
            emit(SMASH, wallsBroken, e.lane, e.y); return;
        }
        if (power[0] > 0 || tutorial) {
            power[0] = 0; invulnerable = GameConfig.HIT_GRACE_SECONDS;
            emit(HIT, 0, e.lane, e.y); return;
        }
        hits++; combo = 0; comboTime = 0; charge *= .5f;
        if (mode == GameModes.ZEN || mode == GameModes.COIN_RUSH || (mode == GameModes.STUNT && partyMode)) {
            invulnerable = GameConfig.HIT_GRACE_SECONDS;
            if (mode == GameModes.COIN_RUSH || (mode == GameModes.STUNT && partyMode)) points = Math.max(0, points - 100);
            emit(HIT, hits, e.lane, e.y); return;
        }
        lives--;
        if (lives > 0) {
            invulnerable = GameConfig.HIT_GRACE_SECONDS; emit(HIT, hits, e.lane, e.y);
        } else {
            alive = false; emit(DEATH, hits, playerLane, GameConfig.PLAYER_Y);
        }
    }
    private void finishTimedRun() {
        alive = false;
        won = mode != GameModes.CAMPAIGN || objectiveProgress() >= objectiveTarget();
        emit(COMPLETE, won ? 1 : 0, playerLane, GameConfig.PLAYER_Y);
    }
    public void update(float dt) {
        if (!alive || dt <= 0 || !Float.isFinite(dt)) return;
        dt = Math.min(.05f, dt);
        float duration = durationSeconds();
        if (duration > 0) dt = Math.min(dt, Math.max(0, duration - seconds));
        seconds += dt; overdrive = Math.max(0, overdrive - dt); waveWarning = Math.max(0, waveWarning - dt);
        if (mode == GameModes.STUNT) {
            jumpTime = Math.max(0, jumpTime - dt); duckTime = Math.max(0, duckTime - dt);
            actionCooldown = Math.max(0, actionCooldown - dt); boostFuel = Math.min(1, boostFuel + dt * .012f);
            if (!tutorial) updateBoss();
        }
        float difficulty = mode == GameModes.CAMPAIGN ? StageCatalog.get(stageIndex).difficulty : 1;
        speed = Math.min(GameConfig.MAX_SPEED, (GameConfig.START_SPEED + seconds * .0024f) * difficulty);
        if (mode == GameModes.ZEN) speed = .18f + Math.min(.05f, seconds * .0003f);
        if (mode == GameModes.STUNT) speed = Math.min(GameConfig.MAX_SPEED, .34f + seconds * .0018f + (overdrive > 0 ? .10f : 0));
        if (relaxed) speed *= .8f;
        float velocity = speed * (power[2] > 0 ? .55f : 1);
        playerLane += Math.max(-dt * 12, Math.min(dt * 12, targetLane - playerLane));
        for (int i = 0; i < power.length; i++) power[i] = Math.max(0, power[i] - dt);
        invulnerable = Math.max(0, invulnerable - dt);
        comboTime = Math.max(0, comboTime - dt); if (comboTime == 0) combo = 0;
        if (!tutorial) {
            points += dt * 12 * multiplier();
            spawnTimer -= dt * (power[2] > 0 ? .55f : 1);
            if (spawnTimer <= 0) { wave(); spawnTimer = waveInterval(); }
            powerTimer -= dt;
            if (powerTimer <= 0) {
                spawn(SHIELD + random.nextInt(4), lastGap, -.34f);
                powerTimer = (mode == GameModes.COIN_RUSH ? 9 : 13) + random.nextFloat() * 5;
            }
        }
        for (Entity e : entities) {
            if (!e.active) continue;
            float previous = e.y; e.y += velocity * dt;
            float distance = Math.abs(e.lane - playerLane);
            boolean hazard = e.type == WALL || e.type == JUMP_BARRIER || e.type == DUCK_BEAM;
            if (hazard && previous < GameConfig.PLAYER_Y + .055f && e.y > GameConfig.PLAYER_Y - .09f)
                e.closestApproach = Math.min(e.closestApproach, distance);
            boolean contact = previous < GameConfig.PLAYER_Y + .045f && e.y > GameConfig.PLAYER_Y - .045f && distance < .34f;
            if (e.type == ENERGY && power[1] > 0 && Math.abs(e.y - GameConfig.PLAYER_Y) < .19f) contact = true;
            if (contact) {
                if (hazard) {
                    if (invulnerable > 0) { e.active = false; continue; }
                    if (e.type != WALL && (overdrive > 0 || (e.type == JUMP_BARRIER && jumpHeight() >= .22f) || (e.type == DUCK_BEAM && duckTime > 0))) stuntCleared(e);
                    else { collision(e); if (!alive) return; }
                } else if (e.type == ENERGY) {
                    e.active = false; energy++; combo++; comboTime = GameConfig.COMBO_WINDOW;
                    bestCombo = Math.max(bestCombo, combo); points += 25 * multiplier();
                    addCharge(.1f, e.lane, e.y);
                    if (mode == GameModes.STUNT && e.pattern == GUARDIAN) damageBoss(.035f, e.lane, e.y);
                    emit(combo % 5 == 0 ? COMBO : COLLECT, combo, e.lane, e.y);
                } else if (e.type == BOOST_RING) {
                    e.active = false; boostFuel = Math.min(1, boostFuel + .23f); points += 120 * multiplier();
                    emit(STUNT, BOOST_RING, e.lane, e.y);
                    if (e.pattern == GUARDIAN) damageBoss(.12f, e.lane, e.y);
                } else {
                    e.active = false; powers++; int i = e.type - SHIELD;
                    power[i] = GameConfig.POWER_SECONDS[i]; points += 75;
                    emit(POWER, i, e.lane, e.y);
                }
            }
            if (e.active && hazard && !e.nearChecked && e.y > GameConfig.PLAYER_Y + .055f) {
                e.nearChecked = true;
                if (e.closestApproach >= .34f && e.closestApproach < GameConfig.NEAR_DISTANCE) {
                    nearMisses++; points += 35 * multiplier(); addCharge(.06f, e.lane, e.y);
                    emit(NEAR, nearMisses, e.lane, e.y);
                }
            }
            if (e.active && e.y > 1.08f) {
                if (hazard) { passed++; points += 15 * multiplier(); }
                e.active = false;
            }
        }
        if (duration > 0 && seconds >= duration) { seconds = duration; finishTimedRun(); }
    }
}
