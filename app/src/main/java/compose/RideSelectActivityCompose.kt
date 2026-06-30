package com.pisco.deydempro3

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.pisco.deydempro3.compose.DeydemTheme
import com.pisco.deydempro3.LoginActivitycCompose
import deydemv3.WaitingDriverActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * 🔥 RideSelectActivity migrée en Jetpack Compose
 *
 * Reproduit TOUTES les fonctionnalités de l'original (deydemv3.RideSelectActivity) :
 *  - Vérification de session (SharedPreferences "DeydemUser"), sinon retour login
 *  - Vérification d'une course active (get_client_active_trip.php) -> WaitingDriverActivity
 *  - Carte Google Maps (via AndroidView/MapView), position utilisateur, cercle bleu
 *  - Activation GPS (SettingsClient + dialog de résolution), permission de localisation
 *  - Géocodage inverse de la position de départ (Geocoder)
 *  - Champs Prise/Dépôt -> Google Places Autocomplete (Sénégal uniquement)
 *  - Tracé automatique de l'itinéraire via l'API OSRM dès que les 2 adresses sont choisies
 *  - Calcul distance/durée + détection de ville (Dakar/Thiès) + tarification dynamique
 *  - Sélection du véhicule (seule la carte "Moto" est visible pour le moment, comme l'original)
 *  - Envoi de la commande (create_ride.php) -> WaitingDriverActivity
 *
 * L'ancienne deydemv3.RideSelectActivity reste intacte et non supprimée.
 */
class RideSelectActivityCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 VÉRIFIER SESSION (identique à l'original)
        val session = getSharedPreferences("DeydemUser", Context.MODE_PRIVATE)
        val isLogged = session.getBoolean("is_logged", false)
        val userId = session.getString("user_id", "0") ?: "0"

        if (!isLogged || userId == "0") {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivitycCompose::class.java))
            finish()
            return
        }

        setContent {
            DeydemTheme {
                RideSelectScreen(
                    userId = userId,
                    onActiveRideFound = { rideId ->
                        val i = Intent(this, WaitingDriverActivity::class.java)
                        i.putExtra("ride_id", rideId)
                        startActivity(i)
                        finish()
                    },
                    onRideCreated = { rideId ->
                        val i = Intent(this, WaitingDriverActivity::class.java)
                        i.putExtra("ride_id", rideId)
                        startActivity(i)
                    }
                )
            }
        }
    }
}

private data class VehiclePrices(
    var moto: Int = 0,
    var particulier: Int = 0,
    var taxi: Int = 0,
    var confort: Int = 0
)

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
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

