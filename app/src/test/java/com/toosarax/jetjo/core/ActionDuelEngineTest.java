package com.toosarax.jetjo.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ActionDuelEngineTest {
    private DuelEngine start(boolean cooperative,boolean gentle){return new DuelEngine(927,cooperative,gentle,true);}
    private boolean hazard(int type){return type==GameEngine.WALL||type==GameEngine.JUMP_BARRIER||type==GameEngine.DUCK_BEAM;}
    /** Act on the nearest obstacle's arrival, never on an upcoming row that has not arrived. */
    private void skillInput(DuelEngine duel,int player){
        GameEngine run=duel.players[player];GameEngine.Entity front=null;
        for(GameEngine.Entity item:run.entities)if(item.active&&hazard(item.type)&&item.y<GameConfig.PLAYER_Y+.055f&&(front==null||item.y>front.y))front=item;
        if(front==null)return;int blocked=0,actionLane=-1,actionType=-1;
        for(GameEngine.Entity item:run.entities)if(item.active&&hazard(item.type)&&Math.abs(item.y-front.y)<.015f){
            blocked|=1<<item.lane;if(item.type!=GameEngine.WALL){actionLane=item.lane;actionType=item.type;}
        }
        int safe=-1;for(int lane=0;lane<3;lane++)if((blocked&(1<<lane))==0&&(safe<0||Math.abs(lane-run.playerLane)<Math.abs(safe-run.playerLane)))safe=lane;
        if(actionLane>=0){
            float velocity=run.speed*(run.power[2]>0?.55f:1);
            float arrival=(GameConfig.PLAYER_Y-.045f-front.y)/velocity;
            if(run.overdrive>0||run.jumpTime>0||run.duckTime>0){duel.move(player,actionLane);return;}
            if(arrival>0&&arrival<=.36f&&(actionType==GameEngine.JUMP_BARRIER?duel.jump(player):duel.duck(player))){duel.move(player,actionLane);return;}
        }
        if(safe>=0)duel.move(player,safe);
    }
    private void finish(DuelEngine duel){for(int tick=0;tick<6200&&!duel.finished();tick++)duel.update(GameConfig.STEP);}

    @Test public void oldConstructorRetainsTheOriginalSeventyFiveSecondRace(){
        DuelEngine old=new DuelEngine(927,false,false);assertFalse(old.actionFlight);
        for(GameEngine player:old.players){assertEquals(GameModes.COIN_RUSH,player.mode);assertFalse(player.partyMode);}
        assertEquals(75,old.remainingSeconds(),0);assertFalse(old.jump(0));assertFalse(old.duck(1));assertFalse(old.boost(0));
        finish(old);assertTrue(old.finished());assertEquals(75,old.players[0].seconds,0);
    }
    @Test public void actionConstructorEnablesTwoIsolatedHundredSecondRuns(){
        DuelEngine duel=start(false,false);assertTrue(duel.actionFlight);assertEquals(100,duel.remainingSeconds(),0);
        for(GameEngine player:duel.players){assertEquals(GameModes.STUNT,player.mode);assertTrue(player.partyMode);assertEquals(0,player.lives);}
        assertNotSame(duel.players[0].entities,duel.players[1].entities);assertNotSame(duel.players[0].power,duel.players[1].power);
    }
    @Test public void eachPlayersActionsAndFuelAreIndependent(){
        DuelEngine duel=start(false,false);assertTrue(duel.jump(0));assertEquals(GameConfig.JUMP_SECONDS,duel.players[0].jumpTime,0);assertEquals(0,duel.players[1].jumpTime,0);
        assertTrue(duel.duck(1));assertEquals(0,duel.players[0].duckTime,0);assertEquals(GameConfig.DUCK_SECONDS,duel.players[1].duckTime,0);
        assertTrue(duel.boost(0));assertEquals(.5f,duel.players[1].boostFuel,0);assertEquals(0,duel.players[1].overdrive,0);
        assertFalse(duel.jump(-1));assertFalse(duel.duck(2));assertFalse(duel.boost(Integer.MAX_VALUE));
    }
    @Test public void pauseFreezesActionsFuelClockAndInputs(){
        DuelEngine duel=start(false,false);duel.jump(0);duel.update(GameConfig.STEP);duel.setPaused(true);
        float clock=duel.players[0].seconds,jump=duel.players[0].jumpTime,fuel=duel.players[0].boostFuel;
        assertFalse(duel.jump(1));assertFalse(duel.duck(1));assertFalse(duel.boost(1));duel.move(1,0);
        for(int tick=0;tick<180;tick++)duel.update(GameConfig.STEP);
        assertEquals(clock,duel.players[0].seconds,0);assertEquals(jump,duel.players[0].jumpTime,0);assertEquals(fuel,duel.players[0].boostFuel,0);assertEquals(1,duel.players[1].targetLane);
        duel.setPaused(false);assertTrue(duel.duck(1));duel.update(GameConfig.STEP);assertTrue(duel.players[0].seconds>clock);
    }
    @Test public void repeatedCollisionsPenalizeScoreWithoutEliminatingEitherPlayer(){
        DuelEngine duel=start(false,false);GameEngine run=duel.players[0];run.points=550;run.combo=12;
        for(int hit=1;hit<=5;hit++){
            run.invulnerable=0;run.spawn(hit%2==0?GameEngine.JUMP_BARRIER:GameEngine.DUCK_BEAM,1,GameConfig.PLAYER_Y);duel.update(GameConfig.STEP);
            assertTrue(run.alive);assertEquals(hit,run.hits);assertEquals(0,run.combo);assertEquals(0,run.lives);assertTrue(run.score()>=0);
        }
        assertEquals(51,run.score(),1);assertEquals(0,duel.players[1].hits);assertFalse(duel.finished());
    }
    @Test public void simultaneousPartyHazardsUseGraceAndShieldStillWorks(){
        DuelEngine duel=start(false,false);GameEngine run=duel.players[0];run.points=300;
        for(int i=0;i<8;i++)run.spawn(GameEngine.JUMP_BARRIER,1,GameConfig.PLAYER_Y);
        duel.update(GameConfig.STEP);assertEquals(1,run.hits);assertTrue(run.score()>=200);
        run.invulnerable=0;run.power[0]=1;run.spawn(GameEngine.WALL,1,GameConfig.PLAYER_Y);duel.update(GameConfig.STEP);
        assertEquals(1,run.hits);assertEquals(0,run.power[0],0);assertTrue(run.alive);
    }
    @Test public void soloActionFlightStillUsesHeartsAndEndsAfterThreeHits(){
        GameEngine run=new GameEngine(927);run.mode=GameModes.STUNT;run.start(false);assertFalse(run.partyMode);
        for(int hit=0;hit<3;hit++){run.invulnerable=0;run.spawn(GameEngine.JUMP_BARRIER,1,GameConfig.PLAYER_Y);run.update(GameConfig.STEP);}
        assertFalse(run.alive);assertFalse(run.won);assertEquals(0,run.lives);assertEquals(3,run.hits);
    }
    @Test public void identicalInputsKeepActionRoutesAndFinalScoresIdentical(){
        DuelEngine duel=start(false,false);
        for(int tick=0;tick<6200&&!duel.finished();tick++){
            if(tick%180==0){duel.jump(0);duel.jump(1);}if(tick%180==90){duel.duck(0);duel.duck(1);}if(tick%300==0){duel.boost(0);duel.boost(1);}
            if(tick%73==0){int lane=(tick/73)%3;duel.move(0,lane);duel.move(1,lane);}duel.update(GameConfig.STEP);
            assertEquals(duel.players[0].score(),duel.players[1].score());assertEquals(duel.players[0].bossesDefeated,duel.players[1].bossesDefeated);
            for(int i=0;i<GameConfig.MAX_ENTITIES;i++){GameEngine.Entity a=duel.players[0].entities[i],b=duel.players[1].entities[i];assertEquals(a.active,b.active);if(a.active){assertEquals(a.type,b.type);assertEquals(a.lane,b.lane);assertEquals(a.y,b.y,0);}}
        }
        assertTrue(duel.finished());assertEquals(-1,duel.winner());assertEquals(100,duel.players[0].seconds,0);assertEquals(0,duel.remainingSeconds(),0);
    }
    @Test public void timedFinishEmitsOneEventPerPlayerAndFreezesAllActions(){
        DuelEngine duel=start(false,false);int[] completed={0,0};
        duel.players[0].listener=(event,detail,lane,y)->{if(event==GameEngine.COMPLETE)completed[0]++;};duel.players[1].listener=(event,detail,lane,y)->{if(event==GameEngine.COMPLETE)completed[1]++;};
        finish(duel);assertArrayEquals(new int[]{1,1},completed);long score=duel.players[0].score();float fuel=duel.players[0].boostFuel;
        assertFalse(duel.jump(0));assertFalse(duel.duck(1));assertFalse(duel.boost(0));duel.move(0,0);for(int i=0;i<100;i++)duel.update(GameConfig.STEP);
        assertEquals(score,duel.players[0].score());assertEquals(fuel,duel.players[0].boostFuel,0);assertArrayEquals(new int[]{1,1},completed);
    }
    @Test public void actionCooperationRequiresFourDefeatedGuardiansInsteadOfEnergy(){
        DuelEngine duel=start(true,false);duel.players[0].energy=duel.players[1].energy=999;
        duel.players[0].bossesDefeated=2;duel.players[1].bossesDefeated=2;assertFalse(duel.teamWon());
        duel.players[0].alive=duel.players[1].alive=false;assertTrue(duel.teamWon());assertEquals(4,duel.combinedBosses());
        duel.players[1].bossesDefeated=1;assertFalse(duel.teamWon());duel.players[0].points=100;duel.players[1].points=300;assertEquals(1,duel.winner());
    }
    @Test public void cooperationIsReachableThroughRealSkillPlayOnTwentySeededRoutes(){
        for(boolean gentle:new boolean[]{false,true})for(long seed=1;seed<=20;seed++){
            DuelEngine duel=new DuelEngine(seed,true,gentle,true);
            for(int tick=0;tick<6200&&!duel.finished();tick++){
                skillInput(duel,0);skillInput(duel,1);duel.update(GameConfig.STEP);
                for(GameEngine run:duel.players)for(GameEngine.Entity a:run.entities)if(a.active&&hazard(a.type)){
                    int blocked=0;for(GameEngine.Entity b:run.entities)if(b.active&&hazard(b.type)&&Math.abs(a.y-b.y)<.091f)blocked|=1<<b.lane;
                    assertNotEquals("Closed row seed="+seed+" time="+run.seconds,7,blocked);
                }
            }
            assertTrue("Failed team seed="+seed+" gentle="+gentle+" guardians="+duel.combinedBosses(),duel.teamWon());
            assertEquals(6,duel.combinedBosses());assertEquals(0,duel.players[0].hits);assertEquals(0,duel.players[1].hits);assertEquals(-1,duel.winner());
        }
    }
}
