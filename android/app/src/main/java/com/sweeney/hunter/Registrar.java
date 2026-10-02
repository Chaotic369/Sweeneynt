package com.sweeney.hunter;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Saves this phone's push token where the cloud function looks for it. */
public final class Registrar {
    private Registrar() {}

    public interface Callback { void done(boolean ok, String message); }

    public static void register(Context ctx, String token, Callback cb) {
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            boolean ok = false;
            String msg;
            try {
                String id = "native_" + Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
                URL url = new URL(Config.DB_URL + "/hunterDevices/" + id + ".json");
                String body = "{\"token\":\"" + token + "\",\"type\":\"native\",\"updated\":" + System.currentTimeMillis() + "}";
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("PUT");
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
                int code = c.getResponseCode();
                c.disconnect();
                ok = code >= 200 && code < 300;
                msg = ok ? "Registered" : "Database refused (HTTP " + code + "). Check the hunterDevices rule.";
                if (ok) {
                    SharedPreferences p = app.getSharedPreferences("hp", Context.MODE_PRIVATE);
                    p.edit().putString("token", token).apply();
                }
            } catch (Exception e) {
                msg = "Network error: " + e.getMessage();
            }
            if (cb != null) cb.done(ok, msg);
        }).start();
    }
}
