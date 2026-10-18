package com.toosarax.jetjo.core;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class StuntGameEngineTest {
    private GameEngine start() {GameEngine e=new GameEngine(927);e.mode=GameModes.STUNT;e.start(false);return e;}
    private void advance(GameEngine e,float duration){int n=(int)Math.ceil(duration/GameConfig.STEP);for(int i=0;i<n;i++)e.update(GameConfig.STEP);}
    private boolean hazard(int type){return type==GameEngine.WALL||type==GameEngine.JUMP_BARRIER||type==GameEngine.DUCK_BEAM;}
    private void steerSafely(GameEngine e){
        GameEngine.Entity front=null;
        for(GameEngine.Entity item:e.entities)if(item.active&&hazard(item.type)&&item.y<GameConfig.PLAYER_Y+.055f&&(front==null||item.y>front.y))front=item;
        if(front==null)return;int blocked=0;
        for(GameEngine.Entity item:e.entities)if(item.active&&hazard(item.type)&&Math.abs(item.y-front.y)<.015f)blocked|=1<<item.lane;
        if((blocked&(1<<e.targetLane))==0)return;
        int best=-1;for(int lane=0;lane<3;lane++)if((blocked&(1<<lane))==0&&(best<0||Math.abs(lane-e.playerLane)<Math.abs(best-e.playerLane)))best=lane;
        if(best>=0)e.moveTo(best);
    }
    @Test public void actionModeIsAppendedWithItsOwnDurationAndHearts(){
        GameEngine e=start();assertEquals(6,GameModes.STUNT);assertEquals(7,GameModes.COUNT);
        assertEquals(100,e.durationSeconds(),0);assertEquals(3,e.lives);assertEquals(.5f,e.boostFuel,0);
        e.relaxed=true;e.start(false);assertEquals(5,e.lives);
    }
    @Test public void actionsAreDisabledInAllOriginalModesAndAfterFinish(){
        for(int mode=0;mode<6;mode++){GameEngine e=new GameEngine(1);e.mode=mode;e.start(false);e.boostFuel=1;assertFalse(e.jump());assertFalse(e.duck());assertFalse(e.boost());}
        GameEngine e=start();e.alive=false;assertFalse(e.jump());assertFalse(e.duck());assertFalse(e.boost());
    }
    @Test public void jumpArcPeaksThenLandsAndHasARealCooldown(){
        GameEngine e=start();assertTrue(e.jump());assertEquals(0,e.jumpHeight(),.0001);assertFalse(e.jump());assertFalse(e.duck());
        advance(e,.45f);assertEquals(1,e.jumpHeight(),.01);
        advance(e,.5f);assertEquals(0,e.jumpHeight(),0);assertFalse(e.jump());
        advance(e,.12f);assertTrue(e.duck());assertFalse(e.jump());
    }
    @Test public void correctlyTimedJumpClearsAHurdleAndRewardsSkill(){
        GameEngine e=start();assertTrue(e.jump());advance(e,.18f);
        e.spawn(GameEngine.JUMP_BARRIER,1,GameConfig.PLAYER_Y);e.update(GameConfig.STEP);
        assertEquals(0,e.hits);assertEquals(3,e.lives);assertEquals(1,e.stunts);assertEquals(1,e.combo);assertTrue(e.score()>=150);
    }
    @Test public void jumpTooEarlyOrDuckAtAHurdleCostsOneHeart(){
        GameEngine a=start();a.jump();a.spawn(GameEngine.JUMP_BARRIER,1,GameConfig.PLAYER_Y);a.update(GameConfig.STEP);assertEquals(1,a.hits);
        GameEngine b=start();b.duck();b.spawn(GameEngine.JUMP_BARRIER,1,GameConfig.PLAYER_Y);b.update(GameConfig.STEP);assertEquals(1,b.hits);
    }
    @Test public void duckClearsABeamButJumpAndDuckCannotClearWalls(){
        GameEngine a=start();a.duck();a.spawn(GameEngine.DUCK_BEAM,1,GameConfig.PLAYER_Y);a.update(GameConfig.STEP);assertEquals(0,a.hits);assertEquals(1,a.stunts);
        GameEngine b=start();b.jump();advance(b,.18f);b.spawn(GameEngine.DUCK_BEAM,1,GameConfig.PLAYER_Y);b.update(GameConfig.STEP);assertEquals(1,b.hits);
        GameEngine c=start();c.duck();c.spawn(GameEngine.WALL,1,GameConfig.PLAYER_Y);c.update(GameConfig.STEP);assertEquals(1,c.hits);
    }
    @Test public void actionHazardsShareDamageGraceAndCannotDrainSeveralHeartsAtOnce(){
        GameEngine e=start();for(int i=0;i<10;i++)e.spawn(i%2==0?GameEngine.JUMP_BARRIER:GameEngine.DUCK_BEAM,1,GameConfig.PLAYER_Y);
        e.update(GameConfig.STEP);assertEquals(1,e.hits);assertEquals(2,e.lives);assertTrue(e.alive);
        for(int i=0;i<2;i++){e.invulnerable=0;e.spawn(GameEngine.JUMP_BARRIER,1,GameConfig.PLAYER_Y);e.update(GameConfig.STEP);}
        assertFalse(e.alive);assertFalse(e.won);assertEquals(3,e.hits);
    }
    @Test public void manualBoostConsumesFuelAndSmashesBothActionHazards(){
        GameEngine e=start();assertTrue(e.boost());assertEquals(.5f-GameConfig.BOOST_COST,e.boostFuel,.0001);assertFalse(e.boost());
        e.spawn(GameEngine.JUMP_BARRIER,1,GameConfig.PLAYER_Y);e.spawn(GameEngine.DUCK_BEAM,1,GameConfig.PLAYER_Y);e.update(GameConfig.STEP);
        assertEquals(0,e.hits);assertEquals(2,e.stunts);assertEquals(2,e.wallsBroken);
        e.boostFuel=0;e.overdrive=0;assertFalse(e.boost());
    }
    @Test public void boostRingRefillsFuelWithoutIndexingThePowerArray(){
        GameEngine e=start();e.boostFuel=.9f;e.spawn(GameEngine.BOOST_RING,1,GameConfig.PLAYER_Y);e.update(GameConfig.STEP);
        assertEquals(1,e.boostFuel,0);assertEquals(0,e.stunts);assertEquals(0,e.powers);
        for(float power:e.power)assertEquals(0,power,0);
    }
    @Test public void actionsSendTheDocumentedEventDetailsOnce(){
        GameEngine e=start();List<Integer> actions=new ArrayList<>();e.listener=(event,detail,lane,y)->{if(event==GameEngine.ACTION)actions.add(detail);};
        e.jump();e.jump();advance(e,1.1f);e.duck();e.duck();e.boost();e.boost();
        assertEquals(java.util.Arrays.asList(0,1,2),actions);
    }
    @Test public void guardianArrivalHasTwoSecondsOfWarningAndThreeDistinctPhases(){
        GameEngine e=start();List<Integer> phases=new ArrayList<>();e.listener=(event,detail,lane,y)->{if(event==GameEngine.BOSS)phases.add(detail);};
        for(int phase=1;phase<=3;phase++){
            float entry=22+(phase-1)*26;e.seconds=entry-1;e.update(GameConfig.STEP);
            assertFalse(e.bossActive);assertTrue(e.bossWarning>0&&e.bossWarning<=2);
            e.seconds=entry;e.update(GameConfig.STEP);assertTrue(e.bossActive);assertEquals(phase,e.bossPhase);assertEquals(1,e.bossHealth,0);
            e.seconds=entry+14.1f;e.update(GameConfig.STEP);assertFalse(e.bossActive);
        }
        assertEquals(java.util.Arrays.asList(1,2,3),phases);
    }
    @Test public void skillBreaksGuardianAndRestoresOneHeartOnlyOnce(){
        GameEngine e=start();e.seconds=22;e.update(GameConfig.STEP);e.pattern=GameEngine.GUARDIAN;e.lives=2;e.boostFuel=1;e.boost();
        int[] breaks={0};e.listener=(event,detail,lane,y)->{if(event==GameEngine.BOSS_BREAK)breaks[0]++;};
        for(int i=0;i<4;i++){e.spawn(GameEngine.JUMP_BARRIER,1,GameConfig.PLAYER_Y);e.update(GameConfig.STEP);}
        assertFalse(e.bossActive);assertEquals(0,e.bossHealth,0);assertEquals(1,e.bossesDefeated);assertEquals(1,breaks[0]);assertEquals(3,e.lives);
        e.spawn(GameEngine.BOOST_RING,1,GameConfig.PLAYER_Y);e.update(GameConfig.STEP);assertEquals(1,breaks[0]);assertEquals(4,e.bossHits);
    }
    @Test public void allGeneratedRowsLeaveAnActionFreeCorridorAndAllHazardsAppear(){
        GameEngine e=start();boolean[] types=new boolean[9];
        for(int tick=0;tick<6500&&e.alive;tick++){
            steerSafely(e);e.update(GameConfig.STEP);
            for(GameEngine.Entity a:e.entities)if(a.active){types[a.type]=true;if(hazard(a.type)){
                int blocked=0;for(GameEngine.Entity b:e.entities)if(b.active&&hazard(b.type)&&Math.abs(a.y-b.y)<.091f)blocked|=1<<b.lane;
                assertNotEquals("closed row at "+e.seconds,7,blocked);
            }}
        }
        assertTrue(e.won);assertEquals(0,e.hits);assertEquals(100,e.seconds,0);assertEquals(3,e.bossPhase);
        assertTrue(types[GameEngine.JUMP_BARRIER]);assertTrue(types[GameEngine.DUCK_BEAM]);assertTrue(types[GameEngine.BOOST_RING]);
    }
    @Test public void restartResetsEveryActionAndGuardianField(){
        GameEngine e=start();e.jump();e.seconds=22;e.update(GameConfig.STEP);e.bossHits=9;e.stunts=10;e.bossesDefeated=2;e.start(false);
        assertEquals(0,e.jumpTime,0);assertEquals(0,e.duckTime,0);assertEquals(0,e.bossHits);assertEquals(0,e.stunts);assertEquals(0,e.bossPhase);assertEquals(0,e.bossesDefeated);assertFalse(e.bossActive);assertEquals(.5f,e.boostFuel,0);
    }
    @Test public void originalModesNeverSpawnActionOnlyTypes(){
        for(int mode=0;mode<6;mode++){GameEngine e=new GameEngine(927);e.mode=mode;e.start(false);
            e.spawn(GameEngine.JUMP_BARRIER,1,.2f);e.spawn(GameEngine.DUCK_BEAM,1,.2f);e.spawn(GameEngine.BOOST_RING,1,.2f);
            for(int i=0;i<1800;i++){e.invulnerable=1;e.update(GameConfig.STEP);for(GameEngine.Entity item:e.entities)if(item.active)assertTrue(item.type<=GameEngine.DOUBLE);}
        }
    }
    @Test public void allSixOriginalModeSeededSimulationsKeepTheirPreActionFingerprints(){
        long[] original={273928453425524211L,273928453425524211L,273928453425524211L,3926237693026828107L,5249467709947877468L,4533332464340268537L};
        for(int mode=0;mode<6;mode++)assertEquals("original mode "+mode,original[mode],fingerprint(mode));
    }
    private long mix(long hash,long value){return(hash^value)*0x100000001b3L;}
    private long fingerprint(int mode){
        GameEngine e=new GameEngine(927);e.mode=mode;e.stageIndex=19;e.start(false);long hash=0xcbf29ce484222325L;
        for(int tick=0;tick<1800;tick++){
            e.invulnerable=1;e.moveTo((tick/43)%3);e.update(GameConfig.STEP);
            hash=mix(hash,Float.floatToIntBits(e.seconds));hash=mix(hash,Float.floatToIntBits(e.playerLane));hash=mix(hash,Double.doubleToLongBits(e.points));hash=mix(hash,e.combo);hash=mix(hash,e.waveIndex);
            for(GameEngine.Entity item:e.entities){hash=mix(hash,item.active?1:0);if(item.active){hash=mix(hash,item.type);hash=mix(hash,item.lane);hash=mix(hash,item.style);hash=mix(hash,item.pattern);hash=mix(hash,Float.floatToIntBits(item.y));}}
        }return hash;
    }
}
