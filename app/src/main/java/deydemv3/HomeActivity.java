package deydemv3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pisco.deydempro3.R;
import com.pisco.deydempro3.StartActivitypro;

public class HomeActivity extends AppCompatActivity {
    private ModeManager modeManager;
    private UserManager userManager;

    private Button btnSwitchcli, btnSwitchchauf;
    private TextView txtMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        modeManager = new ModeManager(this);
        userManager = new UserManager(this);

        btnSwitchcli = findViewById(R.id.btnClient);
        btnSwitchchauf = findViewById(R.id.btnChauffeur);
        txtMode = findViewById(R.id.txtMode);

        updateUI();

        btnSwitchcli.setOnClickListener(v -> {
                startActivity(new Intent(this, StartActivity.class));

        });

        btnSwitchchauf.setOnClickListener(v -> {
                startActivity(new Intent(this, StartActivitypro.class));
        });
    }

    private void updateUI() {
        txtMode.setText("Mode actuel : " + modeManager.getMode());
    }
}