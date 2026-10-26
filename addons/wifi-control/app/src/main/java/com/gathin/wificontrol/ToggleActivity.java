package com.gathin.wificontrol;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;

public class ToggleActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean done = false;
        if (Caller.isLauncher(this)) {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            done = wm != null && wm.setWifiEnabled(getIntent().getBooleanExtra("enabled", true));
        }
        setResult(done ? RESULT_OK : RESULT_CANCELED);
        finish();
    }
}
