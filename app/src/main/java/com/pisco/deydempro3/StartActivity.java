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

public class StartActivity extends AppCompatActivity {

    private static final String TAG = "StartActivity";

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
        Log.d(TAG, "checkAll: Vérification Internet, GPS, Connexion, CGU");

        // 🌐 INTERNET
        if (!isInternetAvailable()) {
            Log.d(TAG, "checkAll: Aucune connexion Internet");
            txtStatus.setText("❌ Aucune connexion Internet");
            btnAction.setText("Ouvrir paramètres");
            btnAction.setOnClickListener(v -> {
                Log.d(TAG, "checkAll: ouverture des paramètres WiFi");
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            });
            return;
        }
        Log.d(TAG, "checkAll: Internet OK");

        // 📍 GPS
        if (!isGpsEnabled()) {
            Log.d(TAG, "checkAll: GPS désactivé");
            txtStatus.setText("📍 Activez le GPS");
            btnAction.setText("Activer GPS");
            btnAction.setOnClickListener(v -> {
                Log.d(TAG, "checkAll: ouverture des paramètres GPS");
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            });
            return;
        }
        Log.d(TAG, "checkAll: GPS OK");

        // 🔐 CONNEXION CHAUFFEUR
        SharedPreferences userSp = getSharedPreferences("user", MODE_PRIVATE);
        int driverId = userSp.getInt("driver_id", 0);

        if (driverId == 0) {
            Log.d(TAG, "checkAll: Chauffeur non connecté");
            txtStatus.setText("🔐 Connexion requise");
            btnAction.setText("Se connecter");
            btnAction.setOnClickListener(v -> {
                Log.d(TAG, "checkAll: ouverture LoginActivity");
                startActivity(new Intent(this, LoginActivity.class));
            });
            return;
        }
        Log.d(TAG, "checkAll: Chauffeur connecté avec ID=" + driverId);

        // 📄 CGU
        if (!isCguAccepted()) {
            Log.d(TAG, "checkAll: CGU non acceptées");
            txtStatus.setText("📄 Acceptation des CGU requise");
            btnAction.setText("Lire les CGU");
            btnAction.setOnClickListener(v -> {
                Log.d(TAG, "checkAll: ouverture CguActivity");
                startActivity(new Intent(this, CguActivity.class));
            });
            return;
        }
        Log.d(TAG, "checkAll: CGU acceptées");

