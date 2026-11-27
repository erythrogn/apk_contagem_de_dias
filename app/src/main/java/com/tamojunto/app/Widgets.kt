package com.tamojunto.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

// ---- Widgets da tela inicial ----
// Cada widget é uma tela desenhada no estilo do site (papel, azul-tinta, dourado, Cormorant e Montserrat),
// com áreas de toque por cima. Os dados vêm de uma cópia gravada pelo app, então funcionam com o app fechado.
// Limite do Android: widget não roda animação contínua; o conteúdo muda no toque e nas atualizações.

data class EventoWidget(val titulo: String, val data: String, val tipo: String)

data class DadosWidget(
    val inicio: String?,
    val nomeOutra: String,
    val eventos: List<EventoWidget>,
    val casal: String? = null,
    val slot: String? = null,
    val cartasNovas: Int = 0,
    val ultimaTexto: String = "",
    val ultimaDe: String = ""
)

private const val ARQUIVO_WIDGETS = "widgets_casal"
private const val CHAVE_WIDGETS = "dados"
private const val CHAVE_UNIDADE = "unidade"
private const val CHAVE_CASAL = "casal"
private const val CHAVE_SLOT = "slot"
private const val CHAVE_STATUS = "status_carta"
private const val CHAVE_STATUS_EM = "status_carta_em"

// Frases prontas da cartinha: um toque envia, sem abrir o app.
private val MENSAGENS_RAPIDAS = listOf("Pensando em você", "Saudade de você", "Te amo", "Boa noite")

private val ChaveAcao = ActionParameters.Key<String>("acao")
private val ChaveUnidade = ActionParameters.Key<String>("unidade")
private val ChaveTextoCarta = ActionParameters.Key<String>("texto")

// Quem pediu uma cartinha pelo app (ou pelo widget, para escrever com calma): o app abre o formulário.
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
        .put("cartasNovas", dados.cartasNovas)
        .put("ultimaTexto", dados.ultimaTexto)
        .put("ultimaDe", dados.ultimaDe)
    // Casal e slot ficam à parte, para o envio de cartinha funcionar sem o app aberto.
    prefsWidgets(contexto).edit()
        .putString(CHAVE_WIDGETS, json.toString())
        .putString(CHAVE_CASAL, dados.casal ?: "")
        .putString(CHAVE_SLOT, dados.slot ?: "")
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
            EventoWidget(e.optString("t"), e.optString("d"), e.optString("r"))
        }
        DadosWidget(
            inicio = json.optString("inicio").ifBlank { null },
            nomeOutra = json.optString("nomeOutra"),
            eventos = eventos,
            cartasNovas = json.optInt("cartasNovas", 0),
            ultimaTexto = json.optString("ultimaTexto"),
            ultimaDe = json.optString("ultimaDe")
        )
    } catch (erro: Exception) {
        vazio
    }
}

private fun lerUnidadeWidget(contexto: Context): String =
    prefsWidgets(contexto).getString(CHAVE_UNIDADE, "dias") ?: "dias"

// Monta o que os widgets mostram a partir do que está no app. A regra de "Próximas datas" é a mesma da tela inicial.
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
    val recebidas = dados.cartinhas.filter { it.para == eu }
    val ultima = recebidas.firstOrNull()
    return DadosWidget(
        inicio = dados.dataNamoro,
        nomeOutra = nomes[outra].orEmpty(),
        eventos = eventos,
        casal = dados.casalId,
        slot = dados.meuSlot,
        cartasNovas = recebidas.size,
        ultimaTexto = ultima?.texto.orEmpty(),
        ultimaDe = if (ultima != null && ultima.de == outra) nomes[outra].orEmpty() else ""
    )
}

// Grava a cópia e pede que os três widgets se redesenhem.
suspend fun atualizarWidgets(contexto: Context, dados: DadosWidget) {
    salvarDadosWidget(contexto, dados)
    ContagemWidget().updateAll(contexto)
    EventosWidget().updateAll(contexto)
    CartaWidget().updateAll(contexto)
}

// Abre o app. Com carta = true, o formulário de cartinha abre sozinho.
private fun abrirApp(contexto: Context, carta: Boolean = false): Action =
    actionStartActivity(
        ComponentName(contexto, MainActivity::class.java),
        if (carta) actionParametersOf(ChaveAcao to "carta") else actionParametersOf()
    )

// ---- Ações dos widgets ----

// Toque na contagem em célula pequena: dias, meses, anos e de volta a dias.
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

