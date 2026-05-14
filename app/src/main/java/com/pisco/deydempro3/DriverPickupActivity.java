package com.pisco.deydempro3;

import static com.pisco.deydempro3.Constants.BASE_URL;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashMap;
import java.util.Map;

public class DriverPickupActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    GoogleMap mMap;

    TextView tvPickupAddress;
    TextView tvDirection;

    Button btnArrived;

    String rideId;

    double pickupLat;
    double pickupLng;

    double driverLat;
    double driverLng;

    String pickupAddress;
    private FusedLocationProviderClient fusedLocationClient;

    private Marker driverMarker;

    private Polyline currentPolyline;

    private boolean firstZoom = true;
    TextView tvEta;
    TextView tvDistance;
    private LatLng lastDriverLatLng;
    private boolean userTouchMap = false;
    ImageButton btnRecenter;
    LinearLayout btnCall;
    LinearLayout btnCancelRide;
    String customerPhone;
    String userId;
    String clientId;

    String customerName;
    String customerPhoto;
    Button btnStartTrip;
    Button btnCompleteTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_pickup);

         userId =
                getSharedPreferences(
                        "DeydemUser",
                        MODE_PRIVATE
                ).getString(
                        "user_id",
                        "0"
                );

        Toast.makeText(
                this,
                "USER ID = " + userId,
                Toast.LENGTH_LONG
        ).show();

        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDirection = findViewById(R.id.tvDirection);
        btnArrived = findViewById(R.id.btnArrived);
        tvEta = findViewById(R.id.tvEta);
        tvDistance = findViewById(R.id.tvDistance);
        btnRecenter =
                findViewById(R.id.btnRecenter);
        btnCall = findViewById(R.id.btnCall);
        btnCancelRide = findViewById(R.id.btnCancelRide);
        btnStartTrip =
                findViewById(R.id.btnStartTrip);

        btnCompleteTrip =
                findViewById(R.id.btnCompleteTrip);
        customerPhone =
                getIntent().getStringExtra(
                        "phone"
                );

        rideId = getIntent().getStringExtra("ride_id");
        clientId =
                getIntent().getStringExtra(
                        "client_id"
                );
        Toast.makeText(
                this,
                "clientId = " + clientId,
                Toast.LENGTH_LONG
        ).show();
        getClientInfo();

        pickupLat = getIntent().getDoubleExtra("pickup_lat",0);
        pickupLng = getIntent().getDoubleExtra("pickup_lng",0);

        driverLat = getIntent().getDoubleExtra("driver_lat",0);
        driverLng = getIntent().getDoubleExtra("driver_lng",0);

        pickupAddress = getIntent().getStringExtra("pickup_address");

        tvPickupAddress.setText(pickupAddress);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        btnCall.setOnClickListener(v -> {

            if(customerPhone == null ||
                    customerPhone.isEmpty()){

                Toast.makeText(
                        this,
                        "Numéro introuvable",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Intent intent =
                    new Intent(
                            Intent.ACTION_DIAL
                    );

            intent.setData(
                    Uri.parse(
                            "tel:" + customerPhone
                    )
            );

            startActivity(intent);
        });

        btnCancelRide.setOnClickListener(v -> {

            cancelRide();
        });

        btnArrived.setOnClickListener(v -> {

//            Toast.makeText(
//                    this,
//                    "Arrivé au pickup",
//                    Toast.LENGTH_SHORT
//            ).show();
            btnStartTrip.setVisibility(View.VISIBLE);
            btnArrived.setVisibility(View.GONE);

            updateTripStatus();
        });

        btnRecenter.setOnClickListener(v -> {

            userTouchMap = false;

            if(driverMarker != null){

                CameraPosition cameraPosition =
                        new CameraPosition.Builder()
                                .target(
                                        driverMarker.getPosition()
                                )
                                .zoom(17f)
                                .tilt(45f)
                                .bearing(
                                        driverMarker.getRotation()
                                )
                                .build();

                mMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                                cameraPosition
                        )
                );
            }
        });

        btnStartTrip.setOnClickListener(v -> {

            startTrip();

        });

        btnCompleteTrip.setOnClickListener(v -> {

            completeTrip();

        });
    }

    private void completeTrip(){

        String url =
                BASE_URL + "complete_trip.php";

        StringRequest request =
                new StringRequest(

                        Request.Method.POST,
                        url,

                        response -> {

                            Toast.makeText(
                                    this,
                                    "Course terminée",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Intent intent =
                                    new Intent(
                                            DriverPickupActivity.this,
                                            DriverHomeActivity.class
                                    );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            );

                            startActivity(intent);

                            finish();

                        },

                        error -> {

                            Toast.makeText(
                                    this,
                                    "Erreur fin course",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                ){

                    @Override
                    protected Map<String,String> getParams(){

                        Map<String,String> params =
                                new HashMap<>();

                        params.put(
                                "ride_id",
                                rideId
                        );

                        return params;
                    }
                };

        Volley.newRequestQueue(this)
                .add(request);
    }

    private void startTrip(){

        String url =
                BASE_URL + "start_trip.php";

        StringRequest request =
                new StringRequest(

                        Request.Method.POST,
                        url,

                        response -> {

                            Toast.makeText(
                                    this,
                                    "Course démarrée",
                                    Toast.LENGTH_SHORT
                            ).show();

                            btnStartTrip.setVisibility(View.GONE);

                            btnCompleteTrip.setVisibility(View.VISIBLE);

                        },

                        error -> {

                            Toast.makeText(
                                    this,
                                    "Erreur démarrage",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                ){

                    @Override
                    protected Map<String,String> getParams(){

                        Map<String,String> params =
                                new HashMap<>();

                        params.put(
                                "ride_id",
                                rideId
                        );

                        return params;
                    }
                };

        Volley.newRequestQueue(this)
                .add(request);
    }

    private void getClientInfo(){

        String url =
                "https://pisco.alwaysdata.net/get_client_info.php?client_id="
                        + clientId;

        JsonObjectRequest request =
                new JsonObjectRequest(

                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            try {

                                if(response.getBoolean("success")){

                                    JSONObject client =
                                            response.getJSONObject("client");

                                    customerName =
                                            client.getString("name");

                                    customerPhone =
                                            client.getString("phone");

                                    customerPhoto =
                                            client.optString("photo");

                                    //
                                    // 🔥 AFFICHAGE
                                    //
                                    tvDirection.setText(
                                            customerName
                                    );

                                    Log.d(
                                            "CLIENT_INFO",
                                            response.toString()
                                    );
                                }

                            } catch(Exception e){
                                e.printStackTrace();
                            }

                        },

                        error -> {

                            Log.e(
                                    "CLIENT_ERROR",
                                    error.toString()
                            );
                        }

                );

        Volley.newRequestQueue(this)
                .add(request);
    }

    private void cancelRide(){

        String url =
                "https://pisco.alwaysdata.net/refuse_trip.php";

        Log.d(
                "CANCEL_RIDE",
                "URL = " + url
        );

        Log.d(
                "CANCEL_RIDE",
                "ride_id = " + rideId
        );

        Log.d(
                "CANCEL_RIDE",
                "driver_id = " + userId
        );

        StringRequest request =
                new StringRequest(

                        Request.Method.POST,
                        url,

                        response -> {

                            Log.d(
                                    "CANCEL_RESPONSE",
                                    response
                            );

                            Toast.makeText(
                                    this,
                                    "Course annulée",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        },

                        error -> {

                            Log.e(
                                    "CANCEL_ERROR",
                                    error.toString()
                            );

                            if(error.networkResponse != null){

                                Log.e(
                                        "CANCEL_CODE",
                                        String.valueOf(
                                                error.networkResponse.statusCode
                                        )
                                );
                            }

                            Toast.makeText(
                                    this,
                                    "Erreur annulation",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                ){

                    @Override
                    protected Map<String,String> getParams(){

                        Map<String,String> params =
                                new HashMap<>();

                        params.put(
                                "ride_id",
                                rideId
                        );

                        params.put(
                                "driver_id",
                                userId
                        );

                        Log.d(
                                "CANCEL_PARAMS",
                                params.toString()
                        );

                        return params;
                    }
                };

        Volley.newRequestQueue(this)
                .add(request);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;

        mMap.setOnCameraMoveStartedListener(reason -> {

            if(reason ==
                    GoogleMap.OnCameraMoveStartedListener
                            .REASON_GESTURE){

                userTouchMap = true;
            }
        });

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );

            return;
        }

        //mMap.setMyLocationEnabled(true);

        // bouton position actuelle
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        LatLng driverPos;

        if(driverLat == 0 || driverLng == 0){

            driverPos =
                    new LatLng(
                            pickupLat,
                            pickupLng
                    );
        } else {

            driverPos =
                    new LatLng(
                            driverLat,
                            driverLng
                    );
        }
        LatLng pickupPos =
                new LatLng(
                        pickupLat,
                        pickupLng
                );
        mMap.addMarker(
                new MarkerOptions()
                        .position(pickupPos)
                        .title("Pickup")
                        .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN
                                )
                        )
        );
        drawRoute(driverPos, pickupPos);
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){

        LocationRequest request =
                LocationRequest.create();

        request.setInterval(3000);

        request.setPriority(
                Priority.PRIORITY_HIGH_ACCURACY
        );

        fusedLocationClient.requestLocationUpdates(

                request,

                new LocationCallback(){

                    @Override
                    public void onLocationResult(
                            LocationResult result
                    ){

                        if(result == null) return;

                        Location location =
                                result.getLastLocation();

                        if(location == null) return;

                        driverLat =
                                location.getLatitude();

                        driverLng =
                                location.getLongitude();

                        LatLng driverPos =
                                new LatLng(
                                        driverLat,
                                        driverLng
                                );
                        //
                        // 🔥 IGNORE GPS JUMPS
                        //
                        if(lastDriverLatLng != null){

                            float distance =
                                    distanceBetween(
                                            lastDriverLatLng,
                                            driverPos
                                    );

                            // ignore saut GPS > 120m
                            if(distance > 120){

                                return;
                            }
                        }

                        lastDriverLatLng = driverPos;

                        LatLng pickupPos =
                                new LatLng(
                                        pickupLat,
                                        pickupLng
                                );

                        sendDriverLocation(
                                driverLat,
                                driverLng
                        );
                        //
                        // 🔥 DRIVER MARKER
                        //
//
// 🔥 DRIVER MARKER
//
                        //
// 🔥 DRIVER MARKER
//
                        if(driverMarker == null){

                            driverMarker =
                                    mMap.addMarker(

                                            new MarkerOptions()
                                                    .position(driverPos)
                                                    .title("Vous")
                                                    .flat(true)
                                                    .anchor(0.5f,0.5f)
                                                    .rotation(0)
                                                    .icon(
                                                            resizeMapIcons(
                                                                    R.drawable.car_top,
                                                                    256,
                                                                    256
                                                            )
                                                    )
                                    );

                        } else {

                            animateDriverMarker(
                                    driverMarker,
                                    driverPos
                            );

                            driverMarker.setRotation(
                                    getBearing(
                                            driverMarker.getPosition(),
                                            driverPos
                                    )
                            );
                            //
// 🔥 SMART CAMERA
//
                            if(!userTouchMap &&
                                    (firstZoom || !isMarkerVisible(driverPos))){

                                CameraPosition cameraPosition =
                                        new CameraPosition.Builder()
                                                .target(driverPos)
                                                .zoom(17f)
                                                .tilt(45f)
                                                .bearing(
                                                        driverMarker.getRotation()
                                                )
                                                .build();

                                mMap.animateCamera(
                                        CameraUpdateFactory.newCameraPosition(
                                                cameraPosition
                                        )
                                );

                                firstZoom = false;
                            }
                        }



                        //
                        // 🔥 REDRAW ROUTE
                        //
                        drawRouteRealtime(
                                driverPos,
                                pickupPos
                        );
                    }

                },

                getMainLooper()
        );
    }


    private void sendDriverLocation(
            double lat,
            double lng
    ){

        String url =
                "https://pisco.alwaysdata.net/update_driver_location.php";

        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        url,

                        response -> {

                        },

                        error -> {

                        }

                ){

                    @Override
                    protected java.util.Map<String,String> getParams(){

                        java.util.Map<String,String> params =
                                new java.util.HashMap<>();

                        params.put(
                                "driver_id",
                                getSharedPreferences(
                                        "DeydemUser",
                                        MODE_PRIVATE
                                ).getString(
                                        "user_id",
                                        "0"
                                )
                        );

                        params.put(
                                "lat",
                                String.valueOf(lat)
                        );

                        params.put(
                                "lng",
                                String.valueOf(lng)
                        );

                        return params;
                    }
                };

        Volley.newRequestQueue(this)
                .add(request);
    }

    private BitmapDescriptor resizeMapIcons(
            int resourceId,
            int width,
            int height
    ){

        Bitmap imageBitmap =
                BitmapFactory.decodeResource(
                        getResources(),
                        resourceId
                );

        if(imageBitmap == null){

            return BitmapDescriptorFactory.defaultMarker();
        }

        Bitmap resizedBitmap =
                Bitmap.createScaledBitmap(
                        imageBitmap,
                        width,
                        height,
                        false
                );

        return BitmapDescriptorFactory.fromBitmap(
                resizedBitmap
        );
    }
    private void drawRouteRealtime(
            LatLng origin,
            LatLng destination
    ){

        String url =
                "https://router.project-osrm.org/route/v1/driving/"
                        + origin.longitude + "," + origin.latitude
                        + ";"
                        + destination.longitude + "," + destination.latitude
                        + "?overview=full&geometries=geojson";

        @SuppressLint("DefaultLocale") JsonObjectRequest request =
                new JsonObjectRequest(

                        Request.Method.GET,
                        url,
                        null,

                        response -> {

                            try {

                                JSONArray routes =
                                        response.getJSONArray("routes");

                                if(routes.length() == 0){
                                    return;
                                }

                                JSONObject route =
                                        routes.getJSONObject(0);
                                double distance =
                                        route.getDouble("distance");

                                double duration =
                                        route.getDouble("duration");

                                String distanceText;

                                if(distance < 1000){

                                    distanceText =
                                            ((int) distance) + " m";

                                } else {

                                    distanceText =
                                            String.format(
                                                    "%.1f km",
                                                    distance / 1000
                                            );
                                }

                                tvDistance.setText(distanceText);

                                int minutes =
                                        (int) (duration / 60);

                                tvEta.setText(
                                        minutes + " min"
                                );

                                JSONObject geometry =
                                        route.getJSONObject("geometry");

                                JSONArray coordinates =
                                        geometry.getJSONArray("coordinates");

                                PolylineOptions options =
                                        new PolylineOptions()
                                                .width(16f)
                                                .color(Color.BLUE)
                                                .geodesic(true);

                                for(int i=0; i<coordinates.length(); i++){

                                    JSONArray point =
                                            coordinates.getJSONArray(i);

                                    double lng =
                                            point.getDouble(0);

                                    double lat =
                                            point.getDouble(1);

                                    options.add(
                                            new LatLng(lat,lng)
                                    );
                                }

                                //
                                // 🔥 REMOVE OLD
                                //
                                if(currentPolyline != null){
                                    currentPolyline.remove();
                                }

                                //
                                // 🔥 DRAW NEW
                                //
                                currentPolyline =
                                        mMap.addPolyline(options);

                                //
                                // 🔥 CAMERA
                                //
                                if(firstZoom){

                                    try{

                                        LatLngBounds.Builder builder =
                                                new LatLngBounds.Builder();

                                        builder.include(origin);
                                        builder.include(destination);

                                        LatLngBounds bounds =
                                                builder.build();

                                        mMap.animateCamera(
                                                CameraUpdateFactory.newLatLngBounds(
                                                        bounds,
                                                        250
                                                )
                                        );

                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }

                                    firstZoom = false;
                                }

                            } catch (Exception e){
                                e.printStackTrace();
                            }

                        },

                        error -> {

                            error.printStackTrace();

                        }

                ){

                    //
                    // 🔥 HEADERS
                    //
                    @Override
                    public Map<String, String> getHeaders(){

                        Map<String, String> headers =
                                new HashMap<>();

                        headers.put(
                                "User-Agent",
                                "Mozilla/5.0"
                        );

                        headers.put(
                                "Accept",
                                "application/json"
                        );

                        headers.put(
                                "Connection",
                                "keep-alive"
                        );

                        return headers;
                    }
                };

        Volley.newRequestQueue(this)
                .add(request);
    }

    private void animateDriverMarker(
            final Marker marker,
            final LatLng toPosition
    ){

        if(marker == null) return;

        LatLng startPosition =
                marker.getPosition();

        ValueAnimator valueAnimator =
                ValueAnimator.ofFloat(0,1);

        valueAnimator.setDuration(1500);

        valueAnimator.setInterpolator(
                new LinearInterpolator()
        );

        valueAnimator.addUpdateListener(animation -> {

            float v =
                    (float) animation.getAnimatedValue();

            double lng =
                    v * toPosition.longitude
                            + (1 - v)
                            * startPosition.longitude;

            double lat =
                    v * toPosition.latitude
                            + (1 - v)
                            * startPosition.latitude;

            LatLng newPos =
                    new LatLng(lat,lng);

            marker.setPosition(newPos);

        });

        valueAnimator.start();
    }

    private float getBearing(
            LatLng start,
            LatLng end
    ){

        double lat =
                Math.abs(start.latitude - end.latitude);

        double lng =
                Math.abs(start.longitude - end.longitude);

        if(start.latitude < end.latitude &&
                start.longitude < end.longitude)

            return (float)(Math.toDegrees(Math.atan(lng / lat)));

        else if(start.latitude >= end.latitude &&
                start.longitude < end.longitude)

            return (float)((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);

        else if(start.latitude >= end.latitude &&
                start.longitude >= end.longitude)

            return (float)(Math.toDegrees(Math.atan(lng / lat)) + 180);

        else if(start.latitude < end.latitude &&
                start.longitude >= end.longitude)

            return (float)((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);

        return -1;
    }
    private void drawRoute(LatLng origin, LatLng destination){

        LatLngBounds.Builder builder =
                new LatLngBounds.Builder();

        builder.include(origin);
        builder.include(destination);

        LatLngBounds bounds =
                builder.build();

        try{

            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            200
                    )
            );

        }catch (Exception e){
            e.printStackTrace();
        }

    }



private void updateTripStatus(){

    String url =
            "https://pisco.alwaysdata.net/update_trip_status.php";

    StringRequest request = new StringRequest(
            Request.Method.POST,
            url,
            response -> {

            },
            error -> {

            }
    ){

        @Override
        protected java.util.Map<String, String> getParams(){

            java.util.Map<String,String> params =
                    new java.util.HashMap<>();

            params.put("ride_id", rideId);
            params.put("status", "onplace");

            return params;
        }
    };

    Volley.newRequestQueue(this)
            .add(request);
}

    private boolean isMarkerVisible(LatLng latLng){

        if(mMap == null) return false;

        LatLngBounds bounds =
                mMap.getProjection()
                        .getVisibleRegion()
                        .latLngBounds;

        return bounds.contains(latLng);
    }

    private float distanceBetween(
            LatLng start,
            LatLng end
    ){

        float[] results = new float[1];

        Location.distanceBetween(
                start.latitude,
                start.longitude,
                end.latitude,
                end.longitude,
                results
        );

        return results[0];
    }
}