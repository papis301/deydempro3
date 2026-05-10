package deydemv3;

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
import com.pisco.deydempro3.SelectRoleActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivityc extends AppCompatActivity {

    private EditText etPhone, etPassword;
    private Button btnLogin, btnregister;

    // 🔥 URL API
    private static final String URL_LOGIN =
            "https://pisco.alwaysdata.net/login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginc);

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        btnregister = findViewById(R.id.btnregister);

        btnLogin.setOnClickListener(v -> loginUser());

        btnregister.setOnClickListener(v -> openRegister());
    }

    // 🔥 OUVRIR REGISTER
    private void openRegister() {

        Intent intent =
                new Intent(
                        LoginActivityc.this,
                        RegisterActivity.class
                );

        startActivity(intent);
    }

    // 🔥 LOGIN USER
    private void loginUser() {

        String phone =
                etPhone.getText().toString().trim();

        String password =
                etPassword.getText().toString().trim();

        //
        // 🔥 VALIDATION
        //
        if (phone.isEmpty() || password.isEmpty()) {

            Toast.makeText(this,
                    "Veuillez remplir tous les champs",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        //
        // 🔥 VALIDATION NUMÉRO
        //
        if (phone.length() < 9) {

            Toast.makeText(this,
                    "Numéro invalide",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        //
        // 🔥 FORMAT SÉNÉGAL
        //
//        if (!phone.startsWith("+221")) {
//            phone = "+221" + phone;
//        }

        //
        // 🔥 LOADING
        //
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Connexion...");
        pd.setCancelable(false);
        pd.show();

        String finalPhone = phone;

        //
        // 🔥 REQUÊTE VOLLEY
        //
        StringRequest req = new StringRequest(

                Request.Method.POST,
                URL_LOGIN,

                response -> {

                    pd.dismiss();

                    Log.d("LOGIN_RESPONSE", response);

                    try {

                        JSONObject json =
                                new JSONObject(response);

                        boolean success =
                                json.getBoolean("success");

                        //
                        // 🔥 LOGIN OK
                        //
                        if (success) {

                            JSONObject userObj =
                                    json.getJSONObject("user");

                            //
                            // 🔥 INFOS USER
                            //
                            String userId =
                                    userObj.getString("id");

                            String role =
                                    userObj.getString("role");

                            String mode =
                                    userObj.getString("mode");

                            String phonee =
                                    userObj.getString("phone");

                            String nomProfil =
                                    userObj.optString("nom_profil", "");

                            String profileImage =
                                    userObj.optString("profile_image", "");

                            String typeVehicule =
                                    userObj.optString("type_vehicule", "");

                            //
                            // 🔥 SAUVEGARDE SESSION
                            //
                            SharedPreferences sp =
                                    getSharedPreferences(
                                            "DeydemUser",
                                            MODE_PRIVATE
                                    );

                            SharedPreferences.Editor editor =
                                    sp.edit();

                            editor.putString("user_id", userId);
                            editor.putString("role", role);
                            editor.putString("mode", mode);
                            editor.putString("phone", phonee);
                            editor.putString("nom_profil", nomProfil);
                            editor.putString("profile_image", profileImage);
                            editor.putString("type_vehicule", typeVehicule);

                            editor.putBoolean("is_logged", true);

                            editor.apply();

                            //
                            // 🔥 SUCCESS
                            //
                            Toast.makeText(this,
                                    "Connexion réussie",
                                    Toast.LENGTH_SHORT).show();

                            //
                            // 🔥 REDIRECTION
                            //
                            Intent intent =
                                    new Intent(
                                            LoginActivityc.this,
                                            SelectRoleActivity.class
                                    );

                            startActivity(intent);

                            finish();

                        } else {

                            //
                            // 🔥 MESSAGE ERREUR
                            //
                            String msg =
                                    json.getString("message");

                            Toast.makeText(this,
                                    msg,
                                    Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {

                        e.printStackTrace();

                        Toast.makeText(this,
                                "Erreur lecture serveur",
                                Toast.LENGTH_SHORT).show();
                    }
                },

                error -> {

                    pd.dismiss();

                    Log.e("LOGIN_ERROR",
                            error.toString());

                    Toast.makeText(this,
                            "Erreur réseau",
                            Toast.LENGTH_LONG).show();
                }

        ) {

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params =
                        new HashMap<>();

                params.put("phone", finalPhone);
                params.put("password", password);

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