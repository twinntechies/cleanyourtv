package io.github.luyzfernando08.sleeptimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/**
 * The one timer: when it ends, and the two alarms behind it. The warning alarm fires a minute early and opens
 * WarningActivity (or, for a bedtime, the countdown bar); the sleep alarm, a moment after the end, is the backstop
 * that puts the TV to sleep even if the warning never showed. A bedtime is a timer that sets itself again for the
 * same time the next day.
 */
final class Timer {
    private Timer() { }

    static final int[] MINUTES = {15, 30, 60, 90, 120};
    static final long WARN_MS = 60_000;
    static final String ACTION_WARN = "io.github.luyzfernando08.sleeptimer.WARN";
    static final String ACTION_SLEEP = "io.github.luyzfernando08.sleeptimer.SLEEP";
    static final String ACTION_TEST = "io.github.luyzfernando08.sleeptimer.TEST";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("timer", Context.MODE_PRIVATE);
    }

    /** When the TV goes to sleep, in wall-clock millis, or 0 when no timer is on. */
    static long end(Context ctx) {
        long end = prefs(ctx).getLong("end", 0);
        return end > System.currentTimeMillis() - 5_000 ? end : 0;
    }

    /** Whether the timer that is on was set by the bedtime rather than for a number of minutes. */
    static boolean endIsBedtime(Context ctx) {
        return prefs(ctx).getBoolean("end_is_bedtime", false);
    }

    /** The daily bedtime in minutes after midnight, or -1 when there is none. */
    static int bedtime(Context ctx) {
        return prefs(ctx).getInt("bedtime", -1);
    }

    static void start(Context ctx, int minutes) {
        startSeconds(ctx, minutes * 60);
    }

    static void startSeconds(Context ctx, int seconds) {
        startAt(ctx, System.currentTimeMillis() + seconds * 1000L, false);
    }

    static void setBedtime(Context ctx, int minuteOfDay) {
        prefs(ctx).edit().putInt("bedtime", minuteOfDay).apply();
        scheduleBedtime(ctx);
    }

    static void clearBedtime(Context ctx) {
        prefs(ctx).edit().remove("bedtime").apply();
        cancel(ctx);
    }

    /** Arms the timer for the next time the bedtime comes round, when one is set. */
    static void scheduleBedtime(Context ctx) {
        int minute = bedtime(ctx);
        if (minute < 0) return;
        Calendar at = Calendar.getInstance();
        at.set(Calendar.HOUR_OF_DAY, minute / 60);
        at.set(Calendar.MINUTE, minute % 60);
        at.set(Calendar.SECOND, 0);
        at.set(Calendar.MILLISECOND, 0);
        if (at.getTimeInMillis() <= System.currentTimeMillis() + 5_000) at.add(Calendar.DAY_OF_MONTH, 1);
        startAt(ctx, at.getTimeInMillis(), true);
    }

    private static void startAt(Context ctx, long end, boolean bedtime) {
        prefs(ctx).edit().putLong("end", end).putBoolean("end_is_bedtime", bedtime).apply();
        schedule(ctx, ACTION_WARN, Math.max(System.currentTimeMillis(), end - WARN_MS));
        schedule(ctx, ACTION_SLEEP, end + 2_000);
    }

    static void cancel(Context ctx) {
        prefs(ctx).edit().remove("end").remove("end_is_bedtime").apply();
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        am.cancel(pending(ctx, ACTION_WARN));
        am.cancel(pending(ctx, ACTION_SLEEP));
    }

    /** Ends the timer and puts the TV to sleep, then arms the next bedtime. False when the service is off. */
    static boolean sleepNow(Context ctx) {
        cancel(ctx);
        boolean slept = SleepService.sleep();
        scheduleBedtime(ctx);
        return slept;
    }

    /** The TV was already off when the timer ended: nothing to do but wait for the next bedtime. */
    static void skip(Context ctx) {
        cancel(ctx);
        scheduleBedtime(ctx);
    }

    static boolean canSleep() {
        return SleepService.running();
    }

    private static void schedule(Context ctx, String action, long at) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        PendingIntent pi = pending(ctx, action);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static PendingIntent pending(Context ctx, String action) {
        Intent intent = new Intent(ctx, AlarmReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
