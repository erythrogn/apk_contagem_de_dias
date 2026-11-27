package com.tamojunto.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class EventoWidget(
    val titulo: String,
    val data: String,
    val tipo: String,
    val chave: String = "",
    val colecao: String = ""
)

data class CartaRecebidaWidget(
    val texto: String,
    val de: String
)

data class DadosWidget(
    val inicio: String?,
    val nomeOutra: String,
    val eventos: List<EventoWidget>,
    val casal: String? = null,
    val slot: String? = null,
    val cartasNovas: Int = 0,
    val ultimaTexto: String = "",
    val ultimaDe: String = "",
    val cartasRecebidas: List<CartaRecebidaWidget> = emptyList()
)

private const val ARQUIVO_WIDGETS = "widgets_casal"
private const val CHAVE_WIDGETS = "dados"
private const val CHAVE_UNIDADE = "unidade"
private const val CHAVE_CASAL = "casal"
private const val CHAVE_SLOT = "slot"
private const val CHAVE_STATUS = "status_carta"
private const val CHAVE_STATUS_EM = "status_carta_em"
private const val CHAVE_STATUS_CONTAGEM = "status_contagem"
private const val CHAVE_STATUS_CONTAGEM_EM = "status_contagem_em"
private const val CHAVE_STATUS_EVENTO = "status_evento"
private const val CHAVE_STATUS_EVENTO_EM = "status_evento_em"
private const val CHAVE_PAGINA_EVENTOS = "pagina_eventos"
private const val CHAVE_INDICE_CARTA = "indice_carta"
private const val CHAVE_PAGINA_FRASES = "pagina_frases"
private const val CHAVE_TEMA_WIDGET = "tema_widget"
private const val CHAVE_MOSTRAR_COMP = "mostrar_composicao"
private const val CHAVE_MOSTRAR_PING = "mostrar_ping"
private const val CHAVE_FRASES_CUSTOM = "frases_custom"

private data class PaletaWidget(
    val id: String,
    val nome: String,
    val contagemTopo: Color,
    val contagemBase: Color,
    val eventosTopo: Color,
    val eventosBase: Color,
    val cartaTopo: Color,
    val cartaBase: Color,
    val bordaPrincipal: Color,
    val bordaSecundaria: Color,
    val destaqueAtivo: Color,
    val pillSuave: Color,
    val trilho: Color
)

private val PALETAS_WIDGET = listOf(
    PaletaWidget(
        id = "creme",
        nome = "Papel Creme",
        contagemTopo = Color(0xFFFAF3E4),
        contagemBase = Color(0xFFEFE1C4),
        eventosTopo = Color(0xFFFCF9F2),
        eventosBase = Color(0xFFF4EEDF),
        cartaTopo = Color(0xFFEBF3FB),
        cartaBase = Color(0xFFD8E6F5),
        bordaPrincipal = Color(0xFFD2B984),
        bordaSecundaria = Color(0xFFA2BAD6),
        destaqueAtivo = Color(0xFF47607E),
        pillSuave = Color(0xFFF6E9CC),
        trilho = Color(0xFFE8D9B8)
    ),
    PaletaWidget(
        id = "azul",
        nome = "Azul Névoa",
        contagemTopo = Color(0xFFEDF4FB),
        contagemBase = Color(0xFFD9E7F5),
        eventosTopo = Color(0xFFF4F8FC),
        eventosBase = Color(0xFFE5EFF8),
        cartaTopo = Color(0xFFF9F3E6),
        cartaBase = Color(0xFFEFE2C8),
        bordaPrincipal = Color(0xFF9FB7D3),
        bordaSecundaria = Color(0xFFD2B984),
        destaqueAtivo = Color(0xFF3E5A7A),
        pillSuave = Color(0xFFDCE8F6),
        trilho = Color(0xFFD2E1F0)
    ),
    PaletaWidget(
        id = "lavanda",
        nome = "Lavanda Suave",
        contagemTopo = Color(0xFFF4F0FA),
        contagemBase = Color(0xFFE5DDF2),
        eventosTopo = Color(0xFFF9F7FC),
        eventosBase = Color(0xFFEEE9F6),
        cartaTopo = Color(0xFFFBF4E6),
        cartaBase = Color(0xFFF1E4CA),
        bordaPrincipal = Color(0xFFB8A9D4),
        bordaSecundaria = Color(0xFFD2B984),
        destaqueAtivo = Color(0xFF554A73),
        pillSuave = Color(0xFFE6DFF2),
        trilho = Color(0xFFDDD4EC)
    )
)

private val CartaoCartaPastel = Color(0xE0FFFFFF)
private val BotaoAzulSuavePastel = Color(0xFFC5D8EE)
private val TintaPastel = Color(0xFF2B3F57)
private val TintaSuavePastel = Color(0xFF5D718A)
private val CremePastel = Color(0xFFFCF9F2)
private val OcrePastel = Color(0xFF8C6835)
private val SeloMesPastel = Color(0xFFF4E4C1)
private val UrgenteFundoPastel = Color(0xFFF3DFBA)

private val FRASES_PADRAO = listOf(
    "Pensando em você",
    "Saudade de você",
    "Te amo",
    "Boa noite",
    "Bom dia, amor",
    "Tô chegando",
    "Quero te ver",
    "Tudo bem aí?"
)

private val ChaveAcao = ActionParameters.Key<String>("acao")
private val ChaveUnidade = ActionParameters.Key<String>("unidade")
private val ChaveTextoCarta = ActionParameters.Key<String>("texto")
private val ChaveOrigemStatus = ActionParameters.Key<String>("origem")
private val ChaveEventoId = ActionParameters.Key<String>("evento_chave")
private val ChaveEventoColecao = ActionParameters.Key<String>("evento_colecao")
private val ChaveEventoTitulo = ActionParameters.Key<String>("evento_titulo")
private val ChaveDelta = ActionParameters.Key<Int>("delta")
private val ChaveAbaEditor = ActionParameters.Key<String>("aba_editor")

object PedidosWidget {
    var cartaPendente by mutableStateOf(false)

    fun lerIntent(intent: Intent?) {
        if (intent?.getStringExtra("acao") == "carta") cartaPendente = true
    }
}

private fun prefsWidgets(contexto: Context) =
    contexto.getSharedPreferences(ARQUIVO_WIDGETS, Context.MODE_PRIVATE)

private fun paletaAtual(contexto: Context): PaletaWidget {
    val id = prefsWidgets(contexto).getString(CHAVE_TEMA_WIDGET, "creme") ?: "creme"
    return PALETAS_WIDGET.firstOrNull { it.id == id } ?: PALETAS_WIDGET.first()
}

private fun lerFrasesRapidas(contexto: Context): List<String> {
    val raw = prefsWidgets(contexto).getString(CHAVE_FRASES_CUSTOM, null) ?: return FRASES_PADRAO
    return try {
        val arr = JSONArray(raw)
        val lista = (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }
        if (lista.size >= 4) lista else FRASES_PADRAO
    } catch (_: Exception) {
        FRASES_PADRAO
    }
}

private fun salvarFrasesRapidas(contexto: Context, frases: List<String>) {
    val limpas = frases.map { it.trim() }.filter { it.isNotBlank() }
    val arr = JSONArray()
    (limpas.ifEmpty { FRASES_PADRAO }).forEach { arr.put(it) }
    prefsWidgets(contexto).edit().putString(CHAVE_FRASES_CUSTOM, arr.toString()).apply()
}

fun salvarDadosWidget(contexto: Context, dados: DadosWidget) {
    val eventos = JSONArray()
    dados.eventos.forEach { evento ->
        eventos.put(
            JSONObject()
                .put("t", evento.titulo)
                .put("d", evento.data)
                .put("r", evento.tipo)
                .put("k", evento.chave)
                .put("c", evento.colecao)
        )
    }
    val cartas = JSONArray()
    val listaCartas = dados.cartasRecebidas.ifEmpty {
        if (dados.ultimaTexto.isNotBlank()) listOf(CartaRecebidaWidget(dados.ultimaTexto, dados.ultimaDe)) else emptyList()
    }
    listaCartas.forEach { carta ->
        cartas.put(JSONObject().put("t", carta.texto).put("d", carta.de))
    }
    val json = JSONObject()
        .put("inicio", dados.inicio ?: "")
        .put("nomeOutra", dados.nomeOutra)
        .put("eventos", eventos)
        .put("cartasNovas", dados.cartasNovas)
        .put("ultimaTexto", dados.ultimaTexto)
        .put("ultimaDe", dados.ultimaDe)
        .put("cartasRecebidas", cartas)
    prefsWidgets(contexto).edit()
        .putString(CHAVE_WIDGETS, json.toString())
        .putString(CHAVE_CASAL, dados.casal ?: "")
        .putString(CHAVE_SLOT, dados.slot ?: "")
        .putInt(CHAVE_INDICE_CARTA, 0)
        .apply()
}

fun lerDadosWidget(contexto: Context): DadosWidget {
    val vazio = DadosWidget(null, "", emptyList())
    val texto = prefsWidgets(contexto).getString(CHAVE_WIDGETS, null) ?: return vazio
    return try {
        val json = JSONObject(texto)
        val lista = json.optJSONArray("eventos") ?: JSONArray()
        val eventos = (0 until lista.length()).map { i ->
            val e = lista.getJSONObject(i)
            EventoWidget(
                titulo = e.optString("t"),
                data = e.optString("d"),
                tipo = e.optString("r"),
                chave = e.optString("k"),
                colecao = e.optString("c")
            )
        }
        val arrCartas = json.optJSONArray("cartasRecebidas") ?: JSONArray()
        val cartas = (0 until arrCartas.length()).mapNotNull { i ->
            val c = arrCartas.optJSONObject(i) ?: return@mapNotNull null
            val t = c.optString("t")
            if (t.isBlank()) null else CartaRecebidaWidget(t, c.optString("d"))
        }
        val ultimaTexto = json.optString("ultimaTexto")
        val ultimaDe = json.optString("ultimaDe")
        DadosWidget(
            inicio = json.optString("inicio").ifBlank { null },
            nomeOutra = json.optString("nomeOutra"),
            eventos = eventos,
            casal = prefsWidgets(contexto).getString(CHAVE_CASAL, null)?.ifBlank { null },
            slot = prefsWidgets(contexto).getString(CHAVE_SLOT, null)?.ifBlank { null },
            cartasNovas = json.optInt("cartasNovas", 0),
            ultimaTexto = ultimaTexto,
            ultimaDe = ultimaDe,
            cartasRecebidas = cartas.ifEmpty {
                if (ultimaTexto.isNotBlank()) listOf(CartaRecebidaWidget(ultimaTexto, ultimaDe)) else emptyList()
            }
        )
    } catch (_: Exception) {
        vazio
    }
}

