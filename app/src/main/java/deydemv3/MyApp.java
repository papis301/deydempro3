package deydemv3;

import android.app.Application;

import com.google.android.libraries.places.api.Places;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBAmGv7nVuVuYx8Zmph6DmBH1SxIIa9UAM");
        }
    }
}
