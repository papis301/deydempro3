package com.pisco.deydempro3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class VerifyOtpActivity extends AppCompatActivity {

    EditText edtOtp;
    Button btnVerify;

    FirebaseAuth auth;

    String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        auth = FirebaseAuth.getInstance();

        edtOtp = findViewById(R.id.edtOtp);
        btnVerify = findViewById(R.id.btnVerify);

        verificationId =
                getIntent().getStringExtra("verificationId");

        btnVerify.setOnClickListener(v -> {

            String otp =
                    edtOtp.getText().toString().trim();

            verifyCode(otp);
        });
    }

    private void verifyCode(String code){

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(
                        verificationId,
                        code
                );

        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {

                    if(task.isSuccessful()){

                        Toast.makeText(this,
                                "Connexion réussie",
                                Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(
                                this,
                                SelectRoleActivity.class
                        ));

                        finish();

                    } else {

                        Toast.makeText(this,
                                "Code incorrect",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}