package deydemv3;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
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

    EditText etPhone, etPassword, etReferral;
    Button btnRegister, seconnecter;

    String URL = "https://pisco.alwaysdata.net/register.php"; // 🔥 mets ton lien API

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registerc);

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etReferral = findViewById(R.id.etReferral); // "client" ou "driver"
        btnRegister = findViewById(R.id.btnRegister);
        seconnecter = findViewById(R.id.seconnecter);

        btnRegister.setOnClickListener(v -> registerUser());
        seconnecter.setOnClickListener(v -> seconnecter());
    }

    private void seconnecter() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivityc.class);
        startActivity(intent);
    }

    private void registerUser() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = "client"; //etRole.getText().toString().trim();
        String referralCode = etReferral.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Inscription...");
        pd.show();

        StringRequest req = new StringRequest(Request.Method.POST, URL,
                response -> {

                    pd.dismiss();
                    Log.d("Response", response);

                    try {

                        JSONObject json = new JSONObject(response);

                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                        if(success){

                            // inscription confirmée
                            seconnecter();

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur lecture serveur", Toast.LENGTH_LONG).show();
                    }

                },
                error -> {
                    pd.dismiss();
                    Toast.makeText(this, "Erreur : " + error.toString(), Toast.LENGTH_LONG).show();
                    Log.d("Erreur", error.toString());
                }
        ) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("phone", phone);
                params.put("password", password);
                params.put("role", role); // chauffeur ou client
                params.put("referral_code", referralCode);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "Mozilla/5.0");
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };


        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(req);
    }
}
