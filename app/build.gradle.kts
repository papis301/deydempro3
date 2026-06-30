plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pisco.deydempro3"
    compileSdk {
        version = release(36)
    }

    // 🔥 Force core-ktx à une version compatible AGP 8.x / compileSdk 36
    configurations.all {
        resolutionStrategy {
            force("androidx.core:core-ktx:1.16.0")
            force("androidx.core:core:1.16.0")
        }
    }

    defaultConfig {
        applicationId = "com.pisco.deydempro3"
        minSdk = 24
        targetSdk = 36
        versionCode = 12
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    implementation("org.osmdroid:osmdroid-android:6.1.14")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.maps.android:android-maps-utils:3.4.0")
    // Glide pour charger les images
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // Optionnel: pour les transformations d'image
    implementation ("jp.wasabeef:glide-transformations:4.3.0")

    // Si vous voulez charger depuis URL HTTPS
    implementation ("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.libraries.places:places:3.4.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("com.google.firebase:firebase-bom:33.1.0")

    // 🔥 Jetpack Compose (nouveau, n'affecte pas le code Java/Views existant)
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation(libs.ui.test.manifest)

}