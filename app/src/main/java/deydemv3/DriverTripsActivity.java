package deydemv3;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pisco.deydempro3.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class DriverTripsActivity extends AppCompatActivity {

    private RecyclerView recyclerTrips;

    private ArrayList<TripModel> tripList =
            new ArrayList<>();

    private DriverTripsAdapter adapter;

    private String userId = "";
    private TextView txtEmpty;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_driver_trips);
        //
        // 🔥 INIT
        //
        recyclerTrips =
                findViewById(R.id.recyclerTrips);

        recyclerTrips.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new DriverTripsAdapter(
                        this,
                        tripList
                );
        txtEmpty = findViewById(R.id.txtEmpty);

        recyclerTrips.setAdapter(adapter);

        //
        // 🔥 USER ID
        //
        // USER
        SharedPreferences sp =
                getSharedPreferences("DeydemUser", MODE_PRIVATE);

        userId =
                sp.getString("user_id", "0");
        Toast.makeText(
                this,
                "response id"+userId,
                Toast.LENGTH_SHORT
        ).show();

        //
        // 🔥 LOAD TRIPS
        //
        loadTrips();
    }

    private void loadTrips(){

        String url =
                "https://pisco.alwaysdata.net/get_driver_trips.php?driver_id="
                        + userId;

        @SuppressLint("NotifyDataSetChanged") StringRequest request =
                new StringRequest(

                        Request.Method.GET,
                        url,

                        response -> {

                            try {

                                JSONObject json =
                                        new JSONObject(response);

                                if(json.getBoolean("success")){


                                    JSONArray trips =
                                            json.getJSONArray("trips");

                                    //
                                    // 🔥 CLEAR
                                    //
                                    tripList.clear();

                                    //
                                    // 🔥 LOOP
                                    //
                                    for(int i = 0; i < trips.length(); i++){

                                        JSONObject item =
                                                trips.getJSONObject(i);

                                        TripModel trip =
                                                new TripModel();

                                        trip.setId(
                                                item.getString("id")
                                        );

                                        trip.setClientName(
                                                item.optString(
                                                        "client_name",
                                                        "Client"
                                                )
                                        );

                                        trip.setPickup(
                                                item.optString(
                                                        "pickup_address",
                                                        ""
                                                )
                                        );

                                        trip.setDropoff(
                                                item.optString(
                                                        "dropoff_address",
                                                        ""
                                                )
                                        );

                                        trip.setVehicle(
                                                item.optString(
                                                        "vehicle_type",
                                                        ""
                                                )
                                        );

                                        trip.setPrice(
                                                item.optString(
                                                        "price",
                                                        "0"
                                                )
                                        );

                                        trip.setCancelledBy(
                                                item.optString(
                                                        "cancelled_by",
                                                        ""
                                                )
                                        );

                                        String status =
                                                item.optString(
                                                        "status",
                                                        "pending"
                                                );

                                        if(status == null || status.isEmpty()){

                                            status = "pending";
                                        }

                                        trip.setStatus(status);

                                        trip.setDate(
                                                item.optString(
                                                        "created_at",
                                                        ""
                                                )
                                        );

                                        //
                                        // 🔥 ADD
                                        //
                                        tripList.add(trip);
                                    }

                                    //
                                    // 🔥 REFRESH
                                    //
                                    adapter.notifyDataSetChanged();
                                    if(tripList.isEmpty()){

                                        txtEmpty.setVisibility(View.VISIBLE);

                                    } else {

                                        txtEmpty.setVisibility(View.GONE);
                                    }

                                } else {

                                    Toast.makeText(
                                            this,
                                            "Aucune course",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                            } catch(Exception e){

                                e.printStackTrace();

                                Toast.makeText(
                                        this,
                                        "Erreur JSON",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                        },

                        error -> {

                            Toast.makeText(
                                    this,
                                    "Erreur réseau",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                );

        Volley.newRequestQueue(this)
                .add(request);
    }
}