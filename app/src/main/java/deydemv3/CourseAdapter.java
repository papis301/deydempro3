package deydemv3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.pisco.deydempro3.R;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {

    private List<Course> courseList;
    private OnCancelClickListener cancelClickListener;

    public interface OnCancelClickListener {
        void onCancel(int courseId);
    }

    public CourseAdapter(List<Course> courseList, OnCancelClickListener listener) {
        this.courseList = courseList;
        this.cancelClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtPickup, txtDropoff, txtStatus;
        Button btnCancel;
        TextView txtDate;

        public ViewHolder(View itemView) {
            super(itemView);
            txtPickup = itemView.findViewById(R.id.txtPickup);
            txtDropoff = itemView.findViewById(R.id.txtDropoff);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            txtDate = itemView.findViewById(R.id.txtDate);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coursec, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Course c = courseList.get(position);

        holder.txtPickup.setText("Départ : " + c.getPickup_address());
        holder.txtDropoff.setText("Arrivée : " + c.getDropoff_address());
        holder.txtStatus.setText("Statut : " + c.getStatus());
        holder.txtDate.setText("Date : " + c.getCreated_at());

        if (c.getStatus().equals("pending")) {
            holder.btnCancel.setVisibility(View.VISIBLE);

            holder.btnCancel.setOnClickListener(v -> {
                if (cancelClickListener != null) {
                    cancelClickListener.onCancel(c.getId());
                }
            });

        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }
}

