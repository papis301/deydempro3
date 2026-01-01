package com.pisco.deydempro3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CguActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cgu);

        CheckBox cbAccept = findViewById(R.id.cbAccept);
        Button btnAccept = findViewById(R.id.btnAccept);

        cbAccept.setOnCheckedChangeListener((buttonView, isChecked) ->
                btnAccept.setEnabled(isChecked)
        );

        btnAccept.setOnClickListener(v -> {
            SharedPreferences sp = getSharedPreferences("DeydemPro", MODE_PRIVATE);
            sp.edit().putBoolean("cgu_accepted", true).apply();

            startActivity(new Intent(this, StartActivity.class));
            finish();
        });
    }
}