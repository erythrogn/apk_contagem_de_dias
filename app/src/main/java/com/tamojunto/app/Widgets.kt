package com.tamojunto.app

import android.content.ComponentName
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import kotlin.math.ceil
import org.json.JSONArray
import org.json.JSONObject

// ---- Widgets da tela inicial ----
// Os três leem uma cópia dos dados gravada pelo app (SharedPreferences), então funcionam com o app fechado.
// Cada um se adapta ao tamanho da célula: de um quadradinho até um cartão grande.
// Limite do Android: widget não roda animação contínua. A mudança acontece no toque e a cada atualização.

data class EventoWidget(val titulo: String, val data: String, val tipo: String)

data class DadosWidget(val inicio: String?, val nomeOutra: String, val eventos: List<EventoWidget>)

private const val ARQUIVO_WIDGETS = "widgets_casal"
private const val CHAVE_WIDGETS = "dados"
private const val CHAVE_UNIDADE = "unidade"

// Quem pediu uma cartinha pelo widget: o app abre o formulário assim que a tela aparece.
object PedidosWidget {
    var cartaPendente by mutableStateOf(false)

    fun lerIntent(intent: Intent?) {
        if (intent?.getStringExtra("acao") == "carta") cartaPendente = true
    }
}

private fun prefsWidgets(contexto: Context) =
    contexto.getSharedPreferences(ARQUIVO_WIDGETS, Context.MODE_PRIVATE)

fun salvarDadosWidget(contexto: Context, dados: DadosWidget) {
    val eventos = JSONArray()
    dados.eventos.forEach { evento ->
        eventos.put(JSONObject().put("t", evento.titulo).put("d", evento.data).put("r", evento.tipo))
    }
    val json = JSONObject()
        .put("inicio", dados.inicio ?: "")
        .put("nomeOutra", dados.nomeOutra)
        .put("eventos", eventos)
    prefsWidgets(contexto).edit().putString(CHAVE_WIDGETS, json.toString()).apply()
}

fun lerDadosWidget(contexto: Context): DadosWidget {
    val vazio = DadosWidget(null, "", emptyList())
    val texto = prefsWidgets(contexto).getString(CHAVE_WIDGETS, null) ?: return vazio
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

// A unidade da contagem (dias, meses ou anos) é escolhida no próprio widget, com um toque.
private fun lerUnidadeWidget(contexto: Context): String =
    prefsWidgets(contexto).getString(CHAVE_UNIDADE, "dias") ?: "dias"

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
    }.sortedBy { it.data }.take(8)
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

// Abre o app. Com carta = true, o pedido segue como parâmetro "acao" para o formulário abrir sozinho.
private fun abrirApp(contexto: Context, carta: Boolean = false): Action =
    actionStartActivity(
        ComponentName(contexto, MainActivity::class.java),
        if (carta) actionParametersOf(ChaveAcao to "carta") else actionParametersOf()
    )

private val ChaveAcao = ActionParameters.Key<String>("acao")

// Toque na contagem: passa de dias para meses, de meses para anos, e volta a dias.
class CiclarUnidadeCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val proxima = when (lerUnidadeWidget(context)) {
            "dias" -> "meses"
            "meses" -> "anos"
            else -> "dias"
        }
        prefsWidgets(context).edit().putString(CHAVE_UNIDADE, proxima).apply()
        ContagemWidget().updateAll(context)
    }
}

private fun estilo(tamanho: Int, cor: Color, negrito: Boolean = false, serifa: Boolean = false) = TextStyle(
    color = ColorProvider(cor),
    fontSize = tamanho.sp,
    fontWeight = if (negrito) FontWeight.Bold else FontWeight.Normal,
    fontFamily = if (serifa) FontFamily.Serif else FontFamily.SansSerif
)

private val MESES_CURTOS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

// Texto na fonte do site (Cormorant), desenhado num bitmap. O Glance não aceita as fontes do app,
// então os títulos dos widgets são imagens feitas com o mesmo arquivo .ttf que já está no projeto.
class RotuloImagem(val imagem: ImageProvider, val largura: Dp, val altura: Dp, val descricao: String)

private fun rotuloImagem(contexto: Context, texto: String, fonte: Int, tamanhoSp: Float, cor: Color): RotuloImagem {
    val metricas = contexto.resources.displayMetrics
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(contexto, fonte)
        textSize = tamanhoSp * metricas.scaledDensity
        color = cor.toArgb()
    }
    val medidas = paint.fontMetrics
    val largura = ceil(paint.measureText(texto)).toInt().coerceAtLeast(1)
    val altura = ceil(medidas.descent - medidas.ascent).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawText(texto, 0f, -medidas.ascent, paint)
    return RotuloImagem(
        imagem = ImageProvider(bitmap),
        largura = (largura / metricas.density).dp,
        altura = (altura / metricas.density).dp,
        descricao = texto
    )
}

@Composable
private fun ImagemRotulo(rotulo: RotuloImagem) {
    Image(
        provider = rotulo.imagem,
        contentDescription = rotulo.descricao,
        modifier = GlanceModifier.width(rotulo.largura).height(rotulo.altura)
    )
}

