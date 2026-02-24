package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;

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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DeliveryNavigationActivity extends FragmentActivity {

    GoogleMap mMap;
    double pickupLat, pickupLng, dropLat, dropLng;
    String deliveryId, client_id, phonerecup;
    float totalDistanceToPickup = -1; // en mètres
    boolean goingToPickup = true; // phase 1 : vers le pickup
    Bitmap resizeMarker(int drawable, int width, int height) {
        Bitmap image = BitmapFactory.decodeResource(getResources(), drawable);
        return Bitmap.createScaledBitmap(image, width, height, false);
    }

    FusedLocationProviderClient locationClient;
    Marker driverMarker;
    Polyline routeLine;

    BottomSheetBehavior<View> bottomSheetBehavior;
    View bottomSheet;
    Button btnSurPlace, btnDemarrer, btnTerminer, btnCall,  btnAnnuler;
    ;
    TextView txtStatus;

    private enum CourseState { GOING_TO_PICKUP, ARRIVED, ONGOING, COMPLETED }
    CourseState currentState = CourseState.GOING_TO_PICKUP;

    int driverId;
    String status = "going_to_pickup";
    float commission;
    String URL_STATUS = BASE_URL + "update_delivery_status.php";
    TextView txtPrice ;
    double price;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_navigation);

        pickupLat = getIntent().getDoubleExtra("pickup_lat", 0);
        pickupLng = getIntent().getDoubleExtra("pickup_lng", 0);
        dropLat = getIntent().getDoubleExtra("drop_lat", 0);
        dropLng = getIntent().getDoubleExtra("drop_lng", 0);
        client_id = getIntent().getStringExtra("client_id");
        deliveryId = getIntent().getStringExtra("delivery_id");
         price = Double.parseDouble(getIntent().getStringExtra("price"));
        commission = (float) (price * 0.12); // 10% commission

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(260);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setHideable(false);

        btnSurPlace = bottomSheet.findViewById(R.id.btnSurPlace);
        btnDemarrer = bottomSheet.findViewById(R.id.btnDemarrer);
        btnTerminer = bottomSheet.findViewById(R.id.btnTerminer);
        txtStatus = bottomSheet.findViewById(R.id.txtStatus);
        txtPrice = bottomSheet.findViewById(R.id.txtPrice);
        btnCall = findViewById(R.id.btnCall);
        btnAnnuler = bottomSheet.findViewById(R.id.btnAnnuler);
        txtPrice = bottomSheet.findViewById(R.id.txtPrice);

        updateBottomSheetUI();

        driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        // Charger le numéro du client
        StringRequest req = new StringRequest(Request.Method.POST,
                BASE_URL + "get_user_by_id.php",
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getBoolean("success")) {
                            JSONObject user = obj.getJSONObject("user");
                            phonerecup = user.getString("phone");
                        }
                    } catch (Exception e) { Log.e("USER_PARSE_ERR", e.getMessage()); }
                },
                error -> Log.e("USER_REQ_ERR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("user_id", client_id);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);

        startService(new Intent(this, DriverLocationService.class));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapNav);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            showPickupDropMarkers();

            // 🔹 Traceroute initial vers pickup
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationClient.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        requestRoute(loc.getLatitude(), loc.getLongitude(), pickupLat, pickupLng);
                    }
                });
            }

            startLiveTracking();
        });

        btnCall.setOnClickListener(v -> callClient(phonerecup));

        btnSurPlace.setOnClickListener(v -> {
            currentState = CourseState.ARRIVED;
            onSurPlace();
            updateBottomSheetUI();

            // 🔹 Effacer la route vers le pickup
            clearRouteLine();
        });

        btnDemarrer.setOnClickListener(v -> {
            currentState = CourseState.ONGOING;
            onDemarrer();
            updateBottomSheetUI();

            // 🔹 Afficher la route vers la destination finale
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationClient.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        requestRoute(loc.getLatitude(), loc.getLongitude(), dropLat, dropLng);
                    }
                });
            }
        });

        btnTerminer.setOnClickListener(v -> {
            currentState = CourseState.COMPLETED;
            onTerminer();
            completeTrip(deliveryId);
        });

        btnAnnuler.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Annuler la course")
                    .setMessage("Voulez-vous vraiment annuler cette course ?")
                    .setPositiveButton("Oui", (dialog, which) -> {

                        // 🔴 Annulation réelle de la course
                        cancelCourse(
                                "driver",
                                driverId,
                                "NO_SHOW",
                                "Client absent au point de retrait"
                        );
                        annulerCourse();

                    })
                    .setNegativeButton("Non", null)
                    .show();
        });


    }

    private void callClient(String phone){
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private void completeTrip(String deliveryId){

        String url = BASE_URL + "complete_trip.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {

                    try {

                        JSONObject obj = new JSONObject(response);

                        if(obj.getBoolean("success")){

                            Toast.makeText(this,
                                    "Course terminée\nGain : "
                                            + obj.getInt("driver_gain") + " FCFA",
                                    Toast.LENGTH_LONG).show();

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                },
                error -> Toast.makeText(this, "Erreur", Toast.LENGTH_SHORT).show()
        ){

            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("delivery_id", deliveryId);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void annulerCourse() {
        StringRequest req = new StringRequest(
                Request.Method.POST,
                BASE_URL + "cancel_course_by_driver.php",
                response -> {
                    Toast.makeText(this,
                            "Course annulée",
                            Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(this, MapDeliveriesActivity.class));
                    finish();
                },
                error -> Toast.makeText(this,
                        "Erreur annulation",
                        Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("delivery_id", deliveryId);
                p.put("status", "cancelled_by_driver");
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
                txtPrice.setText(String.format("%,.0f FCFA", price));
                btnSurPlace.setVisibility(View.VISIBLE);
                break;
            case ARRIVED:
                txtStatus.setText("📍 Sur place");
                txtPrice.setText(String.format("%,.0f FCFA", price));
                btnDemarrer.setVisibility(View.VISIBLE);
                break;
            case ONGOING:
                txtStatus.setText("🚚 Livraison en cours");
                txtPrice.setText(String.format("%,.0f FCFA", price));
                btnTerminer.setVisibility(View.VISIBLE);
                break;
            case COMPLETED:
                txtStatus.setText("✅ Livraison terminée");
                txtPrice.setText(String.format("%,.0f FCFA", price));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
        }
        // 🔴 Bouton Annuler autorisé uniquement avant démarrage
        if (currentState == CourseState.GOING_TO_PICKUP ||
                currentState == CourseState.ARRIVED) {
            btnAnnuler.setVisibility(View.VISIBLE);
        } else {
            btnAnnuler.setVisibility(View.GONE);
        }
    }

    private void clearRouteLine() {
        if (routeLine != null) {
            routeLine.remove();
            routeLine = null;
        }
    }

    private void cancelCourse(
            String cancelledBy,      // "driver"
            int userId,               // driver_id
            String reasonCode,        // ex: "NO_SHOW"
            String reasonText         // ex: "Client absent"
    ) {

        String url = BASE_URL + "add_course_cancellation.php";

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Localisation non autorisée", Toast.LENGTH_SHORT).show();
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {

            double lat = location != null ? location.getLatitude() : 0;
            double lng = location != null ? location.getLongitude() : 0;

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        Log.e("CANCEL_COURSE", response);

                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.getBoolean("success")) {

                                Toast.makeText(
                                        DeliveryNavigationActivity.this,
                                        "Course annulée avec succès",
                                        Toast.LENGTH_LONG
                                ).show();

                                // 🔁 Retour à l'écran principal
                                Intent intent = new Intent(
                                        DeliveryNavigationActivity.this,
                                        MapDeliveriesActivity.class
                                );
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();

                            } else {
                                Toast.makeText(
                                        DeliveryNavigationActivity.this,
                                        obj.getString("message"),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        } catch (Exception e) {
                            Log.e("CANCEL_PARSE_ERR", e.getMessage());
                        }
                    },
                    error -> Log.e("CANCEL_ERR", error.toString())
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("course_id", deliveryId); // ID de la course
                    params.put("cancelled_by", cancelledBy); // driver
                    params.put("user_id", String.valueOf(userId));
                    params.put("reason_code", reasonCode);
                    params.put("reason_text", reasonText);
                    params.put("latitude", String.valueOf(lat));
                    params.put("longitude", String.valueOf(lng));
                    return params;
                }
            };

            Volley.newRequestQueue(this).add(request);
        });
    }


    private void onSurPlace() {
        updateStatusOnServer("onplace");
        savePositionWithStatus("onplace");

        btnSurPlace.setVisibility(View.GONE);
        btnDemarrer.setVisibility(View.VISIBLE);
        goingToPickup = false;
    }

    private void onDemarrer() {
        updateStatusOnServer("ongoing");
        savePositionWithStatus("ongoing");

        btnDemarrer.setVisibility(View.GONE);
        btnTerminer.setVisibility(View.VISIBLE);
    }

    private void onTerminer() {
        updateStatusOnServer("completed");
        savePositionWithStatus("completed");

        btnTerminer.setVisibility(View.GONE);

        //Toast.makeText(this, "Livraison terminée "+driverId+":"+deliveryId, Toast.LENGTH_LONG).show();
        sendCommissionAndUpdateBalance(commission);
        startActivity(new Intent(this, MapDeliveriesActivity.class));
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
                Map<String,String> p = new HashMap<>();
                p.put("delivery_id", deliveryId);
                p.put("driver_id", String.valueOf(driverId));
                p.put("commission", String.valueOf(commission));
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);

    }

    private void updateStatusOnServer(String newStatus) {
        StringRequest req = new StringRequest(Request.Method.POST, URL_STATUS,
                response -> Log.e("STATUS_UPDATE", response),
                error -> Log.e("STATUS_ERR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("delivery_id", deliveryId);
                p.put("status", newStatus);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void savePositionWithStatus(String status) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        locationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc == null) return;
            checkSecurityAndSendPosition(status, loc);
        });
    }

    private void checkSecurityAndSendPosition(String statusc, Location loc) {
        String url = BASE_URL + "save_delivery_position.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {},
                error -> Log.e("SECURITY_ERR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("delivery_id", deliveryId);
                p.put("driver_id", String.valueOf(driverId));
                p.put("status", statusc);
                p.put("lat", String.valueOf(loc.getLatitude()));
                p.put("lng", String.valueOf(loc.getLongitude()));
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);




    }

    private void showPickupDropMarkers() {
        mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .position(new LatLng(pickupLat, pickupLng))
                .title("Lieu de récupération")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .position(new LatLng(dropLat, dropLng))
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(pickupLat, pickupLng), 14));
    }

    private void requestRoute(double fromLat, double fromLng, double toLat, double toLng) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + fromLng + "," + fromLat + ";" + toLng + "," + toLat
                + "?geometries=geojson&overview=full";

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray coords = obj.getJSONArray("routes")
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

                        clearRouteLine();
                        routeLine = mMap.addPolyline(polyline);

                    } catch (Exception e) {
                        Log.e("ROUTE_ERR", e.getMessage());
                    }
                },
                error -> Log.e("ROUTE_FAIL", error.toString())
        );
        Volley.newRequestQueue(this).add(req);
    }

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

                        if (driverMarker == null) {
                            driverMarker = mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                                    .position(driverPos)
                                    .title("Vous")
                                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMarker(R.drawable.icmoto, 80, 80))));
                        } else {
                            driverMarker.setPosition(driverPos);
                        }

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverPos, 16));

                        // 🔹 Mise à jour dynamique du tracé
                        if (currentState == CourseState.GOING_TO_PICKUP && routeLine == null) {
                            requestRoute(loc.getLatitude(), loc.getLongitude(), pickupLat, pickupLng);
                        } else if (currentState == CourseState.ONGOING) {
                            requestRoute(loc.getLatitude(), loc.getLongitude(), dropLat, dropLng);
                        }
                    }
                },
                getMainLooper()
        );
    }
}
