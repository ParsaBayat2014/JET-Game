package com.toosarax.jetjo.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProgressionTest {
    @Test public void thresholdsHaveCorrectBoundary(){for(int l=2;l<=100;l++){assertEquals(l-1,Progression.level(Progression.threshold(l)-1));assertEquals(l,Progression.level(Progression.threshold(l)));}}
    @Test public void xpIsMonotonic(){assertTrue(Progression.rewardXp(1000,10,30)>Progression.rewardXp(500,5,15));}
    @Test public void negativeXpDoesNotCreateNegativeLevel(){assertEquals(1,Progression.level(-10));}
    @Test public void contentUnlocksAtAdvertisedLevels(){assertEquals(1,Progression.zoneCount(2));assertEquals(2,Progression.zoneCount(3));assertEquals(3,Progression.zoneCount(7));}
    @Test public void everyCharacterHasAnExplicitUnlockPolicy(){
        assertEquals(36,Progression.SKIN_LEVELS.length); assertEquals(36,Progression.SKIN_COSTS.length);
        for(int i=0;i<36;i++){
            assertEquals(i>=10&&i<18,Progression.isPaidSkin(i));
            if(i>=18){assertTrue(Progression.skinLevel(i)>=3);assertTrue(Progression.skinCost(i)>0);}
        }
        assertFalse(Progression.isPaidSkin(-1)); assertFalse(Progression.isPaidSkin(36));
        assertEquals(Integer.MAX_VALUE,Progression.skinLevel(36)); assertEquals(Integer.MAX_VALUE,Progression.skinCost(-1));
    }
    @Test public void originalEarnedUnlocksStayStable(){
        assertArrayEquals(new int[]{1,1,2,3,4,5,7,9,12,15},java.util.Arrays.copyOf(Progression.SKIN_LEVELS,10));
        assertArrayEquals(new int[]{0,0,80,140,220,320,460,620,800,1000},java.util.Arrays.copyOf(Progression.SKIN_COSTS,10));
        for(int i=19;i<36;i++){
            assertTrue(Progression.skinLevel(i)>Progression.skinLevel(i-1));
            assertTrue(Progression.skinCost(i)>Progression.skinCost(i-1));
        }
    }
}

