package com.pisco.deydempro3;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import deydemv3.RideSelectActivity;

public class SelectRoleActivity extends AppCompatActivity {

    private LinearLayout btnClient, btnDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);
        checkGPS();

        btnClient = findViewById(R.id.btnClient);
        btnDriver = findViewById(R.id.btnDriver);

        if (!isConnected()) {

            new AlertDialog.Builder(this)
                    .setTitle("Pas de connexion")
                    .setMessage("Veuillez activer Internet")
                    .setPositiveButton("Réessayer",
                            (dialog, which) -> recreate())
                    .setCancelable(false)
                    .show();

            return;
        }

        // 🔵 CLIENT
        btnClient.setOnClickListener(v -> {

            if(!isGpsEnabled()){

                checkGPS();
                return;
            }

            startActivity(
                    new Intent(
                            this,
                            RideSelectActivity.class
                    )
            );
        });

        // 🚗 DRIVER
        btnDriver.setOnClickListener(v -> {

            if(!isGpsEnabled()){

                checkGPS();
                return;
            }

            startActivity(
                    new Intent(
                            this,
                            DriverHomeActivity.class
                    )
            );
        });
    }

    private boolean isGpsEnabled(){

        LocationManager lm =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE
                        );

        if(lm == null){
            return false;
        }

        return lm.isProviderEnabled(
                LocationManager.GPS_PROVIDER
        );
    }
//    private void switchModeAPI(String newMode) {
//
//        String url = "https://pisco.alwaysdata.net/switch_mode.php";
//
//        StringRequest request = new StringRequest(Request.Method.POST, url,
//
//                response -> {
//                    try {
//                        JSONObject json = new JSONObject(response);
//
//                        if(json.getBoolean("success")){
//
//                            // 🔥 sauvegarder local
//                            ModeManager.setMode(this, newMode);
//
//                            Toast.makeText(this,
//                                    "Mode : " + newMode,
//                                    Toast.LENGTH_SHORT).show();
//
//                            // 🔥 redirection unique
////                            if(newMode.equals("driver")){
////                                startActivity(new Intent(this, StartActivitypro.class));
////                            } else {
////                                startActivity(new Intent(this, RideSelectActivity.class));
////                            }
//
//                            finish();
//                        }
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                },
//
//                error -> Toast.makeText(this, "Erreur réseau ❌", Toast.LENGTH_SHORT).show()
//        ) {
//            @Override
//            protected Map<String, String> getParams() {
//
//                Map<String, String> params = new HashMap<>();
//                params.put("user_id", String.valueOf(getUserId())); // 🔥 à implémenter
//                params.put("mode", newMode);
//
//                return params;
//            }
//        };
//
//        Volley.newRequestQueue(this).add(request);
//    }

    private boolean isConnected() {

        ConnectivityManager cm =
                (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        Network network = cm.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                cm.getNetworkCapabilities(network);

        return capabilities != null &&
                (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                                ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                );
    }

    private void checkGPS(){

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean enabled =
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        ||
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if(!enabled){

            new AlertDialog.Builder(this)
                    .setTitle("GPS désactivé")
                    .setMessage("Activez le GPS pour continuer")
                    .setCancelable(false)
                    .setPositiveButton("Activer", (d,w)->{
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .show();
        }
    }

    // 🔥 récupère ton user id (à adapter)
    private int getUserId() {
        return getSharedPreferences("DeydemUser", MODE_PRIVATE)
                .getInt("user_id", 0);
    }
}