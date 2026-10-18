package com.toosarax.jetjo.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class StageCatalogTest {
    @Test public void campaignHasTwelveFreeWorldsWithFourMissionsEach() {
        assertEquals(48, StageCatalog.COUNT); assertEquals(12, StageCatalog.WORLD_COUNT);
        for (int i = 0; i < StageCatalog.COUNT; i++) {
            StageCatalog.Stage s = StageCatalog.get(i);
            assertEquals(i, s.index); assertEquals(i / 4, s.world); assertEquals(i % 4 + 1, s.number);
            assertTrue(s.duration >= 35 && s.duration <= 79); assertTrue(s.target > 0);
            assertTrue(s.difficulty >= 1 && s.difficulty <= 1.35f);
            assertFalse(s.title(true).isEmpty()); assertFalse(s.worldName(false).isEmpty()); assertFalse(s.objectiveName(true).isEmpty());
        }
        assertEquals(0, StageCatalog.get(-50).index); assertEquals(47, StageCatalog.get(100).index);
    }
    @Test public void stageSeedsAreStableAndDistinct() {
        for (int i = 0; i < StageCatalog.COUNT; i++) for (int j = i + 1; j < StageCatalog.COUNT; j++)
            assertNotEquals(StageCatalog.get(i).seed, StageCatalog.get(j).seed);
    }
    @Test public void stableModeIdsIncludeOnlySupportedModes() {
        for (int i = 0; i < GameModes.COUNT; i++) { assertTrue(GameModes.valid(i)); assertFalse(GameModes.name(i, true).isEmpty()); }
        assertFalse(GameModes.valid(-1)); assertFalse(GameModes.valid(GameModes.COUNT));
    }
    @Test public void newExpeditionsHaveSpecificNamedGoals() {
        java.util.HashSet<String> names = new java.util.HashSet<>();
        java.util.HashSet<Integer> goals = new java.util.HashSet<>();
        for (int i = 24; i < StageCatalog.COUNT; i++) {
            StageCatalog.Stage s = StageCatalog.get(i);
            assertTrue(names.add(s.titleEn)); assertFalse(s.titleFa.isEmpty()); goals.add(s.goal);
            if (s.goal == StageCatalog.SURVIVE) assertEquals((int)s.duration,s.target);
        }
        assertTrue(goals.contains(StageCatalog.POWER)); assertTrue(goals.contains(StageCatalog.ENERGY));
        assertTrue(goals.contains(StageCatalog.SURVIVE));
    }
    @Test public void originalCampaignConfigurationsStayStable() {
        for (int i = 0; i < 24; i++) {
            StageCatalog.Stage s = StageCatalog.get(i); int number=i%4+1, world=i/4;
            assertEquals(i==0?35:40+world*4+(number-1)*3,s.duration,0);
            assertEquals(1+world*.065f,s.difficulty,0);
            assertEquals(0x4E454F4EL+i*341873128712L,s.seed);
            assertEquals(i==0?StageCatalog.SURVIVE:number==1?StageCatalog.ENERGY:number==2?StageCatalog.NEAR:number==3?StageCatalog.COMBO:StageCatalog.OVERDRIVE,s.goal);
        }
    }
}
