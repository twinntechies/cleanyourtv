package com.gathin.wificontrol;

import android.app.Activity;
import android.content.ComponentName;

/** Both screens act only for the cleanyourtv launcher, which starts them for a result so the caller is known. */
final class Caller {
    private Caller() {}

    static boolean isLauncher(Activity activity) {
        ComponentName caller = activity.getCallingActivity();
        return caller != null && "com.gathin.tvhome".equals(caller.getPackageName());
    }
}
