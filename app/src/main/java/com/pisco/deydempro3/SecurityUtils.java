package com.pisco.deydempro3;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.pisco.deydempro3.DriverBlockedActivity;
import com.pisco.deydempro3.VolleySingleton;
import com.pisco.deydempro3.Constants;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SecurityUtils {

    public static void checkDriverSecurity(
            Activity activity,
            int driverId,
            double lat,
            double lng,
            Runnable onAllowed
    ) {

        String url = Constants.BASE_URL + "save_delivery_position.php";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                response -> {

                    try {
                        JSONObject obj = new JSONObject(response);

                        // 🚨 CHAUFFEUR BLOQUÉ
                        if (!obj.getBoolean("success")
                                && obj.optBoolean("blocked")) {

                            String message = obj.optString(
                                    "message",
                                    "Votre compte est bloqué"
                            );

                            Intent i = new Intent(
                                    activity,
                                    DriverBlockedActivity.class
                            );
                            i.putExtra("message", message);

                            activity.startActivity(i);
                            activity.finish();
                            return;
                        }

                        // ✅ OK → continuer l’activité
                        onAllowed.run();

                    } catch (Exception e) {
                        Log.e("SECURITY", e.getMessage());
                    }
                },
                error -> Log.e("SECURITY_ERR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("driver_id", String.valueOf(driverId));
                p.put("delivery_id", "0"); // ou cours actuel
                p.put("status", "security_check");
                p.put("lat", String.valueOf(lat));
                p.put("lng", String.valueOf(lng));
                return p;
            }
        };

        VolleySingleton.getInstance(activity).addToRequestQueue(req);
    }
}

