package deydemv3;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.common.api.ResolvableApiException;
import android.content.IntentSender;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.pisco.deydempro3.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
public class RideSelectActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    TextView tvprise, tvdepot;
    TextView tvadress1,tvadress2;
    private static final int PICKUP_REQUEST = 1001;
    private static final int DROPOFF_REQUEST = 1002;
    LatLng pickupLatLng, dropoffLatLng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ride_select);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        tvprise = findViewById(R.id.prise);
        tvadress1 = findViewById(R.id.tvadress);

        tvdepot = findViewById(R.id.depot);
        tvadress2 = findViewById(R.id.tvadress2);

        tvprise.setOnClickListener(v -> openAutocomplete(PICKUP_REQUEST));
        tvadress1.setOnClickListener(v -> openAutocomplete(PICKUP_REQUEST));

        tvdepot.setOnClickListener(v -> openAutocomplete(DROPOFF_REQUEST));
        tvadress2.setOnClickListener(v -> openAutocomplete(DROPOFF_REQUEST));

        checkGPS();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == 1001) {
                if (resultCode == RESULT_OK) {
                    getUserLocation(); // 🔥 centre la map après activation
                }
            }
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);

                if (requestCode == PICKUP_REQUEST) {
                    pickupLatLng = place.getLatLng();
                    if (tvprise != null) tvadress1.setText(place.getAddress());
                    //btnClearPickup.setVisibility(View.VISIBLE);
                }

                if (requestCode == DROPOFF_REQUEST) {
                    dropoffLatLng = place.getLatLng();
                    if (tvdepot != null) tvadress2.setText(place.getAddress());
                    //btnClearDropoff.setVisibility(View.VISIBLE);
                }

                if (pickupLatLng != null && dropoffLatLng != null) {
                    updateMap(); // 🔥 TRACE AUTOMATIQUE
                }

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("Places", "Status: " + status.getStatusMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
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
                mMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                        .center(userLocation)
                        .radius(100)
                        .strokeColor(Color.BLUE)
                        .fillColor(0x220000FF)
                        .strokeWidth(2));

                // 🔥 récupérer adresse
                getAddressFromLocation(userLocation);
                pickupLatLng = userLocation;
            }
        });
    }

    private void getAddressFromLocation(LatLng latLng) {

        new Thread(() -> {
            try {
                android.location.Geocoder geocoder =
                        new android.location.Geocoder(this, java.util.Locale.getDefault());

                java.util.List<android.location.Address> addresses =
                        geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {

                    String address = addresses.get(0).getAddressLine(0);

                    runOnUiThread(() -> {
                        tvadress1.setText(address); // 🔥 ICI
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateMap() {
        if (mMap == null) return;

        mMap.clear();

        if (pickupLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(pickupLatLng)
                    .title("Départ")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        if (dropoffLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(dropoffLatLng)
                    .title("Destination"));
        }

        // 🔥 tracer route automatiquement
        if (pickupLatLng != null && dropoffLatLng != null) {

            Log.d("ROUTE", "pickup: " + pickupLatLng);
            Log.d("ROUTE", "drop: " + dropoffLatLng);

            drawOSRMRoute(pickupLatLng, dropoffLatLng);

            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(pickupLatLng)
                    .include(dropoffLatLng)
                    .build();

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        }
    }

//    private void updateMap() {
//        if (mMap == null) return;
//
//        mMap.clear();
//
//        if (pickupLatLng != null) {
//            mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup")
//                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
//        }
//
//        if (dropoffLatLng != null) {
//            mMap.addMarker(new MarkerOptions().position(dropoffLatLng).title("Dropoff"));
//        }
//
//        if (pickupLatLng != null && dropoffLatLng != null) {
//
//            double distanceKm = calculateDistance(
//                    pickupLatLng.latitude, pickupLatLng.longitude,
//                    dropoffLatLng.latitude, dropoffLatLng.longitude
//            );
//
//           // if (tvDistance != null) tvDistance.setText(String.format(Locale.US, "Distance : %.2f km", distanceKm));
//
//            drawOSRMRoute(pickupLatLng, dropoffLatLng);
//            //updatePrice();
//
//            LatLngBounds bounds = new LatLngBounds.Builder()
//                    .include(pickupLatLng)
//                    .include(dropoffLatLng)
//                    .build();
//
//            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
//        }
//    }
private void drawOSRMRoute(LatLng origin, LatLng destination) {
    new Thread(() -> {
        try {

            String url = "https://router.project-osrm.org/route/v1/driving/"
                    + origin.longitude + "," + origin.latitude + ";"
                    + destination.longitude + "," + destination.latitude
                    + "?overview=full&geometries=geojson";

            Log.d("ROUTE_URL", url);

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            in.close();

            JSONObject json = new JSONObject(response.toString());

            JSONArray coords = json.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");

            PolylineOptions polylineOptions = new PolylineOptions()
                    .width(10f)
                    .color(Color.BLUE);

            for (int i = 0; i < coords.length(); i++) {
                JSONArray c = coords.getJSONArray(i);
                double lng = c.getDouble(0);
                double lat = c.getDouble(1);
                polylineOptions.add(new LatLng(lat, lng));
            }

            runOnUiThread(() -> {
                if (mMap != null) {
                    mMap.addPolyline(polylineOptions);
                }
            });

        } catch (Exception e) {
            Log.e("ROUTE_ERROR", e.toString());
        }
    }).start();
}
//    private void drawOSRMRoute(LatLng origin, LatLng destination) {
//        new Thread(() -> {
//            try {
//                String url = "http://router.project-osrm.org/route/v1/driving/"
//                        + origin.longitude + "," + origin.latitude + ";"
//                        + destination.longitude + "," + destination.latitude
//                        + "?overview=full&geometries=geojson";
//
//                URL obj = new URL(url);
//                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//                con.setRequestMethod("GET");
//
//                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//                StringBuilder response = new StringBuilder();
//                String line;
//                while ((line = in.readLine()) != null) response.append(line);
//                in.close();
//
//                JSONObject json = new JSONObject(response.toString());
//                JSONArray coords = json.getJSONArray("routes")
//                        .getJSONObject(0)
//                        .getJSONObject("geometry")
//                        .getJSONArray("coordinates");
//
//                PolylineOptions polylineOptions = new PolylineOptions();
//                polylineOptions.width(12f);
//                polylineOptions.color(Color.BLUE);
//
//                for (int i = 0; i < coords.length(); i++) {
//                    JSONArray c = coords.getJSONArray(i);
//                    double lng = c.getDouble(0);
//                    double lat = c.getDouble(1);
//                    polylineOptions.add(new LatLng(lat, lng));
//                }
//
//                runOnUiThread(() -> {
//                    if (mMap != null) mMap.addPolyline(polylineOptions);
//                });
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }


    private void openAutocomplete(int requestCode) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );

        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields
        )
                .setCountries(Arrays.asList("SN"))   // Limite au Sénégal
                .build(this);

        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate(); // recharge l'activité
        }
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // ✅ afficher bouton localisation
        mMap.setMyLocationEnabled(true);

        // ✅ récupérer position utilisateur
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {

                        LatLng userLocation = new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        );

                        // déplacer caméra
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

                        mMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                                .center(userLocation)
                                .radius(100) // mètres
                                .strokeColor(Color.BLUE)
                                .fillColor(0x220000FF)
                                .strokeWidth(2));
                    }
                });
    }
}