private fun lerUnidadeWidget(contexto: Context): String =
    prefsWidgets(contexto).getString(CHAVE_UNIDADE, "dias") ?: "dias"

fun montarDadosWidget(dados: DadosDoCasal, nomes: Map<String, String>): DadosWidget {
    val eu = dados.meuSlot ?: "pessoa1"
    val outra = if (eu == "pessoa1") "pessoa2" else "pessoa1"
    val hoje = LocalDate.now(FUSO_BELEM).toString()
    val fontes: List<Triple<String, String, List<Item>>> = listOf(
        Triple("Viagem", "viagens", dados.viagens.toList()),
        Triple("Filme", "filmes", dados.filmes.toList()),
        Triple("Série", "series", dados.series.toList()),
        Triple("Jogo", "jogos", dados.jogos.toList()),
        Triple("Coisinha", "coisinhas", dados.coisinhas.toList()),
        Triple("Plano", "planos", dados.planos.toList())
    )
    val eventos = fontes.flatMap { (rotulo, colecao, itens) ->
        itens
            .filter { !it.feito && it.dataIso.isNotBlank() && it.dataIso >= hoje }
            .map { EventoWidget(it.titulo, it.dataIso, rotulo, it.chave, colecao) }
    }.sortedBy { it.data }.take(12)
    val recebidas = dados.cartinhas.filter { it.para == eu }
    val ultima = recebidas.firstOrNull()
    val nomeOutraPessoa = nomes[outra].orEmpty()
    val listaCartas = recebidas.take(6).map { carta ->
        CartaRecebidaWidget(
            texto = carta.texto,
            de = if (carta.de == outra) nomeOutraPessoa else ""
        )
    }
    return DadosWidget(
        inicio = dados.dataNamoro,
        nomeOutra = nomeOutraPessoa,
        eventos = eventos,
        casal = dados.casalId,
        slot = dados.meuSlot,
        cartasNovas = recebidas.size,
        ultimaTexto = ultima?.texto.orEmpty(),
        ultimaDe = if (ultima != null && ultima.de == outra) nomeOutraPessoa else "",
        cartasRecebidas = listaCartas
    )
}

suspend fun atualizarWidgets(contexto: Context, dados: DadosWidget) {
    salvarDadosWidget(contexto, dados)
    ContagemWidget().updateAll(contexto)
    EventosWidget().updateAll(contexto)
    CartaWidget().updateAll(contexto)
}

private fun abrirApp(contexto: Context, carta: Boolean = false): Action =
    actionStartActivity(
        ComponentName(contexto, MainActivity::class.java),
        if (carta) actionParametersOf(ChaveAcao to "carta") else actionParametersOf()
    )

private fun abrirEditorWidget(contexto: Context, aba: String): Action =
    actionStartActivity(
        ComponentName(contexto, WidgetEditorActivity::class.java),
        actionParametersOf(ChaveAbaEditor to aba)
    )

class CiclarUnidadeCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val proxima = when (lerUnidadeWidget(context)) {
            "dias" -> "meses"
            "meses" -> "anos"
            else -> "dias"
        }
        prefsWidgets(context).edit().putString(CHAVE_UNIDADE, proxima).apply()
        ContagemWidget().update(context, glanceId)
    }
}

class SetarUnidadeCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val unidade = parameters[ChaveUnidade] ?: return
        prefsWidgets(context).edit().putString(CHAVE_UNIDADE, unidade).apply()
        ContagemWidget().update(context, glanceId)
    }
}

class EnviarCartaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val texto = parameters[ChaveTextoCarta] ?: return
        val origem = parameters[ChaveOrigemStatus] ?: "carta"
        val prefs = prefsWidgets(context)
        val casal = prefs.getString(CHAVE_CASAL, "").orEmpty()
        val slot = prefs.getString(CHAVE_SLOT, "").orEmpty()
        val status = if (casal.isBlank() || slot.isBlank()) {
            "Abra o app uma vez para entrar"
        } else {
            try {
                val para = if (slot == "pessoa1") "pessoa2" else "pessoa1"
                FirebaseDatabase.getInstance().getReference("casais/$casal/cartinhas").push()
                    .setValue(mapOf("de" to slot, "para" to para, "texto" to texto, "em" to ServerValue.TIMESTAMP))
                    .await()
                if (origem == "contagem") "Carinho enviado" else "Cartinha enviada"
            } catch (_: Exception) {
                "Sem conexão agora"
            }
        }
        val agora = System.currentTimeMillis()
        if (origem == "contagem") {
            prefs.edit()
                .putString(CHAVE_STATUS_CONTAGEM, status)
                .putLong(CHAVE_STATUS_CONTAGEM_EM, agora)
                .apply()
            ContagemWidget().update(context, glanceId)
        } else {
            prefs.edit()
                .putString(CHAVE_STATUS, status)
                .putLong(CHAVE_STATUS_EM, agora)
                .apply()
            CartaWidget().update(context, glanceId)
        }
    }
}

class ConcluirEventoCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val chave = parameters[ChaveEventoId].orEmpty()
        val colecao = parameters[ChaveEventoColecao].orEmpty()
        val titulo = parameters[ChaveEventoTitulo].orEmpty()
        val prefs = prefsWidgets(context)
        val casal = prefs.getString(CHAVE_CASAL, "").orEmpty()

        if (casal.isBlank() || chave.isBlank() || colecao.isBlank()) {
            prefs.edit()
                .putString(CHAVE_STATUS_EVENTO, "Abra o app uma vez para entrar")
                .putLong(CHAVE_STATUS_EVENTO_EM, System.currentTimeMillis())
                .apply()
            EventosWidget().update(context, glanceId)
            return
        }

        try {
            // As regras exigem updatedBy em qualquer atualização; sem usuário, a gravação falha.
            val email = FirebaseAuth.getInstance().currentUser?.email
                ?: throw IllegalStateException("Sem usuário logado")
            val atualizacao: Map<String, Any> = when (colecao) {
                "filmes" -> mapOf("watched" to true)
                "series" -> mapOf("done" to true, "status" to "concluida")
                "jogos" -> mapOf("done" to true, "status" to "zerado")
                "viagens" -> mapOf("status" to "feita")
                else -> mapOf("done" to true)
            } + mapOf("updatedBy" to email, "updatedAt" to ServerValue.TIMESTAMP)
            FirebaseDatabase.getInstance()
                .getReference("casais/$casal/$colecao/$chave")
                .updateChildren(atualizacao)
                .await()

            val atual = lerDadosWidget(context)
            val restantes = atual.eventos.filterNot { it.chave == chave && it.colecao == colecao }
            salvarDadosWidget(context, atual.copy(eventos = restantes))

            val resumo = if (titulo.isNotBlank()) "Concluído: $titulo" else "Marcado como concluído"
            prefs.edit()
                .putString(CHAVE_STATUS_EVENTO, resumo)
                .putLong(CHAVE_STATUS_EVENTO_EM, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {
            prefs.edit()
                .putString(CHAVE_STATUS_EVENTO, "Não foi possível salvar")
                .putLong(CHAVE_STATUS_EVENTO_EM, System.currentTimeMillis())
                .apply()
        }
        EventosWidget().update(context, glanceId)
    }
}

class PaginarEventosCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[ChaveDelta] ?: return
        val prefs = prefsWidgets(context)
        val total = lerDadosWidget(context).eventos.size
        if (total <= 1) return
        val atual = prefs.getInt(CHAVE_PAGINA_EVENTOS, 0)
        val proximo = (atual + delta).coerceIn(0, total - 1)
        prefs.edit().putInt(CHAVE_PAGINA_EVENTOS, proximo).apply()
        EventosWidget().update(context, glanceId)
    }
}

class NavegarCartaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[ChaveDelta] ?: return
        val prefs = prefsWidgets(context)
        val cartas = lerDadosWidget(context).cartasRecebidas
        if (cartas.size <= 1) return
        val atual = prefs.getInt(CHAVE_INDICE_CARTA, 0)
        val proximo = (atual + delta + cartas.size) % cartas.size
        prefs.edit().putInt(CHAVE_INDICE_CARTA, proximo).apply()
        CartaWidget().update(context, glanceId)
    }
}

class AlternarPaginaFrasesCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = prefsWidgets(context)
        val frases = lerFrasesRapidas(context)
        val totalPaginas = max(1, (frases.size + 3) / 4)
        val atual = prefs.getInt(CHAVE_PAGINA_FRASES, 0)
        val proxima = (atual + 1) % totalPaginas
        prefs.edit().putInt(CHAVE_PAGINA_FRASES, proxima).apply()
        CartaWidget().update(context, glanceId)
    }
}

class ZonaToque(val x: Float, val y: Float, val w: Float, val h: Float, val acao: Action)

private class Tela(val contexto: Context, val largura: Float, val altura: Float) {
    // Supersampling nítido até 2.75x respeitando limite seguro de pixels para RemoteViews sem travamento.
    private val escala = run {
        val densidade = min(contexto.resources.displayMetrics.density, 2.75f)
        val area = max(1f, largura * altura)
        min(densidade, sqrt(680_000f / area)).coerceAtLeast(1f)
    }
    val bitmap: Bitmap = Bitmap.createBitmap(
        max(1, (largura * escala).toInt()),
        max(1, (altura * escala).toInt()),
        Bitmap.Config.ARGB_8888
    ).also { it.prepareToDraw() }
    val canvas = Canvas(bitmap).also { it.scale(escala, escala) }
    val zonas = mutableListOf<ZonaToque>()

    fun zona(x: Float, y: Float, w: Float, h: Float, acao: Action) {
        if (w <= 0f || h <= 0f) return
        zonas.add(ZonaToque(x.coerceAtLeast(0f), y.coerceAtLeast(0f), w, h, acao))
    }

