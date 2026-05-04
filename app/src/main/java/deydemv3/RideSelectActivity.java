package deydemv3;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.common.api.ResolvableApiException;
import android.content.IntentSender;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
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
import java.util.Locale;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
public class RideSelectActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    TextView tvprise, tvdepot, tvdistance;
    TextView tvadress1,tvadress2;
    private static final int PICKUP_REQUEST = 1001;
    private static final int DROPOFF_REQUEST = 1002;
    LatLng pickupLatLng, dropoffLatLng = null;
    String selectedVehicle = "";
    Button btnCommande;
    String userId, tel;

    int finalPrixParticulier = 0;
    int finalPrixTaxi = 0;
    int finalPrixConfort = 0;
    double distanceKm = 0;
    int durationMin = 0;
    double distanceValue = 0;
    int durationValue = 0;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ride_select);

        // Récupérer user_id depuis SharedPreferences
        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        userId = sp.getString("user_id", "0");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        tvprise = findViewById(R.id.prise);
        tvadress1 = findViewById(R.id.tvadress);
        tvdistance = findViewById(R.id.distance);

        tvdepot = findViewById(R.id.depot);
        tvadress2 = findViewById(R.id.tvadress2);
        btnCommande = findViewById(R.id.commande);

        tvprise.setOnClickListener(v -> openAutocomplete(PICKUP_REQUEST));
        tvadress1.setOnClickListener(v -> openAutocomplete(PICKUP_REQUEST));

        tvdepot.setOnClickListener(v -> openAutocomplete(DROPOFF_REQUEST));
        tvadress2.setOnClickListener(v -> openAutocomplete(DROPOFF_REQUEST));

        checkGPS();

        LinearLayout btnParticulier = findViewById(R.id.layoutParticulier);
        LinearLayout btnTaxi = findViewById(R.id.layoutTaxi);
        LinearLayout btnConfort = findViewById(R.id.layoutConfort);

        btnParticulier.setOnClickListener(v -> selectVehicle("PARTICULIER"));
        btnTaxi.setOnClickListener(v -> selectVehicle("TAXI"));
        btnConfort.setOnClickListener(v -> selectVehicle("CONFORT"));

        selectVehicle("PARTICULIER");

        btnCommande.setOnClickListener(v -> {

            if (selectedVehicle.isEmpty()) {
                Toast.makeText(this, "Choisissez un véhicule", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pickupLatLng == null || dropoffLatLng == null) {
                Toast.makeText(this, "Choisissez les adresses", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 DONNÉES DE COMMANDE
            Log.d("COMMANDE", "Type: " + selectedVehicle);
            Log.d("COMMANDE", "Pickup: " + pickupLatLng);
            Log.d("COMMANDE", "Drop: " + dropoffLatLng);

            Toast.makeText(this, "Commande envoyée (" + selectedVehicle + ")", Toast.LENGTH_SHORT).show();

            // 👉 ici tu enverras au serveur plus tard
            sendRide();
        });
    }

    private void selectVehicle(String type) {

        selectedVehicle = type;

        LinearLayout p = findViewById(R.id.layoutParticulier);
        LinearLayout t = findViewById(R.id.layoutTaxi);
        LinearLayout c = findViewById(R.id.layoutConfort);

        // reset
        p.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        t.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        c.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        // highlight sélection
        if (type.equals("PARTICULIER")) {
            p.setBackgroundResource(R.drawable.selected_bg);
        } else if (type.equals("TAXI")) {
            t.setBackgroundResource(R.drawable.selected_bg);
        } else if (type.equals("CONFORT")) {
            c.setBackgroundResource(R.drawable.selected_bg);
        }
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

                // 🔥 récupérer adresse
                getAddressFromLocation(userLocation);
                pickupLatLng = userLocation;
            }
        });
    }

    private void getAddressFromLocation(LatLng latLng) {

        new Thread(() -> {
            try {
                Geocoder geocoder =
                        new Geocoder(this, Locale.getDefault());

                List<Address> addresses =
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

            double distanceKm = calculateDistance(
                    pickupLatLng.latitude, pickupLatLng.longitude,
                    dropoffLatLng.latitude, dropoffLatLng.longitude
            );

//            double distanceMeters = json.getJSONArray("routes")
//                    .getJSONObject(0)
//                    .getDouble("distance");
//
//            double durationSec = json.getJSONArray("routes")
//                    .getJSONObject(0)
//                    .getDouble("duration");



// 🔥 sauvegarder
            distanceValue = distanceKm;

// 🔥 mettre à jour prix avec vraie distance
            updatePrice(distanceKm);

// 🔥 afficher distance

            if (tvdistance != null) {
                tvdistance.setText(String.format("Distance : %.2f km", distanceKm));
            }


// 🔥 appel calcul prix
            updatePrice(distanceKm);
            distanceValue = distanceKm;
            Log.d("OSRM", "Distance: " + distanceKm);
            Log.d("OSRM", "Durée: " + durationMin);

            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(pickupLatLng)
                    .include(dropoffLatLng)
                    .build();

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        }


    }

    private String getSelectedPrice() {

        int price = 0;

        if (selectedVehicle.equals("PARTICULIER")) {
            price = finalPrixParticulier;
        } else if (selectedVehicle.equals("TAXI")) {
            price = finalPrixTaxi;
        } else if (selectedVehicle.equals("CONFORT")) {
            price = finalPrixConfort;
        }

        return String.valueOf(price);
    }

//
private void sendRide() {

    String url = "https://pisco.alwaysdata.net/create_ride.php"; // 🔥 remplace

    com.android.volley.RequestQueue queue =
            com.android.volley.toolbox.Volley.newRequestQueue(this);

    com.android.volley.toolbox.StringRequest request =
            new com.android.volley.toolbox.StringRequest(
                    com.android.volley.Request.Method.POST,
                    url,

                    response -> {
                        try {
                            org.json.JSONObject json = new org.json.JSONObject(response);

                            if (json.getBoolean("success")) {

                                String rideId = json.getString("ride_id");

                                android.widget.Toast.makeText(this,
                                        "Commande envoyée 🚀 ID: " + rideId,
                                        android.widget.Toast.LENGTH_LONG).show();

                                // 👉 ici tu peux passer à écran suivant
                                // startActivity(new Intent(this, WaitingDriverActivity.class));
                                Intent i = new Intent(this, WaitingDriverActivity.class);
                                i.putExtra("ride_id", rideId);
                                startActivity(i);

                            } else {
                                android.widget.Toast.makeText(this,
                                        "Erreur serveur",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },

                    error -> {
                        android.widget.Toast.makeText(this,
                                "Erreur réseau ❌",
                                android.widget.Toast.LENGTH_SHORT).show();

                        android.util.Log.e("API_ERROR", error.toString());
                    }
            ) {

                @Override
                protected java.util.Map<String, String> getParams() {

                    java.util.Map<String, String> params = new java.util.HashMap<>();

                    params.put("client_id", String.valueOf(userId)); // 🔥 adapte
                    params.put("trip_type", "taxi"); // ou livraison

                    params.put("pickup_address", tvadress1.getText().toString());
                    params.put("pickup_lat", String.valueOf(pickupLatLng.latitude));
                    params.put("pickup_lng", String.valueOf(pickupLatLng.longitude));

                    params.put("dropoff_address", tvadress2.getText().toString());
                    params.put("dropoff_lat", String.valueOf(dropoffLatLng.latitude));
                    params.put("dropoff_lng", String.valueOf(dropoffLatLng.longitude));

                    params.put("price", getSelectedPrice());
                    params.put("distance_km", String.valueOf(distanceValue));
                    params.put("vehicle_type", selectedVehicle.toLowerCase());

                    return params;
                }
            };

    queue.add(request);
}

    private String detectCity(LatLng latLng) {
        try {
            Geocoder geocoder =
                    new Geocoder(this, Locale.getDefault());

            List<Address> addresses =
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {

                String city = addresses.get(0).getLocality(); // ville

                if (city != null) {
                    if (city.toLowerCase().contains("dakar")) return "DAKAR";
                    if (city.toLowerCase().contains("thies") || city.toLowerCase().contains("thiès")) return "THIES";
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "OTHER";
    }

    private void updatePrice(double distanceKm) {

        // 🔥 détecter ville (on prend le pickup)
        String city = detectCity(pickupLatLng);

        double baseParticulier = 0, perKmParticulier = 0;
        double baseTaxi = 0, perKmTaxi = 0;
        double baseConfort = 0, perKmConfort = 0;

        // 🔥 TARIFS PAR VILLE
        if (city.equals("DAKAR")) {

             baseParticulier = 700;
             perKmParticulier = 300;

             baseTaxi = 1000;
             perKmTaxi = 350;

             baseConfort = 900;
             perKmConfort = 350;

        } else if (city.equals("THIES")) {

            baseParticulier = 500;
            perKmParticulier = 150;

            baseTaxi = 800;
            perKmTaxi = 200;

            baseConfort = 700;
            perKmConfort = 200;

        } 


        // 🔥 CALCUL
        int prixParticulier = (int) Math.round(baseParticulier + (distanceKm * perKmParticulier));
        int prixTaxi = (int) Math.round(baseTaxi + (distanceKm * perKmTaxi));
        int prixConfort = (int) Math.round(baseConfort + (distanceKm * perKmConfort));

        // 🔥 MINIMUM (important)
        //if (prixParticulier < 600) prixParticulier = 600;
        //if (prixTaxi < 1000) prixTaxi = 1000;
        //if (prixConfort < 900) prixConfort = 900;

         finalPrixParticulier = prixParticulier;
         finalPrixTaxi = prixTaxi;
         finalPrixConfort = prixConfort;
        runOnUiThread(() -> {

            TextView p1 = findViewById(R.id.priceParticulier);
            TextView p2 = findViewById(R.id.priceTaxi);
            TextView p3 = findViewById(R.id.priceConfort);

            if (p1 != null) p1.setText(finalPrixParticulier + " FCFA");
            if (p2 != null) p2.setText(finalPrixTaxi + " FCFA");
            if (p3 != null) p3.setText(finalPrixConfort + " FCFA");
        });
    }

private void drawOSRMRoute(LatLng origin, LatLng destination) {
    new Thread(() -> {
        try {

            String url = "http://router.project-osrm.org/route/v1/driving/"
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

            JSONObject route = json.getJSONArray("routes").getJSONObject(0);

            double distanceMeters = route.getDouble("distance");
            double durationSec = route.getDouble("duration");

// 🔥 conversion
            double distanceKm = distanceMeters / 1000.0;
            int durationMin = (int) (durationSec / 60);

            distanceValue = distanceKm;
            durationValue = durationMin;

            runOnUiThread(() -> {
                if (mMap != null) {
                    mMap.addPolyline(polylineOptions);
                }
                tvdistance.setText(
                        String.format("%.2f km • %d min", distanceKm, durationMin)
                );
                Log.e("distance",  String.format(Locale.getDefault(), "%.2f km • %d min", distanceKm, durationMin));
            });

        } catch (Exception e) {
            Log.e("ROUTE_ERROR", e.toString());
        }
    }).start();
}


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

                        mMap.addCircle(new CircleOptions()
                                .center(userLocation)
                                .radius(100) // mètres
                                .strokeColor(Color.BLUE)
                                .fillColor(0x220000FF)
                                .strokeWidth(2));
                    }
                });
    }
}