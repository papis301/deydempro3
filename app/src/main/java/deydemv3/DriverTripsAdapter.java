package deydemv3;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pisco.deydempro3.R;

import java.util.ArrayList;

public class DriverTripsAdapter extends RecyclerView.Adapter<DriverTripsAdapter.TripViewHolder> {

    private Context context;

    private ArrayList<TripModel> list;

    public DriverTripsAdapter(
            Context context,
            ArrayList<TripModel> list
    ){

        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(context)
                        .inflate(
                                R.layout.item_driver_trip,
                                parent,
                                false
                        );

        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TripViewHolder holder,
            int position
    ) {

        TripModel trip =
                list.get(position);

        //
        // 🔥 CLIENT
        //
        holder.txtClient.setText(
                trip.getClientName()
        );

        //
        // 🔥 VEHICLE
        //
        holder.txtVehicle.setText(
                trip.getVehicle()
        );

        //
        // 🔥 PRICE
        //
        holder.txtPrice.setText(
                trip.getPrice() + " CFA"
        );

        //
        // 🔥 PICKUP
        //
        holder.txtPickup.setText(
                "📍 " + trip.getPickup()
        );

        //
        // 🔥 DROPOFF
        //
        holder.txtDropoff.setText(
                "🏁 " + trip.getDropoff()
        );

        //
        // 🔥 STATUS
        //
        holder.txtStatus.setText(
                trip.getStatus()
        );
        holder.txtDate.setText(
                "📅 " + trip.getDate()
        );

        if(
                trip.getStatus() != null
                        && trip.getStatus().equals("cancelled")
                        && trip.getCancelledBy() != null
                        && !trip.getCancelledBy().isEmpty()
        ){

            holder.txtCancelledBy.setVisibility(View.VISIBLE);

            holder.txtCancelledBy.setText(
                    "❌ Annulé par "
                            + trip.getCancelledBy()
            );

        } else {

            holder.txtCancelledBy.setVisibility(View.GONE);
        }

        //
        // 🔥 COLORS
        //
        switch (trip.getStatus()){

            case "completed":

                holder.txtStatus.setTextColor(
                        Color.parseColor("#4CAF50")
                );

                break;

            case "cancelled":

                holder.txtStatus.setTextColor(
                        Color.parseColor("#F44336")
                );

                break;

            case "ongoing":

                holder.txtStatus.setTextColor(
                        Color.parseColor("#2196F3")
                );

                break;

            default:

                holder.txtStatus.setTextColor(
                        Color.parseColor("#FF9800")
                );

                break;
        }
    }

    @Override
    public int getItemCount() {

        return list.size();
    }

    //
    // 🔥 VIEW HOLDER
    //
    public static class TripViewHolder
            extends RecyclerView.ViewHolder {

        TextView txtClient;
        TextView txtVehicle;
        TextView txtPrice;
        TextView txtPickup;
        TextView txtDropoff;
        TextView txtStatus;
        TextView txtDate;
        TextView txtCancelledBy;

        public TripViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            txtClient =
                    itemView.findViewById(
                            R.id.txtClient
                    );

            txtVehicle =
                    itemView.findViewById(
                            R.id.txtVehicle
                    );

            txtPrice =
                    itemView.findViewById(
                            R.id.txtPrice
                    );

            txtPickup =
                    itemView.findViewById(
                            R.id.txtPickup
                    );

            txtDropoff =
                    itemView.findViewById(
                            R.id.txtDropoff
                    );

            txtStatus =
                    itemView.findViewById(
                            R.id.txtStatus
                    );
            txtDate =
                    itemView.findViewById(
                            R.id.txtDate
                    );

            txtCancelledBy =
                    itemView.findViewById(
                            R.id.txtCancelledBy
                    );
        }
    }
}
