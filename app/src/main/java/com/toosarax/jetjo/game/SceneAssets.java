package com.toosarax.jetjo.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Original illustrated JET Worlds pack, decoded off the UI thread with bounded shared cache. */
public final class SceneAssets {
    public static final int COUNT = 16;
    private static final int[] CAMPAIGN_WORLDS={0,3,2,1,5,4,7,11,8,9,12,15};
    public static int campaignWorld(int chapter){return CAMPAIGN_WORLDS[Math.max(0,Math.min(CAMPAIGN_WORLDS.length-1,chapter))];}
    private static final String[] IDS = {"aurora", "solar", "ocean", "candy", "cyber", "crystal", "lunar", "rainbow", "forest", "coral", "toy", "arcade", "sunset", "volcano", "snow", "spaceport"};
    private static final String[] EN = {"Aurora Gardens", "Solar Frontier", "Ocean of Stars", "Candy Cosmos", "Neon Metropolis", "Crystal Caverns", "Moonlight Station", "Rainbow Runway", "Firefly Forest", "Coral Kingdom", "Toy Planet", "Arcade Circuit", "Sunset Islands", "Ember Volcano", "Snowflake Peaks", "Starship Harbor"};
    private static final String[] FA = {"باغ شفق", "مرز خورشید", "اقیانوس ستاره‌ها", "کهکشان آب‌نبات", "شهر نئونی", "غار بلور", "ایستگاه مهتاب", "مسیر رنگین‌کمان", "جنگل شب‌تاب", "سرزمین مرجان", "سیاره اسباب‌بازی", "مدار آرکید", "جزیره‌های غروب", "آتشفشان اخگر", "قله‌های برفی", "بندر ستاره‌پیما"};
    private static final int[] COLORS = {0xff5befc4, 0xffffb14a, 0xff3ebdfb, 0xfffc7fc1, 0xff59e9db, 0xff8ba1fe, 0xffa1b5fa, 0xff81f0b1, 0xff72e484, 0xff4ae2d7, 0xfffdc561, 0xfff773e9, 0xfffba264, 0xffff8b3f, 0xffa5e1fe, 0xff87d3f4};
    private static final String[] TRACKS = {"aurora_drift", "solar_heartbeat", "ocean_glide", "candy_bounce", "cyber_cruise", "crystal_echo", "forest_fireflies", "coral_waltz", "toy_parade", "arcade_flux", "sunset_soar", "spaceport_launch"};
    private static final int[] WORLD_TRACK = {0, 1, 2, 3, 4, 5, 2, 3, 6, 7, 8, 9, 10, 1, 5, 11};
    private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "jetjo-world-decode");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<Integer, Bitmap> CACHE = new LruCache<Integer, Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(Integer key, Bitmap value) { return value.getAllocationByteCount(); }
    };
    private final Context context;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private final int sample;
    private final Runnable invalidator;
    private Bitmap bitmap;
    private int world = -1;
    private int generation;
    private boolean released;
    private Shader fallback;
    private float oldWidth = -1, oldHeight = -1;

    public SceneAssets(Context context) { this(context, 1, null); }
    private SceneAssets(Context context, int sample, Runnable invalidator) {
        this.context = context.getApplicationContext();
        this.sample = sample;
        this.invalidator = invalidator;
    }
    private static int valid(int index) { return Math.max(0, Math.min(COUNT - 1, index)); }
    public static String id(int index) { return IDS[valid(index)]; }
    public static String nameEn(int index) { return EN[valid(index)]; }
    public static String nameFa(int index) { return FA[valid(index)]; }
    public static int color(int index) { return COLORS[valid(index)]; }
    public static int musicTrack(int index) { return WORLD_TRACK[valid(index)]; }
    /** Soundtrack path by world. Use trackPath for independent music-room selection. */
    public static String musicPath(int index) { return trackPath(musicTrack(index)); }
    public static String trackPath(int index) { return "content/music/" + TRACKS[Math.max(0, Math.min(TRACKS.length - 1, index))] + ".wav"; }
    public static int trackCount() { return TRACKS.length; }
    public static String trackName(int index) { return trackNameEn(index); }
    public static String trackNameFa(int index) {
        String[] names = {"سفر شفق", "ضربان خورشید", "سر خوردن روی اقیانوس", "رقص آب‌نبات", "پرواز نئونی", "پژواک بلور", "شب‌تاب‌های جنگل", "والس مرجان", "رژه اسباب‌بازی", "ریتم آرکید", "پرواز در غروب", "پرتاب ستاره‌پیما"};
        return names[Math.max(0, Math.min(names.length - 1, index))];
    }
    public static String trackNameEn(int index) {
        String[] names = {"Aurora Drift", "Solar Heartbeat", "Ocean Glide", "Candy Bounce", "Cyber Cruise", "Crystal Echo", "Forest Fireflies", "Coral Waltz", "Toy Parade", "Arcade Flux", "Sunset Soar", "Spaceport Launch"};
        return names[Math.max(0, Math.min(names.length - 1, index))];
    }
    public int selected() { return Math.max(0, world); }
    /** All selects and drawing must be called on the UI thread. */
    public void select(int index) {
        index = valid(index);
        if (released || world == index) return;
        world = index;
        fallback = null;
        bitmap = null;
        int request = ++generation;
        int key = index * 10 + sample;
        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) { bitmap = cached; if (invalidator != null) invalidator.run(); return; }
        final int selected = index;
        DECODER.execute(() -> {
            Bitmap result = CACHE.get(key);
            if (result == null || result.isRecycled()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sample;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                options.inScaled = false;
                try (InputStream input = context.getAssets().open("content/worlds/" + IDS[selected] + ".jpg")) {
                    result = BitmapFactory.decodeStream(input, null, options);
                    if (result != null) CACHE.put(key, result);
                } catch (IOException | OutOfMemoryError ignored) { result = null; }
            }
            final Bitmap loaded = result;
            MAIN.post(() -> {
                if (!released && request == generation) { bitmap = loaded; if (invalidator != null) invalidator.run(); }
            });
        });
    }
    public void draw(Canvas canvas, float width, float height, float time, boolean reducedMotion) {
        if (width <= 0 || height <= 0) return;
        if (world < 0) select(0);
        Bitmap current = bitmap;
        if (current == null || current.isRecycled()) {
            if (fallback == null || oldWidth != width || oldHeight != height) {
                oldWidth = width; oldHeight = height;
                fallback = new LinearGradient(0, 0, width, height, new int[]{0xff080d19, color(world) & 0xff303f4f}, null, Shader.TileMode.CLAMP);
            }
            paint.setShader(fallback); canvas.drawRect(0, 0, width, height, paint); paint.setShader(null); return;
        }
        float scale = Math.max(width / current.getWidth(), height / current.getHeight()) * (reducedMotion ? 1f : 1.035f);
        float dw = current.getWidth() * scale, dh = current.getHeight() * scale;
        float dx = reducedMotion ? 0 : (float)Math.sin(time * .12f) * width * .008f;
        float dy = reducedMotion ? 0 : (float)Math.cos(time * .1f) * height * .008f;
        float left = (width - dw) * .5f + dx, top = (height - dh) * .5f + dy;
        destination.set(left, top, left + dw, top + dh);
        paint.setColor(0xffffffff); paint.setAlpha(255); paint.setShader(null);
        canvas.drawBitmap(current, null, destination, paint);
    }
    public void release() { released = true; generation++; bitmap = null; }

    /** Static themed world card; redraws once when asynchronous decode completes. */
    @android.annotation.SuppressLint("ViewConstructor")
    public static final class Preview extends View {
        private SceneAssets scene;
        private final int world;
        public Preview(Context context, int world) {
            super(context); this.world = valid(world);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            scene = new SceneAssets(context, 2, this::invalidate);
            scene.select(this.world);
        }
        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (scene == null) { scene = new SceneAssets(getContext(), 2, this::invalidate); scene.select(world); }
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (scene != null) scene.draw(canvas, getWidth(), getHeight(), 0, true);
        }
        @Override protected void onDetachedFromWindow() {
            if (scene != null) { scene.release(); scene = null; }
            super.onDetachedFromWindow();
        }
    }
}