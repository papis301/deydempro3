package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import com.google.android.material.bottomsheet.BottomSheetBehavior;


public class DeliveryNavigationActivity extends FragmentActivity {

    GoogleMap mMap;
    double pickupLat, pickupLng, dropLat, dropLng;

    BottomSheetBehavior<View> bottomSheetBehavior;
    View bottomSheet;

    Button btnSurPlace, btnDemarrer, btnTerminer;
    TextView txtStatus;

    private enum CourseState {
        GOING_TO_PICKUP,
        ARRIVED,
        ONGOING,
        COMPLETED
    }

    CourseState currentState = CourseState.GOING_TO_PICKUP;

    FusedLocationProviderClient locationClient;
    Marker driverMarker;
    Polyline routeLine;

    Button btnCall;
    String deliveryId, client_id, phonerecup;
    String status = "going_to_pickup";
    String URL_STATUS = BASE_URL + "update_delivery_status.php";
    float totalDistanceToPickup = -1; // en mètres
    boolean canSurPlace = false;


    int driverId;

    boolean goingToPickup = true; // phase 1 : vers le pickup
    Bitmap resizeMarker(int drawable, int width, int height) {
        Bitmap image = BitmapFactory.decodeResource(getResources(), drawable);
        return Bitmap.createScaledBitmap(image, width, height, false);
    }

    private float distanceBetween(double lat1, double lng1, double lat2, double lng2) {
        Location a = new Location("A");
        a.setLatitude(lat1);
        a.setLongitude(lng1);

        Location b = new Location("B");
        b.setLatitude(lat2);
        b.setLongitude(lng2);

        return a.distanceTo(b); // en mètres
    }

    double commission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_navigation);


        pickupLat = getIntent().getDoubleExtra("pickup_lat", 0);
        pickupLng = getIntent().getDoubleExtra("pickup_lng", 0);
        dropLat = getIntent().getDoubleExtra("drop_lat", 0);
        dropLng = getIntent().getDoubleExtra("drop_lng", 0);
        client_id = getIntent().getStringExtra("client_id");
        double price = Double.parseDouble(
                getIntent().getStringExtra("price")
        );

// 10% de commission
         commission = price * 0.10;

// Montant net chauffeur
        double netAmount = price - commission;


        locationClient = LocationServices.getFusedLocationProviderClient(this);




         bottomSheet = findViewById(R.id.bottomSheet);
        BottomSheetBehavior<View> bottomSheetBehavior =
                BottomSheetBehavior.from(bottomSheet);

// 🔥 OBLIGATOIRE
        bottomSheetBehavior.setPeekHeight(260); // hauteur visible
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

// Facultatif mais pro
        bottomSheetBehavior.setHideable(false);

        btnSurPlace = bottomSheet.findViewById(R.id.btnSurPlace);
        btnDemarrer = bottomSheet.findViewById(R.id.btnDemarrer);
        btnTerminer = bottomSheet.findViewById(R.id.btnTerminer);
        txtStatus = bottomSheet.findViewById(R.id.txtStatus);
        btnCall = findViewById(R.id.btnCall);

        updateBottomSheetUI();

        btnSurPlace.setOnClickListener(v -> {
            currentState = CourseState.ARRIVED;
            onSurPlace();
            updateBottomSheetUI();
        });

        btnDemarrer.setOnClickListener(v -> {
            currentState = CourseState.ONGOING;
            onDemarrer();
            updateBottomSheetUI();
        });

        btnTerminer.setOnClickListener(v -> {
            currentState = CourseState.COMPLETED;
            onTerminer();
        });


        driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        deliveryId = getIntent().getStringExtra("delivery_id");
       // Toast.makeText(this, "id course"+deliveryId, Toast.LENGTH_SHORT).show();
//        btnSurPlace.setEnabled(false);
//        btnSurPlace.setAlpha(0.5f); // effet visuel bouton désactivé


