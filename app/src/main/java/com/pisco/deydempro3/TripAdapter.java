package com.pisco.deydempro3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder>{

    private List<Trip> list;

    public TripAdapter(List<Trip> list){
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView route, price, gain, status, comm, date;

        public ViewHolder(View v){
            super(v);
            route = v.findViewById(R.id.tvRoute);
            price = v.findViewById(R.id.tvPrice);
            gain = v.findViewById(R.id.tvGain);
            status = v.findViewById(R.id.tvStatus);
            comm = v.findViewById(R.id.tvCom);
            date = v.findViewById(R.id.tvDate);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){

        Trip t = list.get(position);

        holder.route.setText(t.pickup + " → " + t.dropoff);
        holder.price.setText("Prix : " + t.price + " FCFA");
        holder.gain.setText("Gain : " + t.gain + " FCFA");
        holder.status.setText("Status : " + t.status);
        holder.comm.setText("Commission : " + t.commission);
        holder.date.setText("Commission : " + t.date);
    }

    @Override
    public int getItemCount(){
        return list.size();
    }
}
