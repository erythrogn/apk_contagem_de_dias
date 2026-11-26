package com.tamojunto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TamoJuntoTema {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    RaizDoApp()
                }
            }
        }
    }
}

// Mostra o login enquanto ninguém está entrado e a tela inicial depois.
@Composable
fun RaizDoApp() {
    var usuario by remember { mutableStateOf<FirebaseUser?>(FirebaseAuth.getInstance().currentUser) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val ouvinte = FirebaseAuth.AuthStateListener { usuario = it.currentUser }
        auth.addAuthStateListener(ouvinte)
        onDispose { auth.removeAuthStateListener(ouvinte) }
    }

    val atual = usuario
    if (atual == null) {
        TelaLogin()
    } else {
        TelaPrincipal(atual)
    }
}
