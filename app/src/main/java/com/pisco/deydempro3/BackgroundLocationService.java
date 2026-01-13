package com.pisco.deydempro3;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BackgroundLocationService extends Service {

    private static final int NOTIFICATION_ID = 123;
    private static final String CHANNEL_ID = "delivery_channel";
    private PowerManager.WakeLock wakeLock;
    private MediaPlayer notificationSound;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        acquireWakeLock();

        // Initialiser le son
        notificationSound = MediaPlayer.create(this, R.raw.new_order);
        notificationSound.setVolume(1.0f, 1.0f);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Delivery Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new deliveries");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MapDeliveriesActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Livraisons actives")
                .setContentText("Recherche de nouvelles courses...")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MapDeliveries:WakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
    }

    public void playNotificationSound() {
        if (notificationSound != null && !notificationSound.isPlaying()) {
            try {
                notificationSound.start();

                // 🔥 Réveiller l'écran si éteint
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                if (!powerManager.isInteractive()) {
                    PowerManager.WakeLock screenWakeLock = powerManager.newWakeLock(
                            PowerManager.FULL_WAKE_LOCK |
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                    PowerManager.ON_AFTER_RELEASE,
                            "MapDeliveries:ScreenWake"
                    );
                    screenWakeLock.acquire(3000); // 3 secondes
                    screenWakeLock.release();
                }

            } catch (Exception e) {
                Log.e("SOUND_ERROR", "Erreur lecture son: " + e.getMessage());
            }
        }
    }

    public void showNewDeliveryNotification(String price, String pickupAddress) {
        Intent intent = new Intent(this, MapDeliveriesActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📦 Nouvelle course disponible!")
                .setContentText("💰 " + price + " FCFA - " + pickupAddress)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL) // Son + vibration + LED
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID + 1, notification);

        // Jouer le son
        playNotificationSound();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (notificationSound != null) {
            notificationSound.release();
        }
        super.onDestroy();
    }
}