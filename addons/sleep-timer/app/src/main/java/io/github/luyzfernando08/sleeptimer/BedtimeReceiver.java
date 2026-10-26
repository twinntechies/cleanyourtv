package io.github.luyzfernando08.sleeptimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarms do not survive a reboot or follow a clock change, so the bedtime is armed again after either. */
public class BedtimeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Timer.end(context) == 0 || Timer.endIsBedtime(context)) Timer.scheduleBedtime(context);
    }
}
