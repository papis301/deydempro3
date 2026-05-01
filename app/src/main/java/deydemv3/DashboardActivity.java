package deydemv3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pisco.deydempro3.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    CoursesAdapter adapter;

    ArrayList<CourseModel> allCourses = new ArrayList<>();
    ArrayList<CourseModel> displayedCourses = new ArrayList<>();

    MaterialButton btnAll, btnPending, btnOngoing;

    Handler handler = new Handler();
    String url = "https://pisco.alwaysdata.net/get_my_courses.php";

    String currentFilter = "all";
    TextView txtEmpty;
    FloatingActionButton fabNewCourse;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        recyclerView = findViewById(R.id.rvCourses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CoursesAdapter(this, displayedCourses);
        recyclerView.setAdapter(adapter);

        btnAll = findViewById(R.id.btnAll);
        btnPending = findViewById(R.id.btnPending);
        btnOngoing = findViewById(R.id.btnOngoing);
        txtEmpty = findViewById(R.id.txtEmpty);
        fabNewCourse = findViewById(R.id.fabNewCourse);

        fabNewCourse.setOnClickListener(v -> {
            startActivity(new Intent(this, PickupDeliveryActivity.class));
        });


        btnAll.setOnClickListener(v -> {
            currentFilter = "all";
            applyFilter();
            selectButton(btnAll);
        });

        btnPending.setOnClickListener(v -> {
            currentFilter = "pending";
            applyFilter();
            selectButton(btnPending);
        });

        btnOngoing.setOnClickListener(v -> {
            currentFilter = "ongoing";
            applyFilter();
            selectButton(btnOngoing);
        });

        // Chargement initial
        fetchCourses();

        // Rafraîchissement auto toutes les 3 secondes
        handler.postDelayed(refreshRunnable, 3000);
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchCourses();
            handler.postDelayed(this, 3000);
        }
    };


    private void fetchCourses() {

        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        String clientId = sp.getString("user_id", "0");

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Log.e("JSON", response);
                    try {

                        // 🔴 Si l'API retourne une erreur
                        if (response.trim().startsWith("{")) {
                            JSONObject errorObj = new JSONObject(response);
                            Toast.makeText(
                                    this,
                                    errorObj.getString("message"),
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        // 🟢 Sinon tableau normal
                        JSONArray array = new JSONArray(response);

                        allCourses.clear();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject course = array.getJSONObject(i);

                            int driverId = 0;
                            if (!course.isNull("driver_id")) {
                                driverId = course.getInt("driver_id");
                            }

                            CourseModel model = new CourseModel(
                                    course.getInt("id"),
                                    course.getString("pickup_address"),
                                    course.getString("dropoff_address"),
                                    course.getInt("price"),
                                    course.getString("status"),
                                    "",
                                    course.getDouble("pickup_lat"),
                                    course.getDouble("pickup_lng"),
                                    course.getDouble("dropoff_lat"),
                                    course.getDouble("dropoff_lng"),
                                    driverId
                            );

                            allCourses.add(model);
                        }

                        applyFilter();

                    } catch (Exception e) {
                        Log.e("JSON ERROR", e.toString());
                    }
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("client_id", clientId); // 🔥 TRÈS IMPORTANT
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }




    private void applyFilter() {
        displayedCourses.clear();

        if (currentFilter.equals("all")) {
            displayedCourses.addAll(allCourses);
        } else {
            for (CourseModel c : allCourses) {
                if (c.status.equalsIgnoreCase(currentFilter)) {
                    displayedCourses.add(c);
                }
            }
        }

        adapter.notifyDataSetChanged();

        // 🔥 GESTION CLIENT NOUVEAU
        if (displayedCourses.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            txtEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            txtEmpty.setVisibility(View.GONE);
        }
    }


    private void selectButton(MaterialButton selected) {
        btnAll.setChecked(false);
        btnPending.setChecked(false);
        btnOngoing.setChecked(false);
        selected.setChecked(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }
}
