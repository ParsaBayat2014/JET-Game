package com.toosarax.jetjo.game;

import android.content.Context;
import android.app.ActivityManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.core.GameEngine;
import com.toosarax.jetjo.data.Catalog;
import com.toosarax.jetjo.data.Profile;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Real GLES2 perspective mesh scene. GameView owns simulation; this surface only consumes detached frames. */
@android.annotation.SuppressLint("ViewConstructor")
public final class FlightGLView extends GLSurfaceView {
    private static final String LOG_TAG = "Jet3D";
    private final Profile profile;
    private final SceneRenderer renderer = new SceneRenderer();
    private final AtomicBoolean failed = new AtomicBoolean();
    private final AtomicBoolean failureNotified = new AtomicBoolean();
    private volatile FlightSceneFrame snapshot;
    private volatile FlightSceneFrame drawnLayout;
    private volatile Runnable failureListener;
    private volatile Runnable readyListener;
    private volatile boolean released;
    private volatile boolean sceneReady;
    private volatile int sceneGeneration;
    private int readyNotifiedGeneration = -1;
    private volatile String failureReason = "", gpuDescription = "GPU not initialized";
    private boolean resumed;
    private int selectedWorld;
    public FlightGLView(Context context, Profile profile) {
        super(context); this.profile = profile; selectedWorld = Math.max(0, Math.min(SceneAssets.COUNT - 1, profile.zone));
        setEGLContextClientVersion(2); setEGLConfigChooser(8, 8, 8, 0, 16, 0); setPreserveEGLContextOnPause(true);
        setRenderer(renderer); setRenderMode(RENDERMODE_WHEN_DIRTY); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false); setClickable(false);
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null || manager.getDeviceConfigurationInfo().reqGlEsVersion < 0x20000) {
            int requested = manager == null ? 0 : manager.getDeviceConfigurationInfo().reqGlEsVersion;
            setVisibility(View.GONE); fail("GLES2 unavailable; reportedVersion=0x" + Integer.toHexString(requested), null);
        }
    }
    /** UI-thread publication is synchronous; the GL thread never observes live engine or profile state. */
    public void publish(GameEngine engine, float clock) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Publish flight on the simulation UI thread");
        if (released || failed.get()) return;
        snapshot = FlightSceneFrame.single(FlightSnapshot.capture(engine, profile.skin, profile.trail, clock, profile.reducedMotion, profile.highContrast, selectedWorld)); requestRender();
    }
    /** Two detached snapshots are published together and rendered into one EGL surface/context. */
    public void publishPair(FlightSnapshot left, FlightSnapshot right, float leftFraction) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Publish paired flight on the UI thread");
        if (released || failed.get()) return;
        snapshot = FlightSceneFrame.pair(left, right, leftFraction); requestRender();
    }
    public void setWorld(int world) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Select flight world on the UI thread");
        selectedWorld = Math.max(0, Math.min(SceneAssets.COUNT - 1, world));
    }
    /** Select the lane nearest its rendered collision-plane center, including short/wide viewports. */
    public int laneForScreenX(float x, int width, int height) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Select flight lane on the UI thread");
        FlightSceneFrame publication = snapshot; FlightSnapshot current = publication == null ? null : publication.left;
        float camera = current == null ? 0 : FlightCamera.cameraOffset(current.playerLane, current.reducedMotion);
        return FlightCamera.laneForScreenX(x, width, height, camera);
    }
    public int laneForScreenX(float x, int width, int height, int player) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Select paired flight lane on the UI thread");
        FlightSceneFrame publication = snapshot;
        if (publication == null || !publication.paired()) return laneForScreenX(x, width, height);
        int left = publication.leftWidth(width), viewportWidth = player == 1 ? width - left : left;
        FlightSnapshot current = player == 1 ? publication.right : publication.left;
        return FlightCamera.laneForScreenX(x - (player == 1 ? left : 0), viewportWidth, height, FlightCamera.cameraOffset(current.playerLane, current.reducedMotion));
    }
    public void setFailureListener(Runnable listener) { failureListener = listener; if (failed.get() && listener != null) postFailure(listener); }
    /** True only after this GL context has drawn a complete game frame without a GL error. */
    public boolean sceneReady() {
        FlightSceneFrame latest = snapshot, drawn = drawnLayout;
        return sceneReady && !released && !failed.get() && latest != null && drawn != null
                && latest.paired() == drawn.paired() && latest.leftFraction == drawn.leftFraction;
    }
    public String failureReason() { return failureReason; }
    /** Register on the UI thread. A recreated EGL context or resized surface receives a fresh ready notification. */
    public void setReadyListener(Runnable listener) {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Register flight status on the UI thread");
        readyListener = listener;
        if (sceneReady() && listener != null) postReady(listener, sceneGeneration);
    }
    private void postReady(Runnable listener, int generation) {
        post(() -> {
            if (sceneReady() && generation == sceneGeneration && readyListener == listener && readyNotifiedGeneration != generation) {
                readyNotifiedGeneration = generation; listener.run();
            }
        });
    }
    private void markReady(FlightSceneFrame publication) {
        if (released || failed.get()) return;
        drawnLayout = publication;
        if (sceneReady) return;
        sceneReady = true; Log.i(LOG_TAG, "First 3D frame drawn; viewports=" + (publication.paired() ? 2 : 1) + "; generation=" + sceneGeneration + "; " + gpuDescription);
        Runnable listener = readyListener; if (listener != null) postReady(listener, sceneGeneration);
    }
    private void postFailure(Runnable listener) {
        post(() -> { if (!released && failureListener == listener && failureNotified.compareAndSet(false, true)) listener.run(); });
    }
    private void fail(String reason, Throwable cause) {
        sceneReady = false;
        if (failed.compareAndSet(false, true)) {
            failureReason = reason + (cause == null ? "" : ": " + cause.getClass().getSimpleName() + " · " + cause.getMessage());
            if (cause == null) Log.w(LOG_TAG, failureReason + "; " + gpuDescription); else Log.w(LOG_TAG, failureReason + "; " + gpuDescription, cause);
            Runnable listener = failureListener; if (listener != null) postFailure(listener);
        }
    }
    public void resumeScene() { if (released || resumed) return; resumed = true; onResume(); requestRender(); }
    public void pauseScene() { if (!resumed) return; resumed = false; onPause(); }
    public void releaseScene() {
        if (released) return; released = true; sceneReady = false; snapshot = null; drawnLayout = null; failureListener = null; readyListener = null;
        // If already paused, detach tears down the EGL context and reclaims its GPU objects.
        // Do not resume a scene just to run deletion while a menu is covering it.
        queueEvent(renderer::dispose); pauseScene();
    }
    @Override protected void onDetachedFromWindow() { releaseScene(); super.onDetachedFromWindow(); }

    private final class SceneRenderer implements Renderer {
        private static final String VERTEX =
                "uniform mat4 uMvp; uniform mat4 uModel; uniform mat3 uNormal; uniform vec3 uCamera;" +
                "attribute vec3 aPosition; attribute vec3 aNormal; varying float vLight; varying float vFog;" +
                "void main(){ vec4 w=uModel*vec4(aPosition,1.0);" +
                "vec3 n=normalize(uNormal*aNormal); vLight=0.30+0.70*max(dot(n,normalize(vec3(-0.45,0.8,0.35))),0.0);" +
                "vFog=clamp((length(w.xyz-uCamera)-12.0)/85.0,0.0,1.0); gl_Position=uMvp*vec4(aPosition,1.0); }";
        private static final String FRAGMENT =
                "precision mediump float; uniform vec4 uColor; uniform vec3 uFogColor; uniform float uEmission;" +
                "varying float vLight; varying float vFog; void main(){" +
                "vec3 light=uColor.rgb*mix(vLight,1.0,uEmission);" +
                "gl_FragColor=vec4(mix(light,uFogColor,vFog*vFog),uColor.a); }";
        private FlightMesh cube, ship, octa, sphere, cone, ring, disk;
        private int program, position, normal, mvpHandle, modelHandle, normalHandle, colorHandle, emissionHandle, fogHandle, cameraHandle;
        private final float[] projection = new float[16], view = new float[16], viewProjection = new float[16], model = new float[16], mvp = new float[16], normalMatrix = new float[9];
        private float aspect = .56f, time, travel, cameraX, fogR, fogG, fogB;
        private int surfaceWidth = 1, surfaceHeight = 1;
        private int accent;
        private boolean ready;
        private FlightSnapshot frame;
        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            ready = false; sceneReady = false; drawnLayout = null; sceneGeneration++;
            if (released || failed.get()) return;
            try {
                gpuDescription = "vendor=" + GLES20.glGetString(GLES20.GL_VENDOR) + "; renderer=" + GLES20.glGetString(GLES20.GL_RENDERER) + "; version=" + GLES20.glGetString(GLES20.GL_VERSION);
                Log.i(LOG_TAG, "Surface created; generation=" + sceneGeneration + "; " + gpuDescription);
                int vertex = 0, fragment = 0;
                try {
                    vertex = shader(GLES20.GL_VERTEX_SHADER, VERTEX); fragment = shader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT);
                    program = GLES20.glCreateProgram(); GLES20.glAttachShader(program, vertex); GLES20.glAttachShader(program, fragment); GLES20.glLinkProgram(program);
                    int[] status = new int[1]; GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
                    String info = GLES20.glGetProgramInfoLog(program);
                    if (info != null && !info.trim().isEmpty()) Log.w(LOG_TAG, "Program link log: " + info);
                    if (status[0] == 0) throw new IllegalStateException("Flight shader link; info=" + info);
                } finally {
                    if (vertex != 0) GLES20.glDeleteShader(vertex); if (fragment != 0) GLES20.glDeleteShader(fragment);
                }
                position = GLES20.glGetAttribLocation(program, "aPosition"); normal = GLES20.glGetAttribLocation(program, "aNormal");
                mvpHandle = GLES20.glGetUniformLocation(program, "uMvp"); modelHandle = GLES20.glGetUniformLocation(program, "uModel"); normalHandle = GLES20.glGetUniformLocation(program, "uNormal");
                colorHandle = GLES20.glGetUniformLocation(program, "uColor"); emissionHandle = GLES20.glGetUniformLocation(program, "uEmission"); fogHandle = GLES20.glGetUniformLocation(program, "uFogColor"); cameraHandle = GLES20.glGetUniformLocation(program, "uCamera");
                if (position < 0 || normal < 0 || mvpHandle < 0 || modelHandle < 0 || normalHandle < 0 || colorHandle < 0 || emissionHandle < 0 || fogHandle < 0 || cameraHandle < 0) throw new IllegalStateException("Flight shader contract");
                cube = FlightMesh.cube(); ship = FlightMesh.ship(); octa = FlightMesh.octahedron(); sphere = FlightMesh.sphere(); cone = FlightMesh.cone(); ring = FlightMesh.ring(); disk = FlightMesh.disk();
                cube.upload(); ship.upload(); octa.upload(); sphere.upload(); cone.upload(); ring.upload(); disk.upload();
                GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glDepthFunc(GLES20.GL_LEQUAL); GLES20.glDisable(GLES20.GL_CULL_FACE);
                GLES20.glEnable(GLES20.GL_BLEND); GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                checkGl("Flight GL setup"); ready = true;
            } catch (RuntimeException | OutOfMemoryError exception) {
                int error = GLES20.glGetError(); fail("Surface setup failed; pending GLerror=0x" + Integer.toHexString(error), exception); dispose();
            }
        }
        private int shader(int type, String code) {
            int shader = GLES20.glCreateShader(type); if (shader == 0) throw new IllegalStateException("Flight shader creation");
            GLES20.glShaderSource(shader, code); GLES20.glCompileShader(shader); int[] compiled = new int[1]; GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            String info = GLES20.glGetShaderInfoLog(shader); String stage = type == GLES20.GL_VERTEX_SHADER ? "vertex" : "fragment";
            if (info != null && !info.trim().isEmpty()) Log.w(LOG_TAG, stage + " shader log: " + info);
            if (compiled[0] == 0) { GLES20.glDeleteShader(shader); throw new IllegalStateException("Flight " + stage + " shader compilation; info=" + info); } return shader;
        }
        private void checkGl(String stage) { int error = GLES20.glGetError(); if (error != GLES20.GL_NO_ERROR) throw new IllegalStateException(stage + "; GLerror=0x" + Integer.toHexString(error)); }
        @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
            int nextWidth = Math.max(1, width), nextHeight = Math.max(1, height);
            if (surfaceWidth != nextWidth || surfaceHeight != nextHeight) {
                sceneReady = false; drawnLayout = null; sceneGeneration++;
                Log.i(LOG_TAG, "Surface resized; generation=" + sceneGeneration + "; width=" + nextWidth + "; height=" + nextHeight);
            }
            surfaceWidth = nextWidth; surfaceHeight = nextHeight;
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
        }
        @Override public void onDrawFrame(GL10 gl) {
            if (!ready || released || failed.get()) { GLES20.glClearColor(.03f, .05f, .09f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); return; }
            FlightSceneFrame publication = snapshot;
            if (publication == null) { GLES20.glClearColor(.03f, .05f, .09f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); return; }
            try {
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                int left = publication.leftWidth(surfaceWidth);
                renderViewport(publication.left, 0, left);
                if (publication.paired() && surfaceWidth - left > 0) renderViewport(publication.right, left, surfaceWidth - left);
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0); GLES20.glDisableVertexAttribArray(position); GLES20.glDisableVertexAttribArray(normal);
                checkGl("Flight GL draw"); markReady(publication);
            } catch (RuntimeException | OutOfMemoryError exception) {
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST); ready = false; int error = GLES20.glGetError(); fail("Frame failed; pending GLerror=0x" + Integer.toHexString(error), exception);
            }
        }
        private void renderViewport(FlightSnapshot viewportFrame, int left, int width) {
                frame = viewportFrame;
                GLES20.glViewport(left, 0, width, surfaceHeight); GLES20.glScissor(left, 0, width, surfaceHeight);
                aspect = Math.max(.25f, width / (float)surfaceHeight);
                Matrix.perspectiveM(projection, 0, FlightCamera.verticalFov(aspect), aspect, .2f, 130);
                time = frame.reducedMotion ? 0 : frame.clock; travel = frame.reducedMotion ? 0 : frame.clock * (11 + frame.speed * 24);
                accent = SceneAssets.color(frame.world); fogR = .024f + ((accent >>> 16) & 255) / 255f * .033f; fogG = .035f + ((accent >>> 8) & 255) / 255f * .035f; fogB = .065f + (accent & 255) / 255f * .045f;
                GLES20.glClearColor(fogR, fogG, fogB, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); GLES20.glUseProgram(program);
                cameraX = FlightCamera.cameraOffset(frame.playerLane, frame.reducedMotion);
                Matrix.setLookAtM(view, 0, cameraX, FlightCamera.CAMERA_HEIGHT, FlightCamera.CAMERA_Z, cameraX * FlightCamera.AIM_X_FACTOR, FlightCamera.AIM_HEIGHT, FlightCamera.AIM_Z, 0, 1, 0); Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0);
                GLES20.glUniform3f(fogHandle, fogR, fogG, fogB); GLES20.glUniform3f(cameraHandle, cameraX, FlightCamera.CAMERA_HEIGHT, FlightCamera.CAMERA_Z);
                road(); scenery(); entities(); if (frame.bossActive) boss(); if (frame.alive) pilot(); streaks();
        }
        private float bend(float z) { return ((float)Math.sin(z * .035f + time * .12f) - (float)Math.sin(time * .12f)) * 1.9f; }
        private float lane(float lane, float z) { return bend(z) + (lane - 1) * FlightCamera.LANE_SPACING; }
        private float roll() { return frame.reducedMotion ? 0 : Math.max(-25, Math.min(25, (frame.targetLane - frame.playerLane) * -26)); }
        private void road() {
            for (int i = 0; i < 17; i++) {
                float z = 9 - i * 6, x = bend(z), yaw = (float)Math.toDegrees(Math.atan2(bend(z + 3) - bend(z - 3), 6));
                draw(cube, x, -.22f, z, 7.2f, .42f, 6.12f, yaw, 0, 0, frame.highContrast ? 0xff183341 : 0xff11242f, .1f, 1);
                for (int line = 0; line < 4; line++) {
                    float xx = x + (line - 1.5f) * 2.3f;
                    draw(cube, xx, .018f, z, line == 0 || line == 3 ? .055f : .030f, .040f, 6.1f, yaw, 0, 0, accent, .9f, 1);
                }
                if ((i & 1) == 0) draw(cube, x, .024f, z + (travel % 6) - 3, 6.8f, .025f, .07f, yaw, 0, 0, 0xff76a9b7, .65f, .60f);
            }
        }
        private void scenery() {
            for (int i = 0; i < 12; i++) {
                float z = 7 - ((i * 8 + travel * .56f) % 103), h = 2.8f + (i * 17 % 8) * .65f;
                for (int side = -1; side <= 1; side += 2) {
                    float x = bend(z) + side * (7.8f + i % 3 * 1.5f); int color = (frame.world % 3 == 1) ? 0xff423753 : (frame.world % 3 == 2) ? 0xff243f54 : 0xff203746;
                    draw(cube, x, h * .5f, z, 2.2f + i % 3 * .5f, h, 3.3f, side * (i % 3) * 8, 0, 0, color, .08f, 1);
                    if (i % 2 == 0) draw(cube, x - side * 1.18f, h * .58f, z, .055f, h * .70f, .30f, 0, 0, 0, accent, .92f, 1);
                }
            }
            if (frame.world % 4 == 0 || frame.bossActive) for (int i = 0; i < 5; i++) {
                float z = -7 - ((i * 15 + travel * .35f) % 77), x = bend(z);
                draw(cube, x - 4.0f, 3.0f, z, .25f, 6, .35f, 0, 0, 0, accent, .78f, 1);
                draw(cube, x + 4.0f, 3.0f, z, .25f, 6, .35f, 0, 0, 0, accent, .78f, 1);
                draw(cube, x, 5.9f, z, 8.25f, .20f, .38f, 0, 0, 0, accent, .85f, 1);
            }
            draw(sphere, -19, 17, -63, 8, 8, 8, 0, 0, 0, (frame.world % 2 == 0) ? 0xff477192 : 0xffb78371, .08f, 1);
            draw(ring, -19, 17, -63, 13, 13, 13, 0, 25, 16, accent, .75f, .85f);
            for (int i = 0; i < 24; i++) {
                float z = -25 - (i * 17 % 65), x = (i * 31 % 63) - 31, y = 7 + i * 11 % 15;
                draw(octa, x, y, z, .10f, .10f, .10f, 0, 0, 0, i % 3 == 0 ? accent : 0xffd2e8ff, 1, 1);
            }
        }
        private void entities() {
            for (int i = 0; i < frame.count; i++) {
                float z = (frame.y[i] - GameConfig.PLAYER_Y) * 78; if (z > 12 || z < -96) continue; float x = lane(frame.lane[i], z); int type = frame.type[i];
                if (type == GameEngine.WALL) {
                    shadow(x, z, .9f, .7f, .3f);
                    if (frame.pattern[i] == GameEngine.METEORS) draw(octa, x, .80f, z, .85f, .85f, .85f, time * 24 + i * 15, time * 11, 0, 0xffed8c71, .08f, 1);
                    else { draw(cube, x, .72f, z, 1.48f, 1.45f, .90f, 0, 0, 0, 0xffbb4765, .10f, 1); draw(cube, x, .78f, z + .47f, 1.0f, .16f, .06f, 0, 0, 0, 0xffffa481, .88f, 1); }
                } else if (type == 6) {
                    draw(cube, x, .34f, z, 1.60f, .66f, .58f, 0, 0, 0, 0xffff914e, .24f, 1); draw(cube, x, .70f, z, 1.66f, .09f, .65f, 0, 0, 0, 0xffffd884, .95f, 1);
                } else if (type == 7) {
                    // Lower edge 1.185: upright ship cockpit intersects, ducking animals clear.
                    draw(cube, x, 1.40f, z, 1.87f, .43f, .45f, 0, 0, 0, 0xffe66bb8, .75f, 1);
                    draw(cube, x - .98f, .75f, z, .12f, 1.55f, .28f, 0, 0, 0, 0xff765277, .18f, 1); draw(cube, x + .98f, .75f, z, .12f, 1.55f, .28f, 0, 0, 0, 0xff765277, .18f, 1);
                } else if (type == 8) draw(ring, x, 1.45f, z, 1.4f, 1.4f, 1.4f, 0, 90, 0, 0xff70ffe0, .95f, 1);
                else if (type == GameEngine.ENERGY) draw(octa, x, .9f + (frame.reducedMotion ? 0 : (float)Math.sin(time * 3 + i) * .10f), z, .28f, .48f, .28f, time * 80, 0, 0, 0xffffde7f, .65f, 1);
                else {
                    int color = type == GameEngine.SHIELD ? 0xff7beaff : type == GameEngine.MAGNET ? 0xffc5a1ff : type == GameEngine.SLOW ? 0xffffd77f : 0xff8cffb9;
                    draw(octa, x, .92f, z, .52f, .62f, .52f, time * 43, 0, 0, color, .75f, 1); draw(ring, x, .92f, z, .83f, .83f, .83f, time * 20, 45, 15, color, .95f, 1);
                }
            }
        }
        private void pilot() {
            float x = lane(frame.playerLane, 0), height = FlightCamera.PLAYER_HEIGHT + frame.jumpHeight * 2.6f - (frame.duckTime > 0 ? .30f : 0), compression = frame.duckTime > 0 ? .45f : 1;
            int skin = Math.max(0, Math.min(Catalog.COLORS.length - 1, frame.skin)), color = Catalog.COLORS[skin];
            shadow(x, 0, .90f + frame.jumpHeight * .18f, 1.25f, Math.max(.12f, .45f - frame.jumpHeight * .20f));
            float bank = roll(), bounce = frame.reducedMotion ? 0 : (float)Math.sin(time * 4) * .03f;
            if (skin >= 18) character(x, height + bounce, compression, skin, color, bank);
            else {
                draw(ship, x, height + bounce, 0, .66f, compression * .95f, .79f, 0, frame.jumpHeight * -8, bank, color, .14f, 1);
                draw(sphere, x, height + compression * .22f, -.25f, .20f, compression * .19f, .39f, 0, 0, bank, 0xffd9efff, .3f, 1);
                if (skin % 3 == 1) { draw(cube, x - .58f, height, .35f, .18f, .14f * compression, .65f, 0, 0, bank, 0xffeef3ff, .2f, 1); draw(cube, x + .58f, height, .35f, .18f, .14f * compression, .65f, 0, 0, bank, 0xffeef3ff, .2f, 1); }
            }
            float flame = frame.reducedMotion ? .6f : .7f + (float)Math.sin(time * 38) * .12f; if (frame.overdrive > 0) flame *= 1.8f;
            draw(cone, x - .19f, height -.05f, .75f, .22f, flame, .22f, 0, 90, bank, frame.overdrive > 0 ? 0xffffd97c : color, 1, 1);
            draw(cone, x + .19f, height -.05f, .75f, .22f, flame, .22f, 0, 90, bank, frame.overdrive > 0 ? 0xffffd97c : color, 1, 1);
            if (frame.overdrive > 0 || frame.powers[0] > 0) {
                int halo = frame.overdrive > 0 ? 0xffffdc81 : 0xff83eaff;
                draw(ring, x, height, 0, 1.22f, 1.22f, 1.22f, 0, 63, time * 28, halo, .95f, .85f); draw(ring, x, height, 0, 1.23f, 1.23f, 1.23f, 0, -35, -time * 18, halo, .9f, .70f);
            }
            if (frame.powers[1] > 0) draw(ring, x, .05f, 0, 2.7f, 2.7f, 2.7f, time * 15, 0, 0, 0xffc5a1ff, .95f, .4f);
        }
        private void character(float x, float y, float compression, int skin, int color, float bank) {
            int variant = (skin - 18) % 6;
            draw(sphere, x, y, 0, .52f, .38f * compression, .72f, 0, 0, bank, color, .12f, 1);
            draw(sphere, x, y + .27f * compression, -.35f, .41f, .41f * compression, .43f, 0, 0, bank, color, .15f, 1);
            draw(sphere, x - .14f, y + .32f * compression, -.75f, .07f, .095f * compression, .035f, 0, 0, bank, 0xff122535, .18f, 1);
            draw(sphere, x + .14f, y + .32f * compression, -.75f, .07f, .095f * compression, .035f, 0, 0, bank, 0xff122535, .18f, 1);
            if (variant == 0 || variant == 1 || variant == 4) {
                float ear = variant == 0 ? .85f : .35f;
                draw(cone, x - .23f, y + .55f * compression, -.32f, .31f, ear * compression, .27f, 0, 0, bank - 12, color, .12f, 1);
                draw(cone, x + .23f, y + .55f * compression, -.32f, .31f, ear * compression, .27f, 0, 0, bank + 12, color, .12f, 1);
            } else if (variant == 2 || variant == 5) {
                draw(ship, x - .56f, y, .08f, .42f, .30f * compression, .30f, -75, 0, bank, color, .18f, 1);
                draw(ship, x + .56f, y, .08f, .42f, .30f * compression, .30f, 75, 0, bank, color, .18f, 1);
            } else {
                draw(sphere, x, y + .13f, .21f, .61f, .33f * compression, .56f, 0, 0, bank, 0xff4f937b, .12f, 1);
            }
        }
        private void boss() {
            float z = -13 + (frame.reducedMotion ? 0 : (float)Math.sin(time * .75f) * 1.5f), x = bend(z) + (frame.reducedMotion ? 0 : (float)Math.sin(time * .8f) * 2.1f), y = 3.0f;
            int color = frame.bossWarning > 0 ? 0xffff667e : frame.bossPhase % 2 == 0 ? 0xffa7a3ff : 0xff7cddf2;
            shadow(x, z, 2.3f, 1.15f, .3f); draw(ship, x, y, z, 1.9f, 1.4f, 1.3f, 180, 0, (float)Math.sin(time) * 5, color, .12f, 1);
            draw(sphere, x, y + .20f, z, .75f, .45f, .60f, 0, 0, 0, 0xff2a344f, .08f, 1);
            draw(octa, x -.46f, y + .20f, z + .56f, .15f, .14f, .18f, 0, 0, 0, 0xffffa883, .95f, 1); draw(octa, x +.46f, y + .20f, z + .56f, .15f, .14f, .18f, 0, 0, 0, 0xffffa883, .95f, 1);
            draw(ring, x, y, z, 2.9f, 2.9f, 2.9f, time * 15, 25, 0, color, .88f, .8f);
            draw(cube, x, 4.4f, z, 3.8f, .12f, .14f, 0, 0, 0, 0xff374559, .45f, 1);
            float health = Math.max(0, Math.min(1, frame.bossHealth)); if (health > 0) draw(cube, x - (1 - health) * 1.9f, 4.4f, z + .02f, 3.8f * health, .13f, .17f, 0, 0, 0, 0xffbaffb5, .95f, 1);
        }
        private void streaks() {
            if (frame.reducedMotion) return; float boost = frame.overdrive > 0 ? 1 : Math.min(1, (frame.speed - .24f) * 3);
            if (boost <= .08f) return;
            for (int i = 0; i < 10; i++) { float z = 9 - ((i * 11 + travel * 1.6f) % 81), x = (i % 2 == 0 ? -1 : 1) * (4.8f + i % 3 * .8f) + bend(z);
                draw(cube, x, .35f + i % 3 * .60f, z, .025f, .025f, 3 + boost * 5, 0, 0, 0, frame.overdrive > 0 ? 0xffffdc81 : accent, 1, .7f);
            }
        }
        private void shadow(float x, float z, float width, float depth, float alpha) { draw(disk, x, .032f, z, width, 1, depth, 0, 0, 0, 0xff030711, 1, alpha); }
        private void draw(FlightMesh mesh, float x, float y, float z, float sx, float sy, float sz, float yaw, float pitch, float roll, int color, float emission, float alpha) {
            Matrix.setIdentityM(model, 0); Matrix.translateM(model, 0, x, y, z); if (yaw != 0) Matrix.rotateM(model, 0, yaw, 0, 1, 0); if (pitch != 0) Matrix.rotateM(model, 0, pitch, 1, 0, 0); if (roll != 0) Matrix.rotateM(model, 0, roll, 0, 0, 1); Matrix.scaleM(model, 0, sx, sy, sz);
            // Inverse-transpose of rotation × diagonal scale, without per-object allocations.
            float ix = 1 / Math.max(.0001f, sx * sx), iy = 1 / Math.max(.0001f, sy * sy), iz = 1 / Math.max(.0001f, sz * sz);
            normalMatrix[0] = model[0] * ix; normalMatrix[1] = model[1] * ix; normalMatrix[2] = model[2] * ix;
            normalMatrix[3] = model[4] * iy; normalMatrix[4] = model[5] * iy; normalMatrix[5] = model[6] * iy;
            normalMatrix[6] = model[8] * iz; normalMatrix[7] = model[9] * iz; normalMatrix[8] = model[10] * iz;
            Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0); GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0); GLES20.glUniformMatrix4fv(modelHandle, 1, false, model, 0); GLES20.glUniformMatrix3fv(normalHandle, 1, false, normalMatrix, 0);
            GLES20.glUniform4f(colorHandle, ((color >>> 16) & 255) / 255f, ((color >>> 8) & 255) / 255f, (color & 255) / 255f, alpha); GLES20.glUniform1f(emissionHandle, emission); mesh.draw(position, normal);
        }
        private void dispose() {
            ready = false; sceneReady = false; if (cube != null) cube.delete(); if (ship != null) ship.delete(); if (octa != null) octa.delete(); if (sphere != null) sphere.delete(); if (cone != null) cone.delete(); if (ring != null) ring.delete(); if (disk != null) disk.delete();
            if (program != 0) { GLES20.glDeleteProgram(program); program = 0; }
        }
    }
}