@Composable
fun RideSelectScreen(
    userId: String,
    onActiveRideFound: (String) -> Unit = {},
    onRideCreated: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapView = rememberMapViewWithLifecycle()

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var pickupLatLng by remember { mutableStateOf<LatLng?>(null) }
    var dropoffLatLng by remember { mutableStateOf<LatLng?>(null) }
    var pickupAddress by remember { mutableStateOf("vous êtes où ?") }
    var dropoffAddress by remember { mutableStateOf("Vous allez où ?") }
    var distanceText by remember { mutableStateOf("Distance 0.0 km") }
    var selectedVehicle by remember { mutableStateOf("MOTO") }
    var prices by remember { mutableStateOf(VehiclePrices()) }
    var distanceValue by remember { mutableStateOf(0.0) }

    // 🔥 LANCEUR : AUTOCOMPLETE PICKUP
    val pickupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            pickupLatLng = place.latLng
            pickupAddress = place.address ?: pickupAddress
            val drop = dropoffLatLng
            val pick = pickupLatLng
            if (pick != null && drop != null) {
                updateRoute(
                    context, scope, googleMap, pick, drop,
                    onDistanceText = { distanceText = it },
                    onDistanceValue = { distanceValue = it },
                    onPrices = { prices = it }
                )
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR && result.data != null) {
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            android.util.Log.e("Places", "Status: " + status.statusMessage)
        }
    }

    // 🔥 LANCEUR : AUTOCOMPLETE DROPOFF
    val dropoffLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            dropoffLatLng = place.latLng
            dropoffAddress = place.address ?: dropoffAddress
            val drop = dropoffLatLng
            val pick = pickupLatLng
            if (pick != null && drop != null) {
                updateRoute(
                    context, scope, googleMap, pick, drop,
                    onDistanceText = { distanceText = it },
                    onDistanceValue = { distanceValue = it },
                    onPrices = { prices = it }
                )
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR && result.data != null) {
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            android.util.Log.e("Places", "Status: " + status.statusMessage)
        }
    }

    fun openAutocomplete(forPickup: Boolean) {
        val fields = listOf(
            Place.Field.ID, Place.Field.NAME,
            Place.Field.ADDRESS, Place.Field.LAT_LNG
        )
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountries(listOf("SN"))
            .build(context)

        if (forPickup) pickupLauncher.launch(intent) else dropoffLauncher.launch(intent)
    }

    // 🔥 LANCEUR : RÉSOLUTION GPS (dialog "Activer le GPS")
    val gpsResolutionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            getUserLocation(context, googleMap) { loc ->
                pickupLatLng = loc
                scope.launch {
                    pickupAddress = reverseGeocode(context, loc) ?: pickupAddress
                }
            }
        }
    }

    // 🔥 LANCEUR : PERMISSION LOCALISATION
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            googleMap?.isMyLocationEnabled = true
            getUserLocation(context, googleMap) { loc ->
                pickupLatLng = loc
                scope.launch {
                    pickupAddress = reverseGeocode(context, loc) ?: pickupAddress
                }
            }
        }
    }

    // 🔥 AU LANCEMENT : vérifier course active + vérifier GPS
    LaunchedEffect(Unit) {
        checkActiveRide(context, userId, onActiveRideFound)
        checkGps(
            onAlreadyEnabled = {
                getUserLocation(context, googleMap) { loc ->
                    pickupLatLng = loc
                    scope.launch {
                        pickupAddress = reverseGeocode(context, loc) ?: pickupAddress
                    }
                }
            },
            onNeedsResolution = { intentSender ->
                gpsResolutionLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            },
            context = context
        )
    }

    fun handleCommande() {
        if (selectedVehicle.isEmpty()) {
            Toast.makeText(context, "Choisissez un véhicule", Toast.LENGTH_SHORT).show()
            return
        }
        val pickup = pickupLatLng
        val dropoff = dropoffLatLng
        if (pickup == null || dropoff == null) {
            Toast.makeText(context, "Choisissez les adresses", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Commande envoyée ($selectedVehicle)", Toast.LENGTH_SHORT).show()

        val price = when (selectedVehicle) {
            "PARTICULIER" -> prices.particulier
            "TAXI" -> prices.taxi
            "CONFORT" -> prices.confort
            else -> prices.moto
        }

        sendRide(
            context = context,
            userId = userId,
            pickupAddress = pickupAddress,
            pickup = pickup,
            dropoffAddress = dropoffAddress,
            dropoff = dropoff,
            price = price,
            distanceValue = distanceValue,
            vehicleType = selectedVehicle.lowercase(),
            onSuccess = onRideCreated
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔥 CARTE + PANNEAU DE RECHERCHE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            ) { mv ->
                mv.getMapAsync { map ->
                    googleMap = map

                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        map.isMyLocationEnabled = true
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }

            // 🔥 PANNEAU DE RECHERCHE (Prise / Dépôt)
            Column(
                modifier = Modifier
                    .padding(top = 40.dp, start = 10.dp, end = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Text(
                    text = distanceText,
                    fontSize = 15.sp,
                    color = Color(0xFF2196F3),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                )

                Row(modifier = Modifier.clickable { openAutocomplete(true) }) {
                    Text(
                        text = "Prise",
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(text = pickupAddress, color = Color(0xFF333333))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(vertical = 8.dp)
                        .background(Color(0xFFEEEEEE))
                )

                Row(modifier = Modifier.clickable { openAutocomplete(false) }) {
                    Text(
                        text = "Dépôt",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(text = dropoffAddress)
                }
            }
        }

        // 🔥 PANNEAU DU BAS : véhicules + commande
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            // 🔥 Seule la carte Moto est visible pour le moment (comme l'original)
            Row(modifier = Modifier.fillMaxWidth()) {
                VehicleCard(
                    iconRes = R.drawable.ic_bike,
                    label = "Moto",
                    price = "${prices.moto} FCFA",
                    selected = selectedVehicle == "MOTO",
                    modifier = Modifier.width(100.dp),
                    onClick = { selectedVehicle = "MOTO" }
                )
            }

            Button(
                onClick = { handleCommande() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text("PASSER COMMANDE")
            }
        }
    }
}

@Composable
private fun VehicleCard(
    iconRes: Int,
    label: String,
    price: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFE8F5E9) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(40.dp)
        )
        Text(text = label, fontSize = 12.sp)
        Text(text = price, fontSize = 12.sp, color = Color(0xFF00C853))
    }
}

// ============================================================
// 🔥 LOGIQUE RÉSEAU / GPS / CALCULS (équivalents des méthodes
// privées de l'Activity Java d'origine)
// ============================================================

private fun checkActiveRide(
    context: Context,
    userId: String,
    onActiveRideFound: (String) -> Unit
) {
    val url = Constants.BASE_URL + "get_client_active_trip.php?client_id=$userId"

    val request = StringRequest(
        Request.Method.GET,
        url,
        { response ->
            try {
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    onActiveRideFound(json.getString("ride_id"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        { error -> android.util.Log.e("ACTIVE_RIDE", error.toString()) }
    )

    Volley.newRequestQueue(context).add(request)
}

private fun checkGps(
    context: Context,
    onAlreadyEnabled: () -> Unit,
    onNeedsResolution: (android.content.IntentSender) -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    val client = LocationServices.getSettingsClient(context)

    client.checkLocationSettings(builder.build())
        .addOnSuccessListener { onAlreadyEnabled() }
        .addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    onNeedsResolution(e.resolution.intentSender)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
}

private fun getUserLocation(
    context: Context,
    map: GoogleMap?,
    onLocation: (LatLng) -> Unit
) {
    if (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)

                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16f))
                map?.addCircle(
                    CircleOptions()
                        .center(userLocation)
                        .radius(100.0)
                        .strokeColor(android.graphics.Color.BLUE)
                        .fillColor(0x220000FF)
                        .strokeWidth(2f)
                )

                onLocation(userLocation)
            }
        }
}

private suspend fun reverseGeocode(context: Context, latLng: LatLng): String? =
    withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

private fun updateRoute(
    context: Context,
    scope: CoroutineScope,
    map: GoogleMap?,
    pickup: LatLng,
    dropoff: LatLng,
    onDistanceText: (String) -> Unit,
    onDistanceValue: (Double) -> Unit,
    onPrices: (VehiclePrices) -> Unit
) {
    map?.clear()

    map?.addMarker(
        MarkerOptions().position(pickup).title("Départ")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
    )
    map?.addMarker(MarkerOptions().position(dropoff).title("Destination"))

    val bounds = LatLngBounds.Builder().include(pickup).include(dropoff).build()
    map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))

    val straightLineKm = calculateDistance(
        pickup.latitude, pickup.longitude, dropoff.latitude, dropoff.longitude
    )
    onDistanceValue(straightLineKm)
    onDistanceText("Distance : %.2f km".format(straightLineKm))

    scope.launch {
        val city = detectCity(context, pickup)
        onPrices(computePrices(city, straightLineKm))

        // 🔥 Tracé OSRM (remplace la distance approximative par la vraie route)
        drawOsrmRoute(pickup, dropoff)?.let { result ->
            withContext(Dispatchers.Main) {
                map?.addPolyline(
                    PolylineOptions().width(10f).color(android.graphics.Color.BLUE)
                        .addAll(result.points)
                )
                onDistanceText("%.2f km • %d min".format(result.distanceKm, result.durationMin))
                onDistanceValue(result.distanceKm)
            }
        }
    }
}

