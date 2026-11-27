package com.tamojunto.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        PedidosWidget.lerIntent(intent)
        // Desenha atrás das barras do sistema, com ícones escuros (o fundo do app é claro).
        // Color aqui é o do Android (android.graphics), que tem TRANSPARENT; o do Compose não tem.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            TamoJuntoTema {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    RaizDoApp()
                }
            }
        }
    }

    // Com singleTop, o app já aberto recebe o pedido do widget por aqui.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        PedidosWidget.lerIntent(intent)
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
