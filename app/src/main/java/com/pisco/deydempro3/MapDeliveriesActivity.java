package com.pisco.deydempro3;

import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MapDeliveriesActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener {

    GoogleMap mMap;

    HashMap<Marker, MapDelivery> markerDeliveries = new HashMap<>();

    String URL = "http://192.168.1.7/deydemlivraisonphpmysql/get_deliveries_map.php";
    String URL_ACCEPT = "http://192.168.1.7/deydemlivraisonphpmysql/accept_delivery.php";

    View infoView;
    TextView infoPickup, infoDropoff, infoPrice;
    Button btnAccept;

    MapDelivery selectedDelivery = null;
    private final int REFRESH_INTERVAL = 3000; // 3 sec
    private android.os.Handler refreshHandler = new android.os.Handler();
    boolean firstLocation = true;
    FusedLocationProviderClient fusedLocationClient;
    Marker driverMarker;
    boolean firstFix = true;

    Bitmap resizeMarker(int drawable, int width, int height) {
        Bitmap image = BitmapFactory.decodeResource(getResources(), drawable);
        return Bitmap.createScaledBitmap(image, width, height, false);
    }


    @SuppressLint("PotentialBehaviorOverride")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_deliveries);

        // Load the custom popup
        infoView = getLayoutInflater().inflate(R.layout.delivery_info_window, null);

        infoPickup = infoView.findViewById(R.id.infoPickup);
        infoDropoff = infoView.findViewById(R.id.infoDropoff);
        infoPrice = infoView.findViewById(R.id.infoPrice);
        btnAccept = infoView.findViewById(R.id.btnAccept);

        btnAccept.setOnClickListener(v -> acceptDelivery());

        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;

//            LatLng dakar = new LatLng(14.695, -17.444);
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dakar, 11));
            //mMap.setOnMarkerClickListener(this);
            // 1) Adapter pour remplir l'info window avec notre layout
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                // Utilisé pour remplacer toute la fenêtre (retourne View)
                @Override
                public View getInfoWindow(Marker marker) {
                    // On utilise getInfoContents au lieu de getInfoWindow pour laisser bordures par défaut
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    // inflate et remplir la vue
                    View v = getLayoutInflater().inflate(R.layout.delivery_info_window, null);
                    TextView infoPickup = v.findViewById(R.id.infoPickup);
                    TextView infoDropoff = v.findViewById(R.id.infoDropoff);
                    TextView infoPrice = v.findViewById(R.id.infoPrice);

                    MapDelivery d = markerDeliveries.get(marker);
                    if (d != null) {
                        infoPickup.setText("Pickup : " + d.pickup);
                        infoDropoff.setText("Dropoff : " + d.dropoff);
                        infoPrice.setText("Prix : " + d.price + " FCFA");
                    } else {
                        infoPickup.setText(marker.getTitle());
                        infoDropoff.setText("");
                        infoPrice.setText("");
                    }
                    return v;
                }
            });

// 2) Au clic sur un marker, on affiche l'info window
            mMap.setOnMarkerClickListener(marker -> {
                // enregistre la livraison sélectionnée
                selectedDelivery = markerDeliveries.get(marker);
                marker.showInfoWindow();
                // retourner true pour consommer l'événement (on a appelé showInfoWindow nous-mêmes)
                return true;
            });