    fun retangulo(x: Float, y: Float, w: Float, h: Float, raio: Float, cor: Color, contorno: Boolean = false, espessura: Float = 1f) {
        if (w <= 0f || h <= 0f) return
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            color = cor.toArgb()
            style = if (contorno) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = espessura
        }
        val r = min(raio, min(w, h) / 2f)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), r, r, pincel)
    }

    fun fundoDegrade(raio: Float, topo: Color, base: Color) {
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, 0f, altura,
                topo.toArgb(),
                base.toArgb(),
                Shader.TileMode.CLAMP
            )
        }
        val r = min(raio, min(largura, altura) / 2f)
        canvas.drawRoundRect(RectF(0f, 0f, largura, altura), r, r, pincel)
    }

    fun sombraSuave(x: Float, y: Float, w: Float, h: Float, raio: Float, alpha: Float = 0.08f) {
        retangulo(x, y + 1.5f, w, h, raio, TintaPastel.copy(alpha = alpha))
    }

    fun moldura(cor: Color) {
        val raioBase = min(22f, min(largura, altura) * 0.22f)
        retangulo(4.5f, 4.5f, largura - 9f, altura - 9f, max(6f, raioBase - 3f), cor.copy(alpha = 0.85f), contorno = true, espessura = 1f)
        retangulo(8.5f, 8.5f, largura - 17f, altura - 17f, max(4f, raioBase - 7f), cor.copy(alpha = 0.35f), contorno = true, espessura = 0.6f)
    }

    fun ornamentoCentral(cx: Float, cy: Float, larguraLinha: Float, cor: Color) {
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.copy(alpha = 0.65f).toArgb()
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val meio = larguraLinha / 2f
        canvas.drawLine(cx - meio, cy, cx - 10f, cy, pincel)
        canvas.drawLine(cx + 10f, cy, cx + meio, cy, pincel)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.toArgb()
            style = Paint.Style.FILL
        }
        val losango = Path().apply {
            moveTo(cx, cy - 3f)
            lineTo(cx + 3f, cy)
            lineTo(cx, cy + 3f)
            lineTo(cx - 3f, cy)
            close()
        }
        canvas.drawPath(losango, fill)
        canvas.drawCircle(cx - 6.5f, cy, 1.1f, fill)
        canvas.drawCircle(cx + 6.5f, cy, 1.1f, fill)
    }

    fun coracao(cx: Float, cy: Float, tamanho: Float, cor: Color, preenchido: Boolean = true) {
        val r = tamanho / 2f
        val topY = cy - r * 0.38f
        val bottomY = cy + r * 0.88f
        val path = Path().apply {
            moveTo(cx, topY)
            cubicTo(
                cx - r * 0.55f, cy - r * 1.05f,
                cx - r * 1.15f, cy - r * 0.18f,
                cx, bottomY
            )
            moveTo(cx, topY)
            cubicTo(
                cx + r * 0.55f, cy - r * 1.05f,
                cx + r * 1.15f, cy - r * 0.18f,
                cx, bottomY
            )
        }
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.toArgb()
            style = if (preenchido) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = 1.4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, pincel)
    }

    fun check(cx: Float, cy: Float, tamanho: Float, cor: Color) {
        val r = tamanho / 2f
        val path = Path().apply {
            moveTo(cx - r * 0.65f, cy + r * 0.05f)
            lineTo(cx - r * 0.18f, cy + r * 0.55f)
            lineTo(cx + r * 0.72f, cy - r * 0.48f)
        }
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, pincel)
    }

    fun lapis(cx: Float, cy: Float, tamanho: Float, cor: Color) {
        val r = tamanho / 2f
        val path = Path().apply {
            moveTo(cx - r * 0.65f, cy + r * 0.65f)
            lineTo(cx - r * 0.52f, cy + r * 0.15f)
            lineTo(cx + r * 0.35f, cy - r * 0.72f)
            lineTo(cx + r * 0.72f, cy - r * 0.35f)
            lineTo(cx - r * 0.15f, cy + r * 0.52f)
            close()
        }
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 1.3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, pincel)
    }

    fun circulo(cx: Float, cy: Float, raio: Float, cor: Color) {
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, raio, pincel)
    }

    fun fonte(cor: Color, fonteRes: Int, tamanho: Float): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            color = cor.toArgb()
            textSize = tamanho
            typeface = ResourcesCompat.getFont(contexto, fonteRes) ?: Typeface.DEFAULT
        }

    fun fonteAjustada(
        t: String,
        cor: Color,
        fonteRes: Int,
        tamanhoMax: Float,
        tamanhoMin: Float,
        maxLargura: Float
    ): TextPaint {
        val pincel = fonte(cor, fonteRes, tamanhoMax)
        val medido = pincel.measureText(t)
        if (medido > maxLargura && maxLargura > 0f) {
            pincel.textSize = max(tamanhoMin, tamanhoMax * (maxLargura / medido))
        }
        return pincel
    }

    fun baselineCentro(centroY: Float, pincel: TextPaint): Float =
        centroY - (pincel.ascent() + pincel.descent()) / 2f

    fun texto(t: String, x: Float, y: Float, pincel: TextPaint, maxLargura: Float, alinha: Paint.Align = Paint.Align.LEFT): Float {
        pincel.textAlign = alinha
        val cortado = TextUtils.ellipsize(t, pincel, max(1f, maxLargura), TextUtils.TruncateAt.END).toString()
        canvas.drawText(cortado, x, y, pincel)
        return pincel.measureText(cortado)
    }

    fun icone(res: Int, x: Float, y: Float, w: Float, h: Float, cor: Color? = null) {
        ResourcesCompat.getDrawable(contexto.resources, res, null)?.mutate()?.let { desenho ->
            if (cor != null) desenho.setTint(cor.toArgb())
            desenho.setBounds(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt())
            desenho.draw(canvas)
        }
    }
}

@Composable
private fun TelaWidget(tela: Tela, descricao: String) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Image(
            provider = ImageProvider(tela.bitmap),
            contentDescription = descricao,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize()
        )
        tela.zonas.forEach { zona ->
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Spacer(GlanceModifier.height(zona.y.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Spacer(GlanceModifier.width(zona.x.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(zona.w.dp)
                            .height(zona.h.dp)
                            .clickable(onClick = zona.acao)
                    ) {}
                }
            }
        }
    }
}

private val UNIDADES = listOf("dias" to "dias", "meses" to "meses", "anos" to "anos")

private fun Tela.botoesUnidade(
    y: Float,
    altura: Float,
    unidadeAtual: String,
    larguraTotal: Float,
    x0: Float,
    paleta: PaletaWidget
) {
    val raioTrack = altura / 2f
    retangulo(x0, y, larguraTotal, altura, raioTrack, paleta.trilho.copy(alpha = 0.72f))
    retangulo(x0, y, larguraTotal, altura, raioTrack, paleta.bordaPrincipal.copy(alpha = 0.65f), contorno = true, espessura = 0.7f)

    val padInterno = 3f
    val larguraItem = (larguraTotal - padInterno * 2f) / UNIDADES.size
    val alturaItem = altura - padInterno * 2f
    val raioPill = alturaItem / 2f
    val tamFonte = (alturaItem * 0.44f).coerceIn(9.5f, 11f)

    UNIDADES.forEachIndexed { i, (chave, rotulo) ->
        val x = x0 + padInterno + i * larguraItem
        val yItem = y + padInterno
        val ativa = chave == unidadeAtual
        if (ativa) {
            sombraSuave(x, yItem, larguraItem, alturaItem, raioPill, alpha = 0.14f)
            retangulo(x, yItem, larguraItem, alturaItem, raioPill, paleta.destaqueAtivo)
        }
        val pincel = fonte(
            if (ativa) CremePastel else TintaPastel.copy(alpha = 0.85f),
            R.font.montserrat_semibold,
            tamFonte
        )
        texto(rotulo, x + larguraItem / 2f, baselineCentro(yItem + alturaItem / 2f, pincel), pincel, larguraItem - 6f, Paint.Align.CENTER)
        zona(x, y, larguraItem, altura, actionRunCallback<SetarUnidadeCallback>(actionParametersOf(ChaveUnidade to chave)))
    }
}

private fun statusRecenteChave(
    prefs: android.content.SharedPreferences,
    chaveTexto: String,
    chaveEm: String,
    janelaMs: Long = 10 * 60_000L
): String? {
    val status = prefs.getString(chaveTexto, null) ?: return null
    val em = prefs.getLong(chaveEm, 0L)
    return if (System.currentTimeMillis() - em < janelaMs) status else null
}

