package deydemv3;


import static deydemv3.Constants.BASE_URL;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pisco.deydempro3.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CourseDetailActivity extends AppCompatActivity {

    TextView tvPickup, tvDropoff, tvPrice, tvStatus, tvDate;
    MaterialButton btnCall, btnMap, btnCancel;
    MaterialCardView statusCard;

    CourseModel course;
    int courseId;
    String phonerecupe;
    LinearLayout layoutDriver;
    ImageView imgDriver;
    TextView tvDriverName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        course = getIntent().getParcelableExtra("course");
        if (course == null) {
            finish();
            return;
        }

        courseId = course.id;


        Toast.makeText(this, "Course driver "+course.driverId, Toast.LENGTH_SHORT).show();



        tvPickup = findViewById(R.id.tvPickup);
        tvDropoff = findViewById(R.id.tvDropoff);
        tvPrice = findViewById(R.id.tvPrice);
        tvStatus = findViewById(R.id.tvStatus);
        tvDate = findViewById(R.id.tvDate);

        btnCall = findViewById(R.id.btnCall);
        btnMap = findViewById(R.id.btnMap);
        btnCancel = findViewById(R.id.btnCancel);
        statusCard = findViewById(R.id.statusCard);

        tvPickup.setText(course.pickup);
        tvDropoff.setText(course.dropoff);
        tvPrice.setText(course.price + " FCFA");
        tvStatus.setText(course.status.toUpperCase());
        layoutDriver = findViewById(R.id.layoutDriver);
        imgDriver = findViewById(R.id.imgDriver);
        tvDriverName = findViewById(R.id.tvDriverName);

        recupphonedriver(course.driverId);



        // 🎨 Couleur du statut
        switch (course.status) {
            case "pending":
                statusCard.setCardBackgroundColor(Color.LTGRAY);
                btnCancel.setVisibility(View.VISIBLE);
                btnCall.setVisibility(View.GONE);
                break;

            case "accepted":
                statusCard.setCardBackgroundColor(Color.parseColor("#2196F3"));
                btnCall.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                break;

            case "ongoing":
                statusCard.setCardBackgroundColor(Color.parseColor("#FFC107"));
                btnCall.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                break;

            case "completed":
                statusCard.setCardBackgroundColor(Color.parseColor("#4CAF50"));
                btnCall.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
                break;

            case "cancelled":
                statusCard.setCardBackgroundColor(Color.RED);
                btnCall.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
                break;
        }



        // 📞 Appeler chauffeur
        btnCall.setOnClickListener(v -> {
            recupphonedriver(course.driverId);
        });

        // 🗺 Voir sur la carte
        btnMap.setOnClickListener(v -> {
            Intent i = new Intent(this, CourseMapActivity.class);
            i.putExtra("course", course);
            startActivity(i);
        });

        // ❌ Annuler
        btnCancel.setOnClickListener(v -> {
            // 👉 Appel API annulation (déjà géré dans l’adapter si tu veux)
            showCancelDialog(courseId);
            //finish();
        });
    }

    private void showCancelDialog(int courseId) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
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
                Toast.makeText(this, "Veuillez choisir une raison", Toast.LENGTH_SHORT).show();
                return;
            }

            String reason;

            if (checkedId == R.id.rbOther) {
                reason = etOther.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Veuillez préciser la raison", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                RadioButton rb = view.findViewById(checkedId);
                reason = rb.getText().toString();
            }

            dialog.dismiss();
            cancelCourse(courseId, reason);
            finish();
        });
    }

    private void cancelCourse(int courseId, String reason) {

        String url = "https://pisco.alwaysdata.net/cancel_course.php";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Toast.makeText(this, "Course annulée", Toast.LENGTH_SHORT).show();
                    //fetchCourses(); // refresh
                },
                error -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("course_id", String.valueOf(courseId));
                params.put("cancel_reason", reason);
                params.put("cancelled_by", "client"); // ou driver
                return params;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }



    private void recupphonedriver(int driverId) {

        StringRequest req = new StringRequest(
                Request.Method.POST,
                BASE_URL + "get_user_by_id.php",
                response -> {
                    Log.d("numero", response);
                    try {
                        JSONObject obj = new JSONObject(response);

                        if (obj.getBoolean("success")) {
                            JSONObject user = obj.getJSONObject("user");

                            String phone = user.getString("phone");
                            String name = user.getString("nom_profil");       // récupérer le nom
                            String photo = user.getString("profile_image");     // récupérer l'URL photo
                            String imageUrl = BASE_URL +
                                    "uploads/profiles/" + photo;
                            // ✅ Afficher le layout chauffeur
                            layoutDriver.setVisibility(View.VISIBLE);

                            // ✅ Afficher nom
                            tvDriverName.setText(name);

                            // ✅ Afficher photo avec Glide
                            if (photo != null && !photo.isEmpty()) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_user)
                                        .error(R.drawable.ic_user)
                                        .into(imgDriver);
                            }

                            // ✅ Afficher numéro
                            tvDate.setText("📞 Chauffeur : " + phone);

                            // ✅ Action du bouton Appeler
                            btnCall.setOnClickListener(v -> {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + phone));
                                startActivity(intent);
                            });

                        } else {
                            Toast.makeText(this, "Infos chauffeur indisponibles", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur parsing", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("user_id", String.valueOf(driverId));
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }


}
