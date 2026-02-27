package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

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
    ImageView btnNotif, btnNp, histor;

    private final int LOCATION_REQUEST_CODE = 1001;
    private String driverVehicleType = "";
    Switch switchOnline;
    RelativeLayout container;
    FrameLayout slider;
    TextView textStatus;

    boolean isOnline = false;
    boolean assignmentRequested = false;
    boolean hasActiveTrip = false;
    boolean popupVisible = false;
    long lastProposalTime = 0;
    final long COOLDOWN_TIME = 30000; // 30s

    // ==========================
    // LIFECYCLE
    // ==========================
    private BackgroundLocationService backgroundService;
    private boolean serviceBound = false;
//    private final ServiceConnection serviceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            BackgroundLocationService.LocalBinder binder =
//                    (BackgroundLocationService.LocalBinder) service;
//            backgroundService = binder.getService();
//            serviceBound = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            serviceBound = false;
//        }
//    };


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_deliveries);
        View root = findViewById(android.R.id.content);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottomInset = insets.getSystemWindowInsetBottom();

            View bottomSheet = findViewById(R.id.bottomSheetInfo);
            bottomSheet.setPadding(
                    bottomSheet.getPaddingLeft(),
                    bottomSheet.getPaddingTop(),
                    bottomSheet.getPaddingRight(),
                    bottomInset + 16
            );

            return insets;
        });

        container = findViewById(R.id.toggleContainer);
        slider = findViewById(R.id.slider);
        textStatus = findViewById(R.id.textStatus);

        // Charger état sauvegardé
        isOnline = getSharedPreferences("user", MODE_PRIVATE)
                .getBoolean("is_online", false);

        updateUI(isOnline);

        container.setOnClickListener(v -> {

            int endPosition = container.getWidth() - slider.getWidth() - dpToPx(8);

            if (!isOnline) {

                slider.animate()
                        .translationX(endPosition)
                        .setDuration(300)
                        .start();

                textStatus.setText("En ligne");
                textStatus.setTextColor(getResources().getColor(R.color.online_green));

            } else {

                slider.animate()
                        .translationX(0)
                        .setDuration(300)
                        .start();

                textStatus.setText("Hors ligne");
                textStatus.setTextColor(getResources().getColor(R.color.uber_yellow));
            }

            isOnline = !isOnline;

            // Sauvegarder
            getSharedPreferences("user", MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_online", isOnline)
                    .apply();

            // API update
            updateOnlineStatusAPI(isOnline);
        });

        Intent serviceIntent = new Intent(this, BackgroundLocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Binder le service
        //bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        View bottomSheet = findViewById(R.id.bottomSheetInfo);

        BottomSheetBehavior<View> behavior =
                BottomSheetBehavior.from(bottomSheet);

// 🔥 FORCER L’AFFICHAGE
        behavior.setPeekHeight(500); // DOIT matcher la hauteur
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

// 🔒 FIXE (NON EXPANDABLE)
        behavior.setDraggable(false);
        behavior.setHideable(false);


        getDriverVehicleType();
       // badgeNotif = findViewById(R.id.badgeNotif);
        btnNp = findViewById(R.id.btnNp);
        btnNotif = findViewById(R.id.btnNotif);
        histor = findViewById(R.id.histo);

        int notifCount = 3;

        btnNp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MapDeliveriesActivity.this, CompleteProfileActivity.class));
                finish();
            }
        });

        histor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MapDeliveriesActivity.this, TripHistoryActivity.class));
                finish();
            }
        });

