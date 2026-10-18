package com.toosarax.jetjo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.data.Profile;
import com.toosarax.jetjo.data.Catalog;
import com.toosarax.jetjo.game.Art;
import com.toosarax.jetjo.game.SceneAssets;
import com.toosarax.jetjo.game.FlightPainter;
import com.toosarax.jetjo.game.FlightSnapshot;
import com.toosarax.jetjo.game.FlightGLView;
import com.toosarax.jetjo.multiplayer.LanProtocol;
import com.toosarax.jetjo.multiplayer.LanSession;
import com.toosarax.jetjo.ui.Ui;
import com.toosarax.jetjo.ui.Words;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Two phones, one Wi-Fi/hotspot, no remote service. The hosting phone runs the match. */
public final class LocalMultiplayerActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private Profile profile;
    private Words words;
    private Ui ui;
    private FrameLayout root;
    private LanSession session;
    private RaceView race;
    private TextView status, score, timer, rendererStatus;
    private Button pause;
    private boolean hosting, manualPaused, inBackground, finished;
    private int generation, lane = 1, selectedSkin;
    public static void start(Activity activity) { activity.startActivity(new Intent(activity, LocalMultiplayerActivity.class)); }
    private String t(String en, String fa) { return words.t(en, fa); }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); profile = new Profile(this); words = new Words(profile); ui = new Ui(this, words);
        root = new FrameLayout(this); root.setBackgroundColor(Ui.BG); setContentView(root); lobby("");
    }
    private LinearLayout page() {
        if (race != null) { race.release(); race = null; }
        root.removeAllViews(); ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1)); LinearLayout page = ui.column();
        page.setPadding(ui.dp(20), ui.dp(20), ui.dp(20), ui.dp(24)); scroll.addView(page, new ScrollView.LayoutParams(-1, -2)); return page;
    }
    private void button(LinearLayout page, String text, boolean primary, Runnable action) { ui.add(page, ui.button(text, primary, action), -2, 10); }
    private void leaveSession() {
        generation++; if (race != null) { race.release(); race = null; }
        LanSession old = session; session = null; if (old != null) old.close();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void lobby(String notice) {
        leaveSession(); finished = false; manualPaused = false; lane = 1;
        LinearLayout page = page(); button(page, t("‹ Home", "خانه ›"), false, this::finish);
        ui.add(page, ui.text(t("JET · nearby duel", "جت · رقابت نزدیک"), 28, Ui.TEXT, true), -2, 12);
        ui.label(page, t("Connect both phones to the same Wi-Fi, or turn on a hotspot on one phone and connect the other. Internet data is not required.", "هر دو گوشی را به یک وای‌فای وصل کنید؛ یا هات‌اسپات یک گوشی را روشن کنید و گوشی دوم را به آن وصل کنید. اینترنت لازم نیست."), 14, Ui.MUTED, 12);
        if (!notice.isEmpty()) ui.label(page, notice, 14, Ui.GOLD, 12);
        ui.label(page, t("Your character · free roster for nearby play", "شخصیت تو · گروه رایگان برای رقابت نزدیک"), 13, Ui.LIME, 8);
        Spinner characters = new Spinner(this); java.util.ArrayList<Integer> skinIds = new java.util.ArrayList<>(); java.util.ArrayList<String> skinNames = new java.util.ArrayList<>();
        for (int skin = 0; skin < Catalog.SKINS.length; skin++) if (LanProtocol.freeSkin(skin)) { skinIds.add(skin); skinNames.add(words.t(Catalog.SKINS[skin])); }
        ArrayAdapter<String> characterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, skinNames); characterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        characters.setAdapter(characterAdapter); characters.setSelection(Math.max(0, skinIds.indexOf(selectedSkin))); characters.setContentDescription(t("Your nearby character", "شخصیت تو در رقابت نزدیک"));
        characters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { selectedSkin = skinIds.get(position); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        }); ui.add(page, characters, 52, 14);
        LinearLayout graphics = ui.card(page);
        ui.label(graphics, t("Choose the view on this phone", "نمای این گوشی را انتخاب کن"), 18, Ui.TEXT, 8);
        ui.label(graphics, t("Real 3D uses the GPU. The lighter 2.5D view keeps the same match, controls and fairness. Each phone chooses independently.",
                "نمای سه‌بعدی از پردازنده گرافیکی استفاده می‌کند. نمای سبک‌تر ۲٫۵بعدی همان بازی و کنترل‌ها را دارد. انتخاب هر گوشی مستقل است."), 13, Ui.MUTED, 10);
        ui.pair(graphics,
                ui.button(profile.render3d ? t("3D selected ✓", "سه‌بعدی انتخاب‌شده ✓") : t("3D view", "نمای سه‌بعدی"), profile.render3d,
                        () -> { profile.render3d = true; profile.save(); lobby(""); }),
                ui.button(!profile.render3d ? t("2.5D selected ✓", "۲٫۵بعدی انتخاب‌شده ✓") : t("2.5D view", "نمای ۲٫۵بعدی"), !profile.render3d,
                        () -> { profile.render3d = false; profile.save(); lobby(""); }));
        LinearLayout host = ui.card(page);
        ui.label(host, t("1 · Create a room", "۱ · ساخت اتاق"), 20, Ui.TEXT, 10);
        TextView loading = ui.text(t("Checking network…", "در حال بررسی شبکه…"), 13, Ui.MUTED, false); ui.add(host, loading, -2, 8);
        Spinner addresses = new Spinner(this); addresses.setContentDescription(t("Host network address", "آدرس شبکه میزبان")); ui.add(host, addresses, 48, 8);
        Button create = ui.button(t("Create room", "ساخت اتاق"), true, () -> {
            Object selected = addresses.getSelectedItem(); if (selected != null) host(selected.toString());
        }); create.setEnabled(false); ui.add(host, create, -2, 0);
        final int pageGeneration = generation;
        Thread network = new Thread(() -> {
            List<String> found = LanSession.localAddresses();
            main.post(() -> {
                if (generation != pageGeneration || isFinishing()) return;
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, found); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                addresses.setAdapter(adapter); create.setEnabled(!found.isEmpty());
                loading.setText(found.isEmpty() ? t("No local IPv4 address. Enable Wi-Fi/hotspot, then refresh.", "آدرس شبکه پیدا نشد. وای‌فای یا هات‌اسپات را روشن کنید و تازه‌سازی بزنید.") : t("Choose the address for the Wi-Fi or hotspot you share.", "آدرس وای‌فای یا هات‌اسپات مشترکتان را انتخاب کنید."));
            });
        }, "jetjo-network-list"); network.setDaemon(true); network.start();
        button(page, t("Refresh network", "تازه‌سازی شبکه"), false, () -> lobby(""));
        LinearLayout join = ui.card(page); ui.label(join, t("2 · Join your friend's room", "۲ · ورود به اتاق دوستت"), 20, Ui.TEXT, 10);
        EditText address = field(join, t("Host IP · e.g. 192.168.1.4", "آی‌پی میزبان · مثل 192.168.1.4"), InputType.TYPE_CLASS_PHONE, 15);
        EditText port = field(join, t("Port shown by host", "شماره پورت میزبان"), InputType.TYPE_CLASS_NUMBER, 5);
        EditText code = field(join, t("Six-digit room code", "کد شش‌رقمی اتاق"), InputType.TYPE_CLASS_NUMBER, 6);
        button(join, t("Join room", "ورود به اتاق"), true, () -> {
            try {
                String ip = asciiDigits(address.getText().toString().trim()); int p = Integer.parseInt(asciiDigits(port.getText().toString().trim()));
                String pin = asciiDigits(code.getText().toString().trim()); connect(ip, p, pin);
            } catch (NumberFormatException error) { code.setError(t("Enter the host IP, port and room code.", "آی‌پی، پورت و کد میزبان را وارد کنید.")); }
        });
        ui.label(page, t("75 seconds, identical routes, free characters for both pilots. Crashes cost points but you keep flying. Nearby matches don't change coins, purchases or solo records.", "۷۵ ثانیه، مسیر یکسان و شخصیت‌های رایگان برای هر دو خلبان. برخورد امتیاز کم می‌کند ولی پرواز ادامه دارد. رقابت نزدیک روی سکه، خریدها و رکورد تک‌نفره اثری ندارد."), 13, Ui.MUTED, 12);
        ui.label(page, t("A router with guest isolation can block phone-to-phone play; use your phone's hotspot in that case. Bluetooth is not used for this mode.", "اگر وای‌فای مهمان اتصال دو گوشی را مسدود می‌کند، از هات‌اسپات گوشی استفاده کنید. این حالت از بلوتوث استفاده نمی‌کند."), 12, Ui.MUTED, 0);
    }
    private EditText field(LinearLayout page, String label, int type, int length) {
        ui.label(page, label, 12, Ui.MUTED, 5); EditText edit = new EditText(this); edit.setSingleLine(true); edit.setInputType(type);
        edit.setTextColor(Ui.TEXT); edit.setTextSize(16); edit.setTypeface(ui.regular); edit.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10));
        edit.setBackground(ui.panel(Ui.BG, 12)); edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(length)});
        edit.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); edit.setTextDirection(View.TEXT_DIRECTION_LTR); edit.setContentDescription(label); ui.add(page, edit, 52, 12); return edit;
    }
    private static String asciiDigits(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) { char c = text.charAt(i); int digit = Character.digit(c, 10); result.append(digit >= 0 ? (char) ('0' + digit) : c); } return result.toString();
    }
    private void waiting(boolean host) {
        hosting = host; finished = false; manualPaused = false; lane = 1; LinearLayout page = page();
        ui.add(page, ui.text(t("Nearby flight room", "اتاق پرواز نزدیک"), 26, Ui.TEXT, true), -2, 14);
        status = ui.text(t("Connecting…", "در حال اتصال…"), 17, Ui.GOLD, true); ui.add(page, status, -2, 16);
        ui.label(page, t("Keep this page open on both phones. Share the IP, port and room code shown on the host.", "این صفحه را روی هر دو گوشی باز نگه دارید. آی‌پی، پورت و کد نمایش‌داده‌شده روی میزبان را در گوشی دوم وارد کنید."), 14, Ui.MUTED, 20);
        button(page, t("Cancel", "لغو"), false, () -> lobby("")); getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private LanSession.Listener listener(int expected) {
        return new LanSession.Listener() {
            private boolean current() { return generation == expected && !isFinishing(); }
            @Override public void room(String address, int port, String pin) {
                if (!current()) return;
                status.setText(String.format(Locale.ROOT, t("Waiting for your friend\n\nIP: %s\nPort: %d\nRoom code: %s", "منتظر دوستت\n\nIP: %s\nPort: %d\nکد اتاق: %s"), address, port, pin));
                status.setTextDirection(View.TEXT_DIRECTION_LTR); status.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); status.setTextIsSelectable(true);
            }
            @Override public void connected(boolean host) { if (current()) { hosting = host; play(); } }
            @Override public void snapshot(LanProtocol.Snapshot snapshot) {
                if (!current() || race == null || finished) return; race.update(snapshot);
                int own = hosting ? 0 : 1, rival = 1 - own;
                score.setText(String.format(Locale.ROOT, t("YOU %s  ·  FRIEND %s", "تو %s  ·  دوستت %s"), words.n(snapshot.pilots[own].score), words.n(snapshot.pilots[rival].score)));
                timer.setText(snapshot.paused ? t("Paused on one phone", "بازی روی یکی از گوشی‌ها متوقف است") : String.format(Locale.ROOT, t("%s seconds · Combo %s", "%s ثانیه · زنجیره %s"), words.n((long) Math.ceil(LanProtocol.MATCH_SECONDS - snapshot.seconds)), words.n(snapshot.pilots[own].combo)));
                if (snapshot.finished) result(snapshot);
            }
            @Override public void failed(String reason) {
                if (current() && !finished) lobby(t("Connection ended. Check that both phones use the same network, the IP and port are correct, and the room code matches. Create a fresh room to retry.", "اتصال پایان یافت. شبکه مشترک، آی‌پی، پورت و کد اتاق را بررسی کنید. برای تلاش دوباره یک اتاق تازه بسازید."));
            }
        };
    }
    private void host(String address) {
        leaveSession(); waiting(true); int expected = generation;
        try { session = LanSession.host(address, profile.zone, selectedSkin, main::post, listener(expected)); }
        catch (IOException error) { lobby(t("Connect to Wi-Fi or enable your hotspot first.", "اول به وای‌فای وصل شوید یا هات‌اسپات را روشن کنید.")); }
    }
    private void connect(String address, int port, String pin) {
        leaveSession(); waiting(false); int expected = generation;
        try { session = LanSession.join(address, port, pin, selectedSkin, main::post, listener(expected)); }
        catch (IOException error) { lobby(t("Enter the host's private IPv4 address, port and six-digit room code.", "آی‌پی محلی، پورت و کد شش‌رقمی میزبان را وارد کنید.")); }
    }
    private void play() {
        root.removeAllViews(); LinearLayout layout = ui.column(); root.addView(layout, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout hud = ui.column(); hud.setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(8)); ui.add(layout, hud, -2, 0);
        score = ui.text(t("Ready…", "آماده…"), 18, Ui.TEXT, true); ui.add(hud, score, -2, 7);
        timer = ui.text(t("75-second duel", "رقابت ۷۵ ثانیه‌ای"), 13, Ui.GOLD, false); ui.add(hud, timer, -2, 7);
        pause = ui.button(t("Ⅱ Pause", "Ⅱ مکث"), false, () -> { manualPaused = !manualPaused; sendInput(); pause.setText(manualPaused ? t("▶ Resume", "▶ ادامه") : t("Ⅱ Pause", "Ⅱ مکث")); });
        ui.pair(hud, pause, ui.button(t("Leave room", "خروج از اتاق"), false, () -> lobby("")));
        rendererStatus = ui.text("", 12, Ui.MUTED, false); ui.add(hud, rendererStatus, -2, 6);
        FrameLayout flight = new FrameLayout(this); layout.addView(flight, new LinearLayout.LayoutParams(-1, 0, 1));
        race = new RaceView(); race.attach(flight, generation);
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL); controls.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        controls.setPadding(ui.dp(10), ui.dp(8), ui.dp(10), ui.dp(10)); controls.setBaselineAligned(false);
        for (int i = 0; i < 3; i++) {
            final int selected = i; Button move = ui.button(i == 0 ? "◀" : i == 1 ? "●" : "▶", true, () -> { lane = selected; sendInput(); });
            move.setContentDescription(i == 0 ? t("Left lane", "مسیر چپ") : i == 1 ? t("Middle lane", "مسیر وسط") : t("Right lane", "مسیر راست"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ui.dp(58), 1); params.setMargins(ui.dp(3), 0, ui.dp(3), 0); controls.addView(move, params);
        }
        ui.add(layout, controls, -2, 0); sendInput();
    }
    private void sendInput() { LanSession current = session; if (current != null) current.input(lane, manualPaused || inBackground); }
    private void result(LanProtocol.Snapshot state) {
        finished = true; int own = hosting ? 0 : 1; long mine = state.pilots[own].score, theirs = state.pilots[1 - own].score;
        leaveSession(); LinearLayout page = page();
        ui.add(page, ui.text(mine == theirs ? t("A perfect tie!", "مساوی شد!") : mine > theirs ? t("You win!", "تو بردی!") : t("Your friend wins!", "دوستت برد!"), 30, Ui.LIME, true), -2, 18);
        ui.add(page, ui.metric(t("Your score", "امتیاز تو"), words.n(mine), Ui.GOLD), -2, 12);
        ui.add(page, ui.metric(t("Friend's score", "امتیاز دوستت"), words.n(theirs), Ui.LIME), -2, 18);
        ui.label(page, t("Same route, two pilots. Ready for a rematch?", "یک مسیر، دو خلبان. آماده دور بعدی؟"), 15, Ui.MUTED, 18);
        button(page, t("New room / rematch", "اتاق تازه / بازی دوباره"), true, () -> lobby("")); button(page, t("Home", "خانه"), false, this::finish);
    }
    @Override protected void onPause() { inBackground = true; if (race != null) race.pauseRenderer(); sendInput(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); inBackground = false; if (race != null) race.resumeRenderer(); sendInput(); }
    @Override protected void onStop() {
        super.onStop(); if (session != null) lobby(t("The room closed when the game went into the background. Create or join a room again.", "با رفتن بازی به پس‌زمینه اتاق بسته شد. دوباره اتاق بسازید یا وارد شوید."));
    }
    @Override protected void onDestroy() { leaveSession(); if (race != null) race.release(); main.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override public void onBackPressed() { if (session != null) lobby(""); else super.onBackPressed(); }

    @android.annotation.SuppressLint("ViewConstructor")
    private final class RaceView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final SceneAssets scenes;
        private LanProtocol.Snapshot state;
        private FlightSnapshot ownFrame, friendFrame;
        private final FlightPainter[] painters = {new FlightPainter(), new FlightPainter()};
        private FlightGLView graphics;
        private FrameLayout layer;
        private int expectedGeneration;
        private boolean released, fallback;
        RaceView() {
            super(LocalMultiplayerActivity.this); scenes = new SceneAssets(LocalMultiplayerActivity.this); scenes.select(profile.zone);
            setContentDescription(t("Your track on the left, friend's track on the right. Tap your lane below to move.",
                    "مسیر تو سمت چپ و مسیر دوستت سمت راست است. برای حرکت، دکمه مسیر پایین را لمس کن."));
            setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
        private boolean current() { return !released && race == this && expectedGeneration == generation && !isFinishing(); }
        void attach(FrameLayout parent, int expected) {
            layer = parent; expectedGeneration = expected;
            if (profile.render3d) {
                final FlightGLView renderer = new FlightGLView(LocalMultiplayerActivity.this, profile);
                graphics = renderer; layer.addView(renderer, new FrameLayout.LayoutParams(-1, -1));
                renderer.setReadyListener(() -> {
                    if (!current() || graphics != renderer || !renderer.sceneReady()) return;
                    showRendererStatus(); invalidate();
                });
                renderer.setFailureListener(() -> {
                    if (!current() || graphics != renderer) return;
                    graphics = null; fallback = true; renderer.releaseScene(); layer.removeView(renderer);
                    showRendererStatus(); invalidate();
                });
            }
            layer.addView(this, new FrameLayout.LayoutParams(-1, -1)); showRendererStatus();
            // Wait for a paired match snapshot before claiming a ready 3D game scene.
        }
        private void showRendererStatus() {
            if (!current() || rendererStatus == null) return;
            if (graphics == null) rendererStatus.setText(fallback ? t("2.5D view · available on this device", "نمای ۲٫۵بعدی · فعال در این دستگاه")
                    : t("2.5D view", "نمای ۲٫۵بعدی"));
            else rendererStatus.setText(graphics.sceneReady() ? t("3D view · both tracks ready", "نمای سه‌بعدی · هر دو مسیر آماده")
                    : t("Preparing graphics…", "آماده‌سازی تصویر…"));
        }
        void update(LanProtocol.Snapshot value) {
            if (!current()) return;
            state = value; scenes.select(value.world);
            int own = hosting ? 0 : 1;
            ownFrame = FlightSnapshot.fromPilot(value.pilots[own], value.seconds, value.world, profile.reducedMotion, profile.highContrast);
            friendFrame = FlightSnapshot.fromPilot(value.pilots[1 - own], value.seconds, value.world, profile.reducedMotion, profile.highContrast);
            if (graphics != null) {
                graphics.publishPair(ownFrame, friendFrame, .62f);
                if (!inBackground && !value.paused && !value.finished) graphics.resumeScene(); else graphics.pauseScene();
                showRendererStatus();
            }
            invalidate();
        }
        void pauseRenderer() { if (graphics != null) graphics.pauseScene(); }
        void resumeRenderer() {
            if (!current() || graphics == null || state == null || state.paused || state.finished || inBackground) return;
            graphics.resumeScene(); showRendererStatus();
        }
        void release() {
            if (released) return; released = true; scenes.release();
            FlightGLView renderer = graphics; graphics = null;
            if (renderer != null) { renderer.releaseScene(); if (layer != null) layer.removeView(renderer); }
            layer = null; ownFrame = friendFrame = null;
        }
        @Override protected void onDetachedFromWindow() { release(); super.onDetachedFromWindow(); }
        @Override protected void onDraw(Canvas canvas) {
            float width = getWidth(), height = getHeight(); if (width <= 0 || height <= 0 || released) return;
            LanProtocol.Snapshot s = state;
            boolean ready3d = graphics != null && graphics.sceneReady();
            if (!ready3d) {
                scenes.draw(canvas, width, height, s == null ? 0 : s.seconds, profile.reducedMotion);
                paint.setColor(0x85040b13); canvas.drawRect(0, 0, width, height, paint);
            }
            if (s == null) { postInvalidateDelayed(50); return; }
            float split = width * .62f;
            track(canvas, ownFrame, ui.dp(5), split - ui.dp(10), height, t("YOU", "تو"), 0, !ready3d);
            track(canvas, friendFrame, split + ui.dp(5), width - split - ui.dp(10), height, t("FRIEND", "دوستت"), 1, !ready3d);
            paint.setColor(Ui.LIME); paint.setStrokeWidth(ui.dp(1)); canvas.drawLine(split, 0, split, height, paint);
            if (s.paused) {
                paint.setColor(0xc7080d19); canvas.drawRect(0, 0, width, height, paint); paint.setColor(Ui.TEXT);
                paint.setTextSize(ui.dp(22)); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(ui.bold);
                canvas.drawText(t("PAUSED", "مکث"), width * .5f, height * .5f, paint);
            }
        }
        private void track(Canvas canvas, FlightSnapshot frame, float left, float width, float height, String label, int index, boolean drawScene) {
            paint.setTypeface(ui.bold); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(ui.dp(12)); paint.setColor(Ui.TEXT);
            canvas.drawText(label, left + width * .5f, ui.dp(19), paint);
            if (!drawScene || frame == null) return;
            float top = ui.dp(30); canvas.save(); canvas.translate(left, top); canvas.clipRect(0, 0, width, Math.max(1, height - top));
            painters[index].draw(canvas, frame, width, Math.max(1, height - top), SceneAssets.color(frame.world)); canvas.restore();
        }
    }
}