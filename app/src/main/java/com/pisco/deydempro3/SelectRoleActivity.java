package com.pisco.deydempro3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import deydemv3.RideSelectActivity;
import deydemv3.StartActivity;


public class SelectRoleActivity extends AppCompatActivity {

    private LinearLayout btnClient, btnDriver;
    private ModeManager modeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        modeManager = new ModeManager(this);

        btnClient = findViewById(R.id.btnClient);
        btnDriver = findViewById(R.id.btnDriver);

        btnClient.setOnClickListener(v -> {
            modeManager.setMode(ModeManager.MODE_CLIENT);
            startActivity(new Intent(this, RideSelectActivity.class));
            //startActivity(new Intent(this, StartActivity.class));

            //finish();
        });

        btnDriver.setOnClickListener(v -> {
            modeManager.setMode(ModeManager.MODE_DRIVER);
            startActivity(new Intent(this, StartActivitypro.class));
            //finish();
        });
    }
}