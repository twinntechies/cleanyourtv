package com.gathin.tvhome;

import android.content.Context;
import android.widget.TextView;

/**
 * bold_clock: the orange colon between the hour and the minutes. Lit for the first half of every second and dimmed
 * for the second, and it only blinks while it is on screen.
 */
public class BlinkingColonView extends TextView {

    private boolean blinking;

    private final Runnable blink = new Runnable() {
        @Override public void run() {
            long ms = System.currentTimeMillis() % 1000;
            setAlpha(ms < 500 ? 1f : 0.2f);
            postDelayed(this, ms < 500 ? 500 - ms : 1000 - ms);
        }
    };

    public BlinkingColonView(Context context) {
        super(context);
        setText(":");
        setTextColor(0xFFFF9F0A);
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        setBlinking(visibility == VISIBLE);
    }

    @Override protected void onDetachedFromWindow() {
        setBlinking(false);
        super.onDetachedFromWindow();
    }

    private void setBlinking(boolean on) {
        if (on == blinking) return;
        blinking = on;
        removeCallbacks(blink);
        if (on) blink.run();
        else setAlpha(1f);
    }
}
