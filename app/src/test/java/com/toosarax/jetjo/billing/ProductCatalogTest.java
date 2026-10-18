package com.toosarax.jetjo.billing;
import org.junit.Test;
import static org.junit.Assert.*;
import com.toosarax.jetjo.data.Catalog;
import com.toosarax.jetjo.data.Cosmetics;
import java.util.HashSet;
public class ProductCatalogTest {
    @Test public void productsUnique(){assertEquals(3,new HashSet<>(Products.IDS).size());}
    @Test public void thirtySixCharactersHaveDistinctNamesAndColors(){
        assertEquals(36,Catalog.SKINS.length); assertEquals(Catalog.SKINS.length,Catalog.COLORS.length);
        HashSet<String> names=new HashSet<>();
        for(String[] skin:Catalog.SKINS){assertEquals(2,skin.length);assertFalse(skin[1].isEmpty());assertTrue(names.add(skin[0]));}
    }
    @Test public void earnedShipsDoNotRequirePurchase(){for(int i=0;i<Catalog.SKINS.length;i++)if(i<10||i>=18)assertEquals(-1,Products.packForSkin(i));}
    @Test public void eachPackDeliversAdvertisedShipCount(){int[] count=new int[3];for(int i=10;i<18;i++)count[Products.packForSkin(i)]++;assertArrayEquals(new int[]{3,3,2},count);}
    @Test public void cosmeticsHaveMatchingProgressionThresholds(){
        assertEquals(12,Cosmetics.TRAILS.length); assertEquals(Cosmetics.TRAILS.length,Catalog.TRAIL_LEVELS.length);
        assertEquals(8,Cosmetics.BADGES.length); assertEquals(Cosmetics.BADGES.length,Catalog.BADGE_STARS.length);
        assertEquals(16,Catalog.SCENE_LEVELS.length);
        assertEquals(32,Catalog.ACHIEVEMENTS.length); assertEquals(Catalog.ACHIEVEMENTS.length,Catalog.ACH_DESC.length);
    }
}

