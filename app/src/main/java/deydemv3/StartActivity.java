package deydemv3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pisco.deydempro3.R;
import com.pisco.deydempro3.SelectRoleActivity;

import org.json.JSONObject;

public class StartActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnRetry;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // 🔗 Bind views
        tvStatus = findViewById(R.id.tvStatus);
        btnRetry = findViewById(R.id.btnRetry);

        btnRetry.setOnClickListener(v -> checkFlow());

        // ⏳ Splash de 2 secondes
        new Handler().postDelayed(this::startFlow, 2000);
    }

    /**
     * Point d’entrée logique de l’app
     * 1️⃣ Vérifie CGU
     * 2️⃣ Puis Internet + session
     */
    private void startFlow() {
        if (!isCguAccepted()) {
            startActivity(new Intent(this, CguActivity.class));
            finish();
            return;
        }

        checkFlow();
    }

    /**
     * Vérifie Internet + session utilisateur
     */
    private void checkFlow() {

        if (!isConnected()) {
            tvStatus.setText("Aucune connexion Internet !");
            btnRetry.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Veuillez activer Internet", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        String userId = sp.getString("user_id", "0");
        //String tel = sp.getString("phone", "");
        Log.e("START_DEBUG", "user_id = " + userId);


        if (userId.equals("0")) {
            startActivity(new Intent(this, LoginActivityc.class));
            finish();
        } else {
            checkUserFromServer(userId); // ⏳ async → finish plus tard
        }
    }


    /**
     * Vérifie si les CGU sont acceptées
     */
    private boolean isCguAccepted() {
        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        return sp.getBoolean("cgu_accepted", false);
    }

    /**
     * Vérifie la connexion Internet
     */
    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    private void checkUserFromServer(String userId) {

        String url = "https://pisco.alwaysdata.net/get_user_by_id.php?user_id=" + userId;

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        Log.e("START_API", response);

                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);

                        if (success) {
                            startActivity(new Intent(this, SelectRoleActivity.class));
                        } else {
                            clearSession();
                            startActivity(new Intent(this, LoginActivityc.class));
                        }

                        finish();

                    } catch (Exception e) {
                        clearSession();
                        startActivity(new Intent(this, LoginActivityc.class));
                        finish();
                    }
                },
                error -> {
                    Toast.makeText(this,
                            "Erreur serveur, veuillez vous reconnecter",
                            Toast.LENGTH_LONG).show();
                    clearSession();
                    startActivity(new Intent(this, LoginActivityc.class));
                    finish();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }


    private void clearSession() {
        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);

        boolean cguAccepted = sp.getBoolean("cgu_accepted", false);

        sp.edit()
                .clear()
                .putBoolean("cgu_accepted", cguAccepted)
                .apply();
    }



}
