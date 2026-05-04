package deydemv3;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pisco.deydempro3.R;

import org.json.JSONObject;

public class WaitingDriverActivity extends AppCompatActivity {

    int rideId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_driver);

        rideId = getIntent().getIntExtra("ride_id", 0);

        //showSearching();

        startDriverSearch();
    }

    // 🔵 AFFICHER RECHERCHE
//    private void showSearching() {
//        findViewById(R.id.layoutSearching).setVisibility(View.VISIBLE);
//        findViewById(R.id.layoutDriver).setVisibility(View.GONE);
//    }
//
//    // 🟢 AFFICHER CHAUFFEUR
//    private void showDriver(String name, String car) {
//
//        findViewById(R.id.layoutSearching).setVisibility(View.GONE);
//        findViewById(R.id.layoutDriver).setVisibility(View.VISIBLE);
//
//        ((TextView)findViewById(R.id.tvDriverName)).setText(name);
//        ((TextView)findViewById(R.id.tvCar)).setText(car);
//    }

    // 🔁 BOUCLE RECHERCHE
    private void startDriverSearch() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                checkDriverStatus();

                startDriverSearch(); // boucle

            }
        }, 5000); // toutes les 5 secondes
    }

    // 📡 API CHECK
    private void checkDriverStatus() {

        String url = "http://TON_IP/check_driver.php?ride_id=" + rideId;

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,

                response -> {
                    try {
                        JSONObject json = new JSONObject(response);

                        if (json.getBoolean("found")) {

                            String name = json.getString("driver_name");
                            String car = json.getString("vehicle");

                            //showDriver(name, car);

                            Toast.makeText(this, "Chauffeur trouvé 🚗", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },

                error -> Log.e("API", error.toString())
        );

        Volley.newRequestQueue(this).add(request);
    }
}