private data class OsrmResult(
    val points: List<LatLng>,
    val distanceKm: Double,
    val durationMin: Int
)

private suspend fun drawOsrmRoute(origin: LatLng, destination: LatLng): OsrmResult? =
    withContext(Dispatchers.IO) {
        try {
            val url = "http://router.project-osrm.org/route/v1/driving/" +
                    "${origin.longitude},${origin.latitude};" +
                    "${destination.longitude},${destination.latitude}" +
                    "?overview=full&geometries=geojson"

            val con = URL(url).openConnection() as HttpURLConnection
            con.requestMethod = "GET"

            val response = BufferedReader(InputStreamReader(con.inputStream)).use { it.readText() }

            val json = JSONObject(response)
            val route = json.getJSONArray("routes").getJSONObject(0)
            val coords: JSONArray = route.getJSONObject("geometry").getJSONArray("coordinates")

            val points = (0 until coords.length()).map { i ->
                val c = coords.getJSONArray(i)
                LatLng(c.getDouble(1), c.getDouble(0))
            }

            val distanceKm = route.getDouble("distance") / 1000.0
            val durationMin = (route.getDouble("duration") / 60).toInt()

            OsrmResult(points, distanceKm, durationMin)
        } catch (e: Exception) {
            android.util.Log.e("ROUTE_ERROR", e.toString())
            null
        }
    }

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    return r * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)))
}

