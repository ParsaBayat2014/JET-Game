package com.toosarax.jetjo.multiplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class DuelMatchTest {
    @Test public void identicalInputProducesEqualScoreAndFullRace() {
        DuelMatch match = new DuelMatch(700, 0);
        for (int i = 0; i < 5000; i++) match.step((i / 80) % 3, (i / 80) % 3, false);
        assertTrue(match.finished()); LanProtocol.Snapshot state = match.snapshot(); assertEquals(75, state.seconds, 0);
        assertEquals(state.pilots[0].score, state.pilots[1].score); assertEquals(state.pilots[0].hits, state.pilots[1].hits);
        int finalTick = state.tick; for (int i = 0; i < 30; i++) match.step(0, 2, false); assertEquals(finalTick, match.snapshot().tick);
    }
    @Test public void pauseFreezesBothTracksAndClock() {
        DuelMatch match = new DuelMatch(5, 3); for (int i = 0; i < 180; i++) match.step(0, 2, false);
        LanProtocol.Snapshot before = match.snapshot(); for (int i = 0; i < 60; i++) match.step(1, 1, true);
        LanProtocol.Snapshot paused = match.snapshot(); assertTrue(paused.paused); assertEquals(before.tick, paused.tick); assertEquals(before.seconds, paused.seconds, 0);
        assertEquals(before.pilots[0].score, paused.pilots[0].score); assertEquals(before.pilots[1].score, paused.pilots[1].score);
        match.step(1, 1, false); assertTrue(match.snapshot().seconds > paused.seconds); assertFalse(match.snapshot().paused);
    }
    @Test public void differentInputsControlIndependentShips() {
        DuelMatch match = new DuelMatch(5, 0); for (int i = 0; i < 30; i++) match.step(0, 2, false);
        assertEquals(0, match.snapshot().pilots[0].lane, .001); assertEquals(2, match.snapshot().pilots[1].lane, .001);
    }
}
