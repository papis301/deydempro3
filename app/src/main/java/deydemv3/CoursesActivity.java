package deydemv3;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pisco.deydempro3.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoursesActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    CourseAdapter adapter;
    List<Course> courseList = new ArrayList<>();
    String URL_GET = "https://pisco.alwaysdata.net/get_courses.php";
    String URL_CANCEL = "https://pisco.alwaysdata.net/cancel_course.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);

        recyclerView = findViewById(R.id.recyclerCourses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CourseAdapter(courseList, courseId -> cancelCourse(courseId));
        recyclerView.setAdapter(adapter);

        loadCourses();
    }

    private void loadCourses() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, URL_GET, null,
                response -> {
                    courseList.clear();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            Course c = new Course(
                                    obj.getInt("id"),
                                    obj.getString("pickup_address"),
                                    obj.getString("dropoff_address"),
                                    obj.getString("status"),
                                    obj.getInt("price"),
                                    obj.getString("created_at")
                            );
                            courseList.add(c);

                        } catch (Exception e) { }
                    }

                    // Trier : pending en premier
                    Collections.sort(courseList, (a, b) ->
                            a.getStatus().equals("pending") && !b.getStatus().equals("pending") ? -1 : 1
                    );

                    adapter.notifyDataSetChanged();
                },
                error -> Toast.makeText(this, "Erreur chargement", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }

    private void cancelCourse(int courseId) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest req = new StringRequest(Request.Method.POST, URL_CANCEL,
                response -> {
                    Toast.makeText(this, "Course annulée", Toast.LENGTH_SHORT).show();
                    loadCourses(); // refresh
                },
                error -> Toast.makeText(this, "Erreur annulation", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("course_id", String.valueOf(courseId));
                return params;
            }
        };

        queue.add(req);
    }
}
