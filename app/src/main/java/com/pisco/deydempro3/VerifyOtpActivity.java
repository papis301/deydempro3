// 🔥 VerifyOtpActivity.java
// Vérification code OTP Twilio

package com.pisco.deydempro3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class VerifyOtpActivity extends AppCompatActivity {

    EditText edtOtp;
    Button btnVerify;

    String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        phone = getIntent().getStringExtra("phone");

        edtOtp = findViewById(R.id.edtOtp);
        btnVerify = findViewById(R.id.btnVerify);

        btnVerify.setOnClickListener(v -> {

            String otp = edtOtp.getText().toString().trim();

            verifyOtp(phone, otp);
        });
    }

    private void verifyOtp(String phone, String otp){

        String url = Constants.BASE_URL + "verify_otp.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,

                response -> {

                    if(response.contains("success")){

                        Toast.makeText(this,
                                "Connexion réussie",
                                Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(this,
                                SelectRoleActivity.class));

                        finish();

                    } else {

                        Toast.makeText(this,
                                "Code incorrect",
                                Toast.LENGTH_SHORT).show();
                    }

                },

                error -> Toast.makeText(this,
                        "Erreur réseau",
                        Toast.LENGTH_SHORT).show()

        ){

            @Override
            protected Map<String, String> getParams(){

                Map<String, String> params = new HashMap<>();

                params.put("phone", phone);
                params.put("otp", otp);

                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}