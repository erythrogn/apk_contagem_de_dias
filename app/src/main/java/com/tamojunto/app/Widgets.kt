package com.tamojunto.app

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
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import androidx.glance.layout.ContentScale
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
import kotlin.math.sqrt
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

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

private val FundoContagemTopo = Color(0xFFF9F2E2)
private val FundoContagemBase = Color(0xFFEFE1C3)
private val FundoEventosTopo = Color(0xFFFCF9F2)
private val FundoEventosBase = Color(0xFFF4EEDF)
private val FundoCartaTopo = Color(0xFFEAF2FA)
private val FundoCartaBase = Color(0xFFD8E6F5)

private val CartaoCartaPastel = Color(0xD9FFFFFF)
private val TrilhoSegmentoPastel = Color(0xFFE8D9B8)
private val BotaoOuroPastel = Color(0xFFF6E9CC)
private val BotaoAzulSuavePastel = Color(0xFFC5D8EE)
private val BotaoAtivoPastel = Color(0xFF4B6482)
private val BordaOuroPastel = Color(0xFFD2B984)
private val BordaAzulPastel = Color(0xFFA2BAD6)
private val TintaPastel = Color(0xFF2E425B)
private val TintaSuavePastel = Color(0xFF5F738C)
private val CremePastel = Color(0xFFFCF9F2)
private val OcrePastel = Color(0xFF8F6B38)
private val SeloMesPastel = Color(0xFFF4E4C1)
private val UrgenteFundoPastel = Color(0xFFF3DFBA)

private val MENSAGENS_RAPIDAS = listOf("Pensando em você", "Saudade de você", "Te amo", "Boa noite")

private val ChaveAcao = ActionParameters.Key<String>("acao")
private val ChaveUnidade = ActionParameters.Key<String>("unidade")
private val ChaveTextoCarta = ActionParameters.Key<String>("texto")

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

class SetarUnidadeCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val unidade = parameters[ChaveUnidade] ?: return
        prefsWidgets(context).edit().putString(CHAVE_UNIDADE, unidade).apply()
        ContagemWidget().updateAll(context)
    }
}

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

class ZonaToque(val x: Float, val y: Float, val w: Float, val h: Float, val acao: Action)

private class Tela(val contexto: Context, val largura: Float, val altura: Float) {
    private val escala = run {
        val densidade = min(contexto.resources.displayMetrics.density, 2f)
        val area = max(1f, largura * altura)
        min(densidade, sqrt(350_000f / area)).coerceAtLeast(0.75f)
    }
    val bitmap: Bitmap = Bitmap.createBitmap(
        max(1, (largura * escala).toInt()),
        max(1, (altura * escala).toInt()),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap).also { it.scale(escala, escala) }
    val zonas = mutableListOf<ZonaToque>()

    fun zona(x: Float, y: Float, w: Float, h: Float, acao: Action) {
        if (w <= 0f || h <= 0f) return
        zonas.add(ZonaToque(x.coerceAtLeast(0f), y.coerceAtLeast(0f), w, h, acao))
    }

    fun retangulo(x: Float, y: Float, w: Float, h: Float, raio: Float, cor: Color, contorno: Boolean = false, espessura: Float = 1f) {
        if (w <= 0f || h <= 0f) return
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.toArgb()
            style = if (contorno) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = espessura
        }
        val r = min(raio, min(w, h) / 2f)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), r, r, pincel)
    }

    fun fundoDegrade(raio: Float, topo: Color, base: Color) {
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    fun circulo(cx: Float, cy: Float, raio: Float, cor: Color) {
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cor.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, raio, pincel)
    }

    fun fonte(cor: Color, fonteRes: Int, tamanho: Float): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
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

