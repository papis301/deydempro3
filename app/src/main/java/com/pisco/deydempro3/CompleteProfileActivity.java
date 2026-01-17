package com.pisco.deydempro3;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 101;
    private static final int PERMISSION_IMAGE = 201;

    private ImageView imgProfile;
    private EditText etFullName;
    private Button btnSubmit;

    private Uri imageUri;
    private int driverId;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0);

        if (driverId == 0) {
            finish();
            return;
        }



        imgProfile = findViewById(R.id.imgProfile);
        etFullName = findViewById(R.id.etFullName);
        btnSubmit = findViewById(R.id.btnSubmit);

        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

        imgProfile.setOnClickListener(v -> openGallery());
        btnSubmit.setOnClickListener(v -> submitProfile());

        loadProfile();
    }

    /* ================= IMAGE ================= */

    private void openGallery() {
        if (!checkPermission()) return;

        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private boolean checkPermission() {
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{permission}, PERMISSION_IMAGE);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imgProfile.setImageURI(imageUri);
        }
    }

    /* ================= SUBMIT ================= */

    private void submitProfile() {
        String fullName = etFullName.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre nom", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "Veuillez choisir une photo", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog.setMessage("Enregistrement...");
        dialog.show();

        String url = Constants.BASE_URL + "complete_driver_profile.php";

        VolleySingleton request = new VolleySingleton(
                Request.Method.POST,
                url,
                response -> {
                    Log.d("SERVER_RESPONSE", response);

                    dialog.dismiss();
                    try {
                        JSONObject json = new JSONObject(response);
                        Toast.makeText(this,
                                json.getString("message"),
                                Toast.LENGTH_SHORT).show();

                        if (json.getBoolean("success")) {
                            startActivity(new Intent(this, MapDeliveriesActivity.class));
                            finish();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Erreur serveur", Toast.LENGTH_SHORT).show();
                }
        ) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("driver_id", String.valueOf(driverId));
                params.put("nom_profil", fullName);
                return params;
            }

            @Override
            public Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();

                try {
                    InputStream is = getContentResolver().openInputStream(imageUri);
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                    byte[] data = new byte[1024];
                    int n;
                    while ((n = is.read(data)) != -1) {
                        buffer.write(data, 0, n);
                    }

                    params.put(
                            "profile_image",
                            new DataPart(
                                    "profile_" + driverId + ".jpg",
                                    buffer.toByteArray(),
                                    "image/jpeg"
                            )
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return params;
            }
        };

        VolleySingleton.getInstance(this).add(
                request
        );

    }

    private void loadProfile() {

        String url = Constants.BASE_URL +
                "get_driver_profile.php?driver_id=" + driverId;

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);

                        if (!json.getBoolean("success")) return;

                        JSONObject driver = json.getJSONObject("driver");

                        String fullName = driver.optString("nom_profil", "");
                        String image = driver.optString("profile_image", "");
                        int completed = driver.getInt("profile_completed");

                        // 👉 Affichage
                        etFullName.setText(fullName);

                        if (!image.isEmpty() && !image.equals("null")) {
                            String imageUrl = Constants.BASE_URL +
                                    "uploads/profiles/" + image;

                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_user)
                                    .into(imgProfile);
                        }

                        // 🔒 Si profil complété → bloquer
                        if (completed == 1) {
                            etFullName.setEnabled(false);
                            imgProfile.setEnabled(false);
                            btnSubmit.setEnabled(false);

                            Toast.makeText(this,
                                    "Profil déjà complété",
                                    Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {}
        );

        Volley.newRequestQueue(this).add(request);
    }

}