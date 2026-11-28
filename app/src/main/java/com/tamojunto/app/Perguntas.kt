package com.tamojunto.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// Pergunta do dia, como o servidor devolve. A resposta da outra pessoa vem nula até as duas responderem.
data class PerguntaDia(
    val data: String,
    val pergunta: String,
    val tema: String,
    val euRespondeu: Boolean,
    val euTexto: String,
    val outraRespondeu: Boolean,
    val outraTexto: String?,
    val revelada: Boolean
)

data class PerguntasApp(val hoje: PerguntaDia, val historico: List<PerguntaDia>)

private val TEMAS_PERGUNTA = mapOf(
    "intimidade" to "Intimidade",
    "rotina" to "Rotina",
    "relacionamento" to "Relacionamento",
    "memorias" to "Memórias",
    "sonhos" to "Sonhos"
)

private fun JSONObject.textoDe(campo: String): String = optString(campo, "")

private fun lerPergunta(j: JSONObject): PerguntaDia {
    val eu = j.optJSONObject("eu") ?: JSONObject()
    val outra = j.optJSONObject("outra") ?: JSONObject()
    return PerguntaDia(
        data = j.textoDe("data"),
        pergunta = j.textoDe("pergunta"),
        tema = j.textoDe("tema"),
        euRespondeu = eu.optBoolean("respondeu", false),
        euTexto = eu.textoDe("texto"),
        outraRespondeu = outra.optBoolean("respondeu", false),
        outraTexto = if (outra.isNull("texto")) null else outra.optString("texto", ""),
        revelada = j.optBoolean("revelada", false)
    )
}

// Conversa com /api/perguntas-dia. A pergunta é criada pelo servidor na primeira vez que alguém pede o dia.
object PerguntasServidor {
    // Devolve as perguntas, ou a mensagem do servidor para mostrar na tela.
    suspend fun carregar(): Pair<PerguntasApp?, String> {
        val r = Servidor.obter("/api/perguntas-dia")
        val j = r.corpo
        if (!r.ok || j == null) {
            return null to r.msg.ifBlank { "Não consegui carregar a pergunta de hoje." }
        }
        val hoje = j.optJSONObject("hoje")?.let(::lerPergunta)
            ?: return null to "Não consegui carregar a pergunta de hoje."
        val lista = j.optJSONArray("historico") ?: JSONArray()
        val historico = (0 until lista.length()).mapNotNull { lista.optJSONObject(it)?.let(::lerPergunta) }
        return PerguntasApp(hoje, historico) to ""
    }

    // Devolve vazio se a resposta foi guardada, ou a mensagem do servidor.
    suspend fun responder(data: String, texto: String): String {
        val r = Servidor.postJson(
            "/api/perguntas-dia/responder",
            JSONObject().put("data", data).put("texto", texto)
        )
        return if (r.ok) "" else r.msg.ifBlank { "Não consegui enviar agora." }
    }
}

// Cartão do Início: mostra a pergunta de hoje e abre a tela de resposta.
@Composable
internal fun CartaoPerguntaHoje(hoje: PerguntaDia?, erro: String, aoAbrir: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Tinta.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .clickable(onClick = aoAbrir)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Pergunta de hoje", style = EstiloRotulo, color = TintaSuave)
        when {
            hoje != null -> {
                Text(hoje.pergunta, style = EstiloTituloSecao, color = Tinta)
                Text(
                    when {
                        hoje.revelada -> "As respostas de vocês estão abertas"
                        hoje.euRespondeu -> "Sua resposta está guardada. Falta a outra pessoa."
                        else -> "Toque para responder"
                    },
                    style = EstiloRotulo,
                    color = Ocre
                )
            }
            erro.isNotBlank() -> Text(erro, style = EstiloApoio, color = TintaSuave)
            else -> Text("Carregando…", style = EstiloApoio, color = TintaSuave)
        }
    }
}

// Resposta de uma pessoa, em texto. Usada quando as duas já responderam.
@Composable
private fun TextoDaResposta(rotulo: String, texto: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(rotulo, style = EstiloRotulo, color = TintaSuave)
        Text(texto.orEmpty(), style = EstiloApoio, color = Tinta)
    }
}

// Bolinha que pisca enquanto a outra pessoa ainda não respondeu.
@Composable
private fun PontoPulsando() {
    val transicao = rememberInfiniteTransition(label = "pontoAguardando")
    val opacidade by transicao.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "opacidadePonto"
    )
    Box(Modifier.size(8.dp).alpha(opacidade).background(Ocre, CircleShape))
}

