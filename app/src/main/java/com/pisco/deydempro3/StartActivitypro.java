package com.pisco.deydempro3;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class StartActivitypro extends AppCompatActivity {

    private static final String TAG = "StartActivitypro";

    TextView txtStatus;
    Button btnAction;

    String CHECK_DELIVERY_URL = Constants.BASE_URL + "check_active_delivery.php";
    String CHECK_DRIVER_URL = Constants.BASE_URL + "get_driver.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        txtStatus = findViewById(R.id.txtStatus);
        btnAction = findViewById(R.id.btnAction);

        Log.d(TAG, "onCreate: démarrage de l'application");
        checkAll();
    }

    private void checkAll() {

        Log.d(TAG, "checkAll: Vérification CGU → Internet → GPS → Connexion");

        // 📄 CGU EN PREMIER
        if (!isCguAccepted()) {

            txtStatus.setText("📄 Acceptation des CGU requise");
            btnAction.setText("Lire les CGU");
            btnAction.setEnabled(true);

            btnAction.setOnClickListener(v ->
                    startActivity(new Intent(this, CguActivity.class)));

            return;
        }

        // 🌐 INTERNET
        if (!isInternetAvailable()) {

            txtStatus.setText("❌ Aucune connexion Internet");
            btnAction.setText("Ouvrir paramètres");
            btnAction.setEnabled(true);

            btnAction.setOnClickListener(v ->
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));

            return;
        }

        // 📍 GPS
        if (!isGpsEnabled()) {

            txtStatus.setText("📍 Activez le GPS");
            btnAction.setText("Activer GPS");
            btnAction.setEnabled(true);

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
            btnAction.setEnabled(true);

            btnAction.setOnClickListener(v ->
                    startActivity(new Intent(this, LoginActivity.class)));

            return;
        }

        // 🚀 Vérification statut chauffeur
        txtStatus.setText("⏳ Vérification du statut chauffeur...");
        btnAction.setEnabled(false);

        checkDriverStatus(driverId);
    }

    private void checkDriverStatus(int driverId) {

        String url = CHECK_DRIVER_URL + "?driver_id=" + driverId;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {

                    try {
                        JSONObject obj = new JSONObject(response);

                        if (!obj.getBoolean("success")) {
                            showRetry("⚠ Erreur de récupération du compte");
                            return;
                        }

                        JSONObject driver = obj.getJSONObject("driver");

                        String status = driver.getString("status");
                        int bloque = driver.getInt("bloque_par_admin");
                        String docsStatus = driver.getString("docs_status");
                        int partenaireId = driver.optInt("partner_id", 0);
                        // ⛔ Compte bloqué
                        if (!"active".equals(status) || bloque == 1) {

                            txtStatus.setText("⛔ Votre compte est bloqué");
                            btnAction.setText("Contacter support");
                            btnAction.setEnabled(true);

                            btnAction.setOnClickListener(v -> {

                                String phoneNumber = "221767741008";
                                String message = "Mon compte est bloqué "+driverId;

                                try {
                                    message = java.net.URLEncoder.encode(message, "UTF-8");
                                } catch (Exception ignored) {}

                                String urlw = "https://wa.me/" + phoneNumber + "?text=" + message;

                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(android.net.Uri.parse(urlw));
                                startActivity(i);
                            });

                            return;
                        }

                        // 📄 Documents non approuvés
                        if (!"approved".equals(docsStatus)) {

                            txtStatus.setText("📄 Documents requis");
                            btnAction.setText("Compléter les documents");
                            btnAction.setEnabled(true);

                            btnAction.setOnClickListener(v -> {
                                Intent i = new Intent(this, DriverDocumentsActivity.class);
                                i.putExtra("docs_status", docsStatus);
                                startActivity(i);
                                finish();
                            });

                            return;
                        }

                        // 🤝 Vérifier partenaire
                        if(partenaireId == 0){

                            txtStatus.setText("🤝 Contacter un partenaire");
                            btnAction.setText("Voir les partenaires");
                            btnAction.setEnabled(true);

                            btnAction.setOnClickListener(v -> {
                                startActivity(new Intent(this, SelectPartnerActivity.class));
                                finish();
                            });

                            return;
                        }

                        // ✅ Vérifier course active
                        txtStatus.setText("⏳ Vérification des courses...");
                        checkActiveDelivery(driverId);

                    } catch (Exception e) {
                        showRetry("⚠ Erreur interne");
                    }
                },
                error -> showRetry("⚠ Erreur réseau")
        );

        Volley.newRequestQueue(this).add(req);
    }

    // 🚚 Vérifier course active
    private void checkActiveDelivery(int driverId) {

        String url = CHECK_DELIVERY_URL + "?driver_id=" + driverId;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {

                    try {
                        JSONObject obj = new JSONObject(response);

                        if (obj.getBoolean("success") && obj.getBoolean("has_delivery")) {

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

        Volley.newRequestQueue(this).add(req);
    }

    // 🌐 INTERNET
    private boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    // 📍 GPS
    private boolean isGpsEnabled() {
        LocationManager lm =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // 📄 CGU
    private boolean isCguAccepted() {
        SharedPreferences sp = getSharedPreferences("DeydemPro", MODE_PRIVATE);
        return sp.getBoolean("cgu_accepted", false);
    }

    // 🔁 Retry helper
    private void showRetry(String message) {
        txtStatus.setText(message);
        btnAction.setText("Réessayer");
        btnAction.setEnabled(true);
        btnAction.setOnClickListener(v -> checkAll());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAll();
    }
}
