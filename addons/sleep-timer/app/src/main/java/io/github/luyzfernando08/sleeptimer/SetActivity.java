package io.github.luyzfernando08.sleeptimer;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Toast;

/**
 * Sets the timer with no screen of its own, for a launcher's dropdown: the int extra "minutes" starts a timer that
 * long (1 to 240), 0 cancels it and the bedtime; the int extra "bedtime" (minutes after midnight, 0 to 1439) sets a
 * daily bedtime. A toast says when the TV will go to standby, or that no timer is set.
 */
public class SetActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int bedtime = getIntent().getIntExtra("bedtime", -1);
        int minutes = getIntent().getIntExtra("minutes", -1);
        if (bedtime >= 0 && bedtime < 24 * 60) {
            Timer.setBedtime(this, bedtime);
            String at = DateFormat.getTimeFormat(this).format(Timer.end(this));
            Toast.makeText(this, Timer.canSleep() ? getString(R.string.bedtime_on, at) : getString(R.string.no_service),
                    Toast.LENGTH_LONG).show();
        } else if (minutes == 0) {
            Timer.clearBedtime(this);
            Toast.makeText(this, R.string.state_off, Toast.LENGTH_SHORT).show();
        } else if (minutes > 0 && minutes <= 240) {
            Timer.start(this, minutes);
            String at = DateFormat.getTimeFormat(this).format(Timer.end(this));
            Toast.makeText(this, Timer.canSleep() ? getString(R.string.state_on, at) : getString(R.string.no_service),
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
