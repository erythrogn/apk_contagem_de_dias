package com.tamojunto.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser

// Ícone de traço fino e arredondado, desenhado sobre viewBox 24, como os humores e as estrelas.
@Composable
internal fun IconeLinha(caminhos: List<String>, tamanho: Dp, cor: Color, espessura: Float = 1.8f) {
    val prontos = remember(caminhos) {
        caminhos.map { PathParser.createPathFromPathData(it).asComposePath() }
    }
    Canvas(modifier = Modifier.size(tamanho)) {
        val escala = size.minDimension / 24f
        withTransform({ scale(escala, escala, Offset.Zero) }) {
            prontos.forEach { caminho ->
                drawPath(
                    path = caminho,
                    color = cor,
                    style = Stroke(width = espessura, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

// Rótulos da barra inferior: Montserrat semibold, pequeno e sem caixa-alta.
internal val EstiloMenu = TextStyle(
    fontFamily = Sem,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp
)

// Barra inferior no papel do site: filete dourado no topo e, na aba ativa, um traço dourado embaixo.
@Composable
internal fun BarraInferior(abas: List<Aba>, atual: Aba, aoEscolher: (Aba) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Papel)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ouro.copy(alpha = 0.8f)))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            abas.forEach { aba ->
                val ativa = aba == atual
                val cor = if (ativa) Tinta else TintaSuave
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { aoEscolher(aba) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconeLinha(caminhos = aba.caminhos, tamanho = 24.dp, cor = cor)
                    Text(aba.rotulo, style = EstiloMenu, color = cor, modifier = Modifier.padding(top = 4.dp))
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .width(20.dp)
                            .height(2.dp)
                            .background(if (ativa) Ouro else Color.Transparent)
                    )
                }
            }
        }
    }
}