// Toque numa das unidades (dias, meses, anos) na contagem grande.
class SetarUnidadeCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val unidade = parameters[ChaveUnidade] ?: return
        prefsWidgets(context).edit().putString(CHAVE_UNIDADE, unidade).apply()
        ContagemWidget().updateAll(context)
    }
}

// Toque numa frase pronta: envia a cartinha direto para a outra pessoa.
class EnviarCartaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val texto = parameters[ChaveTextoCarta] ?: return
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
                "Cartinha enviada"
            } catch (erro: Exception) {
                "Não foi possível enviar"
            }
        }
        prefs.edit()
            .putString(CHAVE_STATUS, status)
            .putLong(CHAVE_STATUS_EM, System.currentTimeMillis())
            .apply()
        CartaWidget().updateAll(context)
    }
}

// ---- Desenho ----

// Área de toque em unidades de dp, relativa ao canto superior esquerdo do widget.
class ZonaToque(val x: Float, val y: Float, val w: Float, val h: Float, val acao: Action)

// Tela de um widget: o bitmap desenhado e as áreas de toque. Coordenadas em dp; o bitmap usa escala até 2x
// para ficar leve o bastante para o Android transferir ao launcher.
private class Tela(val contexto: Context, val largura: Float, val altura: Float) {
    private val escala = min(contexto.resources.displayMetrics.density, 2f)
    val bitmap: Bitmap = Bitmap.createBitmap(
        max(1, (largura * escala).toInt()),
        max(1, (altura * escala).toInt()),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap).also { it.scale(escala, escala) }
    val zonas = mutableListOf<ZonaToque>()

    fun zona(x: Float, y: Float, w: Float, h: Float, acao: Action) {
        zonas.add(ZonaToque(x, y, w, h, acao))
    }

    fun retangulo(x: Float, y: Float, w: Float, h: Float, raio: Float, cor: Color, contorno: Boolean = false, espessura: Float = 1f) {
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.toArgb()
            style = if (contorno) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = espessura
        }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), raio, raio, pincel)
    }

    // Moldura dupla, como o cartão do site.
    fun moldura(cor: Color) {
        retangulo(5f, 5f, largura - 10f, altura - 10f, 18f, cor, contorno = true, espessura = 1f)
        retangulo(9f, 9f, largura - 18f, altura - 18f, 14f, cor.copy(alpha = 0.45f), contorno = true, espessura = 0.6f)
    }

    fun fonte(cor: Color, fonteRes: Int, tamanho: Float): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cor.toArgb()
        textSize = tamanho
        typeface = ResourcesCompat.getFont(contexto, fonteRes) ?: Typeface.DEFAULT
    }

    // Desenha um texto cortado com reticências se passar do espaço. Devolve a largura usada.
    fun texto(t: String, x: Float, y: Float, pincel: TextPaint, maxLargura: Float, alinha: Paint.Align = Paint.Align.LEFT): Float {
        pincel.textAlign = alinha
        val cortado = TextUtils.ellipsize(t, pincel, max(1f, maxLargura), TextUtils.TruncateAt.END).toString()
        canvas.drawText(cortado, x, y, pincel)
        return pincel.measureText(cortado)
    }

    fun icone(res: Int, x: Float, y: Float, w: Float, h: Float) {
        ResourcesCompat.getDrawable(contexto.resources, res, null)?.let { desenho ->
            desenho.setBounds(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt())
            desenho.draw(canvas)
        }
    }
}

