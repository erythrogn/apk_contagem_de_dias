package com.tamojunto.app

import android.Manifest
import android.graphics.Bitmap
import android.os.Looper
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.Locale

// Início de um grupo: a mesma página do casal, com as coisas do grupo (como grupo_inicio.js).
@Composable
fun TelaInicioGrupo(gid: String) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val clienteLocal = remember(contexto) { LocationServices.getFusedLocationProviderClient(contexto) }
    val pedidoLocal = remember {
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
            .setMinUpdateDistanceMeters(50f)
            .build()
    }
    var grupo by remember(gid) { mutableStateOf<GrupoInicio?>(null) }
    var erro by remember(gid) { mutableStateOf("") }
    var agora by remember { mutableStateOf(LocalDateTime.now(FUSO_BELEM)) }
    var unidade by rememberSaveable { mutableStateOf("dias") }
    var visor by remember { mutableStateOf<FotoGrupo?>(null) }
    var apagarFoto by remember { mutableStateOf<FotoGrupo?>(null) }
    var mapaAberto by rememberSaveable { mutableStateOf(false) }
    var lendo by remember { mutableStateOf<CartinhaGrupo?>(null) }
    var escrevendo by rememberSaveable { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    var avisoFoto by remember { mutableStateOf("") }
    var pegandoLocal by remember { mutableStateOf(false) }
    var enviandoFoto by remember { mutableStateOf(false) }
    val estadoAtual = rememberUpdatedState(grupo)

    fun atualizar() {
        escopo.launch {
            val (novo, msg) = InicioServidor.carregarGrupo(gid)
            if (novo != null) {
                grupo = novo
                erro = ""
            } else {
                erro = msg
            }
        }
    }

    // Atualiza a cada 20 s, como o site.
    LaunchedEffect(gid) {
        while (true) {
            atualizar()
            delay(20_000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            agora = LocalDateTime.now(FUSO_BELEM)
            delay(1000)
        }
    }

    // Ao sair do Início, as fotos novas são marcadas como vistas (o site faz o mesmo).
    DisposableEffect(gid) {
        onDispose {
            val fids = estadoAtual.value?.fotos?.filter { !it.vista }?.map { it.fid }.orEmpty()
            if (fids.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    Servidor.postJson(
                        "/api/grupos/foto/vista",
                        JSONObject().put("gid", gid).put("fids", JSONArray(fids))
                    )
                }
            }
        }
    }

    val compartilhando = grupo?.euCompartilho == true
    DisposableEffect(compartilhando, gid) {
        if (!compartilhando) {
            onDispose { }
        } else {
            val retorno = object : LocationCallback() {
                override fun onLocationResult(resultado: LocationResult) {
                    resultado.lastLocation?.let { local ->
                        escopo.launch {
                            Servidor.postJson(
                                "/api/grupos/localizacao",
                                JSONObject().put("gid", gid).put("lat", local.latitude).put("lng", local.longitude)
                            )
                        }
                    }
                }
            }
            try {
                clienteLocal.requestLocationUpdates(pedidoLocal, retorno, Looper.getMainLooper())
            } catch (falha: SecurityException) {
                aviso = "Sem permissão de localização. Libere no aparelho para compartilhar."
            }
            onDispose {
                try {
                    clienteLocal.removeLocationUpdates(retorno)
                } catch (falha: SecurityException) {
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
                    Servidor.postJson(
                        "/api/grupos/localizacao",
                        JSONObject().put("gid", gid).put("lat", local.latitude).put("lng", local.longitude)
                    )
                    aviso = ""
                    atualizar()
                } else {
                    aviso = "Não consegui achar sua posição agora. Verifique se o GPS está ligado."
                }
            }
        } else {
            aviso = "Sem permissão de localização. Libere no aparelho para compartilhar."
        }
    }

    val pegarFotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            escopo.launch {
                enviandoFoto = true
                var falha = ""
                uris.forEach { uri ->
                    val arquivo = lerFoto(contexto, uri)
                    if (arquivo == null) {
                        falha = "Não consegui ler uma das fotos."
                    } else {
                        val resposta = Servidor.enviarFotoComCampos(
                            "/api/grupos/foto",
                            mapOf("gid" to gid),
                            arquivo.first,
                            arquivo.second
                        )
                        if (!resposta.ok) falha = resposta.msg
                    }
                }
                avisoFoto = falha
                enviandoFoto = false
                atualizar()
            }
        }
    }

    val inicio = grupo
    if (inicio == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            if (erro.isNotBlank()) {
                Text(erro, textAlign = TextAlign.Center)
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    val contagem = calcularContagem(inicio.dataIso, agora)
    val pessoas = inicio.pessoas
    val eu = pessoas.firstOrNull { it.eu }
    val euAtivo = eu?.compartilha == true
    val outras = pessoas.filter { !it.eu }
    val outrasAtivas = outras.filter { it.compartilha }
    val nomeDe = { uid: String -> pessoas.firstOrNull { it.uid == uid }?.nome.orEmpty().ifBlank { "alguém" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (contagem != null) {
            item {
                CabecalhoContagem(
                    contagem = contagem,
                    linhaDeData = "${inicio.nome} · desde ${inicio.dataExtenso}",
                    unidade = unidade,
                    aoMudar = { unidade = it }
                )
            }
        }
        if (erro.isNotBlank()) {
            item { Text(erro, style = MaterialTheme.typography.bodyMedium) }
        }
        inicio.hoje?.let { hoje -> item { CartaoHoje(hoje) } }
        item {
            CartaoFotosGrupo(
                fotos = inicio.fotos,
                enviando = enviandoFoto,
                aviso = avisoFoto,
                aoAdicionar = {
                    pegarFotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                aoAbrir = { visor = it }
            )
        }
        inicio.laco?.let { laco -> item { CartaoLaco(laco) } }
        item {
            CartaoClimaGrupo(
                inicio = inicio,
                eu = eu,
                euAtivo = euAtivo,
                outras = outras,
                outrasAtivas = outrasAtivas,
                aviso = aviso,
                pegandoLocal = pegandoLocal,
                aoAlternarCompartilhar = { ligar ->
                    if (ligar) {
                        pedirLocal.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    } else {
                        escopo.launch {
                            Servidor.postJson("/api/grupos/localizacao/parar", JSONObject().put("gid", gid))
                            atualizar()
                        }
                    }
                },
                aoEscolherHumor = { id ->
                    escopo.launch {
                        Servidor.postJson("/api/grupos/humor", JSONObject().put("gid", gid).put("id", id))
                        atualizar()
                    }
                },
                aoVerMapa = { mapaAberto = true },
                aoLer = { lendo = it },
                aoEscrever = { escrevendo = true },
                nomeDe = nomeDe
            )
        }
    }

    lendo?.let { carta ->
        AlertDialog(
            onDismissRequest = {
                escopo.launch {
                    Servidor.postJson("/api/grupos/cartinha/lida", JSONObject().put("gid", gid).put("cid", carta.cid))
                    lendo = null
                    atualizar()
                }
            },
            title = { Text("Cartinha de ${carta.nomeDe}") },
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
            confirmButton = {
                TextButton(onClick = {
                    escopo.launch {
                        Servidor.postJson("/api/grupos/cartinha/lida", JSONObject().put("gid", gid).put("cid", carta.cid))
                        lendo = null
                        atualizar()
                    }
                }) { Text("Fechar e apagar") }
            }
        )
    }
    if (escrevendo) {
        DialogoEscreverCartaGrupo(
            destinatarios = outras,
            aoEnviar = { para, texto ->
                escopo.launch {
                    Servidor.postJson(
                        "/api/grupos/cartinha",
                        JSONObject().put("gid", gid).put("para", para).put("texto", texto)
                    )
                    atualizar()
                }
            },
            aoFechar = { escrevendo = false }
        )
    }
    visor?.let { foto ->
        Dialog(onDismissRequest = { visor = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = foto.url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    if (foto.podeApagar) {
                        TextButton(onClick = { apagarFoto = foto }) { Text("Apagar", color = Color.White) }
                    }
                    TextButton(onClick = { visor = null }) { Text("Fechar", color = Color.White) }
                }
            }
        }
    }
    apagarFoto?.let { foto ->
        DialogoConfirmar(
            titulo = "Apagar esta foto para todos?",
            texto = "Ela some do Início de todas as pessoas do grupo.",
            rotuloConfirmar = "Apagar",
            aoConfirmar = {
                escopo.launch {
                    Servidor.postJson("/api/grupos/foto/apagar", JSONObject().put("gid", gid).put("fid", foto.fid))
                    visor = null
                    atualizar()
                }
            },
            aoFechar = { apagarFoto = null }
        )
    }
    if (mapaAberto && euAtivo) {
        val pontos = pessoas
            .filter { it.compartilha && it.lat != null && it.lng != null }
            .map { Triple(it.nome, it.lat ?: 0.0, it.lng ?: 0.0) }
        DialogoMapaGrupo(pontos = pontos, aoFechar = { mapaAberto = false })
    }
}

@Composable
private fun CartaoFotosGrupo(
    fotos: List<FotoGrupo>,
    enviando: Boolean,
    aviso: String,
    aoAdicionar: () -> Unit,
    aoAbrir: (FotoGrupo) -> Unit
) {
    CartaoSecao(
        "Fotos do grupo",
        "Cada foto fica aqui até todos do grupo verem. Depois ela some sozinha."
    ) {
        if (fotos.isEmpty()) {
            Text("Nenhuma foto esperando para ser vista.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(fotos, key = { it.fid }) { foto ->
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
        OutlinedButton(
            onClick = aoAdicionar,
            enabled = !enviando,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        ) {
            Text(if (enviando) "Enviando fotos…" else "Adicionar fotos")
        }
        if (aviso.isNotBlank()) {
            Text(aviso, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun CartaoClimaGrupo(
    inicio: GrupoInicio,
    eu: PessoaLocal?,
    euAtivo: Boolean,
    outras: List<PessoaLocal>,
    outrasAtivas: List<PessoaLocal>,
    aviso: String,
    pegandoLocal: Boolean,
    aoAlternarCompartilhar: (Boolean) -> Unit,
    aoEscolherHumor: (String) -> Unit,
    aoVerMapa: () -> Unit,
    aoLer: (CartinhaGrupo) -> Unit,
    aoEscrever: () -> Unit,
    nomeDe: (String) -> String
) {
    val maisDistante = outras
        .filter { it.distanciaM != null }
        .maxByOrNull { it.distanciaM ?: 0.0 }
    val grande = if (euAtivo && maisDistante != null) textoDistancia((maisDistante.distanciaM ?: 0.0) / 1000) else "—"
    val texto = when {
        !euAtivo -> "Ative sua localização para ver as distâncias."
        outrasAtivas.isEmpty() -> "Ainda ninguém mais do grupo compartilhou a localização."
        maisDistante == null -> "Ainda ninguém mais do grupo compartilhou a localização."
        else -> "Até ${maisDistante.nome}, a mais distante. ${outrasAtivas.size} " +
            (if (outrasAtivas.size == 1) "pessoa" else "pessoas") + " no mapa."
    }
    val humorEu = inicio.humores[inicio.eu]

    CartaoSecao("Como está o tempo entre vocês") {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            inicio.pessoas.sortedByDescending { it.eu }.forEach { pessoa ->
                val humor = inicio.humores[pessoa.uid]
                val opcao = OPCOES_HUMOR.firstOrNull { it.id == humor?.id }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pessoa.nome.ifBlank { "?" }, style = MaterialTheme.typography.labelLarge)
                    Text(
                        opcao?.simbolo ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (opcao != null && humor != null) rotuloComTempo(opcao.rotulo, humor.em) else "Ainda não escolheu",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(grande, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(texto, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Compartilhar minha localização", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = euAtivo,
                    onCheckedChange = aoAlternarCompartilhar,
                    enabled = !pegandoLocal,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (pegandoLocal) {
                Text("Lendo sua posição…", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = aoVerMapa,
                enabled = euAtivo && outrasAtivas.isNotEmpty(),
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Ver no mapa") }
            if (aviso.isNotBlank()) {
                Text(aviso, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }
        }

        if (outras.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                outras.sortedBy { it.distanciaM ?: Double.MAX_VALUE }.forEach { pessoa ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(pessoa.nome, style = MaterialTheme.typography.bodySmall)
                        Text(
                            pessoa.distanciaM?.let { textoDistancia(it / 1000) } ?: "sem localização",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Text("Como você está hoje?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp))
        OPCOES_HUMOR.chunked(3).forEach { linha ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                linha.forEach { opcao ->
                    FilterChip(
                        selected = humorEu?.id == opcao.id,
                        onClick = { aoEscolherHumor(opcao.id) },
                        label = { Text("${opcao.simbolo} ${opcao.rotulo}") }
                    )
                }
            }
        }

        Text("Para você", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 18.dp))
        if (inicio.cartinhas.isEmpty()) {
            Text("Nenhuma cartinha ainda.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        } else {
            inicio.cartinhas.forEach { carta ->
                OutlinedButton(onClick = { aoLer(carta) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(rotuloComTempo("✉ Cartinha de ${carta.nomeDe.ifBlank { nomeDe(carta.de) }}", carta.em))
                }
            }
        }
        if (inicio.enviadas.isNotEmpty()) {
            Text(
                "Enviadas (ainda não lidas)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
            inicio.enviadas.forEach { carta ->
                Text(
                    "Para ${carta.nomePara}" + tempoAtras(carta.em).let { if (it.isBlank()) "" else " · $it" },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Button(onClick = aoEscrever, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Text("Escrever cartinha")
        }
    }
}

@Composable
private fun DialogoEscreverCartaGrupo(
    destinatarios: List<PessoaLocal>,
    aoEnviar: (String, String) -> Unit,
    aoFechar: () -> Unit
) {
    var para by rememberSaveable { mutableStateOf(destinatarios.firstOrNull()?.uid.orEmpty()) }
    var texto by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Nova cartinha") },
        text = {
            Column {
                Text("Para", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    destinatarios.forEach { pessoa ->
                        FilterChip(
                            selected = para == pessoa.uid,
                            onClick = { para = pessoa.uid },
                            label = { Text(pessoa.nome.ifBlank { "?" }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = texto,
                    onValueChange = { if (it.length <= 500) texto = it },
                    placeholder = { Text("Um recado curto…") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                Text("${texto.length} de 500 letras", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = texto.isNotBlank() && para.isNotBlank(),
                onClick = {
                    aoEnviar(para, texto.trim())
                    aoFechar()
                }
            ) { Text("Enviar cartinha") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

// Mapa com todas as pessoas que estão compartilhando (Leaflet e OpenStreetMap, como o mapa do grupo).
@Composable
private fun DialogoMapaGrupo(pontos: List<Triple<String, Double, Double>>, aoFechar: () -> Unit) {
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF3EFD9))) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mapa do grupo", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = aoFechar) { Text("Fechar") }
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        loadDataWithBaseURL(
                            "https://contagemdedias.vercel.app/",
                            htmlMapaGrupo(pontos),
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                }
            )
            Text(
                "Só o grupo vê esta tela.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun htmlMapaGrupo(pontos: List<Triple<String, Double, Double>>): String {
    fun num(valor: Double) = String.format(Locale.US, "%.6f", valor)
    fun nome(texto: String) = texto.replace("'", "").replace("<", "").ifBlank { "Pessoa" }
    val marcadores = pontos.joinToString("\n") { (rotulo, lat, lng) ->
        "L.marker([${num(lat)},${num(lng)}]).addTo(m).bindTooltip('${nome(rotulo)}',{permanent:true});"
    }
    val limites = pontos.joinToString(",") { (_, lat, lng) -> "[${num(lat)},${num(lng)}]" }
    return """
<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
<style>html,body,#mapa{height:100%;margin:0;background:#D6E2EA}</style></head>
<body><div id="mapa"></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
var m=L.map('mapa');
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(m);
$marcadores
var pts=[$limites];
if(pts.length){ m.fitBounds(L.latLngBounds(pts).pad(0.35),{maxZoom:15}); }
</script></body></html>
""".trimIndent()
}
