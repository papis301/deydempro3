package deydemv3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pisco.deydempro3.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
            SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
            sp.edit().putBoolean("cgu_accepted", true).apply();
            updateDocsStatus();
            startActivity(new Intent(this, StartActivity.class));
            finish();
        });
    }

    private void updateDocsStatus() {

        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        String userId = sp.getString("user_id", "0");

        if (userId == null || userId.equals("0")) return;

        String url = "https://pisco.alwaysdata.net/update_docs_statusclient.php";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (!json.getBoolean("success")) {
                            Log.e("CGU", "Erreur MAJ docs_status");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("CGU", "Erreur réseau")
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userId);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

}
