package deydemv3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.pisco.deydempro3.R;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RideSelectActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    TextView tvprise, tvdepot;
    EditText tvadress;
    private static final int PICKUP_REQUEST = 1001;
    LatLng pickupLatLng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ride_select);

        tvprise = findViewById(R.id.prise);
        tvadress = findViewById(R.id.tvadress);

        tvprise.setOnClickListener(v -> openAutocomplete(PICKUP_REQUEST));
        tvadress.setOnClickListener(v -> openAutocomplete(PICKUP_REQUEST));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);

                if (requestCode == PICKUP_REQUEST) {
                    pickupLatLng = place.getLatLng();
                    if (tvprise != null) tvadress.setText(place.getAddress());
                    //btnClearPickup.setVisibility(View.VISIBLE);
                }


                updateMap();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("Places", "Status: " + status.getStatusMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void updateMap() {
        if (mMap == null) return;

        mMap.clear();

        if (pickupLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(pickupLatLng)
                    .build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));



    }
    private void openAutocomplete(int requestCode) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );

        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields
        )
                .setCountries(Arrays.asList("SN"))   // Limite au Sénégal
                .build(this);

        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng dakar = new LatLng(14.7167, -17.4677);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dakar, 12));
    }
}