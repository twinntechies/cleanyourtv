package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

import java.util.Calendar;

/**
 * analog_clock: the time as a small frosted-glass dial in the header instead of the digital clock. Twelve ticks,
 * white hour and minute hands and an orange seconds hand. It redraws once a second, and only while it is on screen.
 */
public class AnalogClockView extends View {

    private static final int SECONDS_COLOR = 0xFFFF9F0A;

    private final Paint face = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hand = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint seconds = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private boolean ticking;

    private final Runnable tickRunnable = new Runnable() {
        @Override public void run() {
            invalidate();
            postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
        }
    };

    public AnalogClockView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(density);
        rim.setColor(0x38FFFFFF);
        tick.setStrokeCap(Paint.Cap.ROUND);
        hand.setStrokeCap(Paint.Cap.ROUND);
        hand.setColor(0xFFFFFFFF);
        seconds.setStrokeCap(Paint.Cap.ROUND);
        seconds.setColor(SECONDS_COLOR);
        seconds.setStrokeWidth(1.2f * density);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        face.setShader(new LinearGradient(0, 0, 0, h, 0x2EFFFFFF, 0x14FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - density;
        canvas.drawCircle(cx, cy, r, face);
        canvas.drawCircle(cx, cy, r, rim);

        for (int i = 0; i < 12; i++) {
            boolean major = i % 3 == 0;
            tick.setColor(major ? 0xE6FFFFFF : 0x80FFFFFF);
            tick.setStrokeWidth((major ? 2f : 1.2f) * density);
            double a = Math.toRadians(i * 30);
            float outer = r * 0.86f, inner = r * (major ? 0.70f : 0.76f);
            float sx = (float) Math.sin(a), sy = (float) -Math.cos(a);
            canvas.drawLine(cx + sx * inner, cy + sy * inner, cx + sx * outer, cy + sy * outer, tick);
        }

        Calendar now = Calendar.getInstance();
        float sec = now.get(Calendar.SECOND);
        float min = now.get(Calendar.MINUTE) + sec / 60f;
        float hour = now.get(Calendar.HOUR) + min / 60f;
        hand.setStrokeWidth(3f * density);
        drawHand(canvas, cx, cy, hour * 30f, r * 0.48f, 0, hand);
        hand.setStrokeWidth(2f * density);
        drawHand(canvas, cx, cy, min * 6f, r * 0.72f, 0, hand);
        drawHand(canvas, cx, cy, sec * 6f, r * 0.80f, r * 0.18f, seconds);
        canvas.drawCircle(cx, cy, 2.4f * density, seconds);
    }

    private static void drawHand(Canvas canvas, float cx, float cy, float degrees, float length, float tail, Paint p) {
        double a = Math.toRadians(degrees);
        float sx = (float) Math.sin(a), sy = (float) -Math.cos(a);
        canvas.drawLine(cx - sx * tail, cy - sy * tail, cx + sx * length, cy + sy * length, p);
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        setTicking(visibility == VISIBLE);
    }

    @Override protected void onDetachedFromWindow() {
        setTicking(false);
        super.onDetachedFromWindow();
    }

    private void setTicking(boolean on) {
        if (on == ticking) return;
        ticking = on;
        removeCallbacks(tickRunnable);
        if (on) tickRunnable.run();
    }
}
