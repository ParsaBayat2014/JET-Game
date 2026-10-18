package com.toosarax.jetjo.data;

import android.content.Context;
import android.util.AtomicFile;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.core.GameModes;
import com.toosarax.jetjo.core.StageCatalog;
import com.toosarax.jetjo.core.Progression;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Versioned atomic local profile. Purchase receipts are kept in a separate store. */
public final class Profile {
    private final AtomicFile file;
    private byte[] legacyProfileBytes;
    private final java.util.Map<GameEngine,Long> recorded=new java.util.WeakHashMap<>();
    public long high,xp,totalEnergy,totalPassed,totalPowers,sprintHigh,dailyHigh,rushHigh,stuntHigh;
    public int runs,bestCombo,coins,skin,zone,totalNear,totalOverdrives,trail,badge,musicTrack,duelRuns,duelWins,coopWins,totalStunts,guardiansDefeated;
    public float longest;
    public boolean render3d=true,tutorial,music=true,sound=true,haptic=true,reducedMotion,relaxed,highContrast;
    public String language=Locale.getDefault().getLanguage().equals("fa")?"fa":"en",day="",challengeDate="",storageError="";
    public final boolean[] owned=new boolean[Catalog.SKINS.length],achievements=new boolean[Catalog.ACHIEVEMENTS.length],claimed=new boolean[5];
    public final long[] daily=new long[5];
    public final int[] stars=new int[StageCatalog.COUNT];
    public long totalSeconds;
    public Profile(Context context) {
        file=new AtomicFile(new File(context.getFilesDir(),"profile-v1.json"));
        owned[0]=owned[1]=true;load();boolean dayChanged=resetDay();if((dayChanged||legacyProfileBytes!=null)&&!storageError.equals("read"))save();
    }
    public int level(){return Progression.level(xp);}
    public int ownedCount(){int n=0;for(int i=0;i<owned.length;i++)if(!Progression.isPaidSkin(i)&&owned[i])n++;return n;}
    public int newOwnedCount(){int n=0;for(int i=18;i<owned.length;i++)if(owned[i])n++;return n;}
    public int originalOwnedCount(){int n=0;for(int i=0;i<10;i++)if(owned[i])n++;return n;}
    public int achievementCount(){int n=0;for(boolean b:achievements)if(b)n++;return n;}
    public int starCount(){int n=0;for(int s:stars)n+=s;return n;}
    public int clearedStages(){int n=0;for(int s:stars)if(s>0)n++;return n;}
    public boolean stageAvailable(int index){return index>=0&&index<stars.length&&(index==0||stars[index-1]>0);}
    public boolean trailAvailable(int index){return index>=0&&index<Catalog.TRAIL_LEVELS.length&&level()>=Catalog.TRAIL_LEVELS[index];}
    public boolean sceneAvailable(int index){return index>=0&&index<Catalog.SCENE_LEVELS.length&&level()>=Catalog.SCENE_LEVELS[index];}
    public boolean badgeAvailable(int index){return index>=0&&index<Catalog.BADGE_STARS.length&&starCount()>=Catalog.BADGE_STARS[index];}
    public long best(int mode){return mode==GameModes.STUNT?stuntHigh:mode==GameModes.SPRINT?sprintHigh:mode==GameModes.DAILY?dailyHigh:mode==GameModes.COIN_RUSH?rushHigh:high;}
    public boolean resetDay(){
        String today=utcDate(System.currentTimeMillis());
        if(day.isEmpty()||today.compareTo(day)>0){
            day=today;for(int i=0;i<5;i++){daily[i]=0;claimed[i]=false;}
            if(!challengeDate.equals(today)){challengeDate=today;dailyHigh=0;}
            return true;
        }return false;
    }
    public int target(int i){return Catalog.MISSION_BASE[i]+Math.floorMod(day.hashCode()+i,3)*Catalog.MISSION_BASE[i]/4;}
    public int reward(int i){return 40+i*15;}
    public boolean claim(int i){
        resetDay();if(i<0||i>=5||claimed[i]||daily[i]<target(i))return false;
        int oldCoins=coins;long oldXp=xp;boolean[] oldAchievements=achievements.clone();
        claimed[i]=true;coins+=reward(i);xp+=reward(i);checkAchievements();
        if(save())return true;
        claimed[i]=false;coins=oldCoins;xp=oldXp;System.arraycopy(oldAchievements,0,achievements,0,achievements.length);return false;
    }
    public boolean unlock(int i){
        if(i<0||i>=owned.length||Progression.isPaidSkin(i)||owned[i]||level()<Progression.skinLevel(i)||coins<Progression.skinCost(i))return false;
        int oldCoins=coins;boolean[] oldAchievements=achievements.clone();
        coins-=Progression.skinCost(i);owned[i]=true;checkAchievements();
        if(save())return true;
        coins=oldCoins;owned[i]=false;System.arraycopy(oldAchievements,0,achievements,0,achievements.length);return false;
    }
    public int record(GameEngine run){
        if(run==null||run.alive||run.tutorial||run.partyMode||run.mode==GameModes.ZEN)return 0;
        Long previous=recorded.get(run);if(previous!=null&&previous==run.runSequence())return 0;
        recorded.put(run,run.runSequence());
        resetDay();runs++;
        if(run.mode==GameModes.STUNT){stuntHigh=Math.max(stuntHigh,run.score());totalStunts+=run.stunts;guardiansDefeated+=run.bossesDefeated;}
        if(run.mode==GameModes.SURVIVAL)high=Math.max(high,run.score());
        if(run.mode==GameModes.SPRINT)sprintHigh=Math.max(sprintHigh,run.score());
        if(run.mode==GameModes.COIN_RUSH)rushHigh=Math.max(rushHigh,run.score());
        if(run.mode==GameModes.DAILY){challengeDate=day;dailyHigh=Math.max(dailyHigh,run.score());}
        int bonus=0;
        if(run.mode==GameModes.CAMPAIGN&&run.won){
            int index=Math.max(0,Math.min(stars.length-1,run.stageIndex));
            int earnedStars=run.stageStars();
            bonus=Math.max(0,earnedStars-stars[index])*30;
            stars[index]=Math.max(stars[index],earnedStars);
        }
        totalNear+=run.nearMisses;totalOverdrives+=run.overdriveActivations;totalSeconds+=(long)run.seconds;
        int earned=Progression.rewardXp(run.score(),run.energy,run.seconds)+bonus;
        xp+=earned;coins+=run.energy+(int)(run.score()/200)+bonus;
        totalEnergy+=run.energy;totalPassed+=run.passed;totalPowers+=run.powers;
        bestCombo=Math.max(bestCombo,run.bestCombo);longest=Math.max(longest,run.seconds);
        daily[0]=Math.max(daily[0],run.score());daily[1]+=(int)run.seconds;daily[2]+=run.energy;daily[3]+=run.powers;daily[4]+=run.passed;
        checkAchievements();save();return earned;
    }
    public void checkAchievements(){
        boolean[] a={runs>=1,runs>=10,runs>=50,totalPassed>=100,totalEnergy>=100,totalEnergy>=1000,bestCombo>=10,bestCombo>=25,Math.max(stuntHigh,Math.max(rushHigh,Math.max(high,Math.max(sprintHigh,dailyHigh))))>=5000,longest>=60,longest>=120,totalPowers>=20,ownedCount()>=4,originalOwnedCount()>=10,level()>=5,level()>=10,
            clearedStages()>=1,clearedStages()>=8,clearedStages()>=24,starCount()>=36,totalNear>=50,totalOverdrives>=20,rushHigh>=3000,totalSeconds>=1800,newOwnedCount()>=1,newOwnedCount()>=8,ownedCount()>=28,clearedStages()>=32,clearedStages()>=StageCatalog.COUNT,starCount()>=StageCatalog.COUNT*3,level()>=25,totalPowers>=100};
        for(int i=0;i<a.length;i++)if(a[i]&&!achievements[i]){achievements[i]=true;coins+=25;}
    }
    public void reset(){
        high=xp=totalEnergy=totalPassed=totalPowers=sprintHigh=dailyHigh=rushHigh=stuntHigh=totalSeconds=0;
        runs=bestCombo=coins=skin=zone=trail=badge=totalNear=totalOverdrives=duelRuns=duelWins=coopWins=totalStunts=guardiansDefeated=0;storageError="";longest=0;tutorial=false;day="";challengeDate="";
        for(int i=0;i<owned.length;i++)owned[i]=i<2;
        for(int i=0;i<achievements.length;i++)achievements[i]=false;
        for(int i=0;i<stars.length;i++)stars[i]=0;
        resetDay();save();
    }
    private void load(){
        if(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists())return;
        try{
            byte[] source=file.readFully();
            JSONObject j=new JSONObject(new String(source,StandardCharsets.UTF_8));
            if(originalV1(j)){legacyProfileBytes=source.clone();j=migrateOriginalV1(j);}
            high=Math.max(0,j.optLong("high"));xp=Math.max(0,j.optLong("xp"));coins=Math.max(0,j.optInt("coins"));runs=Math.max(0,j.optInt("runs"));
            totalEnergy=Math.max(0,j.optLong("energy"));totalPassed=Math.max(0,j.optLong("passed"));totalPowers=Math.max(0,j.optLong("powers"));bestCombo=Math.max(0,j.optInt("combo"));longest=(float)j.optDouble("longest",0);if(!Float.isFinite(longest)||longest<0)longest=0;
            render3d=j.optBoolean("render3d",true);tutorial=j.optBoolean("tutorial");music=j.optBoolean("music",true);sound=j.optBoolean("sound",true);haptic=j.optBoolean("haptic",true);reducedMotion=j.optBoolean("reducedMotion");relaxed=j.optBoolean("relaxed");highContrast=j.optBoolean("highContrast");
            language=j.optString("language",language).equals("fa")?"fa":"en";day=j.optString("day","");challengeDate=j.optString("challengeDate","");
            readBools(j.optJSONArray("owned"),owned);for(int i=10;i<18;i++)owned[i]=false;owned[0]=owned[1]=true;readBools(j.optJSONArray("achievements"),achievements);readBools(j.optJSONArray("claimed"),claimed);
            JSONArray d=j.optJSONArray("daily");if(d!=null)for(int i=0;i<5;i++)daily[i]=Math.max(0,d.optLong(i));
            JSONArray stages=j.optJSONArray("stars");if(stages!=null)for(int i=0;i<stars.length;i++)stars[i]=Math.max(0,Math.min(3,stages.optInt(i)));
            skin=Math.max(0,Math.min(Catalog.SKINS.length-1,j.optInt("skin")));if(!Progression.isPaidSkin(skin)&&!owned[skin])skin=0;
            stuntHigh=Math.max(0,j.optLong("stuntHigh"));totalStunts=Math.max(0,j.optInt("totalStunts"));guardiansDefeated=Math.max(0,j.optInt("guardiansDefeated"));sprintHigh=Math.max(0,j.optLong("sprintHigh"));dailyHigh=Math.max(0,j.optLong("dailyHigh"));rushHigh=Math.max(0,j.optLong("rushHigh"));totalNear=Math.max(0,j.optInt("totalNear"));totalOverdrives=Math.max(0,j.optInt("overdrives"));totalSeconds=Math.max(0,j.optLong("totalSeconds"));
            zone=Math.max(0,Math.min(Catalog.SCENE_LEVELS.length-1,j.optInt("zone")));if(!sceneAvailable(zone))zone=0;
            trail=Math.max(0,Math.min(Catalog.TRAIL_LEVELS.length-1,j.optInt("trail")));if(!trailAvailable(trail))trail=0;
            badge=Math.max(0,Math.min(Catalog.BADGE_STARS.length-1,j.optInt("badge")));if(!badgeAvailable(badge))badge=0;
            musicTrack=Math.max(0,Math.min(com.toosarax.jetjo.game.SceneAssets.trackCount()-1,j.optInt("musicTrack")));
            duelRuns=Math.max(0,j.optInt("duelRuns"));duelWins=Math.max(0,Math.min(duelRuns,j.optInt("duelWins")));coopWins=Math.max(0,Math.min(duelRuns,j.optInt("coopWins")));
        }catch(Exception e){storageError="read";}
    }
    private static String utcDate(long millis){
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);format.setTimeZone(TimeZone.getTimeZone("UTC"));return format.format(new Date(millis));
    }
    private static boolean originalV1(JSONObject j){
        return j.optInt("schema",0)==1&&(j.has("best")||j.has("selectedSkin")||(j.has("skins")&&!j.has("owned")));
    }
    private static long oldNumber(JSONObject j,String key){return Math.max(0,Math.min(1_000_000_000L,j.optLong(key,0)));}
    /** Convert cumulative XP to the new curve without taking away a legacy pilot level. */
    private static long migratedXp(long oldXp){
        int level=1;long spent=0;
        while(level<100){long cost=140L+40L*(level-1);if(oldXp-spent<cost)break;spent+=cost;level++;}
        long base=Progression.threshold(level);
        if(level==100)return Math.max(base,oldXp);
        long next=Progression.threshold(level+1),cost=140L+40L*(level-1);
        return base+(next-base)*(oldXp-spent)/cost;
    }
    /** Published 1.0 used different keys and achievement meanings in the same private filename. */
    private static JSONObject migrateOriginalV1(JSONObject old) throws org.json.JSONException {
        JSONObject next=new JSONObject();long oldXp=oldNumber(old,"xp");
        next.put("schema",5);next.put("xp",migratedXp(oldXp));next.put("high",oldNumber(old,"best"));
        next.put("coins",oldNumber(old,"coins"));next.put("runs",oldNumber(old,"runs"));
        next.put("energy",oldNumber(old,"totalEnergy"));next.put("passed",oldNumber(old,"totalDodged"));
        next.put("powers",oldNumber(old,"totalPowers"));next.put("combo",oldNumber(old,"bestCombo"));
        double last=old.optDouble("lastSeconds",0);next.put("longest",Double.isFinite(last)?Math.max(0,Math.min(10_000_000,last)):0);
        for(String key:new String[]{"tutorial","reducedMotion"})next.put(key,old.optBoolean(key,false));
        for(String key:new String[]{"music","sound"})next.put(key,old.optBoolean(key,true));
        next.put("haptic",old.optBoolean("haptics",true));next.put("language",old.optString("language",Locale.getDefault().getLanguage().equals("fa")?"fa":"en"));
        long oldDay=old.optLong("day",Math.floorDiv(System.currentTimeMillis(),86_400_000L));
        next.put("day",utcDate(Math.multiplyExact(oldDay,86_400_000L)));next.put("challengeDate",next.getString("day"));
        boolean[] migratedOwned=new boolean[Catalog.SKINS.length];migratedOwned[0]=migratedOwned[1]=true;
        JSONArray oldSkins=old.optJSONArray("skins");for(int i=0;i<10;i++)if(oldSkins!=null)migratedOwned[i]=oldSkins.optBoolean(i,migratedOwned[i]);
        migratedOwned[0]=migratedOwned[1]=true;
        int oldLevel=Progression.level(migratedXp(oldXp));for(int i=2;i<=5;i++)if(oldLevel>=i)migratedOwned[i]=true;
        // Keep earned slots 0..9. A legacy name such as NOVA must never grant paid slot 12.
        next.put("owned",bools(migratedOwned));int selected=(int)oldNumber(old,"selectedSkin");
        next.put("skin",selected<10&&migratedOwned[selected]?selected:0);
        int environment=(int)oldNumber(old,"environment");next.put("zone",environment<3?environment:0);
        boolean[] migratedAchievements=new boolean[Catalog.ACHIEVEMENTS.length];JSONArray oldAchievements=old.optJSONArray("achievements");
        int[][] pairs={{0,0},{1,1},{2,2},{4,8},{6,4},{7,5},{10,6},{11,7},{12,12},{13,13},{14,14},{15,15}};
        if(oldAchievements!=null)for(int[] pair:pairs)if(oldAchievements.optBoolean(pair[0],false))migratedAchievements[pair[1]]=true;
        next.put("achievements",bools(migratedAchievements));
        boolean[] oldClaimed=new boolean[5];readBools(old.optJSONArray("claimed"),oldClaimed);next.put("claimed",bools(oldClaimed));
        JSONArray dailyProgress=new JSONArray(),oldDaily=old.optJSONArray("daily");
        for(int i=0;i<5;i++)dailyProgress.put(oldDaily==null?0:Math.max(0,Math.min(1_000_000_000L,oldDaily.optLong(i,0))));
        next.put("daily",dailyProgress);return next;
    }
    /** Write exact legacy bytes first; a failed/mismatched backup never replaces the old profile. */
    private boolean preserveLegacyProfile(){
        AtomicFile backup=new AtomicFile(new File(file.getBaseFile().getParentFile(),"profile-v1-legacy-backup.json"));FileOutputStream out=null;
        try{
            if(backup.getBaseFile().exists()||new File(backup.getBaseFile()+".bak").exists())return java.util.Arrays.equals(legacyProfileBytes,backup.readFully());
            out=backup.startWrite();out.write(legacyProfileBytes);backup.finishWrite(out);return true;
        }catch(Exception failure){if(out!=null)backup.failWrite(out);return false;}
    }
    private static void readBools(JSONArray a,boolean[] b){if(a!=null)for(int i=0;i<b.length;i++)b[i]=a.optBoolean(i,b[i]);}
    private static JSONArray bools(boolean[] b){JSONArray a=new JSONArray();for(boolean v:b)a.put(v);return a;}
    public boolean save(){
        if(storageError.equals("read"))return false;
        if(legacyProfileBytes!=null&&!preserveLegacyProfile()){storageError="write";return false;}
        FileOutputStream out=null;
        try{
            JSONObject j=new JSONObject();j.put("schema",5);j.put("render3d",render3d);j.put("stuntHigh",stuntHigh);j.put("totalStunts",totalStunts);j.put("guardiansDefeated",guardiansDefeated);j.put("duelRuns",duelRuns);j.put("duelWins",duelWins);j.put("coopWins",coopWins);j.put("sprintHigh",sprintHigh);j.put("dailyHigh",dailyHigh);j.put("rushHigh",rushHigh);j.put("challengeDate",challengeDate);j.put("totalNear",totalNear);j.put("high",high);j.put("xp",xp);j.put("coins",coins);j.put("runs",runs);
            j.put("overdrives",totalOverdrives);j.put("totalSeconds",totalSeconds);j.put("energy",totalEnergy);j.put("passed",totalPassed);j.put("powers",totalPowers);j.put("combo",bestCombo);j.put("longest",longest);
            j.put("tutorial",tutorial);j.put("music",music);j.put("sound",sound);j.put("haptic",haptic);j.put("reducedMotion",reducedMotion);j.put("relaxed",relaxed);j.put("highContrast",highContrast);j.put("language",language);j.put("day",day);j.put("skin",skin);j.put("zone",zone);j.put("trail",trail);j.put("badge",badge);j.put("musicTrack",musicTrack);
            j.put("owned",bools(owned));j.put("achievements",bools(achievements));j.put("claimed",bools(claimed));
            JSONArray a=new JSONArray();for(long v:daily)a.put(v);j.put("daily",a);JSONArray s=new JSONArray();for(int v:stars)s.put(v);j.put("stars",s);
            out=file.startWrite();out.write(j.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);legacyProfileBytes=null;storageError="";return true;
        }catch(Exception e){if(out!=null)file.failWrite(out);storageError="write";return false;}
    }
}

