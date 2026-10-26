package io.github.luyzfernando08.sleeptimer;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * Puts the TV to sleep for Timer. The "lock screen" global action (Android 9 and later) sends the device to
 * sleep, which an ordinary app cannot do otherwise; a device admin's lockNow() is ignored on boxes without the
 * device-admin feature, the Mi Box S among them. The service asks for no events and reads no windows. It also
 * draws a bedtime's countdown bar (BedtimeCountdown), as an overlay that takes no focus.
 */
public class SleepService extends AccessibilityService {
    private static SleepService instance;
    private BedtimeCountdown countdown;

    static boolean running() {
        return instance != null;
    }

    static boolean sleep() {
        SleepService service = instance;
        return service != null && service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    /** Shows the bedtime countdown bar. False when the service is off, for the caller's fallback. */
    static boolean showCountdown() {
        SleepService service = instance;
        if (service == null) return false;
        service.countdown.show();
        return true;
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
        countdown = new BedtimeCountdown(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (countdown != null) countdown.hide();
        instance = null;
        super.onDestroy();
    }
}
