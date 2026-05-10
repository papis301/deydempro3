package com.pisco.deydempro3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneOtpActivity extends AppCompatActivity {

    EditText edtPhone;
    Button btnNext;

    FirebaseAuth auth;

    String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_otp);

        auth = FirebaseAuth.getInstance();

        edtPhone = findViewById(R.id.edtPhone);
        btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {

            String phone = edtPhone.getText().toString().trim();

            if (phone.isEmpty()) {

                Toast.makeText(this,
                        "Entrer numéro",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 Sénégal
            if (!phone.startsWith("+221")) {
                phone = "+221" + phone;
            }

            sendOtp(phone);
        });
    }

    private void sendOtp(String phone) {

        Toast.makeText(this,
                "Envoi OTP...",
                Toast.LENGTH_SHORT).show();

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(
                        @NonNull PhoneAuthCredential credential) {

                    // 🔥 Auto verification possible
                    Log.d("OTP", "Auto verification");

                }

                @Override
                public void onVerificationFailed(
                        @NonNull FirebaseException e) {

                    Log.e("OTP_ERROR", e.getMessage());

                    Toast.makeText(
                            PhoneOtpActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }

                @Override
                public void onCodeSent(
                        @NonNull String s,
                        @NonNull PhoneAuthProvider.ForceResendingToken token) {

                    super.onCodeSent(s, token);

                    verificationId = s;

                    Toast.makeText(
                            PhoneOtpActivity.this,
                            "Code envoyé",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent i = new Intent(
                            PhoneOtpActivity.this,
                            VerifyOtpActivity.class
                    );

                    i.putExtra("verificationId", verificationId);

                    startActivity(i);
                }
            };
}