package com.pisco.deydempro3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class MapDeliveriesActivity extends FragmentActivity {

    private GoogleMap mMap;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;

    private Marker driverMarker;
    private boolean firstFix = true;

    private final HashMap<Marker, MapDelivery> markerDeliveries = new HashMap<>();

    private final String URL_DELIVERIES =
            "https://pisco.alwaysdata.net/get_deliveries_map.php";

    private final int REFRESH_INTERVAL = 3000;
    private final Handler refreshHandler = new Handler();

    private MediaPlayer newOrderSound;
    private int lastDeliveryCount = 0;

    // ==============================
    // 🔵 ON CREATE
    // ==============================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_deliveries);

        newOrderSound = MediaPlayer.create(this, R.raw.new_order);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            enableMyLocation();
            startLocationUpdates();
            startAutoRefresh();
        });
    }

    // ==============================
    // 📍 GPS & POSITION
    // ==============================
    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        LocationRequest request = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc == null) return;

                LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());

                if (driverMarker == null) {
                    driverMarker = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("Ma position")
                            .icon(BitmapDescriptorFactory.fromBitmap(
                                    resizeMarker(R.drawable.icmoto, 60, 60)))
                    );
                } else {
                    driverMarker.setPosition(pos);
                }

                if (firstFix) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
                    firstFix = false;
                }
            }
        };

        locationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
    }

    // ==============================
    // 📦 CHARGER LES LIVRAISONS
    // ==============================
    private void loadDeliveries() {

        StringRequest req = new StringRequest(Request.Method.GET, URL_DELIVERIES,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);

                        if (arr.length() > lastDeliveryCount && lastDeliveryCount != 0) {
                            playNewOrderSound();
                        }
                        lastDeliveryCount = arr.length();

                        HashMap<String, Boolean> active = new HashMap<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            String id = o.getString("id");
                            active.put(id, true);

                            boolean exists = false;
                            for (Marker m : markerDeliveries.keySet()) {
                                if (markerDeliveries.get(m).id.equals(id)) {
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {
                                Marker m = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(
                                                o.getDouble("pickup_lat"),
                                                o.getDouble("pickup_lng")))
                                        .title("Nouvelle livraison")
                                        .icon(BitmapDescriptorFactory
                                                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                );

                                markerDeliveries.put(m, new MapDelivery(
                                        id,
                                        o.getString("pickup_address"),
                                        o.getDouble("pickup_lat"),
                                        o.getDouble("pickup_lng"),
                                        o.getString("dropoff_address"),
                                        o.getDouble("dropoff_lat"),
                                        o.getDouble("dropoff_lng"),
                                        o.getString("price")
                                ));
                            }
                        }

                        // Nettoyage markers supprimés
                        for (Marker m : new HashMap<>(markerDeliveries).keySet()) {
                            if (!active.containsKey(markerDeliveries.get(m).id)) {
                                m.remove();
                                markerDeliveries.remove(m);
                            }
                        }

                    } catch (Exception e) {
                        Log.e("DELIVERY_ERR", e.getMessage());
                    }
                },
                error -> Log.e("NET_ERR", error.toString())
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ==============================
    // 🔁 AUTO REFRESH
    // ==============================
    private void startAutoRefresh() {
        refreshHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadDeliveries();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }, REFRESH_INTERVAL);
    }

    // ==============================
    // 🔊 SON NOUVELLE COURSE
    // ==============================
    private void playNewOrderSound() {
        if (newOrderSound != null) {
            newOrderSound.start();
        }
    }

    // ==============================
    // 🔧 UTILS
    // ==============================
    private Bitmap resizeMarker(int res, int w, int h) {
        Bitmap img = BitmapFactory.decodeResource(getResources(), res);
        return Bitmap.createScaledBitmap(img, w, h, false);
    }

    // ==============================
    // 🧹 CLEAN
    // ==============================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacksAndMessages(null);

        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }

        if (newOrderSound != null) {
            newOrderSound.release();
        }
    }

    // ==============================
    // 📛 PERMISSION RESULT
    // ==============================
    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 1001 && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            startLocationUpdates();
        }
    }
}
