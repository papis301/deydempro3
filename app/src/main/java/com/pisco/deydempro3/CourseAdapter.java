package com.pisco.deydempro3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.VH> {

    ArrayList<JSONObject> list = new ArrayList<>();
    OnAcceptClick listener;

    public interface OnAcceptClick { void onAccept(int courseId); }

    public void setListener(OnAcceptClick l) { listener = l; }

    public void setCourses(JSONArray arr) {
        list.clear();
        for (int i = 0; i < arr.length(); i++)
            list.add(arr.optJSONObject(i));
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false));
    }

    @Override
    public void onBindViewHolder(VH h, int i) {
        JSONObject c = list.get(i);

        h.pickup.setText(c.optString("pickup_address"));
        h.drop.setText(c.optString("dropoff_address"));
        h.price.setText(c.optInt("price") + " FCFA");

        int courseId = c.optInt("id");

        h.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(courseId);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    class VH extends RecyclerView.ViewHolder {
        TextView pickup, drop, price;
        Button btnAccept;

        VH(View v) {
            super(v);
            pickup = v.findViewById(R.id.tvPickup);
            drop = v.findViewById(R.id.tvDrop);
            price = v.findViewById(R.id.tvPrice);
            btnAccept = v.findViewById(R.id.btnAccept);
        }
    }
}