        // 🚀 Vérification du statut chauffeur avant les courses
        Log.d(TAG, "checkAll: Vérification du statut chauffeur...");
        txtStatus.setText("⏳ Vérification du statut chauffeur...");
        btnAction.setEnabled(false);
        checkDriverStatus(driverId);
    }



    private void checkDriverStatus(int driverId) {
        String url = CHECK_DRIVER_URL + "?driver_id=" + driverId;
        Log.d(TAG, "checkDriverStatus: URL=" + url);

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d(TAG, "checkDriverStatus: réponse = " + response);

                    try {
                        JSONObject obj = new JSONObject(response);

                        if (!obj.getBoolean("success")) {
                            Log.d(TAG, "checkDriverStatus: success=false");
                            txtStatus.setText("⚠ Erreur de récupération du compte");
                            btnAction.setText("Réessayer");
                            btnAction.setEnabled(true);
                            btnAction.setOnClickListener(v -> checkAll());
                            return;
                        }

                        JSONObject driver = obj.getJSONObject("driver");

                        String status = driver.getString("status");
                        int bloque = driver.getInt("bloque_par_admin");
                        String docsStatus = driver.getString("docs_status");

                        Log.d(TAG, "checkDriverStatus:");
                        Log.d(TAG, "status=" + status);
                        Log.d(TAG, "bloque_par_admin=" + bloque);
                        Log.d(TAG, "docs_status=" + docsStatus);

                        // ⛔ Compte bloqué ou inactif
                        if (!"active".equals(status) || bloque == 1) {
                            Log.d(TAG, "Compte bloqué ou inactif");

                            txtStatus.setText("⛔ Votre compte est inactif ou bloqué");
                            btnAction.setText("Contacter support");
                            btnAction.setEnabled(true);

                            btnAction.setOnClickListener(v -> {
                                String phoneNumber = "221767741008"; // numéro support sans +
                                String message = "Mon compte est inactif";

                                try {
                                    message = java.net.URLEncoder.encode(message, "UTF-8");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // URL universelle pour WhatsApp
                                String urlw = "https://wa.me/" + phoneNumber + "?text=" + message;

                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(android.net.Uri.parse(urlw));

                                // Ouvre WhatsApp ou le navigateur si WhatsApp installé
                                startActivity(i);
                            });
                            return; // ⛔ STOP TOTAL
                        }

                        // 📄 Documents non approuvés
                        if (!"approved".equals(docsStatus)) {
                            Log.d(TAG, "Documents non approuvés → redirection");

                            txtStatus.setText("📄 Documents requis");
                            btnAction.setText("Compléter les documents");
                            btnAction.setEnabled(true);

                            btnAction.setOnClickListener(v -> {
                                Intent i = new Intent(this, DriverDocumentsActivity.class);
                                i.putExtra("docs_status", docsStatus);
                                startActivity(i);
                                finish();
                            });
                            return; // ⛔ STOP ICI
                        }

                        // ✅ TOUT EST OK → vérifier les courses
                        Log.d(TAG, "Chauffeur OK + docs approuvés → vérification courses");
                        txtStatus.setText("⏳ Vérification des courses...");
                        btnAction.setEnabled(false);
                        checkActiveDelivery(driverId);

                    } catch (Exception e) {
                        Log.e(TAG, "checkDriverStatus: Erreur JSON", e);
                        txtStatus.setText("⚠ Erreur interne");
                        btnAction.setText("Réessayer");
                        btnAction.setEnabled(true);
                        btnAction.setOnClickListener(v -> checkAll());
                    }
                },
                error -> {
                    Log.e(TAG, "checkDriverStatus: Erreur réseau", error);
                    txtStatus.setText("⚠ Erreur réseau");
                    btnAction.setText("Réessayer");
                    btnAction.setEnabled(true);
                    btnAction.setOnClickListener(v -> checkAll());
                }
        );

        Volley.newRequestQueue(this).add(req);




    }


    // ======================================
    // 🚚 Vérifier course active
    // ======================================
    private void checkActiveDelivery(int driverId) {
        String url = CHECK_DELIVERY_URL + "?driver_id=" + driverId;
        Log.d(TAG, "checkActiveDelivery: URL=" + url);

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d(TAG, "checkActiveDelivery: réponse reçue: " + response);
                    try {
                        JSONObject obj = new JSONObject(response);

                        if (obj.getBoolean("success") && obj.getBoolean("has_delivery")) {
                            JSONObject d = obj.getJSONObject("delivery");
                            Log.d(TAG, "checkActiveDelivery: livraison trouvée: " + d.toString());

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
                            Log.d(TAG, "checkActiveDelivery: aucune course active, ouverture MapDeliveriesActivity");
                            startActivity(new Intent(this, MapDeliveriesActivity.class));
                        }

                        finish();

                    } catch (Exception e) {
                        Log.e(TAG, "checkActiveDelivery: Erreur JSON", e);
                        startActivity(new Intent(this, MapDeliveriesActivity.class));
                        finish();
                    }
                },
                error -> {
                    Log.e(TAG, "checkActiveDelivery: Erreur réseau", error);
                    startActivity(new Intent(this, MapDeliveriesActivity.class));
                    finish();
                }
        );

        Volley.newRequestQueue(this).add(req);




    }

    // ===============================
    // 🌐 INTERNET
    // ===============================
    private boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        boolean connected = net != null && net.isConnected();
        Log.d(TAG, "isInternetAvailable: " + connected);
        return connected;
    }

    // ===============================
    // 📍 GPS
    // ===============================
    private boolean isGpsEnabled() {
        LocationManager lm =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d(TAG, "isGpsEnabled: " + enabled);
        return enabled;
    }

    // ===============================
    // 📄 CGU
    // ===============================
    private boolean isCguAccepted() {
        SharedPreferences sp = getSharedPreferences("DeydemPro", MODE_PRIVATE);
        boolean accepted = sp.getBoolean("cgu_accepted", false);
        Log.d(TAG, "isCguAccepted: " + accepted);
        return accepted;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: re-vérification de tous les checks");
        checkAll(); // re-check après retour paramètres ou CGU
    }
}
