package com.toosarax.jetjo.game;

import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.core.GameModes;
import com.toosarax.jetjo.multiplayer.DuelMatch;
import com.toosarax.jetjo.multiplayer.LanProtocol;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlightSnapshotTest {
    @Test public void publicationDetachesEntitiesAndPowersFromLiveSimulation() {
        GameEngine engine = new GameEngine(4); engine.start(false); engine.spawn(GameEngine.WALL, 0, .2f); engine.power[0] = 4;
        FlightSnapshot frame = FlightSnapshot.capture(engine, 18, 2, 11, false, true, 3);
        assertEquals(1, frame.count); assertEquals(GameEngine.WALL, frame.type[0]); assertEquals(.2f, frame.y[0], 0); assertEquals(4, frame.powers[0], 0);
        engine.entities[0].active = false; engine.entities[0].y = .9f; engine.power[0] = 0; engine.playerLane = 2; engine.start(false);
        assertEquals(1, frame.count); assertEquals(.2f, frame.y[0], 0); assertEquals(4, frame.powers[0], 0); assertEquals(1, frame.playerLane, 0);
        assertEquals(18, frame.skin); assertEquals(2, frame.trail); assertEquals(3, frame.world); assertEquals(11, frame.clock, 0); assertTrue(frame.highContrast);
    }
    @Test public void reusedEngineCannotMutatePriorSnapshotsAndSnapshotCannotMutateEngine() {
        GameEngine engine = new GameEngine(10); engine.start(false); engine.spawn(GameEngine.ENERGY, 1, .2f);
        FlightSnapshot first = FlightSnapshot.capture(engine, 1, 0, 0, false, false); engine.entities[0].y = .4f;
        FlightSnapshot second = FlightSnapshot.capture(engine, 1, 0, 1, false, false);
        first.y[0] = .7f; first.powers[0] = 100;
        assertEquals(.4f, second.y[0], 0); assertEquals(0, engine.power[0], 0); assertEquals(.4f, engine.entities[0].y, 0); assertNotSame(first.y, second.y);
    }
    @Test public void entitySnapshotStaysWithinFixedPoolAndPreservesStuntTypes() {
        GameEngine engine = new GameEngine(15); engine.mode = GameModes.STUNT; engine.start(false);
        for (int i = 0; i < GameConfig.MAX_ENTITIES + 10; i++) engine.spawn(GameEngine.JUMP_BARRIER + i % 3, i % 3, -.2f + i * .004f);
        FlightSnapshot frame = FlightSnapshot.capture(engine, 0, 0, 0, true, false);
        assertEquals(GameConfig.MAX_ENTITIES, frame.count); assertEquals(frame.count, frame.type.length); assertEquals(frame.count, frame.lane.length); assertEquals(frame.count, frame.style.length); assertEquals(frame.count, frame.pattern.length);
        for (int i = 0; i < frame.count; i++) { assertTrue(frame.type[i] >= GameEngine.JUMP_BARRIER && frame.type[i] <= GameEngine.BOOST_RING); assertTrue(Float.isFinite(frame.y[i])); }
    }
    @Test public void actionAndBossStateAreCapturedInOneFrame() {
        GameEngine engine = new GameEngine(1); engine.mode = GameModes.STUNT; engine.start(false); assertTrue(engine.jump()); engine.update(.05f);
        engine.bossActive = true; engine.bossPhase = 2; engine.bossHealth = .4f; engine.bossWarning = .8f; engine.boostFuel = .6f;
        FlightSnapshot frame = FlightSnapshot.capture(engine, 35, 0, 5, false, false);
        assertEquals(engine.jumpHeight(), frame.jumpHeight, 0); assertTrue(frame.jumpHeight > 0); assertEquals(.6f, frame.boostFuel, 0);
        assertTrue(frame.bossActive); assertEquals(2, frame.bossPhase); assertEquals(.4f, frame.bossHealth, 0); assertEquals(.8f, frame.bossWarning, 0);
        engine.bossActive = false; engine.bossHealth = 0; engine.jumpTime = 0; assertTrue(frame.bossActive); assertTrue(frame.jumpHeight > 0);
    }
    @Test public void lanPilotCanRenderWithoutReferencingOrChangingProtocolData() {
        DuelMatch match = new DuelMatch(2, 7, 18, 35); for (int i = 0; i < 300; i++) match.step(0, 2, false);
        LanProtocol.Pilot pilot = match.snapshot().pilots[1]; FlightSnapshot frame = FlightSnapshot.fromPilot(pilot, 5, 7, true, true);
        assertEquals(35, frame.skin); assertEquals(pilot.lane, frame.playerLane, 0); assertEquals(pilot.items.length, frame.count); assertEquals(7, frame.world);
        assertTrue(frame.reducedMotion); assertTrue(frame.highContrast); assertFalse(frame.bossActive); assertEquals(0, frame.jumpHeight, 0);
        frame.powers[0] = 100; assertTrue(pilot.powers[0] < 100);
        for (int i = 0; i < frame.count; i++) { assertEquals(pilot.items[i].type, frame.type[i]); assertEquals(pilot.items[i].lane, frame.lane[i]); assertEquals(pilot.items[i].y, frame.y[i], 0); }
    }
}
