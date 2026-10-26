package io.github.luyzfernando08.sleeptimer;

import android.accessibilityservice.AccessibilityService;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * A bedtime's last minute: a slim bar at the top of the screen, over whatever is playing, with the seconds left and
 * a line that drains to zero. It takes no focus and no keys, so the app underneath keeps the remote; at zero the TV
 * goes to sleep. Drawn as an accessibility overlay by SleepService, so it needs no draw-over-apps permission.
 */
final class BedtimeCountdown {
    private final AccessibilityService service;
    private final WindowManager windows;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private View bar;
    private TextView seconds;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long end = Timer.end(service);
            if (end == 0) {                                    // turned off or moved from the launcher
                hide();
                return;
            }
            if (!service.getSystemService(PowerManager.class).isInteractive()) {   // switched off with the remote
                hide();
                Timer.skip(service);
                return;
            }
            long left = (end - System.currentTimeMillis() + 999) / 1000;
            if (left <= 0) {
                hide();
                Timer.sleepNow(service);
                return;
            }
            seconds.setText(String.format(Locale.getDefault(), "%d:%02d", left / 60, left % 60));
            handler.postDelayed(this, Math.max(1, (end - System.currentTimeMillis()) % 1000));
        }
    };

    BedtimeCountdown(AccessibilityService service) {
        this.service = service;
        windows = service.getSystemService(WindowManager.class);
    }

    void show() {
        if (bar != null) return;
        long end = Timer.end(service);
        if (end == 0) return;
        bar = build(Math.max(0, end - System.currentTimeMillis()));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = Ui.dp(service, 28);
        windows.addView(bar, lp);
        bar.setAlpha(0f);
        bar.setTranslationY(-Ui.dp(service, 24));
        bar.animate().alpha(1f).translationY(0f).setDuration(350).setInterpolator(new DecelerateInterpolator()).start();
        handler.post(tick);
    }

    void hide() {
        handler.removeCallbacks(tick);
        if (bar == null) return;
        windows.removeView(bar);
        bar = null;
    }

    private View build(long leftMs) {
        LinearLayout box = new LinearLayout(service);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable glass = new GradientDrawable();
        glass.setColor(0xE6161826);
        glass.setCornerRadius(Ui.dp(service, 22));
        glass.setStroke(Math.max(1, Ui.dp(service, 1) / 2), 0x33FFFFFF);
        box.setBackground(glass);
        int padH = Ui.dp(service, 22), padV = Ui.dp(service, 14);
        box.setPadding(padH, padV, padH, padV);

        LinearLayout row = new LinearLayout(service);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView moon = new ImageView(service);
        moon.setImageResource(R.drawable.ic_moon);
        int icon = Ui.dp(service, 24);
        row.addView(moon, new LinearLayout.LayoutParams(icon, icon));
        TextView title = Ui.text(service, 18, 0xE6FFFFFF);
        title.setText(R.string.bedtime_countdown);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = Ui.dp(service, 12);
        tlp.rightMargin = Ui.dp(service, 24);
        row.addView(title, tlp);
        seconds = Ui.text(service, 26, Color.WHITE);
        seconds.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        row.addView(seconds);
        box.addView(row, new LinearLayout.LayoutParams(Ui.dp(service, 440), LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout track = new FrameLayout(service);
        track.setBackground(Ui.pill(service, 0x26FFFFFF));
        View fill = new View(service);
        GradientDrawable glow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFB9B2FF, 0xFFFF9F0A});
        glow.setCornerRadius(Ui.dp(service, 2));
        fill.setBackground(glow);
        fill.setPivotX(0f);
        track.addView(fill, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(service, 4));
        plp.topMargin = Ui.dp(service, 12);
        box.addView(track, plp);

        float from = Math.min(1f, leftMs / (float) Timer.WARN_MS);
        ObjectAnimator drain = ObjectAnimator.ofFloat(fill, View.SCALE_X, from, 0f);
        drain.setDuration(leftMs);
        drain.setInterpolator(new LinearInterpolator());
        drain.start();
        return box;
    }
}
