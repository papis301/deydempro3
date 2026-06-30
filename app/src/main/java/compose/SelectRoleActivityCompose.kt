package com.pisco.deydempro3

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pisco.deydempro3.compose.DeydemTheme
import deydemv3.RideSelectActivity

/**
 * 🔥 SelectRoleActivityCompose — version Jetpack Compose de SelectRoleActivity
 *
 * Coexiste avec l'ancienne SelectRoleActivity.java (non supprimée, non modifiée).
 * Reproduit toutes les fonctionnalités d'origine :
 *  - Vérification GPS au lancement, avec dialog d'activation
 *  - Vérification de connexion Internet, avec dialog bloquant + "Réessayer"
 *  - Carte Client → vérifie GPS → RideSelectActivity (deydemv3)
 *  - Carte Chauffeur → vérifie GPS → DriverHomeActivity
 */
class SelectRoleActivityCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DeydemTheme {
                SelectRoleScreen(
                    isGpsEnabled = { isGpsEnabled() },
                    onClientClick = {
                        startActivity(Intent(this, RideSelectActivityCompose::class.java))
                    },
                    onDriverClick = {
                        startActivity(Intent(this, DriverHomeActivity::class.java))
                    },
                    onOpenLocationSettings = {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    isConnected = isConnected()
                )
            }
        }
    }

    private fun isGpsEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false

        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun isConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}

@Composable
fun SelectRoleScreen(
    isGpsEnabled: () -> Boolean = { true },
    onClientClick: () -> Unit = {},
    onDriverClick: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    isConnected: Boolean = true
) {
    val context = LocalContext.current

    // 🔥 Dialog "Pas de connexion" — bloquant, avec bouton Réessayer
    var showNoConnectionDialog by remember { mutableStateOf(!isConnected) }

    // 🔥 Dialog "GPS désactivé" — affiché au lancement si désactivé,
    // et revérifié à chaque tentative de clic sur une carte (comme l'original)
    var showGpsDialog by remember { mutableStateOf(isConnected && !isGpsEnabled()) }

    // 🔥 Revérifie automatiquement le GPS quand l'utilisateur revient sur l'écran
    // (ex: après avoir activé le GPS depuis les paramètres système, puis appui sur Retour)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isGpsEnabled()) {
                    showGpsDialog = false
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 🔥 Reproduit fidèlement isGpsEnabled() + checkGPS() appelés
    // avant chaque navigation dans le code Java original
    val tryNavigate: (() -> Unit) -> Unit = { onSuccess ->
        if (isGpsEnabled()) {
            showGpsDialog = false
            onSuccess()
        } else {
            showGpsDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F8E7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(30.dp))

            // 🔥 LOGO
            Image(
                painter = painterResource(id = R.drawable.logodeydempro),
                contentDescription = "Logo Dey Dem",
                modifier = Modifier.size(130.dp),
                contentScale = ContentScale.Fit
            )

            // 🔥 TITLE
            Text(
                text = "Choisissez votre profil",
                color = Color(0xFF111111),
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                modifier = Modifier.padding(top = 20.dp)
            )

            // 🔥 SUBTITLE
            Text(
                text = "Veuillez sélectionner une option pour continuer",
                color = Color(0xFF777777),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )

            // 🔥 INDICATOR
            Row(
                modifier = Modifier.padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(6.dp)
                        .background(Color(0xFF00C853))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF6EE7B7))
                )
            }

            // 🔥 CARDS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 35.dp)
            ) {
                RoleCard(
                    imageRes = R.drawable.client,
                    iconRes = R.drawable.ic_user,
                    title = "Client",
                    titleColor = Color(0xFF00B248),
                    description = "Réservez un trajet facilement",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    onClick = { tryNavigate(onClientClick) }
                )

                RoleCard(
                    imageRes = R.drawable.chauffeur,
                    iconRes = R.drawable.ic_steering,
                    title = "Chauffeur",
                    titleColor = Color(0xFF009688),
                    description = "Gagnez de l'argent en conduisant",
                    modifier = Modifier.weight(1f),
                    onClick = { tryNavigate(onDriverClick) }
                )
            }

            // 🔥 SECURITY CARD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 35.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_security),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 15.dp)
                ) {
                    Text(
                        text = "Sécurisé  Fiable",
                        color = Color(0xFF111111),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Vos données sont protégées et sécurisées",
                        color = Color(0xFF666666),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_check_green),
                    contentDescription = null,
                    modifier = Modifier.size(35.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // 🔥 DIALOG : PAS DE CONNEXION (bloquant, non annulable)
        if (showNoConnectionDialog) {
            AlertDialog(
                onDismissRequest = { /* non annulable, comme l'original */ },
                title = { Text("Pas de connexion") },
                text = { Text("Veuillez activer Internet") },
                confirmButton = {
                    TextButton(onClick = {
                        // 🔥 équivalent de recreate() : on relance l'Activity
                        (context as? ComponentActivity)?.recreate()
                    }) {
                        Text("Réessayer")
                    }
                }
            )
        }

        // 🔥 DIALOG : GPS DÉSACTIVÉ
        if (showGpsDialog) {
            AlertDialog(
                onDismissRequest = { /* non annulable, comme l'original */ },
                title = { Text("GPS désactivé") },
                text = { Text("Activez le GPS pour continuer") },
                confirmButton = {
                    TextButton(onClick = {
                        onOpenLocationSettings()
                        // Le dialog reste affiché : il se refermera seulement
                        // au prochain clic réussi sur une carte (GPS revérifié)
                    }) {
                        Text("Activer")
                    }
                }
            )
        }
    }
}

@Composable
private fun RoleCard(
    imageRes: Int,
    iconRes: Int,
    title: String,
    titleColor: Color,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )

        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(45.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null
            )
        }

        Text(
            text = title,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 15.dp)
        )

        Text(
            text = description,
            color = Color(0xFF666666),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 25.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00B248), Color(0xFF49D65E))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow),
                contentDescription = "Continuer"
            )
        }
    }
}