package com.tamojunto.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Estrela de cinco pontas desenhada em Compose (o site também usa estrelas em SVG, sem caractere).
private fun caminhoEstrela(escala: Float): Path {
    val caminho = Path()
    for (i in 0 until 10) {
        val raio = if (i % 2 == 0) 11.0 else 4.6
        val angulo = -PI / 2 + i * PI / 5
        val x = ((12 + raio * cos(angulo)) * escala).toFloat()
        val y = ((12 + raio * sin(angulo)) * escala).toFloat()
        if (i == 0) caminho.moveTo(x, y) else caminho.lineTo(x, y)
    }
    caminho.close()
    return caminho
}

// Cinco estrelas. Com aoMudar, tocar em uma estrela define a nota; tocar na nota atual a tira, como no site.
@Composable
fun Estrelas(valor: Int, aoMudar: ((Int) -> Unit)? = null, tamanho: Dp = 22.dp, cor: Color = Ocre) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { n ->
            val base = Modifier.size(tamanho)
            val modificador = if (aoMudar != null) base.clickable { aoMudar(if (valor == n) 0 else n) } else base
            Canvas(modificador) {
                val escala = size.minDimension / 24f
                val forma = caminhoEstrela(escala)
                if (n <= valor) drawPath(forma, color = cor)
                drawPath(forma, color = cor, style = Stroke(width = 1.4f * escala))
            }
        }
    }
}
