package com.gathin.tvhome;

import android.content.Intent;
import android.service.dreams.DreamService;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Screensaver: the launcher's thin clock and date on black, over a slow ambient glow (ClockScene).
 * No network, no images: cheap enough for a 1 GB TV.
 * With idle_lock it is the lock screen instead: keys are swallowed and OK unlocks; woken any other way (Home, the TV
 * sleeping) it leaves LockActivity up, so OK is still needed.
 */
public class ClockDream extends DreamService {

    private ClockScene scene;
    private boolean lock;
    private boolean unlocked;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            lock = Config.load(this).idleLock;
        } catch (RuntimeException e) {
            lock = false;
        }
        setInteractive(lock);   // without idle_lock any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(lock ? R.layout.lock_screen : R.layout.dream_clock);
        scene = new ClockScene(getWindow().getDecorView());
        scene.useHomeWallpaper(this, lock);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!lock) return super.dispatchKeyEvent(event);
        int code = event.getKeyCode();
        boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (ok && event.getAction() == KeyEvent.ACTION_UP) {
            unlocked = true;
            LockActivity.locked = false;
            finish();
        }
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return lock || super.dispatchGenericMotionEvent(event);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        scene.start();
    }

    @Override
    public void onDreamingStopped() {
        scene.stop();
        keepLocked();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (scene != null) scene.stop();
        keepLocked();
        super.onDetachedFromWindow();
    }

    private void keepLocked() {
        if (!lock || unlocked) return;
        unlocked = true;   // once
        LockActivity.locked = true;
        try {
            startActivity(new Intent(this, LockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ignored) {
            // Home still brings the lock screen back (MainActivity.onResume).
        }
    }
}
