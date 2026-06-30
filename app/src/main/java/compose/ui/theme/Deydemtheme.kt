package com.pisco.deydempro3.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Thème Compose dédié aux nouveaux écrans Jetpack Compose.
 *
 * ⚠️ Ce fichier est totalement indépendant des styles XML existants
 * (res/values/styles.xml, themes.xml). Il ne modifie et n'affecte
 * en rien l'apparence des Activities Java/Views actuelles.
 */

private val DeydemGreen = Color(0xFF00A152)
private val DeydemGreenDark = Color(0xFF00C853)

private val LightColors = lightColorScheme(
    primary = DeydemGreen,
    secondary = DeydemGreenDark,
)

private val DarkColors = darkColorScheme(
    primary = DeydemGreenDark,
    secondary = DeydemGreen,
)

@Composable
fun DeydemTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}