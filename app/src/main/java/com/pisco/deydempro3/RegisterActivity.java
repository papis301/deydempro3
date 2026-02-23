package com.pisco.deydempro3;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText etPhone, etPassword, etConfirm;
    Button btnRegister, btnGoLogin;
    Spinner spVehicleType;

    // 🔥 Ton API LOCAL
    String URL = "https://pisco.alwaysdata.net/register_driver.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

         spVehicleType = findViewById(R.id.spVehicleType);

        String[] vehicles = {"Choisir", "moto", "voiture"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                vehicles
        );

        spVehicleType.setAdapter(adapter);

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);

        btnRegister = findViewById(R.id.btnRegister);
        btnGoLogin = findViewById(R.id.btnGoLogin);

        btnRegister.setOnClickListener(v -> registerDriver());
        btnGoLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private void registerDriver() {

        String phone = etPhone.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();
        String vehicleType = spVehicleType.getSelectedItem().toString();

        // ✅ Vérification véhicule
        if(vehicleType.equals("Choisir")){
            Toast.makeText(this,"Choisissez un type de véhicule",Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Champs manquants", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Création du compte...");
        dialog.show();

        StringRequest req = new StringRequest(
                Request.Method.POST,
                URL,
                response -> {
                    dialog.dismiss();
                    Log.d("reponse", response);

                    try {
                        JSONObject json = new JSONObject(response);

                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        // ✅ Afficher message API
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                        if(success){
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur de lecture réponse", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    dialog.dismiss();
                    Log.e("Erreur", error.toString());
                    Toast.makeText(this, "Erreur réseau", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> data = new HashMap<>();
                data.put("phone", phone);
                data.put("password", pass);
                data.put("type", "driver");
                data.put("type_vehicule", vehicleType);

                return data;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }
}