private fun desenharContagem(
    tela: Tela,
    dados: DadosWidget,
    unidade: String,
    contagem: Contagem?,
    statusPing: String?,
    paleta: PaletaWidget,
    mostrarComp: Boolean,
    mostrarPing: Boolean
) {
    val largura = tela.largura
    val altura = tela.altura
    val par = contagem?.let { numeroDaUnidade(it, unidade) }
    val numero = par?.let { formatarMilhar(it.first.toInt()) } ?: "--"
    val palavra = par?.second ?: "dias"
    val desde = statusPing ?: (dados.inicio?.let { "desde ${dataBr(it)}" } ?: "Defina a data do namoro no site")
    val composicao = if (mostrarComp) contagem?.composicao.orEmpty() else ""
    val abrir = abrirApp(tela.contexto)
    val abrirConfig = abrirEditorWidget(tela.contexto, "estilo")
    val raioFundo = min(24f, min(largura, altura) * 0.22f)
    val acaoPing = actionRunCallback<EnviarCartaCallback>(
        actionParametersOf(
            ChaveTextoCarta to "Pensando em você",
            ChaveOrigemStatus to "contagem"
        )
    )

    // Progresso sutil até o próximo mesversário (ciclo de 30 dias)
    val progressoCiclo = ((contagem?.dias ?: 0L) % 30L) / 30f

    tela.fundoDegrade(raioFundo, paleta.contagemTopo, paleta.contagemBase)
    tela.moldura(paleta.bordaPrincipal)

    when {
        largura >= 210f && altura >= 200f -> {
            val pad = 18f
            val btnH = (altura * 0.13f).coerceIn(28f, 34f)
            val btnY = altura - pad - btnH
            val areaTopo = pad + 4f
            val areaBase = btnY - 12f
            val maxTexto = largura - pad * 2f - 76f

            // Botão de edição completa do widget no canto superior esquerdo
            val btnTam = 25f
            val editX = pad
            val topBtnY = areaTopo - 2f
            tela.retangulo(editX, topBtnY, btnTam, btnTam, btnTam / 2f, paleta.pillSuave.copy(alpha = 0.85f))
            tela.retangulo(editX, topBtnY, btnTam, btnTam, btnTam / 2f, paleta.bordaPrincipal.copy(alpha = 0.7f), contorno = true, espessura = 0.7f)
            tela.lapis(editX + btnTam / 2f, topBtnY + btnTam / 2f, 10f, TintaSuavePastel)
            tela.zona(0f, 0f, editX + btnTam + 6f, topBtnY + btnTam + 8f, abrirConfig)

            // Botão rápido de carinho no canto superior direito
            val heartX = largura - pad - btnTam
            if (mostrarPing) {
                tela.sombraSuave(heartX, topBtnY, btnTam, btnTam, btnTam / 2f, alpha = 0.08f)
                tela.retangulo(heartX, topBtnY, btnTam, btnTam, btnTam / 2f, paleta.pillSuave)
                tela.retangulo(heartX, topBtnY, btnTam, btnTam, btnTam / 2f, paleta.bordaPrincipal.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
                tela.coracao(heartX + btnTam / 2f, topBtnY + btnTam / 2f, 10.5f, OcrePastel)
                tela.zona(heartX - 4f, 0f, btnTam + pad + 4f, topBtnY + btnTam + 8f, acaoPing)
            }

            val pDesde = tela.fonte(
                if (statusPing != null) paleta.destaqueAtivo else OcrePastel,
                if (statusPing != null) R.font.montserrat_semibold else R.font.cormorant_garamond_italic,
                if (statusPing != null) 10.5f else (altura * 0.064f).coerceIn(13f, 17f)
            )
            val yDesde = areaTopo + (-pDesde.ascent())
            tela.texto(desde, largura / 2f, yDesde, pDesde, maxTexto, Paint.Align.CENTER)

            val yOrnamento = yDesde + pDesde.descent() + 6f
            tela.ornamentoCentral(largura / 2f, yOrnamento, min(96f, largura * 0.36f), paleta.bordaPrincipal)

            val temComp = composicao.isNotBlank()
            val pComp = tela.fonte(TintaSuavePastel, R.font.montserrat_regular, (altura * 0.042f).coerceIn(9.5f, 11.5f))
            val yComp = areaBase - pComp.descent()
            if (temComp) {
                tela.texto(composicao, largura / 2f, yComp, pComp, largura - pad * 2f - 8f, Paint.Align.CENTER)
            }

            // Barra ultrafina de progresso do ciclo mensal
            val barW = min(110f, largura * 0.42f)
            val barX = (largura - barW) / 2f
            val barY = (if (temComp) yComp + pComp.ascent() - 7f else areaBase - 4f)
            tela.retangulo(barX, barY, barW, 2.5f, 1.25f, paleta.trilho)
            tela.retangulo(barX, barY, max(4f, barW * progressoCiclo), 2.5f, 1.25f, paleta.destaqueAtivo)

            val meioTopo = yOrnamento + 6f
            val meioBase = barY - 6f
            val meioAltura = max(40f, meioBase - meioTopo)

            val pNum = tela.fonteAjustada(
                numero,
                TintaPastel,
                R.font.cormorant_garamond_semibold,
                (meioAltura * 0.68f).coerceIn(40f, 94f),
                26f,
                largura - pad * 2f - 8f
            )
            val pPal = tela.fonte(TintaPastel, R.font.cormorant_garamond_regular, (meioAltura * 0.20f).coerceIn(15f, 23f))
            val altNum = -pNum.ascent()
            val altPal = -pPal.ascent() + pPal.descent()
            val gap = (meioAltura * 0.05f).coerceIn(4f, 9f)
            val blocoH = altNum + gap + altPal
            val inicioBloco = meioTopo + (meioAltura - blocoH) / 2f
            val yNum = inicioBloco + altNum
            val yPal = yNum + gap + (-pPal.ascent())

            tela.texto(numero, largura / 2f, yNum, pNum, largura - pad * 2f - 8f, Paint.Align.CENTER)
            tela.texto("$palavra juntos", largura / 2f, yPal, pPal, largura - pad * 2f - 8f, Paint.Align.CENTER)

            val fimTopoEsq = editX + btnTam + 6f
            val iniTopoDir = if (mostrarPing) heartX - 4f else largura
            tela.zona(fimTopoEsq, 0f, iniTopoDir - fimTopoEsq, topBtnY + btnTam + 8f, abrir)
            tela.zona(0f, topBtnY + btnTam + 8f, largura, btnY - topBtnY - btnTam - 12f, abrir)
            tela.botoesUnidade(btnY, btnH, unidade, largura - pad * 2f, pad, paleta)
        }

        largura >= 200f -> {
            val pad = 16f
            val btnH = (altura * 0.24f).coerceIn(24f, 30f)
            val btnY = altura - pad - btnH
            val areaTopo = pad
            val areaBase = btnY - 8f
            val areaH = max(32f, areaBase - areaTopo)

            val gapCol = 12f
            val larguraUtil = largura - pad * 2f - gapCol
            val colEsq = larguraUtil * 0.42f
            val xDir = pad + colEsq + gapCol

            val btnTam = 23f
            val heartX = largura - pad - btnTam
            val editX = if (mostrarPing) heartX - 5f - btnTam else heartX
            val colDir = max(20f, editX - xDir - 6f)

            val pNum = tela.fonteAjustada(
                numero,
                TintaPastel,
                R.font.cormorant_garamond_semibold,
                (areaH * 0.94f).coerceIn(28f, 60f),
                20f,
                colEsq
            )
            tela.texto(numero, pad + colEsq / 2f, tela.baselineCentro(areaTopo + areaH / 2f, pNum), pNum, colEsq, Paint.Align.CENTER)

            tela.retangulo(pad + colEsq + gapCol / 2f, areaTopo + 4f, 0.8f, areaH - 8f, 0f, paleta.bordaPrincipal.copy(alpha = 0.55f))

            // Botão de editar widget
            tela.retangulo(editX, areaTopo, btnTam, btnTam, btnTam / 2f, paleta.pillSuave.copy(alpha = 0.85f))
            tela.retangulo(editX, areaTopo, btnTam, btnTam, btnTam / 2f, paleta.bordaPrincipal.copy(alpha = 0.7f), contorno = true, espessura = 0.7f)
            tela.lapis(editX + btnTam / 2f, areaTopo + btnTam / 2f, 9.5f, TintaSuavePastel)
            tela.zona(editX - 2f, 0f, btnTam + 4f, areaTopo + btnTam + 6f, abrirConfig)

            if (mostrarPing) {
                tela.sombraSuave(heartX, areaTopo, btnTam, btnTam, btnTam / 2f, alpha = 0.08f)
                tela.retangulo(heartX, areaTopo, btnTam, btnTam, btnTam / 2f, paleta.pillSuave)
                tela.retangulo(heartX, areaTopo, btnTam, btnTam, btnTam / 2f, paleta.bordaPrincipal.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
                tela.coracao(heartX + btnTam / 2f, areaTopo + btnTam / 2f, 10f, OcrePastel)
                tela.zona(heartX - 2f, 0f, btnTam + pad + 2f, areaTopo + btnTam + 6f, acaoPing)
            }

            val mostrarLinhaComp = composicao.isNotBlank() && areaH >= 44f
            val pPal = tela.fonte(TintaPastel, R.font.cormorant_garamond_regular, (areaH * 0.34f).coerceIn(14f, 21f))
            val pDesde = tela.fonte(
                if (statusPing != null) paleta.destaqueAtivo else OcrePastel,
                if (statusPing != null) R.font.montserrat_semibold else R.font.cormorant_garamond_italic,
                if (statusPing != null) 10f else (areaH * 0.24f).coerceIn(11f, 14f)
            )

            if (mostrarLinhaComp) {
                val pComp = tela.fonte(TintaSuavePastel, R.font.montserrat_regular, (areaH * 0.18f).coerceIn(9f, 10.5f))
                tela.texto("$palavra juntos", xDir, tela.baselineCentro(areaTopo + areaH * 0.23f, pPal), pPal, colDir)
                tela.texto(desde, xDir, tela.baselineCentro(areaTopo + areaH * 0.56f, pDesde), pDesde, largura - xDir - pad)
                tela.texto(composicao, xDir, tela.baselineCentro(areaTopo + areaH * 0.85f, pComp), pComp, largura - xDir - pad)
            } else {
                tela.texto("$palavra juntos", xDir, tela.baselineCentro(areaTopo + areaH * 0.34f, pPal), pPal, colDir)
                tela.texto(desde, xDir, tela.baselineCentro(areaTopo + areaH * 0.72f, pDesde), pDesde, largura - xDir - pad)
            }

            tela.zona(0f, 0f, editX - 4f, btnY - 4f, abrir)
            tela.botoesUnidade(btnY, btnH, unidade, largura - pad * 2f, pad, paleta)
        }

        else -> {
            val pad = 14f
            val menor = min(largura, altura)
            val maxTexto = largura - pad * 2f

            val btnTam = 22f
            val editX = pad - 2f
            val topY = pad - 2f
            tela.retangulo(editX, topY, btnTam, btnTam, btnTam / 2f, paleta.pillSuave.copy(alpha = 0.85f))
            tela.lapis(editX + btnTam / 2f, topY + btnTam / 2f, 9f, TintaSuavePastel)
            tela.zona(0f, 0f, editX + btnTam + 4f, topY + btnTam + 6f, abrirConfig)

            val heartX = largura - pad - btnTam + 2f
            if (mostrarPing) {
                tela.retangulo(heartX, topY, btnTam, btnTam, btnTam / 2f, paleta.pillSuave)
                tela.retangulo(heartX, topY, btnTam, btnTam, btnTam / 2f, paleta.bordaPrincipal.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
                tela.coracao(heartX + btnTam / 2f, topY + btnTam / 2f, 9.5f, OcrePastel)
                tela.zona(heartX - 4f, 0f, btnTam + pad + 4f, topY + btnTam + 6f, acaoPing)
            }

            val pNum = tela.fonteAjustada(
                numero,
                TintaPastel,
                R.font.cormorant_garamond_semibold,
                (menor * 0.38f).coerceIn(24f, 54f),
                18f,
                maxTexto
            )
            val pPal = tela.fonte(
                if (statusPing != null) OcrePastel else TintaPastel,
                if (statusPing != null) R.font.montserrat_semibold else R.font.cormorant_garamond_regular,
                if (statusPing != null) 9.5f else (menor * 0.15f).coerceIn(12.5f, 20f)
            )
            val altNum = -pNum.ascent()
            val altPal = -pPal.ascent() + pPal.descent()
            val gap = (menor * 0.04f).coerceIn(3f, 7f)
            val inicioY = (altura - (altNum + gap + altPal) - 10f) / 2f + 4f
            val yNum = inicioY + altNum
            val yPal = yNum + gap + (-pPal.ascent())

            tela.texto(numero, largura / 2f, yNum, pNum, maxTexto, Paint.Align.CENTER)
            tela.texto(statusPing ?: palavra, largura / 2f, yPal, pPal, maxTexto, Paint.Align.CENTER)

            val idxUnidade = UNIDADES.indexOfFirst { it.first == unidade }.coerceAtLeast(0)
            val yDots = altura - 14f
            val espacoDots = 8f
            val xStart = largura / 2f - espacoDots
            UNIDADES.indices.forEach { idx ->
                val ativo = idx == idxUnidade
                tela.circulo(
                    xStart + idx * espacoDots,
                    yDots,
                    if (ativo) 2.3f else 1.6f,
                    if (ativo) paleta.destaqueAtivo else paleta.bordaPrincipal
                )
            }

            tela.zona(0f, topY + btnTam + 6f, largura, altura - topY - btnTam - 6f, actionRunCallback<CiclarUnidadeCallback>())
        }
    }
}

class ContagemWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = prefsWidgets(context)
        val dados = lerDadosWidget(context)
        val unidade = lerUnidadeWidget(context)
        val paleta = paletaAtual(context)
        val mostrarComp = prefs.getBoolean(CHAVE_MOSTRAR_COMP, true)
        val mostrarPing = prefs.getBoolean(CHAVE_MOSTRAR_PING, true)
        val statusPing = statusRecenteChave(prefs, CHAVE_STATUS_CONTAGEM, CHAVE_STATUS_CONTAGEM_EM, 4 * 60_000L)
        val contagem = dados.inicio?.let { calcularContagem(it, LocalDateTime.now(FUSO_BELEM)) }
        provideContent {
            val tamanho = LocalSize.current
            val larguraDp = if (tamanho.width.value > 1f) tamanho.width.value else 250f
            val alturaDp = if (tamanho.height.value > 1f) tamanho.height.value else 110f
            val tela = remember(
                larguraDp,
                alturaDp,
                unidade,
                paleta.id,
                mostrarComp,
                mostrarPing,
                statusPing,
                dados.inicio,
                contagem?.dias,
                contagem?.composicao
            ) {
                Tela(context, larguraDp, alturaDp).also {
                    desenharContagem(it, dados, unidade, contagem, statusPing, paleta, mostrarComp, mostrarPing)
                }
            }
            TelaWidget(tela, "Contagem de dias")
        }
    }
}

class ContagemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ContagemWidget()
}

