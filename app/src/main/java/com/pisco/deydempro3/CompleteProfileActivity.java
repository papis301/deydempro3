package com.pisco.deydempro3;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    /* ================= CONSTANTES ================= */

    private static final int PERMISSION_IMAGE = 201;
    private static final int CAMERA_REQUEST = 900;
    private static final int GALLERY_REQUEST = 901;

    /* ================= DOCUMENT TYPES ================= */

    enum DocType {
        PROFILE("profile_photo"),
        ID_FRONT("id_card_front"),
        ID_BACK("id_card_back"),
        PERMIT_FRONT("permit_front"),
        PERMIT_BACK("permit_back"),
        CG_FRONT("vehicle_card_front"),
        CG_BACK("vehicle_card_back"),
        VEH1("vehicle_photo_1"),
        VEH2("vehicle_photo_2"),
        VEH3("vehicle_photo_3"),
        VEH4("vehicle_photo_4");

        public final String value;
        DocType(String v) { value = v; }
    }

    /* ================= UI ================= */

    private ImageView imgProfile, imgIdFront, imgIdBack,
            imgPermitFront, imgPermitBack,
            imgCgFront, imgCgBack,
            imgVeh1, imgVeh2, imgVeh3, imgVeh4;

    private EditText etFullName;
    private Spinner spVehicleType;
    private Button btnSubmit;

    /* ================= DATA ================= */

    private int driverId;
    private ProgressDialog dialog;

    private Uri cameraImageUri;
    private ImageView currentImageView;
    private DocType currentDocType;

    /* ================= ON CREATE ================= */

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

        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

        /* ===== INIT UI ===== */

        imgProfile = findViewById(R.id.imgProfile);
        imgIdFront = findViewById(R.id.imgIdFront);
        imgIdBack = findViewById(R.id.imgIdBack);
        imgPermitFront = findViewById(R.id.imgPermitFront);
        imgPermitBack = findViewById(R.id.imgPermitBack);
        imgCgFront = findViewById(R.id.imgCgFront);
        imgCgBack = findViewById(R.id.imgCgBack);
        imgVeh1 = findViewById(R.id.imgVeh1);
        imgVeh2 = findViewById(R.id.imgVeh2);
        imgVeh3 = findViewById(R.id.imgVeh3);
        imgVeh4 = findViewById(R.id.imgVeh4);

        etFullName = findViewById(R.id.etFullName);
        spVehicleType = findViewById(R.id.spVehicleType);
        btnSubmit = findViewById(R.id.btnSubmit);

        /* ===== SPINNER ===== */

        String[] vehicles = {"Moto", "Voiture", "Tricycle"};
        spVehicleType.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                vehicles
        ));

        /* ===== IMAGE CLICKS ===== */

        bind(imgProfile, DocType.PROFILE);
        bind(imgIdFront, DocType.ID_FRONT);
        bind(imgIdBack, DocType.ID_BACK);
        bind(imgPermitFront, DocType.PERMIT_FRONT);
        bind(imgPermitBack, DocType.PERMIT_BACK);
        bind(imgCgFront, DocType.CG_FRONT);
        bind(imgCgBack, DocType.CG_BACK);
        bind(imgVeh1, DocType.VEH1);
        bind(imgVeh2, DocType.VEH2);
        bind(imgVeh3, DocType.VEH3);
        bind(imgVeh4, DocType.VEH4);

        btnSubmit.setOnClickListener(v -> submitProfile());
        loadDriverDocuments();

    }

    /* ================= IMAGE HANDLING ================= */

    private void bind(ImageView img, DocType type) {
        img.setOnClickListener(v -> chooseImage(img, type));
    }

    private void chooseImage(ImageView img, DocType type) {
        currentImageView = img;
        currentDocType = type;

        new AlertDialog.Builder(this)
                .setTitle("Choisir une image")
                .setItems(new String[]{"Caméra", "Galerie"}, (d, i) -> {
                    if (i == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    private void openCamera() {
        if (!checkPermission()) return;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "doc_" + System.currentTimeMillis());
        cameraImageUri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST);
    }

    private boolean checkPermission() {
        String perm = Build.VERSION.SDK_INT >= 33 ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, perm)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{perm}, PERMISSION_IMAGE);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        Uri uri = null;

        if (requestCode == CAMERA_REQUEST) uri = cameraImageUri;
        else if (requestCode == GALLERY_REQUEST && data != null) uri = data.getData();

        if (uri == null) return;

        currentImageView.setImageURI(uri);
        uploadDocument(uri, currentDocType.value);
    }

    /* ================= UPLOAD DOCUMENT ================= */

    private void uploadDocument(Uri uri, String documentType) {

        String url = Constants.BASE_URL + "upload_driver_documents.php";

        VolleySingleton request = new VolleySingleton(
                Request.Method.POST,
                url,
                response -> Log.d("UPLOAD", response),
                error -> Log.e("UPLOAD_ERR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("driver_id", String.valueOf(driverId));
                p.put("document_type", documentType);
                return p;
            }

            @Override
            public Map<String, DataPart> getByteData() {
                Map<String, DataPart> map = new HashMap<>();
                try (InputStream is = getContentResolver().openInputStream(uri);
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

                    byte[] data = new byte[4096];
                    int n;
                    while ((n = is.read(data)) != -1) buffer.write(data, 0, n);

                    map.put("file",
                            new DataPart(
                                    documentType + ".jpg",
                                    buffer.toByteArray(),
                                    "image/jpeg"
                            )
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return map;
            }
        };

        VolleySingleton.getInstance(this).add(request);
    }

    /* ================= SUBMIT PROFILE ================= */

    private void submitProfile() {

        String fullName = etFullName.getText().toString().trim();
        if (fullName.isEmpty()) {
            Toast.makeText(this, "Nom obligatoire", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog.setMessage("Enregistrement...");
        dialog.show();

        String url = Constants.BASE_URL + "complete_driver_profile.php";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                r -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Profil enregistré", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MapDeliveriesActivity.class));
                    finish();
                },
                e -> dialog.dismiss()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("driver_id", String.valueOf(driverId));
                p.put("nom_profil", fullName);
                p.put("vehicle_type", spVehicleType.getSelectedItem().toString());
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void loadDriverDocuments() {

        String url = Constants.BASE_URL
                + "get_driver_documents.php?driver_id=" + driverId;

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (!json.getBoolean("success")) return;

                        JSONArray docs = json.getJSONArray("documents");

                        for (int i = 0; i < docs.length(); i++) {
                            JSONObject d = docs.getJSONObject(i);

                            String type = d.getString("document_type");
                            String path = d.getString("file_path");
                            String status = d.getString("status");

                            String fullUrl =  path;

                            switch (type) {

                                case "profile_photo":
                                    showImage(imgProfile, fullUrl);
                                    break;

                                case "id_card_front":
                                    showImage(imgIdFront, fullUrl);
                                    break;

                                case "id_card_back":
                                    showImage(imgIdBack, fullUrl);
                                    break;

                                case "permit_front":
                                    showImage(imgPermitFront, fullUrl);
                                    break;

                                case "permit_back":
                                    showImage(imgPermitBack, fullUrl);
                                    break;

                                case "cg_front":
                                    showImage(imgCgFront, fullUrl);
                                    break;

                                case "cg_back":
                                    showImage(imgCgBack, fullUrl);
                                    break;

                                case "veh1":
                                    showImage(imgVeh1, fullUrl);
                                    break;

                                case "veh2":
                                    showImage(imgVeh2, fullUrl);
                                    break;

                                case "veh3":
                                    showImage(imgVeh3, fullUrl);
                                    break;

                                case "veh4":
                                    showImage(imgVeh4, fullUrl);
                                    break;
                            }
                        }

                    } catch (Exception e) {
                        Log.e("DOC_PARSE", e.getMessage());
                    }
                },
                error -> Log.e("DOC_ERR", error.toString())
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void showImage(ImageView img, String url) {
        img.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(Constants.BASE_URL + "uploads/driver_documents/" + url)
                .placeholder(R.drawable.ic_upload)
                .error(R.drawable.ic_error)
                .into(imgIdFront);

    }

}
