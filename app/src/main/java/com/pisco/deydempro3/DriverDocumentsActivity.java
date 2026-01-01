package com.pisco.deydempro3;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class DriverDocumentsActivity extends AppCompatActivity {

    Button btnPick, btnSend;
    RecyclerView recycler;

    ArrayList<Uri> images = new ArrayList<>();
    ImageAdapter adapter;

    static final int PICK = 101;

    String driverPhone = "";

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

        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ImageAdapter(this, images);
        recycler.setAdapter(adapter);

        // =====================
        // 📞 Numéro chauffeur
        // =====================
        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        driverPhone = sp.getString("phone", "Inconnu");

        // =====================
        // 🎯 Actions
        // =====================
        btnPick.setOnClickListener(v -> pickImages());
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

    // =====================
    // 📤 Envoi WhatsApp
    // =====================
    private void sendViaWhatsApp(ArrayList<Uri> imageUris, String phone) {

        String adminWhatsApp = "221767741008"; // format international

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
        intent.putExtra("jid", adminWhatsApp + "@s.whatsapp.net");
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);

            // 👉 après WhatsApp → écran attente validation
            redirectToPending();

        } catch (Exception e) {
            Toast.makeText(this,
                    "WhatsApp n'est pas installé",
                    Toast.LENGTH_LONG).show();
        }
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
