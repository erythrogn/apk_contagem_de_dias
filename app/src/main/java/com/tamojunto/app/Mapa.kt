package com.tamojunto.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

// Mapa nativo, sem WebView: os azulejos do OpenStreetMap (o mesmo fundo do site) entram como imagens,
// e os pontos, a linha tracejada e os nomes são desenhados em Compose. Projeção Web Mercator.
data class PontoMapa(val nome: String, val lat: Double, val lng: Double)

private const val AZULEJO = 256.0

private fun xMundo(lng: Double, zoom: Int): Double = (lng + 180.0) / 360.0 * AZULEJO * 2.0.pow(zoom)

private fun yMundo(lat: Double, zoom: Int): Double {
    val r = Math.toRadians(lat)
    return (1.0 - ln(tan(r) + 1.0 / cos(r)) / PI) / 2.0 * AZULEJO * 2.0.pow(zoom)
}

// O maior zoom em que todos os pontos cabem na tela com folga.
private fun escolherZoom(pontos: List<PontoMapa>, largura: Double, altura: Double): Int {
    for (z in 16 downTo 2) {
        val xs = pontos.map { xMundo(it.lng, z) }
        val ys = pontos.map { yMundo(it.lat, z) }
        val dx = (xs.maxOrNull() ?: 0.0) - (xs.minOrNull() ?: 0.0)
        val dy = (ys.maxOrNull() ?: 0.0) - (ys.minOrNull() ?: 0.0)
        if (dx < largura * 0.6 && dy < altura * 0.6) return z
    }
    return 2
}

@Composable
fun MapaNativo(pontos: List<PontoMapa>, modifier: Modifier = Modifier) {
    if (pontos.isEmpty()) return
    val contexto = LocalContext.current
    BoxWithConstraints(modifier.clipToBounds().background(Color(0xFFD6E2EA))) {
        val largura = maxWidth.value.toDouble()
        val altura = maxHeight.value.toDouble()
        val zoom = escolherZoom(pontos, largura, altura)
        // Unidade do mundo = 1 dp. O centro do mapa é o meio da área ocupada pelos pontos.
        val xs = pontos.map { xMundo(it.lng, zoom) }
        val ys = pontos.map { yMundo(it.lat, zoom) }
        val centroX = (xs.maxOrNull()!! + xs.minOrNull()!!) / 2
        val centroY = (ys.maxOrNull()!! + ys.minOrNull()!!) / 2
        val esquerda = centroX - largura / 2
        val topo = centroY - altura / 2
        val nQuadros = 2.0.pow(zoom).toInt()

        val txMin = floor(esquerda / AZULEJO).toInt()
        val txMax = floor((esquerda + largura) / AZULEJO).toInt()
        val tyMin = maxOf(0, floor(topo / AZULEJO).toInt())
        val tyMax = minOf(nQuadros - 1, floor((topo + altura) / AZULEJO).toInt())

        for (ty in tyMin..tyMax) {
            for (tx in txMin..txMax) {
                val xNormal = ((tx % nQuadros) + nQuadros) % nQuadros
                val pedido = ImageRequest.Builder(contexto)
                    .data("https://tile.openstreetmap.org/$zoom/$xNormal/$ty.png")
                    .addHeader("User-Agent", "ContagemDeDias/1.0 (app Android)")
                    .crossfade(false)
                    .build()
                AsyncImage(
                    model = pedido,
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = (tx * AZULEJO - esquerda).toFloat().dp, y = (ty * AZULEJO - topo).toFloat().dp)
                        .size(256.dp)
                )
            }
        }

        // Posições dos pontos na tela, em dp.
        val marcas = pontos.map { p ->
            Triple(
                p,
                (xMundo(p.lng, zoom) - esquerda).toFloat(),
                (yMundo(p.lat, zoom) - topo).toFloat()
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val k = 1.dp.toPx()
            // Com duas pessoas, a linha tracejada entre elas, como no mapa do casal do site.
            if (marcas.size == 2) {
                drawLine(
                    color = TintaSuave,
                    start = Offset(marcas[0].second * k, marcas[0].third * k),
                    end = Offset(marcas[1].second * k, marcas[1].third * k),
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 24f))
                )
            }
            marcas.forEachIndexed { indice, (_, x, y) ->
                val centro = Offset(x * k, y * k)
                drawCircle(color = Ouro, radius = 9.dp.toPx(), center = centro)
                drawCircle(
                    color = if (indice == 0) Tinta else Ocre,
                    radius = 9.dp.toPx(),
                    center = centro,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Nomes ao lado de cada ponto.
        marcas.forEach { (ponto, x, y) ->
            Text(
                ponto.nome.ifBlank { "Você" },
                style = EstiloRotulo,
                color = Tinta,
                modifier = Modifier
                    .offset(x = (x - 40f).dp, y = (y - 34f).dp)
                    .background(Papel)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        Text(
            "© OpenStreetMap",
            style = EstiloRotulo,
            color = Tinta,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Papel.copy(alpha = 0.85f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
