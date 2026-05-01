package deydemv3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pisco.deydempro3.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    TextView tvPhone, tvReferralCode, tvRefCount, tvFreeRides, tvBonus;
    Button btnCopy, btnShare, btnLogout;;

    String referralCode, userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvPhone = findViewById(R.id.tvPhone);
        tvReferralCode = findViewById(R.id.tvReferralCode);
        tvRefCount = findViewById(R.id.tvRefCount);
        tvFreeRides = findViewById(R.id.tvFreeRides);
        tvBonus = findViewById(R.id.tvBonus);
        btnCopy = findViewById(R.id.btnCopy);
        btnShare = findViewById(R.id.btnShare);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> logout());

        SharedPreferences sp = getSharedPreferences("DeydemUser",MODE_PRIVATE);
         userId = sp.getString("user_id","0");

        loadProfile();

        btnCopy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

            android.content.ClipData clip =
                    android.content.ClipData.newPlainText("Code", referralCode);

            clipboard.setPrimaryClip(clip);
            Toast.makeText(this,"Code copié",Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> {
            shareApp(referralCode);
        });

    }

    private void loadProfile(){

        StringRequest request = new StringRequest(Request.Method.POST,
                "https://pisco.alwaysdata.net/get_profile.php",
                response -> {
                    try{
                        JSONObject json = new JSONObject(response);

                        if(json.getBoolean("success")){
                            tvPhone.setText(json.getString("phone"));
                            referralCode = json.getString("referral_code");
                            tvReferralCode.setText(referralCode);

                            tvRefCount.setText("Personnes parrainées : "
                                    + json.getInt("referrals"));

                            tvFreeRides.setText("Courses gratuites : "
                                    + json.getInt("free_rides"));

                            tvBonus.setText("Bonus : "
                                    + json.getInt("bonus") + " FCFA");
                        }

                    }catch(Exception e){
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this,"Erreur réseau",Toast.LENGTH_SHORT).show()
        ){
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("user_id",userId);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void shareApp(String referralCode) {

        String message =
                "🚀 Télécharge Dey Dem et gagne une course gratuite 🎁\n\n" +
                        "👉 Code parrainage : " + referralCode + "\n\n" +
                        "📲 https://play.google.com/store/apps/details?id=com.pisco.deydemv3";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);

        startActivity(Intent.createChooser(intent, "Partager via"));
    }

    private void logout(){

        SharedPreferences.Editor editor =
                getSharedPreferences("DeydemUser", MODE_PRIVATE).edit();

        editor.clear();
        editor.apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivityc.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
    }

}

