package com.tamojunto.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser

// Menu inferior: as mesmas abas da barra do site (Início, Notas, Filmes, Coisinhas e Mais).
enum class Aba(val rotulo: String, val icone: ImageVector) {
    INICIO("Início", Icons.Filled.Home),
    NOTAS("Notas", Icons.Filled.Edit),
    FILMES("Filmes", Icons.Filled.PlayArrow),
    COISINHAS("Coisinhas", Icons.Filled.Favorite),
    MAIS("Mais", Icons.Filled.Menu)
}

@Composable
fun TelaPrincipal(usuario: FirebaseUser) {
    val dados = remember(usuario.uid) { DadosDoCasal(usuario.uid) }
    DisposableEffect(usuario.uid) {
        dados.iniciar()
        onDispose { dados.parar() }
    }

    LaunchedEffect(usuario.uid) { Servidor.entrar() }

    var aba by rememberSaveable { mutableStateOf(Aba.INICIO) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Aba.entries.forEach { item ->
                    NavigationBarItem(
                        selected = aba == item,
                        onClick = { aba = item },
                        icon = { Icon(item.icone, contentDescription = item.rotulo) },
                        label = { Text(item.rotulo) }
                    )
                }
            }
        }
    ) { espaco ->
        Box(modifier = Modifier.padding(espaco)) {
            when (aba) {
                Aba.INICIO -> TelaInicio(dados, usuario)
                Aba.NOTAS -> TelaNotas()
                Aba.FILMES -> TelaFilmes(dados)
                Aba.COISINHAS -> TelaCoisinhas(dados)
                Aba.MAIS -> TelaMais(dados, usuario)
            }
        }
    }
}

@Composable
fun TelaMais(dados: DadosDoCasal, usuario: FirebaseUser) {
    var emConfiguracoes by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = emConfiguracoes) { emConfiguracoes = false }

    if (emConfiguracoes) {
        TelaConfiguracoes(dados, usuario)
    } else {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Mais", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Missões, calendário e viagens ainda não foram portados para o app.",
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(onClick = { emConfiguracoes = true }, modifier = Modifier.padding(top = 24.dp)) {
                Text("Configurações")
            }
        }
    }
}
