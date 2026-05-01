package com.pisco.deydempro3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SelectPartnerActivity extends AppCompatActivity {

    ListView listPartners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_partner);

        listPartners = findViewById(R.id.listPartners);

        loadPartners();
    }

    private void loadPartners(){

        String url = Constants.BASE_URL + "get_partenaires.php";

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {

                    try{

                        JSONObject obj = new JSONObject(response);
                        JSONArray arr = obj.getJSONArray("partenaires");

                        ArrayList<String> names = new ArrayList<>();
                        ArrayList<String> phones = new ArrayList<>();

                        for(int i=0;i<arr.length();i++){

                            JSONObject p = arr.getJSONObject(i);

                            names.add(p.getString("nom"));
                            phones.add(p.getString("telephone"));
                        }

                        ArrayAdapter<String> adapter =
                                new ArrayAdapter<>(this,
                                        android.R.layout.simple_list_item_2,
                                        android.R.id.text1,
                                        names);

                        listPartners.setAdapter(adapter);

                        listPartners.setOnItemClickListener((parent, view, position, id) -> {

                            String phone = phones.get(position);

                            Intent i = new Intent(Intent.ACTION_DIAL);
                            i.setData(android.net.Uri.parse("tel:" + phone));
                            startActivity(i);
                        });

                    }catch(Exception e){
                        e.printStackTrace();
                    }

                },
                error -> {}
        );

        Volley.newRequestQueue(this).add(req);
    }
}