// Corpo do widget: a imagem desenhada e, por cima, as áreas que respondem ao toque.
@Composable
private fun TelaWidget(tela: Tela, descricao: String) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Image(
            provider = ImageProvider(tela.bitmap),
            contentDescription = descricao,
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

// ---- 1. Contagem de dias ----

private val UNIDADES = listOf("dias" to "dias", "meses" to "meses", "anos" to "anos")

// Três botões de unidade. O escolhido fica em azul-tinta; os outros, em papel.
private fun Tela.botoesUnidade(y: Float, altura: Float, unidadeAtual: String, larguraTotal: Float, x0: Float) {
    val espaco = 6f
    val largura = (larguraTotal - espaco * (UNIDADES.size - 1)) / UNIDADES.size
    UNIDADES.forEachIndexed { i, (chave, rotulo) ->
        val x = x0 + i * (largura + espaco)
        val ativa = chave == unidadeAtual
        retangulo(x, y, largura, altura, 9f, if (ativa) Tinta else Papel)
        val pincel = fonte(if (ativa) Creme else Tinta, R.font.montserrat_semibold, 10.5f)
        texto(rotulo, x + largura / 2, y + altura / 2 + 3.5f, pincel, largura - 4f, Paint.Align.CENTER)
        zona(x, y, largura, altura, actionRunCallback<SetarUnidadeCallback>(actionParametersOf(ChaveUnidade to chave)))
    }
}

private fun desenharContagem(tela: Tela, dados: DadosWidget, unidade: String, contagem: Contagem?) {
    val largura = tela.largura
    val altura = tela.altura
    val par = contagem?.let { numeroDaUnidade(it, unidade) }
    val numero = par?.let { formatarMilhar(it.first.toInt()) } ?: "--"
    val palavra = par?.second ?: "dias"
    val desde = dados.inicio?.let { "desde ${dataBr(it)}" } ?: "Defina a data do namoro no site"
    val composicao = contagem?.composicao.orEmpty()
    val abrir = abrirApp(tela.contexto)

    tela.retangulo(0f, 0f, largura, altura, 22f, Ouro)
    tela.moldura(Tinta)

    when {
        // Cartão completo: data, número grande, palavra, composição e as três unidades.
        largura >= 240f && altura >= 240f -> {
            tela.texto(desde, largura / 2, 50f, tela.fonte(Tinta, R.font.cormorant_garamond_italic, 17f), largura - 40f, Paint.Align.CENTER)
            tela.texto(numero, largura / 2, 152f, tela.fonte(Tinta, R.font.cormorant_garamond_semibold, 98f), largura - 40f, Paint.Align.CENTER)
            tela.texto("$palavra juntos", largura / 2, 188f, tela.fonte(Tinta, R.font.cormorant_garamond_regular, 24f), largura - 40f, Paint.Align.CENTER)
            tela.texto(composicao, largura / 2, 212f, tela.fonte(TintaSuave, R.font.montserrat_light, 11f), largura - 40f, Paint.Align.CENTER)
            tela.zona(0f, 0f, largura, altura - 80f, abrir)
            tela.botoesUnidade(altura - 58f, 32f, unidade, largura - 36f, 18f)
        }
        // Cartão largo e baixo: número à esquerda, palavra e composição à direita, unidades embaixo.
        largura >= 240f -> {
            tela.texto(numero, 18f, altura - 52f, tela.fonte(Tinta, R.font.cormorant_garamond_semibold, 64f), 150f)
            tela.texto(palavra + " juntos", 150f, 46f, tela.fonte(Tinta, R.font.cormorant_garamond_regular, 22f), largura - 164f)
            tela.texto(desde, 150f, 62f, tela.fonte(Tinta, R.font.cormorant_garamond_italic, 13f), largura - 164f)
            tela.texto(composicao, 150f, 80f, tela.fonte(TintaSuave, R.font.montserrat_light, 10f), largura - 164f)
            tela.zona(0f, 0f, largura, altura - 40f, abrir)
            tela.botoesUnidade(altura - 34f, 26f, unidade, largura - 36f, 18f)
        }
        // Quadradinho: o número e a palavra centralizados. Um toque troca a unidade.
        else -> {
            tela.texto(numero, largura / 2, altura / 2 + 4f, tela.fonte(Tinta, R.font.cormorant_garamond_semibold, 44f), largura - 16f, Paint.Align.CENTER)
            tela.texto(palavra, largura / 2, altura / 2 + 28f, tela.fonte(Tinta, R.font.cormorant_garamond_regular, 18f), largura - 16f, Paint.Align.CENTER)
            tela.zona(0f, 0f, largura, altura, actionRunCallback<CiclarUnidadeCallback>())
        }
    }
}

class ContagemWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 250.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dados = lerDadosWidget(context)
        val unidade = lerUnidadeWidget(context)
        val contagem = dados.inicio?.let { calcularContagem(it, LocalDateTime.now(FUSO_BELEM)) }
        provideContent {
            val tamanho = LocalSize.current
            val tela = remember(tamanho.width.value, tamanho.height.value, unidade, contagem?.dias) {
                Tela(context, tamanho.width.value, tamanho.height.value).also {
                    desenharContagem(it, dados, unidade, contagem)
                }
            }
            TelaWidget(tela, "Contagem de dias")
        }
    }
}

class ContagemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ContagemWidget()
}

// ---- 2. Próximas datas ----

