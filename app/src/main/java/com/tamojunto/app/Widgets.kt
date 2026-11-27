package com.tamojunto.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import androidx.glance.Image
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.json.JSONArray
import org.json.JSONObject

// ---- Widgets da tela inicial ----
// Os três leem uma cópia dos dados gravada pelo app (SharedPreferences), então funcionam com o app fechado.
// Cada um tem cor e layout próprios: a contagem em dourado, os eventos em papel, a cartinha em azul-tinta.

data class EventoWidget(val titulo: String, val data: String, val tipo: String)

data class DadosWidget(val inicio: String?, val nomeOutra: String, val eventos: List<EventoWidget>)

private const val ARQUIVO_WIDGETS = "widgets_casal"
private const val CHAVE_WIDGETS = "dados"

// Quem pediu uma cartinha pelo widget: o app abre o formulário assim que a tela aparece.
object PedidosWidget {
    var cartaPendente by mutableStateOf(false)

    fun lerIntent(intent: Intent?) {
        if (intent?.getStringExtra("acao") == "carta") cartaPendente = true
    }
}

fun salvarDadosWidget(contexto: Context, dados: DadosWidget) {
    val eventos = JSONArray()
    dados.eventos.forEach { evento ->
        eventos.put(JSONObject().put("t", evento.titulo).put("d", evento.data).put("r", evento.tipo))
    }
    val json = JSONObject()
        .put("inicio", dados.inicio ?: "")
        .put("nomeOutra", dados.nomeOutra)
        .put("eventos", eventos)
    contexto.getSharedPreferences(ARQUIVO_WIDGETS, Context.MODE_PRIVATE)
        .edit()
        .putString(CHAVE_WIDGETS, json.toString())
        .apply()
}

fun lerDadosWidget(contexto: Context): DadosWidget {
    val vazio = DadosWidget(null, "", emptyList())
    val texto = contexto.getSharedPreferences(ARQUIVO_WIDGETS, Context.MODE_PRIVATE)
        .getString(CHAVE_WIDGETS, null) ?: return vazio
    return try {
        val json = JSONObject(texto)
        val lista = json.optJSONArray("eventos") ?: JSONArray()
        val eventos = (0 until lista.length()).map { i ->
            val e = lista.getJSONObject(i)
            EventoWidget(e.optString("t"), e.optString("d"), e.optString("r"))
        }
        DadosWidget(
            inicio = json.optString("inicio").ifBlank { null },
            nomeOutra = json.optString("nomeOutra"),
            eventos = eventos
        )
    } catch (erro: Exception) {
        vazio
    }
}

// Monta o que os widgets mostram a partir do que está no app. Mesma regra de "Próximas datas" da tela inicial.
fun montarDadosWidget(dados: DadosDoCasal, nomes: Map<String, String>): DadosWidget {
    val eu = dados.meuSlot ?: "pessoa1"
    val outra = if (eu == "pessoa1") "pessoa2" else "pessoa1"
    val hoje = LocalDate.now(FUSO_BELEM).toString()
    val fontes: List<Pair<String, List<Item>>> = listOf(
        "Viagem" to dados.viagens.toList(),
        "Filme" to dados.filmes.toList(),
        "Série" to dados.series.toList(),
        "Jogo" to dados.jogos.toList(),
        "Coisinha" to dados.coisinhas.toList(),
        "Plano" to dados.planos.toList()
    )
    val eventos = fontes.flatMap { (rotulo, itens) ->
        itens
            .filter { !it.feito && it.dataIso.isNotBlank() && it.dataIso >= hoje }
            .map { EventoWidget(it.titulo, it.dataIso, rotulo) }
    }.sortedBy { it.data }.take(4)
    return DadosWidget(
        inicio = dados.dataNamoro,
        nomeOutra = nomes[outra].orEmpty(),
        eventos = eventos
    )
}

// Grava a cópia e pede que os três widgets se redesenhem.
suspend fun atualizarWidgets(contexto: Context, dados: DadosWidget) {
    salvarDadosWidget(contexto, dados)
    ContagemWidget().updateAll(contexto)
    EventosWidget().updateAll(contexto)
    CartaWidget().updateAll(contexto)
}

private fun abrirApp(contexto: Context, carta: Boolean = false): Action =
    actionStartActivity(
        Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (carta) putExtra("acao", "carta")
        }
    )

private fun estilo(tamanho: Int, cor: Color, negrito: Boolean = false, serifa: Boolean = false) = TextStyle(
    color = ColorProvider(cor),
    fontSize = tamanho.sp,
    fontWeight = if (negrito) FontWeight.Bold else FontWeight.Normal,
    fontFamily = if (serifa) FontFamily.Serif else FontFamily.SansSerif
)

