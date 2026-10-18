package com.toosarax.jetjo.core;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regressions for terminal input, flight-school isolation and fixed-step resumption. */
public final class GameStateAuditTest {
    private GameEngine start(int mode){GameEngine run=new GameEngine(927);run.mode=mode;run.start(false);return run;}
    @Test public void settlementIdentityChangesOnlyWhenTheEngineStartsAnotherRun(){
        GameEngine run=new GameEngine(927);run.mode=GameModes.STUNT;assertEquals(0,run.runSequence());
        run.start(false);long first=run.runSequence();assertEquals(1,first);run.update(GameConfig.STEP);assertEquals(first,run.runSequence());
        run.seconds=run.durationSeconds()-.01f;run.update(GameConfig.STEP);assertFalse(run.alive);assertEquals(first,run.runSequence());
        run.moveTo(0);run.jump();run.update(GameConfig.STEP);assertEquals(first,run.runSequence());
        run.start(false);long second=run.runSequence();assertEquals(first+1,second);assertTrue(run.alive);assertEquals(0,run.seconds,0);
        run.start(true);assertEquals(second+1,run.runSequence());assertTrue(run.tutorial);
        run.start(false);assertEquals(second+2,run.runSequence());assertFalse(run.tutorial);
    }
    @Test public void noLaneInputCanChangeACompletedTimedRun(){
        for(int mode:new int[]{GameModes.SPRINT,GameModes.DAILY,GameModes.ZEN,GameModes.CAMPAIGN,GameModes.COIN_RUSH,GameModes.STUNT}){
            GameEngine run=start(mode);run.seconds=run.durationSeconds()-.01f;run.update(.02f);
            assertFalse("mode "+mode,run.alive);int target=run.targetLane,moves=run.moves;long score=run.score();float clock=run.seconds;
            run.moveTo(0);run.moveTo(2);run.jump();run.duck();run.boost();run.update(GameConfig.STEP);
            assertEquals("terminal target for mode "+mode,target,run.targetLane);assertEquals(moves,run.moves);assertEquals(score,run.score());assertEquals(clock,run.seconds,0);
        }
    }
    @Test public void noLaneInputCanChangeADeadSurvivalRun(){
        GameEngine run=start(GameModes.SURVIVAL);run.spawn(GameEngine.WALL,1,GameConfig.PLAYER_Y);run.update(GameConfig.STEP);
        assertFalse(run.alive);int moves=run.moves;run.moveTo(0);assertEquals(1,run.targetLane);assertEquals(moves,run.moves);
        run.start(false);run.moveTo(0);assertEquals(0,run.targetLane);assertEquals(1,run.moves);
    }
    @Test public void slowFlightSchoolDoesNotStartAnyGuardianEncounter(){
        GameEngine run=new GameEngine(927);run.mode=GameModes.STUNT;run.start(true);int[] guardians={0};
        run.listener=(event,detail,lane,y)->{if(event==GameEngine.BOSS||event==GameEngine.BOSS_BREAK)guardians[0]++;};
        for(int tick=0;tick<2700;tick++)run.update(GameConfig.STEP);
        assertTrue(run.alive);assertTrue(run.tutorial);assertEquals(0,run.bossPhase);assertFalse(run.bossActive);assertEquals(0,run.bossWarning,0);assertEquals(0,guardians[0]);
        for(GameEngine.Entity entity:run.entities)assertFalse(entity.active);
        run.start(false);run.seconds=22;run.update(GameConfig.STEP);assertEquals(1,run.bossPhase);assertTrue(run.bossActive);
    }
    @Test public void invalidOrPausedUpdatesCannotAdvanceActionsOrGuardianState(){
        GameEngine run=start(GameModes.STUNT);run.seconds=20.5f;run.jump();run.boost();run.update(GameConfig.STEP);
        float clock=run.seconds,jump=run.jumpTime,turbo=run.overdrive,warning=run.bossWarning,fuel=run.boostFuel;long score=run.score();
        for(float step:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})run.update(step);
        assertEquals(clock,run.seconds,0);assertEquals(jump,run.jumpTime,0);assertEquals(turbo,run.overdrive,0);assertEquals(warning,run.bossWarning,0);assertEquals(fuel,run.boostFuel,0);assertEquals(score,run.score());
        run.update(GameConfig.STEP);assertTrue(run.seconds>clock);assertTrue(run.jumpTime<jump);assertTrue(run.overdrive<turbo);
    }
    @Test public void aFiniteActionRunEmitsOnlyOneCompletionEvenAfterMoreInput(){
        GameEngine run=start(GameModes.STUNT);int[] complete={0},death={0};run.listener=(event,detail,lane,y)->{if(event==GameEngine.COMPLETE)complete[0]++;if(event==GameEngine.DEATH)death[0]++;};
        run.seconds=GameConfig.STUNT_SECONDS-.01f;run.update(GameConfig.STEP);
        for(int i=0;i<120;i++){run.moveTo(i%3);run.jump();run.duck();run.boost();run.update(GameConfig.STEP);}
        assertTrue(run.won);assertEquals(1,complete[0]);assertEquals(0,death[0]);assertEquals(GameConfig.STUNT_SECONDS,run.seconds,0);
    }
}
