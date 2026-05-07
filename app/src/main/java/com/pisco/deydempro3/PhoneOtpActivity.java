// 🔥 PhoneOtpActivity.java
// Écran numéro téléphone + envoi OTP Twilio

package com.pisco.deydempro3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class PhoneOtpActivity extends AppCompatActivity {

    EditText edtPhone;
    Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_otp);

        edtPhone = findViewById(R.id.edtPhone);
        btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {

            String phone = edtPhone.getText().toString().trim();

            if(phone.isEmpty()){
                Toast.makeText(this, "Entrer numéro", Toast.LENGTH_SHORT).show();
                return;
            }

            sendOtp(phone);
        });
    }

    private void sendOtp(String phone){

        String url = Constants.BASE_URL + "send_otp.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,

                response -> {

                    Intent i = new Intent(this, VerifyOtpActivity.class);
                    i.putExtra("phone", phone);
                    startActivity(i);

                },

                error -> Toast.makeText(this,
                        "Erreur OTP",
                        Toast.LENGTH_SHORT).show()

        ){

            @Override
            protected Map<String, String> getParams(){

                Map<String, String> params = new HashMap<>();
                params.put("phone", phone);

                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}