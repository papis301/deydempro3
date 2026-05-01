package deydemv3;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pisco.deydempro3.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CourseMapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    GoogleMap mMap;
    CourseModel course;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_map);

        course = (CourseModel) getIntent().getSerializableExtra("course");

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng pickup = new LatLng(course.pickupLat, course.pickupLng);
        LatLng dropoff = new LatLng(course.dropLat, course.dropLng);


        mMap.addMarker(new MarkerOptions()
                .position(pickup)
                .title("Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_GREEN)));

        mMap.addMarker(new MarkerOptions()
                .position(dropoff)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_RED)));

        drawOSRMRoute(pickup, dropoff);
    }

    // 🔥 OSRM ROUTE
    private void drawOSRMRoute(LatLng origin, LatLng destination) {

        String url = "https://router.project-osrm.org/route/v1/driving/"
                + origin.longitude + "," + origin.latitude + ";"
                + destination.longitude + "," + destination.latitude
                + "?overview=full&geometries=geojson";

        new Thread(() -> {
            try {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));

                String line;
                StringBuilder response = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());

                JSONArray coords = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                double distanceKm = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getDouble("distance") / 1000;

                int durationMin = (int) (
                        json.getJSONArray("routes")
                                .getJSONObject(0)
                                .getDouble("duration") / 60);

                PolylineOptions polylineOptions = new PolylineOptions()
                        .width(10f)
                        .color(Color.BLUE);

                for (int i = 0; i < coords.length(); i++) {
                    JSONArray c = coords.getJSONArray(i);
                    polylineOptions.add(new LatLng(
                            c.getDouble(1),
                            c.getDouble(0)
                    ));
                }

                runOnUiThread(() -> {
                    mMap.addPolyline(polylineOptions);

                    LatLngBounds bounds = new LatLngBounds.Builder()
                            .include(origin)
                            .include(destination)
                            .build();

                    mMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds, 150)
                    );

                    Toast.makeText(this,
                            "Distance : " + String.format("%.1f", distanceKm)
                                    + " km | ⏱ " + durationMin + " min",
                            Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
