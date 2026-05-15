package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import deydemv3.LoginActivityc;

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
    private Handler dispatchHandler = new Handler();

    private Runnable dispatchRunnable;

    private MediaPlayer sound;
    private boolean dispatchRunning = false;
    private Polyline currentPolyline;
    private Marker pickupMarker;
    private Marker dropoffMarker;
    private double currentLat = 0;
    private double currentLng = 0;

    private double pickupLat = 0;
    private double pickupLng = 0;

    private String pickupAddress = "";
    private String customerName = "";
    private String customerPhone = "";
    private String customerPhoto = "";
    private String client_id = "";
    ImageView btnMenu;
    LinearLayout menuLayout;
    boolean menuVisible = false;
    TextView txtDriverName;
    TextView txtPhone;
    ImageView imgProfile;

    TextView txtSolde;

    TextView txtJobs;

    TextView txtDistance;

    TextView txtHours;

    // ================================
    // 🚀 ON CREATE
    // ================================
    @SuppressLint("MissingInflatedId")
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
        btnMenu = findViewById(R.id.btnMenu);
        menuLayout = findViewById(R.id.menuLayout);
        txtDriverName = findViewById(R.id.txtDriverName);

        txtPhone = findViewById(R.id.txtPhone);

        imgProfile = findViewById(R.id.imgProfile);
        txtSolde = findViewById(R.id.txtSolde);

        txtJobs = findViewById(R.id.txtJobs);

        txtDistance = findViewById(R.id.txtDistance);

        txtHours = findViewById(R.id.txtHours);

        // USER
        SharedPreferences sp =
                getSharedPreferences("DeydemUser", MODE_PRIVATE);

        userId =
                sp.getString("user_id", "0");
        boolean savedOnline =
                sp.getBoolean(
                        "driver_online",
                        false
                );

        switchOnline.setChecked(savedOnline);

        isOnline = savedOnline;

        if(isOnline){

            setOnlineUI();

            Toast.makeText(
                    this,
                    "Connexion chauffeur...",
                    Toast.LENGTH_SHORT
            ).show();
        }
        //
// 🔥 MODE DRIVER
//
        updateDriverMode();


