package com.tamojunto.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

val FUSO_BELEM: ZoneId = ZoneId.of("America/Belem")

// ---- Dados que o servidor calcula (/api/inicio): o mesmo Laço e o mesmo resumo do dia do site ----

data class AtividadeLaco(val rotulo: String, val xp: Int)
data class RegraLaco(val categoria: String, val limite: Int, val atividades: List<AtividadeLaco>)
data class LimiteHoje(val categoria: String, val usado: Int, val limite: Int)

data class LacoApp(
    val nivel: Int,
    val total: Int,
    val fase: String,
    val significado: String,
    val prestigio: Boolean,
    val percentual: Int,
    val xp: Int,
    val falta: Int,
    val proximoNivel: Int?,
    val regras: List<RegraLaco>,
    val limitesHoje: List<LimiteHoje>
)

data class MissaoHoje(val titulo: String, val xp: Int, val expiraTexto: String)
data class PedidoAtencao(val titulo: String, val quem: String)
data class ResumoHoje(val missao: MissaoHoje?, val esperandoVoce: PedidoAtencao?)
data class FotoCasal(val nome: String, val url: String)

data class InicioApp(
    val dataIso: String,
    val dataExtenso: String,
    val fotos: List<FotoCasal>,
    val laco: LacoApp?,
    val hoje: ResumoHoje?,
    val eu: String,
    val nomes: Map<String, String>
)

private fun JSONObject.texto(campo: String): String = optString(campo, "")
private fun JSONObject.inteiro(campo: String): Int = optInt(campo, 0)
private fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

private fun JSONObject.paraLaco(): LacoApp = LacoApp(
    nivel = inteiro("nivel"),
    total = inteiro("total"),
    fase = texto("fase"),
    significado = texto("significado"),
    prestigio = optBoolean("prestigio", false),
    percentual = inteiro("percentual"),
    xp = inteiro("xp"),
    falta = inteiro("falta"),
    proximoNivel = if (isNull("proximo_nivel")) null else getInt("proximo_nivel"),
    regras = optJSONArray("regras")?.objetos()?.map { r ->
        RegraLaco(
            categoria = r.texto("categoria"),
            limite = r.inteiro("limite"),
            atividades = r.optJSONArray("atividades")
                ?.objetos()
                ?.map { AtividadeLaco(it.texto("rotulo"), it.inteiro("xp")) }
                .orEmpty()
        )
    }.orEmpty(),
    limitesHoje = optJSONArray("limites_hoje")?.objetos()?.map {
        LimiteHoje(it.texto("categoria"), it.inteiro("usado"), it.inteiro("limite"))
    }.orEmpty()
)

private fun JSONObject.paraHoje(): ResumoHoje = ResumoHoje(
    missao = optJSONObject("missao")?.let { MissaoHoje(it.texto("titulo"), it.inteiro("xp"), it.texto("expira_texto")) },
    esperandoVoce = optJSONObject("esperando_voce")?.let { PedidoAtencao(it.texto("titulo"), it.texto("quem")) }
)

object InicioServidor {
    // Devolve os dados, ou a mensagem do servidor para mostrar na tela.
    suspend fun carregar(): Pair<InicioApp?, String> {
        val r = Servidor.obter("/api/inicio")
        val j = r.corpo
        if (!r.ok || j == null) return null to r.msg
        val inicio = j.getJSONObject("inicio")
        val nomes = j.optJSONObject("nomes")
        val fotos = j.optJSONArray("fotos")?.objetos()?.map { FotoCasal(it.texto("nome"), it.texto("url")) }.orEmpty()
        return InicioApp(
            dataIso = inicio.texto("data_iso"),
            dataExtenso = inicio.texto("data_extenso"),
            fotos = fotos,
            laco = if (j.isNull("laco")) null else j.getJSONObject("laco").paraLaco(),
            hoje = if (j.isNull("hoje")) null else j.getJSONObject("hoje").paraHoje(),
            eu = j.texto("eu"),
            nomes = mapOf("pessoa1" to nomes?.texto("pessoa1").orEmpty(), "pessoa2" to nomes?.texto("pessoa2").orEmpty())
        ) to ""
    }
}

