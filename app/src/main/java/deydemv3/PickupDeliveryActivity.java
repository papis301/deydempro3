package deydemv3;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.pisco.deydempro3.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PickupDeliveryActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    // bottom sheet views (from inflated view)
    TextView tvPickup, tvDropoff, tvDistance, tvPrice, priceMoto, priceVoiture;
    Button btnSelectPickup, btnSelectDropoff, btnconfirme;
    Button btnMoto, btnVoiture;

    // top-right menu cards (in activity layout)
    MaterialCardView btnTaxi, btnCourses, btnSettings, btnProfil;
    String vehicle = "";

    String userId, tel;

    LatLng pickupLatLng = null;
    LatLng dropoffLatLng = null;

    private static final int PICKUP_REQUEST = 1001;
    private static final int DROPOFF_REQUEST = 1002;
    String mode = "delivery";
    private boolean priceCalculated = false;
    ImageView btnClearPickup, btnClearDropoff;
    private double calculatedPrice = 0;

    //String vehicle = "Moto"; // valeur par défaut

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup_delivery);

        // Récupérer user_id depuis SharedPreferences
        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        userId = sp.getString("user_id", "0");
        //tel = sp.getString("phone", "");

        Toast.makeText(this, "ID user connecté : " + userId , Toast.LENGTH_SHORT).show();

        // Top-right menu (activity layout)
        btnTaxi = findViewById(R.id.btnTaxi);
        btnCourses = findViewById(R.id.btnCourses);
        btnSettings = findViewById(R.id.btnSettings);
        btnProfil = findViewById(R.id.btnProfil);

        btnProfil.setOnClickListener(v ->
                startActivity(new Intent(PickupDeliveryActivity.this,
                        ProfileActivity.class)));


        // Crée et inflates le bottom sheet (mais NE l'affiche PAS tout de suite)
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_vehicle_options, null);

        // Init des vues du bottom sheet à partir de sheetView (TOUJOURS utiliser sheetView.findViewById)
        btnMoto = sheetView.findViewById(R.id.btnMoto);
        btnVoiture = sheetView.findViewById(R.id.btnVoiture);

        tvPickup = sheetView.findViewById(R.id.tvPickup);
        tvDropoff = sheetView.findViewById(R.id.tvDropoff);
        tvDistance = sheetView.findViewById(R.id.tvDistance);
        tvPrice = sheetView.findViewById(R.id.tvPrice);

        btnSelectPickup = sheetView.findViewById(R.id.btnPickup);
        btnSelectDropoff = sheetView.findViewById(R.id.btnDropoff);
        btnconfirme = sheetView.findViewById(R.id.btnConfirm);
        priceMoto = sheetView.findViewById(R.id.priceMoto);
        priceVoiture = sheetView.findViewById(R.id.priceVoiture);

        // Cards for vehicle (if present in layout)
        MaterialCardView cardMoto = sheetView.findViewById(R.id.cardMoto);
        MaterialCardView cardVoiture = sheetView.findViewById(R.id.cardVoiture);
        btnClearPickup = sheetView.findViewById(R.id.btnClearPickup);
        btnClearDropoff = sheetView.findViewById(R.id.btnClearDropoff);
        // Map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Listeners pour ouvrir Places Autocomplete
        btnSelectPickup.setOnClickListener(v -> openAutocomplete(PICKUP_REQUEST));
        btnSelectDropoff.setOnClickListener(v -> openAutocomplete(DROPOFF_REQUEST));

        // Confirm button
        btnconfirme.setOnClickListener(v -> {
            if (pickupLatLng == null) {
                showError("Veuillez choisir le lieu de recuperation.");
                return;
            }
            if (dropoffLatLng == null) {
                showError("Veuillez choisir la destination.");
                return;
            }
            sendCourseToServer();
        });

        // checkbox / boutons moto / voiture (UI)
        // si tu as de vrais boutons "btnMoto/btnVoiture"
        if (btnMoto != null && btnVoiture != null) {
            // style initial
            highlightSelected(btnMoto, btnVoiture);

            btnMoto.setOnClickListener(v -> {
                vehicle = "Moto";
                highlightSelected(btnMoto, btnVoiture);
                // reflect on card stroke if card exists
                if (cardMoto != null && cardVoiture != null) {
                    cardMoto.setStrokeColor(Color.BLACK);
                    cardVoiture.setStrokeColor(Color.TRANSPARENT);
                }
                updatePrice();
            });

            btnVoiture.setOnClickListener(v -> {
                vehicle = "Voiture";
                highlightSelected(btnVoiture, btnMoto);
                if (cardMoto != null && cardVoiture != null) {
                    cardVoiture.setStrokeColor(Color.BLACK);
                    cardMoto.setStrokeColor(Color.TRANSPARENT);
                }
                updatePrice();
            });
        }

        // card clicks (si tu utilises les cards)
        if (cardMoto != null && cardVoiture != null) {
            cardMoto.setOnClickListener(v -> {

                vehicle = "moto";

                cardMoto.setStrokeColor(Color.parseColor("#00C569"));
                cardVoiture.setStrokeColor(Color.TRANSPARENT);

                cardMoto.setCardElevation(8f);
                cardVoiture.setCardElevation(2f);

                updatePrice();
            });

            cardVoiture.setOnClickListener(v -> {

                vehicle = "voiture";

                cardVoiture.setStrokeColor(Color.parseColor("#00C569"));
                cardMoto.setStrokeColor(Color.TRANSPARENT);

                cardVoiture.setCardElevation(8f);
                cardMoto.setCardElevation(2f);

                updatePrice();
            });
        }

        // Quand l'utilisateur clique sur "Courses" (menu), on affiche le bottom sheet
        btnCourses.setOnClickListener(v -> {
            cardVoiture.setVisibility(View.GONE);
            vehicle = "moto";
            bottomSheet.setContentView(sheetView);
            bottomSheet.show();
            bottomSheet.setOnShowListener(dialog -> {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                View sheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (sheet != null) {
                    sheet.setBackgroundColor(Color.TRANSPARENT);

                    // Hauteur max = 70% de l'écran
                    int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);
                    sheet.getLayoutParams().height = height;

                    // Marge bottom réelle (50dp)
                    int marginBottom = (int) (50 * getResources().getDisplayMetrics().density);

                    // Cast correct
                    ViewGroup.LayoutParams layoutParams = sheet.getLayoutParams();
                    if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin = marginBottom;
                        sheet.setLayoutParams(layoutParams);
                    }

                    sheet.requestLayout();
                }

            });

        });

        btnTaxi.setOnClickListener(v -> {
            cardMoto.setVisibility(View.GONE);
            vehicle = "voiture";
            bottomSheet.setContentView(sheetView);
            bottomSheet.show();
            bottomSheet.setOnShowListener(dialog -> {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                View sheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (sheet != null) {
                    sheet.setBackgroundColor(Color.TRANSPARENT);

                    // Hauteur max = 70% de l'écran
                    int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);
                    sheet.getLayoutParams().height = height;

                    // Marge bottom réelle (50dp)
                    int marginBottom = (int) (50 * getResources().getDisplayMetrics().density);

                    // Cast correct
                    ViewGroup.LayoutParams layoutParams = sheet.getLayoutParams();
                    if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin = marginBottom;
                        sheet.setLayoutParams(layoutParams);
                    }

                    sheet.requestLayout();
                }

            });

        });

        btnClearPickup.setOnClickListener(v -> {

            pickupLatLng = null;
            tvPickup.setText("Aucun lieu choisi");
            btnClearPickup.setVisibility(View.GONE);

            resetAfterClear();
        });

        btnClearDropoff.setOnClickListener(v -> {

            dropoffLatLng = null;
            tvDropoff.setText("Aucun lieu choisi");
            btnClearDropoff.setVisibility(View.GONE);

            resetAfterClear();
        });


        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(PickupDeliveryActivity.this, DashboardActivity.class)));

    }

    private void resetAfterClear() {

        if (mMap != null) {
            mMap.clear();
        }

        tvDistance.setText("Distance : -");
        tvPrice.setText("Prix : 0 FCFA");

        priceCalculated = false;

    }

    private void assignDelivery(int deliveryId){

        String url = "https://pisco.alwaysdata.net/assign_delivery_auto.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {

                    try {

                        JSONObject obj = new JSONObject(response);

                        if(obj.getBoolean("success")){

                            int driverId = obj.getInt("driver_id");
                            double distance = obj.getDouble("distance_km");

                            Log.d("ASSIGN", "Driver=" + driverId + " Distance=" + distance);

                            Toast.makeText(this,
                                    "Chauffeur trouvé à " + distance + " km",
                                    Toast.LENGTH_LONG).show();

                        } else {

                            Toast.makeText(this,
                                    obj.getString("message"),
                                    Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                },
                error -> Toast.makeText(this, "Erreur assignation", Toast.LENGTH_LONG).show()
        ){

            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("delivery_id", String.valueOf(deliveryId));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }



    private void highlightSelected(Button selected, Button other) {
        if (selected != null) {
            selected.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00C569")));
            selected.setTextColor(Color.WHITE);
        }
        if (other != null) {
            other.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
            other.setTextColor(Color.BLACK);
        }
    }

    private void showError(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Information requise")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void updatePrice() {

        if (pickupLatLng == null || dropoffLatLng == null) {
            return;
        }

        priceCalculated = false;
        //btnconfirme.setEnabled(false);
        tvPrice.setText("Calcul en cours...");

        String url = "https://pisco.alwaysdata.net/calculate_price.php";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Log.d("PRICE_RESPONSE", response);
                    try {
                        JSONObject json = new JSONObject(response);

                        if (json.getBoolean("success")) {

                            double distance = json.getDouble("distance_km");
                            //double price = json.getDouble("price");

                            calculatedPrice = json.getDouble("price");

                            tvPrice.setText("Prix : " + (int) calculatedPrice + " FCFA");
                            tvDistance.setText("Distance : " + distance + " km");
                           // tvPrice.setText("Prix : " + price + " FCFA");

                            priceMoto.setText(calculatedPrice + " FCFA");
                            priceVoiture.setText(calculatedPrice + " FCFA");

                            priceCalculated = true;
                            btnconfirme.setEnabled(true); // 🔥 activation ici

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                },
                error -> {
                    Log.e("PRICE_ERROR", error.toString());
                    tvPrice.setText("Erreur calcul");
                    btnconfirme.setEnabled(false);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();
                params.put("pickup_lat", String.valueOf(pickupLatLng.latitude));
                params.put("pickup_lng", String.valueOf(pickupLatLng.longitude));
                params.put("drop_lat", String.valueOf(dropoffLatLng.latitude));
                params.put("drop_lng", String.valueOf(dropoffLatLng.longitude));
                params.put("vehicle_type", vehicle);

                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

//    private void updatePrice() {
//
//        if (tvPrice == null || priceMoto == null || priceVoiture == null) return;
//
//        if (pickupLatLng == null || dropoffLatLng == null) {
//            tvPrice.setText("Prix : 0 FCFA");
//            priceMoto.setText(" : 0 FCFA");
//            priceVoiture.setText(" : 0 FCFA");
//            return;
//        }
//
//        double distanceKm = calculateDistance(
//                pickupLatLng.latitude, pickupLatLng.longitude,
//                dropoffLatLng.latitude, dropoffLatLng.longitude
//        );
//
//        // Prix moto
//        double motoPerKm = 500;
//        double totalMoto;
//        if (distanceKm <= 10) {
//            totalMoto = distanceKm * motoPerKm;
//        } else {
//            totalMoto = (10 * motoPerKm) + (distanceKm - 10) * (motoPerKm / 2.0);
//        }
//
//        // Prix voiture
//        double carPerKm = 700;
//        double totalVoiture;
//        if (distanceKm <= 10) {
//            totalVoiture = distanceKm * carPerKm;
//        } else {
//            totalVoiture = (10 * carPerKm) + (distanceKm - 10) * (carPerKm / 2.0);
//        }
//
////        if (isThies()) {
////            totalMoto = Math.max(400, Math.min(1000, totalMoto));
////            totalVoiture = Math.max(400, Math.min(1000, totalVoiture));
////        }
//
//        // Affichage dans les sections Moto / Voiture
//        priceMoto.setText(String.format(Locale.US, "Moto : %.0f FCFA", totalMoto));
//        priceVoiture.setText(String.format(Locale.US, "Voiture : %.0f FCFA", totalVoiture));
//
//        // Affichage principal selon le véhicule sélectionné
//        double chosenPrice = vehicle.equalsIgnoreCase("Moto") ? totalMoto : totalVoiture;
//        tvPrice.setText(String.format(Locale.US, "Prix : %.0f FCFA", chosenPrice));
//
//        // Distance affichée
//        if (tvDistance != null)
//            tvDistance.setText(String.format(Locale.US, "Distance : %.2f km", distanceKm));
//    }


    private void sendCourseToServer() {
        // safety checks
        if (pickupLatLng == null || dropoffLatLng == null) {
            showError("Pickup / Dropoff manquant");
            return;
        }

//        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
//        String clientId = sp.getString("user_id", "0");

        String url = "https://pisco.alwaysdata.net/create_course.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("response : ", response);
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        if (success) {

                            int deliveryId = json.getInt("delivery_id"); // 🔥 récupérer id

                            Toast.makeText(this, "Course enregistrée", Toast.LENGTH_SHORT).show();

                            // 🔥 assigner automatiquement
                           // assignDelivery(deliveryId);

                            startActivity(new Intent(PickupDeliveryActivity.this, DashboardActivity.class));
                        } else {
                            String msg = json.optString("message", "Erreur serveur");
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Réponse serveur invalide", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    String msg = error.getMessage();
                    Toast.makeText(this, "Erreur réseau : " + (msg != null ? msg : "connection"), Toast.LENGTH_LONG).show();
                    Log.d("Message serveur", String.valueOf(error));
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("client_id", userId != null ? userId : "0");
                params.put("pickup", tvPickup != null ? tvPickup.getText().toString() : "");
                params.put("dropoff", tvDropoff != null ? tvDropoff.getText().toString() : "");
                params.put("pickup_lat", String.valueOf(pickupLatLng.latitude));
                params.put("pickup_lng", String.valueOf(pickupLatLng.longitude));
                params.put("drop_lat", String.valueOf(dropoffLatLng.latitude));
                params.put("drop_lng", String.valueOf(dropoffLatLng.longitude));
                // distance plain number
                String dist = tvDistance != null ? tvDistance.getText().toString().replace("Distance : ", "").replace(" km", "") : "0";
                params.put("distance_km", dist.replace(",", "."));
                params.put("vehicle_type", vehicle);
                // price number only
                params.put("price", String.valueOf((int) calculatedPrice));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);

                if (requestCode == PICKUP_REQUEST) {
                    pickupLatLng = place.getLatLng();
                    if (tvPickup != null) tvPickup.setText(place.getAddress());
                    btnClearPickup.setVisibility(View.VISIBLE);
                }

                if (requestCode == DROPOFF_REQUEST) {
                    dropoffLatLng = place.getLatLng();
                    if (tvDropoff != null) tvDropoff.setText(place.getAddress());
                    btnClearDropoff.setVisibility(View.VISIBLE);
                }

                updateMap();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("Places", "Status: " + status.getStatusMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    // Met à jour la carte avec markers, ligne et prix
    private void updateMap() {
        if (mMap == null) return;

        mMap.clear();

        if (pickupLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        if (dropoffLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(dropoffLatLng).title("Dropoff"));
        }

        if (pickupLatLng != null && dropoffLatLng != null) {

            double distanceKm = calculateDistance(
                    pickupLatLng.latitude, pickupLatLng.longitude,
                    dropoffLatLng.latitude, dropoffLatLng.longitude
            );

            if (tvDistance != null) tvDistance.setText(String.format(Locale.US, "Distance : %.2f km", distanceKm));

            drawOSRMRoute(pickupLatLng, dropoffLatLng);
            updatePrice();

            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(pickupLatLng)
                    .include(dropoffLatLng)
                    .build();

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        }
    }

    // Appel OSRM + tracé de la polyline
    private void drawOSRMRoute(LatLng origin, LatLng destination) {
        new Thread(() -> {
            try {
                String url = "http://router.project-osrm.org/route/v1/driving/"
                        + origin.longitude + "," + origin.latitude + ";"
                        + destination.longitude + "," + destination.latitude
                        + "?overview=full&geometries=geojson";

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray coords = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.width(12f);
                polylineOptions.color(Color.BLUE);

                for (int i = 0; i < coords.length(); i++) {
                    JSONArray c = coords.getJSONArray(i);
                    double lng = c.getDouble(0);
                    double lat = c.getDouble(1);
                    polylineOptions.add(new LatLng(lat, lng));
                }

                runOnUiThread(() -> {
                    if (mMap != null) mMap.addPolyline(polylineOptions);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Haversine
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng dakar = new LatLng(14.7167, -17.4677);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dakar, 12));
    }

    private boolean isThies() {
        if (tvPickup == null) return false;

        String addr = tvPickup.getText().toString().toLowerCase(Locale.US);
        return addr.contains("thiès") || addr.contains("thies");
    }

}
