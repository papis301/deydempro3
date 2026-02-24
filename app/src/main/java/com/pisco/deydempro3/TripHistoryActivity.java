package com.pisco.deydempro3;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TripHistoryActivity extends AppCompatActivity {

    RecyclerView recycler;
    List<Trip> tripList = new ArrayList<>();
    TripAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);

        recycler = findViewById(R.id.recyclerTrips);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TripAdapter(tripList);
        recycler.setAdapter(adapter);

        loadTrips();
    }

    private void loadTrips(){

        int driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        String url = Constants.BASE_URL + "get_driver_trips.php?driver_id=" + driverId;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {

                    try {

                        JSONArray arr = new JSONArray(response);

                        for(int i=0; i<arr.length(); i++){

                            JSONObject o = arr.getJSONObject(i);

                            tripList.add(new Trip(
                                    o.getString("pickup_address"),
                                    o.getString("dropoff_address"),
                                    o.getString("price"),
                                    o.getString("driver_gain"),
                                    o.getString("status"),
                                    o.getString("commission"),
                                    o.getString("completed_at")
                            ));
                        }

                        adapter.notifyDataSetChanged();

                    } catch (Exception e){
                        e.printStackTrace();
                    }

                },
                error -> Toast.makeText(this,"Erreur chargement",Toast.LENGTH_LONG).show()
        );

        Volley.newRequestQueue(this).add(req);
    }
}