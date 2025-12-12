package com.pisco.deydempro3;

import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
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
        });
    }

    private void loadDeliveries() {

        StringRequest req = new StringRequest(Request.Method.GET, URL,
                response -> {
                    Log.e("DEBUG_JSON", response);

                    try {
                        JSONArray arr = new JSONArray(response);

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);

                            MapDelivery d = new MapDelivery(
                                    o.getString("id"),
                                    o.getString("pickup_address"),
                                    o.getDouble("pickup_lat"),
                                    o.getDouble("pickup_lng"),
                                    o.getString("dropoff_address"),
                                    o.getDouble("dropoff_lat"),
                                    o.getDouble("dropoff_lng"),
                                    o.getString("price")
                            );

                            // Add pickup marker
                            // Add PICKUP marker (vert)
                            Marker pickupMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(d.pickupLat, d.pickupLng))
                                    .title("Pickup : " + d.pickup)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            );

// Add DROPOFF marker (rouge)
//                            Marker dropMarker = mMap.addMarker(new MarkerOptions()
//                                    .position(new LatLng(d.dropLat, d.dropLng))
//                                    .title("Destination : " + d.dropoff)
//                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//                            );

// Associer les 2 markers à la livraison
                            markerDeliveries.put(pickupMarker, d);
                            //markerDeliveries.put(dropMarker, d);

                        }

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(14.695, -17.444), 12));

                    } catch (Exception e) {
                        Log.e("ERR", e.getMessage());
                    }
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(req);
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
                    infoView.setVisibility(View.GONE);
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> d = new HashMap<>();
                d.put("delivery_id", selectedDelivery.id);
                d.put("driver_id", "1"); // replace with real value from SharedPreferences
                return d;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }
}