private fun desenharEventos(tela: Tela, eventos: List<EventoWidget>) {
    val largura = tela.largura
    val altura = tela.altura
    tela.retangulo(0f, 0f, largura, altura, 22f, Papel)
    tela.moldura(Ouro)

    tela.texto("Próximas datas", 22f, 40f, tela.fonte(Tinta, R.font.cormorant_garamond_semibold, 24f), largura - 44f)
    tela.retangulo(22f, 50f, largura - 44f, 1f, 0f, Ouro)

    val linhaAltura = 48f
    val linhas = when {
        altura < 230f -> 2
        altura < 330f -> 3
        else -> 5
    }
    if (eventos.isEmpty()) {
        tela.texto("Nenhuma data marcada ainda.", 22f, 88f, tela.fonte(TintaSuave, R.font.cormorant_garamond_italic, 16f), largura - 44f)
    }
    eventos.take(linhas).forEachIndexed { i, evento ->
        val base = 64f + i * linhaAltura
        val data = runCatching { LocalDate.parse(evento.data) }.getOrNull()

        // Bloco com o dia e o mês, como o selo de data do site.
        tela.retangulo(18f, base, 42f, 40f, 8f, Tinta)
        tela.texto(
            data?.dayOfMonth?.toString()?.padStart(2, '0') ?: "--",
            39f, base + 22f, tela.fonte(Creme, R.font.cormorant_garamond_semibold, 20f), 40f, Paint.Align.CENTER
        )
        tela.texto(
            data?.let { MESES_CURTOS[it.monthValue - 1] }.orEmpty(),
            39f, base + 35f, tela.fonte(Ouro, R.font.montserrat_semibold, 8.5f), 40f, Paint.Align.CENTER
        )

        val x = 70f
        tela.texto(evento.titulo.ifBlank { "Sem título" }, x, base + 16f, tela.fonte(Tinta, R.font.montserrat_semibold, 13f), largura - x - 16f)
        val tipo = tela.texto(evento.tipo, x, base + 33f, tela.fonte(Ocre, R.font.montserrat_semibold, 10f), largura - x - 16f)
        val falta = textoFalta(data)
        if (falta.isNotBlank()) {
            tela.texto("  ·  $falta", x + tipo, base + 33f, tela.fonte(TintaSuave, R.font.montserrat_regular, 10f), largura - x - tipo - 16f)
        }
        if (i < linhas - 1 && i < eventos.size - 1) {
            tela.retangulo(70f, base + linhaAltura - 5f, largura - 88f, 0.6f, 0f, Ouro)
        }
    }
    if (eventos.size > linhas) {
        tela.texto(
            "+${eventos.size - linhas} no app",
            largura - 22f, altura - 14f, tela.fonte(TintaSuave, R.font.montserrat_regular, 10f), 120f, Paint.Align.RIGHT
        )
    }
    tela.zona(0f, 0f, largura, altura, abrirApp(tela.contexto))
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
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(250.dp, 180.dp), DpSize(250.dp, 280.dp), DpSize(340.dp, 380.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val eventos = lerDadosWidget(context).eventos
        provideContent {
            val tamanho = LocalSize.current
            val tela = remember(tamanho.width.value, tamanho.height.value) {
                Tela(context, tamanho.width.value, tamanho.height.value).also { desenharEventos(it, eventos) }
            }
            TelaWidget(tela, "Próximas datas do casal")
        }
    }
}

class EventosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventosWidget()
}

// ---- 3. Cartinha ----

// Só mostra a confirmação de envio por 15 minutos; depois volta o resumo das cartinhas.
private fun statusRecente(prefs: android.content.SharedPreferences): String? {
    val status = prefs.getString(CHAVE_STATUS, null) ?: return null
    val em = prefs.getLong(CHAVE_STATUS_EM, 0L)
    return if (System.currentTimeMillis() - em < 15 * 60_000L) status else null
}