//        if(notifCount > 0){
//            badgeNotif.setText(String.valueOf(notifCount));
//            badgeNotif.setVisibility(View.VISIBLE);
//        }else{
//            badgeNotif.setVisibility(View.GONE);
//        }

        int driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        if (driverId == 0) {
            // pas connecté
            startActivity(new Intent(MapDeliveriesActivity.this, LoginActivity.class));
            finish();
        }

        txtSolde = findViewById(R.id.txtSolde);

        newOrderSound = MediaPlayer.create(this, R.raw.new_order);
        newOrderSound.setLooping(false);
        newOrderSound.setVolume(1.0f, 1.0f);
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

        startDispatchLoop();
    }
    private void updateUI(boolean online){

        if(online){

            slider.post(() -> {
                int endPosition = container.getWidth() - slider.getWidth() - dpToPx(8);
                slider.setTranslationX(endPosition);
            });

            textStatus.setText("En ligne");
            textStatus.setTextColor(getResources().getColor(R.color.online_green));
            
               // autoAssignDriver();
            

        } else {

            slider.setTranslationX(0);
            textStatus.setText("Hors ligne");
            textStatus.setTextColor(getResources().getColor(R.color.uber_yellow));
        }
    }

    private void autoAssignDriver(){

        if(!isOnline) return;
        if(hasActiveTrip) return;
        if(popupVisible) return;
        if(assignmentRequested) return;
        if(!driverVehicleType.equalsIgnoreCase("voiture")) return;

        // 🔥 COOLDOWN 1 MINUTE
        long now = System.currentTimeMillis();
        if(now - lastProposalTime < COOLDOWN_TIME){
            return;
        }

        assignmentRequested = true;

        int driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        String url = Constants.BASE_URL + "auto_assign_driver.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {

                    assignmentRequested = false;

                    try {

                        JSONObject obj = new JSONObject(response);

                        if(obj.getBoolean("success")){

                            selectedDelivery = new MapDelivery(
                                    obj.getString("delivery_id"),
                                    obj.getString("pickup_address"),
                                    obj.getDouble("pickup_lat"),
                                    obj.getDouble("pickup_lng"),
                                    obj.getString("dropoff_address"),
                                    obj.getDouble("dropoff_lat"),
                                    obj.getDouble("dropoff_lng"),
                                    obj.getString("price"),
                                    obj.optString("client_id","0"),
                                    obj.getString("vehicle_type")
                            );

                            popupVisible = true;
                            lastProposalTime = System.currentTimeMillis();
                            playTripSound();
                            showIncomingTrip(selectedDelivery);
                        }

                    } catch (Exception e){
                        e.printStackTrace();
                    }

                },
                error -> {
                    assignmentRequested = false;
                    Log.e("ASSIGN_ERR", error.toString());
                }
        ){

            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("driver_id", String.valueOf(driverId));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private int dpToPx(int dp){
        return (int) (dp * getResources().getDisplayMetrics().density);
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

    private void updateOnlineStatusAPI(boolean online){

        int driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        String url = Constants.BASE_URL + "update_online_status.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> Log.d("ONLINE_STATUS", response),
                error -> Toast.makeText(this,"Erreur réseau",Toast.LENGTH_SHORT).show()
        ){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("driver_id", String.valueOf(driverId));
                params.put("is_online", online ? "1" : "0");
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
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

    // ==========================
    // MAP SETUP
    // ==========================

    @SuppressLint("PotentialBehaviorOverride")
    private void setupMap(int driverId) {

        mMap.setOnMarkerClickListener(marker -> {

            // 🚫 Si hors ligne → on bloque
            if(!isOnline){
                Toast.makeText(
                        MapDeliveriesActivity.this,
                        "Vous êtes hors ligne",
                        Toast.LENGTH_SHORT
                ).show();
                return true; // empêche le clic
            }

            // 🚗 Si voiture → pas de clic
            if(driverVehicleType.equalsIgnoreCase("voiture")){
                return true;
            }

            MapDelivery d = markerDeliveries.get(marker);

            if (d != null) {
                showAcceptDialog(d, String.valueOf(driverId));
                return true;
            }

            return false;
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
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
                if(isOnline){
                   // autoAssignDriver();
                    sendDriverLocation();
                }

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

    private void startDispatchLoop(){

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(
                        isOnline &&
                                !hasActiveTrip &&
                                !popupVisible &&
                                driverVehicleType.equalsIgnoreCase("voiture")
                ){
                    autoAssignDriver();
                }

                handler.postDelayed(this, 7000);
            }
        },7000);
    }

    private void playTripSound(){

        try{

            if(newOrderSound != null){

                if(newOrderSound.isPlaying()){
                    newOrderSound.seekTo(0);
                }

                newOrderSound.start();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void sendDriverLocation(){

        if(driverMarker == null) return;

        double lat = driverMarker.getPosition().latitude;
        double lng = driverMarker.getPosition().longitude;

        int driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        String url = Constants.BASE_URL + "update_driver_location.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> Log.d("LOCATION_UPDATE", response),
                error -> Log.e("LOCATION_ERR", error.toString())
        ){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("driver_id", String.valueOf(driverId));
                params.put("lat", String.valueOf(lat));
                params.put("lng", String.valueOf(lng));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    // ==========================
    // LOAD DELIVERIES
    // ==========================

    private void loadDeliveries() {

        if(!isOnline){
            return;
        }
        if(driverVehicleType.isEmpty()){
            return; // attendre chargement type
        }

        // 🚗 Si chauffeur voiture → pas d'affichage
        if(driverVehicleType.equalsIgnoreCase("voiture")){
            return;
        }

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
                                    o.getString("client_id"),
                                    o.getString("vehicle_type")
                            );

                            // 🔥 FILTRAGE AUTOMATIQUE
                            if(driverVehicleType.equalsIgnoreCase("moto")){

                                if(!d.type_vehicule.equalsIgnoreCase("moto")){
                                    continue;
                                }
                            }
//                            if(!driverVehicleType.isEmpty()){
//
//                                String deliveryVehicle = d.type_vehicule.toLowerCase();
//
//                                if(!deliveryVehicle.contains(driverVehicleType)){
//                                    continue; // ignore livraison incompatible
//                                }
//                            }

                            boolean exists = false;
                            for (Marker m : markerDeliveries.keySet()) {
                                if (markerDeliveries.get(m).id.equals(id)) {
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {

                                String vehicleType = d.type_vehicule.toLowerCase();

                                float markerColor = BitmapDescriptorFactory.HUE_GREEN;

                                if(vehicleType.contains("voiture")){
                                    markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                                }
                                else if(vehicleType.contains("moto")){
                                    markerColor = BitmapDescriptorFactory.HUE_RED;
                                }

                                Marker m = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(d.pickupLat, d.pickupLng))
                                        .title("Pickup")
                                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
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

        Volley.newRequestQueue(this).add(req);


    }

    private void showIncomingTrip(MapDelivery trip){

        AlertDialog dialog;

        View view = getLayoutInflater().inflate(R.layout.dialog_accept_trip,null);

        TextView txtInfo = view.findViewById(R.id.txtTripInfo);
        TextView txtTimer = view.findViewById(R.id.txtTimer);
        Button btnAccept = view.findViewById(R.id.btnAccept);

        txtInfo.setText(
                "📍 " + trip.pickup +
                        "\n🏁 " + trip.dropoff +
                        "\n💰 " + trip.price + " FCFA"
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.show();

        // ⏳ COUNTDOWN 15s
        new CountDownTimer(15000,1000){

            public void onTick(long millisUntilFinished){
                txtTimer.setText("Temps restant : " + millisUntilFinished/1000 + "s");
            }

            public void onFinish(){
                popupVisible = false;
                dialog.dismiss();
                if(newOrderSound != null && newOrderSound.isPlaying()){
                    newOrderSound.stop();
                    newOrderSound = MediaPlayer.create(MapDeliveriesActivity.this, R.raw.new_order);
                }
                Toast.makeText(MapDeliveriesActivity.this,
                        "Course expirée",
                        Toast.LENGTH_SHORT).show();
            }

        }.start();

        btnAccept.setOnClickListener(v -> {
            popupVisible = false;
            hasActiveTrip = true;
            lastProposalTime = 0;

            dialog.dismiss();

            if(newOrderSound != null && newOrderSound.isPlaying()){
                newOrderSound.stop();
                newOrderSound = MediaPlayer.create(MapDeliveriesActivity.this, R.raw.new_order);
            }

            int driverId = getSharedPreferences("user",MODE_PRIVATE)
                    .getInt("driver_id",0);

            acceptDelivery(String.valueOf(driverId));
        });


    }

    private void getDriverVehicleType() {

        int driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        String url = BASE_URL + "get_driver_vehicle.php?driver_id=" + driverId;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);

                        if(obj.getBoolean("success")){

                            driverVehicleType = obj.getString("type_vehicule").toLowerCase();

                            Log.d("VEHICLE_TYPE", driverVehicleType);

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("VEHICLE_ERR", error.toString())
        );

        Volley.newRequestQueue(this).add(req);
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

        Volley.newRequestQueue(this).add(req);

    }

    // ==========================
    // UTILS
    // ==========================

    private void playNewOrderSound() {
        //if (newOrderSound != null) newOrderSound.start();
        if (newOrderSound != null) {
            // S'assurer que le volume est au maximum
            newOrderSound.setVolume(1.0f, 1.0f);
            newOrderSound.start();
        }
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
        hasActiveTrip = false;
    }
}
