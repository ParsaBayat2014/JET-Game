package com.toosarax.jetjo.game;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;
import com.toosarax.jetjo.data.Catalog;

// Constructed programmatically with explicit game dependencies; never inflated from XML.
@android.annotation.SuppressLint("ViewConstructor")
public final class HeroView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Rect visible = new Rect();
    private final int skin;
    private final boolean compact, reduced;
    private final long started = SystemClock.uptimeMillis();
    private boolean framePosted;
    private final Runnable nextFrame = () -> {
        framePosted = false;
        if (canAnimate()) invalidate();
    };

    private final ViewTreeObserver.OnScrollChangedListener scrollCheck = () -> {
        if (!framePosted && canAnimate()) invalidate();
    };

    public HeroView(Context context, int skin, boolean compact, boolean reduced) {
        super(context);
        this.skin = Math.max(0, Math.min(Catalog.COLORS.length - 1, skin));
        this.compact = compact; this.reduced = reduced;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private boolean canAnimate() {
        return !reduced && isAttachedToWindow() && isShown() && hasWindowFocus()
                && getWindowVisibility() == VISIBLE && getGlobalVisibleRect(visible)
                && (Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled());
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        boolean animate = canAnimate();
        float time = animate ? ((SystemClock.uptimeMillis() - started) % 600000) / 1000f : 0;
        int accent = Catalog.COLORS[skin];
        p.setStyle(Paint.Style.FILL); p.setShader(null);
        if (!compact) {
            // Quiet runway framing puts the ship ahead of the decorative animation.
            p.setColor(0xff122838); p.setStrokeWidth(Math.max(1, width * .002f));
            for (int i = -5; i <= 5; i++) {
                canvas.drawLine(width * .5f + i * width * .10f, height * .1f,
                        width * .5f + i * width * .27f, height, p);
            }
            for (int i = 0; i < 7; i++) {
                float y = height * (.12f + i * i * .023f);
                canvas.drawLine(0, y, width, y, p);
            }
            float radius = Math.min(height * .37f, width * .30f);
            float centerX = width * .5f, centerY = height * .45f;
            p.setStyle(Paint.Style.STROKE); p.setColor((accent & 0xffffff) | 0x30000000);
            p.setStrokeWidth(Math.max(1, width * .002f));
            canvas.drawCircle(centerX, centerY, radius, p);
            p.setColor((accent & 0xffffff) | 0x70000000);
            float angle = animate ? -80 + time * 12 : -80;
            canvas.drawArc(centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius, angle, 74, false, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(accent);
            canvas.drawCircle(width * .83f, height * .18f, height * .014f, p);
            canvas.drawCircle(width * .14f, height * .69f, height * .010f, p);
            p.setColor((accent & 0xffffff) | 0x40000000);
            canvas.drawCircle(width * .77f, height * .74f, height * .024f, p);
        }
        float size = Math.min(height * (compact ? .27f : .26f), width * .25f);
        float y = height * .43f + (animate ? (float) Math.sin(time * 1.5f) * height * .018f : 0);
        Art.ship(canvas, p, path, width * .5f, y, size, skin, time, animate);
        if (animate && !framePosted) {
            framePosted = true;
            postDelayed(nextFrame, compact ? 48 : 33);
        }
    }

    private void stopFrames() {
        removeCallbacks(nextFrame); framePosted = false;
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(scrollCheck);
    }
    @Override protected void onDetachedFromWindow() {
        stopFrames();
        if (getViewTreeObserver().isAlive()) getViewTreeObserver().removeOnScrollChangedListener(scrollCheck);
        super.onDetachedFromWindow();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) stopFrames(); else invalidate();
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (!focused) stopFrames(); else invalidate();
    }
}
