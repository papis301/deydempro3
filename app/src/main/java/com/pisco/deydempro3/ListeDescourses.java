package com.pisco.deydempro3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class ListeDescourses extends AppCompatActivity implements CourseAdapter.OnAcceptClick {

    RecyclerView rv;
    Button goOnlineBtn;
    CourseAdapter adapter;

    int driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liste_descourses);

        rv = findViewById(R.id.rvCourses);
        goOnlineBtn = findViewById(R.id.btnGoOnline);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter();
        adapter.setListener(this);
        rv.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        driverId = prefs.getInt(Constants.KEY_DRIVER_ID, -1);

        goOnlineBtn.setOnClickListener(v -> startLocationService());

        loadPendingCourses();
    }

    private void loadPendingCourses() {
        String url = Constants.BASE_URL + "get_pending_courses.php";

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONArray arr = response.getJSONArray("courses");
                        adapter.setCourses(arr);
                    } catch (Exception e) { e.printStackTrace(); }
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(req);




    }

    private void startLocationService() {
        Intent svc = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        }

        setDriverOnline();
    }

    private void setDriverOnline() {
        String url = Constants.BASE_URL + "driver_status.php";
        JSONObject obj = new JSONObject();
        try {
            obj.put("driver_id", driverId);
            obj.put("is_online", 1);
        } catch (Exception e) { }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                obj,
                response -> { },
                error -> {}
        );

        Volley.newRequestQueue(this).add(req);




    }

    @Override
    public void onAccept(int courseId) {

        String url = Constants.BASE_URL + "accept_course.php";

        JSONObject data = new JSONObject();
        try {
            data.put("driver_id", driverId);
            data.put("course_id", courseId);
        } catch (Exception e) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                data,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            Toast.makeText(this, "Course acceptée", Toast.LENGTH_SHORT).show();
                            loadPendingCourses();
                        } else {
                            Toast.makeText(this, "Déjà prise", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {}
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(req);




    }
}
