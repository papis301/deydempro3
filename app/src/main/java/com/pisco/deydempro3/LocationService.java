package com.pisco.deydempro3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.*;

import org.json.JSONObject;

public class LocationService extends Service {

    FusedLocationProviderClient fused;
    int driverId;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        driverId = prefs.getInt(Constants.KEY_DRIVER_ID, -1);

        fused = LocationServices.getFusedLocationProviderClient(this);

        createNotification();
        startLocationUpdates();
    }

    private void createNotification() {
        String id = "location_channel";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(id, "Driver Location", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }

        Notification notif = new NotificationCompat
                .Builder(this, id)
                .setContentTitle("Localisation active")
                .setContentText("La position est envoyée...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();

        startForeground(1, notif);
    }

    private void startLocationUpdates() {
        LocationRequest req = LocationRequest.create()
                .setInterval(7000)
                .setFastestInterval(3000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fused.requestLocationUpdates(req, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) sendLocation(loc);
            }
        }, Looper.getMainLooper());
    }

    private void sendLocation(Location loc) {
        String url = Constants.BASE_URL + "update_location.php";

        JSONObject obj = new JSONObject();
        try {
            obj.put("driver_id", driverId);
            obj.put("lat", loc.getLatitude());
            obj.put("lng", loc.getLongitude());
        } catch (Exception e) {}

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, obj,
                response -> {},
                error -> {}
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