private val MESES_CURTOS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

// Envelope desenhado no mesmo traço fino dos ícones do app (viewBox 24).
private fun iconeEnvelope(cor: Int, tamanhoPx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cor
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val escala = tamanhoPx / 24f
    canvas.scale(escala, escala)
    listOf("M3 5h18v14H3z", "M3 5l9 8 9-8").forEach { caminho ->
        canvas.drawPath(PathParser.createPathFromPathData(caminho), paint)
    }
    return bitmap
}

// ---- 1. Contagem de dias: bloco dourado, número grande em serifa ----

class ContagemWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val inicio = lerDadosWidget(context).inicio
        val contagem = inicio?.let { calcularContagem(it, LocalDateTime.now(FUSO_BELEM)) }
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Ouro)
                    .padding(16.dp)
                    .clickable(onClick = abrirApp(context))
            ) {
                Text(
                    if (inicio == null) "Defina a data do namoro no site." else "desde ${dataBr(inicio)}",
                    style = estilo(11, Tinta)
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    contagem?.let { formatarMilhar(it.dias.toInt()) } ?: "--",
                    style = estilo(44, Tinta, negrito = true, serifa = true)
                )
                Text("dias juntos", style = estilo(16, Tinta, serifa = true))
                Spacer(GlanceModifier.height(6.dp))
                Text(contagem?.composicao.orEmpty(), style = estilo(11, TintaSuave), maxLines = 2)
            }
        }
    }
}

class ContagemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ContagemWidget()
}

// ---- 2. Próximas datas: papel, com um bloco azul para o dia de cada evento ----

class EventosWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val eventos = lerDadosWidget(context).eventos
        provideContent {
            // Em tela alta cabem três linhas; em tela baixa, duas.
            val linhas = if (LocalSize.current.height >= 200.dp) 3 else 2
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Papel)
                    .padding(14.dp)
                    .clickable(onClick = abrirApp(context))
            ) {
                Text("Próximas datas", style = estilo(15, Tinta, negrito = true, serifa = true))
                Text("Os próximos compromissos", style = estilo(11, TintaSuave))
                Spacer(GlanceModifier.height(8.dp))
                if (eventos.isEmpty()) {
                    Text("Nenhuma data marcada ainda.", style = estilo(12, TintaSuave))
                } else {
                    eventos.take(linhas).forEach { evento -> LinhaEvento(evento) }
                }
            }
        }
    }
}

@Composable
private fun LinhaEvento(evento: EventoWidget) {
    val data = runCatching { LocalDate.parse(evento.data) }.getOrNull()
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(start = 0.dp, top = 3.dp, end = 0.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = GlanceModifier.width(40.dp).background(Tinta).padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                data?.dayOfMonth?.toString()?.padStart(2, '0') ?: "--",
                style = estilo(17, Creme, negrito = true, serifa = true)
            )
            Text(data?.let { MESES_CURTOS[it.monthValue - 1] }.orEmpty(), style = estilo(10, Ouro))
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(evento.titulo.ifBlank { "Sem título" }, style = estilo(13, Tinta, negrito = true), maxLines = 1)
            Text(textoFalta(data), style = estilo(11, TintaSuave), maxLines = 1)
        }
    }
}

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

class EventosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventosWidget()
}

// ---- 3. Cartinha: azul-tinta, com o envelope e um botão que abre o formulário no app ----

class CartaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nome = lerDadosWidget(context).nomeOutra
        val icone = ImageProvider(iconeEnvelope(Creme.toArgb(), 96))
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Tinta)
                    .padding(16.dp)
                    .clickable(onClick = abrirApp(context, carta = true))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(provider = icone, contentDescription = null, modifier = GlanceModifier.size(26.dp))
                    Spacer(GlanceModifier.width(8.dp))
                    Text("Cartinha", style = estilo(18, Creme, negrito = true, serifa = true))
                }
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    if (nome.isBlank()) "Escreva uma cartinha" else "Para $nome",
                    style = estilo(12, Creme),
                    maxLines = 1
                )
                Spacer(GlanceModifier.defaultWeight())
                Box(
                    modifier = GlanceModifier
                        .background(Ouro)
                        .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 8.dp)
                ) {
                    Text("Escrever agora", style = estilo(13, Tinta, negrito = true))
                }
            }
        }
    }
}

class CartaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CartaWidget()
}
