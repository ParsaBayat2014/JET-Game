package com.toosarax.jetjo.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class GameEngineTest {
    private GameEngine run(){GameEngine e=new GameEngine(712);e.start(false);return e;}
    private void steps(GameEngine e,int n){for(int i=0;i<n;i++)e.update(GameConfig.STEP);}
    @Test public void survivalScoreAccumulatesFractions(){GameEngine e=run();steps(e,120);assertEquals(24,e.score(),1);}
    @Test public void openingIsSafe(){GameEngine e=run();steps(e,170);for(GameEngine.Entity x:e.entities)assertFalse(x.active);assertTrue(e.alive);}
    @Test public void collisionEndsRun(){GameEngine e=run();e.spawn(GameEngine.WALL,1,GameConfig.PLAYER_Y);e.update(.016f);assertFalse(e.alive);}
    @Test public void shieldAbsorbsOnlyOneHit(){GameEngine e=run();e.power[0]=8;e.spawn(GameEngine.WALL,1,GameConfig.PLAYER_Y);e.update(.016f);assertTrue(e.alive);assertEquals(0,e.power[0],0);e.invulnerable=0;e.spawn(GameEngine.WALL,1,GameConfig.PLAYER_Y);e.update(.016f);assertFalse(e.alive);}
    @Test public void magnetCollectsAcrossLanes(){GameEngine e=run();e.power[1]=9;e.spawn(GameEngine.ENERGY,0,.74f);e.update(.016f);assertEquals(1,e.energy);}
    @Test public void slowPowerUsesRealTime(){GameEngine e=run();e.power[2]=1;steps(e,61);assertEquals(0,e.power[2],0);assertEquals(61/60f,e.seconds,.001);}
    @Test public void doubleScoreAffectsCollectibles(){GameEngine a=run(),b=run();b.power[3]=10;a.spawn(1,1,.82f);b.spawn(1,1,.82f);a.update(.016f);b.update(.016f);assertEquals(a.points*2,b.points,.001);}
    @Test public void tenComboActivatesOverdriveAndExpires(){GameEngine e=run();for(int i=0;i<10;i++){e.spawn(1,1,.82f);e.update(.016f);}assertTrue(e.overdrive>0);assertEquals(10,e.bestCombo);for(int i=0;i<245;i++){e.invulnerable=1;e.update(GameConfig.STEP);}assertEquals(0,e.overdrive,0);}
    @Test public void comboExpires(){GameEngine e=run();e.spawn(1,1,.82f);e.update(.016f);steps(e,194);assertEquals(0,e.combo);}
    @Test public void nearMissIsAwardedOnce(){GameEngine e=run();e.playerLane=.85f;e.targetLane=0;e.spawn(0,0,.873f);steps(e,20);assertEquals(1,e.nearMisses);}
    @Test public void sprintFinishesAtNinetySeconds(){GameEngine e=run();e.mode=1;for(int i=0;i<5500;i++){e.invulnerable=1;e.update(GameConfig.STEP);}assertTrue(e.won);assertFalse(e.alive);assertEquals(90,e.seconds,0);}
    @Test public void pauseByNotUpdatingPreservesState(){GameEngine e=run();steps(e,12);double points=e.points;float seconds=e.seconds;e.update(0);e.update(Float.NaN);assertEquals(points,e.points,0);assertEquals(seconds,e.seconds,0);}
    @Test public void inputIsBounded(){GameEngine e=run();e.moveTo(-100);steps(e,12);assertEquals(0,e.playerLane,.001);e.moveTo(100);steps(e,20);assertEquals(2,e.playerLane,.001);}
    @Test public void sameSeedProducesSameDailyCircuit(){GameEngine a=run(),b=run();for(int t=0;t<6000;t++){a.invulnerable=b.invulnerable=1;a.update(GameConfig.STEP);b.update(GameConfig.STEP);for(int i=0;i<a.entities.length;i++){assertEquals(a.entities[i].active,b.entities[i].active);assertEquals(a.entities[i].lane,b.entities[i].lane);assertEquals(a.entities[i].y,b.entities[i].y,0);}}}
    @Test public void longSimulationStaysBoundedAndEachWallRowHasAGap(){GameEngine e=run();for(int t=0;t<36000;t++){e.invulnerable=1;e.update(GameConfig.STEP);assertTrue(e.speed<=GameConfig.MAX_SPEED);if(t%60==0)for(GameEngine.Entity a:e.entities){if(!a.active||a.type!=0)continue;int walls=0;for(GameEngine.Entity b:e.entities)if(b.active&&b.type==0&&Math.abs(a.y-b.y)<.00001f)walls++;assertTrue(walls<3);}}assertTrue(e.alive);}
    @Test public void restartClearsRunEffects(){GameEngine e=run();e.power[0]=8;e.points=900;e.start(false);assertEquals(0,e.score());assertEquals(0,e.power[0],0);assertEquals(1,e.playerLane,0);}
}