//
// 🔥 VÉRIFIER SESSION
//
        if (userId == null || userId.isEmpty() || userId.equals("0")) {

            Toast.makeText(this,
                    "Veuillez vous connecter",
                    Toast.LENGTH_LONG).show();

            Intent intent =
                    new Intent(
                            DriverHomeActivity.this,
                            LoginActivityc.class
                    );

            startActivity(intent);

            finish();

            return;
        } Toast.makeText(this,"id"+ userId,Toast.LENGTH_SHORT).show();


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

            sp.edit()
                    .putBoolean(
                            "driver_online",
                            isOnline
                    )
                    .apply();

            if(isOnline){
                setOnlineUI();
                startDispatchLoop(); // démarre UNE seule fois
                checkActiveTrip();
                Log.d(
                        "DRIVER_ID",
                        getSharedPreferences(
                                "DeydemUser",
                                MODE_PRIVATE
                        ).getString(
                                "user_id",
                                "0"
                        )
                );
            } else {
                setOfflineUI();
                stopDispatchLoop();
            }

            updateStatus(isOnline);
        });

        btnMenu.setOnClickListener(v -> {

            if(menuVisible){

                menuLayout.animate()
                        .translationX(-menuLayout.getWidth())
                        .setDuration(250);

                menuLayout.setVisibility(View.GONE);

                menuVisible = false;

            } else {

                menuLayout.setVisibility(View.VISIBLE);

                menuLayout.setTranslationX(
                        -menuLayout.getWidth()
                );

                menuLayout.animate()
                        .translationX(0)
                        .setDuration(250);

                menuVisible = true;
            }
        });

        findViewById(R.id.btnLogout)
                .setOnClickListener(v -> {

                    startActivity(
                            new Intent(
                                    this,
                                    LoginActivityc.class
                            )
                    );

                    finish();
                });
        loadDriverProfile();
    }

    private void loadDriverProfile(){

        String url =
                BASE_URL
                        + "get_driver_profile.php?driver_id="
                        + userId;

        JsonObjectRequest request =
                new JsonObjectRequest(

                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            try {

                                Log.d(
                                        "PROFILE_RESPONSE",
                                        response.toString()
                                );

                                if(response.getBoolean("success")){

                                    JSONObject driver =
                                            response.getJSONObject("driver");

                                    String name =
                                            driver.optString("name");

                                    String phone =
                                            driver.optString("phone");

                                    String photo =
                                            driver.optString("photo");

                                    String solde =
                                            driver.optString("solde");

                                    String totalCourses =
                                            driver.optString("total_courses");

                                    String completed =
                                            driver.optString("completed");

                                    String rating =
                                            driver.optString("rating");

                                    //
                                    // 🔥 UI
                                    //
                                    txtDriverName.setText(name);

                                    txtPhone.setText(phone);
                                    txtSolde.setText(solde + " CFA");

                                    txtJobs.setText(totalCourses);

                                    txtDistance.setText(completed);

                                    txtHours.setText(rating);

                                }

                            } catch(Exception e){
                                e.printStackTrace();
                            }

                        },

                        error -> {

                            Log.e(
                                    "PROFILE_ERROR",
                                    error.toString()
                            );
                        }

                );

        Volley.newRequestQueue(this)
                .add(request);
    }

    private void checkActiveTrip(){

        String url =
                BASE_URL
                        + "get_active_trip.php?driver_id="
                        + userId;

        StringRequest request =
                new StringRequest(
                        Request.Method.GET,
                        url,

                        response -> {

                            try {

                                Log.d(
                                        "ACTIVE_TRIP",
                                        response
                                );

                                JSONObject obj =
                                        new JSONObject(response);

                                if(obj.getBoolean("success")){

                                    hasActiveTrip = true;

                                    JSONObject trip =
                                            obj.getJSONObject("trip");

                                    String rideId =
                                            trip.optString("id");


                                    double pickupLat =
                                            trip.optDouble("pickup_lat");

                                    double pickupLng =
                                            trip.optDouble("pickup_lng");

                                    double dropoffLat =
                                            trip.optDouble("dropoff_lat");

                                    double dropoffLng =
                                            trip.optDouble("dropoff_lng");

                                    String pickupAddress =
                                            trip.optString("pickup_address");

                                    String dropoffAddress =
                                            trip.optString("dropoff_address");

                                    String status =
                                            trip.optString("status");

                                     customerName =
                                            trip.optString("customer_profil");

                                     customerPhone =
                                            trip.optString("customer_phone");

                                     customerPhoto =
                                            trip.optString("customer_photo");
                                     client_id =
                                            trip.optString("client_id");

                                    //
                                    // 🔥 REDIRECTION
                                    //
                                    Intent intent =
                                            new Intent(
                                                    DriverHomeActivity.this,
                                                    DriverPickupActivity.class
                                            );

                                    intent.putExtra(
                                            "ride_id",
                                            rideId
                                    );
                                    intent.putExtra(
                                            "client_id",
                                            trip.optString("client_id")
                                    );

                                    intent.putExtra(
                                            "pickup_lat",
                                            pickupLat
                                    );

                                    intent.putExtra(
                                            "pickup_lng",
                                            pickupLng
                                    );

                                    intent.putExtra(
                                            "dropoff_lat",
                                            dropoffLat
                                    );

                                    intent.putExtra(
                                            "dropoff_lng",
                                            dropoffLng
                                    );

                                    intent.putExtra(
                                            "pickup_address",
                                            pickupAddress
                                    );

                                    intent.putExtra(
                                            "dropoff_address",
                                            dropoffAddress
                                    );

                                    intent.putExtra(
                                            "driver_lat",
                                            currentLat
                                    );

                                    intent.putExtra(
                                            "driver_lng",
                                            currentLng
                                    );

                                    intent.putExtra(
                                            "trip_status",
                                            status
                                    );
                                    intent.putExtra(
                                            "customer_profil",
                                            customerName
                                    );

                                    intent.putExtra(
                                            "customer_phone",
                                            customerPhone
                                    );

                                    intent.putExtra(
                                            "customer_photo",
                                            customerPhoto
                                    );

                                    startActivity(intent);

                                    //finish();
                                }

                            } catch(Exception e){
                                e.printStackTrace();
                            }

                        },

                        error -> {

                            Log.e(
                                    "ACTIVE_TRIP_ERROR",
                                    error.toString()
                            );
                        }

                );

        Volley.newRequestQueue(this)
                .add(request);
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

    private void updateDriverMode() {

        String url = BASE_URL + "update_mode_driver.php";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,

                response -> Log.d("MODE_UPDATE", response),

                error -> Log.e("MODE_ERROR", error.toString())

        ) {

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params =
                        new HashMap<>();

                params.put("user_id", userId);

                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
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

    private void drawRoute(
            LatLng origin,
            LatLng destination,
            TextView tvDistance,
            TextView tvDuration
    ){

        String url =
                "https://router.project-osrm.org/route/v1/driving/"
                        + origin.longitude + "," + origin.latitude
                        + ";"
                        + destination.longitude + "," + destination.latitude
                        + "?overview=full&geometries=geojson";

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            try {

                                Log.d(
                                        "ROUTE_RESPONSE",
                                        response.toString()
                                );

                                JSONArray routes =
                                        response.getJSONArray("routes");

                                if(routes.length() == 0) return;

                                JSONObject route =
                                        routes.getJSONObject(0);

                                //
                                // 🔥 DISTANCE
                                //
                                double distanceMeters =
                                        route.getDouble("distance");

                                double distanceKm =
                                        distanceMeters / 1000;

                                //
                                // 🔥 DURATION
                                //
                                double durationSec =
                                        route.getDouble("duration");

                                double durationMin =
                                        durationSec / 60;

                                tvDistance.setText(
                                        String.format(
                                                "%.2f km",
                                                distanceKm
                                        )
                                );

                                tvDuration.setText(
                                        ((int)durationMin) + " min"
                                );

                                Log.d(
                                        "ROUTE_DISTANCE",
                                        distanceKm + " km"
                                );

                                Log.d(
                                        "ROUTE_DURATION",
                                        durationMin + " min"
                                );

                                JSONObject geometry =
                                        route.getJSONObject("geometry");

                                JSONArray coordinates =
                                        geometry.getJSONArray("coordinates");

                                PolylineOptions polylineOptions =
                                        new PolylineOptions()
                                                .width(14f)
                                                .color(Color.BLUE)
                                                .geodesic(true);

                                for(int i=0; i<coordinates.length(); i++){

                                    JSONArray point =
                                            coordinates.getJSONArray(i);

                                    double lng =
                                            point.getDouble(0);

                                    double lat =
                                            point.getDouble(1);

                                    polylineOptions.add(
                                            new LatLng(lat,lng)
                                    );
                                }

                                //
                                // 🔥 REMOVE OLD
                                //
                                if(currentPolyline != null){
                                    currentPolyline.remove();
                                }

                                //
                                // 🔥 DRAW
                                //
                                currentPolyline =
                                        mMap.addPolyline(polylineOptions);

                            } catch (Exception e){
                                e.printStackTrace();
                            }

                        },

                        error -> {

                            Log.e(
                                    "ROUTE_ERROR",
                                    error.toString()
                            );

                            if(error.networkResponse != null){

                                Log.e(
                                        "ROUTE_CODE",
                                        String.valueOf(
                                                error.networkResponse.statusCode
                                        )
                                );
                            }
                        }

                ){

                    @Override
                    public Map<String, String> getHeaders(){

                        Map<String,String> headers =
                                new HashMap<>();

                        headers.put(
                                "User-Agent",
                                "Mozilla/5.0"
                        );

                        headers.put(
                                "Accept",
                                "application/json"
                        );

                        return headers;
                    }
                };


        Volley.newRequestQueue(this).add(request);
    }

    private float getBearing(
            LatLng start,
            LatLng end
    ){

        double lat =
                Math.abs(start.latitude - end.latitude);

        double lng =
                Math.abs(start.longitude - end.longitude);

        if(start.latitude < end.latitude
                && start.longitude < end.longitude)
            return (float)(Math.toDegrees(Math.atan(lng / lat)));

        else if(start.latitude >= end.latitude
                && start.longitude < end.longitude)
            return (float)((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);

        else if(start.latitude >= end.latitude
                && start.longitude >= end.longitude)
            return (float)(Math.toDegrees(Math.atan(lng / lat)) + 180);

        else if(start.latitude < end.latitude
                && start.longitude >= end.longitude)
            return (float)((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);

        return -1;
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
                currentLat = loc.getLatitude();
                currentLng = loc.getLongitude();

                // 🔥 afficher / déplacer marker
                if(driverMarker == null){

                    driverMarker =
                            mMap.addMarker(

                                    new MarkerOptions()
                                            .position(pos)
                                            .title("Vous")
                                            .flat(true)
                                            .anchor(0.5f,0.5f)
                                            .rotation(0f)
                                            .icon(
                                                    resizeMapIcon(
                                                            R.drawable.car_top,
                                                            128,
                                                            128
                                                    )
                                            )
                            );

                } else {

                    driverMarker.setPosition(pos);

                    driverMarker.setRotation(
                            loc.getBearing()
                    );
                }

                // 🔥 centrer une fois
                if(firstLocation){
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos,16));
                    firstLocation = false;
                }

                // 🔥 envoyer position au serveur
                sendLocation(pos.latitude, pos.longitude);
                if(isOnline && !dispatchRunning){

                    startDispatchLoop();

                    checkActiveTrip();
                }
            }
        }, getMainLooper());
    }

    private BitmapDescriptor resizeMapIcon(
            int iconResId,
            int width,
            int height
    ){

        Bitmap imageBitmap =
                BitmapFactory.decodeResource(
                        getResources(),
                        iconResId
                );

        Bitmap resizedBitmap =
                Bitmap.createScaledBitmap(
                        imageBitmap,
                        width,
                        height,
                        false
                );

        return BitmapDescriptorFactory.fromBitmap(
                resizedBitmap
        );
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

                if(dispatchRunning){
                    handler.postDelayed(this, 7000);
                }
            }
        },7000);
    }

    private void stopDispatchLoop(){

        dispatchRunning = false;

        handler.removeCallbacksAndMessages(null);

        Log.d(
                "DISPATCH",
                "loop stopped"
        );
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
        if(isFinishing() || isDestroyed()){
            return;
        }

        popupVisible = true;

        if(sound != null && !sound.isPlaying()){
            sound.start();
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_incoming_trip);
        Window window = dialog.getWindow();

        if(window != null){

            window.setGravity(Gravity.BOTTOM);

            window.setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );

            window.getAttributes().windowAnimations =
                    android.R.style.Animation_Dialog;
        }

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        dialog.setCancelable(false);

        TextView tvPickup =
                dialog.findViewById(R.id.tvPickup);
        TextView tvTimer =
                dialog.findViewById(R.id.tvTimer);

        TextView tvDropoff =
                dialog.findViewById(R.id.tvDropoff);

        TextView tvPrice =
                dialog.findViewById(R.id.tvPrice);

        TextView tvDistance =
                dialog.findViewById(R.id.tvDistance);

        TextView tvDuration =
                dialog.findViewById(R.id.tvDuration);

        Button btnAccept =
                dialog.findViewById(R.id.btnAccept);

        Button btnIgnore =
                dialog.findViewById(R.id.btnIgnore);

        //
        // 🔥 DATA
        //
        tvPickup.setText(
                trip.optString("pickup_address")
        );

        tvDropoff.setText(
                trip.optString("dropoff_address")
        );

        tvPrice.setText(
                trip.optString("price") + " FCFA"
        );

        tvDistance.setText(
                trip.optString("distance_km") + " km"
        );

        Vibrator vibrator =
                (Vibrator) getSystemService(VIBRATOR_SERVICE);

        if(vibrator != null){
            vibrator.vibrate(1000);
        }
        pickupLat =
                trip.optDouble("pickup_lat");

        pickupLng =
                trip.optDouble("pickup_lng");

        pickupAddress =
                trip.optString("pickup_address");
        client_id =
                trip.optString("client_id");

        CountDownTimer timer =
                new CountDownTimer(15000,1000) {

                    @Override
                    public void onTick(long millisUntilFinished) {

                        tvTimer.setText(
                                String.valueOf(
                                        millisUntilFinished / 1000
                                )
                        );
                    }

                    @Override
                    public void onFinish() {


                        if(dialog.isShowing()){

                            popupVisible = false;

                            hasActiveTrip = false;

                            lastProposalTime = 0;

                            timeoutTrip(
                                    trip.optString("delivery_id")
                            );

                            dialog.dismiss();

                            Toast.makeText(
                                    DriverHomeActivity.this,
                                    "Temps écoulé",
                                    Toast.LENGTH_SHORT
                            ).show();

                            autoAssignDriver();
                        }
                    }
                };

        timer.start();

        //
        // 🔥 ACCEPTER
        //
        btnAccept.setOnClickListener(v -> {
            timer.cancel();
            popupVisible = false;
            hasActiveTrip = true;

            dialog.dismiss();

            acceptTrip(
                    trip.optString("delivery_id")
            );
        });

        //
        // 🔥 REFUSER
        //
        btnIgnore.setOnClickListener(v -> {
            timer.cancel();
            popupVisible = false;

            hasActiveTrip = false;

            lastProposalTime = 0;

            //
            // 🔥 API REFUS
            //
            refuseTrip(
                    trip.optString("delivery_id")
            );

            //
            // 🔥 REMOVE POLYLINE
            //
            if(currentPolyline != null){
                currentPolyline.remove();
                currentPolyline = null;
            }

            //
            // 🔥 REMOVE PICKUP MARKER
            //
            if(pickupMarker != null){
                pickupMarker.remove();
                pickupMarker = null;
            }

            //
            // 🔥 REMOVE DROPOFF MARKER
            //
            if(dropoffMarker != null){
                dropoffMarker.remove();
                dropoffMarker = null;
            }

            //
            // 🔥 RESTORE DRIVER MARKER
            //
            if(driverMarker != null){

                LatLng driverPos =
                        driverMarker.getPosition();

                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                driverPos,
                                16f
                        )
                );
            }

            dialog.dismiss();

            //
            // 🔥 RELANCE RAPIDE DISPATCH
            //
            autoAssignDriver();
        });

        //
