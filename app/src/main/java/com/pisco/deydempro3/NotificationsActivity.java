package com.pisco.deydempro3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private LinearLayout layoutEmpty;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);
        layoutEmpty = findViewById(R.id.layoutEmpty);


        fetchNotifications();
    }

    private void fetchNotifications() {
        String url = "https://pisco.alwaysdata.net/get_notifications.php";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if(jsonObject.getString("status").equals("success")) {
                            JSONArray array = jsonObject.getJSONArray("data");
                            notificationList.clear();
                            for(int i=0;i<array.length();i++){
                                JSONObject obj = array.getJSONObject(i);
                                NotificationItem item = new NotificationItem(
                                        obj.getInt("id"),
                                        obj.getString("title"),
                                        obj.getString("message"),
                                        obj.getString("type"),
                                        obj.optString("action_url", null),
                                        obj.getString("created_at")
                                );
                                notificationList.add(item);

                                // Popup immédiat pour les notifications urgentes ou update
                                if(item.getType().equals("urgent") || item.getType().equals("update")) {
                                    showPopup(item);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            if(notificationList.isEmpty()){
                                recyclerView.setVisibility(View.GONE);
                                layoutEmpty.setVisibility(View.VISIBLE);
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                                layoutEmpty.setVisibility(View.GONE);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }

    private void showPopup(NotificationItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getTitle())
                .setMessage(item.getMessage())
                .setCancelable(false);

        if(item.getActionUrl() != null && !item.getActionUrl().isEmpty()){
            builder.setPositiveButton("Télécharger", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getActionUrl()));
                startActivity(browserIntent);
            });
        }

        builder.setNegativeButton("Fermer", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
