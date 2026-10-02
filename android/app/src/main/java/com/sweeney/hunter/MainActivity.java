package com.sweeney.hunter;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(60, 60, 60, 60);
        root.setBackgroundColor(Color.BLACK);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, 50);
        root.addView(status);

        root.addView(button("Allow background (battery)", v -> requestBatteryExemption()));
        root.addView(button("Send test notification", v ->
                PushService.show(this, 1, "Prey", "Test: notifications work")));
        root.addView(button("Open site", v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.SITE_URL)))));
        setContentView(root);

        PushService.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        registerToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerToken(); // refresh on every open
    }

    private Button button(String label, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setOnClickListener(l);
        return btn;
    }

    private void setStatus(String s) { runOnUiThread(() -> status.setText(s)); }

    private void registerToken() {
        setStatus("Registering...");
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(t -> {
            if (!t.isSuccessful() || t.getResult() == null) {
                setStatus("Could not get push token. Check internet.");
                return;
            }
            Registrar.register(this, t.getResult(), (ok, msg) ->
                    setStatus(ok ? "Hunter ready.\nEvery prey message will notify this phone." : msg));
        });
    }

    private void requestBatteryExemption() {
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                setStatus("Battery already unrestricted.");
                return;
            }
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }
}
