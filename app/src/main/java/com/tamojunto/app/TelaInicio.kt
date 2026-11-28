package com.tamojunto.app

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.min

// Início: a mesma página do site, com a contagem, o Laço, o clima entre vocês e as cartinhas.
// Seletor de espaço (casal ou grupo) no topo, e o conteúdo do espaço escolhido.
@Composable
fun TelaInicio(dados: DadosDoCasal, usuario: FirebaseUser) {
    var gidEscolhido by rememberSaveable { mutableStateOf<String?>(null) }
    var grupos by remember { mutableStateOf<List<GrupoResumo>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            grupos = InicioServidor.carregarGrupos()
            delay(60_000)
        }
    }
    // Sem casal, o primeiro grupo abre sozinho.
    val gidAtivo = gidEscolhido ?: if (dados.casalId == null) grupos.firstOrNull()?.gid else null

    Column(Modifier.fillMaxSize()) {
        if (dados.casalId != null || grupos.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dados.casalId != null) {
                    FiltroSite(texto = "Casal", ativo = gidAtivo == null, aoClicar = { gidEscolhido = null })
                }
                grupos.forEach { grupo ->
                    FiltroSite(
                        texto = grupo.nome.ifBlank { "Grupo" },
                        ativo = gidAtivo == grupo.gid,
                        aoClicar = { gidEscolhido = grupo.gid }
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize().weight(1f)) {
            if (gidAtivo == null) {
                InicioDoCasal(dados, usuario)
            } else {
                TelaInicioGrupo(gidAtivo)
            }
        }
    }
}