// ---- Contagem (mesma conta do contagem.js: começa à meia-noite de Belém do dia do namoro) ----

class Contagem(
    val dias: Long,
    val meses: Long,
    val anos: Long,
    val composicao: String,
    val horas: Long,
    val minutos: Long,
    val segundos: Long
)

private fun plural(n: Long, um: String, varios: String) = if (n == 1L) um else varios

fun calcularContagem(dataIso: String, agora: LocalDateTime): Contagem? {
    val inicio = runCatching { LocalDate.parse(dataIso.take(10)) }.getOrNull() ?: return null
    val comeco = inicio.atStartOfDay()
    if (agora.isBefore(comeco)) return null
    val diff = Duration.between(comeco, agora)
    val totalSeg = diff.seconds
    val dias = diff.toDays() + 1

    var meses = (agora.year - comeco.year) * 12L + (agora.monthValue - comeco.monthValue)
    if (comeco.plusMonths(meses).isAfter(agora)) meses -= 1
    val diasNoMes = Duration.between(comeco.plusMonths(meses), agora).toDays()
    val anos = meses / 12
    val mesesResto = meses % 12

    val partes = mutableListOf<String>()
    if (anos > 0) partes += "$anos ${plural(anos, "ano", "anos")}"
    if (mesesResto > 0) partes += "$mesesResto ${plural(mesesResto, "mês", "meses")}"
    if (diasNoMes > 0 || partes.isEmpty()) partes += "$diasNoMes ${plural(diasNoMes, "dia", "dias")}"
    val composicao = if (partes.size > 1) partes.dropLast(1).joinToString(", ") + " e " + partes.last() else partes.first()

    return Contagem(dias, meses, anos, composicao, (totalSeg / 3600) % 24, (totalSeg / 60) % 60, totalSeg % 60)
}

// O número grande e a palavra, conforme a unidade escolhida (dias, meses ou anos).
fun numeroDaUnidade(c: Contagem, unidade: String): Pair<Long, String> = when (unidade) {
    "meses" -> c.meses to plural(c.meses, "mês", "meses")
    "anos" -> c.anos to plural(c.anos, "ano", "anos")
    else -> c.dias to plural(c.dias, "dia", "dias")
}

// ---- Distância e tempo (juntos-comum.js) ----

fun distanciaKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val raio = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return 2 * raio * asin(sqrt(h))
}

fun textoDistancia(km: Double): String = when {
    km < 1 -> "${max(1, (km * 1000).roundToInt())} m"
    km < 100 -> String.format(Locale("pt", "BR"), "%.1f km", km)
    else -> String.format(Locale("pt", "BR"), "%,d km", km.roundToInt())
}

// "agora", "há 5 min", "há 2 h"... Vazio quando não há momento registrado.
fun tempoAtras(quando: Long): String {
    if (quando <= 0L) return ""
    val minutos = (System.currentTimeMillis() - quando) / 60_000
    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "há $minutos min"
        minutos < 60 * 24 -> "há ${minutos / 60} h"
        else -> "há ${minutos / (60 * 24)} dias"
    }
}

fun rotuloComTempo(rotulo: String, quando: Long): String {
    val tempo = tempoAtras(quando)
    return if (tempo.isBlank()) rotulo else "$rotulo · $tempo"
}

fun formatarMilhar(n: Int): String = String.format(Locale("pt", "BR"), "%,d", n)

// Os seis humores do site. Cada um vira um símbolo de tempo, como no site.
data class OpcaoHumor(val id: String, val rotulo: String, val simbolo: String)

val OPCOES_HUMOR = listOf(
    OpcaoHumor("feliz", "Feliz", "☀"),
    OpcaoHumor("apaixonado", "Apaixonado(a)", "♥"),
    OpcaoHumor("tranquilo", "Tranquilo(a)", "☁"),
    OpcaoHumor("cansado", "Cansado(a)", "≋"),
    OpcaoHumor("triste", "Triste", "☂"),
    OpcaoHumor("estressado", "Estressado(a)", "⚡")
)