private fun Tela.botoesUnidade(y: Float, altura: Float, unidadeAtual: String, larguraTotal: Float, x0: Float) {
    val raioTrack = altura / 2f
    retangulo(x0, y, larguraTotal, altura, raioTrack, TrilhoSegmentoPastel.copy(alpha = 0.72f))
    retangulo(x0, y, larguraTotal, altura, raioTrack, BordaOuroPastel.copy(alpha = 0.65f), contorno = true, espessura = 0.7f)

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
            retangulo(x, yItem, larguraItem, alturaItem, raioPill, BotaoAtivoPastel)
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

private fun desenharContagem(tela: Tela, dados: DadosWidget, unidade: String, contagem: Contagem?) {
    val largura = tela.largura
    val altura = tela.altura
    val par = contagem?.let { numeroDaUnidade(it, unidade) }
    val numero = par?.let { formatarMilhar(it.first.toInt()) } ?: "--"
    val palavra = par?.second ?: "dias"
    val desde = dados.inicio?.let { "desde ${dataBr(it)}" } ?: "Defina a data do namoro no site"
    val composicao = contagem?.composicao.orEmpty()
    val abrir = abrirApp(tela.contexto)
    val raioFundo = min(24f, min(largura, altura) * 0.22f)

    tela.fundoDegrade(raioFundo, FundoContagemTopo, FundoContagemBase)
    tela.moldura(BordaOuroPastel)

    when {
        largura >= 210f && altura >= 200f -> {
            val pad = 18f
            val btnH = (altura * 0.13f).coerceIn(28f, 34f)
            val btnY = altura - pad - btnH
            val areaTopo = pad + 4f
            val areaBase = btnY - 12f
            val maxTexto = largura - pad * 2f - 8f

            val pDesde = tela.fonte(OcrePastel, R.font.cormorant_garamond_italic, (altura * 0.066f).coerceIn(13.5f, 17.5f))
            val yDesde = areaTopo + (-pDesde.ascent())
            tela.texto(desde, largura / 2f, yDesde, pDesde, maxTexto, Paint.Align.CENTER)

            val yOrnamento = yDesde + pDesde.descent() + 6f
            tela.ornamentoCentral(largura / 2f, yOrnamento, min(96f, largura * 0.36f), BordaOuroPastel)

            val temComp = composicao.isNotBlank()
            val pComp = tela.fonte(TintaSuavePastel, R.font.montserrat_regular, (altura * 0.042f).coerceIn(9.5f, 11.5f))
            val yComp = areaBase - pComp.descent()
            if (temComp) {
                tela.texto(composicao, largura / 2f, yComp, pComp, maxTexto, Paint.Align.CENTER)
            }

            val meioTopo = yOrnamento + 6f
            val meioBase = if (temComp) yComp + pComp.ascent() - 6f else areaBase
            val meioAltura = max(40f, meioBase - meioTopo)

            val pNum = tela.fonteAjustada(
                numero,
                TintaPastel,
                R.font.cormorant_garamond_semibold,
                (meioAltura * 0.66f).coerceIn(40f, 94f),
                26f,
                maxTexto
            )
            val pPal = tela.fonte(TintaPastel, R.font.cormorant_garamond_regular, (meioAltura * 0.19f).coerceIn(15f, 23f))
            val altNum = -pNum.ascent()
            val altPal = -pPal.ascent() + pPal.descent()
            val gap = (meioAltura * 0.05f).coerceIn(4f, 9f)
            val blocoH = altNum + gap + altPal
            val inicioBloco = meioTopo + (meioAltura - blocoH) / 2f
            val yNum = inicioBloco + altNum
            val yPal = yNum + gap + (-pPal.ascent())

            tela.texto(numero, largura / 2f, yNum, pNum, maxTexto, Paint.Align.CENTER)
            tela.texto("$palavra juntos", largura / 2f, yPal, pPal, maxTexto, Paint.Align.CENTER)

            tela.zona(0f, 0f, largura, btnY - 4f, abrir)
            tela.botoesUnidade(btnY, btnH, unidade, largura - pad * 2f, pad)
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
            val colDir = largura - xDir - pad

            val pNum = tela.fonteAjustada(
                numero,
                TintaPastel,
                R.font.cormorant_garamond_semibold,
                (areaH * 0.94f).coerceIn(28f, 60f),
                20f,
                colEsq
            )
            tela.texto(numero, pad + colEsq / 2f, tela.baselineCentro(areaTopo + areaH / 2f, pNum), pNum, colEsq, Paint.Align.CENTER)

            tela.retangulo(pad + colEsq + gapCol / 2f, areaTopo + 4f, 0.8f, areaH - 8f, 0f, BordaOuroPastel.copy(alpha = 0.55f))

            val mostrarComp = composicao.isNotBlank() && areaH >= 44f
            val pPal = tela.fonte(TintaPastel, R.font.cormorant_garamond_regular, (areaH * 0.34f).coerceIn(14f, 21f))
            val pDesde = tela.fonte(OcrePastel, R.font.cormorant_garamond_italic, (areaH * 0.24f).coerceIn(11f, 14f))

            if (mostrarComp) {
                val pComp = tela.fonte(TintaSuavePastel, R.font.montserrat_regular, (areaH * 0.18f).coerceIn(9f, 10.5f))
                tela.texto("$palavra juntos", xDir, tela.baselineCentro(areaTopo + areaH * 0.23f, pPal), pPal, colDir)
                tela.texto(desde, xDir, tela.baselineCentro(areaTopo + areaH * 0.56f, pDesde), pDesde, colDir)
                tela.texto(composicao, xDir, tela.baselineCentro(areaTopo + areaH * 0.85f, pComp), pComp, colDir)
            } else {
                tela.texto("$palavra juntos", xDir, tela.baselineCentro(areaTopo + areaH * 0.34f, pPal), pPal, colDir)
                tela.texto(desde, xDir, tela.baselineCentro(areaTopo + areaH * 0.72f, pDesde), pDesde, colDir)
            }

            tela.zona(0f, 0f, largura, btnY - 4f, abrir)
            tela.botoesUnidade(btnY, btnH, unidade, largura - pad * 2f, pad)
        }

        else -> {
            val pad = 14f
            val menor = min(largura, altura)
            val maxTexto = largura - pad * 2f
            val pNum = tela.fonteAjustada(
                numero,
                TintaPastel,
                R.font.cormorant_garamond_semibold,
                (menor * 0.38f).coerceIn(24f, 54f),
                18f,
                maxTexto
            )
            val pPal = tela.fonte(TintaPastel, R.font.cormorant_garamond_regular, (menor * 0.15f).coerceIn(12.5f, 20f))
            val altNum = -pNum.ascent()
            val altPal = -pPal.ascent() + pPal.descent()
            val gap = (menor * 0.04f).coerceIn(3f, 7f)
            val inicioY = (altura - (altNum + gap + altPal) - 10f) / 2f
            val yNum = inicioY + altNum
            val yPal = yNum + gap + (-pPal.ascent())

            tela.texto(numero, largura / 2f, yNum, pNum, maxTexto, Paint.Align.CENTER)
            tela.texto(palavra, largura / 2f, yPal, pPal, maxTexto, Paint.Align.CENTER)

            val idxUnidade = UNIDADES.indexOfFirst { it.first == unidade }.coerceAtLeast(0)
            val yDots = altura - 15f
            val espacoDots = 8f
            val xStart = largura / 2f - espacoDots
            UNIDADES.indices.forEach { idx ->
                val ativo = idx == idxUnidade
                tela.circulo(
                    xStart + idx * espacoDots,
                    yDots,
                    if (ativo) 2.3f else 1.6f,
                    if (ativo) BotaoAtivoPastel else BordaOuroPastel
                )
            }

            tela.zona(0f, 0f, largura, altura, actionRunCallback<CiclarUnidadeCallback>())
        }
    }
}

class ContagemWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dados = lerDadosWidget(context)
        val unidade = lerUnidadeWidget(context)
        val contagem = dados.inicio?.let { calcularContagem(it, LocalDateTime.now(FUSO_BELEM)) }
        provideContent {
            val tamanho = LocalSize.current
            val larguraDp = if (tamanho.width.value > 1f) tamanho.width.value else 250f
            val alturaDp = if (tamanho.height.value > 1f) tamanho.height.value else 110f
            val tela = remember(larguraDp, alturaDp, unidade, dados.inicio, contagem?.dias, contagem?.composicao) {
                Tela(context, larguraDp, alturaDp).also {
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

private fun desenharEventos(tela: Tela, eventos: List<EventoWidget>) {
    val largura = tela.largura
    val altura = tela.altura
    val raioFundo = min(24f, min(largura, altura) * 0.22f)
    val pad = 16f

    tela.fundoDegrade(raioFundo, FundoEventosTopo, FundoEventosBase)
    tela.moldura(BordaOuroPastel)

    val tamTitulo = if (largura < 200f || altura < 160f) 18f else 22f
    val pTitulo = tela.fonte(TintaPastel, R.font.cormorant_garamond_semibold, tamTitulo)
    val tituloY = pad + (-pTitulo.ascent()) + 2f
    tela.texto("Próximas datas", pad + 4f, tituloY, pTitulo, largura - pad * 2f - 48f)

    if (eventos.isNotEmpty()) {
        val badgeW = 28f
        val badgeH = 18f
        val badgeX = largura - pad - 4f - badgeW
        val badgeY = tituloY + pTitulo.ascent() + (pTitulo.descent() - pTitulo.ascent() - badgeH) / 2f
        tela.retangulo(badgeX, badgeY, badgeW, badgeH, badgeH / 2f, TrilhoSegmentoPastel)
        val pBadge = tela.fonte(TintaPastel, R.font.montserrat_semibold, 9.5f)
        tela.texto("${eventos.size}", badgeX + badgeW / 2f, tela.baselineCentro(badgeY + badgeH / 2f, pBadge), pBadge, badgeW - 4f, Paint.Align.CENTER)
    }

    val divisorY = tituloY + pTitulo.descent() + 7f
    tela.retangulo(pad + 4f, divisorY, largura - pad * 2f - 8f, 0.8f, 0f, BordaOuroPastel.copy(alpha = 0.85f))

    val topoLista = divisorY + 10f
    val linhaAltura = if (altura < 200f) 44f else 50f
    val seloH = linhaAltura - 8f
    val seloW = if (largura < 200f) 38f else 44f

    val utilSemRodape = max(linhaAltura, altura - topoLista - pad)
    val maxSemRodape = max(1, (utilSemRodape / linhaAltura).toInt())
    val linhas = if (eventos.size > maxSemRodape) {
        val utilComRodape = max(linhaAltura, altura - topoLista - pad - 16f)
        max(1, (utilComRodape / linhaAltura).toInt())
    } else {
        maxSemRodape
    }

    if (eventos.isEmpty()) {
        val pVazio = tela.fonte(TintaSuavePastel, R.font.cormorant_garamond_italic, 15.5f)
        tela.texto("Nenhuma data marcada ainda.", pad + 4f, topoLista + 22f, pVazio, largura - pad * 2f - 8f)
    }

    val hoje = LocalDate.now(FUSO_BELEM)
    eventos.take(linhas).forEachIndexed { i, evento ->
        val base = topoLista + i * linhaAltura
        val data = runCatching { LocalDate.parse(evento.data) }.getOrNull()
        val diasFaltando = data?.let { ChronoUnit.DAYS.between(hoje, it) }
        val urgente = diasFaltando != null && diasFaltando in 0L..1L

        tela.sombraSuave(pad + 2f, base, seloW, seloH, 9f, alpha = 0.12f)
        tela.retangulo(pad + 2f, base, seloW, seloH, 9f, BotaoAtivoPastel)
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

        val x = pad + 2f + seloW + 10f
        val maxLarg = max(10f, largura - x - pad)
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

        if (i < linhas - 1 && i < eventos.size - 1) {
            tela.retangulo(x, base + linhaAltura - 4f, maxLarg, 0.6f, 0f, BordaOuroPastel.copy(alpha = 0.65f))
        }
    }

    if (eventos.size > linhas) {
        val pMais = tela.fonte(OcrePastel, R.font.montserrat_semibold, 9.5f)
        tela.texto(
            "+${eventos.size - linhas} no app",
            largura - pad - 4f,
            altura - pad + 2f,
            pMais,
            120f,
            Paint.Align.RIGHT
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
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val eventos = lerDadosWidget(context).eventos
        provideContent {
            val tamanho = LocalSize.current
            val larguraDp = if (tamanho.width.value > 1f) tamanho.width.value else 250f
            val alturaDp = if (tamanho.height.value > 1f) tamanho.height.value else 180f
            val tela = remember(larguraDp, alturaDp, eventos) {
                Tela(context, larguraDp, alturaDp).also { desenharEventos(it, eventos) }
            }
            TelaWidget(tela, "Próximas datas do casal")
        }
    }
}

class EventosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventosWidget()
}

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
    val raioFundo = min(24f, min(largura, altura) * 0.22f)
    val pad = 16f

    tela.fundoDegrade(raioFundo, FundoCartaTopo, FundoCartaBase)
    tela.moldura(BordaAzulPastel)

    val grande = largura >= 210f && altura >= 210f
    val media = largura >= 200f && !grande
    val pequena = !grande && !media

    val iconeTam = if (pequena) (min(largura, altura) * 0.16f).coerceIn(15f, 20f) else 22f
    tela.icone(R.drawable.ic_envelope_creme, pad, pad, iconeTam, iconeTam, TintaPastel)

    val badgeLarg = if (!pequena && dados.cartasNovas > 0) 32f else 0f
    val xTitulo = pad + iconeTam + if (pequena) 6f else 8f
    val maxTitulo = max(20f, largura - xTitulo - pad - badgeLarg)
    val pTitulo = tela.fonteAjustada(
        "Cartinha",
        TintaPastel,
        R.font.cormorant_garamond_semibold,
        if (pequena) 19f else 24f,
        13f,
        maxTitulo
    )
    tela.texto("Cartinha", xTitulo, tela.baselineCentro(pad + iconeTam / 2f, pTitulo), pTitulo, maxTitulo)

    if (!pequena && dados.cartasNovas > 0) {
        val bW = 24f
        val bH = 18f
        val bX = largura - pad - bW
        val bY = pad + (iconeTam - bH) / 2f
        tela.retangulo(bX, bY, bW, bH, bH / 2f, BotaoAtivoPastel)
        val pB = tela.fonte(CremePastel, R.font.montserrat_semibold, 9.5f)
        tela.texto("${dados.cartasNovas}", bX + bW / 2f, tela.baselineCentro(bY + bH / 2f, pB), pB, bW - 4f, Paint.Align.CENTER)
    }

    val linha = when {
        status != null -> status
        pequena && dados.cartasNovas == 1 -> "1 nova"
        pequena && dados.cartasNovas > 1 -> "${dados.cartasNovas} novas"
        pequena -> "Sem novas"
        dados.cartasNovas == 1 -> "1 cartinha nova para você"
        dados.cartasNovas > 1 -> "${dados.cartasNovas} cartinhas novas para você"
        else -> "Toque para enviar um carinho"
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
            val cardH = (topoGrid - 9f - cardY).coerceAtLeast(40f)

            tela.sombraSuave(pad, cardY, largura - pad * 2f, cardH, 12f, alpha = 0.08f)
            tela.retangulo(pad, cardY, largura - pad * 2f, cardH, 12f, CartaoCartaPastel)
            tela.retangulo(pad, cardY, largura - pad * 2f, cardH, 12f, BordaAzulPastel.copy(alpha = 0.65f), contorno = true, espessura = 0.7f)
            tela.retangulo(pad + 6f, cardY + 8f, 2.5f, cardH - 16f, 2f, BordaOuroPastel)

            val autor = if (dados.ultimaDe.isNotBlank()) "de ${dados.ultimaDe}" else "Último recado"
            val pAutor = tela.fonte(OcrePastel, R.font.montserrat_semibold, 9f)
            val trecho = dados.ultimaTexto.ifBlank { "Nenhum recado por aqui ainda." }
            val pTrecho = tela.fonte(TintaPastel, R.font.cormorant_garamond_italic, (cardH * 0.28f).coerceIn(14.5f, 17.5f))
            val maxCard = largura - pad * 2f - 24f

            tela.texto(autor, pad + 14f, tela.baselineCentro(cardY + cardH * 0.30f, pAutor), pAutor, maxCard)
            tela.texto(trecho, pad + 14f, tela.baselineCentro(cardY + cardH * 0.69f, pTrecho), pTrecho, maxCard)

            tela.zona(0f, 0f, largura, topoGrid - 4f, abrirTudo)

            val colunaLargura = (largura - pad * 2f - gap) / 2f
            val pBtn = tela.fonte(TintaPastel, R.font.montserrat_semibold, 10f)
            MENSAGENS_RAPIDAS.forEachIndexed { i, frase ->
                val coluna = i % 2
                val linhaIdx = i / 2
                val x = pad + coluna * (colunaLargura + gap)
                val y = topoGrid + linhaIdx * (btnMsgH + gap)
                val raioBtn = btnMsgH / 2f
                tela.sombraSuave(x, y, colunaLargura, btnMsgH, raioBtn, alpha = 0.07f)
                tela.retangulo(x, y, colunaLargura, btnMsgH, raioBtn, BotaoOuroPastel)
                tela.retangulo(x, y, colunaLargura, btnMsgH, raioBtn, BordaOuroPastel.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
                tela.texto(frase, x + colunaLargura / 2f, tela.baselineCentro(y + btnMsgH / 2f, pBtn), pBtn, colunaLargura - 10f, Paint.Align.CENTER)
                tela.zona(x, y, colunaLargura, btnMsgH, actionRunCallback<EnviarCartaCallback>(actionParametersOf(ChaveTextoCarta to frase)))
            }

            val raioEscrever = btnEscreverH / 2f
            tela.retangulo(pad, btnEscreverY, largura - pad * 2f, btnEscreverH, raioEscrever, BotaoAzulSuavePastel)
            tela.retangulo(pad, btnEscreverY, largura - pad * 2f, btnEscreverH, raioEscrever, BordaAzulPastel.copy(alpha = 0.85f), contorno = true, espessura = 0.7f)
            tela.texto(
                "Escrever uma cartinha com calma",
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

            tela.zona(0f, 0f, largura, btnY - 4f, abrirTudo)

            MENSAGENS_RAPIDAS.take(2).forEachIndexed { i, frase ->
                val x = pad + i * (colunaLargura + gap)
                tela.sombraSuave(x, btnY, colunaLargura, btnH, raioBtn, alpha = 0.07f)
                tela.retangulo(x, btnY, colunaLargura, btnH, raioBtn, BotaoOuroPastel)
                tela.retangulo(x, btnY, colunaLargura, btnH, raioBtn, BordaOuroPastel.copy(alpha = 0.8f), contorno = true, espessura = 0.7f)
                tela.texto(frase, x + colunaLargura / 2f, tela.baselineCentro(btnY + btnH / 2f, pBtn), pBtn, colunaLargura - 10f, Paint.Align.CENTER)
                tela.zona(x, btnY, colunaLargura, btnH, actionRunCallback<EnviarCartaCallback>(actionParametersOf(ChaveTextoCarta to frase)))
            }
        }

        else -> {
            val btnH = (altura * 0.25f).coerceIn(23f, 28f)
            val btnY = altura - pad - btnH
            val raioBtn = btnH / 2f
            val pBtn = tela.fonte(TintaPastel, R.font.montserrat_semibold, 10.5f)
            tela.sombraSuave(pad, btnY, largura - pad * 2f, btnH, raioBtn, alpha = 0.08f)
            tela.retangulo(pad, btnY, largura - pad * 2f, btnH, raioBtn, BotaoOuroPastel)
            tela.retangulo(pad, btnY, largura - pad * 2f, btnH, raioBtn, BordaOuroPastel.copy(alpha = 0.85f), contorno = true, espessura = 0.7f)
            tela.texto("Escrever", largura / 2f, tela.baselineCentro(btnY + btnH / 2f, pBtn), pBtn, largura - pad * 2f - 8f, Paint.Align.CENTER)
            tela.zona(0f, 0f, largura, altura, abrirCarta)
        }
    }
}

class CartaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dados = lerDadosWidget(context)
        val status = statusRecente(prefsWidgets(context))
        provideContent {
            val tamanho = LocalSize.current
            val larguraDp = if (tamanho.width.value > 1f) tamanho.width.value else 250f
            val alturaDp = if (tamanho.height.value > 1f) tamanho.height.value else 110f
            val tela = remember(larguraDp, alturaDp, status, dados.cartasNovas, dados.ultimaTexto, dados.ultimaDe) {
                Tela(context, larguraDp, alturaDp).also {
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
