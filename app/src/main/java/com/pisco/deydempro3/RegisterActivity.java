package com.pisco.deydempro3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText etPhone, etPassword, etConfirm;
    Button btnRegister, btnGoLogin;

    // 🔥 Ton API LOCAL
    String URL = "http://192.168.1.7/deydemlivraisonphpmysql/register_driver.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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

        if (phone.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Champs manquants", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }

        // ---------- VOLLEY POST REQUEST ----------
        StringRequest req = new StringRequest(
                Request.Method.POST,
                URL,
                response -> {
                    Log.d("reponse", response);
                    Toast.makeText(this, response, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                },
                error -> {
                    Log.e("Erreur", error.toString());
                    Toast.makeText(this, "Erreur réseau : " + error.toString(), Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> data = new HashMap<>();
                data.put("phone", phone);
                data.put("password", pass);
                data.put("type", "driver");
                return data;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(req);
    }
}
