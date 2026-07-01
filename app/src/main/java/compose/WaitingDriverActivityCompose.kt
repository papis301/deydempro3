package com.pisco.deydempro3

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.pisco.deydempro3.compose.DeydemTheme
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * 🔥 WaitingDriverActivityCompose — version Jetpack Compose de deydemv3.WaitingDriverActivity
 *
 * Coexiste avec l'ancienne deydemv3.WaitingDriverActivity (non supprimée, non modifiée).
 *
 * Reproduit TOUTES les fonctionnalités de l'original :
 *  - Carte Google Maps : position utilisateur, marker chauffeur animé (icône car_top)
 *  - Polling check_driver.php (3s puis 5s) — chauffeur trouvé ?
 *  - Polling check_trip_status.php (5s) — course annulée ou terminée ?
 *  - Chargement du profil chauffeur (get_driver_profile.php) : nom, véhicule, position
 *  - Deux états UI : "Recherche en cours..." / "Chauffeur trouvé"
 *  - Bouton Annuler (dialog confirmation) → cancel_ride.php → RideSelectActivityCompose
 *  - Bouton Appeler le chauffeur (Intent.ACTION_DIAL)
 *  - Dialog "Course annulée par le chauffeur" (retour à l'état "Recherche")
 *  - Redirection automatique quand status = "completed"
 */
class WaitingDriverActivityCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rideId = intent.getStringExtra("ride_id") ?: ""

        Toast.makeText(this, "Commande envoyée 🚀 ID: $rideId", Toast.LENGTH_LONG).show()

        setContent {
            DeydemTheme {
                WaitingDriverScreen(
                    rideId = rideId,
                    onRideCancelled = {
                        startActivity(
                            Intent(this, RideSelectActivityCompose::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    },
                    onRideCompleted = {
                        Toast.makeText(this, "Course terminée ✅", Toast.LENGTH_LONG).show()
                        finish()
                    }
                )
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────
// 🔥 STATE
// ───────────────────────────────────────────────────────────────────

private data class DriverInfo(
    val name: String = "",
    val vehicle: String = "",
    val phone: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

private sealed class WaitingState {
    object Searching : WaitingState()
    data class DriverFound(val info: DriverInfo) : WaitingState()
}

// ───────────────────────────────────────────────────────────────────
// 🔥 COMPOSABLE PRINCIPAL
// ───────────────────────────────────────────────────────────────────

@Composable
fun WaitingDriverScreen(
    rideId: String,
    onRideCancelled: () -> Unit = {},
    onRideCompleted: () -> Unit = {}
) {
    val context = LocalContext.current

    var waitingState by remember { mutableStateOf<WaitingState>(WaitingState.Searching) }
    var driverFound by remember { mutableStateOf(false) }
    var driverUIShown by remember { mutableStateOf(false) }
    var driverId by remember { mutableStateOf("") }
    var driverMarker by remember { mutableStateOf<Marker?>(null) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var tripCancelledDialogShown by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showDriverCancelledDialog by remember { mutableStateOf(false) }

    val mapView = rememberWaitingMapViewWithLifecycle()

    // 🔥 INITIALISATION CARTE : position utilisateur
    LaunchedEffect(googleMap) {
        val map = googleMap ?: return@LaunchedEffect
        initUserLocation(context, map)
    }

    // 🔥 POLLING : check_driver.php (3s puis toutes les 5s)
    LaunchedEffect(rideId) {
        delay(3000L)
        while (true) {
            checkDriverStatus(
                context = context,
                rideId = rideId,
                onDriverFound = { id, phone ->
                    if (!driverFound) {
                        driverFound = true
                        driverId = id
                        Toast.makeText(context, "Chauffeur trouvé 🚗", Toast.LENGTH_SHORT).show()
                    }
                    // 🔥 Charger le profil à chaque poll pour mettre à jour la position
                    loadDriverProfile(
                        context = context,
                        driverId = id,
                        onProfileLoaded = { info ->
                            val newState = WaitingState.DriverFound(info)
                            waitingState = newState
                            if (!driverUIShown) {
                                driverUIShown = true
                            }
                            // 🔥 Mettre à jour le marker chauffeur
                            updateDriverMarker(
                                context = context,
                                map = googleMap,
                                lat = info.lat,
                                lng = info.lng,
                                existingMarker = driverMarker,
                                onMarkerUpdated = { driverMarker = it },
                                animateCamera = !driverUIShown
                            )
                        }
                    )
                }
            )
            delay(5000L)
        }
    }

    // 🔥 POLLING : check_trip_status.php (toutes les 5s)
    LaunchedEffect(rideId) {
        delay(5000L)
        while (true) {
            checkTripStatus(
                context = context,
                rideId = rideId,
                onCancelledByDriver = {
                    if (driverFound && !tripCancelledDialogShown) {
                        tripCancelledDialogShown = true
                        // 🔥 Reset état chauffeur
                        driverFound = false
                        driverUIShown = false
                        driverId = ""
                        driverMarker?.remove()
                        driverMarker = null
                        waitingState = WaitingState.Searching
                        showDriverCancelledDialog = true
                    }
                },
                onCompleted = onRideCompleted
            )
            delay(5000L)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🔥 UI
    // ──────────────────────────────────────────────────────────────

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔥 CARTE — prend tout l'espace disponible au-dessus du panneau
        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            ) { mv ->
                mv.getMapAsync { map ->
                    googleMap = map
                }
            }
        }

        // 🔥 PANNEAU BAS — directement sous la carte, sans vide
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
        ) {
            when (val state = waitingState) {

                // ──── ÉTAT : RECHERCHE EN COURS ────
                is WaitingState.Searching -> {
                    SearchingPanel(
                        onCancelClick = { showCancelConfirmDialog = true }
                    )
                }

                // ──── ÉTAT : CHAUFFEUR TROUVÉ ────
                is WaitingState.DriverFound -> {
                    DriverFoundPanel(
                        info = state.info,
                        onCancelClick = { showCancelConfirmDialog = true },
                        onCallClick = {
                            if (state.info.phone.isEmpty()) {
                                Toast.makeText(
                                    context, "Numéro indisponible", Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:${state.info.phone}")
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // 🔥 DIALOG : Confirmation annulation par le client
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Annuler la course") },
            text = { Text("Voulez-vous vraiment annuler ?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirmDialog = false
                    cancelRide(context, rideId, onRideCancelled)
                }) { Text("Oui", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Non")
                }
            }
        )
    }

    // 🔥 DIALOG : Annulation par le chauffeur
    if (showDriverCancelledDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Course annulée") },
            text = { Text("Le chauffeur a annulé la course.") },
            confirmButton = {
                TextButton(onClick = {
                    showDriverCancelledDialog = false
                    tripCancelledDialogShown = false
                }) { Text("OK") }
            }
        )
    }
}

// ───────────────────────────────────────────────────────────────────
// 🔥 SOUS-COMPOSABLES
// ───────────────────────────────────────────────────────────────────

@Composable
private fun SearchingPanel(onCancelClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Recherche d'un chauffeur...",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF111111)
        )

        Spacer(modifier = Modifier.height(12.dp))

        CircularProgressIndicator(
            color = Color(0xFF00C853),
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Veuillez patienter...",
            fontSize = 13.sp,
            color = Color(0xFF777777),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancelClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            Text("❌ Annuler la recherche")
        }
    }
}

@Composable
private fun DriverFoundPanel(
    info: DriverInfo,
    onCancelClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // Entête
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar initiale
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00C853)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = info.name.firstOrNull()?.uppercase() ?: "C",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🚗 ${info.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF111111)
                )
                Text(
                    text = "${info.vehicle} • ${info.phone}",
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
                Text(
                    text = "Arrive dans 2 min",
                    fontSize = 12.sp,
                    color = Color(0xFF00C853),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCallClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("📞 Appeler", color = Color.White)
            }

            OutlinedButton(
                onClick = onCancelClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                modifier = Modifier.weight(1f)
            ) {
                Text("❌ Annuler")
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────
// 🔥 MAPVIEW LIFECYCLE (même pattern que RideSelectActivityCompose)
// ───────────────────────────────────────────────────────────────────

@Composable
private fun rememberWaitingMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { id = android.view.View.generateViewId() }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return mapView
}

// ───────────────────────────────────────────────────────────────────
// 🔥 LOGIQUE RÉSEAU / GPS
// ───────────────────────────────────────────────────────────────────

private fun initUserLocation(context: Context, map: GoogleMap) {
    if (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    map.isMyLocationEnabled = true

    LocationServices.getFusedLocationProviderClient(context)
        .lastLocation
        .addOnSuccessListener { location ->
            location ?: return@addOnSuccessListener
            val userLatLng = LatLng(location.latitude, location.longitude)
            map.addMarker(MarkerOptions().position(userLatLng).title("Ma position"))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))
        }
}

private fun checkDriverStatus(
    context: Context,
    rideId: String,
    onDriverFound: (driverId: String, phone: String) -> Unit
) {
    val url = Constants.BASE_URL + "check_driver.php?ride_id=$rideId"

    val request = StringRequest(
        Request.Method.GET, url,
        { response ->
            try {
                val json = JSONObject(response)
                if (json.getBoolean("found")) {
                    onDriverFound(
                        json.getString("driver_id"),
                        json.getString("phone")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        { error ->
            Log.e("CHECK_DRIVER", error.toString())
        }
    )

    Volley.newRequestQueue(context).add(request)
}

private fun loadDriverProfile(
    context: Context,
    driverId: String,
    onProfileLoaded: (DriverInfo) -> Unit
) {
    val url = Constants.BASE_URL + "get_driver_profile.php?driver_id=$driverId"

    val request = StringRequest(
        Request.Method.GET, url,
        { response ->
            try {
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    val driver = json.getJSONObject("driver")
                    onProfileLoaded(
                        DriverInfo(
                            name = driver.getString("name"),
                            vehicle = driver.getString("vehicle_type"),
                            phone = driver.optString("phone", ""),
                            lat = driver.getDouble("last_lat"),
                            lng = driver.getDouble("last_lng")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erreur profil chauffeur", Toast.LENGTH_SHORT).show()
            }
        },
        { error -> Log.e("DRIVER_PROFILE", error.toString()) }
    )

    Volley.newRequestQueue(context).add(request)
}

private fun updateDriverMarker(
    context: Context,
    map: GoogleMap?,
    lat: Double,
    lng: Double,
    existingMarker: Marker?,
    onMarkerUpdated: (Marker) -> Unit,
    animateCamera: Boolean
) {
    map ?: return
    val driverPos = LatLng(lat, lng)

    if (existingMarker == null) {
        val icon = resizeMapIcon(context, R.drawable.car_top, 90, 90)
        val marker = map.addMarker(
            MarkerOptions()
                .position(driverPos)
                .title("Chauffeur")
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(icon)
        )
        marker?.let { onMarkerUpdated(it) }
    } else {
        existingMarker.position = driverPos
    }

    if (animateCamera) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(driverPos, 15f))
    }
}

private fun resizeMapIcon(context: Context, iconResId: Int, width: Int, height: Int) =
    BitmapDescriptorFactory.fromBitmap(
        Bitmap.createScaledBitmap(
            BitmapFactory.decodeResource(context.resources, iconResId),
            width, height, false
        )
    )

private fun checkTripStatus(
    context: Context,
    rideId: String,
    onCancelledByDriver: () -> Unit,
    onCompleted: () -> Unit
) {
    val url = Constants.BASE_URL + "check_trip_status.php?ride_id=$rideId"

    val request = StringRequest(
        Request.Method.GET, url,
        { response ->
            try {
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    when (json.getString("status")) {
                        "pending" -> onCancelledByDriver()
                        "completed" -> onCompleted()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        { error -> Log.e("TRIP_STATUS", error.toString()) }
    )

    Volley.newRequestQueue(context).add(request)
}

private fun cancelRide(
    context: Context,
    rideId: String,
    onSuccess: () -> Unit
) {
    val url = Constants.BASE_URL + "cancel_ride.php"

    val request = object : StringRequest(
        Method.POST, url,
        { response ->
            Log.d("CANCEL_RESPONSE", response)
            try {
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    Toast.makeText(context, "Course annulée ✅", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, json.getString("message"), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erreur parsing JSON", Toast.LENGTH_SHORT).show()
            }
        },
        { error ->
            Log.e("CANCEL_ERROR", error.toString())
            Toast.makeText(context, "Erreur réseau ❌", Toast.LENGTH_SHORT).show()
        }
    ) {
        override fun getParams() = mapOf("ride_id" to rideId)
    }

    Volley.newRequestQueue(context).add(request)
}