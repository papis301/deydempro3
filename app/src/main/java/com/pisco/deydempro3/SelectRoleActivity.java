package com.pisco.deydempro3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

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

        btnClient = findViewById(R.id.btnClient);
        btnDriver = findViewById(R.id.btnDriver);

        // 🔵 CLIENT
        btnClient.setOnClickListener(v -> {
            startActivity(new Intent(this, RideSelectActivity.class));
            //switchModeAPI("client");
        });

        // 🚗 DRIVER
        btnDriver.setOnClickListener(v -> {
            //startActivity(new Intent(this, StartActivitypro.class));
            startActivity(new Intent(this, DriverHomeActivity.class));

            //switchModeAPI("driver");
        });
    }

    private void switchModeAPI(String newMode) {

        String url = "https://pisco.alwaysdata.net/switch_mode.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,

                response -> {
                    try {
                        JSONObject json = new JSONObject(response);

                        if(json.getBoolean("success")){

                            // 🔥 sauvegarder local
                            ModeManager.setMode(this, newMode);

                            Toast.makeText(this,
                                    "Mode : " + newMode,
                                    Toast.LENGTH_SHORT).show();

                            // 🔥 redirection unique
                            if(newMode.equals("driver")){
                                startActivity(new Intent(this, StartActivitypro.class));
                            } else {
                                startActivity(new Intent(this, RideSelectActivity.class));
                            }

                            finish();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },

                error -> Toast.makeText(this, "Erreur réseau ❌", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(getUserId())); // 🔥 à implémenter
                params.put("mode", newMode);

                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    // 🔥 récupère ton user id (à adapter)
    private int getUserId() {
        return getSharedPreferences("USER", MODE_PRIVATE)
                .getInt("user_id", 0);
    }
}