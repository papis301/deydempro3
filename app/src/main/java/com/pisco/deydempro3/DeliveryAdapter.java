package com.pisco.deydempro3;

import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.MyViewHolder> {

    Context context;
    List<Delivery> list;
    String acceptUrl = "http://192.168.1.7/deydemlivraisonphpmysql/accept_delivery.php";

    public DeliveryAdapter(Context context, List<Delivery> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_delivery, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder h, int position) {
        Delivery d = list.get(position);

        h.tvPickup.setText("Pickup : " + d.pickup_address);
        h.tvDrop.setText("Destination : " + d.dropoff_address);
        h.tvPrice.setText(d.price + " FCFA");

        // CLIC SUR "ACCEPTER"
        h.btnAccept.setOnClickListener(v -> acceptDelivery(d));
    }

    /**
     * Méthode correcte pour accepter une livraison
     */
    private void acceptDelivery(Delivery delivery) {

        StringRequest req = new StringRequest(Request.Method.POST, acceptUrl,
                response -> {
                    Toast.makeText(context, "Livraison acceptée ✔", Toast.LENGTH_LONG).show();

                    // ➜ Redirection vers la navigation
                    Intent i = new Intent(context, DeliveryNavigationActivity.class);
                    i.putExtra("delivery_id", delivery.id);
                    i.putExtra("pickup_lat", delivery.pickup_lat);
                    i.putExtra("pickup_lng", delivery.pickup_lng);
                    i.putExtra("drop_lat", delivery.dropoff_lat);
                    i.putExtra("drop_lng", delivery.dropoff_lng);
                    i.putExtra("pickup_address", delivery.pickup_address);
                    i.putExtra("dropoff_address", delivery.dropoff_address);
                    i.putExtra("price", delivery.price);

                    // IMPORTANT : pour démarrer une Activity depuis un adapter
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                },
                error -> {
                    Toast.makeText(context, "Erreur réseau ❌", Toast.LENGTH_SHORT).show();
                    Log.e("ACCEPT_ERROR", error.toString());
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("delivery_id", delivery.id);
                params.put("driver_id", "1");  // Remplacer par SharedPreferences
                return params;
            }
        };

        Volley.newRequestQueue(context).add(req);
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvPickup, tvDrop, tvPrice;
        Button btnAccept;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDrop = itemView.findViewById(R.id.tvDrop);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnAccept = itemView.findViewById(R.id.btnAccept);
        }
    }
}
