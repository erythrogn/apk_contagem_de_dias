package com.tamojunto.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

// Primeira tela de dados: lê o perfil do usuário no mesmo Realtime Database que o site usa.
@Composable
fun TelaInicio(usuario: FirebaseUser) {
    var carregando by remember { mutableStateOf(true) }
    var nome by remember { mutableStateOf<String?>(null) }
    var temCasal by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(usuario.uid) {
        FirebaseDatabase.getInstance().getReference("usuarios/${usuario.uid}").get()
            .addOnSuccessListener { perfil ->
                nome = perfil.child("nome").getValue(String::class.java) ?: usuario.displayName
                temCasal = perfil.child("casal_id").exists()
                carregando = false
            }
            .addOnFailureListener {
                erro = "Não consegui carregar seus dados."
                carregando = false
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            carregando -> CircularProgressIndicator()
            erro != null -> Text(erro ?: "")
            else -> {
                Text(
                    text = "Olá, ${nome ?: "de novo"}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (temCasal) "Seu casal já está configurado." else "Você ainda não tem um casal.",
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
            }
        }
        Button(onClick = { FirebaseAuth.getInstance().signOut() }) {
            Text("Sair")
        }
    }
}
