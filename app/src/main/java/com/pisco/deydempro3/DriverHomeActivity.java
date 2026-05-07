package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DriverHomeActivity extends FragmentActivity implements OnMapReadyCallback {

    // 🔥 MAP + GPS
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker driverMarker;
    private boolean firstLocation = true;

    // 🔥 UI
    private Switch switchOnline;
    private TextView txtStatus, txtTitle, txtSubtitle;
    private ImageView imgStatus;
    private LinearLayout banner;

    // 🔥 STATE
    private boolean isOnline = false;
    private boolean hasActiveTrip = false;
    private boolean popupVisible = false;
    private boolean assignmentRequested = false;

    private long lastProposalTime = 0;
    private final long COOLDOWN = 15000;

    private String userId;

    private Handler handler = new Handler();

    private MediaPlayer sound;
    private boolean dispatchRunning = false;

    // ================================
    // 🚀 ON CREATE
    // ================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        // UI
        switchOnline = findViewById(R.id.switchOnline);
        txtStatus = findViewById(R.id.txtStatus);
        txtTitle = findViewById(R.id.txtTitle);
        txtSubtitle = findViewById(R.id.txtSubtitle);
        imgStatus = findViewById(R.id.imgStatus);
        banner = findViewById(R.id.bannerOffline);

        // USER
        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        userId = sp.getString("user_id", "0");

        // MAP
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sound = MediaPlayer.create(this, R.raw.new_order);

        checkGPS();

        // 🔥 SWITCH ONLINE / OFFLINE
        switchOnline.setOnCheckedChangeListener((btn, isChecked) -> {

            isOnline = isChecked;

            if(isOnline){
                setOnlineUI();
                startDispatchLoop(); // démarre UNE seule fois
            } else {
                setOfflineUI();
            }

            updateStatus(isOnline);
        });
    }

    // ================================
    // 🔥 GPS CHECK + ACTIVATION
    // ================================
    private void checkGPS(){

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(!enabled){

            new AlertDialog.Builder(this)
                    .setTitle("GPS désactivé")
                    .setMessage("Activez le GPS pour continuer")
                    .setCancelable(false)
                    .setPositiveButton("Activer", (d,w)->{
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .show();
        }
    }

    // ================================
    // 🔥 MAP READY
    // ================================
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        checkPermission();
    }

    // ================================
    // 🔥 PERMISSION GPS
    // ================================
    private void checkPermission(){

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},100);

        } else {
            startLocationUpdates();
        }
    }

    // ================================
    // 📍 POSITION CHAUFFEUR (TEMPS RÉEL)
    // ================================
    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){

        LocationRequest request = LocationRequest.create();
        request.setInterval(3000);
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(request, new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult result){

                if(result == null) return;

                Location loc = result.getLastLocation();
                if(loc == null) return;

                LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());

                // 🔥 afficher / déplacer marker
                if(driverMarker == null){
                    driverMarker = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("Moi"));
                } else {
                    driverMarker.setPosition(pos);
                }

                // 🔥 centrer une fois
                if(firstLocation){
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos,16));
                    firstLocation = false;
                }

                // 🔥 envoyer position au serveur
                sendLocation(pos.latitude, pos.longitude);
            }
        }, getMainLooper());
    }

    // ================================
    // 📡 ENVOI POSITION
    // ================================
    private void sendLocation(double lat, double lng){

        String url = BASE_URL + "update_driver_location.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                res -> Log.d("LOC", res),
                err -> Log.e("LOC_ERR", err.toString())
        ){
            protected Map<String,String> getParams(){
                Map<String,String> p = new HashMap<>();
                p.put("driver_id", userId);
                p.put("lat", String.valueOf(lat));
                p.put("lng", String.valueOf(lng));
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    // ================================
    // 🔄 BOUCLE ASSIGNATION
    // ================================
    private void startDispatchLoop(){

        if(dispatchRunning) return; // 🔥 évite double boucle
        dispatchRunning = true;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(isOnline && !hasActiveTrip && !popupVisible){
                    autoAssignDriver();
                    Log.d("DISPATCH", "loop active");
                }

                handler.postDelayed(this, 7000);
            }
        },7000);
    }

    // ================================
    // 🚗 AUTO ASSIGNATION
    // ================================
    private void autoAssignDriver(){
        Log.d("AUTO_ASSIGN", "appel méthode");
        if(assignmentRequested) return;

        long now = System.currentTimeMillis();
        if(now - lastProposalTime < COOLDOWN) return;

        assignmentRequested = true;

        String url = BASE_URL + "auto_assign_driver.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {

                    assignmentRequested = false;

                    try{
                        JSONObject obj = new JSONObject(response);

                        if(obj.getBoolean("success")){

                            lastProposalTime = System.currentTimeMillis();

                            showIncomingTrip(obj);
                            Log.d("ASSIGN_RESPONSE", response);

                        } else {

                            Log.d("ASSIGN_RESPONSE", "Aucune course");
                            lastProposalTime = 0; // 🔥 relance rapide
                        }

                    }catch(Exception e){
                        e.printStackTrace();
                    }
                },
                error -> {
                    assignmentRequested = false;
                    Log.e("ASSIGN_ERROR", error.toString());

                }
        ){
            protected Map<String,String> getParams(){
                Map<String,String> p = new HashMap<>();
                p.put("driver_id", userId);
                Log.d("DRIVER_ID", userId);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    // ================================
    // 🔔 POPUP COURSE
    // ================================
    private void showIncomingTrip(JSONObject trip){

        popupVisible = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nouvelle course");

        builder.setMessage("Pickup: " + trip.optString("pickup_address")
                + "\nPrix: " + trip.optString("price"));

        builder.setCancelable(false);

        builder.setPositiveButton("ACCEPTER", (d,w)->{

            popupVisible = false;
            hasActiveTrip = true;

            acceptTrip(trip.optString("delivery_id"));
        });

        builder.setNegativeButton("REFUSER", (d,w)->{
            popupVisible = false;
            lastProposalTime = 0; // 🔥 important sinon bloqué
        });

        builder.show();

        // 🔥 son
        if(sound != null) sound.start();
    }

    // ================================
    // ✅ ACCEPTER COURSE
    // ================================
    private void acceptTrip(String rideId){

        hasActiveTrip = true;

        String url = BASE_URL + "accept_delivery.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                res -> {
                    Toast.makeText(this,"Course acceptée",Toast.LENGTH_SHORT).show();
                },
                err -> {
                    hasActiveTrip = false;
                    Log.e("ACCEPT_ERR", err.toString());
                }
        ){
            protected Map<String,String> getParams(){
                Map<String,String> p = new HashMap<>();
                p.put("delivery_id", rideId);
                p.put("driver_id", userId);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    // ================================
    // 🔄 UI ONLINE / OFFLINE
    // ================================
    private void setOnlineUI(){
        txtStatus.setText("Online");
        banner.setBackgroundColor(Color.GREEN);
        imgStatus.setImageResource(R.drawable.ic_online);
        txtTitle.setText("You are online");
    }

    private void setOfflineUI(){
        txtStatus.setText("Offline");
       // banner.setBackgroundColor(Color.ORANGE);
        imgStatus.setImageResource(R.drawable.ic_moon);
        txtTitle.setText("You are offline");
    }

    // ================================
    // 📡 UPDATE STATUS SERVEUR
    // ================================
    private void updateStatus(boolean online){

        String url = BASE_URL + "update_online_status.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                res -> {},
                err -> {}
        ){
            protected Map<String,String> getParams(){
                Map<String,String> p = new HashMap<>();
                p.put("driver_id", userId);
                p.put("is_online", online ? "1":"0");
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }
}