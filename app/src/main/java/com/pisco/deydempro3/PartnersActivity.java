package com.pisco.deydempro3;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class PartnersActivity
        extends AppCompatActivity {

    ListView listPartners;

    TextView txtEmpty;

    ArrayList<HashMap<String, String>>
            partnersList;

    PartnersAdapter adapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_partners
        );

        //
        // 🔥 INIT
        //
        listPartners =
                findViewById(
                        R.id.listPartners
                );

        txtEmpty =
                findViewById(
                        R.id.txtEmpty
                );

        partnersList =
                new ArrayList<>();

        adapter =
                new PartnersAdapter(

                        this,

                        partnersList
                );

        listPartners.setAdapter(adapter);

        //
        // 🔥 LOAD PARTNERS
        //
        loadPartners();
    }

    //
    // 🔥 LOAD PARTNERS
    //
    private void loadPartners(){

        ProgressDialog dialog =
                new ProgressDialog(this);

        dialog.setMessage(
                "Chargement des partenaires..."
        );

        dialog.setCancelable(false);

        dialog.show();

        String url =
                "https://pisco.alwaysdata.net/api_get_available_partners.php";

        RequestQueue queue =
                Volley.newRequestQueue(this);

        StringRequest request =
                new StringRequest(

                        Request.Method.GET,

                        url,

                        response -> {

                            dialog.dismiss();

                            Log.d(
                                    "PARTNERS_RESPONSE",
                                    response
                            );

                            try {

                                JSONObject obj =
                                        new JSONObject(
                                                response
                                        );

                                boolean success =
                                        obj.getBoolean(
                                                "success"
                                        );

                                if(success){

                                    JSONArray partners =
                                            obj.getJSONArray(
                                                    "partners"
                                            );

                                    partnersList.clear();

                                    //
                                    // 🔥 EMPTY
                                    //
                                    if(partners.length() == 0){

                                        txtEmpty.setVisibility(
                                                View.VISIBLE
                                        );

                                        listPartners.setVisibility(
                                                View.GONE
                                        );

                                        return;
                                    }

                                    //
                                    // 🔥 SHOW LIST
                                    //
                                    txtEmpty.setVisibility(
                                            View.GONE
                                    );

                                    listPartners.setVisibility(
                                            View.VISIBLE
                                    );

                                    //
                                    // 🔥 LOOP
                                    //
                                    for(
                                            int i = 0;
                                            i < partners.length();
                                            i++
                                    ){

                                        JSONObject item =
                                                partners.getJSONObject(i);

                                        HashMap<String, String> map =
                                                new HashMap<>();

                                        map.put(

                                                "id",

                                                item.optString(
                                                        "id"
                                                )
                                        );

                                        map.put(

                                                "nom",

                                                item.optString(
                                                        "nom"
                                                )
                                        );

                                        map.put(

                                                "telephone",

                                                item.optString(
                                                        "telephone"
                                                )
                                        );

                                        partnersList.add(map);
                                    }

                                    adapter.notifyDataSetChanged();

                                } else {

                                    Toast.makeText(

                                            this,

                                            obj.optString(
                                                    "message",
                                                    "Erreur"
                                            ),

                                            Toast.LENGTH_LONG

                                    ).show();
                                }

                            } catch (Exception e){

                                e.printStackTrace();
                                Log.d("erreur", e.toString());

                                Toast.makeText(

                                        this,

                                        "Erreur JSON",

                                        Toast.LENGTH_LONG

                                ).show();
                            }

                        },

                        error -> {

                            dialog.dismiss();

                            Log.e(

                                    "PARTNERS_ERROR",

                                    error.toString()
                            );

                            Toast.makeText(

                                    this,

                                    "Erreur réseau",

                                    Toast.LENGTH_LONG

                            ).show();
                        }
                );

        queue.add(request);
    }
}