// Listeners
//        btnSurPlace.setOnClickListener(v -> onSurPlace());
//        btnDemarrer.setOnClickListener(v -> onDemarrer());
//        btnTerminer.setOnClickListener(v -> onTerminer());

        StringRequest req = new StringRequest(Request.Method.POST,
                BASE_URL + "get_user_by_id.php",
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getBoolean("success")) {
                            JSONObject user = obj.getJSONObject("user");
                            phonerecup = user.getString("phone");

                        }
                    } catch (Exception e) {}
                },
                error -> {}
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("user_id", client_id);
                return p;
            }
        };
        // service GPS
        startService(new Intent(this, DriverLocationService.class));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapNav);

        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            showPickupDropMarkers();
            startLiveTracking();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    requestRoute(loc.getLatitude(), loc.getLongitude(), pickupLat, pickupLng);
                }
            });
        });

        btnCall.setOnClickListener(v -> callClient(phonerecup));

    }


    private void callClient(String phone){
           Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private void checkSecurityAndSendPosition(
            String statusc,
            Location loc
    ) {

        String url = BASE_URL + "save_delivery_position.php";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);

                        // 🚨 BLOQUÉ PAR LE SERVEUR
                        if (!obj.getBoolean("success")
                                && obj.optBoolean("blocked")) {

                            String message = obj.optString(
                                    "message",
                                    "Compte bloqué pour activité suspecte"
                            );

                            Intent i = new Intent(
                                    this,
                                    DriverBlockedActivity.class
                            );
                            i.putExtra("message", message);
                            startActivity(i);
                            finish();
                            return;
                        }

                    } catch (Exception e) {
                        Log.e("SECURITY_PARSE", e.getMessage());
                    }
                },
                error -> Log.e("SECURITY_ERR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("delivery_id", deliveryId);
                p.put("driver_id", String.valueOf(driverId));
                p.put("status", statusc);
                p.put("lat", String.valueOf(loc.getLatitude()));
                p.put("lng", String.valueOf(loc.getLongitude()));
                return p;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }


    private void updateStatusOnServer(String newStatus) {

        StringRequest req = new StringRequest(Request.Method.POST, URL_STATUS,
                response -> {
                    Log.e("STATUS_UPDATE", "Serveur: " + response);
                },
                error -> {
                    Log.e("STATUS_ERR", error.toString());
                    Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("delivery_id", String.valueOf(deliveryId));
                p.put("status", newStatus);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void updateBottomSheetUI() {

        btnSurPlace.setVisibility(View.GONE);
        btnDemarrer.setVisibility(View.GONE);
        btnTerminer.setVisibility(View.GONE);

        switch (currentState) {

            case GOING_TO_PICKUP:
                txtStatus.setText("📦 En route vers le client");
                btnSurPlace.setVisibility(View.VISIBLE);
                break;

            case ARRIVED:
                txtStatus.setText("📍 Sur place");
                btnDemarrer.setVisibility(View.VISIBLE);
                break;

            case ONGOING:
                txtStatus.setText("🚚 Livraison en cours");
                btnTerminer.setVisibility(View.VISIBLE);
                break;

            case COMPLETED:
                txtStatus.setText("✅ Livraison terminée");
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
        }
    }


    private void savePositionWithStatus(String status, int driverId) {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) return;

            checkSecurityAndSendPosition(
                    status,
                    location
            );
//            sendPositionToServer(
//                    status,
//                    location.getLatitude(),
//                    location.getLongitude(),
//                    driverId
//            );
        });
    }

    private void sendPositionToServer(String status, double lat, double lng, int driverId) {

        String url = BASE_URL + "save_delivery_position.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> Log.e("GPS_SAVE", response),
                error -> Log.e("GPS_ERR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("delivery_id", String.valueOf(deliveryId));
                p.put("driver_id", String.valueOf(driverId)); // à remplacer par le vrai ID connecté
                p.put("status", status);
                p.put("lat", String.valueOf(lat));
                p.put("lng", String.valueOf(lng));
                return p;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }


    private void onSurPlace() {
        updateStatusOnServer("accepted");
        savePositionWithStatus("accepted", driverId);

        btnSurPlace.setVisibility(View.GONE);
        btnDemarrer.setVisibility(View.VISIBLE);

       // Toast.makeText(this, "Sur place "+driverId+":"+deliveryId, Toast.LENGTH_SHORT).show();

        // 🔹 Démarrage du traceroute vers pickup
        goingToPickup = true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                requestRoute(loc.getLatitude(), loc.getLongitude(), pickupLat, pickupLng);
            }
        });
    }





    private void onDemarrer() {

        updateStatusOnServer("ongoing");
        savePositionWithStatus("ongoing", driverId);

        btnDemarrer.setVisibility(View.GONE);
        btnTerminer.setVisibility(View.VISIBLE);


        Toast.makeText(this, "Livraison démarrée"+driverId+":"+deliveryId, Toast.LENGTH_SHORT).show();
    }


    private void onTerminer() {

        updateStatusOnServer("completed");
        savePositionWithStatus("completed", driverId);

        btnTerminer.setVisibility(View.GONE);

        Toast.makeText(this, "Livraison terminée"+driverId+":"+deliveryId, Toast.LENGTH_LONG).show();
        sendCommissionAndUpdateBalance(commission);
        Intent i = new Intent(this, MapDeliveriesActivity.class);
        startActivity(i);
        finish();
    }

    private void sendCommissionAndUpdateBalance(double commission) {

        String url = BASE_URL + "process_commission.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> Log.e("COMMISSION", response),
                error -> Log.e("COMMISSION_ERR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("delivery_id", deliveryId);
                p.put("driver_id", String.valueOf(driverId));
                p.put("commission", String.valueOf(commission));
                return p;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }



    private void showPickupDropMarkers() {
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(pickupLat, pickupLng))
                .title("Lieu de récupération")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(dropLat, dropLng))
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(pickupLat, pickupLng), 14));
    }

    // ===============================//
    // 🔵 ROUTE OSRM
    // ===============================//

    private void requestRoute(double fromLat, double fromLng, double toLat, double toLng) {

        String url = "https://router.project-osrm.org/route/v1/driving/"
                + fromLng + "," + fromLat + ";" + toLng + "," + toLat
                + "?geometries=geojson";

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {

                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray coords = obj
                                .getJSONArray("routes")
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONArray("coordinates");

                        PolylineOptions polyline = new PolylineOptions()
                                .width(12)
                                .color(0xFF0066FF);

                        for (int i = 0; i < coords.length(); i++) {
                            JSONArray c = coords.getJSONArray(i);
                            polyline.add(new LatLng(c.getDouble(1), c.getDouble(0)));
                        }

                        if (routeLine != null) routeLine.remove();
                        routeLine = mMap.addPolyline(polyline);

                    } catch (Exception e) {
                        Log.e("ROUTE_ERR", e.getMessage());
                    }

                },
                error -> Log.e("ROUTE_FAIL", error.toString())
        );

        Volley.newRequestQueue(this).add(req);
    }

    // ===============================//
    // 🟢 TRACKING LIVE DU LIVREUR
    // ===============================//

    private void startLiveTracking() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        locationClient.requestLocationUpdates(
                LocationRequest.create()
                        .setInterval(2000)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY),

                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult result) {

                        Location loc = result.getLastLocation();
                        if (loc == null) return;

                        checkSecurityAndSendPosition(status, loc);

                        LatLng driverPos = new LatLng(loc.getLatitude(), loc.getLongitude());

                        // 🔥 Mettre à jour marker du livreur
                        if (driverMarker == null) {

                            driverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(driverPos)
                                    .title("Vous")
                                    .icon(BitmapDescriptorFactory.fromBitmap(
                                            resizeMarker(R.drawable.icmoto, 80, 80)
                                    ))
                            );

                        } else {
                            driverMarker.setPosition(driverPos);
                        }

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverPos, 16));


                        // 🔥 Trace dynamique selon la phase
                        if (goingToPickup) {
                            requestRoute(
                                    loc.getLatitude(), loc.getLongitude(),
                                    pickupLat, pickupLng
                            );

                            // Détection d'arrivée
                            float dist = loc.distanceTo(locationFromLatLng(pickupLat, pickupLng));
                            if (dist < 30) { // moins de 30 mètres
                                goingToPickup = false;
                            }

                        } else {
                            requestRoute(
                                    loc.getLatitude(), loc.getLongitude(),
                                    dropLat, dropLng
                            );
                        }
                    }
                },
                getMainLooper()
        );
    }

    private Location locationFromLatLng(double lat, double lng) {
        Location L = new Location("x");
        L.setLatitude(lat);
        L.setLongitude(lng);
        return L;
    }
}