// Um dia já passado. Toque para abrir as respostas; a seta gira com a animação.
@Composable
private fun CartaoDiaAnterior(d: PerguntaDia, aberto: Boolean, aoAlternar: () -> Unit) {
    val giro by animateFloatAsState(
        targetValue = if (aberto) 180f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "giroSeta"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = aoAlternar)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(dataBr(d.data) + " · " + (TEMAS_PERGUNTA[d.tema] ?: ""), style = EstiloRotulo, color = TintaSuave)
                Text(d.pergunta, style = EstiloApoio, color = Tinta)
            }
            Box(Modifier.graphicsLayer { rotationZ = giro }) {
                IconeLinha(caminhos = listOf("M6 9l6 6 6-6"), tamanho = 18.dp, cor = TintaSuave)
            }
        }
        AnimatedVisibility(
            visible = aberto,
            enter = expandVertically(tween(280)) + fadeIn(tween(280)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(160))
        ) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (d.revelada) {
                    TextoDaResposta("Você", d.euTexto)
                    TextoDaResposta("A outra pessoa", d.outraTexto)
                } else if (d.euRespondeu) {
                    TextoDaResposta("Você", d.euTexto)
                    Text("A outra pessoa ainda não respondeu.", style = EstiloRotulo, color = Ocre)
                } else {
                    Text("Você não respondeu esta.", style = EstiloRotulo, color = Ocre)
                }
            }
        }
    }
}

// Conteúdo da folha: a pergunta de hoje, a sua resposta, o estado de quem falta e o histórico.
@Composable
internal fun ConteudoPerguntasDia(aoMudar: () -> Unit) {
    val escopo = rememberCoroutineScope()
    var estado by remember { mutableStateOf<PerguntasApp?>(null) }
    var erro by remember { mutableStateOf("") }
    var texto by rememberSaveable { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var avisoEnvio by remember { mutableStateOf("") }
    var abertas by remember { mutableStateOf(setOf<String>()) }
    var entrou by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val (app, msg) = PerguntasServidor.carregar()
        estado = app
        erro = msg
        entrou = true
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            "Perguntas de vocês",
            style = EstiloTituloSecao,
            color = Tinta,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        val hoje = estado?.hoje
        if (hoje == null) {
            Text(erro.ifBlank { "Carregando…" }, style = EstiloApoio, color = TintaSuave, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            return@Column
        }

        // Data e tema, em uma linha só.
        Text(
            dataBr(hoje.data) + "  ·  " + (TEMAS_PERGUNTA[hoje.tema] ?: ""),
            style = EstiloRotulo,
            color = Ocre,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // A pergunta entra com uma transição suave.
        AnimatedVisibility(
            visible = entrou,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600, easing = FastOutSlowInEasing)) { it / 6 }
        ) {
            Text(
                hoje.pergunta,
                style = EstiloTituloSecao,
                color = Tinta,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            hoje.revelada -> {
                Text("As duas responderam", style = EstiloRotulo, color = Ocre, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                TextoDaResposta("Você", hoje.euTexto)
                TextoDaResposta("A outra pessoa", hoje.outraTexto)
            }
            hoje.euRespondeu -> {
                // Sua resposta fica visível; o ponto pisca até a outra pessoa responder.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PontoPulsando()
                    Text("Aguardando a outra pessoa", style = EstiloRotulo, color = TintaSuave)
                }
                TextoDaResposta("Você", hoje.euTexto)
                Text("As respostas aparecem juntas quando a outra pessoa responder.", style = EstiloRotulo, color = TintaSuave)
            }
            else -> {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(500) },
                    label = { Text("Sua resposta") },
                    supportingText = { Text("${texto.length}/500") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSite(
                    texto = if (enviando) "Enviando…" else "Enviar resposta",
                    primario = true,
                    habilitado = !enviando && texto.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    aoClicar = {
                        enviando = true
                        escopo.launch {
                            val msg = PerguntasServidor.responder(hoje.data, texto.trim())
                            enviando = false
                            if (msg.isBlank()) {
                                texto = ""
                                avisoEnvio = ""
                                val (app, m) = PerguntasServidor.carregar()
                                estado = app ?: estado
                                erro = m
                                aoMudar()
                            } else {
                                avisoEnvio = msg
                            }
                        }
                    }
                )
                if (avisoEnvio.isNotBlank()) Text(avisoEnvio, style = EstiloRotulo, color = Ocre)
                Text("Vocês só veem as respostas depois que as duas responderem.", style = EstiloRotulo, color = TintaSuave)
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.12f)))

        Text("Dias anteriores", style = EstiloTituloSecao, color = Tinta)
        val anteriores = estado?.historico.orEmpty()
        if (anteriores.isEmpty()) {
            Text("As perguntas dos dias anteriores aparecem aqui.", style = EstiloApoio, color = TintaSuave)
        }
        anteriores.forEach { d ->
            CartaoDiaAnterior(
                d = d,
                aberto = d.data in abertas,
                aoAlternar = { abertas = if (d.data in abertas) abertas - d.data else abertas + d.data }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.08f)))
        }
    }
}

// Folha com a pergunta do dia, aberta pelo cartão do Início.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolhaPerguntasDia(aoFechar: () -> Unit, aoMudar: () -> Unit) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = aoFechar, sheetState = estado) {
        ConteudoPerguntasDia(aoMudar = aoMudar)
    }
}