private fun desenharEventos(
    tela: Tela,
    eventos: List<EventoWidget>,
    paginaOffset: Int,
    statusEvento: String?,
    paleta: PaletaWidget
) {
    val largura = tela.largura
    val altura = tela.altura
    val raioFundo = min(24f, min(largura, altura) * 0.22f)
    val pad = 16f

    tela.fundoDegrade(raioFundo, paleta.eventosTopo, paleta.eventosBase)
    tela.moldura(paleta.bordaPrincipal)

    val tamTitulo = if (largura < 200f || altura < 160f) 18f else 22f
    val pTitulo = tela.fonte(TintaPastel, R.font.cormorant_garamond_semibold, tamTitulo)
    val tituloY = pad + (-pTitulo.ascent()) + 2f

    val divisorY = tituloY + pTitulo.descent() + 7f
    val topoLista = divisorY + 10f
    val linhaAltura = if (altura < 200f) 44f else 50f
    val seloH = linhaAltura - 8f
    val seloW = if (largura < 200f) 38f else 44f

    val temRodapeStatus = statusEvento != null
    val utilLista = max(linhaAltura, altura - topoLista - pad - (if (temRodapeStatus) 16f else 0f))
    val linhasPorPagina = max(1, (utilLista / linhaAltura).toInt())

    val maxInicio = max(0, eventos.size - linhasPorPagina)
    val inicio = paginaOffset.coerceIn(0, maxInicio)
    val temAnterior = inicio > 0
    val temProximo = inicio + linhasPorPagina < eventos.size
    val mostrarPaginacao = eventos.size > linhasPorPagina

    val navTam = 22f
    val navY = tituloY + pTitulo.ascent() + (pTitulo.descent() - pTitulo.ascent() - navTam) / 2f
    val espacoDireita = if (mostrarPaginacao) 82f else 34f
    tela.texto("Próximas datas", pad + 4f, tituloY, pTitulo, largura - pad * 2f - espacoDireita)

    // Botão "+" para criar nova data direto pelo widget sem abrir o app
    val addX = largura - pad - 4f - navTam
    val pNav = tela.fonte(TintaPastel, R.font.montserrat_semibold, 12f)
    tela.retangulo(addX, navY, navTam, navTam, navTam / 2f, paleta.pillSuave)
    tela.retangulo(addX, navY, navTam, navTam, navTam / 2f, paleta.bordaPrincipal.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
    tela.texto("+", addX + navTam / 2f, tela.baselineCentro(navY + navTam / 2f, pNav), pNav, navTam, Paint.Align.CENTER)
    tela.zona(addX - 3f, navY - 4f, navTam + 6f, navTam + 8f, abrirEditorWidget(tela.contexto, "evento"))

    if (mostrarPaginacao) {
        val nextX = addX - 5f - navTam
        val prevX = nextX - 5f - navTam

        tela.retangulo(prevX, navY, navTam, navTam, navTam / 2f, if (temAnterior) paleta.pillSuave else paleta.trilho.copy(alpha = 0.4f))
        tela.texto("‹", prevX + navTam / 2f, tela.baselineCentro(navY + navTam / 2f, pNav), pNav, navTam, Paint.Align.CENTER)
        if (temAnterior) {
            tela.zona(prevX - 2f, navY - 4f, navTam + 4f, navTam + 8f, actionRunCallback<PaginarEventosCallback>(actionParametersOf(ChaveDelta to -linhasPorPagina)))
        }

        tela.retangulo(nextX, navY, navTam, navTam, navTam / 2f, if (temProximo) paleta.pillSuave else paleta.trilho.copy(alpha = 0.4f))
        tela.texto("›", nextX + navTam / 2f, tela.baselineCentro(navY + navTam / 2f, pNav), pNav, navTam, Paint.Align.CENTER)
        if (temProximo) {
            tela.zona(nextX - 2f, navY - 4f, navTam + 4f, navTam + 8f, actionRunCallback<PaginarEventosCallback>(actionParametersOf(ChaveDelta to linhasPorPagina)))
        }

        tela.zona(0f, 0f, prevX - 6f, divisorY, abrirApp(tela.contexto))
    } else {
        tela.zona(0f, 0f, addX - 6f, divisorY, abrirApp(tela.contexto))
    }

    tela.retangulo(pad + 4f, divisorY, largura - pad * 2f - 8f, 0.8f, 0f, paleta.bordaPrincipal.copy(alpha = 0.85f))

    val visiveis = eventos.drop(inicio).take(linhasPorPagina)
    if (visiveis.isEmpty()) {
        val pVazio = tela.fonte(TintaSuavePastel, R.font.cormorant_garamond_italic, 15.5f)
        tela.texto("Tudo em dia. Toque no + para marcar.", pad + 4f, topoLista + 22f, pVazio, largura - pad * 2f - 8f)
        tela.zona(0f, divisorY, largura, altura - divisorY, abrirEditorWidget(tela.contexto, "evento"))
    }

    val hoje = LocalDate.now(FUSO_BELEM)
    visiveis.forEachIndexed { i, evento ->
        val base = topoLista + i * linhaAltura
        val data = runCatching { LocalDate.parse(evento.data) }.getOrNull()
        val diasFaltando = data?.let { ChronoUnit.DAYS.between(hoje, it) }
        val urgente = diasFaltando != null && diasFaltando in 0L..1L
        val podeConcluir = evento.chave.isNotBlank() && evento.colecao.isNotBlank()

        tela.sombraSuave(pad + 2f, base, seloW, seloH, 9f, alpha = 0.12f)
        tela.retangulo(pad + 2f, base, seloW, seloH, 9f, paleta.destaqueAtivo)
        val pDia = tela.fonte(CremePastel, R.font.cormorant_garamond_semibold, (seloH * 0.48f).coerceIn(15f, 19.5f))
        val pMes = tela.fonte(SeloMesPastel, R.font.montserrat_semibold, (seloH * 0.21f).coerceIn(7.5f, 8.5f))
        val centroSeloX = pad + 2f + seloW / 2f
        tela.texto(
            data?.dayOfMonth?.toString()?.padStart(2, '0') ?: "--",
            centroSeloX,
            tela.baselineCentro(base + seloH * 0.38f, pDia),
            pDia,
            seloW - 4f,
            Paint.Align.CENTER
        )
        tela.texto(
            data?.let { MESES_CURTOS[it.monthValue - 1] }?.uppercase().orEmpty(),
            centroSeloX,
            tela.baselineCentro(base + seloH * 0.78f, pMes),
            pMes,
            seloW - 4f,
            Paint.Align.CENTER
        )

        val checkTam = if (podeConcluir) 24f else 0f
        val checkX = largura - pad - checkTam
        val checkY = base + (seloH - checkTam) / 2f

        val x = pad + 2f + seloW + 10f
        val maxLarg = max(10f, (if (podeConcluir) checkX - 8f else largura - pad) - x)
        val pNome = tela.fonte(TintaPastel, R.font.montserrat_semibold, 12.5f)
        val pTipo = tela.fonte(OcrePastel, R.font.montserrat_semibold, 9.5f)
        val pFalta = tela.fonte(if (urgente) TintaPastel else TintaSuavePastel, if (urgente) R.font.montserrat_semibold else R.font.montserrat_regular, 9.5f)

        tela.texto(evento.titulo.ifBlank { "Sem título" }, x, tela.baselineCentro(base + seloH * 0.32f, pNome), pNome, maxLarg)

        val ySub = tela.baselineCentro(base + seloH * 0.76f, pTipo)
        val tipoW = tela.texto(evento.tipo, x, ySub, pTipo, maxLarg)
        val falta = textoFalta(data)
        if (falta.isNotBlank() && maxLarg - tipoW > 24f) {
            if (urgente) {
                val pillX = x + tipoW + 8f
                val pillTextW = pFalta.measureText(falta)
                val pillW = min(maxLarg - tipoW - 8f, pillTextW + 12f)
                val pillH = 14f
                val pillY = base + seloH * 0.76f - pillH / 2f
                tela.retangulo(pillX, pillY, pillW, pillH, pillH / 2f, UrgenteFundoPastel)
                tela.texto(falta, pillX + pillW / 2f, tela.baselineCentro(pillY + pillH / 2f, pFalta), pFalta, pillW - 4f, Paint.Align.CENTER)
            } else {
                tela.texto("  ·  $falta", x + tipoW, ySub, pFalta, maxLarg - tipoW)
            }
        }

        if (podeConcluir) {
            tela.retangulo(checkX, checkY, checkTam, checkTam, 7f, paleta.pillSuave.copy(alpha = 0.75f))
            tela.retangulo(checkX, checkY, checkTam, checkTam, 7f, paleta.bordaPrincipal, contorno = true, espessura = 0.8f)
            tela.check(checkX + checkTam / 2f, checkY + checkTam / 2f, 11f, OcrePastel)

            tela.zona(0f, base, checkX - 4f, linhaAltura, abrirApp(tela.contexto))
            tela.zona(
                checkX - 4f,
                base,
                checkTam + pad + 4f,
                linhaAltura,
                actionRunCallback<ConcluirEventoCallback>(
                    actionParametersOf(
                        ChaveEventoId to evento.chave,
                        ChaveEventoColecao to evento.colecao,
                        ChaveEventoTitulo to evento.titulo
                    )
                )
            )
        } else {
            tela.zona(0f, base, largura, linhaAltura, abrirApp(tela.contexto))
        }

        if (i < visiveis.size - 1) {
            tela.retangulo(x, base + linhaAltura - 4f, maxLarg, 0.6f, 0f, paleta.bordaPrincipal.copy(alpha = 0.65f))
        }
    }

    if (statusEvento != null) {
        val pStatus = tela.fonte(paleta.destaqueAtivo, R.font.montserrat_semibold, 9.5f)
        tela.texto(
            statusEvento,
            largura / 2f,
            altura - pad + 2f,
            pStatus,
            largura - pad * 2f,
            Paint.Align.CENTER
        )
    }
}

private val MESES_CURTOS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

private fun textoFalta(data: LocalDate?): String {
    if (data == null) return ""
    val dias = ChronoUnit.DAYS.between(LocalDate.now(FUSO_BELEM), data)
    return when {
        dias < 0 -> "já passou"
        dias == 0L -> "hoje"
        dias == 1L -> "amanhã"
        else -> "em $dias dias"
    }
}

class EventosWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = prefsWidgets(context)
        val eventos = lerDadosWidget(context).eventos
        val pagina = prefs.getInt(CHAVE_PAGINA_EVENTOS, 0)
        val paleta = paletaAtual(context)
        val statusEvento = statusRecenteChave(prefs, CHAVE_STATUS_EVENTO, CHAVE_STATUS_EVENTO_EM, 4 * 60_000L)
        provideContent {
            val tamanho = LocalSize.current
            val larguraDp = if (tamanho.width.value > 1f) tamanho.width.value else 250f
            val alturaDp = if (tamanho.height.value > 1f) tamanho.height.value else 180f
            val tela = remember(larguraDp, alturaDp, eventos, pagina, statusEvento, paleta.id) {
                Tela(context, larguraDp, alturaDp).also { desenharEventos(it, eventos, pagina, statusEvento, paleta) }
            }
            TelaWidget(tela, "Próximas datas do casal")
        }
    }
}

class EventosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventosWidget()
}

private fun desenharCartinha(
    tela: Tela,
    dados: DadosWidget,
    status: String?,
    indiceCarta: Int,
    paginaFrases: Int,
    paleta: PaletaWidget,
    frases: List<String>
) {
    val largura = tela.largura
    val altura = tela.altura
    val abrirCarta = abrirEditorWidget(tela.contexto, "carta")
    val abrirTudo = abrirApp(tela.contexto)
    val raioFundo = min(24f, min(largura, altura) * 0.22f)
    val pad = 16f

    tela.fundoDegrade(raioFundo, paleta.cartaTopo, paleta.cartaBase)
    tela.moldura(paleta.bordaSecundaria)

    val grande = largura >= 210f && altura >= 210f
    val media = largura >= 200f && !grande
    val pequena = !grande && !media

    val cartas = dados.cartasRecebidas
    val idxSeguro = if (cartas.isNotEmpty()) indiceCarta.coerceIn(0, cartas.size - 1) else 0
    val cartaAtual = cartas.getOrNull(idxSeguro)

    val paginasFrases = frases.chunked(4).ifEmpty { listOf(FRASES_PADRAO.take(4)) }
    val pagIdxSeguro = paginaFrases.coerceIn(0, paginasFrases.lastIndex)
    val frasesAtuais = paginasFrases[pagIdxSeguro]

    val iconeTam = if (pequena) (min(largura, altura) * 0.16f).coerceIn(15f, 20f) else 22f
    tela.icone(R.drawable.ic_envelope_creme, pad, pad, iconeTam, iconeTam, TintaPastel)

    val temTrocaFrases = !pequena && paginasFrases.size > 1
    val badgeLarg = if (!pequena && dados.cartasNovas > 0) 30f else 0f
    val trocaLarg = if (temTrocaFrases) 30f else 0f
    val xTitulo = pad + iconeTam + if (pequena) 6f else 8f
    val maxTitulo = max(20f, largura - xTitulo - pad - badgeLarg - trocaLarg)
    val pTitulo = tela.fonteAjustada(
        "Cartinha",
        TintaPastel,
        R.font.cormorant_garamond_semibold,
        if (pequena) 19f else 24f,
        13f,
        maxTitulo
    )
    tela.texto("Cartinha", xTitulo, tela.baselineCentro(pad + iconeTam / 2f, pTitulo), pTitulo, maxTitulo)

    var cursorDireita = largura - pad
    if (!pequena && dados.cartasNovas > 0) {
        val bW = 24f
        val bH = 18f
        cursorDireita -= bW
        val bY = pad + (iconeTam - bH) / 2f
        tela.retangulo(cursorDireita, bY, bW, bH, bH / 2f, paleta.destaqueAtivo)
        val pB = tela.fonte(CremePastel, R.font.montserrat_semibold, 9.5f)
        tela.texto("${dados.cartasNovas}", cursorDireita + bW / 2f, tela.baselineCentro(bY + bH / 2f, pB), pB, bW - 4f, Paint.Align.CENTER)
        cursorDireita -= 6f
    }

    if (temTrocaFrases) {
        val pW = 26f
        val pH = 18f
        cursorDireita -= pW
        val pY = pad + (iconeTam - pH) / 2f
        tela.retangulo(cursorDireita, pY, pW, pH, pH / 2f, paleta.pillSuave)
        tela.retangulo(cursorDireita, pY, pW, pH, pH / 2f, paleta.bordaPrincipal.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
        val pPag = tela.fonte(OcrePastel, R.font.montserrat_semibold, 9f)
        tela.texto("${pagIdxSeguro + 1}/${paginasFrases.size}", cursorDireita + pW / 2f, tela.baselineCentro(pY + pH / 2f, pPag), pPag, pW - 2f, Paint.Align.CENTER)
        tela.zona(cursorDireita - 4f, pad - 4f, pW + 8f, iconeTam + 8f, actionRunCallback<AlternarPaginaFrasesCallback>())
    }

    val linha = when {
        status != null -> status
        pequena && dados.cartasNovas == 1 -> "1 nova"
        pequena && dados.cartasNovas > 1 -> "${dados.cartasNovas} novas"
        pequena -> "Toque rápido"
        dados.cartasNovas == 1 -> "1 cartinha para ler"
        dados.cartasNovas > 1 -> "${dados.cartasNovas} cartinhas (${idxSeguro + 1}/${cartas.size})"
        else -> "Envie um carinho em 1 toque"
    }
    val corLinha = if (status != null) OcrePastel else TintaSuavePastel
    val pLinha = tela.fonte(corLinha, R.font.montserrat_semibold, if (pequena) 9.5f else 10f)
    val yLinha = pad + iconeTam + if (pequena) 13f else 14f
    tela.texto(linha, pad, yLinha, pLinha, largura - pad * 2f)

    when {
        grande -> {
            val gap = 7f
            val btnEscreverH = (altura * 0.115f).coerceIn(27f, 32f)
            val btnEscreverY = altura - pad - btnEscreverH
            val btnMsgH = (altura * 0.115f).coerceIn(27f, 32f)
            val gridH = btnMsgH * 2f + gap
            val topoGrid = btnEscreverY - gap - gridH

            val cardY = yLinha + 10f
            val cardH = (topoGrid - 9f - cardY).coerceAtLeast(42f)
            val cardW = largura - pad * 2f

            tela.sombraSuave(pad, cardY, cardW, cardH, 12f, alpha = 0.08f)
            tela.retangulo(pad, cardY, cardW, cardH, 12f, CartaoCartaPastel)
            tela.retangulo(pad, cardY, cardW, cardH, 12f, paleta.bordaSecundaria.copy(alpha = 0.65f), contorno = true, espessura = 0.7f)
            tela.retangulo(pad + 6f, cardY + 8f, 2.5f, cardH - 16f, 2f, paleta.bordaPrincipal)

            val autorTexto = when {
                cartaAtual != null && cartaAtual.de.isNotBlank() -> "de ${cartaAtual.de}"
                cartaAtual != null -> "cartinha recebida"
                else -> "Último recado"
            }
            val pAutor = tela.fonte(OcrePastel, R.font.montserrat_semibold, 9f)
            val trecho = cartaAtual?.texto ?: dados.ultimaTexto.ifBlank { "Nenhum recado por aqui ainda." }
            val pTrecho = tela.fonte(TintaPastel, R.font.cormorant_garamond_italic, (cardH * 0.28f).coerceIn(14.5f, 17.5f))

            val acaoLarg = if (cartaAtual != null) (if (cartas.size > 1) 82f else 44f) else 0f
            val maxCard = max(20f, cardW - 24f - acaoLarg)

            tela.texto(autorTexto, pad + 14f, tela.baselineCentro(cardY + cardH * 0.29f, pAutor), pAutor, maxCard)
            tela.texto(trecho, pad + 14f, tela.baselineCentro(cardY + cardH * 0.69f, pTrecho), pTrecho, cardW - 24f)

            if (cartaAtual != null) {
                var cx = pad + cardW - 8f
                val btnCardH = 20f
                val btnCardY = cardY + 6f

                val curtirW = 36f
                cx -= curtirW
                tela.retangulo(cx, btnCardY, curtirW, btnCardH, btnCardH / 2f, paleta.pillSuave)
                tela.coracao(cx + curtirW / 2f, btnCardY + btnCardH / 2f, 9.5f, OcrePastel)
                tela.zona(
                    cx - 2f,
                    cardY,
                    curtirW + 10f,
                    cardH,
                    actionRunCallback<EnviarCartaCallback>(
                        actionParametersOf(
                            ChaveTextoCarta to "Li e amei sua cartinha",
                            ChaveOrigemStatus to "carta"
                        )
                    )
                )

                if (cartas.size > 1) {
                    val navW = 19f
                    cx -= (navW + 4f)
                    tela.retangulo(cx, btnCardY, navW, btnCardH, btnCardH / 2f, BotaoAzulSuavePastel.copy(alpha = 0.7f))
                    val pNav = tela.fonte(TintaPastel, R.font.montserrat_semibold, 10f)
                    tela.texto("›", cx + navW / 2f, tela.baselineCentro(btnCardY + btnCardH / 2f, pNav), pNav, navW, Paint.Align.CENTER)
                    tela.zona(cx - 2f, cardY, navW + 4f, cardH, actionRunCallback<NavegarCartaCallback>(actionParametersOf(ChaveDelta to 1)))

                    cx -= (navW + 3f)
                    tela.retangulo(cx, btnCardY, navW, btnCardH, btnCardH / 2f, BotaoAzulSuavePastel.copy(alpha = 0.7f))
                    tela.texto("‹", cx + navW / 2f, tela.baselineCentro(btnCardY + btnCardH / 2f, pNav), pNav, navW, Paint.Align.CENTER)
                    tela.zona(cx - 2f, cardY, navW + 4f, cardH, actionRunCallback<NavegarCartaCallback>(actionParametersOf(ChaveDelta to -1)))
                }

                tela.zona(pad, cardY, max(10f, cx - pad - 4f), cardH, abrirTudo)
            } else {
                tela.zona(pad, cardY, cardW, cardH, abrirTudo)
            }

            val colunaLargura = (largura - pad * 2f - gap) / 2f
            val pBtn = tela.fonte(TintaPastel, R.font.montserrat_semibold, 10f)
            frasesAtuais.forEachIndexed { i, frase ->
                val coluna = i % 2
                val linhaIdx = i / 2
                val x = pad + coluna * (colunaLargura + gap)
                val y = topoGrid + linhaIdx * (btnMsgH + gap)
                val raioBtn = btnMsgH / 2f
                tela.sombraSuave(x, y, colunaLargura, btnMsgH, raioBtn, alpha = 0.07f)
                tela.retangulo(x, y, colunaLargura, btnMsgH, raioBtn, paleta.pillSuave)
                tela.retangulo(x, y, colunaLargura, btnMsgH, raioBtn, paleta.bordaPrincipal.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
                tela.texto(frase, x + colunaLargura / 2f, tela.baselineCentro(y + btnMsgH / 2f, pBtn), pBtn, colunaLargura - 10f, Paint.Align.CENTER)
                tela.zona(
                    x,
                    y,
                    colunaLargura,
                    btnMsgH,
                    actionRunCallback<EnviarCartaCallback>(
                        actionParametersOf(ChaveTextoCarta to frase, ChaveOrigemStatus to "carta")
                    )
                )
            }

            val raioEscrever = btnEscreverH / 2f
            tela.retangulo(pad, btnEscreverY, largura - pad * 2f, btnEscreverH, raioEscrever, BotaoAzulSuavePastel)
            tela.retangulo(pad, btnEscreverY, largura - pad * 2f, btnEscreverH, raioEscrever, paleta.bordaSecundaria.copy(alpha = 0.85f), contorno = true, espessura = 0.7f)
            tela.texto(
                "Escrever ou editar frases",
                largura / 2f,
                tela.baselineCentro(btnEscreverY + btnEscreverH / 2f, pBtn),
                pBtn,
                largura - pad * 2f - 14f,
                Paint.Align.CENTER
            )
            tela.zona(pad, btnEscreverY, largura - pad * 2f, btnEscreverH, abrirCarta)
        }

        media -> {
            val gap = 8f
            val btnH = (altura * 0.25f).coerceIn(25f, 30f)
            val btnY = altura - pad - btnH
            val colunaLargura = (largura - pad * 2f - gap) / 2f
            val pBtn = tela.fonte(TintaPastel, R.font.montserrat_semibold, 10f)
            val raioBtn = btnH / 2f

            tela.zona(0f, yLinha - 10f, largura, btnY - yLinha + 6f, abrirCarta)

            frasesAtuais.take(2).forEachIndexed { i, frase ->
                val x = pad + i * (colunaLargura + gap)
                tela.sombraSuave(x, btnY, colunaLargura, btnH, raioBtn, alpha = 0.07f)
                tela.retangulo(x, btnY, colunaLargura, btnH, raioBtn, paleta.pillSuave)
                tela.retangulo(x, btnY, colunaLargura, btnH, raioBtn, paleta.bordaPrincipal.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
                tela.texto(frase, x + colunaLargura / 2f, tela.baselineCentro(btnY + btnH / 2f, pBtn), pBtn, colunaLargura - 10f, Paint.Align.CENTER)
                tela.zona(
                    x,
                    btnY,
                    colunaLargura,
                    btnH,
                    actionRunCallback<EnviarCartaCallback>(
                        actionParametersOf(ChaveTextoCarta to frase, ChaveOrigemStatus to "carta")
                    )
                )
            }
        }

        else -> {
            val gap = 6f
            val btnH = (altura * 0.24f).coerceIn(22f, 26f)
            val btnEscreverY = altura - pad - btnH
            val btnRapidoY = btnEscreverY - gap - btnH
            val raioBtn = btnH / 2f
            val pBtn = tela.fonte(TintaPastel, R.font.montserrat_semibold, 10f)
            val primeiraFrase = frases.firstOrNull() ?: "Te amo"

            tela.retangulo(pad, btnRapidoY, largura - pad * 2f, btnH, raioBtn, paleta.pillSuave)
            tela.retangulo(pad, btnRapidoY, largura - pad * 2f, btnH, raioBtn, paleta.bordaPrincipal.copy(alpha = 0.85f), contorno = true, espessura = 0.7f)
            tela.texto(primeiraFrase, largura / 2f, tela.baselineCentro(btnRapidoY + btnH / 2f, pBtn), pBtn, largura - pad * 2f - 8f, Paint.Align.CENTER)
            tela.zona(
                pad,
                btnRapidoY,
                largura - pad * 2f,
                btnH,
                actionRunCallback<EnviarCartaCallback>(
                    actionParametersOf(ChaveTextoCarta to primeiraFrase, ChaveOrigemStatus to "carta")
                )
            )

            tela.retangulo(pad, btnEscreverY, largura - pad * 2f, btnH, raioBtn, BotaoAzulSuavePastel)
            tela.texto("Escrever", largura / 2f, tela.baselineCentro(btnEscreverY + btnH / 2f, pBtn), pBtn, largura - pad * 2f - 8f, Paint.Align.CENTER)
            tela.zona(0f, 0f, largura, btnRapidoY - 2f, abrirCarta)
            tela.zona(pad, btnEscreverY, largura - pad * 2f, btnH, abrirCarta)
        }
    }
}

class CartaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = prefsWidgets(context)
        val dados = lerDadosWidget(context)
        val status = statusRecenteChave(prefs, CHAVE_STATUS, CHAVE_STATUS_EM)
        val indiceCarta = prefs.getInt(CHAVE_INDICE_CARTA, 0)
        val paginaFrases = prefs.getInt(CHAVE_PAGINA_FRASES, 0)
        val paleta = paletaAtual(context)
        val frases = lerFrasesRapidas(context)
        provideContent {
            val tamanho = LocalSize.current
            val larguraDp = if (tamanho.width.value > 1f) tamanho.width.value else 250f
            val alturaDp = if (tamanho.height.value > 1f) tamanho.height.value else 110f
            val tela = remember(
                larguraDp,
                alturaDp,
                status,
                indiceCarta,
                paginaFrases,
                paleta.id,
                frases,
                dados.cartasNovas,
                dados.ultimaTexto,
                dados.ultimaDe,
                dados.cartasRecebidas
            ) {
                Tela(context, larguraDp, alturaDp).also {
                    desenharCartinha(it, dados, status, indiceCarta, paginaFrases, paleta, frases)
                }
            }
            TelaWidget(tela, "Cartinha")
        }
    }
}

class CartaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CartaWidget()
}

