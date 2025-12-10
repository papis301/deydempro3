package com.pisco.deydempro3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    EditText etPhone, etPassword, etConfirm;
    Button btnRegister, btnGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etPhone   = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirm  = findViewById(R.id.etConfirm);

        btnRegister = findViewById(R.id.btnRegister);
        btnGoLogin = findViewById(R.id.btnGoLogin);

        btnRegister.setOnClickListener(v -> registerDriver());
        btnGoLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private void registerDriver() {

        String phone = etPhone.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (phone.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Champs manquants", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = Constants.BASE_URL + "register_driver.php";

        JSONObject data = new JSONObject();
        try {
            data.put("phone", phone);
            data.put("password", pass);
            data.put("type", "driver"); // IMPORTANT pour ta base usersdeydem
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                data,
                response -> {
                    try {
                        if (response.getBoolean("success")) {

                            int id = response.getInt("driver_id");

                            SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
                            prefs.edit().putInt(Constants.KEY_DRIVER_ID, id).apply();

                            Toast.makeText(this, "Compte créé", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();

                        } else {
                            Toast.makeText(this,
                                    response.getString("message"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Erreur réseau !", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }
}