// 3) Clic sur l'info window -> déclenche acceptation
            mMap.setOnInfoWindowClickListener(marker -> {
                MapDelivery d = markerDeliveries.get(marker);
                if (d != null) {
                    selectedDelivery = d;
                    acceptDelivery(); // ta méthode d'accept
                }
            });

            loadDeliveries();
            startAutoRefresh();

        });
        startLocationUpdates();

    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        LocationRequest request = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location loc = locationResult.getLastLocation();
                if (loc == null) return;

                LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());

                // Ajouter / mettre à jour marker du livreur
                if (driverMarker == null) {

                    driverMarker = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("Moi")
                            .icon(BitmapDescriptorFactory.fromBitmap(
                                    resizeMarker(R.drawable.icmoto, 80, 80)
                            ))
                    );

                } else {
                    driverMarker.setPosition(pos);
                }

                // Centrage automatique AU PREMIER FIX
                if (firstFix) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
                    firstFix = false;
                }
            }
        }, getMainLooper());
    }

    private void startAutoRefresh() {
        refreshHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadDeliveries(); // recharge proprement
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }, REFRESH_INTERVAL);
    }


    private void loadDeliveries() {

        StringRequest req = new StringRequest(Request.Method.GET, URL,
                response -> {

                    try {

                        JSONArray arr = new JSONArray(response);

                        // Liste temporaire pour savoir quels markers conserver
                        HashMap<String, Boolean> activeIds = new HashMap<>();

                        for (int i = 0; i < arr.length(); i++) {

                            JSONObject o = arr.getJSONObject(i);

                            String id = o.getString("id");
                            activeIds.put(id, true);

                            MapDelivery newDelivery = new MapDelivery(
                                    id,
                                    o.getString("pickup_address"),
                                    o.getDouble("pickup_lat"),
                                    o.getDouble("pickup_lng"),
                                    o.getString("dropoff_address"),
                                    o.getDouble("dropoff_lat"),
                                    o.getDouble("dropoff_lng"),
                                    o.getString("price")
                            );

                            // Vérifier si ce marker existe déjà
                            boolean exists = false;

                            for (Marker m : markerDeliveries.keySet()) {
                                if (markerDeliveries.get(m).id.equals(id)) {
                                    exists = true;
                                    break;
                                }
                            }

                            // Nouveau marker à ajouter
                            if (!exists) {
                                Marker pickupMarker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(newDelivery.pickupLat, newDelivery.pickupLng))
                                        .title("Pickup : " + newDelivery.pickup)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                );
                                markerDeliveries.put(pickupMarker, newDelivery);
                            }
                        }

                        // Supprimer les markers qui n'existent plus
                        for (Marker m : new HashMap<>(markerDeliveries).keySet()) {
                            if (!activeIds.containsKey(markerDeliveries.get(m).id)) {
                                m.remove();
                                markerDeliveries.remove(m);
                            }
                        }

                    } catch (Exception e) {
                        Log.e("ERR", e.getMessage());
                    }

                },
                error -> {}
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);

    }


    @Override
    public boolean onMarkerClick(Marker marker) {

        selectedDelivery = markerDeliveries.get(marker);

        if (selectedDelivery == null) return false;

        infoPickup.setText("Pickup : " + selectedDelivery.pickup);
        infoDropoff.setText("Dropoff : " + selectedDelivery.dropoff);
        infoPrice.setText("Prix : " + selectedDelivery.price + " FCFA");

        infoView.setVisibility(View.VISIBLE);

        return true;
    }

    private void acceptDelivery() {
        if (selectedDelivery == null) return;

        StringRequest req = new StringRequest(Request.Method.POST, URL_ACCEPT,
                response -> {
                    Toast.makeText(this, "Livraison acceptée", Toast.LENGTH_LONG).show();

                    // REDIRECTION AVEC LES INFOS
                    Intent i = new Intent(this, DeliveryNavigationActivity.class);
                    i.putExtra("delivery_id", selectedDelivery.id);
                    i.putExtra("pickup_lat", selectedDelivery.pickupLat);
                    i.putExtra("pickup_lng", selectedDelivery.pickupLng);
                    i.putExtra("drop_lat", selectedDelivery.dropLat);
                    i.putExtra("drop_lng", selectedDelivery.dropLng);
                    i.putExtra("pickup_address", selectedDelivery.pickup);
                    i.putExtra("dropoff_address", selectedDelivery.dropoff);
                    i.putExtra("price", selectedDelivery.price);
                    startActivity(i);

                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> d = new HashMap<>();
                d.put("delivery_id", selectedDelivery.id);
                d.put("driver_id", "1");
                return d;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacksAndMessages(null);
    }




}