// ============================================================================
// Painel completo de edição e configuração do Widget (Android 15/16 Reconfigurable)
// ============================================================================

class WidgetEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val abaInicial = intent?.getStringExtra("aba_editor") ?: "estilo"

        setContent {
            PainelEditorWidget(
                contexto = this,
                abaInicial = abaInicial,
                aoConcluir = {
                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setResult(Activity.RESULT_OK, result)
                    }
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PainelEditorWidget(
    contexto: Context,
    abaInicial: String,
    aoConcluir: () -> Unit
) {
    val escopo = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val prefs = remember { prefsWidgets(contexto) }

    val fonteSerifa = remember {
        FontFamily(
            Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
            Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
            Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold)
        )
    }

    var aba by rememberSaveable { mutableStateOf(abaInicial) }
    var temaEscolhido by rememberSaveable { mutableStateOf(prefs.getString(CHAVE_TEMA_WIDGET, "creme") ?: "creme") }
    var mostrarComp by rememberSaveable { mutableStateOf(prefs.getBoolean(CHAVE_MOSTRAR_COMP, true)) }
    var mostrarPing by rememberSaveable { mutableStateOf(prefs.getBoolean(CHAVE_MOSTRAR_PING, true)) }

    val frases = remember {
        mutableStateListOf<String>().apply { addAll(lerFrasesRapidas(contexto)) }
    }
    var novaFrase by rememberSaveable { mutableStateOf("") }

    // Campos de envio rápido de cartinha
    var textoCarta by rememberSaveable { mutableStateOf("") }
    var enviandoCarta by remember { mutableStateOf(false) }
    var feedbackCarta by remember { mutableStateOf("") }

    // Campos de criação rápida de data/evento
    var tituloEvento by rememberSaveable { mutableStateOf("") }
    var dataEvento by rememberSaveable { mutableStateOf(LocalDate.now(FUSO_BELEM).plusDays(1).toString()) }
    var colecaoEvento by rememberSaveable { mutableStateOf("coisinhas") }
    var salvandoEvento by remember { mutableStateOf(false) }
    var feedbackEvento by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = aoConcluir,
        sheetState = sheetState,
        containerColor = Color(0xFFFBF8EC),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Personalizar e Agir",
                style = TextStyle(fontFamily = fonteSerifa, fontSize = 28.sp),
                color = TintaPastel
            )

            // Abas do editor rápido
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF1E8D2))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "estilo" to "Visual & Frases",
                    "carta" to "Nova Cartinha",
                    "evento" to "Nova Data"
                ).forEach { (id, rotulo) ->
                    val ativa = aba == id
                    val fundo by animateColorAsState(
                        targetValue = if (ativa) TintaPastel else Color.Transparent,
                        animationSpec = tween(200),
                        label = "abaEditorWidget"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(fundo)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                aba = id
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rotulo,
                            style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                            color = if (ativa) CremePastel else TintaSuavePastel
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = aba,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "conteudoEditorWidget"
            ) { secaoAtual ->
                when (secaoAtual) {
                    "carta" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Escreva uma cartinha sem precisar abrir o aplicativo completo.",
                                style = TextStyle(fontFamily = Sem, fontSize = 12.5.sp),
                                color = TintaSuavePastel
                            )
                            OutlinedTextField(
                                value = textoCarta,
                                onValueChange = { textoCarta = it },
                                label = { Text("Mensagem") },
                                minLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (feedbackCarta.isNotBlank()) {
                                Text(feedbackCarta, fontSize = 12.sp, color = OcrePastel)
                            }
                            Button(
                                enabled = textoCarta.isNotBlank() && !enviandoCarta,
                                onClick = {
                                    val casal = prefs.getString(CHAVE_CASAL, "").orEmpty()
                                    val slot = prefs.getString(CHAVE_SLOT, "").orEmpty()
                                    if (casal.isBlank() || slot.isBlank()) {
                                        feedbackCarta = "Abra o app uma vez para vincular seu casal."
                                        return@Button
                                    }
                                    enviandoCarta = true
                                    escopo.launch {
                                        try {
                                            val para = if (slot == "pessoa1") "pessoa2" else "pessoa1"
                                            FirebaseDatabase.getInstance().getReference("casais/$casal/cartinhas").push()
                                                .setValue(
                                                    mapOf(
                                                        "de" to slot,
                                                        "para" to para,
                                                        "texto" to textoCarta.trim(),
                                                        "em" to ServerValue.TIMESTAMP
                                                    )
                                                )
                                                .await()
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            prefs.edit()
                                                .putString(CHAVE_STATUS, "Cartinha enviada")
                                                .putLong(CHAVE_STATUS_EM, System.currentTimeMillis())
                                                .apply()
                                            CartaWidget().updateAll(contexto)
                                            aoConcluir()
                                        } catch (_: Exception) {
                                            enviandoCarta = false
                                            feedbackCarta = "Sem conexão no momento."
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TintaPastel, contentColor = CremePastel),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (enviandoCarta) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CremePastel, strokeWidth = 2.dp)
                                } else {
                                    Text("Enviar cartinha", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    "evento" -> {
                        val dataValida = dataIsoOuNull(dataEvento) != null
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = tituloEvento,
                                onValueChange = { tituloEvento = it },
                                label = { Text("O que vamos fazer?") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "coisinhas" to "Coisinha",
                                    "viagens" to "Viagem",
                                    "filmes" to "Filme",
                                    "series" to "Série",
                                    "jogos" to "Jogo"
                                ).forEach { (col, rot) ->
                                    FilterChip(
                                        selected = colecaoEvento == col,
                                        onClick = { colecaoEvento = col },
                                        label = { Text(rot, fontSize = 11.5.sp) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = dataEvento,
                                onValueChange = { dataEvento = it },
                                label = { Text("Data (AAAA-MM-DD)") },
                                singleLine = true,
                                isError = !dataValida,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (feedbackEvento.isNotBlank()) {
                                Text(feedbackEvento, fontSize = 12.sp, color = OcrePastel)
                            }
                            Button(
                                enabled = tituloEvento.isNotBlank() && dataValida && !salvandoEvento,
                                onClick = {
                                    val casal = prefs.getString(CHAVE_CASAL, "").orEmpty()
                                    if (casal.isBlank()) {
                                        feedbackEvento = "Abra o app uma vez para vincular seu casal."
                                        return@Button
                                    }
                                    val usuario = FirebaseAuth.getInstance().currentUser
                                    if (usuario == null) {
                                        feedbackEvento = "Entre no app para adicionar eventos."
                                        return@Button
                                    }
                                    salvandoEvento = true
                                    escopo.launch {
                                        try {
                                            val ref = FirebaseDatabase.getInstance()
                                                .getReference("casais/$casal/$colecaoEvento")
                                                .push()
                                            val chaveNova = ref.key.orEmpty()
                                            // As regras exigem autoria na criação (createdBy e createdByUid).
                                            val payload = mutableMapOf<String, Any>(
                                                "title" to tituloEvento.trim(),
                                                "targetDate" to dataEvento.trim(),
                                                "createdBy" to (usuario.email ?: ""),
                                                "createdByUid" to usuario.uid,
                                                "createdAt" to ServerValue.TIMESTAMP
                                            )
                                            if (colecaoEvento == "filmes") {
                                                payload["watched"] = false
                                            } else {
                                                payload["done"] = false
                                            }
                                            if (colecaoEvento == "coisinhas") {
                                                payload["category"] = "visitar"
                                            }
                                            ref.setValue(payload).await()

                                            val atual = lerDadosWidget(contexto)
                                            val rotuloTipo = when (colecaoEvento) {
                                                "viagens" -> "Viagem"
                                                "filmes" -> "Filme"
                                                "series" -> "Série"
                                                "jogos" -> "Jogo"
                                                else -> "Coisinha"
                                            }
                                            val novaLista = (atual.eventos + EventoWidget(
                                                titulo = tituloEvento.trim(),
                                                data = dataEvento.trim(),
                                                tipo = rotuloTipo,
                                                chave = chaveNova,
                                                colecao = colecaoEvento
                                            )).sortedBy { it.data }
                                            salvarDadosWidget(contexto, atual.copy(eventos = novaLista))
                                            EventosWidget().updateAll(contexto)
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            aoConcluir()
                                        } catch (_: Exception) {
                                            salvandoEvento = false
                                            feedbackEvento = "Não foi possível salvar agora."
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TintaPastel, contentColor = CremePastel),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (salvandoEvento) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CremePastel, strokeWidth = 2.dp)
                                } else {
                                    Text("Adicionar ao widget", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "PALETA DOS WIDGETS",
                                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.4.sp),
                                color = OcrePastel
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PALETAS_WIDGET.forEach { pal ->
                                    val sel = temaEscolhido == pal.id
                                    Surface(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            temaEscolhido = pal.id
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = pal.contagemTopo,
                                        border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) TintaPastel else pal.bordaPrincipal),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.linearGradient(listOf(pal.contagemBase, pal.cartaBase)))
                                                    .border(1.dp, pal.bordaPrincipal, CircleShape)
                                            )
                                            Text(
                                                text = pal.nome,
                                                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp),
                                                color = TintaPastel
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mostrar composição (anos e meses)", fontSize = 13.sp, color = TintaPastel)
                                Switch(
                                    checked = mostrarComp,
                                    onCheckedChange = { mostrarComp = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = TintaPastel)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Atalho de carinho na contagem", fontSize = 13.sp, color = TintaPastel)
                                Switch(
                                    checked = mostrarPing,
                                    onCheckedChange = { mostrarPing = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = TintaPastel)
                                )
                            }

                            Text(
                                text = "FRASES RÁPIDAS DA CARTINHA",
                                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.4.sp),
                                color = OcrePastel
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = novaFrase,
                                    onValueChange = { novaFrase = it.take(28) },
                                    placeholder = { Text("Nova frase rápida", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    enabled = novaFrase.isNotBlank() && frases.size < 8,
                                    onClick = {
                                        frases.add(novaFrase.trim())
                                        novaFrase = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TintaPastel, contentColor = CremePastel)
                                ) {
                                    Text("+")
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                frases.forEachIndexed { idx, f ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { if (frases.size > 2) frases.removeAt(idx) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFF3E6C8),
                                            selectedLabelColor = TintaPastel
                                        ),
                                        label = { Text("$f  ×", fontSize = 11.sp) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    prefs.edit()
                                        .putString(CHAVE_TEMA_WIDGET, temaEscolhido)
                                        .putBoolean(CHAVE_MOSTRAR_COMP, mostrarComp)
                                        .putBoolean(CHAVE_MOSTRAR_PING, mostrarPing)
                                        .apply()
                                    salvarFrasesRapidas(contexto, frases)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    escopo.launch {
                                        ContagemWidget().updateAll(contexto)
                                        EventosWidget().updateAll(contexto)
                                        CartaWidget().updateAll(contexto)
                                        aoConcluir()
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TintaPastel, contentColor = CremePastel),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Aplicar nos widgets", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}