package com.tamojunto.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser

// Menu inferior: as mesmas abas da barra do site (Início, Notas, Filmes, Coisinhas e Mais).
// Os traços dos ícones seguem o estilo dos humores e das estrelas, sem caractere nem emoji.
enum class Aba(val rotulo: String, val caminhos: List<String>) {
    INICIO(
        "Início",
        listOf("M3 10.5L12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")
    ),
    NOTAS(
        "Notas",
        listOf(
            "M6 3h9l3 3v14a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z",
            "M9 11h6M9 15h6M9 19h3"
        )
    ),
    FILMES(
        "Filmes",
        listOf("M3 6h18v13H3z", "M3 10h18", "M7 6l2 4M12 6l2 4M17 6l2 4")
    ),
    COISINHAS(
        "Coisinhas",
        listOf("M12 20s-7-4.4-7-9.5A3.8 3.8 0 0 1 12 8a3.8 3.8 0 0 1 7 2.5C19 15.6 12 20 12 20z")
    ),
    MAIS(
        "Mais",
        listOf("M4 7h16M4 12h16M4 17h16")
    )
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
        containerColor = Color.Transparent,
        bottomBar = {
            BarraInferior(abas = Aba.entries, atual = aba, aoEscolher = { aba = it })
        }
    ) { espaco ->
        Box(modifier = Modifier.padding(espaco)) {
            when (aba) {
                Aba.INICIO -> TelaInicio(dados, usuario)
                Aba.NOTAS -> TelaNotas(dados)
                Aba.FILMES -> TelaFilmes(dados)
                Aba.COISINHAS -> TelaCoisinhas(dados)
                Aba.MAIS -> TelaMais(dados, usuario)
            }
        }
    }
}

@Composable
fun TelaMais(dados: DadosDoCasal, usuario: FirebaseUser) {
    // Vazio: menu. Senão, o nome da tela aberta a partir do menu.
    var destino by rememberSaveable { mutableStateOf("") }
    // Amigos volta para Perfil; as outras telas voltam para o menu.
    BackHandler(enabled = destino.isNotEmpty()) { destino = if (destino == "amigos") "perfil" else "" }

    when (destino) {
        "config" -> TelaConfiguracoes(dados, usuario)
        "perfil" -> ComVoltar(rotulo = "Mais", aoVoltar = { destino = "" }) {
            TelaPerfil(aoAbrir = { destino = it })
        }
        "amigos" -> ComVoltar(rotulo = "Perfil", aoVoltar = { destino = "perfil" }) { TelaAmigos() }
        "calendario" -> ComVoltar(rotulo = "Mais", aoVoltar = { destino = "" }) { TelaCalendario(dados) }
        "viagens" -> ComVoltar(rotulo = "Mais", aoVoltar = { destino = "" }) { TelaViagens(dados) }
        else -> Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .papelDoSite()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Mais", style = EstiloTituloSecao, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            LinhaMenu(
                caminhos = listOf("M4 7h9M17 7h3M4 17h3M11 17h9", "M14 5v4M7 15v4"),
                titulo = "Configurações",
                apoio = "Conta, casal e aparência",
                ativo = true,
                aoClicar = { destino = "config" }
            )
            LinhaMenu(
                caminhos = listOf("M12 12a4 4 0 1 0 0-.01M4 20c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5"),
                titulo = "Perfil",
                apoio = "Seu @, amigos e pedidos",
                ativo = true,
                aoClicar = { destino = "perfil" }
            )
            LinhaMenu(
                caminhos = listOf("M4 6h16v14H4z", "M4 10h16", "M8 3v4M16 3v4"),
                titulo = "Calendário",
                apoio = "Planos, mesversário e datas",
                ativo = true,
                aoClicar = { destino = "calendario" }
            )
            LinhaMenu(
                caminhos = listOf("M3 12h18", "M12 3c3 3.5 3 14.5 0 18", "M12 3c-3 3.5-3 14.5 0 18"),
                titulo = "Viagens",
                apoio = "Roteiros e preparativos",
                ativo = true,
                aoClicar = { destino = "viagens" }
            )
            LinhaMenu(
                caminhos = listOf("M12 3l2.6 5.3 5.9.9-4.2 4.1 1 5.8L12 16.6 6.7 19.1l1-5.8-4.2-4.1 5.9-.9z"),
                titulo = "Missões",
                apoio = "Em breve no app",
                ativo = false,
                aoClicar = {}
            )
        }
    }
}

// Cabeçalho para voltar à tela anterior (o menu Mais ou o Perfil), acima da tela aberta.
@Composable
private fun ComVoltar(rotulo: String, aoVoltar: () -> Unit, conteudo: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = aoVoltar)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconeLinha(caminhos = listOf("M15 19l-7-7 7-7"), tamanho = 18.dp, cor = Tinta, espessura = 1.5f)
            Text(rotulo, style = EstiloApoio, color = Tinta, modifier = Modifier.padding(start = 6.dp))
        }
        Box(Modifier.weight(1f)) { conteudo() }
    }
}

// Linha do menu Mais: ícone de traço, título em itálico serifado e um apoio pequeno, com seta à direita.
@Composable
private fun LinhaMenu(
    caminhos: List<String>,
    titulo: String,
    apoio: String,
    ativo: Boolean,
    aoClicar: () -> Unit
) {
    val cor = if (ativo) Tinta else TintaSuave
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = ativo, onClick = aoClicar)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconeLinha(caminhos = caminhos, tamanho = 26.dp, cor = cor)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(titulo, style = EstiloApoio, color = cor)
                Text(apoio, style = EstiloRotulo, color = TintaSuave)
            }
            if (ativo) {
                IconeLinha(caminhos = listOf("M9 6l6 6-6 6"), tamanho = 18.dp, cor = TintaSuave)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.12f)))
    }
}