@Composable
private fun InicioDoCasal(dados: DadosDoCasal, usuario: FirebaseUser) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val clienteLocal = remember(contexto) { LocationServices.getFusedLocationProviderClient(contexto) }
    val pedidoLocal = remember {
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
            .setMinUpdateDistanceMeters(50f)
            .build()
    }
    var inicio by remember { mutableStateOf<InicioApp?>(null) }
    var erroInicio by remember { mutableStateOf("") }
    var agora by remember { mutableStateOf(LocalDateTime.now(FUSO_BELEM)) }
    var unidade by rememberSaveable { mutableStateOf("dias") }
    var lendo by remember { mutableStateOf<Cartinha?>(null) }
    var escrevendo by rememberSaveable { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    var pegandoLocal by remember { mutableStateOf(false) }
    var visor by remember { mutableStateOf<FotoCasal?>(null) }
    var apagarFoto by remember { mutableStateOf<FotoCasal?>(null) }
    var mapaAberto by rememberSaveable { mutableStateOf(false) }
    var enviandoFoto by remember { mutableStateOf(false) }
    var avisoFoto by remember { mutableStateOf("") }

    val eu = inicio?.eu?.ifBlank { null } ?: dados.meuSlot ?: "pessoa1"
    val outra = if (eu == "pessoa1") "pessoa2" else "pessoa1"
    val compartilhando = dados.localizacoes[eu]?.ativo == true

    fun atualizarInicio() {
        escopo.launch {
            val (novos, erro) = InicioServidorCasal.carregar()
            if (novos != null) {
                inicio = novos
                erroInicio = ""
            } else {
                erroInicio = erro
            }
        }
    }

    // Relógio: a contagem anda a cada segundo, como no site.
    LaunchedEffect(Unit) {
        while (true) {
            agora = LocalDateTime.now(FUSO_BELEM)
            delay(1000)
        }
    }
    // Dados que o servidor calcula: ao entrar e a cada minuto.
    LaunchedEffect(dados.casalId) {
        while (dados.casalId != null) {
            atualizarInicio()
            delay(60_000)
        }
    }

    // Compartilhamento contínuo: enquanto estiver ligado, o aparelho manda a posição quando anda 50 m
    // (ou a cada minuto), como o site. Ao desligar, a posição é retirada do ar.
    DisposableEffect(compartilhando) {
        if (!compartilhando) {
            onDispose { }
        } else {
            val retorno = object : LocationCallback() {
                override fun onLocationResult(resultado: LocationResult) {
                    resultado.lastLocation?.let { dados.atualizarLocalizacao(it.latitude, it.longitude) }
                }
            }
            try {
                clienteLocal.requestLocationUpdates(pedidoLocal, retorno, Looper.getMainLooper())
            } catch (erro: SecurityException) {
                aviso = "Sem permissão de localização. Libere no aparelho para compartilhar."
            }
            onDispose {
                try {
                    clienteLocal.removeLocationUpdates(retorno)
                } catch (erro: SecurityException) {
                    // Sem permissão, nada estava sendo enviado.
                }
            }
        }
    }

    val pedirLocal = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes.values.any { it }) {
            escopo.launch {
                pegandoLocal = true
                val local = lerLocalizacao(contexto)
                pegandoLocal = false
                if (local != null) {
                    dados.atualizarLocalizacao(local.latitude, local.longitude)
                    aviso = ""
                } else {
                    aviso = "Não consegui achar sua posição agora. Verifique se o GPS está ligado."
                }
            }
        } else {
            aviso = "Sem permissão de localização. Libere no aparelho para compartilhar."
        }
    }

    val pegarFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            escopo.launch {
                enviandoFoto = true
                val arquivo = lerFoto(contexto, uri)
                avisoFoto = if (arquivo == null) {
                    "Não consegui ler esta foto."
                } else {
                    val resposta = Servidor.enviarFoto("/api/config/fotos", arquivo.first, arquivo.second)
                    if (resposta.ok) "" else resposta.msg
                }
                enviandoFoto = false
                atualizarInicio()
            }
        }
    }

    if (dados.carregando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (dados.casalId == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Você ainda não tem um casal. Criar ou entrar em um casal ainda não está no app.",
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val inicioIso = dados.dataNamoro ?: inicio?.dataIso
    val contagem = inicioIso?.let { calcularContagem(it, agora) }
    if (contagem == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Falta a data do namoro. Defina no site, em Configurações.", textAlign = TextAlign.Center)
        }
        return
    }

    val nomeEu = inicio?.nomes?.get(eu).orEmpty().ifBlank { "Você" }
    val nomeOutra = inicio?.nomes?.get(outra).orEmpty().ifBlank { "Parceiro(a)" }
    val hojeIso = LocalDate.now(FUSO_BELEM).toString()
    val proximas = (dados.filmes + dados.series + dados.jogos + dados.coisinhas + dados.viagens + dados.planos)
        .filter { !it.feito && it.dataIso.isNotBlank() && it.dataIso >= hojeIso }
        .sortedBy { it.dataIso }
        .take(3)
    val locEu = dados.localizacoes[eu]?.takeIf { it.ativo }
    val locOutra = dados.localizacoes[outra]?.takeIf { it.ativo }

    // Pergunta do dia: o cartão mostra a de hoje e a folha deixa responder.
    var perguntas by remember { mutableStateOf<PerguntasApp?>(null) }
    var erroPerguntas by remember { mutableStateOf("") }
    var abrirPerguntas by rememberSaveable { mutableStateOf(false) }
    val escopoPerguntas = rememberCoroutineScope()
    fun atualizarPerguntas() {
        escopoPerguntas.launch {
            val (app, msg) = PerguntasServidor.carregar()
            if (app != null) perguntas = app
            erroPerguntas = if (app == null) msg else ""
        }
    }
    LaunchedEffect(dados.casalId) { atualizarPerguntas() }

    // Ordem e cartões escondidos da página: ao vivo, para os dois verem a mesma página.
    var prefInicio by remember { mutableStateOf(PREF_INICIO_PADRAO) }
    var organizando by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(dados.casalId) {
        val casal = dados.casalId
        if (casal == null) {
            onDispose { }
        } else {
            val parar = PreferenciasApp.observarInicio("casais/$casal") { prefInicio = it }
            onDispose { parar() }
        }
    }
    if (organizando && dados.casalId != null) {
        OrganizarInicioFolha(
            pref = prefInicio,
            aoMudar = { novo ->
                prefInicio = novo
                PreferenciasApp.salvarInicio("casais/${dados.casalId}", novo)
            },
            aoFechar = { organizando = false }
        )
    }
    if (abrirPerguntas) {
        FolhaPerguntasDia(
            aoFechar = { abrirPerguntas = false; atualizarPerguntas() },
            aoMudar = { atualizarPerguntas() }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = LocalAlturaDock.current),
        verticalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        item {
            CabecalhoContagem(
                contagem = contagem,
                // Aqui a contagem já existe, então inicioIso não é nulo; o let evita o erro de tipo.
                linhaDeData = "desde " + (inicio?.dataExtenso?.ifBlank { null } ?: inicioIso?.let(::dataBr).orEmpty()),
                unidade = unidade,
                aoMudar = { unidade = it }
            )
        }
        if (erroInicio.isNotBlank() && inicio == null) {
            item { Text(erroInicio, style = MaterialTheme.typography.bodyMedium) }
        }
        if (dados.casalId != null) {
            item {
                BotaoSite(
                    texto = "Organizar a página",
                    aoClicar = { organizando = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Cada cartão na ordem e com a visibilidade que o casal escolheu.
        prefInicio.ordem.filter { it !in prefInicio.ocultos }.forEach { chave ->
            when (chave) {
                "hoje" -> inicio?.hoje?.let { hoje -> item { CartaoHoje(hoje) } }
                "perguntas" -> item {
                    CartaoPerguntaHoje(hoje = perguntas?.hoje, erro = erroPerguntas, aoAbrir = { abrirPerguntas = true })
                }
                "proximas" -> item { CartaoProximas(proximas) }
                "fotos" -> item {
                    CartaoFotos(
                        fotos = inicio?.fotos.orEmpty(),
                        enviando = enviandoFoto,
                        aviso = avisoFoto,
                        aoAdicionar = {
                            pegarFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        aoAbrir = { visor = it }
                    )
                }
                "laco" -> inicio?.laco?.let { laco -> item { CartaoLaco(laco) } }
                "clima" -> item {
                    CartaoClima(
                        dados = dados,
                        eu = eu,
                        outra = outra,
                        nomeEu = nomeEu,
                        nomeOutra = nomeOutra,
                        aviso = aviso,
                        pegandoLocal = pegandoLocal,
                        compartilhando = compartilhando,
                        aoAlternarCompartilhar = { ligar ->
                            if (ligar) {
                                pedirLocal.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            } else {
                                dados.pararLocalizacao()
                            }
                        },
                        podeMapa = locEu != null && locOutra != null,
                        aoVerMapa = { mapaAberto = true },
                        aoLer = { lendo = it },
                        aoEscrever = { escrevendo = true }
                    )
                }
            }
        }
    }

    lendo?.let { carta ->
        DialogoLerCartinha(
            carta = carta,
            nomeDe = if (carta.de == eu) nomeEu else nomeOutra,
            aoFechar = {
                dados.lerCartinha(carta.chave)
                lendo = null
            }
        )
    }
    if (escrevendo) {
        DialogoEscreverCartinha(
            nomeOutra = nomeOutra,
            aoEnviar = { dados.enviarCartinha(it) },
            aoFechar = { escrevendo = false }
        )
    }
    visor?.let { foto ->
        VisorFoto(
            foto = foto,
            aoApagar = { apagarFoto = foto },
            aoFechar = { visor = null }
        )
    }
    apagarFoto?.let { foto ->
        DialogoConfirmar(
            titulo = "Apagar esta foto?",
            texto = "Ela some da página inicial de vocês.",
            rotuloConfirmar = "Apagar",
            aoConfirmar = {
                escopo.launch {
                    Servidor.apagar("/api/config/fotos/${foto.nome}")
                    visor = null
                    atualizarInicio()
                }
            },
            aoFechar = { apagarFoto = null }
        )
    }
    if (mapaAberto && locEu != null && locOutra != null) {
        DialogoMapa(
            eu = locEu,
            outra = locOutra,
            nomeEu = nomeEu,
            nomeOutra = nomeOutra,
            aoFechar = { mapaAberto = false }
        )
    }
}

// Número grande, unidade, composição e relógio: o topo da página do site.
@Composable
internal fun CabecalhoContagem(contagem: Contagem, linhaDeData: String, unidade: String, aoMudar: (String) -> Unit) {
    val (numero, palavra) = numeroDaUnidade(contagem, unidade)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            linhaDeData,
            style = EstiloRotulo,
            color = TintaSuave,
            textAlign = TextAlign.Center
        )
        Text("tamo junto há", style = EstiloApoio, modifier = Modifier.padding(top = 16.dp))
        Text("$numero", style = EstiloNumero, color = Tinta, textAlign = TextAlign.Center)
        Text(palavra, style = EstiloApoio, textAlign = TextAlign.Center)
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            listOf("dias" to "dias", "meses" to "meses", "anos" to "anos").forEach { (chave, nome) ->
                BotaoUnidade(texto = nome, ativo = unidade == chave, aoClicar = { aoMudar(chave) })
            }
        }
        Text(
            contagem.composicao,
            style = EstiloApoio,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
        // Relógio: divisórias finas entre as medidas, como no site.
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.Center) {
            MedidaDoTempo(contagem.horas, "horas", separador = false)
            MedidaDoTempo(contagem.minutos, "minutos", separador = true)
            MedidaDoTempo(contagem.segundos, "segundos", separador = true)
        }
    }
}

// Unidade escolhida: texto em itálico com um filete dourado embaixo quando ativa.
@Composable
private fun BotaoUnidade(texto: String, ativo: Boolean, aoClicar: () -> Unit) {
    Column(
        Modifier.clickable(onClick = aoClicar).padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(texto, style = EstiloApoio, color = if (ativo) Tinta else TintaSuave)
        Box(
            Modifier
                .padding(top = 2.dp)
                .width(32.dp)
                .height(1.dp)
                .background(if (ativo) Ouro else Color.Transparent)
        )
    }
}

@Composable
private fun MedidaDoTempo(valor: Long, rotulo: String, separador: Boolean) {
    Column(
        Modifier
            .drawBehind {
                if (separador) {
                    drawLine(
                        color = Tinta.copy(alpha = 0.18f),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor.toString().padStart(2, '0'), style = EstiloRelogio)
        Text(rotulo, style = EstiloRotulo, color = TintaSuave)
    }
}

// Seção da página: filete dourado em degradê, título em itálico e o conteúdo, sem caixa.
@Composable
internal fun CartaoSecao(titulo: String, subtitulo: String? = null, conteudo: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Ouro, Color.Transparent)))
        )
        Text(
            titulo,
            style = EstiloTituloSecao,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        if (subtitulo != null) {
            Text(
                subtitulo,
                style = EstiloRotulo,
                color = TintaSuave,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
        Column(Modifier.fillMaxWidth().padding(top = 16.dp)) { conteudo() }
    }
}

@Composable
internal fun CartaoHoje(hoje: ResumoHoje) {
    val pede = hoje.esperandoVoce
    val missao = hoje.missao
    CartaoSecao(
        titulo = "Hoje",
        subtitulo = if (pede != null) "Uma coisa pede a sua atenção" else "Nada pede a sua atenção agora"
    ) {
        if (pede != null) {
            Text(pede.titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                "${pede.quem} Falta a sua confirmação para o XP entrar no Laço.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (missao != null) {
            Text(
                missao.titulo,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = if (pede != null) 12.dp else 0.dp)
            )
            Text(
                "Missão do dia · +${missao.xp} XP · ${missao.expiraTexto}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else if (pede == null) {
            Text("Amanhã tem uma nova missão do dia.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// "hoje", "amanhã" ou "em N dias", com o dia por extenso.
private fun quandoTexto(iso: String): String {
    val alvo = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    val dias = ChronoUnit.DAYS.between(LocalDate.now(FUSO_BELEM), alvo)
    val quando = when (dias) {
        0L -> "hoje"
        1L -> "amanhã"
        else -> "em $dias dias"
    }
    return "$quando · ${alvo.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR")))}"
}

@Composable
private fun CartaoProximas(proximas: List<Item>) {
    CartaoSecao("Próximas datas", "Os próximos três compromissos") {
        if (proximas.isEmpty()) {
            Text("Nenhuma data marcada ainda.", style = MaterialTheme.typography.bodyMedium)
        } else {
            proximas.forEach { ev ->
                Column(Modifier.padding(bottom = 10.dp)) {
                    Text(ev.titulo.ifBlank { "Sem título" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        quandoTexto(ev.dataIso),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoFotos(
    fotos: List<FotoCasal>,
    enviando: Boolean,
    aviso: String,
    aoAdicionar: () -> Unit,
    aoAbrir: (FotoCasal) -> Unit
) {
    CartaoSecao("Fotos de vocês", "Toque em uma foto para ver em tela cheia.") {
        if (fotos.isEmpty()) {
            Text("Nenhuma foto ainda.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(fotos, key = { it.nome }) { foto ->
                    AsyncImage(
                        model = foto.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 220.dp, height = 280.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { aoAbrir(foto) }
                    )
                }
            }
        }
        BotaoSite(
            texto = if (enviando) "Enviando foto…" else "Adicionar foto",
            aoClicar = aoAdicionar,
            habilitado = !enviando,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        if (aviso.isNotBlank()) {
            Text(aviso, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

// Foto em tela cheia, com apagar (como no site).
@Composable
private fun VisorFoto(foto: FotoCasal, aoApagar: () -> Unit, aoFechar: () -> Unit) {
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = foto.url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                TextButton(onClick = aoApagar) { Text("Apagar", color = Color.White) }
                TextButton(onClick = aoFechar) { Text("Fechar", color = Color.White) }
            }
        }
    }
}

@Composable
internal fun CartaoLaco(laco: LacoApp) {
    var aberto by rememberSaveable { mutableStateOf(false) }
    val rotuloFase = if (laco.prestigio) "${laco.fase} · Prestígio" else laco.fase
    CartaoSecao("Laço", "Nível ${laco.nivel} de ${laco.total}") {
        Text(rotuloFase, style = MaterialTheme.typography.titleMedium)
        Text(
            laco.significado,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp)
        )
        LinearProgressIndicator(
            progress = { laco.percentual / 100f },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${formatarMilhar(laco.xp)} XP", style = MaterialTheme.typography.bodySmall)
            val proximo = laco.proximoNivel
            Text(
                if (proximo != null) "faltam ${formatarMilhar(laco.falta)} para o nível $proximo" else "continua acumulando",
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = { aberto = !aberto }) {
            Text(if (aberto) "Esconder as regras" else "Como ganhar XP")
        }
        if (aberto) {
            laco.regras.forEach { grupo ->
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        "${grupo.categoria} · até ${grupo.limite} XP por dia",
                        style = MaterialTheme.typography.labelLarge
                    )
                    grupo.atividades.forEach { atividade ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(atividade.rotulo, style = MaterialTheme.typography.bodySmall)
                            Text("${atividade.xp} XP", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (laco.limitesHoje.isNotEmpty()) {
                Text(
                    "Hoje: " + laco.limitesHoje.joinToString(" · ") { "${it.categoria} ${it.usado}/${it.limite}" },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CartaoClima(
    dados: DadosDoCasal,
    eu: String,
    outra: String,
    nomeEu: String,
    nomeOutra: String,
    aviso: String,
    pegandoLocal: Boolean,
    compartilhando: Boolean,
    aoAlternarCompartilhar: (Boolean) -> Unit,
    podeMapa: Boolean,
    aoVerMapa: () -> Unit,
    aoLer: (Cartinha) -> Unit,
    aoEscrever: () -> Unit
) {
    val humorEu = dados.humores[eu]
    val humorOutra = dados.humores[outra]
    val locEu = dados.localizacoes[eu]?.takeIf { it.ativo }
    val locOutra = dados.localizacoes[outra]?.takeIf { it.ativo }
    val km = if (locEu != null && locOutra != null) distanciaKm(locEu.lat, locEu.lng, locOutra.lat, locOutra.lng) else null
    val recebidas = dados.cartinhas.filter { it.para == eu }
    val enviadas = dados.cartinhas.filter { it.de == eu }

    CartaoSecao("Como está o tempo entre vocês") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PessoaComHumor(nomeEu, humorEu)
            PessoaComHumor(nomeOutra, humorOutra)
        }

        Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (km != null) textoDistancia(km) else "—",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            val texto = when {
                locEu != null && locOutra != null && km != null -> {
                    val situacao = if (km < 0.05) "Vocês estão no mesmo lugar" else "Vocês estão separados"
                    "$situacao · atualizado ${tempoAtras(min(locEu.em, locOutra.em))}"
                }
                locEu == null -> "Ligue o compartilhamento para ver a distância entre vocês."
                else -> "$nomeOutra ainda não compartilhou a localização."
            }
            Text(
                texto,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Compartilhar minha localização", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = compartilhando,
                    onCheckedChange = aoAlternarCompartilhar,
                    enabled = !pegandoLocal,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (pegandoLocal) {
                Text("Lendo sua posição…", style = MaterialTheme.typography.bodySmall)
            }
            BotaoSite(texto = "Ver no mapa", aoClicar = aoVerMapa, habilitado = podeMapa, modifier = Modifier.padding(top = 8.dp))
            if (aviso.isNotBlank()) {
                Text(aviso, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }
        }

        Text(
            "Como você está hoje?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 18.dp)
        )
        OPCOES_HUMOR.chunked(2).forEach { linha ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linha.forEach { opcao ->
                    FiltroSite(
                        texto = opcao.rotulo,
                        ativo = humorEu?.id == opcao.id,
                        aoClicar = { dados.definirHumor(opcao.id) },
                        modifier = Modifier.weight(1f),
                        icone = { IconeHumor(opcao, tamanho = 18.dp) }
                    )
                }
            }
        }

        Text(
            "Para você",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 18.dp)
        )
        if (recebidas.isEmpty()) {
            Text(
                "Nenhuma cartinha de $nomeOutra ainda.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            recebidas.forEach { carta ->
                BotaoSite(
                    texto = rotuloComTempo("Cartinha de $nomeOutra", carta.em),
                    aoClicar = { aoLer(carta) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }
        if (enviadas.isNotEmpty()) {
            Text(
                "Enviadas (ainda não lidas)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
            enviadas.forEach { carta ->
                Text(
                    "Para $nomeOutra · ${rotuloComTempo(carta.texto.take(60), carta.em)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        BotaoSite(
            texto = "Escrever cartinha",
            aoClicar = aoEscrever,
            primario = true,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
    }
}

@Composable
private fun PessoaComHumor(nome: String, humor: Humor?) {
    val opcao = opcaoDeHumor(humor?.id)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(nome, style = EstiloApoio, color = Tinta)
        Box(Modifier.padding(vertical = 8.dp).height(44.dp), contentAlignment = Alignment.Center) {
            if (opcao != null) {
                IconeHumor(opcao, tamanho = 44.dp, cor = Tinta)
            } else {
                Text("—", style = EstiloApoio, color = TintaSuave)
            }
        }
        Text(
            if (opcao != null && humor != null) rotuloComTempo(opcao.rotulo, humor.em) else "Ainda não escolheu",
            style = EstiloRotulo,
            color = TintaSuave,
            textAlign = TextAlign.Center
        )
    }
}

// Mapa com os dois pontos e a linha entre eles (Leaflet e OpenStreetMap, como no site).
@Composable
private fun DialogoMapa(eu: Localizacao, outra: Localizacao, nomeEu: String, nomeOutra: String, aoFechar: () -> Unit) {
    val km = distanciaKm(eu.lat, eu.lng, outra.lat, outra.lng)
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().papelDoSite().padding(horizontal = 30.dp, vertical = 36.dp)) {
            Text("Vocês no mapa", style = EstiloTituloSecao, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            MapaNativo(
                pontos = listOf(PontoMapa(nomeEu, eu.lat, eu.lng), PontoMapa(nomeOutra, outra.lat, outra.lng)),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 16.dp)
            )
            Text(
                "Distância em linha reta: ${textoDistancia(km)}. Só o casal vê esta tela.",
                style = EstiloApoio,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            )
            TextButton(onClick = aoFechar, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Fechar") }
        }
    }
}

@Composable
private fun DialogoLerCartinha(carta: Cartinha, nomeDe: String, aoFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Cartinha de $nomeDe") },
        text = {
            Column {
                Text(carta.texto, style = MaterialTheme.typography.bodyLarge)
                Text(
                    tempoAtras(carta.em),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar e apagar") } }
    )
}

@Composable
internal fun DialogoEscreverCartinha(nomeOutra: String, aoEnviar: (String) -> Unit, aoFechar: () -> Unit) {
    var texto by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Para $nomeOutra") },
        text = {
            Column {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { if (it.length <= 500) texto = it },
                    placeholder = { Text("Um recado curto…") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${texto.length} de 500 letras",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = texto.isNotBlank(),
                onClick = {
                    aoEnviar(texto.trim())
                    aoFechar()
                }
            ) { Text("Enviar cartinha") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

// Uma leitura da posição, usada ao ligar o compartilhamento.
internal suspend fun lerLocalizacao(contexto: Context): Location? = try {
    LocationServices.getFusedLocationProviderClient(contexto)
        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .await()
} catch (erro: Exception) {
    null
}
