package com.pisco.deydempro3;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    EditText phoneEd, passEd;
    Button loginBtn, registerBtn;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        phoneEd = findViewById(R.id.etPhone);
        passEd  = findViewById(R.id.etPassword);
        loginBtn = findViewById(R.id.btnLogin);
        registerBtn = findViewById(R.id.btnRegister);

        loginBtn.setOnClickListener(v -> loginDriver());
        registerBtn.setOnClickListener(v -> RegisterDriver());
    }

    private void RegisterDriver() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        finish();
    }

    private void loginDriver() {
        String phone = phoneEd.getText().toString().trim();
        String pass  = passEd.getText().toString().trim();

        if (phone.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Champs vides", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = Constants.BASE_URL + "login_driver.php";

        JSONObject data = new JSONObject();
        try {
            data.put("phone", phone);
            data.put("password", pass);
        } catch (JSONException e) { e.printStackTrace(); }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                data,
                response -> {
                    Log.e("reponse", String.valueOf(response));
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject driver = response.getJSONObject("driver");
                            int driverId = driver.getInt("id");

                            String phonerecup = driver.getString("phone");
                            int solde = driver.getInt("solde");
                            String status = driver.getString("status");

                            Toast.makeText(this, " id "+driverId, Toast.LENGTH_SHORT).show();
                            SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
                            prefs.edit().putInt(Constants.KEY_DRIVER_ID, driverId)
                                    .putString("solde", String.valueOf(solde))
                                    .apply();



                            startActivity(new Intent(LoginActivity.this, MapDeliveriesActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(req);




    }
}
