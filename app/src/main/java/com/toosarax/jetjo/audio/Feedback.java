package com.toosarax.jetjo.audio;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.os.*;
import com.toosarax.jetjo.R;
import com.toosarax.jetjo.data.Profile;
import com.toosarax.jetjo.game.SceneAssets;

/** Small pooled effects and a single streamed soundtrack; never loads PCM into a byte array. */
public final class Feedback {
    public static final int CLICK=0,COLLECT=1,COMBO=2,SHIELD=3,MAGNET=4,SLOW=5,DOUBLE=6,HIT=7,OVER=8,LEVEL=9,ACHIEVEMENT=10,JUMP=11,DUCK=12,TURBO=13,GUARDIAN=14,GUARDIAN_BREAK=15,STUNT=16;
    private final Profile profile;
    private final Context context;
    private final SoundPool pool;
    private final int[] sounds=new int[17];
    private final boolean[] loaded=new boolean[64];
    private final Vibrator vibrator;
    private final AudioManager manager;
    private final AudioAttributes attributes;
    private MediaPlayer music;
    private boolean foreground,focus,ducked,prepared,closed;
    private int currentTrack=-1;
    private AudioFocusRequest focusRequest;
    private long lastHaptic; private float musicVolume=.30f;
    private final AudioManager.OnAudioFocusChangeListener focusListener=change->{
        if(closed)return;
        ducked=change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
        focus=change>0||ducked;updateMusic();
    };
    public Feedback(Activity activity,Profile p){
        context=activity.getApplicationContext();profile=p;
        vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        pool=new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attributes).build();
        pool.setOnLoadCompleteListener((s,id,status)->{if(id<loaded.length)loaded[id]=status==0;});
        int[] res={R.raw.click,R.raw.collect,R.raw.combo,R.raw.shield,R.raw.magnet,R.raw.slow,R.raw.double_score,R.raw.collision,R.raw.game_over,R.raw.level,R.raw.achievement,R.raw.jump,R.raw.duck,R.raw.turbo,R.raw.guardian,R.raw.guardian_break,R.raw.stunt};
        for(int i=0;i<res.length;i++)sounds[i]=pool.load(context,res[i],1);
        if(Build.VERSION.SDK_INT>=26)focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener).build();
        selectTrack(p.musicTrack);
    }
    public void selectTrack(int track){
        if(closed)return;
        track=Math.max(0,Math.min(SceneAssets.trackCount()-1,track));if(track==currentTrack&&music!=null)return;
        currentTrack=track;prepared=false;
        if(music!=null){music.setOnPreparedListener(null);music.release();music=null;}
        MediaPlayer next=new MediaPlayer();music=next;next.setAudioAttributes(attributes);
        try(AssetFileDescriptor fd=context.getAssets().openFd(SceneAssets.trackPath(track))){
            next.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            next.setLooping(true);
            next.setOnPreparedListener(player->{if(closed||music!=player)return;prepared=true;updateMusic();});
            next.setOnErrorListener((player,what,extra)->{if(music==player){prepared=false;music=null;player.release();}return true;});
            next.prepareAsync();
        }catch(Exception unavailable){next.release();music=MediaPlayer.create(context,R.raw.music);if(music!=null){prepared=true;music.setLooping(true);updateMusic();}}
    }
    @SuppressWarnings("deprecation")
    public void resume(){if(closed)return;foreground=true;pool.autoResume();requestFocus();updateMusic();}
    @SuppressWarnings("deprecation")
    private void requestFocus(){
        if(!profile.sound&&!profile.music){
            if(Build.VERSION.SDK_INT>=26)manager.abandonAudioFocusRequest(focusRequest);
            else manager.abandonAudioFocus(focusListener);
            focus=false;ducked=false;return;
        }
        int result=Build.VERSION.SDK_INT>=26?manager.requestAudioFocus(focusRequest):manager.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
        focus=result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;ducked=false;
    }
    public void updateMusic(){
        if(closed||music==null||!prepared)return;
        MediaPlayer player=music;
        try{
            float volume=ducked?musicVolume*.3f:musicVolume;player.setVolume(volume,volume);
            if(foreground&&profile.music&&focus){if(!player.isPlaying())player.start();}
            else if(player.isPlaying())player.pause();
        }catch(IllegalStateException unavailable){if(music==player){music=null;prepared=false;}player.release();}
    }
    public void gameIntensity(boolean turbo,boolean guardian){float next=turbo?.42f:guardian?.36f:.30f;if(next==musicVolume)return;musicVolume=next;updateMusic();}
    public void settingsChanged(){if(closed)return;selectTrack(profile.musicTrack);if(foreground)requestFocus();updateMusic();}
    @SuppressWarnings("deprecation")
    public void pause(){
        if(closed)return;foreground=false;updateMusic();pool.autoPause();
        if(Build.VERSION.SDK_INT>=26)manager.abandonAudioFocusRequest(focusRequest);
        else manager.abandonAudioFocus(focusListener);focus=false;ducked=false;
    }
    @SuppressWarnings("deprecation")
    public void play(int sound){
        if(sound<0||sound>=sounds.length||closed)return;
        if(foreground&&profile.sound&&focus&&sounds[sound]<loaded.length&&loaded[sounds[sound]]){float volume=ducked?.20f:.65f;pool.play(sounds[sound],volume,volume,1,0,1);}
        long now=SystemClock.uptimeMillis();
        if(foreground&&profile.haptic&&vibrator!=null&&vibrator.hasVibrator()&&now-lastHaptic>(sound==COLLECT?220:80)){
            int ms=sound==GUARDIAN_BREAK||sound==TURBO?42:sound==HIT||sound==OVER?36:sound==CLICK||sound==JUMP||sound==DUCK?8:13;
            if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(ms,70));else vibrator.vibrate(ms);lastHaptic=now;
        }
    }
    public void release(){if(closed)return;pause();closed=true;pool.release();if(music!=null){music.setOnPreparedListener(null);music.setOnErrorListener(null);music.release();music=null;}prepared=false;}
}

