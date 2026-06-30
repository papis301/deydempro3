package com.pisco.deydempro3

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pisco.deydempro3.compose.DeydemTheme
import kotlinx.coroutines.delay

/**
 * 🔥 SplashActivity migrée en Jetpack Compose
 *
 * Identique visuellement à l'original (dégradé vert, logo centré, texte blanc),
 * avec en plus une animation de fondu + échelle au démarrage.
 *
 * Le manifest reste inchangé (MAIN / LAUNCHER pointe toujours sur .SplashActivity).
 */
@SuppressLint("CustomSplashScreen")
class SplashActivityCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DeydemTheme {
                SplashScreen(
                    onFinished = {
                        startActivity(Intent(this, SelectRoleActivityCompose::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // État de l'animation : démarre à false, passe à true au lancement
    var visible by remember { mutableStateOf(false) }

    // Animation de l'opacité : 0 → 1 en 700ms
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    // Animation d'échelle : 0.7 → 1 en 700ms (effet de "pop" du logo)
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // Dégradé identique à splash_bg.xml : #00C853 → #00695C à 135°
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00C853),
            Color(0xFF00695C)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)   // ~135°
    )

    // Lancement : déclenche l'animation dès le premier frame,
    // puis attend 2 secondes avant de passer à l'écran suivant
    LaunchedEffect(Unit) {
        visible = true
        delay(2000L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logodeydempro),
                contentDescription = "Logo Dey Dem",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Nom de l'app
            Text(
                text = "DEY DEM",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slogan
            Text(
                text = "Votre trajet, notre priorité",
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}