// ---- 1. Contagem de dias: bloco dourado. Pequeno, largo ou alto, cada um mostra o que cabe. ----

class ContagemWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 250.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dados = lerDadosWidget(context)
        val unidade = lerUnidadeWidget(context)
        val contagem = dados.inicio?.let { calcularContagem(it, LocalDateTime.now(FUSO_BELEM)) }
        val par = contagem?.let { numeroDaUnidade(it, unidade) }
        val numeroGrande = rotuloImagem(
            context, par?.let { formatarMilhar(it.first.toInt()) } ?: "--",
            R.font.cormorant_garamond_semibold, 56f, Tinta
        )
        val numeroPequeno = rotuloImagem(
            context, par?.let { formatarMilhar(it.first.toInt()) } ?: "--",
            R.font.cormorant_garamond_semibold, 40f, Tinta
        )
        val palavra = rotuloImagem(context, par?.second ?: "dias", R.font.cormorant_garamond_regular, 20f, Tinta)
        val desde = rotuloImagem(
            context,
            dados.inicio?.let { "desde ${dataBr(it)}" } ?: "Defina a data do namoro no site",
            R.font.cormorant_garamond_italic, 15f, Tinta
        )
        val composicao = contagem?.composicao.orEmpty()

        provideContent {
            val tamanho = LocalSize.current
            val largo = tamanho.width >= 240.dp
            val alto = tamanho.height >= 240.dp
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(Ouro)
                    .padding(14.dp)
                    .clickable(onClick = actionRunCallback<CiclarUnidadeCallback>())
            ) {
                when {
                    // Alto e largo: o cartão completo, com a data, o número e a composição.
                    alto && largo -> {
                        ImagemRotulo(desde)
                        Spacer(GlanceModifier.height(4.dp))
                        ImagemRotulo(numeroGrande)
                        ImagemRotulo(palavra)
                        Spacer(GlanceModifier.defaultWeight())
                        Text(composicao, style = estilo(12, TintaSuave), maxLines = 2)
                    }
                    // Largo e baixo: número à esquerda, a palavra e a composição à direita.
                    largo -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ImagemRotulo(numeroGrande)
                            Spacer(GlanceModifier.width(12.dp))
                            Column {
                                ImagemRotulo(palavra)
                                Text(composicao, style = estilo(11, TintaSuave), maxLines = 2)
                            }
                        }
                    }
                    // Quadradinho: o número e a palavra, centralizados.
                    else -> {
                        ImagemRotulo(numeroPequeno)
                        ImagemRotulo(palavra)
                    }
                }
            }
        }
    }
}

class ContagemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ContagemWidget()
}

// ---- 2. Próximas datas: papel, com um bloco azul para o dia de cada evento. Mostra mais linhas conforme cresce. ----

class EventosWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(250.dp, 180.dp), DpSize(250.dp, 280.dp), DpSize(340.dp, 380.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val eventos = lerDadosWidget(context).eventos
        val titulo = rotuloImagem(context, "Próximas datas", R.font.cormorant_garamond_semibold, 22f, Tinta)
        provideContent {
            val alto = LocalSize.current.height
            val linhas = when {
                alto < 230.dp -> 2
                alto < 330.dp -> 3
                else -> 5
            }
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(Papel)
                    .padding(14.dp)
                    .clickable(onClick = abrirApp(context))
            ) {
                ImagemRotulo(titulo)
                if (alto >= 230.dp) {
                    Text("Os próximos compromissos", style = estilo(11, TintaSuave))
                }
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
        // Filete dourado à esquerda: o detalhe de marca que o site usa nos cartões.
        Box(modifier = GlanceModifier.width(3.dp).height(34.dp).background(Ouro)) {}
        Spacer(GlanceModifier.width(8.dp))
        Column(
            modifier = GlanceModifier.width(40.dp).cornerRadius(8.dp).background(Tinta).padding(4.dp),
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
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nome = lerDadosWidget(context).nomeOutra
        val icone = ImageProvider(R.drawable.ic_envelope_creme)
        val titulo = rotuloImagem(context, "Cartinha", R.font.cormorant_garamond_semibold, 24f, Creme)
        provideContent {
            val largo = LocalSize.current.width >= 200.dp
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(Tinta)
                    .padding(14.dp)
                    .clickable(onClick = abrirApp(context, carta = true))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(provider = icone, contentDescription = null, modifier = GlanceModifier.size(24.dp))
                    Spacer(GlanceModifier.width(8.dp))
                    ImagemRotulo(titulo)
                }
                if (largo) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        if (nome.isBlank()) "Escreva uma cartinha" else "Para $nome",
                        style = estilo(12, Creme),
                        maxLines = 1
                    )
                }
                Spacer(GlanceModifier.defaultWeight())
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(12.dp)
                        .background(Ouro)
                        .padding(start = 12.dp, top = 7.dp, end = 12.dp, bottom = 7.dp)
                ) {
                    Text(
                        if (largo) "Escrever agora" else "Escrever",
                        style = estilo(13, Tinta, negrito = true)
                    )
                }
            }
        }
    }
}

class CartaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CartaWidget()
}
