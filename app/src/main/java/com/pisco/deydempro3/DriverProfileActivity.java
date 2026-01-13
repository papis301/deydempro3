package com.pisco.deydempro3;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverProfileActivity extends AppCompatActivity {

    private static final int PICK_PERMIS_REQUEST = 100;
    private static final int PICK_CARTE_REQUEST = 101;
    private static final int PICK_ASSURANCE_REQUEST = 102;
    private static final int PICK_VEHICULE_REQUEST = 103;
    private static final int PICK_PROFILE_REQUEST = 104;

    private TextInputEditText etFullName, etPhone, etEmail, etVehicleType, etPlateNumber;
    private Button btnUpdate, btnLogout;
    private ImageView ivProfile, ivPermis, ivCarte, ivAssurance, ivVehicule;
    private TextView tvPermisStatus, tvCarteStatus, tvAssuranceStatus, tvVehiculeStatus;

    private ProgressDialog progressDialog;
    private String driverId;
    private Uri permisUri, carteUri, assuranceUri, vehiculeUri, profileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile);

        // Récupérer l'ID du chauffeur
        driverId = getSharedPreferences("user", MODE_PRIVATE)
                .getInt("driver_id", 0) + "";

        if (driverId.equals("0")) {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        loadDriverProfile();
        setupClickListeners();
    }

    private void initViews() {
        // Initialiser les vues
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etVehicleType = findViewById(R.id.etVehicleType);
        etPlateNumber = findViewById(R.id.etPlateNumber);

        btnUpdate = findViewById(R.id.btnUpdate);
        btnLogout = findViewById(R.id.btnLogout);

        ivProfile = findViewById(R.id.ivProfile);
        ivPermis = findViewById(R.id.ivPermis);
        ivCarte = findViewById(R.id.ivCarte);
        ivAssurance = findViewById(R.id.ivAssurance);
        ivVehicule = findViewById(R.id.ivVehicule);

        tvPermisStatus = findViewById(R.id.tvPermisStatus);
        tvCarteStatus = findViewById(R.id.tvCarteStatus);
        tvAssuranceStatus = findViewById(R.id.tvAssuranceStatus);
        tvVehiculeStatus = findViewById(R.id.tvVehiculeStatus);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement...");
        progressDialog.setCancelable(false);
    }

    private void setupClickListeners() {
        // Bouton de mise à jour
        btnUpdate.setOnClickListener(v -> updateProfile());

        // Bouton de déconnexion
        btnLogout.setOnClickListener(v -> logout());

        // Image de profil
        ivProfile.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_PROFILE_REQUEST);
            }
        });

        // Permis de conduire
        ivPermis.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_PERMIS_REQUEST);
            }
        });

        // Carte grise
        ivCarte.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_CARTE_REQUEST);
            }
        });

        // Assurance
        ivAssurance.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_ASSURANCE_REQUEST);
            }
        });

        // Photo du véhicule
        ivVehicule.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_VEHICULE_REQUEST);
            }
        });
    }

    private boolean checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PICK_PERMIS_REQUEST);
            return false;
        }
        return true;
    }

    private void loadDriverProfile() {
        progressDialog.show();

        String url = Constants.BASE_URL + "get_driver_profile.php?driver_id=" + driverId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonObject = new JSONObject(response);

                        if (jsonObject.getBoolean("success")) {
                            JSONObject driver = jsonObject.getJSONObject("driver");

                            etFullName.setText(driver.getString("full_name"));
                            etPhone.setText(driver.getString("phone"));
                            etEmail.setText(driver.getString("email"));
                            etVehicleType.setText(driver.getString("vehicle_type"));
                            etPlateNumber.setText(driver.getString("plate_number"));

                            // Charger les images si disponibles
                            String profileImage = driver.getString("profile_image");
                            String permisImage = driver.getString("permis_image");
                            String carteImage = driver.getString("carte_image");
                            String assuranceImage = driver.getString("assurance_image");
                            String vehiculeImage = driver.getString("vehicule_image");

                            // Mettre à jour les statuts
                            updateDocumentStatus(tvPermisStatus, permisImage, "Permis");
                            updateDocumentStatus(tvCarteStatus, carteImage, "Carte Grise");
                            updateDocumentStatus(tvAssuranceStatus, assuranceImage, "Assurance");
                            updateDocumentStatus(tvVehiculeStatus, vehiculeImage, "Véhicule");

                            // Charger les images avec Glide
                            loadImage(profileImage, ivProfile);
                            loadImage(permisImage, ivPermis);
                            loadImage(carteImage, ivCarte);
                            loadImage(assuranceImage, ivAssurance);
                            loadImage(vehiculeImage, ivVehicule);

                        } else {
                            Toast.makeText(this, jsonObject.getString("message"),
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur de parsing", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void updateDocumentStatus(TextView textView, String imageUrl, String docName) {
        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
            textView.setText("✓ " + docName + " vérifié");
            textView.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else {
            textView.setText("✗ " + docName + " manquant");
            textView.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    private void loadImage(String imageUrl, ImageView imageView) {
        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
            String fullUrl = Constants.BASE_URL + "uploads/" + imageUrl;
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_document)
                    .error(R.drawable.ic_document)
                    .into(imageView);
        }
    }

    private void updateProfile() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String vehicleType = etVehicleType.getText().toString().trim();
        String plateNumber = etPlateNumber.getText().toString().trim();

        if (fullName.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Nom et téléphone sont obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Mise à jour...");
        progressDialog.show();

        String url = Constants.BASE_URL + "update_driver_profile.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Toast.makeText(this, jsonObject.getString("message"),
                                Toast.LENGTH_SHORT).show();

                        if (jsonObject.getBoolean("success")) {
                            // Mettre à jour les préférences
                            getSharedPreferences("user", MODE_PRIVATE)
                                    .edit()
                                    .putString("full_name", fullName)
                                    .putString("phone", phone)
                                    .apply();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("driver_id", driverId);
                params.put("full_name", fullName);
                params.put("phone", phone);
                params.put("email", email);
                params.put("vehicle_type", vehicleType);
                params.put("plate_number", plateNumber);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void uploadDocument(int requestCode, Uri imageUri, String documentType) {
        if (imageUri == null) return;

        progressDialog.setMessage("Upload en cours...");
        progressDialog.show();

        String url = Constants.BASE_URL + "upload_document.php";

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(
                Request.Method.POST, url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Toast.makeText(this, jsonObject.getString("message"),
                                Toast.LENGTH_SHORT).show();

                        if (jsonObject.getBoolean("success")) {
                            // Recharger le profil
                            loadDriverProfile();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Erreur upload", Toast.LENGTH_SHORT).show();
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("driver_id", driverId);
                params.put("document_type", documentType);
                return params;
            }

            @Override
            public Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();

                try {
                    // Convertir URI en byte array
                    byte[] imageData = getBytesFromUri(imageUri);
                    String fileName = documentType + "_" + driverId + ".jpg";

                    params.put("image", new DataPart(fileName, imageData, "image/jpeg"));

                } catch (IOException e) {
                    e.printStackTrace();
                }

                return params;
            }
        };

        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private byte[] getBytesFromUri(Uri uri) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileInputStream inputStream = new FileInputStream(new File(uri.getPath()));

        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        return outputStream.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            switch (requestCode) {
                case PICK_PROFILE_REQUEST:
                    profileUri = imageUri;
                    ivProfile.setImageURI(imageUri);
                    uploadDocument(requestCode, imageUri, "profile");
                    break;

                case PICK_PERMIS_REQUEST:
                    permisUri = imageUri;
                    ivPermis.setImageURI(imageUri);
                    uploadDocument(requestCode, imageUri, "permis");
                    break;

                case PICK_CARTE_REQUEST:
                    carteUri = imageUri;
                    ivCarte.setImageURI(imageUri);
                    uploadDocument(requestCode, imageUri, "carte");
                    break;

                case PICK_ASSURANCE_REQUEST:
                    assuranceUri = imageUri;
                    ivAssurance.setImageURI(imageUri);
                    uploadDocument(requestCode, imageUri, "assurance");
                    break;

                case PICK_VEHICULE_REQUEST:
                    vehiculeUri = imageUri;
                    ivVehicule.setImageURI(imageUri);
                    uploadDocument(requestCode, imageUri, "vehicule");
                    break;
            }
        }
    }

    private void logout() {
        // Effacer les préférences
        getSharedPreferences("user", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Rediriger vers Login
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PICK_PERMIS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée
            } else {
                Toast.makeText(this, "Permission nécessaire pour accéder aux images",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}