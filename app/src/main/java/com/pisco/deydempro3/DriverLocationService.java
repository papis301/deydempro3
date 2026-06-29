package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.*;

import java.util.HashMap;
import java.util.Map;

public class DriverLocationService extends Service {

    FusedLocationProviderClient fused;
    LocationCallback locationCallback;

    String UPDATE_URL = BASE_URL + "update_driver_position.php";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, "driver_channel")
                .setContentTitle("DeyDem — Suivi en cours")
                .setContentText("Votre position est actualisée…")
                .setSmallIcon(R.drawable.ic_motov)
                .build();

        startForeground(1, notification);

        fused = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest req = LocationRequest.create();
        req.setInterval(2000);              // 🔥 toutes les 2 secondes
        req.setFastestInterval(1000);
        req.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult res) {
                for (Location loc : res.getLocations()) {
                    sendLocationToServer(loc.getLatitude(), loc.getLongitude());
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fused.requestLocationUpdates(req, locationCallback, getMainLooper());
        }
    }

    private void sendLocationToServer(double lat, double lng) {
        StringRequest req = new StringRequest(Request.Method.POST, UPDATE_URL,
                response -> Log.d("GPS_UPDATE", response),
                error -> Log.e("GPS_ERROR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("driver_id", "1");  // ⚠️ Remplacer par SharedPreferences
                p.put("lat", String.valueOf(lat));
                p.put("lng", String.valueOf(lng));
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        fused.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    "driver_channel",
                    "Driver GPS",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(ch);
        }
    }
}

