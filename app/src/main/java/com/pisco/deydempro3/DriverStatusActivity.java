package com.pisco.deydempro3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DriverStatusActivity extends AppCompatActivity {

    TextView txtTitle, txtMessage;
    Button btnAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_status);

        txtTitle = findViewById(R.id.txtTitle);
        txtMessage = findViewById(R.id.txtMessage);
        btnAction = findViewById(R.id.btnAction);

        // Données envoyées depuis StartActivitypro / API
        String status = getIntent().getStringExtra("status");
        String reason = getIntent().getStringExtra("reason");

        if ("pending".equals(status)) {
            showPending();
        } else if ("blocked".equals(status)) {
            showBlocked(reason);
        }
    }

    private void showPending() {
        txtTitle.setText("⏳ Validation en cours");
        txtMessage.setText(
                "Votre compte chauffeur est en attente de validation.\n\n" +
                        "Nos équipes vérifient vos documents.\n" +
                        "Vous serez notifié dès que votre compte sera activé."
        );

        btnAction.setText("Actualiser");
        btnAction.setOnClickListener(v -> recreate());
    }

    private void showBlocked(String reason) {
        txtTitle.setText("🚫 Compte bloqué");

        if (reason == null || reason.isEmpty()) {
            reason = "Documents invalides ou non conformes.";
        }

        txtMessage.setText(
                "Votre compte a été bloqué pour la raison suivante :\n\n" +
                        "❗ " + reason + "\n\n" +
                        "Veuillez corriger le problème et contacter l'administration."
        );

        btnAction.setText("Envoyer les documents");
        btnAction.setOnClickListener(v ->
                startActivity(new Intent(this, DriverDocumentsActivity.class))
        );
    }
}
