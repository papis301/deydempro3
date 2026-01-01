package com.pisco.deydempro3;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

public class StartActivity extends AppCompatActivity {

    TextView txtStatus;
    Button btnAction;

    String CHECK_URL = Constants.BASE_URL + "check_active_delivery.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        txtStatus = findViewById(R.id.txtStatus);
        btnAction = findViewById(R.id.btnAction);

        checkAll();
    }

    private void checkAll() {

        // 🌐 INTERNET
        if (!isInternetAvailable()) {
            txtStatus.setText("❌ Aucune connexion Internet");
            btnAction.setText("Ouvrir paramètres");
            btnAction.setOnClickListener(v ->
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
            return;
        }

        // 📍 GPS
        if (!isGpsEnabled()) {
            txtStatus.setText("📍 Activez le GPS");
            btnAction.setText("Activer GPS");
            btnAction.setOnClickListener(v ->
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
            return;
        }

        // 🔐 CONNEXION CHAUFFEUR
        SharedPreferences userSp = getSharedPreferences("user", MODE_PRIVATE);
        int driverId = userSp.getInt("driver_id", 0);

        if (driverId == 0) {
            txtStatus.setText("🔐 Connexion requise");
            btnAction.setText("Se connecter");
            btnAction.setOnClickListener(v ->
                    startActivity(new Intent(this, LoginActivity.class)));
            return;
        }

        // 📄 CGU
        if (!isCguAccepted()) {
            txtStatus.setText("📄 Acceptation des CGU requise");
            btnAction.setText("Lire les CGU");
            btnAction.setOnClickListener(v ->
                    startActivity(new Intent(this, CguActivity.class)));
            return;
        }

        // 🚀 COURSE ACTIVE
        txtStatus.setText("⏳ Vérification des courses...");
        btnAction.setEnabled(false);
        checkActiveDelivery(driverId);
    }

    // ======================================
    // 🚚 Vérifier course active
    // ======================================
    private void checkActiveDelivery(int driverId) {

        String url = CHECK_URL + "?driver_id=" + driverId;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);

                        if (obj.getBoolean("success")
                                && obj.getBoolean("has_delivery")) {

                            JSONObject d = obj.getJSONObject("delivery");

                            Intent i = new Intent(this, DeliveryNavigationActivity.class);
                            i.putExtra("delivery_id", d.getString("id"));
                            i.putExtra("pickup_lat", d.getDouble("pickup_lat"));
                            i.putExtra("pickup_lng", d.getDouble("pickup_lng"));
                            i.putExtra("drop_lat", d.getDouble("dropoff_lat"));
                            i.putExtra("drop_lng", d.getDouble("dropoff_lng"));
                            i.putExtra("status", d.getString("status"));
                            i.putExtra("pickup_address", d.getString("pickup_address"));
                            i.putExtra("dropoff_address", d.getString("dropoff_address"));
                            i.putExtra("price", d.getString("price"));
                            i.putExtra("client_id", d.getString("client_id"));

                            startActivity(i);

                        } else {
                            startActivity(new Intent(this, MapDeliveriesActivity.class));
                        }

                        finish();

                    } catch (Exception e) {
                        startActivity(new Intent(this, MapDeliveriesActivity.class));
                        finish();
                    }
                },
                error -> {
                    startActivity(new Intent(this, MapDeliveriesActivity.class));
                    finish();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ===============================
    // 🌐 INTERNET
    // ===============================
    private boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    // ===============================
    // 📍 GPS
    // ===============================
    private boolean isGpsEnabled() {
        LocationManager lm =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // ===============================
    // 📄 CGU
    // ===============================
    private boolean isCguAccepted() {
        SharedPreferences sp = getSharedPreferences("DeydemPro", MODE_PRIVATE);
        return sp.getBoolean("cgu_accepted", false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAll(); // re-check après retour paramètres ou CGU
    }
}
