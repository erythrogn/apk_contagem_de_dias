package com.tamojunto.app

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
@Composable
fun TelaInicio(dados: DadosDoCasal, usuario: FirebaseUser) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    var inicio by remember { mutableStateOf<InicioApp?>(null) }
    var erroInicio by remember { mutableStateOf("") }
    var agora by remember { mutableStateOf(LocalDateTime.now(FUSO_BELEM)) }
    var unidade by rememberSaveable { mutableStateOf("dias") }
    var lendo by remember { mutableStateOf<Cartinha?>(null) }
    var escrevendo by rememberSaveable { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    var pegandoLocal by remember { mutableStateOf(false) }

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
            val (novos, erro) = InicioServidor.carregar()
            if (novos != null) {
                inicio = novos
                erroInicio = ""
            } else {
                erroInicio = erro
            }
            delay(60_000)
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
            aviso = "Sem permissão de localização. Libere no aparelho para ver a distância."
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

    val eu = inicio?.eu?.ifBlank { null } ?: dados.meuSlot ?: "pessoa1"
    val outra = if (eu == "pessoa1") "pessoa2" else "pessoa1"
    val nomeEu = inicio?.nomes?.get(eu).orEmpty().ifBlank { "Você" }
    val nomeOutra = inicio?.nomes?.get(outra).orEmpty().ifBlank { "Parceiro(a)" }
    val hojeIso = LocalDate.now(FUSO_BELEM).toString()
    val proximas = (dados.filmes + dados.series + dados.jogos + dados.coisinhas + dados.viagens + dados.planos)
        .filter { !it.feito && it.dataIso.isNotBlank() && it.dataIso >= hojeIso }
        .sortedBy { it.dataIso }
        .take(3)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CabecalhoContagem(
                contagem = contagem,
                desde = inicio?.dataExtenso?.ifBlank { null } ?: dataBr(inicioIso),
                unidade = unidade,
                aoMudar = { unidade = it }
            )
        }
        if (erroInicio.isNotBlank() && inicio == null) {
            item { Text(erroInicio, style = MaterialTheme.typography.bodyMedium) }
        }
        inicio?.hoje?.let { hoje -> item { CartaoHoje(hoje) } }
        item { CartaoProximas(proximas) }
        item { CartaoFotos(inicio?.fotos.orEmpty()) }
        inicio?.laco?.let { laco -> item { CartaoLaco(laco) } }
        item {
            CartaoClima(
                dados = dados,
                eu = eu,
                outra = outra,
                nomeEu = nomeEu,
                nomeOutra = nomeOutra,
                aviso = aviso,
                pegandoLocal = pegandoLocal,
                aoPedirLocal = {
                    pedirLocal.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                },
                aoLer = { lendo = it },
                aoEscrever = { escrevendo = true }
            )
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
}

// Número grande, unidade, composição e relógio: o topo da página do site.
@Composable
private fun CabecalhoContagem(contagem: Contagem, desde: String, unidade: String, aoMudar: (String) -> Unit) {
    val (numero, palavra) = numeroDaUnidade(contagem, unidade)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "desde $desde",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text("tamo junto há", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 14.dp))
        Text("$numero", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text(palavra, style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("dias" to "Dias", "meses" to "Meses", "anos" to "Anos").forEach { (chave, nome) ->
                FilterChip(selected = unidade == chave, onClick = { aoMudar(chave) }, label = { Text(nome) })
            }
        }
        Text(
            contagem.composicao,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            MedidaDoTempo(contagem.horas, "horas")
            MedidaDoTempo(contagem.minutos, "minutos")
            MedidaDoTempo(contagem.segundos, "segundos")
        }
    }
}

@Composable
private fun MedidaDoTempo(valor: Long, rotulo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor.toString().padStart(2, '0'), style = MaterialTheme.typography.headlineSmall)
        Text(rotulo, style = MaterialTheme.typography.labelSmall)
    }
}

// Cartão padrão das seções: título em serifa, subtítulo e o conteúdo.
@Composable
private fun CartaoSecao(titulo: String, subtitulo: String? = null, conteudo: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall)
            if (subtitulo != null) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) { conteudo() }
        }
    }
}

@Composable
private fun CartaoHoje(hoje: ResumoHoje) {
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
private fun CartaoFotos(fotos: List<String>) {
    CartaoSecao("Fotos de vocês", "Deslize para ver outras fotos.") {
        if (fotos.isEmpty()) {
            Text(
                "Nenhuma foto ainda. Adicione fotos no site, em Configurações.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(fotos) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 220.dp, height = 280.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoLaco(laco: LacoApp) {
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
    aoPedirLocal: () -> Unit,
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
                locEu == null -> "Compartilhe sua localização para ver a distância entre vocês."
                else -> "$nomeOutra ainda não compartilhou a localização."
            }
            Text(
                texto,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedButton(
                onClick = aoPedirLocal,
                enabled = !pegandoLocal,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    when {
                        pegandoLocal -> "Lendo sua posição…"
                        locEu == null -> "Ativar minha localização"
                        else -> "Atualizar minha localização"
                    }
                )
            }
            if (aviso.isNotBlank()) {
                Text(aviso, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }
        }

        Text(
            "Como você está hoje?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 18.dp)
        )
        OPCOES_HUMOR.chunked(3).forEach { linha ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                linha.forEach { opcao ->
                    FilterChip(
                        selected = humorEu?.id == opcao.id,
                        onClick = { dados.definirHumor(opcao.id) },
                        label = { Text("${opcao.simbolo} ${opcao.rotulo}") }
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
                OutlinedButton(onClick = { aoLer(carta) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(rotuloComTempo("✉ Cartinha de $nomeOutra", carta.em))
                }
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
        Button(onClick = aoEscrever, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Text("Escrever cartinha")
        }
    }
}

@Composable
private fun PessoaComHumor(nome: String, humor: Humor?) {
    val opcao = OPCOES_HUMOR.firstOrNull { it.id == humor?.id }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(nome, style = MaterialTheme.typography.labelLarge)
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
private fun DialogoEscreverCartinha(nomeOutra: String, aoEnviar: (String) -> Unit, aoFechar: () -> Unit) {
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

// Pede uma leitura única da posição (o site atualiza enquanto a pessoa quiser; aqui é ao tocar no botão).
@SuppressLint("MissingPermission")
private suspend fun lerLocalizacao(contexto: Context): Location? = try {
    LocationServices.getFusedLocationProviderClient(contexto)
        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .await()
} catch (erro: Exception) {
    null
}
