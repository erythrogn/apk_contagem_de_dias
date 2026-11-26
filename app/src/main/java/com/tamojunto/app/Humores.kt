package com.tamojunto.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser

// Os seis humores do site. Cada humor é um tempo: sol, coração, nuvem, névoa, chuva e trovão.
// Os traços são os mesmos do site (juntos-comum.js), desenhados aqui sem emoji.
data class OpcaoHumor(val id: String, val rotulo: String, val caminhos: List<String>)

private const val NUVEM = "M7 17h10a4 4 0 0 0 0-8 6 6 0 0 0-11.4 1.6A3.2 3.2 0 0 0 7 17z"

val OPCOES_HUMOR = listOf(
    OpcaoHumor(
        "feliz", "Feliz",
        listOf(
            "M16 12a4 4 0 1 1-8 0a4 4 0 1 1 8 0",
            "M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M18.4 5.6L17 7M7 17l-1.4 1.4"
        )
    ),
    OpcaoHumor(
        "apaixonado", "Apaixonado(a)",
        listOf("M12 20s-7-4.4-7-9.5A3.8 3.8 0 0 1 12 8a3.8 3.8 0 0 1 7 2.5C19 15.6 12 20 12 20z")
    ),
    OpcaoHumor("tranquilo", "Tranquilo(a)", listOf(NUVEM)),
    OpcaoHumor("cansado", "Cansado(a)", listOf("M4 10h16M6 14h12M8 18h8")),
    OpcaoHumor("triste", "Triste", listOf(NUVEM, "M9 20l-1 1.5M13 20l-1 1.5M17 20l-1 1.5")),
    OpcaoHumor("estressado", "Estressado(a)", listOf(NUVEM, "M13 14l-2 4h3l-2 4"))
)

fun opcaoDeHumor(id: String?): OpcaoHumor? = OPCOES_HUMOR.firstOrNull { it.id == id }

// Desenha o humor como traço fino, arredondado, como o ícone do site (stroke 1.4 sobre viewBox 24).
@Composable
fun IconeHumor(opcao: OpcaoHumor?, tamanho: Dp = 28.dp, cor: Color = LocalContentColor.current) {
    if (opcao == null) return
    val caminhos = remember(opcao.id) {
        opcao.caminhos.map { PathParser.createPathFromPathData(it).asComposePath() }
    }
    Canvas(modifier = Modifier.size(tamanho)) {
        val escala = size.minDimension / 24f
        withTransform({ scale(escala, escala, Offset.Zero) }) {
            caminhos.forEach { caminho ->
                drawPath(
                    path = caminho,
                    color = cor,
                    style = Stroke(width = 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
