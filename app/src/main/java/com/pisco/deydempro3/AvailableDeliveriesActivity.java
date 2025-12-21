package com.pisco.deydempro3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class AvailableDeliveriesActivity extends AppCompatActivity {

    RecyclerView recycler;
    ArrayList<Delivery> deliveries;
    String URL = "https://pisco.alwaysdata.net/get_deliveries.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_deliveries);

        recycler = findViewById(R.id.recyclerDeliveries);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        deliveries = new ArrayList<>();

        loadDeliveries();
    }

    private void loadDeliveries() {

        StringRequest req = new StringRequest(Request.Method.GET, URL,
                response -> {
                    Log.e("SERVER_RAW", response);

                    try {
                        JSONArray arr = new JSONArray(response);

                        deliveries.clear();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);

                            deliveries.add(new Delivery(
                                    o.getString("id"),
                                    o.getString("pickup_address"),
                                    o.getString("pickup_lat"),
                                    o.getString("pickup_lng"),
                                    o.getString("dropoff_address"),
                                    o.getString("dropoff_lat"),
                                    o.getString("dropoff_lng"),
                                    o.getString("price")
                            ));

                        }

                        recycler.setAdapter(new DeliveryAdapter(this, deliveries));

                    } catch (Exception e) {
                        Log.e("JSON_ERR", e.getMessage());
                    }

                },
                error -> Log.e("Err", error.toString())
        );

        Volley.newRequestQueue(this).add(req);
    }
}