// 🔥 PICKUP
//
        double pickupLat =
                trip.optDouble("pickup_lat");

        double pickupLng =
                trip.optDouble("pickup_lng");

//
// 🔥 DROPOFF
//
        double dropLat =
                trip.optDouble("dropoff_lat");

        double dropLng =
                trip.optDouble("dropoff_lng");

        LatLng pickup =
                new LatLng(pickupLat, pickupLng);

        LatLng dropoff =
                new LatLng(dropLat, dropLng);

//
// 🔥 CLEAR OLD MARKERS
//
        // mMap.clear();
        //
// 🔥 REMOVE OLD MARKERS
//
        if(pickupMarker != null){
            pickupMarker.remove();
        }

        if(dropoffMarker != null){
            dropoffMarker.remove();
        }

        if(currentPolyline != null){
            currentPolyline.remove();
        }

        if(driverMarker != null){
            driverMarker.remove();
        }

//
// 🔥 PICKUP MARKER
//
        pickupMarker = mMap.addMarker(
                new MarkerOptions()
                        .position(pickup)
                        .title("Pickup")
                        .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN
                                )
                        )
        );

//
// 🔥 DROPOFF MARKER
//
        dropoffMarker = mMap.addMarker(
                new MarkerOptions()
                        .position(dropoff)
                        .title("Destination")
                        .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED
                                )
                        )
        );

