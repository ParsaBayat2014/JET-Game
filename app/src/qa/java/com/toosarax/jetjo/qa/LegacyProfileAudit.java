package com.toosarax.jetjo.qa;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.AtomicFile;
import com.toosarax.jetjo.core.Progression;
import com.toosarax.jetjo.data.Profile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;

/** Published-v1 schema fixtures use unique private QA directories and real Android AtomicFile. */
final class LegacyProfileAudit {
    private static void check(boolean result,String why){if(!result)throw new IllegalStateException(why);}
    private static Context files(Context base,File folder){return new ContextWrapper(base){@Override public File getFilesDir(){return folder;}};}
    private static File folder(File parent,String name){File result=new File(parent,name);check(result.mkdir(),"Cannot create legacy QA folder "+name);return result;}
    private static byte[] bytes(JSONObject object){return object.toString().getBytes(StandardCharsets.UTF_8);}
    private static byte[] read(File file) throws Exception{return new AtomicFile(file).readFully();}
    private static void write(File file,byte[] contents) throws Exception{try(FileOutputStream output=new FileOutputStream(file)){output.write(contents);}}
    private static JSONArray flags(boolean... values){JSONArray result=new JSONArray();for(boolean value:values)result.put(value);return result;}
    /** Field names and semantic indices copied from the actual 1.0 PlayerState/Catalog source. */
    private static JSONObject fixture(long day,long xp) throws Exception {
        JSONObject old=new JSONObject();old.put("schema",1).put("xp",xp).put("day",day).put("best",12345).put("coins",777).put("runs",12)
            .put("totalEnergy",4567).put("totalDodged",987).put("totalPowers",88).put("bestCombo",26).put("missionClaims",9).put("shieldSaves",3)
            .put("selectedSkin",8).put("environment",2).put("lastScore",321).put("lastEnergy",9).put("lastXp",77).put("lastSeconds",77.5)
            .put("lastDodged",10).put("lastCombo",4).put("tutorial",true).put("music",false).put("sound",false).put("haptics",false)
            .put("reducedMotion",true).put("language","fa");
        old.put("skins",flags(true,true,true,true,true,true,false,true,true,false));
        old.put("achievements",flags(true,true,false,true,true,true,true,true,true,true,true,true,true,false,true,false,true,true));
        old.put("claimed",flags(true,false,true,false,false));old.put("daily",new JSONArray(new int[]{2500,100,99,8,77}));return old;
    }
    static String run(Context context) throws Exception {
        File root=folder(context.getFilesDir(),"legacy-profile-audit-"+System.nanoTime());long today=Math.floorDiv(System.currentTimeMillis(),86_400_000L);
        File current=folder(root,"current-day"),source=new File(current,"profile-v1.json"),backup=new File(current,"profile-v1-legacy-backup.json");
        byte[] original=bytes(fixture(today,1700));write(source,original);Profile migrated=new Profile(files(context,current));
        check(migrated.storageError.isEmpty(),"Legacy migration failed");check(migrated.high==12345&&migrated.coins==777&&migrated.runs==12,"Legacy score/credits/runs changed");
        check(migrated.totalEnergy==4567&&migrated.totalPassed==987&&migrated.totalPowers==88&&migrated.bestCombo==26,"Legacy flight statistics lost");
        check(migrated.longest==77.5f&&migrated.xp==2998&&migrated.level()==7,"Legacy duration or level/progress lost");
        check(migrated.skin==8&&migrated.zone==2&&migrated.owned[2]&&migrated.owned[7]&&migrated.owned[8]&&!migrated.owned[6]&&!migrated.owned[9],"Legacy free ownership/selection changed");
        for(int index=10;index<18;index++)check(!migrated.owned[index],"Legacy cosmetic name granted paid skin "+index);
        check(migrated.tutorial&&!migrated.music&&!migrated.sound&&!migrated.haptic&&migrated.reducedMotion&&migrated.language.equals("fa"),"Legacy settings changed");
        check(migrated.daily[0]==2500&&migrated.daily[1]==100&&migrated.daily[4]==77&&migrated.claimed[0]&&migrated.claimed[2],"Current UTC day lost daily progress or claim flags");
        long xp=migrated.xp;int coins=migrated.coins;check(!migrated.claim(0)&&migrated.coins==coins&&migrated.xp==xp,"Legacy claimed reward could be paid twice");
        check(migrated.achievements[0]&&migrated.achievements[1]&&!migrated.achievements[2]&&migrated.achievements[8]&&migrated.achievements[4]&&migrated.achievements[5]&&migrated.achievements[6]&&migrated.achievements[7]&&migrated.achievements[12]&&!migrated.achievements[13]&&migrated.achievements[14]&&!migrated.achievements[15],"Legacy achievement meanings were not mapped");
        for(int index=16;index<migrated.achievements.length;index++)check(!migrated.achievements[index],"Old achievement index granted a new campaign achievement");
        check(Arrays.equals(original,read(backup)),"Raw legacy backup changed bytes");check(new JSONObject(new String(read(source),StandardCharsets.UTF_8)).getInt("schema")==5,"Migration did not persist schema 5");
        Profile reloaded=new Profile(files(context,current));check(reloaded.xp==xp&&reloaded.coins==coins&&reloaded.level()==7&&reloaded.skin==8&&reloaded.claimed[0],"Reload remigrated XP or lost claims/ownership");
        check(Arrays.equals(original,read(backup)),"Reload overwrote raw legacy backup");
        File yesterday=folder(root,"previous-day");write(new File(yesterday,"profile-v1.json"),bytes(fixture(today-1,1700)));Profile rolled=new Profile(files(context,yesterday));
        for(int i=0;i<5;i++)check(rolled.daily[i]==0&&!rolled.claimed[i],"Previous UTC day did not roll over");check(rolled.coins==777&&rolled.xp==2998,"Rollover granted/spent economy");
        File tomorrow=folder(root,"future-day");write(new File(tomorrow,"profile-v1.json"),bytes(fixture(today+1,1700)));Profile future=new Profile(files(context,tomorrow));
        check(future.claimed[0]&&future.daily[0]==2500&&!future.resetDay(),"Clock rollback reopened legacy rewards");
        long[][] xpCases={{0,0,1},{139,218,1},{140,220,2},{319,518,2},{320,520,3},{1700,2998,7},{207899,409858,99},{207900,409860,100},{1_000_000_000L,1_000_000_000L,100}};
        for(int index=0;index<xpCases.length;index++){
            File levelFolder=folder(root,"xp-"+index);write(new File(levelFolder,"profile-v1.json"),bytes(fixture(today,xpCases[index][0])));Profile levelProfile=new Profile(files(context,levelFolder));
            check(levelProfile.xp==xpCases[index][1]&&Progression.level(levelProfile.xp)==xpCases[index][2],"Legacy XP boundary/progress changed at "+xpCases[index][0]);
        }
        File blocked=folder(root,"blocked-backup"),blockedProfile=new File(blocked,"profile-v1.json"),blockedBackup=new File(blocked,"profile-v1-legacy-backup.json");
        write(blockedProfile,original);check(blockedBackup.mkdir(),"Cannot create backup blocker");File marker=new File(blockedBackup,"qa-only-marker");write(marker,new byte[]{1});
        Profile failing=new Profile(files(context,blocked));check(!failing.storageError.isEmpty()&&!failing.save()&&Arrays.equals(original,read(blockedProfile)),"Backup failure replaced the original v1 profile");
        check(marker.delete()&&blockedBackup.delete(),"Cannot clear owned QA backup blocker");check(failing.save()&&Arrays.equals(original,read(blockedBackup)),"Safe migration retry did not preserve v1 bytes");
        File mismatch=folder(root,"mismatched-backup"),mismatchProfile=new File(mismatch,"profile-v1.json"),mismatchBackup=new File(mismatch,"profile-v1-legacy-backup.json");
        byte[] previousBackup="previous distinct legacy bytes".getBytes(StandardCharsets.UTF_8);write(mismatchProfile,original);write(mismatchBackup,previousBackup);Profile mismatchState=new Profile(files(context,mismatch));
        check(!mismatchState.storageError.isEmpty()&&!mismatchState.save()&&Arrays.equals(original,read(mismatchProfile))&&Arrays.equals(previousBackup,read(mismatchBackup)),"Mismatched backup was overwritten or destroyed v1");
        return "Real AtomicFile schema-1 migration; raw backup, score/credits/free slots/settings, level+progress, semantic achievements, UTC claims/rollover, reload and backup-failure safety.";
    }
}
