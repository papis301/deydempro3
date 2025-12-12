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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class DeliveryNavigationActivity extends FragmentActivity {

    GoogleMap mMap;
    double pickupLat, pickupLng, dropLat, dropLng;

    FusedLocationProviderClient locationClient;
    Marker driverMarker;
    Polyline routeLine;

    Button btnSurPlace, btnDemarrer, btnTerminer;
    int deliveryId;
    String status = "going_to_pickup";


    boolean goingToPickup = true; // phase 1 : vers le pickup
    Bitmap resizeMarker(int drawable, int width, int height) {
        Bitmap image = BitmapFactory.decodeResource(getResources(), drawable);
        return Bitmap.createScaledBitmap(image, width, height, false);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_navigation);

        pickupLat = getIntent().getDoubleExtra("pickup_lat", 0);
        pickupLng = getIntent().getDoubleExtra("pickup_lng", 0);
        dropLat = getIntent().getDoubleExtra("drop_lat", 0);
        dropLng = getIntent().getDoubleExtra("drop_lng", 0);

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        btnSurPlace = findViewById(R.id.btnSurPlace);
        btnDemarrer = findViewById(R.id.btnDemarrer);
        btnTerminer = findViewById(R.id.btnTerminer);

        deliveryId = getIntent().getIntExtra("delivery_id", 0);

// Listeners
        btnSurPlace.setOnClickListener(v -> onSurPlace());
        btnDemarrer.setOnClickListener(v -> onDemarrer());
        btnTerminer.setOnClickListener(v -> onTerminer());


        // service GPS
        startService(new Intent(this, DriverLocationService.class));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapNav);

        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            showPickupDropMarkers();
            startLiveTracking();
        });
    }

    private void onSurPlace() {
        status = "arrived_pickup";

        btnSurPlace.setVisibility(View.GONE);
        btnDemarrer.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Vous êtes sur place", Toast.LENGTH_SHORT).show();
    }

    private void onDemarrer() {
        status = "delivering";

        btnDemarrer.setVisibility(View.GONE);
        btnTerminer.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Livraison démarrée", Toast.LENGTH_SHORT).show();

        // tracer route pickup -> dropoff
        drawRouteToDropoff();
    }

    private void onTerminer() {
        status = "completed";

        btnTerminer.setVisibility(View.GONE);

        Toast.makeText(this, "Livraison terminée", Toast.LENGTH_LONG).show();

        finish(); // ou redirection vers écran résultat
    }

    private void drawRouteToDropoff() {
        LatLng pickup = new LatLng(pickupLat, pickupLng);
        LatLng drop = new LatLng(dropLat, dropLng);

        mMap.addPolyline(new PolylineOptions()
                .add(pickup, drop)
                .width(10)
                .color(0xFFFF8800)
        );
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
