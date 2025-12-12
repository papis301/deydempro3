package com.pisco.deydempro3;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

        h.btnAccept.setOnClickListener(v -> acceptDelivery(d.id));
    }

    private void acceptDelivery(String deliveryId) {

        StringRequest req = new StringRequest(Request.Method.POST, acceptUrl,
                response -> {
                    Toast.makeText(context, response, Toast.LENGTH_LONG).show();
                    Log.d("reponse", response);
                },
                error -> Toast.makeText(context, "Erreur réseau", Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> data = new HashMap<>();
                data.put("delivery_id", deliveryId);
                data.put("driver_id", "1"); // 🔥 mets driver réel (SharedPreferences)
                return data;
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