//
// 🔥 DRIVER MARKER
//
        LatLng driverPos =
                new LatLng(currentLat, currentLng);

        driverMarker =
                mMap.addMarker(
                        new MarkerOptions()
                                .position(driverPos)
                                .title("Moi")
                );
        //
// 🔥 DRAW ROUTE
//
        drawRoute(
                pickup,
                dropoff,
                tvDistance,
                tvDuration
        );
//
// 🔥 CAMERA BOUNDS
//
        //
// 🔥 BOUNDS
//
        LatLngBounds.Builder builder =
                new LatLngBounds.Builder();

        builder.include(pickup);
        builder.include(dropoff);

        LatLngBounds bounds =
                builder.build();

//
// 🔥 CAMERA POSITION
//
        CameraUpdate update =
                CameraUpdateFactory.newLatLngBounds(
                        bounds,
                        250 // padding
                );

        try{
            mMap.animateCamera(update);
        }catch(Exception e){
            e.printStackTrace();
        }

//
// 🔥 BEARING
//
        float bearing =
                getBearing(pickup, dropoff);

//
// 🔥 CAMERA STYLE UBER
//
        CameraPosition cameraPosition =
                new CameraPosition.Builder()
                        .target(bounds.getCenter())
                        .zoom(14.5f)
                        .bearing(bearing)
                        .tilt(45f)
                        .build();

        mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                        cameraPosition
                )
        );

        dialog.show();
    }

    private void timeoutTrip(String rideId){

        String url =
                BASE_URL + "trip_timeout.php";

        StringRequest request =
                new StringRequest(

                        Request.Method.POST,
                        url,

                        response -> {

                            Log.d(
                                    "TIMEOUT_RESPONSE",
                                    response
                            );
                        },

                        error -> {

                            Log.e(
                                    "TIMEOUT_ERROR",
                                    error.toString()
                            );
                        }

                ){

                    @Override
                    protected Map<String,String> getParams(){

                        Map<String,String> params =
                                new HashMap<>();

                        params.put(
                                "ride_id",
                                rideId
                        );

                        params.put(
                                "driver_id",
                                userId
                        );

                        return params;
                    }
                };

        Volley.newRequestQueue(this)
                .add(request);
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
                    Intent intent = new Intent(
                            DriverHomeActivity.this,
                            DriverPickupActivity.class
                    );

                    intent.putExtra("ride_id", rideId);
                    intent.putExtra(
                            "client_id", client_id);

                    intent.putExtra("pickup_lat", pickupLat);
                    intent.putExtra("pickup_lng", pickupLng);

                    intent.putExtra("driver_lat", currentLat);
                    intent.putExtra("driver_lng", currentLng);

                    intent.putExtra("pickup_address", pickupAddress);

                    startActivity(intent);
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

    private void refuseTrip(String rideId){

        String url =
                BASE_URL + "refuse_trip.php";

        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        url,

                        response -> {

                            Log.d(
                                    "REFUSE_RESPONSE",
                                    response
                            );
                        },

                        error -> {

                            Log.e(
                                    "REFUSE_ERROR",
                                    error.toString()
                            );
                        }

                ){

                    @Override
                    protected Map<String, String> getParams(){

                        Map<String,String> params =
                                new HashMap<>();

                        params.put(
                                "ride_id",
                                rideId
                        );

                        params.put(
                                "driver_id",
                                userId
                        );

                        return params;
                    }
                };

        Volley.newRequestQueue(this)
                .add(request);
    }

    // ================================
    // 🔄 UI ONLINE / OFFLINE
    // ================================
    private void setOnlineUI(){
        txtStatus.setText("Online");
        banner.setBackgroundColor(Color.GREEN);
        imgStatus.setImageResource(R.drawable.ic_online);
        txtTitle.setText("Vous etes en ligne");
        txtSubtitle.setText("Vous recevrez des courses");
    }

    private void setOfflineUI(){
        txtStatus.setText("Offline");
        banner.setBackgroundColor(Color.parseColor("#FF9800"));
        imgStatus.setImageResource(R.drawable.ic_moon);
        txtTitle.setText("Vous etes hors ligne");
        txtSubtitle.setText("Passer en ligne pour recevoir des courses");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //
        // 🔥 STOP DISPATCH
        //
        stopDispatchLoop();

        //
        // 🔥 STOP GPS
        //
        if(fusedLocationClient != null){

            fusedLocationClient.removeLocationUpdates(
                    new LocationCallback(){}
            );
        }

        //
        // 🔥 RELEASE SOUND
        //
        if(sound != null){

            sound.release();
            sound = null;
        }

        //
        // 🔥 CLEAR HANDLER
        //
        handler.removeCallbacksAndMessages(null);

        Log.d(
                "DRIVER_HOME",
                "onDestroy"
        );
    }
}