package deydemv3;

import static deydemv3.Constants.BASE_URL;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pisco.deydempro3.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CoursesAdapter extends RecyclerView.Adapter<CoursesAdapter.ViewHolder> {

    ArrayList<CourseModel> list;
    Context context;

    public CoursesAdapter(Context context, ArrayList<CourseModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_courses, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CourseModel c = list.get(position);

        h.tvPickup.setText(c.pickup);
        h.tvDropoff.setText(c.dropoff);
        h.tvStatus.setText(c.status);
        h.tvPrice.setText(c.price + " FCFA");
        h.tvPhone.setText("Date : " + c.phone);

        // 🎨 Couleur selon statut
        switch (c.status) {
            case "pending":
                h.card.setStrokeColor(Color.GRAY);
                h.btnCancel.setVisibility(View.VISIBLE);
                break;

            case "accepted":
                h.card.setStrokeColor(Color.BLUE);
                h.btnCancel.setVisibility(View.VISIBLE);
                break;

            case "ongoing":
                h.card.setStrokeColor(Color.YELLOW);
                h.btnCancel.setVisibility(View.VISIBLE);
                break;

            case "completed":
                h.card.setStrokeColor(Color.GREEN);
                h.btnCancel.setVisibility(View.GONE);
                break;

            case "cancelled":
                h.card.setStrokeColor(Color.RED);
                h.btnCancel.setVisibility(View.GONE);
                break;
        }

        // ❌ ANNULER COURSE
        h.btnCancel.setOnClickListener(v -> {
            //confirmCancel(c.id);
            showCancelDialog(c.id);
        });

        h.itemView.setOnClickListener(v -> {

            BottomSheetDialog dialog = new BottomSheetDialog(context);
            View sheet = LayoutInflater.from(context)
                    .inflate(R.layout.bottom_sheet_course, null);

            TextView tvPickup = sheet.findViewById(R.id.tvPickup);
            TextView tvDropoff = sheet.findViewById(R.id.tvDropoff);
            TextView tvPrice = sheet.findViewById(R.id.tvPrice);
            TextView tvStatus = sheet.findViewById(R.id.tvStatus);
            //TextView tvDate = sheet.findViewById(R.id.tvDate);

            MaterialButton btnMap = sheet.findViewById(R.id.btnMap);
            MaterialButton btnDetails = sheet.findViewById(R.id.btnDetails);
            MaterialButton btnCancel = sheet.findViewById(R.id.btnCancel);

            tvPickup.setText("📍 " + c.pickup);
            tvDropoff.setText("➡ " + c.dropoff);
            tvPrice.setText("💰 " + c.price + " FCFA");
            tvStatus.setText("Statut : " + c.status);

            // Annuler visible seulement si pending
            btnCancel.setVisibility(
                    c.status.equals("pending") ? View.VISIBLE : View.GONE
            );

            btnDetails.setOnClickListener(x -> {
               //String phone = recupphonedriver(c.driverId);
               //Toast.makeText(context, "numero"+phone, Toast.LENGTH_LONG).show();
                Intent i = new Intent(context, CourseDetailActivity.class);
                i.putExtra("course", (Parcelable) c);
                context.startActivity(i);
                dialog.dismiss();
            });

            btnCancel.setOnClickListener(x -> {
                // TODO : appel API annulation
                dialog.dismiss();
            });

            btnMap.setOnClickListener(x -> {

                Intent i = new Intent(context, CourseMapActivity.class);
                i.putExtra("course", c);
                context.startActivity(i);
                dialog.dismiss();
            });


            dialog.setContentView(sheet);
            dialog.show();
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // 🔔 CONFIRMATION
//    private void confirmCancel(int courseId) {
//        new AlertDialog.Builder(context)
//                .setTitle("Annuler la course")
//                .setMessage("Voulez-vous vraiment annuler cette course ?")
//                .setPositiveButton("Oui", (d, w) -> cancelCourse(courseId))
//                .setNegativeButton("Non", null)
//                .show();
//    }

    private void showCancelDialog(int courseId) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_cancel_course, null);

        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();

        RadioGroup rg = view.findViewById(R.id.rgReasons);
        TextInputLayout tilOther = view.findViewById(R.id.tilOther);
        TextInputEditText etOther = view.findViewById(R.id.etOther);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbOther) {
                tilOther.setVisibility(View.VISIBLE);
            } else {
                tilOther.setVisibility(View.GONE);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {

            int checkedId = rg.getCheckedRadioButtonId();
            if (checkedId == -1) {
                Toast.makeText(context, "Veuillez choisir une raison", Toast.LENGTH_SHORT).show();
                return;
            }

            String reason;

            if (checkedId == R.id.rbOther) {
                reason = etOther.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(context, "Veuillez préciser la raison", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                RadioButton rb = view.findViewById(checkedId);
                reason = rb.getText().toString();
            }

            dialog.dismiss();
            cancelCourse(courseId, reason);
        });
    }


    // 🔥 APPEL API ANNULATION
    private void cancelCourse(int courseId, String reason) {

        String url = "https://pisco.alwaysdata.net/cancel_course.php";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Toast.makeText(context, "Course annulée", Toast.LENGTH_SHORT).show();
                    //fetchCourses(); // refresh
                },
                error -> Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("course_id", String.valueOf(courseId));
                params.put("cancel_reason", reason);
                params.put("cancelled_by", "client"); // ou driver

                // 🔥 IMPORTANT
                params.put("reason_code", "CLIENT_CANCEL");
                params.put("reason_text", reason);
                return params;
            }
        };

        Volley.newRequestQueue(context).add(req);
    }


    private String recupphonedriver(int driverId){
        final String[] phonerecup = {null};
        StringRequest req = new StringRequest(Request.Method.POST,
                BASE_URL + "get_user_by_id.php",
                response -> {
                    Log.d("numero", response);
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getBoolean("success")) {
                            JSONObject user = obj.getJSONObject("user");
                             phonerecup[0] = user.getString("phone");

                        }
                    } catch (Exception e) {}
                },
                error -> {}
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("user_id", String.valueOf(driverId));
                return p;
            }
        };
        Volley.newRequestQueue(context).add(req);
        return phonerecup[0];
    }

    // 🔗 VIEW HOLDER
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvPickup, tvDropoff, tvPrice, tvStatus, tvPhone;
        MaterialCardView card;
        MaterialButton btnCancel;

        public ViewHolder(@NonNull View v) {
            super(v);

            card = v.findViewById(R.id.cardCourse);
            tvPickup = v.findViewById(R.id.tvPickup);
            tvDropoff = v.findViewById(R.id.tvDropoff);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvPhone = v.findViewById(R.id.tvPhone);
            btnCancel = v.findViewById(R.id.btnCancel);
        }
    }
}
