package com.tamojunto.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun TelaLogin() {
    val contexto = LocalContext.current
    var entrando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    // O id do cliente web vem do google-services.json (gerado pelo plugin do Google).
    val clienteGoogle = remember {
        GoogleSignIn.getClient(
            contexto,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(contexto.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }

    val lancador = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        try {
            val conta = GoogleSignIn.getSignedInAccountFromIntent(resultado.data)
                .getResult(ApiException::class.java)
            val credencial = GoogleAuthProvider.getCredential(conta.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credencial)
                .addOnCompleteListener { tarefa ->
                    entrando = false
                    if (!tarefa.isSuccessful) erro = "Não consegui entrar. Tente de novo."
                }
        } catch (e: ApiException) {
            entrando = false
            erro = "Entrada cancelada ou sem conexão."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "tamo junto", style = MaterialTheme.typography.headlineLarge)
        Text(text = "Entre com a sua conta Google.", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        Button(
            enabled = !entrando,
            onClick = {
                entrando = true
                erro = null
                lancador.launch(clienteGoogle.signInIntent)
            }
        ) {
            Text("Entrar com Google")
        }
        erro?.let {
            Text(text = it, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
