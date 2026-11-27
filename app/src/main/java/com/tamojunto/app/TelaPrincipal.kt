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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser

// Menu inferior: as mesmas abas da barra do site (Início, Notas, Filmes, Coisinhas e Mais).
// Os traços dos ícones seguem o estilo dos humores e das estrelas, sem caractere nem emoji.
enum class Aba(val rotulo: String, val caminhos: List<String>, val espessura: Float = 1.25f) {
    // Os mesmos traços do menu do site (header.html): viewBox 24, traço 1.25, pontas arredondadas.
    INICIO("Início", listOf("M3 10l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")),
    NOTAS(
        "Notas",
        listOf("M20.24 12.24a6 6 0 0 0-8.49-8.49L5 10.5V19h8.5z", "M16 8L2 22")
    ),
    FILMES(
        "Filmes",
        listOf("M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20z", "M10 8l6 4-6 4z")
    ),
    COISINHAS(
        "Coisinhas",
        listOf(
            "M8 22h8",
            "M12 15v7",
            "M12 15a7.03 7.03 0 0 0 7-7c0-2-1-4-3-6H8c-2 2-3 4-3 6a7.03 7.03 0 0 0 7 7z"
        )
    ),
    // Três pontos cheios: traço grosso sobre um ponto de zero de comprimento.
    MAIS("Mais", listOf("M5 12h0M12 12h0M19 12h0"), espessura = 3.2f)
}

@Composable
fun TelaPrincipal(usuario: FirebaseUser) {
    val dados = remember(usuario.uid) { DadosDoCasal(usuario.uid) }
    DisposableEffect(usuario.uid) {
        dados.iniciar()
        onDispose { dados.parar() }
    }

    LaunchedEffect(usuario.uid) { Servidor.entrar() }

    val contexto = LocalContext.current
    var nomesCasal by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var escreverCarta by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(usuario.uid, dados.casalId) {
        if (dados.casalId != null) {
            val (inicio, _) = InicioServidorCasal.carregar()
            nomesCasal = inicio?.nomes ?: emptyMap()
        }
    }

    // Os widgets da tela inicial leem uma cópia destes dados: grava de novo sempre que algo muda.
    LaunchedEffect(usuario.uid) {
        snapshotFlow { montarDadosWidget(dados, nomesCasal) }
            .collect { atualizarWidgets(contexto, it) }
    }

    // Um toque no widget de cartinha abre o formulário direto.
    LaunchedEffect(PedidosWidget.cartaPendente) {
        if (PedidosWidget.cartaPendente) {
            PedidosWidget.cartaPendente = false
            escreverCarta = true
        }
    }

    var aba by rememberSaveable { mutableStateOf(Aba.INICIO) }

    // Páginas em tela cheia: a tela desenha atrás das barras do sistema, e só o topo fica abaixo
    // da barra de status. A barra flutua por cima do fim da tela, e as listas reservam espaço para ela.
    val areaGesto = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    CompositionLocalProvider(LocalAlturaDock provides AlturaBarraInferior + areaGesto) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                when (aba) {
                    Aba.INICIO -> TelaInicio(dados, usuario)
                    Aba.NOTAS -> TelaNotas(dados)
                    Aba.FILMES -> TelaFilmes(dados)
                    Aba.COISINHAS -> TelaCoisinhas(dados)
                    Aba.MAIS -> TelaMais(dados, usuario)
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BarraInferior(abas = Aba.entries, atual = aba, aoEscolher = { aba = it })
            }
        }
    }

    if (escreverCarta) {
        val eu = dados.meuSlot ?: "pessoa1"
        val outra = if (eu == "pessoa1") "pessoa2" else "pessoa1"
        DialogoEscreverCartinha(
            nomeOutra = nomesCasal[outra].orEmpty().ifBlank { "Parceiro(a)" },
            aoEnviar = { dados.enviarCartinha(it) },
            aoFechar = { escreverCarta = false }
        )
    }
}

