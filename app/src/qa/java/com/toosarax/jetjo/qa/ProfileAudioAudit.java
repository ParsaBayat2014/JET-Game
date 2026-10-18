package com.toosarax.jetjo.qa;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.AudioManager;
import com.toosarax.jetjo.audio.Feedback;
import com.toosarax.jetjo.core.DuelEngine;
import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.core.GameModes;
import com.toosarax.jetjo.core.Progression;
import com.toosarax.jetjo.data.Profile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Actual Android AtomicFile/profile checks in a unique QA-only directory. */
final class ProfileAudioAudit {
    private static void check(boolean result,String why){if(!result)throw new IllegalStateException(why);}
    private static Context files(Context base,File folder){return new ContextWrapper(base){@Override public File getFilesDir(){return folder;}};}
    private static void finish(GameEngine run){for(int i=0;i<7500&&run.alive;i++)run.update(GameConfig.STEP);check(!run.alive,"Timed run did not finish");}
    static String profile(Context context) throws Exception {
        File folder=new File(context.getFilesDir(),"profile-audit-"+System.nanoTime());check(folder.mkdirs(),"QA directory unavailable");
        Profile p=new Profile(files(context,folder));
        GameEngine run=new GameEngine(107);run.mode=GameModes.COIN_RUSH;run.start(false);
        check(p.record(run)==0&&p.runs==0,"Live flight awarded economy");
        finish(run);check(p.record(run)>0&&p.runs==1,"Terminal flight not recorded");
        long xp=p.xp;int coins=p.coins;check(p.record(run)==0&&p.xp==xp&&p.coins==coins&&p.runs==1,"Repeated settlement duplicated rewards");
        run.start(false);finish(run);check(p.record(run)>0&&p.runs==2,"Restarted engine was mistaken for a duplicate result");
        DuelEngine party=new DuelEngine(107,true,true,true);for(int i=0;i<7500&&!party.finished();i++)party.update(GameConfig.STEP);
        check(p.record(party.players[0])==0&&p.runs==2,"Party flight awarded solo economy");
        GameEngine school=new GameEngine(108);school.mode=GameModes.STUNT;school.start(true);school.alive=false;
        check(p.record(school)==0&&p.runs==2,"Tutorial awarded economy");
        GameEngine zen=new GameEngine(109);zen.mode=GameModes.ZEN;zen.start(false);zen.alive=false;
        check(p.record(zen)==0,"Zen awarded economy");
        p.daily[0]=p.target(0);check(p.claim(0),"Completed daily claim failed");coins=p.coins;xp=p.xp;
        check(!p.claim(0)&&p.coins==coins&&p.xp==xp,"Daily reward was claimed twice");
        p.coins=2000;p.xp=Progression.threshold(5);check(p.unlock(2),"Earned character did not unlock");
        check(p.save(),"Profile did not save");Profile reloaded=new Profile(files(context,folder));
        check(reloaded.owned[2]&&reloaded.claimed[0]&&reloaded.coins==p.coins&&reloaded.xp==p.xp&&reloaded.runs==2,"Atomic profile reload changed progress");
        File blocker=new File(folder,"blocked-files");try(FileOutputStream stream=new FileOutputStream(blocker)){stream.write(1);}
        Profile failing=new Profile(files(context,blocker));failing.coins=2000;failing.xp=Progression.threshold(5);failing.daily[0]=failing.target(0);
        boolean[] before=failing.achievements.clone();coins=failing.coins;xp=failing.xp;
        check(!failing.claim(0)&&!failing.claimed[0]&&failing.coins==coins&&failing.xp==xp&&Arrays.equals(before,failing.achievements),"Failed daily save left a phantom reward");
        check(!failing.unlock(2)&&!failing.owned[2]&&failing.coins==coins&&Arrays.equals(before,failing.achievements),"Failed unlock save spent credits or granted ownership");
        File extreme=new File(folder,"extreme");check(extreme.mkdir(),"QA extreme directory unavailable");
        try(FileOutputStream stream=new FileOutputStream(new File(extreme,"profile-v1.json"))){stream.write("{\"longest\":1e300}".getBytes(StandardCharsets.UTF_8));}
        Profile sanitized=new Profile(files(context,extreme));check(sanitized.longest==0&&sanitized.save(),"Non-finite saved duration blocked future saving");
        return "Real AtomicFile reload; duplicate/live/party/tutorial/Zen rejected; save failures rolled back; extreme duration sanitized.";
    }
    static String audio(Activity activity) throws Exception {
        File folder=new File(activity.getFilesDir(),"audio-audit-"+System.nanoTime());check(folder.mkdirs(),"Audio QA directory unavailable");
        Profile p=new Profile(files(activity,folder));p.sound=false;p.music=false;p.haptic=false;
        Feedback feedback=new Feedback(activity,p);
        try {
            java.lang.reflect.Field field=Feedback.class.getDeclaredField("focusListener");field.setAccessible(true);
            AudioManager.OnAudioFocusChangeListener listener=(AudioManager.OnAudioFocusChangeListener)field.get(feedback);
            java.lang.reflect.Field duck=Feedback.class.getDeclaredField("ducked");duck.setAccessible(true);
            java.lang.reflect.Field focus=Feedback.class.getDeclaredField("focus");focus.setAccessible(true);
            listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);feedback.gameIntensity(true,false);
            check(duck.getBoolean(feedback)&&focus.getBoolean(feedback),"Turbo lost temporary audio ducking");
            listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);check(!duck.getBoolean(feedback)&&focus.getBoolean(feedback),"Audio gain did not restore normal focus");
            listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);check(!focus.getBoolean(feedback),"Transient focus loss was ignored");
            feedback.release();feedback.release();feedback.resume();feedback.settingsChanged();feedback.selectTrack(1);feedback.play(Feedback.TURBO);
            return "Actual Feedback/MediaPlayer initialization; duck/gain/loss transition and repeated-release callbacks safe (silent QA).";
        } finally {feedback.release();}
    }
}
