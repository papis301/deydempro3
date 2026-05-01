package deydemv3;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pisco.deydempro3.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchLocationActivity extends AppCompatActivity {

    EditText etSearch;
    ListView listResults;

    ArrayList<String> displayList = new ArrayList<>();
    ArrayList<Double> latList = new ArrayList<>();
    ArrayList<Double> lonList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_location);

        etSearch = findViewById(R.id.etSearch);
        listResults = findViewById(R.id.listResults);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            @Override public void afterTextChanged(Editable s){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    searchNominatim(s.toString());
                }
            }
        });

        listResults.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent();
            i.putExtra("address", displayList.get(position));
            i.putExtra("lat", latList.get(position));
            i.putExtra("lon", lonList.get(position));
            setResult(RESULT_OK, i);
            finish();
        });
    }

//

    private void searchNominatim(String query) {
        String url = "https://nominatim.openstreetmap.org/search?addressdetails=1&q=sn+"+query+"&format=jsonv2";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        displayList.clear();
                        latList.clear();
                        lonList.clear();

                        JSONArray arr = new JSONArray(response);

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);

                            displayList.add(obj.getString("display_name"));
                            latList.add(obj.getDouble("lat"));
                            lonList.add(obj.getDouble("lon"));
                        }

                        listResults.setAdapter(new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_list_item_1,
                                displayList
                        ));

                    } catch (Exception e) { e.printStackTrace(); }
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "YourAppName/1.0 (contact@example.com)");
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

}
