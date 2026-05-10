package deydemv3;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pisco.deydempro3.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etPhone, etPassword, etReferral;
    private Button btnRegister, seconnecter;

    // 🔥 URL API
    private static final String URL_REGISTER =
            "https://pisco.alwaysdata.net/register.php";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registerc);

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etReferral = findViewById(R.id.etReferral);

        btnRegister = findViewById(R.id.btnRegister);
        seconnecter = findViewById(R.id.seconnecter);

        btnRegister.setOnClickListener(v -> registerUser());

        seconnecter.setOnClickListener(v -> openLogin());
    }

    // 🔥 OUVRIR LOGIN
    private void openLogin() {

        Intent intent =
                new Intent(RegisterActivity.this,
                        LoginActivityc.class);

        startActivity(intent);
    }

    // 🔥 INSCRIPTION
    private void registerUser() {

        String phone =
                etPhone.getText().toString().trim();

        String password =
                etPassword.getText().toString().trim();

        String referralCode =
                etReferral.getText().toString().trim();

        // 🔥 ROLE PAR DÉFAUT
        String role = "client";

        //
        // 🔥 VALIDATIONS
        //
        if (phone.isEmpty() || password.isEmpty()) {

            Toast.makeText(this,
                    "Veuillez remplir tous les champs",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        // 🔥 VALIDATION NUMÉRO
        if (phone.length() < 9) {

            Toast.makeText(this,
                    "Numéro invalide",
                    Toast.LENGTH_SHORT).show();

            return;
        }

//        // 🔥 FORMAT SÉNÉGAL
//        if (!phone.startsWith("+221")) {
//            phone = "+221" + phone;
//        }

        // 🔥 VALIDATION PASSWORD
        if (password.length() < 4) {

            Toast.makeText(this,
                    "Mot de passe trop court",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        //
        // 🔥 LOADING
        //
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Inscription...");
        pd.setCancelable(false);
        pd.show();

        //
        // 🔥 REQUÊTE VOLLEY
        //
        String finalPhone = phone;

        StringRequest req = new StringRequest(
                Request.Method.POST,
                URL_REGISTER,

                response -> {

                    pd.dismiss();

                    Log.d("REGISTER_RESPONSE", response);

                    try {

                        JSONObject json =
                                new JSONObject(response);

                        boolean success =
                                json.getBoolean("success");

                        String message =
                                json.getString("message");

                        Toast.makeText(this,
                                message,
                                Toast.LENGTH_LONG).show();

                        if (success) {

                            //
                            // 🔥 RÉCUPÉRATION DONNÉES
                            //
                            int userId =
                                    json.getInt("user_id");

                            String userRole =
                                    json.getString("role");

                            String mode =
                                    json.getString("mode");

                            //
                            // 🔥 SAUVEGARDE SESSION
                            //
                            SharedPreferences prefs =
                                    getSharedPreferences(
                                            "DeydemUser",
                                            MODE_PRIVATE
                                    );

                            prefs.edit()
                                    .putString("user_id", String.valueOf(userId))
                                    .putString("phone", finalPhone)
                                    .putString("role", userRole)
                                    .putString("mode", mode)
                                    .apply();

                            //
                            // 🔥 OUVRIR APP
                            //
                            Intent intent =
                                    new Intent(
                                            RegisterActivity.this,
                                            StartActivity.class
                                    );

                            startActivity(intent);

                            finish();
                        }

                    } catch (Exception e) {

                        e.printStackTrace();

                        Toast.makeText(this,
                                "Erreur lecture serveur",
                                Toast.LENGTH_LONG).show();
                    }

                },

                error -> {

                    pd.dismiss();

                    Toast.makeText(this,
                            "Erreur réseau",
                            Toast.LENGTH_LONG).show();

                    Log.e("VOLLEY_ERROR",
                            error.toString());
                }

        ) {

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params =
                        new HashMap<>();

                params.put("phone", finalPhone);
                params.put("password", password);
                params.put("role", role);

                // 🔥 CODE PARRAIN
                params.put("referral_code", referralCode);

                return params;
            }

            @Override
            public Map<String, String> getHeaders() {

                Map<String, String> headers =
                        new HashMap<>();

                headers.put("Accept", "application/json");

                return headers;
            }
        };

        //
        // 🔥 ENVOI REQUÊTE
        //
        RequestQueue queue =
                Volley.newRequestQueue(this);

        queue.add(req);
    }
}