private suspend fun detectCity(context: Context, latLng: LatLng): String =
    withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val city = addresses?.firstOrNull()?.locality?.lowercase()

            when {
                city == null -> "OTHER"
                city.contains("dakar") -> "DAKAR"
                city.contains("thies") || city.contains("thiès") -> "THIES"
                else -> "OTHER"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "OTHER"
        }
    }

private fun roundPrice(price: Int): Int = Math.round(price / 50.0).toInt() * 50

private fun computePrices(city: String, distanceKm: Double): VehiclePrices {
    var baseParticulier = 0.0; var perKmParticulier = 0.0
    var baseTaxi = 0.0; var perKmTaxi = 0.0
    var baseConfort = 0.0; var perKmConfort = 0.0
    var baseMoto = 0.0; var perKmMoto = 0.0

    when (city) {
        "DAKAR" -> {
            baseParticulier = 700.0; perKmParticulier = 300.0
            baseTaxi = 1000.0; perKmTaxi = 350.0
            baseConfort = 900.0; perKmConfort = 350.0
            baseMoto = 500.0; perKmMoto = 175.0
        }
        "THIES" -> {
            baseParticulier = 500.0; perKmParticulier = 100.0
            baseTaxi = 600.0; perKmTaxi = 150.0
            baseConfort = 600.0; perKmConfort = 150.0
            baseMoto = 300.0; perKmMoto = 75.0
        }
    }

    return VehiclePrices(
        moto = roundPrice((baseMoto + distanceKm * perKmMoto).toInt()),
        particulier = roundPrice((baseParticulier + distanceKm * perKmParticulier).toInt()),
        taxi = roundPrice((baseTaxi + distanceKm * perKmTaxi).toInt()),
        confort = roundPrice((baseConfort + distanceKm * perKmConfort).toInt())
    )
}

private fun sendRide(
    context: Context,
    userId: String,
    pickupAddress: String,
    pickup: LatLng,
    dropoffAddress: String,
    dropoff: LatLng,
    price: Int,
    distanceValue: Double,
    vehicleType: String,
    onSuccess: (String) -> Unit
) {
    val url = Constants.BASE_URL + "create_ride.php"

    val request = object : StringRequest(
        Request.Method.POST,
        url,
        { response ->
            try {
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    onSuccess(json.getString("ride_id"))
                } else {
                    Toast.makeText(context, "Erreur serveur", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        { error ->
            Toast.makeText(context, "Erreur réseau ❌", Toast.LENGTH_SHORT).show()
            android.util.Log.e("API_ERROR", error.toString())
        }
    ) {
        override fun getParams(): Map<String, String> {
            return mapOf(
                "client_id" to userId,
                "trip_type" to "taxi",
                "pickup_address" to pickupAddress,
                "pickup_lat" to pickup.latitude.toString(),
                "pickup_lng" to pickup.longitude.toString(),
                "dropoff_address" to dropoffAddress,
                "dropoff_lat" to dropoff.latitude.toString(),
                "dropoff_lng" to dropoff.longitude.toString(),
                "price" to price.toString(),
                "distance_km" to distanceValue.toString(),
                "vehicle_type" to vehicleType
            )
        }
    }

    Volley.newRequestQueue(context).add(request)
}