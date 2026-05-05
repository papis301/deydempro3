package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.location.*;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class DriverHomeActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Switch switchOnline;
    private TextView txtStatus;


    String userId;
    ImageView imgStatus;
    TextView txtTitle, txtSubtitle;
    private TextView txtSolde;
    LinearLayout banner;

    private FusedLocationProviderClient fusedLocationClient;
    private Marker driverMarker;
    private boolean firstLocation = true;
    boolean isOnline = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        switchOnline = findViewById(R.id.switchOnline);
        txtStatus = findViewById(R.id.txtStatus);
        checkGPS();
        imgStatus = findViewById(R.id.imgStatus);
        txtTitle = findViewById(R.id.txtTitle);
        txtSubtitle = findViewById(R.id.txtSubtitle);
        banner = findViewById(R.id.bannerOffline);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        txtSolde = findViewById(R.id.txtSolde);

        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        userId = sp.getString("user_id", "0");

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        checkPermission();
        refreshSolde();

        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {

            isOnline = isChecked;

            if(isOnline){
                txtStatus.setText("Online");
                setOnlineUI();
            } else {
                txtStatus.setText("Offline");
                setOfflineUI();
            }

            // 🔥 ici appel API
            updateStatus(isOnline);
        });
    }

    private void setOfflineUI(){

        banner.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));

        imgStatus.setImageResource(R.drawable.ic_moon);

        txtTitle.setText("You are offline !");
        txtSubtitle.setText("Go online to start accepting jobs.");

    }

    private void setOnlineUI(){

        banner.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));

        imgStatus.setImageResource(R.drawable.ic_online); // 🚗 ou ✔️

        txtTitle.setText("You are online");
        txtSubtitle.setText("You can now receive rides");

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng dakar = new LatLng(14.7167, -17.4677);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dakar, 14));
    }

    private void updateStatus(boolean online){
        // TODO: appel API update_online_status.php
        String url = Constants.BASE_URL + "update_online_status.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> Log.d("ONLINE_STATUS", response),
                error -> Toast.makeText(this,"Erreur réseau",Toast.LENGTH_SHORT).show()
        ){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("driver_id", String.valueOf(userId));
                params.put("is_online", online ? "1" : "0");
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void checkPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);

        } else {
            startLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        LocationRequest request = LocationRequest.create();
        request.setInterval(3000);
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {

                if (result == null) return;

                Location location = result.getLastLocation();
                if (location == null) return;

                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());

                // 🔥 créer ou déplacer le marker
                if (driverMarker == null) {

                    driverMarker = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("Moi"));
//                    driverMarker = mMap.addMarker(new MarkerOptions()
//                            .position(pos)
//                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));

                } else {
                    driverMarker.setPosition(pos);
                }

                // 🔥 centrer une seule fois
                if (firstLocation) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
                    firstLocation = false;
                }
            }
        }, getMainLooper());
    }

    private void checkGPS() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);

        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(this, locationSettingsResponse -> {
                    // ✅ GPS activé
                    Log.d("GPS", "GPS activé");
                    getUserLocation();
                })
                .addOnFailureListener(this, e -> {

                    if (e instanceof ResolvableApiException) {
                        try {
                            // 🔥 demande activation GPS
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(this, 1001);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
    }

    private void getUserLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location != null && mMap != null) {

                LatLng userLocation = new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                );

                // déplacer caméra
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

                // cercle bleu
                mMap.addCircle(new CircleOptions()
                        .center(userLocation)
                        .radius(100)
                        .strokeColor(Color.BLUE)
                        .fillColor(0x220000FF)
                        .strokeWidth(2));

            }
        });
    }

    private void refreshSolde() {


        String url = BASE_URL + "get_driver_balance.php?driver_id=" + userId;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);

                        if (obj.getBoolean("success")) {
                            int solde = obj.getInt("solde");
                            txtSolde.setText("Solde : " + solde + " FCFA");

                            if (solde <= 0) {
                                showBlockedDialog(solde);
                            }

                            getSharedPreferences("user", MODE_PRIVATE)
                                    .edit()
                                    .putInt("solde", solde)
                                    .apply();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("SOLDE_ERR", error.toString())
        );

        Volley.newRequestQueue(this).add(req);
    }

    private void showBlockedDialog(int solde) {
        new AlertDialog.Builder(this)
                .setTitle("Compte bloqué")
                .setMessage(
                        "Votre compte est bloqué.\n\n" +
                                "Solde actuel : " + solde + " FCFA\n\n" +
                                "Veuillez recharger votre compte."
                )
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGPS();
    }
}