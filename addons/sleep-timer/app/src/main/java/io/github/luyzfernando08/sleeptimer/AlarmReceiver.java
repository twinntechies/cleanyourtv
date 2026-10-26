package io.github.luyzfernando08.sleeptimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * The timer's two alarms: a minute early, the warning; just after the end, sleep if nobody answered. A TV that is
 * already off is left alone, and a bedtime then waits for the next day.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Timer.ACTION_TEST.equals(intent.getAction())) {
            // From adb only (the receiver needs WRITE_SECURE_SETTINGS): a timer of a few seconds, to try it out.
            Timer.startSeconds(context, intent.getIntExtra("seconds", 70));
            return;
        }
        long end = Timer.end(context);
        if (end == 0) {
            // Cancelled, or long past: some firmware (Xiaomi's) turns wakeup alarms into ordinary ones, so an alarm
            // due while the TV was in standby arrives when it is next switched on. That is a missed night, not a
            // reason to sleep; a bedtime waits for the next one.
            if (Timer.endIsBedtime(context)) Timer.skip(context);
            return;
        }
        boolean on = context.getSystemService(PowerManager.class).isInteractive();
        if (Timer.ACTION_WARN.equals(intent.getAction())) {
            if (!on) return;
            if (Timer.endIsBedtime(context) && SleepService.showCountdown()) return;
            context.startActivity(new Intent(context, WarningActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (Timer.ACTION_SLEEP.equals(intent.getAction()) && System.currentTimeMillis() >= end) {
            if (on) Timer.sleepNow(context);
            else Timer.skip(context);
        }
    }
}
