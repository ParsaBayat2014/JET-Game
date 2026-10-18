package com.toosarax.jetjo.core;
import org.junit.Test;
import static org.junit.Assert.*;

public class AdvancedGameEngineTest {
    private GameEngine start(int mode) { GameEngine e = new GameEngine(927); e.mode = mode; e.start(false); return e; }
    private void finishSafely(GameEngine e) {
        for (int i = 0; i < 16000 && e.alive; i++) { steerSafely(e); e.update(GameConfig.STEP); }
    }
    /** Follow the nearest upcoming gate's marked energy corridor, not future waves. */
    private void steerSafely(GameEngine e) {
        GameEngine.Entity front = null;
        for (GameEngine.Entity x : e.entities)
            if (x.active && x.type == GameEngine.WALL && x.y < GameConfig.PLAYER_Y + .055f
                    && (front == null || x.y > front.y)) front = x;
        if (front == null) return;
        int blocked = 0, marked = -1;
        for (GameEngine.Entity x : e.entities) if (x.active && Math.abs(x.y - front.y) < .015f) {
            if (x.type == GameEngine.WALL) blocked |= 1 << x.lane;
            if (x.type == GameEngine.ENERGY) marked = x.lane;
        }
        if (marked >= 0 && (blocked & (1 << marked)) == 0) { e.moveTo(marked); return; }
        if ((blocked & (1 << e.targetLane)) == 0) return;
        int best = -1;
        for (int lane = 0; lane < 3; lane++) if ((blocked & (1 << lane)) == 0
                && (best < 0 || Math.abs(lane - e.playerLane) < Math.abs(best - e.playerLane))) best = lane;
        e.moveTo(best);
    }
    @Test public void allFortyEightCampaignMissionsAreReachable() {
        for (boolean gentle : new boolean[]{false, true}) for (int index = 0; index < StageCatalog.COUNT; index++) {
            GameEngine e = new GameEngine(1); e.mode = GameModes.CAMPAIGN; e.stageIndex = index; e.relaxed = gentle; e.start(false);
            finishSafely(e);
            String stage = "stage " + index + " gentle=" + gentle + " goal=" + e.objectiveProgress() + "/" + e.objectiveTarget() + " hits=" + e.hits;
            assertFalse(stage, e.alive); assertTrue(stage, e.won); assertEquals(stage, 3, e.stageStars());
            assertEquals(StageCatalog.get(index).duration, e.seconds, 0);
        }
    }
    @Test public void campaignFailsWhenItsObjectiveWasNotReached() {
        GameEngine e = new GameEngine(1); e.mode = GameModes.CAMPAIGN; e.stageIndex = 2; e.start(false);
        e.seconds = e.durationSeconds() - .01f; e.update(.02f);
        assertFalse(e.alive); assertFalse(e.won); assertEquals(0, e.stageStars());
    }
    @Test public void campaignHasThreeHeartsAndGraceAfterDamage() {
        GameEngine e = start(GameModes.CAMPAIGN);
        for (int hit = 1; hit <= 3; hit++) {
            e.invulnerable = 0; e.spawn(GameEngine.WALL, 1, GameConfig.PLAYER_Y); e.update(.016f);
            assertEquals(hit, e.hits); assertEquals(3-hit, e.lives); assertEquals(hit < 3, e.alive);
        }
    }
    @Test public void finiteModesHaveOneTerminalEventAndDoNotAdvanceAfterward() {
        for (int mode : new int[]{GameModes.SPRINT, GameModes.DAILY, GameModes.ZEN, GameModes.COIN_RUSH}) {
            GameEngine e = start(mode); int[] completions = {0};
            e.listener = (event, detail, lane, y) -> { if (event == GameEngine.COMPLETE) completions[0]++; };
            finishSafely(e); assertTrue(e.won); assertFalse(e.alive); assertEquals(1, completions[0]);
            float seconds = e.seconds; double points = e.points; e.update(.05f);
            assertEquals(seconds, e.seconds, 0); assertEquals(points, e.points, 0); assertEquals(1, completions[0]);
        }
    }
    @Test public void zenAndTreasureCollisionsAreNonlethal() {
        for (int mode : new int[]{GameModes.ZEN, GameModes.COIN_RUSH}) {
            GameEngine e = start(mode); e.points = 300; e.combo = 8;
            e.spawn(GameEngine.WALL, 1, GameConfig.PLAYER_Y); e.update(.016f);
            assertTrue(e.alive); assertEquals(1, e.hits); assertEquals(0, e.combo);
            if (mode == GameModes.COIN_RUSH) assertTrue(e.points < 250);
        }
        assertFalse(GameModes.hasEconomyRewards(GameModes.ZEN));
    }
    @Test public void overdriveSmashesWallsWithoutSpendingHearts() {
        GameEngine e = start(GameModes.CAMPAIGN); e.overdrive = 3;
        e.spawn(GameEngine.WALL, 1, GameConfig.PLAYER_Y); e.update(.016f);
        assertTrue(e.alive); assertEquals(0, e.hits); assertEquals(3, e.lives); assertEquals(1, e.wallsBroken);
    }
    @Test public void restartReplaysTheSameSeededLayout() {
        GameEngine a = start(GameModes.DAILY), b = start(GameModes.DAILY);
        for (int i = 0; i < 1000; i++) { steerSafely(a); a.update(GameConfig.STEP); }
        a.start(false);
        for (int i = 0; i < 1000; i++) {
            steerSafely(a); steerSafely(b); a.update(GameConfig.STEP); b.update(GameConfig.STEP);
            for (int j = 0; j < a.entities.length; j++) {
                assertEquals(a.entities[j].active, b.entities[j].active);
                if (a.entities[j].active) {
                    assertEquals(a.entities[j].lane, b.entities[j].lane);
                    assertEquals(a.entities[j].type, b.entities[j].type);
                    assertEquals(a.entities[j].pattern, b.entities[j].pattern);
                    assertEquals(a.entities[j].y, b.entities[j].y, 0);
                }
            }
        }
    }
    @Test public void overlapNeverClosesAllThreeLanes() {
        GameEngine e = start(GameModes.SURVIVAL);
        for (int i = 0; i < 36000; i++) {
            e.invulnerable = 1; e.update(GameConfig.STEP);
            if (i % 30 == 0) for (GameEngine.Entity a : e.entities) if (a.active && a.type == GameEngine.WALL) {
                int blocked = 0;
                for (GameEngine.Entity b : e.entities) if (b.active && b.type == GameEngine.WALL && Math.abs(a.y-b.y) < .091f) blocked |= 1 << b.lane;
                assertNotEquals("overlapping gates at " + e.seconds, 7, blocked);
            }
        }
    }
    @Test public void invalidSpawnsCannotCorruptPowerArrayOrPool() {
        GameEngine e = start(GameModes.SURVIVAL);
        e.spawn(400, 1, .82f); e.spawn(GameEngine.WALL, -1, .82f); e.spawn(GameEngine.ENERGY, 1, Float.NaN);
        for (GameEngine.Entity x : e.entities) assertFalse(x.active);
        for (int i = 0; i < GameConfig.MAX_ENTITIES * 2; i++) e.spawn(GameEngine.ENERGY, 1, .82f);
        e.update(.016f); assertEquals(GameConfig.MAX_ENTITIES, e.energy);
    }
    @Test public void gentleRunStartsWithExtraHeartsAndMovesMoreSlowly() {
        GameEngine a = start(GameModes.CAMPAIGN), b = new GameEngine(927); b.mode = GameModes.CAMPAIGN; b.relaxed = true; b.start(false);
        a.update(.05f); b.update(.05f); assertEquals(5, b.lives); assertTrue(b.speed < a.speed);
    }
    @Test public void starsReflectDamageAndOnlySuccessfulCampaigns() {
        GameEngine e = start(GameModes.CAMPAIGN); e.alive = false; e.won = true;
        assertEquals(3, e.stageStars()); e.hits = 1; assertEquals(2, e.stageStars()); e.hits = 2; assertEquals(1, e.stageStars());
        e.won = false; assertEquals(0, e.stageStars()); e.mode = GameModes.SPRINT; e.won = true; assertEquals(0, e.stageStars());
    }
}