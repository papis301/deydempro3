package com.pisco.deydempro3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.pisco.deydempro3.Constants
import com.pisco.deydempro3.R
import com.pisco.deydempro3.RideSelectActivityCompose
import com.pisco.deydempro3.compose.DeydemTheme
import deydemv3.RegisterActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 🔥 LoginActivityc migrée en Jetpack Compose
 *
 * Reproduit TOUTES les fonctionnalités de l'original (deydemv3.LoginActivityc) :
 *  - Champs téléphone + mot de passe (avec bascule afficher/masquer)
 *  - Validation (champs vides, longueur du numéro)
 *  - Appel POST vers login.php (Volley)
 *  - Indicateur de chargement bloquant pendant la requête
 *  - Sauvegarde de la session dans SharedPreferences "DeydemUser"
 *    (user_id, role, mode, phone, nom_profil, profile_image, type_vehicule, is_logged)
 *  - Toast succès/erreur, gestion erreur réseau et erreur JSON
 *  - Bouton "Créer un compte" → RegisterActivity (deydemv3, inchangée)
 *  - Redirection après connexion → RideSelectActivityCompose
 *
 * L'ancienne deydemv3.LoginActivityc reste intacte et non supprimée.
 */
class LoginActivitycCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DeydemTheme {
                LoginScreen(
                    onLoginSuccess = {
                        startActivity(
                            Intent(this, RideSelectActivityCompose::class.java)
                        )
                        finish()
                    },
                    onRegisterClick = {
                        startActivity(
                            Intent(this, RegisterActivity::class.java)
                        )
                    }
                )
            }
        }
    }
}

private const val URL_LOGIN_SUFFIX = "login.php"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onRegisterClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 🔥 IMAGE D'EN-TÊTE (style Uber/Bolt)
            Image(
                painter = painterResource(id = R.drawable.deydem),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentScale = ContentScale.Crop
            )

            // 🔥 CARTE BLANCHE ARRONDIE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Connexion",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = "Connectez-vous pour continuer",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 4.dp)
                )

                // 🔥 CHAMP TÉLÉPHONE
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00C569),
                        focusedLabelColor = Color(0xFF00C569)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )

                // 🔥 CHAMP MOT DE PASSE (avec bascule afficher/masquer)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible)
                                    "Masquer le mot de passe"
                                else
                                    "Afficher le mot de passe"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00C569),
                        focusedLabelColor = Color(0xFF00C569)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                )

                // 🔥 BOUTON SE CONNECTER
                Button(
                    onClick = {
                        loginUser(
                            context = context,
                            phone = phone.trim(),
                            password = password.trim(),
                            onStart = { isLoading = true },
                            onFinished = { isLoading = false },
                            onSuccess = onLoginSuccess
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C569),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp)
                        .height(50.dp)
                ) {
                    Text("Se connecter")
                }

                // 🔥 BOUTON CRÉER UN COMPTE
                OutlinedButton(
                    onClick = onRegisterClick,
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00C569)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(50.dp)
                ) {
                    Text("Créer un compte")
                }
            }
        }

        // 🔥 LOADING BLOQUANT (équivalent du ProgressDialog "Connexion...")
        if (isLoading) {
            AlertDialog(
                onDismissRequest = { /* non annulable, comme l'original */ },
                confirmButton = {},
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF00C569))
                        Text(
                            text = "Connexion...",
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            )
        }
    }
}

/**
 * 🔥 Reproduit fidèlement loginUser() de l'Activity Java d'origine :
 * validation, appel Volley vers login.php, sauvegarde SharedPreferences,
 * gestion des erreurs.
 */
private fun loginUser(
    context: Context,
    phone: String,
    password: String,
    onStart: () -> Unit,
    onFinished: () -> Unit,
    onSuccess: () -> Unit
) {
    // 🔥 VALIDATION
    if (phone.isEmpty() || password.isEmpty()) {
        Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
        return
    }

    // 🔥 VALIDATION NUMÉRO
    if (phone.length < 9) {
        Toast.makeText(context, "Numéro invalide", Toast.LENGTH_SHORT).show()
        return
    }

    onStart()

    val url = Constants.BASE_URL + URL_LOGIN_SUFFIX

    val req = object : StringRequest(
        Request.Method.POST,
        url,
        { response ->
            onFinished()

            try {
                val json = JSONObject(response)
                val success = json.getBoolean("success")

                if (success) {
                    val userObj = json.getJSONObject("user")

                    val userId = userObj.getString("id")
                    val role = userObj.getString("role")
                    val mode = userObj.getString("mode")
                    val phonee = userObj.getString("phone")
                    val nomProfil = userObj.optString("nom_profil", "")
                    val profileImage = userObj.optString("profile_image", "")
                    val typeVehicule = userObj.optString("type_vehicule", "")

                    // 🔥 SAUVEGARDE SESSION
                    val sp = context.getSharedPreferences("DeydemUser", Context.MODE_PRIVATE)
                    sp.edit()
                        .putString("user_id", userId)
                        .putString("role", role)
                        .putString("mode", mode)
                        .putString("phone", phonee)
                        .putString("nom_profil", nomProfil)
                        .putString("profile_image", profileImage)
                        .putString("type_vehicule", typeVehicule)
                        .putBoolean("is_logged", true)
                        .apply()

                    Toast.makeText(context, "Connexion réussie", Toast.LENGTH_SHORT).show()

                    onSuccess()
                } else {
                    val msg = json.getString("message")
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erreur lecture serveur", Toast.LENGTH_SHORT).show()
            }
        },
        { error ->
            onFinished()
            Toast.makeText(context, "Erreur réseau", Toast.LENGTH_LONG).show()
        }
    ) {
        override fun getParams(): Map<String, String> {
            return mapOf(
                "phone" to phone,
                "password" to password
            )
        }

        override fun getHeaders(): Map<String, String> {
            return mapOf("Accept" to "application/json")
        }
    }

    Volley.newRequestQueue(context).add(req)
}