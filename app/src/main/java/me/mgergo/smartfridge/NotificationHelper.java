package me.mgergo.smartfridge;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationHelper {
    public static void sendExpiryNotification(Context context, @Nullable FridgeItem item) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Notification", "No permission to send notification");
            return;
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.e("Notification", "Permissions denied in system settings");
            return;
        }

        String title, text;
        if (item != null) {
            title = "Expires soon: " + item.getName() + " (x" + item.getAmount() + ")";
            text = "Expiry date: " + item.getExpirationDate();
        } else {
            title = "Check your fridge!";
            text = "You may have items expiring soon.";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "expiry_channel")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.fridge_icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(item.hashCode(), builder.build());
        } catch (SecurityException ex) {
            Log.e("Notification", "Security error: " + ex.getMessage());
        }
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "expiry_channel",
                    "Expiry notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