private fun desenharCartinha(tela: Tela, dados: DadosWidget, status: String?) {
    val largura = tela.largura
    val altura = tela.altura
    val abrirCarta = abrirApp(tela.contexto, carta = true)
    val abrirTudo = abrirApp(tela.contexto)

    tela.retangulo(0f, 0f, largura, altura, 22f, Tinta)
    tela.moldura(Ouro)
    tela.icone(R.drawable.ic_envelope_creme, 18f, 18f, 26f, 26f)
    tela.texto("Cartinha", 52f, 40f, tela.fonte(Creme, R.font.cormorant_garamond_semibold, 26f), largura - 66f)

    // Linha de situação: confirmação de envio, ou quantas cartinhas chegaram.
    val linha = when {
        status != null -> status
        dados.cartasNovas == 1 -> "1 cartinha nova para você"
        dados.cartasNovas > 1 -> "${dados.cartasNovas} cartinhas novas para você"
        else -> "Nenhuma cartinha nova"
    }
    val corLinha = if (status != null) Ouro else Creme.copy(alpha = 0.8f)
    tela.texto(linha, 18f, 62f, tela.fonte(corLinha, R.font.montserrat_semibold, 10.5f), largura - 36f)

    val grande = largura >= 240f && altura >= 240f
    val media = largura >= 240f && !grande

    when {
        grande -> {
            // Última cartinha recebida, em destaque.
            tela.retangulo(18f, 76f, largura - 36f, 56f, 10f, Creme.copy(alpha = 0.1f))
            val autor = if (dados.ultimaDe.isNotBlank()) "de ${dados.ultimaDe}" else "Chegue aqui quando ela chegar"
            tela.texto(autor, 30f, 94f, tela.fonte(Creme.copy(alpha = 0.75f), R.font.montserrat_light, 9.5f), largura - 60f)
            val trecho = dados.ultimaTexto.ifBlank { "Nenhum recado por aqui ainda." }
            tela.texto(trecho, 30f, 116f, tela.fonte(Creme, R.font.cormorant_garamond_italic, 17f), largura - 60f)

            // Frases prontas em grade de duas colunas.
            val colunaLargura = (largura - 36f - 8f) / 2f
            val topo = altura - 118f
            MENSAGENS_RAPIDAS.forEachIndexed { i, frase ->
                val coluna = i % 2
                val linhaIdx = i / 2
                val x = 18f + coluna * (colunaLargura + 8f)
                val y = topo + linhaIdx * 34f
                tela.retangulo(x, y, colunaLargura, 28f, 9f, Ouro)
                tela.texto(frase, x + colunaLargura / 2, y + 18f, tela.fonte(Tinta, R.font.montserrat_semibold, 10.5f), colunaLargura - 8f, Paint.Align.CENTER)
                tela.zona(x, y, colunaLargura, 28f, actionRunCallback<EnviarCartaCallback>(actionParametersOf(ChaveTextoCarta to frase)))
            }
            tela.retangulo(18f, altura - 40f, largura - 36f, 26f, 9f, Creme.copy(alpha = 0.14f))
            tela.texto("Escrever uma cartinha com calma", largura / 2, altura - 23f, tela.fonte(Creme, R.font.montserrat_semibold, 10.5f), largura - 50f, Paint.Align.CENTER)
            tela.zona(0f, 0f, largura, altura, abrirTudo)
            tela.zona(18f, altura - 40f, largura - 36f, 26f, abrirCarta)
        }
        media -> {
            // Duas frases prontas numa linha e o botão de escrever.
            val colunaLargura = (largura - 36f - 8f) / 2f
            MENSAGENS_RAPIDAS.take(2).forEachIndexed { i, frase ->
                val x = 18f + i * (colunaLargura + 8f)
                val y = altura - 40f
                tela.retangulo(x, y, colunaLargura, 28f, 9f, Ouro)
                tela.texto(frase, x + colunaLargura / 2, y + 18f, tela.fonte(Tinta, R.font.montserrat_semibold, 10.5f), colunaLargura - 8f, Paint.Align.CENTER)
                tela.zona(x, y, colunaLargura, 28f, actionRunCallback<EnviarCartaCallback>(actionParametersOf(ChaveTextoCarta to frase)))
            }
            tela.zona(0f, 0f, largura, altura - 46f, abrirTudo)
        }
        else -> {
            // Quadradinho: só o botão de escrever no app.
            tela.retangulo(18f, altura - 40f, largura - 36f, 26f, 9f, Ouro)
            tela.texto("Escrever", largura / 2, altura - 23f, tela.fonte(Tinta, R.font.montserrat_semibold, 11f), largura - 40f, Paint.Align.CENTER)
            tela.zona(0f, 0f, largura, altura, abrirCarta)
        }
    }
}

class CartaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 250.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dados = lerDadosWidget(context)
        val status = statusRecente(prefsWidgets(context))
        provideContent {
            val tamanho = LocalSize.current
            val tela = remember(tamanho.width.value, tamanho.height.value, status, dados.cartasNovas, dados.ultimaTexto) {
                Tela(context, tamanho.width.value, tamanho.height.value).also {
                    desenharCartinha(it, dados, status)
                }
            }
            TelaWidget(tela, "Cartinha")
        }
    }
}

class CartaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CartaWidget()
}
