package com.gathin.wificontrol;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Joins one network for the launcher's Wi-Fi dropdown: a saved or open network at once, a protected one after a
 * password card. It waits up to 20 s for the connection and says so when the password looks wrong.
 */
@SuppressWarnings("deprecation")
public class JoinActivity extends Activity {
    private static final int BLUE = 0xFF0A84FF;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WifiManager wm;
    private String ssid;
    private String caps;
    private float dp;
    private EditText password;
    private TextView message;
    private View buttons;
    private long deadline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ssid = getIntent().getStringExtra("ssid");
        caps = getIntent().getStringExtra("caps");
        if (caps == null) caps = "";
        dp = getResources().getDisplayMetrics().density;
        if (!Caller.isLauncher(this) || wm == null || ssid == null || ssid.isEmpty() || caps.contains("EAP")) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setResult(RESULT_OK);
        WifiConfiguration saved = savedNetwork();
        boolean open = !caps.contains("PSK") && !caps.contains("SAE") && !caps.contains("WEP");
        if (saved != null) {
            connect(saved.networkId);
            finish();
        } else if (open) {
            WifiConfiguration config = baseConfig();
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            int id = wm.addNetwork(config);
            if (id != -1) connect(id);
            finish();
        } else {
            buildCard();
        }
    }

    private WifiConfiguration savedNetwork() {
        try {
            List<WifiConfiguration> configs = wm.getConfiguredNetworks();
            if (configs == null) return null;
            for (WifiConfiguration c : configs) {
                if (("\"" + ssid + "\"").equals(c.SSID)) return c;
            }
        } catch (SecurityException ignored) {
        }
        return null;
    }

    private WifiConfiguration baseConfig() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        return config;
    }

    private void connect(int networkId) {
        wm.disconnect();
        wm.enableNetwork(networkId, true);
        wm.reconnect();
    }

    private void join() {
        String key = password.getText().toString();
        boolean wep = caps.contains("WEP");
        if (!wep && key.length() < 8) {
            showMessage("The password needs at least 8 characters.", 0xFFFF6961);
            return;
        }
        WifiConfiguration config = baseConfig();
        if (wep) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.wepKeys[0] = "\"" + key + "\"";
            config.wepTxKeyIndex = 0;
        } else {
            config.preSharedKey = "\"" + key + "\"";
            if (!caps.contains("PSK") && Build.VERSION.SDK_INT >= 30) {
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
            } else {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            }
        }
        int id = wm.addNetwork(config);
        if (id == -1) {
            showMessage("Android refused to save this network.", 0xFFFF6961);
            return;
        }
        connect(id);
        buttons.setVisibility(View.GONE);
        password.setEnabled(false);
        showMessage("Connecting\u2026", 0xA6FFFFFF);
        deadline = System.currentTimeMillis() + 20_000;
        handler.postDelayed(this::checkConnected, 1000);
    }

    private void checkConnected() {
        if (isFinishing()) return;
        WifiInfo info = wm.getConnectionInfo();
        if (info != null && ("\"" + ssid + "\"").equals(info.getSSID())
                && info.getSupplicantState() == SupplicantState.COMPLETED && info.getIpAddress() != 0) {
            showMessage("Connected", 0xFF30D158);
            handler.postDelayed(this::finish, 800);
        } else if (System.currentTimeMillis() > deadline) {
            showMessage("Couldn't connect. Check the password and try again.", 0xFFFF6961);
            password.setEnabled(true);
            buttons.setVisibility(View.VISIBLE);
            password.requestFocus();
        } else {
            handler.postDelayed(this::checkConnected, 500);
        }
    }

    private void showMessage(String text, int color) {
        message.setText(text);
        message.setTextColor(color);
        message.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void buildCard() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(new ColorDrawable(0x99000000));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = px(24);
        card.setPadding(pad, pad, pad, px(20));
        card.setBackground(rounded(0xF21E1E20, 18, 0x26FFFFFF));
        card.setElevation(px(16));

        TextView title = text(ssid, 20, 0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(title);
        TextView subtitle = text("Enter the password for this network", 13, 0x99FFFFFF);
        subtitle.setPadding(0, px(4), 0, px(16));
        card.addView(subtitle);

        password = new EditText(this);
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTextColor(0xFFFFFFFF);
        password.setHintTextColor(0x66FFFFFF);
        password.setHint("Password");
        password.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        password.setPadding(px(14), px(12), px(14), px(12));
        StateListDrawable field = new StateListDrawable();
        field.addState(new int[]{android.R.attr.state_focused}, rounded(0x26FFFFFF, 10, BLUE));
        field.addState(new int[]{}, rounded(0x1AFFFFFF, 10, 0x1FFFFFFF));
        password.setBackground(field);
        password.setImeOptions(EditorInfo.IME_ACTION_GO);
        password.setOnEditorActionListener((v, action, event) -> {
            join();
            return true;
        });
        card.addView(password);

        CheckBox show = new CheckBox(this);
        show.setText("Show password");
        show.setTextColor(0xB3FFFFFF);
        show.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        show.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));
        show.setBackground(focusOnly());
        show.setPadding(px(4), px(6), px(10), px(6));
        show.setOnCheckedChangeListener((b, checked) -> {
            password.setInputType(InputType.TYPE_CLASS_TEXT | (checked
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            password.setSelection(password.length());
        });
        LinearLayout.LayoutParams showLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        showLp.topMargin = px(8);
        card.addView(show, showLp);

        message = text("", 13, 0x99FFFFFF);
        message.setVisibility(View.GONE);
        message.setPadding(0, px(8), 0, 0);
        card.addView(message);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.END);
        TextView cancel = button(getString(android.R.string.cancel), 0x33FFFFFF);
        cancel.setOnClickListener(v -> finish());
        TextView joinButton = button("Join", BLUE);
        joinButton.setOnClickListener(v -> join());
        row.addView(cancel);
        LinearLayout.LayoutParams joinLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        joinLp.setMarginStart(px(10));
        row.addView(joinButton, joinLp);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = px(18);
        card.addView(row, rowLp);
        buttons = row;

        root.addView(card, new FrameLayout.LayoutParams(px(420), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(root);
        password.requestFocus();
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    private TextView button(String label, int color) {
        TextView b = text(label, 14, 0xFFFFFFFF);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(px(96));
        b.setPadding(px(18), px(9), px(18), px(9));
        b.setFocusable(true);
        b.setClickable(true);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, rounded(color, 10, 0xFFFFFFFF));
        bg.addState(new int[]{}, rounded(color, 10, 0));
        b.setBackground(bg);
        return b;
    }

    private StateListDrawable focusOnly() {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, rounded(0x26FFFFFF, 8, 0));
        bg.addState(new int[]{}, new ColorDrawable(0));
        return bg;
    }

    private GradientDrawable rounded(int color, int radiusDp, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp * dp);
        if (stroke != 0) d.setStroke(Math.max(1, Math.round(dp * (stroke == 0xFFFFFFFF || stroke == BLUE ? 2 : 1))), stroke);
        return d;
    }

    private int px(int value) {
        return Math.round(value * dp);
    }
}
