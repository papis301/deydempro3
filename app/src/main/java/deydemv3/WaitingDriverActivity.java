package deydemv3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.pisco.deydempro3.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WaitingDriverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private View layoutSearching, layoutDriver;
    private TextView tvDriverName, tvCar;

    private String rideId;
    private Handler handler = new Handler();

    private boolean driverFound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_driver);

        rideId = getIntent().getStringExtra("ride_id");
        android.widget.Toast.makeText(this,
                "Commande envoyée 🚀 ID: " + rideId,
                android.widget.Toast.LENGTH_LONG).show();

        layoutSearching = findViewById(R.id.layoutSearching);
        layoutDriver = findViewById(R.id.layoutDriver);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvCar = findViewById(R.id.tvCar);

        // MAP
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // 🔥 boutons
        initButtons();

        showSearching();

        startDriverSearch();
    }

    // ================= MAP =================

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initLocation();
    }

    private void initLocation() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {

                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.addMarker(new MarkerOptions()
                        .position(userLatLng)
                        .title("Ma position"));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
            }
        });
    }

    // ================= UI =================

    private void showSearching() {
        layoutSearching.setVisibility(View.VISIBLE);
        layoutDriver.setVisibility(View.GONE);
    }

    private void showDriver(String name, String car) {

        layoutSearching.setVisibility(View.GONE);
        layoutDriver.setVisibility(View.VISIBLE);

        tvDriverName.setText(name);
        tvCar.setText(car);
    }

    // ================= DRIVER SEARCH =================

    private void startDriverSearch() {

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (!driverFound) {
                    checkDriverStatus();
                    handler.postDelayed(this, 5000);
                }
            }
        }, 3000);
    }

    private void checkDriverStatus() {

        String url = "https://pisco.alwaysdata.net/check_driver.php?ride_id=" + rideId;

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,

                response -> {
                    try {
                        JSONObject json = new JSONObject(response);

                        if (json.getBoolean("found")) {

                            driverFound = true;

                            String name = json.getString("driver_name");
                            String car = json.getString("vehicle");

                            showDriver(name, car);

                            Toast.makeText(this, "Chauffeur trouvé 🚗", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },

                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    // ================= BUTTONS =================

    private void initButtons() {

        // ❌ Annuler
        findViewById(R.id.btnCancel).setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("Annuler la course")
                    .setMessage("Voulez-vous vraiment annuler ?")
                    .setPositiveButton("Oui", (d, i) -> cancelRide())
                    .setNegativeButton("Non", null)
                    .show();
        });

        // 📋 Liste commandes
//        findViewById(R.id.btnOrders).setOnClickListener(v -> {
//            startActivity(new Intent(this, OrdersActivity.class));
//        });
    }

    private void cancelRide() {

        String url = "https://pisco.alwaysdata.net/cancel_ride.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,

                response -> {
                    Log.d("CANCEL_RESPONSE", response);

                    try {
                        JSONObject json = new JSONObject(response);

                        if (json.getBoolean("success")) {
                            Toast.makeText(this, "Course annulée ✅", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, json.getString("message"), Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur parsing JSON", Toast.LENGTH_SHORT).show();
                    }
                },

                error -> {
                    Log.e("CANCEL_ERROR", error.toString());

                    if (error.networkResponse != null) {
                        Log.e("CANCEL_ERROR_CODE", String.valueOf(error.networkResponse.statusCode));
                    }

                    Toast.makeText(this, "Erreur réseau ❌", Toast.LENGTH_SHORT).show();
                }

        ) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();
                params.put("ride_id", String.valueOf(rideId));

                Log.d("CANCEL_DEBUG", "ride_id = " + rideId);

                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}