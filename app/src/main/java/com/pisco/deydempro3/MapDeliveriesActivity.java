package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MapDeliveriesActivity extends FragmentActivity {

    private GoogleMap mMap;
    private FusedLocationProviderClient locationClient;
    private Marker driverMarker;

    private final HashMap<Marker, MapDelivery> markerDeliveries = new HashMap<>();

    private final String URL_LIST = "https://pisco.alwaysdata.net/get_deliveries_map.php";
    private final String URL_ACCEPT = "https://pisco.alwaysdata.net/accept_delivery.php";

    private final int REFRESH_INTERVAL = 3000;
    private final Handler handler = new Handler();

    private boolean firstFix = true;
    private int lastDeliveryCount = 0;

    private MediaPlayer newOrderSound;
    private MapDelivery selectedDelivery;
    private TextView txtSolde;
    TextView badgeNotif;
    ImageView btnNotif;

    private final int LOCATION_REQUEST_CODE = 1001;

    // ==========================
    // LIFECYCLE
    // ==========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_deliveries);

        badgeNotif = findViewById(R.id.badgeNotif);
        btnNotif = findViewById(R.id.btnNotif);

        int notifCount = 3;

        if(notifCount > 0){
            badgeNotif.setText(String.valueOf(notifCount));
            badgeNotif.setVisibility(View.VISIBLE);
        }else{
            badgeNotif.setVisibility(View.GONE);
        }

        int driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        if (driverId == 0) {
            // pas connecté
            startActivity(new Intent(MapDeliveriesActivity.this, LoginActivity.class));
            finish();
        }

        txtSolde = findViewById(R.id.txtSolde);

        newOrderSound = MediaPlayer.create(this, R.raw.new_order);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Vérifier les permissions et lancer la localisation
        checkLocationPermission();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        mapFragment.getMapAsync(map -> {
            mMap = map;
            setupMap(driverId);
            loadDeliveries();
            startAutoRefresh();
        });

        btnNotif.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class))
        );
    }

    // ==========================
    // PERMISSIONS
    // ==========================

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==========================
    // SOLDE
    // ==========================

    private void refreshSolde() {
        int driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        String url = BASE_URL + "get_driver_balance.php?driver_id=" + driverId;

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

        VolleySingleton.getInstance(this).addToRequestQueue(req);
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

    // ==========================
    // MAP SETUP
    // ==========================

    @SuppressLint("PotentialBehaviorOverride")
    private void setupMap(int driverId) {
        mMap.setOnMarkerClickListener(marker -> {
            MapDelivery d = markerDeliveries.get(marker);
            if (d != null) {
                showAcceptDialog(d, String.valueOf(driverId));
                return true;
            }
            return false;
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); // Affiche le point bleu
        }
    }

    // ==========================
    // GPS LIVREUR
    // ==========================

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        LocationRequest request = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {

                Location loc = result.getLastLocation();
                if (loc == null) return;

                LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());

                if (driverMarker == null) {
                    driverMarker = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("Moi")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    );
                } else {
                    driverMarker.setPosition(pos);
                }

                if (firstFix) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                    firstFix = false;
                }
            }
        }, getMainLooper());
    }

    // ==========================
    // LOAD DELIVERIES
    // ==========================

    private void loadDeliveries() {
        StringRequest req = new StringRequest(Request.Method.GET, URL_LIST,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);

                        if (arr.length() > lastDeliveryCount && lastDeliveryCount != 0) {
                            playNewOrderSound();
                        }
                        lastDeliveryCount = arr.length();

                        HashMap<String, Boolean> activeIds = new HashMap<>();

                        for (int i = 0; i < arr.length(); i++) {

                            JSONObject o = arr.getJSONObject(i);
                            String id = o.getString("id");
                            activeIds.put(id, true);

                            MapDelivery d = new MapDelivery(
                                    id,
                                    o.getString("pickup_address"),
                                    o.getDouble("pickup_lat"),
                                    o.getDouble("pickup_lng"),
                                    o.getString("dropoff_address"),
                                    o.getDouble("dropoff_lat"),
                                    o.getDouble("dropoff_lng"),
                                    o.getString("price"),
                                    o.getString("client_id")
                            );

                            boolean exists = false;
                            for (Marker m : markerDeliveries.keySet()) {
                                if (markerDeliveries.get(m).id.equals(id)) {
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {
                                Marker m = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(d.pickupLat, d.pickupLng))
                                        .title("Pickup")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                );
                                markerDeliveries.put(m, d);
                            }
                        }

                        for (Marker m : new HashMap<>(markerDeliveries).keySet()) {
                            if (!activeIds.containsKey(markerDeliveries.get(m).id)) {
                                m.remove();
                                markerDeliveries.remove(m);
                            }
                        }

                    } catch (Exception e) {
                        Log.e("LOAD_ERR", e.getMessage());
                    }
                },
                error -> Log.e("NETWORK", error.toString())
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ==========================
    // ACCEPT DIALOG
    // ==========================

    private void showAcceptDialog(MapDelivery d, String driverId) {
        new AlertDialog.Builder(this)
                .setTitle("📦 Nouvelle course")
                .setMessage(
                        "📍 Pickup :\n" + d.pickup +
                                "\n\n🏁 Destination :\n" + d.dropoff +
                                "\n\n💰 Prix : " + d.price + " FCFA"
                )
                .setCancelable(false)
                .setPositiveButton("✅ ACCEPTER", (dialog, which) -> {
                    selectedDelivery = d;
                    acceptDelivery(driverId);
                })
                .setNegativeButton("❌ ANNULER", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ==========================
    // ACCEPT DELIVERY
    // ==========================

    private void acceptDelivery(String driverId) {
        StringRequest req = new StringRequest(Request.Method.POST, URL_ACCEPT,
                response -> {
                    Toast.makeText(this, "Livraison acceptée", Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(this, DeliveryNavigationActivity.class);
                    i.putExtra("delivery_id", selectedDelivery.id);
                    i.putExtra("pickup_lat", selectedDelivery.pickupLat);
                    i.putExtra("pickup_lng", selectedDelivery.pickupLng);
                    i.putExtra("drop_lat", selectedDelivery.dropLat);
                    i.putExtra("drop_lng", selectedDelivery.dropLng);
                    i.putExtra("pickup_address", selectedDelivery.pickup);
                    i.putExtra("dropoff_address", selectedDelivery.dropoff);
                    i.putExtra("price", selectedDelivery.price);
                    i.putExtra("client_id", selectedDelivery.client_id);
                    startActivity(i);
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("delivery_id", selectedDelivery.id);
                p.put("driver_id", driverId);
                return p;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ==========================
    // UTILS
    // ==========================

    private void playNewOrderSound() {
        if (newOrderSound != null) newOrderSound.start();
    }

    private void startAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadDeliveries();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }, REFRESH_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (newOrderSound != null) newOrderSound.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSolde();
    }
}
