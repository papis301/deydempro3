package com.pisco.deydempro3;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DriverDocumentsActivity extends AppCompatActivity {

    Button btnPick, btnSend, btnRefresh;
    RecyclerView recycler;

    ArrayList<Uri> images = new ArrayList<>();
    ImageAdapter adapter;

    static final int PICK = 101;
    int driverId;
    String driverPhone = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_driver_documents);

        // =====================
        // 🔗 UI
        // =====================
        btnPick = findViewById(R.id.btnPick);
        btnSend = findViewById(R.id.btnSend);
        recycler = findViewById(R.id.recycler);
        btnRefresh = findViewById(R.id.btnRefresh);

        SharedPreferences userSp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
         driverId = userSp.getInt("driver_id", 0);

        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ImageAdapter(this, images);
        recycler.setAdapter(adapter);

        // =====================
        // 📞 Numéro chauffeur
        // =====================
        SharedPreferences sp = getSharedPreferences("DeydemUser", MODE_PRIVATE);
        driverPhone = sp.getString("phone", "Inconnu");

        // =====================
        // 🎯 Actions
        // =====================
        btnPick.setOnClickListener(v -> pickImages());
        btnRefresh.setOnClickListener(v -> checkDocsStatus(driverId));
        btnSend.setOnClickListener(v -> {
            if (images.isEmpty()) {
                Toast.makeText(this,
                        "Veuillez ajouter les documents",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            sendViaWhatsApp(images, driverPhone);
        });
    }

    // =====================
    // 📸 Sélection images
    // =====================
    private void pickImages() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, PICK);
    }

    @Override
    protected void onActivityResult(int r, int c, @Nullable Intent d) {
        super.onActivityResult(r, c, d);

        if (r == PICK && c == RESULT_OK && d != null) {

            images.clear();

            if (d.getClipData() != null) {
                for (int i = 0; i < d.getClipData().getItemCount(); i++) {
                    images.add(d.getClipData().getItemAt(i).getUri());
                }
            } else if (d.getData() != null) {
                images.add(d.getData());
            }

            adapter.notifyDataSetChanged();
        }
    }

    private void checkDocsStatus(int driverId) {

        String url = "https://pisco.alwaysdata.net/check_docs_status.php?driver_id=" + driverId;

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);

                        if (!obj.getBoolean("success")) {
                            Toast.makeText(this,
                                    "Erreur serveur",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String status = obj.getString("docs_status");

                        if ("approved".equals(status)) {

                            Toast.makeText(this,
                                    "✅ Compte approuvé",
                                    Toast.LENGTH_SHORT).show();

                            // 👉 redirection vers les courses
                            startActivity(new Intent(
                                    this,
                                    MapDeliveriesActivity.class
                            ));
                            finish();

                        } else if ("rejected".equals(status)) {
                            Toast.makeText(this,
                                    "❌ Documents rejetés",
                                    Toast.LENGTH_LONG).show();

                        } else {
                            Toast.makeText(this,
                                    "⏳ Toujours en attente de validation",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this,
                        "Erreur réseau",
                        Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(req);




    }


    // =====================
    // 📤 Envoi WhatsApp
    // =====================
    private void sendViaWhatsApp(ArrayList<Uri> imageUris, String phone) {

        String message =
                "Bonjour,\n\n" +
                        "Voici mes documents chauffeur DeyDem.\n\n" +
                        "📞 Numéro d’inscription : " + phone + "\n\n" +
                        "Documents envoyés :\n" +
                        "- Permis de conduire\n" +
                        "- Carte d’identité\n" +
                        "- Carte grise\n" +
                        "- Photos du véhicule\n\n" +
                        "Merci.";

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
        intent.putExtra(Intent.EXTRA_TEXT, message);

        // ✅ permission URIs
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // ✅ forcer WhatsApp
        intent.setPackage("com.whatsapp");

        try {

            markDocsAsSent(driverId);
            startActivity(intent);

            // 🔁 retour app après WhatsApp
            //redirectToPending();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "WhatsApp n'est pas installé",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void markDocsAsSent(int driverId) {

        String url = "https://pisco.alwaysdata.net/mark_docs_sent.php";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    // OK silencieux
                },
                error -> {
                    // log seulement
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("driver_id", String.valueOf(driverId));
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);




    }



    // =====================
    // ⏳ Attente validation
    // =====================
    private void redirectToPending() {
        startActivity(new Intent(this, DriverStatusActivity.class));
        finish();
    }

    // ======================================================
    // ❌ ENVOI PAR EMAIL (DÉSACTIVÉ / EN COMMENTAIRE)
    // ======================================================
    /*
    private void sendEmail() {

        if (images.isEmpty()) {
            Toast.makeText(this, "Aucun document", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent email = new Intent(Intent.ACTION_SEND_MULTIPLE);
        email.setType("message/rfc822");
        email.putExtra(Intent.EXTRA_EMAIL,
                new String[]{"admin@deydem.com"});
        email.putExtra(Intent.EXTRA_SUBJECT,
                "Documents chauffeur - Validation");
        email.putExtra(Intent.EXTRA_TEXT,
                "Merci de valider mes documents.");

        email.putParcelableArrayListExtra(
                Intent.EXTRA_STREAM, images);

        startActivity(Intent.createChooser(email, "Envoyer"));
    }
    */
}
