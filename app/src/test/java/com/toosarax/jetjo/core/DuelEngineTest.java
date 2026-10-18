package com.toosarax.jetjo.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class DuelEngineTest {
    @Test public void teamTargetIsReachableOnActualSeededRoutes() {
        for(long seed=1;seed<=20;seed++){
            DuelEngine duel=new DuelEngine(seed,true,true);
            for(int tick=0;tick<5000&&!duel.finished();tick++){
                for(GameEngine run:duel.players){
                    GameEngine.Entity front=null;
                    for(GameEngine.Entity entity:run.entities)if(entity.active&&entity.type==GameEngine.WALL&&entity.y<GameConfig.PLAYER_Y+.055f&&(front==null||entity.y>front.y))front=entity;
                    if(front==null)continue;
                    int blocked=0,marked=-1;
                    for(GameEngine.Entity entity:run.entities)if(entity.active&&Math.abs(entity.y-front.y)<.015f){if(entity.type==GameEngine.WALL)blocked|=1<<entity.lane;if(entity.type==GameEngine.ENERGY)marked=entity.lane;}
                    if(marked>=0&&(blocked&(1<<marked))==0)run.moveTo(marked);
                    else if((blocked&(1<<run.targetLane))!=0)for(int lane=0;lane<3;lane++)if((blocked&(1<<lane))==0){run.moveTo(lane);break;}
                }
                duel.update(GameConfig.STEP);
            }
            assertTrue("Unreachable team target for seed "+seed+": "+duel.combinedEnergy(),duel.teamWon());
        }
    }
    @Test public void identicalInputsGiveIdenticalRuns() {
        DuelEngine duel=new DuelEngine(4512,false,false);
        for(int tick=0;tick<4600;tick++) {
            if(tick%71==0){int lane=(tick/71)%3;duel.move(0,lane);duel.move(1,lane);}
            duel.update(GameConfig.STEP);
        }
        assertTrue(duel.finished());assertEquals(-1,duel.winner());
        assertEquals(duel.players[0].energy,duel.players[1].energy);
        assertEquals(75,duel.players[0].seconds,0);
        assertEquals(duel.players[0].score(),duel.players[1].score());
    }
    @Test public void pauseStopsClockAndInputs() {
        DuelEngine duel=new DuelEngine(72,false,true);
        duel.update(GameConfig.STEP);float before=duel.players[0].seconds;
        duel.setPaused(true);duel.move(0,0);
        for(int i=0;i<1000;i++)duel.update(GameConfig.STEP);
        assertEquals(before,duel.players[0].seconds,0);assertEquals(1,duel.players[0].targetLane);
        duel.setPaused(false);duel.move(0,0);duel.update(GameConfig.STEP);
        assertTrue(duel.players[0].seconds>before);assertEquals(0,duel.players[0].targetLane);
    }
    @Test public void onePlayersInputDoesNotMoveTheOther() {
        DuelEngine duel=new DuelEngine(22,false,false);
        duel.move(0,2);duel.move(-1,0);duel.move(2,0);
        assertEquals(2,duel.players[0].targetLane);assertEquals(1,duel.players[1].targetLane);
        assertNotSame(duel.players[0].entities[0],duel.players[1].entities[0]);
        duel.players[0].points=50;assertEquals(0,duel.players[1].score());
    }
    @Test public void teamGoalRequiresFinishedMatchAndBothPlayersContribute() {
        DuelEngine duel=new DuelEngine(12,true,false);
        duel.players[0].energy=40;duel.players[1].energy=40;
        assertFalse(duel.teamWon());
        duel.players[0].alive=duel.players[1].alive=false;
        assertTrue(duel.teamWon());duel.players[1].energy=39;assertFalse(duel.teamWon());
    }
    @Test public void finishedMatchIsFrozenAndTieIsDistinct() {
        DuelEngine duel=new DuelEngine(5,false,false);
        assertEquals(-2,duel.winner());duel.players[0].alive=duel.players[1].alive=false;
        duel.players[0].points=500;duel.players[1].points=200;
        assertEquals(0,duel.winner());float time=duel.players[0].seconds;
        duel.update(GameConfig.STEP);duel.move(0,0);
        assertEquals(time,duel.players[0].seconds,0);assertEquals(1,duel.players[0].targetLane);
        duel.players[1].points=500;assertEquals(-1,duel.winner());
    }
}
