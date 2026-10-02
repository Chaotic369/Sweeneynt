package com.sweeney.hunter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class PushService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        Registrar.register(this, token, null);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> d = message.getData();
        String title = d.get("title");
        String body = d.get("body");
        if (title == null || title.isEmpty()) title = "New message";
        if (body == null || body.isEmpty()) body = "New message";
        String msgId = d.get("msgId");
        int id = (msgId != null ? msgId : String.valueOf(System.nanoTime())).hashCode();
        show(this, id, title, body);
    }

    static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(Config.CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        Config.CHANNEL_ID, "Prey messages", NotificationManager.IMPORTANCE_HIGH);
                ch.enableVibration(true);
                ch.setVibrationPattern(new long[]{0, 200, 100, 200});
                nm.createNotificationChannel(ch);
            }
        }
    }

    static void show(Context ctx, int id, String title, String body) {
        ensureChannel(ctx);
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(Config.SITE_URL));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);

        Notification n = new NotificationCompat.Builder(ctx, Config.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        try {
            ctx.getSystemService(NotificationManager.class).notify(id, n);
        } catch (SecurityException ignored) { /* notification permission not granted */ }
    }
}