@Composable
fun TelaMais(dados: DadosDoCasal, usuario: FirebaseUser) {
    // Vazio: menu. Senão, o nome da tela aberta a partir do menu.
    var destino by rememberSaveable { mutableStateOf("") }
    val pai = when {
        destino == "amigos" || destino == "grupos" -> "perfil"
        destino.startsWith("grupo:") -> "grupos"
        else -> ""
    }
    BackHandler(enabled = destino.isNotEmpty()) { destino = pai }

    when {
        destino == "config" -> TelaConfiguracoes(dados, usuario)
        destino == "perfil" -> ComVoltar(rotulo = "Mais", aoVoltar = { destino = "" }) {
            TelaPerfil(aoAbrir = { destino = it })
        }
        destino == "amigos" -> ComVoltar(rotulo = "Perfil", aoVoltar = { destino = "perfil" }) { TelaAmigos() }
        destino == "calendario" -> ComVoltar(rotulo = "Mais", aoVoltar = { destino = "" }) { TelaCalendario(dados) }
        destino == "viagens" -> ComVoltar(rotulo = "Mais", aoVoltar = { destino = "" }) { TelaViagens(dados) }
        destino == "missoes" -> ComVoltar(rotulo = "Mais", aoVoltar = { destino = "" }) { TelaMissoes() }
        destino == "ceu" -> ComVoltar(rotulo = "Mais", aoVoltar = { destino = "" }) { TelaCeu() }
        destino == "grupos" -> ComVoltar(rotulo = "Perfil", aoVoltar = { destino = "perfil" }) {
            TelaGrupos(aoAbrir = { destino = "grupo:$it" })
        }
        destino.startsWith("grupo:") -> ComVoltar(rotulo = "Duos e grupos", aoVoltar = { destino = "grupos" }) {
            TelaGrupoDetalhe(gid = destino.removePrefix("grupo:"), aoSair = { destino = "grupos" })
        }
        else -> Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .papelDoSite()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Mais", style = EstiloTituloSecao, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            LinhaMenu(
                caminhos = listOf("M4 21v-7", "M4 10V3", "M12 21v-9", "M12 8V3", "M20 21v-5", "M20 12V3", "M1 14h6", "M9 8h6", "M17 16h6"),
                titulo = "Configurações",
                apoio = "Conta, casal e aparência",
                ativo = true,
                aoClicar = { destino = "config" }
            )
            LinhaMenu(
                caminhos = listOf("M12 4a4 4 0 1 0 0 8 4 4 0 1 0 0-8z", "M4 21a8 8 0 0 1 16 0"),
                titulo = "Perfil",
                apoio = "Seu @, amigos e pedidos",
                ativo = true,
                aoClicar = { destino = "perfil" }
            )
            LinhaMenu(
                caminhos = listOf("M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M16 2v4", "M8 2v4", "M3 10h18", "M12 14.5a1.5 1.5 0 1 0 0 3 1.5 1.5 0 1 0 0-3z"),
                titulo = "Calendário",
                apoio = "Planos, mesversário e datas",
                ativo = true,
                aoClicar = { destino = "calendario" }
            )
            LinhaMenu(
                caminhos = listOf("M3 11l19-9-9 19-2-8-8-2z"),
                titulo = "Viagens",
                apoio = "Roteiros e preparativos",
                ativo = true,
                aoClicar = { destino = "viagens" }
            )
            LinhaMenu(
                caminhos = listOf("M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z", "M4 22v-7"),
                titulo = "Missões",
                apoio = "Missões para fazer juntos e o XP do Laço",
                ativo = true,
                aoClicar = { destino = "missoes" }
            )
            LinhaMenu(
                caminhos = listOf("M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"),
                titulo = "Céu de vocês",
                apoio = "As histórias que viraram estrela",
                ativo = true,
                aoClicar = { destino = "ceu" }
            )
            // Espaço para o último item não ficar embaixo da barra flutuante.
            Spacer(Modifier.height(LocalAlturaDock.current))
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
            IconeLinha(caminhos = listOf("M15 19l-7-7 7-7"), tamanho = 18.dp, cor = Tinta)
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
                IconeLinha(caminhos = listOf("M9 18l6-6-6-6"), tamanho = 18.dp, cor = TintaSuave)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.12f)))
    }
}
