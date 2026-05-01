package deydemv3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.pisco.deydempro3.R;

public class SupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        findViewById(R.id.btnWhatsapp).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/221770000000"));
            startActivity(intent);
        });

//        findViewById(R.id.btnEmail).setOnClickListener(v -> {
//            Intent intent = new Intent(Intent.ACTION_SENDTO);
//            intent.setData(Uri.parse("mailto:support@deydem.com"));
//            intent.putExtra(Intent.EXTRA_SUBJECT, "Support client");
//            startActivity(intent);
//        });

//        findViewById(R.id.btnFaq).setOnClickListener(v ->
//                Toast.makeText(this, "FAQ bientôt disponible", Toast.LENGTH_SHORT).show()
//        );
    }
}
